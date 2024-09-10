package gov.nasa.gsfc.seadas.earthdatacloud.ui;

import org.esa.snap.rcp.SnapApp;

import javax.swing.*;
import java.awt.*;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class HarmonySearchServiceDiaglog extends JDialog{

    public static final String TITLE = "Search OB-CLOUD data through Harmony Search Service"; /*I18N*/

    public HarmonySearchServiceDiaglog(){
        super(SnapApp.getDefault().getMainFrame(), TITLE, JDialog.DEFAULT_MODALITY_TYPE);
    }

    public JDialog createSearchServiceInputDiaglog() {
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridLayout(3, 1, 10, 10)); // GridLayout with 3 rows, 1 column

        // Create an array of strings for the dropdown options
        String[] options = {"PACE", "Hawkeye", "SeaWIFS"};

        // Create the JComboBox and populate it with options
        JComboBox<String> comboBox = new JComboBox<>(options);
        comboBox.setBounds(50, 50, 200, 30); // Set position and size

        // Add an ActionListener to respond to user selections
        comboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Get selected item
                String selectedItem = (String) comboBox.getSelectedItem();
                // Show a message dialog with the selected item
                JOptionPane.showMessageDialog(inputPanel, "You selected: " + selectedItem);
            }
        });


        // Create the checkboxes with labels
        JCheckBox checkBox1 = new JCheckBox("Level 2");
        JCheckBox checkBox2 = new JCheckBox("Level 3");

        // Create a label to display the state of the checkboxes
        JLabel statusLabel = new JLabel("Select an option and checkboxes:");

        // Add an ActionListener to handle checkbox state changes
        ActionListener actionListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Get the state of the checkboxes
                boolean isChecked1 = checkBox1.isSelected();
                boolean isChecked2 = checkBox2.isSelected();

                // Update the label text based on checkbox states
                statusLabel.setText("Level 2: " + (isChecked1 ? "Checked" : "Not Checked") +
                        ", Level 3: " + (isChecked2 ? "Checked" : "Not Checked"));
            }
        };

        // Add the ActionListener to the checkboxes
        checkBox1.addActionListener(actionListener);
        checkBox2.addActionListener(actionListener);

        inputPanel.add(comboBox);
        inputPanel.add(checkBox1);
        inputPanel.add(checkBox2);
        inputPanel.add(statusLabel);

        return this;
    }


    public static void main(String[] args) {
        // Create a new JFrame to hold our components
        JFrame frame = new JFrame("Dropdown Example");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 200);
        frame.setLayout(null); // Use null layout for absolute positioning

        // Create an array of strings for the dropdown options
        String[] options = {"Option 1", "Option 2", "Option 3"};

        // Create the JComboBox and populate it with options
        JComboBox<String> comboBox = new JComboBox<>(options);
        comboBox.setBounds(50, 50, 200, 30); // Set position and size

        // Add an ActionListener to respond to user selections
        comboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Get selected item
                String selectedItem = (String) comboBox.getSelectedItem();
                // Show a message dialog with the selected item
                JOptionPane.showMessageDialog(frame, "You selected: " + selectedItem);
            }
        });

        // Add the JComboBox to the frame
        frame.add(comboBox);

        // Make the frame visible
        frame.setVisible(true);
    }
}
