package gov.nasa.gsfc.seadas.earthdatacloud.data;

import gov.nasa.gsfc.seadas.earthdatacloud.auth.WebPageFetcherWithJWT;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

public class FileVariableMetadataFetcher {

    private static final Set<String> EXCLUDED_EXACT = Set.of(
            // time / bookkeeping
            "year", "day", "msec",

            // instrument / detector metadata
            "detnum", "mside",

            // navigation / structural
            "wavelength",

            // flags
            "l2_flags",

            // calibration / geometry
            "f0", "tau_r", "tilt",

            // calibration tables
            "vcal_gain", "vcal_offset",

            // control grid
            "cntl_pt_rows", "cntl_pt_cols",

            // SeaDAS-reader-hidden variables
            "aw",
            "bbw",
            "csol_z",
            "k_no2",
            "k_oz",
            "rrsdiff"
    );

    private static final String[] EXCLUDED_PREFIXES = new String[] {
            "flag_",
            "qual_",
            "qc_"
    };

    private static boolean isUserFacingScienceVariable(String name) {
        if (name == null || name.isBlank()) return false;

        String lower = name.toLowerCase(Locale.ROOT);

        if (EXCLUDED_EXACT.contains(lower)) return false;

        for (String prefix : EXCLUDED_PREFIXES) {
            if (lower.startsWith(prefix)) return false;
        }

        return true;
    }
    /**
     * Read actual variables from the NetCDF file itself.
     * This is the preferred source for the variable picker.
     */
    public static List<String> fetchVariablesFromFile(String fileUrl) throws Exception {
        if (fileUrl == null || fileUrl.isBlank()) {
            throw new IllegalArgumentException("fileUrl is blank");
        }

        Path tmp = null;
        try {
            tmp = downloadToTemp(fileUrl);

            try (NetcdfFile nc = NetcdfFile.open(tmp.toString())) {
                List<Variable> vars = nc.getVariables();

                Set<String> dimensionNames = nc.getDimensions()
                        .stream()
                        .map(Dimension::getShortName)
                        .collect(Collectors.toSet());

                List<String> candidates = new ArrayList<>();

                for (Variable v : vars) {
                    String shortName = v.getShortName();
                    String fullName = v.getFullName();

                    if (shortName == null || shortName.isBlank()) continue;
                    if (fullName == null || fullName.isBlank()) continue;

                    if (isCoordinateOrStructural(v, dimensionNames)) continue;
                    if (!isUserFacingScienceVariable(shortName)) continue;

                    candidates.add(fullName);
                }

                Collections.sort(candidates, String.CASE_INSENSITIVE_ORDER);
                return candidates;
            }

        } finally {
            if (tmp != null) {
                try {
                    Files.deleteIfExists(tmp);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static Path downloadToTemp(String fileUrl) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(fileUrl).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(30_000);
        conn.setReadTimeout(120_000);
        conn.setInstanceFollowRedirects(true);

        // Token optional; many granules are public
        try {
            String token = WebPageFetcherWithJWT.getAccessToken("urs.earthdata.nasa.gov");
            if (token != null && !token.isBlank()) {
                conn.setRequestProperty("Authorization", "Bearer " + token);
            }
        } catch (Exception ignored) {
        }

        int status = conn.getResponseCode();
        if (status >= 400) {
            throw new Exception("Failed to fetch file metadata from source file. HTTP " + status + " for " + fileUrl);
        }

        String fileName = extractFileName(fileUrl);
        String suffix = fileName.endsWith(".nc") ? ".nc" : ".tmp";
        Path tmp = Files.createTempFile("seadas_varmeta_", suffix);

        try (InputStream in = conn.getInputStream();
             OutputStream out = Files.newOutputStream(tmp)) {

            in.transferTo(out);
        }

        return tmp;
    }

    private static boolean isCoordinateOrStructural(Variable v, Set<String> dimensionNames) {
        String name = v.getShortName();

        // Skip if variable name matches a dimension exactly
        if (dimensionNames.contains(name)) return true;

        // Skip common coordinate/navigation variables
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.equals("lat") || lower.equals("latitude") ||
                lower.equals("lon") || lower.equals("longitude") ||
                lower.equals("time") ||
                lower.equals("x") || lower.equals("y") ||
                lower.equals("number_of_lines") ||
                lower.equals("pixels_per_line")) {
            return true;
        }

        // Skip variables explicitly marked as coordinates/bounds
        Attribute standardName = v.findAttributeIgnoreCase("standard_name");
        if (standardName != null) {
            String s = standardName.getStringValue();
            if (s != null) {
                String sl = s.toLowerCase(Locale.ROOT);
                if (sl.contains("latitude") || sl.contains("longitude") || sl.equals("time")) {
                    return true;
                }
            }
        }

        Attribute axis = v.findAttributeIgnoreCase("axis");
        if (axis != null) return true;

        Attribute bounds = v.findAttributeIgnoreCase("bounds");
        if (bounds != null) return true;

        Attribute cfRole = v.findAttributeIgnoreCase("cf_role");
        if (cfRole != null) return true;

        // Skip scalar bookkeeping variables with no dimensions unless they look science-like
        if (v.getDimensions().isEmpty()) {
            return true;
        }

        return false;
    }

    private static String extractFileName(String url) {
        int slash = url.lastIndexOf('/');
        String name = slash >= 0 ? url.substring(slash + 1) : url;
        int q = name.indexOf('?');
        return q > 0 ? name.substring(0, q) : name;
    }
}
