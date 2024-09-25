package gov.nasa.gsfc.seadas.earthdatacloud.action;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutionException;

public class DataRetrievalTask extends SwingWorker<Void, Void> {
    private JProgressBar progressBar;
    private JLabel label;
    private String urlString;
    private String bearerToken;
    private StringBuilder content;
    private JButton startButton;
    private JButton cancelButton;
    private JScrollPane scrollPane;

    // Constructor to pass the components
    public DataRetrievalTask(JProgressBar progressBar, JLabel label, JButton button) {
        this.progressBar = progressBar;
        this.label = label;
        urlString = "https://harmony.uat.earthdata.nasa.gov/C1265136924-OB_CLOUD/ogc-api-coverages/1.0.0/collections/all/coverage/rangeset"; // Replace with your URL
        bearerToken = "eyJ0eXAiOiJKV1QiLCJvcmlnaW4iOiJFYXJ0aGRhdGEgTG9naW4iLCJzaWciOiJlZGxqd3RwdWJrZXlfdWF0IiwiYWxnIjoiUlMyNTYifQ.eyJ0eXBlIjoiVXNlciIsInVpZCI6ImF5bnVyIiwiZXhwIjoxNzMxMDI1Mzc5LCJpYXQiOjE3MjU4NDEzNzksImlzcyI6Imh0dHBzOi8vdWF0LnVycy5lYXJ0aGRhdGEubmFzYS5nb3YifQ.E2NQ3ZwN3n440M1cWNsl0kkjl61a_6vcSlUW0Ef1NTRqWneioTFu9R09eXhdvj3yy2_j7YadZBbPoi-UNVLSq6KZ8IW-NBkOcnx4izhWxluoYkZ0lcB5V8UNhGh2meX-VVoTROitms5X0InRWNyhg6OzAvyBpD7JCRH-erO-NZ9FsPucrSP6vwT0NgvUUOs2tKAvQ2-0meoX9zELL63M47qBgbcgOt4Bh1VQRqoAONXwubGLT-bGf1RVnV_L3xscryp6kbbAO8v6ORnyzNfFxuX5Oc6Kuko2EzGUbXXoBmGOef0BZnjIl7eBmspvClr0hzYOSX4DkeU-giGAt_JAhg";
    }

    public DataRetrievalTask(JProgressBar progressBar, JLabel label, JButton startButton, JButton cancelButton, JScrollPane scrollPane) {
        this.progressBar = progressBar;
        this.label = label;
        this.startButton = startButton;
        this.cancelButton = cancelButton;
        this.scrollPane = scrollPane;
        urlString = "https://harmony.uat.earthdata.nasa.gov/C1265136924-OB_CLOUD/ogc-api-coverages/1.0.0/collections/all/coverage/rangeset"; // Replace with your URL
        bearerToken = "eyJ0eXAiOiJKV1QiLCJvcmlnaW4iOiJFYXJ0aGRhdGEgTG9naW4iLCJzaWciOiJlZGxqd3RwdWJrZXlfdWF0IiwiYWxnIjoiUlMyNTYifQ.eyJ0eXBlIjoiVXNlciIsInVpZCI6ImF5bnVyIiwiZXhwIjoxNzMxMDI1Mzc5LCJpYXQiOjE3MjU4NDEzNzksImlzcyI6Imh0dHBzOi8vdWF0LnVycy5lYXJ0aGRhdGEubmFzYS5nb3YifQ.E2NQ3ZwN3n440M1cWNsl0kkjl61a_6vcSlUW0Ef1NTRqWneioTFu9R09eXhdvj3yy2_j7YadZBbPoi-UNVLSq6KZ8IW-NBkOcnx4izhWxluoYkZ0lcB5V8UNhGh2meX-VVoTROitms5X0InRWNyhg6OzAvyBpD7JCRH-erO-NZ9FsPucrSP6vwT0NgvUUOs2tKAvQ2-0meoX9zELL63M47qBgbcgOt4Bh1VQRqoAONXwubGLT-bGf1RVnV_L3xscryp6kbbAO8v6ORnyzNfFxuX5Oc6Kuko2EzGUbXXoBmGOef0BZnjIl7eBmspvClr0hzYOSX4DkeU-giGAt_JAhg";
    }


    @Override
    protected Void doInBackground() throws Exception {
        try {
            setContent(new StringBuilder());
            URL url = new URL(getUrlString());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            // Set request method to GET and add Bearer token to the Authorization header
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + getBearerToken());
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
                connection.setRequestProperty("Authorization", "Bearer " + getBearerToken());
                connection.setRequestProperty("User-Agent", "Mozilla/5.0");
                status = connection.getResponseCode();
            }
            int bytesRead = 0;
            int progress = 0;

            int contentLength = connection.getContentLength();
            // Check if the response is OK (200)
            if (status == HttpURLConnection.HTTP_OK) {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        content.append(inputLine).append("\n");
                        if (contentLength > 0) { // If content length is available
                            progress = (int) ((double) bytesRead / contentLength * 100);
                            setProgress(progress); // Update progress
                        }
                    }
                }
            } else {
                throw new Exception("Failed to fetch content. HTTP status code: " + status);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void process(java.util.List<Void> chunks) {
        // This method can be used to update UI periodically if needed
    }

    @Override
    protected void done() {
        if (!isCancelled()) {
            progressBar.setValue(100); // Ensure progress bar is full
            label.setText("Download complete!");
        }
        cancelButton.setEnabled(false);
        startButton.setEnabled(true);
    }

    public String getUrlString() {
        return urlString;
    }

    public void setUrlString(String urlString) {
        this.urlString = urlString;
    }

    public String getBearerToken() {
        return bearerToken;
    }

    public void setBearerToken(String bearerToken) {
        this.bearerToken = bearerToken;
    }

    public String getContent() {
        return content.toString();
    }

    public void setContent(StringBuilder content) {
        this.content = content;
    }

    public JScrollPane getScrollPane() {
        return scrollPane;
    }
}
