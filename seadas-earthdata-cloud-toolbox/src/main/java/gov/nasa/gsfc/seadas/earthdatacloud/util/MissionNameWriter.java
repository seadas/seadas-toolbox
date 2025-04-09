package gov.nasa.gsfc.seadas.earthdatacloud.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.Set;

public class MissionNameWriter {

    public static void writeMissionNames(Set<String> missionKeys, String outputPath) {
        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(new File(outputPath), StandardCharsets.UTF_8))) {
            for (String key : missionKeys) {
                writer.write(key);
                writer.newLine();
            }
            System.out.println("✅ mission_names.txt written with " + missionKeys.size() + " entries.");
        } catch (Exception e) {
            System.err.println("❌ Failed to write mission_names.txt: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

