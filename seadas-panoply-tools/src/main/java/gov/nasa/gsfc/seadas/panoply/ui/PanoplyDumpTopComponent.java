package gov.nasa.gsfc.seadas.panoply.ui;

import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
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
import java.awt.BorderLayout;
import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

@TopComponent.Description(preferredID = "PanoplyDumpTopComponent", persistenceType = TopComponent.PERSISTENCE_ALWAYS)
@TopComponent.Registration(mode = "output", openAtStartup = false)
@ActionID(category = "Window", id = "gov.nasa.gsfc.seadas.panoply.ui.PanoplyDumpTopComponent")
@ActionReference(path = "Menu/Window", position = 19300)
@TopComponent.OpenActionRegistration(displayName = "#CTL_PanoplyDumpAction", preferredID = "PanoplyDumpTopComponent")
@Messages({
        "CTL_PanoplyDumpAction=Panoply Dump",
        "CTL_PanoplyDumpTopComponent=Panoply Dump",
        "HINT_PanoplyDumpTopComponent=Shows Panoply-style (ncdump) text for the selected variable"
})
public final class PanoplyDumpTopComponent extends TopComponent implements LookupListener, PropertyChangeListener {

    private static final String HINT = "Select a variable under Metadata \u2192 Panoply \u2192 <group> …";
    private static final String PAN  = "Panoply";
    private static final String ZWSP = "\u200B"; // suffix used for stable sort order

    // Accepted group names (match case-insensitively)
    private static final String[] GROUPS = new String[]{
            "Geophysical_Data",
            "Navigation_Data",
            "Processing_Control",
            "Scan_Line_Attributes",
            "Sensor_Band_Parameters"
    };

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
                // A) direct MetadataElement from node/ancestors
                MetadataElement me = firstMetadataElementInAncestors(n);
                if (isPanoplyVar(me)) {
                    dump = buildDumpFrom(me);
                    if (dump != null) break;
                }
                // B) derive var name from path and resolve from current product
                String varName = deriveVarNameFromPath(n);
                if (varName != null) {
                    MetadataElement resolved = resolvePanoplyVarFromCurrentProduct(varName);
                    if (isPanoplyVar(resolved)) {
                        dump = buildDumpFrom(resolved);
                        if (dump != null) break;
                    }
                }
            }
        }

        // C) global context fallback
        if (dump == null) {
            MetadataElement me = org.openide.util.Utilities.actionsGlobalContext().lookup(MetadataElement.class);
            if (isPanoplyVar(me)) {
                dump = buildDumpFrom(me);
            }
        }

        textArea.setText((dump != null && !dump.isEmpty()) ? dump : HINT);
        textArea.setCaretPosition(0);
    }

    /** True only for elements Panoply → (any GROUPS, case-insensitive) → <var> */
    private static boolean isPanoplyVar(MetadataElement el) {
        if (el == null) return false;
        MetadataElement parent = el.getParentElement();
        if (parent == null) return false;
        MetadataElement grand = parent.getParentElement();
        if (grand == null) return false;
        if (!equalsIc(grand.getName(), PAN)) return false;
        return isAllowedGroupName(parent.getName());
    }

    /** climb node→parents and return the first MetadataElement found in any lookup */
    private static MetadataElement firstMetadataElementInAncestors(Node node) {
        for (Node cur = node; cur != null; cur = cur.getParentNode()) {
            MetadataElement me = cur.getLookup().lookup(MetadataElement.class);
            if (me != null) return me;
        }
        return null;
    }

    /** From a node under Product → Metadata → Panoply → <group> → <var>, return "<var>" */
    private static String deriveVarNameFromPath(Node node) {
        java.util.ArrayList<String> up = new java.util.ArrayList<>(16);
        for (Node cur = node; cur != null; cur = cur.getParentNode()) {
            String nm = safeNodeName(cur);
            if (nm != null && !nm.isEmpty()) up.add(nm);
        }
        if (up.isEmpty()) return null;

        Collections.reverse(up); // root→leaf

        int iPan = lastIndexOfIc(up, PAN);
        if (iPan < 0 || iPan + 2 >= up.size()) return null;

        String groupName = up.get(iPan + 1);
        if (!isAllowedGroupName(groupName)) return null;

        String var = up.get(iPan + 2);
        return (var != null && !var.isEmpty()) ? var : null;
    }

    /** Resolve Panoply/<group>/<varName> from the selected product (case-insensitive). */
    private static MetadataElement resolvePanoplyVarFromCurrentProduct(String varName) {
        SnapApp app = SnapApp.getDefault();
        if (app == null) return null;
        ProductSceneView view = app.getSelectedProductSceneView();
        if (view == null || view.getProduct() == null) return null;

        MetadataElement root = view.getProduct().getMetadataRoot();
        if (root == null) return null;

        MetadataElement pan = getChildIgnoreCase(root, PAN);
        if (pan == null) return null;

        for (String g : GROUPS) {
            MetadataElement group = getChildIgnoreCase(pan, g);
            if (group != null) {
                MetadataElement var = getChildIgnoreCase(group, varName);
                if (var != null) return var;
            }
        }
        return null;
    }
    /** Build plain ncdump text from the Panoply var element.
     *  Supports two storage styles:
     *  A) One big attribute named "ncdump" (value holds the whole text)
     *  B) Many attributes whose *names* are lines suffixed with ZWSP + order
     */
    private static String buildDumpFrom(MetadataElement varElem) {
        MetadataAttribute[] arr = varElem.getAttributes();
        if (arr == null || arr.length == 0) return null;

        // --- Style A: single "ncdump" attribute whose *value* is the full text ---
        if (arr.length == 1) {
            MetadataAttribute a = arr[0];
            String name = a.getName();
            String val  = a.getData() != null ? a.getData().getElemString() : null;
            if (name != null && name.equalsIgnoreCase("ncdump") && val != null && !val.isEmpty()) {
                return ensureTrailingNewline(val);
            }
        }
        // Also handle the case where there are a few attrs, none with ZWSP, but at least
        // one has a non-empty value — concatenate their values line-by-line.
        boolean hasZWSP = false;
        for (MetadataAttribute a : arr) {
            String n = a.getName();
            if (n != null && n.indexOf(ZWSP) >= 0) { hasZWSP = true; break; }
        }
        if (!hasZWSP) {
            StringBuilder byValue = new StringBuilder();
            for (MetadataAttribute a : arr) {
                String v = a.getData() != null ? a.getData().getElemString() : null;
                if (v != null && !v.isEmpty()) {
                    byValue.append(v).append('\n');
                }
            }
            if (byValue.length() > 0) return byValue.toString();
            // If still nothing useful, fall through to name-based as last resort
        }

        // --- Style B: many attributes whose *names* are the lines + ZWSP + order ---
        java.util.List<MetadataAttribute> attrs = new ArrayList<>(Arrays.asList(arr));
        attrs.sort(Comparator.comparingInt(a -> {
            String n = a.getName();
            int i = n != null ? n.lastIndexOf(ZWSP) : -1;
            if (i < 0) return Integer.MAX_VALUE;
            try { return Integer.parseInt(n.substring(i + 1)); }
            catch (NumberFormatException ex) { return Integer.MAX_VALUE; }
        }));

        StringBuilder sb = new StringBuilder(Math.max(2048, attrs.size() * 64));
        for (MetadataAttribute a : attrs) {
            String line = a.getName();
            if (line == null) continue;
            int i = line.lastIndexOf(ZWSP);
            if (i >= 0) line = line.substring(0, i);
            sb.append(line).append('\n');
        }
        String out = sb.toString().trim();
        return out.isEmpty() ? null : out + '\n';
    }

    private static String ensureTrailingNewline(String s) {
        return (s.endsWith("\n") || s.endsWith("\r")) ? s : (s + "\n");
    }

    /** Get child MetadataElement by name, case-insensitive. */
    private static MetadataElement getChildIgnoreCase(MetadataElement parent, String name) {
        if (parent == null || name == null) return null;
        for (MetadataElement ch : parent.getElements()) {
            if (equalsIc(ch.getName(), name)) return ch;
        }
        return null;
    }

    private static boolean isAllowedGroupName(String name) {
        if (name == null) return false;
        for (String g : GROUPS) {
            if (equalsIc(g, name)) return true;
        }
        return false;
    }

    private static boolean equalsIc(String a, String b) {
        return a != null && b != null && a.equalsIgnoreCase(b);
    }

    private static int lastIndexOfIc(java.util.List<String> list, String needle) {
        for (int i = list.size() - 1; i >= 0; i--) {
            if (equalsIc(list.get(i), needle)) return i;
        }
        return -1;
    }

    /** Strip any HTML decorations and trim. */
    private static String safeNodeName(Node n) {
        if (n == null) return null;
        String s = n.getName();
        if (s == null || s.isEmpty()) s = n.getDisplayName();
        if (s == null) return null;
        return s.replaceAll("<[^>]+>", "").trim();
    }
}
