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
 * Time: 10:57 AM
 * To change this template use File | Settings | File Templates.
 */
public class CoastlineColorComboBox {
    private LandMasksData landMasksData;

    private JLabel jLabel;
    private ColorComboBox colorExComboBox = new ColorComboBox();

    public CoastlineColorComboBox(LandMasksData landMasksData) {

        this.landMasksData = landMasksData;

        jLabel = new JLabel("Color");
        jLabel.setToolTipText("Coastline mask color");

        colorExComboBox.setSelectedColor(new Color(225,225,225));
        colorExComboBox.setPreferredSize(colorExComboBox.getPreferredSize());
        colorExComboBox.setMinimumSize(colorExComboBox.getPreferredSize());

        colorExComboBox.setSelectedColor(landMasksData.getCoastlineMaskColor());

        addControlListeners();
    }


    private void addControlListeners() {

        colorExComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                landMasksData.setCoastlineMaskColor(colorExComboBox.getSelectedColor());
            }
        });
    }


    public JLabel getjLabel() {
        return jLabel;
    }

    public JComboBox getColorExComboBox() {
        return colorExComboBox;
    }
}
