package gov.nasa.gsfc.seadas.earthdatacloud.ui;

import gov.nasa.gsfc.seadas.earthdatacloud.preferences.Earthdata_Cloud_Controller;
import gov.nasa.gsfc.seadas.earthdatacloud.util.RegionUtils;
import gov.nasa.gsfc.seadas.earthdatacloud.util.RegionsInfo;
import org.jdatepicker.impl.*;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.text.DecimalFormat;
import java.util.*;

public class OBDAACDataBrowserPanels {
    private final OBDAACDataBrowser browser;

    public OBDAACDataBrowserPanels(OBDAACDataBrowser browser) {
        this.browser = browser;
    }

    public JPanel createLeftPanel() {
        GridBagConstraints gbc = new GridBagConstraints();
        GridBagLayout layout = new GridBagLayout();

        GridBagConstraints c = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;

        JPanel panel = new JPanel(layout);
        gbc.insets = new Insets(0, 0, 0, 0);

        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        panel.add(createSatelliteProductsPanel(), gbc);

        gbc.gridy = 1;
        gbc.gridx = 0;
        gbc.insets = new Insets(10, 0, 0, 10);
        gbc.gridwidth = 1;
        panel.add(createTemporalPanel(), gbc);

        gbc.gridy = 1;
        gbc.gridx = 1;
        gbc.insets = new Insets(10, 0, 0, 0);
        panel.add(createDayNightPanel(), gbc);

        return panel;
    }

    public JPanel createRightPanel() {
        GridBagConstraints gbc = new GridBagConstraints();
        GridBagLayout layout = new GridBagLayout();

        GridBagConstraints c = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;

        JPanel panel = new JPanel(layout);
        gbc.insets = new Insets(0, 0, 0, 0);

        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.weighty = 1;
        panel.add(createSpatialPanel(), gbc);

        return panel;
    }

    public JPanel createFilterPanel() {
        GridBagConstraints gbc = new GridBagConstraints();
        GridBagLayout layout = new GridBagLayout();

        GridBagConstraints c = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;

        JPanel panel = new JPanel(layout);
        gbc.insets = new Insets(0, 10, 0, 10);

        gbc.gridy = 0;
        gbc.gridx = 0;
        panel.add(createLeftPanel(), gbc);

        gbc.gridy = 0;
        gbc.gridx = 1;
        gbc.weighty = 1;
        gbc.insets = new Insets(0, 0, 0, 10);
        panel.add(createRightPanel(), gbc);

        return panel;
    }

    private JPanel createSatelliteProductsPanel() {
        System.out.println("Creating Satellite Products Panel");
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEtchedBorder());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(1, 1, 1, 1);
        gbc.anchor = GridBagConstraints.NORTHWEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(new JLabel("Satellite/Instrument:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        panel.add(browser.satelliteDropdown, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(new JLabel("Data Level:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        panel.add(browser.levelDropdown, gbc);

        gbc.weighty = 1;
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(new JLabel("Product Name:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        panel.add(browser.productDropdown, gbc);

        return panel;
    }

    private JPanel createTemporalPanel() {
        browser.temporalPanel = new JPanel(new GridBagLayout());
        browser.temporalPanel.setBorder(BorderFactory.createTitledBorder("Temporal Filter"));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 2, 2, 2);
        c.anchor = GridBagConstraints.NORTHWEST;

        browser.startDatePicker = browser.createDatePicker();
        browser.endDatePicker = browser.createDatePicker();

        Dimension fieldSize = new Dimension(150, 28);
        browser.startDatePicker.setPreferredSize(fieldSize);
        browser.startDatePicker.setMinimumSize(fieldSize);
        browser.endDatePicker.setPreferredSize(fieldSize);
        browser.endDatePicker.setMinimumSize(fieldSize);

        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.NONE;
        browser.temporalPanel.add(new JLabel("Start Date:"), c);

        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        browser.temporalPanel.add(browser.startDatePicker, c);

        c.gridx = 0;
        c.gridy = 1;
        c.fill = GridBagConstraints.NONE;
        browser.temporalPanel.add(new JLabel("End Date:"), c);

        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        browser.temporalPanel.add(browser.endDatePicker, c);

        browser.dateRangeHintLabel = new JLabel(" "); // Declare this as a field
        browser.dateRangeHintLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        browser.dateRangeHintLabel.setForeground(Color.DARK_GRAY);

        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 2;
        browser.temporalPanel.add(browser.dateRangeHintLabel, c);

        browser.dateRangeLabel = new JLabel("Valid date range: ");
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weighty = 1;
        c.insets = new Insets(5, 5, 5, 5);
        browser.temporalPanel.add(browser.dateRangeLabel, c);

        return browser.temporalPanel;
    }

    private JPanel createBoundingBoxPanel() {
        System.out.println("Creating BoundingBox Panel");
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createLineBorder(new Color(0,100,100)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.anchor = GridBagConstraints.WEST;

        Dimension fieldSize = new Dimension(150, 28);

        // North (maxLat)
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JLabel maxLatLabel = new JLabel(Earthdata_Cloud_Controller.PROPERTY_MAXLAT_LABEL + ":");
        maxLatLabel.setToolTipText(Earthdata_Cloud_Controller.PROPERTY_MAXLAT_TOOLTIP);
        panel.add(maxLatLabel, gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        browser.maxLatField = new JTextField(Earthdata_Cloud_Controller.getPreferenceMaxLat());
        browser.maxLatField.setToolTipText(Earthdata_Cloud_Controller.PROPERTY_MAXLAT_TOOLTIP);
        browser.maxLatField.setPreferredSize(fieldSize);
        browser.maxLatField.setMinimumSize(fieldSize);
        panel.add(browser.maxLatField, gbc);

        // South (minLat)
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JLabel minLatLabel = new JLabel(Earthdata_Cloud_Controller.PROPERTY_MINLAT_LABEL + ":");
        minLatLabel.setToolTipText(Earthdata_Cloud_Controller.PROPERTY_MINLAT_TOOLTIP);
        panel.add(minLatLabel, gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        browser.minLatField = new JTextField(Earthdata_Cloud_Controller.getPreferenceMinLat());
        browser.minLatField.setToolTipText(Earthdata_Cloud_Controller.PROPERTY_MINLAT_TOOLTIP);
        browser.minLatField.setPreferredSize(fieldSize);
        browser.minLatField.setMinimumSize(fieldSize);
        panel.add(browser.minLatField, gbc);

        // West (minLon)
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JLabel minLonLabel = new JLabel(Earthdata_Cloud_Controller.PROPERTY_MINLON_LABEL + ":");
        minLonLabel.setToolTipText(Earthdata_Cloud_Controller.PROPERTY_MINLON_TOOLTIP);
        panel.add(minLonLabel, gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        browser.minLonField = new JTextField(Earthdata_Cloud_Controller.getPreferenceMinLon());
        browser.minLonField.setToolTipText(Earthdata_Cloud_Controller.PROPERTY_MINLON_TOOLTIP);
        browser.minLonField.setPreferredSize(fieldSize);
        browser.minLonField.setMinimumSize(fieldSize);
        panel.add(browser.minLonField, gbc);

        // East (maxLon)
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JLabel maxLonLabel = new JLabel(Earthdata_Cloud_Controller.PROPERTY_MAXLON_LABEL + ":");
        maxLonLabel.setToolTipText(Earthdata_Cloud_Controller.PROPERTY_MAXLON_TOOLTIP);
        panel.add(maxLonLabel, gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        browser.maxLonField = new JTextField(Earthdata_Cloud_Controller.getPreferenceMaxLon());
        browser.maxLonField.setToolTipText(Earthdata_Cloud_Controller.PROPERTY_MAXLON_TOOLTIP);
        browser.maxLonField.setPreferredSize(fieldSize);
        browser.maxLonField.setMinimumSize(fieldSize);
        panel.add(browser.maxLonField, gbc);

        panel.setMinimumSize(panel.getPreferredSize());
        panel.setPreferredSize(panel.getPreferredSize());

        return panel;
    }

    public JPanel createSpatialPanel() {
        System.out.println("Creating createSpatialPanel");
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Spatial Filter"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(1, 1, 1, 1);
        gbc.anchor = GridBagConstraints.WEST;

        Dimension fieldSize = new Dimension(150, 28);

        // North (maxLat)
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JLabel maxLatLabel = new JLabel(Earthdata_Cloud_Controller.PROPERTY_MAXLAT_LABEL + ":");
        maxLatLabel.setToolTipText(Earthdata_Cloud_Controller.PROPERTY_MAXLAT_TOOLTIP);
        panel.add(maxLatLabel, gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        browser.maxLatField = new JTextField(Earthdata_Cloud_Controller.getPreferenceMaxLat());
        browser.maxLatField.setToolTipText(Earthdata_Cloud_Controller.PROPERTY_MAXLAT_TOOLTIP);
        browser.maxLatField.setPreferredSize(fieldSize);
        browser.maxLatField.setMinimumSize(fieldSize);
        panel.add(browser.maxLatField, gbc);

        // South (minLat)
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JLabel minLatLabel = new JLabel(Earthdata_Cloud_Controller.PROPERTY_MINLAT_LABEL + ":");
        minLatLabel.setToolTipText(Earthdata_Cloud_Controller.PROPERTY_MINLAT_TOOLTIP);
        panel.add(minLatLabel, gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        browser.minLatField = new JTextField(Earthdata_Cloud_Controller.getPreferenceMinLat());
        browser.minLatField.setToolTipText(Earthdata_Cloud_Controller.PROPERTY_MINLAT_TOOLTIP);
        browser.minLatField.setPreferredSize(fieldSize);
        browser.minLatField.setMinimumSize(fieldSize);
        panel.add(browser.minLatField, gbc);

        // West (minLon)
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JLabel minLonLabel = new JLabel(Earthdata_Cloud_Controller.PROPERTY_MINLON_LABEL + ":");
        minLonLabel.setToolTipText(Earthdata_Cloud_Controller.PROPERTY_MINLON_TOOLTIP);
        panel.add(minLonLabel, gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        browser.minLonField = new JTextField(Earthdata_Cloud_Controller.getPreferenceMinLon());
        browser.minLonField.setToolTipText(Earthdata_Cloud_Controller.PROPERTY_MINLON_TOOLTIP);
        browser.minLonField.setPreferredSize(fieldSize);
        browser.minLonField.setMinimumSize(fieldSize);
        panel.add(browser.minLonField, gbc);

        // East (maxLon)
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JLabel maxLonLabel = new JLabel(Earthdata_Cloud_Controller.PROPERTY_MAXLON_LABEL + ":");
        maxLonLabel.setToolTipText(Earthdata_Cloud_Controller.PROPERTY_MAXLON_TOOLTIP);
        panel.add(maxLonLabel, gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        browser.maxLonField = new JTextField(Earthdata_Cloud_Controller.getPreferenceMaxLon());
        browser.maxLonField.setToolTipText(Earthdata_Cloud_Controller.PROPERTY_MAXLON_TOOLTIP);
        browser.maxLonField.setPreferredSize(fieldSize);
        browser.maxLonField.setMinimumSize(fieldSize);
        panel.add(browser.maxLonField, gbc);

        // Separator line
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 4;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(10, 2, 10, 2);
        panel.add(new JSeparator(), gbc);
        gbc.insets = new Insets(2, 2, 2, 2);

        // Coordinates field
        gbc.gridy = 4;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JLabel coordinatesLabel = new JLabel("Coordinates:");
        coordinatesLabel.setToolTipText("Used to set fields north, south, west and east");
        panel.add(coordinatesLabel, gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        browser.coordinates = new JTextField("");
        browser.coordinates.setToolTipText("Used to set fields north, south, west and east");
        browser.coordinates.setPreferredSize(fieldSize);
        browser.coordinates.setMinimumSize(fieldSize);
        panel.add(browser.coordinates, gbc);

        // Box Size field
        gbc.gridy = 5;
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JLabel boxSizeLabel = new JLabel(Earthdata_Cloud_Controller.PROPERTY_BOX_SIZE_LABEL + ":");
        boxSizeLabel.setToolTipText(Earthdata_Cloud_Controller.PROPERTY_BOX_SIZE_TOOLTIP);
        panel.add(boxSizeLabel, gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        browser.boxSize = new JTextField(Earthdata_Cloud_Controller.getPreferenceBoxSize());
        browser.boxSize.setToolTipText(Earthdata_Cloud_Controller.PROPERTY_BOX_SIZE_TOOLTIP);
        browser.boxSize.setPreferredSize(fieldSize);
        browser.boxSize.setMinimumSize(fieldSize);
        panel.add(browser.boxSize, gbc);

        // Regional selection dropdown
        gbc.gridy = 6;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JLabel regionLabel = new JLabel("Region:");
        regionLabel.setToolTipText(Earthdata_Cloud_Controller.PROPERTY_REGION_TOOLTIP);
        panel.add(regionLabel, gbc);
        
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        
        // Load regions from the proper data source
        ArrayList<RegionsInfo> regionsList = RegionUtils.getAuxDataRegions("regions.txt", false);
        String[] regionNames = new String[regionsList.size()];
        for (int i = 0; i < regionsList.size(); i++) {
            regionNames[i] = regionsList.get(i).getName();
        }
        
        browser.regions = new JComboBox<>(regionNames);
        browser.regions.setSelectedIndex(0);
        
        // Add action listener to populate coordinates when region is selected
        browser.regions.addActionListener(e -> {
            int selectedIndex = browser.regions.getSelectedIndex();
            if (selectedIndex >= 0 && selectedIndex < regionsList.size()) {
                RegionsInfo selectedRegion = regionsList.get(selectedIndex);
                if (selectedRegion.isRegion()) {
                    // It's a bounding box region
                    browser.maxLatField.setText(selectedRegion.getNorth());
                    browser.minLatField.setText(selectedRegion.getSouth());
                    browser.minLonField.setText(selectedRegion.getWest());
                    browser.maxLonField.setText(selectedRegion.getEast());
                } else {
                    // It's a point location
                    String coords = selectedRegion.getCoordinates();
                    if (!coords.isEmpty()) {
                        String[] parts = coords.split("\\s+");
                        if (parts.length == 2) {
                            browser.maxLatField.setText(parts[0]);
                            browser.minLatField.setText(parts[0]);
                            browser.minLonField.setText(parts[1]);
                            browser.maxLonField.setText(parts[1]);
                        }
                    }
                }
            }
        });
        
        panel.add(browser.regions, gbc);

        return panel;
    }

    public JPanel createPaginationButtonPanel(JPanel panel1, JPanel panel2) {
        // (move method body from OBDAACDataBrowser, update field access to browser.field)
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(panel1, BorderLayout.CENTER);
        panel.add(panel2, BorderLayout.SOUTH);
        return panel;
    }

    public JPanel createPaginationPanel() {
        // Create pagination components properly like in the original code
        JPanel panel = new JPanel(new BorderLayout());

        JPanel fetchedPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        browser.prevPageButton = new JButton("Previous");
        browser.nextPageButton = new JButton("Next");
        browser.pageLabel = new JLabel("Page 1");
        browser.fetchedLabel = new JLabel("");

        browser.prevPageButton.addActionListener(e -> {
            if (browser.currentPage > 1) {
                browser.currentPage--;
                browser.updateResultsTable(browser.currentPage);
            }
        });

        browser.nextPageButton.addActionListener(e -> {
            if (browser.currentPage < browser.totalPages) {
                browser.currentPage++;
                browser.updateResultsTable(browser.currentPage);
            }
        });

        fetchedPanel.add(browser.fetchedLabel);
        navPanel.add(browser.prevPageButton);
        navPanel.add(browser.pageLabel);
        navPanel.add(browser.nextPageButton);

        JButton downloadButton = new JButton("Download");
        downloadButton.addActionListener(e -> browser.downloadSelectedFiles());
        JPanel downloadPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        downloadPanel.add(downloadButton);

        panel.add(fetchedPanel, BorderLayout.WEST);
        panel.add(navPanel, BorderLayout.CENTER);
        panel.add(downloadPanel, BorderLayout.EAST);

        return panel;
    }

    public JPanel createDayNightPanel() {
        // (move method body from OBDAACDataBrowser, update field access to browser.field)
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Day/Night Filter"));
        
        browser.dayButton = new JRadioButton("Day");
        browser.nightButton = new JRadioButton("Night");
        browser.bothButton = new JRadioButton("Both");
        browser.dayButton.setSelected(true);
        
        browser.dayNightGroup = new ButtonGroup();
        browser.dayNightGroup.add(browser.dayButton);
        browser.dayNightGroup.add(browser.nightButton);
        browser.dayNightGroup.add(browser.bothButton);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 5, 2, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        
        // Stack buttons vertically
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(browser.dayButton, gbc);
        
        gbc.gridy = 1;
        panel.add(browser.nightButton, gbc);
        
        gbc.gridy = 2;
        panel.add(browser.bothButton, gbc);
        
        return panel;
    }
} 