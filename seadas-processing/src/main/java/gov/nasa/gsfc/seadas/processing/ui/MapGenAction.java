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
        category = "Processing", id = "gov.nasa.gsfc.seadas.processing.ui.MapGenAction"
)
@ActionRegistration(
        displayName = "#CTL_ MapGenAction_Name",
        popupText = "#CTL_ MapGenAction_Name"
)
@ActionReference(
        path = "Menu/SeaDAS-Toolbox/OCSSW Processors",
        position = 140
)
@NbBundle.Messages({
        "CTL_MapGenAction_Name=mapgen...",
        "CTL_MapGenAction_ProgramName=mapgen",
        "CTL_MapGenAction_DialogTitle=mapgen",
        "CTL_MapGenAction_XMLFileName=mapgen.xml",
        "CTL_MapGenAction_Description=Create a mapped output."
})


public class MapGenAction extends CallCloProgramAction implements ContextAwareAction, LookupListener {

    private final Lookup lkp;

    public MapGenAction() {
        this(Utilities.actionsGlobalContext());
    }

    public MapGenAction(Lookup lkp) {
        this.lkp = lkp;
        Lookup.Result<ProductNode> lkpContext = lkp.lookupResult(ProductNode.class);
        lkpContext.addLookupListener(WeakListeners.create(LookupListener.class, this, lkpContext));
        putValue(Action.NAME, Bundle.CTL_MapGenAction_Name());
        putValue(Action.SHORT_DESCRIPTION, Bundle.CTL_MapGenAction_Description());
        setProgramName(Bundle.CTL_MapGenAction_ProgramName());
        setDialogTitle(Bundle.CTL_MapGenAction_DialogTitle());
        setXmlFileName(Bundle.CTL_MapGenAction_XMLFileName());
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
        return new MapGenAction(actionContext);
    }
}