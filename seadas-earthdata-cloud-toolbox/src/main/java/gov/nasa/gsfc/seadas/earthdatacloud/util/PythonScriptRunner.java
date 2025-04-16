package gov.nasa.gsfc.seadas.earthdatacloud.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PythonScriptRunner {

    private static final String BASE_SCRIPT_PATH = "seadas-toolbox/seadas-earthdata-cloud-toolbox/src/main/";

    private static final String TOOLBOX_SCRIPT_ROOT = resolveToolboxScriptRoot();
    Path toolboxRoot = resolveToolboxRoot();
    Path scriptPath = toolboxRoot.resolve("src/main/MissionDateRangeFinder.py");
    Path resourceDir = toolboxRoot.resolve("src/main/resources/json-files");

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

    private static Path resolveToolboxRoot() {
        Path current = Paths.get(System.getProperty("user.dir")).toAbsolutePath();

        while (current != null) {
            if (Files.exists(current.resolve("seadas-toolbox/seadas-earthdata-cloud-toolbox"))) {
                return current.resolve("seadas-toolbox/seadas-earthdata-cloud-toolbox");
            }
            current = current.getParent();
        }
        throw new RuntimeException("Could not locate seadas-earthdata-cloud-toolbox directory");
    }
    private static File extractScript(String scriptName) throws IOException {
        // Construct the path to the script within the JAR
        String resourcePath = "scripts/" + scriptName;

        // Load the resource as a stream
        InputStream inputStream = PythonScriptRunner.class.getClassLoader().getResourceAsStream(resourcePath);
        if (inputStream == null) {
            throw new FileNotFoundException("Resource not found: " + resourcePath);
        }

        // Create a temporary file to write the script to
        File tempScript = File.createTempFile(scriptName, null);
        tempScript.deleteOnExit();

        // Copy the contents of the resource to the temporary file
        try (OutputStream outputStream = new FileOutputStream(tempScript)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }

        return tempScript;
    }


    public static void runMetadataScript() {
        runPythonScript("scripts/CMR_script.py");
    }

    public static void runDateRangeScript() {
        runPythonScript("scripts/MissionDateRangeFinder.py");
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
