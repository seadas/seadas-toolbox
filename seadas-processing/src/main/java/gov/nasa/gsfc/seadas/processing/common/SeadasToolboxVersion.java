package gov.nasa.gsfc.seadas.processing.common;

import com.bc.ceres.core.runtime.Version;
import gov.nasa.gsfc.seadas.processing.ocssw.OCSSWInfoGUI;
import org.esa.snap.runtime.Config;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openide.modules.ModuleInfo;
import org.openide.modules.Modules;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.prefs.Preferences;

import static gov.nasa.gsfc.seadas.processing.ocssw.OCSSWConfigData.SEADAS_OCSSW_TAG_PROPERTY;

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
            return OCSSWTagList.get(0);
        } else {
            return null;
        }
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
        final Preferences preferences = Config.instance("seadas").load().preferences();
        return preferences.get(SEADAS_OCSSW_TAG_PROPERTY, null);
    }


}