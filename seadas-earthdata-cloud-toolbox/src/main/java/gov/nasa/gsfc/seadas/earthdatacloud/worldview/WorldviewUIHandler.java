package gov.nasa.gsfc.seadas.earthdatacloud.worldview;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Color;

public class WorldviewUIHandler {

    // References to your existing UI components
    private JTextField urlInputField;
    private JTextField minLonField, minLatField, maxLonField, maxLatField;

    public void setupSyncListener() {
        urlInputField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { sync(); }
            public void removeUpdate(DocumentEvent e) { sync(); }
            public void changedUpdate(DocumentEvent e) { sync(); }

            private void sync() {
                String input = urlInputField.getText().trim();
                if (input.isEmpty()) return;

                // Use the parser logic from the previous step
                double[] coords = WorldviewIntegration.parseAndValidateURL(input);

                if (coords != null && coords.length == 4) {
                    // Update the SeaDAS manual bbox fields
                    minLonField.setText(String.valueOf(coords[0]));
                    minLatField.setText(String.valueOf(coords[1]));
                    maxLonField.setText(String.valueOf(coords[2]));
                    maxLatField.setText(String.valueOf(coords[3]));

                    // Visual feedback: success
                    urlInputField.setBackground(new Color(200, 255, 200));
                } else {
                    // Visual feedback: invalid URL format
                    urlInputField.setBackground(new Color(255, 220, 220));
                }
            }
        });
    }
}
