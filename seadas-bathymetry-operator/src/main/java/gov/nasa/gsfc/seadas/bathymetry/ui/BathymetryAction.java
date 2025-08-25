package gov.nasa.gsfc.seadas.bathymetry.ui;

import com.bc.ceres.multilevel.MultiLevelImage;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import gov.nasa.gsfc.seadas.bathymetry.operator.BathymetryOp;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.*;
import org.openide.util.actions.Presenter;

import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.operator.FormatDescriptor;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.util.HashMap;
import java.util.Map;


/**
 * This registers an action which calls the "bathymetry" Operator
 *
 * @author Danny Knowles
 * @author Aynur Abdurazik
 * @author Bing Yang
 */
@ActionID(category = "Processing", id = "gov.nasa.gsfc.seadas.bathymetry.ui.BathymetryAction" )
@ActionRegistration(displayName = "#CTL_BathymetryAction_Text", lazy = false)
//        iconBase = "gov/nasa/gsfc/seadas/bathymetry/ui/icons/bathymetry.png")
@ActionReferences({
        @ActionReference(path = "Menu/SeaDAS-Toolbox/General Tools", position = 10),
        @ActionReference(path = "Menu/Raster/Masks"),
        @ActionReference(path = "Toolbars/SeaDAS Toolbox", position = 10)
})
@NbBundle.Messages({
        "CTL_BathymetryAction_Text=Bathymetry",
        "CTL_BathymetryAction_Description=Add bathymetry-elevation band and mask."
})

public final class BathymetryAction extends AbstractSnapAction
        implements LookupListener, Presenter.Menu, Presenter.Toolbar  {

    public static final String COMMAND_ID = "Bathymetry & Elevation";
    public static final String TOOL_TIP = "Add bathymetry-elevation band and mask";
    public static final String SMALLICON = "gov/nasa/gsfc/seadas/bathymetry/ui/icons/bathymetry.png";
    public static final String LARGEICON = "gov/nasa/gsfc/seadas/bathymetry/ui/icons/bathymetry24.png";

    public static final String TARGET_TOOL_BAR_NAME = "layersToolBar";
    public static final String BATHYMETRY_PRODUCT_NAME = "BathymetryOp";

    private final Lookup lookup;
    private final Lookup.Result<ProductNode> viewResult;

    public  BathymetryAction() {
        this(null);
    }

    public BathymetryAction(Lookup lookup) {
        putValue(ACTION_COMMAND_KEY, getClass().getName());
        putValue(SELECTED_KEY, false);
        putValue(NAME, Bundle.CTL_BathymetryAction_Text());
        putValue(SMALL_ICON, ImageUtilities.loadImageIcon(SMALLICON, false));
        putValue(LARGE_ICON_KEY, ImageUtilities.loadImageIcon(LARGEICON, false));
        putValue(SHORT_DESCRIPTION, Bundle.CTL_BathymetryAction_Description());
        this.lookup = lookup != null ? lookup : Utilities.actionsGlobalContext();
        this.viewResult = this.lookup.lookupResult(ProductNode.class);
        this.viewResult.addLookupListener(WeakListeners.create(LookupListener.class, this, viewResult));
        updateEnabledState();
    }
//    @Override
//    public void start(final SnapApp snapApp) {
//        final ExecCommand action = snapApp.getCommandManager().createExecCommand(COMMAND_ID,
//                new ToolbarCommand(visatApp));
//
//       String iconFilename = ResourceInstallationUtils.getIconFilename(ICON, BathymetryVPI.class);
//
//        try {
//            URL iconUrl = new URL(iconFilename);
//            ImageIcon imageIcon = new ImageIcon(iconUrl);
//            action.setLargeIcon(imageIcon);
//        } catch (MalformedURLException e) {
//           e.printStackTrace();
//        }
//
//
//        final AbstractButton lwcButton = visatApp.createToolButton(COMMAND_ID);
//        lwcButton.setToolTipText(TOOL_TIP);
//
//        visatApp.getMainFrame().addWindowListener(new WindowAdapter() {
//            @Override
//            public void windowOpened(WindowEvent e) {
//                CommandBar layersBar = visatApp.getToolBar(TARGET_TOOL_BAR_NAME);
//                layersBar.add(lwcButton);
//            }
//        });
//
//        final AbstractButton lwcButton = visatApp.createToolButton(COMMAND_ID);
//        lwcButton.setToolTipText(TOOL_TIP);
//
//        final AbstractButton lwcButton2 = visatApp.createToolButton(COMMAND_ID);
//        lwcButton2.setToolTipText(TOOL_TIP);
//
//        visatApp.getMainFrame().addWindowListener(new WindowAdapter() {
//            @Override
//            public void windowOpened(WindowEvent e) {
//                CommandBar layersBar = visatApp.getToolBar(TARGET_TOOL_BAR_NAME);
//                if (layersBar != null) {
//                    layersBar.add(lwcButton);
//                }
//
//
//                CommandBar seadasDefaultBar = visatApp.getToolBar("seadasDeluxeToolsToolBar");
//               if (seadasDefaultBar != null) {
//                    seadasDefaultBar.add(lwcButton2);
//                }
//            }
//
//        });
//
//
//    }


    private void showBathymetry(final SnapApp snapApp) {
        final Product product = snapApp.getSelectedProduct(SnapApp.SelectionSourceHint.AUTO);
        if (product != null) {
            final ProductNodeGroup<Mask> maskGroup = product.getMaskGroup();
            final ProductNodeGroup<Band> bandGroup = product.getBandGroup();

            /*
               A simple boolean switch to enable this to run with or without the intermediate user dialogs
            */
            boolean useDialogs = true;

            final BathymetryData bathymetryData = new BathymetryData();


            if (!useDialogs) {
                bathymetryData.setCreateMasks(true);
            }


            /*
                Determine whether these auxilliary masks and associated products have already be created.
                This would be the case when run a second time on the same product.
            */

            final boolean[] masksCreated = {false};
            final boolean[] bandCreated = {false};

            for (String name : maskGroup.getNodeNames()) {
                if (name.equals(bathymetryData.getMaskName())) {
                    masksCreated[0] = true;
                }
            }


            for (String name : bandGroup.getNodeNames()) {
                if (name.equals(BathymetryOp.BATHYMETRY_BAND_NAME)) {
                    bandCreated[0] = true;
                }
            }

            /*
                For the case where this is being run a second time, prompt the user to determine whether to delete
                and re-create the products and masks.
             */


            if (masksCreated[0]) {
                bandCreated[0] = true;

                if (useDialogs) {
                    bathymetryData.setDeleteMasks(false);
                    BathymetryDialog bathymetryDialog = new BathymetryDialog(bathymetryData, masksCreated[0], bandCreated[0]);
                    bathymetryDialog.setVisible(true);
                    bathymetryDialog.dispose();
                }

                if (bathymetryData.isDeleteMasks() || !useDialogs) {
                    masksCreated[0] = false;


//                    for (String name : bandGroup.getNodeNames()) {
//                        if (
//                                name.equals(bathymetryData.getBathymetryBandName())) {
//  //                          Band bathymetryBand = bandGroup.get(name);
//
////                            product.getBand(name).dispose();
//
//                            bandGroup.remove(bandGroup.get(name));
////                            product.removeBand(bathymetryBand);
//
//                        }
//                    }

                    for (String name : maskGroup.getNodeNames()) {
                        if (name.equals(bathymetryData.getMaskName())) {
//                            maskGroup.get(name).dispose();
                            maskGroup.remove(maskGroup.get(name));
                        }
                    }


                }
            }


            if (!masksCreated[0]) {
                if (useDialogs) {
                    bathymetryData.setCreateMasks(false);
                    BathymetryDialog bathymetryDialog = new BathymetryDialog(bathymetryData, masksCreated[0], bandCreated[0]);
                    bathymetryDialog.setVisible(true);
                }

                if (bathymetryData.isCreateMasks()) {
                    final SourceFileInfo sourceFileInfo = bathymetryData.getSourceFileInfo();


                    if (sourceFileInfo.isEnabled() && sourceFileInfo.getExistingFile() != null) {


                        final String[] msg = {"Creating bathymetry band and mask"};

                        if (bandCreated[0] == true) {
                            msg[0] = "recreating bathymetry mask";
                        }
                        ProgressMonitorSwingWorker pmSwingWorker = new ProgressMonitorSwingWorker(snapApp.getMainFrame(),
                                msg[0]) {

                            @Override
                            protected Void doInBackground(com.bc.ceres.core.ProgressMonitor pm) throws Exception {

                                int totalWork = 30;
                                int workDone = 0;
                                pm.beginTask(msg[0], totalWork);

                                try {

                                    if (bandCreated[0] != true) {
                                        Map<String, Object> parameters = new HashMap<String, Object>();

                                        parameters.put("resolution", sourceFileInfo.getResolution(SourceFileInfo.Unit.METER));
                                        parameters.put("filename", sourceFileInfo.getExistingFile().getName());

                                        /*
                                           Create a new product, which will contain the bathymetry band, then add this band to current product.
                                        */

                                        pm.setSubTaskName("Running operator: " + BATHYMETRY_PRODUCT_NAME);
                                        Product bathymetryProduct = null;

                                        try {
                                            bathymetryProduct = GPF.createProduct(BATHYMETRY_PRODUCT_NAME, parameters, product);
                                            workDone += sleepPreviewThread(1000,4, pm, totalWork, workDone);

                                            if (bathymetryProduct == null) {
                                                pm.setSubTaskName("Operator Failed: " + BATHYMETRY_PRODUCT_NAME);
                                                return null;
                                            }
                                        } catch (OperatorException e) {
                                            pm.setSubTaskName("Operator Failed: " + BATHYMETRY_PRODUCT_NAME);
                                            return null;
                                        }



                                        if (bathymetryData.isCreateTopographyBand()) {
                                            pm.setSubTaskName("Creating band '" + BathymetryOp.TOPOGRAPHY_BAND_NAME + "'");
                                            Band topographyBand = bathymetryProduct.getBand(BathymetryOp.TOPOGRAPHY_BAND_NAME);
                                            reformatSourceImage(topographyBand, new ImageLayout(product.getBandAt(0).getSourceImage()));
                                            workDone += sleepPreviewThread(1000,4, pm, totalWork, workDone);

                                            pm.setSubTaskName("Adding band '" + BathymetryOp.TOPOGRAPHY_BAND_NAME + "'");
                                            topographyBand.setName(BathymetryOp.TOPOGRAPHY_BAND_NAME);
                                            product.addBand(topographyBand);
                                        }


                                        if (bathymetryData.isCreateElevationBand()) {
                                            pm.setSubTaskName("Creating band '" + BathymetryOp.ELEVATION_BAND_NAME + "'");
                                            Band elevationBand = bathymetryProduct.getBand(BathymetryOp.ELEVATION_BAND_NAME);
                                            reformatSourceImage(elevationBand, new ImageLayout(product.getBandAt(0).getSourceImage()));
                                            workDone += sleepPreviewThread(1000,4, pm, totalWork, workDone);

                                            pm.setSubTaskName("Adding band '" + BathymetryOp.ELEVATION_BAND_NAME + "'");
                                            elevationBand.setName(BathymetryOp.ELEVATION_BAND_NAME);
                                            product.addBand(elevationBand);
                                        }

                                        pm.setSubTaskName("Creating band '" + BathymetryOp.BATHYMETRY_BAND_NAME + "'");
                                        Band bathymetryBand = bathymetryProduct.getBand(BathymetryOp.BATHYMETRY_BAND_NAME);
                                        reformatSourceImage(bathymetryBand, new ImageLayout(product.getBandAt(0).getSourceImage()));
                                        workDone += sleepPreviewThread(1000,4, pm, totalWork, workDone);

                                        pm.setSubTaskName("Adding band '" + BathymetryOp.BATHYMETRY_BAND_NAME + "'");
                                        bathymetryBand.setName(BathymetryOp.BATHYMETRY_BAND_NAME);
                                        product.addBand(bathymetryBand);

                                    }

                                    workDone += incrementWork(pm, totalWork, workDone);

                                    pm.setSubTaskName("Creating mask '" + bathymetryData.getMaskName() + "'");

                                    Mask bathymetryMask = Mask.BandMathsType.create(
                                            bathymetryData.getMaskName(),
                                            bathymetryData.getMaskDescription(),
                                            product.getSceneRasterWidth(),
                                            product.getSceneRasterHeight(),
                                            bathymetryData.getMaskMath(),
                                            bathymetryData.getMaskColor(),
                                            bathymetryData.getMaskTransparency());
                                    maskGroup.add(bathymetryMask);

                                    workDone += sleepPreviewThread(1000,4, pm, totalWork, workDone);


                                    if (bathymetryData.isShowMaskAllBands()) {
                                        pm.setSubTaskName("Enabling masks in all bands");

                                        String[] bandNames = product.getBandNames();
                                        for (String bandName : bandNames) {
                                            RasterDataNode raster = product.getRasterDataNode(bandName);
                                            raster.getOverlayMaskGroup().add(bathymetryMask);
                                        }
                                    }


                                } finally {
                                    pm.setSubTaskName("Bathymetry Tool run finished");
                                    sleepPreviewThread(500);

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



    private void sleepPreviewThread(long milliSeconds) {
        try {
            Thread.sleep(milliSeconds);
        } catch (InterruptedException e3) {
            Thread.currentThread().interrupt();
        }
    }

    private int sleepPreviewThread(long milliSeconds, int numDivisions, com.bc.ceres.core.ProgressMonitor pm, int totalWork, int workDone) {

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

    private int incrementWork(com.bc.ceres.core.ProgressMonitor pm, int totalWork, int workDone) {
        if (workDone < totalWork) {
            pm.worked(1);
            return 1;
        } else {
            return 0;
        }
    }


    private void reformatSourceImage(Band band, ImageLayout imageLayout) {
        RenderingHints renderingHints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, imageLayout);
        MultiLevelImage sourceImage = band.getSourceImage();
        Raster r = sourceImage.getData();
        DataBuffer db = r.getDataBuffer();
        int t = db.getDataType();
        int dataType = sourceImage.getData().getDataBuffer().getDataType();
        RenderedImage newImage = FormatDescriptor.create(sourceImage, dataType, renderingHints);
        band.setSourceImage(newImage);
    }

//    private class ToolbarCommand extends CommandAdapter {
//        private final VisatApp visatApp;

//        public ToolbarCommand(VisatApp visatApp) {
//            this.visatApp = visatApp;
//        }

    @Override
    public void actionPerformed(ActionEvent e) {
        showBathymetry(SnapApp.getDefault());
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



//        @Override
//        public void updateState(CommandEvent event) {
//            Product selectedProduct = visatApp.getSelectedProduct();
//            boolean productSelected = selectedProduct != null;
//            boolean hasBands = false;
//            boolean hasGeoCoding = false;
//            if (productSelected) {
//                hasBands = selectedProduct.getNumBands() > 0;
//                hasGeoCoding = selectedProduct.getGeoCoding() != null;
//           }
//            event.getCommand().setEnabled(productSelected && hasBands && hasGeoCoding);
//        }
//    }
//}

