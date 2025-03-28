package gov.nasa.gsfc.seadas.earthdatacloud.action;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

// Custom TableCellRenderer to display clickable links in the table
public class LinkCellRenderer extends DefaultTableCellRenderer {
    @Override
//    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
//        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
//        if (value != null) {  // Check if value is not null
//            String link = value.toString();
//            label.setText("<html><a href=''>" + link + "</a></html>");
//        } else {
//            label.setText("No link"); // Handle null values
//        }
//        return label;
//    }
    public Component getTableCellRendererComponent(
            JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {

        JLabel label = new JLabel("<html><a href=''>" + value + "</a></html>");
        label.setForeground(Color.BLUE);
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return label;
    }
}
