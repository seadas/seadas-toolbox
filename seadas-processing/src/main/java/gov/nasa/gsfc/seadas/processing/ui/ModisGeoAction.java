package gov.nasa.gsfc.seadas.processing.ui;

import gov.nasa.gsfc.seadas.processing.common.CallCloProgramAction;
import org.esa.snap.core.datamodel.ProductNode;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.*;

import javax.swing.*;

/**
 * @author Aynur Abdurazik
 * @since SeaDAS 8.0
 * @see
 */
@ActionID(
        category = "Processing", id = "gov.nasa.gsfc.seadas.processing.ui.ModisGeoAction"
)
@ActionRegistration(
        displayName = "#CTL_ ModisGeoAction_Name",
        popupText = "#CTL_ ModisGeoAction_Name"
)
@ActionReference(
        path = "Menu/SeaDAS-OCSSW",
        position = 40
)
@NbBundle.Messages({
        "CTL_ModisGeoAction_Name=modis_GEO...",
        "CTL_ModisGeoAction_ProgramName=modis_GEO.py",
        "CTL_ModisGeoAction_DialogTitle=modis_GEO",
        "CTL_ModisGeoAction_XMLFileName=modis_GEO.xml",
        "CTL_ModisGeoAction_Description=Process MODIS L0 to GEO."
})
public class ModisGeoAction extends CallCloProgramAction implements ContextAwareAction, LookupListener {

    private final Lookup lkp;

    public  ModisGeoAction() {
        this(Utilities.actionsGlobalContext());
    }

    public   ModisGeoAction(Lookup lkp) {
        this.lkp = lkp;
        Lookup.Result<ProductNode> lkpContext = lkp.lookupResult(ProductNode.class);
        lkpContext.addLookupListener(WeakListeners.create(LookupListener.class, this, lkpContext));
        putValue(Action.NAME, Bundle.CTL_ModisGeoAction_Name());
        putValue(Action.SHORT_DESCRIPTION, Bundle.CTL_ModisGeoAction_Description());
        setProgramName(Bundle.CTL_ModisGeoAction_ProgramName());
        setDialogTitle(Bundle.CTL_ModisGeoAction_DialogTitle());
        setXmlFileName(Bundle.CTL_ModisGeoAction_XMLFileName());
    }
//
//    @Override
//    public void actionPerformed(ActionEvent e) {
//
//    }

    @Override
    public void resultChanged(LookupEvent lookupEvent) {

    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new ModisGeoAction(actionContext);
    }
}