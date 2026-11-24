package gov.nasa.gsfc.seadas.processing.ocssw;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Checks that this Mac's macOS is new enough for a local OCSSW. SeaDAS itself runs on older
 * macOS versions than OCSSW does, so only the processors are checked.
 * <p>
 * Before installing, the minimum of the current OCSSW release is used. Once OCSSW is installed,
 * the minimum is read from its own files (the macOS version they were built for), so older or
 * newer OCSSW versions are judged by what they actually need.
 */
public final class MacOcsswVersionCheck {

    /** Minimum macOS of the current OCSSW Mac release (V2026.5). Update with OCSSW. */
    static final int CURRENT_OCSSW_MIN_MACOS = 26;

    /** Linked by every OCSSW program; its minimum macOS stands for the whole installation. */
    private static final String PROBE_LIBRARY = "lib" + File.separator + "libgenutils.dylib";

    private static final String TITLE = "SeaDAS Processors";
    private static final Logger LOG = Logger.getLogger(MacOcsswVersionCheck.class.getName());

    private MacOcsswVersionCheck() {
    }

    /**
     * Before installing OCSSW: on a Mac older than the current OCSSW release needs, says so and
     * asks whether to go on (an older OCSSW version may still work). Returns false to stop.
     */
    public static boolean confirmInstall(Component parent) {
        if (!MacPythonSetup.isMac()) {
            return true;
        }
        int[] running = runningMacOS();
        if (running == null || running[0] >= CURRENT_OCSSW_MIN_MACOS) {
            return true;
        }
        String message = "<html><body style='width: 420px'>The current SeaDAS processors (OCSSW) need macOS "
                + CURRENT_OCSSW_MIN_MACOS + " or later. This Mac has macOS " + format(running) + ".<br><br>"
                + "To process data on this Mac, update macOS, or set the OCSSW location to Docker "
                + "(SeaDAS-Toolbox &gt; SeaDAS Processor Location). Viewing data works without OCSSW.<br><br>"
                + "Continue only if you are installing an older OCSSW version made for this macOS.</body></html>";
        Object[] options = {"Cancel", "Continue Anyway"};
        int answer = JOptionPane.showOptionDialog(parent, message, TITLE, JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE, null, options, options[0]);
        return answer == 1;
    }

    /**
     * Before running a processor: if the installed OCSSW was built for a newer macOS than this
     * Mac has, says so and returns false.
     */
    public static boolean checkInstalled(Component parent, String ocsswRoot) {
        if (!MacPythonSetup.isMac() || ocsswRoot == null) {
            return true;
        }
        int[] running = runningMacOS();
        int[] required = requiredMacOS(new File(ocsswRoot, PROBE_LIBRARY));
        if (running == null || required == null || compare(running, required) >= 0) {
            return true;
        }
        JOptionPane.showMessageDialog(parent,
                "<html><body style='width: 420px'>The installed SeaDAS processors (OCSSW) need macOS "
                        + format(required) + " or later. This Mac has macOS " + format(running) + ".<br><br>"
                        + "To process data on this Mac, update macOS, or set the OCSSW location to Docker "
                        + "(SeaDAS-Toolbox &gt; SeaDAS Processor Location).</body></html>",
                TITLE, JOptionPane.ERROR_MESSAGE);
        return false;
    }

    /**
     * This Mac's macOS version as {major, minor}, or null if unknown. Asks sw_vers, because Java
     * built with an older SDK may see a compatibility version (macOS 26 reports itself as 16).
     */
    static int[] runningMacOS() {
        String version = null;
        try {
            Process process = new ProcessBuilder("/usr/bin/sw_vers", "-productVersion").redirectErrorStream(true).start();
            if (process.waitFor(10, TimeUnit.SECONDS) && process.exitValue() == 0) {
                version = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            } else {
                process.destroyForcibly();
            }
        } catch (IOException e) {
            LOG.log(Level.FINE, "sw_vers failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (version == null || version.isEmpty()) {
            version = System.getProperty("os.version", "");
        }
        int[] parsed = parse(version);
        if (parsed != null && parsed[0] == 16) {
            parsed[0] = 26;  // there is no macOS 16: it's macOS 26 seen through the compatibility version
        }
        return parsed;
    }

    /** The minimum macOS a 64-bit Mach-O file was built for, as {major, minor}, or null. */
    static int[] requiredMacOS(File file) {
        if (!file.isFile()) {
            return null;
        }
        try (RandomAccessFile in = new RandomAccessFile(file, "r")) {
            byte[] head = new byte[(int) Math.min(in.length(), 65536)];
            in.readFully(head);
            ByteBuffer b = ByteBuffer.wrap(head).order(ByteOrder.LITTLE_ENDIAN);
            if (head.length < 32 || b.getInt(0) != 0xfeedfacf) {
                return null;
            }
            int commands = b.getInt(16);
            int offset = 32;
            for (int i = 0; i < commands && offset + 16 <= head.length; i++) {
                int command = b.getInt(offset);
                int size = b.getInt(offset + 4);
                if (command == 0x32) {          // LC_BUILD_VERSION: platform, minos, sdk
                    return decode(b.getInt(offset + 12));
                }
                if (command == 0x24) {          // LC_VERSION_MIN_MACOSX: version, sdk
                    return decode(b.getInt(offset + 8));
                }
                if (size <= 0) {
                    break;
                }
                offset += size;
            }
        } catch (IOException e) {
            LOG.log(Level.FINE, "Can't read " + file, e);
        }
        return null;
    }

    private static int[] decode(int version) {
        return new int[]{version >>> 16, (version >>> 8) & 0xff};
    }

    static int[] parse(String version) {
        String[] parts = version.trim().split("\\.");
        try {
            return new int[]{Integer.parseInt(parts[0]), parts.length > 1 ? Integer.parseInt(parts[1]) : 0};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static int compare(int[] a, int[] b) {
        return a[0] != b[0] ? Integer.compare(a[0], b[0]) : Integer.compare(a[1], b[1]);
    }

    static String format(int[] version) {
        return version[1] == 0 ? String.valueOf(version[0]) : version[0] + "." + version[1];
    }
}
