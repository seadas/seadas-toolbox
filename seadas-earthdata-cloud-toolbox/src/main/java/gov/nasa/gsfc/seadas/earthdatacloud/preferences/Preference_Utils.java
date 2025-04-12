package gov.nasa.gsfc.seadas.earthdatacloud.preferences;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.ValidationException;

import javax.swing.*;

public class Preference_Utils {

    public static void authenticatePropertyStringNumber(Property property, double lowerLimit, double upperLimit, String label) {
        if (property != null) {
            String valueStr =  "";
            if (property.getValue() != null) {
                valueStr = ((String) property.getValue()).trim();
            }
            if (valueStr == null) {
                valueStr = "";
            }

            String labelFormatted;
            if (label != null && label.trim().length() > 0) {
                labelFormatted = "'" + label + "' ";
            } else {
                labelFormatted = "Value ";
            }

            String valueSentence = labelFormatted + "= '" + valueStr + "'.  ";


            String minLatStr = property.getValue();
            if (!authenticateStringNumber(minLatStr, lowerLimit, upperLimit)) {
                try {
                    property.setValue("");
                } catch (ValidationException e) {
                }
                JOptionPane.showMessageDialog(null, "WARNING!: " + valueSentence + "Value must be between " + lowerLimit + " and " + upperLimit);
            }
        }
    }


    public static boolean authenticateStringNumber(String strNum, double lowerLimit, double upperLimit) {
        try {
            double d = Double.parseDouble(strNum);
            if (d >= lowerLimit && d <= upperLimit) {
                return true;
            }
        } catch (NumberFormatException nfe) {
        }

        return false;
    }

    public static String authenticatedStringNumber(String strNum) {
        if (isNumeric(strNum)) {
            return strNum;
        } else {
            return "";
        }

    }

    public static boolean isNumeric(String strNum) {
        if (strNum == null) {
            return false;
        }
        try {
            double d = Double.parseDouble(strNum);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

}
