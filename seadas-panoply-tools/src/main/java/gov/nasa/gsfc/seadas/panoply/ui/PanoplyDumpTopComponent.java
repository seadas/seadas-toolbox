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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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
    private static final String PAN  = "Panoply";
    // In your build you renamed nodes to lowercase. Keep both for safety.
    private static final String GEO_L = "geophysical_data";
    private static final String GEO_U = "Geophysical_Data";
    private static final String ZWSP = "\u200B"; // suffix storing sort order

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
                    if (isPanoplyVar(me)) {
                        dump = buildVarDump(me);
                        if (dump != null) break;
                    }
                    if (isGeophysicalGroup(me)) {
                        dump = buildGroupDump(me, GEO_L /*canonical group name*/);
                        if (dump != null) break;
                    }
                }
            }
        }

        // Fallback on direct context
        if (dump == null) {
            MetadataElement me = org.openide.util.Utilities.actionsGlobalContext()
                    .lookup(MetadataElement.class);
            if (isPanoplyVar(me)) {
                dump = buildVarDump(me);
            } else if (isGeophysicalGroup(me)) {
                dump = buildGroupDump(me, GEO_L);
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

    /** true only for elements Panoply → (geophysical_data | Geophysical_Data) → <var> */
    private static boolean isPanoplyVar(MetadataElement el) {
        if (el == null) return false;
        MetadataElement p = el.getParentElement();
        if (p == null) return false;
        MetadataElement g = p.getParentElement();
        if (g == null) return false;
        boolean isGeo = GEO_L.equalsIgnoreCase(p.getName()) || GEO_U.equalsIgnoreCase(p.getName());
        return isGeo && PAN.equals(g.getName());
    }

    /** true for the geophysical_data group node under Panoply */
    private static boolean isGeophysicalGroup(MetadataElement el) {
        if (el == null) return false;
        MetadataElement g = el.getParentElement();
        return g != null
                && PAN.equals(g.getName())
                && (GEO_L.equalsIgnoreCase(el.getName()) || GEO_U.equalsIgnoreCase(el.getName()));
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

    /** Build a dump for the group (header + ‘variables:’ + each child variable’s dump indented). */
    private String buildGroupDump(MetadataElement groupElem, String canonicalGroupName) {
        // Determine file label
        String fileLabel = currentFilePathOrName();

        StringBuilder out = new StringBuilder(8192);
        out.append("Group \"").append(canonicalGroupName).append("\"\n");
        out.append("In file \"").append(fileLabel).append("\"\n");
        out.append("Group full name: ").append(canonicalGroupName).append('\n');
        out.append("variables:\n");

        // Iterate child elements = variables
        MetadataElement[] vars = groupElem.getElements();
        if (vars != null && vars.length > 0) {
            // Keep UI order (natural by name)
            Arrays.sort(vars, Comparator.comparing(MetadataElement::getName, String.CASE_INSENSITIVE_ORDER));
            for (int vi = 0; vi < vars.length; vi++) {
                MetadataElement v = vars[vi];
                List<MetadataAttribute> lines = orderedAttrs(v);
                if (!lines.isEmpty()) {
                    // Indent each stored line by two spaces (stored lines already indent attributes by 2)
                    for (MetadataAttribute a : lines) {
                        String line = stripOrder(a.getName());
                        if (!line.isEmpty()) {
                            out.append("  ").append(line).append('\n');
                        }
                    }
                    if (vi < vars.length - 1) out.append('\n'); // blank line between variables
                }
            }
        }
        return out.toString();
    }

    private static List<MetadataAttribute> orderedAttrs(MetadataElement element) {
        MetadataAttribute[] arr = element.getAttributes();
        if (arr == null || arr.length == 0) return List.of();
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

    private static String currentFilePathOrName() {
        SnapApp app = SnapApp.getDefault();
        if (app != null) {
            ProductSceneView v = app.getSelectedProductSceneView();
            if (v != null) {
                Product p = v.getProduct();
                if (p != null) {
                    try {
                        // Product#getFileLocation() exists in SNAP 12; fall back if null.
                        File f = p.getFileLocation();
                        if (f != null) return f.getAbsolutePath();
                    } catch (Throwable ignored) { /* older API */ }
                    return p.getName();
                }
            }
        }
        return "<unknown>";
    }
}
