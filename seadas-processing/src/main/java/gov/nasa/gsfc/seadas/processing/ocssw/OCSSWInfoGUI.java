package gov.nasa.gsfc.seadas.processing.ocssw;


import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.swing.binding.BindingContext;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.runtime.Config;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.GridBagUtils;
import org.esa.snap.ui.ModalDialog;
import org.esa.snap.ui.UIUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.*;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static gov.nasa.gsfc.seadas.processing.ocssw.OCSSW.*;
import static gov.nasa.gsfc.seadas.processing.ocssw.OCSSWConfigData.*;
import static gov.nasa.gsfc.seadas.processing.ocssw.OCSSWInfo.*;


/**
 * @author Aynur Abdurazik
 * @author Daniel Knowles
 */
// DEC 2018 - Daniel Knowles - Applied formatting logic for GUI component alignment and arrangement
//                             Added listeners to set Apply button based on textfield values
//                             Minor variable renaming
// MAY 2019 - Knowles - Added OCSSW branch field


public class OCSSWInfoGUI {

    final String PANEL_NAME = "OCSSW Configuration";
    final String HELP_ID = "ocsswInfoConfig";

    final String BRANCH_TOOLTIP = "<html>The OCSSW installation branch<br>" +
            "This by default will match the SeaDAS version (first two decimal fields)<br> " +
            "For instance SeaDAS 7.5.3 by default will use OCSSW branch 7.5<br>" +
            "If OCSSW was manually updated to a different branch then this parameter needs to be set to match</html>";

    JPanel paramPanel = GridBagUtils.createPanel();
    JPanel paramSubPanel;

    ModalDialog modalDialog;


    JTextField ocsswBranchTextfield;
    JComboBox ocsswLocationComboBox;
    JTextField ocsswSharedDir;
    JTextField ocsswRootTextfield;
    JTextField ocsswserverAddressTextfield;
    JTextField serverPortTextfield;
    JTextField serverInputStreamPortTextfield;
    JTextField serverErrorStreamPortTextfield;


    PropertyContainer pc = new PropertyContainer();

    OCSSWConfigData ocsswConfigData = new OCSSWConfigData();

    boolean windowsOS;

    String[] ocsswLocations;

    public static void main(String args[]) {

        final AppContext appContext = SnapApp.getDefault().getAppContext();
        final Window parent = appContext.getApplicationWindow();
        OCSSWInfoGUI ocsswInfoGUI = new OCSSWInfoGUI();
        ocsswInfoGUI.init(parent);
    }

    public void init(Window parent) {
        String operatingSystem = System.getProperty("os.name");
        if (operatingSystem.toLowerCase().contains("windows")) {
            windowsOS = true;
        } else {
            windowsOS = false;
        }


        JPanel mainPanel = GridBagUtils.createPanel();

        modalDialog = new ModalDialog(parent, PANEL_NAME, mainPanel, ModalDialog.ID_OK_CANCEL_HELP, HELP_ID);

        modalDialog.getButton(ModalDialog.ID_OK).setText("OK");
        modalDialog.getButton(ModalDialog.ID_CANCEL).setText("Cancel");
        modalDialog.getButton(ModalDialog.ID_HELP).setText("Help");


        GridBagConstraints gbc = createConstraints();

        gbc.fill = GridBagConstraints.NONE;
        gbc.weighty = 0;
        gbc.weightx = 0;

        JPanel ocsswBranchPanel = ocsswBranchPanel();
        ocsswBranchPanel.setToolTipText(BRANCH_TOOLTIP);
        mainPanel.add(ocsswBranchPanel, gbc);

        gbc.gridy += 1;
        gbc.weighty = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(makeParamPanel(), gbc);


        // Add filler panel at bottom which expands as needed to force all components within this panel to the top
        gbc.gridy += 1;
        gbc.weighty = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.VERTICAL;
        JPanel fillerPanel = new JPanel();
        fillerPanel.setMinimumSize(fillerPanel.getPreferredSize());
        mainPanel.add(fillerPanel, gbc);


        mainPanel.setMinimumSize(mainPanel.getMinimumSize());


        modalDialog.getButton(ModalDialog.ID_OK).setMinimumSize(modalDialog.getButton(ModalDialog.ID_OK).getPreferredSize());

        // Specifically set sizes for dialog here
        Dimension minimumSizeAdjusted = adjustDimension(modalDialog.getJDialog().getMinimumSize(), 25, 25);
        Dimension preferredSizeAdjusted = adjustDimension(modalDialog.getJDialog().getPreferredSize(), 25, 25);
        modalDialog.getJDialog().setMinimumSize(minimumSizeAdjusted);
        modalDialog.getJDialog().setPreferredSize(preferredSizeAdjusted);

        modalDialog.getJDialog().pack();


        int dialogResult;

        boolean finish = false;
        while (!finish) {
            dialogResult = modalDialog.show();

            if (dialogResult == ModalDialog.ID_OK) {
                if (checkParameters()) {
                    ocsswConfigData.updateconfigData(pc);
                    finish = true;
                }
            } else {
                finish = true;
            }
        }

        return;

    }



    private JPanel makeParamPanel() {

        final Preferences preferences = Config.instance("seadas").load().preferences();
        String lastOcsswLocation = preferences.get(SEADAS_OCSSW_LOCATION_PROPERTY, SEADAS_OCSSW_LOCATION_DEFAULT_VALUE);
        GridBagConstraints gbc = createConstraints();

        JLabel ocsswLocationLabel = new JLabel(OCSSW_LOCATION_LABEL + ": ");




        ArrayList<String> ocsswLocationArrayList = new ArrayList<String>();

        if (!windowsOS) {
            ocsswLocationArrayList.add(OCSSW_LOCATION_LOCAL);
            ocsswLocationLabel.setToolTipText("Note: Windows operating system detected so no local directory option");
        }
        ocsswLocationArrayList.add(OCSSW_LOCATION_VIRTUAL_MACHINE);
        ocsswLocationArrayList.add(OCSSW_LOCATION_REMOTE_SERVER);

        ocsswLocations = ocsswLocationArrayList.toArray(new String[ocsswLocationArrayList.size()]);

//        String[] ocsswLocations = {OCSSW_LOCATION_LOCAL, OCSSW_LOCATION_VIRTUAL_MACHINE, OCSSW_LOCATION_REMOTE_SERVER};

        int lastOcsswLocationIndex = 0;
        for (int i = 0; i < ocsswLocations.length; i++) {
            if (lastOcsswLocation.equals(ocsswLocations[i])) {
                lastOcsswLocationIndex = i;
                break;
            }
        }


        ocsswLocationComboBox = new JComboBox(ocsswLocations);
        ocsswLocationComboBox.setSelectedIndex(lastOcsswLocationIndex);

        ocsswLocationComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                // Remove previous selection of paramSubPanel
                if (paramSubPanel != null) {
                    paramPanel.remove(paramSubPanel);
                }

                // Create new paramSubPanel
                String ocssLocationString = (String) ocsswLocationComboBox.getSelectedItem();
                switch (ocssLocationString) {
                    case OCSSW_LOCATION_LOCAL:
                        paramSubPanel = getLocalOCSSWPanel();
                        break;
                    case OCSSW_LOCATION_VIRTUAL_MACHINE:
                        paramSubPanel = getVirtualMachinePanel();
                        break;
                    case OCSSW_LOCATION_REMOTE_SERVER:
                        paramSubPanel = getRemoteServerPanel();
                        break;
                }

                updateParamPanel(paramSubPanel);
                preferences.put(SEADAS_OCSSW_LOCATION_PROPERTY, ocssLocationString);

                try {
                    preferences.flush();
                } catch (BackingStoreException bse) {
                    SnapApp.getDefault().getLogger().severe(bse.getMessage());
                }
            }
        });


        gbc.weighty = 0;
        gbc.insets.top = 10;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        paramPanel.add(getLocationPanel(ocsswLocationLabel, ocsswLocationComboBox), gbc);


        // Determine ideal preferred size and minimum size of the param panel by invoking each selection to obtain
        // the respective values and then use the largest values.

        ocsswLocationComboBox.setSelectedIndex(0);
        Dimension preferredSize0 = paramPanel.getPreferredSize();
        Dimension minimumSize0 = paramPanel.getMinimumSize();

        Dimension preferredSize1 = paramPanel.getPreferredSize();
        Dimension minimumSize1 = paramPanel.getMinimumSize();
        if (ocsswLocations.length > 1) {
            ocsswLocationComboBox.setSelectedIndex(1);
            preferredSize1 = paramPanel.getPreferredSize();
            minimumSize1 = paramPanel.getMinimumSize();
        }

        Dimension preferredSize2 = paramPanel.getPreferredSize();
        Dimension minimumSize2 = paramPanel.getMinimumSize();
        if (ocsswLocations.length > 2) {
            ocsswLocationComboBox.setSelectedIndex(2);
             preferredSize2 = paramPanel.getPreferredSize();
             minimumSize2 = paramPanel.getMinimumSize();
        }

        Integer preferredWidths[] = {preferredSize0.width, preferredSize1.width, preferredSize2.width};
        Integer preferredHeights[] = {preferredSize0.height, preferredSize1.height, preferredSize2.height};
        Integer minimumWidths[] = {minimumSize0.width, minimumSize1.width, minimumSize2.width};
        Integer minimumHeights[] = {minimumSize0.height, minimumSize1.height, minimumSize2.height};

        int preferredWidth = Collections.max(Arrays.asList(preferredWidths));
        int preferredHeight = Collections.max(Arrays.asList(preferredHeights));
        int minimumWidth = Collections.max(Arrays.asList(minimumWidths));
        int minimumHeight = Collections.max(Arrays.asList(minimumHeights));

        Dimension preferredParamPanelSize = new Dimension(preferredWidth, preferredHeight);
        Dimension minimumParamPanelSize = new Dimension(minimumWidth, minimumHeight);


        ocsswLocationComboBox.setSelectedIndex(lastOcsswLocationIndex);

        // Specifically set preferred and minimum size
        paramPanel.setPreferredSize(preferredParamPanelSize);
        paramPanel.setMinimumSize(minimumParamPanelSize);

        return paramPanel;
    }


    private JPanel getLocationPanel(JLabel ocsswLocationLabel, JComboBox ocsswLocationComboBox) {

        JPanel panel = GridBagUtils.createPanel();
        GridBagConstraints gbc = createConstraints();

        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(ocsswLocationLabel, gbc);

        gbc.gridx += 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(ocsswLocationComboBox, gbc);

        ocsswLocationLabel.setMinimumSize(ocsswLocationLabel.getPreferredSize());
        ocsswLocationComboBox.setMinimumSize(ocsswLocationComboBox.getPreferredSize());
        panel.setMinimumSize(panel.getPreferredSize());

        return panel;
    }


    private void updateParamPanel(JPanel newSubParamPanel) {

        paramPanel.validate();

        GridBagConstraints gbc = createConstraints();
        gbc.gridy = 1;
        gbc.weighty = 0;
        gbc.insets.top = 5;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        newSubParamPanel.setMinimumSize(newSubParamPanel.getMinimumSize());

        paramPanel.add(newSubParamPanel, gbc);

        // Add filler panel at bottom which expands as needed to force all components within this panel to the top
        gbc.insets.top = 0;
        gbc.gridy = 2;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.VERTICAL;
        JPanel fillerPanel = new JPanel();
        fillerPanel.setMinimumSize(fillerPanel.getPreferredSize());
        paramPanel.add(fillerPanel, gbc);


        paramPanel.repaint();
        paramPanel.validate();
    }


    private JPanel getVirtualMachinePanel() {

        final Preferences preferences = Config.instance("seadas").load().preferences();
        JPanel panel = GridBagUtils.createPanel();
        GridBagConstraints gbc = createConstraints();
        panel.setBorder(UIUtils.createGroupBorder("Virtual Machine"));

        JLabel ocsswSharedDirLabel = new JLabel(OCSSW_SHARED_DIR_LABEL + ": ");
        ocsswSharedDir = new JTextField(20);


        ocsswSharedDirLabel.setMinimumSize(ocsswSharedDirLabel.getPreferredSize());
        ocsswSharedDir.setMinimumSize(new JTextField(10).getPreferredSize());


        pc.addProperty(Property.create(SEADAS_CLIENT_SERVER_SHARED_DIR_PROPERTY, preferences.get(SEADAS_CLIENT_SERVER_SHARED_DIR_PROPERTY, SEADAS_CLIENT_SERVER_SHARED_DIR_DEFAULT_VALUE)));
        pc.getDescriptor(SEADAS_CLIENT_SERVER_SHARED_DIR_PROPERTY).setDisplayName(SEADAS_CLIENT_SERVER_SHARED_DIR_PROPERTY);

        final BindingContext ctx = new BindingContext(pc);

        ctx.bind(SEADAS_CLIENT_SERVER_SHARED_DIR_PROPERTY, ocsswSharedDir);


        JButton ocsswSharedDirButton = new JButton("...");


        ocsswSharedDirButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                File newDir = getDir();
                if (newDir != null) {
                    ocsswSharedDir.setText(newDir.getAbsolutePath());
                    pc.setValue(SEADAS_CLIENT_SERVER_SHARED_DIR_PROPERTY, ocsswSharedDir.getText());
                }
            }
        });

        ocsswSharedDirButton.setMinimumSize(ocsswSharedDirButton.getPreferredSize());

        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(ocsswSharedDirLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(ocsswSharedDir, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(ocsswSharedDirButton, gbc);

        panel.setMinimumSize(panel.getMinimumSize());


        return panel;
    }


    private JPanel getRemoteServerPanel() {

        final Preferences preferences = Config.instance("seadas").load().preferences();

        JPanel panel = GridBagUtils.createPanel();
        GridBagConstraints gbc = createConstraints();
        panel.setBorder(UIUtils.createGroupBorder("Remote Server"));


        JLabel ocsswServerAddressLabel = new JLabel(OCSSW_SERVER_ADDRESS_LABEL + ": ");
        JLabel serverPortLabel = new JLabel(SERVER_PORT_LABEL + ": ");
        JLabel serverInputStreamPortLabel = new JLabel(SERVER_INPUT_STREAM_PORT_LABEL + ": ");
        JLabel serverErrorStreamPortLabel = new JLabel(SERVER_ERROR_STREAM_PORT_LABEL + ": ");

        ocsswserverAddressTextfield = new JTextField(20);
        serverPortTextfield = new JTextField(4);
        serverInputStreamPortTextfield = new JTextField(4);
        serverErrorStreamPortTextfield = new JTextField(4);

        // Set minimum size for each component

        ocsswServerAddressLabel.setMinimumSize(ocsswServerAddressLabel.getPreferredSize());
        serverPortLabel.setMinimumSize(serverPortLabel.getPreferredSize());
        serverInputStreamPortLabel.setMinimumSize(serverInputStreamPortLabel.getPreferredSize());
        serverErrorStreamPortLabel.setMinimumSize(serverErrorStreamPortLabel.getPreferredSize());

        ocsswserverAddressTextfield.setMinimumSize(new JTextField(10).getPreferredSize());
        serverPortTextfield.setMinimumSize(serverPortTextfield.getPreferredSize());
        serverInputStreamPortTextfield.setMinimumSize(serverInputStreamPortTextfield.getPreferredSize());
        serverErrorStreamPortTextfield.setMinimumSize(serverErrorStreamPortTextfield.getPreferredSize());


        pc.addProperty(Property.create(SEADAS_OCSSW_SERVER_ADDRESS_PROPERTY, preferences.get(SEADAS_OCSSW_SERVER_ADDRESS_PROPERTY, SEADAS_OCSSW_SERVER_ADDRESS_DEFAULT_VALUE)));
        pc.getDescriptor(SEADAS_OCSSW_SERVER_ADDRESS_PROPERTY).setDisplayName(SEADAS_OCSSW_SERVER_ADDRESS_PROPERTY);

        pc.addProperty(Property.create(SEADAS_OCSSW_PORT_PROPERTY, preferences.get(SEADAS_OCSSW_PORT_PROPERTY, SEADAS_OCSSW_PORT_DEFAULT_VALUE)));
        pc.getDescriptor(SEADAS_OCSSW_PORT_PROPERTY).setDisplayName(SEADAS_OCSSW_PORT_PROPERTY);

        pc.addProperty(Property.create(SEADAS_OCSSW_PROCESSINPUTSTREAMPORT_PROPERTY, preferences.get(SEADAS_OCSSW_PROCESSINPUTSTREAMPORT_PROPERTY, SEADAS_OCSSW_PROCESSINPUTSTREAMPORT_DEFAULT_VALUE)));
        pc.getDescriptor(SEADAS_OCSSW_PROCESSINPUTSTREAMPORT_PROPERTY).setDisplayName(SEADAS_OCSSW_PROCESSINPUTSTREAMPORT_PROPERTY);

        pc.addProperty(Property.create(SEADAS_OCSSW_PROCESSERRORSTREAMPORT_PROPERTY, preferences.get(SEADAS_OCSSW_PROCESSERRORSTREAMPORT_PROPERTY, SEADAS_OCSSW_PROCESSERRORSTREAMPORT_DEFAULT_VALUE)));
        pc.getDescriptor(SEADAS_OCSSW_PROCESSERRORSTREAMPORT_PROPERTY).setDisplayName(SEADAS_OCSSW_PROCESSERRORSTREAMPORT_PROPERTY);



        final BindingContext ctx = new BindingContext(pc);

        ctx.bind(SEADAS_OCSSW_PORT_PROPERTY, serverPortTextfield);
        ctx.bind(SEADAS_OCSSW_PROCESSINPUTSTREAMPORT_PROPERTY, serverInputStreamPortTextfield);
        ctx.bind(SEADAS_OCSSW_PROCESSERRORSTREAMPORT_PROPERTY, serverErrorStreamPortTextfield);
        ctx.bind(SEADAS_OCSSW_SERVER_ADDRESS_PROPERTY, ocsswserverAddressTextfield);


        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(ocsswServerAddressLabel, gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        panel.add(ocsswserverAddressTextfield, gbc);

        gbc.gridy += 1;

        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(serverPortLabel, gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 1;
        panel.add(serverPortTextfield, gbc);

        gbc.gridy += 1;

        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(serverInputStreamPortLabel, gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 1;
        panel.add(serverInputStreamPortTextfield, gbc);

        gbc.gridy += 1;

        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(serverErrorStreamPortLabel, gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 1;
        panel.add(serverErrorStreamPortTextfield, gbc);

        panel.setMinimumSize(panel.getMinimumSize());


        return panel;
    }


    private JPanel getLocalOCSSWPanel() {

        final Preferences preferences = Config.instance("seadas").load().preferences();
        JPanel panel = GridBagUtils.createPanel();
        GridBagConstraints gbc = createConstraints();

        panel.setBorder(UIUtils.createGroupBorder("Local Server"));


        JLabel ocsswRootLabel = new JLabel(OCSSW_ROOT_LABEL + ": ");

        ocsswRootLabel.setMinimumSize(ocsswRootLabel.getPreferredSize());

        ocsswRootTextfield = new JTextField(20);
        ocsswRootTextfield.setMinimumSize(new JTextField(10).getPreferredSize());


        pc.addProperty(Property.create(SEADAS_OCSSW_ROOT_PROPERTY, preferences.get(SEADAS_OCSSW_ROOT_PROPERTY, SEADAS_OCSSW_ROOT_DEFAULT_VALUE)));
        pc.getDescriptor(SEADAS_OCSSW_ROOT_PROPERTY).setDisplayName(SEADAS_OCSSW_ROOT_PROPERTY);

        final BindingContext ctx = new BindingContext(pc);

        ctx.bind(SEADAS_OCSSW_ROOT_PROPERTY, ocsswRootTextfield);


        JButton ocsswRootButton = new JButton("...");
        ocsswRootButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                File newDir = getDir();
                if (newDir != null) {
                    ocsswRootTextfield.setText(newDir.getAbsolutePath());
                    pc.setValue(SEADAS_OCSSW_ROOT_PROPERTY, ocsswRootTextfield.getText());
                }
            }
        });

        ocsswRootButton.setMinimumSize(ocsswRootButton.getPreferredSize());


        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(ocsswRootLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(ocsswRootTextfield, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(ocsswRootButton, gbc);

        panel.setMinimumSize(panel.getMinimumSize());


        return panel;
    }


    public File getDir() {

        File selectedFile = null;
        JFileChooser jFileChooser = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
        jFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnValue = jFileChooser.showOpenDialog(null);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            selectedFile = jFileChooser.getSelectedFile();
            //System.out.println(selectedFile.getAbsolutePath());
        }
        return selectedFile;
    }


    public static GridBagConstraints createConstraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 1;
        gbc.weighty = 1;
        return gbc;
    }


    private JPanel ocsswBranchPanel() {

        final Preferences preferences = Config.instance("seadas").load().preferences();

        JPanel panel = GridBagUtils.createPanel();

        GridBagConstraints gbc = createConstraints();

        JLabel ocsswBranchLabel = new JLabel(OCSSW_BRANCH_LABEL + ": ");
        ocsswBranchTextfield = new JTextField(10);

        ocsswBranchLabel.setMinimumSize(ocsswBranchLabel.getPreferredSize());
        ocsswBranchTextfield.setMinimumSize(new JTextField(5).getPreferredSize());
        ocsswBranchLabel.setToolTipText(BRANCH_TOOLTIP);

        pc.addProperty(Property.create(SEADAS_OCSSW_BRANCH_PROPERTY, preferences.get(SEADAS_OCSSW_BRANCH_PROPERTY, SEADAS_OCSSW_BRANCH_DEFAULT_VALUE)));
        pc.getDescriptor(SEADAS_OCSSW_BRANCH_PROPERTY).setDisplayName(SEADAS_OCSSW_BRANCH_PROPERTY);

        final BindingContext ctx = new BindingContext(pc);

        ctx.bind(SEADAS_OCSSW_BRANCH_PROPERTY, ocsswBranchTextfield);

        gbc.fill = GridBagConstraints.NONE;

        gbc.weighty = 0;
        gbc.weightx = 0;
        panel.add(ocsswBranchLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(ocsswBranchTextfield, gbc);

        return panel;
    }




    private Dimension adjustDimension(Dimension dimension, int widthAdjustment, int heightAdjustment) {

        if (dimension == null) {
            return null;
        }

        int width = dimension.width + widthAdjustment;
        int height = dimension.height + heightAdjustment;

        return new Dimension(width, height);
    }




    /**
     * If there is a conflict between the String ocsswroot and the environment variable OCSSWROOT
     * a GUI prompts the users whether this conflict is okay.  If there is no conflict or if there
     * is a conflict and the user approves then 'true' is returned.
     *
     * @param field
     * @param ocsswroot
     * @return
     */
    private Boolean checkIfEnvironmentVariableConflict(String field, String ocsswroot) {

        Map<String, String> env = System.getenv();
        String ocsswroot_env = env.get("OCSSWROOT");

        if (ocsswroot_env == null || ocsswroot_env.trim().length() == 0) {
            return true;
        }

        if (ocsswroot_env.equals(ocsswroot)) {
            return true;
        }

        String msg = "<html>" +
                "WARNING!: You are defining OCSSW ROOT to be '" + ocsswroot + "'<br>" +
                "but on your system the environment variable OCSSWROOT points to '" + ocsswroot_env + "'<br>" +
                "This mismatch could cause problems or conflict between GUI and command line operations<br>"+
                "</html>";


        final int dialogResult = getUserResponse(msg, "Continue", "Back");

        if (dialogResult == ModalDialog.ID_OK) {
            return true;
        } else {
            return false;
        }


    }



    private Boolean checkParameters() {

        if (!isTextFieldValidBranch(OCSSW_BRANCH_LABEL, ocsswBranchTextfield)) {
            return false;
        }

        String ocssLocationString = (String) ocsswLocationComboBox.getSelectedItem();
        switch (ocssLocationString) {
            case OCSSW_LOCATION_LOCAL:
                if (!directoryCheck(OCSSW_ROOT_LABEL, ocsswRootTextfield.getText())) {
                    return false;
                }

                if (!checkIfEnvironmentVariableConflict(OCSSW_ROOT_LABEL, ocsswRootTextfield.getText())) {
                    return false;
                }

                break;

            case OCSSW_LOCATION_VIRTUAL_MACHINE:
                if (!directoryCheck(OCSSW_SHARED_DIR_LABEL, ocsswSharedDir.getText())) {
                    return false;
                }

                break;

            case OCSSW_LOCATION_REMOTE_SERVER:
                if (!isTextFieldValidIP(OCSSW_SERVER_ADDRESS_LABEL, ocsswserverAddressTextfield)) {
                    return false;
                }

                if (!isTextFieldValidPort(SERVER_PORT_LABEL, serverPortTextfield)) {
                    return false;
                }

                if (!isTextFieldValidPort(SERVER_INPUT_STREAM_PORT_LABEL, serverInputStreamPortTextfield)) {
                    return false;
                }

                if (!isTextFieldValidPort(SERVER_ERROR_STREAM_PORT_LABEL, serverErrorStreamPortTextfield)) {
                    return false;
                }

                break;
        }

        return true;
    }




    public boolean isTextFieldValidBranch(String field, JTextField textfield) {

        boolean valid = true;

        if (textfield != null) {
            if (textfieldHasValue(textfield)) {
                String branch = textfield.getText().trim();

                if (!isValidBranch(branch)) {
                    notifyError("<html>" + field + "='" + branch + "' is not a valid OCSSW branch. <br>" +
                            "The OCSSW branch must contain 2 fields of the form X.X where X is an integer</html>");
                    return false;
                }

                if (!isDefaultBranch(branch)) {
                    return false;
                }

            } else {
                notifyError("'" + field + "' must contain an OCSSW Branch");
                valid = false;
            }
        } else {
            notifyError("'" + field + "' not defined");
            valid = false;
        }

        return valid;
    }



    public boolean isDefaultBranch(String branch) {

        if (branch == null) {
            return false;
        }

        branch.trim();

        if (SEADAS_OCSSW_BRANCH_DEFAULT_VALUE.equals(branch)) {
            return true;
        }

        String msg = "<html>" +
                "WARNING!: Your current SeaDAS version has default branch='" + SEADAS_OCSSW_BRANCH_DEFAULT_VALUE + "'<br>"+
                "You have selected to use branch='" + branch + "'<br>" +
                "This version mismatch could cause possible problems when running from the SeaDAS GUI" +
                "</html>";

        final int dialogResult = getUserResponse(msg, "Continue", "Back");

        if (dialogResult == ModalDialog.ID_OK) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * This method scans for valid OCSSW tags at https://oceandata.sci.gsfc.nasa.gov/manifest/tags, returns a list of tags that start with capital letter "V"
     * @return List of valid OCSSW tags for SeaDAS
     */
    public ArrayList<String> getValidOcsswTagsFromURL(){
        ArrayList<String> validOcsswTags = new ArrayList<>();
        int i =0;
        try {
            Connection connection = Jsoup.connect("https://oceandata.sci.gsfc.nasa.gov/manifest/tags");
            Document doc = connection.get(); doc.children();
            String tagName;
            for (Element file : doc.getElementsByAttribute("href")) {
                tagName = file.attr("href");
                System.out.println(tagName);
                if (tagName.startsWith("V")) {
                    validOcsswTags.add(tagName);
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        return validOcsswTags;
    }

    /**
     * This method scans for valid OCSSW tags at https://oceandata.sci.gsfc.nasa.gov/manifest/tags, returns a list of tags that start with capital letter "V"
     * @return List of valid OCSSW tags for SeaDAS
     */
    public ArrayList<String> getValidOcsswTagsFromCLI(){
        ArrayList<String> validOcsswTags = new ArrayList<>();
        int i =0;
        try {
            Connection connection = Jsoup.connect("https://oceandata.sci.gsfc.nasa.gov/manifest/tags");
            Document doc = connection.get(); doc.children();
            String tagName;
            for (Element file : doc.getElementsByAttribute("href")) {
                tagName = file.attr("href");
                System.out.println(tagName);
                if (tagName.startsWith("V")) {
                    validOcsswTags.add(tagName);
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        return validOcsswTags;
    }

    /**
     * This method downloads the ocssw installer program ocssw_install and manifest.py to a tmp directory
     * @return
     */
    public boolean downloadOCSSWInstaller() {

        boolean downloadSuccessful = true;
        try {

            //download install_ocssw
            URL website = new URL(OCSSW_INSTALLER_URL);
            ReadableByteChannel rbc = Channels.newChannel(website.openStream());
            FileOutputStream fos = new FileOutputStream(TMP_OCSSW_INSTALLER);
            fos.getChannel().transferFrom(rbc, 0, 1 << 24);
            fos.close();
            (new File(TMP_OCSSW_INSTALLER)).setExecutable(true);

            //download manifest.py
            website = new URL(OCSSW_MANIFEST_URL);
            rbc = Channels.newChannel(website.openStream());
            fos = new FileOutputStream(TMP_OCSSW_MANIFEST);
            fos.getChannel().transferFrom(rbc, 0, 1 << 24);
            fos.close();
            (new File(TMP_OCSSW_MANIFEST)).setExecutable(true);

            //download seadasVersion.json
            website = new URL(OCSSW_SEADAS_VERSIONS_URL);
            rbc = Channels.newChannel(website.openStream());
            fos = new FileOutputStream(TMP_SEADAS_OCSSW_VERSIONS_FILE);
            fos.getChannel().transferFrom(rbc, 0, 1 << 24);
            fos.close();

            //download directory
            website = new URL("https://oceandata.sci.gsfc.nasa.gov/manifest/tags");

            Scanner sc = new Scanner(website.openStream());
            while(sc.hasNextLine())
                System.out.println(sc.nextLine());

            System.out.println(website.toString());
        } catch (MalformedURLException malformedURLException) {
            downloadSuccessful = false;
            malformedURLException.printStackTrace();
        } catch (FileNotFoundException fileNotFoundException) {
            downloadSuccessful = false;
            fileNotFoundException.printStackTrace();
        } catch (IOException ioe) {
            downloadSuccessful = false;
            ioe.printStackTrace();
        } finally {
            return downloadSuccessful;
        }
    }


    public static boolean isValidBranch(final String branch) {

        String[] arrOfBranch = branch.split("\\.");

        if (arrOfBranch.length == 2) {
            if (isNumeric(arrOfBranch[0]) && isNumeric(arrOfBranch[1])) {
                return true;
            }

        } else {
            return false;
        }

        return false;
    }

    public boolean isTextFieldValidIP(String label, JTextField textfield) {

        if (textfield != null) {
            if (textfieldHasValue(textfield)) {
                if (isValidIP(textfield.getText())) {
                    return true;
                } else {
                    notifyError("<html>" + label + "='" + textfield.getText() + "' is not a valid IP address</html>");
                }
            } else {
                notifyError("'" + label + "' must contain an IP address");
            }
        } else {
            notifyError("'" + label + "' not defined");
        }

        return false;
    }


    public boolean isTextFieldValidPort(String label, JTextField textfield) {

        if (textfield != null) {
            if (textfieldHasValue(textfield)) {
                if (isValidPort(textfield.getText())) {
                    return true;
                } else {
                    notifyError("'" + label + "=" + textfield.getText() + "' is not a valid port");
                }
            } else {
                notifyError("'" + label + "' must contain a port number");
            }
        } else {
            notifyError("'" + label + "' not defined");
        }

        return false;
    }

    public static boolean textfieldHasValue(JTextField textfield) {
        if (textfield.getText() != null && textfield.getText().length() > 0) {
            return true;
        } else {
            return false;
        }
    }


    public static boolean isValidPort(String str) {
        int port;

        try {
            port = Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return false;
        }

        if (port >= 1 && port <= 65535) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isValidIP(final String ip) {
        String PATTERN = "^((0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)\\.){3}(0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)$";

        return ip.matches(PATTERN);
    }


    public static boolean isNumeric(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }


    private int getUserResponse(String msg, String okText, String cancelText) {

        JPanel panel = GridBagUtils.createPanel();

        JLabel label = new JLabel(msg);

        GridBagConstraints gbc = createConstraints();

        gbc.anchor = GridBagConstraints.NORTH;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 1;
        gbc.weightx = 1;

        panel.add(label, gbc);

        ModalDialog dialog = new ModalDialog(modalDialog.getParent(), PANEL_NAME, panel, ModalDialog.ID_OK_CANCEL, HELP_ID);
        dialog.getButton(ModalDialog.ID_OK).setText(okText);
        dialog.getButton(ModalDialog.ID_CANCEL).setText(cancelText);
        dialog.getButton(ModalDialog.ID_OK).setMinimumSize(dialog.getButton(ModalDialog.ID_OK).getPreferredSize());

        // Specifically set sizes for dialog here
        Dimension minimumSizeAdjusted = adjustDimension(dialog.getJDialog().getMinimumSize(), 25, 50);
        Dimension preferredSizeAdjusted = adjustDimension(dialog.getJDialog().getPreferredSize(), 25, 50);
        dialog.getJDialog().setMinimumSize(minimumSizeAdjusted);
        dialog.getJDialog().setPreferredSize(preferredSizeAdjusted);

        dialog.getJDialog().pack();

        return dialog.show();
    }





    private Boolean directoryCheck(String field, String filename) {
        File dir = new File(filename);

        if (dir.exists()) {
            return true;
        }

        String msg = "<html>" + field + " directory: '" + filename + "' does not exist" + "</html>";

        final int dialogResult = getUserResponse(msg, "Create Directory", "Back");

        if (dialogResult == ModalDialog.ID_OK) {

            try {
                dir.mkdirs();

                if (dir.exists()) {
                    return true;
                } else {
                    msg = "<html>Failed to create directory '" +  filename + "'<br></html>";
                }
            } catch (Exception e){
                msg = "<html>Failed to create directory '" +  filename + "'<br>" +
                        e.toString() + "</html>";
            }

            notifyError(msg);
        }

        return false;
    }



    private void notifyError(String msg) {
        JOptionPane.showMessageDialog(null, msg, PANEL_NAME, JOptionPane.WARNING_MESSAGE);
    }

}
