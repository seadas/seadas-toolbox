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
@ActionID(category = "Processing", id = "gov.nasa.gsfc.seadas.processing.OCSSWInstallerAction")
@ActionRegistration(
        displayName = "#CTL_OCSSWInstallerAction_Name",
        popupText = "#CTL_OCSSWInstallerAction_Name"
)
@ActionReference(
        path = "Menu/SeaDAS-Toolbox",
        position = 10,
        separatorAfter = 11
)
@NbBundle.Messages({
        "CTL_OCSSWInstallerAction_Name=Install/Update SeaDAS Processors...",
        "CTL_OCSSWInstallerAction_ProgramName=install_ocssw",
        "CTL_OCSSWInstallerAction_DialogTitle=Install/Update SeaDAS Processors",
        "CTL_OCSSWInstallerAction_XMLFileName=ocssw_installer.xml",
        "CTL_OCSSWInstallerAction_Description=Install/Update SeaDAS processors and supported missions"
})

public class OCSSWInstallerAction extends CallCloProgramAction

        implements  LookupListener {  //ContextAwareAction

   private final Lookup lookup;

    public static final String HELP_ID = "install_ocssw";
    private HelpCtx helpCtx;

    public OCSSWInstallerAction() {
        this(Utilities.actionsGlobalContext());
        helpCtx = new HelpCtx(getProgramName());
    }


    public  OCSSWInstallerAction(Lookup lookup) {
        this.lookup = lookup != null ? lookup : Utilities.actionsGlobalContext();
        Lookup.Result<ProductNode> lkpContext = lookup.lookupResult(ProductNode.class);
        lkpContext.addLookupListener(WeakListeners.create(LookupListener.class, this, lkpContext));
        putValue(Action.NAME, Bundle.CTL_OCSSWInstallerAction_Name());
        putValue(Action.SHORT_DESCRIPTION, Bundle.CTL_OCSSWInstallerAction_Description());
        setProgramName(Bundle.CTL_OCSSWInstallerAction_ProgramName());
        setDialogTitle(Bundle.CTL_OCSSWInstallerAction_DialogTitle());
        setXmlFileName(Bundle.CTL_OCSSWInstallerAction_XMLFileName());
        setHelpId(getProgramName());
    }

//    @Override
//    public void actionPerformed(ActionEvent e) {
//
//    }

    @Override
    public HelpCtx getHelpCtx() {
        return helpCtx;
    }

    @Override
    public void resultChanged(LookupEvent lookupEvent) {

    }

//    @Override
    /**
     * for ContextAwareAction interface
     */
//    public Action createContextAwareInstance(Lookup actionContext) {
//        return new OCSSWInstallerAction(actionContext);
//    }
}
