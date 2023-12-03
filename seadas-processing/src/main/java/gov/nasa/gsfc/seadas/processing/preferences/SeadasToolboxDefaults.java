package gov.nasa.gsfc.seadas.processing.preferences;

public class SeadasToolboxDefaults {

    // Preferences property prefix
    private static final String PROPERTY_SEADAS_ROOT_KEY = "seadas.toolbox";

    private static final String PROPERTY_L2GEN_ROOT_KEY = PROPERTY_SEADAS_ROOT_KEY + ".l2gen";

    public static final String PROPERTY_L2GEN_SECTION_KEY = PROPERTY_L2GEN_ROOT_KEY + ".section";
    public static final String PROPERTY_L2GEN_SECTION_LABEL = "l2gen";
    public static final String PROPERTY_L2GEN_SECTION_TOOLTIP = "Options for the L2gen GUI";


    public static final String PROPERTY_L2GEN_SHORTCUTS_KEY = PROPERTY_L2GEN_ROOT_KEY + ".shortcuts";
    public static final String PROPERTY_L2GEN_SHORTCUTS_LABEL = "L2prod Wavelength Shortcuts";
    public static final String PROPERTY_L2GEN_SHORTCUTS_TOOLTIP = "Use wavelength shortcuts (i.e. Rrs_vvv, Rrs_iii) in l2prod";
    public static final boolean PROPERTY_L2GEN_SHORTCUTS_DEFAULT = true;
    public static final Class PROPERTY_L2GEN_SHORTCUTS_TYPE = Boolean.class;




    private static final String PROPERTY_L3MAPGEN_ROOT_KEY = PROPERTY_SEADAS_ROOT_KEY + ".l3mapgen";

    public static final String PROPERTY_L3MAPGEN_SECTION_KEY = PROPERTY_L3MAPGEN_ROOT_KEY + ".section";
    public static final String PROPERTY_L3MAPGEN_SECTION_LABEL = "l3mapgen";
    public static final String PROPERTY_L3MAPGEN_SECTION_TOOLTIP = "Options for the l3mapgen GUI";


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

    private static final String PROPERTY_RESTORE_KEY_SUFFIX = PROPERTY_SEADAS_ROOT_KEY + ".restore.defaults";

    public static final String PROPERTY_RESTORE_SECTION_KEY = PROPERTY_RESTORE_KEY_SUFFIX + ".section";
    public static final String PROPERTY_RESTORE_SECTION_LABEL = "Restore";
    public static final String PROPERTY_RESTORE_SECTION_TOOLTIP = "Restores preferences to the package defaults";

    public static final String PROPERTY_RESTORE_DEFAULTS_KEY = PROPERTY_RESTORE_KEY_SUFFIX + ".apply";
    public static final String PROPERTY_RESTORE_DEFAULTS_LABEL = "Default (SeaDAS Toolbox Preferences)";
    public static final String PROPERTY_RESTORE_DEFAULTS_TOOLTIP = "Restore all color bar legend preferences to the original default";
    public static final boolean PROPERTY_RESTORE_DEFAULTS_DEFAULT = false;






    public static final String PROPERTY_ONLY_RELEASE_TAGS_KEY = PROPERTY_SEADAS_ROOT_KEY + ".installer" + ".include.only.release.tags";
    public static final String PROPERTY_ONLY_RELEASE_TAGS_LABEL = "Include only SeaDAS-OCSSW release tags";
    public static final String PROPERTY_ONLY_RELEASE_TAGS_TOOLTIP = "Include only official SeaDAS-OCSSW release tags in the GUI installer";
    public static final boolean PROPERTY_ONLY_RELEASE_TAGS_DEFAULT = true;
    public static final Class PROPERTY_ONLY_RELEASE_TAGS_TYPE = Boolean.class;


    // Property Setting: Restore Defaults

    private static final String PROPERTY_INSTALLER_RESTORE_KEY_SUFFIX = PROPERTY_SEADAS_ROOT_KEY + ".installer.restore.defaults";

    public static final String PROPERTY_INSTALLER_RESTORE_SECTION_KEY = PROPERTY_INSTALLER_RESTORE_KEY_SUFFIX + ".section";
    public static final String PROPERTY_INSTALLER_RESTORE_SECTION_LABEL = "Restore";
    public static final String PROPERTY_INSTALLER_RESTORE_SECTION_TOOLTIP = "Restores preferences to the package defaults";

    public static final String PROPERTY_INSTALLER_RESTORE_DEFAULTS_KEY = PROPERTY_INSTALLER_RESTORE_KEY_SUFFIX + ".apply";
    public static final String PROPERTY_INSTALLER_RESTORE_DEFAULTS_LABEL = "Default (SeaDAS Toolbox Preferences)";
    public static final String PROPERTY_INSTALLER_RESTORE_DEFAULTS_TOOLTIP = "Restore all color bar legend preferences to the original default";
    public static final boolean PROPERTY_INSTALLER_RESTORE_DEFAULTS_DEFAULT = false;





}
