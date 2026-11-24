package gov.nasa.gsfc.seadas.processing.earthdata;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

import static org.junit.Assert.*;

public class NetrcFileTest {

    private static final String HOST = "urs.earthdata.nasa.gov";

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private Path netrc(String content) throws Exception {
        Path p = tmp.getRoot().toPath().resolve(".netrc");
        if (content != null) {
            Files.write(p, content.getBytes(StandardCharsets.UTF_8));
        }
        return p;
    }

    private static String text(Path p) throws Exception {
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
    }

    @Test
    public void createsTheFileOwnerOnly() throws Exception {
        Path p = netrc(null);
        new NetrcFile(p).write(HOST, "alice", "s3cret!");
        assertEquals("machine " + HOST + " login alice password s3cret!\n", text(p));
        if (p.getFileSystem().supportedFileAttributeViews().contains("posix")) {
            assertEquals("rw-------", PosixFilePermissions.toString(Files.getPosixFilePermissions(p)));
        }
        assertArrayEquals(new String[]{"alice", "s3cret!"}, new NetrcFile(p).read(HOST));
    }

    @Test
    public void replacesAMultiLineEntryAndKeepsTheOthers() throws Exception {
        Path p = netrc("machine example.com login bob password x\n"
                + "machine " + HOST + "\n"
                + "    login old\n"
                + "    password oldpw\n"
                + "machine other.org login carol password y\n");
        new NetrcFile(p).write(HOST, "alice", "new");
        assertEquals("machine example.com login bob password x\n"
                + "machine " + HOST + " login alice password new\n"
                + "machine other.org login carol password y\n", text(p));
    }

    @Test
    public void addsAnEntryAtTheEnd() throws Exception {
        Path p = netrc("machine example.com login bob password x");
        new NetrcFile(p).write(HOST, "alice", "pw");
        assertEquals("machine example.com login bob password x\n\nmachine " + HOST + " login alice password pw\n", text(p));
        assertArrayEquals(new String[]{"bob", "x"}, new NetrcFile(p).read("example.com"));
    }

    @Test
    public void keepsADefaultEntryAfterIt() throws Exception {
        Path p = netrc("machine " + HOST + " login old password oldpw\ndefault login anonymous password me@x\n");
        new NetrcFile(p).write(HOST, "alice", "pw");
        assertEquals("machine " + HOST + " login alice password pw\ndefault login anonymous password me@x\n", text(p));
    }

    @Test
    public void readsNothingWhenMissingOrIncomplete() throws Exception {
        assertNull(new NetrcFile(netrc(null)).read(HOST));
        assertNull(new NetrcFile(netrc("machine " + HOST + " login alice\n")).read(HOST));
        assertNull(new NetrcFile(netrc("machine example.com login a password b\n")).read(HOST));
    }

    @Test
    public void rejectsSpaces() {
        try {
            NetrcFile.checkToken("password", "two words");
            fail();
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("space"));
        }
        try {
            NetrcFile.checkToken("user name", "");
            fail();
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("empty"));
        }
    }
}
