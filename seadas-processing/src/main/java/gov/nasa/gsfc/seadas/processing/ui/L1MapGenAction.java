package gov.nasa.gsfc.seadas.processing.ui;

import gov.nasa.gsfc.seadas.processing.common.CallCloProgramAction;
import org.esa.snap.core.datamodel.ProductNode;
import org.openide.awt.ActionID;
import org.openide.awt.ActionRegistration;
import org.openide.util.*;

import javax.swing.*;

/**
 * @author Aynur Abdurazik
 * @since SeaDAS 8.0
 * @see
 */
@ActionID(
        category = "Processing", id = "gov.nasa.gsfc.seadas.processing.ui.L1MapGenAction"
)
@ActionRegistration(
        displayName = "#CTL_ L1MapGenAction_Name",
        popupText = "#CTL_ L1MapGenAction_Name"
)
//@ActionReference(
//        path = "Menu/SeaDAS-OCSSW",
//        position = 100
//)
@NbBundle.Messages({
        "CTL_L1MapGenAction_Name=l1mapgen...",
        "CTL_L1MapGenAction_ProgramName=l1mapgen",
        "CTL_L1MapGenAction_DialogTitle=l1mapgen",
        "CTL_L1MapGenAction_XMLFileName=l1mapgen.xml",
        "CTL_L1MapGenAction_Description=Process MODIS L0 to L1A."
})
public class L1MapGenAction extends CallCloProgramAction implements ContextAwareAction, LookupListener {

    private final Lookup lkp;

    public  L1MapGenAction() {
        this(Utilities.actionsGlobalContext());
    }

    public   L1MapGenAction(Lookup lkp) {
        this.lkp = lkp;
        Lookup.Result<ProductNode> lkpContext = lkp.lookupResult(ProductNode.class);
        lkpContext.addLookupListener(WeakListeners.create(LookupListener.class, this, lkpContext));
        putValue(Action.NAME, Bundle.CTL_L1MapGenAction_Name());
        putValue(Action.SHORT_DESCRIPTION, Bundle.CTL_L1MapGenAction_Description());
        setProgramName(Bundle.CTL_L1MapGenAction_ProgramName());
        setDialogTitle(Bundle.CTL_L1MapGenAction_DialogTitle());
        setXmlFileName(Bundle.CTL_L1MapGenAction_XMLFileName());
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
        return new L1MapGenAction(actionContext);
    }
}
