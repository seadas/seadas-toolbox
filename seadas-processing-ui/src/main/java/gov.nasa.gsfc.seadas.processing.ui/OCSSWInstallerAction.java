package gov.nasa.gsfc.seadas.processing.ui;

import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.*;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * @author Aynur Abdurazik
 * @since SeaDAS 8.0
 * @see
 */
@ActionID(
        category = "File",
        id = "gov.nasa.gsfc.seadas.processing.ui.OCSSWInstallerAction"
)
@ActionRegistration(
        displayName = "#CTL_OCSSWInstallerActionText",
        popupText = "#CTL_OCSSWInstallerActionText"
)
@ActionReference(
        path = "Menu/OC Processing",
        position = 70
)
@NbBundle.Messages({
        "CTL_OCSSWInstallerActionText=OCSSW Installer",
        "CTL_OCSSWInstallerDialogTitle=Install OCSSW",
        "CTL_OCSSWInstallerDescription=Install OCSSW package."
})

public class OCSSWInstallerAction extends AbstractSnapAction implements ContextAwareAction, LookupListener {

    private final Lookup lkp;

    public OCSSWInstallerAction() {
        this(Utilities.actionsGlobalContext());
    }

    public  OCSSWInstallerAction(Lookup lkp) {
        this.lkp = lkp;
        Lookup.Result<ProductNode> lkpContext = lkp.lookupResult(ProductNode.class);
        lkpContext.addLookupListener(WeakListeners.create(LookupListener.class, this, lkpContext));
        putValue(Action.NAME, Bundle.CTL_OCSSWInstallerActionText());
        putValue(Action.SHORT_DESCRIPTION, Bundle.CTL_OCSSWInstallerDescription());
    }

    @Override
    public void actionPerformed(ActionEvent e) {

    }

    @Override
    public void resultChanged(LookupEvent lookupEvent) {

    }

    @Override
    public Action createContextAwareInstance(Lookup lookup) {
        return null;
    }
}
