package gov.nasa.gsfc.seadas.imageanimator.action;

import gov.nasa.gsfc.seadas.imageanimator.ui.ImageAnimatorDialog;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.esa.snap.ui.product.ProductSceneView;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.*;
import org.openide.util.actions.Presenter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;

/**
 * @author Aynur Abdurazik
 */

@ActionID(category = "View", id = "ImageAnimatorAction")
@ActionRegistration(displayName = "#CTL_ImageAnimatorActionName")
@ActionReferences({
        @ActionReference(path = "Menu/SeaDAS-Toolbox/General Tools", position = 100),
        @ActionReference(path = "Menu/View", position = 600),
        @ActionReference(path = "Toolbars/SeaDAS Toolbox", position = 100)
})
@NbBundle.Messages({
        "CTL_ImageAnimatorActionName=Image Animator",
        "CTL_ImageAnimatorActionToolTip=Show/hide Image Animator for the selected images"
})
public class ShowImageAnimatorAction extends AbstractSnapAction implements LookupListener, Presenter.Menu, Presenter.Toolbar  {

    Product product;
//    private boolean enabled = false;
    public static String SMALLICON = "gov/nasa/gsfc/seadas/image-animator/ui/icons/ImageAnimatorWhite24.png";
    public static String LARGEICON = "gov/nasa/gsfc/seadas/image-animator/ui/icons/ImageAnimatorWhite24.png";

    private final Lookup lookup;
    private final Lookup.Result<ProductSceneView> viewResult;

    public  ShowImageAnimatorAction() {
        this(null);
    }

    public   ShowImageAnimatorAction(Lookup lookup) {
        putValue(ACTION_COMMAND_KEY, getClass().getName());
        putValue(SELECTED_KEY, false);
        putValue(NAME, Bundle.CTL_ImageAnimatorActionName());
        putValue(SMALL_ICON, ImageUtilities.loadImageIcon(SMALLICON, false));
        putValue(LARGE_ICON_KEY, ImageUtilities.loadImageIcon(LARGEICON, false));
        putValue(SHORT_DESCRIPTION, Bundle.CTL_ImageAnimatorActionToolTip());
        this.lookup = lookup != null ? lookup : Utilities.actionsGlobalContext();
        this.viewResult = this.lookup.lookupResult(ProductSceneView.class);
        this.viewResult.addLookupListener(WeakListeners.create(LookupListener.class, this, viewResult));
        updateEnabledState();
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        SnapApp snapApp = SnapApp.getDefault();
        product = snapApp.getSelectedProduct(SnapApp.SelectionSourceHint.VIEW);
        ProductNodeGroup<Band> products = product.getBandGroup();
        SMALLICON = "gov/nasa/gsfc/seadas/image-animator/ui/icons/ImageAnimatorGreen24.png";
        LARGEICON = "gov/nasa/gsfc/seadas/image-animator/ui/icons/ImageAnimatorGreen24.png";
        putValue(SMALL_ICON, ImageUtilities.loadImageIcon(SMALLICON, false));
        putValue(LARGE_ICON_KEY, ImageUtilities.loadImageIcon(LARGEICON, false));
        updateEnabledState();


        ImageAnimatorDialog imageAnimatorDialog = new ImageAnimatorDialog(product, getActiveBands(products));
        imageAnimatorDialog.setVisible(true);
        imageAnimatorDialog.dispose();
        SMALLICON = "gov/nasa/gsfc/seadas/image-animator/ui/icons/ImageAnimatorWhite24.png";
        LARGEICON = "gov/nasa/gsfc/seadas/image-animator/ui/icons/ImageAnimatorWhite24.png";
        putValue(SMALL_ICON, ImageUtilities.loadImageIcon(SMALLICON, false));
        putValue(LARGE_ICON_KEY, ImageUtilities.loadImageIcon(LARGEICON, false));
        updateEnabledState();
    }

    private ArrayList<String> getActiveBands(ProductNodeGroup<Band> products) {
        Band[] bands = new Band[products.getNodeCount()];
        ArrayList<Band> activeBands = new ArrayList<>();
        ArrayList<String> activeBandNames = new ArrayList<>();
        products.toArray(bands);
        for (Band band : bands) {
            if (band.getImageInfo() != null) {
                activeBands.add(band);
                activeBandNames.add(band.getName());
            }
        }
        return activeBandNames;
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
        button.setIcon(ImageUtilities.loadImageIcon(LARGEICON,false));
        return button;
    }

    @Override
    public void resultChanged(LookupEvent ignored) {
        updateEnabledState();
    }

    protected void updateEnabledState() {
        final Product selectedProduct = SnapApp.getDefault().getSelectedProduct(SnapApp.SelectionSourceHint.AUTO);
        boolean productSelected = selectedProduct != null;
        boolean hasBands = false;
//        boolean hasGeoCoding = false;
        if (productSelected) {
            hasBands = selectedProduct.getNumBands() > 0;
//            hasGeoCoding = selectedProduct.getSceneGeoCoding() != null;
        }
//        super.setEnabled(!viewResult.allInstances().isEmpty() && hasBands);
        super.setEnabled(hasBands);
    }
}

