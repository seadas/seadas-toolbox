package gov.nasa.gsfc.seadas.earthdatacloud.ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BrowseImagePreview {
    private static final String BASE_URL = "https://oceandata.sci.gsfc.nasa.gov/browse_images/";

    public static String getPreviewUrl(String dataFilename) {
        return BASE_URL + getImageFilename(dataFilename);
    }

    public static boolean isImageAvailable(String filename) {
        try {
            URL url = new URL(getPreviewUrl(filename));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            int responseCode = connection.getResponseCode();
            return (responseCode == HttpURLConnection.HTTP_OK);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String getFullImageUrl(String dataFilename) {
        // Ensure the filename ends with .nc, then add .png
        if (!dataFilename.endsWith(".nc")) {
            return null;  // Invalid or unsupported file
        }
        return BASE_URL + dataFilename + ".png";
    }


    public static ImageIcon loadPreviewImage(String filename) {
        try {
            URL imageUrl = new URL(getPreviewUrl(filename));
            Image image = ImageIO.read(imageUrl);
            if (image == null) return null;
            Image scaledImage = image.getScaledInstance(150, -1, Image.SCALE_SMOOTH);
            return new ImageIcon(scaledImage);
        } catch (IOException e) {
            return null;
        }
    }

    private static String getImageFilename(String dataFilename) {
        return dataFilename + ".png";  // Keep the .nc and add .png
    }
}
