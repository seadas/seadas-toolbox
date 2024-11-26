package gov.nasa.gsfc.seadas.earthdatacloud.ui;

import javax.swing.*;
import java.awt.*;

class SpinningImagePanel extends JPanel {

    private Image image;

    // Constructor to load the image
    public SpinningImagePanel(String imagePath) {
        this.image = new ImageIcon(imagePath).getImage(); // Load the image from the given path
    }

    // Override paintComponent to draw the image
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Draw the image at the top-left corner of the panel (0,0)
        if (image != null) {
            g.drawImage(image, 0, 0, getWidth(), getHeight(), this);
        }
    }
}
