package gov.nasa.gsfc.seadas.earthdatacloud.matchup;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Joins the service's rows to the SeaBASS file they came from, writing the
 * columns {@code mk_matchup.py} (seabass-utils 0.0.6) appends:
 * {@code <prefix>_l2fname}, {@code _tdiff}, {@code _rrsaot_cv} (ocean color),
 * {@code _<var>} and {@code _<var>_sd} per variable ({@code _qual_sst_mean} and
 * {@code _max} for SST), {@code _<var>_<wl>}, {@code _sd} and {@code _unc} per
 * PACE OCI band, then {@code matchup_found} ({@code yes}/{@code no}). Values are
 * copied as the service wrote them, which is the text the script writes.
 *
 * <p><b>One satellite row per observation.</b> An observation can have rows
 * from several granules. The joiner writes the best: a matched granule first,
 * then the smallest |tdiff|, then the granule name (design decision D2, pending
 * the science team; the script keeps whichever granule it processed last). A
 * rejected granule's values are written with {@code matchup_found=no}, as the
 * script does; an observation in no granule gets {@code no} and missing values.
 *
 * <p><b>Rows.</b> {@link Rows#ALL} keeps every row of the file, as the script's
 * {@code --add_failed_matchups}; {@link Rows#MATCHED} keeps only the rows with a
 * matchup, as the script does by default.
 *
 * <p><b>Several products.</b> Each gets its own columns. {@code matchup_found}
 * is then per product ({@code <prefix>_matchup_found}), since one column can't
 * say which matched. Two products with the same prefix (e.g. MODIS Aqua ocean
 * color and SST) can't share a file: their columns would collide.
 *
 * <p>Every service row echoes the observation it answers ({@code obs_lat},
 * {@code obs_lon}, {@code obs_datetime}); a row that doesn't match its
 * observation is left out with a warning.
 */
public final class ResultJoiner {

    /** Which SeaBASS rows the output keeps. */
    public enum Rows {
        /** Every row, with {@code matchup_found} (the script's {@code --add_failed_matchups}). */
        ALL,
        /** Only rows with a matched granule (the script's default). */
        MATCHED
    }

    /**
     * One product's service results.
     *
     * @param csvs by SeaBASS data row (0-based): the CSVs of the job for that observation
     */
    public record ProductRows(MatchupProduct product, Map<Integer, List<MatchupCsv>> csvs) {
    }

    /**
     * About the run, for the header comments.
     *
     * @param when    when the run finished
     * @param harmony the Harmony the jobs ran in
     * @param extra   more comment lines (without the {@code !}), e.g. where the run record is kept
     */
    public record RunInfo(Instant when, URI harmony, List<String> extra) {
    }

    /**
     * The result.
     *
     * @param writer   the SeaBASS output, ready to write
     * @param chosen   per product (collection ID), the service row written for each SeaBASS data row
     * @param warnings rows left out, and why
     */
    public record Joined(SeabassWriter writer, Map<String, Map<Integer, Map<String, String>>> chosen,
                         List<String> warnings) {
    }

    static final String MATCHED_STATUS = "matched";
    static final String REJECTED_STATUS = "rejected";
    private static final double POSITION_TOLERANCE_DEG = 1e-5;
    private static final Duration TIME_TOLERANCE = Duration.ofSeconds(1);

    private ResultJoiner() {
    }

    /** The row to write from an observation's rows: matched first, then the smallest |tdiff|; null if none matched or was rejected. */
    public static Map<String, String> best(List<Map<String, String>> rows) {
        Comparator<Map<String, String>> order = Comparator
                .comparing((Map<String, String> row) -> !MATCHED_STATUS.equals(row.get("matchup_status")))
                .thenComparingDouble(row -> Math.abs(number(row.get("tdiff"))))
                .thenComparing(row -> row.getOrDefault("granule", ""));
        return rows.stream()
                .filter(row -> MATCHED_STATUS.equals(row.get("matchup_status"))
                        || REJECTED_STATUS.equals(row.get("matchup_status")))
                .min(order).orElse(null);
    }

    public static Joined join(SeabassFile file, List<SeabassObservation> observations, List<ProductRows> products,
                              Rows keep, RunInfo run) {
        Map<Integer, SeabassObservation> byRow = new LinkedHashMap<>();
        for (SeabassObservation observation : observations) {
            byRow.put(observation.row(), observation);
        }
        List<String> warnings = new ArrayList<>();
        SeabassWriter writer = new SeabassWriter(file);
        Map<String, Map<Integer, Map<String, String>>> chosen = new LinkedHashMap<>();
        Set<String> prefixes = new HashSet<>();
        Set<Integer> matchedRows = new LinkedHashSet<>();
        boolean sst = false;

        for (ProductRows product : products) {
            String collection = product.product().collectionId();
            Map<Integer, List<Map<String, String>>> rows = new LinkedHashMap<>();
            Set<String> header = new LinkedHashSet<>();
            String prefix = null;
            for (Map.Entry<Integer, List<MatchupCsv>> entry : product.csvs().entrySet()) {
                int row = entry.getKey();
                SeabassObservation observation = byRow.get(row);
                for (MatchupCsv csv : entry.getValue()) {
                    header.addAll(csv.header());
                    for (Map<String, String> serviceRow : csv.rows()) {
                        String problem = observation == null ? "no such observation"
                                : mismatch(observation, serviceRow);
                        String rowPrefix = null;
                        if (problem == null) {
                            try {
                                rowPrefix = MatchupProduct.prefix(serviceRow.getOrDefault("granule", ""));
                            } catch (IllegalArgumentException e) {
                                problem = e.getMessage();
                            }
                        }
                        if (problem == null && prefix != null && !prefix.equals(rowPrefix)) {
                            problem = "granule " + serviceRow.get("granule") + " is not from the same mission as "
                                    + prefix;
                        }
                        if (problem != null) {
                            warnings.add(collection + ", line " + line(file, row) + ": row for "
                                    + serviceRow.get("granule") + " left out: " + problem);
                            continue;
                        }
                        prefix = rowPrefix;
                        rows.computeIfAbsent(row, k -> new ArrayList<>()).add(serviceRow);
                    }
                }
            }

            Map<Integer, Map<String, String>> best = new LinkedHashMap<>();
            rows.forEach((row, candidates) -> {
                Map<String, String> pick = best(candidates);
                if (pick != null) {
                    best.put(row, pick);
                }
            });
            chosen.put(collection, Collections.unmodifiableMap(best));
            best.forEach((row, pick) -> {
                if (MATCHED_STATUS.equals(pick.get("matchup_status"))) {
                    matchedRows.add(row);
                }
            });

            if (prefix != null && !best.isEmpty()) {
                if (!prefixes.add(prefix)) {
                    throw new IllegalArgumentException("Two products with the column prefix " + prefix
                            + " can't be written to one SeaBASS file; write them to separate files");
                }
                for (Column column : columns(new ArrayList<>(header), prefix, product.product())) {
                    if (column.optional() && best.values().stream().allMatch(r -> blank(r.get(column.source())))) {
                        continue; // as the script, which writes rrsaot_cv only when there were CV bands
                    }
                    sst |= column.name().contains("qual_sst");
                    for (Map.Entry<Integer, Map<String, String>> pick : best.entrySet()) {
                        writer.set(pick.getKey(), column.name(), column.units(), pick.getValue().get(column.source()));
                    }
                }
            }
            String found = products.size() == 1 ? "matchup_found"
                    : (prefix != null ? prefix : collection.toLowerCase()) + "_matchup_found";
            for (int row = 0; row < file.rowCount(); row++) {
                Map<String, String> pick = best.get(row);
                boolean yes = pick != null && MATCHED_STATUS.equals(pick.get("matchup_status"));
                writer.set(row, found, "none", yes ? "yes" : "no");
            }
        }

        if (keep == Rows.MATCHED) {
            writer.retainRows(matchedRows);
        }
        for (String line : comments(products, run, sst)) {
            writer.addComment(line);
        }
        return new Joined(writer, Collections.unmodifiableMap(chosen), List.copyOf(warnings));
    }

    // ------------------------------------------------------------ columns

    /** An output column: its name (before lower-casing), units, and the service column it comes from. */
    record Column(String name, String units, String source, boolean optional) {
    }

    /** The output columns for a product, in the script's order, from the service's header. */
    static List<Column> columns(List<String> header, String prefix, MatchupProduct product) {
        List<Column> columns = new ArrayList<>();
        columns.add(new Column(prefix + "_l2fname", "none", "granule", false));
        columns.add(new Column(prefix + "_tdiff", "seconds", "tdiff", false));
        columns.add(new Column(prefix + "_rrsaot_cv", "unitless", "rrsaot_cv", true));
        Set<String> all = new HashSet<>(header);
        for (String source : header) {
            if (!source.startsWith("sat_") || source.endsWith("_valid_pixel_count")) {
                continue;
            }
            String rest = source.substring(4);
            int cut = rest.lastIndexOf('_');
            if (cut < 0) {
                continue;
            }
            String base = rest.substring(0, cut);
            String suffix = rest.substring(cut + 1);
            String name = prefix + "_" + base;
            if (base.contains("qual_sst")) {
                if (suffix.equals("mean") || suffix.equals("max")) {
                    columns.add(new Column(name + "_" + suffix, "none", source, false));
                }
            } else if (all.contains("sat_" + base + "_unc")) {
                String cube = base.replaceFirst("_[0-9.]+$", "");
                switch (suffix) {
                    case "mean" -> columns.add(new Column(name, product.unitsOf(cube), source, false));
                    case "sd" -> columns.add(new Column(name + "_sd", product.unitsOf(cube), source, false));
                    case "unc" -> columns.add(new Column(name + "_unc", product.unitsOf(cube + "_unc"), source, false));
                    default -> { }
                }
            } else if (suffix.equals("mean")) {
                columns.add(new Column(name, product.unitsOf(base), source, false));
            } else if (suffix.equals("sd")) {
                columns.add(new Column(name + "_sd", product.unitsOf(base), source, false));
            }
        }
        return columns;
    }

    // ------------------------------------------------------------ checks

    /** Why a service row doesn't answer this observation, or null when it does. */
    static String mismatch(SeabassObservation observation, Map<String, String> row) {
        if (!observation.usable()) {
            return "the observation can't be matched (" + observation.problem() + ")";
        }
        double lat = number(row.get("obs_lat"));
        double lon = number(row.get("obs_lon"));
        if (!(Math.abs(lat - observation.lat()) <= POSITION_TOLERANCE_DEG
                && Math.abs(lon - observation.lon()) <= POSITION_TOLERANCE_DEG)) {
            return "it is for " + row.get("obs_lat") + ", " + row.get("obs_lon") + ", not " + observation.lat()
                    + ", " + observation.lon();
        }
        Instant expected = observation.time().toInstant(ZoneOffset.UTC);
        try {
            Instant echoed = Instant.parse(row.getOrDefault("obs_datetime", ""));
            if (Duration.between(expected, echoed).abs().compareTo(TIME_TOLERANCE) > 0) {
                return "it is for " + echoed + ", not " + expected;
            }
        } catch (DateTimeParseException e) {
            return "its obs_datetime can't be read: " + row.get("obs_datetime");
        }
        return null;
    }

    private static int line(SeabassFile file, int row) {
        return row >= 0 && row < file.rowCount() ? file.lineNumber(row) : -1;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static double number(String text) {
        try {
            return text == null || text.isBlank() ? Double.POSITIVE_INFINITY : Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return Double.POSITIVE_INFINITY;
        }
    }

    // ------------------------------------------------------------ header comments

    static List<String> comments(List<ProductRows> products, RunInfo run, boolean sst) {
        List<String> lines = new ArrayList<>();
        lines.add(" ");
        lines.add(" File amended by the SeaDAS satellite-in-situ matchup tool on "
                + DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.ofInstant(run.when(),
                ZoneOffset.UTC)) + " UTC.");
        lines.add(" Satellite values from the satellite-in-situ-matchup service in NASA Harmony ("
                + run.harmony() + "),");
        lines.add(" which runs the per-observation matchup of seabass-utils mk_matchup.py 0.0.6.");
        for (ProductRows product : products) {
            lines.add(" Product: " + product.product().shortName() + " (" + product.product().collectionId() + ")");
        }
        lines.add(" One satellite granule per observation: a matched granule first, then the smallest");
        lines.add("     time difference. A rejected granule's values are kept, with matchup_found=no.");
        lines.add(" WARNINGS: in situ data are NOT adjusted to water-leaving values, and potential");
        lines.add("     oversampling by the in situ data in time or space is NOT accounted for.");
        lines.add(" EXCLUSION CRITERIA (fixed in the service), after S.W. Bailey and P.J. Werdell, \"A multi-sensor");
        lines.add(" approach for the on-orbit validation of ocean color satellite data products\",");
        lines.add(" Rem. Sens. Environ. 102, 12-23 (2006):");
        lines.add("     Box size of satellite extract = 5 pixels by 5 pixels");
        lines.add("     Minimum percent valid satellite pixels = 50");
        lines.add("     Maximum time difference between satellite and in situ = 3 hours");
        lines.add("     Maximum coefficient of variation of satellite pixels = 0.15 (ocean color products)");
        lines.add(" NOTE: the coefficient of variation uses Rrs between 405 nm and 570 nm and AOT between");
        lines.add("     860 nm and 900 nm, except the MODIS land bands at 469 nm and 555 nm.");
        if (sst) {
            lines.add(" For SST products, screen sst with qual_sst_mean or qual_sst_max: 0 (best) to 4 (worst),");
            lines.add("     over the 5 by 5 pixel extract.");
        }
        lines.addAll(run.extra() == null ? List.of() : run.extra());
        lines.add(" ");
        return lines;
    }
}
