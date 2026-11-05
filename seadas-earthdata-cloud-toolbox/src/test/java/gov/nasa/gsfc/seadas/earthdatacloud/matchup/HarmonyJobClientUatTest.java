package gov.nasa.gsfc.seadas.earthdatacloud.matchup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The client against the real service in Harmony UAT, with the sprint-review
 * demo observations. Runs only when {@code UAT_TOKEN} holds an Earthdata Login
 * (UAT) token; takes about half a minute.
 */
@EnabledIfEnvironmentVariable(named = "UAT_TOKEN", matches = ".{20,}")
class HarmonyJobClientUatTest {

    private record Demo(String collection, double lat, double lon, String time, String status, String levels) {
    }

    private static final List<Demo> DEMO = List.of(
            new Demo("C1272944705-OB_CLOUD", 33.0, -119.5, "2010-10-10T18:07:30Z", "matched", null),
            new Demo("C1281448693-OB_CLOUD", 40.596, 5.813, "2026-06-03T11:35:00Z", "matched", null),
            new Demo("C1281448693-OB_CLOUD", 55.914, 19.015, "2026-06-03T11:35:00Z", "rejected", "5"),
            new Demo("C1272968279-OB_CLOUD", 42.925, 40.537, "2017-04-06T09:33:00Z", "matched", null));

    @Test
    void theDemoObservationsMatchAsInTheNotebook(@TempDir Path dir) throws Exception {
        HarmonyJobClient client = HarmonyJobClient.create(HarmonyEnvironment.UAT, () -> System.getenv("UAT_TOKEN"));
        List<MatchupRequest> requests = new ArrayList<>();
        for (Demo demo : DEMO) {
            requests.add(MatchupRequest.of(demo.collection(), demo.lat(), demo.lon(), Instant.parse(demo.time()))
                    .withLabel("seadas-matchup-test"));
        }

        HarmonyJobClient.BatchResult result = client.runBatch(requests, 2, () -> false,
                new HarmonyJobClient.BatchListener() {
                    public void submitted(int index, HarmonyJob job) {
                        System.out.println("UAT job " + index + ": "
                                + HarmonyEnvironment.UAT.workflowUi(job.jobId()));
                    }
                });

        assertFalse(result.cancelled());
        assertEquals(DEMO.size(), result.outcomes().size());
        for (HarmonyJobClient.Outcome outcome : result.outcomes()) {
            Demo demo = DEMO.get(outcome.index());
            assertTrue(outcome.job() != null && outcome.job().successful(), demo + ": " + outcome);

            List<Map<String, String>> rows = new ArrayList<>();
            for (Path csv : client.downloadAll(outcome.job(), dir)) {
                rows.addAll(readCsv(csv));
            }
            assertFalse(rows.isEmpty(), demo.toString());
            Map<String, String> best = rows.stream().filter(r -> demo.status().equals(r.get("matchup_status")))
                    .findFirst().orElseThrow(() -> new AssertionError(demo + ": no " + demo.status() + " row in " + rows));
            assertEquals(demo.lat(), Double.parseDouble(best.get("obs_lat")), 1e-6, "the service echoes the observation");
            assertEquals(demo.lon(), Double.parseDouble(best.get("obs_lon")), 1e-6);
            if (demo.levels() != null) {
                assertTrue(Arrays.asList(best.get("exclusion_levels").split(" ")).contains(demo.levels()),
                        best.get("exclusion_levels"));
            }
        }
    }

    @Test
    void aRunOfTheTwoPaceObservationsJoinsToSeabass(@TempDir Path dir) throws Exception {
        Path sb = dir.resolve("pace-demo.sb");
        Files.write(sb, List.of("/begin_header", "/missing=-9999", "/delimiter=comma",
                "/fields=station,date,time,lat,lon", "/units=none,yyyymmdd,hh:mm:ss,degrees,degrees", "/end_header",
                "ligurian,20260603,11:35:00,40.596,5.813", "baltic,20260603,11:35:00,55.914,19.015"));
        String pace = "C1281448693-OB_CLOUD";
        MatchupRun run = MatchupRun.create(sb, new MatchupRun.Settings(HarmonyEnvironment.UAT, List.of(pace),
                0.1, Duration.ofHours(3), List.of(), 2), dir.resolve("pace-demo.matchup.json"));
        HarmonyJobClient client = HarmonyJobClient.create(HarmonyEnvironment.UAT, () -> System.getenv("UAT_TOKEN"));

        run.execute(client, () -> false, new MatchupRun.Listener() {
        });
        assertTrue(run.finished());
        assertEquals(2, run.counts().get(MatchupRun.State.DONE), run.tasks().toString());

        MatchupProduct product = MatchupProduct.fetch(HttpTransport.jdk(), HarmonyEnvironment.UAT.cmr(), pace);
        assertEquals("sr^-1", product.unitsOf("Rrs"));
        ResultJoiner.Joined joined = run.join(Map.of(pace, product), ResultJoiner.Rows.ALL);
        assertTrue(joined.warnings().isEmpty(), joined.warnings().toString());
        Path out = dir.resolve("pace-demo.matchup.sb");
        joined.writer().write(out);

        SeabassFile result = SeabassFile.read(out);
        assertEquals("yes", result.value(0, "matchup_found"));
        assertEquals("PACE_OCI.20260603T113224.L2.OC_AOP.V3_2.NRT.nc", result.value(0, "oci_pace_l2fname"));
        assertEquals("-143.0", result.value(0, "oci_pace_tdiff"));
        assertEquals("yes", result.value(1, "matchup_found"), "the earlier matched pass (design D2)");
        assertEquals("-5844.0", result.value(1, "oci_pace_tdiff"));
        assertEquals("sr^-1", result.units().get(result.fieldIndex("oci_pace_rrs_442.3")));
        System.out.println("UAT run: " + result.fields().size() + " fields, label " + run.label());
    }

    @Test
    void theCoverageCheckFindsTheDemoGranules() throws Exception {
        CoverageChecker checker = CoverageChecker.create(HarmonyEnvironment.UAT, () -> System.getenv("UAT_TOKEN"));
        List<MatchupRequest> requests = new ArrayList<>();
        for (Demo demo : DEMO) {
            requests.add(MatchupRequest.of(demo.collection(), demo.lat(), demo.lon(), Instant.parse(demo.time())));
        }
        // The Baltic observation at 18:00 the day before: no PACE pass within 3 h.
        requests.add(MatchupRequest.of("C1281448693-OB_CLOUD", 55.914, 19.015,
                Instant.parse("2026-06-02T18:00:00Z")));

        CoverageChecker.Result result = checker.check(requests, () -> false, (c, t) -> {
        });
        assertTrue(result.errors().isEmpty(), result.errors().toString());
        assertEquals(2, result.granules().get(2), "the Baltic one: two PACE passes (design D2)");
        assertTrue(result.granules().subList(0, 4).stream().allMatch(g -> g > 0), result.granules().toString());
        assertEquals(0, result.granules().get(4));
    }

    @Test
    void theProductTableMatchesCmr() throws Exception {
        for (HarmonyEnvironment environment : HarmonyEnvironment.values()) {
            List<MatchupCollections.Entry> entries = MatchupCollections.in(environment);
            StringBuilder query = new StringBuilder("page_size=100");
            for (MatchupCollections.Entry entry : entries) {
                query.append("&concept_id%5B%5D=").append(entry.id(environment));
            }
            URI uri = URI.create(environment.cmr() + "/search/collections.json?" + query);
            Map<String, String> names = new HashMap<>();
            try (HttpTransport.Response response = HttpTransport.jdk().send("GET", uri,
                    Map.of("Accept", "application/json"), null)) {
                assertEquals(200, response.status());
                org.json.JSONArray feed = new org.json.JSONObject(response.text()).getJSONObject("feed")
                        .getJSONArray("entry");
                for (int i = 0; i < feed.length(); i++) {
                    names.put(feed.getJSONObject(i).getString("id"), feed.getJSONObject(i).getString("short_name"));
                }
            }
            for (MatchupCollections.Entry entry : entries) {
                assertEquals(entry.shortName(), names.get(entry.id(environment)), environment + " " + entry);
            }
        }
    }

    @Test
    void aBadTokenIsReportedAsALoginProblem() {
        HarmonyJobClient client = new HarmonyJobClient(HarmonyEnvironment.UAT.harmony(),
                () -> "not-a-token-not-a-token", HttpTransport.jdk(), d -> Thread.sleep(d.toMillis()),
                Duration.ofSeconds(5));
        HarmonyException e = org.junit.jupiter.api.Assertions.assertThrows(HarmonyException.class,
                () -> client.statuses(List.of("f7c9563c-7355-413f-aee6-f023d00014fe")));
        assertTrue(e.unauthorized(), e.httpStatus() + " " + e.getMessage());
    }

    /** The service's CSV: a header row, then values with no commas or quotes in them. */
    private static List<Map<String, String>> readCsv(Path csv) throws Exception {
        List<String> lines = Files.readAllLines(csv);
        String[] header = lines.get(0).split(",", -1);
        List<Map<String, String>> rows = new ArrayList<>();
        for (String line : lines.subList(1, lines.size())) {
            if (line.isBlank()) {
                continue;
            }
            String[] values = line.split(",", -1);
            Map<String, String> row = new HashMap<>();
            for (int i = 0; i < header.length && i < values.length; i++) {
                row.put(header[i], values[i]);
            }
            rows.add(row);
        }
        return rows;
    }
}
