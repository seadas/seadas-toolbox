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
                "https://oceandata.sci.gsfc.nasa.gov/browse_images/PACE_OCI.20250420T120000.L2.BGC_NRT.nc.png",
                "https://oceandata.sci.gsfc.nasa.gov/browse_images/PACE_OCI.20250420T120000.L2.BGC.NRT.nc.png"
        ), urls);
    }

    @Test
    void buildsNonNrtFallbackUrlsForGranulesWithNrtSuffix() {
        List<String> urls = ImagePreviewHelper.buildPreviewUrls("PACE_OCI.20250420T120000.L2.BGC_NRT.nc");

        assertEquals(List.of(
                "https://oceandata.sci.gsfc.nasa.gov/browse_images/PACE_OCI.20250420T120000.L2.BGC_NRT.nc.png",
                "https://oceandata.sci.gsfc.nasa.gov/browse_images/PACE_OCI.20250420T120000.L2.BGC.nc.png"
        ), urls);
    }

    @Test
    void usesCmrBrowseLinkBeforeGeneratedFallbackUrls() {
        String cmrBrowseUrl = "https://oceandata.sci.gsfc.nasa.gov/browse_images/PACE_OCI.20240305T000858.L2.OC_BGC.V3_1.nc.png?file_path=PACE_OCI/IMAGES/EDBRS/2024/0305";

        List<String> urls = ImagePreviewHelper.buildPreviewUrls("PACE_OCI.20240305T000858.L2.OC_BGC.V3_1.nc", cmrBrowseUrl);

        assertEquals(cmrBrowseUrl, urls.get(0));
        assertEquals("https://oceandata.sci.gsfc.nasa.gov/browse_images/PACE_OCI.20240305T000858.L2.OC_BGC.V3_1.nc.png", urls.get(1));
    }

    @Test
    void buildsProductNrtFallbackUrlsForVersionedGranules() {
        List<String> urls = ImagePreviewHelper.buildPreviewUrls("PACE_OCI.20260417T000800.L2.OC_BGC.V3_1.nc");

        assertEquals(List.of(
                "https://oceandata.sci.gsfc.nasa.gov/browse_images/PACE_OCI.20260417T000800.L2.OC_BGC.V3_1.nc.png",
                "https://oceandata.sci.gsfc.nasa.gov/browse_images/PACE_OCI.20260417T000800.L2.OC_BGC.V3_1_NRT.nc.png",
                "https://oceandata.sci.gsfc.nasa.gov/browse_images/PACE_OCI.20260417T000800.L2.OC_BGC_NRT.V3_1.nc.png",
                "https://oceandata.sci.gsfc.nasa.gov/browse_images/PACE_OCI.20260417T000800.L2.OC_BGC.V3_1.NRT.nc.png"
        ), urls);
    }
}
