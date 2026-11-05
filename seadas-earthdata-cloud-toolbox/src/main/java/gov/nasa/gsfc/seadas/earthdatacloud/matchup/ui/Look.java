package gov.nasa.gsfc.seadas.earthdatacloud.matchup.ui;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
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
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

/**
 * The matchup window's look, from the design mockup: its colours, cards, pills
 * and table style. Buttons, checkboxes and other controls stay SeaDAS's own.
 */
final class Look {

    static final Color TEXT = new Color(0x1B2430);
    static final Color MUTED = new Color(0x4F5B67);
    static final Color BLUE = new Color(0x1E5FA8);
    static final Color BLUE_DARK = new Color(0x164A85);
    static final Color BLUE_PALE = new Color(0xDCE7F4);
    static final Color BLUE_PALER = new Color(0xE6EDF5);
    static final Color ORANGE = new Color(0xC26A1A);
    static final Color ORANGE_TEXT = new Color(0x8A4A10);
    static final Color ORANGE_PALE = new Color(0xFFF1DE);
    static final Color RED_TEXT = new Color(0x9B1C12);
    static final Color RED_PALE = new Color(0xFDE8E6);
    static final Color GREY_PALE = new Color(0xEEF1F4);
    static final Color TRACK = new Color(0xE4E8EC);
    static final Color BORDER = new Color(0xC9D1D9);
    static final Color BORDER_DARK = new Color(0x9AA5B1);
    static final Color MAIN = new Color(0xFBFCFD);
    static final Color NAV = new Color(0xF4F6F8);
    static final Color NAV_LINE = new Color(0xD3D9DF);
    static final Color FOOTER = new Color(0xE4E8EC);
    static final Color FOOTER_LINE = new Color(0xC3CBD3);
    static final Color SKIPPED_ROW = new Color(0xFBF6EF);
    static final Color SKIPPED_TEXT = new Color(0x5B6672);
    static final Color RULES = new Color(0xF6F8FA);

    private Look() {
    }

    static Font base() {
        Font font = UIManager.getFont("Label.font");
        return font != null ? font.deriveFont(Font.PLAIN) : new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    }

    static Font font(float delta, int style) {
        Font base = base();
        return base.deriveFont(style, base.getSize2D() + delta);
    }

    static Font mono() {
        return new Font(Font.MONOSPACED, Font.PLAIN, base().getSize());
    }

    // ------------------------------------------------------------ text

    /** A step's heading, with the line under it. */
    static JPanel heading(String title, JLabel subtitle) {
        JPanel panel = clear(new BorderLayout(0, 3));
        JLabel label = new JLabel(title);
        label.setFont(font(7, Font.BOLD));
        label.setForeground(TEXT);
        panel.add(label, BorderLayout.NORTH);
        if (subtitle != null) {
            panel.add(subtitle, BorderLayout.CENTER);
        }
        return panel;
    }

    static Wrap muted(String text) {
        Wrap label = new Wrap(text);
        label.setForeground(MUTED);
        return label;
    }

    static Wrap small(String text) {
        Wrap label = muted(text);
        label.setFont(font(-1, Font.PLAIN));
        return label;
    }

    static JLabel cardTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(font(1, Font.BOLD));
        label.setForeground(TEXT);
        return label;
    }

    /**
     * A label whose text wraps to the width it is given, as a paragraph does.
     * Plain text is escaped; HTML is used as it is.
     */
    static class Wrap extends JLabel {
        /** The width assumed before the label is laid out. */
        private static final int FIRST_WIDTH = 360;

        Wrap(String text) {
            setVerticalAlignment(TOP);
            setFont(base());
            setText(text);
        }

        @Override
        public void setText(String text) {
            super.setText(text == null || text.startsWith("<html>") ? text
                    : "<html>" + Text.escape(text) + "</html>");
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension line = super.getPreferredSize();
            javax.swing.text.View view = (javax.swing.text.View) getClientProperty(
                    javax.swing.plaf.basic.BasicHTML.propertyKey);
            if (view == null || isPreferredSizeSet()) {
                return line;
            }
            Insets insets = getInsets();
            int width = getWidth() > 0 ? getWidth() : Math.min(line.width, FIRST_WIDTH);
            int inner = Math.max(1, width - insets.left - insets.right);
            view.setSize(inner, 0);
            int height = (int) Math.ceil(view.getPreferredSpan(javax.swing.text.View.Y_AXIS));
            return new Dimension(Math.min(line.width, width), height + insets.top + insets.bottom);
        }

        @Override
        public Dimension getMinimumSize() {
            return new Dimension(10, getPreferredSize().height);
        }

        /** As wide as the layout allows, as tall as the text then needs. */
        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }

        @Override
        public void setBounds(int x, int y, int width, int height) {
            int before = getWidth();
            super.setBounds(x, y, width, height);
            if (before != width) {
                javax.swing.SwingUtilities.invokeLater(this::revalidate);
            }
        }
    }

    /** A panel that takes its scroll pane's width, so rows inside it wrap rather than scroll sideways. */
    static final class Column extends JPanel implements javax.swing.Scrollable {
        Column(java.awt.LayoutManager layout) {
            super(layout);
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(java.awt.Rectangle visible, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(java.awt.Rectangle visible, int orientation, int direction) {
            return visible.height;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    static String hex(Color color) {
        return String.format("#%06X", color.getRGB() & 0xFFFFFF);
    }

    // ------------------------------------------------------------ panels

    /** A see-through panel. */
    static JPanel clear(java.awt.LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(false);
        return panel;
    }

    /** A row of components, left to right. */
    static JPanel row(int gap, Component... components) {
        JPanel panel = clear(new FlowLayout(FlowLayout.LEFT, gap, 0));
        ((FlowLayout) panel.getLayout()).setAlignOnBaseline(true);
        // FlowLayout puts a gap before the first component too; take it back so rows line up.
        panel.setBorder(BorderFactory.createEmptyBorder(0, -gap, 0, 0));
        for (Component component : components) {
            panel.add(component);
        }
        return panel;
    }

    /** A white rounded card. */
    static final class Card extends JPanel {
        private Color fill = Color.WHITE;
        private Color line = BORDER;

        Card(java.awt.LayoutManager layout) {
            super(layout);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        }

        /** In a vertical box: the box's width, but only its own height. */
        @Override
        public java.awt.Dimension getMaximumSize() {
            return new java.awt.Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }

        Card colors(Color newFill, Color newLine) {
            fill = newFill;
            line = newLine;
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            RoundRectangle2D shape = new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 10, 10);
            g2.setColor(fill);
            g2.fill(shape);
            g2.setColor(line);
            g2.draw(shape);
            g2.dispose();
        }
    }

    /** A scroll pane drawn as a white card with a thin border. */
    static JScrollPane scroll(JComponent view) {
        JScrollPane scroll = new JScrollPane(view);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));
        scroll.getViewport().setBackground(Color.WHITE);
        return scroll;
    }

    // ------------------------------------------------------------ pills

    /** Text on a rounded patch of colour: a state, a count, a file. */
    static class Pill extends JLabel {
        private Color fill = GREY_PALE;
        private Color line;
        private boolean dashed;
        private final int arc;

        Pill(String text) {
            this(text, 999);
        }

        Pill(String text, int arc) {
            super(text);
            this.arc = arc;
            setOpaque(false);
            setFont(font(-1, Font.BOLD));
            setBorder(BorderFactory.createEmptyBorder(3, 9, 3, 9));
        }

        Pill colors(Color newFill, Color text) {
            fill = newFill;
            setForeground(text);
            line = null;
            dashed = false;
            return this;
        }

        Pill line(Color newLine, boolean dash) {
            line = newLine;
            dashed = dash;
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            Dimension size = getPreferredSize();
            int h = Math.min(getHeight(), size.height);
            int y = (getHeight() - h) / 2;
            int w = Math.min(getWidth(), size.width);
            float round = Math.min(arc, h);
            g2.setColor(fill);
            g2.fillRoundRect(0, y, w - 1, h - 1, (int) round, (int) round);
            if (line != null) {
                g2.setColor(line);
                if (dashed) {
                    g2.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1, new float[]{3, 2},
                            0));
                }
                g2.drawRoundRect(0, y, w - 1, h - 1, (int) round, (int) round);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** A pill's colours by kind of state. */
    enum Tone {
        BLUE(BLUE_PALER, BLUE_DARK, null, false),
        STRONG_BLUE(BLUE_PALE, BLUE_DARK, null, false),
        ORANGE(ORANGE_PALE, ORANGE_TEXT, null, false),
        RED(RED_PALE, RED_TEXT, null, false),
        GREY(GREY_PALE, MUTED, null, false),
        DASHED(NAV, SKIPPED_TEXT, BORDER_DARK, true);

        final Color fill;
        final Color text;
        final Color line;
        final boolean dashed;

        Tone(Color fill, Color text, Color line, boolean dashed) {
            this.fill = fill;
            this.text = text;
            this.line = line;
            this.dashed = dashed;
        }

        Pill apply(Pill pill) {
            pill.colors(fill, text);
            if (line != null) {
                pill.line(line, dashed);
            }
            return pill;
        }
    }

    // ------------------------------------------------------------ bars

    /** A rounded bar filled in segments, each a fraction of the whole. */
    static final class Bar extends JComponent {
        private final int height;
        private List<Double> fractions = List.of();
        private List<Color> colors = List.of();

        Bar(int height) {
            this.height = height;
            setPreferredSize(new Dimension(100, height));
            setMinimumSize(new Dimension(20, height));
        }

        void set(List<Double> newFractions, List<Color> newColors) {
            fractions = newFractions;
            colors = newColors;
            repaint();
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, height);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            int y = (getHeight() - height) / 2;
            RoundRectangle2D track = new RoundRectangle2D.Float(0, y, getWidth(), height, height, height);
            g2.setColor(TRACK);
            g2.fill(track);
            g2.clip(track);
            double x = 0;
            for (int i = 0; i < fractions.size(); i++) {
                double w = Math.max(0, Math.min(1, fractions.get(i))) * getWidth();
                g2.setColor(colors.get(i));
                g2.fillRect((int) Math.round(x), y, (int) Math.round(x + w) - (int) Math.round(x), height);
                x += w;
            }
            g2.dispose();
        }
    }

    // ------------------------------------------------------------ tables

    /** The design's table: light header, horizontal lines only, roomy rows. */
    static void style(JTable table) {
        table.setRowHeight(Math.max(26, table.getFontMetrics(base()).getHeight() + 12));
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(TRACK);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFillsViewportHeight(true);
        table.setBackground(Color.WHITE);
        table.setForeground(TEXT);
        table.setSelectionBackground(BLUE_PALE);
        table.setSelectionForeground(TEXT);
        table.getTableHeader().setReorderingAllowed(false);
        TableCellRenderer header = table.getTableHeader().getDefaultRenderer();
        table.getTableHeader().setDefaultRenderer((t, value, selected, focus, row, column) -> {
            Component c = header.getTableCellRendererComponent(t, value, selected, focus, row, column);
            if (c instanceof JLabel label) {
                JLabel plain = new JLabel(label.getText(), label.getIcon(), SwingConstants.LEFT);
                plain.setOpaque(true);
                plain.setBackground(GREY_PALE);
                plain.setForeground(new Color(0x3D4955));
                plain.setFont(base().deriveFont(Font.BOLD));
                plain.setHorizontalTextPosition(SwingConstants.LEFT);
                plain.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                        BorderFactory.createEmptyBorder(6, 10, 6, 10)));
                return plain;
            }
            return c;
        });
        Cell text = new Cell(false);
        table.setDefaultRenderer(Object.class, text);
        table.setDefaultRenderer(String.class, text);
        table.setDefaultRenderer(Integer.class, new Cell(true));
        table.setDefaultRenderer(Double.class, new Cell(true));
    }

    /** A table cell with the design's padding; numbers in a fixed-width font. */
    static class Cell extends DefaultTableCellRenderer {
        static final Border PADDING = BorderFactory.createEmptyBorder(0, 10, 0, 10);
        private final boolean number;

        Cell(boolean number) {
            this.number = number;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus,
                                                       int row, int column) {
            super.getTableCellRendererComponent(table, value, selected, false, row, column);
            setBorder(PADDING);
            setHorizontalAlignment(LEFT);
            setFont(number ? mono() : base());
            if (value instanceof Double d) {
                setText(String.format(java.util.Locale.ROOT, "%.4f", d));
            }
            if (!selected) {
                setBackground(Color.WHITE);
                setForeground(TEXT);
            }
            return this;
        }
    }

    /** A table cell showing its text as a pill, coloured by {@link #tone}. */
    abstract static class PillCell implements TableCellRenderer {
        private final JPanel holder = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        private final Pill pill = new Pill("");

        PillCell() {
            holder.add(pill);
        }

        abstract Tone tone(Object value, int modelRow);

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus,
                                                       int row, int column) {
            String text = value == null ? "" : value.toString();
            pill.setText(text);
            pill.setVisible(!text.isEmpty());
            tone(value, table.convertRowIndexToModel(row)).apply(pill);
            holder.setBackground(selected ? table.getSelectionBackground() : Color.WHITE);
            int top = Math.max(0, (table.getRowHeight(row) - pill.getPreferredSize().height) / 2 - 1);
            holder.setBorder(BorderFactory.createEmptyBorder(top, 0, 0, 0));
            holder.setToolTipText(null);
            return holder;
        }
    }

    // ------------------------------------------------------------ painting

    static Graphics2D smooth(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return g2;
    }

    /** Text width in a font. */
    static int width(Component c, Font font, String text) {
        FontMetrics metrics = c.getFontMetrics(font);
        return metrics.stringWidth(text);
    }

    static Insets none() {
        return new Insets(0, 0, 0, 0);
    }
}
