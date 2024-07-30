package gov.nasa.gsfc.seadas.earthdatacloud.action;

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
import java.util.ArrayList;


@ActionID(category = "View", id = "EarthCloudDataAction")
@ActionRegistration(displayName = "#CTL_EarthCloudDataActionName")
@ActionReferences({
        @ActionReference(path = "Menu/EarthData-Cloud-Toolbox", position = 100),
        @ActionReference(path = "Menu/View", position = 600),
        @ActionReference(path = "Toolbars", position = 100)
})
@NbBundle.Messages({
        "CTL_EarthCloudDataActionName=EarthData Cloud Toolbox",
        "CTL_EarthDataCloudActionToolTip=Show/hide EarthData Cloud Toolbox"
})

public class ShowEarthDataCloudMenuAction extends AbstractSnapAction implements LookupListener, Presenter.Menu, Presenter.Toolbar  {

    Product product;
    //    private boolean enabled = false;
    public static String SMALLICON = "gov/nasa/gsfc/seadas/image-animator/ui/icons/ImageAnimatorWhite24.png";
    public static String LARGEICON = "gov/nasa/gsfc/seadas/image-animator/ui/icons/ImageAnimatorWhite24.png";

    private final Lookup lookup;

    public  ShowEarthDataCloudMenuAction() {
        this(null);
    }

    public   ShowEarthDataCloudMenuAction(Lookup lookup) {
        putValue(ACTION_COMMAND_KEY, getClass().getName());
        putValue(SELECTED_KEY, false);
        putValue(NAME, Bundle.CTL_ImageAnimatorActionName());
        putValue(SMALL_ICON, ImageUtilities.loadImageIcon(SMALLICON, false));
        putValue(LARGE_ICON_KEY, ImageUtilities.loadImageIcon(LARGEICON, false));
        putValue(SHORT_DESCRIPTION, Bundle.CTL_ImageAnimatorActionToolTip());
        this.lookup = lookup != null ? lookup : Utilities.actionsGlobalContext();
        updateEnabledState();
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        SnapApp snapApp = SnapApp.getDefault();
        product = snapApp.getSelectedProduct(SnapApp.SelectionSourceHint.VIEW);

        SMALLICON = "gov/nasa/gsfc/seadas/image-animator/ui/icons/ImageAnimatorGreen24.png";
        LARGEICON = "gov/nasa/gsfc/seadas/image-animator/ui/icons/ImageAnimatorGreen24.png";
        putValue(SMALL_ICON, ImageUtilities.loadImageIcon(SMALLICON, false));
        putValue(LARGE_ICON_KEY, ImageUtilities.loadImageIcon(LARGEICON, false));
        updateEnabledState();


        SMALLICON = "gov/nasa/gsfc/seadas/image-animator/ui/icons/ImageAnimatorWhite24.png";
        LARGEICON = "gov/nasa/gsfc/seadas/image-animator/ui/icons/ImageAnimatorWhite24.png";
        putValue(SMALL_ICON, ImageUtilities.loadImageIcon(SMALLICON, false));
        putValue(LARGE_ICON_KEY, ImageUtilities.loadImageIcon(LARGEICON, false));
        updateEnabledState();
    }

    protected void updateEnabledState() {

    }

    @Override
    public void resultChanged(LookupEvent ignored) {
        updateEnabledState();
    }

    @Override
    public JMenuItem getMenuPresenter() {
        return null;
    }

    @Override
    public Component getToolbarPresenter() {
        return null;
    }
}
