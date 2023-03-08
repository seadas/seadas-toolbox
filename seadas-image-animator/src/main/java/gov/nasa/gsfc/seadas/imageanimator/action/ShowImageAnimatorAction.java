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


    final String DEFAULT_STYLE_FORMAT = "fill:%s; fill-opacity:0.5; stroke:%s; stroke-opacity:1.0; stroke-width:1.0; stroke-dasharray:%s; symbol:cross";
    Product product;
    double noDataValue;
    private GeoCoding geoCoding;
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

        if (imageAnimatorDialog.getFilteredBandName() != null) {
            if (product.getBand(imageAnimatorDialog.getFilteredBandName()) != null)
                product.getBandGroup().remove(product.getBand(imageAnimatorDialog.getFilteredBandName()));
        }

        setGeoCoding(product.getSceneGeoCoding());
        ImageAnimatorData imageAnimatorData = imageAnimatorDialog.getImageAnimatorData();
        noDataValue = imageAnimatorDialog.getNoDataValue();
        try {
            ArrayList<VectorDataNode> vectorDataNodes = createVectorDataNodesforImages(imageAnimatorData);
            for (VectorDataNode vectorDataNode : vectorDataNodes) {
                // remove the old vector data node with the same name.
                if (product.getVectorDataGroup().contains(vectorDataNode.getName())) {
                    product.getVectorDataGroup().remove(product.getVectorDataGroup().get(vectorDataNode.getName()));
                }
                product.getVectorDataGroup().add(vectorDataNode);
                if (sceneView != null) {
                    sceneView.setLayersVisible(vectorDataNode);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    public ArrayList<VectorDataNode> createVectorDataNodesforImages(ImageAnimatorData imageAnimatorData) {


        double scalingFactor = imageAnimatorData.getBand().getScalingFactor();
        double scalingOffset = imageAnimatorData.getBand().getScalingOffset();

        ArrayList<VectorDataNode> vectorDataNodes = new ArrayList<VectorDataNode>();

        //Register "ImageAnimator" operator if it's not in the registry
        OperationRegistry registry = JAI.getDefaultInstance().getOperationRegistry();
        String modeName = "rendered";
        boolean imageAnimatorIsRegistered = false;
        for (String name : registry.getDescriptorNames(modeName)) {
            if (name.contains("ImageAnimator")) {
                imageAnimatorIsRegistered = true;
            }
        }
        if (!imageAnimatorIsRegistered) {
            new ImageAnimator1Spi().updateRegistry(registry);
        }

        ParameterBlockJAI pb = new ParameterBlockJAI("ImageAnimator");
        pb.setSource("source0", imageAnimatorData.getBand().getSourceImage());
        ArrayList<Double> noDataList = new ArrayList<>();
        noDataList.add(noDataValue);
        pb.setParameter("nodata", noDataList);
        return vectorDataNodes;
    }

    private FeatureCollection<SimpleFeatureType, SimpleFeature> createImageAnimatorFeatureCollection(ParameterBlockJAI pb) {

        RenderedOp dest = JAI.create("ImageAnimator", pb);
        Collection<LineString > imageAnimators = (Collection<LineString >) dest.getProperty(ImageAnimatorDescriptor.CONTOUR_PROPERTY_NAME);
        SimpleFeatureType featureType = null;
        FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection = null;
        try {
            featureType = createFeatureType(geoCoding);
            featureCollection = new ListFeatureCollection(featureType);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        LineString lineString;
        Coordinate[] geomCoordinates;
        PrecisionModel geomPrecisionModel;
        PrecisionModel precisionModel;
        for (LineString geomLineString : imageAnimators) {
            geomCoordinates = geomLineString.getCoordinates();
            geomPrecisionModel = geomLineString.getPrecisionModel();
            precisionModel = new PrecisionModel(geomPrecisionModel.getScale(), geomPrecisionModel.getOffsetX(), geomPrecisionModel.getOffsetY());
            lineString = new LineString(transformaCoordinates(geomCoordinates), precisionModel, geomLineString.getSRID());
            Coordinate[] coordinates = lineString.getCoordinates();
            for (int i = 0; i < coordinates.length; i++) {
                coordinates[i].x = coordinates[i].x + 0.5;
                coordinates[i].y = coordinates[i].y + 0.5;
            }
            final SimpleFeature feature = createFeature(featureType, lineString);

            if (feature != null) {
                ((ListFeatureCollection) featureCollection).add(feature);
            }
        }

        final CoordinateReferenceSystem mapCRS = geoCoding.getMapCRS();
        //System.out.println("geo coding : " +  geoCoding.getImageCRS().toString());
        if (!mapCRS.equals(DefaultGeographicCRS.WGS84) || (geoCoding instanceof CrsGeoCoding)) {
            try {
                transformFeatureCollection(featureCollection, geoCoding.getImageCRS(), mapCRS);
            } catch (TransformException e) {
                Dialogs.showError("transformation failed!");
            }
        }

        return featureCollection;
    }

    private Coordinate[] transformaCoordinates(Coordinate[] geomCoordinates){
        Coordinate[] coordinates = new Coordinate[geomCoordinates.length];
        int i = 0;

        for (Coordinate coordinate:geomCoordinates) {
            coordinates[i++] = new Coordinate(coordinate.x, coordinate.y, coordinate.z);
        }
        return coordinates;
    }

    private static void transformFeatureCollection(FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection, CoordinateReferenceSystem sourceCRS, CoordinateReferenceSystem targetCRS) throws TransformException {
        final GeometryCoordinateSequenceTransformer transform = FeatureUtils.getTransform(sourceCRS, targetCRS);
        final FeatureIterator<SimpleFeature> features = featureCollection.features();
        final GeometryFactory geometryFactory = new GeometryFactory();
        while (features.hasNext()) {
            final SimpleFeature simpleFeature = features.next();
            //System.out.println("simple feature : " +  simpleFeature.toString());
            final LineString sourceLine = (LineString) simpleFeature.getDefaultGeometry();
            final LineString targetLine = transform.transformLineString((LineString) sourceLine, geometryFactory);
            simpleFeature.setDefaultGeometry(targetLine);
        }
    }

    private SimpleFeatureType createFeatureType(GeoCoding geoCoding) throws IOException {
        SimpleFeatureTypeBuilder ftb = new SimpleFeatureTypeBuilder();
        ftb.setName("gov.nasa.gsfc.imageAnimator.imageAnimatorVectorData");
        ftb.add("imageAnimator_lines", LineString.class, geoCoding.getImageCRS());
        ftb.setDefaultGeometry("imageAnimator_lines");
        final SimpleFeatureType ft = ftb.buildFeatureType();
        ft.getUserData().put("imageAnimatorVectorData", "true");
        return ft;
    }

    private static SimpleFeature createFeature(SimpleFeatureType type, LineString lineString) {

        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(type);
        /*0*/
        fb.add(lineString);
        return fb.buildFeature(null);
    }

    public GeoCoding getGeoCoding() {
        return geoCoding;
    }

    public void setGeoCoding(GeoCoding geoCoding) {
        this.geoCoding = geoCoding;
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

