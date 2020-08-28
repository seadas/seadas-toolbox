package gov.nasa.gsfc.seadas.processing.ocssw;


import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.swing.binding.BindingContext;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.about.SnapAboutBox;
import org.esa.snap.runtime.Config;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.GridBagUtils;
import org.esa.snap.ui.ModalDialog;
import org.esa.snap.core.datamodel.Product;
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
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Paths;
import java.util.*;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.openide.modules.ModuleInfo;
import org.openide.modules.Modules;

import static gov.nasa.gsfc.seadas.processing.ocssw.OCSSW.*;
import static gov.nasa.gsfc.seadas.processing.ocssw.OCSSWConfigData.*;
import static gov.nasa.gsfc.seadas.processing.ocssw.OCSSWInfo.*;


/**
 * @author Aynur Abdurazik
 * @author Bing Yang
 *
 */
// May 2020 - Yang - print out relevant system info for SeaDAS or OCSSW troubleshooting

public class GetSysInfoGUI {

    final String PANEL_NAME = "Seadas/System Information";
    final String HELP_ID = "getSysInfo";
    String sysInfoText;
    String sysInfoText2;

    ModalDialog modalDialog;

//    PropertyContainer pc = new PropertyContainer();

    boolean windowsOS;
    private String ocsswScriptsDirPath;
    private String ocsswSeadasInfoPath;
    private String ocsswRunnerScriptPath;
    private String ocsswBinDirPath;
    private String ocsswRootEnv = System.getenv(SEADAS_OCSSW_ROOT_ENV);



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

        modalDialog = new ModalDialog(parent, PANEL_NAME, mainPanel, ModalDialog.ID_OK_CANCEL_HELP, HELP_ID){
            @Override

            protected void onOK(){
                SystemUtils.copyToClipboard(sysInfoText);
            }
        };

        modalDialog.getButton(ModalDialog.ID_OK).setText("Copy To Clipboard");
        modalDialog.getButton(ModalDialog.ID_CANCEL).setText("Close");
        modalDialog.getButton(ModalDialog.ID_HELP).setText("Help");

        GridBagConstraints gbc = createConstraints();

        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;
        gbc.weightx = 1;
        JPanel sysInfoPanel = sysInfoPanel();
        mainPanel.add(sysInfoPanel, gbc);

        // Add filler panel at bottom which expands as needed to force all components within this panel to the top
        gbc.gridy += 1;
        gbc.weighty = 0;
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
            if (dialogResult == ModalDialog.ID_CANCEL) {
                finish = true;
            } else {
                finish = true;
            }

//            finish = true;
        }

        return;

    }

    protected JPanel sysInfoPanel(){

        JPanel panel = GridBagUtils.createPanel();
        GridBagConstraints gbc = createConstraints();

//        JTextArea sysInfoTextarea = new JTextArea(45,66);
//        sysInfoTextarea.setLineWrap(true);
//        sysInfoTextarea.setEditable(false);

        JTextPane sysInfoTextpane = new JTextPane();

//        JScrollPane scroll = new JScrollPane(sysInfoTextarea);
        JScrollPane scroll = new JScrollPane(sysInfoTextpane);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

//        panel.add(scroll, gbc);


//        final Preferences preferences = Config.instance("seadas").load().preferences();
//        String lastOcsswLocation = preferences.get(SEADAS_OCSSW_LOCATION_PROPERTY, SEADAS_OCSSW_LOCATION_DEFAULT_VALUE);

        SnapApp snapapp = SnapApp.getDefault();
        String appNameVersion = snapapp.getInstanceName();
        String appName = SystemUtils.getApplicationName();
        String appReleaseVersionFromPOM = SystemUtils.getReleaseVersion();
        File appHomeDir = SystemUtils.getApplicationHomeDir();
        File appDataDir = SystemUtils.getApplicationDataDir();

        Path appBinDir = appHomeDir.toPath().resolve("bin");
        Path appEtcDir = appHomeDir.toPath().resolve("etc");
        Path appConfig = appEtcDir.resolve("snap.properties");

        Path dataDirPath = appDataDir.toPath();
        Path dataEtcPath = dataDirPath.resolve("etc");
        Path snapProperties = dataEtcPath.resolve("snap.properties");

        Path vmOptions = appBinDir.resolve("pconvert.vmoptions");
        Path vmOptionsGpt = appBinDir.resolve("gpt.vmoptions");

        String jre = System.getProperty("java.runtime.name") + " " + System.getProperty("java.runtime.version");
        String jvm = System.getProperty("java.vm.name") + " by " + System.getProperty("java.vendor");
        String memory = Math.round(Runtime.getRuntime().maxMemory() /1024. /1024.) + " MiB";

        ModuleInfo seadasProcessingModuleInfo = Modules.getDefault().ownerOf(OCSSWInfoGUI.class);
        ModuleInfo desktopModuleInfo = Modules.getDefault().ownerOf(SnapAboutBox.class);
        ModuleInfo engineModuleInfo = Modules.getDefault().ownerOf(Product.class);

        Path seadasProperties = dataEtcPath.resolve("seadas.properties");

//        System.out.println("\nMain Application Platform:");
//        System.out.println("Application Name Version: " + appNameVersion);
//        System.out.println("Application Home Directory: " + appHomeDir.toString());
//        System.out.println("Application Data Directory: " + appDataDir.toString());
//        System.out.println("Application Configuration: " + appConfig.toString());
//        System.out.println("Virtual Memory Configuration: " + vmOptions.toString());
//        System.out.println("Virtual Memory Configuration (gpt): " + vmOptionsGpt.toString());
//        System.out.println("Desktop Specification Version: " + desktopModuleInfo.getSpecificationVersion());
//        System.out.println("Desktop Implementation Version: " + desktopModuleInfo.getImplementationVersion());
//        System.out.println("Engine Specification Version: " + engineModuleInfo.getSpecificationVersion());
//        System.out.println("Engine Implementation Version: " + engineModuleInfo.getImplementationVersion());
//        System.out.println("JRE: " + jre);
//        System.out.println("JVM: " + jvm);
//        System.out.println("Memory: " + memory);
//
//        System.out.println("SeaDAS Toolbox Specification Version: " + seadasProcessingModuleInfo.getSpecificationVersion());
//        System.out.println("SeaDAS Toolbox Implementation Version: " + seadasProcessingModuleInfo.getImplementationVersion());
//

//        String test =Config.instance().preferences().get("seadas.version", null);
//        System.out.println("seadas.version=" + test);

        OCSSWInfo ocsswInfo = OCSSWInfo.getInstance();
        String ocsswRootOcsswInfo = ocsswInfo.getOcsswRoot();
        String ocsswLogDir = ocsswInfo.getLogDirPath();
        String ocsswLocation = ocsswInfo.getOcsswLocation();
        String ocsswDebugInfo = ocsswInfo.getOcsswDebugInfo();
        Boolean ocsswDebug;
        if (ocsswDebugInfo.equals("true")){
            ocsswDebug = true;
        } else{
            ocsswDebug = false;
        }

        //        System.out.println("appDir = " + installDir);
        //        System.out.println("ocsswRootOcsswInfo = " + ocsswRootOcsswInfo);
        //        System.out.println("OCSSW Log Directory = " + ocsswLogDir);
        //        System.out.println("OCSSW Location = " + lastOcsswLocation);

        sysInfoText = "Main Application Platform: " + "\n";

        sysInfoText += "Application Version: " + appNameVersion + "\n";
        sysInfoText += "Installation Directory: " + appHomeDir.toString() + "\n";

        appendToPane(sysInfoTextpane, sysInfoText, Color.BLACK);

        if (!appHomeDir.isDirectory()) {
            sysInfoText += "WARNING!! Directory '" + appHomeDir.toString() + "' does not exist" + "\n";
            sysInfoText2 = "WARNING!! Directory '" + appHomeDir.toString() + "' does not exist" + "\n";
            appendToPane(sysInfoTextpane, sysInfoText2, Color.RED);
        }
        sysInfoText += "Data Directory: " + appDataDir.toString() + "\n";
        sysInfoText2 = "Data Directory: " + appDataDir.toString() + "\n";
        appendToPane(sysInfoTextpane, sysInfoText2, Color.BLACK);

        if (!appDataDir.isDirectory()) {
            sysInfoText += "WARNING!! Directory '" + appDataDir.toString() + "' does not exist" + "\n";
            sysInfoText2 = "WARNING!! Directory '" + appDataDir.toString() + "' does not exist" + "\n";
            appendToPane(sysInfoTextpane, sysInfoText2, Color.RED);
        }
        sysInfoText += "Configuration: " + appConfig.toString() + "\n";
        sysInfoText2 = "Configuration: " + appConfig.toString() + "\n";
        appendToPane(sysInfoTextpane, sysInfoText2, Color.BLACK);
        if (!Files.exists(appConfig)) {
            sysInfoText += "WARNING!! File '" + appConfig.toString() + "' does not exist" + "\n";
            sysInfoText2 = "WARNING!! File '" + appConfig.toString() + "' does not exist" + "\n";
            appendToPane(sysInfoTextpane, sysInfoText2, Color.BLACK);
        }
        if (!Files.exists(snapProperties)) {
            sysInfoText += "Configuration2: null" + "\n";
            sysInfoText2 = "Configuration2: null" + "\n";
            appendToPane(sysInfoTextpane, sysInfoText2, Color.BLACK);
        } else {
            sysInfoText += "Configuration2: " + snapProperties.toString() + "\n";
            sysInfoText2 = "Configuration2: " + snapProperties.toString() + "\n";
            appendToPane(sysInfoTextpane, sysInfoText2, Color.BLACK);
        }
        sysInfoText += "VM Configuration: " + vmOptions.toString() + "\n";
        sysInfoText2 = "VM Configuration: " + vmOptions.toString() + "\n";
        appendToPane(sysInfoTextpane, sysInfoText2, Color.BLACK);
        if (!Files.exists(vmOptions)) {
            sysInfoText += "WARNING!! File '" + vmOptions.toString() + "' does not exist" + "\n";
            sysInfoText2 = "WARNING!! File '" + vmOptions.toString() + "' does not exist" + "\n";
            appendToPane(sysInfoTextpane, sysInfoText2, Color.RED);
        }
        sysInfoText += "VM Configuration (gpt): " + vmOptionsGpt.toString() + "\n";
        if (!Files.exists(vmOptionsGpt)) {
            sysInfoText += "WARNING!! File '" + vmOptionsGpt.toString() + "' does not exist" + "\n";
            sysInfoText2 = "WARNING!! File '" + vmOptionsGpt.toString() + "' does not exist" + "\n";
            appendToPane(sysInfoTextpane, sysInfoText2, Color.RED);
        }
        sysInfoText += "Desktop Specification Version: " + desktopModuleInfo.getSpecificationVersion() + "\n";
        sysInfoText2 = "Desktop Specification Version: " + desktopModuleInfo.getSpecificationVersion() + "\n";
        if (ocsswDebug) {
            sysInfoText += "Desktop Implementation Version: " + desktopModuleInfo.getImplementationVersion() + "\n";
            sysInfoText2 += "Desktop Implementation Version: " + desktopModuleInfo.getImplementationVersion() + "\n";
        }
        sysInfoText += "Engine Specification Version: " + engineModuleInfo.getSpecificationVersion() + "\n";
        sysInfoText2 += "Engine Specification Version: " + engineModuleInfo.getSpecificationVersion() + "\n";
        if (ocsswDebug) {
            sysInfoText += "Engine Implementation Version: " + engineModuleInfo.getImplementationVersion() + "\n";
            sysInfoText2 += "Engine Implementation Version: " + engineModuleInfo.getImplementationVersion() + "\n";
        }
        sysInfoText += "JRE: " + jre + "\n";
        sysInfoText2 += "JRE: " + jre + "\n";
        sysInfoText += "JVM: " + jvm + "\n";
        sysInfoText2 += "JVM: " + jvm + "\n";
        sysInfoText += "Memory: " + memory + "\n\n";
        sysInfoText2 += "Memory: " + memory + "\n\n";

        sysInfoText += "SeaDAS Toolbox: " + "\n";
        sysInfoText2 += "SeaDAS Toolbox: " + "\n";

        sysInfoText += "SeaDAS Toolbox Specification Version: " + seadasProcessingModuleInfo.getSpecificationVersion() + "\n";
        sysInfoText2 += "SeaDAS Toolbox Specification Version: " + seadasProcessingModuleInfo.getSpecificationVersion() + "\n";
        if (ocsswDebug) {
            sysInfoText += "SeaDAS Toolbox Implementation Version: " + seadasProcessingModuleInfo.getImplementationVersion() + "\n";
            sysInfoText2 += "SeaDAS Toolbox Implementation Version: " + seadasProcessingModuleInfo.getImplementationVersion() + "\n";
        }
        if (!Files.exists(seadasProperties)) {
            sysInfoText += "Configuration: null" + "\n";
            sysInfoText2 += "Configuration: null" + "\n";
        } else {
            sysInfoText += "Configuration: " + seadasProperties.toString() + "\n";
            sysInfoText2 += "Configuration: " + seadasProperties.toString() + "\n";
        }
        sysInfoText += "OCSSW Root Directory: " + ocsswRootOcsswInfo + "\n";
        sysInfoText2 += "OCSSW Root Directory: " + ocsswRootOcsswInfo + "\n";
        appendToPane(sysInfoTextpane, sysInfoText2, Color.BLACK);

        if ((ocsswRootOcsswInfo != null ) && !Files.exists(Paths.get(ocsswRootOcsswInfo))) {
            sysInfoText += "WARNING!! Directory '" + ocsswRootOcsswInfo + "' does not exist" + "\n";
            sysInfoText2 = "WARNING!! Directory '" + ocsswRootOcsswInfo + "' does not exist" + "\n";
            appendToPane(sysInfoTextpane, sysInfoText2, Color.RED);
        }
        sysInfoText += "OCSSW Log Directory: " + ocsswLogDir + "\n";
        sysInfoText2 = "OCSSW Log Directory: " + ocsswLogDir + "\n";
        appendToPane(sysInfoTextpane, sysInfoText2, Color.BLACK);
        if ((ocsswLogDir != null ) && !Files.exists(Paths.get(ocsswLogDir))) {
            sysInfoText += "WARNING!! Directory '" + ocsswLogDir + "' does not exist" + "\n";
            sysInfoText2 = "WARNING!! Directory '" + ocsswLogDir + "' does not exist" + "\n";
            appendToPane(sysInfoTextpane, sysInfoText2, Color.RED);
        }
        sysInfoText += "OCSSW Location: " + ocsswLocation + "\n";
        sysInfoText2 = "OCSSW Location: " + ocsswLocation + "\n";

        sysInfoText += "Environment {$OCSSWROOT} (external): " + ocsswRootEnv + "\n";
        sysInfoText2 += "Environment {$OCSSWROOT} (external): " + ocsswRootEnv + "\n";
        appendToPane(sysInfoTextpane, sysInfoText2, Color.BLACK);
        if ((ocsswRootOcsswInfo != null ) && !ocsswRootOcsswInfo.equals(ocsswRootEnv)){
            sysInfoText +=  "WARNING!: An environment variable for OCSSWROOT exists which does not match the GUI configuration. " +
                    "The GUI will use '" + ocsswRootOcsswInfo + "' as the ocssw root inside the GUI." + "\n";
            sysInfoText2 =  "WARNING!: An environment variable for OCSSWROOT exists which does not match the GUI configuration. " +
                    "The GUI will use '" + ocsswRootOcsswInfo + "' as the ocssw root inside the GUI." + "\n";
            appendToPane(sysInfoTextpane, sysInfoText2, Color.RED);
        }

        sysInfoText += "\n-----------------------------------------------\n\n";
        sysInfoText2 = "\n-----------------------------------------------\n\n";
        appendToPane(sysInfoTextpane, sysInfoText2, Color.BLACK);

//        String appDir = Config.instance().installDir().toString();

        ocsswScriptsDirPath = ocsswRootOcsswInfo + File.separator + OCSSW_SCRIPTS_DIR_SUFFIX;
        ocsswRunnerScriptPath = ocsswScriptsDirPath + System.getProperty("file.separator") + OCSSW_RUNNER_SCRIPT;
        ocsswBinDirPath = ocsswRootOcsswInfo + System.getProperty("file.separator") + OCSSW_BIN_DIR_SUFFIX;

        String command = ocsswRunnerScriptPath + " --ocsswroot " + ocsswRootOcsswInfo + " " + OCSSW_SEADAS_INFO_PROGRAM_NAME ;

        ocsswSeadasInfoPath = ocsswBinDirPath + System.getProperty("file.separator") + OCSSW_SEADAS_INFO_PROGRAM_NAME;

        if ((ocsswRootOcsswInfo == null ) || !Files.exists(Paths.get(ocsswRootOcsswInfo))) {
            if ((ocsswRootEnv != null) & Files.exists(Paths.get(ocsswRootEnv))) {
                sysInfoText += "NASA Science Processing (OCSSW): " + "\n";
                sysInfoText2 = "NASA Science Processing (OCSSW): " + "\n";
                appendToPane(sysInfoTextpane, sysInfoText2, Color.BLACK);

                sysInfoText += "WARNING! Processing not configured in the GUI but an installation currently exists in the directory '" + ocsswRootEnv +
                       "'. To configure the GUI to use this installation then update the 'local directory'  in Menu > SeaDAS-OCSSW > OCSSW Configuration";
                sysInfoText2 = "WARNING! Processing not configured in the GUI but an installation currently exists in the directory '" + ocsswRootEnv +
                        "'. To configure the GUI to use this installation then update the 'local directory'  in Menu > SeaDAS-OCSSW > OCSSW Configuration";
                appendToPane(sysInfoTextpane, sysInfoText2, Color.RED);

//               sysInfoTextarea.setText(sysInfoText);
            } else {
                sysInfoText += "NASA Science Processing (OCSSW): " + "\n";
                sysInfoText2 = "NASA Science Processing (OCSSW): " + "\n";
                appendToPane(sysInfoTextpane, sysInfoText2, Color.BLACK);

                sysInfoText += "Warning! Processers not installed " + "\n\n";
                sysInfoText2 = "Warning! Processers not installed " + "\n\n";
                appendToPane(sysInfoTextpane, sysInfoText2, Color.RED);

                printGeneralSystemInfo(ocsswDebug);
                appendToPane(sysInfoTextpane, sysInfoText2, Color.BLACK);
//                sysInfoTextarea.setText(sysInfoText);
            }
        } else {

            if (!Files.isExecutable(Paths.get(ocsswSeadasInfoPath))) {
                sysInfoText += "NASA Science Processing (OCSSW): " + "\n";
                sysInfoText2 = "NASA Science Processing (OCSSW): " + "\n";
                appendToPane(sysInfoTextpane, sysInfoText2, Color.BLACK);

                sysInfoText += "WARNING! Cannot find 'seadas_info' in the OCSSW bin directory" + "\n\n";
                sysInfoText2 = "WARNING! Cannot find 'seadas_info' in the OCSSW bin directory" + "\n\n";
                appendToPane(sysInfoTextpane, sysInfoText2, Color.RED);

                printGeneralSystemInfo(ocsswDebug);
                appendToPane(sysInfoTextpane, sysInfoText2, Color.BLACK);

//                sysInfoTextarea.setText(sysInfoText);
            } else {
//            System.out.println("command is: " + command);
                sysInfoText2 = "";
                try {
                    Process process = Runtime.getRuntime().exec(command);

                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(process.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sysInfoText += line + "\n";
                        sysInfoText2 += line + "\n";
                    }
//                    appendToPane(sysInfoTextpane, sysInfoText2, Color.BLACK);

//                    sysInfoTextarea.setText(sysInfoText);
                    reader.close();

                    process.destroy();

                    if (process.exitValue() != 0) {
                        System.out.println("WARNING!: Non zero exit code returned for \'" + command + "\' ");
                    }

                } catch (IOException e) {
                    String warning = "WARNING!! Could not retrieve system parameters because command \'" + command + "\' failed";
                    sysInfoText += warning + "\n";
                    sysInfoText2 += warning + "\n";
                    sysInfoText += e.toString() + "\n";
                    sysInfoText2 += e.toString() + "\n";
                    e.printStackTrace();
                }
                appendToPane(sysInfoTextpane, sysInfoText2, Color.BLACK);
            }
        }
        sysInfoTextpane.setEditable(false);

        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        sysInfoTextpane.setPreferredSize(new Dimension(750,750));

        panel.add(scroll, gbc);

        return panel;
    }

    private void printGeneralSystemInfo(Boolean ocsswDebug) {
        sysInfoText += "General System and Software: " + "\n";
        sysInfoText2 = "General System and Software: " + "\n";
        sysInfoText += "Operating System: " + System.getProperty("os.name");
        sysInfoText += " " + System.getProperty("os.version") + "\n";
        sysInfoText2 += "Operating System: " + System.getProperty("os.name");
        sysInfoText2 += " " + System.getProperty("os.version") + "\n";
        sysInfoText += "Java Version: " + System.getProperty("java.version") + "\n";
        sysInfoText2 += "Java Version: " + System.getProperty("java.version") + "\n";

        if (ocsswDebug) {
            try {
                Process process = Runtime.getRuntime().exec("python --version");

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    sysInfoText += line + "\n";
                    sysInfoText2 += line + "\n";
                }

                reader.close();

                process.destroy();

                if (process.exitValue() != 0) {
                    System.out.println("WARNING!: Non zero exit code returned for 'python --version' ");
                }

            } catch (IOException e) {
                String warning = "WARNING!! Could not retrieve system parameters because 'pyhton --version' failed";
                sysInfoText += warning + "\n";
                sysInfoText2 += warning + "\n";
                sysInfoText += e.toString() + "\n";
                sysInfoText2 += e.toString() + "\n";
                e.printStackTrace();
            }
        }
    }

    private void appendToPane(JTextPane tp, String msg, Color c)
    {
        StyleContext sc = StyleContext.getDefaultStyleContext();
        AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, c);

        aset = sc.addAttribute(aset, StyleConstants.FontFamily, "Lucida Console");
        aset = sc.addAttribute(aset, StyleConstants.Alignment, StyleConstants.ALIGN_JUSTIFIED);

        int len = tp.getDocument().getLength();
        tp.setCaretPosition(len);
        tp.setCharacterAttributes(aset, false);
        tp.replaceSelection(msg);
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

    private Dimension adjustDimension(Dimension dimension, int widthAdjustment, int heightAdjustment) {

        if (dimension == null) {
            return null;
        }

        int width = dimension.width + widthAdjustment;
        int height = dimension.height + heightAdjustment;

        return new Dimension(width, height);
    }
}
