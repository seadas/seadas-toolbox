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
        category = "Processing", id = "gov.nasa.gsfc.seadas.processing.ui.L3BinAction"
)
@ActionRegistration(
        displayName = "#CTL_ L3BinAction_Name",
        popupText = "#CTL_ L3BinAction_Name"
)
@ActionReference(
        path = "Menu/SeaDAS-Toolbox/SeaDAS Processors",
        position = 170
)
@NbBundle.Messages({
        "CTL_L3BinAction_Name=l3bin...",
        "CTL_L3BinAction_ProgramName=l3bin",
        "CTL_L3BinAction_DialogTitle=l3bin",
        "CTL_L3BinAction_XMLFileName=l3bin.xml",
        "CTL_L3BinAction_Description=Create a L3 bin file from L3 file(s)."
})

public class L3BinAction  extends CallCloProgramAction implements ContextAwareAction, LookupListener {

    private final Lookup lkp;
    private HelpCtx helpCtx;

    public  L3BinAction() {
        this(Utilities.actionsGlobalContext());
        helpCtx = new HelpCtx(getProgramName());
    }

    public   L3BinAction(Lookup lkp) {
        this.lkp = lkp;
        Lookup.Result<ProductNode> lkpContext = lkp.lookupResult(ProductNode.class);
        lkpContext.addLookupListener(WeakListeners.create(LookupListener.class, this, lkpContext));
        putValue(Action.NAME, Bundle.CTL_L3BinAction_Name());
        putValue(Action.SHORT_DESCRIPTION, Bundle.CTL_L3BinAction_Description());
        setProgramName(Bundle.CTL_L3BinAction_ProgramName());
        setDialogTitle(Bundle.CTL_L3BinAction_DialogTitle());
        setXmlFileName(Bundle.CTL_L3BinAction_XMLFileName());
        setHelpId(getProgramName());
    }
//
//    @Override
//    public void actionPerformed(ActionEvent e) {
//
//    }


    @Override
    public HelpCtx getHelpCtx() {
        return helpCtx;
    }

    @Override
    public void resultChanged(LookupEvent lookupEvent) {

    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new L3BinAction(actionContext);
    }
}
