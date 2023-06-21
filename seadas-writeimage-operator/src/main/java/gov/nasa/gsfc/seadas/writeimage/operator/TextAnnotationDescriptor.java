package gov.nasa.gsfc.seadas.writeimage.operator;

import javax.media.jai.OperationDescriptorImpl;
import javax.media.jai.ROI;
import javax.media.jai.registry.RenderedRegistryMode;
import java.util.Arrays;
import java.util.Collection;

public class TextAnnotationDescriptor extends OperationDescriptorImpl {

    private static final String[] paramNames = {
            "roi",
            "band",
            "levels",
            "interval",
            "nodata",
            "strictNodata",
            "simplify",
            "smooth"
    };

    private static final Class[] paramClasses = {
            javax.media.jai.ROI.class,
            Integer.class,
            Collection.class,
            Number.class,
            Collection.class,
            Boolean.class,
            Boolean.class,
            Boolean.class
    };

    // package access for use by ContourOpImage
    static final Object[] paramDefaults = {
            (ROI) null,
            Integer.valueOf(0),
            (Collection) null,
            (Number) null,
            Arrays.asList(Double.NaN, Double.POSITIVE_INFINITY,
                    Double.NEGATIVE_INFINITY, Double.MAX_VALUE),
            Boolean.TRUE,
            Boolean.TRUE,
            Boolean.FALSE,
    };
    public TextAnnotationDescriptor(){
       super(new String[][]{
                        {"GlobalName", "Contour"},
                        {"LocalName", "Contour"},
                        {"Vendor", "org.jaitools.media.jai"},
                        {"Description", "Traces contours based on source image values"},
                        {"DocURL", "http://code.google.com/p/jaitools/"},
                        {"Version", "1.1.0"},

                        {"arg0Desc", paramNames[0] + " an optional ROI"},

                        {"arg1Desc", paramNames[1] + " (Integer, default=0) " +
                                "the source image band to process"},

                        {"arg2Desc", paramNames[2] + " (Collection<? extends Number>) " +
                                "values for which to generate contours"},

                        {"arg3Desc", paramNames[3] + " (Number) " +
                                "interval between contour values (ignored if levels arg is supplied)"},

                        {"arg4Desc", paramNames[4] + " (Collection) " +
                                "values to be treated as NO_DATA; elements can be Number and/or Range"},

                        {"arg5Desc", paramNames[5] + " (Boolean, default=true) " +
                                "if true, use strict NODATA exclusion; if false use accept some NODATA"},

                        {"arg6Desc", paramNames[6] + " (Boolean, default=true) " +
                                "whether to simplify contour lines by removing colinear vertices"},

                        {"arg7Desc", paramNames[7] + " (Boolean, default=false) " +
                                "whether to smooth contour lines using Bezier interpolation"}
                },
                new String[]{RenderedRegistryMode.MODE_NAME},   // supported modes

                1,                                              // number of sources

                paramNames,
                paramClasses,
                paramDefaults,

                null                                            // valid values (none defined)
          );

    }
    public TextAnnotationDescriptor getInstance() {
        return this;
    }
}
