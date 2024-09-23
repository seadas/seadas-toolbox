package gov.nasa.gsfc.seadas.earthdatacloud.ui;

import gov.nasa.gsfc.seadas.earthdatacloud.action.WebPageFetcherWithJWT;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.ui.UIUtils;
import org.esa.snap.ui.tool.ToolButtonFactory;
import org.openide.util.HelpCtx;

import javax.swing.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.event.SwingPropertyChangeSupport;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;

public class HarmonySearchServiceDiaglog extends JDialog{

    public static final String TITLE = "Search OB-CLOUD data through Harmony Search Service"; /*I18N*/
    public static final String DEFAULT_SELECTED_MISSION = "PACE";
    private SwingPropertyChangeSupport propertyChangeSupport;
    private Component helpButton = null;
    private final static String helpId = "searchServiceHelp";
    private final static String HELP_ICON = "icons/Help24.gif";

    JPanel searchInputMainPanel;

    public HarmonySearchServiceDiaglog(){
        super(SnapApp.getDefault().getMainFrame(), TITLE, JDialog.DEFAULT_MODALITY_TYPE);
        setLayout(new BorderLayout());
        setSize(1000, 1000);

        propertyChangeSupport = new SwingPropertyChangeSupport(this);

        helpButton = getHelpButton();
        createSearchServiceInputPanel();

//        propertyChangeSupport.addPropertyChangeListener(NEW_BAND_SELECTED_PROPERTY, getBandPropertyListener());
//        propertyChangeSupport.addPropertyChangeListener(DELETE_BUTTON_PRESSED_PROPERTY, getDeleteButtonPropertyListener());
//
//        propertyChangeSupport.addPropertyChangeListener(NEW_FILTER_SELECTED_PROPERTY, getFilterButtonPropertyListener());
//        propertyChangeSupport.addPropertyChangeListener(FILTER_STATUS_CHANGED_PROPERTY, getFilterCheckboxPropertyListener());
    }

    protected AbstractButton getHelpButton() {
        if (helpId != null) {
            final AbstractButton helpButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon(HELP_ICON),
                    false);
            helpButton.setToolTipText("Help.");
            helpButton.setName("helpButton");
            helpButton.addActionListener(e ->getHelpCtx().display());
            return helpButton;
        }

        return null;
    }

    public HelpCtx getHelpCtx() {
        return new HelpCtx(helpId);
    }

    public JPanel createSearchServiceInputPanel() {

        searchInputMainPanel = new JPanel(new GridBagLayout());
        searchInputMainPanel.setBorder(BorderFactory.createTitledBorder(""));

        JPanel missionSelectionPanel = new JPanel(new GridBagLayout());

        JPanel dataLevelPanel = new JPanel();
        dataLevelPanel.setLayout(new GridLayout(3, 1, 10, 10)); // GridLayout with 3 rows, 1 column

        // Create an array of strings for the dropdown options
        String[] options = {"PACE", "Hawkeye", "SeaWIFS"};

        // Create the JComboBox and populate it with options
        JComboBox<String> comboBox = new JComboBox<>(options);
        comboBox.setBounds(50, 50, 200, 10); // Set position and size
        comboBox.setEditable(false);
        comboBox.setSelectedItem(DEFAULT_SELECTED_MISSION);
        comboBox.setName("selectMissionJComboBox");
        comboBox.setToolTipText("Select mission for search");

        // Add an ActionListener to respond to user selections
        comboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Get selected item
                String selectedItem = (String) comboBox.getSelectedItem();
                // Show a message dialog with the selected item
                JOptionPane.showMessageDialog(dataLevelPanel, "You selected: " + selectedItem);
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
        missionSelectionPanel.add(comboBox);
        dataLevelPanel.add(checkBox1);
        dataLevelPanel.add(checkBox2);
        dataLevelPanel.add(statusLabel);


        JButton searchButton = new JButton("Search");
        searchButton.setEnabled(true);
        searchButton.setName("searchButton");
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setEnabled(true);
        cancelButton.setName("cancelButton");

        final JScrollPane[] scrollPane = {new JScrollPane()};


        ActionListener searchButtonActionListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                WebPageFetcherWithJWT webPageFetcherWithJWT = new WebPageFetcherWithJWT();
                scrollPane[0] =new JScrollPane(webPageFetcherWithJWT.getSearchDataListTable());
                add(scrollPane[0], BorderLayout.SOUTH);
                pack();
                repaint();
            }
        };

        ActionListener cancelButtonActionListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {

            }
        };

        searchButton.addActionListener(searchButtonActionListener);
        cancelButton.addActionListener(cancelButtonActionListener);

        searchInputMainPanel.add(missionSelectionPanel, new ExGridBagConstraints(0, 0, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, 5));
        searchInputMainPanel.add(dataLevelPanel, new ExGridBagConstraints(0, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, 5));
        searchInputMainPanel.add(searchButton, new ExGridBagConstraints(0, 2, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, 5));
        searchInputMainPanel.add(cancelButton, new ExGridBagConstraints(1, 2, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, 5));
        //searchInputMainPanel.add(scrollPane[0]);

        add(searchInputMainPanel, BorderLayout.NORTH);

        //searchInputMainPanel.getRootPane().setDefaultButton((JButton) ((JPanel) searchInputMainPanel.getComponent(1)).getComponent(1));
        setModalityType(ModalityType.APPLICATION_MODAL);
        setTitle("Search OB-CLOUD Data");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        pack();
        return searchInputMainPanel;
    }

    @Override
    public void addPropertyChangeListener(String name, PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(name, listener);
    }

    @Override
    public void removePropertyChangeListener(String name, PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(name, listener);
    }
}
