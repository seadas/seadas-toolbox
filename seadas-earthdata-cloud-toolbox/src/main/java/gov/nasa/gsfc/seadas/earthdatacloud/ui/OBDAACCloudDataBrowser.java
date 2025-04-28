package gov.nasa.gsfc.seadas.earthdatacloud.ui;

import gov.nasa.gsfc.seadas.earthdatacloud.data.GranuleFetcher;
import gov.nasa.gsfc.seadas.earthdatacloud.util.Constants;
import gov.nasa.gsfc.seadas.earthdatacloud.util.FileDownloader;
import gov.nasa.gsfc.seadas.earthdatacloud.data.MetadataLoader;
import gov.nasa.gsfc.seadas.earthdatacloud.util.ImagePreviewHandler;
import org.jdatepicker.impl.JDatePickerImpl;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.net.URI;
import java.util.*;
import java.util.List;

import static gov.nasa.gsfc.seadas.earthdatacloud.ui.UIComponentsFactory.createFilterPanel;

public class OBDAACCloudDataBrowser extends JFrame {
    private JComboBox<String> satelliteDropdown;
    private JComboBox<String> levelDropdown;
    private JComboBox<String> productDropdown;
    private JDatePickerImpl startDatePicker, endDatePicker;
    private JTextField minLatField, maxLatField, minLonField, maxLonField;
    private JSpinner maxResultsSpinner;
    private JTable resultsTable;
    private DefaultTableModel tableModel;
    private JWindow imagePreviewWindow;
    private JLabel imageLabel;
    private Map<String, String> fileLinkMap;

    private MetadataLoader metadataLoader;
    private GranuleFetcher granuleFetcher;
    private FileDownloader fileDownloader;
    private ImagePreviewHandler imagePreviewHelper;

    private Map<String, JSONObject> metadataMap = new HashMap<>();
    private final Map<String, String> productNameTooltips = new HashMap<>();
    private JLabel dateRangeHintLabel = new JLabel();  // Declare as a class member
    private JPanel temporalPanel;
    private JLabel dateRangeLabel; // a

    Map<String, String[]> missionDateRanges = Map.of(
            "SeaHawk/HawkEye", new String[]{"2018-12-01", "2023-12-31"},
            "MODISA", new String[]{"2002-07-04", "2024-12-31"},
            "VIIRSN", new String[]{"2011-10-28", "2024-12-31"}
            // Add more as needed
    );

    public OBDAACCloudDataBrowser() {
        setTitle("CLOUD Data Browser – Harmony Powered");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 650);
        setLayout(new GridBagLayout());

        // Initialize utilities
        metadataLoader = new MetadataLoader();
        granuleFetcher = new GranuleFetcher();
        fileDownloader = new FileDownloader();
        imagePreviewHelper = new ImagePreviewHandler();

        metadataMap = MetadataLoader.loadAllMetadata(Constants.JSON_DIR);
        missionDateRanges = MetadataLoader.loadMissionDateRangesFromFile();
        initComponents();
    }

//    private void initializeGUI(){
//
//        GridBagConstraints gbc = new GridBagConstraints();
//        gbc.gridx = 0;
//        gbc.gridy = 0;
//        gbc.gridwidth = 2;
//        gbc.fill = GridBagConstraints.HORIZONTAL;
//        gbc.anchor = GridBagConstraints.WEST;
//        gbc.insets = new Insets(10, 10, 5, 10);
//        add(UIComponentsFactory.createHeaderPanel(), gbc);
//
//        gbc.gridy++;
//        gbc.insets = new Insets(5, 10, 5, 10);
//        gbc.fill = GridBagConstraints.HORIZONTAL;
//        gbc.gridx = 0;
//        gbc.gridy = 0;
//
//        List<String> sortedSatellites = new ArrayList<>(metadataMap.keySet());
//        Collections.sort(sortedSatellites);
//        satelliteDropdown = new JComboBox<>(sortedSatellites.toArray(new String[0]));
//
//        levelDropdown = new JComboBox<>();
//        productDropdown = new JComboBox<>();
//
//        temporalPanel = UIComponentsFactory.createTemporalPanel();
//
//        satelliteDropdown.addActionListener(e -> {
//            updateLevelsAndProducts();
//            String satelliteKey = (String) satelliteDropdown.getSelectedItem();
//            String minDate = missionDateRanges.containsKey(satelliteKey) ? missionDateRanges.get(satelliteKey)[0] : "N/A";
//            String maxDate = missionDateRanges.containsKey(satelliteKey) ? missionDateRanges.get(satelliteKey)[1] : "N/A";
//            updateDateRangeLabel(minDate, maxDate);
//            if (satelliteKey != null && missionDateRanges.containsKey(satelliteKey)) {
//                String[] range = missionDateRanges.get(satelliteKey);
//                String tooltip = "Valid date range: " + range[0] + " to " + range[1];
//                temporalPanel.setToolTipText(tooltip);
//                // Set tooltip on individual components
//                ((JComponent) startDatePicker.getComponent(0)).setToolTipText(tooltip);
//                ((JComponent) startDatePicker.getComponent(1)).setToolTipText(tooltip);
//                ((JComponent) endDatePicker.getComponent(0)).setToolTipText(tooltip);
//                ((JComponent) endDatePicker.getComponent(1)).setToolTipText(tooltip);
//
//            } else {
//                temporalPanel.setToolTipText("Valid date range not available.");
//            }
//        });
//
//        gbc.gridx = 0; gbc.gridy = 0;
//        add(new JLabel("Satellite/Instrument:"), gbc);
//        gbc.gridx = 1; add(satelliteDropdown, gbc);
//
//        gbc.gridx = 0; gbc.gridy++;
//        add(new JLabel("Data Level:"), gbc);
//        gbc.gridx = 1; add(levelDropdown, gbc);
//
//        gbc.gridx = 0; gbc.gridy++;
//        add(new JLabel("Product Name:"), gbc);
//        gbc.gridx = 1; add(productDropdown, gbc);
//
//        // Filters in grouped panel
//        gbc.gridy++;
//        gbc.gridx = 0;
//        gbc.gridwidth = 2;
//        add(createFilterPanel(), gbc);
//
//        // Add panel for max API results and results per page
//        gbc.gridy++;
//        gbc.gridx = 0;
//        gbc.gridwidth = 2;
//        JPanel paginationPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
//        paginationPanel.add(new JLabel("Max API Results:"));
//        maxApiResultsSpinner = new JSpinner(new SpinnerNumberModel(100, 1, 10000, 1));
//        maxApiResultsSpinner.setPreferredSize(new Dimension(80, 25));
//        paginationPanel.add(maxApiResultsSpinner);
//        paginationPanel.add(Box.createHorizontalStrut(20));
//        paginationPanel.add(new JLabel("Results Per Page:"));
//        resultsPerPageSpinner = new JSpinner(new SpinnerNumberModel(25, 1, 1000, 1));
//        resultsPerPageSpinner.setPreferredSize(new Dimension(80, 25));
//        paginationPanel.add(resultsPerPageSpinner);
//        add(paginationPanel, gbc);
//
//        gbc.gridy++;
//        gbc.gridx = 0;
//        JButton searchButton = new JButton("Search");
//        searchButton.addActionListener(e -> GranuleFetcher.fetchGranules());
//        JButton cancelButton = new JButton("Cancel");
//        cancelButton.addActionListener(e -> System.exit(0));
//        JPanel buttonPanel = new JPanel();
//        buttonPanel.add(searchButton);
//        buttonPanel.add(cancelButton);
//        gbc.gridwidth = 2;
//        add(buttonPanel, gbc);
//
//        gbc.gridy++;
//        gbc.gridx = 0;
//        gbc.gridwidth = 2;
//        String[] columnNames = {"File Name", "Download File"};
//        tableModel = new DefaultTableModel(columnNames, 0) {
//            @Override
//            public Class<?> getColumnClass(int columnIndex) {
//                return (columnIndex == 1) ? Boolean.class : String.class;
//            }
//
//            @Override
//            public boolean isCellEditable(int row, int column) {
//                return column == 1; // Only checkbox editable
//            }
//        };
//
//        resultsTable = new JTable(tableModel);
//        resultsTable.setDefaultRenderer(Object.class, new TableCellRenderer() {
//            @Override
//            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
//                                                           boolean hasFocus, int row, int column) {
//                JLabel label = new JLabel("<html><a href='#'>" + value + "</a></html>");
//                return label;
//            }
//        });
//
//        resultsTable.addMouseMotionListener(new MouseMotionAdapter() {
//            @Override
//            public void mouseMoved(MouseEvent e) {
//                int row = resultsTable.rowAtPoint(e.getPoint());
//                int col = resultsTable.columnAtPoint(e.getPoint());
//
//                // Only show preview if hovering over the File Name column (e.g., column 0)
//                if (col == 0 && row >= 0) {
//                    String fileName = (String) tableModel.getValueAt(row, 0);
//                    ImagePreviewHandler.showImagePreview(fileName, e.getLocationOnScreen());
//                } else {
//                    ImagePreviewHandler.hideImagePreview();
//                }
//            }
//        });
//
//        resultsTable.addMouseListener(new MouseAdapter() {
//            @Override
//            public void mouseClicked(MouseEvent e) {
//                int row = resultsTable.rowAtPoint(e.getPoint());
//                int col = resultsTable.columnAtPoint(e.getPoint());
//
//                // Only act if the clicked column is the File Name column (e.g., 0)
//                if (col == 0) {
//                    String fileName = (String) tableModel.getValueAt(row, 0);
//                    String browseUrl = BrowseImagePreview.getFullImageUrl(fileName);
//                    if (browseUrl != null) {
//                        try {
//                            Desktop.getDesktop().browse(new URI(browseUrl));
//                        } catch (Exception ex) {
//                            ex.printStackTrace();
//                        }
//                    }
//                }
//            }
//        });
//
////        resultsTable.addMouseListener(new MouseAdapter() {
////            public void mouseClicked(MouseEvent e) {
////                int row = resultsTable.rowAtPoint(e.getPoint());
////                if (row >= 0) {
////                    String fileName = (String) tableModel.getValueAt(row, 0);
////                    String imageUrl = getPreviewUrl(fileName);
////                    if (imageUrl != null) {
////                        showImageInDialog(imageUrl);
////                    }
////                }
////            }
////        });
//
//        resultsTable.addMouseListener(new MouseAdapter() {
//            public void mouseExited(MouseEvent e) {
//                hideImagePreview();
//            }
//        });
//
//        resultsTable.setFillsViewportHeight(true);
//        JScrollPane scrollPane = new JScrollPane(resultsTable);
//        scrollPane.setPreferredSize(new Dimension(800, 300));
//
//
//        // Results container that holds the table and pagination
//        resultsContainer = new JPanel(new BorderLayout());
//        resultsContainer.setVisible(false); // 👈 initially hidden
//        resultsContainer.setPreferredSize(new Dimension(750, 400));  // Adjust height as needed
//
//        resultsContainer.removeAll();  // clean up old content if any
//        resultsContainer.add(scrollPane, BorderLayout.CENTER);
//        resultsContainer.add(createPaginationPanel(), BorderLayout.SOUTH);
//        resultsContainer.setVisible(false); // 👈 initially hidden
//
//// Add the container to the frame layout
//        gbc.gridx = 0;
//        gbc.gridy++;
//        gbc.gridwidth = 3;
//        gbc.weightx = 1.0;
//        gbc.weighty = 1.0;
//        gbc.fill = GridBagConstraints.BOTH;
//        add(resultsContainer, gbc);
//
//
//        if (satelliteDropdown.getItemCount() > 0) {
//            satelliteDropdown.setSelectedIndex(0);
//            updateLevelsAndProducts();
//        }
//    }

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
    private void initComponents() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;

        satelliteDropdown = UIComponentsFactory.createDropdown();
        levelDropdown = UIComponentsFactory.createDropdown();
        productDropdown = UIComponentsFactory.createDropdown();
        startDatePicker = UIComponentsFactory.createDatePicker();
        endDatePicker = UIComponentsFactory.createDatePicker();

        minLatField = UIComponentsFactory.createCoordinateField();
        maxLatField = UIComponentsFactory.createCoordinateField();
        minLonField = UIComponentsFactory.createCoordinateField();
        maxLonField = UIComponentsFactory.createCoordinateField();
        maxResultsSpinner = UIComponentsFactory.createResultSpinner();

        tableModel = new DefaultTableModel(new String[]{"File Name"}, 0);
        resultsTable = new JTable(tableModel);

        resultsTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int row = resultsTable.rowAtPoint(e.getPoint());
                if (row >= 0) {
                    String fileName = (String) tableModel.getValueAt(row, 0);
                    imagePreviewHelper.showFullImageDialog(fileName, OBDAACCloudDataBrowser.this);
                }
            }
        });

        // Layout placement
        add(new JLabel("Satellite/Instrument:"), gbc);
        gbc.gridx = 1;
        add(satelliteDropdown, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        add(new JLabel("Level:"), gbc);
        gbc.gridx = 1;
        add(levelDropdown, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        add(new JLabel("Product:"), gbc);
        gbc.gridx = 1;
        add(productDropdown, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        add(new JLabel("Start Date:"), gbc);
        gbc.gridx = 1;
        add(startDatePicker, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        add(new JLabel("End Date:"), gbc);
        gbc.gridx = 1;
        add(endDatePicker, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        add(new JLabel("Min Lat:"), gbc);
        gbc.gridx = 1;
        add(minLatField, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        add(new JLabel("Max Lat:"), gbc);
        gbc.gridx = 1;
        add(maxLatField, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        add(new JLabel("Min Lon:"), gbc);
        gbc.gridx = 1;
        add(minLonField, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        add(new JLabel("Max Lon:"), gbc);
        gbc.gridx = 1;
        add(maxLonField, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        add(new JLabel("Max Results:"), gbc);
        gbc.gridx = 1;
        add(maxResultsSpinner, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(e -> performSearch());
        add(searchButton, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        JScrollPane scrollPane = new JScrollPane(resultsTable);
        scrollPane.setPreferredSize(new Dimension(750, 250));
        add(scrollPane, gbc);
    }

    private void performSearch() {
        String satellite = (String) satelliteDropdown.getSelectedItem();
        String level = (String) levelDropdown.getSelectedItem();
        String product = (String) productDropdown.getSelectedItem();
        int maxResults = (Integer) maxResultsSpinner.getValue();

        String startDate = UIComponentsFactory.getFormattedDate(startDatePicker);
        String endDate = UIComponentsFactory.getFormattedDate(endDatePicker);

        String minLat = minLatField.getText().trim();
        String maxLat = maxLatField.getText().trim();
        String minLon = minLonField.getText().trim();
        String maxLon = maxLonField.getText().trim();

        tableModel.setRowCount(0);
        fileLinkMap = granuleFetcher.fetchGranules(product, maxResults, startDate, endDate, minLat, maxLat, minLon, maxLon, tableModel);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new OBDAACCloudDataBrowser().setVisible(true));
    }
}

