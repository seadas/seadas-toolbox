package gov.nasa.gsfc.seadas.earthdatacloud.ui;

import java.util.regex.*;

public class MetadataProcessor {
    public static void main(String[] args) {
        String[] testShortNames = {
                "PACE_OCI_L2_AOP_NRT",
                "SeaHawk-1_HawkEye_L1",
                "ADEOS-I_OCTS_L3m_CHL",
                "OrbView-2_SeaWiFS_L2_OC",
                "Nimbus-7_CZCS_L0"
        };

        for (String shortName : testShortNames) {
            String[] result = extractMetadata(shortName);
            System.out.println("Short Name: " + shortName);
            System.out.println("  Satellite/Instrument: " + result[0]);
            System.out.println("  Data Level: " + result[1]);
            System.out.println("  Product Name: " + result[2]);
            System.out.println();
        }
    }

    private static String[] extractMetadata(String shortName) {
        // Pattern: (satellite_instrument)_(Lx)_(product)?
        Pattern pattern = Pattern.compile("^(.*?)_(L\\d+[a-zA-Z]*)(?:_(.*))?$");
        Matcher matcher = pattern.matcher(shortName);

        String satelliteInstr = "Unknown";
        String dataLevel = "Unknown";
        String productName = "General"; // Default if no specific product name

        if (matcher.find()) {
            satelliteInstr = matcher.group(1); // Extract Satellite/Instrument
            dataLevel = matcher.group(2); // Extract Data Level
            if (matcher.group(3) != null) {
                productName = matcher.group(3); // Extract Product Name if exists
            }
        }

        return new String[]{satelliteInstr, dataLevel, productName};
    }
}
