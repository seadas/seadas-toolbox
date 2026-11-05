package gov.nasa.gsfc.seadas.earthdatacloud.matchup;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static gov.nasa.gsfc.seadas.earthdatacloud.matchup.FakeHarmony.reply;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchupRunTest {

    private static final String MODISA = "C3380708980-OB_CLOUD";
    private static final String OTHER = "C1272968279-OB_CLOUD";

    private static Path cruise(Path dir) throws IOException {
        Path file = dir.resolve("cruise.sb");
        Files.write(file, List.of("/begin_header", "/missing=-9999", "/delimiter=comma",
                "/fields=station,date,time,lat,lon,chl", "/units=none,yyyymmdd,hh:mm:ss,degrees,degrees,mg/m^3",
                "/end_header",
                "1,20240520,19:30:00,29.9702,-87.1583,0.407",
                "2,20240521,14:17:00,29.0915,-84.91,0.111",
                "3,20240521,17:10:00,-9999,-84.705,0.124",
                "4,20240522,14:00:00,27.91,-83.2018,0.264"));
        return file;
    }

    private static MatchupRun.Settings settings(String... collections) {
        return new MatchupRun.Settings(HarmonyEnvironment.UAT, List.of(collections), 0.1, Duration.ofHours(3),
                List.of(), 2);
    }

    private static List<MatchupRun.State> states(MatchupRun run) {
        return run.tasks().stream().map(MatchupRun.Task::state).toList();
    }

    @Test
    void aNewRunHasATaskPerObservationAndProduct(@TempDir Path dir) throws Exception {
        Path record = dir.resolve("runs/cruise.matchup.json");
        MatchupRun run = MatchupRun.create(cruise(dir), settings(MODISA, OTHER), record);

        assertEquals(8, run.tasks().size());
        assertEquals(List.of(MatchupRun.State.PENDING, MatchupRun.State.PENDING, MatchupRun.State.SKIPPED,
                MatchupRun.State.PENDING), states(run).subList(0, 4));
        assertTrue(run.tasks().get(2).message().startsWith("Latitude missing"), run.tasks().get(2).message());
        assertTrue(Files.exists(record));
        JSONObject json = new JSONObject(Files.readString(record));
        assertEquals(run.label(), json.getString("label"));
        assertTrue(run.label().startsWith("seadas-matchup-"));
        assertEquals(64, json.getString("seabassSha256").length());

        assertThrows(IOException.class, () -> MatchupRun.create(cruise(dir), settings(MODISA), record),
                "an existing record is never overwritten");
    }

    @Test
    void aRunDownloadsEachResultAndJoinsThem(@TempDir Path dir) throws Exception {
        FakeHarmony harmony = new FakeHarmony();
        MatchupRun run = MatchupRun.create(cruise(dir), settings(MODISA), dir.resolve("cruise.matchup.json"));
        List<MatchupRun.Task> changes = new ArrayList<>();
        AtomicInteger lastSettled = new AtomicInteger();

        run.execute(harmony.client(), () -> false, new MatchupRun.Listener() {
            public void changed(MatchupRun.Task task) {
                changes.add(task);
            }

            public void progress(int settled, int total) {
                assertEquals(4, total);
                lastSettled.set(settled);
            }
        });

        assertTrue(run.finished());
        assertEquals(4, lastSettled.get());
        assertEquals(Map.of(MatchupRun.State.DONE, 3, MatchupRun.State.SKIPPED, 1, MatchupRun.State.PENDING, 0,
                MatchupRun.State.SUBMITTED, 0, MatchupRun.State.FAILED, 0), run.counts());
        MatchupRun.Task first = run.tasks().get(0);
        assertEquals("successful", first.status());
        assertEquals(List.of(first.jobId() + "/G.csv"), first.files());
        assertTrue(Files.exists(run.resultsFolder().resolve(first.files().get(0))));
        assertTrue(changes.stream().anyMatch(t -> t.state() == MatchupRun.State.SUBMITTED));
        assertTrue(harmony.calls("GET", "/" + MODISA).stream()
                .allMatch(c -> c.uri().getRawQuery().contains("label=" + run.label())));

        MatchupProduct modis = new MatchupProduct(MODISA, "MODISA_L2_OC", Map.of("chlor_a", "mg m^-3"));
        ResultJoiner.Joined joined = run.join(Map.of(MODISA, modis), ResultJoiner.Rows.ALL);
        assertTrue(joined.warnings().isEmpty(), joined.warnings().toString());
        List<String> out = joined.writer().lines();
        assertTrue(out.get(3).endsWith(",modis_aqua_l2fname,modis_aqua_tdiff,modis_aqua_rrsaot_cv,"
                + "modis_aqua_chlor_a,modis_aqua_chlor_a_sd,matchup_found"), out.get(3));
        assertTrue(out.get(4).endsWith(",none,seconds,unitless,mg_m^-3,mg_m^-3,none"), out.get(4));
        int end = out.indexOf("/end_header");
        assertTrue(out.get(end + 1).endsWith(",0.407,AQUA_MODIS.20240520T191501.L2.OC.nc,60.0,0.05,0.384000,"
                + "0.012000,yes"), out.get(end + 1));
        assertTrue(out.get(end + 3).endsWith(",0.124,-9999,-9999,-9999,-9999,-9999,no"), out.get(end + 3));
        assertTrue(out.get(end - 2).contains("Harmony jobs labelled " + run.label()), out.get(end - 2));
    }

    @Test
    void aStoppedRunCancelsItsJobsAndResumesLater(@TempDir Path dir) throws Exception {
        FakeHarmony harmony = new FakeHarmony();
        harmony.pollsPerJob = 100;
        Path record = dir.resolve("cruise.matchup.json");
        MatchupRun run = MatchupRun.create(cruise(dir), settings(MODISA), record);
        AtomicInteger polls = new AtomicInteger();

        run.execute(harmony.client(), () -> polls.get() >= 2, new MatchupRun.Listener() {
            public void progress(int settled, int total) {
                polls.incrementAndGet();
            }
        });

        assertFalse(run.finished());
        assertEquals(2, harmony.calls.stream().filter(c -> c.path().endsWith("/cancel")).count());
        assertTrue(run.tasks().stream().filter(t -> t.state() != MatchupRun.State.SKIPPED)
                .allMatch(t -> t.state() == MatchupRun.State.PENDING && t.jobId() == null));

        harmony.pollsPerJob = 1;
        MatchupRun resumed = MatchupRun.load(record);
        resumed.execute(harmony.client(), () -> false, new MatchupRun.Listener() {
        });
        assertTrue(resumed.finished());
        assertEquals(3, resumed.counts().get(MatchupRun.State.DONE));
    }

    @Test
    void anExpiredTokenLeavesARecordThatResumesWithoutResubmitting(@TempDir Path dir) throws Exception {
        FakeHarmony harmony = new FakeHarmony();
        Path record = dir.resolve("cruise.matchup.json");
        MatchupRun run = MatchupRun.create(cruise(dir), settings(MODISA), record);
        harmony.queue("/jobs/status", call -> reply(401, "{\"description\":\"Token expired\"}"));

        HarmonyException e = assertThrows(HarmonyException.class,
                () -> run.execute(harmony.client(), () -> false, new MatchupRun.Listener() {
                }));
        assertTrue(e.unauthorized());
        List<MatchupRun.Task> saved = MatchupRun.load(record).tasks();
        assertEquals(2, saved.stream().filter(t -> t.state() == MatchupRun.State.SUBMITTED).count());
        assertTrue(saved.stream().filter(t -> t.state() == MatchupRun.State.SUBMITTED).allMatch(t -> t.jobId() != null));
        int submits = harmony.calls("GET", "/" + MODISA).size();

        MatchupRun resumed = MatchupRun.load(record);
        resumed.execute(harmony.client(), () -> false, new MatchupRun.Listener() {
        });
        assertTrue(resumed.finished());
        assertEquals(submits + 1, harmony.calls("GET", "/" + MODISA).size(), "only the never-submitted one");
    }

    @Test
    void failedJobsAndRefusedRequestsCanBeRetried(@TempDir Path dir) throws Exception {
        FakeHarmony harmony = new FakeHarmony();
        harmony.queue("/" + MODISA, call -> reply(422, "{\"description\":\"Error: no granules found\"}"));
        MatchupRun run = MatchupRun.create(cruise(dir), settings(MODISA), dir.resolve("r.json"));
        harmony.pollsPerJob = 0;
        HarmonyJobClient client = harmony.client();

        run.execute(client, () -> false, new MatchupRun.Listener() {
            public void changed(MatchupRun.Task task) {
                if (task.state() == MatchupRun.State.SUBMITTED && task.status() == null) {
                    harmony.finalStatus.put(task.jobId(), "failed");
                }
            }
        });

        assertEquals(3, run.counts().get(MatchupRun.State.FAILED));
        MatchupRun.Task refused = run.tasks().get(0);
        assertNull(refused.jobId());
        assertEquals("no granules found", refused.message());
        MatchupRun.Task failed = run.tasks().get(1);
        assertNotNull(failed.jobId());
        assertEquals("failed", failed.status());
        assertEquals("msg", failed.message());

        run.retryFailed();
        harmony.finalStatus.clear();
        run.execute(client, () -> false, new MatchupRun.Listener() {
        });
        assertEquals(3, run.counts().get(MatchupRun.State.DONE));
    }

    @Test
    void aChangedSeabassFileCantResumeARun(@TempDir Path dir) throws Exception {
        Path sb = cruise(dir);
        Path record = dir.resolve("r.json");
        MatchupRun.create(sb, settings(MODISA), record);
        Files.writeString(sb, Files.readString(sb).replace("0.407", "0.408"));

        IOException e = assertThrows(IOException.class, () -> MatchupRun.load(record));
        assertTrue(e.getMessage().contains("has changed"), e.getMessage());

        Files.writeString(record, "{not json");
        assertThrows(IOException.class, () -> MatchupRun.load(record));
    }

    @Test
    void theRecordRoundTrips(@TempDir Path dir) throws Exception {
        FakeHarmony harmony = new FakeHarmony();
        Path record = dir.resolve("r.json");
        MatchupRun run = MatchupRun.create(cruise(dir), new MatchupRun.Settings(HarmonyEnvironment.UAT,
                List.of(MODISA), 0.25, Duration.ofMinutes(30), List.of("chlor_a"), 3), record);
        run.execute(harmony.client(), () -> false, new MatchupRun.Listener() {
        });

        MatchupRun loaded = MatchupRun.load(record);
        assertEquals(run.tasks(), loaded.tasks());
        assertEquals(run.settings(), loaded.settings());
        assertEquals(run.label(), loaded.label());
        assertFalse(Files.exists(record.resolveSibling("r.json.tmp")));
        assertTrue(harmony.calls("GET", "/" + MODISA).get(0).path().contains("/collections/chlor_a/"));
    }

    @Test
    void theCoverageCheckSkipsObservationsWithNoOverpass(@TempDir Path dir) throws Exception {
        Path record = dir.resolve("r.json");
        MatchupRun run = MatchupRun.create(cruise(dir), settings(MODISA, OTHER), record);
        // OTHER has no granules anywhere; MODISA none for row 2, and CMR fails for row 4.
        CoverageCheckerTest.FakeCmr cmr = new CoverageCheckerTest.FakeCmr(uri -> {
            String query = java.net.URLDecoder.decode(uri.getRawQuery(), java.nio.charset.StandardCharsets.UTF_8);
            if (query.contains(OTHER) || query.contains("bounding_box=-85.01")) {
                return CoverageCheckerTest.FakeCmr.hits(0);
            }
            if (query.contains("bounding_box=-83.3018")) {
                return CoverageCheckerTest.FakeCmr.error(400, "{\"errors\":[\"bad box\"]}");
            }
            return CoverageCheckerTest.FakeCmr.hits(1);
        });

        MatchupRun.Coverage coverage = run.checkCoverage(new CoverageChecker(cmr, FakeHarmony.ROOT, () -> null,
                d -> {
                }, 2), () -> false, (checked, total) -> assertEquals(6, total));

        assertEquals(new MatchupRun.Coverage(3, 1, 1, 1, 2, List.of("CMR granule search: HTTP 400: bad box"), false),
                coverage);
        List<MatchupRun.Task> saved = MatchupRun.load(record).tasks();
        assertEquals(List.of(MatchupRun.State.PENDING, MatchupRun.State.SKIPPED, MatchupRun.State.SKIPPED,
                MatchupRun.State.PENDING), saved.subList(0, 4).stream().map(MatchupRun.Task::state).toList());
        assertEquals(MatchupRun.NO_OVERPASS, saved.get(1).message());
        assertTrue(saved.get(2).message().startsWith("Latitude missing"), "an earlier reason is kept");
        assertTrue(saved.subList(4, 8).stream().allMatch(t -> t.state() == MatchupRun.State.SKIPPED));

        FakeHarmony harmony = new FakeHarmony();
        run.execute(harmony.client(), () -> false, new MatchupRun.Listener() {
        });
        assertEquals(2, harmony.calls("GET", "/" + MODISA).size());
        assertEquals(0, harmony.calls("GET", "/" + OTHER).size());
    }
}
