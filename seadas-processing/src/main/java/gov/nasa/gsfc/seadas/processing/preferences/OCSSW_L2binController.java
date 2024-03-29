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
        id = "L2bin")
@org.openide.util.NbBundle.Messages({
        "Options_DisplayName_OCSSW_L2bin=L2bin",
        "Options_Keywords_OCSSW_L2bin=seadas, ocssw, l2bin"
})
public final class OCSSW_L2binController extends DefaultConfigController {

    Property restoreDefaults;

    boolean propertyValueChangeEventsEnabled = true;


    // Preferences property prefix
    private static final String PROPERTY_L2BIN_ROOT_KEY = SeadasToolboxDefaults.PROPERTY_SEADAS_ROOT_KEY + ".l2bin";

    public static final String PROPERTY_L2BIN_L3BPROD_KEY = PROPERTY_L2BIN_ROOT_KEY + ".l3bprod";
    public static final String PROPERTY_L2BIN_L3BPROD_LABEL = "l3bprod";
    public static final String PROPERTY_L2BIN_L3BPROD_TOOLTIP = "Product (or product list)";
    public static final String PROPERTY_L2BIN_L3BPROD_DEFAULT = "";

    public static final String PROPERTY_L2BIN_PRODTYPE_KEY = PROPERTY_L2BIN_ROOT_KEY + ".prodtype";
    public static final String PROPERTY_L2BIN_PRODTYPE_LABEL = "prodtype";
    public static final String PROPERTY_L2BIN_PRODTYPE_TOOLTIP = "Product type";
    public static final String PROPERTY_L2BIN_PRODTYPE_DEFAULT = "regional";

    public static final String PROPERTY_L2BIN_RESOLUTION_KEY = PROPERTY_L2BIN_ROOT_KEY + ".resolution";
    public static final String PROPERTY_L2BIN_RESOLUTION_LABEL = "resolution";
    public static final String PROPERTY_L2BIN_RESOLUTION_TOOLTIP = "Bin resolution";
    public static final String PROPERTY_L2BIN_RESOLUTION_DEFAULT = "";

    public static final String PROPERTY_L2BIN_AREA_WEIGHTING_KEY = PROPERTY_L2BIN_ROOT_KEY + ".area_weighting";
    public static final String PROPERTY_L2BIN_AREA_WEIGHTING_LABEL = "area_weighting";
    public static final String PROPERTY_L2BIN_AREA_WEIGHTING_TOOLTIP = "Area Weighting";
    public static final String PROPERTY_L2BIN_AREA_WEIGHTING_DEFAULT = "0";

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

        //
        // Initialize the default value contained within each property descriptor
        // This is done so subsequently the restoreDefaults actions can be performed
        //

        initPropertyDefaults(context, PROPERTY_L2BIN_L3BPROD_KEY, PROPERTY_L2BIN_L3BPROD_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L2BIN_PRODTYPE_KEY, PROPERTY_L2BIN_PRODTYPE_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L2BIN_RESOLUTION_KEY, PROPERTY_L2BIN_RESOLUTION_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L2BIN_AREA_WEIGHTING_KEY, PROPERTY_L2BIN_AREA_WEIGHTING_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L2BIN_FLAGUSE_KEY, PROPERTY_L2BIN_FLAGUSE_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L2BIN_LATNORTH_KEY, PROPERTY_L2BIN_LATNORTH_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L2BIN_LATSOUTH_KEY, PROPERTY_L2BIN_LATSOUTH_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L2BIN_LONWEST_KEY, PROPERTY_L2BIN_LONWEST_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L2BIN_LONEAST_KEY, PROPERTY_L2BIN_LONEAST_DEFAULT);

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
        return new HelpCtx("OCSSW_L2binPreferences");
    }

    @SuppressWarnings("UnusedDeclaration")
    static class SeadasToolboxBean {

        @Preference(key = PROPERTY_L2BIN_L3BPROD_KEY,
                label = PROPERTY_L2BIN_L3BPROD_LABEL,
                description = PROPERTY_L2BIN_L3BPROD_TOOLTIP)
        String l2binL3bprodDefault = PROPERTY_L2BIN_L3BPROD_DEFAULT;

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

        @Preference(key = PROPERTY_L2BIN_FLAGUSE_KEY,
                label = PROPERTY_L2BIN_FLAGUSE_LABEL,
                description = PROPERTY_L2BIN_FLAGUSE_TOOLTIP)
        String l2binFlaguseDefault = PROPERTY_L2BIN_FLAGUSE_DEFAULT;

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




}
