package gov.nasa.gsfc.seadas.processing.l2gen.userInterface;

import gov.nasa.gsfc.seadas.processing.core.L2genData;
import gov.nasa.gsfc.seadas.processing.l2gen.productData.L2genWavelengthInfo;
import gov.nasa.gsfc.seadas.processing.common.GridBagConstraintsCustom;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: knowles
 * Date: 5/11/12
 * Time: 3:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class L2genWavelengthLimiterPanel extends JPanel {

    private L2genData l2genData;

    private JPanel waveLimiterJPanel;

    private ArrayList<JCheckBox> wavelengthsJCheckboxArrayList = new ArrayList<JCheckBox>();

    private boolean waveLimiterControlHandlersEnabled = false;

    private JButton
            uvButton,
            visibleButton,
            nearInfraredButton,
            swirButton,
            infraredButton;

    private String
            UV_LABEL = "UV",
            VISIBLE_LABEL = "Visible",
            NIR_LABEL = "NIR",
            SWIR_LABEL = "SWIR",
            IR_LABEL = "IR";

    private String SELECT_ALL = "Select All";
    private String DESELECT_ALL = "Deselect All";

    private String
            SELECT_ALL_UV = SELECT_ALL + " " + UV_LABEL,
            DESELECT_ALL_UV = DESELECT_ALL + " " + UV_LABEL,
            SELECT_ALL_VISIBLE = SELECT_ALL + " " + VISIBLE_LABEL,
            DESELECT_ALL_VISIBLE = DESELECT_ALL + " " + VISIBLE_LABEL,
            SELECT_ALL_NEAR_INFRARED = SELECT_ALL + " " + NIR_LABEL,
            DESELECT_ALL_NEAR_INFRARED = DESELECT_ALL + " " + NIR_LABEL,
            SELECT_ALL_SWIR = SELECT_ALL + " " + SWIR_LABEL,
            DESELECT_ALL_SWIR = DESELECT_ALL + " " + SWIR_LABEL,
            SELECT_ALL_INFRARED = SELECT_ALL + " " + IR_LABEL,
            DESELECT_ALL_INFRARED = DESELECT_ALL + " " + IR_LABEL;
            ;




    L2genWavelengthLimiterPanel(L2genData l2genData) {
        this.l2genData = l2genData;
        initComponents();
        addComponents();

    }

    public void initComponents() {
        waveLimiterJPanel = createWaveLimiterJPanel();
        uvButton = createUVButton();
        visibleButton = createVisibleButton();
        nearInfraredButton = createNearInfraredButton();
        swirButton = createSwirButton();
        infraredButton = createInfraredButton();
    }

    public void addComponents() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createTitledBorder("Wavelength 3-Way Toggle Selection"));
        setToolTipText("<html>The wavelengths selected here are applied <br>in one of the 3-way toggle steps<br>when you check a wavelength dependent product checkbox</html>");

        JPanel innerPanel = new JPanel(new GridBagLayout());

        innerPanel.add(uvButton,
                new GridBagConstraintsCustom(0, 0, 1, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE));

        innerPanel.add(visibleButton,
                new GridBagConstraintsCustom(0, 1, 1, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE));

        innerPanel.add(nearInfraredButton,
                new GridBagConstraintsCustom(0, 2, 1, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE));

        innerPanel.add(swirButton,
                new GridBagConstraintsCustom(0, 3, 1, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE));

        innerPanel.add(infraredButton,
                new GridBagConstraintsCustom(0, 4, 1, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE));

        innerPanel.add(waveLimiterJPanel,
                new GridBagConstraintsCustom(0, 5, 1, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE));

                innerPanel.add(new JPanel(),
                new GridBagConstraintsCustom(0, 6, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.BOTH));

        JScrollPane innerScroll = new JScrollPane(innerPanel);
        innerScroll.setBorder(null);

        add(innerScroll,
                new GridBagConstraintsCustom(0, 0, 1, 1, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH));
    }



    private JButton createUVButton() {

        final JButton jButton = new JButton(SELECT_ALL_UV);

        jButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (jButton.getText().equals(SELECT_ALL_UV)) {
                    l2genData.setSelectedAllWaveLimiter(L2genWavelengthInfo.WaveType.UV, true);
                } else if (jButton.getText().equals(DESELECT_ALL_UV)) {
                    l2genData.setSelectedAllWaveLimiter(L2genWavelengthInfo.WaveType.UV, false);
                }
            }
        });


        l2genData.addPropertyChangeListener(L2genData.WAVE_LIMITER, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                updateUVButton();
            }
        });

        l2genData.addPropertyChangeListener(L2genData.IFILE, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                updateUVButton();
            }
        });

        return jButton;
    }


    private void updateUVButton() {

        // Set UV 'Select All' toggle to appropriate text and enabled
        if (l2genData.hasWaveType(L2genWavelengthInfo.WaveType.UV)) {
            uvButton.setEnabled(true);
            uvButton.setVisible(true);
            if (l2genData.isSelectedAllWaveLimiter(L2genWavelengthInfo.WaveType.UV)) {
                if (!uvButton.getText().equals(DESELECT_ALL_UV)) {
                    uvButton.setText(DESELECT_ALL_UV);
                }
            } else {
                if (!uvButton.getText().equals(SELECT_ALL_UV)) {
                    uvButton.setText(SELECT_ALL_UV);
                }
            }
        } else {
            uvButton.setEnabled(false);
            uvButton.setVisible(false);
        }
    }




    private JButton createVisibleButton() {

        final JButton jButton = new JButton(SELECT_ALL_VISIBLE);

        jButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (jButton.getText().equals(SELECT_ALL_VISIBLE)) {
                    l2genData.setSelectedAllWaveLimiter(L2genWavelengthInfo.WaveType.VISIBLE, true);
                } else if (jButton.getText().equals(DESELECT_ALL_VISIBLE)) {
                    l2genData.setSelectedAllWaveLimiter(L2genWavelengthInfo.WaveType.VISIBLE, false);
                }
            }
        });

        l2genData.addPropertyChangeListener(L2genData.WAVE_LIMITER, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                updateVisibleButton();
            }
        });

        l2genData.addPropertyChangeListener(L2genData.IFILE, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                updateVisibleButton();
            }
        });

        return jButton;
    }


    private void updateVisibleButton() {

        // Set VISIBLE 'Select All' toggle to appropriate text and enabled
        if (l2genData.hasWaveType(L2genWavelengthInfo.WaveType.VISIBLE)) {
            visibleButton.setEnabled(true);
            visibleButton.setVisible(true);
            if (l2genData.isSelectedAllWaveLimiter(L2genWavelengthInfo.WaveType.VISIBLE)) {
                if (!visibleButton.getText().equals(DESELECT_ALL_VISIBLE)) {
                    visibleButton.setText(DESELECT_ALL_VISIBLE);
                }
            } else {
                if (!visibleButton.getText().equals(SELECT_ALL_VISIBLE)) {
                    visibleButton.setText(SELECT_ALL_VISIBLE);
                }
            }
        } else {
            visibleButton.setEnabled(false);
            visibleButton.setVisible(false);
        }
    }



    private JButton createNearInfraredButton() {

        final JButton jButton = new JButton(SELECT_ALL_NEAR_INFRARED);

        jButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (jButton.getText().equals(SELECT_ALL_NEAR_INFRARED)) {
                    l2genData.setSelectedAllWaveLimiter(L2genWavelengthInfo.WaveType.NIR, true);
                } else if (jButton.getText().equals(DESELECT_ALL_NEAR_INFRARED)) {
                    l2genData.setSelectedAllWaveLimiter(L2genWavelengthInfo.WaveType.NIR, false);
                }
            }
        });


        l2genData.addPropertyChangeListener(L2genData.WAVE_LIMITER, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                updateNearInfraredButton();
            }
        });

        l2genData.addPropertyChangeListener(L2genData.IFILE, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                updateNearInfraredButton();
            }
        });

        return jButton;
    }


    private void updateNearInfraredButton() {
        // Set NEAR_INFRARED 'Select All' toggle to appropriate text and enabled
        if (l2genData.hasWaveType(L2genWavelengthInfo.WaveType.NIR)) {
            nearInfraredButton.setEnabled(true);
            nearInfraredButton.setVisible(true);
            if (l2genData.isSelectedAllWaveLimiter(L2genWavelengthInfo.WaveType.NIR)) {
                if (!nearInfraredButton.getText().equals(DESELECT_ALL_NEAR_INFRARED)) {
                    nearInfraredButton.setText(DESELECT_ALL_NEAR_INFRARED);
                }
            } else {
                if (!nearInfraredButton.getText().equals(SELECT_ALL_NEAR_INFRARED)) {
                    nearInfraredButton.setText(SELECT_ALL_NEAR_INFRARED);
                }
            }
        } else {
            nearInfraredButton.setEnabled(false);
            nearInfraredButton.setVisible(false);
        }
    }




    private JButton createSwirButton() {

        final JButton jButton = new JButton(SELECT_ALL_SWIR);

        jButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (jButton.getText().equals(SELECT_ALL_SWIR)) {
                    l2genData.setSelectedAllWaveLimiter(L2genWavelengthInfo.WaveType.SWIR, true);
                } else if (jButton.getText().equals(DESELECT_ALL_SWIR)) {
                    l2genData.setSelectedAllWaveLimiter(L2genWavelengthInfo.WaveType.SWIR, false);
                }
            }
        });


        l2genData.addPropertyChangeListener(L2genData.WAVE_LIMITER, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                updateSwirButton();
            }
        });

        l2genData.addPropertyChangeListener(L2genData.IFILE, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                updateSwirButton();
            }
        });

        return jButton;
    }


    private void updateSwirButton() {

        // Set UV 'Select All' toggle to appropriate text and enabled
        if (l2genData.hasWaveType(L2genWavelengthInfo.WaveType.SWIR)) {
            swirButton.setEnabled(true);
            swirButton.setVisible(true);
            if (l2genData.isSelectedAllWaveLimiter(L2genWavelengthInfo.WaveType.SWIR)) {
                if (!swirButton.getText().equals(DESELECT_ALL_SWIR)) {
                    swirButton.setText(DESELECT_ALL_SWIR);
                }
            } else {
                if (!swirButton.getText().equals(SELECT_ALL_SWIR)) {
                    swirButton.setText(SELECT_ALL_SWIR);
                }
            }
        } else {
            swirButton.setEnabled(false);
            swirButton.setVisible(false);
        }
    }





    private JButton createInfraredButton() {

        final JButton jButton = new JButton(SELECT_ALL_INFRARED);

        jButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (jButton.getText().equals(SELECT_ALL_INFRARED)) {
                    l2genData.setSelectedAllWaveLimiter(L2genWavelengthInfo.WaveType.IR, true);
                } else if (jButton.getText().equals(DESELECT_ALL_INFRARED)) {
                    l2genData.setSelectedAllWaveLimiter(L2genWavelengthInfo.WaveType.IR, false);
                }
            }
        });


        l2genData.addPropertyChangeListener(L2genData.WAVE_LIMITER, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                updateInfraredButton();
            }
        });

        l2genData.addPropertyChangeListener(L2genData.IFILE, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                updateInfraredButton();
            }
        });

        return jButton;
    }


    private void updateInfraredButton() {

        // Set INFRARED 'Select All' toggle to appropriate text and enabled
        if (l2genData.hasWaveType(L2genWavelengthInfo.WaveType.IR)) {
            infraredButton.setEnabled(true);
            infraredButton.setVisible(true);
            if (l2genData.isSelectedAllWaveLimiter(L2genWavelengthInfo.WaveType.IR)) {
                if (!infraredButton.getText().equals(DESELECT_ALL_INFRARED)) {
                    infraredButton.setText(DESELECT_ALL_INFRARED);
                }
            } else {
                if (!infraredButton.getText().equals(SELECT_ALL_INFRARED)) {
                    infraredButton.setText(SELECT_ALL_INFRARED);
                }
            }
        } else {
            infraredButton.setEnabled(false);
            infraredButton.setVisible(false);
        }
    }

//    private class InfraredButton {
//
//
//        private static final String selectAll = SELECT_ALL_INFRARED;
//        private static final String deselectAll = DESELECT_ALL_INFRARED;
//
//
//        InfraredButton() {
//        }
//
//        private JButton createInfraredButton() {
//
//            final JButton jButton = new JButton(selectAll);
//
//            jButton.addActionListener(new ActionListener() {
//                @Override
//                public void actionPerformed(ActionEvent e) {
//                    if (jButton.getText().equals(selectAll)) {
//                        l2genData.setSelectedAllWaveLimiter(L2genWavelengthInfo.WaveType.INFRARED, true);
//                    } else if (jButton.getText().equals(deselectAll)) {
//                        l2genData.setSelectedAllWaveLimiter(L2genWavelengthInfo.WaveType.INFRARED, false);
//                    }
//                }
//            });
//
//
//            l2genData.addPropertyChangeListener(L2genData.WAVE_LIMITER, new PropertyChangeListener() {
//                @Override
//                public void propertyChange(PropertyChangeEvent evt) {
//                    updateInfraredButton();
//                }
//            });
//
//            l2genData.addPropertyChangeListener(L2genData.IFILE, new PropertyChangeListener() {
//                @Override
//                public void propertyChange(PropertyChangeEvent evt) {
//                    updateInfraredButton();
//                }
//            });
//
//            return jButton;
//        }
//
//
//        private void updateInfraredButton() {
//
//            // Set INFRARED 'Select All' toggle to appropriate text and enabled
//            if (l2genData.hasWaveType(L2genWavelengthInfo.WaveType.INFRARED)) {
//                nearInfraredButton.setEnabled(true);
//                if (l2genData.isSelectedAllWaveLimiter(L2genWavelengthInfo.WaveType.INFRARED)) {
//                    if (!infraredButton.getText().equals(deselectAll)) {
//                        infraredButton.setText(deselectAll);
//                    }
//                } else {
//                    if (!infraredButton.getText().equals(selectAll)) {
//                        infraredButton.setText(selectAll);
//                    }
//                }
//            } else {
//                nearInfraredButton.setEnabled(false);
//            }
//        }
//    }



    private JPanel createWaveLimiterJPanel() {

        final JPanel jPanel = new JPanel(new GridBagLayout());

        l2genData.addPropertyChangeListener(L2genData.WAVE_LIMITER, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                updateWaveLimiterSelectionStates();
            }
        });

        l2genData.addPropertyChangeListener(L2genData.IFILE, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                updateWavelengthLimiterPanel();
                updateWaveLimiterSelectionStates();
            }
        });

        return jPanel;
    }


    private void updateWavelengthLimiterPanel() {

        waveLimiterJPanel.removeAll();

        // clear this because we dynamically rebuild it when input file selection is made or changed
        wavelengthsJCheckboxArrayList.clear();

        ArrayList<JCheckBox> wavelengthGroupCheckboxes = new ArrayList<JCheckBox>();

        for (L2genWavelengthInfo waveLimiterInfo : l2genData.getWaveLimiterInfos()) {

            final String currWavelength = waveLimiterInfo.getWavelengthString();
            final JCheckBox currJCheckBox = new JCheckBox(currWavelength);

            currJCheckBox.setName(currWavelength);

            // add current JCheckBox to the externally accessible arrayList
            wavelengthsJCheckboxArrayList.add(currJCheckBox);

            // add listener for current checkbox
            currJCheckBox.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    if (waveLimiterControlHandlersEnabled) {
                        l2genData.setSelectedWaveLimiter(currWavelength, currJCheckBox.isSelected());
                    }
                }
            });

            wavelengthGroupCheckboxes.add(currJCheckBox);
        }


        if (l2genData.hasWaveType(L2genWavelengthInfo.WaveType.UV)) {
            l2genData.setSelectedAllWaveLimiter(L2genWavelengthInfo.WaveType.UV, true);
        }

        if (l2genData.hasWaveType(L2genWavelengthInfo.WaveType.VISIBLE)) {
            l2genData.setSelectedAllWaveLimiter(L2genWavelengthInfo.WaveType.VISIBLE, true);
        }

        if (l2genData.hasWaveType(L2genWavelengthInfo.WaveType.NIR)) {
            l2genData.setSelectedAllWaveLimiter(L2genWavelengthInfo.WaveType.NIR, true);
        }

        if (l2genData.hasWaveType(L2genWavelengthInfo.WaveType.SWIR)) {
            l2genData.setSelectedAllWaveLimiter(L2genWavelengthInfo.WaveType.SWIR, true);
        }

        if (l2genData.hasWaveType(L2genWavelengthInfo.WaveType.IR)) {
            l2genData.setSelectedAllWaveLimiter(L2genWavelengthInfo.WaveType.IR, true);
        }


        // some GridBagLayout formatting variables
        int gridyCnt = 0;
        int gridxCnt = 0;
        int NUMBER_OF_COLUMNS = 2;


        for (JCheckBox wavelengthGroupCheckbox : wavelengthGroupCheckboxes) {
            // add current JCheckBox to the panel

            waveLimiterJPanel.add(wavelengthGroupCheckbox,
                    new GridBagConstraintsCustom(gridxCnt, gridyCnt, 1, 1, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE));


            // increment GridBag coordinates
            if (gridxCnt < (NUMBER_OF_COLUMNS - 1)) {
                gridxCnt++;
            } else {
                gridxCnt = 0;
                gridyCnt++;
            }
        }

        // just in case
        l2genData.fireEvent(l2genData.WAVE_LIMITER);
        // updateWaveLimiterSelectionStates();
    }


    /**
     * Set all waveLimiterInfos controls to agree with l2genData
     */
    private void updateWaveLimiterSelectionStates() {

        // Turn off control handlers until all controls are set
        waveLimiterControlHandlersEnabled = false;

        // Set all checkboxes to agree with l2genData
        for (L2genWavelengthInfo waveLimiterInfo : l2genData.getWaveLimiterInfos()) {
            for (JCheckBox currJCheckbox : wavelengthsJCheckboxArrayList) {
                if (waveLimiterInfo.getWavelengthString().equals(currJCheckbox.getName())) {
                    if (waveLimiterInfo.isSelected() != currJCheckbox.isSelected()) {
                        currJCheckbox.setSelected(waveLimiterInfo.isSelected());
                    }
                }
            }
        }


        // Turn on control handlers now that all controls are set
        waveLimiterControlHandlersEnabled = true;
    }


}
