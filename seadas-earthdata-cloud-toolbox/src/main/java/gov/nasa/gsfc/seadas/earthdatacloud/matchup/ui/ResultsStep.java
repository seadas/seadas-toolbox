package gov.nasa.gsfc.seadas.earthdatacloud.matchup.ui;

import gov.nasa.gsfc.seadas.earthdatacloud.matchup.HarmonyEnvironment;
import gov.nasa.gsfc.seadas.earthdatacloud.matchup.HttpTransport;
import gov.nasa.gsfc.seadas.earthdatacloud.matchup.MatchupCollections;
import gov.nasa.gsfc.seadas.earthdatacloud.matchup.MatchupProduct;
import gov.nasa.gsfc.seadas.earthdatacloud.matchup.MatchupResults;
import gov.nasa.gsfc.seadas.earthdatacloud.matchup.MatchupRun;
import gov.nasa.gsfc.seadas.earthdatacloud.matchup.ResultJoiner;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Step 4: what each observation got, and the exports. */
final class ResultsStep extends Step {

    private static final String ALL_PRODUCTS = "All products";

    private final JLabel summary = new JLabel(" ");
    private final JComboBox<String> show = new JComboBox<>();
    private final JComboBox<String> product = new JComboBox<>();
    private final ItemTable model = new ItemTable();
    private final JTable table = new JTable(model);
    private final JButton exportSeabass = new JButton("Export SeaBASS…");
    private final JButton exportCsv = new JButton("Export CSV…");
    private MatchupResults results;
    private boolean updating;

    ResultsStep(MatchupSession session) {
        super(session);

        show.addItem("All");
        for (MatchupResults.Outcome outcome : MatchupResults.Outcome.values()) {
            show.addItem(outcome.words());
        }
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        filters.add(new JLabel("Show"));
        filters.add(show);
        filters.add(new JLabel("Product"));
        filters.add(product);
        JPanel top = new JPanel(new GridLayout(0, 1, 0, 6));
        top.add(summary);
        top.add(filters);
        add(top, BorderLayout.NORTH);

        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setAutoCreateRowSorter(true);
        table.setDefaultRenderer(String.class, new ReasonRenderer());
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        buttons.add(exportSeabass);
        buttons.add(exportCsv);
        buttons.add(new JLabel("The SeaBASS export adds the columns mk_matchup.py writes; the CSV has every granule row."));
        add(buttons, BorderLayout.SOUTH);

        show.addActionListener(e -> filter());
        product.addActionListener(e -> filter());
        exportSeabass.addActionListener(e -> exportSeabass());
        exportCsv.addActionListener(e -> exportCsv());
        refresh();
    }

    @Override
    String title() {
        return "Results";
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
            product.addItem(ALL_PRODUCTS);
            for (MatchupCollections.Entry entry : session.chosen()) {
                product.addItem(entry.shortName());
            }
            product.setSelectedItem(selected != null ? selected : ALL_PRODUCTS);
        } finally {
            updating = false;
        }
        boolean have = results != null;
        exportSeabass.setEnabled(have);
        exportCsv.setEnabled(have);
        filter();
    }

    private String collection() {
        String name = (String) product.getSelectedItem();
        if (name == null || ALL_PRODUCTS.equals(name)) {
            return null;
        }
        return session.chosen().stream().filter(e -> e.shortName().equals(name)).findFirst()
                .map(e -> e.id(session.options().environment())).orElse(null);
    }

    private void filter() {
        if (updating) {
            return;
        }
        if (results == null) {
            summary.setText(" ");
            model.set(List.of(), List.of());
            return;
        }
        String collection = collection();
        MatchupRun run = session.run();
        String waiting = run != null && !run.finished() ? " (the run isn't finished)" : "";
        summary.setText("<html>" + Text.escape(results.summary(collection)) + waiting + "</html>");

        String wanted = (String) show.getSelectedItem();
        List<MatchupResults.Item> items = new ArrayList<>();
        for (MatchupResults.Item item : results.items()) {
            if ((collection == null || collection.equals(item.task().collection()))
                    && ("All".equals(wanted) || item.outcome().words().equals(wanted))) {
                items.add(item);
            }
        }
        // By observation, then product in the run's order.
        List<String> order = session.run() == null ? List.of() : session.run().settings().collections();
        items.sort(java.util.Comparator.comparingInt((MatchupResults.Item i) -> i.task().row())
                .thenComparingInt(i -> order.indexOf(i.task().collection())));
        // One product: its variables' means as columns.
        List<String> variables = new ArrayList<>();
        if (collection != null) {
            for (MatchupResults.Item item : items) {
                if (item.chosen() != null) {
                    for (String column : item.chosen().keySet()) {
                        if (column.startsWith("sat_") && column.endsWith("_mean") && !variables.contains(column)) {
                            variables.add(column);
                        }
                    }
                }
            }
        }
        model.set(items, variables);
        int[] widths = {40, 45, 140, 150, 95, 190, 300, 70, 70};
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(i < widths.length ? widths[i] : 90);
        }
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

    // ------------------------------------------------------------ table

    private final class ItemTable extends AbstractTableModel {
        private final String[] fixed = {"#", "Line", "Date-time (UTC)", "Product", "Outcome", "Reason", "Granule",
                "tdiff (s)", "CV"};
        private List<MatchupResults.Item> items = List.of();
        private List<String> variables = List.of();

        void set(List<MatchupResults.Item> newItems, List<String> newVariables) {
            boolean structure = !newVariables.equals(variables);
            items = newItems;
            variables = newVariables;
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
            return fixed.length + variables.size();
        }

        @Override
        public String getColumnName(int column) {
            if (column < fixed.length) {
                return fixed[column];
            }
            String name = variables.get(column - fixed.length);
            return name.substring("sat_".length(), name.length() - "_mean".length());
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return column <= 1 ? Integer.class : String.class;
        }

        @Override
        public Object getValueAt(int row, int column) {
            MatchupResults.Item item = items.get(row);
            Map<String, String> chosen = item.chosen();
            return switch (column) {
                case 0 -> item.task().row() + 1;
                case 1 -> item.observation() == null ? null : item.observation().lineNumber();
                case 2 -> item.observation() == null ? "" : Text.time(item.observation().time());
                case 3 -> MatchupCollections.byId(item.task().collection()).map(MatchupCollections.Entry::shortName)
                        .orElse(item.task().collection());
                case 4 -> item.outcome().words();
                case 5 -> item.reason() == null ? "" : item.reason();
                case 6 -> chosen == null ? "" : chosen.getOrDefault("granule", "");
                case 7 -> chosen == null ? "" : chosen.getOrDefault("tdiff", "");
                case 8 -> chosen == null ? "" : chosen.getOrDefault("rrsaot_cv", "");
                default -> chosen == null ? "" : chosen.getOrDefault(variables.get(column - fixed.length), "");
            };
        }
    }

    /** Shows a rejection's full reasons as the tooltip. */
    private final class ReasonRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean selected, boolean focus,
                                                       int row, int column) {
            Component c = super.getTableCellRendererComponent(t, value, selected, focus, row, column);
            String tip = null;
            if (t.convertColumnIndexToModel(column) == 5) {
                MatchupResults.Item item = model.item(t.convertRowIndexToModel(row));
                tip = item.outcome() == MatchupResults.Outcome.REJECTED
                        ? MatchupResults.reasonInFull(item.chosen().get("exclusion_levels")) : item.reason();
            }
            setToolTipText(tip == null || tip.isEmpty() ? null : tip);
            return c;
        }
    }
}
