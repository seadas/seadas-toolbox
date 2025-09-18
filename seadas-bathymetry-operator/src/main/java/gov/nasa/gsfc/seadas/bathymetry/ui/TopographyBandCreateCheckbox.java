package gov.nasa.gsfc.seadas.bathymetry.ui;

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
public class TopographyBandCreateCheckbox {
    private BathymetryData bathymetryData;

    private JLabel jLabel;
    private JCheckBox jCheckBox = new JCheckBox();

    private static String DEFAULT_NAME = "Create Topography Band";
    private static String DEFAULT_TOOLTIPS = "Note: this can take longer to run";

    public TopographyBandCreateCheckbox(BathymetryData bathymetryData) {


        this.bathymetryData = bathymetryData;

        jLabel = new JLabel(DEFAULT_NAME);
        jLabel.setToolTipText(DEFAULT_TOOLTIPS);
        jCheckBox.setSelected(bathymetryData.isCreateTopographyBand());

        addControlListeners();
    }

    private void addControlListeners() {
        jCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                bathymetryData.setCreateTopographyBand(jCheckBox.isSelected());
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
