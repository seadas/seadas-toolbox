package gov.nasa.gsfc.seadas.earthdatacloud.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

public class PythonScriptRunner {

    private static final String BASE_SCRIPT_PATH = "seadas-toolbox/seadas-earthdata-cloud-toolbox/src/main/";

    private static final String TOOLBOX_SCRIPT_ROOT = resolveToolboxScriptRoot();

    private static String resolveToolboxScriptRoot() {
        // Try to find seadas-toolbox root dynamically
        String baseDir = System.getProperty("user.dir");

        // Climb up to find the expected toolbox folder
        File dir = new File(baseDir);
        while (dir != null) {
            File test = new File(dir, "seadas-toolbox" + File.separator + "seadas-earthdata-cloud-toolbox");
            if (test.exists()) {
                return test.getAbsolutePath() + File.separator + "src" + File.separator + "main";
            }
            dir = dir.getParentFile();
        }

        // Fallback if not found
        System.err.println("❌ Could not locate seadas-toolbox root from: " + baseDir);
        return baseDir;  // default to current dir
    }

//    public static void runAllScripts() {
//        System.out.println("🔄 Running all Python scripts...");
//        runMetadataScript();
//        runDateRangeScript();
//    }
//
//    public static void runDateRangeScript(Set<String> missionKeys) {
//        Path missionListFile = Paths.get("mission_names.txt");
//        try {
//            Files.write(missionListFile, missionKeys, StandardCharsets.UTF_8);
//            runPythonScriptWithArgs(DATE_RANGE_SCRIPT, missionListFile.toAbsolutePath().toString());
//        } catch (IOException e) {
//            System.err.println("❌ Failed to write mission list file: " + e.getMessage());
//            e.printStackTrace();
//        } finally {
//            try {
//                Files.deleteIfExists(missionListFile);
//            } catch (IOException e) {
//                System.err.println("⚠️ Failed to delete temporary mission list file.");
//            }
//        }
//    }

//    private static void runPythonScript(String scriptPath) {
//        runPythonScriptWithArgs(scriptPath);
//    }
//
//    private static void runPythonScriptWithArgs(String... args) {
//        try {
//            ProcessBuilder pb = new ProcessBuilder();
//            pb.command(buildPythonCommand(args));
//            pb.directory(new File(System.getProperty("user.dir")));
//            pb.redirectErrorStream(true);
//
//            Process process = pb.start();
//            try (BufferedReader reader = new BufferedReader(
//                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
//                String line;
//                while ((line = reader.readLine()) != null) {
//                    System.out.println("[PYTHON] " + line);
//                }
//            }
//
//            int exitCode = process.waitFor();
//            if (exitCode != 0) {
//                System.err.println("❌ Python script exited with code: " + exitCode);
//            }
//
//        } catch (IOException | InterruptedException e) {
//            System.err.println("❌ Error running Python script: " + String.join(" ", args));
//            e.printStackTrace();
//        }
//    }

    public static void runMetadataScript() {
        runPythonScript("CMR_script.py");
    }

    public static void runDateRangeScript() {
        runPythonScript("MissionDateRangeFinder.py");
    }

    private static void runPythonScript(String scriptName) {
        try {
            String fullScriptPath = TOOLBOX_SCRIPT_ROOT + File.separator + scriptName;

            ProcessBuilder pb = new ProcessBuilder("python", fullScriptPath);
            pb.directory(new File(TOOLBOX_SCRIPT_ROOT));
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[PYTHON] " + line);
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("❌ Python script exited with code: " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("❌ Error running Python script: " + scriptName);
            e.printStackTrace();
        }
    }
    private static String[] buildPythonCommand(String... scriptArgs) {
        String[] command = new String[scriptArgs.length + 1];
        command[0] = "python";
        System.arraycopy(scriptArgs, 0, command, 1, scriptArgs.length);
        return command;
    }
}
