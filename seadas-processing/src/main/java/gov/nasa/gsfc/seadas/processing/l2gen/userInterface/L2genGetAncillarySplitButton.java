package gov.nasa.gsfc.seadas.processing.l2gen.userInterface;

import gov.nasa.gsfc.seadas.processing.core.L2genData;
import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;


/**
 * Created with IntelliJ IDEA.
 * User: knowles
 * Date: 12/14/12
 * Time: 3:58 PM
 * To change this template use File | Settings | File Templates.
 */
// May 22, 2023 - Knowles - Updated to remove JideSplitButton as it was breaking things with Java11 on Mac



// http://java.dzone.com/news/drop-down-buttons-swing-new-al


public class L2genGetAncillarySplitButton {

    private L2genData l2genData;

    private JComboBox ancillaryComboBox;

    private final String GET_ANCILLARY = " Get Ancillary";
    private final  String REFRESH_ANCILLARY = " Refresh";
    private final String NEAR_REAL_TIME_NO2_ANCILLARY = " Near Real-Time NO2";
    private final String FORCE_DOWNLOAD_ANCILLARY = " Force Download";


    public L2genGetAncillarySplitButton(L2genData l2genData) {

        this.l2genData = l2genData;

        final String[] jComboBoxArray = new String[5];
        final String[] jComboBoxToolTipsArray = new String[5];
        jComboBoxArray[0] = "-- Get Ancillary Options --";
        jComboBoxToolTipsArray[0] = "Retrieve ancillary files with options below";

        jComboBoxArray[1] = GET_ANCILLARY;
        jComboBoxToolTipsArray[1] = "Run getanc";

        jComboBoxArray[2] = REFRESH_ANCILLARY;
        jComboBoxToolTipsArray[2] = "Run getanc with option:" + REFRESH_ANCILLARY;

        jComboBoxArray[3] = NEAR_REAL_TIME_NO2_ANCILLARY;
        jComboBoxToolTipsArray[3] = "Run getanc with option:" + NEAR_REAL_TIME_NO2_ANCILLARY;

        jComboBoxArray[4] = FORCE_DOWNLOAD_ANCILLARY;
        jComboBoxToolTipsArray[4] = "Run getanc with option:" + FORCE_DOWNLOAD_ANCILLARY;

        ancillaryComboBox = new JComboBox(jComboBoxArray);
        ancillaryComboBox.setEnabled(false);

        final MyComboBoxRenderer myComboBoxRenderer = new MyComboBoxRenderer();
        myComboBoxRenderer.setTooltipList(jComboBoxToolTipsArray);

        Boolean[] jComboBoxEnabledArray = {false, true, true, true, true};

        myComboBoxRenderer.setEnabledList(jComboBoxEnabledArray);

        ancillaryComboBox.setRenderer(myComboBoxRenderer);
        ancillaryComboBox.setEditable(false);

        ancillaryComboBox.setPreferredSize(ancillaryComboBox.getPreferredSize());
        ancillaryComboBox.setMaximumSize(ancillaryComboBox.getPreferredSize());
        ancillaryComboBox.setMinimumSize(ancillaryComboBox.getPreferredSize());

        addControlListeners();
        addEventListeners();
    }

    private void addControlListeners() {

        ancillaryComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (GET_ANCILLARY.equals(ancillaryComboBox.getSelectedItem())) {
                    l2genData.setAncillaryFiles(false, false, false);
                } else if (REFRESH_ANCILLARY.equals(ancillaryComboBox.getSelectedItem())) {
                    l2genData.setAncillaryFiles(true,false,false);
                } else if (NEAR_REAL_TIME_NO2_ANCILLARY.equals(ancillaryComboBox.getSelectedItem())) {
                    l2genData.setAncillaryFiles(false,false,true);
                } else if (FORCE_DOWNLOAD_ANCILLARY.equals(ancillaryComboBox.getSelectedItem())) {
                    l2genData.setAncillaryFiles(false,true,false);
                } else {
                }

            }
        });

    }

    private void addEventListeners() {
        l2genData.addPropertyChangeListener(L2genData.IFILE, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                ancillaryComboBox.setEnabled(l2genData.isValidIfile());
            }
        });

    }

    public JComboBox getAncillarySplitButton() {
        return ancillaryComboBox;
    }



    class MyComboBoxRenderer extends BasicComboBoxRenderer {

        private String[] tooltips;
        private Boolean[] enabledList;



        public Component getListCellRendererComponent(JList list, Object value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {


            if (isSelected) {
                if (-1 < index && index < tooltips.length) {
                    list.setToolTipText(tooltips[index]);
                }

                if (-1 < index && index < enabledList.length) {
                    if (enabledList[index] == true) {
                        setBackground(Color.blue);
                        setForeground(Color.white);
                    } else {
                        setBackground(Color.blue);
                        setForeground(Color.gray);
                    }
                }


            } else {

                if (-1 < index && index < enabledList.length) {
                    if (enabledList[index] == true) {
                        setBackground(Color.white);
                        setForeground(Color.black);
                    } else {
                        setBackground(Color.white);
                        setForeground(Color.gray);
                    }

                }

            }

            setFont(list.getFont());
            setText((value == null) ? "" : value.toString());
            return this;
        }

        public void setTooltipList(String[] tooltipList) {
            this.tooltips = tooltipList;
        }

        public void setEnabledList(Boolean[] enabledList) {
            this.enabledList = enabledList;
        }
    }

}
