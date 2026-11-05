package gov.nasa.gsfc.seadas.earthdatacloud.matchup;

import java.net.URI;

/** A Harmony deployment and the Earthdata Login that issues its tokens. */
public enum HarmonyEnvironment {
    PRODUCTION("https://harmony.earthdata.nasa.gov", "urs.earthdata.nasa.gov"),
    /** For testing; collections and the service are the UAT ones. */
    UAT("https://harmony.uat.earthdata.nasa.gov", "uat.urs.earthdata.nasa.gov");

    private final URI harmony;
    private final String loginHost;

    HarmonyEnvironment(String harmony, String loginHost) {
        this.harmony = URI.create(harmony);
        this.loginHost = loginHost;
    }

    /** Harmony's root URL, without a trailing slash. */
    public URI harmony() {
        return harmony;
    }

    /** The Earthdata Login host whose token Harmony accepts (the {@code .netrc} machine). */
    public String loginHost() {
        return loginHost;
    }

    /** The job's page in Harmony's workflow UI, for users to follow or report. */
    public URI workflowUi(String jobId) {
        return URI.create(harmony + "/workflow-ui/" + jobId);
    }
}
