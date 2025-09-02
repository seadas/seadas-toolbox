package gov.nasa.gsfc.seadas.panoply;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductManager;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.rcp.SnapApp;

import java.io.File;
import java.lang.reflect.Method;

public final class StartupHook {

    private static ProductManager.Listener listener;
    private static ProductManager pm;

    private StartupHook() {}

    public static void init() {
        if (listener != null) {
            System.out.println("[Panoply] StartupHook already initialized");
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
                Product p = e.getProduct();
                System.out.println("[Panoply] productAdded: " + safeName(p));

                File f = p.getFileLocation();
                String src = (f != null ? f.getAbsolutePath() : null);
                System.out.println("[Panoply] Source for " + safeName(p) + ": " + src);

                if (src != null) {
                    try {
                        System.out.println("[Panoply] Attaching to " + p.getName() + " from " + src);
                        PanoplyStyleMetadataBuilder.attachPanoplyMetadata(p, src);
                        System.out.println("[Panoply] Attached ✓");
                    } catch (Throwable ex) {
                        System.out.println("[Panoply] attach failed: " +
                                ex.getClass().getSimpleName() + ": " + ex.getMessage());
                    }
                }
            }

            @Override
            public void productRemoved(ProductManager.Event e) {
                // no-op (you could remove Panoply subtree if desired)
            }
        };

        pm.addListener(listener);
        System.out.println("[Panoply] ProductManager listener registered");
    }

    public static void shutdown() {
        if (pm != null && listener != null) {
            try {
                pm.removeListener(listener);
                System.out.println("[Panoply] ProductManager listener removed");
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

    private static String safeName(Product p) {
        try { return p != null ? p.getName() : "<null>"; }
        catch (Throwable t) { return "<unknown>"; }
    }
}
