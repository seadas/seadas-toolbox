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
import org.openide.util.Utilities;
import org.openide.windows.TopComponent;

import javax.swing.*;
import java.awt.*;
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
public final class PanoplyDumpTopComponent extends TopComponent implements LookupListener {

    private static final String HINT = "Select a variable under Metadata \u2192 Panoply \u2192 Geophysical_Data \u2026";
    private static final String PANOPLY = "Panoply";
    private static final String GEO_DATA = "Geophysical_Data";
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

    @Override public void componentOpened() {
        nodeSel = Utilities.actionsGlobalContext().lookupResult(Node.class);
        nodeSel.addLookupListener(this);
        resultChanged(null);
    }

    @Override public void componentClosed() {
        if (nodeSel != null) {
            nodeSel.removeLookupListener(this);
            nodeSel = null;
        }
    }

    @Override public void resultChanged(LookupEvent ev) {
        SwingUtilities.invokeLater(this::updateFromSelection);
    }

    private void updateFromSelection() {
        String dump = null;

        if (nodeSel != null) {
            for (Node n : nodeSel.allInstances()) {
                // 1) Best case: the node exposes the MetadataElement
                MetadataElement me = n.getLookup().lookup(MetadataElement.class);
                if (isPanoplyVar(me)) {
                    dump = buildDumpFrom(me);
                    break;
                }

                // 2) Fallback: derive var name from the node path in the explorer
                VarPath vp = derivePanoplyVarFromNodePath(n);
                if (vp != null) {
                    MetadataElement me2 = resolvePanoplyVarFromCurrentProduct(vp.varName);
                    if (me2 != null) {
                        dump = buildDumpFrom(me2);
                        break;
                    }
                }
            }
        }

        // 3) Final fallback: some views put MetadataElement directly in global ctx
        if (dump == null) {
            MetadataElement me = Utilities.actionsGlobalContext().lookup(MetadataElement.class);
            if (isPanoplyVar(me)) {
                dump = buildDumpFrom(me);
            }
        }

        textArea.setText(dump != null && !dump.isEmpty() ? dump : HINT);
        textArea.setCaretPosition(0);
    }

    /** Check tree position Panoply → Geophysical_Data → <var> */
    private static boolean isPanoplyVar(MetadataElement el) {
        if (el == null) return false;
        MetadataElement p = el.getParentElement();
        if (p == null) return false;
        MetadataElement g = p.getParentElement();
        return g != null && GEO_DATA.equals(p.getName()) && PANOPLY.equals(g.getName());
    }

    /** Try to recognize a node under Panoply/Geophysical_Data and extract var name. */
    private static VarPath derivePanoplyVarFromNodePath(Node node) {
        if (node == null) return null;

        // Walk up parents and collect names
        String name = safeNodeName(node);
        String parent = null, grand = null;

        Node p = node.getParentNode();
        if (p != null) {
            parent = safeNodeName(p);
            Node g = p.getParentNode();
            if (g != null) {
                grand = safeNodeName(g);
            }
        }

        if (GEO_DATA.equals(parent) && PANOPLY.equals(grand) && name != null && !name.isEmpty()) {
            return new VarPath(name);
        }
        return null;
    }

    /** Resolve metadata element Panoply/Geophysical_Data/<varName> from the selected product. */
    private static MetadataElement resolvePanoplyVarFromCurrentProduct(String varName) {
        SnapApp app = SnapApp.getDefault();
        if (app == null) return null;
        ProductSceneView view = app.getSelectedProductSceneView();
        if (view == null || view.getProduct() == null) return null;

        MetadataElement root = view.getProduct().getMetadataRoot();
        if (root == null) return null;

        MetadataElement pan = root.getElement(PANOPLY);
        if (pan == null) return null;
        MetadataElement geo = pan.getElement(GEO_DATA);
        if (geo == null) return null;
        return geo.getElement(varName);
    }

    /** Rebuild plain ncdump lines from stored attributes (names carry \u200Border suffix). */
    private static String buildDumpFrom(MetadataElement varElem) {
        List<MetadataAttribute> attrs = new ArrayList<>(Arrays.asList(varElem.getAttributes()));
        attrs.sort(Comparator.comparingInt(a -> {
            String n = a.getName();
            int i = n.lastIndexOf(ZWSP);
            if (i < 0) return Integer.MAX_VALUE;
            try { return Integer.parseInt(n.substring(i + 1)); }
            catch (NumberFormatException ex) { return Integer.MAX_VALUE; }
        }));

        StringBuilder sb = new StringBuilder(Math.max(2048, attrs.size() * 64));
        for (MetadataAttribute a : attrs) {
            String line = a.getName();
            int i = line.lastIndexOf(ZWSP);
            if (i >= 0) line = line.substring(0, i);
            sb.append(line).append('\n');
        }
        return sb.toString();
    }

    /** Defensive name extraction (fall back to display name, strip HTML if any). */
    private static String safeNodeName(Node n) {
        if (n == null) return null;
        String s = n.getName();
        if (s == null || s.isEmpty()) s = n.getDisplayName();
        if (s == null) return null;
        // remove trivial HTML wrappers if a renderer added them
        return s.replaceAll("<[^>]+>", "").trim();
    }

    private record VarPath(String varName) {}
}
