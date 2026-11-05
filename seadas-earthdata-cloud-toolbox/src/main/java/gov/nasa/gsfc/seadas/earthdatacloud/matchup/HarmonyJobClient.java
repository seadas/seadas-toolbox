package gov.nasa.gsfc.seadas.earthdatacloud.matchup;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpConnectTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Submits matchup jobs to Harmony, follows them and downloads their CSVs.
 * No Swing: the GUI runs it on a worker thread and listens.
 *
 * <p>Calls that only read (status, job, download) are retried on network errors,
 * 429 and 5xx. A submit is retried only when Harmony can't have created a job
 * (no connection, 429, 503), so a retry never leaves a duplicate job behind.
 */
public final class HarmonyJobClient {

    /** Most job IDs {@code /jobs/status} takes in one call (harmony-py's limit). */
    static final int MAX_STATUS_IDS = 2000;
    static final int ATTEMPTS = 4;
    static final Duration MAX_RETRY_WAIT = Duration.ofSeconds(60);
    private static final int MAX_REDIRECTS = 5;
    private static final String USER_AGENT = "SeaDAS-satellite-in-situ-matchup Java/"
            + System.getProperty("java.version");

    /** Waits between polls and retries; tests replace it. */
    @FunctionalInterface
    public interface Sleeper {
        void sleep(Duration duration) throws InterruptedException;
    }

    /** What a batch run reports as it goes, on the thread running it. */
    public interface BatchListener {
        /** Harmony accepted request {@code index} as a job. */
        default void submitted(int index, HarmonyJob job) {
        }

        /** Request {@code index} reached a final status; the job has its result links. */
        default void finished(int index, HarmonyJob job) {
        }

        /** Request {@code index} has no job: Harmony refused it, or it couldn't be sent. */
        default void failed(int index, IOException error) {
        }

        /** After each poll: how many requests are done, out of all. */
        default void progress(int done, int total) {
        }
    }

    /**
     * One request's outcome.
     *
     * @param jobId the job, or null when none was created
     * @param job   the final job with its links, or null when it didn't finish
     * @param error why it has no final job, or null
     */
    public record Outcome(int index, String jobId, HarmonyJob job, IOException error) {
    }

    /** A batch run's outcomes, by request index; {@code cancelled} when the run was stopped. */
    public record BatchResult(List<Outcome> outcomes, boolean cancelled) {
    }

    private final URI harmony;
    private final Supplier<String> token;
    private final HttpTransport http;
    private final Sleeper sleeper;
    private final Duration pollInterval;

    /**
     * @param token        the Earthdata Login token, asked for on each call so a renewed one is used
     * @param pollInterval time between status polls
     */
    public HarmonyJobClient(URI harmony, Supplier<String> token, HttpTransport http, Sleeper sleeper,
                            Duration pollInterval) {
        this.harmony = URI.create(harmony.toString().replaceAll("/+$", ""));
        this.token = Objects.requireNonNull(token, "token");
        this.http = Objects.requireNonNull(http, "http");
        this.sleeper = Objects.requireNonNull(sleeper, "sleeper");
        this.pollInterval = Objects.requireNonNull(pollInterval, "pollInterval");
    }

    /** A client for an environment, polling every 5 s. */
    public static HarmonyJobClient create(HarmonyEnvironment environment, Supplier<String> token) {
        return new HarmonyJobClient(environment.harmony(), token, HttpTransport.jdk(),
                duration -> Thread.sleep(duration.toMillis()), Duration.ofSeconds(5));
    }

    public URI harmony() {
        return harmony;
    }

    // ------------------------------------------------------------ single calls

    /** Submits one request; the returned job is usually {@code accepted} or {@code running}. */
    public HarmonyJob submit(MatchupRequest request) throws IOException, InterruptedException {
        return HarmonyJob.fromJson(json(call("GET", request.uri(harmony), null, false)));
    }

    /** The status and progress of jobs, in one call per 2000 IDs. Jobs Harmony doesn't know are left out. */
    public Map<String, HarmonyJob> statuses(Collection<String> jobIds) throws IOException, InterruptedException {
        List<String> ids = new ArrayList<>(jobIds);
        Map<String, HarmonyJob> statuses = new LinkedHashMap<>();
        for (int i = 0; i < ids.size(); i += MAX_STATUS_IDS) {
            List<String> chunk = ids.subList(i, Math.min(ids.size(), i + MAX_STATUS_IDS));
            String body = new JSONObject().put("jobIDs", new JSONArray(chunk)).toString();
            JSONObject json = json(call("POST", URI.create(harmony + "/jobs/status"), body, true));
            JSONArray jobs = json.optJSONArray("jobStatuses");
            for (int j = 0; jobs != null && j < jobs.length(); j++) {
                HarmonyJob job = HarmonyJob.fromJson(jobs.getJSONObject(j));
                statuses.put(job.jobId(), job);
            }
        }
        return statuses;
    }

    /** A job with all its links (every page of them). */
    public HarmonyJob job(String jobId) throws IOException, InterruptedException {
        HarmonyJob job = HarmonyJob.fromJson(json(call("GET", jobUri(jobId, ""), null, true)));
        for (int page = 0; page < 1000; page++) {
            HarmonyJob.Link next = job.nextPage();
            if (next == null) {
                return job;
            }
            HarmonyJob more = HarmonyJob.fromJson(json(call("GET", harmonyOnly(next.href()), null, true)));
            job = job.withMoreLinks(more.links());
        }
        throw new IOException("Too many pages of links for job " + jobId);
    }

    /** Asks Harmony to cancel a job. */
    public void cancel(String jobId) throws IOException, InterruptedException {
        call("POST", jobUri(jobId, "/cancel"), "", true).close();
    }

    /**
     * Downloads a job's output files into {@code dir/<jobId>/} (one folder per
     * job, since many jobs return a file for the same granule).
     */
    public List<Path> downloadAll(HarmonyJob job, Path dir) throws IOException, InterruptedException {
        Path folder = dir.resolve(job.jobId());
        Files.createDirectories(folder);
        List<Path> files = new ArrayList<>();
        for (HarmonyJob.Link link : job.dataLinks()) {
            files.add(download(link.href(), folder));
        }
        return files;
    }

    /**
     * Downloads one result file into {@code folder}, named as in its URL. The token
     * goes only to Harmony, not to the storage Harmony redirects to.
     */
    public Path download(URI href, Path folder) throws IOException, InterruptedException {
        String name = fileName(href);
        Path target = folder.resolve(name);
        Path part = folder.resolve(name + ".part");
        URI current = href;
        for (int hop = 0; hop <= MAX_REDIRECTS; hop++) {
            boolean toHarmony = sameHost(current, harmony);
            try (HttpTransport.Response response = retrying("GET", current, null, true, toHarmony)) {
                int status = response.status();
                if (isRedirect(status)) {
                    String location = response.header("location");
                    if (location == null) {
                        throw new IOException("Redirect without a location: " + current);
                    }
                    current = current.resolve(location);
                    continue;
                }
                if (status >= 400) {
                    throw error(status, response.text());
                }
                try (InputStream in = response.body()) {
                    Files.copy(in, part, StandardCopyOption.REPLACE_EXISTING);
                }
                Files.move(part, target, StandardCopyOption.REPLACE_EXISTING);
                return target;
            } finally {
                Files.deleteIfExists(part);
            }
        }
        throw new IOException("Too many redirects downloading " + href);
    }

    // ------------------------------------------------------------ batches

    /** Runs requests with at most {@code maxInFlight} jobs at a time; see {@link #runBatch(List, Map, int, BooleanSupplier, BatchListener)}. */
    public BatchResult runBatch(List<MatchupRequest> requests, int maxInFlight, BooleanSupplier cancelled,
                                BatchListener listener) throws IOException, InterruptedException {
        return runBatch(requests, Map.of(), maxInFlight, cancelled, listener);
    }

    /**
     * Runs requests with at most {@code maxInFlight} jobs at a time, submitting
     * the next as jobs finish, until every request is done or {@code cancelled}
     * says stop. A stopped run cancels its jobs in Harmony.
     *
     * <p>{@code submitted} gives job IDs from an earlier run (by request index):
     * those jobs are followed, not submitted again, so a run can resume.
     *
     * <p>A request Harmony refuses, or that can't be sent, is an outcome with an
     * error and the run goes on. A rejected token stops the run: every request
     * would fail the same way.
     *
     * @throws HarmonyException when the token is rejected, or polling fails after retries
     */
    public BatchResult runBatch(List<MatchupRequest> requests, Map<Integer, String> submitted, int maxInFlight,
                                BooleanSupplier cancelled, BatchListener listener)
            throws IOException, InterruptedException {
        if (maxInFlight < 1) {
            throw new IllegalArgumentException("maxInFlight must be at least 1: " + maxInFlight);
        }
        int total = requests.size();
        Outcome[] outcomes = new Outcome[total];
        Map<String, Integer> inFlight = new LinkedHashMap<>();
        List<Integer> queue = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            String jobId = submitted.get(i);
            if (jobId != null) {
                inFlight.put(jobId, i);
            } else {
                queue.add(i);
            }
        }
        int next = 0;
        int done = 0;
        boolean interrupted = false;
        try {
            while (next < queue.size() || !inFlight.isEmpty()) {
                if (cancelled.getAsBoolean()) {
                    break;
                }
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }
                while (inFlight.size() < maxInFlight && next < queue.size()) {
                    int index = queue.get(next++);
                    try {
                        HarmonyJob job = submit(requests.get(index));
                        listener.submitted(index, job);
                        if (job.terminal()) {
                            HarmonyJob full = job(job.jobId());
                            outcomes[index] = new Outcome(index, full.jobId(), full, null);
                            listener.finished(index, full);
                            done++;
                        } else {
                            inFlight.put(job.jobId(), index);
                        }
                    } catch (HarmonyException e) {
                        if (e.unauthorized()) {
                            throw e;
                        }
                        outcomes[index] = new Outcome(index, null, null, e);
                        listener.failed(index, e);
                        done++;
                    } catch (IOException | JSONException e) {
                        IOException error = e instanceof IOException io ? io
                                : new IOException("Harmony's reply wasn't a job: " + e.getMessage(), e);
                        outcomes[index] = new Outcome(index, null, null, error);
                        listener.failed(index, error);
                        done++;
                    }
                }
                listener.progress(done, total);
                if (inFlight.isEmpty()) {
                    continue;
                }

                sleeper.sleep(pollInterval);
                Map<String, HarmonyJob> statuses = statuses(inFlight.keySet());
                for (String jobId : new ArrayList<>(inFlight.keySet())) {
                    HarmonyJob status = statuses.get(jobId);
                    int index = inFlight.get(jobId);
                    if (status == null) {
                        inFlight.remove(jobId);
                        IOException error = new HarmonyException(404, null, "Harmony has no job " + jobId);
                        outcomes[index] = new Outcome(index, jobId, null, error);
                        listener.failed(index, error);
                        done++;
                    } else if (status.terminal()) {
                        inFlight.remove(jobId);
                        HarmonyJob full = job(jobId);
                        outcomes[index] = new Outcome(index, jobId, full, null);
                        listener.finished(index, full);
                        done++;
                    }
                }
                listener.progress(done, total);
            }
        } catch (InterruptedException e) {
            // Thrown with the flag cleared, so the cancel calls below can still be made.
            interrupted = true;
        }
        if (inFlight.isEmpty() && next >= queue.size()) {
            return new BatchResult(collect(outcomes), false);
        }
        cancelAll(inFlight.keySet());
        for (Map.Entry<String, Integer> entry : inFlight.entrySet()) {
            int index = entry.getValue();
            outcomes[index] = new Outcome(index, entry.getKey(), null, new IOException("Cancelled"));
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
        return new BatchResult(collect(outcomes), true);
    }

    private void cancelAll(Collection<String> jobIds) {
        for (String jobId : jobIds) {
            try {
                call("POST", jobUri(jobId, "/cancel"), "", false).close();
            } catch (IOException | InterruptedException ignored) {
                // Best effort: the job still ends on its own, and the run record keeps its ID.
            }
        }
    }

    private static List<Outcome> collect(Outcome[] outcomes) {
        List<Outcome> list = new ArrayList<>();
        for (Outcome outcome : outcomes) {
            if (outcome != null) {
                list.add(outcome);
            }
        }
        return list;
    }

    // ------------------------------------------------------------ HTTP

    private URI jobUri(String jobId, String suffix) {
        if (!jobId.matches("[0-9a-fA-F-]{36}")) {
            throw new IllegalArgumentException("not a Harmony job ID: " + jobId);
        }
        return URI.create(harmony + "/jobs/" + URLEncoder.encode(jobId, StandardCharsets.UTF_8) + suffix);
    }

    private URI harmonyOnly(URI uri) throws IOException {
        if (!sameHost(uri, harmony)) {
            throw new IOException("Harmony link points elsewhere: " + uri);
        }
        return uri;
    }

    /**
     * A Harmony API call, retried as {@code idempotent} allows. A redirect within
     * Harmony is followed with a GET (an async submit answers 303 to its job's
     * page); a redirect anywhere else is the Earthdata login, so the token is bad.
     * An error status is an exception.
     */
    private HttpTransport.Response call(String method, URI uri, String body, boolean idempotent)
            throws IOException, InterruptedException {
        HttpTransport.Response response = retrying(method, uri, body, idempotent, true);
        for (int hop = 0; isRedirect(response.status()); hop++) {
            int status = response.status();
            String location = response.header("location");
            response.close();
            URI target = location == null ? null : uri.resolve(location);
            if (target == null || !sameHost(target, harmony)) {
                throw new HarmonyException(status, null,
                        "Harmony asked for an Earthdata login: the token is missing or expired");
            }
            if (hop >= MAX_REDIRECTS) {
                throw new IOException("Too many redirects from " + uri);
            }
            uri = target;
            response = retrying("GET", uri, null, true, true);
        }
        int status = response.status();
        if (status >= 400) {
            throw error(status, response.text());
        }
        return response;
    }

    private HttpTransport.Response retrying(String method, URI uri, String body, boolean idempotent,
                                            boolean withToken) throws IOException, InterruptedException {
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", USER_AGENT);
        headers.put("Accept", "application/json");
        if (body != null && !body.isEmpty()) {
            headers.put("Content-Type", "application/json");
        }
        if (withToken) {
            String bearer = token.get();
            if (bearer != null && !bearer.isBlank()) {
                headers.put("Authorization", "Bearer " + bearer.strip());
            }
        }
        for (int attempt = 1; ; attempt++) {
            HttpTransport.Response response;
            try {
                response = http.send(method, uri, headers, body);
            } catch (ConnectException | HttpConnectTimeoutException e) {
                if (attempt >= ATTEMPTS) {
                    throw e;
                }
                sleeper.sleep(backoff(attempt, null));
                continue;
            } catch (IOException e) {
                if (!idempotent || attempt >= ATTEMPTS) {
                    throw e;
                }
                sleeper.sleep(backoff(attempt, null));
                continue;
            }
            int status = response.status();
            boolean retry = status == 429 || status == 503 || (idempotent && status >= 500);
            if (!retry || attempt >= ATTEMPTS) {
                return response;
            }
            String retryAfter = response.header("retry-after");
            response.close();
            sleeper.sleep(backoff(attempt, retryAfter));
        }
    }

    /** 2, 4, 8 s, or what Harmony's Retry-After asks (at most a minute). */
    static Duration backoff(int attempt, String retryAfter) {
        if (retryAfter != null) {
            try {
                long seconds = Long.parseLong(retryAfter.strip());
                return Duration.ofSeconds(Math.max(1, Math.min(seconds, MAX_RETRY_WAIT.toSeconds())));
            } catch (NumberFormatException ignored) {
                // An HTTP date: fall back to the usual wait.
            }
        }
        return Duration.ofSeconds(1L << Math.min(attempt, 5));
    }

    private static JSONObject json(HttpTransport.Response response) throws IOException {
        String text = response.text();
        try {
            return new JSONObject(text);
        } catch (JSONException e) {
            throw new IOException("Harmony's reply wasn't JSON: " + abbreviate(text), e);
        }
    }

    /** Harmony's error, using its {@code description} when the body has one. */
    static HarmonyException error(int status, String body) {
        String code = null;
        String message = null;
        try {
            JSONObject json = new JSONObject(body);
            code = json.optString("code", null);
            message = json.optString("description", null);
        } catch (JSONException ignored) {
            // Not JSON (e.g. an HTML error page).
        }
        if (message == null || message.isBlank()) {
            message = "Harmony returned HTTP " + status + (body.isBlank() ? "" : ": " + abbreviate(body));
        } else if (message.startsWith("Error: ")) {
            message = message.substring("Error: ".length());
        }
        if (status == 401 || status == 403) {
            message = "Earthdata Login token not accepted (HTTP " + status + "): " + message;
        }
        return new HarmonyException(status, code, message);
    }

    private static String abbreviate(String text) {
        String flat = text.replaceAll("\\s+", " ").strip();
        return flat.length() <= 300 ? flat : flat.substring(0, 300) + "...";
    }

    private static boolean isRedirect(int status) {
        return status == 301 || status == 302 || status == 303 || status == 307 || status == 308;
    }

    /** Whether {@code uri} is on Harmony's host and scheme: the only place the token is sent. */
    private static boolean sameHost(URI uri, URI harmony) {
        return uri.getHost() != null && uri.getHost().equalsIgnoreCase(harmony.getHost())
                && harmony.getScheme().equalsIgnoreCase(uri.getScheme());
    }

    /** The last path segment, made safe as a file name. */
    static String fileName(URI href) {
        String path = href.getPath() == null ? "" : href.getPath();
        String name = path.substring(path.lastIndexOf('/') + 1).replaceAll("[^A-Za-z0-9._-]", "_");
        if (name.isEmpty() || name.matches("\\.+")) {
            throw new IllegalArgumentException("no file name in " + href);
        }
        return name;
    }
}
