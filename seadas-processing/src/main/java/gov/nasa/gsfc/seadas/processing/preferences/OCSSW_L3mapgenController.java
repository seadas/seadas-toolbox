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

    private static final String FAV = ".favorites";

    Property restoreDefaults;
    Property projection;
    Property north;
    Property south;
    Property west;
    Property east;
    
    Property fav1SetToDefault;
    Property fav1Projection;
    Property fav1NSWE;

    Property fav2SetToDefault;
    Property fav2Projection;
    Property fav2NSWE;

    Property fav3SetToDefault;
    Property fav3Projection;
    Property fav3NSWE;

    boolean propertyValueChangeEventsEnabled = true;


    // todo add more parameters:   wavelength_3d, suite, fudge, product_rgb, num_cache
    // todo add some more favorite projections
    // todo compare param tooltips here and with .xml file and with ocssw help
    // todo update help page



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

    public static final String PROPERTY_L3MAPGEN_FAV_PROJECTIONS_SECTION_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + ".FAV.projections.section";
    public static final String PROPERTY_L3MAPGEN_FAV_PROJECTIONS_SECTION_LABEL = "Favorite Projections";
    public static final String PROPERTY_L3MAPGEN_FAV_PROJECTIONS_SECTION_TOOLTIP = "Set projection as default";

    private static final String INDENTATION_SPACES = "           ";

    public static final String PROPERTY_L3MAPGEN_FAV1_SET_AS_DEFAULT_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + FAV + ".1.set.as.default";
    public static final String PROPERTY_L3MAPGEN_FAV1_SET_AS_DEFAULT_LABEL = "1. Set as Default";
    public static final String PROPERTY_L3MAPGEN_FAV1_SET_AS_DEFAULT_TOOLTIP = "Set projection and/or bounds as default";
    public static final boolean PROPERTY_L3MAPGEN_FAV1_SET_AS_DEFAULT_DEFAULT = false;

    public static final String PROPERTY_L3MAPGEN_FAV1_PROJECTION_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + FAV + ".1.projection";
    public static final String PROPERTY_L3MAPGEN_FAV1_PROJECTION_LABEL = INDENTATION_SPACES + "Projection";
    public static final String PROPERTY_L3MAPGEN_FAV1_PROJECTION_TOOLTIP = "Custom projection";
    public static final String PROPERTY_L3MAPGEN_FAV1_PROJECTION_DEFAULT = "+proj=laea +lon_0=152.0 +lat_0=-22.0";

    public static final String PROPERTY_L3MAPGEN_FAV1_PROJECTION_DESC_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + FAV + ".1.projection.description";
    public static final String PROPERTY_L3MAPGEN_FAV1_PROJECTION_DESC_LABEL = INDENTATION_SPACES + "Description";
    public static final String PROPERTY_L3MAPGEN_FAV1_PROJECTION_DESC_TOOLTIP = "Description of custom projection";
    public static final String PROPERTY_L3MAPGEN_FAV1_PROJECTION_DESC_DEFAULT = "Great Barrier Reef - Southern (Lambert Equal Area)";

    public static final String PROPERTY_L3MAPGEN_FAV1_NSWE_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + FAV + ".1.nswe.bounds";
    public static final String PROPERTY_L3MAPGEN_FAV1_NSWE_LABEL = INDENTATION_SPACES + "N,S,W,E Bounds";
    public static final String PROPERTY_L3MAPGEN_FAV1_NSWE_TOOLTIP = "Geographic boundaries.  Comma delimited. Format N,S,W,E. ";
    public static final String PROPERTY_L3MAPGEN_FAV1_NSWE_DEFAULT = "45,23,34,35";



    public static final String PROPERTY_L3MAPGEN_FAV2_SET_AS_DEFAULT_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + FAV + ".2.set.as.default";
    public static final String PROPERTY_L3MAPGEN_FAV2_SET_AS_DEFAULT_LABEL = "2. Set as Default";
    public static final String PROPERTY_L3MAPGEN_FAV2_SET_AS_DEFAULT_TOOLTIP = "Set projection and/or bounds as default";
    public static final boolean PROPERTY_L3MAPGEN_FAV2_SET_AS_DEFAULT_DEFAULT = false;

    public static final String PROPERTY_L3MAPGEN_FAV2_PROJECTION_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + FAV + ".2.projection";
    public static final String PROPERTY_L3MAPGEN_FAV2_PROJECTION_LABEL = INDENTATION_SPACES + "Projection";
    public static final String PROPERTY_L3MAPGEN_FAV2_PROJECTION_TOOLTIP = "Custom projection";
    public static final String PROPERTY_L3MAPGEN_FAV2_PROJECTION_DEFAULT = "+proj=laea +lon_0=-76.1 +lat_0=37.8";

    public static final String PROPERTY_L3MAPGEN_FAV2_PROJECTION_DESC_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + FAV + ".2.projection.description";
    public static final String PROPERTY_L3MAPGEN_FAV2_PROJECTION_DESC_LABEL = INDENTATION_SPACES + "Description";
    public static final String PROPERTY_L3MAPGEN_FAV2_PROJECTION_DESC_TOOLTIP = "Description of custom projection";
    public static final String PROPERTY_L3MAPGEN_FAV2_PROJECTION_DESC_DEFAULT = "Chesapeake Bay (Lambert Equal Area)";

    public static final String PROPERTY_L3MAPGEN_FAV2_NSWE_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + FAV + ".2.nswe.bounds";
    public static final String PROPERTY_L3MAPGEN_FAV2_NSWE_LABEL = INDENTATION_SPACES + "N,S,W,E Bounds";
    public static final String PROPERTY_L3MAPGEN_FAV2_NSWE_TOOLTIP = "Geographic boundaries.  Comma delimited. Format N,S,W,E. ";
    public static final String PROPERTY_L3MAPGEN_FAV2_NSWE_DEFAULT = "45,23,34,35";



    public static final String PROPERTY_L3MAPGEN_FAV3_SET_AS_DEFAULT_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + FAV + ".3.set.as.default";
    public static final String PROPERTY_L3MAPGEN_FAV3_SET_AS_DEFAULT_LABEL = "3. Set as Default";
    public static final String PROPERTY_L3MAPGEN_FAV3_SET_AS_DEFAULT_TOOLTIP = "Set projection and/or bounds as default";
    public static final boolean PROPERTY_L3MAPGEN_FAV3_SET_AS_DEFAULT_DEFAULT = false;

    public static final String PROPERTY_L3MAPGEN_FAV3_PROJECTION_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + FAV + ".3.projection";
    public static final String PROPERTY_L3MAPGEN_FAV3_PROJECTION_LABEL = INDENTATION_SPACES + "Projection";
    public static final String PROPERTY_L3MAPGEN_FAV3_PROJECTION_TOOLTIP = "Custom projection";
    public static final String PROPERTY_L3MAPGEN_FAV3_PROJECTION_DEFAULT = "";

    public static final String PROPERTY_L3MAPGEN_FAV3_PROJECTION_DESC_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + FAV + ".3.description";
    public static final String PROPERTY_L3MAPGEN_FAV3_PROJECTION_DESC_LABEL = INDENTATION_SPACES + "Description";
    public static final String PROPERTY_L3MAPGEN_FAV3_PROJECTION_DESC_TOOLTIP = "Description of custom projection";
    public static final String PROPERTY_L3MAPGEN_FAV3_PROJECTION_DESC_DEFAULT = "";

    public static final String PROPERTY_L3MAPGEN_FAV3_NSWE_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + FAV + ".3.nswe.bounds";
    public static final String PROPERTY_L3MAPGEN_FAV3_NSWE_LABEL = INDENTATION_SPACES + "N,S,W,E Bounds";
    public static final String PROPERTY_L3MAPGEN_FAV3_NSWE_TOOLTIP = "Geographic boundaries.  Comma delimited. Format N,S,W,E. ";
    public static final String PROPERTY_L3MAPGEN_FAV3_NSWE_DEFAULT = "";





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
        projection = initPropertyDefaults(context, PROPERTY_L3MAPGEN_PROJECTION_KEY, PROPERTY_L3MAPGEN_PROJECTION_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L3MAPGEN_RESOLUTION_KEY, PROPERTY_L3MAPGEN_RESOLUTION_DEFAULT);
        initPropertyDefaults(context, PROPERTY_L3MAPGEN_INTERP_KEY, PROPERTY_L3MAPGEN_INTERP_DEFAULT);
        north = initPropertyDefaults(context, PROPERTY_L3MAPGEN_NORTH_KEY, PROPERTY_L3MAPGEN_NORTH_DEFAULT);
        south = initPropertyDefaults(context, PROPERTY_L3MAPGEN_SOUTH_KEY, PROPERTY_L3MAPGEN_SOUTH_DEFAULT);
        west = initPropertyDefaults(context, PROPERTY_L3MAPGEN_WEST_KEY, PROPERTY_L3MAPGEN_WEST_DEFAULT);
        east = initPropertyDefaults(context, PROPERTY_L3MAPGEN_EAST_KEY, PROPERTY_L3MAPGEN_EAST_DEFAULT);

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
                    favSet.setValue(true);

                    projection.setValue("");
                    if (favProjection.getValue() != null) {
                        if (favProjection.getValue().toString() != null) {
                            projection.setValue(favProjection.getValue().toString().trim());
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


    



}
