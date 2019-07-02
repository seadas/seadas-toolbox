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
        category = "Processing", id = "gov.nasa.gsfc.seadas.processing.ui.L2MapGenAction"
)
@ActionRegistration(
        displayName = "#CTL_ L2MapGenAction_Name",
        popupText = "#CTL_ L2MapGenAction_Name"
)
@ActionReference(
        path = "Menu/SeaDAS OCSSW",
        position = 140
)
@NbBundle.Messages({
        "CTL_L2MapGenAction_Name=l2mapgen...",
        "CTL_L2MapGenAction_ProgramName=l2mapgen",
        "CTL_L2MapGenAction_DialogTitle=l2mapgen",
        "CTL_L2MapGenAction_XMLFileName=l2mapgen.xml",
        "CTL_L2MapGenAction_Description=Process MODIS L0 to L1A."
})


public class L2MapGenAction extends CallCloProgramAction implements ContextAwareAction, LookupListener {

    private final Lookup lkp;

    public L2MapGenAction() {
        this(Utilities.actionsGlobalContext());
    }

    public L2MapGenAction(Lookup lkp) {
        this.lkp = lkp;
        Lookup.Result<ProductNode> lkpContext = lkp.lookupResult(ProductNode.class);
        lkpContext.addLookupListener(WeakListeners.create(LookupListener.class, this, lkpContext));
        putValue(Action.NAME, Bundle.CTL_L2MapGenAction_Name());
        putValue(Action.SHORT_DESCRIPTION, Bundle.CTL_L2MapGenAction_Description());
        setProgramName(Bundle.CTL_L2MapGenAction_ProgramName());
        setDialogTitle(Bundle.CTL_L2MapGenAction_DialogTitle());
        setXmlFileName(Bundle.CTL_L2MapGenAction_XMLFileName());
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
        return new L2MapGenAction(actionContext);
    }
}