package gov.nasa.gsfc.seadas.earthdatacloud.action;

import gov.nasa.gsfc.seadas.earthdatacloud.auth.WebPageFetcherWithJWT;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

public class DataRetrievalTask extends SwingWorker<JSONObject, Void> {
    private JProgressBar progressBar;
    private String urlString;
    private String bearerToken;
    private JButton startButton;
    private JButton cancelButton;

    public DataRetrievalTask(JProgressBar progressBar, JButton startButton, JButton cancelButton) {
        this.progressBar = progressBar;
        this.startButton = startButton;
        this.cancelButton = cancelButton;
        urlString = "https://harmony.uat.earthdata.nasa.gov/C1265136924-OB_CLOUD/ogc-api-coverages/1.0.0/collections/all/coverage/rangeset"; // Replace with your URL
    }


    public static String fetchContent(String urlString, String bearerToken) throws Exception {
        StringBuilder content = new StringBuilder();
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", "Bearer " + bearerToken);
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");

        connection.setInstanceFollowRedirects(true);

        int status = connection.getResponseCode();
        if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == HttpURLConnection.HTTP_SEE_OTHER) {
            String newUrl = connection.getHeaderField("Location");
            connection = (HttpURLConnection) new URL(newUrl).openConnection();
            connection.setRequestProperty("Authorization", "Bearer " + bearerToken);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            status = connection.getResponseCode();
        }

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

    public JSONObject doInBackground() throws Exception {
        String webContent = null;
        JTable jTable = null;

        String url = "https://harmony.earthdata.nasa.gov/C3020920290-OB_CLOUD/ogc-api-coverages/1.0.0/collections/all/coverage/rangeset?maxresults=25";
        String bearerToken = WebPageFetcherWithJWT.getAccessToken("urs.earthdata.nasa.gov");
        final int MAX_ITERATIONS = 1000;
        final int SLEEP_INTERVAL = 10;

        JSONObject jsonResponse = null;
        JSONArray tableArray = null;

        try {

            webContent = fetchContent(url, bearerToken);
            jsonResponse = new JSONObject(webContent);
            boolean done = false;
            int counter = 0;
            String jobId;
            String jobUrl;
            if (jsonResponse.has("jobID")) {
                jobId = jsonResponse.getString("jobID");
                jobUrl = "https://harmony.earthdata.nasa.gov/jobs/" + jobId;
            } else {
                jobUrl = url;
            }

            int progress = jsonResponse.getInt("progress");

            while (!done && counter < MAX_ITERATIONS) {
                counter++;
                int progressIndicator = progress > 0 ? progress : counter % 100;
                progressBar.setValue(progressIndicator);

                webContent = fetchContent(jobUrl, bearerToken);
                jsonResponse = new JSONObject(webContent);
                progress = jsonResponse.getInt("progress");

                if (jsonResponse.has("status") && "successful".equals(jsonResponse.getString("status"))) {
                    done = true;
                }
            }

            if (done) {
                System.out.println("Status is successful!");
                tableArray = (JSONArray) jsonResponse.get("links");
                System.out.println("Response: " + jsonResponse.toString(4)); // Pretty print the JSON
                jTable = getJTableNew(tableArray);
            } else {
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return jsonResponse;
    }

    public JTable getJTableNew(JSONArray dataArray) {

        String[] columnNames = {"Title", "HREF"};
        Object[][] dataSearchResult = new Object[dataArray.length()][2];


        for (int i = 0; i < dataArray.length(); i++) {
            JSONObject dataURL = dataArray.getJSONObject(i);
            if (dataURL.has("temporal")) {
                dataSearchResult[i][0] = dataURL.getString("title");
                dataSearchResult[i][1] = dataURL.getString("href");
            }
        }

        DefaultTableModel model = new DefaultTableModel(dataSearchResult, columnNames);

        JTable searchResultTable = new JTable(model);

        searchResultTable.getColumnModel().getColumn(1).setCellRenderer(new LinkCellRenderer());

        searchResultTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int row = searchResultTable.rowAtPoint(e.getPoint());
                int col = searchResultTable.columnAtPoint(e.getPoint());

                if (col == 1) { // If the link column is clicked
                    String url = (String) searchResultTable.getValueAt(row, col);
                    if (url != null && !url.isEmpty()) { // Ensure the URL is not null or empty
                        try {
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

    public static void removeEmptyRows(DefaultTableModel model) {
        for (int row = model.getRowCount() - 1; row >= 0; row--) {
            boolean isEmpty = true;
            for (int col = 0; col < model.getColumnCount(); col++) {
                Object value = model.getValueAt(row, col);
                if (value != null && !value.toString().trim().isEmpty()) {
                    isEmpty = false; // Found a non-empty value
                    break;
                }
            }
            if (isEmpty) {
                model.removeRow(row);
            }
        }
    }


    @Override
    protected void process(java.util.List<Void> chunks) {
    }

    @Override
    protected void done() {
        if (!isCancelled()) {
            progressBar.setValue(100); // Ensure progress bar is full
            progressBar.setString("Search completed!");
        }
    }

    public String getUrlString() {
        return urlString;
    }

    public void setUrlString(String urlString) {
        this.urlString = urlString;
    }

    public void setBearerToken(String bearerToken) {
        this.bearerToken = bearerToken;
    }
}
