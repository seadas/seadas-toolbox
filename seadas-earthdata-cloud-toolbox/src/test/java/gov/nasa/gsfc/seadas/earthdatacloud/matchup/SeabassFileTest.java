package gov.nasa.gsfc.seadas.earthdatacloud.matchup;

import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeabassFileTest {

    static Path resource(String name) {
        try {
            return Path.of(SeabassFileTest.class.getResource(name).toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    static SeabassFile parse(String... lines) throws SeabassFormatException {
        return SeabassFile.parse(List.of(lines), null);
    }

    @Test
    void readsCommaFileWithCommentsAndHistory() throws Exception {
        SeabassFile file = SeabassFile.read(resource("VIIRS_2024_HPLC_NASA_R1.sb"));

        assertEquals(SeabassFile.Delimiter.COMMA, file.delimiter());
        assertEquals("-9999", file.missingText());
        assertEquals(99, file.rowCount());
        assertEquals(37, file.fields().size());
        assertEquals(37, file.units().size());
        assertTrue(file.hasField("Tot_Chl_a"), "field lookup ignores case");
        assertEquals("tot_chl_a", file.fields().get(10));
        assertEquals("comma", file.headers().get("delimiter"));
        assertEquals("19:30:00[GMT]", file.headers().get("start_time"));
        assertEquals(8, file.headerLines().stream().filter(l -> l.startsWith("!")).count());
        assertEquals("13-2586", file.value(0, "hplc_gsfc_id"));
        assertEquals("12.0", file.value(0, "bottle"), "values keep their text");
        assertTrue(file.isMissing(file.value(2, "bottle")), "-9999.0 is the missing value -9999");
        assertTrue(file.warnings().isEmpty());
    }

    @Test
    void readsSpaceDelimitedFile() throws Exception {
        SeabassFile file = SeabassFile.read(resource("BENCALTChla.csv.sb"));

        assertEquals(SeabassFile.Delimiter.SPACE, file.delimiter());
        assertEquals(146, file.rowCount());
        assertEquals("-32.6590", file.value(0, "lat"));
        assertEquals("05", file.value(0, "day"));
    }

    @Test
    void missingRequiredHeadersAreErrors() {
        assertThrows(SeabassFormatException.class, () -> parse(
                "/begin_header", "/delimiter=comma", "/fields=lat,lon", "/end_header", "1,2"));
        assertThrows(SeabassFormatException.class, () -> parse(
                "/begin_header", "/missing=-9999", "/fields=lat,lon", "/end_header", "1,2"));
        assertThrows(SeabassFormatException.class, () -> parse(
                "/begin_header", "/missing=-9999", "/delimiter=comma", "/end_header", "1,2"));
        assertThrows(SeabassFormatException.class, () -> parse(
                "/begin_header", "/missing=-9999", "/delimiter=comma", "/fields=lat,lon", "1,2"));
        assertThrows(SeabassFormatException.class, () -> parse(
                "/begin_header", "/missing=n/a", "/delimiter=comma", "/fields=lat,lon", "/end_header"));
        assertThrows(SeabassFormatException.class, () -> parse(
                "/begin_header", "/missing=-9999", "/delimiter=semicolon", "/fields=lat,lon", "/end_header"));
    }

    @Test
    void badLinesNameTheLine() {
        SeabassFormatException header = assertThrows(SeabassFormatException.class, () -> parse(
                "/begin_header", "/missing=-9999", "not a header", "/end_header"));
        assertTrue(header.getMessage().contains("line 3"), header.getMessage());

        SeabassFormatException data = assertThrows(SeabassFormatException.class, () -> parse(
                "/begin_header", "/missing=-9999", "/delimiter=comma", "/fields=lat,lon,chl",
                "/end_header", "1,2,3", "1,2"));
        assertTrue(data.getMessage().contains("line 7") && data.getMessage().contains("2 values for 3 fields"),
                data.getMessage());

        assertThrows(SeabassFormatException.class, () -> parse(
                "/begin_header", "/missing=-9999", "/delimiter=comma", "/fields=lat,lon",
                "/end_header", "1,2,3"));
        assertThrows(SeabassFormatException.class, () -> parse(
                "/begin_header", "/missing=-9999", "/delimiter=comma", "/fields=lat,LAT", "/end_header"));
    }

    @Test
    void toleratesTrailingDelimitersBlankLinesAndMissingUnits() throws Exception {
        SeabassFile file = parse("/begin_header", "/missing=-9999", "/delimiter=comma",
                "/fields=lat,lon", "", "/end_header", "1,2,", "", "3,4");

        assertEquals(2, file.rowCount());
        assertEquals("4", file.value(1, "lon"));
        assertEquals(7, file.lineNumber(0));
        assertEquals(9, file.lineNumber(1));
        assertEquals(List.of("No /units header"), file.warnings());
    }

    @Test
    void observationsFromTheHplcFile() throws Exception {
        SeabassFile file = SeabassFile.read(resource("VIIRS_2024_HPLC_NASA_R1.sb"));
        List<SeabassObservation> obs = SeabassObservations.of(file);

        assertEquals("date/time", SeabassObservations.timeSource(file));
        assertEquals(99, obs.size());
        SeabassObservation first = obs.get(0);
        assertTrue(first.usable());
        assertEquals(LocalDateTime.of(2024, 5, 20, 19, 30, 0), first.time());
        assertEquals(29.9702, first.lat());
        assertEquals(-87.1583, first.lon());
        assertNull(first.note());
        assertTrue(obs.stream().allMatch(SeabassObservation::usable));
    }

    @Test
    void observationsFromYearMonthDayFields() throws Exception {
        SeabassFile file = SeabassFile.read(resource("BENCALTChla.csv.sb"));
        SeabassObservation first = SeabassObservations.of(file).get(0);

        assertEquals("year/month/day/hour/minute/second", SeabassObservations.timeSource(file));
        assertEquals(LocalDateTime.of(2002, 10, 5, 14, 5, 0), first.time());
        assertEquals(-32.659, first.lat());
        assertEquals(18.0672, first.lon());
    }

    @Test
    void mixedCaseFieldsAreFound() throws Exception {
        SeabassFile file = SeabassFile.read(resource("EXPORTS_EXPORTSNA_DY131_HyperSAS_20210501_080000_L2_Rrs_R1.sb"));
        List<SeabassObservation> obs = SeabassObservations.of(file);

        assertEquals(11, obs.size());
        assertEquals(LocalDateTime.of(2021, 5, 1, 8, 2, 46), obs.get(0).time());
        assertTrue(file.hasField("rrs353.2"));
    }

    @Test
    void aProfileWithoutPositionHasNoUsableObservations() throws Exception {
        SeabassFile file = SeabassFile.read(resource("VIIRS_2024_ag_st_12_77m_R4.sb"));
        List<SeabassObservation> obs = SeabassObservations.of(file);

        assertEquals("/start_date and /start_time", SeabassObservations.timeSource(file));
        assertEquals(LocalDateTime.of(2024, 5, 24, 16, 57, 0), obs.get(0).time());
        assertFalse(obs.get(0).usable());
        assertTrue(obs.get(0).problem().startsWith("No position"), obs.get(0).problem());
    }

    @Test
    void badRowsGetAProblemAndTheRestStayUsable() throws Exception {
        SeabassFile file = parse("/begin_header", "/missing=-9999", "/delimiter=comma",
                "/fields=date,time,lat,lon", "/end_header",
                "20240520,14:02:00,29.158,-78.995",
                "-9999,15:40:00,29.201,-78.951",
                "20240520,25:00:00,29.201,-78.951",
                "20240521,18:02:00,92.601,-78.535",
                "20240521,18:02:00,29.601,-9999",
                "20240521,18:02:00,29.601,190.0",
                "20241301,18:02:00,29.601,-78.5");
        List<SeabassObservation> obs = SeabassObservations.of(file);

        assertTrue(obs.get(0).usable());
        assertEquals("Date-time can't be read from date/time: date is missing", obs.get(1).problem());
        assertTrue(obs.get(2).problem().startsWith("Date-time can't be read"), obs.get(2).problem());
        assertEquals("Latitude out of range: 92.601", obs.get(3).problem());
        assertEquals("Longitude missing or not a number: -9999", obs.get(4).problem());
        assertTrue(obs.get(5).problem().startsWith("Longitude out of range"), obs.get(5).problem());
        assertTrue(obs.get(6).problem().startsWith("Date-time can't be read"), obs.get(6).problem());
        assertEquals(6, obs.get(4).lineNumber() - 4);
    }

    private static LocalDateTime timeOf(String fields, String row, String... extraHeaders) throws Exception {
        String[] lines = new String[5 + extraHeaders.length];
        lines[0] = "/begin_header";
        lines[1] = "/missing=-9999";
        lines[2] = "/delimiter=comma";
        System.arraycopy(extraHeaders, 0, lines, 3, extraHeaders.length);
        lines[3 + extraHeaders.length] = "/fields=" + fields + ",lat,lon";
        lines[4 + extraHeaders.length] = "/end_header";
        String[] all = new String[lines.length + 1];
        System.arraycopy(lines, 0, all, 0, lines.length);
        all[lines.length] = row + ",10.0,20.0";
        SeabassObservation obs = SeabassObservations.of(parse(all)).get(0);
        assertTrue(obs.usable(), String.valueOf(obs.problem()));
        return obs.time();
    }

    @Test
    void everyDateTimeCombination() throws Exception {
        LocalDateTime t = LocalDateTime.of(2024, 5, 20, 14, 2, 3);
        assertEquals(t, timeOf("date,time", "20240520,14:02:03"));
        assertEquals(t, timeOf("year,month,day,hour,minute,second", "2024,5,20,14,2,3"));
        assertEquals(t, timeOf("year,month,day,time", "2024,05,20,14:02:03"));
        assertEquals(t, timeOf("date,hour,minute,second", "20240520,14,2,3"));
        assertEquals(t, timeOf("date_time", "2024-05-20 14:02:03"));
        assertEquals(t, timeOf("year,sdy,hour,minute,second", "2024,141,14,2,3"));
        assertEquals(t, timeOf("year,sdy,time", "2024,141,14:02:03"));
        assertEquals(t, timeOf("time", "14:02:03", "/start_date=20240520"));
        assertEquals(t, timeOf("hour,minute,second", "14,2,3", "/start_date=20240520"));
        assertEquals(t.withSecond(0), timeOf("year,month,day,hour,minute", "2024,5,20,14,2"));
        assertEquals(t.withSecond(0), timeOf("date,hour,minute", "20240520,14,2"));
        assertEquals(t.withSecond(0), timeOf("year,sdy,hour,minute", "2024,141,14,2"));
        assertEquals(t.withMinute(0).withSecond(0), timeOf("year,month,day,hour", "2024,5,20,14"));
        assertEquals(t.withMinute(0).withSecond(0), timeOf("date,hour", "20240520,14"));
        assertEquals(t.withMinute(0).withSecond(0), timeOf("year,sdy,hour", "2024,141,14"));
        assertEquals(t.toLocalDate().atStartOfDay(), timeOf("year,month,day", "2024,5,20"));
        assertEquals(t.toLocalDate().atStartOfDay(), timeOf("date", "20240520"));
        assertEquals(t.toLocalDate().atStartOfDay(), timeOf("year,sdy", "2024,141"));
        assertEquals(t, timeOf("station", "7", "/start_date=20240520", "/start_time=14:02:03[GMT]"));
        assertEquals(t.toLocalDate().atStartOfDay(), timeOf("station", "7", "/start_date=20240520"));
        assertEquals(t, timeOf("year,month,day,hour,minute,second", "2024.0,5.0,20,14,2,3.0"),
                "whole numbers written as decimals, as Python's int() reads them");
    }

    @Test
    void theScriptsOrderDecidesBetweenCombinations() throws Exception {
        assertEquals(LocalDateTime.of(2024, 5, 20, 14, 2, 3),
                timeOf("year,month,day,hour,minute,second,date,time", "1999,1,1,0,0,0,20240520,14:02:03"));
        assertEquals(LocalDateTime.of(2024, 5, 20, 14, 2, 3),
                timeOf("time,hour,minute,second", "14:02:03,1,1,1", "/start_date=20240520"));
    }

    @Test
    void aMissingHourIsNoted() throws Exception {
        SeabassObservation obs = SeabassObservations.of(parse("/begin_header", "/missing=-9999",
                "/delimiter=comma", "/fields=date,lat,lon", "/end_header", "20240520,10,20")).get(0);

        assertTrue(obs.usable());
        assertEquals("No time of day in the file: 00:00 UTC assumed", obs.note());
    }

    @Test
    void noDateTimeFields() throws Exception {
        SeabassObservation obs = SeabassObservations.of(parse("/begin_header", "/missing=-9999",
                "/delimiter=comma", "/fields=lat,lon", "/end_header", "10,20")).get(0);

        assertFalse(obs.usable());
        assertTrue(obs.problem().startsWith("No date and time"), obs.problem());
    }
}
