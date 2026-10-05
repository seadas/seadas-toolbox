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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

/**
 * The OB_CLOUD collections the data browser offers, as one JSON object per
 * satellite/instrument: {"L2": [{"product_name": "OC", "short_name": "MODISA_L2_OC"}, ...], ...},
 * plus each satellite/instrument's date range (mission_date_ranges.json).
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

    private static final String DATE_RANGES_FILE = "mission_date_ranges.json";

    /**
     * Satellite/instrument -> its products by level, and satellite/instrument ->
     * {start, end} as yyyy-MM-dd, with end "present" for missions still producing data.
     */
    public static final class Contents {
        public final Map<String, JSONObject> missions;
        public final Map<String, String[]> dateRanges;

        public Contents(Map<String, JSONObject> missions, Map<String, String[]> dateRanges) {
            this.missions = missions;
            this.dateRanges = dateRanges;
        }

        /**
         * True if both hold the same satellites, levels, products and date ranges.
         */
        public boolean sameAs(Contents other) {
            if (!missions.keySet().equals(other.missions.keySet()) || !dateRanges.keySet().equals(other.dateRanges.keySet())) {
                return false;
            }
            for (String key : missions.keySet()) {
                if (!missions.get(key).similar(other.missions.get(key))) {
                    return false;
                }
            }
            for (String key : dateRanges.keySet()) {
                if (!Arrays.equals(dateRanges.get(key), other.dateRanges.get(key))) {
                    return false;
                }
            }
            return true;
        }
    }

    public static Path getCacheDir() {
        return SystemUtils.getAuxDataPath().resolve("earthdata-cloud").resolve("json-files");
    }

    /**
     * The last list fetched from CMR, or the bundled one if there is none yet or it cannot be read.
     */
    public static Contents load() {
        Contents cached = loadCache();
        return cached != null ? cached : loadBundled();
    }

    public static Contents loadBundled() {
        Map<String, JSONObject> missions = new TreeMap<>();
        for (String name : BUNDLED_FILES) {
            JSONObject json = readBundled(name + ".json");
            if (json != null) {
                missions.put(name, json);
            }
        }
        JSONObject dateRanges = readBundled(DATE_RANGES_FILE);
        return new Contents(missions, dateRanges != null ? parseDateRanges(dateRanges) : new TreeMap<>());
    }

    private static JSONObject readBundled(String fileName) {
        InputStream input = ObCloudCollectionCatalog.class.getClassLoader().getResourceAsStream("json-files/" + fileName);
        if (input == null) {
            return null;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            return new JSONObject(new JSONTokener(reader));
        } catch (Exception e) {
            SystemUtils.LOG.warning("Cannot read bundled json-files/" + fileName + ": " + e.getMessage());
            return null;
        }
    }

    private static Contents loadCache() {
        Path dir = getCacheDir();
        if (!Files.isDirectory(dir)) {
            return null;
        }
        Map<String, JSONObject> missions = new TreeMap<>();
        Map<String, String[]> dateRanges = new TreeMap<>();
        try (DirectoryStream<Path> files = Files.newDirectoryStream(dir, "*.json")) {
            for (Path file : files) {
                try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    String name = file.getFileName().toString();
                    JSONObject json = new JSONObject(new JSONTokener(reader));
                    if (name.equals(DATE_RANGES_FILE)) {
                        dateRanges = parseDateRanges(json);
                    } else {
                        missions.put(name.substring(0, name.length() - ".json".length()), json);
                    }
                }
            }
        } catch (Exception e) {
            SystemUtils.LOG.warning("Cannot read " + dir + ", using the bundled collection list: " + e.getMessage());
            return null;
        }
        return missions.isEmpty() ? null : new Contents(missions, dateRanges);
    }

    private static Map<String, String[]> parseDateRanges(JSONObject json) {
        Map<String, String[]> dateRanges = new TreeMap<>();
        for (String key : json.keySet()) {
            JSONObject range = json.getJSONObject(key);
            String start = range.optString("start", null);
            if (start != null) {
                dateRanges.put(key, new String[]{start, range.optString("end", "present")});
            }
        }
        return dateRanges;
    }

    /**
     * Fetches every OB_CLOUD collection from CMR, categorizes it by short name and
     * works out each satellite/instrument's date range.
     *
     * @throws IOException if CMR cannot be reached or returns nothing usable
     */
    public static Contents fetchFromCmr() throws IOException {
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

        Map<String, JSONObject> missions = categorize(entries);
        if (missions.isEmpty()) {
            throw new IOException("CMR returned no OB_CLOUD collections");
        }
        return new Contents(missions, dateRanges(entries));
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
     * Each satellite/instrument's date range from its collections' time_start and
     * time_end: the earliest start, and the latest end or "present" if any collection
     * has no end. Level-4 collections only count for missions that have nothing else:
     * they can be model products reaching back before the mission (MODISA_L4m_ELOEV
     * starts in 2000, Aqua launched in 2002).
     */
    static Map<String, String[]> dateRanges(JSONArray entries) {
        Map<String, List<JSONObject>> all = new TreeMap<>();
        Map<String, List<JSONObject>> withoutL4 = new TreeMap<>();
        for (int i = 0; i < entries.length(); i++) {
            JSONObject entry = entries.getJSONObject(i);
            Matcher matcher = SHORT_NAME.matcher(entry.optString("short_name", ""));
            if (!matcher.matches()) {
                continue;
            }
            all.computeIfAbsent(matcher.group(1), k -> new ArrayList<>()).add(entry);
            if (!matcher.group(2).startsWith("L4")) {
                withoutL4.computeIfAbsent(matcher.group(1), k -> new ArrayList<>()).add(entry);
            }
        }
        Map<String, String[]> ranges = new TreeMap<>();
        for (String satellite : all.keySet()) {
            String start = null;
            String end = null;
            boolean ongoing = false;
            for (JSONObject entry : withoutL4.getOrDefault(satellite, all.get(satellite))) {
                String entryStart = day(entry.optString("time_start", ""));
                String entryEnd = day(entry.optString("time_end", ""));
                if (entryStart != null && (start == null || entryStart.compareTo(start) < 0)) {
                    start = entryStart;
                }
                if (entryEnd == null) {
                    ongoing = true;
                } else if (end == null || entryEnd.compareTo(end) > 0) {
                    end = entryEnd;
                }
            }
            if (start != null) {
                ranges.put(satellite, new String[]{start, ongoing || end == null ? "present" : end});
            }
        }
        return ranges;
    }

    // yyyy-MM-dd from an ISO time such as 2002-07-04T00:00:00.000Z, or null
    private static String day(String time) {
        return time.matches("\\d{4}-\\d{2}-\\d{2}.*") ? time.substring(0, 10) : null;
    }

    /**
     * Replaces the cached list.  It is written to a new directory first, so a failed
     * write leaves the previous list in place.
     */
    public static void saveCache(Contents contents) throws IOException {
        Path dir = getCacheDir();
        Path tmp = dir.resolveSibling(dir.getFileName() + ".new");
        deleteTree(tmp);
        Files.createDirectories(tmp);
        for (Map.Entry<String, JSONObject> entry : contents.missions.entrySet()) {
            Files.write(tmp.resolve(entry.getKey() + ".json"), entry.getValue().toString(4).getBytes(StandardCharsets.UTF_8));
        }
        Files.write(tmp.resolve(DATE_RANGES_FILE), dateRangesJson(contents.dateRanges).toString(4).getBytes(StandardCharsets.UTF_8));
        deleteTree(dir);
        Files.move(tmp, dir);
    }

    static JSONObject dateRangesJson(Map<String, String[]> dateRanges) {
        JSONObject json = new JSONObject();
        dateRanges.forEach((satellite, range) -> json.put(satellite, new JSONObject().put("start", range[0]).put("end", range[1])));
        return json;
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
