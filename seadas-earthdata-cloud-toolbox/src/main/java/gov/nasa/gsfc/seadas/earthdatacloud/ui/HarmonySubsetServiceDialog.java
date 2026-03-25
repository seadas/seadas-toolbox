package gov.nasa.gsfc.seadas.earthdatacloud.ui;

import gov.nasa.gsfc.seadas.earthdatacloud.action.HarmonySubsetTask;
import gov.nasa.gsfc.seadas.earthdatacloud.data.CmrVariableMetadataFetcher;
import gov.nasa.gsfc.seadas.earthdatacloud.data.FileVariableMetadataFetcher;
import gov.nasa.gsfc.seadas.earthdatacloud.data.CmrGranuleMetadataFetcher;
import gov.nasa.gsfc.seadas.earthdatacloud.data.VariableItem;
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

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.io.IOException;
import java.io.File;
import java.util.prefs.Preferences;

public class HarmonySubsetServiceDialog extends JDialog {

    public static final String TITLE = "Harmony Subset Service";
    private SwingPropertyChangeSupport propertyChangeSupport;
    private Component helpButton = null;
    private final static String helpId = "subsetServiceHelp";
    private final static String HELP_ICON = "icons/Help24.gif";

    // UI Components
    private JTextField urlInputField;
    private JTextField latMinField, latMaxField, lonMinField, lonMaxField;
    private JList<VariableItem> variableList;
    private JProgressBar progressBar;
    private JButton subsetButton, cancelButton, doneButton;
    private HarmonySubsetTask currentSubsetTask;
    private JTextArea statusArea;

    // Data
    private String selectedFileUrl;
    private Double searchLatMin, searchLatMax, searchLonMin, searchLonMax;
    private String cmrGranuleId;                 // optional, may be null
    private Double granuleLatMin, granuleLatMax; // optional
    private Double granuleLonMin, granuleLonMax; // optional

    private CmrGranuleMetadataFetcher.GranuleMeta meta;
    private String variablesLoadedForUrl;
    private File selectedOutputFile;
    private static final String PREF_LAST_DOWNLOAD_DIR = "harmonySubsetLastDownloadDir";
    private final Preferences prefs = Preferences.userNodeForPackage(HarmonySubsetServiceDialog.class);
    public HarmonySubsetServiceDialog() {
        this(null, null, null, null, null);
    }

    public HarmonySubsetServiceDialog(String fileUrl, Double latMin, Double latMax, Double lonMin, Double lonMax) {
        super(SnapApp.getDefault().getMainFrame(), TITLE, JDialog.DEFAULT_MODALITY_TYPE);
        this.selectedFileUrl = fileUrl;
        this.searchLatMin = latMin;
        this.searchLatMax = latMax;
        this.searchLonMin = lonMin;
        this.searchLonMax = lonMax;

        setLayout(new BorderLayout());

        propertyChangeSupport = new SwingPropertyChangeSupport(this);

        helpButton = getHelpButton();

        // Create main content
        JPanel mainPanel = createMainPanel();
        add(mainPanel, BorderLayout.CENTER);

        // Create button panel
        JPanel buttonPanel = createButtonPanel();
        add(buttonPanel, BorderLayout.SOUTH);

        // Fetch file metadata to get available variables
        if (selectedFileUrl != null) {
            loadVariablesForSelectedGranule();
        }

        pack();

        // Optional: prevent the dialog from becoming too small
        setMinimumSize(getSize());

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

    private String variablesLoadedForCollectionId;

    private synchronized void loadVariablesForSelectedGranule() {
        if (meta == null || meta.collectionConceptId == null || meta.collectionConceptId.isBlank()) {
            SwingUtilities.invokeLater(() -> {
                updateVariableList(new ArrayList<>());
                updateStatus("No collection metadata available for variable detection.");
                variableList.setEnabled(true);
            });
            return;
        }

        String collectionId = meta.collectionConceptId;

        // If already loaded for this collection in the current UI state, do nothing.
        if (collectionId.equals(variablesLoadedForCollectionId)) {
            return;
        }

        updateStatus("Detecting available variables from file metadata...");
        variableList.setEnabled(false);

        new Thread(() -> {
            List<VariableItem> variables = new ArrayList<>();

            try {
                System.out.println("=== Starting variable detection for collection: " + collectionId);

                variables = FileVariableMetadataFetcher.fetchVariablesFromFile(selectedFileUrl);

                if (variables == null) {
                    variables = new ArrayList<>();
                }

                System.out.println("Detected variables from file metadata: " + variables);

                // Mark as loaded only after fetch completes successfully.
                variablesLoadedForCollectionId = collectionId;

            } catch (Exception ex) {
                System.out.println("Variable detection failed: " + ex.getMessage());
                ex.printStackTrace();
            }

            final List<VariableItem> finalVariables = new ArrayList<>(variables);
            SwingUtilities.invokeLater(() -> {
                updateVariableList(finalVariables);
                variableList.setEnabled(true);

                if (finalVariables.isEmpty()) {
                    updateStatus("No variables detected. Subsetting will use geographic bounds only.");
                } else {
                    updateStatus("Variable detection complete. Found " + finalVariables.size() + " variables.");
                }
            });
        }, "variable-loader").start();
    }

    private String getVariableCacheKey(CmrGranuleMetadataFetcher.GranuleMeta granuleMeta, String fileName) {
        if (granuleMeta != null && granuleMeta.collectionConceptId != null
                && !granuleMeta.collectionConceptId.isBlank()) {
            return granuleMeta.collectionConceptId;
        }
        return fileName != null ? fileName : "unknown";
    }
    private void loadVariablesForGranule() {
        DefaultListModel<VariableItem> model = (DefaultListModel<VariableItem>) variableList.getModel();
        model.clear();
        variableList.setEnabled(false);

        SwingWorker<List<VariableItem>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<VariableItem> doInBackground() throws Exception {
                return CmrVariableMetadataFetcher.fetchVariablesFromCollection(
                        meta.collectionConceptId);
            }

            @Override
            protected void done() {
                try {
                    List<VariableItem> vars = get();
                    updateVariableList(vars);
                } catch (Exception e) {
                    e.printStackTrace();
                    model.clear();
                } finally {
                    variableList.setEnabled(true);
                }
            }
        };

        worker.execute();
    }

    private void updateVariableList(List<VariableItem> variables) {
        DefaultListModel<VariableItem> model = (DefaultListModel<VariableItem>) variableList.getModel();
        model.clear();

        for (VariableItem v : variables) {
            model.addElement(v);
        }

        if (!variables.isEmpty()) {
            variableList.setSelectionInterval(0, variables.size() - 1);
        } else {
            variableList.clearSelection();
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
        JPanel outerPanel = new JPanel(new GridBagLayout());
        outerPanel.setBorder(BorderFactory.createTitledBorder("Subset Parameters"));

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);

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

        DefaultListModel<VariableItem> variableModel = new DefaultListModel<>();
        variableList = new JList<>(variableModel);
        variableList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        variableList.setEnabled(false);
        updateStatus("Loading variables...");
        updateVariableList(new ArrayList<>());

        JScrollPane variableScrollPane = new JScrollPane(variableList);
        variableScrollPane.setPreferredSize(new Dimension(520, 180));

        // Start empty; real values will be loaded from file metadata.
        updateVariableList(new ArrayList<>());

        JButton previewButton = new JButton("Preview Coverage");
        previewButton.addActionListener(e -> previewGranuleCoverage());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(previewButton);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.anchor = GridBagConstraints.CENTER;

        int row = 0;

        // Spatial label
        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(spatialLabel, gbc);

        // Lat Min
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Lat Min:"), gbc);

        gbc.gridx = 1;
        panel.add(latMinField, gbc);
        row++;

        // Lat Max
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Lat Max:"), gbc);

        gbc.gridx = 1;
        panel.add(latMaxField, gbc);
        row++;

        // Lon Min
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Lon Min:"), gbc);

        gbc.gridx = 1;
        panel.add(lonMinField, gbc);
        row++;

        // Lon Max
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Lon Max:"), gbc);

        gbc.gridx = 1;
        panel.add(lonMaxField, gbc);
        row++;

        // Preview button
        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(buttonPanel, gbc);

        // Variables label
        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 8, 6, 8);
        panel.add(variableLabel, gbc);

        // Variable list
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(variableScrollPane, gbc);

        // Center the compact content panel inside the full tab area
        GridBagConstraints outerGbc = new GridBagConstraints();
        outerGbc.gridx = 0;
        outerGbc.gridy = 0;
        outerGbc.weightx = 1.0;
        outerGbc.weighty = 1.0;
        outerGbc.anchor = GridBagConstraints.CENTER;
        outerGbc.fill = GridBagConstraints.NONE;

        outerPanel.add(panel, outerGbc);

        return outerPanel;
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
            dialog.setVisible(true);
            dialog.setFootprintPolygons(meta.polygons);

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
        cancelButton.addActionListener(e -> {
            if (currentSubsetTask != null && !currentSubsetTask.isDone()) {
                currentSubsetTask.cancel(true);
                updateStatus("Subset request cancelled.");
                subsetButton.setEnabled(true);
                cancelButton.setEnabled(false);
                doneButton.setEnabled(true);
                progressBar.setIndeterminate(false);
                progressBar.setString("Cancelled");
            } else {
                dispose();
            }
        });

        doneButton = new JButton("Done");
        doneButton.setEnabled(false);
        doneButton.addActionListener(e -> dispose());

        panel.add(subsetButton);
        panel.add(cancelButton);
        panel.add(doneButton);

        return panel;
    }

    public void onSubsetStarted() {
        subsetButton.setEnabled(false);
        cancelButton.setEnabled(true);
        doneButton.setEnabled(false);
    }

    public void onSubsetSucceeded() {
        subsetButton.setEnabled(true);
        cancelButton.setEnabled(false);
        doneButton.setEnabled(true);
        getRootPane().setDefaultButton(doneButton);
    }

    public void onSubsetFailed() {
        subsetButton.setEnabled(true);
        cancelButton.setEnabled(false);
        doneButton.setEnabled(true);
        getRootPane().setDefaultButton(doneButton);
    }

    public void onSubsetCancelled() {
        subsetButton.setEnabled(true);
        cancelButton.setEnabled(false);
        doneButton.setEnabled(true);
        getRootPane().setDefaultButton(doneButton);
    }

    private void validateInputUrl() {
        String url = urlInputField.getText().trim();
        if (url.isEmpty()) {
            statusArea.setText("Please enter a URL");
            return;
        }

        selectedFileUrl = url;
        variablesLoadedForUrl = null;
        fetchFileMetadata();

        statusArea.setText("URL validation completed. Ready for subsetting.");
    }

    private synchronized void fetchFileMetadata() {
        if (selectedFileUrl == null || selectedFileUrl.isBlank()) {
            return;
        }

        if (selectedFileUrl.equals(variablesLoadedForUrl)) {
            return;
        }

        variableList.setEnabled(false);
        updateStatus("Detecting available variables from file metadata...");

        final String fileUrl = selectedFileUrl;

        new Thread(() -> {
            List<VariableItem> variables = new ArrayList<>();

            try {
                System.out.println("=== Starting variable detection for: " + fileUrl);

                List<VariableItem> detected = FileVariableMetadataFetcher.fetchVariablesFromFile(fileUrl);
                if (detected != null) {
                    variables = detected;
                }

                System.out.println("Detected variables from actual file metadata: " + variables);

                variablesLoadedForUrl = fileUrl;

            } catch (Exception e) {
                System.out.println("Exception in variable detection: " + e.getMessage());
                e.printStackTrace();
            }

            final List<VariableItem> finalVariables = new ArrayList<>(variables);
            SwingUtilities.invokeLater(() -> {
                updateVariableList(finalVariables);
                variableList.setEnabled(true);

                if (finalVariables.isEmpty()) {
                    updateStatus("No variables detected. Subsetting will use geographic bounds only.");
                } else {
                    updateStatus("Variable detection complete. Found " + finalVariables.size() + " variables.");
                }
            });
        }, "file-metadata-loader").start();
    }

    private void requestSubset() {
        try {
            System.out.println("=== Starting subset request ===");

            if (!validateInputs()) {
                System.out.println("Input validation failed");
                return;
            }

            System.out.println("Input validation passed");

            // Ask user where to save result
            File outputFile = promptForOutputFile();
            if (outputFile == null) {
                System.out.println("Subset request cancelled by user at file save prompt.");
                updateStatus("Subset request cancelled.");
                return;
            }

            selectedOutputFile = outputFile;
            System.out.println("Selected output file: " + outputFile.getAbsolutePath());

            // Disable buttons during processing
            subsetButton.setEnabled(false);
            cancelButton.setEnabled(false);

            // Get subset parameters
            JSONObject subsetParams = getSubsetParameters();
            subsetParams.put("outputFile", outputFile.getAbsolutePath());

            System.out.println("Subset parameters: " + subsetParams.toString());

            currentSubsetTask = new HarmonySubsetTask(
                    subsetParams,
                    progressBar,
                    subsetButton,
                    cancelButton,
                    this
            );

            onSubsetStarted();

            System.out.println("Starting HarmonySubsetTask...");
            currentSubsetTask.addPropertyChangeListener(evt -> {
                if ("progress".equals(evt.getPropertyName())) {
                    int p = (Integer) evt.getNewValue();
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(p);
                    progressBar.setString(p + "%");
                }
            });
            currentSubsetTask.execute();

        } catch (Exception e) {
            System.err.println("Error in requestSubset: " + e.getMessage());
            e.printStackTrace();

            onSubsetFailed();

            JOptionPane.showMessageDialog(
                    this,
                    "Error starting subset request: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private File promptForOutputFile() {
        String suggestedName = buildSuggestedSubsetFilename();

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Subset Result");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setSelectedFile(new File(suggestedName));

        String lastDir = prefs.get(PREF_LAST_DOWNLOAD_DIR, null);
        if (lastDir != null && !lastDir.isBlank()) {
            File dir = new File(lastDir);
            if (dir.exists() && dir.isDirectory()) {
                chooser.setCurrentDirectory(dir);
            }
        }

        int result = chooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return null;
        }

        File file = chooser.getSelectedFile();
        if (file == null) {
            return null;
        }

        if (!file.getName().toLowerCase().endsWith(".nc")) {
            file = new File(file.getParentFile(), file.getName() + ".nc");
        }

        if (file.exists()) {
            int overwrite = JOptionPane.showConfirmDialog(
                    this,
                    "The selected file already exists.\nDo you want to overwrite it?",
                    "Confirm Overwrite",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (overwrite != JOptionPane.YES_OPTION) {
                return null;
            }
        }

        File parent = file.getParentFile();
        if (parent != null) {
            prefs.put(PREF_LAST_DOWNLOAD_DIR, parent.getAbsolutePath());
        }

        return file;
    }

    private String buildSuggestedSubsetFilename() {
        String baseName = null;

        // 1. Prefer producer granule id
        if (meta != null) {
            if (meta.producerGranuleId != null && !meta.producerGranuleId.isBlank()) {
                baseName = meta.producerGranuleId;
            } else if (meta.granuleId != null && !meta.granuleId.isBlank()) {
                baseName = meta.granuleId;
            }
        }

        // 2. Fallback: derive from URL
        if (baseName == null || baseName.isBlank()) {
            String url = urlInputField.getText().trim();
            if (!url.isEmpty()) {
                int idx = url.lastIndexOf('/');
                baseName = (idx >= 0) ? url.substring(idx + 1) : url;
            }
        }

        // 3. Final fallback
        if (baseName == null || baseName.isBlank()) {
            baseName = "harmony_subset";
        }

        // Remove .nc if already present
        if (baseName.toLowerCase().endsWith(".nc")) {
            baseName = baseName.substring(0, baseName.length() - 3);
        }

        return baseName + "_subset.nc";
    }

    private boolean validateInputs() {
        String url = urlInputField.getText().trim();
        if (url.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please enter a data file URL.",
                    "Validation Error",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }

        String latMin = latMinField.getText().trim();
        String latMax = latMaxField.getText().trim();
        String lonMin = lonMinField.getText().trim();
        String lonMax = lonMaxField.getText().trim();

        boolean anyBbox =
                !latMin.isEmpty() || !latMax.isEmpty() || !lonMin.isEmpty() || !lonMax.isEmpty();

        boolean fullBbox =
                !latMin.isEmpty() && !latMax.isEmpty() && !lonMin.isEmpty() && !lonMax.isEmpty();

        int total = variableList.getModel().getSize();
        int selected = variableList.getSelectedIndices().length;
        boolean allVariablesSelected = total > 0 && selected == total;

        // Reject partial bbox entry
        if (anyBbox && !fullBbox) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please provide all four subset boundary values, or leave all of them blank.",
                    "Incomplete Subset Boundary",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }

        // Block full-granule/all-variables requests through the subset service
        if (!fullBbox && allVariablesSelected) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please enter subset boundaries or select specific variables.\n\n" +
                            "Requesting the full granule with all variables is not allowed through the subset operation.",
                    "Subset Required",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }

        // Validate numeric bbox values only when a full bbox is provided
        if (fullBbox) {
            final double latMinVal;
            final double latMaxVal;
            final double lonMinVal;
            final double lonMaxVal;

            try {
                latMinVal = Double.parseDouble(latMin);
                latMaxVal = Double.parseDouble(latMax);
                lonMinVal = Double.parseDouble(lonMin);
                lonMaxVal = Double.parseDouble(lonMax);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(
                        this,
                        "Invalid spatial bounds. Please enter valid numbers.",
                        "Validation Error",
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }

            if (latMinVal < -90.0 || latMaxVal > 90.0) {
                JOptionPane.showMessageDialog(
                        this,
                        "Latitude values must be between -90 and 90.",
                        "Validation Error",
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }

            if (lonMinVal < -180.0 || lonMaxVal > 180.0) {
                JOptionPane.showMessageDialog(
                        this,
                        "Longitude values must be between -180 and 180.",
                        "Validation Error",
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }

            if (latMinVal >= latMaxVal) {
                JOptionPane.showMessageDialog(
                        this,
                        "Latitude minimum must be less than latitude maximum.",
                        "Validation Error",
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }

            if (lonMinVal >= lonMaxVal) {
                JOptionPane.showMessageDialog(
                        this,
                        "Longitude minimum must be less than longitude maximum.",
                        "Validation Error",
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }
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

        String latMin = latMinField.getText().trim();
        String latMax = latMaxField.getText().trim();
        String lonMin = lonMinField.getText().trim();
        String lonMax = lonMaxField.getText().trim();

        if (!latMin.isEmpty()) {
            params.put("latMin", latMin);
        }
        if (!latMax.isEmpty()) {
            params.put("latMax", latMax);
        }
        if (!lonMin.isEmpty()) {
            params.put("lonMin", lonMin);
        }
        if (!lonMax.isEmpty()) {
            params.put("lonMax", lonMax);
        }

        int total = variableList.getModel().getSize();
        int selected = variableList.getSelectedIndices().length;
        boolean allVariablesSelected = total > 0 && selected == total;

        params.put("allVariablesSelected", allVariablesSelected);

        if (!allVariablesSelected && !variableList.isSelectionEmpty()) {
            JSONArray variables = new JSONArray();
            for (VariableItem item : variableList.getSelectedValuesList()) {
                variables.put(item.fullName);
            }
            params.put("variables", variables);
        }

        return params;
    }

    /**
     * Preview approximate granule coverage from CMR metadata.
     * This is not the exact file navigation boundary.
     */
    private void previewGranuleCoverage() {
        String dataUrl = urlInputField.getText().trim();
        if (dataUrl.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please enter a data file URL first",
                    "No URL",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String fileName = dataUrl.substring(dataUrl.lastIndexOf('/') + 1);
        updateStatus("Fetching approximate granule coverage from CMR...");

        new Thread(() -> {
            HttpURLConnection conn = null;

            try {
                String encodedFileName = java.net.URLEncoder.encode(
                        fileName, java.nio.charset.StandardCharsets.UTF_8.toString());

                String cmrUrl = "https://cmr.earthdata.nasa.gov/search/granules.json"
                        + "?readable_granule_name=" + encodedFileName
                        + "&provider=OB_CLOUD"
                        + "&page_size=10";

                System.out.println("Fetching granule coverage: " + cmrUrl);

                java.net.URL urlObj = new java.net.URL(cmrUrl);
                conn = (java.net.HttpURLConnection) urlObj.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                int status = conn.getResponseCode();
                if (status != 200) {
                    final int finalStatus = status;
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                            this,
                            "Failed to fetch granule information. HTTP status: " + finalStatus,
                            "CMR Error",
                            JOptionPane.ERROR_MESSAGE));
                    return;
                }

                String response;
                try (java.io.InputStream is = conn.getInputStream();
                     java.util.Scanner s = new java.util.Scanner(is, java.nio.charset.StandardCharsets.UTF_8.name()).useDelimiter("\\A")) {
                    response = s.hasNext() ? s.next() : "";
                }

                org.json.JSONObject json = new org.json.JSONObject(response);
                org.json.JSONArray entries = json.getJSONObject("feed").optJSONArray("entry");

                if (entries == null || entries.length() == 0) {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                            this,
                            "Granule not found in CMR: " + fileName,
                            "Granule Not Found",
                            JOptionPane.ERROR_MESSAGE));
                    return;
                }

                // Try to find the best matching granule
                org.json.JSONObject granule = entries.getJSONObject(0);
                for (int i = 0; i < entries.length(); i++) {
                    org.json.JSONObject g = entries.getJSONObject(i);

                    String producerGranuleId = g.optString("producer_granule_id", "");
                    String title = g.optString("title", "");

                    System.out.println("CMR match " + i
                            + ": id=" + g.optString("id", "")
                            + ", title=" + title
                            + ", producer_granule_id=" + producerGranuleId);

                    if (fileName.equals(producerGranuleId) || fileName.equals(title)) {
                        granule = g;
                        break;
                    }
                }

                String granuleId = granule.optString("id", "");
                String timeStart = granule.optString("time_start", "");
                String timeEnd = granule.optString("time_end", "");
                String title = granule.optString("title", "");
                String producerGranuleId = granule.optString("producer_granule_id", "");

                double minLat = Double.POSITIVE_INFINITY;
                double maxLat = Double.NEGATIVE_INFINITY;
                double minLon = Double.POSITIVE_INFINITY;
                double maxLon = Double.NEGATIVE_INFINITY;

                boolean boundsFound = false;

                // First try CMR boxes (preferred)
                org.json.JSONArray boxes = granule.optJSONArray("boxes");
                if (boxes != null && boxes.length() > 0) {
                    String box = boxes.get(0).toString();
                    String[] parts = box.trim().split("[,\\s]+");

                    if (parts.length >= 4) {
                        // CMR box format: south west north east
                        double south = Double.parseDouble(parts[0]);
                        double west = Double.parseDouble(parts[1]);
                        double north = Double.parseDouble(parts[2]);
                        double east = Double.parseDouble(parts[3]);

                        minLat = south;
                        maxLat = north;
                        minLon = west;
                        maxLon = east;
                        boundsFound = true;

                        System.out.println("Using CMR box for bounds: " + box);
                    }
                }

                // Fallback to polygons
                if (!boundsFound) {
                    org.json.JSONArray polygons = granule.optJSONArray("polygons");
                    if (polygons != null && polygons.length() > 0) {
                        for (int p = 0; p < polygons.length(); p++) {
                            Object polyObj = polygons.get(p);
                            String text;

                            if (polyObj instanceof String) {
                                text = (String) polyObj;
                            } else if (polyObj instanceof org.json.JSONArray) {
                                StringBuilder sb = new StringBuilder();
                                flattenJsonArray((org.json.JSONArray) polyObj, sb);
                                text = sb.toString();
                            } else {
                                text = polyObj.toString();
                            }

                            String[] coords = text.trim().split("[,\\s]+");
                            if (coords.length < 4 || coords.length % 2 != 0) {
                                continue;
                            }

                            for (int i = 0; i < coords.length; i += 2) {
                                // Important: CMR polygon order is lon, lat
                                double lon = Double.parseDouble(coords[i]);
                                double lat = Double.parseDouble(coords[i + 1]);

                                minLat = Math.min(minLat, lat);
                                maxLat = Math.max(maxLat, lat);
                                minLon = Math.min(minLon, lon);
                                maxLon = Math.max(maxLon, lon);
                            }

                            boundsFound = true;
                        }

                        System.out.println("Using CMR polygons for bounds.");
                    }
                }

                if (!boundsFound
                        || Double.isInfinite(minLat) || Double.isInfinite(maxLat)
                        || Double.isInfinite(minLon) || Double.isInfinite(maxLon)) {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                            this,
                            "No spatial coverage information found for this granule.",
                            "No Coverage Data",
                            JOptionPane.WARNING_MESSAGE));
                    return;
                }

                final double fMinLat = minLat;
                final double fMaxLat = maxLat;
                final double fMinLon = minLon;
                final double fMaxLon = maxLon;

                SwingUtilities.invokeLater(() -> {
                    String message =
                            "Approximate granule coverage from CMR metadata\n\n" +
                                    "Granule ID: " + granuleId + "\n" +
                                    "Title: " + title + "\n" +
                                    "Producer Granule ID: " + producerGranuleId + "\n" +
                                    "Time Start: " + timeStart + "\n" +
                                    "Time End: " + timeEnd + "\n\n" +
                                    String.format("Latitude range: %.6f to %.6f%n", fMinLat, fMaxLat) +
                                    String.format("Longitude range: %.6f to %.6f%n%n", fMinLon, fMaxLon) +
                                    "Note: These bounds come from CMR metadata and may differ from the actual file navigation.";

                    JOptionPane.showMessageDialog(
                            this,
                            message,
                            "Granule Coverage",
                            JOptionPane.INFORMATION_MESSAGE);
                });

            } catch (Exception e) {
                System.err.println("Error previewing granule coverage: " + e.getMessage());
                e.printStackTrace();

                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                        this,
                        "Error fetching granule coverage: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE));
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }).start();
    }

    private void flattenJsonArray(org.json.JSONArray array, StringBuilder sb) {
        for (int i = 0; i < array.length(); i++) {
            Object item = array.get(i);
            if (item instanceof org.json.JSONArray) {
                flattenJsonArray((org.json.JSONArray) item, sb);
            } else {
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                sb.append(item.toString());
            }
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
            if (statusArea != null) {
                statusArea.setText(message);
                statusArea.setCaretPosition(0);
            }
        });
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