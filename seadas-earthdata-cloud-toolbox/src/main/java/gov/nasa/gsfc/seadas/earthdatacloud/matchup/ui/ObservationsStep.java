package gov.nasa.gsfc.seadas.earthdatacloud.matchup.ui;

import gov.nasa.gsfc.seadas.earthdatacloud.matchup.MatchupRun;
import gov.nasa.gsfc.seadas.earthdatacloud.matchup.SeabassObservation;
import gov.nasa.gsfc.seadas.earthdatacloud.matchup.SeabassObservations;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

/** Step 1: the SeaBASS file and its observations. */
final class ObservationsStep extends Step {

    private static final Preferences PREFS = Preferences.userNodeForPackage(ObservationsStep.class);
    private static final String LAST_DIR = "matchup.lastDirectory";

    private final JLabel summary = new JLabel(" ");
    private final ObservationTable model = new ObservationTable();
    private final Runnable next;

    ObservationsStep(MatchupSession session, Runnable next) {
        super(session);
        this.next = next;

        JButton open = new JButton("Open SeaBASS file…");
        open.addActionListener(e -> chooseFile());
        JButton resume = new JButton("Resume a run…");
        resume.setToolTipText("Carry on with a run saved earlier (a .matchup-run.json file)");
        resume.addActionListener(e -> chooseRecord());
        JPanel top = new JPanel(new BorderLayout());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        buttons.add(open);
        buttons.add(javax.swing.Box.createHorizontalStrut(6));
        buttons.add(resume);
        top.add(buttons, BorderLayout.WEST);
        summary.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));
        top.add(summary, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        JTable table = new JTable(model);
        table.setAutoCreateRowSorter(true);
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(1).setPreferredWidth(50);
        table.getColumnModel().getColumn(2).setPreferredWidth(150);
        table.getColumnModel().getColumn(5).setPreferredWidth(320);
        add(new JScrollPane(table), BorderLayout.CENTER);

        add(new JLabel("<html>Each observation needs a date and time (UTC) and a position. Rows without them "
                + "are listed but not sent.</html>"), BorderLayout.SOUTH);

        session.addListener(this::refresh);
        refresh();
    }

    @Override
    String title() {
        return "Observations";
    }

    private void refresh() {
        model.set(session.observations());
        if (session.file() == null) {
            summary.setText("No file open");
            return;
        }
        String time = SeabassObservations.timeSource(session.file());
        summary.setText("<html><b>" + Text.escape(session.seabass().getFileName().toString()) + "</b>: "
                + Text.plural(session.observations().size(), "record", "records") + ", "
                + session.usableObservations() + " usable"
                + (time == null ? "" : "; time from " + Text.escape(time)) + "</html>");
        summary.setToolTipText(session.seabass().toString());
    }

    private void chooseFile() {
        JFileChooser chooser = new JFileChooser(PREFS.get(LAST_DIR, System.getProperty("user.home")));
        chooser.setDialogTitle("Open SeaBASS file");
        chooser.setFileFilter(new FileNameExtensionFilter("SeaBASS files (*.sb, *.txt, *.csv)", "sb", "txt",
                "csv", "dat"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File selected = chooser.getSelectedFile();
        PREFS.put(LAST_DIR, selected.getParent());
        if (confirmLeavingRun()) {
            openFile(selected.toPath());
        }
    }

    /** Opens a SeaBASS file, offering to resume its earlier run if there is one. */
    void openFile(Path path) {
        Path record = MatchupSession.latestRecord(path.toAbsolutePath().normalize());
        if (record != null) {
            Object[] choices = {"Resume it", "Start a new run", "Cancel"};
            int choice = JOptionPane.showOptionDialog(this, Text.wrap("A matchup run for this file was saved earlier: "
                            + record.getFileName() + ".\n\nResume it, or start a new run (the earlier one is kept)?"),
                    "Earlier run", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, choices,
                    choices[0]);
            if (choice == 0) {
                resumeRecord(record);
                return;
            }
            if (choice != 1) {
                return;
            }
        }
        try {
            session.open(path);
            if (session.usableObservations() > 0) {
                next.run();
            }
        } catch (IOException e) {
            Background.showError(this, "Can't read the SeaBASS file", e);
        }
    }

    private void chooseRecord() {
        JFileChooser chooser = new JFileChooser(PREFS.get(LAST_DIR, System.getProperty("user.home")));
        chooser.setDialogTitle("Resume a matchup run");
        chooser.setFileFilter(new FileNameExtensionFilter("Matchup run records (*.matchup-run*.json)", "json"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION && confirmLeavingRun()) {
            PREFS.put(LAST_DIR, chooser.getSelectedFile().getParent());
            resumeRecord(chooser.getSelectedFile().toPath());
        }
    }

    private void resumeRecord(Path record) {
        try {
            session.resume(record);
            Map<MatchupRun.State, Integer> counts = session.run().counts();
            if (counts.get(MatchupRun.State.DONE) > 0 || counts.get(MatchupRun.State.FAILED) > 0
                    || counts.get(MatchupRun.State.SUBMITTED) > 0) {
                next.run();
            }
        } catch (IOException e) {
            Background.showError(this, "Can't resume the run", e);
        }
    }

    private boolean confirmLeavingRun() {
        if (session.run() == null || session.run().finished() || !session.started()) {
            return true;
        }
        return JOptionPane.showConfirmDialog(this, Text.wrap("The current run isn't finished. It is saved and can "
                        + "be resumed later from " + session.run().record().getFileName() + ".\n\nOpen another file?"),
                "Run not finished", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION;
    }

    /** The observations as a table. */
    private static final class ObservationTable extends AbstractTableModel {
        private static final String[] COLUMNS = {"#", "Line", "Date-time (UTC)", "Latitude", "Longitude", "Note"};
        private List<SeabassObservation> rows = List.of();

        void set(List<SeabassObservation> observations) {
            rows = observations;
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return rows.size();
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
            return column <= 1 ? Integer.class : column == 3 || column == 4 ? Double.class : String.class;
        }

        @Override
        public Object getValueAt(int row, int column) {
            SeabassObservation o = rows.get(row);
            return switch (column) {
                case 0 -> o.row() + 1;
                case 1 -> o.lineNumber();
                case 2 -> Text.time(o.time());
                case 3 -> Double.isNaN(o.lat()) ? null : o.lat();
                case 4 -> Double.isNaN(o.lon()) ? null : o.lon();
                default -> o.problem() != null ? "⚠ " + o.problem() : o.note() == null ? "" : o.note();
            };
        }
    }
}
