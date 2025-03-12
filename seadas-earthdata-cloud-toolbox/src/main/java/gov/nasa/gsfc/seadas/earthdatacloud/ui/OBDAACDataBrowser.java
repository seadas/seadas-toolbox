package gov.nasa.gsfc.seadas.earthdatacloud.ui;

import org.jdatepicker.impl.JDatePanelImpl;
import org.jdatepicker.impl.JDatePickerImpl;
import org.jdatepicker.impl.UtilDateModel;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Properties;

public class OBDAACDataBrowser extends JFrame {
    private JComboBox<String> satelliteDropdown;
    private JComboBox<String> levelDropdown;
    private JComboBox<String> productDropdown;
    private JTextField searchField;
    private JDatePickerImpl startDatePicker, endDatePicker;
    private JCheckBox enableSpatialFiltering;
    private JTextField minLatField, maxLatField, minLonField, maxLonField;
    private JSONArray metadata;
    public OBDAACDataBrowser() {
        setTitle("OBDAAC Data Browser");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(7, 2));

        loadMetadata();
        initComponents();
    }

    private JSONArray loadMetadata() {
        try {
            // Define the correct module directory path dynamically
            String moduleDir = System.getProperty("user.dir") + File.separator + "seadas-toolbox" + File.separator + "seadas-earthdata-cloud-toolbox";
            String filePath = Paths.get(moduleDir, "src", "main", "resources", "obdaac_metadata.json").toString();

            System.out.println("📄 Loading metadata from: " + filePath);

            // Read JSON file
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(new FileReader(filePath));

            // Ensure it is a JSON array
            if (obj instanceof JSONArray) {
                System.out.println("✅ Metadata successfully loaded.");
                return (JSONArray) obj;
            } else {
                System.err.println("❌ Unexpected JSON format: Expected JSONArray but got " + obj.getClass().getSimpleName());
                return new JSONArray(); // Return an empty array instead of null
            }
        } catch (IOException | org.json.simple.parser.ParseException e) {
            System.err.println("❌ Error loading metadata: " + e.getMessage());
            e.printStackTrace();
            return new JSONArray(); // Return an empty array on failure
        }
    }

    private void initComponents() {
        // Dropdowns
        satelliteDropdown = new JComboBox<>(new DefaultComboBoxModel<>(
                getSatelliteNames().toArray(new String[0])
        ));

        satelliteDropdown = new JComboBox<>(new DefaultComboBoxModel<>(getSatelliteNames().toArray(new String[0])));
        levelDropdown = new JComboBox<>();
        productDropdown = new JComboBox<>();
        searchField = new JTextField();

        // Date Pickers
        startDatePicker = createDatePicker();
        endDatePicker = createDatePicker();

        // Spatial Constraints
        enableSpatialFiltering = new JCheckBox("Enable Spatial Filtering");
        minLatField = new JTextField();
        maxLatField = new JTextField();
        minLonField = new JTextField();
        maxLonField = new JTextField();

        enableSpatialFiltering.addActionListener(e -> toggleSpatialFields(enableSpatialFiltering.isSelected()));

        // Add Components to Frame
        add(new JLabel("Satellite/Instrument:")); add(satelliteDropdown);
        add(new JLabel("Data Level:")); add(levelDropdown);
        add(new JLabel("Product Name:")); add(productDropdown);
        add(new JLabel("Search String:")); add(searchField);
        add(new JLabel("Start Date:")); add(startDatePicker);
        add(new JLabel("End Date:")); add(endDatePicker);
        add(enableSpatialFiltering); add(new JPanel());
        add(new JLabel("Min Lat:")); add(minLatField);
        add(new JLabel("Max Lat:")); add(maxLatField);
        add(new JLabel("Min Lon:")); add(minLonField);
        add(new JLabel("Max Lon:")); add(maxLonField);

        satelliteDropdown.addActionListener(e -> updateLevels());
        levelDropdown.addActionListener(e -> updateProducts());
    }
    private java.util.List<String> getSatelliteNames() {
        java.util.List<String> satelliteNames = new java.util.ArrayList<>();

        if (metadata == null || metadata.isEmpty()) {
            System.err.println("⚠ Warning: Metadata is empty or null.");
            return satelliteNames;
        }

        for (Object obj : metadata) {
            if (obj instanceof JSONObject) {
                JSONObject entry = (JSONObject) obj;
                String satellite = (String) entry.get("satellite");
                if (satellite != null && !satelliteNames.contains(satellite)) {
                    satelliteNames.add(satellite);
                }
            }
        }

        return satelliteNames;
    }

    private void updateLevels() {
        String selectedSatellite = (String) satelliteDropdown.getSelectedItem();
        levelDropdown.removeAllItems();

        for (Object obj : metadata) {
            if (obj instanceof JSONObject) {
                JSONObject entry = (JSONObject) obj;
                String satellite = (String) entry.get("satellite");

                if (selectedSatellite.equals(satellite)) {
                    JSONArray levels = (JSONArray) entry.get("levels"); // Assuming "levels" key exists
                    if (levels != null) {
                        for (Object level : levels) {
                            levelDropdown.addItem((String) level);
                        }
                    }
                    break; // Stop searching after finding the matching satellite
                }
            }
        }
    }

    private void updateProducts() {
        String selectedSatellite = (String) satelliteDropdown.getSelectedItem();
        String selectedLevel = (String) levelDropdown.getSelectedItem();
        productDropdown.removeAllItems();

        for (Object obj : metadata) {
            if (obj instanceof JSONObject) {
                JSONObject entry = (JSONObject) obj;
                String satellite = (String) entry.get("satellite");

                if (selectedSatellite.equals(satellite)) {
                    JSONArray levels = (JSONArray) entry.get("levels"); // Assuming "levels" key exists
                    if (levels != null) {
                        for (Object levelObj : levels) {
                            JSONObject levelEntry = (JSONObject) levelObj;
                            String level = (String) levelEntry.get("level");

                            if (selectedLevel.equals(level)) {
                                JSONArray products = (JSONArray) levelEntry.get("products"); // Assuming "products" key exists
                                if (products != null) {
                                    for (Object productObj : products) {
                                        JSONObject product = (JSONObject) productObj;
                                        productDropdown.addItem((String) product.get("short_name"));
                                    }
                                }
                                break; // Stop searching after finding the correct level
                            }
                        }
                    }
                    break; // Stop searching after finding the matching satellite
                }
            }
        }
    }


    private JDatePickerImpl createDatePicker() {
        // Create the date model
        UtilDateModel model = new UtilDateModel();

        // Create properties for date picker (required for localization)
        Properties p = new Properties();
        p.put("text.today", "Today");
        p.put("text.month", "Month");
        p.put("text.year", "Year");

        // Create date panel with properties
        JDatePanelImpl datePanel = new JDatePanelImpl(model, p);

        // Create date picker with date panel
        return new JDatePickerImpl(datePanel, new org.jdatepicker.impl.DateComponentFormatter());
    }

    private void toggleSpatialFields(boolean enable) {
        minLatField.setEnabled(enable);
        maxLatField.setEnabled(enable);
        minLonField.setEnabled(enable);
        maxLonField.setEnabled(enable);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new OBDAACDataBrowser().setVisible(true));
    }
}
