package gov.nasa.gsfc.seadas.processing.ui;

import gov.nasa.gsfc.seadas.processing.common.CallCloProgramAction;
import gov.nasa.gsfc.seadas.processing.common.SeadasFileUtils;
import gov.nasa.gsfc.seadas.processing.core.ProcessorModel;
import org.esa.snap.core.datamodel.ProductNode;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.*;

import javax.swing.*;
import java.io.File;

/**
 * GUI for annotate_l3mapgen: draws coastlines, borders, gridlines and land on an l3mapgen map and writes a PNG.
 */
@ActionID(
        category = "Processing", id = "gov.nasa.gsfc.seadas.processing.ui.AnnotateL3MapgenAction"
)
@ActionRegistration(
        displayName = "#CTL_AnnotateL3MapgenAction_Name",
        popupText = "#CTL_AnnotateL3MapgenAction_Description"
)
@ActionReference(
        path = "Menu/SeaDAS-Toolbox/SeaDAS Processors",
        position = 361
)
@NbBundle.Messages({
        "CTL_AnnotateL3MapgenAction_Name=annotate_l3mapgen...",
        "CTL_AnnotateL3MapgenAction_ProgramName=annotate_l3mapgen",
        "CTL_AnnotateL3MapgenAction_DialogTitle=annotate_l3mapgen",
        "CTL_AnnotateL3MapgenAction_XMLFileName=annotate_l3mapgen.xml",
        "CTL_AnnotateL3MapgenAction_Description=Adds coastlines, borders, gridlines and land to an l3mapgen map and writes a PNG image"
})
public class AnnotateL3MapgenAction extends CallCloProgramAction implements ContextAwareAction, LookupListener {

    public AnnotateL3MapgenAction() {
        this(Utilities.actionsGlobalContext());
    }

    public AnnotateL3MapgenAction(Lookup lkp) {
        Lookup.Result<ProductNode> lkpContext = lkp.lookupResult(ProductNode.class);
        lkpContext.addLookupListener(WeakListeners.create(LookupListener.class, this, lkpContext));
        putValue(Action.NAME, Bundle.CTL_AnnotateL3MapgenAction_Name());
        putValue(Action.SHORT_DESCRIPTION, Bundle.CTL_AnnotateL3MapgenAction_Description());
        setProgramName(Bundle.CTL_AnnotateL3MapgenAction_ProgramName());
        setDialogTitle(Bundle.CTL_AnnotateL3MapgenAction_DialogTitle());
        setXmlFileName(Bundle.CTL_AnnotateL3MapgenAction_XMLFileName());
    }

    /**
     * Shows the PNG as soon as it is written. Opening it as a product is not useful: SeaDAS would show each
     * color channel as a separate grayscale band.
     */
    @Override
    protected void displayOutput(ProcessorModel processorModel) {
        String ofileName = processorModel.getOfileName();
        if (ofileName == null || ofileName.trim().isEmpty()) {
            return;
        }
        File ofile = new File(ofileName.trim());
        if (!ofile.isAbsolute()) {
            ofile = SeadasFileUtils.createFile(ocssw.getOfileDir(), ofileName.trim());
        }
        ImagePreviewWindow.show(ofile);
    }

    @Override
    public void resultChanged(LookupEvent lookupEvent) {
    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new AnnotateL3MapgenAction(actionContext);
    }
}
