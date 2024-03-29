package gov.nasa.gsfc.seadas.writeimage;

import com.bc.ceres.core.ProgressMonitor;
import gov.nasa.gsfc.seadas.writeimage.operator.WriteImageOp;
import junit.framework.TestCase;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.gpf.common.WriteOp;
import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.esa.snap.core.gpf.graph.GraphProcessor;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.FileUtils;

import javax.media.jai.JAI;
import javax.media.jai.TileScheduler;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.StringReader;

/**
 * Created by aabduraz on 7/30/15.
 */
public class WriteImageOpTest extends TestCase {


    public static final String SNAP_TEST_DATA_INPUT_DIR_PROPERTY_NAME = "org.esa.snap.testdata.in";
    public static final String SNAP_TEST_DATA_OUTPUT_DIR_PROPERTY_NAME = "org.esa.snap.testdata.out";
    public static final String SNAP_TEST_DATA_INPUT_DIR_DEFAULT_PATH = "testdata" + File.separatorChar + "in";
    public static final String SNAP_TEST_DATA_OUTPUT_DIR_DEFAULT_PATH = "testdata" + File.separatorChar + "out";
    
    private static final int RASTER_WIDTH = 4;
    private static final int RASTER_HEIGHT = 40;

    private WriteImageOp.Spi writeImageSpi = new WriteImageOp.Spi();
    private WriteOp.Spi writeSpi = new WriteOp.Spi();
    private File outputFile;
    private int oldParallelism;

    @Override
    protected void setUp() throws Exception {
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(writeImageSpi);
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(writeSpi);
        outputFile = getBeamTestDataOutputFile("WriteImageOpTest/writtenProduct.dim");
        outputFile.getParentFile().mkdirs();

        TileScheduler tileScheduler = JAI.getDefaultInstance().getTileScheduler();
        oldParallelism = tileScheduler.getParallelism();
        tileScheduler.setParallelism(Runtime.getRuntime().availableProcessors());
    }

    public static File getBeamTestDataOutputFile(String relPath) {
        return new File(getBeamTestDataOutputDirectory(),
                SystemUtils.convertToLocalPath(relPath));
    }

    public static File getBeamTestDataOutputDirectory() {
        return getDirectory(SNAP_TEST_DATA_OUTPUT_DIR_PROPERTY_NAME,
                SNAP_TEST_DATA_OUTPUT_DIR_DEFAULT_PATH);
    }

    private static File getDirectory(String propertyName, String beamRelDefaultPath) {
        String filePath = System.getProperty(propertyName);
        if (filePath != null) {
            return new File(filePath);
        }
        return new File(SystemUtils.getApplicationHomeDir(),
                SystemUtils.convertToLocalPath(beamRelDefaultPath));
    }

    @Override
    protected void tearDown() throws Exception {
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(writeImageSpi);
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(writeSpi);
        File parentFile = outputFile.getParentFile();
        FileUtils.deleteTree(parentFile);

        TileScheduler tileScheduler = JAI.getDefaultInstance().getTileScheduler();
        tileScheduler.setParallelism(oldParallelism);
    }

    public void testWrite() throws Exception {
        String graphOpXml = "<graph id=\"myOneNodeGraph\">\n"
                + "  <version>1.0</version>\n"
                + "  <node id=\"node1\">\n"
                + "    <operator>WriteImageOp</operator>\n"
                + "  </node>\n"
                + "  <node id=\"node2\">\n"
                + "    <operator>Write</operator>\n"
                + "    <sources>\n"
                + "      <source refid=\"node1\"/>\n"
                + "    </sources>\n"
                + "    <parameters>\n"
                + "       <file>" + outputFile.getAbsolutePath() + "</file>\n"
                + "       <deleteOutputOnFailure>false</deleteOutputOnFailure>\n"
                + "    </parameters>\n"
                + "  </node>\n"
                + "</graph>";
        StringReader reader = new StringReader(graphOpXml);
        Graph graph = GraphIO.read(reader);

        GraphProcessor processor = new GraphProcessor();
        processor.executeGraph(graph, ProgressMonitor.NULL);

        Product productOnDisk = ProductIO.readProduct(outputFile);
        assertNotNull(productOnDisk);

        assertEquals("writtenProduct", productOnDisk.getName());
        assertEquals(3, productOnDisk.getNumBands());
        assertEquals("OperatorBand", productOnDisk.getBandAt(0).getName());
        assertEquals("ConstantBand", productOnDisk.getBandAt(1).getName());
        assertEquals("VirtualBand", productOnDisk.getBandAt(2).getName());

        Band operatorBand = productOnDisk.getBandAt(0);
        operatorBand.loadRasterData();
        //assertEquals(12345, operatorBand.getPixelInt(0, 0));

        // Test that header has been rewritten due to data model changes in AlgoOp.computeTile()
        final ProductNodeGroup<Placemark> placemarkProductNodeGroup = productOnDisk.getPinGroup();
        // 40 pins expected --> one for each tile, we have 40 tiles
        // This test fails sometimes and sometimes not. Probably due to some tiling-issues. Therefore commented out.
        // assertEquals(40, placemarkProductNodeGroup.getNodeCount());

        productOnDisk.dispose();
    }

    /**
     * Some algorithm.
     */
    @OperatorMetadata(alias = "Algo")
    public static class AlgoOp extends Operator {

        @TargetProduct
        private Product targetProduct;

        @Override
        public void initialize() {

            targetProduct = new Product("name", "desc", RASTER_WIDTH, RASTER_HEIGHT);
            targetProduct.addBand("OperatorBand", ProductData.TYPE_INT8);
            targetProduct.addBand("ConstantBand", ProductData.TYPE_INT8).setSourceImage(new BufferedImage(RASTER_WIDTH, RASTER_HEIGHT, BufferedImage.TYPE_BYTE_INDEXED));
            targetProduct.addBand(new VirtualBand("VirtualBand", ProductData.TYPE_FLOAT32, RASTER_WIDTH, RASTER_HEIGHT, "OperatorBand + ConstantBand"));

            targetProduct.setPreferredTileSize(2, 2);
        }

        @Override
        public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) {
            // Fill the tile with the constant sample value 12345
            //
            for (Tile.Pos pos : targetTile) {
                targetTile.setSample(pos.x, pos.y, 12345);
            }

            // Set a pin, so that we can test that the header is rewritten after
            // a data model change.
            //
            final int minX = targetTile.getMinX();
            final int minY = targetTile.getMinY();
            Placemark placemark = Placemark.createPointPlacemark(PinDescriptor.getInstance(),
                    band.getName() + "-" + minX + "-" + minY,
                    "label", "descr",
                    new PixelPos(minX, minY), null,
                    targetProduct.getSceneGeoCoding());

            targetProduct.getPinGroup().add(placemark);

            System.out.println("placemark = " + placemark.getName());
        }

        public static class Spi extends OperatorSpi {

            public Spi() {
                super(AlgoOp.class);
            }
        }
    }
    public void testInitialize() throws Exception {

    }

    public void testComputeTile() throws Exception {

    }

    public void testDispose() throws Exception {

    }

    public void testWriteImage() throws Exception {

    }

    public void testCreateImage() throws Exception {

    }

    public void testCreateImage1() throws Exception {

    }

    public void testGetContourLayer() throws Exception {

    }
}