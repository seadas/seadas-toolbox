package gov.nasa.gsfc.seadas.processing.ocssw;

import gov.nasa.gsfc.seadas.processing.common.SeadasToolboxVersion;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.runtime.Config;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jsonp.JsonProcessingFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

import javax.json.JsonObject;
import javax.json.stream.JsonGenerator;
import javax.swing.*;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

import static gov.nasa.gsfc.seadas.processing.ocssw.OCSSWConfigData.*;

/**
 * Created by aabduraz on 5/25/17.
 */

/**
 * OCSSWInfo is an Singleton class.
 */
public class OCSSWInfo {

    public final static String OCSSW_VM_SERVER_SHARED_DIR_PROPERTY = "seadas.ocssw.sharedDir";
    public static final String SEADAS_CLIENT_ID_PROPERTY = "seadas.client.id";
//    public static final String OCSSW_KEEP_FILES_ON_SERVER_PROPERTY = "seadas.ocssw.keepFilesOnServer";
    public static final String OCSSW_DELETE_FILES_ON_SERVER_PROPERTY = "seadas.ocssw.deleteFilesOnServer";
    public static final String OS_64BIT_ARCHITECTURE = "_64";
    public static final String OS_32BIT_ARCHITECTURE = "_32";

    public static final String SEADAS_VERSION_PROPERTY = "seadas.version";
    public static final String SEADAS_LOG_DIR_PROPERTY = "seadas.log.dir";
    public static final String OCSSW_LOCATION_PROPERTY = "seadas.ocssw.location";
    public static final String OCSSW_LOCATION_LOCAL = "local";
    public static final String OCSSW_LOCATION_VIRTUAL_MACHINE = "virtualMachine";
    public static final String OCSSW_LOCATION_REMOTE_SERVER = "remoteServer";
    public static final String OCSSW_PROCESS_INPUT_STREAM_PORT = "seadas.ocssw.processInputStreamPort";
    public static final String OCSSW_PROCESS_ERROR_STREAM_PORT = "seadas.ocssw.processErrorStreamPort";
    private static final Pattern PATTERN = Pattern.compile("^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");

    public static final String OCSSW_SCRIPTS_DIR_SUFFIX = "bin";
    public static final String OCSSW_DATA_DIR_SUFFIX = "share";
    public static final String OCSSW_BIN_DIR_SUFFIX = "bin";
    public static final String OCSSW_SRC_DIR_NAME = "ocssw_src";

    public static final String OCSSW_INSTALLER_PROGRAM_NAME = "install_ocssw";
    public static final String OCSSW_RUNNER_SCRIPT = "ocssw_runner";
    public static final String OCSSW_SEADAS_INFO_PROGRAM_NAME = "seadas_info";

    public static final String VIRTUAL_MACHINE_SERVER_API = "localhost";

    private static OCSSWInfo ocsswInfo = null;

    private static String sessionId = null;

    private int processInputStreamPort;
    private int processErrorStreamPort;

    private static boolean ocsswServerUp;
    private static boolean ocsswExist;
    private String ocsswRoot;
    private String ocsswDataDirPath;
    private String ocsswScriptsDirPath;
    private String ocsswInstallerScriptPath;
    private String ocsswRunnerScriptPath;
    private String ocsswBinDirPath;

    private String ocsswLocation;
    private String logDirPath;
    private String resourceBaseUri;
    private String ocsswTag;

    private static String seadasVersion;

    public static String getSessionId() {
        return sessionId;
    }

    public static void setSessionId(String sessionId) {
        OCSSWInfo.sessionId = sessionId;
    }

    public static String getSeadasVersion() {
        return seadasVersion;
    }

    public static void setSeadasVersion(String seadasVersion) {
        OCSSWInfo.seadasVersion = seadasVersion;
    }

    public boolean isOcsswServerUp() {
        return ocsswServerUp;
    }

    public void setOcsswServerUp(boolean ocsswServerUp) {
        OCSSWInfo.ocsswServerUp = ocsswServerUp;
    }

    public String getOcsswLocation() {
        return ocsswLocation;
    }

    private void setOcsswLocation(String location) {
        ocsswLocation = location;
    }

    private String clientId;
    String sharedDirPath;

    final Preferences preferences;

    private OCSSWInfo() {
        preferences = Config.instance("seadas").load().preferences();
        if (preferences != null ) {
            logDirPath = preferences.get(SEADAS_LOG_DIR_PROPERTY, System.getProperty("user.dir"));
            File logDir = new File(getLogDirPath());
            if (!logDir.exists()) {
                try {
                    Files.createDirectory(Paths.get(logDirPath));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            seadasVersion = preferences.get(SEADAS_VERSION_PROPERTY, null);
        }
    }

    public static OCSSWInfo getInstance() {
        if (ocsswInfo == null) {
            ocsswInfo = new OCSSWInfo();
            ocsswInfo.detectOcssw();
        }

        return ocsswInfo;
    }

    public static OCSSWInfo updateOCSSWInfo() {
        ocsswInfo = new OCSSWInfo();
        ocsswInfo.detectOcssw();
        return ocsswInfo;
    }

    public int getProcessInputStreamPort() {
        return processInputStreamPort;
    }


    public int getProcessErrorStreamPort() {
        return processErrorStreamPort;
    }


    public boolean isOCSSWExist() {
        return ocsswExist;
    }

    public void detectOcssw() {

        Date date = new Date();
        //System.out.println(sdf.format(date));
        sessionId = date.toString();
        String ocsswLocationPropertyValue = preferences.get(OCSSW_LOCATION_PROPERTY, null);
        if (ocsswLocationPropertyValue == null) {
            return;
        }

        if(ocsswLocationPropertyValue.equals(OCSSW_LOCATION_REMOTE_SERVER)) {
            ocsswLocationPropertyValue = preferences.get(SEADAS_OCSSW_SERVER_ADDRESS_PROPERTY, null);
        }

        setOcsswLocation(null);

        boolean isValidOcsswPropertyValue = isValidOcsswLocationProperty(ocsswLocationPropertyValue);

        if ((ocsswLocationPropertyValue.equalsIgnoreCase(OCSSW_LOCATION_LOCAL) && OsUtils.getOperatingSystemType() != OsUtils.OSType.Windows)
                || (!isValidOcsswPropertyValue && (OsUtils.getOperatingSystemType() == OsUtils.OSType.Linux || OsUtils.getOperatingSystemType() == OsUtils.OSType.MacOS))) {
            setOcsswLocation(OCSSW_LOCATION_LOCAL);
            initializeLocalOCSSW();
        } else {
            if (ocsswLocationPropertyValue.equalsIgnoreCase(OCSSW_LOCATION_VIRTUAL_MACHINE)) {
                setOcsswLocation(OCSSW_LOCATION_VIRTUAL_MACHINE);
                initializeRemoteOCSSW(VIRTUAL_MACHINE_SERVER_API);

            } else if (validate(ocsswLocationPropertyValue)) {
                setOcsswLocation(OCSSW_LOCATION_REMOTE_SERVER);
                initializeRemoteOCSSW(ocsswLocationPropertyValue);
            }
        }

        if (ocsswLocationPropertyValue == null) {
            Dialogs.showError("Remote OCSSW Initialization", "Please provide OCSSW server location in $HOME/.seadas/etc/seadas.properties");
            return;
        }

    }


    public static void displayRemoteServerDownMessage(){
        JOptionPane.showMessageDialog(new JOptionPane(), "Remote server is down. OCSSW is not accessible. Please start OCSSW remote server.",
                "OCSSW Initialization Warning",
                JOptionPane.WARNING_MESSAGE);
    }

    /**
     * OCSSW_LOCATION_PROPERTY takes only one of the following three values: local, virtualMachine, IP address of a remote server
     *
     * @return
     */
    private boolean isValidOcsswLocationProperty(String ocsswLocationPropertyValue) {
        if (ocsswLocationPropertyValue.equalsIgnoreCase(OCSSW_LOCATION_LOCAL)
                || ocsswLocationPropertyValue.equalsIgnoreCase(OCSSW_LOCATION_VIRTUAL_MACHINE)
                || validate(ocsswLocationPropertyValue)) {
            return true;
        }
        return false;
    }

    public boolean validate(final String ip) {
        return PATTERN.matcher(ip).matches();
    }

    public void initializeLocalOCSSW() {
        ocsswServerUp = true;

        final Preferences preferences = Config.instance("seadas").load().preferences();

        String ocsswRootString = preferences.get(SEADAS_OCSSW_ROOT_PROPERTY, null);

        if (ocsswRootString == null) {
            final Preferences preferencesSnap = Config.instance().load().preferences();

            ocsswRootString = preferencesSnap.get(SEADAS_OCSSW_ROOT_PROPERTY, null);
        }
        String ocsswRoot1 = "$" + SEADAS_OCSSW_ROOT_ENV;
        String ocsswRoot2 = "${" + SEADAS_OCSSW_ROOT_ENV + "}";
        if (ocsswRootString != null &&
                (ocsswRootString.equals(ocsswRoot1) || ocsswRootString.equals(ocsswRoot2))) {
            ocsswRootString = System.getenv(SEADAS_OCSSW_ROOT_ENV);
            if (ocsswRootString == null) {
                ocsswRootString = " ";
                // todo  open a popup warning
            }
        } else if (ocsswRootString == null ) {
            ocsswRootString = System.getenv(SEADAS_OCSSW_ROOT_ENV);
        }

        // todo This appears to remove this pattern at beginning of string, why is this check needed?
//        if (ocsswRootString != null && ocsswRootString.startsWith("$")) {
//            ocsswRootString = System.getProperty(ocsswRootString.substring(ocsswRootString.indexOf("{") + 1, ocsswRootString.indexOf("}"))) + ocsswRootString.substring(ocsswRootString.indexOf("}") + 1);
//        }

        if (ocsswRootString == null || ocsswRootString.length() == 0) {
            // File ocsswRootDir = new File(SystemUtils.getApplicationHomeDir() + File.separator + "ocssw");
            File ocsswRootDir = new File(SystemUtils.getApplicationHomeDir(), "ocssw");
            ocsswRootString = ocsswRootDir.getAbsolutePath();
        }


        if (ocsswRootString != null && ocsswRootString.length() > 0) {
//            final File dir = new File(ocsswRootString + File.separator + OCSSW_SCRIPTS_DIR_SUFFIX);
            final File dir = new File(ocsswRootString, OCSSW_SCRIPTS_DIR_SUFFIX);
            //SeadasLogger.getLogger().info("server ocssw root path: " + dir.getAbsoluteFile());
            if (dir.isDirectory()) {
                ocsswExist = true;
            }
        }

        ocsswRoot = ocsswRootString;
        ocsswScriptsDirPath = ocsswRoot + File.separator + OCSSW_SCRIPTS_DIR_SUFFIX;
        ocsswDataDirPath = ocsswRoot + File.separator + OCSSW_DATA_DIR_SUFFIX;
        ocsswInstallerScriptPath = ocsswScriptsDirPath + System.getProperty("file.separator") + OCSSW_INSTALLER_PROGRAM_NAME;
        ocsswRunnerScriptPath = ocsswScriptsDirPath + System.getProperty("file.separator") + OCSSW_RUNNER_SCRIPT;
        ocsswBinDirPath = ocsswRoot + System.getProperty("file.separator") + OCSSW_BIN_DIR_SUFFIX;
    }

    private boolean initializeRemoteOCSSW(String serverAPI) {
        ocsswServerUp = false;
        ocsswExist = false;
        final String BASE_URI_PORT_NUMBER_PROPERTY = "seadas.ocssw.port";
        final String OCSSW_REST_SERVICES_CONTEXT_PATH = "ocsswws";
        String baseUriPortNumber = preferences.get(BASE_URI_PORT_NUMBER_PROPERTY, "6400");
        resourceBaseUri = "http://" + serverAPI + ":" + baseUriPortNumber + "/" + OCSSW_REST_SERVICES_CONTEXT_PATH + "/";
        //System.out.println("server URL:" + resourceBaseUri);
        final ClientConfig clientConfig = new ClientConfig();
        clientConfig.register(MultiPartFeature.class);
        clientConfig.register(JsonProcessingFeature.class).property(JsonGenerator.PRETTY_PRINTING, true);
        Client c = ClientBuilder.newClient(clientConfig);
        WebTarget target = c.target(resourceBaseUri);
        JsonObject jsonObject = null;
        SeadasToolboxVersion seadasToolboxVersion = new SeadasToolboxVersion();
        ocsswTag = seadasToolboxVersion.getLatestOCSSWTagForInstalledRelease();
        if (ocsswTag == null) {
            ocsswTag = SEADAS_OCSSW_TAG_DEFAULT_VALUE;
        }

        //System.out.println("ocssw tag = " + ocsswTag);
        try {
            jsonObject = target.path("ocssw").path("ocsswInfo").path(ocsswTag).request(MediaType.APPLICATION_JSON_TYPE).get(JsonObject.class);
        } catch (Exception e) {
            e.printStackTrace();
            writeException(e);
            return false;
        }
        if (jsonObject != null) {
            ocsswServerUp = true;
            ocsswExist = jsonObject.getBoolean("ocsswExists");
            ocsswRoot = jsonObject.getString("ocsswRoot");
            ocsswDataDirPath = jsonObject.getString("ocsswDataDirPath");
            ocsswInstallerScriptPath = jsonObject.getString("ocsswInstallerScriptPath");
            ocsswRunnerScriptPath = jsonObject.getString("ocsswRunnerScriptPath");
            ocsswScriptsDirPath = jsonObject.getString("ocsswScriptsDirPath");
            //todo decide what will happen when sharedDirPath is null
            sharedDirPath = preferences.get(OCSSW_VM_SERVER_SHARED_DIR_PROPERTY, null);
            //if ( sharedDirPath == null ) {
            clientId = preferences.get(SEADAS_CLIENT_ID_PROPERTY, System.getProperty("user.name"));
            String deleteFilesOnServer = preferences.get(OCSSW_DELETE_FILES_ON_SERVER_PROPERTY, "true");
            Response response = target.path("ocssw").path("manageClientWorkingDirectory").path(clientId).request().put(Entity.entity(deleteFilesOnServer, MediaType.TEXT_PLAIN_TYPE));
            // }
            processInputStreamPort = new Integer(preferences.get(OCSSW_PROCESS_INPUT_STREAM_PORT, "6402")).intValue();
            processErrorStreamPort = new Integer(preferences.get(OCSSW_PROCESS_ERROR_STREAM_PORT, "6403")).intValue();
        }
        //System.out.println("ocsswExist = " + ocsswExist);
        return ocsswExist;
    }

    private void writeException(Exception e) {
        FileWriter fileWriter = null;
        try {

            fileWriter = new FileWriter(new File(logDirPath + File.separator + this.getClass().getName() + "_" + e.getClass().getName() + ".log"));
            fileWriter.write(e.getMessage());
            if (fileWriter != null) {
                fileWriter.close();
            }
        } catch (Exception exc) {
            JOptionPane.showMessageDialog(new JOptionPane(), "Log file directory is not accessible to write.",
                    "OCSSW Initialization Warning",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    public static String getOSName() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (System.getProperty("os.arch").indexOf("64") != -1) {
            return osName + OS_64BIT_ARCHITECTURE;
        } else {
            return osName + OS_32BIT_ARCHITECTURE;
        }
    }

    public String getResourceBaseUri() {
        return resourceBaseUri;
    }


    public String getOcsswRoot() {
        return ocsswRoot;
    }

    public void setOcsswRoot(String ocsswRootNewValue) {
        ocsswRoot = ocsswRootNewValue;
    }

    public String getOcsswDataDirPath() {
        return ocsswDataDirPath;
    }

    public String getOcsswScriptsDirPath() {
        return ocsswScriptsDirPath;
    }

    public String getOcsswInstallerScriptPath() {
        return ocsswInstallerScriptPath;
    }

    public String getOcsswRunnerScriptPath() {
        return ocsswRunnerScriptPath;
    }

    public String getOcsswBinDirPath() {
        return ocsswBinDirPath;
    }

    public String getLogDirPath() {
        return logDirPath;
    }

    public void setLogDirPath(String logDirPath) {
        this.logDirPath = logDirPath;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getContextProperty(String key, String defaultValue) {
        return preferences.get(key, defaultValue);
    }

    public String getOcsswDebugInfo(){
        return preferences.get(SEADAS_OCSSW_DEBUG, SEADAS_OCSSW_DEBUG_DEFAULT_VALUE);
    }
}
