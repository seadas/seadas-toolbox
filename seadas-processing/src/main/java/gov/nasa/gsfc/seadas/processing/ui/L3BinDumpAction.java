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
        category = "Processing", id = "gov.nasa.gsfc.seadas.processing.ui.L3BinDumpAction"
)
@ActionRegistration(
        displayName = "#CTL_ L3BinDumpAction_Name",
        popupText = "#CTL_ L3BinDumpAction_Name"
)
@ActionReference(
        path = "Menu/SeaDAS-OCSSW",
        position = 210
)
@NbBundle.Messages({
        "CTL_L3BinDumpAction_Name=l3bindump...",
        "CTL_L3BinDumpAction_ProgramName=l3bindump",
        "CTL_L3BinDumpAction_DialogTitle=l3bindump",
        "CTL_L3BinDumpAction_XMLFileName=l3bindump.xml",
        "CTL_L3BinDumpAction_Description=Create a L3 Mapped Image."
})

public class L3BinDumpAction extends CallCloProgramAction implements ContextAwareAction, LookupListener {

    private final Lookup lkp;

    public L3BinDumpAction() {
        this(Utilities.actionsGlobalContext());
    }

    public L3BinDumpAction(Lookup lkp) {
        this.lkp = lkp;
        Lookup.Result<ProductNode> lkpContext = lkp.lookupResult(ProductNode.class);
        lkpContext.addLookupListener(WeakListeners.create(LookupListener.class, this, lkpContext));
        putValue(Action.NAME, Bundle.CTL_L3BinDumpAction_Name());
        putValue(Action.SHORT_DESCRIPTION, Bundle.CTL_L3BinDumpAction_Description());
        setProgramName(Bundle.CTL_L3BinDumpAction_ProgramName());
        setDialogTitle(Bundle.CTL_L3BinDumpAction_DialogTitle());
        setXmlFileName(Bundle.CTL_L3BinDumpAction_XMLFileName());
    }

    @Override
    public void resultChanged(LookupEvent lookupEvent) {

    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new L3BinDumpAction(actionContext);
    }
}