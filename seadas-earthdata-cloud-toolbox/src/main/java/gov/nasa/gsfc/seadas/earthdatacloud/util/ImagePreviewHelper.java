package gov.nasa.gsfc.seadas.earthdatacloud.util;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.net.URL;
import java.util.Map;

public class ImagePreviewHelper {
    private JWindow previewWindow;
    private JLabel previewLabel;
    private String currentImageUrl = null;

    public ImagePreviewHelper() {
        previewWindow = new JWindow();
        previewLabel = new JLabel();
        previewWindow.getContentPane().add(previewLabel);
        previewWindow.setSize(300, 300);
        previewWindow.setAlwaysOnTop(true);
    }

    public void attachToTable(JTable table, Map<String, String> fileLinkMap) {
        table.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());

                if (row >= 0 && col == 0) { // Only for file name column
                    String fileName = (String) table.getValueAt(row, 0);
                    String imageUrl = getPreviewUrl(fileName);
                    if (imageUrl != null && !imageUrl.equals(currentImageUrl)) {
                        showImagePreview(imageUrl, table, e.getLocationOnScreen());
                        currentImageUrl = imageUrl;
                    }
                } else {
                    hideImagePreview();
                }
            }
        });

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                hideImagePreview();
            }
        });
    }

    private String getPreviewUrl(String fileName) {
        return "https://oceandata.sci.gsfc.nasa.gov/browse_images/" + fileName + ".png";
    }

    private void showImagePreview(String imageUrl, Component parent, Point screenLocation) {
        try {
            Image image = ImageIO.read(new URL(imageUrl));
            if (image != null) {
                Image scaled = image.getScaledInstance(300, -1, Image.SCALE_SMOOTH);
                previewLabel.setIcon(new ImageIcon(scaled));
                previewWindow.pack();
                previewWindow.setLocation(screenLocation.x + 15, screenLocation.y + 15);
                previewWindow.setVisible(true);
            } else {
                hideImagePreview();
            }
        } catch (Exception e) {
            hideImagePreview();
        }
    }

    private void hideImagePreview() {
        previewWindow.setVisible(false);
        currentImageUrl = null;
    }

    public void showFullImageDialog(String fileName, Component parent) {
        try {
            String imageUrl = getPreviewUrl(fileName);
            Image image = ImageIO.read(new URL(imageUrl));
            if (image != null) {
                ImageIcon icon = new ImageIcon(image);
                JLabel label = new JLabel(icon);
                JScrollPane scrollPane = new JScrollPane(label);
                scrollPane.setPreferredSize(new Dimension(700, 700));
                JOptionPane.showMessageDialog(parent, scrollPane, "Full Image Preview", JOptionPane.PLAIN_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(parent, "Image not available.");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(parent, "Failed to load image.");
        }
    }
}
