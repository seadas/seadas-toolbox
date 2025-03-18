package gov.nasa.gsfc.seadas.earthdatacloud.ui;

import org.jdatepicker.impl.JDatePanelImpl;
import org.jdatepicker.impl.JDatePickerImpl;
import org.jdatepicker.impl.UtilDateModel;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


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

        loadMetadata();  // Make sure this runs BEFORE initComponents()

        if (processedMetadata.isEmpty()) {
            System.err.println("❌ Error: Processed metadata is empty! GUI will not work.");
            return;
        }

        initComponents();
    }

    private java.util.Map<String, java.util.Map<String, java.util.List<String>>> processedMetadata = new java.util.HashMap<>();

    private void loadMetadata() {
        try {
            // Load JSON file
            Path metadataPath = Paths.get(System.getProperty("user.dir"), "seadas-toolbox",
                    "seadas-earthdata-cloud-toolbox", "src", "main",
                    "resources", "obdaac_metadata.json");

            System.out.println("📄 Loading metadata from: " + metadataPath.toAbsolutePath());

            if (!Files.exists(metadataPath) || Files.size(metadataPath) == 0) {
                System.err.println("❌ Error: Metadata file does not exist or is empty.");
                return;
            }

            // Read and parse JSON
            String content = new String(Files.readAllBytes(metadataPath), StandardCharsets.UTF_8);
            JSONParser parser = new JSONParser();
            JSONArray jsonArray = (JSONArray) parser.parse(content);

            // Process into a structured format
            processedMetadata.clear();

            for (Object obj : jsonArray) {
                JSONObject entry = (JSONObject) obj;

                // 🔍 Extract Satellite & Instrument
                String satellite = ((JSONArray) entry.get("platforms")).get(0).toString();
                String instrument = extractInstrument(entry); // ✅ Now correctly detects instrument
                String satelliteInstrumentKey = satellite + "/" + instrument;

                // 🔍 Extract Level & Product
                String level = entry.get("processing_level_id").toString();
                String productName = extractProductName(entry);

                // 🔄 Organize Metadata
                processedMetadata.putIfAbsent(satelliteInstrumentKey, new HashMap<>());
                processedMetadata.get(satelliteInstrumentKey).putIfAbsent(level, new ArrayList<>());
                processedMetadata.get(satelliteInstrumentKey).get(level).add(productName);

                System.out.println("✅ Stored: " + satelliteInstrumentKey + " | Level: " + level + " | Product: " + productName);
            }

            System.out.println("✅ Final Processed Metadata Structure:");
            System.out.println(processedMetadata);

        } catch (Exception e) {
            System.err.println("❌ Error loading metadata: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String extractInstrument(JSONObject entry) {
        // ✅ 1️⃣ Check "sensors" field first
        if (entry.containsKey("sensors")) {
            JSONArray sensors = (JSONArray) entry.get("sensors");
            if (!sensors.isEmpty()) {
                return sensors.get(0).toString();  // ✅ First sensor is the instrument
            }
        }

        // ✅ 2️⃣ Extract from "short_name" and ensure it's not just "L1", "L2"
        if (entry.containsKey("short_name")) {
            String shortName = entry.get("short_name").toString();
            String[] parts = shortName.split("_");
            if (parts.length > 1 && !parts[1].matches("L[0-9]+")) {  // Avoid L1, L2
                return parts[1];  // ✅ Second part is usually the instrument
            }
        }

        // ✅ 3️⃣ Check "dataset_id" for fallback
        if (entry.containsKey("dataset_id")) {
            String datasetId = entry.get("dataset_id").toString();
            String[] datasetParts = datasetId.split("\\s+");
            if (datasetParts.length > 1 && !datasetParts[0].matches("L[0-9]+")) {
                return datasetParts[0];  // ✅ First part of dataset_id
            }
        }

        // ✅ 4️⃣ Special Cases: Fix Naming for PACE, SeaHawk, etc.
        String satellite = ((JSONArray) entry.get("platforms")).get(0).toString();
        if (satellite.equalsIgnoreCase("SeaHawk-1")) return "HawkEye";
        if (satellite.equalsIgnoreCase("ADEOS-I")) return "OCTS";
        if (satellite.equalsIgnoreCase("Nimbus-7")) return "CZCS";
        if (satellite.equalsIgnoreCase("OrbView-2")) return "SeaWiFS";
        if (satellite.equalsIgnoreCase("ENVISAT")) return "MERIS";

        if (satellite.equalsIgnoreCase("PACE")) {
            if (entry.get("short_name").toString().contains("OCI")) return "OCI";
            if (entry.get("short_name").toString().contains("HARP2")) return "HARP2";
            if (entry.get("short_name").toString().contains("SPEXONE")) return "SPEXONE";
            if (entry.get("short_name").toString().contains("EPH")) return "EPH";
            if (entry.get("short_name").toString().contains("HSK")) return "HSK";
            if (entry.get("short_name").toString().contains("HKT")) return "HKT";
        }

        return "Unknown Instrument";  // ❌ Final fallback
    }




    private String extractProductName(JSONObject entry) {
        String title = entry.get("title").toString();

        // ✅ 1️⃣ Check for Acronyms in Parentheses
        if (title.matches(".*\\(([^)]+)\\).*")) {
            return title.replaceAll(".*\\(([^)]+)\\).*", "$1");  // Extracts the acronym
        }

        // ✅ 2️⃣ If No Acronym, Try Extracting Last Meaningful Word
        String[] words = title.split(" ");
        if (words.length > 1) {
            String lastWord = words[words.length - 1];  // Last word in title
            if (lastWord.length() <= 4) {  // Ensure it's a short and meaningful identifier
                return lastWord;
            }
        }

        // ❌ 3️⃣ Fallback: Return Full Name (If No Better Option)
        return title;
    }


    private void initComponents() {
        if (processedMetadata == null || processedMetadata.isEmpty()) {
            System.err.println("⚠ Warning: Processed metadata is empty, skipping UI updates.");
            return;
        }

        // Dropdowns
        satelliteDropdown = new JComboBox<>();
        levelDropdown = new JComboBox<>();
        productDropdown = new JComboBox<>();

        // Populate satellites
        for (String satellite : processedMetadata.keySet()) {
            satelliteDropdown.addItem(satellite);
        }

        // Trigger first update if possible
        if (satelliteDropdown.getItemCount() > 0) {
            satelliteDropdown.setSelectedIndex(0);
            updateLevels();  // 🔄 Ensure levels populate
        }

        // Add listeners
        satelliteDropdown.addActionListener(e -> updateLevels());
        levelDropdown.addActionListener(e -> updateProducts());

        // Add components to frame
        add(new JLabel("Satellite/Instrument:")); add(satelliteDropdown);
        add(new JLabel("Data Level:")); add(levelDropdown);
        add(new JLabel("Product Name:")); add(productDropdown);
    }

    private java.util.List<String> getSatelliteNames() {
        return new ArrayList<>(processedMetadata.keySet());
    }

    private void updateLevels() {
        String selectedSatellite = (String) satelliteDropdown.getSelectedItem();
        levelDropdown.removeAllItems();

        if (selectedSatellite == null || !processedMetadata.containsKey(selectedSatellite)) {
            System.err.println("⚠ No metadata for selected satellite: " + selectedSatellite);
            return;
        }

        Map<String, List<String>> levels = processedMetadata.get(selectedSatellite);

        if (levels == null || levels.isEmpty()) {
            System.err.println("⚠ No levels found for satellite: " + selectedSatellite);
            return;
        }

        for (String level : levels.keySet()) {
            levelDropdown.addItem(level);
        }

        // Ensure the first available level is selected
        if (levelDropdown.getItemCount() > 0) {
            levelDropdown.setSelectedIndex(0);
            updateProducts(); // 🔄 Ensure products are updated
        }
    }



    private void updateProducts() {
        String selectedSatelliteInstrument = (String) satelliteDropdown.getSelectedItem();
        String selectedLevel = (String) levelDropdown.getSelectedItem();
        productDropdown.removeAllItems();

        if (selectedSatelliteInstrument == null || selectedLevel == null) {
            System.err.println("⚠ No satellite/instrument or level selected.");
            return;
        }

        if (!processedMetadata.containsKey(selectedSatelliteInstrument)) {
            System.err.println("❌ Error: Satellite/Instrument not found: " + selectedSatelliteInstrument);
            return;
        }

        Map<String, List<String>> levels = processedMetadata.get(selectedSatelliteInstrument);
        if (!levels.containsKey(selectedLevel)) {
            System.err.println("❌ Error: No products found for level: " + selectedLevel);
            return;
        }

        List<String> products = levels.get(selectedLevel);
        if (products == null || products.isEmpty()) {
            System.err.println("⚠ No products available for level: " + selectedLevel);
            return;
        }

        System.out.println("📄 Products for " + selectedLevel + ": " + products);

        for (String product : products) {
            productDropdown.addItem(product);
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
