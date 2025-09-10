package gov.nasa.gsfc.seadas.processing.core;

import com.bc.ceres.core.runtime.Version;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
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

    private long startTime = 0;
    // todo temporarily disabling these for Docker
    private boolean runProcessorToAutoPopulateL2bin = true;
    private boolean runProcessorToAutoPopulateL3mapgen = true;

    private boolean progressMonitorIsRunning = false;

    private boolean workingUpdateOfile = false;

    private String programName;
    String ofileNameDefault = "";
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
            L1BEXTRACT_OCI = "l1bextract_oci",
            L1BEXTRACT_OCI_XML_FILE = "l1bextract_oci.xml",
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
    public static OCSSW ocssw;
    private String fileExtensions = null;

    FileInfo inputFileInfo;

    public ProcessorModel(String name, OCSSW ocssw) {

        setProgramName(name);
        this.setOcssw(ocssw);
        acceptsParFile = false;
        hasGeoFile = false;
        readyToRun = true;
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
        processorID = ProcessorTypeInfo.getProcessorID(getProgramName());
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
//            System.out.println("suite" + "|" + currentOption.getValue() + "|" + newValue + "|" + suiteChanged);
            if (!suiteChanged) {
                ignore = true;
            }
        }
        if (currentOption.getName().equals("ifile")) {
            ifileChanged = !doTheyEqualAfterTrimming(currentOption.getValue(), newValue);
//            System.out.println("ifile" + "|" + currentOption.getValue() + "|" + newValue + "|" + ifileChanged);
            if (!ifileChanged) {
                ignore = true;
            }
        }

        if (!ignore) {
            updateParamInfo(currentOption.getName(), newValue);
            if ("l2bin".equalsIgnoreCase(getProgramName())) {
                if (ifileChanged) {
//                    System.out.println("PROBABLY NOT NEEDED NEVER HIT updateL2BinParams() SPOT zxcvzbzb");
                    updateL2BinParams(true);
//                    updateParamsWithProgressMonitor("Re-Initializing with input file '" + getPrimaryInputFileOptionName() + "'");
                } else if (suiteChanged) {
                    setWorkingUpdateOfile(true);
//                    System.out.println("SPOT SUITE SELECTED: updateL2BinParams() 1    qweioyrtrt");
                    updateL2BinParams(false);
//                    System.out.println("SPOT SUITE SELECTED: updateL2BinParams() 2    qweioyrtrt");
                    setWorkingUpdateOfile(false);
                    l2BinPropertyChangeHandler();

//
//                    String resolution = getParamValue("resolution");
//                    String suite = getParamValue("suite");
//                    String l3bprod = getParamValue("l3bprod");
//                    String prodtype = getParamValue("prodtype");
//                    String north = getParamValue("latnorth");
//                    String south = getParamValue("latsouth");
//                    String west = getParamValue("lonwest");
//                    String east = getParamValue("loneast");
//
//                    String ifileName = getPrimaryInputFileOptionName();
//                    String ofileName = OFileUtils.getOfileForL2BinWrapper(ifileName, ofileNameDefault, getProgramName(), resolution, l3bprod, suite, prodtype, north, south, west, east);
//                    updateOFileInfo(ofileName);


//                    updateParamsWithProgressMonitor("Reconfiguring with suite '" + getParamInfo("suite").getValue());
                }
            }
            if ("l3mapgen".equalsIgnoreCase(getProgramName())) {
                if (ifileChanged) {
//                    System.out.println("PROBABLY NOT NEEDED NEVER HIT updateL3MapgenParams() SPOT fdfddss");
                    updateL3MapgenParams(true);
//                    updateParamsWithProgressMonitor("Re-initializing with input file '" + getPrimaryInputFileOptionName() + "'");
                } else if (suiteChanged) {
                    setWorkingUpdateOfile(true);
//                    System.out.println("SPOT SUITE SELECTED: updateL3MapgenParams() 1   thhhtv");
                    updateL3MapgenParams(false);
//                    System.out.println("SPOT SUITE SELECTED: updateL3MapgenParams() 2   thhhtv");
                    setWorkingUpdateOfile(false);
                    l3mapgenPropertyChangeHandler();

//                    String resolution = getParamValue("resolution");
//                    String oformat = getParamValue("oformat");
//                    String product = getParamValue("product");
//                    String projection = getParamValue("projection");
//                    String interp = getParamValue("interp");
//                    String north = getParamValue("north");
//                    String south = getParamValue("south");
//                    String west = getParamValue("west");
//                    String east = getParamValue("east");
//                    String ifileName = getPrimaryInputFileOptionName();
//                    String ofileName = OFileUtils.getOfileForL3MapGenWrapper(ifileName, ofileNameDefault, getProgramName(), resolution, oformat, product, projection, interp, north, south, west, east);
//                    updateOFileInfo(ofileName);
//                    updateParamsWithProgressMonitor("Reconfiguring with suite '" + getParamInfo("suite").getValue());
                }
            }

            checkCompleteness();
        }
    }



    public void retrieveParamsFromProgram(boolean ifileChanged, boolean suiteChanged) {
        // this gets overridden and used if needed for instance by l2bin and l3mapgen

    }


    protected void checkCompleteness() {
        boolean complete = true;

        for (ParamInfo param : paramList.getParamArray()) {
            if ( (param.getType().equals(ParamInfo.Type.IFILE)||param.getType().equals(ParamInfo.Type.OFILE)) &&
                    (param.getValue() == null || param.getValue().trim().length() == 0) )
            {
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

    public boolean workingUpdateOfile() {
        return workingUpdateOfile;
    }

    public void setWorkingUpdateOfile(boolean workingUpdateOfile) {
         this.workingUpdateOfile = workingUpdateOfile;
    }


    public void updateParamInfo(String paramName, String newValue) {

        ParamInfo option = getParamInfo(paramName);
        if (option != null) {
            String oldValue = option.getValue();
            option.setValue(newValue);
            //checkCompleteness();
            if (!(oldValue.contains(newValue) && oldValue.trim().length() == newValue.trim().length())) {
                SeadasFileUtils.debug("property changed from " + oldValue + " to " + newValue);
                propertyChangeSupport.firePropertyChange(option.getName(), oldValue, newValue);
            }
        }
    }


//
//    public void updateParamsWithProgressMonitor(String progressMonitorMessage) {
//        if (!progressMonitorIsRunning) {
//            progressMonitorIsRunning = true;
//            ProgressMonitorSwingWorker pmSwingWorker = new ProgressMonitorSwingWorker(SnapApp.getDefault().getMainFrame(), getProgramName()) {
//
//                @Override
//                protected Void doInBackground(com.bc.ceres.core.ProgressMonitor pm) throws Exception {
//
//                    int totalWorkPlannedMaster = 100;
//                    int workDoneMaster = 0;
//                    pm.beginTask(progressMonitorMessage, totalWorkPlannedMaster);
//
//
//                    try {
//                        if ("l2bin".equalsIgnoreCase(getProgramName())) {
////                            System.out.println("updateL3MapgenParams() SPOT swswswwsw");
//                            updateL2BinParams();
//                        }
//                        if ("l3mapgen".equalsIgnoreCase(getProgramName())) {
////                            System.out.println("updateL3MapgenParams() SPOT ddededede");
//                            updateL3MapgenParams();
//                        }
//
//                        if (pm != null && pm.isCanceled()) {
//                            pm.done();
//                            progressMonitorIsRunning = false;
//
//                            return null;
//                        }
//                    } finally {
//                        if (pm != null && pm.isCanceled()) {
//                            pm.done();
//                            progressMonitorIsRunning = false;
//
//                            return null;
//                        }
//                        pm.done();
//                    }
//
//                    return null;
//                }
//            };
//
//            pmSwingWorker.executeWithBlocking();
//
//            progressMonitorIsRunning = false;
//        }
//    }
//
//
//
//    public boolean updateIFileInfoWithProgressMonitor(String ifileName) {
//        if (!progressMonitorIsRunning) {
//            progressMonitorIsRunning = true;
//
//            ProgressMonitorSwingWorker pmSwingWorker = new ProgressMonitorSwingWorker(SnapApp.getDefault().getMainFrame(), getProgramName()) {
//
//                @Override
//                protected Void doInBackground(com.bc.ceres.core.ProgressMonitor pm) throws Exception {
//
//                    int totalWorkPlannedMaster = 100;
//                    int workDoneMaster = 0;
//                    pm.beginTask("Initializing with input file '" + ifileName + "'", totalWorkPlannedMaster);
//
//
//                    try {
//                        updateIFileInfo(ifileName);
//
//                        if (pm != null && pm.isCanceled()) {
//                            pm.done();
//                            progressMonitorIsRunning = false;
//                            return null;
//                        }
//                    } finally {
//                        if (pm != null && pm.isCanceled()) {
//                            pm.done();
//                            progressMonitorIsRunning = false;
//
//                            return null;
//                        }
//                        pm.done();
//                    }
//                    progressMonitorIsRunning = false;
//
//                    return null;
//                }
//            };
//
//            pmSwingWorker.executeWithBlocking();
//
//        }
//
//        progressMonitorIsRunning = false;
//
//        return isIfileValid;
//    }
//


    public boolean updateIFileInfo(String ifileName) {
        System.out.println("Enter  updateIFileInfo(String ifileName)  - ifileName=" + ifileName);
        System.out.println("Time elapsed=" + timeElapsed());



        if (getProgramName() != null && (getProgramName().equals("multilevel_processor"))) {
            return true;
        }

        workingUpdateOfile = true;
        ParamInfo suiteParamInfo = getParamInfo("suite");
        if (suiteParamInfo != null) {
            setParamValue("suite", suiteParamInfo.getDefaultValue());
        }


        File ifile = new File(ifileName);

        inputFileInfo = new FileInfo(ifile.getParent(), ifile.getName(), ocssw);

        if (getProgramName() != null && verifyIFilePath(ifileName)) {

            if ("l3mapgen".equalsIgnoreCase(getProgramName())) {

                ofileNameDefault = ocssw.getOfileName(ifileName, getProgramName());

                if (ofileNameDefault != null) {
                    isIfileValid = true;
                    updateParamInfo(getPrimaryInputFileOptionName(), ifileName + "\n");
                    //updateGeoFileInfo(ifileName, inputFileInfo);
                    updateParamValues(new File(ifileName));

                    String resolution = getParamValue("resolution");
                    String oformat = getParamValue("oformat");
                    String product = getParamValue("product");
                    String projection = getParamValue("projection");
                    String interp = getParamValue("interp");
                    String north = getParamValue("north");
                    String south = getParamValue("south");
                    String west = getParamValue("west");
                    String east = getParamValue("east");

                    System.out.println("SPOT IFILE SELECTED: getOfileForL3MapGenWrapper() 1 uuuiiiii");
                    System.out.println("Time elapsed=" + timeElapsed());
                    String ofileName = OFileUtils.getOfileForL3MapGenWrapper(ifileName, ofileNameDefault, getProgramName(), resolution, oformat, product, projection, interp, north, south, west, east);
                    System.out.println("SPOT IFILE SELECTED: getOfileForL3MapGenWrapper() 2");
                    System.out.println("Time elapsed=" + timeElapsed());

                    updateOFileInfo(ofileName);
                    System.out.println("Time elapsed=" + timeElapsed());

                } else {
                    badIfileClearAndWarn(ifileName);
                }


            } else if ("l2bin".equalsIgnoreCase(getProgramName())) {

                ofileNameDefault = ocssw.getOfileName(ifileName, getProgramName());

                if (ofileNameDefault != null) {
                    isIfileValid = true;
                    updateParamInfo(getPrimaryInputFileOptionName(), ifileName + "\n");
                    //l2bin doesn't need geofile
                    //updateGeoFileInfo(ifileName, inputFileInfo);
                    updateParamValues(new File(ifileName));

                    String resolution = getParamValue("resolution");
                    String suite = getParamValue("suite");
                    String l3bprod = getParamValue("l3bprod");
                    String prodtype = getParamValue("prodtype");
                    String north = getParamValue("latnorth");
                    String south = getParamValue("latsouth");
                    String west = getParamValue("lonwest");
                    String east = getParamValue("loneast");

                    System.out.println("SPOT IFILE SELECTED: getOfileForL3BinWrapper() 1   reererrre");
                    String ofileName = OFileUtils.getOfileForL2BinWrapper(ifileName, ofileNameDefault, getProgramName(), resolution, l3bprod, suite, prodtype, north, south, west, east);
                    System.out.println("SPOT IFILE SELECTED: getOfileForL3BinWrapper() 2 ");

                    updateOFileInfo(ofileName);

                } else {
                    badIfileClearAndWarn(ifileName);
                }



            } else if ("l3bin".equalsIgnoreCase(getProgramName())) {

                ofileNameDefault = ocssw.getOfileName(ifileName, getProgramName());

                if (ofileNameDefault != null) {
                    isIfileValid = true;
                    updateParamInfo(getPrimaryInputFileOptionName(), ifileName + "\n");
//                    updateGeoFileInfo(ifileName, inputFileInfo);
                    updateParamValues(new File(ifileName));

                    String resolve = getParamValue("resolve");
                    String prod = getParamValue("prod");
                    String north = getParamValue("latnorth");
                    String south = getParamValue("latsouth");
                    String west = getParamValue("lonwest");
                    String east = getParamValue("loneast");

                    String ofileName = OFileUtils.getOfileForL3BinWrapper(ifileName, ofileNameDefault, getProgramName(), resolve, prod, north, south, west, east);

                    updateOFileInfo(ofileName);

                } else {
                    badIfileClearAndWarn(ifileName);
                }

            } else {
                //ocssw.setIfileName(ifileName);
                String ofileName = getOcssw().getOfileName(ifileName, getProgramName());

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

            Answer answer = Dialogs.requestDecision(getProgramName(), "Cannot compute output file name. Would you like to continue anyway?", true, null);
            switch (answer) {
                case CANCELLED:
                    updateParamInfo(getPrimaryInputFileOptionName(), "" + "\n");
                    break;
            }
        }

        workingUpdateOfile = false;


        return isIfileValid;
    }





    private void badIfileClearAndWarn(String ifileName) {

        String SPACES = "&nbsp;&nbsp;&nbsp;&nbsp;";

        String[] msgArray = {"WARNING!: Failed to authenticate input file.",
                "Input file = '" + ifileName + "'",
                "",
                "Possible failure reasons:",
                "1. The input file is not a valid or supported satellite data file",
                "2. The python3 'requests' module is not installed.",
                SPACES + SPACES + "See: Menu > Seadas-Toolbox > Software & System Info",
                SPACES + SPACES + "Look for: Python3 Requests Installed: YES"};

        StringBuilder stringBuilder = new StringBuilder("<html><br>");
        for (String msg : msgArray) {
            stringBuilder.append(SPACES + msg + SPACES + "<br>");
        }
        stringBuilder.append("<br>"+ SPACES + "</html>");


        SimpleDialogMessage dialog = new SimpleDialogMessage("Process Warning for: " + programName, stringBuilder.toString());
        dialog.setVisible(true);
        dialog.setEnabled(true);

        // todo This doesn't seem to clear correctly the params in the GUI
//        for (ParamInfo paramInfo : paramList.getParamArray()) {
//            updateParamInfo(paramInfo.getName(), paramInfo.getDefaultValueOriginal());
//        }
//        updateParamInfo(getPrimaryInputFileOptionName(), "");
//        updateParamInfo(getPrimaryOutputFileOptionName(), "");

    }

    //todo: change the path to get geo filename from ifile
    public boolean updateGeoFileInfo(String ifileName, FileInfo inputFileInfo) {
        if (isGeofileRequired()) {
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

        return false;
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

    private long timeElapsed() {
        long timeElapsed = (System.nanoTime() - startTime) / 1000000;

        return timeElapsed;
    }

    public void setParamValue(String name, String value) {

        if (startTime == 0) {
            startTime = System.nanoTime();
        }
        System.out.println("Enter: setParamValue(String name, String value)  - " + name + " - " + value);
        System.out.println("Time elapsed=" + timeElapsed());
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

                if ("l2bin".equalsIgnoreCase(getProgramName()) || "l3mapgen".equalsIgnoreCase(getProgramName())) {
                    System.out.println("current ifile =" + getParamInfo("ifile").getValue());
                    System.out.println("new ifile =" + value);
                    String currentIfile = getParamInfo("ifile").getValue();
                    if (currentIfile == null) {currentIfile = "";}
                    String newIfile = value;
                    if (newIfile == null) { newIfile = ""; }

                    if (!currentIfile.trim().equalsIgnoreCase(newIfile.trim())) {
//                        updateIFileInfoWithProgressMonitor(value);
                        updateIFileInfo(value);
                    }
                } else {
                    updateIFileInfo(value);
                }

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

        System.out.println("Time elapsed=" + timeElapsed());
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
        System.out.println("Enter: updateParamValues(File selectedFile)");
        System.out.println("Time elapsed=" + timeElapsed());

        if (selectedFile != null && getProgramName() != null && (l2prodProcessors.contains(getProgramName()) || "l3mapgen".equalsIgnoreCase(getProgramName()))) {
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

            if ("l3mapgen".equalsIgnoreCase(getProgramName()) || "l3bin".equalsIgnoreCase(getProgramName())) {
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

                if ("l3mapgen".equalsIgnoreCase(getProgramName())) {
                    setWorkingUpdateOfile(true);
                    System.out.println("Time elapsed=" + timeElapsed());
                    System.out.println("SPOT IFILE SELECTED: updateL3MapgenParams() 1   tyuuuii");
                    updateL3MapgenParams(true);
                    System.out.println("SPOT IFILE SELECTED: updateL3MapgenParams() 2   tyuuuii");
                    System.out.println("Time elapsed=" + timeElapsed());
                    setWorkingUpdateOfile(false);

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

                        if ("l2bin".equalsIgnoreCase(getProgramName())) {
                            if (v != null && v.getShortName().equalsIgnoreCase("l2_flags")) {
                                updateParamValuesFlagUse(v);
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
            if ("l2bin".equalsIgnoreCase(getProgramName())) {
                updateProductFieldWithBandNames("l3bprod", bandNames);
                updateProductFieldWithBandNames("composite_prod", bandNames);
            }

            if ("l3mapgen".equalsIgnoreCase(getProgramName())) {
                updateProductFieldWithBandNames("product", bandNames);
            }

            if ("l3bin".equalsIgnoreCase(getProgramName())) {
                updateProductFieldWithBandNames("prod", bandNames);
                updateProductFieldWithBandNames("composite_prod", bandNames);
            }
        }

        if ("l2bin".equalsIgnoreCase(getProgramName()) || "l3mapgen".equalsIgnoreCase(getProgramName())) {
            updateSuite(selectedFile);
        }
    }


    private void updateParamValuesFlagUse(Variable flagGroup) {

        try {
            Attribute flagMeaningAttribute = flagGroup.attributes().findAttribute("flag_meanings");
            Array array = flagMeaningAttribute.getValues();
            String flagMeanings = array.toString();

            if (flagMeanings.length() > 0) {
                if ("l2bin".equalsIgnoreCase(getProgramName())) {
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

        if ("l2bin".equalsIgnoreCase(getProgramName())) {
            setWorkingUpdateOfile(true);
            System.out.println("SPOT IFILE SELECTED: updateL2BinParams() 1    eeeeeeee");
            updateL2BinParams(true);
            System.out.println("SPOT IFILE SELECTED: updateL2BinParams() 2    eeeeeeee");
            setWorkingUpdateOfile(false);
//            l2BinPropertyChangeHandler();

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

//            paramList.getPropertyChangeSupport().firePropertyChange(field, "-1", newValue);
            updateParamInfo(field, newValue);
//            fireEvent(field, "-1", newValue);
            boolean bugCheckDeleteMe = true;
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
            if (getProgramName() != null && verifyIFilePath(ifileName)) {
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

                Answer answer = Dialogs.requestDecision(getProgramName(), "Cannot compute output file name. Would you like to continue anyway?", true, null);
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
                    setProgramName(L1AEXTRACT_MODIS);
                    xmlFileName = L1AEXTRACT_MODIS_XML_FILE;
                } else if (missionName.indexOf("SeaWiFS") != -1 && fileType.indexOf("1A") != -1 || missionName.indexOf("CZCS") != -1) {
                    setProgramName(L1AEXTRACT_SEAWIFS);
                    xmlFileName = L1AEXTRACT_SEAWIFS_XML_FILE;
                } else if (missionName.indexOf("OCI") != -1 && fileType.indexOf("1B") != -1) {
                    setProgramName(L1BEXTRACT_OCI);
                    xmlFileName = L1BEXTRACT_OCI_XML_FILE;
                } else if ((missionName.indexOf("VIIRS") != -1
                        || missionName.indexOf("VIIRSJ1") != -1
                        || missionName.indexOf("VIIRSJ2") != -1)
                        && fileType.indexOf("1A") != -1) {
                    setProgramName(L1AEXTRACT_VIIRS);
                    xmlFileName = L1AEXTRACT_VIIRS_XML_FILE;
                } else if ((fileType.indexOf("L2") != -1 || fileType.indexOf("Level 2") != -1) ||
                        (missionName.indexOf("OCTS") != -1 && (fileType.indexOf("L1") != -1 || fileType.indexOf("Level 1") != -1))) {
                    setProgramName(L2EXTRACT);
                    xmlFileName = L2EXTRACT_XML_FILE;
                }
            }
            setProgramName(getProgramName());
            ocssw.setProgramName(getProgramName());
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


    private void updateL3MapgenParams(boolean ifileChanged) {
        System.out.println("Enter: updateL3MapgenParams()");
        if (!runProcessorToAutoPopulateL3mapgen) {
            return;
        }

        if (!"l3mapgen".equalsIgnoreCase(getProgramName())) {
            return;
        }

        ParamInfo ifileParamInfo = paramList.getInfo("ifile");
        if (ifileParamInfo != null) {
            File ifile = new File(ifileParamInfo.getValue());

            if (ifile != null && ifile.exists()) {
                String suite = ParamInfo.NULL_STRING;;
                if (ifileChanged) {
                    if (OCSSW_L3mapgenController.getPreferenceSuite() != null && OCSSW_L3mapgenController.getPreferenceSuite().trim().length() > 0 ) {
                        suite = OCSSW_L3mapgenController.getPreferenceSuite().trim();
                    }

                    ParamInfo option = getParamInfo("suite");
                    if (option != null) {
                        option.setValue(suite);
                    }

                } else {
                    ParamInfo suiteParamInfo = paramList.getInfo("suite");
                    if (suiteParamInfo != null) {
                        suite = suiteParamInfo.getValue();
                    }
                }


                final String L3MAPGEN_PROGRAM_NAME = "l3mapgen";
//                File dataDir = SystemUtils.getApplicationDataDir();
//                File auxDir = new File(dataDir, "auxdata");
//                File l3mapgenAuxDir = new File(auxDir, L3MAPGEN_PROGRAM_NAME);
//                l3mapgenAuxDir.mkdirs();

                File workingDir = ifile.getParentFile();

                if (workingDir != null && workingDir.exists()) {

                    File l3mapgenConfigParFile = new File(workingDir, L3MAPGEN_PROGRAM_NAME + "_" + ifile.getName() +"_config_params.par");
                    try {
                        createL2binAuxParFile(L3MAPGEN_PROGRAM_NAME, ifile, suite, l3mapgenConfigParFile);

                        if (l3mapgenConfigParFile.exists()) {
                            boolean precedence = OCSSW_L3mapgenController.getPreferenceAutoFillPrecedence();
                            boolean passAll = OCSSW_L3mapgenController.getPreferencePassAll();


                            if (OCSSW_L3mapgenController.getPreferenceAutoFillAll() || OCSSW_L3mapgenController.getPreferenceAutoFillProduct()) {
                                updateParamInfosFromAuxParFile(l3mapgenConfigParFile.getAbsolutePath(), "product", OCSSW_L3mapgenController.getPreferenceProduct(), precedence, passAll);
                                updateParamInfosFromAuxParFile(l3mapgenConfigParFile.getAbsolutePath(), "wavelength_3D", OCSSW_L3mapgenController.getPreferenceWavelength3D(), precedence, passAll);
                            }

                            if (OCSSW_L3mapgenController.getPreferenceAutoFillAll() || OCSSW_L3mapgenController.getPreferenceAutoFillOther()) {
                                updateParamInfosFromAuxParFile(l3mapgenConfigParFile.getAbsolutePath(), "projection", OCSSW_L3mapgenController.getPreferenceProjection(), precedence, passAll);

                                // replace smi if specified in preferences
                                ParamInfo projectionParamInfo = paramList.getInfo("projection");
                                if (projectionParamInfo != null && "smi".equals(projectionParamInfo.getValue())) {
                                    String smiReplacement = OCSSW_L3mapgenController.getPreferenceProjectionSmiReplacement();
                                    if (smiReplacement != null && smiReplacement.trim().length() > 0) {
                                        updateParamInfo("projection", smiReplacement);
                                        fireEvent("projection", "smi", smiReplacement);
                                    }
                                }

                                updateParamInfosFromAuxParFile(l3mapgenConfigParFile.getAbsolutePath(), "resolution", OCSSW_L3mapgenController.getPreferenceResolution(), precedence, passAll);
                                updateParamInfosFromAuxParFile(l3mapgenConfigParFile.getAbsolutePath(), "width", OCSSW_L3mapgenController.getPreferenceWidth(), precedence, passAll);
                                updateParamInfosFromAuxParFile(l3mapgenConfigParFile.getAbsolutePath(), "interp", OCSSW_L3mapgenController.getPreferenceInterp(), precedence, passAll);
                                updateParamInfosFromAuxParFile(l3mapgenConfigParFile.getAbsolutePath(), "fudge", OCSSW_L3mapgenController.getPreferenceFudge(), precedence, passAll);
//                            updateParamInfosFromAuxParFile(auxParFile.getAbsolutePath(), "threshold",  "", precedence, passAll);
                                updateParamInfosFromAuxParFile(l3mapgenConfigParFile.getAbsolutePath(), "mask_land", OCSSW_L3mapgenController.getPreferenceMaskLand(), precedence, passAll);
                                updateParamInfosFromAuxParFile(l3mapgenConfigParFile.getAbsolutePath(), "rgb_land", OCSSW_L3mapgenController.getPreferenceRGBLand(), precedence, passAll);
                                updateParamInfosFromAuxParFile(l3mapgenConfigParFile.getAbsolutePath(), "land", OCSSW_L3mapgenController.getPreferenceLand(), precedence, passAll);
                                updateParamInfosFromAuxParFile(l3mapgenConfigParFile.getAbsolutePath(), "apply_pal", OCSSW_L3mapgenController.getPreferenceApplyPal(), precedence, passAll);
                                updateParamInfosFromAuxParFile(l3mapgenConfigParFile.getAbsolutePath(), "palfile", OCSSW_L3mapgenController.getPreferencePalfile(), precedence, passAll);
                                updateParamInfosFromAuxParFile(l3mapgenConfigParFile.getAbsolutePath(), "datamin", OCSSW_L3mapgenController.getPreferenceDataMin(), precedence, passAll);
                                updateParamInfosFromAuxParFile(l3mapgenConfigParFile.getAbsolutePath(), "datamax", OCSSW_L3mapgenController.getPreferenceDataMax(), precedence, passAll);
                                updateParamInfosFromAuxParFile(l3mapgenConfigParFile.getAbsolutePath(), "scale_type", OCSSW_L3mapgenController.getPreferenceScaleType(), precedence, passAll);
                                updateParamInfosFromAuxParFile(l3mapgenConfigParFile.getAbsolutePath(), "use_transparency", OCSSW_L3mapgenController.getPreferenceUseTransparency(), precedence, passAll);
                                updateParamInfosFromAuxParFile(l3mapgenConfigParFile.getAbsolutePath(), "use_rgb", OCSSW_L3mapgenController.getPreferenceUseRGB(), precedence, passAll);
                                updateParamInfosFromAuxParFile(l3mapgenConfigParFile.getAbsolutePath(), "product_rgb", OCSSW_L3mapgenController.getPreferenceProductRGB(), precedence, passAll);
                                updateParamInfosFromAuxParFile(l3mapgenConfigParFile.getAbsolutePath(), "num_cache", OCSSW_L3mapgenController.getPreferenceNumCache(), precedence, passAll);
                                updateParamInfosFromAuxParFile(l3mapgenConfigParFile.getAbsolutePath(), "oformat", OCSSW_L3mapgenController.getPreferenceOformat(), precedence, passAll);

                            }
                        }
                    } catch (IOException e) {
                        SimpleDialogMessage dialog = new SimpleDialogMessage(L3MAPGEN_PROGRAM_NAME + " - Warning", "Failed to initialize default params from file: " + l3mapgenConfigParFile.getAbsolutePath());
                        dialog.setVisible(true);
                        dialog.setEnabled(true);
                    }

                    l3mapgenConfigParFile.delete();

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

    private void updateL2BinParams(boolean ifileChanged) {
        if (!runProcessorToAutoPopulateL2bin) {
            return;
        }

        if (!"l2bin".equalsIgnoreCase(getProgramName())) {
            return;
        }

        ParamInfo ifileParamInfo = paramList.getInfo("ifile");
        if (ifileParamInfo != null) {
            File ifile = new File(ifileParamInfo.getValue());
            File textFile = null;


            if (ifile != null && ifile.exists()) {
                String suite = ParamInfo.NULL_STRING;;
                if (ifileChanged) {
                    if (OCSSW_L2binController.getPreferenceSuite() != null && OCSSW_L2binController.getPreferenceSuite().trim().length() > 0 ) {
                        suite = OCSSW_L2binController.getPreferenceSuite().trim();
                    }

                    ParamInfo option = getParamInfo("suite");
                    if (option != null) {
                        option.setValue(suite);
                    }

                } else {
                    ParamInfo suiteParamInfo = paramList.getInfo("suite");
                    if (suiteParamInfo != null) {
                        suite = suiteParamInfo.getValue();
                    }
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
//                File dataDir = SystemUtils.getApplicationDataDir();
//                File auxDir = new File(dataDir, "auxdata");
//                File l2binAuxDir = new File(auxDir, L2BIN_PROGRAM_NAME);
//                l2binAuxDir.mkdirs();

                File workingDir = ifile.getParentFile();


                if (workingDir != null && workingDir.exists()) {


                    File l2binConfigParFile = new File(workingDir, L2BIN_PROGRAM_NAME + "_" + ifile.getName() +"_config_params.par");
                    try {
                        if (textFile != null) {
                            createL2binAuxParFile(L2BIN_PROGRAM_NAME, textFile, suite, l2binConfigParFile);
                        } else {
                            createL2binAuxParFile(L2BIN_PROGRAM_NAME, ifile, suite, l2binConfigParFile);
                        }
                        if (l2binConfigParFile.exists()) {

                            boolean precedence = OCSSW_L2binController.getPreferenceAutoFillPrecedence();
                            boolean passAll = OCSSW_L2binController.getPreferencePassAll();

                            if (OCSSW_L2binController.getPreferenceAutoFillAll() || OCSSW_L2binController.getPreferenceFlaguseAutoFillEnable()) {
                                updateParamInfosFromAuxParFile(l2binConfigParFile.getAbsolutePath(), "flaguse", OCSSW_L2binController.getPreferenceFlaguse(), precedence, passAll);
                            }

                            if (OCSSW_L2binController.getPreferenceAutoFillAll() || OCSSW_L2binController.getPreferenceL3bprodAutoFillEnable()) {
                                updateParamInfosFromAuxParFile(l2binConfigParFile.getAbsolutePath(), OCSSW_L2binController.PROPERTY_L2BIN_L3BPROD_LABEL, OCSSW_L2binController.getPreferenceL3bprod(), precedence, passAll);
                                updateParamInfosFromAuxParFile(l2binConfigParFile.getAbsolutePath(), "output_wavelengths", OCSSW_L2binController.getPreferenceOutputWavelengths(), precedence, passAll);
                            }

                            if (OCSSW_L2binController.getPreferenceAutoFillAll() || OCSSW_L2binController.getPreferenceAutoFillEnable()) {
                                updateParamInfosFromAuxParFile(l2binConfigParFile.getAbsolutePath(), "resolution", OCSSW_L2binController.getPreferenceResolution(), precedence, passAll);
                                updateParamInfosFromAuxParFile(l2binConfigParFile.getAbsolutePath(), "area_weighting", OCSSW_L2binController.getPreferenceAreaWeighting(), precedence, passAll);
                                updateParamInfosFromAuxParFile(l2binConfigParFile.getAbsolutePath(), "latnorth", OCSSW_L2binController.getPreferenceLatnorth(), precedence, passAll);
                                updateParamInfosFromAuxParFile(l2binConfigParFile.getAbsolutePath(), "latsouth", OCSSW_L2binController.getPreferenceLatsouth(), precedence, passAll);
                                updateParamInfosFromAuxParFile(l2binConfigParFile.getAbsolutePath(), "lonwest", OCSSW_L2binController.getPreferenceLonwest(), precedence, passAll);
                                updateParamInfosFromAuxParFile(l2binConfigParFile.getAbsolutePath(), "loneast", OCSSW_L2binController.getPreferenceLoneast(), precedence, passAll);
                                updateParamInfosFromAuxParFile(l2binConfigParFile.getAbsolutePath(), "sday", OCSSW_L2binController.getPreferenceSday(), precedence, passAll);
                                updateParamInfosFromAuxParFile(l2binConfigParFile.getAbsolutePath(), "eday", OCSSW_L2binController.getPreferenceEday(), precedence, passAll);
                                updateParamInfosFromAuxParFile(l2binConfigParFile.getAbsolutePath(), "night", OCSSW_L2binController.getPreferenceNight(), precedence, passAll);
                                updateParamInfosFromAuxParFile(l2binConfigParFile.getAbsolutePath(), "delta_crossing_time", OCSSW_L2binController.getPreferenceDeltaCross(), precedence, passAll);
                                updateParamInfosFromAuxParFile(l2binConfigParFile.getAbsolutePath(), "prodtype", OCSSW_L2binController.getPreferenceProdtype(), precedence, passAll);
                                updateParamInfosFromAuxParFile(l2binConfigParFile.getAbsolutePath(), "rowgroup", OCSSW_L2binController.getPreferenceRowGroup(), precedence, passAll);
                                updateParamInfosFromAuxParFile(l2binConfigParFile.getAbsolutePath(), "composite_prod", OCSSW_L2binController.getPreferenceCompositeProd(), precedence, passAll);
                                updateParamInfosFromAuxParFile(l2binConfigParFile.getAbsolutePath(), "composite_scheme", OCSSW_L2binController.getPreferenceCompositeScheme(), precedence, passAll);
                                updateParamInfosFromAuxParFile(l2binConfigParFile.getAbsolutePath(), "qual_prod", "", precedence, passAll);
                                updateParamInfosFromAuxParFile(l2binConfigParFile.getAbsolutePath(), "qual_max", "", precedence, passAll);
                                updateParamInfosFromAuxParFile(l2binConfigParFile.getAbsolutePath(), "minobs", "", precedence, passAll);
                            }
                        }
                    } catch (IOException e) {
                        SimpleDialogMessage dialog = new SimpleDialogMessage(L2BIN_PROGRAM_NAME + " - Warning", "Failed to initialize default params from file: " + l2binConfigParFile.getAbsolutePath());
                        dialog.setVisible(true);
                        dialog.setEnabled(true);
                    }



                    l2binConfigParFile.delete();

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
        if (thisProgramName.equalsIgnoreCase(getProgramName())) {
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
            while (p.isAlive()) {
                // waiting for process to finish
            }
//            ocssw.waitForProcess();
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




    public void updateParamInfosFromAuxParFile(String parfile, String parameter, String prefValue, boolean precedence, boolean passAll) throws IOException {

        if (parameter == null || parameter.trim().length() == 0) {
            return;
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

        String valueOriginal = paramInfo.getValue();
        if (valueOriginal == null) {
            valueOriginal = "";
        }

        String valueNew = "";

        if (prefValue != null && prefValue.length() > 0) {
                valueNew = prefValue;
        } else {
            if (!valueOriginal.equals(paramInfo.getDefaultValueOriginal())) {
                valueNew = paramInfo.getDefaultValueOriginal();
            }
        }


        if (prefValue != null && prefValue.length() > 0 && precedence) {
            paramValueSet = true;
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
                            if ("boolean".equals(paramInfo.getType())) {
                                if ("1".equals(value) || "true".equalsIgnoreCase(value)) {
                                    value = "true";
                                } else {
                                    value = "false";
                                }
                            }

                            if (!passAll) {
                                paramInfo.setDefaultValue(value);
                            }

                            if (!paramValueSet) {
                                valueNew = value;
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
            suitesByMission = getSuites(selectedFile, missionDir, getProgramName());
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
            suitesByCommon = getSuites(selectedFile, commonDir, getProgramName());
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
            suitesBySubSensor = getSuites(selectedFile, subSensorDir, getProgramName());
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


    public void l3BinPropertyChangeHandler() {
        if (!workingUpdateOfile()) {
            setWorkingUpdateOfile(true);

            String ifileName = getParamValue(getPrimaryInputFileOptionName());
            String resolve = getParamValue("resolve");
            String prod = getParamValue("prod");
            String north = getParamValue("latnorth");
            String south = getParamValue("latsouth");
            String west = getParamValue("lonwest");
            String east = getParamValue("loneast");

            String ofileName = OFileUtils.getOfileForL3BinWrapper(ifileName, ofileNameDefault, getProgramName(), resolve, prod, north, south, west, east);

            updateOFileInfo(ofileName);

            setWorkingUpdateOfile(false);
        }
    }


    public void l2BinPropertyChangeHandler() {
        if (!workingUpdateOfile()) {
            setWorkingUpdateOfile(true);

            String ifileName = getParamValue(getPrimaryInputFileOptionName());
            String resolution = getParamValue("resolution");
            String l3bprod = getParamValue("l3bprod");
            String suite = getParamValue("suite");
            String prodtype = getParamValue("prodtype");
            String north = getParamValue("latnorth");
            String south = getParamValue("latsouth");
            String west = getParamValue("lonwest");
            String east = getParamValue("loneast");

            System.out.println("SPOT HANDLER: getOfileForL2BinWrapper() 1   rrrrrrrrr");
            String ofileName = OFileUtils.getOfileForL2BinWrapper(ifileName, ofileNameDefault, getProgramName(), resolution, l3bprod, suite, prodtype, north, south, west, east);
            System.out.println("SPOT HANDLER: getOfileForL2BinWrapper() 2");

            updateOFileInfo(ofileName);

            setWorkingUpdateOfile(false);
        }
    }

    public void l3mapgenPropertyChangeHandler() {
        if (!workingUpdateOfile()) {
            setWorkingUpdateOfile(true);

            String ifileName = getParamValue(getPrimaryInputFileOptionName());
            String resolution = getParamValue("resolution");
            String oformat = getParamValue("oformat");
            String product = getParamValue("product");
            String projection = getParamValue("projection");
            String interp = getParamValue("interp");
            String north = getParamValue("north");
            String south = getParamValue("south");
            String west = getParamValue("west");
            String east = getParamValue("east");

            if (projection != null && projection.trim().startsWith("#")) {
                ParamInfo projectionParamInfo = getParamInfo("projection");
                projectionParamInfo.setValue(projectionParamInfo.getDefaultValueOriginal());
                // todo Maybe this?
//                updateParamInfo("projection", projectionParamInfo.getDefaultValue());
            }

            System.out.println("SPOT HANDLER: getOfileForL3MapGenWrapper() 1 tttttttt");
            String ofileName = OFileUtils.getOfileForL3MapGenWrapper(ifileName, ofileNameDefault, getProgramName(), resolution, oformat, product, projection, interp, north, south, west, east);
            System.out.println("SPOT HANDLER: getOfileForL3MapGenWrapper() 2");


            updateOFileInfo(ofileName);

            setWorkingUpdateOfile(false);
        }
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
                    l2BinPropertyChangeHandler();
                }
            });

            addPropertyChangeListener("l3bprod", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                    l2BinPropertyChangeHandler();
                }
            });

//            addPropertyChangeListener("suite", new PropertyChangeListener() {
//                @Override
//                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
//                    l2BinPropertyChangeHandler();
//                    int stillHere = 0;
//                }
//            });

            addPropertyChangeListener("prodtype", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                    l2BinPropertyChangeHandler();
                }
            });

            addPropertyChangeListener("latnorth", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                    l2BinPropertyChangeHandler();
                }
            });


            addPropertyChangeListener("latsouth", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                    l2BinPropertyChangeHandler();
                }
            });


            addPropertyChangeListener("lonwest", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                    l2BinPropertyChangeHandler();
                }
            });


            addPropertyChangeListener("loneast", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                    l2BinPropertyChangeHandler();
                }
            });

        }

        @Override
        public void updateParamValues(Product selectedProduct) {
            System.out.println("TEST 54545454");

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

        private InputStream getParamInfoInputStream(String thisProgramName, File ifile, String suite, File xmlFile) throws IOException {

            if (xmlFile.exists()) {
                xmlFile.delete();
            }

            String executable = thisProgramName;
            if (thisProgramName.equalsIgnoreCase(getProgramName())) {
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
                while (p.isAlive()) {
                    // waiting for process to finish
                }
//                ocssw.waitForProcess();
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
                    l3BinPropertyChangeHandler();
                }
            });

            addPropertyChangeListener("resolve", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                    l3BinPropertyChangeHandler();
                }
            });

            addPropertyChangeListener("latnorth", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                    l3BinPropertyChangeHandler();
                }
            });


            addPropertyChangeListener("latsouth", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                    l3BinPropertyChangeHandler();
                }
            });


            addPropertyChangeListener("lonwest", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                    l3BinPropertyChangeHandler();
                }
            });

            addPropertyChangeListener("loneast", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                    l3BinPropertyChangeHandler();
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




    private static class L3MAPGEN_Processor extends ProcessorModel {

        L3MAPGEN_Processor(final String programName, String xmlFileName, OCSSW ocssw) {
            super(programName, xmlFileName, ocssw);
            setOpenInSeadas(true);

            ParamInfo palfileParamInfo = getParamInfo("palfile");

            if (palfileParamInfo != null) {
                OCSSWInfo ocsswInfo = OCSSWInfo.getInstance();
                if (ocsswInfo != null) {
                    String ocsswDataDirPath = ocsswInfo.getOcsswDataDirPath();
                    File ocsswDataDirFile = new File(ocsswDataDirPath);

                    if (ocsswDataDirFile != null && ocsswDataDirFile.exists()) {
                        File ocsswCommonDirFile = new File(ocsswDataDirFile, "common");

                        if (ocsswCommonDirFile != null && ocsswCommonDirFile.exists()) {
                            File ocsswCommonPaletteDirFile = new File(ocsswCommonDirFile, "palette");

                            if (ocsswCommonPaletteDirFile != null && ocsswCommonPaletteDirFile.exists()) {
                                File[] palfileFiles = ocsswCommonPaletteDirFile.listFiles();

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
                    }
                }
            }


            addPropertyChangeListener("resolution", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                    l3mapgenPropertyChangeHandler();
                }
            });


            addPropertyChangeListener("oformat", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                    l3mapgenPropertyChangeHandler();
                }
            });


            addPropertyChangeListener("product", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                    l3mapgenPropertyChangeHandler();
                }
            });


            addPropertyChangeListener("projection", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                    l3mapgenPropertyChangeHandler();
                }
            });


            addPropertyChangeListener("interp", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                    l3mapgenPropertyChangeHandler();
                }
            });


            addPropertyChangeListener("north", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                    l3mapgenPropertyChangeHandler();
                }
            });

            addPropertyChangeListener("south", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                    l3mapgenPropertyChangeHandler();
                }
            });

            addPropertyChangeListener("west", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                    l3mapgenPropertyChangeHandler();
                }
            });

            addPropertyChangeListener("east", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                    l3mapgenPropertyChangeHandler();
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

//
//        @Override
//        public void retrieveParamsFromProgram(boolean ifileChanged, boolean suiteChanged) {
//            // this gets overridden and used if needed for instance by l2bin and l3mapgen
//
//            if (ifileChanged) {
//                updateL3MapgenParams();
////                    updateParamsWithProgressMonitor("Re-initializing with input file '" + getPrimaryInputFileOptionName() + "'");
//            } else if (suiteChanged) {
//                updateL3MapgenParams();
////                    updateParamsWithProgressMonitor("Reconfiguring with suite '" + getParamInfo("suite").getValue());
//            }
//
//        }
//

    }

    public static String stripPalFilenameExtension(String filename) {
        if (filename == null || filename.trim().length() == 0) {
            return filename;
        }

        String fileBasename = filename;

        if (filename.endsWith(".pal")) {
            fileBasename = filename.substring(0, filename.length() - 4);
        }

        return fileBasename;
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
