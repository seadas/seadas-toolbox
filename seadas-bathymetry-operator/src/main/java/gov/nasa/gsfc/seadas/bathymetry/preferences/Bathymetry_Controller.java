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

package gov.nasa.gsfc.seadas.bathymetry.preferences;

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
 * * Panel handling LandCoastMask preferences. Sub-panel of the "SeaDAS-Toolbox"-panel.
 *
 * @author Daniel Knowles
 */


@OptionsPanelController.SubRegistration(location = "SeaDAS",
        displayName = "#Options_DisplayName_Bathymetry",
        keywords = "#Options_Keywords_Bathymetry",
        keywordsCategory = "General Tools",
        id = "Bathymetry_preferences",
        position = 7)

@org.openide.util.NbBundle.Messages({
        "Options_DisplayName_Bathymetry=Bathymetry",
        "Options_Keywords_Bathymetry=bathymetry topography elevation"
})
public final class Bathymetry_Controller extends DefaultConfigController {

    Property restoreDefaults;

    boolean propertyValueChangeEventsEnabled = true;


    // Preferences property prefix
    private static final String PROPERTY_BATHYMETRY_ROOT_KEY = "seadas.toolbox.bathymetry";


    public static final String PROPERTY_BATHYMETRY_SECTION_KEY = PROPERTY_BATHYMETRY_ROOT_KEY + ".bathymetry.mask.section";
    public static final String PROPERTY_BATHYMETRY_SECTION_LABEL = "Bathymetry Mask Options";
    public static final String PROPERTY_BATHYMETRY_SECTION_TOOLTIP = "Bathymetry mask options";

    public static final String PROPERTY_BATHYMETRY_MASK_CREATE_KEY = PROPERTY_BATHYMETRY_ROOT_KEY + ".bathymetry.mask.create";
    public static final String PROPERTY_BATHYMETRY_MASK_CREATE_LABEL = "Create Bathymetry Mask";
    public static final String PROPERTY_BATHYMETRY_MASK_CREATE_TOOLTIP = "Bathymetry mask will be created ";
    public static final boolean PROPERTY_BATHYMETRY_MASK_CREATE_DEFAULT = true;

    public static final String PROPERTY_BATHYMETRY_MASK_NAME_KEY = PROPERTY_BATHYMETRY_ROOT_KEY + ".bathymetry.mask.name";
    public static final String PROPERTY_BATHYMETRY_MASK_NAME_LABEL = "Bathymetry Mask Name";
    public static final String PROPERTY_BATHYMETRY_MASK_NAME_TOOLTIP = "Bathymetry mask name";
    public static final String PROPERTY_BATHYMETRY_NAME_DEFAULT = "BathymetryMask";

    public static final String PROPERTY_BATHYMETRY_MASK_COLOR_KEY = PROPERTY_BATHYMETRY_ROOT_KEY + ".bathymetry.mask.color";
    public static final String PROPERTY_BATHYMETRY_MASK_COLOR_LABEL = "Bathymetry Mask Color";
    public static final String PROPERTY_BATHYMETRY_MASK_COLOR_TOOLTIP = "Bathymetry mask color";
    public static final Color PROPERTY_BATHYMETRY_MASK_COLOR_DEFAULT = new Color(0,0,255);

    public static final String PROPERTY_BATHYMETRY_MASK_TRANSPARENCY_KEY = PROPERTY_BATHYMETRY_ROOT_KEY + ".bathymetry.mask.transparency";
    public static final String PROPERTY_BATHYMETRY_MASK_TRANSPARENCY_LABEL = "Bathymetry Mask Transparency";
    public static final String PROPERTY_BATHYMETRY_MASK_TRANSPARENCY_TOOLTIP = "Bathymetry mask transparency ";
    public static final double PROPERTY_BATHYMETRY_MASK_TRANSPARENCY_DEFAULT = 0.5;

    public static final String PROPERTY_BATHYMETRY_MASK_MIN_DEPTH_KEY = PROPERTY_BATHYMETRY_ROOT_KEY + ".bathymetry.mask.min.depth";
    public static final String PROPERTY_BATHYMETRY_MASK_MIN_DEPTH_LABEL = "Bathymetry Mask Min Depth";
    public static final String PROPERTY_BATHYMETRY_MASK_MIN_DEPTH_TOOLTIP = "Bathymetry mask minimum water depth";
    public static final double PROPERTY_BATHYMETRY_MASK_MIN_DEPTH_DEFAULT = 0.0;

    public static final String PROPERTY_BATHYMETRY_MASK_MAX_DEPTH_KEY = PROPERTY_BATHYMETRY_ROOT_KEY + ".bathymetry.mask.max.depth";
    public static final String PROPERTY_BATHYMETRY_MASK_MAX_DEPTH_LABEL = "Bathymetry Mask Max Depth";
    public static final String PROPERTY_BATHYMETRY_MASK_MAX_DEPTH_TOOLTIP = "Bathymetry mask maximum water depth";
    public static final double PROPERTY_BATHYMETRY_MASK_MAX_DEPTH_DEFAULT = 11000;



    public static final String PROPERTY_BATHYMETRY_ALL_BANDS_SHOW_KEY = PROPERTY_BATHYMETRY_ROOT_KEY + ".bathymetry.mask.show.all.bands";
    public static final String PROPERTY_BATHYMETRY_ALL_BANDS_SHOW_LABEL = "Bathymetry Mask Show All Bands";
    public static final String PROPERTY_BATHYMETRY_ALL_BANDS_SHOW_TOOLTIP = "Bathymetry mask will be displayed in all bands ";
    public static final boolean PROPERTY_BATHYMETRY_ALL_BANDS_SHOW_DEFAULT = false;


    public static final String PROPERTY_BAND_OPTIONS_SECTION_KEY = PROPERTY_BATHYMETRY_ROOT_KEY + ".bathymetry.band.options.section";
    public static final String PROPERTY_BAND_OPTIONS_SECTION_LABEL = "Band Options";
    public static final String PROPERTY_BAND_OPTIONS_SECTION_TOOLTIP = "Band options";

    public static final String PROPERTY_BATHYMETRY_BAND_CREATE_KEY = PROPERTY_BATHYMETRY_ROOT_KEY + ".bathymetry.band.create";
    public static final String PROPERTY_BATHYMETRY_BAND_CREATE_LABEL = "Create Bathymetry Band";
    public static final String PROPERTY_BATHYMETRY_BAND_CREATE_TOOLTIP = "Bathymetry band will be created ";
    public static final boolean PROPERTY_BATHYMETRY_BAND_CREATE_DEFAULT = true;

    public static final String PROPERTY_TOPOGRAPHY_BAND_CREATE_KEY = PROPERTY_BATHYMETRY_ROOT_KEY + ".topography.band.create";
    public static final String PROPERTY_TOPOGRAPHY_BAND_CREATE_LABEL = "Create Topography Band";
    public static final String PROPERTY_TOPOGRAPHY_BAND_CREATE_TOOLTIP = "Topography band will be created ";
    public static final boolean PROPERTY_TOPOGRAPHY_BAND_CREATE_DEFAULT = false;

    public static final String PROPERTY_ELEVATION_BAND_CREATE_KEY = PROPERTY_BATHYMETRY_ROOT_KEY + ".elevation.band.create";
    public static final String PROPERTY_ELEVATION_BAND_CREATE_LABEL = "Create Elevation Band";
    public static final String PROPERTY_ELEVATION_BAND_CREATE_TOOLTIP = "Elevation band will be created ";
    public static final boolean PROPERTY_ELEVATION_BAND_CREATE_DEFAULT = false;




    // Property Setting: Restore Defaults

    private static final String PROPERTY_RESTORE_KEY_SUFFIX = PROPERTY_BATHYMETRY_ROOT_KEY + ".restore.defaults";

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


        //
        // Initialize the default value contained within each property descriptor
        // This is done so subsequently the restoreDefaults actions can be performed
        //

        initPropertyDefaults(context, PROPERTY_BATHYMETRY_SECTION_KEY, true);
        initPropertyDefaults(context, PROPERTY_BATHYMETRY_MASK_CREATE_KEY, PROPERTY_BATHYMETRY_MASK_CREATE_DEFAULT);
        initPropertyDefaults(context, PROPERTY_BATHYMETRY_MASK_COLOR_KEY, PROPERTY_BATHYMETRY_MASK_COLOR_DEFAULT);
        initPropertyDefaults(context, PROPERTY_BATHYMETRY_MASK_NAME_KEY, PROPERTY_BATHYMETRY_NAME_DEFAULT);
        initPropertyDefaults(context, PROPERTY_BATHYMETRY_MASK_TRANSPARENCY_KEY, PROPERTY_BATHYMETRY_MASK_TRANSPARENCY_DEFAULT);
        initPropertyDefaults(context, PROPERTY_BATHYMETRY_MASK_MIN_DEPTH_KEY, PROPERTY_BATHYMETRY_MASK_MIN_DEPTH_DEFAULT);
        initPropertyDefaults(context, PROPERTY_BATHYMETRY_MASK_MAX_DEPTH_KEY, PROPERTY_BATHYMETRY_MASK_MAX_DEPTH_DEFAULT);


        initPropertyDefaults(context, PROPERTY_BAND_OPTIONS_SECTION_KEY, true);
        initPropertyDefaults(context, PROPERTY_BATHYMETRY_BAND_CREATE_KEY, PROPERTY_BATHYMETRY_BAND_CREATE_DEFAULT);
        initPropertyDefaults(context, PROPERTY_TOPOGRAPHY_BAND_CREATE_KEY, PROPERTY_TOPOGRAPHY_BAND_CREATE_DEFAULT);
        initPropertyDefaults(context, PROPERTY_ELEVATION_BAND_CREATE_KEY, PROPERTY_ELEVATION_BAND_CREATE_DEFAULT);


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
        return new HelpCtx("bathymetry");
    }

    @SuppressWarnings("UnusedDeclaration")
    static class SeadasToolboxBean {




        @Preference(key = PROPERTY_BATHYMETRY_SECTION_KEY,
                label = PROPERTY_BATHYMETRY_SECTION_LABEL,
                description = PROPERTY_BATHYMETRY_SECTION_TOOLTIP)
        boolean bathymetrySectionDefault = true;

        @Preference(key = PROPERTY_BATHYMETRY_MASK_CREATE_KEY,
                label = PROPERTY_BATHYMETRY_MASK_CREATE_LABEL,
                description = PROPERTY_BATHYMETRY_MASK_CREATE_TOOLTIP)
        boolean bathymetryShowDefault = PROPERTY_BATHYMETRY_MASK_CREATE_DEFAULT;

        @Preference(key = PROPERTY_BATHYMETRY_MASK_NAME_KEY,
                label = PROPERTY_BATHYMETRY_MASK_NAME_LABEL,
                description = PROPERTY_BATHYMETRY_MASK_NAME_TOOLTIP)
        String bathymetryNameDefault = PROPERTY_BATHYMETRY_NAME_DEFAULT;

        @Preference(key = PROPERTY_BATHYMETRY_MASK_COLOR_KEY,
                label = PROPERTY_BATHYMETRY_MASK_COLOR_LABEL,
                description = PROPERTY_BATHYMETRY_MASK_COLOR_TOOLTIP)
        Color bathymetryColorDefault = PROPERTY_BATHYMETRY_MASK_COLOR_DEFAULT;

        @Preference(key = PROPERTY_BATHYMETRY_MASK_TRANSPARENCY_KEY,
                label = PROPERTY_BATHYMETRY_MASK_TRANSPARENCY_LABEL,
                interval = "[0,1]",
                description = PROPERTY_BATHYMETRY_MASK_TRANSPARENCY_TOOLTIP)
        double bathymetryTransparencyDefault = PROPERTY_BATHYMETRY_MASK_TRANSPARENCY_DEFAULT;


        @Preference(key = PROPERTY_BATHYMETRY_MASK_MIN_DEPTH_KEY,
                label = PROPERTY_BATHYMETRY_MASK_MIN_DEPTH_LABEL,
                interval = "[0,11000]",
                description = PROPERTY_BATHYMETRY_MASK_MIN_DEPTH_TOOLTIP)
        double bathymetryMaskMinDepthDefault = PROPERTY_BATHYMETRY_MASK_MIN_DEPTH_DEFAULT;


        @Preference(key = PROPERTY_BATHYMETRY_MASK_MAX_DEPTH_KEY,
                label = PROPERTY_BATHYMETRY_MASK_MAX_DEPTH_LABEL,
                interval = "[0,11000]",
                description = PROPERTY_BATHYMETRY_MASK_MAX_DEPTH_TOOLTIP)
        double bathymetryMaskMaxDepthDefault = PROPERTY_BATHYMETRY_MASK_MAX_DEPTH_DEFAULT;




        @Preference(key = PROPERTY_BAND_OPTIONS_SECTION_KEY,
                label = PROPERTY_BAND_OPTIONS_SECTION_LABEL,
                description = PROPERTY_BAND_OPTIONS_SECTION_TOOLTIP)
        boolean bandOptionsSectionDefault = true;

        @Preference(key = PROPERTY_BATHYMETRY_BAND_CREATE_KEY,
                label = PROPERTY_BATHYMETRY_BAND_CREATE_LABEL,
                description = PROPERTY_BATHYMETRY_BAND_CREATE_TOOLTIP)
        boolean bathymetryBandCreateDefault = PROPERTY_BATHYMETRY_BAND_CREATE_DEFAULT;

        @Preference(key = PROPERTY_TOPOGRAPHY_BAND_CREATE_KEY,
                label = PROPERTY_TOPOGRAPHY_BAND_CREATE_LABEL,
                description = PROPERTY_TOPOGRAPHY_BAND_CREATE_TOOLTIP)
        boolean topographyBandCreateDefault = PROPERTY_TOPOGRAPHY_BAND_CREATE_DEFAULT;

        @Preference(key = PROPERTY_ELEVATION_BAND_CREATE_KEY,
                label = PROPERTY_ELEVATION_BAND_CREATE_LABEL,
                description = PROPERTY_ELEVATION_BAND_CREATE_TOOLTIP)
        boolean elevationBandCreateDefault = PROPERTY_ELEVATION_BAND_CREATE_DEFAULT;



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





    public static boolean getPreferenceBathymetryMaskCreate() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyBool(PROPERTY_BATHYMETRY_MASK_CREATE_KEY, PROPERTY_BATHYMETRY_MASK_CREATE_DEFAULT);
    }


    public static String getPreferenceBathymetryMaskName() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        String bathymetryMaskName = preferences.getPropertyString(PROPERTY_BATHYMETRY_MASK_NAME_KEY, PROPERTY_BATHYMETRY_NAME_DEFAULT);

        if (bathymetryMaskName != null && bathymetryMaskName.length() > 2) {
            return bathymetryMaskName;
        } else {
            return PROPERTY_BATHYMETRY_NAME_DEFAULT;
        }
    }


    public static Color getPreferenceBathymetryMaskColor() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyColor(PROPERTY_BATHYMETRY_MASK_COLOR_KEY, PROPERTY_BATHYMETRY_MASK_COLOR_DEFAULT);
    }

    public static double getPreferenceBathymetryMaskTransparency() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyDouble(PROPERTY_BATHYMETRY_MASK_TRANSPARENCY_KEY, PROPERTY_BATHYMETRY_MASK_TRANSPARENCY_DEFAULT);
    }

    public static double getPreferenceBathymetryMaskMinDepth() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyDouble(PROPERTY_BATHYMETRY_MASK_MIN_DEPTH_KEY, PROPERTY_BATHYMETRY_MASK_MIN_DEPTH_DEFAULT);
    }


    public static double getPreferenceBathymetryMaskMaxDepth() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyDouble(PROPERTY_BATHYMETRY_MASK_MAX_DEPTH_KEY, PROPERTY_BATHYMETRY_MASK_MAX_DEPTH_DEFAULT);
    }




    public static boolean getPreferenceBathymetryBandCreate() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyBool(PROPERTY_BATHYMETRY_BAND_CREATE_KEY, PROPERTY_BATHYMETRY_BAND_CREATE_DEFAULT);
    }

    public static boolean getPreferenceTopographyBandCreate() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyBool(PROPERTY_TOPOGRAPHY_BAND_CREATE_KEY, PROPERTY_TOPOGRAPHY_BAND_CREATE_DEFAULT);
    }

    public static boolean getPreferenceElevationBandCreate() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyBool(PROPERTY_ELEVATION_BAND_CREATE_KEY, PROPERTY_ELEVATION_BAND_CREATE_DEFAULT);
    }






}
