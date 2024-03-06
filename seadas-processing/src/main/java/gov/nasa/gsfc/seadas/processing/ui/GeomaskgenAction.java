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
        category = "Processing", id = "gov.nasa.gsfc.seadas.processing.ui.GeomaskgenAction"
)
@ActionRegistration(
        displayName = "#CTL_ GeomaskgenAction_Name",
        popupText = "#CTL_ GeomaskgenAction_Name"
)
@ActionReference(
        path = "Menu/SeaDAS-Toolbox/SeaDAS Processors/Tools",
        position = 150
)
@NbBundle.Messages({
        "CTL_GeomaskgenAction_Name=geomask_gen...",
        "CTL_GeomaskgenAction_ProgramName=geomask_gen",
        "CTL_GeomaskgenAction_DialogTitle=geomask_gen",
        "CTL_GeomaskgenAction_XMLFileName=geomask_gen.xml",
        "CTL_GeomaskgenAction_Description=Generates a mask file from input WKT or Shapefiles"
})

public class GeomaskgenAction extends CallCloProgramAction implements ContextAwareAction, LookupListener {

    private final Lookup lkp;

    public GeomaskgenAction() {
        this(Utilities.actionsGlobalContext());
    }

    public GeomaskgenAction(Lookup lkp) {
        this.lkp = lkp;
        Lookup.Result<ProductNode> lkpContext = lkp.lookupResult(ProductNode.class);
        lkpContext.addLookupListener(WeakListeners.create(LookupListener.class, this, lkpContext));
        putValue(Action.NAME, Bundle.CTL_GeomaskgenAction_Name());
        putValue(Action.SHORT_DESCRIPTION, Bundle.CTL_GeomaskgenAction_Description());
        setProgramName(Bundle.CTL_GeomaskgenAction_ProgramName());
        setDialogTitle(Bundle.CTL_GeomaskgenAction_DialogTitle());
        setXmlFileName(Bundle.CTL_GeomaskgenAction_XMLFileName());
    }

    @Override
    public void resultChanged(LookupEvent lookupEvent) {

    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new GeomaskgenAction(actionContext);
    }
}
