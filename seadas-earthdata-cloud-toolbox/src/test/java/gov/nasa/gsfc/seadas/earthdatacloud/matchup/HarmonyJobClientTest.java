package gov.nasa.gsfc.seadas.earthdatacloud.matchup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static gov.nasa.gsfc.seadas.earthdatacloud.matchup.FakeHarmony.reply;
import static gov.nasa.gsfc.seadas.earthdatacloud.matchup.SeabassFileTest.resource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HarmonyJobClientTest {

    private static final Instant WHEN = Instant.parse("2010-10-10T18:07:30Z");

    private static String fixture(String name) throws IOException {
        return Files.readString(resource(name));
    }

    private static MatchupRequest request(double lat) {
        return MatchupRequest.of("C1272944705-OB_CLOUD", lat, -119.5, WHEN);
    }

    // ------------------------------------------------------------ requests

    @Test
    void requestUrlIsTheOneHarmonyRanInUat() {
        MatchupRequest request = new MatchupRequest("C1272944705-OB_CLOUD", 33.0, -119.5, WHEN, 0.25,
                Duration.ofMinutes(30), null, null);
        URI uri = request.uri(HarmonyEnvironment.UAT.harmony());

        assertEquals("/C1272944705-OB_CLOUD/ogc-api-coverages/1.0.0/collections/all/coverage/rangeset", uri.getPath());
        // As Harmony echoed it for UAT job f7c9563c (harmony-job-successful.json), decoded.
        assertEquals("subset=lat(32.75:33.25)&subset=lon(-119.75:-119.25)"
                        + "&subset=time(\"2010-10-10T17:37:30Z\":\"2010-10-10T18:37:30Z\")"
                        + "&format=text/csv&forceAsync=true",
                URLDecoder.decode(uri.getRawQuery(), StandardCharsets.UTF_8));
    }

    @Test
    void requestBoxShrinksEvenlyNearThePolesAndTheAntimeridianAndCarriesALabel() {
        MatchupRequest request = new MatchupRequest("C1-OB_CLOUD", 89.95, -179.95, WHEN, 0.1,
                Duration.ofHours(3), List.of("chlor_a", "Rrs_443"), "seadas-run-1");
        String query = URLDecoder.decode(request.uri(FakeHarmony.ROOT).getRawQuery(), StandardCharsets.UTF_8);

        assertTrue(query.contains("lat(89.9:90.0)"), "midpoint stays 89.95: " + query);
        assertTrue(query.contains("lon(-180.0:-179.9)"), "midpoint stays -179.95: " + query);
        assertTrue(query.contains("time(\"2010-10-10T15:07:30Z\":\"2010-10-10T21:07:30Z\")"), query);
        assertTrue(query.endsWith("&label=seadas-run-1"), query);
        assertTrue(request.uri(FakeHarmony.ROOT).getPath().contains("/collections/chlor_a,Rrs_443/coverage"));
        // Harmony matches CMR's names, group included (checked in UAT, 2026-11-05).
        URI grouped = request.withVariables(List.of("geophysical_data/chlor_a")).uri(FakeHarmony.ROOT);
        assertTrue(grouped.getRawPath().endsWith("/collections/geophysical_data%2Fchlor_a/coverage/rangeset"),
                grouped.getRawPath());
    }

    @Test
    void requestsThatCantBeRightAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> MatchupRequest.of("MODIST_L2_OC", 0, 0, WHEN));
        assertThrows(IllegalArgumentException.class, () -> MatchupRequest.of("C1-OB_CLOUD", 91, 0, WHEN));
        assertThrows(IllegalArgumentException.class, () -> MatchupRequest.of("C1-OB_CLOUD", 0, 181, WHEN));
        assertThrows(IllegalArgumentException.class, () -> MatchupRequest.of("C1-OB_CLOUD", Double.NaN, 0, WHEN));
        assertThrows(IllegalArgumentException.class, () -> MatchupRequest.of("C1-OB_CLOUD", 0, 180.0, WHEN));
        assertThrows(IllegalArgumentException.class, () -> MatchupRequest.of("C1-OB_CLOUD", -89.9995, 0, WHEN));
        assertThrows(IllegalArgumentException.class, () -> new MatchupRequest("C1-OB_CLOUD", 0, 0, WHEN, 0.1,
                Duration.ofHours(4), null, null));
        assertThrows(IllegalArgumentException.class, () -> new MatchupRequest("C1-OB_CLOUD", 0, 0, WHEN, 0,
                Duration.ofHours(1), null, null));
        assertThrows(IllegalArgumentException.class,
                () -> MatchupRequest.of("C1-OB_CLOUD", 0, 0, WHEN).withVariables(List.of("a,b")));
    }

    // ------------------------------------------------------------ single calls

    @Test
    void submitSendsTheTokenAndReadsTheJob() throws Exception {
        FakeHarmony harmony = new FakeHarmony();
        HarmonyJob job = harmony.client().submit(request(33.0));

        assertEquals("running", job.status());
        FakeHarmony.Call call = harmony.calls.get(0);
        assertEquals("GET", call.method());
        assertEquals("Bearer TEST-TOKEN", call.headers().get("Authorization"));
        assertTrue(call.headers().get("User-Agent").startsWith("SeaDAS-satellite-in-situ-matchup"));
        FakeHarmony.Call followed = harmony.calls.get(1);
        assertEquals("/jobs/" + job.jobId(), followed.path(), "the 303 to the job's page is followed");
        assertEquals("Bearer TEST-TOKEN", followed.headers().get("Authorization"));
    }

    @Test
    void harmonysRefusalIsShownInItsOwnWords() throws Exception {
        FakeHarmony harmony = new FakeHarmony();
        harmony.queue("/C1272944705", call -> reply(422, fixtureUnchecked("harmony-error-422.json")));

        HarmonyException e = assertThrows(HarmonyException.class, () -> harmony.client().submit(request(33.0)));
        assertEquals(422, e.httpStatus());
        assertEquals("harmony.UnsupportedOperation", e.code());
        assertTrue(e.getMessage().startsWith("the requested combination of operations"), e.getMessage());
        assertTrue(e.getMessage().endsWith("satellite-in-situ-matchup requires spatialSubset."), e.getMessage());
        assertFalse(e.unauthorized());
        assertEquals(1, harmony.calls.size(), "a 422 isn't retried");
    }

    @Test
    void aLoginRedirectMeansTheTokenIsMissingOrExpired() {
        FakeHarmony harmony = new FakeHarmony();
        harmony.queue("/jobs/", call -> reply(303, "", Map.of("location", "https://urs.test/oauth/authorize")));

        HarmonyException e = assertThrows(HarmonyException.class,
                () -> harmony.client().job("f7c9563c-7355-413f-aee6-f023d00014fe"));
        assertTrue(e.unauthorized());
        assertTrue(e.getMessage().contains("token is missing or expired"), e.getMessage());
    }

    @Test
    void readsHarmonysJobAndBatchStatusReplies() throws Exception {
        FakeHarmony harmony = new FakeHarmony();
        harmony.queue("/jobs/f7c9563c", call -> reply(200, fixtureUnchecked("harmony-job-successful.json")));
        harmony.queue("/jobs/status", call -> reply(200, fixtureUnchecked("harmony-jobs-status.json")));
        HarmonyJobClient client = harmony.client();

        HarmonyJob job = client.job("f7c9563c-7355-413f-aee6-f023d00014fe");
        assertTrue(job.successful() && job.terminal());
        assertEquals(1, job.dataLinks().size());
        assertEquals("TERRA_MODIS.20101010T180501.L2.OC.csv", job.dataLinks().get(0).title());

        Map<String, HarmonyJob> statuses = client.statuses(List.of("f7c9563c-7355-413f-aee6-f023d00014fe",
                "82277934-0fc3-4cca-9892-aa1da802d89c", "00000000-0000-0000-0000-000000000000"));
        assertEquals(2, statuses.size(), "jobs Harmony doesn't know are left out");
        assertEquals("successful", statuses.get("82277934-0fc3-4cca-9892-aa1da802d89c").status());
    }

    @Test
    void statusesAreAskedForInChunksOf2000() throws Exception {
        FakeHarmony harmony = new FakeHarmony();
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < 2500; i++) {
            ids.add(String.format("00000000-0000-0000-0000-%012d", i));
        }
        harmony.client().statuses(ids);

        List<FakeHarmony.Call> calls = harmony.calls("POST", "/jobs/status");
        assertEquals(2, calls.size());
        assertTrue(calls.get(0).body().contains("000000001999") && !calls.get(0).body().contains("000000002000"));
    }

    @Test
    void everyPageOfLinksIsRead() throws Exception {
        FakeHarmony harmony = new FakeHarmony();
        String id = "f7c9563c-7355-413f-aee6-f023d00014fe";
        harmony.queue("/jobs/" + id, call -> reply(200, FakeHarmony.job(id, "successful")
                .put("links", new org.json.JSONArray()
                        .put(link("data", FakeHarmony.ROOT + "/service-results/a.csv"))
                        .put(link("next", FakeHarmony.ROOT + "/jobs/" + id + "?page=2&limit=1"))).toString()));
        harmony.queue("/jobs/" + id, call -> reply(200, FakeHarmony.job(id, "successful")
                .put("links", new org.json.JSONArray()
                        .put(link("data", FakeHarmony.ROOT + "/service-results/b.csv"))).toString()));

        HarmonyJob job = harmony.client().job(id);
        assertEquals(List.of("a.csv", "b.csv"),
                job.dataLinks().stream().map(l -> HarmonyJobClient.fileName(l.href())).toList());
        assertNull(job.nextPage());
    }

    @Test
    void downloadsFollowTheRedirectWithoutTheToken(@TempDir Path dir) throws Exception {
        FakeHarmony harmony = new FakeHarmony();
        String id = "f7c9563c-7355-413f-aee6-f023d00014fe";
        HarmonyJob job = HarmonyJob.fromJson(FakeHarmony.job(id, "successful"));

        List<Path> files = harmony.client().downloadAll(job, dir);

        assertEquals(List.of(dir.resolve(id).resolve("G.csv")), files);
        assertEquals("obs_lat,obs_lon\n10.0,20.0\n", Files.readString(files.get(0)));
        assertEquals(2, harmony.calls.size());
        assertEquals("Bearer TEST-TOKEN", harmony.calls.get(0).headers().get("Authorization"));
        assertEquals("storage.test", harmony.calls.get(1).uri().getHost());
        assertNull(harmony.calls.get(1).headers().get("Authorization"), "the token stays with Harmony");
        try (var listing = Files.list(dir.resolve(id))) {
            assertEquals(1, listing.count(), "no .part file left");
        }
    }

    @Test
    void fileNamesFromLinksAreSafe() {
        assertEquals("a_b.csv", HarmonyJobClient.fileName(URI.create("https://h.test/x/a%20b.csv")));
        assertEquals("x.csv", HarmonyJobClient.fileName(URI.create("https://h.test/../../x.csv")));
        assertThrows(IllegalArgumentException.class, () -> HarmonyJobClient.fileName(URI.create("https://h.test/x/")));
        assertThrows(IllegalArgumentException.class, () -> HarmonyJobClient.fileName(URI.create("https://h.test/..")));
    }

    // ------------------------------------------------------------ retries

    @Test
    void readsAreRetriedAfterServerErrors() throws Exception {
        FakeHarmony harmony = new FakeHarmony();
        harmony.queue("/jobs/status", call -> reply(502, "<html>Bad Gateway</html>"));
        harmony.queue("/jobs/status", call -> reply(503, ""));

        harmony.client().statuses(List.of("f7c9563c-7355-413f-aee6-f023d00014fe"));
        assertEquals(3, harmony.calls.size());
        assertEquals(List.of(Duration.ofSeconds(2), Duration.ofSeconds(4)), harmony.sleeps);
    }

    @Test
    void readsGiveUpAfterFourTries() {
        FakeHarmony harmony = new FakeHarmony();
        for (int i = 0; i < 4; i++) {
            harmony.queue("/jobs/status", call -> reply(500, "<html>oops</html>"));
        }
        HarmonyException e = assertThrows(HarmonyException.class,
                () -> harmony.client().statuses(List.of("f7c9563c-7355-413f-aee6-f023d00014fe")));
        assertEquals(500, e.httpStatus());
        assertEquals(4, harmony.calls.size());
    }

    @Test
    void aSubmitIsRetriedOnlyWhenNoJobCanHaveBeenMade() throws Exception {
        FakeHarmony harmony = new FakeHarmony();
        harmony.queue("/C1272944705", call -> reply(429, "", Map.of("retry-after", "7")));
        harmony.client().submit(request(33.0));
        assertEquals(2, harmony.calls("GET", "/C1272944705").size());
        assertEquals(List.of(Duration.ofSeconds(7)), harmony.sleeps);

        FakeHarmony gateway = new FakeHarmony();
        gateway.queue("/C1272944705", call -> reply(502, ""));
        assertThrows(HarmonyException.class, () -> gateway.client().submit(request(33.0)));
        assertEquals(1, gateway.calls.size(), "a 502 may hide a job that was made");
    }

    @Test
    void backoffHonoursRetryAfterUpToAMinute() {
        assertEquals(Duration.ofSeconds(2), HarmonyJobClient.backoff(1, null));
        assertEquals(Duration.ofSeconds(8), HarmonyJobClient.backoff(3, null));
        assertEquals(Duration.ofSeconds(60), HarmonyJobClient.backoff(1, "3600"));
        assertEquals(Duration.ofSeconds(2), HarmonyJobClient.backoff(1, "Wed, 21 Oct 2026 07:28:00 GMT"));
    }

    // ------------------------------------------------------------ batches

    @Test
    void aBatchKeepsAtMostTheLimitInFlightAndReportsEveryRequest() throws Exception {
        FakeHarmony harmony = new FakeHarmony();
        List<MatchupRequest> requests = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            requests.add(request(30.0 + i));
        }
        List<String> events = new ArrayList<>();
        AtomicInteger lastDone = new AtomicInteger();

        HarmonyJobClient.BatchResult result = harmony.client().runBatch(requests, 3, () -> false,
                new HarmonyJobClient.BatchListener() {
                    public void submitted(int index, HarmonyJob job) {
                        events.add("submitted " + index);
                    }

                    public void finished(int index, HarmonyJob job) {
                        events.add("finished " + index);
                    }

                    public void failed(int index, IOException error) {
                        events.add("failed " + index);
                    }

                    public void progress(int done, int total) {
                        assertTrue(done >= lastDone.get() && total == 7);
                        lastDone.set(done);
                    }
                });

        assertFalse(result.cancelled());
        assertEquals(7, result.outcomes().size());
        assertEquals(7, lastDone.get());
        assertEquals(3, harmony.maxRunning);
        assertEquals(List.of("submitted 0", "submitted 1", "submitted 2"), events.subList(0, 3));
        for (HarmonyJobClient.Outcome outcome : result.outcomes()) {
            assertTrue(outcome.job().successful(), String.valueOf(outcome));
            assertEquals(1, outcome.job().dataLinks().size());
        }
        assertEquals(7, harmony.calls("GET", "/C1272944705").size());
    }

    @Test
    void aRefusedRequestIsAnOutcomeAndTheRunGoesOn() throws Exception {
        FakeHarmony harmony = new FakeHarmony();
        harmony.queue("/C1272944705", call -> reply(422, fixtureUnchecked("harmony-error-422.json")));

        HarmonyJobClient.BatchResult result = harmony.client().runBatch(
                List.of(request(30.0), request(31.0)), 1, () -> false, new HarmonyJobClient.BatchListener() {
                });

        HarmonyJobClient.Outcome refused = result.outcomes().get(0);
        assertNull(refused.jobId());
        assertEquals(422, ((HarmonyException) refused.error()).httpStatus());
        assertTrue(result.outcomes().get(1).job().successful());
    }

    @Test
    void aRejectedTokenStopsTheRun() {
        FakeHarmony harmony = new FakeHarmony();
        harmony.queue("/C1272944705", call -> reply(401, "{\"description\":\"Unauthorized\"}"));

        HarmonyException e = assertThrows(HarmonyException.class, () -> harmony.client().runBatch(
                List.of(request(30.0), request(31.0)), 2, () -> false, new HarmonyJobClient.BatchListener() {
                }));
        assertTrue(e.unauthorized());
        assertEquals(1, harmony.calls.size());
    }

    @Test
    void aCancelledRunCancelsItsJobsInHarmony() throws Exception {
        FakeHarmony harmony = new FakeHarmony();
        harmony.pollsPerJob = 100;
        AtomicInteger polls = new AtomicInteger();

        HarmonyJobClient.BatchResult result = harmony.client().runBatch(
                List.of(request(30.0), request(31.0), request(32.0)), 2, () -> polls.get() >= 2,
                new HarmonyJobClient.BatchListener() {
                    public void progress(int done, int total) {
                        polls.incrementAndGet();
                    }
                });

        assertTrue(result.cancelled());
        assertEquals(2, harmony.calls("POST", "/jobs/").stream().filter(c -> c.path().endsWith("/cancel")).count());
        assertEquals(2, result.outcomes().size(), "the third request was never submitted");
        assertTrue(result.outcomes().stream().allMatch(o -> o.jobId() != null && o.job() == null));
        assertEquals(2, harmony.calls("GET", "/C1272944705").size());
    }

    @Test
    void anInterruptCancelsTheRunToo() throws Exception {
        FakeHarmony harmony = new FakeHarmony();
        harmony.pollsPerJob = 100;
        HarmonyJobClient client = new HarmonyJobClient(FakeHarmony.ROOT, () -> "T", harmony,
                duration -> {
                    throw new InterruptedException();
                }, Duration.ofSeconds(5));

        HarmonyJobClient.BatchResult result = client.runBatch(List.of(request(30.0)), 1, () -> false,
                new HarmonyJobClient.BatchListener() {
                });

        assertTrue(result.cancelled());
        assertTrue(Thread.interrupted(), "the interrupt is kept for the caller");
        assertEquals(1, harmony.calls.stream().filter(c -> c.path().endsWith("/cancel")).count());
    }

    @Test
    void aResumedRunFollowsItsJobsWithoutSubmittingThemAgain() throws Exception {
        FakeHarmony harmony = new FakeHarmony();
        String earlier = "11111111-2222-3333-4444-555555555555";
        harmony.pollsLeft.put(earlier, 1);
        Map<Integer, String> submitted = new HashMap<>();
        submitted.put(0, earlier);

        HarmonyJobClient.BatchResult result = harmony.client().runBatch(List.of(request(30.0), request(31.0)),
                submitted, 2, () -> false, new HarmonyJobClient.BatchListener() {
                });

        assertEquals(1, harmony.calls("GET", "/C1272944705").size());
        assertEquals(earlier, result.outcomes().get(0).jobId());
        assertTrue(result.outcomes().get(0).job().successful());
    }

    private static org.json.JSONObject link(String rel, String href) {
        return new org.json.JSONObject().put("rel", rel).put("href", href);
    }

    private static String fixtureUnchecked(String name) {
        try {
            return fixture(name);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
