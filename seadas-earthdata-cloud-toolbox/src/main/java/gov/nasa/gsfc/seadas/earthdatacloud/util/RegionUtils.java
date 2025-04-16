package gov.nasa.gsfc.seadas.earthdatacloud.util;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.util.ResourceInstaller;
import org.esa.snap.core.util.SystemUtils;

import java.io.*;
import java.nio.file.Path;
import java.text.DecimalFormat;
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

                final ResourceInstaller resourceInstaller = new ResourceInstaller(sourceDirPath, auxdataDirPath, false);

                resourceInstaller.install(".*." + REGIONS_FILE, ProgressMonitor.NULL);

            } catch (IOException e) {
                System.out.println("ERROR");

//                    SnapApp.getDefault().handleError("Unable to install " + AUXDATA + "/" + SENSOR_INFO + "/" + USER_PROJECTIONS_XML, e);
            }
        }


        ArrayList<RegionsInfo> regionsInfos = new ArrayList<RegionsInfo>();

        if (regionsAuxDirFile != null) {
            if (regionsAuxDirFile.exists()) {

                try (BufferedReader br = new BufferedReader(new FileReader(regionsAuxDirFile))) {
                    RegionsInfo selectInfo = new RegionsInfo("Select", "-999", "-999", "-999", "-999");
                    regionsInfos.add(selectInfo);

                    String line;
                    while ((line = br.readLine()) != null) {
                        boolean regionFound = false;
                        line = line.trim();
                        if (line.startsWith("--") && line.endsWith("--")) {
                            if (line.contains("BLANK")) {
                                RegionsInfo sectionInfo = new RegionsInfo("", "-999", "-999", "-999", "-999");
                                regionsInfos.add(sectionInfo);
                            } else {
                                RegionsInfo sectionInfo = new RegionsInfo(line, "-999", "-999", "-999", "-999");
                                regionsInfos.add(sectionInfo);
                            }
                        } else {
                            String[] values = line.split("=");
                            if (values != null && values.length == 2) {
                                String name = trimString(values[0]);
                                String coords = trimString(values[1]);

                                if (!regionFound) {
                                    String[] coordinatesArray = coords.split(",");


                                    if (coordinatesArray != null) {
                                        String north = "";
                                        String south = "";
                                        String west = "";
                                        String east = "";

                                        if (coordinatesArray.length == 4) {
                                            north = trimString(coordinatesArray[0]);
                                            south = trimString(coordinatesArray[1]);
                                            west = trimString(coordinatesArray[2]);
                                            east = trimString(coordinatesArray[3]);
                                        } else if (coordinatesArray.length == 2) {
                                            north = trimString(coordinatesArray[0]);
                                            south = trimString(coordinatesArray[0]);
                                            west = trimString(coordinatesArray[1]);
                                            east = trimString(coordinatesArray[1]);
                                        }

                                        if (name.length() > 0 && north.length() > 0 && south.length() > 0 && west.length() > 0 && east.length() > 0) {
                                            north = convertFromMinutesToDecimal(north);
                                            south = convertFromMinutesToDecimal(south);
                                            west = convertFromMinutesToDecimal(west);
                                            east = convertFromMinutesToDecimal(east);

                                            if (validateCoordinates(north, south, west, east)) {
                                                RegionsInfo regionsInfo = new RegionsInfo(name, north, south, west, east);
                                                regionsInfos.add(regionsInfo);
                                                regionFound = true;
                                            }
                                        }
                                    }
                                }


                                if (!regionFound) {

                                    String[] coordinatesSpaceSplitArray = coords.split("\\s+");

                                    if (coordinatesSpaceSplitArray != null) {

                                        String lat = "";
                                        String lon = "";

                                        if (coordinatesSpaceSplitArray.length >= 2) {
                                            lat = trimString(coordinatesSpaceSplitArray[0]);
                                            lon = trimString(coordinatesSpaceSplitArray[1]);
                                            System.out.println(line);
                                            System.out.println("lat=" + lat);
                                            System.out.println("lon=" + lon);

                                            if (name.length() > 0 && lat.length() > 0 && lon.length() > 0) {
                                                lat = convertLatFromMinutesSecondsToDecimal(lat);
                                                lon = convertLonFromMinutesSecondsToDecimal(lon);
                                            }

                                            System.out.println("converted lat=" + lat);
                                            System.out.println("converted lon=" + lon);
                                            String north = lat;
                                            String south = lat;
                                            String west = lon;
                                            String east = lon;

                                            if (validateCoordinates(north, south, west, east)) {
//                                            System.out.println(name);
                                                RegionsInfo regionsInfo = new RegionsInfo(name, north, south, west, east);
                                                regionsInfos.add(regionsInfo);
                                                regionFound = true;
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
        }

        return regionsInfos;

    }




    private static String convertLatFromMinutesSecondsToDecimal(String latStr) {
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





    private static String convertLonFromMinutesSecondsToDecimal(String lonStr) {
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
                    double degreesDouble = convertToDecimal(degrees, -9999.0);
                    double minutesDouble = convertToDecimal(minutes, -9999.0);
                    if (degreesDouble != -9999.0 && minutesDouble != -9999.0) {
                        // good
                        if (coordMinutesSplitArray.length == 1) {
                            double convertedValue = degreesDouble + minutesDouble/60.0;
                            DecimalFormat df = new DecimalFormat("###.##");
                            return df.format(convertedValue);
                            // todo combine degrees and minutes

                        } else if (coordMinutesSplitArray.length == 2) {
                            String seconds = coordMinutesSplitArray[1];
                            if (seconds != null && seconds.endsWith("″")) {
                                seconds = seconds.substring(0, seconds.length() - 1);
                                double secondsDouble = convertToDecimal(seconds, -9999.0);
                                if (secondsDouble != -9999.0) {
                                    double convertedValue = degreesDouble + minutesDouble/60.0 + secondsDouble/(60*60);
                                    DecimalFormat df = new DecimalFormat("###.##");
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



    private static double convertToDecimal(String coordInput, double fail) {
        if (coordInput == null || coordInput.trim().length() == 0) {
            return fail;
        }

        coordInput = coordInput.trim();

        try {
            double coordInputDouble = Double.parseDouble(coordInput);
            return coordInputDouble;
        } catch (NullPointerException e) {
        } catch (NumberFormatException e) {
        } catch (ArithmeticException e) {
        }

        return fail;
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
            if (north == -999 && south == -999 && west == -999 && east == -999) {
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
