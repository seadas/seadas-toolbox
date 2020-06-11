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

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
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
 * @author Bing Yang
 *
 */
// May 2020

public class GetSysInfoGUI {

    final String PANEL_NAME = "Seadas Configuration";
    final String HELP_ID = "getSysInfoConfig";

    JPanel paramPanel = GridBagUtils.createPanel();

    ModalDialog modalDialog;

    JTextField ocsswBranchTextfield;
    JComboBox ocsswLocationComboBox;
    JTextField ocsswSharedDir;
    JTextField ocsswRootTextfield;
    JTextField ocsswLogDirTextfield;
    JTextField ocsswserverAddressTextfield;
    JTextField serverPortTextfield;
    JTextField serverInputStreamPortTextfield;
    JTextField serverErrorStreamPortTextfield;


    PropertyContainer pc = new PropertyContainer();

    boolean windowsOS;

    String[] ocsswLocations;

    public static void main(String args[]) {

        final AppContext appContext = SnapApp.getDefault().getAppContext();
        final Window parent = appContext.getApplicationWindow();
        GetSysInfoGUI getSysInfoGUI = new GetSysInfoGUI();
        getSysInfoGUI.init(parent);
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
        mainPanel.add(appDirPanel(), gbc);

        gbc.gridy += 1;
        gbc.weighty = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JPanel sysInfoPanel = sysInfoPanel();
        mainPanel.add(sysInfoPanel, gbc);

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
            finish = true;
        }

        return;

    }

    private JPanel sysInfoPanel(){

        JLabel sysInfoLabel = new JLabel("SysInfo :");
        sysInfoLabel.setMinimumSize(sysInfoLabel.getPreferredSize());

        JTextArea sysInfoTextarea = new JTextArea();

        JPanel panel = GridBagUtils.createPanel();
        GridBagConstraints gbc = createConstraints();
        String appDir = Config.instance().installDir().toString();
        OCSSWInfo ocsswInfo = OCSSWInfo.getInstance();
        String ocsswRootOcsswInfo = ocsswInfo.getOcsswRoot();
        String command = ocsswRootOcsswInfo + "/scripts/ocssw_runner --ocsswroot " + ocsswRootOcsswInfo
                + " " + ocsswRootOcsswInfo + "/scripts/seadas_info2.py --AppDir " + appDir;
        System.out.println("command is: " + command);

        try {
            Process process = Runtime.getRuntime().exec(command);

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            String line;
            String sysText = "";
            while ((line = reader.readLine()) != null) {
                sysText = sysText + line;
                sysText = sysText + "\n";
            }

            sysInfoTextarea.setText(sysText);
            reader.close();

            process.destroy();

            if (process.exitValue() != 0) {
                System.out.println("Abnormal process termination");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(sysInfoLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(sysInfoTextarea, gbc);

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

    private JPanel appDirPanel() {

        final Preferences preferences = Config.instance("seadas").load().preferences();
        String lastOcsswLocation = preferences.get(SEADAS_OCSSW_LOCATION_PROPERTY, SEADAS_OCSSW_LOCATION_DEFAULT_VALUE);

        String installDir = Config.instance().installDir().toString();

        OCSSWInfo ocsswInfo = OCSSWInfo.getInstance();
        String ocsswRootOcsswInfo = ocsswInfo.getOcsswRoot();
        String ocsswLogDir = ocsswInfo.getLogDirPath();

        JPanel panel = GridBagUtils.createPanel();

        GridBagConstraints gbc = createConstraints();

        JLabel seadasInfoLabel = new JLabel("SeaDAS Info" + ": ");
        JTextArea seadasInfoTextarea = new JTextArea();

        seadasInfoLabel.setMinimumSize(seadasInfoLabel.getPreferredSize());

        String seadasInfoText = "";

//        System.out.println("appDir = " + installDir);
        seadasInfoText = seadasInfoText + "Application Directoy : " + installDir + "\n";

//        System.out.println("ocsswRootOcsswInfo = " + ocsswRootOcsswInfo);
        seadasInfoText = seadasInfoText + "ocsswRootOcsswInfo : " + ocsswRootOcsswInfo + "\n";

//        System.out.println("OCSSW Log Directory = " + ocsswLogDir);
        seadasInfoText = seadasInfoText + "OCSSW Log Directory : " + ocsswLogDir + "\n";

//        System.out.println("OCSSW Location = " + lastOcsswLocation);
        seadasInfoText = seadasInfoText + "OCSSW Location : " + lastOcsswLocation + "\n";

        seadasInfoTextarea.setText(seadasInfoText);

        gbc.fill = GridBagConstraints.NONE;

        gbc.weighty = 0;
        gbc.weightx = 0;
        panel.add(seadasInfoLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(seadasInfoTextarea, gbc);

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
}
