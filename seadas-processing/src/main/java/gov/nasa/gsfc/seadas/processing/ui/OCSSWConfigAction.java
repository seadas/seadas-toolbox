package gov.nasa.gsfc.seadas.processing.ui;

import gov.nasa.gsfc.seadas.processing.ocssw.OCSSWInfoGUI;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.esa.snap.ui.AppContext;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

@ActionID(
        category = "Processing", id = "gov.nasa.gsfc.seadas.processing.ui.OCSSWConfigAction"
)
@ActionRegistration(
        displayName = "#CTL_OCSSWConfigAction_Name",
        popupText = "#CTL_OCSSWConfigAction_Name"
)
@ActionReference(
        path = "Menu/SeaDAS-OCSSW",
        position = 250
)
@NbBundle.Messages({
        "CTL_OCSSWConfigAction_Name=OCSSW Configuration",
        "CTL_OCSSWConfigAction_DialogTitle=OCSSW Configuration",
        "CTL_OCSSWConfigAction_Description= Set values for seadas.config properties (variables)."
})
public class OCSSWConfigAction extends AbstractSnapAction implements ContextAwareAction, LookupListener {


    private final Lookup lkp;

    public OCSSWConfigAction() {
        this(Utilities.actionsGlobalContext());
    }

    public OCSSWConfigAction(Lookup lkp) {
        this.lkp = lkp;
        Lookup.Result<ProductNode> lkpContext = lkp.lookupResult(ProductNode.class);
        lkpContext.addLookupListener(WeakListeners.create(LookupListener.class, this, lkpContext));
        putValue(Action.NAME, Bundle.CTL_OCSSWConfigAction_Name());
        putValue(Action.SHORT_DESCRIPTION, Bundle.CTL_OCSSWConfigAction_Description());
    }

    @Override
    public void resultChanged(LookupEvent lookupEvent) {

    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new OCSSWConfigAction(actionContext);
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        final AppContext appContext = getAppContext();

        final Window parent = appContext.getApplicationWindow();
        OCSSWInfoGUI ocsswInfoGUI = new OCSSWInfoGUI();
        ocsswInfoGUI.init(parent);
    }
}
