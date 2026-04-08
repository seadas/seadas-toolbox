package gov.nasa.gsfc.seadas.earthdatacloud.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CmrVariableMetadataFetcher {

    public static List<VariableItem> fetchVariablesFromCollection(String collectionConceptId) throws Exception {
        if (collectionConceptId == null || collectionConceptId.isBlank()) {
            throw new IllegalArgumentException("collectionConceptId is blank");
        }

        throw new UnsupportedOperationException(
                "CMR /search/variables does not support collection_concept_id. " +
                        "Use FileVariableMetadataFetcher.fetchVariablesFromFile(...) instead."
        );
    }

    private static String stringValue(Object value) {
        return value == null ? null : value.toString().trim();
    }
}
