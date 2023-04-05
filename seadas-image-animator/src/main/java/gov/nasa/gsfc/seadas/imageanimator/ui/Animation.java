package gov.nasa.gsfc.seadas.imageanimator.ui;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import gov.nasa.gsfc.seadas.imageanimator.operator.ImageAnimatorOp;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.util.Debug;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.ui.product.ProductSceneImage;
import org.esa.snap.ui.product.ProductSceneView;
import org.openide.awt.UndoRedo;

import java.text.MessageFormat;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import javax.swing.*;

import static org.esa.snap.rcp.actions.window.OpenImageViewAction.getProductSceneView;
import static org.esa.snap.rcp.actions.window.OpenRGBImageViewAction.openDocumentWindow;
import static org.esa.snap.ui.UIUtils.*;



public class Animation {
    Thread thread;
    ImageIcon images;
    JFrame frame;
    JLabel label;
    int i = 0;
    int j;
    final int numImages = 3;

    Product product;

    private final ProgressMonitor pm;

    public static void main(String[] args) {
        Animation sa = new Animation();
    }

    public Animation() {
        pm = ProgressMonitor.NULL;
    }

    public Animation(String frameTitle) {
        pm = ProgressMonitor.NULL;
        frame = new JFrame(frameTitle);
        thread = new Thread();
        label = new JLabel();
        Panel panel = new Panel();
        panel.add(label);
        frame.add(panel, BorderLayout.CENTER);
        frame.setSize(500, 500);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        while (true) {
            bandImagesAnimator();
        }
    }

    public void startAnimate(){

        SnapApp snapApp = SnapApp.getDefault();
        final ProductSceneView sceneView = snapApp.getSelectedProductSceneView();
        ImageAnimatorOp imageAnimatorOp = new ImageAnimatorOp();
        RenderedImage renderedImage = imageAnimatorOp.createImage(sceneView);
        BufferedImage bufferedImage = ImageAnimatorOp.toBufferedImage(renderedImage);

        product = snapApp.getSelectedProduct(SnapApp.SelectionSourceHint.VIEW);



        Runnable r = new Runnable() {
            @Override
            public void run() {
                JPanel gui = new JPanel();

                final AnimatedImage[] tiles = new AnimatedImage[1];

                RenderedImage renderedImage;
                BufferedImage bufferedImage;
                RasterDataNode raster = product.getRasterDataNode("chlor_a");
                ProductSceneImage productSceneImage = new ProductSceneImage(raster, sceneView);
                ProductSceneView currentSceneView = new ProductSceneView(productSceneImage);
                currentSceneView.updateImage();


                renderedImage = imageAnimatorOp.createImage(currentSceneView);
                bufferedImage = ImageAnimatorOp.toBufferedImage(renderedImage);



                for (int ii = 0; ii < tiles.length; ii++) {
                    tiles[ii] = new AnimatedImage();
                    gui.add(new JLabel(new ImageIcon(bufferedImage)));
                }
                ActionListener listener = new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        for (int i = 0; i < tiles.length; i++) {
                            tiles[i].paintImage();
                            gui.repaint();
                        }
                    }
                };
                Timer timer = new Timer(50, listener);
                timer.start();

                JOptionPane.showMessageDialog(null, gui);
                timer.stop();
            }
        };
        SwingUtilities.invokeLater(r);
    }

    private ProductSceneImage createProductSceneImage(final RasterDataNode raster, ProductSceneView existingView, com.bc.ceres.core.ProgressMonitor pm) {
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

    public void bandImagesAnimator() {
        SnapApp snapApp = SnapApp.getDefault();
        final ProductSceneView sceneView = snapApp.getSelectedProductSceneView();
        ImageAnimatorOp imageAnimatorOp = new ImageAnimatorOp();
        RenderedImage renderedImage = imageAnimatorOp.createImage(sceneView);
        BufferedImage bufferedImage = ImageAnimatorOp.toBufferedImage(renderedImage);
        snapApp.getProductManager().getProduct(0);
        product = snapApp.getSelectedProduct(SnapApp.SelectionSourceHint.VIEW);
        ProductNodeGroup<Band> products = product.getBandGroup();
        try {

            for (ProductNode band : products.toArray()) {
                //images = new ImageIcon(band.getProduct().getRasterDataNode(band.getName()).getGeophysicalImage().getAsBufferedImage());
                //images = new ImageIcon((Image) sceneView.getBaseImageLayer().getImage());
                images = new ImageIcon(bufferedImage);
                label.setIcon(images);
                thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    public void test(){
        Band[] bands = product.getBands();
        try {
            for (Band band : bands) {
                images = new ImageIcon(band.getProduct().getRasterDataNode(band.getName()).getGeophysicalImage().getAsBufferedImage());
                //images = new ImageIcon((Image) sceneView.getBaseImageLayer().getImage());
                label.setIcon(images);
                thread.sleep(1000);
            }
        } catch (InterruptedException e){
            throw new RuntimeException(e);
        }
    }

    public void swingAnimator() {
        try {
            for (i = 0; i < numImages; i++) {
                images = new ImageIcon("images/img" + (i + 1) + ".png");
                label.setIcon(images);
                thread.sleep(1000);
            }
        } catch (InterruptedException e) {
        }
    }



    class AnimatedImage extends BufferedImage {
        GradientPaint[] frameGradient;
        int frame = 0;

        AnimatedImage() {
            super(600, 600, BufferedImage.TYPE_INT_RGB);
            frameGradient = new GradientPaint[6];
            for (int i = 0; i < frameGradient.length; i++) {
                frameGradient[i] = new GradientPaint(0f, (float) i, Color.BLUE, 0f,
                        (float) i + 13, Color.RED, true);
            }
        }

        public void paintImage() {
            Graphics2D g = createGraphics();
            if (frame == frameGradient.length - 1)
                frame = 0;
            else
                frame++;
            g.setPaint(frameGradient[frame]);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.dispose();
        }
    }


    private void openProductSceneView(RasterDataNode rasterDataNode) {
        SnapApp snapApp = SnapApp.getDefault();
        snapApp.setStatusBarMessage("Opening image view...");

        setRootFrameWaitCursor(snapApp.getMainFrame());

        String progressMonitorTitle = MessageFormat.format("Creating image for ''{0}''", rasterDataNode.getName());

        ProductSceneView existingView = getProductSceneView(rasterDataNode);
        SwingWorker<ProductSceneImage, Object> worker = new ProgressMonitorSwingWorker<ProductSceneImage, Object>(snapApp.getMainFrame(), progressMonitorTitle) {

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
                try {
                    return createProductSceneImage(rasterDataNode, existingView, pm);
                } finally {
                    if (pm.isCanceled()) {
                        rasterDataNode.unloadRasterData();
                    }
                }
            }
        };
        worker.execute();
    }

}
