package gov.nasa.gsfc.seadas.processing.ui;

import gov.nasa.gsfc.seadas.processing.processor.MultilevelProcessorAction;
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
        category = "Processing", id = "gov.nasa.gsfc.seadas.processing.ui.MultiLevelProcessorAction"
)
@ActionRegistration(
        displayName = "#CTL_ MultiLevelProcessorAction_Name",
        popupText = "#CTL_ MultiLevelProcessorAction_Name"
)
@ActionReference(
        path = "Menu/SeaDAS OCSSW",
        position = 220
)
@NbBundle.Messages({
        "CTL_MultiLevelProcessorAction_Name=multilevel_processor...",
        "CTL_MultiLevelProcessorAction_ProgramName=multilevel_processor.py",
        "CTL_MultiLevelProcessorAction_DialogTitle=multilevel_processor.py",
        "CTL_MultiLevelProcessorAction_XMLFileName=multilevel_processor.xml",
        "CTL_MultiLevelProcessorAction_Description=Process a file through many levels."
})

public class MultiLevelProcessorAction extends MultilevelProcessorAction implements ContextAwareAction, LookupListener {

    private final Lookup lkp;

    public  MultiLevelProcessorAction() {
        this(Utilities.actionsGlobalContext());
    }

    public   MultiLevelProcessorAction(Lookup lkp) {
        this.lkp = lkp;
        Lookup.Result<ProductNode> lkpContext = lkp.lookupResult(ProductNode.class);
        lkpContext.addLookupListener(WeakListeners.create(LookupListener.class, this, lkpContext));
        putValue(Action.NAME, Bundle.CTL_MultiLevelProcessorAction_Name());
        putValue(Action.SHORT_DESCRIPTION, Bundle.CTL_MultiLevelProcessorAction_Description());
        setProgramName(Bundle.CTL_MultiLevelProcessorAction_ProgramName());
        setDialogTitle(Bundle.CTL_MultiLevelProcessorAction_DialogTitle());
        setXmlFileName(Bundle.CTL_MultiLevelProcessorAction_XMLFileName());
    }

    @Override
    public void resultChanged(LookupEvent lookupEvent) {

    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new MultiLevelProcessorAction(actionContext);
    }
}

