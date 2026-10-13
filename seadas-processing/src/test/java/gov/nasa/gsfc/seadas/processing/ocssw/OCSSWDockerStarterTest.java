package gov.nasa.gsfc.seadas.processing.ocssw;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class OCSSWDockerStarterTest {

    private static final String OCSSW_DIR = new File("ocssw-docker").getAbsolutePath();
    private static final String SHARED_DIR = new File("shared").getAbsolutePath();

    private static File onHost(String containerPath) {
        return OCSSWDockerStarter.containerPathOnHost(containerPath, OCSSW_DIR, SHARED_DIR);
    }

    @Test
    public void mapsOcsswPaths() {
        assertEquals(new File(OCSSW_DIR, "var" + File.separator + "anc" + File.separator + "x.nc"),
                onHost("/root/ocssw/var/anc/x.nc"));
        assertEquals(new File(OCSSW_DIR), onHost("/root/ocssw"));
    }

    @Test
    public void mapsSharedDirPaths() {
        assertEquals(new File(SHARED_DIR, "in.nc"), onHost("/root/seadasClientServerShared/in.nc"));
    }

    @Test
    public void acceptsWindowsSeparators() {
        // A File made from "/root/..." on Windows prints with backslashes
        assertEquals(new File(SHARED_DIR, "in.nc"), onHost("\\root\\seadasClientServerShared\\in.nc"));
    }

    @Test
    public void ignoresOtherPaths() {
        assertNull(onHost("/root/ocssw-other/x.nc"));
        assertNull(onHost("/tmp/x.nc"));
    }
}
