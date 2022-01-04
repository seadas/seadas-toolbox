/*
Author: Danny Knowles
    Don Shea
*/

package gov.nasa.gsfc.seadas.processing.processor;

import com.bc.ceres.swing.selection.SelectionChangeEvent;
import com.bc.ceres.swing.selection.SelectionChangeListener;
import gov.nasa.gsfc.seadas.processing.common.*;
import gov.nasa.gsfc.seadas.processing.core.MultiParamList;
import gov.nasa.gsfc.seadas.processing.core.ParamInfo;
import gov.nasa.gsfc.seadas.processing.core.ParamList;
import gov.nasa.gsfc.seadas.processing.core.ProcessorModel;
import gov.nasa.gsfc.seadas.processing.ocssw.OCSSW;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.ui.AppContext;

import javax.swing.*;
import javax.swing.event.SwingPropertyChangeSupport;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import static gov.nasa.gsfc.seadas.processing.common.FileSelector.PROPERTY_KEY_APP_LAST_OPEN_DIR;

public class MultlevelProcessorForm extends JPanel implements CloProgramUI {

    public enum Processor {
        MAIN("main"),
        MODIS_L1A("modis_L1A"),
//        L1AEXTRACT_MODIS("l1aextract_modis"),
//        L1AEXTRACT_SEAWIFS("l1aextract_seawifs"),
//        L1AEXTRACT_VIIRS("l1aextract_viirs"),
        EXTRACTOR("l1aextract"),
//        L1MAPGEN("l1mapgen"),
//        GEO("geo"),
        GEOLOCATE_HAWKEYE("geolocate_hawkeye"),
        GEOLOCATE_VIIRS("geolocate_viirs"),
        MODIS_GEO("modis_GEO"),
        MIXED_GEO("mixed_GEO"),
        MODIS_L1B("modis_L1B"),
        MIXED_L1B("mixed_L1B"),
        CALIBRATE_VIIRS("calibrate_viirs"),
//        LEVEL_1B("level 1b"),
        L1BGEN("l1bgen_generic"),
        L1BRSGEN("l1brsgen"),
        L2GEN("l2gen"),
        L2EXTRACT("l2extract"),
        L2BRSGEN("l2brsgen"),
//        L2MAPGEN("l2mapgen"),
        L2BIN("l2bin"),
        L3BIN("l3bin"),
        //        SMIGEN("smigen"),
        L3MAPGEN("l3mapgen");

        private Processor(String name) {
            this.name = name;
        }

        private final String name;

        public String toString() {
            return name;
        }
    }


    /*
   MultlevelProcessorForm
       tabbedPane
           mainPanel
               primaryIOPanel
                   sourceProductFileSelector (ifile)
               parfilePanel
                   importPanel
                       importParfileButton
                       retainParfileCheckbox
                   exportParfileButton
                   parfileScrollPane
                       parfileTextArea
           chainScrollPane
               chainPanel
                   nameLabel
                   keepLabel
                   paramsLabel
                   configLabel
                   progRowPanel


    */

    private AppContext appContext;

    private JFileChooser jFileChooser;

    private final JTabbedPane tabbedPane;

    private JPanel mainPanel;
    private JPanel primaryPanel;
    private JPanel primaryIOPanel;
    private JPanel primaryOptionPanel;
    private SeadasFileSelector sourceProductFileSelector;
    private JScrollPane parfileScrollPane;
    private JPanel parfilePanel;
    private JPanel importPanel;
    private JButton importParfileButton;
    private JCheckBox retainIFileCheckbox;
    private JCheckBox overwriteCheckBox;
    private JCheckBox use_existingCheckBox;
    private JCheckBox deletefilesCheckBox;
    private JCheckBox use_ancillaryCheckBox;
    private JButton exportParfileButton;
    private JTextArea parfileTextArea;
    private FileSelector odirSelectorOld;
    private ActiveFileSelector odirSelector;

    private JScrollPane chainScrollPane;
    private JPanel chainPanel;
    private JLabel nameLabel;
    private JLabel plusLabel;
    private JLabel paramsLabel;
    private JLabel odirLabel;

    private JPanel spacer;

    public String MAIN_PARSTRING_EVENT = "MAIN_PARSTRING_EVENT";
    static public String ODIR_EVENT = "ODIR";
    static public final String IFILE = "ifile";
    public String missionName;
    public FileInfoFinder fileInfoFinder;

    private ArrayList<MultilevelProcessorRow> rows;

    String xmlFileName;
    ProcessorModel processorModel;
    private SwingPropertyChangeSupport propertyChangeSupport;

    private boolean checkboxControlHandlerEnabled = true;

    boolean displayDisabledProcessors = true;  // todo  maybe add as a preferences

    OCSSW ocssw;

    MultlevelProcessorForm(AppContext appContext, String xmlFileName, OCSSW ocssw) {
        this.appContext = appContext;
        this.xmlFileName = xmlFileName;
        this.ocssw = ocssw;

        propertyChangeSupport = new SwingPropertyChangeSupport(this);

        jFileChooser = new JFileChooser();

        // create main panel
        sourceProductFileSelector = new SeadasFileSelector(SnapApp.getDefault().getAppContext(), IFILE, true);
        sourceProductFileSelector.initProducts();
        //sourceProductFileSelector.setProductNameLabel(new JLabel(IFILE));
        sourceProductFileSelector.getFileNameComboBox().setPrototypeDisplayValue(
                "123456789 123456789 123456789 123456789 123456789 ");
        sourceProductFileSelector.addSelectionChangeListener(new SelectionChangeListener() {
            @Override
            public void selectionChanged(SelectionChangeEvent selectionChangeEvent) {
                handleIFileChanged();
            }

            @Override
            public void selectionContextChanged(SelectionChangeEvent selectionChangeEvent) {
                //To change body of implemented methods use File | Settings | File Templates.
            }
        });


        odirSelector = new ActiveFileSelector(propertyChangeSupport, ODIR_EVENT, "odir", ParamInfo.Type.DIR);

//        odirSelector = new FileSelector(VisatApp.getApp(), ParamInfo.Type.DIR, "odir");
//
//        odirSelector.addPropertyChangeListener(new PropertyChangeListener() {
//            @Override
//            public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
//                handleOdirChanged();
//            }
//        });

        odirSelector.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                handleOdirChanged();
            }
        });

        overwriteCheckBox = new JCheckBox("overwrite");
        overwriteCheckBox.setToolTipText("overwrite existing intermediate and output files");
        overwriteCheckBox.setSelected(false);
        overwriteCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (checkboxControlHandlerEnabled) {
                    setCheckboxControlHandlerEnabled(false);
                    handleoverwriteCheckBox();
                    setCheckboxControlHandlerEnabled(true);
                }
            }
        });

        use_existingCheckBox = new JCheckBox("use_existing");
        use_existingCheckBox.setToolTipText("do not re-create intermediate files if they already exist");
        use_existingCheckBox.setSelected(false);
        use_existingCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (checkboxControlHandlerEnabled) {
                    setCheckboxControlHandlerEnabled(false);
                    handleuse_existingCheckBox();
                    setCheckboxControlHandlerEnabled(true);
                }
            }
        });

        deletefilesCheckBox = new JCheckBox("deletefiles");
        deletefilesCheckBox.setToolTipText("delete intermediate files");
        deletefilesCheckBox.setSelected(false);
        deletefilesCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (checkboxControlHandlerEnabled) {
                    setCheckboxControlHandlerEnabled(false);
                    handledeletefilesCheckBox();
                    setCheckboxControlHandlerEnabled(true);
                }
            }
        });

        use_ancillaryCheckBox = new JCheckBox("use_ancillary");
        use_ancillaryCheckBox.setToolTipText("Get the ancillary data for l2gen processing");
        use_ancillaryCheckBox.setSelected(false);
        use_ancillaryCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (checkboxControlHandlerEnabled) {
                    setCheckboxControlHandlerEnabled(false);
                    handleuse_ancillaryCheckBox();
                    setCheckboxControlHandlerEnabled(true);
                }
            }
        });

        primaryIOPanel = new JPanel(new GridBagLayout());
        primaryIOPanel.setBorder(BorderFactory.createTitledBorder("Primary I/O Files"));
        primaryIOPanel.add(sourceProductFileSelector.createDefaultPanel(),
                new GridBagConstraintsCustom(0, 0, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL));

        primaryIOPanel.add(odirSelector.getJPanel(),
                new GridBagConstraintsCustom(0, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL));

        primaryOptionPanel = new JPanel(new GridBagLayout());
        primaryOptionPanel.setBorder(BorderFactory.createTitledBorder("Main Options"));

        primaryOptionPanel.add(overwriteCheckBox,
                new GridBagConstraintsCustom(0, 0, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL));
        primaryOptionPanel.add(use_existingCheckBox,
                new GridBagConstraintsCustom(1, 0, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL));
        primaryOptionPanel.add(deletefilesCheckBox,
                new GridBagConstraintsCustom(2, 0, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL));
        primaryOptionPanel.add(use_ancillaryCheckBox,
                new GridBagConstraintsCustom(3, 0, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL));

        primaryPanel = new JPanel(new GridBagLayout());
        primaryPanel.add(primaryIOPanel,
                new GridBagConstraintsCustom(0, 0, 1, 1, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL));
        primaryPanel.add(primaryOptionPanel,
                new GridBagConstraintsCustom(0, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL));

        retainIFileCheckbox = new JCheckBox("Retain Selected IFILE");

        importParfileButton = new JButton("Load Parameters");
        importParfileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String contents = SeadasGuiUtils.importFile(jFileChooser);
                if (contents != null) {
                    File defaultIFileDirectory = null;
                    if (jFileChooser.getSelectedFile() != null) {
                        defaultIFileDirectory = jFileChooser.getSelectedFile().getParentFile();
                    }
                    setParamString(contents, retainIFileCheckbox.isSelected(), defaultIFileDirectory);
                }
            }
        });

        importPanel = new JPanel(new GridBagLayout());
        importPanel.setBorder(BorderFactory.createEtchedBorder());
        importPanel.add(importParfileButton,
                new GridBagConstraintsCustom(0, 0, 1, 1, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL));
        importPanel.add(retainIFileCheckbox,
                new GridBagConstraintsCustom(1, 0, 1, 1, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL));

        exportParfileButton = new JButton("Save Parameters");
        exportParfileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String contents = getParamString();
                SeadasGuiUtils.exportFile(jFileChooser, contents + "\n");
            }
        });

        parfileTextArea = new JTextArea();
        parfileTextArea.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void focusLost(FocusEvent e) {
                handleParamStringChange();
            }
        });
        parfileScrollPane = new JScrollPane(parfileTextArea);
        parfileScrollPane.setBorder(null);

        parfilePanel = new JPanel(new GridBagLayout());
        parfilePanel.setBorder(BorderFactory.createTitledBorder("Parfile"));
        parfilePanel.add(importPanel,
                new GridBagConstraintsCustom(0, 0, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE));
        parfilePanel.add(exportParfileButton,
                new GridBagConstraintsCustom(1, 0, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE));
        parfilePanel.add(parfileScrollPane,
                new GridBagConstraintsCustom(0, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH, 0, 2));

        mainPanel = new JPanel(new GridBagLayout());
        mainPanel.add(primaryPanel,
                new GridBagConstraintsCustom(0, 0, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL));
        mainPanel.add(parfilePanel,
                new GridBagConstraintsCustom(0, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH));

        // create chain panel
        nameLabel = new JLabel("Processor");
        Font font = nameLabel.getFont().deriveFont(Font.BOLD);
        nameLabel.setFont(font);
        plusLabel = new JLabel("+");
        plusLabel.setToolTipText("Add the processor to the chain");
        plusLabel.setFont(font);
        paramsLabel = new JLabel("Parameters");
        paramsLabel.setFont(font);
        paramsLabel.setToolTipText("Parameters for the processor");
        odirLabel = new JLabel("Odir");
        odirLabel.setFont(font);
        spacer = new JPanel();

        chainPanel = new JPanel(new GridBagLayout());

        chainPanel.add(nameLabel,
                new GridBagConstraintsCustom(0, 0, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(2, 2, 2, -8)));
        chainPanel.add(plusLabel,
                new GridBagConstraintsCustom(1, 0, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(2, -8, 2, -8)));
        chainPanel.add(paramsLabel,
                new GridBagConstraintsCustom(2, 0, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(2, -8, 2, 2)));
        chainPanel.add(odirLabel,
                new GridBagConstraintsCustom(3, 0, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(2, -8, 2, 2)));

        createRows();
        int rowNum = 1;
        for (MultilevelProcessorRow row : rows) {
            if (!row.getName().equals(Processor.MAIN.toString())) {
                row.attachComponents(chainPanel, rowNum);
                rowNum++;
            }
        }
        chainPanel.add(spacer,
                new GridBagConstraintsCustom(0, rowNum, 0, 1, GridBagConstraints.WEST, GridBagConstraints.VERTICAL));

        chainScrollPane = new JScrollPane(chainPanel);
        chainScrollPane.setBorder(null);

        tabbedPane = new JTabbedPane();
        tabbedPane.add("Main", mainPanel);
        tabbedPane.add("Processor Chain", chainScrollPane);

        // add the tabbed pane
        setLayout(new GridBagLayout());
        add(tabbedPane, new GridBagConstraintsCustom(0, 0, 1, 1, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH));

//        setMinimumSize(getPreferredSize());
//        setMaximumSize(new Dimension(2000,2000));
    }

    void createRows() {
        Processor[] rowNames = {
                Processor.MAIN,
                Processor.MODIS_L1A,
//                Processor.GEO,
                Processor.MODIS_GEO,
                Processor.GEOLOCATE_HAWKEYE,
                Processor.GEOLOCATE_VIIRS,
                Processor.MIXED_GEO,
                Processor.EXTRACTOR,
//                Processor.L1AEXTRACT_MODIS,
//                Processor.L1AEXTRACT_SEAWIFS,
//                Processor.L1AEXTRACT_VIIRS,
//                Processor.L1MAPGEN,
//                Processor.LEVEL_1B,
                Processor.MODIS_L1B,
                Processor.CALIBRATE_VIIRS,
                Processor.L1BGEN,
                Processor.MIXED_L1B,
                Processor.L1BRSGEN,
                Processor.L2GEN,
                Processor.L2EXTRACT,
                Processor.L2BRSGEN,
//                Processor.L2MAPGEN,
                Processor.L2BIN,
                Processor.L3BIN,
//                Processor.SMIGEN,
                Processor.L3MAPGEN
        };
        rows = new ArrayList<MultilevelProcessorRow>();

        for (Processor processor : rowNames) {
            MultilevelProcessorRow row = new MultilevelProcessorRow(processor.toString(), this, ocssw);
            row.addPropertyChangeListener(MultilevelProcessorRow.PARAM_STRING_EVENT, new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    updateParamString();
                }
            });
            rows.add(row);

        }
    }


    public AppContext getAppContext() {
        return appContext;
    }

    @Override
    public JPanel getParamPanel() {
        return this;
    }

    public ParamList getFinalParamList() {
        MultiParamList paramList = new MultiParamList();
        for (MultilevelProcessorRow row : rows) {
            String name = row.getName();
            ParamList list = (ParamList) row.getParamList().clone();
            list.removeInfo(MultilevelProcessorRow.PLUS_PARAM);
            if ((name.equals(Processor.MODIS_L1B.toString()) ||
                    name.equals(Processor.CALIBRATE_VIIRS.toString()) ||
                    name.equals(Processor.L1BGEN.toString()) ||
                    name.equals(Processor.MIXED_L1B.toString())) &&
                    !list.getParamArray().isEmpty()) {
                name = "level 1b";
            }
            if ((name.equals(Processor.MODIS_GEO.toString()) ||
                    name.equals(Processor.GEOLOCATE_HAWKEYE.toString()) ||
                    name.equals(Processor.MIXED_GEO.toString()) ||
                    name.equals(Processor.GEOLOCATE_VIIRS.toString())) &&
                    !list.getParamArray().isEmpty()) {
                name = "geo";
            }
            paramList.addParamList(name, list);
//            paramList.addParamList(name, row.getParamList());
        }
        return paramList;
    }

    public ParamList getParamList() {
        MultiParamList paramList = new MultiParamList();
        for (MultilevelProcessorRow row : rows) {
            String name = row.getName();
            if (!row.getParamList().getParamArray().isEmpty()){
                if (name.equals(Processor.MODIS_L1B.toString()) && missionName != null) {
                    if (missionName.contains("MODIS")) {
                        name = "level 1b";
//                    } else {
//                        row.deselectPlusCheckBox();
//                        SimpleDialogMessage dialog = new SimpleDialogMessage(null, "ERROR: can't do modis_L1B on the ifile");
//                        dialog.setVisible(true);
//                        dialog.setEnabled(true);
                    }
                } else if (name.equals(Processor.MIXED_L1B.toString())) {
                    if (missionName.contains("mixed") && missionName != null) {
                        name = "level 1b";
//                    } else {
//                        row.deselectPlusCheckBox();
//                        SimpleDialogMessage dialog = new SimpleDialogMessage(null, "can't do calibrate_viirs on the ifile");
//                        dialog.setVisible(true);
//                        dialog.setEnabled(true);
                    }
                } else if (name.equals(Processor.CALIBRATE_VIIRS.toString()) && missionName != null) {
                    if (missionName.contains("VIIRS")) {
                        name = "level 1b";
//                    } else {
//                        row.deselectPlusCheckBox();
//                        SimpleDialogMessage dialog = new SimpleDialogMessage(null, "can't do calibrate_viirs on the ifile");
//                        dialog.setVisible(true);
//                        dialog.setEnabled(true);
                    }
                } else if (name.equals(Processor.L1BGEN.toString()) && missionName != null) {
                    if (!missionName.contains("MODIS") && !missionName.contains("VIIRS")) {
                        name = "level 1b";
//                    } else {
//                        row.deselectPlusCheckBox();
//                        SimpleDialogMessage dialog = new SimpleDialogMessage(null, "can't do l1bgen_generic on the ifile");
//                        dialog.setVisible(true);
//                        dialog.setEnabled(true);
                    }
                } else if (name.equals(Processor.MODIS_GEO.toString()) && missionName != null) {
                    if (missionName.contains("MODIS")) {
                        name = "geo";
//                    } else {
//                        row.deselectPlusCheckBox();
//                        SimpleDialogMessage dialog = new SimpleDialogMessage(null, "can't do modis_GEO on the ifile");
//                        dialog.setVisible(true);
//                        dialog.setEnabled(true);
                    }
                } else if (name.equals(Processor.MIXED_GEO.toString()) && missionName != null) {
                    if (missionName.contains("mixed")) {
                        name = "geo";
//                    } else {
//                        row.deselectPlusCheckBox();
//                        SimpleDialogMessage dialog = new SimpleDialogMessage(null, "can't do modis_GEO on the ifile");
//                        dialog.setVisible(true);
//                        dialog.setEnabled(true);
                    }
                } else if (name.equals(Processor.GEOLOCATE_HAWKEYE.toString()) && missionName != null) {
                    if (missionName.contains("HAWKEYE")) {
                        name = "geo";
//                    } else {
//                        row.deselectPlusCheckBox();
//                        SimpleDialogMessage dialog = new SimpleDialogMessage(null, "can't do geolocate__hawkeye on the ifile");
//                        dialog.setVisible(true);
//                        dialog.setEnabled(true);
                    }
                } else if (name.equals(Processor.GEOLOCATE_VIIRS.toString()) && missionName != null) {
                    if (missionName.contains("VIIRS")) {
                        name = "geo";
//                    } else {
//                        row.deselectPlusCheckBox();
//                        SimpleDialogMessage dialog = new SimpleDialogMessage(null, "can't do do geolocate_viirs on the ifile");
//                        dialog.setVisible(true);
//                        dialog.setEnabled(true);
                    }
                }
            }
            paramList.addParamList(name, row.getParamList());
        }
        return paramList;
    }

    @Override
    public ProcessorModel getProcessorModel() {
        if (processorModel == null) {
            processorModel = new MultilevelProcessorModel("multilevel_processor", xmlFileName, ocssw);
            processorModel.setReadyToRun(true);
        }
        processorModel.setParamList(getFinalParamList());
        return processorModel;
    }

    @Override
    public File getSelectedSourceProduct() {
        if (getSourceProductFileSelector() != null) {
            return getSourceProductFileSelector().getSelectedFile();
        }
        return null;
    }

    @Override
    public boolean isOpenOutputInApp() {
        return false;
    }

    public SeadasFileSelector getSourceProductFileSelector() {
        return sourceProductFileSelector;
    }

    public void prepareShow() {
        if (getSourceProductFileSelector() != null) {
            getSourceProductFileSelector().initProducts();
        }
    }

    public void prepareHide() {
        if (getSourceProductFileSelector() != null) {
            getSourceProductFileSelector().releaseFiles();
        }
    }

    private MultilevelProcessorRow getRow(String name) {
        for (MultilevelProcessorRow row : rows) {
            if (row.getName().equals(name)) {
                return row;
            } else {
                if (name.equals("level 1b")) {
                    if (row.getName().equals(Processor.MODIS_L1B.toString())) {
                        if (missionName != null && missionName.contains("MODIS")) {
                            return row;
                        }
                    }
                    if (row.getName().equals(Processor.MIXED_L1B.toString())) {
                        if (missionName != null && missionName.contains("mixed")) {
                            return row;
                        }
                    }
                    if (row.getName().equals(Processor.CALIBRATE_VIIRS.toString())) {
                        if (missionName != null && missionName.contains("VIIRS")) {
                            return row;
                        }
                    }
                    if (row.getName().equals(Processor.L1BGEN.toString())) {
                        if (missionName != null &&
                                !missionName.contains("MODIS") &&
                                !missionName.contains("mixed") &&
                                !missionName.contains("VIIRS")) {
                            return row;
                        }
                    }
                }
                if (name.equals("geo")) {
                    if (row.getName().equals(Processor.MODIS_GEO.toString())) {
                        if (missionName != null && missionName.contains("MODIS")) {
                            return row;
                        }
                    }
                    if (row.getName().equals(Processor.MIXED_GEO.toString())) {
                        if (missionName != null && missionName.contains("mixed")) {
                            return row;
                        }
                    }
                    if (row.getName().equals(Processor.GEOLOCATE_HAWKEYE.toString())) {
                        if (missionName != null && missionName.contains("HAWKEYE")) {
                            return row;
                        }
                    }
                    if (row.getName().equals(Processor.GEOLOCATE_VIIRS.toString())) {
                        if (missionName != null && missionName.contains("VIIRS")) {
                            return row;
                        }
                    }
                }
            }
        }
        return null;
    }

    public String getParamString() {
        String paramString = getParamList().getParamString("\n");

        // This rigged up thingy adds in some comments
        String[] lines = paramString.split("\n");
        StringBuilder stringBuilder = new StringBuilder();

        for (String line : lines) {
            line = line.trim();
            if (!line.contains(MultilevelProcessorRow.PLUS_PARAM)) {
                stringBuilder.append(line).append("\n");
            }
            if (line.toLowerCase().startsWith(IFILE)) {
                String iFilename = line.substring(IFILE.length() + 1, line.length()).trim();
                File iFile = new File(iFilename);

                if (!iFile.exists()) {
                    stringBuilder.append("## WARNING: ifile '").append(iFilename).append("' does not exist").append("\n");
                }
            }
        }

        return stringBuilder.toString();
    }

    public void setParamString(String str) {
        setParamString(str, false, null);
    }


    public void setParamString(String str, boolean retainIFile, File defaultIFileDirectory) {

        String oldODir = getRow(Processor.MAIN.toString()).getParamList().getValue(MultilevelProcessorRow.ODIR_PARAM);


        String[] lines = str.split("\n");

        String section = Processor.MAIN.toString();
        StringBuilder stringBuilder = new StringBuilder();

        for (String line : lines) {
            line = line.trim();

            // get rid of comment lines
            if (line.length() > 0 && line.charAt(0) != '#') {

                // locate new section line
                if (line.charAt(0) == '[' && line.contains("]")) {

                    // determine next section
                    int endIndex = line.indexOf(']');
                    String nextSection = line.substring(1, endIndex).trim();


                    if (nextSection.length() > 0) {

                        // set the params for this section
                        MultilevelProcessorRow row = getRow(section);
                        if (stringBuilder.length() > 0) {
                            if (row != null) {
                                if (!row.getName().equals(Processor.MAIN.toString())) {
                                    row.setParamString("plusToChain=1", retainIFile);
                                }
                                row.setParamString(stringBuilder.toString(), retainIFile);
                            }
                            if (row.getName().equals(Processor.MAIN.toString())) {
                                if (row.getParamList().getValue("overwrite").equals(ParamInfo.BOOLEAN_TRUE)) {
                                    overwriteCheckBox.setSelected(true);
                                } else {
                                    overwriteCheckBox.setSelected(false);
                                }
                                if (row.getParamList().getValue("use_existing").equals(ParamInfo.BOOLEAN_TRUE)) {
                                    use_existingCheckBox.setSelected(true);
                                } else {
                                    use_existingCheckBox.setSelected(false);
                                }
                                if (row.getParamList().getValue("deletefiles").equals(ParamInfo.BOOLEAN_TRUE)) {
                                    deletefilesCheckBox.setSelected(true);
                                } else {
                                    deletefilesCheckBox.setSelected(false);
                                }
                                if (row.getParamList().getValue("use_ancillary").equals(ParamInfo.BOOLEAN_TRUE)) {
                                    use_ancillaryCheckBox.setSelected(true);
                                } else {
                                    use_ancillaryCheckBox.setSelected(false);
                                }
                            }
                            stringBuilder.setLength(0);
                        } else if (!nextSection.equals(Processor.MAIN.toString())){
                            if (row != null) {
                                row.setParamString("plusToChain=1", retainIFile);
                            }
                        }
                        section = nextSection;
                    }


//                    line = line.substring(1).trim();
//                    String[] words = line.split("\\s+", 2);
//                    section = words[0];
//                    int i = section.indexOf(']');
//                    if (i != -1) {
//                        section = section.substring(0, i).trim();
//                    }


                } else {
                    if (line.toLowerCase().startsWith(IFILE)) {

                        String originalIFilename = line.substring(IFILE.length() + 1, line.length()).trim();
                        File originalIFile = new File(originalIFilename);

                        String absoluteIFilename;
                        if (originalIFile.isAbsolute()) {
                            absoluteIFilename = originalIFilename;
                        } else {
                            File absoluteFile = new File(defaultIFileDirectory, originalIFilename);
                            absoluteIFilename = absoluteFile.getAbsolutePath();
                        }

                        findMissionName(absoluteIFilename);

                        stringBuilder.append(IFILE).append("=").append(absoluteIFilename).append("\n");

                    } else {
                        stringBuilder.append(line).append("\n");
                    }
                }
            }
        }

        MultilevelProcessorRow row = getRow(section);
        if (row != null) {
            row.setParamString("plusToChain=1", retainIFile);
            if (stringBuilder.length() > 0) {
                row.setParamString(stringBuilder.toString(), retainIFile);
            }
//            } else {
//                row.setParamString("plusToChain=1", retainIFile);
//            }
            if (row.getName().equals(Processor.MAIN.toString())) {
                if (row.getParamList().getValue("overwrite").equals(ParamInfo.BOOLEAN_TRUE)) {
                    overwriteCheckBox.setSelected(true);
                } else {
                    overwriteCheckBox.setSelected(false);
                }
                if (row.getParamList().getValue("use_existing").equals(ParamInfo.BOOLEAN_TRUE)) {
                    use_existingCheckBox.setSelected(true);
                } else {
                    use_existingCheckBox.setSelected(false);
                }
                if (row.getParamList().getValue("deletefiles").equals(ParamInfo.BOOLEAN_TRUE)) {
                    deletefilesCheckBox.setSelected(true);
                } else {
                    deletefilesCheckBox.setSelected(false);
                }
                if (row.getParamList().getValue("use_ancillary").equals(ParamInfo.BOOLEAN_TRUE)) {
                    use_ancillaryCheckBox.setSelected(true);
                } else {
                    use_ancillaryCheckBox.setSelected(false);
                }
            }
        }

        String newODir = getRow(Processor.MAIN.toString()).getParamList().getValue(MultilevelProcessorRow.ODIR_PARAM);
        propertyChangeSupport.firePropertyChange(ODIR_EVENT, oldODir, newODir);

        for (MultilevelProcessorRow row2 : rows) {
            String name = row2.getName();
            if (!row2.getParamList().getParamArray().isEmpty()){
                if (name.equals(Processor.MODIS_L1B.toString()) ||
                        name.equals(Processor.MIXED_L1B.toString()) ||
                        name.equals(Processor.CALIBRATE_VIIRS.toString()) ||
                        name.equals(Processor.L1BGEN.toString())) {
                    if (!str.contains("level 1b")) {
                        if (row2.getParamList().getValue(MultilevelProcessorRow.PLUS_PARAM).equals(ParamInfo.BOOLEAN_FALSE)) {
                            row2.deselectPlusCheckBox();
                        } else {
                            row2.setParamValue(MultilevelProcessorRow.PLUS_PARAM, ParamInfo.BOOLEAN_FALSE);
                        }
                        row2.setParamValue(MultilevelProcessorRow.ODIR_PARAM, "");
                        row2.clearConfigPanel();
                    }
                } else if (name.equals(Processor.MODIS_GEO.toString()) ||
                        name.equals(Processor.MIXED_GEO.toString()) ||
                        name.equals(Processor.GEOLOCATE_HAWKEYE.toString()) ||
                        name.equals(Processor.GEOLOCATE_VIIRS.toString())) {
                    if (!str.contains("geo")) {
                        row2.setParamValue(MultilevelProcessorRow.ODIR_PARAM, "");
                        if (row2.getParamList().getValue(MultilevelProcessorRow.PLUS_PARAM).equals(ParamInfo.BOOLEAN_FALSE)) {
                            row2.deselectPlusCheckBox();
                        } else {
                            row2.setParamValue(MultilevelProcessorRow.PLUS_PARAM, ParamInfo.BOOLEAN_FALSE);
                        }
                        row2.clearConfigPanel();
                    }
                } else {
                    if (!str.contains(name)) {
                        row2.setParamValue(MultilevelProcessorRow.ODIR_PARAM, "");
                        if (row2.getParamList().getValue(MultilevelProcessorRow.PLUS_PARAM).equals(ParamInfo.BOOLEAN_FALSE)) {
                            row2.deselectPlusCheckBox();
                        } else {
                            row2.setParamValue(MultilevelProcessorRow.PLUS_PARAM, ParamInfo.BOOLEAN_FALSE);
                        }
                        row2.clearConfigPanel();
                    }
                }
            }
        }
        updateParamString();
    }

    private void updateParamString() {

        ArrayList<File> fileArrayList = new ArrayList<File>();
        String fileList = getRow(Processor.MAIN.toString()).getParamList().getValue(IFILE);
        String filenameArray[] = fileList.split(",");
        for (String filename : filenameArray) {
            fileArrayList.add(new File(filename));
        }

        String oldIfileName = null;
        if (sourceProductFileSelector.getSelectedFile() != null) {
            oldIfileName = sourceProductFileSelector.getSelectedFile().getAbsolutePath();
        }
        String newIfileName = getRow(Processor.MAIN.toString()).getParamList().getValue(IFILE);

        if (newIfileName != null && !newIfileName.equals(oldIfileName) || oldIfileName != null && !oldIfileName.equals(newIfileName)) {
            if (fileArrayList.size() > 1) {
                File listFile = setSelectedMultiFileList(fileArrayList);

                sourceProductFileSelector.setSelectedFile(listFile);
            } else {
                File file = new File(getRow(Processor.MAIN.toString()).getParamList().getValue(IFILE));
                sourceProductFileSelector.setSelectedFile(file);
            }
        }

        //todo this should not be needed but is needed at the moment because parfileTextArea doesn't trigger event in this case
//        String newODir = getRow(Processor.MAIN.toString()).getParamList().getValue("odir");
//        propertyChangeSupport.firePropertyChange(ODIR_EVENT, null, newODir);


        parfileTextArea.setText(getParamString());
    }


    public File setSelectedMultiFileList(ArrayList<File> tmpArrayList) {
        File[] files = new File[tmpArrayList.size()];
        tmpArrayList.toArray(files);

        String homeDirPath = SystemUtils.getUserHomeDir().getPath();
        String openDir = appContext.getPreferences().getPropertyString(PROPERTY_KEY_APP_LAST_OPEN_DIR,
                homeDirPath);
        File currentDirectory = new File(openDir);
        File fileListFile = new File(currentDirectory, "_inputFiles.lst");

        StringBuilder fileNames = new StringBuilder();
        for (File file : files) {
            fileNames.append(file.getAbsolutePath() + "\n");
        }
        FileWriter fileWriter = null;
        try {

            fileWriter = new FileWriter(fileListFile);
            fileWriter.write(fileNames.toString());
            fileWriter.close();
        } catch (IOException ioe) {
        }

        return fileListFile;

    }


    private void handleParamStringChange() {
        String newStr = parfileTextArea.getText();
        String oldStr = getParamString();
        if (!newStr.equals(oldStr)) {
            setParamString(newStr);
        }
    }

    private void handleIFileChanged() {
        String ifileName = sourceProductFileSelector.getSelectedFile().getAbsolutePath();
        MultilevelProcessorRow row = getRow(Processor.MAIN.toString());
        String oldIFile = row.getParamList().getValue(IFILE);
        if (!ifileName.equals(oldIFile)) {
            findMissionName(ifileName);
            row.setParamValue(IFILE, ifileName);
            for (MultilevelProcessorRow row2 : rows) {
                if (!row2.getName().equals(Processor.MAIN.toString())) {
                    if (!row2.getParamList().getParamArray().isEmpty()) {
                        row2.setParamValue(MultilevelProcessorRow.PLUS_PARAM, ParamInfo.BOOLEAN_FALSE);
                        row2.getParamList().clear();
                    };
                }
            }
            if (missionName != null) {
                if (missionName.contains("HAWKEYE")) {
                    MultilevelProcessorRow row_geo_hawkeye = getRow(Processor.GEOLOCATE_HAWKEYE.toString());
                    row_geo_hawkeye.setParamValue(MultilevelProcessorRow.PLUS_PARAM, ParamInfo.BOOLEAN_FALSE);
                } else if (missionName.contains("VIIRS")) {
                    MultilevelProcessorRow row_cal_viirs= getRow(Processor.CALIBRATE_VIIRS.toString());
                    MultilevelProcessorRow row_geo_viirs = getRow(Processor.GEOLOCATE_VIIRS.toString());
                    row_cal_viirs.setParamValue(MultilevelProcessorRow.PLUS_PARAM, ParamInfo.BOOLEAN_FALSE);
                    row_geo_viirs.setParamValue(MultilevelProcessorRow.PLUS_PARAM, ParamInfo.BOOLEAN_FALSE);
                } else if (missionName.contains("MODIS")) {
                    MultilevelProcessorRow row_modis_geo= getRow(Processor.MODIS_GEO.toString());
                    MultilevelProcessorRow row_modis_l1b = getRow(Processor.MODIS_L1B.toString());
                    row_modis_geo.setParamValue(MultilevelProcessorRow.PLUS_PARAM, ParamInfo.BOOLEAN_FALSE);
                    row_modis_l1b.setParamValue(MultilevelProcessorRow.PLUS_PARAM, ParamInfo.BOOLEAN_FALSE);
                } else if (missionName.contains("mixed")) {
                    MultilevelProcessorRow row_mixed_geo= getRow(Processor.MIXED_GEO.toString());
                    MultilevelProcessorRow row_mixed_l1b = getRow(Processor.MIXED_L1B.toString());
                    row_mixed_geo.setParamValue(MultilevelProcessorRow.PLUS_PARAM, ParamInfo.BOOLEAN_FALSE);
                    row_mixed_l1b.setParamValue(MultilevelProcessorRow.PLUS_PARAM, ParamInfo.BOOLEAN_FALSE);
                } else {
                    MultilevelProcessorRow row_l1bgen = getRow(Processor.L1BGEN.toString());
                    row_l1bgen.setParamValue(MultilevelProcessorRow.PLUS_PARAM, ParamInfo.BOOLEAN_FALSE);
                }
            }
//            findMissionName(ifileName);
//            row.setParamValue(IFILE, ifileName);
//            for (MultilevelProcessorRow row2 : rows) {
//                if (!row2.getName().equals(Processor.MAIN.toString())) {
//                    row2.getParamList().clear();
//                }
//            }
            parfileTextArea.setText(getParamString());
        }
    }


    private void handleOdirChanged() {
        String odirName = odirSelector.getFilename();
        MultilevelProcessorRow row = getRow(Processor.MAIN.toString());
        String oldOdir = row.getParamList().getValue(MultilevelProcessorRow.ODIR_PARAM);
        if (!odirName.equals(oldOdir)) {
            row.setParamValue(MultilevelProcessorRow.ODIR_PARAM, odirName);
            parfileTextArea.setText(getParamString());
        }
    }


    public String getIFile() {
        return getRow(Processor.MAIN.toString()).getParamList().getValue(IFILE);
    }

    public String getFirstIFile() {
        String fileName = getIFile();
        if (fileName.contains(",")) {
            String[] files = fileName.split(",");
            fileName = files[0].trim();
        } else if (fileName.contains(" ")) {
            String[] files = fileName.trim().split(" ");
            fileName = files[0].trim();
        }

        // todo : need to check for file being a list of files.

        return fileName;
    }

    public void addPropertyChangeListener(String name, PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(name, listener);
    }

    public void removePropertyChangeListener(String name, PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(name, listener);
    }

    public void setCheckboxControlHandlerEnabled(boolean checkboxControlHandlerEnabled) {
        this.checkboxControlHandlerEnabled = checkboxControlHandlerEnabled;
    }

    private void handleoverwriteCheckBox() {
        ParamList paramList = getRow(Processor.MAIN.toString()).getParamList();
        String oldParamString = getParamString();

        if (overwriteCheckBox.isSelected()) {
            paramList.setValue("overwrite", ParamInfo.BOOLEAN_TRUE);
        } else {
            paramList.setValue("overwrite", ParamInfo.BOOLEAN_FALSE);
        }
        String str = getParamString();
        parfileTextArea.setText(str);
        propertyChangeSupport.firePropertyChange("paramString", oldParamString, str);
    }

    private void handleuse_existingCheckBox() {
        ParamList paramList = getRow(Processor.MAIN.toString()).getParamList();
        String oldParamString = getParamString();

        if (use_existingCheckBox.isSelected()) {
            paramList.setValue("use_existing", ParamInfo.BOOLEAN_TRUE);
        } else {
            paramList.setValue("use_existing", ParamInfo.BOOLEAN_FALSE);
        }
        String str = getParamString();
        parfileTextArea.setText(str);
        propertyChangeSupport.firePropertyChange("paramString", oldParamString, str);
    }

    private void handledeletefilesCheckBox() {
        ParamList paramList = getRow(Processor.MAIN.toString()).getParamList();
        String oldParamString = getParamString();

        if (deletefilesCheckBox.isSelected()) {
            paramList.setValue("deletefiles", ParamInfo.BOOLEAN_TRUE);
        } else {
            paramList.setValue("deletefiles", ParamInfo.BOOLEAN_FALSE);
        }
        String str = getParamString();
        parfileTextArea.setText(str);
        propertyChangeSupport.firePropertyChange("paramString", oldParamString, str);
    }

    private void handleuse_ancillaryCheckBox() {
        ParamList paramList = getRow(Processor.MAIN.toString()).getParamList();
        String oldParamString = getParamString();

        if (use_ancillaryCheckBox.isSelected()) {
            paramList.setValue("use_ancillary", ParamInfo.BOOLEAN_TRUE);
        } else {
            paramList.setValue("use_ancillary", ParamInfo.BOOLEAN_FALSE);
        }
        String str = getParamString();
        parfileTextArea.setText(str);
        propertyChangeSupport.firePropertyChange("paramString", oldParamString, str);
    }

    private void findMissionName(String fileName) {
        fileInfoFinder = new FileInfoFinder(fileName, ocssw);
        missionName = fileInfoFinder.getMissionName();
        final ArrayList<String> fileList = SeadasGuiUtils.myReadDataFile(fileName);
        if (missionName != null) {
            if (fileList.size() > 0) { //file contains a list of files
                if (missionName.equals("unknown")) {
                    String instrument = "unknown";
                    for (String nextFileName : fileList) {
                        if (nextFileName.length() > 0 && (nextFileName.charAt(0) != '#')) {
                            if (!nextFileName.contains(File.separator)) {
                                File iFile = new File(fileName);
                                String iFilePath = iFile.getParent();
                                String absoluteFileName = iFilePath + File.separator + nextFileName;
                                fileInfoFinder = new FileInfoFinder(absoluteFileName, ocssw);
                                if (fileInfoFinder.getMissionName() != null
                                        && !fileInfoFinder.getMissionName().equals("unknown")) {
                                    missionName = fileInfoFinder.getMissionName();
                                } else {
                                    continue;
                                }
                            } else {
                                fileInfoFinder = new FileInfoFinder(nextFileName, ocssw);
                                if (fileInfoFinder.getMissionName() != null
                                        && !fileInfoFinder.getMissionName().equals("unknown")) {
                                    missionName = fileInfoFinder.getMissionName();
                                } else {
                                    continue;
                                }
                            }
                            if (instrument.equals("unknown")) {
                                if (missionName.contains(" ")) {
                                    instrument = missionName.split(" ")[0];
                                } else {
                                    instrument = missionName;
                                }
                            } else if (!missionName.contains(instrument)) {
                                missionName = "mixed";
                                setRowVisible(Processor.MODIS_L1A.toString(), false);
                                setRowVisible(Processor.GEOLOCATE_VIIRS.toString(), false);
                                setRowVisible(Processor.GEOLOCATE_HAWKEYE.toString(), false);
                                setRowVisible(Processor.MODIS_GEO.toString(), false);
                                setRowVisible(Processor.MIXED_GEO.toString(), true);
                                setRowVisible(Processor.EXTRACTOR.toString(), false);
                                setRowVisible(Processor.CALIBRATE_VIIRS.toString(), false);
                                setRowVisible(Processor.MODIS_L1B.toString(), false);
                                setRowVisible(Processor.MIXED_L1B.toString(), true);
                                setRowVisible(Processor.L1BGEN.toString(), false);
                                setRowVisible(Processor.L1BRSGEN.toString(), false);
                                break;
                            }
                        }
                    }
                    if (missionName.contains("VIIRS")) {
                        setRowVisible(Processor.MODIS_L1A.toString(), false);
                        setRowVisible(Processor.GEOLOCATE_VIIRS.toString(), true);
                        setRowVisible(Processor.GEOLOCATE_HAWKEYE.toString(), false);
                        setRowVisible(Processor.MODIS_GEO.toString(), false);
                        setRowVisible(Processor.MIXED_GEO.toString(), false);
                        setRowVisible(Processor.CALIBRATE_VIIRS.toString(), true);
                        setRowVisible(Processor.MODIS_L1B.toString(), false);
                        setRowVisible(Processor.MIXED_L1B.toString(), false);
                        setRowVisible(Processor.L1BGEN.toString(), false);
                    } else if (missionName.contains("MODIS")) {
                        setRowVisible(Processor.MODIS_L1A.toString(), true);
                        setRowVisible(Processor.GEOLOCATE_VIIRS.toString(), false);
                        setRowVisible(Processor.GEOLOCATE_HAWKEYE.toString(), false);
                        setRowVisible(Processor.MODIS_GEO.toString(), true);
                        setRowVisible(Processor.MIXED_GEO.toString(), false);
                        setRowVisible(Processor.CALIBRATE_VIIRS.toString(), false);
                        setRowVisible(Processor.MODIS_L1B.toString(), true);
                        setRowVisible(Processor.MIXED_L1B.toString(), false);
                        setRowVisible(Processor.L1BGEN.toString(), false);
                    } else if (missionName.contains("HAWKEYE")) {
                        setRowVisible(Processor.MODIS_L1A.toString(), false);
                        setRowVisible(Processor.GEOLOCATE_VIIRS.toString(), false);
                        setRowVisible(Processor.GEOLOCATE_HAWKEYE.toString(), true);
                        setRowVisible(Processor.MODIS_GEO.toString(), false);
                        setRowVisible(Processor.MIXED_GEO.toString(), false);
                        setRowVisible(Processor.CALIBRATE_VIIRS.toString(), false);
                        setRowVisible(Processor.MODIS_L1B.toString(), false);
                        setRowVisible(Processor.MIXED_L1B.toString(), false);
                        setRowVisible(Processor.L1BGEN.toString(), true);
                    } else if (!missionName.contains("mixed")) {
                        setRowVisible(Processor.MODIS_L1A.toString(), false);
                        setRowVisible(Processor.GEOLOCATE_VIIRS.toString(), false);
                        setRowVisible(Processor.GEOLOCATE_HAWKEYE.toString(), false);
                        setRowVisible(Processor.MODIS_GEO.toString(), false);
                        setRowVisible(Processor.MIXED_GEO.toString(), false);
                        setRowVisible(Processor.CALIBRATE_VIIRS.toString(), false);
                        setRowVisible(Processor.MODIS_L1B.toString(), false);
                        setRowVisible(Processor.MIXED_L1B.toString(), false);
                        setRowVisible(Processor.L1BGEN.toString(), true);
                    }
                    setRowVisible(Processor.EXTRACTOR.toString(), false);
                    setRowVisible(Processor.L1BRSGEN.toString(), false);
                }
            } else { //file is a single file
                if (missionName.contains("VIIRS")) {
                    setRowVisible(Processor.MODIS_L1A.toString(), false);
                    setRowVisible(Processor.GEOLOCATE_VIIRS.toString(), true);
                    setRowVisible(Processor.GEOLOCATE_HAWKEYE.toString(), false);
                    setRowVisible(Processor.MODIS_GEO.toString(), false);
                    setRowVisible(Processor.MIXED_GEO.toString(), false);
                    setRowVisible(Processor.EXTRACTOR.toString(), true);
                    setRowVisible(Processor.CALIBRATE_VIIRS.toString(), true);
                    setRowVisible(Processor.MODIS_L1B.toString(), false);
                    setRowVisible(Processor.MIXED_L1B.toString(), false);
                    setRowVisible(Processor.L1BGEN.toString(), false);
                    setRowVisible(Processor.L1BRSGEN.toString(), true);
                } else if (missionName.contains("MODIS")) {
                    setRowVisible(Processor.MODIS_L1A.toString(), true);
                    setRowVisible(Processor.GEOLOCATE_VIIRS.toString(), false);
                    setRowVisible(Processor.GEOLOCATE_HAWKEYE.toString(), false);
                    setRowVisible(Processor.MODIS_GEO.toString(), true);
                    setRowVisible(Processor.MIXED_GEO.toString(), false);
                    setRowVisible(Processor.EXTRACTOR.toString(), true);
                    setRowVisible(Processor.CALIBRATE_VIIRS.toString(), false);
                    setRowVisible(Processor.MODIS_L1B.toString(), true);
                    setRowVisible(Processor.MIXED_L1B.toString(), false);
                    setRowVisible(Processor.L1BGEN.toString(), false);
                    setRowVisible(Processor.L1BRSGEN.toString(), true);
                } else if (missionName.contains("HAWKEYE")) {
                    setRowVisible(Processor.MODIS_L1A.toString(), false);
                    setRowVisible(Processor.GEOLOCATE_VIIRS.toString(), false);
                    setRowVisible(Processor.GEOLOCATE_HAWKEYE.toString(), true);
                    setRowVisible(Processor.MODIS_GEO.toString(), false);
                    setRowVisible(Processor.MIXED_GEO.toString(), false);
                    setRowVisible(Processor.EXTRACTOR.toString(), false);
                    setRowVisible(Processor.CALIBRATE_VIIRS.toString(), false);
                    setRowVisible(Processor.MODIS_L1B.toString(), false);
                    setRowVisible(Processor.MIXED_L1B.toString(), false);
                    setRowVisible(Processor.L1BGEN.toString(), true);
                    setRowVisible(Processor.L1BRSGEN.toString(), true);
                } else if (missionName.contains("SEAWIFS")) {
                    setRowVisible(Processor.MODIS_L1A.toString(), false);
                    setRowVisible(Processor.GEOLOCATE_VIIRS.toString(), false);
                    setRowVisible(Processor.GEOLOCATE_HAWKEYE.toString(), false);
                    setRowVisible(Processor.MODIS_GEO.toString(), false);
                    setRowVisible(Processor.MIXED_GEO.toString(), false);
                    setRowVisible(Processor.EXTRACTOR.toString(), true);
                    setRowVisible(Processor.CALIBRATE_VIIRS.toString(), false);
                    setRowVisible(Processor.MODIS_L1B.toString(), false);
                    setRowVisible(Processor.MIXED_L1B.toString(), false);
                    setRowVisible(Processor.L1BGEN.toString(), true);
                    setRowVisible(Processor.L1BRSGEN.toString(), true);
                } else {
                    setRowVisible(Processor.MODIS_L1A.toString(), false);
                    setRowVisible(Processor.GEOLOCATE_VIIRS.toString(), false);
                    setRowVisible(Processor.GEOLOCATE_HAWKEYE.toString(), false);
                    setRowVisible(Processor.MODIS_GEO.toString(), false);
                    setRowVisible(Processor.MIXED_GEO.toString(), false);
                    setRowVisible(Processor.EXTRACTOR.toString(), false);
                    setRowVisible(Processor.CALIBRATE_VIIRS.toString(), false);
                    setRowVisible(Processor.MODIS_L1B.toString(), false);
                    setRowVisible(Processor.MIXED_L1B.toString(), false);
                    setRowVisible(Processor.L1BGEN.toString(), true);
                    setRowVisible(Processor.L1BRSGEN.toString(), true);
                }
            }
        }
    }

    private void setRowVisible(String rowName, Boolean visible) {
        if (displayDisabledProcessors) {
            if (rowName.contains("mixed_")) {
                getRow(rowName).getConfigButton().setVisible(visible);
                getRow(rowName).getPlusCheckBox().setVisible(visible);
                getRow(rowName).getParamTextField().setVisible(visible);
                getRow(rowName).getOdirSelector().setVisible(visible);
            } else {
                getRow(rowName).getConfigButton().setEnabled(visible);
                getRow(rowName).getPlusCheckBox().setEnabled(visible);
                getRow(rowName).getParamTextField().setVisible(visible);
                getRow(rowName).getOdirSelector().setVisible(visible);
            }
        }
    }
}