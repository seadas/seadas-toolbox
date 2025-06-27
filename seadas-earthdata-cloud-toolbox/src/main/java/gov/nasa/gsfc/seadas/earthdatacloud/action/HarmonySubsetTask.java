package gov.nasa.gsfc.seadas.earthdatacloud.action;

import gov.nasa.gsfc.seadas.earthdatacloud.auth.WebPageFetcherWithJWT;
import gov.nasa.gsfc.seadas.earthdatacloud.ui.HarmonySubsetServiceDialog;
import gov.nasa.gsfc.seadas.earthdatacloud.util.FileDownloadManager;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class HarmonySubsetTask extends SwingWorker<JSONObject, Void> {
    private JProgressBar progressBar;
    private JButton subsetButton;
    private JButton cancelButton;
    private HarmonySubsetServiceDialog dialog;
    private JSONObject subsetParameters;

    public HarmonySubsetTask(JSONObject subsetParameters, JProgressBar progressBar, 
                           JButton subsetButton, JButton cancelButton, 
                           HarmonySubsetServiceDialog dialog) {
        this.subsetParameters = subsetParameters;
        this.progressBar = progressBar;
        this.subsetButton = subsetButton;
        this.cancelButton = cancelButton;
        this.dialog = dialog;
    }

    @Override
    public JSONObject doInBackground() throws Exception {
        try {
            System.out.println("=== HarmonySubsetTask.doInBackground() started ===");
            dialog.updateStatus("Starting subset request...");
            
            // Get authentication token
            System.out.println("Getting authentication token...");
            String bearerToken = WebPageFetcherWithJWT.getAccessToken("urs.earthdata.nasa.gov");
            System.out.println("Authentication token obtained: " + (bearerToken != null ? "SUCCESS" : "FAILED"));
            
            // Build Harmony subset request URL
            System.out.println("Building Harmony subset URL...");
            String harmonyUrl = buildHarmonySubsetUrl();
            System.out.println("Harmony URL: " + harmonyUrl);
            dialog.updateStatus("Requesting subset from Harmony: " + harmonyUrl);
            
            // Make initial request
            System.out.println("Making initial request to Harmony...");
            String response = fetchContent(harmonyUrl, bearerToken);
            System.out.println("Response received: " + response.substring(0, Math.min(200, response.length())) + "...");
            JSONObject jsonResponse = new JSONObject(response);
            
            // Check if we got a job ID (asynchronous processing)
            if (jsonResponse.has("jobID")) {
                String jobId = jsonResponse.getString("jobID");
                String jobUrl = "https://harmony.earthdata.nasa.gov/jobs/" + jobId;
                
                System.out.println("Job ID received: " + jobId);
                dialog.updateStatus("Subset job created. Job ID: " + jobId);
                
                // Poll for job completion
                return pollJobCompletion(jobUrl, bearerToken);
            } else {
                // Synchronous response
                System.out.println("Synchronous response received");
                dialog.updateStatus("Subset completed synchronously");
                return jsonResponse;
            }
            
        } catch (Exception e) {
            System.err.println("Exception in HarmonySubsetTask.doInBackground(): " + e.getMessage());
            e.printStackTrace();
            dialog.updateStatus("Error during subset request: " + e.getMessage());
            throw e;
        }
    }

    private String buildHarmonySubsetUrl() {
        StringBuilder urlBuilder = new StringBuilder();
        
        // Base URL - we'll need to extract collection ID from the input URL
        String inputUrl = subsetParameters.getString("url");
        String collectionId = extractCollectionId(inputUrl);
        
        urlBuilder.append("https://harmony.earthdata.nasa.gov/")
                 .append(collectionId)
                 .append("/ogc-api-coverages/1.0.0/collections/all/coverage/rangeset");
        
        // Add subset parameters
        boolean firstParam = true;
        
        // Spatial subset
        if (subsetParameters.has("latMin") && subsetParameters.has("latMax")) {
            String latMin = subsetParameters.getString("latMin");
            String latMax = subsetParameters.getString("latMax");
            urlBuilder.append(firstParam ? "?" : "&")
                     .append("subset=lat(").append(latMin).append(":").append(latMax).append(")");
            firstParam = false;
        }
        
        if (subsetParameters.has("lonMin") && subsetParameters.has("lonMax")) {
            String lonMin = subsetParameters.getString("lonMin");
            String lonMax = subsetParameters.getString("lonMax");
            urlBuilder.append(firstParam ? "?" : "&")
                     .append("subset=lon(").append(lonMin).append(":").append(lonMax).append(")");
            firstParam = false;
        }
        
        // Temporal subset
        if (subsetParameters.has("startDate") && subsetParameters.has("endDate")) {
            String startDate = subsetParameters.getString("startDate") + "T00:00:00Z";
            String endDate = subsetParameters.getString("endDate") + "T23:59:59Z";
            urlBuilder.append(firstParam ? "?" : "&")
                     .append("subset=time(\"").append(startDate).append("\":\"").append(endDate).append("\")");
            firstParam = false;
        }
        
        // Variables
        if (subsetParameters.has("variables")) {
            List<String> variables = (List<String>) subsetParameters.get("variables");
            if (!variables.isEmpty()) {
                String varsString = String.join(",", variables);
                urlBuilder.append(firstParam ? "?" : "&")
                         .append("variables=").append(varsString);
                firstParam = false;
            }
        }
        
        // Output format
        if (subsetParameters.has("format")) {
            String format = subsetParameters.getString("format");
            urlBuilder.append(firstParam ? "?" : "&")
                     .append("format=").append(format);
            firstParam = false;
        }
        
        // Coordinate system
        if (subsetParameters.has("crs")) {
            String crs = subsetParameters.getString("crs");
            urlBuilder.append(firstParam ? "?" : "&")
                     .append("crs=").append(crs);
        }
        
        return urlBuilder.toString();
    }

    private String extractCollectionId(String inputUrl) {
        // Extract collection ID from input URL
        // This is a simplified approach - in practice, you might need more sophisticated parsing
        if (inputUrl.contains("C3020920290-OB_CLOUD")) {
            return "C3020920290-OB_CLOUD";
        } else if (inputUrl.contains("C1265136924-OB_CLOUD")) {
            return "C1265136924-OB_CLOUD";
        } else {
            // Default collection for ocean color data
            return "C3020920290-OB_CLOUD";
        }
    }

    private JSONObject pollJobCompletion(String jobUrl, String bearerToken) throws Exception {
        final int MAX_ITERATIONS = 1000;
        final int SLEEP_INTERVAL = 2000; // 2 seconds
        
        int counter = 0;
        boolean done = false;
        JSONObject jsonResponse = null;
        
        while (!done && counter < MAX_ITERATIONS && !isCancelled()) {
            counter++;
            
            // Update progress
            int progress = Math.min(counter * 2, 95); // Progress up to 95%
            progressBar.setValue(progress);
            dialog.updateStatus("Polling job status... (attempt " + counter + ")");
            
            // Sleep before next poll
            Thread.sleep(SLEEP_INTERVAL);
            
            // Check job status
            String response = fetchContent(jobUrl, bearerToken);
            jsonResponse = new JSONObject(response);
            
            if (jsonResponse.has("status")) {
                String status = jsonResponse.getString("status");
                if ("successful".equals(status)) {
                    done = true;
                    dialog.updateStatus("Job completed successfully!");
                } else if ("failed".equals(status)) {
                    String message = jsonResponse.has("message") ? jsonResponse.getString("message") : "Unknown error";
                    throw new Exception("Job failed: " + message);
                } else if ("running".equals(status)) {
                    // Continue polling
                    if (jsonResponse.has("progress")) {
                        int jobProgress = jsonResponse.getInt("progress");
                        progressBar.setValue(jobProgress);
                    }
                }
            }
        }
        
        if (!done) {
            throw new Exception("Job polling timed out after " + MAX_ITERATIONS + " attempts");
        }
        
        return jsonResponse;
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
        if (status == HttpURLConnection.HTTP_MOVED_TEMP || 
            status == HttpURLConnection.HTTP_MOVED_PERM || 
            status == HttpURLConnection.HTTP_SEE_OTHER) {
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

    @Override
    protected void process(java.util.List<Void> chunks) {
        // Not used in this implementation
    }

    @Override
    protected void done() {
        if (isCancelled()) {
            progressBar.setValue(0);
            progressBar.setString("Cancelled");
            dialog.updateStatus("Subset request was cancelled");
            dialog.subsetCompleted(false, "Request was cancelled");
        } else {
            try {
                JSONObject result = get();
                progressBar.setValue(100);
                progressBar.setString("Completed");
                
                // Handle successful completion
                if (result != null && result.has("links")) {
                    JSONArray links = result.getJSONArray("links");
                    dialog.updateStatus("Subset completed! Found " + links.length() + " result files.");
                    
                    // Download the results
                    downloadResults(links);
                    
                    dialog.subsetCompleted(true, "Subset completed successfully");
                } else {
                    dialog.subsetCompleted(false, "No results found in response");
                }
                
            } catch (Exception e) {
                progressBar.setValue(0);
                progressBar.setString("Failed");
                dialog.updateStatus("Subset failed: " + e.getMessage());
                dialog.subsetCompleted(false, e.getMessage());
            }
        }
    }

    private void downloadResults(JSONArray links) {
        try {
            dialog.updateStatus("Preparing to download " + links.length() + " files...");
            
            // Extract download URLs and create a map
            java.util.Map<String, String> fileLinkMap = new java.util.HashMap<>();
            java.util.List<String> filesToDownload = new java.util.ArrayList<>();
            
            for (int i = 0; i < links.length(); i++) {
                JSONObject link = links.getJSONObject(i);
                if (link.has("href") && link.has("title")) {
                    String downloadUrl = link.getString("href");
                    String fileName = link.getString("title");
                    
                    fileLinkMap.put(fileName, downloadUrl);
                    filesToDownload.add(fileName);
                }
            }
            
            // Use the existing FileDownloadManager to download the files
            FileDownloadManager downloadManager = new FileDownloadManager();
            downloadManager.downloadSelectedFiles(filesToDownload, fileLinkMap, dialog, 
                (downloadedCount, downloadDir) -> {
                    dialog.updateStatus("Downloaded " + downloadedCount + " files to: " + downloadDir);
                });
            
        } catch (Exception e) {
            dialog.updateStatus("Error during download: " + e.getMessage());
        }
    }
} 