package gov.nasa.gsfc.seadas.processing.ui;

import gov.nasa.gsfc.seadas.processing.common.CallCloProgramAction;
import org.esa.snap.core.datamodel.ProductNode;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.*;

import javax.swing.*;

/**
 * @author Daniel Knowles
 * @since SeaDAS 8.0
 * @see
 */
@ActionID(
        category = "Processing", id = "gov.nasa.gsfc.seadas.processing.ui.L2MergeAction"
)
@ActionRegistration(
        displayName = "#CTL_ L2MergeAction_Name",
        popupText = "#CTL_ L2MergeAction_Name"
)
@ActionReference(
        path = "Menu/SeaDAS-Toolbox/SeaDAS Processors/Tools",
        position = 150
)
@NbBundle.Messages({
        "CTL_L2MergeAction_Name=l2merge...",
        "CTL_L2MergeAction_ProgramName=l2merge",
        "CTL_L2MergeAction_DialogTitle=l2merge",
        "CTL_L2MergeAction_XMLFileName=l2merge.xml",
        "CTL_L2MergeAction_Description=Merges multiple L2 input files into a single L2 output file"
})

public class L2MergeAction extends CallCloProgramAction implements ContextAwareAction, LookupListener {

    private final Lookup lkp;

    public L2MergeAction() {
        this(Utilities.actionsGlobalContext());
    }

    public L2MergeAction(Lookup lkp) {
        this.lkp = lkp;
        Lookup.Result<ProductNode> lkpContext = lkp.lookupResult(ProductNode.class);
        lkpContext.addLookupListener(WeakListeners.create(LookupListener.class, this, lkpContext));
        putValue(Action.NAME, Bundle.CTL_L2MergeAction_Name());
        putValue(Action.SHORT_DESCRIPTION, Bundle.CTL_L2MergeAction_Description());
        setProgramName(Bundle.CTL_L2MergeAction_ProgramName());
        setDialogTitle(Bundle.CTL_L2MergeAction_DialogTitle());
        setXmlFileName(Bundle.CTL_L2MergeAction_XMLFileName());
    }

    @Override
    public void resultChanged(LookupEvent lookupEvent) {

    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new L2MergeAction(actionContext);
    }
}
