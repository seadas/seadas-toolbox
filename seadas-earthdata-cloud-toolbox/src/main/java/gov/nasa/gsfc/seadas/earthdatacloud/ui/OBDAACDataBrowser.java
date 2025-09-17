package gov.nasa.gsfc.seadas.earthdatacloud.ui;

import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import gov.nasa.gsfc.seadas.earthdatacloud.preferences.Earthdata_Cloud_Controller;
import gov.nasa.gsfc.seadas.earthdatacloud.util.FileDownloadManager;
import org.esa.snap.rcp.SnapApp;
import gov.nasa.gsfc.seadas.earthdatacloud.util.*;
import org.esa.snap.ui.UIUtils;
import org.esa.snap.ui.tool.ToolButtonFactory;
import org.jdatepicker.impl.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.openide.util.HelpCtx;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class OBDAACDataBrowser extends JPanel {
    private JComboBox<String> satelliteDropdown, levelDropdown, productDropdown;
    private JDatePickerImpl startDatePicker, endDatePicker;
    private JTextField minLatField, maxLatField, minLonField, maxLonField, coordinates, boxSize;
    private JComboBox regions;
    private JComboBox locations;
    private JComboBox user_regions;
    private JComboBox user_locations;
    private boolean working = false;
    private JTable resultsTable;
    private DefaultTableModel tableModel;
    private final Map<String, String> productNameTooltips = new HashMap<>();

    private final Map<String, JSONObject> metadataMap = new HashMap<>();
    private JRadioButton dayButton, nightButton, bothButton;
    private final Map<String, String> fileLinkMap = new HashMap<>();

    private int currentPage = 1;
    private int totalFetched = 1;
    private int totalPages = 1;
    private JSpinner maxApiResultsSpinner;
    private JSpinner resultsPerPageSpinner;

    private JLabel pageLabel;
    private JLabel fetchedLabel;
    private List<String[]> allGranules = new ArrayList<>();
    private JLabel dateRangeHintLabel = new JLabel();  // Declare as a class member
    private JPanel temporalPanel;
    private JLabel dateRangeLabel; // add this as a field so we can update it later

    private JPanel resultsContainer;
    private final Map<String, String> fileSpatialMap = new HashMap<>();
    private JDialog parentDialog;
    private ImagePreviewHelper imagePreviewHelper;
    private FileDownloadManager downloadManager;

    private Component helpButton = null;
    private final static String HELP_ID = "earthdataCloudSearch";
    private final static String HELP_ICON = "icons/Help24.gif";

    Map<String, String[]> missionDateRanges = Map.of(
            "SeaHawk/HawkEye", new String[]{"2018-12-01", "2023-12-31"},
            "MODISA", new String[]{"2002-07-04", "2024-12-31"},
            "VIIRSN", new String[]{"2011-10-28", "2024-12-31"}
    );

    public OBDAACDataBrowser(JDialog parentDialog) {
        this.parentDialog = parentDialog;
        setLayout(new GridBagLayout());
        setSize(850, 600);
        setLayout(new GridBagLayout());
        loadMetadata();
        imagePreviewHelper = new ImagePreviewHelper();
        downloadManager = new FileDownloadManager();
        loadMissionDateRangesFromFile();
        initComponents();
    }

    private void loadMissionDateRangesFromFile() {
        missionDateRanges = new HashMap<>();

        Path externalFile = Paths.get("seadas-toolbox", "seadas-earthdata-cloud-toolbox",
                "src", "main", "resources", "json-files", "mission_date_ranges.json");

        if (Files.exists(externalFile)) {
//            System.out.println("Loading mission_date_ranges.json from external path: " + externalFile.toAbsolutePath());
            try (BufferedReader reader = Files.newBufferedReader(externalFile, StandardCharsets.UTF_8)) {
                loadDateRangesFromReader(reader);
                return;
            } catch (IOException e) {
                System.err.println("Failed to read external mission date ranges: " + e.getMessage());
            }
        }

//        System.out.println("Loading mission_date_ranges.json from classpath");
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("json-files/mission_date_ranges.json")) {
            if (input != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
                loadDateRangesFromReader(reader);
            } else {
//                System.err.println("Resource not found: json-files/mission_date_ranges.json");
            }
        } catch (IOException e) {
//            System.err.println("Failed to read mission date ranges from classpath: " + e.getMessage());
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
        JSONTokener tokener;
        Set<String> missionKeys = new HashSet<>();
        try {
            if (!usedExternal) {
                String[] resourceFiles = {
                        "CZCS.json", "HAWKEYE.json", "HICO.json", "MERGED_S3_OLCI.json", "MERIS.json",
                        "MODISA.json", "MODIST.json", "OCTS.json", "OLCIS3A.json", "OLCIS3B.json",
                        "PACE_HARP2.json", "PACE_OCI.json", "PACE_SPEXONE.json", // Add expected resources
                        "SeaWiFS.json", "VIIRSJ1.json", "VIIRSJ2.json", "VIIRSN.json"
                };

                for (String fileName : resourceFiles) {
                    String key = fileName.replace(".json", "");
                    InputStream input = getClass().getClassLoader()
                            .getResourceAsStream("json-files/" + fileName);
                    if (input == null) {
//                        System.err.println("⚠ Missing embedded resource: " + fileName);
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

        setupSatelliteDropdowns();
        setupSatelliteDropdownListener();

        gbc.gridy++;
        gbc.gridx = 0;
        add(createFilterPanel(), gbc);

        // Initialize spinners first
        maxApiResultsSpinner = new JSpinner();
        resultsPerPageSpinner = new JSpinner();
        
        OBDAACDataBrowserPanels panels = new OBDAACDataBrowserPanels();
        JPanel paginationPanel = panels.createSpinnerPanel(maxApiResultsSpinner, resultsPerPageSpinner);
        JPanel buttonPanel = panels.createButtonPanel(
            this::runFetchWrapper,
            () -> {
                if (parentDialog != null) {
                    parentDialog.dispose();
                }
            }
        );

        gbc.gridy++;
        gbc.gridx = 0;
        add(createPaginationButtonPanel(paginationPanel, buttonPanel), gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        setupResultsTable();
        setupResultsContainer(gbc);
    }

    private void setupSatelliteDropdowns() {
        List<String> sortedSatellites = new ArrayList<>(metadataMap.keySet());
        Collections.sort(sortedSatellites);
        satelliteDropdown = new JComboBox<>(sortedSatellites.toArray(new String[0]));

        levelDropdown = new JComboBox<>();
        productDropdown = new JComboBox<>();
    }

    private void setupSatelliteDropdownListener() {
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
                ((JComponent) startDatePicker.getComponent(0)).setToolTipText(tooltip);
                ((JComponent) startDatePicker.getComponent(1)).setToolTipText(tooltip);
                ((JComponent) endDatePicker.getComponent(0)).setToolTipText(tooltip);
                ((JComponent) endDatePicker.getComponent(1)).setToolTipText(tooltip);

            } else {
                temporalPanel.setToolTipText("Valid date range not available.");
            }
        });
    }

    private JPanel createPaginationButtonPanel(JPanel panel1, JPanel panel2) {
        GridBagConstraints gbc = new GridBagConstraints();
        GridBagLayout layout = new GridBagLayout();

        GridBagConstraints c = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.NONE;

        JPanel panel = new JPanel(layout);

        gbc.gridy = 0;
        gbc.gridx = 0;
        panel.add(panel1, gbc);

        gbc.gridx++;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JLabel fill = new JLabel("");
        panel.add(fill, gbc);
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;

        gbc.gridx++;
        gbc.anchor = GridBagConstraints.NORTHEAST;
        panel.add(panel2, gbc);


        helpButton = getHelpButton(HELP_ID);
        gbc.gridx++;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(helpButton, gbc);

        return panel;
    }

    private void setupResultsTable() {
        createTableModel();
        createTable();
        configureTableAppearance();
        setupTableRenderers();
        setupTableEventHandlers();
    }

    private void createTableModel() {
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
    }

    private void createTable() {
        resultsTable = new JTable(tableModel);
    }

    private void configureTableAppearance() {
        Font fontOriginal = resultsTable.getFont();

        double fontSizeZoom = Earthdata_Cloud_Controller.getPreferenceResultFontZoom();
        if (fontSizeZoom < Earthdata_Cloud_Controller.PROPERTY_RESULTS_FONT_ZOOM_MODE_MIN_VALUE || fontSizeZoom > Earthdata_Cloud_Controller.PROPERTY_RESULTS_FONT_ZOOM_MODE_MAX_VALUE) {
            fontSizeZoom = Earthdata_Cloud_Controller.PROPERTY_RESULTS_FONT_ZOOM_MODE_DEFAULT;
        }

        int fontSizeOriginal = fontOriginal.getSize();
        int fontSizeNew = (int) Math.round(fontSizeOriginal * fontSizeZoom / 100);
        Font fontNew = new Font(fontOriginal.getName(), fontOriginal.getStyle(), fontSizeNew);
        resultsTable.setFont(fontNew);

        int rowHeightOriginal = resultsTable.getRowHeight();
        int rowBuffer = 30;
        int rowHeightNew = (int) Math.round(rowHeightOriginal * (fontSizeZoom + rowBuffer) / 100);
        resultsTable.setRowHeight(rowHeightNew);

        JLabel tmpCol1Label = new JLabel(" Download File ");
        int tmpVol1LabelWidth = (int) Math.ceil(tmpCol1Label.getPreferredSize().getWidth());
        resultsTable.getColumnModel().getColumn(1).setPreferredWidth(tmpVol1LabelWidth);
        resultsTable.getColumnModel().getColumn(1).setMinWidth(tmpVol1LabelWidth);
        resultsTable.getColumnModel().getColumn(1).setMaxWidth(tmpVol1LabelWidth);
    }

    private void setupTableRenderers() {
        imagePreviewHelper.attachToTable(resultsTable, fileLinkMap, parentDialog);
        resultsTable.setDefaultRenderer(Object.class, new TableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                JLabel label = new JLabel("<html><a href='#'>" + value + "</a></html>");
                return label;
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
                }

                if (column == 0) {
                    setHorizontalAlignment(JLabel.LEFT);
                }
                if (column == 1) {
                    setHorizontalAlignment(JLabel.LEFT);
                }
                return c;
            }
        });
    }

    private void setupTableEventHandlers() {
        resultsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = resultsTable.rowAtPoint(e.getPoint());
                int col = resultsTable.columnAtPoint(e.getPoint());

                if (Earthdata_Cloud_Controller.getPreferenceImageLinkInclude() && col == 0) {
                    String fileName = (String) tableModel.getValueAt(row, 0);
                    String browseUrl = fileName + ".png";
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
    }

    private void setupResultsContainer(GridBagConstraints gbc) {
        resultsTable.setFillsViewportHeight(true);
        JScrollPane scrollPane = new JScrollPane(resultsTable);
        scrollPane.setPreferredSize(new Dimension(700, 500));

        resultsContainer = new JPanel(new BorderLayout());
        resultsContainer.setVisible(false); // 👈 initially hidden
        resultsContainer.setPreferredSize(new Dimension(700, 600));  // Adjust height as needed

        resultsContainer.removeAll();  // clean up old content if any
        resultsContainer.add(scrollPane, BorderLayout.CENTER);
        resultsContainer.add(createPaginationPanel(), BorderLayout.SOUTH);
        resultsContainer.setVisible(false); // 👈 initially hidden

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 3;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        add(resultsContainer, gbc);

        if (satelliteDropdown.getItemCount() > 0) {
            int satelliteIndex = 0;
            String satellitePref = Earthdata_Cloud_Controller.getPreferenceSatellite();
            if (satellitePref != null && satellitePref.trim().length() > 0) {
                for (int i = 0; i < satelliteDropdown.getItemCount(); i++) {
                    String satellite = (String) satelliteDropdown.getItemAt(i);
                    if (satellitePref.trim().equalsIgnoreCase(satellite)) {
                        satelliteIndex = i;
                    }
                }
            }
            satelliteDropdown.setSelectedIndex(satelliteIndex);
            updateLevelsAndProducts();
        }
    }

    // Add back the correct createPaginationPanel method
    private JPanel createPaginationPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel fetchedPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton prevButton = new JButton("Previous");
        JButton nextButton = new JButton("Next");
        pageLabel = new JLabel("Page 1");
        fetchedLabel = new JLabel("");

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

        fetchedPanel.add(fetchedLabel);
        navPanel.add(prevButton);
        navPanel.add(pageLabel);
        navPanel.add(nextButton);

        JButton downloadButton = new JButton("Download");
        downloadButton.addActionListener(e -> downloadSelectedFiles());
        
        JButton subsetButton = new JButton("Subset");
        subsetButton.addActionListener(e -> subsetSelectedFiles());
        
        JPanel downloadPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        downloadPanel.add(subsetButton);
        downloadPanel.add(downloadButton);

        panel.add(fetchedPanel, BorderLayout.WEST);
        panel.add(navPanel, BorderLayout.CENTER);
        panel.add(downloadPanel, BorderLayout.EAST);

        return panel;
    }

    private void runFetchWrapper() {
        ProgressMonitorSwingWorker pmSwingWorker = new ProgressMonitorSwingWorker(SnapApp.getDefault().getMainFrame(),
                "Earthdata Cloud Browser") {

            @Override
            protected Void doInBackground(com.bc.ceres.core.ProgressMonitor pm) throws Exception {

                int totalWorkPlannedMaster = 100;
                int workDoneMaster = 0;
                pm.beginTask("Searching for results", totalWorkPlannedMaster);


                try {
                    fetchGranules(pm);

                    if (pm != null && pm.isCanceled()) {
                        pm.done();
                        return null;
                    }
                } finally {
                    if (pm != null && pm.isCanceled()) {
                        pm.done();
                        return null;
                    }
                    pm.done();
                }

                return null;
            }
        };

        pmSwingWorker.executeWithBlocking();

    }

    private void downloadSelectedFiles() {
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

        downloadManager.downloadSelectedFiles(filesToDownload, fileLinkMap, this, 
            (downloadedCount, downloadDir) -> {
                // Callback when download completes
                for (String fileName : filesToDownload) {
                    lockFileCheckbox(fileName);
                }
            });
    }

    private void subsetSelectedFiles() {
        // Get the first selected file (for now, we'll subset one file at a time)
        String selectedFile = null;
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (Boolean.TRUE.equals(tableModel.getValueAt(i, 1))) {
                selectedFile = (String) tableModel.getValueAt(i, 0);
                break;
            }
        }

        if (selectedFile == null) {
            JOptionPane.showMessageDialog(this, "Please select a file to subset.", "No File Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Get the URL for the selected file
        String fileUrl = fileLinkMap.get(selectedFile);
        if (fileUrl == null) {
            JOptionPane.showMessageDialog(this, "Could not find URL for selected file: " + selectedFile, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Get current spatial bounds from the search dialog
        Double latMin = null, latMax = null, lonMin = null, lonMax = null;
        try {
            if (!minLatField.getText().trim().isEmpty()) {
                latMin = Double.parseDouble(minLatField.getText().trim());
            }
            if (!maxLatField.getText().trim().isEmpty()) {
                latMax = Double.parseDouble(maxLatField.getText().trim());
            }
            if (!minLonField.getText().trim().isEmpty()) {
                lonMin = Double.parseDouble(minLonField.getText().trim());
            }
            if (!maxLonField.getText().trim().isEmpty()) {
                lonMax = Double.parseDouble(maxLonField.getText().trim());
            }
        } catch (NumberFormatException e) {
            // If any of the spatial bounds are invalid, just pass null values
            // The subset dialog will handle this gracefully
        }

        // Open the Harmony subset service dialog with spatial bounds
        HarmonySubsetServiceDialog subsetDialog = new HarmonySubsetServiceDialog(fileUrl, latMin, latMax, lonMin, lonMax);
        subsetDialog.setVisible(true);
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

    private JPanel createLeftPanel() {
        GridBagConstraints gbc = new GridBagConstraints();
        GridBagLayout layout = new GridBagLayout();

        GridBagConstraints c = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;

        JPanel panel = new JPanel(layout);
        gbc.insets = new Insets(0, 0, 0, 0);

        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        panel.add(createSatelliteProductsPanel(), gbc);

        gbc.gridy = 1;
        gbc.gridx = 0;
        gbc.insets = new Insets(10, 0, 0, 10);
        gbc.gridwidth = 1;
        panel.add(createTemporalPanel(), gbc);

        gbc.gridy = 1;
        gbc.gridx = 1;
        gbc.insets = new Insets(10, 0, 0, 0);
        panel.add(createDayNightPanel(), gbc);

        return panel;
    }


    protected AbstractButton getHelpButton(String helpId) {
        if (helpId != null) {
            final AbstractButton helpButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon(HELP_ICON),
                    false);
            helpButton.setToolTipText("Help for OB_CLOUD Data Browser");
            helpButton.setName("helpButton");
            helpButton.addActionListener(e -> getHelpCtx(helpId).display());
            return helpButton;
        }

        return null;
    }

    public HelpCtx getHelpCtx(String helpId) {
        return new HelpCtx(helpId);
    }



    private JPanel createRightPanel() {
        GridBagConstraints gbc = new GridBagConstraints();
        GridBagLayout layout = new GridBagLayout();

        GridBagConstraints c = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;

        JPanel panel = new JPanel(layout);
        gbc.insets = new Insets(0, 0, 0, 0);

        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.weighty = 1;
        panel.add(createSpatialPanel(), gbc);

        return panel;
    }


    private JPanel createFilterPanel() {
        GridBagConstraints gbc = new GridBagConstraints();
        GridBagLayout layout = new GridBagLayout();

        GridBagConstraints c = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;

        JPanel panel = new JPanel(layout);
        gbc.insets = new Insets(0, 10, 0, 10);

        gbc.gridy = 0;
        gbc.gridx = 0;
        panel.add(createLeftPanel(), gbc);

        gbc.gridy = 0;
        gbc.gridx = 1;
        gbc.weighty = 1;
        gbc.insets = new Insets(0, 0, 0, 10);
        panel.add(createRightPanel(), gbc);

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
            int levelIndex = 0;
            String dataLevelPref = Earthdata_Cloud_Controller.getPreferenceDataLevel();
            if (dataLevelPref != null && dataLevelPref.trim().length() > 0) {
                for (int i = 0; i < levelDropdown.getItemCount(); i++) {
                    String level = (String) levelDropdown.getItemAt(i);
                    if (dataLevelPref.trim().equalsIgnoreCase(level)) {
                        levelIndex = i;
                    }
                }
            }

            levelDropdown.setSelectedIndex(levelIndex);
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
            List<String> productNames = new ArrayList<>();
            Map<String, String> tooltipMap = new HashMap<>();

            for (int i = 0; i < products.length(); i++) {
                JSONObject prod = products.getJSONObject(i);
                String name = prod.optString("product_name", "Unknown");
                String shortName = prod.optString("short_name", "");
                productNames.add(name);
                tooltipMap.put(name, shortName);
            }

            Collections.sort(productNames);

            for (String name : productNames) {
                productDropdown.addItem(name);
                productNameTooltips.put(name, tooltipMap.get(name));
            }

            int selectedIndex = 0;
            String productPref = Earthdata_Cloud_Controller.getPreferenceProduct();
            if (productPref != null && productPref.trim().length() > 0) {
                for (int i = 0; i < productDropdown.getItemCount(); i++) {
                    String product = (String) productDropdown.getItemAt(i);
                    if (productPref.trim().equalsIgnoreCase(product)) {
                        selectedIndex = i;
                    }
                }
            }

            productDropdown.setSelectedIndex(selectedIndex);
        }
    }


    private JPanel createSatelliteProductsPanel() {
//        System.out.println("Creating Satellite Products Panel");
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEtchedBorder());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.NORTHWEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(new JLabel("Satellite/Instrument:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        panel.add(satelliteDropdown, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(new JLabel("Data Level:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        panel.add(levelDropdown, gbc);

        gbc.weighty = 1;
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(new JLabel("Product Name:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        panel.add(productDropdown, gbc);

        return panel;
    }


    private JPanel createTemporalPanel() {
        temporalPanel = new JPanel(new GridBagLayout());
        temporalPanel.setBorder(BorderFactory.createTitledBorder("Temporal Filter"));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 2, 2, 2);
        c.anchor = GridBagConstraints.NORTHWEST;

        startDatePicker = createDatePicker();
        endDatePicker = createDatePicker();

        Dimension fieldSize = new Dimension(150, 28);
        startDatePicker.setPreferredSize(fieldSize);
        startDatePicker.setMinimumSize(fieldSize);
        endDatePicker.setPreferredSize(fieldSize);
        endDatePicker.setMinimumSize(fieldSize);

        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.NONE;
        temporalPanel.add(new JLabel("Start Date:"), c);

        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        temporalPanel.add(startDatePicker, c);

        c.gridx = 0;
        c.gridy = 1;
        c.fill = GridBagConstraints.NONE;
        temporalPanel.add(new JLabel("End Date:"), c);

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
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weighty = 1;
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

    private JPanel createClassicBoundingBoxPanel() {
//        System.out.println("Creating Classic BoundingBox Panel");


        JTextField tmpTextField = new JTextField(" 124°00′10″W ");
        Dimension preferredTextFieldSize = tmpTextField.getPreferredSize();
        int preferredColWidth = (int) Math.ceil(preferredTextFieldSize.getWidth() / 2.0);
        Dimension preferredLabelSize = new Dimension(preferredColWidth, 1);

        JPanel panel = new JPanel(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets.top = 0;
        gbc.insets.bottom = 0;
        gbc.insets.left = 0;
        gbc.insets.right = 0;

        gbc.fill = GridBagConstraints.NONE;


        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        JLabel tmpLabel0 = new JLabel("");
        tmpLabel0.setMinimumSize(preferredLabelSize);
        panel.add(tmpLabel0, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JLabel tmpLabel1 = new JLabel("");
        tmpLabel1.setMinimumSize(preferredLabelSize);
        panel.add(tmpLabel1, gbc);

        gbc.gridx = 2;
        gbc.weightx = 1.0;
        JLabel tmpLabel2 = new JLabel("");
        tmpLabel2.setMinimumSize(preferredLabelSize);
        panel.add(tmpLabel2, gbc);

        gbc.gridx = 3;
        gbc.weightx = 1.0;
        JLabel tmpLabel3 = new JLabel("");
        tmpLabel3.setMinimumSize(preferredLabelSize);
        panel.add(tmpLabel3, gbc);

        gbc.gridx = 4;
        gbc.weightx = 1.0;
        JLabel tmpLabel4 = new JLabel("");
        tmpLabel4.setMinimumSize(preferredLabelSize);
        panel.add(tmpLabel4, gbc);

        gbc.gridx = 5;
        gbc.weightx = 1.0;
        JLabel tmpLabel5 = new JLabel("");
        tmpLabel5.setMinimumSize(preferredLabelSize);
        panel.add(tmpLabel5, gbc);



        gbc.gridy = 0;
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        JLabel maxLatLabel = new JLabel(Earthdata_Cloud_Controller.PROPERTY_MAXLAT_LABEL + ":");
        maxLatLabel.setToolTipText(Earthdata_Cloud_Controller.PROPERTY_MAXLAT_TOOLTIP);
        panel.add(maxLatLabel, gbc);

        gbc.gridx = 2;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        maxLatField = new JTextField(Earthdata_Cloud_Controller.getPreferenceMaxLat());
        maxLatField.setToolTipText(Earthdata_Cloud_Controller.PROPERTY_MAXLAT_TOOLTIP);
        panel.add(maxLatField, gbc);
        gbc.gridwidth = 1;


        gbc.gridy++;
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        JLabel minLonLabel = new JLabel(Earthdata_Cloud_Controller.PROPERTY_MINLON_LABEL + ":");
        minLonLabel.setToolTipText(Earthdata_Cloud_Controller.PROPERTY_MINLON_TOOLTIP);
        panel.add(minLonLabel, gbc);


        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets.left = 0;
        gbc.insets.right = 2;
        minLonField = new JTextField(Earthdata_Cloud_Controller.getPreferenceMinLon());
        minLonField.setToolTipText(Earthdata_Cloud_Controller.PROPERTY_MINLON_TOOLTIP);
        panel.add(minLonField, gbc);
        gbc.gridwidth = 1;

        gbc.gridx = 3;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets.left = 2;
        gbc.insets.right = 0;
        maxLonField = new JTextField(Earthdata_Cloud_Controller.getPreferenceMaxLon());
        maxLonField.setToolTipText(Earthdata_Cloud_Controller.PROPERTY_MAXLON_TOOLTIP);
        panel.add(maxLonField, gbc);
        gbc.gridwidth = 1;

        gbc.insets.left = 0;
        gbc.insets.right = 0;

        gbc.gridx = 5;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        JLabel maxLonLabel = new JLabel(":" + Earthdata_Cloud_Controller.PROPERTY_MAXLON_LABEL);
        maxLonLabel.setToolTipText(Earthdata_Cloud_Controller.PROPERTY_MAXLON_TOOLTIP);
        panel.add(maxLonLabel, gbc);

        gbc.gridy++;
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 1.0;
        JLabel minLatLabel = new JLabel(Earthdata_Cloud_Controller.PROPERTY_MINLAT_LABEL + ":");
        minLatLabel.setToolTipText(Earthdata_Cloud_Controller.PROPERTY_MINLAT_TOOLTIP);
        panel.add(minLatLabel, gbc);

        gbc.gridx = 2;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 1.0;
        minLatField = new JTextField(Earthdata_Cloud_Controller.getPreferenceMinLat());
        minLatField.setToolTipText(Earthdata_Cloud_Controller.PROPERTY_MINLAT_TOOLTIP);
        panel.add(minLatField, gbc);
        gbc.gridwidth = 1;

        minLatField.setMinimumSize(preferredTextFieldSize);
        minLatField.setPreferredSize(preferredTextFieldSize);
        maxLatField.setMinimumSize(preferredTextFieldSize);
        maxLatField.setPreferredSize(preferredTextFieldSize);
        minLonField.setMinimumSize(preferredTextFieldSize);
        minLonField.setPreferredSize(preferredTextFieldSize);
        maxLonField.setMinimumSize(preferredTextFieldSize);
        maxLonField.setPreferredSize(preferredTextFieldSize);

        panel.setMinimumSize(panel.getPreferredSize());
        panel.setPreferredSize(panel.getPreferredSize());

        return panel;

    }


    private JPanel createSpatialPanel() {
//        System.out.println("Creating createSpatialPanel");
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Spatial Filter"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.anchor = GridBagConstraints.CENTER;

        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth = 2;
        gbc.weightx = 0;

        panel.add(createClassicBoundingBoxPanel(), gbc);

        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridy += 1;
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        Insets insetsOrig = gbc.insets;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(2, 2, 0, 2);
        panel.add(new JSeparator(), gbc);
        gbc.insets = insetsOrig;

//        gbc.gridy++;
//        gbc.weighty = 1;
//        gbc.fill = GridBagConstraints.BOTH;
//        gbc.gridwidth = 2;
//        JLabel fill = new JLabel("");
//        panel.add(fill, gbc);
//        gbc.weighty = 0;
//        gbc.gridwidth = 1;

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JLabel coordinatesLabel = new JLabel("Coordinates:");
        coordinatesLabel.setToolTipText("Used to set fields north, south, west and east");
        coordinatesLabel.setMinimumSize(coordinatesLabel.getPreferredSize());
        coordinatesLabel.setPreferredSize(coordinatesLabel.getPreferredSize());
        panel.add(coordinatesLabel, gbc);


        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        coordinates = new JTextField("");
        coordinates.setToolTipText("Used to set fields north, south, west and east");
        panel.add(coordinates, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JLabel boxSizeLabel = new JLabel(Earthdata_Cloud_Controller.PROPERTY_BOX_SIZE_LABEL + ":");
        boxSizeLabel.setToolTipText(Earthdata_Cloud_Controller.PROPERTY_BOX_SIZE_TOOLTIP);
        panel.add(boxSizeLabel, gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        boxSize = new JTextField(Earthdata_Cloud_Controller.getPreferenceBoxSize());
        boxSize.setToolTipText(Earthdata_Cloud_Controller.PROPERTY_BOX_SIZE_TOOLTIP);
        panel.add(boxSize, gbc);

        createSpatialPanelHandlers();

        gbc.gridy += 1;
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        insetsOrig = gbc.insets;
        gbc.gridwidth = 2;
        panel.add(new JSeparator(), gbc);
        gbc.insets = insetsOrig;
        gbc.gridwidth = 1;

        JTextField tmpTextField = new JTextField("123456789012345678901234567890");
        Dimension regionEntryPreferredSize = tmpTextField.getPreferredSize();

        JLabel tmpLabel = new JLabel("123456789012345678901234567890");
        Dimension regionLabelPreferredSize = tmpLabel.getPreferredSize();


        if (Earthdata_Cloud_Controller.getPreferencePresetRegionsSelectorInclude()) {

            try {
                String REGIONS_FILE = "regions.txt";
                String TOOLTIP = "<html>Pre-Defined Locations/Regions<br>Sets north, south, west, east based on contents of ~/.seadas/auxdata/regions/regions.txt</html>";
                ArrayList<RegionsInfo> regionsInfos = RegionUtils.getAuxDataRegions(REGIONS_FILE, true);

                Object[] regionsInfosArray = regionsInfos.toArray();

                gbc.gridy++;
                gbc.gridx = 0;
                gbc.fill = GridBagConstraints.NONE;
                gbc.weightx = 0;
                JLabel regionLabel = new JLabel("Preset Regions:");
                regionLabel.setToolTipText(TOOLTIP);
                regionLabel.setMinimumSize(regionLabelPreferredSize);
                panel.add(regionLabel, gbc);

                gbc.gridx = 1;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.weightx = 1.0;
                regions = new JComboBox(regionsInfosArray);
                regions.setMaximumRowCount(20);
                regions.setToolTipText(TOOLTIP);

                panel.add(regions, gbc);
                regions.setMinimumSize(regionEntryPreferredSize);

                regions.addActionListener(evt -> {
                    if (!working) {
                        working = true;
                        RegionsInfo selectedRegionInfo = (RegionsInfo) regions.getSelectedItem();
//                    System.out.println("regions change" + selectedRegionInfo.getName());

                        if (!RegionsInfo.SPECIAL_ENTRY.equals(selectedRegionInfo.getNorth())) {
                            maxLatField.setText(selectedRegionInfo.getNorth());
                            minLatField.setText(selectedRegionInfo.getSouth());
                            minLonField.setText(selectedRegionInfo.getWest());
                            maxLonField.setText(selectedRegionInfo.getEast());
                            coordinates.setText(selectedRegionInfo.getCoordinates());
                        } else {
                            maxLatField.setText("");
                            minLatField.setText("");
                            minLonField.setText("");
                            maxLonField.setText("");
                            coordinates.setText("");
                        }


                        if (locations != null) {
                            locations.setSelectedIndex(0);
                        }

                        if (user_regions != null) {
                            user_regions.setSelectedIndex(0);
                        }

                        if (user_locations != null) {
                            user_locations.setSelectedIndex(0);
                        }

                        handleUpdateFromCoordinates();

                        panel.repaint();
                        panel.updateUI();
                        parentDialog.repaint();

                        working = false;
                    }
                });


            } catch (Exception e) {
            }
        }


        if (Earthdata_Cloud_Controller.getPreferencePresetLocationsSelectorInclude()) {

            try {
                String REGIONS_FILE = "locations.txt";
                String TOOLTIP = "<html>Pre-Defined Locations/Regions<br>Sets north, south, west, east based on contents of ~/.seadas/auxdata/regions/locations.txt</html>";
                ArrayList<RegionsInfo> regionsInfos = RegionUtils.getAuxDataRegions(REGIONS_FILE, true);

                Object[] regionsInfosArray = regionsInfos.toArray();

                gbc.gridy++;
                gbc.gridx = 0;
                gbc.fill = GridBagConstraints.NONE;
                gbc.weightx = 0;
                JLabel regionLabel = new JLabel("Preset Locations:");
                regionLabel.setToolTipText(TOOLTIP);
                regionLabel.setMinimumSize(regionLabelPreferredSize);
                panel.add(regionLabel, gbc);

                gbc.gridx = 1;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.weightx = 1.0;
                locations = new JComboBox(regionsInfosArray);
                locations.setMaximumRowCount(20);
                locations.setToolTipText(TOOLTIP);

                panel.add(locations, gbc);
                locations.setMinimumSize(regionEntryPreferredSize);

                locations.addActionListener(evt -> {
                    if (!working) {
                        working = true;
                        RegionsInfo selectedRegionInfo = (RegionsInfo) locations.getSelectedItem();
//                    System.out.println("regions change" + selectedRegionInfo.getName());

                        if (!RegionsInfo.SPECIAL_ENTRY.equals(selectedRegionInfo.getNorth())) {
                            maxLatField.setText(selectedRegionInfo.getNorth());
                            minLatField.setText(selectedRegionInfo.getSouth());
                            minLonField.setText(selectedRegionInfo.getWest());
                            maxLonField.setText(selectedRegionInfo.getEast());
                            coordinates.setText(selectedRegionInfo.getCoordinates());
                        } else {
                            maxLatField.setText("");
                            minLatField.setText("");
                            minLonField.setText("");
                            maxLonField.setText("");
                            coordinates.setText("");
                        }

                        if (regions != null) {
                            regions.setSelectedIndex(0);
                        }

                        if (user_regions != null) {
                            user_regions.setSelectedIndex(0);
                        }

                        if (user_locations != null) {
                            user_locations.setSelectedIndex(0);
                        }

                        handleUpdateFromCoordinates();

                        panel.repaint();
                        panel.updateUI();
                        parentDialog.repaint();

                        working = false;
                    }
                });


            } catch (Exception e) {
            }
        }


        if (Earthdata_Cloud_Controller.getPreferenceUserRegionSelectorInclude()) {

            try {
                String REGIONS_FILE = "user_regions.txt";
                String TOOLTIP = "<html>User-Defined Regions<br>Sets coordinates based on contents of ~/.seadas/auxdata/regions/user_regions.txt</html>";

                ArrayList<RegionsInfo> regionsInfos = RegionUtils.getAuxDataRegions(REGIONS_FILE, false);

                Object[] regionsInfosArray = regionsInfos.toArray();

                gbc.gridy++;
                gbc.gridx = 0;
                gbc.fill = GridBagConstraints.NONE;
                gbc.weightx = 0;
                JLabel regionLabel = new JLabel("User Regions:");
                regionLabel.setToolTipText(TOOLTIP);
                regionLabel.setMinimumSize(regionLabelPreferredSize);
                panel.add(regionLabel, gbc);

                gbc.gridx = 1;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.weightx = 1.0;
                user_regions = new JComboBox(regionsInfosArray);
                user_regions.setMaximumRowCount(20);
                user_regions.setToolTipText(TOOLTIP);

                panel.add(user_regions, gbc);
                user_regions.setMinimumSize(regionEntryPreferredSize);

                user_regions.addActionListener(evt -> {
                    if (!working) {
                        working = true;
                        RegionsInfo selectedRegionInfo = (RegionsInfo) user_regions.getSelectedItem();
//                        System.out.println("regions change" + selectedRegionInfo.getName());

                        if (!RegionsInfo.SPECIAL_ENTRY.equals(selectedRegionInfo.getNorth())) {
                            maxLatField.setText(selectedRegionInfo.getNorth());
                            minLatField.setText(selectedRegionInfo.getSouth());
                            minLonField.setText(selectedRegionInfo.getWest());
                            maxLonField.setText(selectedRegionInfo.getEast());
                            coordinates.setText(selectedRegionInfo.getCoordinates());
                        } else {
                            maxLatField.setText("");
                            minLatField.setText("");
                            minLonField.setText("");
                            maxLonField.setText("");
                            coordinates.setText("");
                        }

                        if (regions != null) {
                            regions.setSelectedIndex(0);
                        }

                        if (locations != null) {
                            locations.setSelectedIndex(0);
                        }

                        if (user_locations != null) {
                            user_locations.setSelectedIndex(0);
                        }

                        handleUpdateFromCoordinates();


                        panel.repaint();
                        panel.updateUI();
                        parentDialog.repaint();

                        working = false;
                    }
                });


            } catch (Exception e) {
            }
        }


        if (Earthdata_Cloud_Controller.getPreferenceUserLocationsSelectorInclude()) {

            try {
                String REGIONS_FILE = "user_locations.txt";
                String TOOLTIP = "<html>User-Defined Locations<br>Sets north, south, west, east based on contents of ~/.seadas/auxdata/regions/user_locations.txt</html>";

                ArrayList<RegionsInfo> regionsInfos = RegionUtils.getAuxDataRegions(REGIONS_FILE, false);

                Object[] regionsInfosArray = regionsInfos.toArray();

                gbc.gridy++;
                gbc.gridx = 0;
                gbc.fill = GridBagConstraints.NONE;
                gbc.weightx = 0;
                JLabel regionLabel = new JLabel("User Locations:");
                regionLabel.setToolTipText(TOOLTIP);
                regionLabel.setMinimumSize(regionLabelPreferredSize);
                panel.add(regionLabel, gbc);

                gbc.gridx = 1;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.weightx = 1.0;
                user_locations = new JComboBox(regionsInfosArray);
                user_locations.setMaximumRowCount(20);
                user_locations.setToolTipText(TOOLTIP);

                panel.add(user_locations, gbc);
                user_locations.setMinimumSize(regionEntryPreferredSize);

                user_locations.addActionListener(evt -> {
                    if (!working) {
                        working = true;
                        RegionsInfo selectedRegionInfo = (RegionsInfo) user_locations.getSelectedItem();
//                        System.out.println("regions change" + selectedRegionInfo.getName());

                        if (!RegionsInfo.SPECIAL_ENTRY.equals(selectedRegionInfo.getNorth())) {
                            maxLatField.setText(selectedRegionInfo.getNorth());
                            minLatField.setText(selectedRegionInfo.getSouth());
                            minLonField.setText(selectedRegionInfo.getWest());
                            maxLonField.setText(selectedRegionInfo.getEast());
                            coordinates.setText(selectedRegionInfo.getCoordinates());
                        } else {
                            maxLatField.setText("");
                            minLatField.setText("");
                            minLonField.setText("");
                            maxLonField.setText("");
                            coordinates.setText("");
                        }

                        if (regions != null) {
                            regions.setSelectedIndex(0);
                        }

                        if (locations != null) {
                            locations.setSelectedIndex(0);
                        }

                        if (user_regions != null) {
                            user_regions.setSelectedIndex(0);
                        }

                        handleUpdateFromCoordinates();

                        panel.repaint();
                        panel.updateUI();
                        parentDialog.repaint();

                        working = false;
                    }
                });

            } catch (Exception e) {
            }
        }



        if (Earthdata_Cloud_Controller.getPreferenceRegion() != null && Earthdata_Cloud_Controller.getPreferenceRegion().length() > 0) {
            if (regions != null) {
                for (int i = 0; i < regions.getItemCount(); i++) {
                    if (regions.getItemAt(i) != null) {
                        RegionsInfo regionsInfo = (RegionsInfo) regions.getItemAt(i);
                        if (Earthdata_Cloud_Controller.getPreferenceRegion().trim().equalsIgnoreCase(regionsInfo.getName())) {
                            regions.setSelectedIndex(i);
                        }
                    }
                }
            }

            if (user_regions != null) {
                for (int i = 0; i < user_regions.getItemCount(); i++) {
                    if (user_regions.getItemAt(i) != null) {
                        RegionsInfo regionsInfo = (RegionsInfo) user_regions.getItemAt(i);
                        if (Earthdata_Cloud_Controller.getPreferenceRegion().trim().equalsIgnoreCase(regionsInfo.getName())) {
                            user_regions.setSelectedIndex(i);
                        }
                    }
                }
            }

            if (user_locations != null) {
                for (int i = 0; i < user_locations.getItemCount(); i++) {
                    if (user_locations.getItemAt(i) != null) {
                        RegionsInfo regionsInfo = (RegionsInfo) user_locations.getItemAt(i);
                        if (Earthdata_Cloud_Controller.getPreferenceRegion().trim().equalsIgnoreCase(regionsInfo.getName())) {
                            user_locations.setSelectedIndex(i);
                        }
                    }
                }
            }
        }


        JTextField tmpLon = new JTextField("1234567890123");
        minLonField.setMinimumSize(tmpLon.getPreferredSize());


        panel.setMinimumSize(panel.getPreferredSize());
        panel.setPreferredSize(panel.getPreferredSize());

        return panel;
    }


    private void createSpatialPanelHandlers() {


        minLatField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
            }

            @Override
            public void focusLost(FocusEvent e) {
                setLatControlsHandler(minLatField);
            }
        });
        minLatField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                setLatControlsHandler(minLatField);
            }
        });


        maxLatField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
            }

            @Override
            public void focusLost(FocusEvent e) {
                setLatControlsHandler(maxLatField);
            }
        });
        maxLatField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                setLatControlsHandler(maxLatField);
            }
        });


        minLonField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
            }

            @Override
            public void focusLost(FocusEvent e) {
                setLonControlsHandler(minLonField);
            }
        });
        minLonField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                setLonControlsHandler(minLonField);
            }
        });


        maxLonField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
            }

            @Override
            public void focusLost(FocusEvent e) {
                setLonControlsHandler(maxLonField);
            }
        });
        maxLonField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                setLonControlsHandler(maxLonField);
            }
        });


        coordinates.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
            }

            @Override
            public void focusLost(FocusEvent e) {
                try {
                    if (!working) {
                        working = true;
                        handleUpdateFromCoordinates();
                    }
                } catch (Exception ex) {
                }

                if (regions != null) {
                    regions.setSelectedIndex(0);
                }

                if (locations != null) {
                    locations.setSelectedIndex(0);
                }

                if (user_regions != null) {
                    user_regions.setSelectedIndex(0);
                }

                if (user_locations != null) {
                    user_locations.setSelectedIndex(0);
                }

                working = false;
            }
        });
        coordinates.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                try {
                    if (!working) {
                        working = true;
                        handleUpdateFromCoordinates();
                    }
                } catch (Exception ex) {
                }

                if (regions != null) {
                    regions.setSelectedIndex(0);
                }

                if (locations != null) {
                    locations.setSelectedIndex(0);
                }

                if (user_regions != null) {
                    user_regions.setSelectedIndex(0);
                }

                if (user_locations != null) {
                    user_locations.setSelectedIndex(0);
                }

                working = false;
            }
        });


        boxSize.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
            }

            @Override
            public void focusLost(FocusEvent e) {
                try {
                    if (!working) {
                        working = true;
                        handleUpdateFromCoordinates();
                    }
                } catch (Exception ex) {
                }

                working = false;
            }
        });
        boxSize.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                try {
                    if (!working) {
                        working = true;
                        handleUpdateFromCoordinates();
                    }
                } catch (Exception ex) {
                }

                working = false;
            }
        });

    }


    private void setLatControlsHandler(Object sourceControl) {

        if (!working) {
            working = true;

            boolean minSet = false;
            boolean maxSet = false;
            if (minLatField != null && minLatField.getText() != null && minLatField.getText().trim().length() > 0) {
                minSet = true;
            }
            if (maxLatField != null && maxLatField.getText() != null && maxLatField.getText().trim().length() > 0) {
                maxSet = true;
            }

            if (sourceControl == minLatField || sourceControl == maxLatField) {
                coordinates.setText("");
                if (regions != null) {
                    regions.setSelectedIndex(0);
                }

                if (locations != null) {
                    locations.setSelectedIndex(0);
                }

                if (user_regions != null) {
                    user_regions.setSelectedIndex(0);
                }

                if (user_locations != null) {
                    user_locations.setSelectedIndex(0);
                }
            }

            boolean valid = true;
            double FAIL_DOUBLE = -9999.0;

            String minLat = RegionUtils.convertLatToDecimal(minLatField.getText());
            String maxLat = RegionUtils.convertLatToDecimal(maxLatField.getText());

            double minLatDouble = RegionUtils.convertStringToDouble(minLat, FAIL_DOUBLE);
            if (minSet) {
                if (!minLat.equals(minLatField.getText())) {
                    minLatField.setText(minLat);
                }
                if (minLatDouble == FAIL_DOUBLE || minLatDouble < -90 || minLatDouble > 90) {
                    valid = false;
                    JOptionPane.showMessageDialog(null, "WARNING!: 'South' is invalid");
                    minLatField.setText("");
                }
            }


            double maxLatDouble = RegionUtils.convertStringToDouble(maxLat, FAIL_DOUBLE);
            if (maxSet) {
                if (!maxLat.equals(maxLatField.getText())) {
                    maxLatField.setText(maxLat);
                }
                if (maxLatDouble == FAIL_DOUBLE || maxLatDouble < -90 || maxLatDouble > 90) {
                    valid = false;
                    JOptionPane.showMessageDialog(null, "WARNING!: 'North' is invalid");
                    maxLatField.setText("");
                }
            }

            if (valid && minSet && maxSet && minLatDouble > maxLatDouble) {
                if (sourceControl == minLatField) {
                    JOptionPane.showMessageDialog(null, "WARNING!: 'South' cannot be greater than 'North'");
                    minLatField.setText("");
                } else {
                    JOptionPane.showMessageDialog(null, "WARNING!: 'North' cannot be less than 'South'");
                    maxLatField.setText("");
                }
            }

            working = false;
        }
    }


    private void setLonControlsHandler(Object sourceControl) {

        if (!working) {
            working = true;

            boolean minSet = false;
            boolean maxSet = false;
            if (minLonField != null && minLonField.getText() != null && minLonField.getText().trim().length() > 0) {
                minSet = true;
            }
            if (maxLonField != null && maxLonField.getText() != null && maxLonField.getText().trim().length() > 0) {
                maxSet = true;
            }

            if (sourceControl == minLonField || sourceControl == maxLonField) {
                coordinates.setText("");
                if (regions != null) {
                    regions.setSelectedIndex(0);
                }

                if (locations != null) {
                    locations.setSelectedIndex(0);
                }

                if (user_regions != null) {
                    user_regions.setSelectedIndex(0);
                }

                if (user_locations != null) {
                    user_locations.setSelectedIndex(0);
                }
            }

            boolean valid = true;
            double FAIL_DOUBLE = -9999.0;

            String minLon = RegionUtils.convertLonToDecimal(minLonField.getText());
            String maxLon = RegionUtils.convertLonToDecimal(maxLonField.getText());


            double minLonDouble = RegionUtils.convertStringToDouble(minLon, FAIL_DOUBLE);
            if (minSet) {
                if (!minLon.equals(minLonField.getText())) {
                    minLonField.setText(minLon);
                }
                if (minLonDouble == FAIL_DOUBLE || minLonDouble < -180 || minLonDouble > 180) {
                    valid = false;
                    JOptionPane.showMessageDialog(null, "WARNING!: 'West' is invalid");
                    minLonField.setText("");
                }
            }


            double maxLonDouble = RegionUtils.convertStringToDouble(maxLon, FAIL_DOUBLE);
            if (maxSet) {
                if (!maxLon.equals(maxLonField.getText())) {
                    maxLonField.setText(maxLon);
                }
                if (maxLonDouble == FAIL_DOUBLE || maxLonDouble < -180 || maxLonDouble > 180) {
                    valid = false;
                    JOptionPane.showMessageDialog(null, "WARNING!: 'East' is invalid");
                    maxLonField.setText("");
                }
            }

            if (valid && minSet && maxSet) {
            }

            working = false;
        }
    }


    private void handleUpdateFromCoordinates() {
        boolean valid = false;

        String coordinatesValue = coordinates.getText();
        if (coordinatesValue == null || coordinatesValue.trim().length() == 0) {
            return;
        }

        coordinatesValue = coordinatesValue.replace(",", " ");

        String[] coordinatesSplitArray = coordinatesValue.split("\\s+");


        if (coordinatesSplitArray.length == 2) {
            String lat = RegionUtils.convertLatToDecimal(coordinatesSplitArray[0]);
            String lon = RegionUtils.convertLonToDecimal(coordinatesSplitArray[1]);

            if (RegionUtils.validateCoordinates(lat, lon)) {
                double FAIL_DOUBLE = -9999.0;
                double latDouble = RegionUtils.convertStringToDouble(lat, FAIL_DOUBLE);
                double lonDouble = RegionUtils.convertStringToDouble(lon, FAIL_DOUBLE);

                String boxSizeValue = boxSize.getText();

                double boxHeight = 0;
                double boxWidth = 0;
                boolean boxValid = true;

                String warnMsg = "";

                String[] boxSizeSplitOnXArray = boxSizeValue.split("x");
                if (boxSizeSplitOnXArray.length == 1) {
                    double boxValueDouble = RegionUtils.convertStringToDouble(boxSizeSplitOnXArray[0], FAIL_DOUBLE);
                    if (boxValueDouble != FAIL_DOUBLE && boxValueDouble >= 0) {
                        boxHeight = boxValueDouble;
                        boxWidth = boxValueDouble;
                    } else {
                        boxValid = false;
                        warnMsg = "WARNING!: Box Size is invalid";
                    }
                } else if (boxSizeSplitOnXArray.length == 2) {
                    double boxValueLonDouble = RegionUtils.convertStringToDouble(boxSizeSplitOnXArray[0], FAIL_DOUBLE);
                    if (boxValueLonDouble != FAIL_DOUBLE && boxValueLonDouble >= 0) {
                        double boxValueLatDouble = RegionUtils.convertStringToDouble(boxSizeSplitOnXArray[1], FAIL_DOUBLE);
                        if (boxValueLatDouble != FAIL_DOUBLE && boxValueLatDouble >= 0) {
                            boxHeight = boxValueLatDouble;
                            boxWidth = boxValueLonDouble;
                        } else {
                            warnMsg = "WARNING!: Box Size (height) is invalid";
                            boxValid = false;
                        }
                    } else {
                        warnMsg = "WARNING!: Box Size (width) is invalid";
                        boxValid = false;
                    }
                } else {
                    boxValid = false;
                    warnMsg = "WARNING!: Box Size is invalid";
                }

                if (!boxValid) {
                    JOptionPane.showMessageDialog(null, warnMsg);
                }


                if (latDouble != FAIL_DOUBLE && lonDouble != FAIL_DOUBLE && boxValid) {
                    valid = true;


                    if (boxHeight >= 0 && boxWidth >= 0) {
                        double EARTH_CIRCUMFERENCE = 40075.017;
                        double degreesPerKmAlongLat = 360 / EARTH_CIRCUMFERENCE;

                        boolean unitsKm = false;    // todo investigate this

                        double minLat;
                        double maxLat;
                        double minLon;
                        double maxLon;
                        if (unitsKm) {
                            double EARTH_RADIUS_EQUATOR = 6378.1370;
                            double midLatRadiansAbs = Math.abs(latDouble) * Math.PI / 180.0;
                            double cosineMidLat = Math.cos(midLatRadiansAbs);
                            double circumferenceAtMidLat = 2.0 * Math.PI * EARTH_RADIUS_EQUATOR * Math.cos(midLatRadiansAbs);
                            double circumferenceAtEquator = 2.0 * Math.PI * EARTH_RADIUS_EQUATOR;
//                            System.out.println("cosineMidLat=" + cosineMidLat);
//                            System.out.println("circumferenceAtMidLat=" + circumferenceAtMidLat);
//                            System.out.println("circumferenceAtEquator=" + circumferenceAtEquator);

                            minLat = latDouble - 0.5 * boxHeight * degreesPerKmAlongLat;
                            maxLat = latDouble + 0.5 * boxHeight * degreesPerKmAlongLat;

                            if (latDouble >= -89 && latDouble <= 89) {
                                double degreesPerKmAlongLon = 360.0 / circumferenceAtMidLat;
                                minLon = lonDouble - 0.5 * boxWidth * degreesPerKmAlongLon;
                                maxLon = lonDouble + 0.5 * boxWidth * degreesPerKmAlongLon;
                            } else {
                                minLon = -180.0;
                                maxLon = 180.0;
                            }
                        } else {
                            minLat = latDouble - 0.5 * boxHeight;
                            maxLat = latDouble + 0.5 * boxHeight;
                            minLon = lonDouble - 0.5 * boxWidth;
                            maxLon = lonDouble + 0.5 * boxWidth;
                        }


                        if (maxLat > 90) {
                            maxLat = 90;
                        }
                        if (minLat < -90) {
                            minLat = -90;
                        }

                        if (maxLon > 180) {
                            maxLon = -180 + (maxLon - 180);
                        }
                        if (minLon < -180) {
                            minLon = 180 + (minLon + 180);
                        }


                        DecimalFormat df = new DecimalFormat("###.####");
                        String minLatStr = df.format(minLat);
                        String maxLatStr = df.format(maxLat);
                        String minLonStr = df.format(minLon);
                        String maxLonStr = df.format(maxLon);

                        minLatField.setText(minLatStr);
                        maxLatField.setText(maxLatStr);
                        minLonField.setText(minLonStr);
                        maxLonField.setText(maxLonStr);
                    } else {
                        maxLatField.setText(lat);
                        minLatField.setText(lat);
                        minLonField.setText(lon);
                        maxLonField.setText(lon);
                    }
                } else {
                    if (latDouble == FAIL_DOUBLE) {
                        JOptionPane.showMessageDialog(null, "WARNING!: Coordinates has invalid 'latitude'");
                    } else if (lonDouble == FAIL_DOUBLE) {
                        JOptionPane.showMessageDialog(null, "WARNING!: Coordinates has invalid 'longitude'");
                    } else if (!boxValid) {
                        JOptionPane.showMessageDialog(null, "WARNING!: Box size error - must be numeric and >= 0");
                    }
                }
            } else {
                JOptionPane.showMessageDialog(null, "WARNING!: Coordinates not valid");
            }
        } else {
            JOptionPane.showMessageDialog(null, "WARNING!: Coordinates not valid");
        }

        if (!valid) {
            minLatField.setText("");
            maxLatField.setText("");
            minLonField.setText("");
            maxLonField.setText("");
        }

    }

    private JDatePickerImpl createDatePicker() {
        UtilDateModel model = new UtilDateModel();
        Properties p = new Properties();
        p.put("text.today", "Today");
        p.put("text.month", "Month");
        p.put("text.year", "Year");
        return new JDatePickerImpl(new JDatePanelImpl(model, p), new DateComponentFormatter());
    }

    private void fetchGranules(com.bc.ceres.core.ProgressMonitor pm) {
        fileLinkMap.clear();
        tableModel.setRowCount(0);
        allGranules.clear();

        totalPages = 1;
        currentPage = 1;

        int workDone = 0;
        int workDoneThisIncrement = 0;
        int totalWork = 100;

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            tableModel.removeRow(i);
            allGranules.remove(i);
        }

        totalFetched = 0;
        updateResultsTable(currentPage);

        resultsTable.revalidate();
        resultsTable.repaint();
        SwingUtilities.invokeLater(() -> {
            resultsTable.revalidate();
            resultsTable.repaint();
        });


        String productName = (String) productDropdown.getSelectedItem();
        String shortName = productNameTooltips.getOrDefault(productName, productName);
        int maxApiResults = (Integer) maxApiResultsSpinner.getValue();
        double workInAnIncrement = (int) Math.floor(maxApiResults / totalWork);
//        System.out.println("workInAnIncrement=" + workInAnIncrement);

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

        minLat = RegionUtils.convertLatToDecimal(minLat);
        maxLat = RegionUtils.convertLatToDecimal(maxLat);
        minLon = RegionUtils.convertLonToDecimal(minLon);
        maxLon = RegionUtils.convertLonToDecimal(maxLon);



        boolean hasSpatial = !minLat.isEmpty() && !maxLat.isEmpty() && !minLon.isEmpty() && !maxLon.isEmpty();

        String dayNightFlag = null;
        if (dayButton.isSelected()) {
            dayNightFlag = "Day";
        } else if (nightButton.isSelected()) {
            dayNightFlag = "Night";
        }

        int pageSize = 2000;
        int page = 1;
        totalFetched = 0;


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

                BufferedReader bufferedReader = null;
                if (conn != null) {
                    InputStream inputStream = conn.getInputStream();
                    if (inputStream != null) {
                        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                        if (inputStreamReader != null) {
                            bufferedReader = new BufferedReader(inputStreamReader);
                        }
                    }
                }

                if (bufferedReader != null) {
                    JSONTokener tokener = new JSONTokener(bufferedReader);

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

                        if (pm != null) {
                            if (workDone < (totalWork - 2)) {
                                if (workDoneThisIncrement >= workInAnIncrement) {
                                    if (pm.isCanceled()) {
                                        return;
                                    }
                                    pm.worked(1);

                                    workDone++;
                                    workDoneThisIncrement = 0;

                                } else {
                                    workDoneThisIncrement++;
                                }
                            }
                        }

                        if (totalFetched >= maxApiResults) break;
                    }

                    if (entries.length() < pageSize) break;
                    page++;
                }
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

            totalPages = (int) Math.ceil((double) allGranules.size() / getResultsPerPage());
            currentPage = 1;
            updateResultsTable(currentPage);

        } catch (Exception e) {
            e.printStackTrace();
        }
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

        if (totalFetched > 0) {
            if (pageLabel != null) {
                pageLabel.setText("Page " + page + " of " + totalPages);
            }
        } else {
            if (pageLabel != null) {
                pageLabel.setText("");
            }
        }

        if (fetchedLabel != null) {
            fetchedLabel.setText("Files found: " + totalFetched + "     ");
        }
    }

    private int getResultsPerPage() {
        return (Integer) resultsPerPageSpinner.getValue();
    }

    private JPanel createDayNightPanel() {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder("Day/Night"));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JRadioButton dayButton = new JRadioButton("Day", Earthdata_Cloud_Controller.getPreferenceIsDay());
        JRadioButton nightButton = new JRadioButton("Night", Earthdata_Cloud_Controller.getPreferenceIsNight());
        JRadioButton bothButton = new JRadioButton("Both", Earthdata_Cloud_Controller.getPreferenceIsDayNightBoth());

        ButtonGroup group = new ButtonGroup();
        group.add(dayButton);
        group.add(nightButton);
        group.add(bothButton);

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

        JLabel tmp = new JLabel("1234567890123");
        Dimension tmpDimension = new Dimension(tmp.getPreferredSize().width, panel.getPreferredSize().height);
        panel.setMinimumSize(tmpDimension);
        panel.setPreferredSize(tmpDimension);

        this.dayButton = dayButton;
        this.nightButton = nightButton;
        this.bothButton = bothButton;
        return panel;
    }

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
}
