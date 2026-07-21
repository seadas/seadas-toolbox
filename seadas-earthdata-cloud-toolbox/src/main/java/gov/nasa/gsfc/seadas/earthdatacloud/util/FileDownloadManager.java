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
    private JLabel progressLabel;
    private JLabel progressFileLabel;
    private SwingWorker<DownloadResult, DownloadProgress> downloadWorker;
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
                                    String searchCriteriaString,
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


        // Check if any files selected
        if (filesToDownload == null || filesToDownload.size() == 0) {
            JOptionPane.showMessageDialog(parentComponent, "No files selected");
            return;
        }

        // Get download directory from user
        File requestedUrlsListFile = getRequestedUrlsListFile(parentComponent);
        if (requestedUrlsListFile == null) {
            return; // User cancelled
        }

        Path downloadDir = requestedUrlsListFile.getParentFile().toPath();
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
        boolean urlFileWriteStatus = writeRequestedUrlListFile(requestedUrlsListString, requestedUrlsListFile, searchCriteriaString);

        if (!urlFileWriteStatus) {
            JOptionPane.showMessageDialog(parentComponent, "ERROR!!: Failed to write file urls list:\n" + requestedUrlsListFile.getAbsolutePath());
            return;
        }

        if (!downloadListOnly) {
            showProgressDialog(parentComponent, filesToDownload.size());

            downloadWorker = new SwingWorker<>() {
                @Override
                protected DownloadResult doInBackground() {
                    int downloadedCount = 0;

                    for (int i = 0; i < filesToDownload.size(); i++) {
                        if (isCancelled()) {
                            break;
                        }
                        final int fileIndex = i;
                        String fileName = filesToDownload.get(fileIndex);
                        String url = fileLinkMap.get(fileName);

                        publish(new DownloadProgress(fileIndex, filesToDownload.size(), fileName, 0));

                        if (url != null && downloadFile(url, downloadDir,
                                currentFileProgress -> publish(new DownloadProgress(fileIndex, filesToDownload.size(),
                                        fileName, currentFileProgress)))) {
                            downloadedCount++;
                        }

                        publish(new DownloadProgress(fileIndex + 1, filesToDownload.size(), fileName, 0));
                    }

                    return new DownloadResult(downloadedCount, isCancelled());
                }

                @Override
                protected void process(List<DownloadProgress> chunks) {
                    if (!chunks.isEmpty()) {
                        updateProgressBar(chunks.get(chunks.size() - 1));
                    }
                }

                @Override
                protected void done() {
                    hideProgressDialog();

                    DownloadResult result;
                    try {
                        result = get();
                    } catch (Exception e) {
                        result = new DownloadResult(0, isCancelled());
                    }

                    if (result.cancelled) {
                        JOptionPane.showMessageDialog(parentComponent, "Download cancelled.");
                        return;
                    }

                    showCompletionMessage(parentComponent, downloadDir, result.downloadedCount, onComplete);
                }
            };

            downloadWorker.execute();
            progressDialog.setVisible(true);
        } else {
            JOptionPane.showMessageDialog(parentComponent, "File urls list written to:\n" + requestedUrlsListFile.getAbsolutePath());
        }

    }


    private void showCompletionMessage (Component parentComponent, Path downloadDir,int downloadedCount,
                                        DownloadCompleteCallback onComplete){
        final int finalDownloadedCount = downloadedCount;
        JOptionPane.showMessageDialog(parentComponent,
                finalDownloadedCount + " file(s) downloaded to:\n" + downloadDir.toAbsolutePath());

        if (onComplete != null) {
            onComplete.onDownloadComplete(finalDownloadedCount, downloadDir);
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
        return downloadFile(fileUrl, outputDir, null);
    }

    private boolean downloadFile(String fileUrl, Path outputDir, DownloadProgressListener progressListener) {
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
                long totalBytes = conn.getContentLengthLong();
                try (InputStream in = conn.getInputStream()) {
                    Files.createDirectories(outputDir);
                    Path outputPath = outputDir.resolve(fileName);
                    copyWithProgress(in, outputPath, totalBytes, progressListener);
                    System.out.println("Downloaded: " + fileName);
                    return true;
                }
            } else {
                System.err.println("Download failed for " + fileUrl + "\nHTTP status: " + status);
                return false;
            }
            
        } catch (InterruptedIOException e) {
            return false;
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
    public boolean writeRequestedUrlListFile(String requestedUrlsListString, File requestedUrlsListFile, String searchCriteriaString) {

        if (requestedUrlsListFile == null || requestedUrlsListString == null) {
            return false;
        }

        try {
            Path downloadDir = requestedUrlsListFile.getParentFile().toPath();
            Files.createDirectories(downloadDir);

            requestedUrlsListString =
                    searchCriteriaString +
                    "Search Results - Returned File URLs:\n"
                    + requestedUrlsListString + "\n\n";


            if (!requestedUrlsListFile.exists()) {
                // Create new files list
                BufferedWriter writer = new BufferedWriter(new FileWriter(requestedUrlsListFile, false));
                writer.write(requestedUrlsListString);
                writer.close();
            } else {
                // Append the list to the existing files list
                BufferedWriter writer = new BufferedWriter(new FileWriter(requestedUrlsListFile, true));
                writer.append("-------------------------------------------------------\n" + requestedUrlsListString);
                writer.close();
            }

            return true;

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to write '" + requestedUrlsListFile.getAbsolutePath() + "'  Error: " + e.getMessage());
            return false;
        }
    }



    private void copyWithProgress(InputStream in, Path outputPath, long totalBytes,
                                  DownloadProgressListener progressListener) throws IOException {
        try (OutputStream out = Files.newOutputStream(outputPath)) {
            byte[] buffer = new byte[8192];
            long bytesCopied = 0;
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                if (downloadWorker != null && downloadWorker.isCancelled()) {
                    throw new InterruptedIOException("Download cancelled");
                }
                out.write(buffer, 0, bytesRead);
                bytesCopied += bytesRead;
                if (progressListener != null && totalBytes > 0) {
                    progressListener.onProgress(Math.min(1.0, bytesCopied / (double) totalBytes));
                }
            }
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
        progressDialog.setLayout(new BorderLayout(8, 8));
        progressDialog.setSize(520, 155);
        progressDialog.setLocationRelativeTo(parent);
        
        progressBar = new JProgressBar(0, 1000);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        progressBar.setString("Preparing...");

        progressLabel = new JLabel("Preparing download...");
        progressFileLabel = new JLabel(" ");

        JPanel messagePanel = new JPanel(new GridLayout(2, 1, 0, 2));
        messagePanel.setBorder(BorderFactory.createEmptyBorder(8, 10, 0, 10));
        messagePanel.add(progressLabel);
        messagePanel.add(progressFileLabel);

        progressDialog.add(messagePanel, BorderLayout.NORTH);
        progressDialog.add(progressBar, BorderLayout.CENTER);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> {
            if (downloadWorker != null) {
                downloadWorker.cancel(true);
            }
        });
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 8, 10));
        buttonPanel.add(cancelButton);
        progressDialog.add(buttonPanel, BorderLayout.SOUTH);

        progressDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
    }
    
    /**
     * Updates the progress bar value.
     * 
     * @param progress The new progress value
     */
    private void updateProgressBar(DownloadProgress progress) {
        if (progressBar != null) {
            double completedFiles = progress.completedFiles;
            double currentFileProgress = progress.currentFileProgress;
            int progressValue = (int) Math.round(((completedFiles + currentFileProgress)
                    / Math.max(1, progress.totalFiles)) * 1000);
            progressBar.setValue(Math.min(1000, progressValue));
            progressBar.setString(formatOverallProgress(progressValue));
        }

        if (progressLabel != null) {
            int currentFileNumber = Math.min(progress.completedFiles + 1, progress.totalFiles);
            progressLabel.setText("Downloading file " + currentFileNumber + " of " + progress.totalFiles);
        }

        if (progressFileLabel != null) {
            progressFileLabel.setText(shortenFileName(progress.fileName, 62));
            progressFileLabel.setToolTipText(progress.fileName);
        }
    }

    private String formatOverallProgress(int progressValue) {
        int percent = Math.min(100, Math.max(0, progressValue / 10));
        return percent + "% overall";
    }

    private String shortenFileName(String fileName, int maxLength) {
        if (fileName == null || fileName.length() <= maxLength) {
            return fileName;
        }

        int prefixLength = Math.max(1, (maxLength - 3) / 2);
        int suffixLength = Math.max(1, maxLength - 3 - prefixLength);
        return fileName.substring(0, prefixLength) + "..." + fileName.substring(fileName.length() - suffixLength);
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
        progressBar = null;
        progressLabel = null;
        progressFileLabel = null;
        downloadWorker = null;
    }
    
    /**
     * Opens a file chooser dialog for selecting the urls list file (which establishes a downloads directory).
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
        String downloadDirStr = Earthdata_Cloud_Controller.getPreferenceDownloadDirectory();
        File downloadDirFile = null;
        
        if (downloadDirStr != null && downloadDirStr.trim().length() > 0) {
            downloadDirFile = new File(downloadDirStr);
            if (!downloadDirFile.exists()) {
                downloadDirFile.mkdirs();
            }
        }
        
        if (downloadDirFile == null) {
            File userHomeDir = SystemUtils.getUserHomeDir();
            downloadDirFile = new File(userHomeDir, "Downloads");
            if (!downloadDirFile.exists()) {
                downloadDirFile = userHomeDir;
            }
        }


        if (downloadDirFile != null && downloadDirFile.exists()) {
            fileChooser.setCurrentDirectory(downloadDirFile);
            final File urlFile = new File(downloadDirFile, URL_LIST_FILENAME);
            fileChooser.setSelectedFile(urlFile);
        } else {
            // could not establish a default downloads directory to use, user should be able to pick one though in the finder.
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

        File selectedDownloadDirFile = selectedFile.getParentFile();
        if (selectedDownloadDirFile != null) {
            // Save to preferences
            String selectedDownloadDirectoryString = selectedDownloadDirFile.getAbsolutePath();
            Earthdata_Cloud_Controller.setPreferenceDownloadDirectory(selectedDownloadDirectoryString);
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

    private interface DownloadProgressListener {
        void onProgress(double currentFileProgress);
    }

    private static class DownloadProgress {
        private final int completedFiles;
        private final int totalFiles;
        private final String fileName;
        private final double currentFileProgress;

        private DownloadProgress(int completedFiles, int totalFiles, String fileName, double currentFileProgress) {
            this.completedFiles = completedFiles;
            this.totalFiles = totalFiles;
            this.fileName = fileName;
            this.currentFileProgress = currentFileProgress;
        }
    }

    private static class DownloadResult {
        private final int downloadedCount;
        private final boolean cancelled;

        private DownloadResult(int downloadedCount, boolean cancelled) {
            this.downloadedCount = downloadedCount;
            this.cancelled = cancelled;
        }
    }
} 
