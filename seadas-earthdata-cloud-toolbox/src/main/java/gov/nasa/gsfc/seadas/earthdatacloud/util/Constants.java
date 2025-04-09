package gov.nasa.gsfc.seadas.earthdatacloud.util;

public class Constants {
    public static final String BASE_CMR_URL = "https://cmr.earthdata.nasa.gov/search/granules.json";
    public static final String PROVIDER = "OB_CLOUD";
    public static final int DEFAULT_MAX_RESULTS = 25;
    public static final int MAX_PAGE_SIZE = 2000;
    public static final String BROWSE_IMAGE_BASE_URL = "https://oceandata.sci.gsfc.nasa.gov/browse_images/";
    public static final String DOWNLOAD_FOLDER = "downloads";
    public static final String PYTHON_METADATA_SCRIPT = "seadas-toolbox/seadas-earthdata-cloud-toolbox/src/main/CMR_script.py";
    public static final String PYTHON_DATERANGE_SCRIPT = "seadas-toolbox/seadas-earthdata-cloud-toolbox/src/main/MissionDateRangeFinder.py";
    public static final String MISSION_LIST_FILE = "mission_names.txt";
    public static final String MISSION_DATE_JSON = "src/main/resources/json-files/mission_date_ranges.json";
    public static final String JSON_DIR = "seadas-toolbox/seadas-earthdata-cloud-toolbox/src/main/resources/json-files";

    public static final String METADATA_JSON = "obdaac_metadata.json";
    public static final String MISSION_RANGES_JSON = "mission_date_ranges.json";
    public static final String MISSION_RANGES_GENERATED_JSON = "mission_date_ranges_from_java.json";

    public static final int DEFAULT_WIDTH = 850;
    public static final int DEFAULT_HEIGHT = 600;

    public static final int DEFAULT_RESULTS_PER_PAGE = 25;
    public static final int MAX_RESULTS = 10000;

}

