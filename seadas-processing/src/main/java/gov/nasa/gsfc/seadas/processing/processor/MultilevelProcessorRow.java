package gov.nasa.gsfc.seadas.processing.processor;

import gov.nasa.gsfc.seadas.processing.common.CloProgramUI;
import gov.nasa.gsfc.seadas.processing.common.GridBagConstraintsCustom;
import gov.nasa.gsfc.seadas.processing.common.ProgramUIFactory;
import gov.nasa.gsfc.seadas.processing.common.SeadasLogger;
import gov.nasa.gsfc.seadas.processing.core.L2genData;
import gov.nasa.gsfc.seadas.processing.core.ParamInfo;
import gov.nasa.gsfc.seadas.processing.core.ParamList;
import gov.nasa.gsfc.seadas.processing.l2gen.userInterface.L2genForm;
import gov.nasa.gsfc.seadas.processing.ocssw.OCSSW;
import org.esa.snap.ui.ModalDialog;
import org.esa.snap.ui.UIUtils;

import javax.swing.*;
import javax.swing.event.SwingPropertyChangeSupport;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Created with IntelliJ IDEA.
 * User: dshea
 * Date: 8/21/12
 * Time: 8:21 AM
 * To change this template use File | Settings | File Templates.
 */
public class MultilevelProcessorRow {
    public static final String PARAM_STRING_EVENT = "paramString";
//    public static final String KEEPFILES_PARAM = "keepfiles";
    public static final String PLUS_PARAM = "plusToChain";
    public static final String ODIR_PARAM = "odir";

    private static final String LONGEST_BUTTON_LABEL = "multilevel_processor";

    private String name;
    private CloProgramUI cloProgramUI;
    private MultlevelProcessorForm parentForm;

    private JButton configButton;
//    private JCheckBox keepCheckBox;
    private JCheckBox plusCheckBox;
    private JTextField paramTextField;
    private ActiveFileSelector odirSelector;
    private JPanel configPanel;
    private ParamList paramList;
    private SwingPropertyChangeSupport propertyChangeSupport;

    private boolean checkboxControlHandlerEnabled = true;

    OCSSW ocssw;


    public MultilevelProcessorRow(String name, MultlevelProcessorForm parentForm, OCSSW ocssw) {
        this.name = name;
        this.parentForm = parentForm;
        this.ocssw = ocssw;

        propertyChangeSupport = new SwingPropertyChangeSupport(this);
        paramList = new ParamList();
        configButton = new JButton(LONGEST_BUTTON_LABEL);
        configButton.setPreferredSize(configButton.getPreferredSize());
        configButton.setText(name);
        configButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleButtonEvent();
            }
        });
        plusCheckBox = new JCheckBox();
        plusCheckBox.setSelected(false);
        plusCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (checkboxControlHandlerEnabled) {
                    setCheckboxControlHandlerEnabled(false);
                    handleplusCheckBox();
                    setCheckboxControlHandlerEnabled(true);
                }
            }
        });
        paramTextField = new JTextField();
        paramTextField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleParamTextField();
            }
        });
        paramTextField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {

            }

            @Override
            public void focusLost(FocusEvent e) {
                handleParamTextField();
            }
        });

        odirSelector = new ActiveFileSelector(propertyChangeSupport, "ODIR", "", ParamInfo.Type.DIR);

        odirSelector.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                handleOdirChanged();
            }
        });
// todo made this change to fix problem where l2gen unchecks the box after user checks the box
        //      essentially now all config user at startup
//        if (name.equals(MultlevelProcessorForm.Processor.MAIN.toString()) || name.equals("l2gen")) {
//            createConfigPanel();plus
//        }
        if (name.equals(MultlevelProcessorForm.Processor.MAIN.toString())) {
            createConfigPanel();
        }


    }

    public String getName() {
        return name;
    }

    // this method assumes the the JPanel passed in is using a grid bag layout
    public void attachComponents(JPanel base, int row) {
        plusCheckBox.setToolTipText(configButton.getText() + " processor add to the chain");
        configButton.setToolTipText("Open " + configButton.getText() + " GUI to set params");
        base.add(configButton,
                new GridBagConstraintsCustom(0, row, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 2, 2, 0)));

        base.add(plusCheckBox,
                new GridBagConstraintsCustom(1, row, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(2, 0, 2, 0)));


        base.add(paramTextField,
                new GridBagConstraintsCustom(2, row, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 0, 2, 2)));

        if (!name.equals("main")) {
            base.add(odirSelector.getJPanel(),
                    new GridBagConstraintsCustom(3, row, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 0, 2, 2)));
        }
    }

    private void createConfigPanel() {
        createConfigPanel(false);
    }

    private void createConfigPanel(boolean plusfiles) {
        if (configPanel == null) {
            if (name.equals(MultlevelProcessorForm.Processor.MAIN.toString())) {
                cloProgramUI = new ProgramUIFactory("multilevel_processor", "multilevel_processor.xml", ocssw);
                //  configPanel = (JPanel) cloProgramUI;
                configPanel = cloProgramUI.getParamPanel();
            } else if (name.equals("geo")) {
                cloProgramUI = new ProgramUIFactory("modis_GEO", "modis_GEO.xml", ocssw);
                configPanel = cloProgramUI.getParamPanel();
            } else if (name.equals("l2gen")) {
            //    cloProgramUI = new L2genForm(parentForm.getAppContext(), "l2gen.xml", getTinyIFile(), false, L2genData.Mode.L2GEN, true, true);
                cloProgramUI = new L2genForm(parentForm.getAppContext(), "l2gen.xml", null  , false, L2genData.Mode.L2GEN, true, true, ocssw);
                configPanel = cloProgramUI.getParamPanel();

            } else if (name.equals("l2extract")) {
                cloProgramUI = new ProgramUIFactory("l2extract", "l1aextract.xml", ocssw);
                configPanel = cloProgramUI.getParamPanel();
            } else {
                String xmlFile = name.replace("", "").concat(".xml");
                cloProgramUI = new ProgramUIFactory(name, xmlFile, ocssw);
                configPanel = cloProgramUI.getParamPanel();
            }

            // set parameters to default values
            getParamListFromCloProgramUI();
            if (plusfiles) {
                paramList.setParamString(PLUS_PARAM + "=" + plusfiles);
                paramList.setParamString(ODIR_PARAM + "=" + "");
            } else {
                paramList.setParamString("");
            }
            paramList.setParamString("");
        }
    }


    public void clearConfigPanel() {
        cloProgramUI = null;
        configPanel = null;
        paramTextField.setText("");
        paramList = new ParamList();
    }

    private void getParamListFromCloProgramUI() {
        paramList = (ParamList) cloProgramUI.getProcessorModel().getParamList().clone();
        cleanIOParams(paramList);
        if (plusCheckBox.isSelected()) {
            paramList.addInfo(new ParamInfo(PLUS_PARAM, ParamInfo.BOOLEAN_TRUE, ParamInfo.Type.BOOLEAN, ParamInfo.BOOLEAN_FALSE));
            if (paramList.getInfo(ODIR_PARAM) == null) {
                paramList.addInfo(new ParamInfo(ODIR_PARAM, ""));
            }
        } else {
            paramList.addInfo(new ParamInfo(PLUS_PARAM, ParamInfo.BOOLEAN_FALSE, ParamInfo.Type.BOOLEAN, ParamInfo.BOOLEAN_FALSE));
        }
    }

    private void handleplusCheckBox() {
        boolean plusSelected = plusCheckBox.isSelected();
        boolean test2 = paramList.isValueTrue(PLUS_PARAM);
        createConfigPanel(plusCheckBox.isSelected());

        if (plusCheckBox.isSelected() != plusSelected) {
            plusCheckBox.setSelected(plusSelected);
        }
//        test1 = keepCheckBox.isSelected();
//        test2 = paramList.isValueTrue(KEEPFILES_PARAM);

        if (plusCheckBox.isSelected() != paramList.isValueTrue(PLUS_PARAM)) {
            String oldParamString = getParamString();
            if (plusCheckBox.isSelected()) {
                paramList.setValue(PLUS_PARAM, ParamInfo.BOOLEAN_TRUE);
                paramList.setValue(ODIR_PARAM, "");
            } else {
                paramList.setValue(PLUS_PARAM, ParamInfo.BOOLEAN_FALSE);
            }
            String str = getParamString();
            propertyChangeSupport.firePropertyChange(PARAM_STRING_EVENT, oldParamString, str);
        }
    }

    private void handleOdirChanged() {
        if (paramList.getParamArray().isEmpty()) {
            createConfigPanel(true);
            paramList.setValue(PLUS_PARAM, ParamInfo.BOOLEAN_TRUE);
            if (paramList.getInfo(ODIR_PARAM) == null) {
                paramList.addInfo(new ParamInfo(ODIR_PARAM, ""));
            }
        }
        String oldParamString = paramList.getParamString();
        String odirName = odirSelector.getFilename();
        String oldOdir = paramList.getValue("odir");
        if (odirName != null && !odirName.equals(oldOdir)) {
            paramList.setValue("odir", odirName);
        }
        updateplusCheckbox();
        String str = paramList.getParamString();
        propertyChangeSupport.firePropertyChange(PARAM_STRING_EVENT, oldParamString, str);
    }

    private void handleParamTextField() {
        createConfigPanel();
        String oldParamString = getParamString();
        String str = paramTextField.getText();
        ParamInfo param = paramList.getInfo(PLUS_PARAM);
        if (param != null) {
            str = str + " " + param.getParamString();
        }
        paramList.setParamString(str);
        str = getParamString();
        updateParamTextField();
        updateplusCheckbox();
        if (!oldParamString.equals(str)) {
            propertyChangeSupport.firePropertyChange(PARAM_STRING_EVENT, oldParamString, str);
        }
    }

    private void handleButtonEvent() {
        createConfigPanel();

        final Window parent = parentForm.getAppContext().getApplicationWindow();
        final ModalDialog modalDialog = new ModalDialog(parent, name, configPanel, ModalDialog.ID_OK_CANCEL_HELP, name);

        // modalDialog.getButton(ModalDialog.ID_OK).setEnabled(true);
        modalDialog.getButton(ModalDialog.ID_OK).setText("Save");
        modalDialog.getButton(ModalDialog.ID_HELP).setText("");
        modalDialog.getButton(ModalDialog.ID_HELP).setIcon(UIUtils.loadImageIcon("icons/Help24.gif"));

        //Make sure program is only executed when the "run" button is clicked.
        ((JButton) modalDialog.getButton(ModalDialog.ID_OK)).setDefaultCapable(false);
        modalDialog.getJDialog().getRootPane().setDefaultButton(null);

        // load the UI with the current param values
        ParamList list = (ParamList) paramList.clone();
        list.removeInfo(PLUS_PARAM);
        cloProgramUI.setParamString(list.getParamString("\n"));
        String oldUIParamString = cloProgramUI.getParamString();

        final int dialogResult = modalDialog.show();

        SeadasLogger.getLogger().info("dialog result: " + dialogResult);

        if (dialogResult != ModalDialog.ID_OK) {
            return;
        }

        String str = cloProgramUI.getParamString();
        if (!oldUIParamString.equals(str)) {
            getParamListFromCloProgramUI();
            updateParamList();
            propertyChangeSupport.firePropertyChange(PARAM_STRING_EVENT, oldUIParamString, str);
        }

    }

    private void updateplusCheckbox() {
        Boolean check = paramList.isValueTrue(PLUS_PARAM) || !paramList.isDefault();
        plusCheckBox.setSelected(check);
    }

    private void updateOdir() {
        ParamList list = (ParamList) paramList.clone();
        list.removeInfo(PLUS_PARAM);
        String str = paramList.getInfo(ODIR_PARAM).getValue();
        odirSelector.getFileSelector().setFilename(str);
    }

    private void updateParamTextField() {
        ParamList list = (ParamList) paramList.clone();
        list.removeInfo(PLUS_PARAM);
        if (list.getInfo("use_existing") == null) {
            list.removeInfo(ODIR_PARAM);
        }
        paramTextField.setText(list.getParamString(" "));
    }

    private void updateParamList() {
        updateOdir();
        updateParamTextField();
        updateplusCheckbox();
    }

    public ParamList getParamList() {
        return paramList;
    }

    public String getParamString(String separator) {
        return paramList.getParamString(separator);
    }

    public String getParamString() {
        return paramList.getParamString();
    }

    public void setParamString(String str, boolean retainIFile) {
        createConfigPanel();
        String oldParamString = getParamString();
        paramList.setParamString(str, retainIFile, true);
        str = getParamString();
        if (!oldParamString.equals(str)) {
            updateParamList();
            propertyChangeSupport.firePropertyChange(PARAM_STRING_EVENT, oldParamString, str);
        }
    }

    public void setParamValue(String name, String str) {
        String oldParamString = getParamString();
        paramList.setValue(name, str);
        str = getParamString();
        if (!oldParamString.equals(str)) {
            updateParamList();
            propertyChangeSupport.firePropertyChange(PARAM_STRING_EVENT, oldParamString, str);
        }
    }


    private void cleanIOParams(ParamList list) {
        if (name.equals(MultlevelProcessorForm.Processor.MAIN.toString())) {
            return;
        }

        String[] IOParams = {"ifile", "ofile", "infile", "geofile"};

        for (String IOParam : IOParams) {
            ParamInfo param = list.getInfo(IOParam);
            if (param != null) {
                list.setValue(param.getName(), param.getDefaultValue());
            }
        }
    }

    public void addPropertyChangeListener(String name, PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(name, listener);
    }

    public void removePropertyChangeListener(String name, PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(name, listener);
    }

//    private File getTinyIFile() {
//        String ifileName = parentForm.getFirstIFile();
//        FileInfo fileInfo = null;
//        String missionName = (new MissionInfo(MissionInfo.Id.SEAWIFS)).getName();
//
//        if (ifileName != null) {
//            fileInfo = new FileInfo(ifileName);
//            MissionInfo.Id missionId = fileInfo.getMissionId();
//            if (missionId == MissionInfo.Id.SEAWIFS ||
//                    missionId == MissionInfo.Id.MODISA ||
//                    missionId == MissionInfo.Id.MODIST ||
//                    missionId == MissionInfo.Id.MERIS ||
//                    missionId == MissionInfo.Id.CZCS ||
//                    missionId == MissionInfo.Id.OCTS) {
//                missionName = fileInfo.getMissionName();
//            }
//        }
//
//        missionName = missionName.replace(' ', '_');
//        String tinyFileName = "tiny_" + missionName;
//
//        if (fileInfo != null && fileInfo.isGeofileRequired()) {
//            L2genData.installResource(tinyFileName + ".GEO");
//        }
//
//        return L2genData.installResource(tinyFileName);
//    }

    public boolean isCheckboxControlHandlerEnabled() {
        return checkboxControlHandlerEnabled;
    }

    public void setCheckboxControlHandlerEnabled(boolean checkboxControlHandlerEnabled) {
        this.checkboxControlHandlerEnabled = checkboxControlHandlerEnabled;
    }
}
