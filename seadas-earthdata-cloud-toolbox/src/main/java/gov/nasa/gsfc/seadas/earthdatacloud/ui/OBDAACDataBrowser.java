package gov.nasa.gsfc.seadas.earthdatacloud.ui;

import gov.nasa.gsfc.seadas.earthdatacloud.auth.WebPageFetcherWithJWT;
import gov.nasa.gsfc.seadas.earthdatacloud.util.ImagePreviewHelper;
import gov.nasa.gsfc.seadas.earthdatacloud.util.MissionNameWriter;
import gov.nasa.gsfc.seadas.earthdatacloud.util.PythonScriptRunner;
import gov.nasa.gsfc.seadas.earthdatacloud.util.PythonScriptRunner_old;
import org.jdatepicker.impl.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.Timer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static gov.nasa.gsfc.seadas.earthdatacloud.ui.BrowseImagePreview.getPreviewUrl;

public class OBDAACDataBrowser extends JPanel {
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
    private JSpinner maxResultsSpinner;
    private ThumbnailPreview thumbnailPreview;
    private JLabel dateRangeHintLabel = new JLabel();  // Declare as a class member
    private JPanel temporalPanel;
    private JLabel dateRangeLabel; // add this as a field so we can update it later

    private JDialog progressDialog;
    private JProgressBar progressBar;
    private JPanel resultsContainer;
    private JScrollPane scrollPane;
    private final Map<String, String> fileSpatialMap = new HashMap<>();
    private JDialog parentDialog;
    private ImagePreviewHelper imagePreviewHelper;

    Map<String, String[]> missionDateRanges = Map.of(
            "SeaHawk/HawkEye", new String[]{"2018-12-01", "2023-12-31"},
            "MODISA", new String[]{"2002-07-04", "2024-12-31"},
            "VIIRSN", new String[]{"2011-10-28", "2024-12-31"}
            // Add more as needed
    );
    public OBDAACDataBrowser(JDialog parentDialog) {
//        setTitle("OB_CLOUD Data Browser - powered by Harmony Search");
//        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.parentDialog = parentDialog;
        setLayout(new GridBagLayout());
        setSize(850, 600);
        setLayout(new GridBagLayout());
        loadMetadata();
        imagePreviewHelper = new ImagePreviewHelper();
//        Set<String> allSatellites = metadataMap.keySet(); // or your satelliteDropdown items
//        missionDateRanges = fetchMissionDateRanges(allSatellites);
        loadMissionDateRangesFromFile();
        initComponents();
        //thumbnailPreview = new ThumbnailPreview(this);
    }

    private void showProgressDialog(Component parent, int max) {
        progressDialog = new JDialog(SwingUtilities.getWindowAncestor(parent), "Downloading...", Dialog.ModalityType.APPLICATION_MODAL);
        progressDialog.setLayout(new BorderLayout());
        progressDialog.setSize(400, 100);
        progressDialog.setLocationRelativeTo(parent);

        progressBar = new JProgressBar(0, max);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);

        progressDialog.add(new JLabel("Please wait..."), BorderLayout.NORTH);
        progressDialog.add(progressBar, BorderLayout.CENTER);
        progressDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        new Thread(() -> progressDialog.setVisible(true)).start(); // Run in a separate thread
    }



    private void updateProgressBar(int value) {
        if (progressBar != null) {
            progressBar.setValue(value);
        }
    }
    private void hideProgressDialog() {
        if (progressDialog != null) {
            progressDialog.setVisible(false);  // Optional, in case it's still visible
            progressDialog.dispose();
            progressDialog = null;
        }
    }

//    private void loadMissionDateRangesFromFile() {
//        missionDateRanges = new HashMap<>();
//
//        Path filePath = Paths.get("seadas-toolbox", "seadas-earthdata-cloud-toolbox",
//                "src", "main", "resources", "json-files", "mission_date_ranges.json");
//            Path missionRangePath = filePath.resolve("mission_date_ranges.json");
//            if (Files.exists(missionRangePath)) {
//                try (BufferedReader reader = Files.newBufferedReader(missionRangePath, StandardCharsets.UTF_8)) {
//                    JSONObject json = new JSONObject(new JSONTokener(reader));
//                    for (String key : json.keySet()) {
//                        JSONObject dates = json.getJSONObject(key);
//                        String start = dates.optString("start", null);
//                        String end = dates.optString("end", "present");
//                        if (start != null) {
//                            missionDateRanges.put(key, new String[]{start, end});
//                        }
//                    }
//                } catch (IOException e) {
//                    System.err.println("Failed to read mission date ranges: " + e.getMessage());
//                }
//            }
//    }
private void loadMissionDateRangesFromFile() {
    missionDateRanges = new HashMap<>();

    // First try external file override
    Path externalFile = Paths.get("seadas-toolbox", "seadas-earthdata-cloud-toolbox",
            "src", "main", "resources", "json-files", "mission_date_ranges.json");

    if (Files.exists(externalFile)) {
        System.out.println("Loading mission_date_ranges.json from external path: " + externalFile.toAbsolutePath());
        try (BufferedReader reader = Files.newBufferedReader(externalFile, StandardCharsets.UTF_8)) {
            loadDateRangesFromReader(reader);
            return;
        } catch (IOException e) {
            System.err.println("Failed to read external mission date ranges: " + e.getMessage());
        }
    }

    // Otherwise fall back to classpath (e.g., bundled in JAR)
    System.out.println("Loading mission_date_ranges.json from classpath");
    try (InputStream input = getClass().getClassLoader().getResourceAsStream("json-files/mission_date_ranges.json")) {
        if (input != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
            loadDateRangesFromReader(reader);
        } else {
            System.err.println("Could not find mission_date_ranges.json in resources");
        }
    } catch (IOException e) {
        System.err.println("Failed to read mission date ranges from classpath: " + e.getMessage());
    }
}

    private void loadDateRangesFromReader(BufferedReader reader) throws IOException {
        JSONObject json = new JSONObject(new JSONTokener(reader));
        for (String key : json.keySet()) {
            JSONObject dates = json.getJSONObject(key);
            String start = dates.optString("start", null);
            String end = dates.optString("end", "present");
            if (start != null) {
                missionDateRanges.put(key, new String[]{start, end});
            }
        }
    }


    private void loadMetadata() {
        boolean usedExternal = false;
        try {
            Path jsonDir = PythonScriptRunner_old.resourceDir;


            if (!Files.exists(PythonScriptRunner_old.resourceDir) || !Files.isDirectory(PythonScriptRunner_old.resourceDir)) {
                PythonScriptRunner.runMetadataScript();
            }
            JSONTokener tokener;
            Set<String> missionKeys = new HashSet<>();
            if (Files.exists(jsonDir) && Files.isDirectory(jsonDir)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(jsonDir, "*.json")) {
                    for (Path path : stream) {
                        String key = path.getFileName().toString().replace(".json", "");
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                                new FileInputStream(path.toFile()), StandardCharsets.UTF_8))) {
                            tokener = new JSONTokener(reader);
                            JSONObject json = new JSONObject(tokener);
                            metadataMap.put(key, json);
                        }
                        String fileName = path.getFileName().toString();
                        if (!fileName.equals("mission_date_ranges.json")) {
                            missionKeys.add(key);
                        }
                    }
                }
                usedExternal = true;
            }
            if (!usedExternal) {
                String[] resourceFiles = {
                        "CZCS.json", "HAWKEYE.json","HICO.json", "MERGED_S3_OLCI.json", "MERIS.json",
                        "MODISA.json", "MODIST.json", "OCTS.json", "OLCIS3A.json", "OLCIS3B.json",
                        "PACE_HARP2.json", "PACE_OCI.json", "PACE_SPEXONE.json", // Add expected resources
                        "SeaWiFS.json", "VIIRSJ1.json","VIIRSJ2.json","VIIRSN.json"
                };

                for (String fileName : resourceFiles) {
                    String key = fileName.replace(".json", "");
                    InputStream input = getClass().getClassLoader()
                            .getResourceAsStream("json-files/" + fileName);
                    if (input == null) {
                        System.err.println("⚠ Missing embedded resource: " + fileName);
                        continue;
                    }

                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                        tokener = new JSONTokener(reader);
                        JSONObject json = new JSONObject(tokener);
                        metadataMap.put(key, json);
                        missionKeys.add(key);
                    }
                }
            }

            if (!Files.exists(jsonDir.resolve("mission_date_ranges.json"))) {
                //PythonScriptRunner.runDateRangeScript(missionKeys);
                // 1. Generate mission_names.txt from dropdown values
                String missionListPath = "seadas-toolbox/seadas-earthdata-cloud-toolbox/src/main/resources/json-files/mission_names.txt";
                MissionNameWriter.writeMissionNames(metadataMap.keySet(), missionListPath);

                // 2. Call Python to generate mission_date_ranges.json
                PythonScriptRunner_old.runMissionDateRangeScript(missionListPath);
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
//
//        GridBagConstraints gbc = new GridBagConstraints();
//        gbc.insets = new Insets(0, 10, 5, 10);
//        gbc.gridx = 0;
//        gbc.gridy = 0;
//        gbc.gridwidth = 2;  // Full width
//        gbc.anchor = GridBagConstraints.WEST;
//        gbc.fill = GridBagConstraints.NONE;
//
//        add(UIComponentsFactory.createHeaderPanel(), gbc);

        List<String> sortedSatellites = new ArrayList<>(metadataMap.keySet());
        Collections.sort(sortedSatellites);
        satelliteDropdown = new JComboBox<>(sortedSatellites.toArray(new String[0]));

        levelDropdown = new JComboBox<>();
        productDropdown = new JComboBox<>();

        satelliteDropdown.addActionListener(e -> {
            updateLevelsAndProducts();
            String satelliteKey = (String) satelliteDropdown.getSelectedItem();
            String minDate = missionDateRanges.containsKey(satelliteKey) ? missionDateRanges.get(satelliteKey)[0] : "N/A";
            String maxDate = missionDateRanges.containsKey(satelliteKey) ? missionDateRanges.get(satelliteKey)[1] : "N/A";
            updateDateRangeLabel(minDate, maxDate);
            if (satelliteKey != null && missionDateRanges.containsKey(satelliteKey)) {
                String[] range = missionDateRanges.get(satelliteKey);
                String tooltip = "Valid date range: " + range[0] + " to " + range[1];
                temporalPanel.setToolTipText(tooltip);
                // Set tooltip on individual components
                ((JComponent) startDatePicker.getComponent(0)).setToolTipText(tooltip);
                ((JComponent) startDatePicker.getComponent(1)).setToolTipText(tooltip);
                ((JComponent) endDatePicker.getComponent(0)).setToolTipText(tooltip);
                ((JComponent) endDatePicker.getComponent(1)).setToolTipText(tooltip);

            } else {
                temporalPanel.setToolTipText("Valid date range not available.");
            }
        });

        //gbc.gridx = 0; gbc.gridy++;
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

        //TODO: Add a button to refresh the metadata
//        JButton refreshButton = new JButton("Refresh Metadata");
//        refreshButton.addActionListener(e -> refreshMetadata());
//        paginationPanel.add(refreshButton);
        add(paginationPanel, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(e -> fetchGranules());
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(
                e -> {
                    if (parentDialog != null) {
                        parentDialog.dispose();
                    }
                });
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(searchButton);
        buttonPanel.add(cancelButton);
        gbc.gridwidth = 2;
        add(buttonPanel, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        String[] columnNames = {"File Name", "Download File"};
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
        imagePreviewHelper.attachToTable(resultsTable, fileLinkMap);
        resultsTable.setDefaultRenderer(Object.class, new TableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                JLabel label = new JLabel("<html><a href='#'>" + value + "</a></html>");
                return label;
            }
        });

//        resultsTable.addMouseMotionListener(new MouseMotionAdapter() {
//            @Override
//            public void mouseMoved(MouseEvent e) {
//                int row = resultsTable.rowAtPoint(e.getPoint());
//                int col = resultsTable.columnAtPoint(e.getPoint());
//
//                // Only show preview if hovering over the File Name column (e.g., column 0)
//                if (col == 0 && row >= 0) {
//                    String fileName = (String) tableModel.getValueAt(row, 0);
//                    showImagePreview(fileName, e.getLocationOnScreen());
//                } else {
//                    hideImagePreview();
//                }
//            }
//        });

        resultsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = resultsTable.rowAtPoint(e.getPoint());
                int col = resultsTable.columnAtPoint(e.getPoint());

                // Only act if the clicked column is the File Name column (e.g., 0)
                if (col == 0) {
                    String fileName = (String) tableModel.getValueAt(row, 0);
                    String browseUrl = BrowseImagePreview.getFullImageUrl(fileName);
                    if (browseUrl != null) {
                        try {
                            Desktop.getDesktop().browse(new URI(browseUrl));
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
        });

        resultsTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {

                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (column == 0 && value instanceof String) {
                    String fileName = (String) value;
                    String tooltip = fileSpatialMap.getOrDefault(fileName, "No spatial info");
                    ((JComponent) c).setToolTipText(tooltip);
                }
                return c;
            }
        });

//        resultsTable.addMouseListener(new MouseAdapter() {
//            public void mouseClicked(MouseEvent e) {
//                int row = resultsTable.rowAtPoint(e.getPoint());
//                if (row >= 0) {
//                    String fileName = (String) tableModel.getValueAt(row, 0);
//                    String imageUrl = getPreviewUrl(fileName);
//                    if (imageUrl != null) {
//                        showImageInDialog(imageUrl);
//                    }
//                }
//            }
//        });

        resultsTable.addMouseListener(new MouseAdapter() {
            public void mouseExited(MouseEvent e) {
                hideImagePreview();
            }
        });

        resultsTable.setFillsViewportHeight(true);
        JScrollPane scrollPane = new JScrollPane(resultsTable);
        scrollPane.setPreferredSize(new Dimension(800, 300));


        // Results container that holds the table and pagination
        resultsContainer = new JPanel(new BorderLayout());
        resultsContainer.setVisible(false); // 👈 initially hidden
        resultsContainer.setPreferredSize(new Dimension(750, 400));  // Adjust height as needed

        resultsContainer.removeAll();  // clean up old content if any
        resultsContainer.add(scrollPane, BorderLayout.CENTER);
        resultsContainer.add(createPaginationPanel(), BorderLayout.SOUTH);
        resultsContainer.setVisible(false); // 👈 initially hidden

// Add the container to the frame layout
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 3;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        add(resultsContainer, gbc);


        if (satelliteDropdown.getItemCount() > 0) {
            satelliteDropdown.setSelectedIndex(0);
            updateLevelsAndProducts();
        }
    }

    private void showImageInDialog(String imageUrl) {
        try {
            Image originalImage = ImageIO.read(new URL(imageUrl));
            if (originalImage == null) {
                JOptionPane.showMessageDialog(this, "Image not available or not valid.");
                return;
            }

            ImageIcon icon = new ImageIcon(originalImage);
            JLabel label = new JLabel(icon);
            JScrollPane imageScrollPane = new JScrollPane(label);

            JSlider zoomSlider = new JSlider(10, 400, 100); // zoom range: 10% to 400%
            zoomSlider.setMajorTickSpacing(50);
            zoomSlider.setPaintTicks(true);
            zoomSlider.setPaintLabels(true);

            zoomSlider.addChangeListener(e -> {
                int zoomPercent = zoomSlider.getValue();
                int newWidth = originalImage.getWidth(null) * zoomPercent / 100;
                int newHeight = originalImage.getHeight(null) * zoomPercent / 100;
                Image scaled = originalImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
                label.setIcon(new ImageIcon(scaled));
            });

            JPanel panel = new JPanel(new BorderLayout());
            panel.add(imageScrollPane, BorderLayout.CENTER);
            panel.add(zoomSlider, BorderLayout.SOUTH);
            panel.setPreferredSize(new Dimension(800, 600));

            JOptionPane.showMessageDialog(this, panel, "Image Preview (Zoom)", JOptionPane.PLAIN_MESSAGE);

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Failed to load image preview.");
            e.printStackTrace();
        }
    }

    private void showImagePreview(String fileName, Point screenLocation) {
        if (imagePreviewWindow == null) {
            imagePreviewWindow = new JWindow();
            imageLabel = new JLabel();
            imagePreviewWindow.getContentPane().add(imageLabel);
        }

        try {
            String imageUrl = getPreviewUrl(fileName);
            ImageIcon icon = BrowseImagePreview.loadPreviewImage(fileName);
            if (icon != null) {
                imageLabel.setIcon(icon);
                imagePreviewWindow.pack();
                imagePreviewWindow.setLocation(screenLocation.x + 20, screenLocation.y + 20);
                imagePreviewWindow.setVisible(true);
            } else {
                hideImagePreview();
            }
        } catch (Exception e) {
            hideImagePreview();
        }
    }
    private void hideImagePreview() {
        if (imagePreviewWindow != null) {
            imagePreviewWindow.setVisible(false);
        }
    }

    private void downloadSelectedFiles() {
        if (earthdataCredentials == null) {
            earthdataCredentials = WebPageFetcherWithJWT.getCredentials("urs.earthdata.nasa.gov");
            if (earthdataCredentials == null) {
                JOptionPane.showMessageDialog(this, "Earthdata credentials not found in ~/.netrc");
                return;
            }
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Directory to Save Files");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (fileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File selectedDir = fileChooser.getSelectedFile();

        List<String> filesToDownload = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (Boolean.TRUE.equals(tableModel.getValueAt(i, 1))) {
                filesToDownload.add((String) tableModel.getValueAt(i, 0));
            }
        }

        if (filesToDownload.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No files selected for download.");
            return;
        }

        SwingUtilities.invokeLater(() -> showProgressDialog(this, filesToDownload.size()));

        new Thread(() -> {

            int downloadedCount = 0;

            for (int i = 0; i < filesToDownload.size(); i++) {
                String fileName = filesToDownload.get(i);
                String url = fileLinkMap.get(fileName);
                if (url != null && downloadFile(url, selectedDir.toPath())) {
                    lockFileCheckbox(fileName);
                    downloadedCount++;
                }
                int progress = i + 1;
                SwingUtilities.invokeLater(() -> updateProgressBar(progress));
                lockFileCheckbox(fileName);
            }

            int finalDownloadedCount = downloadedCount;
// First hide the progress dialog
            SwingUtilities.invokeLater(() -> hideProgressDialog());

// Then show the message in a separate EDT task
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this, finalDownloadedCount + " file(s) downloaded to:\n" + selectedDir.getAbsolutePath());
            });

        }).start();
    }


    private boolean downloadFile(String fileUrl, Path outputDir) {
        try {
            String fileName = extractFileNameFromUrl(fileUrl);
            String token = WebPageFetcherWithJWT.getAccessToken("urs.earthdata.nasa.gov");

            HttpURLConnection conn = (HttpURLConnection) new URL(fileUrl).openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("Authorization", "Bearer " + token);

            int status = conn.getResponseCode();
            if (status == HttpURLConnection.HTTP_MOVED_PERM || status == HttpURLConnection.HTTP_MOVED_TEMP || status == 303) {
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
                JOptionPane.showMessageDialog(this, "Download failed for " + fileUrl + "\nHTTP status: " + status);
                return false;
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Download failed: " + e.getMessage());
            return false;
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

        List<String> sortedLevels = new ArrayList<>(json.keySet());
        sortedLevels.removeIf(level -> "L0".equalsIgnoreCase(level));
        Collections.sort(sortedLevels);
        for (String level : sortedLevels) {
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
            // Collect product names and tooltips
            List<String> productNames = new ArrayList<>();
            Map<String, String> tooltipMap = new HashMap<>();

            for (int i = 0; i < products.length(); i++) {
                JSONObject prod = products.getJSONObject(i);
                String name = prod.optString("product_name", "Unknown");
                String shortName = prod.optString("short_name", "");
                productNames.add(name);
                tooltipMap.put(name, shortName);
            }

            // Sort alphabetically
            Collections.sort(productNames);

            // Populate dropdown
            for (String name : productNames) {
                productDropdown.addItem(name);
                productNameTooltips.put(name, tooltipMap.get(name));
            }
        }
    }


    private JPanel createTemporalPanel() {
        temporalPanel = new JPanel(new GridBagLayout());
        temporalPanel.setBorder(BorderFactory.createTitledBorder("Temporal Filter"));

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
        temporalPanel.add(new JLabel("Start Date:"), c);

        // Start Date Picker
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        temporalPanel.add(startDatePicker, c);

        // End Date Label
        c.gridx = 0;
        c.gridy = 1;
        c.fill = GridBagConstraints.NONE;
        temporalPanel.add(new JLabel("End Date:"), c);

        // End Date Picker
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        temporalPanel.add(endDatePicker, c);

        dateRangeHintLabel = new JLabel(" "); // Declare this as a field
        dateRangeHintLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        dateRangeHintLabel.setForeground(Color.DARK_GRAY);

        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 2;
        temporalPanel.add(dateRangeHintLabel, c);

        dateRangeLabel = new JLabel("Valid date range: ");
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(5, 5, 5, 5);
        temporalPanel.add(dateRangeLabel, c);

        return temporalPanel;
    }
    private void updateDateRangeLabel(String minDate, String maxDate) {
        String labelText = "Valid date range: " + minDate + " to " + maxDate;
        if (dateRangeLabel != null) {
            dateRangeLabel.setText(labelText);
        }
    }

    private void updateDatePickerBounds() {
        String selectedSatellite = (String) satelliteDropdown.getSelectedItem();
        if (missionDateRanges.containsKey(selectedSatellite)) {
            String[] range = missionDateRanges.get(selectedSatellite);
            String minDateStr = range[0];
            String maxDateStr = range[1];

            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                Date minDate = sdf.parse(minDateStr);
                Date maxDate = sdf.parse(maxDateStr);

                // Set constraints
                UtilDateModel startModel = (UtilDateModel) startDatePicker.getModel();
                //startModel.setDate(range[0]);
//                startModel.setMinimum(minDate);
//                startModel.setMaximum(maxDate);

                UtilDateModel endModel = (UtilDateModel) endDatePicker.getModel();
//                endModel.setMinimum(minDate);
//                endModel.setMaximum(maxDate);

                // Update label
                dateRangeHintLabel.setText("Valid range for " + selectedSatellite + ": " + minDateStr + " to " + maxDateStr);
            } catch (java.text.ParseException e) {
                throw new RuntimeException(e);
            }
        } else {
            dateRangeHintLabel.setText("");
        }
    }
    private void updateDateRangeHint() {
        String satellite = (String) satelliteDropdown.getSelectedItem();
        if (satellite != null && missionDateRanges.containsKey(satellite)) {
            String[] range = missionDateRanges.get(satellite);
            dateRangeHintLabel.setText("Valid mission range: " + range[0] + " to " + range[1]);
        } else {
            dateRangeHintLabel.setText(" ");
        }
    }

    private boolean isDateInValidRange(String satellite, Date date) {
        if (date == null || !missionDateRanges.containsKey(satellite)) return true;

        try {
            String[] range = missionDateRanges.get(satellite);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date min = sdf.parse(range[0]);
            Date max = sdf.parse(range[1]);

            return !date.before(min) && !date.after(max);
        } catch (Exception e) {
            return true;
        }
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

    private Map<String, String[]> fetchMissionDateRanges(Set<String> satelliteKeys) {
        Map<String, String[]> ranges = new HashMap<>();

        Map<String, String[]> knownMissionsMap = Map.ofEntries(
                Map.entry("SeaWiFS", new String[]{"1997-09-01", "2010-12-31"}),
                Map.entry("MODIS_Aqua", new String[]{"2002-05-04", "present"}),
                Map.entry("MODIS_Terra", new String[]{"1999-12-18", "present"}),
                Map.entry("VIIRS", new String[]{"2011-10-28", "present"}),
                Map.entry("Landsat_8", new String[]{"2013-02-11", "present"}),
                Map.entry("PACE_HARP2", new String[]{"2024-02-08", "present"}),
                Map.entry("PACE_OCI", new String[]{"2024-02-08", "present"}),
                Map.entry("PACE_SPEXONE", new String[]{"2024-02-08", "present"}),
                Map.entry("PACE", new String[]{"2024-02-08", "present"}),
                Map.entry("HAWKEYE", new String[]{"2024-01-01", "present"}),
                Map.entry("MERGED_S3_OLCI", new String[]{"2016-02-16", "present"}),
                Map.entry("Sentinel_3_OLCI", new String[]{"2016-02-16", "present"}),
                Map.entry("Sentinel_3A_OLCI", new String[]{"2016-02-16", "present"}),
                Map.entry("Sentinel_3B_OLCI", new String[]{"2018-04-25", "present"})
        );

        List<String> suspiciousDates = List.of(
                "1960-01-01", "1347-01-01", "1900-01-01", "1970-01-01", "2099-12-31", "2100-12-31"
        );

        for (String mission : satelliteKeys) {
            try {
                String encodedMission = URLEncoder.encode(mission, StandardCharsets.UTF_8);
                String urlStr = "https://cmr.earthdata.nasa.gov/search/collections.json?keyword=" + encodedMission + "&page_size=100";
                HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
                conn.setRequestProperty("Accept", "application/json");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();

                JSONObject json = new JSONObject(response.toString());
                JSONArray collections = json.getJSONObject("feed").getJSONArray("entry");

                List<String> startDates = new ArrayList<>();
                List<String> endDates = new ArrayList<>();

                for (int i = 0; i < collections.length(); i++) {
                    JSONObject col = collections.getJSONObject(i);
                    String s = col.optString("time_start", null);
                    String e = col.optString("time_end", null);

                    if (s != null && !suspiciousDates.contains(s.split("T")[0])) startDates.add(s.split("T")[0]);
                    if (e != null && !e.equals("present") && !suspiciousDates.contains(e.split("T")[0])) endDates.add(e.split("T")[0]);
                }

                String earliest = startDates.isEmpty() ? null : Collections.min(startDates);
                String latest = endDates.isEmpty() ? "present" : Collections.max(endDates);

                if (earliest == null || suspiciousDates.contains(earliest) || suspiciousDates.contains(latest)) {
                    if (knownMissionsMap.containsKey(mission)) {
                        String[] fallback = knownMissionsMap.get(mission);
                        System.out.println("⚠️ Suspicious CMR dates for " + mission + ", using fallback: " + fallback[0] + " → " + fallback[1]);
                        ranges.put(mission, fallback);
                    } else {
                        System.err.println("❌ No valid date range found for " + mission);
                    }
                } else {
                    ranges.put(mission, new String[]{earliest, latest});
                    System.out.println("🗓️ " + mission + ": " + earliest + " → " + latest);
                }

            } catch (Exception e) {
                System.err.println("❌ Error fetching range for " + mission + ": " + e.getMessage());
                if (knownMissionsMap.containsKey(mission)) {
                    String[] fallback = knownMissionsMap.get(mission);
                    System.out.println("🔁 Using fallback for " + mission + ": " + fallback[0] + " → " + fallback[1]);
                    ranges.put(mission, fallback);
                }
            }
        }

        saveMissionDateRangesToFile(ranges);

        return ranges;
    }

    private void saveMissionDateRangesToFile(Map<String, String[]> ranges) {
        JSONObject json = new JSONObject();
        for (Map.Entry<String, String[]> entry : ranges.entrySet()) {
            String[] range = entry.getValue();
            JSONObject rangeObj = new JSONObject();
            rangeObj.put("start", range[0]);
            rangeObj.put("end", range[1]);
            json.put(entry.getKey(), rangeObj);
        }

        try {
            Path outFile = Paths.get(System.getProperty("user.dir"),
                    "seadas-toolbox",
                    "seadas-earthdata-cloud-toolbox",
                    "src",
                    "main",
                    "resources",
                    "json-files",
                    "mission_date_ranges.json");

            Files.createDirectories(outFile.getParent());
            Files.writeString(outFile, json.toString(4), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            System.out.println("Saved mission date ranges to: " + outFile.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Failed to save mission date ranges: " + e.getMessage());
        }
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

        Date startDateVal = (Date) startDatePicker.getModel().getValue();
        Date endDateVal = (Date) endDatePicker.getModel().getValue();
        String satellite = (String) satelliteDropdown.getSelectedItem();

        if (!isDateInValidRange(satellite, startDateVal) || !isDateInValidRange(satellite, endDateVal)) {
            JOptionPane.showMessageDialog(this,
                    "Selected date(s) are outside the valid mission range for " + satellite + ".\n" +
                            "Please enter dates within the supported range.",
                    "Invalid Date Range", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String minLat = minLatField.getText().trim();
        String maxLat = maxLatField.getText().trim();
        String minLon = minLonField.getText().trim();
        String maxLon = maxLonField.getText().trim();
        boolean hasSpatial = !minLat.isEmpty() && !maxLat.isEmpty() && !minLon.isEmpty() && !maxLon.isEmpty();

        String dayNightFlag = null;
        if (dayButton.isSelected()) {
            dayNightFlag = "Day";
        } else if (nightButton.isSelected()) {
            dayNightFlag = "Night";
        }
        // "Both" selected means we skip adding the flag

        int pageSize = 2000;
        int page = 1;
        int totalFetched = 0;

        resultsContainer.setVisible(false); // before the loop

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

                if (dayNightFlag != null) {
                    urlBuilder.append("&day_night_flag=").append(dayNightFlag);
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

                    String fileName = entry.optString("producer_granule_id", "");
                    JSONArray links = entry.optJSONArray("links");

                    if (fileName != null && !fileName.isEmpty() && links != null) {
                        for (int j = 0; j < links.length(); j++) {
                            JSONObject link = links.getJSONObject(j);

                            String spatialInfo = "No spatial info";
                            if (entry.has("boxes")) {
                                JSONArray boxes = entry.getJSONArray("boxes");
                                if (!boxes.isEmpty()) {
                                    spatialInfo = "Bounding Box: " + boxes.getString(0);
                                }
                            } else if (entry.has("polygons")) {
                                spatialInfo = "Polygon coverage available";
                            } else if (entry.has("center")) {
                                spatialInfo = "Center: " + entry.getString("center");
                            }
                            fileSpatialMap.put(fileName, spatialInfo);

                            if (link.has("href") && link.getString("href").endsWith(".nc")) {
                                String href = link.getString("href");
                                allGranules.add(new String[]{fileName, href});
                                tableModel.addRow(new Object[]{fileName});
                                resultsTable.putClientProperty(fileName, href);
                                fileLinkMap.put(fileName, href);

                                break;
                            }


                        }
                    }

                    totalFetched++;
                    if (totalFetched >= maxApiResults) break;
                }

                if (entries.length() < pageSize) break;
                page++;
            }

            if (!allGranules.isEmpty()) {
                resultsContainer.setVisible(true);
                resultsContainer.revalidate();
                resultsContainer.repaint();
                SwingUtilities.invokeLater(() -> {
                    resultsContainer.revalidate();
                    resultsContainer.repaint();
                });
                Window topLevelWindow = SwingUtilities.getWindowAncestor(resultsContainer);
                if (topLevelWindow != null) {
                    topLevelWindow.revalidate();
                    topLevelWindow.repaint();
                    topLevelWindow.pack(); // optional: resizes the window to fit content
                }
            }

            // Setup pagination
            totalPages = (int) Math.ceil((double) allGranules.size() / getResultsPerPage());
            currentPage = 1;
            updateResultsTable(currentPage);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


//    private void refreshMetadata() {
//        int confirm = JOptionPane.showConfirmDialog(this,
//                "This will refresh metadata and mission date ranges.\nDo you want to continue?",
//                "Refresh Metadata", JOptionPane.YES_NO_OPTION);
//
//        if (confirm == JOptionPane.YES_OPTION) {
//            try {
//                // Run scripts
//                PythonScriptRunner_old.runAllScripts();
//
//                // Reload data
//                loadMetadata();  // Should re-read updated JSON files
//                missionDateRanges = metadataLoader.loadMissionDateRanges();
//
//                // Repopulate dropdowns
//                populateSatelliteDropdown();
//                updateLevels();
//                updateProducts();
//
//                JOptionPane.showMessageDialog(this, "✅ Metadata and date ranges refreshed successfully.");
//            } catch (Exception ex) {
//                JOptionPane.showMessageDialog(this, "❌ Failed to refresh metadata.\n" + ex.getMessage());
//                ex.printStackTrace();
//            }
//        }
//    }

    private JPanel createPaginationPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Left side: Pagination buttons
        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton prevButton = new JButton("Previous");
        JButton nextButton = new JButton("Next");
        pageLabel = new JLabel("Page 1");

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

        navPanel.add(prevButton);
        navPanel.add(pageLabel);
        navPanel.add(nextButton);

        // Right side: Download button
        JButton downloadButton = new JButton("Download");
        downloadButton.addActionListener(e -> downloadSelectedFiles());
        JPanel downloadPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        downloadPanel.add(downloadButton);

        // Add both to main panel
        panel.add(navPanel, BorderLayout.CENTER);
        panel.add(downloadPanel, BorderLayout.EAST);

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

        JRadioButton dayButton = new JRadioButton("Day", true);
        JRadioButton nightButton = new JRadioButton("Night");
        JRadioButton bothButton = new JRadioButton("Both");

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


//    public static void main(String[] args) {
//        SwingUtilities.invokeLater(() -> new OBDAACDataBrowser().setVisible(true));
//    }
public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> {
        JFrame frame = new JFrame("OB_CLOUD Data Browser via Harmony Search Service");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(new OBDAACDataBrowser(new JDialog()));
        frame.setSize(900, 700);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    });
}

//    String imageUrl = ImagePreviewHandler.getPreviewUrl(fileName);
//ImagePreviewHandler.showImageInDialog(this, imageUrl);

}
