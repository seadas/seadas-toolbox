package gov.nasa.gsfc.seadas.earthdatacloud.matchup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static gov.nasa.gsfc.seadas.earthdatacloud.matchup.SeabassFileTest.parse;
import static gov.nasa.gsfc.seadas.earthdatacloud.matchup.SeabassFileTest.resource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeabassWriterTest {

    @Test
    void withNothingAddedTheFileComesBackAsItWas() throws Exception {
        for (String name : List.of("VIIRS_2024_HPLC_NASA_R1.sb",
                "EXPORTS_EXPORTSNA_DY131_HyperSAS_20210501_080000_L2_Rrs_R1.sb",
                "VIIRS_2024_ag_st_12_77m_R4.sb")) {
            Path path = resource(name);
            List<String> original = Files.readAllLines(path, StandardCharsets.ISO_8859_1).stream()
                    .map(String::strip).filter(l -> !l.isEmpty()).toList();

            assertEquals(original, new SeabassWriter(SeabassFile.read(path)).lines(), name);
        }
    }

    @Test
    void appendsColumnsAsTheScriptNamesThem() throws Exception {
        SeabassFile file = SeabassFile.read(resource("VIIRS_2024_HPLC_NASA_R1.sb"));
        SeabassWriter writer = new SeabassWriter(file);
        writer.set(0, "AQUA_MODIS_l2fname", "none", "AQUA_MODIS.20240520T191501.L2.OC.nc");
        writer.set(0, "aqua_modis_tdiff", "Seconds", "-1501.0");
        writer.set(0, "aqua_modis_chlor_a", "mg m^-3", "0.384000");
        writer.set(1, "aqua_modis_chlor_a", "ignored", "");
        writer.addComment(" File amended by SeaDAS Satellite-In Situ Matchup.");

        List<String> out = writer.lines();
        String fields = out.stream().filter(l -> l.startsWith("/fields=")).findFirst().orElseThrow();
        String units = out.stream().filter(l -> l.startsWith("/units=")).findFirst().orElseThrow();
        assertTrue(fields.endsWith(",Gyro,Tchl,aqua_modis_l2fname,aqua_modis_tdiff,aqua_modis_chlor_a"), fields);
        assertTrue(units.endsWith(",mg/m^3,none,seconds,mg m^-3"), units);
        assertEquals("! File amended by SeaDAS Satellite-In Situ Matchup.", out.get(out.indexOf("/end_header") - 1));

        int header = out.indexOf("/end_header");
        String first = out.get(header + 1);
        String second = out.get(header + 2);
        String last = out.get(out.size() - 1);
        assertTrue(first.endsWith(",0.513,AQUA_MODIS.20240520T191501.L2.OC.nc,-1501.0,0.384000"), first);
        assertTrue(second.endsWith(",0.528,-9999,-9999,-9999"), "blank and unset values are missing: " + second);
        assertTrue(last.endsWith(",-9999,-9999,-9999"), last);

        SeabassFile reread = SeabassFile.parse(out, null);
        assertEquals(40, reread.fields().size());
        assertEquals(40, reread.units().size());
        assertEquals("0.384000", reread.value(0, "aqua_modis_chlor_a"));
        assertEquals(file.value(5, "Tot_Chl_a"), reread.value(5, "tot_chl_a"));
    }

    @Test
    void settingAnExistingFieldOverwritesThatRowOnly() throws Exception {
        SeabassFile file = parse("/begin_header", "/missing=-9999", "/delimiter=comma",
                "/fields=date,time,lat,lon,aqua_modis_chlor_a", "/units=yyyymmdd,hh:mm:ss,degrees,degrees,mg/m^3",
                "/end_header", "20240520,14:02:00,29.1,-78.9,0.20", "20240520,15:02:00,29.2,-78.8,0.30");
        SeabassWriter writer = new SeabassWriter(file);
        writer.set(1, "AQUA_MODIS_CHLOR_A", "mg/m^3", "0.31");

        List<String> out = writer.lines();
        assertTrue(writer.addedFields().isEmpty());
        assertEquals("/fields=date,time,lat,lon,aqua_modis_chlor_a", out.get(3));
        assertEquals("20240520,14:02:00,29.1,-78.9,0.20", out.get(6));
        assertEquals("20240520,15:02:00,29.2,-78.8,0.31", out.get(7));
    }

    @Test
    void spaceDelimitedFilesStaySpaceDelimited() throws Exception {
        SeabassFile file = SeabassFile.read(resource("BENCALTChla.csv.sb"));
        SeabassWriter writer = new SeabassWriter(file);
        writer.set(0, "aqua_modis_tdiff", "seconds", "120.0");

        List<String> out = writer.lines();
        String first = out.get(out.indexOf("/end_header") + 1);
        assertEquals("2002 10 05 14 05 00 20855 02_10_02 2 -32.6590 18.0672 3.32 0.000 0.000 1.690 0.066 0.010 1.766 120.0",
                first);
        assertThrows(IllegalArgumentException.class, () -> writer.set(0, "x_l2fname", "none", "two words"));
    }

    @Test
    void refusesValuesAndNamesThatWouldBreakTheFile() throws Exception {
        SeabassWriter writer = new SeabassWriter(SeabassFile.read(resource("VIIRS_2024_HPLC_NASA_R1.sb")));

        assertThrows(IllegalArgumentException.class, () -> writer.set(0, "a,b", "none", "1"));
        assertThrows(IllegalArgumentException.class, () -> writer.set(0, " ", "none", "1"));
        assertThrows(IllegalArgumentException.class, () -> writer.set(0, "x", "none", "1,2"));
        assertThrows(IndexOutOfBoundsException.class, () -> writer.set(99, "x", "none", "1"));
    }

    @Test
    void writesWithUnixLineEndingsAndNeverOverTheInput(@TempDir Path dir) throws IOException {
        Path input = dir.resolve("in.sb");
        Files.copy(resource("VIIRS_2024_HPLC_NASA_R1.sb"), input);
        SeabassWriter writer = new SeabassWriter(SeabassFile.read(input));
        writer.set(0, "aqua_modis_tdiff", "seconds", "1.0");

        Path output = dir.resolve("out.sb");
        writer.write(output);
        String text = Files.readString(output, StandardCharsets.ISO_8859_1);
        assertFalse(text.contains("\r"));
        assertTrue(text.endsWith("\n"));
        assertEquals(99, SeabassFile.read(output).rowCount());

        assertThrows(IOException.class, () -> writer.write(input));
        assertEquals(Files.readString(resource("VIIRS_2024_HPLC_NASA_R1.sb")), Files.readString(input));
    }
}
