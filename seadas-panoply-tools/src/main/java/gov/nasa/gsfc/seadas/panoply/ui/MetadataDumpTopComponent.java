package gov.nasa.gsfc.seadas.panoply.ui;

import org.esa.snap.core.datamodel.MetadataElement;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.nodes.Node;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Dimension;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@TopComponent.Description(
        preferredID = "MetadataDumpTopComponent",
        persistenceType = TopComponent.PERSISTENCE_ONLY_OPENED
)
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

public final class MetadataDumpTopComponent extends TopComponent implements LookupListener, PropertyChangeListener {

    private static final String HINT = "Select a variable or group under Metadata \u2192 MetadataDump …";


    private final JTextArea textArea = new JTextArea();
    private org.openide.util.Lookup.Result<Node> nodeSel;
    private volatile org.esa.snap.core.datamodel.MetadataElement lastShown; // what we last showed
    private volatile boolean selfActive = false;

    private static final String PREFERRED_ID = "MetadataDumpTopComponent";
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

    public static MetadataDumpTopComponent findInstance() {
        TopComponent tc = WindowManager.getDefault().findTopComponent(PREFERRED_ID);
        return (tc instanceof MetadataDumpTopComponent) ? (MetadataDumpTopComponent) tc : null;
    }

    public static MetadataDumpTopComponent openSingleton() {
        // Find existing instance by preferred ID
        MetadataDumpTopComponent tc = findInstance();
        if (tc == null) {
            // Construct once
            tc = new MetadataDumpTopComponent();
        }

        // Open only if needed
        if (!tc.isOpened()) {
            tc.open();
        }

        // Request focus (no further window opens)
        tc.requestActive();
        return tc;
    }


    /** Call this when product/selection changes away from our nodes */
    public void clearView() {
        textArea.setText("");     // your JTextArea reference
        lastShown = null;         // ensure we don't think we’re showing stale section
    }
    @Override
    public void componentOpened() {
        TopComponent.getRegistry().addPropertyChangeListener(this);
        nodeSel = org.openide.util.Utilities.actionsGlobalContext().lookupResult(Node.class);
        nodeSel.addLookupListener(this);
        USER_CLOSED = false;                  // user opened / window available
        updateFromActivatedNodes();
    }

    @Override
    protected void componentClosed() {
        USER_CLOSED = true;                   // user explicitly closed
        if (nodeSel != null) {
            nodeSel.removeLookupListener(this);
            nodeSel = null;
        }
        TopComponent.getRegistry().removePropertyChangeListener(this);
        lastShown = null;
        textArea.setText("");                 // clear UI too
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

    public void showSection(org.esa.snap.core.datamodel.MetadataElement section) {
        if (section == null) return;

        // Remember last shown so selection handler can avoid flicker
        lastShown = section;

        // If this is the top wrapper, render an ncdump-like whole-file view
        boolean isWrapper = "Metadata_Dump".equals(section.getName());
        final StringBuilder sb = new StringBuilder();

//        if (isWrapper) {
//            // Resolve something we can open & something we can display
//            String resolvedPath = resolveNetcdfPath(section);          // open this if possible
//            String displayName  = (resolvedPath != null && !resolvedPath.isEmpty())
//                    ? resolvedPath
//                    : inferNameFromChildren(section);
//
//            if (displayName != null && !displayName.isEmpty()) {
//                sb.append("netcdf ").append(displayName).append(" {\n");
//            } else {
//                sb.append("netcdf (unknown) {\n");
//            }
//
//            // Dimensions: try real file first, else heuristic
//            String dimBlock = buildDimensionsBlockFromNetcdf(resolvedPath);
//            if (dimBlock == null) {
//                dimBlock = buildDimensionsBlockHeuristic(section);
//            }
//            if (dimBlock != null && !dimBlock.isEmpty()) {
//                sb.append(dimBlock);
//            }
//
//            // Rest of the aggregated dump
//            java.util.List<String> all = new java.util.ArrayList<>();
//            collectDescendantLines(section, all);
//            for (String line : all) sb.append(line).append('\n');
//
//            sb.append("}\n");
//            textArea.setText(sb.toString());
//            textArea.setCaretPosition(0);
//            return;
//        }

        if (isWrapper) {
            // 1) If a 'ncdump' child exists and has line_* -> show it verbatim
            org.esa.snap.core.datamodel.MetadataElement ncd = childByName(section, "ncdump");
            if (ncd != null && hasDumpLines(ncd)) {
                // Render exactly the ncdump text (it already includes "netcdf ... { ... }")
                java.util.List<String> lines = ownLines(ncd);
                StringBuilder sbN = new StringBuilder(lines.size() * 64);
                for (String ln : lines) sbN.append(ln).append('\n');
                textArea.setText(sbN.toString());
                textArea.setCaretPosition(0);
                lastShown = section;
                return;
            }

            // 2) Fallback: your existing aggregated/root rendering (dimensions + groups)
            // (keep your previous code here unchanged)
            String resolvedPath = resolveNetcdfPath(section);
            String displayName  = (resolvedPath != null && !resolvedPath.isEmpty())
                    ? resolvedPath
                    : inferNameFromChildren(section);

            //StringBuilder sb = new StringBuilder();
            if (displayName != null && !displayName.isEmpty()) {
                sb.append("netcdf ").append(displayName).append(" {\n");
            } else {
                sb.append("netcdf (unknown) {\n");
            }

            String dimBlock = buildDimensionsBlockFromNetcdf(resolvedPath);
            if (dimBlock == null) dimBlock = buildDimensionsBlockHeuristic(section);
            if (dimBlock != null && !dimBlock.isEmpty()) sb.append(dimBlock);

            java.util.List<String> all = new java.util.ArrayList<>();
            collectDescendantLines(section, all);
            for (String line : all) sb.append(line).append('\n');

            sb.append("}\n");
            textArea.setText(sb.toString());
            textArea.setCaretPosition(0);
            lastShown = section;
            return;

//            java.util.List<String> ncdLines = linesWithPrefix(section, "ncdump_line_");
//            if (!ncdLines.isEmpty()) {
//                StringBuilder sbN = new StringBuilder(ncdLines.size() * 64);
//                for (String ln : ncdLines) sbN.append(ln).append('\n');
//                textArea.setText(sbN.toString());
//                textArea.setCaretPosition(0);
//                lastShown = section;
//                return;
//            }
        }


        // Otherwise: aggregate this group with all its descendants
        final List<String> aggregated = new ArrayList<>();
        collectDescendantLines(section, aggregated);
        if (aggregated.isEmpty()) {
            textArea.setText(HINT);
        } else {
            for (String s : aggregated) sb.append(s).append('\n');
            textArea.setText(sb.toString());
            textArea.setCaretPosition(0);
        }
    }


    private static java.util.List<String> linesWithPrefix(org.esa.snap.core.datamodel.MetadataElement e, String prefix) {
        java.util.List<org.esa.snap.core.datamodel.MetadataAttribute> attrs = new java.util.ArrayList<>();
        for (int i = 0; i < e.getNumAttributes(); i++) {
            org.esa.snap.core.datamodel.MetadataAttribute a = e.getAttributeAt(i);
            if (a != null && a.getName() != null && a.getName().startsWith(prefix)) {
                attrs.add(a);
            }
        }
        attrs.sort(java.util.Comparator.comparingInt(a -> {
            String n = a.getName();
            int us = n.lastIndexOf('_');
            try { return Integer.parseInt(n.substring(us + 1)); } catch (Exception ignore) { return Integer.MAX_VALUE; }
        }));
        java.util.List<String> out = new java.util.ArrayList<>(attrs.size());
        for (org.esa.snap.core.datamodel.MetadataAttribute a : attrs) {
            Object v = a.getData() != null ? a.getData().getElemString() : null;
            if (v != null) out.add(v.toString());
        }
        return out;
    }

    // Debounce rapid selection changes
    private final javax.swing.Timer debounce = new javax.swing.Timer(120, e -> doUpdateFromSelection());
    { debounce.setRepeats(false); }

    private void updateFromActivatedNodes() {
        org.openide.nodes.Node[] sel = org.openide.windows.TopComponent.getRegistry().getActivatedNodes();
        if (sel == null || sel.length == 0) {
            clearView();             // <- clear stale dump
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

    private MetadataElement nearestDumpElement(MetadataElement start) {
        for (MetadataElement cur = start; cur != null; cur = cur.getParentElement()) {
            if (hasDumpLines(cur)) return cur;
            if ("Metadata_Dump".equals(cur.getName())) {
                return cur;            // <- allow wrapper to be shown/aggregated
            }
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

    // Get direct child by name (returns null if not found)
    private static org.esa.snap.core.datamodel.MetadataElement childByName(
            org.esa.snap.core.datamodel.MetadataElement parent, String name) {
        if (parent == null || name == null) return null;
        for (int i = 0; i < parent.getNumElements(); i++) {
            org.esa.snap.core.datamodel.MetadataElement c = parent.getElementAt(i);
            if (c != null && name.equals(c.getName())) return c;
        }
        return null;
    }
    // NEW: helper – return this element's own "line_*" text in order
    private static java.util.List<String> ownLines(org.esa.snap.core.datamodel.MetadataElement section) {
        final java.util.List<org.esa.snap.core.datamodel.MetadataAttribute> attrs = new java.util.ArrayList<>();
        for (int i = 0; i < section.getNumAttributes(); i++) {
            org.esa.snap.core.datamodel.MetadataAttribute a = section.getAttributeAt(i);
            if (a != null) {
                String n = a.getName();
                if (n != null && n.startsWith("line_")) attrs.add(a);
            }
        }
        attrs.sort(java.util.Comparator.comparingInt(a -> {
            String n = a.getName();
            int us = (n != null) ? n.lastIndexOf('_') : -1;
            if (us >= 0 && us + 1 < n.length()) {
                try { return Integer.parseInt(n.substring(us + 1)); } catch (Exception ignore) {}
            }
            return Integer.MAX_VALUE;
        }));
        final java.util.List<String> out = new java.util.ArrayList<>(attrs.size());
        for (org.esa.snap.core.datamodel.MetadataAttribute a : attrs) {
            Object v = a.getData() != null ? a.getData().getElemString() : null;
            if (v != null) out.add(v.toString());
        }
        return out;
    }

    // NEW: recursively collect child lines (depth-first)
    private static void collectDescendantLines(org.esa.snap.core.datamodel.MetadataElement section,
                                               java.util.List<String> sink) {
        // own lines first
        sink.addAll(ownLines(section));
        // then children (depth-first)
        for (int i = 0; i < section.getNumElements(); i++) {
            org.esa.snap.core.datamodel.MetadataElement child = section.getElementAt(i);
            if (child != null) collectDescendantLines(child, sink);
        }
    }

    // NEW: attempt to infer filename from any child's "In file \"...\"" line
    private static String inferBasenameFromChildren(org.esa.snap.core.datamodel.MetadataElement root) {
        final Pattern p = Pattern.compile("^In file\\s+\"([^\"]+)\"\\s*$");
        Deque<MetadataElement> dq = new ArrayDeque<>();
        dq.add(root);
        while (!dq.isEmpty()) {
            org.esa.snap.core.datamodel.MetadataElement e = dq.removeFirst();
            for (String line : ownLines(e)) {
                Matcher m = p.matcher(line);
                if (m.find()) return m.group(1);
            }
            for (int i = 0; i < e.getNumElements(); i++) {
                org.esa.snap.core.datamodel.MetadataElement c = e.getElementAt(i);
                if (c != null) dq.addLast(c);
            }
        }
        return null;
    }

    // Try to pull a full path from any child's 'In file "..."' line
    private static String inferFullPathFromChildren(MetadataElement root) {
        final Pattern p = Pattern.compile("^\\s*(?:In file|in file)\\s+\"([^\"]+)\"\\s*$");
        Deque<MetadataElement> dq = new ArrayDeque<>();
        dq.add(root);
        while (!dq.isEmpty()) {
            MetadataElement e = dq.removeFirst();
            for (String line : ownLines(e)) {
                Matcher m = p.matcher(line);
                if (m.find()) return m.group(1);
            }
            for (int i = 0; i < e.getNumElements(); i++) {
                MetadataElement c = e.getElementAt(i);
                if (c != null) dq.addLast(c);
            }
        }
        return null;
    }

    // Build a dimensions block from a real NetCDF file if available.
    private static String buildDimensionsBlockFromNetcdf(String filePath) {
        if (filePath == null || filePath.isEmpty()) return null;
        try (ucar.nc2.NetcdfFile nc = ucar.nc2.NetcdfFiles.open(filePath)) {
            StringBuilder sb = new StringBuilder();
            sb.append("  dimensions:\n");
            for (ucar.nc2.Dimension d : nc.getDimensions()) {
                sb.append("    ").append(d.getShortName()).append(" = ").append(d.getLength());
                if (d.isUnlimited()) sb.append("   // (unlimited)");
                sb.append(";\n");
            }
            return sb.toString();
        } catch (Exception ignore) {
            return null;
        }
    }

    // Heuristic fallback: scan descendant attributes/lines for likely "dimension" items.
    private static String buildDimensionsBlockHeuristic(MetadataElement root) {
        // Keep insertion order stable
        Map<String, Long> dims = new LinkedHashMap<>();

        // 1) Prefer explicit dimension-looking attributes: names with number_of_*, *_per_*, *_lines, *_bands, etc.
        Pattern nameHint = Pattern.compile("(?i)(^number_of_.+|.+_per_.+|lines?$|bands?$|pixels?$)");
        for (String line : collectAllLines(root)) {
            // Try to catch "name = 123;" patterns
            // Examples: "number_of_lines = 1710;" or "pixels_per_line = 1272;"
            Matcher m = Pattern.compile("^\\s*([A-Za-z0-9_]+)\\s*=\\s*([0-9]+)\\s*;?\\s*$").matcher(line);
            if (m.find()) {
                String name = m.group(1);
                String valStr = m.group(2);
                if (nameHint.matcher(name).find()) {
                    try {
                        long v = Long.parseLong(valStr);
                        dims.putIfAbsent(name, v);
                    } catch (NumberFormatException ignored) { /* skip */ }
                }
            }
        }

        if (dims.isEmpty()) return null;

        StringBuilder sb = new StringBuilder();
        sb.append("  dimensions:\n");
        for (Map.Entry<String, Long> e : dims.entrySet()) {
            sb.append("    ").append(e.getKey()).append(" = ").append(e.getValue()).append(";\n");
        }
        return sb.toString();
    }

    // Utility: flatten all descendant lines (used by heuristic)
    private static List<String> collectAllLines(MetadataElement root) {
        List<String> out = new ArrayList<>();
        collectDescendantLines(root, out);
        return out;
    }

    // NEW: get the active Product file location (best source of truth)
    private static java.io.File getProductFile(MetadataElement section) {
        try {
            org.esa.snap.core.datamodel.Product p = section.getProduct();
            if (p != null) return p.getFileLocation();
        } catch (Throwable ignore) {}
        return null;
    }

    // NEW: tolerate comment markers ("// ") and whitespace before 'In file "..."
// Examples it will match: "In file "x.nc"", "// In file "x.nc"", "   //   In file "x.nc""
    private static final Pattern IN_FILE_PATTERN =
            Pattern.compile("^\\s*(?://\\s*)?(?i:In\\s+file)\\s+\"([^\"]+)\"\\s*$");

    // NEW: pull a candidate name from any descendant "In file" line (comments OK)
    private static String inferNameFromChildren(MetadataElement root) {
        java.util.Deque<MetadataElement> dq = new java.util.ArrayDeque<>();
        dq.add(root);
        while (!dq.isEmpty()) {
            MetadataElement e = dq.removeFirst();
            for (String line : ownLines(e)) {
                java.util.regex.Matcher m = IN_FILE_PATTERN.matcher(line);
                if (m.find()) return m.group(1);
            }
            for (int i = 0; i < e.getNumElements(); i++) {
                MetadataElement c = e.getElementAt(i);
                if (c != null) dq.addLast(c);
            }
        }
        return null;
    }

    // NEW: resolve an openable path, trying (1) product file, (2) child "In file" name,
// and (3) combining child name with product directory if child name is basename only.
    private static String resolveNetcdfPath(MetadataElement section) {
        java.io.File productFile = getProductFile(section);
        String childName = inferNameFromChildren(section); // may be "PACE_OCI....nc" or a full path

        // 1) If product file exists, it’s the most reliable
        if (productFile != null && productFile.exists()) {
            return productFile.getAbsolutePath();
        }

        // 2) If the child name looks like a full path or URL, try it directly
        if (childName != null && !childName.isEmpty()) {
            if (childName.startsWith("http://") || childName.startsWith("https://") ||
                    childName.startsWith("file:/") || childName.contains(java.io.File.separator)) {
                return childName;
            }
        }

        // 3) If we have a product directory and only a basename from children, join them
        if (productFile != null && childName != null && !childName.isEmpty()) {
            java.io.File dir = productFile.getParentFile();
            if (dir != null && dir.isDirectory()) {
                java.io.File candidate = new java.io.File(dir, childName);
                return candidate.getAbsolutePath();
            }
        }

        // 4) Last resort: return the childName (even if it’s just a basename)
        return childName; // may still be useful for display
    }


}
