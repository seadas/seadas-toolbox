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

    private SwingWorker<ImageIcon, Void> loadWorker;
    private String hoveredFileName;
    private String displayedFileName;
    private Point hoverLocationOnScreen;
    private JDialog parentDialog;
    private Component previewAnchor;

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

        String cachedResolvedUrl = resolvedPreviewUrlCache.get(fileName);
        if (cachedResolvedUrl != null) {
            ImageIcon cachedIcon = previewCache.get(cachedResolvedUrl);
            if (cachedIcon != null) {
                displayIcon(fileName, cachedIcon);
                return;
            }
        }

        List<String> previewUrls = getPreviewUrls(fileName);
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
            protected ImageIcon doInBackground() throws Exception {
                for (String previewUrl : previewUrls) {
                    BufferedImage image = tryReadImage(previewUrl);
                    if (isCancelled()) {
                        return null;
                    }
                    if (image != null) {
                        resolvedPreviewUrlCache.put(fileName, previewUrl);
                        return new ImageIcon(scaleImage(image));
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
                    ImageIcon icon = get();
                    if (icon == null) {
                        showStatus("Preview unavailable");
                        return;
                    }

                    String resolvedUrl = resolvedPreviewUrlCache.get(fileName);
                    if (resolvedUrl != null) {
                        previewCache.put(resolvedUrl, icon);
                    }
                    displayIcon(fileName, icon);
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
            ImageIcon cachedIcon = previewCache.get(previewUrl);
            if (cachedIcon != null) {
                return cachedIcon;
            }
        }
        return null;
    }

    private List<String> getPreviewUrls(String fileName) {
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

    private List<String> getAlternatePreviewFileNames(String fileName) {
        Set<String> alternates = new LinkedHashSet<>();

        if (fileName.contains("_NRT")) {
            alternates.add(fileName.replace("_NRT", ""));
        } else {
            addInsertedNrtVariants(alternates, fileName);
        }

        if (fileName.contains(".NRT")) {
            alternates.add(fileName.replace(".NRT", ""));
        } else {
            alternates.add(insertBeforeExtension(fileName, ".NRT"));
        }

        return alternates.stream()
                .filter(candidate -> candidate != null && !candidate.isBlank() && !candidate.equals(fileName))
                .toList();
    }

    private void addInsertedNrtVariants(Set<String> alternates, String fileName) {
        String beforeVersion = insertBeforeLastDotBeforeExtension(fileName, "_NRT");
        if (beforeVersion != null) {
            alternates.add(beforeVersion);
        }

        String beforeExtension = insertBeforeExtension(fileName, "_NRT");
        if (beforeExtension != null) {
            alternates.add(beforeExtension);
        }
    }

    private String insertBeforeLastDotBeforeExtension(String fileName, String token) {
        int extensionDot = fileName.lastIndexOf('.');
        if (extensionDot <= 0) {
            return insertBeforeExtension(fileName, token);
        }

        int insertPosition = fileName.lastIndexOf('.', extensionDot - 1);
        if (insertPosition <= 0) {
            return insertBeforeExtension(fileName, token);
        }
        return fileName.substring(0, insertPosition) + token + fileName.substring(insertPosition);
    }

    private String insertBeforeExtension(String fileName, String token) {
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
        for (String previewUrl : getPreviewUrls(fileName)) {
            ImageIcon cachedIcon = previewCache.get(previewUrl);
            if (cachedIcon != null) {
                resolvedPreviewUrlCache.put(fileName, previewUrl);
                showImageDialog(parent, cachedIcon);
                return;
            }
        }

        for (String previewUrl : getPreviewUrls(fileName)) {
            BufferedImage image = tryReadImage(previewUrl);
            if (image != null) {
                ImageIcon icon = new ImageIcon(image);
                previewCache.put(previewUrl, icon);
                resolvedPreviewUrlCache.put(fileName, previewUrl);
                showImageDialog(parent, icon);
                return;
            }
        }

        JOptionPane.showMessageDialog(parent, "Image not available.");
    }

    private void showImageDialog(Component parent, ImageIcon icon) {
        JLabel label = new JLabel(icon);
        JScrollPane scrollPane = new JScrollPane(label);
        scrollPane.setPreferredSize(new Dimension(900, 700));
        JOptionPane.showMessageDialog(parent, scrollPane, "Full Image Preview", JOptionPane.PLAIN_MESSAGE);
    }
}
