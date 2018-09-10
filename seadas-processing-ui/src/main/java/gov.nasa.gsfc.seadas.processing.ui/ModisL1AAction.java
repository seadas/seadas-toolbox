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
        category = "Processing", id = "gov.nasa.gsfc.seadas.processing.ui.ModisL1AAction"
)
@ActionRegistration(
        displayName = "#CTL_ ModisL1AAction_Name",
        popupText = "#CTL_ ModisL1AAction_Name"
)
@ActionReference(
        path = "Menu/OC Processing",
        position = 30
)
@NbBundle.Messages({
        "CTL_ModisL1AAction_Name=modis_L1A...",
        "CTL_ModisL1AAction_ProgramName=modis_L1A.py",
        "CTL_ModisL1AAction_DialogTitle=modis_L1A",
        "CTL_ModisL1AAction_XMLFileName=modis_L1A.xml",
        "CTL_ModisL1AAction_Description=Process MODIS L0 to L1A."
})
public class ModisL1AAction extends CallCloProgramAction implements ContextAwareAction, LookupListener {

    private final Lookup lkp;

    public  ModisL1AAction() {
        this(Utilities.actionsGlobalContext());
    }

    public   ModisL1AAction(Lookup lkp) {
        this.lkp = lkp;
        Lookup.Result<ProductNode> lkpContext = lkp.lookupResult(ProductNode.class);
        lkpContext.addLookupListener(WeakListeners.create(LookupListener.class, this, lkpContext));
        putValue(Action.NAME, Bundle.CTL_ModisL1AAction_Name());
        putValue(Action.SHORT_DESCRIPTION, Bundle.CTL_ModisL1AAction_Description());
        setProgramName(Bundle.CTL_ModisL1AAction_ProgramName());
        setDialogTitle(Bundle.CTL_ModisL1AAction_DialogTitle());
        setXmlFileName(Bundle.CTL_ModisL1AAction_XMLFileName());
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
        return new ModisL1AAction(actionContext);
    }
}