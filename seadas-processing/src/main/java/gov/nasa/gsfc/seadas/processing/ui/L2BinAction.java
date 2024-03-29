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
        category = "Processing", id = "gov.nasa.gsfc.seadas.processing.ui.L2BinAction"
)
@ActionRegistration(
        displayName = "#CTL_ L2BinAction_Name",
        popupText = "#CTL_ L2BinAction_Name"
)
@ActionReference(
        path = "Menu/SeaDAS-Toolbox/SeaDAS Processors",
        position = 150
)
@NbBundle.Messages({
        "CTL_L2BinAction_Name=l2bin...",
        "CTL_L2BinAction_ProgramName=l2bin",
        "CTL_L2BinAction_DialogTitle=l2bin",
        "CTL_L2BinAction_XMLFileName=l2bin.xml",
        "CTL_L2BinAction_Description=Creates a L3 bin file from input L2 file(s)"
})

public class L2BinAction extends CallCloProgramAction implements ContextAwareAction, LookupListener {

    private final Lookup lkp;

    public  L2BinAction() {
        this(Utilities.actionsGlobalContext());
    }

    public   L2BinAction(Lookup lkp) {
        this.lkp = lkp;
        Lookup.Result<ProductNode> lkpContext = lkp.lookupResult(ProductNode.class);
        lkpContext.addLookupListener(WeakListeners.create(LookupListener.class, this, lkpContext));
        putValue(Action.NAME, Bundle.CTL_L2BinAction_Name());
        putValue(Action.SHORT_DESCRIPTION, Bundle.CTL_L2BinAction_Description());
        setProgramName(Bundle.CTL_L2BinAction_ProgramName());
        setDialogTitle(Bundle.CTL_L2BinAction_DialogTitle());
        setXmlFileName(Bundle.CTL_L2BinAction_XMLFileName());
    }

    @Override
    public void resultChanged(LookupEvent lookupEvent) {

    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new L2BinAction(actionContext);
    }
}
