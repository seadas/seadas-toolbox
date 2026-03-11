package gov.nasa.gsfc.seadas.earthdatacloud.worldview;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WorldviewIntegration {

    // Regex to detect either 'r' (Rectangle/Snapshot) or 'v' (Viewport)
    private static final String COORD_REGEX = "[rv]=([\\d\\.-]+),([\\d\\.-]+),([\\d\\.-]+),([\\d\\.-]+)";

    /**
     * Parses the Worldview URL and returns rounded coordinates.
     * Includes logic to handle the Anti-Meridian (180/-180).
     */
    public static double[] parseAndValidateURL(String url) {
        Pattern pattern = Pattern.compile(COORD_REGEX);
        Matcher matcher = pattern.matcher(url);

        if (matcher.find()) {
            try {
                double minLon = Double.parseDouble(matcher.group(1));
                double minLat = Double.parseDouble(matcher.group(2));
                double maxLon = Double.parseDouble(matcher.group(3));
                double maxLat = Double.parseDouble(matcher.group(4));

                // Anti-Meridian Logic:
                // If minLon > maxLon, the box crosses the 180 line.
                // We normalize these for SeaDAS standards.
                if (minLon > maxLon) {
                    System.out.println("Warning: Selection crosses the Anti-Meridian.");
                }

                return new double[] {
                        round(minLon),
                        round(minLat),
                        round(maxLon),
                        round(maxLat)
                };
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Rounds to 4 decimal places as per SeaDAS standards.
     */
    private static double round(double value) {
        // Using Math.round for a clean double return
        return Math.round(value * 10000.0) / 10000.0;
    }

    /**
     * Generates a Worldview URL starting with a specific ROI.
     * Uses both 'v' (view) and 'r' (rectangle tool) so the red box appears immediately.
     */
    public static String getLaunchURL(double minLon, double minLat, double maxLon, double maxLat) {
        String coords = String.format("%f,%f,%f,%f", minLon, minLat, maxLon, maxLat);
        return "https://worldview.earthdata.nasa.gov/?p=geographic&v=" + coords + "&r=" + coords;
    }
}