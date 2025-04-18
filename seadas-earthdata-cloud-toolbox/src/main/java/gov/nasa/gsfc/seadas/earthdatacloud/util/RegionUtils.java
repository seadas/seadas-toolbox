package gov.nasa.gsfc.seadas.earthdatacloud.util;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.util.ResourceInstaller;
import org.esa.snap.core.util.SystemUtils;

import java.io.*;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.ArrayList;


public class RegionUtils {


    public static ArrayList<RegionsInfo> getAuxDataRegions(String REGIONS_FILE, boolean replaceExisting) {

        String REGIONS = "regions";

        Path auxdataDirPath = SystemUtils.getAuxDataPath().resolve(REGIONS);
        File regionsAuxDir = auxdataDirPath.toFile();
        regionsAuxDir.mkdirs();

        File regionsAuxDirFile = new File(regionsAuxDir, REGIONS_FILE);

        if (regionsAuxDirFile == null ||  !regionsAuxDirFile.exists()) {
            try {
                Path sourceBasePath = ResourceInstaller.findModuleCodeBasePath(RegionUtils.class);
                Path sourceDirPath = sourceBasePath.resolve("auxdata");

                final ResourceInstaller resourceInstaller = new ResourceInstaller(sourceDirPath, auxdataDirPath, replaceExisting);

                resourceInstaller.install(".*." + REGIONS_FILE, ProgressMonitor.NULL);

            } catch (IOException e) {
                System.out.println("ERROR");

//                    SnapApp.getDefault().handleError("Unable to install " + AUXDATA + "/" + SENSOR_INFO + "/" + USER_PROJECTIONS_XML, e);
            }
        }


        ArrayList<RegionsInfo> regionsInfos = new ArrayList<RegionsInfo>();

        if (regionsAuxDirFile != null && regionsAuxDirFile.exists()) {

            try (BufferedReader br = new BufferedReader(new FileReader(regionsAuxDirFile))) {
                RegionsInfo selectInfo = new RegionsInfo("Select", RegionsInfo.SPECIAL_ENTRY, RegionsInfo.SPECIAL_ENTRY, RegionsInfo.SPECIAL_ENTRY, RegionsInfo.SPECIAL_ENTRY);
                regionsInfos.add(selectInfo);

                String line;
                while ((line = br.readLine()) != null) {

                    line = line.trim();
                    if (line.length() == 0 ||  line.startsWith("#")) {
                        continue;
                    }
                    if (line.startsWith("--") && line.endsWith("--")) {
                        if (line.contains("BLANK")) {
                            RegionsInfo sectionInfo = new RegionsInfo("", RegionsInfo.SPECIAL_ENTRY, RegionsInfo.SPECIAL_ENTRY, RegionsInfo.SPECIAL_ENTRY, RegionsInfo.SPECIAL_ENTRY);
                            regionsInfos.add(sectionInfo);
                        } else {
                            RegionsInfo sectionInfo = new RegionsInfo(line, RegionsInfo.SPECIAL_ENTRY, RegionsInfo.SPECIAL_ENTRY, RegionsInfo.SPECIAL_ENTRY, RegionsInfo.SPECIAL_ENTRY);
                            regionsInfos.add(sectionInfo);
                        }
                    } else {
                        String[] lineSplitOnEqualsSignArray = line.split("=");
                        if (lineSplitOnEqualsSignArray != null && lineSplitOnEqualsSignArray.length == 2) {
                            String name = trimString(lineSplitOnEqualsSignArray[0]);

                            if (name != null || name.length() > 0) {
                                String coordinates = trimString(lineSplitOnEqualsSignArray[1]);

                                String[] coordinatesSplitArray = coordinates.split("\\s+");

                                if (coordinatesSplitArray != null) {
                                    String north = null;
                                    String south = null;
                                    String west = null;
                                    String east = null;

                                    if (coordinatesSplitArray.length == 2) {
                                        String lat = convertLatToDecimal(coordinatesSplitArray[0]);
                                        String lon = convertLonToDecimal(coordinatesSplitArray[1]);

//                                        north = lat;
//                                        south = lat;
//                                        west = lon;
//                                        east = lon;

                                        // todo Danny

                                        north = lat;
                                        south = "";
                                        west = lon;
                                        east =  "";

                                        if (validateCoordinates(north, west)) {
                                            RegionsInfo regionsInfo = new RegionsInfo(name, north, south, west, east);
                                            regionsInfos.add(regionsInfo);
                                        }

                                    } else if (coordinatesSplitArray.length == 4) {
                                        north = convertLatToDecimal(coordinatesSplitArray[0]);
                                        south = convertLatToDecimal(coordinatesSplitArray[1]);
                                        west = convertLonToDecimal(coordinatesSplitArray[2]);
                                        east = convertLonToDecimal(coordinatesSplitArray[3]);

                                        if (validateCoordinates(north, south, west, east)) {
                                            RegionsInfo regionsInfo = new RegionsInfo(name, north, south, west, east);
                                            regionsInfos.add(regionsInfo);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {

            }
        }

        return regionsInfos;
    }






    public static String convertLatToDecimal(String latStr) {
        if (latStr == null) {
            return latStr;
        }
        
        latStr = latStr.trim();

        if (latStr.endsWith("N") || latStr.endsWith("S")) {
            boolean isNegative = false;
            if (latStr.endsWith("S")) {
                isNegative = true;
            }

            String latStrConverted = latStr.substring(0, latStr.length() - 1);
            latStrConverted = convertFromMinutesSecondsToDecimal(latStrConverted);

            if (isNegative) {
                latStrConverted = "-" + latStrConverted;
            }

            return latStrConverted;
            
        } 
        
        return latStr;
    }





    public static String convertLonToDecimal(String lonStr) {
        if (lonStr == null) {
            return lonStr;
        }

        lonStr = lonStr.trim();

        if (lonStr.endsWith("W") || lonStr.endsWith("E")) {
            boolean isNegative = false;
            if (lonStr.endsWith("W")) {
                isNegative = true;
            }

            String lonStrConverted = lonStr.substring(0, lonStr.length() - 1);
            lonStrConverted = convertFromMinutesSecondsToDecimal(lonStrConverted);

            if (isNegative) {
                lonStrConverted = "-" + lonStrConverted;
            }

            return lonStrConverted;

        }

        return lonStr;

    }


    private static String convertFromMinutesSecondsToDecimal(String coord) {

        String convertedCoord = coord;

        if (coord == null) {
            return coord;
        }

        coord = coord.trim();

        String[] coordDegreeSplitArray = coord.split("°");

        if (coordDegreeSplitArray != null && coordDegreeSplitArray.length > 0) {
            String degrees = coordDegreeSplitArray[0];

            if (coordDegreeSplitArray.length == 1) {
                convertedCoord = degrees;
            } else if (coordDegreeSplitArray.length == 2) {
                String minutesSeconds = coordDegreeSplitArray[1];
                String[] coordMinutesSplitArray = minutesSeconds.split("′");

                if (coordMinutesSplitArray != null && coordMinutesSplitArray.length > 0) {
                    String minutes = coordMinutesSplitArray[0];
                    double degreesDouble = convertStringToDouble(degrees, -9999.0);
                    double minutesDouble = convertStringToDouble(minutes, -9999.0);
                    if (degreesDouble != -9999.0 && minutesDouble != -9999.0) {
                        // good
                        if (coordMinutesSplitArray.length == 1) {
                            double convertedValue = degreesDouble + minutesDouble/60.0;
                            DecimalFormat df = new DecimalFormat("###.###");
                            return df.format(convertedValue);
                            // todo combine degrees and minutes

                        } else if (coordMinutesSplitArray.length == 2) {
                            String seconds = coordMinutesSplitArray[1];
                            if (seconds != null && seconds.endsWith("″")) {
                                seconds = seconds.substring(0, seconds.length() - 1);
                                double secondsDouble = convertStringToDouble(seconds, -9999.0);
                                if (secondsDouble != -9999.0) {
                                    double convertedValue = degreesDouble + minutesDouble/60.0 + secondsDouble/(60*60);
                                    DecimalFormat df = new DecimalFormat("###.###");
                                    return df.format(convertedValue);
                                    //  todo combine degrees and minutes and seconds
                                }
                            }
                        }
                    } else {
                        return coord;
                    }
                }
            }
        }

        return convertedCoord;
    }





    private static String convertFromMinutesToDecimal(String coordInput) {
        if (coordInput == null || coordInput.trim().length() == 0) {
            return coordInput;
        }

        coordInput = coordInput.trim();
        if (coordInput.endsWith("'")) {
            String coordOutStr = coordInput.substring(0, coordInput.length() - 1);
            String[] values = coordOutStr.split("\\.");
            if (values.length == 2) {
                String degreesStr = values[0];
                String minutesStr = values[1];

                try {
                    double degrees = Double.parseDouble(degreesStr);
                    double minutes = Double.parseDouble(minutesStr);
                    double decimalValue = minutes/60.0;
                    double value = degrees + decimalValue;

                    DecimalFormat df = new DecimalFormat("###.##");
                    return df.format(value);
                } catch (NullPointerException e) {
                } catch (NumberFormatException e) {
                } catch (ArithmeticException e) {
                }
            }
        }

        return coordInput;
    }



    public static int convertStringToInt(String valueStr, int valueFailInt) {
        double valueDouble = convertStringToDouble(valueStr, (double) valueFailInt);

        if (valueDouble != valueFailInt) {
            double valueFloor = Math.floor(valueDouble);
            if (valueDouble == valueFloor) {
                return (int) valueFloor;
            }
        }

        return valueFailInt;
    }

    public static double convertStringToDouble(String valueStr, double valueFailDouble) {
        if (valueStr == null || valueStr.trim().length() == 0) {
            return valueFailDouble;
        }

        valueStr = valueStr.trim();

        try {
            double valueDouble = Double.parseDouble(valueStr);
            return valueDouble;
        } catch (NullPointerException e) {
        } catch (NumberFormatException e) {
        } catch (ArithmeticException e) {
        }

        return valueFailDouble;
    }

    private static boolean validateCoordinates(String northStr, String westStr) {
        try {
            double north = Double.parseDouble(northStr);
            double west = Double.parseDouble(westStr);
            if (north >= -90 && north <= 90) {
                if (west >= -180 && west <= 180) {
                    return true;
                }
            }
            if (north == RegionsInfo.SPECIAL_ENTRY_DOUBLE && west == RegionsInfo.SPECIAL_ENTRY_DOUBLE) {
                return true;
            }
        } catch (NullPointerException e) {
        } catch (NumberFormatException e) {
        }

        return false;
    }



    private static boolean validateCoordinates(String northStr, String southStr, String westStr, String eastStr) {
        try {
            double north = Double.parseDouble(northStr);
            double south = Double.parseDouble(southStr);
            double west = Double.parseDouble(westStr);
            double east = Double.parseDouble(eastStr);
            if (north >= -90 && north <= 90) {
                if (south >= -90 && south <= 90) {
                    if (west >= -180 && west <= 180) {
                        if (east >= -180 && east <= 180) {
                            return true;
                        }
                    }
                }
            }
            if (north == RegionsInfo.SPECIAL_ENTRY_DOUBLE && south == RegionsInfo.SPECIAL_ENTRY_DOUBLE && west == RegionsInfo.SPECIAL_ENTRY_DOUBLE && east == RegionsInfo.SPECIAL_ENTRY_DOUBLE) {
                return true;
            }
        } catch (NullPointerException e) {
        } catch (NumberFormatException e) {
        }

        return false;
    }


    private static String trimString(String str) {
        if (str != null) {
            return str.trim();
        }
        return "";
    }



}
