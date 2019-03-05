package gov.nasa.gsfc.seadas.watermask.ui;

import org.esa.snap.core.gpf.ui.DefaultSingleTargetProductDialog;
import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.esa.snap.ui.AppContext;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import java.awt.event.ActionEvent;

@ActionID(category = "Processing", id = "gov.nasa.gsfc.seadas.watermask.ui.WaterMaskAction" )
@ActionRegistration(displayName = "#CTL_WaterMaskAction_Text")
@ActionReference(path = "Menu/Raster/Masks", position = 300)
@NbBundle.Messages({"CTL_WaterMaskAction_Text=LandWaterMask3"})
public class WaterMaskAction extends AbstractSnapAction {

    private static final String OPERATOR_ALIAS = "LandWaterMask2";
    private static final String HELP_ID = "watermaskScientificTool";

    public WaterMaskAction() {
        putValue(SHORT_DESCRIPTION, "Creating an accurate, fractional, shapefile-based land-water mask.");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final AppContext appContext = getAppContext();

        final DefaultSingleTargetProductDialog dialog = new DefaultSingleTargetProductDialog(OPERATOR_ALIAS, appContext,
                "Land Water Mask3",
                HELP_ID);
        dialog.setTargetProductNameSuffix("_watermask3");
        dialog.getJDialog().pack();
        dialog.show();
    }
}
