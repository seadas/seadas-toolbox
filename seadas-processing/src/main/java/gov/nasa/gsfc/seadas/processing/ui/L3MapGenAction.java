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
        category = "Processing", id = "gov.nasa.gsfc.seadas.processing.ui.L3MapGenAction"
)
@ActionRegistration(
        displayName = "#CTL_ L3MapGenAction_Name",
        popupText = "#CTL_ L3MapGenAction_Name"
)
@ActionReference(
        path = "Menu/SeaDAS-Toolbox/SeaDAS Processors",
        position = 185)
@NbBundle.Messages({
        "CTL_L3MapGenAction_Name=l3mapgen...",
        "CTL_L3MapGenAction_ProgramName=l3mapgen",
        "CTL_L3MapGenAction_DialogTitle=l3mapgen",
        "CTL_L3MapGenAction_XMLFileName=l3mapgen.xml",
        "CTL_L3MapGenAction_Description=Create a L3 Mapped Image."
})

public class L3MapGenAction extends CallCloProgramAction implements ContextAwareAction, LookupListener {

    private final Lookup lkp;

    public  L3MapGenAction() {
        this(Utilities.actionsGlobalContext());
    }

    public   L3MapGenAction(Lookup lkp) {
        this.lkp = lkp;
        Lookup.Result<ProductNode> lkpContext = lkp.lookupResult(ProductNode.class);
        lkpContext.addLookupListener(WeakListeners.create(LookupListener.class, this, lkpContext));
        putValue(Action.NAME, Bundle.CTL_L3MapGenAction_Name());
        putValue(Action.SHORT_DESCRIPTION, Bundle.CTL_L3MapGenAction_Description());
        setProgramName(Bundle.CTL_L3MapGenAction_ProgramName());
        setDialogTitle(Bundle.CTL_L3MapGenAction_DialogTitle());
        setXmlFileName(Bundle.CTL_L3MapGenAction_XMLFileName());
    }

    @Override
    public void resultChanged(LookupEvent lookupEvent) {

    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new L3MapGenAction(actionContext);
    }
}

