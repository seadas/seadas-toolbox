package gov.nasa.gsfc.seadas.processing.core;

import com.bc.ceres.core.ProgressMonitor;
import gov.nasa.gsfc.seadas.processing.common.XmlReader;
import gov.nasa.gsfc.seadas.processing.preferences.OCSSW_L2binController;
import gov.nasa.gsfc.seadas.processing.preferences.OCSSW_L3mapgenController;
import org.esa.snap.core.util.ResourceInstaller;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.rcp.util.Dialogs;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: Aynur Abdurazik (aabduraz)
 * Date: 3/19/12
 * Time: 8:59 AM
 * To change this template use File | Settings | File Templates.
 */
public class ParamUtils {

    private String OCDATAROOT = System.getenv("OCDATAROOT");

    public static final String PAR = "par";
    public static final String GEOFILE = "geofile";
    public static final String SPIXL = "spixl";
    public static final String EPIXL = "epixl";
    public static final String DPIXL = "dpixl";
    public static final String SLINE = "sline";
    public static final String ELINE = "eline";
    public static final String DLINE = "dline";
    public static final String NORTH = "north";
    public static final String SOUTH = "south";
    public static final String WEST = "west";
    public static final String EAST = "east";
    public static final String IFILE = "ifile";
    public static final String OFILE = "ofile";

    public static final String OPTION_NAME = "name";
    public static final String OPTION_TYPE = "type";

    public static final String XML_ELEMENT_HAS_GEO_FILE = "hasGeoFile";
    public static final String XML_ELEMENT_HAS_PAR_FILE = "hasParFile";

    public static final String NO_XML_FILE_SPECIFIED = "No XML file Specified";

    public final String INVALID_IFILE_EVENT = "INVALID_IFILE_EVENT";
    public final String PARFILE_CHANGE_EVENT = "PARFILE_CHANGE_EVENT";

    public final String WAVE_LIMITER_CHANGE_EVENT = "WAVE_LIMITER_EVENT";

    public final String DEFAULTS_CHANGED_EVENT = "DEFAULTS_CHANGED_EVENT";

    public final static String DEFAULT_PAR_FILE_NAME = "par";
    public final static String DEFAULT_PROGRESS_REGEX = "Processing scan .+?\\((\\d+) of (\\d+)\\)";

    private static int longestIFileNameLength;

    public enum nullValueOverrides {
        IFILE, OFILE, PAR, GEOFILE
    }

    public static Set<String> getPrimaryOptions(String parXMLFileName) {

        Set<String> primaryOptions = new HashSet<String>();
        XmlReader xmlReader = new XmlReader();
        InputStream paramStream = ParamUtils.class.getResourceAsStream(parXMLFileName);
        Element rootElement = xmlReader.parseAndGetRootElement(paramStream);
        NodeList optionNodelist = rootElement.getElementsByTagName("primaryOption");
        if (optionNodelist == null || optionNodelist.getLength() == 0) {
            //SeadasLogger.getLogger().warning("primaryOptions does not exist!");
            primaryOptions.add("ifile");
            primaryOptions.add("ofile");
            return primaryOptions;
        }
        for (int i = 0; i < optionNodelist.getLength(); i++) {
            Element optionElement = (Element) optionNodelist.item(i);
            String name = optionElement.getFirstChild().getNodeValue();
            primaryOptions.add(name);
        }
        return primaryOptions;
    }

    public static String getParFileOptionName(String parXMLFileName) {

        XmlReader xmlReader = new XmlReader();
        InputStream paramStream = ParamUtils.class.getResourceAsStream(parXMLFileName);
        Element rootElement = xmlReader.parseAndGetRootElement(paramStream);
        NodeList optionNodelist = rootElement.getElementsByTagName("parFileOptionName");
        if (optionNodelist == null || optionNodelist.getLength() == 0) {
            //SeadasLogger.getLogger().warning("par file option name is not specified in the xml file. 'par' is used as a default name.");
            return DEFAULT_PAR_FILE_NAME;
        }
        return optionNodelist.item(0).getFirstChild().getNodeValue();
    }

    public static String getProgressRegex(String parXMLFileName) {

        XmlReader xmlReader = new XmlReader();
        InputStream paramStream = ParamUtils.class.getResourceAsStream(parXMLFileName);
        Element rootElement = xmlReader.parseAndGetRootElement(paramStream);
        NodeList optionNodelist = rootElement.getElementsByTagName("progressRegex");
        if (optionNodelist == null || optionNodelist.getLength() == 0) {
            //SeadasLogger.getLogger().warning("progress meter regular expression is not specified in the xml file.");
            return DEFAULT_PROGRESS_REGEX;
        }
        return optionNodelist.item(0).getFirstChild().getNodeValue();
    }

    public static boolean getOptionStatus(String parXMLFileName, String elementName) {

        boolean optionStatus = false;
        XmlReader xmlReader = new XmlReader();
        InputStream paramStream = ParamUtils.class.getResourceAsStream(parXMLFileName);
        Element rootElement = xmlReader.parseAndGetRootElement(paramStream);
        NodeList optionNodelist = rootElement.getElementsByTagName(elementName);
        if (optionNodelist == null || optionNodelist.getLength() == 0) {
            //SeadasLogger.getLogger().warning(elementName + " exist: " + optionStatus);
            return optionStatus;
        }
        Element metaDataElement = (Element) optionNodelist.item(0);

        String name = metaDataElement.getTagName();
        //SeadasLogger.getLogger().fine("tag name: " + name);
        //   if (name.equals(elementName)) {
        optionStatus = Boolean.parseBoolean(metaDataElement.getFirstChild().getNodeValue());
        //SeadasLogger.getLogger().fine(name + " value = " + metaDataElement.getFirstChild().getNodeValue() + " " + optionStatus);
        //  }

        return optionStatus;
    }


    public static int getLongestIFileNameLength() {
        return longestIFileNameLength;
    }

    public static ArrayList<ParamInfo> computeParamList(String paramXmlFileName) {

        if (paramXmlFileName.equals(NO_XML_FILE_SPECIFIED)) {
            return getDefaultParamList();
        }

        final ArrayList<ParamInfo> paramList = new ArrayList<ParamInfo>();

        XmlReader xmlReader = new XmlReader();
        InputStream paramStream = ParamUtils.class.getResourceAsStream(paramXmlFileName);
        if (paramStream == null) {
            Dialogs.showError("XML file " + paramXmlFileName + " not found.");
            return null;
        }

        Element rootElement = xmlReader.parseAndGetRootElement(paramStream);
        if (rootElement == null) {
            Dialogs.showError("XML file " + paramXmlFileName + " root element not found.");
            return null;
        }
        NodeList optionNodelist = rootElement.getElementsByTagName("option");
        if (optionNodelist == null || optionNodelist.getLength() == 0) {
            return null;
        }

        longestIFileNameLength = 0;

        for (int i = 0; i < optionNodelist.getLength(); i++) {

            Element optionElement = (Element) optionNodelist.item(i);

            String name = XmlReader.getTextValue(optionElement, OPTION_NAME);
            debug("option name: " + name);
            String tmpType = XmlReader.getAttributeTextValue(optionElement, OPTION_TYPE);
            debug("option type: " + tmpType);

            ParamInfo.Type type = ParamInfo.Type.HELP;

            if (tmpType != null) {
                if (tmpType.toLowerCase().equals("boolean")) {
                    type = ParamInfo.Type.BOOLEAN;
                } else if (tmpType.toLowerCase().equals("int")) {
                    type = ParamInfo.Type.INT;
                } else if (tmpType.toLowerCase().equals("float")) {
                    type = ParamInfo.Type.FLOAT;
                } else if (tmpType.toLowerCase().equals("string")) {
                    type = ParamInfo.Type.STRING;
                } else if (tmpType.toLowerCase().equals("ifile")) {
                    type = ParamInfo.Type.IFILE;
                    if (name.length() > longestIFileNameLength) {
                        longestIFileNameLength = name.length();
                    }
                } else if (tmpType.toLowerCase().equals("ofile")) {
                    type = ParamInfo.Type.OFILE;
                } else if (tmpType.toLowerCase().equals("help")) {
                    type = ParamInfo.Type.HELP;
                } else if (tmpType.toLowerCase().equals("dir")) {
                    type = ParamInfo.Type.DIR;
                } else if (tmpType.toLowerCase().equals("flags")) {
                    type = ParamInfo.Type.FLAGS;
                } else if (tmpType.toLowerCase().equals("button")) {
                    type = ParamInfo.Type.BUTTON;
                }
            }

            String value = XmlReader.getTextValue(optionElement, "value");

            if (name != null) {
                String nullValueOverrides[] = {ParamUtils.IFILE, ParamUtils.OFILE, ParamUtils.PAR, ParamUtils.GEOFILE};
                for (String nullValueOverride : nullValueOverrides) {
                    if (name.equals(nullValueOverride)) {
                        value = ParamInfo.NULL_STRING;
                    }
                }
            }

            String description = XmlReader.getTextValue(optionElement, "description");
            String colSpan = XmlReader.getTextValue(optionElement, "colSpan");
            String source = XmlReader.getTextValue(optionElement, "source");
            String order = XmlReader.getTextValue(optionElement, "order");
            String usedAs = XmlReader.getTextValue(optionElement, "usedAs");
            String defaultValue = XmlReader.getTextValue(optionElement, "default");

            final String optionNameTmp = ParamUtils.removePreceedingDashes(name);

            if ("l2bin.xml".equals(paramXmlFileName)) {
                switch (optionNameTmp) {
                    case "flaguse":
                        if (!OCSSW_L2binController.getPreferenceFlaguseSelectorEnable()) {
                            // todo Possibly delete this whole block
//                            type = ParamInfo.Type.STRING;
                        }
                }
            }





            // set the value and the default to the current value from the XML file
            //ParamInfo paramInfo = (type.equals(ParamInfo.Type.OFILE)) ? new OFileParamInfo(name, value, type, value) : new ParamInfo(name, value, type, value);
            ParamInfo paramInfo = (type.equals(ParamInfo.Type.OFILE)) ? new OFileParamInfo(name, value, type, defaultValue) : new ParamInfo(name, value, type, defaultValue);
            paramInfo.setDescription(description);

            if (colSpan != null) {
                try {
                    int colSpanInt = Integer.parseInt(colSpan);
                    if (colSpanInt > 0) {
                        paramInfo.setColSpan(colSpanInt);
                    }
                } catch (Exception e) {
                    System.out.println("ERROR: colSpan not an integer for param: " + name + "in xml file: " + paramXmlFileName);
                }
            } else {
                paramInfo.setColSpan(1);
            }


            paramInfo.setSource(source);

            if (order != null) {
                try {
                    int orderInt = Integer.parseInt(order);
                    if (orderInt >= 0) {
                        paramInfo.setOrder(orderInt);
                    }
                } catch (Exception e) {
                    System.out.println("ERROR: order not an integer for param: " + name + "in xml file: " + paramXmlFileName);
                }
            }

            if (usedAs != null) {
                paramInfo.setUsedAs(usedAs);
            }

            NodeList validValueNodelist = optionElement.getElementsByTagName("validValue");

            // todo


            final String optionName = ParamUtils.removePreceedingDashes(paramInfo.getName());

            if (validValueNodelist != null && validValueNodelist.getLength() > 0) {
                for (int j = 0; j < validValueNodelist.getLength(); j++) {

                    Element validValueElement = (Element) validValueNodelist.item(j);

                    String validValueValue = XmlReader.getTextValue(validValueElement, "value");
                    String validValueDescription = XmlReader.getTextValue(validValueElement, "description");

                    ParamValidValueInfo paramValidValueInfo = new ParamValidValueInfo(validValueValue);

                    paramValidValueInfo.setDescription(validValueDescription);
                    paramInfo.addValidValueInfo(paramValidValueInfo);
                }

            }

            if ("l3mapgen.xml".equals(paramXmlFileName)) {
                switch (optionName) {
                    case "projection":
                        addValidValueToParamInfo(OCSSW_L3mapgenController.getPreferenceFAV1Projection(),
                                OCSSW_L3mapgenController.getPreferenceFAV1ProjectionDescription(),
                                paramInfo);
                        addValidValueToParamInfo(OCSSW_L3mapgenController.getPreferenceFAV2Projection(),
                                OCSSW_L3mapgenController.getPreferenceFAV2ProjectionDescription(),
                                paramInfo);
                        addValidValueToParamInfo(OCSSW_L3mapgenController.getPreferenceFAV3Projection(),
                                OCSSW_L3mapgenController.getPreferenceFAV3ProjectionDescription(),
                                paramInfo);
                }
            }


            if ("l2bin.xml".equals(paramXmlFileName)) {
                switch (optionName) {
                    case "flaguse":
                        String additionalFlags = OCSSW_L2binController.getPreferenceFlaguseAdditionalFlags();
                        if (additionalFlags != null && additionalFlags.length() > 0) {
                            String[] values = additionalFlags.split("[,\\s]");
                            for(String flag : values) {
                                addValidValueToParamInfo(flag, flag, paramInfo);
                            }
                        }
//
//                        addFavoriteProjection("~GEOREGION",
//                                "not GEOREGION",
//                                paramInfo);
//                        addFavoriteProjection("~LAND",
//                                "not LAND",
//                                paramInfo);
//                        addFavoriteProjection("~COASTZ",
//                                "not COASTZ",
//                                paramInfo);
                }
            }



//            Path getColorSchemesAuxDir = SystemUtils.getAuxDataPath().resolve(ColorManipulationDefaults.DIR_NAME_COLOR_SCHEMES);
//            if (getColorSchemesAuxDir != null) {
//                this.colorSchemesAuxDir = getColorSchemesAuxDir.toFile();
//                if (!colorSchemesAuxDir.exists()) {
//                    return;
//                }
//            } else {
//
//                colorSchemeLookupUserFile = new File(this.colorSchemesAuxDir, ColorManipulationDefaults.COLOR_SCHEME_LOOKUP_USER_FILENAME);
//
//

            if ("l3mapgen.xml".equals(paramXmlFileName)) {
                switch (optionName) {
                    case "product":
                        paramInfo.setValue(OCSSW_L3mapgenController.getPreferenceProduct());
                        break;
                    case "projection":
//                        getUserProjections();

//                        addFavoriteProjection(OCSSW_L3mapgenController.getPreferencePROJECTION_FAV1(),
//                                OCSSW_L3mapgenController.getPreferencePROJECTION_FAV1_NAME(),
//                                paramInfo);
//                        addFavoriteProjection(OCSSW_L3mapgenController.getPreferencePROJECTION_FAV2(),
//                                OCSSW_L3mapgenController.getPreferencePROJECTION_FAV2_NAME(),
//                                paramInfo);


//                        String fav1 = OCSSW_L3mapgenController.getPreferencePROJECTION_FAV1();
//                        if (fav1 != null && fav1.length() > 0) {
//                            ParamValidValueInfo paramValidValueInfo = new ParamValidValueInfo(fav1);
//                            String fav1Name = OCSSW_L3mapgenController.getPreferencePROJECTION_FAV1_NAME();
//                            if (fav1Name != null && fav1Name.length() > 0) {
//                                paramValidValueInfo.setDescription(fav1Name);
//                            }
//                        }
//
//                        paramInfo.addValidValueInfo(paramValidValueInfo);

                        paramInfo.setValue(OCSSW_L3mapgenController.getPreferenceProjection());
                        break;
                    case "resolution":
                        paramInfo.setValue(OCSSW_L3mapgenController.getPreferenceResolution());
                        break;
                    case "interp":
                        paramInfo.setValue(OCSSW_L3mapgenController.getPreferenceInterp());
                        break;
                    case "north":
                        paramInfo.setValue(OCSSW_L3mapgenController.getPreferenceNorth());
                        break;
                    case "south":
                        paramInfo.setValue(OCSSW_L3mapgenController.getPreferenceSouth());
                        break;
                    case "west":
                        paramInfo.setValue(OCSSW_L3mapgenController.getPreferenceWest());
                        break;
                    case "east":
                        paramInfo.setValue(OCSSW_L3mapgenController.getPreferenceEast());
                        break;
                }
            }


            if ("l2bin.xml".equals(paramXmlFileName)) {
                switch (optionName) {
                    case "l3bprod":
                        paramInfo.setValue(OCSSW_L2binController.getPreferenceL3bprod());
                        break;
                    case "prodtype":
                        paramInfo.setValue(OCSSW_L2binController.getPreferenceProdtype());
                        break;
                    case "resolution":
                        paramInfo.setValue(OCSSW_L2binController.getPreferenceResolution());
                        break;
                    case "area_weighting":
                        paramInfo.setValue(OCSSW_L2binController.getPreferenceAreaWeighting());
                        break;
                    case "flaguse":
                        paramInfo.setValue(OCSSW_L2binController.getPreferenceFlaguse());
                        break;
                    case "latnorth":
                        paramInfo.setValue(OCSSW_L2binController.getPreferenceLatnorth());
                        break;
                    case "latsouth":
                        paramInfo.setValue(OCSSW_L2binController.getPreferenceLatsouth());
                        break;
                    case "lonwest":
                        paramInfo.setValue(OCSSW_L2binController.getPreferenceLonwest());
                        break;
                    case "loneast":
                        paramInfo.setValue(OCSSW_L2binController.getPreferenceLoneast());
                        break;
                }
            }





            paramList.add(paramInfo);

        }

        return paramList;
    }



    private static void addValidValueToParamInfo(String fav, String favName, ParamInfo paramInfo) {
        if (fav != null && fav.length() > 0) {
            ParamValidValueInfo paramValidValueInfo = new ParamValidValueInfo(fav);
            if (favName != null && favName.length() > 0) {
                paramValidValueInfo.setDescription(favName);
            }
            if (paramValidValueInfo != null) {
                paramInfo.addValidValueInfo(paramValidValueInfo);
            }
        }
    }


    private static void getUserProjections() {

        HashMap<String, String> ociWavelengths = new HashMap<String, String>();

        String SENSOR_INFO = "sensor_info";
        String AUXDATA = "auxdata";
        String USER_PROJECTIONS_XML = "auxdata/user_projections.xml";


            File sensorInfoAuxDir = SystemUtils.getAuxDataPath().resolve(SENSOR_INFO).toFile();
            File user_projections_file = new File(sensorInfoAuxDir, USER_PROJECTIONS_XML);

            if (user_projections_file == null ||  !user_projections_file.exists()) {
                try {
                    Path auxdataDir = SystemUtils.getAuxDataPath().resolve(SENSOR_INFO);

                    Path sourceBasePath = ResourceInstaller.findModuleCodeBasePath(ParamUtils.class);
                    Path sourceDirPath = sourceBasePath.resolve("auxdata");
                    System.out.println("sourceDirPath=" + sourceDirPath);

                    final ResourceInstaller resourceInstaller = new ResourceInstaller(sourceDirPath, auxdataDir);

                    resourceInstaller.install(".*." + USER_PROJECTIONS_XML, ProgressMonitor.NULL);

                } catch (IOException e) {
                    System.out.println("ERROR");

//                    SnapApp.getDefault().handleError("Unable to install " + AUXDATA + "/" + SENSOR_INFO + "/" + USER_PROJECTIONS_XML, e);
                }
            }

//            if (sensorInfoAuxDir != null && sensorInfoAuxDir.exists()) {
//
//                if (ociBandPassFile != null && ociBandPassFile.exists()) {
//
//                    try (BufferedReader br = new BufferedReader(new FileReader(ociBandPassFile))) {
//                        String line;
//                        while ((line = br.readLine()) != null) {
//                            String[] values = line.split(",");
//                            if (values != null && values.length > 3) {
//                                ociWavelengths.put(values[1].trim(), values[2].trim());
//                            }
//                        }
//                    } catch (Exception e) {
//
//                    }
//                }
//            }


//        int spectralBandIndex = 0;
//        for (String name : product.getBandNames()) {
//            Band band = product.getBandAt(product.getBandIndex(name));
//            if (name.matches("\\w+_\\d{3,}")) {
//                String[] parts = name.split("_");
//                String wvlstr = parts[parts.length - 1].trim();
//                //Some bands have the wvl portion in the middle...
//                if (!wvlstr.matches("^\\d{3,}")) {
//                    wvlstr = parts[parts.length - 2].trim();
//                }
//
//                if (SeadasProductReader.Mission.OCI.toString().equals(sensor) &&
//                        (SeadasProductReader.ProcessingLevel.L2.toString().equals(processing_level) ||
//                                SeadasProductReader.ProcessingLevel.L3m.toString().equals(processing_level))) {
//                    wvlstr = getPaceOCIWavelengths(wvlstr, ociWavelengths);
//                }
//
//                final float wavelength = Float.parseFloat(wvlstr);
//                band.setSpectralWavelength(wavelength);
//                band.setSpectralBandIndex(spectralBandIndex++);
//            }



    }





    /**
     * Create a default array list with ifile, ofile,  spixl, epixl, sline, eline options
     *
     * @return
     */
    public static ArrayList<ParamInfo> getDefaultParamList() {
        ArrayList<ParamInfo> defaultParamList = new ArrayList<ParamInfo>();
        return defaultParamList;
    }

    static void debug(String debugMessage) {
        //System.out.println(debugMessage);
    }

    public static String removePreceedingDashes(String optionName) {
        return optionName.replaceAll("--", "");
    }


}
