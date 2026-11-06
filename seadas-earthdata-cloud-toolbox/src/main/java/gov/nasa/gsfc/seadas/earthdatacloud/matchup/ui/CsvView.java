package gov.nasa.gsfc.seadas.earthdatacloud.matchup.ui;

import gov.nasa.gsfc.seadas.earthdatacloud.matchup.MatchupResults;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The CSV export, before exporting: every granule row the service returned,
 * with all its columns, in a window beside the results.
 */
final class CsvView extends JDialog {

    static final String TITLE = "View CSV";

    /**
     * @param csv   the rows to show, with the export's columns
     * @param total how many rows the export has
     * @param what  what the rows are, e.g. {@code PACE OCI AOP NRT · matched}
     */
    CsvView(Component parent, MatchupResults.Csv csv, int total, String what) {
        super(SwingUtilities.getWindowAncestor(parent), TITLE + " · " + what, ModalityType.MODELESS);
        JButton close = new JButton("Close");
        close.addActionListener(e -> dispose());
        JPanel content = content(csv, total, close);
        setContentPane(content);
        getRootPane().setDefaultButton(close);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setPreferredSize(new Dimension(1100, 560));
        pack();
        Window owner = getOwner();
        setLocationRelativeTo(owner);
    }

    /** The window's content: the table, a note on what it shows, and {@code close}. */
    static JPanel content(MatchupResults.Csv csv, int total, JButton close) {
        Table model = new Table(withoutEmptyColumns(csv));
        JTable table = new JTable(model);
        Look.style(table);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        TableRowSorter<Table> sorter = new TableRowSorter<>(model);
        for (int i = 0; i < model.getColumnCount(); i++) {
            sorter.setComparator(i, NUMBERS_FIRST);
            // Wide enough for the name (bold in the header) and the longest value, within reason.
            java.awt.FontMetrics header = table.getFontMetrics(table.getFont().deriveFont(java.awt.Font.BOLD));
            java.awt.FontMetrics cells = table.getFontMetrics(table.getFont());
            int width = Math.max(header.stringWidth(model.getColumnName(i)), cells.stringWidth(model.widest(i))) + 24;
            width = Math.max(60, Math.min(360, width));
            table.getColumnModel().getColumn(i).setPreferredWidth(width);
        }
        table.setRowSorter(sorter);

        int hidden = csv.header().size() - model.getColumnCount();
        String note = Text.plural(csv.rows().size(), "granule row", "granule rows") + " of " + total
                + (hidden > 0 ? " · " + Text.plural(hidden, "column", "columns") + " empty in these rows not shown"
                : "") + ". Export CSV… writes every row and column.";

        JPanel south = Look.clear(new BorderLayout(12, 0));
        south.add(Look.small(note), BorderLayout.CENTER);
        JPanel buttons = Look.clear(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttons.add(close);
        south.add(buttons, BorderLayout.EAST);

        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setBackground(Look.MAIN);
        content.setBorder(BorderFactory.createEmptyBorder(16, 16, 12, 16));
        content.add(Look.scroll(table), BorderLayout.CENTER);
        content.add(south, BorderLayout.SOUTH);
        return content;
    }

    /** Drops the columns no shown row has a value in (another product's columns, say). */
    static MatchupResults.Csv withoutEmptyColumns(MatchupResults.Csv csv) {
        List<Integer> keep = new ArrayList<>();
        for (int c = 0; c < csv.header().size(); c++) {
            for (List<String> row : csv.rows()) {
                if (!row.get(c).isEmpty()) {
                    keep.add(c);
                    break;
                }
            }
        }
        if (csv.rows().isEmpty()) {
            return csv;
        }
        List<String> header = new ArrayList<>();
        keep.forEach(c -> header.add(csv.header().get(c)));
        List<List<String>> rows = new ArrayList<>();
        for (List<String> row : csv.rows()) {
            List<String> values = new ArrayList<>();
            keep.forEach(c -> values.add(row.get(c)));
            rows.add(values);
        }
        return new MatchupResults.Csv(header, rows);
    }

    /** Sorts numbers by value, before text; empty last. */
    static final Comparator<String> NUMBERS_FIRST = (a, b) -> {
        Double x = number(a);
        Double y = number(b);
        if (x != null && y != null) {
            return Double.compare(x, y);
        }
        if (x != null || y != null) {
            return x != null ? -1 : 1;
        }
        if (a.isEmpty() != b.isEmpty()) {
            return a.isEmpty() ? 1 : -1;
        }
        return a.compareTo(b);
    };

    private static Double number(String s) {
        try {
            return s.isEmpty() ? null : Double.valueOf(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static final class Table extends AbstractTableModel {
        private final MatchupResults.Csv csv;

        Table(MatchupResults.Csv csv) {
            this.csv = csv;
        }

        /** The column's longest value. */
        String widest(int column) {
            String widest = "";
            for (List<String> row : csv.rows()) {
                if (row.get(column).length() > widest.length()) {
                    widest = row.get(column);
                }
            }
            return widest;
        }

        @Override
        public int getRowCount() {
            return csv.rows().size();
        }

        @Override
        public int getColumnCount() {
            return csv.header().size();
        }

        @Override
        public String getColumnName(int column) {
            return csv.header().get(column);
        }

        @Override
        public Object getValueAt(int row, int column) {
            return csv.rows().get(row).get(column);
        }
    }
}
