package gov.nasa.gsfc.seadas.imageanimator.operator;

import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glevel.MultiLevelImage;
import com.bc.ceres.grender.Viewport;
import com.bc.ceres.grender.support.BufferedImageRendering;
import com.bc.ceres.grender.support.DefaultViewport;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.util.math.MathUtils;
import org.esa.snap.ui.product.ProductSceneImage;
import org.esa.snap.ui.product.ProductSceneView;

import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.operator.FormatDescriptor;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.*;
import java.util.Hashtable;

public class ImageAnimatorOp {


    private int imageWidth;
    private int imageHeight;
    double heightWidthRatio;

   public ImageAnimatorOp(){

   }
    public RenderedImage createImage(ProductSceneView view) {
        final boolean useAlpha = true; //!BMP_FORMAT_DESCRIPTION[0].equals(imageFormat) && !JPEG_FORMAT_DESCRIPTION[0].equals(imageFormat);
        final boolean entireImage = false;
        final boolean geoReferenced = false; //GEOTIFF_FORMAT_DESCRIPTION[0].equals(imageFormat)
        Dimension dimension = new Dimension(getImageDimensions(view, entireImage));
        return createImage(view, entireImage, dimension, useAlpha, geoReferenced);
    }
    static RenderedImage createImage(ProductSceneView view, boolean fullScene, Dimension dimension,
                                     boolean alphaChannel, boolean geoReferenced) {
        final int imageType = alphaChannel ? BufferedImage.TYPE_4BYTE_ABGR : BufferedImage.TYPE_3BYTE_BGR;
       // final BufferedImage bufferedImage = new BufferedImage(dimension.width, dimension.height, imageType);
        final BufferedImage bufferedImage = new BufferedImage(804, 1739, imageType);
        final BufferedImageRendering imageRendering = createRendering(view, fullScene,
                geoReferenced, bufferedImage);
        if (!alphaChannel) {
            final Graphics2D graphics = imageRendering.getGraphics();
            graphics.setColor(view.getLayerCanvas().getBackground());
            graphics.fillRect(0, 0, dimension.width, dimension.height);
        }
        view.getRootLayer().render(imageRendering);

        return bufferedImage;
    }

    private static BufferedImageRendering createRendering(ProductSceneView view, boolean fullScene,
                                                          boolean geoReferenced, BufferedImage bufferedImage) {
        final Viewport vp1 = view.getLayerCanvas().getViewport();
        final Viewport vp2 = new DefaultViewport(new Rectangle(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight()),
                vp1.isModelYAxisDown());
        if (fullScene) {
            vp2.zoom(view.getBaseImageLayer().getModelBounds());
        } else {
            setTransform(vp1, vp2);
        }

        final BufferedImageRendering imageRendering = new BufferedImageRendering(bufferedImage, vp2);
        if (geoReferenced) {
            // because image to model transform is stored with the exported image we have to invert
            // image to view transformation
            final AffineTransform m2iTransform = view.getBaseImageLayer().getModelToImageTransform(0);
            final AffineTransform v2mTransform = vp2.getViewToModelTransform();
            v2mTransform.preConcatenate(m2iTransform);
            final AffineTransform v2iTransform = new AffineTransform(v2mTransform);
            final Graphics2D graphics2D = imageRendering.getGraphics();
            v2iTransform.concatenate(graphics2D.getTransform());
            graphics2D.setTransform(v2iTransform);
        }
        return imageRendering;
    }

    private static void setTransform(Viewport vp1, Viewport vp2) {
        vp2.setTransform(vp1);

        final Rectangle rectangle1 = vp1.getViewBounds();
        final Rectangle rectangle2 = vp2.getViewBounds();
        final double w1 = rectangle1.getWidth();
        final double w2 = rectangle2.getWidth();
        final double h1 = rectangle1.getHeight();
        final double h2 = rectangle2.getHeight();
        final double x1 = rectangle1.getX();
        final double y1 = rectangle1.getY();
        final double cx = (x1 + w1) / 2.0;
        final double cy = (y1 + h1) / 2.0;

        final double magnification;
        if (w1 > h1) {
            magnification = w2 / w1;
        } else {
            magnification = h2 / h1;
        }

        final Point2D modelCenter = vp1.getViewToModelTransform().transform(new Point2D.Double(cx, cy), null);
        final double zoomFactor = vp1.getZoomFactor() * magnification;
        if (zoomFactor > 0.0) {
            vp2.setZoomFactor(zoomFactor, modelCenter.getX(), modelCenter.getY());
        }
    }

    private void reformatSourceImage(Band band, ImageLayout imageLayout) {
        RenderingHints renderingHints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, imageLayout);
        MultiLevelImage sourceImage = band.getSourceImage();
        Raster r = sourceImage.getData();
        DataBuffer db = r.getDataBuffer();
        int t = db.getDataType();
        int dataType = sourceImage.getData().getDataBuffer().getDataType();
        RenderedImage newImage = FormatDescriptor.create(sourceImage, dataType, renderingHints);
        band.setSourceImage(newImage);
    }

    /**
     * Converts the given rendered image into an image of the given {#link java.awt.image.BufferedImage} type.
     *
     * @param image     the source image
     * @param imageType the  {#link java.awt.image.BufferedImage} type
     * @return the buffered image of the given type
     */
    public static BufferedImage convertImage(RenderedImage image, int imageType) {
        final BufferedImage newImage;
        final int width = image.getWidth();
        final int height = image.getHeight();
        if (imageType != BufferedImage.TYPE_CUSTOM) {
            newImage = new BufferedImage(width, height, imageType);
        } else {
            // create custom image
            final ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
            final ColorModel cm = new ComponentColorModel(cs, false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
            final WritableRaster wr = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, width, height, 3 * width, 3,
                    new int[]{2, 1, 0}, null);
            newImage = new BufferedImage(cm, wr, false, null);
        }
        final Graphics2D graphics = newImage.createGraphics();
        graphics.drawRenderedImage(image, null);
        graphics.dispose();
        return newImage;
    }

    public Dimension getImageDimensions(ProductSceneView view, boolean full) {
        final Rectangle2D bounds;
        if (full) {
            final ImageLayer imageLayer = view.getBaseImageLayer();
            final Rectangle2D modelBounds = imageLayer.getModelBounds();
            Rectangle2D imageBounds = imageLayer.getModelToImageTransform().createTransformedShape(modelBounds).getBounds2D();

            final double mScale = modelBounds.getWidth() / modelBounds.getHeight();
            final double iScale = imageBounds.getHeight() / imageBounds.getWidth();
            double scaleFactorX = mScale * iScale;
            bounds = new Rectangle2D.Double(0, 0, scaleFactorX * imageBounds.getWidth(), 1 * imageBounds.getHeight());
        } else {
            bounds = view.getLayerCanvas().getViewport().getViewBounds();
        }

        imageWidth = toInteger(bounds.getWidth());
        imageHeight = toInteger(bounds.getHeight());
        heightWidthRatio = (double) imageHeight / (double) imageWidth;
        return new Dimension(imageWidth, imageHeight);
    }

    private int toInteger(double value) {
        return MathUtils.floorInt(value);
    }

    public static BufferedImage toBufferedImage(RenderedImage image) {
        if (image instanceof BufferedImage) {
            return (BufferedImage) image;
        }

        ColorModel colorModel = image.getColorModel();
        int width = image.getWidth();
        int height = image.getHeight();
        WritableRaster raster = colorModel.createCompatibleWritableRaster(width, height);
        boolean isAlphaPremultiplied = colorModel.isAlphaPremultiplied();
        Hashtable props = new Hashtable();
        String[] keys = image.getPropertyNames();

        if (keys != null) {
            for (int i = 0; i < keys.length; i++) {
                props.put(keys[i], image.getProperty(keys[i]));
            }
        }
        BufferedImage bufferedImage = new BufferedImage(colorModel, raster, isAlphaPremultiplied, props);
        image.copyData(raster);

        return bufferedImage;

    }

}
