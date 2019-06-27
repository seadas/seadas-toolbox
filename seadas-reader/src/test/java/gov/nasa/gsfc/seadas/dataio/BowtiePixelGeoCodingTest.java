package gov.nasa.gsfc.seadas.dataio;

import org.esa.snap.core.dataio.ProductFlipper;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductSubsetBuilder;
import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.ProductUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.esa.snap.core.util.Debug.assertNotNull;
import static org.esa.snap.core.util.Debug.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class BowtiePixelGeoCodingTest {

    @Test
    public void testTransferGeoCoding() throws URISyntaxException, IOException {
        Product product = ProductIO.readProduct(new File(getClass().getResource("bowtiepixelgeocoding_test_product.L2_sub").toURI()));
        assertTrue(product.getSceneGeoCoding() instanceof BowtiePixelGeoCoding);

        Product targetProduct = new Product("name", "type", product.getSceneRasterWidth(), product.getSceneRasterHeight());

        assertNull(targetProduct.getSceneGeoCoding());
        ProductUtils.copyGeoCoding(product, targetProduct);

        assertNotNull(targetProduct.getSceneGeoCoding());
        assertTrue(targetProduct.getSceneGeoCoding() instanceof BowtiePixelGeoCoding);
    }

    @Test
    public void testLatAndLonAreCorrectlySubsetted() throws URISyntaxException, IOException {
        Product product = ProductIO.readProduct(new File(getClass().getResource("bowtiepixelgeocoding_test_product.L2_sub").toURI()));
        GeoCoding sourcceGeoCoding = product.getSceneGeoCoding();
        assertTrue(sourcceGeoCoding instanceof BowtiePixelGeoCoding);

        ProductSubsetDef subsetDef = new ProductSubsetDef();
        subsetDef.setRegion(50, 50, 10, 10);
        subsetDef.addNodeName("chlor_a");
        Product targetProduct = product.createSubset(subsetDef, "subset", "");

        GeoCoding targetSceneGeoCoding = targetProduct.getSceneGeoCoding();
        assertNotNull(targetSceneGeoCoding);
        assertTrue(targetSceneGeoCoding instanceof BowtiePixelGeoCoding);
        assertTrue(targetProduct.containsBand("latitude"));
        assertTrue(targetProduct.containsBand("longitude"));

        PixelPos sourcePixelPos = new PixelPos(50.5f, 50.5f);
        GeoPos expected = sourcceGeoCoding.getGeoPos(sourcePixelPos, new GeoPos());
        PixelPos targetPixelPos = new PixelPos(0.5f, 0.5f);
        GeoPos actual = targetSceneGeoCoding.getGeoPos(targetPixelPos, new GeoPos());
        assertEquals(expected.getLat(), actual.getLat(), 1.0e-6);
        assertEquals(expected.getLon(), actual.getLon(), 1.0e-6);
    }


    @Test
    public void testScanLineOffset() throws URISyntaxException, IOException {
        Product product = ProductIO.readProduct(new File(getClass().getResource("bowtiepixelgeocoding_test_product.L2_sub").toURI()));

        // latitude values increasing
        BowtiePixelGeoCoding bowtiePixelGeoCoding = (BowtiePixelGeoCoding) product.getSceneGeoCoding();
        assertEquals(0, bowtiePixelGeoCoding.getScanlineOffset());

        // flipped product, latitude values decreasing
        Product flippedProduct = ProductFlipper.createFlippedProduct(product, ProductFlipper.FLIP_BOTH, "f", "f");
        bowtiePixelGeoCoding = (BowtiePixelGeoCoding) flippedProduct.getSceneGeoCoding();
        assertEquals(0, bowtiePixelGeoCoding.getScanlineOffset());

        // small product, just one scan (10 lines)
        testScanlineOffsetOnSubset(product, 0, 10, 0);
        // other small products, with different offsets
        testScanlineOffsetOnSubset(product, 0, 30, 0);
        testScanlineOffsetOnSubset(product, 1, 30, 9);
        testScanlineOffsetOnSubset(product, 2, 30, 8);
        testScanlineOffsetOnSubset(product, 3, 30, 7);
        testScanlineOffsetOnSubset(product, 4, 30, 6);
        testScanlineOffsetOnSubset(product, 5, 30, 5);
        testScanlineOffsetOnSubset(product, 6, 30, 4);
        testScanlineOffsetOnSubset(product, 7, 30, 3);
        testScanlineOffsetOnSubset(product, 8, 30, 2);
        testScanlineOffsetOnSubset(product, 9, 30, 1);
        testScanlineOffsetOnSubset(product, 10, 30, 0);
    }

    private static void testScanlineOffsetOnSubset(Product product, int yStart, int heigth, int scanlineOffset) throws IOException {
        ProductSubsetDef subsetDef = new ProductSubsetDef();
        subsetDef.setRegion(0, yStart, product.getSceneRasterWidth(), heigth);
        Product subsetProduct = ProductSubsetBuilder.createProductSubset(product, subsetDef, "s", "s");
        BowtiePixelGeoCoding bowtiePixelGeoCoding = (BowtiePixelGeoCoding) subsetProduct.getSceneGeoCoding();
        assertEquals("for y=" + yStart + " scanlineOffset", scanlineOffset, bowtiePixelGeoCoding.getScanlineOffset());
    }
}