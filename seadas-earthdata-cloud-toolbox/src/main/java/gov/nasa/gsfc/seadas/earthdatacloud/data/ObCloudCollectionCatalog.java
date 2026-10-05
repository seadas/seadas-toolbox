package gov.nasa.gsfc.seadas.earthdatacloud.data;

import org.esa.snap.core.util.SystemUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

/**
 * The OB_CLOUD collections the data browser offers, as one JSON object per
 * satellite/instrument: {"L2": [{"product_name": "OC", "short_name": "MODISA_L2_OC"}, ...], ...}.
 * <p>
 * The files bundled under resources/json-files are a snapshot of CMR. The browser
 * refreshes them from CMR each time it opens and keeps the result in
 * ~/.seadas/auxdata/earthdata-cloud/json-files, which is used instead of the
 * bundled files from then on. The categorizing matches scripts/CMR_script.py,
 * which developers use to refresh the bundled files.
 */
public class ObCloudCollectionCatalog {

    private static final String CMR_COLLECTIONS_URL =
            "https://cmr.earthdata.nasa.gov/search/collections.json?provider=OB_CLOUD&page_size=";
    private static final int PAGE_SIZE = 2000;  // the most CMR returns per page

    // <satellite/instrument>_<level>[_<product>], e.g. PACE_OCI_L2_AOP_NRT
    private static final Pattern SHORT_NAME = Pattern.compile("^(.*?)_(L\\d+[a-zA-Z]*)(?:_(.*))?$");

    // The files in resources/json-files; keep in step with them.
    private static final String[] BUNDLED_FILES = {
            "AVHRR", "CALIPSO_CALIOP", "CZCS", "GOCI", "HAWKEYE", "HICO", "HYCOM", "MERGED_S3_OLCI",
            "MERIS", "MODISA", "MODIST", "OCTS", "OLCIS3A", "OLCIS3B", "OSCAR", "PACE_HARP2",
            "PACE_OCI", "PACE_SPEXONE", "SeaWiFS", "SeaWiFS_GAC", "SeaWiFS_MLAC", "VIIRSJ1", "VIIRSJ2", "VIIRSN"
    };

    public static Path getCacheDir() {
        return SystemUtils.getAuxDataPath().resolve("earthdata-cloud").resolve("json-files");
    }

    /**
     * The last list fetched from CMR, or the bundled one if there is none yet or it cannot be read.
     */
    public static Map<String, JSONObject> load() {
        Map<String, JSONObject> cached = loadCache();
        return cached != null ? cached : loadBundled();
    }

    public static Map<String, JSONObject> loadBundled() {
        Map<String, JSONObject> catalog = new TreeMap<>();
        for (String name : BUNDLED_FILES) {
            InputStream input = ObCloudCollectionCatalog.class.getClassLoader().getResourceAsStream("json-files/" + name + ".json");
            if (input == null) {
                continue;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                catalog.put(name, new JSONObject(new JSONTokener(reader)));
            } catch (Exception e) {
                SystemUtils.LOG.warning("Cannot read bundled json-files/" + name + ".json: " + e.getMessage());
            }
        }
        return catalog;
    }

    private static Map<String, JSONObject> loadCache() {
        Path dir = getCacheDir();
        if (!Files.isDirectory(dir)) {
            return null;
        }
        Map<String, JSONObject> catalog = new TreeMap<>();
        try (DirectoryStream<Path> files = Files.newDirectoryStream(dir, "*.json")) {
            for (Path file : files) {
                try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    String name = file.getFileName().toString();
                    catalog.put(name.substring(0, name.length() - ".json".length()), new JSONObject(new JSONTokener(reader)));
                }
            }
        } catch (Exception e) {
            SystemUtils.LOG.warning("Cannot read " + dir + ", using the bundled collection list: " + e.getMessage());
            return null;
        }
        return catalog.isEmpty() ? null : catalog;
    }

    /**
     * Fetches every OB_CLOUD collection from CMR and categorizes it by short name.
     *
     * @throws IOException if CMR cannot be reached or returns nothing usable
     */
    public static Map<String, JSONObject> fetchFromCmr() throws IOException {
        JSONArray entries = new JSONArray();
        String searchAfter = null;
        while (true) {
            HttpURLConnection connection = (HttpURLConnection) new URL(CMR_COLLECTIONS_URL + PAGE_SIZE).openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("Accept-Encoding", "gzip");
            if (searchAfter != null) {
                connection.setRequestProperty("CMR-Search-After", searchAfter);
            }
            try {
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    throw new IOException("CMR returned HTTP " + connection.getResponseCode());
                }
                InputStream input = connection.getInputStream();
                if ("gzip".equalsIgnoreCase(connection.getContentEncoding())) {
                    input = new GZIPInputStream(input);
                }
                JSONArray page;
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                    page = new JSONObject(new JSONTokener(reader)).getJSONObject("feed").optJSONArray("entry");
                }
                if (page == null) {
                    break;
                }
                for (int i = 0; i < page.length(); i++) {
                    entries.put(page.get(i));
                }
                searchAfter = connection.getHeaderField("CMR-Search-After");
                if (page.length() < PAGE_SIZE || searchAfter == null) {
                    break;
                }
            } finally {
                connection.disconnect();
            }
        }

        Map<String, JSONObject> catalog = categorize(entries);
        if (catalog.isEmpty()) {
            throw new IOException("CMR returned no OB_CLOUD collections");
        }
        return catalog;
    }

    static Map<String, JSONObject> categorize(JSONArray entries) {
        // satellite -> level -> products, in CMR order like CMR_script.py
        Map<String, Map<String, JSONArray>> grouped = new TreeMap<>();
        for (int i = 0; i < entries.length(); i++) {
            String shortName = entries.getJSONObject(i).optString("short_name", "");
            Matcher matcher = SHORT_NAME.matcher(shortName);
            if (!matcher.matches()) {
                continue;
            }
            String product = matcher.group(3) != null ? matcher.group(3) : "General";
            JSONArray products = grouped.computeIfAbsent(matcher.group(1), k -> new LinkedHashMap<>())
                    .computeIfAbsent(matcher.group(2), k -> new JSONArray());
            boolean seen = false;
            for (int j = 0; j < products.length() && !seen; j++) {
                seen = shortName.equals(products.getJSONObject(j).optString("short_name"));
            }
            if (!seen) {
                products.put(new JSONObject().put("product_name", product).put("short_name", shortName));
            }
        }
        Map<String, JSONObject> catalog = new TreeMap<>();
        grouped.forEach((satellite, levels) -> catalog.put(satellite, new JSONObject(levels)));
        return catalog;
    }

    /**
     * Replaces the cached list.  It is written to a new directory first, so a failed
     * write leaves the previous list in place.
     */
    public static void saveCache(Map<String, JSONObject> catalog) throws IOException {
        Path dir = getCacheDir();
        Path tmp = dir.resolveSibling(dir.getFileName() + ".new");
        deleteTree(tmp);
        Files.createDirectories(tmp);
        for (Map.Entry<String, JSONObject> entry : catalog.entrySet()) {
            Files.write(tmp.resolve(entry.getKey() + ".json"), entry.getValue().toString(4).getBytes(StandardCharsets.UTF_8));
        }
        deleteTree(dir);
        Files.move(tmp, dir);
    }

    /**
     * True if both hold the same satellites, levels and products.
     */
    public static boolean sameContent(Map<String, JSONObject> a, Map<String, JSONObject> b) {
        if (!a.keySet().equals(b.keySet())) {
            return false;
        }
        for (String key : a.keySet()) {
            if (!a.get(key).similar(b.get(key))) {
                return false;
            }
        }
        return true;
    }

    private static void deleteTree(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(path)) {
            for (Path p : (Iterable<Path>) paths.sorted(Comparator.reverseOrder())::iterator) {
                Files.delete(p);
            }
        }
    }
}
