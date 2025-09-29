package gov.nasa.gsfc.seadas.watermask.ui;

import gov.nasa.gsfc.seadas.watermask.preferences.Landmask_Controller;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * Created with IntelliJ IDEA.
 * User: knowles
 * Date: 9/4/12
 * Time: 10:27 AM
 * To change this template use File | Settings | File Templates.
 */
public class WatermaskCreateCheckbox {
    private LandMasksData landMasksData;

    private JLabel jLabel;
    private JCheckBox jCheckBox = new JCheckBox();

    public WatermaskCreateCheckbox(LandMasksData landMasksData) {

        this.landMasksData = landMasksData;

        jLabel = new JLabel(Landmask_Controller.PROPERTY_WATER_MASK_CREATE_LABEL);
        jLabel.setToolTipText(Landmask_Controller.PROPERTY_WATER_MASK_CREATE_TOOLTIP);
        jCheckBox.setSelected(landMasksData.isCreateWatermask());

        addControlListeners();
    }

    private void addControlListeners() {
        jCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                landMasksData.setCreateWatermask(jCheckBox.isSelected());
            }
        });
    }


    public JLabel getjLabel() {
        return jLabel;
    }

    public JCheckBox getjCheckBox() {
        return jCheckBox;
    }
}
