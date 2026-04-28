package gov.nasa.gsfc.seadas.earthdatacloud.util;

import gov.nasa.gsfc.seadas.earthdatacloud.auth.WebPageFetcherWithJWT;
import gov.nasa.gsfc.seadas.earthdatacloud.preferences.Earthdata_Cloud_Controller;
import org.esa.snap.core.util.SystemUtils;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages file downloads for Earthdata Cloud data files.
 * This class encapsulates all download-related functionality including
 * progress tracking, authentication, and file management.
 */
public class FileDownloadManager {
    
    private JDialog progressDialog;
    private JProgressBar progressBar;
    private String[] earthdataCredentials;

    String URL_LIST_FILENAME = "requested_download_files_url_list.txt";
    
    /**
     * Downloads multiple files with progress tracking and user feedback.
     * 
     * @param filesToDownload List of file names to download
     * @param fileLinkMap Map of file names to their download URLs
     * @param parentComponent Parent component for dialogs
     * @param onComplete Callback to execute when download completes
     */
    public void downloadSelectedFiles(List<String> filesToDownload,
                                    boolean downloadListOnly,
                                    java.util.Map<String, String> fileLinkMap,
                                    Component parentComponent,
                                    DownloadCompleteCallback onComplete) {


        // Check credentials
        if (earthdataCredentials == null) {
            earthdataCredentials = WebPageFetcherWithJWT.getCredentials("urs.earthdata.nasa.gov");
            if (earthdataCredentials == null) {
                JOptionPane.showMessageDialog(parentComponent, "Earthdata credentials not found in ~/.netrc");
                return;
            }
        }
        
        // Get download directory from user
        File requestedUrlsListFile = getRequestedUrlsListFile(parentComponent);
        if (requestedUrlsListFile == null) {
            return; // User cancelled
        }

        Path downloadDir = requestedUrlsListFile.getParentFile().toPath();
//                Path downloadDir = selectDownloadDirectory(parentComponent);
        if (downloadDir == null) {
            return; // User cancelled
        }
        


        // Create a listing of all file urls requested for download which will be saved as a text file in the requested
        // download directory
        String requestedUrlsListString = "";
        for (int i = 0; i < filesToDownload.size(); i++) {
            String fileName = filesToDownload.get(i);
            String url = fileLinkMap.get(fileName);


            if (url != null) {
                requestedUrlsListString += url + "\n";
            }
        }
        writeRequestedUrlListFile(requestedUrlsListString, requestedUrlsListFile);



        if (!downloadListOnly) {

            // Show progress dialog
            showProgressDialog(parentComponent, filesToDownload.size());

            // Start download in background thread
            new Thread(() -> {


                int downloadedCount = 0;
                for (int i = 0; i < filesToDownload.size(); i++) {
                    String fileName = filesToDownload.get(i);
                    String url = fileLinkMap.get(fileName);

                    if (url != null && downloadFile(url, downloadDir)) {
                        downloadedCount++;
                    }

                    // Update progress
                    int progress = i + 1;
                    SwingUtilities.invokeLater(() -> updateProgressBar(progress));
                }


                // Hide progress dialog
                SwingUtilities.invokeLater(() -> hideProgressDialog());

                // Show completion message and execute callback
                final int finalDownloadedCount = downloadedCount;
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(parentComponent,
                            finalDownloadedCount + " file(s) downloaded to:\n" + downloadDir.toAbsolutePath());

                    if (onComplete != null) {
                        onComplete.onDownloadComplete(finalDownloadedCount, downloadDir);
                    }
                });


            }).start();
        } else {
            JOptionPane.showMessageDialog(parentComponent, "File urls list written to:\n" + downloadDir.toAbsolutePath());
        }
    }
    
    /**
     * Downloads a single file from the given URL to the specified directory.
     * 
     * @param fileUrl The URL of the file to download
     * @param outputDir The directory to save the file to
     * @return true if download was successful, false otherwise
     */
    public boolean downloadFile(String fileUrl, Path outputDir) {
        try {
            String fileName = extractFileNameFromUrl(fileUrl);
            String token = WebPageFetcherWithJWT.getAccessToken("urs.earthdata.nasa.gov");
            
            HttpURLConnection conn = (HttpURLConnection) new URL(fileUrl).openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("Authorization", "Bearer " + token);
            
            int status = conn.getResponseCode();
            if (status == HttpURLConnection.HTTP_MOVED_PERM || 
                status == HttpURLConnection.HTTP_MOVED_TEMP || 
                status == 303) {
                String newUrl = conn.getHeaderField("Location");
                conn = (HttpURLConnection) new URL(newUrl).openConnection();
                conn.setRequestProperty("Authorization", "Bearer " + token);
                status = conn.getResponseCode();
            }
            
            if (status == 200) {
                try (InputStream in = conn.getInputStream()) {
                    Files.createDirectories(outputDir);
                    Path outputPath = outputDir.resolve(fileName);
                    Files.copy(in, outputPath, StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("Downloaded: " + fileName);
                    return true;
                }
            } else {
                System.err.println("Download failed for " + fileUrl + "\nHTTP status: " + status);
                return false;
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Download failed: " + e.getMessage());
            return false;
        }
    }


    /**
     * Write a text file containing a listing of the file URLs requested for download
     *    - Includes urls regardless of whether the download fails
     *    - Appends to existing list file if the list file already exits

     *
     * @param requestedUrlsListString A string containing a listing of the file URLs requested for download
     * @param requestedUrlsListFile The file to create which contains the URLs requested for download
     * @return true if file list is written, false otherwise
     */
    public boolean writeRequestedUrlListFile(String requestedUrlsListString, File requestedUrlsListFile) {

        if (requestedUrlsListFile == null || requestedUrlsListString == null) {
            return false;
        }

        try {

            Path downloadDir = requestedUrlsListFile.getParentFile().toPath();
            Files.createDirectories(downloadDir);

            if (!requestedUrlsListFile.exists()) {
                // Create new files list
                requestedUrlsListString = "Listing of all file urls requested for download\n\n" + requestedUrlsListString;

                BufferedWriter writer = new BufferedWriter(new FileWriter(requestedUrlsListFile, false));
                writer.write(requestedUrlsListString);
                writer.close();
            } else {
                // Append the list to the existing files list
                BufferedWriter writer = new BufferedWriter(new FileWriter(requestedUrlsListFile, true));
                writer.append(requestedUrlsListString);
                writer.close();
            }

            return true;


        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to write '" + requestedUrlsListFile.getAbsolutePath() + "'  Error: " + e.getMessage());
            return false;
        }
    }


    /**
     * Extracts the filename from a URL, handling query parameters.
     * 
     * @param url The URL to extract filename from
     * @return The extracted filename, or "downloaded_file.nc" as fallback
     */
    public String extractFileNameFromUrl(String url) {
        try {
            Pattern pattern = Pattern.compile("([^/]+\\.nc)(\\?.*)?$");
            Matcher matcher = pattern.matcher(url);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "downloaded_file.nc";
    }
    
    /**
     * Shows a progress dialog for the download operation.
     * 
     * @param parent Parent component for the dialog
     * @param max Maximum value for the progress bar
     */
    private void showProgressDialog(Component parent, int max) {
        progressDialog = new JDialog(SwingUtilities.getWindowAncestor(parent), 
                                   "Downloading...", 
                                   Dialog.ModalityType.APPLICATION_MODAL);
        progressDialog.setLayout(new BorderLayout());
        progressDialog.setSize(400, 100);
        progressDialog.setLocationRelativeTo(parent);
        
        progressBar = new JProgressBar(0, max);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        
        progressDialog.add(new JLabel("Please wait..."), BorderLayout.NORTH);
        progressDialog.add(progressBar, BorderLayout.CENTER);
        progressDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        
        new Thread(() -> progressDialog.setVisible(true)).start();
    }
    
    /**
     * Updates the progress bar value.
     * 
     * @param value The new progress value
     */
    private void updateProgressBar(int value) {
        if (progressBar != null) {
            progressBar.setValue(value);
        }
    }
    
    /**
     * Hides and disposes the progress dialog.
     */
    private void hideProgressDialog() {
        if (progressDialog != null) {
            progressDialog.setVisible(false);
            progressDialog.dispose();
            progressDialog = null;
        }
    }
    
    /**
     * Opens a file chooser dialog for selecting the download directory.
     * Handles preference management and directory creation.
     * 
     * @param parentComponent Parent component for the dialog
     * @return Selected directory path, or null if cancelled
     */
    private File getRequestedUrlsListFile(Component parentComponent) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select where to download files and the URL Listing");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        
        // Set initial directory based on preferences
        String parentDownloadDirStr = Earthdata_Cloud_Controller.getPreferenceDownloadDirectory();
        File parentDownloadDirFile = null;
        
        if (parentDownloadDirStr != null && parentDownloadDirStr.trim().length() > 0) {
            parentDownloadDirFile = new File(parentDownloadDirStr);
            if (!parentDownloadDirFile.exists()) {
                parentDownloadDirFile.mkdirs();
            }
        }
        
        if (parentDownloadDirFile == null) {
            File userHomeDir = SystemUtils.getUserHomeDir();
            parentDownloadDirFile = new File(userHomeDir, "Downloads");
            if (!parentDownloadDirFile.exists()) {
                parentDownloadDirFile = userHomeDir;
            }
        }


        
        if (parentDownloadDirFile != null && parentDownloadDirFile.exists()) {
            fileChooser.setCurrentDirectory(parentDownloadDirFile);
            final File urlFile = new File(parentDownloadDirFile, URL_LIST_FILENAME);
            fileChooser.setSelectedFile(urlFile);

        }
        
        if (fileChooser.showSaveDialog(parentComponent) != JFileChooser.APPROVE_OPTION) {
            return null;
        }

        File selectedFile = fileChooser.getSelectedFile();
        if (selectedFile == null) {
            return null;
        }

//        Path selectedFilePath = selectedFile.toPath();
//        Path selectedDownloadDirectoryPath = selectedFilePath.getParent();
//        Earthdata_Cloud_Controller.setPreferenceDownloadDirectory(selectedDownloadDirectoryPath.toString());

        File selectedDownloadParentFile = selectedFile.getParentFile();
        if (selectedDownloadParentFile != null) {
            // Save to preferences
            String selectedDownloadParentFileString = selectedDownloadParentFile.getAbsolutePath();
            Earthdata_Cloud_Controller.setPreferenceDownloadDirectory(selectedDownloadParentFileString);
        }
        
        return selectedFile;
    }


    
    /**
     * Callback interface for download completion events.
     */
    public interface DownloadCompleteCallback {
        /**
         * Called when download operation completes.
         * 
         * @param downloadedCount Number of files successfully downloaded
         * @param downloadDir Directory where files were downloaded
         */
        void onDownloadComplete(int downloadedCount, Path downloadDir);
    }
} 