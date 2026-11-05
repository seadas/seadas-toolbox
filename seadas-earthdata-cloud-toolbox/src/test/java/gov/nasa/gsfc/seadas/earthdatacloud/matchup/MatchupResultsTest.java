package gov.nasa.gsfc.seadas.earthdatacloud.matchup;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchupResultsTest {

    private static final String MODISA = "C3380708980-OB_CLOUD";

    private static MatchupRun run(Path dir, String status) throws Exception {
        Path sb = dir.resolve("cruise.sb");
        Files.write(sb, List.of("/begin_header", "/missing=-9999", "/delimiter=comma",
                "/fields=station,date,time,lat,lon", "/units=none,yyyymmdd,hh:mm:ss,degrees,degrees", "/end_header",
                "1,20240520,19:30:00,29.9702,-87.1583",
                "2,20240521,14:17:00,29.0915,-84.91",
                "3,20240521,17:10:00,-9999,-84.705"));
        MatchupRun run = MatchupRun.create(sb, new MatchupRun.Settings(HarmonyEnvironment.PRODUCTION, List.of(MODISA),
                0.1, Duration.ofHours(3), List.of(), 2), dir.resolve("cruise.matchup-run.json"));
        // No granule for row 2.
        run.checkCoverage(new CoverageChecker(new CoverageCheckerTest.FakeCmr(uri ->
                CoverageCheckerTest.FakeCmr.hits(uri.getRawQuery().contains("-85.01") ? 0 : 1)),
                FakeHarmony.ROOT, () -> null, d -> {
        }, 1), () -> false, (c, t) -> {
        });
        FakeHarmony harmony = new FakeHarmony();
        harmony.csvStatus = status;
        run.execute(harmony.client(), () -> false, new MatchupRun.Listener() {
        });
        return run;
    }

    @Test
    void eachObservationHasAnOutcomeAndTheSummaryCountsThem(@TempDir Path dir) throws Exception {
        MatchupResults results = MatchupResults.of(run(dir, "matched"));

        assertEquals(List.of(MatchupResults.Outcome.MATCHED, MatchupResults.Outcome.NO_OVERPASS,
                MatchupResults.Outcome.SKIPPED), results.items().stream().map(MatchupResults.Item::outcome).toList());
        assertEquals("0.384000", results.items().get(0).chosen().get("sat_chlor_a_mean"));
        assertNull(results.items().get(0).reason());
        assertTrue(results.items().get(2).reason().startsWith("Latitude missing"), results.items().get(2).reason());
        assertEquals("1 matched · 1 no overpass · 0 failed · 1 not usable", results.summary(null));
        assertEquals(1, results.rows().size());
        assertTrue(results.rows().get(0).chosen());
    }

    @Test
    void rejectionsReadAsWords(@TempDir Path dir) throws Exception {
        MatchupResults results = MatchupResults.of(run(dir, "rejected"));

        MatchupResults.Item rejected = results.items().get(0);
        assertEquals(MatchupResults.Outcome.REJECTED, rejected.outcome());
        assertEquals("too variable (5)", rejected.reason());
        assertEquals("0 matched · 1 rejected (too variable 1) · 1 no overpass · 0 failed · 1 not usable",
                results.summary(MODISA));
        assertEquals("time (1), cloud or flags (4)", MatchupResults.reason("1 4"));
        assertEquals("1: time difference above the limit; 4: unflagged percentage below the threshold",
                MatchupResults.reasonInFull("1 4"));
        assertEquals("", MatchupResults.reason(""));
    }

    @Test
    void theCsvHasEveryGranuleRowWithItsSeabassRow(@TempDir Path dir) throws Exception {
        MatchupResults results = MatchupResults.of(run(dir, "matched"));
        Path csv = dir.resolve("out.csv");
        results.writeCsv(csv);

        List<String> lines = Files.readAllLines(csv);
        assertEquals(2, lines.size());
        assertTrue(lines.get(0).startsWith("seabass_row,seabass_line,product,collection,chosen,obs_lat,obs_lon,"),
                lines.get(0));
        assertTrue(lines.get(1).startsWith("1,7,MODISA_L2_OC," + MODISA + ",yes,29.9702,-87.1583,"), lines.get(1));
    }

    @Test
    void cmrVariableNamesKeepTheirGroup() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("cmr-variables-C1272944705-OB_CLOUD.json")) {
            JSONObject json = new JSONObject(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            List<String> names = MatchupProduct.namesFrom(json);
            assertTrue(names.contains("geophysical_data/chlor_a"), names.toString());
            assertFalse(names.contains("chlor_a"));
        }
    }
}
