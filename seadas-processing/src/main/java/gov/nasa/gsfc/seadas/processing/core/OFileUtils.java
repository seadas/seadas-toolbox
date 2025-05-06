package gov.nasa.gsfc.seadas.processing.core;

import gov.nasa.gsfc.seadas.processing.preferences.OCSSW_L2binController;
import gov.nasa.gsfc.seadas.processing.preferences.OCSSW_L3binController;
import gov.nasa.gsfc.seadas.processing.preferences.OCSSW_L3mapgenController;

import java.io.File;

public class OFileUtils {


    private static final String DELIMITOR_NUMBER = "_";
    private static final String DELIMITOR_STRING = ".";

    public static String getOfileForL3BinWrapper(String ifileName, String ofileNameDefault, String programName, String resolve, String prod, String north, String south, String west, String east) {
        String ifileBaseName = stripFilenameExtension(ifileName);


        String ofileName;

        if (OCSSW_L3binController.OFILE_NAMING_SCHEME_IFILE_REPLACE.equalsIgnoreCase(OCSSW_L3binController.getPreferenceOfileNamingScheme())) {
            String orginalKeyString = OCSSW_L3binController.getPreferenceOfileNamingSchemeIfileOriginal();
            String replacementKeyString = OCSSW_L3binController.getPreferenceOfileNamingSchemeIfileReplace();
            ofileName = getOfileWithIfileReplaceString(ifileName, orginalKeyString, replacementKeyString);
        } else if (OCSSW_L3binController.OFILE_NAMING_SCHEME_OCSSW.equalsIgnoreCase(OCSSW_L3binController.getPreferenceOfileNamingScheme())) {
            ofileName = getOfileFullPathWithIfileDir(ifileName, ofileNameDefault);
        } else if (OCSSW_L3binController.OFILE_NAMING_SCHEME_OCSSW_SHORT.equalsIgnoreCase(OCSSW_L3binController.getPreferenceOfileNamingScheme())) {
            ofileName = getOfileFullPathWithIfileDir(ifileName, ofileNameDefault);
            ofileName = ofileName.replace(".DAY.", ".");
            ofileName = ofileName.replace(".DAY", "");
            ofileName = ofileName.replace(".8D.", ".");
            ofileName = ofileName.replace(".8D", "");
            ofileName = ofileName.replace(".MO.", ".");
            ofileName = ofileName.replace(".MO", "");
            ofileName = ofileName.replace(".YR.", ".");
            ofileName = ofileName.replace(".YR", "");
            ofileName = ofileName.replace(".CU.", ".");
            ofileName = ofileName.replace(".CU", "");

        } else if (OCSSW_L3binController.OFILE_NAMING_SCHEME_SIMPLE.equalsIgnoreCase(OCSSW_L2binController.getPreferenceOfileNamingScheme())) {
            ofileName = getOfileSimple(ifileName);
        } else {
            ofileName = getOfileFullPathWithIfileDir(ifileName, ofileNameDefault);
        }


        // if it fails gives it a simple name (for example 'output')
        if (ofileName == null || ofileName.length() == 0) {
            ofileName = getOfileSimple(ifileName);
        }


        String foundExtension = "";
        if (ofileName.endsWith(".sub")) {
            foundExtension = ".sub";
            ofileName = stripFilenameExtractExtension(ofileName, foundExtension);
        }
        if (ofileName.endsWith(".extract")) {
            foundExtension = ".extract";
            ofileName = stripFilenameExtractExtension(ofileName, foundExtension);
        }
        if (ofileName.endsWith(".subset")) {
            foundExtension = ".subset";
            ofileName = stripFilenameExtractExtension(ofileName, foundExtension);
        }


        ofileName += getOfileForL3BinAddOns(resolve, prod, north, south, west, east);


        if (ofileName.contains(" ")) {
            ofileName = ofileName.replace(" ", "");
        }
        ofileName = trimStringChars(ofileName, ".", false, true, true);
        ofileName = trimStringChars(ofileName, "_", false, true, true);
        ofileName = trimStringChars(ofileName, "-", false, true, true);

        ofileName += foundExtension;


        if (ofileName.equalsIgnoreCase(ifileBaseName)) {
            ofileName = ofileName + "_out";
        }

        ofileName += ".nc";

        return ofileName;
    }


    public static  String getOfileForL2BinWrapper(String ifileName, String ofileNameDefault, String programName, String resolution, String l3bprod, String suite, String prodtype, String north, String south, String west, String east) {
        String ifileBaseName = stripFilenameExtension(ifileName);


        String ofileName;

        if (OCSSW_L2binController.OFILE_NAMING_SCHEME_IFILE_REPLACE.equalsIgnoreCase(OCSSW_L2binController.getPreferenceOfileNamingScheme())) {
            String orginalKeyString = OCSSW_L2binController.getPreferenceOfileNamingSchemeIfileOriginal();
            String replacementKeyString = OCSSW_L2binController.getPreferenceOfileNamingSchemeIfileReplace();
            ofileName = getOfileWithIfileReplaceString(ifileName, orginalKeyString, replacementKeyString);
        } else if (OCSSW_L2binController.OFILE_NAMING_SCHEME_OCSSW.equalsIgnoreCase(OCSSW_L2binController.getPreferenceOfileNamingScheme())) {
            ofileName = getOfileFullPathWithIfileDir(ifileName, ofileNameDefault);
        } else if (OCSSW_L2binController.OFILE_NAMING_SCHEME_OCSSW_SHORT.equalsIgnoreCase(OCSSW_L2binController.getPreferenceOfileNamingScheme())) {
            ofileName = getOfileFullPathWithIfileDir(ifileName, ofileNameDefault);
            ofileName = ofileName.replace(".DAY.", ".");
            ofileName = ofileName.replace(".DAY", "");
            ofileName = ofileName.replace(".8D.", ".");
            ofileName = ofileName.replace(".8D", "");
            ofileName = ofileName.replace(".MO.", ".");
            ofileName = ofileName.replace(".MO", "");
            ofileName = ofileName.replace(".YR.", ".");
            ofileName = ofileName.replace(".YR", "");
            ofileName = ofileName.replace(".CU.", ".");
            ofileName = ofileName.replace(".CU", "");

        } else if (OCSSW_L2binController.OFILE_NAMING_SCHEME_SIMPLE.equalsIgnoreCase(OCSSW_L2binController.getPreferenceOfileNamingScheme())) {
            ofileName = getOfileSimple(ifileName);
        } else {
            ofileName = getOfileFullPathWithIfileDir(ifileName, ofileNameDefault);
        }


        // if it fails gives it a simple name (for example 'output')
        if (ofileName == null || ofileName.length() == 0) {
            ofileName = getOfileSimple(ifileName);
        }


        String foundExtension = "";
        if (ofileName.endsWith(".sub")) {
            foundExtension = ".sub";
            ofileName = stripFilenameExtractExtension(ofileName, foundExtension);
        }
        if (ofileName.endsWith(".extract")) {
            foundExtension = ".extract";
            ofileName = stripFilenameExtractExtension(ofileName, foundExtension);
        }
        if (ofileName.endsWith(".subset")) {
            foundExtension = ".subset";
            ofileName = stripFilenameExtractExtension(ofileName, foundExtension);
        }


        ofileName += getOfileForL2BinAddOns(resolution, l3bprod, suite, prodtype, north, south, west, east);


        if (ofileName.contains(" ")) {
            ofileName = ofileName.replace(" ", "");
        }
        ofileName = trimStringChars(ofileName, ".", false, true, true);
        ofileName = trimStringChars(ofileName, "_", false, true, true);
        ofileName = trimStringChars(ofileName, "-", false, true, true);


        ofileName += foundExtension;


        if (ofileName.equalsIgnoreCase(ifileBaseName)) {
            ofileName = ofileName + "_out";
        }


        ofileName += ".nc";

        return ofileName;
    }


    public  static String getOfileForL3MapGenWrapper(String ifileName, String ofileNameDefault, String programName, String resolution, String oformat, String product, String projection, String interp, String north, String south, String west, String east) {
        String ifileBaseName = stripFilenameExtension(ifileName);


        String ofileName;
        if (OCSSW_L3mapgenController.OFILE_NAMING_SCHEME_IFILE_REPLACE.equalsIgnoreCase(OCSSW_L3mapgenController.getPreferenceOfileNamingScheme())) {
            String orginalKeyString = OCSSW_L3mapgenController.getPreferenceOfileNamingSchemeIfileOriginal();
            String replacementKeyString = OCSSW_L3mapgenController.getPreferenceOfileNamingSchemeIfileReplace();
            ofileName = getOfileWithIfileReplaceString(ifileName, orginalKeyString, replacementKeyString);
        } else if (OCSSW_L3mapgenController.OFILE_NAMING_SCHEME_OCSSW.equalsIgnoreCase(OCSSW_L3mapgenController.getPreferenceOfileNamingScheme())) {
            ofileName = getOfileFullPathWithIfileDir(ifileName, ofileNameDefault);
        } else if (OCSSW_L3mapgenController.OFILE_NAMING_SCHEME_OCSSW_SHORT.equalsIgnoreCase(OCSSW_L3mapgenController.getPreferenceOfileNamingScheme())) {
            ofileName = getOfileFullPathWithIfileDir(ifileName, ofileNameDefault);
            ofileName = ofileName.replace(".DAY.", ".");
            ofileName = ofileName.replace(".DAY", "");
            ofileName = ofileName.replace(".8D.", ".");
            ofileName = ofileName.replace(".8D", "");
            ofileName = ofileName.replace(".MO.", ".");
            ofileName = ofileName.replace(".MO", "");
            ofileName = ofileName.replace(".YR.", ".");
            ofileName = ofileName.replace(".YR", "");
            ofileName = ofileName.replace(".CU.", ".");
            ofileName = ofileName.replace(".CU", "");

        } else if (OCSSW_L3mapgenController.OFILE_NAMING_SCHEME_SIMPLE.equalsIgnoreCase(OCSSW_L3mapgenController.getPreferenceOfileNamingScheme())) {
            ofileName = getOfileSimple(ifileName);
        } else {
            String orginalKeyString = OCSSW_L3mapgenController.getPreferenceOfileNamingSchemeIfileOriginal();
            String replacementKeyString = OCSSW_L3mapgenController.getPreferenceOfileNamingSchemeIfileReplace();
            ofileName = getOfileWithIfileReplaceString(ifileName, orginalKeyString, replacementKeyString);
        }

        // if it fails gives it a simple name (for example 'output')
        if (ofileName == null || ofileName.length() == 0) {
            ofileName = getOfileSimple(ifileName);
        }


        String foundExtension = "";
        if (ofileName.endsWith(".sub")) {
            foundExtension = ".sub";
            ofileName = stripFilenameExtractExtension(ofileName, foundExtension);
        }
        if (ofileName.endsWith(".extract")) {
            foundExtension = ".extract";
            ofileName = stripFilenameExtractExtension(ofileName, foundExtension);
        }
        if (ofileName.endsWith(".subset")) {
            foundExtension = ".subset";
            ofileName = stripFilenameExtractExtension(ofileName, foundExtension);
        }


        ofileName += getOfileForL3MapGenAddOns(resolution, product, projection, interp, north, south, west, east);


        if (OCSSW_L3mapgenController.OFILE_NAMING_SCHEME_IFILE_PLUS_SUFFIX.equalsIgnoreCase(OCSSW_L3mapgenController.getPreferenceOfileNamingScheme())) {
            String suffix = "_suffix";
            ofileName += suffix;
        }


        if (ofileName.contains(" ")) {
            ofileName = ofileName.replace(" ", "");
        }
        ofileName = trimStringChars(ofileName, ".", false, true, true);
        ofileName = trimStringChars(ofileName, "_", false, true, true);
        ofileName = trimStringChars(ofileName, "-", false, true, true);


        ofileName += foundExtension;


        if (ofileName.equalsIgnoreCase(ifileBaseName)) {
            ofileName = ofileName + "_out";
        }

        ofileName = getOfileForL3MapGenAddExtension(ofileName, oformat);

        return ofileName;
    }






    private static String getOfileSimple(String ifilename) {

        String ofilename = "output";

        if (ifilename != null && ifilename.trim().length() > 0) {
            File file = new File(ifilename);
            if (file != null) {
                File parentFile = file.getParentFile();
                if (parentFile != null) {
                    String parentPath = parentFile.getAbsolutePath();
                    File file2 = new File(parentPath, ofilename);
                    if (file2 != null) {
                        ofilename = file2.getAbsolutePath();
                    }
                }
            }
        }

        return ofilename;
    }


    private static String getOfileWithIfileReplaceString(String ifilename, String orginalKeyString, String replacementKeyString) {

        String parentPath = null;
        String ifileBasename = ifilename;
        boolean pathRemoved = false;
        String ofileBaseName;

        File ifileFile = new File(ifilename);

        if (ifileFile != null) {
            if (ifileFile.getParentFile() != null) {
                parentPath = ifileFile.getParentFile().getAbsolutePath();
            }
            ifileBasename = ifileFile.getName();
            pathRemoved = true;
        }

        ifileBasename = stripFilenameExtension(ifileBasename);
//
//        String orginalKeyString = OCSSW_L3mapgenController.getPreferenceOfileNamingSchemeIfileOriginal();
//        String replacementKeyString = OCSSW_L3mapgenController.getPreferenceOfileNamingSchemeIfileReplace();
        if (replacementKeyString == null) {
            replacementKeyString = "";
        }

        if (orginalKeyString != null && orginalKeyString.length() > 0) {
            ofileBaseName = ifileBasename.replace(orginalKeyString, replacementKeyString);
        } else {
            ofileBaseName = ifileBasename;
        }

        String ofilename = null;

        if (pathRemoved && parentPath != null) {
            File oFile = new File(parentPath, ofileBaseName);
            if (oFile != null) {
                ofilename = oFile.getAbsolutePath();
            }
        }

        if (ofilename == null) {
            ofilename = ofileBaseName;
        }

        return ofilename;
    }




    private static String getOfileForL2BinAddOns(String resolution, String l3bprod, String suite, String prodtype, String north, String south, String west, String east) {

        if (OCSSW_L2binController.OFILE_NAMING_SCHEME_SUFFIX_NONE.equals(OCSSW_L2binController.getPreferenceOfileNamingSchemeSuffixOptions())) {
            return "";
        }

        String keyString;
        if (OCSSW_L2binController.OFILE_NAMING_SCHEME_SUFFIX1.equals(OCSSW_L2binController.getPreferenceOfileNamingSchemeSuffixOptions())) {
            keyString = OCSSW_L2binController.getPreferenceOfileNamingSchemeSuffix1();
        } else if (OCSSW_L2binController.OFILE_NAMING_SCHEME_SUFFIX2.equals(OCSSW_L2binController.getPreferenceOfileNamingSchemeSuffixOptions())) {
            keyString = OCSSW_L2binController.getPreferenceOfileNamingSchemeSuffix2();
        } else if (OCSSW_L2binController.OFILE_NAMING_SCHEME_SUFFIX_DEFAULT.equals(OCSSW_L2binController.getPreferenceOfileNamingSchemeSuffixOptions())) {
            keyString = OCSSW_L2binController.OFILE_NAMING_SCHEME_SUFFIX_DEFAULT;
        } else if (OCSSW_L2binController.OFILE_NAMING_SCHEME_SUFFIX_DEFAULT2.equals(OCSSW_L2binController.getPreferenceOfileNamingSchemeSuffixOptions())) {
            keyString = OCSSW_L2binController.OFILE_NAMING_SCHEME_SUFFIX_DEFAULT2;
        } else if (OCSSW_L2binController.OFILE_NAMING_SCHEME_SUFFIX_DEFAULT3.equals(OCSSW_L2binController.getPreferenceOfileNamingSchemeSuffixOptions())) {
            keyString = OCSSW_L2binController.OFILE_NAMING_SCHEME_SUFFIX_DEFAULT3;
        } else if (OCSSW_L2binController.OFILE_NAMING_SCHEME_SUFFIX_DEFAULT4.equals(OCSSW_L2binController.getPreferenceOfileNamingSchemeSuffixOptions())) {
            keyString = OCSSW_L2binController.OFILE_NAMING_SCHEME_SUFFIX_DEFAULT4;
        } else {
            keyString = OCSSW_L2binController.OFILE_NAMING_SCHEME_SUFFIX_DEFAULT;
        }


        if (keyString == null || keyString.trim().length() == 0) {
            return "";
        }


        if (checkForVariantMatch(keyString, "l3bprod") || checkForVariantMatch(keyString, "L3BPROD") ||
                checkForVariantMatch(keyString, "product") || checkForVariantMatch(keyString, "PRODUCT")
        ) {
            String productSingle = "";
            if (l3bprod != null && l3bprod.trim().length() > 0) {
                String[] productsArray = l3bprod.split("[,\\s]");
                if (productsArray != null && productsArray.length == 1) {
                    if (productsArray[0] != null) {
                        productSingle = productsArray[0].trim();
                        if (productSingle.length() > 0) {
                            productSingle = productsArray[0];
                        }
                    }
                }
            }
            keyString = replaceAnyKeyStringVariant(keyString, "l3bprod", productSingle, DELIMITOR_STRING);
            keyString = replaceAnyKeyStringVariant(keyString, "product", productSingle, DELIMITOR_STRING);
            keyString = replaceAnyKeyStringVariant(keyString, "L3BPROD", productSingle.toUpperCase(), DELIMITOR_STRING);
            keyString = replaceAnyKeyStringVariant(keyString, "PRODUCT", productSingle.toUpperCase(), DELIMITOR_STRING);
        }


        if (checkForVariantMatch(keyString, "l3bprod_list") || checkForVariantMatch(keyString, "L3BPROD_LIST") ||
                checkForVariantMatch(keyString, "product_list") || checkForVariantMatch(keyString, "PRODUCT_LIST")
        ) {
            String productList = "";
            if (l3bprod != null && l3bprod.trim().length() > 0) {
                String[] productsArray = l3bprod.split("[,\\s]");

                for (String currProduct : productsArray) {
                    if (keyString.toLowerCase().contains("[.l3bprod_list]") || keyString.toLowerCase().contains("[.product_list]")) {
                        productList += "." + currProduct;
                    } else if (keyString.toLowerCase().contains("[-l3bprod_list]") || keyString.toLowerCase().contains("[-product_list]")) {
                        productList += "-" + currProduct;
                    } else {
                        productList += "_" + currProduct;
                    }
                }
            }
            keyString = replaceAnyKeyStringVariant(keyString, "l3bprod_list", productList, DELIMITOR_STRING);
            keyString = replaceAnyKeyStringVariant(keyString, "product_list", productList, DELIMITOR_STRING);
            keyString = replaceAnyKeyStringVariant(keyString, "L3BPROD_LIST", productList.toUpperCase(), DELIMITOR_STRING);
            keyString = replaceAnyKeyStringVariant(keyString, "PRODUCT_LIST", productList.toUpperCase(), DELIMITOR_STRING);
        }


        keyString = getOfileAddOnResolutionL2BinL3Bin(resolution, keyString);


        if (checkForVariantMatch(keyString, "prodtype") || checkForVariantMatch(keyString, "PRODTYPE")) {
            if (prodtype == null) {
                prodtype = "";
            }
            keyString = replaceAnyKeyStringVariant(keyString, "prodtype", prodtype, DELIMITOR_STRING);
            keyString = replaceAnyKeyStringVariant(keyString, "PRODTYPE", prodtype.toUpperCase(), DELIMITOR_STRING);
        }


        if (checkForVariantMatch(keyString, "suite") || checkForVariantMatch(keyString, "SUITE")) {
            if (suite == null) {
                suite = "";
            }
            keyString = replaceAnyKeyStringVariant(keyString, "suite", suite, DELIMITOR_STRING);
            keyString = replaceAnyKeyStringVariant(keyString, "SUITE", suite.toUpperCase(), DELIMITOR_STRING);
        }

        keyString = keystringReplaceNSWE(keyString, north, south, west, east);


        return keyString;
    }




    private static String getOfileForL3BinAddOns(String resolution, String prod, String north, String south, String west, String east) {

        if (OCSSW_L3binController.OFILE_NAMING_SCHEME_SUFFIX_NONE.equals(OCSSW_L3binController.getPreferenceOfileNamingSchemeSuffixOptions())) {
            return "";
        }

        String keyString;
        if (OCSSW_L3binController.OFILE_NAMING_SCHEME_SUFFIX1.equals(OCSSW_L3binController.getPreferenceOfileNamingSchemeSuffixOptions())) {
            keyString = OCSSW_L3binController.getPreferenceOfileNamingSchemeSuffix1();
        } else if (OCSSW_L3binController.OFILE_NAMING_SCHEME_SUFFIX2.equals(OCSSW_L3binController.getPreferenceOfileNamingSchemeSuffixOptions())) {
            keyString = OCSSW_L3binController.getPreferenceOfileNamingSchemeSuffix2();
        } else if (OCSSW_L3binController.OFILE_NAMING_SCHEME_SUFFIX_DEFAULT.equals(OCSSW_L3binController.getPreferenceOfileNamingSchemeSuffixOptions())) {
            keyString = OCSSW_L3binController.OFILE_NAMING_SCHEME_SUFFIX_DEFAULT;
        } else if (OCSSW_L3binController.OFILE_NAMING_SCHEME_SUFFIX_DEFAULT2.equals(OCSSW_L3binController.getPreferenceOfileNamingSchemeSuffixOptions())) {
            keyString = OCSSW_L3binController.OFILE_NAMING_SCHEME_SUFFIX_DEFAULT2;
//        } else if (OCSSW_L3binController.OFILE_NAMING_SCHEME_SUFFIX_DEFAULT3.equals(OCSSW_L3binController.getPreferenceOfileNamingSchemeSuffixOptions())) {
//            keyString = OCSSW_L3binController.OFILE_NAMING_SCHEME_SUFFIX_DEFAULT3;
//        } else if (OCSSW_L3binController.OFILE_NAMING_SCHEME_SUFFIX_DEFAULT4.equals(OCSSW_L3binController.getPreferenceOfileNamingSchemeSuffixOptions())) {
//            keyString = OCSSW_L3binController.OFILE_NAMING_SCHEME_SUFFIX_DEFAULT4;
        } else {
            keyString = OCSSW_L3binController.OFILE_NAMING_SCHEME_SUFFIX_DEFAULT;
        }


        if (keyString == null || keyString.trim().length() == 0) {
            return "";
        }


        if (checkForVariantMatch(keyString, "prod") || checkForVariantMatch(keyString, "PROD") ||
                checkForVariantMatch(keyString, "product") || checkForVariantMatch(keyString, "PRODUCT")
        ) {
            String productSingle = "";
            if (prod != null && prod.trim().length() > 0) {
                String[] productsArray = prod.split("[,\\s]");
                if (productsArray != null && productsArray.length == 1) {
                    if (productsArray[0] != null) {
                        productSingle = productsArray[0].trim();
                        if (productSingle.length() > 0) {
                            productSingle = productsArray[0];
                        }
                    }
                }
            }
            keyString = replaceAnyKeyStringVariant(keyString, "prod", productSingle, DELIMITOR_STRING);
            keyString = replaceAnyKeyStringVariant(keyString, "product", productSingle, DELIMITOR_STRING);
            keyString = replaceAnyKeyStringVariant(keyString, "PROD", productSingle.toUpperCase(), DELIMITOR_STRING);
            keyString = replaceAnyKeyStringVariant(keyString, "PRODUCT", productSingle.toUpperCase(), DELIMITOR_STRING);
        }


        if (checkForVariantMatch(keyString, "prod_list") || checkForVariantMatch(keyString, "PROD_LIST") ||
                checkForVariantMatch(keyString, "product_list") || checkForVariantMatch(keyString, "PRODUCT_LIST")
        ) {
            String productList = "";
            if (prod != null && prod.trim().length() > 0) {
                String[] productsArray = prod.split("[,\\s]");

                for (String currProduct : productsArray) {
                    if (keyString.toLowerCase().contains("[.prod_list]") || keyString.toLowerCase().contains("[.product_list]")) {
                        productList += "." + currProduct;
                    } else if (keyString.toLowerCase().contains("[-prod_list]") || keyString.toLowerCase().contains("[-product_list]")) {
                        productList += "-" + currProduct;
                    } else {
                        productList += "_" + currProduct;
                    }
                }
            }
            keyString = replaceAnyKeyStringVariant(keyString, "prod_list", productList, DELIMITOR_STRING);
            keyString = replaceAnyKeyStringVariant(keyString, "product_list", productList, DELIMITOR_STRING);
            keyString = replaceAnyKeyStringVariant(keyString, "PROD_LIST", productList.toUpperCase(), DELIMITOR_STRING);
            keyString = replaceAnyKeyStringVariant(keyString, "PRODUCT_LIST", productList.toUpperCase(), DELIMITOR_STRING);
        }


        keyString = getOfileAddOnResolutionL2BinL3Bin(resolution, keyString);


        keyString = keystringReplaceNSWE(keyString, north, south, west, east);

        return keyString;
    }





    private static String getOfileForL3MapGenAddOns(String resolution, String product, String projection, String interp, String north, String south, String west, String east) {


        if (OCSSW_L3mapgenController.OFILE_NAMING_SCHEME_SUFFIX_NONE.equals(OCSSW_L3mapgenController.getPreferenceOfileNamingSchemeSuffixOptions())) {
            return "";
        }

        String keyString;
        if (OCSSW_L3mapgenController.OFILE_NAMING_SCHEME_SUFFIX1.equals(OCSSW_L3mapgenController.getPreferenceOfileNamingSchemeSuffixOptions())) {
            keyString = OCSSW_L3mapgenController.getPreferenceOfileNamingSchemeSuffix1();
        } else if (OCSSW_L3mapgenController.OFILE_NAMING_SCHEME_SUFFIX2.equals(OCSSW_L3mapgenController.getPreferenceOfileNamingSchemeSuffixOptions())) {
            keyString = OCSSW_L3mapgenController.getPreferenceOfileNamingSchemeSuffix2();
        } else if (OCSSW_L3mapgenController.OFILE_NAMING_SCHEME_SUFFIX_DEFAULT.equals(OCSSW_L3mapgenController.getPreferenceOfileNamingSchemeSuffixOptions())) {
            keyString = OCSSW_L3mapgenController.OFILE_NAMING_SCHEME_SUFFIX_DEFAULT;
        } else if (OCSSW_L3mapgenController.OFILE_NAMING_SCHEME_SUFFIX_DEFAULT2.equals(OCSSW_L3mapgenController.getPreferenceOfileNamingSchemeSuffixOptions())) {
            keyString = OCSSW_L3mapgenController.OFILE_NAMING_SCHEME_SUFFIX_DEFAULT2;
        } else if (OCSSW_L3mapgenController.OFILE_NAMING_SCHEME_SUFFIX_DEFAULT3.equals(OCSSW_L3mapgenController.getPreferenceOfileNamingSchemeSuffixOptions())) {
            keyString = OCSSW_L3mapgenController.OFILE_NAMING_SCHEME_SUFFIX_DEFAULT3;
        } else if (OCSSW_L3mapgenController.OFILE_NAMING_SCHEME_SUFFIX_DEFAULT4.equals(OCSSW_L3mapgenController.getPreferenceOfileNamingSchemeSuffixOptions())) {
            keyString = OCSSW_L3mapgenController.OFILE_NAMING_SCHEME_SUFFIX_DEFAULT4;
        } else {
            keyString = OCSSW_L3mapgenController.OFILE_NAMING_SCHEME_SUFFIX_DEFAULT;
        }


        if (keyString == null || keyString.trim().length() == 0) {
            return "";
        }


        ;


        if (checkForVariantMatch(keyString, "product") || checkForVariantMatch(keyString, "PRODUCT")) {
            String productSingle = "";
            if (product != null && product.trim().length() > 0) {
                String[] productsArray = product.split("[,\\s]");
                if (productsArray != null && productsArray.length == 1) {
                    if (productsArray[0] != null) {
                        productSingle = productsArray[0].trim();
                        if (productSingle.length() > 0) {
                            productSingle = productsArray[0];
                        }
                    }
                }
            }
            keyString = replaceAnyKeyStringVariant(keyString, "product", productSingle, DELIMITOR_STRING);
            keyString = replaceAnyKeyStringVariant(keyString, "PRODUCT", productSingle.toUpperCase(), DELIMITOR_STRING);
        }


        if (checkForVariantMatch(keyString, "product_list") || checkForVariantMatch(keyString, "PRODUCT_LIST")) {
            String productList = "";
            if (product != null && product.trim().length() > 0) {
                String[] productsArray = product.split("[,\\s]");

                for (String currProduct : productsArray) {
                    if (keyString.toLowerCase().contains("[.product_list]")) {
                        productList += "." + currProduct;
                    } else if (keyString.toLowerCase().contains("[-product_list]")) {
                        productList += "-" + currProduct;
                    } else {
                        productList += "_" + currProduct;
                    }
                }
            }

            keyString = replaceAnyKeyStringVariant(keyString, "product_list", productList, DELIMITOR_STRING);
            keyString = replaceAnyKeyStringVariant(keyString, "PRODUCT_LIST", productList.toUpperCase(), DELIMITOR_STRING);
        }


        if (checkForVariantMatch(keyString, "resolution") || checkForVariantMatch(keyString, "RESOLUTION")) {
            if (resolution == null) {
                resolution = "";
            }
            keyString = replaceAnyKeyStringVariant(keyString, "resolution", resolution, DELIMITOR_NUMBER);
            keyString = replaceAnyKeyStringVariant(keyString, "RESOLUTION", resolution.toUpperCase(), DELIMITOR_NUMBER);
        }

        if (checkForVariantMatch(keyString, "resolution_units") || checkForVariantMatch(keyString, "RESOLUTION_UNITS")) {
            if (resolution == null) {
                resolution = "";
            }

            resolution = resolution.trim();
            if (isNumeric(resolution)) {
                resolution = resolution + "m";
            }
            keyString = replaceAnyKeyStringVariant(keyString, "resolution_units", resolution, DELIMITOR_NUMBER);
            keyString = replaceAnyKeyStringVariant(keyString, "RESOLUTION_UNITS", resolution.toUpperCase(), DELIMITOR_NUMBER);
        }


        if (checkForVariantMatch(keyString, "projection") || checkForVariantMatch(keyString, "PROJECTION")) {
            String projectionName = "";
            if (projection != null && projection.trim().length() > 0) {
                String[] projectionArray = projection.split("[\\s]");

                if (projectionArray.length > 1) {
                    String[] projectionArray2 = projectionArray[0].split("=");
                    if (projectionArray2.length > 1) {
                        projectionName = projectionArray2[1];
                    } else {
                        projectionName = projectionArray[0];
                    }
                } else {
                    projectionName = projection;
                }
            }

            keyString = replaceAnyKeyStringVariant(keyString, "projection", projectionName, DELIMITOR_STRING);
            keyString = replaceAnyKeyStringVariant(keyString, "PROJECTION", projectionName.toUpperCase(), DELIMITOR_STRING);
        }


        if (checkForVariantMatch(keyString, "interp") || checkForVariantMatch(keyString, "INTERP")) {
            if (interp == null) {
                interp = "";
            }
            keyString = replaceAnyKeyStringVariant(keyString, "interp", interp, DELIMITOR_STRING);
            keyString = replaceAnyKeyStringVariant(keyString, "INTERP", interp.toUpperCase(), DELIMITOR_STRING);
        }


        keyString = keystringReplaceNSWE(keyString, north, south, west, east);

        return keyString;
    }





    private static String getOfileAddOnResolutionL2BinL3Bin(String resolution, String keyString) {


        if (checkForVariantMatch(keyString, "resolution") || checkForVariantMatch(keyString, "RESOLUTION")) {
            if (resolution == null) {
                resolution = "";
            }
            keyString = replaceAnyKeyStringVariant(keyString, "resolution", resolution, DELIMITOR_NUMBER);
            keyString = replaceAnyKeyStringVariant(keyString, "RESOLUTION", resolution.toUpperCase(), DELIMITOR_NUMBER);
        }


        if (checkForVariantMatch(keyString, "resolve") || checkForVariantMatch(keyString, "RESOLVE")) {
            if (resolution == null) {
                resolution = "";
            }
            keyString = replaceAnyKeyStringVariant(keyString, "resolve", resolution, DELIMITOR_NUMBER);
            keyString = replaceAnyKeyStringVariant(keyString, "RESOLVE", resolution.toUpperCase(), DELIMITOR_NUMBER);
        }


        if (checkForVariantMatch(keyString, "resolution_units") || checkForVariantMatch(keyString, "RESOLUTION_UNITS")
                || checkForVariantMatch(keyString, "resolve_units") || checkForVariantMatch(keyString, "RESOLVE_UNITS")
        ) {
            if (resolution == null) {
                resolution = "";
            }

            String resolution_units = resolution;
            switch (resolution) {
                case "HH":
                    resolution_units = "50m";
                    break;
                case "HQ":
                    resolution_units = "100m";
                    break;
                case "Q":
                    resolution_units = "250m";
                    break;
                case "H":
                    resolution_units = "500m";
                    break;
                case "1":
                    resolution_units = "1.1km";
                    break;
                case "2":
                    resolution_units = "2.3km";
                    break;
                case "4":
                    resolution_units = "4.6km";
                    break;
                case "9":
                    resolution_units = "9.2km";
                    break;
                case "18":
                    resolution_units = "18.5km";
                    break;
                case "36":
                    resolution_units = "36km";
                    break;
                case "QD":
                    resolution_units = "0.25degree";
                    break;
                case "HD":
                    resolution_units = "0.5degree";
                    break;
                case "1D":
                    resolution_units = "1degree";
                    break;
            }

            keyString = replaceAnyKeyStringVariant(keyString, "resolution_units", resolution_units, DELIMITOR_NUMBER);
            keyString = replaceAnyKeyStringVariant(keyString, "resolve_units", resolution_units, DELIMITOR_NUMBER);
            keyString = replaceAnyKeyStringVariant(keyString, "RESOLUTION_UNITS", resolution_units.toUpperCase(), DELIMITOR_NUMBER);
            keyString = replaceAnyKeyStringVariant(keyString, "RESOLVE_UNITS", resolution_units.toUpperCase(), DELIMITOR_NUMBER);
        }

        return keyString;
    }


    public static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch(NumberFormatException e){
            return false;
        }
    }

    private static String keystringReplaceNSWE(String keyString, String north, String south, String west, String east) {

        //    [_nswe]  [_nswe°] [_NSWE°] [_nswedegrees]


        // make sure key is uppercase
        keyString = convertAnyUpperCaseKeyToLowerCase(keyString, "north");
        keyString = convertAnyUpperCaseKeyToLowerCase(keyString, "north°");
        keyString = convertAnyUpperCaseKeyToLowerCase(keyString, "north_deg");

        keyString = convertAnyUpperCaseKeyToLowerCase(keyString, "south");
        keyString = convertAnyUpperCaseKeyToLowerCase(keyString, "south°");
        keyString = convertAnyUpperCaseKeyToLowerCase(keyString, "south_deg");

        keyString = convertAnyUpperCaseKeyToLowerCase(keyString, "west");
        keyString = convertAnyUpperCaseKeyToLowerCase(keyString, "west°");
        keyString = convertAnyUpperCaseKeyToLowerCase(keyString, "west_deg");

        keyString = convertAnyUpperCaseKeyToLowerCase(keyString, "east");
        keyString = convertAnyUpperCaseKeyToLowerCase(keyString, "east°");
        keyString = convertAnyUpperCaseKeyToLowerCase(keyString, "east_deg");

        keyString = convertAnyUpperCaseKeyToLowerCase(keyString, "nswe");
        keyString = convertAnyUpperCaseKeyToLowerCase(keyString, "nswe°");
        keyString = convertAnyUpperCaseKeyToLowerCase(keyString, "nswe_deg");


        if (checkForVariantMatch(keyString, "north")
                || checkForVariantMatch(keyString, "north°")
                || checkForVariantMatch(keyString, "north_deg")
        ) {
            if (north == null) {
                north = "";
            }
            keyString = replaceAnyKeyStringVariant(keyString, "north", north, null, "N", DELIMITOR_NUMBER);
            keyString = replaceAnyKeyStringVariant(keyString, "north°", north, null, "°N", DELIMITOR_NUMBER);
            keyString = replaceAnyKeyStringVariant(keyString, "north_deg", north, null, "°N", DELIMITOR_NUMBER);
        }


        if (checkForVariantMatch(keyString, "south")
                || checkForVariantMatch(keyString, "south°")
                || checkForVariantMatch(keyString, "south_deg")
        ) {
            if (south == null) {
                south = "";
            }
            keyString = replaceAnyKeyStringVariant(keyString, "south", south, null, "S", DELIMITOR_NUMBER);
            keyString = replaceAnyKeyStringVariant(keyString, "south°", south, null, "°S", DELIMITOR_NUMBER);
            keyString = replaceAnyKeyStringVariant(keyString, "south_deg", south, null, "°S", DELIMITOR_NUMBER);
        }


        if (checkForVariantMatch(keyString, "west")
                || checkForVariantMatch(keyString, "west°")
                || checkForVariantMatch(keyString, "west_deg")
        ) {
            if (west == null) {
                west = "";
            }
            keyString = replaceAnyKeyStringVariant(keyString, "west", west, null, "W", DELIMITOR_NUMBER);
            keyString = replaceAnyKeyStringVariant(keyString, "west°", west, null, "°W", DELIMITOR_NUMBER);
            keyString = replaceAnyKeyStringVariant(keyString, "west_deg", west, null, "°W", DELIMITOR_NUMBER);
        }


        if (checkForVariantMatch(keyString, "east")
                || checkForVariantMatch(keyString, "east°")
                || checkForVariantMatch(keyString, "east_deg")
        ) {
            if (east == null) {
                east = "";
            }
            keyString = replaceAnyKeyStringVariant(keyString, "east", east, null, "E", DELIMITOR_NUMBER);
            keyString = replaceAnyKeyStringVariant(keyString, "east°", east, null, "°E", DELIMITOR_NUMBER);
            keyString = replaceAnyKeyStringVariant(keyString, "east_deg", east, null, "°E", DELIMITOR_NUMBER);
        }


        if (checkForVariantMatch(keyString, "nswe")
                || checkForVariantMatch(keyString, "nswe°")
                || checkForVariantMatch(keyString, "nswe_deg")
                || checkForVariantMatch(keyString, "nsew")
                || checkForVariantMatch(keyString, "nsew°")
                || checkForVariantMatch(keyString, "nsew_deg°")
        ) {
            if (north == null) {
                north = "";
            }
            if (south == null) {
                south = "";
            }
            if (west == null) {
                west = "";
            }
            if (east == null) {
                east = "";
            }

            String nswe = "";
            String nsweDeg = "";
            String nsweDegSymbol = "";
            String nsew = "";
            String nsewDeg = "";
            String nsewDegSymbol = "";

            if (north.length() > 0) {
                nswe = nswe + "_" + north + "N";
                nsweDeg = nsweDeg + "_" + north + "degN";
                nsweDegSymbol = nsweDegSymbol + "_" + north + "°N";

                nsew = nsew + "_" + north + "N";
                nsewDeg = nsewDeg + "_" + north + "degN";
                nsewDegSymbol = nsewDegSymbol + "_" + north + "°N";
            }

            if (south.length() > 0) {
                nswe = nswe + "_" + south + "S";
                nsweDeg = nsweDeg + "_" + south + "degS";
                nsweDegSymbol = nsweDegSymbol + "_" + south + "°S";

                nsew = nsew + "_" + south + "S";
                nsewDeg = nsewDeg + "_" + south + "degS";
                nsewDegSymbol = nsewDegSymbol + "_" + south + "°S";
            }

            if (west.length() > 0) {
                nswe = nswe + "_" + west + "W";
                nsweDeg = nsweDeg + "_" + west + "degW";
                nsweDegSymbol = nsweDegSymbol + "_" + west + "°W";
            }
            if (east.length() > 0) {
                nswe = nswe + "_" + east + "E";
                nsweDeg = nsweDeg + "_" + east + "degE";
                nsweDegSymbol = nsweDegSymbol + "_" + east + "°E";

                nsew = nsew + "_" + east + "E";
                nsewDeg = nsewDeg + "_" + east + "degE";
                nsewDegSymbol = nsewDegSymbol + "_" + east + "°E";
            }

            if (west.length() > 0) {
                nsew = nsew + "_" + west + "W";
                nsewDeg = nsewDeg + "_" + west + "degW";
                nsewDegSymbol = nsewDegSymbol + "_" + west + "°W";
            }


            if (nswe.length() > 0) {
                nswe = nswe.substring(1);
            }
            if (nsweDeg.length() > 0) {
                nsweDeg = nsweDeg.substring(1);
            }
            if (nsweDegSymbol.length() > 0) {
                nsweDegSymbol = nsweDegSymbol.substring(1);
            }

            if (nsew.length() > 0) {
                nsew = nsew.substring(1);
            }
            if (nsewDeg.length() > 0) {
                nsewDeg = nsewDeg.substring(1);
            }
            if (nsewDegSymbol.length() > 0) {
                nsewDegSymbol = nsewDegSymbol.substring(1);
            }


            keyString = replaceAnyKeyStringVariant(keyString, "nswe", nswe, DELIMITOR_NUMBER);
            keyString = replaceAnyKeyStringVariant(keyString, "nswe_deg", nsweDeg, DELIMITOR_NUMBER);
            keyString = replaceAnyKeyStringVariant(keyString, "nswe°", nsweDegSymbol, DELIMITOR_NUMBER);
            keyString = replaceAnyKeyStringVariant(keyString, "nsew", nsew, DELIMITOR_NUMBER);
            keyString = replaceAnyKeyStringVariant(keyString, "nsew_deg", nsewDeg, DELIMITOR_NUMBER);
            keyString = replaceAnyKeyStringVariant(keyString, "nsew°", nsewDegSymbol, DELIMITOR_NUMBER);
        }

        return keyString;
    }




    private static String convertAnyUpperCaseKeyToLowerCase(String keyString, String key) {

        String keyUpperCase = key.toUpperCase();
        keyString = keyString.replace("[" + keyUpperCase + "]", "[" + key + "]");
        keyString = keyString.replace("[." + keyUpperCase + "]", "[." + key + "]");
        keyString = keyString.replace("[_" + keyUpperCase + "]", "[_" + key + "]");
        keyString = keyString.replace("[-" + keyUpperCase + "]", "[-" + key + "]");
        keyString = keyString.replace("[keyUpperCase]", "[key]");

        return keyString;
    }


    private static String replaceAnyKeyStringVariant(String keyString, String key, String value, String delimitorDefault) {
        return replaceAnyKeyStringVariant(keyString, key, value, null, null, delimitorDefault);
    }

    private static String replaceAnyKeyStringVariant(String keyString, String key, String value, String prefix, String suffix, String delimitorDefault) {

        if (value == null) {
            value = "";
        }

        if (value.length() > 0) {
            if (prefix != null) {
                value = prefix + value;
            }
            if (suffix != null) {
                value = value + suffix;
            }
        }
        value = value.trim();

        if (value.length() > 0) {
            keyString = keyString.replace("[" + key + "]", delimitorDefault + value);  // default
            keyString = keyString.replace("[." + key + "]", "." + value);
            keyString = keyString.replace("[_" + key + "]", "_" + value);
            keyString = keyString.replace("[-" + key + "]", "-" + value);
        } else {
            keyString = keyString.replace("[" + key + "]", "");
            keyString = keyString.replace("[." + key + "]", "");
            keyString = keyString.replace("[_" + key + "]", "");
            keyString = keyString.replace("[-" + key + "]", "");
        }

        keyString = keyString.replace("[-" + key + "]", "-" + value);

        return keyString;
    }

    private static boolean checkForVariantMatch(String keyString, String key) {

        if (keyString.contains("[" + key + "]") || keyString.contains("[." + key + "]") || keyString.contains("[_" + key + "]") || keyString.contains("[-" + key + "]")) {
            return true;
        }

        return false;
    }


    private static String trimStringChars(String string, String key, boolean trimStart, boolean trimEnd, boolean trimDuplicates) {

        String stringOriginal = string;

        if (string == null || key == null) {
            return stringOriginal;
        }

        string = string.trim();
        key = key.trim();

        if (string.length() == 0 || key.length() == 0) {
            return stringOriginal;
        }


        while (trimDuplicates && string.contains(key + key)) {
            string = string.replace((key + key), key);
        }


        if (trimStart && string.startsWith(".")) {
            string = string.substring(1, string.length());
        }

        if (trimEnd && string.endsWith(".")) {
            string = string.substring(0, string.length() - 1);
        }


        return string;

    }

    private static String stripFilenameExtension(String filename) {
        if (filename == null || filename.trim().length() == 0) {
            return filename;
        }

        String fileBasename = filename;

        if (filename.endsWith(".nc")) {
            fileBasename = filename.substring(0, filename.length() - 3);
        }

        return fileBasename;
    }

    private static String stripFilenameExtractExtension(String filename, String extension) {
        if (filename == null || filename.trim().length() == 0 || extension == null || extension.trim().length() == 0) {
            return filename;
        }

        String fileBasename = filename;

        if (filename.endsWith(extension)) {
            fileBasename = filename.substring(0, filename.length() - extension.length());
        }

        return fileBasename;
    }







    private  static String getOfileFullPathWithIfileDir(String ifileOriginal, String ofilenameDefault) {

        if (ifileOriginal == null || ifileOriginal.trim().length() == 0 ||  ofilenameDefault == null || ofilenameDefault.trim().length() == 0) {
            return "";
        }
        //added the following line to prevent have double parent path for ofile
        //todo check whether this line gets used in Docker
        if (ofilenameDefault.contains(File.separator)) {
            ofilenameDefault = ofilenameDefault.substring(ofilenameDefault.lastIndexOf(File.separator) + 1);
        }

        // add the path
        File file = new File(ifileOriginal);
        if (file != null) {
            File parentFile = file.getParentFile();

            if (parentFile != null) {
                String parentPath = parentFile.getAbsolutePath();

                if (parentPath != null && parentPath.trim().length() > 0) {
                    File file2 = new File(parentPath, ofilenameDefault);
                    if (file2 != null) {
                        ofilenameDefault = file2.getAbsolutePath();
                    }
                }
            }
        }

        String ofilename = stripFilenameExtension(ofilenameDefault);
        return ofilename;
    }





    private static String getOfileForL3MapGenAddExtension(String ofilename, String oformat) {

        if (oformat == null || oformat.trim().length() == 0) {
            oformat = "NETCDF4";
        }
        oformat = oformat.toUpperCase();

        switch (oformat) {
            case "NETCDF4":
                ofilename += ".nc";
                break;
            case "HDF4":
                ofilename += ".hdf";
                break;
            case "PNG":
                ofilename += ".png";
                break;
            case "PPM":
                ofilename += ".ppm";
                break;
            case "TIFF":
                ofilename += ".tiff";
                break;
            default:
                ofilename += ".nc";
                break;

        }

        return ofilename;
    }









}
