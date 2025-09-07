package gov.nasa.gsfc.seadas.panoply.ui;

import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.ui.product.ProductSceneView;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.nodes.Node;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.TopComponent;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.*;
import java.util.List;

@TopComponent.Description(preferredID = "PanoplyDumpTopComponent", persistenceType = TopComponent.PERSISTENCE_ALWAYS)
@TopComponent.Registration(mode = "output", openAtStartup = false)
@ActionID(category = "Window", id = "gov.nasa.gsfc.seadas.panoply.ui.PanoplyDumpTopComponent")
@ActionReference(path = "Menu/Window", position = 19300)
@TopComponent.OpenActionRegistration(displayName = "#CTL_PanoplyDumpAction", preferredID = "PanoplyDumpTopComponent")
@Messages({
        "CTL_PanoplyDumpAction=Panoply Dump",
        "CTL_PanoplyDumpTopComponent=Panoply Dump",
        "HINT_PanoplyDumpTopComponent=Shows Panoply-style (ncdump) text for the selected variable or group"
})
public final class PanoplyDumpTopComponent extends TopComponent implements LookupListener, PropertyChangeListener {

    private static final String HINT = "Select a variable or group under Metadata \u2192 Panoply …";

    // Panoply sections (accept both lower/camel cases, compare case-insensitively)
    private static final String PAN = "Panoply";
    private static final String GEO = "geophysical_data";
    private static final String NAV = "navigation_data";
    private static final String PROC = "processing_control";
    private static final String SLA = "scan_line_attributes";
    private static final String SBP = "sensor_band_parameters";

    // We stored the preformatted lines as attribute names, with a ZWSP+index suffix for stable ordering
    private static final String ZWSP = "\u200B";

    private final JTextArea textArea = new JTextArea();
    private org.openide.util.Lookup.Result<Node> nodeSel;

    public PanoplyDumpTopComponent() {
        setName(Bundle.CTL_PanoplyDumpTopComponent());
        setToolTipText(Bundle.HINT_PanoplyDumpTopComponent());
        textArea.setEditable(false);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        textArea.setLineWrap(false);
        setLayout(new BorderLayout());
        add(new JScrollPane(textArea), BorderLayout.CENTER);
        textArea.setText(HINT);
    }

    @Override
    public void componentOpened() {
        TopComponent.getRegistry().addPropertyChangeListener(this);
        nodeSel = org.openide.util.Utilities.actionsGlobalContext().lookupResult(Node.class);
        nodeSel.addLookupListener(this);
        updateFromActivatedNodes();
    }

    @Override
    public void componentClosed() {
        TopComponent.getRegistry().removePropertyChangeListener(this);
        if (nodeSel != null) nodeSel.removeLookupListener(this);
        nodeSel = null;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        String p = evt.getPropertyName();
        if (TopComponent.Registry.PROP_ACTIVATED_NODES.equals(p) ||
                TopComponent.Registry.PROP_ACTIVATED.equals(p)) {
            SwingUtilities.invokeLater(this::updateFromActivatedNodes);
        }
    }

    @Override
    public void resultChanged(LookupEvent ev) {
        SwingUtilities.invokeLater(this::updateFromActivatedNodes);
    }

    private void updateFromActivatedNodes() {
        String dump = null;

        Node[] act = TopComponent.getRegistry().getActivatedNodes();
        if (act != null) {
            for (Node n : act) {
                MetadataElement me = firstMetadataElementInAncestors(n);
                if (isUnderPanoply(me)) {
                    dump = buildDump(me);
                    if (dump != null && !dump.isEmpty()) break;
                }
            }
        }

        if (dump == null) {
            // last fallback: direct lookup
            MetadataElement me = org.openide.util.Utilities.actionsGlobalContext().lookup(MetadataElement.class);
            if (isUnderPanoply(me)) {
                dump = buildDump(me);
            }
        }

        textArea.setText((dump != null && !dump.isEmpty()) ? dump : HINT);
        textArea.setCaretPosition(0);
    }

    // ---------- selection helpers ----------

    private static MetadataElement firstMetadataElementInAncestors(Node node) {
        for (Node cur = node; cur != null; cur = cur.getParentNode()) {
            MetadataElement me = cur.getLookup().lookup(MetadataElement.class);
            if (me != null) return me;
        }
        return null;
    }

    /** true if element’s grandparent is "Panoply" */
    private static boolean isUnderPanoply(MetadataElement el) {
        if (el == null) return false;
        MetadataElement p = el.getParentElement();
        if (p == null) return false;
        MetadataElement g = p.getParentElement();
        return g != null && PAN.equalsIgnoreCase(g.getName());
    }

    /** parent section name in lower-case (e.g. geophysical_data, processing_control, …) */
    private static String parentSectionLower(MetadataElement el) {
        MetadataElement p = el != null ? el.getParentElement() : null;
        return p != null ? p.getName().toLowerCase(Locale.ROOT) : "";
    }

    /** full path under Panoply in lower-case, like "processing_control/flag_percentages" */
    private static String fullPanoplyPathLower(MetadataElement el) {
        if (el == null) return "";
        LinkedList<String> parts = new LinkedList<>();
        MetadataElement cur = el;
        while (cur != null) {
            parts.addFirst(cur.getName().toLowerCase(Locale.ROOT));
            cur = cur.getParentElement();
            if (cur != null && PAN.equalsIgnoreCase(cur.getName())) {
                // stop before adding "Panoply" itself
                break;
            }
        }
        return String.join("/", parts);
    }

    /** best-effort file path of the product for header lines */
    private static String filePathOf(MetadataElement el) {
        Product p = el != null ? el.getProduct() : null;
        if (p != null) {
            try {
                File f = p.getFileLocation();
                if (f != null) return f.getAbsolutePath();
            } catch (Throwable ignore) {}
        }
        // fallback: try selected view
        SnapApp app = SnapApp.getDefault();
        if (app != null) {
            ProductSceneView v = app.getSelectedProductSceneView();
            if (v != null && v.getProduct() != null) {
                try {
                    File f = v.getProduct().getFileLocation();
                    if (f != null) return f.getAbsolutePath();
                } catch (Throwable ignore) {}
            }
        }
        return "(unknown)";
    }

    // ---------- dump builders ----------

    /** Dispatch: variables under geophysical_data use preformatted lines; other sections dump as groups. */
    private static String buildDump(MetadataElement el) {
        if (el == null) return null;
        String section = parentSectionLower(el);

        if (GEO.equals(section)) {
            return buildVariableDump(el);
        }

        if (NAV.equals(section) || PROC.equals(section) || SLA.equals(section) || SBP.equals(section)) {
            return buildGroupDump(el);
        }

        // Anything else under Panoply: try group style as a fallback
        return buildGroupDump(el);
    }

    /** Variable dump: we stored each preformatted line as an attribute *name* + ZWSP + order. */
    private static String buildVariableDump(MetadataElement varElem) {
        String full = fullPanoplyPathLower(varElem);        // e.g. geophysical_data/chlor_a
        String name = varElem.getName();                    // e.g. chlor_a
        String file = filePathOf(varElem);

        StringBuilder sb = new StringBuilder(2048);
        sb.append("Variable \"").append(name).append("\"\n");
        sb.append("In file \"").append(file).append("\"\n");
        sb.append("Variable full name: ").append(full).append('\n');

        List<MetadataAttribute> attrs = ordered(varElem.getAttributes());
        for (MetadataAttribute a : attrs) {
            String line = stripOrderSuffix(a.getName());
            sb.append(line).append('\n');
        }
        return sb.toString();
    }

    /** Group dump: header + either preformatted lines, attributes, or child-element pairs. */
    private static String buildGroupDump(MetadataElement groupElem) {
        String full = fullPanoplyPathLower(groupElem);      // e.g. processing_control/flag_percentages
        String name = groupElem.getName();                  // e.g. flag_percentages
        String file = filePathOf(groupElem);

        StringBuilder sb = new StringBuilder(2048);
        sb.append("Group \"").append(name).append("\"\n");
        sb.append("In file \"").append(file).append("\"\n");
        sb.append("Group full name: ").append(full).append('\n');
        sb.append("// group attributes:\n");

        // 1) If we have preformatted attribute-lines (stored as attr names), use them.
        List<MetadataAttribute> attrs = ordered(groupElem.getAttributes());
        if (!attrs.isEmpty() && looksPreformatted(attrs)) {
            for (MetadataAttribute a : attrs) {
                String line = stripOrderSuffix(a.getName());
                sb.append(line).append('\n');
            }
            return sb.toString();
        }

        // 2) Otherwise, render real attributes if present…
        if (!attrs.isEmpty()) {
            for (MetadataAttribute a : attrs) {
                sb.append(renderPair(a.getName(), a.getData())).append('\n');
            }
            return sb.toString();
        }

        // 3) …and if the group itself has no attributes, derive pairs from child elements.
        //    This is the case for processing_control/flag_percentages, input_parameters, etc.
        String[] childNames = groupElem.getElementNames();
        if (childNames != null && childNames.length > 0) {
            // Keep stable order by name
            Arrays.sort(childNames, String.CASE_INSENSITIVE_ORDER);
            for (String childName : childNames) {
                MetadataElement child = groupElem.getElement(childName);
                for (String line : collectGroupEntryLines(childName, child)) {
                    sb.append(line).append('\n');
                }
            }
        }

        return sb.toString();
    }


    /** Build one or more ":name = value; // type" lines from a child element. */
    private static List<String> collectGroupEntryLines(String childName, MetadataElement child) {
        List<String> out = new ArrayList<>(1);
        if (child == null) return out;

        // If the child stores preformatted lines as attribute names, honor them first.
        List<MetadataAttribute> attrs = ordered(child.getAttributes());
        if (!attrs.isEmpty() && looksPreformatted(attrs)) {
            for (MetadataAttribute a : attrs) {
                out.add(stripOrderSuffix(a.getName()));
            }
            return out;
        }

        // If the child has a single meaningful attribute, render ":childName = value; // type".
        if (!attrs.isEmpty()) {
            // Heuristics: prefer an attribute literally named "value", otherwise:
            // - if exactly one attribute, use it
            // - else try one with same name as the child
            MetadataAttribute chosen = null;

            for (MetadataAttribute a : attrs) {
                if ("value".equalsIgnoreCase(a.getName())) {
                    chosen = a;
                    break;
                }
            }
            if (chosen == null) {
                if (attrs.size() == 1) {
                    chosen = attrs.get(0);
                } else {
                    for (MetadataAttribute a : attrs) {
                        if (a.getName().equalsIgnoreCase(childName)) {
                            chosen = a;
                            break;
                        }
                    }
                }
            }
            // If still not chosen and multiple attributes exist, render them all as separate lines
            if (chosen != null) {
                out.add(renderPair(childName, chosen.getData()));
            } else {
                for (MetadataAttribute a : attrs) {
                    out.add(renderPair(a.getName(), a.getData()));
                }
            }
            return out;
        }

        // No attributes at all: emit a placeholder to make the situation visible.
        out.add(":" + childName + " = ;");
        return out;
    }

    /** Render a single ncdump-style line from name + ProductData value. */
    private static String renderPair(String name, ProductData pd) {
        String val = renderValue(pd);
        String t   = renderType(pd);
        return ":" + name + " = " + val + ";" + (t.isEmpty() ? "" : " // " + t);
    }

    // ---------- rendering helpers ----------

    /** Sort attributes by integer suffix after ZWSP; items without suffix come last in original order. */
    private static List<MetadataAttribute> ordered(MetadataAttribute[] arr) {
        List<MetadataAttribute> list = new ArrayList<>();
        if (arr != null) Collections.addAll(list, arr);
        list.sort(Comparator.comparingInt(a -> {
            String n = a.getName();
            int i = n.lastIndexOf(ZWSP);
            if (i < 0) return Integer.MAX_VALUE;
            try { return Integer.parseInt(n.substring(i + 1)); }
            catch (NumberFormatException ex) { return Integer.MAX_VALUE; }
        }));
        return list;
    }

    private static String stripOrderSuffix(String name) {
        int i = name.lastIndexOf(ZWSP);
        return (i >= 0) ? name.substring(0, i) : name;
    }

    /** true if most attributes look like preformatted text lines (start with ':' or contain '=' etc.). */
    private static boolean looksPreformatted(List<MetadataAttribute> attrs) {
        if (attrs == null || attrs.isEmpty()) return false;
        int hits = 0;
        for (MetadataAttribute a : attrs) {
            String n = stripOrderSuffix(a.getName());
            if (n.startsWith(":") || n.contains("=") || n.endsWith(";")) hits++;
        }
        return hits >= Math.max(1, attrs.size() / 2);
    }

    private static String renderValue(ProductData pd) {
        if (pd == null) return "\"\"";
        try {
            int n = pd.getNumElems();
            if (n <= 1) {
                String s = pd.getElemString();
                return formatScalar(s, pd);
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < n; i++) {
                if (i > 0) sb.append(", ");
                sb.append(formatScalar(pd.getElemStringAt(i), pd));
            }
            return sb.toString();
        } catch (Throwable t) {
            // last resort
            return pd.toString();
        }
    }

    private static String formatScalar(String s, ProductData pd) {
        if (s == null) return "\"\"";
        // Heuristic: if data type is textual, quote it
        String type = renderType(pd).toLowerCase(Locale.ROOT);
        if (type.contains("string") || type.contains("char")) {
            return "\"" + s.replace("\"", "\\\"") + "\"";
        }
        return s;
    }

    private static String renderType(ProductData pd) {
        if (pd == null) return "";
        try {
            return ProductData.getTypeString(pd.getType());
        } catch (Throwable ignore) {
            return "";
        }
    }
}
