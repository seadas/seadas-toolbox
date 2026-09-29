package gov.nasa.gsfc.seadas.processing.ocssw;

import org.esa.snap.rcp.SnapApp;
import org.openide.windows.OnShowing;

import javax.swing.*;

/**
 * When SeaDAS starts with OCSSW location "docker" and the OCSSW server is not
 * answering, starts the OCSSW Docker container (see {@link OCSSWDockerStarter}).
 */
@OnShowing
public class OCSSWDockerAutoStart implements Runnable {

    @Override
    public void run() {
        if (!OCSSWDockerStarter.isDockerLocation()) {
            return;
        }
        // Contacting the server can take a moment; keep it off the event dispatch thread.
        Thread thread = new Thread(() -> {
            if (!OCSSWInfo.getInstance().isOcsswServerUp()) {
                SwingUtilities.invokeLater(() -> OCSSWDockerStarter.start(SnapApp.getDefault().getMainFrame()));
            }
        }, "OCSSW Docker auto-start");
        thread.setDaemon(true);
        thread.start();
    }
}
