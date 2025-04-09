package gov.nasa.gsfc.seadas.earthdatacloud.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

public class PythonScriptRunner_old {

    static Path toolboxRoot = resolveToolboxRoot();
    static Path scriptPath = toolboxRoot.resolve("src/main/MissionDateRangeFinder.py");
    public static Path resourceDir = toolboxRoot.resolve("src/main/resources/json-files");
    public static Path resolveToolboxRoot() {
        Path current = Paths.get("").toAbsolutePath(); // Starting from current working dir

        while (current != null) {
            Path candidate = current.resolve("seadas-toolbox/seadas-earthdata-cloud-toolbox");
            Path scriptPath = candidate.resolve("src/main/MissionDateRangeFinder.py");

            if (Files.exists(scriptPath)) {
                return candidate;
            }

            current = current.getParent();  // Move up one level
        }

        throw new RuntimeException("❌ Could not locate toolbox root containing seadas-earthdata-cloud-toolbox");
    }

    public static void runMetadataScript() {
        runPythonScript("seadas-toolbox/seadas-earthdata-cloud-toolbox/src/main/CMR_script.py");
    }

    public static void runDateRangeScript() {
        runPythonScript("seadas-toolbox/seadas-earthdata-cloud-toolbox/src/main/MissionDateRangeFinder.py");
    }

    public static void runAllScripts() {
        System.out.println("Running all Python scripts...");
        runMetadataScript();
        runDateRangeScript();
    }

    private static void runPythonScript(String scriptPath) {
        try {
            ProcessBuilder pb = new ProcessBuilder("python", scriptPath);
            pb.directory(new File(System.getProperty("user.dir")));
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
                System.err.println(" Python script exited with code: " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            System.err.println(" Error running Python script: " + scriptPath);
            e.printStackTrace();
        }
    }

    public static void runMissionDateRangeScript(String missionListPath) {
        try {
            String script = scriptPath.toString();
            System.out.println("toolbox root:" + toolboxRoot.toString());
            System.out.println("script path = " + scriptPath.toString());
            System.out.println("resources root:" + resourceDir.toString());
            ProcessBuilder pb = new ProcessBuilder("python", script, missionListPath);
            pb.directory(new File(String.valueOf(toolboxRoot)));
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[PYTHON] " + line);
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("❌ Python script exited with code " + exitCode);
            }
        } catch (Exception e) {
            System.out.println("script path = " + scriptPath.toString());
            System.err.println("❌ Error running generate_mission_date_ranges.py: " + e.getMessage());
            e.printStackTrace();
        }
    }
    public static void runDateRangeScript(Set<String> missionKeys) {
        try {
            // Write mission names to a temp file
            Path missionListFile = Paths.get("mission_names.txt");
            Files.write(missionListFile, missionKeys);

            // Call Python with the mission list file
            //ProcessBuilder pb = new ProcessBuilder("python", scriptPath, missionNamesPath);
            ProcessBuilder pb = new ProcessBuilder("python",
                    "seadas-toolbox/seadas-earthdata-cloud-toolbox/src/main/MissionDateRangeFinder.py",
                    missionListFile.toAbsolutePath().toString());

            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[PYTHON] " + line);
                }
            }

            process.waitFor();
            Files.deleteIfExists(missionListFile);  // Cleanup
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
