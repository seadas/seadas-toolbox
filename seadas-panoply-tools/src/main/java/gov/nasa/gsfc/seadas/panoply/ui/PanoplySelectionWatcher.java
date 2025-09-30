package gov.nasa.gsfc.seadas.panoply.ui;

import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.ProductNode;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.Utilities;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

public final class PanoplySelectionWatcher implements LookupListener {
    private org.openide.util.Lookup.Result<MetadataElement> result;
    private static final String DUMP_TC_ID = "MetadataDumpTopComponent"; // update if you used a different preferredID

    public void start() {
        result = Utilities.actionsGlobalContext().lookupResult(MetadataElement.class);
        result.addLookupListener(this);
        resultChanged(null);
    }
    public void stop() {
        if (result != null) { result.removeLookupListener(this); result = null; }
    }

    @Override
    public void resultChanged(LookupEvent ev) {
        org.openide.nodes.Node[] sel = TopComponent.getRegistry().getActivatedNodes();
        if (sel == null || sel.length == 0) return;

        ProductNode pn = sel[0].getLookup().lookup(ProductNode.class);
        MetadataElement target = null;
        if (pn instanceof MetadataAttribute) {
            target = nearestDumpElement(((MetadataAttribute) pn).getParentElement());
        } else if (pn instanceof MetadataElement) {
            //target = nearestDumpElement((MetadataElement) pn);
            MetadataElement me = (MetadataElement) pn;
            // NEW: if it's the wrapper, show it (aggregated view will handle children)
            if ("Metadata_Dump".equals(me.getName())) {
                target = me;
            } else {
                if (hasDumpLines(me)) {
                    target = me;
                } else {
                    target = nearestDumpElement(me);
                }
            }
        }
        if (target == null) return;

        TopComponent tcRaw = WindowManager.getDefault().findTopComponent(DUMP_TC_ID);
        if (tcRaw == null) return;
        MetadataDumpTopComponent tc = (MetadataDumpTopComponent) tcRaw;

        if (!MetadataDumpTopComponent.isUserClosed()) {
            if (!tc.isOpened()) tc.open();
            tc.requestActive();
        }
        tc.showSection(target);
    }

    private static boolean hasDumpLines(MetadataElement e) {
        for (int i = 0; i < e.getNumAttributes(); i++) {
            MetadataAttribute a = e.getAttributeAt(i);
            if (a != null) {
                String n = a.getName();
                if (n != null && n.startsWith("line_")) return true;
            }
        }
        return false;
    }

    protected static MetadataElement nearestDumpElement(MetadataElement start) {
        for (MetadataElement cur = start; cur != null; cur = cur.getParentElement()) {
            if (hasDumpLines(cur)) return cur;
            if ("Metadata_Dump".equals(cur.getName())) break; // don't bubble above the wrapper
        }
        return null;
    }



}
