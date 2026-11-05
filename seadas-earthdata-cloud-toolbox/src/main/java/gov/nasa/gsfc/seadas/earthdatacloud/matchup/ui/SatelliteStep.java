package gov.nasa.gsfc.seadas.earthdatacloud.matchup.ui;

import gov.nasa.gsfc.seadas.earthdatacloud.matchup.CoverageChecker;
import gov.nasa.gsfc.seadas.earthdatacloud.matchup.HarmonyEnvironment;
import gov.nasa.gsfc.seadas.earthdatacloud.matchup.HarmonyException;
import gov.nasa.gsfc.seadas.earthdatacloud.matchup.HttpTransport;
import gov.nasa.gsfc.seadas.earthdatacloud.matchup.MatchupCollections;
import gov.nasa.gsfc.seadas.earthdatacloud.matchup.MatchupProduct;
import gov.nasa.gsfc.seadas.earthdatacloud.matchup.MatchupRun;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
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
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/** Step 2: products, time window, variables, and the coverage check. */
final class SatelliteStep extends Step {

    private final HarmonyEnvironment environment;
    /** Rows of the product list: one per sensor and product, with its standard and near-real-time versions. */
    private final List<ProductRow> rows = new ArrayList<>();
    private final JCheckBox nrt = new JCheckBox("Use the near-real-time versions");
    private final JSpinner window = new JSpinner(new SpinnerNumberModel(3.0, 0.25, 3.0, 0.25));
    private final JRadioButton allVariables = new JRadioButton("All variables", true);
    private final JRadioButton someVariables = new JRadioButton("Choose…");
    private final JButton changeVariables = new JButton("Change…");
    private final JLabel variablesLabel = Look.small("");
    private final JLabel timeRule = new JLabel();
    private final JButton check = new JButton("Check coverage");
    private final JProgressBar checkProgress = new JProgressBar();
    private final Look.Wrap coverageLabel = new Look.Wrap(" ");
    private final JPanel coverageBars = Look.clear(new GridBagLayout());
    private final Look.Wrap totalLabel = new Look.Wrap(" ");
    private final Look.Wrap lockedLabel = new Look.Wrap("");
    private final AtomicBoolean stopCheck = new AtomicBoolean();
    private SwingWorker<?, ?> checking;
    private boolean updating;

    SatelliteStep(MatchupSession session) {
        super(session);
        environment = session.options().environment();

        lockedLabel.setForeground(Look.ORANGE_TEXT);
        JPanel north = Look.clear(new BorderLayout(0, 8));
        north.add(Look.heading("Satellite data", Look.muted("Earthdata Archive Level-2 products to match against. "
                + "Each observation becomes one request per product.")), BorderLayout.NORTH);
        north.add(lockedLabel, BorderLayout.SOUTH);
        add(north, BorderLayout.NORTH);

        JPanel columns = Look.clear(new GridLayout(1, 2, 14, 0));
        columns.add(productsCard());
        JPanel right = Look.clear(null);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.add(timeCard());
        right.add(Box.createVerticalStrut(14));
        right.add(rulesCard());
        right.add(Box.createVerticalStrut(14));
        right.add(coverageCard());
        right.add(Box.createVerticalGlue());
        columns.add(right);
        add(columns, BorderLayout.CENTER);

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
        changeVariables.addActionListener(e -> chooseVariables());
        nrt.addActionListener(e -> {
            if (!updating) {
                switchVersions(nrt.isSelected());
            }
        });
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

    // ------------------------------------------------------------ layout

    private JComponent productsCard() {
        Map<String, List<MatchupCollections.Entry>> byRow = new LinkedHashMap<>();
        for (MatchupCollections.Entry entry : MatchupCollections.in(environment)) {
            byRow.computeIfAbsent(entry.sensor() + "|" + entry.product(), k -> new ArrayList<>()).add(entry);
        }
        JPanel list = Look.clear(null);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        Look.Column holder = new Look.Column(new BorderLayout());
        holder.add(list, BorderLayout.NORTH);
        list.add(left(Look.cardTitle("Products")));
        String group = null;
        for (List<MatchupCollections.Entry> versions : byRow.values()) {
            String rowGroup = group(versions.get(0));
            if (!rowGroup.equals(group)) {
                group = rowGroup;
                JLabel heading = Look.small(group.toUpperCase(Locale.ROOT));
                heading.setFont(Look.font(-1, Font.BOLD));
                heading.setBorder(BorderFactory.createEmptyBorder(12, 0, 3, 0));
                list.add(left(heading));
            }
            ProductRow row = new ProductRow(versions);
            rows.add(row);
            list.add(left(row.panel));
        }
        JPanel toggle = Look.clear(new BorderLayout());
        toggle.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(10, 0, 0, 0),
                BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Look.TRACK),
                        BorderFactory.createEmptyBorder(8, 0, 0, 0))));
        nrt.setOpaque(false);
        nrt.setToolTipText("Near-real-time products come out within hours; the standard ones are reprocessed "
                + "later with better ancillary data.");
        toggle.add(nrt, BorderLayout.WEST);
        list.add(left(toggle));

        Look.Card card = new Look.Card(new BorderLayout());
        JScrollPane scroll = new JScrollPane(holder, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private static String group(MatchupCollections.Entry entry) {
        if (entry.product().startsWith("SST")) {
            return "Sea surface temperature";
        }
        return entry.sensor().startsWith("PACE") ? "PACE" : "Ocean color";
    }

    private static String rowName(MatchupCollections.Entry entry) {
        if (entry.product().equals("ocean color")) {
            return entry.sensor();
        }
        if (entry.product().equals("AOP")) {
            return entry.sensor() + ", apparent optical properties";
        }
        return entry.sensor() + " " + entry.product();
    }

    private JComponent timeCard() {
        Look.Card card = new Look.Card(new BorderLayout(0, 10));
        card.add(Look.cardTitle("Time and variables"), BorderLayout.NORTH);
        window.setEditor(new JSpinner.NumberEditor(window, "0.0#"));
        ((JSpinner.DefaultEditor) window.getEditor()).getTextField().setColumns(4);
        ButtonGroup group = new ButtonGroup();
        group.add(allVariables);
        group.add(someVariables);
        allVariables.setOpaque(false);
        someVariables.setOpaque(false);
        JPanel body = Look.clear(new BorderLayout(0, 8));
        body.add(Look.row(8, new JLabel("Time window ±"), window, new JLabel("hours"),
                Look.small("at most 3 h")), BorderLayout.NORTH);
        body.add(Look.row(12, allVariables, someVariables, changeVariables, variablesLabel), BorderLayout.CENTER);
        card.add(body, BorderLayout.CENTER);
        return fullWidth(card);
    }

    private JComponent rulesCard() {
        Look.Card card = new Look.Card(new BorderLayout(0, 8)).colors(Look.RULES, Look.BORDER);
        JLabel title = Look.cardTitle("Matchup rules");
        card.add(Look.row(8, title, Look.small("fixed in the cloud service, as in mk_matchup.py")),
                BorderLayout.NORTH);
        JPanel grid = Look.clear(new GridLayout(0, 2, 18, 6));
        grid.add(rule("Pixel box", "5 × 5"));
        grid.add(rule("Valid pixels", "≥ 50%"));
        grid.add(timeRule);
        grid.add(rule("Coefficient of variation", "≤ 0.15"));
        card.add(grid, BorderLayout.CENTER);
        card.add(Look.muted("Flags: the script’s defaults for each product (SST: LAND, NAVFAIL, NAVWARN)"),
                BorderLayout.SOUTH);
        return fullWidth(card);
    }

    private static JLabel rule(String name, String value) {
        return new JLabel("<html>" + name + " <b><font face='monospaced'>" + value + "</font></b></html>");
    }

    private JComponent coverageCard() {
        Look.Card card = new Look.Card(new BorderLayout(0, 10)).colors(Color.WHITE, Look.BLUE);
        JPanel head = Look.clear(new BorderLayout());
        head.add(Look.cardTitle("Coverage"), BorderLayout.WEST);
        head.add(check, BorderLayout.EAST);
        card.add(head, BorderLayout.NORTH);
        JPanel body = Look.clear(null);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        checkProgress.setVisible(false);
        checkProgress.setStringPainted(true);
        for (JComponent c : List.of(checkProgress, coverageLabel, coverageBars, totalLabel)) {
            c.setAlignmentX(LEFT_ALIGNMENT);
            body.add(c);
            body.add(Box.createVerticalStrut(10));
        }
        card.add(body, BorderLayout.CENTER);
        return fullWidth(card);
    }

    private static JComponent left(JComponent c) {
        c.setAlignmentX(LEFT_ALIGNMENT);
        return c;
    }

    private static JComponent fullWidth(JComponent c) {
        c.setAlignmentX(LEFT_ALIGNMENT);
        return c;
    }

    // ------------------------------------------------------------ step

    @Override
    String title() {
        return "Satellite data";
    }

    @Override
    String help() {
        return "Tick the products to match against. A run takes one product per sensor, because the output names "
                + "its columns after the sensor; make a second run for another product of the same sensor.\n\n"
                + "The time window is how far from the observation a granule may be (at most 3 hours, the matchup's "
                + "limit). Variables: all, or only those you choose; the time difference, CV and pixel counts are "
                + "always reported.\n\n"
                + "Check coverage asks CMR which observations have a granule in their box and time window. Those "
                + "without one aren't sent, which saves Harmony jobs.";
    }

    @Override
    String navNote() {
        if (session.file() == null) {
            return null;
        }
        return Text.plural(session.usableObservations(), "observation", "observations") + " from "
                + session.seabass().getFileName();
    }

    @Override
    String nextLabel() {
        MatchupRun run = session.run();
        if (session.started() || session.chosen().isEmpty()) {
            return "Next ›";
        }
        if (session.coverage() != null && run != null) {
            return "Run " + Text.plural(run.counts().get(MatchupRun.State.PENDING), "request", "requests") + " ›";
        }
        return "Run up to " + Text.plural(session.usableObservations() * session.chosen().size(), "request",
                "requests") + " ›";
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

    private void refresh() {
        updating = true;
        try {
            boolean locked = session.started();
            Set<String> chosen = new LinkedHashSet<>();
            session.chosen().forEach(e -> chosen.add(e.shortName()));
            if (!session.chosen().isEmpty()) {
                nrt.setSelected(session.chosen().stream().anyMatch(e -> e.nrt() && rowOf(e).versions.size() > 1));
            }
            for (ProductRow row : rows) {
                row.show(nrt.isSelected(), chosen, locked);
            }
            nrt.setEnabled(!locked);

            window.setValue(session.halfWindow().toSeconds() / 3600.0);
            timeRule.setText(rule("Time difference", "≤ " + hours(session.halfWindow())).getText());
            boolean some = !session.variables().isEmpty();
            allVariables.setSelected(!some);
            someVariables.setSelected(some);
            variablesLabel.setText(some ? summarise(session.variables()) : "");
            variablesLabel.setToolTipText(some ? String.join(", ", session.variables()) : null);
            changeVariables.setVisible(some);

            lockedLabel.setText(locked ? "<html>This run has started, so its products, window and variables are "
                    + "fixed. To change them, open the SeaBASS file again and start a new run.</html>" : "");
            lockedLabel.setVisible(locked);
            window.setEnabled(!locked);
            allVariables.setEnabled(!locked);
            someVariables.setEnabled(!locked);
            changeVariables.setEnabled(!locked && !session.chosen().isEmpty());
            check.setEnabled(checking != null || (!locked && !session.chosen().isEmpty() && session.file() != null));
            check.setText(checking != null ? "Stop" : session.coverage() != null ? "Check coverage again"
                    : "Check coverage");
            showCoverage();
        } finally {
            updating = false;
        }
    }

    private static String hours(Duration window) {
        double h = window.toSeconds() / 3600.0;
        return (h == Math.rint(h) ? String.valueOf((long) h) : String.format(Locale.ROOT, "%.2f", h)
                .replaceAll("0$", "")) + " h";
    }

    private static String summarise(List<String> names) {
        List<String> shortNames = names.stream().map(n -> n.substring(n.lastIndexOf('/') + 1)).toList();
        return shortNames.size() <= 3 ? String.join(", ", shortNames)
                : String.join(", ", shortNames.subList(0, 2)) + " and " + (shortNames.size() - 2) + " more";
    }

    private ProductRow rowOf(MatchupCollections.Entry entry) {
        return rows.stream().filter(r -> r.versions.contains(entry)).findFirst().orElseThrow();
    }

    /** Ticks or clears a product, keeping one product per sensor. */
    private void toggle(ProductRow row, boolean on) {
        MatchupCollections.Entry entry = row.shown;
        List<MatchupCollections.Entry> chosen = new ArrayList<>(session.chosen());
        chosen.removeIf(row.versions::contains);
        if (on) {
            for (MatchupCollections.Entry other : chosen) {
                if (other.mission().equals(entry.mission())) {
                    JOptionPane.showMessageDialog(this, Text.wrap("The output names its columns after the sensor ("
                                    + entry.columnPrefix() + "_…), so a run can have one " + entry.sensor()
                                    + " product. " + rowName(other) + " is already ticked; clear it first, or make a "
                                    + "second run for " + rowName(entry) + "."),
                            "One product per sensor", JOptionPane.INFORMATION_MESSAGE);
                    refresh();
                    return;
                }
            }
            chosen.add(entry);
        }
        choose(chosen);
    }

    /** Swaps each ticked product for its other version, where there is one. */
    private void switchVersions(boolean nearRealTime) {
        List<MatchupCollections.Entry> chosen = new ArrayList<>();
        for (MatchupCollections.Entry entry : session.chosen()) {
            chosen.add(rowOf(entry).version(nearRealTime));
        }
        if (chosen.equals(session.chosen())) {
            refresh();
        } else {
            choose(chosen);
        }
    }

    /** In list order, which is the order of the output's columns. */
    private void choose(List<MatchupCollections.Entry> chosen) {
        List<MatchupCollections.Entry> ordered = new ArrayList<>();
        for (ProductRow row : rows) {
            for (MatchupCollections.Entry version : row.versions) {
                if (chosen.contains(version)) {
                    ordered.add(version);
                }
            }
        }
        session.choose(ordered);
    }

    private void showCoverage() {
        MatchupRun.Coverage coverage = session.coverage();
        MatchupRun run = session.run();
        long usable = session.usableObservations();
        List<MatchupCollections.Entry> chosen = session.chosen();
        coverageBars.removeAll();
        if (chosen.isEmpty()) {
            coverageLabel.setText("Tick at least one product.");
            totalLabel.setText(" ");
            coverageBars.setVisible(false);
            return;
        }
        coverageBars.setVisible(true);
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.insets = new Insets(0, 0, 8, 10);
        c.anchor = GridBagConstraints.WEST;
        for (MatchupCollections.Entry entry : chosen) {
            long requests = usable;
            if (coverage != null && run != null) {
                String id = entry.id(environment);
                requests = run.tasks().stream().filter(t -> t.collection().equals(id)
                        && t.state() != MatchupRun.State.SKIPPED).count();
            }
            JLabel name = new JLabel(shortLabel(entry));
            name.setToolTipText(entry.label());
            c.gridx = 0;
            c.weightx = 0;
            c.fill = GridBagConstraints.NONE;
            coverageBars.add(name, c);
            Look.Bar bar = new Look.Bar(10);
            bar.set(List.of(usable == 0 ? 0 : coverage == null ? 0 : (double) requests / usable),
                    List.of(Look.BLUE));
            c.gridx = 1;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            coverageBars.add(bar, c);
            JLabel count = new JLabel((coverage == null ? "up to " : "") + Text.plural(requests, "request",
                    "requests"));
            count.setFont(Look.mono());
            c.gridx = 2;
            c.weightx = 0;
            c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.EAST;
            coverageBars.add(count, c);
            c.anchor = GridBagConstraints.WEST;
            c.gridy++;
        }
        coverageBars.revalidate();
        coverageBars.repaint();

        int inFlight = session.options().maxInFlight();
        if (coverage == null) {
            coverageLabel.setText("<html>Not checked yet. The check asks CMR which observations have an overpass; "
                    + "the others aren’t sent.</html>");
            totalLabel.setText("<html><b>Up to " + Text.plural(usable * chosen.size(), "request", "requests")
                    + "</b> · about " + inFlight + " at a time</html>");
            return;
        }
        StringBuilder text = new StringBuilder("<html><b>").append(coverage.withOverpass()).append("</b> of ")
                .append(coverage.observations()).append(coverage.observations() == 1 ? " observation" : " observations")
                .append(" have at least one overpass; <b>").append(coverage.noOverpass())
                .append("</b> have none and won’t be sent.");
        if (coverage.unknown() > 0) {
            text.append(" ").append(coverage.unknown()).append(" couldn’t be checked and will be sent anyway.");
        }
        if (coverage.cancelled()) {
            text.append(" (Check stopped.)");
        }
        if (!coverage.errors().isEmpty()) {
            text.append("<br><font color='").append(Look.hex(Look.ORANGE_TEXT)).append("'>")
                    .append(Text.escape(coverage.errors().get(0))).append("</font>");
        }
        coverageLabel.setText(text.append("</html>").toString());
        long requests = run == null ? coverage.requests()
                : run.tasks().stream().filter(t -> t.state() != MatchupRun.State.SKIPPED).count();
        totalLabel.setText("<html><b>" + Text.plural(requests, "request", "requests") + "</b> in all · about "
                + inFlight + " at a time</html>");
    }

    /** {@code MODIS Aqua OC}, {@code PACE OCI AOP}, {@code VIIRS SNPP SST3}: for narrow places. */
    static String shortLabel(MatchupCollections.Entry entry) {
        String product = entry.product().equals("ocean color") ? "OC" : entry.product();
        return entry.sensor() + " " + product + (entry.nrt() ? " NRT" : "");
    }

    void checkCoverage() {
        MatchupRun run;
        try {
            run = session.ensureRun();
        } catch (Exception e) {
            Background.showError(this, "Can't start the run", e);
            return;
        }
        stopCheck.set(false);
        checkProgress.setValue(0);
        checkProgress.setString("Asking CMR…");
        checkProgress.setVisible(true);
        checking = Background.run(this, "Coverage check", () -> {
            CoverageChecker checker = CoverageChecker.create(environment, this::token);
            try {
                return run.checkCoverage(checker, stopCheck::get, (done, total) -> SwingUtilities.invokeLater(() -> {
                    checkProgress.setMaximum(total);
                    checkProgress.setValue(done);
                    checkProgress.setString(done + " of " + total + " searches");
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
    private String token() {
        try {
            return session.tokens().token(environment);
        } catch (Exception e) {
            return null;
        }
    }

    private void chooseVariables() {
        List<MatchupCollections.Entry> chosen = session.chosen();
        if (chosen.isEmpty()) {
            refresh();
            return;
        }
        changeVariables.setEnabled(false);
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

    /** One line of the product list: a checkbox, then the short name of the version in use. */
    private final class ProductRow {
        final List<MatchupCollections.Entry> versions;
        final JPanel panel = Look.clear(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));
        final JCheckBox box;
        final JLabel id = new JLabel();
        MatchupCollections.Entry shown;

        ProductRow(List<MatchupCollections.Entry> versions) {
            this.versions = versions;
            box = new JCheckBox(rowName(versions.get(0)));
            box.setOpaque(false);
            id.setFont(Look.mono().deriveFont(Look.base().getSize2D() - 1));
            id.setForeground(Look.MUTED);
            id.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 0));
            panel.add(box);
            panel.add(id);
            box.addActionListener(e -> {
                if (!updating) {
                    toggle(this, box.isSelected());
                }
            });
            shown = version(false);
        }

        /** The version wanted, or the only one this environment has. */
        MatchupCollections.Entry version(boolean nearRealTime) {
            return versions.stream().filter(e -> e.nrt() == nearRealTime).findFirst().orElse(versions.get(0));
        }

        void show(boolean nearRealTime, Set<String> chosen, boolean locked) {
            MatchupCollections.Entry ticked = versions.stream().filter(e -> chosen.contains(e.shortName()))
                    .findFirst().orElse(null);
            shown = ticked != null ? ticked : version(nearRealTime);
            box.setSelected(ticked != null);
            box.setEnabled(!locked);
            id.setText(shown.shortName());
            box.setToolTipText(shown.label());
        }
    }
}
