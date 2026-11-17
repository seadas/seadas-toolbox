package gov.nasa.gsfc.seadas.metadata;

import org.netbeans.api.autoupdate.OperationContainer;
import org.netbeans.api.autoupdate.OperationException;
import org.netbeans.api.autoupdate.OperationSupport;
import org.netbeans.api.autoupdate.UpdateElement;
import org.netbeans.api.autoupdate.UpdateManager;
import org.netbeans.api.autoupdate.UpdateUnit;
import org.openide.modules.ModuleInfo;
import org.openide.util.Lookup;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Switches off the SeaDAS 11 "panoply.tools" module if it is still installed.
 * <p>
 * SeaDAS 11 shipped the Metadata Dump tool as {@code panoply.tools}; SeaDAS 12 renamed it to
 * {@code metadata.tools}. An old copy can survive an upgrade (in the install folder or in the
 * user folder ~/.seadas). Then both modules run, and the old one's product-close listener throws
 * a NullPointerException, which stops SNAP from removing closed products from the Product Explorer.
 * <p>
 * This removes the old listener for the current session and disables the old module for later ones.
 */
final class LegacyModuleGuard {

    static final String LEGACY_MODULE = "gov.nasa.gsfc.seadas.seadas.panoply.tools";
    private static final String LEGACY_STARTUP_HOOK = "gov.nasa.gsfc.seadas.panoply.StartupHook";
    private static final Logger LOG = Logger.getLogger(LegacyModuleGuard.class.getName());

    private LegacyModuleGuard() {
    }

    static void run() {
        ModuleInfo legacy = findEnabledModule(LEGACY_MODULE);
        if (legacy == null) {
            return;
        }
        LOG.warning("Found obsolete module " + LEGACY_MODULE + " " + legacy.getSpecificationVersion()
                + " (replaced by seadas-metadata-tools). Disabling it.");
        removeLegacyProductListener(legacy);
        disableModule(LEGACY_MODULE);
    }

    private static ModuleInfo findEnabledModule(String codeNameBase) {
        for (ModuleInfo module : Lookup.getDefault().lookupAll(ModuleInfo.class)) {
            if (codeNameBase.equals(module.getCodeNameBase()) && module.isEnabled()) {
                return module;
            }
        }
        return null;
    }

    /** Calls the old module's own StartupHook.shutdown(), which removes its ProductManager listener. */
    private static void removeLegacyProductListener(ModuleInfo legacy) {
        try {
            Class<?> hook = Class.forName(LEGACY_STARTUP_HOOK, true, legacy.getClassLoader());
            hook.getMethod("shutdown").invoke(null);
            LOG.info("Removed the product listener of " + LEGACY_MODULE);
        } catch (ReflectiveOperationException | LinkageError e) {
            LOG.log(Level.WARNING, "Could not remove the product listener of " + LEGACY_MODULE, e);
        }
    }

    /** Disables the module the same way Tools > Plugins does, so it stays off after a restart. */
    private static void disableModule(String codeNameBase) {
        try {
            for (UpdateUnit unit : UpdateManager.getDefault().getUpdateUnits(UpdateManager.TYPE.MODULE)) {
                if (!codeNameBase.equals(unit.getCodeName())) {
                    continue;
                }
                UpdateElement installed = unit.getInstalled();
                if (installed == null) {
                    return;
                }
                OperationContainer<OperationSupport> container = OperationContainer.createForDirectDisable();
                if (!container.canBeAdded(unit, installed)) {
                    LOG.warning("Can't disable " + codeNameBase + "; remove it with Tools > Plugins.");
                    return;
                }
                container.add(installed);
                container.getSupport().doOperation(null);
                LOG.info("Disabled " + codeNameBase);
                return;
            }
        } catch (OperationException | RuntimeException e) {
            LOG.log(Level.WARNING, "Could not disable " + codeNameBase + "; remove it with Tools > Plugins.", e);
        }
    }
}
