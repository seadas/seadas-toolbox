package gov.nasa.gsfc.seadas.processing.core;

import com.bc.ceres.core.runtime.Version;
import gov.nasa.gsfc.seadas.processing.common.*;
import gov.nasa.gsfc.seadas.processing.ocssw.OCSSW;
import gov.nasa.gsfc.seadas.processing.ocssw.OCSSWClient;
import gov.nasa.gsfc.seadas.processing.ocssw.OCSSWInfo;
import gov.nasa.gsfc.seadas.processing.ocssw.OCSSWLocal;
import gov.nasa.gsfc.seadas.processing.preferences.OCSSW_InstallerController;
import gov.nasa.gsfc.seadas.processing.preferences.OCSSW_L2binController;
import gov.nasa.gsfc.seadas.processing.preferences.OCSSW_L3binController;
import gov.nasa.gsfc.seadas.processing.preferences.OCSSW_L3mapgenController;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.VersionChecker;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.rcp.util.Dialogs.Answer;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import ucar.ma2.Array;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import javax.swing.event.SwingPropertyChangeSupport;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static gov.nasa.gsfc.seadas.processing.common.ExtractorUI.*;
import static gov.nasa.gsfc.seadas.processing.common.FilenamePatterns.getGeoFileInfo;
import static gov.nasa.gsfc.seadas.processing.common.OCSSWInstallerForm.VALID_TAGS_OPTION_NAME;
import static gov.nasa.gsfc.seadas.processing.core.L2genData.GEOFILE;

/**
 * Created by IntelliJ IDEA. User: Aynur Abdurazik (aabduraz) Date: 3/16/12
 * Time: 2:20 PM To change this template use File | Settings | File Templates.
 */
public class ProcessorModel implements SeaDASProcessorModel, Cloneable {

    protected String programName;

    protected ParamList paramList;
    private boolean acceptsParFile;
    private boolean hasGeoFile;

    private Set<String> primaryOptions;
    private String parFileOptionName;
    private String subPanel0Title;
    private String subPanel1Title;
    private String subPanel2Title;
    private String subPanel3Title;
    private String subPanel4Title;
    private int numColumns;
    private int columnWidth;

    private static final String DELIMITOR_NUMBER = "_";
    private static final String DELIMITOR_STRING = ".";

    private boolean readyToRun;
    private final String runButtonPropertyName = "RUN_BUTTON_STATUS_CHANGED";
    private final String allparamInitializedPropertyName = "ALL_PARAMS_INITIALIZED";
    private final String l2prodProcessors = "l2mapgen l2brsgen l2bin l2bin_aquarius l3bin smigen";

    final public String L1AEXTRACT_MODIS = "l1aextract_modis",
            L1AEXTRACT_MODIS_XML_FILE = "l1aextract_modis.xml",
            L1AEXTRACT_SEAWIFS = "l1aextract_seawifs",
            L1AEXTRACT_SEAWIFS_XML_FILE = "l1aextract_seawifs.xml",
            L1AEXTRACT_VIIRS = "l1aextract_viirs",
            L1AEXTRACT_VIIRS_XML_FILE = "l1aextract_viirs.xml",
            L2EXTRACT = "l2extract",
            L2EXTRACT_XML_FILE = "l2extract.xml";

    private ProcessorModel secondaryProcessor;
    private Pattern progressPattern;

    private ProcessorTypeInfo.ProcessorID processorID;

    private final SwingPropertyChangeSupport propertyChangeSupport = new SwingPropertyChangeSupport(this);

    private boolean multipleInputFiles;

    private boolean openInSeadas;

    private String prodParamName = "prod";

    private static String[] cmdArrayPrefix;
    private String[] cmdArraySuffix;

    boolean isIfileValid = false;
    private static OCSSW ocssw;
    private String fileExtensions = null;

    FileInfo inputFileInfo;

    public ProcessorModel(String name, OCSSW ocssw) {

        programName = name;
        this.setOcssw(ocssw);
        acceptsParFile = false;
        hasGeoFile = false;
        readyToRun = false;
        multipleInputFiles = false;
        paramList = new ParamList();
        setParFileOptionName(ParamUtils.DEFAULT_PAR_FILE_NAME);
        setSubPanel0Title("");
        setSubPanel1Title("");
        setSubPanel2Title("");
        setSubPanel3Title("");
        setSubPanel4Title("");
        setNumColumns(4);
        setColumnWidth(20);
        processorID = ProcessorTypeInfo.getProcessorID(programName);
        primaryOptions = new HashSet<String>();
        primaryOptions.add("ifile");
        primaryOptions.add("ofile");
        progressPattern = Pattern.compile(ParamUtils.DEFAULT_PROGRESS_REGEX);
        setOpenInSeadas(false);
        setCommandArrayPrefix();
        setCommandArraySuffix();
    }

    public ProcessorModel(String name, String parXMLFileName, OCSSW ocssw) {
        this(name, ocssw);
        if (parXMLFileName != null && parXMLFileName.length() > 0) {
            setParamList((ArrayList<ParamInfo>) ParamUtils.computeParamList(parXMLFileName));
            acceptsParFile = ParamUtils.getOptionStatus(parXMLFileName, "hasParFile");
            setParFileOptionName(ParamUtils.getParFileOptionName(parXMLFileName));
            setSubPanel0Title(ParamUtils.getElementString(parXMLFileName, "subPanel0Title"));
            setSubPanel1Title(ParamUtils.getElementString(parXMLFileName, "subPanel1Title"));
            setSubPanel2Title(ParamUtils.getElementString(parXMLFileName, "subPanel2Title"));
            setSubPanel3Title(ParamUtils.getElementString(parXMLFileName, "subPanel3Title"));
            setSubPanel4Title(ParamUtils.getElementString(parXMLFileName, "subPanel4Title"));
            setNumColumns(ParamUtils.getElementInt(parXMLFileName, "numColumns"));
            setColumnWidth(ParamUtils.getElementInt(parXMLFileName, "columnWidth"));
            progressPattern = Pattern.compile(ParamUtils.getProgressRegex(parXMLFileName));
            hasGeoFile = ParamUtils.getOptionStatus(parXMLFileName, "hasGeoFile");
            setPrimaryOptions(ParamUtils.getPrimaryOptions(parXMLFileName));
            setOpenInSeadas(false);
            setCommandArrayPrefix();
            setCommandArraySuffix();
        }
    }

    public ProcessorModel(String name, ArrayList<ParamInfo> paramList, OCSSW ocssw) {
        this(name, ocssw);
        setParamList(paramList);
    }

    public static ProcessorModel valueOf(String programName, String xmlFileName, OCSSW ocssw) {
        ProcessorTypeInfo.ProcessorID processorID = ProcessorTypeInfo.getProcessorID(programName);
        switch (processorID) {
            case EXTRACTOR:
                return new Extractor_Processor(programName, xmlFileName, ocssw);
            case MODIS_L1B:
                return new Modis_L1B_Processor(programName, xmlFileName, ocssw);
            case LONLAT2PIXLINE:
                return new LonLat2Pixels_Processor(programName, xmlFileName, ocssw);
//            case SMIGEN:
//                return new SMIGEN_Processor(programName, xmlFileName, ocssw);
            case L3MAPGEN:
                return new L3MAPGEN_Processor(programName, xmlFileName, ocssw);
            case MAPGEN:
                return new MAPGEN_Processor(programName, xmlFileName, ocssw);
            case L2BIN:
                return new L2Bin_Processor(programName, xmlFileName, ocssw);
//            case L2BIN_AQUARIUS:
//                return new L2Bin_Processor(programName, xmlFileName, ocssw);
            case L3BIN:
                return new L3Bin_Processor(programName, xmlFileName, ocssw);
            case L3BINDUMP:
                return new L3BinDump_Processor(programName, xmlFileName, ocssw);
            case OCSSW_INSTALLER:
                return new OCSSWInstaller_Processor(programName, xmlFileName, ocssw);
            default:
        }
        return new ProcessorModel(programName, xmlFileName, ocssw);
    }

    void setCommandArrayPrefix() {
        OCSSWInfo ocsswInfo = OCSSWInfo.getInstance();
        cmdArrayPrefix = new String[4];
        getCmdArrayPrefix()[0] = ocsswInfo.getOcsswRunnerScriptPath();
        getCmdArrayPrefix()[1] = "--ocsswroot";
        getCmdArrayPrefix()[2] = ocsswInfo.getOcsswRoot();
        getCmdArrayPrefix()[3] = getProgramName();
    }

    private void setCommandArraySuffix() {
        setCmdArraySuffix(new String[0]);
    }

    public void addParamInfo(ParamInfo info) {
        paramList.addInfo(info);
    }

    public void removeParamInfo(ParamInfo paramInfo) {
        paramList.removeInfo(paramInfo.getName());
    }

    public boolean isReadyToRun() {
        return readyToRun;
    }

    public void setReadyToRun(boolean readyToRun) {
        boolean oldValue = this.readyToRun;
        this.readyToRun = readyToRun;
        fireEvent(getRunButtonPropertyName(), oldValue, readyToRun);
    }

    public String getOfileName() {
        return getParamValue(getPrimaryOutputFileOptionName());
    }

    public boolean isMultipleInputFiles() {
        return multipleInputFiles;
    }

    public void setMultipleInputFiles(boolean multipleInputFiles) {
        this.multipleInputFiles = multipleInputFiles;
    }

    public void createsmitoppmProcessorModel(String ofileName) {
        ProcessorModel smitoppm = new ProcessorModel("smitoppm_4_ui", getOcssw());
        smitoppm.setAcceptsParFile(false);
        ParamInfo pi1 = new ParamInfo("ifile", getParamValue(getPrimaryOutputFileOptionName()));
        pi1.setOrder(0);
        pi1.setType(ParamInfo.Type.IFILE);
        ParamInfo pi2 = new ParamInfo("ofile", ofileName);
        pi2.setOrder(1);
        pi2.setType(ParamInfo.Type.OFILE);
        smitoppm.addParamInfo(pi1);
        smitoppm.addParamInfo(pi2);
        setSecondaryProcessor(smitoppm);

    }

    public void addParamInfo(String name, String value, ParamInfo.Type type) {
        ParamInfo info = new ParamInfo(name, value, type);
        addParamInfo(info);
    }

    public void addParamInfo(String name, String value, ParamInfo.Type type, int order) {
        ParamInfo info = new ParamInfo(name, value, type);
        info.setOrder(order);
        addParamInfo(info);
    }

    public String getPrimaryInputFileOptionName() {
        for (String name : primaryOptions) {
            ParamInfo param = paramList.getInfo(name);
            if ((param != null)
                    && (param.getType() == ParamInfo.Type.IFILE)
                    && (!param.getName().toLowerCase().contains("geo"))) {
                return name;
            }
        }
        return null;
    }

    public String getPrimaryOutputFileOptionName() {
        for (String name : primaryOptions) {
            ParamInfo param = paramList.getInfo(name);
            if ((param != null) && (param.getType() == ParamInfo.Type.OFILE)) {
                return name;
            }
        }
        return null;
    }

    public boolean hasGeoFile() {
        return hasGeoFile;
    }

    public void setHasGeoFile(boolean hasGeoFile) {
        boolean oldValue = this.hasGeoFile;
        this.hasGeoFile = hasGeoFile;
        paramList.getPropertyChangeSupport().firePropertyChange("geofile", oldValue, hasGeoFile);
    }

    public boolean isValidProcessor() {
        //SeadasLogger.getLogger().info("program location: " + OCSSWInfo.getInstance().getOcsswRunnerScriptPath());
        return OCSSWInfo.getInstance().getOcsswRunnerScriptPath() != null;
    }

    public String getProgramName() {
        return programName;
    }

    public ArrayList<ParamInfo> getProgramParamList() {
        return paramList.getParamArray();
    }

    public boolean hasPrimaryOutputFile() {
        String name = getPrimaryOutputFileOptionName();
        if (name == null) {
            return false;
        } else {
            return true;
        }
    }

    public void setAcceptsParFile(boolean acceptsParFile) {
        this.acceptsParFile = acceptsParFile;
    }

    public boolean acceptsParFile() {
        return acceptsParFile;
    }

    public boolean doTheyEqualAfterTrimming(String string1, String string2) {
        if (string1 == null) {
            string1 = "";
        }
        if (string2 == null) {
            string2 = "";
        }

        string1 = string1.trim();
        string2 = string2.trim();

        if (string1.equals(string2)) {
            return true;
        } else {
            return false;
        }
    }

    public boolean doTheyEqual(String string1, String string2) {
        if (string1 == null && string2 == null) {
            return true;
        }

        if (string1 == null && string2 != null) {
            return false;
        }

        if (string1 != null && string2 == null) {
            return false;
        }

        if (string1.equals(string2)) {
            return true;
        } else {
            return false;
        }
    }


    public void updateParamInfo(ParamInfo currentOption, String newValue) {
        if (currentOption == null || currentOption.getName() == null) {
            return;
        }

        if (doTheyEqual(currentOption.getValue(), newValue)) {
            return;
        }


        boolean suiteChanged = false;
        boolean ifileChanged = false;
        boolean ignore = false;  // ignore is suite or ifile and it is not changed

        if (currentOption.getName().equals("suite")) {
            suiteChanged = !doTheyEqualAfterTrimming(currentOption.getValue(), newValue);
            System.out.println("suite" + "|" + currentOption.getValue() + "|" + newValue + "|" + suiteChanged);
            if (!suiteChanged) {
                ignore = true;
            }
        }
        if (currentOption.getName().equals("ifile")) {
            ifileChanged = !doTheyEqualAfterTrimming(currentOption.getValue(), newValue);
            System.out.println("ifile" + "|" + currentOption.getValue() + "|" + newValue + "|" + ifileChanged);
            if (!ifileChanged) {
                ignore = true;
            }
        }

        if (!ignore) {
            updateParamInfo(currentOption.getName(), newValue);
            if ("l2bin".equalsIgnoreCase(programName)) {
                if (suiteChanged || ifileChanged) {
                    updateL2BinParams();
                }
            }
            if ("l3mapgen".equalsIgnoreCase(programName)) {
                if (suiteChanged || ifileChanged) {
                    updateL3MapgenParams();
                }
            }

            checkCompleteness();
        }
    }

    protected void checkCompleteness() {
        boolean complete = true;

        for (ParamInfo param : paramList.getParamArray()) {
            if (param.getValue() == null || param.getValue().trim().length() == 0) {
                complete = false;
                break;
            }
        }

        if (complete) {
            fireEvent(getAllparamInitializedPropertyName(), false, true);
        }
    }

    public ParamInfo getParamInfo(String paramName) {
        return paramList.getInfo(paramName);
    }

    public String getParamValue(String paramName) {
        ParamInfo option = getParamInfo(paramName);
        if (option != null) {
            return option.getValue();
        }
        return null;
    }

    public void updateParamInfo(String paramName, String newValue) {

        ParamInfo option = getParamInfo(paramName);
        if (option != null) {
            String oldValue = option.getValue();
            option.setValue(newValue);
            checkCompleteness();
            if (!(oldValue.contains(newValue) && oldValue.trim().length() == newValue.trim().length())) {
                SeadasFileUtils.debug("property changed from " + oldValue + " to " + newValue);
                propertyChangeSupport.firePropertyChange(option.getName(), oldValue, newValue);
            }
        }
    }

    public boolean updateIFileInfo(String ifileName) {

        if (programName != null && (programName.equals("multilevel_processor"))) {
            return true;
        }

        File ifile = new File(ifileName);

        inputFileInfo = new FileInfo(ifile.getParent(), ifile.getName(), ocssw);

        if (programName != null && verifyIFilePath(ifileName)) {

            if ("l3mapgen".equalsIgnoreCase(programName)) {
                String resolution = getParamValue("resolution");
                String oformat = getParamValue("oformat");
                String product = getParamValue("product");
                String projection = getParamValue("projection");
                String interp = getParamValue("interp");
                String north = getParamValue("north");
                String south = getParamValue("south");
                String west = getParamValue("west");
                String east = getParamValue("east");

                String ofileName = getOfileForL3MapGenWrapper(ifileName, getOcssw(), programName, resolution, oformat, product, projection, interp, north, south, west, east);

                if (ofileName != null) {
                    isIfileValid = true;
                    updateParamInfo(getPrimaryInputFileOptionName(), ifileName + "\n");
                    updateGeoFileInfo(ifileName, inputFileInfo);
                    updateOFileInfo(ofileName);

                    updateParamValues(new File(ifileName));
                }
            } else if ("l2bin".equalsIgnoreCase(programName)) {
                String resolution = getParamValue("resolution");
                String suite = getParamValue("suite");
                String l3bprod = getParamValue("l3bprod");
                String prodtype = getParamValue("prodtype");
                String north = getParamValue("latnorth");
                String south = getParamValue("latsouth");
                String west = getParamValue("lonwest");
                String east = getParamValue("loneast");


                String ofileName = getOfileForL2BinWrapper(ifileName, getOcssw(), programName, resolution, l3bprod, suite, prodtype, north, south, west, east);

                if (ofileName != null) {
                    isIfileValid = true;
                    updateParamInfo(getPrimaryInputFileOptionName(), ifileName + "\n");
                    updateGeoFileInfo(ifileName, inputFileInfo);
                    updateOFileInfo(ofileName);

                    updateParamValues(new File(ifileName));
                }

            } else if ("l3bin".equalsIgnoreCase(programName)) {
                String resolve = getParamValue("resolve");
                String prod = getParamValue("prod");
                String north = getParamValue("latnorth");
                String south = getParamValue("latsouth");
                String west = getParamValue("lonwest");
                String east = getParamValue("loneast");

                String ofileName = getOfileForL3BinWrapper(ifileName, getOcssw(), programName, resolve, prod, north, south, west, east);

                if (ofileName != null) {
                    isIfileValid = true;
                    updateParamInfo(getPrimaryInputFileOptionName(), ifileName + "\n");
                    updateGeoFileInfo(ifileName, inputFileInfo);
                    updateOFileInfo(ofileName);

                    updateParamValues(new File(ifileName));
                }

            } else {
                //ocssw.setIfileName(ifileName);
                String ofileName = getOcssw().getOfileName(ifileName, programName);

                //SeadasLogger.getLogger().info("ofile name from finding next level name: " + ofileName);
                if (ofileName != null) {
                    isIfileValid = true;
                    updateParamInfo(getPrimaryInputFileOptionName(), ifileName + "\n");
                    updateGeoFileInfo(ifileName, inputFileInfo);
                    updateOFileInfo(getOFileFullPath(ofileName));
                    updateParamValues(new File(ifileName));
                }
            }
        } else {
            isIfileValid = false;
            updateParamInfo(getPrimaryOutputFileOptionName(), "" + "\n");
            removePropertyChangeListeners(getPrimaryInputFileOptionName());

            Answer answer = Dialogs.requestDecision(programName, "Cannot compute output file name. Would you like to continue anyway?", true, null);
            switch (answer) {
                case CANCELLED:
                    updateParamInfo(getPrimaryInputFileOptionName(), "" + "\n");
                    break;
            }
        }
        return isIfileValid;
    }

    //todo: change the path to get geo filename from ifile
    public boolean updateGeoFileInfo(String ifileName, FileInfo inputFileInfo) {
        FileInfo geoFileInfo = getGeoFileInfo(inputFileInfo, ocssw);
        if (geoFileInfo != null) {
            setHasGeoFile(true);
            updateParamInfo(GEOFILE, geoFileInfo.getFile().getAbsolutePath());
            return true;
        } else {
            setHasGeoFile(false);
            return false;
        }
    }

    public boolean updateOFileInfo(String newValue) {
        if (newValue != null && newValue.trim().length() > 0) {
            //String ofile = getOFileFullPath(newValue);
            updateParamInfo(getPrimaryOutputFileOptionName(), newValue + "\n");
            setReadyToRun(newValue.trim().length() == 0 ? false : true);
            return true;
        }
        return false;
    }

    public void setParamValue(String name, String value) {
        SeadasFileUtils.debug("primary io file option names: " + getPrimaryInputFileOptionName() + " " + getPrimaryOutputFileOptionName());
        SeadasFileUtils.debug("set param value: " + name + " " + value);
        SeadasFileUtils.debug(name + " " + value);
        if (name.trim().equals(getPrimaryInputFileOptionName())) {
            if (value.contains(" ")) {
                SimpleDialogMessage dialog = new SimpleDialogMessage(null, "<html><br>&nbsp;&nbsp;WARNING!!<br> " +
                        "&nbsp;&nbsp;Directory path and/or filename cannot have a space in it&nbsp;&nbsp;<br>&nbsp;</html>");
                dialog.setVisible(true);
                dialog.setEnabled(true);
            } else {
                updateIFileInfo(value);
            }
        } else if (name.trim().equals(getPrimaryOutputFileOptionName())) {
            if (value.contains(" ")) {
                SimpleDialogMessage dialog = new SimpleDialogMessage(null, "<html><br>&nbsp;&nbsp;WARNING!!<br> " +
                        "&nbsp;&nbsp;Directory path and/or filename cannot have a space in it&nbsp;&nbsp;<br>&nbsp;</html>");
                dialog.setVisible(true);
                dialog.setEnabled(true);
            } else {
                updateOFileInfo(getOFileFullPath(value));
            }
        } else {
            updateParamInfo(name, value);
        }
    }

    public String[] getCmdArrayPrefix() {
        return cmdArrayPrefix;
    }

    public EventInfo[] eventInfos = {
            new EventInfo("none", this),};

    private EventInfo getEventInfo(String name) {
        for (EventInfo eventInfo : eventInfos) {
            if (name.equals(eventInfo.getName())) {
                return eventInfo;
            }
        }
        return null;
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        SeadasFileUtils.debug(" added property name: " + propertyName);
        if (propertyName != null) {
            EventInfo eventInfo = getEventInfo(propertyName);
            if (eventInfo == null) {
                propertyChangeSupport.addPropertyChangeListener(propertyName, listener);
            } else {
                eventInfo.addPropertyChangeListener(listener);
            }
        }
    }

    public void removePropertyChangeListeners(String propertyName) {
        EventInfo eventInfo = getEventInfo(propertyName);
        PropertyChangeListener[] propertyListeners = propertyChangeSupport.getPropertyChangeListeners(propertyName);
        for (PropertyChangeListener propertyChangeListener : propertyListeners) {
            propertyChangeSupport.removePropertyChangeListener(propertyChangeListener);

            if (eventInfo == null) {
                propertyChangeSupport.removePropertyChangeListener(propertyChangeListener);
            } else {
                eventInfo.removePropertyChangeListener(propertyChangeListener);
            }
        }
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        EventInfo eventInfo = getEventInfo(propertyName);
        if (eventInfo == null) {
            propertyChangeSupport.removePropertyChangeListener(propertyName, listener);
        } else {
            eventInfo.removePropertyChangeListener(listener);
        }
    }

    public void disableEvent(String name) {
        EventInfo eventInfo = getEventInfo(name);
        if (eventInfo == null) {
            //SeadasLogger.getLogger().severe("disableEvent - eventInfo not found for " + name);
            SeadasFileUtils.debug("severe: disableEvent - eventInfo not found for " + name);
        } else {
            eventInfo.setEnabled(false);
        }
    }

    public void enableEvent(String name) {
        EventInfo eventInfo = getEventInfo(name);
        if (eventInfo == null) {
            //SeadasLogger.getLogger().severe("enableEvent - eventInfo not found for " + name);
            SeadasFileUtils.debug("severe: enableEvent - eventInfo not found for " + name);
        } else {
            eventInfo.setEnabled(true);
        }
    }

    public void fireEvent(String name) {
        fireEvent(name, null, null);
    }

    public void fireEvent(String name, Serializable oldValue, Serializable newValue) {
        EventInfo eventInfo = getEventInfo(name);
        if (eventInfo == null) {
            propertyChangeSupport.firePropertyChange(new PropertyChangeEvent(this, name, oldValue, newValue));
        } else {
            eventInfo.fireEvent(oldValue, newValue);
        }
    }

    public void fireAllParamEvents() {
        for (ParamInfo paramInfo : paramList.getParamArray()) {
            if (paramInfo.getName() != null && !paramInfo.getName().toLowerCase().equals("none")) {
                fireEvent(paramInfo.getName());
            }
        }
    }

    public File getRootDir() {
        File rootDir = (new File(getParamValue(getPrimaryInputFileOptionName()))).getParentFile();
        if (rootDir != null) {
            return rootDir;
        } else {
            try {
                rootDir = new File(OCSSWInfo.getInstance().getOcsswRoot());
            } catch (Exception e) {
                //SeadasLogger.getLogger().severe("error in getting ocssw root!");
                SeadasFileUtils.debug("severe: error in getting ocssw root!");
            }

        }
        return rootDir == null ? new File(".") : rootDir;
    }

    public ProcessorModel getSecondaryProcessor() {
        return secondaryProcessor;
    }

    public void setSecondaryProcessor(ProcessorModel secondaryProcessor) {
        this.secondaryProcessor = secondaryProcessor;
    }

    public boolean isValidIfile() {
        return isIfileValid;
    }

    public boolean isGeofileRequired() {
        return hasGeoFile;
    }

    @Override
    public boolean isWavelengthRequired() {
        return true;
    }

    boolean verifyIFilePath(String ifileName) {

        File ifile = new File(ifileName);

        if (ifile.exists()) {
            return true;
        }
        return false;
    }

    private String getIfileDirString() {
        String ifileDir;
        try {
            ifileDir = getParamValue(getPrimaryInputFileOptionName());
            ifileDir = ifileDir.substring(0, ifileDir.lastIndexOf(File.separator));
        } catch (Exception e) {
            ifileDir = System.getProperty("user.dir");
        }
        return ifileDir;
    }

    public File getIFileDir() {
        if (new File(getIfileDirString()).isDirectory()) {
            return new File(getIfileDirString());
        } else {
            return null;
        }
    }

    String getOFileFullPath(String fileName) {
        if (fileName.indexOf(File.separator) != -1 && new File(fileName).getParentFile().exists()) {
            return fileName;
        } else {
            String ofileNameWithoutPath = fileName.substring(fileName.lastIndexOf(File.separator) + 1);
            return getIfileDirString() + File.separator + ofileNameWithoutPath;
        }
    }

    public void setProgramName(String programName) {
        this.programName = programName;
    }

    public ParamList getParamList() {
        return paramList;
    }

    public void setParamList(ParamList paramList) {
        this.paramList = paramList;
    }

    public void setParamList(ArrayList<ParamInfo> paramArray) {
        paramList.clear();
        for (ParamInfo param : paramArray) {
            paramList.addInfo(param);
        }
    }

    public SwingPropertyChangeSupport getPropertyChangeSupport() {
        return paramList.getPropertyChangeSupport();
    }

    public void appendPropertyChangeSupport(SwingPropertyChangeSupport propertyChangeSupport) {
        paramList.appendPropertyChangeSupport(propertyChangeSupport);
    }

    public Set<String> getPrimaryOptions() {
        return primaryOptions;
    }

    public void setPrimaryOptions(Set<String> primaryOptions) {
        this.primaryOptions = primaryOptions;
    }

    public String getRunButtonPropertyName() {
        return runButtonPropertyName;
    }

    public String getAllparamInitializedPropertyName() {
        return allparamInitializedPropertyName;
    }

    private String executionLogMessage;

    public String getExecutionLogMessage() {
        return executionLogMessage;
    }

    public void setExecutionLogMessage(String executionLogMessage) {
        this.executionLogMessage = executionLogMessage;
    }

    public void setProgressPattern(Pattern progressPattern) {
        this.progressPattern = progressPattern;
    }

    public Pattern getProgressPattern() {
        return progressPattern;
    }

    public boolean isOpenInSeadas() {
        return openInSeadas;
    }

    public void setOpenInSeadas(boolean openInSeadas) {
        this.openInSeadas = openInSeadas;
    }

    String getProdParamName() {
        return prodParamName;
    }

    void setProdPramName(String prodPramName) {
        this.prodParamName = prodPramName;
    }

    public void updateParamValues(Product selectedProduct) {
        updateParamValues(selectedProduct.getFileLocation());
    }

    public void updateParamValues(File selectedFile) {

        if (selectedFile != null && programName != null && (l2prodProcessors.contains(programName) || "l3mapgen".equalsIgnoreCase(programName))) {
            //   stay
        } else {
            return;
        }

//        if (selectedFile == null || (programName != null && (!l2prodProcessors.contains(programName)) || "l3mapgen".equalsIgnoreCase(programName))) {
//            return;
//        }


        NetcdfFile ncFile = null;
        File fileTmp = null;

        if (selectedFile.getName().endsWith(".txt")) {

            try (BufferedReader br = new BufferedReader(new FileReader(selectedFile))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line != null && line.trim().length() > 0 && !line.trim().startsWith("#") && !line.trim().startsWith("//")) {
                        fileTmp = new File(line.trim());
                        if (fileTmp != null) {
                            try {
                                ncFile = NetcdfFile.open(fileTmp.getAbsolutePath());
                            } catch (IOException ioe) {
                                ncFile = null;
                            }
                        }

                        if (ncFile != null) {
                            break;
                        }
                    }

                }
            } catch (Exception e) {
                ncFile = null;
            }
        } else {
            try {
                ncFile = NetcdfFile.open(selectedFile.getAbsolutePath());
            } catch (IOException ioe) {
                ncFile = null;
            }
        }


        ArrayList<String> products = new ArrayList<String>();
        if (ncFile != null) {

            if ("l3mapgen".equalsIgnoreCase(programName) || "l3bin".equalsIgnoreCase(programName)) {
                try {
                    Attribute unitsAttribute = ncFile.findGlobalAttribute("units");
                    Array array = unitsAttribute.getValues();
                    String units = array.toString();
                    String[] values = units.split("[,\\s]");
                    for (String value : values) {
                        String[] values2 = value.split(":");
                        if (values2.length == 2) {
                            String bandName = values2[0];
                            products.add(bandName);
                        }
                    }

                } catch (Exception e) {
                }

                if ("l3mapgen".equalsIgnoreCase(programName)) {
                    updateL3MapgenParams();
                }

            } else {

                java.util.List<Variable> var = null;

                List<ucar.nc2.Group> groups = ncFile.getRootGroup().getGroups();


                for (ucar.nc2.Group g : groups) {
                    //retrieve geophysical data to fill in "product" value ranges
                    if (g.getShortName().equalsIgnoreCase("Geophysical_Data")) {
                        var = g.getVariables();
                        break;
                    }
                }
                if (var != null) {
                    for (Variable v : var) {
                        products.add(v.getShortName());

                        if ("l2bin".equalsIgnoreCase(programName)) {
                            if (v != null && v.getShortName().equalsIgnoreCase("l2_flags")) {
                                updateFlagUseWrapper(v);
                            }
                        }
                    }
                }
            }
        }


        String[] bandNames = new String[products.size()];
        products.toArray(bandNames);
        String testThisToSeeIfProduct = getProdParamName();

        if (bandNames != null) {
            if ("l2bin".equalsIgnoreCase(programName)) {
                updateProductFieldWithBandNames("l3bprod", bandNames);
                updateProductFieldWithBandNames("composite_prod", bandNames);
            }

            if ("l3mapgen".equalsIgnoreCase(programName)) {
                updateProductFieldWithBandNames("product", bandNames);
            }

            if ("l3bin".equalsIgnoreCase(programName)) {
                updateProductFieldWithBandNames("prod", bandNames);
                updateProductFieldWithBandNames("composite_prod", bandNames);
            }
        }

        if ("l2bin".equalsIgnoreCase(programName) || "l3mapgen".equalsIgnoreCase(programName)) {
            updateSuite(selectedFile);
        }
    }


    private void updateFlagUseWrapper(Variable flagGroup) {

        try {
            Attribute flagMeaningAttribute = flagGroup.attributes().findAttribute("flag_meanings");
            Array array = flagMeaningAttribute.getValues();
            String flagMeanings = array.toString();

            if (flagMeanings.length() > 0) {
                if ("l2bin".equalsIgnoreCase(programName)) {
                    ParamInfo flaguseParamInfo = paramList.getInfo("flaguse");
                    if (flaguseParamInfo == null) {
                        flaguseParamInfo = new ParamInfo("flaguse");
                        flaguseParamInfo.setDescription("flaguse");
                    }
                    flaguseParamInfo.clearValidValueInfos();

                    String[] values1 = flagMeanings.split("[,\\s]");
                    Arrays.sort(values1);

                    for (String value : values1) {
                        ParamValidValueInfo test = new ParamValidValueInfo(value);
                        test.setDescription(value);
                        flaguseParamInfo.getValidValueInfos().add(test);
                    }

//                                        ParamValidValueInfo test = new ParamValidValueInfo("NONE");
//                                        test.setDescription("NONE");
//                                        flaguseParamInfo.getValidValueInfos().add(test);

//                                    paramList.getPropertyChangeSupport().firePropertyChange("flaguse", oldValue, newValue);
                }
            }

        } catch (Exception e) {
        }

        if ("l2bin".equalsIgnoreCase(programName)) {
            updateL2BinParams();
        }


    }


    private void updateProductFieldWithBandNames(String field, String[] bandNames) {
        ParamInfo pi = getParamInfo(field);
        if (pi != null) {
            pi.clearValidValueInfos();

            ParamValidValueInfo paramValidValueInfo;
            for (String bandName : bandNames) {
                paramValidValueInfo = new ParamValidValueInfo(bandName);
                paramValidValueInfo.setDescription(bandName);
                pi.addValidValueInfo(paramValidValueInfo);
            }

            String newValue = pi.getValue() != null ? pi.getValue() : "";

            paramList.getPropertyChangeSupport().firePropertyChange(field, "-1", newValue);
            updateParamInfo(field, newValue);
            fireEvent(field, "-1", newValue);
        }
    }


    @Override
    public String getImplicitInputFileExtensions() {
        return fileExtensions;
    }

    public void setImplicitInputFileExtensions(String fileExtensions) {

        this.fileExtensions = fileExtensions;
    }

    public String[] getCmdArraySuffix() {
        return cmdArraySuffix;
    }

    public void setCmdArraySuffix(String[] cmdArraySuffix) {
        this.cmdArraySuffix = cmdArraySuffix;
    }

    public String getParFileOptionName() {
        return parFileOptionName;
    }

    public void setParFileOptionName(String parFileOptionName) {
        this.parFileOptionName = parFileOptionName;
    }


    public String getSubPanel0Title() {
        return subPanel0Title;
    }

    public void setSubPanel0Title(String subPanel0Title) {
        this.subPanel0Title = subPanel0Title;
    }


    public String getSubPanel1Title() {
        return subPanel1Title;
    }

    public void setSubPanel1Title(String subPanel1Title) {
        this.subPanel1Title = subPanel1Title;
    }

    public String getSubPanel2Title() {
        return subPanel2Title;
    }

    public void setSubPanel2Title(String subPanel2Title) {
        this.subPanel2Title = subPanel2Title;
    }

    public String getSubPanel3Title() {
        return subPanel3Title;
    }

    public void setSubPanel3Title(String subPanel3Title) {
        this.subPanel3Title = subPanel3Title;
    }

    public String getSubPanel4Title() {
        return subPanel4Title;
    }

    public void setSubPanel4Title(String subPanel4Title) {
        this.subPanel4Title = subPanel4Title;
    }

    public int getNumColumns() {
        return numColumns;
    }

    public void setNumColumns(int numColumns) {
        if (numColumns > 0) {
            this.numColumns = numColumns;
        }
    }


    public int getColumnWidth() {
        return columnWidth;
    }

    public void setColumnWidth(int columnWidth) {
        if (columnWidth > 0) {
            this.columnWidth = columnWidth;
        }
    }


    public OCSSW getOcssw() {
        return ocssw;
    }

    public void setOcssw(OCSSW ocssw) {
        this.ocssw = ocssw;
    }

    private static class Extractor_Processor extends ProcessorModel {

        Extractor_Processor(String programName, String xmlFileName, OCSSW ocssw) {
            super(programName, xmlFileName, ocssw);
        }

        @Override
        public boolean updateIFileInfo(String ifileName) {
            File ifile = new File(ifileName);

            inputFileInfo = new FileInfo(ifile.getParent(), ifile.getName(), ocssw);
            if (inputFileInfo.getTypeId() == FileTypeInfo.Id.UNKNOWN) {

            }
            selectExtractorProgram();
            isIfileValid = false;
            if (programName != null && verifyIFilePath(ifileName)) {
                //ocssw.setIfileName(ifileName);
                String ofileName = new File(ifileName).getParent() + File.separator + getOcssw().getOfileName(ifileName);
                //SeadasLogger.getLogger().info("ofile name from finding next level name: " + ofileName);
                if (ofileName != null) {
                    //programName = getOcssw().getProgramName();
                    setParamList(ParamUtils.computeParamList(getOcssw().getXmlFileName()));
                    isIfileValid = true;
                    updateParamInfo(getPrimaryInputFileOptionName(), ifileName + "\n");
                    //updateGeoFileInfo(ifileName, inputFileInfo);
                    updateOFileInfo(getOFileFullPath(ofileName));
                    updateParamValues(new File(ifileName));
                }
            } else {
                isIfileValid = false;
                updateParamInfo(getPrimaryOutputFileOptionName(), "" + "\n");
                removePropertyChangeListeners(getPrimaryInputFileOptionName());

                Answer answer = Dialogs.requestDecision(programName, "Cannot compute output file name. Would you like to continue anyway?", true, null);
                switch (answer) {
                    case CANCELLED:
                        updateParamInfo(getPrimaryInputFileOptionName(), "" + "\n");
                        break;
                }
            }
            return isIfileValid;
        }

        void selectExtractorProgram() {
            String missionName = inputFileInfo.getMissionName();
            String fileType = inputFileInfo.getFileTypeName();
            String xmlFileName = ocssw.getXmlFileName();
            if (missionName != null && fileType != null) {
                if (missionName.indexOf("MODIS") != -1 && fileType.indexOf("1A") != -1) {
                    programName = L1AEXTRACT_MODIS;
                    xmlFileName = L1AEXTRACT_MODIS_XML_FILE;
                } else if (missionName.indexOf("SeaWiFS") != -1 && fileType.indexOf("1A") != -1 || missionName.indexOf("CZCS") != -1) {
                    programName = L1AEXTRACT_SEAWIFS;
                    xmlFileName = L1AEXTRACT_SEAWIFS_XML_FILE;
                } else if ((missionName.indexOf("VIIRS") != -1
                        || missionName.indexOf("VIIRSJ1") != -1
                        || missionName.indexOf("VIIRSJ2") != -1)
                        && fileType.indexOf("1A") != -1) {
                    programName = L1AEXTRACT_VIIRS;
                    xmlFileName = L1AEXTRACT_VIIRS_XML_FILE;
                } else if ((fileType.indexOf("L2") != -1 || fileType.indexOf("Level 2") != -1) ||
                        (missionName.indexOf("OCTS") != -1 && (fileType.indexOf("L1") != -1 || fileType.indexOf("Level 1") != -1))) {
                    programName = L2EXTRACT;
                    xmlFileName = L2EXTRACT_XML_FILE;
                }
            }
            setProgramName(programName);
            ocssw.setProgramName(programName);
            ocssw.setXmlFileName(xmlFileName);
            setPrimaryOptions(ParamUtils.getPrimaryOptions(xmlFileName));
        }

    }

    private static class Modis_L1B_Processor extends ProcessorModel {

        Modis_L1B_Processor(String programName, String xmlFileName, OCSSW ocssw) {
            super(programName, xmlFileName, ocssw);
        }

        public boolean updateOFileInfo(String ofileName) {
            updateParamInfo("--okm", ofileName.replaceAll("LAC", "LAC"));
            getParamInfo("--okm").setDefaultValue(getParamValue("--okm"));
            updateParamInfo("--hkm", ofileName.replaceAll("_MODIS.", "_MODIS_HKM."));
            getParamInfo("--hkm").setDefaultValue(getParamValue("--hkm"));
            updateParamInfo("--qkm", ofileName.replaceAll("_MODIS.", "_MODIS_QKM."));
            getParamInfo("--qkm").setDefaultValue(getParamValue("--qkm"));
            updateParamInfo("--obc", ofileName.replaceAll("_MODIS.", "_MODIS_OBC."));
            getParamInfo("--obc").setDefaultValue(getParamValue("--obc"));
            setReadyToRun(ofileName.trim().length() == 0 ? false : true);
            return true;
        }

        public String getOfileName() {

            StringBuilder ofileNameList = new StringBuilder();
            if (!(getParamInfo("--del-okm").getValue().equals("true") || getParamInfo("--del-okm").getValue().equals("1"))) {
                ofileNameList.append("\n" + getParamValue("--okm"));
            }
            if (!(getParamInfo("--del-hkm").getValue().equals("true") || getParamInfo("--del-hkm").getValue().equals("1"))) {
                ofileNameList.append("\n" + getParamValue("--hkm"));
            }
            if (!(getParamInfo("--del-qkm").getValue().equals("true") || getParamInfo("--del-qkm").getValue().equals("1"))) {
                ofileNameList.append("\n" + getParamValue("--qkm"));
            }
            if (getParamInfo("--keep-obc").getValue().equals("true") || getParamInfo("--keep-obc").getValue().equals("1")) {
                ofileNameList.append("\n" + getParamValue("--obc"));
            }
            return ofileNameList.toString();
        }
    }

    private static class LonLat2Pixels_Processor extends ProcessorModel {

        static final String _SWlon = "SWlon";
        static final String _SWlat = "SWlat";
        static final String _NElon = "NElon";
        static final String _NElat = "NElat";

        public static String LON_LAT_2_PIXEL_PROGRAM_NAME = "lonlat2pixel";

        LonLat2Pixels_Processor(String programName, String xmlFileName, OCSSW ocssw) {
            super(programName, xmlFileName, ocssw);
            addPropertyChangeListener("ifile", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                    checkCompleteness();

                }
            });
            addPropertyChangeListener(_SWlon, new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                    checkCompleteness();

                }
            });
            addPropertyChangeListener(_SWlat, new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                    checkCompleteness();

                }
            });
            addPropertyChangeListener(_NElon, new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                    checkCompleteness();

                }
            });
            addPropertyChangeListener(_NElat, new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                    checkCompleteness();
                }
            });
        }

        @Override
        public void checkCompleteness() {
            String valueOfSWlon = getParamList().getInfo(_SWlon).getValue();
            String valueOfSWlat = getParamList().getInfo(_SWlat).getValue();
            String valueOfNElon = getParamList().getInfo(_NElon).getValue();
            String valueOfNElat = getParamList().getInfo(_NElat).getValue();

            if ((valueOfSWlon != null && valueOfSWlon.trim().length() > 0)
                    && (valueOfSWlat != null && valueOfSWlat.trim().length() > 0)
                    && (valueOfNElon != null && valueOfNElon.trim().length() > 0)
                    && (valueOfNElat != null && valueOfNElat.trim().length() > 0)) {
                HashMap<String, String> lonlats = ocssw.computePixelsFromLonLat(this);
                if (lonlats != null) {
                    updateParamInfo(START_PIXEL_PARAM_NAME, lonlats.get(START_PIXEL_PARAM_NAME));
                    updateParamInfo(END_PIXEL_PARAM_NAME, lonlats.get(END_PIXEL_PARAM_NAME));
                    updateParamInfo(START_LINE_PARAM_NAME, lonlats.get(START_LINE_PARAM_NAME));
                    updateParamInfo(END_LINE_PARAM_NAME, lonlats.get(END_LINE_PARAM_NAME));
                    fireEvent(getAllparamInitializedPropertyName(), false, true);
                }
            }
        }

        public void updateParamInfo(String paramName, String newValue) {

            ParamInfo option = getParamInfo(paramName);
            if (option != null) {
                option.setValue(newValue);
            }
        }

        public boolean updateIFileInfo(String ifileName) {
            updateParamInfo(getPrimaryInputFileOptionName(), ifileName);
            ocssw.setIfileName(ifileName);
            return true;
        }
    }


    private void updateL3MapgenParams() {

        if (!"l3mapgen".equalsIgnoreCase(programName)) {
            return;
        }

        ParamInfo ifileParamInfo = paramList.getInfo("ifile");
        if (ifileParamInfo != null) {
            File ifile = new File(ifileParamInfo.getValue());

            if (ifile != null && ifile.exists()) {
                String suite = null;
                ParamInfo suiteParamInfo = paramList.getInfo("suite");
                if (suiteParamInfo != null) {
                    suite = suiteParamInfo.getValue();
                }


                final String L3MAPGEN_PROGRAM_NAME = "l3mapgen";
                File dataDir = SystemUtils.getApplicationDataDir();
                File auxDir = new File(dataDir, "auxdata");
                File l3mapgenAuxDir = new File(auxDir, L3MAPGEN_PROGRAM_NAME);
                l3mapgenAuxDir.mkdirs();

                File auxParFile = new File(l3mapgenAuxDir, L3MAPGEN_PROGRAM_NAME + "_params.par");
                try {
                    createL2binAuxParFile(L3MAPGEN_PROGRAM_NAME, ifile, suite, auxParFile);

                    if (auxParFile.exists()) {
                        boolean precedence = OCSSW_L3mapgenController.getPreferenceAutoFillPrecedence();
                        boolean precedenceNullSuite = OCSSW_L3mapgenController.getPreferenceAutoFillPrecedenceNullSuite();
                        boolean passAll = OCSSW_L3mapgenController.getPreferencePassAll();


                        if (OCSSW_L3mapgenController.getPreferenceAutoFillAll() || OCSSW_L3mapgenController.getPreferenceAutoFillProduct()) {
                            updateParamInfosFromAuxParFile(auxParFile.getAbsolutePath(), "product", OCSSW_L3mapgenController.getPreferenceProduct(), precedence, precedenceNullSuite, passAll);
                            updateParamInfosFromAuxParFile(auxParFile.getAbsolutePath(), "wavelength_3D", OCSSW_L3mapgenController.getPreferenceWavelength3D(), precedence, precedenceNullSuite, passAll);
                        }

                        if (OCSSW_L3mapgenController.getPreferenceAutoFillAll() || OCSSW_L3mapgenController.getPreferenceAutoFillOther()) {
                            updateParamInfosFromAuxParFile(auxParFile.getAbsolutePath(), "projection", OCSSW_L3mapgenController.getPreferenceProjection(), precedence, precedenceNullSuite, passAll);
                            updateParamInfosFromAuxParFile(auxParFile.getAbsolutePath(), "resolution", OCSSW_L3mapgenController.getPreferenceResolution(), precedence, precedenceNullSuite, passAll);
                            updateParamInfosFromAuxParFile(auxParFile.getAbsolutePath(), "width",  "", precedence, precedenceNullSuite, passAll);
                            updateParamInfosFromAuxParFile(auxParFile.getAbsolutePath(), "interp", OCSSW_L3mapgenController.getPreferenceInterp(), precedence, precedenceNullSuite, passAll);
                            updateParamInfosFromAuxParFile(auxParFile.getAbsolutePath(), "fudge", OCSSW_L3mapgenController.getPreferenceFudge(), precedence, precedenceNullSuite, passAll);
                            updateParamInfosFromAuxParFile(auxParFile.getAbsolutePath(), "threshold",  "", precedence, precedenceNullSuite, passAll);
                            updateParamInfosFromAuxParFile(auxParFile.getAbsolutePath(), "mask_land", OCSSW_L3mapgenController.getPreferenceMaskLand(), precedence, precedenceNullSuite, passAll);
                            updateParamInfosFromAuxParFile(auxParFile.getAbsolutePath(), "rgb_land", OCSSW_L3mapgenController.getPreferenceRGBLand(), precedence, precedenceNullSuite, passAll);
                            updateParamInfosFromAuxParFile(auxParFile.getAbsolutePath(), "land",  "", precedence, precedenceNullSuite, passAll);
                            updateParamInfosFromAuxParFile(auxParFile.getAbsolutePath(), "apply_pal", OCSSW_L3mapgenController.getPreferenceApplyPal(), precedence, precedenceNullSuite, passAll);
                            updateParamInfosFromAuxParFile(auxParFile.getAbsolutePath(), "palfile", OCSSW_L3mapgenController.getPreferencePalfile(), precedence, precedenceNullSuite, passAll);
                            updateParamInfosFromAuxParFile(auxParFile.getAbsolutePath(), "datamin", OCSSW_L3mapgenController.getPreferenceDataMin(), precedence, precedenceNullSuite, passAll);
                            updateParamInfosFromAuxParFile(auxParFile.getAbsolutePath(), "datamax", OCSSW_L3mapgenController.getPreferenceDataMax(), precedence, precedenceNullSuite, passAll);
                            updateParamInfosFromAuxParFile(auxParFile.getAbsolutePath(), "scale_type", OCSSW_L3mapgenController.getPreferenceScaleType(), precedence, precedenceNullSuite, passAll);
                            updateParamInfosFromAuxParFile(auxParFile.getAbsolutePath(), "use_transparency", OCSSW_L3mapgenController.getPreferenceUseTransparency(), precedence, precedenceNullSuite, passAll);
                            updateParamInfosFromAuxParFile(auxParFile.getAbsolutePath(), "use_rgb", OCSSW_L3mapgenController.getPreferenceUseRGB(), precedence, precedenceNullSuite, passAll);
                            updateParamInfosFromAuxParFile(auxParFile.getAbsolutePath(), "product_rgb", OCSSW_L3mapgenController.getPreferenceProductRGB(), precedence, precedenceNullSuite, passAll);
                            updateParamInfosFromAuxParFile(auxParFile.getAbsolutePath(), "num_cache", OCSSW_L3mapgenController.getPreferenceNumCache(), precedence, precedenceNullSuite, passAll);
                            updateParamInfosFromAuxParFile(auxParFile.getAbsolutePath(), "oformat", OCSSW_L3mapgenController.getPreferenceOformat(), precedence, precedenceNullSuite, passAll);

                        }
                    }
                } catch (IOException e) {
                    SimpleDialogMessage dialog = new SimpleDialogMessage(L3MAPGEN_PROGRAM_NAME + " - Warning", "Failed to initialize default params from file: " + auxParFile.getAbsolutePath());
                    dialog.setVisible(true);
                    dialog.setEnabled(true);
                }


// todo the xml file for l2bin is badly fomatted for V2025.0, if they fix this then consider using xml file in command block below to potentially fill more parameters

//                    File xmlFile = new File(l2binAuxDir, "l2bin" + "_params.xml");
//
//                    try {
////                        InputStream paramInfoStream = new FileInputStream(xmlFile);
//                        InputStream paramInfoStream = getParamInfoInputStream("l2bin", ifile, suite, xmlFile);
//                        if (auxParFile.exists()) {
//                            updateParamInfosWithXml(paramInfoStream);
//                        }
//                    } catch (IOException e) {
//                        SimpleDialogMessage dialog = new SimpleDialogMessage("l2bin - Warning", "Failed to initialize default params from file: " + xmlFile.getAbsolutePath());
//                        dialog.setVisible(true);
//                        dialog.setEnabled(true);
//                    }
            }
        }


    }

    private void updateL2BinParams() {

        if (!"l2bin".equalsIgnoreCase(programName)) {
            return;
        }

        ParamInfo ifileParamInfo = paramList.getInfo("ifile");
        if (ifileParamInfo != null) {
            File ifile = new File(ifileParamInfo.getValue());
            File textFile = null;


            if (ifile != null && ifile.exists()) {
                String suite = null;
                ParamInfo suiteParamInfo = paramList.getInfo("suite");
                if (suiteParamInfo != null) {
                    suite = suiteParamInfo.getValue();
                }


                if (ifile != null && ifile.exists() && ifile.getName().endsWith(".txt")) {

                    try (BufferedReader br = new BufferedReader(new FileReader(ifile))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            if (line != null && line.trim().length() > 0) {
                                textFile = new File(line.trim());
                            }

                        }
                    } catch (Exception e) {

                    }
                }


                final String L2BIN_PROGRAM_NAME = "l2bin";
                File dataDir = SystemUtils.getApplicationDataDir();
                File auxDir = new File(dataDir, "auxdata");
                File l2binAuxDir = new File(auxDir, L2BIN_PROGRAM_NAME);
                l2binAuxDir.mkdirs();


                File auxParFile = new File(l2binAuxDir, L2BIN_PROGRAM_NAME + "_params.par");
                try {
                    if (textFile != null) {
                        createL2binAuxParFile(L2BIN_PROGRAM_NAME, textFile, suite, auxParFile);
                    } else {
                        createL2binAuxParFile(L2BIN_PROGRAM_NAME, ifile, suite, auxParFile);
                    }
                    if (auxParFile.exists()) {

                        boolean precedence = OCSSW_L2binController.getPreferenceAutoFillPrecedence();
                        boolean precedenceNullSuite = OCSSW_L2binController.getPreferenceAutoFillPrecedenceNullSuite();
                        boolean passAll = OCSSW_L2binController.getPreferencePassAll();

                        if (OCSSW_L2binController.getPreferenceAutoFillAll() || OCSSW_L2binController.getPreferenceFlaguseAutoFillEnable()) {
                            updateParamInfosFromAuxParFile(auxParFile.getAbsolutePath(), "flaguse", OCSSW_L2binController.getPreferenceFlaguse(), precedence, precedenceNullSuite, passAll);
                        }

                        if (OCSSW_L2binController.getPreferenceAutoFillAll() || OCSSW_L2binController.getPreferenceL3bprodAutoFillEnable()) {
                            updateParamInfosFromAuxParFile(auxParFile.getAbsolutePath(), OCSSW_L2binController.PROPERTY_L2BIN_L3BPROD_LABEL, OCSSW_L2binController.getPreferenceL3bprod(), precedence, precedenceNullSuite, passAll);
                            updateParamInfosFromAuxParFile(auxParFile.getAbsolutePath(), "output_wavelengths", OCSSW_L2binController.getPreferenceOutputWavelengths(), precedence, precedenceNullSuite, passAll);
                        }

                        if (OCSSW_L2binController.getPreferenceAutoFillAll() || OCSSW_L2binController.getPreferenceAutoFillEnable()) {
                            updateParamInfosFromAuxParFile(auxParFile.getAbsolutePath(), "resolution", OCSSW_L2binController.getPreferenceResolution(), precedence, precedenceNullSuite, passAll);
                            updateParamInfosFromAuxParFile(auxParFile.getAbsolutePath(), "area_weighting", OCSSW_L2binController.getPreferenceAreaWeighting(), precedence, precedenceNullSuite, passAll);
                            updateParamInfosFromAuxParFile(auxParFile.getAbsolutePath(), "latnorth", OCSSW_L2binController.getPreferenceLatnorth(), precedence, precedenceNullSuite, passAll);
                            updateParamInfosFromAuxParFile(auxParFile.getAbsolutePath(), "latsouth", OCSSW_L2binController.getPreferenceLatsouth(), precedence, precedenceNullSuite, passAll);
                            updateParamInfosFromAuxParFile(auxParFile.getAbsolutePath(), "lonwest", OCSSW_L2binController.getPreferenceLonwest(), precedence, precedenceNullSuite, passAll);
                            updateParamInfosFromAuxParFile(auxParFile.getAbsolutePath(), "loneast", OCSSW_L2binController.getPreferenceLoneast(), precedence, precedenceNullSuite, passAll);
                            updateParamInfosFromAuxParFile(auxParFile.getAbsolutePath(), "sday", OCSSW_L2binController.getPreferenceSday(), precedence, precedenceNullSuite, passAll);
                            updateParamInfosFromAuxParFile(auxParFile.getAbsolutePath(), "eday", OCSSW_L2binController.getPreferenceEday(), precedence, precedenceNullSuite, passAll);
                            updateParamInfosFromAuxParFile(auxParFile.getAbsolutePath(), "night", OCSSW_L2binController.getPreferenceNight(), precedence, precedenceNullSuite, passAll);
                            updateParamInfosFromAuxParFile(auxParFile.getAbsolutePath(), "delta_crossing_time", OCSSW_L2binController.getPreferenceDeltaCross(), precedence, precedenceNullSuite, passAll);
                            updateParamInfosFromAuxParFile(auxParFile.getAbsolutePath(), "prodtype", OCSSW_L2binController.getPreferenceProdtype(), precedence, precedenceNullSuite, passAll);
                            updateParamInfosFromAuxParFile(auxParFile.getAbsolutePath(), "rowgroup", OCSSW_L2binController.getPreferenceRowGroup(), precedence, precedenceNullSuite, passAll);
                            updateParamInfosFromAuxParFile(auxParFile.getAbsolutePath(), "composite_prod", OCSSW_L2binController.getPreferenceCompositeProd(), precedence, precedenceNullSuite, passAll);
                            updateParamInfosFromAuxParFile(auxParFile.getAbsolutePath(), "composite_scheme", OCSSW_L2binController.getPreferenceCompositeScheme(), precedence, precedenceNullSuite, passAll);
                            updateParamInfosFromAuxParFile(auxParFile.getAbsolutePath(), "qual_prod", "", precedence, precedenceNullSuite, passAll);
                            updateParamInfosFromAuxParFile(auxParFile.getAbsolutePath(), "qual_max", "", precedence, precedenceNullSuite, passAll);
                            updateParamInfosFromAuxParFile(auxParFile.getAbsolutePath(), "minobs", "", precedence, precedenceNullSuite, passAll);
                        }
                    }
                } catch (IOException e) {
                    SimpleDialogMessage dialog = new SimpleDialogMessage(L2BIN_PROGRAM_NAME + " - Warning", "Failed to initialize default params from file: " + auxParFile.getAbsolutePath());
                    dialog.setVisible(true);
                    dialog.setEnabled(true);
                }


// todo the xml file for l2bin is badly fomatted for V2025.0, if they fix this then consider using xml file in command block below to potentially fill more parameters

//                    File xmlFile = new File(l2binAuxDir, "l2bin" + "_params.xml");
//
//                    try {
////                        InputStream paramInfoStream = new FileInputStream(xmlFile);
//                        InputStream paramInfoStream = getParamInfoInputStream("l2bin", ifile, suite, xmlFile);
//                        if (auxParFile.exists()) {
//                            updateParamInfosWithXml(paramInfoStream);
//                        }
//                    } catch (IOException e) {
//                        SimpleDialogMessage dialog = new SimpleDialogMessage("l2bin - Warning", "Failed to initialize default params from file: " + xmlFile.getAbsolutePath());
//                        dialog.setVisible(true);
//                        dialog.setEnabled(true);
//                    }
            }
        }


        // OLD CODE
//            String currentFlagUse = SeadasFileUtils.getKeyValueFromParFile(new File(missionDir, parFileName), "flaguse");
//            if (currentFlagUse == null) {
//                currentFlagUse = DEFAULT_FLAGUSE;
//            }
//            if (currentFlagUse != null) {
//                ArrayList<ParamValidValueInfo> validValues = getParamInfo("flaguse").getValidValueInfos();
//                for (ParamValidValueInfo paramValidValueInfo : validValues) {
//                    if (currentFlagUse.contains(paramValidValueInfo.getValue().trim())) {
//                        paramValidValueInfo.setSelected(true);
//                    } else {
//                        paramValidValueInfo.setSelected(false);
//                    }
//                }
//                super.updateParamInfo("flaguse", currentFlagUse);
//                fireEvent("flaguse", null, currentFlagUse);
//            }
    }


    private void createL2binAuxParFile(String thisProgramName, File ifile, String suite, File parfile) throws IOException {

        if (parfile.exists()) {
            parfile.delete();
        }

        String executable = thisProgramName;
        if (thisProgramName.equalsIgnoreCase(programName)) {
//            System.out.println("I am " + thisProgramName);
        }
//        System.out.println("programName=" + programName);

        ProcessorModel processorModel = new ProcessorModel(executable, ocssw);

        processorModel.setAcceptsParFile(true);

        processorModel.setAcceptsParFile(true);
        processorModel.addParamInfo("ifile", ifile.getAbsolutePath(), ParamInfo.Type.IFILE);

        if (suite != null) {
            processorModel.addParamInfo("suite", suite, ParamInfo.Type.STRING);
        }


        processorModel.addParamInfo("-dump_options_paramfile", parfile.getAbsolutePath(), ParamInfo.Type.OFILE);

        try {
            Process p = ocssw.executeSimple(processorModel);
            ocssw.waitForProcess();
            if (ocssw instanceof OCSSWLocal) {
                File tmpParFileToDel = new File(ParFileManager.tmpParFileToDelString);
                tmpParFileToDel.delete();
            }

            if (ocssw.getProcessExitValue() != 0) {
                throw new IOException(thisProgramName + " failed to run");
            }

            ocssw.getIntermediateOutputFiles(processorModel);

            if (!parfile.exists()) {
                //SeadasLogger.getLogger().severe("l2gen can't find paramInfo.xml file!");
                Dialogs.showError("SEVERE: " + parfile.getAbsolutePath() + " not found!");

                throw new IOException("problem creating Parameter file: " + parfile.getAbsolutePath());
            } else {
//                    return new FileInputStream(parfile);
            }

        } catch (IOException e) {
            throw new IOException("problem creating Parameter file: " + parfile.getAbsolutePath());
        }
    }


    public void updateParamInfosFromAuxParFile(String parfile, String parameter, String prefValue, boolean precedence, boolean precedenceNullSuite) throws IOException {
        updateParamInfosFromAuxParFile(parfile, parameter, prefValue, precedence, precedenceNullSuite, false);
    }


    public void updateParamInfosFromAuxParFile(String parfile, String parameter, String prefValue, boolean precedence, boolean precedenceNullSuite, boolean passAll) throws IOException {

        if (parameter == null || parameter.trim().length() == 0) {
            return;
        }
        
        boolean suiteIsSet = false;
        ParamInfo suiteParamInfo = paramList.getInfo("suite");
        if (suiteParamInfo != null && suiteParamInfo.getValue() != null && suiteParamInfo.getValue().trim().length() > 0) {
            suiteIsSet = true;
        }


        ParamInfo paramInfo = paramList.getInfo(parameter);

        if (paramInfo == null) {
            return;
        }

        if (passAll) {
            paramInfo.setDefaultValue("");
        } else {
            paramInfo.setDefaultValue(paramInfo.getDefaultValueOriginal());
        }

        boolean paramValueSet = false;

        // todo this handles suite precedence
        String valueOriginal = paramInfo.getValue();
        if (valueOriginal == null) {
            valueOriginal = "";
        }

        String valueNew = "";

        if (prefValue != null && prefValue.length() > 0) {
            if (!prefValue.equals(paramInfo.getValue())) {
                valueNew = prefValue;
//                updateParamInfo(parameter, prefValue);
//                fireEvent(parameter, paramInfo.getValue(), prefValue);
            }

        } else {
            if (!valueOriginal.equals(paramInfo.getDefaultValueOriginal())) {
                valueNew = paramInfo.getDefaultValueOriginal();
//                updateParamInfo(parameter, paramInfo.getDefaultValueOriginal());
//                fireEvent(parameter, valueOriginal, paramInfo.getDefaultValueOriginal());
            }
        }


        if (prefValue != null && prefValue.length() > 0) {
            if (suiteIsSet) {
                if (precedence) {
                    paramValueSet = true;
                }
            } else {
                if (precedenceNullSuite) {
                    paramValueSet = true;
                }
            }
        }



        BufferedReader br = new BufferedReader(new FileReader(parfile));
        try {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();


            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();

                if (line != null) {
                    String[] values = line.split("=", 2);
                    if (values != null && values.length == 2) {
                        String name = values[0].trim();
                        String value = values[1].trim();

                        if (!paramValueSet && parameter.equals(name)) {
                            String originalValue = paramInfo.getValue();

                            if ("boolean".equals(paramInfo.getType())) {
                                if ("1".equals(value) || "true".equalsIgnoreCase(value)) {
                                    value = "true";
                                } else {
                                    value = "false";
                                }
                                originalValue = "";
                            }

                            if (!passAll) {
                                paramInfo.setDefaultValue(value);
                            }

                            if (!paramValueSet) {
                                valueNew = value;
//                                updateParamInfo(parameter, value);
//                                fireEvent(parameter, originalValue, value);
                                break;
                            }
                        }
                    }
                }

            }
        } finally {
            br.close();
        }

        updateParamInfo(parameter, valueNew);
        fireEvent(parameter, valueOriginal, valueNew);

        return;
    }


    private void updateSuite(File selectedFile) {

        File fileTmp = null;

        if (selectedFile.getName().endsWith(".txt")) {
            try (BufferedReader br = new BufferedReader(new FileReader(selectedFile))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line != null && line.trim().length() > 0 && !line.trim().startsWith("#") && !line.trim().startsWith("//")) {
                        fileTmp = new File(line.trim());
                        if (fileTmp != null) {
                            break;
                        }
                    }
                }
            } catch (Exception e) {
            }
        } else {
        }

        if (fileTmp != null) {
            selectedFile = fileTmp;
        }


        FileInfo ifileInfo = new FileInfo(selectedFile, ocssw);


        File missionDir = ifileInfo.getMissionDirectory();
        File subSensorDir = ifileInfo.getSubsensorDirectory();

        File commonDir = null;
        if (missionDir != null && missionDir.getParentFile() != null) {
            commonDir = new File(missionDir.getParentFile().getAbsolutePath(), "common");
        }

        String[] suitesByMission = null;
        String[] suitesByCommon = null;
        String[] suitesBySubSensor = null;
        ArrayList<String> suitesArrayList = new ArrayList();

        try {
            suitesByMission = getSuites(selectedFile, missionDir, programName);
            if (suitesByMission != null && suitesByMission.length > 0) {
                for (String suite : suitesByMission) {
                    if (suite != null && suite.trim().length() > 0) {
                        suitesArrayList.add(suite);
                    }
                }
            }
        } catch (Exception e) {
        }

        try {
            suitesByCommon = getSuites(selectedFile, commonDir, programName);
            if (suitesByCommon != null && suitesByCommon.length > 0) {
                for (String suite : suitesByCommon) {
                    if (suite != null && suite.trim().length() > 0) {
                        suitesArrayList.add(suite);
                    }
                }
            }
        } catch (Exception e) {
        }

        try {
            suitesBySubSensor = getSuites(selectedFile, subSensorDir, programName);
            if (suitesBySubSensor != null && suitesBySubSensor.length > 0) {
                for (String suite : suitesBySubSensor) {
                    if (suite != null && suite.trim().length() > 0) {
                        suitesArrayList.add(suite);
                    }
                }
            }
        } catch (Exception e) {
        }


        String[] suites = new String[suitesArrayList.size()];
        suitesArrayList.toArray(suites);
        Arrays.sort(suites);


        ArrayList<String> suitesArrayListCleaned = new ArrayList();
        for (String suite : suites) {
            suitesArrayListCleaned.add(suite);
        }
        String[] suitesCleaned = new String[suitesArrayListCleaned.size()];
        suitesArrayListCleaned.toArray(suitesCleaned);


        String suiteName;
        ArrayList<ParamValidValueInfo> suiteValidValues = new ArrayList<ParamValidValueInfo>();
        for (String fileName : suitesCleaned) {
            suiteName = fileName.substring(fileName.indexOf("_", fileName.indexOf("_") + 1) + 1, fileName.indexOf("."));
            suiteValidValues.add(new ParamValidValueInfo(suiteName));
        }
        ArrayList<ParamValidValueInfo> oldValidValues = (ArrayList<ParamValidValueInfo>) getParamInfo("suite").getValidValueInfos().clone();
        getParamInfo("suite").setValidValueInfos(suiteValidValues);
        fireEvent("suite", oldValidValues, suiteValidValues);
        // todo commenting out this to test
//            updateFlagUse(DEFAULT_PAR_FILE_NAME);
    }


    private String[] getSuites(File selectedFile, File missionDir, String programName) {
        FileInfo ifileInfo = new FileInfo(selectedFile, ocssw);

        if (missionDir == null) {
            try {
                LineNumberReader reader = new LineNumberReader(new FileReader(selectedFile));
                String sampleFileName = reader.readLine();
                missionDir = new FileInfo(sampleFileName).getMissionDirectory();
            } catch (FileNotFoundException fnfe) {

            } catch (IOException ioe) {

            }

        }


        String[] suites = null;
        HashMap<String, Boolean> missionSuites;
        if (OCSSWInfo.getInstance().getOcsswLocation().equals(OCSSWInfo.OCSSW_LOCATION_LOCAL)) {
            suites = missionDir.list(new FilenameFilter() {
                @Override

                public boolean accept(File file, String s) {
                    if ("l2bin".equalsIgnoreCase(programName)) {
                        return s.contains("l2bin_defaults_");
                    }
                    if ("l3mapgen".equalsIgnoreCase(programName)) {
                        return s.contains("l3mapgen_defaults_");
                    }
                    return false;
                }

            });
        } else {
            OCSSWClient ocsswClient = new OCSSWClient();
            WebTarget target = ocsswClient.getOcsswWebTarget();
            if ("l2bin".equalsIgnoreCase(programName)) {
                missionSuites = target.path("ocssw").path("l2bin_suites").path(ifileInfo.getMissionName()).request(MediaType.APPLICATION_JSON)
                        .get(new GenericType<HashMap<String, Boolean>>() {
                        });
            } else if ("l3mapgen".equalsIgnoreCase(programName)) {
                missionSuites = target.path("ocssw").path("l3mapgen_suites").path(ifileInfo.getMissionName()).request(MediaType.APPLICATION_JSON)
                        .get(new GenericType<HashMap<String, Boolean>>() {
                        });
            } else {
                return suites;
            }
            int i = 0;
            suites = new String[missionSuites.size()];
            for (Map.Entry<String, Boolean> entry : missionSuites.entrySet()) {
                String missionName = entry.getKey();
                Boolean missionStatus = entry.getValue();

                if (missionStatus) {
                    suites[i++] = missionName;
                }

            }
        }

        return suites;
    }


    private static class L2Bin_Processor extends ProcessorModel {

        private static final String DEFAULT_PAR_FILE_NAME = "l2bin_defaults.par";
        private static final String PAR_FILE_PREFIX = "l2bin_defaults_";
        String DEFAULT_FLAGUSE;
        File missionDir;
        FileInfo ifileInfo;

        L2Bin_Processor(String programName, String xmlFileName, OCSSW ocssw) {
            super(programName, xmlFileName, ocssw);
            setProdPramName("l3bprod");
            setMultipleInputFiles(true);
            missionDir = null;


            addPropertyChangeListener("resolution", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {

                    String ifileName = getParamValue(getPrimaryInputFileOptionName());
                    String resolution = getParamValue("resolution");
                    String l3bprod = getParamValue("l3bprod");
                    String suite = getParamValue("suite");
                    String prodtype = getParamValue("prodtype");
                    String north = getParamValue("latnorth");
                    String south = getParamValue("latsouth");
                    String west = getParamValue("lonwest");
                    String east = getParamValue("loneast");

                    String ofileName = getOfileForL2BinWrapper(ifileName, getOcssw(), programName, resolution, l3bprod, suite, prodtype, north, south, west, east);

                    updateOFileInfo(ofileName);
                }
            });

            addPropertyChangeListener("l3bprod", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {

                    String ifileName = getParamValue(getPrimaryInputFileOptionName());
                    String resolution = getParamValue("resolution");
                    String l3bprod = getParamValue("l3bprod");
                    String suite = getParamValue("suite");
                    String prodtype = getParamValue("prodtype");
                    String north = getParamValue("latnorth");
                    String south = getParamValue("latsouth");
                    String west = getParamValue("lonwest");
                    String east = getParamValue("loneast");

                    String ofileName = getOfileForL2BinWrapper(ifileName, getOcssw(), programName, resolution, l3bprod, suite, prodtype, north, south, west, east);

                    updateOFileInfo(ofileName);
                }
            });

            addPropertyChangeListener("suite", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {

                    String ifileName = getParamValue(getPrimaryInputFileOptionName());
                    String resolution = getParamValue("resolution");
                    String l3bprod = getParamValue("l3bprod");
                    String suite = getParamValue("suite");
                    String prodtype = getParamValue("prodtype");
                    String north = getParamValue("latnorth");
                    String south = getParamValue("latsouth");
                    String west = getParamValue("lonwest");
                    String east = getParamValue("loneast");

                    String ofileName = getOfileForL2BinWrapper(ifileName, getOcssw(), programName, resolution, l3bprod, suite, prodtype, north, south, west, east);

                    updateOFileInfo(ofileName);
                }
            });

            addPropertyChangeListener("prodtype", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {

                    String ifileName = getParamValue(getPrimaryInputFileOptionName());
                    String resolution = getParamValue("resolution");
                    String l3bprod = getParamValue("l3bprod");
                    String suite = getParamValue("suite");
                    String prodtype = getParamValue("prodtype");
                    String north = getParamValue("latnorth");
                    String south = getParamValue("latsouth");
                    String west = getParamValue("lonwest");
                    String east = getParamValue("loneast");

                    String ofileName = getOfileForL2BinWrapper(ifileName, getOcssw(), programName, resolution, l3bprod, suite, prodtype, north, south, west, east);

                    updateOFileInfo(ofileName);
                }
            });

            addPropertyChangeListener("latnorth", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {

                    String ifileName = getParamValue(getPrimaryInputFileOptionName());
                    String resolution = getParamValue("resolution");
                    String l3bprod = getParamValue("l3bprod");
                    String suite = getParamValue("suite");
                    String prodtype = getParamValue("prodtype");
                    String north = getParamValue("latnorth");
                    String south = getParamValue("latsouth");
                    String west = getParamValue("lonwest");
                    String east = getParamValue("loneast");

                    String ofileName = getOfileForL2BinWrapper(ifileName, getOcssw(), programName, resolution, l3bprod, suite, prodtype, north, south, west, east);

                    updateOFileInfo(ofileName);
                }
            });


            addPropertyChangeListener("latsouth", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {

                    String ifileName = getParamValue(getPrimaryInputFileOptionName());
                    String resolution = getParamValue("resolution");
                    String l3bprod = getParamValue("l3bprod");
                    String suite = getParamValue("suite");
                    String prodtype = getParamValue("prodtype");
                    String north = getParamValue("latnorth");
                    String south = getParamValue("latsouth");
                    String west = getParamValue("lonwest");
                    String east = getParamValue("loneast");

                    String ofileName = getOfileForL2BinWrapper(ifileName, getOcssw(), programName, resolution, l3bprod, suite, prodtype, north, south, west, east);

                    updateOFileInfo(ofileName);
                }
            });


            addPropertyChangeListener("lonwest", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {

                    String ifileName = getParamValue(getPrimaryInputFileOptionName());
                    String resolution = getParamValue("resolution");
                    String l3bprod = getParamValue("l3bprod");
                    String suite = getParamValue("suite");
                    String prodtype = getParamValue("prodtype");
                    String north = getParamValue("latnorth");
                    String south = getParamValue("latsouth");
                    String west = getParamValue("lonwest");
                    String east = getParamValue("loneast");

                    String ofileName = getOfileForL2BinWrapper(ifileName, getOcssw(), programName, resolution, l3bprod, suite, prodtype, north, south, west, east);

                    updateOFileInfo(ofileName);
                }
            });


            addPropertyChangeListener("loneast", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {

                    String ifileName = getParamValue(getPrimaryInputFileOptionName());
                    String resolution = getParamValue("resolution");
                    String l3bprod = getParamValue("l3bprod");
                    String suite = getParamValue("suite");
                    String prodtype = getParamValue("prodtype");
                    String north = getParamValue("latnorth");
                    String south = getParamValue("latsouth");
                    String west = getParamValue("lonwest");
                    String east = getParamValue("loneast");

                    String ofileName = getOfileForL2BinWrapper(ifileName, getOcssw(), programName, resolution, l3bprod, suite, prodtype, north, south, west, east);

                    updateOFileInfo(ofileName);
                }
            });

        }

        @Override
        public void updateParamValues(Product selectedProduct) {
            if (selectedProduct != null) {
                String sampleFileName = selectedProduct.getFileLocation().getAbsolutePath();
                ifileInfo = new FileInfo(sampleFileName);
                if (ifileInfo.getMissionId().equals(MissionInfo.Id.UNKNOWN)) {
                    try (BufferedReader br = new BufferedReader(new FileReader(sampleFileName))) {
                        String listedFileName;
                        while ((listedFileName = br.readLine()) != null) {
                            ifileInfo = new FileInfo(listedFileName);
                            if (!ifileInfo.getMissionId().equals(MissionInfo.Id.UNKNOWN)) {
                                break;
                            }
                        }
                    } catch (Exception e) {

                    }
                }
                missionDir = ifileInfo.getMissionDirectory();
                if (missionDir == null) {
                    try {
                        LineNumberReader reader = new LineNumberReader(new FileReader(new File(selectedProduct.getFileLocation().getAbsolutePath())));
                        sampleFileName = reader.readLine();
                        missionDir = new FileInfo(sampleFileName).getMissionDirectory();
                    } catch (FileNotFoundException fnfe) {

                    } catch (IOException ioe) {

                    }

                }
                DEFAULT_FLAGUSE = SeadasFileUtils.getKeyValueFromParFile(new File(missionDir, DEFAULT_PAR_FILE_NAME), "flaguse");
                updateSuite();
                super.updateParamValues(new File(sampleFileName));
            }
        }

        private void updateSuite() {

            String[] suites;
            HashMap<String, Boolean> missionSuites;
            if (OCSSWInfo.getInstance().getOcsswLocation().equals(OCSSWInfo.OCSSW_LOCATION_LOCAL)) {
                suites = missionDir.list(new FilenameFilter() {
                    @Override
                    public boolean accept(File file, String s) {
                        return s.contains("l2bin_defaults_");
                    }
                });
            } else {
                OCSSWClient ocsswClient = new OCSSWClient();
                WebTarget target = ocsswClient.getOcsswWebTarget();
                missionSuites = target.path("ocssw").path("l2bin_suites").path(ifileInfo.getMissionName()).request(MediaType.APPLICATION_JSON)
                        .get(new GenericType<HashMap<String, Boolean>>() {
                        });
                int i = 0;
                suites = new String[missionSuites.size()];
                for (Map.Entry<String, Boolean> entry : missionSuites.entrySet()) {
                    String missionName = entry.getKey();
                    Boolean missionStatus = entry.getValue();

                    if (missionStatus) {
                        suites[i++] = missionName;
                    }

                }
            }
            String suiteName;
            ArrayList<ParamValidValueInfo> suiteValidValues = new ArrayList<ParamValidValueInfo>();
            for (String fileName : suites) {
                suiteName = fileName.substring(fileName.indexOf("_", fileName.indexOf("_") + 1) + 1, fileName.indexOf("."));
                suiteValidValues.add(new ParamValidValueInfo(suiteName));
            }
            ArrayList<ParamValidValueInfo> oldValidValues = (ArrayList<ParamValidValueInfo>) getParamInfo("suite").getValidValueInfos().clone();
            getParamInfo("suite").setValidValueInfos(suiteValidValues);
            fireEvent("suite", oldValidValues, suiteValidValues);
            // todo commenting out this to test
//            updateFlagUse(DEFAULT_PAR_FILE_NAME);
        }

//        @Override
//        public void updateParamInfo(ParamInfo currentOption, String newValue) {
//
////            if (currentOption.getName().equals("suite")) {
////                updateFlagUse(PAR_FILE_PREFIX + newValue + ".par");
////            }
//            super.updateParamInfo(currentOption, newValue);
//            if (currentOption.getName().equals("suite")  || currentOption.getName().equals("ifile")) {
//                updateFlagUse(PAR_FILE_PREFIX + newValue + ".par");
//            }
//        }

//        private void updateFlagUse(String parFileName) {
//
//            ParamInfo ifileParamInfo = paramList.getInfo("ifile");
//            if (ifileParamInfo != null) {
//                File ifile = new File(ifileParamInfo.getValue());
//
//                if (ifile != null && ifile.exists()) {
//                    String suite = null;
//                    ParamInfo suiteParamInfo = paramList.getInfo("suite");
//                    if (suiteParamInfo != null) {
//                        suite = suiteParamInfo.getValue();
//                    }
//
//                    final String L2BIN_PROGRAM_NAME = "l2bin";
//                    File dataDir = SystemUtils.getApplicationDataDir();
//                    File l2binAuxDir = new File(dataDir, L2BIN_PROGRAM_NAME);
//                    l2binAuxDir.mkdirs();
//
//                    File auxParFile = new File(l2binAuxDir, L2BIN_PROGRAM_NAME + "_params.par");
//                    try {
//                        createL2binAuxParFile(L2BIN_PROGRAM_NAME, ifile, suite, auxParFile);
//                        if (auxParFile.exists()) {
//                            updateParamInfosFromL2binAuxParFile(auxParFile.getAbsolutePath());
//                        }
//                    } catch (IOException e) {
//                        SimpleDialogMessage dialog = new SimpleDialogMessage(L2BIN_PROGRAM_NAME + " - Warning", "Failed to initialize default params from file: " + auxParFile.getAbsolutePath());
//                        dialog.setVisible(true);
//                        dialog.setEnabled(true);
//                    }
//
//
//// todo the xml file for l2bin is badly fomatted for V2025.0, if they fix this then consider using xml file in command block below to potentially fill more parameters
//
////                    File xmlFile = new File(l2binAuxDir, "l2bin" + "_params.xml");
////
////                    try {
//////                        InputStream paramInfoStream = new FileInputStream(xmlFile);
////                        InputStream paramInfoStream = getParamInfoInputStream("l2bin", ifile, suite, xmlFile);
////                        if (auxParFile.exists()) {
////                            updateParamInfosWithXml(paramInfoStream);
////                        }
////                    } catch (IOException e) {
////                        SimpleDialogMessage dialog = new SimpleDialogMessage("l2bin - Warning", "Failed to initialize default params from file: " + xmlFile.getAbsolutePath());
////                        dialog.setVisible(true);
////                        dialog.setEnabled(true);
////                    }
//                }
//            }
//
//
//
//            // OLD CODE
////            String currentFlagUse = SeadasFileUtils.getKeyValueFromParFile(new File(missionDir, parFileName), "flaguse");
////            if (currentFlagUse == null) {
////                currentFlagUse = DEFAULT_FLAGUSE;
////            }
////            if (currentFlagUse != null) {
////                ArrayList<ParamValidValueInfo> validValues = getParamInfo("flaguse").getValidValueInfos();
////                for (ParamValidValueInfo paramValidValueInfo : validValues) {
////                    if (currentFlagUse.contains(paramValidValueInfo.getValue().trim())) {
////                        paramValidValueInfo.setSelected(true);
////                    } else {
////                        paramValidValueInfo.setSelected(false);
////                    }
////                }
////                super.updateParamInfo("flaguse", currentFlagUse);
////                fireEvent("flaguse", null, currentFlagUse);
////            }
//        }
//
//
//
//
//
//
//        private void createL2binAuxParFile(String thisProgramName, File ifile, String suite, File parfile) throws IOException {
//
//            if (parfile.exists()) {
//                parfile.delete();
//            }
//
//            String executable = thisProgramName;
//            if (thisProgramName.equalsIgnoreCase(programName)) {
//                System.out.println("I am " + thisProgramName);
//            }
//            System.out.println("programName=" + programName);
//
//            ProcessorModel processorModel = new ProcessorModel(executable, ocssw);
//
//            processorModel.setAcceptsParFile(true);
//
//            processorModel.setAcceptsParFile(true);
//            processorModel.addParamInfo("ifile", ifile.getAbsolutePath(), ParamInfo.Type.IFILE);
//
//            if (suite != null) {
//                processorModel.addParamInfo("suite", suite, ParamInfo.Type.STRING);
//            }
//
//
//            processorModel.addParamInfo("-dump_options_paramfile", parfile.getAbsolutePath(), ParamInfo.Type.OFILE);
//
//            try {
//                Process p = ocssw.executeSimple(processorModel);
//                ocssw.waitForProcess();
//                if (ocssw instanceof OCSSWLocal) {
//                    File tmpParFileToDel = new File(ParFileManager.tmpParFileToDelString);
//                    tmpParFileToDel.delete();
//                }
//
//                if (ocssw.getProcessExitValue() != 0) {
//                    throw new IOException(thisProgramName + " failed to run");
//                }
//
//                ocssw.getIntermediateOutputFiles(processorModel);
//
//                if (!parfile.exists()) {
//                    //SeadasLogger.getLogger().severe("l2gen can't find paramInfo.xml file!");
//                    Dialogs.showError("SEVERE: " + parfile.getAbsolutePath() + " not found!");
//
//                    throw new IOException("problem creating Parameter file: " + parfile.getAbsolutePath());
//                } else {
////                    return new FileInputStream(parfile);
//                }
//
//            } catch (IOException e) {
//                throw new IOException("problem creating Parameter file: " + parfile.getAbsolutePath());
//            }
//        }
//
//
//
//
//        public void updateParamInfosFromL2binAuxParFile(String parfile) throws IOException  {
//
//            BufferedReader br = new BufferedReader(new FileReader(parfile));
//            try {
//                StringBuilder sb = new StringBuilder();
//                String line = br.readLine();
//
//                while (line != null) {
//                    sb.append(line);
//                    sb.append(System.lineSeparator());
//                    line = br.readLine();
//                    System.out.println(line);
//
//                    if (line != null) {
//                        String[] values = line.split("=");
//                        if (values != null && values.length == 2) {
//                            String name = values[0].trim();
//                            String value = values[1].trim();
//                            System.out.println("name=" + name + "  value=" + value);
//
//                            if ("flaguse".equals(name)) {
//                                ParamInfo flaguseParamInfo = paramList.getInfo("flaguse");
//                                String originalFlaguse = flaguseParamInfo.getValue();
////                                flaguseParamInfo.setValue(value);
//                                super.updateParamInfo("flaguse", value);
//                                fireEvent("flaguse", originalFlaguse, value);
//                            }
//                        }
//                    }
//
//                }
////                String everything = sb.toString();
//            } finally {
//                br.close();
//
//            }
//
//        }


        private InputStream getParamInfoInputStream(String thisProgramName, File ifile, String suite, File xmlFile) throws IOException {

            if (xmlFile.exists()) {
                xmlFile.delete();
            }

            String executable = thisProgramName;
            if (thisProgramName.equalsIgnoreCase(programName)) {
//                System.out.println("I am " + thisProgramName);
            }
//            System.out.println("programName=" + programName);

            ProcessorModel processorModel = new ProcessorModel(executable, ocssw);

            processorModel.setAcceptsParFile(true);

            processorModel.setAcceptsParFile(true);
            processorModel.addParamInfo("ifile", ifile.getAbsolutePath(), ParamInfo.Type.IFILE);

            if (suite != null) {
                processorModel.addParamInfo("suite", suite, ParamInfo.Type.STRING);
            }


            processorModel.addParamInfo("-dump_options_xmlfile", xmlFile.getAbsolutePath(), ParamInfo.Type.OFILE);

            try {
                Process p = ocssw.executeSimple(processorModel);
                ocssw.waitForProcess();
                if (ocssw instanceof OCSSWLocal) {
                    File tmpParFileToDel = new File(ParFileManager.tmpParFileToDelString);
                    tmpParFileToDel.delete();
                }

                if (ocssw.getProcessExitValue() != 0) {
                    throw new IOException(thisProgramName + " failed to run");
                }

                ocssw.getIntermediateOutputFiles(processorModel);

                if (!xmlFile.exists()) {
                    //SeadasLogger.getLogger().severe("l2gen can't find paramInfo.xml file!");
                    Dialogs.showError("SEVERE: " + xmlFile.getAbsolutePath() + " not found!");

                    return null;
                } else {
                    return new FileInputStream(xmlFile);
                }

            } catch (IOException e) {
                throw new IOException("problem creating Parameter XML file: " + e.getMessage());
            }
        }


        public void updateParamInfosWithXml(InputStream stream) throws IOException {
            XmlReader reader = new XmlReader();
            if (reader != null) {
                Element rootElement = reader.parseAndGetRootElement(stream);

                if (rootElement == null) {
                    throw new IOException();
                }

                NodeList optionNodelist = rootElement.getElementsByTagName("option");

                if (optionNodelist != null && optionNodelist.getLength() > 0) {
                    for (int i = 0; i < optionNodelist.getLength(); i++) {

                        Element optionElement = (Element) optionNodelist.item(i);

                        String name = XmlReader.getTextValue(optionElement, "name");

                        if (name != null) {
                            name = name.toLowerCase();
                            String value = XmlReader.getTextValue(optionElement, "value");

                            if (value == null || value.length() == 0) {
                                value = XmlReader.getTextValue(optionElement, "default");
                            }

                            if ("flaguse".equals(name)) {

                                ParamInfo flaguseParamInfo = paramList.getInfo("flaguse");
                                flaguseParamInfo.setValue(value);
                            }
                        }
                    }
                }
            }
        }


    }

    private static class L2BinAquarius_Processor extends ProcessorModel {

        L2BinAquarius_Processor(String programName, String xmlFileName, OCSSW ocssw) {
            super(programName, xmlFileName, ocssw);
            setMultipleInputFiles(true);
        }
    }

    private static class L3Bin_Processor extends ProcessorModel {

        L3Bin_Processor(String programName, String xmlFileName, OCSSW ocssw) {
            super(programName, xmlFileName, ocssw);
            setMultipleInputFiles(true);
            addPropertyChangeListener("prod", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                    String ifileName = getParamValue(getPrimaryInputFileOptionName());
                    String resolve = getParamValue("resolve");
                    String prod = getParamValue("prod");
                    String north = getParamValue("latnorth");
                    String south = getParamValue("latsouth");
                    String west = getParamValue("lonwest");
                    String east = getParamValue("loneast");

                    String ofileName = getOfileForL3BinWrapper(ifileName, getOcssw(), programName, resolve, prod, north, south, west, east);

                    updateOFileInfo(ofileName);
                }
            });

            addPropertyChangeListener("resolve", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                    String ifileName = getParamValue(getPrimaryInputFileOptionName());
                    String resolve = getParamValue("resolve");
                    String prod = getParamValue("prod");
                    String north = getParamValue("latnorth");
                    String south = getParamValue("latsouth");
                    String west = getParamValue("lonwest");
                    String east = getParamValue("loneast");

                    String ofileName = getOfileForL3BinWrapper(ifileName, getOcssw(), programName, resolve, prod, north, south, west, east);

                    updateOFileInfo(ofileName);
                }
            });

            addPropertyChangeListener("latnorth", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                    String ifileName = getParamValue(getPrimaryInputFileOptionName());
                    String resolve = getParamValue("resolve");
                    String prod = getParamValue("prod");
                    String north = getParamValue("latnorth");
                    String south = getParamValue("latsouth");
                    String west = getParamValue("lonwest");
                    String east = getParamValue("loneast");

                    String ofileName = getOfileForL3BinWrapper(ifileName, getOcssw(), programName, resolve, prod, north, south, west, east);

                    updateOFileInfo(ofileName);
                }
            });


            addPropertyChangeListener("latsouth", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                    String ifileName = getParamValue(getPrimaryInputFileOptionName());
                    String resolve = getParamValue("resolve");
                    String prod = getParamValue("prod");
                    String north = getParamValue("latnorth");
                    String south = getParamValue("latsouth");
                    String west = getParamValue("lonwest");
                    String east = getParamValue("loneast");

                    String ofileName = getOfileForL3BinWrapper(ifileName, getOcssw(), programName, resolve, prod, north, south, west, east);

                    updateOFileInfo(ofileName);
                }
            });


            addPropertyChangeListener("lonwest", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                    String ifileName = getParamValue(getPrimaryInputFileOptionName());
                    String resolve = getParamValue("resolve");
                    String prod = getParamValue("prod");
                    String north = getParamValue("latnorth");
                    String south = getParamValue("latsouth");
                    String west = getParamValue("lonwest");
                    String east = getParamValue("loneast");

                    String ofileName = getOfileForL3BinWrapper(ifileName, getOcssw(), programName, resolve, prod, north, south, west, east);

                    updateOFileInfo(ofileName);
                }
            });

            addPropertyChangeListener("loneast", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                    String ifileName = getParamValue(getPrimaryInputFileOptionName());
                    String resolve = getParamValue("resolve");
                    String prod = getParamValue("prod");
                    String north = getParamValue("latnorth");
                    String south = getParamValue("latsouth");
                    String west = getParamValue("lonwest");
                    String east = getParamValue("loneast");

                    String ofileName = getOfileForL3BinWrapper(ifileName, getOcssw(), programName, resolve, prod, north, south, west, east);

                    updateOFileInfo(ofileName);
                }
            });

        }
    }


    private static class L3BinDump_Processor extends ProcessorModel {

        L3BinDump_Processor(String programName, String xmlFileName, OCSSW ocssw) {
            super(programName, xmlFileName, ocssw);
        }

        @Override
        public boolean updateIFileInfo(String ifileName) {

            File ifile = new File(ifileName);

            inputFileInfo = new FileInfo(ifile.getParent(), ifile.getName(), ocssw);

            if (inputFileInfo.isTypeId(FileTypeInfo.Id.L3BIN)) {
                isIfileValid = true;
                updateParamInfo(getPrimaryInputFileOptionName(), ifileName + "\n");
                updateParamValues(new File(ifileName));

            } else {
                isIfileValid = false;
                removePropertyChangeListeners(getPrimaryInputFileOptionName());
            }

            setReadyToRun(isIfileValid);
            return isIfileValid;

        }
    }


    private static class SMIGEN_Processor extends ProcessorModel {

        SMIGEN_Processor(final String programName, String xmlFileName, OCSSW ocssw) {
            super(programName, xmlFileName, ocssw);
            setOpenInSeadas(true);
            addPropertyChangeListener("prod", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                    String ifileName = getParamValue(getPrimaryInputFileOptionName());
                    if (ifileName != null) {
                        String oldProdValue = (String) propertyChangeEvent.getOldValue();
                        String newProdValue = (String) propertyChangeEvent.getNewValue();
                        String[] additionalOptions = {"--suite=" + newProdValue, "--resolution=" + getParamValue("resolution")};
                        //String ofileName = SeadasFileUtils.findNextLevelFileName(getParamValue(getPrimaryInputFileOptionName()), programName, additionalOptions);
                        String ofileName = ocssw.getOfileName(ifileName, additionalOptions);
                        updateOFileInfo(ofileName);
                    }
                }
            });

            addPropertyChangeListener("resolution", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                    String ifileName = getParamValue(getPrimaryInputFileOptionName());
                    String oldResolutionValue = (String) propertyChangeEvent.getOldValue();
                    String newResolutionValue = (String) propertyChangeEvent.getNewValue();
                    String suite = getParamValue("prod");
                    if (suite == null || suite.trim().length() == 0) {
                        suite = "all";
                    }
                    String[] additionalOptions = {"--resolution=" + newResolutionValue, "--suite=" + suite};
                    //String ofileName = SeadasFileUtils.findNextLevelFileName(getParamValue(getPrimaryInputFileOptionName()), programName, additionalOptions);
                    String ofileName = ocssw.getOfileName(ifileName, additionalOptions);
                    updateOFileInfo(ofileName);
                }
            });
        }

    }


    private static String getOfileForL2BinOcssw(String ifileOriginal, String ofilenameDefault) {

        // add the path
        File file = new File(ifileOriginal);
        if (file != null) {
            if (file.getParentFile() != null) {
                String parentPath = file.getParentFile().getAbsolutePath();
                File file2 = new File(parentPath, ofilenameDefault);
                if (file2 != null) {
                    ofilenameDefault = file2.getAbsolutePath();
                }
            }
        }
//

        String ofilename = stripFilenameExtension(ofilenameDefault);


        return ofilename;
    }


    private static String getOfileForL3MapGenOcssw(String ifileOriginal, String ofilenameDefault, String resolution, String oformat, String product, String projection) {

        // add the path
        File file = new File(ifileOriginal);
        if (file != null) {
            String parentPath = file.getParentFile().getAbsolutePath();
            File file2 = new File(parentPath, ofilenameDefault);
            if (file2 != null) {
                ofilenameDefault = file2.getAbsolutePath();
            }
        }
//

        String ofilename = stripFilenameExtension(ofilenameDefault);

//        String ofilename = ofilenameDefault;
//
//        if (ofilenameDefault.endsWith(".nc")) {
//            ofilename = ofilenameDefault.substring(0,ofilenameDefault.length() - 3);
//        }

//        ofilename += getOfileForL3MapGenAddOns(resolution, product, projection);
//
//        // todo maybe check ofile against ifile
//
//        ofilename = getOfileForL3MapGenAddExtension(ofilename, oformat);

        return ofilename;
    }


    private static String getOfileForL3MapGenAddExtension(String ofilename, String oformat) {

        if (oformat == null || oformat.trim().length() == 0) {
            oformat = "NETCDF4";
        }
        oformat = oformat.toUpperCase();

        switch (oformat) {
            case "NETCDF4":
                ofilename += ".nc";
                break;
            case "HDF4":
                ofilename += ".hdf";
                break;
            case "PNG":
                ofilename += ".png";
                break;
            case "PPM":
                ofilename += ".ppm";
                break;
            case "TIFF":
                ofilename += ".tiff";
                break;
            default:
                ofilename += ".nc";
                break;

        }

        return ofilename;
    }


    private static String getOfileForL3BinAddOns(String resolution, String prod, String north, String south, String west, String east) {

        if (OCSSW_L3binController.OFILE_NAMING_SCHEME_SUFFIX_NONE.equals(OCSSW_L3binController.getPreferenceOfileNamingSchemeSuffixOptions())) {
            return "";
        }

        String keyString;
        if (OCSSW_L3binController.OFILE_NAMING_SCHEME_SUFFIX1.equals(OCSSW_L3binController.getPreferenceOfileNamingSchemeSuffixOptions())) {
            keyString = OCSSW_L3binController.getPreferenceOfileNamingSchemeSuffix1();
        } else if (OCSSW_L3binController.OFILE_NAMING_SCHEME_SUFFIX2.equals(OCSSW_L3binController.getPreferenceOfileNamingSchemeSuffixOptions())) {
            keyString = OCSSW_L3binController.getPreferenceOfileNamingSchemeSuffix2();
        } else if (OCSSW_L3binController.OFILE_NAMING_SCHEME_SUFFIX_DEFAULT.equals(OCSSW_L3binController.getPreferenceOfileNamingSchemeSuffixOptions())) {
            keyString = OCSSW_L3binController.OFILE_NAMING_SCHEME_SUFFIX_DEFAULT;
        } else if (OCSSW_L3binController.OFILE_NAMING_SCHEME_SUFFIX_DEFAULT2.equals(OCSSW_L3binController.getPreferenceOfileNamingSchemeSuffixOptions())) {
            keyString = OCSSW_L3binController.OFILE_NAMING_SCHEME_SUFFIX_DEFAULT2;
//        } else if (OCSSW_L3binController.OFILE_NAMING_SCHEME_SUFFIX_DEFAULT3.equals(OCSSW_L3binController.getPreferenceOfileNamingSchemeSuffixOptions())) {
//            keyString = OCSSW_L3binController.OFILE_NAMING_SCHEME_SUFFIX_DEFAULT3;
//        } else if (OCSSW_L3binController.OFILE_NAMING_SCHEME_SUFFIX_DEFAULT4.equals(OCSSW_L3binController.getPreferenceOfileNamingSchemeSuffixOptions())) {
//            keyString = OCSSW_L3binController.OFILE_NAMING_SCHEME_SUFFIX_DEFAULT4;
        } else {
            keyString = OCSSW_L3binController.OFILE_NAMING_SCHEME_SUFFIX_DEFAULT;
        }


        if (keyString == null || keyString.trim().length() == 0) {
            return "";
        }


        if (checkForVariantMatch(keyString, "prod") || checkForVariantMatch(keyString, "PROD") ||
                checkForVariantMatch(keyString, "product") || checkForVariantMatch(keyString, "PRODUCT")
        ) {
            String productSingle = "";
            if (prod != null && prod.trim().length() > 0) {
                String[] productsArray = prod.split("[,\\s]");
                if (productsArray != null && productsArray.length == 1) {
                    if (productsArray[0] != null) {
                        productSingle = productsArray[0].trim();
                        if (productSingle.length() > 0) {
                            productSingle = productsArray[0];
                        }
                    }
                }
            }
            keyString = replaceAnyKeyStringVariant(keyString, "prod", productSingle, DELIMITOR_STRING);
            keyString = replaceAnyKeyStringVariant(keyString, "product", productSingle, DELIMITOR_STRING);
            keyString = replaceAnyKeyStringVariant(keyString, "PROD", productSingle.toUpperCase(), DELIMITOR_STRING);
            keyString = replaceAnyKeyStringVariant(keyString, "PRODUCT", productSingle.toUpperCase(), DELIMITOR_STRING);
        }


        if (checkForVariantMatch(keyString, "prod_list") || checkForVariantMatch(keyString, "PROD_LIST") ||
                checkForVariantMatch(keyString, "product_list") || checkForVariantMatch(keyString, "PRODUCT_LIST")
        ) {
            String productList = "";
            if (prod != null && prod.trim().length() > 0) {
                String[] productsArray = prod.split("[,\\s]");

                for (String currProduct : productsArray) {
                    if (keyString.toLowerCase().contains("[.prod_list]") || keyString.toLowerCase().contains("[.product_list]")) {
                        productList += "." + currProduct;
                    } else if (keyString.toLowerCase().contains("[-prod_list]") || keyString.toLowerCase().contains("[-product_list]")) {
                        productList += "-" + currProduct;
                    } else {
                        productList += "_" + currProduct;
                    }
                }
            }
            keyString = replaceAnyKeyStringVariant(keyString, "prod_list", productList, DELIMITOR_STRING);
            keyString = replaceAnyKeyStringVariant(keyString, "product_list", productList, DELIMITOR_STRING);
            keyString = replaceAnyKeyStringVariant(keyString, "PROD_LIST", productList.toUpperCase(), DELIMITOR_STRING);
            keyString = replaceAnyKeyStringVariant(keyString, "PRODUCT_LIST", productList.toUpperCase(), DELIMITOR_STRING);
        }


        keyString = getOfileAddOnResolutionL2BinL3Bin(resolution, keyString);


        keyString = keystringReplaceNSWE(keyString, north, south, west, east);

        return keyString;
    }


    private static String getOfileForL2BinAddOns(String resolution, String l3bprod, String suite, String prodtype, String north, String south, String west, String east) {

        if (OCSSW_L2binController.OFILE_NAMING_SCHEME_SUFFIX_NONE.equals(OCSSW_L2binController.getPreferenceOfileNamingSchemeSuffixOptions())) {
            return "";
        }

        String keyString;
        if (OCSSW_L2binController.OFILE_NAMING_SCHEME_SUFFIX1.equals(OCSSW_L2binController.getPreferenceOfileNamingSchemeSuffixOptions())) {
            keyString = OCSSW_L2binController.getPreferenceOfileNamingSchemeSuffix1();
        } else if (OCSSW_L2binController.OFILE_NAMING_SCHEME_SUFFIX2.equals(OCSSW_L2binController.getPreferenceOfileNamingSchemeSuffixOptions())) {
            keyString = OCSSW_L2binController.getPreferenceOfileNamingSchemeSuffix2();
        } else if (OCSSW_L2binController.OFILE_NAMING_SCHEME_SUFFIX_DEFAULT.equals(OCSSW_L2binController.getPreferenceOfileNamingSchemeSuffixOptions())) {
            keyString = OCSSW_L2binController.OFILE_NAMING_SCHEME_SUFFIX_DEFAULT;
        } else if (OCSSW_L2binController.OFILE_NAMING_SCHEME_SUFFIX_DEFAULT2.equals(OCSSW_L2binController.getPreferenceOfileNamingSchemeSuffixOptions())) {
            keyString = OCSSW_L2binController.OFILE_NAMING_SCHEME_SUFFIX_DEFAULT2;
        } else if (OCSSW_L2binController.OFILE_NAMING_SCHEME_SUFFIX_DEFAULT3.equals(OCSSW_L2binController.getPreferenceOfileNamingSchemeSuffixOptions())) {
            keyString = OCSSW_L2binController.OFILE_NAMING_SCHEME_SUFFIX_DEFAULT3;
        } else if (OCSSW_L2binController.OFILE_NAMING_SCHEME_SUFFIX_DEFAULT4.equals(OCSSW_L2binController.getPreferenceOfileNamingSchemeSuffixOptions())) {
            keyString = OCSSW_L2binController.OFILE_NAMING_SCHEME_SUFFIX_DEFAULT4;
        } else {
            keyString = OCSSW_L2binController.OFILE_NAMING_SCHEME_SUFFIX_DEFAULT;
        }


        if (keyString == null || keyString.trim().length() == 0) {
            return "";
        }


        if (checkForVariantMatch(keyString, "l3bprod") || checkForVariantMatch(keyString, "L3BPROD") ||
                checkForVariantMatch(keyString, "product") || checkForVariantMatch(keyString, "PRODUCT")
        ) {
            String productSingle = "";
            if (l3bprod != null && l3bprod.trim().length() > 0) {
                String[] productsArray = l3bprod.split("[,\\s]");
                if (productsArray != null && productsArray.length == 1) {
                    if (productsArray[0] != null) {
                        productSingle = productsArray[0].trim();
                        if (productSingle.length() > 0) {
                            productSingle = productsArray[0];
                        }
                    }
                }
            }
            keyString = replaceAnyKeyStringVariant(keyString, "l3bprod", productSingle, DELIMITOR_STRING);
            keyString = replaceAnyKeyStringVariant(keyString, "product", productSingle, DELIMITOR_STRING);
            keyString = replaceAnyKeyStringVariant(keyString, "L3BPROD", productSingle.toUpperCase(), DELIMITOR_STRING);
            keyString = replaceAnyKeyStringVariant(keyString, "PRODUCT", productSingle.toUpperCase(), DELIMITOR_STRING);
        }


        if (checkForVariantMatch(keyString, "l3bprod_list") || checkForVariantMatch(keyString, "L3BPROD_LIST") ||
                checkForVariantMatch(keyString, "product_list") || checkForVariantMatch(keyString, "PRODUCT_LIST")
        ) {
            String productList = "";
            if (l3bprod != null && l3bprod.trim().length() > 0) {
                String[] productsArray = l3bprod.split("[,\\s]");

                for (String currProduct : productsArray) {
                    if (keyString.toLowerCase().contains("[.l3bprod_list]") || keyString.toLowerCase().contains("[.product_list]")) {
                        productList += "." + currProduct;
                    } else if (keyString.toLowerCase().contains("[-l3bprod_list]") || keyString.toLowerCase().contains("[-product_list]")) {
                        productList += "-" + currProduct;
                    } else {
                        productList += "_" + currProduct;
                    }
                }
            }
            keyString = replaceAnyKeyStringVariant(keyString, "l3bprod_list", productList, DELIMITOR_STRING);
            keyString = replaceAnyKeyStringVariant(keyString, "product_list", productList, DELIMITOR_STRING);
            keyString = replaceAnyKeyStringVariant(keyString, "L3BPROD_LIST", productList.toUpperCase(), DELIMITOR_STRING);
            keyString = replaceAnyKeyStringVariant(keyString, "PRODUCT_LIST", productList.toUpperCase(), DELIMITOR_STRING);
        }


        keyString = getOfileAddOnResolutionL2BinL3Bin(resolution, keyString);


        if (checkForVariantMatch(keyString, "prodtype") || checkForVariantMatch(keyString, "PRODTYPE")) {
            if (prodtype == null) {
                prodtype = "";
            }
            keyString = replaceAnyKeyStringVariant(keyString, "prodtype", prodtype, DELIMITOR_STRING);
            keyString = replaceAnyKeyStringVariant(keyString, "PRODTYPE", prodtype.toUpperCase(), DELIMITOR_STRING);
        }


        if (checkForVariantMatch(keyString, "suite") || checkForVariantMatch(keyString, "SUITE")) {
            if (suite == null) {
                suite = "";
            }
            keyString = replaceAnyKeyStringVariant(keyString, "suite", suite, DELIMITOR_STRING);
            keyString = replaceAnyKeyStringVariant(keyString, "SUITE", suite.toUpperCase(), DELIMITOR_STRING);
        }

        keyString = keystringReplaceNSWE(keyString, north, south, west, east);


        return keyString;
    }


    private static String getOfileAddOnResolutionL2BinL3Bin(String resolution, String keyString) {


        if (checkForVariantMatch(keyString, "resolution") || checkForVariantMatch(keyString, "RESOLUTION")) {
            if (resolution == null) {
                resolution = "";
            }
            keyString = replaceAnyKeyStringVariant(keyString, "resolution", resolution, DELIMITOR_NUMBER);
            keyString = replaceAnyKeyStringVariant(keyString, "RESOLUTION", resolution.toUpperCase(), DELIMITOR_NUMBER);
        }


        if (checkForVariantMatch(keyString, "resolve") || checkForVariantMatch(keyString, "RESOLVE")) {
            if (resolution == null) {
                resolution = "";
            }
            keyString = replaceAnyKeyStringVariant(keyString, "resolve", resolution, DELIMITOR_NUMBER);
            keyString = replaceAnyKeyStringVariant(keyString, "RESOLVE", resolution.toUpperCase(), DELIMITOR_NUMBER);
        }


        if (checkForVariantMatch(keyString, "resolution_units") || checkForVariantMatch(keyString, "RESOLUTION_UNITS")
                || checkForVariantMatch(keyString, "resolve_units") || checkForVariantMatch(keyString, "RESOLVE_UNITS")
        ) {
            if (resolution == null) {
                resolution = "";
            }

            String resolution_units = resolution;
            switch (resolution) {
                case "HH":
                    resolution_units = "50m";
                    break;
                case "HQ":
                    resolution_units = "100m";
                    break;
                case "Q":
                    resolution_units = "250m";
                    break;
                case "H":
                    resolution_units = "500m";
                    break;
                case "1":
                    resolution_units = "1.1km";
                    break;
                case "2":
                    resolution_units = "2.3km";
                    break;
                case "4":
                    resolution_units = "4.6km";
                    break;
                case "9":
                    resolution_units = "9.2km";
                    break;
                case "18":
                    resolution_units = "18.5km";
                    break;
                case "36":
                    resolution_units = "36km";
                    break;
                case "QD":
                    resolution_units = "0.25degree";
                    break;
                case "HD":
                    resolution_units = "0.5degree";
                    break;
                case "1D":
                    resolution_units = "1degree";
                    break;
            }

            keyString = replaceAnyKeyStringVariant(keyString, "resolution_units", resolution_units, DELIMITOR_NUMBER);
            keyString = replaceAnyKeyStringVariant(keyString, "resolve_units", resolution_units, DELIMITOR_NUMBER);
            keyString = replaceAnyKeyStringVariant(keyString, "RESOLUTION_UNITS", resolution_units.toUpperCase(), DELIMITOR_NUMBER);
            keyString = replaceAnyKeyStringVariant(keyString, "RESOLVE_UNITS", resolution_units.toUpperCase(), DELIMITOR_NUMBER);
        }

        return keyString;
    }


    private static String getOfileForL3MapGenAddOns(String resolution, String product, String projection, String interp, String north, String south, String west, String east) {


        if (OCSSW_L3mapgenController.OFILE_NAMING_SCHEME_SUFFIX_NONE.equals(OCSSW_L3mapgenController.getPreferenceOfileNamingSchemeSuffixOptions())) {
            return "";
        }

        String keyString;
        if (OCSSW_L3mapgenController.OFILE_NAMING_SCHEME_SUFFIX1.equals(OCSSW_L3mapgenController.getPreferenceOfileNamingSchemeSuffixOptions())) {
            keyString = OCSSW_L3mapgenController.getPreferenceOfileNamingSchemeSuffix1();
        } else if (OCSSW_L3mapgenController.OFILE_NAMING_SCHEME_SUFFIX2.equals(OCSSW_L3mapgenController.getPreferenceOfileNamingSchemeSuffixOptions())) {
            keyString = OCSSW_L3mapgenController.getPreferenceOfileNamingSchemeSuffix2();
        } else if (OCSSW_L3mapgenController.OFILE_NAMING_SCHEME_SUFFIX_DEFAULT.equals(OCSSW_L3mapgenController.getPreferenceOfileNamingSchemeSuffixOptions())) {
            keyString = OCSSW_L3mapgenController.OFILE_NAMING_SCHEME_SUFFIX_DEFAULT;
        } else if (OCSSW_L3mapgenController.OFILE_NAMING_SCHEME_SUFFIX_DEFAULT2.equals(OCSSW_L3mapgenController.getPreferenceOfileNamingSchemeSuffixOptions())) {
            keyString = OCSSW_L3mapgenController.OFILE_NAMING_SCHEME_SUFFIX_DEFAULT2;
        } else if (OCSSW_L3mapgenController.OFILE_NAMING_SCHEME_SUFFIX_DEFAULT3.equals(OCSSW_L3mapgenController.getPreferenceOfileNamingSchemeSuffixOptions())) {
            keyString = OCSSW_L3mapgenController.OFILE_NAMING_SCHEME_SUFFIX_DEFAULT3;
        } else if (OCSSW_L3mapgenController.OFILE_NAMING_SCHEME_SUFFIX_DEFAULT4.equals(OCSSW_L3mapgenController.getPreferenceOfileNamingSchemeSuffixOptions())) {
            keyString = OCSSW_L3mapgenController.OFILE_NAMING_SCHEME_SUFFIX_DEFAULT4;
        } else {
            keyString = OCSSW_L3mapgenController.OFILE_NAMING_SCHEME_SUFFIX_DEFAULT;
        }


        if (keyString == null || keyString.trim().length() == 0) {
            return "";
        }


        ;


        if (checkForVariantMatch(keyString, "product") || checkForVariantMatch(keyString, "PRODUCT")) {
            String productSingle = "";
            if (product != null && product.trim().length() > 0) {
                String[] productsArray = product.split("[,\\s]");
                if (productsArray != null && productsArray.length == 1) {
                    if (productsArray[0] != null) {
                        productSingle = productsArray[0].trim();
                        if (productSingle.length() > 0) {
                            productSingle = productsArray[0];
                        }
                    }
                }
            }
            keyString = replaceAnyKeyStringVariant(keyString, "product", productSingle, DELIMITOR_STRING);
            keyString = replaceAnyKeyStringVariant(keyString, "PRODUCT", productSingle.toUpperCase(), DELIMITOR_STRING);
        }


        if (checkForVariantMatch(keyString, "product_list") || checkForVariantMatch(keyString, "PRODUCT_LIST")) {
            String productList = "";
            if (product != null && product.trim().length() > 0) {
                String[] productsArray = product.split("[,\\s]");

                for (String currProduct : productsArray) {
                    if (keyString.toLowerCase().contains("[.product_list]")) {
                        productList += "." + currProduct;
                    } else if (keyString.toLowerCase().contains("[-product_list]")) {
                        productList += "-" + currProduct;
                    } else {
                        productList += "_" + currProduct;
                    }
                }
            }

            keyString = replaceAnyKeyStringVariant(keyString, "product_list", productList, DELIMITOR_STRING);
            keyString = replaceAnyKeyStringVariant(keyString, "PRODUCT_LIST", productList.toUpperCase(), DELIMITOR_STRING);
        }


        if (checkForVariantMatch(keyString, "resolution") || checkForVariantMatch(keyString, "RESOLUTION")) {
            if (resolution == null) {
                resolution = "";
            }
            keyString = replaceAnyKeyStringVariant(keyString, "resolution", resolution, DELIMITOR_NUMBER);
            keyString = replaceAnyKeyStringVariant(keyString, "RESOLUTION", resolution.toUpperCase(), DELIMITOR_NUMBER);
        }

        if (checkForVariantMatch(keyString, "resolution_units") || checkForVariantMatch(keyString, "RESOLUTION_UNITS")) {
            if (resolution == null) {
                resolution = "";
            }

            resolution = resolution.trim();
            if (isNumeric(resolution)) {
                resolution = resolution + "m";
            }
            keyString = replaceAnyKeyStringVariant(keyString, "resolution_units", resolution, DELIMITOR_NUMBER);
            keyString = replaceAnyKeyStringVariant(keyString, "RESOLUTION_UNITS", resolution.toUpperCase(), DELIMITOR_NUMBER);
        }


        if (checkForVariantMatch(keyString, "projection") || checkForVariantMatch(keyString, "PROJECTION")) {
            String projectionName = "";
            if (projection != null && projection.trim().length() > 0) {
                String[] projectionArray = projection.split("[\\s]");

                if (projectionArray.length > 1) {
                    String[] projectionArray2 = projectionArray[0].split("=");
                    if (projectionArray2.length > 1) {
                        projectionName = projectionArray2[1];
                    } else {
                        projectionName = projectionArray[0];
                    }
                } else {
                    projectionName = projection;
                }
            }

            keyString = replaceAnyKeyStringVariant(keyString, "projection", projectionName, DELIMITOR_STRING);
            keyString = replaceAnyKeyStringVariant(keyString, "PROJECTION", projectionName.toUpperCase(), DELIMITOR_STRING);
        }


        if (checkForVariantMatch(keyString, "interp") || checkForVariantMatch(keyString, "INTERP")) {
            if (interp == null) {
                interp = "";
            }
            keyString = replaceAnyKeyStringVariant(keyString, "interp", interp, DELIMITOR_STRING);
            keyString = replaceAnyKeyStringVariant(keyString, "INTERP", interp.toUpperCase(), DELIMITOR_STRING);
        }


        keyString = keystringReplaceNSWE(keyString, north, south, west, east);

        return keyString;
    }


    public static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch(NumberFormatException e){
            return false;
        }
    }

    private static String keystringReplaceNSWE(String keyString, String north, String south, String west, String east) {

        //    [_nswe]  [_nswe°] [_NSWE°] [_nswedegrees]


        // make sure key is uppercase
        keyString = convertAnyUpperCaseKeyToLowerCase(keyString, "north");
        keyString = convertAnyUpperCaseKeyToLowerCase(keyString, "north°");
        keyString = convertAnyUpperCaseKeyToLowerCase(keyString, "north_deg");

        keyString = convertAnyUpperCaseKeyToLowerCase(keyString, "south");
        keyString = convertAnyUpperCaseKeyToLowerCase(keyString, "south°");
        keyString = convertAnyUpperCaseKeyToLowerCase(keyString, "south_deg");

        keyString = convertAnyUpperCaseKeyToLowerCase(keyString, "west");
        keyString = convertAnyUpperCaseKeyToLowerCase(keyString, "west°");
        keyString = convertAnyUpperCaseKeyToLowerCase(keyString, "west_deg");

        keyString = convertAnyUpperCaseKeyToLowerCase(keyString, "east");
        keyString = convertAnyUpperCaseKeyToLowerCase(keyString, "east°");
        keyString = convertAnyUpperCaseKeyToLowerCase(keyString, "east_deg");

        keyString = convertAnyUpperCaseKeyToLowerCase(keyString, "nswe");
        keyString = convertAnyUpperCaseKeyToLowerCase(keyString, "nswe°");
        keyString = convertAnyUpperCaseKeyToLowerCase(keyString, "nswe_deg");


        if (checkForVariantMatch(keyString, "north")
                || checkForVariantMatch(keyString, "north°")
                || checkForVariantMatch(keyString, "north_deg")
        ) {
            if (north == null) {
                north = "";
            }
            keyString = replaceAnyKeyStringVariant(keyString, "north", north, null, "N", DELIMITOR_NUMBER);
            keyString = replaceAnyKeyStringVariant(keyString, "north°", north, null, "°N", DELIMITOR_NUMBER);
            keyString = replaceAnyKeyStringVariant(keyString, "north_deg", north, null, "°N", DELIMITOR_NUMBER);
        }


        if (checkForVariantMatch(keyString, "south")
                || checkForVariantMatch(keyString, "south°")
                || checkForVariantMatch(keyString, "south_deg")
        ) {
            if (south == null) {
                south = "";
            }
            keyString = replaceAnyKeyStringVariant(keyString, "south", south, null, "S", DELIMITOR_NUMBER);
            keyString = replaceAnyKeyStringVariant(keyString, "south°", south, null, "°S", DELIMITOR_NUMBER);
            keyString = replaceAnyKeyStringVariant(keyString, "south_deg", south, null, "°S", DELIMITOR_NUMBER);
        }


        if (checkForVariantMatch(keyString, "west")
                || checkForVariantMatch(keyString, "west°")
                || checkForVariantMatch(keyString, "west_deg")
        ) {
            if (west == null) {
                west = "";
            }
            keyString = replaceAnyKeyStringVariant(keyString, "west", west, null, "W", DELIMITOR_NUMBER);
            keyString = replaceAnyKeyStringVariant(keyString, "west°", west, null, "°W", DELIMITOR_NUMBER);
            keyString = replaceAnyKeyStringVariant(keyString, "west_deg", west, null, "°W", DELIMITOR_NUMBER);
        }


        if (checkForVariantMatch(keyString, "east")
                || checkForVariantMatch(keyString, "east°")
                || checkForVariantMatch(keyString, "east_deg")
        ) {
            if (east == null) {
                east = "";
            }
            keyString = replaceAnyKeyStringVariant(keyString, "east", east, null, "E", DELIMITOR_NUMBER);
            keyString = replaceAnyKeyStringVariant(keyString, "east°", east, null, "°E", DELIMITOR_NUMBER);
            keyString = replaceAnyKeyStringVariant(keyString, "east_deg", east, null, "°E", DELIMITOR_NUMBER);
        }


        if (checkForVariantMatch(keyString, "nswe")
                || checkForVariantMatch(keyString, "nswe°")
                || checkForVariantMatch(keyString, "nswe_deg")
                || checkForVariantMatch(keyString, "nsew")
                || checkForVariantMatch(keyString, "nsew°")
                || checkForVariantMatch(keyString, "nsew_deg°")
        ) {
            if (north == null) {
                north = "";
            }
            if (south == null) {
                south = "";
            }
            if (west == null) {
                west = "";
            }
            if (east == null) {
                east = "";
            }

            String nswe = "";
            String nsweDeg = "";
            String nsweDegSymbol = "";
            String nsew = "";
            String nsewDeg = "";
            String nsewDegSymbol = "";
            
            if (north.length() > 0) {
                nswe = nswe + "_" + north + "N";
                nsweDeg = nsweDeg + "_" + north + "degN";
                nsweDegSymbol = nsweDegSymbol + "_" + north + "°N";

                nsew = nsew + "_" + north + "N";
                nsewDeg = nsewDeg + "_" + north + "degN";
                nsewDegSymbol = nsewDegSymbol + "_" + north + "°N";
            }
            
            if (south.length() > 0) {
                nswe = nswe + "_" + south + "S";
                nsweDeg = nsweDeg + "_" + south + "degS";
                nsweDegSymbol = nsweDegSymbol + "_" + south + "°S";

                nsew = nsew + "_" + south + "S";
                nsewDeg = nsewDeg + "_" + south + "degS";
                nsewDegSymbol = nsewDegSymbol + "_" + south + "°S";
            }
            
            if (west.length() > 0) {
                nswe = nswe + "_" + west + "W";
                nsweDeg = nsweDeg + "_" + west + "degW";
                nsweDegSymbol = nsweDegSymbol + "_" + west + "°W";
            }
            if (east.length() > 0) {
                nswe = nswe + "_" + east + "E";
                nsweDeg = nsweDeg + "_" + east + "degE";
                nsweDegSymbol = nsweDegSymbol + "_" + east + "°E";

                nsew = nsew + "_" + east + "E";
                nsewDeg = nsewDeg + "_" + east + "degE";
                nsewDegSymbol = nsewDegSymbol + "_" + east + "°E";
            }
            
            if (west.length() > 0) {
                nsew = nsew + "_" + west + "W";
                nsewDeg = nsewDeg + "_" + west + "degW";
                nsewDegSymbol = nsewDegSymbol + "_" + west + "°W";
            }
            

            if (nswe.length() > 0) {
                nswe = nswe.substring(1);
            }
            if (nsweDeg.length() > 0) {
                nsweDeg = nsweDeg.substring(1);
            }
            if (nsweDegSymbol.length() > 0) {
                nsweDegSymbol = nsweDegSymbol.substring(1);
            }

            if (nsew.length() > 0) {
                nsew = nsew.substring(1);
            }
            if (nsewDeg.length() > 0) {
                nsewDeg = nsewDeg.substring(1);
            }
            if (nsewDegSymbol.length() > 0) {
                nsewDegSymbol = nsewDegSymbol.substring(1);
            }
            

            keyString = replaceAnyKeyStringVariant(keyString, "nswe", nswe, DELIMITOR_NUMBER);
            keyString = replaceAnyKeyStringVariant(keyString, "nswe_deg", nsweDeg, DELIMITOR_NUMBER);
            keyString = replaceAnyKeyStringVariant(keyString, "nswe°", nsweDegSymbol, DELIMITOR_NUMBER);
            keyString = replaceAnyKeyStringVariant(keyString, "nsew", nsew, DELIMITOR_NUMBER);
            keyString = replaceAnyKeyStringVariant(keyString, "nsew_deg", nsewDeg, DELIMITOR_NUMBER);
            keyString = replaceAnyKeyStringVariant(keyString, "nsew°", nsewDegSymbol, DELIMITOR_NUMBER);
        }

        return keyString;
    }


    private static String convertAnyUpperCaseKeyToLowerCase(String keyString, String key) {

        String keyUpperCase = key.toUpperCase();
        keyString = keyString.replace("[" + keyUpperCase + "]", "[" + key + "]");
        keyString = keyString.replace("[." + keyUpperCase + "]", "[." + key + "]");
        keyString = keyString.replace("[_" + keyUpperCase + "]", "[_" + key + "]");
        keyString = keyString.replace("[-" + keyUpperCase + "]", "[-" + key + "]");
        keyString = keyString.replace("[keyUpperCase]", "[key]");

        return keyString;
    }


    private static String replaceAnyKeyStringVariant(String keyString, String key, String value, String delimitorDefault) {
        return replaceAnyKeyStringVariant(keyString, key, value, null, null, delimitorDefault);
    }

    private static String replaceAnyKeyStringVariant(String keyString, String key, String value, String prefix, String suffix, String delimitorDefault) {

        if (value == null) {
            value = "";
        }

        if (value.length() > 0) {
            if (prefix != null) {
                value = prefix + value;
            }
            if (suffix != null) {
                value = value + suffix;
            }
        }
        value = value.trim();

        if (value.length() > 0) {
            keyString = keyString.replace("[" + key + "]", delimitorDefault + value);  // default
            keyString = keyString.replace("[." + key + "]", "." + value);
            keyString = keyString.replace("[_" + key + "]", "_" + value);
            keyString = keyString.replace("[-" + key + "]", "-" + value);
        } else {
            keyString = keyString.replace("[" + key + "]", "");
            keyString = keyString.replace("[." + key + "]", "");
            keyString = keyString.replace("[_" + key + "]", "");
            keyString = keyString.replace("[-" + key + "]", "");
        }

        keyString = keyString.replace("[-" + key + "]", "-" + value);

        return keyString;
    }

    private static boolean checkForVariantMatch(String keyString, String key) {

        if (keyString.contains("[" + key + "]") || keyString.contains("[." + key + "]") || keyString.contains("[_" + key + "]") || keyString.contains("[-" + key + "]")) {
            return true;
        }

        return false;
    }


    private static String trimStringChars(String string, String key, boolean trimStart, boolean trimEnd, boolean trimDuplicates) {

        String stringOriginal = string;

        if (string == null || key == null) {
            return stringOriginal;
        }

        string = string.trim();
        key = key.trim();

        if (string.length() == 0 || key.length() == 0) {
            return stringOriginal;
        }


        while (trimDuplicates && string.contains(key + key)) {
            string = string.replace((key + key), key);
        }


        if (trimStart && string.startsWith(".")) {
            string = string.substring(1, string.length());
        }

        if (trimEnd && string.endsWith(".")) {
            string = string.substring(0, string.length() - 1);
        }


        return string;

    }

    private static String stripFilenameExtension(String filename) {
        if (filename == null || filename.trim().length() == 0) {
            return filename;
        }

        String fileBasename = filename;

        if (filename.endsWith(".nc")) {
            fileBasename = filename.substring(0, filename.length() - 3);
        }

        return fileBasename;
    }

    private static String stripFilenameExtractExtension(String filename, String extension) {
        if (filename == null || filename.trim().length() == 0 || extension == null || extension.trim().length() == 0) {
            return filename;
        }

        String fileBasename = filename;

        if (filename.endsWith(extension)) {
            fileBasename = filename.substring(0, filename.length() - extension.length());
        }

        return fileBasename;
    }


    private static String stripPalFilenameExtension(String filename) {
        if (filename == null || filename.trim().length() == 0) {
            return filename;
        }

        String fileBasename = filename;

        if (filename.endsWith(".pal")) {
            fileBasename = filename.substring(0, filename.length() - 4);
        }

        return fileBasename;
    }


    public static String getOfileForL3BinWrapper(String ifileName, OCSSW ocssw, String programName, String resolve, String prod, String north, String south, String west, String east) {
        String ifileBaseName = stripFilenameExtension(ifileName);


        String ofileName;

        if (OCSSW_L3binController.OFILE_NAMING_SCHEME_IFILE_REPLACE.equalsIgnoreCase(OCSSW_L3binController.getPreferenceOfileNamingScheme())) {
            String orginalKeyString = OCSSW_L3binController.getPreferenceOfileNamingSchemeIfileOriginal();
            String replacementKeyString = OCSSW_L3binController.getPreferenceOfileNamingSchemeIfileReplace();
            ofileName = getOfileWithReplaceForL3MapGen(ifileName, orginalKeyString, replacementKeyString);
        } else if (OCSSW_L3binController.OFILE_NAMING_SCHEME_OCSSW.equalsIgnoreCase(OCSSW_L3binController.getPreferenceOfileNamingScheme())) {
            String ofileNameDefault = ocssw.getOfileName(ifileName, programName);
            ofileName = getOfileForL2BinOcssw(ifileName, ofileNameDefault);
        } else if (OCSSW_L3binController.OFILE_NAMING_SCHEME_OCSSW_SHORT.equalsIgnoreCase(OCSSW_L3binController.getPreferenceOfileNamingScheme())) {
//            String ofileNameDefault = getOcssw().getOfileName(ifileName, programName);
            String ofileNameDefault = ocssw.getOfileName(ifileName, programName);

            ofileName = getOfileForL2BinOcssw(ifileName, ofileNameDefault);
            ofileName = ofileName.replace(".DAY.", ".");
            ofileName = ofileName.replace(".DAY", "");
            ofileName = ofileName.replace(".8D.", ".");
            ofileName = ofileName.replace(".8D", "");
            ofileName = ofileName.replace(".MO.", ".");
            ofileName = ofileName.replace(".MO", "");
            ofileName = ofileName.replace(".YR.", ".");
            ofileName = ofileName.replace(".YR", "");
            ofileName = ofileName.replace(".CU.", ".");
            ofileName = ofileName.replace(".CU", "");

        } else if (OCSSW_L2binController.OFILE_NAMING_SCHEME_SIMPLE.equalsIgnoreCase(OCSSW_L2binController.getPreferenceOfileNamingScheme())) {
            ofileName = getOfileSimple(ifileName);
        } else {
            String ofileNameDefault = ocssw.getOfileName(ifileName, programName);
            ofileName = getOfileForL2BinOcssw(ifileName, ofileNameDefault);
        }


        // if it fails gives it a simple name (for example 'output')
        if (ofileName == null || ofileName.length() == 0) {
            ofileName = getOfileSimple(ifileName);
        }


        String foundExtension = "";
        if (ofileName.endsWith(".sub")) {
            foundExtension = ".sub";
            ofileName = stripFilenameExtractExtension(ofileName, foundExtension);
        }
        if (ofileName.endsWith(".extract")) {
            foundExtension = ".extract";
            ofileName = stripFilenameExtractExtension(ofileName, foundExtension);
        }
        if (ofileName.endsWith(".subset")) {
            foundExtension = ".subset";
            ofileName = stripFilenameExtractExtension(ofileName, foundExtension);
        }


        ofileName += getOfileForL3BinAddOns(resolve, prod, north, south, west, east);


        if (ofileName.contains(" ")) {
            ofileName = ofileName.replace(" ", "");
        }
        ofileName = trimStringChars(ofileName, ".", false, true, true);
        ofileName = trimStringChars(ofileName, "_", false, true, true);
        ofileName = trimStringChars(ofileName, "-", false, true, true);

        ofileName += foundExtension;


        if (ofileName.equalsIgnoreCase(ifileBaseName)) {
            ofileName = ofileName + "_out";
        }

        ofileName += ".nc";

        return ofileName;
    }


    public static String getOfileForL2BinWrapper(String ifileName, OCSSW ocssw, String programName, String resolution, String l3bprod, String suite, String prodtype, String north, String south, String west, String east) {
        String ifileBaseName = stripFilenameExtension(ifileName);


        String ofileName;

        if (OCSSW_L2binController.OFILE_NAMING_SCHEME_IFILE_REPLACE.equalsIgnoreCase(OCSSW_L2binController.getPreferenceOfileNamingScheme())) {
            String orginalKeyString = OCSSW_L2binController.getPreferenceOfileNamingSchemeIfileOriginal();
            String replacementKeyString = OCSSW_L2binController.getPreferenceOfileNamingSchemeIfileReplace();
            ofileName = getOfileWithReplaceForL3MapGen(ifileName, orginalKeyString, replacementKeyString);
        } else if (OCSSW_L2binController.OFILE_NAMING_SCHEME_OCSSW.equalsIgnoreCase(OCSSW_L2binController.getPreferenceOfileNamingScheme())) {
            String ofileNameDefault = ocssw.getOfileName(ifileName, programName);
            ofileName = getOfileForL2BinOcssw(ifileName, ofileNameDefault);
        } else if (OCSSW_L2binController.OFILE_NAMING_SCHEME_OCSSW_SHORT.equalsIgnoreCase(OCSSW_L2binController.getPreferenceOfileNamingScheme())) {
//            String ofileNameDefault = getOcssw().getOfileName(ifileName, programName);
            String ofileNameDefault = ocssw.getOfileName(ifileName, programName);

            ofileName = getOfileForL2BinOcssw(ifileName, ofileNameDefault);
            ofileName = ofileName.replace(".DAY.", ".");
            ofileName = ofileName.replace(".DAY", "");
            ofileName = ofileName.replace(".8D.", ".");
            ofileName = ofileName.replace(".8D", "");
            ofileName = ofileName.replace(".MO.", ".");
            ofileName = ofileName.replace(".MO", "");
            ofileName = ofileName.replace(".YR.", ".");
            ofileName = ofileName.replace(".YR", "");
            ofileName = ofileName.replace(".CU.", ".");
            ofileName = ofileName.replace(".CU", "");

        } else if (OCSSW_L2binController.OFILE_NAMING_SCHEME_SIMPLE.equalsIgnoreCase(OCSSW_L2binController.getPreferenceOfileNamingScheme())) {
            ofileName = getOfileSimple(ifileName);
        } else {
            String ofileNameDefault = ocssw.getOfileName(ifileName, programName);
            ofileName = getOfileForL2BinOcssw(ifileName, ofileNameDefault);
        }


        // if it fails gives it a simple name (for example 'output')
        if (ofileName == null || ofileName.length() == 0) {
            ofileName = getOfileSimple(ifileName);
        }


        String foundExtension = "";
        if (ofileName.endsWith(".sub")) {
            foundExtension = ".sub";
            ofileName = stripFilenameExtractExtension(ofileName, foundExtension);
        }
        if (ofileName.endsWith(".extract")) {
            foundExtension = ".extract";
            ofileName = stripFilenameExtractExtension(ofileName, foundExtension);
        }
        if (ofileName.endsWith(".subset")) {
            foundExtension = ".subset";
            ofileName = stripFilenameExtractExtension(ofileName, foundExtension);
        }


        ofileName += getOfileForL2BinAddOns(resolution, l3bprod, suite, prodtype, north, south, west, east);


        if (ofileName.contains(" ")) {
            ofileName = ofileName.replace(" ", "");
        }
        ofileName = trimStringChars(ofileName, ".", false, true, true);
        ofileName = trimStringChars(ofileName, "_", false, true, true);
        ofileName = trimStringChars(ofileName, "-", false, true, true);


        ofileName += foundExtension;


        if (ofileName.equalsIgnoreCase(ifileBaseName)) {
            ofileName = ofileName + "_out";
        }


        ofileName += ".nc";

        return ofileName;
    }


    public static String getOfileForL3MapGenWrapper(String ifileName, OCSSW ocssw, String programName, String resolution, String oformat, String product, String projection, String interp, String north, String south, String west, String east) {
        String ifileBaseName = stripFilenameExtension(ifileName);


        String ofileName;
        if (OCSSW_L3mapgenController.OFILE_NAMING_SCHEME_IFILE_REPLACE.equalsIgnoreCase(OCSSW_L3mapgenController.getPreferenceOfileNamingScheme())) {
            String orginalKeyString = OCSSW_L3mapgenController.getPreferenceOfileNamingSchemeIfileOriginal();
            String replacementKeyString = OCSSW_L3mapgenController.getPreferenceOfileNamingSchemeIfileReplace();
            ofileName = getOfileWithReplaceForL3MapGen(ifileName, orginalKeyString, replacementKeyString);
        } else if (OCSSW_L3mapgenController.OFILE_NAMING_SCHEME_OCSSW.equalsIgnoreCase(OCSSW_L3mapgenController.getPreferenceOfileNamingScheme())) {
            String ofileNameDefault = ocssw.getOfileName(ifileName, programName);
            ofileName = getOfileForL3MapGenOcssw(ifileName, ofileNameDefault, resolution, oformat, product, projection);
        } else if (OCSSW_L3mapgenController.OFILE_NAMING_SCHEME_OCSSW_SHORT.equalsIgnoreCase(OCSSW_L3mapgenController.getPreferenceOfileNamingScheme())) {
//            String ofileNameDefault = getOcssw().getOfileName(ifileName, programName);
            String ofileNameDefault = ocssw.getOfileName(ifileName, programName);

            ofileName = getOfileForL3MapGenOcssw(ifileName, ofileNameDefault, resolution, oformat, product, projection);
            ofileName = ofileName.replace(".DAY.", ".");
            ofileName = ofileName.replace(".DAY", "");
            ofileName = ofileName.replace(".8D.", ".");
            ofileName = ofileName.replace(".8D", "");
            ofileName = ofileName.replace(".MO.", ".");
            ofileName = ofileName.replace(".MO", "");
            ofileName = ofileName.replace(".YR.", ".");
            ofileName = ofileName.replace(".YR", "");
            ofileName = ofileName.replace(".CU.", ".");
            ofileName = ofileName.replace(".CU", "");

        } else if (OCSSW_L3mapgenController.OFILE_NAMING_SCHEME_SIMPLE.equalsIgnoreCase(OCSSW_L3mapgenController.getPreferenceOfileNamingScheme())) {
            ofileName = getOfileSimple(ifileName);
        } else {
            String orginalKeyString = OCSSW_L3mapgenController.getPreferenceOfileNamingSchemeIfileOriginal();
            String replacementKeyString = OCSSW_L3mapgenController.getPreferenceOfileNamingSchemeIfileReplace();
            ofileName = getOfileWithReplaceForL3MapGen(ifileName, orginalKeyString, replacementKeyString);
        }

        // if it fails gives it a simple name (for example 'output')
        if (ofileName == null || ofileName.length() == 0) {
            ofileName = getOfileSimple(ifileName);
        }


        String foundExtension = "";
        if (ofileName.endsWith(".sub")) {
            foundExtension = ".sub";
            ofileName = stripFilenameExtractExtension(ofileName, foundExtension);
        }
        if (ofileName.endsWith(".extract")) {
            foundExtension = ".extract";
            ofileName = stripFilenameExtractExtension(ofileName, foundExtension);
        }
        if (ofileName.endsWith(".subset")) {
            foundExtension = ".subset";
            ofileName = stripFilenameExtractExtension(ofileName, foundExtension);
        }


        ofileName += getOfileForL3MapGenAddOns(resolution, product, projection, interp, north, south, west, east);


        if (OCSSW_L3mapgenController.OFILE_NAMING_SCHEME_IFILE_PLUS_SUFFIX.equalsIgnoreCase(OCSSW_L3mapgenController.getPreferenceOfileNamingScheme())) {
            String suffix = "_suffix";
            ofileName += suffix;
        }


        if (ofileName.contains(" ")) {
            ofileName = ofileName.replace(" ", "");
        }
        ofileName = trimStringChars(ofileName, ".", false, true, true);
        ofileName = trimStringChars(ofileName, "_", false, true, true);
        ofileName = trimStringChars(ofileName, "-", false, true, true);


        ofileName += foundExtension;


        if (ofileName.equalsIgnoreCase(ifileBaseName)) {
            ofileName = ofileName + "_out";
        }

        ofileName = getOfileForL3MapGenAddExtension(ofileName, oformat);

        return ofileName;
    }


    private static String getOfileSimple(String ifilename) {

        String ofilename = "output";

        if (ifilename != null && ifilename.trim().length() > 0) {
            File file = new File(ifilename);
            if (file != null) {
                File parentFile = file.getParentFile();
                if (parentFile != null) {
                    String parentPath = parentFile.getAbsolutePath();
                    File file2 = new File(parentPath, ofilename);
                    if (file2 != null) {
                        ofilename = file2.getAbsolutePath();
                    }
                }
            }
        }

        return ofilename;
    }


    private static String getOfileWithReplaceForL3MapGen(String ifilename, String orginalKeyString, String replacementKeyString) {

        String parentPath = null;
        String ifileBasename = ifilename;
        boolean pathRemoved = false;
        String ofileBaseName;

        File ifileFile = new File(ifilename);

        if (ifileFile != null) {
            if (ifileFile.getParentFile() != null) {
                parentPath = ifileFile.getParentFile().getAbsolutePath();
            }
            ifileBasename = ifileFile.getName();
            pathRemoved = true;
        }

        ifileBasename = stripFilenameExtension(ifileBasename);
//
//        String orginalKeyString = OCSSW_L3mapgenController.getPreferenceOfileNamingSchemeIfileOriginal();
//        String replacementKeyString = OCSSW_L3mapgenController.getPreferenceOfileNamingSchemeIfileReplace();
        if (replacementKeyString == null) {
            replacementKeyString = "";
        }

        if (orginalKeyString != null && orginalKeyString.length() > 0) {
            ofileBaseName = ifileBasename.replace(orginalKeyString, replacementKeyString);
        } else {
            ofileBaseName = ifileBasename;
        }

        String ofilename = null;

        if (pathRemoved && parentPath != null) {
            File oFile = new File(parentPath, ofileBaseName);
            if (oFile != null) {
                ofilename = oFile.getAbsolutePath();
            }
        }

        if (ofilename == null) {
            ofilename = ofileBaseName;
        }

        return ofilename;
    }


    private static class L3MAPGEN_Processor extends ProcessorModel {

        L3MAPGEN_Processor(final String programName, String xmlFileName, OCSSW ocssw) {
            super(programName, xmlFileName, ocssw);
            setOpenInSeadas(true);

            OCSSWInfo ocsswInfo = OCSSWInfo.getInstance();
            String ocsswDataDirPath = ocsswInfo.getOcsswDataDirPath();
            if (ocsswDataDirPath != null) {
                ParamInfo palfileParamInfo = getParamInfo("palfile");

                if (palfileParamInfo != null) {
                    String palfileDirName = ocsswDataDirPath + System.getProperty("file.separator") + "common" + System.getProperty("file.separator") + "palette";
                    File palfileFile = new File(palfileDirName);
                    File[] palfileFiles = palfileFile.listFiles();
                    palfileParamInfo.clearValidValueInfos();

                    String basenames[] = new String[palfileFiles.length];
                    int data[] = new int[10];
                    for (int i = 0; i < palfileFiles.length; i++) {
                        basenames[i] = palfileFiles[i].getName();
                    }

                    Arrays.sort(basenames);

                    for (String basename : basenames) {
                        basename = stripPalFilenameExtension(basename);
                        ParamValidValueInfo validValueInfo = new ParamValidValueInfo(basename);
                        validValueInfo.setDescription(basename);
                        palfileParamInfo.getValidValueInfos().add(validValueInfo);
                    }
                }
            }


            addPropertyChangeListener("resolution", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {

                    String ifileName = getParamValue(getPrimaryInputFileOptionName());
//                    String resolution = (String) propertyChangeEvent.getNewValue();
                    String resolution = getParamValue("resolution");
                    String oformat = getParamValue("oformat");
                    String product = getParamValue("product");
                    String projection = getParamValue("projection");
                    String interp = getParamValue("interp");
                    String north = getParamValue("north");
                    String south = getParamValue("south");
                    String west = getParamValue("west");
                    String east = getParamValue("east");

                    String ofileName = getOfileForL3MapGenWrapper(ifileName, getOcssw(), programName, resolution, oformat, product, projection, interp, north, south, west, east);

                    updateOFileInfo(ofileName);
                }
            });


            addPropertyChangeListener("oformat", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                    String ifileName = getParamValue(getPrimaryInputFileOptionName());

//                    String resolution = getParamValue("resolution");
//                    String oformat = (String) propertyChangeEvent.getNewValue();
//                    String product = getParamValue("product");
//                    String projection = getParamValue("projection");
//                    String interp = getParamValue("interp");

                    String resolution = getParamValue("resolution");
                    String oformat = getParamValue("oformat");
                    String product = getParamValue("product");
                    String projection = getParamValue("projection");
                    String interp = getParamValue("interp");
                    String north = getParamValue("north");
                    String south = getParamValue("south");
                    String west = getParamValue("west");
                    String east = getParamValue("east");

                    String ofileName = getOfileForL3MapGenWrapper(ifileName, getOcssw(), programName, resolution, oformat, product, projection, interp, north, south, west, east);

                    updateOFileInfo(ofileName);
                }
            });


            addPropertyChangeListener("product", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                    String ifileName = getParamValue(getPrimaryInputFileOptionName());

//                    String resolution = getParamValue("resolution");
//                    String oformat = getParamValue("oformat");
//                    String product = (String) propertyChangeEvent.getNewValue();
//                    String projection = getParamValue("projection");
//                    String interp = getParamValue("interp");

                    String resolution = getParamValue("resolution");
                    String oformat = getParamValue("oformat");
                    String product = getParamValue("product");
                    String projection = getParamValue("projection");
                    String interp = getParamValue("interp");
                    String north = getParamValue("north");
                    String south = getParamValue("south");
                    String west = getParamValue("west");
                    String east = getParamValue("east");

                    String ofileName = getOfileForL3MapGenWrapper(ifileName, getOcssw(), programName, resolution, oformat, product, projection, interp, north, south, west, east);


                    updateOFileInfo(ofileName);
                }
            });


            addPropertyChangeListener("projection", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {

//                    String projection = (String) propertyChangeEvent.getNewValue();
//
//
//                    if (projection != null && projection.trim().startsWith("#")) {
//                        paramList.getPropertyChangeSupport().firePropertyChange("projection", "-1", "");
//                        updateParamInfo("projection", "");
//                        fireEvent("projection", "-1", "");
//                        return;
//                    }

                    String ifileName = getParamValue(getPrimaryInputFileOptionName());

//                    String resolution = getParamValue("resolution");
//                    String oformat = getParamValue("oformat");
//                    String product = getParamValue("product");
//                    String interp = getParamValue("interp");

                    String resolution = getParamValue("resolution");
                    String oformat = getParamValue("oformat");
                    String product = getParamValue("product");
                    String projection = getParamValue("projection");
                    String interp = getParamValue("interp");

                    if (projection != null && projection.trim().startsWith("#")) {
                        paramList.getPropertyChangeSupport().firePropertyChange("projection", "-1", "");
                        updateParamInfo("projection", "");
                        fireEvent("projection", "-1", "");
                        return;
                    }

                    String north = getParamValue("north");
                    String south = getParamValue("south");
                    String west = getParamValue("west");
                    String east = getParamValue("east");

                    String ofileName = getOfileForL3MapGenWrapper(ifileName, getOcssw(), programName, resolution, oformat, product, projection, interp, north, south, west, east);


                    updateOFileInfo(ofileName);
                }
            });


            addPropertyChangeListener("interp", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                    String ifileName = getParamValue(getPrimaryInputFileOptionName());

                    String resolution = getParamValue("resolution");
                    String oformat = getParamValue("oformat");
                    String product = getParamValue("product");
                    String projection = getParamValue("projection");
                    String interp = (String) propertyChangeEvent.getNewValue();
                    String north = getParamValue("north");
                    String south = getParamValue("south");
                    String west = getParamValue("west");
                    String east = getParamValue("east");

                    String ofileName = getOfileForL3MapGenWrapper(ifileName, getOcssw(), programName, resolution, oformat, product, projection, interp, north, south, west, east);

                    updateOFileInfo(ofileName);
                }
            });


            addPropertyChangeListener("north", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                    String ifileName = getParamValue(getPrimaryInputFileOptionName());

                    String resolution = getParamValue("resolution");
                    String oformat = getParamValue("oformat");
                    String product = getParamValue("product");
                    String projection = getParamValue("projection");
                    String interp = (String) propertyChangeEvent.getNewValue();
                    String north = getParamValue("north");
                    String south = getParamValue("south");
                    String west = getParamValue("west");
                    String east = getParamValue("east");

                    String ofileName = getOfileForL3MapGenWrapper(ifileName, getOcssw(), programName, resolution, oformat, product, projection, interp, north, south, west, east);

                    updateOFileInfo(ofileName);
                }
            });

            addPropertyChangeListener("south", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                    String ifileName = getParamValue(getPrimaryInputFileOptionName());

                    String resolution = getParamValue("resolution");
                    String oformat = getParamValue("oformat");
                    String product = getParamValue("product");
                    String projection = getParamValue("projection");
                    String interp = (String) propertyChangeEvent.getNewValue();
                    String north = getParamValue("north");
                    String south = getParamValue("south");
                    String west = getParamValue("west");
                    String east = getParamValue("east");

                    String ofileName = getOfileForL3MapGenWrapper(ifileName, getOcssw(), programName, resolution, oformat, product, projection, interp, north, south, west, east);

                    updateOFileInfo(ofileName);
                }
            });

            addPropertyChangeListener("west", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                    String ifileName = getParamValue(getPrimaryInputFileOptionName());

                    String resolution = getParamValue("resolution");
                    String oformat = getParamValue("oformat");
                    String product = getParamValue("product");
                    String projection = getParamValue("projection");
                    String interp = (String) propertyChangeEvent.getNewValue();
                    String north = getParamValue("north");
                    String south = getParamValue("south");
                    String west = getParamValue("west");
                    String east = getParamValue("east");

                    String ofileName = getOfileForL3MapGenWrapper(ifileName, getOcssw(), programName, resolution, oformat, product, projection, interp, north, south, west, east);

                    updateOFileInfo(ofileName);
                }
            });

            addPropertyChangeListener("east", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                    String ifileName = getParamValue(getPrimaryInputFileOptionName());

                    String resolution = getParamValue("resolution");
                    String oformat = getParamValue("oformat");
                    String product = getParamValue("product");
                    String projection = getParamValue("projection");
                    String interp = (String) propertyChangeEvent.getNewValue();
                    String north = getParamValue("north");
                    String south = getParamValue("south");
                    String west = getParamValue("west");
                    String east = getParamValue("east");

                    String ofileName = getOfileForL3MapGenWrapper(ifileName, getOcssw(), programName, resolution, oformat, product, projection, interp, north, south, west, east);

                    updateOFileInfo(ofileName);
                }
            });


            // todo bypassing all this additional ofile renaming at least for now as it does not always return the best name
            if (1 == 2) {
                addPropertyChangeListener("product", new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                        String ifileName = getParamValue(getPrimaryInputFileOptionName());
                        if (ifileName != null) {
                            String oldProdValue = (String) propertyChangeEvent.getOldValue();
                            String newProdValue = (String) propertyChangeEvent.getNewValue();
                            String[] additionalOptions = {"--suite=" + newProdValue, "--resolution=" + getParamValue("resolution"), "--oformat=" + getParamValue("oformat")};
                            String ofileName = ocssw.getOfileName(ifileName, additionalOptions);
                            //String ofileName = SeadasFileUtils.findNextLevelFileName(getParamValue(getPrimaryInputFileOptionName()), programName, additionalOptions);
                            updateOFileInfo(ofileName);
                        }
                    }
                });

                addPropertyChangeListener("resolution", new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                        String ifileName = getParamValue(getPrimaryInputFileOptionName());
                        String oldResolutionValue = (String) propertyChangeEvent.getOldValue();
                        String newResolutionValue = (String) propertyChangeEvent.getNewValue();
                        String suite = getParamValue("product");
                        if (suite == null || suite.trim().length() == 0) {
                            suite = "all";
                        }
                        String[] additionalOptions = {"--resolution=" + newResolutionValue, "--suite=" + suite, "--oformat=" + getParamValue("oformat")};
                        //String ofileName = SeadasFileUtils.findNextLevelFileName(getParamValue(getPrimaryInputFileOptionName()), programName, additionalOptions);
                        String ofileName = ocssw.getOfileName(ifileName, additionalOptions);
                        updateOFileInfo(ofileName);
                    }
                });

                addPropertyChangeListener("oformat", new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                        String ifileName = getParamValue(getPrimaryInputFileOptionName());
                        String oldFormatValue = (String) propertyChangeEvent.getOldValue();
                        String newFormatValue = (String) propertyChangeEvent.getNewValue();
                        String suite = getParamValue("product");
                        if (suite == null || suite.trim().length() == 0) {
                            suite = "all";
                        }
                        String[] additionalOptions = {"--resolution=" + getParamValue("resolution"), "--suite=" + suite, "--oformat=" + newFormatValue};
                        //String ofileName = SeadasFileUtils.findNextLevelFileName(getParamValue(getPrimaryInputFileOptionName()), programName, additionalOptions);
                        String ofileName = ocssw.getOfileName(ifileName, additionalOptions);
                        updateOFileInfo(ofileName);
                    }
                });
            }


        }

    }

    private static class MAPGEN_Processor extends ProcessorModel {

        MAPGEN_Processor(final String programName, String xmlFileName, OCSSW ocssw) {
            super(programName, xmlFileName, ocssw);
            setOpenInSeadas(false);

            addPropertyChangeListener("product", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                    String ifileName = getParamValue(getPrimaryInputFileOptionName());
                    if (ifileName != null) {
                        String oldProdValue = (String) propertyChangeEvent.getOldValue();
                        String newProdValue = (String) propertyChangeEvent.getNewValue();
                        String[] additionalOptions = {"--suite=" + newProdValue, "--resolution=" + getParamValue("resolution"), "--oformat=" + getParamValue("oformat")};
                        String ofileName = ocssw.getOfileName(ifileName, additionalOptions);
                        //String ofileName = SeadasFileUtils.findNextLevelFileName(getParamValue(getPrimaryInputFileOptionName()), programName, additionalOptions);
                        updateOFileInfo(ofileName);
                    }
                }
            });

            addPropertyChangeListener("resolution", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                    String ifileName = getParamValue(getPrimaryInputFileOptionName());
                    String oldResolutionValue = (String) propertyChangeEvent.getOldValue();
                    String newResolutionValue = (String) propertyChangeEvent.getNewValue();
                    String suite = getParamValue("product");
                    if (suite == null || suite.trim().length() == 0) {
                        suite = "all";
                    }
                    String[] additionalOptions = {"--resolution=" + newResolutionValue, "--suite=" + suite, "--oformat=" + getParamValue("oformat")};
                    //String ofileName = SeadasFileUtils.findNextLevelFileName(getParamValue(getPrimaryInputFileOptionName()), programName, additionalOptions);
                    String ofileName = ocssw.getOfileName(ifileName, additionalOptions);
                    updateOFileInfo(ofileName);
                }
            });

            addPropertyChangeListener("oformat", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                    String ifileName = getParamValue(getPrimaryInputFileOptionName());
                    String oldFormatValue = (String) propertyChangeEvent.getOldValue();
                    String newFormatValue = (String) propertyChangeEvent.getNewValue();
                    String suite = getParamValue("product");
                    if (suite == null || suite.trim().length() == 0) {
                        suite = "all";
                    }
                    String[] additionalOptions = {"--resolution=" + getParamValue("resolution"), "--suite=" + suite, "--oformat=" + newFormatValue};
                    //String ofileName = SeadasFileUtils.findNextLevelFileName(getParamValue(getPrimaryInputFileOptionName()), programName, additionalOptions);
                    String ofileName = ocssw.getOfileName(ifileName, additionalOptions);
                    updateOFileInfo(ofileName);
                }
            });

        }

    }

    private static class OCSSWInstaller_Processor extends ProcessorModel {
        ArrayList<String> validOcsswTags = new ArrayList<>();

        OCSSWInstaller_Processor(String programName, String xmlFileName, OCSSW ocssw) {
            super(programName, xmlFileName, ocssw);
            updateTags();
            getValidOCSSWTags4SeaDASVersion();
        }

        @Override
        void setCommandArrayPrefix() {
            cmdArrayPrefix = new String[2];
            cmdArrayPrefix[0] = ocssw.TMP_OCSSW_BOOTSTRAP;
            getCmdArrayPrefix()[1] = ocssw.TMP_OCSSW_INSTALLER;
        }

        private void updateTags() {
            validOcsswTags = ocssw.getOcsswTags();
            ListIterator<String> listIterator = validOcsswTags.listIterator();
            ParamValidValueInfo paramValidValueInfo;
            ArrayList<ParamValidValueInfo> tagValidValues = new ArrayList<>();

            String vTagPatternString = "^V\\d\\d\\d\\d.\\d+$";
            Pattern vTagPattern = Pattern.compile(vTagPatternString);
            String rTagPatternString = "^R\\d\\d\\d\\d.\\d+$";
            Pattern rTagPattern = Pattern.compile(rTagPatternString);
            String tTagPatternString = "^T\\d\\d\\d\\d.\\d+$";
            Pattern tTagPattern = Pattern.compile(tTagPatternString);

            while (listIterator.hasNext()) {
                paramValidValueInfo = new ParamValidValueInfo(listIterator.next());

                boolean addTag = false;

                // Special case where unexpected low number of tags so likely hard-coded defaults have been used so show all regardless if T-tag or R-Tag
                if (tagValidValues.size() <= 3) {
                    addTag = true;
                }

                if (paramValidValueInfo != null && paramValidValueInfo.getValue() != null) {
                    String currTag = paramValidValueInfo.getValue().trim();

                    if (!includeOfficialReleaseTagsOnly()) {
                        addTag = true;
                    }

                    if (!addTag) {
                        Matcher vMatcher = vTagPattern.matcher(currTag);
                        if (vMatcher != null && vMatcher.find()) {
                            addTag = true;
                        }
                    }

                    if (!addTag) {
                        Matcher rMatcher = rTagPattern.matcher(currTag);
                        if (rMatcher != null && rMatcher.find()) {
                            addTag = true;
                        }
                    }

                    if (addTag) {
                        tagValidValues.add(paramValidValueInfo);
                    }
                }
            }

            paramList.getInfo(VALID_TAGS_OPTION_NAME).setValidValueInfos(tagValidValues);
            //System.out.println(paramList.getInfo(TAG_OPTION_NAME).getName());
        }


        private boolean includeOfficialReleaseTagsOnly() {
            return OCSSW_InstallerController.getPreferenceUseReleaseTagsOnly();
        }


        /**
         * This method scans for valid OCSSW tags at https://oceandata.sci.gsfc.nasa.gov/manifest/tags, returns a list of tags that start with capital letter "V"
         *
         * @return List of valid OCSSW tags for SeaDAS
         */
        public ArrayList<String> getValidOcsswTagsFromURL() {
            ArrayList<String> validOcsswTags = new ArrayList<>();
            try {

                URL tagsURL = new URL("https://oceandata.sci.gsfc.nasa.gov/manifest/tags/");
                URLConnection tagsConnection = tagsURL.openConnection();
                BufferedReader in = new BufferedReader(new InputStreamReader(tagsConnection.getInputStream()));

                String inputLine, tagName;
                String tokenString = "href=";
                int sp, ep;
                while ((inputLine = in.readLine()) != null) {
                    if (inputLine.indexOf(tokenString) != -1) {
                        sp = inputLine.indexOf(">");
                        ep = inputLine.lastIndexOf("<");
                        tagName = inputLine.substring(sp + 1, ep - 1);
                        //System.out.println("tag: " + tagName);
                        if (tagName.startsWith("V") ||
                                tagName.startsWith("R") ||
                                tagName.startsWith("T")) {
                            validOcsswTags.add(tagName);
                        }
                    }
                }

                in.close();

            } catch (IOException ioe) {
                ioe.printStackTrace();
            }

            return validOcsswTags;
        }

        public void getValidOCSSWTags4SeaDASVersion() {
            //JSON parser object to parse read file
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
                validSeaDASTags.forEach(tagObject -> parseValidSeaDASTagObject((JSONObject) tagObject));
                in.close();

            } catch (IOException ioe) {
                ioe.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        private static void parseValidSeaDASTagObject(JSONObject tagObject) {
            Version currentVersion = VersionChecker.getInstance().getLocalVersion();
            String seadasVersionString = currentVersion.toString();
            //Get seadas version
            String seadasVersion = (String) tagObject.get("seadas");
            //Get corresponding ocssw tags for seadas
            JSONArray ocsswTags = (JSONArray) tagObject.get("ocssw");
            //System.out.println(ocsswTags);

        }

        /**
         * /tmp/install_ocssw --tag $TAGNAME -i ocssw-new --seadas --modist $MISSIONNAME
         *
         * @return
         */
        @Override
        public String[] getCmdArraySuffix() {
            String[] cmdArraySuffix = new String[2];
            cmdArraySuffix[0] = "--tag=" + paramList.getInfo(VALID_TAGS_OPTION_NAME).getValue();
            cmdArraySuffix[1] = "--seadas";
            return cmdArraySuffix;
        }

        @Override
        public ParamList getParamList() {
            ParamInfo paramInfo;
            paramInfo = new ParamInfo(("--tag"), paramList.getInfo(VALID_TAGS_OPTION_NAME).getValue(), ParamInfo.Type.STRING);
            paramInfo.setUsedAs(ParamInfo.USED_IN_COMMAND_AS_OPTION);
            paramList.addInfo(paramInfo);
            paramInfo = new ParamInfo(("--seadas"), "true", ParamInfo.Type.FLAGS);
            paramInfo.setUsedAs(ParamInfo.USED_IN_COMMAND_AS_FLAG);
            paramList.addInfo(paramInfo);
            return paramList;
        }
    }

}
