package gov.nasa.gsfc.seadas.earthdatacloud.matchup;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The Harmony request for one observation against one collection: a small box
 * and a time range centred on it, answered by the service as CSV.
 *
 * <p>The service takes the box's midpoint as the observation, so the box only
 * has to find the granules; it is clamped at the poles and at the antimeridian
 * (an observation within the half-width of ±180° gets a box that stops there).
 * The service applies its own 3 h rule, so the window is capped at 3 h.
 *
 * @param collectionId CMR concept ID of the Level-2 collection, e.g. {@code C1281448693-OB_CLOUD}
 * @param lat          observation latitude, degrees north
 * @param lon          observation longitude, degrees east, -180 to 180
 * @param time         observation time
 * @param halfBoxDeg   half-width of the search box, degrees
 * @param halfWindow   half-width of the time range, at most 3 h
 * @param variables    variables to report, or {@code all}
 * @param label        Harmony job label, so a user can find the run's jobs; may be null
 */
public record MatchupRequest(String collectionId, double lat, double lon, Instant time, double halfBoxDeg,
                             Duration halfWindow, List<String> variables, String label) {

    public static final double DEFAULT_HALF_BOX_DEG = 0.1;
    public static final Duration DEFAULT_HALF_WINDOW = Duration.ofHours(3);
    public static final Duration MAX_HALF_WINDOW = Duration.ofHours(3);

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_INSTANT;

    public MatchupRequest {
        Objects.requireNonNull(collectionId, "collectionId");
        Objects.requireNonNull(time, "time");
        Objects.requireNonNull(halfWindow, "halfWindow");
        if (!collectionId.matches("C\\d+-[A-Z0-9_]+")) {
            throw new IllegalArgumentException("not a CMR collection concept ID: " + collectionId);
        }
        if (!(Math.abs(lat) <= 90.0)) {
            throw new IllegalArgumentException("latitude out of range: " + lat);
        }
        if (!(Math.abs(lon) <= 180.0)) {
            throw new IllegalArgumentException("longitude out of range (-180 to 180): " + lon);
        }
        if (!(halfBoxDeg > 0.0 && halfBoxDeg <= 5.0)) {
            throw new IllegalArgumentException("box half-width must be above 0 and at most 5 degrees: " + halfBoxDeg);
        }
        if (halfWindow.isNegative() || halfWindow.isZero() || halfWindow.compareTo(MAX_HALF_WINDOW) > 0) {
            throw new IllegalArgumentException("time window must be above 0 and at most 3 h: " + halfWindow);
        }
        variables = variables == null || variables.isEmpty() ? List.of("all") : List.copyOf(variables);
        for (String variable : variables) {
            if (variable.isBlank() || variable.contains(",") || variable.contains("/")) {
                throw new IllegalArgumentException("not a variable name: " + variable);
            }
        }
    }

    /** All variables, the default box and window, no label. */
    public static MatchupRequest of(String collectionId, double lat, double lon, Instant time) {
        return new MatchupRequest(collectionId, lat, lon, time, DEFAULT_HALF_BOX_DEG, DEFAULT_HALF_WINDOW,
                List.of("all"), null);
    }

    public MatchupRequest withLabel(String newLabel) {
        return new MatchupRequest(collectionId, lat, lon, time, halfBoxDeg, halfWindow, variables, newLabel);
    }

    public MatchupRequest withVariables(List<String> newVariables) {
        return new MatchupRequest(collectionId, lat, lon, time, halfBoxDeg, halfWindow, newVariables, label);
    }

    /** The request's URL under a Harmony root, as harmony-py builds it (OGC Coverages, async). */
    public URI uri(URI harmony) {
        double south = Math.max(-90.0, lat - halfBoxDeg);
        double north = Math.min(90.0, lat + halfBoxDeg);
        double west = Math.max(-180.0, lon - halfBoxDeg);
        double east = Math.min(180.0, lon + halfBoxDeg);
        Instant start = time.minus(halfWindow).truncatedTo(ChronoUnit.SECONDS);
        Instant stop = time.plus(halfWindow).truncatedTo(ChronoUnit.SECONDS);

        List<String> query = new ArrayList<>();
        query.add("subset=" + encode("lat(" + number(south) + ":" + number(north) + ")"));
        query.add("subset=" + encode("lon(" + number(west) + ":" + number(east) + ")"));
        query.add("subset=" + encode("time(\"" + ISO.format(start) + "\":\"" + ISO.format(stop) + "\")"));
        query.add("format=" + encode("text/csv"));
        query.add("forceAsync=true");
        if (label != null && !label.isBlank()) {
            query.add("label=" + encode(label));
        }
        String path = "/" + collectionId + "/ogc-api-coverages/1.0.0/collections/"
                + encode(String.join(",", variables)) + "/coverage/rangeset";
        return URI.create(harmony + path + "?" + String.join("&", query));
    }

    private static String number(double value) {
        String text = String.format(java.util.Locale.ROOT, "%.6f", value);
        text = text.replaceAll("0+$", "");
        return text.endsWith(".") ? text + "0" : text;
    }

    private static String encode(String text) {
        return URLEncoder.encode(text, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
