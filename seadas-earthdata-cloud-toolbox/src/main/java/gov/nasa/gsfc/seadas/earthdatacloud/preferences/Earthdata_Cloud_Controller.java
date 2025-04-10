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

package gov.nasa.gsfc.seadas.earthdatacloud.preferences;

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
        displayName = "#Options_DisplayName_Earthdata_Cloud",
        keywords = "#Options_Keywords_Earthdata_Cloud",
        keywordsCategory = "General Tools",
        id = "Earthdata_Cloud_preferences",
        position = 7)

@org.openide.util.NbBundle.Messages({
        "Options_DisplayName_Earthdata_Cloud=Earthdata Cloud",
        "Options_Keywords_Earthdata_Cloud=seadas, earthdata, cloud"
})
public final class Earthdata_Cloud_Controller extends DefaultConfigController {

    Property restoreDefaults;

    boolean propertyValueChangeEventsEnabled = true;

    // Preferences property prefix
    private static final String PROPERTY_ROOT_KEY = "seadas.toolbox.earthdata_cloud";


    public static final String PROPERTY_SATELLITE_NAME_KEY = PROPERTY_ROOT_KEY + ".satellite";
    public static final String PROPERTY_SATELLITE_LABEL = "Satellite/Instrument";
    public static final String PROPERTY_SATELLITE_TOOLTIP = "Satellite";
    public static final String PROPERTY_SATELLITE_DEFAULT = "";

    public static final String PROPERTY_DATA_LEVEL_KEY = PROPERTY_ROOT_KEY + ".data_level";
    public static final String PROPERTY_DATA_LEVEL_LABEL = "Data Level";
    public static final String PROPERTY_DATA_LEVEL_TOOLTIP = "Data level";
    public static final String PROPERTY_DATA_LEVEL_DEFAULT = "";

    public static final String PROPERTY_PRODUCT_KEY = PROPERTY_ROOT_KEY + ".product";
    public static final String PROPERTY_PRODUCT_LABEL = "Product Name";
    public static final String PROPERTY_PRODUCT_TOOLTIP = "Product";
    public static final String PROPERTY_PRODUCT_DEFAULT = "";

    public static final String PROPERTY_MINLAT_KEY = PROPERTY_ROOT_KEY + ".minlat";
    public static final String PROPERTY_MINLAT_LABEL = "Min Lat";
    public static final String PROPERTY_MINLAT_TOOLTIP = "Minumum latitude";
    public static final String PROPERTY_MINLAT_DEFAULT = "";

    public static final String PROPERTY_MAXLAT_KEY = PROPERTY_ROOT_KEY + ".maxlat";
    public static final String PROPERTY_MAXLAT_LABEL = "Max Lat";
    public static final String PROPERTY_MAXLAT_TOOLTIP = "Maximum latitude";
    public static final String PROPERTY_MAXLAT_DEFAULT = "";

    public static final String PROPERTY_MINLON_KEY = PROPERTY_ROOT_KEY + ".minlon";
    public static final String PROPERTY_MINLON_LABEL = "Min Lon";
    public static final String PROPERTY_MINLON_TOOLTIP = "Minimum longitude";
    public static final String PROPERTY_MINLON_DEFAULT = "";

    public static final String PROPERTY_MAXLON_KEY = PROPERTY_ROOT_KEY + ".maxlon";
    public static final String PROPERTY_MAXLON_LABEL = "Max Lon";
    public static final String PROPERTY_MAXLON_TOOLTIP = "Maximum longitude";
    public static final String PROPERTY_MAXLON_DEFAULT = "";




    // Property Setting: Restore Defaults

    private static final String PROPERTY_RESTORE_KEY_SUFFIX = PROPERTY_ROOT_KEY + ".restore.defaults";

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
        initPropertyDefaults(context, PROPERTY_SATELLITE_NAME_KEY, PROPERTY_SATELLITE_DEFAULT);
        initPropertyDefaults(context, PROPERTY_DATA_LEVEL_KEY, PROPERTY_DATA_LEVEL_DEFAULT);
        initPropertyDefaults(context, PROPERTY_PRODUCT_KEY, PROPERTY_PRODUCT_DEFAULT);
        initPropertyDefaults(context, PROPERTY_MINLAT_KEY, PROPERTY_MINLAT_DEFAULT);
        initPropertyDefaults(context, PROPERTY_MAXLAT_KEY, PROPERTY_MAXLAT_DEFAULT);
        initPropertyDefaults(context, PROPERTY_MINLON_KEY, PROPERTY_MINLON_DEFAULT);
        initPropertyDefaults(context, PROPERTY_MAXLON_KEY, PROPERTY_MAXLON_DEFAULT);



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
        return new HelpCtx("coastlineLandMasks");
    }

    @SuppressWarnings("UnusedDeclaration")
    static class SeadasToolboxBean {

        @Preference(key = PROPERTY_SATELLITE_NAME_KEY,
                label = PROPERTY_SATELLITE_LABEL,
                description = PROPERTY_SATELLITE_TOOLTIP)
        String satelliteDefault = PROPERTY_SATELLITE_DEFAULT;

        @Preference(key = PROPERTY_DATA_LEVEL_KEY,
                label = PROPERTY_DATA_LEVEL_LABEL,
                description = PROPERTY_DATA_LEVEL_TOOLTIP)
        String dataLevelDefault = PROPERTY_DATA_LEVEL_DEFAULT;

        @Preference(key = PROPERTY_PRODUCT_KEY,
                label = PROPERTY_PRODUCT_LABEL,
                description = PROPERTY_PRODUCT_TOOLTIP)
        String productDefault = PROPERTY_PRODUCT_DEFAULT;

        @Preference(key = PROPERTY_MINLAT_KEY,
                label = PROPERTY_MINLAT_LABEL,
                description = PROPERTY_MINLAT_TOOLTIP)
        String minlatDefault = PROPERTY_MINLAT_DEFAULT;

        @Preference(key = PROPERTY_MAXLAT_KEY,
                label = PROPERTY_MAXLAT_LABEL,
                description = PROPERTY_MAXLAT_TOOLTIP)
        String maxlatDefault = PROPERTY_MAXLAT_DEFAULT;

        @Preference(key = PROPERTY_MINLON_KEY,
                label = PROPERTY_MINLON_LABEL,
                description = PROPERTY_MINLON_TOOLTIP)
        String minlonDefault = PROPERTY_MINLON_DEFAULT;

        @Preference(key = PROPERTY_MAXLON_KEY,
                label = PROPERTY_MAXLON_LABEL,
                description = PROPERTY_MAXLON_TOOLTIP)
        String maxlonDefault = PROPERTY_MAXLON_DEFAULT;



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



    public static String getPreferenceSatellite() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(PROPERTY_SATELLITE_NAME_KEY, PROPERTY_SATELLITE_DEFAULT);
    }


    public static String getPreferenceDataLevel() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(PROPERTY_DATA_LEVEL_KEY, PROPERTY_DATA_LEVEL_DEFAULT);
    }

    public static String getPreferenceProduct() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        return preferences.getPropertyString(PROPERTY_PRODUCT_KEY, PROPERTY_PRODUCT_DEFAULT);
    }


    public static String getPreferenceMinLat() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        String minLat = preferences.getPropertyString(PROPERTY_MINLAT_KEY, PROPERTY_MINLAT_DEFAULT);
        return authenticatedStringNumber(minLat);
    }

    public static String getPreferenceMaxLat() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        String maxLat = preferences.getPropertyString(PROPERTY_MAXLAT_KEY, PROPERTY_MAXLAT_DEFAULT);
        return authenticatedStringNumber(maxLat);
    }

    public static String getPreferenceMinLon() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        String minLon = preferences.getPropertyString(PROPERTY_MINLON_KEY, PROPERTY_MINLON_DEFAULT);
        return authenticatedStringNumber(minLon);
    }

    public static String getPreferenceMaxLon() {
        final PropertyMap preferences = SnapApp.getDefault().getAppContext().getPreferences();
        String maxLon = preferences.getPropertyString(PROPERTY_MAXLON_KEY, PROPERTY_MAXLON_DEFAULT);
        return authenticatedStringNumber(maxLon);
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
