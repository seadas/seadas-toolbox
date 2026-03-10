package gov.nasa.gsfc.seadas.earthdatacloud.ui;

import gov.nasa.gsfc.seadas.earthdatacloud.action.HarmonySubsetTask;
import gov.nasa.gsfc.seadas.earthdatacloud.data.VariableMetadataFetcher;
import gov.nasa.gsfc.seadas.earthdatacloud.data.FileVariableMetadataFetcher;
import gov.nasa.gsfc.seadas.earthdatacloud.data.CmrGranuleMetadataFetcher;
import gov.nasa.gsfc.seadas.earthdatacloud.util.GeoMapper;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.ui.UIUtils;
import org.esa.snap.ui.tool.ToolButtonFactory;
import org.json.JSONObject;
import org.json.JSONArray;
import org.openide.util.HelpCtx;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import javax.swing.event.SwingPropertyChangeSupport;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;

import java.net.URL;
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
    private Double searchLatMin, searchLatMax, searchLonMin, searchLonMax;
    private String cmrGranuleId;                 // optional, may be null
    private Double granuleLatMin, granuleLatMax; // optional
    private Double granuleLonMin, granuleLonMax; // optional

    private CmrGranuleMetadataFetcher.GranuleMeta meta;
    private String variablesLoadedForUrl;

    public HarmonySubsetServiceDialog() {
        this(null, null, null, null, null);
    }

    public HarmonySubsetServiceDialog(Window owner) {
        this(null, null, null, null, null);
        setLocationRelativeTo(owner);
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

    public void setGranuleId(String granuleId) {
        this.cmrGranuleId = granuleId;
    }

    public void setGranuleBounds(double latMin, double latMax, double lonMin, double lonMax) {
        this.granuleLatMin = Math.min(latMin, latMax);
        this.granuleLatMax = Math.max(latMin, latMax);
        this.granuleLonMin = Math.min(lonMin, lonMax);
        this.granuleLonMax = Math.max(lonMin, lonMax);
    }

    private synchronized void fetchFileMetadata() {
        if (selectedFileUrl == null || selectedFileUrl.isBlank()) {
            return;
        }
        if (selectedFileUrl.equals(variablesLoadedForUrl)) {
            return;
        }
        variablesLoadedForUrl = selectedFileUrl;

        new Thread(() -> {
            try {
                System.out.println("=== Starting variable detection for: " + selectedFileUrl);
                updateStatus("Detecting available variables from file metadata...");

                List<String> variables = null;

                try {
                    variables = FileVariableMetadataFetcher.fetchVariablesFromFile(selectedFileUrl);
                    System.out.println("Detected variables from actual file metadata: " + variables);
                } catch (Exception fileEx) {
                    System.out.println("File metadata variable lookup failed: " + fileEx.getMessage());
                }

                if (variables == null || variables.isEmpty()) {
                    variables = extractVariablesFromFileName(selectedFileUrl);
                    System.out.println("Detected variables from filename fallback: " + variables);
                }

                final List<String> finalVariables = variables;
                SwingUtilities.invokeLater(() -> {
                    updateVariableList(finalVariables);
                    updateStatus("Variable detection complete. Found " + finalVariables.size() + " variables.");
                });

            } catch (Exception e) {
                System.out.println("Exception in variable detection: " + e.getMessage());
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    updateStatus("Error detecting variables: " + e.getMessage());
                    updateVariableList(getDefaultVariables());
                });
            }
        }).start();
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


        JButton drawBoxButton = new JButton("Draw Bounding Box...");
        drawBoxButton.addActionListener(e -> openBBoxDialog());
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        panel.add(drawBoxButton, gbc);

        // Preview Coverage button
        JButton previewButton = new JButton("Preview Coverage");
        previewButton.addActionListener(e -> previewGranuleCoverage());
        gbc.gridx = 1; gbc.gridy = 5; gbc.gridwidth = 2;
        panel.add(previewButton, gbc);

        // Variables
        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2;
        panel.add(variableLabel, gbc);

        gbc.gridx = 0; gbc.gridy = 7; gbc.gridwidth = 2; gbc.weighty = 1.0;
        panel.add(variableScrollPane, gbc);

        return panel;
    }

    private void openBBoxDialog() {
        // Prefer granule bounds for extent/clamp
        if (granuleLatMin == null || granuleLatMax == null || granuleLonMin == null || granuleLonMax == null) {
            Double[] bb = CmrGranuleMetadataFetcher.computeBBoxFromPolygons(meta.polygons);
            if (bb != null) {
                granuleLatMin = bb[0];
                granuleLatMax = bb[1];
                granuleLonMin = bb[2];
                granuleLonMax = bb[3];
            }
        }

        if (granuleLatMin == null || granuleLatMax == null || granuleLonMin == null || granuleLonMax == null) {
            JOptionPane.showMessageDialog(this, "Granule bounds not available.", "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            String fileUrl = urlInputField.getText().trim();
            if (fileUrl.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a data file URL first",
                        "No URL", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (searchLatMin == null || searchLatMax == null || searchLonMin == null || searchLonMax == null) {
                JOptionPane.showMessageDialog(this,
                        "Bounds are not available yet.\nClick 'Preview Coverage' first (or enter bounds manually).",
                        "No Bounds", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);

            // Your CMR-based method that returns a browse image URL (png/jpg)
            String previewUrl = getPreviewUrlFromCmr(fileName, null);

            if (previewUrl == null || previewUrl.isBlank()) {
                JOptionPane.showMessageDialog(this,
                        "No preview image URL found for:\n" + fileName,
                        "No Preview", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Load the preview image here (dialog expects BufferedImage)
            BufferedImage previewImage = ImageIO.read(new URL(previewUrl));
            if (previewImage == null) {
                JOptionPane.showMessageDialog(this,
                        "Preview image could not be decoded:\n" + previewUrl,
                        "Preview Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            BBoxSelectionDialog dialog = new BBoxSelectionDialog(
                    SnapApp.getDefault().getMainFrame(),
                    previewImage,
                    granuleLatMin, granuleLatMax,
                    granuleLonMin, granuleLonMax
            );

            dialog.setClampToExtent(true);
            dialog.setFootprintPolygons(meta.polygons);
            dialog.setVisible(true);

            if (dialog.isConfirmed()) {
                latMinField.setText(String.format("%.4f", dialog.getLatMin()));
                latMaxField.setText(String.format("%.4f", dialog.getLatMax()));
                lonMinField.setText(String.format("%.4f", dialog.getLonMin()));
                lonMaxField.setText(String.format("%.4f", dialog.getLonMax()));
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Failed to open bounding box dialog:\n" + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    private String getPreviewUrlFromCmr(String fileName, String collectionConceptId) throws IOException {
        // fileName should be just the granule filename (no path)
        String encodedName = java.net.URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8);

        StringBuilder cmr = new StringBuilder("https://cmr.earthdata.nasa.gov/search/granules.umm_json")
                .append("?provider=OB_CLOUD")
                .append("&page_size=10")
                .append("&readable_granule_name=").append(encodedName);

        // Strongly recommended to disambiguate (avoid wrong entry[0])
        if (collectionConceptId != null && !collectionConceptId.isBlank()) {
            cmr.append("&collection_concept_id=")
                    .append(java.net.URLEncoder.encode(collectionConceptId, java.nio.charset.StandardCharsets.UTF_8));
        }

        java.net.URL urlObj = new java.net.URL(cmr.toString());
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) urlObj.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(30_000);

        int status = conn.getResponseCode();
        if (status >= 400) {
            throw new IOException("CMR preview lookup failed: HTTP " + status + " for " + cmr);
        }

        String response;
        try (java.io.InputStream is = conn.getInputStream()) {
            java.util.Scanner s = new java.util.Scanner(is, java.nio.charset.StandardCharsets.UTF_8).useDelimiter("\\A");
            response = s.hasNext() ? s.next() : "";
        }

        org.json.JSONObject json = new org.json.JSONObject(response);
        org.json.JSONArray items = json.optJSONArray("items");
        if (items == null || items.isEmpty()) {
            return null;
        }

        // Prefer exact filename match if possible
        org.json.JSONObject best = null;
        for (int i = 0; i < items.length(); i++) {
            org.json.JSONObject item = items.getJSONObject(i);
            org.json.JSONObject umm = item.optJSONObject("umm");
            if (umm == null) continue;

            // CMR UMM sometimes uses GranuleUR, sometimes additional fields; try a few
            String granuleUr = umm.optString("GranuleUR", "");
            if (fileName.equals(granuleUr)) {
                best = item;
                break;
            }
            // Some collections don't populate GranuleUR; fall back later
            if (best == null) best = item;
        }

        if (best == null) return null;

        org.json.JSONObject umm = best.optJSONObject("umm");
        if (umm == null) return null;

        org.json.JSONArray related = umm.optJSONArray("RelatedUrls");
        if (related == null) return null;

        // Heuristic ranking: prefer true image links and visualization/browse types
        String candidate = null;
        for (int i = 0; i < related.length(); i++) {
            org.json.JSONObject r = related.getJSONObject(i);

            String type = r.optString("Type", "").trim();
            String subtype = r.optString("Subtype", "").trim();
            String url = r.optString("URL", "").trim();

            if (url.isEmpty()) continue;

            // Normalize (remove whitespace/newlines)
            url = url.replace(" ", "%20");

            // Must be absolute, otherwise Java URL() will fail in your bbox dialog
            if (!(url.startsWith("http://") || url.startsWith("https://"))) {
                // If you *know* some URLs are relative, you can resolve them here:
                // url = new java.net.URL(new java.net.URL("https://oceandata.sci.gsfc.nasa.gov/"), url).toString();
                continue;
            }

            String urlLower = url.toLowerCase();
            boolean looksLikeImage = urlLower.endsWith(".png") || urlLower.endsWith(".jpg") || urlLower.endsWith(".jpeg");
            boolean isVisualization =
                    type.equalsIgnoreCase("GET RELATED VISUALIZATION") ||
                            type.toUpperCase().contains("VISUALIZATION") ||
                            type.toUpperCase().contains("BROWSE") ||
                            subtype.toUpperCase().contains("BROWSE") ||
                            subtype.toUpperCase().contains("THUMBNAIL");

            // Strong preference: visualization + actual image extension
            if (isVisualization && looksLikeImage) {
                return url;
            }

            // Next best: any image-looking URL
            if (candidate == null && looksLikeImage) {
                candidate = url;
            }

            // Last resort: visualization link even if extension unknown
            if (candidate == null && isVisualization) {
                candidate = url;
            }
        }

        return candidate;
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
            subsetTask.addPropertyChangeListener(evt -> {
                if ("progress".equals(evt.getPropertyName())) {
                    int p = (Integer) evt.getNewValue();
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(p);
                    progressBar.setString(p + "%");
                }
            });
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

        if (meta != null) {
            params.put("granuleId", meta.granuleId);
            params.put("collectionId", meta.collectionConceptId);
        }

        if (!latMinField.getText().isEmpty())
            params.put("latMin", latMinField.getText());

        if (!latMaxField.getText().isEmpty())
            params.put("latMax", latMaxField.getText());

        if (!lonMinField.getText().isEmpty())
            params.put("lonMin", lonMinField.getText());

        if (!lonMaxField.getText().isEmpty())
            params.put("lonMax", lonMaxField.getText());

        if (!variableList.isSelectionEmpty()) {
            JSONArray variables = new JSONArray(variableList.getSelectedValuesList());
            params.put("variables", variables);
        }
        params.put("allVariablesSelected",
                variableList.getSelectedIndices().length == variableList.getModel().getSize());

//        System.out.println("UL lat/lon: " + ulLat + ", " + ulLon);
//        System.out.println("UR lat/lon: " + urLat + ", " + urLon);
//        System.out.println("LR lat/lon: " + lrLat + ", " + lrLon);
//        System.out.println("LL lat/lon: " + llLat + ", " + llLon);
//        System.out.println("bbox minLon,minLat,maxLon,maxLat: "
//                + lonMinField.getText() + "," + latMinField.getText() + "," + lonMaxField.getText() + "," + latMaxField.getText());
//        System.out.println("polygon: " + polygonWkt);

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
    private GeoMapper createSnapMapper(BufferedImage image, double minLat, double maxLat, double minLon, double maxLon) {
        return (x, y, w, h) -> {
            // Map panel pixel to image percentage
            double fX = (double) x / w;
            double fY = (double) y / h;

            // Map percentage to the GEOGRAPHIC extent provided by CMR
            double lon = minLon + (fX * (maxLon - minLon));
            double lat = maxLat - (fY * (maxLat - minLat)); // Inverted Y

            return new double[]{lat, lon};
        };
    }

    public CmrGranuleMetadataFetcher.GranuleMeta getMeta() {
        return meta;
    }

    public void setMeta(CmrGranuleMetadataFetcher.GranuleMeta meta) {
        this.meta = meta;
        if (selectedFileUrl != null) {
            fetchFileMetadata();
        }
    }
}