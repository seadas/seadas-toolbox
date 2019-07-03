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
        category = "Processing", id = "gov.nasa.gsfc.seadas.processing.ui.SmiGenAction"
)
@ActionRegistration(
        displayName = "#CTL_ SmiGenAction_Name",
        popupText = "#CTL_ SmiGenAction_Name"
)
@ActionReference(
        path = "Menu/SeaDAS-OCSSW",
        position = 200
)
@NbBundle.Messages({
        "CTL_SmiGenAction_Name=smigen...",
        "CTL_SmiGenAction_ProgramName=smigen",
        "CTL_SmiGenAction_DialogTitle=smigen",
        "CTL_SmiGenAction_XMLFileName=smigen.xml",
        "CTL_SmiGenAction_Description=Create a L3 map file."
})

public class SmiGenAction extends CallCloProgramAction implements ContextAwareAction, LookupListener {

    private final Lookup lkp;

    public  SmiGenAction() {
        this(Utilities.actionsGlobalContext());
    }

    public   SmiGenAction(Lookup lkp) {
        this.lkp = lkp;
        Lookup.Result<ProductNode> lkpContext = lkp.lookupResult(ProductNode.class);
        lkpContext.addLookupListener(WeakListeners.create(LookupListener.class, this, lkpContext));
        putValue(Action.NAME, Bundle.CTL_SmiGenAction_Name());
        putValue(Action.SHORT_DESCRIPTION, Bundle.CTL_SmiGenAction_Description());
        setProgramName(Bundle.CTL_SmiGenAction_ProgramName());
        setDialogTitle(Bundle.CTL_SmiGenAction_DialogTitle());
        setXmlFileName(Bundle.CTL_SmiGenAction_XMLFileName());
    }

    @Override
    public void resultChanged(LookupEvent lookupEvent) {

    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new SmiGenAction(actionContext);
    }
}