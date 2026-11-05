package gov.nasa.gsfc.seadas.earthdatacloud.matchup;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static gov.nasa.gsfc.seadas.earthdatacloud.matchup.SeabassFileTest.parse;
import static gov.nasa.gsfc.seadas.earthdatacloud.matchup.SeabassFileTest.resource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The joiner on the CSVs the service returned in UAT for the sprint-review demo observations. */
class ResultJoinerTest {

    private static final String VIIRSN = "C1272968279-OB_CLOUD";
    private static final String PACE = "C1281448693-OB_CLOUD";
    private static final String MODIST = "C1272944705-OB_CLOUD";
    private static final ResultJoiner.RunInfo RUN = new ResultJoiner.RunInfo(
            Instant.parse("2026-11-04T21:00:00Z"), URI.create("https://harmony.uat.earthdata.nasa.gov"),
            List.of(" Run record: cruise.matchup.json"));

    /** The demo observations, plus one with no granule and one that can't be matched. */
    private static SeabassFile cruise() throws SeabassFormatException {
        return parse("/begin_header", "/investigators=Test", "/missing=-9999", "/delimiter=comma",
                "/fields=station,date,time,lat,lon,chl",
                "/units=none,yyyymmdd,hh:mm:ss,degrees,degrees,mg/m^3",
                "! demo stations", "/end_header",
                "1,20170406,09:33:00,42.925,40.537,1.20",
                "2,20101010,18:07:30,33.0,-119.5,0.31",
                "3,20260603,11:35:00,55.914,19.015,2.10",
                "4,20260603,11:35:00,10.0,-30.0,0.05",
                "5,20260603,11:35:00,-9999,-30.0,0.05");
    }

    private static MatchupProduct product(String collection, String shortName) throws IOException {
        JSONObject variables = new JSONObject(Files.readString(resource("cmr-variables-" + collection + ".json")));
        return new MatchupProduct(collection, shortName, MatchupProduct.unitsFrom(variables));
    }

    private static MatchupCsv csv(String name) throws IOException {
        return MatchupCsv.read(resource("csv/" + name));
    }

    private static ResultJoiner.ProductRows rows(MatchupProduct product, Object... rowAndCsvs) {
        Map<Integer, List<MatchupCsv>> csvs = new LinkedHashMap<>();
        for (int i = 0; i < rowAndCsvs.length; i += 2) {
            csvs.computeIfAbsent((Integer) rowAndCsvs[i], k -> new ArrayList<>()).add((MatchupCsv) rowAndCsvs[i + 1]);
        }
        return new ResultJoiner.ProductRows(product, csvs);
    }

    private static ResultJoiner.ProductRows viirs() throws IOException {
        return rows(product(VIIRSN, "VIIRSN_L2_OC"), 0, csv("SNPP_VIIRS.20170406T093001.L2.OC.csv"));
    }

    private static ResultJoiner.ProductRows pace(String... granules) throws IOException {
        Object[] pairs = new Object[granules.length * 2];
        for (int i = 0; i < granules.length; i++) {
            pairs[2 * i] = 2;
            pairs[2 * i + 1] = csv("baltic/" + granules[i]);
        }
        return rows(product(PACE, "PACE_OCI_L2_AOP_NRT"), pairs);
    }

    private static List<String> list(List<String> out, String header) {
        String line = out.stream().filter(l -> l.startsWith(header)).findFirst().orElseThrow();
        return Arrays.asList(line.substring(header.length()).split(",", -1));
    }

    private static Map<String, String> row(List<String> out, int dataRow) {
        List<String> fields = list(out, "/fields=");
        List<String> values = Arrays.asList(out.get(out.indexOf("/end_header") + 1 + dataRow).split(",", -1));
        assertEquals(fields.size(), values.size());
        Map<String, String> row = new LinkedHashMap<>();
        for (int i = 0; i < fields.size(); i++) {
            row.put(fields.get(i), values.get(i));
        }
        return row;
    }

    @Test
    void writesTheScriptsColumnsWithCmrUnits() throws Exception {
        SeabassFile file = cruise();
        ResultJoiner.Joined joined = ResultJoiner.join(file, SeabassObservations.of(file), List.of(viirs()),
                ResultJoiner.Rows.ALL, RUN);
        List<String> out = joined.writer().lines();
        List<String> fields = list(out, "/fields=");
        List<String> units = list(out, "/units=");

        assertEquals(List.of("station", "date", "time", "lat", "lon", "chl",
                "viirs_suomi-npp_l2fname", "viirs_suomi-npp_tdiff", "viirs_suomi-npp_rrsaot_cv",
                "viirs_suomi-npp_aot_862", "viirs_suomi-npp_aot_862_sd",
                "viirs_suomi-npp_angstrom", "viirs_suomi-npp_angstrom_sd",
                "viirs_suomi-npp_rrs_410", "viirs_suomi-npp_rrs_410_sd"), fields.subList(0, 15));
        assertEquals("matchup_found", fields.get(fields.size() - 1));
        assertEquals(fields.size(), units.size());
        Map<String, String> unitOf = new LinkedHashMap<>();
        for (int i = 0; i < fields.size(); i++) {
            unitOf.put(fields.get(i), units.get(i));
        }
        assertEquals("none", unitOf.get("viirs_suomi-npp_l2fname"));
        assertEquals("seconds", unitOf.get("viirs_suomi-npp_tdiff"));
        assertEquals("unitless", unitOf.get("viirs_suomi-npp_rrsaot_cv"));
        assertEquals("none", unitOf.get("viirs_suomi-npp_aot_862"), "no units in the granule");
        assertEquals("sr^-1", unitOf.get("viirs_suomi-npp_rrs_443_sd"));
        assertEquals("mg_m^-3", unitOf.get("viirs_suomi-npp_chlor_a"));
        assertEquals("einstein_m^-2_day^-1", unitOf.get("viirs_suomi-npp_par"));
        assertTrue(fields.stream().noneMatch(f -> f.contains("valid_pixel_count")), "the script writes no counts");

        Map<String, String> matched = row(out, 0);
        assertEquals("SNPP_VIIRS.20170406T093001.L2.OC.nc", matched.get("viirs_suomi-npp_l2fname"));
        assertEquals("109.0", matched.get("viirs_suomi-npp_tdiff"));
        assertEquals("0.04508071885470606", matched.get("viirs_suomi-npp_rrsaot_cv"));
        assertEquals("1.334571", matched.get("viirs_suomi-npp_chlor_a"), "the service's text, as the script writes it");
        assertEquals("0.033018", matched.get("viirs_suomi-npp_chlor_a_sd"));
        assertEquals("yes", matched.get("matchup_found"));
        assertEquals("1.20", matched.get("chl"), "in situ values untouched");

        Map<String, String> none = row(out, 3);
        assertEquals("-9999", none.get("viirs_suomi-npp_l2fname"));
        assertEquals("no", none.get("matchup_found"));
        assertEquals("no", row(out, 4).get("matchup_found"), "an observation that couldn't be matched");
        assertTrue(joined.warnings().isEmpty(), joined.warnings().toString());
    }

    @Test
    void thePaceBandsGetTheirOwnColumns() throws Exception {
        SeabassFile file = cruise();
        ResultJoiner.Joined joined = ResultJoiner.join(file, SeabassObservations.of(file),
                List.of(pace("PACE_OCI.20260603T113224.L2.OC_AOP.V3_2.NRT.csv")), ResultJoiner.Rows.ALL, RUN);
        List<String> out = joined.writer().lines();
        List<String> fields = list(out, "/fields=");
        List<String> units = list(out, "/units=");
        MatchupCsv service = csv("baltic/PACE_OCI.20260603T113224.L2.OC_AOP.V3_2.NRT.csv");
        long bands = service.header().stream().filter(h -> h.endsWith("_unc")).count();

        assertEquals(6 + 3 + 4 * 2 + bands * 3 + 1, fields.size());
        int i = fields.indexOf("oci_pace_rrs_442.3");
        assertTrue(i > 0, fields.toString());
        assertEquals(List.of("oci_pace_rrs_442.3", "oci_pace_rrs_442.3_sd", "oci_pace_rrs_442.3_unc"),
                fields.subList(i, i + 3));
        assertEquals(List.of("sr^-1", "sr^-1", "sr^-1"), units.subList(i, i + 3));
        assertEquals("w_m^-2_um^-1_sr^-1", units.get(fields.indexOf("oci_pace_nflh")), "lower-cased, as the script");
        assertEquals("nm", units.get(fields.indexOf("oci_pace_avw")));

        Map<String, String> rejected = row(out, 2);
        assertEquals("PACE_OCI.20260603T113224.L2.OC_AOP.V3_2.NRT.nc", rejected.get("oci_pace_l2fname"));
        assertEquals("0.18763333123580567", rejected.get("oci_pace_rrsaot_cv"), "a rejected granule's values are kept");
        assertEquals("no", rejected.get("matchup_found"));
        assertEquals(service.rows().get(0).get("sat_Rrs_442.3_unc"), rejected.get("oci_pace_rrs_442.3_unc"));
    }

    @Test
    void aMatchedGranuleBeatsACloserRejectedOne() throws Exception {
        SeabassFile file = cruise();
        ResultJoiner.Joined joined = ResultJoiner.join(file, SeabassObservations.of(file),
                List.of(pace("PACE_OCI.20260603T113224.L2.OC_AOP.V3_2.NRT.csv",
                        "PACE_OCI.20260603T095405.L2.OC_AOP.V3_2.NRT.csv")), ResultJoiner.Rows.ALL, RUN);

        Map<String, String> baltic = row(joined.writer().lines(), 2);
        assertEquals("PACE_OCI.20260603T095405.L2.OC_AOP.V3_2.NRT.nc", baltic.get("oci_pace_l2fname"));
        assertEquals("-5844.0", baltic.get("oci_pace_tdiff"));
        assertEquals("yes", baltic.get("matchup_found"));
        assertEquals("PACE_OCI.20260603T095405.L2.OC_AOP.V3_2.NRT.nc",
                joined.chosen().get(PACE).get(2).get("granule"));
    }

    @Test
    void theBestRowRule() {
        Map<String, String> farMatched = Map.of("matchup_status", "matched", "tdiff", "-5000.0", "granule", "B");
        Map<String, String> nearMatched = Map.of("matchup_status", "matched", "tdiff", "300.0", "granule", "C");
        Map<String, String> sameAsNear = Map.of("matchup_status", "matched", "tdiff", "-300.0", "granule", "A");
        Map<String, String> nearRejected = Map.of("matchup_status", "rejected", "tdiff", "5.0", "granule", "D");
        Map<String, String> outside = Map.of("matchup_status", "not_in_granule", "tdiff", "", "granule", "E");

        assertEquals(nearMatched, ResultJoiner.best(List.of(farMatched, nearMatched, nearRejected, outside)));
        assertEquals(sameAsNear, ResultJoiner.best(List.of(nearMatched, sameAsNear)), "a tie goes to the granule name");
        assertEquals(nearRejected, ResultJoiner.best(List.of(outside, nearRejected)));
        assertNull(ResultJoiner.best(List.of(outside)));
    }

    @Test
    void matchedRowsOnlyAsTheScriptDoesByDefault() throws Exception {
        SeabassFile file = cruise();
        ResultJoiner.Joined joined = ResultJoiner.join(file, SeabassObservations.of(file),
                List.of(pace("PACE_OCI.20260603T113224.L2.OC_AOP.V3_2.NRT.csv")), ResultJoiner.Rows.MATCHED, RUN);
        List<String> out = joined.writer().lines();
        assertEquals(out.size() - 1, out.indexOf("/end_header"), "the one PACE row was rejected");

        ResultJoiner.Joined viirs = ResultJoiner.join(file, SeabassObservations.of(file), List.of(viirs()),
                ResultJoiner.Rows.MATCHED, RUN);
        List<String> kept = viirs.writer().lines();
        assertEquals(kept.size() - 2, kept.indexOf("/end_header"));
        assertTrue(kept.get(kept.size() - 1).startsWith("1,20170406,"));
    }

    @Test
    void severalProductsEachGetAMatchupFoundColumn() throws Exception {
        SeabassFile file = cruise();
        ResultJoiner.ProductRows modis = rows(product(MODIST, "MODIST_L2_OC"), 1,
                csv("TERRA_MODIS.20101010T180501.L2.OC.csv"));
        ResultJoiner.Joined joined = ResultJoiner.join(file, SeabassObservations.of(file), List.of(viirs(), modis),
                ResultJoiner.Rows.ALL, RUN);
        List<String> out = joined.writer().lines();
        List<String> fields = list(out, "/fields=");

        assertTrue(fields.contains("viirs_suomi-npp_matchup_found") && fields.contains("modis_terra_matchup_found"));
        assertTrue(fields.indexOf("viirs_suomi-npp_matchup_found") < fields.indexOf("modis_terra_l2fname"));
        assertEquals("yes", row(out, 0).get("viirs_suomi-npp_matchup_found"));
        assertEquals("no", row(out, 0).get("modis_terra_matchup_found"));
        assertEquals("95.0", row(out, 1).get("modis_terra_tdiff"));
        assertEquals("yes", row(out, 1).get("modis_terra_matchup_found"));
        assertTrue(out.contains("! Product: MODIST_L2_OC (C1272944705-OB_CLOUD)"));
    }

    @Test
    void twoProductsWithOnePrefixCantShareAFile() throws Exception {
        SeabassFile file = cruise();
        ResultJoiner.ProductRows other = rows(product(VIIRSN, "VIIRSN_L2_IOP"), 0,
                csv("SNPP_VIIRS.20170406T093001.L2.OC.csv"));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> ResultJoiner.join(file,
                SeabassObservations.of(file), List.of(viirs(), other), ResultJoiner.Rows.ALL, RUN));
        assertTrue(e.getMessage().contains("viirs_suomi-npp"), e.getMessage());
    }

    @Test
    void aRowForAnotherObservationIsLeftOut() throws Exception {
        SeabassFile file = cruise();
        ResultJoiner.ProductRows wrong = rows(product(VIIRSN, "VIIRSN_L2_OC"), 1,
                csv("SNPP_VIIRS.20170406T093001.L2.OC.csv"));
        ResultJoiner.Joined joined = ResultJoiner.join(file, SeabassObservations.of(file), List.of(wrong),
                ResultJoiner.Rows.ALL, RUN);

        assertEquals(1, joined.warnings().size());
        assertTrue(joined.warnings().get(0).contains("line 10") && joined.warnings().get(0).contains("42.925"),
                joined.warnings().get(0));
        assertEquals("no", row(joined.writer().lines(), 1).get("matchup_found"));
        assertTrue(joined.chosen().get(VIIRSN).isEmpty());
    }

    @Test
    void theHeaderSaysHowTheFileWasMade() throws Exception {
        SeabassFile file = cruise();
        List<String> out = ResultJoiner.join(file, SeabassObservations.of(file), List.of(viirs()),
                ResultJoiner.Rows.ALL, RUN).writer().lines();
        int end = out.indexOf("/end_header");

        assertEquals("! demo stations", out.get(out.indexOf("/fields=" + String.join(",", list(out, "/fields="))) + 2),
                "the file's own comments stay where they were");
        assertTrue(out.contains("! File amended by the SeaDAS satellite-in-situ matchup tool on 2026-11-04 21:00:00 UTC."));
        assertTrue(out.contains("! Satellite values from the satellite-in-situ-matchup service in NASA Harmony "
                + "(https://harmony.uat.earthdata.nasa.gov),"));
        assertTrue(out.contains("!     Maximum time difference between satellite and in situ = 3 hours"));
        assertTrue(out.subList(0, end).stream().noneMatch(l -> l.contains("qual_sst")), "no SST note without SST");
        assertEquals("! Run record: cruise.matchup.json", out.get(end - 2));
    }

    @Test
    void columnPrefixesAreTheGranulesNotCmrs() {
        assertEquals("viirs_jpss-1", MatchupProduct.prefix("JPSS1_VIIRS.20261001T224201.L2.OC.nc"));
        assertEquals("modis_aqua", MatchupProduct.prefix("AQUA_MODIS.20240520T191501.L2.SST.nc"));
        assertEquals("oci_pace", MatchupProduct.prefix("PACE_OCI.20260603T113224.L2.OC_AOP.V3_2.NRT.nc"));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> MatchupProduct.prefix("SEASTAR_SEAWIFS_GAC.20000101T000000.L2.OC.nc"));
        assertTrue(e.getMessage().contains("SEASTAR_SEAWIFS_GAC"), e.getMessage());
    }

    @Test
    void sstColumnsGetMeanAndMaxForQualSst() {
        List<String> header = new ArrayList<>(MatchupCsv.FIXED);
        header.addAll(List.of("sat_sst_valid_pixel_count", "sat_sst_mean", "sat_sst_sd",
                "sat_qual_sst_valid_pixel_count", "sat_qual_sst_mean", "sat_qual_sst_max"));
        MatchupProduct sst = new MatchupProduct("C1-OB_CLOUD", "MODISA_L2_SST", Map.of("sst", "degrees_C"));

        List<ResultJoiner.Column> columns = ResultJoiner.columns(header, "modis_aqua", sst);
        assertEquals(List.of("modis_aqua_l2fname", "modis_aqua_tdiff", "modis_aqua_rrsaot_cv", "modis_aqua_sst",
                        "modis_aqua_sst_sd", "modis_aqua_qual_sst_mean", "modis_aqua_qual_sst_max"),
                columns.stream().map(ResultJoiner.Column::name).toList());
        assertEquals(List.of("none", "seconds", "unitless", "degrees_C", "degrees_C", "none", "none"),
                columns.stream().map(ResultJoiner.Column::units).toList());
    }

    @Test
    void readsQuotedCsvValues() throws Exception {
        MatchupCsv csv = MatchupCsv.parse(String.join(",", MatchupCsv.FIXED) + "\r\n"
                + "1.0,2.0,2026-06-03T11:35:00.000Z,\"A, \"\"odd\"\" name.nc\",matched,,1.0,,25,25,0\r\n", "x.csv");
        assertEquals("A, \"odd\" name.nc", csv.rows().get(0).get("granule"));
        assertThrows(IOException.class, () -> MatchupCsv.parse("a,b\n1,2\n", "y.csv"));
        assertThrows(IOException.class, () -> MatchupCsv.parse(String.join(",", MatchupCsv.FIXED) + "\n1,2\n", "z.csv"));
        assertEquals(Set.of(), Set.copyOf(MatchupCsv.parse(String.join(",", MatchupCsv.FIXED) + "\n", "e.csv").rows()));
    }

    @Test
    void fetchesUnitsFromCmr() throws Exception {
        String variables = Files.readString(resource("cmr-variables-" + VIIRSN + ".json"));
        String collection = new JSONObject().put("items", new org.json.JSONArray().put(new JSONObject()
                .put("umm", new JSONObject().put("ShortName", "VIIRSN_L2_OC"))
                .put("meta", new JSONObject().put("associations", new JSONObject()
                        .put("variables", new org.json.JSONArray().put("V1-OB_CLOUD").put("V2-OB_CLOUD")))))).toString();
        List<URI> asked = new ArrayList<>();
        HttpTransport cmr = (method, uri, headers, body) -> {
            asked.add(uri);
            return FakeHarmony.reply(200, uri.getPath().contains("collections") ? collection : variables);
        };

        MatchupProduct product = MatchupProduct.fetch(cmr, HarmonyEnvironment.UAT.cmr(), VIIRSN);
        assertEquals("VIIRSN_L2_OC", product.shortName());
        assertEquals("mg_m^-3", product.unitsOf("chlor_a"));
        assertEquals("none", product.unitsOf("aot_862"));
        assertEquals("none", product.unitsOf("not_a_variable"));
        assertTrue(asked.get(1).getRawQuery().contains("concept_id%5B%5D=V1-OB_CLOUD&concept_id%5B%5D=V2-OB_CLOUD"),
                asked.get(1).toString());
    }
}
