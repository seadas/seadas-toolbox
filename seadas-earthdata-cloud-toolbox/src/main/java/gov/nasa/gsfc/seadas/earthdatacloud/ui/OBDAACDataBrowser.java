package gov.nasa.gsfc.seadas.earthdatacloud.ui;

import gov.nasa.gsfc.seadas.earthdatacloud.action.WebPageFetcherWithJWT;
import org.jdatepicker.impl.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OBDAACDataBrowser extends JFrame {
    private JComboBox<String> satelliteDropdown, levelDropdown, productDropdown;
    private JDatePickerImpl startDatePicker, endDatePicker;
    private JTextField minLatField, maxLatField, minLonField, maxLonField;
    private JTable resultsTable;
    private DefaultTableModel tableModel;
    private final Map<String, String> productNameTooltips = new HashMap<>();

    private final Map<String, JSONObject> metadataMap = new HashMap<>();
    private JRadioButton dayButton, nightButton, bothButton;
    private ButtonGroup dayNightGroup;
    private final Map<String, String> fileLinkMap = new HashMap<>();

    private int currentPage = 1;
    private int totalPages = 1;
    private List<String[]> pagedResults = new ArrayList<>();
    private JLabel pageInfoLabel;
    private JButton prevPageButton, nextPageButton;
    private static final int RESULTS_PER_PAGE = 25;
    private JSpinner maxApiResultsSpinner;
    private JSpinner resultsPerPageSpinner;

    private JLabel pageLabel;
    private List<String[]> allGranules = new ArrayList<>();
    private JWindow imagePreviewWindow;
    private JLabel imageLabel;
    private String[] earthdataCredentials;

    public OBDAACDataBrowser() {
        setTitle("OB_CLOUD Data Browser via Harmony Search Service");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new GridBagLayout());
        setSize(900, 600);
        loadMetadata();
        initComponents();
    }

    private void loadMetadata() {
        try {
            Path jsonDir = Paths.get(System.getProperty("user.dir"), "seadas-toolbox",
                    "seadas-earthdata-cloud-toolbox", "src", "main", "resources", "json-files");

            JSONTokener tokener;
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(jsonDir, "*.json")) {
                for (Path path : stream) {
                    String key = path.getFileName().toString().replace(".json", "");
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                            new FileInputStream(path.toFile()), StandardCharsets.UTF_8))) {
                        tokener = new JSONTokener(reader);
                        JSONObject json = new JSONObject(tokener);
                        metadataMap.put(key, json);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading metadata: " + e.getMessage());
        }
    }

    private void initComponents() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;

        satelliteDropdown = new JComboBox<>(metadataMap.keySet().toArray(new String[0]));
        levelDropdown = new JComboBox<>();
        productDropdown = new JComboBox<>();

        satelliteDropdown.addActionListener(e -> updateLevelsAndProducts());

        gbc.gridx = 0; gbc.gridy = 0;
        add(new JLabel("Satellite/Instrument:"), gbc);
        gbc.gridx = 1; add(satelliteDropdown, gbc);

        gbc.gridx = 0; gbc.gridy++;
        add(new JLabel("Data Level:"), gbc);
        gbc.gridx = 1; add(levelDropdown, gbc);

        gbc.gridx = 0; gbc.gridy++;
        add(new JLabel("Product Name:"), gbc);
        gbc.gridx = 1; add(productDropdown, gbc);

        // Filters in grouped panel
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        add(createFilterPanel(), gbc);

        // Add panel for max API results and results per page
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        JPanel paginationPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        paginationPanel.add(new JLabel("Max API Results:"));
        maxApiResultsSpinner = new JSpinner(new SpinnerNumberModel(100, 1, 10000, 1));
        maxApiResultsSpinner.setPreferredSize(new Dimension(80, 25));
        paginationPanel.add(maxApiResultsSpinner);
        paginationPanel.add(Box.createHorizontalStrut(20));
        paginationPanel.add(new JLabel("Results Per Page:"));
        resultsPerPageSpinner = new JSpinner(new SpinnerNumberModel(25, 1, 1000, 1));
        resultsPerPageSpinner.setPreferredSize(new Dimension(80, 25));
        paginationPanel.add(resultsPerPageSpinner);
        add(paginationPanel, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(e -> fetchGranules());
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> System.exit(0));
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(searchButton);
        buttonPanel.add(cancelButton);
        gbc.gridwidth = 2;
        add(buttonPanel, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        String[] columnNames = {"File Name", "Download"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return (columnIndex == 1) ? Boolean.class : String.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 1; // Only checkbox editable
            }
        };

        resultsTable = new JTable(tableModel);
        resultsTable.setDefaultRenderer(Object.class, new TableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                JLabel label = new JLabel("<html><a href='#'>" + value + "</a></html>");
                return label;
            }
        });

        resultsTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int row = resultsTable.rowAtPoint(e.getPoint());
                if (row >= 0) {
                    String fileName = (String) tableModel.getValueAt(row, 0);
                    if (BrowseImagePreview.isImageAvailable(fileName)) {
                        ImageIcon preview = BrowseImagePreview.loadPreviewImage(fileName);
                        if (preview != null) {
                            showImagePreview(preview, e.getLocationOnScreen());
                        }
                    }
                }
            }
        });

        resultsTable.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(MouseEvent e) {
                int row = resultsTable.rowAtPoint(e.getPoint());
                if (row >= 0) {
                    String fileName = (String) tableModel.getValueAt(row, 0);
                    if (BrowseImagePreview.isImageAvailable(fileName)) {
                        ImageIcon preview = BrowseImagePreview.loadPreviewImage(fileName);
                        if (preview != null) {
                            showImagePreview(preview, e.getLocationOnScreen());
                        }
                    } else {
                        hideImagePreview();
                    }
                }
            }
        });


        JScrollPane scrollPane = new JScrollPane(resultsTable);
        scrollPane.setPreferredSize(new Dimension(550, 250));
        resultsTable.getColumnModel().getColumn(0).setPreferredWidth(500);
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        add(scrollPane, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        add(createPaginationPanel(), gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        JButton downloadSelectedButton = new JButton("Download Selected");
        downloadSelectedButton.addActionListener(e -> downloadSelectedFiles());
        add(downloadSelectedButton, gbc);

        if (satelliteDropdown.getItemCount() > 0) {
            satelliteDropdown.setSelectedIndex(0);
            updateLevelsAndProducts();
        }
    }

    private void downloadSelectedFiles() {
        if (earthdataCredentials == null) {
            earthdataCredentials = WebPageFetcherWithJWT.getCredentials("urs.earthdata.nasa.gov");
            System.out.println("Username: " + earthdataCredentials[0]);
            System.out.println("Password: " + earthdataCredentials[1]);
            if (earthdataCredentials == null) {
                JOptionPane.showMessageDialog(this, "Earthdata credentials not found in ~/.netrc");
                return;
            }
        }
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Boolean isSelected = (Boolean) tableModel.getValueAt(i, 1);
            if (Boolean.TRUE.equals(isSelected)) {
                String fileName = (String) tableModel.getValueAt(i, 0);
                System.out.println("Filename: " + fileName);
                String url = fileLinkMap.get(fileName);
                if (url != null) {
                    downloadFile(url);
                }
            }
        }
    }
// Simplified OBDAACDataBrowser.java with filename cleanup and GUI download feedback (checkbox used for persistent status and locked after download)

    private void downloadFile(String fileUrl) {
        try {
            String fileName = extractFileNameFromUrl(fileUrl);
            String token = WebPageFetcherWithJWT.getAccessToken("urs.earthdata.nasa.gov");

            HttpURLConnection conn = (HttpURLConnection) new URL(fileUrl).openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("Authorization", "Bearer " + token);

            int status = conn.getResponseCode();

            if (status == HttpURLConnection.HTTP_MOVED_PERM || status == HttpURLConnection.HTTP_MOVED_TEMP || status == 303) {
                String newUrl = conn.getHeaderField("Location");
                System.out.println("Redirected to: " + newUrl);
                conn = (HttpURLConnection) new URL(newUrl).openConnection();
                conn.setRequestProperty("Authorization", "Bearer " + token);
                status = conn.getResponseCode();
            }

            if (status == 200) {
                try (InputStream in = conn.getInputStream()) {
                    Files.createDirectories(Paths.get("downloads"));
                    Path outputPath = Paths.get("downloads/" + fileName);
                    Files.copy(in, outputPath, StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("Downloaded: " + fileName);

                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this, "Downloaded: " + fileName);
                        lockFileCheckbox(fileName);
                    });
                }
            } else {
                int finalStatus = status;
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "Download failed. HTTP status code: " + finalStatus);
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this, "Download failed: " + e.getMessage());
            });
        }
    }

    private void lockFileCheckbox(String fileName) {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String currentName = (String) tableModel.getValueAt(i, 0);
            if (currentName != null && fileName.contains(currentName.replaceAll(".*?(PACE_OCI\\..*?\\.nc).*", "$1"))) {
                // Keep it checked and disable editing
                resultsTable.getColumnModel().getColumn(1).setCellEditor(null);
                break;
            }
        }
    }

    // Unified "Download Selected" button that opens folder first, then downloads
    private void addCombinedDownloadPanel(JPanel panel, GridBagConstraints gbc) {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));

        JButton downloadSelectedButton = new JButton("Download Selected");
        downloadSelectedButton.addActionListener(e -> {
            try {
                File downloadsDir = new File("downloads");
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs();
                }
                Desktop.getDesktop().open(downloadsDir);
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Failed to open downloads folder: " + ex.getMessage());
            }

            downloadSelectedFiles();
        });

        buttonPanel.add(downloadSelectedButton);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(buttonPanel, gbc);
    }

    // Helper to extract clean file name from OAuth-style or CloudFront-style URLs
    private String extractFileNameFromUrl(String url) {
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


    private JWindow previewWindow;
    private JLabel previewLabel;
    private void showImagePreview(ImageIcon imageIcon, Point location) {
        if (previewWindow == null) {
            previewWindow = new JWindow();
            previewLabel = new JLabel();
            previewWindow.getContentPane().add(previewLabel);
        }
        previewLabel.setIcon(imageIcon);
        previewWindow.pack();
        previewWindow.setLocation(location.x + 10, location.y + 10);
        previewWindow.setVisible(true);
    }

    private void hideImagePreview() {
        if (previewWindow != null) {
            previewWindow.setVisible(false);
        }
    }

//    private void hideImagePreview() {
//        imagePreviewWindow.setVisible(false);
//    }
    private JPanel createFilterPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 10, 0));

        panel.add(createTemporalPanel());
        panel.add(createSpatialPanel());
        panel.add(createDayNightPanel());

        return panel;
    }
    private void updateLevelsAndProducts() {
        String selectedSatellite = (String) satelliteDropdown.getSelectedItem();
        if (selectedSatellite == null || !metadataMap.containsKey(selectedSatellite)) return;

        JSONObject json = metadataMap.get(selectedSatellite);
        levelDropdown.removeAllItems();
        productDropdown.removeAllItems();

        for (String level : json.keySet()) {
            levelDropdown.addItem(level);
        }

        levelDropdown.addActionListener(e -> updateProducts());
        if (levelDropdown.getItemCount() > 0) {
            levelDropdown.setSelectedIndex(0);
            updateProducts();
        }
        productDropdown.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof String) {
                    String productName = (String) value;
                    String tooltip = productNameTooltips.get(productName);
                    label.setToolTipText(tooltip);
                }
                return label;
            }
        });
    }

    private void updateProducts() {
        String selectedSatellite = (String) satelliteDropdown.getSelectedItem();
        String selectedLevel = (String) levelDropdown.getSelectedItem();
        if (selectedSatellite == null || selectedLevel == null) return;

        JSONObject json = metadataMap.get(selectedSatellite);
        productDropdown.removeAllItems();
        productNameTooltips.clear(); // reset tooltip map

        JSONArray products = json.optJSONArray(selectedLevel);
        if (products != null) {
            for (int i = 0; i < products.length(); i++) {
                JSONObject prod = products.getJSONObject(i);
                String name = prod.optString("product_name", "Unknown");
                String shortName = prod.optString("short_name", "");
                productDropdown.addItem(name);
                productNameTooltips.put(name, shortName);
            }
        }
    }

    private JPanel createTemporalPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Temporal Filter"));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 2, 2, 2);
        c.anchor = GridBagConstraints.WEST;

        // Create date pickers
        startDatePicker = createDatePicker();
        endDatePicker = createDatePicker();

        // Set fixed size to ensure visibility
        Dimension fieldSize = new Dimension(150, 28);
        startDatePicker.setPreferredSize(fieldSize);
        startDatePicker.setMinimumSize(fieldSize);
        endDatePicker.setPreferredSize(fieldSize);
        endDatePicker.setMinimumSize(fieldSize);

        // Start Date Label
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Start Date:"), c);

        // Start Date Picker
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(startDatePicker, c);

        // End Date Label
        c.gridx = 0;
        c.gridy = 1;
        c.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("End Date:"), c);

        // End Date Picker
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(endDatePicker, c);

        return panel;
    }


    private JPanel createSpatialPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Spatial Filter"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.anchor = GridBagConstraints.WEST;

        // Label on the left (column 0)
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Min Lat:"), gbc);

        // Field on the right (column 1)
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        minLatField = new JTextField();
        panel.add(minLatField, gbc);

        // Max Lat
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(new JLabel("Max Lat:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        maxLatField = new JTextField();
        panel.add(maxLatField, gbc);

        // Min Lon
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(new JLabel("Min Lon:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        minLonField = new JTextField();
        panel.add(minLonField, gbc);

        // Max Lon
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(new JLabel("Max Lon:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        maxLonField = new JTextField();
        panel.add(maxLonField, gbc);

        return panel;
    }



    private JDatePickerImpl createDatePicker() {
        UtilDateModel model = new UtilDateModel();
        Properties p = new Properties();
        p.put("text.today", "Today");
        p.put("text.month", "Month");
        p.put("text.year", "Year");
        return new JDatePickerImpl(new JDatePanelImpl(model, p), new DateComponentFormatter());
    }


    private void fetchGranules() {
        fileLinkMap.clear();
        tableModel.setRowCount(0);
        allGranules.clear();

        String productName = (String) productDropdown.getSelectedItem();
        String shortName = productNameTooltips.getOrDefault(productName, productName);
        int maxApiResults = (Integer) maxApiResultsSpinner.getValue();

        String startDate = null, endDate = null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        if (startDatePicker.getModel().getValue() != null) {
            startDate = sdf.format(startDatePicker.getModel().getValue());
        }
        if (endDatePicker.getModel().getValue() != null) {
            endDate = sdf.format(endDatePicker.getModel().getValue());
        }

        String minLat = minLatField.getText().trim();
        String maxLat = maxLatField.getText().trim();
        String minLon = minLonField.getText().trim();
        String maxLon = maxLonField.getText().trim();
        boolean hasSpatial = !minLat.isEmpty() && !maxLat.isEmpty() && !minLon.isEmpty() && !maxLon.isEmpty();

        int pageSize = 2000;
        int page = 1;
        int totalFetched = 0;

        try {
            while (totalFetched < maxApiResults) {
                StringBuilder urlBuilder = new StringBuilder("https://cmr.earthdata.nasa.gov/search/granules.json?provider=OB_CLOUD");
                urlBuilder.append("&short_name=").append(URLEncoder.encode(shortName, StandardCharsets.UTF_8));
                urlBuilder.append("&page_size=").append(pageSize);
                urlBuilder.append("&page_num=").append(page);

                if (startDate != null) {
                    urlBuilder.append("&temporal=").append(startDate).append("T00:00:00Z,");
                    urlBuilder.append(endDate != null ? endDate + "T23:59:59Z" : "");
                }

                if (hasSpatial) {
                    urlBuilder.append("&bounding_box=")
                            .append(minLon).append(",")
                            .append(minLat).append(",")
                            .append(maxLon).append(",")
                            .append(maxLat);
                }

                URL url = new URL(urlBuilder.toString());
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                JSONTokener tokener = new JSONTokener(in);
                JSONObject json = new JSONObject(tokener);
                JSONArray entries = json.getJSONObject("feed").getJSONArray("entry");

                for (int i = 0; i < entries.length(); i++) {
                    JSONObject entry = entries.getJSONObject(i);
                    String title = entry.getString("title");
                    JSONArray links = entry.getJSONArray("links");
                    for (int j = 0; j < links.length(); j++) {
                        JSONObject link = links.getJSONObject(j);
                        if (link.has("href") && link.getString("href").endsWith(".nc")) {
                            String href = link.getString("href");
                            allGranules.add(new String[]{title, href});
                            fileLinkMap.put(title, href);
                            break;
                        }
                    }
                    totalFetched++;
                    if (totalFetched >= maxApiResults) break;
                }

                if (entries.length() < pageSize) break;
                page++;
            }

            // Setup pagination
            totalPages = (int) Math.ceil((double) allGranules.size() / getResultsPerPage());
            currentPage = 1;
            updateResultsTable(currentPage);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void fetchGranulesOld() {
        fileLinkMap.clear();
        tableModel.setRowCount(0);
        resultsTable.putClientProperty("fileMapping", new HashMap<String, String>());


        String productName = (String) productDropdown.getSelectedItem();
        String shortName = productNameTooltips.getOrDefault(productName, productName);
        int maxResults = (Integer) maxApiResultsSpinner.getValue();

        String startDate = null, endDate = null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        if (startDatePicker.getModel().getValue() != null) {
            startDate = sdf.format(startDatePicker.getModel().getValue());
        }
        if (endDatePicker.getModel().getValue() != null) {
            endDate = sdf.format(endDatePicker.getModel().getValue());
        }

        String minLat = minLatField.getText().trim();
        String maxLat = maxLatField.getText().trim();
        String minLon = minLonField.getText().trim();
        String maxLon = maxLonField.getText().trim();
        boolean hasSpatial = !minLat.isEmpty() && !maxLat.isEmpty() && !minLon.isEmpty() && !maxLon.isEmpty();

        int pageSize = 2000;
        int page = 1;
        int totalFetched = 0;

        try {
            while (totalFetched < maxResults) {
                StringBuilder urlBuilder = new StringBuilder("https://cmr.earthdata.nasa.gov/search/granules.json?provider=OB_CLOUD");
                urlBuilder.append("&short_name=").append(URLEncoder.encode(shortName, StandardCharsets.UTF_8));
                urlBuilder.append("&page_size=").append(pageSize);
                urlBuilder.append("&page_num=").append(page);

                if (startDate != null) {
                    urlBuilder.append("&temporal=").append(startDate).append("T00:00:00Z,");
                    urlBuilder.append(endDate != null ? endDate + "T23:59:59Z" : "");
                }

                if (hasSpatial) {
                    urlBuilder.append("&bounding_box=")
                            .append(minLon).append(",")
                            .append(minLat).append(",")
                            .append(maxLon).append(",")
                            .append(maxLat);
                }

                URL url = new URL(urlBuilder.toString());
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                JSONTokener tokener = new JSONTokener(in);
                JSONObject json = new JSONObject(tokener);
                JSONArray entries = json.getJSONObject("feed").getJSONArray("entry");

                for (int i = 0; i < entries.length(); i++) {
                    JSONObject entry = entries.getJSONObject(i);
                    String title = entry.getString("title");
                    JSONArray links = entry.getJSONArray("links");
                    for (int j = 0; j < links.length(); j++) {
                        JSONObject link = links.getJSONObject(j);
                        if (link.has("href") && link.getString("href").endsWith(".nc")) {
                            String href = link.getString("href");
                            tableModel.addRow(new Object[]{title});
                            resultsTable.putClientProperty(title, href);
                            fileLinkMap.put(title, href);
                            break;
                        }
                    }
                    totalFetched++;
                    if (totalFetched >= maxResults) break;
                }
                if (entries.length() < pageSize) break;
                page++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private JPanel createPaginationPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        JButton prevButton = new JButton("Previous");
        JButton nextButton = new JButton("Next");
        JLabel pageLabel = new JLabel("Page 1"); // This label will be updated dynamically

        prevButton.addActionListener(e -> {
            if (currentPage > 1) {
                currentPage--;
                updateResultsTable(currentPage);
            }
        });

        nextButton.addActionListener(e -> {
            if (currentPage < totalPages) {
                currentPage++;
                updateResultsTable(currentPage);
            }
        });

        panel.add(prevButton);
        panel.add(pageLabel);
        panel.add(nextButton);
        this.pageLabel = pageLabel;

        return panel;
    }

    private void updateResultsTable(int page) {
        int resultsPerPage = getResultsPerPage();
        tableModel.setRowCount(0);

        int start = (page - 1) * resultsPerPage;
        int end = Math.min(start + resultsPerPage, allGranules.size());

        for (int i = start; i < end; i++) {
            String[] row = allGranules.get(i);
            tableModel.addRow(new Object[]{row[0], false, false});
            resultsTable.putClientProperty(row[0], row[1]);  // Store URL
        }

        if (pageLabel != null) {
            pageLabel.setText("Page " + page + " of " + totalPages);
        }
    }


    private int getResultsPerPage() {
        return (Integer) resultsPerPageSpinner.getValue();
    }


    private JPanel createDayNightPanel() {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder("Day/Night Filter"));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JRadioButton dayButton = new JRadioButton("Day");
        JRadioButton nightButton = new JRadioButton("Night");
        JRadioButton bothButton = new JRadioButton("Both", true);

        ButtonGroup group = new ButtonGroup();
        group.add(dayButton);
        group.add(nightButton);
        group.add(bothButton);

        // Add padding and alignment
        dayButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        nightButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        bothButton.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(Box.createVerticalStrut(5));
        panel.add(dayButton);
        panel.add(Box.createVerticalStrut(5));
        panel.add(nightButton);
        panel.add(Box.createVerticalStrut(5));
        panel.add(bothButton);
        panel.add(Box.createVerticalGlue());

        // Save for later use
        this.dayButton = dayButton;
        this.nightButton = nightButton;
        this.bothButton = bothButton;

        return panel;
    }

    private void showPage(int pageNum) {
        tableModel.setRowCount(0);
        int start = (pageNum - 1) * RESULTS_PER_PAGE;
        int end = Math.min(start + RESULTS_PER_PAGE, pagedResults.size());
        for (int i = start; i < end; i++) {
            tableModel.addRow(pagedResults.get(i));
        }
        currentPage = pageNum;
        pageInfoLabel.setText("Page " + currentPage + " of " + totalPages);
        prevPageButton.setEnabled(currentPage > 1);
        nextPageButton.setEnabled(currentPage < totalPages);
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new OBDAACDataBrowser().setVisible(true));
    }
}
