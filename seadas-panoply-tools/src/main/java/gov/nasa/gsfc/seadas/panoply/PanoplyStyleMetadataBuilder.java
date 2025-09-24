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
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

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

    private static final String PAN = "MetadataDump";
    private static final String ZWSP = "\u200B"; // suffix to preserve dump-line order

    private PanoplyStyleMetadataBuilder() {}

    public static void attachPanoplyMetadata(Product product, String fileUrlOrPath) {
        if (product == null) return;

        final MetadataElement metaRoot = product.getMetadataRoot();
        if (metaRoot == null) return;

        // Reuse if it already exists (e.g., loaded from DIMAP)
        MetadataElement panoplyRoot = metaRoot.getElement(PAN); // PAN == "panoply"
        if (panoplyRoot != null) {
            // already present → do nothing (avoid duplicate roots)
            return;
        }

        // Otherwise build a fresh tree and attach once
        panoplyRoot = new MetadataElement(PAN);

        // Open NetCDF safely (and tolerate null/invalid path)
        try (NetcdfFile nc = (fileUrlOrPath != null && !fileUrlOrPath.isEmpty())
                ? NetcdfFiles.open(fileUrlOrPath) : null) {

            final Group root = (nc != null) ? nc.getRootGroup() : null;

            // Exactly the five nodes requested (TitleCase to match dump window logic)
            safeAdd(panoplyRoot, () -> buildGroupSection(root, "geophysical_data",       "Geophysical_Data"),      "Geophysical_Data");
            safeAdd(panoplyRoot, () -> buildGroupSection(root, "navigation_data",        "Navigation_Data"),       "Navigation_Data");
            safeAdd(panoplyRoot, () -> buildProcessingControl((nc != null) ? nc : null),                               "Processing_Control");
            safeAdd(panoplyRoot, () -> buildGroupSection(root, "scan_line_attributes",   "Scan_Line_Attributes"),  "Scan_Line_Attributes");
            safeAdd(panoplyRoot, () -> buildGroupSection(root, "sensor_band_parameters", "Sensor_Band_Parameters"),"Sensor_Band_Parameters");

        } catch (Throwable t) {
            System.out.println("[Panoply] attach: open/build failed: " + t.getClass().getSimpleName() + ": " + t.getMessage());
            // Even if build fails, we still attach a single (empty) panoply root so the UI shows it once.
        }

        //clearMetadataRoot(metaRoot);
        // Attach one time only
        metaRoot.addElement(panoplyRoot);
        //hoistPanoplyChildrenToMetaRoot(panoplyRoot, metaRoot);
    }

    /** Remove ALL attributes and child elements from a MetadataElement (root-safe). */
    private static void clearMetadataRoot(MetadataElement elem) {
        // remove attributes (from end to start)
        for (int i = elem.getNumAttributes() - 1; i >= 0; i--) {
            MetadataAttribute a = elem.getAttributeAt(i);
            if (a != null) elem.removeAttribute(a);
        }
        // remove child elements (from end to start)
        for (int i = elem.getNumElements() - 1; i >= 0; i--) {
            MetadataElement child = elem.getElementAt(i);
            if (child != null) elem.removeElement(child);
        }
    }

    /**
     * Moves all child elements of the given panoply root node directly under the metadata root.
     * - Preserves the original child order.
     * - Removes any existing root-level elements with the same names to avoid duplicates.
     * - Removes the panoply wrapper itself at the end.
     *
     * @return true if a flatten/hoist happened, false if nothing to do or inputs invalid.
     */
    public static boolean hoistPanoplyChildrenToMetaRoot(MetadataElement panoplyRootNode, MetadataElement metaRoot) {
        if (panoplyRootNode == null || metaRoot == null) return false;
        if (panoplyRootNode == metaRoot) return false; // nothing to hoist

        // Snapshot children (order matters)
        List<MetadataElement> children = new ArrayList<>();
        for (int i = 0; i < panoplyRootNode.getNumElements(); i++) {
            MetadataElement kid = panoplyRootNode.getElementAt(i);
            if (kid != null) children.add(kid);
        }

        // If no children, just remove the empty wrapper (if it *is* under metaRoot)
        if (children.isEmpty()) {
            if (panoplyRootNode.getParentElement() == metaRoot) {
                metaRoot.removeElement(panoplyRootNode);
                return true;
            }
            return false;
        }

        // Avoid duplicates at the root: remove any root-level elements with the same names
        Set<String> names = children.stream().map(MetadataElement::getName).collect(Collectors.toSet());
        for (int i = metaRoot.getNumElements() - 1; i >= 0; i--) {
            MetadataElement existing = metaRoot.getElementAt(i);
            if (existing != null && names.contains(existing.getName())) {
                metaRoot.removeElement(existing);
            }
        }

        // Detach children from wrapper before reparenting
        for (MetadataElement kid : children) {
            panoplyRootNode.removeElement(kid);
        }

        // If the wrapper is already under metaRoot, remove it now
        if (panoplyRootNode.getParentElement() == metaRoot) {
            metaRoot.removeElement(panoplyRootNode);
        } else {
            // Otherwise just ensure we don't leave it orphaned in some other branch
            MetadataElement parent = panoplyRootNode.getParentElement();
            if (parent != null) parent.removeElement(panoplyRootNode);
        }

        // Reattach hoisted children at the root in original order
        for (MetadataElement kid : children) {
            metaRoot.addElement(kid);
        }

        return true;
    }

    public static void rebuildPanoplyMetadata(Product product, String fileUrlOrPath) {
        if (product == null || product.getMetadataRoot() == null) return;
        final MetadataElement metaRoot = product.getMetadataRoot();
        MetadataElement existing = metaRoot.getElement(PAN);
        if (existing != null) metaRoot.removeElement(existing);
        attachPanoplyMetadata(product, fileUrlOrPath);
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

    // Use XML-safe attribute names; store text in the value.
// Example names: line_0000, line_0001, ...
    private static void addLine(MetadataElement elem, String line, int order) {
        String key = "line_" + String.format("%05d", order);
        elem.addAttribute(new MetadataAttribute(key, ProductData.createInstance(line), true));
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


    // ------------------ dump helpers ------------------

    /** Create a child element named after the variable and store ncdump-like lines as ordered attributes. */
    private static MetadataElement buildVarDump(Variable v, Group contextGroup) {
        String varName = v.getShortName();
        MetadataElement varElem = new MetadataElement(varName);

        List<String> lines = new ArrayList<>(16);
        // Header: <type> <name>(dim=...);
        lines.add("  " + varHeaderLine(v));

        // Attributes
        for (Attribute a : v.attributes()) {
            lines.add("  " + attrDumpLine(a));
        }

        // Persist the lines as attributes with \u200B<order> suffix in the name
        for (int i = 0; i < lines.size(); i++) {
            addLine(varElem, lines.get(i), i);
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
