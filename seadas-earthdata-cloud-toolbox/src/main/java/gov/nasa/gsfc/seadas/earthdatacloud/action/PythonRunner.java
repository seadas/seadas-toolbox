package gov.nasa.gsfc.seadas.earthdatacloud.action;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

public class PythonRunner {
    public static void main(String[] args) {
        try {
            ProcessBuilder pb = new ProcessBuilder("python", "script.py");
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            int exitCode = process.waitFor();
            System.out.println("Exited with code: " + exitCode);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

}
