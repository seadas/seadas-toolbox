package gov.nasa.gsfc.seadas.processing.core;

import com.bc.ceres.core.runtime.Version;
import gov.nasa.gsfc.seadas.processing.common.*;
import gov.nasa.gsfc.seadas.processing.ocssw.OCSSW;
import gov.nasa.gsfc.seadas.processing.ocssw.OCSSWClient;
import gov.nasa.gsfc.seadas.processing.ocssw.OCSSWInfo;
import gov.nasa.gsfc.seadas.processing.ocssw.OCSSWLocal;
import gov.nasa.gsfc.seadas.processing.preferences.OCSSW_InstallerController;
import gov.nasa.gsfc.seadas.processing.preferences.OCSSW_L2binController;
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
        setNumColumns(0);
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

    public void updateParamInfo(ParamInfo currentOption, String newValue) {
        updateParamInfo(currentOption.getName(), newValue);
        if ("l2bin".equalsIgnoreCase(programName)) {
            if (currentOption.getName().equals("suite") || currentOption.getName().equals("ifile")) {
                updateFlagUse(null);
            }
        }
        checkCompleteness();
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

        if (selectedFile.getName().endsWith(".txt")) {
            try {
                LineNumberReader lineNumberReader = new LineNumberReader(new FileReader(selectedFile));
                String sampleFileName = lineNumberReader.readLine();
                if (new File(sampleFileName).exists()) {
                    selectedFile = new File(sampleFileName);
                    //System.out.println("sample file name: " + sampleFileName + System.currentTimeMillis());
                } else {
                    return;
                }
            } catch (FileNotFoundException fnfe) {

            } catch (IOException ioe) {

            }
        }

        NetcdfFile ncFile = null;
        try {
            ncFile = NetcdfFile.open(selectedFile.getAbsolutePath());
        } catch (IOException ioe) {

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
                updateFlagUse(null);
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
        this.numColumns = numColumns;
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


    private void updateFlagUse(String parFileName) {

        if (!"l2bin".equalsIgnoreCase(programName)) {
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

                final String L2BIN_PROGRAM_NAME = "l2bin";
                File dataDir = SystemUtils.getApplicationDataDir();
                File l2binAuxDir = new File(dataDir, L2BIN_PROGRAM_NAME);
                l2binAuxDir.mkdirs();

                File auxParFile = new File(l2binAuxDir, L2BIN_PROGRAM_NAME + "_params.par");
                try {
                    createL2binAuxParFile(L2BIN_PROGRAM_NAME, ifile, suite, auxParFile);
                    if (auxParFile.exists()) {
                        updateParamInfosFromL2binAuxParFile(auxParFile.getAbsolutePath());
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


    public void updateParamInfosFromL2binAuxParFile(String parfile) throws IOException {

        String flagUsePref = OCSSW_L2binController.getPreferenceFlaguse();
        if (flagUsePref != null && flagUsePref.trim().length() > 0) {
            ParamInfo flaguseParamInfo = paramList.getInfo("flaguse");
            String originalFlaguse = flaguseParamInfo.getValue();
            updateParamInfo("flaguse", flagUsePref);
            fireEvent("flaguse", originalFlaguse, flagUsePref);
            return;
        }

        if (!OCSSW_L2binController.getPreferenceFlaguseAutoFillEnable()) {
            return;
        }


        BufferedReader br = new BufferedReader(new FileReader(parfile));
        try {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();


            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
//                System.out.println(line);

                if (line != null) {
                    String[] values = line.split("=");
                    if (values != null && values.length == 2) {
                        String name = values[0].trim();
                        String value = values[1].trim();
//                        System.out.println("name=" + name + "  value=" + value);

                        if ("suite".equals(name)) {
                            ParamInfo paramInfo = paramList.getInfo("suite");
                            String originalParamInfo = paramInfo.getValue();
//                                flaguseParamInfo.setValue(value);
                            updateParamInfo("suite", value);
                            fireEvent("suite", originalParamInfo, value);
                        }

                        if ("flaguse".equals(name)) {
                            ParamInfo flaguseParamInfo = paramList.getInfo("flaguse");
                            String originalFlaguse = flaguseParamInfo.getValue();
//                                flaguseParamInfo.setValue(value);
                            updateParamInfo("flaguse", value);
                            fireEvent("flaguse", originalFlaguse, value);
                        }
                    }
                }

            }
//                String everything = sb.toString();
        } finally {
            br.close();
        }
    }


    private void updateSuite(File selectedFile) {

        FileInfo ifileInfo = new FileInfo(selectedFile, ocssw);

        File missionDir = ifileInfo.getMissionDirectory();
        if (missionDir == null) {
            try {
                LineNumberReader reader = new LineNumberReader(new FileReader(selectedFile));
                String sampleFileName = reader.readLine();
                missionDir = new FileInfo(sampleFileName).getMissionDirectory();
            } catch (FileNotFoundException fnfe) {

            } catch (IOException ioe) {

            }

        }


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
                    String oldProdValue = (String) propertyChangeEvent.getOldValue();
                    String newProdValue = (String) propertyChangeEvent.getNewValue();
                    String ofileName = getParamValue(getPrimaryOutputFileOptionName());
                    if (oldProdValue.trim().length() > 0 && ofileName.indexOf(oldProdValue) != -1) {
                        ofileName = ofileName.replaceAll(oldProdValue, newProdValue);
                    } else {
                        ofileName = ofileName + "_" + newProdValue;
                    }
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

        // make sure key is uppercase
        keyString = convertAnyUpperCaseKeyToLowerCase(keyString, "resolution");
        keyString = convertAnyUpperCaseKeyToLowerCase(keyString, "product");
        keyString = convertAnyUpperCaseKeyToLowerCase(keyString, "product_single");
        keyString = convertAnyUpperCaseKeyToLowerCase(keyString, "projection");
        keyString = convertAnyUpperCaseKeyToLowerCase(keyString, "interp");
        keyString = convertAnyUpperCaseKeyToLowerCase(keyString, "north");
        keyString = convertAnyUpperCaseKeyToLowerCase(keyString, "south");
        keyString = convertAnyUpperCaseKeyToLowerCase(keyString, "west");
        keyString = convertAnyUpperCaseKeyToLowerCase(keyString, "east");
        keyString = convertAnyUpperCaseKeyToLowerCase(keyString, "nswe");
        keyString = convertAnyUpperCaseKeyToLowerCase(keyString, "n.s.w.e");
        keyString = convertAnyUpperCaseKeyToLowerCase(keyString, "n_s_w_e");
        keyString = convertAnyUpperCaseKeyToLowerCase(keyString, "northsouthwesteast");
        keyString = convertAnyUpperCaseKeyToLowerCase(keyString, "north.south.west.east");
        keyString = convertAnyUpperCaseKeyToLowerCase(keyString, "north_south_west_east");



        if (checkForVariantMatch(keyString, "product_single")) {
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
            keyString = replaceAnyKeyStringVariant(keyString, "product_single", productSingle);
        }



        if (checkForVariantMatch(keyString, "product")) {
            String productList = "";
            if (product != null && product.trim().length() > 0) {
                String[] productsArray = product.split("[,\\s]");

                for (String currProduct : productsArray) {
                    if (keyString.contains("[_product]")) {
                        productList += "_" + currProduct;
                    } else if (keyString.contains("[-product]")) {
                        productList += "-" + currProduct;
                    } else {
                        productList += "." + currProduct;
                    }
                }
            }

            keyString = replaceAnyKeyStringVariant(keyString, "product", productList);
        }


        if (checkForVariantMatch(keyString, "resolution")) {
            if (resolution == null) {
                resolution = "";
            }
            keyString = replaceAnyKeyStringVariant(keyString, "resolution", resolution);
        }


        if (checkForVariantMatch(keyString, "projection")) {
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

            keyString = replaceAnyKeyStringVariant(keyString, "projection", projectionName);
        }


        if (checkForVariantMatch(keyString, "interp")) {
            if (interp == null) {
                interp = "";
            }
            keyString = replaceAnyKeyStringVariant(keyString, "interp", interp);
        }


        if (checkForVariantMatch(keyString, "north")) {
            if (north == null) {
                north = "";
            }
            keyString = replaceAnyKeyStringVariant(keyString, "north", north, "N", null);
        }

        if (checkForVariantMatch(keyString, "south")) {
            if (south == null) {
                south = "";
            }
            keyString = replaceAnyKeyStringVariant(keyString, "south", south, "S", null);
        }

        if (checkForVariantMatch(keyString, "west")) {
            if (west == null) {
                west = "";
            }
            keyString = replaceAnyKeyStringVariant(keyString, "west", west, "W", null);
        }

        if (checkForVariantMatch(keyString, "east")) {
            if (east == null) {
                east = "";
            }
            keyString = replaceAnyKeyStringVariant(keyString, "east", east, "E", null);
        }

        if (checkForVariantMatch(keyString, "nswe")
                || checkForVariantMatch(keyString, "n.s.w.e")
                || checkForVariantMatch(keyString, "n_s_w_e")
                || checkForVariantMatch(keyString, "northsouthwesteast")
                || checkForVariantMatch(keyString, "north.south.west.east")
                || checkForVariantMatch(keyString, "north_south_west_east")
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
            String northSouthWestEast = "";
            if (north.length() > 0 && south.length() > 0 && west.length() > 0 && east.length() > 0) {
                nswe = north + "N_" + south + "S_" + west + "W_" + east + "E";
                northSouthWestEast = north + "North_" + south + "South_" + west + "West_" + east + "East";
            }

            keyString = replaceAnyKeyStringVariant(keyString, "nswe", nswe);
            keyString = replaceAnyKeyStringVariant(keyString, "n.s.w.e", nswe);
            keyString = replaceAnyKeyStringVariant(keyString, "n_s_w_e", nswe);
            keyString = replaceAnyKeyStringVariant(keyString, "northsouthwesteast", northSouthWestEast);
            keyString = replaceAnyKeyStringVariant(keyString, "north.south.west.east", northSouthWestEast);
            keyString = replaceAnyKeyStringVariant(keyString, "north_south_west_east", northSouthWestEast);
        }


//
//        simpleFormat = trimStringChars(simpleFormat, ".", false, true, true);
//        simpleFormat = trimStringChars(simpleFormat, "_", false, true, true);
//        simpleFormat = trimStringChars(simpleFormat, "-", false, true, true);

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


    private static String replaceAnyKeyStringVariant(String keyString, String key, String value) {
        return replaceAnyKeyStringVariant( keyString,  key, value, null, null);
    }

    private static String replaceAnyKeyStringVariant(String keyString, String key, String value, String prefix, String suffix) {

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

        keyString = keyString.replace("[" + key + "]", "." + value);
        keyString = keyString.replace("[." + key + "]", "." + value);
        keyString = keyString.replace("[_" + key + "]", "_" + value);
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
        String fileBasename = filename;

        if (filename.endsWith(".nc")) {
            fileBasename = filename.substring(0, filename.length() - 3);
        }

        return fileBasename;
    }

    private static String stripPalFilenameExtension(String filename) {
        String fileBasename = filename;

        if (filename.endsWith(".pal")) {
            fileBasename = filename.substring(0, filename.length() - 4);
        }

        return fileBasename;
    }



    public static String getOfileForL3MapGenWrapper(String ifileName, OCSSW ocssw, String programName, String resolution, String oformat, String product, String projection, String interp, String north, String south, String west, String east) {
        String ifileBaseName = stripFilenameExtension(ifileName);

        String ofileName;
        if (OCSSW_L3mapgenController.OFILE_NAMING_SCHEME_IFILE_REPLACE.equalsIgnoreCase(OCSSW_L3mapgenController.getPreferenceOfileNamingScheme())) {
            ofileName = getOfileWithReplaceForL3MapGen(ifileName);
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
            ofileName = getOfileForL3MapGenSimple(ifileName);
        } else {
            ofileName = getOfileWithReplaceForL3MapGen(ifileName);
        }

        // if it fails gives it a simple name (for example 'output')
        if (ofileName == null || ofileName.length() == 0) {
            ofileName = getOfileForL3MapGenSimple(ifileName);
        }


        ofileName += getOfileForL3MapGenAddOns(resolution, product, projection, interp,  north, south, west, east);


        if (OCSSW_L3mapgenController.OFILE_NAMING_SCHEME_IFILE_PLUS_SUFFIX.equalsIgnoreCase(OCSSW_L3mapgenController.getPreferenceOfileNamingScheme())) {
            String suffix = "_suffix";
            ofileName += suffix;
        }

        if (ofileName.equalsIgnoreCase(ifileBaseName)) {
            ofileName = ofileName + "_out";
        }


        if (ofileName.contains(" ")) {
            ofileName = ofileName.replace(" ", "");
        }
        ofileName = trimStringChars(ofileName, ".", false, true, true);
        ofileName = trimStringChars(ofileName, "_", false, true, true);
        ofileName = trimStringChars(ofileName, "-", false, true, true);


        ofileName = getOfileForL3MapGenAddExtension(ofileName, oformat);

        return ofileName;
    }


    private static String getOfileForL3MapGenSimple(String ifilename) {

        String ifileBasename = stripFilenameExtension(ifilename);

        String ofilename = "output";

        File file = new File(ifilename);
        if (file != null) {
            String parentPath = file.getParentFile().getAbsolutePath();
            File file2 = new File(parentPath, ofilename);
            if (file2 != null) {
                ofilename = file2.getAbsolutePath();
            }
        }


//       String simpleFormat = OCSSW_L3mapgenController.getPreferenceOfileNamingSchemeSimpleFormat();

        //   simpleFormat = "[IFILE]_mapped";
        String DEFAULT_SUFFIX = "mapped";

//        boolean filenameReplaced = false;
//        String ofileBasename = simpleFormat;
//        if (ofileBasename != null) {
//            ofileBasename = ofileBasename.trim();
//            if (ofileBasename.length() > 0) {
//                String  ofileBasename2 = ofileBasename.replace("[IFILE]",ifileBasename);
//                ofileBasename2 = ofileBasename2.replace("[ifile]",ofileBasename2);
//                if (!ofileBasename.equals(ofileBasename2)) {
//                    filenameReplaced = true;
//                    ofileBasename = ofileBasename2;
//                }
//            }
//        }
//
//        if (ofileBasename == null || ofileBasename.trim().length() == 0) {
////            ofileBasename = ifileBasename + "." + DEFAULT_SUFFIX;
////            filenameReplaced = true;
//
//            return "";
//        }


//        ofilename += getOfileForL3MapGenAddOns(resolution, product, projection);
//
//
//        if (ofilename.equalsIgnoreCase(ifileBasename)) {
//            ofilename = ofilename + "." + DEFAULT_SUFFIX;
//        }
//
//
//        ofilename += getOfileForL3MapGenAddExtension(ofilename, oformat);

        // add the path
//        if (!filenameReplaced) {
//            File file = new File(ifilename);
//            if (file != null) {
//                String parentPath = file.getParentFile().getAbsolutePath();
//                File file2 = new File(parentPath, ofilename);
//                if (file2 != null) {
//                    ofilename = file2.getAbsolutePath();
//                }
//            }
//        }

        return ofilename;
    }


    private static String getOfileWithReplaceForL3MapGen(String ifilename) {

        String parentPath = null;
        String ifileBasename = ifilename;
        boolean pathRemoved = false;
        String ofileBaseName;

        File ifileFile = new File(ifilename);

        if (ifileFile != null) {
            parentPath = ifileFile.getParentFile().getAbsolutePath();
            ifileBasename = ifileFile.getName();
            pathRemoved = true;
        }

        ifileBasename = stripFilenameExtension(ifileBasename);

        String orginalKeyString = OCSSW_L3mapgenController.getPreferenceOfileNamingSchemeIfileOriginal();
        String replacementKeyString = OCSSW_L3mapgenController.getPreferenceOfileNamingSchemeIfileReplace();
        if (replacementKeyString == null) {
            replacementKeyString = "";
        }

        if (orginalKeyString != null && orginalKeyString.length() > 0) {
            ofileBaseName = ifileBasename.replace(orginalKeyString, replacementKeyString);
        } else {
            ofileBaseName = ifileBasename;
        }

        String ofilename = null;

        if (pathRemoved) {
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
