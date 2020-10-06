package gov.nasa.gsfc.seadas.processing.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: Aynur Abdurazik (aabduraz)
 * Date: 6/20/12
 * Time: 3:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class ProcessorTypeInfo {

    public static enum ProcessorID {
        EXTRACTOR,
        MODIS_L1A,
        MODIS_GEO,
        GEOLOCATE_VIIRS,
        L1BGEN,
        MODIS_L1B,
        CALIBRATE_VIIRS,
        L1BRSGEN,
        L2BRSGEN,
        L1MAPGEN,
        L2MAPGEN,
        L2BIN,
        L2BIN_AQUARIUS,
        L3BIN,
        L3MAPGEN,
        SMIGEN,
        SMITOPPM,
        LONLAT2PIXLINE,
//        MULTILEVEL_PROCESSOR_PY,
        MULTILEVEL_PROCESSOR,
        OCSSW_INSTALLER,
        L2GEN,
        L3GEN,
        L2GEN_AQUARIUS,
        L3BINDUMP,
        OBPG_FILE_TYPE_PY,
        NEXT_LEVEL_NAME_PY,
        UPDATE_LUTS,
        NOID
    }

    private static final Map<String, ProcessorID> processorHashMap = new HashMap<String, ProcessorID>() {{

        put("l1aextract_modis", ProcessorID.EXTRACTOR);
        put("l1aextract_seawifs", ProcessorID.EXTRACTOR);
        put("l1aextract_viirs", ProcessorID.EXTRACTOR);
        put("l2extract", ProcessorID.EXTRACTOR);
        put("extractor", ProcessorID.EXTRACTOR);
        put("modis_L1A", ProcessorID.MODIS_L1A);
        put("modis_GEO", ProcessorID.MODIS_GEO);
        put("geolocate_viirs", ProcessorID.GEOLOCATE_VIIRS);
        put("l1bgen", ProcessorID.L1BGEN);
        put("modis_L1B", ProcessorID.MODIS_L1B);
        put("calibrate_viirs", ProcessorID.CALIBRATE_VIIRS);
        put("l1brsgen", ProcessorID.L1BRSGEN);
        put("l2brsgen", ProcessorID.L2BRSGEN);
        put("l1mapgen", ProcessorID.L1MAPGEN);
        put("l2mapgen", ProcessorID.L2MAPGEN);
        put("l2bin", ProcessorID.L2BIN);
        put("l2bin_aquarius", ProcessorID.L2BIN_AQUARIUS);
        put("l2gen", ProcessorID.L2GEN);
        put("l3gen", ProcessorID.L3GEN);
        put("l2gen_aquarius", ProcessorID.L2GEN_AQUARIUS);
        put("l3bin", ProcessorID.L3BIN);
        put("l3mapgen", ProcessorID.L3MAPGEN);
        put("smigen", ProcessorID.SMIGEN);
        put("smitoppm", ProcessorID.SMITOPPM);
        put("lonlat2pixline", ProcessorID.LONLAT2PIXLINE);
//        put("multilevel_processor", ProcessorID.MULTILEVEL_PROCESSOR_PY);
        put("multilevel_processor", ProcessorID.MULTILEVEL_PROCESSOR);
        put("install_ocssw", ProcessorID.OCSSW_INSTALLER);
        put("l3bindump", ProcessorID.L3BINDUMP);
        put("obpg_file_type", ProcessorID.OBPG_FILE_TYPE_PY);
        put("next_level_name", ProcessorID.NEXT_LEVEL_NAME_PY);
        put("update_luts", ProcessorID.UPDATE_LUTS);

    }};

    public static String getExcludedProcessorNames() {
        return     "multilevel_processor" +
                "smitoppm" +
                "l1aextract_modis" +
                "l1aextract_seawifs" +
                "l2extract" +
                "next_level_name" +
                "obpg_file_type"+
                "lonlat2pixline";
    }


    public static Set<String> getProcessorNames() {
        return processorHashMap.keySet();
    }

    public static ProcessorID getProcessorID(String processorName) {
        return processorHashMap.get(processorName);
    }
}
