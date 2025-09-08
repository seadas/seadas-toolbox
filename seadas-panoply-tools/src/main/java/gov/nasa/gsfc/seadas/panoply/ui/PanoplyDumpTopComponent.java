package gov.nasa.gsfc.seadas.panoply.ui;

import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.ui.product.ProductSceneView;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.nodes.Node;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.TopComponent;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;

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
    private static final String PAN  = "Panoply";
    private static final String ZWSP = "\u200B"; // suffix storing sort order

    // All top-level groups (case-insensitive)
    private static final java.util.Set<String> TOP_GROUPS = new HashSet<>(java.util.Arrays.asList(
            "geophysical_data",
            "navigation_data",
            "processing_control",
            "scan_line_attributes",
            "sensor_band_parameters",
            // tolerant with legacy title-casing:
            "Geophysical_Data", "Navigation_Data", "Processing_Control",
            "Scan_Line_Attributes", "Sensor_Band_Parameters"
    ));

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

    @Override public void componentOpened() {
        TopComponent.getRegistry().addPropertyChangeListener(this);
        nodeSel = org.openide.util.Utilities.actionsGlobalContext().lookupResult(Node.class);
        nodeSel.addLookupListener(this);
        updateFromActivatedNodes();
    }

    @Override public void componentClosed() {
        TopComponent.getRegistry().removePropertyChangeListener(this);
        if (nodeSel != null) nodeSel.removeLookupListener(this);
        nodeSel = null;
    }

    @Override public void propertyChange(PropertyChangeEvent evt) {
        String p = evt.getPropertyName();
        if (TopComponent.Registry.PROP_ACTIVATED_NODES.equals(p) ||
                TopComponent.Registry.PROP_ACTIVATED.equals(p)) {
            SwingUtilities.invokeLater(this::updateFromActivatedNodes);
        }
    }

    @Override public void resultChanged(LookupEvent ev) {
        SwingUtilities.invokeLater(this::updateFromActivatedNodes);
    }

    // ---------------- core selection handling ----------------

    private void updateFromActivatedNodes() {
        String dump = null;

        Node[] act = TopComponent.getRegistry().getActivatedNodes();
        if (act != null) {
            for (Node n : act) {
                // A) try a MetadataElement directly from the node chain
                MetadataElement me = firstMetadataElementInAncestors(n);
                if (me != null && (isPanoplyVariable(me) || isPanoplyTopGroup(me))) {
                    dump = buildDumpFrom(me, n);
                    if (notEmpty(dump)) break;
                }

                // B) derive variable name from path and resolve from current product
                String varName = deriveVarNameFromPath(n);
                if (notEmpty(varName)) {
                    MetadataElement resolved = resolvePanoplyVarFromCurrentProduct(varName);
                    if (resolved != null && (isPanoplyVariable(resolved) || isPanoplyTopGroup(resolved))) {
                        dump = buildDumpFrom(resolved, n);
                        if (notEmpty(dump)) break;
                    }
                }
            }
        }

        // C) fallback: global context
        if (!notEmpty(dump)) {
            MetadataElement me = org.openide.util.Utilities.actionsGlobalContext().lookup(MetadataElement.class);
            if (me != null && (isPanoplyVariable(me) || isPanoplyTopGroup(me))) {
                dump = buildDumpFrom(me, null);
            }
        }

        final String text = notEmpty(dump) ? dump : HINT;

        // auto-open only when we have real content
        if (!HINT.equals(text) && !isOpened()) {
            SwingUtilities.invokeLater(this::open);
        }

        textArea.setText(text);
        textArea.setCaretPosition(0);
    }

    // ---------------- predicates & lookups ----------------

    /** climb node→parents and return the first MetadataElement found in any lookup */
    private static MetadataElement firstMetadataElementInAncestors(Node node) {
        for (Node cur = node; cur != null; cur = cur.getParentNode()) {
            MetadataElement me = cur.getLookup().lookup(MetadataElement.class);
            if (me != null) return me;
        }
        return null;
    }

    /** true only for elements Panoply → <top group> → <var> */
    private static boolean isPanoplyVariable(MetadataElement el) {
        if (el == null) return false;
        MetadataElement p = el.getParentElement();
        if (p == null) return false;
        MetadataElement g = p.getParentElement();
        if (g == null) return false;
        return PAN.equalsIgnoreCase(g.getName()) && isTopGroupName(p.getName());
    }

    /** true for top-level groups directly under Panoply */
    private static boolean isPanoplyTopGroup(MetadataElement el) {
        if (el == null) return false;
        MetadataElement parent = el.getParentElement();
        return parent != null && PAN.equalsIgnoreCase(parent.getName()) && isTopGroupName(el.getName());
    }

    private static boolean isTopGroupName(String name) {
        if (name == null) return false;
        if (TOP_GROUPS.contains(name)) return true;
        for (String g : TOP_GROUPS) {
            if (g.equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    // Try to derive "<var>" from a node path under Product → Metadata → Panoply → <group> → <var>
    private static String deriveVarNameFromPath(Node node) {
        java.util.List<String> up = new java.util.ArrayList<>(16);
        for (Node cur = node; cur != null; cur = cur.getParentNode()) {
            String nm = safeNodeName(cur);
            if (nm != null && !nm.isEmpty()) up.add(nm);
        }
        if (up.isEmpty()) return null;
        java.util.Collections.reverse(up);

        // find "Panoply, <group>, <var>"
        int iPan = indexOf(up, PAN);
        if (iPan < 0 || iPan + 2 >= up.size()) return null;
        String group = up.get(iPan + 1);
        if (!isTopGroupName(group)) return null;
        // If there’s a third item, that’s the variable name
        if (iPan + 2 < up.size()) {
            String var = up.get(iPan + 2);
            return (var != null && !var.isEmpty()) ? var : null;
        }
        return null;
    }

    private static int indexOf(java.util.List<String> list, String needle) {
        for (int i = 0; i < list.size(); i++) {
            if (needle.equalsIgnoreCase(list.get(i))) return i;
        }
        return -1;
    }

    /** Resolve Panoply/<top-group>/<varName> from the selected product */
    private static MetadataElement resolvePanoplyVarFromCurrentProduct(String varName) {
        try {
            SnapApp app = SnapApp.getDefault();
            if (app == null) return null;
            ProductSceneView view = app.getSelectedProductSceneView();
            if (view == null || view.getProduct() == null) return null;

            MetadataElement root = view.getProduct().getMetadataRoot();
            if (root == null) return null;

            MetadataElement pan = root.getElement(PAN);
            if (pan == null) return null;

            // scan groups for the var
            for (MetadataElement group : pan.getElements()) {
                if (group != null && isTopGroupName(group.getName())) {
                    MetadataElement var = group.getElement(varName);
                    if (var != null) return var;
                }
            }
        } catch (Throwable ignore) {}
        return null;
    }

    // ---------------- dump builders ----------------

    /** Dispatch: build dump for a variable or for a group. */
    private String buildDumpFrom(MetadataElement el, Node fromNode) {
        if (isPanoplyVariable(el)) {
            return buildVarDump(el);
        }
        if (isPanoplyTopGroup(el)) {
            return buildGroupDump(el, fromNode);
        }
        return null;
    }

    /** variable element → plain ncdump lines (stored as attributes with ZWSP+order) */
    private static String buildVarDump(MetadataElement varElem) {
        java.util.List<MetadataAttribute> attrs = orderedAttrs(varElem);
        if (attrs.isEmpty()) return null;

        StringBuilder sb = new StringBuilder(Math.max(2048, attrs.size() * 64));
        for (MetadataAttribute a : attrs) {
            String line = stripOrder(a.getName());
            sb.append(line).append('\n');
        }
        return sb.toString();
    }

    /** group element → group header + children (variables or processing_control subgroups) */
    private String buildGroupDump(MetadataElement groupElem, Node fromNode) {
        final String groupName = groupElem.getName();
        final String fileLabel = currentFileName(fromNode);

        StringBuilder out = new StringBuilder(8192);
        out.append("Group \"").append(groupName).append("\"\n");
        out.append("In file \"").append(fileLabel).append("\"\n");
        out.append("Group full name: ").append(groupName).append('\n');

        MetadataElement[] kids = groupElem.getElements();
        if (kids == null || kids.length == 0) {
            out.append("// group is empty\n");
            return out.toString();
        }

        Arrays.sort(kids, Comparator.comparing(MetadataElement::getName, String.CASE_INSENSITIVE_ORDER));

        if ("processing_control".equalsIgnoreCase(groupName)) {
            // subgroups: flag_percentages, input_parameters, …
            for (int i = 0; i < kids.length; i++) {
                MetadataElement sub = kids[i];
                dumpSubgroup(sub, groupName + "/" + sub.getName(), fileLabel, out);
                if (i < kids.length - 1) out.append('\n');
            }
        } else {
            // other groups list variables
            out.append("variables:\n");
            for (int i = 0; i < kids.length; i++) {
                java.util.List<MetadataAttribute> lines = orderedAttrs(kids[i]);
                if (!lines.isEmpty()) {
                    for (MetadataAttribute a : lines) {
                        out.append("  ").append(stripOrder(a.getName())).append('\n');
                    }
                    if (i < kids.length - 1) out.append('\n');
                }
            }
        }
        return out.toString();
    }

    /** processing_control subgroup → header + attributes (lines) */
    private static void dumpSubgroup(MetadataElement sub,
                                     String fullPath,
                                     String fileLabel,
                                     StringBuilder out) {
        out.append("Group \"").append(sub.getName()).append("\"\n");
        out.append("In file \"").append(fileLabel).append("\"\n");
        out.append("Group full name: ").append(fullPath).append('\n');
        out.append("// group attributes:\n");

        java.util.List<MetadataAttribute> lines = orderedAttrs(sub);
        if (lines.isEmpty()) {
            out.append("// (none)\n");
            return;
        }
        for (MetadataAttribute a : lines) {
            out.append(stripOrder(a.getName())).append('\n');
        }
    }

    // ---------------- attribute helpers ----------------

    private static java.util.List<MetadataAttribute> orderedAttrs(MetadataElement el) {
        MetadataAttribute[] arr = el.getAttributes();
        java.util.List<MetadataAttribute> list = new ArrayList<>();
        if (arr != null) list.addAll(java.util.Arrays.asList(arr));
        list.sort(Comparator.comparingInt(a -> {
            String n = a.getName();
            int i = n.lastIndexOf(ZWSP);
            if (i < 0) return Integer.MAX_VALUE;
            try { return Integer.parseInt(n.substring(i + 1)); }
            catch (NumberFormatException ignore) { return Integer.MAX_VALUE; }
        }));
        return list;
    }

    private static String stripOrder(String s) {
        int i = s.lastIndexOf(ZWSP);
        return (i >= 0) ? s.substring(0, i) : s;
    }

    // ---------------- file label helpers ----------------

    private static String currentFileName(Node node) {
        // 1) Walk node→parents: try Product or File in lookup
        for (Node cur = node; cur != null; cur = cur.getParentNode()) {
            Product p = (cur.getLookup() != null) ? cur.getLookup().lookup(Product.class) : null;
            String fn = fileFromProduct(p);
            if (fn != null) return fn;

            java.io.File f = (cur.getLookup() != null) ? cur.getLookup().lookup(java.io.File.class) : null;
            if (f != null && f.getName() != null && !f.getName().isEmpty()) return f.getName();
        }

        // 2) Use the active scene view’s product
        try {
            SnapApp app = SnapApp.getDefault();
            if (app != null) {
                ProductSceneView v = app.getSelectedProductSceneView();
                if (v != null) {
                    String s = fileFromProduct(v.getProduct());
                    if (s != null) return s;
                }
            }
        } catch (Throwable ignore) {}

        return "<unknown>";
    }

    private static String fileFromProduct(Product p) {
        if (p == null) return null;
        try {
            java.io.File f = p.getFileLocation(); // SNAP 12
            if (f != null && f.getName() != null && !f.getName().isEmpty()) return f.getName();
        } catch (Throwable ignore) {}
        return p.getName(); // fallback to product name
    }

    // ---------------- utils ----------------

    private static boolean notEmpty(String s) {
        return s != null && !s.isEmpty();
    }

    private static String safeNodeName(Node n) {
        if (n == null) return null;
        String s = n.getName();
        if (s == null || s.isEmpty()) s = n.getDisplayName();
        if (s == null) return null;
        return s.replaceAll("<[^>]+>", "").trim();
    }
}
