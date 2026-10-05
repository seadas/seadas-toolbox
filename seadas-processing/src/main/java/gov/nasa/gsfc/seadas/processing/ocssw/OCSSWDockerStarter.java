package gov.nasa.gsfc.seadas.processing.ocssw;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.runtime.Version;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import gov.nasa.gsfc.seadas.processing.common.SeadasToolboxVersion;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.runtime.Config;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.prefs.Preferences;

import static gov.nasa.gsfc.seadas.processing.ocssw.OCSSWConfigData.*;

/**
 * Starts the OCSSW server container for OCSSW location "docker" by running
 * bin/start_ocssw_docker (Linux, macOS) or bin/start_ocssw_docker.ps1 (Windows)
 * from the SeaDAS installation.  The script pulls the seadas/ocssw-run image,
 * creates the OCSSW and shared directories, copies .netrc and starts the
 * container; afterwards OCSSWInfo is re-initialized so SeaDAS sees the server.
 */
public class OCSSWDockerStarter {

    private static final String SCRIPT_UNIX = "start_ocssw_docker";
    private static final String SCRIPT_WINDOWS = "start_ocssw_docker.ps1";
    // Full path of the start script, overriding the one in the installation's bin.
    private static final String SCRIPT_PROPERTY = "seadas.ocssw.docker.script";
    private static final String IMAGE_REPOSITORY = "seadas/ocssw-run";
    private static final String LOG_FILE_NAME = "ocssw_docker_start.log";
    private static final String TITLE = "OCSSW Docker";

    // Exit codes of the start scripts.
    private static final int E_NO_DOCKER = 2;
    private static final int E_DAEMON = 3;
    private static final int E_PULL = 4;

    private static final AtomicBoolean starting = new AtomicBoolean(false);

    public static boolean isDockerLocation() {
        Preferences preferences = Config.instance("seadas").load().preferences();
        return OCSSWInfo.OCSSW_LOCATION_DOCKER.equals(preferences.get(OCSSWInfo.OCSSW_LOCATION_PROPERTY, null));
    }

    /**
     * Makes sure the OCSSW server is reachable before OCSSW is used.  Does nothing
     * unless the OCSSW location is docker and the server is not answering.
     * OCSSWInfo only checks the server when it is created, so the port is probed
     * as well: the container may have stopped since.
     *
     * @return false if the server is still not reachable; the user has been told why
     */
    public static boolean ensureServer(Window parent) {
        if (!isDockerLocation() || (OCSSWInfo.getInstance().isOcsswServerUp() && isServerPortOpen())) {
            return true;
        }
        return start(parent);
    }

    private static boolean isServerPortOpen() {
        Preferences preferences = Config.instance("seadas").load().preferences();
        try (Socket socket = new Socket()) {
            int port = Integer.parseInt(preferences.get(SEADAS_OCSSW_PORT_PROPERTY, SEADAS_OCSSW_PORT_DEFAULT_VALUE).trim());
            socket.connect(new InetSocketAddress(OCSSWInfo.DOCKER_SERVER_API, port), 2000);
            return true;
        } catch (IOException | NumberFormatException e) {
            return false;
        }
    }

    /**
     * Runs the start script with a progress dialog and blocks until it finishes.
     * Must be called on the event dispatch thread.
     */
    public static boolean start(Window parent) {
        if (!starting.compareAndSet(false, true)) {
            return false;
        }
        try {
            File script = scriptFile();
            if (!script.isFile()) {
                Dialogs.showError(TITLE, "Cannot find " + script + ".\n"
                        + "To use a copy elsewhere, set -D" + SCRIPT_PROPERTY + "=<path to " + scriptName() + ">.\n"
                        + "Or start the OCSSW Docker container yourself, see the SeaDAS help on OCSSW in Docker.");
                return false;
            }

            final List<String> command = buildCommand(script);
            final StringBuilder output = new StringBuilder();
            ProgressMonitorSwingWorker<Integer, Void> worker = new ProgressMonitorSwingWorker<Integer, Void>(parent, "Starting OCSSW in Docker") {
                @Override
                protected Integer doInBackground(ProgressMonitor pm) throws Exception {
                    pm.beginTask("Starting the OCSSW Docker container", ProgressMonitor.UNKNOWN);
                    Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            output.append(line).append('\n');
                            if (line.startsWith("==> ")) {
                                pm.setSubTaskName(line.substring(4));
                            }
                            if (pm.isCanceled()) {
                                process.destroy();
                                break;
                            }
                        }
                    }
                    int exitCode = process.waitFor();
                    pm.done();
                    return pm.isCanceled() ? -1 : exitCode;
                }
            };
            worker.executeWithBlocking();

            int exitCode;
            try {
                exitCode = worker.get();
            } catch (Exception e) {
                output.append(e).append('\n');
                exitCode = 1;
            }
            File logFile = writeLog(command, output, exitCode);

            if (exitCode == -1) {
                return false;
            }
            if (exitCode != 0) {
                Dialogs.showError(TITLE, failureMessage(exitCode, output, logFile));
                return false;
            }

            OCSSWInfo.updateOCSSWInfo();
            if (!OCSSWInfo.getInstance().isOcsswServerUp()) {
                Dialogs.showError(TITLE, "The OCSSW Docker container started, but SeaDAS cannot reach the OCSSW server.\n"
                        + "Check the server port in the OCSSW configuration.\nDetails: " + logFile);
                return false;
            }
            return true;
        } finally {
            starting.set(false);
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

    private static String scriptName() {
        return isWindows() ? SCRIPT_WINDOWS : SCRIPT_UNIX;
    }

    /**
     * The start script: the file named by -Dseadas.ocssw.docker.script if set,
     * otherwise bin/start_ocssw_docker[.ps1] in the SeaDAS installation.  The
     * property is for development runs, whose application home (for example
     * snap-desktop/snap-application/target/snap) has no installer scripts.
     */
    private static File scriptFile() {
        String path = System.getProperty(SCRIPT_PROPERTY);
        if (path != null && !path.trim().isEmpty()) {
            return new File(path.trim());
        }
        return new File(new File(SystemUtils.getApplicationHomeDir(), "bin"), scriptName());
    }

    private static List<String> buildCommand(File script) {
        Preferences preferences = Config.instance("seadas").load().preferences();
        String image = preferences.get(SEADAS_OCSSW_DOCKER_IMAGE_PROPERTY, defaultImage());
        String ocsswDir = preferences.get(SEADAS_OCSSW_DOCKER_DIR_PROPERTY, getOcsswDockerDirDefaultValue());
        String sharedDir = preferences.get(SEADAS_CLIENT_SERVER_SHARED_DIR_PROPERTY, getSeadasClientServerSharedDirDefaultValue());
        String port = preferences.get(SEADAS_OCSSW_PORT_PROPERTY, SEADAS_OCSSW_PORT_DEFAULT_VALUE);
        String inputPort = preferences.get(SEADAS_OCSSW_PROCESSINPUTSTREAMPORT_PROPERTY, SEADAS_OCSSW_PROCESSINPUTSTREAMPORT_DEFAULT_VALUE);
        String errorPort = preferences.get(SEADAS_OCSSW_PROCESSERRORSTREAMPORT_PROPERTY, SEADAS_OCSSW_PROCESSERRORSTREAMPORT_DEFAULT_VALUE);

        List<String> command = new ArrayList<>();
        if (isWindows()) {
            command.add("powershell.exe");
            command.add("-NoProfile");
            command.add("-ExecutionPolicy");
            command.add("Bypass");
            command.add("-File");
            command.add(script.getAbsolutePath());
            addOption(command, "-Image", image);
            addOption(command, "-OcsswDir", ocsswDir);
            addOption(command, "-SharedDir", sharedDir);
            addOption(command, "-Port", port);
            addOption(command, "-InputPort", inputPort);
            addOption(command, "-ErrorPort", errorPort);
        } else {
            command.add("/bin/bash");
            command.add(script.getAbsolutePath());
            addOption(command, "--image", image);
            addOption(command, "--ocssw-dir", ocsswDir);
            addOption(command, "--shared-dir", sharedDir);
            addOption(command, "--port", port);
            addOption(command, "--input-port", inputPort);
            addOption(command, "--error-port", errorPort);
        }
        return command;
    }

    private static void addOption(List<String> command, String option, String value) {
        if (value != null && !value.trim().isEmpty()) {
            command.add(option);
            command.add(value.trim());
        }
    }

    /**
     * seadas/ocssw-run:<installed SeaDAS Toolbox version>, e.g. seadas/ocssw-run:12.0.0,
     * or null to let the script use its own default.
     */
    private static String defaultImage() {
        try {
            Version version = SeadasToolboxVersion.getSeadasToolboxInstalledVersion();
            return IMAGE_REPOSITORY + ":" + version.getMajor() + "." + version.getMinor() + "." + version.getMicro();
        } catch (Exception e) {
            return null;
        }
    }

    private static File writeLog(List<String> command, StringBuilder output, int exitCode) {
        File logFile = new File(OCSSWInfo.getInstance().getLogDirPath(), LOG_FILE_NAME);
        try {
            Files.write(logFile.toPath(), (String.join(" ", command) + "\n\n" + output + "\nexit code: " + exitCode + "\n")
                    .getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            SystemUtils.LOG.warning("Cannot write " + logFile + ": " + e.getMessage());
        }
        return logFile;
    }

    private static String failureMessage(int exitCode, StringBuilder output, File logFile) {
        StringBuilder message = new StringBuilder("SeaDAS could not start the OCSSW Docker container.\n\n");
        // The script's own ERROR: lines say what went wrong and how to fix it.
        for (String line : output.toString().split("\n")) {
            if (line.startsWith("ERROR: ")) {
                message.append(line.substring(7)).append('\n');
            }
        }
        if (exitCode == E_NO_DOCKER || exitCode == E_DAEMON) {
            message.append("\nSeaDAS will try again the next time you run an OCSSW processor.\n");
        } else if (exitCode == E_PULL) {
            message.append("\nCheck your internet connection.\n");
        }
        message.append("\nDetails: ").append(logFile);
        return message.toString();
    }
}
