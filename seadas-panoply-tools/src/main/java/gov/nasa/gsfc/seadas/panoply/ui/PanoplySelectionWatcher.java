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
        if (result != null) {
            result.removeLookupListener(this);
            result = null;
        }
    }

    @Override
    public void resultChanged(LookupEvent ev) {
        MetadataElement el = Utilities.actionsGlobalContext().lookup(MetadataElement.class);
        if (!isPanoplyGeophysicalVar(el)) return;

        var tc = (gov.nasa.gsfc.seadas.panoply.ui.PanoplyDumpTopComponent)
                WindowManager.getDefault().findTopComponent("PanoplyDumpTopComponent");
        if (tc != null) {
            tc.open();
            tc.requestActive();
        }
    }

    private static boolean isPanoplyGeophysicalVar(MetadataElement el) {
        if (el == null) return false;
        var parent = el.getParentElement();
        if (parent == null) return false;
        var grand = parent.getParentElement();
        return "Geophysical_Data".equals(parent.getName()) &&
                grand != null && "Panoply".equals(grand.getName());
    }
}
