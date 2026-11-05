package gov.nasa.gsfc.seadas.earthdatacloud.matchup.ui;

import gov.nasa.gsfc.seadas.earthdatacloud.auth.WebPageFetcherWithJWT;
import gov.nasa.gsfc.seadas.earthdatacloud.matchup.HarmonyEnvironment;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

/** Earthdata Login tokens for Harmony and CMR; asked for off the event thread. */
interface TokenSource {

    /** A token for the environment, fetched once and kept. */
    String token(HarmonyEnvironment environment) throws IOException;

    /** Drops a kept token, after Harmony or CMR refused it. */
    void forget(HarmonyEnvironment environment);

    /**
     * Tokens from Earthdata Login, with the {@code ~/.netrc} login for the
     * environment's host, as the other Earthdata Cloud tools get them.
     */
    static TokenSource netrc() {
        return new TokenSource() {
            private final Map<HarmonyEnvironment, String> kept = new EnumMap<>(HarmonyEnvironment.class);

            @Override
            public synchronized String token(HarmonyEnvironment environment) throws IOException {
                String token = kept.get(environment);
                if (token == null) {
                    try {
                        token = WebPageFetcherWithJWT.getAccessToken(environment.loginHost());
                    } catch (RuntimeException e) {
                        throw new IOException("Couldn't get an Earthdata Login token from " + environment.loginHost()
                                + " with the login in ~/.netrc (machine " + environment.loginHost() + "): "
                                + rootMessage(e), e);
                    }
                    if (token == null || token.isBlank() || token.equals("empty")) {
                        throw new IOException("Earthdata Login gave no token for the login in ~/.netrc (machine "
                                + environment.loginHost() + ")");
                    }
                    kept.put(environment, token);
                }
                return token;
            }

            @Override
            public synchronized void forget(HarmonyEnvironment environment) {
                kept.remove(environment);
            }
        };
    }

    /** A fixed token, for trying the window outside SeaDAS. */
    static TokenSource fixed(String token) {
        return new TokenSource() {
            @Override
            public String token(HarmonyEnvironment environment) throws IOException {
                if (token == null || token.isBlank()) {
                    throw new IOException("No token");
                }
                return token;
            }

            @Override
            public void forget(HarmonyEnvironment environment) {
            }
        };
    }

    private static String rootMessage(Throwable e) {
        while (e.getCause() != null) {
            e = e.getCause();
        }
        return e.getMessage() != null ? e.getMessage() : e.toString();
    }
}
