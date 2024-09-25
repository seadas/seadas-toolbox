package gov.nasa.gsfc.seadas.earthdatacloud.action;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public class WebPageFetcherWithJWT {

    private JProgressBar progressBar;
    private JLabel label;
    private JButton button;
    private String webContent;

    public WebPageFetcherWithJWT(String webContent) {
        this.progressBar = progressBar;
        this.label = label;
        this.button = button;
        this.webContent = webContent;
    }

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

    // Method to remove empty rows from the JTable's model
    public static void removeEmptyRows(DefaultTableModel model) {
        for (int row = model.getRowCount() - 1; row >= 0; row--) {
            boolean isEmpty = true;
            // Check if all cells in the row are empty (null or empty string)
            for (int col = 0; col < model.getColumnCount(); col++) {
                Object value = model.getValueAt(row, col);
                if (value != null && !value.toString().trim().isEmpty()) {
                    isEmpty = false; // Found a non-empty value
                    break;
                }
            }
            // If the row is empty, remove it from the model
            if (isEmpty) {
                model.removeRow(row);
            }
        }
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
            // Fetch the webContent of the webpage
            //String webContent = fetchContent(url, bearerToken);

            // Parse the response as JSON
            jsonResponse = new JSONObject(webContent);
            boolean done = false;
            int counter = 0;
            String jobId = jsonResponse.getString("jobID");
            String jobUrl = "https://harmony.uat.earthdata.nasa.gov/jobs/" + jobId;

            while(!done && counter < MAX_ITERATIONS) {
                counter++;
                Thread.sleep(SLEEP_INTERVAL);

                //webContent = fetchContent(jobUrl, bearerToken);
                jsonResponse = new JSONObject(webContent);

                // Check if the "status" field is equal to "successful"
                if (jsonResponse.has("status") && "successful".equals(jsonResponse.getString("status"))) {
                    done = true;
                }
            }

            if(done) {
                System.out.println("Status is successful!");
                tableArray = (JSONArray) jsonResponse.get("links");
                System.out.println("Response: " + jsonResponse.toString(4)); // Pretty print the JSON
                jTable = getJTableNew(tableArray);
            } else {
                // Job did not finish after MAX_ITERATIONS * SLEEP_INTERVAL milliseconds
                // output error message
            }
        } catch (Exception e) {
            System.err.println("Error fetching webpage: " + e.getMessage());
        }

        return jTable;

    }

    public JTable getSearchDataList(String webContent){
        String url = "https://harmony.uat.earthdata.nasa.gov/C1265136924-OB_CLOUD/ogc-api-coverages/1.0.0/collections/all/coverage/rangeset"; // Replace with your URL
        String bearerToken = "eyJ0eXAiOiJKV1QiLCJvcmlnaW4iOiJFYXJ0aGRhdGEgTG9naW4iLCJzaWciOiJlZGxqd3RwdWJrZXlfdWF0IiwiYWxnIjoiUlMyNTYifQ.eyJ0eXBlIjoiVXNlciIsInVpZCI6ImF5bnVyIiwiZXhwIjoxNzMxMDI1Mzc5LCJpYXQiOjE3MjU4NDEzNzksImlzcyI6Imh0dHBzOi8vdWF0LnVycy5lYXJ0aGRhdGEubmFzYS5nb3YifQ.E2NQ3ZwN3n440M1cWNsl0kkjl61a_6vcSlUW0Ef1NTRqWneioTFu9R09eXhdvj3yy2_j7YadZBbPoi-UNVLSq6KZ8IW-NBkOcnx4izhWxluoYkZ0lcB5V8UNhGh2meX-VVoTROitms5X0InRWNyhg6OzAvyBpD7JCRH-erO-NZ9FsPucrSP6vwT0NgvUUOs2tKAvQ2-0meoX9zELL63M47qBgbcgOt4Bh1VQRqoAONXwubGLT-bGf1RVnV_L3xscryp6kbbAO8v6ORnyzNfFxuX5Oc6Kuko2EzGUbXXoBmGOef0BZnjIl7eBmspvClr0hzYOSX4DkeU-giGAt_JAhg";
        final int MAX_ITERATIONS = 100;
        final int SLEEP_INTERVAL = 3000;

        JSONObject jsonResponse = null;
        JSONArray tableArray = null;
        JTable jTable = null;
        try {
            // Fetch the webContent of the webpage
            //String webContent = fetchContent(url, bearerToken);

            // Parse the response as JSON
            jsonResponse = new JSONObject(webContent);
            boolean done = false;
            int counter = 0;
            String jobId = jsonResponse.getString("jobID");
            String jobUrl = "https://harmony.uat.earthdata.nasa.gov/jobs/" + jobId;

            while(!done && counter < MAX_ITERATIONS) {
                counter++;
                Thread.sleep(SLEEP_INTERVAL);

                //webContent = fetchContent(jobUrl, bearerToken);
                jsonResponse = new JSONObject(webContent);

                // Check if the "status" field is equal to "successful"
                if (jsonResponse.has("status") && "successful".equals(jsonResponse.getString("status"))) {
                    done = true;
                }
            }

            if(done) {
                System.out.println("Status is successful!");
                tableArray = (JSONArray) jsonResponse.get("links");
                System.out.println("Response: " + jsonResponse.toString(4)); // Pretty print the JSON
                jTable = getJTableNew(tableArray);
            } else {
                // Job did not finish after MAX_ITERATIONS * SLEEP_INTERVAL milliseconds
                // output error message
            }
        } catch (Exception e) {
            System.err.println("Error fetching webpage: " + e.getMessage());
        }

        return jTable;

    }


    public JTable getJTableNew(JSONArray dataArray){

        //String[] columnNames = { "Rel", "HREF", "Title", "Temporal", "BBox" };
        String[] columnNames = { "Title", "HREF"  };
        Object[][] dataSearchResult = new Object[dataArray.length()][2];


        // Iterate over the students array and add each row to the searchResultTable
        for (int i = 0; i < dataArray.length(); i++) {
            JSONObject dataURL = dataArray.getJSONObject(i);
            if (dataURL.has("temporal") ) {
                dataSearchResult[i][0] = dataURL.getString("title");
                dataSearchResult[i][1] = dataURL.getString("href");
            }
        }

        DefaultTableModel model = new DefaultTableModel(dataSearchResult, columnNames);

        JTable searchResultTable = new JTable(model);

        // Set a custom renderer for the 'Link' column to display as a clickable link
        searchResultTable.getColumnModel().getColumn(1).setCellRenderer(new LinkCellRenderer());

        // Add a mouse listener to detect clicks on the link
        searchResultTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int row = searchResultTable.rowAtPoint(e.getPoint());
                int col = searchResultTable.columnAtPoint(e.getPoint());

                if (col == 1) { // If the link column is clicked
                    String url = (String) searchResultTable.getValueAt(row, col);
                    if (url != null && !url.isEmpty()) { // Ensure the URL is not null or empty
                        try {
                            // Open the link in the default browser
                            Desktop.getDesktop().browse(new URI(url));
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    } else {
                        System.out.println("No valid URL in this cell.");
                    }
                }
            }
        });

        removeEmptyRows(model);
        return searchResultTable;
    }
}

// Custom TableCellRenderer to display clickable links in the table
class LinkCellRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (value != null) {  // Check if value is not null
            String link = value.toString();
            label.setText("<html><a href=''>" + link + "</a></html>");
        } else {
            label.setText("No link"); // Handle null values
        }
        return label;
    }
}