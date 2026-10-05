package gov.nasa.gsfc.seadas.earthdatacloud.data;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObCloudCollectionCatalogTest {

    private static JSONObject collection(String shortName, String start, String end) {
        JSONObject entry = new JSONObject().put("short_name", shortName).put("time_start", start);
        return end != null ? entry.put("time_end", end) : entry;
    }

    private static JSONArray entries(String... shortNames) {
        JSONArray entries = new JSONArray();
        for (String shortName : shortNames) {
            entries.put(new JSONObject().put("short_name", shortName));
        }
        return entries;
    }

    @Test
    void categorizesShortNamesBySatelliteLevelAndProduct() {
        Map<String, JSONObject> catalog = ObCloudCollectionCatalog.categorize(entries(
                "PACE_OCI_L2_AOP", "PACE_OCI_L2_AOP_NRT", "PACE_OCI_L1B", "MODISA_L3m_CHL",
                "PACE_OCI_L2_AOP", "NOT-A-COLLECTION"));

        assertEquals(Set.of("PACE_OCI", "MODISA"), catalog.keySet());
        JSONArray l2 = catalog.get("PACE_OCI").getJSONArray("L2");
        assertEquals(2, l2.length());  // the duplicate is dropped
        assertEquals("AOP", l2.getJSONObject(0).getString("product_name"));
        assertEquals("AOP_NRT", l2.getJSONObject(1).getString("product_name"));
        assertEquals("PACE_OCI_L2_AOP_NRT", l2.getJSONObject(1).getString("short_name"));
        assertEquals("General", catalog.get("PACE_OCI").getJSONArray("L1B").getJSONObject(0).getString("product_name"));
        assertEquals("CHL", catalog.get("MODISA").getJSONArray("L3m").getJSONObject(0).getString("product_name"));
    }

    @Test
    void worksOutMissionDateRanges() {
        JSONArray entries = new JSONArray()
                .put(collection("MODISA_L2_OC", "2002-07-04T00:00:00.000Z", null))
                .put(collection("MODISA_L3m_CHL", "2002-07-04T00:00:00.000Z", null))
                .put(collection("MODISA_L4m_ELOEV", "2000-01-01T00:00:00.000Z", "2021-10-31T23:59:59.999Z"))
                .put(collection("SeaWiFS_L2_OC", "1997-09-04T00:00:00.000Z", "2010-12-11T23:59:59.999Z"))
                .put(collection("SeaWiFS_L1A", "1997-09-05T00:00:00.000Z", "2010-12-10T00:00:00.000Z"))
                .put(collection("OSCAR_L4m_ELOEV", "2000-01-01T00:00:00.000Z", "2009-12-31T23:59:59.999Z"));

        Map<String, String[]> ranges = ObCloudCollectionCatalog.dateRanges(entries);

        // Level-4 only counts when a mission has nothing else
        assertArrayEquals(new String[]{"2002-07-04", "present"}, ranges.get("MODISA"));
        assertArrayEquals(new String[]{"1997-09-04", "2010-12-11"}, ranges.get("SeaWiFS"));
        assertArrayEquals(new String[]{"2000-01-01", "2009-12-31"}, ranges.get("OSCAR"));
    }

    @Test
    void comparesContents() {
        JSONArray entries = new JSONArray()
                .put(collection("MODISA_L2_OC", "2002-07-04T00:00:00.000Z", null))
                .put(collection("MODISA_L2_IOP", "2002-07-04T00:00:00.000Z", null));
        ObCloudCollectionCatalog.Contents a = new ObCloudCollectionCatalog.Contents(
                ObCloudCollectionCatalog.categorize(entries), ObCloudCollectionCatalog.dateRanges(entries));
        ObCloudCollectionCatalog.Contents b = new ObCloudCollectionCatalog.Contents(
                ObCloudCollectionCatalog.categorize(entries), ObCloudCollectionCatalog.dateRanges(entries));

        assertTrue(a.sameAs(b));

        entries.put(collection("MODISA_L2_SST", "2002-07-04T00:00:00.000Z", null));
        assertFalse(a.sameAs(new ObCloudCollectionCatalog.Contents(
                ObCloudCollectionCatalog.categorize(entries), ObCloudCollectionCatalog.dateRanges(entries))));

        entries.put(collection("MODISA_L1A", "2002-07-01T00:00:00.000Z", null));
        assertFalse(a.sameAs(new ObCloudCollectionCatalog.Contents(
                a.missions, ObCloudCollectionCatalog.dateRanges(entries))));
    }

    @Test
    void loadsEveryBundledFile() {
        ObCloudCollectionCatalog.Contents bundled = ObCloudCollectionCatalog.loadBundled();

        assertEquals(24, bundled.missions.size());
        assertTrue(bundled.missions.get("PACE_OCI").has("L2"));
        assertEquals(24, bundled.dateRanges.size());
    }
}
