package gov.nasa.gsfc.seadas.imageanimator.ui;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.grender.Viewport;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import gov.nasa.gsfc.seadas.imageanimator.operator.ImageAnimatorOp;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.util.Debug;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.actions.window.OpenImageViewAction;
import org.esa.snap.rcp.windows.ProductSceneViewTopComponent;
import org.esa.snap.ui.product.ProductSceneImage;
import org.esa.snap.ui.product.ProductSceneView;
import org.openide.awt.UndoRedo;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.text.MessageFormat;
import java.util.ArrayList;

import static org.esa.snap.rcp.actions.window.OpenImageViewAction.getProductSceneView;
import static org.esa.snap.rcp.actions.window.OpenRGBImageViewAction.openDocumentWindow;
import static org.esa.snap.ui.UIUtils.setRootFrameDefaultCursor;
import static org.esa.snap.ui.UIUtils.setRootFrameWaitCursor;


public class Animation {

    Product product;

    private final ProgressMonitor pm;

    public static void main(String[] args) {
        Animation sa = new Animation();
    }

    public Animation() {
        pm = ProgressMonitor.NULL;
    }

    public boolean checkImages(TreePath[] treePaths) {

        SnapApp snapApp = SnapApp.getDefault();

        product = snapApp.getSelectedProduct(SnapApp.SelectionSourceHint.VIEW);

        ArrayList<String> parents = new ArrayList<String>();
        final ArrayList<String> selectedBandsList = new ArrayList<>();

        String currentSelectedBand;
        for (TreePath treePath : treePaths) {
            if (treePath.getParentPath() != null) {
                parents.add(String.valueOf(treePath.getParentPath().getLastPathComponent()));
            }
        }

        for (TreePath treePath : treePaths) {
            currentSelectedBand = String.valueOf(treePath.getLastPathComponent());
            if (!parents.contains(currentSelectedBand)) {
                selectedBandsList.add(currentSelectedBand);
            }
        }

        final String[] selectedBandNames = selectedBandsList.toArray(new String[0]);

        for (int i = 0; i < selectedBandNames.length; i++) {
            RasterDataNode raster = product.getRasterDataNode(selectedBandNames[i]);
            ProductSceneViewTopComponent tc = OpenImageViewAction.getProductSceneViewTopComponent(raster);
            if (tc == null) {
                return false;
            }
        }
        return true;
    }

    public void createImages(TreePath[] treePaths) {

        SnapApp snapApp = SnapApp.getDefault();
        final ProductSceneView sceneView = snapApp.getSelectedProductSceneView();

        product = snapApp.getSelectedProduct(SnapApp.SelectionSourceHint.VIEW);

        ArrayList<String> parents = new ArrayList<String>();
        final ArrayList<String> selectedBandsList = new ArrayList<>();

        String currentSelectedBand;
        for (TreePath treePath : treePaths) {
            if (treePath.getParentPath() != null) {
                parents.add(String.valueOf(treePath.getParentPath().getLastPathComponent()));
            }
        }

        for (TreePath treePath : treePaths) {
            currentSelectedBand = String.valueOf(treePath.getLastPathComponent());
            if (!parents.contains(currentSelectedBand)) {
                selectedBandsList.add(currentSelectedBand);
            }
        }

        final String[] selectedBandNames = selectedBandsList.toArray(new String[0]);
        final RasterDataNode[] rasters = new RasterDataNode[selectedBandNames.length];

        for (int i = 0; i < selectedBandNames.length; i++) {
            RasterDataNode raster = product.getRasterDataNode(selectedBandNames[i]);
            OpenImageViewAction.openImageView(raster);
            rasters[i] = raster;
        }
    }

    public ImageIcon[] openImages(TreePath[] treePaths) {

        SnapApp snapApp = SnapApp.getDefault();
        final ProductSceneView sceneView = snapApp.getSelectedProductSceneView();
        Viewport standardViewPort = sceneView.getLayerCanvas().getViewport();
        ImageAnimatorOp imageAnimatorOp = new ImageAnimatorOp();
        product = snapApp.getSelectedProduct(SnapApp.SelectionSourceHint.VIEW);

        ArrayList<String> parents = new ArrayList<String>();
        final ArrayList<String> selectedBandsList = new ArrayList<>();

        String currentSelectedBand;
        for (TreePath treePath : treePaths) {
            if (treePath.getParentPath() != null) {
                parents.add(String.valueOf(treePath.getParentPath().getLastPathComponent()));
            }
        }

        for (TreePath treePath : treePaths) {
            currentSelectedBand = String.valueOf(treePath.getLastPathComponent());
            if (!parents.contains(currentSelectedBand)) {
                selectedBandsList.add(currentSelectedBand);
            }
        }

        final String[] selectedBandNames = selectedBandsList.toArray(new String[0]);
        final RenderedImage[] renderedImages = new RenderedImage[selectedBandNames.length];
        final RasterDataNode[] rasters = new RasterDataNode[selectedBandNames.length];
        ProductSceneView myView = null;
        RenderedImage renderedImage;

        for (int i = 0; i < selectedBandNames.length; i++) {
            RasterDataNode raster = product.getRasterDataNode(selectedBandNames[i]);
            OpenImageViewAction.openImageView(raster);
            rasters[i] = raster;
        }
        ImageIcon[] images = new ImageIcon[renderedImages.length];
        for (int i = 0; i < selectedBandNames.length; i++) {
            myView = getProductSceneView(rasters[i]);
            renderedImage = imageAnimatorOp.createImage(myView, standardViewPort);
            renderedImages[i] = renderedImage;
            images[i] = new ImageIcon((BufferedImage) renderedImage);
        }
        return images;
    }

    private static ProductSceneImage createProductSceneImage(final RasterDataNode raster, ProductSceneView existingView, com.bc.ceres.core.ProgressMonitor pm) {
        Debug.assertNotNull(raster);
        Debug.assertNotNull(pm);

        try {
            pm.beginTask("Creating image...", 1);

            ProductSceneImage sceneImage;
            if (existingView != null) {
                sceneImage = new ProductSceneImage(raster, existingView);
            } else {
                sceneImage = new ProductSceneImage(raster,
                        SnapApp.getDefault().getPreferencesPropertyMap(),
                        SubProgressMonitor.create(pm, 1));
            }
            sceneImage.initVectorDataCollectionLayer();
            sceneImage.initMaskCollectionLayer();
            return sceneImage;
        } finally {
            pm.done();
        }
    }

    public static void openProductSceneView(RasterDataNode rasterDataNode) {
        SnapApp snapApp = SnapApp.getDefault();
        snapApp.setStatusBarMessage("Opening image view...");

        setRootFrameWaitCursor(snapApp.getMainFrame());

        String progressMonitorTitle = MessageFormat.format("Creating image for ''{0}''", rasterDataNode.getName());

        ProductSceneView existingView = getProductSceneView(rasterDataNode);

        ProgressMonitorSwingWorker<ProductSceneImage, Object> worker = new ProgressMonitorSwingWorker<ProductSceneImage, Object>(snapApp.getMainFrame(), progressMonitorTitle) {

            @Override
            public void done() {
                setRootFrameDefaultCursor(snapApp.getMainFrame());
                snapApp.setStatusBarMessage("");
                try {
                    ProductSceneImage sceneImage = get();
                    UndoRedo.Manager undoManager = SnapApp.getDefault().getUndoManager(sceneImage.getProduct());
                    ProductSceneView view = new ProductSceneView(sceneImage, undoManager);
                    openDocumentWindow(view);
                } catch (Exception e) {
                    snapApp.handleError(MessageFormat.format("Failed to open image view.\n\n{0}", e.getMessage()), e);
                }
            }

            @Override
            protected ProductSceneImage doInBackground(com.bc.ceres.core.ProgressMonitor pm) {
                pm.beginTask("Creating Image ", 10);
                pm.worked(1);
                try {
                    return createProductSceneImage(rasterDataNode, existingView, pm);
                } finally {
                    if (pm.isCanceled()) {
                        rasterDataNode.unloadRasterData();
                    }
                }
            }
        };
        worker.executeWithBlocking();
    }

}
