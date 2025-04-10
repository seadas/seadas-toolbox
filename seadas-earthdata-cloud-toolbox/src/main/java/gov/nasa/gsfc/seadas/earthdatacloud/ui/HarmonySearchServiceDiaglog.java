package gov.nasa.gsfc.seadas.earthdatacloud.ui;

import gov.nasa.gsfc.seadas.earthdatacloud.action.DataRetrievalTask;
import gov.nasa.gsfc.seadas.earthdatacloud.auth.WebPageFetcherWithJWT;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.ui.UIUtils;
import org.esa.snap.ui.tool.ToolButtonFactory;
import org.json.JSONObject;
import org.openide.util.HelpCtx;

import javax.swing.*;
import java.awt.*;
import javax.swing.event.SwingPropertyChangeSupport;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeListener;
import java.util.concurrent.ExecutionException;

public class HarmonySearchServiceDiaglog extends JDialog{

    public static final String TITLE = "OB_CLOUD Data Browser - powered by Harmony Search"; /*I18N*/
    public static final String DEFAULT_SELECTED_MISSION = "PACE";
    private SwingPropertyChangeSupport propertyChangeSupport;
    private Component helpButton = null;
    private final static String helpId = "searchServiceHelp";
    private final static String HELP_ICON = "icons/Help24.gif";

    JPanel searchInputMainPanel;

    public HarmonySearchServiceDiaglog(){
        super(SnapApp.getDefault().getMainFrame(), TITLE, JDialog.DEFAULT_MODALITY_TYPE);
        setLayout(new BorderLayout());
        setSize(900, 700);

        propertyChangeSupport = new SwingPropertyChangeSupport(this);

        helpButton = getHelpButton();
        //createSearchServiceInputPanel();
        OBDAACDataBrowser embeddedBrowser = new OBDAACDataBrowser(this);
        add(embeddedBrowser, BorderLayout.CENTER);
        pack();  // This fixes layout sizing
        setLocationRelativeTo(null);  // Center on screen (optional but nice)
        Window parent = SnapApp.getDefault().getMainFrame();
        setLocationRelativeTo(parent);  // Centers over the SeaDAS window
        Point location = getLocation();
        setLocation(location.x-200, Math.max(0, location.y - 200));
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


        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createTitledBorder(""));

        searchInputMainPanel = new JPanel(new GridBagLayout());
        searchInputMainPanel.setBorder(BorderFactory.createTitledBorder(""));


        JButton searchButton = new JButton("Search");
        searchButton.setEnabled(true);
        searchButton.setName("searchButton");

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setEnabled(true);
        cancelButton.setName("cancelButton");

        final DataRetrievalTask[] task = new DataRetrievalTask[1];
        // Create a JProgressBar
        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setMinimumSize(new Dimension(200, 20)); // Min size
        progressBar.setMaximumSize(new Dimension(350, 20)); // Max size
        progressBar.setPreferredSize(new Dimension(300, 20));
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        add(progressBar, BorderLayout.CENTER);

        searchButton.addActionListener(e -> {
            // Disable the start button while downloading
            searchButton.setEnabled(false);
            cancelButton.setEnabled(true);
            // Start a SwingWorker to download data and update the progress bar
            task[0] = new DataRetrievalTask(progressBar, searchButton, cancelButton);
            task[0].execute();// Executes the task in the background
            new Thread(() -> {
                try {
                    // Wait for the task to finish and get the result
                    JSONObject jsonObject = task[0].get();    // This blocks until doInBackground() is done
                    // Update the label in the EDT with the result
                    if (jsonObject != null ) {
                        SwingUtilities.invokeLater(() -> {
                            //resultLabel.setText(result);
                            searchButton.setEnabled(true); // Re-enable the button after the task
                            WebPageFetcherWithJWT webPageFetcherWithJWT = new WebPageFetcherWithJWT();
                            var pane = new JScrollPane();
                            pane.getViewport().add(webPageFetcherWithJWT.getSearchDataList(jsonObject));

                            mainPanel.add(pane, new ExGridBagConstraints(0, 2, 2, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 10));
                            revalidate();
                            pack();
                            repaint();
                        });
                    } else {
                        System.out.println("Task returned null!");
                    }
                } catch (InterruptedException | ExecutionException ex) {
                    ex.printStackTrace();
                }
            }).start();
        });

        // Action listener for the "Cancel" button
        cancelButton.addActionListener(e -> {
            if (task[0] != null && !task[0].isDone()) {
                task[0].cancel(true); // Request cancellation of the task
            }
            dispose();
            pack();
            repaint();
        });

        searchInputMainPanel.add(getMissionsPanel(), new ExGridBagConstraints(0, 0, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, 5));
        searchInputMainPanel.add(getProductsPanel(), new ExGridBagConstraints(1, 0, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, 5));
        searchInputMainPanel.add(getDataLevelPanel(), new ExGridBagConstraints(2, 0, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, 5));
        searchInputMainPanel.add(searchButton, new ExGridBagConstraints(1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, 50));
        searchInputMainPanel.add(cancelButton, new ExGridBagConstraints(2, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, 5));

        mainPanel.add(searchInputMainPanel, new ExGridBagConstraints(0, 0, 0, 0, GridBagConstraints.NORTH, GridBagConstraints.NONE, 5));
        mainPanel.add(progressBar, new ExGridBagConstraints(0, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 5));


        add(mainPanel);

        setModalityType(ModalityType.APPLICATION_MODAL);
        setTitle("Browse OB-CLOUD Data through the Harmony Search Service");
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

    private JPanel getMissionsPanel(){
        JPanel missionSelectionPanel = new JPanel(new GridBagLayout());

        // Create an array of strings for the dropdown options
        String[] options = {"PACE", "Hawkeye", "SeaWIFS"};
        boolean[] disabledItems = {false, true, true};
        // Create the JComboBox and populate it with options
        JComboBox<String> comboBox = new JComboBox<>(options);
        comboBox.setBounds(50, 50, 200, 10); // Set position and size
        comboBox.setEditable(false);
        comboBox.setSelectedItem(DEFAULT_SELECTED_MISSION);
        comboBox.setName("selectMissionJComboBox");
        comboBox.setToolTipText("Select mission for search");
        comboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                // If the item is disabled, make it gray and non-selectable
                if (index >= 0 && disabledItems[index]) {
                    component.setEnabled(false);
                    component.setForeground(Color.GRAY);
                } else {
                    component.setEnabled(true);
                    component.setForeground(Color.BLACK);
                }

                return component;
            }
        });

        // Add an ItemListener to prevent selecting disabled items
        comboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                // If the item is disabled, reset the selection to the last valid item
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    int selectedIndex = comboBox.getSelectedIndex();
                    if (disabledItems[selectedIndex]) {
                        JOptionPane.showMessageDialog(null, "This item is disabled.");
                        comboBox.setSelectedIndex(0); // Change to a valid selection (here, the first item)
                    }
                }
            }
        });


        JLabel missionSelectionLabel = new JLabel("Missions:");

        missionSelectionPanel.add(missionSelectionLabel, new ExGridBagConstraints(0, 0, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, 5));
        missionSelectionPanel.add(comboBox, new ExGridBagConstraints(1, 0, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, 5));

        return missionSelectionPanel;
    }
    private JPanel getProductsPanel() {

        JPanel productSelectionPanel = new JPanel(new GridBagLayout());

        // Create an array of strings for the dropdown productOptions
        String[] productOptions = {"BGC", "OIP", "OAP"};
        boolean[] disabledItems = {false, true, true};
        // Create the JComboBox and populate it with productOptions
        JComboBox<String> comboBox = new JComboBox<>(productOptions);
        comboBox.setBounds(50, 50, 200, 10); // Set position and size
        comboBox.setEditable(false);
        comboBox.setSelectedItem(DEFAULT_SELECTED_MISSION);
        comboBox.setName("selectProductJComboBox");
        comboBox.setToolTipText("Select product for search");
        comboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                // If the item is disabled, make it gray and non-selectable
                if (index >= 0 && disabledItems[index]) {
                    component.setEnabled(false);
                    component.setForeground(Color.GRAY);
                } else {
                    component.setEnabled(true);
                    component.setForeground(Color.BLACK);
                }
                return component;
            }
        });

        // Add an ItemListener to prevent selecting disabled items
        comboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                // If the item is disabled, reset the selection to the last valid item
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    int selectedIndex = comboBox.getSelectedIndex();
                    if (disabledItems[selectedIndex]) {
                        JOptionPane.showMessageDialog(null, "This item is disabled.");
                        comboBox.setSelectedIndex(0); // Change to a valid selection (here, the first item)
                    }
                }
            }
        });

        JLabel productSelectionLabel = new JLabel("Products:");

        productSelectionPanel.add(productSelectionLabel, new ExGridBagConstraints(0, 0, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, 5));
        productSelectionPanel.add(comboBox, new ExGridBagConstraints(1, 0, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, 5));

        return productSelectionPanel;
    }

    private JPanel getDataLevelPanel() {

        JPanel dataLevelSelectionPanel = new JPanel(new GridBagLayout());

        // Create an array of strings for the dropdown dataLevelOptions
        String[] dataLevelOptions = {"Level 2", "Level 3"};
        boolean[] disabledItems = {false, true};
        // Create the JComboBox and populate it with dataLevelOptions
        JComboBox<String> comboBox = new JComboBox<>(dataLevelOptions);
        comboBox.setBounds(50, 50, 200, 10); // Set position and size
        comboBox.setEditable(false);
        comboBox.setSelectedItem(DEFAULT_SELECTED_MISSION);
        comboBox.setName("selectDataLevelJComboBox");
        comboBox.setToolTipText("Select data level for search");
        comboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                // If the item is disabled, make it gray and non-selectable
                if (index >= 0 && disabledItems[index]) {
                    component.setEnabled(false);
                    component.setForeground(Color.GRAY);
                } else {
                    component.setEnabled(true);
                    component.setForeground(Color.BLACK);
                }
                return component;
            }
        });

        // Add an ItemListener to prevent selecting disabled items
        comboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                // If the item is disabled, reset the selection to the last valid item
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    int selectedIndex = comboBox.getSelectedIndex();
                    if (disabledItems[selectedIndex]) {
                        JOptionPane.showMessageDialog(null, "This item is disabled.");
                        comboBox.setSelectedIndex(0); // Change to a valid selection (here, the first item)
                    }
                }
            }
        });

        JLabel dataLevelSelectionLabel = new JLabel("Data Level:");

        dataLevelSelectionPanel.add(dataLevelSelectionLabel, new ExGridBagConstraints(0, 0, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, 5));
        dataLevelSelectionPanel.add(comboBox, new ExGridBagConstraints(1, 0, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, 5));

        return dataLevelSelectionPanel;
    }
}
