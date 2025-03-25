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
import org.esa.snap.ui.GridBagUtils;
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
        id = "L2bin")
@org.openide.util.NbBundle.Messages({
        "Options_DisplayName_OCSSW_L2bin=L2bin",
        "Options_Keywords_OCSSW_L2bin=seadas, ocssw, l2bin"
})
public final class OCSSW_L2binController extends DefaultConfigController {

    private static final String FAV = ".favorites";

    Property flaguse;
    Property l3bprod;
    Property output_wavelengths;
    Property selector;
    Property suite;
    Property resolution;
    Property north;
    Property south;
    Property west;
    Property east;

    Property favoriteSettingsSection;

    Property fav1SetToDefault;
    Property fav1FlagUse;
    Property fav1_l3bprod;
    Property fav1_output_wavelengths;
    Property fav1NSWE;
    Property fav1_suite;
    Property fav1_resolution;

    Property namingScheme;
    Property fieldsAdd;
    
    Property restoreDefaults;

    boolean propertyValueChangeEventsEnabled = true;

    // todo compare param tooltips here and with .xml file and with ocssw help
    // todo update help page


    public static final String  OFILE_NAMING_SCHEME_SIMPLE = "output";
    public static final String OFILE_NAMING_SCHEME_IFILE_PLUS_SUFFIX = "IFILE + SUFFIX";
    public static final String  OFILE_NAMING_SCHEME_OCSSW_SHORT = "OCSSW (do not derive time field)";
    public static final String  OFILE_NAMING_SCHEME_OCSSW = "OCSSW";
    public static final String OFILE_NAMING_SCHEME_IFILE_REPLACE = "IFILE (String-Replace)";


    public static final String OFILE_NAMING_SCHEME_SUFFIX_NONE = "No Suffix";
    public static final String OFILE_NAMING_SCHEME_SUFFIX1 = "Suffix Custom 1";
    public static final String OFILE_NAMING_SCHEME_SUFFIX2 = "Suffix Custom 2";
    public static final String OFILE_NAMING_SCHEME_SUFFIX_DEFAULT = "[.l3bprod][.resolution]";
    public static final String OFILE_NAMING_SCHEME_SUFFIX_DEFAULT2 = "[_l3bprod][_resolution]";
    public static final String OFILE_NAMING_SCHEME_SUFFIX_DEFAULT3 = "[.l3bprod][.resolution][.prodtype][.suite]";
    public static final String OFILE_NAMING_SCHEME_SUFFIX_DEFAULT4 = "[_l3bprod][_resolution][_prodtype][_suite]";
    
    
    // Preferences property prefix
    private static final String PROPERTY_L2BIN_ROOT_KEY = SeadasToolboxDefaults.PROPERTY_SEADAS_ROOT_KEY + ".l2bin";


    public static final String PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SECTION_KEY = PROPERTY_L2BIN_ROOT_KEY + ".ofile.naming.scheme.section";
    public static final String PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SECTION_LABEL = "Ofile Naming Scheme";
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
            "[.l3bprod] : adds 'l3bprod' field with '.' as delimiter<br>" +
            "[.resolution] : adds 'resolution' field with '.' as delimiter<br>" +
            "[.prodtype] : adds 'prodtype' field with '.' as delimiter<br>" +
            "[.suite] : adds 'suite' field with '.' as delimiter<br>" +
            "</html>";

    public static final String PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SUFFIX1_KEY = PROPERTY_L2BIN_ROOT_KEY + ".ofile.naming.scheme.suffix1";
    public static final String PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SUFFIX1_LABEL = OFILE_NAMING_SCHEME_SUFFIX1;
    public static final String PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SUFFIX1_TOOLTIP = SUFFIX_LIST_TOOLTIPS;
    public static final String PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SUFFIX1_DEFAULT = "[.l3bprod][.resolution]";


    public static final String PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SUFFIX2_KEY = PROPERTY_L2BIN_ROOT_KEY + ".ofile.naming.scheme.suffix2";
    public static final String PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SUFFIX2_LABEL = OFILE_NAMING_SCHEME_SUFFIX2;
    public static final String PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SUFFIX2_TOOLTIP = SUFFIX_LIST_TOOLTIPS;
    public static final String PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SUFFIX2_DEFAULT = "[.l3bprod][.resolution][.prodtype][.suite]";
    
    
    
    
    public static final String PROPERTY_L2BIN_L3BPROD_KEY = PROPERTY_L2BIN_ROOT_KEY + ".l3bprod";
    public static final String PROPERTY_L2BIN_L3BPROD_LABEL = "l3bprod";
    public static final String PROPERTY_L2BIN_L3BPROD_TOOLTIP = "Product (or product list)";
    public static final String PROPERTY_L2BIN_L3BPROD_DEFAULT = "Rrs";

    public static final String PROPERTY_L2BIN_OUTPUT_WAVELENGTHS_KEY = PROPERTY_L2BIN_ROOT_KEY + ".output_wavelengths";
    public static final String PROPERTY_L2BIN_OUTPUT_WAVELENGTHS_LABEL = "output_wavelengths";
    public static final String PROPERTY_L2BIN_OUTPUT_WAVELENGTHS_TOOLTIP = "Comma delimited list of 3D wavelengths";
    public static final String PROPERTY_L2BIN_OUTPUT_WAVELENGTHS_DEFAULT = "";

    public static final String PROPERTY_L2BIN_SUITE_KEY = PROPERTY_L2BIN_ROOT_KEY + ".suite";
    public static final String PROPERTY_L2BIN_SUITE_LABEL = "suite";
    public static final String PROPERTY_L2BIN_SUITE_TOOLTIP = "Product Suite";
    public static final String PROPERTY_L2BIN_SUITE_DEFAULT = "DTOCEAN";

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

    public static final String PROPERTY_L2BIN_RESOLUTION_KEY = PROPERTY_L2BIN_ROOT_KEY + ".resolution";
    public static final String PROPERTY_L2BIN_RESOLUTION_LABEL = "resolution";
    public static final String PROPERTY_L2BIN_RESOLUTION_TOOLTIP = "Bin resolution";
    public static final String PROPERTY_L2BIN_RESOLUTION_DEFAULT = "18";

    public static final String PROPERTY_L2BIN_AREA_WEIGHTING_KEY = PROPERTY_L2BIN_ROOT_KEY + ".area_weighting";
    public static final String PROPERTY_L2BIN_AREA_WEIGHTING_LABEL = "area_weighting";
    public static final String PROPERTY_L2BIN_AREA_WEIGHTING_TOOLTIP = "Area Weighting";
    public static final String PROPERTY_L2BIN_AREA_WEIGHTING_DEFAULT = "";

    public static final String PROPERTY_L2BIN_FLAGUSE_KEY = PROPERTY_L2BIN_ROOT_KEY + ".flaguse";
    public static final String PROPERTY_L2BIN_FLAGUSE_LABEL = "flaguse";
    public static final String PROPERTY_L2BIN_FLAGUSE_TOOLTIP = "Flags to use for binning";
    public static final String PROPERTY_L2BIN_FLAGUSE_DEFAULT = "";

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


    public static final String PROPERTY_L2BIN_FLAGUSE_SELECTOR_KEY = PROPERTY_L2BIN_ROOT_KEY + ".flaguse.selector.disable";
    public static final String PROPERTY_L2BIN_FLAGUSE_SELECTOR_LABEL = "Autofill 'flaguse' with suite defaults";
    public static final String PROPERTY_L2BIN_FLAGUSE_SELECTOR_TOOLTIP = "<html>Autofills flaguse with the suite defaults. <br> Note: if flaguse is empty then suite defaults are used.</html>";
    public static final boolean PROPERTY_L2BIN_FLAGUSE_SELECTOR_DEFAULT = true;



    // Favorites/Stored Custom Settings

    public static final String PROPERTY_L2BIN_FAVORITE_SETTINGS_SECTION_KEY = PROPERTY_L2BIN_ROOT_KEY + "favorite.settings.section";
    public static final String PROPERTY_L2BIN_FAVORITE_SETTINGS_SECTION_LABEL = "Favorite Settings";
    public static final String PROPERTY_L2BIN_FAVORITE_SETTINGS_SECTION_TOOLTIP = "Store and Load favorite settings";

    private static final String INDENTATION_SPACES = "           ";

    public static final String PROPERTY_L2BIN_FAV1_LOAD_KEY = PROPERTY_L2BIN_ROOT_KEY + FAV + ".1.load";
    public static final String PROPERTY_L2BIN_FAV1_LOAD_LABEL = "1. Set as Default";
    public static final String PROPERTY_L2BIN_FAV1_LOAD_TOOLTIP = "Loads settings into the preferences";
    public static final boolean PROPERTY_L2BIN_FAV1_LOAD_DEFAULT = false;

    public static final String PROPERTY_L2BIN_FAV1_DESCRIPTION_KEY = PROPERTY_L2BIN_ROOT_KEY + FAV + ".1.description";
    public static final String PROPERTY_L2BIN_FAV1_DESCRIPTION_LABEL = INDENTATION_SPACES + "Description/Notes";
    public static final String PROPERTY_L2BIN_FAV1_DESCRIPTION_TOOLTIP = "Description/notes for stored setting";
    public static final String PROPERTY_L2BIN_FAV1_DESCRIPTION_DEFAULT = "My sample of custom flags";

    public static final String PROPERTY_L2BIN_FAV1_FLAGUSE_KEY = PROPERTY_L2BIN_ROOT_KEY + FAV + ".1.flaguse";
    public static final String PROPERTY_L2BIN_FAV1_FLAGUSE_LABEL = INDENTATION_SPACES + "flaguse";
    public static final String PROPERTY_L2BIN_FAV1_FLAGUSE_TOOLTIP = "Favorites 1: flaguse";
    public static final String PROPERTY_L2BIN_FAV1_FLAGUSE_DEFAULT = "NAVFAIL CLDICE STRAYLIGHT";

    public static final String PROPERTY_L2BIN_FAV1_L3BPROD_KEY = PROPERTY_L2BIN_ROOT_KEY + FAV + ".1.l3bprod";
    public static final String PROPERTY_L2BIN_FAV1_L3BPROD_LABEL = INDENTATION_SPACES + "l3bprod";
    public static final String PROPERTY_L2BIN_FAV1_L3BPROD_TOOLTIP = "Favorites 1: l3bprod";
    public static final String PROPERTY_L2BIN_FAV1_L3BPROD_DEFAULT = "";

    public static final String PROPERTY_L2BIN_FAV1_OUTPUT_WAVELENGTHS_KEY = PROPERTY_L2BIN_ROOT_KEY + FAV + ".1.output_wavelengths";
    public static final String PROPERTY_L2BIN_FAV1_OUTPUT_WAVELENGTHS_LABEL = INDENTATION_SPACES + "output_wavelengths";
    public static final String PROPERTY_L2BIN_FAV1_OUTPUT_WAVELENGTHS_TOOLTIP = "Favorites 1: output_wavelengths";
    public static final String PROPERTY_L2BIN_FAV1_OUTPUT_WAVELENGTHS_DEFAULT = "";

    public static final String PROPERTY_L2BIN_FAV1_SUITE_KEY = PROPERTY_L2BIN_ROOT_KEY + FAV + ".1.suite";
    public static final String PROPERTY_L2BIN_FAV1_SUITE_LABEL = INDENTATION_SPACES + "suite";
    public static final String PROPERTY_L2BIN_FAV1_SUITE_TOOLTIP = "Favorites 1: suite";
    public static final String PROPERTY_L2BIN_FAV1_SUITE_DEFAULT = "";

    public static final String PROPERTY_L2BIN_FAV1_RESOLUTION_KEY = PROPERTY_L2BIN_ROOT_KEY + FAV + ".1.resolution";
    public static final String PROPERTY_L2BIN_FAV1_RESOLUTION_LABEL = INDENTATION_SPACES + "resolution";
    public static final String PROPERTY_L2BIN_FAV1_RESOLUTION_TOOLTIP = "Favorites 1: resolution";
    public static final String PROPERTY_L2BIN_FAV1_RESOLUTION_DEFAULT = "";

    public static final String PROPERTY_L2BIN_FAV1_NSWE_KEY = PROPERTY_L2BIN_ROOT_KEY + FAV + ".1.nswe.bounds";
    public static final String PROPERTY_L2BIN_FAV1_NSWE_LABEL = INDENTATION_SPACES + "N,S,W,E Bounds";
    public static final String PROPERTY_L2BIN_FAV1_NSWE_TOOLTIP = "Geographic boundaries.  Comma delimited. Format N,S,W,E. ";
    public static final String PROPERTY_L2BIN_FAV1_NSWE_DEFAULT = "45,23,34,35";



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


        initPropertyDefaults(context, PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SECTION_KEY, true);
        namingScheme = initPropertyDefaults(context, PROPERTY_L2BIN_OFILE_NAMING_SCHEME_KEY, PROPERTY_L2BIN_OFILE_NAMING_SCHEME_DEFAULT);
        fieldsAdd = initPropertyDefaults(context, PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SUFFIX_OPTIONS_KEY, PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SUFFIX_OPTIONS_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SUFFIX1_KEY, PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SUFFIX1_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SUFFIX2_KEY, PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SUFFIX2_DEFAULT);
        
        
        //
        // Initialize the default value contained within each property descriptor
        // This is done so subsequently the restoreDefaults actions can be performed
        //

        l3bprod = initPropertyDefaults(context, PROPERTY_L2BIN_L3BPROD_KEY, PROPERTY_L2BIN_L3BPROD_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L2BIN_PRODTYPE_KEY, PROPERTY_L2BIN_PRODTYPE_DEFAULT);
        resolution = initPropertyDefaults(context, PROPERTY_L2BIN_RESOLUTION_KEY, PROPERTY_L2BIN_RESOLUTION_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L2BIN_AREA_WEIGHTING_KEY, PROPERTY_L2BIN_AREA_WEIGHTING_DEFAULT);
        flaguse = initPropertyDefaults(context, PROPERTY_L2BIN_FLAGUSE_KEY, PROPERTY_L2BIN_FLAGUSE_DEFAULT);
        north = initPropertyDefaults(context, PROPERTY_L2BIN_LATNORTH_KEY, PROPERTY_L2BIN_LATNORTH_DEFAULT);
        south = initPropertyDefaults(context, PROPERTY_L2BIN_LATSOUTH_KEY, PROPERTY_L2BIN_LATSOUTH_DEFAULT);
        west = initPropertyDefaults(context, PROPERTY_L2BIN_LONWEST_KEY, PROPERTY_L2BIN_LONWEST_DEFAULT);
        east = initPropertyDefaults(context, PROPERTY_L2BIN_LONEAST_KEY, PROPERTY_L2BIN_LONEAST_DEFAULT);
        selector = initPropertyDefaults(context, PROPERTY_L2BIN_FLAGUSE_SELECTOR_KEY, PROPERTY_L2BIN_FLAGUSE_SELECTOR_DEFAULT);
        output_wavelengths = initPropertyDefaults(context, PROPERTY_L2BIN_OUTPUT_WAVELENGTHS_KEY, PROPERTY_L2BIN_OUTPUT_WAVELENGTHS_DEFAULT);
        suite = initPropertyDefaults(context, PROPERTY_L2BIN_SUITE_KEY, PROPERTY_L2BIN_SUITE_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L2BIN_COMPOSITE_PROD_KEY, PROPERTY_L2BIN_COMPOSITE_PROD_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L2BIN_COMPOSITE_SCHEME_KEY, PROPERTY_L2BIN_COMPOSITE_SCHEME_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L2BIN_ROW_GROUP_KEY, PROPERTY_L2BIN_ROW_GROUP_DEFAULT);



        favoriteSettingsSection = initPropertyDefaults(context, PROPERTY_L2BIN_FAVORITE_SETTINGS_SECTION_KEY, true);

        fav1SetToDefault = initPropertyDefaults(context, PROPERTY_L2BIN_FAV1_LOAD_KEY, PROPERTY_L2BIN_FAV1_LOAD_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L2BIN_FAV1_DESCRIPTION_KEY, PROPERTY_L2BIN_FAV1_DESCRIPTION_DEFAULT);
        fav1FlagUse = initPropertyDefaults(context, PROPERTY_L2BIN_FAV1_FLAGUSE_KEY, PROPERTY_L2BIN_FAV1_FLAGUSE_DEFAULT);
        fav1_l3bprod = initPropertyDefaults(context, PROPERTY_L2BIN_FAV1_L3BPROD_KEY, PROPERTY_L2BIN_FAV1_L3BPROD_DEFAULT);
        fav1_output_wavelengths = initPropertyDefaults(context, PROPERTY_L2BIN_FAV1_OUTPUT_WAVELENGTHS_KEY, PROPERTY_L2BIN_FAV1_OUTPUT_WAVELENGTHS_DEFAULT);
        fav1NSWE = initPropertyDefaults(context, PROPERTY_L2BIN_FAV1_NSWE_KEY, PROPERTY_L2BIN_FAV1_NSWE_DEFAULT);
        fav1_suite = initPropertyDefaults(context, PROPERTY_L2BIN_FAV1_SUITE_KEY, PROPERTY_L2BIN_FAV1_SUITE_DEFAULT);
        fav1_resolution = initPropertyDefaults(context, PROPERTY_L2BIN_FAV1_RESOLUTION_KEY, PROPERTY_L2BIN_FAV1_RESOLUTION_DEFAULT);





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

        flaguse.addPropertyChangeListener(evt -> {
            if (flaguse.getValue() != null && flaguse.getValue().toString().trim().length() > 0) {
                context.setComponentsEnabled(PROPERTY_L2BIN_FLAGUSE_SELECTOR_KEY, false);
            } else {
                context.setComponentsEnabled(PROPERTY_L2BIN_FLAGUSE_SELECTOR_KEY, true);
            }
        });

        // Handle fav1 events -
        fav1SetToDefault.addPropertyChangeListener(evt -> {
            handleSetFav(context, fav1SetToDefault, fav1FlagUse, fav1_l3bprod, fav1_output_wavelengths, fav1NSWE, fav1_suite, fav1_resolution);
        });
        fav1FlagUse.addPropertyChangeListener(evt -> {
            handleSetFav(context, fav1SetToDefault, fav1FlagUse, fav1_l3bprod, fav1_output_wavelengths, fav1NSWE, fav1_suite, fav1_resolution);
        });
        fav1_l3bprod.addPropertyChangeListener(evt -> {
            handleSetFav(context, fav1SetToDefault, fav1FlagUse, fav1_l3bprod, fav1_output_wavelengths, fav1NSWE, fav1_suite, fav1_resolution);
        });
        fav1_output_wavelengths.addPropertyChangeListener(evt -> {
            handleSetFav(context, fav1SetToDefault, fav1FlagUse, fav1_l3bprod, fav1_output_wavelengths, fav1NSWE, fav1_suite, fav1_resolution);
        });
        fav1NSWE.addPropertyChangeListener(evt -> {
            handleSetFav(context, fav1SetToDefault, fav1FlagUse, fav1_l3bprod, fav1_output_wavelengths, fav1NSWE, fav1_suite, fav1_resolution);
        });
        fav1_suite.addPropertyChangeListener(evt -> {
            handleSetFav(context, fav1SetToDefault, fav1FlagUse, fav1_l3bprod, fav1_output_wavelengths, fav1NSWE, fav1_suite, fav1_resolution);
        });
        fav1_resolution.addPropertyChangeListener(evt -> {
            handleSetFav(context, fav1SetToDefault, fav1FlagUse, fav1_l3bprod, fav1_output_wavelengths, fav1NSWE, fav1_suite, fav1_resolution);
        });


        fieldsAdd.addPropertyChangeListener(evt -> {
            enablement(context);
        });

        namingScheme.addPropertyChangeListener(evt -> {
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
            if (!property.getName().toUpperCase().contains(FAV)) {
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
     * Handles favorite action
     *
     * @param context
     * @author Daniel Knowles
     */
    private void handleSetFav(BindingContext context, Property favSet, Property favFlagUse, Property fav_l3bprod, Property fav_output_wavelengths, Property favNSWE, Property fav_suite, Property fav_resolution) {


        if (propertyValueChangeEventsEnabled) {
            propertyValueChangeEventsEnabled = false;
            try {
                if (favSet.getValue()) {
                    // set all favorites checkboxes to false then reset current favorite checkbox to true.
                    fav1SetToDefault.setValue(false);
                    favSet.setValue(true);

                    flaguse.setValue("");
                    if (favFlagUse.getValue() != null) {
                        if (favFlagUse.getValue().toString() != null) {
                            flaguse.setValue(favFlagUse.getValue().toString().trim());
                        }
                    }

                    l3bprod.setValue("");
                    if (fav_l3bprod.getValue() != null) {
                        if (fav_l3bprod.getValue().toString() != null) {
                            l3bprod.setValue(fav_l3bprod.getValue().toString().trim());
                        }
                    }

                    suite.setValue("");
                    if (fav_suite.getValue() != null) {
                        if (fav_suite.getValue().toString() != null) {
                            suite.setValue(fav_suite.getValue().toString().trim());
                        }
                    }

                    resolution.setValue("");
                    if (fav_resolution.getValue() != null) {
                        if (fav_resolution.getValue().toString() != null) {
                            resolution.setValue(fav_resolution.getValue().toString().trim());
                        }
                    }

                    output_wavelengths.setValue("");
                    if (fav_output_wavelengths.getValue() != null) {
                        if (fav_output_wavelengths.getValue().toString() != null) {
                            output_wavelengths.setValue(fav_output_wavelengths.getValue().toString().trim());
                        }
                    }

                    north.setValue("");
                    south.setValue("");
                    west.setValue("");
                    east.setValue("");
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
                    flaguse.setValue(flaguse.getDescriptor().getDefaultValue());
                    l3bprod.setValue(l3bprod.getDescriptor().getDefaultValue());
                    suite.setValue(suite.getDescriptor().getDefaultValue());
                    resolution.setValue(resolution.getDescriptor().getDefaultValue());
                    output_wavelengths.setValue(output_wavelengths.getDescriptor().getDefaultValue());
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

        System.out.println("propertyName=" + propertyName);

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
        return new HelpCtx("OCSSW_L2binPreferences");
    }

    @SuppressWarnings("UnusedDeclaration")
    static class SeadasToolboxBean {

        @Preference(key = PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SECTION_KEY,
                label = PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SECTION_LABEL,
                description = PROPERTY_L2BIN_OFILE_NAMING_SCHEME_SECTION_TOOLTIP)
        boolean l2bin_OFILE_NAMING_SCHEME_SECTION = true;

        @Preference(key = PROPERTY_L2BIN_OFILE_NAMING_SCHEME_KEY,
                label = PROPERTY_L2BIN_OFILE_NAMING_SCHEME_LABEL,
                valueSet = {OFILE_NAMING_SCHEME_SIMPLE,OFILE_NAMING_SCHEME_OCSSW, OFILE_NAMING_SCHEME_OCSSW_SHORT},
                description = PROPERTY_L2BIN_OFILE_NAMING_SCHEME_TOOLTIP)
        String l2binOfileNamingSchemeDefault = PROPERTY_L2BIN_OFILE_NAMING_SCHEME_DEFAULT;


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
        
        
        

        @Preference(key = PROPERTY_L2BIN_SUITE_KEY,
                label = PROPERTY_L2BIN_SUITE_LABEL,
                description = PROPERTY_L2BIN_SUITE_TOOLTIP)
        String l2binSuiteDefault = PROPERTY_L2BIN_SUITE_DEFAULT;

        @Preference(key = PROPERTY_L2BIN_L3BPROD_KEY,
                label = PROPERTY_L2BIN_L3BPROD_LABEL,
                description = PROPERTY_L2BIN_L3BPROD_TOOLTIP)
        String l2binL3bprodDefault = PROPERTY_L2BIN_L3BPROD_DEFAULT;


        @Preference(key = PROPERTY_L2BIN_OUTPUT_WAVELENGTHS_KEY,
                label = PROPERTY_L2BIN_OUTPUT_WAVELENGTHS_LABEL,
                description = PROPERTY_L2BIN_OUTPUT_WAVELENGTHS_TOOLTIP)
        String l2binOutputWavelengthsDefault = PROPERTY_L2BIN_OUTPUT_WAVELENGTHS_DEFAULT;


        @Preference(key = PROPERTY_L2BIN_FLAGUSE_KEY,
                label = PROPERTY_L2BIN_FLAGUSE_LABEL,
                description = PROPERTY_L2BIN_FLAGUSE_TOOLTIP)
        String l2binFlaguseDefault = PROPERTY_L2BIN_FLAGUSE_DEFAULT;


        @Preference(key = PROPERTY_L2BIN_FLAGUSE_SELECTOR_KEY,
                label = PROPERTY_L2BIN_FLAGUSE_SELECTOR_LABEL,
                description = PROPERTY_L2BIN_FLAGUSE_SELECTOR_TOOLTIP)
        boolean isPropertyL2binFlaguseSelectorDefault = true;

        @Preference(key = PROPERTY_L2BIN_PRODTYPE_KEY,
                label = PROPERTY_L2BIN_PRODTYPE_LABEL,
                description = PROPERTY_L2BIN_PRODTYPE_TOOLTIP)
        String l2binProdtypeDefault = PROPERTY_L2BIN_PRODTYPE_DEFAULT;

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




        // Favorites

        @Preference(key = PROPERTY_L2BIN_FAVORITE_SETTINGS_SECTION_KEY,
                label = PROPERTY_L2BIN_FAVORITE_SETTINGS_SECTION_LABEL,
                description = PROPERTY_L2BIN_FAVORITE_SETTINGS_SECTION_TOOLTIP)
        boolean L2bin_FAVORITE_PROJECTIONS_SECTION = true;



        @Preference(key = PROPERTY_L2BIN_FAV1_LOAD_KEY,
                label = PROPERTY_L2BIN_FAV1_LOAD_LABEL,
                description = PROPERTY_L2BIN_FAV1_LOAD_TOOLTIP)
        boolean L2bin_FAV1_LOAD_KEY = PROPERTY_L2BIN_FAV1_LOAD_DEFAULT;


        @Preference(key = PROPERTY_L2BIN_FAV1_DESCRIPTION_KEY,
                label = PROPERTY_L2BIN_FAV1_DESCRIPTION_LABEL,
                description = PROPERTY_L2BIN_FAV1_DESCRIPTION_TOOLTIP)
        String L2bin_FAV1_DESCRIPTION_KEY= PROPERTY_L2BIN_FAV1_DESCRIPTION_DEFAULT;

        @Preference(key = PROPERTY_L2BIN_FAV1_SUITE_KEY,
                label = PROPERTY_L2BIN_FAV1_SUITE_LABEL,
                description = PROPERTY_L2BIN_FAV1_SUITE_TOOLTIP)
        String l2binFav1SuiteDefault = PROPERTY_L2BIN_FAV1_SUITE_DEFAULT;


        @Preference(key = PROPERTY_L2BIN_FAV1_L3BPROD_KEY,
                label = PROPERTY_L2BIN_FAV1_L3BPROD_LABEL,
                description = PROPERTY_L2BIN_FAV1_L3BPROD_TOOLTIP)
        String l2binFav1L3bprodDefault = PROPERTY_L2BIN_FAV1_L3BPROD_DEFAULT;

        @Preference(key = PROPERTY_L2BIN_FAV1_OUTPUT_WAVELENGTHS_KEY,
                label = PROPERTY_L2BIN_FAV1_OUTPUT_WAVELENGTHS_LABEL,
                description = PROPERTY_L2BIN_FAV1_OUTPUT_WAVELENGTHS_TOOLTIP)
        String l2binFav1OutputWavelengthsDefault= PROPERTY_L2BIN_FAV1_OUTPUT_WAVELENGTHS_DEFAULT;

        @Preference(key = PROPERTY_L2BIN_FAV1_FLAGUSE_KEY,
                label = PROPERTY_L2BIN_FAV1_FLAGUSE_LABEL,
                description = PROPERTY_L2BIN_FAV1_FLAGUSE_TOOLTIP)
        String L2bin_FAV1_FLAGUSE_KEY = PROPERTY_L2BIN_FAV1_FLAGUSE_DEFAULT;

        @Preference(key = PROPERTY_L2BIN_FAV1_RESOLUTION_KEY,
                label = PROPERTY_L2BIN_FAV1_RESOLUTION_LABEL,
                description = PROPERTY_L2BIN_FAV1_RESOLUTION_TOOLTIP)
        String l2binFav1ResolutionDefault = PROPERTY_L2BIN_FAV1_RESOLUTION_DEFAULT;



        @Preference(key = PROPERTY_L2BIN_FAV1_NSWE_KEY,
                label = PROPERTY_L2BIN_FAV1_NSWE_LABEL,
                description = PROPERTY_L2BIN_FAV1_NSWE_TOOLTIP)
        String l2binFav1NsweDefault= PROPERTY_L2BIN_FAV1_NSWE_DEFAULT;










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


    public static boolean getPreferenceFlaguseAutoFillEnable() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyBool(PROPERTY_L2BIN_FLAGUSE_SELECTOR_KEY, PROPERTY_L2BIN_FLAGUSE_SELECTOR_DEFAULT);
    }



}
