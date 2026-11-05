package gov.nasa.gsfc.seadas.earthdatacloud.matchup;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchupCollectionsTest {

    @Test
    void productionHasTheServiceRecordsTwentyFiveAndUatTheSixWithGranules() {
        assertEquals(25, MatchupCollections.in(HarmonyEnvironment.PRODUCTION).size());
        assertEquals(List.of("MODISA_L2_OC_NRT", "MODIST_L2_OC", "VIIRSN_L2_OC", "VIIRSJ1_L2_OC", "VIIRSJ2_L2_OC",
                        "PACE_OCI_L2_AOP_NRT"),
                MatchupCollections.in(HarmonyEnvironment.UAT).stream().map(MatchupCollections.Entry::shortName)
                        .toList());
    }

    @Test
    void everyEntryIsConsistent() {
        Set<String> ids = new HashSet<>();
        Set<String> names = new HashSet<>();
        for (MatchupCollections.Entry entry : MatchupCollections.all()) {
            assertTrue(names.add(entry.shortName()), "listed twice: " + entry.shortName());
            assertEquals(entry.shortName().endsWith("_NRT"), entry.nrt(), entry.shortName());
            assertNotNull(entry.columnPrefix(), "no column prefix for " + entry.mission());
            for (String id : entry.ids().values()) {
                assertTrue(id.matches("C\\d+-OB_(CLOUD|DAAC)"), id);
                assertTrue(ids.add(id), "concept ID listed twice: " + id);
                // Every ID makes a valid request.
                MatchupRequest.of(id, 0, 0, java.time.Instant.EPOCH);
            }
        }
    }

    @Test
    void entriesAreFoundByConceptIdAndLabelled() {
        MatchupCollections.Entry pace = MatchupCollections.byId("C1281448693-OB_CLOUD").orElseThrow();
        assertEquals("PACE_OCI_L2_AOP_NRT", pace.shortName());
        assertEquals("C4124887001-OB_CLOUD", pace.id(HarmonyEnvironment.PRODUCTION));
        assertEquals("oci_pace", pace.columnPrefix());
        assertEquals("PACE OCI AOP, near real time (PACE_OCI_L2_AOP_NRT)", pace.label());
        assertEquals("viirs_jpss-1", MatchupCollections.byId("C3166805767-OB_DAAC").orElseThrow().columnPrefix());
        assertTrue(MatchupCollections.byId("C1-OB_CLOUD").isEmpty());
    }

    @Test
    void aBrokenTableIsAnError() {
        assertThrows(java.io.IOException.class, () -> MatchupCollections.parse("{\"collections\":[{\"shortName\":"
                + "\"X\",\"sensor\":\"s\",\"product\":\"p\",\"nrt\":false,\"mission\":\"M\",\"ids\":{\"SIT\":\"C1-X\"}}]}"));
    }
}
