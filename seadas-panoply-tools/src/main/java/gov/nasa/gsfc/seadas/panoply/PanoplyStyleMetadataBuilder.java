package gov.nasa.gsfc.seadas.panoply;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.constants.CF;

import java.io.IOException;
import java.util.List;

public final class PanoplyStyleMetadataBuilder {

    private PanoplyStyleMetadataBuilder() {}

    /** Attach a Panoply-style metadata subtree to a product. Idempotent. */
    public static void attachPanoplyMetadata(Product product, String fileUrlOrPath) throws IOException {
        // Prevent duplicates if called twice
        if (product.getMetadataRoot().getElement("Panoply") != null) {
            return;
        }

        try (NetcdfFile nc = NetcdfFiles.open(fileUrlOrPath)) {
            MetadataElement root = new MetadataElement("Panoply");

            root.addElement(buildGlobalAttributes(nc));
            root.addElement(buildDimensions(nc));
            root.addElement(buildGeophysicalData(nc));                 // Panoply-like per-variable dump
            root.addElement(buildCoordinateSystems(fileUrlOrPath));    // uses NetcdfDataset
            root.addElement(buildCoverageSummary(nc));

            product.getMetadataRoot().addElement(root);
        }
    }

    // -------------------- Sections --------------------

    private static MetadataElement buildGlobalAttributes(NetcdfFile nc) {
        MetadataElement e = new MetadataElement("Global_Attributes");
        for (Attribute a : nc.getGlobalAttributes()) {
            e.addAttribute(toMetaAttr(a));
        }
        return e;
    }

    private static MetadataElement buildDimensions(NetcdfFile nc) {
        MetadataElement e = new MetadataElement("Dimensions");
        for (Dimension d : nc.getDimensions()) {
            MetadataElement de = new MetadataElement(d.getShortName());
            de.addAttribute(sAttr("length", Integer.toString(d.getLength())));
            de.addAttribute(sAttr("isUnlimited", Boolean.toString(d.isUnlimited())));
            e.addElement(de);
        }
        return e;
    }

    /** Panoply-like per-variable listing with a single multi-line text dump. */
    private static MetadataElement buildGeophysicalData(NetcdfFile nc) {
        MetadataElement geo = new MetadataElement("Geophysical_Data");

        for (Variable v : nc.getVariables()) {
            // Skip coordinate/aux variables; keep geophysical fields
            if (v.isCoordinateVariable() || v.getRank() == 0) continue;

            MetadataElement ve = new MetadataElement(v.getShortName());

            // One multi-line attribute holding the formatted dump
            String dump = formatVarDump(v);
            ve.addAttribute(new MetadataAttribute(
                    "dump", ProductData.createInstance(dump), true));

            geo.addElement(ve);
        }
        return geo;
    }

    /** Use NetcdfDatasets.openDataset(...) for CS info to avoid Enhance enum coupling. */
    private static MetadataElement buildCoordinateSystems(String fileUrlOrPath) throws IOException {
        try (NetcdfDataset ds = NetcdfDatasets.openDataset(fileUrlOrPath)) {
            MetadataElement e = new MetadataElement("Coordinate_Systems");
            for (ucar.nc2.dataset.CoordinateSystem cs : ds.getCoordinateSystems()) {
                String name = cs.getName() != null ? cs.getName() : "coord_sys";
                MetadataElement cse = new MetadataElement(name);

                cse.addAttribute(sAttr("isLatLon", Boolean.toString(cs.isLatLon())));
                cse.addAttribute(sAttr("domainRank", Integer.toString(cs.getDomain().size())));

                // Axes
                MetadataElement axes = new MetadataElement("Axes");
                for (CoordinateAxis ax : cs.getCoordinateAxes()) {
                    MetadataElement axe = new MetadataElement(ax.getShortName());
                    axe.addAttribute(sAttr("axisType", String.valueOf(ax.getAxisType())));
                    axe.addAttribute(sAttr("dataType", ax.getDataType().toString()));
                    axe.addAttribute(sAttr("units", nvl(ax.getUnitsString())));
                    axe.addAttribute(sAttr("shape", shapeString(ax.getShape())));
                    addIfPresent(axe, CF.STANDARD_NAME, ax.findAttributeIgnoreCase(CF.STANDARD_NAME));
                    addIfPresent(axe, CF.LONG_NAME,     ax.findAttributeIgnoreCase(CF.LONG_NAME));
                    axes.addElement(axe);
                }
                cse.addElement(axes);

                // Variables mapped to this CS (optional)
                MetadataElement mapped = new MetadataElement("Variables_Mapped");
                for (Variable v : ds.getVariables()) {
                    if (cs.isCoordinateSystemFor(v)) {
                        mapped.addElement(new MetadataElement(v.getShortName()));
                    }
                }
                cse.addElement(mapped);

                e.addElement(cse);
            }
            return e;
        }
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

    // -------------------- Panoply-like formatter --------------------
    private static String formatVarDump(Variable v) {
        StringBuilder out = new StringBuilder();

        // Header: "float chlor_a(number_of_lines=1710, pixels_per_line=1272);"
        out.append(v.getDataType().toString().toLowerCase())
                .append(' ')
                .append(v.getShortName())
                .append('(')
                .append(dimDecl(v.getDimensions()))
                .append(");\n");

        // Attributes in Panoply-like order with leading ":" and trailing ";"
        dumpAttr(out, "long_name",     v.findAttributeIgnoreCase("long_name"));
        dumpAttr(out, "units",         v.findAttributeIgnoreCase("units"));
        dumpAttr(out, "coordinates",   v.findAttributeIgnoreCase("coordinates"));
        dumpAttr(out, "standard_name", v.findAttributeIgnoreCase("standard_name"));

        dumpAttr(out, "_FillValue",    v.findAttribute("_FillValue"));
        dumpAttr(out, "valid_min",     v.findAttribute("valid_min"));
        dumpAttr(out, "valid_max",     v.findAttribute("valid_max"));
        dumpAttr(out, "valid_range",   v.findAttribute("valid_range")); // shown when present

        dumpAttr(out, "scale_factor",  v.findAttribute("scale_factor"));
        dumpAttr(out, "add_offset",    v.findAttribute("add_offset"));

        // Prefer singular "reference", fall back to "references"
        Attribute ref = v.findAttributeIgnoreCase("reference");
        if (ref == null) ref = v.findAttributeIgnoreCase("references");
        dumpAttr(out, "reference", ref);

        // Optional: _ChunkSizes = 256U, 1272U; // uint
        maybeAppendChunkSizes(out, v);

        return out.toString();
    }

    private static void dumpAttr(StringBuilder out, String name, Attribute a) {
        if (a == null) return;

        out.append("  :").append(name).append(" = ")
                .append(panoplyValue(a))
                .append(';');

        // Add trailing type comment for numeric values
        if (!a.isString()) {
            out.append(" // ").append(typeLabel(a.getDataType()));
        }
        out.append('\n');
    }

    /** Render an attribute value like Panoply: quote strings, add numeric suffixes (f/U), keep lists comma-separated. */
    private static String panoplyValue(Attribute a) {
        if (a.isString()) {
            if (a.getLength() <= 1) return quote(a.getStringValue());
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < a.getLength(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(quote(a.getStringValue(i)));
            }
            return sb.toString();
        }

        // Numeric
        String suff = numericSuffix(a.getDataType()); // "f" for float, "U" for uint*, "" otherwise
        if (a.getLength() == 1) {
            Object nv = a.getNumericValue();
            return nv != null ? nv.toString() + suff : "";
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < a.getLength(); i++) {
                if (i > 0) sb.append(", ");
                Object nv = a.getNumericValue(i);
                if (nv != null) sb.append(nv.toString()).append(suff);
            }
            return sb.toString();
        }
    }

    private static String numericSuffix(ucar.ma2.DataType dt) {
        if (dt == null) return "";
        switch (dt) {
            case FLOAT:  return "f";
            case UBYTE:
            case USHORT:
            case UINT:
            case ULONG:  return "U";
            default:     return ""; // Panoply typically omits 'd' for doubles, no suffix for signed ints
        }
    }

    private static String typeLabel(ucar.ma2.DataType dt) {
        if (dt == null) return "";
        switch (dt) {
            case UBYTE:  return "ubyte";
            case USHORT: return "ushort";
            case UINT:   return "uint";
            case ULONG:  return "ulong";
            default:     return dt.toString().toLowerCase();
        }
    }


    /** Try to emit `_ChunkSizes = 256U, 1272U; // uint` if available in your NetCDF version; otherwise skip silently. */
    private static void maybeAppendChunkSizes(StringBuilder out, Variable v) {
        try {
            int[] sizes = null;

            // NetCDF-Java 5.5+: Variable#getChunkSizes()
            try {
                java.lang.reflect.Method m = v.getClass().getMethod("getChunkSizes");
                Object res = m.invoke(v);
                if (res instanceof int[]) sizes = (int[]) res;
            } catch (NoSuchMethodException ignore) {
                // Older: Variable#getChunking() -> Chunking#getChunkSizes()
                try {
                    java.lang.reflect.Method gm = v.getClass().getMethod("getChunking");
                    Object chunk = gm.invoke(v);
                    if (chunk != null) {
                        java.lang.reflect.Method gs = chunk.getClass().getMethod("getChunkSizes");
                        Object res = gs.invoke(chunk);
                        if (res instanceof int[]) sizes = (int[]) res;
                    }
                } catch (NoSuchMethodException ignoreToo) {}
            }

            if (sizes != null && sizes.length > 0) {
                out.append("  :_ChunkSizes = ");
                for (int i = 0; i < sizes.length; i++) {
                    if (i > 0) out.append(", ");
                    out.append(sizes[i]).append('U');
                }
                out.append("; // uint\n");
            }
        } catch (Throwable ignored) {
            // If not supported by your NetCDF build, just omit the line.
        }
    }


    private static String dimDecl(List<Dimension> dims) {
        // Panoply-like: dimName=len, dimName=len
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < dims.size(); i++) {
            if (i > 0) sb.append(", ");
            Dimension d = dims.get(i);
            sb.append(d.getShortName()).append('=').append(d.getLength());
        }
        return sb.toString();
    }

    private static String joinInts(int[] arr) {
        if (arr == null || arr.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(arr[i]);
        }
        return sb.toString();
    }

    /** Display value like Panoply: quote strings; keep numbers; preserve arrays. */
    private static String attrDisplayedValue(Attribute a) {
        if (a == null) return "";

        if (a.isString()) {
            if (a.getLength() <= 1) {
                return quote(a.getStringValue());
            } else {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < a.getLength(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(quote(a.getStringValue(i)));
                }
                return sb.toString();
            }
        }

        if (a.getLength() == 1) {
            Object nv = a.getNumericValue();
            return nv != null ? nv.toString() + typeHint(a) : "";
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < a.getLength(); i++) {
                if (i > 0) sb.append(", ");
                Object nv = a.getNumericValue(i);
                sb.append(nv != null ? nv.toString() : "");
            }
            return sb.toString();
        }
    }

    private static String quote(String s) {
        return s == null ? "\"\"" : "\"" + s.replace("\"", "\\\"") + "\"";
    }

    private static String typeHint(Attribute a) {
        try {
            var t = a.getDataType();
            if (t != null) {
                String k = t.toString().toLowerCase();
                if ("char".equals(k)) return " // char";
                return " // " + k;
            }
        } catch (Throwable ignored) {}
        return "";
    }

    // -------------------- Common helpers --------------------

    private static void addIfPresent(MetadataElement e, String name, Attribute a) {
        if (a != null) {
            e.addAttribute(new MetadataAttribute(name,
                    ProductData.createInstance(attrString(a)), true));
        }
    }

    private static MetadataAttribute toMetaAttr(Attribute a) {
        return new MetadataAttribute(a.getShortName(),
                ProductData.createInstance(attrString(a)), true);
    }

    /** Generic stringify for attributes used outside the pretty dump. */
    private static String attrString(Attribute a) {
        if (a == null) return "";
        if (a.isString()) {
            if (a.getLength() <= 1) {
                return a.getStringValue();
            } else {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < a.getLength(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(a.getStringValue(i));
                }
                return sb.toString();
            }
        }
        if (a.getLength() == 1) {
            Object nv = a.getNumericValue();
            return nv != null ? nv.toString() : "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < a.getLength(); i++) {
            if (i > 0) sb.append(", ");
            Object nv = a.getNumericValue(i);
            sb.append(nv != null ? nv.toString() : "");
        }
        return sb.toString();
    }

    private static String shapeString(int[] shape) {
        if (shape == null || shape.length == 0) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < shape.length; i++) {
            if (i > 0) sb.append(" x ");
            sb.append(shape[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    private static String nvl(String s) { return s == null ? "" : s; }

    private static MetadataAttribute sAttr(String name, String value) {
        return new MetadataAttribute(name, ProductData.createInstance(nvl(value)), true);
    }
}
