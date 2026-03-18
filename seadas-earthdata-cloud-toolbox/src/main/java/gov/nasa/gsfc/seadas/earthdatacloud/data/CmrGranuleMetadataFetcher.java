package gov.nasa.gsfc.seadas.earthdatacloud.data;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class CmrGranuleMetadataFetcher {

    private static final String PROVIDER = "OB_CLOUD";
    private static final String CMR_GRANULES_URL = "https://cmr.earthdata.nasa.gov/search/granules.json";
    private static volatile String AUTH_TOKEN; // optional

    // Small LRU cache (keeps UI snappy on repeated actions)
    private static final int MAX_CACHE = 200;
    private static final Map<String, GranuleMeta> CACHE =
            new LinkedHashMap<>(MAX_CACHE, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, GranuleMeta> eldest) {
                    return size() > MAX_CACHE;
                }
            };

    public static class GranuleMeta {
        public final String granuleId;
        public final String collectionConceptId;
        public final String title;
        public final String producerGranuleId;
        public final Double minLat, maxLat, minLon, maxLon;
        public final JSONArray polygons; // may be null

        public GranuleMeta(String granuleId,
                           String collectionConceptId, String title, String producerGranuleId,
                           Double minLat, Double maxLat,
                           Double minLon, Double maxLon,
                           JSONArray polygons) {
            this.granuleId = granuleId;
            this.collectionConceptId = collectionConceptId;
            this.title = title;
            this.producerGranuleId = producerGranuleId;
            this.minLat = minLat;
            this.maxLat = maxLat;
            this.minLon = minLon;
            this.maxLon = maxLon;
            this.polygons = polygons;
        }
    }
    public static Double[] computeBBoxFromPolygons(JSONArray polygons) {
        if (polygons == null || polygons.isEmpty()) return null;

        double minLat = Double.POSITIVE_INFINITY;
        double maxLat = Double.NEGATIVE_INFINITY;
        double minLon = Double.POSITIVE_INFINITY;
        double maxLon = Double.NEGATIVE_INFINITY;

        for (Object polyObj : polygons) {
            if (polyObj == null) continue;

            if (polyObj instanceof JSONArray polyArr) {
                for (Object ringObj : polyArr) {
                    if (ringObj == null) continue;
                    String ring = ringObj.toString().trim();
                    if (ring.isEmpty()) continue;

                    String[] parts = ring.split("\\s+");
                    for (int i = 0; i + 1 < parts.length; i += 2) {
                        double lat = Double.parseDouble(parts[i]);
                        double lon = Double.parseDouble(parts[i + 1]);
                        minLat = Math.min(minLat, lat);
                        maxLat = Math.max(maxLat, lat);
                        minLon = Math.min(minLon, lon);
                        maxLon = Math.max(maxLon, lon);
                    }
                }
            } else {
                String ring = polyObj.toString().trim();
                if (ring.isEmpty()) continue;

                String[] parts = ring.split("\\s+");
                for (int i = 0; i + 1 < parts.length; i += 2) {
                    double lat = Double.parseDouble(parts[i]);
                    double lon = Double.parseDouble(parts[i + 1]);
                    minLat = Math.min(minLat, lat);
                    maxLat = Math.max(maxLat, lat);
                    minLon = Math.min(minLon, lon);
                    maxLon = Math.max(maxLon, lon);
                }
            }
        }

        if (!Double.isFinite(minLat) || !Double.isFinite(minLon)) return null;
        return new Double[]{minLat, maxLat, minLon, maxLon};
    }
    /** Fetch granuleId + bbox (+ optional polygons) by readable granule name (file name). */
    public static GranuleMeta fetchGranuleMetadataByFileName(String fileName) throws Exception {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("fileName is blank");
        }

        synchronized (CACHE) {
            GranuleMeta cached = CACHE.get(fileName);
            if (cached != null) return cached;
        }

        String url = CMR_GRANULES_URL
                + "?provider=" + PROVIDER
                + "&page_size=1"
                + "&readable_granule_name=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8);

        HttpURLConnection conn = createConnection(url);

        int status = conn.getResponseCode();
        if (status >= 400) {
            throw new Exception("CMR granule lookup failed HTTP " + status + " for " + url);
        }

        String response = readResponse(conn);
        JSONObject json = (JSONObject) new JSONParser().parse(response);
        JSONObject feed = (JSONObject) json.get("feed");
        JSONArray entry = (JSONArray) feed.get("entry");

        if (entry == null || entry.isEmpty()) {
            throw new Exception("No CMR granule entry found for: " + fileName);
        }

        JSONObject g = (JSONObject) entry.get(0);

        String granuleId = (String) g.get("id");
        String collectionConceptId = (String) g.get("collection_concept_id");
        String title = (String) g.get("title");
        String producerGranuleId = (String) g.get("producer_granule_id");

        System.out.println("granule id: " + granuleId);
        System.out.println("collection concept id: " + collectionConceptId);
        System.out.println("granule title: " + g.get("title"));
        System.out.println("granule size: " + g.get("granule_size"));
        System.out.println("producer granule id: " + g.get("producer_granule_id"));
        System.out.println("data set id: " + g.get("dataset_id"));

        Double minLat = null, maxLat = null, minLon = null, maxLon = null;

        // Prefer "boxes" if present (fast bbox)
        JSONArray boxes = (JSONArray) g.get("boxes");
        if (boxes != null && !boxes.isEmpty()) {
            String[] parts = ((String) boxes.get(0)).trim().split("\\s+");
            // lat1 lon1 lat2 lon2
            double lat1 = Double.parseDouble(parts[0]);
            double lon1 = Double.parseDouble(parts[1]);
            double lat2 = Double.parseDouble(parts[2]);
            double lon2 = Double.parseDouble(parts[3]);
            minLat = Math.min(lat1, lat2);
            maxLat = Math.max(lat1, lat2);
            minLon = Math.min(lon1, lon2);
            maxLon = Math.max(lon1, lon2);
        }

        // Optional: polygons (more precise footprint)
        JSONArray polygons = (JSONArray) g.get("polygons"); // may be null

        GranuleMeta meta = new GranuleMeta(
                granuleId,
                collectionConceptId,
                title,
                producerGranuleId,
                minLat, maxLat,
                minLon, maxLon,
                polygons
        );

        synchronized (CACHE) {
            CACHE.put(fileName, meta);
        }
        return meta;
    }

    private static HttpURLConnection createConnection(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");

        // Only attach token if you actually have one; otherwise keep it public & fast.
        String token = AUTH_TOKEN;
        if (token == null || token.isBlank()) {
            // If needed (restricted collections), uncomment:
            // AUTH_TOKEN = WebPageFetcherWithJWT.getAccessToken("urs.earthdata.nasa.gov");
            // token = AUTH_TOKEN;
        }
        if (token != null && !token.isBlank()) {
            conn.setRequestProperty("Authorization", "Bearer " + token);
        }

        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(30_000);
        return conn;
    }

    private static String readResponse(HttpURLConnection conn) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }
}