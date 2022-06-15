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
        category = "Processing", id = "gov.nasa.gsfc.seadas.processing.ui.ExtractorAction"
)
@ActionRegistration(
        displayName = "#CTL_ ExtractorAction_Name",
        popupText = "#CTL_ ExtractorAction_Name"
)
@ActionReference(
        path = "Menu/SeaDAS-Toolbox/SeaDAS Processors",
        position = 10,
        separatorAfter = 11)
@NbBundle.Messages({
        "CTL_ExtractorAction_Name=extractors...",
        "CTL_ExtractorAction_ProgramName=extractor",
        "CTL_ExtractorAction_DialogTitle=extractor",
        "CTL_ExtractorAction_XMLFileName=extractor.xml",
        "CTL_ExtractorAction_Description=Creates L1 extract from L1 input file or creates L2 extract from L2 input file"
})
public class ExtractorAction extends CallCloProgramAction implements ContextAwareAction, LookupListener {

    private final Lookup lkp;

    public  ExtractorAction() {
        this(Utilities.actionsGlobalContext());
    }

    public   ExtractorAction(Lookup lkp) {
        this.lkp = lkp;
        Lookup.Result<OCSSWInfo> lkpContext = lkp.lookupResult(OCSSWInfo.class);
        lkpContext.addLookupListener(WeakListeners.create(LookupListener.class, this, lkpContext));
        putValue(Action.NAME, Bundle.CTL_ExtractorAction_Name());
        putValue(Action.SHORT_DESCRIPTION, Bundle.CTL_ExtractorAction_Description());
        setProgramName(Bundle.CTL_ExtractorAction_ProgramName());
        setDialogTitle(Bundle.CTL_ExtractorAction_DialogTitle());
        setXmlFileName(Bundle.CTL_ExtractorAction_XMLFileName());
        //setEnableState();
    }


    @Override
    public void resultChanged(LookupEvent lookupEvent) {
        //setEnableState();
    }

    public void setEnableState() {
        boolean state = false;
//        OCSSWInfo ocsswInfo = lkp.lookup(OCSSWInfo.class);
        if (ocsswInfo != null) {
            state = ocsswInfo.isOCSSWExist();
        }
        setEnabled(state);
    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new ExtractorAction(actionContext);
    }
}