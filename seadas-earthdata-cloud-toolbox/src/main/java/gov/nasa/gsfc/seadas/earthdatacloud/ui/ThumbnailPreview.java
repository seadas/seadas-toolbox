package gov.nasa.gsfc.seadas.earthdatacloud.ui;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.io.IOException;
import javax.imageio.ImageIO;

public class ThumbnailPreview {
    private final JWindow previewWindow;
    private final JLabel imageLabel;

    public ThumbnailPreview(JFrame parentFrame) {
        previewWindow = new JWindow(parentFrame);
        previewWindow.setAlwaysOnTop(true);
        previewWindow.setFocusableWindowState(false);
        previewWindow.setBackground(new Color(0, 0, 0, 0)); // Transparent background

        imageLabel = new JLabel();
        previewWindow.getContentPane().add(imageLabel);
        previewWindow.pack();
    }

    public void showImage(String imageUrl, Point location) {
        try {
            Image img = ImageIO.read(new URL(imageUrl));
            if (img != null) {
                Image scaled = img.getScaledInstance(150, -1, Image.SCALE_SMOOTH);
                imageLabel.setIcon(new ImageIcon(scaled));
                previewWindow.pack();
                previewWindow.setLocation(location);
                previewWindow.setVisible(true);
            }
        } catch (IOException e) {
            // Silently fail or log
        }
    }

    public void hide() {
        previewWindow.setVisible(false);
    }
}
