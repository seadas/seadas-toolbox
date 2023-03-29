package gov.nasa.gsfc.seadas.imageanimator.ui;

import com.bc.ceres.glevel.MultiLevelImage;
import gov.nasa.gsfc.seadas.imageanimator.operator.ImageAnimatorOp;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.ui.product.ProductSceneView;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import javax.swing.*;

public class Animation {
    Thread thread;
    ImageIcon images;
    JFrame frame;
    JLabel label;
    int i = 0;
    int j;
    final int numImages = 3;

    Product product;

    public static void main(String[] args) {
        Animation sa = new Animation();
    }

    public Animation() {
    }

//    public Animation(String frameTitle) {
//        frame = new JFrame(frameTitle);
//        thread = new Thread();
//        label = new JLabel();
//        Panel panel = new Panel();
//        panel.add(label);
//        frame.add(panel, BorderLayout.CENTER);
//        frame.setSize(500, 500);
//        frame.setVisible(true);
//        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        while (true) {
//            bandImagesAnimator();
//        }
//    }

    public void startAnimate(){

        SnapApp snapApp = SnapApp.getDefault();
        final ProductSceneView sceneView = snapApp.getSelectedProductSceneView();
        ImageAnimatorOp imageAnimatorOp = new ImageAnimatorOp();
        RenderedImage renderedImage = imageAnimatorOp.createImage(sceneView);
        BufferedImage bufferedImage = ImageAnimatorOp.toBufferedImage(renderedImage);
        Product testProdcut = snapApp.getProductManager().getProduct(0);
        product = snapApp.getSelectedProduct(SnapApp.SelectionSourceHint.VIEW);

        ProductNodeGroup<Band> products = product.getBandGroup();
        ProductNode[] productNodeList = products.toArray();

        MultiLevelImage sourceImage = product.getBandAt(0).getSourceImage();

        Runnable r = new Runnable() {
            @Override
            public void run() {
                JPanel gui = new JPanel();

                final AnimatedImage[] tiles = new AnimatedImage[2];
                final BufferedImage[] animatedImages = new BufferedImage[products.getNodeCount()];

                MultiLevelImage sourceImage;
                BufferedImage bufferedImage;

                for (int i = 0; i < products.getNodeCount(); i++){
                    bufferedImage = ImageAnimatorOp.toBufferedImage(product.getBandAt(i).getSourceImage());
                    if( bufferedImage != null) {
                        animatedImages[i] = bufferedImage;
                    }

                    gui.add(new JLabel(new ImageIcon(bufferedImage )));
                }

//                for (int ii = 0; ii < tiles.length; ii++) {
//                    tiles[ii] = new AnimatedImage();
//                    gui.add(new JLabel(new ImageIcon(bufferedImage)));
//                }
                ActionListener listener = new ActionListener() {
//
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        for (int i = 0; i < tiles.length; i++) {
                            tiles[i].paintImage();
                            gui.repaint();
                        }
                    }

//                    @Override
//                    public void actionPerformed(ActionEvent e) {
//                        for (int i = 0; i < animatedImages.length; i++) {
//                            Graphics2D g2d = animatedImages[i].createGraphics();
//                            AffineTransform transform = createTransform(product.getBandAt(i), animatedImages[i]);
//                            g2d.drawRenderedImage(animatedImages[i], transform);
//                            gui.repaint();
//                        }
//                    }

                    private AffineTransform createTransform(RasterDataNode raster, BufferedImage image) {

                        AffineTransform transform = raster.getSourceImage().getModel().getImageToModelTransform(0);
                        transform.concatenate(createTransform(raster, image));
                        return transform;
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
//                label.setIcon(images);
//                thread.sleep(1000);
//            }
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
//
//    }

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

}
