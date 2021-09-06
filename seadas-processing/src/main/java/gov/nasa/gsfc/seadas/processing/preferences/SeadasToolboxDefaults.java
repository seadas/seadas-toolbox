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
    public static final boolean PROPERTY_L2GEN_SHORTCUTS_DEFAULT = false;
    public static final Class PROPERTY_L2GEN_SHORTCUTS_TYPE = Boolean.class;

}
