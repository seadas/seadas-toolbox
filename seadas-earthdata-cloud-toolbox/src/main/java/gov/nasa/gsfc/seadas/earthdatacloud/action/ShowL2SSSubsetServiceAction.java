package gov.nasa.gsfc.seadas.earthdatacloud.action;

import gov.nasa.gsfc.seadas.earthdatacloud.ui.HarmonySubsetServiceDiaglog;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.*;
import org.openide.util.actions.Presenter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;


@ActionID(
        category = "View", id = "HarmonySubsetServiceAction"
)

@ActionRegistration(
        displayName = "#CTL_HarmonySubsetServiceActionName",
        popupText = "#CTL_HarmonySubsetServiceActionName"
)

//@ActionReference(
//        path = "Menu/Earthdata-Cloud/Harmony Services",
//        position = 100
//)
//@NbBundle.Messages({
//        "CTL_HarmonySubsetServiceActionName=Subset Service",
//        "CTL_HarmonySubsetServiceActionToolTip=Show/hide Subset Service"
//})
@ActionReferences({
        @ActionReference(path = "Menu/Earthdata-Cloud", position = 40)
})

@NbBundle.Messages({
        "CTL_HarmonySubsetServiceActionName=OB_CLOUD Data Subsetter",
        "CTL_HarmonySubsetServiceActionToolTip=Show/hide Subset Service"
})

public class ShowL2SSSubsetServiceAction extends AbstractSnapAction implements LookupListener, Presenter.Menu, Presenter.Toolbar  {

    Product product;
    //    private boolean enabled = false;
    // todo - later - Danny this is where icon would go
//    public static String SMALLICON = "gov/nasa/gsfc/seadas/image-animator/ui/icons/ImageAnimatorWhite24.png";
//    public static String LARGEICON = "gov/nasa/gsfc/seadas/image-animator/ui/icons/ImageAnimatorWhite24.png";

    private final Lookup lookup;

    public ShowL2SSSubsetServiceAction() {
        this(null);
    }

    public ShowL2SSSubsetServiceAction(Lookup lookup) {
        putValue(ACTION_COMMAND_KEY, getClass().getName());
        putValue(SELECTED_KEY, false);
        putValue(NAME, Bundle.CTL_HarmonySubsetServiceActionName());
//        putValue(SMALL_ICON, ImageUtilities.loadImageIcon(SMALLICON, false));
//        putValue(LARGE_ICON_KEY, ImageUtilities.loadImageIcon(LARGEICON, false));
        putValue(SHORT_DESCRIPTION, Bundle.CTL_HarmonySubsetServiceActionToolTip());
        this.lookup = lookup != null ? lookup : Utilities.actionsGlobalContext();
        updateEnabledState();
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        SnapApp snapApp = SnapApp.getDefault();
        //product = snapApp.getSelectedProduct(SnapApp.SelectionSourceHint.VIEW);
        product = snapApp.getSelectedProduct(SnapApp.SelectionSourceHint.VIEW);
//        SMALLICON = "gov/nasa/gsfc/seadas/image-animator/ui/icons/ImageAnimatorGreen24.png";
//        LARGEICON = "gov/nasa/gsfc/seadas/image-animator/ui/icons/ImageAnimatorGreen24.png";
//        putValue(SMALL_ICON, ImageUtilities.loadImageIcon(SMALLICON, false));
//        putValue(LARGE_ICON_KEY, ImageUtilities.loadImageIcon(LARGEICON, false));
        updateEnabledState();
//        SMALLICON = "gov/nasa/gsfc/seadas/image-animator/ui/icons/ImageAnimatorWhite24.png";
//        LARGEICON = "gov/nasa/gsfc/seadas/image-animator/ui/icons/ImageAnimatorWhite24.png";
//        putValue(SMALL_ICON, ImageUtilities.loadImageIcon(SMALLICON, false));
//        putValue(LARGE_ICON_KEY, ImageUtilities.loadImageIcon(LARGEICON, false));
        updateEnabledState();

        HarmonySubsetServiceDiaglog harmonySubsetServiceDiaglog = new HarmonySubsetServiceDiaglog();
        harmonySubsetServiceDiaglog.setVisible(true);
        harmonySubsetServiceDiaglog.dispose();
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
//        button.setIcon(ImageUtilities.loadImageIcon(LARGEICON,false));
        return button;
    }
}
