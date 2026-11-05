package gov.nasa.gsfc.seadas.earthdatacloud.matchup;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * What the output needs about a collection that the service's CSV doesn't
 * carry: the column prefix and each variable's units.
 *
 * <p><b>Prefix.</b> {@code mk_matchup.py} names columns
 * {@code <instrument>_<platform>_...} from the granule's global attributes.
 * CMR's platform names differ for some missions (CMR {@code NOAA-20}, granule
 * {@code JPSS-1}), so the prefix comes from a table keyed on the granule name,
 * each entry checked against a real granule's attributes.
 *
 * <p><b>Units</b> come from the collection's CMR variable records, which carry
 * the granule's {@code units} attributes (checked for VIIRS and PACE OCI).
 *
 * @param units variable name (without its group) to units as CMR has them, or null for none
 */
public record MatchupProduct(String collectionId, String shortName, Map<String, String> units) {

    /**
     * Granule name prefix to the script's column prefix (lower-cased
     * {@code instrument} and {@code platform} attributes). Checked on
     * AQUA_MODIS.20260531T215001.L2.OC, TERRA_MODIS.20251201T023000.L2.OC,
     * SNPP_VIIRS.20170406T093001.L2.OC, JPSS1_VIIRS.20261001T224201.L2.OC,
     * JPSS2_VIIRS.20261001T205400.L2.OC and PACE_OCI.20260603T113224.L2.OC_AOP.
     */
    static final Map<String, String> PREFIXES = Map.of(
            "AQUA_MODIS", "modis_aqua",
            "TERRA_MODIS", "modis_terra",
            "SNPP_VIIRS", "viirs_suomi-npp",
            "JPSS1_VIIRS", "viirs_jpss-1",
            "JPSS2_VIIRS", "viirs_jpss-2",
            "PACE_OCI", "oci_pace");

    public MatchupProduct {
        units = Collections.unmodifiableMap(new LinkedHashMap<>(units));
    }

    /** The column prefix for a granule, e.g. {@code viirs_jpss-1} for {@code JPSS1_VIIRS.2026...nc}. */
    public static String prefix(String granule) {
        int dot = granule.indexOf('.');
        String mission = dot < 0 ? granule : granule.substring(0, dot);
        String prefix = PREFIXES.get(mission.toUpperCase(Locale.ROOT));
        if (prefix == null) {
            throw new IllegalArgumentException("No column prefix known for " + granule
                    + ": the matchup tool supports " + String.join(", ", PREFIXES.keySet()));
        }
        return prefix;
    }

    /**
     * A variable's units as the script writes them: whitespace becomes {@code _}
     * ({@code mg m^-3} → {@code mg_m^-3}), and none is {@code none}. The writer
     * lower-cases them, as the script does.
     */
    public String unitsOf(String variable) {
        String text = units.get(variable);
        if (text == null || text.isBlank()) {
            return "none";
        }
        return text.strip().replaceAll("\\s", "_");
    }

    // ------------------------------------------------------------ CMR

    /** Reads a collection and its variables' units from CMR (no token needed for public collections). */
    public static MatchupProduct fetch(HttpTransport http, URI cmr, String collectionId)
            throws IOException, InterruptedException {
        JSONObject collections = get(http, URI.create(cmr + "/search/collections.umm_json?concept_id="
                + URLEncoder.encode(collectionId, StandardCharsets.UTF_8)));
        JSONArray items = collections.optJSONArray("items");
        if (items == null || items.isEmpty()) {
            throw new IOException("CMR has no collection " + collectionId);
        }
        JSONObject item = items.getJSONObject(0);
        String shortName = item.getJSONObject("umm").optString("ShortName", collectionId);
        List<String> variableIds = new ArrayList<>();
        JSONObject associations = item.getJSONObject("meta").optJSONObject("associations");
        JSONArray ids = associations == null ? null : associations.optJSONArray("variables");
        for (int i = 0; ids != null && i < ids.length(); i++) {
            variableIds.add(ids.getString(i));
        }

        Map<String, String> units = new LinkedHashMap<>();
        for (int i = 0; i < variableIds.size(); i += 100) {
            List<String> chunk = variableIds.subList(i, Math.min(variableIds.size(), i + 100));
            StringBuilder query = new StringBuilder("page_size=100");
            for (String id : chunk) {
                query.append("&concept_id[]=").append(URLEncoder.encode(id, StandardCharsets.UTF_8));
            }
            units.putAll(unitsFrom(get(http, URI.create(cmr + "/search/variables.umm_json?"
                    + query.toString().replace("[]", "%5B%5D")))));
        }
        return new MatchupProduct(collectionId, shortName, units);
    }

    /** Variable name (without its group path) to units, from a CMR {@code variables.umm_json} reply. */
    static Map<String, String> unitsFrom(JSONObject variables) {
        Map<String, String> units = new LinkedHashMap<>();
        JSONArray items = variables.optJSONArray("items");
        for (int i = 0; items != null && i < items.length(); i++) {
            JSONObject umm = items.getJSONObject(i).getJSONObject("umm");
            String name = umm.optString("Name", "");
            name = name.substring(name.lastIndexOf('/') + 1);
            if (!name.isEmpty()) {
                units.put(name, umm.optString("Units", null));
            }
        }
        return units;
    }

    private static JSONObject get(HttpTransport http, URI uri) throws IOException, InterruptedException {
        try (HttpTransport.Response response = http.send("GET", uri, Map.of("Accept", "application/json"), null)) {
            String text = response.text();
            if (response.status() != 200) {
                throw new IOException("CMR returned HTTP " + response.status() + " for " + uri);
            }
            return new JSONObject(text);
        } catch (JSONException e) {
            throw new IOException("CMR's reply wasn't JSON: " + uri, e);
        }
    }
}
