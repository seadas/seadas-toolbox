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
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.SwingPropertyChangeSupport;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import static org.esa.snap.ui.GridBagUtils.createConstraints;

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
    protected Boolean flaguseTextfieldIgnore = false;
    private JTextField field = new JTextField();

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


        JPanel fileParamPanel = new JPanel();
        fileParamPanel.setName("file parameter panel");
        JPanel buttonPanel = new JPanel();
        buttonPanel.setName("button panel");


        TableLayout fileParamLayout = new TableLayout(1);
        fileParamLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        fileParamPanel.setLayout(fileParamLayout);


        int numColumns = paramList.size() % 4 < paramList.size() % 5 ? 4 : 5;
        if (processorModel.getNumColumns() > 0) {
            numColumns = processorModel.getNumColumns();
        }
        
        // MainPanel

        final JPanel paramPanel = new JPanel();
        GridBagConstraints gbcMainPanel = createSubPanelGridBagConstraints();
        final JPanel textFieldMainPanel = GridBagUtils.createPanel();
        final JPanel booleanParamMainPanel = new JPanel();
        booleanParamMainPanel.setLayout(new TableLayout(numColumns));
        boolean subPanel0Found = false;



        // SubPanel1

        GridBagConstraints gbcSubPanel1 = createSubPanelGridBagConstraints();
        final JPanel textFieldSubPanel1 = GridBagUtils.createPanel();
        final JPanel booleanParamSubPanel1 = new JPanel();
        booleanParamSubPanel1.setLayout(new TableLayout(numColumns));
        boolean subPanel1Found = false;

        // SubPanel2

        GridBagConstraints gbcSubPanel2 = createSubPanelGridBagConstraints();
        final JPanel textFieldSubPanel2 = GridBagUtils.createPanel();
        final JPanel booleanParamSubPanel2 = new JPanel();
        booleanParamSubPanel2.setLayout(new TableLayout(numColumns));
        boolean subPanel2Found = false;


        // SubPanel3

        GridBagConstraints gbcSubPanel3 = createSubPanelGridBagConstraints();
        final JPanel textFieldSubPanel3 = GridBagUtils.createPanel();
        final JPanel booleanParamSubPanel3 = new JPanel();
        booleanParamSubPanel3.setLayout(new TableLayout(numColumns));
        boolean subPanel3Found = false;


        // SubPanel4

        GridBagConstraints gbcSubPanel4 = createSubPanelGridBagConstraints();
        final JPanel textFieldSubPanel4 = GridBagUtils.createPanel();
        final JPanel booleanParamSubPanel4 = new JPanel();
        booleanParamSubPanel4.setLayout(new TableLayout(numColumns));
        boolean subPanel4Found = false;


        


        
        Iterator<ParamInfo> itr = paramList.iterator();
        while (itr.hasNext()) {
            final ParamInfo pi = itr.next();

            if (pi.getSubPanelIndex() == 1) {
                subPanel1Found = true;
            } else if (pi.getSubPanelIndex() == 2) {
                subPanel2Found = true;
            } else if (pi.getSubPanelIndex() == 3) {
                subPanel3Found = true;
            } else if (pi.getSubPanelIndex() == 4) {
                subPanel4Found = true;
            }  else {
                subPanel0Found = true;
            }
            

            if (!(pi.getName().equals(processorModel.getPrimaryInputFileOptionName()) ||
                    pi.getName().equals(processorModel.getPrimaryOutputFileOptionName()) ||
                    pi.getName().equals(L2genData.GEOFILE) ||
                    pi.getName().equals("verbose") ||
                    pi.getName().equals("--verbose"))) {

                if (pi.getColSpan() > numColumns) {
                    gbcMainPanel.gridwidth = numColumns;
                    gbcSubPanel1.gridwidth = numColumns;
                    gbcSubPanel2.gridwidth = numColumns;
                    gbcSubPanel3.gridwidth = numColumns;
                    gbcSubPanel4.gridwidth = numColumns;
                } else {
                    gbcMainPanel.gridwidth = pi.getColSpan();
                    gbcSubPanel1.gridwidth = pi.getColSpan();
                    gbcSubPanel2.gridwidth = pi.getColSpan();
                    gbcSubPanel3.gridwidth = pi.getColSpan();
                    gbcSubPanel4.gridwidth = pi.getColSpan();
                }


                

                if (pi.hasValidValueInfos() && pi.getType() != ParamInfo.Type.FLAGS) {
                    if (pi.getSubPanelIndex() == 1) {
                        gbcSubPanel1 = preIncrementGridy(gbcSubPanel1, numColumns);
                        textFieldSubPanel1.add(makeComboBoxOptionPanel(pi, gbcSubPanel1.gridwidth), gbcSubPanel1);
                        gbcSubPanel1 = incrementGridxGridy(gbcSubPanel1, numColumns);
                    } else if (pi.getSubPanelIndex() == 2) {
                        gbcSubPanel2 = preIncrementGridy(gbcSubPanel2, numColumns);
                        textFieldSubPanel2.add(makeComboBoxOptionPanel(pi, gbcSubPanel2.gridwidth), gbcSubPanel2);
                        gbcSubPanel2 = incrementGridxGridy(gbcSubPanel2, numColumns);
                    } else if (pi.getSubPanelIndex() == 3) {
                        gbcSubPanel3 = preIncrementGridy(gbcSubPanel3, numColumns);
                        textFieldSubPanel3.add(makeComboBoxOptionPanel(pi, gbcSubPanel3.gridwidth), gbcSubPanel3);
                        gbcSubPanel3 = incrementGridxGridy(gbcSubPanel3, numColumns);
                    } else if (pi.getSubPanelIndex() == 4) {
                        gbcSubPanel4 = preIncrementGridy(gbcSubPanel4, numColumns);
                        textFieldSubPanel4.add(makeComboBoxOptionPanel(pi, gbcSubPanel4.gridwidth), gbcSubPanel4);
                        gbcSubPanel4 = incrementGridxGridy(gbcSubPanel4, numColumns);
                    } else {
                        gbcMainPanel = preIncrementGridy(gbcMainPanel, numColumns);
                        textFieldMainPanel.add(makeComboBoxOptionPanel(pi, gbcMainPanel.gridwidth), gbcMainPanel);
                        gbcMainPanel = incrementGridxGridy(gbcMainPanel, numColumns);
                    }
                } else {
                    switch (pi.getType()) {
                        case BOOLEAN:
                            if (pi.getSubPanelIndex() == 1) {
                                booleanParamSubPanel1.add(makeBooleanOptionField(pi));
                            } else if (pi.getSubPanelIndex() == 2) {
                                booleanParamSubPanel2.add(makeBooleanOptionField(pi));
                            } else if (pi.getSubPanelIndex() == 3) {
                                booleanParamSubPanel3.add(makeBooleanOptionField(pi));
                            } else if (pi.getSubPanelIndex() == 4) {
                                booleanParamSubPanel4.add(makeBooleanOptionField(pi));
                            } else {
                                booleanParamMainPanel.add(makeBooleanOptionField(pi));
                            }
                            break;
                        case IFILE:
                            fileParamPanel.add(createIOFileOptionField(pi));
                            break;
                        case OFILE:
                            if (pi.getSubPanelIndex() == 1) {
                                gbcSubPanel1 = preIncrementGridy(gbcSubPanel1, numColumns);
                                textFieldSubPanel1.add(createIOFileOptionField(pi), gbcSubPanel1);
                                gbcSubPanel1 = incrementGridxGridy(gbcSubPanel1, numColumns);
                            } else if (pi.getSubPanelIndex() == 2) {
                                gbcSubPanel2 = preIncrementGridy(gbcSubPanel2, numColumns);
                                textFieldSubPanel2.add(createIOFileOptionField(pi), gbcSubPanel2);
                                gbcSubPanel2 = incrementGridxGridy(gbcSubPanel2, numColumns);
                            } else if (pi.getSubPanelIndex() == 3) {
                                gbcSubPanel3 = preIncrementGridy(gbcSubPanel3, numColumns);
                                textFieldSubPanel3.add(createIOFileOptionField(pi), gbcSubPanel3);
                                gbcSubPanel3 = incrementGridxGridy(gbcSubPanel3, numColumns);
                            } else if (pi.getSubPanelIndex() == 4) {
                                gbcSubPanel4 = preIncrementGridy(gbcSubPanel4, numColumns);
                                textFieldSubPanel4.add(createIOFileOptionField(pi), gbcSubPanel4);
                                gbcSubPanel4 = incrementGridxGridy(gbcSubPanel4, numColumns);
                            } else {
                                gbcMainPanel = preIncrementGridy(gbcMainPanel, numColumns);
                                textFieldMainPanel.add(createIOFileOptionField(pi), gbcMainPanel);
                                gbcMainPanel = incrementGridxGridy(gbcMainPanel, numColumns);
                            }
                            break;
                        case DIR:
                            fileParamPanel.add(createIOFileOptionField(pi));
                            break;
                        case STRING:
                            if (pi.getSubPanelIndex() == 1) {
                                gbcSubPanel1 = preIncrementGridy(gbcSubPanel1, numColumns);
                                textFieldSubPanel1.add(makeOptionField(pi, gbcSubPanel1.gridwidth), gbcSubPanel1);
                                gbcSubPanel1 = incrementGridxGridy(gbcSubPanel1, numColumns);
                            } else if (pi.getSubPanelIndex() == 2) {
                                gbcSubPanel2 = preIncrementGridy(gbcSubPanel2, numColumns);
                                textFieldSubPanel2.add(makeOptionField(pi, gbcSubPanel2.gridwidth), gbcSubPanel2);
                                gbcSubPanel2 = incrementGridxGridy(gbcSubPanel2, numColumns);
                            } else if (pi.getSubPanelIndex() == 3) {
                                gbcSubPanel3 = preIncrementGridy(gbcSubPanel3, numColumns);
                                textFieldSubPanel3.add(makeOptionField(pi, gbcSubPanel3.gridwidth), gbcSubPanel3);
                                gbcSubPanel3 = incrementGridxGridy(gbcSubPanel3, numColumns);
                            } else if (pi.getSubPanelIndex() == 4) {
                                gbcSubPanel4 = preIncrementGridy(gbcSubPanel4, numColumns);
                                textFieldSubPanel4.add(makeOptionField(pi, gbcSubPanel4.gridwidth), gbcSubPanel4);
                                gbcSubPanel4 = incrementGridxGridy(gbcSubPanel4, numColumns);
                            } else {
                                gbcMainPanel = preIncrementGridy(gbcMainPanel, numColumns);
                                textFieldMainPanel.add(makeOptionField(pi, gbcMainPanel.gridwidth), gbcMainPanel);
                                gbcMainPanel = incrementGridxGridy(gbcMainPanel, numColumns);
                            }
                            break;
                        case INT:
                            if (pi.getSubPanelIndex() == 1) {
                                gbcSubPanel1 = preIncrementGridy(gbcSubPanel1, numColumns);
                                textFieldSubPanel1.add(makeOptionField(pi, gbcSubPanel1.gridwidth), gbcSubPanel1);
                                gbcSubPanel1 = incrementGridxGridy(gbcSubPanel1, numColumns);
                            } else if (pi.getSubPanelIndex() == 2) {
                                gbcSubPanel2 = preIncrementGridy(gbcSubPanel2, numColumns);
                                textFieldSubPanel2.add(makeOptionField(pi, gbcSubPanel2.gridwidth), gbcSubPanel2);
                                gbcSubPanel2 = incrementGridxGridy(gbcSubPanel2, numColumns);
                            } else if (pi.getSubPanelIndex() == 3) {
                                gbcSubPanel3 = preIncrementGridy(gbcSubPanel3, numColumns);
                                textFieldSubPanel3.add(makeOptionField(pi, gbcSubPanel3.gridwidth), gbcSubPanel3);
                                gbcSubPanel3 = incrementGridxGridy(gbcSubPanel3, numColumns);
                            } else if (pi.getSubPanelIndex() == 4) {
                                gbcSubPanel4 = preIncrementGridy(gbcSubPanel4, numColumns);
                                textFieldSubPanel4.add(makeOptionField(pi, gbcSubPanel4.gridwidth), gbcSubPanel4);
                                gbcSubPanel4 = incrementGridxGridy(gbcSubPanel4, numColumns);
                            } else {
                                gbcMainPanel = preIncrementGridy(gbcMainPanel, numColumns);
                                textFieldMainPanel.add(makeOptionField(pi, gbcMainPanel.gridwidth), gbcMainPanel);
                                gbcMainPanel = incrementGridxGridy(gbcMainPanel, numColumns);
                            }
                            break;
                        case FLOAT:
                            if (pi.getSubPanelIndex() == 1) {
                                gbcSubPanel1 = preIncrementGridy(gbcSubPanel1, numColumns);
                                textFieldSubPanel1.add(makeOptionField(pi, gbcSubPanel1.gridwidth), gbcSubPanel1);
                                gbcSubPanel1 = incrementGridxGridy(gbcSubPanel1, numColumns);
                            } else if (pi.getSubPanelIndex() == 2) {
                                gbcSubPanel2 = preIncrementGridy(gbcSubPanel2, numColumns);
                                textFieldSubPanel2.add(makeOptionField(pi, gbcSubPanel2.gridwidth), gbcSubPanel2);
                                gbcSubPanel2 = incrementGridxGridy(gbcSubPanel2, numColumns);
                            } else if (pi.getSubPanelIndex() == 3) {
                                gbcSubPanel3 = preIncrementGridy(gbcSubPanel3, numColumns);
                                textFieldSubPanel3.add(makeOptionField(pi, gbcSubPanel3.gridwidth), gbcSubPanel3);
                                gbcSubPanel3 = incrementGridxGridy(gbcSubPanel3, numColumns);
                            } else if (pi.getSubPanelIndex() == 4) {
                                gbcSubPanel4 = preIncrementGridy(gbcSubPanel4, numColumns);
                                textFieldSubPanel4.add(makeOptionField(pi, gbcSubPanel4.gridwidth), gbcSubPanel4);
                                gbcSubPanel4 = incrementGridxGridy(gbcSubPanel4, numColumns);
                            } else {
                                gbcMainPanel = preIncrementGridy(gbcMainPanel, numColumns);
                                textFieldMainPanel.add(makeOptionField(pi, gbcMainPanel.gridwidth), gbcMainPanel);
                                gbcMainPanel = incrementGridxGridy(gbcMainPanel, numColumns);
                            }
                            break;
                        case FLAGS:
                            int origInsetsTop = gbcMainPanel.insets.top;
                            int origInsetsBottom = gbcMainPanel.insets.bottom;
                            int origInsetsLeft = gbcMainPanel.insets.left;
                            gbcMainPanel.insets.top = 8;
                            gbcMainPanel.insets.bottom = 6;
                            gbcMainPanel.insets.left = 0;

                            textFieldMainPanel.add(makeButtonOptionPanel(pi), gbcMainPanel);
                            gbcMainPanel = incrementGridxGridy(gbcMainPanel, numColumns);

                            gbcMainPanel.insets.top = origInsetsTop;
                            gbcMainPanel.insets.bottom = origInsetsBottom;
                            gbcMainPanel.insets.left = origInsetsLeft;

                            break;
                        case BUTTON:
                            buttonPanel.add(makeActionButtonPanel(pi));
                            break;
                    }


                    //paramPanel.add(makeOptionField(pi));
                }

                gbcMainPanel.gridwidth = 1;
                gbcSubPanel1.gridwidth = 1;
                gbcSubPanel2.gridwidth = 1;
                gbcSubPanel3.gridwidth = 1;
                gbcSubPanel4.gridwidth = 1;

            }
        }



        JPanel subPanel0 = createSubPanel(textFieldMainPanel, booleanParamMainPanel, subPanel0Found, 0);
        JPanel subPanel1 = createSubPanel(textFieldSubPanel1, booleanParamSubPanel1, subPanel1Found, 1);
        JPanel subPanel2 = createSubPanel(textFieldSubPanel2, booleanParamSubPanel2, subPanel2Found, 2);
        JPanel subPanel3 = createSubPanel(textFieldSubPanel3, booleanParamSubPanel3, subPanel3Found, 3);
        JPanel subPanel4 = createSubPanel(textFieldSubPanel4, booleanParamSubPanel4, subPanel4Found, 4);



        GridBagConstraints gbcMain = new GridBagConstraints();
        gbcMain.gridx = 0;
        gbcMain.gridy = 0;
        gbcMain.weightx = 1;
        gbcMain.weighty = 0;
        gbcMain.anchor = GridBagConstraints.NORTHWEST;
        gbcMain.fill = GridBagConstraints.NONE;
        gbcMain = setSubPanelGbcInsetsToDefault(gbcMain);


        final JPanel mainPanel = GridBagUtils.createPanel();

        if (subPanel0Found) {
            setSubPanelGbcInsets(gbcMain,  0);
            mainPanel.add(subPanel0, gbcMain);
            gbcMain.gridy++;
            gbcMain = setSubPanelGbcInsetsToDefault(gbcMain);
        }

        if (subPanel1Found) {
            setSubPanelGbcInsets(gbcMain,  1);
            mainPanel.add(subPanel1, gbcMain);
            gbcMain.gridy++;
            gbcMain = setSubPanelGbcInsetsToDefault(gbcMain);
        }

        if (subPanel2Found) {
            setSubPanelGbcInsets(gbcMain,  2);
            mainPanel.add(subPanel2, gbcMain);
            gbcMain.gridy++;
            gbcMain = setSubPanelGbcInsetsToDefault(gbcMain);
        }

        if (subPanel3Found) {
            setSubPanelGbcInsets(gbcMain,  3);
            mainPanel.add(subPanel3, gbcMain);
            gbcMain.gridy++;
            gbcMain = setSubPanelGbcInsetsToDefault(gbcMain);
        }

        if (subPanel4Found) {
            setSubPanelGbcInsets(gbcMain,  4);
            mainPanel.add(subPanel4, gbcMain);
            gbcMain.gridy++;
            gbcMain = setSubPanelGbcInsetsToDefault(gbcMain);
        }

        mainPanel.add(buttonPanel, gbcMain);

        return mainPanel;
    }


    private GridBagConstraints setSubPanelGbcInsets(GridBagConstraints gbc, int panelIndex) {
        if (subPanelHasBorder(panelIndex)) {
            gbc.insets.top = 10;
            gbc.insets.bottom = 10;
            gbc.insets.left = 8;
            gbc.insets.right = 8;
        } else {
            setSubPanelGbcInsetsToDefault(gbc);
        }

        return gbc;
    }


    private GridBagConstraints setSubPanelGbcInsetsToDefault(GridBagConstraints gbc) {
        gbc.insets.top = 0;
        gbc.insets.bottom = 0;
        gbc.insets.left = 0;
        gbc.insets.right = 0;

        return gbc;
    }



    private GridBagConstraints createSubPanelGridBagConstraints() {
        GridBagConstraints gbcMainPanel = new GridBagConstraints();

            gbcMainPanel.gridx = 0;
            gbcMainPanel.gridy = 0;
            gbcMainPanel.weightx = 1;
            gbcMainPanel.weighty = 1;
            gbcMainPanel.fill = GridBagConstraints.NONE;
            gbcMainPanel.anchor = GridBagConstraints.NORTHWEST;
            gbcMainPanel.insets.top = 5;
            gbcMainPanel.insets.bottom = 7;
            gbcMainPanel.insets.left = 5;
            gbcMainPanel.insets.right = 35;

            return gbcMainPanel;
    }


    private  boolean subPanelHasBorder(int panelIndex) {
        String subPanelTitle = getSubPanelTitle(panelIndex);

        if (subPanelTitle != null) {
            return true;
        }

        return false;
    }


    private String getSubPanelTitle(int panelIndex) {
        String subPanelTitle = null;
        if (panelIndex == 1) {
            subPanelTitle = processorModel.getSubPanel1Title();
        } else if (panelIndex == 2) {
            subPanelTitle = processorModel.getSubPanel2Title();
        } else if (panelIndex == 3) {
            subPanelTitle = processorModel.getSubPanel3Title();
        } else if (panelIndex == 4) {
            subPanelTitle = processorModel.getSubPanel4Title();
        } else {
            subPanelTitle = processorModel.getSubPanel0Title();
        }

        return subPanelTitle;
    }



    private JPanel createSubPanel(JPanel textFieldSubPanel, JPanel booleanParamSubPanel, boolean subPanelFound, int panelIndex) {
        JPanel subPanel = new JPanel();
        if (subPanelFound) {
            subPanel.setName("subPanel " + panelIndex);
            TableLayout subPanelLayout = new TableLayout(1);
            subPanel.setLayout(subPanelLayout);

            String subPanelTitle = getSubPanelTitle(panelIndex);

            if (subPanelTitle != null && subPanelTitle.length() > 0) {
                if (subPanelTitle.trim().length() > 0) {
                    Font baseFont = subPanel.getFont();
                    Font newFont = new Font(baseFont.getName(), Font.ITALIC, baseFont.getSize());
                    Color titleColor = new Color(0,50,50);
                    Border etchedBorder = BorderFactory.createEtchedBorder();
                    Border titleBorder = BorderFactory.createTitledBorder(etchedBorder, subPanelTitle,TitledBorder.LEFT, TitledBorder.ABOVE_TOP, newFont, titleColor);
                    subPanel.setBorder(titleBorder);

//                    subPanel.setBorder(BorderFactory.createTitledBorder(subPanelTitle));
                } else if (subPanelTitle.length() > 0) {
                    subPanel.setBorder(BorderFactory.createEtchedBorder());
                } else {
//                    subPanel.setBorder(BorderFactory.createEmptyBorder());
                }
            }

            subPanel.add(textFieldSubPanel);
            subPanel.add(booleanParamSubPanel);
        }

        return subPanel;
    }


    GridBagConstraints preIncrementGridy(GridBagConstraints gbc, int numColumns) {

        int gridX = gbc.gridx + gbc.gridwidth;
        if (gridX > (numColumns)) {
            gbc.gridy += 1;
            gbc.gridx = 0;
            gbc.insets.top = 0;
        }

        return gbc;
    }

    GridBagConstraints incrementGridxGridy(GridBagConstraints gbc, int numColumns) {
        gbc.gridx += gbc.gridwidth;
        if (gbc.gridx > (numColumns - 1)) {
            gbc.gridy += 1;
            gbc.gridx = 0;
            gbc.insets.top = 0;
        }

        return gbc;
    }

    protected JPanel makeOptionField(final ParamInfo pi, int colSpan) {

        final String optionName = ParamUtils.removePreceedingDashes(pi.getName());
        final JPanel optionPanel = new JPanel();
        optionPanel.setName(optionName);
        TableLayout fieldLayout = new TableLayout(1);
        fieldLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        optionPanel.setLayout(fieldLayout);
        optionPanel.setName(optionName);
        optionPanel.add(new JLabel(" " + optionName));
        if (pi.getDescription() != null) {
            optionPanel.setToolTipText(pi.getDescription().replaceAll("\\s+", " "));
        }

        
        if (pi.getValue() == null || pi.getValue().trim().length() == 0) {
            if (pi.getDefaultValue() != null && pi.getDefaultValue().trim().length() != 0) {
                processorModel.updateParamInfo(pi, pi.getDefaultValue());
            }
        }

        final PropertyContainer vc = new PropertyContainer();

        vc.addProperty(Property.create(optionName, pi.getValue()));


        vc.getDescriptor(optionName).setDisplayName(optionName);
        final BindingContext ctx = new BindingContext(vc);
        final JTextField field = new JTextField();

        int firstColWidth = 10;
        int additionalColWidth = 14;

        if (colSpan >= 5) {
            field.setColumns(firstColWidth + (colSpan - 1) * additionalColWidth);
        } else if (colSpan == 4) {
            field.setColumns(firstColWidth + 44);
        } else if (colSpan == 3) {
            field.setColumns(firstColWidth + 30);
        } else if (colSpan == 2) {
            field.setColumns(firstColWidth + 15);
        } else {
            field.setColumns(firstColWidth);
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
                    if ("suite".equalsIgnoreCase(pi.getName())) {
                        int i= 0; //DEBUG
                    }
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

    private JPanel makeComboBoxOptionPanel(final ParamInfo pi, int colSpan) {
        final JPanel singlePanel = new JPanel();

        String optionName = ParamUtils.removePreceedingDashes(pi.getName());

        TableLayout comboParamLayout = new TableLayout(1);
        comboParamLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        singlePanel.setLayout(comboParamLayout);

        final JLabel optionNameLabel = new JLabel(" " + ParamUtils.removePreceedingDashes(pi.getName()));

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

        Dimension preferredComboBoxSize;

        int firstColWidth = 9;

        if (colSpan >= 5) {
            String initString = getStringOfSetLength( firstColWidth + (colSpan - 1) * 21);
            final String[] tmpValues = {initString};
            JComboBox<String> tmpComboBox = new JComboBox<String>(tmpValues);
            preferredComboBoxSize = tmpComboBox.getPreferredSize();
        } else if (colSpan == 4) {
            String initString = getStringOfSetLength(firstColWidth + 66);
            final String[] tmpValues = {initString};
            JComboBox<String> tmpComboBox = new JComboBox<String>(tmpValues);
            preferredComboBoxSize = tmpComboBox.getPreferredSize();
        } else if (colSpan == 3) {
            String initString = getStringOfSetLength(firstColWidth + 43);
            final String[] tmpValues = {initString};
            JComboBox<String> tmpComboBox = new JComboBox<String>(tmpValues);
            preferredComboBoxSize = tmpComboBox.getPreferredSize();
        } else if (colSpan == 2) {
            String initString = getStringOfSetLength(firstColWidth + 22);
            final String[] tmpValues = {initString};
            JComboBox<String> tmpComboBox = new JComboBox<String>(tmpValues);
            preferredComboBoxSize = tmpComboBox.getPreferredSize();
        } else {
            String initString = getStringOfSetLength(firstColWidth);
            final String[] tmpValues = {initString};
            JComboBox<String> tmpComboBox = new JComboBox<String>(tmpValues);
            preferredComboBoxSize = tmpComboBox.getPreferredSize();
        }

        final JComboBox<String> inputList = new JComboBox<String>(values);
        ComboboxToolTipRenderer renderer = new ComboboxToolTipRenderer();
        inputList.setRenderer(renderer);
        renderer.setTooltips(toolTips);
        inputList.setEditable(true);
        inputList.setName(pi.getName());
//        inputList.setPreferredSize(new Dimension(inputList.getPreferredSize().width,
//                inputList.getPreferredSize().height));
        inputList.setPreferredSize(preferredComboBoxSize);

        if (pi.getDescription() != null) {
            inputList.setToolTipText(pi.getDescription().replaceAll("\\s+", " "));
        }
        int defaultValuePosition = new ArrayList<String>(Arrays.asList(values)).indexOf(optionDefaultValue);

        if (defaultValuePosition != -1) {
            inputList.setSelectedIndex(defaultValuePosition);
        }



        final PropertyContainer vc = new PropertyContainer();
        vc.addProperty(Property.create(optionName, optionDefaultValue));
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

    private String getStringOfSetLength(int numChars) {
        String string = "";

        for (int i=0; i<numChars; i++) {
            string = string + "0";
        }

        return string;
    }

    private JPanel makeButtonOptionPanel(final ParamInfo pi) {
        final JPanel singlePanel = new JPanel();
        final JTextField field = new JTextField();

        TableLayout comboParamLayout = new TableLayout(8);
        comboParamLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        singlePanel.setLayout(comboParamLayout);

        final JButton optionNameButton = new JButton(ParamUtils.removePreceedingDashes(pi.getName()));
        optionNameButton.setName("optionButton");
        optionNameButton.setToolTipText(pi.getDescription().replaceAll("\\s+", " "));

        optionNameButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {

                // determine any custum user flag in the flaguse textfield
                String flaguseField = field.getText();

                String customFlagList = "";
                if (field != null && field.getText().length() > 0) {
                    String[] originalFlagArray = field.getText().split("[,\\s]");


                    for (String originalFlagName : originalFlagArray) {
                        boolean flagIsValid = false;
                        originalFlagName = originalFlagName.trim().toUpperCase();
                        for (ParamValidValueInfo validValueInfo : pi.getValidValueInfos()) {
                            String validFlagName = validValueInfo.getValue();
                            if (validFlagName != null && validFlagName.length() > 0) {
                                if (originalFlagName.equalsIgnoreCase(validFlagName.trim())) {
                                    flagIsValid = true;
                                }
                            }
                            String validFlagNameNegated = "~" + validValueInfo.getValue();
                            if (validFlagNameNegated != null && validFlagNameNegated.length() > 0) {
                                if (originalFlagName.equalsIgnoreCase(validFlagNameNegated.trim())) {
                                    flagIsValid = true;
                                }
                            }

                        }

                        if (!flagIsValid) {
                            if (customFlagList.length() > 0) {
                                customFlagList = customFlagList + "," + originalFlagName;
                            } else {
                                customFlagList = originalFlagName;
                            }
                        }
                    }
                }


                processorModel.updateParamInfo(pi, field.getText());
                String selectedFlags = chooseValidValues(pi);


                if (!"-1".equals(selectedFlags)) {
                    String newFlagList = "";

                    if (customFlagList != null && customFlagList.length() > 0) {
                        if (selectedFlags != null && selectedFlags.length() > 0) {
                            newFlagList = selectedFlags + "," + customFlagList;
                        } else {
                            newFlagList = customFlagList;
                        }
                    } else {
                        newFlagList = selectedFlags;
                    }

                    processorModel.updateParamInfo(pi, newFlagList);
                    String value2 = pi.getValue();
                }
            }
        });


        singlePanel.add(optionNameButton);
        singlePanel.setName(pi.getName());
        if (pi.getDescription() != null) {
            singlePanel.setToolTipText(pi.getDescription().replaceAll("\\s+", " "));
        }

        field.setText(pi.getValue());
        field.setColumns(47);
        if (pi.getDescription() != null) {
            field.setToolTipText(pi.getDescription().replaceAll("\\s+", " "));
        }
        field.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                flaguseTextfieldIgnore = true;
                String flaguse = field.getText();
                processorModel.updateParamInfo(pi, field.getText());
                String value = pi.getValue();
                flaguseTextfieldIgnore = false;
            }
        });


        processorModel.addPropertyChangeListener(pi.getName(), new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                if (!flaguseTextfieldIgnore) {
                    String flaguse = field.getText();
                    field.setText(pi.getValue());
                    String value = pi.getValue();
                }
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

        JPanel panel = GridBagUtils.createPanel();

        GridBagConstraints gbc = createConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;
        gbc.weightx = 1;
        gbc.anchor = GridBagConstraints.NORTHWEST;


        JPanel leftPanel = GridBagUtils.createPanel();
        GridBagConstraints gbcLeft = createConstraints();
        gbcLeft.fill = GridBagConstraints.BOTH;
        gbcLeft.weighty = 1;
        gbcLeft.weightx = 1;
        gbcLeft.anchor = GridBagConstraints.NORTHWEST;

        JPanel rightPanel = GridBagUtils.createPanel();
        GridBagConstraints gbcRight = createConstraints();
        gbcRight.fill = GridBagConstraints.BOTH;
        gbcRight.weighty = 1;
        gbcRight.weightx = 1;
        gbcRight.anchor = GridBagConstraints.NORTHWEST;




        String choosenValues = "";
        final ArrayList<ParamValidValueInfo> validValues = pi.getValidValueInfos();

        ParamValidValueInfo paramValidValueInfo;
        Iterator<ParamValidValueInfo> itr = validValues.iterator();
        int rowCount = 0;
        while (itr.hasNext()) {
            paramValidValueInfo = itr.next();
            if (!paramValidValueInfo.getValue().trim().equals("SPARE")) {
                rowCount++;
            }
        }
        int totalRows = rowCount;


        int halfNumTotalRows = (int) Math.floor(totalRows/2.0);
        rowCount = 0;
        int colCount = 0;

        itr = validValues.iterator();
        while (itr.hasNext()) {
            paramValidValueInfo = itr.next();
            if (!paramValidValueInfo.getValue().trim().equals("SPARE")) {
                // todo set this based on textfield

                if (pi.getValue() != null && pi.getValue().length() > 0) {
                    paramValidValueInfo.setSelected(false);
                    paramValidValueInfo.setSelectedNegated(false);

                    String[] values = pi.getValue().split("[,\\s]");
                    for (String value : values) {
                        if (value.trim().equalsIgnoreCase(paramValidValueInfo.getValue().trim().toUpperCase())) {
                            paramValidValueInfo.setSelected(true);
                        }
                        if (value.trim().equalsIgnoreCase("~" + paramValidValueInfo.getValue().trim().toUpperCase())) {
                            paramValidValueInfo.setSelectedNegated(true);
                        }
                    }
                }

                if (colCount == 0) {
                    gbcLeft.gridx=0;
                    leftPanel.add(makeValidValueCheckboxPositive(paramValidValueInfo), gbcLeft);
                    gbcLeft.gridx=1;
                    leftPanel.add(makeValidValueCheckboxNegative(paramValidValueInfo), gbcLeft);
                    gbcLeft.gridy++;
                } else {
                    gbcRight.gridx=0;
                    rightPanel.add(makeValidValueCheckboxPositive(paramValidValueInfo), gbcRight);
                    gbcRight.gridx=1;
                    rightPanel.add(makeValidValueCheckboxNegative(paramValidValueInfo), gbcRight);
                    gbcRight.gridy++;
                }

                rowCount++;
                if (rowCount > halfNumTotalRows) {
                    colCount = 1;
                    rowCount = 0;
                }
            }
        }

//        leftColumnPanel.repaint();
//        leftColumnPanel.validate();
//        rightColumnPanel.repaint();
//        rightColumnPanel.validate();


        gbc.gridx = 0;
        panel.add(leftPanel, gbc);
        gbc.gridx = 1;
        panel.add(new JLabel("              "), gbc);
        gbc.gridx = 2;
        panel.add(rightPanel, gbc);
//
//        panel.repaint();
//        panel.validate();



        final Window parent = SnapApp.getDefault().getMainFrame();
        String dialogTitle = "Option Selector: " + processorModel.getProgramName() + " - " + pi.getName();



        String origChosenValues = "";
        itr = validValues.iterator();

        while (itr.hasNext()) {
            paramValidValueInfo = itr.next();
            if (paramValidValueInfo.isSelected()) {
                origChosenValues = origChosenValues + paramValidValueInfo.getValue() + ",";
            }
        }
        if (choosenValues.indexOf(",") != -1) {
            origChosenValues = origChosenValues.substring(0, origChosenValues.lastIndexOf(","));
        }



        final ModalDialog modalDialog = new ModalDialog(parent, dialogTitle, panel, ModalDialog.ID_OK, pi.getName());
        final int dialogResult = modalDialog.show();
        if (dialogResult != ModalDialog.ID_OK) {

        }

        itr = validValues.iterator();

        while (itr.hasNext()) {
            paramValidValueInfo = itr.next();
            if (paramValidValueInfo.isSelected()) {
                choosenValues = choosenValues + paramValidValueInfo.getValue() + ",";
            }
            if (paramValidValueInfo.isSelectedNegated()) {
                choosenValues = choosenValues + "~" + paramValidValueInfo.getValue() + ",";
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
        optionPanel.setPreferredSize(new Dimension(300, 40));
        TableLayout booleanLayout = new TableLayout(3);
        booleanLayout.setTableFill(TableLayout.Fill.NONE);

        optionPanel.setLayout(booleanLayout);
        optionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        optionPanel.setAlignmentY(Component.CENTER_ALIGNMENT);
//        optionPanel.add(new JLabel(emptySpace + ParamUtils.removePreceedingDashes(optionName) + emptySpace));


        final PropertySet vc = new PropertyContainer();
        vc.addProperty(Property.create(optionName, optionValue));
        vc.getDescriptor(optionName).setDisplayName(optionName);

        final BindingContext ctx = new BindingContext(vc);
        final JCheckBox field = new JCheckBox(optionName);
//        field.setHorizontalAlignment(JFormattedTextField.LEFT);
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



        final String optionNameNegated = "~" + paramValidValueInfo.getValue();
        final boolean optionValueNegated = paramValidValueInfo.isSelectedNegated();

        final JCheckBox fieldNegatedCheckBox = new JCheckBox(optionNameNegated);

        fieldNegatedCheckBox.setName(optionNameNegated);
        fieldNegatedCheckBox.setSelected(paramValidValueInfo.isSelectedNegated());
        if (paramValidValueInfo.getDescription() != null) {
            fieldNegatedCheckBox.setToolTipText("NOT " +paramValidValueInfo.getDescription().replaceAll("\\s+", " "));
        }


        final PropertySet vc2 = new PropertyContainer();
        vc2.addProperty(Property.create(optionNameNegated, optionValueNegated));
        vc2.getDescriptor(optionNameNegated).setDisplayName(optionNameNegated);
        final BindingContext ctx2 = new BindingContext(vc2);

        ctx2.bind(optionNameNegated, fieldNegatedCheckBox);

        ctx2.addPropertyChangeListener(optionNameNegated, new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent pce) {
                paramValidValueInfo.setSelectedNegated(fieldNegatedCheckBox.isSelected());
            }
        });

        processorModel.addPropertyChangeListener(paramValidValueInfo.getValue(), new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                fieldNegatedCheckBox.setSelected(paramValidValueInfo.isSelectedNegated());
            }
        });


        final JLabel fieldNegatedLabel = new JLabel("      ");
        if (paramValidValueInfo.getDescription() != null) {
            fieldNegatedCheckBox.setToolTipText("NOT " +paramValidValueInfo.getDescription().replaceAll("\\s+", " "));
        }

        optionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        optionPanel.add(fieldNegatedLabel);
        optionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        optionPanel.add(fieldNegatedCheckBox);


        return optionPanel;

    }


    private JPanel makeValidValueCheckboxPositive(final ParamValidValueInfo paramValidValueInfo) {

        final String flagName = paramValidValueInfo.getValue();
        final boolean flagSelected = paramValidValueInfo.isSelected();

        JPanel panel = GridBagUtils.createPanel();

        GridBagConstraints gbc = createConstraints();

        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;
        gbc.weightx = 1;
        gbc.anchor = GridBagConstraints.WEST;

        panel.setName(flagName);
        panel.setBorder(new EtchedBorder());
        panel.setPreferredSize(new Dimension(150, 40));

        final PropertySet vc = new PropertyContainer();
        vc.addProperty(Property.create(flagName, flagSelected));
        vc.getDescriptor(flagName).setDisplayName(flagName);

        final BindingContext ctx = new BindingContext(vc);
        final JCheckBox flagCheckBox = new JCheckBox(flagName);

        flagCheckBox.setName(flagName);
        flagCheckBox.setSelected(paramValidValueInfo.isSelected());
        if (paramValidValueInfo.getDescription() != null) {
            flagCheckBox.setToolTipText(paramValidValueInfo.getDescription().replaceAll("\\s+", " "));
            panel.setToolTipText(paramValidValueInfo.getDescription().replaceAll("\\s+", " "));
        }

        ctx.bind(flagName, flagCheckBox);

        ctx.addPropertyChangeListener(flagName, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent pce) {
                paramValidValueInfo.setSelected(flagCheckBox.isSelected());
//                if (field.isSelected()) {
//                    paramValidValueInfo.setSelectedNegated(false);
//                }
            }
        });

        processorModel.addPropertyChangeListener(paramValidValueInfo.getValue(), new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                flagCheckBox.setSelected(paramValidValueInfo.isSelected());
//                if (paramValidValueInfo.isSelectedNegated()) {
//                    field.setSelected(false);
//                }
            }
        });



        panel.add(flagCheckBox, gbc);

        return panel;
    }





    private JPanel makeValidValueCheckboxNegative(final ParamValidValueInfo paramValidValueInfo) {

        final String flagName = "~" + paramValidValueInfo.getValue();
        final boolean flagSelectedNegated = paramValidValueInfo.isSelectedNegated();

        JPanel panel = GridBagUtils.createPanel();

        if ("NONE".equalsIgnoreCase(paramValidValueInfo.getValue())) {
            return panel;
        }

        GridBagConstraints gbc = createConstraints();

        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;
        gbc.weightx = 1;
        gbc.anchor = GridBagConstraints.WEST;

        panel.setName(flagName);
        panel.setBorder(new EtchedBorder());
        panel.setPreferredSize(new Dimension(150, 40));

        final PropertySet vc = new PropertyContainer();
        vc.addProperty(Property.create(flagName, flagSelectedNegated));
        vc.getDescriptor(flagName).setDisplayName(flagName);

        final BindingContext ctx = new BindingContext(vc);
        final JCheckBox flagCheckBox = new JCheckBox(flagName);

        flagCheckBox.setName(flagName);
        flagCheckBox.setSelected(paramValidValueInfo.isSelectedNegated());
        if (paramValidValueInfo.getDescription() != null) {
            flagCheckBox.setToolTipText("NOT " + paramValidValueInfo.getDescription().replaceAll("\\s+", " "));
            panel.setToolTipText("NOT " + paramValidValueInfo.getDescription().replaceAll("\\s+", " "));
        }

        ctx.bind(flagName, flagCheckBox);

        ctx.addPropertyChangeListener(flagName, new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent pce) {
                paramValidValueInfo.setSelectedNegated(flagCheckBox.isSelected());
//                if (field.isSelected()) {
//                    paramValidValueInfo.setSelected(false);
//                }
            }
        });

        processorModel.addPropertyChangeListener(paramValidValueInfo.getValue(), new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                flagCheckBox.setSelected(paramValidValueInfo.isSelectedNegated());
//                if (paramValidValueInfo.isSelected()) {
//                    field.setSelected(false);
//                }
            }
        });

        panel.add(flagCheckBox, gbc);

        return panel;
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
