package gov.nasa.gsfc.seadas.earthdatacloud.action;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public class WebPageFetcherWithJWT {

    public WebPageFetcherWithJWT() {
    }


    // Method to remove empty rows from the JTable's model
    public static void removeEmptyRows(DefaultTableModel model) {
        for (int row = model.getRowCount() - 1; row >= 0; row--) {
            boolean isEmpty = true;
            // Check if all cells in the row are empty (null or empty string)
            for (int col = 0; col < model.getColumnCount(); col++) {
                Object value = model.getValueAt(row, col);
                if (value != null && !value.toString().trim().isEmpty()) {
                    isEmpty = false; // Found a non-empty value
                    break;
                }
            }
            // If the row is empty, remove it from the model
            if (isEmpty) {
                model.removeRow(row);
            }
        }
    }

    public JTable getCollectionList(JSONObject jsonResponse) {

        JSONArray tableArray = null;
        JTable jTable = null;
        tableArray = (JSONArray) jsonResponse.get("collections");
        System.out.println("Response: " + jsonResponse.toString(4)); // Pretty print the JSON
        jTable = getJTableNew(tableArray);
        return jTable;

    }


    public JTable getSearchDataList(JSONObject jsonResponse) {

        JSONArray tableArray = null;
        JTable jTable = null;
        tableArray = (JSONArray) jsonResponse.get("links");
        System.out.println("Response: " + jsonResponse.toString(4)); // Pretty print the JSON
        jTable = getJTableNew(tableArray);
        return jTable;

    }


    public JTable getJTableNew(JSONArray dataArray) {

        //String[] columnNames = { "Rel", "HREF", "Title", "Temporal", "BBox" };
        String[] columnNames = {"Title", "HREF"};
        Object[][] dataSearchResult = new Object[dataArray.length()][2];


        // Iterate over the students array and add each row to the searchResultTable
        for (int i = 0; i < dataArray.length(); i++) {
            JSONObject dataURL = dataArray.getJSONObject(i);
            if (dataURL.has("temporal")) {
                dataSearchResult[i][0] = dataURL.getString("title");
                dataSearchResult[i][1] = dataURL.getString("href");
            }
        }

        DefaultTableModel model = new DefaultTableModel(dataSearchResult, columnNames);

        JTable searchResultTable = new JTable(model);

        // Set a custom renderer for the 'Link' column to display as a clickable link
        searchResultTable.getColumnModel().getColumn(1).setCellRenderer(new LinkCellRenderer());

        // Add a mouse listener to detect clicks on the link
        searchResultTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int row = searchResultTable.rowAtPoint(e.getPoint());
                int col = searchResultTable.columnAtPoint(e.getPoint());

                if (col == 1) { // If the link column is clicked
                    String url = (String) searchResultTable.getValueAt(row, col);
                    if (url != null && !url.isEmpty()) { // Ensure the URL is not null or empty
                        try {
                            // Open the link in the default browser
                            Desktop.getDesktop().browse(new URI(url));
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    } else {
                        System.out.println("No valid URL in this cell.");
                    }
                }
            }
        });

        removeEmptyRows(model);
        return searchResultTable;
    }
}

// Custom TableCellRenderer to display clickable links in the table
class LinkCellRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (value != null) {  // Check if value is not null
            String link = value.toString();
            label.setText("<html><a href=''>" + link + "</a></html>");
        } else {
            label.setText("No link"); // Handle null values
        }
        return label;
    }
}