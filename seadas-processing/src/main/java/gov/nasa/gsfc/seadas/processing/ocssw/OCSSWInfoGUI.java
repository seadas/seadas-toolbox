package gov.nasa.gsfc.seadas.processing.ocssw;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.swing.binding.BindingContext;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

import static gov.nasa.gsfc.seadas.processing.ocssw.OCSSWConfigData.SEADAS_OCSSW_ROOT_DEFAULT_VALUE;
import static gov.nasa.gsfc.seadas.processing.ocssw.OCSSWConfigData.SEADAS_OCSSW_ROOT_PROPERTY;
import static gov.nasa.gsfc.seadas.processing.ocssw.OCSSWInfo.*;

public class OCSSWInfoGUI {

    final String PARAM_PANEL_NAME = "paramPanel";
    JPanel paramPanel = new JPanel();
    JPanel paramSubPanel;

    PropertyContainer pc = new PropertyContainer();

    OCSSWConfigData ocsswConfigData = new OCSSWConfigData();

    public static void main(String args[]) {
        OCSSWInfoGUI ocsswInfoGUI = new OCSSWInfoGUI();
        ocsswInfoGUI.init();
        ;
    }

    private void init() {


        //Creating the Frame
        JFrame frame = new JFrame("OCSSW Configuration");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 400);

        //Creating the MenuBar and adding components
        JMenuBar mb = new JMenuBar();
        JMenu m1 = new JMenu("FILE");
        JMenu m2 = new JMenu("Help");
        mb.add(m1);
        mb.add(m2);
        JMenuItem m11 = new JMenuItem("Load");
        JMenuItem m22 = new JMenuItem("Save as");
        m1.add(m11);
        m1.add(m22);


        //Creating the bottomPanel at bottom and adding components
        JPanel bottomPanel = new JPanel(); // the bottomPanel is not visible in output

        JButton ok = new JButton("OK");
        ok.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ocsswConfigData.updateconfigData(pc);
                closeCurrentWindow(e);
            }
        });
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                closeCurrentWindow(e);
            }
        });
        JButton apply = new JButton("Apply");
        apply.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ocsswConfigData.updateconfigData(pc);
            }
        });

        JButton help = new JButton("Help");
        apply.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

            }
        });

        bottomPanel.add(ok);
        bottomPanel.add(cancel);
        bottomPanel.add(apply);
        bottomPanel.add(help);

        // Text Area at the Center
        JTextArea ta = new JTextArea();

        //Adding Components to the frame.
        frame.getContentPane().add(BorderLayout.SOUTH, bottomPanel);
        frame.getContentPane().add(BorderLayout.NORTH, mb);
        frame.getContentPane().add(BorderLayout.CENTER, makeParamPanel());
        frame.setVisible(true);
    }

    private void closeCurrentWindow(ActionEvent e){

    }

    private JPanel makeParamPanel() {

        paramPanel.setBackground(Color.white);
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
        c.gridx = 1;
        c.gridy = 2;
        c.insets = new Insets(30, 20, 0, 20);  //top padding
        c.anchor = GridBagConstraints.PAGE_END; //bottom of space

        paramPanel.add(newSubParamPanel, c);
        paramPanel.repaint();
        paramPanel.validate();
    }

    private JPanel getVirtualOCSSWParamPanel() {

        JPanel virtualOCSSWParamPanel = new JPanel();
        virtualOCSSWParamPanel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        JLabel ocsswRootLabel = new JLabel("OCSSW Shared Dir:");
        JTextField ocsswRoot = new JTextField();


        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.gridy = 0;

        virtualOCSSWParamPanel.add(ocsswRootLabel, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.5;
        c.gridx = 2;
        c.gridy = 0;

        virtualOCSSWParamPanel.add(ocsswRoot, c);

        return virtualOCSSWParamPanel;
    }

    private JPanel getRemoteOCSSWParamPanel() {

        JPanel remoteOCSSWParamPanel = new JPanel();
        remoteOCSSWParamPanel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        JLabel ocsswServerAddressLabel = new JLabel("OCSSW Server Address:");
        JTextField ocsswserverAddress = new JTextField(10); // accepts upto 10 characters


        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.gridy = 0;

        remoteOCSSWParamPanel.add(ocsswServerAddressLabel, c);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.5;
        c.gridx = 2;
        c.gridy = 0;       //third row
        remoteOCSSWParamPanel.add(ocsswserverAddress, c);

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
                ocsswRoot.setText(getDir().getAbsolutePath());
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
