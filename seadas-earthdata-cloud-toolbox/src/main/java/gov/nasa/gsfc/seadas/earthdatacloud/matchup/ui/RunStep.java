package gov.nasa.gsfc.seadas.earthdatacloud.matchup.ui;

import gov.nasa.gsfc.seadas.earthdatacloud.matchup.HarmonyEnvironment;
import gov.nasa.gsfc.seadas.earthdatacloud.matchup.HarmonyException;
import gov.nasa.gsfc.seadas.earthdatacloud.matchup.HarmonyJobClient;
import gov.nasa.gsfc.seadas.earthdatacloud.matchup.MatchupCollections;
import gov.nasa.gsfc.seadas.earthdatacloud.matchup.MatchupRun;
import gov.nasa.gsfc.seadas.earthdatacloud.matchup.SeabassObservation;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/** Step 3: send the jobs, follow them, download their results. */
final class RunStep extends Step {

    private static final int JOB_COLUMN = 5;
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");

    private final Look.Wrap subtitle = Look.muted(" ");
    private final Look.Bar bar = new Look.Bar(14);
    private final Look.Wrap percent = Look.small(" ");
    private final Stat done = new Stat("done");
    private final Stat running = new Stat("running");
    private final Stat queued = new Stat("queued");
    private final Stat failed = new Stat("failed");
    private final Stat noOverpass = new Stat("no overpass");
    private final Stat notUsable = new Stat("not usable");
    private final JLabel savedTo = new JLabel(" ");
    private final Look.Wrap savedNote = Look.small(" ");
    private final JButton runButton = new JButton("Start");
    private final JButton pauseButton = new JButton("Pause");
    private final JButton retryButton = new JButton("Retry failed");
    private final TaskTable model = new TaskTable();
    private final AtomicBoolean stopRequested = new AtomicBoolean();
    private final Timer timer = new Timer(700, e -> refresh());
    private final Runnable finished;
    private SwingWorker<?, ?> worker;
    private Instant startedAt;
    private Duration ranFor;

    RunStep(MatchupSession session, Runnable finished) {
        super(session);
        this.finished = finished;

        JPanel head = Look.clear(new BorderLayout(16, 0));
        head.add(Look.heading("Run", subtitle), BorderLayout.CENTER);
        head.add(Look.row(8, runButton, pauseButton, retryButton), BorderLayout.EAST);

        JPanel progress = Look.clear(new BorderLayout(0, 6));
        progress.add(bar, BorderLayout.NORTH);
        progress.add(percent, BorderLayout.CENTER);

        Look.Card saved = new Look.Card(new BorderLayout(0, 2));
        saved.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        saved.add(savedTo, BorderLayout.NORTH);
        saved.add(savedNote, BorderLayout.CENTER);
        JPanel stats = Look.clear(new BorderLayout(10, 0));
        stats.add(Look.row(10, done.card, running.card, queued.card, failed.card, noOverpass.card,
                notUsable.card), BorderLayout.WEST);
        stats.add(saved, BorderLayout.CENTER);

        JPanel north = Look.clear(null);
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        for (JPanel p : List.of(head, progress, stats)) {
            p.setAlignmentX(LEFT_ALIGNMENT);
            north.add(p);
            north.add(Box.createVerticalStrut(14));
        }
        add(north, BorderLayout.NORTH);

        JTable table = new JTable(model);
        Look.style(table);
        table.setAutoCreateRowSorter(true);
        int[] widths = {55, 150, 160, 100, 80, 110, 330};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        table.getColumnModel().getColumn(3).setCellRenderer(new Look.PillCell() {
            @Override
            Look.Tone tone(Object value, int row) {
                return switch (model.task(row).state()) {
                    case DONE -> Look.Tone.BLUE;
                    case SUBMITTED -> Look.Tone.ORANGE;
                    case FAILED -> Look.Tone.RED;
                    case SKIPPED -> Look.Tone.DASHED;
                    case PENDING -> Look.Tone.GREY;
                };
            }
        });
        table.getColumnModel().getColumn(JOB_COLUMN).setCellRenderer(new Look.Cell(true) {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean selected, boolean focus,
                                                           int row, int column) {
                super.getTableCellRendererComponent(t, value, selected, focus, row, column);
                boolean link = value != null && !value.toString().equals("—");
                if (link && !selected) {
                    setForeground(Look.BLUE);
                }
                setToolTipText(link ? "Open the job in Harmony's workflow page" : null);
                return this;
            }
        });
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int column = table.columnAtPoint(e.getPoint());
                if (row >= 0 && (e.getClickCount() == 2
                        || table.convertColumnIndexToModel(column) == JOB_COLUMN)) {
                    openJob(model.task(table.convertRowIndexToModel(row)));
                }
            }
        });
        table.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int column = table.columnAtPoint(e.getPoint());
                boolean job = column >= 0 && table.convertColumnIndexToModel(column) == JOB_COLUMN;
                table.setCursor(Cursor.getPredefinedCursor(job ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            }
        });
        add(Look.scroll(table), BorderLayout.CENTER);
        add(Look.small("A failed request shows Harmony’s message here; Retry failed sends it again, and the rest of "
                + "the run carries on."), BorderLayout.SOUTH);

        runButton.addActionListener(e -> start(false));
        retryButton.addActionListener(e -> start(true));
        pauseButton.setToolTipText("Stops sending and cancels this run's jobs in Harmony; Resume carries on later");
        pauseButton.addActionListener(e -> {
            stopRequested.set(true);
            pauseButton.setEnabled(false);
            refresh();
        });

        session.addListener(this::refresh);
        refresh();
    }

    @Override
    String title() {
        return "Run";
    }

    @Override
    String help() {
        return "Start sends one Harmony job per observation and product, a few at a time, and downloads each "
                + "result. The run is saved as it goes, so you can close the window or SeaDAS and resume later: "
                + "open the SeaBASS file again, or use Resume a run… in step 1.\n\n"
                + "Pause cancels the jobs in Harmony and stops sending; Resume sends what is left. A failed job "
                + "shows Harmony's message; Retry failed sends those again.\n\n"
                + "Click a job to see it in Harmony's workflow page.";
    }

    @Override
    String navNote() {
        return "You can keep working in SeaDAS while this runs.";
    }

    @Override
    String nextLabel() {
        MatchupRun run = session.run();
        return run != null && run.finished() ? "See results ›" : "See results so far ›";
    }

    @Override
    String unavailableReason() {
        if (session.file() == null) {
            return "Open a SeaBASS file first";
        }
        return session.chosen().isEmpty() ? "Tick at least one product first" : null;
    }

    @Override
    void shown() {
        refresh();
    }

    @Override
    boolean busy() {
        return worker != null;
    }

    @Override
    void stop() {
        stopRequested.set(true);
    }

    /** Whether Start would send anything now. */
    boolean canStart() {
        MatchupRun run = session.run();
        return worker == null && session.file() != null && !session.chosen().isEmpty()
                && (run == null || !run.finished());
    }

    private void refresh() {
        MatchupRun run = session.run();
        model.set(run);
        boolean busy = worker != null;
        int inFlight = session.options().maxInFlight();
        if (run == null) {
            long upTo = session.usableObservations() * session.chosen().size();
            subtitle.setText("Up to " + Text.plural(upTo, "request", "requests") + " to Harmony, " + inFlight
                    + " at a time. Nothing sent yet.");
            bar.set(List.of(), List.of());
            percent.setText(" ");
            for (Stat stat : List.of(done, running, queued, failed, noOverpass, notUsable)) {
                stat.set(0);
            }
            savedTo.setText("Not saved yet");
            savedNote.setText("Start saves the run beside the SeaBASS file.");
        } else {
            Map<MatchupRun.State, Integer> counts = run.counts();
            int total = run.tasks().size();
            int sent = total - counts.get(MatchupRun.State.SKIPPED);
            int doneCount = counts.get(MatchupRun.State.DONE);
            int failedCount = counts.get(MatchupRun.State.FAILED);
            StringBuilder line = new StringBuilder(Text.plural(sent, "request", "requests") + " to Harmony, "
                    + inFlight + " at a time.");
            if (busy && stopRequested.get()) {
                line.append(" Pausing: cancelling this run's jobs in Harmony…");
            } else if (startedAt != null) {
                Duration elapsed = busy ? Duration.between(startedAt, Instant.now()) : ranFor;
                line.append(" Started ").append(CLOCK.format(LocalTime.ofInstant(startedAt, ZoneId.systemDefault())));
                if (elapsed != null) {
                    line.append(" · ").append(busy ? "elapsed " : "ran for ").append(duration(elapsed));
                }
            } else if (run.started()) {
                line.append(run.finished() ? " Finished." : " Paused; Resume sends the rest.");
            }
            subtitle.setText(line.toString());
            double whole = Math.max(1, sent);
            bar.set(List.of(doneCount / whole, failedCount / whole), List.of(Look.BLUE, Look.ORANGE));
            percent.setText(sent == 0 ? "Nothing to send: every observation was left out"
                    : Math.round(100.0 * (doneCount + failedCount) / sent) + "% done"
                    + (failedCount > 0 ? " · " + failedCount + " failed" : ""));
            done.set(doneCount);
            running.set(counts.get(MatchupRun.State.SUBMITTED));
            queued.set(counts.get(MatchupRun.State.PENDING));
            failed.set(failedCount);
            noOverpass.set(skipped(run, true));
            notUsable.set(skipped(run, false));
            savedTo.setText("<html>Saved to <font face='monospaced'>"
                    + Text.escape(run.record().getFileName().toString()) + "</font></html>");
            savedTo.setToolTipText(run.record().toString());
            savedNote.setText("<html>Close the window any time; open the run again to pick up where it stopped. "
                    + "Harmony jobs are labelled " + Text.escape(run.label()) + ".</html>");
        }
        failed.number.setForeground(failed.value > 0 ? Look.RED_TEXT : Look.TEXT);
        boolean hasWork = run == null || !run.finished();
        boolean hasFailed = run != null && run.counts().get(MatchupRun.State.FAILED) > 0;
        runButton.setEnabled(!busy && hasWork && session.file() != null && !session.chosen().isEmpty());
        runButton.setText(run != null && run.started() && hasWork ? "Resume" : "Start");
        pauseButton.setEnabled(busy && !stopRequested.get());
        retryButton.setEnabled(!busy && hasFailed);
    }

    private static String duration(Duration d) {
        long s = d.toSeconds();
        if (s < 60) {
            return s + " s";
        }
        if (s < 3600) {
            return s / 60 + " min " + s % 60 + " s";
        }
        return s / 3600 + " h " + s % 3600 / 60 + " min";
    }

    void start(boolean retry) {
        MatchupRun run;
        try {
            run = session.ensureRun();
            if (retry) {
                run.retryFailed();
            }
        } catch (Exception e) {
            Background.showError(this, "Can't start the run", e);
            return;
        }
        int jobs = run.counts().get(MatchupRun.State.PENDING);
        if (!run.started() && jobs > 50 && session.coverage() == null) {
            int answer = JOptionPane.showConfirmDialog(this, Text.wrap("This sends up to " + jobs
                            + " Harmony jobs. The coverage check (step 2) leaves out observations with no overpass.\n\n"
                            + "Send them all now?"), "Many jobs", JOptionPane.OK_CANCEL_OPTION);
            if (answer != JOptionPane.OK_OPTION) {
                return;
            }
        }
        HarmonyEnvironment environment = session.options().environment();
        stopRequested.set(false);
        startedAt = Instant.now();
        ranFor = null;
        timer.start();
        worker = Background.run(this, "Matchup run", () -> {
            String token = session.tokens().token(environment);
            HarmonyJobClient client = HarmonyJobClient.create(environment, () -> token);
            try {
                run.execute(client, stopRequested::get, new MatchupRun.Listener() {
                });
            } catch (HarmonyException e) {
                if (e.unauthorized()) {
                    session.tokens().forget(environment);
                    throw new HarmonyException(e.httpStatus(), e.code(), e.getMessage() + ". The run is saved: "
                            + "check the Earthdata login in ~/.netrc (machine " + environment.loginHost()
                            + "), then press Resume.");
                }
                throw e;
            }
            return run.finished();
        }, finishedNow -> {
            if (finishedNow) {
                finished.run();
            }
        }, () -> {
            worker = null;
            ranFor = Duration.between(startedAt, Instant.now());
            timer.stop();
            refresh();
            session.changed();
        });
        refresh();
        session.changed();
    }

    private void openJob(MatchupRun.Task task) {
        if (task == null || task.jobId() == null) {
            return;
        }
        try {
            Desktop.getDesktop().browse(session.options().environment().workflowUi(task.jobId()));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, session.options().environment().workflowUi(task.jobId()).toString(),
                    "Harmony job", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Requests not sent: with no granule in the time window (no overpass), or
     * for an observation without a usable time or position (not usable). A
     * repeat of an earlier row's place and time is neither: it shares that
     * row's request.
     */
    static int skipped(MatchupRun run, boolean noOverpass) {
        int n = 0;
        for (MatchupRun.Task task : run.tasks()) {
            if (task.state() == MatchupRun.State.SKIPPED && task.sameAs() < 0) {
                boolean overpass = task.message() != null && task.message().startsWith(MatchupRun.NO_OVERPASS);
                n += overpass == noOverpass ? 1 : 0;
            }
        }
        return n;
    }

    /** A count in a small card: the number large, its meaning under it. */
    private static final class Stat {
        final Look.Card card = new Look.Card(new BorderLayout(0, 2));
        final JLabel number = new JLabel("0");
        int value;

        Stat(String label) {
            card.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
            number.setFont(new Font(Font.MONOSPACED, Font.BOLD, Math.round(Look.base().getSize2D() + 9)));
            number.setForeground(Look.TEXT);
            card.add(number, BorderLayout.NORTH);
            card.add(Look.small(label), BorderLayout.CENTER);
            card.setPreferredSize(new java.awt.Dimension(104, card.getPreferredSize().height));
        }

        void set(int n) {
            value = n;
            number.setText(String.valueOf(n));
        }
    }

    /** The run's tasks. */
    private static final class TaskTable extends AbstractTableModel {
        private static final String[] COLUMNS = {"Obs", "Date-time (UTC)", "Product", "State", "Granules",
                "Harmony job", "Message"};
        private List<MatchupRun.Task> tasks = List.of();
        private final Map<Integer, SeabassObservation> observations = new HashMap<>();
        private MatchupRun run;

        void set(MatchupRun newRun) {
            if (newRun != run) {
                run = newRun;
                observations.clear();
                if (run != null) {
                    run.observations().forEach(o -> observations.put(o.row(), o));
                }
            }
            List<MatchupRun.Task> next = run == null ? List.of() : run.tasks();
            boolean sameRows = next.size() == tasks.size();
            tasks = next;
            if (sameRows && !tasks.isEmpty()) {
                fireTableRowsUpdated(0, tasks.size() - 1);
            } else {
                fireTableDataChanged();
            }
        }

        MatchupRun.Task task(int row) {
            return row < tasks.size() ? tasks.get(row) : null;
        }

        @Override
        public int getRowCount() {
            return tasks.size();
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
            return column == 0 ? Integer.class : String.class;
        }

        @Override
        public Object getValueAt(int row, int column) {
            MatchupRun.Task t = tasks.get(row);
            return switch (column) {
                case 0 -> t.row() + 1;
                case 1 -> {
                    SeabassObservation o = observations.get(t.row());
                    yield o == null || o.time() == null ? "—" : Text.time(o.time()).substring(0, 16);
                }
                case 2 -> MatchupCollections.byId(t.collection()).map(SatelliteStep::shortLabel)
                        .orElse(t.collection());
                case 3 -> switch (t.state()) {
                    case PENDING -> "queued";
                    case SUBMITTED -> "running";
                    case DONE -> "done";
                    case FAILED -> "failed";
                    case SKIPPED -> t.sameAs() >= 0 ? "same as #" + (t.sameAs() + 1)
                            : t.message() != null && t.message().startsWith(MatchupRun.NO_OVERPASS) ? "no overpass"
                            : "not usable";
                };
                case 4 -> t.granules() == null ? "—" : String.valueOf(t.granules());
                case JOB_COLUMN -> t.jobId() == null ? "—" : t.jobId().substring(0, Math.min(8, t.jobId().length()))
                        + "…";
                default -> t.message() == null ? "" : t.message();
            };
        }
    }
}
