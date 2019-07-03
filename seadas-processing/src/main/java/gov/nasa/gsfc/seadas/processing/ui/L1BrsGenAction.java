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
        category = "Processing", id = "gov.nasa.gsfc.seadas.processing.ui.L1BrsGenAction"
)
@ActionRegistration(
        displayName = "#CTL_ L1BrsGenAction_Name",
        popupText = "#CTL_ L1BrsGenAction_Name"
)
@ActionReference(
        path = "Menu/SeaDAS-OCSSW",
        position = 90
)
@NbBundle.Messages({
        "CTL_L1BrsGenAction_Name=l1brsgen...",
        "CTL_L1BrsGenAction_ProgramName=l1brsgen",
        "CTL_L1BrsGenAction_DialogTitle=l1brsgen",
        "CTL_L1BrsGenAction_XMLFileName=l1brsgen.xml",
        "CTL_L1BrsGenAction_Description=Process MODIS L0 to L1A."
})

public class L1BrsGenAction extends CallCloProgramAction implements ContextAwareAction, LookupListener {

    private final Lookup lkp;

    public  L1BrsGenAction() {
        this(Utilities.actionsGlobalContext());
    }

    public   L1BrsGenAction(Lookup lkp) {
        this.lkp = lkp;
        Lookup.Result<ProductNode> lkpContext = lkp.lookupResult(ProductNode.class);
        lkpContext.addLookupListener(WeakListeners.create(LookupListener.class, this, lkpContext));
        putValue(Action.NAME, Bundle.CTL_L1BrsGenAction_Name());
        putValue(Action.SHORT_DESCRIPTION, Bundle.CTL_L1BrsGenAction_Description());
        setProgramName(Bundle.CTL_L1BrsGenAction_ProgramName());
        setDialogTitle(Bundle.CTL_L1BrsGenAction_DialogTitle());
        setXmlFileName(Bundle.CTL_L1BrsGenAction_XMLFileName());
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
        return new L1BrsGenAction(actionContext);
    }
}
