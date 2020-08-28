/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.nasa.gsfc.seadas.about;

import com.bc.ceres.core.runtime.Version;
import gov.nasa.gsfc.seadas.processing.ocssw.OCSSWInfoGUI;
import org.esa.snap.rcp.about.AboutBox;
import org.esa.snap.rcp.util.BrowserUtils;
import org.openide.modules.ModuleInfo;
import org.openide.modules.Modules;

import javax.swing.*;
import java.awt.*;


/**
 * @author Aynur Abdurazik
 * @author Daniel Knowles
 */


@AboutBox(displayName = "SeaDAS-Toolbox", position = 15)
public class SeaDASAboutBox extends JPanel {

    private final static String PACKAGE = "SeaDAS-Toolbox";
    private final static String SEADAS_VERSION = "1.0-Beta";

    private final static String RELEASE_NOTES_URL = "https://github.com/seadas/seadas-toolbox/blob/master/ReleaseNotes.md";
    private final static String RELEASE_NOTES_URL_NAME = PACKAGE + " " + SEADAS_VERSION + " Release Notes";

    private final static String OCEAN_COLOR_WEB_URL = "https://oceancolor.gsfc.nasa.gov/";
    private final static String OCEAN_COLOR_WEB_URL_NAME = "NASA Ocean Color Web";

    private final static String SEADAS_WEB_URL = "https://seadas.gsfc.nasa.gov/";
    private final static String SEADAS_WEB_URL_NAME = "SeaDAS Web";

    private final static String releaseNotesUrlString = "https://senbox.atlassian.net/issues/?filter=-4&jql=project%20%3D%20SIIITBX%20AND%20fixVersion%20%3D%20";


    public SeaDASAboutBox() {
        super(new BorderLayout());
        ImageIcon aboutImage = new ImageIcon(SeaDASAboutBox.class.getResource("about_seadas.png"));
        JLabel banner = new JLabel(aboutImage);

        JLabel infoText = new JLabel("<html>"
                + "The <i>SeaDAS-Toolbox</i> contains all the NASA Ocean Biology science processing<br>"
                + "science tools.  This tools allow users to process data between the various levels<br>"
                + "(i.e. Level 0 through Level 3).  This toolbox also provides additional GUI tools<br> "
                + "related to ocean sciences."
                + "</html>"
        );



        GridBagConstraints gbc = new GridBagConstraints();
        JPanel jPanel = new JPanel(new GridBagLayout());

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.insets.left = 5;
        gbc.insets.top = 5;

        gbc.gridy = 0;
        infoText.setMinimumSize(infoText.getPreferredSize());
        jPanel.add(infoText, gbc);

        gbc.gridy = 1;
        gbc.insets.left = 15;
//        jPanel.add(getUrlJLabel(RELEASE_NOTES_URL, RELEASE_NOTES_URL_NAME), gbc);
//
//        gbc.gridy = 2;
        jPanel.add(getUrlJLabel(SEADAS_WEB_URL, SEADAS_WEB_URL_NAME), gbc);

        gbc.gridy = 2;
        jPanel.add(getUrlJLabel(OCEAN_COLOR_WEB_URL, OCEAN_COLOR_WEB_URL_NAME), gbc);

        gbc.gridy = 3;
        jPanel.add(banner, gbc);

        gbc.gridy = 4;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        jPanel.add(new JLabel(), gbc);

        gbc.gridy = 5;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets.bottom=10;

        jPanel.add(createVersionPanel(), gbc);

        add(jPanel, BorderLayout.WEST);

        ModuleInfo seadasProcessingModuleInfo = Modules.getDefault().ownerOf(OCSSWInfoGUI.class);
        System.out.println("SeaDAS Toolbox Specification Version: " + seadasProcessingModuleInfo.getSpecificationVersion());
        System.out.println("SeaDAS Toolbox Implementation Version: " + seadasProcessingModuleInfo.getImplementationVersion());

    }

    private JPanel createVersionPanel() {
        final JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
//        final ModuleInfo moduleInfo = Modules.getDefault().ownerOf(SeaDASAboutBox.class);
        // todo Overrode this because SeaDASAboutBox version not defined in macro
        final ModuleInfo moduleInfo = Modules.getDefault().ownerOf(OCSSWInfoGUI.class);

        panel.add(new JLabel("<html><b>SeaDAS Toolbox version " + moduleInfo.getSpecificationVersion() + "</b>", SwingConstants.RIGHT));

        Version specVersion = Version.parseVersion(moduleInfo.getSpecificationVersion().toString());
        String versionString = String.format("%s.%s.%s", specVersion.getMajor(), specVersion.getMinor(), specVersion.getMicro());
        String changelogUrl = releaseNotesUrlString + versionString;
        changelogUrl = RELEASE_NOTES_URL;
        final JLabel releaseNoteLabel = new JLabel("<html><a href=\"" + changelogUrl + "\">Release Notes</a>", SwingConstants.RIGHT);
        releaseNoteLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        releaseNoteLabel.addMouseListener(new BrowserUtils.URLClickAdaptor(changelogUrl));
        panel.add(releaseNoteLabel);
        return panel;
    }


    private JLabel getUrlJLabel(String url, String name) {
        final JLabel jLabel = new JLabel("<html> " +
                "<a href=\"" + url + "\">" + name + "</a></html>");
        jLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        jLabel.addMouseListener(new BrowserUtils.URLClickAdaptor(url));
        return jLabel;
    }

}
