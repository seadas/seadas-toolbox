package gov.nasa.gsfc.seadas.earthdatacloud.data;

import org.json.JSONObject;
import org.json.JSONTokener;

import javax.swing.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

public class MetadataLoaderBackup {
    private static final String JSON_DIR = "seadas-toolbox/seadas-earthdata-cloud-toolbox/src/main/resources/json-files";

    /**
     * Loads all metadata JSON files and returns a map where key = mission name and value = its JSON object.
     */
    public static Map<String, JSONObject> loadAllMetadata() {
        Map<String, JSONObject> metadataMap = new HashMap<>();
        try {
            Path jsonDir = Paths.get(System.getProperty("user.dir"), JSON_DIR);
            if (!Files.exists(jsonDir) || !Files.isDirectory(jsonDir)) {
                JOptionPane.showMessageDialog(null, "Metadata directory not found: " + jsonDir);
                return metadataMap;
            }

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(jsonDir, "*.json")) {
                for (Path path : stream) {
                    if (path.getFileName().toString().equals("mission_date_ranges.json")) continue;

                    String key = path.getFileName().toString().replace(".json", "");
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(new FileInputStream(path.toFile()), StandardCharsets.UTF_8))) {
                        JSONTokener tokener = new JSONTokener(reader);
                        JSONObject json = new JSONObject(tokener);
                        metadataMap.put(key, json);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error loading metadata: " + e.getMessage());
        }
        return metadataMap;
    }

    /**
     * Loads mission date ranges from mission_date_ranges.json.
     * @return Map of satellite/instrument name -> [startDate, endDate]
     */
    public static Map<String, String[]> loadMissionDateRanges() {
        Map<String, String[]> ranges = new HashMap<>();
        try {
            Path path = Paths.get(System.getProperty("user.dir"), JSON_DIR, "mission_date_ranges.json");
            if (!Files.exists(path)) {
                JOptionPane.showMessageDialog(null, "Mission date range file not found: " + path);
                return ranges;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path.toFile()), StandardCharsets.UTF_8))) {
                JSONTokener tokener = new JSONTokener(reader);
                JSONObject json = new JSONObject(tokener);

                for (String key : json.keySet()) {
                    JSONObject range = json.getJSONObject(key);
                    String start = range.optString("start", null);
                    String end = range.optString("end", "present");
                    if (start != null) {
                        ranges.put(key, new String[]{start, end});
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error loading mission date ranges: " + e.getMessage());
        }
        return ranges;
    }
}
