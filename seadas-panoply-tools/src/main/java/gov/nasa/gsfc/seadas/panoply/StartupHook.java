package gov.nasa.gsfc.seadas.panoply;

import gov.nasa.gsfc.seadas.panoply.ui.MetadataDumpTopComponent;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductManager;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.rcp.SnapApp;
import org.openide.nodes.Node;
import org.openide.util.WeakListeners;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.lang.reflect.Method;
import java.util.*;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

public final class StartupHook {

    private static ProductManager.Listener listener;
    private static ProductManager pm;
    private static final AtomicBoolean CLOSING_SNAP_METADATA = new AtomicBoolean(false);


    private StartupHook() {}

    public static void init() {
        if (listener != null) {
            //System.out.println("[Panoply] StartupHook already initialized");
            return;
        }
        pm = resolveProductManager();
        if (pm == null) {
            System.out.println("[Panoply] ERROR: Could not resolve ProductManager; Panoply hook disabled");
            return;
        }

        listener = new ProductManager.Listener() {
            @Override
            public void productAdded(ProductManager.Event e) {
                Product product = e.getProduct();
                //System.out.println("[Panoply] productAdded: " + safeName(product));

                File f = product.getFileLocation();
                String filePath = (f != null ? f.getAbsolutePath() : null);
                //System.out.println("[Panoply] Source for " + safeName(product) + ": " + filePath);

                if (filePath != null) {
                    try {
                        //System.out.println("[Panoply] Attaching to " + product.getName() + " from " + filePath);
                        //PanoplyStyleMetadataBuilder.attachPanoplyMetadata(product, filePath);
                        PanoplyStyleMetadataBuilder.addAllGroupsUnderDumpRoot(product, filePath);

                        //System.out.println("[Panoply] Attached ✓");
                    } catch (Throwable ex) {
                        System.out.println("[Panoply] attach failed: " +
                                ex.getClass().getSimpleName() + ": " + ex.getMessage());
                    }
                }
            }

            @Override
            public void productRemoved(ProductManager.Event e) {
                MetadataDumpTopComponent.findInstance().clearView();
            }
        };

        pm.addListener(listener);
        installMetadataWindowGuard();
        //System.out.println("[Panoply] ProductManager listener registered");
    }

    public static void shutdown() {
        if (pm != null && listener != null) {
            try {
                pm.removeListener(listener);
                //System.out.println("[Panoply] ProductManager listener removed");
            } catch (Throwable t) {
                System.out.println("[Panoply] Warning removing listener: " + t.getMessage());
            }
        }
        listener = null;
        pm = null;
    }

    // ---------- helpers ----------

    private static ProductManager resolveProductManager() {
        // 1) Older SNAPs: ProductManager.getInstance()
        try {
            Method m = ProductManager.class.getMethod("getInstance");
            Object obj = m.invoke(null);
            if (obj instanceof ProductManager) {
                System.out.println("[Panoply] PM via ProductManager.getInstance()");
                return (ProductManager) obj;
            }
        } catch (ReflectiveOperationException ignore) {
        }

        // 2) SNAP Desktop: SnapApp.getDefault().getProductManager()
        try {
            SnapApp app = SnapApp.getDefault();
            if (app != null) {
                ProductManager viaApp = app.getProductManager();
                if (viaApp != null) {
                    System.out.println("[Panoply] PM via SnapApp.getDefault()");
                    return viaApp;
                }
            }
        } catch (Throwable ignore) {
        }

        // 3) Engine fallback: GPF.getDefaultInstance().getProductManager()
        try {
            ProductManager viaGpf = GPF.getDefaultInstance().getProductManager();
            if (viaGpf != null) {
                System.out.println("[Panoply] PM via GPF.getDefaultInstance()");
                return viaGpf;
            }
        } catch (Throwable ignore) {
        }

        return null;
    }

    static void installMetadataWindowGuard() {
        // Listen only to OPENED changes (most reliable for our purpose)
        TopComponent.getRegistry().addPropertyChangeListener(evt -> {
            if (!TopComponent.Registry.PROP_OPENED.equals(evt.getPropertyName())) return;
            closeDefaultMetadataWindowsOnce();
        });
    }

    /** Close SNAP's default "Metadata" TopComponent if open. Never opens/focuses our TC here. */
    private static void closeDefaultMetadataWindowsOnce() {
        if (!CLOSING_SNAP_METADATA.compareAndSet(false, true)) return; // already running

        try {
            WindowManager wm = WindowManager.getDefault();
            for (TopComponent tc : wm.getRegistry().getOpened()) {
                // Skip our own panel
                if (tc instanceof gov.nasa.gsfc.seadas.panoply.ui.MetadataDumpTopComponent) continue;

                String id = null;
                try { id = wm.findTopComponentID(tc); } catch (Throwable ignore) {}

                String cls = tc.getClass().getName();
                String name = tc.getName();
                String display = tc.getDisplayName();

                boolean looksLikeSnapMetadata =
                        (id != null && (
                                id.equals("MetadataTopComponent") ||
                                        id.equals("org.esa.snap.ui.MetadataTopComponent") ||
                                        id.equals("org.esa.snap.ui.metadata.MetadataTopComponent")))
                                || (cls != null && cls.toLowerCase().contains("metadatatopcomponent"))
                                || (name != null && name.equalsIgnoreCase("Metadata"))
                                || (display != null && display.equalsIgnoreCase("Metadata"));

                if (looksLikeSnapMetadata) {
                    try { tc.close(); } catch (Throwable ignore) {}
                }
            }
        } finally {
            CLOSING_SNAP_METADATA.set(false);
        }
    }

}
