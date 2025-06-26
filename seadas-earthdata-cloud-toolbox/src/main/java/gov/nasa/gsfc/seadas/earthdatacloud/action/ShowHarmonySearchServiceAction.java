package gov.nasa.gsfc.seadas.earthdatacloud.action;

import gov.nasa.gsfc.seadas.earthdatacloud.ui.HarmonySearchServiceDiaglog;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;
import org.openide.util.*;
import org.openide.util.actions.Presenter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;


@ActionID(
        category = "View", id = "HarmonySearchServiceAction"
)

@ActionRegistration(
        displayName = "#CTL_HarmonySearchServiceActionName",
        popupText = "#CTL_HarmonySearchServiceActionName"
)

@ActionReferences({
        @ActionReference(path = "Menu/Earthdata-Cloud", position = 40)
})

@NbBundle.Messages({
        "CTL_HarmonySearchServiceActionName=OB_CLOUD Data Browser",
        "CTL_HarmonySearchServiceActionToolTip=Show/hide Search Service"
})

public class ShowHarmonySearchServiceAction extends AbstractSnapAction implements LookupListener, Presenter.Menu, Presenter.Toolbar  {

    Product product;

    private final Lookup lookup;

    public  ShowHarmonySearchServiceAction() {
        this(null);
    }

    public   ShowHarmonySearchServiceAction(Lookup lookup) {
        putValue(ACTION_COMMAND_KEY, getClass().getName());
        putValue(SELECTED_KEY, false);
        putValue(NAME, Bundle.CTL_HarmonySearchServiceActionName());
        putValue(SHORT_DESCRIPTION, Bundle.CTL_HarmonySearchServiceActionToolTip());
        this.lookup = lookup != null ? lookup : Utilities.actionsGlobalContext();
        updateEnabledState();
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        SnapApp snapApp = SnapApp.getDefault();
        product = snapApp.getSelectedProduct(SnapApp.SelectionSourceHint.VIEW);
        updateEnabledState();
        updateEnabledState();

        HarmonySearchServiceDiaglog harmonySearchServiceDiaglog = new HarmonySearchServiceDiaglog();
        harmonySearchServiceDiaglog.setVisible(true);
        harmonySearchServiceDiaglog.dispose();
        updateEnabledState();

    }

    protected void updateEnabledState() {
        super.setEnabled(true);
    }

    @Override
    public void resultChanged(LookupEvent ignored) {
        updateEnabledState();
    }

    @Override
    public JMenuItem getMenuPresenter() {
        JMenuItem menuItem = new JMenuItem(this);
        menuItem.setIcon(null);
        return menuItem;
    }

    @Override
    public Component getToolbarPresenter() {
        JButton button = new JButton(this);
        button.setText(null);
        return button;
    }
}
