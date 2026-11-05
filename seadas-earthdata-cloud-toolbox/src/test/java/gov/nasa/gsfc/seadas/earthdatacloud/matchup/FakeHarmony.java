package gov.nasa.gsfc.seadas.earthdatacloud.matchup;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * A stand-in for Harmony: submitted jobs run for a set number of polls, then
 * succeed with one CSV link. Records every call; individual replies can be
 * scripted with {@link #queue}.
 */
class FakeHarmony implements HttpTransport {

    static final URI ROOT = URI.create("https://harmony.test");

    record Call(String method, URI uri, Map<String, String> headers, String body) {
        String path() {
            return uri.getPath();
        }
    }

    final List<Call> calls = new ArrayList<>();
    final List<Duration> sleeps = new ArrayList<>();
    final Map<String, Integer> pollsLeft = new LinkedHashMap<>();
    final Map<String, String> finalStatus = new HashMap<>();
    /** Replies used, first in first out, before the simulation for a matching path prefix. */
    final Map<String, List<Function<Call, Response>>> queued = new HashMap<>();
    int pollsPerJob = 2;
    int maxRunning;

    HarmonyJobClient client() {
        return new HarmonyJobClient(ROOT, () -> "TEST-TOKEN", this, sleeps::add, Duration.ofSeconds(5));
    }

    void queue(String pathPrefix, Function<Call, Response> reply) {
        queued.computeIfAbsent(pathPrefix, k -> new ArrayList<>()).add(reply);
    }

    static Response reply(int status, String body) {
        return reply(status, body, Map.of());
    }

    static Response reply(int status, String body, Map<String, String> headers) {
        return new Response(status, headers, new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
    }

    List<Call> calls(String method, String pathPrefix) {
        return calls.stream().filter(c -> c.method().equals(method) && c.path().startsWith(pathPrefix)).toList();
    }

    @Override
    public Response send(String method, URI uri, Map<String, String> headers, String body) throws IOException {
        Call call = new Call(method, uri, Map.copyOf(headers), body);
        calls.add(call);
        for (Map.Entry<String, List<Function<Call, Response>>> entry : queued.entrySet()) {
            if (call.path().startsWith(entry.getKey()) && !entry.getValue().isEmpty()) {
                return entry.getValue().remove(0).apply(call);
            }
        }
        String path = uri.getPath();
        if (!uri.getHost().equals(ROOT.getHost())) {
            return reply(200, "obs_lat,obs_lon\n10.0,20.0\n");
        }
        if (path.endsWith("/coverage/rangeset")) {
            String id = UUID.randomUUID().toString();
            pollsLeft.put(id, pollsPerJob);
            maxRunning = Math.max(maxRunning, running());
            // As Harmony answers an async request: see the job's page.
            return reply(303, "", Map.of("location", "/jobs/" + id));
        }
        if (path.equals("/jobs/status")) {
            JSONArray ids = new JSONObject(body).getJSONArray("jobIDs");
            JSONArray statuses = new JSONArray();
            for (int i = 0; i < ids.length(); i++) {
                String id = ids.getString(i);
                if (!pollsLeft.containsKey(id)) {
                    continue;
                }
                int left = pollsLeft.get(id);
                if (left > 0) {
                    pollsLeft.put(id, left - 1);
                }
                statuses.put(new JSONObject().put("jobID", id).put("status", status(id)).put("progress", 50));
            }
            return reply(200, new JSONObject().put("jobStatuses", statuses).toString());
        }
        if (path.startsWith("/jobs/") && path.endsWith("/cancel")) {
            String id = path.substring("/jobs/".length(), path.length() - "/cancel".length());
            finalStatus.put(id, "canceled");
            pollsLeft.put(id, 0);
            return reply(200, job(id, "canceled").toString());
        }
        if (path.startsWith("/jobs/")) {
            String id = path.substring("/jobs/".length());
            return reply(200, job(id, status(id)).toString());
        }
        if (path.startsWith("/service-results/")) {
            return reply(307, "", Map.of("location", "https://storage.test/bucket" + path.substring(16)));
        }
        return reply(404, "{\"code\":\"harmony.NotFound\",\"description\":\"Error: no route\"}");
    }

    int running() {
        return (int) pollsLeft.values().stream().filter(left -> left > 0).count();
    }

    String status(String id) {
        return pollsLeft.getOrDefault(id, 0) > 0 ? "running" : finalStatus.getOrDefault(id, "successful");
    }

    static JSONObject job(String id, String status) {
        JSONArray links = new JSONArray();
        if (!status.equals("running")) {
            links.put(new JSONObject().put("rel", "data").put("type", "text/csv").put("title", "G.csv")
                    .put("href", ROOT + "/service-results/staging/public/" + id + "/1/G.csv"));
        }
        return new JSONObject().put("jobID", id).put("status", status).put("progress", 100)
                .put("message", "msg").put("links", links);
    }
}
