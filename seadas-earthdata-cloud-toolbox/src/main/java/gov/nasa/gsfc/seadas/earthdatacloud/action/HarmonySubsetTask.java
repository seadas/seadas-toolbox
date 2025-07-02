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
            
            // Get authentication token from production endpoint
            System.out.println("Getting authentication token...");
            String bearerToken = WebPageFetcherWithJWT.getAccessToken("urs.earthdata.nasa.gov");
            if (bearerToken != null && bearerToken.length() > 12) {
                System.out.println("Bearer token (start/end): " + bearerToken.substring(0, 6) + "..." + bearerToken.substring(bearerToken.length() - 6));
            } else {
                System.out.println("Bearer token is null or too short to display.");
            }
            
            // Build Harmony URL using OGC API format
            System.out.println("Building Harmony OGC API URL...");
            String harmonyUrl = buildHarmonySubsetUrl();
            System.out.println("Harmony URL: " + harmonyUrl);
            
            // Log the collection ID being used
            String collectionId = harmonyUrl.substring(harmonyUrl.indexOf("harmony.earthdata.nasa.gov/") + 30);
            if (collectionId.contains("/")) {
                collectionId = collectionId.substring(0, collectionId.indexOf('/'));
            }
            System.out.println("Using collection ID: " + collectionId);
            
            // Use GET request for OGC API
            String response = fetchContent(harmonyUrl, bearerToken);
            System.out.println("Harmony response: " + response.substring(0, Math.min(200, response.length())) + "...");
            
            // For OGC API, the response is typically the data itself or a job status
            // We'll need to handle this differently than the previous JSON response
            if (response.contains("jobID") || response.contains("status")) {
                // This might be a job response
                JSONObject jsonResponse = new JSONObject(response);
                if (jsonResponse.has("jobID")) {
                    String jobId = jsonResponse.getString("jobID");
                    String jobUrl = "https://harmony.uat.earthdata.nasa.gov/jobs/" + jobId;
                    
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
            } else {
                // This might be direct data or an error response
                System.out.println("Direct response received (likely data or error)");
                dialog.updateStatus("Subset request completed");
                
                // Create a simple response object
                JSONObject result = new JSONObject();
                result.put("response", response);
                result.put("url", harmonyUrl);
                return result;
            }
            
        } catch (Exception e) {
            System.err.println("Exception in HarmonySubsetTask.doInBackground(): " + e.getMessage());
            e.printStackTrace();
            dialog.updateStatus("Error during subset request: " + e.getMessage());
            throw e;
        }
    }

    private String buildHarmonySubsetUrl() {
        // Extract collection ID from the file URL
        String inputUrl = subsetParameters.getString("url");
        
        // Try to determine collection ID based on file content/name patterns (this now includes CMR lookup)
        String collectionId = determineCollectionIdFromFileName(inputUrl);
        
        if (collectionId == null) {
            // Prompt user for manual entry if all else fails
            String userInput = javax.swing.JOptionPane.showInputDialog(null, 
                "Could not determine collection ID for file:\n" + inputUrl + 
                "\n\nPlease enter the collection concept ID (must end with -OB_CLOUD):" +
                "\n• PACE OCI L2: C3385050043-OB_CLOUD" +
                "\n• PACE OCI L3 CHL: C3300834690-OB_CLOUD" +
                "\n• MODIS: C3020920290-OB_CLOUD" +
                "\n• VIIRS: C1940468264-OB_CLOUD", 
                "Enter Collection ID", 
                javax.swing.JOptionPane.QUESTION_MESSAGE);
            if (userInput != null && !userInput.trim().isEmpty()) {
                collectionId = userInput.trim();
                // Validate that it ends with OB_CLOUD
                if (!collectionId.endsWith("-OB_CLOUD")) {
                    throw new RuntimeException("Invalid collection ID. All collections must end with '-OB_CLOUD'. Got: " + collectionId);
                }
            } else {
                throw new RuntimeException("Could not determine collection ID for file URL: " + inputUrl);
            }
        }

        System.out.println("Using collection ID: " + collectionId);

        // Extract granule ID from the file URL if possible
        String granuleId = extractGranuleIdFromUrl(inputUrl);
        
        // Build the OGC API Coverages URL
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append("https://harmony.earthdata.nasa.gov/")
                  .append(collectionId)
                  .append("/ogc-api-coverages/1.0.0/collections/all/coverage/rangeset");
        
        // Add query parameters
        boolean firstParam = true;

        // Add granule ID if available
        if (granuleId != null) {
            urlBuilder.append(firstParam ? "?" : "&").append("granuleId=").append(granuleId);
            firstParam = false;
        }

        // Add spatial subset using OGC API format
        if (subsetParameters.has("lonMin") && subsetParameters.has("latMin") &&
            subsetParameters.has("lonMax") && subsetParameters.has("latMax")) {
            String latSubset = String.format("subset=lat(%s:%s)",
                subsetParameters.getString("latMin"),
                subsetParameters.getString("latMax"));
            String lonSubset = String.format("subset=lon(%s:%s)",
                subsetParameters.getString("lonMin"),
                subsetParameters.getString("lonMax"));
            urlBuilder.append(firstParam ? "?" : "&").append(latSubset);
            urlBuilder.append("&").append(lonSubset);
            firstParam = false;
        }

        // Add temporal subset if explicitly requested (optional - many collections don't support it)
        if (subsetParameters.has("includeTemporal") && subsetParameters.getBoolean("includeTemporal") && 
            subsetParameters.has("startDate") && subsetParameters.has("endDate")) {
            try {
                String startDate = subsetParameters.getString("startDate") + "T00:00:00.000Z";
                String endDate = subsetParameters.getString("endDate") + "T23:59:59.999Z";
                urlBuilder.append(firstParam ? "?" : "&").append("subset=time(\"").append(startDate).append("\":\"").append(endDate).append("\")");
                firstParam = false;
                System.out.println("Added temporal subset: " + startDate + " to " + endDate);
            } catch (Exception e) {
                System.out.println("Warning: Could not add temporal subset: " + e.getMessage());
                // Continue without temporal subset
            }
        } else {
            System.out.println("Skipping temporal subset (not requested or not supported by this collection)");
        }

        // Note: Removed variables, format, and crs parameters to match working example
        // The working example only uses: granuleId, subset, skipPreview, pixelSubset

        // Add standard parameters for better performance
        urlBuilder.append(firstParam ? "?" : "&").append("skipPreview=true&pixelSubset=True");

        return urlBuilder.toString();
    }
    

    
    /**
     * Determine collection ID based on file name patterns
     */
    private String determineCollectionIdFromFileName(String fileUrl) {
        String fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
        System.out.println("Analyzing file name: " + fileName);
        
        // PACE OCI patterns - use production collection IDs
        if (fileName.contains("PACE_OCI") || fileName.contains("OCI")) {
            // For production, use available PACE collections
            if (fileName.contains("L2")) {
                // First try to get the actual collection ID from CMR for this specific granule
                String actualCollectionId = getCollectionIdForGranule(fileName);
                if (actualCollectionId != null) {
                    System.out.println("Found actual collection ID for granule: " + actualCollectionId);
                    return actualCollectionId;
                }
                System.out.println("Detected PACE OCI L2 file, using production collection: C3385050043-OB_CLOUD");
                return "C3385050043-OB_CLOUD"; // From working example
            } else if (fileName.contains("L3") && fileName.contains("CHL")) {
                System.out.println("Detected PACE OCI L3 CHL file, using production collection: C3300834690-OB_CLOUD");
                return "C3300834690-OB_CLOUD"; // From working example
            } else {
                System.out.println("Detected PACE OCI file, using default production collection: C3385050043-OB_CLOUD");
                return "C3385050043-OB_CLOUD";
            }
        }
        
        // MODIS patterns - use production collection IDs
        if (fileName.contains("MODIS") || fileName.contains("AQUA") || fileName.contains("TERRA")) {
            System.out.println("Detected MODIS file, using production collection: C3020920290-OB_CLOUD");
            return "C3020920290-OB_CLOUD";
        }
        
        // VIIRS patterns - use production collection IDs
        if (fileName.contains("VIIRS") || fileName.contains("SNPP") || fileName.contains("JPSS")) {
            System.out.println("Detected VIIRS file, using production collection: C1940468264-OB_CLOUD");
            return "C1940468264-OB_CLOUD";
        }
        
        // Default to a working production collection
        System.out.println("Using default production collection: C3385050043-OB_CLOUD");
        return "C3385050043-OB_CLOUD";
    }

    /**
     * Get the actual collection ID for a specific granule from CMR
     */
    private String getCollectionIdForGranule(String fileName) {
        try {
            String cmrUrl = "https://cmr.earthdata.nasa.gov/search/granules.json?readable_granule_name=" + fileName + "&provider=OB_CLOUD";
            System.out.println("Looking up collection ID for granule: " + cmrUrl);
            
            java.net.URL url = new java.net.URL(cmrUrl);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int status = conn.getResponseCode();
            if (status == 200) {
                java.io.InputStream is = conn.getInputStream();
                java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
                String response = s.hasNext() ? s.next() : "";
                org.json.JSONObject json = new org.json.JSONObject(response);
                org.json.JSONArray entries = json.getJSONObject("feed").getJSONArray("entry");
                
                if (entries.length() > 0) {
                    String collectionId = entries.getJSONObject(0).getString("collection_concept_id");
                    System.out.println("Found collection ID for granule: " + collectionId);
                    return collectionId;
                } else {
                    System.out.println("No granule found in CMR for file: " + fileName);
                }
            } else {
                System.err.println("CMR request failed with status: " + status);
            }
        } catch (Exception e) {
            System.err.println("Error getting collection ID for granule: " + e.getMessage());
        }
        return null;
    }

    private String buildHarmonyJobRequest() {
        JSONObject jobRequest = new JSONObject();
        
        // Extract collection ID from input URL
        String inputUrl = subsetParameters.getString("url");
        String collectionId = extractCollectionId(inputUrl);
        
        // Build the job request
        jobRequest.put("collection", collectionId);
        
        // Add subset parameters
        JSONObject subset = new JSONObject();
        
        // Spatial subset
        if (subsetParameters.has("latMin") && subsetParameters.has("latMax") && 
            subsetParameters.has("lonMin") && subsetParameters.has("lonMax")) {
            JSONObject spatial = new JSONObject();
            spatial.put("lat", new JSONArray().put(subsetParameters.getDouble("latMin")).put(subsetParameters.getDouble("latMax")));
            spatial.put("lon", new JSONArray().put(subsetParameters.getDouble("lonMin")).put(subsetParameters.getDouble("lonMax")));
            subset.put("spatial", spatial);
        }
        
        // Temporal subset
        if (subsetParameters.has("startDate") && subsetParameters.has("endDate")) {
            String startDate = subsetParameters.getString("startDate") + "T00:00:00Z";
            String endDate = subsetParameters.getString("endDate") + "T23:59:59Z";
            subset.put("temporal", new JSONArray().put(startDate).put(endDate));
        }
        
        // Variables
        if (subsetParameters.has("variables")) {
            subset.put("variables", subsetParameters.getJSONArray("variables"));
        }
        
        if (subset.length() > 0) {
            jobRequest.put("subset", subset);
        }
        
        // Output options
        if (subsetParameters.has("format")) {
            jobRequest.put("format", subsetParameters.getString("format"));
        }
        
        if (subsetParameters.has("crs")) {
            jobRequest.put("crs", subsetParameters.getString("crs"));
        }
        
        return jobRequest.toString();
    }

    private String extractCollectionId(String inputUrl) {
        System.out.println("Extracting collection ID from URL: " + inputUrl);
        
        // First try to extract from URL using regex
        String collectionId = gov.nasa.gsfc.seadas.earthdatacloud.ui.HarmonySubsetServiceDialog.extractCollectionIdFromUrl(inputUrl);
        
        // Use the extracted collection ID if available
        if (collectionId != null) {
            System.out.println("Extracted collection ID from URL: " + collectionId);
            return collectionId;
        }
        
        // Try to determine based on file name patterns
        collectionId = determineCollectionIdFromFileName(inputUrl);
        if (collectionId != null) {
            System.out.println("Determined collection ID from file name: " + collectionId);
            return collectionId;
        }
        
        // If all else fails, use a reasonable default
        System.out.println("Using default production collection: C3385050043-OB_CLOUD");
        return "C3385050043-OB_CLOUD";
    }

    private JSONObject pollJobCompletion(String jobUrl, String bearerToken) throws Exception {
        System.out.println("Polling job completion at: " + jobUrl);
        dialog.updateStatus("Polling job status...");
        
        int attempts = 0;
        final int MAX_ITERATIONS = 60; // 5 minutes with 5-second intervals
        boolean done = false;
        JSONObject jsonResponse = null;
        
        while (attempts < MAX_ITERATIONS && !done) {
            attempts++;
            System.out.println("Polling attempt " + attempts + "/" + MAX_ITERATIONS);
            
            try {
                Thread.sleep(5000); // Wait 5 seconds between polls
                
                String response = fetchContent(jobUrl, bearerToken);
                jsonResponse = new JSONObject(response);
                
                String status = jsonResponse.getString("status");
                System.out.println("Job status: " + status);
                dialog.updateStatus("Job status: " + status);
                
                if ("successful".equals(status)) {
                    done = true;
                    System.out.println("Job completed successfully");
                } else if ("failed".equals(status)) {
                    done = true;
                    String message = jsonResponse.has("message") ? jsonResponse.getString("message") : "Job failed";
                    throw new Exception("Job failed: " + message);
                } else if ("running".equals(status) || "accepted".equals(status)) {
                    // Continue polling
                    progressBar.setValue((attempts * 100) / MAX_ITERATIONS);
                } else {
                    // Unknown status, continue polling
                    System.out.println("Unknown status: " + status + ", continuing to poll");
                }
                
            } catch (Exception e) {
                System.err.println("Error during polling: " + e.getMessage());
                if (attempts >= MAX_ITERATIONS) {
                    throw e;
                }
            }
        }
        
        if (!done) {
            throw new Exception("Job polling timed out after " + MAX_ITERATIONS + " attempts");
        }
        
        return jsonResponse;
    }

    public static String createJob(String urlString, String jsonData, String bearerToken) throws Exception {
        StringBuilder content = new StringBuilder();
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "Bearer " + bearerToken);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");
        connection.setDoOutput(true);

        // Write JSON data
        try (java.io.OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonData.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        int status = connection.getResponseCode();
        if (status == HttpURLConnection.HTTP_OK || status == HttpURLConnection.HTTP_CREATED) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine).append("\n");
                }
            }
        } else if (status == HttpURLConnection.HTTP_UNAUTHORIZED || status == HttpURLConnection.HTTP_FORBIDDEN) {
            throw new Exception("Authentication failed (" + status + "). Please check your Earthdata Login credentials and token.");
        } else if (status == HttpURLConnection.HTTP_NOT_FOUND) {
            // Read error response for better debugging
            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine).append("\n");
                }
            } catch (Exception e) {
                content.append("Could not read error response: ").append(e.getMessage());
            }
            
            // Extract collection ID from URL for better error message
            String collectionId = urlString.substring(urlString.lastIndexOf('/') + 1);
            if (collectionId.contains("?")) {
                collectionId = collectionId.substring(0, collectionId.indexOf('?'));
            }
            
            throw new Exception("Collection not found (404). The collection ID '" + collectionId + "' may be invalid or the collection may not be available in Harmony. " +
                              "All collections must be from the OB_CLOUD provider (ending with -OB_CLOUD). " +
                              "Please verify the collection ID or try a different collection. Response: " + content.toString());
        } else {
            // Read error response for better debugging
            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine).append("\n");
                }
            } catch (Exception e) {
                content.append("Could not read error response: ").append(e.getMessage());
            }
            throw new Exception("Failed to create job. HTTP status code: " + status + ". Response: " + content.toString());
        }
        return content.toString();
    }

    /**
     * Fetch content from URL using GET request
     */
    public static String fetchContent(String urlString, String bearerToken) throws Exception {
        StringBuilder content = new StringBuilder();
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("GET");
        if (bearerToken != null) {
            connection.setRequestProperty("Authorization", "Bearer " + bearerToken);
        }
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");
        connection.setRequestProperty("Accept", "*/*");
        connection.setInstanceFollowRedirects(true);

        int status = connection.getResponseCode();
        if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == HttpURLConnection.HTTP_SEE_OTHER) {
            String newUrl = connection.getHeaderField("Location");
            connection = (HttpURLConnection) new URL(newUrl).openConnection();
            connection.setRequestMethod("GET");
            if (bearerToken != null) {
                connection.setRequestProperty("Authorization", "Bearer " + bearerToken);
            }
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setRequestProperty("Accept", "*/*");
            status = connection.getResponseCode();
        }

        System.out.println("Response Code: " + status);

        if (status >= 400) {
            // Read error stream
            try (java.io.BufferedReader in = new java.io.BufferedReader(
                    new java.io.InputStreamReader(connection.getErrorStream()))) {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
            }
            throw new Exception("HTTP request failed with status: " + status + ". Response: " + content.toString());
        }

        // Read response
        try (java.io.BufferedReader in = new java.io.BufferedReader(
                new java.io.InputStreamReader(connection.getInputStream()))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
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

    /**
     * Extract granule ID from file URL by querying CMR
     */
    private String extractGranuleIdFromUrl(String fileUrl) {
        try {
            String fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
            System.out.println("Extracting granule ID for file: " + fileName);
            
            // Query CMR to get the granule ID
            String cmrUrl = "https://cmr.earthdata.nasa.gov/search/granules.json?readable_granule_name=" + fileName + "&provider=OB_CLOUD";
            System.out.println("CMR granule lookup URL: " + cmrUrl);
            
            java.net.URL url = new java.net.URL(cmrUrl);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int status = conn.getResponseCode();
            if (status == 200) {
                java.io.InputStream is = conn.getInputStream();
                java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
                String response = s.hasNext() ? s.next() : "";
                org.json.JSONObject json = new org.json.JSONObject(response);
                org.json.JSONArray entries = json.getJSONObject("feed").getJSONArray("entry");
                
                if (entries.length() > 0) {
                    String granuleId = entries.getJSONObject(0).getString("id");
                    System.out.println("Found granule ID: " + granuleId);
                    return granuleId;
                } else {
                    System.out.println("No granule found in CMR for file: " + fileName);
                }
            } else {
                System.err.println("CMR request failed with status: " + status);
                // Try to read error response
                try {
                    java.io.InputStream is = conn.getErrorStream();
                    if (is != null) {
                        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
                        String errorResponse = s.hasNext() ? s.next() : "";
                        System.err.println("CMR error response: " + errorResponse);
                    }
                } catch (Exception e) {
                    System.err.println("Could not read CMR error response: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Error extracting granule ID: " + e.getMessage());
        }
        
        System.out.println("Could not extract granule ID, will proceed without it");
        return null;
    }
} 