package gov.nasa.gsfc.seadas.earthdatacloud.ui;

import gov.nasa.gsfc.seadas.earthdatacloud.action.LinkCellRenderer;
import org.jdatepicker.impl.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class OBDAACDataBrowser extends JFrame {
    private JComboBox<String> satelliteDropdown, levelDropdown, productDropdown;
    private JDatePickerImpl startDatePicker, endDatePicker;
    private JTextField minLatField, maxLatField, minLonField, maxLonField;
    private JTable resultsTable;
    private DefaultTableModel tableModel;
    private JSpinner maxResultsSpinner;
    private final Map<String, String> productNameTooltips = new HashMap<>();

    private final Map<String, JSONObject> metadataMap = new HashMap<>();
    private JRadioButton dayButton, nightButton, bothButton;
    private ButtonGroup dayNightGroup;

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

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        JPanel filterPanel = new JPanel(new GridLayout(1, 3, 10, 0)); // 👈 Now 3 columns
        filterPanel.add(createTemporalPanel());
        filterPanel.add(createSpatialPanel());
        filterPanel.add(createDayNightPanel());
        add(filterPanel, gbc);

// Max Results Label
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        add(new JLabel("Max Results:"), gbc);

// Max Results Spinner
        gbc.gridx = 1;
        maxResultsSpinner = new JSpinner(new SpinnerNumberModel(25, 1, 10000, 1));
        maxResultsSpinner.setPreferredSize(new Dimension(80, 25));

        JPanel spinnerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        spinnerPanel.add(maxResultsSpinner);
        add(spinnerPanel, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0)); // 15px spacing
        JButton searchButton = new JButton("Search");
        JButton cancelButton = new JButton("Cancel");

// Set uniform button size
        Dimension buttonSize = new Dimension(110, 30); // you can tweak this if needed
        searchButton.setPreferredSize(buttonSize);
        cancelButton.setPreferredSize(buttonSize);

        searchButton.addActionListener(e -> fetchGranules());
        cancelButton.addActionListener(e -> System.exit(0)); // or other cancel logic

// Add buttons to panel
        buttonPanel.add(searchButton);
        buttonPanel.add(cancelButton);

// Add to layout
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        add(buttonPanel, gbc);

        gbc.gridy++; gbc.gridx = 0; gbc.gridwidth = 2;
        String[] columns = {"Granule ID", "Download URL", "Day/Night Flag"};
        tableModel = new DefaultTableModel(columns, 0);
        resultsTable = new JTable(tableModel);
        resultsTable.getColumnModel().getColumn(1).setCellRenderer(new LinkCellRenderer());
        resultsTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int row = resultsTable.rowAtPoint(e.getPoint());
                int col = resultsTable.columnAtPoint(e.getPoint());
                if (col == 1) {
                    String url = (String) resultsTable.getValueAt(row, col);
                    try {
                        Desktop.getDesktop().browse(new URI(url));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
        JScrollPane scrollPane = new JScrollPane(resultsTable);
        scrollPane.setPreferredSize(new Dimension(750, 250));
        add(scrollPane, gbc);

        if (satelliteDropdown.getItemCount() > 0) {
            satelliteDropdown.setSelectedIndex(0);
            updateLevelsAndProducts();
        }
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
        c.insets = new Insets(5, 5, 5, 5);
        c.anchor = GridBagConstraints.WEST;

        // Create date pickers
        startDatePicker = createDatePicker();
        endDatePicker = createDatePicker();

        // Set fixed size to ensure visibility
        Dimension fieldSize = new Dimension(180, 28);
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
        gbc.insets = new Insets(4, 5, 4, 5);
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
        tableModel.setRowCount(0);
        @SuppressWarnings("unchecked")
        Map<String, String> fileMap = (Map<String, String>) resultsTable.getClientProperty("fileMapping");
        if (fileMap == null) {
            fileMap = new HashMap<>();
            resultsTable.putClientProperty("fileMapping", fileMap);
        }

        String product = (String) productDropdown.getSelectedItem();
        int maxResults = (Integer) maxResultsSpinner.getValue();

        int pageSize = 2000;
        int page = 1;
        int totalFetched = 0;

        try {
            while (totalFetched < maxResults) {
                URL url = new URL("https://cmr.earthdata.nasa.gov/search/granules.json?provider=OB_CLOUD&short_name=" + product + "&page_size=" + pageSize + "&page_num=" + page);
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
                            fileMap.put(title, href);
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


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new OBDAACDataBrowser().setVisible(true));
    }
}
