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

    private String hoveringFileName = null;
    private String workingFileName = null;
    private String finishedFileName = null;
    boolean startingUp = true;
    private boolean previewIsDisplayed = false;
    Thread th = null;

    public ImagePreviewHelper() {
        previewWindow = new JWindow();
        previewLabel = new JLabel();
        previewWindow.getContentPane().add(previewLabel);
        previewWindow.setSize(300, 300);
        previewWindow.setAlwaysOnTop(true);
    }

//    public void handleMouseMoved(JTable table, JDialog parentDialog, MouseEvent e) {
//        int row = table.rowAtPoint(e.getPoint());
//
//    }


    public void attachToTable(JTable table, Map<String, String> fileLinkMap, JDialog parentDialog) {

        table.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {

                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());


                boolean hoveringFileNameChanged = false;

                if (row >= 0 & row < table.getRowCount()) { // for all columns in the row
                    String fileName = (String) table.getValueAt(row, 0);
                    if (fileName != null) {
                        if (!fileName.equalsIgnoreCase(hoveringFileName)) {
                            hoveringFileNameChanged = true;
                        }
                    } else {
                        if (hoveringFileName != null) {
                            hoveringFileNameChanged = true;
                        }
                    }


                    if (hoveringFileNameChanged) {
                        hoveringFileName = fileName;
//                        System.out.println("Hovering fileName=" + hoveringFileName);


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
                                        while (th != null && th.isAlive() && i < 1000) { // stay alive for 100 seconds
//                                            System.out.println("th alive num iter =" + i);

                                            if (hoveringFileName != null) {
                                                String hoveringFileNameSnapshot = hoveringFileName;  // lock this as the main thread could change it
                                                if (!hoveringFileNameSnapshot.equalsIgnoreCase(finishedFileName) || startingUp) {
                                                    startingUp = false;
                                                    String imageUrl = getPreviewUrl(hoveringFileNameSnapshot);
                                                    showImagePreview(imageUrl, table, e.getLocationOnScreen(), parentDialog);
                                                    currentImageUrl = imageUrl;
                                                    if (hoveringFileNameSnapshot.equalsIgnoreCase(hoveringFileName)) {
                                                        finishedFileName = hoveringFileNameSnapshot;
                                                    } else {
                                                        hideImagePreview();
                                                        finishedFileName = null;
                                                    }
                                                    i = 0;
                                                }
                                            } else {
                                                hideImagePreview();
                                            }

                                            try {
                                                // sleep 0.1 second
                                                Thread.sleep(100);
                                            } catch (InterruptedException e3) {
                                                // recommended because catching InterruptedException clears interrupt flag
                                                Thread.currentThread().interrupt();
//                                                System.out.println("Being killed by exit");
                                                // you probably want to quit if the thread is interrupted
                                                return;
                                            }

                                            i++;
                                        }


                                        killImagePreviewThread();

                                    }
                                };
                                th = new Thread(r);
                                th.start();
                            }

                        }
                    }
                } else {
                    if (hoveringFileName != null) {
                        hoveringFileNameChanged = true;
                        hoveringFileName = null;
                    }

                    if (hoveringFileNameChanged) {
//                        System.out.println("Hovering fileName=" + hoveringFileName);
                        table.setBackground(Color.WHITE);
                        table.setForeground(Color.BLACK);
                        table.setSelectionBackground(Color.WHITE);
                        table.setSelectionForeground(Color.BLACK);

                        hideImagePreview();
                    }
                }


            }
        });




        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {

                boolean hoveringFileNameChanged = false;
                if (hoveringFileName != null) {
                    hoveringFileNameChanged = true;
                    hoveringFileName = null;
                }
//                System.out.println("Exited");


                if (hoveringFileNameChanged) {
//                    System.out.println("Hovering fileName=" + hoveringFileName);
                    table.setSelectionBackground(Color.WHITE);
                    table.setSelectionForeground(Color.BLACK);

                    hideImagePreview();
                }

                killImagePreviewThread();

            }
        });
    }



    private void killImagePreviewThread() {
        // Kill thread started
        if (th != null) {
            th.interrupt();

            int i = 0;
            System.out.println("Killing from exit th alive num iter =" + i);

            while (th != null && th.isAlive() && i < 100) {
                try {
                    // sleep 1 second
                    Thread.sleep(100);
                } catch (InterruptedException e3) {
                    // recommended because catching InterruptedException clears interrupt flag
                    Thread.currentThread().interrupt();
                    // you probably want to quit if the thread is interrupted
                    return;
                }

                System.out.println("OUT th alive num iter =" + i);

                i++;
            }
            th = null;
            System.out.println("Killed from exit th alive num iter =" + i);

            // Kill thread completed
        }
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
                int windowHeight = previewWindow.getHeight();


                // Assess if it is a very tall narrow image and limit to GUI height
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





                // default to anchor at top of table
                int locationY = tableLocationTop.y;
                int offsetY = (int) Math.abs(parentDialogLocation.y - tableLocationTop.y);
                offsetY = (int) Math.round(0.6 * offsetY);

                boolean floating = false;



//                if (!floating) {
//                    if (windowHeight > parentDialog.getHeight()) {
////                        System.out.println("Anchor Main TOP");
//                        // anchor at top of main GUI
//                        locationY = parentDialog.getLocationOnScreen().y;
//                    } else if (windowHeight > (parent.getHeight() +  offsetY)) {
////                        System.out.println("Anchor Main BOTTOM");
//                        // anchor at bottom of the main GUI
//                        locationY = parentDialogLocationBottom.y - windowHeight;
//                    } else if (windowHeight > offsetY) {
////                        System.out.println("Anchor Above Table TOP");
//                        // anchor near top of the main GUI
//                        locationY = tableLocationTop.y - offsetY;
//                    } else {
////                        System.out.println("Anchor Table TOP");
//                        // use dafault of anchor at top of table but if image is very small then floating
//                        if (windowHeight < (int) Math.round(0.5 * parent.getHeight())) {
////                            System.out.println("Anchor FLOATING");
//                            floating = true;
//                        }
//                    }
//                }


                if (windowHeight < (int) Math.round(0.1 * parentDialog.getHeight())) {
//                            System.out.println("Anchor FLOATING");
                    floating = true;
                }

                if (!floating) {
                    int offsetYTop = (int) Math.floor((parentDialog.getHeight() - windowHeight) / 2.0);
//                    int offsetYTop = 100;
                    locationY = parentDialog.getLocationOnScreen().y + offsetYTop;
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
//        th.interrupt();
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
