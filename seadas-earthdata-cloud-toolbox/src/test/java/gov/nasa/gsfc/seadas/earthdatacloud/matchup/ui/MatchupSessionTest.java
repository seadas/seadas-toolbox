package gov.nasa.gsfc.seadas.earthdatacloud.matchup.ui;

import gov.nasa.gsfc.seadas.earthdatacloud.matchup.HarmonyEnvironment;
import gov.nasa.gsfc.seadas.earthdatacloud.matchup.MatchupCollections;
import gov.nasa.gsfc.seadas.earthdatacloud.matchup.MatchupRun;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchupSessionTest {

    private static Path cruise(Path dir) throws IOException {
        Path sb = dir.resolve("cruise1.sb");
        Files.write(sb, List.of("/begin_header", "/missing=-9999", "/delimiter=comma", "/fields=date,time,lat,lon",
                "/units=yyyymmdd,hh:mm:ss,degrees,degrees", "/end_header", "20240520,19:30:00,29.97,-87.15"));
        return sb;
    }

    private static MatchupSession session() {
        return new MatchupSession(new MatchupSession.Options(HarmonyEnvironment.UAT, 4, 0.1), TokenSource.fixed("x"));
    }

    private static MatchupCollections.Entry entry(String shortName) {
        return MatchupCollections.all().stream().filter(e -> e.shortName().equals(shortName)).findFirst()
                .orElseThrow();
    }

    @Test
    void recordsAndOutputsAreNamedAfterTheSeabassFile(@TempDir Path dir) throws Exception {
        Path sb = cruise(dir);
        assertEquals(dir.resolve("cruise1.matchup-run.json"), MatchupSession.freeRecordPath(sb));
        assertNull(MatchupSession.latestRecord(sb));
        Files.writeString(dir.resolve("cruise1.matchup-run.json"), "{}");
        Files.writeString(dir.resolve("cruise1.matchup-run-2.json"), "{}");
        assertEquals(dir.resolve("cruise1.matchup-run-3.json"), MatchupSession.freeRecordPath(sb));
        assertEquals(dir.resolve("cruise1.matchup-run-2.json"), MatchupSession.latestRecord(sb));
        assertEquals(dir.resolve("cruise1_matchup.sb"), MatchupSession.outputPath(sb, ".sb"));
    }

    @Test
    void aRunThatSentNothingIsReplacedWhenTheSettingsChange(@TempDir Path dir) throws Exception {
        MatchupSession session = session();
        session.open(cruise(dir));
        session.choose(List.of(entry("MODIST_L2_OC")));
        MatchupRun first = session.ensureRun();
        assertSame(first, session.ensureRun(), "same settings, same run");

        session.setHalfWindow(Duration.ofHours(1));
        MatchupRun second = session.ensureRun();
        assertNotSame(first, second);
        assertEquals(Duration.ofHours(1), second.settings().halfWindow());
        assertFalse(Files.exists(dir.resolve("cruise1.matchup-run.json.results")));
        assertEquals(List.of(dir.resolve("cruise1.matchup-run.json")), Files.list(dir)
                .filter(p -> p.toString().endsWith(".json")).toList(), "the first record is gone, its name reused");
        assertEquals("C1272944705-OB_CLOUD", second.settings().collections().get(0), "the UAT concept ID");
    }

    @Test
    void aResumedRunBringsItsSettings(@TempDir Path dir) throws Exception {
        MatchupSession first = session();
        first.open(cruise(dir));
        first.choose(List.of(entry("PACE_OCI_L2_AOP_NRT"), entry("VIIRSN_L2_OC")));
        first.setVariables(List.of("geophysical_data/chlor_a"));
        Path record = first.ensureRun().record();

        MatchupSession second = session();
        second.resume(record);
        assertEquals(List.of("PACE_OCI_L2_AOP_NRT", "VIIRSN_L2_OC"),
                second.chosen().stream().map(MatchupCollections.Entry::shortName).toList());
        assertEquals(List.of("geophysical_data/chlor_a"), second.variables());
        assertSame(second.run(), second.ensureRun());

        MatchupSession production = new MatchupSession(new MatchupSession.Options(HarmonyEnvironment.PRODUCTION, 4,
                0.1), TokenSource.fixed("x"));
        IOException e = assertThrows(IOException.class, () -> production.resume(record));
        assertTrue(e.getMessage().contains("UAT (testing)"), e.getMessage());
    }
}
