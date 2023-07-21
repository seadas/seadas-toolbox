package gov.nasa.gsfc.seadas.imageanimator.ui;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.grender.Viewport;
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
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.tree.TreePath;

import static org.esa.snap.rcp.actions.window.OpenImageViewAction.getProductSceneView;
import static org.esa.snap.rcp.actions.window.OpenRGBImageViewAction.openDocumentWindow;
import static org.esa.snap.ui.UIUtils.*;


public class Animation {

    Product product;

    private final ProgressMonitor pm;

    public static void main(String[] args) {
        Animation sa = new Animation();
    }

    public Animation() {
        pm = ProgressMonitor.NULL;
    }

    public ImageIcon[] createAndOpenImages(TreePath[] treePaths) {

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

        final String[] selecteBandNames = selectedBandsList.toArray(new String[0]);
        final RenderedImage[] renderedImages = new RenderedImage[selecteBandNames.length];
        final RasterDataNode[] rasters = new RasterDataNode[selecteBandNames.length];
        ProductSceneView myView;
        RenderedImage renderedImage;

        for (int i = 0; i < selecteBandNames.length; i++) {
            RasterDataNode raster = product.getRasterDataNode(selecteBandNames[i]);
            if (product.getBand(selecteBandNames[i]).getImageInfo() == null) {
                openProductSceneView(raster);
            }
            rasters[i] = raster;
        }

        ImageIcon[] images = new ImageIcon[renderedImages.length];
        for (int i = 0; i < selecteBandNames.length; i++) {
            myView = getProductSceneView(rasters[i]);
            if (myView == null) {
                System.out.println("myView is null for i = " + i);
            }
            renderedImage = imageAnimatorOp.createImage(myView, standardViewPort);
            renderedImages[i] = renderedImage;
            images[i] = new ImageIcon((BufferedImage) renderedImage);

        }

        return images;
    }

    public void startAnimate(TreePath[] treePaths) {

        SnapApp snapApp = SnapApp.getDefault();
        final ProductSceneView sceneView = snapApp.getSelectedProductSceneView();
        Viewport standardViewPort = sceneView.getLayerCanvas().getViewport();
        ImageAnimatorOp imageAnimatorOp = new ImageAnimatorOp();
        product = snapApp.getSelectedProduct(SnapApp.SelectionSourceHint.VIEW);
        TreePath rootPath = treePaths[0].getParentPath();

        ArrayList<String> parents = new ArrayList<String>();
        final ArrayList<String> selectedBandsList = new ArrayList<>();

        int pathCount = treePaths[0].getPathCount();
        String currentSelectedBand;
        for (TreePath treePath : treePaths) {
            currentSelectedBand = String.valueOf(treePath.getLastPathComponent());
            System.out.println("current node " + currentSelectedBand);
            if (treePath.getParentPath() != null) {
                System.out.println("parent node " + String.valueOf(treePath.getParentPath().getLastPathComponent()));
                parents.add(String.valueOf(treePath.getParentPath().getLastPathComponent()));
            }
        }

        for (TreePath treePath : treePaths) {
            currentSelectedBand = String.valueOf(treePath.getLastPathComponent());
            if (!parents.contains(currentSelectedBand)) {
                System.out.println("this is the band that is selected for animation: " + currentSelectedBand);
                selectedBandsList.add(currentSelectedBand);
            }
        }

        Runnable r = new Runnable() {
            int index = 0;

            @Override
            public void run() {
                JPanel gui = new JPanel();
                final String[] selecteBandNames = selectedBandsList.toArray(new String[0]);
                final RenderedImage[] renderedImages = new RenderedImage[selecteBandNames.length];
                final RasterDataNode[] rasters = new RasterDataNode[selecteBandNames.length];
                ProductSceneView myView;
                RenderedImage renderedImage;

                for (int i = 0; i < selecteBandNames.length; i++) {
                    RasterDataNode raster = product.getRasterDataNode(selecteBandNames[i]);
                    if (product.getBand(selecteBandNames[i]).getImageInfo() == null) {
                        openProductSceneView(raster);
                    }
                    rasters[i] = raster;
                }

                for (int i = 0; i < selecteBandNames.length; i++) {
                    myView = getProductSceneView(rasters[i]);
                    if (myView == null) {
                        System.out.println("myView is null for i = " + i);
                    }
                    renderedImage = imageAnimatorOp.createImage(myView, standardViewPort);
                    renderedImages[i] = renderedImage;

                    if (i == 0) {
                        gui.add(new JLabel(new ImageIcon((BufferedImage) renderedImage)));
                    }
                }

                Timer timer = new Timer(1000, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        JLabel label = (JLabel) gui.getComponent(0);
                        label.setIcon(new ImageIcon((BufferedImage) renderedImages[++index % selecteBandNames.length]));
                        label.repaint();
                        gui.repaint();
                    }
                });
                timer.start();

                JOptionPane.showMessageDialog(null, gui);

                timer.stop();
            }
        };

        SwingUtilities.invokeLater(r);
    }

    public void startAnimateAngular() {

//        SnapApp snapApp = SnapApp.getDefault();
//        final ProductSceneView sceneView = snapApp.getSelectedProductSceneView();
//        Viewport standardViewPort = sceneView.getLayerCanvas().getViewport();
//        ImageAnimatorOp imageAnimatorOp = new ImageAnimatorOp();
////        RenderedImage renderedImage = imageAnimatorOp.createImage(sceneView);
////        BufferedImage bufferedImage = ImageAnimatorOp.toBufferedImage(renderedImage);
////        BufferedImage bi = (BufferedImage)imageAnimatorOp.createImage(sceneView);
//        product = snapApp.getSelectedProduct(SnapApp.SelectionSourceHint.VIEW);


        Runnable r = new Runnable() {
            int index = 0;

            @Override
            public void run() {
//                JPanel gui = new JPanel();
//                final String[] names = {"angstrom", "chlor_a", "chl_ocx", "pic", "poc", "ipar", "nflh", "par"};
//                final RenderedImage[] renderedImages = new RenderedImage[names.length];
//                final RasterDataNode[] rasters = new RasterDataNode[names.length];
//                ProductSceneView myView;
//                RenderedImage renderedImage;

                AngularAnimationTopComponent angularAnimationTopComponent = new AngularAnimationTopComponent();


//                for (int i = 0; i < names.length; i++) {
//                    RasterDataNode raster = product.getRasterDataNode(names[i]);
//                    openProductSceneView(raster);
//                    rasters[i] = raster;
//                }

//                for (int i = 0; i < names.length; i++) {
//                    myView = getProductSceneView(rasters[i]);
//                    renderedImage = imageAnimatorOp.createImage(myView, standardViewPort);
//                    renderedImages[i] = renderedImage;
//
//                    if(i == 0) {
//                        gui.add(new JLabel(new ImageIcon((BufferedImage)renderedImage)));
//                    }
//                }

//                Timer timer = new Timer(1000, new ActionListener() {
//                    @Override
//                    public void actionPerformed(ActionEvent e) {
//                        JLabel label = (JLabel) gui.getComponent(0);
//                        label.setIcon(new ImageIcon((BufferedImage) renderedImages[++index % names.length]));
//                        label.repaint();
//                        gui.repaint();
//                    }
//                });
//                timer.start();
//
//                JOptionPane.showMessageDialog(null, gui);
//
//                timer.stop();
            }
        };

        SwingUtilities.invokeLater(r);
    }

    public void startAnimateSpectrum() {

//        SnapApp snapApp = SnapApp.getDefault();
//        final ProductSceneView sceneView = snapApp.getSelectedProductSceneView();
//        Viewport standardViewPort = sceneView.getLayerCanvas().getViewport();
//        ImageAnimatorOp imageAnimatorOp = new ImageAnimatorOp();
//        RenderedImage renderedImage = imageAnimatorOp.createImage(sceneView);
//        BufferedImage bufferedImage = ImageAnimatorOp.toBufferedImage(renderedImage);
//        BufferedImage bi = (BufferedImage)imageAnimatorOp.createImage(sceneView);
//        product = snapApp.getSelectedProduct(SnapApp.SelectionSourceHint.VIEW);


        Runnable r = new Runnable() {
            int index = 0;

            @Override
            public void run() {
//                JPanel gui = new JPanel();
//                final String[] names = {"angstrom", "chlor_a", "chl_ocx", "pic", "poc", "ipar", "nflh", "par"};
//                final RenderedImage[] renderedImages = new RenderedImage[names.length];
//                final RasterDataNode[] rasters = new RasterDataNode[names.length];
//                ProductSceneView myView;
//                RenderedImage renderedImage;

                SpectrumAnimationTopComponent spectrumAnimationTopComponent = new SpectrumAnimationTopComponent();

//                for (int i = 0; i < names.length; i++) {
//                    RasterDataNode raster = product.getRasterDataNode(names[i]);
//                    openProductSceneView(raster);
//                    rasters[i] = raster;
//                }

//                for (int i = 0; i < names.length; i++) {
//                    myView = getProductSceneView(rasters[i]);
//                    renderedImage = imageAnimatorOp.createImage(myView, standardViewPort);
//                    renderedImages[i] = renderedImage;
//
//                    if(i == 0) {
//                        gui.add(new JLabel(new ImageIcon((BufferedImage)renderedImage)));
//                    }
//                }

//                Timer timer = new Timer(1000, new ActionListener() {
//                    @Override
//                    public void actionPerformed(ActionEvent e) {
//                        JLabel label = (JLabel) gui.getComponent(0);
//                        label.setIcon(new ImageIcon((BufferedImage) renderedImages[++index % names.length]));
//                        label.repaint();
//                        gui.repaint();
//                    }
//                });
//                timer.start();
//
//                JOptionPane.showMessageDialog(null, gui);
//
//                timer.stop();
            }
        };

        SwingUtilities.invokeLater(r);
    }

//    public void animatioTest() {
//        AnimationTest anim = new AnimationTest();
//        JFrame app = new JFrame("Animator test");
//        app.add(anim, BorderLayout.CENTER);
//        app.setSize(300, 300);
//        app.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        app.setSize(anim.getPreferredSize().width + 10, anim.getPreferredSize().height + 30);
//    }

    private static JFrame jFrame;
    private static JLabel jLabel;

    public static void display(BufferedImage image) {
        if (jFrame == null) {
            jFrame = new JFrame();
            jFrame.setTitle("stained_image");
            jFrame.setSize(image.getWidth(), image.getHeight());
            jFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            jLabel = new JLabel();
            jLabel.setIcon(new ImageIcon(image));
            jFrame.getContentPane().add(jLabel, BorderLayout.CENTER);
            jFrame.setLocationRelativeTo(null);
            jFrame.pack();
            jFrame.setVisible(true);
        } else jLabel.setIcon(new ImageIcon(image));
    }

    public static BufferedImage redraw(BufferedImage img, Color bg) {
        BufferedImage rgbImage = new BufferedImage(img.getWidth(),
                img.getHeight(),
                BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g2d = rgbImage.createGraphics();
        g2d.drawImage(img, 0, 0, bg, null);
        g2d.dispose();
        return rgbImage;
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

    private static ProductSceneImage createProductSceneImage(final RasterDataNode raster, ProductSceneView existingView) {
        Debug.assertNotNull(raster);
        final ProductSceneImage[] sceneImage = {null};
        SnapApp snapApp = SnapApp.getDefault();
        System.out.println(raster.getName());

        ProgressMonitorSwingWorker pmSwingWorker = new ProgressMonitorSwingWorker(snapApp.getMainFrame(),
                "Create Product Scene Image") {

            @Override
            protected ProductSceneImage doInBackground(ProgressMonitor pm) throws Exception {


                pm.beginTask("Creating a scene image for " + raster.getName(), 10);

                pm.worked(1);
                try {

                    if (existingView != null) {
                        sceneImage[0] = new ProductSceneImage(raster, existingView);
                    } else {
                        sceneImage[0] = new ProductSceneImage(raster,
                                SnapApp.getDefault().getPreferencesPropertyMap(),
                                SubProgressMonitor.create(pm, 1));
                    }
                    sceneImage[0].initVectorDataCollectionLayer();
                    sceneImage[0].initMaskCollectionLayer();
                } finally {
                    done();
                }
                return sceneImage[0];
            }

//            @Override
//            protected void done() {
//                try {
//                    sceneImage[0] = (ProductSceneImage) get();
//                } catch (InterruptedException interruptedException) {
//                    interruptedException.printStackTrace();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//
//            }
        };
        pmSwingWorker.executeWithBlocking();
        return sceneImage[0];
    }


//    public void bandImagesAnimator() {
//        SnapApp snapApp = SnapApp.getDefault();
//        final ProductSceneView sceneView = snapApp.getSelectedProductSceneView();
//        ImageAnimatorOp imageAnimatorOp = new ImageAnimatorOp();
//        RenderedImage renderedImage = imageAnimatorOp.createImage(sceneView);
//        BufferedImage bufferedImage = ImageAnimatorOp.toBufferedImage(renderedImage);
//        snapApp.getProductManager().getProduct(0);
//        product = snapApp.getSelectedProduct(SnapApp.SelectionSourceHint.VIEW);
//        ProductNodeGroup<Band> products = product.getBandGroup();
//        try {
//
//            for (ProductNode band : products.toArray()) {
//                //images = new ImageIcon(band.getProduct().getRasterDataNode(band.getName()).getGeophysicalImage().getAsBufferedImage());
//                //images = new ImageIcon((Image) sceneView.getBaseImageLayer().getImage());
//                images = new ImageIcon(bufferedImage);
//                jLabel.setIcon(images);
//                thread.sleep(1000);
//            }
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
//
//    }

//    public void test(){
//        Band[] bands = product.getBands();
//        try {
//            for (Band band : bands) {
//                images = new ImageIcon(band.getProduct().getRasterDataNode(band.getName()).getGeophysicalImage().getAsBufferedImage());
//                System.out.println(band.getName());
//                jLabel = new JLabel();
//                jLabel.setIcon(images);
//                thread.sleep(1000);
//            }
//        } catch (InterruptedException e){
//            throw new RuntimeException(e);
//        }
//    }

//    public void swingAnimator() {
//        try {
//            for (i = 0; i < numImages; i++) {
//                images = new ImageIcon("images/img" + (i + 1) + ".png");
//                jLabel.setIcon(images);
//                thread.sleep(1000);
//            }
//        } catch (InterruptedException e) {
//        }
//    }


//    class AnimatedImage extends BufferedImage {
//        GradientPaint[] frameGradient;
//        int frame = 0;
//
//        AnimatedImage() {
//            super(1000, 1200, BufferedImage.TYPE_INT_RGB);
//            frameGradient = new GradientPaint[6];
//            for (int i = 0; i < frameGradient.length; i++) {
//                frameGradient[i] = new GradientPaint(0f, (float) i, Color.BLUE, 0f,
//                        (float) i + 13, Color.RED, true);
//            }
//        }
//
//        public void paintImage() {
//            Graphics2D g = createGraphics();
//            if (frame == frameGradient.length - 1)
//                frame = 0;
//            else
//                frame++;
//            g.setPaint(frameGradient[frame]);
//            g.fillRect(0, 0, getWidth(), getHeight());
//            g.dispose();
//        }
//    }


    public static void openProductSceneView(RasterDataNode rasterDataNode) {
        SnapApp snapApp = SnapApp.getDefault();
        snapApp.setStatusBarMessage("Opening image view...");

        setRootFrameWaitCursor(snapApp.getMainFrame());

        String progressMonitorTitle = MessageFormat.format("Creating image for ''{0}''", rasterDataNode.getName());

        ProductSceneView existingView = getProductSceneView(rasterDataNode);

        ProgressMonitorSwingWorker<ProductSceneImage, Object> worker = new ProgressMonitorSwingWorker<ProductSceneImage, Object>(snapApp.getMainFrame(), progressMonitorTitle) {

            @Override
            public void done() {
//                setRootFrameDefaultCursor(snapApp.getMainFrame());
//                snapApp.setStatusBarMessage("");
//                try {
//                    ProductSceneImage sceneImage = get();
//                    UndoRedo.Manager undoManager = SnapApp.getDefault().getUndoManager(sceneImage.getProduct());
//                    ProductSceneView view = new ProductSceneView(sceneImage, undoManager);
//                    openDocumentWindow(view);
//
//                } catch (Exception e) {
//                    snapApp.handleError(MessageFormat.format("Failed to open image view.\n\n{0}", e.getMessage()), e);
//                }
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
        setRootFrameDefaultCursor(snapApp.getMainFrame());
        snapApp.setStatusBarMessage("");
        try {
            ProductSceneImage sceneImage = worker.get();
            UndoRedo.Manager undoManager = SnapApp.getDefault().getUndoManager(sceneImage.getProduct());
            ProductSceneView view = new ProductSceneView(sceneImage, undoManager);
            openDocumentWindow(view);

        } catch (Exception e) {
            snapApp.handleError(MessageFormat.format("Failed to open image view.\n\n{0}", e.getMessage()), e);
        }
    }

}
