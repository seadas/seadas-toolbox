package gov.nasa.gsfc.seadas.earthdatacloud.action;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
public class FetchOBDAACData extends JFrame {

    private static final String CMR_COLLECTION_URL = "https://cmr.earthdata.nasa.gov/search/collections.json?short_name=";
    private static final String CMR_GRANULE_URL = "https://cmr.earthdata.nasa.gov/search/granules.umm_json?collection_concept_id=";

    private JTextArea outputArea;
    private JButton searchButton, downloadButton, nextButton, prevButton;
    private JComboBox<Integer> pageSizeSelector;
    private JComboBox<String> satelliteSelector, levelSelector, productSelector;
    private List<String> downloadUrls;
    private int currentPage = 0;
    private int pageSize = 20;

    public FetchOBDAACData() {
        setTitle("NASA OB-DAAC Data Downloader");
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new FlowLayout());

        inputPanel.add(new JLabel("Satellite & Instrument:"));
        satelliteSelector = new JComboBox<>(new String[]{"PACE_OCI", "MODIS_AQUA", "MODIS_TERRA"});
        inputPanel.add(satelliteSelector);

        inputPanel.add(new JLabel("Data Level:"));
        levelSelector = new JComboBox<>(new String[]{"L1", "L2", "L3M"});
        inputPanel.add(levelSelector);

        inputPanel.add(new JLabel("Product Name:"));
        productSelector = new JComboBox<>(new String[]{"SFREFL_NRT", "CHL", "SST"});
        inputPanel.add(productSelector);

        searchButton = new JButton("Search");
        inputPanel.add(searchButton);

        downloadButton = new JButton("Download All");
        downloadButton.setEnabled(false);
        inputPanel.add(downloadButton);

        add(inputPanel, BorderLayout.NORTH);

        outputArea = new JTextArea();
        outputArea.setEditable(false);
        add(new JScrollPane(outputArea), BorderLayout.CENTER);

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout());

        controlPanel.add(new JLabel("Items per page:"));
        pageSizeSelector = new JComboBox<>(new Integer[]{5, 10, 20, 50, 100});
        pageSizeSelector.setSelectedItem(20);
        controlPanel.add(pageSizeSelector);

        prevButton = new JButton("Previous");
        prevButton.setEnabled(false);
        controlPanel.add(prevButton);

        nextButton = new JButton("Next");
        nextButton.setEnabled(false);
        controlPanel.add(nextButton);

        add(controlPanel, BorderLayout.SOUTH);

        searchButton.addActionListener(e -> {
            String shortName = generateShortName();
            pageSize = (Integer) pageSizeSelector.getSelectedItem();
            searchForData(shortName);
        });

        pageSizeSelector.addActionListener(e -> {
            pageSize = (Integer) pageSizeSelector.getSelectedItem();
            currentPage = 0;
            displayPage();
        });

        nextButton.addActionListener(e -> {
            if ((currentPage + 1) * pageSize < downloadUrls.size()) {
                currentPage++;
                displayPage();
            }
        });

        prevButton.addActionListener(e -> {
            if (currentPage > 0) {
                currentPage--;
                displayPage();
            }
        });

        downloadButton.addActionListener(e -> downloadAllFiles());
    }

    private String generateShortName() {
        return satelliteSelector.getSelectedItem() + "_" + levelSelector.getSelectedItem() + "_" + productSelector.getSelectedItem();
    }

    private void searchForData(String shortName) {
        outputArea.setText("Searching for data for: " + shortName + "...\n");
        new Thread(() -> {
            try {
                String conceptId = getConceptId(shortName);
                if (conceptId != null) {
                    downloadUrls = getGranuleUrls(conceptId);
                    SwingUtilities.invokeLater(() -> {
                        currentPage = 0;
                        displayPage();
                        downloadButton.setEnabled(true);
                        nextButton.setEnabled(downloadUrls.size() > pageSize);
                    });
                } else {
                    SwingUtilities.invokeLater(() -> outputArea.setText("Concept ID not found for " + shortName));
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> outputArea.setText("Error occurred: " + ex.getMessage()));
            }
        }).start();
    }

    private void displayPage() {
        outputArea.setText("Showing results " + (currentPage * pageSize + 1) + " to " + Math.min((currentPage + 1) * pageSize, downloadUrls.size()) + " of " + downloadUrls.size() + "\n\n");

        for (int i = currentPage * pageSize; i < Math.min((currentPage + 1) * pageSize, downloadUrls.size()); i++) {
            outputArea.append(downloadUrls.get(i) + "\n");
        }

        prevButton.setEnabled(currentPage > 0);
        nextButton.setEnabled((currentPage + 1) * pageSize < downloadUrls.size());
    }

    private void downloadAllFiles() {
        outputArea.append("\nStarting download...\n");
        new Thread(() -> {
            for (String fileUrl : downloadUrls) {
                try {
                    URL url = new URL(fileUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");

                    String fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
                    InputStream in = conn.getInputStream();
                    FileOutputStream out = new FileOutputStream(fileName);

                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }

                    in.close();
                    out.close();

                    SwingUtilities.invokeLater(() -> outputArea.append("Downloaded: " + fileName + "\n"));
                } catch (IOException ex) {
                    SwingUtilities.invokeLater(() -> outputArea.append("Failed to download: " + fileUrl + "\n"));
                }
            }
            SwingUtilities.invokeLater(() -> outputArea.append("\nAll downloads completed."));
        }).start();
    }

    public static String getConceptId(String shortName) throws IOException {
        String urlString = CMR_COLLECTION_URL + shortName;
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        JSONObject jsonResponse = new JSONObject(response.toString());
        JSONArray items = jsonResponse.getJSONObject("feed").getJSONArray("entry");

        if (items.length() > 0) {
            return items.getJSONObject(0).getString("id");
        }
        return null;
    }

    private static List<String> getGranuleUrls(String conceptId) throws IOException {
        List<String> urls = new ArrayList<>();
        int pageSize = 100;  // Request up to 100 results per page
        int pageNum = 1;  // Start from page 1

        while (true) {
            String urlString = String.format(
                    "https://cmr.earthdata.nasa.gov/search/granules.umm_json?collection_concept_id=%s&page_size=%d&page_num=%d",
                    conceptId, pageSize, pageNum
            );

            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            JSONObject jsonResponse = new JSONObject(response.toString());
            JSONArray items = jsonResponse.getJSONArray("items");

            if (items.length() == 0) {
                break; // No more results, exit the loop
            }

            for (int i = 0; i < items.length(); i++) {
                JSONObject umm = items.getJSONObject(i).getJSONObject("umm");
                JSONArray relatedUrls = umm.getJSONArray("RelatedUrls");

                for (int j = 0; j < relatedUrls.length(); j++) {
                    JSONObject relatedUrl = relatedUrls.getJSONObject(j);
                    String urlType = relatedUrl.optString("Type");
                    if ("GET DATA".equalsIgnoreCase(urlType)) {
                        urls.add(relatedUrl.getString("URL"));
                    }
                }
            }

            pageNum++; // Move to the next page
        }

        return urls;
    }

    public static String fetchCollectionIdFromCMR(String fileName, String conceptId) {
        String cmrUrl = "https://cmr.earthdata.nasa.gov/search/granules.json?readable_granule_name=" + fileName;
        if (conceptId != null) {
            cmrUrl += "&concept_id=" + conceptId;
        }
        try {
            System.out.println("CMR granule lookup URL: " + cmrUrl);
            URL url = new URL(cmrUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int status = conn.getResponseCode();
            if (status == 200) {
                InputStream is = conn.getInputStream();
                java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
                String response = s.hasNext() ? s.next() : "";
                JSONObject json = new JSONObject(response);
                JSONArray entries = json.getJSONObject("feed").getJSONArray("entry");
                if (entries.length() > 0) {
                    return entries.getJSONObject(0).getString("collection_concept_id");
                }
            } else {
                InputStream es = conn.getErrorStream();
                String errorResponse = "";
                if (es != null) {
                    java.util.Scanner s = new java.util.Scanner(es).useDelimiter("\\A");
                    errorResponse = s.hasNext() ? s.next() : "";
                }
                System.err.println("CMR request failed with status: " + status + ", response: " + errorResponse);
            }
        } catch (Exception e) {
            System.err.println("Error fetching collection ID from CMR: " + e.getMessage());
        }
        return null;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FetchOBDAACData app = new FetchOBDAACData();
            app.setVisible(true);
        });
    }
}
