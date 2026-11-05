package gov.nasa.gsfc.seadas.earthdatacloud.matchup.ui;

import gov.nasa.gsfc.seadas.earthdatacloud.matchup.HarmonyEnvironment;
import gov.nasa.gsfc.seadas.earthdatacloud.matchup.HttpTransport;
import gov.nasa.gsfc.seadas.earthdatacloud.matchup.MatchupCollections;
import gov.nasa.gsfc.seadas.earthdatacloud.matchup.MatchupProduct;
import gov.nasa.gsfc.seadas.earthdatacloud.matchup.MatchupResults;
import gov.nasa.gsfc.seadas.earthdatacloud.matchup.MatchupResults.Outcome;
import gov.nasa.gsfc.seadas.earthdatacloud.matchup.MatchupRun;
import gov.nasa.gsfc.seadas.earthdatacloud.matchup.ResultJoiner;
import gov.nasa.gsfc.seadas.earthdatacloud.matchup.SeabassFile;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Step 4: what each observation got, and the exports. */
final class ResultsStep extends Step {

    private static final String ALL_PRODUCTS = "All products";
    /** The chips, in the order shown; the last three only when there are some. */
    private static final List<Outcome> CHIPS = List.of(Outcome.MATCHED, Outcome.REJECTED, Outcome.NOT_IN_GRANULE,
            Outcome.NO_OVERPASS, Outcome.FAILED, Outcome.SKIPPED, Outcome.WAITING);

    private final JComboBox<String> product = new JComboBox<>();
    private final Map<String, String> productIds = new LinkedHashMap<>();
    private final JPanel chips = Look.clear(new FlowLayout(FlowLayout.LEFT, 8, 0));
    private final ButtonGroup chipGroup = new ButtonGroup();
    private final Look.Wrap rejections = Look.small(" ");
    private final ItemTable model = new ItemTable();
    private final JTable table = new JTable(model);
    private final JButton exportSeabass = new JButton("Export SeaBASS…");
    private final JButton exportCsv = new JButton("Export CSV…");
    private MatchupResults results;
    private Outcome showing;
    private boolean updating;

    ResultsStep(MatchupSession session) {
        super(session);

        JPanel head = Look.clear(new BorderLayout(16, 0));
        head.add(Look.heading("Results", null), BorderLayout.WEST);
        head.add(Look.row(8, new JLabel("Product"), product), BorderLayout.EAST);
        JPanel filters = Look.clear(new BorderLayout(6, 0));
        filters.add(chips, BorderLayout.WEST);
        filters.add(rejections, BorderLayout.CENTER);
        JPanel north = Look.clear(new BorderLayout(0, 12));
        north.add(head, BorderLayout.NORTH);
        north.add(filters, BorderLayout.SOUTH);
        add(north, BorderLayout.NORTH);

        Look.style(table);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setAutoCreateRowSorter(true);
        add(Look.scroll(table), BorderLayout.CENTER);

        JButton productView = later("Show on product view");
        JButton granule = later("Open granule remotely");
        JButton scatter = later("Scatter plot…");
        JPanel buttons = Look.clear(new BorderLayout());
        buttons.add(Look.row(8, productView, granule, scatter), BorderLayout.WEST);
        buttons.add(Look.row(8, exportCsv, exportSeabass), BorderLayout.EAST);
        JPanel south = Look.clear(null);
        south.setLayout(new BoxLayout(south, BoxLayout.Y_AXIS));
        buttons.setAlignmentX(LEFT_ALIGNMENT);
        JLabel note = Look.small("<html>SeaBASS output: one row per observation, written to a new file. Where "
                + "several granules match, the row uses a matched granule first, then the smallest |tdiff|. "
                + "The CSV keeps every granule.</html>");
        note.setAlignmentX(LEFT_ALIGNMENT);
        south.add(buttons);
        south.add(Box.createVerticalStrut(10));
        south.add(note);
        add(south, BorderLayout.SOUTH);

        product.addActionListener(e -> {
            if (!updating) {
                filter();
            }
        });
        exportSeabass.addActionListener(e -> exportSeabass());
        exportCsv.addActionListener(e -> exportCsv());
        refresh();
    }

    private static JButton later(String text) {
        JButton button = new JButton(text);
        button.setEnabled(false);
        button.setToolTipText("Comes in a later version");
        return button;
    }

    @Override
    String title() {
        return "Results";
    }

    @Override
    String help() {
        return "Each observation and product ends one of these ways: matched; rejected by a matchup rule (the "
                + "reason and its level in brackets: 1 time, 2–4 cloud or flags, 5 too variable); not in granule "
                + "(no pixel of any granule at the position); no overpass (no granule in the time window, so not "
                + "sent); failed; or not usable (no time or position).\n\n"
                + "Export SeaBASS writes the input file with the matchup columns added, as mk_matchup.py does. "
                + "Export CSV writes every granule row the service returned.";
    }

    @Override
    String navNote() {
        MatchupRun run = session.run();
        if (run == null) {
            return null;
        }
        Map<MatchupRun.State, Integer> counts = run.counts();
        int sent = run.tasks().size() - counts.get(MatchupRun.State.SKIPPED);
        return (run.finished() ? "Run finished" : "Run not finished: results so far") + " · "
                + Text.plural(sent, "request", "requests") + " · " + counts.get(MatchupRun.State.FAILED) + " failed";
    }

    @Override
    String unavailableReason() {
        MatchupRun run = session.run();
        return run == null || run.counts().get(MatchupRun.State.DONE) == 0 ? "No results yet" : null;
    }

    @Override
    void shown() {
        MatchupRun run = session.run();
        if (run == null) {
            return;
        }
        Background.run(this, "Reading the results", () -> MatchupResults.of(run), read -> {
            results = read;
            refresh();
        }, null);
    }

    private void refresh() {
        updating = true;
        try {
            String selected = (String) product.getSelectedItem();
            product.removeAllItems();
            productIds.clear();
            for (MatchupCollections.Entry entry : session.chosen()) {
                String name = SatelliteStep.shortLabel(entry);
                productIds.put(name, entry.id(session.options().environment()));
                product.addItem(name);
            }
            if (productIds.size() > 1) {
                product.addItem(ALL_PRODUCTS);
            }
            if (selected != null && (productIds.containsKey(selected) || ALL_PRODUCTS.equals(selected))) {
                product.setSelectedItem(selected);
            }
        } finally {
            updating = false;
        }
        boolean have = results != null;
        exportSeabass.setEnabled(have);
        exportCsv.setEnabled(have);
        filter();
    }

    /** The product shown, or null for all. */
    private String collection() {
        return productIds.get((String) product.getSelectedItem());
    }

    private void filter() {
        if (results == null) {
            model.set(List.of(), List.of(), false);
            rejections.setText(" ");
            showChips(null);
            return;
        }
        String collection = collection();
        showChips(collection);
        Map<String, Integer> why = results.rejections(collection);
        List<String> parts = new ArrayList<>();
        why.forEach((reason, n) -> parts.add(reason + " " + n));
        rejections.setText(parts.isEmpty() ? " " : "Rejected: " + String.join(" · ", parts));

        List<MatchupResults.Item> items = new ArrayList<>();
        for (MatchupResults.Item item : results.items()) {
            if ((collection == null || collection.equals(item.task().collection()))
                    && (showing == null || item.outcome() == showing)) {
                items.add(item);
            }
        }
        // By observation, then product in the run's order.
        List<String> order = session.run() == null ? List.of() : session.run().settings().collections();
        items.sort(Comparator.comparingInt((MatchupResults.Item i) -> i.task().row())
                .thenComparingInt(i -> order.indexOf(i.task().collection())));
        model.set(items, columns(collection, items), collection == null);

        for (int i = 0; i < table.getColumnCount(); i++) {
            String name = model.getColumnName(i);
            int width = switch (name) {
                case "Obs" -> 55;
                case "Date-time (UTC)" -> 150;
                case "Product" -> 145;
                case "Granule" -> 300;
                case "Status" -> 200;
                case "tdiff (s)" -> 80;
                case "CV" -> 65;
                default -> 95;
            };
            table.getColumnModel().getColumn(i).setPreferredWidth(width);
            if (!name.equals("Status") && !name.equals("Product")) {
                table.getColumnModel().getColumn(i).setCellRenderer(new Look.Cell(true));
            }
            if (name.equals("Status")) {
                table.getColumnModel().getColumn(i).setCellRenderer(new Look.PillCell() {
                    @Override
                    Look.Tone tone(Object value, int row) {
                        return tone(model.item(row).outcome());
                    }

                    private Look.Tone tone(Outcome outcome) {
                        return switch (outcome) {
                            case MATCHED -> Look.Tone.STRONG_BLUE;
                            case REJECTED -> Look.Tone.ORANGE;
                            case FAILED -> Look.Tone.RED;
                            case NO_OVERPASS, SKIPPED -> Look.Tone.DASHED;
                            case NOT_IN_GRANULE, WAITING -> Look.Tone.GREY;
                        };
                    }
                });
            }
        }
    }

    /** The filter chips with their counts for the product shown. */
    private void showChips(String collection) {
        chips.removeAll();
        for (javax.swing.AbstractButton b : java.util.Collections.list(chipGroup.getElements())) {
            chipGroup.remove(b);
        }
        Map<Outcome, Integer> counts = results == null ? Map.of() : results.counts(collection);
        int all = counts.values().stream().mapToInt(Integer::intValue).sum();
        addChip("All", all, null);
        for (Outcome outcome : CHIPS) {
            int n = counts.getOrDefault(outcome, 0);
            if (n > 0 || CHIPS.indexOf(outcome) < 4) {
                addChip(capital(outcome.words()), n, outcome);
            }
        }
        chips.revalidate();
        chips.repaint();
    }

    private void addChip(String label, int count, Outcome outcome) {
        Chip chip = new Chip(label, count);
        chip.setSelected(outcome == showing);
        chip.addActionListener(e -> {
            showing = outcome;
            filter();
        });
        chipGroup.add(chip);
        chips.add(chip);
    }

    private static String capital(String words) {
        return Character.toUpperCase(words.charAt(0)) + words.substring(1);
    }

    /**
     * Columns after the fixed ones, for one product: each satellite variable,
     * after its in-situ counterpart when the file has one (chl, chlor_a sat).
     */
    private List<Column> columns(String collection, List<MatchupResults.Item> items) {
        List<Column> columns = new ArrayList<>();
        if (collection == null) {
            return columns;
        }
        List<String> variables = new ArrayList<>();
        for (MatchupResults.Item item : items) {
            if (item.chosen() != null) {
                for (String key : item.chosen().keySet()) {
                    if (key.startsWith("sat_") && key.endsWith("_mean") && !variables.contains(key)) {
                        variables.add(key);
                    }
                }
            }
        }
        // Pairs first (in situ beside satellite), then the other variables.
        SeabassFile file = session.file();
        List<Column> unpaired = new ArrayList<>();
        for (String key : variables) {
            String variable = key.substring("sat_".length(), key.length() - "_mean".length());
            String field = file == null ? null : inSituField(file, variable);
            if (field != null) {
                columns.add(new Column(field + " in situ", field, null));
                columns.add(new Column(variable + " sat", null, key));
            } else {
                unpaired.add(new Column(variable + " sat", null, key));
            }
        }
        columns.addAll(unpaired);
        return columns;
    }

    /** The SeaBASS field measuring what a satellite variable does, or null. */
    static String inSituField(SeabassFile file, String variable) {
        List<String> candidates = new ArrayList<>();
        String lower = variable.toLowerCase(Locale.ROOT);
        if (lower.equals("chlor_a")) {
            candidates.addAll(List.of("chl", "chl_a", "chla", "tchl"));
        } else if (lower.startsWith("sst")) {
            candidates.addAll(List.of("wt", "sst"));
        } else if (lower.matches("rrs_\\d+")) {
            String band = lower.substring(4);
            candidates.addAll(List.of("rrs" + band, "rrs_" + band));
        }
        candidates.add(lower);
        for (String candidate : candidates) {
            for (String field : file.fields()) {
                if (field.toLowerCase(Locale.ROOT).equals(candidate)) {
                    return field;
                }
            }
        }
        return null;
    }

    // ------------------------------------------------------------ exports

    private void exportSeabass() {
        MatchupRun run = session.run();
        JRadioButton all = new JRadioButton("Every observation, with matchup_found yes or no", true);
        JRadioButton matched = new JRadioButton("Only observations with a match (as mk_matchup.py by default)");
        ButtonGroup group = new ButtonGroup();
        group.add(all);
        group.add(matched);
        JPanel options = new JPanel(new GridLayout(0, 1));
        options.add(all);
        options.add(matched);
        if (JOptionPane.showConfirmDialog(this, options, "Export SeaBASS", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
            return;
        }
        Path out = chooseOutput("Export SeaBASS", MatchupSession.outputPath(run.seabass(), ".sb"));
        if (out == null) {
            return;
        }
        ResultJoiner.Rows rows = matched.isSelected() ? ResultJoiner.Rows.MATCHED : ResultJoiner.Rows.ALL;
        HarmonyEnvironment environment = run.settings().environment();
        exportSeabass.setEnabled(false);
        Background.run(this, "Export SeaBASS", () -> {
            Map<String, MatchupProduct> products = new LinkedHashMap<>();
            for (String id : run.settings().collections()) {
                MatchupProduct product = session.products().get(id);
                if (product == null) {
                    product = MatchupProduct.fetch(HttpTransport.jdk(), environment.cmr(), id);
                    session.addProduct(product);
                }
                products.put(id, product);
            }
            ResultJoiner.Joined joined = run.join(products, rows);
            joined.writer().write(out);
            return joined.warnings();
        }, warnings -> {
            String message = "Written: " + out;
            if (!warnings.isEmpty()) {
                message += "\n\nWarnings (also in the file's header):\n" + String.join("\n", warnings);
            }
            JOptionPane.showMessageDialog(this, Text.wrap(message), "Export SeaBASS",
                    warnings.isEmpty() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
        }, () -> exportSeabass.setEnabled(true));
    }

    private void exportCsv() {
        MatchupResults current = results;
        Path out = chooseOutput("Export CSV", MatchupSession.outputPath(session.run().seabass(), ".csv"));
        if (out == null) {
            return;
        }
        Background.run(this, "Export CSV", () -> {
            current.writeCsv(out);
            return out;
        }, written -> JOptionPane.showMessageDialog(this, Text.wrap("Written: " + written), "Export CSV",
                JOptionPane.INFORMATION_MESSAGE), null);
    }

    private Path chooseOutput(String title, Path suggested) {
        JFileChooser chooser = new JFileChooser(suggested.getParent().toFile());
        chooser.setDialogTitle(title);
        chooser.setSelectedFile(suggested.toFile());
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        Path out = chooser.getSelectedFile().toPath().toAbsolutePath().normalize();
        if (out.equals(session.run().seabass())) {
            JOptionPane.showMessageDialog(this, "That is the input file; choose another name.", title,
                    JOptionPane.WARNING_MESSAGE);
            return null;
        }
        if (Files.exists(out) && JOptionPane.showConfirmDialog(this, Text.wrap(out.getFileName()
                + " exists. Replace it?"), title, JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return null;
        }
        return out;
    }

    // ------------------------------------------------------------ chips and table

    /** A rounded filter button: its name and count, filled when chosen. */
    private static final class Chip extends JToggleButton {
        Chip(String label, int count) {
            super(label + "  " + count);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setFont(Look.base().deriveFont(Font.PLAIN));
            getModel().addChangeListener(e -> setFont(Look.base().deriveFont(isSelected() ? Font.BOLD : Font.PLAIN)));
        }

        @Override
        public Dimension getPreferredSize() {
            int w = Look.width(this, Look.base().deriveFont(Font.BOLD), getText()) + 26;
            return new Dimension(w, 30);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Look.smooth(g);
            g2.setColor(isSelected() ? Look.BLUE_PALE : Color.WHITE);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
            g2.setColor(isSelected() ? Look.BLUE : hasFocus() ? Look.BORDER_DARK : Look.BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
            g2.setFont(getFont());
            g2.setColor(Look.TEXT);
            var metrics = g2.getFontMetrics();
            g2.drawString(getText(), (getWidth() - metrics.stringWidth(getText())) / 2,
                    (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
            g2.dispose();
        }
    }

    /** An extra column: an in-situ field, or a satellite value from the chosen granule row. */
    private record Column(String name, String field, String key) {
    }

    private final class ItemTable extends AbstractTableModel {
        private List<MatchupResults.Item> items = List.of();
        private List<Column> extra = List.of();
        private List<String> fixed = List.of();

        void set(List<MatchupResults.Item> newItems, List<Column> newExtra, boolean withProduct) {
            List<String> newFixed = withProduct
                    ? List.of("Obs", "Date-time (UTC)", "Product", "Granule", "Status", "tdiff (s)", "CV")
                    : List.of("Obs", "Date-time (UTC)", "Granule", "Status", "tdiff (s)", "CV");
            boolean structure = !newExtra.equals(extra) || !newFixed.equals(fixed);
            items = newItems;
            extra = newExtra;
            fixed = newFixed;
            if (structure) {
                fireTableStructureChanged();
            } else {
                fireTableDataChanged();
            }
        }

        MatchupResults.Item item(int row) {
            return items.get(row);
        }

        @Override
        public int getRowCount() {
            return items.size();
        }

        @Override
        public int getColumnCount() {
            return fixed.size() + extra.size();
        }

        @Override
        public String getColumnName(int column) {
            return column < fixed.size() ? fixed.get(column) : extra.get(column - fixed.size()).name();
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return getColumnName(column).equals("Obs") ? Integer.class : String.class;
        }

        @Override
        public Object getValueAt(int row, int column) {
            MatchupResults.Item item = items.get(row);
            Map<String, String> chosen = item.chosen();
            if (column >= fixed.size()) {
                Column c = extra.get(column - fixed.size());
                if (c.field() != null) {
                    SeabassFile file = session.file();
                    String value = file == null ? null : file.value(item.task().row(), c.field());
                    return value == null || file.isMissing(value) ? "—" : value;
                }
                return value(chosen, c.key());
            }
            return switch (fixed.get(column)) {
                case "Obs" -> item.task().row() + 1;
                case "Date-time (UTC)" -> item.observation() == null || item.observation().time() == null ? "—"
                        : Text.time(item.observation().time()).substring(0, 16);
                case "Product" -> MatchupCollections.byId(item.task().collection()).map(SatelliteStep::shortLabel)
                        .orElse(item.task().collection());
                case "Granule" -> chosen == null ? "—" : chosen.getOrDefault("granule", "—");
                case "Status" -> status(item);
                case "tdiff (s)" -> value(chosen, "tdiff");
                default -> {
                    String cv = value(chosen, "rrsaot_cv");
                    try {
                        yield String.format(Locale.ROOT, "%.3f", Double.parseDouble(cv));
                    } catch (NumberFormatException e) {
                        yield cv;
                    }
                }
            };
        }

        private String value(Map<String, String> chosen, String key) {
            String value = chosen == null ? null : chosen.get(key);
            return value == null || value.isBlank() ? "—" : value;
        }

        /** {@code matched}, {@code rejected · too variable (5)}, {@code matched · same as #3}. */
        private String status(MatchupResults.Item item) {
            String text = item.outcome().words();
            if (item.outcome() == Outcome.REJECTED && item.chosen() != null) {
                text += " · " + MatchupResults.reason(item.chosen().get("exclusion_levels"));
            }
            if (item.task().sameAs() >= 0) {
                text += " · same as #" + (item.task().sameAs() + 1);
            }
            return text;
        }
    }
}
