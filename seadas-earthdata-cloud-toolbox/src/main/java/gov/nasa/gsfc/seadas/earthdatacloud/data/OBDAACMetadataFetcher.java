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
            if (hasGranules(shortName)) {
                System.out.println(shortName);
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
        try {
            // Get the module's root directory dynamically
            //System.getProperty("user.dir"):This will print the absolute path of the current directory from where your application was initialized.
            //seadas-toolbox\seadas-earthdata-cloud-toolbox\src\main\resources
            String moduleDir = System.getProperty("user.dir") + File.separator + "seadas-toolbox" + File.separator + "seadas-earthdata-cloud-toolbox";

            // Define target path inside the package's resources
            Path targetPath = Paths.get(moduleDir, "src", "main", "resources", "obdaac_metadata.json");

            // Ensure directory exists
            Files.createDirectories(targetPath.getParent());

            // Write JSON to file
            try (FileWriter fileWriter = new FileWriter(targetPath.toFile())) {
                fileWriter.write(metadata.toJSONString());
                fileWriter.flush();
                System.out.println("✅ Metadata successfully saved in: " + targetPath.toAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("❌ Error saving metadata file: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("❌ Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
