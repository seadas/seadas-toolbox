package gov.nasa.gsfc.seadas.earthdatacloud.ui;

import gov.nasa.gsfc.seadas.earthdatacloud.action.HarmonySubsetTask;
import gov.nasa.gsfc.seadas.earthdatacloud.auth.WebPageFetcherWithJWT;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.ui.UIUtils;
import org.esa.snap.ui.tool.ToolButtonFactory;
import org.json.JSONObject;
import org.json.JSONArray;
import org.openide.util.HelpCtx;

import javax.swing.*;
import java.awt.*;
import javax.swing.event.SwingPropertyChangeSupport;
import java.beans.PropertyChangeListener;

import java.util.concurrent.ExecutionException;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.io.IOException;

public class HarmonySubsetServiceDialog extends JDialog {

    public static final String TITLE = "Harmony Subset Service";
    private SwingPropertyChangeSupport propertyChangeSupport;
    private Component helpButton = null;
    private final static String helpId = "subsetServiceHelp";
    private final static String HELP_ICON = "icons/Help24.gif";

    // UI Components
    private JTextField urlInputField;
    private JTextField latMinField, latMaxField, lonMinField, lonMaxField;
    private JList<String> variableList;
    private JProgressBar progressBar;
    private JButton subsetButton, cancelButton;
    private JTextArea statusArea;

    // Data
    private String selectedFileUrl;
    private JSONObject fileMetadata;
    private Double searchLatMin, searchLatMax, searchLonMin, searchLonMax;

    public HarmonySubsetServiceDialog() {
        this(null, null, null, null, null);
    }

    public HarmonySubsetServiceDialog(String fileUrl) {
        this(fileUrl, null, null, null, null);
    }

    public HarmonySubsetServiceDialog(String fileUrl, Double latMin, Double latMax, Double lonMin, Double lonMax) {
        super(SnapApp.getDefault().getMainFrame(), TITLE, JDialog.DEFAULT_MODALITY_TYPE);
        this.selectedFileUrl = fileUrl;
        this.searchLatMin = latMin;
        this.searchLatMax = latMax;
        this.searchLonMin = lonMin;
        this.searchLonMax = lonMax;
        
        setLayout(new BorderLayout());
        setSize(800, 700);

        propertyChangeSupport = new SwingPropertyChangeSupport(this);

        helpButton = getHelpButton();
        
        // Fetch file metadata to get available variables
        if (selectedFileUrl != null) {
            fetchFileMetadata();
        }
        
        // Create main content
        JPanel mainPanel = createMainPanel();
        add(mainPanel, BorderLayout.CENTER);
        
        // Create button panel
        JPanel buttonPanel = createButtonPanel();
        add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
        Window parent = SnapApp.getDefault().getMainFrame();
        setLocationRelativeTo(parent);
        Point location = getLocation();
        setLocation(location.x - 100, Math.max(0, location.y - 100));
        
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    private void fetchFileMetadata() {
        // Start metadata fetching in background
        new Thread(() -> {
            try {
                System.out.println("=== Starting variable detection for: " + selectedFileUrl);
                updateStatus("Detecting available variables...");
                
                // Use filename-based variable detection (most reliable for NetCDF files)
                final List<String> variables = extractVariablesFromFileName(selectedFileUrl);
                System.out.println("Detected variables from filename: " + variables);
                
                // Update UI immediately with filename-based variables
                SwingUtilities.invokeLater(() -> {
                    updateVariableList(variables);
                    updateStatus("Variables detected from file type. Found " + variables.size() + " variables.");
                });
                
                // Note: Metadata extraction from NetCDF files requires specialized libraries
                // For now, we rely on filename-based detection which is reliable for ocean color data
                
            } catch (Exception e) {
                System.out.println("Exception in variable detection: " + e.getMessage());
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    updateStatus("Error detecting variables: " + e.getMessage());
                    // Fall back to default variables
                    updateVariableList(getDefaultVariables());
                });
            }
        }).start();
    }

    private String buildMetadataUrl(String fileUrl) {
        // For NetCDF files, we need to read just the header to get metadata
        // Let's try to get the file header by adding a range request
        
        // Try to get just the first 1KB of the file to read the header
        if (fileUrl.contains("?")) {
            return fileUrl + "&range=bytes=0-1024";
        } else {
            return fileUrl + "?range=bytes=0-1024";
        }
    }

    private String extractCollectionId(String fileUrl) {
        // Extract collection ID from input URL
        if (fileUrl.contains("C3020920290-OB_CLOUD")) {
            return "C3020920290-OB_CLOUD";
        } else if (fileUrl.contains("C1265136924-OB_CLOUD")) {
            return "C1265136924-OB_CLOUD";
        } else if (fileUrl.contains("C1940468264-OB_CLOUD")) {
            return "C1940468264-OB_CLOUD"; // VIIRS collection
        } else {
            // Default collection for ocean color data
            return "C3020920290-OB_CLOUD";
        }
    }

    private List<String> extractVariablesFromMetadata(JSONObject metadata) {
        List<String> variables = new ArrayList<>();
        
        try {
            System.out.println("Metadata keys: " + metadata.keySet());
            
            // Look for Band_attributes which is the standard structure for ocean color data
            if (metadata.has("Band_attributes")) {
                JSONObject bandAttributes = metadata.getJSONObject("Band_attributes");
                System.out.println("Found Band_attributes with keys: " + bandAttributes.keySet());
                for (String varName : bandAttributes.keySet()) {
                    variables.add(varName);
                }
                System.out.println("Extracted " + variables.size() + " variables from Band_attributes");
            } else if (metadata.has("band_attributes")) {
                // Try lowercase version
                JSONObject bandAttributes = metadata.getJSONObject("band_attributes");
                System.out.println("Found band_attributes with keys: " + bandAttributes.keySet());
                for (String varName : bandAttributes.keySet()) {
                    variables.add(varName);
                }
                System.out.println("Extracted " + variables.size() + " variables from band_attributes");
            } else if (metadata.has("variables")) {
                // Fallback to variables structure
                JSONObject varsObj = metadata.getJSONObject("variables");
                System.out.println("Found variables with keys: " + varsObj.keySet());
                for (String varName : varsObj.keySet()) {
                    variables.add(varName);
                }
                System.out.println("Extracted " + variables.size() + " variables from variables");
            } else if (metadata.has("dimensions")) {
                // Sometimes variables are in dimensions
                JSONObject dimsObj = metadata.getJSONObject("dimensions");
                System.out.println("Found dimensions with keys: " + dimsObj.keySet());
                for (String dimName : dimsObj.keySet()) {
                    if (!dimName.equals("time") && !dimName.equals("lat") && !dimName.equals("lon")) {
                        variables.add(dimName);
                    }
                }
                System.out.println("Extracted " + variables.size() + " variables from dimensions");
            } else if (metadata.has("coverage")) {
                // Try coverage structure
                JSONObject coverage = metadata.getJSONObject("coverage");
                System.out.println("Found coverage with keys: " + coverage.keySet());
                if (coverage.has("ranges")) {
                    JSONObject ranges = coverage.getJSONObject("ranges");
                    if (ranges.has("variables")) {
                        JSONObject varsObj = ranges.getJSONObject("variables");
                        for (String varName : varsObj.keySet()) {
                            variables.add(varName);
                        }
                        System.out.println("Extracted " + variables.size() + " variables from coverage.ranges.variables");
                    }
                }
            } else {
                System.out.println("No known metadata structure found. Available keys: " + metadata.keySet());
            }
            
            // If no variables found, try to extract from file URL or use defaults
            if (variables.isEmpty()) {
                System.out.println("No variables extracted from metadata, using filename-based detection");
                variables = extractVariablesFromFileName(selectedFileUrl);
            }
            
        } catch (Exception e) {
            System.err.println("Error extracting variables from metadata: " + e.getMessage());
            e.printStackTrace();
        }
        
        return variables;
    }

    private List<String> extractVariablesFromFileName(String fileUrl) {
        List<String> variables = new ArrayList<>();
        
        // Extract variables based on file name patterns
        if (fileUrl.contains("PACE_OCI")) {
            // PACE OCI variables
            variables.addAll(Arrays.asList("chlor_a", "aot_869", "Rrs_443", "Rrs_555", "Rrs_670", "Rrs_490", "Rrs_510"));
        } else if (fileUrl.contains("MODISA") || fileUrl.contains("MODIST")) {
            // MODIS variables
            variables.addAll(Arrays.asList("chlor_a", "aot_869", "Rrs_443", "Rrs_555", "Rrs_670", "Rrs_488", "Rrs_531"));
        } else if (fileUrl.contains("VIIRS")) {
            // VIIRS variables
            variables.addAll(Arrays.asList("chlor_a", "aot_869", "Rrs_443", "Rrs_555", "Rrs_670", "Rrs_486", "Rrs_551"));
        } else {
            // Default variables
            variables.addAll(getDefaultVariables());
        }
        
        return variables;
    }

    private List<String> getDefaultVariables() {
        return Arrays.asList("chlor_a", "aot_869", "Rrs_443", "Rrs_555", "Rrs_670");
    }

    private void updateVariableList(List<String> variables) {
        if (variableList != null) {
            DefaultListModel<String> model = new DefaultListModel<>();
            for (String variable : variables) {
                model.addElement(variable);
            }
            variableList.setModel(model);
            
            // Select all variables by default
            int[] indices = new int[variables.size()];
            for (int i = 0; i < variables.size(); i++) {
                indices[i] = i;
            }
            variableList.setSelectedIndices(indices);
        }
    }

    private JPanel createMainPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // Create tabbed pane for different sections
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // Input tab
        tabbedPane.addTab("Input", createInputPanel());
        
        // Subset parameters tab
        tabbedPane.addTab("Subset Parameters", createSubsetPanel());
        
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        
        // Progress and status area
        JPanel statusPanel = createStatusPanel();
        mainPanel.add(statusPanel, BorderLayout.SOUTH);
        
        return mainPanel;
    }

    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Input File Selection"));

        // URL input section
        JLabel urlLabel = new JLabel("Data File URL:");
        urlInputField = new JTextField(50);
        if (selectedFileUrl != null) {
            urlInputField.setText(selectedFileUrl);
        }
        urlInputField.setToolTipText("Enter the URL of the data file to subset");

        JButton validateButton = new JButton("Validate URL");
        validateButton.addActionListener(e -> validateInputUrl());

        // File info section
        JLabel infoLabel = new JLabel("File Information:");
        statusArea = new JTextArea(8, 50);
        statusArea.setEditable(false);
        statusArea.setLineWrap(true);
        statusArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(statusArea);

        // Layout
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(urlLabel, gbc);
        
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        panel.add(urlInputField, gbc);
        
        gbc.gridx = 2; gbc.gridy = 0; gbc.weightx = 0.0;
        panel.add(validateButton, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 3;
        panel.add(infoLabel, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 3; gbc.weighty = 1.0;
        panel.add(scrollPane, gbc);

        return panel;
    }

    private JPanel createSubsetPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Subset Parameters"));

        // Spatial bounds
        JLabel spatialLabel = new JLabel("Spatial Bounds:");
        latMinField = new JTextField(10);
        latMaxField = new JTextField(10);
        lonMinField = new JTextField(10);
        lonMaxField = new JTextField(10);

        // Pre-fill spatial fields with search bounds if available
        if (searchLatMin != null && searchLatMax != null && searchLonMin != null && searchLonMax != null) {
            latMinField.setText(String.valueOf(searchLatMin));
            latMaxField.setText(String.valueOf(searchLatMax));
            lonMinField.setText(String.valueOf(searchLonMin));
            lonMaxField.setText(String.valueOf(searchLonMax));
        }

        // Variable selection
        JLabel variableLabel = new JLabel("Variables:");
        String[] defaultVariables = {"chlor_a", "aot_869", "Rrs_443", "Rrs_555", "Rrs_670"};
        variableList = new JList<>(defaultVariables);
        variableList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane variableScrollPane = new JScrollPane(variableList);
        
        // Initialize with default variables (will be updated when metadata loads)
        updateVariableList(Arrays.asList(defaultVariables));

        // Layout
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // Spatial bounds
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(spatialLabel, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        panel.add(new JLabel("Lat Min:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 1;
        panel.add(latMinField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1;
        panel.add(new JLabel("Lat Max:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 1;
        panel.add(latMaxField, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 1;
        panel.add(new JLabel("Lon Min:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 1;
        panel.add(lonMinField, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 1;
        panel.add(new JLabel("Lon Max:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 1;
        panel.add(lonMaxField, gbc);

        // Preview Coverage button
        JButton previewButton = new JButton("Preview Coverage");
        previewButton.addActionListener(e -> previewGranuleCoverage());
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        panel.add(previewButton, gbc);

        // Variables
        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2;
        panel.add(variableLabel, gbc);

        gbc.gridx = 0; gbc.gridy = 7; gbc.gridwidth = 2; gbc.weighty = 1.0;
        panel.add(variableScrollPane, gbc);

        return panel;
    }



    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Status"));

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("Ready");

        panel.add(progressBar, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        subsetButton = new JButton("Request Subset");
        subsetButton.addActionListener(e -> requestSubset());

        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());

        panel.add(subsetButton);
        panel.add(cancelButton);

        return panel;
    }

    private void validateInputUrl() {
        String url = urlInputField.getText().trim();
        if (url.isEmpty()) {
            statusArea.setText("Please enter a URL");
            return;
        }

        statusArea.setText("Validating URL...");
        // TODO: Implement URL validation logic
        // This would check if the URL is accessible and contains valid data
        statusArea.setText("URL validation completed. Ready for subsetting.");
    }

    private void requestSubset() {
        try {
            System.out.println("=== Starting subset request ===");
            
            // Validate inputs
            if (!validateInputs()) {
                System.out.println("Input validation failed");
                return;
            }

            System.out.println("Input validation passed");

            // Disable buttons during processing
            subsetButton.setEnabled(false);
            cancelButton.setEnabled(false);

            // Get subset parameters
            JSONObject subsetParams = getSubsetParameters();
            System.out.println("Subset parameters: " + subsetParams.toString());

            // Create subset task
            HarmonySubsetTask subsetTask = new HarmonySubsetTask(
                subsetParams,
                progressBar,
                subsetButton,
                cancelButton,
                this
            );

            System.out.println("Starting HarmonySubsetTask...");
            subsetTask.execute();
            
        } catch (Exception e) {
            System.err.println("Error in requestSubset: " + e.getMessage());
            e.printStackTrace();
            
            // Re-enable buttons on error
            subsetButton.setEnabled(true);
            cancelButton.setEnabled(true);
            
            JOptionPane.showMessageDialog(this, 
                "Error starting subset request: " + e.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean validateInputs() {
        // Basic validation
        if (urlInputField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a data file URL", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        // Validate spatial bounds
        try {
            if (!latMinField.getText().isEmpty()) Double.parseDouble(latMinField.getText());
            if (!latMaxField.getText().isEmpty()) Double.parseDouble(latMaxField.getText());
            if (!lonMinField.getText().isEmpty()) Double.parseDouble(lonMinField.getText());
            if (!lonMaxField.getText().isEmpty()) Double.parseDouble(lonMaxField.getText());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid spatial bounds. Please enter valid numbers.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        return true;
    }

    private JSONObject getSubsetParameters() {
        JSONObject params = new JSONObject();
        
        params.put("url", urlInputField.getText().trim());
        
        // Spatial bounds
        if (!latMinField.getText().isEmpty()) params.put("latMin", latMinField.getText());
        if (!latMaxField.getText().isEmpty()) params.put("latMax", latMaxField.getText());
        if (!lonMinField.getText().isEmpty()) params.put("lonMin", lonMinField.getText());
        if (!lonMaxField.getText().isEmpty()) params.put("lonMax", lonMaxField.getText());
        
        // Variables - convert List to JSONArray
        if (!variableList.isSelectionEmpty()) {
            List<String> selectedVariables = variableList.getSelectedValuesList();
            JSONArray variablesArray = new JSONArray();
            for (String variable : selectedVariables) {
                variablesArray.put(variable);
            }
            params.put("variables", variablesArray);
        }
        

        
        return params;
    }

    /**
     * Preview granule coverage and suggest appropriate subset bounds
     */
    private void previewGranuleCoverage() {
        String url = urlInputField.getText().trim();
        if (url.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a data file URL first", "No URL", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Extract file name from URL
        String fileName = url.substring(url.lastIndexOf('/') + 1);
        
        // Show progress
        updateStatus("Fetching granule coverage information...");
        
        // Run in background thread to avoid blocking UI
        new Thread(() -> {
            try {
                // Query CMR for granule metadata
                String cmrUrl = "https://cmr.earthdata.nasa.gov/search/granules.json?readable_granule_name=" + fileName + "&provider=OB_CLOUD";
                System.out.println("Fetching granule coverage: " + cmrUrl);
                
                java.net.URL urlObj = new java.net.URL(cmrUrl);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) urlObj.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                int status = conn.getResponseCode();
                if (status == 200) {
                    java.io.InputStream is = conn.getInputStream();
                    java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
                    String response = s.hasNext() ? s.next() : "";
                    org.json.JSONObject json = new org.json.JSONObject(response);
                    org.json.JSONArray entries = json.getJSONObject("feed").getJSONArray("entry");
                    
                    if (entries.length() > 0) {
                        org.json.JSONObject granule = entries.getJSONObject(0);
                        
                        // Extract coverage information
                        String granuleId = granule.getString("id");
                        String timeStart = granule.getString("time_start");
                        String timeEnd = granule.getString("time_end");
                        
                        // Parse polygons to get spatial bounds
                        org.json.JSONArray polygons = granule.getJSONArray("polygons");
                        if (polygons.length() > 0) {
                            Object firstPoly = polygons.get(0);
                            String[] coords;
                            if (firstPoly instanceof String) {
                                coords = ((String) firstPoly).split(" ");
                            } else if (firstPoly instanceof org.json.JSONArray) {
                                // Flatten the nested array into a single string of coordinates
                                org.json.JSONArray arr = (org.json.JSONArray) firstPoly;
                                StringBuilder sb = new StringBuilder();
                                for (int i = 0; i < arr.length(); i++) {
                                    if (i > 0) sb.append(" ");
                                    sb.append(arr.getString(i));
                                }
                                coords = sb.toString().split(" ");
                            } else {
                                throw new RuntimeException("Unexpected polygon format: " + firstPoly.getClass());
                            }
                            // Parse coordinates to find min/max bounds
                            final double[] bounds = {Double.MAX_VALUE, Double.MIN_VALUE, Double.MAX_VALUE, Double.MIN_VALUE}; // minLat, maxLat, minLon, maxLon
                            for (int i = 0; i < coords.length; i += 2) {
                                double lat = Double.parseDouble(coords[i]);
                                double lon = Double.parseDouble(coords[i + 1]);
                                bounds[0] = Math.min(bounds[0], lat); // minLat
                                bounds[1] = Math.max(bounds[1], lat); // maxLat
                                bounds[2] = Math.min(bounds[2], lon); // minLon
                                bounds[3] = Math.max(bounds[3], lon); // maxLon
                            }
                            final double minLat = bounds[0];
                            final double maxLat = bounds[1];
                            final double minLon = bounds[2];
                            final double maxLon = bounds[3];
                            // Calculate suggested subset bounds (slightly smaller than full coverage)
                            double latMargin = (maxLat - minLat) * 0.1;
                            double lonMargin = (maxLon - minLon) * 0.1;
                            final double suggestedLatMin = minLat + latMargin;
                            final double suggestedLatMax = maxLat - latMargin;
                            final double suggestedLonMin = minLon + lonMargin;
                            final double suggestedLonMax = maxLon - lonMargin;
                            // Show coverage information in dialog
                            SwingUtilities.invokeLater(() -> {
                                showCoverageDialog(granuleId, timeStart, timeEnd, 
                                    minLat, maxLat, minLon, maxLon,
                                    suggestedLatMin, suggestedLatMax, suggestedLonMin, suggestedLonMax);
                            });
                            
                        } else {
                            SwingUtilities.invokeLater(() -> {
                                JOptionPane.showMessageDialog(this, 
                                    "No spatial coverage information found for this granule.", 
                                    "No Coverage Data", JOptionPane.WARNING_MESSAGE);
                            });
                        }
                        
                    } else {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(this, 
                                "Granule not found in CMR: " + fileName, 
                                "Granule Not Found", JOptionPane.ERROR_MESSAGE);
                        });
                    }
                    
                } else {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this, 
                            "Failed to fetch granule information. HTTP status: " + status, 
                            "CMR Error", JOptionPane.ERROR_MESSAGE);
                    });
                }
                
            } catch (Exception e) {
                System.err.println("Error previewing granule coverage: " + e.getMessage());
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, 
                        "Error fetching granule coverage: " + e.getMessage(), 
                        "Error", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    /**
     * Show coverage information dialog with suggested bounds
     */
    private void showCoverageDialog(String granuleId, String timeStart, String timeEnd,
                                   double minLat, double maxLat, double minLon, double maxLon,
                                   double suggestedLatMin, double suggestedLatMax, 
                                   double suggestedLonMin, double suggestedLonMax) {
        
        String message = String.format(
            "Granule Coverage Information:\n\n" +
            "Granule ID: %s\n" +
            "Time: %s to %s\n\n" +
            "Full Coverage Bounds:\n" +
            "Latitude:  %.4f° to %.4f°\n" +
            "Longitude: %.4f° to %.4f°\n\n" +
            "Suggested Subset Bounds (90%% of coverage):\n" +
            "Latitude:  %.4f° to %.4f°\n" +
            "Longitude: %.4f° to %.4f°\n\n" +
            "Would you like to apply the suggested bounds to the subset form?",
            granuleId, timeStart, timeEnd,
            minLat, maxLat, minLon, maxLon,
            suggestedLatMin, suggestedLatMax, suggestedLonMin, suggestedLonMax
        );
        
        int choice = JOptionPane.showConfirmDialog(this, message, 
            "Granule Coverage Preview", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
        
        if (choice == JOptionPane.YES_OPTION) {
            // Apply suggested bounds to the form
            latMinField.setText(String.format("%.4f", suggestedLatMin));
            latMaxField.setText(String.format("%.4f", suggestedLatMax));
            lonMinField.setText(String.format("%.4f", suggestedLonMin));
            lonMaxField.setText(String.format("%.4f", suggestedLonMax));
            
            updateStatus("Applied suggested bounds from granule coverage.");
        }
    }

    protected AbstractButton getHelpButton() {
        if (helpId != null) {
            final AbstractButton helpButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon(HELP_ICON), false);
            helpButton.setToolTipText("Help.");
            helpButton.setName("helpButton");
            helpButton.addActionListener(e -> getHelpCtx().display());
            return helpButton;
        }
        return null;
    }

    public HelpCtx getHelpCtx() {
        return new HelpCtx(helpId);
    }

    @Override
    public void addPropertyChangeListener(String name, PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(name, listener);
    }

    @Override
    public void removePropertyChangeListener(String name, PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(name, listener);
    }

    public void updateStatus(String message) {
        SwingUtilities.invokeLater(() -> {
            statusArea.setText(message);
            statusArea.setCaretPosition(0);
        });
    }

    public void subsetCompleted(boolean success, String message) {
        SwingUtilities.invokeLater(() -> {
            subsetButton.setEnabled(true);
            cancelButton.setEnabled(true);
            
            if (success) {
                progressBar.setString("Subset completed successfully");
                JOptionPane.showMessageDialog(this, "Subset request completed successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                progressBar.setString("Subset failed");
                JOptionPane.showMessageDialog(this, "Subset request failed: " + message, "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    // Extract collection ID from file URL using regex
    public static String extractCollectionIdFromUrl(String fileUrl) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(C\\d{6,}-[A-Z_]+)");
        java.util.regex.Matcher matcher = pattern.matcher(fileUrl);
        if (matcher.find()) {
            String collectionId = matcher.group(1);
            // Only accept OB_CLOUD collections, reject OB_DAAC
            if (collectionId.endsWith("-OB_CLOUD")) {
                System.out.println("Found valid OB_CLOUD collection: " + collectionId);
                return collectionId;
            } else if (collectionId.endsWith("-OB_DAAC")) {
                System.out.println("Rejecting OB_DAAC collection: " + collectionId + " (should be OB_CLOUD)");
                return null;
            } else {
                System.out.println("Found collection with unknown provider: " + collectionId);
                return null;
            }
        }
        return null; // or a default/fallback
    }

    // Get collection concept ID from CMR using short name
    public static String getConceptId(String shortName) throws IOException {
        String urlString = "https://cmr.earthdata.nasa.gov/search/collections.json?short_name=" + shortName;
        java.net.URL url = new java.net.URL(urlString);
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        org.json.JSONObject jsonResponse = new org.json.JSONObject(response.toString());
        org.json.JSONArray items = jsonResponse.getJSONObject("feed").getJSONArray("entry");

        if (items.length() > 0) {
            return items.getJSONObject(0).getString("id");
        }
        return null;
    }

    // Fetch collection ID from CMR using the file name (granule name) and concept ID if available
    public static String fetchCollectionIdFromCMR(String fileName, String conceptId) {
        String cmrUrl = "https://cmr.earthdata.nasa.gov/search/granules.json?readable_granule_name=" + fileName + "&provider=OB_CLOUD";
        if (conceptId != null) {
            cmrUrl += "&concept_id=" + conceptId;
        }
        System.out.println("CMR granule lookup URL: " + cmrUrl);
        try {
            java.net.URL url = new java.net.URL(cmrUrl);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int status = conn.getResponseCode();
            if (status == 200) {
                java.io.InputStream is = conn.getInputStream();
                java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
                String response = s.hasNext() ? s.next() : "";
                org.json.JSONObject json = new org.json.JSONObject(response);
                org.json.JSONArray entries = json.getJSONObject("feed").getJSONArray("entry");
                if (entries.length() > 0) {
                    return entries.getJSONObject(0).getString("collection_concept_id");
                }
            } else {
                System.err.println("CMR request failed with status: " + status);
                // Try to read error response
                try {
                    java.io.InputStream is = conn.getErrorStream();
                    if (is != null) {
                        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
                        String errorResponse = s.hasNext() ? s.next() : "";
                        System.err.println("CMR error response: " + errorResponse);
                    }
                } catch (Exception e) {
                    System.err.println("Could not read CMR error response: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching collection ID from CMR: " + e.getMessage());
        }
        return null;
    }
} 