package gov.nasa.gsfc.seadas.processing.l2gen.userInterface;

import gov.nasa.gsfc.seadas.processing.core.L2genData;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Created by IntelliJ IDEA.
 * User: knowles
 * Date: 6/6/12
 * Time: 4:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class L2genExcludeIOfileSpecifier {

        private final JCheckBox jCheckBox;
        private L2genData l2genData;
        private boolean controlHandlerEnabled = true;

        public L2genExcludeIOfileSpecifier(L2genData l2genData) {

            this.l2genData = l2genData;
            String NAME = "Exclude i/o Files";
            String TOOL_TIP = "<html>'Load/Save' parameters functionality will exclude primary i/o file fields<br>and scene dependent ancillary files<br>Useful when reusing parfile for different ifile scenes</html>";

            jCheckBox = new JCheckBox(NAME);

            jCheckBox.setSelected(l2genData.isExcludeCurrentIOfile());
            jCheckBox.setToolTipText(TOOL_TIP);


            addControlListeners();
            addEventListeners();
        }

        private void addControlListeners() {
            jCheckBox.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    if (isControlHandlerEnabled()) {
                        l2genData.setExcludeCurrentIOfile(jCheckBox.isSelected());
                    }
                }
            });
        }

        private void addEventListeners() {
            l2genData.addPropertyChangeListener(l2genData.EXCLUDE_IOFILE, new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    disableControlHandler();
                    jCheckBox.setSelected(l2genData.isExcludeCurrentIOfile());
                    enableControlHandler();
                }
            });

        }

        private boolean isControlHandlerEnabled() {
            return controlHandlerEnabled;
        }

        private void enableControlHandler() {
            controlHandlerEnabled = true;
        }

        private void disableControlHandler() {
            controlHandlerEnabled = false;
        }

        public JCheckBox getjCheckBox() {
            return jCheckBox;
        }
}
