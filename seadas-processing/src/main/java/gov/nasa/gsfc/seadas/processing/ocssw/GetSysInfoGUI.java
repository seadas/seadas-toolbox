package gov.nasa.gsfc.seadas.processing.ocssw;


import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.swing.binding.BindingContext;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.runtime.Config;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.GridBagUtils;
import org.esa.snap.ui.ModalDialog;
import org.esa.snap.ui.UIUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.*;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static gov.nasa.gsfc.seadas.processing.ocssw.OCSSW.*;
import static gov.nasa.gsfc.seadas.processing.ocssw.OCSSWConfigData.*;
import static gov.nasa.gsfc.seadas.processing.ocssw.OCSSWInfo.*;


/**
 * @author Bing Yang
 *
 */
// May 2020

public class GetSysInfoGUI {

    final String PANEL_NAME = "Seadas Configuration";
    final String HELP_ID = "getSysInfoConfig";

    final String BRANCH_TOOLTIP = "<html>The OCSSW installation branch<br>" +
            "This by default will match the SeaDAS version (first two decimal fields)<br> " +
            "For instance SeaDAS 7.5.3 by default will use OCSSW branch 7.5<br>" +
            "If OCSSW was manually updated to a different branch then this parameter needs to be set to match</html>";

    JPanel paramPanel = GridBagUtils.createPanel();
    JPanel paramSubPanel;

    ModalDialog modalDialog;


    JTextField ocsswBranchTextfield;
    JComboBox ocsswLocationComboBox;
    JTextField ocsswSharedDir;
    JTextField ocsswRootTextfield;
    JTextField ocsswLogDirTextfield;
    JTextField appDirTextfield;
    JTextField ocsswserverAddressTextfield;
    JTextField serverPortTextfield;
    JTextField serverInputStreamPortTextfield;
    JTextField serverErrorStreamPortTextfield;


    PropertyContainer pc = new PropertyContainer();

    OCSSWConfigData ocsswConfigData = new OCSSWConfigData();

    boolean windowsOS;

    String[] ocsswLocations;

    public static void main(String args[]) {

        final AppContext appContext = SnapApp.getDefault().getAppContext();
        final Window parent = appContext.getApplicationWindow();
        GetSysInfoGUI getSysInfoGUI = new GetSysInfoGUI();
        getSysInfoGUI.init(parent);
    }

    public void init(Window parent) {
        String operatingSystem = System.getProperty("os.name");
        if (operatingSystem.toLowerCase().contains("windows")) {
            windowsOS = true;
        } else {
            windowsOS = false;
        }


        JPanel mainPanel = GridBagUtils.createPanel();

        modalDialog = new ModalDialog(parent, PANEL_NAME, mainPanel, ModalDialog.ID_OK_CANCEL_HELP, HELP_ID);

//        modalDialog.getButton(ModalDialog.ID_OK).setText("OK");
        modalDialog.getButton(ModalDialog.ID_CANCEL).setText("Cancel");
        modalDialog.getButton(ModalDialog.ID_HELP).setText("Help");


        GridBagConstraints gbc = createConstraints();

        gbc.fill = GridBagConstraints.NONE;
        gbc.weighty = 0;
        gbc.weightx = 0;
        mainPanel.add(appDirPanel(), gbc);

        gbc.gridy += 1;
        gbc.weighty = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(ocsswRootPanel(), gbc);

        gbc.gridy += 1;
        gbc.weighty = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(ocsswLogDirPanel(), gbc);

        gbc.gridy += 1;
        gbc.weighty = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(makeParamPanel(), gbc);

        gbc.gridy += 1;
        gbc.weighty = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JPanel sysInfoPanel = sysInfoPanel();
        mainPanel.add(sysInfoPanel, gbc);

        // Add filler panel at bottom which expands as needed to force all components within this panel to the top
        gbc.gridy += 1;
        gbc.weighty = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.VERTICAL;
        JPanel fillerPanel = new JPanel();
        fillerPanel.setMinimumSize(fillerPanel.getPreferredSize());
        mainPanel.add(fillerPanel, gbc);


        mainPanel.setMinimumSize(mainPanel.getMinimumSize());


//        modalDialog.getButton(ModalDialog.ID_OK).setMinimumSize(modalDialog.getButton(ModalDialog.ID_OK).getPreferredSize());

        // Specifically set sizes for dialog here
        Dimension minimumSizeAdjusted = adjustDimension(modalDialog.getJDialog().getMinimumSize(), 25, 25);
        Dimension preferredSizeAdjusted = adjustDimension(modalDialog.getJDialog().getPreferredSize(), 25, 25);
        modalDialog.getJDialog().setMinimumSize(minimumSizeAdjusted);
        modalDialog.getJDialog().setPreferredSize(preferredSizeAdjusted);

        modalDialog.getJDialog().pack();


        int dialogResult;

        boolean finish = false;
        while (!finish) {
            dialogResult = modalDialog.show();

            if (dialogResult == ModalDialog.ID_OK) {
                if (checkParameters()) {
                    ocsswConfigData.updateconfigData(pc);
                    finish = true;
                }
            } else {
                finish = true;
            }
        }

        return;

    }



    private JPanel makeParamPanel() {

        final Preferences preferences = Config.instance("seadas").load().preferences();
        String lastOcsswLocation = preferences.get(SEADAS_OCSSW_LOCATION_PROPERTY, SEADAS_OCSSW_LOCATION_DEFAULT_VALUE);
        GridBagConstraints gbc = createConstraints();

        JLabel ocsswLocationLabel = new JLabel(OCSSW_LOCATION_LABEL + ": ");




        ArrayList<String> ocsswLocationArrayList = new ArrayList<String>();

        if (!windowsOS) {
            ocsswLocationArrayList.add(OCSSW_LOCATION_LOCAL);
            ocsswLocationLabel.setToolTipText("Note: Windows operating system detected so no local directory option");
        }
        ocsswLocationArrayList.add(OCSSW_LOCATION_VIRTUAL_MACHINE);
        ocsswLocationArrayList.add(OCSSW_LOCATION_REMOTE_SERVER);

        ocsswLocations = ocsswLocationArrayList.toArray(new String[ocsswLocationArrayList.size()]);

//        String[] ocsswLocations = {OCSSW_LOCATION_LOCAL, OCSSW_LOCATION_VIRTUAL_MACHINE, OCSSW_LOCATION_REMOTE_SERVER};

        int lastOcsswLocationIndex = 0;
        for (int i = 0; i < ocsswLocations.length; i++) {
            if (lastOcsswLocation.equals(ocsswLocations[i])) {
                lastOcsswLocationIndex = i;
                break;
            }
        }


        ocsswLocationComboBox = new JComboBox(ocsswLocations);
        ocsswLocationComboBox.setSelectedIndex(lastOcsswLocationIndex);

        gbc.weighty = 0;
        gbc.insets.top = 10;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        paramPanel.add(getLocationPanel(ocsswLocationLabel, ocsswLocationComboBox), gbc);


        // Determine ideal preferred size and minimum size of the param panel by invoking each selection to obtain
        // the respective values and then use the largest values.

        ocsswLocationComboBox.setSelectedIndex(0);
        Dimension preferredSize0 = paramPanel.getPreferredSize();
        Dimension minimumSize0 = paramPanel.getMinimumSize();

        Dimension preferredSize1 = paramPanel.getPreferredSize();
        Dimension minimumSize1 = paramPanel.getMinimumSize();
        if (ocsswLocations.length > 1) {
            ocsswLocationComboBox.setSelectedIndex(1);
            preferredSize1 = paramPanel.getPreferredSize();
            minimumSize1 = paramPanel.getMinimumSize();
        }

        Dimension preferredSize2 = paramPanel.getPreferredSize();
        Dimension minimumSize2 = paramPanel.getMinimumSize();
        if (ocsswLocations.length > 2) {
            ocsswLocationComboBox.setSelectedIndex(2);
             preferredSize2 = paramPanel.getPreferredSize();
             minimumSize2 = paramPanel.getMinimumSize();
        }

        Integer preferredWidths[] = {preferredSize0.width, preferredSize1.width, preferredSize2.width};
        Integer preferredHeights[] = {preferredSize0.height, preferredSize1.height, preferredSize2.height};
        Integer minimumWidths[] = {minimumSize0.width, minimumSize1.width, minimumSize2.width};
        Integer minimumHeights[] = {minimumSize0.height, minimumSize1.height, minimumSize2.height};

        int preferredWidth = Collections.max(Arrays.asList(preferredWidths));
        int preferredHeight = Collections.max(Arrays.asList(preferredHeights));
        int minimumWidth = Collections.max(Arrays.asList(minimumWidths));
        int minimumHeight = Collections.max(Arrays.asList(minimumHeights));

        Dimension preferredParamPanelSize = new Dimension(preferredWidth, preferredHeight);
        Dimension minimumParamPanelSize = new Dimension(minimumWidth, minimumHeight);


        ocsswLocationComboBox.setSelectedIndex(lastOcsswLocationIndex);

        // Specifically set preferred and minimum size
        paramPanel.setPreferredSize(preferredParamPanelSize);
        paramPanel.setMinimumSize(minimumParamPanelSize);

        return paramPanel;
    }

    private JPanel sysInfoPanel(){

        JLabel sysInfoLabel = new JLabel("SysInfo :");
        sysInfoLabel.setMinimumSize(sysInfoLabel.getPreferredSize());

        JTextArea sysInfoTextarea = new JTextArea();
//        sysInfoTextfield.setMinimumSize(new JTextField(10).getPreferredSize());


        JPanel panel = GridBagUtils.createPanel();
        GridBagConstraints gbc = createConstraints();
        String appDir = Config.instance().installDir().toString();
        OCSSWInfo ocsswInfo = OCSSWInfo.getInstance();
        String ocsswRootOcsswInfo = ocsswInfo.getOcsswRoot();
        String command = ocsswRootOcsswInfo + "/scripts/ocssw_runner --ocsswroot " + ocsswRootOcsswInfo
                + " " + ocsswRootOcsswInfo + "/scripts/seadas_info2.py --AppDir " + appDir;
        System.out.println("command is: " + command);

        try {
            Process process = Runtime.getRuntime().exec(command);

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            String line;
            String sysText = "";
            while ((line = reader.readLine()) != null) {
                sysText = sysText + line;
                sysText = sysText + "\n";
//                System.out.println("line : " + line);
//                System.out.print("text :  " + sysText);
            }

            sysInfoTextarea.setText(sysText);
            reader.close();

            process.destroy();

            if (process.exitValue() != 0) {
                System.out.println("Abnormal process termination");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(sysInfoLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(sysInfoTextarea, gbc);

        return panel;
    }

    private JPanel getLocationPanel(JLabel ocsswLocationLabel, JComboBox ocsswLocationComboBox) {

        JPanel panel = GridBagUtils.createPanel();
        GridBagConstraints gbc = createConstraints();

        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(ocsswLocationLabel, gbc);

        gbc.gridx += 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(ocsswLocationComboBox, gbc);

        ocsswLocationLabel.setMinimumSize(ocsswLocationLabel.getPreferredSize());
        ocsswLocationComboBox.setMinimumSize(ocsswLocationComboBox.getPreferredSize());
        panel.setMinimumSize(panel.getPreferredSize());

        return panel;
    }

    public File getDir() {

        File selectedFile = null;
        JFileChooser jFileChooser = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
        jFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnValue = jFileChooser.showOpenDialog(null);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            selectedFile = jFileChooser.getSelectedFile();
            //System.out.println(selectedFile.getAbsolutePath());
        }
        return selectedFile;
    }


    public static GridBagConstraints createConstraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 1;
        gbc.weighty = 1;
        return gbc;
    }


    private JPanel ocsswRootPanel() {

        final Preferences preferences = Config.instance("seadas").load().preferences();

        JPanel panel = GridBagUtils.createPanel();

        GridBagConstraints gbc = createConstraints();

        JLabel ocsswRootLabel = new JLabel(OCSSW_ROOT_LABEL + ": ");
        ocsswRootTextfield = new JTextField(20);

        ocsswRootLabel.setMinimumSize(ocsswRootLabel.getPreferredSize());
        ocsswRootTextfield.setMinimumSize(new JTextField(5).getPreferredSize());

        OCSSWInfo ocsswInfo = OCSSWInfo.getInstance();
        String ocsswRootOcsswInfo = ocsswInfo.getOcsswRoot();
        System.out.println("ocsswTootOcsswInfo : " + ocsswRootOcsswInfo);


        pc.addProperty(Property.create(SEADAS_OCSSW_ROOT_PROPERTY, ocsswRootOcsswInfo));
        pc.getDescriptor(SEADAS_OCSSW_ROOT_PROPERTY).setDisplayName(SEADAS_OCSSW_ROOT_PROPERTY);

        final BindingContext ctx = new BindingContext(pc);

        ctx.bind(SEADAS_OCSSW_ROOT_PROPERTY, ocsswRootTextfield);

        gbc.fill = GridBagConstraints.NONE;

        gbc.weighty = 0;
        gbc.weightx = 0;
        panel.add(ocsswRootLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(ocsswRootTextfield, gbc);

        return panel;
    }


    private JPanel appDirPanel() {

//        final Preferences preferences = Config.instance("seadas").load().preferences();

        String installDir = Config.instance().installDir().toString();
        JPanel panel = GridBagUtils.createPanel();

        GridBagConstraints gbc = createConstraints();

        JLabel appDirLabel = new JLabel("Application Directory" + ": ");
        appDirTextfield = new JTextField(45);

        appDirLabel.setMinimumSize(appDirLabel.getPreferredSize());
        appDirTextfield.setMinimumSize(new JTextField(5).getPreferredSize());


        System.out.println("appDir = " + installDir);


        pc.addProperty(Property.create("appDir", installDir));
        pc.getDescriptor("appDir").setDisplayName("appDir");

        final BindingContext ctx = new BindingContext(pc);

        ctx.bind("appDir", appDirTextfield);

        gbc.fill = GridBagConstraints.NONE;

        gbc.weighty = 0;
        gbc.weightx = 0;
        panel.add(appDirLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(appDirTextfield, gbc);

        return panel;
    }

    private JPanel ocsswLogDirPanel() {

//        final Preferences preferences = Config.instance("seadas").load().preferences();

        OCSSWInfo ocsswInfo = OCSSWInfo.getInstance();
        String ocsswLogDir = ocsswInfo.getLogDirPath();
        JPanel panel = GridBagUtils.createPanel();

        GridBagConstraints gbc = createConstraints();

        JLabel ocsswLogDirLabel = new JLabel("OCSSW Log Directory" + ": ");
        ocsswLogDirTextfield = new JTextField(45);

        ocsswLogDirLabel.setMinimumSize(ocsswLogDirLabel.getPreferredSize());
        ocsswLogDirTextfield.setMinimumSize(new JTextField(5).getPreferredSize());


        System.out.println("OCSSW Log Directory = " + ocsswLogDir);


        pc.addProperty(Property.create("ocsswLogDir", ocsswLogDir));
        pc.getDescriptor("ocsswLogDir").setDisplayName("ocsswLogDir");

        final BindingContext ctx = new BindingContext(pc);

        ctx.bind("ocsswLogDir", ocsswLogDirTextfield);

        gbc.fill = GridBagConstraints.NONE;

        gbc.weighty = 0;
        gbc.weightx = 0;
        panel.add(ocsswLogDirLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(ocsswLogDirTextfield, gbc);

        return panel;
    }


    private Dimension adjustDimension(Dimension dimension, int widthAdjustment, int heightAdjustment) {

        if (dimension == null) {
            return null;
        }

        int width = dimension.width + widthAdjustment;
        int height = dimension.height + heightAdjustment;

        return new Dimension(width, height);
    }




    /**
     * If there is a conflict between the String ocsswroot and the environment variable OCSSWROOT
     * a GUI prompts the users whether this conflict is okay.  If there is no conflict or if there
     * is a conflict and the user approves then 'true' is returned.
     *
     * @param field
     * @param ocsswroot
     * @return
     */
    private Boolean checkIfEnvironmentVariableConflict(String field, String ocsswroot) {

        Map<String, String> env = System.getenv();
        String ocsswroot_env = env.get("OCSSWROOT");

        if (ocsswroot_env == null || ocsswroot_env.trim().length() == 0) {
            return true;
        }

        if (ocsswroot_env.equals(ocsswroot)) {
            return true;
        }

        String msg = "<html>" +
                "WARNING!: You are defining OCSSW ROOT to be '" + ocsswroot + "'<br>" +
                "but on your system the environment variable OCSSWROOT points to '" + ocsswroot_env + "'<br>" +
                "This mismatch could cause problems or conflict between GUI and command line operations<br>"+
                "</html>";


        final int dialogResult = getUserResponse(msg, "Continue", "Back");

        if (dialogResult == ModalDialog.ID_OK) {
            return true;
        } else {
            return false;
        }


    }



    private Boolean checkParameters() {

        if (!isTextFieldValidBranch(OCSSW_BRANCH_LABEL, ocsswBranchTextfield)) {
            return false;
        }

        String ocssLocationString = (String) ocsswLocationComboBox.getSelectedItem();
        switch (ocssLocationString) {
            case OCSSW_LOCATION_LOCAL:
                if (!directoryCheck(OCSSW_ROOT_LABEL, ocsswRootTextfield.getText())) {
                    return false;
                }

                if (!checkIfEnvironmentVariableConflict(OCSSW_ROOT_LABEL, ocsswRootTextfield.getText())) {
                    return false;
                }

                break;

            case OCSSW_LOCATION_VIRTUAL_MACHINE:
                if (!directoryCheck(OCSSW_SHARED_DIR_LABEL, ocsswSharedDir.getText())) {
                    return false;
                }

                break;

            case OCSSW_LOCATION_REMOTE_SERVER:
                if (!isTextFieldValidIP(OCSSW_SERVER_ADDRESS_LABEL, ocsswserverAddressTextfield)) {
                    return false;
                }

                if (!isTextFieldValidPort(SERVER_PORT_LABEL, serverPortTextfield)) {
                    return false;
                }

                if (!isTextFieldValidPort(SERVER_INPUT_STREAM_PORT_LABEL, serverInputStreamPortTextfield)) {
                    return false;
                }

                if (!isTextFieldValidPort(SERVER_ERROR_STREAM_PORT_LABEL, serverErrorStreamPortTextfield)) {
                    return false;
                }

                break;
        }

        return true;
    }




    public boolean isTextFieldValidBranch(String field, JTextField textfield) {

        boolean valid = true;

        if (textfield != null) {
            if (textfieldHasValue(textfield)) {
                String branch = textfield.getText().trim();

                if (!isValidBranch(branch)) {
                    notifyError("<html>" + field + "='" + branch + "' is not a valid OCSSW branch. <br>" +
                            "The OCSSW branch must contain 2 fields of the form X.X where X is an integer</html>");
                    return false;
                }

                if (!isDefaultBranch(branch)) {
                    return false;
                }

            } else {
                notifyError("'" + field + "' must contain an OCSSW Branch");
                valid = false;
            }
        } else {
            notifyError("'" + field + "' not defined");
            valid = false;
        }

        return valid;
    }



    public boolean isDefaultBranch(String branch) {

        if (branch == null) {
            return false;
        }

        branch.trim();

        if (SEADAS_OCSSW_BRANCH_DEFAULT_VALUE.equals(branch)) {
            return true;
        }

        String msg = "<html>" +
                "WARNING!: Your current SeaDAS version has default branch='" + SEADAS_OCSSW_BRANCH_DEFAULT_VALUE + "'<br>"+
                "You have selected to use branch='" + branch + "'<br>" +
                "This version mismatch could cause possible problems when running from the SeaDAS GUI" +
                "</html>";

        final int dialogResult = getUserResponse(msg, "Continue", "Back");

        if (dialogResult == ModalDialog.ID_OK) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * This method scans for valid OCSSW tags at https://oceandata.sci.gsfc.nasa.gov/manifest/tags, returns a list of tags that start with capital letter "V"
     * @return List of valid OCSSW tags for SeaDAS
     */
    public ArrayList<String> getValidOcsswTagsFromURL(){
        ArrayList<String> validOcsswTags = new ArrayList<>();
        int i =0;
        try {
            Connection connection = Jsoup.connect("https://oceandata.sci.gsfc.nasa.gov/manifest/tags");
            Document doc = connection.get(); doc.children();
            String tagName;
            for (Element file : doc.getElementsByAttribute("href")) {
                tagName = file.attr("href");
                System.out.println(tagName);
                if (tagName.startsWith("V")) {
                    validOcsswTags.add(tagName);
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        return validOcsswTags;
    }

    /**
     * This method scans for valid OCSSW tags at https://oceandata.sci.gsfc.nasa.gov/manifest/tags, returns a list of tags that start with capital letter "V"
     * @return List of valid OCSSW tags for SeaDAS
     */
    public ArrayList<String> getValidOcsswTagsFromCLI(){
        ArrayList<String> validOcsswTags = new ArrayList<>();
        int i =0;
        try {
            Connection connection = Jsoup.connect("https://oceandata.sci.gsfc.nasa.gov/manifest/tags");
            Document doc = connection.get(); doc.children();
            String tagName;
            for (Element file : doc.getElementsByAttribute("href")) {
                tagName = file.attr("href");
                System.out.println(tagName);
                if (tagName.startsWith("V")) {
                    validOcsswTags.add(tagName);
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        return validOcsswTags;
    }

    public static boolean isValidBranch(final String branch) {

        String[] arrOfBranch = branch.split("\\.");

        if (arrOfBranch.length == 2) {
            if (isNumeric(arrOfBranch[0]) && isNumeric(arrOfBranch[1])) {
                return true;
            }

        } else {
            return false;
        }

        return false;
    }

    public boolean isTextFieldValidIP(String label, JTextField textfield) {

        if (textfield != null) {
            if (textfieldHasValue(textfield)) {
                if (isValidIP(textfield.getText())) {
                    return true;
                } else {
                    notifyError("<html>" + label + "='" + textfield.getText() + "' is not a valid IP address</html>");
                }
            } else {
                notifyError("'" + label + "' must contain an IP address");
            }
        } else {
            notifyError("'" + label + "' not defined");
        }

        return false;
    }


    public boolean isTextFieldValidPort(String label, JTextField textfield) {

        if (textfield != null) {
            if (textfieldHasValue(textfield)) {
                if (isValidPort(textfield.getText())) {
                    return true;
                } else {
                    notifyError("'" + label + "=" + textfield.getText() + "' is not a valid port");
                }
            } else {
                notifyError("'" + label + "' must contain a port number");
            }
        } else {
            notifyError("'" + label + "' not defined");
        }

        return false;
    }

    public static boolean textfieldHasValue(JTextField textfield) {
        if (textfield.getText() != null && textfield.getText().length() > 0) {
            return true;
        } else {
            return false;
        }
    }


    public static boolean isValidPort(String str) {
        int port;

        try {
            port = Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return false;
        }

        if (port >= 1 && port <= 65535) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isValidIP(final String ip) {
        String PATTERN = "^((0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)\\.){3}(0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)$";

        return ip.matches(PATTERN);
    }


    public static boolean isNumeric(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }


    private int getUserResponse(String msg, String okText, String cancelText) {

        JPanel panel = GridBagUtils.createPanel();

        JLabel label = new JLabel(msg);

        GridBagConstraints gbc = createConstraints();

        gbc.anchor = GridBagConstraints.NORTH;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 1;
        gbc.weightx = 1;

        panel.add(label, gbc);

        ModalDialog dialog = new ModalDialog(modalDialog.getParent(), PANEL_NAME, panel, ModalDialog.ID_OK_CANCEL, HELP_ID);
        dialog.getButton(ModalDialog.ID_OK).setText(okText);
        dialog.getButton(ModalDialog.ID_CANCEL).setText(cancelText);
        dialog.getButton(ModalDialog.ID_OK).setMinimumSize(dialog.getButton(ModalDialog.ID_OK).getPreferredSize());

        // Specifically set sizes for dialog here
        Dimension minimumSizeAdjusted = adjustDimension(dialog.getJDialog().getMinimumSize(), 25, 50);
        Dimension preferredSizeAdjusted = adjustDimension(dialog.getJDialog().getPreferredSize(), 25, 50);
        dialog.getJDialog().setMinimumSize(minimumSizeAdjusted);
        dialog.getJDialog().setPreferredSize(preferredSizeAdjusted);

        dialog.getJDialog().pack();

        return dialog.show();
    }





    private Boolean directoryCheck(String field, String filename) {
        File dir = new File(filename);

        if (dir.exists()) {
            return true;
        }

        String msg = "<html>" + field + " directory: '" + filename + "' does not exist" + "</html>";

        final int dialogResult = getUserResponse(msg, "Create Directory", "Back");

        if (dialogResult == ModalDialog.ID_OK) {

            try {
                dir.mkdirs();

                if (dir.exists()) {
                    return true;
                } else {
                    msg = "<html>Failed to create directory '" +  filename + "'<br></html>";
                }
            } catch (Exception e){
                msg = "<html>Failed to create directory '" +  filename + "'<br>" +
                        e.toString() + "</html>";
            }

            notifyError(msg);
        }

        return false;
    }



    private void notifyError(String msg) {
        JOptionPane.showMessageDialog(null, msg, PANEL_NAME, JOptionPane.WARNING_MESSAGE);
    }

}
