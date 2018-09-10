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
        category = "Processing", id = "gov.nasa.gsfc.seadas.processing.ui.L1BGenAction"
)
@ActionRegistration(
        displayName = "#CTL_ L1BGenAction_Name",
        popupText = "#CTL_ L1BGenAction_Name"
)
@ActionReference(
        path = "Menu/OC Processing",
        position = 80
)
@NbBundle.Messages({
        "CTL_L1BGenAction_Name=l1bgen...",
        "CTL_L1BGenAction_ProgramName=l1bgen",
        "CTL_L1BGenAction_DialogTitle=l1bgen",
        "CTL_L1BGenAction_XMLFileName=l1bgen.xml",
        "CTL_L1BGenAction_Description=Process MODIS L0 to L1A."
})

public class L1BGenAction extends CallCloProgramAction implements ContextAwareAction, LookupListener {

    private final Lookup lkp;

    public  L1BGenAction() {
        this(Utilities.actionsGlobalContext());
    }

    public   L1BGenAction(Lookup lkp) {
        this.lkp = lkp;
        Lookup.Result<ProductNode> lkpContext = lkp.lookupResult(ProductNode.class);
        lkpContext.addLookupListener(WeakListeners.create(LookupListener.class, this, lkpContext));
        putValue(Action.NAME, Bundle.CTL_L1BGenAction_Name());
        putValue(Action.SHORT_DESCRIPTION, Bundle.CTL_L1BGenAction_Description());
        setProgramName(Bundle.CTL_L1BGenAction_ProgramName());
        setDialogTitle(Bundle.CTL_L1BGenAction_DialogTitle());
        setXmlFileName(Bundle.CTL_L1BGenAction_XMLFileName());
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
        return new L1BGenAction(actionContext);
    }
}