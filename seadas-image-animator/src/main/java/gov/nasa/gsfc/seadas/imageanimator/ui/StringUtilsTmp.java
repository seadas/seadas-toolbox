package gov.nasa.gsfc.seadas.imageanimator.ui;

import org.esa.snap.core.util.StringUtils;


public class StringUtilsTmp {

    static  public String cleanUpGroupName(String displayName) {
        if (displayName == null) {
            return null;
        }

        if (displayName.contains("#")) {
            final String[] split = StringUtils.split(displayName, new char[]{'#'}, true);
            final String groupName = split[0];
            if (groupName.length() > 0) {
                displayName = groupName;
            }
        } else {
            if (displayName.startsWith("^")) {
                displayName = displayName.substring(0);
            }

            if (displayName.endsWith("$")) {
                displayName = displayName.substring(0, displayName.length() - 1);
            }

            if (displayName.startsWith("*")) {
                displayName = displayName.substring(0);
            }

            if (displayName.endsWith("*")) {
                displayName = displayName.substring(0, displayName.length() - 1);
            }

            if (displayName.endsWith("_")) {
                displayName = displayName.substring(0, displayName.length() - 1);
            }
        }

        return displayName;
    }


}
