package gov.nasa.gsfc.seadas.panoply.ui;

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

@TopComponent.Description(preferredID = "MetadataDumpTopComponent", persistenceType = TopComponent.PERSISTENCE_ALWAYS)
@TopComponent.Registration(mode = "output", openAtStartup = false)
@ActionID(category = "View", id = "gov.nasa.gsfc.seadas.panoply.ui.MetadataDumpTopComponent")
@ActionReference(path = "Menu/View", position = 19300)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_MetadataDumpAction",
        preferredID = "MetadataDumpTopComponent")
@Messages({
        "CTL_MetadataDumpAction=Metadata Dump",
        "CTL_MetadataDumpTopComponent=Metadata Dump",
        "HINT_MetadataDumpTopComponent=Shows Panoply-style (ncdump) text for the selected variable or group"
})

//@TopComponent.Description(
//        preferredID = "MetadataDumpTopComponent",      // <-- use this exact ID in the watcher
//        persistenceType = TopComponent.PERSISTENCE_ONLY_OPENED
//)
//@TopComponent.Registration(mode = "output", openAtStartup = false)
//@ActionID(category = "Window", id = "gov.nasa.gsfc.seadas.panoply.MetadataDumpTopComponent")
//@ActionRegistration(displayName = "Metadata_Dump")
//@TopComponent.OpenActionRegistration(
//        displayName = "Metadata Dump",
//        preferredID = "MetadataDumpTopComponent"
//)
public final class MetadataDumpTopComponent extends TopComponent implements LookupListener, PropertyChangeListener {

    private static final String HINT = "Select a variable or group under Metadata \u2192 MetadataDump …";


    private final JTextArea textArea = new JTextArea();
    private org.openide.util.Lookup.Result<Node> nodeSel;
    private volatile org.esa.snap.core.datamodel.MetadataElement lastShown; // what we last showed
    private volatile boolean selfActive = false;

    public MetadataDumpTopComponent() {
        setName(Bundle.CTL_MetadataDumpTopComponent());
        setToolTipText(Bundle.HINT_MetadataDumpTopComponent());
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

    @Override
    protected void componentClosed() {
        lastShown = null;
        super.componentClosed();
    }

    @Override protected void componentActivated()   { selfActive = true;  }
    @Override protected void componentDeactivated() { selfActive = false; }
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


    // --- add at top-level inside MetadataDumpTopComponent ---
    private static volatile boolean USER_CLOSED = false;   // tracks if user clicked 'X'

    @Override
    public int getPersistenceType() {
        // only restore if it was explicitly open when the app closed
        return TopComponent.PERSISTENCE_ONLY_OPENED;
    }

    /** Selection watcher uses this to decide whether to auto-open. */
    public static boolean isUserClosed() {
        return USER_CLOSED;
    }

    private boolean lastShownHadContent = false;

    public void showSection(final org.esa.snap.core.datamodel.MetadataElement section) {
        if (section == null) {
            textArea.setText("");
            return;
        }

        // Collect line_XXXXX attributes in order
        final java.util.List<org.esa.snap.core.datamodel.MetadataAttribute> lines = new java.util.ArrayList<>();
        for (int i = 0; i < section.getNumAttributes(); i++) {
            org.esa.snap.core.datamodel.MetadataAttribute a = section.getAttributeAt(i);
            if (a != null) {
                String n = a.getName();
                if (n != null && n.startsWith("line_")) {
                    lines.add(a);
                }
            }
        }
        lines.sort(java.util.Comparator.comparingInt(a -> {
            String n = a.getName();
            int us = (n != null) ? n.lastIndexOf('_') : -1;
            if (us >= 0 && us + 1 < n.length()) {
                try { return Integer.parseInt(n.substring(us + 1)); } catch (Exception ignore) {}
            }
            return Integer.MAX_VALUE;
        }));

        final boolean hasContent = !lines.isEmpty();

        // Only skip re-render if it's literally the same element AND we already rendered content
        if (section == lastShown && lastShownHadContent) return;
        lastShown = section;
        lastShownHadContent = hasContent;

        // Build text
        final StringBuilder sb = new StringBuilder(hasContent ? lines.size() * 64 : 64);
        if (hasContent) {
            for (org.esa.snap.core.datamodel.MetadataAttribute a : lines) {
                String v;
                try { v = (a.getData() != null) ? a.getData().getElemString() : ""; }
                catch (Throwable t) { v = ""; }
                //if (!v.isEmpty()) sb.append(v).append('\n');
                sb.append(v).append('\n');
            }
        } else {
            // Helpful fallback so we can SEE when a node has no dump lines
            sb.append("// (no dump lines on element '").append(section.getName()).append("')\n");
            sb.append("// attributes: ").append(section.getNumAttributes())
                    .append(", children: ").append(section.getNumElements()).append('\n');
        }

        // Debug log so we know what's going on
        System.out.println("[Dump] render '" + section.getName() + "': "
                + (hasContent ? ("lines=" + lines.size()
                + (lines.size() > 0 ? " first=" + lines.get(0).getName() : ""))
                : "NO LINES"));

        // Always update UI on EDT
        javax.swing.SwingUtilities.invokeLater(() -> {
            textArea.setText(sb.toString());   // ensure 'text' is your JTextArea
            textArea.setCaretPosition(0);
        });
    }

    private static int orderOf(org.esa.snap.core.datamodel.MetadataAttribute a) {
        String n = a.getName(); // e.g., line_00012
        int us = (n != null) ? n.lastIndexOf('_') : -1;
        if (us >= 0 && us + 1 < n.length()) {
            try { return Integer.parseInt(n.substring(us + 1)); } catch (Exception ignore) {}
        }
        return Integer.MAX_VALUE;
    }

    // Debounce rapid selection changes
    private final javax.swing.Timer debounce = new javax.swing.Timer(120, e -> doUpdateFromSelection());
    { debounce.setRepeats(false); }

    private void updateFromActivatedNodes() {
        org.openide.nodes.Node[] sel = org.openide.windows.TopComponent.getRegistry().getActivatedNodes();
        if (sel == null || sel.length == 0) return; // keep showing what we had

        org.esa.snap.core.datamodel.ProductNode pn = sel[0].getLookup().lookup(org.esa.snap.core.datamodel.ProductNode.class);
        if (pn == null) return;

        org.esa.snap.core.datamodel.MetadataElement start = null;
        if (pn instanceof org.esa.snap.core.datamodel.MetadataAttribute) {
            start = ((org.esa.snap.core.datamodel.MetadataAttribute) pn).getParentElement();
        } else if (pn instanceof org.esa.snap.core.datamodel.MetadataElement) {
            start = (org.esa.snap.core.datamodel.MetadataElement) pn;
        } else {
            return;
        }
        org.esa.snap.core.datamodel.MetadataElement target = nearestDumpElement(start);
        if (target == null) return;

        showSection(target);
    }


    private void doUpdateFromSelection() {
        // If the dump window is the active TC, ignore selection blips
        if (selfActive) return;

        org.openide.nodes.Node[] sel = org.openide.windows.TopComponent.getRegistry().getActivatedNodes();
        if (sel == null || sel.length == 0) {
            // IMPORTANT: do NOT clear — keep showing lastTarget
            return;
        }

        org.esa.snap.core.datamodel.ProductNode pn = sel[0].getLookup().lookup(org.esa.snap.core.datamodel.ProductNode.class);
        if (pn == null) return;

        org.esa.snap.core.datamodel.MetadataElement start = null;
        if (pn instanceof org.esa.snap.core.datamodel.MetadataAttribute) {
            start = ((org.esa.snap.core.datamodel.MetadataAttribute) pn).getParentElement();
        } else if (pn instanceof org.esa.snap.core.datamodel.MetadataElement) {
            start = (org.esa.snap.core.datamodel.MetadataElement) pn;
        } else {
            return;
        }

        org.esa.snap.core.datamodel.MetadataElement target = nearestDumpElement(start);
        if (target == null || target == lastShown) return;

        lastShown = target;
        showSection(target);  // renders lines; showSection should *not* clear if target is null
    }

    private org.esa.snap.core.datamodel.MetadataElement nearestDumpElement(
            org.esa.snap.core.datamodel.MetadataElement start) {
        for (org.esa.snap.core.datamodel.MetadataElement cur = start; cur != null; cur = cur.getParentElement()) {
            if (hasDumpLines(cur)) return cur;
            if ("Metadata_Dump".equals(cur.getName())) break;
        }
        return null;
    }

    private boolean hasDumpLines(org.esa.snap.core.datamodel.MetadataElement e) {
        if (e == null) return false;
        for (int i = 0; i < e.getNumAttributes(); i++) {
            org.esa.snap.core.datamodel.MetadataAttribute a = e.getAttributeAt(i);
            if (a != null) {
                String n = a.getName();
                if (n != null && n.startsWith("line_")) return true;
            }
        }
        return false;
    }
}
