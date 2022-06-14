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
        category = "Processing", id = "gov.nasa.gsfc.seadas.processing.ui.ModisL1BAction"
)
@ActionRegistration(
        displayName = "#CTL_ ModisL1BAction_Name",
        popupText = "#CTL_ ModisL1BAction_Name"
)
@ActionReference(
        path = "Menu/SeaDAS-Toolbox/SeaDAS Processors/MODIS",
        position = 50
)
@NbBundle.Messages({
        "CTL_ModisL1BAction_Name=modis_L1B...",
        "CTL_ModisL1BAction_ProgramName=modis_L1B",
        "CTL_ModisL1BAction_DialogTitle=modis_L1B",
        "CTL_ModisL1BAction_XMLFileName=modis_L1B.xml",
        "CTL_ModisL1BAction_Description=Process MODIS L0 to L1B."
})
public class ModisL1BAction extends CallCloProgramAction implements ContextAwareAction, LookupListener {

    private final Lookup lkp;

    public  ModisL1BAction() {
        this(Utilities.actionsGlobalContext());
    }

    public   ModisL1BAction(Lookup lkp) {
        this.lkp = lkp;
        Lookup.Result<ProductNode> lkpContext = lkp.lookupResult(ProductNode.class);
        lkpContext.addLookupListener(WeakListeners.create(LookupListener.class, this, lkpContext));
        putValue(Action.NAME, Bundle.CTL_ModisL1BAction_Name());
        putValue(Action.SHORT_DESCRIPTION, Bundle.CTL_ModisL1BAction_Description());
        setProgramName(Bundle.CTL_ModisL1BAction_ProgramName());
        setDialogTitle(Bundle.CTL_ModisL1BAction_DialogTitle());
        setXmlFileName(Bundle.CTL_ModisL1BAction_XMLFileName());
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
        return new ModisL1BAction(actionContext);
    }
}