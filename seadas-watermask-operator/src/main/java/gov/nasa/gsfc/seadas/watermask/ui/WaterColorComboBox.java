package gov.nasa.gsfc.seadas.watermask.ui;

import org.openide.awt.ColorComboBox;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created with IntelliJ IDEA.
 * User: knowles
 * Date: 9/4/12
 * Time: 11:40 AM
 * To change this template use File | Settings | File Templates.
 */
public class WaterColorComboBox {    private LandMasksData landMasksData;

    private JLabel jLabel;
    private ColorComboBox colorExComboBox = new ColorComboBox();

    public WaterColorComboBox(LandMasksData landMasksData) {

        this.landMasksData = landMasksData;

        jLabel = new JLabel("Color");
        jLabel.setToolTipText("Water mask color");
        colorExComboBox.setSelectedColor(new Color(225,225,225));
        colorExComboBox.setPreferredSize(colorExComboBox.getPreferredSize());
        colorExComboBox.setMinimumSize(colorExComboBox.getPreferredSize());

        colorExComboBox.setSelectedColor((landMasksData.getWaterMaskColor()));

        addControlListeners();
    }


    private void addControlListeners() {

        colorExComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                landMasksData.setWaterMaskColor(colorExComboBox.getSelectedColor());
            }
        });
    }


    public JLabel getjLabel() {
        return jLabel;
    }

    public JComboBox  getColorExComboBox() {
        return colorExComboBox;
    }
}
