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

/**
 * * Panel handling l3bin preferences. Sub-panel of the "SeaDAS-Toolbox"-panel.
 *
 * @author Daniel Knowles
 */


@OptionsPanelController.SubRegistration(location = "SeaDAS",
        displayName = "#Options_DisplayName_OCSSW_L3bin",
        keywords = "#Options_Keywords_OCSSW_L3bin",
        keywordsCategory = "Processors",
        id = "L3bin_preferences")
@org.openide.util.NbBundle.Messages({
        "Options_DisplayName_OCSSW_L3bin=L3bin",
        "Options_Keywords_OCSSW_L3bin=seadas, ocssw, l3bin. binning, l3b"
})
public final class OCSSW_L3binController extends DefaultConfigController {

    private static final String FAV = ".favorites";

    Property restoreDefaults;

    Property north;
    Property south;
    Property west;
    Property east;

    Property namingScheme;
    Property fieldsAdd;


    boolean propertyValueChangeEventsEnabled = true;




    public static final String  OFILE_NAMING_SCHEME_SIMPLE = "output";
    public static final String  OFILE_NAMING_SCHEME_OCSSW_SHORT = "OCSSW (do not derive time field)";
    public static final String  OFILE_NAMING_SCHEME_OCSSW = "OCSSW";
    public static final String OFILE_NAMING_SCHEME_IFILE_REPLACE = "IFILE (String-Replace)";

    public static final String OFILE_NAMING_SCHEME_SUFFIX_NONE = "No Suffix";
    public static final String OFILE_NAMING_SCHEME_SUFFIX1 = "Suffix Custom 1";
    public static final String OFILE_NAMING_SCHEME_SUFFIX2 = "Suffix Custom 2";
    public static final String OFILE_NAMING_SCHEME_SUFFIX_DEFAULT = "[prod][resolve]";
    public static final String OFILE_NAMING_SCHEME_SUFFIX_DEFAULT2 = "[prod][resolve][nswe°]";
//    public static final String OFILE_NAMING_SCHEME_SUFFIX_DEFAULT3 = "[.product][.resolution][.projection]";
//    public static final String OFILE_NAMING_SCHEME_SUFFIX_DEFAULT4 = "[_product][_resolution][_projection]";


    // Preferences property prefix
    private static final String PROPERTY_L3BIN_ROOT_KEY = SeadasToolboxDefaults.PROPERTY_SEADAS_ROOT_KEY + ".l3bin";


    public static final String PROPERTY_L3BIN_PARAMETERS_SECTION_KEY = PROPERTY_L3BIN_ROOT_KEY + ".ofile.parameters.section";
    public static final String PROPERTY_L3BIN_PARAMETERS_SECTION_LABEL = "Fields: (Product & General Parameters)";
    public static final String PROPERTY_L3BIN_PARAMETERS_SECTION_TOOLTIP = "L3bin parameters";


    public static final String PROPERTY_L3BIN_PRODUCT_KEY = PROPERTY_L3BIN_ROOT_KEY + ".prod";
    public static final String PROPERTY_L3BIN_PRODUCT_LABEL = "prod";
    public static final String PROPERTY_L3BIN_PRODUCT_TOOLTIP = "Product(s)";
    public static final String PROPERTY_L3BIN_PRODUCT_DEFAULT = "";



    public static final String PROPERTY_L3BIN_RESOLUTION_KEY = PROPERTY_L3BIN_ROOT_KEY + ".resolve";
    public static final String PROPERTY_L3BIN_RESOLUTION_LABEL = "resolve";
    public static final String PROPERTY_L3BIN_RESOLUTION_TOOLTIP = "Resolution";
    public static final String PROPERTY_L3BIN_RESOLUTION_DEFAULT = "";



    public static final String PROPERTY_L3BIN_NORTH_KEY = PROPERTY_L3BIN_ROOT_KEY + ".latnorth";
    public static final String PROPERTY_L3BIN_NORTH_LABEL = "latnorth";
    public static final String PROPERTY_L3BIN_NORTH_TOOLTIP = "Northernmost boundary";
    public static final String PROPERTY_L3BIN_NORTH_DEFAULT = "";

    public static final String PROPERTY_L3BIN_SOUTH_KEY = PROPERTY_L3BIN_ROOT_KEY + ".latsouth";
    public static final String PROPERTY_L3BIN_SOUTH_LABEL = "latsouth";
    public static final String PROPERTY_L3BIN_SOUTH_TOOLTIP = "Southernmost boundary";
    public static final String PROPERTY_L3BIN_SOUTH_DEFAULT = "";

    public static final String PROPERTY_L3BIN_WEST_KEY = PROPERTY_L3BIN_ROOT_KEY + ".lonwest";
    public static final String PROPERTY_L3BIN_WEST_LABEL = "lonwest";
    public static final String PROPERTY_L3BIN_WEST_TOOLTIP = "Westernmost boundary";
    public static final String PROPERTY_L3BIN_WEST_DEFAULT = "";

    public static final String PROPERTY_L3BIN_EAST_KEY = PROPERTY_L3BIN_ROOT_KEY + ".loneast";
    public static final String PROPERTY_L3BIN_EAST_LABEL = "loneast";
    public static final String PROPERTY_L3BIN_EAST_TOOLTIP = "Easternmost boundary";
    public static final String PROPERTY_L3BIN_EAST_DEFAULT = "";

    




    public static final String PROPERTY_L3BIN_OFILE_NAMING_SCHEME_SECTION_KEY = PROPERTY_L3BIN_ROOT_KEY + ".ofile.naming.scheme.section";
    public static final String PROPERTY_L3BIN_OFILE_NAMING_SCHEME_SECTION_LABEL = "Fields: (Naming Scheme for 'ofile')";
    public static final String PROPERTY_L3BIN_OFILE_NAMING_SCHEME_SECTION_TOOLTIP = "Naming scheme to use for autofilling ofile name";

    public static final String PROPERTY_L3BIN_OFILE_NAMING_SCHEME_KEY = PROPERTY_L3BIN_ROOT_KEY + ".ofile.naming.scheme";
    public static final String PROPERTY_L3BIN_OFILE_NAMING_SCHEME_LABEL = "Basename Options";
    public static final String PROPERTY_L3BIN_OFILE_NAMING_SCHEME_TOOLTIP = "Naming scheme to use for autofilling ofile name";
    public static final String PROPERTY_L3BIN_OFILE_NAMING_SCHEME_DEFAULT = OFILE_NAMING_SCHEME_IFILE_REPLACE;


    public static final String PROPERTY_L3BIN_OFILE_NAMING_SCHEME_SUFFIX_OPTIONS_KEY = PROPERTY_L3BIN_ROOT_KEY + ".ofile.naming.scheme.suffix.options";
    public static final String PROPERTY_L3BIN_OFILE_NAMING_SCHEME_SUFFIX_OPTIONS_LABEL = "Suffix Options";
    public static final String PROPERTY_L3BIN_OFILE_NAMING_SCHEME_SUFFIX_OPTIONS_TOOLTIP = "ofile Add Suffix Scheme";
    public static final String PROPERTY_L3BIN_OFILE_NAMING_SCHEME_SUFFIX_OPTIONS_DEFAULT = OFILE_NAMING_SCHEME_SUFFIX1;


    public  static  final  String SUFFIX_LIST_TOOLTIPS = "<html>" +
            "ofile Naming scheme keyed add-ons as suffix of ofile name<br>" +
            "[_product] : adds 'product' field with '_' as delimiter<br>" +
            "[_resolution] : adds 'resolution' field with '_' as delimiter<br>" +
            "[_projection] : adds 'projection' field with '_' as delimiter<br>" +
            "</html>";

    public static final String PROPERTY_L3BIN_OFILE_NAMING_SCHEME_SUFFIX1_KEY = PROPERTY_L3BIN_ROOT_KEY + ".ofile.naming.scheme.suffix1";
    public static final String PROPERTY_L3BIN_OFILE_NAMING_SCHEME_SUFFIX1_LABEL = OFILE_NAMING_SCHEME_SUFFIX1;
    public static final String PROPERTY_L3BIN_OFILE_NAMING_SCHEME_SUFFIX1_TOOLTIP = SUFFIX_LIST_TOOLTIPS;
    public static final String PROPERTY_L3BIN_OFILE_NAMING_SCHEME_SUFFIX1_DEFAULT = OFILE_NAMING_SCHEME_SUFFIX_DEFAULT;


    public static final String PROPERTY_L3BIN_OFILE_NAMING_SCHEME_SUFFIX2_KEY = PROPERTY_L3BIN_ROOT_KEY + ".ofile.naming.scheme.suffix2";
    public static final String PROPERTY_L3BIN_OFILE_NAMING_SCHEME_SUFFIX2_LABEL = OFILE_NAMING_SCHEME_SUFFIX2;
    public static final String PROPERTY_L3BIN_OFILE_NAMING_SCHEME_SUFFIX2_TOOLTIP = SUFFIX_LIST_TOOLTIPS;
    public static final String PROPERTY_L3BIN_OFILE_NAMING_SCHEME_SUFFIX2_DEFAULT = OFILE_NAMING_SCHEME_SUFFIX_DEFAULT2;


    public static final String PROPERTY_L3BIN_OFILE_NAMING_SCHEME_IFILE_ORIGINAL_KEY = PROPERTY_L3BIN_ROOT_KEY + ".ofile.naming.scheme.ifile.original";
    public static final String PROPERTY_L3BIN_OFILE_NAMING_SCHEME_IFILE_ORIGINAL_LABEL = "Basename " + OFILE_NAMING_SCHEME_IFILE_REPLACE +  ": Original";
    public static final String PROPERTY_L3BIN_OFILE_NAMING_SCHEME_IFILE_ORIGINAL_TOOLTIP = "ofile Ifile Original";
    public static final String PROPERTY_L3BIN_OFILE_NAMING_SCHEME_IFILE_ORIGINAL_DEFAULT = ".L3b.";

    public static final String PROPERTY_L3BIN_OFILE_NAMING_SCHEME_IFILE_REPLACE_KEY = PROPERTY_L3BIN_ROOT_KEY + ".ofile.naming.scheme.ifile.replace";
    public static final String PROPERTY_L3BIN_OFILE_NAMING_SCHEME_IFILE_REPLACE_LABEL =  "Basename " + OFILE_NAMING_SCHEME_IFILE_REPLACE + ": Replacement";
    public static final String PROPERTY_L3BIN_OFILE_NAMING_SCHEME_IFILE_REPLACE_TOOLTIP = "ofile Ifile Replace";
    public static final String PROPERTY_L3BIN_OFILE_NAMING_SCHEME_IFILE_REPLACE_DEFAULT = ".L3b.";








    // Property Setting: Restore Defaults

    private static final String PROPERTY_RESTORE_KEY_SUFFIX = PROPERTY_L3BIN_ROOT_KEY + ".restore.defaults";

    public static final String PROPERTY_RESTORE_SECTION_KEY = PROPERTY_RESTORE_KEY_SUFFIX + ".section";
    public static final String PROPERTY_RESTORE_SECTION_LABEL = "Restore";
    public static final String PROPERTY_RESTORE_SECTION_TOOLTIP = "Restores preferences to the package defaults";

    public static final String PROPERTY_RESTORE_DEFAULTS_KEY = PROPERTY_RESTORE_KEY_SUFFIX + ".apply";
    public static final String PROPERTY_RESTORE_DEFAULTS_LABEL = "Default (L3bin Preferences)";
    public static final String PROPERTY_RESTORE_DEFAULTS_TOOLTIP = "Restore all L3bin preferences to the original default";
    public static final boolean PROPERTY_RESTORE_DEFAULTS_DEFAULT = false;
    

    protected PropertySet createPropertySet() {
        return createPropertySet(new SeadasToolboxBean());
    }



    @Override
    protected JPanel createPanel(BindingContext context) {

        String[] interpOptionsArray = {"  ",
                "nearest",
                "bin",
                "area"};


        //
        // Initialize the default value contained within each property descriptor
        // This is done so subsequently the restoreDefaults actions can be performed
        //

        initPropertyDefaults(context, PROPERTY_L3BIN_PARAMETERS_SECTION_KEY, true);
        initPropertyDefaults(context, PROPERTY_L3BIN_PRODUCT_KEY, PROPERTY_L3BIN_PRODUCT_DEFAULT);

;
        initPropertyDefaults(context, PROPERTY_L3BIN_RESOLUTION_KEY, PROPERTY_L3BIN_RESOLUTION_DEFAULT);
        north = initPropertyDefaults(context, PROPERTY_L3BIN_NORTH_KEY, PROPERTY_L3BIN_NORTH_DEFAULT);
        south = initPropertyDefaults(context, PROPERTY_L3BIN_SOUTH_KEY, PROPERTY_L3BIN_SOUTH_DEFAULT);
        west = initPropertyDefaults(context, PROPERTY_L3BIN_WEST_KEY, PROPERTY_L3BIN_WEST_DEFAULT);
        east = initPropertyDefaults(context, PROPERTY_L3BIN_EAST_KEY, PROPERTY_L3BIN_EAST_DEFAULT);


        initPropertyDefaults(context, PROPERTY_L3BIN_OFILE_NAMING_SCHEME_SECTION_KEY, true);
        namingScheme = initPropertyDefaults(context, PROPERTY_L3BIN_OFILE_NAMING_SCHEME_KEY, PROPERTY_L3BIN_OFILE_NAMING_SCHEME_DEFAULT);
        fieldsAdd = initPropertyDefaults(context, PROPERTY_L3BIN_OFILE_NAMING_SCHEME_SUFFIX_OPTIONS_KEY, PROPERTY_L3BIN_OFILE_NAMING_SCHEME_SUFFIX_OPTIONS_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L3BIN_OFILE_NAMING_SCHEME_IFILE_REPLACE_KEY, PROPERTY_L3BIN_OFILE_NAMING_SCHEME_IFILE_REPLACE_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L3BIN_OFILE_NAMING_SCHEME_IFILE_ORIGINAL_KEY, PROPERTY_L3BIN_OFILE_NAMING_SCHEME_IFILE_ORIGINAL_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L3BIN_OFILE_NAMING_SCHEME_SUFFIX1_KEY, PROPERTY_L3BIN_OFILE_NAMING_SCHEME_SUFFIX1_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L3BIN_OFILE_NAMING_SCHEME_SUFFIX2_KEY, PROPERTY_L3BIN_OFILE_NAMING_SCHEME_SUFFIX2_DEFAULT);



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

        if (OFILE_NAMING_SCHEME_IFILE_REPLACE.equals(namingScheme.getValue())) {
            context.setComponentsEnabled(PROPERTY_L3BIN_OFILE_NAMING_SCHEME_IFILE_REPLACE_KEY, true);
            context.setComponentsEnabled(PROPERTY_L3BIN_OFILE_NAMING_SCHEME_IFILE_ORIGINAL_KEY, true);
        } else {
            context.setComponentsEnabled(PROPERTY_L3BIN_OFILE_NAMING_SCHEME_IFILE_REPLACE_KEY, false);
            context.setComponentsEnabled(PROPERTY_L3BIN_OFILE_NAMING_SCHEME_IFILE_ORIGINAL_KEY, false);
        }

        if (OFILE_NAMING_SCHEME_SUFFIX_NONE.equals(fieldsAdd.getValue())
                || OFILE_NAMING_SCHEME_SUFFIX_DEFAULT.equals(fieldsAdd.getValue())
                || OFILE_NAMING_SCHEME_SUFFIX_DEFAULT2.equals(fieldsAdd.getValue())
        ) {
            context.setComponentsEnabled(PROPERTY_L3BIN_OFILE_NAMING_SCHEME_SUFFIX1_KEY, false);
            context.setComponentsEnabled(PROPERTY_L3BIN_OFILE_NAMING_SCHEME_SUFFIX2_KEY, false);
        }

        if (OFILE_NAMING_SCHEME_SUFFIX1.equals(fieldsAdd.getValue())) {
            context.setComponentsEnabled(PROPERTY_L3BIN_OFILE_NAMING_SCHEME_SUFFIX1_KEY, true);
            context.setComponentsEnabled(PROPERTY_L3BIN_OFILE_NAMING_SCHEME_SUFFIX2_KEY, false);
        }

        if (OFILE_NAMING_SCHEME_SUFFIX2.equals(fieldsAdd.getValue())) {
            context.setComponentsEnabled(PROPERTY_L3BIN_OFILE_NAMING_SCHEME_SUFFIX1_KEY, false);
            context.setComponentsEnabled(PROPERTY_L3BIN_OFILE_NAMING_SCHEME_SUFFIX2_KEY, true);
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


    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("l3bin");
    }

    @SuppressWarnings("UnusedDeclaration")
    static class SeadasToolboxBean {


        @Preference(key = PROPERTY_L3BIN_OFILE_NAMING_SCHEME_SECTION_KEY,
                label = PROPERTY_L3BIN_OFILE_NAMING_SCHEME_SECTION_LABEL,
                description = PROPERTY_L3BIN_OFILE_NAMING_SCHEME_SECTION_TOOLTIP)
        boolean l3bin_OFILE_NAMING_SCHEME_SECTION = true;

        @Preference(key = PROPERTY_L3BIN_OFILE_NAMING_SCHEME_KEY,
                label = PROPERTY_L3BIN_OFILE_NAMING_SCHEME_LABEL,
                valueSet = {OFILE_NAMING_SCHEME_SIMPLE, OFILE_NAMING_SCHEME_IFILE_REPLACE,OFILE_NAMING_SCHEME_OCSSW, OFILE_NAMING_SCHEME_OCSSW_SHORT},
                description = PROPERTY_L3BIN_OFILE_NAMING_SCHEME_TOOLTIP)
        String l3binOfileNamingSchemeDefault = PROPERTY_L3BIN_OFILE_NAMING_SCHEME_DEFAULT;

        @Preference(key = PROPERTY_L3BIN_OFILE_NAMING_SCHEME_IFILE_ORIGINAL_KEY,
                label = PROPERTY_L3BIN_OFILE_NAMING_SCHEME_IFILE_ORIGINAL_LABEL,
                description = PROPERTY_L3BIN_OFILE_NAMING_SCHEME_IFILE_ORIGINAL_TOOLTIP)
        String l3binOfileNamingSchemeIfileOriginalDefault = PROPERTY_L3BIN_OFILE_NAMING_SCHEME_IFILE_ORIGINAL_DEFAULT;

        @Preference(key = PROPERTY_L3BIN_OFILE_NAMING_SCHEME_IFILE_REPLACE_KEY,
                label = PROPERTY_L3BIN_OFILE_NAMING_SCHEME_IFILE_REPLACE_LABEL,
                description = PROPERTY_L3BIN_OFILE_NAMING_SCHEME_IFILE_REPLACE_TOOLTIP)
        String l3binOfileNamingSchemeIfileReplaceDefault = PROPERTY_L3BIN_OFILE_NAMING_SCHEME_IFILE_REPLACE_DEFAULT;

        @Preference(key = PROPERTY_L3BIN_OFILE_NAMING_SCHEME_SUFFIX_OPTIONS_KEY,
                label = PROPERTY_L3BIN_OFILE_NAMING_SCHEME_SUFFIX_OPTIONS_LABEL,
                valueSet = {OFILE_NAMING_SCHEME_SUFFIX_NONE,
                        OFILE_NAMING_SCHEME_SUFFIX1,
                        OFILE_NAMING_SCHEME_SUFFIX2,
                        OFILE_NAMING_SCHEME_SUFFIX_DEFAULT,
                        OFILE_NAMING_SCHEME_SUFFIX_DEFAULT2},
                description = PROPERTY_L3BIN_OFILE_NAMING_SCHEME_SUFFIX_OPTIONS_TOOLTIP)
        String l3binOfileNamingSchemeFieldsAddDefault = PROPERTY_L3BIN_OFILE_NAMING_SCHEME_SUFFIX_OPTIONS_DEFAULT;

        @Preference(key = PROPERTY_L3BIN_OFILE_NAMING_SCHEME_SUFFIX1_KEY,
                label = PROPERTY_L3BIN_OFILE_NAMING_SCHEME_SUFFIX1_LABEL,
                description = PROPERTY_L3BIN_OFILE_NAMING_SCHEME_SUFFIX1_TOOLTIP)
        String l3binOfileNamingSchemeSuffix1Default = PROPERTY_L3BIN_OFILE_NAMING_SCHEME_SUFFIX1_DEFAULT;

        @Preference(key = PROPERTY_L3BIN_OFILE_NAMING_SCHEME_SUFFIX2_KEY,
                label = PROPERTY_L3BIN_OFILE_NAMING_SCHEME_SUFFIX2_LABEL,
                description = PROPERTY_L3BIN_OFILE_NAMING_SCHEME_SUFFIX2_TOOLTIP)
        String l3binOfileNamingSchemeSuffix2Default = PROPERTY_L3BIN_OFILE_NAMING_SCHEME_SUFFIX2_DEFAULT;





        @Preference(key = PROPERTY_L3BIN_PARAMETERS_SECTION_KEY,
                label = PROPERTY_L3BIN_PARAMETERS_SECTION_LABEL,
                description = PROPERTY_L3BIN_PARAMETERS_SECTION_TOOLTIP)
        boolean l3bin_PROPERTY_L3BIN_PARAMETERS_SECTION_KEY = true;


        @Preference(key = PROPERTY_L3BIN_PRODUCT_KEY,
                label = PROPERTY_L3BIN_PRODUCT_LABEL,
                description = PROPERTY_L3BIN_PRODUCT_TOOLTIP)
        String l3binProductDefault = PROPERTY_L3BIN_PRODUCT_DEFAULT;



        @Preference(key = PROPERTY_L3BIN_RESOLUTION_KEY,
                label = PROPERTY_L3BIN_RESOLUTION_LABEL,
                description = PROPERTY_L3BIN_RESOLUTION_TOOLTIP)
        String l3binResolutionDefault = PROPERTY_L3BIN_RESOLUTION_DEFAULT;



        @Preference(key = PROPERTY_L3BIN_NORTH_KEY,
                label = PROPERTY_L3BIN_NORTH_LABEL,
                description = PROPERTY_L3BIN_NORTH_TOOLTIP)
        String l3binNorthDefault = PROPERTY_L3BIN_NORTH_DEFAULT;

        @Preference(key = PROPERTY_L3BIN_SOUTH_KEY,
                label = PROPERTY_L3BIN_SOUTH_LABEL,
                description = PROPERTY_L3BIN_SOUTH_TOOLTIP)
        String l3binSouthDefault = PROPERTY_L3BIN_SOUTH_DEFAULT;

        @Preference(key = PROPERTY_L3BIN_WEST_KEY,
                label = PROPERTY_L3BIN_WEST_LABEL,
                description = PROPERTY_L3BIN_WEST_TOOLTIP)
        String l3binWestDefault = PROPERTY_L3BIN_WEST_DEFAULT;

        @Preference(key = PROPERTY_L3BIN_EAST_KEY,
                label = PROPERTY_L3BIN_EAST_LABEL,
                description = PROPERTY_L3BIN_EAST_TOOLTIP)
        String l3binEastDefault = PROPERTY_L3BIN_EAST_DEFAULT;









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


    public static String getPreferenceProd() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(OCSSW_L3binController.PROPERTY_L3BIN_PRODUCT_KEY, OCSSW_L3binController.PROPERTY_L3BIN_PRODUCT_DEFAULT);
    }


    public static String getPreferenceResolve() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(OCSSW_L3binController.PROPERTY_L3BIN_RESOLUTION_KEY, OCSSW_L3binController.PROPERTY_L3BIN_RESOLUTION_DEFAULT);
    }





    public static String getPreferenceNorth() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(OCSSW_L3binController.PROPERTY_L3BIN_NORTH_KEY, OCSSW_L3binController.PROPERTY_L3BIN_NORTH_DEFAULT);
    }

    public static String getPreferenceSouth() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(OCSSW_L3binController.PROPERTY_L3BIN_SOUTH_KEY, OCSSW_L3binController.PROPERTY_L3BIN_SOUTH_DEFAULT);
    }

    public static String getPreferenceWest() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(OCSSW_L3binController.PROPERTY_L3BIN_WEST_KEY, OCSSW_L3binController.PROPERTY_L3BIN_WEST_DEFAULT);
    }

    public static String getPreferenceEast() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(OCSSW_L3binController.PROPERTY_L3BIN_EAST_KEY, OCSSW_L3binController.PROPERTY_L3BIN_EAST_DEFAULT);
    }







    public static String getPreferenceOfileNamingScheme() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(OCSSW_L3binController.PROPERTY_L3BIN_OFILE_NAMING_SCHEME_KEY, OCSSW_L3binController.PROPERTY_L3BIN_OFILE_NAMING_SCHEME_DEFAULT);
    }


    public static String getPreferenceOfileNamingSchemeSuffixOptions() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(OCSSW_L3binController.PROPERTY_L3BIN_OFILE_NAMING_SCHEME_SUFFIX_OPTIONS_KEY, OCSSW_L3binController.PROPERTY_L3BIN_OFILE_NAMING_SCHEME_SUFFIX_OPTIONS_DEFAULT);
    }


    public static String getPreferenceOfileNamingSchemeIfileReplace() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(OCSSW_L3binController.PROPERTY_L3BIN_OFILE_NAMING_SCHEME_IFILE_REPLACE_KEY, OCSSW_L3binController.PROPERTY_L3BIN_OFILE_NAMING_SCHEME_IFILE_REPLACE_DEFAULT);
    }

    public static String getPreferenceOfileNamingSchemeIfileOriginal() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(OCSSW_L3binController.PROPERTY_L3BIN_OFILE_NAMING_SCHEME_IFILE_ORIGINAL_KEY, OCSSW_L3binController.PROPERTY_L3BIN_OFILE_NAMING_SCHEME_IFILE_ORIGINAL_DEFAULT);
    }


    public static String getPreferenceOfileNamingSchemeSuffix1() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(OCSSW_L3binController.PROPERTY_L3BIN_OFILE_NAMING_SCHEME_SUFFIX1_KEY, OCSSW_L3binController.PROPERTY_L3BIN_OFILE_NAMING_SCHEME_SUFFIX1_DEFAULT);
    }

    public static String getPreferenceOfileNamingSchemeSuffix2() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(OCSSW_L3binController.PROPERTY_L3BIN_OFILE_NAMING_SCHEME_SUFFIX2_KEY, OCSSW_L3binController.PROPERTY_L3BIN_OFILE_NAMING_SCHEME_SUFFIX2_DEFAULT);
    }




    



}
