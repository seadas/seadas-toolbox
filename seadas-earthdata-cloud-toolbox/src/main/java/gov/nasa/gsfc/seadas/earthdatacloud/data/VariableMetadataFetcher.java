package gov.nasa.gsfc.seadas.earthdatacloud.data;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class VariableMetadataFetcher {

    /**
     * Try to fetch mission/product-specific variable names from collection metadata.
     * Falls back cleanly if metadata is not rich enough.
     */
    public static List<String> fetchVariables(String collectionConceptId) throws Exception {
        if (collectionConceptId == null || collectionConceptId.isBlank()) {
            throw new IllegalArgumentException("collectionConceptId is blank");
        }

        String cmrUrl = "https://cmr.earthdata.nasa.gov/search/collections.umm_json?concept_id=" + collectionConceptId;
        HttpURLConnection conn = (HttpURLConnection) new URL(cmrUrl).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);

        int status = conn.getResponseCode();
        if (status >= 400) {
            throw new Exception("Collection metadata lookup failed HTTP " + status + " for " + cmrUrl);
        }

        String response = readResponse(conn);
        JSONObject json = new JSONObject(response);
        JSONArray items = json.optJSONArray("items");
        if (items == null || items.isEmpty()) {
            throw new Exception("No collection metadata found for concept id: " + collectionConceptId);
        }

        JSONObject item = items.getJSONObject(0);
        JSONObject umm = item.optJSONObject("umm");
        if (umm == null) {
            throw new Exception("No UMM metadata found for concept id: " + collectionConceptId);
        }

        Set<String> variables = new LinkedHashSet<>();

        // 1. ScienceKeywords → keep only leaf names that look like variable-like product fields
        JSONArray scienceKeywords = umm.optJSONArray("ScienceKeywords");
        if (scienceKeywords != null) {
            for (int i = 0; i < scienceKeywords.length(); i++) {
                JSONObject kw = scienceKeywords.optJSONObject(i);
                if (kw == null) continue;

                String term = kw.optString("VariableLevel1", "").trim();
                addCandidate(variables, term);
                term = kw.optString("VariableLevel2", "").trim();
                addCandidate(variables, term);
                term = kw.optString("VariableLevel3", "").trim();
                addCandidate(variables, term);
                term = kw.optString("DetailedVariable", "").trim();
                addCandidate(variables, term);
            }
        }

        // 2. AdditionalAttributes sometimes contain parameter/measure names
        JSONArray attrs = umm.optJSONArray("AdditionalAttributes");
        if (attrs != null) {
            for (int i = 0; i < attrs.length(); i++) {
                JSONObject attr = attrs.optJSONObject(i);
                if (attr == null) continue;

                String name = attr.optString("Name", "").trim();
                if ("variables".equalsIgnoreCase(name) ||
                        "parameter".equalsIgnoreCase(name) ||
                        "parameters".equalsIgnoreCase(name) ||
                        "measurements".equalsIgnoreCase(name)) {

                    JSONArray values = attr.optJSONArray("Values");
                    if (values != null) {
                        for (int j = 0; j < values.length(); j++) {
                            addCandidate(variables, values.optString(j, ""));
                        }
                    }
                }
            }
        }

        List<String> out = new ArrayList<>(variables);

        // If metadata is too generic, caller should fallback.
        if (out.isEmpty()) {
            throw new Exception("Collection metadata did not contain usable variable names for " + collectionConceptId);
        }

        return out;
    }

    private static void addCandidate(Set<String> vars, String raw) {
        if (raw == null) return;
        String s = raw.trim();
        if (s.isEmpty()) return;

        // Skip obviously generic science-keyword phrases
        if (s.length() > 40) return;
        if (s.contains("OCEAN") || s.contains("EARTH SCIENCE") || s.contains("SPECTRAL")) return;

        // Split comma-separated values if present
        String[] pieces = s.split(",");
        for (String piece : pieces) {
            String v = piece.trim();

            // Normalize a few common naming styles to SeaDAS/Harmony-friendly names when obvious
            v = v.replace(' ', '_');

            if (v.isEmpty()) continue;
            if (v.length() > 40) continue;

            vars.add(v);
        }
    }

    private static String readResponse(HttpURLConnection conn) throws Exception {
        try (InputStream is = conn.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }
}