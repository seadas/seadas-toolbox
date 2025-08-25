package gov.nasa.gsfc.seadas.earthdatacloud.data;

import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VariableSelector {

    // Canonical target wavelengths per sensor (you can tweak freely)
    private static final Map<String, List<Integer>> SENSOR_TO_RRS_WAVELENGTHS = Map.of(
            "PACE_OCI", List.of(443, 490, 510, 555, 670),
            "MODISA",   List.of(443, 488, 531, 555, 670),
            "MODIST",   List.of(443, 488, 531, 555, 670),
            "VIIRS",    List.of(443, 486, 551, 555, 670) // Keep 555 here too in case some files have it
    );

    // Non-Rrs preferred variables (and simple alias lists to probe)
    private static final List<String> CHLOR_ALIASES = List.of("chlor_a", "chl", "chlor");
    private static final List<String> AOT_ALIASES   = List.of("aot_869", "aot_865", "aot870", "aot_870");

    // Rrs name pattern: e.g., Rrs_443, rrs_488
    private static final Pattern RRS_PATTERN = Pattern.compile("(?i)^Rrs_(\\d{3})$");

    // Rrs selection tolerance in nm when exact wavelength not present
    private static final int RRS_TOLERANCE_NM = 5;

    /**
     * Public entrypoint.
     */
    public List<String> extractVariables(String fileUrlOrPath) throws IOException {
        String sensorKey = detectSensorKey(fileUrlOrPath);
        Set<String> availableVars;

        // Open file and gather available variables
        try (NetcdfFile nc = NetcdfFiles.open(fileUrlOrPath)) {
            availableVars = listVariableNames(nc);
        }

        // Build final selection
        List<String> selected = new ArrayList<>();

        // chlor_a or alias
        pickFirstPresent(CHLOR_ALIASES, availableVars).ifPresent(selected::add);

        // aot_869 or alias
        pickFirstPresent(AOT_ALIASES, availableVars).ifPresent(selected::add);

        // Rrs bands
        List<Integer> targets = SENSOR_TO_RRS_WAVELENGTHS.getOrDefault(sensorKey, defaultRrsTargets());
        selected.addAll(selectRrsBands(targets, availableVars));

        // De-duplicate while preserving order
        LinkedHashSet<String> dedup = new LinkedHashSet<>(selected);
        return new ArrayList<>(dedup);
    }

    /**
     * Simple sensor detection from filename/URL.
     */
    private String detectSensorKey(String fileUrl) {
        String s = fileUrl.toUpperCase(Locale.ROOT);
        if (s.contains("PACE_OCI")) return "PACE_OCI";
        if (s.contains("MODISA"))   return "MODISA";
        if (s.contains("MODIST"))   return "MODIST";
        if (s.contains("VIIRS"))    return "VIIRS";
        return "DEFAULT";
    }

    /**
     * Fallback default Rrs targets if sensor unknown.
     */
    private List<Integer> defaultRrsTargets() {
        // A reasonable generic set
        return List.of(443, 488, 510, 551, 555, 670);
    }

    /**
     * Enumerate variable names present in the file (top-level).
     */
    private Set<String> listVariableNames(NetcdfFile nc) {
        Set<String> out = new HashSet<>();
        for (Variable v : nc.getVariables()) {
            out.add(v.getShortName()); // shortName is the usual public variable name
        }
        return out;
    }

    /**
     * Return the first alias present in the file, case-sensitive match by default (netCDF names are case-sensitive).
     * You can relax to case-insensitive if needed.
     */
    private Optional<String> pickFirstPresent(List<String> aliases, Set<String> available) {
        for (String a : aliases) {
            if (available.contains(a)) return Optional.of(a);
        }
        return Optional.empty();
    }

    /**
     * Pick Rrs variables at target wavelengths. If exact isn't found, pick nearest within tolerance.
     */
    private List<String> selectRrsBands(List<Integer> targets, Set<String> availableVars) {
        // Map wavelengths present -> var name, from all Rrs_* variables we see
        Map<Integer, String> rrsByWavelength = new HashMap<>();
        for (String name : availableVars) {
            Matcher m = RRS_PATTERN.matcher(name);
            if (m.matches()) {
                int wl = Integer.parseInt(m.group(1));
                // Prefer exact mapping; if duplicate wl appears, keep the first seen
                rrsByWavelength.putIfAbsent(wl, name);
            }
        }

        List<String> picks = new ArrayList<>();
        for (int target : targets) {
            String chosen = pickExactOrNearestRrs(target, rrsByWavelength, RRS_TOLERANCE_NM);
            if (chosen != null) {
                picks.add(chosen);
            }
        }
        return picks;
    }

    /**
     * Choose exact Rrs target if present; else choose nearest within tolerance.
     */
    private String pickExactOrNearestRrs(int target, Map<Integer, String> rrsByWavelength, int toleranceNm) {
        if (rrsByWavelength.containsKey(target)) {
            return rrsByWavelength.get(target);
        }
        // Find nearest within tolerance
        Integer bestWl = null;
        int bestDelta = Integer.MAX_VALUE;
        for (Integer wl : rrsByWavelength.keySet()) {
            int d = Math.abs(wl - target);
            if (d < bestDelta) {
                bestDelta = d;
                bestWl = wl;
            }
        }
        if (bestWl != null && bestDelta <= toleranceNm) {
            return rrsByWavelength.get(bestWl);
        }
        return null;
    }

    // --- Optional convenience method if you still want a filename-only fallback ---

    public List<String> filenameOnlyFallback(String fileUrl) {
        String key = detectSensorKey(fileUrl);
        switch (key) {
            case "PACE_OCI": return List.of("chlor_a", "aot_869", "Rrs_443", "Rrs_490", "Rrs_510", "Rrs_555", "Rrs_670");
            case "MODISA":
            case "MODIST":   return List.of("chlor_a", "aot_869", "Rrs_443", "Rrs_488", "Rrs_531", "Rrs_555", "Rrs_670");
            case "VIIRS":    return List.of("chlor_a", "aot_869", "Rrs_443", "Rrs_486", "Rrs_551", "Rrs_555", "Rrs_670");
            default:         return List.of("chlor_a", "aot_869", "Rrs_443", "Rrs_488", "Rrs_510", "Rrs_551", "Rrs_555", "Rrs_670");
        }
    }
}

