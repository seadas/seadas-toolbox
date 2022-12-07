package gov.nasa.gsfc.seadas.processing.ocssw;

import com.bc.ceres.core.ProgressMonitor;
import gov.nasa.gsfc.seadas.processing.common.FileInfoFinder;
import gov.nasa.gsfc.seadas.processing.common.MissionInfo;
import gov.nasa.gsfc.seadas.processing.common.ParFileManager;
import gov.nasa.gsfc.seadas.processing.core.*;
import gov.nasa.gsfc.seadas.processing.utilities.SeadasArrayUtils;
import org.apache.commons.lang.ArrayUtils;
import org.esa.snap.core.util.Debug;
import org.esa.snap.rcp.util.Dialogs;

import java.io.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static gov.nasa.gsfc.seadas.processing.common.SeadasFileUtils.debug;
import static gov.nasa.gsfc.seadas.processing.core.L2genData.*;
import static gov.nasa.gsfc.seadas.processing.ocssw.OCSSWInfo.OCSSW_INSTALLER_PROGRAM_NAME;

/**
 * Created by aabduraz on 3/27/17.
 */

public class OCSSWLocal extends OCSSW {

    public static String TMP_OCSSW_INSTALLER_PROGRAM_PATH = (new File(System.getProperty("java.io.tmpdir"), OCSSW_INSTALLER_PROGRAM_NAME)).getPath();

    private static final String DEFAULTS_FILE_PREFIX = "msl12_defaults_",
            AQUARIUS_DEFAULTS_FILE_PREFIX = "l2gen_aquarius_defaults_",
            L3GEN_DEFAULTS_FILE_PREFIX = "msl12_defaults_";

    private String defaultsFilePrefix;

    private final static String L2GEN_PROGRAM_NAME = "l2gen",
            AQUARIUS_PROGRAM_NAME = "l2gen_aquarius",
            L3GEN_PROGRAM_NAME = "l3gen";

    Process process;

    public OCSSWLocal() {
        initialize();
    }

    private void initialize() {
        //missionInfo = new MissionInfo();

    }

    @Override
    public ProcessObserver getOCSSWProcessObserver(Process process, String processName, ProgressMonitor progressMonitor) {
        return new ProcessObserver(process, processName, progressMonitor);
    }

    @Override
    public boolean isMissionDirExist(String missionName) {
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

    @Override
    public int getProcessExitValue() {
        return process.exitValue();
    }

    @Override
    public void waitForProcess() {
        try {
            process.waitFor();
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
    }

    @Override
    public InputStream getProductXMLFile(L2genData.Source source) throws IOException {
        File ocsswShareDir = new File(OCSSWInfo.getInstance().getOcsswRoot(), SHARE_DIR);

        File ocsswCommonDir = new File(ocsswShareDir, COMMON_DIR);

        //todo
        File xmlFile  = new File(ocsswCommonDir, PRODUCT_XML);

        if (source == L2genData.Source.RESOURCES) {
            try {
                return new FileInputStream(xmlFile);
            } catch (IOException e) {
                throw new IOException("problem creating product XML file: " + e.getMessage());
            }
        }
        else if (source == Source.L2GEN) {
            try {
                return new FileInputStream(xmlFile);
            } catch (IOException e) {
                throw new IOException("problem creating product XML file: " + e.getMessage());
            }
        }
        return null;
    }

    @Override
    public Process execute(ProcessorModel processorModel) {
        setProgramName(processorModel.getProgramName());
        setIfileName(processorModel.getParamValue(processorModel.getPrimaryInputFileOptionName()));
        return execute(getProgramCommandArray(processorModel));
    }

    @Override
    public Process executeSimple(ProcessorModel processorModel) {
        return execute(processorModel);
    }

    @Override
    public InputStream executeAndGetStdout(ProcessorModel processorModel) {

        Process process = execute(processorModel);
        try {
            process.waitFor();
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
        return process.getInputStream();
    }

    @Override
    public Process execute(ParamList paramListl) {
        String[] programNameArray = {programName};
        commandArray = SeadasArrayUtils.concatAll(commandArrayPrefix, programNameArray, getCommandArrayParam(paramListl), commandArraySuffix);
        return execute(commandArray);
    }

    /**
     * this method returns a command array for execution.
     * the array is constructed using the paramList data and input/output files.
     * the command array structure is: full pathname of the program to be executed, input file name, params in the required order and finally the output file name.
     * assumption: order starts with 1
     *
     * @return
     */
    public String[] getProgramCommandArray(ProcessorModel processorModel) {

        String[] cmdArray;
        String[] programNameArray = {processorModel.getProgramName()};
        String[] cmdArrayForParams;

        ParFileManager parFileManager = new ParFileManager(processorModel);

        if (processorModel.acceptsParFile()) {
            cmdArrayForParams = parFileManager.getCmdArrayWithParFile();

        } else {
            cmdArrayForParams = getCommandArrayParam(processorModel.getParamList());
        }

        commandArraySuffix = processorModel.getCmdArraySuffix();
        //The final command array is the concatination of commandArrayPrefix, cmdArrayForParams, and commandArraySuffix
        //TODO: for ocssw_install commandArrayPrefix has the program name with the file path, so it can't include programNameArray again
        if (!processorModel.getProgramName().equals(OCSSW_INSTALLER_PROGRAM_NAME)) {
            cmdArray = SeadasArrayUtils.concatAll(commandArrayPrefix, programNameArray, cmdArrayForParams, commandArraySuffix);
        } else {
            cmdArray = SeadasArrayUtils.concatAll(commandArrayPrefix, cmdArrayForParams, commandArraySuffix);
        }

        // get rid of the null strings
        ArrayList<String> cmdList = new ArrayList<String>();
        for (String s : cmdArray) {
            if (s != null) {
                cmdList.add(s);
            }
        }
        cmdArray = cmdList.toArray(new String[cmdList.size()]);

        return cmdArray;
    }

    @Override
    public Process execute(String[] commandArray) {


        StringBuilder sb = new StringBuilder();
        for (String item : commandArray) {
            sb.append(item + " ");
        }

        debug("command array content: " + sb.toString());

        ProcessBuilder processBuilder = new ProcessBuilder(commandArray);


        Map<String, String> env = processBuilder.environment();

        if (ifileDir != null) {
            env.put("PWD", ifileDir);
            processBuilder.directory(new File(ifileDir));
        }

        process = null;
        try {
            process = processBuilder.start();
            if (process != null) {
                debug("Running the program " + commandArray.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return process;
    }

    @Override
    public Process execute(String programName, String[] commandArrayParams) {

        String[] programNameArray = {programName};
        commandArray = SeadasArrayUtils.concatAll(commandArrayPrefix, programNameArray, commandArrayParams, commandArraySuffix);
        return execute(commandArray);
    }

    @Override
    public String executeUpdateLuts(ProcessorModel processorModel) {
        String[] programNameArray = {programName};
        String[] commandArrayParams = getCommandArrayParam(processorModel.getParamList());
        Process process = null;
        for (String param : commandArrayParams) {
            commandArray = SeadasArrayUtils.concatAll(commandArrayPrefix, programNameArray, new String[]{param}, commandArraySuffix);
            process = execute(commandArray);
        }
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return process.exitValue() == 0 ? "Update Luts successful." : "Update Luts failed.";
    }

    @Override
    public void getOutputFiles(ProcessorModel processorModel) {
        //todo implement this method.
    }

    @Override
    public boolean getIntermediateOutputFiles(ProcessorModel processorModel) {
        return true;
    }

    @Override
    public void findFileInfo(String fileName, FileInfoFinder fileInfoFinder) {

        String[] fileTypeCommandArrayParams = {OBPG_FILE_TYPE_PROGRAM_NAME, fileName};

        process = execute((String[]) ArrayUtils.addAll(commandArrayPrefix, fileTypeCommandArrayParams));

        try {

            BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));

            File f = new File(fileName);
            String line = stdInput.readLine();
            while (line != null) {
                if (line.startsWith(f.getName())) {
                    String splitLine[] = line.split(":");
                    if (splitLine.length == 3) {
                        String missionName = splitLine[1].toString().trim();
                        String fileType = splitLine[2].toString().trim();

                        if (fileType.length() > 0) {
                            fileInfoFinder.setFileType(fileType);
                        }

                        if (missionName.length() > 0) {
                            fileInfoFinder.setMissionName(missionName);
                            setMissionName(missionName);
                        }
                        break;
                    }
                }
                line = stdInput.readLine();
            } // while lines
        } catch (IOException ioe) {

            Dialogs.showError(ioe.getMessage());
        }
    }

    @Override
    public String getOfileDir() {
        return ofileDir;
    }

    @Override
    public String getOfileName(String ifileName) {

        if (isOfileNameFound() && this.ifileName.equals(ifileName)) {
            return ofileName;
        }

        if (ifileName == null || programName == null) {
            return null;
        }
        if (programName.equals("l3bindump")) {
            return ifileName + ".xml";
        }
        String[] commandArrayParams = {GET_OUTPUT_NAME_PROGRAM_NAME, ifileName, programName};
        ofileName = findOfileName(ifileName, SeadasArrayUtils.concat(commandArrayPrefix, commandArrayParams));
        setOfileNameFound(true);
        return ofileName;
    }





    @Override
    public String getOfileName(String ifileName, String programName) {
        if(programName.equals("l1bgen_generic")){
            programName = "l1bgen";
        }
        String[] commandArrayParams = {GET_OUTPUT_NAME_PROGRAM_NAME, ifileName, programName};
        ofileName = findOfileName(ifileName, SeadasArrayUtils.concat(commandArrayPrefix, commandArrayParams));
        return ofileName;
    }

    private void addSuites(ArrayList<String> suites, File dir, String prefix) {
        if (dir != null && dir.isDirectory()) {
            for (File file : dir.listFiles()) {
                String filename = file.getName();

                if (filename.startsWith(prefix) && filename.endsWith(".par")) {
                    String suiteName = filename.substring(prefix.length(), filename.length() - 4);
                    if (!suites.contains(suiteName)) {
                        suites.add(suiteName);
                    }
                }
            }

        }
    }

    public String[] getMissionSuites(String missionName, String programName) {
        ArrayList<String> suitesArrayList = new ArrayList<String>();
        MissionInfo missionInfo = new MissionInfo(missionName);
        String prefix = getDefaultsFilePrefix(programName);

        // first look in the common directory
        File dir = new File(ocsswInfo.getOcsswDataDirPath(), "common");
        addSuites(suitesArrayList, dir, prefix);

        // look in sensor dir
        addSuites(suitesArrayList, missionInfo.getDirectory(), prefix);

        // look in subsensor dir
        addSuites(suitesArrayList, missionInfo.getSubsensorDirectory(), prefix);

        if (suitesArrayList.size() > 0) {

            final String[] suitesArray = new String[suitesArrayList.size()];

            int i = 0;
            for (String suite : suitesArrayList) {
                suitesArray[i] = suite;
                i++;
            }

            java.util.Arrays.sort(suitesArray);

            return suitesArray;

        } else {
            return null;
        }
    }

    public String getDefaultsFilePrefix(String programName) {

        defaultsFilePrefix = DEFAULTS_FILE_PREFIX;

        if (programName.equals(L3GEN_PROGRAM_NAME)) {
            defaultsFilePrefix = L3GEN_DEFAULTS_FILE_PREFIX;
        } else if (programName.equals(AQUARIUS_PROGRAM_NAME)) {
            defaultsFilePrefix = AQUARIUS_DEFAULTS_FILE_PREFIX;
        }
        return defaultsFilePrefix;
    }

    @Override
    public String getOfileName(String ifileName, String[] options) {
        if (ifileName == null || programName == null) {
            return null;
        }
        if (programName.equals("l3bindump")) {
            return ifileName + ".xml";
        }

        String[] commandArrayParams = {GET_OUTPUT_NAME_PROGRAM_NAME, ifileName, programName};

        return findOfileName(ifileName, SeadasArrayUtils.concatAll(commandArrayPrefix, commandArrayParams, options));
    }

    @Override
    public String getOfileName(String ifileName, String programName, String suiteValue) {
        this.programName = programName;
        String[] additionalOptions = {"--suite=" + suiteValue};
        return getOfileName(ifileName, additionalOptions);
    }


    private String findOfileName(String ifileName, String[] commandArray) {
        setIfileName(ifileName);
        process = execute(commandArray);

        if (process == null) {
            return null;
        }

        //wait for process to exit
        try {
            Field field = process.getClass().getDeclaredField("hasExited");
            field.setAccessible(true);
            while (!(Boolean) field.get(process)) {
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }

        int exitCode = process.exitValue();
        InputStream is;
        if (exitCode == 0) {
            is = process.getInputStream();
        } else {
            is = process.getErrorStream();
        }


        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);

        try {

            if (exitCode == 0) {
                String line = br.readLine();
                while (line != null) {
                    if (line.startsWith(GET_OUTPUT_NAME_TOKEN)) {
                        return (line.substring(GET_OUTPUT_NAME_TOKEN.length())).trim();
                    }
                    line = br.readLine();
                }
            } else {
                String line = br.readLine();
                while (line != null) {
                    debug("error stream: " + line);
                    line = br.readLine();
                }
                Debug.trace("Failed exit code on program '" + GET_OUTPUT_NAME_PROGRAM_NAME + "'");
            }
        } catch (IOException ioe) {
            Dialogs.showError(ioe.getMessage());
        }
        return null;
    }

    public void setCommandArrayPrefix() {
        if (programName.equals(OCSSW_INSTALLER_PROGRAM_NAME) ) {  //&& !isOCSSWExist()
            commandArrayPrefix = new String[2];
            commandArrayPrefix[0] = TMP_OCSSW_BOOTSTRAP;
            commandArrayPrefix[1] = TMP_OCSSW_INSTALLER;
        } else {
//        if (programName.equals(ocsswInfo.OCSSW_INSTALLER_PROGRAM_NAME)) {
//            commandArrayPrefix = new String[1];
//            commandArrayPrefix[0] = programName;
//            if (!isOCSSWExist()) {
//                commandArrayPrefix[0] = TMP_OCSSW_INSTALLER_PROGRAM_PATH;
//            } else {
//                commandArrayPrefix[0] = ocsswInfo.getOcsswInstallerScriptPath();
//            }
//        } else {
            commandArrayPrefix = new String[3];
            commandArrayPrefix[0] = ocsswInfo.getOcsswRunnerScriptPath();
            commandArrayPrefix[1] = "--ocsswroot";
            commandArrayPrefix[2] = ocsswInfo.getOcsswRoot();
        }
    }


    private String[] getCommandArrayParam(ParamList paramList) {

        ArrayList<String> commandArrayList = new ArrayList<>();

        Iterator<ParamInfo> itr = paramList.getParamArray().iterator();

        ParamInfo option;
        int optionOrder;
        String optionValue;
        int i = 0;
        while (itr.hasNext()) {
            option = itr.next();
            optionOrder = option.getOrder();
            optionValue = option.getValue();
            if (option.getType() != ParamInfo.Type.HELP) {
                if (option.getUsedAs().equals(ParamInfo.USED_IN_COMMAND_AS_ARGUMENT)) {
                    if (option.getValue() != null && option.getValue().length() > 0) {
                        commandArrayList.add(optionValue);
                    }
                } else if (option.getUsedAs().equals(ParamInfo.USED_IN_COMMAND_AS_OPTION) && !option.getDefaultValue().equals(option.getValue())) {
                    commandArrayList.add(option.getName() + "=" + optionValue);
                } else if (option.getUsedAs().equals(ParamInfo.USED_IN_COMMAND_AS_FLAG) && (option.getValue().equals("true") || option.getValue().equals("1"))) {
                    if (option.getName() != null && option.getName().length() > 0) {
                        commandArrayList.add(option.getName());
                    }
                }
            }
        }
        String[] commandArrayParam = new String[commandArrayList.size()];
        commandArrayParam = commandArrayList.toArray(commandArrayParam);
        return commandArrayParam;
    }

    /**
     * /tmp/install_ocssw --tag initial -i ocssw-new --seadas --modist
     *
     * @return
     */
    @Override
    public void setCommandArraySuffix() {
        String[] cmdArraySuffix = new String[2];
        cmdArraySuffix[0] = "--tag=R2020.1";
        cmdArraySuffix[1] = "--seadas";
    }

    @Override
    public HashMap<String, String> computePixelsFromLonLat(ProcessorModel processorModel) {

        String[] commandArray = getProgramCommandArray(processorModel);
        HashMap<String, String> pixels = new HashMap<String, String>();
        try {

            Process process = execute(commandArray);
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            String[] tmp;
            while ((line = stdInput.readLine()) != null) {
                if (line.indexOf("=") != -1) {
                    tmp = line.split("=");
                    pixels.put(tmp[0], tmp[1]);
                }
            }

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return pixels;
    }


    @Override
    public ArrayList<String> readSensorFileIntoArrayList(File file) {
        String lineData;
        ArrayList<String> fileContents = new ArrayList<String>();
        BufferedReader moFile = null;
        try {
            moFile = new BufferedReader(new FileReader(file));
            while ((lineData = moFile.readLine()) != null) {

                fileContents.add(lineData);
            }
        } catch (IOException e) {
            ;
        } finally {
            try {
                moFile.close();
            } catch (Exception e) {
                //Ignore
            }
        }
        return fileContents;
    }

    public void updateOCSSWTags(){
        Runtime rt = Runtime.getRuntime();
        String[] commands = {TMP_OCSSW_BOOTSTRAP, TMP_OCSSW_INSTALLER, "--list_tags"};
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

}
