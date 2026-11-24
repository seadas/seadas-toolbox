package gov.nasa.gsfc.seadas.processing.ui;

import gov.nasa.gsfc.seadas.processing.earthdata.EarthdataLoginDialog;
import org.esa.snap.rcp.SnapApp;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * SeaDAS-Toolbox > Earthdata Login...: sets the NASA Earthdata Login that SeaDAS and OCSSW use.
 */
@ActionID(category = "Processing", id = "gov.nasa.gsfc.seadas.processing.ui.EarthdataLoginAction")
@ActionRegistration(displayName = "#CTL_EarthdataLoginAction_Name")
@ActionReference(path = "Menu/SeaDAS-Toolbox", position = 20)
@NbBundle.Messages({
        "CTL_EarthdataLoginAction_Name=Earthdata Login..."
})
public final class EarthdataLoginAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        EarthdataLoginDialog.show(SnapApp.getDefault().getMainFrame(), null);
    }
}
