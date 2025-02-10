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

    private JTextField shortNameField;
    private JTextArea outputArea;
    private JButton searchButton, downloadButton;
    private List<String> downloadUrls;

    public FetchOBDAACData() {
        setTitle("NASA OB-DAAC Data Downloader");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new FlowLayout());

        inputPanel.add(new JLabel("Enter Short Name:"));
        shortNameField = new JTextField(20);
        inputPanel.add(shortNameField);

        searchButton = new JButton("Search");
        inputPanel.add(searchButton);

        downloadButton = new JButton("Download All");
        downloadButton.setEnabled(false);
        inputPanel.add(downloadButton);

        add(inputPanel, BorderLayout.NORTH);

        outputArea = new JTextArea();
        outputArea.setEditable(false);
        add(new JScrollPane(outputArea), BorderLayout.CENTER);

        searchButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String shortName = shortNameField.getText().trim();
                if (!shortName.isEmpty()) {
                    searchForData(shortName);
                } else {
                    JOptionPane.showMessageDialog(null, "Please enter a short name.");
                }
            }
        });

        downloadButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                downloadAllFiles();
            }
        });
    }

    private void searchForData(String shortName) {
        outputArea.setText("Searching for data...");
        new Thread(() -> {
            try {
                String conceptId = getConceptId(shortName);
                if (conceptId != null) {
                    downloadUrls = getGranuleUrls(conceptId);
                    SwingUtilities.invokeLater(() -> {
                        outputArea.setText("Found " + downloadUrls.size() + " files:\n");
                        for (String url : downloadUrls) {
                            outputArea.append(url + "\n");
                        }
                        downloadButton.setEnabled(true);
                    });
                } else {
                    SwingUtilities.invokeLater(() -> outputArea.setText("Concept ID not found for the provided short name."));
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> outputArea.setText("Error occurred: " + ex.getMessage()));
            }
        }).start();
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

    private static String getConceptId(String shortName) throws IOException {
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
        String urlString = CMR_GRANULE_URL + conceptId;
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

        return urls;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FetchOBDAACData app = new FetchOBDAACData();
            app.setVisible(true);
        });
    }
}
