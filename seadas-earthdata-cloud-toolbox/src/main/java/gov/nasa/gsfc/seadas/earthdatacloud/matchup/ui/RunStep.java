package gov.nasa.gsfc.seadas.earthdatacloud.matchup.ui;

import gov.nasa.gsfc.seadas.earthdatacloud.matchup.HarmonyEnvironment;
import gov.nasa.gsfc.seadas.earthdatacloud.matchup.HarmonyException;
import gov.nasa.gsfc.seadas.earthdatacloud.matchup.HarmonyJobClient;
import gov.nasa.gsfc.seadas.earthdatacloud.matchup.MatchupCollections;
import gov.nasa.gsfc.seadas.earthdatacloud.matchup.MatchupRun;
import gov.nasa.gsfc.seadas.earthdatacloud.matchup.SeabassObservation;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/** Step 3: send the jobs, follow them, download their results. */
final class RunStep extends Step {

    private final JLabel recordLabel = new JLabel(" ");
    private final JLabel countsLabel = new JLabel(" ");
    private final JProgressBar progress = new JProgressBar();
    private final JButton runButton = new JButton("Run");
    private final JButton stopButton = new JButton("Stop");
    private final JButton retryButton = new JButton("Retry failed");
    private final TaskTable model = new TaskTable();
    private final AtomicBoolean stopRequested = new AtomicBoolean();
    private final Timer timer = new Timer(700, e -> refresh());
    private final Runnable finished;
    private SwingWorker<?, ?> running;

    RunStep(MatchupSession session, Runnable finished) {
        super(session);
        this.finished = finished;

        JPanel top = new JPanel();
        top.setLayout(new javax.swing.BoxLayout(top, javax.swing.BoxLayout.Y_AXIS));
        progress.setStringPainted(true);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        buttons.add(runButton);
        buttons.add(stopButton);
        buttons.add(retryButton);
        for (javax.swing.JComponent c : List.of(recordLabel, progress, countsLabel, buttons)) {
            c.setAlignmentX(LEFT_ALIGNMENT);
            top.add(c);
            top.add(javax.swing.Box.createVerticalStrut(6));
        }
        add(top, BorderLayout.NORTH);

        JTable table = new JTable(model);
        table.setAutoCreateRowSorter(true);
        table.getColumnModel().getColumn(0).setPreferredWidth(40);
        table.getColumnModel().getColumn(1).setPreferredWidth(150);
        table.getColumnModel().getColumn(2).setPreferredWidth(160);
        table.getColumnModel().getColumn(3).setPreferredWidth(80);
        table.getColumnModel().getColumn(4).setPreferredWidth(90);
        table.getColumnModel().getColumn(5).setPreferredWidth(260);
        table.setToolTipText("Double-click a row to see its job in Harmony's workflow page");
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (e.getClickCount() == 2 && row >= 0) {
                    openJob(model.task(table.convertRowIndexToModel(row)));
                }
            }
        });
        add(new JScrollPane(table), BorderLayout.CENTER);

        runButton.addActionListener(e -> start(false));
        retryButton.addActionListener(e -> start(true));
        stopButton.addActionListener(e -> {
            stopRequested.set(true);
            stopButton.setEnabled(false);
            countsLabel.setText("Stopping: cancelling this run's jobs in Harmony…");
        });

        session.addListener(this::refresh);
        refresh();
    }

    @Override
    String title() {
        return "Run";
    }

    @Override
    String unavailableReason() {
        if (session.file() == null) {
            return "Open a SeaBASS file first";
        }
        return session.chosen().isEmpty() ? "Choose at least one product first" : null;
    }

    @Override
    void shown() {
        refresh();
    }

    @Override
    boolean busy() {
        return running != null;
    }

    @Override
    void stop() {
        stopRequested.set(true);
    }

    private void refresh() {
        MatchupRun run = session.run();
        model.set(run);
        boolean busy = running != null;
        if (run == null) {
            recordLabel.setText("Nothing sent yet. Run sends one Harmony job per observation and product.");
            progress.setValue(0);
            progress.setString("");
            countsLabel.setText(" ");
        } else {
            recordLabel.setText("<html>Saved as it goes to <b>" + Text.escape(run.record().getFileName().toString())
                    + "</b>; after closing, resume it from step 1.<br>Harmony jobs are labelled <b>"
                    + Text.escape(run.label()) + "</b>. Double-click a row to see its job in Harmony.</html>");
            recordLabel.setToolTipText(run.record().toString());
            Map<MatchupRun.State, Integer> counts = run.counts();
            int total = run.tasks().size();
            int settled = counts.get(MatchupRun.State.DONE) + counts.get(MatchupRun.State.FAILED)
                    + counts.get(MatchupRun.State.SKIPPED);
            progress.setMaximum(Math.max(1, total));
            progress.setValue(settled);
            progress.setString(settled + " of " + total + " settled");
            if (!busy || !stopRequested.get()) {
                countsLabel.setText(counts.get(MatchupRun.State.DONE) + " done · "
                        + counts.get(MatchupRun.State.FAILED) + " failed · "
                        + counts.get(MatchupRun.State.SUBMITTED) + " in Harmony · "
                        + counts.get(MatchupRun.State.PENDING) + " waiting · "
                        + counts.get(MatchupRun.State.SKIPPED) + " not sent (no overpass or not usable)");
            }
        }
        boolean hasWork = run == null || !run.finished();
        boolean hasFailed = run != null && run.counts().get(MatchupRun.State.FAILED) > 0;
        runButton.setEnabled(!busy && hasWork && session.file() != null && !session.chosen().isEmpty());
        runButton.setText(!busy && run != null && run.started() && hasWork ? "Resume" : "Run");
        stopButton.setEnabled(busy && !stopRequested.get());
        retryButton.setEnabled(!busy && hasFailed);
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
        timer.start();
        running = Background.run(this, "Matchup run", () -> {
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
        }, done -> {
            if (done) {
                finished.run();
            }
        }, () -> {
            running = null;
            timer.stop();
            refresh();
            session.changed();
        });
        refresh();
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

    /** The run's tasks. */
    private static final class TaskTable extends AbstractTableModel {
        private static final String[] COLUMNS = {"#", "Date-time (UTC)", "Product", "State", "Harmony", "Message"};
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
                    yield o == null ? "" : Text.time(o.time());
                }
                case 2 -> MatchupCollections.byId(t.collection()).map(MatchupCollections.Entry::shortName)
                        .orElse(t.collection());
                case 3 -> switch (t.state()) {
                    case PENDING -> "waiting";
                    case SUBMITTED -> "in Harmony";
                    case DONE -> "done";
                    case FAILED -> "failed";
                    case SKIPPED -> "not sent";
                };
                case 4 -> t.status() != null ? t.status() : t.jobId() != null ? "running" : "";
                default -> t.message() == null ? "" : t.message();
            };
        }
    }
}
