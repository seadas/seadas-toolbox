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
        category = "Processing", id = "gov.nasa.gsfc.seadas.processing.ui.CalibrateVIIRSAction"
)
@ActionRegistration(
        displayName = "#CTL_ CalibrateVIIRSAction_Name",
        popupText = "#CTL_ CalibrateVIIRSAction_Name"
)
@ActionReference(
        path = "Menu/SeaDAS OCSSW",
        position = 70
)
@NbBundle.Messages({
        "CTL_CalibrateVIIRSAction_Name=calibrate_viirs...",
        "CTL_CalibrateVIIRSAction_ProgramName=calibrate_viirs",
        "CTL_CalibrateVIIRSAction_DialogTitle=calibrate_viirs",
        "CTL_CalibrateVIIRSAction_XMLFileName=calibrate_viirs.xml",
        "CTL_CalibrateVIIRSAction_Description=Takes a VIIRS L1A file and outputs an L1B file."
})

public class CalibrateVIIRSAction  extends CallCloProgramAction implements ContextAwareAction, LookupListener {

    private static final String OPERATOR_ALIAS = "CalibrateVIIRS";
    private static final String HELP_ID = "calibrate_viirs";

    private final Lookup lkp;



    public  CalibrateVIIRSAction() {
        this(Utilities.actionsGlobalContext());
    }

    public   CalibrateVIIRSAction(Lookup lkp) {
        this.lkp = lkp;

        Lookup.Result<OCSSWInfo> lkpContext = lkp.lookupResult(OCSSWInfo.class);
        lkpContext.addLookupListener(WeakListeners.create(LookupListener.class, this, lkpContext));
        putValue(Action.NAME, Bundle.CTL_CalibrateVIIRSAction_Name());
        putValue(Action.SHORT_DESCRIPTION, Bundle.CTL_CalibrateVIIRSAction_Description());
        setProgramName(Bundle.CTL_CalibrateVIIRSAction_ProgramName());
        setDialogTitle(Bundle.CTL_CalibrateVIIRSAction_DialogTitle());
        setXmlFileName(Bundle.CTL_CalibrateVIIRSAction_XMLFileName());
    }


    @Override
    public void resultChanged(LookupEvent lookupEvent) {
        setEnabled(true);
    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new CalibrateVIIRSAction(actionContext);
    }
}