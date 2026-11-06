package gov.nasa.gsfc.seadas.earthdatacloud.matchup;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MatchupPairsTest {

    private static final String COLLECTION = "C1272944705-OB_CLOUD";

    private static SeabassFile file() throws Exception {
        return SeabassFile.parse(List.of(
                "/begin_header",
                "/missing=-9999",
                "/delimiter=comma",
                "/fields=date,time,lat,lon,chl,Rrs443,wt",
                "/units=yyyymmdd,hh:mm:ss,degrees,degrees,mg/m^3,1/sr,degreesC",
                "/end_header",
                "20101010,18:07:30,33.0,-119.5,0.5,0.004,18.2",
                "20101010,18:07:30,34.0,-119.5,2.0,0.003,17.9",
                "20101010,18:07:30,35.0,-119.5,-9999,0.002,17.5",
                "20101010,18:07:30,36.0,-119.5,8.0,0.001,17.0"), Path.of("test.sb"));
    }

    private static MatchupResults.Item item(int row, MatchupResults.Outcome outcome, Map<String, String> chosen) {
        MatchupRun.Task task = new MatchupRun.Task(row, COLLECTION, MatchupRun.State.DONE, "job", "successful", null,
                List.of(), 1);
        return new MatchupResults.Item(task, null, outcome, chosen, null);
    }

    @Test
    void variablesArePairedWithTheirSeabassFields() throws Exception {
        List<MatchupResults.Item> items = List.of(item(0, MatchupResults.Outcome.MATCHED,
                Map.of("sat_chlor_a_mean", "0.6", "sat_Rrs_443_mean", "0.004", "sat_aot_869_mean", "0.1")));
        List<MatchupPairs.Variable> variables = MatchupPairs.variables(items, file());
        assertEquals(2, variables.size());
        assertEquals(List.of("chl", "rrs443"), variables.stream().map(MatchupPairs.Variable::inSitu).sorted().toList());
    }

    @Test
    void pointsAreMatchedResultsWithBothValuesAndRejectedOnlyWhenAsked() throws Exception {
        MatchupPairs.Variable chl = new MatchupPairs.Variable("chlor_a", "chl");
        List<MatchupResults.Item> items = List.of(
                item(0, MatchupResults.Outcome.MATCHED, Map.of("sat_chlor_a_mean", "0.6")),
                item(1, MatchupResults.Outcome.REJECTED, Map.of("sat_chlor_a_mean", "2.5")),
                item(2, MatchupResults.Outcome.MATCHED, Map.of("sat_chlor_a_mean", "1.0")),   // in situ missing
                item(3, MatchupResults.Outcome.MATCHED, Map.of("sat_chlor_a_mean", "")),      // no satellite value
                item(3, MatchupResults.Outcome.NO_OVERPASS, null));
        assertEquals(1, MatchupPairs.points(items, file(), chl, false).size());
        List<MatchupPairs.Point> all = MatchupPairs.points(items, file(), chl, true);
        assertEquals(2, all.size());
        assertEquals(2.0, all.get(1).inSitu());
        assertEquals(false, all.get(1).matched());
    }

    @Test
    void statsOnValuesAndOnLog10() {
        // y = 2x exactly: slope 2, r2 1; bias mean(y - x) = mean(x) = 2.
        List<MatchupPairs.Point> points = List.of(
                new MatchupPairs.Point(null, 1, 2, true),
                new MatchupPairs.Point(null, 2, 4, true),
                new MatchupPairs.Point(null, 3, 6, true));
        MatchupPairs.Stats linear = MatchupPairs.stats(points, false);
        assertEquals(3, linear.n());
        assertEquals(2.0, linear.slope(), 1e-12);
        assertEquals(0.0, linear.intercept(), 1e-12);
        assertEquals(1.0, linear.r2(), 1e-12);
        assertEquals(2.0, linear.bias(), 1e-12);
        assertEquals(2.0, linear.mae(), 1e-12);
        assertEquals(Math.sqrt((1 + 4 + 9) / 3.0), linear.rmse(), 1e-12);
        // In log10 a factor of 2 everywhere is a bias of log10(2), slope 1.
        MatchupPairs.Stats log = MatchupPairs.stats(points, true);
        assertEquals(Math.log10(2), log.bias(), 1e-12);
        assertEquals(1.0, log.slope(), 1e-12);
    }

    @Test
    void tooFewPointsHaveNoStats() {
        assertNull(MatchupPairs.stats(List.of(new MatchupPairs.Point(null, 1, 2, true)), false));
        // In log10, values at or below 0 are left out.
        assertNull(MatchupPairs.stats(List.of(new MatchupPairs.Point(null, 1, 2, true),
                new MatchupPairs.Point(null, -1, 2, true)), true));
    }
}
