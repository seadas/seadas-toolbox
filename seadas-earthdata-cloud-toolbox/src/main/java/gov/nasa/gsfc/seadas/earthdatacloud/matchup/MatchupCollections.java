package gov.nasa.gsfc.seadas.earthdatacloud.matchup;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The products the matchup tool offers: the Level-2 collections associated
 * with the service, with their CMR concept IDs in each environment. Read from
 * {@code matchup-collections.json} next to this class.
 *
 * <p>UAT lists only the collections that have granules there.
 */
public final class MatchupCollections {

    static final String RESOURCE = "matchup-collections.json";

    /**
     * One product.
     *
     * @param sensor  e.g. {@code MODIS Aqua}
     * @param product e.g. {@code ocean color}, {@code SST4}, {@code AOP}
     * @param mission the granule name's first part, e.g. {@code AQUA_MODIS}; gives the column prefix
     * @param ids     concept ID per environment; missing where the environment doesn't have it
     */
    public record Entry(String shortName, String sensor, String product, boolean nrt, String mission,
                        Map<HarmonyEnvironment, String> ids) {
        public Entry {
            ids = Collections.unmodifiableMap(new EnumMap<>(ids));
        }

        /** The concept ID in an environment, or null. */
        public String id(HarmonyEnvironment environment) {
            return ids.get(environment);
        }

        /** The output column prefix, e.g. {@code modis_aqua}. */
        public String columnPrefix() {
            return MatchupProduct.PREFIXES.get(mission);
        }

        /** For a list: {@code MODIS Aqua ocean color, near real time (MODISA_L2_OC_NRT)}. */
        public String label() {
            return sensor + " " + product + (nrt ? ", near real time" : "") + " (" + shortName + ")";
        }
    }

    private static List<Entry> all;

    private MatchupCollections() {
    }

    /** Every product, in the table's order. */
    public static synchronized List<Entry> all() {
        if (all == null) {
            try (InputStream in = MatchupCollections.class.getResourceAsStream(RESOURCE)) {
                if (in == null) {
                    throw new IOException("missing resource " + RESOURCE);
                }
                all = parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return all;
    }

    /** The products an environment has. */
    public static List<Entry> in(HarmonyEnvironment environment) {
        return all().stream().filter(e -> e.id(environment) != null).toList();
    }

    /** The product with this concept ID, in any environment. */
    public static Optional<Entry> byId(String collectionId) {
        return all().stream().filter(e -> e.ids().containsValue(collectionId)).findFirst();
    }

    static List<Entry> parse(String text) throws IOException {
        try {
            JSONArray array = new JSONObject(text).getJSONArray("collections");
            List<Entry> entries = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                JSONObject c = array.getJSONObject(i);
                Map<HarmonyEnvironment, String> ids = new EnumMap<>(HarmonyEnvironment.class);
                JSONObject idJson = c.getJSONObject("ids");
                for (String key : idJson.keySet()) {
                    ids.put(HarmonyEnvironment.valueOf(key), idJson.getString(key));
                }
                entries.add(new Entry(c.getString("shortName"), c.getString("sensor"), c.getString("product"),
                        c.getBoolean("nrt"), c.getString("mission"), ids));
            }
            return List.copyOf(entries);
        } catch (JSONException | IllegalArgumentException e) {
            throw new IOException(RESOURCE + ": " + e.getMessage(), e);
        }
    }
}
