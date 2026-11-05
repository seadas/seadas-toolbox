package gov.nasa.gsfc.seadas.earthdatacloud.matchup;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Finds a result granule's file in the Earthdata Archive, so it can be opened
 * remotely: a CMR granule search by product short name and granule file name.
 *
 * <p>Searches production CMR whatever environment the run used: UAT holds
 * copies of production granules, under the same names, and the archive links
 * UAT gives need a UAT login that SeaDAS's remote reader doesn't have.
 */
public final class GranuleLinks {

    /** Granule and short names are plain file-name characters. */
    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9._-]+");

    private static final String USER_AGENT = "SeaDAS-satellite-in-situ-matchup Java/"
            + System.getProperty("java.version");

    private GranuleLinks() {
    }

    /**
     * The granule's HTTPS link in the Earthdata Archive.
     *
     * @param cmr       the CMR to search, without a trailing slash
     * @param shortName the product, e.g. {@code MODIST_L2_OC}
     * @param granule   the file name, e.g. {@code TERRA_MODIS.20101010T180501.L2.OC.nc}
     * @return the link, or null when CMR has no such granule or no archive link for it
     */
    public static String find(HttpTransport http, URI cmr, String shortName, String granule)
            throws IOException, InterruptedException {
        if (!NAME.matcher(shortName).matches() || !NAME.matcher(granule).matches()) {
            throw new IllegalArgumentException("not a product or granule name: " + shortName + " " + granule);
        }
        URI search = URI.create(cmr + "/search/granules.json?short_name=" + encode(shortName)
                + "&readable_granule_name=" + encode(granule) + "&page_size=10");
        try (HttpTransport.Response response = http.send("GET", search,
                Map.of("Accept", "application/json", "User-Agent", USER_AGENT), null)) {
            String text = response.text();
            if (response.status() != 200) {
                throw new IOException("CMR granule search answered " + response.status());
            }
            return link(text, granule);
        } catch (JSONException e) {
            throw new IOException("CMR granule search: unexpected answer: " + e.getMessage(), e);
        }
    }

    /** From a CMR granules.json answer: the data link to the file on an Earthdata Cloud host. */
    static String link(String json, String granule) {
        JSONArray entries = new JSONObject(json).getJSONObject("feed").optJSONArray("entry");
        if (entries == null) {
            return null;
        }
        for (int i = 0; i < entries.length(); i++) {
            JSONArray links = entries.getJSONObject(i).optJSONArray("links");
            for (int j = 0; links != null && j < links.length(); j++) {
                JSONObject link = links.getJSONObject(j);
                String href = link.optString("href", "");
                if (link.optString("rel", "").endsWith("/data#") && isArchiveFile(href, granule)) {
                    return href;
                }
            }
        }
        return null;
    }

    private static boolean isArchiveFile(String href, String granule) {
        URI uri;
        try {
            uri = URI.create(href);
        } catch (IllegalArgumentException e) {
            return false;
        }
        String host = uri.getHost();
        return "https".equalsIgnoreCase(uri.getScheme()) && host != null
                && host.toLowerCase(Locale.ROOT).endsWith(".earthdatacloud.nasa.gov")
                && uri.getPath() != null && uri.getPath().endsWith("/" + granule);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
