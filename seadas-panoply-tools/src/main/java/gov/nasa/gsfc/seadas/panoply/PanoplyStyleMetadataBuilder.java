package gov.nasa.gsfc.seadas.panoply;

import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Builds the Panoply-style metadata subtree containing exactly:
 *  - Geophysical_Data
 *  - Navigation_Data
 *  - Processing_Control   (with Flag_Percentages, Input_Parameters)
 *  - Scan_Line_Attributes
 *  - Sensor_Band_Parameters
 *
 * Each variable is represented as a MetadataElement whose attributes are
 * the ncdump-style text lines, stored in name order using a \u200B<index> suffix.
 */
public final class PanoplyStyleMetadataBuilder {

    private static final String PAN = "Panoply";
    private static final String ZWSP = "\u200B"; // suffix to preserve dump-line order

    private PanoplyStyleMetadataBuilder() {}

    public static void attachPanoplyMetadata(Product product, String fileUrlOrPath) {
        final MetadataElement panoplyRoot = new MetadataElement(PAN);
        // Attach root immediately so it always appears
        product.getMetadataRoot().addElement(panoplyRoot);

        NetcdfFile nc = null;
        try {
            nc = NetcdfFiles.open(fileUrlOrPath);
            final Group root = nc.getRootGroup();

            // Exactly the five nodes requested (TitleCase to match your dump window logic)
            safeAdd(panoplyRoot, () -> buildGroupSection(root, "geophysical_data",      "Geophysical_Data"),     "Geophysical_Data");
            safeAdd(panoplyRoot, () -> buildGroupSection(root, "navigation_data",       "Navigation_Data"),      "Navigation_Data");
            safeAdd(panoplyRoot, () -> buildProcessingControl(root.getNetcdfFile()),                                               "Processing_Control");
            safeAdd(panoplyRoot, () -> buildGroupSection(root, "scan_line_attributes",  "Scan_Line_Attributes"), "Scan_Line_Attributes");
            safeAdd(panoplyRoot, () -> buildGroupSection(root, "sensor_band_parameters","Sensor_Band_Parameters"),"Sensor_Band_Parameters");

        } catch (Throwable t) {
            System.out.println("[Panoply] attach: open/build failed: " + t.getClass().getSimpleName() + ": " + t.getMessage());
        } finally {
            try { if (nc != null) nc.close(); } catch (Exception ignore) {}
        }
    }

    // ------------------ section builders ------------------

    /** Generic “dump the variables in this group” section. */
    private static MetadataElement buildGroupSection(Group root, String groupShortName, String sectionTitle) {
        MetadataElement section = new MetadataElement(sectionTitle);
        Group g = findGroupRecursive(root, groupShortName);
        if (g == null) {
            // Create the node anyway so users see the expected structure
            return section;
        }
        for (Variable v : g.getVariables()) {
            section.addElement(buildVarDump(v, g)); // ncdump text lines stored as attributes
        }
        return section;
    }

    // ---------------- Processing_Control ----------------

    private static MetadataElement buildProcessingControl(NetcdfFile nc) {
        MetadataElement pcElem = new MetadataElement("Processing_Control");
        Group root = nc.getRootGroup();
        Group pc = childGroup(root, "processing_control");
        if (pc != null) {
            // flag_percentages
            MetadataElement fpElem = new MetadataElement("Flag_Percentages");
            dumpGroupAttributesAsLines(pc, "flag_percentages", fpElem);
            pcElem.addElement(fpElem);

            // input_parameters
            MetadataElement ipElem = new MetadataElement("Input_Parameters");
            dumpGroupAttributesAsLines(pc, "input_parameters", ipElem);
            pcElem.addElement(ipElem);
        }
        return pcElem;
    }

    private static void dumpGroupAttributesAsLines(Group parentGroup, String childSimpleName, MetadataElement targetElem) {
        Group g = childGroup(parentGroup, childSimpleName);
        if (g == null) return;

        int order = 0;
        for (Attribute a : g.getAttributes()) {
            String line = formatAttributeLine(a, true); // group-level attribute with leading ':'
            addLine(targetElem, line, order++);
        }
    }

    private static Group childGroup(Group parent, String name) {
        if (parent == null || name == null) return null;
        for (Group g : parent.getGroups()) {
            if (name.equalsIgnoreCase(g.getShortName())) return g;
        }
        return null;
    }

    // ---------------- Formatting helpers ----------------

    /** Add a line as a sortable attribute (line in the name, empty payload). */
    private static void addLine(MetadataElement elem, String line, int order) {
        elem.addAttribute(new MetadataAttribute(line + ZWSP + order,
                ProductData.createInstance(""), true));
    }

    /** Format an attribute as ncdump-style text. When global==true, prefix with ':' immediately. */
    private static String formatAttributeLine(Attribute a, boolean globalOrGroupLevel) {
        StringBuilder sb = new StringBuilder(128);
        // leading spaces for variable attributes are added by caller as needed
        if (globalOrGroupLevel) sb.append(':');
        sb.append(a.getShortName()).append(" = ");

        if (a.isString()) {
            if (a.getLength() <= 1) {
                sb.append('"').append(a.getStringValue()).append('"');
            } else {
                for (int i = 0; i < a.getLength(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append('"').append(a.getStringValue(i)).append('"');
                }
            }
            sb.append(';');
            return sb.toString();
        }

        // numeric (or opaque) values
        for (int i = 0; i < a.getLength(); i++) {
            if (i > 0) sb.append(", ");
            String v = String.valueOf(a.getNumericValue(i));
            sb.append(applyTypeSuffix(v, a)); // e.g., add 'f' for float, 'S' for short
        }
        sb.append(';');

        String typeComment = numericTypeComment(a);
        if (typeComment != null) {
            sb.append(" // ").append(typeComment);
        }
        return sb.toString();
    }

    private static String applyTypeSuffix(String v, Attribute a) {
        String dt = (a.getDataType() != null) ? a.getDataType().toString().toLowerCase() : "";
        switch (dt) {
            case "float":  return ensureFloatForm(v) + "f";
            case "double": return ensureFloatForm(v); // no suffix
            case "short":  return v + "S";
            case "byte":   return v + "B";
            // int/long/uint/ulong: leave as-is (no easy signedness info here)
            default:       return v;
        }
    }

    private static String numericTypeComment(Attribute a) {
        String dt = (a.getDataType() != null) ? a.getDataType().toString().toLowerCase() : null;
        if (dt == null) return null;
        switch (dt) {
            case "float":
            case "double":
            case "short":
            case "byte":
            case "int":
            case "long":
                return dt;
            default:
                return null;
        }
    }

    private static String ensureFloatForm(String v) {
        return (v.indexOf('.') >= 0 || v.indexOf('e') >= 0 || v.indexOf('E') >= 0) ? v : (v + ".0");
    }

    private static String attrValuesString(Attribute a) {
        if (a.isString()) {
            if (a.getLength() <= 1) return "\"" + a.getStringValue() + "\"";
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < a.getLength(); i++) {
                if (i > 0) sb.append(", ");
                sb.append('"').append(a.getStringValue(i)).append('"');
            }
            return sb.toString();
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < a.getLength(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(String.valueOf(a.getNumericValue(i)));
        }
        return sb.toString();
    }

    private static String typeCommentSuffix(Attribute a) {
        String t = numericTypeComment(a);
        return (t != null) ? " // " + t : "";
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

    private static String nullSafe(String s) { return s == null ? "" : s; }

//    /** processing_control with flag_percentages and input_parameters sub-nodes. */
//    private static MetadataElement buildProcessingControl(Group root) {
//        MetadataElement pcElem = new MetadataElement("Processing_Control");
//        Group pc = findGroupRecursive(root, "processing_control");
//        if (pc == null) {
//            return pcElem; // empty node still shown
//        }
//
//        // Subgroup: flag_percentages
//        MetadataElement flagsElem = new MetadataElement("Flag_Percentages");
//        Group flags = findGroupRecursive(pc, "flag_percentages");
//        if (flags != null) {
//            // Many files put values as variables or as attributes – support both
//            for (Variable v : flags.getVariables()) {
//                flagsElem.addElement(buildVarDump(v, flags));
//            }
//            if (!flags.getAttributes().isEmpty()) {
//                // Synthesize a simple dump for attributes-only subgroup
//                MetadataElement attDump = new MetadataElement("__attributes__");
//                int k = 0;
//                for (Attribute a : flags.getAttributes()) {
//                    String line = attrDumpLine(a);
//                    attDump.addAttribute(new MetadataAttribute(line + ZWSP + (k++),
//                            ProductData.createInstance(""), true));
//                }
//                flagsElem.addElement(attDump);
//            }
//        }
//        pcElem.addElement(flagsElem);
//
//        // Subgroup: input_parameters
//        MetadataElement inputElem = new MetadataElement("Input_Parameters");
//        Group input = findGroupRecursive(pc, "input_parameters");
//        if (input != null) {
//            for (Variable v : input.getVariables()) {
//                inputElem.addElement(buildVarDump(v, input));
//            }
//            if (!input.getAttributes().isEmpty()) {
//                MetadataElement attDump = new MetadataElement("__attributes__");
//                int k = 0;
//                for (Attribute a : input.getAttributes()) {
//                    String line = attrDumpLine(a);
//                    attDump.addAttribute(new MetadataAttribute(line + ZWSP + (k++),
//                            ProductData.createInstance(""), true));
//                }
//                inputElem.addElement(attDump);
//            }
//        }
//        pcElem.addElement(inputElem);
//
//        return pcElem;
//    }

    // ------------------ dump helpers ------------------

    /** Create a child element named after the variable and store ncdump-like lines as ordered attributes. */
    private static MetadataElement buildVarDump(Variable v, Group contextGroup) {
        String varName = v.getShortName();
        MetadataElement varElem = new MetadataElement(varName);

        List<String> lines = new ArrayList<>(16);
        // Header: <type> <name>(dim=...);
        lines.add(varHeaderLine(v));

        // Attributes
        for (Attribute a : v.attributes()) {
            lines.add("  " + attrDumpLine(a));
        }

        // Optional: chunk info, compression, etc. (best-effort and version-safe)
        // (If unavailable in your NetCDF-Java version this will simply be skipped.)

        // Persist the lines as attributes with \u200B<order> suffix in the name
        for (int i = 0; i < lines.size(); i++) {
            String key = lines.get(i) + ZWSP + i;
            varElem.addAttribute(new MetadataAttribute(key, ProductData.createInstance(""), true));
        }
        return varElem;
    }

    private static String varHeaderLine(Variable v) {
        StringBuilder sb = new StringBuilder();
        sb.append(v.getDataType().toString().toLowerCase()).append(' ')
                .append(v.getShortName()).append('(');
        int[] shape = v.getShape();
        List<Dimension> dims = v.getDimensions();
        for (int i = 0; i < dims.size(); i++) {
            if (i > 0) sb.append(", ");
            Dimension d = dims.get(i);
            // name=size format to mirror your example
            sb.append(d.getShortName()).append('=').append(d.getLength());
        }
        sb.append(");");
        return sb.toString();
    }

    private static String attrDumpLine(Attribute a) {
        String name = a.getShortName();
        if ("_FillValue".equalsIgnoreCase(name) || "_FillValue".equals(name)) {
            // Keep the explicit example style with type comment, when possible
            String val = a.isString() ? quoted(a.getStringValue()) : valuesString(a);
            String type = a.getDataType() != null ? a.getDataType().toString().toLowerCase() : "";
            if (a.getDataType() != null && a.getDataType().isFloatingPoint()) {
                // add 'f' for floats where it makes sense
                if ("float".equalsIgnoreCase(a.getDataType().toString()) && !val.endsWith("f")) {
                    val = val + "f";
                }
            }
            return ":" + name + " = " + val + "; // " + (type.isEmpty() ? "value" : type);
        }
        String val = a.isString() ? quoted(a.getStringValue()) : valuesString(a);
        return ":" + name + " = " + val + ";";
    }

    private static String valuesString(Attribute a) {
        if (a.isString()) {
            return quoted(a.getStringValue());
        }
        if (a.getLength() == 1) {
            Object num = a.getNumericValue();
            return num == null ? "" : num.toString();
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < a.getLength(); i++) {
            if (i > 0) sb.append(", ");
            Object num = a.getNumericValue(i);
            sb.append(num == null ? "" : num.toString());
        }
        return sb.toString();
    }

    private static String quoted(String s) {
        if (s == null) return "\"\"";
        return "\"" + s.replace("\"", "\\\"") + "\"";
    }

    // ------------------ utils ------------------

    /** Find a (nested) group by its short name, case-insensitive. */
    private static Group findGroupRecursive(Group start, String shortName) {
        if (start == null || shortName == null) return null;
        if (shortName.equalsIgnoreCase(start.getShortName())) {
            return start;
        }
        for (Group g : start.getGroups()) {
            Group hit = findGroupRecursive(g, shortName);
            if (hit != null) return hit;
        }
        return null;
    }

    /** Add child from supplier, logging but not failing if it throws. */
    private static void safeAdd(MetadataElement parent, Callable<MetadataElement> supplier, String label) {
        try {
            MetadataElement child = supplier.call();
            if (child != null) parent.addElement(child);
        } catch (Throwable t) {
            System.out.println("[Panoply] " + label + " failed: " + t.getClass().getSimpleName() + ": " + t.getMessage());
        }
    }
}
