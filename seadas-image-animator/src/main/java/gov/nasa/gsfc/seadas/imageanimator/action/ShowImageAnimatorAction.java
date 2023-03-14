package gov.nasa.gsfc.seadas.imageanimator.action;

import gov.nasa.gsfc.seadas.imageanimator.data.ImageAnimatorData;
import gov.nasa.gsfc.seadas.imageanimator.operator.ImageAnimator1Spi;
import gov.nasa.gsfc.seadas.imageanimator.operator.ImageAnimatorDescriptor;
import gov.nasa.gsfc.seadas.imageanimator.ui.ImageAnimatorDialog;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.geom.GeometryFactory;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.util.FeatureUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.product.ProductSceneView;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.GeometryCoordinateSequenceTransformer;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.*;
import org.openide.util.actions.Presenter;

import javax.media.jai.JAI;
import javax.media.jai.OperationRegistry;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.RenderedOp;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Aynur Abdurazik
 */

@ActionID(category = "View", id = "ImageAnimatorAction")
@ActionRegistration(displayName = "#CTL_ImageAnimatorActionName")
@ActionReferences({
        @ActionReference(path = "Menu/SeaDAS-Toolbox/General Tools", position = 100),
        @ActionReference(path = "Menu/Layer"),
        @ActionReference(path = "Toolbars/SeaDAS Toolbox", position = 100)
})
@NbBundle.Messages({
        "CTL_ImageAnimatorActionName=Image Animator",
        "CTL_ImageAnimatorActionToolTip=Show/hide Image Animator for the selected images"
})
public class ShowImageAnimatorAction extends AbstractSnapAction implements LookupListener, Presenter.Menu, Presenter.Toolbar  {

    Product product;
//    private boolean enabled = false;
    public static final String SMALLICON = "gov/nasa/gsfc/seadas/imageAnimator/ui/icons/ImageAnimator.png";
    public static final String LARGEICON = "gov/nasa/gsfc/seadas/imageAnimator/ui/icons/ImageAnimator24.png";

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
        AppContext appContext = snapApp.getAppContext();
        final ProductSceneView sceneView = snapApp.getSelectedProductSceneView();
        product = snapApp.getSelectedProduct(SnapApp.SelectionSourceHint.VIEW);
        ProductNodeGroup<Band> products = product.getBandGroup();
        ImageAnimatorDialog imageAnimatorDialog = new ImageAnimatorDialog(product, getActiveBands(products));


        imageAnimatorDialog.setVisible(true);
        imageAnimatorDialog.dispose();
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
        boolean hasGeoCoding = false;
        if (productSelected) {
            hasBands = selectedProduct.getNumBands() > 0;
            hasGeoCoding = selectedProduct.getSceneGeoCoding() != null;
        }
        super.setEnabled(!viewResult.allInstances().isEmpty() && hasBands && hasGeoCoding);
    }
}

