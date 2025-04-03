package gov.nasa.gsfc.seadas.earthdatacloud.ui;

import java.awt.Image;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JToolTip;

public class BrowseImagePreview {
    private static final String BASE_URL = "https://oceandata.sci.gsfc.nasa.gov/browse_images/";

    public static boolean isImageAvailable(String filename) {
        try {
            URL url = new URL(BASE_URL + getImageFilename(filename));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            int responseCode = connection.getResponseCode();
            return (responseCode == HttpURLConnection.HTTP_OK);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static String getImageFilename(String dataFilename) {
        // Implement the mapping logic between data files and browse images
        // This is a placeholder and needs to be adjusted based on actual naming conventions
        return dataFilename.replaceAll("\\.nc$", ".png");
    }

    public static ImageIcon loadPreviewImage(String filename) {
        try {
            URL imageUrl = new URL(BASE_URL + getImageFilename(filename));
            Image image = ImageIO.read(imageUrl);
            // Scale the image to an appropriate size for preview
            Image scaledImage = image.getScaledInstance(200, -1, Image.SCALE_SMOOTH);
            return new ImageIcon(scaledImage);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static JToolTip createImageToolTip(String filename) {
        JToolTip toolTip = new JToolTip();
        ImageIcon icon = loadPreviewImage(filename);
        if (icon != null) {
            JLabel label = new JLabel(icon);
            toolTip.setComponent(label);
        } else {
            toolTip.setTipText("Preview not available");
        }
        return toolTip;
    }
}
