package gov.nasa.gsfc.seadas.earthdatacloud.matchup;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A run's results as the dialog shows them: every granule row the service
 * returned, and one outcome per observation and product with the row chosen
 * for the output (design D2), a reason in words, and counts.
 */
public final class MatchupResults {

    /** What happened to one observation against one product. */
    public enum Outcome {
        MATCHED("matched"),
        REJECTED("rejected"),
        NOT_IN_GRANULE("not in granule"),
        NO_OVERPASS("no overpass"),
        FAILED("failed"),
        /** Not sent: the observation has no usable time or position. */
        SKIPPED("not usable"),
        /** Not run yet, or stopped. */
        WAITING("not run yet");

        private final String words;

        Outcome(String words) {
            this.words = words;
        }

        public String words() {
            return words;
        }
    }

    /**
     * One row of a service CSV.
     *
     * @param chosen whether this is the row the output uses for its observation and product
     */
    public record GranuleRow(MatchupRun.Task task, SeabassObservation observation, Map<String, String> values,
                             boolean chosen) {
        public String status() {
            return values.getOrDefault("matchup_status", "");
        }
    }

    /**
     * One observation against one product.
     *
     * @param chosen the row the output uses, or null
     * @param reason why it isn't matched, in words; null when matched
     */
    public record Item(MatchupRun.Task task, SeabassObservation observation, Outcome outcome,
                       Map<String, String> chosen, String reason) {
    }

    /** Exclusion level to short words, as the dialog shows them (design §4). */
    static final Map<String, String> SHORT_REASONS = Map.of(
            "1", "time",
            "2", "cloud or flags",
            "3", "cloud or flags",
            "4", "cloud or flags",
            "5", "too variable");

    /** Exclusion level to the full reason, as the demo notebook prints it. */
    public static final Map<String, String> REASONS = Map.of(
            "1", "time difference above the limit",
            "2", "every pixel is flagged",
            "3", "fewer unflagged pixels than the box size",
            "4", "unflagged percentage below the threshold",
            "5", "coefficient of variation above the limit");

    private final List<GranuleRow> rows;
    private final List<Item> items;

    private MatchupResults(List<GranuleRow> rows, List<Item> items) {
        this.rows = List.copyOf(rows);
        this.items = List.copyOf(items);
    }

    /** Reads a run's downloaded results. */
    public static MatchupResults of(MatchupRun run) throws IOException {
        Map<Integer, SeabassObservation> observations = new LinkedHashMap<>();
        for (SeabassObservation observation : run.observations()) {
            observations.put(observation.row(), observation);
        }
        List<GranuleRow> rows = new ArrayList<>();
        List<Item> items = new ArrayList<>();
        for (MatchupRun.Task task : run.tasks()) {
            SeabassObservation observation = observations.get(task.row());
            List<Map<String, String>> values = new ArrayList<>();
            if (task.state() == MatchupRun.State.DONE) {
                for (String name : task.files()) {
                    if (name.endsWith(".csv")) {
                        values.addAll(MatchupCsv.read(run.resultsFolder().resolve(name)).rows());
                    }
                }
            }
            Map<String, String> best = ResultJoiner.best(values);
            for (Map<String, String> value : values) {
                rows.add(new GranuleRow(task, observation, value, value == best));
            }
            items.add(item(task, observation, values, best));
        }
        // A row at the same place and time as an earlier one shares that row's outcome.
        Map<String, Item> byKey = new java.util.HashMap<>();
        for (Item item : items) {
            byKey.put(item.task().collection() + "#" + item.task().row(), item);
        }
        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            Item original = byKey.get(item.task().collection() + "#" + item.task().sameAs());
            if (item.task().sameAs() >= 0 && original != null) {
                String same = "same as #" + (item.task().sameAs() + 1);
                items.set(i, new Item(item.task(), item.observation(), original.outcome(), original.chosen(),
                        original.reason() == null ? same : original.reason() + "; " + same));
            }
        }
        return new MatchupResults(rows, items);
    }

    private static Item item(MatchupRun.Task task, SeabassObservation observation, List<Map<String, String>> values,
                             Map<String, String> best) {
        switch (task.state()) {
            case SKIPPED:
                return MatchupRun.NO_OVERPASS.equals(task.message())
                        ? new Item(task, observation, Outcome.NO_OVERPASS, null, "no granule within the time window")
                        : new Item(task, observation, Outcome.SKIPPED, null, task.message());
            case FAILED:
                return new Item(task, observation, Outcome.FAILED, null, task.message());
            case PENDING:
            case SUBMITTED:
                return new Item(task, observation, Outcome.WAITING, null, null);
            default:
                break;
        }
        if (best == null) {
            String why = values.isEmpty() ? "no granule covers the observation"
                    : "the observation is outside every granule's pixels";
            return new Item(task, observation, Outcome.NOT_IN_GRANULE, null, why);
        }
        if ("matched".equals(best.get("matchup_status"))) {
            return new Item(task, observation, Outcome.MATCHED, best, null);
        }
        return new Item(task, observation, Outcome.REJECTED, best, reason(best.get("exclusion_levels")));
    }

    /**
     * A rejection in words, the level in brackets: {@code cloud or flags (3)}.
     * Several levels are joined with commas.
     */
    public static String reason(String levels) {
        if (levels == null || levels.isBlank()) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        for (String level : levels.trim().split("\\s+")) {
            parts.add(SHORT_REASONS.getOrDefault(level, "level") + " (" + level + ")");
        }
        return String.join(", ", parts);
    }

    /** The full reasons, one per level, for a tooltip. */
    public static String reasonInFull(String levels) {
        if (levels == null || levels.isBlank()) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        for (String level : levels.trim().split("\\s+")) {
            parts.add(level + ": " + REASONS.getOrDefault(level, "unknown level"));
        }
        return String.join("; ", parts);
    }

    /** Every granule row, in task order. */
    public List<GranuleRow> rows() {
        return rows;
    }

    /** One per task (observation and product), in task order. */
    public List<Item> items() {
        return items;
    }

    /** Items per outcome, for one product or (null) all. */
    public Map<Outcome, Integer> counts(String collection) {
        Map<Outcome, Integer> counts = new EnumMap<>(Outcome.class);
        for (Outcome outcome : Outcome.values()) {
            counts.put(outcome, 0);
        }
        for (Item item : items) {
            if (collection == null || collection.equals(item.task().collection())) {
                counts.merge(item.outcome(), 1, Integer::sum);
            }
        }
        return counts;
    }

    /** Rejected items by the short reason of their first level, most first. */
    public Map<String, Integer> rejections(String collection) {
        Map<String, Integer> reasons = new LinkedHashMap<>();
        for (Item item : items) {
            if (item.outcome() == Outcome.REJECTED
                    && (collection == null || collection.equals(item.task().collection()))) {
                String levels = item.chosen().getOrDefault("exclusion_levels", "").trim();
                String first = levels.isEmpty() ? "" : levels.split("\\s+")[0];
                reasons.merge(SHORT_REASONS.getOrDefault(first, "other"), 1, Integer::sum);
            }
        }
        Map<String, Integer> sorted = new LinkedHashMap<>();
        reasons.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(e -> sorted.put(e.getKey(), e.getValue()));
        return sorted;
    }

    /**
     * The summary line: {@code 231 matched · 120 rejected (cloud or flags 96, too variable 18) ·
     * 37 not in granule · 21 no overpass · 0 failed}. Outcomes with none are left out,
     * except matched and failed.
     */
    public String summary(String collection) {
        Map<Outcome, Integer> counts = counts(collection);
        List<String> parts = new ArrayList<>();
        for (Outcome outcome : Outcome.values()) {
            int n = counts.get(outcome);
            if (n == 0 && outcome != Outcome.MATCHED && outcome != Outcome.FAILED) {
                continue;
            }
            String part = n + " " + outcome.words();
            if (outcome == Outcome.REJECTED) {
                List<String> why = new ArrayList<>();
                rejections(collection).forEach((reason, count) -> why.add(reason + " " + count));
                part += " (" + String.join(", ", why) + ")";
            }
            parts.add(part);
        }
        return String.join(" · ", parts);
    }

    /**
     * Writes every granule row: the SeaBASS row and line, the product, whether
     * the output uses the row, then the service's columns (all products' columns,
     * in the order first seen; empty where a product has none).
     */
    public void writeCsv(Path out) throws IOException {
        Set<String> columns = new LinkedHashSet<>();
        for (GranuleRow row : rows) {
            columns.addAll(row.values().keySet());
        }
        List<String> header = new ArrayList<>(List.of("seabass_row", "seabass_line", "product", "collection",
                "chosen"));
        header.addAll(columns);
        try (Writer writer = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
            writer.write(csvLine(header));
            for (GranuleRow row : rows) {
                List<String> values = new ArrayList<>();
                values.add(String.valueOf(row.task().row() + 1));
                values.add(row.observation() == null ? "" : String.valueOf(row.observation().lineNumber()));
                values.add(MatchupCollections.byId(row.task().collection())
                        .map(MatchupCollections.Entry::shortName).orElse(""));
                values.add(row.task().collection());
                values.add(row.chosen() ? "yes" : "no");
                for (String column : columns) {
                    values.add(row.values().getOrDefault(column, ""));
                }
                writer.write(csvLine(values));
            }
        }
    }

    private static String csvLine(List<String> values) {
        List<String> quoted = new ArrayList<>();
        for (String value : values) {
            quoted.add(value.contains(",") || value.contains("\"") || value.contains("\n")
                    ? "\"" + value.replace("\"", "\"\"") + "\"" : value);
        }
        return String.join(",", quoted) + "\n";
    }
}
