package gov.nasa.gsfc.seadas.processing.ocssw;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.swing.binding.BindingContext;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.ModalDialog;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

import static gov.nasa.gsfc.seadas.processing.ocssw.OCSSWConfigData.*;
import static gov.nasa.gsfc.seadas.processing.ocssw.OCSSWInfo.*;

public class OCSSWInfoGUI {

    final String PARAM_PANEL_NAME = "paramPanel";
    JPanel paramPanel = new JPanel();
    JPanel paramSubPanel;

    PropertyContainer pc = new PropertyContainer();

    OCSSWConfigData ocsswConfigData = new OCSSWConfigData();

    JButton ok = new JButton("OK");
    JButton cancel = new JButton("Cancel");
    JButton apply = new JButton("Apply");
    JButton help = new JButton("Help");

    public static void main(String args[]) {

        final AppContext appContext = SnapApp.getDefault().getAppContext();
        final Window parent = appContext.getApplicationWindow();
        OCSSWInfoGUI ocsswInfoGUI = new OCSSWInfoGUI();
        ocsswInfoGUI.init(parent);
    }

    public void init(Window parent) {

        JMenuBar mb = new JMenuBar();
        JMenu m1 = new JMenu("FILE");
        JMenu m2 = new JMenu("Help");
        mb.add(m1);
        mb.add(m2);
        JMenuItem m11 = new JMenuItem("Load");
        JMenuItem m22 = new JMenuItem("Save as");
        JMenuItem m33 = new JMenuItem("Exit");
        m1.add(m11);
        m1.add(m22);
        m1.add(m33);


        JPanel mainPanel = new JPanel();
        mainPanel.add(BorderLayout.NORTH, mb);
        mainPanel.add(BorderLayout.SOUTH, makeParamPanel());
        mainPanel.setPreferredSize(mainPanel.getPreferredSize());
        final ModalDialog modalDialog = new ModalDialog(parent, "OCSSW Configuration", mainPanel, ModalDialog.ID_OK_APPLY_CANCEL_HELP, "ocsswInfo");

        modalDialog.getButton(ModalDialog.ID_OK).setText("OK");
        modalDialog.getButton(ModalDialog.ID_CANCEL).setText("Cancel");
        modalDialog.getButton(ModalDialog.ID_APPLY).setText("Apply");
        modalDialog.getButton(ModalDialog.ID_HELP).setText("Help");

        modalDialog.getJDialog().setMaximumSize(modalDialog.getJDialog().getPreferredSize());
        modalDialog.getJDialog().pack();

        final int dialogResult = modalDialog.show();

        if (dialogResult != ModalDialog.ID_OK) {
            ;
            ocsswConfigData.updateconfigData(pc);
            return;
        }
    }

    private JPanel makeParamPanel() {

        paramPanel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();


        JLabel ocsswLocationLabel = new JLabel("OCSSW Location:");
        String[] ocsswLocations = {OCSSW_LOCATION_LOCAL, OCSSW_LOCATION_VIRTUAL_MACHINE, OCSSW_LOCATION_REMOTE_SERVER};


        JComboBox ocsswLocationList = new JComboBox(ocsswLocations);
        ocsswLocationList.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String ocssLocationString = (String) ocsswLocationList.getSelectedItem(); //get the selected item
                switch (ocssLocationString) {//check for a match
                    case OCSSW_LOCATION_LOCAL:
                        paramSubPanel = getLocalOCSSWParamPanel();
                        break;
                    case OCSSW_LOCATION_VIRTUAL_MACHINE:
                        paramSubPanel = getVirtualOCSSWParamPanel();
                        break;
                    case OCSSW_LOCATION_REMOTE_SERVER:
                        paramSubPanel = getRemoteOCSSWParamPanel();
                        break;
                }
                updateParamPanel(paramSubPanel);
            }
        });

        ocsswLocationList.setSelectedIndex(1);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 50, 0, 50);  //top padding
        c.gridx = 0;
        c.gridy = 0;

        paramPanel.add(ocsswLocationLabel, c);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.5;
        c.gridx = 1;
        c.gridy = 0;
        paramPanel.add(ocsswLocationList, c);
        paramPanel.setPreferredSize(paramPanel.getPreferredSize());
        return paramPanel;
    }

    private void updateParamPanel(JPanel newSubParamPanel) {

        Component[] components = paramPanel.getComponents();
        for (int i = 0; i < components.length; i++) {
            if (components[i].getClass() == JPanel.class) {
                paramPanel.remove(i);
            }
        }

        paramPanel.validate();

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.5;
        c.gridx = 0;
        c.gridy = 2;
        c.insets = new Insets(30, 50, 0, 50);  //top padding

        paramPanel.add(newSubParamPanel, c);
        paramPanel.repaint();
        paramPanel.validate();
    }

    private JPanel getVirtualOCSSWParamPanel() {

        JPanel virtualOCSSWParamPanel = new JPanel();
        virtualOCSSWParamPanel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        JLabel ocsswSharedDirLabel = new JLabel("OCSSW Shared Dir:");
        JTextField ocsswSharedDir = new JTextField();

        pc.addProperty(Property.create(SEADAS_CLIENT_SERVER_SHARED_DIR_PROPERTY, SEADAS_CLIENT_SERVER_SHARED_DIR_DEFAULT_VALUE));
        pc.getDescriptor(SEADAS_CLIENT_SERVER_SHARED_DIR_PROPERTY).setDisplayName(SEADAS_CLIENT_SERVER_SHARED_DIR_PROPERTY);

        final BindingContext ctx = new BindingContext(pc);

        ctx.bind(SEADAS_CLIENT_SERVER_SHARED_DIR_PROPERTY, ocsswSharedDir);

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
                    apply.setEnabled(true);
                }
            }
        });

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.gridy = 0;

        virtualOCSSWParamPanel.add(ocsswSharedDirLabel, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.5;
        c.gridx = 2;
        c.gridy = 0;

        virtualOCSSWParamPanel.add(ocsswSharedDir, c);

        c.weightx = 0.5;
        c.gridx = 3;
        c.gridy = 0;

        virtualOCSSWParamPanel.add(ocsswSharedDirButton, c);


        return virtualOCSSWParamPanel;
    }

    private JPanel getRemoteOCSSWParamPanel() {

        JPanel remoteOCSSWParamPanel = new JPanel();
        remoteOCSSWParamPanel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        JLabel ocsswServerAddressLabel = new JLabel("OCSSW Server Address:");
        JTextField ocsswserverAddress = new JTextField(20); // accepts upto 20 characters


        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.gridy = 0;

        remoteOCSSWParamPanel.add(ocsswServerAddressLabel, c);
        c.weightx = 0.5;
        c.gridx = 2;
        c.gridy = 0;       //third row
        remoteOCSSWParamPanel.add(ocsswserverAddress, c);

        JLabel serverPortLabel = new JLabel("Server Port: ");
        JLabel serverInputStreamPortLabel = new JLabel("Server Input Stream Port: ");
        JLabel serverErrorStreamPortLabel = new JLabel("Server Error Stream Port: ");

        JTextField serverPortNumber = new JTextField(4);
        JTextField serverInputStreamPortNumber = new JTextField(4);
        JTextField serverErrorStreamPortNumber = new JTextField(4);

        pc.addProperty(Property.create(SEADAS_OCSSW_PORT_PROPERTY, SEADAS_OCSSW_PORT_DEFAULT_VALUE));
        pc.getDescriptor(SEADAS_OCSSW_PORT_PROPERTY).setDisplayName(SEADAS_OCSSW_PORT_PROPERTY);

        pc.addProperty(Property.create(SEADAS_OCSSW_PROCESSINPUTSTREAMPORT_PROPERTY, SEADAS_OCSSW_PROCESSINPUTSTREAMPORT_DEFAULT_VALUE));
        pc.getDescriptor(SEADAS_OCSSW_PROCESSINPUTSTREAMPORT_PROPERTY).setDisplayName(SEADAS_OCSSW_PROCESSINPUTSTREAMPORT_PROPERTY);

        pc.addProperty(Property.create(SEADAS_OCSSW_PROCESSERRORSTREAMPORT_PROPERTY, SEADAS_OCSSW_PROCESSERRORSTREAMPORT_DEFAULT_VALUE));
        pc.getDescriptor(SEADAS_OCSSW_PROCESSERRORSTREAMPORT_PROPERTY).setDisplayName(SEADAS_OCSSW_PROCESSERRORSTREAMPORT_PROPERTY);


        final BindingContext ctx = new BindingContext(pc);

        ctx.bind(SEADAS_OCSSW_PORT_PROPERTY, serverPortNumber);
        ctx.bind(SEADAS_OCSSW_PROCESSINPUTSTREAMPORT_PROPERTY, serverInputStreamPortNumber);
        ctx.bind(SEADAS_OCSSW_PROCESSERRORSTREAMPORT_PROPERTY, serverErrorStreamPortNumber);
//
//        ctx.addPropertyChangeListener(SEADAS_OCSSW_PORT_PROPERTY, new PropertyChangeListener() {
//
//            @Override
//            public void propertyChange(PropertyChangeEvent pce) {
//
//                System.out.println("value changed!");
//            }
//        });

        c.gridx = 0;
        c.gridy = 2;
        //c.insets = new Insets(20, 0, 0, 0);  //top padding
        remoteOCSSWParamPanel.add(serverPortLabel, c);

        c.gridx = 1;
        c.gridy = 2;
        remoteOCSSWParamPanel.add(serverPortNumber, c);

        c.gridx = 2;
        c.gridy = 2;
        remoteOCSSWParamPanel.add(serverInputStreamPortLabel, c);

        c.gridx = 3;
        c.gridy = 2;
        remoteOCSSWParamPanel.add(serverInputStreamPortNumber, c);

        c.gridx = 4;
        c.gridy = 2;
        remoteOCSSWParamPanel.add(serverErrorStreamPortLabel, c);

        c.gridx = 5;
        c.gridy = 2;
        remoteOCSSWParamPanel.add(serverErrorStreamPortNumber, c);

        return remoteOCSSWParamPanel;
    }

    private JPanel getLocalOCSSWParamPanel() {
        JPanel localOCSSWParamPanel = new JPanel();
        localOCSSWParamPanel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        JLabel ocsswRootLabel = new JLabel("OCSSW ROOT:");
        JTextField ocsswRoot = new JTextField();


        pc.addProperty(Property.create(SEADAS_OCSSW_ROOT_PROPERTY, SEADAS_OCSSW_ROOT_DEFAULT_VALUE));
        pc.getDescriptor(SEADAS_OCSSW_ROOT_PROPERTY).setDisplayName(SEADAS_OCSSW_ROOT_PROPERTY);
        final BindingContext ctx = new BindingContext(pc);

        ctx.bind(SEADAS_OCSSW_ROOT_PROPERTY, ocsswRoot);

        ctx.addPropertyChangeListener(SEADAS_OCSSW_ROOT_PROPERTY, new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent pce) {

                System.out.println("value changed!");
            }
        });

        JButton ocsswDirButton = new JButton("...");
        ocsswDirButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                File newDir = getDir();
                if (newDir != null) {
                    ocsswRoot.setText(newDir.getAbsolutePath());
                    apply.setEnabled(true);
                }
            }
        });
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.gridy = 0;

        localOCSSWParamPanel.add(ocsswRootLabel, c);

        c.weightx = 0.5;
        c.gridx = 2;
        c.gridy = 0;

        localOCSSWParamPanel.add(ocsswRoot, c);

        c.weightx = 0.5;
        c.gridx = 3;
        c.gridy = 0;

        localOCSSWParamPanel.add(ocsswDirButton, c);

        return localOCSSWParamPanel;
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
}
