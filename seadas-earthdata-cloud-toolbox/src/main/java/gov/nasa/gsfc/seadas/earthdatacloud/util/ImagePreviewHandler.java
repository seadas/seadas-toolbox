package gov.nasa.gsfc.seadas.earthdatacloud.util;

import gov.nasa.gsfc.seadas.earthdatacloud.ui.BrowseImagePreview;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URL;

public class ImagePreviewHandler extends Component {

    private static final String BASE_IMAGE_URL = "https://oceandata.sci.gsfc.nasa.gov/browse_images/";

    /**
     * Generates a preview URL based on file name.
     */
    public static String getPreviewUrl(String fileName) {
        return BASE_IMAGE_URL + fileName + ".png";
    }

    /**
     * Shows an image dialog for the given image URL.
     */
    public void showFullImageDialog(String fileName, Component parentComponent) {
        String imageUrl = getPreviewUrl(fileName);
        if (imageUrl == null) return;

        try {
            Image image = ImageIO.read(new URL(imageUrl));
            if (image != null) {
                ImageIcon icon = new ImageIcon(image);
                JLabel label = new JLabel(icon);
                JScrollPane scrollPane = new JScrollPane(label);
                scrollPane.setPreferredSize(new Dimension(600, 600));
                JOptionPane.showMessageDialog(parentComponent, scrollPane, "Full Preview", JOptionPane.PLAIN_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(parentComponent, "Image could not be loaded.");
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(parentComponent, "Failed to load image preview.");
            e.printStackTrace();
        }
    }

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
    public static void showImagePreview(String fileName, Point screenLocation, JWindow imagePreviewWindow, JLabel imageLabel) {
        if (imagePreviewWindow == null) {
            imagePreviewWindow = new JWindow();
            imageLabel = new JLabel();
            imagePreviewWindow.getContentPane().add(imageLabel);
        }

        try {
            String imageUrl = BrowseImagePreview.getPreviewUrl(fileName);
            ImageIcon icon = BrowseImagePreview.loadPreviewImage(fileName);
            if (icon != null) {
                imageLabel.setIcon(icon);
                imagePreviewWindow.pack();
                imagePreviewWindow.setLocation(screenLocation.x + 20, screenLocation.y + 20);
                imagePreviewWindow.setVisible(true);
            } else {
                hideImagePreview(imagePreviewWindow);
            }
        } catch (Exception e) {
            hideImagePreview(imagePreviewWindow);
        }
    }

    public static void hideImagePreview(JWindow imagePreviewWindow) {
        if (imagePreviewWindow != null) {
            imagePreviewWindow.setVisible(false);
        }
    }
}
