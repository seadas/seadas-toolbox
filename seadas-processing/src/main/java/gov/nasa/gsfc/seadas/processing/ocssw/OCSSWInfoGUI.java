package gov.nasa.gsfc.seadas.processing.ocssw;


import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.swing.binding.BindingContext;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.GridBagUtils;
import org.esa.snap.ui.ModalDialog;
import org.esa.snap.ui.UIUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;

import static gov.nasa.gsfc.seadas.processing.ocssw.OCSSWConfigData.*;
import static gov.nasa.gsfc.seadas.processing.ocssw.OCSSWInfo.OCSSW_LOCATION_LOCAL;
import static gov.nasa.gsfc.seadas.processing.ocssw.OCSSWInfo.OCSSW_LOCATION_REMOTE_SERVER;
import static gov.nasa.gsfc.seadas.processing.ocssw.OCSSWInfo.OCSSW_LOCATION_VIRTUAL_MACHINE;


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
    final String HELP_ID = "ocsswInfo";

    final String  BRANCH_TOOLTIP = "<html>The OCSSW installation branch<br>" +
            "This by default will match the SeaDAS version (first two decimal fields)<br> " +
            "For instance SeaDAS 7.5.3 by default will use OCSSW branch 7.5<br>" +
            "If OCSSW was manually updated to a different branch then this parameter needs to be set to match</html>";

    JPanel paramPanel = GridBagUtils.createPanel();
    JPanel paramSubPanel;

    ModalDialog modalDialog;

    JTextField ocsswserverAddressTextfield;
    JTextField serverPortTextfield;
    JTextField serverInputStreamPortTextfield;
    JTextField serverErrorStreamPortTextfield;


    PropertyContainer pc = new PropertyContainer();

    OCSSWConfigData ocsswConfigData = new OCSSWConfigData();


    public static void main(String args[]) {

        final AppContext appContext = SnapApp.getDefault().getAppContext();
        final Window parent = appContext.getApplicationWindow();
        OCSSWInfoGUI ocsswInfoGUI = new OCSSWInfoGUI();
        ocsswInfoGUI.init(parent);
    }

    public void init(Window parent) {

        JPanel mainPanel = GridBagUtils.createPanel();

        modalDialog = new ModalDialog(parent, PANEL_NAME, mainPanel, ModalDialog.ID_OK_APPLY_CANCEL_HELP, HELP_ID);

        modalDialog.getButton(ModalDialog.ID_OK).setText("OK");
        modalDialog.getButton(ModalDialog.ID_CANCEL).setText("Cancel");
        modalDialog.getButton(ModalDialog.ID_APPLY).setText("Apply");
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


        final int dialogResult = modalDialog.show();

        if (dialogResult != ModalDialog.ID_OK) {
            ocsswConfigData.updateconfigData(pc);
            return;
        }
    }

    private Dimension adjustDimension(Dimension dimension, int widthAdjustment, int heightAdjustment) {

        if (dimension == null) {
            return null;
        }

        int width = dimension.width + widthAdjustment;
        int height = dimension.height + heightAdjustment;

        return new Dimension(width, height);
    }






    private JPanel makeParamPanel() {

        GridBagConstraints gbc = createConstraints();

        JLabel ocsswLocationLabel = new JLabel("OCSSW Location: ");
        String[] ocsswLocations = {OCSSW_LOCATION_LOCAL, OCSSW_LOCATION_VIRTUAL_MACHINE, OCSSW_LOCATION_REMOTE_SERVER};


        JComboBox ocsswLocationComboBox = new JComboBox(ocsswLocations);
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

        ocsswLocationComboBox.setSelectedIndex(1);
        Dimension preferredSize1 = paramPanel.getPreferredSize();
        Dimension minimumSize1 = paramPanel.getMinimumSize();

        ocsswLocationComboBox.setSelectedIndex(2);
        Dimension preferredSize2 = paramPanel.getPreferredSize();
        Dimension minimumSize2 = paramPanel.getMinimumSize();

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


        // Set selector to desired default index
        ocsswLocationComboBox.setSelectedIndex(0);

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

//
//        Component[] components = paramPanel.getComponents();
//        for (int i = 0; i < components.length; i++) {
//            if (components[i].getClass() == JPanel.class) {
//                paramPanel.remove(i);
//            }
//        }

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

        JPanel panel = GridBagUtils.createPanel();
        GridBagConstraints gbc = createConstraints();
        panel.setBorder(UIUtils.createGroupBorder("Virtual Machine"));

        JLabel ocsswSharedDirLabel = new JLabel("OCSSW Shared Dir: ");
        JTextField ocsswSharedDir = new JTextField(20);


        ocsswSharedDirLabel.setMinimumSize(ocsswSharedDirLabel.getPreferredSize());
        ocsswSharedDir.setMinimumSize(new JTextField(10).getPreferredSize());


        pc.addProperty(Property.create(SEADAS_CLIENT_SERVER_SHARED_DIR_PROPERTY, SEADAS_CLIENT_SERVER_SHARED_DIR_DEFAULT_VALUE));
        pc.getDescriptor(SEADAS_CLIENT_SERVER_SHARED_DIR_PROPERTY).setDisplayName(SEADAS_CLIENT_SERVER_SHARED_DIR_PROPERTY);

        final BindingContext ctx = new BindingContext(pc);

        ctx.bind(SEADAS_CLIENT_SERVER_SHARED_DIR_PROPERTY, ocsswSharedDir);


        if (simpleTextfieldCheck(ocsswSharedDir, true)) {
            modalDialog.getButton(ModalDialog.ID_APPLY).setEnabled(true);
        } else {
            modalDialog.getButton(ModalDialog.ID_APPLY).setEnabled(false);
        }
        ocsswSharedDir.getDocument().addDocumentListener(simpleTextfieldDocumentListener(ocsswSharedDir, true));


//        ctx.addPropertyChangeListener(SEADAS_CLIENT_SERVER_SHARED_DIR_PROPERTY, new PropertyChangeListener() {
//
//            @Override
//            public void propertyChange(PropertyChangeEvent pce) {
//
//                System.out.println("value changed!");
//            }
//        });

        JButton ocsswSharedDirButton = new JButton("...");


        ocsswSharedDirButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                File newDir = getDir();
                if (newDir != null) {
                    ocsswSharedDir.setText(newDir.getAbsolutePath());
                    modalDialog.getButton(ModalDialog.ID_APPLY).setEnabled(true);
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

        JPanel panel = GridBagUtils.createPanel();
        GridBagConstraints gbc = createConstraints();
        panel.setBorder(UIUtils.createGroupBorder("Remote Server"));


        JLabel ocsswServerAddressLabel = new JLabel("OCSSW Server Address: ");
        JLabel serverPortLabel = new JLabel("Server Port: ");
        JLabel serverInputStreamPortLabel = new JLabel("Server Input Stream Port: ");
        JLabel serverErrorStreamPortLabel = new JLabel("Server Error Stream Port: ");

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


        pc.addProperty(Property.create(SEADAS_OCSSW_PORT_PROPERTY, SEADAS_OCSSW_PORT_DEFAULT_VALUE));
        pc.getDescriptor(SEADAS_OCSSW_PORT_PROPERTY).setDisplayName(SEADAS_OCSSW_PORT_PROPERTY);

        pc.addProperty(Property.create(SEADAS_OCSSW_PROCESSINPUTSTREAMPORT_PROPERTY, SEADAS_OCSSW_PROCESSINPUTSTREAMPORT_DEFAULT_VALUE));
        pc.getDescriptor(SEADAS_OCSSW_PROCESSINPUTSTREAMPORT_PROPERTY).setDisplayName(SEADAS_OCSSW_PROCESSINPUTSTREAMPORT_PROPERTY);

        pc.addProperty(Property.create(SEADAS_OCSSW_PROCESSERRORSTREAMPORT_PROPERTY, SEADAS_OCSSW_PROCESSERRORSTREAMPORT_DEFAULT_VALUE));
        pc.getDescriptor(SEADAS_OCSSW_PROCESSERRORSTREAMPORT_PROPERTY).setDisplayName(SEADAS_OCSSW_PROCESSERRORSTREAMPORT_PROPERTY);


        final BindingContext ctx = new BindingContext(pc);

        ctx.bind(SEADAS_OCSSW_PORT_PROPERTY, serverPortTextfield);
        ctx.bind(SEADAS_OCSSW_PROCESSINPUTSTREAMPORT_PROPERTY, serverInputStreamPortTextfield);
        ctx.bind(SEADAS_OCSSW_PROCESSERRORSTREAMPORT_PROPERTY, serverErrorStreamPortTextfield);


        if (remoteServerParametersCheck()) {
            modalDialog.getButton(ModalDialog.ID_APPLY).setEnabled(true);
        } else {
            modalDialog.getButton(ModalDialog.ID_APPLY).setEnabled(false);
        }
        ocsswserverAddressTextfield.getDocument().addDocumentListener(remoteServerTextfieldsDocumentListener());
        serverPortTextfield.getDocument().addDocumentListener(remoteServerTextfieldsDocumentListener());
        serverInputStreamPortTextfield.getDocument().addDocumentListener(remoteServerTextfieldsDocumentListener());
        serverErrorStreamPortTextfield.getDocument().addDocumentListener(remoteServerTextfieldsDocumentListener());

//
//        ctx.addPropertyChangeListener(SEADAS_OCSSW_PORT_PROPERTY, new PropertyChangeListener() {
//
//            @Override
//            public void propertyChange(PropertyChangeEvent pce) {
//
//                System.out.println("value changed!");
//            }
//        });


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

        JPanel panel = GridBagUtils.createPanel();
        GridBagConstraints gbc = createConstraints();

        panel.setBorder(UIUtils.createGroupBorder("Local Server"));


        JLabel ocsswRootLabel = new JLabel("OCSSW ROOT: ");
        JTextField ocsswRootTextfield = new JTextField(20);

        ocsswRootLabel.setMinimumSize(ocsswRootLabel.getPreferredSize());
        ocsswRootTextfield.setMinimumSize(new JTextField(10).getPreferredSize());


        pc.addProperty(Property.create(SEADAS_OCSSW_ROOT_PROPERTY, SEADAS_OCSSW_ROOT_DEFAULT_VALUE));
        pc.getDescriptor(SEADAS_OCSSW_ROOT_PROPERTY).setDisplayName(SEADAS_OCSSW_ROOT_PROPERTY);
        final BindingContext ctx = new BindingContext(pc);

        ctx.bind(SEADAS_OCSSW_ROOT_PROPERTY, ocsswRootTextfield);


        if (simpleTextfieldCheck(ocsswRootTextfield, true)) {
            modalDialog.getButton(ModalDialog.ID_APPLY).setEnabled(true);
        } else {
            modalDialog.getButton(ModalDialog.ID_APPLY).setEnabled(false);
        }
        ocsswRootTextfield.getDocument().addDocumentListener(simpleTextfieldDocumentListener(ocsswRootTextfield, true));


        ctx.addPropertyChangeListener(SEADAS_OCSSW_ROOT_PROPERTY, new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent pce) {

                System.out.println("value changed!");
            }
        });


        JButton ocsswRootButton = new JButton("...");
        ocsswRootButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                File newDir = getDir();
                if (newDir != null) {
                    ocsswRootTextfield.setText(newDir.getAbsolutePath());
                    modalDialog.getButton(ModalDialog.ID_APPLY).setEnabled(true);
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
            System.out.println(selectedFile.getAbsolutePath());
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



    private DocumentListener remoteServerTextfieldsDocumentListener() {
        return new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                handler(e);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                handler(e);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                handler(e);
            }

            private void handler(DocumentEvent e) {
                if (remoteServerParametersCheck()) {
                    modalDialog.getButton(ModalDialog.ID_APPLY).setEnabled(true);
                } else {
                    modalDialog.getButton(ModalDialog.ID_APPLY).setEnabled(false);
                }
            }
        };
    }

    private boolean remoteServerParametersCheck() {

        if (simpleTextfieldCheck(ocsswserverAddressTextfield, false)
                && simpleTextfieldCheck(serverPortTextfield, false)
                && simpleTextfieldCheck(serverInputStreamPortTextfield, false)
                && simpleTextfieldCheck(serverErrorStreamPortTextfield, false)) {
            return true;
        } else {
            return false;
        }

    }

    private DocumentListener simpleTextfieldDocumentListener(JTextField textfield, boolean directoryCheck) {
        return new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                handler(e);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                handler(e);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                handler(e);
            }

            private void handler(DocumentEvent e) {
                if (simpleTextfieldCheck(textfield, directoryCheck)) {
                    modalDialog.getButton(ModalDialog.ID_APPLY).setEnabled(true);
                } else {
                    modalDialog.getButton(ModalDialog.ID_APPLY).setEnabled(false);
                }
            }
        };
    }


    private boolean simpleTextfieldCheck(JTextField textfield, boolean directoryCheck) {
        if (textfield.getText().length() > 0) {
            if (!directoryCheck) {
                return true;
            } else {
                File dir = new File(textfield.getText());

                if (dir.exists()) {
                    return true;
                } else {
                    return false;
                }
            }
        } else {
            return false;
        }
    }

    private JPanel ocsswBranchPanel() {

        JPanel panel = GridBagUtils.createPanel();

        GridBagConstraints gbc = createConstraints();

        JLabel ocsswBranchLabel = new JLabel("OCSSW Branch: ");
        JTextField ocsswBranchTextfield = new JTextField(10);

        ocsswBranchLabel.setMinimumSize(ocsswBranchLabel.getPreferredSize());
        ocsswBranchTextfield.setMinimumSize(new JTextField(5).getPreferredSize());
        ocsswBranchLabel.setToolTipText(BRANCH_TOOLTIP);

        pc.addProperty(Property.create(SEADAS_OCSSW_BRANCH_PROPERTY, SEADAS_OCSSW_BRANCH_DEFAULT_VALUE));
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


}
