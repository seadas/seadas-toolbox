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
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
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
        "HINT_PanoplyDumpTopComponent=Shows Panoply-style (ncdump) text for the selected variable"
})
public final class PanoplyDumpTopComponent extends TopComponent implements LookupListener, PropertyChangeListener {

    private static final String HINT = "Select a variable under Metadata \u2192 Panoply \u2192 Geophysical_Data …";
    private static final String PAN  = "Panoply";
    private static final String GEO  = "Geophysical_Data";
    private static final String ZWSP = "\u200B"; // used to store sort-order at the end of attr names

    private final JTextArea textArea = new JTextArea();
    // We keep a node result hook if we want, but registry listener is the workhorse:
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
        // Listen to NetBeans global registry (most reliable across SNAP views)
        TopComponent.getRegistry().addPropertyChangeListener(this);

        // Optional: also listen to global Node context if available
        nodeSel = org.openide.util.Utilities.actionsGlobalContext().lookupResult(Node.class);
        nodeSel.addLookupListener(this);

        updateFromActivatedNodes();
    }

    @Override public void componentClosed() {
        TopComponent.getRegistry().removePropertyChangeListener(this);
        if (nodeSel != null) nodeSel.removeLookupListener(this);
        nodeSel = null;
    }

    // React to activated nodes switches
    @Override public void propertyChange(PropertyChangeEvent evt) {
        String p = evt.getPropertyName();
        if (TopComponent.Registry.PROP_ACTIVATED_NODES.equals(p) ||
                TopComponent.Registry.PROP_ACTIVATED.equals(p)) {
            SwingUtilities.invokeLater(this::updateFromActivatedNodes);
        }
    }

    // Fallback listener (not strictly necessary with registry listener, but harmless)
    @Override public void resultChanged(LookupEvent ev) {
        SwingUtilities.invokeLater(this::updateFromActivatedNodes);
    }

    private void updateFromActivatedNodes() {
        String dump = null;

        Node[] act = TopComponent.getRegistry().getActivatedNodes();
        if (act != null) {
            for (Node n : act) {
                System.out.println("[PanoplyDump] Node: " + safeNodeName(n));
                MetadataElement me = firstMetadataElementInAncestors(n);
                if (me != null) {
                    System.out.println("  -> Found MetadataElement: " + me.getName());
                }
                if (isPanoplyVar(me)) {
                    dump = buildDumpFrom(me);
                    break;
                }
            }
        }

        if (dump == null) {
            MetadataElement me = org.openide.util.Utilities.actionsGlobalContext().lookup(MetadataElement.class);
            if (isPanoplyVar(me)) {
                dump = buildDumpFrom(me);
            }
        }

        textArea.setText((dump != null && !dump.isEmpty()) ? dump : HINT);
        textArea.setCaretPosition(0);
    }

    /** true only for elements Panoply → Geophysical_Data → <var> */
    /** true only for elements Panoply → Geophysical_Data → <var> (robust + case-insensitive) */
    private static boolean isPanoplyVar(MetadataElement el) {
        if (el == null) return false;
        MetadataElement p = el.getParentElement();
        if (p == null) return false;
        MetadataElement g = p.getParentElement();
        if (g == null) return false;

        String pn = p.getName() != null ? p.getName().trim() : "";
        String gn = g.getName() != null ? g.getName().trim() : "";

        // Accept case-insensitive exact match
        if ("geophysical_data".equalsIgnoreCase(pn) && "panoply".equalsIgnoreCase(gn)) {
            return true;
        }

        // Be tolerant: if parent isn’t exactly Geophysical_Data, see if grand-grandparent is Panoply
        MetadataElement gg = g.getParentElement();
        if (gg != null) {
            String ggn = gg.getName() != null ? gg.getName().trim() : "";
            if ("panoply".equalsIgnoreCase(gn) && "geophysical_data".equalsIgnoreCase(pn)) return true;
            if ("panoply".equalsIgnoreCase(ggn) && "geophysical_data".equalsIgnoreCase(gn)) {
                // i.e. Panoply → (wrapper) → Geophysical_Data → <var>
                return true;
            }
        }
        return false;
    }

    /** Build plain ncdump text from various storage layouts */
    private static String buildDumpFrom(MetadataElement varElem) {
        if (varElem == null) return null;

        // 1) Preferred: attributes-as-lines (name holds the line, suffixed with \u200B<order>)
        MetadataAttribute[] arr = varElem.getAttributes();
        if (arr != null && arr.length > 0) {
            java.util.List<MetadataAttribute> attrs = new java.util.ArrayList<>(java.util.Arrays.asList(arr));

            // If we find a single "ncdump" attribute, prefer it directly
            for (MetadataAttribute a : attrs) {
                if ("ncdump".equalsIgnoreCase(a.getName())) {
                    String val = a.getData() != null ? a.getData().getElemString() : null;
                    if (val != null && !val.isEmpty()) {
                        // System.out.println("[PanoplyDump] using single ncdump attribute");
                        return val.endsWith("\n") ? val : (val + "\n");
                    }
                }
            }

            // Otherwise: sort by \u200B<order> and strip the suffix from the line
            final String ZWSP = "\u200B";
            attrs.sort(java.util.Comparator.comparingInt(a -> {
                String n = a.getName();
                int i = n != null ? n.lastIndexOf(ZWSP) : -1;
                if (i < 0) return Integer.MAX_VALUE;
                try { return Integer.parseInt(n.substring(i + 1)); }
                catch (NumberFormatException ex) { return Integer.MAX_VALUE; }
            }));

            StringBuilder sb = new StringBuilder(Math.max(2048, attrs.size() * 64));
            int lineCount = 0;
            for (MetadataAttribute a : attrs) {
                String n = a.getName();
                if (n == null) continue;

                // Ignore our book-keeping attribute if present
                if ("ncdump".equalsIgnoreCase(n)) continue;

                int i = n.lastIndexOf(ZWSP);
                String line = (i >= 0) ? n.substring(0, i) : n;
                if (line != null && !line.isBlank()) {
                    sb.append(line).append('\n');
                    lineCount++;
                }
            }
            if (lineCount > 0) {
                // System.out.println("[PanoplyDump] built from attribute names: " + lineCount + " lines");
                return sb.toString();
            }
            // else fall through to child-element forms
        }

        // 2) Single child element "Dump" with attribute "text"
        MetadataElement dumpEl = varElem.getElement("Dump");
        if (dumpEl != null) {
            MetadataAttribute textAttr = dumpEl.getAttribute("text");
            if (textAttr != null && textAttr.getData() != null) {
                String s = textAttr.getData().getElemString();
                if (s != null && !s.isEmpty()) {
                    // System.out.println("[PanoplyDump] built from Dump@text");
                    return s.endsWith("\n") ? s : (s + "\n");
                }
            }
        }

        // 3) Repeated child elements "Line" with attribute "text"
        MetadataElement[] kids = varElem.getElements();
        if (kids != null && kids.length > 0) {
            StringBuilder sb = new StringBuilder(1024);
            int lineCount = 0;
            for (MetadataElement k : kids) {
                if (!"Line".equalsIgnoreCase(k.getName())) continue;
                MetadataAttribute ta = k.getAttribute("text");
                if (ta != null && ta.getData() != null) {
                    String s = ta.getData().getElemString();
                    if (s != null && !s.isEmpty()) {
                        sb.append(s).append('\n');
                        lineCount++;
                    }
                }
            }
            if (lineCount > 0) {
                // System.out.println("[PanoplyDump] built from child Line@text: " + lineCount + " lines");
                return sb.toString();
            }
        }

        // Nothing found
        // System.out.println("[PanoplyDump] no dump content found for " + varElem.getName());
        return null;
    }


    /** climb node→parents and return the first MetadataElement found in any lookup */
    private static MetadataElement firstMetadataElementInAncestors(Node node) {
        for (Node cur = node; cur != null; cur = cur.getParentNode()) {
            MetadataElement me = cur.getLookup().lookup(MetadataElement.class);
            if (me != null) return me;
        }
        return null;
    }

    /** From a node under Product → Metadata → Panoply → Geophysical_Data → <var>, return "<var>" */
    private static String deriveVarNameFromPath(Node node) {
        // Collect names from node up to root
        List<String> up = new ArrayList<>(16);
        for (Node cur = node; cur != null; cur = cur.getParentNode()) {
            String nm = safeNodeName(cur);
            if (nm != null && !nm.isEmpty()) up.add(nm);
        }
        if (up.isEmpty()) return null;

        // now root→leaf
        Collections.reverse(up);

        // Find "... , Panoply, Geophysical_Data, <var>"
        int iPan = indexOf(up, PAN);
        if (iPan < 0 || iPan + 2 >= up.size()) return null;
        if (!GEO.equals(up.get(iPan + 1))) return null;

        String var = up.get(iPan + 2);
        return (var != null && !var.isEmpty()) ? var : null;
    }

    private static int indexOf(List<String> list, String needle) {
        for (int i = 0; i < list.size(); i++) {
            if (needle.equals(list.get(i))) return i;
        }
        return -1;
    }

    /** Resolve Panoply/Geophysical_Data/<varName> from the selected product (ProductSceneView) */
    private static MetadataElement resolvePanoplyVarFromCurrentProduct(String varName) {
        SnapApp app = SnapApp.getDefault();
        if (app == null) return null;
        ProductSceneView view = app.getSelectedProductSceneView();
        if (view == null || view.getProduct() == null) return null;

        MetadataElement root = view.getProduct().getMetadataRoot();
        if (root == null) return null;

        MetadataElement pan = root.getElement(PAN);
        if (pan == null) return null;

        MetadataElement geo = pan.getElement(GEO);
        if (geo == null) return null;

        return geo.getElement(varName);
    }

    /** Defensive node name extraction (strip any HTML decorations) */
    private static String safeNodeName(Node n) {
        if (n == null) return null;
        String s = n.getName();
        if (s == null || s.isEmpty()) s = n.getDisplayName();
        if (s == null) return null;
        return s.replaceAll("<[^>]+>", "").trim();
    }
}
