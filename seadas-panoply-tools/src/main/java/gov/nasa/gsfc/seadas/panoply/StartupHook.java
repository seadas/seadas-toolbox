package gov.nasa.gsfc.seadas.panoply;

import org.openide.modules.OnStart;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductManager;
import org.esa.snap.rcp.SnapApp;
import javax.swing.SwingUtilities;

@OnStart
public class StartupHook implements Runnable {
    @Override
    public void run() {
        ProductManager pm = SnapApp.getDefault().getProductManager();
        pm.addListener(new ProductManager.Listener() {
            @Override
            public void productAdded(ProductManager.Event event) {
                Product p = event.getProduct();
                SwingUtilities.invokeLater(() -> attachPanoply(p));
            }
            @Override
            public void productRemoved(ProductManager.Event event) {
                // no-op
            }
        });
    }

    private void attachPanoply(Product product) {
        try {
            if (product == null) return;
            if (product.getMetadataRoot().getElement("Panoply") != null) return;
            if (product.getFileLocation() == null) return;
            String pathOrUrl = product.getFileLocation().getAbsolutePath();
            PanoplyStyleMetadataBuilder.attachPanoplyMetadata(product, pathOrUrl);
        } catch (Exception ignore) { }
    }
}
