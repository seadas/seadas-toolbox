package gov.nasa.gsfc.seadas.panoply.ui;

import org.esa.snap.core.datamodel.MetadataElement;
import org.openide.awt.ActionID;
import org.openide.awt.ActionRegistration;
import org.openide.awt.ActionReference;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.WindowManager;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;

@ActionID(category = "Panoply", id = "gov.nasa.gsfc.seadas.panoply.ui.ShowPanoplyDumpAction")
@ActionRegistration(displayName = "#CTL_ShowPanoplyDump")
@ActionReference(path = "Editors/Popup", position = 19310, separatorBefore = 19305)
@Messages("CTL_ShowPanoplyDump=Show Panoply Dump")
public final class ShowPanoplyDumpAction extends AbstractAction {

    private final Lookup context;

    public ShowPanoplyDumpAction(Lookup context) {
        super(Bundle.CTL_ShowPanoplyDump());
        this.context = context;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Only enable when a Panoply Geophysical_Data element (variable) is selected
        MetadataElement el = context.lookup(MetadataElement.class);
        if (el == null) return;

        PanoplyDumpTopComponent tc = (PanoplyDumpTopComponent)
                WindowManager.getDefault().findTopComponent("PanoplyDumpTopComponent");
        if (tc != null) {
            tc.open();
            tc.requestActive();
        }
    }
}

