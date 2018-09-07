package gov.nasa.gsfc.seadas.processing.ui;
import gov.nasa.gsfc.seadas.processing.common.CallCloProgramAction;
import org.esa.snap.core.datamodel.ProductNode;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.*;

import javax.swing.*;

/**
 * @author Aynur Abdurazik
 * @since SeaDAS 8.0
 * @see
 */
@ActionID(
        category = "Processing", id = "gov.nasa.gsfc.seadas.processing.ui.ExtractorAction"
)
@ActionRegistration(
        displayName = "#CTL_ ExtractorAction_Name",
        popupText = "#CTL_ ExtractorAction_Name"
)
@ActionReference(
        path = "Menu/OC Processing",
        position = 20
)
@NbBundle.Messages({
        "CTL_ExtractorAction_Name=extractor...",
        "CTL_ExtractorAction_ProgramName=extractor",
        "CTL_ExtractorAction_DialogTitle=extractor",
        "CTL_ExtractorAction_XMLFileName=extractor.xml",
        "CTL_ExtractorAction_Description=Create L1 and L2 Extracts."
})
public class ExtractorAction extends CallCloProgramAction implements ContextAwareAction, LookupListener {

    private final Lookup lkp;

    public  ExtractorAction() {
        this(Utilities.actionsGlobalContext());
    }

    public   ExtractorAction(Lookup lkp) {
        this.lkp = lkp;
        Lookup.Result<ProductNode> lkpContext = lkp.lookupResult(ProductNode.class);
        lkpContext.addLookupListener(WeakListeners.create(LookupListener.class, this, lkpContext));
        putValue(Action.NAME, Bundle.CTL_ExtractorAction_Name());
        putValue(Action.SHORT_DESCRIPTION, Bundle.CTL_ExtractorAction_Description());
        setProgramName(Bundle.CTL_ExtractorAction_ProgramName());
        setDialogTitle(Bundle.CTL_ExtractorAction_DialogTitle());
        setXmlFileName(Bundle.CTL_ExtractorAction_XMLFileName());
    }
//
//    @Override
//    public void actionPerformed(ActionEvent e) {
//
//    }

    @Override
    public void resultChanged(LookupEvent lookupEvent) {

    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new ExtractorAction(actionContext);
    }
}