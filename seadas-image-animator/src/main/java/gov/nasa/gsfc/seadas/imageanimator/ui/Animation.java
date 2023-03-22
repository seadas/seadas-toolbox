package gov.nasa.gsfc.seadas.imageanimator.ui;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.ui.product.ProductSceneView;

import java.awt.*;
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
        frame = new JFrame("Animation Frame");
        thread = new Thread();
        label = new JLabel();
        Panel panel = new Panel();
        panel.add(label);
        frame.add(panel, BorderLayout.CENTER);
        frame.setSize(500, 500);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        while (true) {
            swingAnimator();
        }
    }

    public Animation(String frameTitle) {
        frame = new JFrame(frameTitle);
        thread = new Thread();
        label = new JLabel();

        SnapApp snapApp = SnapApp.getDefault();
        final ProductSceneView sceneView = snapApp.getSelectedProductSceneView();
        images = new ImageIcon((Image) sceneView.getBaseImageLayer().getImage());
        label.setIcon(images);
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
        while (true) {
            bandImagesAnimator();
        }
    }

    public void bandImagesAnimator() {
        SnapApp snapApp = SnapApp.getDefault();
        final ProductSceneView sceneView = snapApp.getSelectedProductSceneView();
        snapApp.getProductManager().getProduct(0);
        product = snapApp.getSelectedProduct(SnapApp.SelectionSourceHint.VIEW);
        ProductNodeGroup<Band> products = product.getBandGroup();
        try {
                images = new ImageIcon((Image) sceneView.getBaseImageLayer().getImage());
                //images = new ImageIcon((Image) sceneView.getBaseImageLayer().getImage());
                label.setIcon(images);
                frame.repaint();

                thread.sleep(1000);
        } catch (InterruptedException e)

    {
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
}
