package gov.nasa.gsfc.seadas.earthdatacloud.ui;

import gov.nasa.gsfc.seadas.earthdatacloud.preferences.Earthdata_Cloud_Controller;
import javax.swing.*;
import java.awt.*;

public class OBDAACDataBrowserPanels {
    
    /**
     * Creates a panel containing Search and Cancel buttons.
     * @param searchAction The action to perform when Search button is clicked
     * @param cancelAction The action to perform when Cancel button is clicked
     * @return JPanel containing the buttons
     */
    public JPanel createButtonPanel(Runnable searchAction, Runnable cancelAction) {
        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(e -> searchAction.run());
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> cancelAction.run());
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(searchButton);
        buttonPanel.add(cancelButton);
        
        return buttonPanel;
    }
    
    /**
     * Creates a panel containing Max Results and Results Per Page spinners.
     * @param maxApiResultsSpinner The spinner for max API results (will be configured)
     * @param resultsPerPageSpinner The spinner for results per page (will be configured)
     * @return JPanel containing the spinners
     */
    public JPanel createSpinnerPanel(JSpinner maxApiResultsSpinner, JSpinner resultsPerPageSpinner) {
        JPanel paginationPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        paginationPanel.add(new JLabel("Max Results:"));

        int maxResultsPref = Earthdata_Cloud_Controller.getPreferenceFetchMaxResults();
        int maxResultsMin = Earthdata_Cloud_Controller.PROPERTY_FETCH_MAX_RESULTS_MIN_VALUE;
        int maxResultsMax = Earthdata_Cloud_Controller.PROPERTY_FETCH_MAX_RESULTS_MAX_VALUE;
        maxApiResultsSpinner.setModel(new SpinnerNumberModel(maxResultsPref, maxResultsMin, maxResultsMax, 1));
        maxApiResultsSpinner.setPreferredSize(new Dimension(80, 25));
        maxApiResultsSpinner.setToolTipText(Earthdata_Cloud_Controller.PROPERTY_FETCH_MAX_RESULTS_TOOLTIP);

        paginationPanel.add(maxApiResultsSpinner);
        paginationPanel.add(Box.createHorizontalStrut(20));
        paginationPanel.add(new JLabel("Results Per Page:"));

        int resultsPerPagePref = Earthdata_Cloud_Controller.getPreferenceFetchResultsPerPage();
        int resultsPerPageMin = Earthdata_Cloud_Controller.PROPERTY_FETCH_RESULTS_PER_PAGE_MIN_VALUE;
        int resultsPerPageMax = Earthdata_Cloud_Controller.PROPERTY_FETCH_RESULTS_PER_PAGE_MAX_VALUE;
        resultsPerPageSpinner.setModel(new SpinnerNumberModel(resultsPerPagePref, resultsPerPageMin, resultsPerPageMax, 1));
        resultsPerPageSpinner.setPreferredSize(new Dimension(80, 25));
        resultsPerPageSpinner.setToolTipText(Earthdata_Cloud_Controller.PROPERTY_FETCH_RESULTS_PER_PAGE_TOOLTIP);

        paginationPanel.add(resultsPerPageSpinner);
        paginationPanel.add(Box.createHorizontalStrut(60));

        return paginationPanel;
    }
} 