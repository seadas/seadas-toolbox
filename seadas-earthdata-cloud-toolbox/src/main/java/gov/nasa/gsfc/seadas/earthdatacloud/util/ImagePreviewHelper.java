package gov.nasa.gsfc.seadas.earthdatacloud.util;

import gov.nasa.gsfc.seadas.earthdatacloud.preferences.Earthdata_Cloud_Controller;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

public class ImagePreviewHelper {
    private JWindow previewWindow;
    private JLabel previewLabel;
    private String currentImageUrl = null;

    private volatile String hoveringFileName = null;
    private String finishedFileName = null;
    boolean startingUp = true;
    private boolean previewIsDisplayed = false;
    Thread th = null;
    private Map<String, String> previewLinkMap;
    private final Map<String, PreviewImage> previewImageCache = new HashMap<>();
    private volatile long previewRequestId = 0;

    public ImagePreviewHelper() {
        previewWindow = new JWindow();
        previewLabel = new JLabel();
        previewWindow.getContentPane().add(previewLabel);
        previewWindow.setSize(300, 300);
        previewWindow.setAlwaysOnTop(true);
    }



    public void attachToTable(JTable table, Map<String, String> previewLinkMap, JDialog parentDialog) {
        this.previewLinkMap = previewLinkMap;

        table.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {

                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());


                boolean hoveringFileNameChanged = false;

                if (row >= 0 & row < table.getRowCount()) { // for all columns in the row
                    String fileName = (String) table.getValueAt(row, 0);

                    hoveringFileNameChanged = !stringCompareEquals(fileName, hoveringFileName);

                    if (fileName != null && hoveringFileNameChanged) {
                        hoveringFileName = fileName;
                        previewRequestId++;

                        table.setBackground(Color.WHITE);
                        table.setForeground(Color.BLACK);

                        if (row >= 0 && row < table.getRowCount()) {
                            table.setRowSelectionInterval(row, row);
                            table.setSelectionBackground(new Color(0, 100, 200));
                            table.setSelectionForeground(Color.WHITE);
                        }


                        String imageUrl = getPreviewUrl(fileName);
                        if (imageUrl != null && !imageUrl.equals(currentImageUrl)) {


                            if (th == null || !th.isAlive()) {
                                startingUp = true;
                                Runnable r = new Runnable() {
                                    public void run() {
                                        int i =0;
                                        String hoveringFileNameStart = hoveringFileName;
                                        finishedFileName = null;

                                        while (th != null && th.isAlive() && i < 1000) { // stay alive for 100 seconds

                                            sleepPreviewThread(100);

                                            String hoveringFileNameCurrent = hoveringFileName; // lock this as it could become null
                                            Point hoveringLocationCurrent = e.getLocationOnScreen();

                                            if (!stringCompareEquals(hoveringFileNameStart, hoveringFileNameCurrent)) {
                                                finishedFileName = null;
                                                hoveringFileNameStart = hoveringFileName; // get latest
                                                i = 0;
                                                continue;  // it has moved continue and sleep
                                            }

                                            if (hoveringFileNameCurrent == null) {
                                                if (finishedFileName != null) {
                                                    hideImagePreview();
                                                    finishedFileName = null;
                                                }
                                                hoveringFileNameStart =  hoveringFileName; // get latest
                                                continue;
                                            }



                                            if (hoveringFileNameCurrent != null) {
                                                if (!stringCompareEquals(hoveringFileNameCurrent, finishedFileName)) {
                                                    String imageUrl = showImagePreview(hoveringFileNameCurrent, table,
                                                            hoveringLocationCurrent, parentDialog);
                                                    if (imageUrl != null) {
                                                        currentImageUrl = imageUrl;
                                                    }
                                                    if (imageUrl != null && stringCompareEquals(hoveringFileNameCurrent,
                                                            hoveringFileName)) {
                                                        finishedFileName = hoveringFileNameCurrent;
                                                    } else {
                                                        finishedFileName = null;
                                                    }
                                                }
                                            }

                                            if (finishedFileName == null) {
                                                i = 0;
                                            } else {
                                                i++;
                                            }

                                            hoveringFileNameStart =  hoveringFileName; // get latest

                                        }


                                        hideImagePreview(); // just in case
                                        killImagePreviewThread();

                                    }
                                };
                                th = new Thread(r);
                                th.start();
                            }

                        }
                    }

                    hoveringFileName = fileName;
                } else {
                    hoveringFileName = null;
                    previewRequestId++;

                    if (hoveringFileNameChanged) {
                        table.setBackground(Color.WHITE);
                        table.setForeground(Color.BLACK);
                        table.setSelectionBackground(Color.WHITE);
                        table.setSelectionForeground(Color.BLACK);
                        table.setBackground(Color.WHITE);
                        table.setForeground(Color.BLACK);
                        table.setBorder(BorderFactory.createEmptyBorder());




                        hideImagePreview();
                        finishedFileName = null;
                    }
                }


            }
        });




        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {

                hoveringFileName = null;
                previewRequestId++;
                table.setSelectionBackground(Color.WHITE);
                table.setSelectionForeground(Color.BLACK);
                table.setBackground(Color.WHITE);
                table.setForeground(Color.BLACK);
                table.setBackground(Color.WHITE);
                table.setForeground(Color.BLACK);
                table.setBorder(BorderFactory.createEmptyBorder());

                hideImagePreview();
            }
        });
    }


    private boolean stringCompareEquals(String string1, String string2) {

        if (string1 == null && string2 != null) {
            return false;
        }

        if (string1 != null && string2 == null) {
            return false;
        }

        if (string1 == null && string2 == null) {
            return true;
        }

        if (string1.equalsIgnoreCase(string2)) {
            return true;
        } else {
            return  false;
        }

    }

    private void sleepPreviewThread(long milliSeconds) {
        try {
            Thread.sleep(milliSeconds);
        } catch (InterruptedException e3) {
            Thread.currentThread().interrupt();
            return;
        }
    }



    private void killImagePreviewThread() {
        if (th != null) {
            th.interrupt();

            int i = 0;
            System.out.println("Killing from exit th alive num iter =" + i);

            while (th != null && th.isAlive() && i < 100) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e3) {
                    Thread.currentThread().interrupt();
                    return;
                }

                System.out.println("OUT th alive num iter =" + i);

                i++;
            }
            th = null;
            System.out.println("Killed from exit th alive num iter =" + i);

        }
    }




    private String getPreviewUrl(String fileName) {
        return "https://oceandata.sci.gsfc.nasa.gov/browse_images/" + fileName + ".png";
    }

    static List<String> buildPreviewUrls(String fileName) {
        Set<String> previewFileNames = new LinkedHashSet<>();
        previewFileNames.add(fileName);
        previewFileNames.addAll(getAlternatePreviewFileNames(fileName));

        List<String> previewUrls = new ArrayList<>();
        for (String previewFileName : previewFileNames) {
            if (previewFileName != null && !previewFileName.isBlank()) {
                String filePathPreviewUrl = getPreviewUrlWithFilePath(previewFileName);
                if (filePathPreviewUrl != null) {
                    previewUrls.add(filePathPreviewUrl);
                }
                previewUrls.add(getPreviewUrlForFileName(previewFileName));
            }
        }
        return previewUrls;
    }

    private static List<String> getAlternatePreviewFileNames(String fileName) {
        Set<String> alternates = new LinkedHashSet<>();
        Set<String> fileNameCandidates = new LinkedHashSet<>();
        fileNameCandidates.add(fileName);
        fileNameCandidates.addAll(getProductNameAliases(fileName));
        alternates.addAll(getProductNameAliases(fileName));

        for (String candidate : fileNameCandidates) {
            if (candidate.contains("_NRT")) {
                alternates.add(candidate.replace("_NRT", ""));
            } else if (!candidate.contains(".NRT")) {
                addInsertedNrtVariants(alternates, candidate);
            }

            if (candidate.contains(".NRT")) {
                alternates.add(candidate.replace(".NRT", ""));
            } else if (!candidate.contains("_NRT")) {
                alternates.add(insertBeforeExtension(candidate, ".NRT"));
            }
        }

        return alternates.stream()
                .filter(candidate -> candidate != null && !candidate.isBlank() && !candidate.equals(fileName))
                .toList();
    }

    private static List<String> getProductNameAliases(String fileName) {
        List<String> aliases = new ArrayList<>();
        if (!fileName.startsWith("PACE_OCI.")) {
            return aliases;
        }

        String[] parts = fileName.split("\\.", -1);
        for (int i = 0; i < parts.length - 1; i++) {
            if ("L2".equals(parts[i]) && i + 1 < parts.length - 1) {
                String product = parts[i + 1];
                if (product.startsWith("OC_")) {
                    parts[i + 1] = product.substring("OC_".length());
                    aliases.add(String.join(".", parts));
                } else {
                    parts[i + 1] = "OC_" + product;
                    aliases.add(String.join(".", parts));
                }
                return aliases;
            }
        }
        return aliases;
    }

    private static String insertBeforeExtension(String fileName, String token) {
        int extensionDot = fileName.lastIndexOf('.');
        if (extensionDot <= 0) {
            return fileName + token;
        }
        return fileName.substring(0, extensionDot) + token + fileName.substring(extensionDot);
    }

    private static void addInsertedNrtVariants(Set<String> alternates, String fileName) {
        String beforeExtension = insertBeforeExtension(fileName, "_NRT");
        if (beforeExtension != null) {
            alternates.add(beforeExtension);
        }
    }

    private static String getPreviewUrlForFileName(String fileName) {
        return "https://oceandata.sci.gsfc.nasa.gov/browse_images/" + fileName + ".png";
    }

    private static String getPreviewUrlWithFilePath(String fileName) {
        String filePath = getPaceL1bImageFilePath(fileName);
        if (filePath == null) {
            return null;
        }
        return getPreviewUrlForFileName(fileName) + "?file_path=" + filePath;
    }

    private static String getPaceL1bImageFilePath(String fileName) {
        if (!fileName.startsWith("PACE_OCI.") || !fileName.contains(".L1B.")) {
            return null;
        }

        String[] parts = fileName.split("\\.", -1);
        if (parts.length < 2 || parts[1].length() < 8) {
            return null;
        }

        String date = parts[1].substring(0, 8);
        if (!date.chars().allMatch(Character::isDigit)) {
            return null;
        }

        return "PACE_OCI/IMAGES/EDBRS/" + date.substring(0, 4) + "/" + date.substring(4, 8);
    }

    private String showImagePreview(String fileName, Component parent, Point screenLocation, JDialog parentDialog) {
        try {
            PreviewImage previewImage = loadPreviewImage(fileName);
            if (previewImage != null && stringCompareEquals(fileName, hoveringFileName)) {
                displayPreviewImage(previewImage.image, parent, screenLocation, parentDialog);
                return previewImage.url;
            }

            if (stringCompareEquals(fileName, hoveringFileName)) {
                hideImagePreview();
            }
        } catch (Exception e) {
            if (stringCompareEquals(fileName, hoveringFileName)) {
                hideImagePreview();
            }
        }
        return null;
    }

    public void hideImagePreview() {
        runOnEdt(() -> {
            previewWindow.setVisible(false);
            currentImageUrl = null;
        });
    }

    public void showFullImageDialog(String fileName, Component parent) {
        try {
            PreviewImage previewImage = loadPreviewImage(fileName);
            if (previewImage != null) {
                ImageIcon icon = new ImageIcon(previewImage.image);
                JLabel label = new JLabel(icon);
                JScrollPane scrollPane = new JScrollPane(label);
                scrollPane.setPreferredSize(new Dimension(700, 700));
                JOptionPane.showMessageDialog(parent, scrollPane, "Full Image Preview", JOptionPane.PLAIN_MESSAGE);
                return;
            }
            JOptionPane.showMessageDialog(parent, "Image not available.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(parent, "Failed to load image.");
        }
    }

    private PreviewImage loadPreviewImage(String fileName) {
        PreviewImage cachedPreviewImage = previewImageCache.get(fileName);
        if (cachedPreviewImage != null) {
            return cachedPreviewImage;
        }

        PreviewImage previewImage = tryLoadPreviewImage(getFastPreviewUrls(fileName));
        if (previewImage != null) {
            previewImageCache.put(fileName, previewImage);
            return previewImage;
        }
        previewImage = tryLoadPreviewImage(getCmrAlternatePreviewUrls(fileName));
        if (previewImage != null) {
            previewImageCache.put(fileName, previewImage);
        }
        return previewImage;
    }

    private PreviewImage tryLoadPreviewImage(List<String> imageUrls) {
        for (String imageUrl : imageUrls) {
            Image image = tryReadImage(imageUrl);
            if (image != null) {
                return new PreviewImage(imageUrl, image);
            }
        }
        return null;
    }

    private void displayPreviewImage(Image image, Component parent, Point screenLocation, JDialog parentDialog) {
        runOnEdt(() -> {
            int browseImageSize = Earthdata_Cloud_Controller.getPreferenceBrowseImageSize();

            Image scaled = null;
            boolean scaleOnHeight = false;
            if (scaleOnHeight) {
                scaled = image.getScaledInstance(-1, browseImageSize, Image.SCALE_SMOOTH);
            } else {
                scaled = image.getScaledInstance(browseImageSize, -1, Image.SCALE_SMOOTH);
            }

            previewLabel.setIcon(new ImageIcon(scaled));
            previewWindow.pack();

            int windowHeight = previewWindow.getHeight();

            Image scaled2 = null;
            if (windowHeight > parentDialog.getHeight()) {
                scaled2 = image.getScaledInstance(-1, parentDialog.getHeight(), Image.SCALE_SMOOTH);
                previewLabel.setIcon(new ImageIcon(scaled2));
                previewWindow.pack();

            }

            windowHeight = previewWindow.getHeight();

            Point parentDialogLocation = parentDialog.getLocationOnScreen();
            Point parentDialogLocationBottom = new Point(parentDialogLocation.x, parentDialogLocation.y + parentDialog.getHeight());
            Point tableLocationTop = parent.getLocationOnScreen();

            int offsetX = parentDialog.getWidth() + 5;

            int locationY = tableLocationTop.y;
            int offsetY = (int) Math.abs(parentDialogLocation.y - tableLocationTop.y);
            offsetY = (int) Math.round(0.6 * offsetY);

            boolean floating = false;

            if (windowHeight < (int) Math.round(0.1 * parentDialog.getHeight())) {
                floating = true;
            }

            if (!floating) {
                int offsetYTop = (int) Math.floor((parentDialog.getHeight() - windowHeight) / 2.0);
                locationY = parentDialog.getLocationOnScreen().y + offsetYTop;
            }

            if (floating) {
                locationY = screenLocation.y - (int) Math.round(0.5 * windowHeight);
            }

            previewWindow.setLocation(parentDialogLocation.x + offsetX, locationY);

            previewWindow.setVisible(true);
        });
    }

    private void runOnEdt(Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            SwingUtilities.invokeLater(runnable);
        }
    }

    private Image tryReadImage(String imageUrl) {
        try {
            URLConnection connection = new URL(imageUrl).openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
            try (InputStream stream = connection.getInputStream()) {
                return ImageIO.read(stream);
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private List<String> getFastPreviewUrls(String fileName) {
        Set<String> previewUrls = new LinkedHashSet<>();
        if (previewLinkMap != null) {
            String previewUrl = previewLinkMap.get(fileName);
            if (previewUrl != null && !previewUrl.isBlank()) {
                previewUrls.add(previewUrl);
            }
        }
        previewUrls.addAll(buildPreviewUrls(fileName));
        return new ArrayList<>(previewUrls);
    }

    private List<String> getCmrAlternatePreviewUrls(String fileName) {
        Set<String> previewUrls = new LinkedHashSet<>();
        String cmrPreviewUrl = getPreviewUrlFromCmr(fileName);
        if (cmrPreviewUrl != null && !cmrPreviewUrl.isBlank()) {
            if (previewLinkMap != null) {
                previewLinkMap.put(fileName, cmrPreviewUrl);
            }
            previewUrls.add(cmrPreviewUrl);
        }
        for (String alternateFileName : getAlternatePreviewFileNames(fileName)) {
            String alternateCmrUrl = getPreviewUrlFromCmr(alternateFileName);
            if (alternateCmrUrl != null && !alternateCmrUrl.isBlank()) {
                previewUrls.add(alternateCmrUrl);
            }
        }
        return new ArrayList<>(previewUrls);
    }

    private static final class PreviewImage {
        private final String url;
        private final Image image;

        private PreviewImage(String url, Image image) {
            this.url = url;
            this.image = image;
        }
    }

    private String getPreviewUrlFromCmr(String fileName) {
        try {
            String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8);
            String cmrUrl = "https://cmr.earthdata.nasa.gov/search/granules.umm_json"
                    + "?readable_granule_name=" + encoded
                    + "&provider=OB_CLOUD";

            HttpURLConnection connection = (HttpURLConnection) new URL(cmrUrl).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);

            try (InputStream stream = connection.getInputStream();
                 Scanner scanner = new Scanner(stream, StandardCharsets.UTF_8).useDelimiter("\\A")) {
                String response = scanner.hasNext() ? scanner.next() : "";
                JSONObject json = new JSONObject(response);
                JSONArray items = json.optJSONArray("items");
                if (items == null || items.isEmpty()) {
                    return null;
                }

                JSONObject umm = items.getJSONObject(0).optJSONObject("umm");
                if (umm == null) {
                    return null;
                }

                JSONArray relatedUrls = umm.optJSONArray("RelatedUrls");
                if (relatedUrls == null) {
                    return null;
                }

                for (int i = 0; i < relatedUrls.length(); i++) {
                    JSONObject relatedUrl = relatedUrls.getJSONObject(i);
                    String type = relatedUrl.optString("Type", "");
                    String mimeType = relatedUrl.optString("MimeType", "");
                    String format = relatedUrl.optString("Format", "");
                    String url = relatedUrl.optString("URL", "");

                    if (!url.isBlank() && (type.toUpperCase().contains("VISUALIZATION")
                            || type.toUpperCase().contains("BROWSE")
                            || "image/png".equalsIgnoreCase(mimeType)
                            || "PNG".equalsIgnoreCase(format))) {
                        return url;
                    }
                }
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }
}
