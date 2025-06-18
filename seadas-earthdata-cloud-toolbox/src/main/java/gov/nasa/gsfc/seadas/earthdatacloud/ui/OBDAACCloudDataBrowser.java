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
    private final Map<String, String> fileLinkMap = new HashMap<>(100);
    private final Map<String, String> productNameTooltips = new HashMap<>(50);
    private JLabel dateRangeHintLabel = new JLabel();  // Declare as a class member
    private JPanel temporalPanel;
    private JLabel dateRangeLabel; // a

    Map<String, String[]> missionDateRanges = Map.of(
            "SeaHawk/HawkEye", new String[]{"2018-12-01", "2023-12-31"},
            "MODISA", new String[]{"2002-07-04", "2024-12-31"},
            "VIIRSN", new String[]{"2011-10-28", "2024-12-31"}
            // Add more as needed
    );

    private MetadataLoader metadataLoader;
    private GranuleFetcher granuleFetcher;
    private FileDownloader fileDownloader;
    private ImagePreviewHandler imagePreviewHelper;

    private Map<String, JSONObject> metadataMap = new HashMap<>();

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
        fileLinkMap.clear();
        fileLinkMap.putAll(granuleFetcher.fetchGranules(product, maxResults, startDate, endDate, minLat, maxLat, minLon, maxLon, tableModel));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new OBDAACCloudDataBrowser().setVisible(true));
    }
}

