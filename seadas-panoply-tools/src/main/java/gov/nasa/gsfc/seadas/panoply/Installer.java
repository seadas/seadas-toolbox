package gov.nasa.gsfc.seadas.panoply;

import gov.nasa.gsfc.seadas.panoply.ui.PanoplySelectionWatcher;
import org.openide.modules.ModuleInstall;

public class Installer extends ModuleInstall {
    private PanoplySelectionWatcher watcher;

    @Override
    public void restored() {
        System.out.println("[Panoply] ModuleInstall restored()");
        // Start selection watcher (auto-opens dump on selection)
        watcher = new PanoplySelectionWatcher();
        watcher.start();
        System.out.println("[Panoply] Selection watcher started");
        // Start product manager hook to attach metadata
        StartupHook.init();
    }

    @Override
    public void close() {
        if (watcher != null) watcher.stop();
        StartupHook.shutdown();
        System.out.println("[Panoply] Module closed");
    }
}
