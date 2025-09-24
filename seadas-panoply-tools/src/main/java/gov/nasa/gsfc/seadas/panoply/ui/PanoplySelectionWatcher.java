package gov.nasa.gsfc.seadas.panoply.ui;

import org.esa.snap.core.datamodel.MetadataElement;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.Utilities;
import org.openide.windows.WindowManager;

public final class PanoplySelectionWatcher implements LookupListener {
    private org.openide.util.Lookup.Result<MetadataElement> result;

    // Recognize the five sections (flat) and any descendants.
// Also tolerates a wrapper named "Panoply" or "MetadataView" if present.
    private static final java.util.Set<String> SECTION_NAMES = java.util.Set.of(
            "Geophysical_Data","Navigation_Data","Processing_Control",
            "Scan_Line_Attributes","Sensor_Band_Parameters");
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
    private static boolean isPanoplyVar(org.esa.snap.core.datamodel.MetadataElement el) {
        if (el == null) return false;
        for (org.esa.snap.core.datamodel.MetadataElement cur = el;
             cur != null; cur = cur.getParentElement()) {
            String name = cur.getName();
            if (SECTION_NAMES.contains(name)) return true;                 // flat layout
            if ("Panoply".equalsIgnoreCase(name)) return true;             // old wrapper
            if ("MetadataView".equalsIgnoreCase(name)) return true;        // new wrapper
        }
        return false;
    }
}
