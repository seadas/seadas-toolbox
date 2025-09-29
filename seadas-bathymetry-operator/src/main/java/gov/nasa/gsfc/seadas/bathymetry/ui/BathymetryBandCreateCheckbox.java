package gov.nasa.gsfc.seadas.bathymetry.ui;

import gov.nasa.gsfc.seadas.bathymetry.preferences.Bathymetry_Controller;

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
public class BathymetryBandCreateCheckbox {
    private BathymetryData bathymetryData;

    private JLabel jLabel;
    private JCheckBox jCheckBox = new JCheckBox();

    public BathymetryBandCreateCheckbox(BathymetryData bathymetryData) {

        this.bathymetryData = bathymetryData;

        jLabel = new JLabel(Bathymetry_Controller.PROPERTY_BATHYMETRY_BAND_CREATE_LABEL);
        jLabel.setToolTipText(Bathymetry_Controller.PROPERTY_BATHYMETRY_BAND_CREATE_TOOLTIP);
        jCheckBox.setSelected(bathymetryData.isCreateBathymetryBand());

        addControlListeners();
    }

    private void addControlListeners() {
        jCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                bathymetryData.setCreateBathymetryBand(jCheckBox.isSelected());
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
