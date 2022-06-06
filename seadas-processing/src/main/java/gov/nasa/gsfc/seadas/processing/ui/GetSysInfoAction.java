package gov.nasa.gsfc.seadas.processing.ui;

import gov.nasa.gsfc.seadas.processing.ocssw.GetSysInfoGUI;
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

/**
 * @author Bing Yang
 * @since SeaDAS 8.0
 * @see
 */
@ActionID(
        category = "Processing", id = "gov.nasa.gsfc.seadas.processing.ui.GetSysInfoAction"
)
@ActionRegistration(
        displayName = "#CTL_GetSysInfoAction_Name",
        popupText = "#CTL_GetSysInfoAction_Description"
)
@ActionReference(
        path = "Menu/SeaDAS-Toolbox",
        position = 1500,
        separatorAfter = 1501)
@NbBundle.Messages({
        "CTL_GetSysInfoAction_Name=SeaDAS/System Info...",
        "CTL_GetSysInfoAction_DialogTitle=SeaDAS/System Information",
        "CTL_GetSysInfoAction_Description=Print SeaDAS and OCSSW info for trouble shooting."
})
public class GetSysInfoAction extends AbstractSnapAction implements ContextAwareAction, LookupListener {

    private final Lookup lkp;

    public GetSysInfoAction() {
        this(Utilities.actionsGlobalContext());
    }

    public GetSysInfoAction(Lookup lkp) {
        this.lkp = lkp;
        Lookup.Result<ProductNode> lkpContext = lkp.lookupResult(ProductNode.class);
        lkpContext.addLookupListener(WeakListeners.create(LookupListener.class, this, lkpContext));
        putValue(Action.NAME, Bundle.CTL_GetSysInfoAction_Name());
        putValue(Action.SHORT_DESCRIPTION, Bundle.CTL_GetSysInfoAction_Description());
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
        return new GetSysInfoAction(actionContext);
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        final AppContext appContext = getAppContext();

        final Window parent = appContext.getApplicationWindow();
        GetSysInfoGUI getSysInfoGUI = new GetSysInfoGUI();
        getSysInfoGUI.init(parent);
    }
}