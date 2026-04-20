package gov.nasa.gsfc.seadas.metadata;

import gov.nasa.gsfc.seadas.metadata.ui.MetadataSelectionWatcher;
import org.openide.modules.ModuleInstall;

public class Installer extends ModuleInstall {
    private MetadataSelectionWatcher watcher;

    @Override
    public void restored() {
        //System.out.println("[Metadata] ModuleInstall restored()");
        // Start selection watcher (auto-opens dump on selection)
        watcher = new MetadataSelectionWatcher();
        watcher.start();
        //System.out.println("[Metadata] Selection watcher started");
        // Start product manager hook to attach metadata
        StartupHook.init();
    }

    @Override
    public void close() {
        if (watcher != null) watcher.stop();
        StartupHook.shutdown();
        //System.out.println("[Metadata] Module closed");
    }
}
