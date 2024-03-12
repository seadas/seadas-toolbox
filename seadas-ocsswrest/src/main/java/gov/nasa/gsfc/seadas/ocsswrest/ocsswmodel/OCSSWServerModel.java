package gov.nasa.gsfc.seadas.ocsswrest.ocsswmodel;

import gov.nasa.gsfc.seadas.ocsswrest.utilities.MissionInfo;
import gov.nasa.gsfc.seadas.ocsswrest.utilities.ServerSideFileUtilities;

import java.io.File;
import java.nio.file.Files;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import static gov.nasa.gsfc.seadas.ocsswrest.utilities.OCSSWInfo.*;
import static java.nio.file.StandardCopyOption.*;

/**
 * Created by aabduraz on 3/27/17.
 */
public class OCSSWServerModel {

    public static final String OS_64BIT_ARCHITECTURE = "_64";
    public static final String OS_32BIT_ARCHITECTURE = "_32";

    private static String NEXT_LEVEL_NAME_FINDER_PROGRAM_NAME = "get_output_name";
    private static String NEXT_LEVEL_FILE_NAME_TOKEN = "Output Name:";
    public static final String OBPG_FILE_TYPE_PROGRAM_NAME = "obpg_file_type";

    public static final String OCSSW_SCRIPTS_DIR_NAME = "bin";
    public static final String OCSSW_DATA_DIR_NAME = "share";
    public static final String OCSSW_BIN_DIR_NAME = "bin";
    public static final String OCSSW_SRC_DIR_NAME = "ocssw_src";
    public static final String OCSSW_COMMON_DIR_NAME = "common";
    public static final String OCSSW_VIIRS_DEM_NAME = "share" + File.separator + "viirs" + File.separator + "dem";

    public static String OCSSW_INSTALLER_PROGRAM = "install_ocssw";
    public static String OCSSW_RUNNER_SCRIPT = "ocssw_runner";

    public static String TMP_OCSSW_INSTALLER_PROGRAM_PATH = (new File(System.getProperty("java.io.tmpdir"), "install_ocssw")).getPath();

    private static boolean ocsswInstalScriptDownloadSuccessful = false;

    public static String getOcsswDataDirPath() {
        return ocsswDataDirPath;
    }


    public static String getOcsswScriptsDirPath() {
        return ocsswScriptsDirPath;
    }

    public static String getOcsswInstallerScriptPath() {
        return ocsswInstallerScriptPath;
    }

    public static String getOcsswRunnerScriptPath() {
        return ocsswRunnerScriptPath;
    }

    public static String getOcsswBinDirPath() {
        return ocsswBinDirPath;
    }

    public static String getOcsswSrcDirPath() {
        return ocsswSrcDirPath;
    }

    public static String getOcsswViirsDemPath() {
        return ocsswViirsDemPath;
    }

    public static String getSeadasVersion() {
        return seadasVersion;
    }

    public static void setSeadasVersion(String seadasVersion) {
        OCSSWServerModel.seadasVersion = seadasVersion;
    }

    public enum ExtractorPrograms {
        L1AEXTRACT_MODIS("l1aextract_modis"),
        L1AEXTRACT_SEAWIFS("l1extract_seawifs"),
        L1AEXTRACT__VIIRS("l1aextract_viirs"),
        L2EXTRACT("l2extract");

        String extractorProgramName;

        ExtractorPrograms(String programName) {
            extractorProgramName = programName;
        }

        public String getExtractorProgramName() {
            return extractorProgramName;
        }
    }

    final String L1AEXTRACT_MODIS = "l1aextract_modis",
            L1AEXTRACT_MODIS_XML_FILE = "l1aextract_modis.xml",
            L1AEXTRACT_SEAWIFS = "l1aextract_seawifs",
            L1AEXTRACT_SEAWIFS_XML_FILE = "l1aextract_seawifs.xml",
            L1AEXTRACT_VIIRS = "l1aextract_viirs",
            L1AEXTRACT_VIIRS_XML_FILE = "l1aextract_viirs.xml",
            L2EXTRACT = "l2extract",
            L2EXTRACT_XML_FILE = "l2extract.xml";

    private static boolean ocsswExist;
    private static String ocsswRoot;
    static String ocsswDataDirPath;
    private static String ocsswScriptsDirPath;
    private static String ocsswInstallerScriptPath;
    private static String ocsswRunnerScriptPath;
    private static String ocsswBinDirPath;
    private static String ocsswSrcDirPath;
    private static String ocsswViirsDemPath;

    private static String seadasVersion;

    static boolean isProgramValid;


    public OCSSWServerModel() {

        initiliaze();
        ocsswExist = isOCSSWExist();

    }

    public static void initiliaze() {
        String ocsswRootPath = System.getProperty("ocsswroot");
        if (ocsswRootPath != null) {
            final File dir = new File(ocsswRootPath + File.separator + OCSSW_SCRIPTS_DIR_NAME);
            System.out.println("server ocssw root path: " + dir.getAbsoluteFile());
            ocsswExist = dir.isDirectory();
            ocsswRoot = ocsswRootPath;
            ocsswScriptsDirPath = ocsswRoot + File.separator + OCSSW_SCRIPTS_DIR_NAME;
            ocsswDataDirPath = ocsswRoot + File.separator + OCSSW_DATA_DIR_NAME;
            ocsswBinDirPath = ocsswRoot + File.separator + OCSSW_BIN_DIR_NAME;
            ocsswSrcDirPath = ocsswRoot + File.separator + OCSSW_SRC_DIR_NAME;
            ocsswInstallerScriptPath = ocsswScriptsDirPath + File.separator + OCSSW_INSTALLER_PROGRAM;
            ocsswRunnerScriptPath = ocsswScriptsDirPath + File.separator + OCSSW_RUNNER_SCRIPT;
            ocsswViirsDemPath = ocsswRoot + File.separator + OCSSW_VIIRS_DEM_NAME;
            copyNetrcFile();
            downloadOCSSWInstaller();
        }
    }

    public static boolean isMissionDirExist(String missionName) {
        MissionInfo missionInfo = new MissionInfo(missionName);
        File dir = missionInfo.getSubsensorDirectory();
        if (dir == null) {
            dir = missionInfo.getDirectory();
        }
        if (dir != null) {
            return dir.isDirectory();
        }
        return false;
    }

    public static boolean isOCSSWExist() {
        return ocsswExist;
    }

    public static String getOcsswRoot() {
        return ocsswRoot;
    }


    /**
     * This method will validate the program name. Only programs exist in $OCSSWROOT/run/scripts and $OSSWROOT/run/bin/"os_name" can be executed on the server side.
     *
     * @param programName
     * @return true if programName is found in the $OCSSWROOT/run/scripts or $OSSWROOT/run/bin/"os_name" directories. Otherwise return false.
     */
    public static boolean isProgramValid(String programName) {
        //"extractor" is special, it can't be validated using the same logic for other programs.
        if (programName.equals("extractor")) {
            return true;
        }
        isProgramValid = false;
        File scriptsFolder = new File(ocsswScriptsDirPath);
        File[] listOfScripts = scriptsFolder.listFiles();
        File runFolder = new File(ocsswBinDirPath);
        File[] listOfPrograms = runFolder.listFiles();

        File[] executablePrograms = ServerSideFileUtilities.concatAll(listOfPrograms, listOfScripts);

        for (File file : executablePrograms) {
            if (file.isFile() && programName.equals(file.getName())) {
                isProgramValid = true;
                break;
            }
        }
        return isProgramValid;
    }

    public static boolean isOcsswInstalScriptDownloadSuccessful() {
        return ocsswInstalScriptDownloadSuccessful;
    }

    /**
     * This method downloads the ocssw installer program ocssw_install to a tmp directory
     *
     * @return
     */
    public static boolean downloadOCSSWInstaller() {

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
            ocsswInstalScriptDownloadSuccessful = true;

            //download install_ocssw
            website = new URL(OCSSW_BOOTSTRAP_URL);
            rbc = Channels.newChannel(website.openStream());
            fos = new FileOutputStream(TMP_OCSSW_BOOTSTRAP);
            fos.getChannel().transferFrom(rbc, 0, 1 << 24);
            fos.close();
            (new File(TMP_OCSSW_BOOTSTRAP)).setExecutable(true);


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
            System.out.println("URL for downloading install_ocssw is not correct!");
        } catch (FileNotFoundException fileNotFoundException) {
            System.out.println("ocssw installation script failed to download. \n" +
                    "Please check network connection or 'seadas.ocssw.root' variable in the 'seadas.config' file. \n" +
                    "possible cause of error: " + fileNotFoundException.getMessage());
        } catch (IOException ioe) {
            System.out.println("ocssw installation script failed to download. \n" +
                    "Please check network connection or 'seadas.ocssw.root' variable in the \"seadas.config\" file. \n" +
                    "possible cause of error: " + ioe.getLocalizedMessage());
        } finally {
            return ocsswInstalScriptDownloadSuccessful;
        }
    }

    public static void copyNetrcFile() {
        File targetFile = new File(System.getProperty("user.home") + File.separator + ".netrc");
        try {
            targetFile.createNewFile();
            Path targetPath = targetFile.toPath();
            File sourceFile = new File(System.getProperty("user.home") + File.separator + "seadasClientServerShared" + File.separator + ".netrc");
            Path sourcePath = sourceFile.toPath();
            System.out.println(".netrc in seadasClientServer " + sourceFile.exists());
            System.out.println(".netrc in home dir " + targetFile.exists());
            Files.copy(sourcePath, targetPath, REPLACE_EXISTING);
        } catch (java.io.IOException ioException) {
            ioException.printStackTrace();
        }
    }
}

