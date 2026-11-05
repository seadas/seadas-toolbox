package gov.nasa.gsfc.seadas.earthdatacloud.matchup.ui;

import gov.nasa.gsfc.seadas.earthdatacloud.matchup.MatchupRun;
import gov.nasa.gsfc.seadas.earthdatacloud.matchup.SeabassFile;
import gov.nasa.gsfc.seadas.earthdatacloud.matchup.SeabassObservation;
import gov.nasa.gsfc.seadas.earthdatacloud.matchup.SeabassObservations;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Step 1: the SeaBASS file and its observations. */
final class ObservationsStep extends Step {

    private static final Preferences PREFS = Preferences.userNodeForPackage(ObservationsStep.class);
    private static final String LAST_DIR = "matchup.lastDirectory";

    /** Fields that place an observation rather than measure something. */
    private static final Set<String> PLACE_FIELDS = Set.of("station", "date", "time", "year", "month", "day", "sdy",
            "hour", "minute", "second", "lat", "lon", "cruise", "associated_files", "associated_file_types");
    /** A field name ending in a wavelength: Rrs412, Rrs_443, a_490.5. */
    private static final Pattern BAND = Pattern.compile("(.*?)_?\\d+(\\.\\d+)?");

    private final Look.Pill counts = new Look.Pill(" ", 28);
    private final Look.Pill fileChip = new Look.Pill(" ");
    private final Look.Wrap footnote = Look.small(" ");
    private final ObservationTable model = new ObservationTable();
    private final Runnable next;

    ObservationsStep(MatchupSession session, Runnable next) {
        super(session);
        this.next = next;

        counts.colors(Color.WHITE, Look.TEXT).line(Look.BORDER, false);
        counts.setFont(Look.base());
        counts.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 12, 5, 12));
        JPanel top = Look.clear(new BorderLayout(16, 0));
        top.add(Look.heading("Observations",
                Look.muted("In-situ records to match. Times are UTC; positions in decimal degrees.")),
                BorderLayout.CENTER);
        top.add(counts, BorderLayout.EAST);

        JButton open = new JButton("Open SeaBASS file…");
        open.addActionListener(e -> chooseFile());
        JButton csv = new JButton("Add CSV… (later)");
        csv.setEnabled(false);
        JButton pins = new JButton("From pins… (later)");
        pins.setEnabled(false);
        JButton resume = new JButton("Resume a run…");
        resume.setToolTipText("Carry on with a run saved earlier (a .matchup-run.json file)");
        resume.addActionListener(e -> chooseRecord());
        fileChip.colors(Look.BLUE_PALER, Look.TEXT);
        fileChip.setFont(Look.font(-1, Font.PLAIN));
        fileChip.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 10, 5, 10));
        JPanel buttons = Look.clear(new BorderLayout());
        buttons.add(Look.row(8, open, csv, pins, resume), BorderLayout.WEST);
        buttons.add(fileChip, BorderLayout.EAST);

        JPanel north = Look.clear(new BorderLayout(0, 14));
        north.add(top, BorderLayout.NORTH);
        north.add(buttons, BorderLayout.SOUTH);
        add(north, BorderLayout.NORTH);

        JTable table = new JTable(model) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                if (!isRowSelected(row) && model.skipped(convertRowIndexToModel(row))) {
                    c.setBackground(Look.SKIPPED_ROW);
                    c.setForeground(Look.SKIPPED_TEXT);
                }
                return c;
            }
        };
        Look.style(table);
        table.setAutoCreateRowSorter(true);
        int[] widths = {40, 110, 150, 90, 95, 130, 300};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        add(Look.scroll(table), BorderLayout.CENTER);
        add(footnote, BorderLayout.SOUTH);

        session.addListener(this::refresh);
        refresh();
    }

    @Override
    String title() {
        return "Observations";
    }

    @Override
    String navNote() {
        return "Runs in Harmony: no downloads, no OCSSW needed.";
    }

    @Override
    String help() {
        return "Open a SeaBASS file of in-situ measurements. Each row needs a date and time (UTC) and a position; "
                + "rows without them are listed but not sent. A row at the same place and time as an earlier one is "
                + "matched once and gets the earlier row's result.\n\n"
                + "A run is saved beside the file as it goes (<name>.matchup-run.json). Opening the file again, "
                + "or Resume a run…, carries on from where it stopped.";
    }

    private void refresh() {
        SeabassFile file = session.file();
        model.set(file, session.observations(), session.duplicates());
        if (file == null) {
            counts.setText("No file open");
            fileChip.setVisible(false);
            footnote.setText(" ");
            return;
        }
        int total = session.observations().size();
        long usable = session.usableObservations();
        long skipped = total - usable;
        counts.setText("<html><b>" + total + "</b> " + (total == 1 ? "record" : "records") + " · <b>" + usable
                + "</b> usable" + (skipped == 0 ? "" : " · <b><font color='" + Look.hex(Look.ORANGE_TEXT) + "'>"
                + skipped + " skipped</font></b>") + "</html>");
        String name = session.seabass().getFileName().toString();
        fileChip.setText(name + " · " + Text.plural(total, "record", "records"));
        fileChip.setToolTipText(session.seabass().toString());
        fileChip.setVisible(true);
        String time = SeabassObservations.timeSource(file);
        footnote.setText("<html>Date-time comes from the SeaBASS fields mk_matchup.py accepts: date and time, "
                + "year/month/day or sdy with the time fields, or the header’s /start_date."
                + (time == null ? "" : " This file: " + Text.escape(time) + ".")
                + " Position comes from the lat and lon fields.</html>");
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

    /**
     * The in-situ fields a row has values for, wavelength series folded:
     * {@code chl, Rrs ×6, wt}.
     */
    static String inSituFields(SeabassFile file, int row) {
        Map<String, List<String>> groups = new LinkedHashMap<>();
        for (String field : file.fields()) {
            String lower = field.toLowerCase(Locale.ROOT);
            if (PLACE_FIELDS.contains(lower) || file.isMissing(file.value(row, field))) {
                continue;
            }
            Matcher band = BAND.matcher(field);
            String key = band.matches() && !band.group(1).isEmpty() ? band.group(1) : field;
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(field);
        }
        List<String> parts = new ArrayList<>();
        groups.forEach((name, list) -> parts.add(list.size() == 1 ? list.get(0) : name + " ×" + list.size()));
        return String.join(", ", parts);
    }

    /** The observations as a table. */
    private static final class ObservationTable extends AbstractTableModel {
        private static final String[] COLUMNS = {"#", "File", "Date-time (UTC)", "Latitude", "Longitude",
                "In-situ fields", "Note"};
        private List<SeabassObservation> rows = List.of();
        private Map<Integer, Integer> duplicates = Map.of();
        private List<String> fields = List.of();
        private String fileName = "";

        void set(SeabassFile file, List<SeabassObservation> observations, Map<Integer, Integer> sameAs) {
            rows = observations;
            duplicates = sameAs;
            fileName = file == null ? "" : file.source().getFileName().toString();
            fields = new ArrayList<>();
            for (SeabassObservation o : observations) {
                fields.add(file == null ? "" : inSituFields(file, o.row()));
            }
            fireTableDataChanged();
        }

        boolean skipped(int row) {
            SeabassObservation o = rows.get(row);
            return !o.usable() || duplicates.containsKey(o.row());
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
            return column == 0 ? Integer.class : column == 3 || column == 4 ? Double.class : String.class;
        }

        @Override
        public Object getValueAt(int row, int column) {
            SeabassObservation o = rows.get(row);
            return switch (column) {
                case 0 -> o.row() + 1;
                case 1 -> fileName;
                case 2 -> Text.time(o.time());
                case 3 -> Double.isNaN(o.lat()) ? null : o.lat();
                case 4 -> Double.isNaN(o.lon()) ? null : o.lon();
                case 5 -> fields.get(row);
                default -> {
                    if (o.problem() != null) {
                        yield "Skipped: " + o.problem();
                    }
                    Integer original = duplicates.get(o.row());
                    if (original != null) {
                        yield "Same place and time as #" + (original + 1) + ": matched once";
                    }
                    yield o.note() == null ? "" : o.note();
                }
            };
        }
    }
}
