package gov.nasa.gsfc.seadas.earthdatacloud.matchup;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoverageCheckerTest {

    private static final URI CMR = URI.create("https://cmr.test");

    /** A CMR that answers each search with what {@code reply} says. */
    static final class FakeCmr implements HttpTransport {
        final List<URI> searches = Collections.synchronizedList(new ArrayList<>());
        final List<Map<String, String>> headers = Collections.synchronizedList(new ArrayList<>());
        final Function<URI, Response> reply;

        FakeCmr(Function<URI, Response> reply) {
            this.reply = reply;
        }

        @Override
        public Response send(String method, URI uri, Map<String, String> h, String body) {
            searches.add(uri);
            headers.add(h);
            return reply.apply(uri);
        }

        static Response hits(int hits) {
            return new Response(200, Map.of("cmr-hits", String.valueOf(hits)),
                    new ByteArrayInputStream("{\"feed\":{\"entry\":[]}}".getBytes(StandardCharsets.UTF_8)));
        }

        static Response error(int status, String body) {
            return FakeHarmony.reply(status, body);
        }
    }

    private static String query(URI uri) {
        return URLDecoder.decode(uri.getRawQuery(), StandardCharsets.UTF_8);
    }

    @Test
    void theSearchIsHarmonysBoxAndRangeAskingOnlyForTheCount() {
        MatchupRequest baltic = MatchupRequest.of("C1281448693-OB_CLOUD", 55.914, 19.015,
                Instant.parse("2026-06-03T11:35:00Z"));
        URI uri = baltic.granuleSearch(HarmonyEnvironment.UAT.cmr());
        assertEquals("https://cmr.uat.earthdata.nasa.gov/search/granules.json", uri.toString().split("\\?")[0]);
        assertEquals("collection_concept_id=C1281448693-OB_CLOUD&bounding_box=18.915,55.814,19.115,56.014"
                + "&temporal=2026-06-03T08:35:00Z,2026-06-03T14:35:00Z&page_size=0", query(uri));

        // The same shrunken box as the Harmony request near 180 degrees.
        MatchupRequest edge = new MatchupRequest("C1-OB_CLOUD", 10, 179.95, Instant.EPOCH, 0.1,
                Duration.ofHours(1), List.of(), null);
        assertTrue(query(edge.granuleSearch(CMR)).contains("bounding_box=179.9,9.9,180.0,10.1"),
                query(edge.granuleSearch(CMR)));
        assertTrue(URLDecoder.decode(edge.uri(CMR).getRawQuery(), StandardCharsets.UTF_8)
                .contains("lon(179.9:180.0)"));
    }

    @Test
    void countsEachRequestsGranulesWithTheToken() throws Exception {
        FakeCmr cmr = new FakeCmr(uri -> FakeCmr.hits(query(uri).contains("bounding_box=-0.1") ? 0 : 2));
        CoverageChecker checker = new CoverageChecker(cmr, CMR, () -> "TOKEN", d -> {
        }, 3);
        List<MatchupRequest> requests = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            requests.add(MatchupRequest.of("C1-OB_CLOUD", 10, i == 4 ? 0 : 20, Instant.EPOCH));
        }
        AtomicInteger last = new AtomicInteger();

        CoverageChecker.Result result = checker.check(requests, () -> false, (checked, total) -> {
            assertEquals(10, total);
            last.accumulateAndGet(checked, Math::max);
        });

        assertEquals(10, last.get());
        assertFalse(result.cancelled());
        assertTrue(result.errors().isEmpty());
        assertEquals(0, result.granules().get(4));
        assertEquals(18, result.granules().stream().mapToInt(Integer::intValue).sum());
        assertTrue(cmr.headers.stream().allMatch(h -> "Bearer TOKEN".equals(h.get("Authorization"))));
    }

    @Test
    void busyAnswersAreRetriedAndOtherErrorsLeaveTheCountUnknown() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        FakeCmr cmr = new FakeCmr(uri -> {
            if (query(uri).contains("bounding_box=29.9")) {
                return FakeCmr.error(400, "{\"errors\":[\"temporal is not valid\"]}");
            }
            return calls.incrementAndGet() <= 2 ? FakeCmr.error(503, "busy") : FakeCmr.hits(1);
        });
        List<Duration> sleeps = Collections.synchronizedList(new ArrayList<>());
        CoverageChecker checker = new CoverageChecker(cmr, CMR, () -> null, sleeps::add, 1);

        CoverageChecker.Result result = checker.check(List.of(
                MatchupRequest.of("C1-OB_CLOUD", 10, 20, Instant.EPOCH),
                MatchupRequest.of("C1-OB_CLOUD", 10, 30, Instant.EPOCH)), () -> false, (c, t) -> {
        });

        assertEquals(1, result.granules().get(0));
        assertNull(result.granules().get(1));
        assertEquals("CMR granule search: HTTP 400: temporal is not valid", result.errors().get(1));
        assertEquals(List.of(Duration.ofSeconds(2), Duration.ofSeconds(4)), sleeps);
        assertFalse(cmr.headers.get(0).containsKey("Authorization"), "no token, no header");
    }

    @Test
    void aRejectedTokenStopsTheCheck() {
        FakeCmr cmr = new FakeCmr(uri -> FakeCmr.error(401, "{\"errors\":[\"Token expired\"]}"));
        CoverageChecker checker = new CoverageChecker(cmr, CMR, () -> "OLD", d -> {
        }, 2);
        List<MatchupRequest> requests = Collections.nCopies(20, MatchupRequest.of("C1-OB_CLOUD", 10, 20,
                Instant.EPOCH));

        HarmonyException e = assertThrows(HarmonyException.class, () -> checker.check(requests, () -> false,
                (c, t) -> {
                }));
        assertTrue(e.unauthorized());
        assertTrue(cmr.searches.size() < 20, "stopped early: " + cmr.searches.size());
    }

    @Test
    void aCancelledCheckSaysSo() throws Exception {
        FakeCmr cmr = new FakeCmr(uri -> FakeCmr.hits(1));
        CoverageChecker checker = new CoverageChecker(cmr, CMR, () -> null, d -> {
        }, 1);
        AtomicInteger checked = new AtomicInteger();
        List<MatchupRequest> requests = Collections.nCopies(10, MatchupRequest.of("C1-OB_CLOUD", 10, 20,
                Instant.EPOCH));

        CoverageChecker.Result result = checker.check(requests, () -> checked.get() >= 3,
                (c, t) -> checked.set(c));

        assertTrue(result.cancelled());
        assertEquals(3, result.granules().stream().filter(g -> g != null).count());
        assertTrue(result.errors().isEmpty());
    }
}
