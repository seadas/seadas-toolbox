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
        category = "Processing", id = "gov.nasa.gsfc.seadas.processing.ui.L2GenAquariusAction"
)
@ActionRegistration(
        displayName = "#CTL_ L2GenAquariusAction_Name",
        popupText = "#CTL_ L2GenAquariusAction_Name"
)
@ActionReference(
        path = "Menu/SeaDAS-OCSSW",
        position = 120
)
@NbBundle.Messages({
        "CTL_L2GenAquariusAction_Name=l2gen_aquarius...",
        "CTL_L2GenAquariusAction_ProgramName=l2gen_aquarius",
        "CTL_L2GenAquariusAction_DialogTitle=l2gen_aquarius",
        "CTL_L2GenAquariusAction_XMLFileName=l2gen_aquarius.xml",
        "CTL_L2GenAquariusAction_Description=Process MODIS L0 to L1A."
})

public class L2GenAquariusAction extends CallCloProgramAction implements ContextAwareAction, LookupListener {

    private final Lookup lkp;

    public  L2GenAquariusAction() {
        this(Utilities.actionsGlobalContext());
    }

    public   L2GenAquariusAction(Lookup lkp) {
        this.lkp = lkp;
        Lookup.Result<ProductNode> lkpContext = lkp.lookupResult(ProductNode.class);
        lkpContext.addLookupListener(WeakListeners.create(LookupListener.class, this, lkpContext));
        putValue(Action.NAME, Bundle.CTL_L2GenAquariusAction_Name());
        putValue(Action.SHORT_DESCRIPTION, Bundle.CTL_L2GenAquariusAction_Description());
        setProgramName(Bundle.CTL_L2GenAquariusAction_ProgramName());
        setDialogTitle(Bundle.CTL_L2GenAquariusAction_DialogTitle());
        setXmlFileName(Bundle.CTL_L2GenAquariusAction_XMLFileName());
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
        return new L2GenAquariusAction(actionContext);
    }
}