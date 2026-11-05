package gov.nasa.gsfc.seadas.earthdatacloud.matchup;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GranuleLinksTest {

    private static final URI CMR = URI.create("https://cmr.earthdata.nasa.gov");
    private static final String GRANULE = "TERRA_MODIS.20101010T180501.L2.OC.nc";
    private static final String ARCHIVE = "https://obdaac-tea.earthdatacloud.nasa.gov/ob-cumulus-prod-public/" + GRANULE;

    /** A CMR granules.json answer with the links CMR gives an OB_CLOUD granule. */
    private static String answer(String... hrefs) {
        StringBuilder links = new StringBuilder();
        for (String href : hrefs) {
            if (!links.isEmpty()) {
                links.append(',');
            }
            links.append("{\"rel\":\"http://esipfed.org/ns/fedsearch/1.1/data#\",\"href\":\"").append(href).append("\"}");
        }
        return "{\"feed\":{\"entry\":[{\"title\":\"" + GRANULE + "\",\"links\":["
                + "{\"rel\":\"http://esipfed.org/ns/fedsearch/1.1/browse#\",\"href\":\"https://x.earthdatacloud.nasa.gov/a.png\"},"
                + links + "]}]}}";
    }

    private static HttpTransport answering(int status, String body, List<URI> asked) {
        return (method, uri, headers, requestBody) -> {
            asked.add(uri);
            return new HttpTransport.Response(status, Map.of(),
                    new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
        };
    }

    @Test
    void findsTheArchiveLinkBySearchingShortNameAndGranuleName() throws Exception {
        List<URI> asked = new ArrayList<>();
        String body = answer("https://search.earthdata.nasa.gov/search/granules?p=C1-OB_CLOUD", ARCHIVE,
                "https://oceandata.sci.gsfc.nasa.gov/directdataaccess/Level-2/Terra-MODIS/");
        assertEquals(ARCHIVE, GranuleLinks.find(answering(200, body, asked), CMR, "MODIST_L2_OC", GRANULE));
        assertEquals("https://cmr.earthdata.nasa.gov/search/granules.json?short_name=MODIST_L2_OC"
                + "&readable_granule_name=" + GRANULE + "&page_size=10", asked.get(0).toString());
    }

    @Test
    void noGranuleOrNoArchiveLinkGivesNull() throws Exception {
        assertNull(GranuleLinks.find(answering(200, "{\"feed\":{\"entry\":[]}}", new ArrayList<>()), CMR,
                "MODIST_L2_OC", GRANULE));
        // A link to another file, or not on an Earthdata Cloud host, isn't taken.
        assertNull(GranuleLinks.link(answer("https://obdaac-tea.earthdatacloud.nasa.gov/p/OTHER.nc",
                "http://obdaac-tea.earthdatacloud.nasa.gov/p/" + GRANULE,
                "https://example.com/p/" + GRANULE), GRANULE));
    }

    @Test
    void aFailedSearchIsAnError() {
        assertThrows(IOException.class, () -> GranuleLinks.find(answering(503, "busy", new ArrayList<>()), CMR,
                "MODIST_L2_OC", GRANULE));
    }

    @Test
    void namesThatAreNotFileNamesAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> GranuleLinks.find(answering(200, "{}", new ArrayList<>()),
                CMR, "MODIST_L2_OC", "x.nc&provider=OTHER"));
    }
}
