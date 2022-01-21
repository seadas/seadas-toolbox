package gov.nasa.gsfc.seadas.processing.preferences;

public class SeadasToolboxDefaults {

    // Preferences property prefix
    private static final String PROPERTY_SEADAS_ROOT_KEY = "seadas.toolbox";

    private static final String PROPERTY_L2GEN_ROOT_KEY = PROPERTY_SEADAS_ROOT_KEY + ".l2gen";

    public static final String PROPERTY_L2GEN_SECTION_KEY = PROPERTY_L2GEN_ROOT_KEY + ".section";
    public static final String PROPERTY_L2GEN_SECTION_LABEL = "L2GEN";
    public static final String PROPERTY_L2GEN_SECTION_TOOLTIP = "Options for the L2gen GUI";


    public static final String PROPERTY_L2GEN_SHORTCUTS_KEY = PROPERTY_L2GEN_ROOT_KEY + ".shortcuts";
    public static final String PROPERTY_L2GEN_SHORTCUTS_LABEL = "L2prod Wavelength Shortcuts";
    public static final String PROPERTY_L2GEN_SHORTCUTS_TOOLTIP = "Use wavelength shortcuts (i.e. Rrs_vvv, Rrs_iii) in l2prod";
    public static final boolean PROPERTY_L2GEN_SHORTCUTS_DEFAULT = true;
    public static final Class PROPERTY_L2GEN_SHORTCUTS_TYPE = Boolean.class;


    public static final String PROPERTY_VALID_TAGS_KEY = ".installer" + ".operational.tags";
    public static final String PROPERTY_VALID_TAGS_LABEL = "Show only operational release tags";
    public static final String PROPERTY_VALID_TAGS_TOOLTIP = "Allow only operational release tags in the GUI installer";
    public static final boolean PROPERTY_VALID_TAGS_DEFAULT = true;
    public static final Class PROPERTY_VALID_TAGS_TYPE = Boolean.class;


    // Property Setting: Restore Defaults

    private static final String PROPERTY_RESTORE_KEY_SUFFIX = PROPERTY_SEADAS_ROOT_KEY + ".restore.defaults";

    public static final String PROPERTY_RESTORE_SECTION_KEY = PROPERTY_RESTORE_KEY_SUFFIX + ".section";
    public static final String PROPERTY_RESTORE_SECTION_LABEL = "Restore";
    public static final String PROPERTY_RESTORE_SECTION_TOOLTIP = "Restores preferences to the package defaults";

    public static final String PROPERTY_RESTORE_DEFAULTS_KEY = PROPERTY_RESTORE_KEY_SUFFIX + ".apply";
    public static final String PROPERTY_RESTORE_DEFAULTS_LABEL = "Default (SeaDAS Toolbox Preferences)";
    public static final String PROPERTY_RESTORE_DEFAULTS_TOOLTIP = "Restore all color bar legend preferences to the original default";
    public static final boolean PROPERTY_RESTORE_DEFAULTS_DEFAULT = false;




}
