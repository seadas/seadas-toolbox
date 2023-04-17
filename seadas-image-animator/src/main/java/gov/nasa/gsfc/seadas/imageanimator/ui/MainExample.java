package gov.nasa.gsfc.seadas.imageanimator.ui;

import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.net.URL;
/*from   w  w  w. j av a2  s  .c  om*/
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class MainExample {

    public static void main(String[] args) throws Exception {
        URL urlImage1 = new URL("http://www.java2s.com/style/download.png");

        final Image fgImage = ImageIO.read(urlImage1);
        int w = fgImage.getWidth(null);
        int h = fgImage.getHeight(null);
        final BufferedImage bgImage = new BufferedImage(w, h,
                BufferedImage.TYPE_INT_RGB);

        final BufferedImage finalImage = new BufferedImage(w, h,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = finalImage.createGraphics();
        g.drawImage(bgImage, 0, 0, null);
        g.drawImage(fgImage, 0, 0, null);
        g.dispose();

        Runnable r = new Runnable() {
            @Override
            public void run() {
                JPanel gui = new JPanel(new GridLayout(1, 0, 5, 5));

                gui.add(new JLabel(new ImageIcon(bgImage)));
                gui.add(new JLabel(new ImageIcon(fgImage)));
                gui.add(new JLabel(new ImageIcon(finalImage)));

                JOptionPane.showMessageDialog(null, gui);
            }
        };
        SwingUtilities.invokeLater(r);
    }
}
