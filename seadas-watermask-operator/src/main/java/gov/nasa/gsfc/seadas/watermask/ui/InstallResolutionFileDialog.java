package gov.nasa.gsfc.seadas.watermask.ui;

import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import gov.nasa.gsfc.seadas.watermask.util.ResourceInstallationUtils;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.rcp.SnapApp;

import javax.media.jai.ImageLayout;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: knowles
 * Date: 1/16/13
 * Time: 1:01 PM
 * To change this template use File | Settings | File Templates.
 */


class InstallResolutionFileDialog extends JDialog {

    public static enum Step {
        INSTALLATION,
        CONFIRMATION
    }

    public SourceFileInfo sourceFileInfo;
    private JLabel jLabel;
    private LandMasksData landMasksData;


    public InstallResolutionFileDialog(LandMasksData landMasksData, SourceFileInfo sourceFileInfo, Step step) {
        this.landMasksData = landMasksData;
        this.sourceFileInfo = sourceFileInfo;


        if (step == Step.INSTALLATION) {
            installationRequestUI();
        } else if (step == Step.CONFIRMATION) {
            installationResultsUI();
        }
    }


    public final void installationRequestUI() {
        JButton installButton = new JButton("Install File");
        installButton.setPreferredSize(installButton.getPreferredSize());
        installButton.setMinimumSize(installButton.getPreferredSize());
        installButton.setMaximumSize(installButton.getPreferredSize());


        installButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {

                SnapApp snapApp = SnapApp.getDefault();

                ProgressMonitorSwingWorker pmSwingWorker = new ProgressMonitorSwingWorker(snapApp.getMainFrame(),
                        "Land Mask Tool: File Installer") {

                    @Override
                    protected Void doInBackground(com.bc.ceres.core.ProgressMonitor pm) throws Exception {

                        int totalWork = 10;
                        int workDone = 0;

                        final String filename = sourceFileInfo.getFile().getName().toString();

                        pm.beginTask("Installing source file: " + filename, totalWork);

                        try {
                            dispose();

                            //  acquire in example: "https://oceandata.sci.gsfc.nasa.gov/SeaDAS/installer/landmask/50m.zip"
                            try {
                                landMasksData.fireEvent(LandMasksData.CONFIRMED_REQUEST_TO_INSTALL_FILE_EVENT);

                                final URL sourceUrl = new URL(LandMasksData.LANDMASK_URL + "/" + filename);

                                File targetFile = ResourceInstallationUtils.getTargetFile(filename);

                                if (!targetFile.exists()) {
                                    FileInstallRunnable fileInstallRunnable = new FileInstallRunnable(sourceUrl, filename, sourceFileInfo, landMasksData);
                                    Thread t = new Thread(fileInstallRunnable);
                                    t.start();

                                    while (fileInstallRunnable.isAlive()) {
                                        sleep(1000);
                                    }
                                }
                            } catch (Exception e) {
                            }
                        } finally {
                            pm.done();
                        }
                        return null;
                    }
                };

                pmSwingWorker.executeWithBlocking();

            }
        });


        JButton cancelButton = new JButton("Cancel");
        cancelButton.setPreferredSize(cancelButton.getPreferredSize());
        cancelButton.setMinimumSize(cancelButton.getPreferredSize());
        cancelButton.setMaximumSize(cancelButton.getPreferredSize());

        cancelButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                dispose();
            }
        });

        JLabel filler = new JLabel("                            ");


        JPanel buttonsJPanel = new JPanel(new GridBagLayout());
        buttonsJPanel.add(cancelButton,
                new ExGridBagConstraints(0, 0, 1, 0, GridBagConstraints.WEST, GridBagConstraints.NONE));
        buttonsJPanel.add(filler,
                new ExGridBagConstraints(1, 0, 0, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL));
        buttonsJPanel.add(installButton,
                new ExGridBagConstraints(2, 0, 1, 0, GridBagConstraints.EAST, GridBagConstraints.NONE));
        //buttonsJPanel.add(helpButton,
        //        new ExGridBagConstraints(3, 0, 1, 0, GridBagConstraints.EAST, GridBagConstraints.NONE));


        jLabel = new JLabel("<html>This tool requires source data file '" + sourceFileInfo.getFile().getName().toString() + "'.  <br>Do you wish to install the file now?</html>");


        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(10,10,10,5);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.weighty = 0;

        JPanel jPanel = new JPanel(new GridBagLayout());
        jPanel.add(jLabel, gbc);
        gbc.gridy++;
        jPanel.add(buttonsJPanel, gbc);



//        JPanel jPanel = new JPanel(new GridBagLayout());
//        jPanel.add(jLabel,
//                new ExGridBagConstraints(0, 0, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE));
//        jPanel.add(new JLabel(""),
//                new ExGridBagConstraints(0, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE));
//        jPanel.add(buttonsJPanel,
//                new ExGridBagConstraints(0, 2, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE));


        add(jPanel);

        setModalityType(ModalityType.APPLICATION_MODAL);


        setTitle("File Installation");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        Rectangle bounds = SnapApp.getDefault().getMainFrame().getBounds();
//        setLocationRelativeTo(null);
        setBounds(bounds.x + 700,bounds.y + 100,100,100);

        pack();


        setPreferredSize(getPreferredSize());
        setMinimumSize(getPreferredSize());
        setMaximumSize(getPreferredSize());
        setSize(getPreferredSize());

    }



    private void sleep(long milliSeconds) {
        try {
            Thread.sleep(milliSeconds);
        } catch (InterruptedException e3) {
            Thread.currentThread().interrupt();
        }
    }


    public final void installationResultsUI() {
        JButton okayButton = new JButton("Okay");
        okayButton.setPreferredSize(okayButton.getPreferredSize());
        okayButton.setMinimumSize(okayButton.getPreferredSize());
        okayButton.setMaximumSize(okayButton.getPreferredSize());


        okayButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                dispose();
            }
        });


        if (sourceFileInfo.isEnabled()) {
            jLabel = new JLabel("File " + sourceFileInfo.getFile().getName().toString() + " has been installed");
            landMasksData.fireEvent(LandMasksData.FILE_INSTALLED_EVENT2);
        } else {
            jLabel = new JLabel("File " + sourceFileInfo.getFile().getName().toString() + " installation failure");
        }

        JPanel jPanel = new JPanel(new GridBagLayout());
        jPanel.add(jLabel,
                new ExGridBagConstraints(0, 0, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE));
        jPanel.add(okayButton,
                new ExGridBagConstraints(0, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE));


        add(jPanel);

        setModalityType(ModalityType.APPLICATION_MODAL);


        setTitle("File Installation");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        Rectangle bounds = SnapApp.getDefault().getMainFrame().getBounds();
//        setLocationRelativeTo(null);
        setBounds(bounds.x + 700,bounds.y + 100,100,100);

        pack();


        setPreferredSize(getPreferredSize());
        setMinimumSize(getPreferredSize());
        setMaximumSize(getPreferredSize());
        setSize(getPreferredSize());

    }
}

