package gov.nasa.gsfc.seadas.earthdatacloud.matchup;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Before a run: how many granules each request's box and time range touch,
 * from the CMR granule search Harmony makes for it. A request with none would
 * give an empty job, so it isn't sent.
 *
 * <p>Asks only for the count ({@code page_size=0}, the {@code CMR-Hits}
 * header). Several searches run at once; failed ones are retried as the job
 * client retries reads.
 */
public final class CoverageChecker {

    /** What a check reports as it goes, from the worker threads. */
    @FunctionalInterface
    public interface Progress {
        void progress(int checked, int total);
    }

    /**
     * @param granules granule count per request, or null where unknown (failed or not checked)
     * @param errors   why a request's count is unknown, by request index
     */
    public record Result(List<Integer> granules, Map<Integer, String> errors, boolean cancelled) {
        public Result {
            granules = Collections.unmodifiableList(new ArrayList<>(granules));
            errors = Collections.unmodifiableMap(new TreeMap<>(errors));
        }
    }

    private static final String USER_AGENT = "SeaDAS-satellite-in-situ-matchup Java/"
            + System.getProperty("java.version");

    private final HttpTransport http;
    private final URI cmr;
    private final Supplier<String> token;
    private final HarmonyJobClient.Sleeper sleeper;
    private final int parallel;

    /**
     * @param token    Earthdata Login token, sent so restricted collections count too; may supply null
     * @param parallel searches at once
     */
    public CoverageChecker(HttpTransport http, URI cmr, Supplier<String> token, HarmonyJobClient.Sleeper sleeper,
                           int parallel) {
        if (parallel < 1) {
            throw new IllegalArgumentException("parallel must be at least 1: " + parallel);
        }
        this.http = http;
        this.cmr = cmr;
        this.token = token;
        this.sleeper = sleeper;
        this.parallel = parallel;
    }

    /** Against an environment's CMR, 8 searches at once. */
    public static CoverageChecker create(HarmonyEnvironment environment, Supplier<String> token) {
        return new CoverageChecker(HttpTransport.jdk(), environment.cmr(), token,
                d -> Thread.sleep(d.toMillis()), 8);
    }

    /**
     * Counts the granules for each request.
     *
     * @throws HarmonyException when CMR rejects the token; nothing else stops the check
     */
    public Result check(List<MatchupRequest> requests, BooleanSupplier cancelled, Progress progress)
            throws HarmonyException, InterruptedException {
        Integer[] granules = new Integer[requests.size()];
        Map<Integer, String> errors = Collections.synchronizedMap(new HashMap<>());
        AtomicInteger next = new AtomicInteger();
        AtomicInteger checked = new AtomicInteger();
        AtomicReference<HarmonyException> login = new AtomicReference<>();
        AtomicBoolean stopped = new AtomicBoolean();
        int threads = Math.max(1, Math.min(parallel, requests.size()));
        ExecutorService pool = Executors.newFixedThreadPool(threads, r -> {
            Thread thread = new Thread(r, "matchup-coverage");
            thread.setDaemon(true);
            return thread;
        });
        try {
            List<Future<?>> workers = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                workers.add(pool.submit(() -> {
                    for (int i = next.getAndIncrement(); i < requests.size(); i = next.getAndIncrement()) {
                        if (login.get() != null) {
                            return null;
                        }
                        if (cancelled.getAsBoolean() || Thread.currentThread().isInterrupted()) {
                            stopped.set(true);
                            return null;
                        }
                        try {
                            granules[i] = count(requests.get(i));
                        } catch (HarmonyException e) {
                            if (e.unauthorized()) {
                                login.compareAndSet(null, e);
                                return null;
                            }
                            errors.put(i, e.getMessage());
                        } catch (IOException e) {
                            errors.put(i, e.getMessage() != null ? e.getMessage() : e.toString());
                        }
                        progress.progress(checked.incrementAndGet(), requests.size());
                    }
                    return null;
                }));
            }
            for (Future<?> worker : workers) {
                try {
                    worker.get();
                } catch (java.util.concurrent.ExecutionException e) {
                    throw new IllegalStateException("coverage check failed", e.getCause());
                }
            }
        } finally {
            pool.shutdownNow();
        }
        if (login.get() != null) {
            throw login.get();
        }
        return new Result(Arrays.asList(granules), errors, stopped.get());
    }

    /** The granule count for one request. */
    int count(MatchupRequest request) throws IOException, InterruptedException {
        URI uri = request.granuleSearch(cmr);
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", USER_AGENT);
        headers.put("Accept", "application/json");
        String bearer = token == null ? null : token.get();
        if (bearer != null && !bearer.isBlank()) {
            headers.put("Authorization", "Bearer " + bearer.strip());
        }
        for (int attempt = 1; ; attempt++) {
            HttpTransport.Response response;
            try {
                response = http.send("GET", uri, headers, null);
            } catch (IOException e) {
                if (attempt >= HarmonyJobClient.ATTEMPTS) {
                    throw new IOException("CMR granule search failed: " + e.getMessage(), e);
                }
                sleeper.sleep(HarmonyJobClient.backoff(attempt, null));
                continue;
            }
            try (response) {
                int status = response.status();
                if ((status == 429 || status >= 500) && attempt < HarmonyJobClient.ATTEMPTS) {
                    sleeper.sleep(HarmonyJobClient.backoff(attempt, response.header("retry-after")));
                    continue;
                }
                if (status == 401 || status == 403) {
                    throw new HarmonyException(status, null,
                            "CMR refused the Earthdata login: the token is missing or expired");
                }
                if (status != 200) {
                    throw new IOException("CMR granule search: HTTP " + status + cmrErrors(response.text()));
                }
                String hits = response.header("cmr-hits");
                if (hits == null) {
                    throw new IOException("CMR granule search: no CMR-Hits header");
                }
                try {
                    return Integer.parseInt(hits.strip());
                } catch (NumberFormatException e) {
                    throw new IOException("CMR granule search: CMR-Hits is " + hits);
                }
            }
        }
    }

    private static String cmrErrors(String body) {
        try {
            JSONArray errors = new JSONObject(body).getJSONArray("errors");
            List<String> list = new ArrayList<>();
            for (int i = 0; i < errors.length(); i++) {
                list.add(String.valueOf(errors.get(i)));
            }
            return ": " + String.join("; ", list);
        } catch (JSONException e) {
            return "";
        }
    }
}
