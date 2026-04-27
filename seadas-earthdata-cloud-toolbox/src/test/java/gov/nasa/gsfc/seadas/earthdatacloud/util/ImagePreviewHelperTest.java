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
}
