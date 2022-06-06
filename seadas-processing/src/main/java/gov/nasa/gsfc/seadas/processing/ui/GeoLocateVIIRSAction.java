package gov.nasa.gsfc.seadas.processing.ui;

import gov.nasa.gsfc.seadas.processing.common.CallCloProgramAction;
import gov.nasa.gsfc.seadas.processing.ocssw.OCSSWInfo;
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
        category = "Processing", id = "gov.nasa.gsfc.seadas.processing.ui.GeoLocateVIIRSAction"
)
@ActionRegistration(
        displayName = "#CTL_ GeoLocateVIIRSAction_Name",
        popupText = "#CTL_ GeoLocateVIIRSAction_Name"
)
@ActionReference(
        path = "Menu/SeaDAS-Toolbox/OCSSW Processors",
        position = 60
)
@NbBundle.Messages({
        "CTL_GeoLocateVIIRSAction_Name=geolocate_viirs...",
        "CTL_GeoLocateVIIRSAction_ProgramName=geolocate_viirs",
        "CTL_GeoLocateVIIRSAction_DialogTitle=geolocate_viirs",
        "CTL_GeoLocateVIIRSAction_XMLFileName=geolocate_viirs.xml",
        "CTL_GeoLocateVIIRSAction_Description=Takes a VIIRS L1A file and outputs geolocation files"
})

public class GeoLocateVIIRSAction extends CallCloProgramAction implements ContextAwareAction, LookupListener{

    private final Lookup lkp;

    public  GeoLocateVIIRSAction() {
        this(Utilities.actionsGlobalContext());
    }

    public   GeoLocateVIIRSAction(Lookup lkp) {
        this.lkp = lkp;
        Lookup.Result<OCSSWInfo> lkpContext = lkp.lookupResult(OCSSWInfo.class);
        lkpContext.addLookupListener(WeakListeners.create(LookupListener.class, this, lkpContext));
        putValue(Action.NAME, Bundle.CTL_GeoLocateVIIRSAction_Name());
        putValue(Action.SHORT_DESCRIPTION, Bundle.CTL_GeoLocateVIIRSAction_Description());
        setProgramName(Bundle.CTL_GeoLocateVIIRSAction_ProgramName());
        setDialogTitle(Bundle.CTL_GeoLocateVIIRSAction_DialogTitle());
        setXmlFileName(Bundle.CTL_GeoLocateVIIRSAction_XMLFileName());
        //setEnableState();
    }

    @Override
    public void resultChanged(LookupEvent lookupEvent) {
        //setEnableState();
    }

    public void setEnableState() {
        boolean state = false;
       // OCSSWInfo ocsswInfo = lkp.lookup(OCSSWInfo.class);
        if (ocsswInfo != null) {
            state = ocsswInfo.isOCSSWExist();
        }
        setEnabled(state);
    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new GeoLocateVIIRSAction(actionContext);
    }
}