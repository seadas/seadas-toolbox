package gov.nasa.gsfc.seadas.earthdatacloud.matchup;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A CSV the service wrote for one granule: a header, then one row per
 * observation (Python's {@code csv} module, so a value with a comma or quote is
 * quoted).
 *
 * @param header column names in order
 * @param rows   each row by column name
 */
public record MatchupCsv(List<String> header, List<Map<String, String>> rows) {

    /** Columns every service CSV starts with. */
    public static final List<String> FIXED = List.of("obs_lat", "obs_lon", "obs_datetime", "granule",
            "matchup_status", "exclusion_levels", "tdiff", "rrsaot_cv", "pixel_count",
            "unflagged_pixel_count", "flagged_pixel_count");

    public MatchupCsv {
        header = List.copyOf(header);
        rows = Collections.unmodifiableList(new ArrayList<>(rows));
    }

    public static MatchupCsv read(Path path) throws IOException {
        return parse(Files.readString(path, StandardCharsets.UTF_8), path.getFileName().toString());
    }

    static MatchupCsv parse(String text, String name) throws IOException {
        List<List<String>> records = records(text, name);
        if (records.isEmpty()) {
            throw new IOException(name + ": empty CSV");
        }
        List<String> header = records.get(0);
        if (!header.containsAll(FIXED)) {
            List<String> missing = new ArrayList<>(FIXED);
            missing.removeAll(header);
            throw new IOException(name + ": not a matchup CSV, no " + String.join(", ", missing));
        }
        List<Map<String, String>> rows = new ArrayList<>();
        for (int r = 1; r < records.size(); r++) {
            List<String> values = records.get(r);
            if (values.size() == 1 && values.get(0).isEmpty()) {
                continue;
            }
            if (values.size() != header.size()) {
                throw new IOException(name + ", record " + (r + 1) + ": " + values.size() + " values for "
                        + header.size() + " columns");
            }
            Map<String, String> row = new LinkedHashMap<>();
            for (int c = 0; c < header.size(); c++) {
                row.put(header.get(c), values.get(c));
            }
            rows.add(row);
        }
        return new MatchupCsv(header, rows);
    }

    private static List<List<String>> records(String text, String name) throws IOException {
        List<List<String>> records = new ArrayList<>();
        List<String> record = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false;
        boolean any = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            any = true;
            if (quoted) {
                if (c == '"' && i + 1 < text.length() && text.charAt(i + 1) == '"') {
                    value.append('"');
                    i++;
                } else if (c == '"') {
                    quoted = false;
                } else {
                    value.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                record.add(value.toString());
                value.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
                record.add(value.toString());
                value.setLength(0);
                records.add(record);
                record = new ArrayList<>();
                any = false;
            } else {
                value.append(c);
            }
        }
        if (quoted) {
            throw new IOException(name + ": unclosed quote");
        }
        if (any) {
            record.add(value.toString());
            records.add(record);
        }
        return records;
    }
}
