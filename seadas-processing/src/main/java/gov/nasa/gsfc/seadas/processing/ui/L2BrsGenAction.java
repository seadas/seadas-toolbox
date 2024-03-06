//package gov.nasa.gsfc.seadas.processing.ui;
//
//import gov.nasa.gsfc.seadas.processing.common.CallCloProgramAction;
//import org.esa.snap.core.datamodel.ProductNode;
//import org.openide.awt.ActionID;
//import org.openide.awt.ActionReference;
//import org.openide.awt.ActionRegistration;
//import org.openide.util.*;
//
//import javax.swing.*;
//
///**
// * @author Aynur Abdurazik
// * @since SeaDAS 8.0
// * @see
// */
//@ActionID(
//        category = "Processing", id = "gov.nasa.gsfc.seadas.processing.ui.L2BrsGenAction"
//)
//@ActionRegistration(
//        displayName = "#CTL_ L2BrsGenAction_Name",
//        popupText = "#CTL_ L2BrsGenAction_Name"
//)
//@ActionReference(
//        path = "Menu/SeaDAS-Toolbox/SeaDAS Processors/Tools",
//        position = 410
//)
//@NbBundle.Messages({
//        "CTL_L2BrsGenAction_Name=l2brsgen...",
//        "CTL_L2BrsGenAction_ProgramName=l2brsgen",
//        "CTL_L2BrsGenAction_DialogTitle=l2brsgen",
//        "CTL_L2BrsGenAction_XMLFileName=l2brsgen.xml",
//        "CTL_L2BrsGenAction_Description=Creates a Level 2 Browse file"
//})
//
//public class L2BrsGenAction extends CallCloProgramAction implements ContextAwareAction, LookupListener {
//
//    private final Lookup lkp;
//
//    public  L2BrsGenAction() {
//        this(Utilities.actionsGlobalContext());
//    }
//
//    public   L2BrsGenAction(Lookup lkp) {
//        this.lkp = lkp;
//        Lookup.Result<ProductNode> lkpContext = lkp.lookupResult(ProductNode.class);
//        lkpContext.addLookupListener(WeakListeners.create(LookupListener.class, this, lkpContext));
//        putValue(Action.NAME, Bundle.CTL_L2BrsGenAction_Name());
//        putValue(Action.SHORT_DESCRIPTION, Bundle.CTL_L2BrsGenAction_Description());
//        setProgramName(Bundle.CTL_L2BrsGenAction_ProgramName());
//        setDialogTitle(Bundle.CTL_L2BrsGenAction_DialogTitle());
//        setXmlFileName(Bundle.CTL_L2BrsGenAction_XMLFileName());
//    }
////
////    @Override
////    public void actionPerformed(ActionEvent e) {
////
////    }
//
//    @Override
//    public void resultChanged(LookupEvent lookupEvent) {
//
//    }
//
//    @Override
//    public Action createContextAwareInstance(Lookup actionContext) {
//        return new L2BrsGenAction(actionContext);
//    }
//}