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
import org.esa.snap.rcp.preferences.DefaultConfigController;
import org.esa.snap.rcp.preferences.Preference;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;

import javax.swing.*;
import java.awt.*;

/**
 * * Panel handling colorbar layer preferences. Sub-panel of the "Layer"-panel.
 *
 * @author Daniel Knowles
 */


@OptionsPanelController.SubRegistration(location = "SeaDAS",
        displayName = "#Options_DisplayName_SeadasToolbox",
        keywords = "#Options_Keywords_SeadasToolbox",
        keywordsCategory = "Processors",
        id = "OCSSW-Processors")
@org.openide.util.NbBundle.Messages({
        "Options_DisplayName_SeadasToolbox=OCSSW-Processors",
        "Options_Keywords_SeadasToolbox=seadas, ocssw, l2gen"
})
public final class SeadasToolboxController extends DefaultConfigController {

    Property restoreDefaults;


    boolean propertyValueChangeEventsEnabled = true;


    protected PropertySet createPropertySet() {
        return createPropertySet(new SeadasToolboxBean());
    }



    @Override
    protected JPanel createPanel(BindingContext context) {

        //
        // Initialize the default value contained within each property descriptor
        // This is done so subsequently the restoreDefaults actions can be performed
        //

        initPropertyDefaults(context, SeadasToolboxDefaults.PROPERTY_L2GEN_SECTION_KEY, true);
        initPropertyDefaults(context, SeadasToolboxDefaults.PROPERTY_L2GEN_SHORTCUTS_KEY, SeadasToolboxDefaults.PROPERTY_L2GEN_SHORTCUTS_DEFAULT);

        initPropertyDefaults(context, SeadasToolboxDefaults.PROPERTY_L3MAPGEN_SECTION_KEY, true);
        initPropertyDefaults(context, SeadasToolboxDefaults.PROPERTY_L3MAPGEN_PRODUCT_KEY, SeadasToolboxDefaults.PROPERTY_L3MAPGEN_PRODUCT_DEFAULT);
        initPropertyDefaults(context, SeadasToolboxDefaults.PROPERTY_L3MAPGEN_PROJECTION_KEY, SeadasToolboxDefaults.PROPERTY_L3MAPGEN_PROJECTION_DEFAULT);
        initPropertyDefaults(context, SeadasToolboxDefaults.PROPERTY_L3MAPGEN_RESOLUTION_KEY, SeadasToolboxDefaults.PROPERTY_L3MAPGEN_RESOLUTION_DEFAULT);
        initPropertyDefaults(context, SeadasToolboxDefaults.PROPERTY_L3MAPGEN_INTERP_KEY, SeadasToolboxDefaults.PROPERTY_L3MAPGEN_INTERP_DEFAULT);
        initPropertyDefaults(context, SeadasToolboxDefaults.PROPERTY_L3MAPGEN_NORTH_KEY, SeadasToolboxDefaults.PROPERTY_L3MAPGEN_NORTH_DEFAULT);
        initPropertyDefaults(context, SeadasToolboxDefaults.PROPERTY_L3MAPGEN_SOUTH_KEY, SeadasToolboxDefaults.PROPERTY_L3MAPGEN_SOUTH_DEFAULT);
        initPropertyDefaults(context, SeadasToolboxDefaults.PROPERTY_L3MAPGEN_WEST_KEY, SeadasToolboxDefaults.PROPERTY_L3MAPGEN_WEST_DEFAULT);
        initPropertyDefaults(context, SeadasToolboxDefaults.PROPERTY_L3MAPGEN_EAST_KEY, SeadasToolboxDefaults.PROPERTY_L3MAPGEN_EAST_DEFAULT);



        initPropertyDefaults(context, SeadasToolboxDefaults.PROPERTY_RESTORE_SECTION_KEY, true);
        restoreDefaults =  initPropertyDefaults(context, SeadasToolboxDefaults.PROPERTY_RESTORE_DEFAULTS_KEY, SeadasToolboxDefaults.PROPERTY_RESTORE_DEFAULTS_DEFAULT);




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

            context.setComponentsEnabled(SeadasToolboxDefaults.PROPERTY_RESTORE_DEFAULTS_KEY, false);
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
                context.setComponentsEnabled(SeadasToolboxDefaults.PROPERTY_RESTORE_DEFAULTS_KEY, !isDefaults(context));
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
        return new HelpCtx("seadasToolboxPreferences");
    }

    @SuppressWarnings("UnusedDeclaration")
    static class SeadasToolboxBean {





        // L2GEN Section

        @Preference(label = SeadasToolboxDefaults.PROPERTY_L2GEN_SECTION_LABEL,
                key = SeadasToolboxDefaults.PROPERTY_L2GEN_SECTION_KEY,
                description = SeadasToolboxDefaults.PROPERTY_L2GEN_SECTION_TOOLTIP)
        boolean l2genSection = true;



        @Preference(label = SeadasToolboxDefaults.PROPERTY_L2GEN_SHORTCUTS_LABEL,
                key = SeadasToolboxDefaults.PROPERTY_L2GEN_SHORTCUTS_KEY,
                description = SeadasToolboxDefaults.PROPERTY_L2GEN_SHORTCUTS_TOOLTIP)
        boolean l2genL2prodWavelengthShortcuts = SeadasToolboxDefaults.PROPERTY_L2GEN_SHORTCUTS_DEFAULT;



        // L3mapgen Section

        @Preference(label = SeadasToolboxDefaults.PROPERTY_L3MAPGEN_SECTION_LABEL,
                key = SeadasToolboxDefaults.PROPERTY_L3MAPGEN_SECTION_KEY,
                description = SeadasToolboxDefaults.PROPERTY_L3MAPGEN_SECTION_TOOLTIP)
        boolean l3mapgenSection = true;

        @Preference(label = SeadasToolboxDefaults.PROPERTY_L3MAPGEN_PRODUCT_LABEL,
                key = SeadasToolboxDefaults.PROPERTY_L3MAPGEN_PRODUCT_KEY,
                description = SeadasToolboxDefaults.PROPERTY_L3MAPGEN_PRODUCT_TOOLTIP)
        String l3mapgenProductDefault = SeadasToolboxDefaults.PROPERTY_L3MAPGEN_PRODUCT_DEFAULT;

        @Preference(label = SeadasToolboxDefaults.PROPERTY_L3MAPGEN_PROJECTION_LABEL,
                key = SeadasToolboxDefaults.PROPERTY_L3MAPGEN_PROJECTION_KEY,
                description = SeadasToolboxDefaults.PROPERTY_L3MAPGEN_PROJECTION_TOOLTIP)
        String l3mapgenProjectionDefault = SeadasToolboxDefaults.PROPERTY_L3MAPGEN_PROJECTION_DEFAULT;

        @Preference(label = SeadasToolboxDefaults.PROPERTY_L3MAPGEN_RESOLUTION_LABEL,
                key = SeadasToolboxDefaults.PROPERTY_L3MAPGEN_RESOLUTION_KEY,
                description = SeadasToolboxDefaults.PROPERTY_L3MAPGEN_RESOLUTION_TOOLTIP)
        String l3mapgenResolutionDefault = SeadasToolboxDefaults.PROPERTY_L3MAPGEN_RESOLUTION_DEFAULT;

        @Preference(label = SeadasToolboxDefaults.PROPERTY_L3MAPGEN_INTERP_LABEL,
                key = SeadasToolboxDefaults.PROPERTY_L3MAPGEN_INTERP_KEY,
                description = SeadasToolboxDefaults.PROPERTY_L3MAPGEN_INTERP_TOOLTIP)
        String l3mapgenInterpDefault = SeadasToolboxDefaults.PROPERTY_L3MAPGEN_INTERP_DEFAULT;



        @Preference(label = SeadasToolboxDefaults.PROPERTY_L3MAPGEN_NORTH_LABEL,
                key = SeadasToolboxDefaults.PROPERTY_L3MAPGEN_NORTH_KEY,
                description = SeadasToolboxDefaults.PROPERTY_L3MAPGEN_NORTH_TOOLTIP)
        String l3mapgenNorthDefault = SeadasToolboxDefaults.PROPERTY_L3MAPGEN_NORTH_DEFAULT;

        @Preference(label = SeadasToolboxDefaults.PROPERTY_L3MAPGEN_SOUTH_LABEL,
                key = SeadasToolboxDefaults.PROPERTY_L3MAPGEN_SOUTH_KEY,
                description = SeadasToolboxDefaults.PROPERTY_L3MAPGEN_SOUTH_TOOLTIP)
        String l3mapgenSouthDefault = SeadasToolboxDefaults.PROPERTY_L3MAPGEN_SOUTH_DEFAULT;

        @Preference(label = SeadasToolboxDefaults.PROPERTY_L3MAPGEN_WEST_LABEL,
                key = SeadasToolboxDefaults.PROPERTY_L3MAPGEN_WEST_KEY,
                description = SeadasToolboxDefaults.PROPERTY_L3MAPGEN_WEST_TOOLTIP)
        String l3mapgenWestDefault = SeadasToolboxDefaults.PROPERTY_L3MAPGEN_WEST_DEFAULT;

        @Preference(label = SeadasToolboxDefaults.PROPERTY_L3MAPGEN_EAST_LABEL,
                key = SeadasToolboxDefaults.PROPERTY_L3MAPGEN_EAST_KEY,
                description = SeadasToolboxDefaults.PROPERTY_L3MAPGEN_EAST_TOOLTIP)
        String l3mapgenEastDefault = SeadasToolboxDefaults.PROPERTY_L3MAPGEN_EAST_DEFAULT;



        // Restore Defaults Section




        @Preference(label = SeadasToolboxDefaults.PROPERTY_RESTORE_SECTION_LABEL,
                key = SeadasToolboxDefaults.PROPERTY_RESTORE_SECTION_KEY,
                description = SeadasToolboxDefaults.PROPERTY_RESTORE_SECTION_TOOLTIP)
        boolean restoreDefaultsSection = true;

        @Preference(label = SeadasToolboxDefaults.PROPERTY_RESTORE_DEFAULTS_LABEL,
                key = SeadasToolboxDefaults.PROPERTY_RESTORE_DEFAULTS_KEY,
                description = SeadasToolboxDefaults.PROPERTY_RESTORE_DEFAULTS_TOOLTIP)
        boolean restoreDefaults = SeadasToolboxDefaults.PROPERTY_RESTORE_DEFAULTS_DEFAULT;

    }

}
