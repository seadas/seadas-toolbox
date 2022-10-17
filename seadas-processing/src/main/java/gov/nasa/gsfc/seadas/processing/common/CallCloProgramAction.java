package gov.nasa.gsfc.seadas.processing.common;


import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import gov.nasa.gsfc.seadas.processing.core.L2genData;
import gov.nasa.gsfc.seadas.processing.core.ProcessObserver;
import gov.nasa.gsfc.seadas.processing.core.ProcessorModel;
import gov.nasa.gsfc.seadas.processing.ocssw.OCSSW;
import gov.nasa.gsfc.seadas.processing.ocssw.OCSSWInfo;
import gov.nasa.gsfc.seadas.processing.ocssw.OCSSWInfoGUI;
import gov.nasa.gsfc.seadas.processing.ocssw.OCSSWLocal;
import gov.nasa.gsfc.seadas.processing.utilities.ScrolledPane;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.runtime.Config;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.ModalDialog;
import org.esa.snap.ui.UIUtils;
import org.openide.util.ContextAwareAction;
import org.openide.util.LookupListener;
import org.openide.util.actions.Presenter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static gov.nasa.gsfc.seadas.processing.ocssw.OCSSW.UPDATE_LUTS_PROGRAM_NAME;
import static gov.nasa.gsfc.seadas.processing.ocssw.OCSSWConfigData.SEADAS_OCSSW_TAG_PROPERTY;


/**
 * @author Aynur Abdurazik
 * @since SeaDAS 7.0
 */

public class CallCloProgramAction extends AbstractSnapAction  implements Presenter.Menu  {

    public static final String CONTEXT_LOG_LEVEL_PROPERTY = SystemUtils.getApplicationContextId() + ".logLevel";
    public static final String LOG_LEVEL_PROPERTY = "logLevel";

    private static final Pattern PATTERN = Pattern.compile("^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");

    private String programName;
    private String dialogTitle;
    private String xmlFileName;

    private boolean printLogToConsole = false;
    private boolean openOutputInApp = true;
    protected OCSSW ocssw;
    public final static int TOTAL_WORK_DEFAULT = 100;

    private static final Set<String> KNOWN_KEYS = new HashSet<>(Arrays.asList("displayName", "programName", "dialogTitle", "helpId", "targetProductNameSuffix"));

    protected OCSSWInfo ocsswInfo = OCSSWInfo.getInstance();

    public static CallCloProgramAction create(Map<String, Object> properties) {
        CallCloProgramAction action = new CallCloProgramAction();
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            if (KNOWN_KEYS.contains(entry.getKey())) {
                action.putValue(entry.getKey(), entry.getValue());
            }
        }
        return action;
    }

//    @Override
//    public void configure(ConfigurationElement config) throws CoreException {
//        programName = getConfigString(config, "programName");
//        if (programName == null) {
//            throw new CoreException("Missing DefaultOperatorAction property 'programName'.");
//        }
//        dialogTitle = getValue(config, "dialogTitle", programName);
//        xmlFileName = getValue(config, "xmlFileName", ParamUtils.NO_XML_FILE_SPECIFIED);
//        super.configure(config);
//        //super.setEnabled(programName.equals(OCSSWInfo.OCSSW_INSTALLER_PROGRAM_NAME) || ocsswInfo.isOCSSWExist());
//    }
    public String getXmlFileName() {
        return xmlFileName;
    }

    public CloProgramUI getProgramUI(AppContext appContext) {
        if (programName.indexOf("extract") != -1) {
            return new ExtractorUI(programName, xmlFileName, ocssw);
        } else if (programName.indexOf("modis_GEO") != -1 || programName.indexOf("modis_L1B") != -1) {
            return new ModisGEO_L1B_UI(programName, xmlFileName, ocssw);
        } else if (programName.indexOf(ocsswInfo.OCSSW_INSTALLER_PROGRAM_NAME) != -1) {
            ocssw.downloadOCSSWInstaller();
            if (!ocssw.isOcsswInstalScriptDownloadSuccessful()) {
                return null;
            }

            if (ocsswInfo.getOcsswLocation() == null || ocsswInfo.getOcsswLocation().equals(OCSSWInfo.OCSSW_LOCATION_LOCAL)) {
                return new OCSSWInstallerFormLocal(appContext, programName, xmlFileName, ocssw);
            } else {
                return new OCSSWInstallerFormRemote(appContext, programName, xmlFileName, ocssw);
            }
        }else if (programName.indexOf("update_luts") != -1   ) {
            return new UpdateLutsUI(programName, xmlFileName, ocssw);
        }

        return new ProgramUIFactory(programName, xmlFileName, ocssw);//, multiIFile);
    }

    public static boolean validate(final String ip) {
        return PATTERN.matcher(ip).matches();
    }

    public void initializeOcsswClient() {

        ocssw = OCSSW.getOCSSWInstance();
        if ( ocssw == null ) {
            final AppContext appContext = getAppContext();
            final Window parent = appContext.getApplicationWindow();
            OCSSWInfoGUI ocsswInfoGUI = new OCSSWInfoGUI();
            ocsswInfoGUI.init(parent);
            ocsswInfo = OCSSWInfo.getInstance();
            ocssw = OCSSW.getOCSSWInstance();
        }
    }

    @Override
    public void actionPerformed(ActionEvent event) {

        initializeOcsswClient();

        if (ocssw == null) {
            return;
        }

        if (!(ocssw instanceof OCSSWLocal) && !OCSSWInfo.getInstance().isOcsswServerUp()) {
            OCSSWInfo.displayRemoteServerDownMessage();
            return;
        }

        ocssw.setProgramName(programName);

        final AppContext appContext = getAppContext();

        final CloProgramUI cloProgramUI = getProgramUI(appContext);

        if (cloProgramUI == null) {
            SeadasFileUtils.debug("getting the GUI for the program failed");
            return;
        }

        final Window parent = appContext.getApplicationWindow();

        final ModalDialog modalDialog = new ModalDialog(parent, dialogTitle, cloProgramUI, ModalDialog.ID_OK_APPLY_CANCEL_HELP, programName);
        modalDialog.getButton(ModalDialog.ID_OK).setEnabled(cloProgramUI.getProcessorModel().isReadyToRun());

        // todo  this block of comments may be deleted at some point if GUI sizing isn't a problem - but keep for the moment just in case
//        double resizeBoost = 1.1;  // resize the OSCCW generic GUIs to help in the case of scroll bar
//        Dimension dim = modalDialog.getJDialog().getPreferredSize();
//        int width = (int) Math.round(dim.width * resizeBoost);
//        int height = (int) Math.round(dim.height * resizeBoost);
//        modalDialog.getJDialog().setPreferredSize(new Dimension(width, height));

        cloProgramUI.getProcessorModel().addPropertyChangeListener(cloProgramUI.getProcessorModel().getRunButtonPropertyName(), new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                if (cloProgramUI.getProcessorModel().isReadyToRun()) {
                    modalDialog.getButton(ModalDialog.ID_OK).setEnabled(true);
                } else {
                    modalDialog.getButton(ModalDialog.ID_OK).setEnabled(false);
                }
                modalDialog.getJDialog().pack();
            }
        });

        cloProgramUI.getProcessorModel().addPropertyChangeListener("geofile", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                modalDialog.getJDialog().validate();
                modalDialog.getJDialog().pack();
            }
        });

        cloProgramUI.getProcessorModel().addPropertyChangeListener("infile", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                modalDialog.getJDialog().validate();
                modalDialog.getJDialog().pack();
            }
        });

        modalDialog.getButton(ModalDialog.ID_OK).setText("Run");
        modalDialog.getButton(ModalDialog.ID_HELP).setText("");
        modalDialog.getButton(ModalDialog.ID_HELP).setIcon(UIUtils.loadImageIcon("icons/Help24.gif"));

        //Make sure program is only executed when the "run" button is clicked.
        ((JButton) modalDialog.getButton(ModalDialog.ID_OK)).setDefaultCapable(false);
        modalDialog.getJDialog().getRootPane().setDefaultButton(null);

        final int dialogResult = modalDialog.show();

        Logger.getLogger(programName).info("dialog result: " + dialogResult);

        if (dialogResult != ModalDialog.ID_OK) {
            cloProgramUI.getProcessorModel().getParamList().clearPropertyChangeSupport();
            cloProgramUI.getProcessorModel().fireEvent(L2genData.CANCEL);
            return;
        }

        modalDialog.getButton(ModalDialog.ID_OK).setEnabled(false);

        final ProcessorModel processorModel = cloProgramUI.getProcessorModel();
        programName = processorModel.getProgramName();
        openOutputInApp = cloProgramUI.isOpenOutputInApp();

        if (!ocssw.isProgramValid()) {
            return;
        }

        if (programName.equals(ocsswInfo.OCSSW_INSTALLER_PROGRAM_NAME) && !ocssw.isOcsswInstalScriptDownloadSuccessful()) {
            displayMessage(programName, "ocssw installation script does not exist." + "\n" + "Please check network connection and rerun ''Install Processor''");
            return;
        }

        if (programName.equals(UPDATE_LUTS_PROGRAM_NAME)) {
            String message = ocssw.executeUpdateLuts(processorModel);
            Dialogs.showInformation(dialogTitle, message, null);
        } else {
            executeProgram(processorModel);
        }

        cloProgramUI.getProcessorModel().fireEvent(L2genData.CANCEL);

    }

    /**
     * @param pm is the model of the ocssw program to be executed
     * @output this is executed as a native process
     */
    public void executeProgram(ProcessorModel pm) {

        final ProcessorModel processorModel = pm;

        ProgressMonitorSwingWorker swingWorker = new ProgressMonitorSwingWorker<String, Object>(getAppContext().getApplicationWindow(), "Running " + programName + " ...") {

            @Override
            protected String doInBackground(com.bc.ceres.core.ProgressMonitor pm) throws Exception {

                ocssw.setMonitorProgress(true);

                final Process process = ocssw.execute(processorModel);
                if (process == null) {
                    throw new IOException(programName + " failed to create process.");
                }
                final ProcessObserver processObserver = ocssw.getOCSSWProcessObserver(process, programName, pm);
                final ConsoleHandler ch = new ConsoleHandler(programName);

                if (programName.equals(ocsswInfo.OCSSW_INSTALLER_PROGRAM_NAME)) {
                    Preferences preferences = Config.instance("seadas").load().preferences();
                    preferences.put(SEADAS_OCSSW_TAG_PROPERTY, processorModel.getParamValue("--tag"));
                }

                pm.beginTask(programName, TOTAL_WORK_DEFAULT);
                processObserver.addHandler(new ProgressHandler(programName, processorModel.getProgressPattern()));


                processObserver.addHandler(ch);

                processObserver.startAndWait();

                processorModel.setExecutionLogMessage(ch.getExecutionErrorLog());

                int exitCode = processObserver.getProcessExitValue();

                pm.done();
                if (exitCode == 0) {
                    String logDir = ocssw.getOCSSWLogDirPath(); //ocsswInfo.getLogDirPath();
                    SeadasFileUtils.writeToDisk(logDir + File.separator + "OCSSW_LOG_" + programName + ".txt",
                            "Execution log for " + "\n" + Arrays.toString(ocssw.getCommandArray()) + "\n" + processorModel.getExecutionLogMessage());
                } else {
                    throw new IOException(programName + " failed with exit code " + exitCode + ".\nCheck log for more details.");
                }

                ocssw.setMonitorProgress(false);
                return processorModel.getOfileName();
            }

            @Override
            protected void done() {
                try {
                    final String outputFileName = get();
                    ocssw.getOutputFiles(processorModel);
                    displayOutput(processorModel);
                    Dialogs.showInformation(dialogTitle, "Program execution completed!\n" + ((outputFileName == null) ? ""
                            : (programName.equals(ocsswInfo.OCSSW_INSTALLER_PROGRAM_NAME) ? "" : ("Output written to:\n" + outputFileName))), null);
                    if (ocsswInfo.getOcsswLocation()!= null && programName.equals(ocsswInfo.OCSSW_INSTALLER_PROGRAM_NAME) && ocsswInfo.getOcsswLocation().equals(OCSSWInfo.OCSSW_LOCATION_LOCAL)) {
                        ocssw.updateOCSSWRootProperty(processorModel.getParamValue("--install_dir"));
                        if (!ocssw.isOCSSWExist()) {
                            enableProcessors();
                        }
                    }

                    ProcessorModel secondaryProcessor = processorModel.getSecondaryProcessor();
                    if (secondaryProcessor != null) {
                        ocssw.setIfileName(secondaryProcessor.getParamValue(secondaryProcessor.getPrimaryInputFileOptionName()));
                        int exitCode = ocssw.execute(secondaryProcessor.getParamList()).exitValue();
                        if (exitCode == 0) {
                            Dialogs.showInformation(secondaryProcessor.getProgramName(),
                                    secondaryProcessor.getProgramName() + " done!\n", null);
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    displayMessage(programName, "execution exception: " + e.getMessage() + "\n" + processorModel.getExecutionLogMessage());
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        swingWorker.execute();
    }

    void displayOutput(ProcessorModel processorModel) throws Exception {
        String ofileName = processorModel.getOfileName();
        if (openOutputInApp) {

            File ifileDir = processorModel.getIFileDir();

            StringTokenizer st = new StringTokenizer(ofileName);
            while (st.hasMoreTokens()) {
                File ofile = SeadasFileUtils.createFile(ocssw.getOfileDir(), st.nextToken());
                getAppContext().getProductManager().addProduct(ProductIO.readProduct(ofile));
            }
        }
    }

    private void enableProcessors() {

//        CommandManager commandManager = getAppContext().getApplicationPage().getCommandManager();
//        String namesToExclude = ProcessorTypeInfo.getExcludedProcessorNames();
//        for (String processorName : ProcessorTypeInfo.getProcessorNames()) {
//            if (!namesToExclude.contains(processorName)) {
//                if (commandManager.getCommand(processorName) != null) {
//                    commandManager.getCommand(processorName).setEnabled(true);
//                }
//            }
//        }
//        commandManager.getCommand("install_ocssw").setText("Update Data Processors");
    }

    private void displayMessage(String programName, String message) {
        ScrolledPane messagePane = new ScrolledPane(programName, message, this.getAppContext().getApplicationWindow());
        messagePane.setVisible(true);
    }

    public String getProgramName() {
        return programName;
    }

    public void setProgramName(String programName) {
        this.programName = programName;
    }

    public String getDialogTitle() {
        return dialogTitle;
    }

    public void setDialogTitle(String dialogTitle) {
        this.dialogTitle = dialogTitle;
    }

    public void setXmlFileName(String xmlFileName) {
        this.xmlFileName = xmlFileName;
    }

    /**
     * Handler that tries to extract progress from stdout of ocssw processor
     */
    public static class ProgressHandler implements ProcessObserver.Handler {

        protected boolean stdoutOn;
        protected String programName;
        protected Pattern progressPattern;
        private double percentWorkedInProgressMonitor = 0;
        private int totalSteps = -1;
        private boolean debug = false;
        private int NULL_MATCH_VALUE = -9999;


        public ProgressHandler(String programName, Pattern progressPattern) {
            this.programName = programName;
            this.progressPattern = progressPattern;
        }


        private void handleLineRead(String line, Process process,  com.bc.ceres.core.ProgressMonitor progressMonitor) {

            Matcher matcher = progressPattern.matcher(line);
            if (matcher != null && matcher.find()) {
                int stepCurrentLine = NULL_MATCH_VALUE;
                int totalStepsCurrentLine = NULL_MATCH_VALUE;

                debugPrint("line=" + line);
                debugPrint("pattern=" + progressPattern.toString());
                try {
                    stepCurrentLine = Integer.parseInt(matcher.group(1));
                    totalStepsCurrentLine = Integer.parseInt(matcher.group(2));
                } catch (Exception e) {
                    debugPrint("Exception thrown on following line:");
                }


                if (stepCurrentLine != NULL_MATCH_VALUE && totalStepsCurrentLine != NULL_MATCH_VALUE) {
                    debugPrint("stepCurrentLine=" + stepCurrentLine);
                    debugPrint("totalStepsCurrentLine=" + totalStepsCurrentLine);

                    // set only once
                    if (totalSteps < 0) {
                        totalSteps = totalStepsCurrentLine;
                        debugPrint("totalSteps=" + totalSteps);
                        if (totalSteps < 1) {
                            // can't allow illegal value which would cause math errors
                            totalSteps = 1;
                            debugPrint("Resetting - totalSteps=" + totalSteps);
                        }
                    }

                    // only proceed with incrementing if totalSteps matches
                    if (totalStepsCurrentLine == totalSteps) {
                        double percentWorkedCurrentLine = 100 * (stepCurrentLine - 1) / totalSteps;
                        debugPrint("percentWorkedCurrentLine=" + percentWorkedCurrentLine);

                        if (percentWorkedCurrentLine > percentWorkedInProgressMonitor) {
                            int newWork = (int) Math.round(percentWorkedCurrentLine - percentWorkedInProgressMonitor);
                            if (newWork > 0) {
                                // don't allow progress to fill up to 100, hold it at 99%
                                if ((percentWorkedInProgressMonitor + newWork) <= 99) {
                                    debugPrint("percentWorkedInProgressMonitor=" + percentWorkedInProgressMonitor);
                                    debugPrint("Adding to progress monitor newWork=" + newWork);

                                    progressMonitor.worked(newWork);
                                    percentWorkedInProgressMonitor += newWork;
                                }
                            }
                        }
                    }
                }
            }
            progressMonitor.setTaskName(programName);
            progressMonitor.setSubTaskName(line);
        }

        private void debugPrint(String msg) {
            if (debug) {
                System.out.println(msg);
            }
        }

        @Override
        public void handleLineOnStdoutRead(String line, Process process, com.bc.ceres.core.ProgressMonitor progressMonitor) {
            stdoutOn = true;
            handleLineRead(line, process, progressMonitor);
        }

        @Override
        public void handleLineOnStderrRead(String line, Process process,  com.bc.ceres.core.ProgressMonitor progressMonitor) {
            if (!stdoutOn) {
                handleLineRead(line, process, progressMonitor);
            }
        }
    }

    public static class ConsoleHandler implements ProcessObserver.Handler {

        String programName;

        private String executionErrorLog = "";

        public ConsoleHandler(String programName) {
            this.programName = programName;
        }

        @Override
        public void handleLineOnStdoutRead(String line, Process process,  com.bc.ceres.core.ProgressMonitor progressMonitor) {
            Logger.getLogger(programName).info(programName + ": " + line);
            executionErrorLog = executionErrorLog + line + "\n";
        }

        @Override
        public void handleLineOnStderrRead(String line, Process process,  com.bc.ceres.core.ProgressMonitor progressMonitor) {
            Logger.getLogger(programName).info(programName + " stderr: " + line);
            executionErrorLog = executionErrorLog + line + "\n";
        }

        public String getExecutionErrorLog() {
            return executionErrorLog;
        }
    }

    private static class TerminationHandler implements ProcessObserver.Handler {

        @Override
        public void handleLineOnStdoutRead(String line, Process process,  com.bc.ceres.core.ProgressMonitor progressMonitor) {
            if (progressMonitor.isCanceled()) {
                process.destroy();
            }
        }

        @Override
        public void handleLineOnStderrRead(String line, Process process,  com.bc.ceres.core.ProgressMonitor progressMonitor) {
            if (progressMonitor.isCanceled()) {
                process.destroy();
            }
        }
    }

    // todo This entire block of code is needed now since ProgressHandler is being called.   This block cab be deleted sometime in future after testing
//    /**
//     * Handler that tries to extract progress from stderr of ocssw_installer
//     */
//    public static class InstallerHandler extends ProgressHandler {
//
//        public InstallerHandler(String programName, Pattern progressPattern) {
//            super(programName, progressPattern);
//        }
//
//
//        public void handleLineRead(String line, Process process,  com.bc.ceres.core.ProgressMonitor progressMonitor) {
//
//            Matcher matcher = progressPattern.matcher(line);
//            if (matcher.find()) {
//                int currentWork = Integer.parseInt(matcher.group(1));
//                int totalWork = Integer.parseInt(matcher.group(2))+1;  // note: buffer added so as not to fill
//                if (!progressSeen) {
//                    progressSeen = true;
//                    progressMonitor.beginTask(programName, totalWork);
//
//                }
//
//                // System.out.println("WORK of TOTAL=" + currentWork + "of " + totalWork);
//
//                // Note: if statement used to prevent progress monitor from filling
//                // up here so that the GUI doesn't close too early.
//                // The progress monitor will be specifically closed when finished.
//                if (currentWork < totalWork) {
//                    progressMonitor.worked(1);
//                }
//
//                currentText = line;
//            }
//
//            progressMonitor.setTaskName(programName);
//            progressMonitor.setSubTaskName(line);
//        }
//
//
//        @Override
//        public void handleLineOnStdoutRead(String line, Process process,  com.bc.ceres.core.ProgressMonitor progressMonitor) {
//            stdoutOn = true;
//
//            handleLineRead(line, process, progressMonitor);
//        }
//
//
//
//        @Override
//        public void handleLineOnStderrRead(String line, Process process,  com.bc.ceres.core.ProgressMonitor progressMonitor) {
//            if (!super.stdoutOn) {
//                handleLineRead(line, process, progressMonitor);
//            }
//        }
//    }

    @Override
    public JMenuItem getMenuPresenter() {
        JMenuItem menuItem = new JMenuItem(this);
        menuItem.setIcon(null);
        menuItem.setName((String) getValue(Action.NAME));
        menuItem.setToolTipText((String) getValue(Action.SHORT_DESCRIPTION));
        return menuItem;
    }
}
