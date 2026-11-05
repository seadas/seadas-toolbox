package gov.nasa.gsfc.seadas.earthdatacloud.matchup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

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
