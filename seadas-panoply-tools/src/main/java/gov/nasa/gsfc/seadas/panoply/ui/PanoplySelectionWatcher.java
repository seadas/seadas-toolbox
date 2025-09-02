package gov.nasa.gsfc.seadas.panoply.ui;

import org.esa.snap.core.datamodel.MetadataElement;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.Utilities;
import org.openide.windows.WindowManager;

public final class PanoplySelectionWatcher implements LookupListener {
    private org.openide.util.Lookup.Result<MetadataElement> result;

    public void start() {
        result = Utilities.actionsGlobalContext().lookupResult(MetadataElement.class);
        result.addLookupListener(this);
        resultChanged(null);
    }
    public void stop() {
        if (result != null) { result.removeLookupListener(this); result = null; }
    }

    @Override public void resultChanged(LookupEvent ev) {
        MetadataElement el = Utilities.actionsGlobalContext().lookup(MetadataElement.class);
        if (!isPanoplyVar(el)) return;
        var tc = (PanoplyDumpTopComponent) WindowManager.getDefault().findTopComponent("PanoplyDumpTopComponent");
        if (tc != null) { tc.open(); tc.requestActive(); }
    }

    private static boolean isPanoplyVar(MetadataElement el) {
        if (el == null) return false;
        var parent = el.getParentElement();
        var grand  = (parent != null) ? parent.getParentElement() : null;
        return parent != null && grand != null &&
                "Geophysical_Data".equals(parent.getName()) &&
                "Panoply".equals(grand.getName());
    }
}
