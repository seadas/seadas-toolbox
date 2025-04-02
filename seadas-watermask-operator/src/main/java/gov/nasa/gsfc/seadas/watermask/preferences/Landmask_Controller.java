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

package gov.nasa.gsfc.seadas.watermask.preferences;

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
        displayName = "#Options_DisplayName_LandCoastMask",
        keywords = "#Options_Keywords_LandCoastMask",
        keywordsCategory = "General Tools",
        id = "LandCoastMask_preferences")
@org.openide.util.NbBundle.Messages({
        "Options_DisplayName_LandCoastMask=Land Coast Mask",
        "Options_Keywords_LandCoastMask=seadas, Land Coast Mask"
})
public final class Landmask_Controller extends DefaultConfigController {

    Property restoreDefaults;

    boolean propertyValueChangeEventsEnabled = true;


    public static final String RESOLUTION_50m = "50 m (SRTM_GC)";
    public static final String RESOLUTION_150m = "150 m (SRTM_GC)";
    public static final String RESOLUTION_1km = "1 km (GSHHS)";
    public static final String RESOLUTION_10km = "10 km (GSHHS)";

    // Preferences property prefix
    private static final String PROPERTY_LANDMASK_ROOT_KEY = "seadas.toolbox.landcoast";


    public static final String PROPERTY_LANDMASK_RESOLUTION_KEY = PROPERTY_LANDMASK_ROOT_KEY + ".landmask.resolution";
    public static final String PROPERTY_LANDMASK_RESOLUTION_LABEL = "Resolution";
    public static final String PROPERTY_LANDMASK_RESOLUTION_TOOLTIP = "Resolution";
    public static final String PROPERTY_LANDMASK_RESOLUTION_DEFAULT = RESOLUTION_1km;



    public static final String PROPERTY_LANDMASK_SECTION_KEY = PROPERTY_LANDMASK_ROOT_KEY + ".landmask.section";
    public static final String PROPERTY_LANDMASK_SECTION_LABEL = "Land Mask Options";
    public static final String PROPERTY_LANDMASK_SECTION_TOOLTIP = "Land mask options";

    public static final String PROPERTY_LANDMASK_NAME_KEY = PROPERTY_LANDMASK_ROOT_KEY + ".landmask.name";
    public static final String PROPERTY_LANDMASK_NAME_LABEL = "Land Mask Name";
    public static final String PROPERTY_LANDMASK_NAME_TOOLTIP = "Land mask name";
    public static final String PROPERTY_LANDMASK_NAME_DEFAULT = "LandMask";

    public static final String PROPERTY_LANDMASK_COLOR_KEY = PROPERTY_LANDMASK_ROOT_KEY + ".landmask.color";
    public static final String PROPERTY_LANDMASK_COLOR_LABEL = "Land Mask Color";
    public static final String PROPERTY_LANDMASK_COLOR_TOOLTIP = "Land mask color";
    public static final Color PROPERTY_LANDMASK_COLOR_DEFAULT = new Color(70,70,0);

    public static final String PROPERTY_LANDMASK_TRANSPARENCY_KEY = PROPERTY_LANDMASK_ROOT_KEY + ".landmask.transparency";
    public static final String PROPERTY_LANDMASK_TRANSPARENCY_LABEL = "Land Mask Transparency";
    public static final String PROPERTY_LANDMASK_TRANSPARENCY_TOOLTIP = "Land mask transparency ";
    public static final double PROPERTY_LANDMASK_TRANSPARENCY_DEFAULT = 0.0;

    public static final String PROPERTY_LANDMASK_SHOW_KEY = PROPERTY_LANDMASK_ROOT_KEY + ".landmask.show";
    public static final String PROPERTY_LANDMASK_SHOW_LABEL = "Land Mask Show";
    public static final String PROPERTY_LANDMASK_SHOW_TOOLTIP = "Land mask will be displayed in all bands ";
    public static final boolean PROPERTY_LANDMASK_SHOW_DEFAULT = false;



    // Property Setting: Restore Defaults

    private static final String PROPERTY_RESTORE_KEY_SUFFIX = PROPERTY_LANDMASK_ROOT_KEY + ".restore.defaults";

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
        initPropertyDefaults(context, PROPERTY_LANDMASK_RESOLUTION_KEY, PROPERTY_LANDMASK_RESOLUTION_DEFAULT);

        initPropertyDefaults(context, PROPERTY_LANDMASK_SECTION_KEY, true);
        initPropertyDefaults(context, PROPERTY_LANDMASK_COLOR_KEY, PROPERTY_LANDMASK_COLOR_DEFAULT);
        initPropertyDefaults(context, PROPERTY_LANDMASK_NAME_KEY, PROPERTY_LANDMASK_NAME_DEFAULT);
        initPropertyDefaults(context, PROPERTY_LANDMASK_TRANSPARENCY_KEY, PROPERTY_LANDMASK_TRANSPARENCY_DEFAULT);
        initPropertyDefaults(context, PROPERTY_LANDMASK_SHOW_KEY, PROPERTY_LANDMASK_SHOW_DEFAULT);



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
        return new HelpCtx("coastlineLandMasks");
    }

    @SuppressWarnings("UnusedDeclaration")
    static class SeadasToolboxBean {



        @Preference(key = PROPERTY_LANDMASK_RESOLUTION_KEY,
                label = PROPERTY_LANDMASK_RESOLUTION_LABEL,
                valueSet = {RESOLUTION_50m, RESOLUTION_150m, RESOLUTION_1km, RESOLUTION_10km},
                description = PROPERTY_LANDMASK_RESOLUTION_TOOLTIP)
        String landmaskResolutionDefault = PROPERTY_LANDMASK_RESOLUTION_DEFAULT;

        @Preference(key = PROPERTY_LANDMASK_SECTION_KEY,
                label = PROPERTY_LANDMASK_SECTION_LABEL,
                description = PROPERTY_LANDMASK_SECTION_TOOLTIP)
        boolean landmaskSectionDefault = true;

        @Preference(key = PROPERTY_LANDMASK_NAME_KEY,
                label = PROPERTY_LANDMASK_NAME_LABEL,
                description = PROPERTY_LANDMASK_NAME_TOOLTIP)
        String landmaskNameDefault = PROPERTY_LANDMASK_NAME_DEFAULT;

        @Preference(key = PROPERTY_LANDMASK_COLOR_KEY,
                label = PROPERTY_LANDMASK_COLOR_LABEL,
                description = PROPERTY_LANDMASK_COLOR_TOOLTIP)
        Color landmaskColorDefault = PROPERTY_LANDMASK_COLOR_DEFAULT;

        @Preference(key = PROPERTY_LANDMASK_TRANSPARENCY_KEY,
                label = PROPERTY_LANDMASK_TRANSPARENCY_LABEL,
                description = PROPERTY_LANDMASK_TRANSPARENCY_TOOLTIP)
        double landmaskTransparencyDefault = PROPERTY_LANDMASK_TRANSPARENCY_DEFAULT;

        @Preference(key = PROPERTY_LANDMASK_SHOW_KEY,
                label = PROPERTY_LANDMASK_SHOW_LABEL,
                description = PROPERTY_LANDMASK_SHOW_TOOLTIP)
        boolean landmaskShowDefault = PROPERTY_LANDMASK_SHOW_DEFAULT;



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



    public static String getPreferenceLandMaskResolution() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(PROPERTY_LANDMASK_RESOLUTION_KEY, PROPERTY_LANDMASK_RESOLUTION_DEFAULT);
    }


    public static String getPreferenceLandMaskName() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        String landMaskName = preferences.getPropertyString(PROPERTY_LANDMASK_NAME_KEY, PROPERTY_LANDMASK_NAME_DEFAULT);

        if (landMaskName != null && landMaskName.length() > 2) {
            return landMaskName;
        } else {
            return PROPERTY_LANDMASK_NAME_DEFAULT;
        }
    }


    public static Color getPreferenceLandMaskColor() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyColor(PROPERTY_LANDMASK_COLOR_KEY, PROPERTY_LANDMASK_COLOR_DEFAULT);
    }

    public static double getPreferenceLandMaskTransparency() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyDouble(PROPERTY_LANDMASK_TRANSPARENCY_KEY, PROPERTY_LANDMASK_TRANSPARENCY_DEFAULT);
    }

    public static boolean getPreferenceLandMaskShow() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyBool(PROPERTY_LANDMASK_SHOW_KEY, PROPERTY_LANDMASK_SHOW_DEFAULT);
    }

    

}
