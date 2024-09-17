package gov.nasa.gsfc.seadas.processing.common;

import com.bc.ceres.core.runtime.Version;
import gov.nasa.gsfc.seadas.processing.ocssw.OCSSWConfigData;
import gov.nasa.gsfc.seadas.processing.ocssw.OCSSWInfo;
import gov.nasa.gsfc.seadas.processing.ocssw.OCSSWInfoGUI;
import org.esa.snap.runtime.Config;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openide.modules.ModuleInfo;
import org.openide.modules.Modules;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static gov.nasa.gsfc.seadas.processing.ocssw.OCSSWConfigData.SEADAS_OCSSW_TAG_PROPERTY;
import static gov.nasa.gsfc.seadas.processing.ocssw.OCSSWConfigData.SEADAS_OCSSW_ROOT_PROPERTY;
import static gov.nasa.gsfc.seadas.processing.ocssw.OCSSWInfo.OCSSW_SEADAS_INFO_PROGRAM_NAME;

// Purpose: returns installation info/status on SeaDAS Toolbox version and OCSSW tag version.

public class SeadasToolboxVersion {

   static String SEADAS_TOOLBOX_VERSION_URL = "https://seadas.gsfc.nasa.gov/downloads/SEADAS_TOOLBOX_VERSION.txt";

    public static enum SeadasToolboxRelease {
        INSTALLED_RELEASE,
        LATEST_RELEASE
    }

    ArrayList<String> OCSSWTagList;



    public static boolean isLatestSeadasToolboxVersion() {
        final Version remoteVersion = getSeadasToolboxLatestVersion();
        final Version localVersion = getSeadasToolboxInstalledVersion();
//        System.out.println("remoteVersion=" + remoteVersion);
//        System.out.println("localVersion=" + localVersion);

        if (remoteVersion != null && localVersion != null) {
            return localVersion.compareTo(remoteVersion) >= 0;
        } else {
            return true;
        }
    }

    public static Version getSeadasToolboxInstalledVersion() {
        ModuleInfo seadasProcessingModuleInfo = Modules.getDefault().ownerOf(OCSSWInfoGUI.class);
        String seadasToolboxVersion = String.valueOf(seadasProcessingModuleInfo.getSpecificationVersion());

        return  Version.parseVersion(seadasToolboxVersion.toUpperCase());
    }

    public String getSeadasToolboxInstalledVersionString() {
        return String.valueOf(getSeadasToolboxInstalledVersion());
    }


    public static Version getSeadasToolboxLatestVersion() {
        try {
            String seadasToolBoxVerionUrlString = SEADAS_TOOLBOX_VERSION_URL;
            return readVersionFromStream(new URL(seadasToolBoxVerionUrlString).openStream());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getSeadasToolboxLatestVersionString() {
        return String.valueOf(getSeadasToolboxLatestVersion());
    }



    private static Version readVersionFromStream(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            line = reader.readLine();
            if (line != null) {
                return Version.parseVersion(line.toUpperCase());
            }
        }
        return null;
    }


    public  ArrayList<String> getOCSSWTagListForInstalledRelease() {
        getOCSSWTagList(SeadasToolboxRelease.INSTALLED_RELEASE);
        return OCSSWTagList;
    }

    public  String getLatestOCSSWTagForInstalledRelease() {
        getOCSSWTagList(SeadasToolboxRelease.INSTALLED_RELEASE);

        if (OCSSWTagList != null && OCSSWTagList.size() > 0) {
            String latestTag = OCSSWTagList.get(0);

            for (String tag: OCSSWTagList) {
                if (compareOcsswTags(latestTag, tag)) {
                    latestTag = tag;
                }
            }
            return latestTag;
        } else {
            return OCSSWConfigData.SEADAS_OCSSW_TAG_DEFAULT_VALUE;
        }
    }

    public boolean compareOcsswTags(String latestTag, String tag) {

        boolean latestTagValid = false;
        boolean tagValid = false;
        boolean tagIsNewer = false;

        String vTagPatternString = "^V\\d\\d\\d\\d.\\d+$";
        Pattern vTagPattern = Pattern.compile(vTagPatternString);

        int latestTagPrefixValue = -1;
        int tagPrefixValue = -1;
        int latestTagSuffixValue = -1;
        int tagSuffixValue = -1;

        if (latestTag != null) {
            Matcher vMatcher = vTagPattern.matcher(latestTag);
            if (vMatcher != null && vMatcher.find()) {
                String latestTagPrefix = latestTag.substring(1,5);
                String latestTagSuffix = latestTag.substring(6);

                try {
                    latestTagPrefixValue = Integer.valueOf(latestTagPrefix);
                    latestTagSuffixValue = Integer.valueOf(latestTagSuffix);

                    latestTagValid = true;
                } catch(Exception e) {
                }
            }
        }

        if (tag != null) {
            Matcher vMatcher = vTagPattern.matcher(tag);
            if (vMatcher != null && vMatcher.find()) {
                String tagPrefix = tag.substring(1,5);
                String tagSuffix = tag.substring(6);

                try {
                    tagPrefixValue = Integer.valueOf(tagPrefix);
                    tagSuffixValue = Integer.valueOf(tagSuffix);

                    tagValid = true;
                } catch(Exception e) {
                }
            }
        }

        if (latestTagValid && tagValid) {

            if (tagPrefixValue > latestTagPrefixValue) {
                tagIsNewer = true;
            } else if (tagPrefixValue == latestTagPrefixValue) {
                if (tagSuffixValue > latestTagSuffixValue) {
                    tagIsNewer = true;
                } else {
                    tagIsNewer = false;
                }
            } else {
                tagIsNewer = false;
            }

        } else if (tagValid) {
            tagIsNewer = true;
        } else {
            tagIsNewer = false;
        }

        return tagIsNewer;
    }


    public  ArrayList<String> getOCSSWTagListForLatestRelease() {
        getOCSSWTagList(SeadasToolboxRelease.LATEST_RELEASE);
        return OCSSWTagList;
    }

    public  String getLatestOCSSWTagForLatestRelease() {
        getOCSSWTagList(SeadasToolboxRelease.LATEST_RELEASE);

        if (OCSSWTagList != null && OCSSWTagList.size() > 0) {
            return OCSSWTagList.get(0);
        } else {
            return null;
        }
    }



    private  ArrayList<String> getOCSSWTagList(SeadasToolboxRelease id) {
        OCSSWTagList = new ArrayList<>();

        JSONParser jsonParser = new JSONParser();
        try {

            URL tagsURL = new URL("https://oceandata.sci.gsfc.nasa.gov/manifest/seadasVersions.json");
            URLConnection tagsConnection = tagsURL.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(tagsConnection.getInputStream()));

            //Read JSON file
            Object obj = jsonParser.parse(in);

            JSONArray validSeaDASTags = (JSONArray) obj;
            //System.out.println(validSeaDASTags);

            //Iterate over seadas tag array
            validSeaDASTags.forEach(tagObject -> parseValidSeaDASTagObject((JSONObject) tagObject, id));
            in.close();

        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return OCSSWTagList;
    }

    private void parseValidSeaDASTagObject(JSONObject tagObject, SeadasToolboxRelease id) {
        String seadasToolboxVersion;
        if (id == SeadasToolboxRelease.LATEST_RELEASE) {
            seadasToolboxVersion = getSeadasToolboxLatestVersionString();
        } else {
            seadasToolboxVersion = getSeadasToolboxInstalledVersionString();

        }

        String seadasToolboxVersionJson = (String) tagObject.get("seadas");

        if (seadasToolboxVersionJson.equals(seadasToolboxVersion)) {
            //Get corresponding ocssw tags for seadas
            JSONArray ocsswTags = (JSONArray) tagObject.get("ocssw");
            if (ocsswTags != null) {
                for (int i = 0; i < ocsswTags.size(); i++) {
                    try {
                        OCSSWTagList.add((String) ocsswTags.get(i));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }


    // todo this could be retrieved instead by an ocssw call
    public String getInstalledOCSSWTag() {
        return getInstalledOCSSWTagGUI();
    }


    public String getInstalledOCSSWTagGUI() {
//        final Preferences preferences = Config.instance("seadas").load().preferences();
//        String ocsswroot = preferences.get(SEADAS_OCSSW_ROOT_PROPERTY, null);
//        String[] command = {OCSSWInfo.getInstance().getOcsswRunnerScriptPath(), " --ocsswroot ", ocsswroot, "install_ocssw"};
        String[] command = {OCSSWInfo.getInstance().getOcsswRunnerScriptPath(), " --ocsswroot ", OCSSWInfo.getInstance().getOcsswRoot(), "install_ocssw", "--installed_tag"};

        System.out.println("command=" + command);
        String tag = null;

        try {

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            String line;

            while ((line = reader.readLine()) != null && tag == null) {
                if (line != null && line.contains("installedTag")) {
                    String[] tagLine = line.split("=");
                    if (tagLine.length == 2) {
                        tag = tagLine[1].trim();
                    }
                }
            }

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
            String warning = "  WARNING!! Exception occurred and could not retrieve tag from command \'" + command.toString() + "\' failed";
            e.printStackTrace();
        }

        return tag;

//        return preferences.get(SEADAS_OCSSW_TAG_PROPERTY, null);
    }


}
