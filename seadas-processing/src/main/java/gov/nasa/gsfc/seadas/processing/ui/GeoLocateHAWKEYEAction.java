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
        category = "Processing", id = "gov.nasa.gsfc.seadas.processing.ui.GeoLocateHAWKEYEAction"
)
@ActionRegistration(
        displayName = "#CTL_ GeoLocateHAWKEYEAction_Name",
        popupText = "#CTL_ GeoLocateHAWKEYEAction_Name"
)
@ActionReference(
        path = "Menu/SeaDAS-Toolbox/SeaDAS Processors/HawkEye",
        position = 60
)
@NbBundle.Messages({
        "CTL_GeoLocateHAWKEYEAction_Name=geolocate_hawkeye...",
        "CTL_GeoLocateHAWKEYEAction_ProgramName=geolocate_hawkeye",
        "CTL_GeoLocateHAWKEYEAction_DialogTitle=geolocate_hawkeye",
        "CTL_GeoLocateHAWKEYEAction_XMLFileName=geolocate_hawkeye.xml",
        "CTL_GeoLocateHAWKEYEAction_Description=Creates a HawkEye GEO file from a HawkEye L1A input file"
})

public class GeoLocateHAWKEYEAction extends CallCloProgramAction implements ContextAwareAction, LookupListener{

    private final Lookup lkp;

    public  GeoLocateHAWKEYEAction() {
        this(Utilities.actionsGlobalContext());
    }

    public   GeoLocateHAWKEYEAction(Lookup lkp) {
        this.lkp = lkp;
        Lookup.Result<OCSSWInfo> lkpContext = lkp.lookupResult(OCSSWInfo.class);
        lkpContext.addLookupListener(WeakListeners.create(LookupListener.class, this, lkpContext));
        putValue(Action.NAME, Bundle.CTL_GeoLocateHAWKEYEAction_Name());
        putValue(Action.SHORT_DESCRIPTION, Bundle.CTL_GeoLocateHAWKEYEAction_Description());
        setProgramName(Bundle.CTL_GeoLocateHAWKEYEAction_ProgramName());
        setDialogTitle(Bundle.CTL_GeoLocateHAWKEYEAction_DialogTitle());
        setXmlFileName(Bundle.CTL_GeoLocateHAWKEYEAction_XMLFileName());
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
        return new GeoLocateHAWKEYEAction(actionContext);
    }
}