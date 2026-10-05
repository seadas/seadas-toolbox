package gov.nasa.gsfc.seadas.earthdatacloud.data;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObCloudCollectionCatalogTest {

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
    void comparesContent() {
        Map<String, JSONObject> a = ObCloudCollectionCatalog.categorize(entries("MODISA_L2_OC", "MODISA_L2_IOP"));
        Map<String, JSONObject> b = ObCloudCollectionCatalog.categorize(entries("MODISA_L2_OC", "MODISA_L2_IOP"));
        Map<String, JSONObject> c = ObCloudCollectionCatalog.categorize(entries("MODISA_L2_OC", "MODISA_L2_IOP", "MODISA_L2_SST"));

        assertTrue(ObCloudCollectionCatalog.sameContent(a, b));
        assertFalse(ObCloudCollectionCatalog.sameContent(a, c));
    }

    @Test
    void loadsEveryBundledFile() {
        Map<String, JSONObject> bundled = ObCloudCollectionCatalog.loadBundled();

        assertEquals(24, bundled.size());
        assertTrue(bundled.get("PACE_OCI").has("L2"));
    }
}
