package gov.nasa.gsfc.seadas.earthdatacloud.matchup.ui;

import gov.nasa.gsfc.seadas.earthdatacloud.matchup.HarmonyEnvironment;
import gov.nasa.gsfc.seadas.earthdatacloud.matchup.MatchupCollections;
import gov.nasa.gsfc.seadas.earthdatacloud.matchup.MatchupProduct;
import gov.nasa.gsfc.seadas.earthdatacloud.matchup.MatchupRequest;
import gov.nasa.gsfc.seadas.earthdatacloud.matchup.MatchupRun;
import gov.nasa.gsfc.seadas.earthdatacloud.matchup.SeabassFile;
import gov.nasa.gsfc.seadas.earthdatacloud.matchup.SeabassObservation;
import gov.nasa.gsfc.seadas.earthdatacloud.matchup.SeabassObservations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What the matchup window is working on: the SeaBASS file, the chosen
 * products and window, and the run. Changed on the event thread only; the
 * window's steps read it and listen for changes.
 */
final class MatchupSession {

    /**
     * Settings from the Earthdata Cloud options.
     *
     * @param maxInFlight most Harmony jobs at once
     * @param halfBoxDeg  half-width of each request's box, degrees
     */
    record Options(HarmonyEnvironment environment, int maxInFlight, double halfBoxDeg) {
        static Options defaults() {
            return new Options(HarmonyEnvironment.PRODUCTION, 10, MatchupRequest.DEFAULT_HALF_BOX_DEG);
        }
    }

    private final Options options;
    private final TokenSource tokens;
    private final List<Runnable> listeners = new ArrayList<>();
    private final Map<String, MatchupProduct> products = new ConcurrentHashMap<>();

    private Path seabass;
    private SeabassFile file;
    private List<SeabassObservation> observations = List.of();
    private List<MatchupCollections.Entry> chosen = List.of();
    private Duration halfWindow = MatchupRequest.DEFAULT_HALF_WINDOW;
    private List<String> variables = List.of();
    private MatchupRun run;
    private MatchupRun.Coverage coverage;

    MatchupSession(Options options, TokenSource tokens) {
        this.options = options;
        this.tokens = tokens;
    }

    // ------------------------------------------------------------ changes

    void addListener(Runnable listener) {
        listeners.add(listener);
    }

    void changed() {
        for (Runnable listener : List.copyOf(listeners)) {
            listener.run();
        }
    }

    /** Opens a SeaBASS file for a new run. */
    void open(Path path) throws IOException {
        SeabassFile read = SeabassFile.read(path);
        List<SeabassObservation> list = SeabassObservations.of(read);
        seabass = path.toAbsolutePath().normalize();
        file = read;
        observations = list;
        run = null;
        coverage = null;
        changed();
    }

    /** Opens a run record to carry on with it; its products and window become the session's. */
    void resume(Path record) throws IOException {
        MatchupRun loaded = MatchupRun.load(record);
        if (loaded.settings().environment() != options.environment()) {
            throw new IOException("This run used Harmony " + EnvironmentNames.name(loaded.settings().environment())
                    + "; the Earthdata Cloud options are set to " + EnvironmentNames.name(options.environment())
                    + ". Change the option to carry on with it.");
        }
        seabass = loaded.seabass();
        file = loaded.file();
        observations = loaded.observations();
        List<MatchupCollections.Entry> entries = new ArrayList<>();
        for (String id : loaded.settings().collections()) {
            entries.add(MatchupCollections.byId(id).orElseThrow(() ->
                    new IOException("The run's product " + id + " is not one this SeaDAS offers")));
        }
        chosen = List.copyOf(entries);
        halfWindow = loaded.settings().halfWindow();
        variables = loaded.settings().variables();
        run = loaded;
        coverage = null;
        changed();
    }

    void choose(List<MatchupCollections.Entry> entries) {
        requireNotStarted();
        chosen = List.copyOf(entries);
        if (!variables.isEmpty()) {
            variables = List.of();
        }
        settingsChanged();
    }

    void setHalfWindow(Duration window) {
        requireNotStarted();
        halfWindow = window;
        settingsChanged();
    }

    void setVariables(List<String> names) {
        requireNotStarted();
        variables = List.copyOf(names);
        settingsChanged();
    }

    private void settingsChanged() {
        coverage = null;
        changed();
    }

    private void requireNotStarted() {
        if (run != null && run.started()) {
            throw new IllegalStateException("The run has started; its settings are fixed");
        }
    }

    /**
     * The run for the current settings, made (and saved) if needed. A run that
     * never sent anything is replaced when the settings have changed.
     */
    MatchupRun ensureRun() throws IOException {
        MatchupRun.Settings settings = settings();
        if (run != null && run.settings().equals(settings)) {
            return run;
        }
        if (run != null) {
            if (run.started()) {
                throw new IllegalStateException("The run has started; its settings are fixed");
            }
            run.discard();
        }
        run = MatchupRun.create(seabass, settings, freeRecordPath(seabass));
        return run;
    }

    void setCoverage(MatchupRun.Coverage found) {
        coverage = found;
        changed();
    }

    // ------------------------------------------------------------ reading

    MatchupRun.Settings settings() {
        List<String> ids = chosen.stream().map(e -> e.id(options.environment())).toList();
        return new MatchupRun.Settings(options.environment(), ids, options.halfBoxDeg(), halfWindow, variables,
                options.maxInFlight());
    }

    Options options() {
        return options;
    }

    TokenSource tokens() {
        return tokens;
    }

    Path seabass() {
        return seabass;
    }

    SeabassFile file() {
        return file;
    }

    List<SeabassObservation> observations() {
        return observations;
    }

    long usableObservations() {
        return observations.stream().filter(SeabassObservation::usable).count();
    }

    List<MatchupCollections.Entry> chosen() {
        return chosen;
    }

    Duration halfWindow() {
        return halfWindow;
    }

    List<String> variables() {
        return variables;
    }

    MatchupRun run() {
        return run;
    }

    boolean started() {
        return run != null && run.started();
    }

    MatchupRun.Coverage coverage() {
        return coverage;
    }

    /** Products read from CMR so far, by concept ID (filled from worker threads). */
    Map<String, MatchupProduct> products() {
        return Collections.unmodifiableMap(products);
    }

    void addProduct(MatchupProduct product) {
        products.put(product.collectionId(), product);
    }

    // ------------------------------------------------------------ files

    /** Where a SeaBASS file's run record goes: {@code <name>.matchup-run.json} beside it. */
    static Path recordPath(Path seabass) {
        return seabass.resolveSibling(stem(seabass) + ".matchup-run.json");
    }

    /** {@link #recordPath}, or {@code <name>.matchup-run-2.json} and so on when taken. */
    static Path freeRecordPath(Path seabass) {
        Path path = recordPath(seabass);
        for (int n = 2; Files.exists(path); n++) {
            path = seabass.resolveSibling(stem(seabass) + ".matchup-run-" + n + ".json");
        }
        return path;
    }

    /** The newest existing record for a SeaBASS file, or null. */
    static Path latestRecord(Path seabass) {
        Path latest = null;
        Path path = recordPath(seabass);
        for (int n = 2; Files.exists(path); n++) {
            latest = path;
            path = seabass.resolveSibling(stem(seabass) + ".matchup-run-" + n + ".json");
        }
        return latest;
    }

    /** The default name of the augmented SeaBASS file: {@code <name>_matchup.sb}. */
    static Path outputPath(Path seabass, String suffix) {
        return seabass.resolveSibling(stem(seabass) + "_matchup" + suffix);
    }

    static String stem(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }
}
