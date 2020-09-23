package gov.nasa.gsfc.seadas.processing.ocssw;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.runtime.RuntimeContext;
import com.bc.ceres.core.runtime.Version;
import gov.nasa.gsfc.seadas.processing.common.FileInfoFinder;
import gov.nasa.gsfc.seadas.processing.common.SeadasLogger;
import gov.nasa.gsfc.seadas.processing.core.*;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.VersionChecker;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.runtime.Config;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static gov.nasa.gsfc.seadas.processing.core.L2genData.OPER_DIR;
import static gov.nasa.gsfc.seadas.processing.ocssw.OCSSWConfigData.SEADAS_LOG_DIR_PROPERTY;
import static gov.nasa.gsfc.seadas.processing.ocssw.OCSSWConfigData.SEADAS_OCSSW_ROOT_PROPERTY;

/**
 * Created by aabduraz on 3/27/17.
 * To install ocssw run: /tmp/install_ocssw --tag $TAG -i $OCSSWROOT --seadas --$MISSIONNAME
 * To get valid ocssw tags: /tmp/install_ocssw --list_tags
 */
public abstract class OCSSW {

    public static final String SEADAS_OCSSW_VERSIONS_JSON_NAME = "seadasVersions.json";

    public static final String OCSSW_CLIENT_SHARED_DIR_NAME_PROPERTY = "ocssw.sharedDir";
    public static final String MLP_PAR_FILE_NAME = "mlp_par_file";
    public static final String OCSSW_INSTALLER_URL = "https://oceandata.sci.gsfc.nasa.gov/manifest/install_ocssw";
    public static final String OCSSW_MANIFEST_URL = "https://oceandata.sci.gsfc.nasa.gov/manifest/manifest.py";
    public static final String OCSSW_SEADAS_VERSIONS_URL = "https://oceandata.sci.gsfc.nasa.gov/manifest/seadasVersions.json";
    public static final String TMP_OCSSW_INSTALLER_OLD = (new File(System.getProperty("java.io.tmpdir"), "install_ocssw.py")).getPath();
    public static final String TMP_OCSSW_INSTALLER = (new File(System.getProperty("java.io.tmpdir"), "install_ocssw")).getPath();
    public static final String TMP_OCSSW_MANIFEST = (new File(System.getProperty("java.io.tmpdir"), "manifest.py")).getPath();
    public static final String TMP_SEADAS_OCSSW_VERSIONS_FILE = (new File(System.getProperty("java.io.tmpdir"), SEADAS_OCSSW_VERSIONS_JSON_NAME)).getPath();

    public static String NEXT_LEVEL_NAME_FINDER_PROGRAM_NAME = "next_level_name.py";
    public static String NEXT_LEVEL_FILE_NAME_TOKEN = "Output Name:";
    public static final String GET_OBPG_FILE_TYPE_PROGRAM_NAME = "get_obpg_file_type.py";
    public static final String UPDATE_LUTS_PROGRAM_NAME = "update_luts.py";
    public static final String SEADAS_CONFIG_DIR = ".snap";

    private static boolean monitorProgress = false;
    private ArrayList<String> ocsswTags;
    private String seadasVersion;
    private String seadasToolboxVersion;
    private ArrayList<String> ocsswTagsValid4CurrentSeaDAS;
    String programName;
    private String xmlFileName;
    String ifileName;
    //File ifileDir;
    private String missionName;
    private String fileType;

    String[] commandArrayPrefix;
    String[] commandArraySuffix;
    String[] commandArray;

    //HashMap<String, Mission> missions;

    OCSSWInfo ocsswInfo = OCSSWInfo.getInstance();

    private int processExitValue;

    private boolean ofileNameFound;

    String ofileName;
    String ofileDir;
    String ifileDir;

    private String serverSharedDirName = null;
    private boolean ocsswInstalScriptDownloadSuccessful = false;

    public static OCSSW getOCSSWInstance() {

        OCSSWInfo ocsswInfo = OCSSWInfo.getInstance();
        if (ocsswInfo == null) {
            return null;
        }

        String ocsswLocation = ocsswInfo.getOcsswLocation();

        if ( ocsswLocation == null) {
            return null;
        }

        if (ocsswLocation.equals(OCSSWInfo.OCSSW_LOCATION_LOCAL)) {
            return new OCSSWLocal();
        } else if (ocsswLocation.equals(OCSSWInfo.OCSSW_LOCATION_VIRTUAL_MACHINE)) {
            return new OCSSWVM();
        } else if (ocsswLocation.equals(OCSSWInfo.OCSSW_LOCATION_REMOTE_SERVER)) {
            return new OCSSWRemote();
        }
        return new OCSSWLocal();
    }

    public abstract ProcessObserver getOCSSWProcessObserver(Process process, String processName, ProgressMonitor progressMonitor);


    public boolean isOCSSWExist() {
        return ocsswInfo.isOCSSWExist();
    }


    public boolean isProgramValid() {
        return true;
    }

    public String getOCSSWLogDirPath(){
        Preferences preferences;
        String appContextId = SystemUtils.getApplicationContextId();
        String logDirPath = ifileDir;
        preferences = Config.instance("seadas").load().preferences();

        if (preferences != null ) {
            logDirPath = preferences.get(SEADAS_LOG_DIR_PROPERTY, ifileDir);
            if (logDirPath == null) {
                logDirPath = System.getProperty("user.home") + File.separator + ".snap" + File.separator +  "seadas_logs";
            }
            File logDir = new File(logDirPath);
            if (!logDir.exists()) {
                try {
                    Files.createDirectory(Paths.get(logDirPath));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return logDirPath;
    }
    public abstract boolean isMissionDirExist(String missionName);

    public abstract String[] getMissionSuites(String missionName, String programName);

    public abstract ArrayList<String> readSensorFileIntoArrayList(File file);

    public abstract Process execute(ProcessorModel processorModel);
    public abstract String executeUpdateLuts(ProcessorModel processorModel);
    public abstract Process executeSimple(ProcessorModel processorModel);
    public abstract InputStream executeAndGetStdout(ProcessorModel processorModel);

    public abstract Process execute(ParamList paramList);

    public abstract Process execute(String[] commandArray);

    public abstract Process execute(String programName, String[] commandArrayParams);

    public abstract void getOutputFiles(ProcessorModel processorModel);

    public abstract boolean getIntermediateOutputFiles(ProcessorModel processorModel);

    public abstract void findFileInfo(String fileName, FileInfoFinder fileInfoFinder);

    public abstract String getOfileDir();

    public abstract String getOfileName(String ifileName);

    public abstract String getOfileName(String ifileName, String programName);

    public abstract String getOfileName(String ifileName, String[] options);

    public abstract String getOfileName(String ifileName, String programName, String suiteValue);

    public String getProgramName() {
        return programName;
    }

    public void setProgramName(String programName) {
        this.programName = programName;
        setCommandArrayPrefix();
        setCommandArraySuffix();
    }


    public abstract void setCommandArrayPrefix();

    public abstract void setCommandArraySuffix();

    public String getMissionName() {
        return missionName;
    }

    public void setMissionName(String missionName) {
        this.missionName = missionName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String[] getCommandArraySuffix() {
        return commandArraySuffix;
    }

    public void setCommandArraySuffix(String[] commandArraySuffix) {
        this.commandArraySuffix = commandArraySuffix;
    }

    public void updateOCSSWRoot(String installDir) {
        FileWriter fileWriter = null;
        try {
            final FileReader reader = new FileReader(new File(RuntimeContext.getConfig().getConfigFilePath()));
            final BufferedReader br = new BufferedReader(reader);

            StringBuilder text = new StringBuilder();
            String line;
            boolean isOCSSWRootSpecified = false;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("seadas.ocssw.root")) {
                    line = "seadas.ocssw.root = " + installDir;
                    isOCSSWRootSpecified = true;
                }
                text.append(line);
                text.append("\n");
            }
            //Append "seadas.ocssw.root = " + installDir + "\n" to the runtime config file if it is not exist
            if (!isOCSSWRootSpecified) {
                text.append("seadas.ocssw.root = " + installDir + "\n");
            }
            fileWriter = new FileWriter(new File(RuntimeContext.getConfig().getConfigFilePath()));
            fileWriter.write(text.toString());
            if (fileWriter != null) {
                fileWriter.close();
            }
            ocsswInfo.setOcsswRoot(installDir);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public void updateOCSSWRootProperty(String installDir) {
        final Preferences preferences = Config.instance("seadas").load().preferences();

        preferences.put(SEADAS_OCSSW_ROOT_PROPERTY, installDir);
        try {
            preferences.flush();
        } catch (BackingStoreException e) {
            SnapApp.getDefault().getLogger().severe(e.getMessage());
        }
    }

    public String[] getCommandArray() {
        return commandArray;
    }

    public void setCommandArray(String[] commandArray) {
        this.commandArray = commandArray;
    }

    public String getIfileName() {
        return ifileName;
    }

    public void setIfileName(String ifileName) {
        this.ifileName = ifileName;
        if (ifileName != null) {
            ifileDir = new File(ifileName).getParent();
        }
        setOfileNameFound(false);
        ofileName = null;
    }

    public String getXmlFileName() {
        return xmlFileName;
    }

    public void setXmlFileName(String xmlFileName) {
        this.xmlFileName = xmlFileName;
    }

    public static boolean isMonitorProgress() {
        return monitorProgress;
    }

    public static void setMonitorProgress(boolean mProgress) {
        monitorProgress = mProgress;
    }

    public abstract HashMap<String, String> computePixelsFromLonLat(ProcessorModel processorModel);

    public int getProcessExitValue() {
        return processExitValue;
    }

    public void setProcessExitValue(int processExitValue) {
        this.processExitValue = processExitValue;
    }

    public void waitForProcess() {
    }

    public boolean isOfileNameFound() {
        return ofileNameFound;
    }

    public void setOfileNameFound(boolean ofileNameFound) {
        this.ofileNameFound = ofileNameFound;
    }

    public String getOCSSWClientSharedDirName() {
        return RuntimeContext.getConfig().getContextProperty(OCSSW_CLIENT_SHARED_DIR_NAME_PROPERTY);
    }

    public void setServerSharedDirName(String name) {
        serverSharedDirName = name;
    }

    public String getServerSharedDirName() {
        return serverSharedDirName;
    }

    private static void handleException(String errorMessage) {
        Dialogs.showError(errorMessage);
    }

    public boolean isOcsswInstalScriptDownloadSuccessful() {
        return ocsswInstalScriptDownloadSuccessful;
    }

    /**
     * This method downloads the ocssw installer program ocssw_install to a tmp directory
     * @return
     */
    public boolean downloadOCSSWInstaller() {

        if (isOcsswInstalScriptDownloadSuccessful()) {
            return ocsswInstalScriptDownloadSuccessful;
        }
        try {
            //download install_ocssw
            URL website = new URL(OCSSW_INSTALLER_URL);
            ReadableByteChannel rbc = Channels.newChannel(website.openStream());
            FileOutputStream fos = new FileOutputStream(TMP_OCSSW_INSTALLER);
            fos.getChannel().transferFrom(rbc, 0, 1 << 24);
            fos.close();
            (new File(TMP_OCSSW_INSTALLER)).setExecutable(true);
            updateOCSSWTags();
            ocsswInstalScriptDownloadSuccessful = true;

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

        } catch (MalformedURLException malformedURLException) {
            handleException("URL for downloading install_ocssw.py is not correct!");
        } catch (FileNotFoundException fileNotFoundException) {
            handleException("ocssw installation script failed to download. \n" +
                    "Please check network connection or 'seadas.ocssw.root' variable in the 'seadas.config' file. \n" +
                    "possible cause of error: " + fileNotFoundException.getMessage());
        } catch (IOException ioe) {
            handleException("ocssw installation script failed to download. \n" +
                    "Please check network connection or 'seadas.ocssw.root' variable in the \"seadas.config\" file. \n" +
                    "possible cause of error: " + ioe.getLocalizedMessage());
        } finally {
            return ocsswInstalScriptDownloadSuccessful;
        }
    }

    public void updateOCSSWTags(){
        Runtime rt = Runtime.getRuntime();
        String[] commands = {TMP_OCSSW_INSTALLER, "--list_tags"};
        Process proc = null;
        try {
            proc = rt.exec(commands);
        } catch (IOException e) {
            e.printStackTrace();
        }

        BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));

        // Read the output from the command
        //System.out.println("Here is the standard output of the command:\n");
        String s = null;

        ArrayList<String> tagsList = new ArrayList<>();
        while (true) {
            try {
                if (!((s = stdInput.readLine()) != null)) break;
            } catch (IOException e) {
                e.printStackTrace();
            }
            //System.out.println(s);
            tagsList.add(s);
        }
        setOcsswTags(tagsList);
        getValidOCSSWTags4SeaDASVersion();
    }
    public void getValidOCSSWTags4SeaDASVersion(){
        //JSON parser object to parse read file
        setOcsswTagsValid4CurrentSeaDAS(new ArrayList<String>());
        JSONParser jsonParser = new JSONParser();
        try {

            URL tagsURL = new URL("https://oceandata.sci.gsfc.nasa.gov/manifest/seadasVersions.json");
            URLConnection tagsConnection = tagsURL.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(tagsConnection.getInputStream()));

            //Read JSON file
            Object obj = jsonParser.parse(in);

            JSONArray validSeaDASTags = (JSONArray) obj;
            System.out.println(validSeaDASTags);

            //Iterate over seadas tag array
            validSeaDASTags.forEach( tagObject -> parseValidSeaDASTagObject( (JSONObject) tagObject ) );
            in.close();

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }  catch (ParseException e) {
            e.printStackTrace();
        }
    }
    private void parseValidSeaDASTagObject(JSONObject tagObject)
    {
        Version currentVersion = VersionChecker.getInstance().getLocalVersion();
        this.seadasVersion = currentVersion.toString();
        seadasToolboxVersion = getClass().getPackage().getImplementationVersion();

        String seadasVersionString = (String)tagObject.get("seadas");

        if (seadasVersionString.equals(seadasToolboxVersion)) {
            //Get corresponding ocssw tags for seadas
            JSONArray ocsswTags = (JSONArray) tagObject.get("ocssw");
            if (ocsswTags != null) {
                for (int i=0;i<ocsswTags.size();i++){
                    try {
                        getOcsswTagsValid4CurrentSeaDAS().add((String) ocsswTags.get(i));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void updateL2genProductInfoXMLFiles(){
        String programName = "l2gen";
        File dataDir = SystemUtils.getApplicationDataDir();
        File l2genDir = new File(dataDir, OPER_DIR);
        l2genDir.mkdirs();

        File xmlFile = new File(l2genDir, L2genData.PRODUCT_INFO_XML);
        ProcessorModel processorModel = new ProcessorModel(programName, this);
        processorModel.setAcceptsParFile(false);
        processorModel.addParamInfo("prodxmlfile", xmlFile.getAbsolutePath(), ParamInfo.Type.OFILE);
        processorModel.getParamInfo("prodxmlfile").setUsedAs(ParamInfo.USED_IN_COMMAND_AS_OPTION);

        try {
            Process p = executeSimple(processorModel);
            waitForProcess();

            if (getProcessExitValue() != 0) {
                throw new IOException(programName + " returned nonzero exitvalue");
            }
            boolean downloadSuccessful = getIntermediateOutputFiles(processorModel);

        } catch (Exception e) {
            SeadasLogger.getLogger().log(Level.SEVERE, "Problem creating product XML file: " + e.getMessage());
        }
    }
    public void updateOCSSWProgramXMLFiles(){
//        String executable = getGuiName();
//        // String executable = SeadasProcessorInfo.getExecutable(iFileInfo, processorId);
//        if (executable.equals("l3gen")) {
//            executable = "l2gen";
//        }
//        ProcessorModel processorModel = new ProcessorModel(executable, ocssw);
//
//        processorModel.setAcceptsParFile(true);
//        processorModel.addParamInfo("ifile", file.getAbsolutePath(), ParamInfo.Type.IFILE);
//
//        if (suite != null) {
//            processorModel.addParamInfo("suite", suite, ParamInfo.Type.STRING);
//        }
//
//        processorModel.addParamInfo("-dump_options_xmlfile", xmlFile.getAbsolutePath(), ParamInfo.Type.OFILE);
//
//        try {
//            // Aquarius will use the static xml file instead of a generated one
//            if (getMode() != L2genData.Mode.L2GEN_AQUARIUS) {
//                Process p = ocssw.executeSimple(processorModel);
//                ocssw.waitForProcess();
//                if (ocssw.getProcessExitValue() != 0) {
//                    throw new IOException("l2gen failed to run");
//                }
//
//                ocssw.getIntermediateOutputFiles(processorModel);
//            }
//
//            if (!xmlFile.exists()) {
//                SeadasLogger.getLogger().severe("l2gen can't find paramInfo.xml file!");
//                VisatApp.getApp().showMessageDialog("", "SEVERE: paramInfo.xml not found!", ModalDialog.ID_OK, null);
//                return null;
//            }
//        }
    }

    public ArrayList<String> getOcsswTags() {
        return ocsswTags;
    }

    public void setOcsswTags(ArrayList<String> ocsswTags) {
        this.ocsswTags = ocsswTags;
    }

    public String getSeadasVersion() {
        return seadasVersion;
    }

    public void setSeadasVersion(String seadasVersion) {
        this.seadasVersion = seadasVersion;
    }

    public ArrayList<String> getOcsswTagsValid4CurrentSeaDAS() {
        return ocsswTagsValid4CurrentSeaDAS;
    }

    public void setOcsswTagsValid4CurrentSeaDAS(ArrayList<String> ocsswTagsValid4CurrentSeaDAS) {
        this.ocsswTagsValid4CurrentSeaDAS = ocsswTagsValid4CurrentSeaDAS;
    }
}
