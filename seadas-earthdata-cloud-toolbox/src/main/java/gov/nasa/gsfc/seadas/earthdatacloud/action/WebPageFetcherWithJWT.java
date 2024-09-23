package gov.nasa.gsfc.seadas.earthdatacloud.action;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;

public class WebPageFetcherWithJWT {

    public static String fetchContent(String urlString, String bearerToken) throws Exception {
        StringBuilder content = new StringBuilder();
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        // Set request method to GET and add Bearer token to the Authorization header
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", "Bearer " + bearerToken);
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");

        // Enable following redirects
        connection.setInstanceFollowRedirects(true);

        // Check if the response is a redirect and follow the redirect if necessary
        int status = connection.getResponseCode();
        if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == HttpURLConnection.HTTP_SEE_OTHER) {
            // Get the redirected URL from the Location header
            String newUrl = connection.getHeaderField("Location");
            // Create a new connection with the redirected URL
            connection = (HttpURLConnection) new URL(newUrl).openConnection();
            connection.setRequestProperty("Authorization", "Bearer " + bearerToken);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            status = connection.getResponseCode();
        }

        // Check if the response is OK (200)
        if (status == HttpURLConnection.HTTP_OK) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine).append("\n");
                }
            }
        } else {
            throw new Exception("Failed to fetch content. HTTP status code: " + status);
        }

        return content.toString();
    }

    public JTable getSearchDataListTable(){
        String url = "https://harmony.uat.earthdata.nasa.gov/C1265136924-OB_CLOUD/ogc-api-coverages/1.0.0/collections/all/coverage/rangeset"; // Replace with your URL
        String bearerToken = "eyJ0eXAiOiJKV1QiLCJvcmlnaW4iOiJFYXJ0aGRhdGEgTG9naW4iLCJzaWciOiJlZGxqd3RwdWJrZXlfdWF0IiwiYWxnIjoiUlMyNTYifQ.eyJ0eXBlIjoiVXNlciIsInVpZCI6ImF5bnVyIiwiZXhwIjoxNzMxMDI1Mzc5LCJpYXQiOjE3MjU4NDEzNzksImlzcyI6Imh0dHBzOi8vdWF0LnVycy5lYXJ0aGRhdGEubmFzYS5nb3YifQ.E2NQ3ZwN3n440M1cWNsl0kkjl61a_6vcSlUW0Ef1NTRqWneioTFu9R09eXhdvj3yy2_j7YadZBbPoi-UNVLSq6KZ8IW-NBkOcnx4izhWxluoYkZ0lcB5V8UNhGh2meX-VVoTROitms5X0InRWNyhg6OzAvyBpD7JCRH-erO-NZ9FsPucrSP6vwT0NgvUUOs2tKAvQ2-0meoX9zELL63M47qBgbcgOt4Bh1VQRqoAONXwubGLT-bGf1RVnV_L3xscryp6kbbAO8v6ORnyzNfFxuX5Oc6Kuko2EzGUbXXoBmGOef0BZnjIl7eBmspvClr0hzYOSX4DkeU-giGAt_JAhg";
        final int MAX_ITERATIONS = 100;
        final int SLEEP_INTERVAL = 3000;

        JSONObject jsonResponse = null;
        JSONArray tableArray = null;
        JTable jTable = null;
        try {
            // Fetch the content of the webpage
            String content = fetchContent(url, bearerToken);

            // Parse the response as JSON
            jsonResponse = new JSONObject(content);
            boolean done = false;
            int counter = 0;
            String jobId = jsonResponse.getString("jobID");
            String jobUrl = "https://harmony.uat.earthdata.nasa.gov/jobs/" + jobId;

            while(!done && counter < MAX_ITERATIONS) {
                counter++;
                Thread.sleep(SLEEP_INTERVAL);

                content = fetchContent(jobUrl, bearerToken);
                jsonResponse = new JSONObject(content);

                // Check if the "status" field is equal to "successful"
                if (jsonResponse.has("status") && "successful".equals(jsonResponse.getString("status"))) {
                    done = true;
                }
            }

            if(done) {
                System.out.println("Status is successful!");
                tableArray = (JSONArray) jsonResponse.get("links");
                System.out.println("Response: " + jsonResponse.toString(4)); // Pretty print the JSON
                jTable = getJTable(tableArray);
            } else {
                // Job did not finish after MAX_ITERATIONS * SLEEP_INTERVAL milliseconds
                // output error message
            }
        } catch (Exception e) {
            System.err.println("Error fetching webpage: " + e.getMessage());
        }

        return jTable;

    }

    public JTable getJTable(JSONArray dataArray){

        String[] columnNames = { "Rel", "HREF", "Title", "Temporal", "BBox" };
        Object[][] dataSearchResult = new Object[dataArray.length()][5];
        

        // Iterate over the students array and add each row to the table
        for (int i = 0; i < dataArray.length(); i++) {
            JSONObject dataURL = dataArray.getJSONObject(i);
            if (dataURL.has("temporal") ) {
                List<String> row = new ArrayList<>();
                dataSearchResult[i][0] = dataURL.getString("rel");
                dataSearchResult[i][1] = dataURL.getString("href");
                dataSearchResult[i][2] = dataURL.getString("title");
            }
        }

        JTable searchResultTable = new JTable(dataSearchResult, columnNames);

        return searchResultTable;

    }

}