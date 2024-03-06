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
        category = "Processing", id = "gov.nasa.gsfc.seadas.processing.ui.L3BinMergeAction"
)
@ActionRegistration(
        displayName = "#CTL_ L3BinMergeAction_Name",
        popupText = "#CTL_ L3BinMergeAction_Name"
)
@ActionReference(
        path = "Menu/SeaDAS-Toolbox/SeaDAS Processors/Tools",
        position = 150
)
@NbBundle.Messages({
        "CTL_L3BinMergeAction_Name=l3binmerge...",
        "CTL_L3BinMergeAction_ProgramName=l3binmerge",
        "CTL_L3BinMergeAction_DialogTitle=l3binmerge",
        "CTL_L3BinMergeAction_XMLFileName=l3binmerge.xml",
        "CTL_L3BinMergeAction_Description=Merges multiple input L3 binned files into a single L3 binned output file"
})

public class L3BinMergeAction extends CallCloProgramAction implements ContextAwareAction, LookupListener {

    private final Lookup lkp;

    public L3BinMergeAction() {
        this(Utilities.actionsGlobalContext());
    }

    public L3BinMergeAction(Lookup lkp) {
        this.lkp = lkp;
        Lookup.Result<ProductNode> lkpContext = lkp.lookupResult(ProductNode.class);
        lkpContext.addLookupListener(WeakListeners.create(LookupListener.class, this, lkpContext));
        putValue(Action.NAME, Bundle.CTL_L3BinMergeAction_Name());
        putValue(Action.SHORT_DESCRIPTION, Bundle.CTL_L3BinMergeAction_Description());
        setProgramName(Bundle.CTL_L3BinMergeAction_ProgramName());
        setDialogTitle(Bundle.CTL_L3BinMergeAction_DialogTitle());
        setXmlFileName(Bundle.CTL_L3BinMergeAction_XMLFileName());
    }

    @Override
    public void resultChanged(LookupEvent lookupEvent) {

    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new L3BinMergeAction(actionContext);
    }
}
