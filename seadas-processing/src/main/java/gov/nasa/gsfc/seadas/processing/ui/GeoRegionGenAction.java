package gov.nasa.gsfc.seadas.processing.ui;

import gov.nasa.gsfc.seadas.processing.common.CallCloProgramAction;
import org.esa.snap.core.datamodel.ProductNode;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.*;

import javax.swing.*;

/**
 * @author Daniel Knowles
 * @since SeaDAS 8.0
 * @see
 */
@ActionID(
        category = "Processing", id = "gov.nasa.gsfc.seadas.processing.ui.GeoRegionGenAction"
)
@ActionRegistration(
        displayName = "#CTL_GeoRegionGenAction_Name",
        popupText = "#CTL_GeoRegionGenAction_Description"
)
@ActionReference(
        path = "Menu/SeaDAS-Toolbox/SeaDAS Processors/Tools",
        position = 150
)
@NbBundle.Messages({
        "CTL_GeoRegionGenAction_Name=georegion_gen...",
        "CTL_GeoRegionGenAction_ProgramName=georegion_gen",
        "CTL_GeoRegionGenAction_DialogTitle=georegion_gen",
        "CTL_GeoRegionGenAction_XMLFileName=georegion_gen.xml",
        "CTL_GeoRegionGenAction_Description=Generates a mask file from input WKT or Shapefiles"
})

public class GeoRegionGenAction extends CallCloProgramAction implements ContextAwareAction, LookupListener {

    private final Lookup lkp;

    public GeoRegionGenAction() {
        this(Utilities.actionsGlobalContext());
    }

    public GeoRegionGenAction(Lookup lkp) {
        this.lkp = lkp;
        Lookup.Result<ProductNode> lkpContext = lkp.lookupResult(ProductNode.class);
        lkpContext.addLookupListener(WeakListeners.create(LookupListener.class, this, lkpContext));
        putValue(Action.NAME, Bundle.CTL_GeoRegionGenAction_Name());
        putValue(Action.SHORT_DESCRIPTION, Bundle.CTL_GeoRegionGenAction_Description());
        setProgramName(Bundle.CTL_GeoRegionGenAction_ProgramName());
        setDialogTitle(Bundle.CTL_GeoRegionGenAction_DialogTitle());
        setXmlFileName(Bundle.CTL_GeoRegionGenAction_XMLFileName());
    }

    @Override
    public void resultChanged(LookupEvent lookupEvent) {

    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new GeoRegionGenAction(actionContext);
    }
}
