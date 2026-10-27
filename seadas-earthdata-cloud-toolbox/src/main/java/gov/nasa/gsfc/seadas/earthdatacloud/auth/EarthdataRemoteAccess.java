package gov.nasa.gsfc.seadas.earthdatacloud.auth;

import org.esa.snap.dataio.netcdf.remote.RemoteAuthProvider;
import org.esa.snap.dataio.netcdf.remote.RemoteFileAccess;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;

/**
 * Lets SNAP's NetCDF reader open Earthdata Cloud files over HTTPS without downloading them, by supplying the
 * Earthdata Login bearer token (from {@code ~/.netrc}, via {@link WebPageFetcherWithJWT}) to NASA hosts.
 * <p>
 * The token is sent only to {@code *.earthdatacloud.nasa.gov} (DAAC archive hosts) and {@code *.earthdata.nasa.gov}
 * (Earthdata Login). The archive answers with a redirect to a signed CloudFront URL, which receives no token.
 */
public final class EarthdataRemoteAccess implements RemoteAuthProvider {

    static final String EARTHDATA_LOGIN_HOST = "urs.earthdata.nasa.gov";

    private static final EarthdataRemoteAccess INSTANCE = new EarthdataRemoteAccess();

    private volatile String token;

    private EarthdataRemoteAccess() {
    }

    /**
     * Registers the Earthdata Login provider with SNAP's remote file access. Safe to call repeatedly.
     */
    public static void ensureRegistered() {
        RemoteFileAccess.registerAuthProvider(INSTANCE);
    }

    @Override
    public boolean handles(URI uri) {
        String host = uri.getHost();
        if (host == null || !"https".equalsIgnoreCase(uri.getScheme())) {
            return false;
        }
        host = host.toLowerCase(Locale.ROOT);
        return host.endsWith(".earthdatacloud.nasa.gov") || host.endsWith(".earthdata.nasa.gov");
    }

    @Override
    public String getAuthorizationHeader(URI uri) throws IOException {
        String t = token;
        if (t == null) {
            synchronized (this) {
                t = token;
                if (t == null) {
                    try {
                        t = WebPageFetcherWithJWT.getAccessToken(EARTHDATA_LOGIN_HOST);
                    } catch (RuntimeException e) {
                        throw new IOException("Could not get an Earthdata Login token. Check the '"
                                + EARTHDATA_LOGIN_HOST + "' entry in your .netrc file.", e);
                    }
                    token = t;
                }
            }
        }
        return "Bearer " + t;
    }

    @Override
    public void invalidate(URI uri) {
        token = null;
    }
}
