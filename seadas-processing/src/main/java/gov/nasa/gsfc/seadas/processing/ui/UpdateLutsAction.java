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
        category = "Processing", id = "gov.nasa.gsfc.seadas.processing.ui.UpdateLutsAction"
)
@ActionRegistration(
        displayName = "#CTL_UpdateLutsAction_Name",
        popupText = "#CTL_UpdateLutsAction_Name"
)
@ActionReference(
        path = "Menu/SeaDAS-OCSSW",
        position = 240
)
@NbBundle.Messages({
        "CTL_UpdateLutsAction_Name=update_luts.py...",
        "CTL_UpdateLutsAction_ProgramName=update_luts.py",
        "CTL_UpdateLutsAction_DialogTitle=update_luts.py",
        "CTL_UpdateLutsAction_XMLFileName=update_luts.xml",
        "CTL_UpdateLutsAction_Description=Retrieve latest lookup tables for specified sensor."
})
public class UpdateLutsAction extends CallCloProgramAction implements ContextAwareAction, LookupListener {

    private final Lookup lkp;
    public static final String HELP_ID = "update_luts.py";
    private HelpCtx helpCtx;

    public  UpdateLutsAction() {
        this(Utilities.actionsGlobalContext());
        helpCtx = new HelpCtx(HELP_ID);
    }

    public  UpdateLutsAction(Lookup lkp) {
        this.lkp = lkp;
        Lookup.Result<ProductNode> lkpContext = lkp.lookupResult(ProductNode.class);
        lkpContext.addLookupListener(WeakListeners.create(LookupListener.class, this, lkpContext));
        putValue(Action.NAME, Bundle.CTL_UpdateLutsAction_Name());
        putValue(Action.SHORT_DESCRIPTION, Bundle.CTL_UpdateLutsAction_Description());
        setProgramName(Bundle.CTL_UpdateLutsAction_ProgramName());
        setDialogTitle(Bundle.CTL_UpdateLutsAction_DialogTitle());
        setXmlFileName(Bundle.CTL_UpdateLutsAction_XMLFileName());
    }

    @Override
    public void resultChanged(LookupEvent lookupEvent) {

    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new UpdateLutsAction(actionContext);
    }
}
