package gov.nasa.gsfc.seadas.earthdatacloud.action;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

public class PythonRunner {
    public static void main(String[] args) {
        try {
            // Create a process to run the Python script
            ProcessBuilder pb = new ProcessBuilder("python", "script.py");
            Process process = pb.start();

            // Get the output of the Python script
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            // Wait for the process to finish
            int exitCode = process.waitFor();
            System.out.println("Exited with code: " + exitCode);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

}
