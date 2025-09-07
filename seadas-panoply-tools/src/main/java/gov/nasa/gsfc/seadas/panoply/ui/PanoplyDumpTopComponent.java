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
    private String buildGroupDump(MetadataElement groupElem, Node fromNode) {
        String groupCanonical = canonicalGroupName(groupElem.getName());
        String fileLabel      = currentFilePathOrName(fromNode, groupElem);

        StringBuilder out = new StringBuilder(8192);
        out.append("Group \"").append(groupCanonical).append("\"\n");
        out.append("In file \"").append(fileLabel).append("\"\n");
        out.append("Group full name: ").append(groupCanonical).append('\n');
        out.append("variables:\n");

        MetadataElement[] children = groupElem.getElements();
        if (children != null && children.length > 0) {
            Arrays.sort(children, Comparator.comparing(MetadataElement::getName, String.CASE_INSENSITIVE_ORDER));
            for (int i = 0; i < children.length; i++) {
                MetadataElement child = children[i];
                List<MetadataAttribute> lines = orderedAttrs(child);
                if (!lines.isEmpty()) {
                    for (MetadataAttribute a : lines) {
                        String line = stripOrder(a.getName());
                        if (!line.isEmpty()) {
                            out.append("  ").append(line).append('\n');
                        }
                    }
                    if (i < children.length - 1) out.append('\n');
                }
            }
        }
        return out.toString();
    }


    private static List<MetadataAttribute> orderedAttrs(MetadataElement element) {
        MetadataAttribute[] arr = element.getAttributes();
        if (arr == null || arr.length == 0) return Collections.emptyList();
        List<MetadataAttribute> list = new ArrayList<>(Arrays.asList(arr));
        list.sort(Comparator.comparingInt(a -> {
            String n = a.getName();
            int i = n.lastIndexOf(ZWSP);
            if (i < 0) return Integer.MAX_VALUE;
            try { return Integer.parseInt(n.substring(i + 1)); }
            catch (NumberFormatException ex) { return Integer.MAX_VALUE; }
        }));
        return list;
    }

    private static String stripOrder(String s) {
        int i = s.lastIndexOf(ZWSP);
        return i >= 0 ? s.substring(0, i) : s;
    }

    private static String canonicalGroupName(String raw) {
        if (raw == null) return "";
        // Prefer lowercase with underscores (matches your recent changes & Panoply examples)
        return raw.toLowerCase(Locale.ROOT);
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
