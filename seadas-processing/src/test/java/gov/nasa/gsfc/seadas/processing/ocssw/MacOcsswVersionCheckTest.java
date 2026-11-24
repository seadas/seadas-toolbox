package gov.nasa.gsfc.seadas.processing.ocssw;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;

import static org.junit.Assert.*;

public class MacOcsswVersionCheckTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    /** A 64-bit Mach-O header with one load command. */
    private File machO(int command, int versionOffset, int version) throws Exception {
        int size = 24;
        ByteBuffer b = ByteBuffer.allocate(32 + size).order(ByteOrder.LITTLE_ENDIAN);
        b.putInt(0xfeedfacf).putInt(0x0100000c).putInt(0).putInt(6).putInt(1).putInt(size).putInt(0).putInt(0);
        b.putInt(32, command).putInt(36, size).putInt(32 + versionOffset, version);
        File f = tmp.newFile();
        Files.write(f.toPath(), b.array());
        return f;
    }

    @Test
    public void readsBuildVersion() throws Exception {
        // LC_BUILD_VERSION: cmd, cmdsize, platform, minos (26.0.0)
        assertArrayEquals(new int[]{26, 0}, MacOcsswVersionCheck.requiredMacOS(machO(0x32, 12, 26 << 16)));
    }

    @Test
    public void readsOldVersionMin() throws Exception {
        // LC_VERSION_MIN_MACOSX: cmd, cmdsize, version (10.13.0)
        assertArrayEquals(new int[]{10, 13}, MacOcsswVersionCheck.requiredMacOS(machO(0x24, 8, (10 << 16) | (13 << 8))));
    }

    @Test
    public void ignoresOtherFiles() throws Exception {
        File text = tmp.newFile();
        Files.write(text.toPath(), "#!/bin/sh\n".getBytes());
        assertNull(MacOcsswVersionCheck.requiredMacOS(text));
        assertNull(MacOcsswVersionCheck.requiredMacOS(new File(tmp.getRoot(), "missing")));
    }

    @Test
    public void parsesAndCompares() {
        assertArrayEquals(new int[]{15, 5}, MacOcsswVersionCheck.parse("15.5.1"));
        assertArrayEquals(new int[]{26, 0}, MacOcsswVersionCheck.parse("26"));
        assertNull(MacOcsswVersionCheck.parse("x"));
        assertTrue(MacOcsswVersionCheck.compare(new int[]{15, 7}, new int[]{26, 0}) < 0);
        assertEquals(0, MacOcsswVersionCheck.compare(new int[]{26, 0}, new int[]{26, 0}));
        assertEquals("26", MacOcsswVersionCheck.format(new int[]{26, 0}));
        assertEquals("15.5", MacOcsswVersionCheck.format(new int[]{15, 5}));
    }
}
