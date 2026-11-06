package gov.nasa.gsfc.seadas.earthdatacloud.matchup.ui;

import gov.nasa.gsfc.seadas.earthdatacloud.matchup.MatchupResults;
import gov.nasa.gsfc.seadas.earthdatacloud.matchup.MatchupRun;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResultsStepTest {

    private static MatchupResults.Item item(String collection, MatchupResults.Outcome outcome,
                                            Map<String, String> chosen) {
        MatchupRun.Task task = new MatchupRun.Task(1, collection, MatchupRun.State.DONE, "job", "successful", null,
                List.of(), 1);
        return new MatchupResults.Item(task, null, outcome, chosen, null);
    }

    @Test
    void aRowsGranuleIsNamedWithItsRequestsProduct() {
        // The service's rows have the granule but no product column (UAT MODIS Terra ocean color here).
        Map<String, String> row = Map.of("granule", "TERRA_MODIS.20101010T180501.L2.OC.nc", "matchup_status", "matched");
        List<MatchupResults.Item> items = List.of(
                item("C1272944705-OB_CLOUD", MatchupResults.Outcome.MATCHED, row),
                item("C1272944705-OB_CLOUD", MatchupResults.Outcome.NO_OVERPASS, null),
                item("C0-UNKNOWN", MatchupResults.Outcome.MATCHED, Map.of("granule", "X.nc")));
        assertEquals(Map.of("TERRA_MODIS.20101010T180501.L2.OC.nc", "MODIST_L2_OC"), ResultsStep.granules(items));
    }
}
