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
        category = "Processing", id = "gov.nasa.gsfc.seadas.processing.ui.L3GenAction"
)
@ActionRegistration(
        displayName = "#CTL_ L3GenAction_Name",
        popupText = "#CTL_ L3GenAction_Name"
)
@ActionReference(
        path = "Menu/OC Processing",
        position = 180
)
@NbBundle.Messages({
        "CTL_L3GenAction_Name=l3gen...",
        "CTL_L3GenAction_ProgramName=l3gen",
        "CTL_L3GenAction_DialogTitle=l3gen",
        "CTL_L3GenAction_XMLFileName=l3gen.xml",
        "CTL_L3GenAction_Description=Process MODIS L0 to L1A."
})

public class L3GenAction  extends CallCloProgramAction implements ContextAwareAction, LookupListener {

    private final Lookup lkp;

    public  L3GenAction() {
        this(Utilities.actionsGlobalContext());
    }

    public   L3GenAction(Lookup lkp) {
        this.lkp = lkp;
        Lookup.Result<ProductNode> lkpContext = lkp.lookupResult(ProductNode.class);
        lkpContext.addLookupListener(WeakListeners.create(LookupListener.class, this, lkpContext));
        putValue(Action.NAME, Bundle.CTL_L3GenAction_Name());
        putValue(Action.SHORT_DESCRIPTION, Bundle.CTL_L3GenAction_Description());
        setProgramName(Bundle.CTL_L3GenAction_ProgramName());
        setDialogTitle(Bundle.CTL_L3GenAction_DialogTitle());
        setXmlFileName(Bundle.CTL_L3GenAction_XMLFileName());
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
        return new L3GenAction(actionContext);
    }
}

