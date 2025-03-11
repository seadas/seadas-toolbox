package gov.nasa.gsfc.seadas.earthdatacloud.data;

import gov.nasa.gsfc.seadas.earthdatacloud.action.WebPageFetcherWithJWT;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class OBDAACMetadataFetcher {
    private static final String CMR_COLLECTIONS_URL = "https://cmr.earthdata.nasa.gov/search/collections.json?provider=OB_CLOUD&page_size=2000";
    private static final String METADATA_FILE = "src/main/resources/obdaac_metadata.json";
    private static String AUTH_TOKEN;

    public static void main(String[] args) {
        AUTH_TOKEN = WebPageFetcherWithJWT.getAccessToken("urs.earthdata.nasa.gov"); // Fetch token only once
        fetchAndSaveMetadata();
    }

    private static void fetchAndSaveMetadata() {
        try {
            System.out.println("Fetching collections from NASA CMR...");
            JSONArray collections = fetchCollections();

            System.out.println("Filtering collections with available granules...");
            JSONArray filteredCollections = filterCollectionsWithGranules(collections);

            System.out.println("Saving metadata to JSON file...");
            saveMetadataToFile(filteredCollections);

            System.out.println("Metadata successfully saved in " + METADATA_FILE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static JSONArray fetchCollections() throws Exception {
        HttpURLConnection conn = createConnection(CMR_COLLECTIONS_URL);
        String response = readResponse(conn);
        JSONObject jsonResponse = (JSONObject) new JSONParser().parse(response);
        JSONObject feed = (JSONObject) jsonResponse.get("feed"); // Extract feed object
        return (JSONArray) feed.get("entry"); // Extract entry array
    }

    private static JSONArray filterCollectionsWithGranules(JSONArray collections) throws Exception {
        JSONArray validCollections = new JSONArray();

        for (Object obj : collections) {
            JSONObject collection = (JSONObject) obj;
            String shortName = (String) collection.get("short_name");
            System.out.println(shortName);
            if (hasGranules(shortName)) {
                validCollections.add(collection);
            }
        }
        return validCollections;
    }

    private static boolean hasGranules(String shortName) throws Exception {
        String granulesPageUrl = "https://mmt.earthdata.nasa.gov/collections?keyword=" + shortName + "&provider=OB_CLOUD";
        HttpURLConnection conn = createConnection(granulesPageUrl);
        String response = readResponse(conn);

        // Debug: Print response to see what we are getting
        System.out.println("Granules check for: " + shortName);
        System.out.println(response);

        // Check if the page contains a granule count greater than 0
        return response.contains(">0<");  // If >0< is found, granules exist
    }


    private static HttpURLConnection createConnection(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        // Use pre-fetched authentication token
        conn.setRequestProperty("Authorization", "Bearer " + AUTH_TOKEN);

        conn.setRequestProperty("Accept", "application/json");
        return conn;
    }

    private static String readResponse(HttpURLConnection conn) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }

    private static void saveMetadataToFile(JSONArray metadata) {
        File file = new File(METADATA_FILE);
        File parentDir = file.getParentFile();

        try {
            // Ensure the parent directory exists
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            // Write the metadata to file
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(metadata.toJSONString());
                System.out.println("Metadata successfully saved in " + METADATA_FILE);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
