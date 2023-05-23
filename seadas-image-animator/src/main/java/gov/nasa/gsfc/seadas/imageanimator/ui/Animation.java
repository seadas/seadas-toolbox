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
        jFrame = new JFrame(frameTitle);
        thread = new Thread();
        jLabel = new JLabel();
        Panel panel = new Panel();
        panel.add(jLabel);
        jFrame.add(panel, BorderLayout.CENTER);
        jFrame.setSize(1500, 1500);
        jFrame.setVisible(true);
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        while (true) {
            bandImagesAnimator();
        }
    }

    public void startAnimate(){

        SnapApp snapApp = SnapApp.getDefault();
        final ProductSceneView sceneView = snapApp.getSelectedProductSceneView();
        Viewport standardViewPort = sceneView.getLayerCanvas().getViewport();
        ImageAnimatorOp imageAnimatorOp = new ImageAnimatorOp();
//        RenderedImage renderedImage = imageAnimatorOp.createImage(sceneView);
//        BufferedImage bufferedImage = ImageAnimatorOp.toBufferedImage(renderedImage);
//        BufferedImage bi = (BufferedImage)imageAnimatorOp.createImage(sceneView);
        product = snapApp.getSelectedProduct(SnapApp.SelectionSourceHint.VIEW);


        Runnable r = new Runnable() {
            int index = 0;
            @Override
            public void run() {
                JPanel gui = new JPanel();
                final String[] names = {"angstrom", "chlor_a", "chl_ocx", "pic", "poc", "ipar", "nflh", "par"};;
                if (snapApp.getSelectedProductSceneView().getProduct().getName().contains("HARP2")) {
                    names[0] = "I_-43_549";
                    names[1] = "I_-32_549";
                    names[2] = "I_-20_549";
                    names[3] = "I_-6_549";
                    names[4] = "I_0_549";
                    names[5] = "I_15_549";
                    names[6] = "I_28_549";
                    names[7] = "I_-52_549";
                }
                final RenderedImage[] renderedImages = new RenderedImage[names.length];
                final RasterDataNode[] rasters = new RasterDataNode[names.length];
                ProductSceneView myView;
                RenderedImage renderedImage;

                for (int i = 0; i < names.length; i++) {
                    RasterDataNode raster = product.getRasterDataNode(names[i]);
                    openProductSceneView(raster);
                    rasters[i] = raster;
                }

                for (int i = 0; i < names.length; i++) {
                    myView = getProductSceneView(rasters[i]);
                    renderedImage = imageAnimatorOp.createImage(myView, standardViewPort);
                    renderedImages[i] = renderedImage;

                    if(i == 0) {
                        gui.add(new JLabel(new ImageIcon((BufferedImage)renderedImage)));
                    }
                }

                Timer timer = new Timer(1000, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        JLabel label = (JLabel) gui.getComponent(0);
                        label.setIcon(new ImageIcon((BufferedImage) renderedImages[++index % names.length]));
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

    public void startAnimateAngular(){

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

    public void startAnimateSpectrum(){

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

    public void animatioTest() {
        AnimationTest anim = new AnimationTest();
        JFrame app = new JFrame("Animator test");
        app.add(anim, BorderLayout.CENTER);
        app.setSize(300, 300);
        app.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        app.setSize(anim.getPreferredSize().width + 10, anim.getPreferredSize().height + 30);
    }

    private static JFrame jFrame;
    private static JLabel jLabel;
    public static void display(BufferedImage image){
        if(jFrame ==null){
            jFrame =new JFrame();
            jFrame.setTitle("stained_image");
            jFrame.setSize(image.getWidth(), image.getHeight());
            jFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            jLabel =new JLabel();
            jLabel.setIcon(new ImageIcon(image));
            jFrame.getContentPane().add(jLabel,BorderLayout.CENTER);
            jFrame.setLocationRelativeTo(null);
            jFrame.pack();
            jFrame.setVisible(true);
        }else jLabel.setIcon(new ImageIcon(image));
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
                jLabel.setIcon(images);
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
                System.out.println(band.getName());
                jLabel = new JLabel();
                jLabel.setIcon(images);
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
                jLabel.setIcon(images);
                thread.sleep(1000);
            }
        } catch (InterruptedException e) {
        }
    }



    class AnimatedImage extends BufferedImage {
        GradientPaint[] frameGradient;
        int frame = 0;

        AnimatedImage() {
            super(1000, 1200, BufferedImage.TYPE_INT_RGB);
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


    public static void openProductSceneView(RasterDataNode rasterDataNode) {
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
