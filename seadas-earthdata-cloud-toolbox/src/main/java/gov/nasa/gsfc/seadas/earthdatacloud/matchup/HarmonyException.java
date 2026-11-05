package gov.nasa.gsfc.seadas.earthdatacloud.matchup;

import java.io.IOException;

/**
 * Harmony refused a call. The message is Harmony's own description when it gave
 * one (e.g. "satellite-in-situ-matchup requires spatialSubset"), so it can be
 * shown to the user as it is.
 */
public class HarmonyException extends IOException {

    private final int httpStatus;
    private final String code;

    public HarmonyException(int httpStatus, String code, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.code = code;
    }

    /** The HTTP status, e.g. 422; 0 when there was none. */
    public int httpStatus() {
        return httpStatus;
    }

    /** Harmony's error code, e.g. {@code harmony.UnsupportedOperation}, or null. */
    public String code() {
        return code;
    }

    /** Whether the user's Earthdata Login token is missing, expired or not accepted. */
    public boolean unauthorized() {
        return httpStatus == 401 || httpStatus == 403 || httpStatus == 303;
    }
}
