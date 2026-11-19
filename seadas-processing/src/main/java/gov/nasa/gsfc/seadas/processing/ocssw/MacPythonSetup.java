package gov.nasa.gsfc.seadas.processing.ocssw;

import javax.swing.*;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Sets up the Python that a local OCSSW needs on macOS.
 * <p>
 * The OCSSW installer is a Python 3 script that needs the {@code requests} library. On a new Mac,
 * {@code /usr/bin/python3} is only a placeholder: running it makes macOS ask to install Apple's
 * Command Line Tools. Before the installer first runs Python, this class checks both, and offers
 * to install whichever is missing. Commands run in a bash login shell, as OCSSW's do, so the
 * checks see the same {@code python3}.
 */
public final class MacPythonSetup {

    private static final Logger LOG = Logger.getLogger(MacPythonSetup.class.getName());

    private static final String TITLE = "Python for SeaDAS Processors";
    private static final String SHIM_PYTHON = "/usr/bin/python3";
    private static final String PIP_COMMAND = "python3 -m pip install --user requests";
    private static final long CHECK_TIMEOUT_SECONDS = 60;
    private static final long PIP_TIMEOUT_SECONDS = 600;
    private static final long TOOLS_WAIT_MINUTES = 60;

    private MacPythonSetup() {
    }

    public static boolean isMac() {
        return System.getProperty("os.name", "").toLowerCase().startsWith("mac");
    }

    /**
     * True on a Mac whose python3 is Apple's placeholder and the Command Line Tools are missing,
     * so running python3 would only bring up Apple's install prompt.
     */
    public static boolean pythonNeedsCommandLineTools() {
        if (!isMac()) {
            return false;
        }
        Result which = loginShell("command -v python3", CHECK_TIMEOUT_SECONDS, null);
        String path = which.exitCode == 0 ? which.output.trim() : "";
        return (path.isEmpty() || path.equals(SHIM_PYTHON)) && !hasCommandLineTools();
    }

    /**
     * On macOS, makes sure python3 and its requests library are there before the OCSSW installer
     * runs, offering to install what is missing. Returns false if the user cancelled or a step
     * failed (the user has been told why). Elsewhere it returns true at once.
     */
    public static boolean ensurePythonForOcssw(Component parent) {
        if (!isMac()) {
            return true;
        }
        if (pythonNeedsCommandLineTools() && !installCommandLineTools(parent)) {
            return false;
        }
        return hasRequests() || installRequests(parent);
    }

    private static boolean hasCommandLineTools() {
        Result result = run(new String[]{"/usr/bin/xcode-select", "-p"}, CHECK_TIMEOUT_SECONDS, null);
        return result.exitCode == 0 && new File(result.output.trim()).isDirectory();
    }

    private static boolean hasRequests() {
        return loginShell("python3 -c 'import requests'", CHECK_TIMEOUT_SECONDS, null).exitCode == 0;
    }

    private static boolean installCommandLineTools(Component parent) {
        int choice = JOptionPane.showOptionDialog(parent,
                "<html><body style='width: 380px'>"
                        + "<p>SeaDAS processors (OCSSW) need Python 3. On macOS it comes with Apple's "
                        + "Command Line Tools, which aren't installed on this Mac.</p><br>"
                        + "<p>Click <b>Install</b> to open Apple's installer, then click <b>Install</b> "
                        + "in Apple's window too. The download takes several minutes; SeaDAS continues "
                        + "when it's done.</p></body></html>",
                TITLE, JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE, null,
                new Object[]{"Install", "Cancel"}, "Install");
        if (choice != 0) {
            return false;
        }

        LOG.info("Starting Apple's Command Line Tools installer");
        run(new String[]{"/usr/bin/xcode-select", "--install"}, CHECK_TIMEOUT_SECONDS, null);

        long deadline = System.nanoTime() + TimeUnit.MINUTES.toNanos(TOOLS_WAIT_MINUTES);
        boolean installed = runWithWaitDialog(parent,
                "<html><body style='width: 340px'>Waiting for Apple's Command Line Tools installer "
                        + "to finish.<br><br>If you don't see Apple's window, check behind SeaDAS "
                        + "or in the Dock.</body></html>",
                cancelled -> {
                    while (!cancelled.getAsBoolean() && System.nanoTime() < deadline) {
                        if (hasCommandLineTools()) {
                            return true;
                        }
                        sleep(3000);
                    }
                    return false;
                });

        if (installed) {
            LOG.info("Command Line Tools are installed");
            return true;
        }
        JOptionPane.showMessageDialog(parent,
                "<html><body style='width: 380px'>The Command Line Tools aren't installed, so SeaDAS "
                        + "processors can't be installed yet.<br><br>To install them yourself, run "
                        + "<b>xcode-select --install</b> in Terminal, then choose Install/Update "
                        + "SeaDAS Processors again.</body></html>",
                TITLE, JOptionPane.WARNING_MESSAGE);
        return false;
    }

    private static boolean installRequests(Component parent) {
        int choice = JOptionPane.showOptionDialog(parent,
                "<html><body style='width: 380px'>The OCSSW installer needs the Python library "
                        + "<b>requests</b>, which isn't installed.<br><br>Click <b>Install</b> to "
                        + "install it for your account (<b>" + PIP_COMMAND + "</b>). "
                        + "This needs internet access.</body></html>",
                TITLE, JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE, null,
                new Object[]{"Install", "Cancel"}, "Install");
        if (choice != 0) {
            return false;
        }

        LOG.info("Running: " + PIP_COMMAND);
        Result[] pip = new Result[1];
        boolean installed = runWithWaitDialog(parent, "Installing the Python library requests ...",
                cancelled -> {
                    pip[0] = loginShell(PIP_COMMAND, PIP_TIMEOUT_SECONDS, cancelled);
                    return pip[0].exitCode == 0 && hasRequests();
                });

        if (installed) {
            LOG.info("requests is installed");
            return true;
        }
        String output = pip[0] == null ? "" : tail(pip[0].output, 12);
        LOG.warning("Installing requests failed:\n" + output);
        JTextArea details = new JTextArea(output, 10, 60);
        details.setEditable(false);
        details.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.add(new JLabel("<html><body style='width: 420px'>Installing the Python library requests "
                + "failed. Install it yourself, for example with <b>" + PIP_COMMAND + "</b> in Terminal, "
                + "then choose Install/Update SeaDAS Processors again.</body></html>"), BorderLayout.NORTH);
        if (!output.isEmpty()) {
            panel.add(new JScrollPane(details), BorderLayout.CENTER);
        }
        JOptionPane.showMessageDialog(parent, panel, TITLE, JOptionPane.WARNING_MESSAGE);
        return false;
    }

    /**
     * Runs task off the event thread while a modal dialog with a Cancel button is shown.
     * Returns the task's result, or false if the user cancelled.
     */
    private static boolean runWithWaitDialog(Component parent, String message, Task task) {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        if (owner == null && parent instanceof Window) {
            owner = (Window) parent;
        }
        JDialog dialog = new JDialog(owner, TITLE, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        AtomicBoolean cancelled = new AtomicBoolean(false);
        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> {
            cancelled.set(true);
            dialog.dispose();
        });

        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        panel.add(new JLabel(message), BorderLayout.NORTH);
        panel.add(bar, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttons.add(cancel);
        panel.add(buttons, BorderLayout.SOUTH);
        dialog.setContentPane(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(owner);

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                return task.run(cancelled::get);
            }

            @Override
            protected void done() {
                dialog.dispose();
            }
        };
        worker.execute();
        dialog.setVisible(true);   // blocks until done() or Cancel closes it

        if (cancelled.get()) {
            return false;
        }
        try {
            return worker.get();
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Python setup step failed", e);
            return false;
        }
    }

    private interface Task {
        boolean run(BooleanSupplier cancelled);
    }

    private static final class Result {
        final int exitCode;
        final String output;

        Result(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output;
        }
    }

    private static Result loginShell(String command, long timeoutSeconds, BooleanSupplier cancelled) {
        return run(new String[]{"/bin/bash", "-l", "-c", command}, timeoutSeconds, cancelled);
    }

    /** Runs a command with stderr merged into stdout. Exit code -1 if it couldn't run, timed out or was cancelled. */
    private static Result run(String[] command, long timeoutSeconds, BooleanSupplier cancelled) {
        Process process;
        try {
            process = new ProcessBuilder(command).redirectErrorStream(true).start();
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Could not run " + String.join(" ", command), e);
            return new Result(-1, e.getMessage() == null ? "" : e.getMessage());
        }
        try {
            process.getOutputStream().close();   // nothing to send; a prompt then fails instead of waiting
        } catch (IOException ignored) {
            // nothing to close
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Thread reader = new Thread(() -> copy(process.getInputStream(), out), "MacPythonSetup-output");
        reader.setDaemon(true);
        reader.start();
        try {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
            while (!process.waitFor(500, TimeUnit.MILLISECONDS)) {
                if ((cancelled != null && cancelled.getAsBoolean()) || System.nanoTime() > deadline) {
                    process.destroyForcibly();
                    reader.join(2000);
                    return new Result(-1, out.toString(StandardCharsets.UTF_8));
                }
            }
            reader.join(2000);
            return new Result(process.exitValue(), out.toString(StandardCharsets.UTF_8));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            return new Result(-1, out.toString(StandardCharsets.UTF_8));
        }
    }

    private static void copy(InputStream in, ByteArrayOutputStream out) {
        byte[] buffer = new byte[8192];
        try (in) {
            int n;
            while ((n = in.read(buffer)) != -1) {
                synchronized (out) {
                    out.write(buffer, 0, n);
                }
            }
        } catch (IOException ignored) {
            // the process ended or was killed
        }
    }

    private static String tail(String text, int lines) {
        String[] all = text.strip().split("\n");
        int from = Math.max(0, all.length - lines);
        return String.join("\n", java.util.Arrays.copyOfRange(all, from, all.length));
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
