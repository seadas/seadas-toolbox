package gov.nasa.gsfc.seadas.processing.ui;

import org.esa.snap.rcp.SnapApp;
import org.esa.snap.ui.SnapFileChooser;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.prefs.Preferences;

/**
 * File > Open Image...: shows plain image files (PNG, JPEG, GIF, BMP) in an image window. File > Open Product
 * would instead open them as products, with each color channel as a separate band.
 */
@ActionID(category = "File", id = "gov.nasa.gsfc.seadas.processing.ui.OpenImageAction")
@ActionRegistration(displayName = "#CTL_OpenImageAction_Name")
@ActionReference(path = "Menu/File", position = 12)
@NbBundle.Messages({
        "CTL_OpenImageAction_Name=Open Image...",
        "CTL_OpenImageAction_Description=Show image files (PNG, JPEG, GIF, BMP) in an image window"
})
public class OpenImageAction extends AbstractAction {

    private static final String LAST_DIR_KEY = "seadas.openImage.lastDir";

    public OpenImageAction() {
        super(Bundle.CTL_OpenImageAction_Name());
        putValue(SHORT_DESCRIPTION, Bundle.CTL_OpenImageAction_Description());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Preferences preferences = SnapApp.getDefault().getPreferences();
        SnapFileChooser chooser = new SnapFileChooser(new File(preferences.get(LAST_DIR_KEY, System.getProperty("user.home"))));
        chooser.setDialogTitle("Open Image");
        chooser.setMultiSelectionEnabled(true);
        chooser.setAcceptAllFileFilterUsed(true);
        FileNameExtensionFilter images = new FileNameExtensionFilter("Image files (PNG, JPEG, GIF, BMP)",
                "png", "jpg", "jpeg", "gif", "bmp");
        chooser.addChoosableFileFilter(images);
        chooser.setFileFilter(images);

        if (chooser.showOpenDialog(SnapApp.getDefault().getMainFrame()) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File[] files = chooser.getSelectedFiles();
        if (files.length == 0 && chooser.getSelectedFile() != null) {
            files = new File[]{chooser.getSelectedFile()};
        }
        if (files.length > 0 && files[0].getParentFile() != null) {
            preferences.put(LAST_DIR_KEY, files[0].getParentFile().getAbsolutePath());
        }
        for (File file : files) {
            ImagePreviewWindow.show(file);
        }
    }
}
