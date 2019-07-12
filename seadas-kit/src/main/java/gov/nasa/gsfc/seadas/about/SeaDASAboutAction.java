/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.nasa.gsfc.seadas.about;

import org.jfree.ui.about.AboutPanel;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.WindowManager;

import javax.swing.JDialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Displays the {@link SeaDASAboutPanel} in a modal dialog.
 *
 * @author Norman Fomferra
 */
@ActionID(category = "Help", id = "org.esa.snap.rcp.about.SeaDASAboutAction" )
@ActionRegistration(displayName = "#CTL_SeaDASAboutAction_Name" )
@ActionReference(path = "Menu/Help/SeaDAS", position = 1000, separatorBefore = 999)
//        position = 1510, separatorBefore = 1500)
@Messages({
        "CTL_SeaDASAboutAction_Name=About SeaDAS",
        "CTL_SeaDASAboutAction_Title=About SeaDAS",
})
public final class SeaDASAboutAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        JDialog dialog = new JDialog(WindowManager.getDefault().getMainWindow(), Bundle.CTL_SeaDASAboutAction_Title(), true);
        dialog.setContentPane(new SeaDASAboutPanel());
        dialog.pack();
        dialog.setLocationRelativeTo(WindowManager.getDefault().getMainWindow());
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setVisible(true);
    }

}
