package gov.nasa.gsfc.seadas.processing.common;

import com.bc.ceres.swing.TableLayout;
import gov.nasa.gsfc.seadas.processing.ocssw.OCSSW;
import gov.nasa.gsfc.seadas.processing.ocssw.OCSSWInfo;
import gov.nasa.gsfc.seadas.processing.core.ParamUtils;
import gov.nasa.gsfc.seadas.processing.core.ProcessorModel;
import org.esa.snap.ui.AppContext;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.util.*;

import static gov.nasa.gsfc.seadas.processing.ocssw.OCSSWInfo.OCSSW_SRC_DIR_NAME;

/**
 * Created by IntelliJ IDEA.
 * User: Aynur Abdurazik (aabduraz)
 * Date: 3/13/13
 * Time: 11:11 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class OCSSWInstallerForm extends JPanel implements CloProgramUI {


    //private FileSelector ocsswDirSelector;
    JTextField fileTextField;
    //private SwingPropertyChangeSupport propertyChangeSupport = new SwingPropertyChangeSupport(this);

    ProcessorModel processorModel;
    private AppContext appContext;
    private JPanel dirPanel;
    private JPanel tagPanel;
    private JPanel missionPanel;
    private JPanel otherPanel;

    //private JPanel superParamPanel;

    public static final String INSTALL_DIR_OPTION_NAME = "--install_dir";
    public final static String VALID_TAGS_OPTION_NAME = "--tag";
    public final static String CURRENT_TAG_OPTION_NAME = "--current_tag";

    public String missionDataDir;
    public OCSSW ocssw;

    private static final Set<String> MISSIONS = new HashSet<String>(Arrays.asList(
            new String[]{"AQUARIUS",
                    "AVHRR",
                    "CZCS",
                    "GOCI",
                    "HAWKEYE",
                    "HICO",
                    "MERIS",
                    "MSIS2A",
                    "MSIS2B",
                    "MODISA",
                    "MODIST",
                    "MOS",
                    "OCM1",
                    "OCM2",
                    "OCRVC",
                    "OCTS",
                    "OLCIS3A",
                    "OLCIS3B",
                    "OLI",
                    "OSMI",
                    "SGLI",
                    "SEAWIFS",
                    "VIIRSN",
                    "VIIRSJ1"}
    ));
    private static final Set<String> DEFAULT_MISSIONS = new HashSet<String>(Arrays.asList(
            new String[]{
                    //"GOCI",
                    //"HICO",
                    "OCRVC"
            }
    ));


    ArrayList<String> validOCSSWTagList = new ArrayList<>();
    String latestOCSSWTagForInstalledRelease; // a hard default which get replace by JSON value if internet connection


    HashMap<String, Boolean> missionDataStatus;
    SeadasToolboxVersion seadasToolboxVersion;


    public OCSSWInstallerForm(AppContext appContext, String programName, String xmlFileName, OCSSW ocssw) {
        this.appContext = appContext;
        this.ocssw = ocssw;

        seadasToolboxVersion = new SeadasToolboxVersion();
        validOCSSWTagList = seadasToolboxVersion.getOCSSWTagListForInstalledRelease();
//        getSeaDASVersionTags();


        // set default
//        if (validOCSSWTagList != null && validOCSSWTagList.size() >= 1) {
        latestOCSSWTagForInstalledRelease = seadasToolboxVersion.getLatestOCSSWTagForInstalledRelease();
//        }

//        for (String tag : validOCSSWTagList) {
//            System.out.println("tag=" + tag);
//        }

        processorModel = ProcessorModel.valueOf(programName, xmlFileName, ocssw);
        processorModel.setReadyToRun(true);
        setMissionDataDir(OCSSWInfo.getInstance().getOcsswDataDirPath());
        init();
        updateMissionValues();
        createUserInterface();
        processorModel.updateParamInfo(INSTALL_DIR_OPTION_NAME, getInstallDir());
        processorModel.addPropertyChangeListener(INSTALL_DIR_OPTION_NAME, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                setMissionDataDir(processorModel.getParamValue(INSTALL_DIR_OPTION_NAME) + File.separator + OCSSWInfo.OCSSW_DATA_DIR_SUFFIX);
                updateMissionStatus();
                updateMissionValues();
                createUserInterface();
            }
        });
    }

    String getMissionDataDir() {
        return missionDataDir;
    }

    void setMissionDataDir(String currentMissionDataDir) {
        missionDataDir = currentMissionDataDir;
    }

    abstract void updateMissionStatus();

    abstract void updateMissionValues();

    String getInstallDir() {
        return OCSSWInfo.getInstance().getOcsswRoot();
    }

    abstract void init();

    public ProcessorModel getProcessorModel() {
        return processorModel;
    }

    public File getSelectedSourceProduct() {
        return null;
    }

    public boolean isOpenOutputInApp() {
        return false;
    }

    public String getParamString() {
        return processorModel.getParamList().getParamString();
    }

    public void setParamString(String paramString) {
        processorModel.getParamList().setParamString(paramString);
    }

    protected void createUserInterface() {

        this.setLayout(new GridBagLayout());

        JPanel paramPanel = new ParamUIFactory(processorModel).createParamPanel();
        reorganizePanel(paramPanel);

        add(dirPanel,
                new GridBagConstraintsCustom(0, 0, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 3));
        add(tagPanel,
                new GridBagConstraintsCustom(0, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 3));

        add(missionPanel,
                new GridBagConstraintsCustom(0, 2, 1, 1, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 3));
        add(otherPanel,
                new GridBagConstraintsCustom(0, 3, 1, 1, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 3));

        setMinimumSize(getPreferredSize());
        setMaximumSize(getPreferredSize());
    }

    public JPanel getParamPanel() {
        JPanel newPanel = new JPanel(new GridBagLayout());
        newPanel.add(missionPanel,
                new GridBagConstraintsCustom(0, 0, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 3));
        newPanel.add(otherPanel,
                new GridBagConstraintsCustom(0, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 2));
        return newPanel;
    }
    //ToDo: missionDataDir test should be differentiated for local and remote servers

    protected void reorganizePanel(JPanel paramPanel) {

        String selectedOcsswTagString = (seadasToolboxVersion.getInstalledOCSSWTag() != null) ?
                seadasToolboxVersion.getInstalledOCSSWTag() :
                seadasToolboxVersion.getLatestOCSSWTagForInstalledRelease();

        dirPanel = new JPanel();
        tagPanel = new JPanel();
        missionPanel = new JPanel(new TableLayout(5));
        missionPanel.setBorder(BorderFactory.createTitledBorder("Mission Data"));

        otherPanel = new JPanel();
        TableLayout otherPanelLayout = new TableLayout(3);
        otherPanelLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        otherPanel.setLayout(otherPanelLayout);
        otherPanel.setBorder(BorderFactory.createTitledBorder("Others"));
        OCSSWInfo ocsswInfo = OCSSWInfo.getInstance();

        JScrollPane jsp = (JScrollPane) paramPanel.getComponent(0);
        JPanel panel = (JPanel) findJPanel(jsp, "param panel");
        Component[] options = panel.getComponents();
        String tmpString;
        for (Component option : options) {
            if (option.getName().equals("boolean field panel")) {
                Component[] bps = ((JPanel) option).getComponents();
                for (Component c : bps) {
                    tmpString = ParamUtils.removePreceedingDashes(c.getName()).toUpperCase();
                    if (MISSIONS.contains(tmpString)) {
                        if (!DEFAULT_MISSIONS.contains(tmpString)) {
                            ((JLabel) ((JPanel) c).getComponent(0)).setToolTipText(((JPanel) c).getComponent(1).getAccessibleContext().getAccessibleDescription());
                            if (ocssw.isMissionDirExist(tmpString) ||
                                    missionDataStatus.get(tmpString)) {
                                ((JPanel) c).getComponents()[0].setEnabled(false);
                            } else {
                                ((JPanel) c).getComponents()[0].setEnabled(true);
                            }
                            missionPanel.add(c);
                        }
                    } else {
                        if (tmpString.equals("SRC")) {
                            ((JLabel) ((JPanel) c).getComponent(0)).setText("Source Code");
                            ((JLabel) ((JPanel) c).getComponent(0)).setToolTipText("install source code");
                            if (new File(ocsswInfo.getOcsswRoot() + System.getProperty("file.separator") + OCSSW_SRC_DIR_NAME).exists()) {
                                ((JPanel) c).getComponents()[0].setEnabled(false);
                            }
                        } else if (tmpString.equals("CLEAN")) {
                            ((JLabel) ((JPanel) c).getComponent(0)).setText("Clean Install");
                            ((JLabel) ((JPanel) c).getComponent(0)).setToolTipText("clean install");
                            ((JPanel) c).getComponents()[0].setEnabled(true);
                        } else if (tmpString.equals("VIIRSDEM")) {
                            ((JLabel) ((JPanel) c).getComponent(0)).setText("VIIRS DEM files");
                            ((JLabel) ((JPanel) c).getComponent(0)).setToolTipText("install VIIRS DEM files needed for geolocation");
                            if (new File(ocsswInfo.getOcsswRoot() + System.getProperty("file.separator") +
                                    "share" + System.getProperty("file.separator") + "viirs" +
                                    System.getProperty("file.separator") + "dem").exists()) {
                                ((JPanel) c).getComponents()[0].setEnabled(false);
                            }
                        }
                        otherPanel.add(c);
                        otherPanel.add(new JLabel("      "));
                    }
                }
            } else if (option.getName().equals("file parameter panel")) {
                Component[] bps = ((JPanel) option).getComponents();
                for (Component c : bps) {
                    dirPanel = (JPanel) c;

                }
                if (!ocsswInfo.getOcsswLocation().equals(ocsswInfo.OCSSW_LOCATION_LOCAL)) {
                    //if ocssw is not local, then disable the button to choose ocssw installation directory
                    ((JLabel) dirPanel.getComponent(0)).setText("Remote install_dir");
                } else {
                    ((JLabel) dirPanel.getComponent(0)).setText("Local install_dir");
                }
                ((JLabel) dirPanel.getComponent(0)).setToolTipText("This directory can be set in SeaDAS-OCSSW > OCSSW Configuration");
                ((JTextField) dirPanel.getComponent(1)).setEditable(false);
                ((JTextField) dirPanel.getComponent(1)).setFocusable(false);
                ((JTextField) dirPanel.getComponent(1)).setEnabled(false);
                ((JTextField) dirPanel.getComponent(1)).setBorder(null);
                ((JTextField) dirPanel.getComponent(1)).setDisabledTextColor(Color.BLUE);
                ((JTextField) dirPanel.getComponent(1)).setForeground(Color.BLUE);
                ((JTextField) dirPanel.getComponent(1)).setBackground(dirPanel.getBackground());
//                ((JTextField) dirPanel.getComponent(1)).setBackground(new Color(250,250,250));
                ((JTextField) dirPanel.getComponent(1)).setToolTipText("This directory can be set in SeaDAS-OCSSW > OCSSW Configuration");
                dirPanel.getComponent(2).setVisible(false);

            } else if (option.getName().equals("text field panel")) {
                Component[] bps = ((JPanel) option).getComponents();
                JPanel tempPanel1, tempPanel2;
                for (Component c : bps) {
                    if (c.getName().equals(VALID_TAGS_OPTION_NAME)) {
                        tempPanel1 = (JPanel) c;


                        if (SeadasToolboxVersion.isLatestSeadasToolboxVersion()) {
                            if (latestOCSSWTagForInstalledRelease == null) {
                                ((JLabel) tempPanel1.getComponent(0)).setText("OCSSW Tag:");
                                ((JLabel) tempPanel1.getComponent(0)).setForeground(Color.BLACK);
                            } else if (latestOCSSWTagForInstalledRelease.equals(selectedOcsswTagString)) {
                                ((JLabel) tempPanel1.getComponent(0)).setText("OCSSW Tag: (latest tag installed)");
                                ((JLabel) tempPanel1.getComponent(0)).setForeground(Color.BLACK);
                            } else {
                                ((JLabel) tempPanel1.getComponent(0)).setText("<html>OCSSW Tag: WARNING!! <br> latest tag is " + latestOCSSWTagForInstalledRelease + "</html>");
                                ((JLabel) tempPanel1.getComponent(0)).setForeground(Color.RED);
                            }
                        } else {
                            if (latestOCSSWTagForInstalledRelease == null) {
                                ((JLabel) tempPanel1.getComponent(0)).setText("<html>OCSSW Tag: <br>Newer version of SeaDAS Toolbox available</html>");
                            } else if (latestOCSSWTagForInstalledRelease.equals(selectedOcsswTagString)) {
                                if (selectedOcsswTagString.equals(seadasToolboxVersion.getLatestOCSSWTagForLatestRelease())) {
                                    ((JLabel) tempPanel1.getComponent(0)).setText("<html>OCSSW Tag:  <br>Newer version of SeaDAS Toolbox available</html>");
                                } else {
                                    ((JLabel) tempPanel1.getComponent(0)).setText("<html>OCSSW Tag: WARNING!! <br> Not the latest tag<br>Newer version of SeaDAS Toolbox available</html>");
                                }
                            } else {
                                if (selectedOcsswTagString.equals(seadasToolboxVersion.getLatestOCSSWTagForLatestRelease())) {
                                    ((JLabel) tempPanel1.getComponent(0)).setText("<html>OCSSW Tag: WARNING!! <br> May not be applicable tag to installed version<br>Newer version of SeaDAS Toolbox available</html>");
                                } else {
                                    ((JLabel) tempPanel1.getComponent(0)).setText("<html>OCSSW Tag: WARNING!! <br> May not be latest/applicable tag<br>Newer version of SeaDAS Toolbox available</html>");
                                }
                            }

                            ((JLabel) tempPanel1.getComponent(0)).setForeground(Color.RED);
                        }


                        JComboBox tags = ((JComboBox) tempPanel1.getComponent(1));
                        tags.setMaximumRowCount(15);
                        JLabel tmp = new JLabel("12345678901234567890");
                        tags.setMinimumSize(tmp.getPreferredSize());


                        //This segment of code is to disable tags that are not compatible with the current SeaDAS version
                        ArrayList<String> validOcsswTags = ocssw.getOcsswTagsValid4CurrentSeaDAS();
                        Font f1 = tags.getFont();
                        Font f2 = new Font("Tahoma", 0, 14);

                        if (selectedOcsswTagString != null) {
                            tags.setSelectedItem(selectedOcsswTagString);
                        }

                        tags.setToolTipText("Latest tag for this release is " + selectedOcsswTagString);


                        tags.setRenderer(new DefaultListCellRenderer() {
                            @Override
                            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                                          int index, boolean isSelected, boolean cellHasFocus) {
                                if (value instanceof JComponent)
                                    return (JComponent) value;

                                boolean itemEnabled = validOCSSWTagList.contains((String) value);
                                super.getListCellRendererComponent(list, value, index,
                                        isSelected && itemEnabled, cellHasFocus);


                                if (itemEnabled) {
                                    if (selectedOcsswTagString.equals(value.toString().trim())) {
                                        list.setToolTipText(value.toString() + " is latest operational tag for this release");
                                    } else {
                                        list.setToolTipText(value.toString() + " is NOT the latest operational tag for this release");
                                    }
                                    if (isSelected) {
                                        setBackground(Color.blue);
                                        setForeground(Color.white);
                                    } else {
                                        setBackground(Color.white);
                                        setForeground(Color.black);
                                    }
                                } else {
                                    if (value.toString().toUpperCase().startsWith("V")) {
                                        list.setToolTipText(value.toString() + " is NOT an optimum operational tag for this release");
                                    } else {
                                        list.setToolTipText(value.toString() + " is NOT an operational tag");
                                    }
                                    if (isSelected) {
                                        setBackground(Color.darkGray);
                                        setForeground(Color.white);
                                    } else {
                                        setBackground(Color.white);
                                        setForeground(Color.gray);
                                    }
                                }

                                return this;
                            }
                        });

                        // code segment ends here
                        tagPanel.add(tempPanel1);
                        ;
                    } else if (c.getName().contains(CURRENT_TAG_OPTION_NAME)) {
                        //|| CURRENT_TAG_OPTION_NAME.contains(c.getName())) {
                        tempPanel2 = (JPanel) c;
                        ((JLabel) tempPanel2.getComponent(0)).setText("Last Installed OCSSW Tag:");
                        tagPanel.add(tempPanel2);
                    }
                }
            }
        }

    }


    private Component findJPanel(Component comp, String panelName) {
        if (comp.getClass() == JPanel.class) return comp;
        if (comp instanceof Container) {
            Component[] components = ((Container) comp).getComponents();
            for (int i = 0; i < components.length; i++) {
                Component child = findJPanel(components[i], components[i].getName());
                if (child != null) {
                    return child;
                }
            }
        }
        return null;
    }


}