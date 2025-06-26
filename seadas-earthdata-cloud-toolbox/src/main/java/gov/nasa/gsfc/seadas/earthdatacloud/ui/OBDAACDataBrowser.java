package gov.nasa.gsfc.seadas.earthdatacloud.ui;

import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import gov.nasa.gsfc.seadas.earthdatacloud.auth.WebPageFetcherWithJWT;
import gov.nasa.gsfc.seadas.earthdatacloud.preferences.Earthdata_Cloud_Controller;
import org.esa.snap.rcp.SnapApp;
import gov.nasa.gsfc.seadas.earthdatacloud.util.*;
import org.esa.snap.core.util.SystemUtils;
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
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OBDAACDataBrowser extends JPanel {
    JComboBox<String> satelliteDropdown, levelDropdown, productDropdown;
    JDatePickerImpl startDatePicker, endDatePicker;
    JTextField minLatField, maxLatField, minLonField, maxLonField, coordinates, boxSize;
    JComboBox regions;
    JComboBox regions2;
    private boolean working = false;
    private JTable resultsTable;
    private DefaultTableModel tableModel;
    private final Map<String, String> productNameTooltips = new HashMap<>();

    private final Map<String, JSONObject> metadataMap = new HashMap<>();
    JRadioButton dayButton, nightButton, bothButton;
    ButtonGroup dayNightGroup;
    private final Map<String, String> fileLinkMap = new HashMap<>();

    int currentPage = 1;
    private int totalFetched = 1;
    int totalPages = 1;
    private List<String[]> pagedResults = new ArrayList<>();
    JLabel pageInfoLabel;
    JButton prevPageButton, nextPageButton;
    private static final int RESULTS_PER_PAGE = 25;
    private JSpinner maxApiResultsSpinner;
    private JSpinner resultsPerPageSpinner;

    JLabel pageLabel;
    JLabel fetchedLabel;
    private List<String[]> allGranules = new ArrayList<>();
    private JWindow imagePreviewWindow;
    private JLabel imageLabel;
    private String[] earthdataCredentials;
    JLabel dateRangeHintLabel = new JLabel();  // Declare as a class member
    JPanel temporalPanel;
    JLabel dateRangeLabel; // add this as a field so we can update it later

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
    );

    private OBDAACDataBrowserPanels panels;

    public OBDAACDataBrowser(JDialog parentDialog) {
        this.parentDialog = parentDialog;
        setLayout(new GridBagLayout());
        setSize(850, 600);
        setLayout(new GridBagLayout());
        loadMetadata();
        imagePreviewHelper = new ImagePreviewHelper();
        loadMissionDateRangesFromFile();
        panels = new OBDAACDataBrowserPanels(this);
        initComponents();
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

    private void loadMissionDateRangesFromFile() {
        missionDateRanges = new HashMap<>();

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

        System.out.println("Loading mission_date_ranges.json from classpath");
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("json-files/mission_date_ranges.json")) {
            if (input != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
                loadDateRangesFromReader(reader);
            } else {
                System.err.println("Resource not found: json-files/mission_date_ranges.json");
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
                ((JComponent) startDatePicker.getComponent(0)).setToolTipText(tooltip);
                ((JComponent) startDatePicker.getComponent(1)).setToolTipText(tooltip);
                ((JComponent) endDatePicker.getComponent(0)).setToolTipText(tooltip);
                ((JComponent) endDatePicker.getComponent(1)).setToolTipText(tooltip);

            } else {
                temporalPanel.setToolTipText("Valid date range not available.");
            }
        });



        gbc.gridy++;
        gbc.gridx = 0;
        add(panels.createFilterPanel(), gbc);



        JPanel paginationPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        paginationPanel.add(new JLabel("Max Results:"));

        int maxResultsPref = Earthdata_Cloud_Controller.getPreferenceFetchMaxResults();
        int maxResultsMin = Earthdata_Cloud_Controller.PROPERTY_FETCH_MAX_RESULTS_MIN_VALUE;
        int maxResultsMax = Earthdata_Cloud_Controller.PROPERTY_FETCH_MAX_RESULTS_MAX_VALUE;
        maxApiResultsSpinner = new JSpinner(new SpinnerNumberModel(maxResultsPref, maxResultsMin, maxResultsMax, 1));
        maxApiResultsSpinner.setPreferredSize(new Dimension(80, 25));
        maxApiResultsSpinner.setToolTipText(Earthdata_Cloud_Controller.PROPERTY_FETCH_MAX_RESULTS_TOOLTIP);


        paginationPanel.add(maxApiResultsSpinner);
        paginationPanel.add(Box.createHorizontalStrut(20));
        paginationPanel.add(new JLabel("Results Per Page:"));

        int resultsPerPagePref = Earthdata_Cloud_Controller.getPreferenceFetchResultsPerPage();
        int resultsPerPageMin = Earthdata_Cloud_Controller.PROPERTY_FETCH_RESULTS_PER_PAGE_MIN_VALUE;
        int resultsPerPageMax = Earthdata_Cloud_Controller.PROPERTY_FETCH_RESULTS_PER_PAGE_MAX_VALUE;
        resultsPerPageSpinner = new JSpinner(new SpinnerNumberModel(resultsPerPagePref, resultsPerPageMin, resultsPerPageMax, 1));
        resultsPerPageSpinner.setPreferredSize(new Dimension(80, 25));
        resultsPerPageSpinner.setToolTipText(Earthdata_Cloud_Controller.PROPERTY_FETCH_RESULTS_PER_PAGE_TOOLTIP);

        paginationPanel.add(resultsPerPageSpinner);
        paginationPanel.add(Box.createHorizontalStrut(60));





        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(e -> runFetchWrapper());
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



        gbc.gridy++;
        gbc.gridx = 0;

        add(panels.createPaginationButtonPanel(paginationPanel, buttonPanel), gbc);

        gbc.gridy++;
        gbc.gridx = 0;
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


        imagePreviewHelper.attachToTable(resultsTable, fileLinkMap, parentDialog);
        resultsTable.setDefaultRenderer(Object.class, new TableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                JLabel label = new JLabel("<html><a href='#'>" + value + "</a></html>");
                return label;
            }
        });


        resultsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = resultsTable.rowAtPoint(e.getPoint());
                int col = resultsTable.columnAtPoint(e.getPoint());

                if (Earthdata_Cloud_Controller.getPreferenceImageLinkInclude() && col == 0) {
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



        resultsTable.setFillsViewportHeight(true);
        JScrollPane scrollPane = new JScrollPane(resultsTable);
        scrollPane.setPreferredSize(new Dimension(700, 500));


        resultsContainer = new JPanel(new BorderLayout());
        resultsContainer.setVisible(false); // 👈 initially hidden
        resultsContainer.setPreferredSize(new Dimension(700, 600));  // Adjust height as needed

        resultsContainer.removeAll();  // clean up old content if any
        resultsContainer.add(scrollPane, BorderLayout.CENTER);
        resultsContainer.add(panels.createPaginationPanel(), BorderLayout.SOUTH);
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

        return panel;
    }





    void downloadSelectedFiles() {
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



        String parentDownloadDirStr = Earthdata_Cloud_Controller.getPreferenceDownloadParentDir();

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

            String downloadDirStr = Earthdata_Cloud_Controller.getPreferenceDownloadDir();

            int currIndex = 1;
            String downloadDirStrNoSuffix = "results";
            boolean retainSuffix = false;

            if (downloadDirStr != null && downloadDirStr.trim().length() > 0) {
                downloadDirStrNoSuffix = downloadDirStr;
                String[] downloadDirStrSplitArray = downloadDirStr.split("-");
                if (downloadDirStrSplitArray.length == 2) {
                    String suffix = downloadDirStrSplitArray[1];
                    int currIndexTmp = RegionUtils.convertStringToInt(suffix, -999);
                    if (currIndexTmp != -999) {
                        downloadDirStrNoSuffix = downloadDirStrSplitArray[0];
                        currIndex = currIndexTmp;
                        if (currIndex == 1) {
                            retainSuffix = true;
                        }
                    }
                }
            }

            String downloadDirStrIndexed;
            File file2 = null;
            while (file2 == null && currIndex < 1000) {
                if (currIndex == 1 && !retainSuffix) {
                    downloadDirStrIndexed = downloadDirStrNoSuffix;
                } else {
                    downloadDirStrIndexed = downloadDirStrNoSuffix + "-" + currIndex;
                }

                file2 = new File(parentDownloadDirFile, downloadDirStrIndexed);
                if (!file2.exists()) {
                    break;
                }
                file2 = null;
                currIndex++;
            }

            if (file2 != null) {
                fileChooser.setSelectedFile(file2);
            }
        }




        if (fileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;


        File selectedDir = fileChooser.getSelectedFile();


        String selectedParent = selectedDir.getParentFile().getAbsolutePath();
        if (selectedParent != null) {
            Earthdata_Cloud_Controller.setPreferenceDownloadParentDir(selectedParent);
        }

        String selectedDownloadDir = selectedDir.getName();
        if (selectedDownloadDir != null) {
            Earthdata_Cloud_Controller.setPreferenceDownloadDir(selectedDownloadDir);
        }


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
            SwingUtilities.invokeLater(() -> hideProgressDialog());

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
                resultsTable.getColumnModel().getColumn(1).setCellEditor(null);
                break;
            }
        }
    }

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
        System.out.println("workInAnIncrement=" + workInAnIncrement);

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
        JPanel downloadPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        downloadPanel.add(downloadButton);

        panel.add(fetchedPanel, BorderLayout.WEST);
        panel.add(navPanel, BorderLayout.CENTER);
        panel.add(downloadPanel, BorderLayout.EAST);

        return panel;
    }


    void updateResultsTable(int page) {
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

    JDatePickerImpl createDatePicker() {
        UtilDateModel model = new UtilDateModel();
        Properties p = new Properties();
        p.put("text.today", "Today");
        p.put("text.month", "Month");
        p.put("text.year", "Year");
        return new JDatePickerImpl(new JDatePanelImpl(model, p), new DateComponentFormatter());
    }
}
