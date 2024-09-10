package gov.nasa.gsfc.seadas.processing.ocssw;


import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.about.SnapAboutBox;
import org.esa.snap.runtime.Config;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.GridBagUtils;
import org.esa.snap.ui.ModalDialog;
import org.esa.snap.core.datamodel.Product;
//import org.jsoup.Connection;
//import org.jsoup.Jsoup;
//import org.jsoup.nodes.Document;
//import org.jsoup.nodes.Element;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.prefs.Preferences;

import org.openide.modules.ModuleInfo;
import org.openide.modules.Modules;

import static gov.nasa.gsfc.seadas.processing.ocssw.OCSSWConfigData.*;
import static gov.nasa.gsfc.seadas.processing.ocssw.OCSSWInfo.*;


/**
 * @author Aynur Abdurazik
 * @author Bing Yang
 */
// May 2020 - Yang - print out relevant system info for SeaDAS or OCSSW troubleshooting

public class GetSysInfoGUI {

    final String PANEL_NAME = "Seadas/System Information";
    final String HELP_ID = "getSysInfo";
    String sysInfoText;
    String currentInfoLine;

    ModalDialog modalDialog;

//    PropertyContainer pc = new PropertyContainer();

    boolean windowsOS;
    private String ocsswScriptsDirPath;
    private String ocsswSeadasInfoPath;
    private String ocsswRunnerScriptPath;
    private String ocsswBinDirPath;
    private String ocsswRootEnv = System.getenv(SEADAS_OCSSW_ROOT_ENV);
    String ocsswRootDocker = SystemUtils.getUserHomeDir().toString() + File.separator + "ocssw";


    private String DASHES = "-----------------------------------------------------------";
    private String INDENT = "  ";


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
            ocsswRootEnv = ocsswRootDocker;
        } else {
            windowsOS = false;
        }


        JPanel mainPanel = GridBagUtils.createPanel();

        modalDialog = new ModalDialog(parent, PANEL_NAME, mainPanel, ModalDialog.ID_OK_CANCEL_HELP, HELP_ID) {
            @Override

            protected void onOK() {
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

    protected JPanel sysInfoPanel() {

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
        String appNameVersion = snapapp.getInstanceName() + " " + SystemUtils.getReleaseVersion();
        String appName = SystemUtils.getApplicationName();
        String appReleaseVersionFromPOM = SystemUtils.getReleaseVersion();

        File appHomeDir = SystemUtils.getApplicationHomeDir();
        File appDataDir = SystemUtils.getApplicationDataDir();

        Path appBinDir = appHomeDir.toPath().resolve("bin");
        Path appEtcDir = appHomeDir.toPath().resolve("etc");
        Path appHomeSnapProperties = appEtcDir.resolve("snap.properties");
        ;
        Path appHomeSnapConf = appEtcDir.resolve("snap.conf");
        Path appHomeSeadasConf = appEtcDir.resolve("seadas.conf");

        boolean isSeadasPlatform = (appNameVersion != null && appNameVersion.toLowerCase().contains("seadas")) ? true : false;


        Path dataDirPath = appDataDir.toPath();
        Path dataEtcPath = dataDirPath.resolve("etc");
        Path runtimeSnapProperties = dataEtcPath.resolve("snap.properties");
        Path runtimeSeadasProperties = dataEtcPath.resolve("seadas.properties");

        Path vmOptions = appBinDir.resolve("pconvert.vmoptions");
        Path vmOptionsGpt = appBinDir.resolve("gpt.vmoptions");

        String jre = System.getProperty("java.runtime.name") + " " + System.getProperty("java.runtime.version");
        String jvm = System.getProperty("java.vm.name") + " by " + System.getProperty("java.vendor");
        String memory = Math.round(Runtime.getRuntime().maxMemory() / 1024. / 1024.) + " MiB";

        ModuleInfo seadasProcessingModuleInfo = Modules.getDefault().ownerOf(OCSSWInfoGUI.class);
        ModuleInfo desktopModuleInfo = Modules.getDefault().ownerOf(SnapAboutBox.class);
        ModuleInfo engineModuleInfo = Modules.getDefault().ownerOf(Product.class);


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
        if (ocsswDebugInfo.equals("true")) {
            ocsswDebug = true;
        } else {
            ocsswDebug = false;
        }

        //        System.out.println("appDir = " + installDir);
        //        System.out.println("ocsswRootOcsswInfo = " + ocsswRootOcsswInfo);
        //        System.out.println("OCSSW Log Directory = " + ocsswLogDir);
        //        System.out.println("OCSSW Location = " + lastOcsswLocation);


        sysInfoText = "";


        currentInfoLine = DASHES + "\n";
        sysInfoText += currentInfoLine;
        appendToPane(sysInfoTextpane, currentInfoLine, Color.BLACK);
        currentInfoLine = INDENT + "Main Application Platform: " + "\n";
        sysInfoText += currentInfoLine;
        appendToPane(sysInfoTextpane, currentInfoLine, Color.BLACK);
        currentInfoLine = DASHES + "\n";
        sysInfoText += currentInfoLine;
        appendToPane(sysInfoTextpane, currentInfoLine, Color.BLACK);


        currentInfoLine = "Application Version: " + appNameVersion + "\n";
        sysInfoText += currentInfoLine;
        appendToPane(sysInfoTextpane, currentInfoLine, Color.BLACK);


        String brandingSuffix = "";
        if (isSeadasPlatform) {
            brandingSuffix = "* (SeaDAS Platform modified)";
        }


        currentInfoLine = "SNAP Engine Version: " + engineModuleInfo.getSpecificationVersion() + brandingSuffix + "\n";
        sysInfoText += currentInfoLine;
        appendToPane(sysInfoTextpane, currentInfoLine, Color.BLACK);

        if (ocsswDebug) {
            currentInfoLine = "Snap Engine Implementation Version: " + engineModuleInfo.getImplementationVersion() + "\n";
            sysInfoText += currentInfoLine;
            appendToPane(sysInfoTextpane, currentInfoLine, Color.BLACK);
        }


        currentInfoLine = "SNAP Desktop Version: " + desktopModuleInfo.getSpecificationVersion() + brandingSuffix + "\n";
        sysInfoText += currentInfoLine;
        appendToPane(sysInfoTextpane, currentInfoLine, Color.BLACK);

        if (ocsswDebug) {
            currentInfoLine = "SNAP Desktop Implementation Version: " + desktopModuleInfo.getImplementationVersion() + "\n";
            sysInfoText += currentInfoLine;
            appendToPane(sysInfoTextpane, currentInfoLine, Color.BLACK);
        }


        currentInfoLine = "SNAP Engine Build Date: " + engineModuleInfo.getBuildVersion() + "\n";
        sysInfoText += currentInfoLine;
        appendToPane(sysInfoTextpane, currentInfoLine, Color.BLACK);

        currentInfoLine = "SNAP Desktop Build Date: " + desktopModuleInfo.getBuildVersion() + "\n";
        sysInfoText += currentInfoLine;
        appendToPane(sysInfoTextpane, currentInfoLine, Color.BLACK);


        currentInfoLine = "Installation Directory: " + appHomeDir.toString() + "\n";
        sysInfoText += currentInfoLine;
        appendToPane(sysInfoTextpane, currentInfoLine, Color.BLACK);
        if (!appHomeDir.isDirectory()) {
            currentInfoLine = "    WARNING!! Directory '" + appHomeDir.toString() + "' does not exist" + "\n";
            sysInfoText += currentInfoLine;
            appendToPane(sysInfoTextpane, currentInfoLine, Color.RED);
        }


        currentInfoLine = "Data Directory: " + appDataDir.toString() + "\n";
        sysInfoText += currentInfoLine;
        appendToPane(sysInfoTextpane, currentInfoLine, Color.BLACK);
        if (!appDataDir.isDirectory()) {
            currentInfoLine = "    WARNING!! Directory '" + appDataDir.toString() + "' does not exist" + "\n";
            sysInfoText += currentInfoLine;
            appendToPane(sysInfoTextpane, currentInfoLine, Color.RED);
        }


        currentInfoLine = "Configuration: " + appHomeSnapProperties.toString() + "\n";
        sysInfoText += currentInfoLine;
        appendToPane(sysInfoTextpane, currentInfoLine, Color.BLACK);
        if (!Files.exists(appHomeSnapProperties)) {
            currentInfoLine = "    WARNING!! File '" + appHomeSnapProperties.toString() + "' does not exist" + "\n";
            sysInfoText += currentInfoLine;
            appendToPane(sysInfoTextpane, currentInfoLine, Color.RED);
        }


        if (isSeadasPlatform) {
            currentInfoLine = "VM Configuration: " + appHomeSeadasConf.toString() + "\n";
            sysInfoText += currentInfoLine;
            appendToPane(sysInfoTextpane, currentInfoLine, Color.BLACK);
            if (!Files.exists(appHomeSeadasConf)) {
                currentInfoLine = "    WARNING!! File '" + appHomeSeadasConf.toString() + "' does not exist" + "\n";
                sysInfoText += currentInfoLine;
                appendToPane(sysInfoTextpane, currentInfoLine, Color.RED);
            }

            currentInfoLine = "VM Configuration: " + appHomeSnapConf.toString() + "\n";
            sysInfoText += currentInfoLine;
            appendToPane(sysInfoTextpane, currentInfoLine, Color.BLACK);
            if (!Files.exists(appHomeSnapConf)) {
                currentInfoLine = "    WARNING!! File '" + appHomeSnapConf.toString() + "' does not exist" + "\n";
                sysInfoText += currentInfoLine;
                appendToPane(sysInfoTextpane, currentInfoLine, Color.RED);
            }
        } else {
            currentInfoLine = "VM Configuration: " + appHomeSnapConf.toString() + "\n";
            sysInfoText += currentInfoLine;
            appendToPane(sysInfoTextpane, currentInfoLine, Color.BLACK);
            if (!Files.exists(appHomeSnapConf)) {
                currentInfoLine = "    WARNING!! File '" + appHomeSnapConf.toString() + "' does not exist" + "\n";
                sysInfoText += currentInfoLine;
                appendToPane(sysInfoTextpane, currentInfoLine, Color.RED);
            }
        }


        currentInfoLine = "VM Configuration (gpt): " + vmOptionsGpt.toString() + "\n";
        sysInfoText += currentInfoLine;
        appendToPane(sysInfoTextpane, currentInfoLine, Color.BLACK);
        if (!Files.exists(vmOptionsGpt)) {
            currentInfoLine = "    WARNING!! File '" + vmOptionsGpt.toString() + "' does not exist" + "\n";
            sysInfoText += currentInfoLine;
            appendToPane(sysInfoTextpane, currentInfoLine, Color.RED);
        }


        currentInfoLine = "VM Configuration (pconvert): " + vmOptions.toString() + "\n";
        sysInfoText += currentInfoLine;
        appendToPane(sysInfoTextpane, currentInfoLine, Color.BLACK);
        if (!Files.exists(vmOptions)) {
            currentInfoLine = "    WARNING!! File '" + vmOptions.toString() + "' does not exist" + "\n";
            sysInfoText += currentInfoLine;
            appendToPane(sysInfoTextpane, currentInfoLine, Color.RED);
        }


        if (!Files.exists(runtimeSnapProperties)) {
            currentInfoLine = "Runtime Configuration: null" + "\n";
            sysInfoText += currentInfoLine;
            appendToPane(sysInfoTextpane, currentInfoLine, Color.BLACK);
        } else {
            currentInfoLine = "Runtime Configuration: " + runtimeSnapProperties.toString() + "\n";
            sysInfoText += currentInfoLine;
            appendToPane(sysInfoTextpane, currentInfoLine, Color.BLACK);
        }


        if (!Files.exists(runtimeSeadasProperties)) {
            currentInfoLine = "Runtime Configuration (SeaDAS Toolbox): null" + "\n";
            sysInfoText += currentInfoLine;
            appendToPane(sysInfoTextpane, currentInfoLine, Color.BLACK);
        } else {
            currentInfoLine = "Runtime Configuration (SeaDAS Toolbox): " + runtimeSeadasProperties.toString() + "\n";
            sysInfoText += currentInfoLine;
            appendToPane(sysInfoTextpane, currentInfoLine, Color.BLACK);
        }


        currentInfoLine = "JRE: " + jre + "\n";
        sysInfoText += currentInfoLine;
        appendToPane(sysInfoTextpane, currentInfoLine, Color.BLACK);


        currentInfoLine = "JVM: " + jvm + "\n";
        sysInfoText += currentInfoLine;
        appendToPane(sysInfoTextpane, currentInfoLine, Color.BLACK);


        currentInfoLine = "Memory: " + memory + "\n";
        sysInfoText += currentInfoLine;
        appendToPane(sysInfoTextpane, currentInfoLine, Color.BLACK);


        if (ocsswInfo.getOcsswLocation() == OCSSW_LOCATION_LOCAL) {
            currentInfoLine = "OCSSWROOT (Java Env): " + ocsswRootEnv + "\n";
            sysInfoText += currentInfoLine;
            appendToPane(sysInfoTextpane, currentInfoLine, Color.BLACK);


            currentInfoLine = "";   // todo temporary nulling this as later lines need editing

            try {
                ProcessBuilder processBuilder = new ProcessBuilder(new String[]{OCSSWInfo.getInstance().getOcsswRunnerScriptPath(), "-c", "-l", "which python3"});
                Process process = processBuilder.start();

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()));
                String line;
                Integer numOfLines = 0;
                currentInfoLine = "Python3 Directory: ";
                while ((line = reader.readLine()) != null) {
                    currentInfoLine += line + "\n";
                    if (line.trim().length() > 1) {
                        numOfLines++;
                    }
                }
                if (numOfLines == 0) {
                    currentInfoLine += "\n";
                }
                sysInfoText += currentInfoLine;
                appendToPane(sysInfoTextpane, currentInfoLine, Color.BLACK);

                if (numOfLines > 1) {
                    currentInfoLine = "NOTE: the extraneous output lines displayed were detected in your login configuration output" + "\n";
                    sysInfoText += currentInfoLine;
                    appendToPane(sysInfoTextpane, currentInfoLine, Color.RED);
                }

                reader.close();

                process.destroy();

                try {
                    Thread.sleep(10);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }

                if (process.exitValue() != 0) {
                    System.out.println("  WARNING!: Non zero exit code returned for 'which python3' ");
                }

            } catch (IOException e) {
                String warning = "  WARNING!! Could not retrieve system parameters because 'which python3' failed";
                currentInfoLine = warning + "\n";
                sysInfoText += currentInfoLine;

                currentInfoLine = e.toString() + "\n";
                sysInfoText += currentInfoLine;

                e.printStackTrace();
            }
        }


        currentInfoLine = "\n\n" + DASHES + "\n";
        sysInfoText += currentInfoLine;
        appendToPane(sysInfoTextpane, currentInfoLine, Color.BLACK);
        currentInfoLine = INDENT + "SeaDAS Toolbox: " + "\n";
        sysInfoText += currentInfoLine;
        appendToPane(sysInfoTextpane, currentInfoLine, Color.BLACK);
        currentInfoLine = DASHES + "\n";
        sysInfoText += currentInfoLine;
        appendToPane(sysInfoTextpane, currentInfoLine, Color.BLACK);


        currentInfoLine = "SeaDAS Toolbox Version: " + seadasProcessingModuleInfo.getSpecificationVersion() + "\n";
        sysInfoText += currentInfoLine;
        appendToPane(sysInfoTextpane, currentInfoLine, Color.BLACK);

        currentInfoLine = "SeaDAS Toolbox Build Date: " + seadasProcessingModuleInfo.getBuildVersion() + "\n";
        sysInfoText += currentInfoLine;
        appendToPane(sysInfoTextpane, currentInfoLine, Color.BLACK);

        if (ocsswDebug) {
            currentInfoLine = "SeaDAS Toolbox Implementation Version: " + seadasProcessingModuleInfo.getImplementationVersion() + "\n";
            sysInfoText += currentInfoLine;
            appendToPane(sysInfoTextpane, currentInfoLine, Color.BLACK);
        }


        if (!Files.exists(runtimeSeadasProperties)) {
            currentInfoLine = "Configuration: null" + "\n";
            sysInfoText += currentInfoLine;

        } else {
            currentInfoLine = "Configuration: " + runtimeSeadasProperties.toString() + "\n";
            sysInfoText += currentInfoLine;
        }
        appendToPane(sysInfoTextpane, currentInfoLine, Color.BLACK);


        if (ocsswDebug) {
            final Preferences preferences = Config.instance("seadas").load().preferences();
            String ocsswRootSeadasProperties = preferences.get(SEADAS_OCSSW_ROOT_PROPERTY, null);
            currentInfoLine = "seadas.ocssw.root (seadas.properties): " + ocsswRootSeadasProperties + "\n";
            sysInfoText += currentInfoLine;
            appendToPane(sysInfoTextpane, currentInfoLine, Color.BLACK);
        }

        if ("docker".equals(ocsswLocation)) {
            currentInfoLine = "OCSSW Docker Root Directory: " + ocsswRootDocker + "\n";
        } else {
            currentInfoLine = "OCSSW Root Directory: " + ocsswRootOcsswInfo + "\n";
        }
        sysInfoText += currentInfoLine;
        appendToPane(sysInfoTextpane, currentInfoLine, Color.BLACK);


        if (ocsswRootOcsswInfo != null) {
            if (ocsswInfo.getOcsswLocation() != OCSSW_LOCATION_LOCAL) {
//                if (!Files.exists(Paths.get(ocsswRootDocker))) {
//                    currentInfoLine = "WARNING!! Directory '" + ocsswRootDocker + "' does not exist" + "\n";
//                    sysInfoText += currentInfoLine;
//                    appendToPane(sysInfoTextpane, currentInfoLine, Color.RED);
//                }
            } else {
                if (!Files.exists(Paths.get(ocsswRootOcsswInfo))) {
                    currentInfoLine = "WARNING!! Directory '" + ocsswRootOcsswInfo + "' does not exist" + "\n";
                    sysInfoText += currentInfoLine;
                    appendToPane(sysInfoTextpane, currentInfoLine, Color.RED);
                }
            }
        }

        currentInfoLine = "OCSSW Docker Log Directory: " + ocsswLogDir + "\n";
        sysInfoText += currentInfoLine;
        appendToPane(sysInfoTextpane, currentInfoLine, Color.BLACK);
        if ((ocsswLogDir != null) && !Files.exists(Paths.get(ocsswLogDir))) {
            currentInfoLine = "WARNING!! Directory '" + ocsswLogDir + "' does not exist" + "\n";
            sysInfoText += currentInfoLine;
            appendToPane(sysInfoTextpane, currentInfoLine, Color.RED);
        }


        currentInfoLine = "OCSSW Location: " + ocsswLocation + "\n";
        sysInfoText += currentInfoLine;
        appendToPane(sysInfoTextpane, currentInfoLine, Color.BLACK);


        //need to consider "docker" condition
        if (ocsswInfo.getOcsswLocation() == OCSSW_LOCATION_LOCAL) {
            currentInfoLine = "Environment {$OCSSWROOT} (external): " + ocsswRootEnv + "\n";
            sysInfoText += currentInfoLine;
            appendToPane(sysInfoTextpane, currentInfoLine, Color.BLACK);
        }

        if ((ocsswRootOcsswInfo != null) && ocsswRootEnv != null && !ocsswRootOcsswInfo.equals(ocsswRootEnv) &&
                "local".equals(ocsswLocation)) {
            currentInfoLine = "  WARNING!: An environment variable for OCSSWROOT exists which does not match the GUI configuration. " +
                    "The GUI will use '" + ocsswRootOcsswInfo + "' as the ocssw root inside the GUI." + "\n";
            sysInfoText += currentInfoLine;
            appendToPane(sysInfoTextpane, currentInfoLine, Color.RED);
        }


        currentInfoLine = "\n\n" + DASHES + "\n";
        sysInfoText += currentInfoLine;
        appendToPane(sysInfoTextpane, currentInfoLine, Color.BLACK);
        currentInfoLine = INDENT + "NASA Science Processing (OCSSW): " + "\n";
        sysInfoText += currentInfoLine;
        appendToPane(sysInfoTextpane, currentInfoLine, Color.BLACK);
        currentInfoLine = DASHES + "\n";
        sysInfoText += currentInfoLine;
        appendToPane(sysInfoTextpane, currentInfoLine, Color.BLACK);

        ocsswScriptsDirPath = ocsswRootOcsswInfo + File.separator + OCSSW_SCRIPTS_DIR_SUFFIX;
        ocsswRunnerScriptPath = ocsswScriptsDirPath + System.getProperty("file.separator") + OCSSW_RUNNER_SCRIPT;
        ocsswBinDirPath = ocsswRootOcsswInfo + System.getProperty("file.separator") + OCSSW_BIN_DIR_SUFFIX;

//        String[] command = {"/bin/bash", ocsswRunnerScriptPath, " --ocsswroot ", ocsswRootOcsswInfo, OCSSW_SEADAS_INFO_PROGRAM_NAME};
        String[] command = {OCSSWInfo.getInstance().getOcsswRunnerScriptPath(), ocsswRunnerScriptPath, " --ocsswroot ", ocsswRootOcsswInfo, OCSSW_SEADAS_INFO_PROGRAM_NAME};
        ocsswSeadasInfoPath = ocsswBinDirPath + System.getProperty("file.separator") + OCSSW_SEADAS_INFO_PROGRAM_NAME;

        if (ocsswInfo.getOcsswLocation() != OCSSW_LOCATION_LOCAL) {
            currentInfoLine = ocsswInfo.getRemoteSeaDASInfo();
            sysInfoText += currentInfoLine;
            appendToPane(sysInfoTextpane, currentInfoLine, Color.BLACK);
        } else {
            if ((ocsswRootOcsswInfo == null) || (!Files.exists(Paths.get(ocsswRootOcsswInfo))) && "local".equals(ocsswLocation)) {
                if ((ocsswRootEnv != null) && Files.exists(Paths.get(ocsswRootEnv))) {
                    currentInfoLine = "WARNING! Processing not configured in the GUI but an installation currently exists in the directory '" + ocsswRootEnv +
                            "'. To configure the GUI to use this installation then update the 'OCSSW ROOT' directory in Menu > SeaDAS-Toolbox > SeaDAS Processors Location";
                    sysInfoText += currentInfoLine;
                    appendToPane(sysInfoTextpane, currentInfoLine, Color.RED);
                } else {
                    currentInfoLine = "  Warning! Processers not installed " + "\n\n";
                    sysInfoText += currentInfoLine;
                    appendToPane(sysInfoTextpane, currentInfoLine, Color.RED);

                    printGeneralSystemInfo(ocsswDebug);
                    appendToPane(sysInfoTextpane, currentInfoLine, Color.BLACK);
                }
            } else if ((!Files.exists(Paths.get(ocsswRootDocker))) && "docker".equals(ocsswLocation)) {
                currentInfoLine = "  Warning (for docker)! Processers not installed " + "\n\n";
                sysInfoText += currentInfoLine;
                appendToPane(sysInfoTextpane, currentInfoLine, Color.RED);

                printGeneralSystemInfo(ocsswDebug);
                appendToPane(sysInfoTextpane, currentInfoLine, Color.BLACK);
            } else {

                if ("docker".equals(ocsswLocation)) {
                    currentInfoLine = "  WARNING! Cannot find 'seadas_info' in the OCSSW Docker bin directory" + "\n\n";
                    sysInfoText += currentInfoLine;
                    appendToPane(sysInfoTextpane, currentInfoLine, Color.RED);

                    printGeneralSystemInfo(ocsswDebug);
                    appendToPane(sysInfoTextpane, currentInfoLine, Color.RED);
                }
            else if (!Files.isExecutable(Paths.get(ocsswSeadasInfoPath)) && "local".equals(ocsswLocation)) {
                    currentInfoLine = "  WARNING! Cannot find 'seadas_info' in the OCSSW bin directory" + "\n\n";
                    sysInfoText += currentInfoLine;
                    appendToPane(sysInfoTextpane, currentInfoLine, Color.RED);

                    printGeneralSystemInfo(ocsswDebug);
                    appendToPane(sysInfoTextpane, currentInfoLine, Color.RED);
                } else {
                    currentInfoLine = "";
                    try {

                        ProcessBuilder processBuilder = new ProcessBuilder(command);
                        Process process = processBuilder.start();

                        BufferedReader reader = new BufferedReader(
                                new InputStreamReader(process.getInputStream()));
                        String line;

                        while ((line = reader.readLine()) != null) {
                            if (!line.contains("NASA Science Processing (OCSSW)")) {
                                if (line.contains("General System and Software")) {
                                    currentInfoLine += "\n" + DASHES + "\n";
                                    currentInfoLine += INDENT + "General System and Software: " + "\n";
                                    currentInfoLine += DASHES + "\n";
                                } else {
                                    currentInfoLine += line + "\n";
                                }
                            }
                        }

                        sysInfoText += currentInfoLine;
                        appendToPane(sysInfoTextpane, currentInfoLine, Color.BLACK);

                        reader.close();
                        process.destroy();

                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }

                        if (process.exitValue() != 0) {
                            System.out.println("  WARNING!: Non zero exit code returned for \'" + command + "\' ");
                        }

                    } catch (IOException e) {
                        String warning = "  WARNING!! Could not retrieve system parameters because command \'" + command.toString() + "\' failed";
                        currentInfoLine = warning + "\n";
                        currentInfoLine += e.toString() + "\n";
                        sysInfoText += currentInfoLine;
                        appendToPane(sysInfoTextpane, currentInfoLine, Color.RED);
                        e.printStackTrace();
                    }
                }
            }
        }

        sysInfoTextpane.setEditable(false);
        sysInfoTextpane.setCaretPosition(0);


        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        sysInfoTextpane.setPreferredSize(new Dimension(750, 750));

        panel.add(scroll, gbc);

        return panel;
    }

    private void printGeneralSystemInfo(Boolean ocsswDebug) {
        currentInfoLine = "\n\n" + DASHES + "\n";
        currentInfoLine += INDENT + "General System and Software (from GUI): " + "\n";
        currentInfoLine += DASHES + "\n\n";

        currentInfoLine += "Operating System: " + System.getProperty("os.name");
        currentInfoLine += " " + System.getProperty("os.version") + "\n";
        currentInfoLine += "Java Version: " + System.getProperty("java.version") + "\n";

        if (ocsswDebug) {
            try {
                Process process = Runtime.getRuntime().exec("python --version");

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    currentInfoLine += line + "\n";
                }

                reader.close();

                process.destroy();

                if (process.exitValue() != 0) {
                    System.out.println("WARNING!: Non zero exit code returned for 'python --version' ");
                }

            } catch (IOException e) {
                String warning = "WARNING!! Could not retrieve system parameters because 'pyhton --version' failed";
                currentInfoLine += warning + "\n";
                currentInfoLine += e.toString() + "\n";
                e.printStackTrace();
            }
        }

        sysInfoText += currentInfoLine;
    }

    private void appendToPane(JTextPane tp, String msg, Color c) {
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
