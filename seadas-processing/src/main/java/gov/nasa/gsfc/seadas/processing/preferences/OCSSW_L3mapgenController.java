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

import com.bc.ceres.binding.*;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.PropertyEditorRegistry;
import com.bc.ceres.swing.binding.PropertyPane;
import org.esa.snap.core.layer.ColorBarLayerType;
import org.esa.snap.core.util.PropertyMap;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.preferences.DefaultConfigController;
import org.esa.snap.rcp.preferences.Preference;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;

import javax.swing.*;
import java.awt.*;

/**
 * * Panel handling l3mapgen preferences. Sub-panel of the "SeaDAS-Toolbox"-panel.
 *
 * @author Daniel Knowles
 */


@OptionsPanelController.SubRegistration(location = "SeaDAS",
        displayName = "#Options_DisplayName_OCSSW_L3mapgen",
        keywords = "#Options_Keywords_OCSSW_L3mapgen",
        keywordsCategory = "Processors",
        id = "L3mapgen_preferences",
        position = 4)
@org.openide.util.NbBundle.Messages({
        "Options_DisplayName_OCSSW_L3mapgen=L3mapgen",
        "Options_Keywords_OCSSW_L3mapgen=seadas, ocssw, l3mapgen. mapping, mapped, l3m"
})
public final class OCSSW_L3mapgenController extends DefaultConfigController {

    private static final String FAV = ".favorites";

    Property restoreDefaults;
    Property autoFillOther;
    Property autoFillAll;
    Property autoFillProduct;
    Property projection;
    Property north;
    Property south;
    Property west;
    Property east;

    Property namingScheme;
    Property fieldsAdd;



    Property fav1SetToDefault;
    Property fav1Projection;
    Property fav1NSWE;

    Property fav2SetToDefault;
    Property fav2Projection;
    Property fav2NSWE;

    Property fav3SetToDefault;
    Property fav3Projection;
    Property fav3NSWE;

    Property fav4SetToDefault;
    Property fav4Projection;
    Property fav4NSWE;

    Property fav5SetToDefault;
    Property fav5Projection;
    Property fav5NSWE;
    
    boolean propertyValueChangeEventsEnabled = true;

    private static final String EMPTY_STRING = "";


    public static final String  OFILE_NAMING_SCHEME_SIMPLE = "output";
    public static final String OFILE_NAMING_SCHEME_IFILE_PLUS_SUFFIX = "IFILE + SUFFIX";
    public static final String  OFILE_NAMING_SCHEME_OCSSW_SHORT = "OCSSW (do not derive time field)";
    public static final String  OFILE_NAMING_SCHEME_OCSSW = "OCSSW";
    public static final String OFILE_NAMING_SCHEME_IFILE_REPLACE = "IFILE";

    public static final String OFILE_NAMING_SCHEME_SUFFIX_NONE = "No Suffix";
    public static final String OFILE_NAMING_SCHEME_SUFFIX1 = "Suffix Custom 1";
    public static final String OFILE_NAMING_SCHEME_SUFFIX2 = "Suffix Custom 2";
    public static final String OFILE_NAMING_SCHEME_SUFFIX_DEFAULT = "[product][resolution_units][nswe]";
    public static final String OFILE_NAMING_SCHEME_SUFFIX_DEFAULT2 = "[resolution_units][nswe]";
    public static final String OFILE_NAMING_SCHEME_SUFFIX_DEFAULT3 = "[product][resolution_units][projection][nswe_deg]";
    public static final String OFILE_NAMING_SCHEME_SUFFIX_DEFAULT4 = "[product][resolution][nswe_deg]";


    // Preferences property prefix
    private static final String PROPERTY_L3MAPGEN_ROOT_KEY = SeadasToolboxDefaults.PROPERTY_SEADAS_ROOT_KEY + ".l3mapgen";

    public static final String PROPERTY_L3MAPGEN_PASS_ALL_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + ".pass.all";
    public static final String PROPERTY_L3MAPGEN_PASS_ALL_LABEL = "Pass all fields";
    public static final String PROPERTY_L3MAPGEN_PASS_ALL_TOOLTIP = "<html>Pass all fields, otherwise only pass fields which are not the default value.</html>";
    public static final boolean PROPERTY_L3MAPGEN_PASS_ALL_DEFAULT = false;

    public static final String PROPERTY_L3MAPGEN_AUTOFILL_ALL_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + ".autofill.all";
    public static final String PROPERTY_L3MAPGEN_AUTOFILL_ALL_LABEL = "Autofill all fields with defaults";
    public static final String PROPERTY_L3MAPGEN_AUTOFILL_ALL_TOOLTIP = "<html>Autofills all fields with the  defaults. <br> Note: if a field is set in the preferences then it overrides the suite default.</html>";
    public static final boolean PROPERTY_L3MAPGEN_AUTOFILL_ALL_DEFAULT = true;
    
    
    public static final String PROPERTY_L3MAPGEN_AUTOFILL_PRODUCT_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + ".autofill.product";
    public static final String PROPERTY_L3MAPGEN_AUTOFILL_PRODUCT_LABEL = "Autofill field 'product' with suite defaults";
    public static final String PROPERTY_L3MAPGEN_AUTOFILL_PRODUCT_TOOLTIP = "<html>Autofills fields with the suite defaults. <br>Note: if field is set in the preferences then it overrides the suite default.</html>";
    public static final boolean PROPERTY_L3MAPGEN_AUTOFILL_PRODUCT_DEFAULT = true;

    public static final String PROPERTY_L3MAPGEN_AUTOFILL_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + ".autofill.other";
    public static final String PROPERTY_L3MAPGEN_AUTOFILL_LABEL = "Autofill other fields with suite defaults";
    public static final String PROPERTY_L3MAPGEN_AUTOFILL_TOOLTIP = "<html>Autofills other field with the suite defaults. <br> Note: if a field is set in the preferences then it overrides the suite default.</html>";
    public static final boolean PROPERTY_L3MAPGEN_AUTOFILL_DEFAULT = true;
    
    public static final String PROPERTY_L3MAPGEN_AUTOFILL_PRECEDENCE_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + ".autofill.precedence";
    public static final String PROPERTY_L3MAPGEN_AUTOFILL_PRECEDENCE_LABEL = "Only set preference if default value not set";
    public static final String PROPERTY_L3MAPGEN_AUTOFILL_PRECEDENCE_TOOLTIP = "<html>Preference value takes precedence and overrides any defaults values.  <br>Otherwise, preference is only used if default value is not set.</html>";
    public static final boolean PROPERTY_L3MAPGEN_AUTOFILL_PRECEDENCE_DEFAULT = true;


    public static final String PROPERTY_L3MAPGEN_PARAMETERS_SECTION_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + ".products.section";
    public static final String PROPERTY_L3MAPGEN_PARAMETERS_SECTION_LABEL = "Product & Suite Parameters";
    public static final String PROPERTY_L3MAPGEN_PARAMETERS_SECTION_TOOLTIP = "Product and suite parameters";

    public static final String PROPERTY_L3MAPGEN_PRODUCT_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + ".product";
    public static final String PROPERTY_L3MAPGEN_PRODUCT_LABEL = "product";
    public static final String PROPERTY_L3MAPGEN_PRODUCT_TOOLTIP = "Product(s)";
    public static final String PROPERTY_L3MAPGEN_PRODUCT_DEFAULT = EMPTY_STRING;

    public static final String PROPERTY_L3MAPGEN_WAVELENGTH_3D_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + ".wavelength_3d";
    public static final String PROPERTY_L3MAPGEN_WAVELENGTH_3D_LABEL = "wavelength_3d";
    public static final String PROPERTY_L3MAPGEN_WAVELENGTH_3D_TOOLTIP = "Field 'wavelength_3d'";
    public static final String PROPERTY_L3MAPGEN_WAVELENGTH_3D_DEFAULT = EMPTY_STRING;

    public static final String PROPERTY_L3MAPGEN_SUITE_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + ".suite";
    public static final String PROPERTY_L3MAPGEN_SUITE_LABEL = "suite";
    public static final String PROPERTY_L3MAPGEN_SUITE_TOOLTIP = "Product Suite";
    public static final String PROPERTY_L3MAPGEN_SUITE_DEFAULT = EMPTY_STRING;
    
  

    public static final String PROPERTY_L3MAPGEN_PARAMETERS_SPATIAL_IMAGE_MODE_SECTION_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + ".parameters.spatial.section";
    public static final String PROPERTY_L3MAPGEN_PARAMETERS_SPATIAL_IMAGE_MODE_SECTION_LABEL = "Geospatial Parameters";
    public static final String PROPERTY_L3MAPGEN_PARAMETERS_SPATIAL_IMAGE_MODE_SECTION_TOOLTIP = "L3mapgen Geospatial parameters";
    
    public static final String PROPERTY_L3MAPGEN_PROJECTION_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + ".projection";
    public static final String PROPERTY_L3MAPGEN_PROJECTION_LABEL = "projection";
    public static final String PROPERTY_L3MAPGEN_PROJECTION_TOOLTIP = "Map projection: can be a proj.4 projection string or one of many predefined projections";
    public static final String PROPERTY_L3MAPGEN_PROJECTION_DEFAULT = EMPTY_STRING;

    public static final String PROPERTY_L3MAPGEN_PROJECTION_SMI_REPLACEMENT_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + ".projection.smi_replacement";
    public static final String PROPERTY_L3MAPGEN_PROJECTION_SMI_REPLACEMENT_LABEL = "*projection (smi)";
    public static final String PROPERTY_L3MAPGEN_PROJECTION_SMI_REPLACEMENT_TOOLTIP = "Set 'projection' to this only when 'projection' is 'smi'";
    public static final String PROPERTY_L3MAPGEN_PROJECTION_SMI_REPLACEMENT_DEFAULT = "platecarree";
    

    public static final String PROPERTY_L3MAPGEN_RESOLUTION_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + ".resolution";
    public static final String PROPERTY_L3MAPGEN_RESOLUTION_LABEL = "resolution";
    public static final String PROPERTY_L3MAPGEN_RESOLUTION_TOOLTIP = "Size of the output mapped pixels in meters or SMI dimensions";
    public static final String PROPERTY_L3MAPGEN_RESOLUTION_DEFAULT = EMPTY_STRING;
    
    public static final String PROPERTY_L3MAPGEN_INTERP_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + ".interp";
    public static final String PROPERTY_L3MAPGEN_INTERP_LABEL = "interp";
    public static final String PROPERTY_L3MAPGEN_INTERP_TOOLTIP = "<html>Interpolation method:<br>" +
            "nearest: use the value of the nearest bin for the pixel<br>" +
            "bin: bin all of the pixels that intersect the area of the output pixel<br>" +
            "area: bin weighted by area of all the pixels that intersect the area of the output pixel" +
            "</html>";
    public static final String PROPERTY_L3MAPGEN_INTERP_DEFAULT = EMPTY_STRING;

    public static final String PROPERTY_L3MAPGEN_FUDGE_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + ".fudge";
    public static final String PROPERTY_L3MAPGEN_FUDGE_LABEL = "fudge";
    public static final String PROPERTY_L3MAPGEN_FUDGE_TOOLTIP = "Fudge factor used to modify size of L3 pixels";
    public static final String PROPERTY_L3MAPGEN_FUDGE_DEFAULT = EMPTY_STRING;

    public static final String PROPERTY_L3MAPGEN_WIDTH_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + ".width";
    public static final String PROPERTY_L3MAPGEN_WIDTH_LABEL = "width";
    public static final String PROPERTY_L3MAPGEN_WIDTH_TOOLTIP = "Width of output image in pixels; supercedes 'resolution' parameter";
    public static final String PROPERTY_L3MAPGEN_WIDTH_DEFAULT = EMPTY_STRING;
    
    public static final String PROPERTY_L3MAPGEN_NORTH_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + ".north";
    public static final String PROPERTY_L3MAPGEN_NORTH_LABEL = "north";
    public static final String PROPERTY_L3MAPGEN_NORTH_TOOLTIP = "Northernmost boundary";
    public static final String PROPERTY_L3MAPGEN_NORTH_DEFAULT = EMPTY_STRING;

    public static final String PROPERTY_L3MAPGEN_SOUTH_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + ".south";
    public static final String PROPERTY_L3MAPGEN_SOUTH_LABEL = "south";
    public static final String PROPERTY_L3MAPGEN_SOUTH_TOOLTIP = "Southernmost boundary";
    public static final String PROPERTY_L3MAPGEN_SOUTH_DEFAULT = EMPTY_STRING;

    public static final String PROPERTY_L3MAPGEN_WEST_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + ".west";
    public static final String PROPERTY_L3MAPGEN_WEST_LABEL = "west";
    public static final String PROPERTY_L3MAPGEN_WEST_TOOLTIP = "Westernmost boundary";
    public static final String PROPERTY_L3MAPGEN_WEST_DEFAULT = EMPTY_STRING;

    public static final String PROPERTY_L3MAPGEN_EAST_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + ".east";
    public static final String PROPERTY_L3MAPGEN_EAST_LABEL = "east";
    public static final String PROPERTY_L3MAPGEN_EAST_TOOLTIP = "Easternmost boundary";
    public static final String PROPERTY_L3MAPGEN_EAST_DEFAULT = EMPTY_STRING;

    
    
    
    
    public static final String PROPERTY_L3MAPGEN_PARAMETERS_IMAGE_MODE_SECTION_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + ".parameters.image.mode.section";
    public static final String PROPERTY_L3MAPGEN_PARAMETERS_IMAGE_MODE_SECTION_LABEL = "Image Mode Parameters";
    public static final String PROPERTY_L3MAPGEN_PARAMETERS_IMAGE_MODE_SECTION_TOOLTIP = "L3mapgen parameters for Image Mode";
    
    public static final String PROPERTY_L3MAPGEN_APPLY_PAL_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + FAV + ".apply_pal";
    public static final String PROPERTY_L3MAPGEN_APPLY_PAL_LABEL = "apply_pal";
    public static final String PROPERTY_L3MAPGEN_APPLY_PAL_TOOLTIP = "Apply color palette (palfile)";
    public static final String PROPERTY_L3MAPGEN_APPLY_PAL_DEFAULT =  EMPTY_STRING;

    public static final String PROPERTY_L3MAPGEN_PALFILE_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + ".palfile";
    public static final String PROPERTY_L3MAPGEN_PALFILE_LABEL = "palfile";
    public static final String PROPERTY_L3MAPGEN_PALFILE_TOOLTIP = "Field 'palfile'";
    public static final String PROPERTY_L3MAPGEN_PALFILE_DEFAULT = EMPTY_STRING;
    
    public static final String PROPERTY_L3MAPGEN_DATAMIN_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + ".datamin";
    public static final String PROPERTY_L3MAPGEN_DATAMIN_LABEL = "datamin";
    public static final String PROPERTY_L3MAPGEN_DATAMIN_TOOLTIP = "Field 'datamin'";
    public static final String PROPERTY_L3MAPGEN_DATAMIN_DEFAULT = EMPTY_STRING;

    public static final String PROPERTY_L3MAPGEN_DATAMAX_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + ".datamax";
    public static final String PROPERTY_L3MAPGEN_DATAMAX_LABEL = "datamax";
    public static final String PROPERTY_L3MAPGEN_DATAMAX_TOOLTIP = "Field 'datamax'";
    public static final String PROPERTY_L3MAPGEN_DATAMAX_DEFAULT = EMPTY_STRING;

    public static final String PROPERTY_L3MAPGEN_SCALE_TYPE_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + ".scale_type";
    public static final String PROPERTY_L3MAPGEN_SCALE_TYPE_LABEL = "scale_type";
    public static final String PROPERTY_L3MAPGEN_SCALE_TYPE_TOOLTIP = "Field 'scale_type'";
    public static final String PROPERTY_L3MAPGEN_SCALE_TYPE_DEFAULT = EMPTY_STRING;
    
    public static final String PROPERTY_L3MAPGEN_USE_TRANSPARENCY_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + FAV + ".use_transparency";
    public static final String PROPERTY_L3MAPGEN_USE_TRANSPARENCY_LABEL = "use_transparency";
    public static final String PROPERTY_L3MAPGEN_USE_TRANSPARENCY_TOOLTIP = "Set image tranparency";
    public static final String PROPERTY_L3MAPGEN_USE_TRANSPARENCY_DEFAULT =  EMPTY_STRING;
    
    public static final String PROPERTY_L3MAPGEN_MASK_LAND_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + FAV + ".mask_land";
    public static final String PROPERTY_L3MAPGEN_MASK_LAND_LABEL = "mask_land";
    public static final String PROPERTY_L3MAPGEN_MASK_LAND_TOOLTIP = "Apply land mask (rgb land)";
    public static final String PROPERTY_L3MAPGEN_MASK_LAND_DEFAULT = EMPTY_STRING;
    
    public static final String PROPERTY_L3MAPGEN_RGB_LAND_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + ".rgb_land";
    public static final String PROPERTY_L3MAPGEN_RGB_LAND_LABEL = "rgb_land";
    public static final String PROPERTY_L3MAPGEN_RGB_LAND_TOOLTIP = "Field 'rgb_land'";
    public static final String PROPERTY_L3MAPGEN_RGB_LAND_DEFAULT = EMPTY_STRING;

    public static final String PROPERTY_L3MAPGEN_LAND_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + ".land";
    public static final String PROPERTY_L3MAPGEN_LAND_LABEL = "land";
    public static final String PROPERTY_L3MAPGEN_LAND_TOOLTIP = "Land Geofile used to create land mask";
    public static final String PROPERTY_L3MAPGEN_LAND_DEFAULT = EMPTY_STRING;

    
    
    public static final String PROPERTY_L3MAPGEN_PARAMETERS_PSEUDO_RGB_SECTION_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + ".parameters_pseudo_rgb.section";
    public static final String PROPERTY_L3MAPGEN_PARAMETERS_PSEUDO_RGB_SECTION_LABEL = "Pseudo True Color (RGB) Image Parameters";
    public static final String PROPERTY_L3MAPGEN_PARAMETERS_PSEUDO_RGB_SECTION_TOOLTIP = "L3mapgen parameters forPseudo True/False Color RGB ";
    
    public static final String PROPERTY_L3MAPGEN_USE_RGB_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + FAV + ".use_rgb";
    public static final String PROPERTY_L3MAPGEN_USE_RGB_LABEL = "use_rgb";
    public static final String PROPERTY_L3MAPGEN_USE_RGB_TOOLTIP = "Make Pseudo True Color RGB Image (product_rgb)";
    public static final String PROPERTY_L3MAPGEN_USE_RGB_DEFAULT =  EMPTY_STRING;

    public static final String PROPERTY_L3MAPGEN_PRODUCT_RGB_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + ".product_rgb";
    public static final String PROPERTY_L3MAPGEN_PRODUCT_RGB_LABEL = "product_rgb";
    public static final String PROPERTY_L3MAPGEN_PRODUCT_RGB_TOOLTIP = "RGB Products (comma delimited)";
    public static final String PROPERTY_L3MAPGEN_PRODUCT_RGB_DEFAULT = EMPTY_STRING;

    
    

    public static final String PROPERTY_L3MAPGEN_PARAMETERS_GENERAL_SECTION_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + ".general.section";
    public static final String PROPERTY_L3MAPGEN_PARAMETERS_GENERAL_SECTION_LABEL = "General Parameters";
    public static final String PROPERTY_L3MAPGEN_PARAMETERS_GENERAL_SECTION_TOOLTIP = "General parameters";
    
    public static final String PROPERTY_L3MAPGEN_NUM_CACHE_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + ".num_cache";
    public static final String PROPERTY_L3MAPGEN_NUM_CACHE_LABEL = "num_cache";
    public static final String PROPERTY_L3MAPGEN_NUM_CACHE_TOOLTIP = "Field 'num_cache'";
    public static final String PROPERTY_L3MAPGEN_NUM_CACHE_DEFAULT = EMPTY_STRING;

    public static final String PROPERTY_L3MAPGEN_OFORMAT_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + ".oformat";
    public static final String PROPERTY_L3MAPGEN_OFORMAT_LABEL = "oformat";
    public static final String PROPERTY_L3MAPGEN_OFORMAT_TOOLTIP = "Field 'oformat'";
    public static final String PROPERTY_L3MAPGEN_OFORMAT_DEFAULT = "";




    public static final String PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_SECTION_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + ".ofile.naming.scheme.section";
    public static final String PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_SECTION_LABEL = "Naming Scheme for 'ofile'";
    public static final String PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_SECTION_TOOLTIP = "Naming scheme to use for autofilling ofile name";

    public static final String PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + ".ofile.naming.scheme";
    public static final String PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_LABEL = "Basename Options";
    public static final String PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_TOOLTIP = "Naming scheme to use for autofilling ofile name";
    public static final String PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_DEFAULT = OFILE_NAMING_SCHEME_OCSSW;


    public static final String PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_SUFFIX_OPTIONS_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + ".ofile.naming.scheme.suffix.options";
    public static final String PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_SUFFIX_OPTIONS_LABEL = "Suffix Options";
    public static final String PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_SUFFIX_OPTIONS_TOOLTIP = "ofile Add Suffix Scheme";
    public static final String PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_SUFFIX_OPTIONS_DEFAULT = OFILE_NAMING_SCHEME_SUFFIX1;


    public  static  final  String SUFFIX_LIST_TOOLTIPS = "<html>" +
            "ofile Naming scheme keyed add-ons as suffix of ofile name<br>" +
            "[product] : adds 'product' field with '.' as delimiter<br>" +
            "[_product] : adds 'product' field with '_' as delimiter<br>" +
            "[-product] : adds 'resolution' field with '-' as delimiter<br>" +
            "[.product] : adds 'projection' field with '.' as delimiter<br>" +
            "</html>";

    public static final String PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_SUFFIX1_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + ".ofile.naming.scheme.suffix1";
    public static final String PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_SUFFIX1_LABEL = OFILE_NAMING_SCHEME_SUFFIX1;
    public static final String PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_SUFFIX1_TOOLTIP = SUFFIX_LIST_TOOLTIPS;
    public static final String PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_SUFFIX1_DEFAULT = OFILE_NAMING_SCHEME_SUFFIX_DEFAULT;


    public static final String PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_SUFFIX2_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + ".ofile.naming.scheme.suffix2";
    public static final String PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_SUFFIX2_LABEL = OFILE_NAMING_SCHEME_SUFFIX2;
    public static final String PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_SUFFIX2_TOOLTIP = SUFFIX_LIST_TOOLTIPS;
    public static final String PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_SUFFIX2_DEFAULT = EMPTY_STRING;


    public static final String PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_IFILE_ORIGINAL_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + ".ofile.naming.scheme.ifile.original";
    public static final String PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_IFILE_ORIGINAL_LABEL = "Basename " + OFILE_NAMING_SCHEME_IFILE_REPLACE +  " (Original Text)";
    public static final String PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_IFILE_ORIGINAL_TOOLTIP = "ofile Ifile Original";
    public static final String PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_IFILE_ORIGINAL_DEFAULT = ".L3b.";

    public static final String PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_IFILE_REPLACE_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + ".ofile.naming.scheme.ifile.replace";
    public static final String PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_IFILE_REPLACE_LABEL =  "Basename " + OFILE_NAMING_SCHEME_IFILE_REPLACE + " (Replacement Text)";
    public static final String PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_IFILE_REPLACE_TOOLTIP = "ofile Ifile Replace";
    public static final String PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_IFILE_REPLACE_DEFAULT = ".L3m.";






    public static final String PROPERTY_L3MAPGEN_FAV_PROJECTIONS_SECTION_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + ".FAV.projections.section";
    public static final String PROPERTY_L3MAPGEN_FAV_PROJECTIONS_SECTION_LABEL = "[ Stored Favorites: Projections ]";
    public static final String PROPERTY_L3MAPGEN_FAV_PROJECTIONS_SECTION_TOOLTIP = "Set projection as default";

    private static final String INDENTATION_SPACES = "           ";

    public static final String PROPERTY_L3MAPGEN_FAV1_SET_AS_DEFAULT_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + FAV + ".1.set_as_default";
    public static final String PROPERTY_L3MAPGEN_FAV1_SET_AS_DEFAULT_LABEL = "1. Set as Default";
    public static final String PROPERTY_L3MAPGEN_FAV1_SET_AS_DEFAULT_TOOLTIP = "Set projection and/or bounds as default";
    public static final boolean PROPERTY_L3MAPGEN_FAV1_SET_AS_DEFAULT_DEFAULT = false;

    public static final String PROPERTY_L3MAPGEN_FAV1_PROJECTION_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + FAV + ".1.projection";
    public static final String PROPERTY_L3MAPGEN_FAV1_PROJECTION_LABEL = INDENTATION_SPACES + "Projection";
    public static final String PROPERTY_L3MAPGEN_FAV1_PROJECTION_TOOLTIP = "Custom projection";
    public static final String PROPERTY_L3MAPGEN_FAV1_PROJECTION_DEFAULT = "+proj=laea +lon_0=152.0 +lat_0=-22.0";

    public static final String PROPERTY_L3MAPGEN_FAV1_PROJECTION_DESC_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + FAV + ".1.projection.description";
    public static final String PROPERTY_L3MAPGEN_FAV1_PROJECTION_DESC_LABEL = INDENTATION_SPACES + "Notes/Description";
    public static final String PROPERTY_L3MAPGEN_FAV1_PROJECTION_DESC_TOOLTIP = "Notes/Description of custom projection";
    public static final String PROPERTY_L3MAPGEN_FAV1_PROJECTION_DESC_DEFAULT = "Great Barrier Reef - Southern (Lambert Equal Area)";

    public static final String PROPERTY_L3MAPGEN_FAV1_NSWE_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + FAV + ".1.nswe.bounds";
    public static final String PROPERTY_L3MAPGEN_FAV1_NSWE_LABEL = INDENTATION_SPACES + "N,S,W,E Bounds";
    public static final String PROPERTY_L3MAPGEN_FAV1_NSWE_TOOLTIP = "Geographic boundaries.  Comma delimited. Format N,S,W,E. ";
    public static final String PROPERTY_L3MAPGEN_FAV1_NSWE_DEFAULT = "45,23,34,35";



    public static final String PROPERTY_L3MAPGEN_FAV2_SET_AS_DEFAULT_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + FAV + ".2.set_as_default";
    public static final String PROPERTY_L3MAPGEN_FAV2_SET_AS_DEFAULT_LABEL = "2. Set as Default";
    public static final String PROPERTY_L3MAPGEN_FAV2_SET_AS_DEFAULT_TOOLTIP = "Set projection and/or bounds as default";
    public static final boolean PROPERTY_L3MAPGEN_FAV2_SET_AS_DEFAULT_DEFAULT = false;

    public static final String PROPERTY_L3MAPGEN_FAV2_PROJECTION_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + FAV + ".2.projection";
    public static final String PROPERTY_L3MAPGEN_FAV2_PROJECTION_LABEL = INDENTATION_SPACES + "Projection";
    public static final String PROPERTY_L3MAPGEN_FAV2_PROJECTION_TOOLTIP = "Custom projection";
    public static final String PROPERTY_L3MAPGEN_FAV2_PROJECTION_DEFAULT = "+proj=laea +lon_0=-76.1 +lat_0=37.8";

    public static final String PROPERTY_L3MAPGEN_FAV2_PROJECTION_DESC_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + FAV + ".2.projection.description";
    public static final String PROPERTY_L3MAPGEN_FAV2_PROJECTION_DESC_LABEL = INDENTATION_SPACES + "Notes/Description";
    public static final String PROPERTY_L3MAPGEN_FAV2_PROJECTION_DESC_TOOLTIP = "Notes/Description of custom projection";
    public static final String PROPERTY_L3MAPGEN_FAV2_PROJECTION_DESC_DEFAULT = "Chesapeake Bay (Lambert Equal Area)";

    public static final String PROPERTY_L3MAPGEN_FAV2_NSWE_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + FAV + ".2.nswe.bounds";
    public static final String PROPERTY_L3MAPGEN_FAV2_NSWE_LABEL = INDENTATION_SPACES + "N,S,W,E Bounds";
    public static final String PROPERTY_L3MAPGEN_FAV2_NSWE_TOOLTIP = "Geographic boundaries.  Comma delimited. Format N,S,W,E. ";
    public static final String PROPERTY_L3MAPGEN_FAV2_NSWE_DEFAULT = "45,23,34,35";



    public static final String PROPERTY_L3MAPGEN_FAV3_SET_AS_DEFAULT_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + FAV + ".3.set_as_default";
    public static final String PROPERTY_L3MAPGEN_FAV3_SET_AS_DEFAULT_LABEL = "3. Set as Default";
    public static final String PROPERTY_L3MAPGEN_FAV3_SET_AS_DEFAULT_TOOLTIP = "Set projection and/or bounds as default";
    public static final boolean PROPERTY_L3MAPGEN_FAV3_SET_AS_DEFAULT_DEFAULT = false;

    public static final String PROPERTY_L3MAPGEN_FAV3_PROJECTION_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + FAV + ".3.projection";
    public static final String PROPERTY_L3MAPGEN_FAV3_PROJECTION_LABEL = INDENTATION_SPACES + "Projection";
    public static final String PROPERTY_L3MAPGEN_FAV3_PROJECTION_TOOLTIP = "Custom projection";
    public static final String PROPERTY_L3MAPGEN_FAV3_PROJECTION_DEFAULT = EMPTY_STRING;

    public static final String PROPERTY_L3MAPGEN_FAV3_PROJECTION_DESC_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + FAV + ".3.description";
    public static final String PROPERTY_L3MAPGEN_FAV3_PROJECTION_DESC_LABEL = INDENTATION_SPACES + "Notes/Description";
    public static final String PROPERTY_L3MAPGEN_FAV3_PROJECTION_DESC_TOOLTIP = "Notes/Description of custom projection";
    public static final String PROPERTY_L3MAPGEN_FAV3_PROJECTION_DESC_DEFAULT = EMPTY_STRING;

    public static final String PROPERTY_L3MAPGEN_FAV3_NSWE_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + FAV + ".3.nswe.bounds";
    public static final String PROPERTY_L3MAPGEN_FAV3_NSWE_LABEL = INDENTATION_SPACES + "N,S,W,E Bounds";
    public static final String PROPERTY_L3MAPGEN_FAV3_NSWE_TOOLTIP = "Geographic boundaries.  Comma delimited. Format N,S,W,E. ";
    public static final String PROPERTY_L3MAPGEN_FAV3_NSWE_DEFAULT = EMPTY_STRING;


    public static final String PROPERTY_L3MAPGEN_FAV4_SET_AS_DEFAULT_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + FAV + ".4.set_as_default";
    public static final String PROPERTY_L3MAPGEN_FAV4_SET_AS_DEFAULT_LABEL = "4. Set as Default";
    public static final String PROPERTY_L3MAPGEN_FAV4_SET_AS_DEFAULT_TOOLTIP = "Set projection and/or bounds as default";
    public static final boolean PROPERTY_L3MAPGEN_FAV4_SET_AS_DEFAULT_DEFAULT = false;

    public static final String PROPERTY_L3MAPGEN_FAV4_PROJECTION_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + FAV + ".4.projection";
    public static final String PROPERTY_L3MAPGEN_FAV4_PROJECTION_LABEL = INDENTATION_SPACES + "Projection";
    public static final String PROPERTY_L3MAPGEN_FAV4_PROJECTION_TOOLTIP = "Custom projection";
    public static final String PROPERTY_L3MAPGEN_FAV4_PROJECTION_DEFAULT = EMPTY_STRING;

    public static final String PROPERTY_L3MAPGEN_FAV4_PROJECTION_DESC_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + FAV + ".4.description";
    public static final String PROPERTY_L3MAPGEN_FAV4_PROJECTION_DESC_LABEL = INDENTATION_SPACES + "Notes/Description";
    public static final String PROPERTY_L3MAPGEN_FAV4_PROJECTION_DESC_TOOLTIP = "Notes/Description of custom projection";
    public static final String PROPERTY_L3MAPGEN_FAV4_PROJECTION_DESC_DEFAULT = EMPTY_STRING;

    public static final String PROPERTY_L3MAPGEN_FAV4_NSWE_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + FAV + ".4.nswe.bounds";
    public static final String PROPERTY_L3MAPGEN_FAV4_NSWE_LABEL = INDENTATION_SPACES + "N,S,W,E Bounds";
    public static final String PROPERTY_L3MAPGEN_FAV4_NSWE_TOOLTIP = "Geographic boundaries.  Comma delimited. Format N,S,W,E. ";
    public static final String PROPERTY_L3MAPGEN_FAV4_NSWE_DEFAULT = EMPTY_STRING;


    public static final String PROPERTY_L3MAPGEN_FAV5_SET_AS_DEFAULT_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + FAV + ".5.set_as_default";
    public static final String PROPERTY_L3MAPGEN_FAV5_SET_AS_DEFAULT_LABEL = "5. Set as Default";
    public static final String PROPERTY_L3MAPGEN_FAV5_SET_AS_DEFAULT_TOOLTIP = "Set projection and/or bounds as default";
    public static final boolean PROPERTY_L3MAPGEN_FAV5_SET_AS_DEFAULT_DEFAULT = false;

    public static final String PROPERTY_L3MAPGEN_FAV5_PROJECTION_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + FAV + ".5.projection";
    public static final String PROPERTY_L3MAPGEN_FAV5_PROJECTION_LABEL = INDENTATION_SPACES + "Projection";
    public static final String PROPERTY_L3MAPGEN_FAV5_PROJECTION_TOOLTIP = "Custom projection";
    public static final String PROPERTY_L3MAPGEN_FAV5_PROJECTION_DEFAULT = EMPTY_STRING;

    public static final String PROPERTY_L3MAPGEN_FAV5_PROJECTION_DESC_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + FAV + ".5.description";
    public static final String PROPERTY_L3MAPGEN_FAV5_PROJECTION_DESC_LABEL = INDENTATION_SPACES + "Notes/Description";
    public static final String PROPERTY_L3MAPGEN_FAV5_PROJECTION_DESC_TOOLTIP = "Notes/Description of custom projection";
    public static final String PROPERTY_L3MAPGEN_FAV5_PROJECTION_DESC_DEFAULT = EMPTY_STRING;

    public static final String PROPERTY_L3MAPGEN_FAV5_NSWE_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + FAV + ".5.nswe.bounds";
    public static final String PROPERTY_L3MAPGEN_FAV5_NSWE_LABEL = INDENTATION_SPACES + "N,S,W,E Bounds";
    public static final String PROPERTY_L3MAPGEN_FAV5_NSWE_TOOLTIP = "Geographic boundaries.  Comma delimited. Format N,S,W,E. ";
    public static final String PROPERTY_L3MAPGEN_FAV5_NSWE_DEFAULT = EMPTY_STRING;

    // Property Setting: Restore Defaults

    private static final String PROPERTY_RESTORE_KEY_SUFFIX = PROPERTY_L3MAPGEN_ROOT_KEY + ".restore.defaults";

    public static final String PROPERTY_RESTORE_SECTION_KEY = PROPERTY_RESTORE_KEY_SUFFIX + ".section";
    public static final String PROPERTY_RESTORE_SECTION_LABEL = "Restore";
    public static final String PROPERTY_RESTORE_SECTION_TOOLTIP = "Restores preferences to the package defaults";

    public static final String PROPERTY_RESTORE_DEFAULTS_KEY = PROPERTY_RESTORE_KEY_SUFFIX + ".apply";
    public static final String PROPERTY_RESTORE_DEFAULTS_LABEL = "Default (L3mapgen Preferences)";
    public static final String PROPERTY_RESTORE_DEFAULTS_TOOLTIP = "Restore all L3mapgen preferences to the original default";
    public static final boolean PROPERTY_RESTORE_DEFAULTS_DEFAULT = false;


    protected PropertySet createPropertySet() {
        return createPropertySet(new SeadasToolboxBean());
    }



    @Override
    protected JPanel createPanel(BindingContext context) {

        String[] interpOptionsArray = {EMPTY_STRING,
                "nearest",
                "bin",
                "area"};


        //
        // Initialize the default value contained within each property descriptor
        // This is done so subsequently the restoreDefaults actions can be performed
        //

        autoFillAll = initPropertyDefaults(context, PROPERTY_L3MAPGEN_AUTOFILL_ALL_KEY, PROPERTY_L3MAPGEN_AUTOFILL_ALL_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L3MAPGEN_PASS_ALL_KEY, PROPERTY_L3MAPGEN_PASS_ALL_DEFAULT);
        autoFillOther = initPropertyDefaults(context, PROPERTY_L3MAPGEN_AUTOFILL_KEY, PROPERTY_L3MAPGEN_AUTOFILL_DEFAULT);
        autoFillProduct = initPropertyDefaults(context, PROPERTY_L3MAPGEN_AUTOFILL_PRODUCT_KEY, PROPERTY_L3MAPGEN_AUTOFILL_PRODUCT_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L3MAPGEN_AUTOFILL_PRECEDENCE_KEY, PROPERTY_L3MAPGEN_AUTOFILL_PRECEDENCE_DEFAULT);


        initPropertyDefaults(context, PROPERTY_L3MAPGEN_PARAMETERS_SECTION_KEY, true);
        initPropertyDefaults(context, PROPERTY_L3MAPGEN_PRODUCT_KEY, PROPERTY_L3MAPGEN_PRODUCT_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L3MAPGEN_WAVELENGTH_3D_KEY, PROPERTY_L3MAPGEN_WAVELENGTH_3D_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L3MAPGEN_SUITE_KEY, PROPERTY_L3MAPGEN_SUITE_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L3MAPGEN_NUM_CACHE_KEY, PROPERTY_L3MAPGEN_NUM_CACHE_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L3MAPGEN_OFORMAT_KEY, PROPERTY_L3MAPGEN_OFORMAT_DEFAULT);

        initPropertyDefaults(context, PROPERTY_L3MAPGEN_PARAMETERS_SPATIAL_IMAGE_MODE_SECTION_KEY, true);
        projection = initPropertyDefaults(context, PROPERTY_L3MAPGEN_PROJECTION_KEY, PROPERTY_L3MAPGEN_PROJECTION_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L3MAPGEN_RESOLUTION_KEY, PROPERTY_L3MAPGEN_RESOLUTION_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L3MAPGEN_INTERP_KEY, PROPERTY_L3MAPGEN_INTERP_DEFAULT);
        north = initPropertyDefaults(context, PROPERTY_L3MAPGEN_FUDGE_KEY, PROPERTY_L3MAPGEN_FUDGE_DEFAULT);
        north = initPropertyDefaults(context, PROPERTY_L3MAPGEN_WIDTH_KEY, PROPERTY_L3MAPGEN_WIDTH_DEFAULT);
        north = initPropertyDefaults(context, PROPERTY_L3MAPGEN_NORTH_KEY, PROPERTY_L3MAPGEN_NORTH_DEFAULT);
        south = initPropertyDefaults(context, PROPERTY_L3MAPGEN_SOUTH_KEY, PROPERTY_L3MAPGEN_SOUTH_DEFAULT);
        west = initPropertyDefaults(context, PROPERTY_L3MAPGEN_WEST_KEY, PROPERTY_L3MAPGEN_WEST_DEFAULT);
        east = initPropertyDefaults(context, PROPERTY_L3MAPGEN_EAST_KEY, PROPERTY_L3MAPGEN_EAST_DEFAULT);

        initPropertyDefaults(context, PROPERTY_L3MAPGEN_PARAMETERS_IMAGE_MODE_SECTION_KEY, true);
        initPropertyDefaults(context, PROPERTY_L3MAPGEN_APPLY_PAL_KEY, PROPERTY_L3MAPGEN_APPLY_PAL_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L3MAPGEN_PALFILE_KEY, PROPERTY_L3MAPGEN_PALFILE_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L3MAPGEN_DATAMIN_KEY, PROPERTY_L3MAPGEN_DATAMIN_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L3MAPGEN_DATAMAX_KEY, PROPERTY_L3MAPGEN_DATAMAX_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L3MAPGEN_SCALE_TYPE_KEY, PROPERTY_L3MAPGEN_SCALE_TYPE_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L3MAPGEN_MASK_LAND_KEY, PROPERTY_L3MAPGEN_MASK_LAND_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L3MAPGEN_RGB_LAND_KEY, PROPERTY_L3MAPGEN_RGB_LAND_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L3MAPGEN_LAND_KEY, PROPERTY_L3MAPGEN_LAND_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L3MAPGEN_USE_TRANSPARENCY_KEY, PROPERTY_L3MAPGEN_USE_TRANSPARENCY_DEFAULT);

        initPropertyDefaults(context, PROPERTY_L3MAPGEN_PARAMETERS_PSEUDO_RGB_SECTION_KEY, true);
        initPropertyDefaults(context, PROPERTY_L3MAPGEN_USE_RGB_KEY, PROPERTY_L3MAPGEN_USE_RGB_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L3MAPGEN_PRODUCT_RGB_KEY, PROPERTY_L3MAPGEN_PRODUCT_RGB_DEFAULT);

        initPropertyDefaults(context, PROPERTY_L3MAPGEN_PARAMETERS_GENERAL_SECTION_KEY, true);



        initPropertyDefaults(context, PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_SECTION_KEY, true);
        namingScheme = initPropertyDefaults(context, PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_KEY, PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_DEFAULT);
        fieldsAdd = initPropertyDefaults(context, PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_SUFFIX_OPTIONS_KEY, PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_SUFFIX_OPTIONS_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_IFILE_REPLACE_KEY, PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_IFILE_REPLACE_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_IFILE_ORIGINAL_KEY, PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_IFILE_ORIGINAL_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_SUFFIX1_KEY, PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_SUFFIX1_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_SUFFIX2_KEY, PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_SUFFIX2_DEFAULT);




        initPropertyDefaults(context, PROPERTY_L3MAPGEN_FAV_PROJECTIONS_SECTION_KEY, true);

        fav1SetToDefault = initPropertyDefaults(context, PROPERTY_L3MAPGEN_FAV1_SET_AS_DEFAULT_KEY, PROPERTY_L3MAPGEN_FAV1_SET_AS_DEFAULT_DEFAULT);
        fav1Projection = initPropertyDefaults(context, PROPERTY_L3MAPGEN_FAV1_PROJECTION_KEY, PROPERTY_L3MAPGEN_FAV1_PROJECTION_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L3MAPGEN_FAV1_PROJECTION_DESC_KEY, PROPERTY_L3MAPGEN_FAV1_PROJECTION_DESC_DEFAULT);
        fav1NSWE = initPropertyDefaults(context, PROPERTY_L3MAPGEN_FAV1_NSWE_KEY, PROPERTY_L3MAPGEN_FAV1_NSWE_DEFAULT);

        fav2SetToDefault = initPropertyDefaults(context, PROPERTY_L3MAPGEN_FAV2_SET_AS_DEFAULT_KEY, PROPERTY_L3MAPGEN_FAV2_SET_AS_DEFAULT_DEFAULT);
        fav2Projection = initPropertyDefaults(context, PROPERTY_L3MAPGEN_FAV2_PROJECTION_KEY, PROPERTY_L3MAPGEN_FAV2_PROJECTION_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L3MAPGEN_FAV2_PROJECTION_DESC_KEY, PROPERTY_L3MAPGEN_FAV2_PROJECTION_DESC_DEFAULT);
        fav2NSWE = initPropertyDefaults(context, PROPERTY_L3MAPGEN_FAV2_NSWE_KEY, PROPERTY_L3MAPGEN_FAV2_NSWE_DEFAULT);

        fav3SetToDefault = initPropertyDefaults(context, PROPERTY_L3MAPGEN_FAV3_SET_AS_DEFAULT_KEY, PROPERTY_L3MAPGEN_FAV3_SET_AS_DEFAULT_DEFAULT);
        fav3Projection = initPropertyDefaults(context, PROPERTY_L3MAPGEN_FAV3_PROJECTION_KEY, PROPERTY_L3MAPGEN_FAV3_PROJECTION_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L3MAPGEN_FAV3_PROJECTION_DESC_KEY, PROPERTY_L3MAPGEN_FAV3_PROJECTION_DESC_DEFAULT);
        fav3NSWE = initPropertyDefaults(context, PROPERTY_L3MAPGEN_FAV3_NSWE_KEY, PROPERTY_L3MAPGEN_FAV3_NSWE_DEFAULT);

        fav4SetToDefault = initPropertyDefaults(context, PROPERTY_L3MAPGEN_FAV4_SET_AS_DEFAULT_KEY, PROPERTY_L3MAPGEN_FAV4_SET_AS_DEFAULT_DEFAULT);
        fav4Projection = initPropertyDefaults(context, PROPERTY_L3MAPGEN_FAV4_PROJECTION_KEY, PROPERTY_L3MAPGEN_FAV4_PROJECTION_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L3MAPGEN_FAV4_PROJECTION_DESC_KEY, PROPERTY_L3MAPGEN_FAV4_PROJECTION_DESC_DEFAULT);
        fav4NSWE = initPropertyDefaults(context, PROPERTY_L3MAPGEN_FAV4_NSWE_KEY, PROPERTY_L3MAPGEN_FAV4_NSWE_DEFAULT);

        fav5SetToDefault = initPropertyDefaults(context, PROPERTY_L3MAPGEN_FAV5_SET_AS_DEFAULT_KEY, PROPERTY_L3MAPGEN_FAV5_SET_AS_DEFAULT_DEFAULT);
        fav5Projection = initPropertyDefaults(context, PROPERTY_L3MAPGEN_FAV5_PROJECTION_KEY, PROPERTY_L3MAPGEN_FAV5_PROJECTION_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L3MAPGEN_FAV5_PROJECTION_DESC_KEY, PROPERTY_L3MAPGEN_FAV5_PROJECTION_DESC_DEFAULT);
        fav5NSWE = initPropertyDefaults(context, PROPERTY_L3MAPGEN_FAV5_NSWE_KEY, PROPERTY_L3MAPGEN_FAV5_NSWE_DEFAULT);



        initPropertyDefaults(context, PROPERTY_RESTORE_SECTION_KEY, true);
        restoreDefaults =  initPropertyDefaults(context, PROPERTY_RESTORE_DEFAULTS_KEY, PROPERTY_RESTORE_DEFAULTS_DEFAULT);




        //
        // Create UI
        //

        TableLayout tableLayout = new TableLayout(2);
        tableLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        tableLayout.setTablePadding(new Insets(4, 10, 0, 0));
        tableLayout.setTableFill(TableLayout.Fill.BOTH);
        tableLayout.setColumnWeightX(1, 1.0);

        JPanel pageUI = new JPanel(tableLayout);

        PropertyEditorRegistry registry = PropertyEditorRegistry.getInstance();

        PropertySet propertyContainer = context.getPropertySet();
        Property[] properties = propertyContainer.getProperties();

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
        return parent;
    }


    @Override
    protected void configure(BindingContext context) {


        // Handle resetDefaults events - set all other components to defaults
        restoreDefaults.addPropertyChangeListener(evt -> {
            handleRestoreDefaults(context);
        });

        // Handle fav1 events -
        fav1SetToDefault.addPropertyChangeListener(evt -> {
            handleSetFav(context, fav1SetToDefault, fav1Projection, fav1NSWE);
        });
        fav1Projection.addPropertyChangeListener(evt -> {
            handleSetFav(context, fav1SetToDefault, fav1Projection, fav1NSWE);
        });
        fav1NSWE.addPropertyChangeListener(evt -> {
            handleSetFav(context, fav1SetToDefault, fav1Projection, fav1NSWE);
        });


        // Handle fav2 events -
        fav2SetToDefault.addPropertyChangeListener(evt -> {
            handleSetFav(context, fav2SetToDefault, fav2Projection, fav2NSWE);
        });
        fav2Projection.addPropertyChangeListener(evt -> {
            handleSetFav(context, fav2SetToDefault, fav2Projection, fav2NSWE);
        });
        fav2NSWE.addPropertyChangeListener(evt -> {
            handleSetFav(context, fav2SetToDefault, fav2Projection, fav2NSWE);
        });



        // Handle fav3 events -
        fav3SetToDefault.addPropertyChangeListener(evt -> {
            handleSetFav(context, fav3SetToDefault, fav3Projection, fav3NSWE);
        });
        fav3Projection.addPropertyChangeListener(evt -> {
            handleSetFav(context, fav3SetToDefault, fav3Projection, fav3NSWE);
        });
        fav3NSWE.addPropertyChangeListener(evt -> {
            handleSetFav(context, fav3SetToDefault, fav3Projection, fav3NSWE);
        });


        // Handle fav4 events -
        fav4SetToDefault.addPropertyChangeListener(evt -> {
            handleSetFav(context, fav4SetToDefault, fav4Projection, fav4NSWE);
        });
        fav4Projection.addPropertyChangeListener(evt -> {
            handleSetFav(context, fav4SetToDefault, fav4Projection, fav4NSWE);
        });
        fav4NSWE.addPropertyChangeListener(evt -> {
            handleSetFav(context, fav4SetToDefault, fav4Projection, fav4NSWE);
        });


        // Handle fav5 events -
        fav5SetToDefault.addPropertyChangeListener(evt -> {
            handleSetFav(context, fav5SetToDefault, fav5Projection, fav5NSWE);
        });
        fav5Projection.addPropertyChangeListener(evt -> {
            handleSetFav(context, fav5SetToDefault, fav5Projection, fav5NSWE);
        });
        fav5NSWE.addPropertyChangeListener(evt -> {
            handleSetFav(context, fav5SetToDefault, fav5Projection, fav5NSWE);
        });



        fieldsAdd.addPropertyChangeListener(evt -> {
            enablement(context);
        });

        namingScheme.addPropertyChangeListener(evt -> {
            enablement(context);
        });

        autoFillOther.addPropertyChangeListener(evt -> {
            enablement(context);
        });

        autoFillProduct.addPropertyChangeListener(evt -> {
            enablement(context);
        });

        autoFillAll.addPropertyChangeListener(evt -> {
            enablement(context);
            handleFillAll();
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
        boolean autoFillOtherProduct =  autoFillProduct.getValue();
        boolean autoFillAllBool = autoFillAll.getValue();

        if (autoFillOtherBool || autoFillOtherProduct) {
            context.setComponentsEnabled(PROPERTY_L3MAPGEN_AUTOFILL_PRECEDENCE_KEY, true);
        } else {
            context.setComponentsEnabled(PROPERTY_L3MAPGEN_AUTOFILL_PRECEDENCE_KEY, false);
        }




        if (autoFillAllBool) {
            context.setComponentsEnabled(PROPERTY_L3MAPGEN_AUTOFILL_PRODUCT_KEY, false);
            context.setComponentsEnabled(PROPERTY_L3MAPGEN_AUTOFILL_KEY, false);
        } else {
            context.setComponentsEnabled(PROPERTY_L3MAPGEN_AUTOFILL_PRODUCT_KEY, true);
            context.setComponentsEnabled(PROPERTY_L3MAPGEN_AUTOFILL_KEY, true);
        }



//        context.bindEnabledState(PROPERTY_L3MAPGEN_AUTOFILL_PRECEDENCE_KEY, PROPERTY_L3MAPGEN_AUTOFILL_KEY);
//        context.bindEnabledState(PROPERTY_L3MAPGEN_AUTOFILL_PRECEDENCE_NULL_SUITE_KEY, PROPERTY_L3MAPGEN_AUTOFILL_KEY);

        if (OFILE_NAMING_SCHEME_IFILE_REPLACE.equals(namingScheme.getValue())) {
            context.setComponentsEnabled(PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_IFILE_REPLACE_KEY, true);
            context.setComponentsEnabled(PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_IFILE_ORIGINAL_KEY, true);
        } else {
            context.setComponentsEnabled(PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_IFILE_REPLACE_KEY, false);
            context.setComponentsEnabled(PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_IFILE_ORIGINAL_KEY, false);
        }

        if (OFILE_NAMING_SCHEME_SUFFIX_NONE.equals(fieldsAdd.getValue())
                || OFILE_NAMING_SCHEME_SUFFIX_DEFAULT.equals(fieldsAdd.getValue())
                || OFILE_NAMING_SCHEME_SUFFIX_DEFAULT2.equals(fieldsAdd.getValue())
                || OFILE_NAMING_SCHEME_SUFFIX_DEFAULT3.equals(fieldsAdd.getValue())
                || OFILE_NAMING_SCHEME_SUFFIX_DEFAULT4.equals(fieldsAdd.getValue())
        ) {
            context.setComponentsEnabled(PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_SUFFIX1_KEY, false);
            context.setComponentsEnabled(PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_SUFFIX2_KEY, false);
        }

        if (OFILE_NAMING_SCHEME_SUFFIX1.equals(fieldsAdd.getValue())) {
            context.setComponentsEnabled(PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_SUFFIX1_KEY, true);
            context.setComponentsEnabled(PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_SUFFIX2_KEY, false);
        }

        if (OFILE_NAMING_SCHEME_SUFFIX2.equals(fieldsAdd.getValue())) {
            context.setComponentsEnabled(PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_SUFFIX1_KEY, false);
            context.setComponentsEnabled(PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_SUFFIX2_KEY, true);
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
            if (!property.getName().toUpperCase().contains(".FAV")) {
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
                            if (!property.getName().toUpperCase().contains(FAV)) {
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
     * Handles autofillall action
     *
     * @author Daniel Knowles
     */
    private void handleFillAll() {
        if (propertyValueChangeEventsEnabled) {
            propertyValueChangeEventsEnabled = false;
            try {
                boolean autoFillAllValue = autoFillAll.getValue();
                autoFillProduct.setValue(autoFillAllValue);
                autoFillOther.setValue(autoFillAllValue);
            } catch (ValidationException e) {
                e.printStackTrace();
            }
            propertyValueChangeEventsEnabled = true;
        }
    }








    /**
     * Handles favorite action
     *
     * @param context
     * @author Daniel Knowles
     */
    private void handleSetFav(BindingContext context, Property favSet, Property favProjection, Property favNSWE) {

        if (propertyValueChangeEventsEnabled) {
            propertyValueChangeEventsEnabled = false;
            try {
                if (favSet.getValue()) {
                    // set all favorites checkboxes to false then reset current favorite checkbox to true.
                    fav1SetToDefault.setValue(false);
                    fav2SetToDefault.setValue(false);
                    fav3SetToDefault.setValue(false);
                    fav4SetToDefault.setValue(false);
                    fav5SetToDefault.setValue(false);
                    favSet.setValue(true);

                    projection.setValue(EMPTY_STRING);
                    if (favProjection.getValue() != null) {
                        if (favProjection.getValue().toString() != null) {
                            projection.setValue(favProjection.getValue().toString().trim());
                        }
                    }


                    north.setValue(EMPTY_STRING);
                    south.setValue(EMPTY_STRING);
                    west.setValue(EMPTY_STRING);
                    east.setValue(EMPTY_STRING);
                    if (favNSWE.getValue() != null) {
                        if (favNSWE.getValue().toString() != null) {
                            String[] values = favNSWE.getValue().toString().split(",");

                            if (values != null && values.length == 4) {
                                north.setValue(values[0].trim());
                                south.setValue(values[1].trim());
                                west.setValue(values[2].trim());
                                east.setValue(values[3].trim());
                            }
                        }
                    }


                } else {
                    projection.setValue(projection.getDescriptor().getDefaultValue());
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
        return new HelpCtx("l3mapgen");
    }

    @SuppressWarnings("UnusedDeclaration")
    static class SeadasToolboxBean {


        @Preference(key = PROPERTY_L3MAPGEN_AUTOFILL_ALL_KEY,
                label = PROPERTY_L3MAPGEN_AUTOFILL_ALL_LABEL,
                description = PROPERTY_L3MAPGEN_AUTOFILL_ALL_TOOLTIP)
        boolean l2binAutofillAllDefault = PROPERTY_L3MAPGEN_AUTOFILL_ALL_DEFAULT;

        @Preference(key = PROPERTY_L3MAPGEN_AUTOFILL_PRODUCT_KEY,
                label = PROPERTY_L3MAPGEN_AUTOFILL_PRODUCT_LABEL,
                description = PROPERTY_L3MAPGEN_AUTOFILL_PRODUCT_TOOLTIP)
        boolean l3mapgenAutofillProductDefault = PROPERTY_L3MAPGEN_AUTOFILL_PRODUCT_DEFAULT;

        @Preference(key = PROPERTY_L3MAPGEN_AUTOFILL_KEY,
                label = PROPERTY_L3MAPGEN_AUTOFILL_LABEL,
                description = PROPERTY_L3MAPGEN_AUTOFILL_TOOLTIP)
        boolean l3mapgenAutofillDefault = PROPERTY_L3MAPGEN_AUTOFILL_DEFAULT;



        @Preference(key = PROPERTY_L3MAPGEN_AUTOFILL_PRECEDENCE_KEY,
                label = PROPERTY_L3MAPGEN_AUTOFILL_PRECEDENCE_LABEL,
                description = PROPERTY_L3MAPGEN_AUTOFILL_PRECEDENCE_TOOLTIP)
        boolean l3mapgenAutofillPrecedenceDefault = PROPERTY_L3MAPGEN_AUTOFILL_PRECEDENCE_DEFAULT;




        @Preference(key = PROPERTY_L3MAPGEN_PASS_ALL_KEY,
                label = PROPERTY_L3MAPGEN_PASS_ALL_LABEL,
                description = PROPERTY_L3MAPGEN_PASS_ALL_TOOLTIP)
        boolean l2binPassAllDefault = PROPERTY_L3MAPGEN_PASS_ALL_DEFAULT;


        @Preference(key = PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_SECTION_KEY,
                label = PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_SECTION_LABEL,
                description = PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_SECTION_TOOLTIP)
        boolean l3mapgen_OFILE_NAMING_SCHEME_SECTION = true;

        @Preference(key = PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_KEY,
                label = PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_LABEL,
                valueSet = {OFILE_NAMING_SCHEME_SIMPLE,OFILE_NAMING_SCHEME_OCSSW, OFILE_NAMING_SCHEME_OCSSW_SHORT, OFILE_NAMING_SCHEME_IFILE_REPLACE},
                description = PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_TOOLTIP)
        String l3mapgenOfileNamingSchemeDefault = PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_DEFAULT;

        @Preference(key = PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_IFILE_ORIGINAL_KEY,
                label = PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_IFILE_ORIGINAL_LABEL,
                description = PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_IFILE_ORIGINAL_TOOLTIP)
        String l3mapgenOfileNamingSchemeIfileOriginalDefault = PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_IFILE_ORIGINAL_DEFAULT;

        @Preference(key = PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_IFILE_REPLACE_KEY,
                label = PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_IFILE_REPLACE_LABEL,
                description = PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_IFILE_REPLACE_TOOLTIP)
        String l3mapgenOfileNamingSchemeIfileReplaceDefault = PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_IFILE_REPLACE_DEFAULT;

        @Preference(key = PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_SUFFIX_OPTIONS_KEY,
                label = PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_SUFFIX_OPTIONS_LABEL,
                valueSet = {OFILE_NAMING_SCHEME_SUFFIX_NONE,
                        OFILE_NAMING_SCHEME_SUFFIX1,
                        OFILE_NAMING_SCHEME_SUFFIX2,
                        OFILE_NAMING_SCHEME_SUFFIX_DEFAULT,
                        OFILE_NAMING_SCHEME_SUFFIX_DEFAULT2,
                        OFILE_NAMING_SCHEME_SUFFIX_DEFAULT3,
                        OFILE_NAMING_SCHEME_SUFFIX_DEFAULT4},
                description = PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_SUFFIX_OPTIONS_TOOLTIP)
        String l3mapgenOfileNamingSchemeFieldsAddDefault = PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_SUFFIX_OPTIONS_DEFAULT;

        @Preference(key = PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_SUFFIX1_KEY,
                label = PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_SUFFIX1_LABEL,
                description = PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_SUFFIX1_TOOLTIP)
        String l3mapgenOfileNamingSchemeSuffix1Default = PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_SUFFIX1_DEFAULT;

        @Preference(key = PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_SUFFIX2_KEY,
                label = PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_SUFFIX2_LABEL,
                description = PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_SUFFIX2_TOOLTIP)
        String l3mapgenOfileNamingSchemeSuffix2Default = PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_SUFFIX2_DEFAULT;





        @Preference(key = PROPERTY_L3MAPGEN_PARAMETERS_SECTION_KEY,
                label = PROPERTY_L3MAPGEN_PARAMETERS_SECTION_LABEL,
                description = PROPERTY_L3MAPGEN_PARAMETERS_SECTION_TOOLTIP)
        boolean l3mapgen_PROPERTY_L3MAPGEN_PARAMETERS_SECTION_KEY = true;






        @Preference(key = PROPERTY_L3MAPGEN_PRODUCT_KEY,
                label = PROPERTY_L3MAPGEN_PRODUCT_LABEL,
                description = PROPERTY_L3MAPGEN_PRODUCT_TOOLTIP)
        String l3mapgenProductDefault = PROPERTY_L3MAPGEN_PRODUCT_DEFAULT;

        @Preference(key = PROPERTY_L3MAPGEN_WAVELENGTH_3D_KEY,
                label = PROPERTY_L3MAPGEN_WAVELENGTH_3D_LABEL,
                description = PROPERTY_L3MAPGEN_WAVELENGTH_3D_TOOLTIP)
        String l3mapgenWavelength3dDefault = PROPERTY_L3MAPGEN_WAVELENGTH_3D_DEFAULT;

        @Preference(key = PROPERTY_L3MAPGEN_SUITE_KEY,
                label = PROPERTY_L3MAPGEN_SUITE_LABEL,
                description = PROPERTY_L3MAPGEN_SUITE_TOOLTIP)
        String l3mapgenSuiteDefault = PROPERTY_L3MAPGEN_SUITE_DEFAULT;





        @Preference(key = PROPERTY_L3MAPGEN_PARAMETERS_SPATIAL_IMAGE_MODE_SECTION_KEY,
                label = PROPERTY_L3MAPGEN_PARAMETERS_SPATIAL_IMAGE_MODE_SECTION_LABEL,
                description = PROPERTY_L3MAPGEN_PARAMETERS_SPATIAL_IMAGE_MODE_SECTION_TOOLTIP)
        boolean l3mapgenParametersSpatialSectionKey = true;

        @Preference(key = PROPERTY_L3MAPGEN_PROJECTION_KEY,
                label = PROPERTY_L3MAPGEN_PROJECTION_LABEL,
                description = PROPERTY_L3MAPGEN_PROJECTION_TOOLTIP)
        String l3mapgenProjectionDefault = PROPERTY_L3MAPGEN_PROJECTION_DEFAULT;


        @Preference(key = PROPERTY_L3MAPGEN_PROJECTION_SMI_REPLACEMENT_KEY,
                label = PROPERTY_L3MAPGEN_PROJECTION_SMI_REPLACEMENT_LABEL,
                description = PROPERTY_L3MAPGEN_PROJECTION_SMI_REPLACEMENT_TOOLTIP)
        String l3mapgenProjectionSmiReplacementDefault = PROPERTY_L3MAPGEN_PROJECTION_SMI_REPLACEMENT_DEFAULT;
        
        @Preference(key = PROPERTY_L3MAPGEN_RESOLUTION_KEY,
                label = PROPERTY_L3MAPGEN_RESOLUTION_LABEL,
                description = PROPERTY_L3MAPGEN_RESOLUTION_TOOLTIP)
        String l3mapgenResolutionDefault = PROPERTY_L3MAPGEN_RESOLUTION_DEFAULT;

        @Preference(key = PROPERTY_L3MAPGEN_WIDTH_KEY,
                label = PROPERTY_L3MAPGEN_WIDTH_LABEL,
                description = PROPERTY_L3MAPGEN_WIDTH_TOOLTIP)
        String l3mapgenWidthDefault = PROPERTY_L3MAPGEN_WIDTH_DEFAULT;



        @Preference(key = PROPERTY_L3MAPGEN_INTERP_KEY,
                label = PROPERTY_L3MAPGEN_INTERP_LABEL,
                valueSet = {EMPTY_STRING, "nearest", "bin", "area"},
                description = PROPERTY_L3MAPGEN_INTERP_TOOLTIP)
        String l3mapgenInterpDefault = PROPERTY_L3MAPGEN_INTERP_DEFAULT;



        @Preference(key = PROPERTY_L3MAPGEN_NORTH_KEY,
                label = PROPERTY_L3MAPGEN_NORTH_LABEL,
                description = PROPERTY_L3MAPGEN_NORTH_TOOLTIP)
        String l3mapgenNorthDefault = PROPERTY_L3MAPGEN_NORTH_DEFAULT;

        @Preference(key = PROPERTY_L3MAPGEN_SOUTH_KEY,
                label = PROPERTY_L3MAPGEN_SOUTH_LABEL,
                description = PROPERTY_L3MAPGEN_SOUTH_TOOLTIP)
        String l3mapgenSouthDefault = PROPERTY_L3MAPGEN_SOUTH_DEFAULT;

        @Preference(key = PROPERTY_L3MAPGEN_WEST_KEY,
                label = PROPERTY_L3MAPGEN_WEST_LABEL,
                description = PROPERTY_L3MAPGEN_WEST_TOOLTIP)
        String l3mapgenWestDefault = PROPERTY_L3MAPGEN_WEST_DEFAULT;

        @Preference(key = PROPERTY_L3MAPGEN_EAST_KEY,
                label = PROPERTY_L3MAPGEN_EAST_LABEL,
                description = PROPERTY_L3MAPGEN_EAST_TOOLTIP)
        String l3mapgenEastDefault = PROPERTY_L3MAPGEN_EAST_DEFAULT;


        @Preference(key = PROPERTY_L3MAPGEN_FUDGE_KEY,
                label = PROPERTY_L3MAPGEN_FUDGE_LABEL,
                description = PROPERTY_L3MAPGEN_FUDGE_TOOLTIP)
        String l3mapgenFudgeDefault = PROPERTY_L3MAPGEN_FUDGE_DEFAULT;






        @Preference(key = PROPERTY_L3MAPGEN_PARAMETERS_IMAGE_MODE_SECTION_KEY,
                label = PROPERTY_L3MAPGEN_PARAMETERS_IMAGE_MODE_SECTION_LABEL,
                description = PROPERTY_L3MAPGEN_PARAMETERS_IMAGE_MODE_SECTION_TOOLTIP)
        boolean l3mapgenParametersImageModeSection = true;


        @Preference(key = PROPERTY_L3MAPGEN_APPLY_PAL_KEY,
                label = PROPERTY_L3MAPGEN_APPLY_PAL_LABEL,
                description = PROPERTY_L3MAPGEN_APPLY_PAL_TOOLTIP,
                valueSet = {EMPTY_STRING, "TRUE", "FALSE"})
        String l3mapgenApplyPalDefault = PROPERTY_L3MAPGEN_APPLY_PAL_DEFAULT;


        @Preference(key = PROPERTY_L3MAPGEN_PALFILE_KEY,
                label = PROPERTY_L3MAPGEN_PALFILE_LABEL,
                description = PROPERTY_L3MAPGEN_PALFILE_TOOLTIP)
        String l3mapgenPalfileDefault = PROPERTY_L3MAPGEN_PALFILE_DEFAULT;


        @Preference(key = PROPERTY_L3MAPGEN_DATAMIN_KEY,
                label = PROPERTY_L3MAPGEN_DATAMIN_LABEL,
                description = PROPERTY_L3MAPGEN_DATAMIN_TOOLTIP)
        String l3mapgenDataminDefault = PROPERTY_L3MAPGEN_DATAMIN_DEFAULT;

        @Preference(key = PROPERTY_L3MAPGEN_DATAMAX_KEY,
                label = PROPERTY_L3MAPGEN_DATAMAX_LABEL,
                description = PROPERTY_L3MAPGEN_DATAMAX_TOOLTIP)
        String l3mapgenDatamaxDefault = PROPERTY_L3MAPGEN_DATAMAX_DEFAULT;

        @Preference(key = PROPERTY_L3MAPGEN_SCALE_TYPE_KEY,
                label = PROPERTY_L3MAPGEN_SCALE_TYPE_LABEL,
                valueSet = {EMPTY_STRING, "linear", "log", "arctan"},
                description = PROPERTY_L3MAPGEN_SCALE_TYPE_TOOLTIP)
        String l3mapgenScaleTypeDefault = PROPERTY_L3MAPGEN_SCALE_TYPE_DEFAULT;



        @Preference(key = PROPERTY_L3MAPGEN_USE_TRANSPARENCY_KEY,
                label = PROPERTY_L3MAPGEN_USE_TRANSPARENCY_LABEL,
                description = PROPERTY_L3MAPGEN_USE_TRANSPARENCY_TOOLTIP,
                valueSet = {EMPTY_STRING, "TRUE", "FALSE"})
        String l3mapgenUseTransparencyDefault = PROPERTY_L3MAPGEN_USE_TRANSPARENCY_DEFAULT;



        @Preference(key = PROPERTY_L3MAPGEN_MASK_LAND_KEY,
                label = PROPERTY_L3MAPGEN_MASK_LAND_LABEL,
                description = PROPERTY_L3MAPGEN_MASK_LAND_TOOLTIP,
                valueSet = {EMPTY_STRING, "TRUE", "FALSE"})
        String L3mapgen_MASK_LAND = PROPERTY_L3MAPGEN_MASK_LAND_DEFAULT;

        @Preference(key = PROPERTY_L3MAPGEN_RGB_LAND_KEY,
                label = PROPERTY_L3MAPGEN_RGB_LAND_LABEL,
                description = PROPERTY_L3MAPGEN_RGB_LAND_TOOLTIP)
        String l3mapgenRgbLandDefault = PROPERTY_L3MAPGEN_RGB_LAND_DEFAULT;

        @Preference(key = PROPERTY_L3MAPGEN_LAND_KEY,
                label = PROPERTY_L3MAPGEN_LAND_LABEL,
                description = PROPERTY_L3MAPGEN_LAND_TOOLTIP)
        String l3mapgenLandDefault = PROPERTY_L3MAPGEN_LAND_DEFAULT;

        @Preference(key = PROPERTY_L3MAPGEN_PARAMETERS_PSEUDO_RGB_SECTION_KEY,
                label = PROPERTY_L3MAPGEN_PARAMETERS_PSEUDO_RGB_SECTION_LABEL,
                description = PROPERTY_L3MAPGEN_PARAMETERS_PSEUDO_RGB_SECTION_TOOLTIP)
        boolean l3mapgenParametersPseudoRgbSection = true;


        @Preference(key = PROPERTY_L3MAPGEN_USE_RGB_KEY,
                label = PROPERTY_L3MAPGEN_USE_RGB_LABEL,
                description = PROPERTY_L3MAPGEN_USE_RGB_TOOLTIP,
                valueSet = {EMPTY_STRING, "TRUE", "FALSE"})
        String l3mapgenUseRgbDefault = PROPERTY_L3MAPGEN_USE_RGB_DEFAULT;



        @Preference(key = PROPERTY_L3MAPGEN_PRODUCT_RGB_KEY,
                label = PROPERTY_L3MAPGEN_PRODUCT_RGB_LABEL,
                description = PROPERTY_L3MAPGEN_PRODUCT_RGB_TOOLTIP)
        String l3mapgenProductRgbDefault = PROPERTY_L3MAPGEN_PRODUCT_RGB_DEFAULT;




        @Preference(key = PROPERTY_L3MAPGEN_PARAMETERS_GENERAL_SECTION_KEY,
                label = PROPERTY_L3MAPGEN_PARAMETERS_GENERAL_SECTION_LABEL,
                description = PROPERTY_L3MAPGEN_PARAMETERS_GENERAL_SECTION_TOOLTIP)
        boolean l3mapgen_PROPERTY_L3MAPGEN_PARAMETERS_GENERAL_SECTION_KEY = true;



        @Preference(key = PROPERTY_L3MAPGEN_OFORMAT_KEY,
                label = PROPERTY_L3MAPGEN_OFORMAT_LABEL,
                valueSet = {EMPTY_STRING, "netCDF4", "HDF4", "png", "ppm", "TIFF"},
                description = PROPERTY_L3MAPGEN_OFORMAT_TOOLTIP)
        String l3mapgenOformatDefault = PROPERTY_L3MAPGEN_OFORMAT_DEFAULT;


        @Preference(key = PROPERTY_L3MAPGEN_NUM_CACHE_KEY,
                label = PROPERTY_L3MAPGEN_NUM_CACHE_LABEL,
                description = PROPERTY_L3MAPGEN_NUM_CACHE_TOOLTIP)
        String l3mapgenNumCacheDefault = PROPERTY_L3MAPGEN_NUM_CACHE_DEFAULT;




        @Preference(key = PROPERTY_L3MAPGEN_FAV_PROJECTIONS_SECTION_KEY,
                label = PROPERTY_L3MAPGEN_FAV_PROJECTIONS_SECTION_LABEL,
                description = PROPERTY_L3MAPGEN_FAV_PROJECTIONS_SECTION_TOOLTIP)
        boolean L3mapgen_FAV_PROJECTIONS_SECTION = true;


        @Preference(label = PROPERTY_L3MAPGEN_FAV1_SET_AS_DEFAULT_LABEL,
                key = PROPERTY_L3MAPGEN_FAV1_SET_AS_DEFAULT_KEY,
                description = PROPERTY_L3MAPGEN_FAV1_SET_AS_DEFAULT_TOOLTIP)
        boolean L3mapgen_FAV1_PROJECTION_USE_DEFAULT = PROPERTY_L3MAPGEN_FAV1_SET_AS_DEFAULT_DEFAULT;

        @Preference(label = PROPERTY_L3MAPGEN_FAV1_PROJECTION_LABEL,
                key = PROPERTY_L3MAPGEN_FAV1_PROJECTION_KEY,
                description = PROPERTY_L3MAPGEN_FAV1_PROJECTION_TOOLTIP)
        String L3mapgen_FAV1_PROJECTION_DEFAULT = PROPERTY_L3MAPGEN_FAV1_PROJECTION_DEFAULT;

        @Preference(label = PROPERTY_L3MAPGEN_FAV1_PROJECTION_DESC_LABEL,
                key = PROPERTY_L3MAPGEN_FAV1_PROJECTION_DESC_KEY,
                description = PROPERTY_L3MAPGEN_FAV1_PROJECTION_DESC_TOOLTIP)
        String L3mapgen_FAV1_PROJECTION_DESC_DEFAULT = PROPERTY_L3MAPGEN_FAV1_PROJECTION_DESC_DEFAULT;

        @Preference(label = PROPERTY_L3MAPGEN_FAV1_NSWE_LABEL,
                key = PROPERTY_L3MAPGEN_FAV1_NSWE_KEY,
                description = PROPERTY_L3MAPGEN_FAV1_NSWE_TOOLTIP)
        String L3mapgen_FAV1_NSWE_DEFAULT = PROPERTY_L3MAPGEN_FAV1_NSWE_DEFAULT;



        @Preference(label = PROPERTY_L3MAPGEN_FAV2_SET_AS_DEFAULT_LABEL,
                key = PROPERTY_L3MAPGEN_FAV2_SET_AS_DEFAULT_KEY,
                description = PROPERTY_L3MAPGEN_FAV2_SET_AS_DEFAULT_TOOLTIP)
        boolean L3mapgen_FAV2_PROJECTION_USE_DEFAULT = PROPERTY_L3MAPGEN_FAV2_SET_AS_DEFAULT_DEFAULT;

        @Preference(label = PROPERTY_L3MAPGEN_FAV2_PROJECTION_LABEL,
                key = PROPERTY_L3MAPGEN_FAV2_PROJECTION_KEY,
                description = PROPERTY_L3MAPGEN_FAV2_PROJECTION_TOOLTIP)
        String L3mapgen_FAV2_PROJECTION_DEFAULT = PROPERTY_L3MAPGEN_FAV2_PROJECTION_DEFAULT;

        @Preference(label = PROPERTY_L3MAPGEN_FAV2_PROJECTION_DESC_LABEL,
                key = PROPERTY_L3MAPGEN_FAV2_PROJECTION_DESC_KEY,
                description = PROPERTY_L3MAPGEN_FAV2_PROJECTION_DESC_TOOLTIP)
        String L3mapgen_FAV2_PROJECTION_DESC_DEFAULT = PROPERTY_L3MAPGEN_FAV2_PROJECTION_DESC_DEFAULT;

        @Preference(label = PROPERTY_L3MAPGEN_FAV2_NSWE_LABEL,
                key = PROPERTY_L3MAPGEN_FAV2_NSWE_KEY,
                description = PROPERTY_L3MAPGEN_FAV2_NSWE_TOOLTIP)
        String L3mapgen_FAV2_NSWE_DEFAULT = PROPERTY_L3MAPGEN_FAV2_NSWE_DEFAULT;

        @Preference(label = PROPERTY_L3MAPGEN_FAV3_SET_AS_DEFAULT_LABEL,
                key = PROPERTY_L3MAPGEN_FAV3_SET_AS_DEFAULT_KEY,
                description = PROPERTY_L3MAPGEN_FAV3_SET_AS_DEFAULT_TOOLTIP)
        boolean L3mapgen_FAV3_PROJECTION_USE_DEFAULT = PROPERTY_L3MAPGEN_FAV3_SET_AS_DEFAULT_DEFAULT;

        @Preference(label = PROPERTY_L3MAPGEN_FAV3_PROJECTION_LABEL,
                key = PROPERTY_L3MAPGEN_FAV3_PROJECTION_KEY,
                description = PROPERTY_L3MAPGEN_FAV3_PROJECTION_TOOLTIP)
        String L3mapgen_FAV3_PROJECTION_DEFAULT = PROPERTY_L3MAPGEN_FAV3_PROJECTION_DEFAULT;

        @Preference(label = PROPERTY_L3MAPGEN_FAV3_PROJECTION_DESC_LABEL,
                key = PROPERTY_L3MAPGEN_FAV3_PROJECTION_DESC_KEY,
                description = PROPERTY_L3MAPGEN_FAV3_PROJECTION_DESC_TOOLTIP)
        String L3mapgen_FAV3_PROJECTION_DESC_DEFAULT = PROPERTY_L3MAPGEN_FAV3_PROJECTION_DESC_DEFAULT;

        @Preference(label = PROPERTY_L3MAPGEN_FAV3_NSWE_LABEL,
                key = PROPERTY_L3MAPGEN_FAV3_NSWE_KEY,
                description = PROPERTY_L3MAPGEN_FAV3_NSWE_TOOLTIP)
        String L3mapgen_FAV3_NSWE_DEFAULT = PROPERTY_L3MAPGEN_FAV3_NSWE_DEFAULT;




        @Preference(label = PROPERTY_L3MAPGEN_FAV4_SET_AS_DEFAULT_LABEL,
                key = PROPERTY_L3MAPGEN_FAV4_SET_AS_DEFAULT_KEY,
                description = PROPERTY_L3MAPGEN_FAV4_SET_AS_DEFAULT_TOOLTIP)
        boolean L3mapgen_FAV4_PROJECTION_USE_DEFAULT = PROPERTY_L3MAPGEN_FAV4_SET_AS_DEFAULT_DEFAULT;

        @Preference(label = PROPERTY_L3MAPGEN_FAV4_PROJECTION_LABEL,
                key = PROPERTY_L3MAPGEN_FAV4_PROJECTION_KEY,
                description = PROPERTY_L3MAPGEN_FAV4_PROJECTION_TOOLTIP)
        String L3mapgen_FAV4_PROJECTION_DEFAULT = PROPERTY_L3MAPGEN_FAV4_PROJECTION_DEFAULT;

        @Preference(label = PROPERTY_L3MAPGEN_FAV4_PROJECTION_DESC_LABEL,
                key = PROPERTY_L3MAPGEN_FAV4_PROJECTION_DESC_KEY,
                description = PROPERTY_L3MAPGEN_FAV4_PROJECTION_DESC_TOOLTIP)
        String L3mapgen_FAV4_PROJECTION_DESC_DEFAULT = PROPERTY_L3MAPGEN_FAV4_PROJECTION_DESC_DEFAULT;

        @Preference(label = PROPERTY_L3MAPGEN_FAV4_NSWE_LABEL,
                key = PROPERTY_L3MAPGEN_FAV4_NSWE_KEY,
                description = PROPERTY_L3MAPGEN_FAV4_NSWE_TOOLTIP)
        String L3mapgen_FAV4_NSWE_DEFAULT = PROPERTY_L3MAPGEN_FAV4_NSWE_DEFAULT;





        @Preference(label = PROPERTY_L3MAPGEN_FAV5_SET_AS_DEFAULT_LABEL,
                key = PROPERTY_L3MAPGEN_FAV5_SET_AS_DEFAULT_KEY,
                description = PROPERTY_L3MAPGEN_FAV5_SET_AS_DEFAULT_TOOLTIP)
        boolean L3mapgen_FAV5_PROJECTION_USE_DEFAULT = PROPERTY_L3MAPGEN_FAV5_SET_AS_DEFAULT_DEFAULT;

        @Preference(label = PROPERTY_L3MAPGEN_FAV5_PROJECTION_LABEL,
                key = PROPERTY_L3MAPGEN_FAV5_PROJECTION_KEY,
                description = PROPERTY_L3MAPGEN_FAV5_PROJECTION_TOOLTIP)
        String L3mapgen_FAV5_PROJECTION_DEFAULT = PROPERTY_L3MAPGEN_FAV5_PROJECTION_DEFAULT;

        @Preference(label = PROPERTY_L3MAPGEN_FAV5_PROJECTION_DESC_LABEL,
                key = PROPERTY_L3MAPGEN_FAV5_PROJECTION_DESC_KEY,
                description = PROPERTY_L3MAPGEN_FAV5_PROJECTION_DESC_TOOLTIP)
        String L3mapgen_FAV5_PROJECTION_DESC_DEFAULT = PROPERTY_L3MAPGEN_FAV5_PROJECTION_DESC_DEFAULT;

        @Preference(label = PROPERTY_L3MAPGEN_FAV5_NSWE_LABEL,
                key = PROPERTY_L3MAPGEN_FAV5_NSWE_KEY,
                description = PROPERTY_L3MAPGEN_FAV5_NSWE_TOOLTIP)
        String L3mapgen_FAV5_NSWE_DEFAULT = PROPERTY_L3MAPGEN_FAV5_NSWE_DEFAULT;





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



    public static boolean getPreferencePassAll() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyBool(PROPERTY_L3MAPGEN_PASS_ALL_KEY, PROPERTY_L3MAPGEN_PASS_ALL_DEFAULT);
    }


    public static boolean getPreferenceAutoFillAll() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyBool(PROPERTY_L3MAPGEN_AUTOFILL_ALL_KEY, PROPERTY_L3MAPGEN_AUTOFILL_ALL_DEFAULT);
    }

    public static boolean getPreferenceAutoFillOther() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyBool(OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_AUTOFILL_KEY, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_AUTOFILL_DEFAULT);
    }

    public static boolean getPreferenceAutoFillProduct() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyBool(OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_AUTOFILL_PRODUCT_KEY, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_AUTOFILL_PRODUCT_DEFAULT);
    }

    public static boolean getPreferenceAutoFillPrecedence() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyBool(OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_AUTOFILL_PRECEDENCE_KEY, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_AUTOFILL_PRECEDENCE_DEFAULT);
    }


    public static String getPreferenceProduct() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_PRODUCT_KEY, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_PRODUCT_DEFAULT);
    }

    public static String getPreferenceWavelength3D() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_WAVELENGTH_3D_KEY, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_WAVELENGTH_3D_DEFAULT);
    }

    public static String getPreferenceSuite() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_SUITE_KEY, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_SUITE_DEFAULT);
    }


    public static String getPreferenceNumCache() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_NUM_CACHE_KEY, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_NUM_CACHE_DEFAULT);
    }


    public static String getPreferenceOformat() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_OFORMAT_KEY, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_OFORMAT_DEFAULT);
    }



    public static String getPreferenceProjection() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_PROJECTION_KEY, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_PROJECTION_DEFAULT);
    }

    public static String getPreferenceProjectionSmiReplacement() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_PROJECTION_SMI_REPLACEMENT_KEY, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_PROJECTION_SMI_REPLACEMENT_DEFAULT);
    }

    public static String getPreferenceResolution() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_RESOLUTION_KEY, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_RESOLUTION_DEFAULT);
    }

    public static String getPreferenceInterp() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_INTERP_KEY, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_INTERP_DEFAULT);
    }


    public static String getPreferenceFudge() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_FUDGE_KEY, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_FUDGE_DEFAULT);
    }

    public static String getPreferenceWidth() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_WIDTH_KEY, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_WIDTH_DEFAULT);
    }


    public static String getPreferenceNorth() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_NORTH_KEY, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_NORTH_DEFAULT);
    }

    public static String getPreferenceSouth() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_SOUTH_KEY, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_SOUTH_DEFAULT);
    }

    public static String getPreferenceWest() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_WEST_KEY, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_WEST_DEFAULT);
    }

    public static String getPreferenceEast() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_EAST_KEY, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_EAST_DEFAULT);
    }



    public static String getPreferenceApplyPal() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_APPLY_PAL_KEY, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_APPLY_PAL_DEFAULT);
    }


    public static String getPreferencePalfile() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_PALFILE_KEY, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_PALFILE_DEFAULT);
    }



    public static String getPreferenceDataMin() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_DATAMIN_KEY, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_DATAMIN_DEFAULT);
    }



    public static String getPreferenceDataMax() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_DATAMAX_KEY, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_DATAMAX_DEFAULT);
    }




    public static String getPreferenceScaleType() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_SCALE_TYPE_KEY, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_SCALE_TYPE_DEFAULT);
    }



    public static String getPreferenceUseTransparency() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(PROPERTY_L3MAPGEN_USE_TRANSPARENCY_KEY, PROPERTY_L3MAPGEN_USE_TRANSPARENCY_DEFAULT);
    }



    public static String getPreferenceMaskLand() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_MASK_LAND_KEY, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_MASK_LAND_DEFAULT);
    }

    public static String getPreferenceRGBLand() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_RGB_LAND_KEY, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_RGB_LAND_DEFAULT);
    }

    public static String getPreferenceLand() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_LAND_KEY, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_LAND_DEFAULT);
    }


    public static String getPreferenceUseRGB() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_USE_RGB_KEY, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_USE_RGB_DEFAULT);
    }

    public static String getPreferenceProductRGB() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_PRODUCT_RGB_KEY, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_PRODUCT_RGB_DEFAULT);
    }



    public static String getPreferenceOfileNamingScheme() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_KEY, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_DEFAULT);
    }


    public static String getPreferenceOfileNamingSchemeSuffixOptions() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_SUFFIX_OPTIONS_KEY, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_SUFFIX_OPTIONS_DEFAULT);
    }


    public static String getPreferenceOfileNamingSchemeIfileReplace() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_IFILE_REPLACE_KEY, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_IFILE_REPLACE_DEFAULT);
    }

    public static String getPreferenceOfileNamingSchemeIfileOriginal() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_IFILE_ORIGINAL_KEY, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_IFILE_ORIGINAL_DEFAULT);
    }



    public static String getPreferenceOfileNamingSchemeSuffix1() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_SUFFIX1_KEY, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_SUFFIX1_DEFAULT);
    }

    public static String getPreferenceOfileNamingSchemeSuffix2() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_SUFFIX2_KEY, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_OFILE_NAMING_SCHEME_SUFFIX2_DEFAULT);
    }



    public static String getPreferenceFAV1Projection() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_FAV1_PROJECTION_KEY, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_FAV1_PROJECTION_DEFAULT);
    }

    public static String getPreferenceFAV1ProjectionDescription() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_FAV1_PROJECTION_DESC_KEY, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_FAV1_PROJECTION_DESC_DEFAULT);
    }



    public static String getPreferenceFAV2Projection() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_FAV2_PROJECTION_KEY, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_FAV2_PROJECTION_DEFAULT);
    }

    public static String getPreferenceFAV2ProjectionDescription() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_FAV2_PROJECTION_DESC_KEY, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_FAV2_PROJECTION_DESC_DEFAULT);
    }

    public static String getPreferenceFAV3Projection() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_FAV3_PROJECTION_KEY, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_FAV3_PROJECTION_DEFAULT);
    }

    public static String getPreferenceFAV3ProjectionDescription() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_FAV3_PROJECTION_DESC_KEY, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_FAV3_PROJECTION_DESC_DEFAULT);
    }


    public static String getPreferenceFAV4Projection() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_FAV4_PROJECTION_KEY, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_FAV4_PROJECTION_DEFAULT);
    }

    public static String getPreferenceFAV4ProjectionDescription() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_FAV4_PROJECTION_DESC_KEY, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_FAV4_PROJECTION_DESC_DEFAULT);
    }


    public static String getPreferenceFAV5Projection() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_FAV5_PROJECTION_KEY, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_FAV5_PROJECTION_DEFAULT);
    }

    public static String getPreferenceFAV5ProjectionDescription() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_FAV5_PROJECTION_DESC_KEY, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_FAV5_PROJECTION_DESC_DEFAULT);
    }






}
