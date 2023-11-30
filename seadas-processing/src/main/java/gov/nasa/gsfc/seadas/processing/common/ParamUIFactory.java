package gov.nasa.gsfc.seadas.processing.common;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import gov.nasa.gsfc.seadas.processing.core.*;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.ui.GridBagUtils;
import org.esa.snap.ui.ModalDialog;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.event.SwingPropertyChangeSupport;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: Aynur Abdurazik (aabduraz)
 * Date: 6/7/12
 * Time: 9:23 AM
 * To change this template use File | Settings | File Templates.
 */

public class ParamUIFactory {

    ProcessorModel processorModel;
    SwingPropertyChangeSupport propertyChangeSupport = new SwingPropertyChangeSupport(this);
    private String emptySpace = "  ";


    private static int controlHandlerIntEnabled = 1;  // enabled if value 1 or greater
    private static int eventHandlerIntEnabled = 1;  // enabled if value 1 or greater

    public ParamUIFactory(ProcessorModel pm) {
        this.processorModel = pm;
    }

    public JPanel createParamPanel() {
        //final JScrollPane textScrollPane = new JScrollPane(parameterTextArea);
        final JScrollPane textScrollPane = new JScrollPane(createParamPanel(processorModel));

//        textScrollPane.setPreferredSize(new Dimension(700, 400));

        final JPanel parameterComponent = new JPanel(new BorderLayout());

        parameterComponent.add(textScrollPane, BorderLayout.NORTH);


        parameterComponent.setPreferredSize(parameterComponent.getPreferredSize());

        if (processorModel.getProgramName().indexOf("smigen") != -1) {
            SMItoPPMUI smItoPPMUI = new SMItoPPMUI(processorModel);
            JPanel smitoppmPanel = smItoPPMUI.getSMItoPPMPanel();
            parameterComponent.add(smitoppmPanel, BorderLayout.SOUTH);
            smitoppmPanel.addPropertyChangeListener(new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                    parameterComponent.validate();
                    parameterComponent.repaint();
                }
            });
        }
        parameterComponent.setMaximumSize(parameterComponent.getPreferredSize());
        parameterComponent.setMinimumSize(parameterComponent.getPreferredSize());
        return parameterComponent;
    }

    protected JPanel createParamPanel(ProcessorModel processorModel) {
        ArrayList<ParamInfo> paramList = processorModel.getProgramParamList();
        JPanel paramPanel = new JPanel();
        paramPanel.setName("param panel");
//        JPanel textFieldPanel = new JPanel();
        final JPanel textFieldPanel = GridBagUtils.createPanel();
        GridBagConstraints gbc = GridBagUtils.createConstraints();
        textFieldPanel.setName("text field panel");
        JPanel booleanParamPanel = new JPanel();
        booleanParamPanel.setName("boolean field panel");
        JPanel fileParamPanel = new JPanel();
        fileParamPanel.setName("file parameter panel");
        JPanel buttonPanel = new JPanel();
        buttonPanel.setName("button panel");

        TableLayout booleanParamLayout = new TableLayout(3);
        booleanParamPanel.setLayout(booleanParamLayout);

        TableLayout fileParamLayout = new TableLayout(1);
        fileParamLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        fileParamPanel.setLayout(fileParamLayout);

        int numberOfOptionsPerLine = paramList.size() % 4 < paramList.size() % 5 ? 4 : 5;
        if ("l3mapgen".equals(processorModel.getProgramName())) {
            numberOfOptionsPerLine = 6;
        }
//        TableLayout textFieldPanelLayout = new TableLayout(numberOfOptionsPerLine);
//        textFieldPanelLayout.setTablePadding(5,5);
//        textFieldPanel.setLayout(textFieldPanelLayout);

        gbc.gridy=0;
        gbc.gridx=0;
        gbc.insets.top = 5;
        gbc.insets.left = 5;
        gbc.insets.right = 25;
        gbc.weighty = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;




        Iterator<ParamInfo> itr = paramList.iterator();
        while (itr.hasNext()) {
            final ParamInfo pi = itr.next();
            if (!(pi.getName().equals(processorModel.getPrimaryInputFileOptionName()) ||
                    pi.getName().equals(processorModel.getPrimaryOutputFileOptionName()) ||
                    pi.getName().equals(L2genData.GEOFILE) ||
                    pi.getName().equals("verbose") ||
                    pi.getName().equals("--verbose"))) {
                final String optionName = ParamUtils.removePreceedingDashes(pi.getName());

                if (("l2bin".equals(processorModel.getProgramName()) && "l3bprod".equals(optionName)) ||
                        ("l3mapgen".equals(processorModel.getProgramName()) && "product".equals(optionName)) ||
                        ("l3mapgen".equals(processorModel.getProgramName()) && "projection".equals(optionName))) {
                    gbc.gridwidth = 2;
                }

                if (pi.hasValidValueInfos() && pi.getType() != ParamInfo.Type.FLAGS) {
                        textFieldPanel.add(makeComboBoxOptionPanel(pi), gbc);
                        gbc = incrementGridxGridy(gbc, numberOfOptionsPerLine);
                } else {
                    switch (pi.getType()) {
                        case BOOLEAN:
                            booleanParamPanel.add(makeBooleanOptionField(pi));
                            break;
                        case IFILE:
                            fileParamPanel.add(createIOFileOptionField(pi));
                            break;
                        case OFILE:
                            fileParamPanel.add(createIOFileOptionField(pi));
                            break;
                        case DIR:
                            fileParamPanel.add(createIOFileOptionField(pi));
                            break;
                        case STRING:
                            textFieldPanel.add(makeOptionField(pi), gbc);
                            gbc = incrementGridxGridy(gbc, numberOfOptionsPerLine);

//                            if (("l2bin".equals(processorModel.getProgramName()) && "l3bprod".equals(optionName)) ||
//                                    ("l3mapgen".equals(processorModel.getProgramName()) && "product".equals(optionName)) ||
//                                    ("l3mapgen".equals(processorModel.getProgramName()) && "projection".equals(optionName))) {
//                                gbc.gridwidth=2;
//                                textFieldPanel.add(makeOptionField(pi), gbc);
//                                gbc = incrementGridxGridy(gbc, numberOfOptionsPerLine);
//                                gbc.gridwidth=1;
//                            } else {
//                                textFieldPanel.add(makeOptionField(pi), gbc);
//                                gbc = incrementGridxGridy(gbc, numberOfOptionsPerLine);
//                            }

                            break;
                        case INT:
                            textFieldPanel.add(makeOptionField(pi), gbc);
                            gbc = incrementGridxGridy(gbc, numberOfOptionsPerLine);
                            break;
                        case FLOAT:
                            textFieldPanel.add(makeOptionField(pi), gbc);
                            gbc = incrementGridxGridy(gbc, numberOfOptionsPerLine);
                            break;
                        case FLAGS:
                            gbc.gridwidth=5;
                            gbc.insets.top = 10;
                            gbc.insets.bottom = 10;
                            textFieldPanel.add(makeButtonOptionPanel(pi), gbc);
                            gbc = incrementGridxGridy(gbc, numberOfOptionsPerLine);
                            gbc.gridwidth=1;
                            gbc.insets.top = 5;
                            gbc.insets.bottom = 5;
                            break;
                        case BUTTON:
                            buttonPanel.add(makeActionButtonPanel(pi));
                            break;
                    }


                    //paramPanel.add(makeOptionField(pi));
                }

                gbc.gridwidth = 1;

            }
        }

        TableLayout paramLayout = new TableLayout(1);

        paramPanel.setLayout(paramLayout);
        paramPanel.add(fileParamPanel);
        paramPanel.add(textFieldPanel);
        paramPanel.add(booleanParamPanel);
        paramPanel.add(buttonPanel);

        return paramPanel;
    }


    GridBagConstraints incrementGridxGridy(GridBagConstraints gbc, int numColumns) {
        gbc.gridx += gbc.gridwidth;
        if (gbc.gridx > (numColumns - 1)) {
            gbc.gridy += 1;
            gbc.gridx = 0;
        }

        return gbc;
    }

    protected JPanel makeOptionField(final ParamInfo pi) {

        final String optionName = ParamUtils.removePreceedingDashes(pi.getName());
        final JPanel optionPanel = new JPanel();
        optionPanel.setName(optionName);
        TableLayout fieldLayout = new TableLayout(1);
        fieldLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        optionPanel.setLayout(fieldLayout);
        optionPanel.setName(optionName);
        optionPanel.add(new JLabel(ParamUtils.removePreceedingDashes(optionName)));
        if (pi.getDescription() != null) {
            optionPanel.setToolTipText(pi.getDescription().replaceAll("\\s+", " "));
        }


        if (pi.getValue() == null || pi.getValue().length() == 0) {
            if (pi.getDefaultValue() != null) {
                processorModel.updateParamInfo(pi, pi.getDefaultValue());
            }
        }

        final PropertyContainer vc = new PropertyContainer();
        vc.addProperty(Property.create(optionName, pi.getValue()));
        vc.getDescriptor(optionName).setDisplayName(optionName);
        final BindingContext ctx = new BindingContext(vc);
        final JTextField field = new JTextField();
//        field.setColumns(optionName.length() > 12 ? 12 : 8);
        field.setColumns(8);
        if ("l3bprod".equals(optionName) || "product".equals(optionName)) {
            field.setColumns(20);
        }
        field.setPreferredSize(field.getPreferredSize());
        field.setMaximumSize(field.getPreferredSize());
        field.setMinimumSize(field.getPreferredSize());
        field.setName(pi.getName());

        if (pi.getDescription() != null) {
            field.setToolTipText(pi.getDescription().replaceAll("\\s+", " "));
        }
        ctx.bind(optionName, field);

        ctx.addPropertyChangeListener(optionName, new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent pce) {
                if (!field.getText().trim().equals(pi.getValue().trim()))
                    processorModel.updateParamInfo(pi, field.getText());
            }
        });
        processorModel.addPropertyChangeListener(pi.getName(), new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                if (!field.getText().trim().equals(pi.getValue().trim()))
                    field.setText(pi.getValue());
            }
        });
        optionPanel.add(field);

        return optionPanel;
    }

    private JPanel makeBooleanOptionField(final ParamInfo pi) {

        final String optionName = pi.getName();
        final boolean optionValue = pi.getValue().equals("true") || pi.getValue().equals("1") ? true : false;

        final JPanel optionPanel = new JPanel();
        optionPanel.setName(optionName);
        TableLayout booleanLayout = new TableLayout(1);
        //booleanLayout.setTableFill(TableLayout.Fill.HORIZONTAL);

        optionPanel.setLayout(booleanLayout);
        optionPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        optionPanel.setAlignmentY(Component.CENTER_ALIGNMENT);
        optionPanel.add(new JLabel(emptySpace + ParamUtils.removePreceedingDashes(optionName) + emptySpace));
        if (pi.getDescription() != null) {
            optionPanel.setToolTipText(pi.getDescription().replaceAll("\\s+", " "));
        }


        final PropertySet vc = new PropertyContainer();
        vc.addProperty(Property.create(optionName, optionValue));
        vc.getDescriptor(optionName).setDisplayName(optionName);

        final BindingContext ctx = new BindingContext(vc);
        final JCheckBox field = new JCheckBox();
        field.setHorizontalAlignment(JFormattedTextField.LEFT);
        field.setName(pi.getName());
        ctx.bind(optionName, field);
        if (pi.getDescription() != null) {
            field.setToolTipText(pi.getDescription().replaceAll("\\s+", " "));
        }

        ctx.addPropertyChangeListener(optionName, new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent pce) {
                processorModel.updateParamInfo(pi, (new Boolean(field.isSelected())).toString());
                SeadasFileUtils.debug(((new Boolean(field.isSelected())).toString() + "  " + field.getText()));

            }
        });

        processorModel.addPropertyChangeListener(pi.getName(), new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                field.setSelected(pi.getValue().equals("true") || pi.getValue().equals("1") ? true : false);
                field.validate();
                field.repaint();
            }
        });

        optionPanel.add(field);

        return optionPanel;

    }

    private JPanel makeComboBoxOptionPanel(final ParamInfo pi) {
        final JPanel singlePanel = new JPanel();

        TableLayout comboParamLayout = new TableLayout(1);
        comboParamLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        singlePanel.setLayout(comboParamLayout);

        final JLabel optionNameLabel = new JLabel(ParamUtils.removePreceedingDashes(pi.getName()));

        singlePanel.add(optionNameLabel);
        singlePanel.setName(pi.getName());
        if (pi.getDescription() != null) {
            singlePanel.setToolTipText(pi.getDescription().replaceAll("\\s+", " "));
        }

        String optionDefaultValue = pi.getValue();


        final ArrayList<ParamValidValueInfo> validValues = pi.getValidValueInfos();
        final String[] values = new String[validValues.size()];
        ArrayList<String> toolTips = new ArrayList<String>();

        Iterator<ParamValidValueInfo> itr = validValues.iterator();
        int i = 0;
        ParamValidValueInfo paramValidValueInfo;
        while (itr.hasNext()) {
            paramValidValueInfo = itr.next();
            values[i] = paramValidValueInfo.getValue();
            if (paramValidValueInfo.getDescription() != null) {
                toolTips.add(paramValidValueInfo.getDescription().replaceAll("\\s+", " "));
            }
            i++;
        }

        final JComboBox<String> inputList = new JComboBox<String>(values);
        ComboboxToolTipRenderer renderer = new ComboboxToolTipRenderer();
        inputList.setRenderer(renderer);
        renderer.setTooltips(toolTips);
        inputList.setEditable(true);
        inputList.setName(pi.getName());
        inputList.setPreferredSize(new Dimension(inputList.getPreferredSize().width,
                inputList.getPreferredSize().height));
        if (pi.getDescription() != null) {
            inputList.setToolTipText(pi.getDescription().replaceAll("\\s+", " "));
        }
        int defaultValuePosition = new ArrayList<String>(Arrays.asList(values)).indexOf(optionDefaultValue);

        if (defaultValuePosition != -1) {
            inputList.setSelectedIndex(defaultValuePosition);
        }

        String optionName = pi.getName();


        final PropertyContainer vc = new PropertyContainer();
        vc.addProperty(Property.create(optionName, pi.getValue()));
        vc.getDescriptor(optionName).setDisplayName(optionName);

        final BindingContext ctx = new BindingContext(vc);

        ctx.bind(optionName, inputList);

        ctx.addPropertyChangeListener(optionName, new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent pce) {

                String newValue = (String) inputList.getSelectedItem();
                processorModel.updateParamInfo(pi, newValue);
            }
        });

        processorModel.addPropertyChangeListener(pi.getName(), new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                //values = updateValidValues(pi);
                int currentChoicePosition = new ArrayList<String>(Arrays.asList(values)).indexOf(pi.getValue());
                if (currentChoicePosition != -1) {
                    inputList.setSelectedIndex(currentChoicePosition);
                }
            }
        });
        singlePanel.add(inputList);
        return singlePanel;
    }

    private JPanel makeButtonOptionPanel(final ParamInfo pi) {
        final JPanel singlePanel = new JPanel();

        TableLayout comboParamLayout = new TableLayout(8);
        comboParamLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        singlePanel.setLayout(comboParamLayout);

        final JButton optionNameButton = new JButton(ParamUtils.removePreceedingDashes(pi.getName()));
        optionNameButton.setName("optionButton");
        optionNameButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                String selectedFlags = chooseValidValues(pi);
                processorModel.updateParamInfo(pi, selectedFlags);
            }
        });
        singlePanel.add(optionNameButton);
        singlePanel.setName(pi.getName());
        if (pi.getDescription() != null) {
            singlePanel.setToolTipText(pi.getDescription().replaceAll("\\s+", " "));
        }

        final JTextField field = new JTextField();
        field.setText(pi.getValue());
        field.setColumns(48);
        if (pi.getDescription() != null) {
            field.setToolTipText(pi.getDescription().replaceAll("\\s+", " "));
        }
        processorModel.addPropertyChangeListener(pi.getName(), new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                field.setText(pi.getValue());
            }
        });
        singlePanel.add(field);
        return singlePanel;
    }

    private JPanel makeActionButtonPanel(final ParamInfo pi) {
        final JPanel singlePanel = new JPanel();

        TableLayout comboParamLayout = new TableLayout(8);
        comboParamLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        singlePanel.setLayout(comboParamLayout);

        final JButton actionButton = new JButton(ParamUtils.removePreceedingDashes(pi.getName()));
        actionButton.setName("actionButton");
        actionButton.setEnabled(false);
        if (pi.getDescription() != null) {
            actionButton.setToolTipText(pi.getDescription().replaceAll("\\s+", " "));
        }
        singlePanel.add(actionButton);
        return singlePanel;
    }

    public void addPropertyChangeListener(String name, PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(name, listener);
    }

    public void removePropertyChangeListener(String name, PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(name, listener);
    }

    public SwingPropertyChangeSupport getPropertyChangeSupport() {
        return propertyChangeSupport;
    }

    public void appendPropertyChangeSupport(SwingPropertyChangeSupport propertyChangeSupport) {
        PropertyChangeListener[] pr = propertyChangeSupport.getPropertyChangeListeners();
        for (int i = 0; i < pr.length; i++) {
            this.propertyChangeSupport.addPropertyChangeListener(pr[i]);
        }
    }

    public void clearPropertyChangeSupport() {
        propertyChangeSupport = new SwingPropertyChangeSupport(this);
    }

    private String chooseValidValues(ParamInfo pi) {
        JPanel validValuesPanel = new JPanel();
        validValuesPanel.setLayout(new TableLayout(3));
        String choosenValues = "";
        final ArrayList<ParamValidValueInfo> validValues = pi.getValidValueInfos();

        Iterator<ParamValidValueInfo> itr = validValues.iterator();
        ParamValidValueInfo paramValidValueInfo;
        while (itr.hasNext()) {
            paramValidValueInfo = itr.next();
            if (!paramValidValueInfo.getValue().trim().equals("SPARE")) {
                validValuesPanel.add(makeValidValueCheckbox(paramValidValueInfo));
            }
        }
        validValuesPanel.repaint();
        validValuesPanel.validate();

        final Window parent = SnapApp.getDefault().getMainFrame();
        String dialogTitle = null;
        final ModalDialog modalDialog = new ModalDialog(parent, dialogTitle, validValuesPanel, ModalDialog.ID_OK, "test");
        final int dialogResult = modalDialog.show();
        if (dialogResult != ModalDialog.ID_OK) {

        }

        itr = validValues.iterator();

        while (itr.hasNext()) {
            paramValidValueInfo = itr.next();
            if (paramValidValueInfo.isSelected()) {
                choosenValues = choosenValues + paramValidValueInfo.getValue() + ",";
            }
        }
        if (choosenValues.indexOf(",") != -1) {
            choosenValues = choosenValues.substring(0, choosenValues.lastIndexOf(","));
        }
        return choosenValues;
    }

    private JPanel makeValidValueCheckbox(final ParamValidValueInfo paramValidValueInfo) {

        final String optionName = paramValidValueInfo.getValue();
        final boolean optionValue = paramValidValueInfo.isSelected();

        final JPanel optionPanel = new JPanel();
        optionPanel.setName(optionName);
        optionPanel.setBorder(new EtchedBorder());
        optionPanel.setPreferredSize(new Dimension(100, 40));
        TableLayout booleanLayout = new TableLayout(1);
        //booleanLayout.setTableFill(TableLayout.Fill.HORIZONTAL);

        optionPanel.setLayout(booleanLayout);
        optionPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        optionPanel.setAlignmentY(Component.CENTER_ALIGNMENT);
        optionPanel.add(new JLabel(emptySpace + ParamUtils.removePreceedingDashes(optionName) + emptySpace));
        if (paramValidValueInfo.getDescription() != null) {
            optionPanel.setToolTipText(paramValidValueInfo.getDescription().replaceAll("\\s+", " "));
        }


        final PropertySet vc = new PropertyContainer();
        vc.addProperty(Property.create(optionName, optionValue));
        vc.getDescriptor(optionName).setDisplayName(optionName);

        final BindingContext ctx = new BindingContext(vc);
        final JCheckBox field = new JCheckBox();
        field.setHorizontalAlignment(JFormattedTextField.LEFT);
        field.setName(optionName);
        field.setSelected(paramValidValueInfo.isSelected());
        if (paramValidValueInfo.getDescription() != null) {
            field.setToolTipText(paramValidValueInfo.getDescription().replaceAll("\\s+", " "));
        }

        ctx.bind(optionName, field);

        ctx.addPropertyChangeListener(optionName, new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent pce) {
                paramValidValueInfo.setSelected(field.isSelected());
            }
        });

        processorModel.addPropertyChangeListener(paramValidValueInfo.getValue(), new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                field.setSelected(paramValidValueInfo.isSelected());
            }
        });

        optionPanel.add(field);

        return optionPanel;

    }


    private String[] updateValidValues(ParamInfo pi) {
        final ArrayList<ParamValidValueInfo> validValues = pi.getValidValueInfos();
        final String[] values = new String[validValues.size()];
        ArrayList<String> toolTips = new ArrayList<String>();

        Iterator<ParamValidValueInfo> itr = validValues.iterator();
        int i = 0;
        ParamValidValueInfo paramValidValueInfo;
        while (itr.hasNext()) {
            paramValidValueInfo = itr.next();
            values[i] = paramValidValueInfo.getValue();
            if (paramValidValueInfo.getDescription() != null) {
                toolTips.add(paramValidValueInfo.getDescription().replaceAll("\\s+", " "));
            }
            i++;
        }
        return values;
    }



    private boolean isControlHandlerEnabled() {
        if (controlHandlerIntEnabled >= 1) {
            return true;
        } else {
            return false;
        }
    }

    private void enableControlHandler() {
        controlHandlerIntEnabled++;
    }

    private void disableControlHandler() {
        controlHandlerIntEnabled--;
    }



    private boolean isEventHandlerEnabled() {
        if (eventHandlerIntEnabled >= 1) {
            return true;
        } else {
            return false;
        }
    }

    private void enableEventHandler() {
        eventHandlerIntEnabled++;
    }

    private void disableEventHandler() {
        eventHandlerIntEnabled--;
    }


    private JPanel createIOFileOptionField(final ParamInfo pi) {


        final FileSelector ioFileSelector = new FileSelector(SnapApp.getDefault().getAppContext(), pi.getType(), ParamUtils.removePreceedingDashes(pi.getName()));
        ioFileSelector.getFileTextField().setColumns(40);
        ioFileSelector.setFilename(pi.getValue());
        if (pi.getDescription() != null) {
            ioFileSelector.getNameLabel().setToolTipText(pi.getDescription().replaceAll("\\s+", " "));
        }

        processorModel.addPropertyChangeListener(pi.getName(), new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                if (isEventHandlerEnabled()) {
                    disableControlHandler();
//                    if (isEventHandlerEnabled() || pi.getName().isEmpty()) {
                    ioFileSelector.setFilename(pi.getValue());
//                    }
                    enableControlHandler();
                }
            }
        });

        ioFileSelector.addPropertyChangeListener(ioFileSelector.getPropertyName(), new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                if (isControlHandlerEnabled()) {
                    disableEventHandler();

                    String iofileName;
                    if (ioFileSelector.getFileName() != null) {
                        iofileName = ioFileSelector.getFileName();
                        processorModel.updateParamInfo(pi, iofileName);
                    }

                    enableEventHandler();
                }
            }
        });



        ioFileSelector.getjPanel().setName(pi.getName());
        return ioFileSelector.getjPanel();
    }

    private class ComboboxToolTipRenderer extends DefaultListCellRenderer {
        ArrayList<String> tooltips;

        @Override
        public Component getListCellRendererComponent(JList list, Object value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {

            JComponent comp = (JComponent) super.getListCellRendererComponent(list,
                    value, index, isSelected, cellHasFocus);

            if (-1 < index && null != value && null != tooltips && !tooltips.isEmpty()) {
                list.setToolTipText(tooltips.get(index));
            }
            return comp;
        }

        public void setTooltips(ArrayList<String> tooltips) {
            this.tooltips = tooltips;
        }
    }

    private class ValidValueChooser extends JPanel {

        String selectedBoxes;
        JPanel valuesPanel;

        ValidValueChooser(ParamInfo paramInfo) {

        }


    }

    private class ValidValuesButtonAction implements ActionListener {

        final JPanel valuesPanel;

        String selectedValues;

        ValidValuesButtonAction(JPanel panel) {
            valuesPanel = panel;
        }

        public void actionPerformed(ActionEvent actionEvent) {
            final Window parent = SnapApp.getDefault().getMainFrame();
            String dialogTitle = null;
            final ModalDialog modalDialog = new ModalDialog(parent, dialogTitle, valuesPanel, ModalDialog.ID_OK, "test");
            final int dialogResult = modalDialog.show();
            if (dialogResult != ModalDialog.ID_OK) {

            }

        }

        void setSelectedValued(String selectedValues) {
            this.selectedValues = selectedValues;

        }

        String getSelectedValues() {
            return selectedValues;
        }
    }

}
