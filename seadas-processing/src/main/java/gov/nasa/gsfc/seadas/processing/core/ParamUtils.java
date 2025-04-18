package gov.nasa.gsfc.seadas.processing.core;

import com.bc.ceres.core.ProgressMonitor;
import gov.nasa.gsfc.seadas.processing.common.XmlReader;
import gov.nasa.gsfc.seadas.processing.preferences.OCSSW_L2binController;
import gov.nasa.gsfc.seadas.processing.preferences.OCSSW_L3binController;
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


    public static String getElementString(String parXMLFileName, String elementName) {

        XmlReader xmlReader = new XmlReader();
        InputStream paramStream = ParamUtils.class.getResourceAsStream(parXMLFileName);
        Element rootElement = xmlReader.parseAndGetRootElement(paramStream);
        NodeList optionNodelist = rootElement.getElementsByTagName(elementName);

        if (optionNodelist != null && optionNodelist.item(0) != null && optionNodelist.item(0).getFirstChild() != null) {
            return optionNodelist.item(0).getFirstChild().getNodeValue();
        }

        return null;
    }


    public static int getElementInt(String parXMLFileName, String elementName) {

        String valueString = ParamUtils.getElementString(parXMLFileName, elementName);

        int valueInt = 0;

        try {
          valueInt = Integer.valueOf(valueString);
        } catch (Exception e) {

        }

        return valueInt;
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
            String putInBooleanPanel = XmlReader.getTextValue(optionElement, "putInBooleanPanel");
            String subPanelIndex = XmlReader.getTextValue(optionElement, "subPanelIndex");
            String source = XmlReader.getTextValue(optionElement, "source");
            String order = XmlReader.getTextValue(optionElement, "order");
            String usedAs = XmlReader.getTextValue(optionElement, "usedAs");
            String defaultValue = XmlReader.getTextValue(optionElement, "default");

            final String optionNameTmp = ParamUtils.removePreceedingDashes(name);

            if ("l2bin.xml".equals(paramXmlFileName)) {
                switch (optionNameTmp) {
                    case "flaguse":
                        if (!OCSSW_L2binController.getPreferenceFlaguseAutoFillEnable()) {
                            // todo Possibly delete this whole block
//                            type = ParamInfo.Type.STRING;
                        }
                }
            }





            // set the value and the default to the current value from the XML file
            //ParamInfo paramInfo = (type.equals(ParamInfo.Type.OFILE)) ? new OFileParamInfo(name, value, type, value) : new ParamInfo(name, value, type, value);
            ParamInfo paramInfo = (type.equals(ParamInfo.Type.OFILE)) ? new OFileParamInfo(name, value, type, defaultValue) : new ParamInfo(name, value, type, defaultValue);
            paramInfo.setDescription(description);
            paramInfo.setDefaultValueOriginal(paramInfo.getDefaultValue());

            int defaultColSpan = 1;
            if (colSpan != null) {
                try {
                    int colSpanInt = Integer.parseInt(colSpan);
                    if (colSpanInt > 0) {
                        paramInfo.setColSpan(colSpanInt);
                    } else {
                        paramInfo.setColSpan(defaultColSpan);
                    }
                } catch (Exception e) {
                    paramInfo.setColSpan(defaultColSpan);
                    System.out.println("ERROR: colSpan not an integer for param: " + name + "in xml file: " + paramXmlFileName);
                }
            } else {
                paramInfo.setColSpan(defaultColSpan);
            }


            int defaultSubPanelIndex = 0;
            if (subPanelIndex != null) {
                try {
                    int subPanelIndexInt = Integer.parseInt(subPanelIndex);
                    if (subPanelIndexInt > 0) {
                        paramInfo.setSubPanelIndex(subPanelIndexInt);
                    } else {
                        paramInfo.setSubPanelIndex(defaultSubPanelIndex);
                    }
                } catch (Exception e) {
                    paramInfo.setSubPanelIndex(defaultSubPanelIndex);
                    System.out.println("ERROR: subPanelInt not an integer for param: " + name + "in xml file: " + paramXmlFileName);
                }
            } else {
                paramInfo.setSubPanelIndex(defaultSubPanelIndex);
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

            if (putInBooleanPanel != null) {
                paramInfo.setPutInBooleanPanel(putInBooleanPanel);
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
                        addValidValueToParamInfo(OCSSW_L3mapgenController.getPreferenceFAV4Projection(),
                                OCSSW_L3mapgenController.getPreferenceFAV4ProjectionDescription(),
                                paramInfo);
                        addValidValueToParamInfo(OCSSW_L3mapgenController.getPreferenceFAV5Projection(),
                                OCSSW_L3mapgenController.getPreferenceFAV5ProjectionDescription(),
                                paramInfo);
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
                boolean passAll = OCSSW_L3mapgenController.getPreferencePassAll();
                boolean autoFillAll = OCSSW_L3mapgenController.getPreferenceAutoFillAll();
                
                
                switch (optionName) {
                    case OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_PRODUCT_LABEL:
                        setParamInfoPreference(paramInfo, OCSSW_L3mapgenController.getPreferenceProduct(), autoFillAll, passAll);
                        setParamInfoToolTip(paramInfo, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_PRODUCT_TOOLTIP);
                        break;
                    case OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_WAVELENGTH_3D_LABEL:
                        setParamInfoPreference(paramInfo, OCSSW_L3mapgenController.getPreferenceWavelength3D(), autoFillAll, passAll);
                        setParamInfoToolTip(paramInfo, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_WAVELENGTH_3D_TOOLTIP);
                        break;
                    case OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_SUITE_LABEL:
                        setParamInfoPreference(paramInfo, OCSSW_L3mapgenController.getPreferenceSuite(), autoFillAll, passAll);
                        setParamInfoToolTip(paramInfo, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_SUITE_TOOLTIP);
                        break;

                    case OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_PROJECTION_LABEL:
                        setParamInfoPreference(paramInfo, OCSSW_L3mapgenController.getPreferenceProjection(), autoFillAll, passAll);
                        if ("smi".equals(paramInfo.getValue())) {
                            String smiReplacement = OCSSW_L3mapgenController.getPreferenceProjectionSmiReplacement();
                            if (smiReplacement != null && smiReplacement.trim().length() > 0) {
                                paramInfo.setValue(smiReplacement);
                            }
                        }
                        setParamInfoToolTip(paramInfo, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_PROJECTION_TOOLTIP);
                        break;

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

                    case OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_RESOLUTION_LABEL:
                        setParamInfoPreference(paramInfo, OCSSW_L3mapgenController.getPreferenceResolution(), autoFillAll, passAll);
                        setParamInfoToolTip(paramInfo, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_RESOLUTION_TOOLTIP);
                        break;
                    case OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_WIDTH_LABEL:
                        setParamInfoPreference(paramInfo, OCSSW_L3mapgenController.getPreferenceWidth(), autoFillAll, passAll);
                        setParamInfoToolTip(paramInfo, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_WIDTH_TOOLTIP);
                        break;
                    case OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_INTERP_LABEL:
                        setParamInfoPreference(paramInfo, OCSSW_L3mapgenController.getPreferenceInterp(), autoFillAll, passAll);
                        setParamInfoToolTip(paramInfo, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_INTERP_TOOLTIP);
                        break;
                    case OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_NORTH_LABEL:
                        setParamInfoPreference(paramInfo, OCSSW_L3mapgenController.getPreferenceNorth(), autoFillAll, passAll);
                        setParamInfoToolTip(paramInfo, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_NORTH_TOOLTIP);
                        break;
                    case OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_SOUTH_LABEL:
                        setParamInfoPreference(paramInfo, OCSSW_L3mapgenController.getPreferenceSouth(), autoFillAll, passAll);
                        setParamInfoToolTip(paramInfo, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_SOUTH_TOOLTIP);
                        break;
                    case OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_WEST_LABEL:
                        setParamInfoPreference(paramInfo, OCSSW_L3mapgenController.getPreferenceWest(), autoFillAll, passAll);
                        setParamInfoToolTip(paramInfo, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_WEST_TOOLTIP);
                        break;
                    case OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_EAST_LABEL:
                        setParamInfoPreference(paramInfo, OCSSW_L3mapgenController.getPreferenceEast(), autoFillAll, passAll);
                        setParamInfoToolTip(paramInfo, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_EAST_TOOLTIP);
                        break;
                    case OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_FUDGE_LABEL:
                        setParamInfoPreference(paramInfo, OCSSW_L3mapgenController.getPreferenceFudge(), autoFillAll, passAll);
                        setParamInfoToolTip(paramInfo, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_FUDGE_TOOLTIP);
                        break;


                    case OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_APPLY_PAL_LABEL:
                        setParamInfoPreference(paramInfo, OCSSW_L3mapgenController.getPreferenceApplyPal(), autoFillAll, passAll);
                        setParamInfoToolTip(paramInfo, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_APPLY_PAL_TOOLTIP);
                        break;
                    case OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_PALFILE_LABEL:
                        setParamInfoPreference(paramInfo, OCSSW_L3mapgenController.getPreferencePalfile(), autoFillAll, passAll);
                        setParamInfoToolTip(paramInfo, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_PALFILE_TOOLTIP);
                        break;
                    case OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_DATAMIN_LABEL:
                        setParamInfoPreference(paramInfo, OCSSW_L3mapgenController.getPreferenceDataMin(), autoFillAll, passAll);
                        setParamInfoToolTip(paramInfo, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_DATAMIN_TOOLTIP);
                        break;
                    case OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_DATAMAX_LABEL:
                        setParamInfoPreference(paramInfo, OCSSW_L3mapgenController.getPreferenceDataMax(), autoFillAll, passAll);
                        setParamInfoToolTip(paramInfo, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_DATAMAX_TOOLTIP);
                        break;
                    case OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_SCALE_TYPE_LABEL:
                        setParamInfoPreference(paramInfo, OCSSW_L3mapgenController.getPreferenceScaleType(), autoFillAll, passAll);
                        setParamInfoToolTip(paramInfo, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_SCALE_TYPE_TOOLTIP);
                        break;
                    case OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_USE_TRANSPARENCY_LABEL:
                        setParamInfoPreference(paramInfo, OCSSW_L3mapgenController.getPreferenceUseTransparency(), autoFillAll, passAll);
                        setParamInfoToolTip(paramInfo, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_USE_TRANSPARENCY_TOOLTIP);
                        break;

                    case OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_MASK_LAND_LABEL:
                        setParamInfoPreference(paramInfo, OCSSW_L3mapgenController.getPreferenceMaskLand(), autoFillAll, passAll);
                        setParamInfoToolTip(paramInfo, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_MASK_LAND_TOOLTIP);
                        break;
                    case OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_RGB_LAND_LABEL:
                        setParamInfoPreference(paramInfo, OCSSW_L3mapgenController.getPreferenceRGBLand(), autoFillAll, passAll);
                        setParamInfoToolTip(paramInfo, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_RGB_LAND_TOOLTIP);
                        break;
                    case OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_LAND_LABEL:
                        setParamInfoPreference(paramInfo, OCSSW_L3mapgenController.getPreferenceLand(), autoFillAll, passAll);
                        setParamInfoToolTip(paramInfo, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_LAND_TOOLTIP);
                        break;
                    case OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_USE_RGB_LABEL:
                        setParamInfoPreference(paramInfo, OCSSW_L3mapgenController.getPreferenceUseRGB(), autoFillAll, passAll);
                        setParamInfoToolTip(paramInfo, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_USE_RGB_TOOLTIP);
                        break;
                    case OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_PRODUCT_RGB_LABEL:
                        setParamInfoPreference(paramInfo, OCSSW_L3mapgenController.getPreferenceProductRGB(), autoFillAll, passAll);
                        setParamInfoToolTip(paramInfo, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_PRODUCT_RGB_TOOLTIP);
                        break;
                    case OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_NUM_CACHE_LABEL:
                        setParamInfoPreference(paramInfo, OCSSW_L3mapgenController.getPreferenceNumCache(), autoFillAll, passAll);
                        setParamInfoToolTip(paramInfo, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_NUM_CACHE_TOOLTIP);
                        break;
                    case OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_OFORMAT_LABEL:
                        setParamInfoPreference(paramInfo, OCSSW_L3mapgenController.getPreferenceOformat(), autoFillAll, passAll);
                        setParamInfoToolTip(paramInfo, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_OFORMAT_TOOLTIP);
                        break;
                }
            }


            if ("l2bin.xml".equals(paramXmlFileName)) {
                boolean passAll = OCSSW_L2binController.getPreferencePassAll();
                boolean autoFillAll = OCSSW_L2binController.getPreferenceAutoFillAll();
                
                switch (optionName) {
                    case OCSSW_L2binController.PROPERTY_L2BIN_L3BPROD_AUTOFILL_LABEL:
                        setParamInfoPreference(paramInfo, OCSSW_L2binController.getPreferenceL3bprod(), autoFillAll, passAll);
                        break;
                    case OCSSW_L2binController.PROPERTY_L2BIN_OUTPUT_WAVELENGTHS_LABEL:
                        setParamInfoPreference(paramInfo, OCSSW_L2binController.getPreferenceOutputWavelengths(), autoFillAll, passAll);
                        break;
                    case OCSSW_L2binController.PROPERTY_L2BIN_SUITE_LABEL:
                        setParamInfoPreference(paramInfo, OCSSW_L2binController.getPreferenceSuite(), autoFillAll, passAll);
                        break;
                    case OCSSW_L2binController.PROPERTY_L2BIN_FLAGUSE_LABEL:
                        setParamInfoPreference(paramInfo, OCSSW_L2binController.getPreferenceFlaguse(), autoFillAll, passAll);
                        break;
                    case OCSSW_L2binController.PROPERTY_L2BIN_RESOLUTION_LABEL:
                        setParamInfoPreference(paramInfo, OCSSW_L2binController.getPreferenceResolution(), autoFillAll, passAll);
                        break;
                    case OCSSW_L2binController.PROPERTY_L2BIN_AREA_WEIGHTING_LABEL:
                        setParamInfoPreference(paramInfo, OCSSW_L2binController.getPreferenceAreaWeighting(), autoFillAll, passAll);
                        break;
                    case OCSSW_L2binController.PROPERTY_L2BIN_LATNORTH_LABEL:
                        setParamInfoPreference(paramInfo, OCSSW_L2binController.getPreferenceLatnorth(),autoFillAll, passAll);
                        break;
                    case OCSSW_L2binController.PROPERTY_L2BIN_LATSOUTH_LABEL:
                        setParamInfoPreference(paramInfo, OCSSW_L2binController.getPreferenceLatsouth(), autoFillAll, passAll);
                        break;
                    case OCSSW_L2binController.PROPERTY_L2BIN_LONWEST_LABEL:
                        setParamInfoPreference(paramInfo, OCSSW_L2binController.getPreferenceLonwest(), autoFillAll, passAll);
                        break;
                    case OCSSW_L2binController.PROPERTY_L2BIN_LONEAST_LABEL:
                        setParamInfoPreference(paramInfo, OCSSW_L2binController.getPreferenceLoneast(), autoFillAll, passAll);
                        break;
                    case OCSSW_L2binController.PROPERTY_L2BIN_SDAY_LABEL:
                        setParamInfoPreference(paramInfo, OCSSW_L2binController.getPreferenceSday(), autoFillAll, passAll);
                        break;
                    case OCSSW_L2binController.PROPERTY_L2BIN_EDAY_LABEL:
                        setParamInfoPreference(paramInfo, OCSSW_L2binController.getPreferenceEday(), autoFillAll, passAll);
                        break;
                    case OCSSW_L2binController.PROPERTY_L2BIN_DELTA_CROSS_LABEL:
                        setParamInfoPreference(paramInfo, OCSSW_L2binController.getPreferenceDeltaCross(), autoFillAll, passAll);
                        break;
                    case OCSSW_L2binController.PROPERTY_L2BIN_NIGHT_LABEL:
                        setParamInfoPreference(paramInfo, OCSSW_L2binController.getPreferenceNight(), autoFillAll, passAll);
                        break;
                    case OCSSW_L2binController.PROPERTY_L2BIN_PRODTYPE_LABEL:
                        setParamInfoPreference(paramInfo, OCSSW_L2binController.getPreferenceProdtype(), autoFillAll, passAll);
                        break;
                    case OCSSW_L2binController.PROPERTY_L2BIN_ROW_GROUP_LABEL:
                        setParamInfoPreference(paramInfo, OCSSW_L2binController.getPreferenceRowGroup(), autoFillAll, passAll);
                        break;
                    case OCSSW_L2binController.PROPERTY_L2BIN_COMPOSITE_PROD_LABEL:
                        setParamInfoPreference(paramInfo, OCSSW_L2binController.getPreferenceCompositeProd(),autoFillAll, passAll);
                        break;
                    case OCSSW_L2binController.PROPERTY_L2BIN_COMPOSITE_SCHEME_LABEL:
                        setParamInfoPreference(paramInfo, OCSSW_L2binController.getPreferenceCompositeScheme(), autoFillAll, passAll);
                        break;
                }
            }




            if ("l3bin.xml".equals(paramXmlFileName)) {
                switch (optionName) {
                    case OCSSW_L3binController.PROPERTY_L3BIN_PRODUCT_LABEL:
                        setParamInfoPreference(paramInfo, OCSSW_L3binController.getPreferenceProd(), false, true);
                        break;
                    case OCSSW_L3binController.PROPERTY_L3BIN_NORTH_LABEL:
                        setParamInfoPreference(paramInfo, OCSSW_L3binController.getPreferenceNorth(), false, true);
                        break;
                    case OCSSW_L3binController.PROPERTY_L3BIN_SOUTH_LABEL:
                        setParamInfoPreference(paramInfo, OCSSW_L3binController.getPreferenceSouth(), false, true);
                        break;
                    case OCSSW_L3binController.PROPERTY_L3BIN_WEST_LABEL:
                        setParamInfoPreference(paramInfo, OCSSW_L3binController.getPreferenceWest(), false, true);
                        break;
                    case OCSSW_L3binController.PROPERTY_L3BIN_EAST_LABEL:
                        setParamInfoPreference(paramInfo, OCSSW_L3binController.getPreferenceEast(), false, true);
                        break;
                    case OCSSW_L3binController.PROPERTY_L3BIN_RESOLUTION_LABEL:
                        setParamInfoPreference(paramInfo, OCSSW_L3binController.getPreferenceResolve(), false, true);
                        break;


//                    case "composite_prod":
//                        paramInfo.setValue(OCSSW_L2binController.getPreferenceCompositeProd());
//                        break;
//                    case "composite_scheme":
//                        paramInfo.setValue(OCSSW_L2binController.getPreferenceCompositeScheme());
//                        break;

                }
            }





            paramList.add(paramInfo);

        }

        return paramList;
    }


    private static void setParamInfoPreference(ParamInfo paramInfo, String preference) {
        setParamInfoPreference(paramInfo, preference, false, false);
    }

    private static void setParamInfoPreference(ParamInfo paramInfo, String preference, boolean fillAll, boolean passAll) {
        if (paramInfo != null) {
//            paramInfo.setDefaultValueOriginal(paramInfo.getDefaultValue());

            if (fillAll) {
                paramInfo.setValue(paramInfo.getDefaultValue());
            }

            if (passAll) {
                paramInfo.setDefaultValue("");
            }

            if (preference != null && preference.trim().length() > 0) {
                paramInfo.setValue(preference);
            }
        }
    }

    private static void setParamInfoToolTip(ParamInfo paramInfo, String tooltip) {
        if (paramInfo != null) {
            if (tooltip != null && tooltip.trim().length() > 0) {
                paramInfo.setDescription(tooltip);
            }
        }
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
