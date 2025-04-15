package gov.nasa.gsfc.seadas.earthdatacloud.util;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.util.ResourceInstaller;
import org.esa.snap.core.util.SystemUtils;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;


public class RegionUtils {


    public static ArrayList<RegionsInfo> getAuxDataRegions() {

        String REGIONS = "regions";
        String REGIONS_FILE = "regions.txt";

        Path auxdataDirPath = SystemUtils.getAuxDataPath().resolve(REGIONS);
        File regionsAuxDir = auxdataDirPath.toFile();
        regionsAuxDir.mkdirs();

        File regionsAuxDirFile = new File(regionsAuxDir, REGIONS_FILE);

        if (regionsAuxDirFile == null ||  !regionsAuxDirFile.exists()) {
            try {
                Path sourceBasePath = ResourceInstaller.findModuleCodeBasePath(RegionUtils.class);
                Path sourceDirPath = sourceBasePath.resolve("auxdata");
                System.out.println("sourceDirPath=" + sourceDirPath);

                final ResourceInstaller resourceInstaller = new ResourceInstaller(sourceDirPath, auxdataDirPath, false);

                resourceInstaller.install(".*." + REGIONS_FILE, ProgressMonitor.NULL);

            } catch (IOException e) {
                System.out.println("ERROR");

//                    SnapApp.getDefault().handleError("Unable to install " + AUXDATA + "/" + SENSOR_INFO + "/" + USER_PROJECTIONS_XML, e);
            }
        }


        ArrayList<RegionsInfo> regionsInfos = new ArrayList<RegionsInfo>();

//        RegionsInfo regionsInfo1 = new RegionsInfo("Test", "-1","-1","-1","-1");
//        RegionsInfo regionsInfo2 = new RegionsInfo("Tes2", "-2","-2","-2","-2");
//        regionsInfos.add(regionsInfo1);
//        regionsInfos.add(regionsInfo2);

        boolean entryAdded = false;
        if (regionsAuxDirFile != null) {
            System.out.println("not null");
            if (regionsAuxDirFile.exists()) {
                System.out.println("exists");

                try (BufferedReader br = new BufferedReader(new FileReader(regionsAuxDirFile))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        System.out.println(line);
                        String[] values = line.split("=");
                        if (values != null && values.length == 2) {
                            String name = trimString(values[0]);
                            String coords = values[1];
                            String[] coordinatesArray = coords.split(",");

                            if (coordinatesArray != null && coordinatesArray.length == 4) {
                                String north = trimString(coordinatesArray[0]);
                                String south = trimString(coordinatesArray[1]);
                                String west = trimString(coordinatesArray[2]);
                                String east = trimString(coordinatesArray[3]);

                                if (name.length() > 0 && north.length() > 0 && south.length() > 0 && west.length() > 0 && east.length() > 0) {
                                    RegionsInfo regionsInfo = new RegionsInfo(name, north, south, west, east);
                                    if (!entryAdded) {
                                        RegionsInfo selectInfo = new RegionsInfo("Select", "-999", "-999", "-999", "-999");
                                        regionsInfos.add(selectInfo);
                                        entryAdded = true;
                                    }
                                    regionsInfos.add(regionsInfo);
                                }
                            }
                        }
                    }
                } catch (Exception e) {

                }
            }
        }

        return regionsInfos;

    }


    private static String trimString(String str) {
        if (str != null) {
            return str.trim();
        }
        return "";
    }



}
