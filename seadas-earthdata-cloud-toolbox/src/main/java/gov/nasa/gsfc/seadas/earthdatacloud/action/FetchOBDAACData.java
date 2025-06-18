package gov.nasa.gsfc.seadas.earthdatacloud.action;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Map;
import java.util.concurrent.Future;

import org.json.JSONArray;
import org.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.cache.CacheBuilder;
import org.apache.commons.cache.CacheLoader;
import org.apache.commons.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FetchOBDAACData extends JFrame {

    private static final String CMR_COLLECTION_URL = "https://cmr.earthdata.nasa.gov/search/collections.json?short_name=";
    private static final String CMR_GRANULE_URL = "https://cmr.earthdata.nasa.gov/search/granules.umm_json?collection_concept_id=";

    private JTextArea outputArea;
    private JButton searchButton, downloadButton, nextButton, prevButton;
    private JComboBox<Integer> pageSizeSelector;
    private JComboBox<String> satelliteSelector, levelSelector, productSelector;
    private List<String> downloadUrls;
    private int currentPage = 0;
    private int pageSize = 20;
    private JLabel cacheStatusLabel;
    private ProgressMonitor progressMonitor;
    private Future<?> currentTask;

    public FetchOBDAACData() {
        setTitle("NASA OB-DAAC Data Downloader");
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new FlowLayout());

        inputPanel.add(new JLabel("Satellite & Instrument:"));
        satelliteSelector = new JComboBox<>(new String[]{"PACE_OCI", "MODIS_AQUA", "MODIS_TERRA"});
        inputPanel.add(satelliteSelector);

        inputPanel.add(new JLabel("Data Level:"));
        levelSelector = new JComboBox<>(new String[]{"L1", "L2", "L3M"});
        inputPanel.add(levelSelector);

        inputPanel.add(new JLabel("Product Name:"));
        productSelector = new JComboBox<>(new String[]{"SFREFL_NRT", "CHL", "SST"});
        inputPanel.add(productSelector);

        searchButton = new JButton("Search");
        inputPanel.add(searchButton);

        downloadButton = new JButton("Download All");
        downloadButton.setEnabled(false);
        inputPanel.add(downloadButton);

        add(inputPanel, BorderLayout.NORTH);

        outputArea = new JTextArea();
        outputArea.setEditable(false);
        add(new JScrollPane(outputArea), BorderLayout.CENTER);

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout());

        controlPanel.add(new JLabel("Items per page:"));
        pageSizeSelector = new JComboBox<>(new Integer[]{5, 10, 20, 50, 100});
        pageSizeSelector.setSelectedItem(20);
        controlPanel.add(pageSizeSelector);

        prevButton = new JButton("Previous");
        prevButton.setEnabled(false);
        controlPanel.add(prevButton);

        nextButton = new JButton("Next");
        nextButton.setEnabled(false);
        controlPanel.add(nextButton);

        cacheStatusLabel = new JLabel("Cache: Ready");
        cacheStatusLabel.setForeground(Color.GRAY);
        controlPanel.add(cacheStatusLabel);

        add(controlPanel, BorderLayout.SOUTH);

        searchButton.addActionListener(e -> {
            String shortName = generateShortName();
            pageSize = (Integer) pageSizeSelector.getSelectedItem();
            searchForData(shortName);
        });

        pageSizeSelector.addActionListener(e -> {
            pageSize = (Integer) pageSizeSelector.getSelectedItem();
            currentPage = 0;
            displayPage();
        });

        nextButton.addActionListener(e -> {
            if ((currentPage + 1) * pageSize < downloadUrls.size()) {
                currentPage++;
                displayPage();
            }
        });

        prevButton.addActionListener(e -> {
            if (currentPage > 0) {
                currentPage--;
                displayPage();
            }
        });

        downloadButton.addActionListener(e -> downloadAllFiles());

        progressMonitor = new ProgressMonitor();
        progressMonitor.setVisible(false);
        add(progressMonitor, BorderLayout.SOUTH);

        progressMonitor.addCancelListener(e -> {
            if (currentTask != null) {
                currentTask.cancel(true);
                progressMonitor.updateProgress(0, "Operation cancelled");
            }
        });

        // Add input validation to coordinate fields
        addCoordinateValidation(minLatField, -90, 90);
        addCoordinateValidation(maxLatField, -90, 90);
        addCoordinateValidation(minLonField, -180, 180);
        addCoordinateValidation(maxLonField, -180, 180);
    }

    private String generateShortName() {
        return satelliteSelector.getSelectedItem() + "_" + levelSelector.getSelectedItem() + "_" + productSelector.getSelectedItem();
    }

    private void searchForData(String shortName) {
        outputArea.setText("Searching for data for: " + shortName + "...\n");
        new Thread(() -> {
            try {
                String conceptId = getConceptId(shortName);
                if (conceptId != null) {
                    downloadUrls = getGranuleUrls(conceptId);
                    SwingUtilities.invokeLater(() -> {
                        currentPage = 0;
                        displayPage();
                        downloadButton.setEnabled(true);
                        nextButton.setEnabled(downloadUrls.size() > pageSize);
                    });
                } else {
                    SwingUtilities.invokeLater(() -> outputArea.setText("Concept ID not found for " + shortName));
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> outputArea.setText("Error occurred: " + ex.getMessage()));
            }
        }).start();
    }

    private void displayPage() {
        outputArea.setText("Showing results " + (currentPage * pageSize + 1) + " to " + Math.min((currentPage + 1) * pageSize, downloadUrls.size()) + " of " + downloadUrls.size() + "\n\n");

        for (int i = currentPage * pageSize; i < Math.min((currentPage + 1) * pageSize, downloadUrls.size()); i++) {
            outputArea.append(downloadUrls.get(i) + "\n");
        }

        prevButton.setEnabled(currentPage > 0);
        nextButton.setEnabled((currentPage + 1) * pageSize < downloadUrls.size());
    }

    private void downloadAllFiles() {
        outputArea.append("\nStarting download...\n");
        new Thread(() -> {
            for (String fileUrl : downloadUrls) {
                try {
                    URL url = new URL(fileUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");

                    String fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
                    InputStream in = conn.getInputStream();
                    FileOutputStream out = new FileOutputStream(fileName);

                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }

                    in.close();
                    out.close();

                    SwingUtilities.invokeLater(() -> outputArea.append("Downloaded: " + fileName + "\n"));
                } catch (IOException ex) {
                    SwingUtilities.invokeLater(() -> outputArea.append("Failed to download: " + fileUrl + "\n"));
                }
            }
            SwingUtilities.invokeLater(() -> outputArea.append("\nAll downloads completed."));
        }).start();
    }

    private static String getConceptId(String shortName) throws IOException {
        String urlString = CMR_COLLECTION_URL + shortName;
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        JSONObject jsonResponse = new JSONObject(response.toString());
        JSONArray items = jsonResponse.getJSONObject("feed").getJSONArray("entry");

        if (items.length() > 0) {
            return items.getJSONObject(0).getString("id");
        }
        return null;
    }

    private static List<String> getGranuleUrls(String conceptId) throws IOException {
        List<String> urls = new ArrayList<>();
        int pageSize = 100;  // Request up to 100 results per page
        int pageNum = 1;  // Start from page 1

        while (true) {
            String urlString = String.format(
                    "https://cmr.earthdata.nasa.gov/search/granules.umm_json?collection_concept_id=%s&page_size=%d&page_num=%d",
                    conceptId, pageSize, pageNum
            );

            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            JSONObject jsonResponse = new JSONObject(response.toString());
            JSONArray items = jsonResponse.getJSONArray("items");

            if (items.length() == 0) {
                break; // No more results, exit the loop
            }

            for (int i = 0; i < items.length(); i++) {
                JSONObject umm = items.getJSONObject(i).getJSONObject("umm");
                JSONArray relatedUrls = umm.getJSONArray("RelatedUrls");

                for (int j = 0; j < relatedUrls.length(); j++) {
                    JSONObject relatedUrl = relatedUrls.getJSONObject(j);
                    String urlType = relatedUrl.optString("Type");
                    if ("GET DATA".equalsIgnoreCase(urlType)) {
                        urls.add(relatedUrl.getString("URL"));
                    }
                }
            }

            pageNum++; // Move to the next page
        }

        return urls;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FetchOBDAACData app = new FetchOBDAACData();
            app.setVisible(true);
        });
    }

    private void updateCacheStatus(String status) {
        SwingUtilities.invokeLater(() -> {
            cacheStatusLabel.setText("Cache: " + status);
            if (status.equals("Hit")) {
                cacheStatusLabel.setForeground(Color.GREEN);
            } else if (status.equals("Miss")) {
                cacheStatusLabel.setForeground(Color.ORANGE);
            } else {
                cacheStatusLabel.setForeground(Color.GRAY);
            }
        });
    }

    private void performSearch() {
        try {
            // Validate inputs
            ValidationResult validationResult = validateInputs();
            if (!validationResult.isValid()) {
                ErrorHandler.showWarning(this, validationResult.getErrorMessage());
                return;
            }
            
            // Disable UI components
            setComponentsEnabled(false);
            progressMonitor.setVisible(true);
            progressMonitor.setIndeterminate(true);
            progressMonitor.updateProgress(0, "Searching...");
            
            // Submit search task
            currentTask = TaskManager.getInstance().submitTask(() -> {
                try {
                    String satellite = (String) satelliteSelector.getSelectedItem();
                    String level = (String) levelSelector.getSelectedItem();
                    String product = (String) productSelector.getSelectedItem();
                    int maxResults = (Integer) pageSizeSelector.getSelectedItem();
                    
                    String startDate = generateShortName();
                    String endDate = generateShortName();
                    
                    String minLat = "0";
                    String maxLat = "0";
                    String minLon = "0";
                    String maxLon = "0";
                    
                    // Clear existing results
                    SwingUtilities.invokeLater(() -> {
                        outputArea.setText("");
                        downloadUrls.clear();
                    });
                    
                    // Fetch results
                    downloadUrls = getGranuleUrls(generateShortName());
                    
                    // Update UI with results
                    SwingUtilities.invokeLater(() -> {
                        currentPage = 0;
                        displayPage();
                        downloadButton.setEnabled(true);
                        nextButton.setEnabled(downloadUrls.size() > pageSize);
                        progressMonitor.updateProgress(100, "Search completed");
                        progressMonitor.setVisible(false);
                        setComponentsEnabled(true);
                    });
                    
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> {
                        ErrorHandler.handleError(this, e, "Error performing search");
                        progressMonitor.setVisible(false);
                        setComponentsEnabled(true);
                    });
                }
            });
            
        } catch (Exception e) {
            ErrorHandler.handleError(this, e, "Error starting search");
            progressMonitor.setVisible(false);
            setComponentsEnabled(true);
        }
    }

    private ValidationResult validateInputs() {
        ValidationResult result = new ValidationResult();
        
        // Validate coordinates
        String minLat = "0";
        String maxLat = "0";
        String minLon = "0";
        String maxLon = "0";
        
        ValidationResult coordResult = InputValidator.validateCoordinates(minLat, maxLat);
        if (!coordResult.isValid()) {
            result.addError("Latitude validation failed: " + coordResult.getErrorMessage());
        }
        
        coordResult = InputValidator.validateCoordinates(minLon, maxLon);
        if (!coordResult.isValid()) {
            result.addError("Longitude validation failed: " + coordResult.getErrorMessage());
        }
        
        // Validate dates
        String startDate = generateShortName();
        String endDate = generateShortName();
        
        ValidationResult dateResult = InputValidator.validateDateRange(startDate, endDate);
        if (!dateResult.isValid()) {
            result.addError("Date validation failed: " + dateResult.getErrorMessage());
        }
        
        // Validate product
        String product = (String) productSelector.getSelectedItem();
        ValidationResult productResult = InputValidator.validateProduct(product);
        if (!productResult.isValid()) {
            result.addError("Product validation failed: " + productResult.getErrorMessage());
        }
        
        return result;
    }

    private void setComponentsEnabled(boolean enabled) {
        satelliteSelector.setEnabled(enabled);
        levelSelector.setEnabled(enabled);
        productSelector.setEnabled(enabled);
        pageSizeSelector.setEnabled(enabled);
        searchButton.setEnabled(enabled);
        downloadButton.setEnabled(enabled);
        prevButton.setEnabled(enabled);
        nextButton.setEnabled(enabled);
        progressMonitor.setCancelEnabled(enabled);
    }

    private void addCoordinateValidation(JTextField field, double min, double max) {
        field.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { validate(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { validate(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { validate(); }
            
            private void validate() {
                try {
                    String text = field.getText().trim();
                    if (!text.isEmpty()) {
                        double value = Double.parseDouble(text);
                        if (value < min || value > max) {
                            field.setBackground(new Color(255, 200, 200));
                        } else {
                            field.setBackground(Color.WHITE);
                        }
                    } else {
                        field.setBackground(Color.WHITE);
                    }
                } catch (NumberFormatException e) {
                    field.setBackground(new Color(255, 200, 200));
                }
            }
        });
    }
}

public class DataValidator {
    public static boolean validateSpatialExtent(double minLat, double maxLat, 
                                             double minLon, double maxLon) {
        return minLat >= -90 && maxLat <= 90 &&
               minLon >= -180 && maxLon <= 180 &&
               minLat <= maxLat && minLon <= maxLon;
    }
}

public class ErrorHandler {
    private static final Logger logger = LoggerFactory.getLogger(ErrorHandler.class);
    
    public static void handleError(Component parent, Exception e, String message) {
        String errorMessage = String.format("%s\nDetails: %s", message, e.getMessage());
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(parent, errorMessage, "Error", 
                                        JOptionPane.ERROR_MESSAGE);
        });
        e.printStackTrace();
    }
    
    private static void logError(Exception e) {
        logger.error("Error occurred", e);
    }
}

public class ConfigurationManager {
    private static final Properties config = new Properties();
    
    static {
        loadConfiguration();
    }
    
    private static void loadConfiguration() {
        try (InputStream input = ConfigurationManager.class
                .getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (input != null) {
                config.load(input);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load configuration", e);
        }
    }
    
    public static String getProperty(String key) {
        return config.getProperty(key);
    }
    
    public static String getProperty(String key, String defaultValue) {
        return config.getProperty(key, defaultValue);
    }
}

public class LoggingManager {
    private static final Logger logger = LoggerFactory.getLogger(LoggingManager.class);
    
    public static void logError(Exception e) {
        logger.error("Error occurred", e);
    }
    
    public static void logInfo(String message) {
        logger.info(message);
    }
}

public class ThreadManager {
    private static final ExecutorService executor = 
        Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    
    public static void submitTask(Runnable task) {
        executor.submit(task);
    }
    
    public static void shutdown() {
        executor.shutdown();
    }
}

public class SimpleCache<K, V> {
    private final Map<K, CacheEntry<V>> cache;
    private final long expirationTimeMillis;
    
    public SimpleCache(long expirationTimeMillis) {
        this.cache = new ConcurrentHashMap<>();
        this.expirationTimeMillis = expirationTimeMillis;
    }
    
    public V get(K key) {
        CacheEntry<V> entry = cache.get(key);
        if (entry != null && !entry.isExpired()) {
            return entry.getValue();
        }
        cache.remove(key);
        return null;
    }
    
    public void put(K key, V value) {
        cache.put(key, new CacheEntry<>(value));
    }
    
    public void clear() {
        cache.clear();
    }
    
    private class CacheEntry<V> {
        private final V value;
        private final long timestamp;
        
        CacheEntry(V value) {
            this.value = value;
            this.timestamp = System.currentTimeMillis();
        }
        
        V getValue() {
            return value;
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > expirationTimeMillis;
        }
    }
}

public class GranuleFetcher {
    private final SimpleCache<String, JSONObject> metadataCache;
    
    public GranuleFetcher() {
        this.metadataCache = new SimpleCache<>(3600000); // 1 hour cache
    }
    
    public Map<String, String> fetchGranules(String product, int maxResults, 
                                           String startDate, String endDate,
                                           String minLat, String maxLat,
                                           String minLon, String maxLon,
                                           DefaultTableModel tableModel) {
        try {
            // Generate cache key
            String cacheKey = String.format("%s_%s_%s_%s_%s_%s_%s_%d",
                    product, startDate, endDate, minLat, maxLat, minLon, maxLon, maxResults);
            
            // Check cache first
            Map<String, String> cachedResult = CacheManager.getInstance().getGranules(cacheKey);
            if (cachedResult != null) {
                updateTableModel(tableModel, cachedResult);
                return cachedResult;
            }
            
            // If not in cache, fetch from API
            Map<String, String> result = fetchFromApi(product, maxResults, startDate, endDate,
                                                    minLat, maxLat, minLon, maxLon);
            
            // Cache the result
            CacheManager.getInstance().putGranules(cacheKey, result);
            
            // Update table model
            updateTableModel(tableModel, result);
            
            return result;
        } catch (Exception e) {
            ErrorHandler.handleError(null, e, "Error fetching granules");
            return new HashMap<>();
        }
    }

    private void updateTableModel(DefaultTableModel tableModel, Map<String, String> results) {
        tableModel.setRowCount(0);
        for (Map.Entry<String, String> entry : results.entrySet()) {
            tableModel.addRow(new Object[]{entry.getKey()});
        }
    }
}

public class ProgressMonitor extends JPanel {
    private final JProgressBar progressBar;
    private final JLabel statusLabel;
    private final JButton cancelButton;
    
    public ProgressMonitor() {
        setLayout(new BorderLayout(5, 5));
        
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        
        statusLabel = new JLabel("Ready");
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        cancelButton = new JButton("Cancel");
        cancelButton.setEnabled(false);
        
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(progressBar, BorderLayout.CENTER);
        topPanel.add(cancelButton, BorderLayout.EAST);
        
        add(topPanel, BorderLayout.NORTH);
        add(statusLabel, BorderLayout.CENTER);
    }
    
    public void updateProgress(int value, String status) {
        SwingUtilities.invokeLater(() -> {
            progressBar.setValue(value);
            statusLabel.setText(status);
        });
    }
    
    public void setIndeterminate(boolean indeterminate) {
        progressBar.setIndeterminate(indeterminate);
    }
    
    public void setCancelEnabled(boolean enabled) {
        cancelButton.setEnabled(enabled);
    }
    
    public void addCancelListener(java.awt.event.ActionListener listener) {
        cancelButton.addActionListener(listener);
    }
}

public class TaskManager {
    private static final int MAX_THREADS = 3;
    private final ExecutorService executor;
    private static TaskManager instance;
    
    private TaskManager() {
        executor = Executors.newFixedThreadPool(MAX_THREADS);
    }
    
    public static synchronized TaskManager getInstance() {
        if (instance == null) {
            instance = new TaskManager();
        }
        return instance;
    }
    
    public Future<?> submitTask(Runnable task) {
        return executor.submit(task);
    }
    
    public void shutdown() {
        executor.shutdown();
    }
}

public class StatusBar extends JPanel {
    private final JLabel messageLabel;
    private final JLabel countLabel;
    
    public StatusBar() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEtchedBorder());
        
        messageLabel = new JLabel("Ready");
        countLabel = new JLabel("0 results");
        
        add(messageLabel, BorderLayout.WEST);
        add(countLabel, BorderLayout.EAST);
    }
    
    public void setMessage(String message) {
        messageLabel.setText(message);
    }
    
    public void setResultCount(int count) {
        countLabel.setText(count + " results");
    }
}
