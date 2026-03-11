package gov.nasa.gsfc.seadas.earthdatacloud.worldview;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WorldviewSyncPanel extends JPanel {

    // UI Components
    private JTextField urlInputField = new JTextField(20);
    private JTextField minLonField = new JTextField(8);
    private JTextField minLatField = new JTextField(8);
    private JTextField maxLonField = new JTextField(8);
    private JTextField maxLatField = new JTextField(8);

    public WorldviewSyncPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new TitledBorder("Region Selection (via NASA Worldview)"));

        // 1. Top Section: Instructions and Launch Button
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton openBtn = new JButton("1. Open Worldview Map");
        openBtn.setToolTipText("Open Worldview in your browser to select a region");
        openBtn.addActionListener(e -> launchWorldview());

        topPanel.add(openBtn);
        topPanel.add(new JLabel(" -> 2. Draw box & Copy URL -> 3. Paste below:"));

        // 2. Middle Section: URL Input
        JPanel midPanel = new JPanel(new BorderLayout(5, 5));
        midPanel.add(new JLabel("Paste Worldview Link:"), BorderLayout.NORTH);
        midPanel.add(urlInputField, BorderLayout.CENTER);

        // Add the Real-Time Listener
        setupSyncListener();

        // 3. Bottom Section: Coordinate Result Fields
        JPanel coordPanel = new JPanel(new GridLayout(2, 4, 5, 5));
        coordPanel.add(new JLabel("West (Lon):"));
        coordPanel.add(minLonField);
        coordPanel.add(new JLabel("East (Lon):"));
        coordPanel.add(maxLonField);
        coordPanel.add(new JLabel("South (Lat):"));
        coordPanel.add(minLatField);
        coordPanel.add(new JLabel("North (Lat):"));
        coordPanel.add(maxLatField);

        // Layout assembly
        add(topPanel, BorderLayout.NORTH);
        add(midPanel, BorderLayout.CENTER);
        add(coordPanel, BorderLayout.SOUTH);
    }

    private void launchWorldview() {
        try {
            // Opens Worldview with Geographic projection forced for compatibility
            Desktop.getDesktop().browse(new URI("https://worldview.earthdata.nasa.gov/?p=geographic"));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not open browser: " + ex.getMessage());
        }
    }

    private void setupSyncListener() {
        urlInputField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { parseAndFill(); }
            public void removeUpdate(DocumentEvent e) { parseAndFill(); }
            public void changedUpdate(DocumentEvent e) { parseAndFill(); }

            private void parseAndFill() {
                String text = urlInputField.getText().trim();
                if (text.isEmpty()) {
                    urlInputField.setBackground(Color.WHITE);
                    return;
                }

                // Regex to find v=minLon,minLat,maxLon,maxLat
                Pattern p = Pattern.compile("v=([\\d\\.-]+),([\\d\\.-]+),([\\d\\.-]+),([\\d\\.-]+)");
                Matcher m = p.matcher(text);

                if (m.find()) {
                    minLonField.setText(m.group(1));
                    minLatField.setText(m.group(2));
                    maxLonField.setText(m.group(3));
                    maxLatField.setText(m.group(4));

                    // Success feedback
                    urlInputField.setBackground(new Color(210, 255, 210));
                } else {
                    // Error feedback
                    urlInputField.setBackground(new Color(255, 210, 210));
                }
            }
        });
    }

    // Getters for SeaDAS to grab the final values
    public String getBBoxString() {
        return String.format("%s,%s,%s,%s",
                minLonField.getText(), minLatField.getText(),
                maxLonField.getText(), maxLatField.getText());
    }

    private void addActionButtons(JPanel parent) {
        JButton copyBtn = new JButton("Copy BBox to Clipboard");
        copyBtn.setToolTipText("Copies coordinates as minLon,minLat,maxLon,maxLat");

        copyBtn.addActionListener(e -> {
            String bbox = getBBoxString(); // Returns "minLon,minLat,maxLon,maxLat"
            if (!bbox.equals(",,,")) { // Simple check to see if fields are filled
                StringSelection selection = new StringSelection(bbox);
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);

                // Temporary feedback on the button text
                String originalText = copyBtn.getText();
                copyBtn.setText("Copied!");
                new Timer(2000, evt -> copyBtn.setText(originalText)).start();
            }
        });

        parent.add(copyBtn);
    }

    /**
     * Validation to ensure coordinates make sense
     */
    public boolean isValidSelection() {
        try {
            double minLon = Double.parseDouble(minLonField.getText());
            double maxLon = Double.parseDouble(maxLonField.getText());
            double minLat = Double.parseDouble(minLatField.getText());
            double maxLat = Double.parseDouble(maxLatField.getText());

            // Check latitude bounds
            if (minLat < -90 || maxLat > 90 || minLat > maxLat) return false;
            // Check longitude bounds
            if (minLon < -180 || maxLon > 180) return false;

            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
