package gov.nasa.gsfc.seadas.earthdatacloud.util;

import gov.nasa.gsfc.seadas.earthdatacloud.ui.BrowseImagePreview;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

public class ImagePreviewHandler extends Component {

    private static final String BASE_IMAGE_URL = "https://oceandata.sci.gsfc.nasa.gov/browse_images/";
    private JWindow previewWindow;
    private JLabel previewLabel;
    private Timer hideTimer;
    public ImagePreviewHandler() {
        previewWindow = new JWindow();
        previewLabel = new JLabel();
        previewWindow.getContentPane().add(previewLabel);
        previewWindow.setAlwaysOnTop(true);
        previewWindow.setFocusableWindowState(false);
        previewWindow.setType(Window.Type.POPUP);
    }
    /**
     * Generates a preview URL based on file name.
     */
    public static String getPreviewUrl(String fileName) {
        return BASE_IMAGE_URL + fileName + ".png";
    }

    public void attachToTable(JTable table) {
        table.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());

                if (row >= 0 && col == 0) {
                    String fileName = (String) table.getValueAt(row, col);
                    showImagePreview(fileName, e.getLocationOnScreen());
                } else {
                    hideImagePreview();
                }
            }
        });

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                hideImagePreview();
            }
        });
    }

    /**
     * Shows an image dialog for the given image URL.
     */

    public static void showImageInDialog(Component parentComponent, String imageUrl) {
        try {
            Image originalImage = ImageIO.read(new URL(imageUrl));
            if (originalImage == null) {
                JOptionPane.showMessageDialog(parentComponent, "Image not available or not valid.");
                return;
            }

            ImageIcon icon = new ImageIcon(originalImage);
            JLabel label = new JLabel(icon);
            JScrollPane imageScrollPane = new JScrollPane(label);

            JSlider zoomSlider = new JSlider(10, 400, 100); // zoom range: 10% to 400%
            zoomSlider.setMajorTickSpacing(50);
            zoomSlider.setPaintTicks(true);
            zoomSlider.setPaintLabels(true);

            zoomSlider.addChangeListener(e -> {
                int zoomPercent = zoomSlider.getValue();
                int newWidth = originalImage.getWidth(null) * zoomPercent / 100;
                int newHeight = originalImage.getHeight(null) * zoomPercent / 100;
                Image scaled = originalImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
                label.setIcon(new ImageIcon(scaled));
            });

            JPanel panel = new JPanel(new BorderLayout());
            panel.add(imageScrollPane, BorderLayout.CENTER);
            panel.add(zoomSlider, BorderLayout.SOUTH);
            panel.setPreferredSize(new Dimension(800, 600));

            JOptionPane.showMessageDialog(parentComponent, panel, "Image Preview (Zoom)", JOptionPane.PLAIN_MESSAGE);

        } catch (IOException e) {
            JOptionPane.showMessageDialog(parentComponent, "Failed to load image preview.");
            e.printStackTrace();
        }
    }

    private void showImagePreview(String fileName, Point locationOnScreen) {
        String url = getPreviewUrl(fileName);
        try {
            BufferedImage image = ImageIO.read(new URL(url));
            if (image == null) return;

            Image scaled = image.getScaledInstance(200, -1, Image.SCALE_SMOOTH);
            previewLabel.setIcon(new ImageIcon(scaled));
            previewWindow.pack();

            previewWindow.setLocation(locationOnScreen.x + 15, locationOnScreen.y + 15);
            previewWindow.setVisible(true);

            if (hideTimer != null && hideTimer.isRunning()) {
                hideTimer.stop();
            }

        } catch (IOException ex) {
            hideImagePreview();
        }
    }

    public void showFullImageDialog(String fileName, Component parent) {
        try {
            String url = getPreviewUrl(fileName);
            BufferedImage image = ImageIO.read(new URL(url));
            if (image != null) {
                JLabel label = new JLabel(new ImageIcon(image));
                JScrollPane scrollPane = new JScrollPane(label);
                scrollPane.setPreferredSize(new Dimension(800, 600));
                JOptionPane.showMessageDialog(parent, scrollPane, "Full Preview", JOptionPane.PLAIN_MESSAGE);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(parent, "Failed to load image preview.");
        }
    }
    private void hideImagePreview() {
        if (hideTimer != null && hideTimer.isRunning()) return;

        hideTimer = new Timer(500, e -> previewWindow.setVisible(false));
        hideTimer.setRepeats(false);
        hideTimer.start();
    }
}
