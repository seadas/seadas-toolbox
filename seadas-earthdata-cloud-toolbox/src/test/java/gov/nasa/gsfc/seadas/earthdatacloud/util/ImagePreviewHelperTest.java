package gov.nasa.gsfc.seadas.earthdatacloud.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImagePreviewHelperTest {

    @Test
    void buildsNrtFallbackUrlsForGranulesWithoutNrtSuffix() {
        List<String> urls = ImagePreviewHelper.buildPreviewUrls("PACE_OCI.20250420T120000.L2.BGC.nc");

        assertEquals(List.of(
                "https://oceandata.sci.gsfc.nasa.gov/browse_images/PACE_OCI.20250420T120000.L2.BGC.nc.png",
                "https://oceandata.sci.gsfc.nasa.gov/browse_images/PACE_OCI.20250420T120000.L2.OC_BGC.nc.png",
                "https://oceandata.sci.gsfc.nasa.gov/browse_images/PACE_OCI.20250420T120000.L2.BGC_NRT.nc.png",
                "https://oceandata.sci.gsfc.nasa.gov/browse_images/PACE_OCI.20250420T120000.L2.BGC.NRT.nc.png",
                "https://oceandata.sci.gsfc.nasa.gov/browse_images/PACE_OCI.20250420T120000.L2.OC_BGC_NRT.nc.png",
                "https://oceandata.sci.gsfc.nasa.gov/browse_images/PACE_OCI.20250420T120000.L2.OC_BGC.NRT.nc.png"
        ), urls);
    }

    @Test
    void buildsNonNrtFallbackUrlsForGranulesWithNrtSuffix() {
        List<String> urls = ImagePreviewHelper.buildPreviewUrls("PACE_OCI.20250420T120000.L2.BGC_NRT.nc");

        assertEquals(List.of(
                "https://oceandata.sci.gsfc.nasa.gov/browse_images/PACE_OCI.20250420T120000.L2.BGC_NRT.nc.png",
                "https://oceandata.sci.gsfc.nasa.gov/browse_images/PACE_OCI.20250420T120000.L2.OC_BGC_NRT.nc.png",
                "https://oceandata.sci.gsfc.nasa.gov/browse_images/PACE_OCI.20250420T120000.L2.BGC.nc.png",
                "https://oceandata.sci.gsfc.nasa.gov/browse_images/PACE_OCI.20250420T120000.L2.OC_BGC.nc.png"
        ), urls);
    }

    @Test
    void buildsNonNrtFallbackUrlForPaceOcBgcVersionedGranulesWithNrtSuffix() {
        List<String> urls = ImagePreviewHelper.buildPreviewUrls("PACE_OCI.20250702T141322.L2.OC_BGC.V3_1.NRT.nc");

        assertEquals(List.of(
                "https://oceandata.sci.gsfc.nasa.gov/browse_images/PACE_OCI.20250702T141322.L2.OC_BGC.V3_1.NRT.nc.png",
                "https://oceandata.sci.gsfc.nasa.gov/browse_images/PACE_OCI.20250702T141322.L2.BGC.V3_1.NRT.nc.png",
                "https://oceandata.sci.gsfc.nasa.gov/browse_images/PACE_OCI.20250702T141322.L2.OC_BGC.V3_1.nc.png",
                "https://oceandata.sci.gsfc.nasa.gov/browse_images/PACE_OCI.20250702T141322.L2.BGC.V3_1.nc.png"
        ), urls);
    }

    @Test
    void buildsNrtFallbackUrlForPaceOcBgcVersionedGranulesWithoutNrtSuffix() {
        List<String> urls = ImagePreviewHelper.buildPreviewUrls("PACE_OCI.20250702T141322.L2.OC_BGC.V3_1.nc");

        assertEquals(List.of(
                "https://oceandata.sci.gsfc.nasa.gov/browse_images/PACE_OCI.20250702T141322.L2.OC_BGC.V3_1.nc.png",
                "https://oceandata.sci.gsfc.nasa.gov/browse_images/PACE_OCI.20250702T141322.L2.BGC.V3_1.nc.png",
                "https://oceandata.sci.gsfc.nasa.gov/browse_images/PACE_OCI.20250702T141322.L2.OC_BGC.V3_1_NRT.nc.png",
                "https://oceandata.sci.gsfc.nasa.gov/browse_images/PACE_OCI.20250702T141322.L2.OC_BGC.V3_1.NRT.nc.png",
                "https://oceandata.sci.gsfc.nasa.gov/browse_images/PACE_OCI.20250702T141322.L2.BGC.V3_1_NRT.nc.png",
                "https://oceandata.sci.gsfc.nasa.gov/browse_images/PACE_OCI.20250702T141322.L2.BGC.V3_1.NRT.nc.png"
        ), urls);
    }

    @Test
    void buildsFallbackUrlsForRealPaceOcBgcV31GranuleNames() {
        List<String> urls = ImagePreviewHelper.buildPreviewUrls("PACE_OCI.20250916T105942.L2.OC_BGC.V3_1.nc");

        assertEquals(List.of(
                "https://oceandata.sci.gsfc.nasa.gov/browse_images/PACE_OCI.20250916T105942.L2.OC_BGC.V3_1.nc.png",
                "https://oceandata.sci.gsfc.nasa.gov/browse_images/PACE_OCI.20250916T105942.L2.BGC.V3_1.nc.png",
                "https://oceandata.sci.gsfc.nasa.gov/browse_images/PACE_OCI.20250916T105942.L2.OC_BGC.V3_1_NRT.nc.png",
                "https://oceandata.sci.gsfc.nasa.gov/browse_images/PACE_OCI.20250916T105942.L2.OC_BGC.V3_1.NRT.nc.png",
                "https://oceandata.sci.gsfc.nasa.gov/browse_images/PACE_OCI.20250916T105942.L2.BGC.V3_1_NRT.nc.png",
                "https://oceandata.sci.gsfc.nasa.gov/browse_images/PACE_OCI.20250916T105942.L2.BGC.V3_1.NRT.nc.png"
        ), urls);
    }

    @Test
    void buildsFallbackUrlsForRealPaceOcBgcV30NrtGranuleNames() {
        List<String> urls = ImagePreviewHelper.buildPreviewUrls("PACE_OCI.20250110T172325.L2.OC_BGC.V3_0.NRT.nc");

        assertEquals(List.of(
                "https://oceandata.sci.gsfc.nasa.gov/browse_images/PACE_OCI.20250110T172325.L2.OC_BGC.V3_0.NRT.nc.png",
                "https://oceandata.sci.gsfc.nasa.gov/browse_images/PACE_OCI.20250110T172325.L2.BGC.V3_0.NRT.nc.png",
                "https://oceandata.sci.gsfc.nasa.gov/browse_images/PACE_OCI.20250110T172325.L2.OC_BGC.V3_0.nc.png",
                "https://oceandata.sci.gsfc.nasa.gov/browse_images/PACE_OCI.20250110T172325.L2.BGC.V3_0.nc.png"
        ), urls);
    }

    @Test
    void buildsFallbackUrlsForRealPaceOcAopNrtGranuleNames() {
        List<String> urls = ImagePreviewHelper.buildPreviewUrls("PACE_OCI.20250809T100037.L2.OC_AOP.V3_1.NRT.nc");

        assertEquals(List.of(
                "https://oceandata.sci.gsfc.nasa.gov/browse_images/PACE_OCI.20250809T100037.L2.OC_AOP.V3_1.NRT.nc.png",
                "https://oceandata.sci.gsfc.nasa.gov/browse_images/PACE_OCI.20250809T100037.L2.AOP.V3_1.NRT.nc.png",
                "https://oceandata.sci.gsfc.nasa.gov/browse_images/PACE_OCI.20250809T100037.L2.OC_AOP.V3_1.nc.png",
                "https://oceandata.sci.gsfc.nasa.gov/browse_images/PACE_OCI.20250809T100037.L2.AOP.V3_1.nc.png"
        ), urls);
    }

    @Test
    void buildsFilePathFallbackUrlForPaceL1bGranuleNames() {
        List<String> urls = ImagePreviewHelper.buildPreviewUrls("PACE_OCI.20250814T230607.L1B.V3.nc");

        assertEquals(List.of(
                "https://oceandata.sci.gsfc.nasa.gov/browse_images/PACE_OCI.20250814T230607.L1B.V3.nc.png?file_path=PACE_OCI/IMAGES/EDBRS/2025/0814",
                "https://oceandata.sci.gsfc.nasa.gov/browse_images/PACE_OCI.20250814T230607.L1B.V3.nc.png",
                "https://oceandata.sci.gsfc.nasa.gov/browse_images/PACE_OCI.20250814T230607.L1B.V3_NRT.nc.png?file_path=PACE_OCI/IMAGES/EDBRS/2025/0814",
                "https://oceandata.sci.gsfc.nasa.gov/browse_images/PACE_OCI.20250814T230607.L1B.V3_NRT.nc.png",
                "https://oceandata.sci.gsfc.nasa.gov/browse_images/PACE_OCI.20250814T230607.L1B.V3.NRT.nc.png?file_path=PACE_OCI/IMAGES/EDBRS/2025/0814",
                "https://oceandata.sci.gsfc.nasa.gov/browse_images/PACE_OCI.20250814T230607.L1B.V3.NRT.nc.png"
        ), urls);
    }
}
