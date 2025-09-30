package gov.nasa.gsfc.seadas.panoply;

import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import ucar.nc2.*;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

/**
 * Builds a groups-only metadata dump under a single wrapper node "Metadata_Dump".
 * - Recursively mirrors NetCDF root.getGroups() (and sub-groups)
 * - Each group node gets ncdump-like lines (variable declarations + group attrs)
 * - Each variable gets its own child node with declaration + its attributes
 * - All generated nodes are tagged (MARKER_ATTR) so we can safely refresh
 * - "Global attributes" / "gattributes" are intentionally ignored per requirement
 */
public final class PanoplyStyleMetadataBuilder {

    private PanoplyStyleMetadataBuilder() {}

    // ---- Public entry point -------------------------------------------------

    /** Wrapper node under SNAP "Metadata" where we place our dump. */
    private static final String DUMP_ROOT_NAME = "Metadata_Dump";

    /** Marker to identify nodes we generated (so we can refresh them safely). */
    private static final String MARKER_ATTR = "_panoply_section";
    private static final String MARKER_VAL  = "v2";
    private static String CURRENT_FILE_BASENAME = null;

    /** Toggle extra stdout logging if you need it. */
    private static final boolean DEBUG = false;

    /**
     * Build (or rebuild) the groups-only dump under "Metadata_Dump".
     * Does nothing if a usable NetCDF source cannot be opened.
     */
    public static void addAllGroupsUnderDumpRoot(Product product, String fileUrlOrPath) {
        if (product == null || product.getMetadataRoot() == null) return;

        try (NetcdfFile nc = openNetcdfIfPossible(product, fileUrlOrPath)) {
            if (nc == null) return;

            try {
                String loc = nc.getLocation();
                if (loc != null) {
                    java.io.File f = new java.io.File(loc);
                    CURRENT_FILE_BASENAME = f.getName();
                } else {
                    CURRENT_FILE_BASENAME = null;
                }
            } catch (Throwable ignore) { CURRENT_FILE_BASENAME = null; }

            // Optional probe
            if (DEBUG) debugProbe(nc, product, fileUrlOrPath);

            MetadataElement metaRoot = product.getMetadataRoot();
            MetadataElement dumpRoot = getOrCreateDumpRoot(metaRoot);

            // Remove only nodes we previously generated (by marker)
            removeGeneratedChildren(dumpRoot);

            // Build every group under the NetCDF root (ignore "gattributes" etc.)
            for (Group child : nc.getRootGroup().getGroups()) {
                MetadataElement section = buildGroupTree(child);
                if (isPopulated(section)) dumpRoot.addElement(section);
            }
            addNcdumpNode(dumpRoot, product, "No metadata found.") ;

            if (DEBUG) debugDumpRootChildren(dumpRoot);
        } catch (Throwable t) {
            System.out.println("[Panoply] groups-only build failed: " + t.getClass().getSimpleName() + ": " + t.getMessage());
        }
    }

    // ---- NetCDF open helpers ------------------------------------------------

    /** Try to obtain a usable NetCDF handle for the given product / path. */
    private static NetcdfFile openNetcdfIfPossible(Product product, String fileUrlOrPath) {
        // 1) Caller-supplied path
        NetcdfFile nc = tryOpenNetcdf(fileUrlOrPath);
        if (nc != null) return nc;

        // 2) Product file location
        if (product != null) {
            File loc = product.getFileLocation();
            if (loc != null) {
                nc = tryOpenNetcdf(loc.getAbsolutePath());
                if (nc != null) return nc;
            }

            // 3) Reader input
            try {
                if (product.getProductReader() != null) {
                    Object in = product.getProductReader().getInput();
                    if (in instanceof File) {
                        nc = tryOpenNetcdf(((File) in).getAbsolutePath());
                        if (nc != null) return nc;
                    } else if (in != null) {
                        nc = tryOpenNetcdf(String.valueOf(in));
                        if (nc != null) return nc;
                    }
                }
            } catch (Throwable ignore) {}
        }

        return null;
    }

    /** Try to open a NetCDF file/URL, skipping known non-NetCDF sidecars. */
    private static NetcdfFile tryOpenNetcdf(String pathOrUrl) {
        if (pathOrUrl == null || pathOrUrl.isEmpty()) return null;
        String low = pathOrUrl.toLowerCase();
        if (low.endsWith(".dim") || low.endsWith(".dimap") || low.endsWith(".xml")) return null;
        try {
            return NetcdfFiles.open(pathOrUrl);
        } catch (Throwable t1) {
            try {
                return NetcdfFile.open(pathOrUrl);
            } catch (Throwable t2) {
                return null;
            }
        }
    }

    private static MetadataElement buildGroupTree(ucar.nc2.Group g) {
        MetadataElement section = new MetadataElement(safeTitle(g.getShortName()));
        tag(section);

        java.util.List<String> lines = new java.util.ArrayList<>();

// Header (ncdump-like)
        lines.add("group: " + g.getShortName() + " {");
        if (CURRENT_FILE_BASENAME != null) {
            lines.add("  // In file \"" + CURRENT_FILE_BASENAME + "\"");
        }


        // VARIABLES (only if present)
        if (!g.getVariables().isEmpty()) {
            lines.add("variables:");
            for (ucar.nc2.Variable v : g.getVariables()) {
                lines.add("  " + varDeclLineWithSizes(v));
                for (ucar.nc2.Attribute a : v.getAttributes()) {
                    lines.add("    " + varAttrLineTyped(a));
                }
                lines.add(""); // blank line between variables
            }
            // spacer between variables and sub-groups (if any)
            if (!g.getGroups().isEmpty() || !g.getAttributes().isEmpty()) {
                lines.add("");
            }
        }

        // SUB-GROUP SUMMARIES (inline, before parent group attributes)
        // Format:
        // group: <name> {
        //   // group attributes:
        //   :attr = ... ;
        // }
        for (ucar.nc2.Group sub : g.getGroups()) {
            lines.add("group: " + sub.getShortName() + " {");
            // if the subgroup has attributes, show them
            if (!sub.getAttributes().isEmpty()) {
                lines.add("  // group attributes:");
                for (ucar.nc2.Attribute a : sub.getAttributes()) {
                    lines.add("  " + varAttrLineTyped(a));
                }
            } else {
                lines.add("  // (no group attributes)");
            }
            lines.add("}");
            lines.add(""); // blank line after each subgroup summary
        }

        // PARENT GROUP ATTRIBUTES (as comment header, per your expected output)
        if (!g.getAttributes().isEmpty()) {
            lines.add("// group attributes:");
            for (ucar.nc2.Attribute a : g.getAttributes()) {
                lines.add(varAttrLineTyped(a));
            }
        }

        // If nothing at all was added (no vars, no subgroups, no attrs), leave a placeholder
        if (g.getVariables().isEmpty() && g.getGroups().isEmpty() && g.getAttributes().isEmpty()) {
            lines.add("// (empty group)");
        }

        lines.add("}");
        addLines(section, lines);

        // Still add real child nodes so clicking sub-groups shows their full content
        for (ucar.nc2.Group sub : g.getGroups()) {
            MetadataElement child = buildGroupTree(sub);
            if (isPopulated(child)) section.addElement(child);
        }
        return section;
    }



    private static String varDeclLineWithSizes(ucar.nc2.Variable v) {
        StringBuilder sb = new StringBuilder();
        sb.append(v.getDataType().toString().toLowerCase()).append(' ')
                .append(v.getShortName()).append('(');

        java.util.List<ucar.nc2.Dimension> dims = v.getDimensions();
        for (int i = 0; i < dims.size(); i++) {
            if (i > 0) sb.append(", ");
            ucar.nc2.Dimension d = dims.get(i);
            sb.append(d.getShortName());
            long len = d.getLength();
            if (len >= 0) {
                sb.append('=').append(len);
            }
        }
        sb.append(") ;");
        return sb.toString();
    }

    private static String varAttrLineTyped(ucar.nc2.Attribute a) {
        StringBuilder sb = new StringBuilder();
        boolean isString = a.isString();

        // name and equals
        sb.append(':').append(a.getShortName()).append(" = ");

        if (isString) {
            // single string
            sb.append('"').append(a.getStringValue()).append('"');
        } else {
            // numeric (scalar or array)
            int n = a.getLength();
            if (n <= 0) {
                sb.append("\"\"");
            } else if (n == 1) {
                sb.append(formatNumericValue(a, 0));
            } else {
                for (int i = 0; i < n; i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(formatNumericValue(a, i));
                }
            }
        }

        sb.append(" ;");

        // trailing type comment like: // float
        String typeLabel = dataTypeLabel(a);
        if (typeLabel != null) {
            sb.append(" // ").append(typeLabel);
        }
        return sb.toString();
    }

    private static String formatNumericValue(ucar.nc2.Attribute a, int idx) {
        ucar.ma2.DataType t = a.getDataType();
        Number num = a.getNumericValue(idx);
        if (num == null) return "NaN";

        switch (t) {
            case FLOAT: {
                float f = num.floatValue();
                String s = java.lang.Float.toString(f);
                if (!s.contains(".")) s = s + ".0";
                return s + "f";
            }
            case DOUBLE: {
                // keep reasonably compact, no suffix
                double d = num.doubleValue();
                String s = java.lang.Double.toString(d);
                if (s.indexOf('E') < 0 && s.indexOf('e') < 0 && s.indexOf('.') < 0) s += ".0";
                return s;
            }
            case UBYTE:
            case USHORT:
            case UINT: {
                // append U to indicate unsigned like ncdump often does for chunk sizes etc.
                long v = num.longValue();
                return Long.toString(v) + "U";
            }
            case BYTE:
            case SHORT:
            case INT:
            case LONG:
                return String.valueOf(num.longValue());
            default:
                return num.toString();
        }
    }

    private static String dataTypeLabel(ucar.nc2.Attribute a) {
        ucar.ma2.DataType t = a.getDataType();
        if (t == null) return null;
        // Map to the human labels you expect to see
        switch (t) {
            case FLOAT:  return "float";
            case DOUBLE: return "double";
            case BYTE:   return "byte";
            case UBYTE:  return "uint8";
            case SHORT:  return "short";
            case USHORT: return "uint16";
            case INT:    return "int";
            case UINT:   return "uint";
            case LONG:   return "long";
            default:     return t.toString().toLowerCase();
        }
    }

    /** Build a variable node containing its declaration + attributes as lines. */
    private static MetadataElement buildVariableElement(Variable v) {
        MetadataElement varElem = new MetadataElement(v.getShortName());
        tag(varElem);

        List<String> lines = new ArrayList<>();
        lines.add(varDeclLine(v));
        for (Attribute a : v.getAttributes()) {
            lines.add("    " + attrLine(a)); // 4-space indent like ncdump
        }
        addLines(varElem, lines);
        return varElem;
    }

    // ---- Formatting & persistence helpers ----------------------------------

    /** Add a generator marker so we can replace these safely on refresh. */
    private static void tag(MetadataElement e) {
        e.addAttribute(new MetadataAttribute(MARKER_ATTR, ProductData.createInstance(MARKER_VAL), true));
    }

    /** True if element has any attributes (lines) or child elements. */
    private static boolean isPopulated(MetadataElement e) {
        return e != null && (e.getNumAttributes() > 0 || e.getNumElements() > 0);
    }

    /** Persist ncdump lines as line_00000, line_00001, ... (values hold the text). */
    private static void addLines(MetadataElement elem, List<String> lines) {
        for (int i = 0; i < lines.size(); i++) {
            String key = "line_" + String.format("%05d", i);
            elem.addAttribute(new MetadataAttribute(key, ProductData.createInstance(lines.get(i)), true));
        }
    }

    /** e.g., "float Rrs_443(time, y, x) ;" */
    private static String varDeclLine(Variable v) {
        StringBuilder sb = new StringBuilder();
        sb.append(v.getDataType().toString().toLowerCase()).append(' ')
                .append(v.getShortName()).append('(');
        for (int i = 0; i < v.getDimensions().size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(v.getDimensions().get(i).getShortName());
        }
        sb.append(") ;");
        return sb.toString();
    }

    /** ncdump-like attribute line (numbers raw, strings quoted). */
    private static String attrLine(Attribute a) {
        StringBuilder sb = new StringBuilder();
        sb.append(a.getShortName()).append(" = ");
        if (a.isString()) {
            sb.append('"').append(a.getStringValue()).append('"');
        } else if (a.getLength() > 1) {
            for (int i = 0; i < a.getLength(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(a.getNumericValue(i));
            }
        } else if (a.getLength() == 1) {
            sb.append(a.getNumericValue());
        } else {
            sb.append("\"\"");
        }
        sb.append(" ;");
        return sb.toString();
    }

    /** TitleCase-ish & XML-safe group name for SNAP nodes. */
    private static String safeTitle(String raw) {
        if (raw == null || raw.isEmpty()) return "Group";
        String s = raw.replaceAll("[^A-Za-z0-9_]+", "_");
        if (Character.isDigit(s.charAt(0))) s = "_" + s;
        String[] parts = s.split("_");
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            if (p.isEmpty()) continue;
            out.append(Character.toUpperCase(p.charAt(0)));
            if (p.length() > 1) out.append(p.substring(1));
            if (i < parts.length - 1) out.append("_");
        }
        return out.toString();
    }

    // ---- SNAP Metadata_Dump root + refresh helpers -------------------------

    /** Get or create the "Metadata_Dump" wrapper under the product metadata root. */
    private static MetadataElement getOrCreateDumpRoot(MetadataElement metaRoot) {
        MetadataElement dump = metaRoot.getElement(DUMP_ROOT_NAME);
        if (dump == null) {
            dump = new MetadataElement(DUMP_ROOT_NAME);
            metaRoot.addElement(dump);
        }
        return dump;
    }

    /** Remove only children previously generated by this builder (identified by marker). */
    private static void removeGeneratedChildren(MetadataElement dumpRoot) {
        // Collect names first to avoid concurrent modification pitfalls
        Set<Integer> toRemove = new HashSet<>();
        for (int i = 0; i < dumpRoot.getNumElements(); i++) {
            MetadataElement child = dumpRoot.getElementAt(i);
            if (child == null) continue;
            MetadataAttribute tag = child.getAttribute(MARKER_ATTR);
            String v = null;
            if (tag != null) {
                try { v = tag.getData() != null ? tag.getData().getElemString() : null; } catch (Throwable ignore) {}
            }
            if (MARKER_VAL.equals(v)) {
                toRemove.add(i);
            }
        }
        // Remove from end to start
        List<Integer> idx = new ArrayList<>(toRemove);
        idx.sort((a, b) -> Integer.compare(b, a));
        for (int i : idx) {
            dumpRoot.removeElement(dumpRoot.getElementAt(i));
        }
    }

    // ---- Debug helpers ------------------------------------------------------

    private static void debugProbe(NetcdfFile nc, Product product, String fileUrlOrPath) {
        System.out.println("=== Panoply DEBUG PROBE ===");
        System.out.println("product.fileLocation = " + (product != null && product.getFileLocation() != null ? product.getFileLocation().getAbsolutePath() : "null"));
        System.out.println("explicit path arg     = " + fileUrlOrPath);
        System.out.println("nc.isNull?            = " + (nc == null));
        if (nc != null) {
            Group root = nc.getRootGroup();
            System.out.println("root name             = " + (root != null ? root.getShortName() : "null"));
            System.out.println("root groups (#)       = " + (root != null ? root.getGroups().size() : -1));
            System.out.println("root vars (#)         = " + (root != null ? root.getVariables().size() : -1));
            System.out.println("root attrs (#)        = " + (root != null ? root.getAttributes().size() : -1));
            if (root != null) {
                for (Group g : root.getGroups()) {
                    System.out.printf("   - group '%s': vars=%d attrs=%d subgroups=%d%n",
                            g.getShortName(), g.getVariables().size(), g.getAttributes().size(), g.getGroups().size());
                }
            }
        }
        System.out.println("===========================");
    }

    private static void debugDumpRootChildren(MetadataElement dumpRoot) {
        System.out.println("[Panoply] Metadata_Dump children:");
        for (int i = 0; i < dumpRoot.getNumElements(); i++) {
            MetadataElement c = dumpRoot.getElementAt(i);
            System.out.printf("  - %s (attrs=%d, elements=%d)%n",
                    c.getName(), c.getNumAttributes(), c.getNumElements());
        }
    }

    private static void addNcdumpNode(MetadataElement wrapper, Product product, String fallbackText) {
        final MetadataElement ncd = new MetadataElement("ncdump");

        String ncdText = null;
        java.io.File pf = product != null ? product.getFileLocation() : null;
        ncdText = runRealNcdump(pf);
        if (ncdText == null) ncdText = fallbackText;

//        if (ncdText != null && !ncdText.isEmpty()) {
//            String[] lines = ncdText.replace("\r\n","\n").replace('\r','\n').split("\n", -1);
//            final int width = 6;
//            for (int i = 0; i < lines.length; i++) {
//                String key = String.format("ncdump_line_%0" + width + "d", i + 1);
//                wrapper.addAttribute(makeAsciiAttr(key, lines[i]));
//            }
//        }
        String[] lines = (ncdText != null ? ncdText : "(ncdump not available)")
                .replace("\r\n","\n").replace('\r','\n').split("\n", -1);

        final int width = 6;
        for (int i = 0; i < lines.length; i++) {
            String key = String.format("line_%0" + width + "d", i + 1);
            ncd.addAttribute(lineAttr(key, lines[i]));
        }
        wrapper.addElement(ncd);
    }

    private static MetadataAttribute makeAsciiAttr(String key, String value) {
        MetadataAttribute a = new MetadataAttribute(key, ProductData.TYPE_ASCII);
        a.getData().setElemString(value != null ? value : "");
        return a;
    }

    private static MetadataAttribute lineAttr(String key, String value) {
        ProductData pd = ProductData.createInstance(value); // ASCII payload
        return new MetadataAttribute(key, pd, true);
    }
    private static String runRealNcdump(java.io.File file) {
        if (file == null || !file.exists()) return null;

        try (NetcdfFile nc = NetcdfFiles.open(file.getAbsolutePath())) {
            StringWriter sw = new StringWriter(1 << 20);
            PrintWriter pw = new PrintWriter(sw);

            // NCdumpW.print(NetcdfFile nc, Writer out,
            //               boolean showAll, boolean showCoords,
            //               boolean ncml, boolean strict,
            //               String varNames, CancelTask ct)
            // Header only: showAll=false
            boolean showAll = false, showCoords = false, ncml = false, strict = false;
            String varNames = null;
            ucar.nc2.util.CancelTask ct = null;

            NCdumpW.print(nc, pw, showAll, showCoords, ncml, strict, varNames, ct);
            pw.flush();
            return sw.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static String synthesizeHeader(java.io.File productFile) {
        if (productFile == null || !productFile.exists()) return null;
        try (ucar.nc2.NetcdfFile nc = ucar.nc2.NetcdfFiles.open(productFile.getAbsolutePath())) {
            StringBuilder sb = new StringBuilder(1<<20);
            sb.append("netcdf ").append(productFile.getAbsolutePath()).append(" {\n");

            sb.append("dimensions:\n");
            for (ucar.nc2.Dimension d : nc.getDimensions()) {
                sb.append("  ").append(d.getShortName())
                        .append(" = ").append(d.getLength());
                if (d.isUnlimited()) sb.append(" // (unlimited)");
                sb.append(";\n");
            }

            sb.append("\nvariables:\n");
            for (ucar.nc2.Variable v : nc.getVariables()) {
                sb.append("  ").append(v.getDataType().toString().toLowerCase()).append(" ")
                        .append(v.getShortName()).append("(")
                        .append(v.getDimensions().stream()
                                .map(ucar.nc2.Dimension::getShortName)
                                .collect(java.util.stream.Collectors.joining(", ")))
                        .append(") ;\n");
                for (ucar.nc2.Attribute a : v.attributes()) {
                    sb.append("    :").append(a.getShortName()).append(" = ")
                            .append(a.getStringValue()).append(" ;\n");
                }
                sb.append("\n");
            }

            sb.append("}\n");
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }


}
