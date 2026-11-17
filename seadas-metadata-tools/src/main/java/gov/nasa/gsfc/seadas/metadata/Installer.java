package gov.nasa.gsfc.seadas.metadata;

import gov.nasa.gsfc.seadas.metadata.ui.MetadataSelectionWatcher;
import org.openide.modules.ModuleInstall;
import org.openide.util.RequestProcessor;
import org.openide.windows.WindowManager;

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
        // Once all modules are up, switch off a leftover SeaDAS 11 panoply module (see LegacyModuleGuard).
        WindowManager.getDefault().invokeWhenUIReady(
                () -> RequestProcessor.getDefault().post(LegacyModuleGuard::run));
    }

    @Override
    public void close() {
        if (watcher != null) watcher.stop();
        StartupHook.shutdown();
        //System.out.println("[Metadata] Module closed");
    }
}
