package gov.nasa.gsfc.seadas.earthdatacloud.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

public class MissionNameWriter {

//    public static void writeMissionNames(Set<String> missionKeys, String outputPath) {
//        try (BufferedWriter writer = new BufferedWriter(
//                new FileWriter(new File(outputPath), StandardCharsets.UTF_8))) {
//            for (String key : missionKeys) {
//                writer.write(key);
//                writer.newLine();
//            }
//            System.out.println("✅ mission_names.txt written with " + missionKeys.size() + " entries.");
//        } catch (Exception e) {
//            System.err.println("❌ Failed to write mission_names.txt: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }

    public static void writeMissionNames(Set<String> missionNames, String outputPath) {
        try {
            Path filePath = Paths.get(outputPath);
            Files.createDirectories(filePath.getParent());  // Ensure parent dir exists
            Files.write(filePath, missionNames);
            System.out.println("✅ mission_names.txt written with " + missionNames.size() + " entries.");
        } catch (IOException e) {
            System.err.println("❌ Failed to write mission_names.txt: " + outputPath);
            e.printStackTrace();
        }
    }

}

