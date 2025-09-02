package gov.nasa.gsfc.seadas.panoply;

import org.openide.modules.ModuleInstall;
import org.esa.snap.core.datamodel.ProductManager;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.rcp.SnapApp;

import javax.swing.SwingUtilities;
import java.io.File;

public class Installer extends ModuleInstall {

    @Override
    public void restored() {
        System.out.println("[Panoply] ModuleInstall restored()");

        ProductManager pm = SnapApp.getDefault().getProductManager();
        System.out.println("[Panoply] Current products at startup: " + pm.getProductCount());

        // Attach to products already open
        for (Product p : pm.getProducts()) {
            System.out.println("[Panoply] Found already-open product: " + safeName(p));
            attachIfNeeded(p);
        }
        // Listen for new products
        pm.addListener(new ProductManager.Listener() {
            @Override
            public void productAdded(ProductManager.Event event) {
                Product p = event.getProduct();
                System.out.println("[Panoply] productAdded: " + safeName(p));
                attachIfNeeded(p);
            }
            @Override public void productRemoved(ProductManager.Event event) {
                System.out.println("[Panoply] productRemoved: " + safeName(event != null ? event.getProduct() : null));
            }
        });


    }

    private static String safeName(Product p) {
        return p == null ? "<null>" : p.getName();
    }

    private void attachIfNeeded(Product p) {
        if (p == null) return;

        // Avoid duplicates
        if (p.getMetadataRoot().getElement("Panoply") != null) {
            System.out.println("[Panoply] Already attached: " + p.getName());
            return;
        }

        // Try to find a usable source path/URL (local file, VFS URL, etc.)
        final String source = findSource(p);
        System.out.println("[Panoply] Source for " + p.getName() + ": " + (source == null ? "<none>" : source));

        SwingUtilities.invokeLater(() -> {
            try {
                if (source != null && new File(source).exists()) {
                    System.out.println("[Panoply] Attaching to " + p.getName() + " from " + source);
                    PanoplyStyleMetadataBuilder.attachPanoplyMetadata(p, source);
                    System.out.println("[Panoply] Attached \u2713");
                } else if (source != null) {
                    // Try anyway (NetCDF can often handle URLs/VFS)
                    System.out.println("[Panoply] Attaching via non-file source: " + source);
                    PanoplyStyleMetadataBuilder.attachPanoplyMetadata(p, source);
                    System.out.println("[Panoply] Attached \u2713 (non-file)");
                } else {
                    // No source — attach a minimal placeholder so you SEE the node
                    System.out.println("[Panoply] No source; attaching placeholder node");
                    var e = new org.esa.snap.core.datamodel.MetadataElement("Panoply");
                    e.addAttribute(new org.esa.snap.core.datamodel.MetadataAttribute(
                            "note",
                            org.esa.snap.core.datamodel.ProductData.createInstance(
                                    "No file/URL detected; NetCDF parse skipped"),
                            true));
                    p.getMetadataRoot().addElement(e);
                }
            } catch (Exception ex) {
                System.out.println("[Panoply] attach failed: " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
                // Still add a node so it's visible
                var e = new org.esa.snap.core.datamodel.MetadataElement("Panoply");
                e.addAttribute(new org.esa.snap.core.datamodel.MetadataAttribute(
                        "error",
                        org.esa.snap.core.datamodel.ProductData.createInstance(ex.toString()),
                        true));
                p.getMetadataRoot().addElement(e);
            }
        });
    }

    /** Try several ways to locate the product's source */
    private String findSource(Product p) {
        // 1) Standard file location
        try {
            File f = p.getFileLocation();
            if (f != null) {
                return f.getAbsolutePath();
            }
        } catch (Throwable ignored) {}

        // 2) Reader input
        try {
            ProductReader r = p.getProductReader();
            if (r != null && r.getInput() != null) {
                Object in = r.getInput();
                if (in instanceof File) {
                    return ((File) in).getAbsolutePath();
                }
                return String.valueOf(in); // could be a path, URL, or stream description
            }
        } catch (Throwable ignored) {}

        // No usable source
        return null;
    }
}
