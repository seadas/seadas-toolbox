package gov.nasa.gsfc.seadas.watermask.ui;

import org.esa.snap.core.gpf.OperatorException;
import com.bc.ceres.multilevel.MultiLevelImage;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.esa.snap.rcp.imgfilter.model.Filter;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.*;
import org.openide.util.actions.Presenter;

import java.awt.event.ActionEvent;

import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.operator.FormatDescriptor;
import javax.swing.*;
import java.awt.*;
import java.awt.image.RenderedImage;
import java.util.HashMap;
import java.util.Map;


/**
 * This registers an action which calls the "CoastlineLandWaterMasks" Operator and based on its generated "water_fraction"
 * band, defines 3 masks in the currently selected product:
 * <ol>
 * <li>Water: water_fraction > 90</li>
 * <li>Land: water_fraction < 10</li>
 * <li>Coastline: water_fraction > 0.1 && water_fraction < 99.9 (meaning if water_fraction is not 0 or 100 --> it is a coastline)</li>
 * </ol>
 * <p/>

 * @author Tonio Fincke
 * @author Danny Knowles
 * @author Marco Peters
 */
@ActionID(category = "Processing", id = "gov.nasa.gsfc.seadas.watermask.ui.WaterMaskAction" )
@ActionRegistration(displayName = "#CTL_WaterMaskAction_Text", lazy = false)
@ActionReferences({
        @ActionReference(path = "Menu/SeaDAS-Toolbox/General Tools", position = 40),
        @ActionReference(path = "Menu/Raster/Masks"),
        @ActionReference(path = "Toolbars/SeaDAS Toolbox", position = 20)
})
@NbBundle.Messages({
        "CTL_WaterMaskAction_Text=Land Mask Tool",
        "CTL_WaterMaskAction_Description=Land Mask Tool -- adds land band, coast band and associated masks."
})

public final class WaterMaskAction extends AbstractSnapAction implements LookupListener, Presenter.Menu, Presenter.Toolbar {

    public static final String COMMAND_ID = "Land, Water, Coast Masks";
    public static final String TOOL_TIP = "Add coastline, land and water masks";
    //    public static final String ICON = "/org/esa/beam/watermask/ui/icons/coastline_24.png";
    //  public static final String ICON = "icons/Coastline24.png";
    public static final String SMALLICON = "gov/nasa/gsfc/seadas/watermask/ui/icons/coastline.png";
    public static final String LARGEICON = "gov/nasa/gsfc/seadas/watermask/ui/icons/coastline_24.png";

    public static final String LAND_WATER_MASK_OP_ALIAS = "CoastlineLandWaterMasks";
    public static final String TARGET_TOOL_BAR_NAME = "layersToolBar";
    private static final String HELP_ID = "watermaskScientificTool";

    private final Lookup lookup;
    private final Lookup.Result<ProductNode> viewResult;

    private final int SLEEP_SHORT_DURATION = 1500;
    private final int SLEEP_MEDIUM_DURATION = 4000;
    private final int SLEEP_LONG_DURATION = 16000;

    private int sleepShort = SLEEP_SHORT_DURATION;
    private int sleepMedium = SLEEP_MEDIUM_DURATION;
    private int sleepLong = SLEEP_LONG_DURATION;


    public  WaterMaskAction() {
        this(null);
    }

    public WaterMaskAction(Lookup lookup){
        putValue(ACTION_COMMAND_KEY, getClass().getName());
        putValue(NAME, Bundle.CTL_WaterMaskAction_Text());
        putValue(SMALL_ICON, ImageUtilities.loadImageIcon(SMALLICON, false));
        putValue(LARGE_ICON_KEY, ImageUtilities.loadImageIcon(LARGEICON, false));
        putValue(SHORT_DESCRIPTION, Bundle.CTL_WaterMaskAction_Description());
        this.lookup = lookup != null ? lookup : Utilities.actionsGlobalContext();
        this.viewResult = this.lookup.lookupResult(ProductNode.class);
        this.viewResult.addLookupListener(WeakListeners.create(LookupListener.class, this, viewResult));
        updateEnabledState();
    }

    private void showLandWaterCoastMasks(final SnapApp snapApp) {



        final Product product = snapApp.getSelectedProduct(SnapApp.SelectionSourceHint.AUTO);
        if (product != null) {

            final ProductNodeGroup<Mask> maskGroup = product.getMaskGroup();
            final ProductNodeGroup<Band> bandGroup = product.getBandGroup();

            /*
               A simple boolean switch to enable this to run with or without the intermediate user dialogs
            */

            // todo for Danny: turn this boolean on when it is ready
            boolean useDialogs = true;

            final LandMasksData landMasksData = new LandMasksData();


            boolean bandsAreOpenForThisFile = bandsAreOpenForThisFile(product);
            if (bandsAreOpenForThisFile) {
                sleepMedium = SLEEP_MEDIUM_DURATION;
                sleepLong = SLEEP_LONG_DURATION;
            } else {
                sleepMedium = SLEEP_SHORT_DURATION;
                sleepLong = SLEEP_SHORT_DURATION;
            }


            if (!useDialogs) {
                landMasksData.setCreateMasks(true);
            }


            String savedLandMaskName = landMasksData.getLandMaskName() + "-SAV";
            String savedWaterMaskName = landMasksData.getWaterMaskName() + "-SAV";
            String savedCoastMaskName = landMasksData.getCoastlineMaskName() + "-SAV";

            /*
                Determine whether these auxilliary masks and associated products have already be created.
                This would be the case when run a second time on the same product.
            */

            final boolean[] masksCreated = {false};

            for (String name : maskGroup.getNodeNames()) {
                if (name.equals(landMasksData.getCoastlineMaskName()) ||
                        name.equals(landMasksData.getLandMaskName()) ||
                        name.equals(landMasksData.getWaterMaskName())) {
                    masksCreated[0] = true;
                }
            }


            for (String name : bandGroup.getNodeNames()) {
                if (name.equals(landMasksData.getWaterFractionBandName()) ||
                        name.equals(landMasksData.getWaterFractionSmoothedName())) {
                    masksCreated[0] = true;
                }
            }

            /*
                For the case where this is being run a second time, prompt the user to determine whether to delete
                and re-create the products and masks.
             */

            if (masksCreated[0]) {
                if (useDialogs) {
                    landMasksData.setDeleteMasks(false);
                    LandMasksDialog landMasksDialog = new LandMasksDialog(landMasksData, masksCreated[0]);
                    landMasksDialog.setVisible(true);
                    landMasksDialog.dispose();

                }

                if (landMasksData.isDeleteMasks() || !useDialogs) {
                    masksCreated[0] = false;


                    // remove any previously saved masks
                    for (String name : maskGroup.getNodeNames()) {
                        if (name.equalsIgnoreCase(savedLandMaskName)
                                || name.equalsIgnoreCase(savedWaterMaskName)
                                || name.equalsIgnoreCase(savedCoastMaskName)) {
                            Mask savedMask = maskGroup.get(name);
                            if (savedMask != null) {
                                String[] bandNames = product.getBandNames();
                                for (String bandName : bandNames) {
                                    RasterDataNode raster = product.getRasterDataNode(bandName);
                                    if (raster != null) {
                                        ProductNodeGroup<Mask> overlayMaskGroup = raster.getOverlayMaskGroup();
                                        if (overlayMaskGroup != null && overlayMaskGroup.contains(savedMask)) {
                                            overlayMaskGroup.remove(savedMask);
                                        }
                                    }
                                }
                                maskGroup.remove(savedMask);
                            }
                        }
                    }


                    // copy mask to saved
                    for (String name : maskGroup.getNodeNames()) {
                        if (name.equalsIgnoreCase(landMasksData.getLandMaskName())
                        || name.equalsIgnoreCase(landMasksData.getWaterMaskName())
                                || name.equalsIgnoreCase(landMasksData.getCoastlineMaskName())) {
                            Mask thisMask = maskGroup.get(name);
                            if (thisMask != null) {
                                String[] bandNames = product.getBandNames();
                                for (String bandName : bandNames) {
                                    RasterDataNode raster = product.getRasterDataNode(bandName);
                                    if (raster != null) {
                                        ProductNodeGroup<Mask> overlayMaskGroup = raster.getOverlayMaskGroup();
                                        if (overlayMaskGroup != null && overlayMaskGroup.contains(thisMask)) {
                                            overlayMaskGroup.remove(thisMask);
                                        }
                                    }
                                }
                                if (name.equals(landMasksData.getLandMaskName())) {
                                    thisMask.setName(savedLandMaskName);
                                } else if (name.equals(landMasksData.getWaterMaskName())) {
                                    thisMask.setName(savedWaterMaskName);
                                } else if (name.equals(landMasksData.getCoastlineMaskName())) {
                                    thisMask.setName(savedCoastMaskName);
                                }
                            }
                        }
                    }



                    for (String name : bandGroup.getNodeNames()) {
                        if (name.equals(landMasksData.getWaterFractionBandName()) ||
                                name.equals(landMasksData.getWaterFractionSmoothedName())) {
                            bandGroup.remove(bandGroup.get(name));
                        }
                    }


                }
            }


            if (!masksCreated[0]) {
                if (useDialogs) {
                    landMasksData.setCreateMasks(false);
                    LandMasksDialog landMasksDialog = new LandMasksDialog(landMasksData, masksCreated[0]);
                    landMasksDialog.setVisible(true);
                }

                if (landMasksData.isCreateMasks()) {
                    final SourceFileInfo sourceFileInfo = landMasksData.getSourceFileInfo();

                    if (sourceFileInfo.isEnabled()) {

                        String pmTitle = "<html>Running ...<br>&nbsp;</html>";
                        if (bandsAreOpenForThisFile) {
                            pmTitle = "<html>Running ...<br><br>Note: Processing speed can be improved <br>by running this tool with no bands opened.<br><br><hr><br></html>";
                        }
                        final String pmTitleFinal = pmTitle;

                        ProgressMonitorSwingWorker pmSwingWorker = new ProgressMonitorSwingWorker(snapApp.getMainFrame(),
                                "Land Mask Tool") {

                            @Override
                            protected Void doInBackground(com.bc.ceres.core.ProgressMonitor pm) throws Exception {

                                boolean createCoastBand = landMasksData.isCreateCoastline();
                                Mask coastlineMask = null;
                                int boxSize = landMasksData.getCoastalGridSize();

                                int totalWork = (4 + 11) * 4 ;
                                if (createCoastBand && boxSize > 1) {
                                    totalWork = (4 + 9 + 11) * 4;
                                }

                                int workDone = 0;
                                pm.setSubTaskName("Initializing");
                                pm.beginTask(pmTitleFinal, totalWork);

                                try {
                                    //  Product landWaterProduct = GPF.createProduct("LandWaterMask", GPF.NO_PARAMS, product);


                                    Map<String, Object> parameters = new HashMap<String, Object>();

                                    Integer supersampling = Integer.valueOf(landMasksData.getSuperSampling());
                                    parameters.put("superSamplingFactor", supersampling);
                                    // parameters.put("subSamplingFactorY", new Integer(landMasksData.getSuperSampling()));
                                    parameters.put("resolution", sourceFileInfo.getResolution(SourceFileInfo.Unit.METER));
                                    parameters.put("mode", sourceFileInfo.getMode().toString());
                                    parameters.put("worldSourceDataFilename", sourceFileInfo.getFile().getName());
                                    parameters.put("copySourceFile", "false");  // when run in GUI don't do this
                                    //    parameters.put("coastalGridSize", landMasksData.getCoastalGridSize());
                                    //    parameters.put("coastalSizeTolerance", landMasksData.getCoastalSizeTolerance());
                                    parameters.put("includeMasks", false);  // don't create masks within the operator, do it later
                                    //                             parameters.put("sourceFileInfo", sourceFileInfo);
                                    /*
                                       Create a new product, which will contain the land_water_fraction band
                                    */




                                    Product landWaterProduct;

                                    try {
//                                        pm.setSubTaskName("Running operator: " + LAND_WATER_MASK_OP_ALIAS);
                                        landWaterProduct = GPF.createProduct(LAND_WATER_MASK_OP_ALIAS, parameters, product);
                                        workDone += sleepPreviewThread(sleepShort,4, pm, totalWork, workDone);

                                        if (landWaterProduct == null) {
                                            pm.setSubTaskName("Operator Failed!!: " + LAND_WATER_MASK_OP_ALIAS);
                                            sleepPreviewThread(sleepMedium);
                                            return null;
                                        }
                                    } catch (OperatorException e) {
                                        pm.setSubTaskName("Operator Failed!!: " + LAND_WATER_MASK_OP_ALIAS);
                                        sleepPreviewThread(sleepMedium);
                                        return null;
                                    }


                                    Band waterFractionBand = landWaterProduct.getBand(landMasksData.getWaterFractionBandName());

                                    workDone += sleepPreviewThread(sleepShort,4, pm, totalWork, workDone);

                                    //    Band coastBand = landWaterProduct.getBand("coast");

                                    // PROBLEM WITH TILE SIZES
                                    // Example: product has tileWidth=498 and tileHeight=611
                                    // resulting image has tileWidth=408 and tileHeight=612
                                    // Why is this happening and where?
                                    // For now we change the image layout here.

                                    pm.setSubTaskName("Creating band: " + landMasksData.getWaterFractionBandName());
                                    reformatSourceImage(waterFractionBand, new ImageLayout(product.getBandAt(0).getSourceImage()), pm);

                                    workDone += sleepPreviewThread(sleepShort,4, pm, totalWork, workDone);

                                    waterFractionBand.setName(landMasksData.getWaterFractionBandName());

                                    product.addBand(waterFractionBand);

                                    pm.setSubTaskName("Band created: " + landMasksData.getWaterFractionBandName());

                                    workDone += sleepPreviewThread(sleepShort,4, pm, totalWork, workDone);


                                    //todo BEAM folks left this as a placeholder
//                    product.addBand(coastBand);

                                    //todo replace with JAI operator "GeneralFilter" which uses a GeneralFilterFunction


                                    if (createCoastBand && boxSize > 1) {

                                        pm.setSubTaskName("Creating band: " + landMasksData.getWaterFractionSmoothedName());

                                        final Filter meanFilter = new Filter("Mean " + Integer.toString(boxSize) + "x" + Integer.toString(boxSize), "mean" + Integer.toString(boxSize), Filter.Operation.MEAN, boxSize, boxSize);
                                        final Kernel meanKernel = new Kernel(meanFilter.getKernelWidth(),
                                                meanFilter.getKernelHeight(),
                                                meanFilter.getKernelOffsetX(),
                                                meanFilter.getKernelOffsetY(),
                                                1.0 / meanFilter.getKernelQuotient(),
                                                meanFilter.getKernelElements());


//                                    final Kernel arithmeticMean3x3Kernel = new Kernel(3, 3, 1.0 / 9.0,
//                                            new double[]{
//                                                    +1, +1, +1,
//                                                    +1, +1, +1,
//                                                    +1, +1, +1,
//                                            });
//todo: 4th argument to ConvolutionFilterBand is a dummy value added to make it compile...may want to look at this...
                                        int count = 1;
//                                    final ConvolutionFilterBand filteredCoastlineBand = new ConvolutionFilterBand(
//                                            landMasksData.getWaterFractionSmoothedName(),
//                                            waterFractionBand,
//                                            arithmeticMean3x3Kernel, count);


                                        final FilterBand filteredCoastlineBand = new GeneralFilterBand(landMasksData.getWaterFractionSmoothedName(), waterFractionBand, GeneralFilterBand.OpType.MEAN, meanKernel, count);

                                        workDone += sleepPreviewThread(sleepShort,4, pm, totalWork, workDone);


                                        pm.setSubTaskName("Copying properties to: " + landMasksData.getWaterFractionSmoothedName());
                                        if (waterFractionBand instanceof Band) {
                                            ProductUtils.copySpectralBandProperties((Band) waterFractionBand, filteredCoastlineBand);
                                        }

                                        workDone += sleepPreviewThread(sleepShort,4, pm, totalWork, workDone);


                                        pm.setSubTaskName("Adding band: " + landMasksData.getWaterFractionSmoothedName());
                                        product.addBand(filteredCoastlineBand);
                                        pm.setSubTaskName("Band created: " + landMasksData.getWaterFractionSmoothedName());

                                        workDone += sleepPreviewThread(sleepShort,4, pm, totalWork, workDone);


                                        pm.setSubTaskName("Creating mask: " + landMasksData.getCoastlineMaskName());

                                        coastlineMask = Mask.BandMathsType.create(
                                                landMasksData.getCoastlineMaskName(),
                                                landMasksData.getCoastlineMaskDescription(),
                                                product.getSceneRasterWidth(),
                                                product.getSceneRasterHeight(),
                                                landMasksData.getCoastalMath(),
                                                landMasksData.getCoastlineMaskColor(),
                                                landMasksData.getCoastlineMaskTransparency());

                                        workDone += sleepPreviewThread(sleepShort,4, pm, totalWork, workDone);


                                        try {
                                            Mask coastlineMaskTmp = maskGroup.get(coastlineMask.getName());
                                            if (coastlineMaskTmp != null) {
                                                pm.setSubTaskName("Deleting existing mask: " + landMasksData.getCoastlineMaskName());
                                                maskGroup.remove(coastlineMaskTmp);
                                                workDone += sleepPreviewThread(sleepShort,4, pm, totalWork, workDone);
                                            }

                                            pm.setSubTaskName("Adding mask: " + landMasksData.getCoastlineMaskName());
                                            workDone += sleepPreviewThread(sleepLong,8, pm, totalWork, workDone);
                                            maskGroup.add(coastlineMask);
                                            workDone += sleepPreviewThread(sleepShort,4, pm, totalWork, workDone);

                                            pm.setSubTaskName("Mask created: " + landMasksData.getCoastlineMaskName());

                                        } catch (Exception e) {
                                            pm.setSubTaskName("ERROR!! Mask not created: " + landMasksData.getCoastlineMaskName());
                                        }




                                        workDone += sleepPreviewThread(sleepShort,4, pm, totalWork, workDone);
                                    }


                                    // Cleanup saved coast mask
                                    Mask savedCoastMask = maskGroup.get(savedCoastMaskName);
                                    if (savedCoastMask != null) {
                                        maskGroup.remove(savedCoastMask);
                                        workDone += sleepPreviewThread(sleepShort,4, pm, totalWork, workDone);

                                    }
                                    // end cleanup saved coast mask



                                    // Create land mask

                                    pm.setSubTaskName("Creating mask: " + landMasksData.getLandMaskName());

                                    Mask landMask = Mask.BandMathsType.create(
                                            landMasksData.getLandMaskName(),
                                            landMasksData.getLandMaskDescription(),
                                            product.getSceneRasterWidth(),
                                            product.getSceneRasterHeight(),
                                            landMasksData.getLandMaskMath(),
                                            landMasksData.getLandMaskColor(),
                                            landMasksData.getLandMaskTransparency());

                                    workDone += sleepPreviewThread(sleepShort,4, pm, totalWork, workDone);



                                    try {
                                        Mask landMaskTmp = maskGroup.get(landMask.getName());
                                        if (landMaskTmp != null) {
                                            pm.setSubTaskName("Deleting existing mask: " + landMasksData.getLandMaskName());
                                            maskGroup.remove(landMaskTmp);
                                            workDone += sleepPreviewThread(sleepShort,4, pm, totalWork, workDone);
                                        }

                                        pm.setSubTaskName("Adding mask: " + landMasksData.getLandMaskName());
                                        workDone += sleepPreviewThread(sleepLong,8, pm, totalWork, workDone);
                                        maskGroup.add(landMask);
                                        workDone += sleepPreviewThread(sleepShort,4, pm, totalWork, workDone);
                                        pm.setSubTaskName("Mask created: " + landMasksData.getLandMaskName());



                                        // Cleanup saved land mask
                                        Mask savedMask = maskGroup.get(savedLandMaskName);
                                        if (savedMask != null) {
                                            maskGroup.remove(savedMask);
                                            workDone += sleepPreviewThread(sleepShort,4, pm, totalWork, workDone);

                                        }
                                        // end cleanup saved land mask



                                    } catch (Exception e) {
                                        pm.setSubTaskName("ERROR!! Mask not created: '" + landMasksData.getLandMaskName());
                                    }


                                    workDone += sleepPreviewThread(sleepShort,4, pm, totalWork, workDone);




                                    // Create water mask

                                    pm.setSubTaskName("Creating mask: " + landMasksData.getWaterMaskName());

                                    Mask waterMask = Mask.BandMathsType.create(
                                            landMasksData.getWaterMaskName(),
                                            landMasksData.getWaterMaskDescription(),
                                            product.getSceneRasterWidth(),
                                            product.getSceneRasterHeight(),
                                            landMasksData.getWaterMaskMath(),
                                            landMasksData.getWaterMaskColor(),
                                            landMasksData.getWaterMaskTransparency());


                                    workDone += sleepPreviewThread(sleepShort,4, pm, totalWork, workDone);

                                    try {
                                        Mask waterMaskTmp = maskGroup.get(waterMask.getName());
                                        if (waterMaskTmp != null) {
                                            pm.setSubTaskName("Deleting existing mask: " + landMasksData.getWaterMaskName());
                                            maskGroup.remove(waterMaskTmp);
                                            workDone += sleepPreviewThread(sleepShort,4, pm, totalWork, workDone);
                                        }

                                        pm.setSubTaskName("Adding mask: " + landMasksData.getWaterMaskName());
                                        workDone += sleepPreviewThread(sleepLong,8, pm, totalWork, workDone);
                                        maskGroup.add(waterMask);
                                        workDone += sleepPreviewThread(sleepShort,4, pm, totalWork, workDone);
                                        pm.setSubTaskName("Mask created: " + landMasksData.getWaterMaskName());

                                        // Cleanup saved water mask
                                        Mask savedMask = maskGroup.get(savedWaterMaskName);
                                        if (savedMask != null) {
                                            maskGroup.remove(savedMask);
                                            workDone += sleepPreviewThread(sleepShort,4, pm, totalWork, workDone);
                                        }
                                        // end cleanup saved water mask



                                    } catch (Exception e) {
                                        pm.setSubTaskName("ERROR!! Mask not created: '" + landMasksData.getWaterMaskName());
                                    }

                                    workDone += sleepPreviewThread(sleepShort,4, pm, totalWork, workDone);



                                    if (landMasksData.isShowLandMaskAllBands()
                                    || landMasksData.isShowWaterMaskAllBands()
                                    || landMasksData.isShowCoastlineMaskAllBands()) {
                                        pm.setSubTaskName("Selecting masks in all bands");

                                        String[] bandNames = product.getBandNames();
                                        for (String bandName : bandNames) {
                                            RasterDataNode raster = product.getRasterDataNode(bandName);
                                            if (raster != null) {
                                                if (landMasksData.isShowCoastlineMaskAllBands()
                                                        && createCoastBand
                                                        && coastlineMask != null
                                                        && raster.getOverlayMaskGroup() != null) {
                                                    if (coastlineMask != null) {
                                                        raster.getOverlayMaskGroup().add(coastlineMask);
                                                    }
                                                }
                                                if (landMasksData.isShowLandMaskAllBands()  && raster.getOverlayMaskGroup() != null) {
                                                    if (landMask != null) {
                                                        raster.getOverlayMaskGroup().add(landMask);
                                                    }
                                                }
                                                if (landMasksData.isShowWaterMaskAllBands() && raster.getOverlayMaskGroup() != null) {
                                                    if (waterMask != null) {
                                                        raster.getOverlayMaskGroup().add(waterMask);
                                                    }
                                                }
                                            }
                                        }
                                    }


                                    //snapApp.setSelectedProductNode(waterFractionBand);

//                                    ProductSceneView selectedProductSceneView = snapApp.getSelectedProductSceneView();
//                                    if (selectedProductSceneView != null) {
//                                        RasterDataNode raster = selectedProductSceneView.getRaster();
//                                        raster.getOverlayMaskGroup().add(landMask);
//                                        raster.getOverlayMaskGroup().add(coastlineMask);
//                                        raster.getOverlayMaskGroup().add(waterMask);
//
//                                    }

                                } finally {
                                    pm.setSubTaskName("Land Water Coast Mask Tool run completed");
                                    sleepPreviewThread(sleepShort);

                                    pm.done();
                                }
                                return null;
                            }


                        };

                        pmSwingWorker.executeWithBlocking();


                    } else {
                        SimpleDialogMessage dialog = new SimpleDialogMessage(null, "Cannot Create Masks: Resolution File Doesn't Exist");
                        dialog.setVisible(true);
                        dialog.setEnabled(true);

                    }
                }
            }


        }

    }


    private boolean bandsAreOpenForThisFile(Product product) {
        boolean bandsAreOpenForThisFile = false;
        Band[] bands = product.getBands();
        for (Band band : bands) {
            if (band.getImageInfo() != null) {
                bandsAreOpenForThisFile = true;
            }
        }

        return bandsAreOpenForThisFile;
    }


    private int incrementWork(com.bc.ceres.core.ProgressMonitor pm, int totalWork, int workDone) {
        if (workDone < totalWork) {
            pm.worked(1);
            return 1;
        } else {
            return 0;
        }
    }


    private void sleepPreviewThread(long milliSeconds) {
//        return;
        try {
            Thread.sleep(milliSeconds);
        } catch (InterruptedException e3) {
            Thread.currentThread().interrupt();
        }
    }

    private int sleepPreviewThread(long milliSeconds, int numDivisions, com.bc.ceres.core.ProgressMonitor pm, int totalWork, int workDone) {

//        return 4;

        int workDoneInitial = workDone;

        if (numDivisions <= 0) {
            return 0;
        }
        double sleepTimeIncrement = milliSeconds / (double) numDivisions;
        long sleepTimeIncrementLong = (long) sleepTimeIncrement;
        try {
            for (int i = 0; i < numDivisions; i++) {
                Thread.sleep(sleepTimeIncrementLong);
                workDone += incrementWork(pm, totalWork, workDone);
            }
            return  workDone - workDoneInitial;
        } catch (InterruptedException e3) {
            Thread.currentThread().interrupt();
            return 0;
        }
    }

    private void reformatSourceImage(Band band, ImageLayout imageLayout, com.bc.ceres.core.ProgressMonitor pm) {
        RenderingHints renderingHints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, imageLayout);
        MultiLevelImage waterFractionSourceImage = band.getSourceImage();
        int waterFractionDataType = waterFractionSourceImage.getData().getDataBuffer().getDataType();
        RenderedImage newImage = FormatDescriptor.create(waterFractionSourceImage, waterFractionDataType,
                renderingHints);

        sleepPreviewThread(1000);

        band.setSourceImage(newImage);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
//        final AppContext appContext = getAppContext();
//        setOverlayEnableState(SnapApp.getDefault().getSelectedProductSceneView());
//        updateActionState();
        showLandWaterCoastMasks(SnapApp.getDefault());
//        final DefaultSingleTargetProductDialog dialog = new DefaultSingleTargetProductDialog(LAND_WATER_MASK_OP_ALIAS, appContext,
//                "Land Water Mask3",
//                HELP_ID);
//        dialog.setTargetProductNameSuffix("_watermask3");
//        dialog.getJDialog().pack();
//        dialog.show();
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
//    public void start(final SnapApp snapApp) {
//        final ExecCommand action = snapApp.getCommandManager().createExecCommand(COMMAND_ID,
//                new ToolbarCommand(snapApp));
//
//        String iconFilename = ResourceInstallationUtils.getIconFilename(ICON, WaterMaskAction.class);
//        //  action.setLargeIcon(UIUtils.loadImageIcon(ICON));
//        try {
//            URL iconUrl = new URL(iconFilename);
//            ImageIcon imageIcon = new ImageIcon(iconUrl);
//            action.setLargeIcon(imageIcon);
//        } catch (MalformedURLException e) {
//            e.printStackTrace();
//        }
//
//
//        final AbstractButton lwcButton = snapApp.createToolButton(COMMAND_ID);
//        lwcButton.setToolTipText(TOOL_TIP);
//
//        final AbstractButton lwcButton2 = snapApp.createToolButton(COMMAND_ID);
//        lwcButton2.setToolTipText(TOOL_TIP);
//
//        snapApp.getMainFrame().addWindowListener(new WindowAdapter() {
//            @Override
//            public void windowOpened(WindowEvent e) {
//                CommandBar layersBar = snapApp.getToolBar(TARGET_TOOL_BAR_NAME);
//                if (layersBar != null) {
//                    layersBar.add(lwcButton);
//                }
//
//
//                CommandBar seadasDefaultBar = snapApp.getToolBar("seadasDeluxeToolsToolBar");
//                if (seadasDefaultBar != null) {
//                    seadasDefaultBar.add(lwcButton2);
//                }
//            }
//
//        });
//    }
//
//
//    private class ToolbarCommand extends CommandAdapter {
//        private final SnapApp snapApp;
//
//        public ToolbarCommand(SnapApp snapApp) {
//            this.snapApp = snapApp;
//        }
//
//        @Override
//        public void actionPerformed(
//                CommandEvent event) {
//            showLandWaterCoastMasks(
//                    snapApp);
//
//        }
//
//        @Override
//        public void updateState(
//                CommandEvent event) {
//            Product selectedProduct = snapApp.getSelectedProduct();
//            boolean productSelected = selectedProduct != null;
//            boolean hasBands = false;
//            boolean hasGeoCoding = false;
//            if (productSelected) {
//                hasBands = selectedProduct.getNumBands() > 0;
//                hasGeoCoding = selectedProduct.getGeoCoding() != null;
//            }
//            event.getCommand().setEnabled(
//                    productSelected && hasBands && hasGeoCoding);
//        }
//    }
}

