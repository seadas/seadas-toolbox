package gov.nasa.gsfc.seadas.earthdatacloud.util;

import gov.nasa.gsfc.seadas.earthdatacloud.preferences.Earthdata_Cloud_Controller;

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

    public void attachToTable(JTable table, Map<String, String> fileLinkMap, JDialog parentDialog) {
        table.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());

//                if (row >= 0 && col == 0) { // Only for file name column
                // todo Danny preferences which columns to hover
//                if (row >= 0 && col == 1) { // for all columns in the row
                if (row >= 0) { // for all columns in the row
                    String fileName = (String) table.getValueAt(row, 0);
                    String imageUrl = getPreviewUrl(fileName);
                    if (imageUrl != null && !imageUrl.equals(currentImageUrl)) {
                        showImagePreview(imageUrl, table, e.getLocationOnScreen(), parentDialog);
                        currentImageUrl = imageUrl;
                    }
                } else {
                    hideImagePreview();
                }


                table.setBackground(Color.WHITE);
                table.setForeground(Color.BLACK);

                if (row >= 0 && row < table.getRowCount()) {
                    table.setRowSelectionInterval(row, row);
                    table.setSelectionBackground(new Color(0, 100, 200));
                    table.setSelectionForeground(Color.WHITE);
                }

//                table.setSelectionBackground(Color.BLUE);


            }
        });

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                hideImagePreview();
                table.setSelectionBackground(Color.WHITE);
                table.setSelectionForeground(Color.BLACK);
            }
        });
    }

    private String getPreviewUrl(String fileName) {
        return "https://oceandata.sci.gsfc.nasa.gov/browse_images/" + fileName + ".png";
    }

    private void showImagePreview(String imageUrl, Component parent, Point screenLocation, JDialog parentDialog) {
        try {
            Image image = ImageIO.read(new URL(imageUrl));
            if (image != null) {
//
//               Rectangle bounds = image.getGraphics().getClipBounds();
//               double crop = 0.5;
//               int widthRaw = bounds.width - bounds.x;
//               int clipWidth = (int) Math.round(crop * widthRaw);
//               int clipX = bounds.x + (int) Math.round(widthRaw * crop/2.0);
//               image.getGraphics().getClipBounds(new Rectangle(clipX,bounds.y,clipWidth,bounds.height));

                // todo  Danny preferences
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

                Point parentDialogLocation = parentDialog.getLocationOnScreen();
                Point parentDialogLocationBottom = new Point(parentDialogLocation.x, parentDialogLocation.y + parentDialog.getHeight());
                Point tableLocationTop = parent.getLocationOnScreen();
                int windowHeight = previewWindow.getHeight();

                int offsetX = parentDialog.getWidth();




                // default to anchor at top of table
                int locationY = tableLocationTop.y;
                int offsetY = (int) Math.abs(parentDialogLocation.y - tableLocationTop.y);
                offsetY = (int) Math.round(0.6 * offsetY);

                boolean floating = false;
                if (!floating) {
                    if (windowHeight > parentDialog.getHeight()) {
                        System.out.println("Anchor Main TOP");
                        // anchor at top of main GUI
                        locationY = parentDialog.getLocationOnScreen().y;
                    } else if (windowHeight > (parent.getHeight() +  offsetY)) {
                        System.out.println("Anchor Main BOTTOM");
                        // anchor at bottom of the main GUI
                        locationY = parentDialogLocationBottom.y - windowHeight;
                    } else if (windowHeight > offsetY) {
                        System.out.println("Anchor Above Table TOP");
                        // anchor near top of the main GUI
                        locationY = tableLocationTop.y - offsetY;
                    } else {
                        System.out.println("Anchor Table TOP");
                        // use dafault of anchor at top of table but if image is very small then floating
                        if (windowHeight < (int) Math.round(0.5 * parent.getHeight())) {
                            System.out.println("Anchor FLOATING");
                            floating = true;
                        }
                    }
                }

                if (floating) {
                    locationY = screenLocation.y - (int) Math.round(0.5 * windowHeight);
                }



                previewWindow.setLocation(parentDialogLocation.x + offsetX, locationY);


                // todo
//                previewWindow.setLocation(screenLocation.x + 75, screenLocation.y + offsetY);
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
