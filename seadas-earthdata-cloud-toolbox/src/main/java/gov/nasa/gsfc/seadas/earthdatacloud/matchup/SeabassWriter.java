package gov.nasa.gsfc.seadas.earthdatacloud.matchup;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Writes a SeaBASS file with columns appended to it, as {@code mk_matchup.py}
 * does with {@code SB_support.py} {@code addDataToOutput} and {@code writeSBfile}.
 *
 * <p>Same as the script: new field names and units are lower case, a blank unit
 * is {@code none}, a blank value or a row with no value is the missing value, and
 * setting a field the file already has overwrites that row's value.
 *
 * <p>Different, on purpose:
 * <ul>
 *   <li>everything already in the file is written as it was: header lines in
 *       their order and case, comment and history lines where they were, and each
 *       value's text (the script rewrites numbers, e.g. {@code -9999} as
 *       {@code -9999.0}, and moves every comment after the headers);</li>
 *   <li>added comments go just before {@code /end_header};</li>
 *   <li>the missing value is written as the {@code /missing} header has it;</li>
 *   <li>the input file is never overwritten.</li>
 * </ul>
 */
public final class SeabassWriter {

    private final SeabassFile input;
    private final List<String> newFields = new ArrayList<>();
    private final Map<String, String> newUnits = new LinkedHashMap<>();
    private final Map<String, Map<Integer, String>> values = new HashMap<>();
    private final List<String> comments = new ArrayList<>();

    public SeabassWriter(SeabassFile input) {
        this.input = input;
    }

    /**
     * Sets one row's value of a field, adding the field (at the end, in the order
     * fields are first set) when the file doesn't have it.
     *
     * @param row   the data row (0-based)
     * @param field the field name; written in lower case
     * @param unit  its unit, used only when the field is new; blank means {@code none}
     * @param value the value as text; null or blank means missing
     */
    public void set(int row, String field, String unit, String value) {
        if (row < 0 || row >= input.rowCount()) {
            throw new IndexOutOfBoundsException("row " + row + " of " + input.rowCount());
        }
        String name = field.strip().toLowerCase(Locale.ROOT);
        if (name.isEmpty() || name.contains(",") || name.contains("=")) {
            throw new IllegalArgumentException("not a valid SeaBASS field name: " + field);
        }
        if (!input.hasField(name) && !newFields.contains(name)) {
            newFields.add(name);
            String u = unit == null || unit.isBlank() ? "none" : unit.strip().toLowerCase(Locale.ROOT);
            newUnits.put(name, u);
        }
        String text = value == null || value.isBlank() ? input.missingText() : value.strip();
        if (splitsOnDelimiter(text)) {
            throw new IllegalArgumentException("value for " + name + " contains the file's delimiter: " + text);
        }
        values.computeIfAbsent(name, k -> new HashMap<>()).put(row, text);
    }

    private boolean splitsOnDelimiter(String text) {
        switch (input.delimiter()) {
            case COMMA:
                return text.contains(",");
            case TAB:
                return text.contains("\t");
            default:
                return text.chars().anyMatch(Character::isWhitespace);
        }
    }

    /** Adds a comment line (without the leading {@code !}) at the end of the header. */
    public void addComment(String text) {
        comments.add(text == null ? "" : text);
    }

    /** The fields this writer adds, in order. */
    public List<String> addedFields() {
        return List.copyOf(newFields);
    }

    /** The output, line by line. */
    public List<String> lines() {
        List<String> out = new ArrayList<>();
        out.add("/begin_header");
        for (String line : input.headerLines()) {
            String lower = line.toLowerCase(Locale.ROOT);
            if (!newFields.isEmpty() && lower.startsWith("/fields=")) {
                out.add(line + "," + String.join(",", newFields));
            } else if (!newFields.isEmpty() && lower.startsWith("/units=")) {
                out.add(line + "," + String.join(",", newUnits.values()));
            } else {
                out.add(line);
            }
        }
        for (String comment : comments) {
            out.add("!" + comment);
        }
        out.add("/end_header");

        String separator = input.delimiter().separator();
        List<String> fields = input.fields();
        for (int row = 0; row < input.rowCount(); row++) {
            List<String> tokens = new ArrayList<>(input.row(row));
            for (int i = 0; i < fields.size(); i++) {
                Map<Integer, String> column = values.get(fields.get(i));
                if (column != null && column.containsKey(row)) {
                    tokens.set(i, column.get(row));
                }
            }
            for (String field : newFields) {
                tokens.add(values.get(field).getOrDefault(row, input.missingText()));
            }
            out.add(String.join(separator, tokens));
        }
        return out;
    }

    /**
     * Writes the output file, with {@code \n} line endings on every system.
     *
     * @throws IOException when {@code out} is the input file, or on a write error
     */
    public void write(Path out) throws IOException {
        Path source = input.source();
        if (source != null && Files.exists(out) && Files.isSameFile(source, out)) {
            throw new IOException("Won't overwrite the input SeaBASS file: " + out);
        }
        Files.writeString(out, String.join("\n", lines()) + "\n", StandardCharsets.ISO_8859_1);
    }
}
