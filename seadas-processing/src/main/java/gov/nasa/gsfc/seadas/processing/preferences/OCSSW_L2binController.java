/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package gov.nasa.gsfc.seadas.processing.preferences;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.PropertyEditorRegistry;
import com.bc.ceres.swing.binding.PropertyPane;
import org.esa.snap.core.util.PropertyMap;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.preferences.DefaultConfigController;
import org.esa.snap.rcp.preferences.Preference;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;

import javax.swing.*;
import java.awt.*;

import static com.bc.ceres.swing.TableLayout.cell;

/**
 * * Panel handling l2bin preferences. Sub-panel of the "SeaDAS-Toolbox"-panel.
 *
 * @author Daniel Knowles
 */


@OptionsPanelController.SubRegistration(location = "SeaDAS/OCSSW",
        displayName = "#Options_DisplayName_OCSSW_L2bin",
        keywords = "#Options_Keywords_OCSSW_L2bin",
        keywordsCategory = "OCSSW",
        id = "L2bin_preferences")
@org.openide.util.NbBundle.Messages({
        "Options_DisplayName_OCSSW_L2bin=L2bin",
        "Options_Keywords_OCSSW_L2bin=seadas, ocssw, l2bin"
})
public final class OCSSW_L2binController extends DefaultConfigController {

    // Preferences property prefix
    private static final String PROPERTY_L2BIN_ROOT_KEY = SeadasToolboxDefaults.PROPERTY_SEADAS_ROOT_KEY + ".l2bin";
    
    public static final String PROPERTY_L2BIN_ROOT_FAV_KEY = PROPERTY_L2BIN_ROOT_KEY + ".favorites";

    Property flaguse;
    Property flaguse_notes;
    Property l3bprod;
    Property output_wavelengths;
    Property suite;
    Property resolution;
    Property north;
    Property south;
    Property west;
    Property east;
    Property l3bprodAutofill;
    Property flaguseAutofill;
    Property autoFillOther;

    Property favoriteFlaguseSection;
    Property fav1SetToDefault;
    Property fav1FlagUse;
    Property fav2SetToDefault;
    Property fav2FlagUse;
    Property fav3SetToDefault;
    Property fav3FlagUse;
    Property fav4SetToDefault;
    Property fav4FlagUse;
    Property fav5SetToDefault;
    Property fav5FlagUse;
    
    Property fav1FlagUseNotes;
    Property fav2FlagUseNotes;
    Property fav3FlagUseNotes;
    Property fav4FlagUseNotes;
    Property fav5FlagUseNotes;
    
    Property favoriteProductsSection;
    Property fav1Products_SetToDefault;
    Property fav1Products_l3bprod;
    Property fav1Products_output_wavelengths;
    Property fav1Products_suite;

    Property fav2Products_SetToDefault;
    Property fav2Products_l3bprod;
    Property fav2Products_output_wavelengths;
    Property fav2Products_suite;


    Property fav3Products_SetToDefault;
    Property fav3Products_l3bprod;
    Property fav3Products_output_wavelengths;
    Property fav3Products_suite;


    Property fav4Products_SetToDefault;
    Property fav4Products_l3bprod;
    Property fav4Products_output_wavelengths;
    Property fav4Products_suite;


    Property fav5Products_SetToDefault;
    Property fav5Products_l3bprod;
    Property fav5Products_output_wavelengths;
    Property fav5Products_suite;


    Property favoriteGeospatial_Section;
    Property fav1Geospatial_SetToDefault;
    Property fav1Geospatial_NSWE;
    Property fav1Geospatial_resolution;

    Property fav2Geospatial_SetToDefault;
    Property fav2Geospatial_NSWE;
    Property fav2Geospatial_resolution;

    Property fav3Geospatial_SetToDefault;
    Property fav3Geospatial_NSWE;
    Property fav3Geospatial_resolution;

    Property fav4Geospatial_SetToDefault;
    Property fav4Geospatial_NSWE;
    Property fav4Geospatial_resolution;

    Property fav5Geospatial_SetToDefault;
    Property fav5Geospatial_NSWE;
    Property fav5Geospatial_resolution;

    Property namingScheme;
    Property fieldsAdd;
    
    Property restoreDefaults;

    boolean propertyValueChangeEventsEnabled = true;


    public static final String  OFILE_NAMING_SCHEME_SIMPLE = "output";
    public static final String OFILE_NAMING_SCHEME_IFILE_PLUS_SUFFIX = "IFILE + SUFFIX";
    public static final String  OFILE_NAMING_SCHEME_OCSSW_SHORT = "OCSSW (do not derive time field)";
    public static final String  OFILE_NAMING_SCHEME_OCSSW = "OCSSW";
    public static final String OFILE_NAMING_SCHEME_IFILE_REPLACE = "IFILE";


    public static final String OFILE_NAMING_SCHEME_SUFFIX_NONE = "No Suffix";
    public static final String OFILE_NAMING_SCHEME_SUFFIX1 = "Suffix Custom 1";
    public static final String OFILE_NAMING_SCHEME_SUFFIX2 = "Suffix Custom 2";
    public static final String OFILE_NAMING_SCHEME_SUFFIX_DEFAULT = "[l3bprod][resolution_units]";
    public static final String OFILE_NAMING_SCHEME_SUFFIX_DEFAULT2 = "[l3bprod][resolution_units][suite]";
    public static final String OFILE_NAMING_SCHEME_SUFFIX_DEFAULT3 = "[l3bprod][resolution][suite][nswe_deg]";
    public static final String OFILE_NAMING_SCHEME_SUFFIX_DEFAULT4 = "[l3bprod][resolution][nswe_deg]";
    
    



    public static final String PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SECTION_KEY = PROPERTY_L2BIN_ROOT_KEY + ".ofile.naming.scheme.section";
    public static final String PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SECTION_LABEL = "Naming Scheme for 'ofile'";
    public static final String PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SECTION_TOOLTIP = "Naming scheme to use for autofilling ofile name";

    public static final String PROPERTY_L2BIN_OFILE_NAMING_SCHEME_KEY = PROPERTY_L2BIN_ROOT_KEY + ".ofile.naming.scheme";
    public static final String PROPERTY_L2BIN_OFILE_NAMING_SCHEME_LABEL = "Basename Options";
    public static final String PROPERTY_L2BIN_OFILE_NAMING_SCHEME_TOOLTIP = "Naming scheme to use for autofilling ofile name";
    public static final String PROPERTY_L2BIN_OFILE_NAMING_SCHEME_DEFAULT = OFILE_NAMING_SCHEME_OCSSW;



    public static final String PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SUFFIX_OPTIONS_KEY = PROPERTY_L2BIN_ROOT_KEY + ".ofile.naming.scheme.suffix.options";
    public static final String PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SUFFIX_OPTIONS_LABEL = "Suffix Options";
    public static final String PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SUFFIX_OPTIONS_TOOLTIP = "ofile Add Suffix Scheme";
    public static final String PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SUFFIX_OPTIONS_DEFAULT = OFILE_NAMING_SCHEME_SUFFIX1;


    public  static  final  String SUFFIX_LIST_TOOLTIPS = "<html>" +
            "ofile Naming scheme keyed add-ons as suffix of ofile name<br>" +
            "[l3bprod] : adds 'l3bprod' field with '.' as delimiter<br>" +
            "[_l3bprod] : adds 'l3bprod' field with '_' as delimiter<br>" +
            "[-l3bprod] : adds 'resolution' field with '-' as delimiter<br>" +
            "[.l3bprod] : adds 'projection' field with '.' as delimiter<br>" +
            "</html>";

    public static final String PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SUFFIX1_KEY = PROPERTY_L2BIN_ROOT_KEY + ".ofile.naming.scheme.suffix1";
    public static final String PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SUFFIX1_LABEL = OFILE_NAMING_SCHEME_SUFFIX1;
    public static final String PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SUFFIX1_TOOLTIP = SUFFIX_LIST_TOOLTIPS;
    public static final String PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SUFFIX1_DEFAULT = OFILE_NAMING_SCHEME_SUFFIX_DEFAULT;


    public static final String PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SUFFIX2_KEY = PROPERTY_L2BIN_ROOT_KEY + ".ofile.naming.scheme.suffix2";
    public static final String PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SUFFIX2_LABEL = OFILE_NAMING_SCHEME_SUFFIX2;
    public static final String PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SUFFIX2_TOOLTIP = SUFFIX_LIST_TOOLTIPS;
    public static final String PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SUFFIX2_DEFAULT = "";


    public static final String PROPERTY_L2BIN_OFILE_NAMING_SCHEME_IFILE_ORIGINAL_KEY = PROPERTY_L2BIN_ROOT_KEY + ".ofile.naming.scheme.ifile.original";
    public static final String PROPERTY_L2BIN_OFILE_NAMING_SCHEME_IFILE_ORIGINAL_LABEL = "Basename " + OFILE_NAMING_SCHEME_IFILE_REPLACE +  " (Original Text)";
    public static final String PROPERTY_L2BIN_OFILE_NAMING_SCHEME_IFILE_ORIGINAL_TOOLTIP = "ofile Ifile Original";
    public static final String PROPERTY_L2BIN_OFILE_NAMING_SCHEME_IFILE_ORIGINAL_DEFAULT = ".L2.";

    public static final String PROPERTY_L2BIN_OFILE_NAMING_SCHEME_IFILE_REPLACE_KEY = PROPERTY_L2BIN_ROOT_KEY + ".ofile.naming.scheme.ifile.replace";
    public static final String PROPERTY_L2BIN_OFILE_NAMING_SCHEME_IFILE_REPLACE_LABEL =  "Basename " + OFILE_NAMING_SCHEME_IFILE_REPLACE + " (Replacement Text)";
    public static final String PROPERTY_L2BIN_OFILE_NAMING_SCHEME_IFILE_REPLACE_TOOLTIP = "ofile Ifile Replace";
    public static final String PROPERTY_L2BIN_OFILE_NAMING_SCHEME_IFILE_REPLACE_DEFAULT = ".L3b.";
    



    public static final String PROPERTY_L2BIN_PARAMETERS_SECTION_KEY = PROPERTY_L2BIN_ROOT_KEY + ".product.parameters.section";
    public static final String PROPERTY_L2BIN_PARAMETERS_SECTION_LABEL = "Product & Suite Parameters";
    public static final String PROPERTY_L2BIN_PARAMETERS_SECTION_TOOLTIP = "L2bin parameters";
    
    
    public static final String PROPERTY_L2BIN_L3BPROD_KEY = PROPERTY_L2BIN_ROOT_KEY + ".l3bprod";
    public static final String PROPERTY_L2BIN_L3BPROD_LABEL = "l3bprod";
    public static final String PROPERTY_L2BIN_L3BPROD_TOOLTIP = "Product (or product list)";
    public static final String PROPERTY_L2BIN_L3BPROD_DEFAULT = "";

    public static final String PROPERTY_L2BIN_OUTPUT_WAVELENGTHS_KEY = PROPERTY_L2BIN_ROOT_KEY + ".output_wavelengths";
    public static final String PROPERTY_L2BIN_OUTPUT_WAVELENGTHS_LABEL = "output_wavelengths";
    public static final String PROPERTY_L2BIN_OUTPUT_WAVELENGTHS_TOOLTIP = "Comma delimited list of 3D wavelengths";
    public static final String PROPERTY_L2BIN_OUTPUT_WAVELENGTHS_DEFAULT = "";

    public static final String PROPERTY_L2BIN_SUITE_KEY = PROPERTY_L2BIN_ROOT_KEY + ".suite";
    public static final String PROPERTY_L2BIN_SUITE_LABEL = "suite";
    public static final String PROPERTY_L2BIN_SUITE_TOOLTIP = "Product Suite";
    public static final String PROPERTY_L2BIN_SUITE_DEFAULT = "";

    public static final String PROPERTY_L2BIN_COMPOSITE_PROD_KEY = PROPERTY_L2BIN_ROOT_KEY + ".composite_prod";
    public static final String PROPERTY_L2BIN_COMPOSITE_PROD_LABEL = "composite_prod";
    public static final String PROPERTY_L2BIN_COMPOSITE_PROD_TOOLTIP = "Product to use as the basis for the composite scheme";
    public static final String PROPERTY_L2BIN_COMPOSITE_PROD_DEFAULT = "";

    public static final String PROPERTY_L2BIN_COMPOSITE_SCHEME_KEY = PROPERTY_L2BIN_ROOT_KEY + ".composite_scheme";
    public static final String PROPERTY_L2BIN_COMPOSITE_SCHEME_LABEL = "composite_scheme";
    public static final String PROPERTY_L2BIN_COMPOSITE_SCHEME_TOOLTIP = "Composite Scheme (uses composite_prod)";
    public static final String PROPERTY_L2BIN_COMPOSITE_SCHEME_DEFAULT = "";

    public static final String PROPERTY_L2BIN_ROW_GROUP_KEY = PROPERTY_L2BIN_ROOT_KEY + ".rowgroup";
    public static final String PROPERTY_L2BIN_ROW_GROUP_LABEL = "rowgroup";
    public static final String PROPERTY_L2BIN_ROW_GROUP_TOOLTIP = "Number of bin rows to process at once";
    public static final String PROPERTY_L2BIN_ROW_GROUP_DEFAULT = "";

    public static final String PROPERTY_L2BIN_PRODTYPE_KEY = PROPERTY_L2BIN_ROOT_KEY + ".prodtype";
    public static final String PROPERTY_L2BIN_PRODTYPE_LABEL = "prodtype";
    public static final String PROPERTY_L2BIN_PRODTYPE_TOOLTIP = "Product type";
    public static final String PROPERTY_L2BIN_PRODTYPE_DEFAULT = "regional";


    public static final String PROPERTY_L2BIN_PARAMETERS_GEOSPATIAL_SECTION_KEY = PROPERTY_L2BIN_ROOT_KEY + ".ofile.parameters.geospatial.section";
    public static final String PROPERTY_L2BIN_PARAMETERS_GEOSPATIAL_SECTION_LABEL = "Geospatial Parameters";
    public static final String PROPERTY_L2BIN_PARAMETERS_GEOSPATIAL_SECTION_TOOLTIP = "L2bin geospatial parameters";

    public static final String PROPERTY_L2BIN_PARAMETERS_BINNING_SECTION_KEY = PROPERTY_L2BIN_ROOT_KEY + ".ofile.parameters.bin_method.section";
    public static final String PROPERTY_L2BIN_PARAMETERS_BINNING_SECTION_LABEL = "General Parameters";
    public static final String PROPERTY_L2BIN_PARAMETERS_BINNING_SECTION_TOOLTIP = "L2bin bin method parameters";

    public static final String PROPERTY_L2BIN_PARAMETERS_FLAGUSE_SECTION_KEY = PROPERTY_L2BIN_ROOT_KEY + ".ofile.parameters.flag_masking.section";
    public static final String PROPERTY_L2BIN_PARAMETERS_FLAGUSE_SECTION_LABEL = "Flag Masking Parameters";
    public static final String PROPERTY_L2BIN_PARAMETERS_FLAGUSE_SECTION_TOOLTIP = "L2bin flag masking parameters";

    public static final String PROPERTY_L2BIN_PARAMETERS_TEMPORAL_SECTION_KEY = PROPERTY_L2BIN_ROOT_KEY + ".ofile.parameters.temporal.section";
    public static final String PROPERTY_L2BIN_PARAMETERS_TEMPORAL_SECTION_LABEL = "Temporal Parameters";
    public static final String PROPERTY_L2BIN_PARAMETERS_TEMPORAL_SECTION_TOOLTIP = "L2bin temporal parameters";
    
    
    public static final String PROPERTY_L2BIN_RESOLUTION_KEY = PROPERTY_L2BIN_ROOT_KEY + ".resolution";
    public static final String PROPERTY_L2BIN_RESOLUTION_LABEL = "resolution";
    public static final String PROPERTY_L2BIN_RESOLUTION_TOOLTIP = "Bin resolution";
    public static final String PROPERTY_L2BIN_RESOLUTION_DEFAULT = "";

    public static final String PROPERTY_L2BIN_AREA_WEIGHTING_KEY = PROPERTY_L2BIN_ROOT_KEY + ".area_weighting";
    public static final String PROPERTY_L2BIN_AREA_WEIGHTING_LABEL = "area_weighting";
    public static final String PROPERTY_L2BIN_AREA_WEIGHTING_TOOLTIP = "Area Weighting";
    public static final String PROPERTY_L2BIN_AREA_WEIGHTING_DEFAULT = "";

    public static final String PROPERTY_L2BIN_FLAGUSE_KEY = PROPERTY_L2BIN_ROOT_KEY + ".flaguse";
    public static final String PROPERTY_L2BIN_FLAGUSE_LABEL = "flaguse";
    public static final String PROPERTY_L2BIN_FLAGUSE_TOOLTIP = "Flags to use for binning";
    public static final String PROPERTY_L2BIN_FLAGUSE_DEFAULT = "";
    
    public static final String PROPERTY_L2BIN_FLAGUSE_NOTES_KEY = PROPERTY_L2BIN_ROOT_KEY + ".flaguse.notes";
    public static final String PROPERTY_L2BIN_FLAGUSE_NOTES_LABEL = "Notes/Description";
    public static final String PROPERTY_L2BIN_FLAGUSE_NOTES_TOOLTIP = "User notes/description";
    public static final String PROPERTY_L2BIN_FLAGUSE_NOTES_DEFAULT = "";

    public static final String PROPERTY_L2BIN_LATNORTH_KEY = PROPERTY_L2BIN_ROOT_KEY + ".latnorth";
    public static final String PROPERTY_L2BIN_LATNORTH_LABEL = "latnorth";
    public static final String PROPERTY_L2BIN_LATNORTH_TOOLTIP = "Northernmost latitude";
    public static final String PROPERTY_L2BIN_LATNORTH_DEFAULT = "";

    public static final String PROPERTY_L2BIN_LATSOUTH_KEY = PROPERTY_L2BIN_ROOT_KEY + ".latsouth";
    public static final String PROPERTY_L2BIN_LATSOUTH_LABEL = "latsouth";
    public static final String PROPERTY_L2BIN_LATSOUTH_TOOLTIP = "Southernmost latitude";
    public static final String PROPERTY_L2BIN_LATSOUTH_DEFAULT = "";

    public static final String PROPERTY_L2BIN_LONWEST_KEY = PROPERTY_L2BIN_ROOT_KEY + ".lonwest";
    public static final String PROPERTY_L2BIN_LONWEST_LABEL = "lonwest";
    public static final String PROPERTY_L2BIN_LONWEST_TOOLTIP = "Westernmost longitude";
    public static final String PROPERTY_L2BIN_LONWEST_DEFAULT = "";

    public static final String PROPERTY_L2BIN_LONEAST_KEY = PROPERTY_L2BIN_ROOT_KEY + ".loneast";
    public static final String PROPERTY_L2BIN_LONEAST_LABEL = "loneast";
    public static final String PROPERTY_L2BIN_LONEAST_TOOLTIP = "Easternmost longitude";
    public static final String PROPERTY_L2BIN_LONEAST_DEFAULT = "";


    public static final String PROPERTY_L2BIN_AUTOFILL_KEY = PROPERTY_L2BIN_ROOT_KEY + ".autofill.other";
    public static final String PROPERTY_L2BIN_AUTOFILL_LABEL = "Autofill other fields with suite defaults";
    public static final String PROPERTY_L2BIN_AUTOFILL_TOOLTIP = "<html>Autofills other field with the suite defaults. <br> Note: if a field is set in the preferences then it overrides the suite default.</html>";
    public static final boolean PROPERTY_L2BIN_AUTOFILL_DEFAULT = true;

    public static final String PROPERTY_L2BIN_FLAGUSE_AUTOFILL_KEY = PROPERTY_L2BIN_ROOT_KEY + ".flaguse.autofill";
    public static final String PROPERTY_L2BIN_FLAGUSE_AUTOFILL_LABEL = "Autofill field 'flaguse' with suite defaults";
    public static final String PROPERTY_L2BIN_FLAGUSE_AUTOFILL_TOOLTIP = "<html>Autofills fields with the suite defaults. <br> Note: if field is set in the preferences then it overrides the suite default.</html>";
    public static final boolean PROPERTY_L2BIN_FLAGUSE_AUTOFILL_DEFAULT = true;

    public static final String PROPERTY_L2BIN_L3BPROD_AUTOFILL_KEY = PROPERTY_L2BIN_ROOT_KEY + ".l3bprod.autofill";
    public static final String PROPERTY_L2BIN_L3BPROD_AUTOFILL_LABEL = "Autofill field 'l3bprod' with suite defaults";
    public static final String PROPERTY_L2BIN_L3BPROD_AUTOFILL_TOOLTIP = "<html>Autofills l3bprod with the suite defaults. <br> Note: if field is set in the preferences then it overrides the suite default.</html>";
    public static final boolean PROPERTY_L2BIN_L3BPROD_AUTOFILL_DEFAULT = true;


    public static final String PROPERTY_L2BIN_AUTOFILL_PRECEDENCE_KEY = PROPERTY_L2BIN_ROOT_KEY + ".autofill.precedence";
    public static final String PROPERTY_L2BIN_AUTOFILL_PRECEDENCE_LABEL = "Preference values take precedence over suite defaults";
    public static final String PROPERTY_L2BIN_AUTOFILL_PRECEDENCE_TOOLTIP = "<html>Preferences takes precedence over suite defaults when suite is specified.  <br>Note: if suite not specified then preferences always takes precedence.</html>";
    public static final boolean PROPERTY_L2BIN_AUTOFILL_PRECEDENCE_DEFAULT = false;

    public static final String PROPERTY_L2BIN_AUTOFILL_PRECEDENCE_NULL_SUITE_KEY = PROPERTY_L2BIN_ROOT_KEY + ".autofill.precedence_null_suite";
    public static final String PROPERTY_L2BIN_AUTOFILL_PRECEDENCE_NULL_SUITE_LABEL = "Preference values take precedence over null suite defaults";
    public static final String PROPERTY_L2BIN_AUTOFILL_PRECEDENCE_NULL_SUITE_TOOLTIP = "<html>Preferences takes precedence over suite defaults when suite is specified.  <br>Note: if suite not specified then preferences always takes precedence.</html>";
    public static final boolean PROPERTY_L2BIN_AUTOFILL_PRECEDENCE_NULL_SUITE_DEFAULT = true;
    
    
    public static final String PROPERTY_L2BIN_SDAY_KEY = PROPERTY_L2BIN_ROOT_KEY + ".sday";
    public static final String PROPERTY_L2BIN_SDAY_LABEL = "sday";
    public static final String PROPERTY_L2BIN_SDAY_TOOLTIP = "Start day";
    public static final String PROPERTY_L2BIN_SDAY_DEFAULT = "";

    public static final String PROPERTY_L2BIN_EDAY_KEY = PROPERTY_L2BIN_ROOT_KEY + ".eday";
    public static final String PROPERTY_L2BIN_EDAY_LABEL = "eday";
    public static final String PROPERTY_L2BIN_EDAY_TOOLTIP = "End day";
    public static final String PROPERTY_L2BIN_EDAY_DEFAULT = "";

    public static final String PROPERTY_L2BIN_DELTA_CROSS_KEY = PROPERTY_L2BIN_ROOT_KEY + ".delta_crossing_time";
    public static final String PROPERTY_L2BIN_DELTA_CROSS_LABEL = "delta_crossing_time";
    public static final String PROPERTY_L2BIN_DELTA_CROSS_TOOLTIP = "delta_crossing_time";
    public static final String PROPERTY_L2BIN_DELTA_CROSS_DEFAULT = "";

    public static final String PROPERTY_L2BIN_NIGHT_KEY = PROPERTY_L2BIN_ROOT_KEY + ".night";
    public static final String PROPERTY_L2BIN_NIGHT_LABEL = "night";
    public static final String PROPERTY_L2BIN_NIGHT_TOOLTIP = "Night";
    public static final String PROPERTY_L2BIN_NIGHT_DEFAULT =  "";



    private static final String INDENTATION_SPACES = "           ";

    // Stored Favorite Settings: flaguse
    
    public static final String PROPERTY_L2BIN_FAVORITE_SETTINGS_SECTION_KEY = PROPERTY_L2BIN_ROOT_FAV_KEY + ".flaguse.section";
    public static final String PROPERTY_L2BIN_FAVORITE_SETTINGS_SECTION_LABEL = "[ Stored Favorites: Flag Masking Parameters ]";
    public static final String PROPERTY_L2BIN_FAVORITE_SETTINGS_SECTION_TOOLTIP = "Favorites for 'flaguse'";


    public static final String PROPERTY_L2BIN_FAV1_FLAGUSE_LOAD_KEY = PROPERTY_L2BIN_ROOT_FAV_KEY + ".1.flaguse.load";
    public static final String PROPERTY_L2BIN_FAV1_FLAGUSE_LOAD_LABEL = "1. Set as Default";
    public static final String PROPERTY_L2BIN_FAV1_FLAGUSE_LOAD_TOOLTIP = "Loads settings into the 'flaguse' preference";
    public static final boolean PROPERTY_L2BIN_FAV1_FLAGUSE_LOAD_DEFAULT = false;
    
    public static final String PROPERTY_L2BIN_FAV1_FLAGUSE_DESCRIPTION_KEY = PROPERTY_L2BIN_ROOT_FAV_KEY + ".1.flaguse.description";
    public static final String PROPERTY_L2BIN_FAV1_FLAGUSE_DESCRIPTION_LABEL = INDENTATION_SPACES + "Notes/Description";
    public static final String PROPERTY_L2BIN_FAV1_FLAGUSE_DESCRIPTION_TOOLTIP = "Description/notes for stored setting of 'flaguse'";
    public static final String PROPERTY_L2BIN_FAV1_FLAGUSE_DESCRIPTION_DEFAULT = "Sample of custom flags";

    public static final String PROPERTY_L2BIN_FAV1_FLAGUSE_KEY = PROPERTY_L2BIN_ROOT_FAV_KEY + ".1.flaguse";
    public static final String PROPERTY_L2BIN_FAV1_FLAGUSE_LABEL = INDENTATION_SPACES + "flaguse";
    public static final String PROPERTY_L2BIN_FAV1_FLAGUSE_TOOLTIP = "Favorites 1: flaguse";
    public static final String PROPERTY_L2BIN_FAV1_FLAGUSE_DEFAULT = "NAVFAIL CLDICE STRAYLIGHT";


    public static final String PROPERTY_L2BIN_FAV2_FLAGUSE_LOAD_KEY = PROPERTY_L2BIN_ROOT_FAV_KEY + ".2.flaguse.load";
    public static final String PROPERTY_L2BIN_FAV2_FLAGUSE_LOAD_LABEL = "2. Set as Default";
    public static final String PROPERTY_L2BIN_FAV2_FLAGUSE_LOAD_TOOLTIP = "Loads settings into the 'flaguse' preference";
    public static final boolean PROPERTY_L2BIN_FAV2_FLAGUSE_LOAD_DEFAULT = false;

    public static final String PROPERTY_L2BIN_FAV2_FLAGUSE_DESCRIPTION_KEY = PROPERTY_L2BIN_ROOT_FAV_KEY + ".2.flaguse.description";
    public static final String PROPERTY_L2BIN_FAV2_FLAGUSE_DESCRIPTION_LABEL = INDENTATION_SPACES + "Notes/Description";
    public static final String PROPERTY_L2BIN_FAV2_FLAGUSE_DESCRIPTION_TOOLTIP = "Description/notes for stored setting of 'flaguse'";
    public static final String PROPERTY_L2BIN_FAV2_FLAGUSE_DESCRIPTION_DEFAULT = "";

    public static final String PROPERTY_L2BIN_FAV2_FLAGUSE_KEY = PROPERTY_L2BIN_ROOT_FAV_KEY + ".2.flaguse";
    public static final String PROPERTY_L2BIN_FAV2_FLAGUSE_LABEL = INDENTATION_SPACES + "flaguse";
    public static final String PROPERTY_L2BIN_FAV2_FLAGUSE_TOOLTIP = "Favorites 2: flaguse";
    public static final String PROPERTY_L2BIN_FAV2_FLAGUSE_DEFAULT = "";


    public static final String PROPERTY_L2BIN_FAV3_FLAGUSE_LOAD_KEY = PROPERTY_L2BIN_ROOT_FAV_KEY + ".3.flaguse.load";
    public static final String PROPERTY_L2BIN_FAV3_FLAGUSE_LOAD_LABEL = "3. Set as Default";
    public static final String PROPERTY_L2BIN_FAV3_FLAGUSE_LOAD_TOOLTIP = "Loads settings into the 'flaguse' preference";
    public static final boolean PROPERTY_L2BIN_FAV3_FLAGUSE_LOAD_DEFAULT = false;

    public static final String PROPERTY_L2BIN_FAV3_FLAGUSE_DESCRIPTION_KEY = PROPERTY_L2BIN_ROOT_FAV_KEY + ".3.flaguse.description";
    public static final String PROPERTY_L2BIN_FAV3_FLAGUSE_DESCRIPTION_LABEL = INDENTATION_SPACES + "Notes/Description";
    public static final String PROPERTY_L2BIN_FAV3_FLAGUSE_DESCRIPTION_TOOLTIP = "Description/notes for stored setting of 'flaguse'";
    public static final String PROPERTY_L2BIN_FAV3_FLAGUSE_DESCRIPTION_DEFAULT = "";

    public static final String PROPERTY_L2BIN_FAV3_FLAGUSE_KEY = PROPERTY_L2BIN_ROOT_FAV_KEY + ".3.flaguse";
    public static final String PROPERTY_L2BIN_FAV3_FLAGUSE_LABEL = INDENTATION_SPACES + "flaguse";
    public static final String PROPERTY_L2BIN_FAV3_FLAGUSE_TOOLTIP = "Favorites 3: flaguse";
    public static final String PROPERTY_L2BIN_FAV3_FLAGUSE_DEFAULT = "";


    public static final String PROPERTY_L2BIN_FAV4_FLAGUSE_LOAD_KEY = PROPERTY_L2BIN_ROOT_FAV_KEY + ".4.flaguse.load";
    public static final String PROPERTY_L2BIN_FAV4_FLAGUSE_LOAD_LABEL = "4. Set as Default";
    public static final String PROPERTY_L2BIN_FAV4_FLAGUSE_LOAD_TOOLTIP = "Loads settings into the 'flaguse' preference";
    public static final boolean PROPERTY_L2BIN_FAV4_FLAGUSE_LOAD_DEFAULT = false;

    public static final String PROPERTY_L2BIN_FAV4_FLAGUSE_DESCRIPTION_KEY = PROPERTY_L2BIN_ROOT_FAV_KEY + ".4.flaguse.description";
    public static final String PROPERTY_L2BIN_FAV4_FLAGUSE_DESCRIPTION_LABEL = INDENTATION_SPACES + "Notes/Description";
    public static final String PROPERTY_L2BIN_FAV4_FLAGUSE_DESCRIPTION_TOOLTIP = "Description/notes for stored setting of 'flaguse'";
    public static final String PROPERTY_L2BIN_FAV4_FLAGUSE_DESCRIPTION_DEFAULT = "";

    public static final String PROPERTY_L2BIN_FAV4_FLAGUSE_KEY = PROPERTY_L2BIN_ROOT_FAV_KEY + ".4.flaguse";
    public static final String PROPERTY_L2BIN_FAV4_FLAGUSE_LABEL = INDENTATION_SPACES + "flaguse";
    public static final String PROPERTY_L2BIN_FAV4_FLAGUSE_TOOLTIP = "Favorites 4: flaguse";
    public static final String PROPERTY_L2BIN_FAV4_FLAGUSE_DEFAULT = "";


    public static final String PROPERTY_L2BIN_FAV5_FLAGUSE_LOAD_KEY = PROPERTY_L2BIN_ROOT_FAV_KEY + ".5.flaguse.load";
    public static final String PROPERTY_L2BIN_FAV5_FLAGUSE_LOAD_LABEL = "5. Set as Default";
    public static final String PROPERTY_L2BIN_FAV5_FLAGUSE_LOAD_TOOLTIP = "Loads settings into the 'flaguse' preference";
    public static final boolean PROPERTY_L2BIN_FAV5_FLAGUSE_LOAD_DEFAULT = false;

    public static final String PROPERTY_L2BIN_FAV5_FLAGUSE_DESCRIPTION_KEY = PROPERTY_L2BIN_ROOT_FAV_KEY + ".5.flaguse.description";
    public static final String PROPERTY_L2BIN_FAV5_FLAGUSE_DESCRIPTION_LABEL = INDENTATION_SPACES + "Notes/Description";
    public static final String PROPERTY_L2BIN_FAV5_FLAGUSE_DESCRIPTION_TOOLTIP = "Description/notes for stored setting of 'flaguse'";
    public static final String PROPERTY_L2BIN_FAV5_FLAGUSE_DESCRIPTION_DEFAULT = "";

    public static final String PROPERTY_L2BIN_FAV5_FLAGUSE_KEY = PROPERTY_L2BIN_ROOT_FAV_KEY + ".5.flaguse";
    public static final String PROPERTY_L2BIN_FAV5_FLAGUSE_LABEL = INDENTATION_SPACES + "flaguse";
    public static final String PROPERTY_L2BIN_FAV5_FLAGUSE_TOOLTIP = "Favorites 5: flaguse";
    public static final String PROPERTY_L2BIN_FAV5_FLAGUSE_DEFAULT = "";




    // Stored Favorite Settings: products

    public static final String PROPERTY_L2BIN_FAVORITE_PRODUCTS_SECTION_KEY = PROPERTY_L2BIN_ROOT_FAV_KEY + ".products.section";
    public static final String PROPERTY_L2BIN_FAVORITE_PRODUCTS_SECTION_LABEL = "[ Stored Favorites: Product Parameters ]";
    public static final String PROPERTY_L2BIN_FAVORITE_PRODUCTS_SECTION_TOOLTIP = "Favorites for products and suite";

    public static final String PROPERTY_L2BIN_FAV1_PRODUCTS_LOAD_KEY = PROPERTY_L2BIN_ROOT_FAV_KEY + ".1.products.load";
    public static final String PROPERTY_L2BIN_FAV1_PRODUCTS_LOAD_LABEL = "1. Set as Default";
    public static final String PROPERTY_L2BIN_FAV1_PRODUCTS_LOAD_TOOLTIP = "Loads settings into the products preferences";
    public static final boolean PROPERTY_L2BIN_FAV1_PRODUCTS_LOAD_DEFAULT = false;
    
    public static final String PROPERTY_L2BIN_FAV1_PRODUCTS_DESCRIPTION_KEY = PROPERTY_L2BIN_ROOT_FAV_KEY + ".1.products.description";
    public static final String PROPERTY_L2BIN_FAV1_PRODUCTS_DESCRIPTION_LABEL = INDENTATION_SPACES + "Notes/Description";
    public static final String PROPERTY_L2BIN_FAV1_PRODUCTS_DESCRIPTION_TOOLTIP = "Description/notes for stored setting of products";
    public static final String PROPERTY_L2BIN_FAV1_PRODUCTS_DESCRIPTION_DEFAULT = "";
    
    public static final String PROPERTY_L2BIN_FAV1_PRODUCTS_L3BPROD_KEY = PROPERTY_L2BIN_ROOT_FAV_KEY + ".1.products.l3bprod";
    public static final String PROPERTY_L2BIN_FAV1_PRODUCTS_L3BPROD_LABEL = INDENTATION_SPACES + "l3bprod";
    public static final String PROPERTY_L2BIN_FAV1_PRODUCTS_L3BPROD_TOOLTIP = "Favorites 1: l3bprod";
    public static final String PROPERTY_L2BIN_FAV1_PRODUCTS_L3BPROD_DEFAULT = "";

    public static final String PROPERTY_L2BIN_FAV1_PRODUCTS_OUTPUT_WAVELENGTHS_KEY = PROPERTY_L2BIN_ROOT_FAV_KEY + ".1.products.output_wavelengths";
    public static final String PROPERTY_L2BIN_FAV1_PRODUCTS_OUTPUT_WAVELENGTHS_LABEL = INDENTATION_SPACES + "output_wavelengths";
    public static final String PROPERTY_L2BIN_FAV1_PRODUCTS_OUTPUT_WAVELENGTHS_TOOLTIP = "Favorites 1: output_wavelengths";
    public static final String PROPERTY_L2BIN_FAV1_PRODUCTS_OUTPUT_WAVELENGTHS_DEFAULT = "";
    
    public static final String PROPERTY_L2BIN_FAV1_PRODUCTS_SUITE_KEY = PROPERTY_L2BIN_ROOT_FAV_KEY + ".1.products.suite";
    public static final String PROPERTY_L2BIN_FAV1_PRODUCTS_SUITE_LABEL = INDENTATION_SPACES + "suite";
    public static final String PROPERTY_L2BIN_FAV1_PRODUCTS_SUITE_TOOLTIP = "Favorites 1: suite";
    public static final String PROPERTY_L2BIN_FAV1_PRODUCTS_SUITE_DEFAULT = "";




    public static final String PROPERTY_L2BIN_FAV2_PRODUCTS_LOAD_KEY = PROPERTY_L2BIN_ROOT_FAV_KEY + ".2.products.load";
    public static final String PROPERTY_L2BIN_FAV2_PRODUCTS_LOAD_LABEL = "2. Set as Default";
    public static final String PROPERTY_L2BIN_FAV2_PRODUCTS_LOAD_TOOLTIP = "Loads settings into the products preferences";
    public static final boolean PROPERTY_L2BIN_FAV2_PRODUCTS_LOAD_DEFAULT = false;

    public static final String PROPERTY_L2BIN_FAV2_PRODUCTS_DESCRIPTION_KEY = PROPERTY_L2BIN_ROOT_FAV_KEY + ".2.products.description";
    public static final String PROPERTY_L2BIN_FAV2_PRODUCTS_DESCRIPTION_LABEL = INDENTATION_SPACES + "Notes/Description";
    public static final String PROPERTY_L2BIN_FAV2_PRODUCTS_DESCRIPTION_TOOLTIP = "Description/notes for stored setting of products";
    public static final String PROPERTY_L2BIN_FAV2_PRODUCTS_DESCRIPTION_DEFAULT = "";

    public static final String PROPERTY_L2BIN_FAV2_PRODUCTS_L3BPROD_KEY = PROPERTY_L2BIN_ROOT_FAV_KEY + ".2.products.l3bprod";
    public static final String PROPERTY_L2BIN_FAV2_PRODUCTS_L3BPROD_LABEL = INDENTATION_SPACES + "l3bprod";
    public static final String PROPERTY_L2BIN_FAV2_PRODUCTS_L3BPROD_TOOLTIP = "Favorites 2: l3bprod";
    public static final String PROPERTY_L2BIN_FAV2_PRODUCTS_L3BPROD_DEFAULT = "";

    public static final String PROPERTY_L2BIN_FAV2_PRODUCTS_OUTPUT_WAVELENGTHS_KEY = PROPERTY_L2BIN_ROOT_FAV_KEY + ".2.products.output_wavelengths";
    public static final String PROPERTY_L2BIN_FAV2_PRODUCTS_OUTPUT_WAVELENGTHS_LABEL = INDENTATION_SPACES + "output_wavelengths";
    public static final String PROPERTY_L2BIN_FAV2_PRODUCTS_OUTPUT_WAVELENGTHS_TOOLTIP = "Favorites 2: output_wavelengths";
    public static final String PROPERTY_L2BIN_FAV2_PRODUCTS_OUTPUT_WAVELENGTHS_DEFAULT = "";

    public static final String PROPERTY_L2BIN_FAV2_PRODUCTS_SUITE_KEY = PROPERTY_L2BIN_ROOT_FAV_KEY + ".2.products.suite";
    public static final String PROPERTY_L2BIN_FAV2_PRODUCTS_SUITE_LABEL = INDENTATION_SPACES + "suite";
    public static final String PROPERTY_L2BIN_FAV2_PRODUCTS_SUITE_TOOLTIP = "Favorites 2: suite";
    public static final String PROPERTY_L2BIN_FAV2_PRODUCTS_SUITE_DEFAULT = "";




    public static final String PROPERTY_L2BIN_FAV3_PRODUCTS_LOAD_KEY = PROPERTY_L2BIN_ROOT_FAV_KEY + ".3.products.load";
    public static final String PROPERTY_L2BIN_FAV3_PRODUCTS_LOAD_LABEL = "3. Set as Default";
    public static final String PROPERTY_L2BIN_FAV3_PRODUCTS_LOAD_TOOLTIP = "Loads settings into the products preferences";
    public static final boolean PROPERTY_L2BIN_FAV3_PRODUCTS_LOAD_DEFAULT = false;

    public static final String PROPERTY_L2BIN_FAV3_PRODUCTS_DESCRIPTION_KEY = PROPERTY_L2BIN_ROOT_FAV_KEY + ".3.products.description";
    public static final String PROPERTY_L2BIN_FAV3_PRODUCTS_DESCRIPTION_LABEL = INDENTATION_SPACES + "Notes/Description";
    public static final String PROPERTY_L2BIN_FAV3_PRODUCTS_DESCRIPTION_TOOLTIP = "Description/notes for stored setting of products";
    public static final String PROPERTY_L2BIN_FAV3_PRODUCTS_DESCRIPTION_DEFAULT = "";

    public static final String PROPERTY_L2BIN_FAV3_PRODUCTS_L3BPROD_KEY = PROPERTY_L2BIN_ROOT_FAV_KEY + ".3.products.l3bprod";
    public static final String PROPERTY_L2BIN_FAV3_PRODUCTS_L3BPROD_LABEL = INDENTATION_SPACES + "l3bprod";
    public static final String PROPERTY_L2BIN_FAV3_PRODUCTS_L3BPROD_TOOLTIP = "Favorites 3: l3bprod";
    public static final String PROPERTY_L2BIN_FAV3_PRODUCTS_L3BPROD_DEFAULT = "";

    public static final String PROPERTY_L2BIN_FAV3_PRODUCTS_OUTPUT_WAVELENGTHS_KEY = PROPERTY_L2BIN_ROOT_FAV_KEY + ".3.products.output_wavelengths";
    public static final String PROPERTY_L2BIN_FAV3_PRODUCTS_OUTPUT_WAVELENGTHS_LABEL = INDENTATION_SPACES + "output_wavelengths";
    public static final String PROPERTY_L2BIN_FAV3_PRODUCTS_OUTPUT_WAVELENGTHS_TOOLTIP = "Favorites 3: output_wavelengths";
    public static final String PROPERTY_L2BIN_FAV3_PRODUCTS_OUTPUT_WAVELENGTHS_DEFAULT = "";

    public static final String PROPERTY_L2BIN_FAV3_PRODUCTS_SUITE_KEY = PROPERTY_L2BIN_ROOT_FAV_KEY + ".3.products.suite";
    public static final String PROPERTY_L2BIN_FAV3_PRODUCTS_SUITE_LABEL = INDENTATION_SPACES + "suite";
    public static final String PROPERTY_L2BIN_FAV3_PRODUCTS_SUITE_TOOLTIP = "Favorites 3: suite";
    public static final String PROPERTY_L2BIN_FAV3_PRODUCTS_SUITE_DEFAULT = "";



    public static final String PROPERTY_L2BIN_FAV4_PRODUCTS_LOAD_KEY = PROPERTY_L2BIN_ROOT_FAV_KEY + ".4.products.load";
    public static final String PROPERTY_L2BIN_FAV4_PRODUCTS_LOAD_LABEL = "4. Set as Default";
    public static final String PROPERTY_L2BIN_FAV4_PRODUCTS_LOAD_TOOLTIP = "Loads settings into the products preferences";
    public static final boolean PROPERTY_L2BIN_FAV4_PRODUCTS_LOAD_DEFAULT = false;

    public static final String PROPERTY_L2BIN_FAV4_PRODUCTS_DESCRIPTION_KEY = PROPERTY_L2BIN_ROOT_FAV_KEY + ".4.products.description";
    public static final String PROPERTY_L2BIN_FAV4_PRODUCTS_DESCRIPTION_LABEL = INDENTATION_SPACES + "Notes/Description";
    public static final String PROPERTY_L2BIN_FAV4_PRODUCTS_DESCRIPTION_TOOLTIP = "Description/notes for stored setting of products";
    public static final String PROPERTY_L2BIN_FAV4_PRODUCTS_DESCRIPTION_DEFAULT = "";

    public static final String PROPERTY_L2BIN_FAV4_PRODUCTS_L3BPROD_KEY = PROPERTY_L2BIN_ROOT_FAV_KEY + ".4.products.l3bprod";
    public static final String PROPERTY_L2BIN_FAV4_PRODUCTS_L3BPROD_LABEL = INDENTATION_SPACES + "l3bprod";
    public static final String PROPERTY_L2BIN_FAV4_PRODUCTS_L3BPROD_TOOLTIP = "Favorites 4: l3bprod";
    public static final String PROPERTY_L2BIN_FAV4_PRODUCTS_L3BPROD_DEFAULT = "";

    public static final String PROPERTY_L2BIN_FAV4_PRODUCTS_OUTPUT_WAVELENGTHS_KEY = PROPERTY_L2BIN_ROOT_FAV_KEY + ".4.products.output_wavelengths";
    public static final String PROPERTY_L2BIN_FAV4_PRODUCTS_OUTPUT_WAVELENGTHS_LABEL = INDENTATION_SPACES + "output_wavelengths";
    public static final String PROPERTY_L2BIN_FAV4_PRODUCTS_OUTPUT_WAVELENGTHS_TOOLTIP = "Favorites 4: output_wavelengths";
    public static final String PROPERTY_L2BIN_FAV4_PRODUCTS_OUTPUT_WAVELENGTHS_DEFAULT = "";

    public static final String PROPERTY_L2BIN_FAV4_PRODUCTS_SUITE_KEY = PROPERTY_L2BIN_ROOT_FAV_KEY + ".4.products.suite";
    public static final String PROPERTY_L2BIN_FAV4_PRODUCTS_SUITE_LABEL = INDENTATION_SPACES + "suite";
    public static final String PROPERTY_L2BIN_FAV4_PRODUCTS_SUITE_TOOLTIP = "Favorites 4: suite";
    public static final String PROPERTY_L2BIN_FAV4_PRODUCTS_SUITE_DEFAULT = "";



    public static final String PROPERTY_L2BIN_FAV5_PRODUCTS_LOAD_KEY = PROPERTY_L2BIN_ROOT_FAV_KEY + ".5.products.load";
    public static final String PROPERTY_L2BIN_FAV5_PRODUCTS_LOAD_LABEL = "5. Set as Default";
    public static final String PROPERTY_L2BIN_FAV5_PRODUCTS_LOAD_TOOLTIP = "Loads settings into the products preferences";
    public static final boolean PROPERTY_L2BIN_FAV5_PRODUCTS_LOAD_DEFAULT = false;

    public static final String PROPERTY_L2BIN_FAV5_PRODUCTS_DESCRIPTION_KEY = PROPERTY_L2BIN_ROOT_FAV_KEY + ".5.products.description";
    public static final String PROPERTY_L2BIN_FAV5_PRODUCTS_DESCRIPTION_LABEL = INDENTATION_SPACES + "Notes/Description";
    public static final String PROPERTY_L2BIN_FAV5_PRODUCTS_DESCRIPTION_TOOLTIP = "Description/notes for stored setting of products";
    public static final String PROPERTY_L2BIN_FAV5_PRODUCTS_DESCRIPTION_DEFAULT = "";

    public static final String PROPERTY_L2BIN_FAV5_PRODUCTS_L3BPROD_KEY = PROPERTY_L2BIN_ROOT_FAV_KEY + ".5.products.l3bprod";
    public static final String PROPERTY_L2BIN_FAV5_PRODUCTS_L3BPROD_LABEL = INDENTATION_SPACES + "l3bprod";
    public static final String PROPERTY_L2BIN_FAV5_PRODUCTS_L3BPROD_TOOLTIP = "Favorites 5: l3bprod";
    public static final String PROPERTY_L2BIN_FAV5_PRODUCTS_L3BPROD_DEFAULT = "";

    public static final String PROPERTY_L2BIN_FAV5_PRODUCTS_OUTPUT_WAVELENGTHS_KEY = PROPERTY_L2BIN_ROOT_FAV_KEY + ".5.products.output_wavelengths";
    public static final String PROPERTY_L2BIN_FAV5_PRODUCTS_OUTPUT_WAVELENGTHS_LABEL = INDENTATION_SPACES + "output_wavelengths";
    public static final String PROPERTY_L2BIN_FAV5_PRODUCTS_OUTPUT_WAVELENGTHS_TOOLTIP = "Favorites 5: output_wavelengths";
    public static final String PROPERTY_L2BIN_FAV5_PRODUCTS_OUTPUT_WAVELENGTHS_DEFAULT = "";

    public static final String PROPERTY_L2BIN_FAV5_PRODUCTS_SUITE_KEY = PROPERTY_L2BIN_ROOT_FAV_KEY + ".5.products.suite";
    public static final String PROPERTY_L2BIN_FAV5_PRODUCTS_SUITE_LABEL = INDENTATION_SPACES + "suite";
    public static final String PROPERTY_L2BIN_FAV5_PRODUCTS_SUITE_TOOLTIP = "Favorites 5: suite";
    public static final String PROPERTY_L2BIN_FAV5_PRODUCTS_SUITE_DEFAULT = "";
    



    // Stored Favorite Settings: geospatial

    public static final String PROPERTY_L2BIN_FAVORITE_GEOSPATIAL_SECTION_KEY = PROPERTY_L2BIN_ROOT_FAV_KEY + ".geospatial.section";
    public static final String PROPERTY_L2BIN_FAVORITE_GEOSPATIAL_SECTION_LABEL = "[ Stored Favorites: Geospatial Parameters ]";
    public static final String PROPERTY_L2BIN_FAVORITE_GEOSPATIAL_SECTION_TOOLTIP = "Favorites for spatial parameters";

    public static final String PROPERTY_L2BIN_FAV1_GEOSPATIAL_LOAD_KEY = PROPERTY_L2BIN_ROOT_FAV_KEY + ".1.geospatial.load";
    public static final String PROPERTY_L2BIN_FAV1_GEOSPATIAL_LOAD_LABEL = "1. Set as Default";
    public static final String PROPERTY_L2BIN_FAV1_GEOSPATIAL_LOAD_TOOLTIP = "Loads settings into the geospatial preferences";
    public static final boolean PROPERTY_L2BIN_FAV1_GEOSPATIAL_LOAD_DEFAULT = false;

    public static final String PROPERTY_L2BIN_FAV1_GEOSPATIAL_DESCRIPTION_KEY = PROPERTY_L2BIN_ROOT_FAV_KEY + ".1.geospatial.description";
    public static final String PROPERTY_L2BIN_FAV1_GEOSPATIAL_DESCRIPTION_LABEL = INDENTATION_SPACES + "Notes/Description";
    public static final String PROPERTY_L2BIN_FAV1_GEOSPATIAL_DESCRIPTION_TOOLTIP = "Description/notes for stored setting of geospatial";
    public static final String PROPERTY_L2BIN_FAV1_GEOSPATIAL_DESCRIPTION_DEFAULT = "";

    public static final String PROPERTY_L2BIN_FAV1_GEOSPATIAL_RESOLUTION_KEY = PROPERTY_L2BIN_ROOT_FAV_KEY + ".1.geospatial.resolution";
    public static final String PROPERTY_L2BIN_FAV1_GEOSPATIAL_RESOLUTION_LABEL = INDENTATION_SPACES + "resolution";
    public static final String PROPERTY_L2BIN_FAV1_GEOSPATIAL_RESOLUTION_TOOLTIP = "Favorites 1: resolution";
    public static final String PROPERTY_L2BIN_FAV1_GEOSPATIAL_RESOLUTION_DEFAULT = "";

    public static final String PROPERTY_L2BIN_FAV1_GEOSPATIAL_NSWE_KEY = PROPERTY_L2BIN_ROOT_FAV_KEY + ".1.geospatial.nswe";
    public static final String PROPERTY_L2BIN_FAV1_GEOSPATIAL_NSWE_LABEL = INDENTATION_SPACES + "N,S,W,E Bounds";
    public static final String PROPERTY_L2BIN_FAV1_GEOSPATIAL_NSWE_TOOLTIP = "Geographic boundaries.  Comma delimited. Format N,S,W,E. ";
    public static final String PROPERTY_L2BIN_FAV1_GEOSPATIAL_NSWE_DEFAULT = "";



    public static final String PROPERTY_L2BIN_FAV2_GEOSPATIAL_LOAD_KEY = PROPERTY_L2BIN_ROOT_FAV_KEY + ".2.geospatial.load";
    public static final String PROPERTY_L2BIN_FAV2_GEOSPATIAL_LOAD_LABEL = "2. Set as Default";
    public static final String PROPERTY_L2BIN_FAV2_GEOSPATIAL_LOAD_TOOLTIP = "Loads settings into the geospatial preferences";
    public static final boolean PROPERTY_L2BIN_FAV2_GEOSPATIAL_LOAD_DEFAULT = false;

    public static final String PROPERTY_L2BIN_FAV2_GEOSPATIAL_DESCRIPTION_KEY = PROPERTY_L2BIN_ROOT_FAV_KEY + ".2.geospatial.description";
    public static final String PROPERTY_L2BIN_FAV2_GEOSPATIAL_DESCRIPTION_LABEL = INDENTATION_SPACES + "Notes/Description";
    public static final String PROPERTY_L2BIN_FAV2_GEOSPATIAL_DESCRIPTION_TOOLTIP = "Description/notes for stored setting of geospatial";
    public static final String PROPERTY_L2BIN_FAV2_GEOSPATIAL_DESCRIPTION_DEFAULT = "";

    public static final String PROPERTY_L2BIN_FAV2_GEOSPATIAL_RESOLUTION_KEY = PROPERTY_L2BIN_ROOT_FAV_KEY + ".2.geospatial.resolution";
    public static final String PROPERTY_L2BIN_FAV2_GEOSPATIAL_RESOLUTION_LABEL = INDENTATION_SPACES + "resolution";
    public static final String PROPERTY_L2BIN_FAV2_GEOSPATIAL_RESOLUTION_TOOLTIP = "Favorites 2: resolution";
    public static final String PROPERTY_L2BIN_FAV2_GEOSPATIAL_RESOLUTION_DEFAULT = "";

    public static final String PROPERTY_L2BIN_FAV2_GEOSPATIAL_NSWE_KEY = PROPERTY_L2BIN_ROOT_FAV_KEY + ".2.geospatial.nswe";
    public static final String PROPERTY_L2BIN_FAV2_GEOSPATIAL_NSWE_LABEL = INDENTATION_SPACES + "N,S,W,E Bounds";
    public static final String PROPERTY_L2BIN_FAV2_GEOSPATIAL_NSWE_TOOLTIP = "Geographic boundaries.  Comma delimited. Format N,S,W,E. ";
    public static final String PROPERTY_L2BIN_FAV2_GEOSPATIAL_NSWE_DEFAULT = "";



    public static final String PROPERTY_L2BIN_FAV3_GEOSPATIAL_LOAD_KEY = PROPERTY_L2BIN_ROOT_FAV_KEY + ".3.geospatial.load";
    public static final String PROPERTY_L2BIN_FAV3_GEOSPATIAL_LOAD_LABEL = "3. Set as Default";
    public static final String PROPERTY_L2BIN_FAV3_GEOSPATIAL_LOAD_TOOLTIP = "Loads settings into the geospatial preferences";
    public static final boolean PROPERTY_L2BIN_FAV3_GEOSPATIAL_LOAD_DEFAULT = false;

    public static final String PROPERTY_L2BIN_FAV3_GEOSPATIAL_DESCRIPTION_KEY = PROPERTY_L2BIN_ROOT_FAV_KEY + ".3.geospatial.description";
    public static final String PROPERTY_L2BIN_FAV3_GEOSPATIAL_DESCRIPTION_LABEL = INDENTATION_SPACES + "Notes/Description";
    public static final String PROPERTY_L2BIN_FAV3_GEOSPATIAL_DESCRIPTION_TOOLTIP = "Description/notes for stored setting of geospatial";
    public static final String PROPERTY_L2BIN_FAV3_GEOSPATIAL_DESCRIPTION_DEFAULT = "";

    public static final String PROPERTY_L2BIN_FAV3_GEOSPATIAL_RESOLUTION_KEY = PROPERTY_L2BIN_ROOT_FAV_KEY + ".3.geospatial.resolution";
    public static final String PROPERTY_L2BIN_FAV3_GEOSPATIAL_RESOLUTION_LABEL = INDENTATION_SPACES + "resolution";
    public static final String PROPERTY_L2BIN_FAV3_GEOSPATIAL_RESOLUTION_TOOLTIP = "Favorites 3: resolution";
    public static final String PROPERTY_L2BIN_FAV3_GEOSPATIAL_RESOLUTION_DEFAULT = "";

    public static final String PROPERTY_L2BIN_FAV3_GEOSPATIAL_NSWE_KEY = PROPERTY_L2BIN_ROOT_FAV_KEY + ".3.geospatial.nswe";
    public static final String PROPERTY_L2BIN_FAV3_GEOSPATIAL_NSWE_LABEL = INDENTATION_SPACES + "N,S,W,E Bounds";
    public static final String PROPERTY_L2BIN_FAV3_GEOSPATIAL_NSWE_TOOLTIP = "Geographic boundaries.  Comma delimited. Format N,S,W,E. ";
    public static final String PROPERTY_L2BIN_FAV3_GEOSPATIAL_NSWE_DEFAULT = "";




    public static final String PROPERTY_L2BIN_FAV4_GEOSPATIAL_LOAD_KEY = PROPERTY_L2BIN_ROOT_FAV_KEY + ".4.geospatial.load";
    public static final String PROPERTY_L2BIN_FAV4_GEOSPATIAL_LOAD_LABEL = "4. Set as Default";
    public static final String PROPERTY_L2BIN_FAV4_GEOSPATIAL_LOAD_TOOLTIP = "Loads settings into the geospatial preferences";
    public static final boolean PROPERTY_L2BIN_FAV4_GEOSPATIAL_LOAD_DEFAULT = false;

    public static final String PROPERTY_L2BIN_FAV4_GEOSPATIAL_DESCRIPTION_KEY = PROPERTY_L2BIN_ROOT_FAV_KEY + ".4.geospatial.description";
    public static final String PROPERTY_L2BIN_FAV4_GEOSPATIAL_DESCRIPTION_LABEL = INDENTATION_SPACES + "Notes/Description";
    public static final String PROPERTY_L2BIN_FAV4_GEOSPATIAL_DESCRIPTION_TOOLTIP = "Description/notes for stored setting of geospatial";
    public static final String PROPERTY_L2BIN_FAV4_GEOSPATIAL_DESCRIPTION_DEFAULT = "";

    public static final String PROPERTY_L2BIN_FAV4_GEOSPATIAL_RESOLUTION_KEY = PROPERTY_L2BIN_ROOT_FAV_KEY + ".4.geospatial.resolution";
    public static final String PROPERTY_L2BIN_FAV4_GEOSPATIAL_RESOLUTION_LABEL = INDENTATION_SPACES + "resolution";
    public static final String PROPERTY_L2BIN_FAV4_GEOSPATIAL_RESOLUTION_TOOLTIP = "Favorites 4: resolution";
    public static final String PROPERTY_L2BIN_FAV4_GEOSPATIAL_RESOLUTION_DEFAULT = "";

    public static final String PROPERTY_L2BIN_FAV4_GEOSPATIAL_NSWE_KEY = PROPERTY_L2BIN_ROOT_FAV_KEY + ".4.geospatial.nswe";
    public static final String PROPERTY_L2BIN_FAV4_GEOSPATIAL_NSWE_LABEL = INDENTATION_SPACES + "N,S,W,E Bounds";
    public static final String PROPERTY_L2BIN_FAV4_GEOSPATIAL_NSWE_TOOLTIP = "Geographic boundaries.  Comma delimited. Format N,S,W,E. ";
    public static final String PROPERTY_L2BIN_FAV4_GEOSPATIAL_NSWE_DEFAULT = "";





    public static final String PROPERTY_L2BIN_FAV5_GEOSPATIAL_LOAD_KEY = PROPERTY_L2BIN_ROOT_FAV_KEY + ".5.geospatial.load";
    public static final String PROPERTY_L2BIN_FAV5_GEOSPATIAL_LOAD_LABEL = "5. Set as Default";
    public static final String PROPERTY_L2BIN_FAV5_GEOSPATIAL_LOAD_TOOLTIP = "Loads settings into the geospatial preferences";
    public static final boolean PROPERTY_L2BIN_FAV5_GEOSPATIAL_LOAD_DEFAULT = false;

    public static final String PROPERTY_L2BIN_FAV5_GEOSPATIAL_DESCRIPTION_KEY = PROPERTY_L2BIN_ROOT_FAV_KEY + ".5.geospatial.description";
    public static final String PROPERTY_L2BIN_FAV5_GEOSPATIAL_DESCRIPTION_LABEL = INDENTATION_SPACES + "Notes/Description";
    public static final String PROPERTY_L2BIN_FAV5_GEOSPATIAL_DESCRIPTION_TOOLTIP = "Description/notes for stored setting of geospatial";
    public static final String PROPERTY_L2BIN_FAV5_GEOSPATIAL_DESCRIPTION_DEFAULT = "";

    public static final String PROPERTY_L2BIN_FAV5_GEOSPATIAL_RESOLUTION_KEY = PROPERTY_L2BIN_ROOT_FAV_KEY + ".5.geospatial.resolution";
    public static final String PROPERTY_L2BIN_FAV5_GEOSPATIAL_RESOLUTION_LABEL = INDENTATION_SPACES + "resolution";
    public static final String PROPERTY_L2BIN_FAV5_GEOSPATIAL_RESOLUTION_TOOLTIP = "Favorites 5: resolution";
    public static final String PROPERTY_L2BIN_FAV5_GEOSPATIAL_RESOLUTION_DEFAULT = "";

    public static final String PROPERTY_L2BIN_FAV5_GEOSPATIAL_NSWE_KEY = PROPERTY_L2BIN_ROOT_FAV_KEY + ".5.geospatial.nswe";
    public static final String PROPERTY_L2BIN_FAV5_GEOSPATIAL_NSWE_LABEL = INDENTATION_SPACES + "N,S,W,E Bounds";
    public static final String PROPERTY_L2BIN_FAV5_GEOSPATIAL_NSWE_TOOLTIP = "Geographic boundaries.  Comma delimited. Format N,S,W,E. ";
    public static final String PROPERTY_L2BIN_FAV5_GEOSPATIAL_NSWE_DEFAULT = "";





    // Property Setting: Restore Defaults

    private static final String PROPERTY_RESTORE_KEY_SUFFIX = PROPERTY_L2BIN_ROOT_KEY + ".restore.defaults";

    public static final String PROPERTY_RESTORE_SECTION_KEY = PROPERTY_RESTORE_KEY_SUFFIX + ".section";
    public static final String PROPERTY_RESTORE_SECTION_LABEL = "Restore";
    public static final String PROPERTY_RESTORE_SECTION_TOOLTIP = "Restores preferences to the package defaults";

    public static final String PROPERTY_RESTORE_DEFAULTS_KEY = PROPERTY_RESTORE_KEY_SUFFIX + ".apply";
    public static final String PROPERTY_RESTORE_DEFAULTS_LABEL = "Default (L2bin Preferences)";
    public static final String PROPERTY_RESTORE_DEFAULTS_TOOLTIP = "Restore all l2bin preferences to the original default";
    public static final boolean PROPERTY_RESTORE_DEFAULTS_DEFAULT = false;




    protected PropertySet createPropertySet() {
        return createPropertySet(new SeadasToolboxBean());
    }



    @Override
    protected JPanel createPanel(BindingContext context) {


        flaguseAutofill = initPropertyDefaults(context, PROPERTY_L2BIN_FLAGUSE_AUTOFILL_KEY, PROPERTY_L2BIN_FLAGUSE_AUTOFILL_DEFAULT);
        l3bprodAutofill = initPropertyDefaults(context, PROPERTY_L2BIN_L3BPROD_AUTOFILL_KEY, PROPERTY_L2BIN_L3BPROD_AUTOFILL_DEFAULT);
        autoFillOther = initPropertyDefaults(context, PROPERTY_L2BIN_AUTOFILL_KEY, PROPERTY_L2BIN_AUTOFILL_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L2BIN_AUTOFILL_PRECEDENCE_KEY, PROPERTY_L2BIN_AUTOFILL_PRECEDENCE_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L2BIN_AUTOFILL_PRECEDENCE_NULL_SUITE_KEY, PROPERTY_L2BIN_AUTOFILL_PRECEDENCE_NULL_SUITE_DEFAULT);
        

        initPropertyDefaults(context, PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SECTION_KEY, true);
        namingScheme = initPropertyDefaults(context, PROPERTY_L2BIN_OFILE_NAMING_SCHEME_KEY, PROPERTY_L2BIN_OFILE_NAMING_SCHEME_DEFAULT);
        fieldsAdd = initPropertyDefaults(context, PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SUFFIX_OPTIONS_KEY, PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SUFFIX_OPTIONS_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SUFFIX1_KEY, PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SUFFIX1_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SUFFIX2_KEY, PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SUFFIX2_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L2BIN_OFILE_NAMING_SCHEME_IFILE_REPLACE_KEY, PROPERTY_L2BIN_OFILE_NAMING_SCHEME_IFILE_REPLACE_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L2BIN_OFILE_NAMING_SCHEME_IFILE_ORIGINAL_KEY, PROPERTY_L2BIN_OFILE_NAMING_SCHEME_IFILE_ORIGINAL_KEY);
        
        //
        // Initialize the default value contained within each property descriptor
        // This is done so subsequently the restoreDefaults actions can be performed
        //

        initPropertyDefaults(context, PROPERTY_L2BIN_PARAMETERS_SECTION_KEY, true);
        l3bprod = initPropertyDefaults(context, PROPERTY_L2BIN_L3BPROD_KEY, PROPERTY_L2BIN_L3BPROD_DEFAULT);
        output_wavelengths = initPropertyDefaults(context, PROPERTY_L2BIN_OUTPUT_WAVELENGTHS_KEY, PROPERTY_L2BIN_OUTPUT_WAVELENGTHS_DEFAULT);
        suite = initPropertyDefaults(context, PROPERTY_L2BIN_SUITE_KEY, PROPERTY_L2BIN_SUITE_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L2BIN_PRODTYPE_KEY, PROPERTY_L2BIN_PRODTYPE_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L2BIN_ROW_GROUP_KEY, PROPERTY_L2BIN_ROW_GROUP_DEFAULT);

        
        initPropertyDefaults(context, PROPERTY_L2BIN_PARAMETERS_FLAGUSE_SECTION_KEY, true);
        flaguse = initPropertyDefaults(context, PROPERTY_L2BIN_FLAGUSE_KEY, PROPERTY_L2BIN_FLAGUSE_DEFAULT);
        flaguse_notes = initPropertyDefaults(context, PROPERTY_L2BIN_FLAGUSE_NOTES_KEY, PROPERTY_L2BIN_FLAGUSE_NOTES_DEFAULT);

        
        initPropertyDefaults(context, PROPERTY_L2BIN_PARAMETERS_GEOSPATIAL_SECTION_KEY, true);
        north = initPropertyDefaults(context, PROPERTY_L2BIN_LATNORTH_KEY, PROPERTY_L2BIN_LATNORTH_DEFAULT);
        south = initPropertyDefaults(context, PROPERTY_L2BIN_LATSOUTH_KEY, PROPERTY_L2BIN_LATSOUTH_DEFAULT);
        west = initPropertyDefaults(context, PROPERTY_L2BIN_LONWEST_KEY, PROPERTY_L2BIN_LONWEST_DEFAULT);
        east = initPropertyDefaults(context, PROPERTY_L2BIN_LONEAST_KEY, PROPERTY_L2BIN_LONEAST_DEFAULT);
        resolution = initPropertyDefaults(context, PROPERTY_L2BIN_RESOLUTION_KEY, PROPERTY_L2BIN_RESOLUTION_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L2BIN_AREA_WEIGHTING_KEY, PROPERTY_L2BIN_AREA_WEIGHTING_DEFAULT);

        initPropertyDefaults(context, PROPERTY_L2BIN_PARAMETERS_BINNING_SECTION_KEY, true);
        initPropertyDefaults(context, PROPERTY_L2BIN_COMPOSITE_PROD_KEY, PROPERTY_L2BIN_COMPOSITE_PROD_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L2BIN_COMPOSITE_SCHEME_KEY, PROPERTY_L2BIN_COMPOSITE_SCHEME_DEFAULT);


        initPropertyDefaults(context, PROPERTY_L2BIN_PARAMETERS_TEMPORAL_SECTION_KEY, true);
        initPropertyDefaults(context, PROPERTY_L2BIN_SDAY_KEY, PROPERTY_L2BIN_SDAY_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L2BIN_EDAY_KEY, PROPERTY_L2BIN_EDAY_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L2BIN_DELTA_CROSS_KEY, PROPERTY_L2BIN_DELTA_CROSS_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L2BIN_NIGHT_KEY, PROPERTY_L2BIN_NIGHT_DEFAULT);

        


        favoriteFlaguseSection = initPropertyDefaults(context, PROPERTY_L2BIN_FAVORITE_SETTINGS_SECTION_KEY, true);

        fav1SetToDefault = initPropertyDefaults(context, PROPERTY_L2BIN_FAV1_FLAGUSE_LOAD_KEY, PROPERTY_L2BIN_FAV1_FLAGUSE_LOAD_DEFAULT);
        fav1FlagUseNotes = initPropertyDefaults(context, PROPERTY_L2BIN_FAV1_FLAGUSE_DESCRIPTION_KEY, PROPERTY_L2BIN_FAV1_FLAGUSE_DESCRIPTION_DEFAULT);
        fav1FlagUse = initPropertyDefaults(context, PROPERTY_L2BIN_FAV1_FLAGUSE_KEY, PROPERTY_L2BIN_FAV1_FLAGUSE_DEFAULT);

        fav2SetToDefault = initPropertyDefaults(context, PROPERTY_L2BIN_FAV2_FLAGUSE_LOAD_KEY, PROPERTY_L2BIN_FAV2_FLAGUSE_LOAD_DEFAULT);
        fav2FlagUseNotes = initPropertyDefaults(context, PROPERTY_L2BIN_FAV2_FLAGUSE_DESCRIPTION_KEY, PROPERTY_L2BIN_FAV2_FLAGUSE_DESCRIPTION_DEFAULT);
        fav2FlagUse = initPropertyDefaults(context, PROPERTY_L2BIN_FAV2_FLAGUSE_KEY, PROPERTY_L2BIN_FAV2_FLAGUSE_DEFAULT);

        fav3SetToDefault = initPropertyDefaults(context, PROPERTY_L2BIN_FAV3_FLAGUSE_LOAD_KEY, PROPERTY_L2BIN_FAV3_FLAGUSE_LOAD_DEFAULT);
        fav3FlagUseNotes =  initPropertyDefaults(context, PROPERTY_L2BIN_FAV3_FLAGUSE_DESCRIPTION_KEY, PROPERTY_L2BIN_FAV3_FLAGUSE_DESCRIPTION_DEFAULT);
        fav3FlagUse = initPropertyDefaults(context, PROPERTY_L2BIN_FAV3_FLAGUSE_KEY, PROPERTY_L2BIN_FAV3_FLAGUSE_DEFAULT);

        fav4SetToDefault = initPropertyDefaults(context, PROPERTY_L2BIN_FAV4_FLAGUSE_LOAD_KEY, PROPERTY_L2BIN_FAV4_FLAGUSE_LOAD_DEFAULT);
        fav4FlagUseNotes =  initPropertyDefaults(context, PROPERTY_L2BIN_FAV4_FLAGUSE_DESCRIPTION_KEY, PROPERTY_L2BIN_FAV4_FLAGUSE_DESCRIPTION_DEFAULT);
        fav4FlagUse = initPropertyDefaults(context, PROPERTY_L2BIN_FAV4_FLAGUSE_KEY, PROPERTY_L2BIN_FAV4_FLAGUSE_DEFAULT);

        fav5SetToDefault = initPropertyDefaults(context, PROPERTY_L2BIN_FAV5_FLAGUSE_LOAD_KEY, PROPERTY_L2BIN_FAV5_FLAGUSE_LOAD_DEFAULT);
        fav5FlagUseNotes =  initPropertyDefaults(context, PROPERTY_L2BIN_FAV5_FLAGUSE_DESCRIPTION_KEY, PROPERTY_L2BIN_FAV5_FLAGUSE_DESCRIPTION_DEFAULT);
        fav5FlagUse = initPropertyDefaults(context, PROPERTY_L2BIN_FAV5_FLAGUSE_KEY, PROPERTY_L2BIN_FAV5_FLAGUSE_DEFAULT);



        favoriteProductsSection = initPropertyDefaults(context, PROPERTY_L2BIN_FAVORITE_PRODUCTS_SECTION_KEY, true);
        fav1Products_SetToDefault = initPropertyDefaults(context, PROPERTY_L2BIN_FAV1_PRODUCTS_LOAD_KEY, PROPERTY_L2BIN_FAV1_PRODUCTS_LOAD_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L2BIN_FAV1_PRODUCTS_DESCRIPTION_KEY, PROPERTY_L2BIN_FAV1_PRODUCTS_DESCRIPTION_DEFAULT);
        fav1Products_l3bprod = initPropertyDefaults(context, PROPERTY_L2BIN_FAV1_PRODUCTS_L3BPROD_KEY, PROPERTY_L2BIN_FAV1_PRODUCTS_L3BPROD_DEFAULT);
        fav1Products_output_wavelengths = initPropertyDefaults(context, PROPERTY_L2BIN_FAV1_PRODUCTS_OUTPUT_WAVELENGTHS_KEY, PROPERTY_L2BIN_FAV1_PRODUCTS_OUTPUT_WAVELENGTHS_DEFAULT);
        fav1Products_suite = initPropertyDefaults(context, PROPERTY_L2BIN_FAV1_PRODUCTS_SUITE_KEY, PROPERTY_L2BIN_FAV1_PRODUCTS_SUITE_DEFAULT);


        fav2Products_SetToDefault = initPropertyDefaults(context, PROPERTY_L2BIN_FAV2_PRODUCTS_LOAD_KEY, PROPERTY_L2BIN_FAV2_PRODUCTS_LOAD_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L2BIN_FAV2_PRODUCTS_DESCRIPTION_KEY, PROPERTY_L2BIN_FAV2_PRODUCTS_DESCRIPTION_DEFAULT);
        fav2Products_l3bprod = initPropertyDefaults(context, PROPERTY_L2BIN_FAV2_PRODUCTS_L3BPROD_KEY, PROPERTY_L2BIN_FAV2_PRODUCTS_L3BPROD_DEFAULT);
        fav2Products_output_wavelengths = initPropertyDefaults(context, PROPERTY_L2BIN_FAV2_PRODUCTS_OUTPUT_WAVELENGTHS_KEY, PROPERTY_L2BIN_FAV2_PRODUCTS_OUTPUT_WAVELENGTHS_DEFAULT);
        fav2Products_suite = initPropertyDefaults(context, PROPERTY_L2BIN_FAV2_PRODUCTS_SUITE_KEY, PROPERTY_L2BIN_FAV2_PRODUCTS_SUITE_DEFAULT);



        fav3Products_SetToDefault = initPropertyDefaults(context, PROPERTY_L2BIN_FAV3_PRODUCTS_LOAD_KEY, PROPERTY_L2BIN_FAV3_PRODUCTS_LOAD_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L2BIN_FAV3_PRODUCTS_DESCRIPTION_KEY, PROPERTY_L2BIN_FAV3_PRODUCTS_DESCRIPTION_DEFAULT);
        fav3Products_l3bprod = initPropertyDefaults(context, PROPERTY_L2BIN_FAV3_PRODUCTS_L3BPROD_KEY, PROPERTY_L2BIN_FAV3_PRODUCTS_L3BPROD_DEFAULT);
        fav3Products_output_wavelengths = initPropertyDefaults(context, PROPERTY_L2BIN_FAV3_PRODUCTS_OUTPUT_WAVELENGTHS_KEY, PROPERTY_L2BIN_FAV3_PRODUCTS_OUTPUT_WAVELENGTHS_DEFAULT);
        fav3Products_suite = initPropertyDefaults(context, PROPERTY_L2BIN_FAV3_PRODUCTS_SUITE_KEY, PROPERTY_L2BIN_FAV3_PRODUCTS_SUITE_DEFAULT);



        fav4Products_SetToDefault = initPropertyDefaults(context, PROPERTY_L2BIN_FAV4_PRODUCTS_LOAD_KEY, PROPERTY_L2BIN_FAV4_PRODUCTS_LOAD_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L2BIN_FAV4_PRODUCTS_DESCRIPTION_KEY, PROPERTY_L2BIN_FAV4_PRODUCTS_DESCRIPTION_DEFAULT);
        fav4Products_l3bprod = initPropertyDefaults(context, PROPERTY_L2BIN_FAV4_PRODUCTS_L3BPROD_KEY, PROPERTY_L2BIN_FAV4_PRODUCTS_L3BPROD_DEFAULT);
        fav4Products_output_wavelengths = initPropertyDefaults(context, PROPERTY_L2BIN_FAV4_PRODUCTS_OUTPUT_WAVELENGTHS_KEY, PROPERTY_L2BIN_FAV4_PRODUCTS_OUTPUT_WAVELENGTHS_DEFAULT);
        fav4Products_suite = initPropertyDefaults(context, PROPERTY_L2BIN_FAV4_PRODUCTS_SUITE_KEY, PROPERTY_L2BIN_FAV4_PRODUCTS_SUITE_DEFAULT);



        fav5Products_SetToDefault = initPropertyDefaults(context, PROPERTY_L2BIN_FAV5_PRODUCTS_LOAD_KEY, PROPERTY_L2BIN_FAV5_PRODUCTS_LOAD_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L2BIN_FAV5_PRODUCTS_DESCRIPTION_KEY, PROPERTY_L2BIN_FAV5_PRODUCTS_DESCRIPTION_DEFAULT);
        fav5Products_l3bprod = initPropertyDefaults(context, PROPERTY_L2BIN_FAV5_PRODUCTS_L3BPROD_KEY, PROPERTY_L2BIN_FAV5_PRODUCTS_L3BPROD_DEFAULT);
        fav5Products_output_wavelengths = initPropertyDefaults(context, PROPERTY_L2BIN_FAV5_PRODUCTS_OUTPUT_WAVELENGTHS_KEY, PROPERTY_L2BIN_FAV5_PRODUCTS_OUTPUT_WAVELENGTHS_DEFAULT);
        fav5Products_suite = initPropertyDefaults(context, PROPERTY_L2BIN_FAV5_PRODUCTS_SUITE_KEY, PROPERTY_L2BIN_FAV5_PRODUCTS_SUITE_DEFAULT);




        favoriteGeospatial_Section = initPropertyDefaults(context, PROPERTY_L2BIN_FAVORITE_GEOSPATIAL_SECTION_KEY, true);
        fav1Geospatial_SetToDefault = initPropertyDefaults(context, PROPERTY_L2BIN_FAV1_GEOSPATIAL_LOAD_KEY, PROPERTY_L2BIN_FAV1_GEOSPATIAL_LOAD_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L2BIN_FAV1_GEOSPATIAL_DESCRIPTION_KEY, PROPERTY_L2BIN_FAV1_GEOSPATIAL_DESCRIPTION_DEFAULT);
        fav1Geospatial_NSWE = initPropertyDefaults(context, PROPERTY_L2BIN_FAV1_GEOSPATIAL_NSWE_KEY, PROPERTY_L2BIN_FAV1_GEOSPATIAL_NSWE_DEFAULT);
        fav1Geospatial_resolution = initPropertyDefaults(context, PROPERTY_L2BIN_FAV1_GEOSPATIAL_RESOLUTION_KEY, PROPERTY_L2BIN_FAV1_GEOSPATIAL_RESOLUTION_DEFAULT);

        fav2Geospatial_SetToDefault = initPropertyDefaults(context, PROPERTY_L2BIN_FAV2_GEOSPATIAL_LOAD_KEY, PROPERTY_L2BIN_FAV2_GEOSPATIAL_LOAD_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L2BIN_FAV2_GEOSPATIAL_DESCRIPTION_KEY, PROPERTY_L2BIN_FAV2_GEOSPATIAL_DESCRIPTION_DEFAULT);
        fav2Geospatial_NSWE = initPropertyDefaults(context, PROPERTY_L2BIN_FAV2_GEOSPATIAL_NSWE_KEY, PROPERTY_L2BIN_FAV2_GEOSPATIAL_NSWE_DEFAULT);
        fav2Geospatial_resolution = initPropertyDefaults(context, PROPERTY_L2BIN_FAV2_GEOSPATIAL_RESOLUTION_KEY, PROPERTY_L2BIN_FAV2_GEOSPATIAL_RESOLUTION_DEFAULT);


        fav3Geospatial_SetToDefault = initPropertyDefaults(context, PROPERTY_L2BIN_FAV3_GEOSPATIAL_LOAD_KEY, PROPERTY_L2BIN_FAV3_GEOSPATIAL_LOAD_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L2BIN_FAV3_GEOSPATIAL_DESCRIPTION_KEY, PROPERTY_L2BIN_FAV3_GEOSPATIAL_DESCRIPTION_DEFAULT);
        fav3Geospatial_NSWE = initPropertyDefaults(context, PROPERTY_L2BIN_FAV3_GEOSPATIAL_NSWE_KEY, PROPERTY_L2BIN_FAV3_GEOSPATIAL_NSWE_DEFAULT);
        fav3Geospatial_resolution = initPropertyDefaults(context, PROPERTY_L2BIN_FAV3_GEOSPATIAL_RESOLUTION_KEY, PROPERTY_L2BIN_FAV3_GEOSPATIAL_RESOLUTION_DEFAULT);


        fav4Geospatial_SetToDefault = initPropertyDefaults(context, PROPERTY_L2BIN_FAV4_GEOSPATIAL_LOAD_KEY, PROPERTY_L2BIN_FAV4_GEOSPATIAL_LOAD_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L2BIN_FAV4_GEOSPATIAL_DESCRIPTION_KEY, PROPERTY_L2BIN_FAV4_GEOSPATIAL_DESCRIPTION_DEFAULT);
        fav4Geospatial_NSWE = initPropertyDefaults(context, PROPERTY_L2BIN_FAV4_GEOSPATIAL_NSWE_KEY, PROPERTY_L2BIN_FAV4_GEOSPATIAL_NSWE_DEFAULT);
        fav4Geospatial_resolution = initPropertyDefaults(context, PROPERTY_L2BIN_FAV4_GEOSPATIAL_RESOLUTION_KEY, PROPERTY_L2BIN_FAV4_GEOSPATIAL_RESOLUTION_DEFAULT);


        fav5Geospatial_SetToDefault = initPropertyDefaults(context, PROPERTY_L2BIN_FAV5_GEOSPATIAL_LOAD_KEY, PROPERTY_L2BIN_FAV5_GEOSPATIAL_LOAD_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L2BIN_FAV5_GEOSPATIAL_DESCRIPTION_KEY, PROPERTY_L2BIN_FAV5_GEOSPATIAL_DESCRIPTION_DEFAULT);
        fav5Geospatial_NSWE = initPropertyDefaults(context, PROPERTY_L2BIN_FAV5_GEOSPATIAL_NSWE_KEY, PROPERTY_L2BIN_FAV5_GEOSPATIAL_NSWE_DEFAULT);
        fav5Geospatial_resolution = initPropertyDefaults(context, PROPERTY_L2BIN_FAV5_GEOSPATIAL_RESOLUTION_KEY, PROPERTY_L2BIN_FAV5_GEOSPATIAL_RESOLUTION_DEFAULT);





        initPropertyDefaults(context, PROPERTY_RESTORE_SECTION_KEY, true);
        restoreDefaults =  initPropertyDefaults(context, PROPERTY_RESTORE_DEFAULTS_KEY, PROPERTY_RESTORE_DEFAULTS_DEFAULT);



        //
        // Create UI
        //

        PropertyEditorRegistry registry = PropertyEditorRegistry.getInstance();

        PropertySet propertyContainer = context.getPropertySet();
        Property[] properties = propertyContainer.getProperties();

        TableLayout tableLayout = new TableLayout(2);
        tableLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        tableLayout.setTablePadding(new Insets(4, 10, 0, 0));
        tableLayout.setTableFill(TableLayout.Fill.BOTH);
        tableLayout.setColumnWeightX(1, 1.0);


        JPanel pageUI = new JPanel(tableLayout);

        int currRow = 0;
        for (Property property : properties) {
            PropertyDescriptor descriptor = property.getDescriptor();
                PropertyPane.addComponent(currRow, tableLayout, pageUI, context, registry, descriptor);
                currRow++;
        }

        pageUI.add(tableLayout.createVerticalSpacer());

        JPanel parent = new JPanel(new BorderLayout());
        parent.add(pageUI, BorderLayout.CENTER);
        parent.add(Box.createHorizontalStrut(50), BorderLayout.EAST);


        // todo tried tabs but performance very slow

//        JPanel pageUI = new JPanel(tableLayout);
//        JPanel pageUIFav = new JPanel(tableLayout);
//
//        int currRow = 0;
//        int currRowFav = 0;
//        for (Property property : properties) {
//            PropertyDescriptor descriptor = property.getDescriptor();
//            if (property.getName().contains(PROPERTY_L2BIN_ROOT_FAV_KEY)) {
//                PropertyPane.addComponent(currRowFav, tableLayout, pageUIFav, context, registry, descriptor);
//                currRowFav++;
//            } else {
//                PropertyPane.addComponent(currRow, tableLayout, pageUI, context, registry, descriptor);
//                currRow++;
//            }
//        }
//
//        pageUI.add(tableLayout.createVerticalSpacer());
//        pageUIFav.add(tableLayout.createVerticalSpacer());
//
//        JTabbedPane tabbedPane = new JTabbedPane();
//        tabbedPane.addTab("L2Bin Preferences", null, pageUI,
//                "Primary fields");
//
//        tabbedPane.addTab("Stored Favorites", null, pageUIFav,
//                "Stored favorites");
//        JPanel parent = new JPanel(new BorderLayout());
//        parent.add(tabbedPane, BorderLayout.CENTER);


        return parent;
    }


    @Override
    protected void configure(BindingContext context) {

        // Handle resetDefaults events - set all other components to defaults
        restoreDefaults.addPropertyChangeListener(evt -> {
            handleRestoreDefaults(context);
        });

//        flaguseAutofill.addPropertyChangeListener(evt -> {
//            final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
//            if (flaguse.getValue() != null && flaguse.getValue().toString().trim().length() > 0) {
//                try {
//                    boolean boolFalse = false;
//                    flaguseAutofill.setValue(boolFalse);
//                } catch (ValidationException e) {
//                    e.printStackTrace();
//                }
//                context.setComponentsEnabled(PROPERTY_L2BIN_FLAGUSE_AUTOFILL_KEY, false);
//            } else {
//                try {
//                    boolean boolTrue = true;
//                    flaguseAutofill.setValue(boolTrue);
//                } catch (ValidationException e) {
//                    e.printStackTrace();
//                }
//                context.setComponentsEnabled(PROPERTY_L2BIN_FLAGUSE_AUTOFILL_KEY, true);
//            }
//        });



        
        
        // Handle fav1 flaguse events -
        fav1SetToDefault.addPropertyChangeListener(evt -> {
            handleSetFavFlaguse(context, fav1SetToDefault, fav1FlagUse, fav1FlagUseNotes);
        });
        fav1FlagUse.addPropertyChangeListener(evt -> {
            handleSetFavFlaguse(context, fav1SetToDefault, fav1FlagUse, fav1FlagUseNotes);
        });
        fav1FlagUseNotes.addPropertyChangeListener(evt -> {
            handleSetFavFlaguse(context, fav1SetToDefault, fav1FlagUse, fav1FlagUseNotes);
        });



        // Handle fav2 flaguse events -
        fav2SetToDefault.addPropertyChangeListener(evt -> {
            handleSetFavFlaguse(context, fav2SetToDefault, fav2FlagUse, fav2FlagUseNotes);
        });
        fav2FlagUse.addPropertyChangeListener(evt -> {
            handleSetFavFlaguse(context, fav2SetToDefault, fav2FlagUse, fav2FlagUseNotes);
        });
        fav2FlagUseNotes.addPropertyChangeListener(evt -> {
            handleSetFavFlaguse(context, fav2SetToDefault, fav2FlagUse, fav2FlagUseNotes);
        });



        // Handle fav3 flaguse events -
        fav3SetToDefault.addPropertyChangeListener(evt -> {
            handleSetFavFlaguse(context, fav3SetToDefault, fav3FlagUse, fav3FlagUseNotes);
        });
        fav3FlagUse.addPropertyChangeListener(evt -> {
            handleSetFavFlaguse(context, fav3SetToDefault, fav3FlagUse, fav3FlagUseNotes);
        });
        fav3FlagUseNotes.addPropertyChangeListener(evt -> {
            handleSetFavFlaguse(context, fav3SetToDefault, fav3FlagUse, fav3FlagUseNotes);
        });



        // Handle fav4 flaguse events -
        fav4SetToDefault.addPropertyChangeListener(evt -> {
            handleSetFavFlaguse(context, fav4SetToDefault, fav4FlagUse, fav4FlagUseNotes);
        });
        fav4FlagUse.addPropertyChangeListener(evt -> {
            handleSetFavFlaguse(context, fav4SetToDefault, fav4FlagUse, fav4FlagUseNotes);
        });
        fav4FlagUseNotes.addPropertyChangeListener(evt -> {
            handleSetFavFlaguse(context, fav4SetToDefault, fav4FlagUse, fav4FlagUseNotes);
        });



        // Handle fav5 flaguse events -
        fav5SetToDefault.addPropertyChangeListener(evt -> {
            handleSetFavFlaguse(context, fav5SetToDefault, fav5FlagUse, fav5FlagUseNotes);
        });
        fav5FlagUse.addPropertyChangeListener(evt -> {
            handleSetFavFlaguse(context, fav5SetToDefault, fav5FlagUse, fav5FlagUseNotes);
        });
        fav5FlagUseNotes.addPropertyChangeListener(evt -> {
            handleSetFavFlaguse(context, fav5SetToDefault, fav5FlagUse, fav5FlagUseNotes);
        });




        fav1Products_SetToDefault.addPropertyChangeListener(evt -> {
            handleSetFavProducts(context, fav1Products_SetToDefault, fav1Products_l3bprod, fav1Products_output_wavelengths, fav1Products_suite);
        });
        fav1Products_l3bprod.addPropertyChangeListener(evt -> {
            handleSetFavProducts(context, fav1Products_SetToDefault, fav1Products_l3bprod, fav1Products_output_wavelengths, fav1Products_suite);
        });
        fav1Products_output_wavelengths.addPropertyChangeListener(evt -> {
            handleSetFavProducts(context, fav1Products_SetToDefault, fav1Products_l3bprod, fav1Products_output_wavelengths, fav1Products_suite);
        });
        fav1Products_suite.addPropertyChangeListener(evt -> {
            handleSetFavProducts(context, fav1Products_SetToDefault, fav1Products_l3bprod, fav1Products_output_wavelengths, fav1Products_suite);
        });


        fav2Products_SetToDefault.addPropertyChangeListener(evt -> {
            handleSetFavProducts(context, fav2Products_SetToDefault, fav2Products_l3bprod, fav2Products_output_wavelengths, fav2Products_suite);
        });
        fav2Products_l3bprod.addPropertyChangeListener(evt -> {
            handleSetFavProducts(context, fav2Products_SetToDefault, fav2Products_l3bprod, fav2Products_output_wavelengths, fav2Products_suite);
        });
        fav2Products_output_wavelengths.addPropertyChangeListener(evt -> {
            handleSetFavProducts(context, fav2Products_SetToDefault, fav2Products_l3bprod, fav2Products_output_wavelengths, fav2Products_suite);
        });
        fav2Products_suite.addPropertyChangeListener(evt -> {
            handleSetFavProducts(context, fav2Products_SetToDefault, fav2Products_l3bprod, fav2Products_output_wavelengths, fav2Products_suite);
        });


        fav3Products_SetToDefault.addPropertyChangeListener(evt -> {
            handleSetFavProducts(context, fav3Products_SetToDefault, fav3Products_l3bprod, fav3Products_output_wavelengths, fav3Products_suite);
        });
        fav3Products_l3bprod.addPropertyChangeListener(evt -> {
            handleSetFavProducts(context, fav3Products_SetToDefault, fav3Products_l3bprod, fav3Products_output_wavelengths, fav3Products_suite);
        });
        fav3Products_output_wavelengths.addPropertyChangeListener(evt -> {
            handleSetFavProducts(context, fav3Products_SetToDefault, fav3Products_l3bprod, fav3Products_output_wavelengths, fav3Products_suite);
        });
        fav3Products_suite.addPropertyChangeListener(evt -> {
            handleSetFavProducts(context, fav3Products_SetToDefault, fav3Products_l3bprod, fav3Products_output_wavelengths, fav3Products_suite);
        });


        fav4Products_SetToDefault.addPropertyChangeListener(evt -> {
            handleSetFavProducts(context, fav4Products_SetToDefault, fav4Products_l3bprod, fav4Products_output_wavelengths, fav4Products_suite);
        });
        fav4Products_l3bprod.addPropertyChangeListener(evt -> {
            handleSetFavProducts(context, fav4Products_SetToDefault, fav4Products_l3bprod, fav4Products_output_wavelengths, fav4Products_suite);
        });
        fav4Products_output_wavelengths.addPropertyChangeListener(evt -> {
            handleSetFavProducts(context, fav4Products_SetToDefault, fav4Products_l3bprod, fav4Products_output_wavelengths, fav4Products_suite);
        });
        fav4Products_suite.addPropertyChangeListener(evt -> {
            handleSetFavProducts(context, fav4Products_SetToDefault, fav4Products_l3bprod, fav4Products_output_wavelengths, fav4Products_suite);
        });


        fav5Products_SetToDefault.addPropertyChangeListener(evt -> {
            handleSetFavProducts(context, fav5Products_SetToDefault, fav5Products_l3bprod, fav5Products_output_wavelengths, fav5Products_suite);
        });
        fav5Products_l3bprod.addPropertyChangeListener(evt -> {
            handleSetFavProducts(context, fav5Products_SetToDefault, fav5Products_l3bprod, fav5Products_output_wavelengths, fav5Products_suite);
        });
        fav5Products_output_wavelengths.addPropertyChangeListener(evt -> {
            handleSetFavProducts(context, fav5Products_SetToDefault, fav5Products_l3bprod, fav5Products_output_wavelengths, fav5Products_suite);
        });
        fav5Products_suite.addPropertyChangeListener(evt -> {
            handleSetFavProducts(context, fav5Products_SetToDefault, fav5Products_l3bprod, fav5Products_output_wavelengths, fav5Products_suite);
        });
        
        



        fav1Geospatial_SetToDefault.addPropertyChangeListener(evt -> {
            handleSetFavGeospatial(context, fav1Geospatial_SetToDefault, fav1Geospatial_resolution, fav1Geospatial_NSWE);
        });
        fav1Geospatial_NSWE.addPropertyChangeListener(evt -> {
            handleSetFavGeospatial(context, fav1Geospatial_SetToDefault, fav1Geospatial_resolution, fav1Geospatial_NSWE);
        });
        fav1Geospatial_resolution.addPropertyChangeListener(evt -> {
            handleSetFavGeospatial(context, fav1Geospatial_SetToDefault, fav1Geospatial_resolution, fav1Geospatial_NSWE);
        });


        fav2Geospatial_SetToDefault.addPropertyChangeListener(evt -> {
            handleSetFavGeospatial(context, fav2Geospatial_SetToDefault, fav2Geospatial_resolution, fav2Geospatial_NSWE);
        });
        fav2Geospatial_NSWE.addPropertyChangeListener(evt -> {
            handleSetFavGeospatial(context, fav2Geospatial_SetToDefault, fav2Geospatial_resolution, fav2Geospatial_NSWE);
        });
        fav2Geospatial_resolution.addPropertyChangeListener(evt -> {
            handleSetFavGeospatial(context, fav2Geospatial_SetToDefault, fav2Geospatial_resolution, fav2Geospatial_NSWE);
        });


        fav3Geospatial_SetToDefault.addPropertyChangeListener(evt -> {
            handleSetFavGeospatial(context, fav3Geospatial_SetToDefault, fav3Geospatial_resolution, fav3Geospatial_NSWE);
        });
        fav3Geospatial_NSWE.addPropertyChangeListener(evt -> {
            handleSetFavGeospatial(context, fav3Geospatial_SetToDefault, fav3Geospatial_resolution, fav3Geospatial_NSWE);
        });
        fav3Geospatial_resolution.addPropertyChangeListener(evt -> {
            handleSetFavGeospatial(context, fav3Geospatial_SetToDefault, fav3Geospatial_resolution, fav3Geospatial_NSWE);
        });


        fav4Geospatial_SetToDefault.addPropertyChangeListener(evt -> {
            handleSetFavGeospatial(context, fav4Geospatial_SetToDefault, fav4Geospatial_resolution, fav4Geospatial_NSWE);
        });
        fav4Geospatial_NSWE.addPropertyChangeListener(evt -> {
            handleSetFavGeospatial(context, fav4Geospatial_SetToDefault, fav4Geospatial_resolution, fav4Geospatial_NSWE);
        });
        fav4Geospatial_resolution.addPropertyChangeListener(evt -> {
            handleSetFavGeospatial(context, fav4Geospatial_SetToDefault, fav4Geospatial_resolution, fav4Geospatial_NSWE);
        });


        fav5Geospatial_SetToDefault.addPropertyChangeListener(evt -> {
            handleSetFavGeospatial(context, fav5Geospatial_SetToDefault, fav5Geospatial_resolution, fav5Geospatial_NSWE);
        });
        fav5Geospatial_NSWE.addPropertyChangeListener(evt -> {
            handleSetFavGeospatial(context, fav5Geospatial_SetToDefault, fav5Geospatial_resolution, fav5Geospatial_NSWE);
        });
        fav5Geospatial_resolution.addPropertyChangeListener(evt -> {
            handleSetFavGeospatial(context, fav5Geospatial_SetToDefault, fav5Geospatial_resolution, fav5Geospatial_NSWE);
        });




        fieldsAdd.addPropertyChangeListener(evt -> {
            enablement(context);
        });

        namingScheme.addPropertyChangeListener(evt -> {
            enablement(context);
        });

        flaguseAutofill.addPropertyChangeListener(evt -> {
            enablement(context);
        });

        l3bprodAutofill.addPropertyChangeListener(evt -> {
            enablement(context);
        });

        autoFillOther.addPropertyChangeListener(evt -> {
            enablement(context);
        });
        
        
        

        // Add listeners to all components in order to uncheck restoreDefaults checkbox accordingly

        PropertySet propertyContainer = context.getPropertySet();
        Property[] properties = propertyContainer.getProperties();

        for (Property property : properties) {
            if (property != restoreDefaults) {
                property.addPropertyChangeListener(evt -> {
                    handlePreferencesPropertyValueChange(context);
                });
            }
        }

        enablement(context);


        // This call is an initialization call which set restoreDefault initial value
        handlePreferencesPropertyValueChange(context);
    }



    private void enablement(BindingContext context) {

        boolean autoFillOtherBool =  autoFillOther.getValue();
        boolean autoFillL3bprod =  l3bprodAutofill.getValue();
        boolean autoFillFlaguse =  flaguseAutofill.getValue();

        if (autoFillOtherBool || autoFillFlaguse || autoFillL3bprod) {
            context.setComponentsEnabled(PROPERTY_L2BIN_AUTOFILL_PRECEDENCE_KEY, true);
            context.setComponentsEnabled(PROPERTY_L2BIN_AUTOFILL_PRECEDENCE_NULL_SUITE_KEY, true);
        } else {
            context.setComponentsEnabled(PROPERTY_L2BIN_AUTOFILL_PRECEDENCE_KEY, false);
            context.setComponentsEnabled(PROPERTY_L2BIN_AUTOFILL_PRECEDENCE_NULL_SUITE_KEY, false);
        }
        

        
        
        if (OFILE_NAMING_SCHEME_IFILE_REPLACE.equals(namingScheme.getValue())) {
            context.setComponentsEnabled(PROPERTY_L2BIN_OFILE_NAMING_SCHEME_IFILE_REPLACE_KEY, true);
            context.setComponentsEnabled(PROPERTY_L2BIN_OFILE_NAMING_SCHEME_IFILE_ORIGINAL_KEY, true);
        } else {
            context.setComponentsEnabled(PROPERTY_L2BIN_OFILE_NAMING_SCHEME_IFILE_REPLACE_KEY, false);
            context.setComponentsEnabled(PROPERTY_L2BIN_OFILE_NAMING_SCHEME_IFILE_ORIGINAL_KEY, false);
        }
        
        if (OFILE_NAMING_SCHEME_SUFFIX_NONE.equals(fieldsAdd.getValue())
                || OFILE_NAMING_SCHEME_SUFFIX_DEFAULT.equals(fieldsAdd.getValue())
                || OFILE_NAMING_SCHEME_SUFFIX_DEFAULT2.equals(fieldsAdd.getValue())
                || OFILE_NAMING_SCHEME_SUFFIX_DEFAULT3.equals(fieldsAdd.getValue())
                || OFILE_NAMING_SCHEME_SUFFIX_DEFAULT4.equals(fieldsAdd.getValue())
        ) {
            context.setComponentsEnabled(PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SUFFIX1_KEY, false);
            context.setComponentsEnabled(PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SUFFIX2_KEY, false);
        }

        if (OFILE_NAMING_SCHEME_SUFFIX1.equals(fieldsAdd.getValue())) {
            context.setComponentsEnabled(PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SUFFIX1_KEY, true);
            context.setComponentsEnabled(PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SUFFIX2_KEY, false);
        }

        if (OFILE_NAMING_SCHEME_SUFFIX2.equals(fieldsAdd.getValue())) {
            context.setComponentsEnabled(PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SUFFIX1_KEY, false);
            context.setComponentsEnabled(PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SUFFIX2_KEY, true);
        }

    }
    


    /**
     * Test all properties to determine whether the current value is the default value
     *
     * @param context
     * @return
     * @author Daniel Knowles
     */
    private boolean isDefaults(BindingContext context) {

        PropertySet propertyContainer = context.getPropertySet();
        Property[] properties = propertyContainer.getProperties();

        for (Property property : properties) {
            if (!property.getName().contains(PROPERTY_L2BIN_ROOT_FAV_KEY)) {
                if (property != restoreDefaults && property.getDescriptor().getDefaultValue() != null) {
                    if (!property.getValue().equals(property.getDescriptor().getDefaultValue())) {
                        return false;
                    }
                }
            }
        }

        return true;
    }


    /**
     * Handles the restore defaults action
     *
     * @param context
     * @author Daniel Knowles
     */
    private void handleRestoreDefaults(BindingContext context) {
        if (propertyValueChangeEventsEnabled) {
            propertyValueChangeEventsEnabled = false;
            try {
                if (restoreDefaults.getValue()) {

                    PropertySet propertyContainer = context.getPropertySet();
                    Property[] properties = propertyContainer.getProperties();

                    for (Property property : properties) {
                        if (property != restoreDefaults && property.getDescriptor().getDefaultValue() != null) {
                            if (!property.getName().contains(PROPERTY_L2BIN_ROOT_FAV_KEY)) {
                                property.setValue(property.getDescriptor().getDefaultValue());
                            }
                        }
                    }
                }
            } catch (ValidationException e) {
                e.printStackTrace();
            }
            propertyValueChangeEventsEnabled = true;

            context.setComponentsEnabled(PROPERTY_RESTORE_DEFAULTS_KEY, false);
        }
    }





    /**
     * Handles favorite action
     *
     * @param context
     * @author Daniel Knowles
     */
    private void handleSetFavFlaguse(BindingContext context, Property favSet, Property favFlagUse, Property favFlagUseNotes) {



        if (propertyValueChangeEventsEnabled) {
            propertyValueChangeEventsEnabled = false;
            try {
                flaguse_notes.setValue("");

                if (favSet.getValue()) {
                    // set all favorites checkboxes to false then reset current favorite checkbox to true.
                    fav1SetToDefault.setValue(false);
                    fav2SetToDefault.setValue(false);
                    fav3SetToDefault.setValue(false);
                    fav4SetToDefault.setValue(false);
                    fav5SetToDefault.setValue(false);
                    favSet.setValue(true);

                    flaguse.setValue("");
                    if (favFlagUse != null && favFlagUse.getValue() != null) {
                        if (favFlagUse.getValue().toString() != null) {
                            flaguse.setValue(favFlagUse.getValue().toString().trim());
                        }
                    }
                    if (favFlagUseNotes != null && favFlagUseNotes.getValue() != null) {
                        if (favFlagUseNotes.getValue().toString() != null) {
                            flaguse_notes.setValue(favFlagUseNotes.getValue().toString());
                        }
                    }
                } else {
                    flaguse.setValue(flaguse.getDescriptor().getDefaultValue());
                }

            } catch (ValidationException e) {
                e.printStackTrace();
            }
            propertyValueChangeEventsEnabled = true;

//            context.setComponentsEnabled(PROPERTY_RESTORE_DEFAULTS_KEY, false);
        }
    }





    /**
     * Handles favorite action
     *
     * @param context
     * @author Daniel Knowles
     */
    private void handleSetFavProducts(BindingContext context, Property favSet, Property fav_l3bprod, Property fav_output_wavelengths, Property fav_suite) {


        if (propertyValueChangeEventsEnabled) {
            propertyValueChangeEventsEnabled = false;
            try {
                if (favSet.getValue()) {
                    // set all favorites checkboxes to false then reset current favorite checkbox to true.
                    fav1Products_SetToDefault.setValue(false);
                    fav2Products_SetToDefault.setValue(false);
                    fav3Products_SetToDefault.setValue(false);
                    fav4Products_SetToDefault.setValue(false);
                    fav5Products_SetToDefault.setValue(false);
                    favSet.setValue(true);
                    
                    l3bprod.setValue("");
                    if (fav_l3bprod != null && fav_l3bprod.getValue() != null) {
                        if (fav_l3bprod.getValue().toString() != null) {
                            l3bprod.setValue(fav_l3bprod.getValue().toString().trim());
                        }
                    }

                    suite.setValue("");
                    if (fav_suite != null && fav_suite.getValue() != null) {
                        if (fav_suite.getValue().toString() != null) {
                            suite.setValue(fav_suite.getValue().toString().trim());
                        }
                    }
                    
                    output_wavelengths.setValue("");
                    if ( fav_output_wavelengths != null && fav_output_wavelengths.getValue() != null) {
                        if (fav_output_wavelengths.getValue().toString() != null) {
                            output_wavelengths.setValue(fav_output_wavelengths.getValue().toString().trim());
                        }
                    }
                } else {
                    l3bprod.setValue(l3bprod.getDescriptor().getDefaultValue());
                    suite.setValue(suite.getDescriptor().getDefaultValue());
                    output_wavelengths.setValue(output_wavelengths.getDescriptor().getDefaultValue());
                }

            } catch (ValidationException e) {
                e.printStackTrace();
            }
            propertyValueChangeEventsEnabled = true;

//            context.setComponentsEnabled(PROPERTY_RESTORE_DEFAULTS_KEY, false);
        }
    }


    
    


    /**
     * Handles favorite action
     *
     * @param context
     * @author Daniel Knowles
     */
    private void handleSetFavGeospatial(BindingContext context, Property favSet, Property fav_resolution, Property favNSWE) {


        if (propertyValueChangeEventsEnabled) {
            propertyValueChangeEventsEnabled = false;
            try {
                if (favSet.getValue()) {
                    // set all favorites checkboxes to false then reset current favorite checkbox to true.
                    fav1Geospatial_SetToDefault.setValue(false);
                    fav2Geospatial_SetToDefault.setValue(false);
                    fav3Geospatial_SetToDefault.setValue(false);
                    fav4Geospatial_SetToDefault.setValue(false);
                    fav5Geospatial_SetToDefault.setValue(false);
                    favSet.setValue(true);
                    

                    resolution.setValue("");
                    if (fav_resolution != null && fav_resolution.getValue() != null) {
                        if (fav_resolution.getValue().toString() != null) {
                            resolution.setValue(fav_resolution.getValue().toString().trim());
                        }
                    }
                    
                    north.setValue("");
                    south.setValue("");
                    west.setValue("");
                    east.setValue("");
                    if ( favNSWE != null && favNSWE.getValue() != null && favNSWE.getValue().toString() != null) {
                            String[] values = favNSWE.getValue().toString().split(",");

                            if (values != null && values.length == 4) {
                                north.setValue(values[0].trim());
                                south.setValue(values[1].trim());
                                west.setValue(values[2].trim());
                                east.setValue(values[3].trim());
                            }
                    }

                } else {
                    resolution.setValue(resolution.getDescriptor().getDefaultValue());
                    north.setValue(north.getDescriptor().getDefaultValue());
                    south.setValue(south.getDescriptor().getDefaultValue());
                    west.setValue(west.getDescriptor().getDefaultValue());
                    east.setValue(east.getDescriptor().getDefaultValue());
                }

            } catch (ValidationException e) {
                e.printStackTrace();
            }
            propertyValueChangeEventsEnabled = true;

//            context.setComponentsEnabled(PROPERTY_RESTORE_DEFAULTS_KEY, false);
        }
    }






    /**
     * Set restoreDefault component because a property has changed
     * @param context
     * @author Daniel Knowles
     */
    private void handlePreferencesPropertyValueChange(BindingContext context) {
        if (propertyValueChangeEventsEnabled) {
            propertyValueChangeEventsEnabled = false;
            try {
                restoreDefaults.setValue(isDefaults(context));
                context.setComponentsEnabled(PROPERTY_RESTORE_DEFAULTS_KEY, !isDefaults(context));
            } catch (ValidationException e) {
                e.printStackTrace();
            }
            propertyValueChangeEventsEnabled = true;
        }
    }


    /**
     * Initialize the property descriptor default value
     *
     * @param context
     * @param propertyName
     * @param propertyDefault
     * @return
     * @author Daniel Knowles
     */
    private Property initPropertyDefaults(BindingContext context, String propertyName, Object propertyDefault) {

//        System.out.println("propertyName=" + propertyName);

        if (context == null) {
            System.out.println("WARNING: context is null");
        }

        Property property = context.getPropertySet().getProperty(propertyName);
        if (property == null) {
            System.out.println("WARNING: property is null");
        }

        property.getDescriptor().setDefaultValue(propertyDefault);

        return property;
    }

    // todo add a help page ... see the ColorBarLayerController for example

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("l2bin");
    }
//    public HelpCtx getHelpCtx() {
//        return new HelpCtx("OCSSW_L2binPreferences");
//    }

    @SuppressWarnings("UnusedDeclaration")
    static class SeadasToolboxBean {

        @Preference(key = PROPERTY_L2BIN_L3BPROD_AUTOFILL_KEY,
                label = PROPERTY_L2BIN_L3BPROD_AUTOFILL_LABEL,
                description = PROPERTY_L2BIN_L3BPROD_AUTOFILL_TOOLTIP)
        boolean l2binL3bprodAutofillDefault = PROPERTY_L2BIN_L3BPROD_AUTOFILL_DEFAULT;

        @Preference(key = PROPERTY_L2BIN_FLAGUSE_AUTOFILL_KEY,
                label = PROPERTY_L2BIN_FLAGUSE_AUTOFILL_LABEL,
                description = PROPERTY_L2BIN_FLAGUSE_AUTOFILL_TOOLTIP)
        boolean l2binFlaguseAutofillDefault = PROPERTY_L2BIN_FLAGUSE_AUTOFILL_DEFAULT;

        @Preference(key = PROPERTY_L2BIN_AUTOFILL_KEY,
                label = PROPERTY_L2BIN_AUTOFILL_LABEL,
                description = PROPERTY_L2BIN_AUTOFILL_TOOLTIP)
        boolean l2binAutofillDefault = PROPERTY_L2BIN_AUTOFILL_DEFAULT;


        @Preference(key = PROPERTY_L2BIN_AUTOFILL_PRECEDENCE_KEY,
                label = PROPERTY_L2BIN_AUTOFILL_PRECEDENCE_LABEL,
                description = PROPERTY_L2BIN_AUTOFILL_PRECEDENCE_TOOLTIP)
        boolean l2binAutofillPrecedenceDefault = PROPERTY_L2BIN_AUTOFILL_PRECEDENCE_DEFAULT;


        @Preference(key = PROPERTY_L2BIN_AUTOFILL_PRECEDENCE_NULL_SUITE_KEY,
                label = PROPERTY_L2BIN_AUTOFILL_PRECEDENCE_NULL_SUITE_LABEL,
                description = PROPERTY_L2BIN_AUTOFILL_PRECEDENCE_NULL_SUITE_TOOLTIP)
        boolean l2binAutofillPrecedenceNullSuiteDefault = PROPERTY_L2BIN_AUTOFILL_PRECEDENCE_NULL_SUITE_DEFAULT;
        




        @Preference(key = PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SECTION_KEY,
                label = PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SECTION_LABEL,
                description = PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SECTION_TOOLTIP)
        boolean l2bin_OFILE_NAMING_SCHEME_SECTION = true;

        @Preference(key = PROPERTY_L2BIN_OFILE_NAMING_SCHEME_KEY,
                label = PROPERTY_L2BIN_OFILE_NAMING_SCHEME_LABEL,
                valueSet = {OFILE_NAMING_SCHEME_SIMPLE,OFILE_NAMING_SCHEME_OCSSW, OFILE_NAMING_SCHEME_OCSSW_SHORT, OFILE_NAMING_SCHEME_IFILE_REPLACE},
                description = PROPERTY_L2BIN_OFILE_NAMING_SCHEME_TOOLTIP)
        String l2binOfileNamingSchemeDefault = PROPERTY_L2BIN_OFILE_NAMING_SCHEME_DEFAULT;


        @Preference(key = PROPERTY_L2BIN_OFILE_NAMING_SCHEME_IFILE_ORIGINAL_KEY,
                label = PROPERTY_L2BIN_OFILE_NAMING_SCHEME_IFILE_ORIGINAL_LABEL,
                description = PROPERTY_L2BIN_OFILE_NAMING_SCHEME_IFILE_ORIGINAL_TOOLTIP)
        String l2binOfileNamingSchemeIfileOriginalDefault = PROPERTY_L2BIN_OFILE_NAMING_SCHEME_IFILE_ORIGINAL_DEFAULT;

        @Preference(key = PROPERTY_L2BIN_OFILE_NAMING_SCHEME_IFILE_REPLACE_KEY,
                label = PROPERTY_L2BIN_OFILE_NAMING_SCHEME_IFILE_REPLACE_LABEL,
                description = PROPERTY_L2BIN_OFILE_NAMING_SCHEME_IFILE_REPLACE_TOOLTIP)
        String l2binOfileNamingSchemeIfileReplaceDefault = PROPERTY_L2BIN_OFILE_NAMING_SCHEME_IFILE_REPLACE_DEFAULT;
        
        
        @Preference(key = PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SUFFIX_OPTIONS_KEY,
                label = PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SUFFIX_OPTIONS_LABEL,
                valueSet = {OFILE_NAMING_SCHEME_SUFFIX_NONE,
                        OFILE_NAMING_SCHEME_SUFFIX1,
                        OFILE_NAMING_SCHEME_SUFFIX2,
                        OFILE_NAMING_SCHEME_SUFFIX_DEFAULT,
                        OFILE_NAMING_SCHEME_SUFFIX_DEFAULT2,
                        OFILE_NAMING_SCHEME_SUFFIX_DEFAULT3,
                        OFILE_NAMING_SCHEME_SUFFIX_DEFAULT4},
                description = PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SUFFIX_OPTIONS_TOOLTIP)
        String l2binOfileNamingSchemeFieldsAddDefault = PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SUFFIX_OPTIONS_DEFAULT;

        @Preference(key = PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SUFFIX1_KEY,
                label = PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SUFFIX1_LABEL,
                description = PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SUFFIX1_TOOLTIP)
        String l2binOfileNamingSchemeSuffix1Default = PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SUFFIX1_DEFAULT;

        @Preference(key = PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SUFFIX2_KEY,
                label = PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SUFFIX2_LABEL,
                description = PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SUFFIX2_TOOLTIP)
        String l2binOfileNamingSchemeSuffix2Default = PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SUFFIX2_DEFAULT;






        @Preference(key = PROPERTY_L2BIN_PARAMETERS_SECTION_KEY,
                label = PROPERTY_L2BIN_PARAMETERS_SECTION_LABEL,
                description = PROPERTY_L2BIN_PARAMETERS_SECTION_TOOLTIP)
        boolean l2bin_PROPERTY_L2BIN_PARAMETERS_SECTION_KEY = true;


        @Preference(key = PROPERTY_L2BIN_L3BPROD_KEY,
                label = PROPERTY_L2BIN_L3BPROD_LABEL,
                description = PROPERTY_L2BIN_L3BPROD_TOOLTIP)
        String l2binL3bprodDefault = PROPERTY_L2BIN_L3BPROD_DEFAULT;


        @Preference(key = PROPERTY_L2BIN_OUTPUT_WAVELENGTHS_KEY,
                label = PROPERTY_L2BIN_OUTPUT_WAVELENGTHS_LABEL,
                description = PROPERTY_L2BIN_OUTPUT_WAVELENGTHS_TOOLTIP)
        String l2binOutputWavelengthsDefault = PROPERTY_L2BIN_OUTPUT_WAVELENGTHS_DEFAULT;


        @Preference(key = PROPERTY_L2BIN_SUITE_KEY,
                label = PROPERTY_L2BIN_SUITE_LABEL,
                description = PROPERTY_L2BIN_SUITE_TOOLTIP)
        String l2binSuiteDefault = PROPERTY_L2BIN_SUITE_DEFAULT;









        @Preference(key = PROPERTY_L2BIN_PARAMETERS_FLAGUSE_SECTION_KEY,
                label = PROPERTY_L2BIN_PARAMETERS_FLAGUSE_SECTION_LABEL,
                description = PROPERTY_L2BIN_PARAMETERS_FLAGUSE_SECTION_TOOLTIP)
        boolean l2bin_PROPERTY_L2BIN_PARAMETERS_FLAGUSE_SECTION_KEY = true;


        @Preference(key = PROPERTY_L2BIN_FLAGUSE_NOTES_KEY,
                label = PROPERTY_L2BIN_FLAGUSE_NOTES_LABEL,
                description = PROPERTY_L2BIN_FLAGUSE_NOTES_TOOLTIP)
        String l2binFlaguseNotesDefault = PROPERTY_L2BIN_FLAGUSE_NOTES_DEFAULT;

        @Preference(key = PROPERTY_L2BIN_FLAGUSE_KEY,
                label = PROPERTY_L2BIN_FLAGUSE_LABEL,
                description = PROPERTY_L2BIN_FLAGUSE_TOOLTIP)
        String l2binFlaguseDefault = PROPERTY_L2BIN_FLAGUSE_DEFAULT;








        @Preference(key = PROPERTY_L2BIN_PARAMETERS_GEOSPATIAL_SECTION_KEY,
                label = PROPERTY_L2BIN_PARAMETERS_GEOSPATIAL_SECTION_LABEL,
                description = PROPERTY_L2BIN_PARAMETERS_GEOSPATIAL_SECTION_TOOLTIP)
        boolean l2bin_PROPERTY_L2BIN_PARAMETERS_GEOSPATIAL_SECTION_KEY = true;

        @Preference(key = PROPERTY_L2BIN_RESOLUTION_KEY,
                label = PROPERTY_L2BIN_RESOLUTION_LABEL,
                description = PROPERTY_L2BIN_RESOLUTION_TOOLTIP)
        String l2binResolutionDefault = PROPERTY_L2BIN_RESOLUTION_DEFAULT;

        @Preference(key = PROPERTY_L2BIN_AREA_WEIGHTING_KEY,
                label = PROPERTY_L2BIN_AREA_WEIGHTING_LABEL,
                description = PROPERTY_L2BIN_AREA_WEIGHTING_TOOLTIP)
        String l2binAreaWeightingDefault = PROPERTY_L2BIN_AREA_WEIGHTING_DEFAULT;


        @Preference(key = PROPERTY_L2BIN_LATNORTH_KEY,
                label = PROPERTY_L2BIN_LATNORTH_LABEL,
                description = PROPERTY_L2BIN_LATNORTH_TOOLTIP)
        String l2binLatnorthDefault = PROPERTY_L2BIN_LATNORTH_DEFAULT;

        @Preference(key = PROPERTY_L2BIN_LATSOUTH_KEY,
                label = PROPERTY_L2BIN_LATSOUTH_LABEL,
                description = PROPERTY_L2BIN_LATSOUTH_TOOLTIP)
        String l2binLatsouthDefault = PROPERTY_L2BIN_LATSOUTH_DEFAULT;

        @Preference(key = PROPERTY_L2BIN_LONWEST_KEY,
                label = PROPERTY_L2BIN_LONWEST_LABEL,
                description = PROPERTY_L2BIN_LONWEST_TOOLTIP)
        String l2binLonwestDefault = PROPERTY_L2BIN_LONWEST_DEFAULT;

        @Preference(key = PROPERTY_L2BIN_LONEAST_KEY,
                label = PROPERTY_L2BIN_LONEAST_LABEL,
                description = PROPERTY_L2BIN_LONEAST_TOOLTIP)
        String l2binLoneastDefault = PROPERTY_L2BIN_LONEAST_DEFAULT;






        @Preference(key = PROPERTY_L2BIN_PARAMETERS_TEMPORAL_SECTION_KEY,
                label = PROPERTY_L2BIN_PARAMETERS_TEMPORAL_SECTION_LABEL,
                description = PROPERTY_L2BIN_PARAMETERS_TEMPORAL_SECTION_TOOLTIP)
        boolean l2bin_PROPERTY_L2BIN_PARAMETERS_TEMPORAL_SECTION_KEY = true;

        @Preference(key = PROPERTY_L2BIN_SDAY_KEY,
                label = PROPERTY_L2BIN_SDAY_LABEL,
                description = PROPERTY_L2BIN_SDAY_TOOLTIP)
        String l2binSdayDefault= PROPERTY_L2BIN_SDAY_DEFAULT;

        @Preference(key = PROPERTY_L2BIN_EDAY_KEY,
                label = PROPERTY_L2BIN_EDAY_LABEL,
                description = PROPERTY_L2BIN_EDAY_TOOLTIP)
        String l2binEdayDefault= PROPERTY_L2BIN_EDAY_DEFAULT;


        @Preference(key = PROPERTY_L2BIN_DELTA_CROSS_KEY,
                label = PROPERTY_L2BIN_DELTA_CROSS_LABEL,
                description = PROPERTY_L2BIN_DELTA_CROSS_TOOLTIP)
        String l2binDeltaCrossDefault= PROPERTY_L2BIN_DELTA_CROSS_DEFAULT;

        @Preference(key = PROPERTY_L2BIN_NIGHT_KEY,
                label = PROPERTY_L2BIN_NIGHT_LABEL,
                description = PROPERTY_L2BIN_NIGHT_TOOLTIP,
                valueSet = {"", "TRUE", "FALSE"})
        String l2binNightDefault= PROPERTY_L2BIN_NIGHT_DEFAULT;






        @Preference(key = PROPERTY_L2BIN_PARAMETERS_BINNING_SECTION_KEY,
                label = PROPERTY_L2BIN_PARAMETERS_BINNING_SECTION_LABEL,
                description = PROPERTY_L2BIN_PARAMETERS_BINNING_SECTION_TOOLTIP)
        boolean l2bin_PROPERTY_L2BIN_PARAMETERS_BINNING_SECTION_KEY = true;


        @Preference(key = PROPERTY_L2BIN_PRODTYPE_KEY,
                label = PROPERTY_L2BIN_PRODTYPE_LABEL,
                description = PROPERTY_L2BIN_PRODTYPE_TOOLTIP)
        String l2binProdtypeDefault = PROPERTY_L2BIN_PRODTYPE_DEFAULT;


        @Preference(key = PROPERTY_L2BIN_COMPOSITE_PROD_KEY,
                label = PROPERTY_L2BIN_COMPOSITE_PROD_LABEL,
                description = PROPERTY_L2BIN_COMPOSITE_PROD_TOOLTIP)
        String l2binCompositeProdDefault = PROPERTY_L2BIN_COMPOSITE_PROD_DEFAULT;

        @Preference(key = PROPERTY_L2BIN_COMPOSITE_SCHEME_KEY,
                label = PROPERTY_L2BIN_COMPOSITE_SCHEME_LABEL,
                description = PROPERTY_L2BIN_COMPOSITE_SCHEME_TOOLTIP)
        String l2binCompositeSchemeDefault = PROPERTY_L2BIN_COMPOSITE_SCHEME_DEFAULT;


        @Preference(key = PROPERTY_L2BIN_ROW_GROUP_KEY,
                label = PROPERTY_L2BIN_ROW_GROUP_LABEL,
                description = PROPERTY_L2BIN_ROW_GROUP_TOOLTIP)
        String l2binRowGroupDefault = PROPERTY_L2BIN_ROW_GROUP_DEFAULT;





        // Favorites 'flaguse'

        @Preference(key = PROPERTY_L2BIN_FAVORITE_SETTINGS_SECTION_KEY,
                label = PROPERTY_L2BIN_FAVORITE_SETTINGS_SECTION_LABEL,
                description = PROPERTY_L2BIN_FAVORITE_SETTINGS_SECTION_TOOLTIP)
        boolean L2bin_FAVORITE_PROJECTIONS_SECTION = true;



        @Preference(key = PROPERTY_L2BIN_FAV1_FLAGUSE_LOAD_KEY,
                label = PROPERTY_L2BIN_FAV1_FLAGUSE_LOAD_LABEL,
                description = PROPERTY_L2BIN_FAV1_FLAGUSE_LOAD_TOOLTIP)
        boolean L2bin_FAV1_FLAGUSE_LOAD_KEY = PROPERTY_L2BIN_FAV1_FLAGUSE_LOAD_DEFAULT;

        @Preference(key = PROPERTY_L2BIN_FAV1_FLAGUSE_DESCRIPTION_KEY,
                label = PROPERTY_L2BIN_FAV1_FLAGUSE_DESCRIPTION_LABEL,
                description = PROPERTY_L2BIN_FAV1_FLAGUSE_DESCRIPTION_TOOLTIP)
        String L2bin_FAV1_FLAGUSE_DESCRIPTION_KEY= PROPERTY_L2BIN_FAV1_FLAGUSE_DESCRIPTION_DEFAULT;

        @Preference(key = PROPERTY_L2BIN_FAV1_FLAGUSE_KEY,
                label = PROPERTY_L2BIN_FAV1_FLAGUSE_LABEL,
                description = PROPERTY_L2BIN_FAV1_FLAGUSE_TOOLTIP)
        String L2bin_FAV1_FLAGUSE_KEY = PROPERTY_L2BIN_FAV1_FLAGUSE_DEFAULT;




        @Preference(key = PROPERTY_L2BIN_FAV2_FLAGUSE_LOAD_KEY,
                label = PROPERTY_L2BIN_FAV2_FLAGUSE_LOAD_LABEL,
                description = PROPERTY_L2BIN_FAV2_FLAGUSE_LOAD_TOOLTIP)
        boolean L2bin_FAV2_FLAGUSE_LOAD_KEY = PROPERTY_L2BIN_FAV2_FLAGUSE_LOAD_DEFAULT;

        @Preference(key = PROPERTY_L2BIN_FAV2_FLAGUSE_DESCRIPTION_KEY,
                label = PROPERTY_L2BIN_FAV2_FLAGUSE_DESCRIPTION_LABEL,
                description = PROPERTY_L2BIN_FAV2_FLAGUSE_DESCRIPTION_TOOLTIP)
        String L2bin_FAV2_FLAGUSE_DESCRIPTION_KEY= PROPERTY_L2BIN_FAV2_FLAGUSE_DESCRIPTION_DEFAULT;

        @Preference(key = PROPERTY_L2BIN_FAV2_FLAGUSE_KEY,
                label = PROPERTY_L2BIN_FAV2_FLAGUSE_LABEL,
                description = PROPERTY_L2BIN_FAV2_FLAGUSE_TOOLTIP)
        String L2bin_FAV2_FLAGUSE_KEY = PROPERTY_L2BIN_FAV2_FLAGUSE_DEFAULT;


        @Preference(key = PROPERTY_L2BIN_FAV3_FLAGUSE_LOAD_KEY,
                label = PROPERTY_L2BIN_FAV3_FLAGUSE_LOAD_LABEL,
                description = PROPERTY_L2BIN_FAV3_FLAGUSE_LOAD_TOOLTIP)
        boolean L2bin_FAV3_FLAGUSE_LOAD_KEY = PROPERTY_L2BIN_FAV3_FLAGUSE_LOAD_DEFAULT;

        @Preference(key = PROPERTY_L2BIN_FAV3_FLAGUSE_DESCRIPTION_KEY,
                label = PROPERTY_L2BIN_FAV3_FLAGUSE_DESCRIPTION_LABEL,
                description = PROPERTY_L2BIN_FAV3_FLAGUSE_DESCRIPTION_TOOLTIP)
        String L2bin_FAV3_FLAGUSE_DESCRIPTION_KEY= PROPERTY_L2BIN_FAV3_FLAGUSE_DESCRIPTION_DEFAULT;

        @Preference(key = PROPERTY_L2BIN_FAV3_FLAGUSE_KEY,
                label = PROPERTY_L2BIN_FAV3_FLAGUSE_LABEL,
                description = PROPERTY_L2BIN_FAV3_FLAGUSE_TOOLTIP)
        String L2bin_FAV3_FLAGUSE_KEY = PROPERTY_L2BIN_FAV3_FLAGUSE_DEFAULT;


        @Preference(key = PROPERTY_L2BIN_FAV4_FLAGUSE_LOAD_KEY,
                label = PROPERTY_L2BIN_FAV4_FLAGUSE_LOAD_LABEL,
                description = PROPERTY_L2BIN_FAV4_FLAGUSE_LOAD_TOOLTIP)
        boolean L2bin_FAV4_FLAGUSE_LOAD_KEY = PROPERTY_L2BIN_FAV4_FLAGUSE_LOAD_DEFAULT;

        @Preference(key = PROPERTY_L2BIN_FAV4_FLAGUSE_DESCRIPTION_KEY,
                label = PROPERTY_L2BIN_FAV4_FLAGUSE_DESCRIPTION_LABEL,
                description = PROPERTY_L2BIN_FAV4_FLAGUSE_DESCRIPTION_TOOLTIP)
        String L2bin_FAV4_FLAGUSE_DESCRIPTION_KEY= PROPERTY_L2BIN_FAV4_FLAGUSE_DESCRIPTION_DEFAULT;

        @Preference(key = PROPERTY_L2BIN_FAV4_FLAGUSE_KEY,
                label = PROPERTY_L2BIN_FAV4_FLAGUSE_LABEL,
                description = PROPERTY_L2BIN_FAV4_FLAGUSE_TOOLTIP)
        String L2bin_FAV4_FLAGUSE_KEY = PROPERTY_L2BIN_FAV4_FLAGUSE_DEFAULT;


        @Preference(key = PROPERTY_L2BIN_FAV5_FLAGUSE_LOAD_KEY,
                label = PROPERTY_L2BIN_FAV5_FLAGUSE_LOAD_LABEL,
                description = PROPERTY_L2BIN_FAV5_FLAGUSE_LOAD_TOOLTIP)
        boolean L2bin_FAV5_FLAGUSE_LOAD_KEY = PROPERTY_L2BIN_FAV5_FLAGUSE_LOAD_DEFAULT;

        @Preference(key = PROPERTY_L2BIN_FAV5_FLAGUSE_DESCRIPTION_KEY,
                label = PROPERTY_L2BIN_FAV5_FLAGUSE_DESCRIPTION_LABEL,
                description = PROPERTY_L2BIN_FAV5_FLAGUSE_DESCRIPTION_TOOLTIP)
        String L2bin_FAV5_FLAGUSE_DESCRIPTION_KEY= PROPERTY_L2BIN_FAV5_FLAGUSE_DESCRIPTION_DEFAULT;

        @Preference(key = PROPERTY_L2BIN_FAV5_FLAGUSE_KEY,
                label = PROPERTY_L2BIN_FAV5_FLAGUSE_LABEL,
                description = PROPERTY_L2BIN_FAV5_FLAGUSE_TOOLTIP)
        String L2bin_FAV5_FLAGUSE_KEY = PROPERTY_L2BIN_FAV5_FLAGUSE_DEFAULT;





        // Favorites products

        @Preference(key = PROPERTY_L2BIN_FAVORITE_PRODUCTS_SECTION_KEY,
                label = PROPERTY_L2BIN_FAVORITE_PRODUCTS_SECTION_LABEL,
                description = PROPERTY_L2BIN_FAVORITE_PRODUCTS_SECTION_TOOLTIP)
        boolean L2bin_FAVORITE_PRODUCTS_SECTION = true;

        
        @Preference(key = PROPERTY_L2BIN_FAV1_PRODUCTS_LOAD_KEY,
                label = PROPERTY_L2BIN_FAV1_PRODUCTS_LOAD_LABEL,
                description = PROPERTY_L2BIN_FAV1_PRODUCTS_LOAD_TOOLTIP)
        boolean L2bin_FAV1_PRODUCTS_LOAD_KEY = PROPERTY_L2BIN_FAV1_PRODUCTS_LOAD_DEFAULT;

        @Preference(key = PROPERTY_L2BIN_FAV1_PRODUCTS_DESCRIPTION_KEY,
                label = PROPERTY_L2BIN_FAV1_PRODUCTS_DESCRIPTION_LABEL,
                description = PROPERTY_L2BIN_FAV1_PRODUCTS_DESCRIPTION_TOOLTIP)
        String L2bin_FAV1_PRODUCTS_DESCRIPTION_KEY= PROPERTY_L2BIN_FAV1_PRODUCTS_DESCRIPTION_DEFAULT;
        
        @Preference(key = PROPERTY_L2BIN_FAV1_PRODUCTS_SUITE_KEY,
                label = PROPERTY_L2BIN_FAV1_PRODUCTS_SUITE_LABEL,
                description = PROPERTY_L2BIN_FAV1_PRODUCTS_SUITE_TOOLTIP)
        String l2binFav1SuiteDefault = PROPERTY_L2BIN_FAV1_PRODUCTS_SUITE_DEFAULT;
        
        @Preference(key = PROPERTY_L2BIN_FAV1_PRODUCTS_L3BPROD_KEY,
                label = PROPERTY_L2BIN_FAV1_PRODUCTS_L3BPROD_LABEL,
                description = PROPERTY_L2BIN_FAV1_PRODUCTS_L3BPROD_TOOLTIP)
        String l2binFav1L3bprodDefault = PROPERTY_L2BIN_FAV1_PRODUCTS_L3BPROD_DEFAULT;

        @Preference(key = PROPERTY_L2BIN_FAV1_PRODUCTS_OUTPUT_WAVELENGTHS_KEY,
                label = PROPERTY_L2BIN_FAV1_PRODUCTS_OUTPUT_WAVELENGTHS_LABEL,
                description = PROPERTY_L2BIN_FAV1_PRODUCTS_OUTPUT_WAVELENGTHS_TOOLTIP)
        String l2binFav1OutputWavelengthsDefault= PROPERTY_L2BIN_FAV1_PRODUCTS_OUTPUT_WAVELENGTHS_DEFAULT;




        @Preference(key = PROPERTY_L2BIN_FAV2_PRODUCTS_LOAD_KEY,
                label = PROPERTY_L2BIN_FAV2_PRODUCTS_LOAD_LABEL,
                description = PROPERTY_L2BIN_FAV2_PRODUCTS_LOAD_TOOLTIP)
        boolean L2bin_FAV2_PRODUCTS_LOAD_KEY = PROPERTY_L2BIN_FAV2_PRODUCTS_LOAD_DEFAULT;

        @Preference(key = PROPERTY_L2BIN_FAV2_PRODUCTS_DESCRIPTION_KEY,
                label = PROPERTY_L2BIN_FAV2_PRODUCTS_DESCRIPTION_LABEL,
                description = PROPERTY_L2BIN_FAV2_PRODUCTS_DESCRIPTION_TOOLTIP)
        String L2bin_FAV2_PRODUCTS_DESCRIPTION_KEY= PROPERTY_L2BIN_FAV2_PRODUCTS_DESCRIPTION_DEFAULT;

        @Preference(key = PROPERTY_L2BIN_FAV2_PRODUCTS_SUITE_KEY,
                label = PROPERTY_L2BIN_FAV2_PRODUCTS_SUITE_LABEL,
                description = PROPERTY_L2BIN_FAV2_PRODUCTS_SUITE_TOOLTIP)
        String l2binFav2SuiteDefault = PROPERTY_L2BIN_FAV2_PRODUCTS_SUITE_DEFAULT;

        @Preference(key = PROPERTY_L2BIN_FAV2_PRODUCTS_L3BPROD_KEY,
                label = PROPERTY_L2BIN_FAV2_PRODUCTS_L3BPROD_LABEL,
                description = PROPERTY_L2BIN_FAV2_PRODUCTS_L3BPROD_TOOLTIP)
        String l2binFav2L3bprodDefault = PROPERTY_L2BIN_FAV2_PRODUCTS_L3BPROD_DEFAULT;

        @Preference(key = PROPERTY_L2BIN_FAV2_PRODUCTS_OUTPUT_WAVELENGTHS_KEY,
                label = PROPERTY_L2BIN_FAV2_PRODUCTS_OUTPUT_WAVELENGTHS_LABEL,
                description = PROPERTY_L2BIN_FAV2_PRODUCTS_OUTPUT_WAVELENGTHS_TOOLTIP)
        String l2binFav2OutputWavelengthsDefault= PROPERTY_L2BIN_FAV2_PRODUCTS_OUTPUT_WAVELENGTHS_DEFAULT;




        @Preference(key = PROPERTY_L2BIN_FAV3_PRODUCTS_LOAD_KEY,
                label = PROPERTY_L2BIN_FAV3_PRODUCTS_LOAD_LABEL,
                description = PROPERTY_L2BIN_FAV3_PRODUCTS_LOAD_TOOLTIP)
        boolean L2bin_FAV3_PRODUCTS_LOAD_KEY = PROPERTY_L2BIN_FAV3_PRODUCTS_LOAD_DEFAULT;

        @Preference(key = PROPERTY_L2BIN_FAV3_PRODUCTS_DESCRIPTION_KEY,
                label = PROPERTY_L2BIN_FAV3_PRODUCTS_DESCRIPTION_LABEL,
                description = PROPERTY_L2BIN_FAV3_PRODUCTS_DESCRIPTION_TOOLTIP)
        String L2bin_FAV3_PRODUCTS_DESCRIPTION_KEY= PROPERTY_L2BIN_FAV3_PRODUCTS_DESCRIPTION_DEFAULT;

        @Preference(key = PROPERTY_L2BIN_FAV3_PRODUCTS_SUITE_KEY,
                label = PROPERTY_L2BIN_FAV3_PRODUCTS_SUITE_LABEL,
                description = PROPERTY_L2BIN_FAV3_PRODUCTS_SUITE_TOOLTIP)
        String l2binFav3SuiteDefault = PROPERTY_L2BIN_FAV3_PRODUCTS_SUITE_DEFAULT;

        @Preference(key = PROPERTY_L2BIN_FAV3_PRODUCTS_L3BPROD_KEY,
                label = PROPERTY_L2BIN_FAV3_PRODUCTS_L3BPROD_LABEL,
                description = PROPERTY_L2BIN_FAV3_PRODUCTS_L3BPROD_TOOLTIP)
        String l2binFav3L3bprodDefault = PROPERTY_L2BIN_FAV3_PRODUCTS_L3BPROD_DEFAULT;

        @Preference(key = PROPERTY_L2BIN_FAV3_PRODUCTS_OUTPUT_WAVELENGTHS_KEY,
                label = PROPERTY_L2BIN_FAV3_PRODUCTS_OUTPUT_WAVELENGTHS_LABEL,
                description = PROPERTY_L2BIN_FAV3_PRODUCTS_OUTPUT_WAVELENGTHS_TOOLTIP)
        String l2binFav3OutputWavelengthsDefault= PROPERTY_L2BIN_FAV3_PRODUCTS_OUTPUT_WAVELENGTHS_DEFAULT;





        @Preference(key = PROPERTY_L2BIN_FAV4_PRODUCTS_LOAD_KEY,
                label = PROPERTY_L2BIN_FAV4_PRODUCTS_LOAD_LABEL,
                description = PROPERTY_L2BIN_FAV4_PRODUCTS_LOAD_TOOLTIP)
        boolean L2bin_FAV4_PRODUCTS_LOAD_KEY = PROPERTY_L2BIN_FAV4_PRODUCTS_LOAD_DEFAULT;

        @Preference(key = PROPERTY_L2BIN_FAV4_PRODUCTS_DESCRIPTION_KEY,
                label = PROPERTY_L2BIN_FAV4_PRODUCTS_DESCRIPTION_LABEL,
                description = PROPERTY_L2BIN_FAV4_PRODUCTS_DESCRIPTION_TOOLTIP)
        String L2bin_FAV4_PRODUCTS_DESCRIPTION_KEY= PROPERTY_L2BIN_FAV4_PRODUCTS_DESCRIPTION_DEFAULT;

        @Preference(key = PROPERTY_L2BIN_FAV4_PRODUCTS_SUITE_KEY,
                label = PROPERTY_L2BIN_FAV4_PRODUCTS_SUITE_LABEL,
                description = PROPERTY_L2BIN_FAV4_PRODUCTS_SUITE_TOOLTIP)
        String l2binFav4SuiteDefault = PROPERTY_L2BIN_FAV4_PRODUCTS_SUITE_DEFAULT;

        @Preference(key = PROPERTY_L2BIN_FAV4_PRODUCTS_L3BPROD_KEY,
                label = PROPERTY_L2BIN_FAV4_PRODUCTS_L3BPROD_LABEL,
                description = PROPERTY_L2BIN_FAV4_PRODUCTS_L3BPROD_TOOLTIP)
        String l2binFav4L3bprodDefault = PROPERTY_L2BIN_FAV4_PRODUCTS_L3BPROD_DEFAULT;

        @Preference(key = PROPERTY_L2BIN_FAV4_PRODUCTS_OUTPUT_WAVELENGTHS_KEY,
                label = PROPERTY_L2BIN_FAV4_PRODUCTS_OUTPUT_WAVELENGTHS_LABEL,
                description = PROPERTY_L2BIN_FAV4_PRODUCTS_OUTPUT_WAVELENGTHS_TOOLTIP)
        String l2binFav4OutputWavelengthsDefault= PROPERTY_L2BIN_FAV4_PRODUCTS_OUTPUT_WAVELENGTHS_DEFAULT;




        @Preference(key = PROPERTY_L2BIN_FAV5_PRODUCTS_LOAD_KEY,
                label = PROPERTY_L2BIN_FAV5_PRODUCTS_LOAD_LABEL,
                description = PROPERTY_L2BIN_FAV5_PRODUCTS_LOAD_TOOLTIP)
        boolean L2bin_FAV5_PRODUCTS_LOAD_KEY = PROPERTY_L2BIN_FAV5_PRODUCTS_LOAD_DEFAULT;

        @Preference(key = PROPERTY_L2BIN_FAV5_PRODUCTS_DESCRIPTION_KEY,
                label = PROPERTY_L2BIN_FAV5_PRODUCTS_DESCRIPTION_LABEL,
                description = PROPERTY_L2BIN_FAV5_PRODUCTS_DESCRIPTION_TOOLTIP)
        String L2bin_FAV5_PRODUCTS_DESCRIPTION_KEY= PROPERTY_L2BIN_FAV5_PRODUCTS_DESCRIPTION_DEFAULT;

        @Preference(key = PROPERTY_L2BIN_FAV5_PRODUCTS_SUITE_KEY,
                label = PROPERTY_L2BIN_FAV5_PRODUCTS_SUITE_LABEL,
                description = PROPERTY_L2BIN_FAV5_PRODUCTS_SUITE_TOOLTIP)
        String l2binFav5SuiteDefault = PROPERTY_L2BIN_FAV5_PRODUCTS_SUITE_DEFAULT;

        @Preference(key = PROPERTY_L2BIN_FAV5_PRODUCTS_L3BPROD_KEY,
                label = PROPERTY_L2BIN_FAV5_PRODUCTS_L3BPROD_LABEL,
                description = PROPERTY_L2BIN_FAV5_PRODUCTS_L3BPROD_TOOLTIP)
        String l2binFav5L3bprodDefault = PROPERTY_L2BIN_FAV5_PRODUCTS_L3BPROD_DEFAULT;

        @Preference(key = PROPERTY_L2BIN_FAV5_PRODUCTS_OUTPUT_WAVELENGTHS_KEY,
                label = PROPERTY_L2BIN_FAV5_PRODUCTS_OUTPUT_WAVELENGTHS_LABEL,
                description = PROPERTY_L2BIN_FAV5_PRODUCTS_OUTPUT_WAVELENGTHS_TOOLTIP)
        String l2binFav5OutputWavelengthsDefault= PROPERTY_L2BIN_FAV5_PRODUCTS_OUTPUT_WAVELENGTHS_DEFAULT;







        // Favorites geospatial

        @Preference(key = PROPERTY_L2BIN_FAVORITE_GEOSPATIAL_SECTION_KEY,
                label = PROPERTY_L2BIN_FAVORITE_GEOSPATIAL_SECTION_LABEL,
                description = PROPERTY_L2BIN_FAVORITE_GEOSPATIAL_SECTION_TOOLTIP)
        boolean L2bin_FAVORITE_GEOSPATIAL_SECTION = true;


        @Preference(key = PROPERTY_L2BIN_FAV1_GEOSPATIAL_LOAD_KEY,
                label = PROPERTY_L2BIN_FAV1_GEOSPATIAL_LOAD_LABEL,
                description = PROPERTY_L2BIN_FAV1_GEOSPATIAL_LOAD_TOOLTIP)
        boolean L2bin_FAV1_GEOSPATIAL_LOAD_KEY = PROPERTY_L2BIN_FAV1_GEOSPATIAL_LOAD_DEFAULT;

        @Preference(key = PROPERTY_L2BIN_FAV1_GEOSPATIAL_DESCRIPTION_KEY,
                label = PROPERTY_L2BIN_FAV1_GEOSPATIAL_DESCRIPTION_LABEL,
                description = PROPERTY_L2BIN_FAV1_GEOSPATIAL_DESCRIPTION_TOOLTIP)
        String L2bin_FAV1_GEOSPATIAL_DESCRIPTION_KEY= PROPERTY_L2BIN_FAV1_GEOSPATIAL_DESCRIPTION_DEFAULT;

        @Preference(key = PROPERTY_L2BIN_FAV1_GEOSPATIAL_RESOLUTION_KEY,
                label = PROPERTY_L2BIN_FAV1_GEOSPATIAL_RESOLUTION_LABEL,
                description = PROPERTY_L2BIN_FAV1_GEOSPATIAL_RESOLUTION_TOOLTIP)
        String l2binFav1ResolutionDefault = PROPERTY_L2BIN_FAV1_GEOSPATIAL_RESOLUTION_DEFAULT;
        
        @Preference(key = PROPERTY_L2BIN_FAV1_GEOSPATIAL_NSWE_KEY,
                label = PROPERTY_L2BIN_FAV1_GEOSPATIAL_NSWE_LABEL,
                description = PROPERTY_L2BIN_FAV1_GEOSPATIAL_NSWE_TOOLTIP)
        String l2binFav1NsweDefault= PROPERTY_L2BIN_FAV1_GEOSPATIAL_NSWE_DEFAULT;



        @Preference(key = PROPERTY_L2BIN_FAV2_GEOSPATIAL_LOAD_KEY,
                label = PROPERTY_L2BIN_FAV2_GEOSPATIAL_LOAD_LABEL,
                description = PROPERTY_L2BIN_FAV2_GEOSPATIAL_LOAD_TOOLTIP)
        boolean L2bin_FAV2_GEOSPATIAL_LOAD_KEY = PROPERTY_L2BIN_FAV2_GEOSPATIAL_LOAD_DEFAULT;

        @Preference(key = PROPERTY_L2BIN_FAV2_GEOSPATIAL_DESCRIPTION_KEY,
                label = PROPERTY_L2BIN_FAV2_GEOSPATIAL_DESCRIPTION_LABEL,
                description = PROPERTY_L2BIN_FAV2_GEOSPATIAL_DESCRIPTION_TOOLTIP)
        String L2bin_FAV2_GEOSPATIAL_DESCRIPTION_KEY= PROPERTY_L2BIN_FAV2_GEOSPATIAL_DESCRIPTION_DEFAULT;

        @Preference(key = PROPERTY_L2BIN_FAV2_GEOSPATIAL_RESOLUTION_KEY,
                label = PROPERTY_L2BIN_FAV2_GEOSPATIAL_RESOLUTION_LABEL,
                description = PROPERTY_L2BIN_FAV2_GEOSPATIAL_RESOLUTION_TOOLTIP)
        String l2binFav2ResolutionDefault = PROPERTY_L2BIN_FAV2_GEOSPATIAL_RESOLUTION_DEFAULT;

        @Preference(key = PROPERTY_L2BIN_FAV2_GEOSPATIAL_NSWE_KEY,
                label = PROPERTY_L2BIN_FAV2_GEOSPATIAL_NSWE_LABEL,
                description = PROPERTY_L2BIN_FAV2_GEOSPATIAL_NSWE_TOOLTIP)
        String l2binFav2NsweDefault= PROPERTY_L2BIN_FAV2_GEOSPATIAL_NSWE_DEFAULT;



        @Preference(key = PROPERTY_L2BIN_FAV3_GEOSPATIAL_LOAD_KEY,
                label = PROPERTY_L2BIN_FAV3_GEOSPATIAL_LOAD_LABEL,
                description = PROPERTY_L2BIN_FAV3_GEOSPATIAL_LOAD_TOOLTIP)
        boolean L2bin_FAV3_GEOSPATIAL_LOAD_KEY = PROPERTY_L2BIN_FAV3_GEOSPATIAL_LOAD_DEFAULT;

        @Preference(key = PROPERTY_L2BIN_FAV3_GEOSPATIAL_DESCRIPTION_KEY,
                label = PROPERTY_L2BIN_FAV3_GEOSPATIAL_DESCRIPTION_LABEL,
                description = PROPERTY_L2BIN_FAV3_GEOSPATIAL_DESCRIPTION_TOOLTIP)
        String L2bin_FAV3_GEOSPATIAL_DESCRIPTION_KEY= PROPERTY_L2BIN_FAV3_GEOSPATIAL_DESCRIPTION_DEFAULT;

        @Preference(key = PROPERTY_L2BIN_FAV3_GEOSPATIAL_RESOLUTION_KEY,
                label = PROPERTY_L2BIN_FAV3_GEOSPATIAL_RESOLUTION_LABEL,
                description = PROPERTY_L2BIN_FAV3_GEOSPATIAL_RESOLUTION_TOOLTIP)
        String l2binFav3ResolutionDefault = PROPERTY_L2BIN_FAV3_GEOSPATIAL_RESOLUTION_DEFAULT;

        @Preference(key = PROPERTY_L2BIN_FAV3_GEOSPATIAL_NSWE_KEY,
                label = PROPERTY_L2BIN_FAV3_GEOSPATIAL_NSWE_LABEL,
                description = PROPERTY_L2BIN_FAV3_GEOSPATIAL_NSWE_TOOLTIP)
        String l2binFav3NsweDefault= PROPERTY_L2BIN_FAV3_GEOSPATIAL_NSWE_DEFAULT;



        @Preference(key = PROPERTY_L2BIN_FAV4_GEOSPATIAL_LOAD_KEY,
                label = PROPERTY_L2BIN_FAV4_GEOSPATIAL_LOAD_LABEL,
                description = PROPERTY_L2BIN_FAV4_GEOSPATIAL_LOAD_TOOLTIP)
        boolean L2bin_FAV4_GEOSPATIAL_LOAD_KEY = PROPERTY_L2BIN_FAV4_GEOSPATIAL_LOAD_DEFAULT;

        @Preference(key = PROPERTY_L2BIN_FAV4_GEOSPATIAL_DESCRIPTION_KEY,
                label = PROPERTY_L2BIN_FAV4_GEOSPATIAL_DESCRIPTION_LABEL,
                description = PROPERTY_L2BIN_FAV4_GEOSPATIAL_DESCRIPTION_TOOLTIP)
        String L2bin_FAV4_GEOSPATIAL_DESCRIPTION_KEY= PROPERTY_L2BIN_FAV4_GEOSPATIAL_DESCRIPTION_DEFAULT;

        @Preference(key = PROPERTY_L2BIN_FAV4_GEOSPATIAL_RESOLUTION_KEY,
                label = PROPERTY_L2BIN_FAV4_GEOSPATIAL_RESOLUTION_LABEL,
                description = PROPERTY_L2BIN_FAV4_GEOSPATIAL_RESOLUTION_TOOLTIP)
        String l2binFav4ResolutionDefault = PROPERTY_L2BIN_FAV4_GEOSPATIAL_RESOLUTION_DEFAULT;

        @Preference(key = PROPERTY_L2BIN_FAV4_GEOSPATIAL_NSWE_KEY,
                label = PROPERTY_L2BIN_FAV4_GEOSPATIAL_NSWE_LABEL,
                description = PROPERTY_L2BIN_FAV4_GEOSPATIAL_NSWE_TOOLTIP)
        String l2binFav4NsweDefault= PROPERTY_L2BIN_FAV4_GEOSPATIAL_NSWE_DEFAULT;



        @Preference(key = PROPERTY_L2BIN_FAV5_GEOSPATIAL_LOAD_KEY,
                label = PROPERTY_L2BIN_FAV5_GEOSPATIAL_LOAD_LABEL,
                description = PROPERTY_L2BIN_FAV5_GEOSPATIAL_LOAD_TOOLTIP)
        boolean L2bin_FAV5_GEOSPATIAL_LOAD_KEY = PROPERTY_L2BIN_FAV5_GEOSPATIAL_LOAD_DEFAULT;

        @Preference(key = PROPERTY_L2BIN_FAV5_GEOSPATIAL_DESCRIPTION_KEY,
                label = PROPERTY_L2BIN_FAV5_GEOSPATIAL_DESCRIPTION_LABEL,
                description = PROPERTY_L2BIN_FAV5_GEOSPATIAL_DESCRIPTION_TOOLTIP)
        String L2bin_FAV5_GEOSPATIAL_DESCRIPTION_KEY= PROPERTY_L2BIN_FAV5_GEOSPATIAL_DESCRIPTION_DEFAULT;

        @Preference(key = PROPERTY_L2BIN_FAV5_GEOSPATIAL_RESOLUTION_KEY,
                label = PROPERTY_L2BIN_FAV5_GEOSPATIAL_RESOLUTION_LABEL,
                description = PROPERTY_L2BIN_FAV5_GEOSPATIAL_RESOLUTION_TOOLTIP)
        String l2binFav5ResolutionDefault = PROPERTY_L2BIN_FAV5_GEOSPATIAL_RESOLUTION_DEFAULT;

        @Preference(key = PROPERTY_L2BIN_FAV5_GEOSPATIAL_NSWE_KEY,
                label = PROPERTY_L2BIN_FAV5_GEOSPATIAL_NSWE_LABEL,
                description = PROPERTY_L2BIN_FAV5_GEOSPATIAL_NSWE_TOOLTIP)
        String l2binFav5NsweDefault= PROPERTY_L2BIN_FAV5_GEOSPATIAL_NSWE_DEFAULT;

        




        
        





        // Restore Defaults Section

        @Preference(key = PROPERTY_RESTORE_SECTION_KEY,
                label = PROPERTY_RESTORE_SECTION_LABEL,
                description = PROPERTY_RESTORE_SECTION_TOOLTIP)
        boolean restoreDefaultsSection = true;

        @Preference(key = PROPERTY_RESTORE_DEFAULTS_KEY,
                label = PROPERTY_RESTORE_DEFAULTS_LABEL,
                description = PROPERTY_RESTORE_DEFAULTS_TOOLTIP)
        boolean restoreDefaultsDefault = PROPERTY_RESTORE_DEFAULTS_DEFAULT;

    }



    public static String getPreferenceOfileNamingScheme() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(OCSSW_L2binController.PROPERTY_L2BIN_OFILE_NAMING_SCHEME_KEY, OCSSW_L2binController.PROPERTY_L2BIN_OFILE_NAMING_SCHEME_DEFAULT);
    }


    public static String getPreferenceOfileNamingSchemeSuffixOptions() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(OCSSW_L2binController.PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SUFFIX_OPTIONS_KEY, OCSSW_L2binController.PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SUFFIX_OPTIONS_DEFAULT);
    }


    public static String getPreferenceOfileNamingSchemeIfileReplace() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(OCSSW_L2binController.PROPERTY_L2BIN_OFILE_NAMING_SCHEME_IFILE_REPLACE_KEY, OCSSW_L2binController.PROPERTY_L2BIN_OFILE_NAMING_SCHEME_IFILE_REPLACE_DEFAULT);
    }

    public static String getPreferenceOfileNamingSchemeIfileOriginal() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(OCSSW_L2binController.PROPERTY_L2BIN_OFILE_NAMING_SCHEME_IFILE_ORIGINAL_KEY, OCSSW_L2binController.PROPERTY_L2BIN_OFILE_NAMING_SCHEME_IFILE_ORIGINAL_DEFAULT);
    }



    

    public static String getPreferenceOfileNamingSchemeSuffix1() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(OCSSW_L2binController.PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SUFFIX1_KEY, OCSSW_L2binController.PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SUFFIX1_DEFAULT);
    }

    public static String getPreferenceOfileNamingSchemeSuffix2() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(OCSSW_L2binController.PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SUFFIX2_KEY, OCSSW_L2binController.PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SUFFIX2_DEFAULT);
    }

    
    
    public static String getPreferenceL3bprod() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(PROPERTY_L2BIN_L3BPROD_KEY, PROPERTY_L2BIN_L3BPROD_DEFAULT);
    }



    public static String getPreferenceProdtype() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(PROPERTY_L2BIN_PRODTYPE_KEY, PROPERTY_L2BIN_PRODTYPE_DEFAULT);
    }

    public static String getPreferenceResolution() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(PROPERTY_L2BIN_RESOLUTION_KEY, PROPERTY_L2BIN_RESOLUTION_DEFAULT);
    }

    public static String getPreferenceAreaWeighting() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(PROPERTY_L2BIN_AREA_WEIGHTING_KEY, PROPERTY_L2BIN_AREA_WEIGHTING_DEFAULT);
    }

    public static String getPreferenceFlaguse() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(PROPERTY_L2BIN_FLAGUSE_KEY, PROPERTY_L2BIN_FLAGUSE_DEFAULT);
    }

    public static String getPreferenceLatnorth() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(PROPERTY_L2BIN_LATNORTH_KEY, PROPERTY_L2BIN_LATNORTH_DEFAULT);
    }

    public static String getPreferenceLatsouth() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(PROPERTY_L2BIN_LATSOUTH_KEY, PROPERTY_L2BIN_LATSOUTH_DEFAULT);
    }

    public static String getPreferenceLonwest() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(PROPERTY_L2BIN_LONWEST_KEY, PROPERTY_L2BIN_LONWEST_DEFAULT);
    }

    public static String getPreferenceLoneast() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(PROPERTY_L2BIN_LONEAST_KEY, PROPERTY_L2BIN_LONEAST_DEFAULT);
    }




    public static String getPreferenceOutputWavelengths() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(PROPERTY_L2BIN_OUTPUT_WAVELENGTHS_KEY, PROPERTY_L2BIN_OUTPUT_WAVELENGTHS_DEFAULT);
    }
    public static String getPreferenceSuite() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(PROPERTY_L2BIN_SUITE_KEY, PROPERTY_L2BIN_SUITE_DEFAULT);
    }
    public static String getPreferenceCompositeProd() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(PROPERTY_L2BIN_COMPOSITE_PROD_KEY, PROPERTY_L2BIN_COMPOSITE_PROD_DEFAULT);
    }
    public static String getPreferenceCompositeScheme() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(PROPERTY_L2BIN_COMPOSITE_SCHEME_KEY, PROPERTY_L2BIN_COMPOSITE_SCHEME_DEFAULT);
    }
    public static String getPreferenceRowGroup() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(PROPERTY_L2BIN_ROW_GROUP_KEY, PROPERTY_L2BIN_ROW_GROUP_DEFAULT);
    }


    public static String getPreferenceSday() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(PROPERTY_L2BIN_SDAY_KEY, PROPERTY_L2BIN_SDAY_DEFAULT);
    }
    public static String getPreferenceEday() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(PROPERTY_L2BIN_EDAY_KEY, PROPERTY_L2BIN_EDAY_DEFAULT);
    }    
    public static String getPreferenceDeltaCross() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(PROPERTY_L2BIN_DELTA_CROSS_KEY, PROPERTY_L2BIN_DELTA_CROSS_DEFAULT);
    }
    public static String getPreferenceNight() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(PROPERTY_L2BIN_NIGHT_KEY, PROPERTY_L2BIN_NIGHT_DEFAULT);
    }




    public static boolean getPreferenceAutoFillEnable() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyBool(PROPERTY_L2BIN_AUTOFILL_KEY, PROPERTY_L2BIN_AUTOFILL_DEFAULT);
    }
    
    public static boolean getPreferenceFlaguseAutoFillEnable() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyBool(PROPERTY_L2BIN_FLAGUSE_AUTOFILL_KEY, PROPERTY_L2BIN_FLAGUSE_AUTOFILL_DEFAULT);
    }

    public static boolean getPreferenceL3bprodAutoFillEnable() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyBool(PROPERTY_L2BIN_L3BPROD_AUTOFILL_KEY, PROPERTY_L2BIN_L3BPROD_AUTOFILL_DEFAULT);
    }

    public static boolean getPreferenceAutoFillPrecedence() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyBool(PROPERTY_L2BIN_AUTOFILL_PRECEDENCE_KEY, PROPERTY_L2BIN_AUTOFILL_PRECEDENCE_DEFAULT);
    }

    public static boolean getPreferenceAutoFillPrecedenceNullSuite() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyBool(PROPERTY_L2BIN_AUTOFILL_PRECEDENCE_NULL_SUITE_KEY, PROPERTY_L2BIN_AUTOFILL_PRECEDENCE_NULL_SUITE_DEFAULT);
    }
}
