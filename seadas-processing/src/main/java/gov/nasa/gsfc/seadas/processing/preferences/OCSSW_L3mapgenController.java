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
        id = "L3mapgen")
@org.openide.util.NbBundle.Messages({
        "Options_DisplayName_OCSSW_L3mapgen=L3mapgen",
        "Options_Keywords_OCSSW_L3mapgen=seadas, ocssw, l3mapgen"
})
public final class OCSSW_L3mapgenController extends DefaultConfigController {

    Property restoreDefaults;

    boolean propertyValueChangeEventsEnabled = true;


    // Preferences property prefix
    private static final String PROPERTY_L3MAPGEN_ROOT_KEY = SeadasToolboxDefaults.PROPERTY_SEADAS_ROOT_KEY + ".l3mapgen";


    public static final String PROPERTY_L3MAPGEN_PRODUCT_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + ".product";
    public static final String PROPERTY_L3MAPGEN_PRODUCT_LABEL = "product";
    public static final String PROPERTY_L3MAPGEN_PRODUCT_TOOLTIP = "Product(s)";
    public static final String PROPERTY_L3MAPGEN_PRODUCT_DEFAULT = "";

    public static final String PROPERTY_L3MAPGEN_PROJECTION_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + ".projection";
    public static final String PROPERTY_L3MAPGEN_PROJECTION_LABEL = "projection";
    public static final String PROPERTY_L3MAPGEN_PROJECTION_TOOLTIP = "Projection";
    public static final String PROPERTY_L3MAPGEN_PROJECTION_DEFAULT = "platecarree";

    public static final String PROPERTY_L3MAPGEN_RESOLUTION_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + ".resolution";
    public static final String PROPERTY_L3MAPGEN_RESOLUTION_LABEL = "resolution";
    public static final String PROPERTY_L3MAPGEN_RESOLUTION_TOOLTIP = "Resolution";
    public static final String PROPERTY_L3MAPGEN_RESOLUTION_DEFAULT = "";

    public static final String PROPERTY_L3MAPGEN_INTERP_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + ".interp";
    public static final String PROPERTY_L3MAPGEN_INTERP_LABEL = "interp";
    public static final String PROPERTY_L3MAPGEN_INTERP_TOOLTIP = "Interpolation type";
    public static final String PROPERTY_L3MAPGEN_INTERP_DEFAULT = "nearest";

    public static final String PROPERTY_L3MAPGEN_NORTH_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + ".north";
    public static final String PROPERTY_L3MAPGEN_NORTH_LABEL = "north";
    public static final String PROPERTY_L3MAPGEN_NORTH_TOOLTIP = "Northernmost boundary";
    public static final String PROPERTY_L3MAPGEN_NORTH_DEFAULT = "";

    public static final String PROPERTY_L3MAPGEN_SOUTH_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + ".south";
    public static final String PROPERTY_L3MAPGEN_SOUTH_LABEL = "south";
    public static final String PROPERTY_L3MAPGEN_SOUTH_TOOLTIP = "Southernmost boundary";
    public static final String PROPERTY_L3MAPGEN_SOUTH_DEFAULT = "";

    public static final String PROPERTY_L3MAPGEN_WEST_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + ".west";
    public static final String PROPERTY_L3MAPGEN_WEST_LABEL = "west";
    public static final String PROPERTY_L3MAPGEN_WEST_TOOLTIP = "Westernmost boundary";
    public static final String PROPERTY_L3MAPGEN_WEST_DEFAULT = "";

    public static final String PROPERTY_L3MAPGEN_EAST_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + ".east";
    public static final String PROPERTY_L3MAPGEN_EAST_LABEL = "east";
    public static final String PROPERTY_L3MAPGEN_EAST_TOOLTIP = "Easternmost boundary";
    public static final String PROPERTY_L3MAPGEN_EAST_DEFAULT = "";
    



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

        String[] interpOptionsArray = {"  ",
                "nearest",
                "bin",
                "area"};


        //
        // Initialize the default value contained within each property descriptor
        // This is done so subsequently the restoreDefaults actions can be performed
        //
        
        initPropertyDefaults(context, PROPERTY_L3MAPGEN_PRODUCT_KEY, PROPERTY_L3MAPGEN_PRODUCT_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L3MAPGEN_PROJECTION_KEY, PROPERTY_L3MAPGEN_PROJECTION_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L3MAPGEN_RESOLUTION_KEY, PROPERTY_L3MAPGEN_RESOLUTION_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L3MAPGEN_INTERP_KEY, PROPERTY_L3MAPGEN_INTERP_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L3MAPGEN_NORTH_KEY, PROPERTY_L3MAPGEN_NORTH_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L3MAPGEN_SOUTH_KEY, PROPERTY_L3MAPGEN_SOUTH_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L3MAPGEN_WEST_KEY, PROPERTY_L3MAPGEN_WEST_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L3MAPGEN_EAST_KEY, PROPERTY_L3MAPGEN_EAST_DEFAULT);



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

        // This call is an initialization call which set restoreDefault initial value
        handlePreferencesPropertyValueChange(context);
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
            if (property != restoreDefaults && property.getDescriptor().getDefaultValue() != null)
                if (!property.getValue().equals(property.getDescriptor().getDefaultValue())) {
                    return false;
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
                        if (property != restoreDefaults && property.getDescriptor().getDefaultValue() != null)
                            property.setValue(property.getDescriptor().getDefaultValue());
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

    // todo add a help page ... see the ColorBarLayerController for example

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("OCSSW_L3mapgenPreferences");
    }

    @SuppressWarnings("UnusedDeclaration")
    static class SeadasToolboxBean {


        @Preference(label = PROPERTY_L3MAPGEN_PRODUCT_LABEL,
                key = PROPERTY_L3MAPGEN_PRODUCT_KEY,
                description = PROPERTY_L3MAPGEN_PRODUCT_TOOLTIP)
        String l3mapgenProductDefault = PROPERTY_L3MAPGEN_PRODUCT_DEFAULT;

        @Preference(label = PROPERTY_L3MAPGEN_PROJECTION_LABEL,
                key = PROPERTY_L3MAPGEN_PROJECTION_KEY,
                description = PROPERTY_L3MAPGEN_PROJECTION_TOOLTIP)
        String l3mapgenProjectionDefault = PROPERTY_L3MAPGEN_PROJECTION_DEFAULT;

        @Preference(label = PROPERTY_L3MAPGEN_RESOLUTION_LABEL,
                key = PROPERTY_L3MAPGEN_RESOLUTION_KEY,
                description = PROPERTY_L3MAPGEN_RESOLUTION_TOOLTIP)
        String l3mapgenResolutionDefault = PROPERTY_L3MAPGEN_RESOLUTION_DEFAULT;

        @Preference(label = PROPERTY_L3MAPGEN_INTERP_LABEL,
                key = PROPERTY_L3MAPGEN_INTERP_KEY,
                valueSet = {" ", "nearest", "bin", "area"},
                description = PROPERTY_L3MAPGEN_INTERP_TOOLTIP)
        String l3mapgenInterpDefault = PROPERTY_L3MAPGEN_INTERP_DEFAULT;



        @Preference(label = PROPERTY_L3MAPGEN_NORTH_LABEL,
                key = PROPERTY_L3MAPGEN_NORTH_KEY,
                description = PROPERTY_L3MAPGEN_NORTH_TOOLTIP)
        String l3mapgenNorthDefault = PROPERTY_L3MAPGEN_NORTH_DEFAULT;

        @Preference(label = PROPERTY_L3MAPGEN_SOUTH_LABEL,
                key = PROPERTY_L3MAPGEN_SOUTH_KEY,
                description = PROPERTY_L3MAPGEN_SOUTH_TOOLTIP)
        String l3mapgenSouthDefault = PROPERTY_L3MAPGEN_SOUTH_DEFAULT;

        @Preference(label = PROPERTY_L3MAPGEN_WEST_LABEL,
                key = PROPERTY_L3MAPGEN_WEST_KEY,
                description = PROPERTY_L3MAPGEN_WEST_TOOLTIP)
        String l3mapgenWestDefault = PROPERTY_L3MAPGEN_WEST_DEFAULT;

        @Preference(label = PROPERTY_L3MAPGEN_EAST_LABEL,
                key = PROPERTY_L3MAPGEN_EAST_KEY,
                description = PROPERTY_L3MAPGEN_EAST_TOOLTIP)
        String l3mapgenEastDefault = PROPERTY_L3MAPGEN_EAST_DEFAULT;



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



    public static String getPreferenceProduct() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_PRODUCT_KEY, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_PRODUCT_DEFAULT);
    }


    public static String getPreferenceProjection() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_PROJECTION_KEY, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_PROJECTION_DEFAULT);
    }

    public static String getPreferenceResolution() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_RESOLUTION_KEY, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_RESOLUTION_DEFAULT);
    }

    public static String getPreferenceInterp() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_INTERP_KEY, OCSSW_L3mapgenController.PROPERTY_L3MAPGEN_INTERP_DEFAULT);
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


}
