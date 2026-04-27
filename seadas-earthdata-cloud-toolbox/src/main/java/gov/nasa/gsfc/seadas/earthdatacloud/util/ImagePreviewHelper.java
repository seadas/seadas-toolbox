package gov.nasa.gsfc.seadas.earthdatacloud.util;

import gov.nasa.gsfc.seadas.earthdatacloud.preferences.Earthdata_Cloud_Controller;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

public class ImagePreviewHelper {

    private static final int HOVER_DELAY_MS = 250;
    private static final int LOADING_WIDTH = 240;
    private static final int LOADING_HEIGHT = 96;
    private static final int WINDOW_GAP = 10;
    private static final int MAX_CACHE_SIZE = 40;

    private JWindow previewWindow;
    private final JPanel previewPanel;
    private final JLabel previewLabel;
    private final JLabel statusLabel;
    private final Timer hoverTimer;
    private final Timer exitTimer;
    private final Map<String, ImageIcon> previewCache;
    private final Map<String, String> resolvedPreviewUrlCache;
    private final Object cacheLock = new Object();

    private SwingWorker<PreviewResult, Void> loadWorker;
    private String hoveredFileName;
    private String displayedFileName;
    private Point hoverLocationOnScreen;
    private JDialog parentDialog;
    private Component previewAnchor;
    private Map<String, String> previewLinkMap;

    private static final class PreviewResult {
        private final String resolvedUrl;
        private final ImageIcon icon;

        private PreviewResult(String resolvedUrl, ImageIcon icon) {
            this.resolvedUrl = resolvedUrl;
            this.icon = icon;
        }
    }

    public ImagePreviewHelper() {
        previewPanel = new JPanel(new BorderLayout(0, 6));
        previewLabel = new JLabel("", SwingConstants.CENTER);
        statusLabel = new JLabel("", SwingConstants.CENTER);
        hoverTimer = new Timer(HOVER_DELAY_MS, e -> showPreviewForHoveredFile());
        exitTimer = new Timer(150, e -> clearHoverStateIfPointerOutsidePreviewArea());
        previewCache = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, ImageIcon> eldest) {
                return size() > MAX_CACHE_SIZE;
            }
        };
        resolvedPreviewUrlCache = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                return size() > MAX_CACHE_SIZE;
            }
        };

        hoverTimer.setRepeats(false);
        exitTimer.setRepeats(false);

        previewLabel.setHorizontalAlignment(SwingConstants.CENTER);
        previewLabel.setVerticalAlignment(SwingConstants.CENTER);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));

        previewPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(150, 150, 150)),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        previewPanel.setBackground(UIManager.getColor("Panel.background"));
        previewPanel.add(previewLabel, BorderLayout.CENTER);
        previewPanel.add(statusLabel, BorderLayout.SOUTH);

        previewWindow = createPreviewWindow(null);
    }

    public void attachToTable(JTable table, Map<String, String> previewLinkMap, JDialog parentDialog) {
        this.parentDialog = parentDialog;
        this.previewAnchor = table;
        this.previewLinkMap = previewLinkMap;
        ensurePreviewWindowOwner(parentDialog != null ? parentDialog : SwingUtilities.getWindowAncestor(table));

        table.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                exitTimer.stop();
                hoverLocationOnScreen = e.getLocationOnScreen();

                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (row < 0 || row >= table.getRowCount() || col != 0) {
                    clearHoverState();
                    return;
                }

                Object value = table.getValueAt(row, 0);
                String fileName = value instanceof String ? (String) value : null;
                if (fileName == null || fileName.isBlank()) {
                    clearHoverState();
                    return;
                }

                if (fileName.equals(displayedFileName) && previewWindow.isVisible()) {
                    positionPreviewWindow();
                    return;
                }

                if (!fileName.equals(hoveredFileName)) {
                    hoveredFileName = fileName;
                    hoverTimer.restart();
                }
            }
        });

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                exitTimer.restart();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                hideImagePreview();
            }
        });
    }

    private void clearHoverState() {
        hoveredFileName = null;
        hoverTimer.stop();
        exitTimer.stop();
        cancelLoadWorker();
        hideImagePreview();
    }

    private void clearHoverStateIfPointerOutsidePreviewArea() {
        if (isPointerInComponent(previewAnchor) || isPointerInComponent(previewWindow)) {
            return;
        }
        clearHoverState();
    }

    private boolean isPointerInComponent(Component component) {
        if (component == null || !component.isShowing()) {
            return false;
        }
        PointerInfo pointerInfo;
        try {
            pointerInfo = MouseInfo.getPointerInfo();
        } catch (HeadlessException ignored) {
            return false;
        } catch (SecurityException ignored) {
            return false;
        }
        if (pointerInfo == null) {
            return false;
        }

        Point pointerLocation = pointerInfo.getLocation();
        Point componentLocation;
        try {
            componentLocation = component.getLocationOnScreen();
        } catch (IllegalComponentStateException ignored) {
            return false;
        }

        Rectangle bounds = new Rectangle(componentLocation, component.getSize());
        return bounds.contains(pointerLocation);
    }

    private void showPreviewForHoveredFile() {
        String fileName = hoveredFileName;
        if (fileName == null || fileName.isBlank()) {
            hideImagePreview();
            return;
        }

        String cachedResolvedUrl = getResolvedPreviewUrl(fileName);
        if (cachedResolvedUrl != null) {
            ImageIcon cachedIcon = getCachedIcon(cachedResolvedUrl);
            if (cachedIcon != null) {
                displayIcon(fileName, cachedIcon);
                return;
            }
        }

        List<String> previewUrls = buildPreviewUrls(fileName, getPreviewLink(fileName));
        if (previewUrls.isEmpty()) {
            hideImagePreview();
            return;
        }

        ImageIcon cachedIcon = getFirstCachedPreview(previewUrls);
        if (cachedIcon != null) {
            displayIcon(fileName, cachedIcon);
            return;
        }

        showLoadingStatus();
        cancelLoadWorker();

        loadWorker = new SwingWorker<>() {
            @Override
            protected PreviewResult doInBackground() {
                PreviewResult result = tryLoadFirstPreview(previewUrls, true);
                if (result != null || isCancelled()) {
                    return result;
                }

                List<String> cmrPreviewUrls = findCmrBrowseUrls(fileName);
                if (!cmrPreviewUrls.isEmpty()) {
                    return tryLoadFirstPreview(cmrPreviewUrls, true);
                }
                return null;
            }

            @Override
            protected void done() {
                if (isCancelled() || !fileName.equals(hoveredFileName)) {
                    return;
                }

                try {
                    PreviewResult result = get();
                    if (result == null || result.icon == null) {
                        showUnavailableStatus();
                        return;
                    }

                    cachePreview(fileName, result.resolvedUrl, result.icon);
                    displayIcon(fileName, result.icon);
                } catch (Exception ignored) {
                    showUnavailableStatus();
                } finally {
                    loadWorker = null;
                }
            }
        };
        loadWorker.execute();
    }

    private void displayIcon(String fileName, ImageIcon icon) {
        displayedFileName = fileName;
        previewLabel.setIcon(icon);
        statusLabel.setText("");
        previewPanel.setPreferredSize(null);
        previewWindow.pack();
        positionPreviewWindow();
        previewWindow.setVisible(true);
        previewWindow.toFront();
    }

    private void showLoadingStatus() {
        if (previewWindow.isVisible() && previewLabel.getIcon() != null) {
            positionPreviewWindow();
            return;
        }

        showStatus("Loading preview...");
    }

    private void showUnavailableStatus() {
        if (previewWindow.isVisible() && previewLabel.getIcon() != null) {
            hideImagePreview();
            return;
        }

        showStatus("Preview unavailable");
    }

    private void showStatus(String text) {
        displayedFileName = null;
        previewLabel.setIcon(null);
        statusLabel.setText(text);
        previewPanel.setPreferredSize(new Dimension(LOADING_WIDTH, LOADING_HEIGHT));
        previewWindow.pack();
        positionPreviewWindow();
        previewWindow.setVisible(true);
        previewWindow.toFront();
    }

    private JWindow createPreviewWindow(Window owner) {
        JWindow window = owner == null ? new JWindow() : new JWindow(owner);
        window.setAlwaysOnTop(true);
        window.setFocusableWindowState(false);
        window.setAutoRequestFocus(false);
        window.setContentPane(previewPanel);
        return window;
    }

    private void ensurePreviewWindowOwner(Window owner) {
        if (owner == null || previewWindow.getOwner() == owner) {
            return;
        }

        boolean wasVisible = previewWindow.isVisible();
        Point location = previewWindow.getLocation();
        previewWindow.setVisible(false);
        previewWindow.dispose();

        previewWindow = createPreviewWindow(owner);
        previewWindow.pack();
        previewWindow.setLocation(location);
        if (wasVisible) {
            previewWindow.setVisible(true);
            previewWindow.toFront();
        }
    }

    private Image scaleImage(BufferedImage image) {
        int configuredHeight = Earthdata_Cloud_Controller.getPreferenceBrowseImageSize();
        int maxHeight = Math.max(Earthdata_Cloud_Controller.PROPERTY_IMAGE_PREVIEW_SIZE_MODE_MIN_VALUE,
                Math.min(configuredHeight, computeMaxHeight()));
        int scaledWidth = Math.max(1, (int) Math.round(image.getWidth() * (maxHeight / (double) image.getHeight())));
        return image.getScaledInstance(scaledWidth, maxHeight, Image.SCALE_SMOOTH);
    }

    private int computeMaxHeight() {
        Rectangle screenBounds = getVisibleScreenBounds();
        int screenLimit = Math.max(200, screenBounds.height - 2 * WINDOW_GAP);
        if (parentDialog == null || !parentDialog.isShowing()) {
            return screenLimit;
        }
        return Math.min(screenLimit, Math.max(200, parentDialog.getHeight() - 2 * WINDOW_GAP));
    }

    private void positionPreviewWindow() {
        Rectangle screenBounds = getVisibleScreenBounds();
        Dimension windowSize = previewWindow.getPreferredSize();

        int x = screenBounds.x + WINDOW_GAP;
        int y = screenBounds.y + WINDOW_GAP;

        if (parentDialog != null && parentDialog.isShowing()) {
            Point dialogLocation = parentDialog.getLocationOnScreen();
            int rightX = dialogLocation.x + parentDialog.getWidth() + WINDOW_GAP;
            int leftX = dialogLocation.x - windowSize.width - WINDOW_GAP;

            if (rightX + windowSize.width <= screenBounds.x + screenBounds.width) {
                x = rightX;
            } else if (leftX >= screenBounds.x) {
                x = leftX;
            } else {
                x = Math.max(screenBounds.x + WINDOW_GAP,
                        screenBounds.x + screenBounds.width - windowSize.width - WINDOW_GAP);
            }

            y = dialogLocation.y + Math.max(0, (parentDialog.getHeight() - windowSize.height) / 2);
        } else if (hoverLocationOnScreen != null) {
            x = hoverLocationOnScreen.x + WINDOW_GAP;
            y = hoverLocationOnScreen.y - windowSize.height / 2;
        } else if (previewAnchor != null && previewAnchor.isShowing()) {
            try {
                Point anchorLocation = previewAnchor.getLocationOnScreen();
                x = anchorLocation.x + previewAnchor.getWidth() + WINDOW_GAP;
                y = anchorLocation.y + WINDOW_GAP;
            } catch (IllegalComponentStateException ignored) {
                x = screenBounds.x + WINDOW_GAP;
                y = screenBounds.y + WINDOW_GAP;
            }
        }

        int maxX = screenBounds.x + screenBounds.width - windowSize.width - WINDOW_GAP;
        int maxY = screenBounds.y + screenBounds.height - windowSize.height - WINDOW_GAP;
        x = Math.max(screenBounds.x + WINDOW_GAP, Math.min(x, maxX));
        y = Math.max(screenBounds.y + WINDOW_GAP, Math.min(y, maxY));

        previewWindow.setLocation(x, y);
    }

    private Rectangle getVisibleScreenBounds() {
        GraphicsConfiguration configuration = null;
        if (parentDialog != null) {
            configuration = parentDialog.getGraphicsConfiguration();
        }
        if (configuration == null && previewAnchor != null) {
            configuration = previewAnchor.getGraphicsConfiguration();
        }
        if (configuration == null) {
            GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
            configuration = environment.getDefaultScreenDevice().getDefaultConfiguration();
        }

        Rectangle bounds = new Rectangle(configuration.getBounds());
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(configuration);
        bounds.x += insets.left;
        bounds.y += insets.top;
        bounds.width -= insets.left + insets.right;
        bounds.height -= insets.top + insets.bottom;
        return bounds;
    }

    private void cancelLoadWorker() {
        if (loadWorker != null && !loadWorker.isDone()) {
            loadWorker.cancel(true);
        }
        loadWorker = null;
    }

    private BufferedImage tryReadImage(String previewUrl) {
        try {
            return ImageIO.read(new URL(previewUrl));
        } catch (Exception ignored) {
            return null;
        }
    }

    private PreviewResult tryLoadFirstPreview(List<String> previewUrls, boolean scale) {
        for (String previewUrl : previewUrls) {
            BufferedImage image = tryReadImage(previewUrl);
            if (loadWorker != null && loadWorker.isCancelled()) {
                return null;
            }
            if (image != null) {
                Image previewImage = scale ? scaleImage(image) : image;
                return new PreviewResult(previewUrl, new ImageIcon(previewImage));
            }
        }
        return null;
    }

    private ImageIcon getFirstCachedPreview(List<String> previewUrls) {
        for (String previewUrl : previewUrls) {
            ImageIcon cachedIcon = getCachedIcon(previewUrl);
            if (cachedIcon != null) {
                return cachedIcon;
            }
        }
        return null;
    }

    static List<String> buildPreviewUrls(String fileName) {
        return buildPreviewUrls(fileName, null);
    }

    static List<String> buildPreviewUrls(String fileName, String preferredPreviewUrl) {
        Set<String> previewFileNames = new LinkedHashSet<>();
        previewFileNames.add(fileName);
        previewFileNames.addAll(getAlternatePreviewFileNames(fileName));

        List<String> previewUrls = new ArrayList<>();
        if (preferredPreviewUrl != null && !preferredPreviewUrl.isBlank()) {
            previewUrls.add(preferredPreviewUrl);
        }
        for (String previewFileName : previewFileNames) {
            if (previewFileName != null && !previewFileName.isBlank()) {
                String generatedUrl = "https://oceandata.sci.gsfc.nasa.gov/browse_images/" + previewFileName + ".png";
                if (!previewUrls.contains(generatedUrl)) {
                    previewUrls.add(generatedUrl);
                }
            }
        }
        return previewUrls;
    }

    private static List<String> getAlternatePreviewFileNames(String fileName) {
        Set<String> alternates = new LinkedHashSet<>();

        if (fileName.contains("_NRT")) {
            alternates.add(fileName.replace("_NRT", ""));
        } else {
            addInsertedNrtVariants(alternates, fileName);
            addProductNrtVariant(alternates, fileName);
        }

        if (fileName.contains(".NRT")) {
            alternates.add(fileName.replace(".NRT", ""));
        } else if (!fileName.contains("_NRT")) {
            alternates.add(insertBeforeExtension(fileName, ".NRT"));
        }

        return alternates.stream()
                .filter(candidate -> candidate != null && !candidate.isBlank() && !candidate.equals(fileName))
                .toList();
    }

    private static void addInsertedNrtVariants(Set<String> alternates, String fileName) {
        String beforeExtension = insertBeforeExtension(fileName, "_NRT");
        if (beforeExtension != null) {
            alternates.add(beforeExtension);
        }
    }

    private static String insertBeforeExtension(String fileName, String token) {
        int extensionDot = fileName.lastIndexOf('.');
        if (extensionDot <= 0) {
            return fileName + token;
        }
        return fileName.substring(0, extensionDot) + token + fileName.substring(extensionDot);
    }

    private static void addProductNrtVariant(Set<String> alternates, String fileName) {
        String[] parts = fileName.split("\\.");
        for (int i = 0; i < parts.length - 1; i++) {
            if (parts[i].matches("L\\d[A-Z]?") && i + 1 < parts.length - 1) {
                String product = parts[i + 1];
                if (!product.endsWith("_NRT")) {
                    parts[i + 1] = product + "_NRT";
                    alternates.add(String.join(".", parts));
                }
                return;
            }
        }
    }

    public void hideImagePreview() {
        previewWindow.setVisible(false);
        displayedFileName = null;
    }

    public void showFullImageDialog(String fileName, Component parent) {
        for (String previewUrl : buildPreviewUrls(fileName, getPreviewLink(fileName))) {
            ImageIcon cachedIcon = getCachedIcon(previewUrl);
            if (cachedIcon != null) {
                cachePreview(fileName, previewUrl, cachedIcon);
                showImageDialog(parent, cachedIcon);
                return;
            }
        }

        final JDialog loadingDialog = createLoadingDialog(parent);
        SwingWorker<PreviewResult, Void> fullImageWorker = new SwingWorker<>() {
            @Override
            protected PreviewResult doInBackground() {
                PreviewResult result = tryLoadFirstPreview(buildPreviewUrls(fileName, getPreviewLink(fileName)), false);
                if (result != null || isCancelled()) {
                    return result;
                }

                List<String> cmrPreviewUrls = findCmrBrowseUrls(fileName);
                if (!cmrPreviewUrls.isEmpty()) {
                    return tryLoadFirstPreview(cmrPreviewUrls, false);
                }
                return null;
            }

            @Override
            protected void done() {
                loadingDialog.dispose();
                try {
                    PreviewResult result = get();
                    if (result == null || result.icon == null) {
                        JOptionPane.showMessageDialog(parent, "Image not available.");
                        return;
                    }
                    cachePreview(fileName, result.resolvedUrl, result.icon);
                    showImageDialog(parent, result.icon);
                } catch (Exception ignored) {
                    JOptionPane.showMessageDialog(parent, "Failed to load image.");
                }
            }
        };

        fullImageWorker.execute();
        loadingDialog.setVisible(true);
    }

    private void showImageDialog(Component parent, ImageIcon icon) {
        JLabel label = new JLabel(icon);
        JScrollPane scrollPane = new JScrollPane(label);
        scrollPane.setPreferredSize(new Dimension(900, 700));
        JOptionPane.showMessageDialog(parent, scrollPane, "Full Image Preview", JOptionPane.PLAIN_MESSAGE);
    }

    private JDialog createLoadingDialog(Component parent) {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(owner, "Loading Preview", Dialog.ModalityType.MODELESS);
        dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        dialog.getContentPane().add(new JLabel("Loading full image preview...", SwingConstants.CENTER),
                BorderLayout.CENTER);
        dialog.setSize(260, 90);
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(parent);
        return dialog;
    }

    private String getResolvedPreviewUrl(String fileName) {
        synchronized (cacheLock) {
            return resolvedPreviewUrlCache.get(fileName);
        }
    }

    private String getPreviewLink(String fileName) {
        return previewLinkMap == null ? null : previewLinkMap.get(fileName);
    }

    private List<String> findCmrBrowseUrls(String fileName) {
        Set<String> cmrPreviewUrls = new LinkedHashSet<>();
        for (String candidateFileName : getAlternatePreviewFileNames(fileName)) {
            addCmrBrowseUrls(candidateFileName, cmrPreviewUrls);
        }
        return new ArrayList<>(cmrPreviewUrls);
    }

    private void addCmrBrowseUrls(String fileName, Set<String> cmrPreviewUrls) {
        HttpURLConnection connection = null;
        try {
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8);
            URL url = new URL("https://cmr.earthdata.nasa.gov/search/granules.json?provider=OB_CLOUD"
                    + "&producer_granule_id=" + encodedFileName
                    + "&page_size=1");
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                JSONObject response = new JSONObject(new JSONTokener(reader));
                JSONArray entries = response.getJSONObject("feed").optJSONArray("entry");
                if (entries == null || entries.isEmpty()) {
                    return;
                }

                JSONArray links = entries.getJSONObject(0).optJSONArray("links");
                if (links == null) {
                    return;
                }

                for (int i = 0; i < links.length(); i++) {
                    JSONObject link = links.getJSONObject(i);
                    String href = link.optString("href", "");
                    String rel = link.optString("rel", "");
                    String type = link.optString("type", "");
                    if (!href.isBlank() && (rel.endsWith("/browse#") || "image/png".equalsIgnoreCase(type))) {
                        cmrPreviewUrls.add(href);
                    }
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private ImageIcon getCachedIcon(String previewUrl) {
        synchronized (cacheLock) {
            return previewCache.get(previewUrl);
        }
    }

    private void cachePreview(String fileName, String previewUrl, ImageIcon icon) {
        synchronized (cacheLock) {
            resolvedPreviewUrlCache.put(fileName, previewUrl);
            if (icon != null) {
                previewCache.put(previewUrl, icon);
            }
        }
    }
}
