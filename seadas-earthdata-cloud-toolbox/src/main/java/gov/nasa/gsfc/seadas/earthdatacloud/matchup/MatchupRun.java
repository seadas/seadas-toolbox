package gov.nasa.gsfc.seadas.earthdatacloud.matchup;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;

/**
 * A matchup run: one SeaBASS file against one or more products, one Harmony job
 * per observation and product. The run record (JSON) is saved after every
 * change, so a run stopped by the user, a closed SeaDAS or an expired token
 * resumes where it was: submitted jobs are followed, not submitted again.
 *
 * <p>Results are downloaded as each job finishes, into
 * {@code <record>.results/<jobId>/}, next to the record.
 */
public final class MatchupRun {

    static final int FORMAT = 1;

    /** Where a task is. */
    public enum State {
        /** Not submitted yet. */
        PENDING,
        /** A job exists; not finished, or its results aren't downloaded yet. */
        SUBMITTED,
        /** The job finished and its results are downloaded. */
        DONE,
        /** No result: Harmony refused the request, or the job failed. */
        FAILED,
        /** Not sent: the observation can't be matched (no time, bad position). */
        SKIPPED
    }

    /**
     * What a run does.
     *
     * @param collections CMR concept IDs of the products, in output order
     * @param variables   variables to report, or empty for all
     * @param maxInFlight most jobs at once
     */
    public record Settings(HarmonyEnvironment environment, List<String> collections, double halfBoxDeg,
                           Duration halfWindow, List<String> variables, int maxInFlight) {
        public Settings {
            collections = List.copyOf(collections);
            variables = variables == null ? List.of() : List.copyOf(variables);
            if (collections.isEmpty()) {
                throw new IllegalArgumentException("a run needs at least one product");
            }
            if (collections.stream().distinct().count() != collections.size()) {
                throw new IllegalArgumentException("a product is listed twice: " + collections);
            }
            if (maxInFlight < 1) {
                throw new IllegalArgumentException("maxInFlight must be at least 1: " + maxInFlight);
            }
        }

        /** Production, the default box and window, all variables, 10 jobs at once. */
        public static Settings of(HarmonyEnvironment environment, List<String> collections) {
            return new Settings(environment, collections, MatchupRequest.DEFAULT_HALF_BOX_DEG,
                    MatchupRequest.DEFAULT_HALF_WINDOW, List.of(), 10);
        }
    }

    /**
     * One observation against one product.
     *
     * @param row     the SeaBASS data row (0-based)
     * @param jobId   the Harmony job, once submitted
     * @param status  Harmony's final job status, once finished
     * @param message why it failed or was skipped, or Harmony's message
     * @param files   downloaded result files, relative to the results folder; empty until done
     */
    public record Task(int row, String collection, State state, String jobId, String status, String message,
                       List<String> files) {
        public Task {
            files = files == null ? List.of() : List.copyOf(files);
        }

        Task with(State newState, String newJobId, String newStatus, String newMessage, List<String> newFiles) {
            return new Task(row, collection, newState, newJobId, newStatus, newMessage, newFiles);
        }
    }

    /** What a run reports as it goes, on the thread running it. */
    public interface Listener {
        /** A task changed; the record is already saved. */
        default void changed(Task task) {
        }

        /** How many tasks are settled (done, failed or skipped), out of all. */
        default void progress(int settled, int total) {
        }
    }

    private final Path record;
    private final Path seabass;
    private final String seabassSha256;
    private final Settings settings;
    private final String label;
    private final Instant created;
    private final SeabassFile file;
    private final Map<Integer, SeabassObservation> observations = new LinkedHashMap<>();
    private final List<Task> tasks;

    private MatchupRun(Path record, Path seabass, String seabassSha256, Settings settings, String label,
                       Instant created, SeabassFile file, List<SeabassObservation> observations, List<Task> tasks) {
        this.record = record;
        this.seabass = seabass;
        this.seabassSha256 = seabassSha256;
        this.settings = settings;
        this.label = label;
        this.created = created;
        this.file = file;
        for (SeabassObservation observation : observations) {
            this.observations.put(observation.row(), observation);
        }
        this.tasks = new ArrayList<>(tasks);
    }

    // ------------------------------------------------------------ create and load

    /**
     * Starts a run and saves its record. Rows that can't be matched are skipped
     * with the reason.
     *
     * @throws IOException when the record already exists, or the SeaBASS file can't be read
     */
    public static MatchupRun create(Path seabass, Settings settings, Path record) throws IOException {
        if (Files.exists(record)) {
            throw new IOException("A run record already exists: " + record + " (load it to resume)");
        }
        Path source = seabass.toAbsolutePath().normalize();
        SeabassFile file = SeabassFile.read(source);
        List<SeabassObservation> observations = SeabassObservations.of(file);
        String label = "seadas-matchup-" + UUID.randomUUID().toString().substring(0, 8);
        List<Task> tasks = new ArrayList<>();
        for (String collection : settings.collections()) {
            for (SeabassObservation observation : observations) {
                String problem = observation.problem();
                if (problem == null) {
                    try {
                        request(settings, collection, observation, label);
                    } catch (IllegalArgumentException e) {
                        problem = e.getMessage();
                    }
                }
                tasks.add(new Task(observation.row(), collection, problem == null ? State.PENDING : State.SKIPPED,
                        null, null, problem, List.of()));
            }
        }
        MatchupRun run = new MatchupRun(record.toAbsolutePath().normalize(), source, sha256(source), settings, label,
                Instant.now(), file, observations, tasks);
        run.save();
        return run;
    }

    /**
     * Loads a run to resume it.
     *
     * @throws IOException when the record can't be read, or the SeaBASS file is gone or has changed
     */
    public static MatchupRun load(Path record) throws IOException {
        Path path = record.toAbsolutePath().normalize();
        JSONObject json;
        try {
            json = new JSONObject(Files.readString(path, StandardCharsets.UTF_8));
            if (json.getInt("format") != FORMAT) {
                throw new IOException(path + ": run record format " + json.getInt("format") + " is not supported");
            }
            Path seabass = Path.of(json.getString("seabass"));
            String sha = json.getString("seabassSha256");
            if (!Files.exists(seabass)) {
                throw new IOException("The run's SeaBASS file is gone: " + seabass);
            }
            if (!sha.equals(sha256(seabass))) {
                throw new IOException("The run's SeaBASS file has changed since the run started: " + seabass
                        + ". Start a new run.");
            }
            JSONObject s = json.getJSONObject("settings");
            Settings settings = new Settings(HarmonyEnvironment.valueOf(s.getString("environment")),
                    strings(s.getJSONArray("collections")), s.getDouble("halfBoxDeg"),
                    Duration.ofSeconds(s.getLong("halfWindowSeconds")), strings(s.getJSONArray("variables")),
                    s.getInt("maxInFlight"));
            List<Task> tasks = new ArrayList<>();
            JSONArray array = json.getJSONArray("tasks");
            for (int i = 0; i < array.length(); i++) {
                JSONObject t = array.getJSONObject(i);
                tasks.add(new Task(t.getInt("row"), t.getString("collection"), State.valueOf(t.getString("state")),
                        t.optString("jobId", null), t.optString("status", null), t.optString("message", null),
                        t.has("files") ? strings(t.getJSONArray("files")) : List.of()));
            }
            SeabassFile file = SeabassFile.read(seabass);
            return new MatchupRun(path, seabass, sha, settings, json.getString("label"),
                    Instant.parse(json.getString("created")), file, SeabassObservations.of(file), tasks);
        } catch (JSONException | IllegalArgumentException | java.time.format.DateTimeParseException e) {
            throw new IOException(path + ": not a readable run record: " + e.getMessage(), e);
        }
    }

    /** Writes the record: to a temporary file, then moved over the old one, so a crash never leaves half a record. */
    public synchronized void save() throws IOException {
        JSONObject s = new JSONObject()
                .put("environment", settings.environment().name())
                .put("collections", new JSONArray(settings.collections()))
                .put("halfBoxDeg", settings.halfBoxDeg())
                .put("halfWindowSeconds", settings.halfWindow().toSeconds())
                .put("variables", new JSONArray(settings.variables()))
                .put("maxInFlight", settings.maxInFlight());
        JSONArray array = new JSONArray();
        for (Task task : tasks) {
            JSONObject t = new JSONObject().put("row", task.row()).put("collection", task.collection())
                    .put("state", task.state().name());
            if (task.jobId() != null) {
                t.put("jobId", task.jobId());
            }
            if (task.status() != null) {
                t.put("status", task.status());
            }
            if (task.message() != null) {
                t.put("message", task.message());
            }
            if (!task.files().isEmpty()) {
                t.put("files", new JSONArray(task.files()));
            }
            array.put(t);
        }
        JSONObject json = new JSONObject().put("format", FORMAT).put("created", created.toString())
                .put("seabass", seabass.toString()).put("seabassSha256", seabassSha256).put("label", label)
                .put("settings", s).put("tasks", array);

        Files.createDirectories(record.getParent());
        Path temp = record.resolveSibling(record.getFileName() + ".tmp");
        Files.writeString(temp, json.toString(1) + "\n", StandardCharsets.UTF_8);
        try {
            Files.move(temp, record, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, record, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    // ------------------------------------------------------------ running

    /**
     * Runs every task not yet done: downloads finished jobs' results, follows
     * submitted jobs, submits pending ones. Returns when all are settled or
     * {@code cancelled} says stop; a stopped run cancels its jobs in Harmony and
     * their tasks go back to pending.
     *
     * @throws HarmonyException when the token is rejected; the record is saved and the run can resume
     */
    public void execute(HarmonyJobClient client, BooleanSupplier cancelled, Listener listener)
            throws IOException, InterruptedException {
        Files.createDirectories(resultsFolder());

        // Jobs that finished before their results were downloaded.
        for (int i = 0; i < tasks.size(); i++) {
            Task task = tasks.get(i);
            if (task.state() == State.SUBMITTED && task.status() != null) {
                if (cancelled.getAsBoolean()) {
                    return;
                }
                settle(i, client.job(task.jobId()), client, listener);
            }
        }

        List<Integer> indexes = new ArrayList<>();
        List<MatchupRequest> requests = new ArrayList<>();
        Map<Integer, String> submitted = new LinkedHashMap<>();
        for (int i = 0; i < tasks.size(); i++) {
            Task task = tasks.get(i);
            if (task.state() == State.PENDING || task.state() == State.SUBMITTED) {
                if (task.state() == State.SUBMITTED) {
                    submitted.put(requests.size(), task.jobId());
                }
                indexes.add(i);
                requests.add(request(settings, task.collection(), observations.get(task.row()), label));
            }
        }
        if (requests.isEmpty()) {
            listener.progress(settled(), tasks.size());
            return;
        }

        IOException[] listenerError = new IOException[1];
        HarmonyJobClient.BatchResult result = client.runBatch(requests, submitted, settings.maxInFlight(), () ->
                cancelled.getAsBoolean() || listenerError[0] != null, new HarmonyJobClient.BatchListener() {
                    @Override
                    public void submitted(int index, HarmonyJob job) {
                        guard(() -> update(indexes.get(index), State.SUBMITTED, job.jobId(), null, null, List.of(),
                                listener));
                    }

                    @Override
                    public void finished(int index, HarmonyJob job) {
                        guard(() -> settle(indexes.get(index), job, client, listener));
                    }

                    @Override
                    public void failed(int index, IOException error) {
                        Task task = tasks.get(indexes.get(index));
                        guard(() -> update(indexes.get(index), State.FAILED, task.jobId(), null, error.getMessage(),
                                List.of(), listener));
                    }

                    @Override
                    public void progress(int done, int total) {
                        listener.progress(settled(), tasks.size());
                    }

                    private void guard(IoAction action) {
                        if (listenerError[0] != null) {
                            return;
                        }
                        try {
                            action.run();
                        } catch (IOException e) {
                            listenerError[0] = e;
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                });

        if (result.cancelled()) {
            for (HarmonyJobClient.Outcome outcome : result.outcomes()) {
                int i = indexes.get(outcome.index());
                if (tasks.get(i).state() == State.SUBMITTED && outcome.job() == null) {
                    update(i, State.PENDING, null, null, null, List.of(), listener);
                }
            }
        }
        if (listenerError[0] != null) {
            throw listenerError[0];
        }
    }

    @FunctionalInterface
    private interface IoAction {
        void run() throws IOException, InterruptedException;
    }

    /** A finished job: download its results, or record why there are none. */
    private void settle(int i, HarmonyJob job, HarmonyJobClient client, Listener listener)
            throws IOException, InterruptedException {
        // Saved first, so a failed download is retried on resume without asking Harmony to wait again.
        update(i, State.SUBMITTED, job.jobId(), job.status(), job.message(), List.of(), listener);
        if (!job.dataLinks().isEmpty() && ("successful".equals(job.status())
                || "complete_with_errors".equals(job.status()))) {
            List<String> files = new ArrayList<>();
            for (Path path : client.downloadAll(job, resultsFolder())) {
                files.add(resultsFolder().relativize(path).toString().replace('\\', '/'));
            }
            update(i, State.DONE, job.jobId(), job.status(), job.successful() ? null : job.message(), files,
                    listener);
        } else if ("successful".equals(job.status())) {
            update(i, State.DONE, job.jobId(), job.status(), "no output: the observation is in no granule",
                    List.of(), listener);
        } else {
            String why = job.message() != null ? job.message() : "Harmony job " + job.status();
            update(i, State.FAILED, job.jobId(), job.status(), why, List.of(), listener);
        }
    }

    private void update(int i, State state, String jobId, String status, String message, List<String> files,
                        Listener listener) throws IOException {
        Task task = tasks.get(i).with(state, jobId, status, message, files);
        synchronized (this) {
            tasks.set(i, task);
        }
        save();
        listener.changed(task);
    }

    /** Puts failed tasks back to pending, so the next {@link #execute} tries them again. */
    public void retryFailed() throws IOException {
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).state() == State.FAILED) {
                tasks.set(i, tasks.get(i).with(State.PENDING, null, null, null, List.of()));
            }
        }
        save();
    }

    static MatchupRequest request(Settings settings, String collection, SeabassObservation observation,
                                  String label) {
        return new MatchupRequest(collection, observation.lat(), observation.lon(),
                observation.time().toInstant(ZoneOffset.UTC), settings.halfBoxDeg(), settings.halfWindow(),
                settings.variables(), label);
    }

    // ------------------------------------------------------------ results

    /**
     * Joins the downloaded results to the SeaBASS file.
     *
     * @param products every collection of the run (e.g. {@link MatchupProduct#fetch})
     */
    public ResultJoiner.Joined join(Map<String, MatchupProduct> products, ResultJoiner.Rows rows)
            throws IOException {
        List<ResultJoiner.ProductRows> productRows = new ArrayList<>();
        for (String collection : settings.collections()) {
            MatchupProduct product = products.get(collection);
            if (product == null) {
                throw new IllegalArgumentException("no product information for " + collection);
            }
            Map<Integer, List<MatchupCsv>> csvs = new LinkedHashMap<>();
            for (Task task : tasks) {
                if (task.collection().equals(collection) && task.state() == State.DONE) {
                    List<MatchupCsv> list = csvs.computeIfAbsent(task.row(), k -> new ArrayList<>());
                    for (String name : task.files()) {
                        if (name.endsWith(".csv")) {
                            list.add(MatchupCsv.read(resultsFolder().resolve(name)));
                        }
                    }
                }
            }
            productRows.add(new ResultJoiner.ProductRows(product, csvs));
        }
        List<String> extra = List.of(" Run record: " + record.getFileName() + "; Harmony jobs labelled " + label);
        return ResultJoiner.join(file, List.copyOf(observations.values()), productRows, rows,
                new ResultJoiner.RunInfo(Instant.now(), settings.environment().harmony(), extra));
    }

    // ------------------------------------------------------------ state

    public Path record() {
        return record;
    }

    /** Where results are downloaded: {@code <record>.results} next to the record. */
    public Path resultsFolder() {
        return record.resolveSibling(record.getFileName() + ".results");
    }

    public Path seabass() {
        return seabass;
    }

    public SeabassFile file() {
        return file;
    }

    public Settings settings() {
        return settings;
    }

    /** The Harmony job label every job of this run carries. */
    public String label() {
        return label;
    }

    public synchronized List<Task> tasks() {
        return Collections.unmodifiableList(new ArrayList<>(tasks));
    }

    public synchronized Map<State, Integer> counts() {
        Map<State, Integer> counts = new EnumMap<>(State.class);
        for (State state : State.values()) {
            counts.put(state, 0);
        }
        for (Task task : tasks) {
            counts.merge(task.state(), 1, Integer::sum);
        }
        return counts;
    }

    /** Whether every task is done, failed or skipped. */
    public boolean finished() {
        return settled() == tasks.size();
    }

    private synchronized int settled() {
        return (int) tasks.stream().filter(t -> t.state() == State.DONE || t.state() == State.FAILED
                || t.state() == State.SKIPPED).count();
    }

    private static List<String> strings(JSONArray array) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            list.add(array.getString(i));
        }
        return list;
    }

    static String sha256(Path path) throws IOException {
        try (InputStream in = new DigestInputStream(Files.newInputStream(path),
                MessageDigest.getInstance("SHA-256"))) {
            in.transferTo(java.io.OutputStream.nullOutputStream());
            return HexFormat.of().formatHex(((DigestInputStream) in).getMessageDigest().digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
