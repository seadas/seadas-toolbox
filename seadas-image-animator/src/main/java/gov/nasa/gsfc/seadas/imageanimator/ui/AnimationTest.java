package gov.nasa.gsfc.seadas.imageanimator.ui;

import com.bc.ceres.grender.Viewport;
import gov.nasa.gsfc.seadas.imageanimator.operator.ImageAnimatorOp;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.ui.product.ProductSceneView;

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.MediaTracker;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;

import javax.swing.*;

import static gov.nasa.gsfc.seadas.imageanimator.ui.Animation.openProductSceneView;
import static org.esa.snap.rcp.actions.window.OpenImageViewAction.getProductSceneView;

public class AnimationTest extends JPanel implements ActionListener {
    ImageIcon images[];
    int totalImages = 3, currentImage = 0, animationDelay = 50;
    Timer animationTimer;
    Product product;

    Thread th;
    ImageIcon images1;
    JFrame frame;
    JLabel lbl;
    int i = 0;
    int j;

    public AnimationTest() {



        frame = new JFrame("Animation Frame");
        th = new Thread();
        lbl = new JLabel();
        JPanel panel = new JPanel();
        panel.add(lbl);
        frame.add(panel, BorderLayout.CENTER);
        frame.setSize(400, 400);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        for(j = 1; j <= 2; j++){
            SwingAnimator();
            if (j == 2)
                j = 0;
        }



//        for (int i = 0; i < images.length; ++i)
//            images[i] = new ImageIcon("images/java" + i + ".gif");
        //startAnimation();
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (images[currentImage].getImageLoadStatus() == MediaTracker.COMPLETE) {
            images[currentImage].paintIcon(this, g, 0, 0);
            currentImage = (currentImage + 1) % totalImages;
        }
    }

    public void SwingAnimator(){

        SnapApp snapApp = SnapApp.getDefault();
        final ProductSceneView sceneView = snapApp.getSelectedProductSceneView();
        Viewport standardViewPort = sceneView.getLayerCanvas().getViewport();
        ImageAnimatorOp imageAnimatorOp = new ImageAnimatorOp();
        images = new ImageIcon[totalImages];
        product = snapApp.getSelectedProduct(SnapApp.SelectionSourceHint.VIEW);
        RenderedImage renderedImage;
        RasterDataNode raster1 = product.getRasterDataNode("angstrom");
        openProductSceneView(raster1);
        RasterDataNode  raster2 = product.getRasterDataNode("poc");
        openProductSceneView(raster2);
        RasterDataNode  raster3 = product.getRasterDataNode("ipar");
        openProductSceneView(raster3);

        ProductSceneView myView;

//                ProductSceneView myView = getProductSceneView(raster1);
//                renderedImage = imageAnimatorOp.createImage(myView, standardViewPort);
//
        //renderedImage = myView.getBaseImageLayer().getImage();

        //for (int ii = 0; ii < tiles.length; ii++) {
        myView = getProductSceneView(raster1);
        renderedImage = imageAnimatorOp.createImage(myView, standardViewPort);
        images[0] = new ImageIcon((BufferedImage)renderedImage);

        //}

        myView = getProductSceneView(raster2);
        renderedImage = imageAnimatorOp.createImage(myView, standardViewPort);
        images[1] = new ImageIcon((BufferedImage)renderedImage);

        myView = getProductSceneView(raster3);
        renderedImage = imageAnimatorOp.createImage(myView, standardViewPort);
        images[2] = new ImageIcon((BufferedImage)renderedImage);

        try{
                lbl.setIcon(images[0]);
                th.sleep(1000);

            lbl.setIcon(images[1]);
            th.sleep(1000);

            lbl.setIcon(images[2]);
            th.sleep(1000);

        }
        catch(InterruptedException e){}
    }


    public void actionPerformed(ActionEvent e) {
        repaint();
    }

    public void startAnimation() {
        if (animationTimer == null) {
            currentImage = 0;
            animationTimer = new Timer(animationDelay, this);
            animationTimer.start();
        } else if (!animationTimer.isRunning())
            animationTimer.restart();
    }

    public void stopAnimation() {
        animationTimer.stop();
    }
    public static void main(String args[]) {
        AnimationTest anim = new AnimationTest();
        JFrame app = new JFrame("Animator test");
        app.add(anim, BorderLayout.CENTER);
        app.setSize(500,1000);
        app.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        app.setSize(anim.getPreferredSize().width + 10, anim.getPreferredSize().height + 30);
        app.setVisible(true);
        app.repaint();
    }
}
