package gov.nasa.gsfc.seadas.earthdatacloud.util;

import gov.nasa.gsfc.seadas.earthdatacloud.auth.WebPageFetcherWithJWT;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;

public class FileDownloader {

    private  String[] earthdataCredentials;
    private Map<String, String> fileLinkMap;
    private  JTable resultsTable;
    private  DefaultTableModel tableModel;
    private Component parentComponent;



    public FileDownloader() {
        // empty constructor
    }


    public FileDownloader(String[] credentials, Map<String, String> fileLinkMap,
                          JTable resultsTable, DefaultTableModel tableModel,
                          Component parentComponent) {
        this.earthdataCredentials = credentials;
        this.fileLinkMap = fileLinkMap;
        this.resultsTable = resultsTable;
        this.tableModel = tableModel;
        this.parentComponent = parentComponent;
    }

    public void initialize(Map<String, String> fileLinkMap, JTable resultsTable, DefaultTableModel tableModel) {
        this.fileLinkMap = fileLinkMap;
        this.resultsTable = resultsTable;
        this.tableModel = tableModel;
    }
    public void downloadSelectedFiles(File saveDirectory) {
        int downloadedCount = 0;
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Boolean isSelected = (Boolean) tableModel.getValueAt(i, 1);
            if (Boolean.TRUE.equals(isSelected)) {
                String fileName = (String) tableModel.getValueAt(i, 0);
                String url = fileLinkMap.get(fileName);
                if (url != null && downloadFile(url, fileName, saveDirectory)) {
                    downloadedCount++;
                    lockFileCheckbox(fileName);
                }
            }
        }

        if (downloadedCount > 0) {
            JOptionPane.showMessageDialog(parentComponent, downloadedCount + " file(s) downloaded successfully.");
        } else {
            JOptionPane.showMessageDialog(parentComponent, "No files selected for download.");
        }
    }

    private boolean downloadFile(String fileUrl, String fileName, File saveDir) {
        try {
            String token = WebPageFetcherWithJWT.getAccessToken("urs.earthdata.nasa.gov");

            HttpURLConnection conn = (HttpURLConnection) new URL(fileUrl).openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("Authorization", "Bearer " + token);
            int status = conn.getResponseCode();

            if (status == HttpURLConnection.HTTP_MOVED_PERM ||
                    status == HttpURLConnection.HTTP_MOVED_TEMP || status == 303) {
                String newUrl = conn.getHeaderField("Location");
                conn = (HttpURLConnection) new URL(newUrl).openConnection();
                conn.setRequestProperty("Authorization", "Bearer " + token);
                status = conn.getResponseCode();
            }

            if (status == 200) {
                try (InputStream in = conn.getInputStream()) {
                    Path outputPath = new File(saveDir, fileName).toPath();
                    Files.copy(in, outputPath, StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("Downloaded: " + fileName);
                    return true;
                }
            } else {
                JOptionPane.showMessageDialog(parentComponent, "Failed to download " + fileName + ". HTTP status: " + status);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(parentComponent, "Download failed for " + fileName + ": " + e.getMessage());
        }
        return false;
    }

    private void lockFileCheckbox(String fileName) {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String currentName = (String) tableModel.getValueAt(i, 0);
            if (currentName != null && fileName.contains(currentName.replaceAll(".*?(PACE_OCI\\..*?\\.nc).*", "$1"))) {
                resultsTable.getColumnModel().getColumn(1).setCellEditor(null);
                break;
            }
        }
    }
}
