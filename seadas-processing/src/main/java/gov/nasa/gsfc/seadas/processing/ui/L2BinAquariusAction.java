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
        category = "Processing", id = "gov.nasa.gsfc.seadas.processing.ui.L2BinAquariusAction"
)
@ActionRegistration(
        displayName = "#CTL_ L2BinAquariusAction_Name",
        popupText = "#CTL_ L2BinAquariusAction_Name"
)
@ActionReference(
        path = "Menu/SeaDAS OCSSW",
        position = 160
)
@NbBundle.Messages({
        "CTL_L2BinAquariusAction_Name=l2bin_aquarius...",
        "CTL_L2BinAquariusAction_ProgramName=l2bin_aquarius",
        "CTL_L2BinAquariusAction_DialogTitle=l2bin_aquarius",
        "CTL_L2BinAquariusAction_XMLFileName=l2bin_aquarius.xml",
        "CTL_L2BinAquariusAction_Description=Create a L3 bin file from Aquarius L2 file(s)."
})


public class L2BinAquariusAction extends CallCloProgramAction implements ContextAwareAction, LookupListener {

    private final Lookup lkp;

    public  L2BinAquariusAction() {
        this(Utilities.actionsGlobalContext());
    }

    public   L2BinAquariusAction(Lookup lkp) {
        this.lkp = lkp;
        Lookup.Result<ProductNode> lkpContext = lkp.lookupResult(ProductNode.class);
        lkpContext.addLookupListener(WeakListeners.create(LookupListener.class, this, lkpContext));
        putValue(Action.NAME, Bundle.CTL_L2BinAquariusAction_Name());
        putValue(Action.SHORT_DESCRIPTION, Bundle.CTL_L2BinAquariusAction_Description());
        setProgramName(Bundle.CTL_L2BinAquariusAction_ProgramName());
        setDialogTitle(Bundle.CTL_L2BinAquariusAction_DialogTitle());
        setXmlFileName(Bundle.CTL_L2BinAquariusAction_XMLFileName());
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
        return new L2BinAquariusAction(actionContext);
    }
}
