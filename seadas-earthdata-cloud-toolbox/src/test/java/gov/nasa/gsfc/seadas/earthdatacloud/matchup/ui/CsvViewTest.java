package gov.nasa.gsfc.seadas.earthdatacloud.matchup.ui;

import gov.nasa.gsfc.seadas.earthdatacloud.matchup.MatchupResults;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CsvViewTest {

    @Test
    void columnsEmptyInEveryShownRowAreLeftOut() {
        MatchupResults.Csv csv = new MatchupResults.Csv(List.of("seabass_row", "sat_chlor_a_mean", "sat_sst_mean"),
                List.of(List.of("1", "0.31", ""), List.of("2", "", "")));
        MatchupResults.Csv shown = CsvView.withoutEmptyColumns(csv);
        assertEquals(List.of("seabass_row", "sat_chlor_a_mean"), shown.header());
        assertEquals(List.of(List.of("1", "0.31"), List.of("2", "")), shown.rows());
    }

    @Test
    void numbersSortByValueBeforeTextAndEmpty() {
        List<String> values = new ArrayList<>(List.of("", "matched", "10", "-143.0", "9.5"));
        values.sort(CsvView.NUMBERS_FIRST);
        assertEquals(List.of("-143.0", "9.5", "10", "matched", ""), values);
    }
}
