package gov.nasa.gsfc.seadas.panoply;

import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds a Panoply-style metadata subtree.
 * Key part: "Geophysical_Data" with one entry per variable rendered as a plain ncdump-like text block.
 */
public final class PanoplyStyleMetadataBuilder {

    private PanoplyStyleMetadataBuilder() {}

    /** Attach a Panoply-style metadata subtree to a product. */
    public static void attachPanoplyMetadata(Product product, String filePathOrUrl) throws IOException {
        final String fileNameOnly = new File(filePathOrUrl).getName();

        try (NetcdfFile nc = NetcdfFiles.open(filePathOrUrl)) {
            MetadataElement root = new MetadataElement("Panoply");

            // Optional: some compact sections users usually want
            root.addElement(buildGlobalAttributes(nc));
            root.addElement(buildDimensions(nc));
            root.addElement(buildCoverageSummary(nc));

            // Main: ncdump-style text blocks for variables under "geophysical_data"
            root.addElement(buildGeophysicalDataDumps(nc, fileNameOnly));

            product.getMetadataRoot().addElement(root);
        }
    }

    // -------------------- Panoply main block: Geophysical_Data (ncdump-like) --------------------

    private static MetadataElement buildGeophysicalDataDumps(NetcdfFile nc, String fileNameOnly) {
        MetadataElement ge = new MetadataElement("Geophysical_Data");

        for (Variable v : nc.getVariables()) {
            // Only variables under geophysical_data/, skip coordinates & scalars
            String full = v.getFullName(); // e.g. "geophysical_data/chlor_a"
            if (v.isCoordinateVariable() || v.getRank() == 0) continue;
            if (!full.startsWith("geophysical_data/")) continue;
            ge.addElement(makeVarDumpElement(v, fileNameOnly));
        }
        return ge;
    }

    /** Make an element named after the variable, whose attributes' NAMES are the dump lines (values empty). */
    private static MetadataElement makeVarDumpElement(Variable v, String fileNameOnly) {
        MetadataElement varElem = new MetadataElement(v.getShortName());
        List<String> lines = new ArrayList<>(32);

        // Header lines
        lines.add("Variable \"" + v.getShortName() + "\"");
        lines.add("In file \"" + fileNameOnly + "\"");
        lines.add("Variable full name: " + v.getFullName());

        // Declaration
        lines.add(varDecl(v));

        // Panoply-ish attribute order (common CF keys first)
        addAttrLine(lines, "long_name",     v.findAttributeIgnoreCase("long_name"));
        addAttrLine(lines, "units",         v.findAttributeIgnoreCase("units"));
        addAttrLine(lines, "coordinates",   v.findAttributeIgnoreCase("coordinates"));
        addAttrLine(lines, "standard_name", v.findAttributeIgnoreCase("standard_name"));

        addAttrLine(lines, "_FillValue",    v.findAttribute("_FillValue"));
        addAttrLine(lines, "valid_min",     v.findAttribute("valid_min"));
        addAttrLine(lines, "valid_max",     v.findAttribute("valid_max"));
        addAttrLine(lines, "valid_range",   v.findAttribute("valid_range"));

        addAttrLine(lines, "scale_factor",  v.findAttribute("scale_factor"));
        addAttrLine(lines, "add_offset",    v.findAttribute("add_offset"));

        Attribute ref = v.findAttributeIgnoreCase("reference");
        if (ref == null) ref = v.findAttributeIgnoreCase("references");
        addAttrLine(lines, "reference", ref);

        // Optional chunk sizes (best-effort, reflection to handle NetCDF-java API differences)
        String chunk = tryChunkSizesLine(v);
        if (chunk != null) lines.add(chunk);

        // Store each line as an attribute NAME; value left empty to avoid table formatting
        // Append zero-width index at the END to preserve display order.
        for (int i = 0; i < lines.size(); i++) {
            String name = lines.get(i) + "\u200B" + String.format("%06d", i);
            varElem.addAttribute(new MetadataAttribute(name, ProductData.createInstance(""), true));
        }
        return varElem;
    }

    private static String varDecl(Variable v) {
        return v.getDataType().toString().toLowerCase() + " " + v.getShortName() +
                "(" + dimDecl(v.getDimensions()) + ");";
    }

    private static String dimDecl(List<Dimension> dims) {
        if (dims == null || dims.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < dims.size(); i++) {
            if (i > 0) sb.append(", ");
            Dimension d = dims.get(i);
            String name = (d.getShortName() != null && !d.getShortName().isEmpty())
                    ? d.getShortName() : ("dim" + i);
            sb.append(name).append('=').append(d.getLength());
        }
        return sb.toString();
    }

    private static void addAttrLine(List<String> lines, String key, Attribute a) {
        if (a == null) return;
        String value = renderAttrValue(a);
        String typeComment = a.isString() ? "" : (" // " + typeLabel(a.getDataType()));
        lines.add("  :" + key + " = " + value + ";" + typeComment);
    }

    private static String tryChunkSizesLine(Variable v) {
        try {
            int[] sizes = null;

            // NetCDF-Java ≥ 5.5: Variable#getChunkSizes()
            try {
                java.lang.reflect.Method m = v.getClass().getMethod("getChunkSizes");
                Object res = m.invoke(v);
                if (res instanceof int[]) sizes = (int[]) res;
            } catch (NoSuchMethodException ignore) {
                // Older: Variable#getChunking() -> getChunkSizes()
                try {
                    java.lang.reflect.Method gm = v.getClass().getMethod("getChunking");
                    Object chunk = gm.invoke(v);
                    if (chunk != null) {
                        java.lang.reflect.Method gs = chunk.getClass().getMethod("getChunkSizes");
                        Object res = gs.invoke(chunk);
                        if (res instanceof int[]) sizes = (int[]) res;
                    }
                } catch (NoSuchMethodException ignoreToo) {
                    // no-op
                }
            }

            if (sizes != null && sizes.length > 0) {
                StringBuilder sb = new StringBuilder("  :_ChunkSizes = ");
                for (int i = 0; i < sizes.length; i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(sizes[i]).append('U');
                }
                sb.append("; // uint");
                return sb.toString();
            }
        } catch (Throwable ignored) {
            // silently skip if unavailable in this build
        }
        return null;
    }

    // -------------------- Optional compact sections --------------------

    private static MetadataElement buildGlobalAttributes(NetcdfFile nc) {
        MetadataElement e = new MetadataElement("Global_Attributes");
        for (Attribute a : nc.getGlobalAttributes()) {
            e.addAttribute(new MetadataAttribute(
                    a.getShortName(),
                    ProductData.createInstance(renderAttrValue(a)),
                    true));
        }
        return e;
    }

    private static MetadataElement buildDimensions(NetcdfFile nc) {
        MetadataElement e = new MetadataElement("Dimensions");
        for (Dimension d : nc.getDimensions()) {
            String dimName = d.getShortName() != null && !d.getShortName().isEmpty()
                    ? d.getShortName() : "dim";
            MetadataElement de = new MetadataElement(dimName);

            // Use string, NOT ProductData.createInstance(int)
            de.addAttribute(new MetadataAttribute(
                    "length",
                    ProductData.createInstance(Integer.toString(d.getLength())),
                    true));

            de.addAttribute(new MetadataAttribute(
                    "isUnlimited",
                    ProductData.createInstance(Boolean.toString(d.isUnlimited())),
                    true));

            e.addElement(de);
        }
        return e;
    }


    private static MetadataElement buildCoverageSummary(NetcdfFile nc) {
        MetadataElement e = new MetadataElement("Coverage_Summary");
        addIfPresent(e, "time_coverage_start", nc.findGlobalAttributeIgnoreCase("time_coverage_start"));
        addIfPresent(e, "time_coverage_end",   nc.findGlobalAttributeIgnoreCase("time_coverage_end"));
        addIfPresent(e, "geospatial_lat_min",  nc.findGlobalAttributeIgnoreCase("geospatial_lat_min"));
        addIfPresent(e, "geospatial_lat_max",  nc.findGlobalAttributeIgnoreCase("geospatial_lat_max"));
        addIfPresent(e, "geospatial_lon_min",  nc.findGlobalAttributeIgnoreCase("geospatial_lon_min"));
        addIfPresent(e, "geospatial_lon_max",  nc.findGlobalAttributeIgnoreCase("geospatial_lon_max"));
        addIfPresent(e, "geospatial_vertical_min", nc.findGlobalAttributeIgnoreCase("geospatial_vertical_min"));
        addIfPresent(e, "geospatial_vertical_max", nc.findGlobalAttributeIgnoreCase("geospatial_vertical_max"));
        return e;
    }

    private static void addIfPresent(MetadataElement e, String name, Attribute a) {
        if (a != null) {
            e.addAttribute(new MetadataAttribute(name,
                    ProductData.createInstance(renderAttrValue(a)), true));
        }
    }

    // -------------------- Value rendering (Panoply/ncdump-like) --------------------

    private static String renderAttrValue(Attribute a) {
        if (a.isString()) {
            if (a.getLength() <= 1) return quote(a.getStringValue());
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < a.getLength(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(quote(a.getStringValue(i)));
            }
            return sb.toString();
        }

        // numeric: add suffix (f for float, U for unsigned)
        String suf = numericSuffix(a.getDataType());
        if (a.getLength() == 1) {
            Object nv = a.getNumericValue();
            return nv != null ? nv.toString() + suf : "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < a.getLength(); i++) {
            if (i > 0) sb.append(", ");
            Object nv = a.getNumericValue(i);
            if (nv != null) sb.append(nv.toString()).append(suf);
        }
        return sb.toString();
    }

    private static String quote(String s) {
        return s == null ? "\"\"" : "\"" + s.replace("\"", "\\\"") + "\"";
    }

    private static String numericSuffix(DataType dt) {
        if (dt == null) return "";
        switch (dt) {
            case FLOAT:  return "f";
            case UBYTE:
            case USHORT:
            case UINT:
            case ULONG:  return "U";
            default:     return "";
        }
    }

    private static String typeLabel(DataType dt) {
        if (dt == null) return "";
        switch (dt) {
            case UBYTE:  return "ubyte";
            case USHORT: return "ushort";
            case UINT:   return "uint";
            case ULONG:  return "ulong";
            default:     return dt.toString().toLowerCase();
        }
    }
}
