package gov.nasa.gsfc.seadas.processing.earthdata;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The user's NASA Earthdata Login, kept in ~/.netrc where SeaDAS, OCSSW and the Docker setup read it.
 */
public final class EarthdataLogin {

    public static final String HOST = "urs.earthdata.nasa.gov";
    public static final String REGISTER_URL = "https://" + HOST + "/users/new";
    public static final String RESET_PASSWORD_URL = "https://" + HOST + "/reset_passwords/new";

    /** Lists the user's tokens; answers 200 for a good login and 401 for a bad one, and changes nothing. */
    private static final String CHECK_URL = "https://" + HOST + "/api/users/tokens";
    private static final int TIMEOUT_MILLIS = 20_000;

    private static final Logger LOG = Logger.getLogger(EarthdataLogin.class.getName());

    public enum Check {OK, REJECTED, UNREACHABLE}

    private EarthdataLogin() {
    }

    /** True if ~/.netrc has a user name and password for Earthdata. Doesn't check them. */
    public static boolean isSaved() {
        return getSavedUserName() != null;
    }

    /** The user name in ~/.netrc, or null. */
    public static String getSavedUserName() {
        try {
            String[] saved = NetrcFile.inHome().read(HOST);
            return saved == null ? null : saved[0];
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Can't read " + NetrcFile.inHome().getPath(), e);
            return null;
        }
    }

    /** Asks Earthdata whether the login is good. Blocks for up to about 20 seconds; don't call on the UI thread. */
    public static Check check(String userName, String password) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(CHECK_URL).openConnection();
            connection.setConnectTimeout(TIMEOUT_MILLIS);
            connection.setReadTimeout(TIMEOUT_MILLIS);
            connection.setInstanceFollowRedirects(false);
            String auth = Base64.getEncoder().encodeToString((userName + ":" + password).getBytes(StandardCharsets.UTF_8));
            connection.setRequestProperty("Authorization", "Basic " + auth);
            int status = connection.getResponseCode();
            if (status == HttpURLConnection.HTTP_OK) {
                return Check.OK;
            }
            if (status == HttpURLConnection.HTTP_UNAUTHORIZED || status == HttpURLConnection.HTTP_FORBIDDEN) {
                return Check.REJECTED;
            }
            LOG.warning("Earthdata login check: unexpected HTTP " + status + " from " + CHECK_URL);
            return Check.UNREACHABLE;
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Earthdata login check: can't reach " + CHECK_URL, e);
            return Check.UNREACHABLE;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /** Saves the login in ~/.netrc. */
    public static void save(String userName, String password) throws IOException {
        NetrcFile.inHome().write(HOST, userName, password);
        LOG.info("Saved the Earthdata login for " + userName + " in " + NetrcFile.inHome().getPath());
    }
}
