package gov.nasa.gsfc.seadas.earthdatacloud.matchup;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * A SeaBASS data file, read the way {@code mk_matchup.py} reads it
 * ({@code seabass/SB_support.py} {@code readSB}, with all masking off).
 *
 * <p>Header keys and field names are matched in lower case, as the script does.
 * Every data value is kept as the text in the file, so writing the file back
 * changes nothing but what is added ({@link SeabassWriter}).
 *
 * <p>Differences from {@code readSB}, all on input it would mishandle:
 * <ul>
 *   <li>a data line with fewer or more values than {@code /fields} is an error
 *       (the script silently drops or misaligns values); trailing delimiters are
 *       allowed;</li>
 *   <li>a header line is any line starting with {@code /}, a comment any line
 *       starting with {@code !} (the script skips a header whose value holds a
 *       {@code !});</li>
 *   <li>{@code /missing=0} is accepted.</li>
 * </ul>
 */
public final class SeabassFile {

    /** The {@code /delimiter} header: how values are split, and the character written between them. */
    public enum Delimiter {
        COMMA(",", Pattern.compile(",+")),
        SPACE(" ", Pattern.compile("\\s+")),
        TAB("\t", Pattern.compile("\t+"));

        private final String separator;
        private final Pattern splitter;

        Delimiter(String separator, Pattern splitter) {
            this.separator = separator;
            this.splitter = splitter;
        }

        public String separator() {
            return separator;
        }

        List<String> split(String line) {
            List<String> tokens = new ArrayList<>(Arrays.asList(splitter.split(line, -1)));
            while (!tokens.isEmpty() && tokens.get(tokens.size() - 1).isEmpty()) {
                tokens.remove(tokens.size() - 1);
            }
            return tokens;
        }
    }

    private final Path source;
    private final List<String> headerLines;
    private final Map<String, String> headers;
    private final List<String> fields;
    private final List<String> units;
    private final String missingText;
    private final double missing;
    private final Delimiter delimiter;
    private final List<List<String>> rows;
    private final List<Integer> rowLines;
    private final List<String> warnings;

    private SeabassFile(Path source, List<String> headerLines, Map<String, String> headers,
                        List<String> fields, List<String> units, String missingText, double missing,
                        Delimiter delimiter, List<List<String>> rows, List<Integer> rowLines,
                        List<String> warnings) {
        this.source = source;
        this.headerLines = Collections.unmodifiableList(headerLines);
        this.headers = Collections.unmodifiableMap(headers);
        this.fields = Collections.unmodifiableList(fields);
        this.units = Collections.unmodifiableList(units);
        this.missingText = missingText;
        this.missing = missing;
        this.delimiter = delimiter;
        this.rows = Collections.unmodifiableList(rows);
        this.rowLines = Collections.unmodifiableList(rowLines);
        this.warnings = Collections.unmodifiableList(warnings);
    }

    /** Reads a SeaBASS file. Bytes are read as ISO-8859-1, so any byte survives a round trip. */
    public static SeabassFile read(Path path) throws IOException {
        return parse(Files.readAllLines(path, StandardCharsets.ISO_8859_1), path);
    }

    /** Parses SeaBASS lines; {@code source} only names the file in messages and may be null. */
    public static SeabassFile parse(List<String> lines, Path source) throws SeabassFormatException {
        String name = source != null ? source.getFileName().toString() : "SeaBASS data";
        List<String> headerLines = new ArrayList<>();
        Map<String, String> headers = new LinkedHashMap<>();
        List<String> warnings = new ArrayList<>();
        int index = 0;
        boolean endHeader = false;

        for (; index < lines.size(); index++) {
            String line = lines.get(index).strip();
            String lower = line.toLowerCase(Locale.ROOT);
            if (lower.startsWith("/begin_header")) {
                continue;
            }
            if (lower.startsWith("/end_header")) {
                endHeader = true;
                index++;
                break;
            }
            if (line.isEmpty()) {
                continue;
            }
            headerLines.add(line);
            if (line.startsWith("!")) {
                continue;
            }
            int eq = line.indexOf('=');
            if (!line.startsWith("/") || eq < 0) {
                throw new SeabassFormatException(name + ", line " + (index + 1)
                        + ": not a header line (/key=value) or comment (!): " + line);
            }
            headers.put(line.substring(1, eq).toLowerCase(Locale.ROOT), line.substring(eq + 1));
        }
        if (!endHeader) {
            throw new SeabassFormatException(name + ": no /end_header line");
        }

        Delimiter delimiter = delimiter(headers.get("delimiter"), name);
        String missingText = required(headers, "missing", name).strip();
        double missing;
        try {
            missing = Double.parseDouble(missingText);
        } catch (NumberFormatException e) {
            throw new SeabassFormatException(name + ": /missing is not a number: " + missingText);
        }
        List<String> fields = splitHeaderList(required(headers, "fields", name));
        for (int i = 0; i < fields.size(); i++) {
            if (fields.indexOf(fields.get(i)) != i) {
                throw new SeabassFormatException(name + ": field " + fields.get(i) + " appears twice in /fields");
            }
        }
        List<String> units = headers.containsKey("units") ? splitHeaderList(headers.get("units")) : List.of();
        if (units.isEmpty()) {
            warnings.add("No /units header");
        } else if (units.size() != fields.size()) {
            warnings.add("/units has " + units.size() + " entries for " + fields.size() + " fields");
        }

        List<List<String>> rows = new ArrayList<>();
        List<Integer> rowLines = new ArrayList<>();
        for (; index < lines.size(); index++) {
            String line = lines.get(index).strip();
            if (line.isEmpty()) {
                continue;
            }
            List<String> tokens = delimiter.split(line);
            if (tokens.size() != fields.size()) {
                throw new SeabassFormatException(name + ", line " + (index + 1) + ": " + tokens.size()
                        + " values for " + fields.size() + " fields (check the file with FCHECK)");
            }
            rows.add(Collections.unmodifiableList(tokens));
            rowLines.add(index + 1);
        }
        return new SeabassFile(source, headerLines, headers, fields, units, missingText, missing,
                delimiter, rows, rowLines, warnings);
    }

    private static Delimiter delimiter(String value, String name) throws SeabassFormatException {
        if (value == null) {
            throw new SeabassFormatException(name + ": no /delimiter header");
        }
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.contains("comma")) {
            return Delimiter.COMMA;
        }
        if (lower.contains("space")) {
            return Delimiter.SPACE;
        }
        if (lower.contains("tab")) {
            return Delimiter.TAB;
        }
        throw new SeabassFormatException(name + ": /delimiter must be comma, space or tab: " + value);
    }

    private static String required(Map<String, String> headers, String key, String name)
            throws SeabassFormatException {
        String value = headers.get(key);
        if (value == null || value.isBlank()) {
            throw new SeabassFormatException(name + ": no /" + key + " header");
        }
        return value;
    }

    private static List<String> splitHeaderList(String value) {
        List<String> items = new ArrayList<>();
        for (String item : value.split(",", -1)) {
            items.add(item.strip().toLowerCase(Locale.ROOT));
        }
        return items;
    }

    /** The file read, or null when parsed from lines. */
    public Path source() {
        return source;
    }

    /** Header lines between {@code /begin_header} and {@code /end_header}, comments included, in order. */
    public List<String> headerLines() {
        return headerLines;
    }

    /** Header values keyed by lower-case name, without the leading {@code /}. */
    public Map<String, String> headers() {
        return headers;
    }

    /** Field names from {@code /fields}, in lower case. */
    public List<String> fields() {
        return fields;
    }

    /** Units from {@code /units}, in lower case; empty when the header is absent. */
    public List<String> units() {
        return units;
    }

    /** The {@code /missing} value as written in the header. */
    public String missingText() {
        return missingText;
    }

    public Delimiter delimiter() {
        return delimiter;
    }

    /** Problems that don't stop the file being used. */
    public List<String> warnings() {
        return warnings;
    }

    public int rowCount() {
        return rows.size();
    }

    /** The values of one data row, as written. */
    public List<String> row(int row) {
        return rows.get(row);
    }

    /** The 1-based line number of a data row in the file. */
    public int lineNumber(int row) {
        return rowLines.get(row);
    }

    /** The index of a field (any case), or -1. */
    public int fieldIndex(String field) {
        return fields.indexOf(field.toLowerCase(Locale.ROOT));
    }

    public boolean hasField(String field) {
        return fieldIndex(field) >= 0;
    }

    /** A value as written, or null when the file has no such field. */
    public String value(int row, String field) {
        int i = fieldIndex(field);
        return i < 0 ? null : rows.get(row).get(i);
    }

    /** Whether a value is the {@code /missing} value (compared as numbers: -9999 and -9999.0 are the same). */
    public boolean isMissing(String value) {
        if (value == null) {
            return true;
        }
        try {
            return Double.parseDouble(value.strip()) == missing;
        } catch (NumberFormatException e) {
            return value.strip().equals(missingText);
        }
    }

    /**
     * Whether a value is not a measurement: the {@code /missing} value, or the
     * {@code /below_detection_limit} or {@code /above_detection_limit} flag
     * (HPLC files write -8888 for a pigment below detection).
     */
    public boolean isFlagged(String value) {
        if (isMissing(value)) {
            return true;
        }
        for (String key : List.of("below_detection_limit", "above_detection_limit")) {
            String flag = headers.get(key);
            if (flag == null || flag.isBlank()) {
                continue;
            }
            try {
                if (Double.parseDouble(value.strip()) == Double.parseDouble(flag.strip())) {
                    return true;
                }
            } catch (NumberFormatException e) {
                if (value.strip().equals(flag.strip())) {
                    return true;
                }
            }
        }
        return false;
    }
}
