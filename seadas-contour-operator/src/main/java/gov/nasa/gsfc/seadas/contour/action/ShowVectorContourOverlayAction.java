package gov.nasa.gsfc.seadas.contour.action;

import com.vividsolutions.jts.geom.PrecisionModel;
import gov.nasa.gsfc.seadas.contour.data.ContourData;
import gov.nasa.gsfc.seadas.contour.data.ContourInterval;
import gov.nasa.gsfc.seadas.contour.operator.Contour1Spi;
import gov.nasa.gsfc.seadas.contour.operator.ContourDescriptor;
import gov.nasa.gsfc.seadas.contour.ui.ContourDialog;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.util.FeatureUtils;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.esa.snap.rcp.actions.layer.overlay.AbstractOverlayAction;
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
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Coordinate;
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

@ActionID(category = "View", id = "OverlayContourLayerAction")
@ActionRegistration(displayName = "#CTL_OverlayContourLayerActionName")
@ActionReferences({
        @ActionReference(path = "Menu/Layer", position = 100),
        @ActionReference(path = "Toolbars/Overlay", position = 100)
})
@NbBundle.Messages({
        "CTL_OverlayContourLayerActionName=Contour Overlay",
        "CTL_OverlayContourLayerActionToolTip=Show/hide Contour overlay for the selected image"
})
public class ShowVectorContourOverlayAction extends AbstractSnapAction implements LookupListener, Presenter.Menu, Presenter.Toolbar  {


    final String DEFAULT_STYLE_FORMAT = "fill:%s; fill-opacity:0.5; stroke:%s; stroke-opacity:1.0; stroke-width:1.0; stroke-dasharray:%s; symbol:cross";
    Product product;
    double noDataValue;
    private GeoCoding geoCoding;
//    private boolean enabled = false;
    public static final String SMALLICON = "gov/nasa/gsfc/seadas/contour/ui/icons/ContourOverlay.png";
    public static final String LARGEICON = "gov/nasa/gsfc/seadas/contour/ui/icons/ContourOverlay24.png";

    private final Lookup lookup;
    private final Lookup.Result<ProductSceneView> viewResult;

//    public  ShowVectorContourOverlayAction() {
//        this(Utilities.actionsGlobalContext());
//    }

    public  ShowVectorContourOverlayAction() {
        this(null);
    }


    public   ShowVectorContourOverlayAction(Lookup lookup) {
        putValue(ACTION_COMMAND_KEY, getClass().getName());
        putValue(SELECTED_KEY, false);
        putValue(NAME, Bundle.CTL_OverlayContourLayerActionName());
        putValue(SMALL_ICON, ImageUtilities.loadImageIcon(SMALLICON, false));
        putValue(LARGE_ICON_KEY, ImageUtilities.loadImageIcon(LARGEICON, false));
        putValue(SHORT_DESCRIPTION, Bundle.CTL_OverlayContourLayerActionToolTip());
        this.lookup = lookup != null ? lookup : Utilities.actionsGlobalContext();
        this.viewResult = this.lookup.lookupResult(ProductSceneView.class);
        this.viewResult.addLookupListener(WeakListeners.create(LookupListener.class, this, viewResult));
        updateEnabledState();
    }



//    @Override
//    public Action createContextAwareInstance(Lookup actionContext) {
//        return new ShowVectorContourOverlayAction(actionContext);
//    }
//    @Override
//    protected void initActionProperties() {
//        putValue(NAME, Bundle.CTL_OverlayContourLayerActionName());
//        putValue(SMALL_ICON, ImageUtilities.loadImageIcon("gov/nasa/gsfc/seadas/contour/ui/icons/ContourOverlay22.png", false));
//        putValue(LARGE_ICON_KEY, ImageUtilities.loadImageIcon("gov/nasa/gsfc/seadas/contour/ui/icons/ContourOverlay24.gif", false));
//        putValue(SHORT_DESCRIPTION, Bundle.CTL_OverlayContourLayerActionToolTip());
//    }
//
    @Override
    public void actionPerformed(ActionEvent event) {
        SnapApp snapApp = SnapApp.getDefault();
        AppContext appContext = snapApp.getAppContext();
        final ProductSceneView sceneView = snapApp.getSelectedProductSceneView();
        product = snapApp.getSelectedProduct(SnapApp.SelectionSourceHint.VIEW);
        ProductNodeGroup<Band> products = product.getBandGroup();
        ContourDialog contourDialog = new ContourDialog(product, getActiveBands(products));
        contourDialog.setVisible(true);
        contourDialog.dispose();

        if (contourDialog.getFilteredBandName() != null) {
            if (product.getBand(contourDialog.getFilteredBandName()) != null)
                product.getBandGroup().remove(product.getBand(contourDialog.getFilteredBandName()));
        }

        if (contourDialog.isContourCanceled()) {
            return;
        }
        setGeoCoding(product.getSceneGeoCoding());
        ContourData contourData = contourDialog.getContourData();
        noDataValue = contourDialog.getNoDataValue();
        try {
            ArrayList<VectorDataNode> vectorDataNodes = createVectorDataNodesforContours(contourData);
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

    public ArrayList<VectorDataNode> createVectorDataNodesforContours(ContourData contourData) {


        double scalingFactor = contourData.getBand().getScalingFactor();
        double scalingOffset = contourData.getBand().getScalingOffset();

        ArrayList<ContourInterval> contourIntervals = contourData.getLevels();
        ArrayList<VectorDataNode> vectorDataNodes = new ArrayList<VectorDataNode>();

        //Register "Contour" operator if it's not in the registry
        OperationRegistry registry = JAI.getDefaultInstance().getOperationRegistry();
        String modeName = "rendered";
        boolean contourIsRegistered = false;
        for (String name : registry.getDescriptorNames(modeName)) {
            if (name.contains("Contour")) {
                contourIsRegistered = true;
            }
        }
        if (!contourIsRegistered) {
            new Contour1Spi().updateRegistry(registry);
        }

        ParameterBlockJAI pb = new ParameterBlockJAI("Contour");
        pb.setSource("source0", contourData.getBand().getSourceImage());
        ArrayList<Double> noDataList = new ArrayList<>();
        noDataList.add(noDataValue);
        pb.setParameter("nodata", noDataList);

        for (ContourInterval interval : contourIntervals) {
            ArrayList<Double> contourInterval = new ArrayList<Double>();
            String vectorName = interval.getContourLevelName();
            if (contourData.isFiltered()) {
                vectorName = vectorName + "_filtered";
            }
            double contourValue = (interval.getContourLevelValue() - scalingOffset) / scalingFactor;
            contourInterval.add(contourValue);
            pb.setParameter("levels", contourInterval);

            FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection = null;

            try {
                featureCollection = createContourFeatureCollection(pb);
            } catch (Exception e) {
                e.printStackTrace();
                if (contourData.getLevels().size() != 0)
                    System.out.println(e.getMessage());
                if (SnapApp.getDefault() != null) {
                    Dialogs.showError("failed to create contour lines");
                }
                continue;
            }
            if (featureCollection.isEmpty()) {
                if (SnapApp.getDefault() != null) {
                    Dialogs.showError("Contour Lines", "No records found for ." + contourData.getBand().getName() + " at " + (contourValue * scalingFactor + scalingOffset));
                }
                    continue;
            }

            final PlacemarkDescriptor placemarkDescriptor = PlacemarkDescriptorRegistry.getInstance().getPlacemarkDescriptor(featureCollection.getSchema());
            placemarkDescriptor.setUserDataOf(featureCollection.getSchema());
            VectorDataNode vectorDataNode = new VectorDataNode(vectorName, featureCollection, placemarkDescriptor);

            //convert RGB color to an hexadecimal value
            //String hex = "#"+Integer.toHexString(interval.getLineColor().getRGB()).substring(2);
            String hex = String.format("#%02x%02x%02x", interval.getLineColor().getRed(), interval.getLineColor().getGreen(), interval.getLineColor().getBlue());
            vectorDataNode.setDefaultStyleCss(String.format(DEFAULT_STYLE_FORMAT, hex, hex, interval.getContourLineStyleValue()));
            vectorDataNodes.add(vectorDataNode);
        }
        return vectorDataNodes;
    }

    private FeatureCollection<SimpleFeatureType, SimpleFeature> createContourFeatureCollection(ParameterBlockJAI pb) {

        RenderedOp dest = JAI.create("Contour", pb);
        Collection<org.locationtech.jts.geom.LineString > contours = (Collection<org.locationtech.jts.geom.LineString >) dest.getProperty(ContourDescriptor.CONTOUR_PROPERTY_NAME);
        SimpleFeatureType featureType = null;
        FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection = null;
        try {
            featureType = createFeatureType(geoCoding);
            featureCollection = new ListFeatureCollection(featureType);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        LineString lineString;
        org.locationtech.jts.geom.Coordinate[] geomCoordinates;
        org.locationtech.jts.geom.PrecisionModel geomPrecisionModel;
        PrecisionModel precisionModel;
        for (org.locationtech.jts.geom.LineString geomLineString : contours) {
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

    private Coordinate[] transformaCoordinates(org.locationtech.jts.geom.Coordinate[] geomCoordinates){
        Coordinate[] coordinates = new Coordinate[geomCoordinates.length];
        int i = 0;

        for (org.locationtech.jts.geom.Coordinate coordinate:geomCoordinates) {
            coordinates[i++] = new Coordinate(coordinate.x, coordinate.y, coordinate.z);
        }
        return coordinates;
    }

    private static void transformFeatureCollection(FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection, CoordinateReferenceSystem sourceCRS, CoordinateReferenceSystem targetCRS) throws TransformException {
        final GeometryCoordinateSequenceTransformer transform = FeatureUtils.getTransform(sourceCRS, targetCRS);
        final FeatureIterator<SimpleFeature> features = featureCollection.features();
        final com.vividsolutions.jts.geom.GeometryFactory geometryFactory = new com.vividsolutions.jts.geom.GeometryFactory();
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
        ftb.setName("gov.nasa.gsfc.contour.contourVectorData");
        ftb.add("contour_lines", LineString.class, geoCoding.getImageCRS());
        ftb.setDefaultGeometry("contour_lines");
        final SimpleFeatureType ft = ftb.buildFeatureType();
        ft.getUserData().put("contourVectorData", "true");
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
//    @Override
//    protected boolean getActionSelectionState(ProductSceneView view) {
//        return view.isGraticuleOverlayEnabled();
////        return isEnabled();
//    }
//
//    @Override
//    protected boolean getActionEnabledState(ProductSceneView view) {
//        return ProductUtils.canGetPixelPos(view.getRaster());
//    }
//
//    @Override
//    protected void setOverlayEnableState(ProductSceneView view) {
//        view.setGraticuleOverlayEnabled(!getActionSelectionState(view));
////        setEnabled(!getActionSelectionState(view));
//    }

//    @Override
//    public boolean isEnabled() {
//        return enabled;
//    }
//
//    @Override
//    public void setEnabled(boolean enabled) {
//        this.enabled = enabled;
//    }
}

