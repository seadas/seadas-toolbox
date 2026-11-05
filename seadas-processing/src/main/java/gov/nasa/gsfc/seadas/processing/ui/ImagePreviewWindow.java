package gov.nasa.gsfc.seadas.processing.ui;

import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.Dialogs;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

/**
 * Shows an image file (such as a PNG written by a processor) in a window, scaled to fit, with a button to
 * open it in the system's image viewer. The window does not block SeaDAS.
 */
public class ImagePreviewWindow extends JDialog {

    private ImagePreviewWindow(File file, BufferedImage image) {
        super(SnapApp.getDefault().getMainFrame(), file.getName(), ModalityType.MODELESS);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JButton openButton = new JButton("Open in Image Viewer");
        openButton.setToolTipText(file.getAbsolutePath());
        openButton.setEnabled(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN));
        openButton.addActionListener(e -> {
            try {
                Desktop.getDesktop().open(file);
            } catch (IOException | RuntimeException ex) {
                Dialogs.showError("Could not open " + file.getAbsolutePath() + ":\n" + ex.getMessage());
            }
        });
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(openButton);
        buttons.add(closeButton);

        getContentPane().add(new ScaledImagePanel(image), BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);

        // Most of the screen, keeping the image's aspect ratio
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        double scale = Math.min(1.0, Math.min(0.8 * screen.width / image.getWidth(), 0.75 * screen.height / image.getHeight()));
        getContentPane().setPreferredSize(new Dimension((int) (image.getWidth() * scale),
                (int) (image.getHeight() * scale) + buttons.getPreferredSize().height));
        pack();
        setLocationRelativeTo(getOwner());
    }

    /**
     * Opens a window showing the image file, or shows an error if it can't be read.
     */
    public static void show(File file) {
        try {
            BufferedImage image = file == null ? null : ImageIO.read(file);
            if (image == null) {
                throw new IOException("not a readable image");
            }
            new ImagePreviewWindow(file, image).setVisible(true);
        } catch (IOException e) {
            SystemUtils.LOG.log(Level.WARNING, "Could not show image " + file, e);
            Dialogs.showError("Could not show the image " + (file == null ? "" : file.getAbsolutePath()) + ":\n" + e.getMessage());
        }
    }

    /** Draws the image as large as fits in the panel, centered, keeping its aspect ratio. */
    private static class ScaledImagePanel extends JPanel {
        private final BufferedImage image;

        ScaledImagePanel(BufferedImage image) {
            this.image = image;
            setBackground(Color.WHITE);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            double scale = Math.min((double) getWidth() / image.getWidth(), (double) getHeight() / image.getHeight());
            int w = (int) (image.getWidth() * scale);
            int h = (int) (image.getHeight() * scale);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.drawImage(image, (getWidth() - w) / 2, (getHeight() - h) / 2, w, h, null);
            g2.dispose();
        }
    }
}
