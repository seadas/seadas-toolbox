package gov.nasa.gsfc.seadas.processing.ui;

import gov.nasa.gsfc.seadas.processing.l2gen.userInterface.L2genAction;
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
//
@ActionID(
        category = "Processing", id = "gov.nasa.gsfc.seadas.processing.ui.L2GenSnapAction"
)
@ActionRegistration(
        displayName = "#CTL_ L2GenAction_Name",
        popupText = "#CTL_ L2GenAction_Name"
)
@ActionReference(
        path = "Menu/SeaDAS-Toolbox/SeaDAS Processors",
        position = 111,
        separatorBefore = 110
)
@NbBundle.Messages({
        "CTL_L2GenAction_Name=l2gen...",
        "CTL_L2GenAction_ProgramName=l2gen",
        "CTL_L2GenAction_DialogTitle=l2gen",
        "CTL_L2GenAction_XMLFileName=l2gen.xml",
        "CTL_L2GenAction_Description=Creates a L2 file from an input L1 file (and input GEO file depending on mission)"
})

public class L2GenSnapAction extends L2genAction implements ContextAwareAction, LookupListener {

    private final Lookup lkp;

    public L2GenSnapAction() {
        this(Utilities.actionsGlobalContext());
    }

    /**
     *
     * @param lkp
     */
    public L2GenSnapAction(Lookup lkp) {
        this.lkp = lkp;
        Lookup.Result<ProductNode> lkpContext = lkp.lookupResult(ProductNode.class);
        lkpContext.addLookupListener(WeakListeners.create(LookupListener.class, this, lkpContext));
        putValue(Action.NAME, Bundle.CTL_L2GenAction_Name());
        putValue(Action.SHORT_DESCRIPTION, Bundle.CTL_L2GenAction_Description());
        setProgramName(Bundle.CTL_L2GenAction_ProgramName());
        setDialogTitle(Bundle.CTL_L2GenAction_DialogTitle());
        setXmlFileName(Bundle.CTL_L2GenAction_XMLFileName());
    }
    /**
     * @param lookupEvent
     * @
     */
    @Override
    public void resultChanged(LookupEvent lookupEvent) {

    }

    /**
     *
     * @param actionContext
     * @return
     */
    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new L2GenSnapAction(actionContext);
    }
}