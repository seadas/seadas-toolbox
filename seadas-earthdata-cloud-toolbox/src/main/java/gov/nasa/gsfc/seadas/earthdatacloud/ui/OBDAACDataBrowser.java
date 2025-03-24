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
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

public class OBDAACDataBrowser extends JFrame {
    private JComboBox<String> satelliteDropdown;
    private JComboBox<String> levelDropdown;
    private JComboBox<String> productDropdown;
    private JDatePickerImpl startDatePicker, endDatePicker;
    private JCheckBox enableSpatialFiltering;
    private JTextField minLatField, maxLatField, minLonField, maxLonField;
    private Map<String, Map<String, List<String>>> processedMetadata = new HashMap<>();
    private final Map<String, String> productNameTooltips = new HashMap<>();


    public OBDAACDataBrowser() {
        setTitle("OBDAAC Data Browser");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(7, 2));

        loadMetadata();  // Calls Python script and processes metadata

        if (processedMetadata.isEmpty()) {
            System.err.println("❌ Error: Processed metadata is empty! GUI will not work.");
            return;
        }

        initComponents();
    }

    /**
     * 🔹 Run Python script to extract metadata.
     * ProcessBuilder pb = new ProcessBuilder("python", "seadas-toolbox/seadas-earthdata-cloud-toolbox/src/main/CMR_script.py");
     */
    private void loadMetadata() {
        try {
            Path jsonDir = Paths.get(System.getProperty("user.dir"), "seadas-toolbox",
                    "seadas-earthdata-cloud-toolbox", "src", "main", "resources", "json-files");

            System.out.println("Reading metadata from directory: " + jsonDir.toAbsolutePath());

            if (!Files.exists(jsonDir) || !Files.isDirectory(jsonDir)) {
                System.err.println("Directory does not exist: " + jsonDir);
                runPythonScriptForCMRMetada();
                //return;
            }

            processedMetadata.clear();
            JSONParser parser = new JSONParser();

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(jsonDir, "*.json")) {
                for (Path path : stream) {
                    String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
                    JSONObject obj = (JSONObject) parser.parse(content);

                    for (Object levelKey : obj.keySet()) {
                        String level = (String) levelKey;
                        JSONArray products = (JSONArray) obj.get(level);

                        String filename = path.getFileName().toString().replace(".json", "");
                        String satelliteInstrumentKey = filename.contains("-") ? filename.replace("-", "/") : filename;

                        processedMetadata.putIfAbsent(satelliteInstrumentKey, new HashMap<>());
                        List<String> productList = new ArrayList<>();
                        for (Object prod : products) {
                            productList.add(prod.toString());
                        }
                        processedMetadata.get(satelliteInstrumentKey).put(level, productList);

                        //System.out.println("Stored: " + satelliteInstrumentKey + " | Level: " + level + " | Product(s): " + productList);
                    }
                }
            }

            System.out.println("Final Processed Metadata Structure:");
            System.out.println(processedMetadata);

        } catch (Exception e) {
            System.err.println("Error loading metadata: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void runPythonScriptForCMRMetada(){
        System.out.println("Running Python script for metadata extraction...");
        try {
            ProcessBuilder pb = new ProcessBuilder("python", "seadas-toolbox/seadas-earthdata-cloud-toolbox/src/main/CMR_script.py");
            pb.directory(new File(System.getProperty("user.dir"))); // Ensure correct directory
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder jsonOutput = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonOutput.append(line);
            }

            process.waitFor();

            if (jsonOutput.length() == 0) {
                System.err.println("❌ Error: Python script returned no metadata.");
            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
        } catch (InterruptedException interruptedException) {
            interruptedException.printStackTrace();
        }
    }
    /**
     * 🔹 Initializes UI Components
     */
    private void initComponents() {
        if (processedMetadata == null || processedMetadata.isEmpty()) {
            System.err.println("⚠ Warning: Processed metadata is empty, skipping UI updates.");
            return;
        }

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10); // spacing between elements
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;

        satelliteDropdown = new JComboBox<>();
        levelDropdown = new JComboBox<>();
        productDropdown = new JComboBox<>();

        for (String satellite : processedMetadata.keySet()) {
            satelliteDropdown.addItem(satellite);
        }

        if (satelliteDropdown.getItemCount() > 0) {
            satelliteDropdown.setSelectedIndex(0);
            updateLevels();
        }

        satelliteDropdown.addActionListener(e -> updateLevels());
        levelDropdown.addActionListener(e -> updateProducts());

        // Satellite/Instrument
        add(new JLabel("Satellite/Instrument:"), gbc);
        gbc.gridx = 1;
        add(satelliteDropdown, gbc);

        // Level
        gbc.gridy++;
        gbc.gridx = 0;
        add(new JLabel("Data Level:"), gbc);
        gbc.gridx = 1;
        add(levelDropdown, gbc);

        // Product
        gbc.gridy++;
        gbc.gridx = 0;
        add(new JLabel("Product Name:"), gbc);
        gbc.gridx = 1;
        add(productDropdown, gbc);

        // Filter Panels (Temporal + Spatial side-by-side)
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        JPanel filterPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        filterPanel.add(createTemporalPanel());
        filterPanel.add(createSpatialPanel());
        add(filterPanel, gbc);
    }


    private JPanel createTemporalPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Temporal Filter"));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);  // Padding
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        // Create date pickers
        startDatePicker = createDatePicker();
        endDatePicker = createDatePicker();

        // Set preferred size for better display
        Dimension dateSize = new Dimension(150, 25);
        startDatePicker.setPreferredSize(dateSize);
        endDatePicker.setPreferredSize(dateSize);
        startDatePicker.setMinimumSize(dateSize);
        endDatePicker.setMinimumSize(dateSize);

        // Start Date Label
        c.gridx = 0;
        c.gridy = 0;
        panel.add(new JLabel("Start Date:"), c);

        // Start Date Picker
        c.gridx = 1;
        c.gridy = 0;
        panel.add(startDatePicker, c);

        // End Date Label
        c.gridx = 0;
        c.gridy = 1;
        panel.add(new JLabel("End Date:"), c);

        // End Date Picker
        c.gridx = 1;
        c.gridy = 1;
        panel.add(endDatePicker, c);

        return panel;
    }


    private JPanel createSpatialPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Spatial Filter"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 5, 4, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;

        enableSpatialFiltering = new JCheckBox("Enable Spatial Filtering");
        panel.add(enableSpatialFiltering, gbc);

        gbc.gridwidth = 1;
        gbc.gridy++;
        gbc.gridx = 0;
        panel.add(new JLabel("Min Lat:"), gbc);
        gbc.gridx = 1;
        minLatField = new JTextField();
        panel.add(minLatField, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        panel.add(new JLabel("Max Lat:"), gbc);
        gbc.gridx = 1;
        maxLatField = new JTextField();
        panel.add(maxLatField, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        panel.add(new JLabel("Min Lon:"), gbc);
        gbc.gridx = 1;
        minLonField = new JTextField();
        panel.add(minLonField, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        panel.add(new JLabel("Max Lon:"), gbc);
        gbc.gridx = 1;
        maxLonField = new JTextField();
        panel.add(maxLonField, gbc);

        enableSpatialFiltering.addItemListener(e -> toggleSpatialFields(enableSpatialFiltering.isSelected()));
        toggleSpatialFields(false);

        return panel;
    }



    private JDatePickerImpl createDatePicker() {
        UtilDateModel model = new UtilDateModel();
        Properties p = new Properties();
        p.put("text.today", "Today");
        p.put("text.month", "Month");
        p.put("text.year", "Year");

        JDatePanelImpl datePanel = new JDatePanelImpl(model, p);
        return new JDatePickerImpl(datePanel, new org.jdatepicker.impl.DateComponentFormatter());
    }

    private void toggleSpatialFields(boolean enabled) {
        minLatField.setEnabled(enabled);
        maxLatField.setEnabled(enabled);
        minLonField.setEnabled(enabled);
        maxLonField.setEnabled(enabled);
    }

    /**
     * 🔹 Updates Level Dropdown
     */
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

        if (levelDropdown.getItemCount() > 0) {
            levelDropdown.setSelectedIndex(0);
            updateProducts();
        }
    }

    /**
     * 🔹 Updates Product Dropdown
     */
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

        for (String product : products) {
            // If product contains short_name and name separated, e.g. "OC|OrbView-2_SeaWiFS_L2_OC"
            if (product.contains(",")) {
                String[] parts = product.split("\\,");
                String shortName = parts[0];
                String displayName = parts[1];
                parts = displayName.split(":");
                String productName = parts[1].isEmpty()?"Noname":parts[1];
                productName = productName.replace("\"", "");
                productName = productName.replace("}", "");
                productDropdown.addItem(productName);
                System.out.println("✅ Display name:" + displayName);
                System.out.println("✅ Product name:" + productName);
                productNameTooltips.put(productName, shortName);
            } else {
                productDropdown.addItem(product);
            }
        }

        System.out.println("📄 Products for " + selectedLevel + ": " + products);

        // Clear tooltip
        productDropdown.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof String) {
                    String productName = (String) value;
                    String shortName = productNameTooltips.getOrDefault(productName, productName);
                    setToolTipText(shortName);
                }
                return c;
            }
        });

    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new OBDAACDataBrowser().setVisible(true));
    }
}
