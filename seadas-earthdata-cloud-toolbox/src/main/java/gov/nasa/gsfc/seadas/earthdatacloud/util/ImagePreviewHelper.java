package gov.nasa.gsfc.seadas.earthdatacloud.util;

import gov.nasa.gsfc.seadas.earthdatacloud.preferences.Earthdata_Cloud_Controller;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ImagePreviewHelper {

    private static final int HOVER_DELAY_MS = 250;
    private static final int LOADING_WIDTH = 240;
    private static final int LOADING_HEIGHT = 96;
    private static final int WINDOW_GAP = 10;
    private static final int MAX_CACHE_SIZE = 40;

    private final JWindow previewWindow;
    private final JPanel previewPanel;
    private final JLabel previewLabel;
    private final JLabel statusLabel;
    private final Timer hoverTimer;
    private final Map<String, ImageIcon> previewCache;
    private final Map<String, String> resolvedPreviewUrlCache;
    private final Object cacheLock = new Object();

    private SwingWorker<PreviewResult, Void> loadWorker;
    private String hoveredFileName;
    private String displayedFileName;
    private Point hoverLocationOnScreen;
    private JDialog parentDialog;
    private Component previewAnchor;

    private static final class PreviewResult {
        private final String resolvedUrl;
        private final ImageIcon icon;

        private PreviewResult(String resolvedUrl, ImageIcon icon) {
            this.resolvedUrl = resolvedUrl;
            this.icon = icon;
        }
    }

    public ImagePreviewHelper() {
        previewWindow = new JWindow();
        previewPanel = new JPanel(new BorderLayout(0, 6));
        previewLabel = new JLabel("", SwingConstants.CENTER);
        statusLabel = new JLabel("", SwingConstants.CENTER);
        hoverTimer = new Timer(HOVER_DELAY_MS, e -> showPreviewForHoveredFile());
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

        previewLabel.setHorizontalAlignment(SwingConstants.CENTER);
        previewLabel.setVerticalAlignment(SwingConstants.CENTER);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));

        previewPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(150, 150, 150)),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        previewPanel.setBackground(UIManager.getColor("Panel.background"));
        previewPanel.add(previewLabel, BorderLayout.CENTER);
        previewPanel.add(statusLabel, BorderLayout.SOUTH);

        previewWindow.setAlwaysOnTop(true);
        previewWindow.setFocusableWindowState(false);
        previewWindow.setContentPane(previewPanel);
    }

    public void attachToTable(JTable table, Map<String, String> fileLinkMap, JDialog parentDialog) {
        this.parentDialog = parentDialog;
        this.previewAnchor = table;

        table.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
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
                clearHoverState();
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
        cancelLoadWorker();
        hideImagePreview();
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

        List<String> previewUrls = buildPreviewUrls(fileName);
        if (previewUrls.isEmpty()) {
            hideImagePreview();
            return;
        }

        ImageIcon cachedIcon = getFirstCachedPreview(previewUrls);
        if (cachedIcon != null) {
            displayIcon(fileName, cachedIcon);
            return;
        }

        showStatus("Loading preview...");
        cancelLoadWorker();

        loadWorker = new SwingWorker<>() {
            @Override
            protected PreviewResult doInBackground() {
                for (String previewUrl : previewUrls) {
                    BufferedImage image = tryReadImage(previewUrl);
                    if (isCancelled()) {
                        return null;
                    }
                    if (image != null) {
                        return new PreviewResult(previewUrl, new ImageIcon(scaleImage(image)));
                    }
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
                        showStatus("Preview unavailable");
                        return;
                    }

                    cachePreview(fileName, result.resolvedUrl, result.icon);
                    displayIcon(fileName, result.icon);
                } catch (Exception ignored) {
                    showStatus("Preview unavailable");
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
    }

    private void showStatus(String text) {
        displayedFileName = null;
        previewLabel.setIcon(null);
        statusLabel.setText(text);
        previewPanel.setPreferredSize(new Dimension(LOADING_WIDTH, LOADING_HEIGHT));
        previewWindow.pack();
        positionPreviewWindow();
        previewWindow.setVisible(true);
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
        Set<String> previewFileNames = new LinkedHashSet<>();
        previewFileNames.add(fileName);
        previewFileNames.addAll(getAlternatePreviewFileNames(fileName));

        List<String> previewUrls = new ArrayList<>();
        for (String previewFileName : previewFileNames) {
            if (previewFileName != null && !previewFileName.isBlank()) {
                previewUrls.add("https://oceandata.sci.gsfc.nasa.gov/browse_images/" + previewFileName + ".png");
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

    public void hideImagePreview() {
        previewWindow.setVisible(false);
        displayedFileName = null;
    }

    public void showFullImageDialog(String fileName, Component parent) {
        for (String previewUrl : buildPreviewUrls(fileName)) {
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
                for (String previewUrl : buildPreviewUrls(fileName)) {
                    BufferedImage image = tryReadImage(previewUrl);
                    if (isCancelled()) {
                        return null;
                    }
                    if (image != null) {
                        return new PreviewResult(previewUrl, new ImageIcon(image));
                    }
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
