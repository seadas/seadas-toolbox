package gov.nasa.gsfc.seadas.processing.ui;

import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import gov.nasa.gsfc.seadas.processing.ocssw.GetSysInfoGUI;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.esa.snap.ui.AppContext;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.*;
import org.openide.util.actions.Presenter;

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
        "CTL_GetSysInfoAction_Name=Software & System Info...",
        "CTL_GetSysInfoAction_DialogTitle=Software & System Information",
        "CTL_GetSysInfoAction_Description=Print SeaDAS and system info for trouble shooting."
})
public class GetSysInfoAction extends AbstractSnapAction implements ContextAwareAction, LookupListener, Presenter.Menu {

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


        ProgressMonitorSwingWorker pmSwingWorker = new ProgressMonitorSwingWorker(SnapApp.getDefault().getMainFrame(),
                "Retrieving SeaDAS processor and system configuration") {

            @Override
            protected Void doInBackground(com.bc.ceres.core.ProgressMonitor pm) throws Exception {

                int totalWork = 10;
                int workDone = 0;
                pm.beginTask("Retrieving SeaDAS processor and system configuration", totalWork);

                try {
                    GetSysInfoGUI getSysInfoGUI = new GetSysInfoGUI();
                    getSysInfoGUI.init(parent, pm);
                } finally {
                    pm.done();
                }
                return null;
            }
        };

        pmSwingWorker.executeWithBlocking();

    }

    @Override
    public JMenuItem getMenuPresenter() {
        JMenuItem menuItem = new JMenuItem(this);
        menuItem.setIcon(null);
        menuItem.setName((String) getValue(Action.NAME));
        menuItem.setToolTipText((String) getValue(Action.SHORT_DESCRIPTION));
        return menuItem;
    }
}