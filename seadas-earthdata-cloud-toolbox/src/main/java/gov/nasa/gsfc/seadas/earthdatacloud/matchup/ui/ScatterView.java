package gov.nasa.gsfc.seadas.earthdatacloud.matchup.ui;

import gov.nasa.gsfc.seadas.earthdatacloud.matchup.MatchupCollections;
import gov.nasa.gsfc.seadas.earthdatacloud.matchup.MatchupPairs;
import gov.nasa.gsfc.seadas.earthdatacloud.matchup.MatchupResults;
import gov.nasa.gsfc.seadas.earthdatacloud.matchup.SeabassFile;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.WindowConstants;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Satellite against in situ for the results shown: one point per observation
 * and product, coloured by product, with the 1:1 line, the least-squares line
 * and the usual agreement statistics.
 */
final class ScatterView extends JDialog {

    static final String TITLE = "Scatter plot";
    /** Fewest points for the least-squares line, R² and slope to mean anything. */
    static final int FIT_POINTS = 3;

    /** Product colours, in the run's order. */
    private static final Color[] COLORS = {new Color(0x1E5FA8), new Color(0xC26A1A), new Color(0x1A8A7A),
            new Color(0x7A4FB5), new Color(0xB0306A), new Color(0x5B6672)};

    /**
     * @param items the results shown (their matched and rejected rows are plotted)
     * @param what  what they are, e.g. {@code All products}
     */
    ScatterView(Component parent, List<MatchupResults.Item> items, SeabassFile file, String what) {
        super(SwingUtilities.getWindowAncestor(parent), TITLE + " · " + what, ModalityType.MODELESS);
        JButton close = new JButton("Close");
        close.addActionListener(e -> dispose());
        setContentPane(content(items, file, close));
        getRootPane().setDefaultButton(close);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(getOwner());
    }

    /** The variables the results can be plotted for; empty when the button has nothing to show. */
    static List<MatchupPairs.Variable> variables(List<MatchupResults.Item> items, SeabassFile file) {
        if (file == null) {
            return List.of();
        }
        List<MatchupPairs.Variable> plottable = new ArrayList<>();
        for (MatchupPairs.Variable v : MatchupPairs.variables(items, file)) {
            if (!MatchupPairs.points(items, file, v, true).isEmpty()) {
                plottable.add(v);
            }
        }
        return plottable;
    }

    /** The window's content: choices, the plot with its legend, the statistics, and {@code close}. */
    static JPanel content(List<MatchupResults.Item> items, SeabassFile file, JButton close) {
        List<MatchupPairs.Variable> variables = variables(items, file);
        Map<String, Color> colors = new LinkedHashMap<>();
        for (MatchupResults.Item item : items) {
            String product = productLabel(item);
            colors.putIfAbsent(product, COLORS[colors.size() % COLORS.length]);
        }

        JComboBox<MatchupPairs.Variable> variable = new JComboBox<>(variables.toArray(new MatchupPairs.Variable[0]));
        variable.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected,
                                                          boolean focus) {
                String text = value instanceof MatchupPairs.Variable v
                        ? v.satellite() + " against " + v.inSitu() + " in situ" : "";
                return super.getListCellRendererComponent(list, text, index, selected, focus);
            }
        });
        JCheckBox log = new JCheckBox("Log axes");
        JCheckBox rejected = new JCheckBox("Include rejected");
        for (JCheckBox box : List.of(log, rejected)) {
            box.setOpaque(false);
        }
        rejected.setToolTipText("Observations a matchup rule rejected, drawn hollow; not in the statistics");

        Plot plot = new Plot(colors);
        JPanel legend = Look.clear(new FlowLayout(FlowLayout.LEFT, 14, 0));
        JPanel stats = Look.clear(null);
        stats.setLayout(new BoxLayout(stats, BoxLayout.Y_AXIS));

        Runnable update = () -> {
            MatchupPairs.Variable v = (MatchupPairs.Variable) variable.getSelectedItem();
            if (v == null) {
                return;
            }
            List<MatchupPairs.Point> points = MatchupPairs.points(items, file, v, rejected.isSelected());
            List<MatchupPairs.Point> matched = points.stream().filter(MatchupPairs.Point::matched).toList();
            MatchupPairs.Stats s = MatchupPairs.stats(matched, log.isSelected());
            plot.set(points, s, log.isSelected(), v.inSitu() + " in situ" + unit(file, v.inSitu()),
                    v.satellite() + ", satellite box mean");
            legend.removeAll();
            for (Map.Entry<String, Color> e : colors.entrySet()) {
                if (points.stream().anyMatch(p -> productLabel(p.item()).equals(e.getKey()))) {
                    legend.add(key(e.getValue(), true, e.getKey()));
                }
            }
            if (points.stream().anyMatch(p -> !p.matched())) {
                legend.add(key(Look.MUTED, false, "rejected"));
            }
            legend.add(lineKey(Look.BORDER_DARK, true, "1:1"));
            if (fits(s)) {
                legend.add(lineKey(Look.TEXT, false, "least squares"));
            }
            showStats(stats, s, matched.size(), points.size() - matched.size());
            legend.revalidate();
            legend.repaint();
            stats.revalidate();
            stats.repaint();
        };
        variable.addActionListener(e -> {
            MatchupPairs.Variable v = (MatchupPairs.Variable) variable.getSelectedItem();
            if (v != null) {
                log.setSelected(v.logByDefault());
            }
            update.run();
        });
        log.addActionListener(e -> update.run());
        rejected.addActionListener(e -> update.run());

        JPanel top = Look.clear(new BorderLayout());
        top.add(Look.row(10, new JLabel("Variable"), variable, log, rejected), BorderLayout.WEST);

        JPanel center = Look.clear(new BorderLayout(0, 8));
        Look.Card card = new Look.Card(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        card.add(plot, BorderLayout.CENTER);
        center.add(card, BorderLayout.CENTER);
        center.add(legend, BorderLayout.SOUTH);

        Look.Card statsCard = new Look.Card(new BorderLayout(0, 8));
        statsCard.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        statsCard.add(Look.cardTitle("Agreement"), BorderLayout.NORTH);
        statsCard.add(stats, BorderLayout.CENTER);
        JPanel east = Look.clear(new BorderLayout());
        east.add(statsCard, BorderLayout.NORTH);
        east.setPreferredSize(new Dimension(270, 10));

        JPanel body = Look.clear(new BorderLayout(14, 0));
        body.add(center, BorderLayout.CENTER);
        body.add(east, BorderLayout.EAST);

        JPanel south = Look.clear(new BorderLayout(12, 0));
        south.add(Look.small("<html>One point per observation and product: the in-situ value against the "
                + "satellite mean in the box. Statistics use matched points only.</html>"), BorderLayout.CENTER);
        JPanel buttons = Look.clear(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttons.add(close);
        south.add(buttons, BorderLayout.EAST);

        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setBackground(Look.MAIN);
        content.setBorder(BorderFactory.createEmptyBorder(16, 16, 12, 16));
        content.add(top, BorderLayout.NORTH);
        content.add(body, BorderLayout.CENTER);
        content.add(south, BorderLayout.SOUTH);
        content.setPreferredSize(new Dimension(1000, 700));

        if (!variables.isEmpty()) {
            variable.setSelectedIndex(0);
        }
        return content;
    }

    static boolean fits(MatchupPairs.Stats s) {
        return s != null && s.n() >= FIT_POINTS && Double.isFinite(s.slope());
    }

    private static String productLabel(MatchupResults.Item item) {
        return MatchupCollections.byId(item.task().collection()).map(SatelliteStep::shortLabel)
                .orElse(item.task().collection());
    }

    private static String unit(SeabassFile file, String field) {
        int i = file.fieldIndex(field);
        String unit = i >= 0 && i < file.units().size() ? file.units().get(i) : "";
        return unit.isBlank() || unit.equalsIgnoreCase("none") || unit.equalsIgnoreCase("unitless")
                ? "" : " (" + unit + ")";
    }

    private static void showStats(JPanel panel, MatchupPairs.Stats s, int matched, int rejected) {
        panel.removeAll();
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Matched", String.valueOf(matched)});
        if (rejected > 0) {
            rows.add(new String[]{"Rejected", rejected + "  (not used)"});
        }
        if (s == null) {
            rows.add(new String[]{"", "Needs two or more matched points" + (matched >= 2 ? " above 0" : "")});
        } else {
            if (s.log() && s.n() < matched) {
                rows.add(new String[]{"Left out", (matched - s.n()) + "  (≤ 0)"});
            }
            if (fits(s)) {
                rows.add(new String[]{"R²", fixed(s.r2(), 3)});
                rows.add(new String[]{"Slope", fixed(s.slope(), 3)});
                rows.add(new String[]{"Intercept", number(s.intercept())});
            } else {
                rows.add(new String[]{"Fit", "needs " + FIT_POINTS + "+ points"});
            }
            if (s.log()) {
                rows.add(new String[]{"Bias", number(s.bias()) + "  (×" + fixed(Math.pow(10, s.bias()), 2) + ")"});
                rows.add(new String[]{"MAE", number(s.mae()) + "  (×" + fixed(Math.pow(10, s.mae()), 2) + ")"});
            } else {
                rows.add(new String[]{"Bias", number(s.bias())});
                rows.add(new String[]{"MAE", number(s.mae())});
            }
            rows.add(new String[]{"RMSE", number(s.rmse())});
        }
        JPanel grid = Look.clear(new java.awt.GridBagLayout());
        java.awt.GridBagConstraints at = new java.awt.GridBagConstraints();
        at.anchor = java.awt.GridBagConstraints.WEST;
        at.insets = new java.awt.Insets(0, 0, 6, 12);
        at.gridy = 0;
        for (String[] row : rows) {
            JLabel name = new JLabel(row[0]);
            name.setForeground(Look.MUTED);
            JLabel value = new JLabel(row[1]);
            value.setFont(Look.mono());
            value.setForeground(Look.TEXT);
            at.gridx = 0;
            at.weightx = 0;
            grid.add(name, at);
            at.gridx = 1;
            at.weightx = 1;
            grid.add(value, at);
            at.gridy++;
        }
        grid.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(grid);
        if (s != null) {
            panel.add(Box.createVerticalStrut(10));
            JComponent note = Look.small(s.log()
                    ? "<html>On log10 values. Bias and MAE are also given as factors: ×1.20 is 20 % high.</html>"
                    : "<html>Bias is the mean of satellite − in situ.</html>");
            note.setAlignmentX(LEFT_ALIGNMENT);
            panel.add(note);
        }
    }

    static String fixed(double value, int decimals) {
        return Double.isFinite(value) ? String.format(Locale.ROOT, "%." + decimals + "f", value) : "—";
    }

    /** Four significant digits, so Rrs (0.004) and chlorophyll (12.3) both read well. */
    static String number(double value) {
        if (!Double.isFinite(value)) {
            return "—";
        }
        if (value == 0) {
            return "0";
        }
        double magnitude = Math.abs(value);
        if (magnitude >= 1e5 || magnitude < 1e-4) {
            return String.format(Locale.ROOT, "%.3e", value);
        }
        int decimals = Math.max(0, 3 - (int) Math.floor(Math.log10(magnitude)));
        return String.format(Locale.ROOT, "%." + decimals + "f", value);
    }

    private static JLabel key(Color color, boolean filled, String text) {
        JLabel label = new JLabel(text);
        label.setIcon(new javax.swing.Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = Look.smooth(g);
                g2.setColor(color);
                Shape dot = new Ellipse2D.Double(x + 1, y + 1, 9, 9);
                if (filled) {
                    g2.fill(dot);
                } else {
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.draw(dot);
                }
                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return 11;
            }

            @Override
            public int getIconHeight() {
                return 11;
            }
        });
        label.setForeground(Look.TEXT);
        return label;
    }

    private static JLabel lineKey(Color color, boolean dashed, String text) {
        JLabel label = new JLabel(text);
        label.setIcon(new javax.swing.Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = Look.smooth(g);
                g2.setColor(color);
                g2.setStroke(stroke(dashed));
                g2.draw(new Line2D.Double(x, y + 6, x + 22, y + 6));
                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return 22;
            }

            @Override
            public int getIconHeight() {
                return 12;
            }
        });
        label.setForeground(Look.TEXT);
        return label;
    }

    private static BasicStroke stroke(boolean dashed) {
        return dashed ? new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{5, 4}, 0)
                : new BasicStroke(1.4f);
    }

    // ------------------------------------------------------------ the plot

    /** The axes, the 1:1 and fitted lines, and the points; the same range on both axes. */
    static final class Plot extends JComponent {
        private static final int LEFT = 70;
        private static final int RIGHT = 16;
        private static final int TOP = 14;
        private static final int BOTTOM = 52;
        private static final double DOT = 9;

        private final Map<String, Color> colors;
        private List<MatchupPairs.Point> points = List.of();
        private MatchupPairs.Stats stats;
        private boolean log;
        private String xLabel = "";
        private String yLabel = "";
        /** The axis range, in plotted units (log10 when log). */
        private double lo;
        private double hi;

        Plot(Map<String, Color> colors) {
            this.colors = colors;
            setPreferredSize(new Dimension(600, 520));
            setFont(Look.base());
            ToolTipManager.sharedInstance().registerComponent(this);
        }

        void set(List<MatchupPairs.Point> newPoints, MatchupPairs.Stats newStats, boolean newLog, String x, String y) {
            points = newPoints;
            stats = newStats;
            log = newLog;
            xLabel = x;
            yLabel = y;
            double[] range = range(points, log);
            lo = range[0];
            hi = range[1];
            repaint();
        }

        /** Both axes' range, padded: whole decades in log10, else 5 % either side. */
        static double[] range(List<MatchupPairs.Point> points, boolean log) {
            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;
            for (MatchupPairs.Point p : points) {
                for (double v : new double[]{p.inSitu(), p.satellite()}) {
                    if (log && v <= 0) {
                        continue;
                    }
                    double t = log ? Math.log10(v) : v;
                    min = Math.min(min, t);
                    max = Math.max(max, t);
                }
            }
            if (min > max) {
                return log ? new double[]{-1, 1} : new double[]{0, 1};
            }
            if (log) {
                double from = Math.floor(min);
                double to = Math.ceil(max);
                return new double[]{from, to == from ? from + 1 : to};
            }
            double pad = max > min ? (max - min) * 0.05 : Math.max(Math.abs(max) * 0.1, 1e-6);
            return new double[]{min - pad, max + pad};
        }

        private double t(double value) {
            return log ? Math.log10(value) : value;
        }

        private double px(double plotted) {
            return LEFT + (plotted - lo) / (hi - lo) * (getWidth() - LEFT - RIGHT);
        }

        private double py(double plotted) {
            return getHeight() - BOTTOM - (plotted - lo) / (hi - lo) * (getHeight() - TOP - BOTTOM);
        }

        private boolean drawn(MatchupPairs.Point p) {
            return !log || p.inSitu() > 0 && p.satellite() > 0;
        }

        @Override
        public String getToolTipText(MouseEvent e) {
            MatchupPairs.Point nearest = null;
            double best = DOT;
            for (MatchupPairs.Point p : points) {
                if (drawn(p)) {
                    double d = e.getPoint().distance(px(t(p.inSitu())), py(t(p.satellite())));
                    if (d <= best) {
                        best = d;
                        nearest = p;
                    }
                }
            }
            if (nearest == null) {
                return null;
            }
            MatchupResults.Item item = nearest.item();
            return "<html><b>Obs " + (item.task().row() + 1) + "</b> · " + Text.escape(productLabel(item))
                    + (nearest.matched() ? "" : " · rejected") + "<br>in situ " + number(nearest.inSitu())
                    + "<br>satellite " + number(nearest.satellite())
                    + (item.chosen() != null && item.chosen().get("granule") != null
                    ? "<br>" + Text.escape(item.chosen().get("granule")) : "") + "</html>";
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Look.smooth(g);
            int w = getWidth();
            int h = getHeight();
            double x0 = px(lo);
            double x1 = px(hi);
            double y0 = py(lo);
            double y1 = py(hi);
            FontMetrics fm = g2.getFontMetrics(getFont());
            g2.setFont(getFont());

            // Grid and tick labels, the same on both axes.
            for (double[] tick : ticks()) {
                double v = tick[0];
                boolean major = tick[1] > 0;
                double x = px(v);
                double y = py(v);
                g2.setColor(major ? Look.TRACK : new Color(0xF1F3F5));
                g2.draw(new Line2D.Double(x, y0, x, y1));
                g2.draw(new Line2D.Double(x0, y, x1, y));
                if (major) {
                    String text = tickLabel(v);
                    g2.setColor(Look.MUTED);
                    g2.drawString(text, (float) (x - fm.stringWidth(text) / 2.0), (float) (y0 + fm.getAscent() + 6));
                    g2.drawString(text, (float) (x0 - fm.stringWidth(text) - 8), (float) (y + fm.getAscent() / 2.0 - 2));
                }
            }
            g2.setColor(Look.BORDER_DARK);
            g2.draw(new Line2D.Double(x0, y0, x1, y0));
            g2.draw(new Line2D.Double(x0, y0, x0, y1));

            // Axis names.
            g2.setColor(Look.TEXT);
            g2.drawString(xLabel, (float) ((x0 + x1 - fm.stringWidth(xLabel)) / 2), h - 10);
            AffineTransform saved = g2.getTransform();
            g2.rotate(-Math.PI / 2);
            g2.drawString(yLabel, (float) (-(y0 + y1 + fm.stringWidth(yLabel)) / 2), 16f);
            g2.setTransform(saved);

            Shape clip = g2.getClip();
            g2.clipRect((int) x0, (int) y1, (int) (x1 - x0) + 1, (int) (y0 - y1) + 1);
            // 1:1, then the least-squares line (in the plotted units).
            g2.setColor(Look.BORDER_DARK);
            g2.setStroke(stroke(true));
            g2.draw(new Line2D.Double(x0, y0, x1, y1));
            if (fits(stats)) {
                g2.setColor(Look.TEXT);
                g2.setStroke(stroke(false));
                g2.draw(new Line2D.Double(px(lo), py(stats.intercept() + stats.slope() * lo),
                        px(hi), py(stats.intercept() + stats.slope() * hi)));
            }
            // Rejected (hollow) under matched (filled).
            for (boolean matched : new boolean[]{false, true}) {
                for (MatchupPairs.Point p : points) {
                    if (p.matched() != matched || !drawn(p)) {
                        continue;
                    }
                    Color color = colors.getOrDefault(productLabel(p.item()), Look.BLUE);
                    Shape dot = new Ellipse2D.Double(px(t(p.inSitu())) - DOT / 2, py(t(p.satellite())) - DOT / 2,
                            DOT, DOT);
                    if (matched) {
                        g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 200));
                        g2.fill(dot);
                        g2.setColor(Color.WHITE);
                        g2.setStroke(new BasicStroke(1f));
                        g2.draw(dot);
                    } else {
                        g2.setColor(color);
                        g2.setStroke(new BasicStroke(1.5f));
                        g2.draw(dot);
                    }
                }
            }
            g2.setClip(clip);
            if (points.isEmpty()) {
                String text = "No points to show";
                g2.setColor(Look.MUTED);
                g2.drawString(text, (float) ((x0 + x1 - fm.stringWidth(text)) / 2), (float) ((y0 + y1) / 2));
            }
            g2.dispose();
        }

        /** Tick positions in plotted units, with 1 for a labelled tick: decades (and 2, 5) in log, else 1-2-5 steps. */
        private List<double[]> ticks() {
            List<double[]> ticks = new ArrayList<>();
            if (log) {
                for (int d = (int) Math.floor(lo); d <= Math.ceil(hi); d++) {
                    for (int m = 1; m <= 9; m++) {
                        double v = d + Math.log10(m);
                        if (v >= lo - 1e-9 && v <= hi + 1e-9) {
                            ticks.add(new double[]{v, m == 1 ? 1 : 0});
                        }
                    }
                }
                return ticks;
            }
            double step = niceStep((hi - lo) / 6);
            for (double v = Math.ceil(lo / step) * step; v <= hi + step * 1e-9; v += step) {
                ticks.add(new double[]{Math.abs(v) < step * 1e-9 ? 0 : v, 1});
            }
            return ticks;
        }

        static double niceStep(double rough) {
            double power = Math.pow(10, Math.floor(Math.log10(rough)));
            double f = rough / power;
            return (f <= 1 ? 1 : f <= 2 ? 2 : f <= 5 ? 5 : 10) * power;
        }

        private String tickLabel(double plotted) {
            return number(log ? Math.pow(10, plotted) : plotted).replaceAll("(\\.\\d*?)0+$", "$1")
                    .replaceAll("\\.$", "");
        }

        @Override
        public Font getFont() {
            return Look.font(-1, Font.PLAIN);
        }
    }
}
