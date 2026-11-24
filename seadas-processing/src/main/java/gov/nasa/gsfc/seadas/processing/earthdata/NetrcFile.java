package gov.nasa.gsfc.seadas.processing.earthdata;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Reads and writes one machine entry of a .netrc file, keeping every other line as it is.
 * <p>
 * An entry starts at a line beginning with {@code machine <host>} and runs until the next line
 * beginning with {@code machine}, {@code default} or {@code macdef}. SeaDAS writes the one-line form
 * {@code machine <host> login <user> password <password>}, which every .netrc reader understands.
 */
public final class NetrcFile {

    private static final Set<PosixFilePermission> OWNER_ONLY = PosixFilePermissions.fromString("rw-------");

    private final Path path;

    public NetrcFile(Path path) {
        this.path = path;
    }

    /** The .netrc file in the user's home folder, the one SeaDAS, OCSSW and the Docker setup read. */
    public static NetrcFile inHome() {
        return new NetrcFile(Paths.get(System.getProperty("user.home"), ".netrc"));
    }

    public Path getPath() {
        return path;
    }

    /** Returns {login, password} for the host, or null if the file or the entry is missing or incomplete. */
    public String[] read(String host) throws IOException {
        if (!Files.isRegularFile(path)) {
            return null;
        }
        String login = null;
        String password = null;
        boolean inEntry = false;
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            String[] tokens = line.trim().split("\\s+");
            if (startsEntry(tokens)) {
                if (inEntry) {
                    break;
                }
                inEntry = isEntryFor(tokens, host);
            }
            if (!inEntry) {
                continue;
            }
            for (int i = 0; i + 1 < tokens.length; i++) {
                if ("login".equals(tokens[i])) {
                    login = tokens[++i];
                } else if ("password".equals(tokens[i])) {
                    password = tokens[++i];
                }
            }
        }
        return login != null && password != null ? new String[]{login, password} : null;
    }

    /**
     * Sets the login and password for the host: replaces its entry, or adds one at the end. Other
     * entries are kept. The file is replaced in one step and only its owner can read it.
     */
    public void write(String host, String login, String password) throws IOException {
        checkToken("user name", login);
        checkToken("password", password);

        List<String> lines = Files.isRegularFile(path)
                ? Files.readAllLines(path, StandardCharsets.UTF_8) : new ArrayList<>();
        List<String> out = new ArrayList<>();
        String entry = "machine " + host + " login " + login + " password " + password;
        boolean inEntry = false;
        boolean written = false;
        for (String line : lines) {
            String[] tokens = line.trim().split("\\s+");
            if (startsEntry(tokens)) {
                inEntry = isEntryFor(tokens, host);
                if (inEntry && !written) {
                    out.add(entry);
                    written = true;
                }
            }
            if (!inEntry) {
                out.add(line);
            }
        }
        if (!written) {
            if (!out.isEmpty() && !out.get(out.size() - 1).trim().isEmpty()) {
                out.add("");
            }
            out.add(entry);
        }

        Path dir = path.toAbsolutePath().getParent();
        Files.createDirectories(dir);
        Path tmp = createOwnerOnlyFile(dir);
        try {
            Files.write(tmp, out, StandardCharsets.UTF_8);
            try {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(tmp);
        }
        restrictToOwner(path);
    }

    /** .netrc separates fields with white space and has no escaping, so values can't contain it. */
    static void checkToken(String what, String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("The " + what + " is empty.");
        }
        if (value.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("The " + what + " contains a space, which a .netrc file can't hold.");
        }
    }

    private static boolean startsEntry(String[] tokens) {
        return tokens.length > 0 && ("machine".equals(tokens[0]) || "default".equals(tokens[0]) || "macdef".equals(tokens[0]));
    }

    private static boolean isEntryFor(String[] tokens, String host) {
        return tokens.length >= 2 && "machine".equals(tokens[0]) && host.equals(tokens[1]);
    }

    private static Path createOwnerOnlyFile(Path dir) throws IOException {
        if (dir.getFileSystem().supportedFileAttributeViews().contains("posix")) {
            return Files.createTempFile(dir, ".netrc", ".tmp", PosixFilePermissions.asFileAttribute(OWNER_ONLY));
        }
        return Files.createTempFile(dir, ".netrc", ".tmp");
    }

    private static void restrictToOwner(Path file) throws IOException {
        if (file.getFileSystem().supportedFileAttributeViews().contains("posix")) {
            Files.setPosixFilePermissions(file, OWNER_ONLY);
        }
    }
}
