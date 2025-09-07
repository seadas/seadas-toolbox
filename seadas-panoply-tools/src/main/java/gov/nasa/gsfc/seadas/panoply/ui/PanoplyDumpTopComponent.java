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
import java.io.File;
import java.util.*;

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

    // All top-level groups we want to handle (case-insensitive)
    private static final Set<String> TOP_GROUPS = new HashSet<>(Arrays.asList(
            "geophysical_data",
            "navigation_data",
            "processing_control",
            "scan_line_attributes",
            "sensor_band_parameters",
            // be tolerant with legacy title-casing just in case:
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

    private void updateFromActivatedNodes() {
        String dump = null;

        Node[] act = TopComponent.getRegistry().getActivatedNodes();
        if (act != null) {
            for (Node n : act) {
                MetadataElement me = firstMetadataElementInAncestors(n);
                if (me != null) {
                    if (isPanoplyVariable(me)) {
                        dump = buildVarDump(me);
                        if (dump != null) break;
                    }
                    if (isPanoplyTopGroup(me)) {
                        dump = buildGroupDump(me, n);
                        if (dump != null) break;
                    }
                }
            }
        }

        // Fallback: direct context
        if (dump == null) {
            MetadataElement me = org.openide.util.Utilities.actionsGlobalContext()
                    .lookup(MetadataElement.class);
            if (isPanoplyVariable(me)) {
                dump = buildVarDump(me);
            } else if (isPanoplyTopGroup(me)) {
                dump = buildGroupDump(me, null);
            }
        }

        textArea.setText((dump != null && !dump.isEmpty()) ? dump : HINT);
        textArea.setCaretPosition(0);
    }

    /** climb node→parents and return the first MetadataElement found in any lookup */
    private static MetadataElement firstMetadataElementInAncestors(Node node) {
        for (Node cur = node; cur != null; cur = cur.getParentNode()) {
            MetadataElement me = cur.getLookup().lookup(MetadataElement.class);
            if (me != null) return me;
        }
        return null;
    }

    /** true only for elements Panoply → <any top group> → <var> */
    private static boolean isPanoplyVariable(MetadataElement el) {
        if (el == null) return false;
        MetadataElement parent = el.getParentElement();
        if (parent == null) return false;
        MetadataElement grand = parent.getParentElement();
        if (grand == null) return false;
        return PAN.equals(grand.getName()) && isTopGroupName(parent.getName());
    }

    /** true for any of the Panoply top-level groups directly under Panoply */
    private static boolean isPanoplyTopGroup(MetadataElement el) {
        if (el == null) return false;
        MetadataElement parent = el.getParentElement();
        return parent != null && PAN.equals(parent.getName()) && isTopGroupName(el.getName());
    }

    private static boolean isTopGroupName(String name) {
        if (name == null) return false;
        if (TOP_GROUPS.contains(name)) return true;
        // case-insensitive fallback
        for (String g : TOP_GROUPS) {
            if (g.equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    /** Build a dump for a variable element (lines were stored as attributes with ZWSP+order). */
    private static String buildVarDump(MetadataElement varElem) {
        List<MetadataAttribute> attrs = orderedAttrs(varElem);
        if (attrs.isEmpty()) return null;

        StringBuilder sb = new StringBuilder(Math.max(2048, attrs.size() * 64));
        for (MetadataAttribute a : attrs) {
            String line = stripOrder(a.getName());
            sb.append(line).append('\n');
        }
        return sb.toString();
    }
    // --- replace your buildGroupDump(...) with this one ---
    private String buildGroupDump(MetadataElement groupElem, Node fromNode) {
        final String groupName   = groupElem.getName();
        final String groupPath   = groupName; // our canonical path is same as name at this level
        final String fileLabel   = currentFilePathOrName(fromNode, groupElem);

        StringBuilder out = new StringBuilder(8192);
        out.append("Group \"").append(groupName).append("\"\n");
        out.append("In file \"").append(fileLabel).append("\"\n");
        out.append("Group full name: ").append(groupPath).append('\n');

        MetadataElement[] kids = groupElem.getElements();
        if (kids == null || kids.length == 0) {
            out.append("// group is empty\n");
            return out.toString();
        }

        Arrays.sort(kids, Comparator.comparing(MetadataElement::getName, String.CASE_INSENSITIVE_ORDER));

        // processing_control has subgroups; others list variables
        if ("processing_control".equalsIgnoreCase(groupName)) {
            for (int i = 0; i < kids.length; i++) {
                MetadataElement sub = kids[i];
                dumpSubgroup(sub, groupPath + "/" + sub.getName(), fileLabel, out);
                if (i < kids.length - 1) out.append('\n');
            }
        } else {
            out.append("variables:\n");
            for (int i = 0; i < kids.length; i++) {
                List<MetadataAttribute> lines = orderedAttrs(kids[i]);
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

    // --- add these two helpers (or replace existing variants) ---
    private static List<MetadataAttribute> orderedAttrs(MetadataElement el) {
        MetadataAttribute[] arr = el.getAttributes();
        List<MetadataAttribute> list = new ArrayList<>();
        if (arr != null) Collections.addAll(list, arr);
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

    // --- new: subgroup dumper used by processing_control ---
    private static void dumpSubgroup(MetadataElement sub,
                                     String fullPath,
                                     String fileLabel,
                                     StringBuilder out) {
        out.append("Group \"").append(sub.getName()).append("\"\n");
        out.append("In file \"").append(fileLabel).append("\"\n");
        out.append("Group full name: ").append(fullPath).append('\n');
        out.append("// group attributes:\n");

        List<MetadataAttribute> lines = orderedAttrs(sub);
        if (lines.isEmpty()) {
            out.append("// (none)\n");
            return;
        }
        for (MetadataAttribute a : lines) {
            out.append(stripOrder(a.getName())).append('\n');
        }
    }

    private static String currentFilePathOrName(Node node, MetadataElement contextElem) {
        // 1) Try the node and its ancestors
        for (Node cur = node; cur != null; cur = cur.getParentNode()) {
            // Try Product from lookup
            Product p = cur.getLookup().lookup(Product.class);
            String v = fileFromProduct(p);
            if (v != null) return v;

            // Try a java.io.File in lookup (some nodes expose it)
            java.io.File f = cur.getLookup().lookup(java.io.File.class);
            if (f != null) return safeFileName(f);
        }

        // 2) Try the active scene view
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

        // 3) Last resort: use the product name if we can reach a product
        try {
            SnapApp app = SnapApp.getDefault();
            if (app != null) {
                // If you have ProductManager API, you could iterate open products here
                // and, e.g., pick the first; but using the active product name is usually fine:
                ProductSceneView v = app.getSelectedProductSceneView();
                if (v != null && v.getProduct() != null) {
                    return v.getProduct().getName();
                }
            }
        } catch (Throwable ignore) {}

        return "<unknown>";
    }

    private static String fileFromProduct(Product p) {
        if (p == null) return null;
        try {
            java.io.File f = p.getFileLocation(); // SNAP 12 API
            if (f != null) return safeFileName(f);
        } catch (Throwable ignore) {}
        // fallback to product name if no file is associated
        return p.getName();
    }

    private static String safeFileName(java.io.File f) {
        // Show the file name (not full path) to match Panoply’s “In file” line style
        String name = (f != null) ? f.getName() : null;
        return (name != null && !name.isEmpty()) ? name : "<unknown>";
    }

}
