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
        category = "Processing", id = "gov.nasa.gsfc.seadas.processing.ui.OCSSWInstallerAction"
)
@ActionRegistration(
        displayName = "#CTL_OCSSWInstallerAction_Name",
        popupText = "#CTL_OCSSWInstallerAction_Name"
)
@ActionReference(
        path = "Menu/OC Processing",
        position = 10
)
@NbBundle.Messages({
        "CTL_OCSSWInstallerAction_Name=Install/Update OC Processors",
        "CTL_OCSSWInstallerAction_ProgramName=install_ocssw.py",
        "CTL_OCSSWInstallerAction_DialogTitle=Install/Update OCSSW",
        "CTL_OCSSWInstallerAction_XMLFileName=ocssw_installer.xml",
        "CTL_OCSSWInstallerAction_Description=Install/Updated OCSSW package."
})

public class OCSSWInstallerAction extends CallCloProgramAction implements ContextAwareAction, LookupListener {

    private final Lookup lkp;

    public OCSSWInstallerAction() {
        this(Utilities.actionsGlobalContext());
    }

    public  OCSSWInstallerAction(Lookup lkp) {
        this.lkp = lkp;
        Lookup.Result<ProductNode> lkpContext = lkp.lookupResult(ProductNode.class);
        lkpContext.addLookupListener(WeakListeners.create(LookupListener.class, this, lkpContext));
        putValue(Action.NAME, Bundle.CTL_OCSSWInstallerAction_Name());
        putValue(Action.SHORT_DESCRIPTION, Bundle.CTL_OCSSWInstallerAction_Description());
        setProgramName(Bundle.CTL_OCSSWInstallerAction_ProgramName());
        setDialogTitle(Bundle.CTL_OCSSWInstallerAction_DialogTitle());
        setXmlFileName(Bundle.CTL_OCSSWInstallerAction_XMLFileName());
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
        return new OCSSWInstallerAction(actionContext);
    }
}
