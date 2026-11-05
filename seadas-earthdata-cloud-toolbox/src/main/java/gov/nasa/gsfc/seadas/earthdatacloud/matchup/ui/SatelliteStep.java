package gov.nasa.gsfc.seadas.earthdatacloud.matchup.ui;

import gov.nasa.gsfc.seadas.earthdatacloud.matchup.CoverageChecker;
import gov.nasa.gsfc.seadas.earthdatacloud.matchup.HarmonyEnvironment;
import gov.nasa.gsfc.seadas.earthdatacloud.matchup.HarmonyException;
import gov.nasa.gsfc.seadas.earthdatacloud.matchup.HttpTransport;
import gov.nasa.gsfc.seadas.earthdatacloud.matchup.MatchupCollections;
import gov.nasa.gsfc.seadas.earthdatacloud.matchup.MatchupProduct;
import gov.nasa.gsfc.seadas.earthdatacloud.matchup.MatchupRun;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/** Step 2: products, time window, variables, and the coverage check. */
final class SatelliteStep extends Step {

    private final ProductTable products;
    private final JTable productTable;
    private final JSpinner window = new JSpinner(new SpinnerNumberModel(3.0, 0.25, 3.0, 0.25));
    private final JRadioButton allVariables = new JRadioButton("all", true);
    private final JRadioButton someVariables = new JRadioButton("chosen:");
    private final JButton chooseVariables = new JButton("Choose…");
    private final JLabel variablesLabel = new JLabel();
    private final JButton check = new JButton("Check coverage");
    private final JProgressBar checkProgress = new JProgressBar();
    private final JLabel coverageLabel = new JLabel(" ");
    private final JLabel lockedLabel = new JLabel();
    private final AtomicBoolean stopCheck = new AtomicBoolean();
    private SwingWorker<?, ?> checking;
    private boolean updating;

    SatelliteStep(MatchupSession session) {
        super(session);
        HarmonyEnvironment environment = session.options().environment();
        products = new ProductTable(MatchupCollections.in(environment));
        productTable = new JTable(products);
        productTable.setRowSelectionAllowed(false);
        productTable.getColumnModel().getColumn(0).setMaxWidth(40);
        productTable.getColumnModel().getColumn(1).setPreferredWidth(110);
        productTable.getColumnModel().getColumn(2).setPreferredWidth(90);
        productTable.getColumnModel().getColumn(3).setPreferredWidth(110);
        productTable.getColumnModel().getColumn(4).setPreferredWidth(170);
        JScrollPane productScroll = new JScrollPane(productTable);
        productScroll.setPreferredSize(new Dimension(560, 200));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints label = new GridBagConstraints();
        label.anchor = GridBagConstraints.NORTHWEST;
        label.insets = new Insets(4, 0, 4, 10);
        GridBagConstraints field = new GridBagConstraints();
        field.gridx = 1;
        field.anchor = GridBagConstraints.WEST;
        field.fill = GridBagConstraints.HORIZONTAL;
        field.weightx = 1;
        field.insets = new Insets(4, 0, 4, 0);

        label.gridy = field.gridy = 0;
        form.add(new JLabel("Products"), label);
        GridBagConstraints table = (GridBagConstraints) field.clone();
        table.fill = GridBagConstraints.BOTH;
        table.weighty = 1;
        form.add(productScroll, table);

        label.gridy = field.gridy = 1;
        form.add(new JLabel("Time window"), label);
        JPanel windowRow = row(new JLabel("±"), window, new JLabel("hours (at most 3, the matchup limit)"));
        window.setEditor(new JSpinner.NumberEditor(window, "0.0#"));
        ((JSpinner.DefaultEditor) window.getEditor()).getTextField().setColumns(4);
        form.add(windowRow, field);

        label.gridy = field.gridy = 2;
        form.add(new JLabel("Variables"), label);
        ButtonGroup group = new ButtonGroup();
        group.add(allVariables);
        group.add(someVariables);
        form.add(row(allVariables, someVariables, chooseVariables, variablesLabel), field);

        label.gridy = field.gridy = 3;
        form.add(new JLabel("Matchup rules"), label);
        form.add(new JLabel("5×5 pixels, ≥ 50% valid, CV ≤ 0.15, within the time window (fixed: mk_matchup.py's)"),
                field);

        label.gridy = field.gridy = 4;
        form.add(new JLabel("Harmony"), label);
        JLabel environmentLabel = new JLabel(EnvironmentNames.name(environment)
                + (environment == HarmonyEnvironment.UAT ? ": test data only. Change it in the Earthdata Cloud options."
                : " (change it in the Earthdata Cloud options)"));
        if (environment == HarmonyEnvironment.UAT) {
            environmentLabel.setForeground(new Color(0xB35900));
        }
        form.add(environmentLabel, field);

        label.gridy = field.gridy = 5;
        form.add(new JLabel(""), label);
        checkProgress.setVisible(false);
        checkProgress.setStringPainted(true);
        form.add(row(check, checkProgress), field);

        label.gridy = field.gridy = 6;
        form.add(new JLabel(""), label);
        form.add(coverageLabel, field);

        lockedLabel.setForeground(new Color(0xB35900));
        lockedLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
        add(lockedLabel, BorderLayout.NORTH);
        add(form, BorderLayout.CENTER);

        window.addChangeListener(e -> {
            if (!updating) {
                session.setHalfWindow(Duration.ofSeconds(Math.round(((Number) window.getValue()).doubleValue() * 3600)));
            }
        });
        allVariables.addActionListener(e -> {
            if (!updating && !session.variables().isEmpty()) {
                session.setVariables(List.of());
            }
        });
        someVariables.addActionListener(e -> {
            if (!updating && session.variables().isEmpty()) {
                chooseVariables();
            }
        });
        chooseVariables.addActionListener(e -> chooseVariables());
        check.addActionListener(e -> {
            if (checking != null) {
                stopCheck.set(true);
            } else {
                checkCoverage();
            }
        });

        session.addListener(this::refresh);
        refresh();
    }

    @Override
    String title() {
        return "Satellite data";
    }

    @Override
    String unavailableReason() {
        return session.file() == null ? "Open a SeaBASS file first" : null;
    }

    @Override
    boolean busy() {
        return checking != null;
    }

    @Override
    void stop() {
        stopCheck.set(true);
    }

    private static JPanel row(JComponent... components) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        for (JComponent component : components) {
            panel.add(component);
        }
        return panel;
    }

    private void refresh() {
        updating = true;
        try {
            products.setChosen(session.chosen());
            window.setValue(session.halfWindow().toSeconds() / 3600.0);
            boolean some = !session.variables().isEmpty();
            allVariables.setSelected(!some);
            someVariables.setSelected(some);
            variablesLabel.setText(some ? summarise(session.variables()) : "");
            variablesLabel.setToolTipText(some ? String.join(", ", session.variables()) : null);

            boolean locked = session.started();
            lockedLabel.setText(locked ? "<html>This run has started, so its products, window and variables are "
                    + "fixed. To change them, open the SeaBASS file again and start a new run.</html>" : "");
            lockedLabel.setVisible(locked);
            productTable.setEnabled(!locked);
            products.locked = locked;
            window.setEnabled(!locked);
            allVariables.setEnabled(!locked);
            someVariables.setEnabled(!locked);
            chooseVariables.setEnabled(!locked && !session.chosen().isEmpty());
            check.setEnabled(checking != null || (!locked && !session.chosen().isEmpty() && session.file() != null));
            check.setText(checking != null ? "Stop" : "Check coverage");
            showCoverage();
        } finally {
            updating = false;
        }
    }

    private static String summarise(List<String> names) {
        List<String> shortNames = names.stream().map(n -> n.substring(n.lastIndexOf('/') + 1)).toList();
        return shortNames.size() <= 4 ? String.join(", ", shortNames)
                : String.join(", ", shortNames.subList(0, 3)) + " and " + (shortNames.size() - 3) + " more";
    }

    private void showCoverage() {
        MatchupRun.Coverage coverage = session.coverage();
        if (coverage == null) {
            int products = session.chosen().size();
            long observations = session.usableObservations();
            coverageLabel.setText(products == 0 ? "Choose at least one product."
                    : "<html>" + Text.plural(observations, "usable observation", "usable observations") + " × "
                    + Text.plural(products, "product", "products") + " = up to " + observations * products
                    + " Harmony jobs. The check finds which observations have an overpass.</html>");
            return;
        }
        StringBuilder text = new StringBuilder("<html>")
                .append(Text.plural(coverage.observations(), "observation", "observations")).append(": ")
                .append(coverage.withOverpass()).append(" with an overpass, ")
                .append(coverage.noOverpass()).append(" none");
        if (coverage.unknown() > 0) {
            text.append(", ").append(coverage.unknown()).append(" not checked");
        }
        text.append(" → <b>").append(Text.plural(coverage.requests(), "Harmony job", "Harmony jobs")).append("</b>");
        MatchupRun run = session.run();
        if (run != null) {
            long none = run.tasks().stream().filter(t -> MatchupRun.NO_OVERPASS.equals(t.message())).count();
            if (none > 0 && session.chosen().size() > 1) {
                text.append("<br>").append(Text.plural(none, "observation–product pair has", "observation–product pairs have"))
                        .append(" no overpass and won't be sent.");
            }
        }
        if (coverage.cancelled()) {
            text.append(" (check stopped)");
        }
        if (!coverage.errors().isEmpty()) {
            text.append("<br>Some searches failed; those observations will be sent anyway: ")
                    .append(Text.escape(coverage.errors().get(0)));
        }
        coverageLabel.setText(text.append("</html>").toString());
    }

    void checkCoverage() {
        MatchupRun run;
        try {
            run = session.ensureRun();
        } catch (Exception e) {
            Background.showError(this, "Can't start the run", e);
            return;
        }
        HarmonyEnvironment environment = session.options().environment();
        stopCheck.set(false);
        checkProgress.setValue(0);
        checkProgress.setString("Asking CMR…");
        checkProgress.setVisible(true);
        checking = Background.run(this, "Coverage check", () -> {
            CoverageChecker checker = CoverageChecker.create(environment, () -> token(environment));
            try {
                return run.checkCoverage(checker, stopCheck::get, (done, total) -> SwingUtilities.invokeLater(() -> {
                    checkProgress.setMaximum(total);
                    checkProgress.setValue(done);
                    checkProgress.setString(done + " of " + total);
                }));
            } catch (HarmonyException e) {
                if (e.unauthorized()) {
                    session.tokens().forget(environment);
                }
                throw e;
            }
        }, coverage -> session.setCoverage(coverage), () -> {
            checking = null;
            checkProgress.setVisible(false);
            refresh();
        });
        refresh();
    }

    /** A token for CMR, or none when the login can't give one (public collections need none). */
    private String token(HarmonyEnvironment environment) {
        try {
            return session.tokens().token(environment);
        } catch (Exception e) {
            return null;
        }
    }

    private void chooseVariables() {
        List<MatchupCollections.Entry> chosen = session.chosen();
        if (chosen.isEmpty()) {
            return;
        }
        HarmonyEnvironment environment = session.options().environment();
        chooseVariables.setEnabled(false);
        Background.run(this, "Reading the products' variables from CMR", () -> {
            List<MatchupProduct> list = new ArrayList<>();
            for (MatchupCollections.Entry entry : chosen) {
                String id = entry.id(environment);
                MatchupProduct product = session.products().get(id);
                if (product == null || product.variables().isEmpty()) {
                    product = MatchupProduct.fetch(HttpTransport.jdk(), environment.cmr(), id);
                    session.addProduct(product);
                }
                list.add(product);
            }
            return list;
        }, this::showVariableChooser, this::refresh);
    }

    /** Lets the user pick from the variables every chosen product has. */
    private void showVariableChooser(List<MatchupProduct> list) {
        Set<String> common = new LinkedHashSet<>(list.get(0).variables());
        for (MatchupProduct product : list.subList(1, list.size())) {
            common.retainAll(product.variables());
        }
        common.removeIf(name -> name.startsWith("navigation_data/") || name.startsWith("scan_line_attributes/")
                || name.startsWith("sensor_band_parameters/") || name.endsWith("/l2_flags"));
        if (common.isEmpty()) {
            JOptionPane.showMessageDialog(this, "The chosen products have no variable in common.", "Variables",
                    JOptionPane.INFORMATION_MESSAGE);
            refresh();
            return;
        }
        JList<String> names = new JList<>(common.toArray(String[]::new));
        names.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        names.setVisibleRowCount(16);
        List<String> current = session.variables();
        List<Integer> selected = new ArrayList<>();
        List<String> all = List.copyOf(common);
        for (int i = 0; i < all.size(); i++) {
            if (current.contains(all.get(i))) {
                selected.add(i);
            }
        }
        names.setSelectedIndices(selected.stream().mapToInt(Integer::intValue).toArray());
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.add(new JLabel("<html>Variables every chosen product has (Ctrl-click for several).<br>"
                + "The time difference, CV and pixel counts are always reported.</html>"), BorderLayout.NORTH);
        panel.add(new JScrollPane(names), BorderLayout.CENTER);
        int answer = JOptionPane.showConfirmDialog(this, panel, "Choose variables", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (answer == JOptionPane.OK_OPTION && !names.getSelectedValuesList().isEmpty()) {
            session.setVariables(names.getSelectedValuesList());
        } else {
            refresh();
        }
    }

    /** The products with a checkbox each. */
    private final class ProductTable extends AbstractTableModel {
        private static final String[] COLUMNS = {"Use", "Sensor", "Product", "Near real time", "Short name"};
        private final List<MatchupCollections.Entry> entries;
        private final Set<String> chosen = new LinkedHashSet<>();
        boolean locked;

        ProductTable(List<MatchupCollections.Entry> entries) {
            this.entries = entries;
        }

        void setChosen(List<MatchupCollections.Entry> list) {
            chosen.clear();
            list.forEach(e -> chosen.add(e.shortName()));
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return entries.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMNS[column];
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return column == 0 ? Boolean.class : String.class;
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return column == 0 && !locked;
        }

        @Override
        public Object getValueAt(int row, int column) {
            MatchupCollections.Entry e = entries.get(row);
            return switch (column) {
                case 0 -> chosen.contains(e.shortName());
                case 1 -> e.sensor();
                case 2 -> e.product();
                case 3 -> e.nrt() ? "yes" : "";
                default -> e.shortName();
            };
        }

        @Override
        public void setValueAt(Object value, int row, int column) {
            MatchupCollections.Entry entry = entries.get(row);
            String name = entry.shortName();
            if (Boolean.TRUE.equals(value)) {
                // The output names columns <instrument>_<platform>_..., so one product per sensor.
                for (MatchupCollections.Entry other : entries) {
                    if (chosen.contains(other.shortName()) && other.mission().equals(entry.mission())) {
                        JOptionPane.showMessageDialog(SatelliteStep.this, Text.wrap("The output names its columns "
                                        + "after the sensor (" + entry.columnPrefix() + "_…), so a run can have one "
                                        + entry.sensor() + " product. " + other.shortName() + " is already chosen; "
                                        + "clear it first, or make a second run for " + name + "."),
                                "One product per sensor", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }
                }
                chosen.add(name);
            } else {
                chosen.remove(name);
            }
            // In table order, which is the order of the output's columns.
            session.choose(entries.stream().filter(e -> chosen.contains(e.shortName())).toList());
        }
    }
}
