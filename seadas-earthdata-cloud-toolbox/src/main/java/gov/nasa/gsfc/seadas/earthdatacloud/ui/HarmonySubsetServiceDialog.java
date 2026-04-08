package gov.nasa.gsfc.seadas.earthdatacloud.ui;

import gov.nasa.gsfc.seadas.earthdatacloud.action.HarmonySubsetTask;
import gov.nasa.gsfc.seadas.earthdatacloud.data.CmrVariableMetadataFetcher;
import gov.nasa.gsfc.seadas.earthdatacloud.data.FileVariableMetadataFetcher;
import gov.nasa.gsfc.seadas.earthdatacloud.data.CmrGranuleMetadataFetcher;
import gov.nasa.gsfc.seadas.earthdatacloud.data.VariableItem;
import gov.nasa.gsfc.seadas.earthdatacloud.preferences.Earthdata_Cloud_Controller;
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
//        panel.setBorder(BorderFactory.createEtchedBorder());

        panel.setOpaque(false);

        JPanel spatialBoundsPanel = createSpatialBoundsPanel();

        JPanel variablesPanel = createVariablesPanel();




        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.anchor = GridBagConstraints.CENTER;

        int row = 0;

        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 0;
        panel.add(spatialBoundsPanel, gbc);
        row++;


        // Variable list
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(variablesPanel, gbc);

        // Center the compact content panel inside the full tab area
        GridBagConstraints outerGbc = new GridBagConstraints();
        outerGbc.gridx = 0;
        outerGbc.gridy = 0;
        outerGbc.weightx = 1.0;
        outerGbc.weighty = 1.0;
        outerGbc.anchor = GridBagConstraints.CENTER;
        outerGbc.fill = GridBagConstraints.NONE;

        outerPanel.add(panel, outerGbc);

        return panel;
    }



    private JPanel createVariablesPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Variables"));


        DefaultListModel<VariableItem> variableModel = new DefaultListModel<>();
        variableList = new JList<>(variableModel);
        variableList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        variableList.setEnabled(false);
        updateStatus("Loading variables...");
        updateVariableList(new ArrayList<>());

        JScrollPane variableScrollPane = new JScrollPane(variableList);
//        variableScrollPane.setPreferredSize(new Dimension(520, 180));
//        variableScrollPane.setPreferredSize(new Dimension(520, 180));

        // Start empty; real values will be loaded from file metadata.
        updateVariableList(new ArrayList<>());





        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.anchor = GridBagConstraints.CENTER;

        int row = 0;

        // Spatial label
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(variableScrollPane, gbc);
        row++;

        return panel;
    }

    private JPanel createSpatialBoundsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Spatial Bounds"));

        JPanel classicPanel = createClassicBoundingBoxPanel();


        // Pre-fill spatial fields with search bounds if available
        if (searchLatMin != null && searchLatMax != null && searchLonMin != null && searchLonMax != null) {
            latMinField.setText(String.valueOf(searchLatMin));
            latMaxField.setText(String.valueOf(searchLatMax));
            lonMinField.setText(String.valueOf(searchLonMin));
            lonMaxField.setText(String.valueOf(searchLonMax));
        }


        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.anchor = GridBagConstraints.CENTER;

        int row = 0;

        // Spatial label
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(classicPanel, gbc);
        row++;


        JButton previewButton = new JButton("Preview Coverage");
        previewButton.addActionListener(e -> previewGranuleCoverage());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(previewButton);


        gbc.gridy = row;
        panel.add(buttonPanel, gbc);




        return panel;
    }


    private JPanel createClassicBoundingBoxPanel() {
//        System.out.println("Creating Classic BoundingBox Panel");


        JTextField tmpTextField = new JTextField(" 124°00′10″W ");
        Dimension preferredTextFieldSize = tmpTextField.getPreferredSize();
        int preferredColWidth = (int) Math.ceil(preferredTextFieldSize.getWidth() / 2.0);
        Dimension preferredLabelSize = new Dimension(preferredColWidth, 1);

        JPanel panel = new JPanel(new GridBagLayout());


        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets.top = 0;
        gbc.insets.bottom = 0;
        gbc.insets.left = 0;
        gbc.insets.right = 0;

        gbc.fill = GridBagConstraints.NONE;


        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        JLabel tmpLabel0 = new JLabel("");
        tmpLabel0.setMinimumSize(preferredLabelSize);
        panel.add(tmpLabel0, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JLabel tmpLabel1 = new JLabel("");
        tmpLabel1.setMinimumSize(preferredLabelSize);
        panel.add(tmpLabel1, gbc);

        gbc.gridx = 2;
        gbc.weightx = 1.0;
        JLabel tmpLabel2 = new JLabel("");
        tmpLabel2.setMinimumSize(preferredLabelSize);
        panel.add(tmpLabel2, gbc);

        gbc.gridx = 3;
        gbc.weightx = 1.0;
        JLabel tmpLabel3 = new JLabel("");
        tmpLabel3.setMinimumSize(preferredLabelSize);
        panel.add(tmpLabel3, gbc);

        gbc.gridx = 4;
        gbc.weightx = 1.0;
        JLabel tmpLabel4 = new JLabel("");
        tmpLabel4.setMinimumSize(preferredLabelSize);
        panel.add(tmpLabel4, gbc);

        gbc.gridx = 5;
        gbc.weightx = 1.0;
        JLabel tmpLabel5 = new JLabel("");
        tmpLabel5.setMinimumSize(preferredLabelSize);
        panel.add(tmpLabel5, gbc);



        gbc.gridy = 0;
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        JLabel maxLatLabel = new JLabel(Earthdata_Cloud_Controller.PROPERTY_MAXLAT_LABEL + ":");
        maxLatLabel.setToolTipText(Earthdata_Cloud_Controller.PROPERTY_MAXLAT_SUBSET_TOOLTIP);
        panel.add(maxLatLabel, gbc);

        gbc.gridx = 2;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        latMaxField = new JTextField(Earthdata_Cloud_Controller.getPreferenceMaxLat());
        latMaxField.setToolTipText(Earthdata_Cloud_Controller.PROPERTY_MAXLAT_SUBSET_TOOLTIP);
        panel.add(latMaxField, gbc);
        gbc.gridwidth = 1;


        gbc.gridy++;
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        JLabel minLonLabel = new JLabel(Earthdata_Cloud_Controller.PROPERTY_MINLON_LABEL + ":");
        minLonLabel.setToolTipText(Earthdata_Cloud_Controller.PROPERTY_MINLON_SUBSET_TOOLTIP);
        panel.add(minLonLabel, gbc);


        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets.left = 0;
        gbc.insets.right = 2;
        lonMinField = new JTextField(Earthdata_Cloud_Controller.getPreferenceMinLon());
        lonMinField.setToolTipText(Earthdata_Cloud_Controller.PROPERTY_MINLON_SUBSET_TOOLTIP);
        panel.add(lonMinField, gbc);
        gbc.gridwidth = 1;

        gbc.gridx = 3;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets.left = 2;
        gbc.insets.right = 0;
        lonMaxField = new JTextField(Earthdata_Cloud_Controller.getPreferenceMaxLon());
        lonMaxField.setToolTipText(Earthdata_Cloud_Controller.PROPERTY_MAXLON_SUBSET_TOOLTIP);
        panel.add(lonMaxField, gbc);
        gbc.gridwidth = 1;

        gbc.insets.left = 0;
        gbc.insets.right = 0;

        gbc.gridx = 5;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        JLabel maxLonLabel = new JLabel(":" + Earthdata_Cloud_Controller.PROPERTY_MAXLON_LABEL);
        maxLonLabel.setToolTipText(Earthdata_Cloud_Controller.PROPERTY_MAXLON_SUBSET_TOOLTIP);
        panel.add(maxLonLabel, gbc);

        gbc.gridy++;
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 1.0;
        JLabel minLatLabel = new JLabel(Earthdata_Cloud_Controller.PROPERTY_MINLAT_LABEL + ":");
        minLatLabel.setToolTipText(Earthdata_Cloud_Controller.PROPERTY_MINLAT_SUBSET_TOOLTIP);
        panel.add(minLatLabel, gbc);

        gbc.gridx = 2;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 1.0;
        latMinField = new JTextField(Earthdata_Cloud_Controller.getPreferenceMinLat());
        latMinField.setToolTipText(Earthdata_Cloud_Controller.PROPERTY_MINLAT_SUBSET_TOOLTIP);
        panel.add(latMinField, gbc);
        gbc.gridwidth = 1;

        latMinField.setMinimumSize(preferredTextFieldSize);
        latMinField.setPreferredSize(preferredTextFieldSize);
        latMaxField.setMinimumSize(preferredTextFieldSize);
        latMaxField.setPreferredSize(preferredTextFieldSize);
        lonMinField.setMinimumSize(preferredTextFieldSize);
        lonMinField.setPreferredSize(preferredTextFieldSize);
        lonMaxField.setMinimumSize(preferredTextFieldSize);
        lonMaxField.setPreferredSize(preferredTextFieldSize);

        panel.setMinimumSize(panel.getPreferredSize());
        panel.setPreferredSize(panel.getPreferredSize());

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
                        + "&page_size=50";

                System.out.println("Fetching granule coverage: " + cmrUrl);

                java.net.URL urlObj = new java.net.URL(cmrUrl);
                conn = (HttpURLConnection) urlObj.openConnection();
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
                     java.util.Scanner s = new java.util.Scanner(
                             is, java.nio.charset.StandardCharsets.UTF_8.name()).useDelimiter("\\A")) {
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

                org.json.JSONObject granule = findExactGranuleMatch(entries, fileName, dataUrl);

                if (granule == null) {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                            this,
                            "Could not find an exact CMR granule match for:\n" + fileName + "\n\n"
                                    + "Coverage preview was cancelled to avoid showing incorrect bounds.",
                            "Exact Granule Match Not Found",
                            JOptionPane.WARNING_MESSAGE));
                    return;
                }

                String granuleId = granule.optString("id", "");
                String timeStart = granule.optString("time_start", "");
                String timeEnd = granule.optString("time_end", "");
                String title = granule.optString("title", "");
                String producerGranuleId = granule.optString("producer_granule_id", "");

                Bounds bounds = extractBoundsFromGranule(granule);

                if (bounds == null) {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                            this,
                            "No spatial coverage information found for this granule.",
                            "No Coverage Data",
                            JOptionPane.WARNING_MESSAGE));
                    return;
                }

                String warning = buildCoverageConsistencyWarning(bounds);

                SwingUtilities.invokeLater(() -> {
                    String message =
                            "Approximate granule coverage from CMR metadata\n\n" +
                                    "Granule ID: " + granuleId + "\n" +
                                    "Title: " + title + "\n" +
                                    "Producer Granule ID: " + producerGranuleId + "\n" +
                                    "Time Start: " + timeStart + "\n" +
                                    "Time End: " + timeEnd + "\n\n" +
                                    String.format("Latitude range: %.6f to %.6f%n", bounds.minLat, bounds.maxLat) +
                                    String.format("Longitude range: %.6f to %.6f%n", bounds.minLon, bounds.maxLon) +
                                    warning +
                                    "\n\nNote: These bounds come from CMR metadata and may differ from the actual file navigation.";

                    JOptionPane.showMessageDialog(
                            this,
                            message,
                            "Granule Coverage",
                            JOptionPane.INFORMATION_MESSAGE);
                });

            } catch (Exception e) {
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
    private String buildCoverageConsistencyWarning(Bounds granuleBounds) {
        try {
            double south = Double.parseDouble(latMinField.getText().trim());
            double north = Double.parseDouble(latMaxField.getText().trim());
            double west  = Double.parseDouble(lonMinField.getText().trim());
            double east  = Double.parseDouble(lonMaxField.getText().trim());

            double reqMinLat = Math.min(south, north);
            double reqMaxLat = Math.max(south, north);

            boolean requestCrossesDateline = west > east;

            boolean latOverlap = rangesOverlap(
                    granuleBounds.minLat, granuleBounds.maxLat,
                    reqMinLat, reqMaxLat);

            boolean lonOverlap;
            if (requestCrossesDateline) {
                lonOverlap = true; // safe for release
            } else {
                lonOverlap = rangesOverlap(
                        granuleBounds.minLon, granuleBounds.maxLon,
                        west, east);
            }

            if (latOverlap && lonOverlap) {
                return "";
            }

            return "\nWARNING: The CMR-reported granule coverage may not overlap the requested region.";

        } catch (Exception e) {
            // safe fallback — do nothing
            return "";
        }
    }

    private boolean rangesOverlap(double min1, double max1, double min2, double max2) {
        return max1 >= min2 && max2 >= min1;
    }
    private Bounds extractBoundsFromGranule(org.json.JSONObject granule) {

        // Try boxes first
        org.json.JSONArray boxes = granule.optJSONArray("boxes");
        if (boxes != null && boxes.length() > 0) {
            String[] parts = boxes.get(0).toString().trim().split("[,\\s]+");
            if (parts.length >= 4) {
                double south = Double.parseDouble(parts[0]);
                double west = Double.parseDouble(parts[1]);
                double north = Double.parseDouble(parts[2]);
                double east = Double.parseDouble(parts[3]);
                return new Bounds(south, north, west, east);
            }
        }

        // Fallback to polygons (robust parsing)
        org.json.JSONArray polygons = granule.optJSONArray("polygons");
        if (polygons == null || polygons.length() == 0) {
            return null;
        }

        String text = polygons.getJSONArray(0).getString(0);
        String[] coords = text.trim().split("[,\\s]+");

        Bounds lonLat = computeBounds(coords, true);
        Bounds latLon = computeBounds(coords, false);

        boolean lonLatValid = isValid(lonLat);
        boolean latLonValid = isValid(latLon);

        if (latLonValid && !lonLatValid) return latLon;
        if (lonLatValid && !latLonValid) return lonLat;

        return latLon; // safe default for PACE
    }

    private Bounds computeBounds(String[] coords, boolean lonLatOrder) {
        double minLat = Double.POSITIVE_INFINITY;
        double maxLat = Double.NEGATIVE_INFINITY;
        double minLon = Double.POSITIVE_INFINITY;
        double maxLon = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < coords.length; i += 2) {
            double a = Double.parseDouble(coords[i]);
            double b = Double.parseDouble(coords[i + 1]);

            double lon = lonLatOrder ? a : b;
            double lat = lonLatOrder ? b : a;

            minLat = Math.min(minLat, lat);
            maxLat = Math.max(maxLat, lat);
            minLon = Math.min(minLon, lon);
            maxLon = Math.max(maxLon, lon);
        }

        return new Bounds(minLat, maxLat, minLon, maxLon);
    }

    private boolean isValid(Bounds b) {
        return b.minLat >= -90 && b.maxLat <= 90 &&
                b.minLon >= -180 && b.maxLon <= 180;
    }

    private static class Bounds {
        double minLat, maxLat, minLon, maxLon;

        Bounds(double minLat, double maxLat, double minLon, double maxLon) {
            this.minLat = minLat;
            this.maxLat = maxLat;
            this.minLon = minLon;
            this.maxLon = maxLon;
        }
    }

    private org.json.JSONObject findExactGranuleMatch(org.json.JSONArray entries, String fileName, String dataUrl) {
        String normalizedTarget = normalizeGranuleName(fileName);

        java.util.List<org.json.JSONObject> exactMatches = new java.util.ArrayList<>();

        for (int i = 0; i < entries.length(); i++) {
            org.json.JSONObject g = entries.getJSONObject(i);

            String producerGranuleId = g.optString("producer_granule_id", "");
            String title = g.optString("title", "");

            boolean matched = false;

            if (normalizedTarget.equals(normalizeGranuleName(producerGranuleId))) {
                matched = true;
            } else if (normalizedTarget.equals(normalizeGranuleName(title))) {
                matched = true;
            } else if (entryHasMatchingLink(g, normalizedTarget)) {
                matched = true;
            }

            System.out.println("CMR candidate " + i
                    + ": id=" + g.optString("id", "")
                    + ", title=" + title
                    + ", producer_granule_id=" + producerGranuleId
                    + ", matched=" + matched);

            if (matched) {
                exactMatches.add(g);
            }
        }

        if (exactMatches.isEmpty()) {
            return null;
        }

        if (exactMatches.size() == 1) {
            return exactMatches.get(0);
        }

        // Prefer exact match on producer_granule_id first
        for (org.json.JSONObject g : exactMatches) {
            String producerGranuleId = g.optString("producer_granule_id", "");
            if (normalizedTarget.equals(normalizeGranuleName(producerGranuleId))) {
                return g;
            }
        }

        // Prefer exact match on title next
        for (org.json.JSONObject g : exactMatches) {
            String title = g.optString("title", "");
            if (normalizedTarget.equals(normalizeGranuleName(title))) {
                return g;
            }
        }

        // Still ambiguous -> return null instead of guessing
        System.err.println("Multiple exact CMR matches found for file: " + fileNameFromUrl(dataUrl));
        return null;
    }

    private boolean entryHasMatchingLink(org.json.JSONObject granule, String normalizedTarget) {
        org.json.JSONArray links = granule.optJSONArray("links");
        if (links == null) {
            return false;
        }

        for (int i = 0; i < links.length(); i++) {
            org.json.JSONObject link = links.optJSONObject(i);
            if (link == null) {
                continue;
            }

            String href = link.optString("href", "");
            String linkFileName = extractFileNameFromHref(href);

            if (normalizedTarget.equals(normalizeGranuleName(linkFileName))) {
                return true;
            }
        }

        return false;
    }

    private String extractFileNameFromHref(String href) {
        if (href == null || href.isEmpty()) {
            return "";
        }

        try {
            String path = new java.net.URL(href).getPath();
            int slash = path.lastIndexOf('/');
            return slash >= 0 ? path.substring(slash + 1) : path;
        } catch (Exception e) {
            int slash = href.lastIndexOf('/');
            return slash >= 0 ? href.substring(slash + 1) : href;
        }
    }

    private String fileNameFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return "";
        }
        int slash = url.lastIndexOf('/');
        return slash >= 0 ? url.substring(slash + 1) : url;
    }

    private String normalizeGranuleName(String s) {
        if (s == null) {
            return "";
        }

        String value = s.trim();

        int queryIdx = value.indexOf('?');
        if (queryIdx >= 0) {
            value = value.substring(0, queryIdx);
        }

        int slash = value.lastIndexOf('/');
        if (slash >= 0) {
            value = value.substring(slash + 1);
        }

        return value.trim().toLowerCase(java.util.Locale.ROOT);
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