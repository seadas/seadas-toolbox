package gov.nasa.gsfc.seadas.earthdatacloud.matchup.ui;

import gov.nasa.gsfc.seadas.earthdatacloud.matchup.HarmonyEnvironment;

/** How the window names Harmony's environments. */
final class EnvironmentNames {

    static final String PRODUCTION = "Production";
    static final String UAT = "UAT (testing)";

    private EnvironmentNames() {
    }

    static String name(HarmonyEnvironment environment) {
        return environment == HarmonyEnvironment.UAT ? UAT : PRODUCTION;
    }
}
