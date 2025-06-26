package gov.nasa.gsfc.seadas.earthdatacloud.data;

import org.json.JSONObject;
import org.json.JSONTokener;

import javax.swing.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class MetadataLoader {

    static Map<String, String[]> missionDateRanges = Map.of(
            "SeaHawk/HawkEye", new String[]{"2018-12-01", "2023-12-31"},
            "MODISA", new String[]{"2002-07-04", "2024-12-31"},
            "VIIRSN", new String[]{"2011-10-28", "2024-12-31"}
    );
    public static Map<String, JSONObject> loadAllMetadata(String jsonDirPath) {
        Map<String, JSONObject> metadataMap = new TreeMap<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(jsonDirPath), "*.json")) {
            for (Path path : stream) {
                String key = path.getFileName().toString().replace(".json", "");
                if (key.equals("mission_date_ranges")) continue;  // Skip date range file
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(new FileInputStream(path.toFile()), StandardCharsets.UTF_8))) {
                    JSONTokener tokener = new JSONTokener(reader);
                    JSONObject json = new JSONObject(tokener);
                    metadataMap.put(key, json);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error loading metadata: " + e.getMessage());
        }
        return metadataMap;
    }

    public static Map<String, String[]> loadMissionDateRanges(String filePath) {
        Map<String, String[]> missionDateRanges = new HashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filePath), StandardCharsets.UTF_8)) {
            JSONTokener tokener = new JSONTokener(reader);
            JSONObject json = new JSONObject(tokener);
            for (String key : json.keySet()) {
                JSONObject range = json.getJSONObject(key);
                String start = range.optString("start", null);
                String end = range.optString("end", "present");
                missionDateRanges.put(key, new String[]{start, end});
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error loading mission date ranges: " + e.getMessage());
        }
        return missionDateRanges;
    }

    public static Map<String, String[]> loadMissionDateRangesFromFile() {
        missionDateRanges = new HashMap<>();

        Path filePath = Paths.get("seadas-toolbox", "seadas-earthdata-cloud-toolbox",
                "src", "main", "resources", "json-files", "mission_date_ranges.json");

        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            JSONTokener tokener = new JSONTokener(reader);
            JSONObject json = new JSONObject(tokener);

            for (String key : json.keySet()) {
                JSONObject dateRange = json.getJSONObject(key);
                String start = dateRange.optString("start", null);
                String end = dateRange.optString("end", "present");
                missionDateRanges.put(key, new String[]{start, end});
            }
        } catch (IOException e) {
            System.err.println("Failed to read mission date ranges: " + e.getMessage());
        }
        return missionDateRanges;
    }
}

