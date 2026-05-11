package gov.nasa.gsfc.seadas.earthdatacloud.data;

import gov.nasa.gsfc.seadas.earthdatacloud.auth.WebPageFetcherWithJWT;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
            JSONArray collections = fetchCollections();

            JSONArray filteredCollections = filterCollectionsWithGranules(collections);

            saveMetadataToFile(filteredCollections);

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
            if (hasGranules(shortName)) {
                validCollections.add(collection);
            }
        }
        return validCollections;
    }

    private static boolean hasGranules(String shortName) throws Exception {
        String granulesPageUrl = "https://cmr.earthdata.nasa.gov/search/granules.json?provider=OB_CLOUD&short_name=" + shortName;
        HttpURLConnection conn = createConnection(granulesPageUrl);
        String response = readResponse(conn);

        JSONObject jsonResponse = (JSONObject) new JSONParser().parse(response);
        JSONObject feed = (JSONObject) jsonResponse.get("feed"); // Extract feed object
        JSONArray granules = (JSONArray) feed.get("entry"); // Extract granules array
        return granules != null && !granules.isEmpty();
    }


    private static HttpURLConnection createConnection(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

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
        try {
            String moduleDir = System.getProperty("user.dir") + File.separator + "seadas-toolbox" + File.separator + "seadas-earthdata-cloud-toolbox";

            Path targetPath = Paths.get(moduleDir, "src", "main", "resources", "obdaac_metadata.json");

            Files.createDirectories(targetPath.getParent());

            try (FileWriter fileWriter = new FileWriter(targetPath.toFile())) {
                fileWriter.write(metadata.toJSONString());
                fileWriter.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
