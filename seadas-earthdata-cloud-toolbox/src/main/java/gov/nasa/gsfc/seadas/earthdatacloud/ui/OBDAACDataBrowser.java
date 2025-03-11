package gov.nasa.gsfc.seadas.earthdatacloud.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.Vector;

import gov.nasa.gsfc.seadas.earthdatacloud.data.OBDAACMetadataFetcher;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class OBDAACDataBrowser {
    private JFrame frame;
    private JTable table;
    private DefaultTableModel tableModel;
    private JComboBox<String> satelliteDropdown;
    private JTextField searchField;
    private JButton updateButton;

    public OBDAACDataBrowser() {
        frame = new JFrame("OBDAAC Data Browser");
        frame.setSize(900, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // Top Panel: Search and Update
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new FlowLayout());

        satelliteDropdown = new JComboBox<>();
        searchField = new JTextField(15);
        updateButton = new JButton("Update Metadata");

        topPanel.add(new JLabel("Select Satellite:"));
        topPanel.add(satelliteDropdown);
        topPanel.add(new JLabel("Search:"));
        topPanel.add(searchField);
        topPanel.add(updateButton);

        frame.add(topPanel, BorderLayout.NORTH);

        // Table Setup
        tableModel = new DefaultTableModel(new String[]{"Short Name", "Product Name", "Start Date", "End Date"}, 0);
        table = new JTable(tableModel);
        frame.add(new JScrollPane(table), BorderLayout.CENTER);

        // Load Data
        loadMetadata();

        // Add Event Listeners
        searchField.addActionListener(e -> searchData());
        satelliteDropdown.addActionListener(e -> filterBySatellite());
        updateButton.addActionListener(e -> updateMetadata());

        frame.setVisible(true);
    }

    private void loadMetadata() {
        try {

            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("obdaac_metadata.json");
            if (inputStream == null) {
                JOptionPane.showMessageDialog(null, "Metadata file not found. Please update metadata first.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            Reader reader = new InputStreamReader(inputStream);
            JSONObject jsonObject = (JSONObject) new JSONParser().parse(reader);


            satelliteDropdown.removeAllItems();
            tableModel.setRowCount(0);

            for (Object key : jsonObject.keySet()) {
                String satellite = key.toString();
                satelliteDropdown.addItem(satellite);

                JSONArray productList = (JSONArray) jsonObject.get(satellite);
                for (Object obj : productList) {
                    JSONObject product = (JSONObject) obj;
                    tableModel.addRow(new Object[]{
                            product.get("short_name"),
                            product.get("product_name"),
                            product.get("time_start"),
                            product.get("time_end")
                    });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void searchData() {
        String query = searchField.getText().toLowerCase();
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);
        sorter.setRowFilter(RowFilter.regexFilter(query));
    }

    private void filterBySatellite() {
        String selectedSatellite = (String) satelliteDropdown.getSelectedItem();
        loadMetadata();
        if (selectedSatellite != null) {
            for (int i = tableModel.getRowCount() - 1; i >= 0; i--) {
                if (!tableModel.getValueAt(i, 0).toString().contains(selectedSatellite)) {
                    tableModel.removeRow(i);
                }
            }
        }
    }

    private void updateMetadata() {
        JOptionPane.showMessageDialog(frame, "Fetching updated metadata...", "Update", JOptionPane.INFORMATION_MESSAGE);
        new Thread(() -> {
            OBDAACMetadataFetcher.main(null);
            SwingUtilities.invokeLater(this::loadMetadata);
        }).start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(OBDAACDataBrowser::new);
    }
}
