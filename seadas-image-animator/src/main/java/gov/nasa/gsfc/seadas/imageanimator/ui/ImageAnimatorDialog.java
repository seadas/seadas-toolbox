package gov.nasa.gsfc.seadas.imageanimator.ui;

import gov.nasa.gsfc.seadas.contour.ui.ExGridBagConstraints;
import gov.nasa.gsfc.seadas.imageanimator.data.ImageAnimatorData;
import gov.nasa.gsfc.seadas.imageanimator.ui.ImageAnimatorFilteredBandAction;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.imgfilter.FilteredBandAction;
import org.esa.snap.rcp.imgfilter.model.Filter;
import org.esa.snap.ui.UIUtils;
import org.esa.snap.ui.product.ProductSceneView;
import org.esa.snap.ui.tool.ToolButtonFactory;
import org.openide.util.HelpCtx;

import javax.help.HelpBroker;
import javax.swing.*;
import javax.swing.event.SwingPropertyChangeSupport;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by IntelliJ IDEA.
 * User: Aynur Abdurazik (aabduraz)
 * Date: 9/9/13
 * Time: 12:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class ImageAnimatorDialog extends JDialog {

    static final String PREF_KEY_AUTO_SHOW_NEW_BANDS = "imageAnimatorLines.autoShowNewBands";

    public static final String TITLE = "Create imageAnimator Lines"; /*I18N*/
    static final String NEW_BAND_SELECTED_PROPERTY = "newBandSelected";
    static final String DELETE_BUTTON_PRESSED_PROPERTY = "deleteButtonPressed";
    static final String NEW_FILTER_SELECTED_PROPERTY = "newFilterSelected";
    static final String FILTER_STATUS_CHANGED_PROPERTY = "filterStatusChanged";
    private ImageAnimatorData imageAnimatorData;
    private Component helpButton = null;
    private HelpBroker helpBroker = null;

    private final static String helpId = "imageAnimatorLinesHelp";
    private final static String HELP_ICON = "icons/Help24.gif";

    private Product product;

    Band selectedBand;
    Band selectedFilteredBand;
    Band selectedUnfilteredBand;
    int numberOfLevels;

    JComboBox bandComboBox;
    ArrayList<ImageAnimatorData> imageAnimators;
    ArrayList<String> activeBands;

    private SwingPropertyChangeSupport propertyChangeSupport;

    JPanel imageAnimatorPanel;
    private boolean imageAnimatorCanceled;
    private String filteredBandName;
    boolean filterBand;

    private double noDataValue;
    RasterDataNode raster;

    private double NULL_DOUBLE = -1.0;
    private double ptsToPixelsMultiplier = NULL_DOUBLE;

    JCheckBox filtered = new JCheckBox("", true);

    public ImageAnimatorDialog(Product product, ArrayList<String> activeBands) {
        super(SnapApp.getDefault().getMainFrame(), TITLE, JDialog.DEFAULT_MODALITY_TYPE);
        this.product = product;

        propertyChangeSupport = new SwingPropertyChangeSupport(this);

        helpButton = getHelpButton();

        ProductSceneView productSceneView = SnapApp.getDefault().getSelectedProductSceneView();
        ProductNodeGroup<Band> bandGroup = product.getBandGroup();

        if (productSceneView != null) {
            ImageInfo selectedImageInfo = productSceneView.getImageInfo();
            Band[] bands = new Band[bandGroup.getNodeCount()];
            bandGroup.toArray(bands);
            for (Band band : bands) {
                if (band.getImageInfo() != null) {
                    if (band.getImageInfo() == selectedImageInfo) {
                        selectedUnfilteredBand = band;
                    }
                }
            }
            selectedBand = getDefaultFilterBand(selectedUnfilteredBand);
            raster = product.getRasterDataNode(selectedUnfilteredBand.getName());


            this.activeBands = activeBands;
            ptsToPixelsMultiplier = getPtsToPixelsMultiplier();
            imageAnimatorData = new ImageAnimatorData(selectedBand, selectedUnfilteredBand.getName(), getFilterShortHandName(), ptsToPixelsMultiplier);
            numberOfLevels = 1;
            imageAnimators = new ArrayList<ImageAnimatorData>();
            propertyChangeSupport.addPropertyChangeListener(NEW_BAND_SELECTED_PROPERTY, getBandPropertyListener());
            propertyChangeSupport.addPropertyChangeListener(DELETE_BUTTON_PRESSED_PROPERTY, getDeleteButtonPropertyListener());

            propertyChangeSupport.addPropertyChangeListener(NEW_FILTER_SELECTED_PROPERTY, getFilterButtonPropertyListener());
            propertyChangeSupport.addPropertyChangeListener(FILTER_STATUS_CHANGED_PROPERTY, getFilterCheckboxPropertyListener());
            noDataValue = selectedBand.getNoDataValue();
            createImageAnimatorUI();
        }
        imageAnimatorCanceled = true;
    }

    private PropertyChangeListener getBandPropertyListener() {
        return new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                for (ImageAnimatorData imageAnimatorData : imageAnimators) {
                    imageAnimatorData.setBand(selectedBand);
                }
            }
        };
    }

    private PropertyChangeListener getFilterButtonPropertyListener() {
        return new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                for (ImageAnimatorData imageAnimatorData : imageAnimators) {

                }
            }
        };
    }

    private PropertyChangeListener getFilterCheckboxPropertyListener() {
        return new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                for (ImageAnimatorData imageAnimatorData : imageAnimators) {

                }
            }
        };
    }

    private PropertyChangeListener getDeleteButtonPropertyListener() {
        return new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                Component[] components = imageAnimatorPanel.getComponents();
                for (Component component : components) {
                    if (component instanceof JPanel) {

                        Component[] jPanelComponents = ((JPanel) component).getComponents();
                        for (Component jPanelComponent : jPanelComponents) {
                            if (component instanceof JPanel && ((JPanel) jPanelComponent).getComponents().length == 0) {
                                ((JPanel) component).remove(jPanelComponent);
                            }
                        }
                    }
                    imageAnimatorPanel.validate();
                    imageAnimatorPanel.repaint();
                }
            }
        };
    }

    @Override
    public void addPropertyChangeListener(String name, PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(name, listener);
    }

    @Override
    public void removePropertyChangeListener(String name, PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(name, listener);
    }


    public SwingPropertyChangeSupport getPropertyChangeSupport() {
        return propertyChangeSupport;
    }

    public void appendPropertyChangeSupport(SwingPropertyChangeSupport propertyChangeSupport) {
        PropertyChangeListener[] pr = propertyChangeSupport.getPropertyChangeListeners();
        for (int i = 0; i < pr.length; i++) {
            this.propertyChangeSupport.addPropertyChangeListener(pr[i]);
        }
    }

    public void clearPropertyChangeSupport() {
        PropertyChangeListener[] pr = propertyChangeSupport.getPropertyChangeListeners();
        for (int i = 0; i < pr.length; i++) {
            this.propertyChangeSupport.removePropertyChangeListener(pr[i]);
        }

    }

    protected AbstractButton getHelpButton() {
        if (helpId != null) {
            final AbstractButton helpButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon(HELP_ICON),
                    false);
            helpButton.setToolTipText("Help.");
            helpButton.setName("helpButton");
            helpButton.addActionListener(e ->getHelpCtx().display());
            return helpButton;
        }

        return null;
    }

    public HelpCtx getHelpCtx() {
        return new HelpCtx(helpId);
    }


    public final JPanel createImageAnimatorUI() {


        final int rightInset = 5;

        imageAnimatorPanel = new JPanel(new GridBagLayout());

        imageAnimatorPanel.setBorder(BorderFactory.createTitledBorder(""));

        final JPanel imageAnimatorContainerPanel = new JPanel(new GridBagLayout());

        final JPanel basicPanel = getImageAnimatorPanel();

        imageAnimatorContainerPanel.add(basicPanel,
                new ExGridBagConstraints(0, 0, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, 5));
        JButton addButton = new JButton("+");
        addButton.setPreferredSize(addButton.getPreferredSize());
        addButton.setMinimumSize(addButton.getPreferredSize());
        addButton.setMaximumSize(addButton.getPreferredSize());
        addButton.setName("addButton");

        addButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                JPanel addedPanel = getImageAnimatorPanel();
                ((JButton) event.getSource()).getParent().add(addedPanel);
                JPanel c = (JPanel) ((JButton) event.getSource()).getParent();
                JPanel jPanel = (JPanel) c.getComponents()[0];
                int numPanels = jPanel.getComponents().length;
                jPanel.add(addedPanel,
                        new ExGridBagConstraints(0, numPanels, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, 5));
                repaint();
                pack();
            }
        });

        imageAnimatorContainerPanel.addPropertyChangeListener(DELETE_BUTTON_PRESSED_PROPERTY, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                Component[] components = imageAnimatorContainerPanel.getComponents();
                for (Component component : components) {
                    if (((JPanel) component).getComponents().length == 0) {
                        imageAnimatorContainerPanel.remove(component);
                    }
                }
                imageAnimatorContainerPanel.validate();
                imageAnimatorContainerPanel.repaint();
            }
        });
        JPanel mainPanel = new JPanel(new GridBagLayout());


        imageAnimatorPanel.add(imageAnimatorContainerPanel,
                new ExGridBagConstraints(0, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, 5));
        imageAnimatorPanel.add(addButton,
                new ExGridBagConstraints(0, 2, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, 5));
        mainPanel.add(getBandPanel(),
                new ExGridBagConstraints(0, 0, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, 5));
        mainPanel.add(imageAnimatorPanel,
                new ExGridBagConstraints(0, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, 5));

        mainPanel.add(getControllerPanel(),
                new ExGridBagConstraints(0, 2, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, 5));

        add(mainPanel);

        //this will set the "Create imageAnimator Lines" button as a default button that listens to the Enter key
        mainPanel.getRootPane().setDefaultButton((JButton) ((JPanel) mainPanel.getComponent(2)).getComponent(2));
        setModalityType(ModalityType.APPLICATION_MODAL);
        setTitle("Create imageAnimator Lines");
        //setTitle("imageAnimator Lines for " + selectedBand.getName() );
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        pack();
        return mainPanel;
    }

    private JPanel getImageAnimatorPanel() {
        return new JPanel();
    }

    /**
     * By default a band should be filtered before running the imageAnimator algorithm on it.
     *
     * @return
     */
    private JPanel getBandPanel() {
        final int rightInset = 5;

        final JPanel bandPanel = new JPanel(new GridBagLayout());
        JLabel bandLabel = new JLabel("Product: ");
        final JTextArea filterMessage = new JTextArea("Using filter " + getFilterShortHandName());

        Collections.swap(activeBands, 0, activeBands.indexOf(selectedUnfilteredBand.getName()));
        //bandComboBox = new JComboBox(activeBands.toArray());
        bandComboBox = new JComboBox(new String[]{selectedUnfilteredBand.getName()});
        bandComboBox.setSelectedItem(selectedBand.getName());
        //bandComboBox.setEnabled(false);
        bandComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                String oldBandName = selectedBand.getName();
                if (filterBand) {
                    product.getBandGroup().remove(product.getBand(selectedBand.getName()));
                }
                selectedUnfilteredBand = product.getBand((String) bandComboBox.getSelectedItem());
                //selectedBand = selectedUnfilteredBand;
                product.getRasterDataNode(oldBandName);
                selectedBand = getDefaultFilterBand(product.getRasterDataNode(oldBandName));
                filtered.setSelected(true);
                filterBand = true;
                filterMessage.setText("Using filter " + getFilterShortHandName());
                //raster = product.getRasterDataNode(selectedUnfilteredBand.getName());
                propertyChangeSupport.firePropertyChange(NEW_BAND_SELECTED_PROPERTY, oldBandName, selectedBand.getName());
                noDataValue = selectedBand.getGeophysicalNoDataValue();
            }
        });

        final JButton filterButton = new JButton("Choose Filter");
        final JCheckBox filtered = new JCheckBox("", true);

        filterMessage.setBackground(Color.lightGray);
        filterMessage.setEditable(false);

        filterButton.addActionListener(new ActionListener() {
            SnapApp snapApp = SnapApp.getDefault();

            @Override
            public void actionPerformed(ActionEvent e) {
                Band currentFilteredBand = selectedFilteredBand;

                SnapApp.getDefault().getPreferences().put(PREF_KEY_AUTO_SHOW_NEW_BANDS, "false");
                FilteredBandAction filteredBandAction = new FilteredBandAction();
                //VisatApp.getApp().setSelectedProductNode(selectedUnfilteredBand);
                if (selectedFilteredBand != null) {
                    product.getBandGroup().remove(product.getBand(selectedFilteredBand.getName()));
                }
                filteredBandAction.actionPerformed(e);
                updateActiveBandList();
                SnapApp.getDefault().getPreferences().put(PREF_KEY_AUTO_SHOW_NEW_BANDS, "true");
                if (filterBand) {
                    filterMessage.setText("Using filter " + getFilterShortHandName());
                    filtered.setSelected(true);
                } else {
                    if (currentFilteredBand != null) {
                        product.getBandGroup().add(currentFilteredBand);
                    }
                }
                propertyChangeSupport.firePropertyChange(NEW_FILTER_SELECTED_PROPERTY, true, false);
            }
        });

        filtered.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (filtered.isSelected()) {
                    //SnapApp.getDefault().setSelectedProductNode(selectedUnfilteredBand);
                    selectedBand = getDefaultFilterBand(selectedBand);
                    selectedFilteredBand = selectedBand;
                    filterMessage.setText("Using filter " + getFilterShortHandName());
                    filterBand = true;
                } else {
                    //VisatApp.getApp().setSelectedProductNode(selectedUnfilteredBand);
                    selectedBand = selectedUnfilteredBand;
                    filterMessage.setText("Not filtered");
                    if (selectedFilteredBand != null) {
                        product.getBandGroup().remove(product.getBand(selectedFilteredBand.getName()));
                    }
                    selectedFilteredBand = null;
                    filterBand = false;
                }
                propertyChangeSupport.firePropertyChange(FILTER_STATUS_CHANGED_PROPERTY, true, false);
            }
        });
        JLabel filler = new JLabel("     ");


        JTextArea  productTextArea = new JTextArea(bandComboBox.getSelectedItem().toString());
        productTextArea.setBackground(Color.lightGray);
        productTextArea.setEditable(false);


        bandPanel.add(filler,
                new ExGridBagConstraints(0, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE));
        bandPanel.add(bandLabel,
                new ExGridBagConstraints(1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE));
        bandPanel.add(productTextArea,
                new ExGridBagConstraints(2, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, rightInset)));
        bandPanel.add(filterButton,
                new ExGridBagConstraints(3, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, rightInset)));
        bandPanel.add(filtered,
                new ExGridBagConstraints(4, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, rightInset)));
        bandPanel.add(filterMessage,
                new ExGridBagConstraints(5, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, rightInset)));
        return bandPanel;
    }

    private void updateActiveBandList() {

        Band[] bands = product.getBands();
        filterBand = false;
        for (Band band : bands) {
            //the image info of the filteredBand of the current band is null; this is to avoid selecting other filter bands and setting them to null
            if (band.getName().contains(selectedUnfilteredBand.getName()) && band.getName().length() > selectedUnfilteredBand.getName().length() && band.equals(bands[bands.length - 1])) {
                selectedBand = band;
                selectedFilteredBand = band;
                filteredBandName = band.getName();
                filterBand = true;
                noDataValue = selectedBand.getNoDataValue();
            }
        }

    }

    private JPanel getControllerPanel() {
        JPanel controllerPanel = new JPanel(new GridBagLayout());

        JButton createimageAnimatorLines = new JButton("Create imageAnimator Lines");
        createimageAnimatorLines.setPreferredSize(createimageAnimatorLines.getPreferredSize());
        createimageAnimatorLines.setMinimumSize(createimageAnimatorLines.getPreferredSize());
        createimageAnimatorLines.setMaximumSize(createimageAnimatorLines.getPreferredSize());
        createimageAnimatorLines.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
//                if (imageAnimatorData.getimageAnimatorIntervals().size() == 0) {
//                    imageAnimatorData.createimageAnimatorLevels(getMinValue(), getMaxValue(), getNumberOfLevels(), logCheckBox.isSelected());
//                }
                imageAnimatorCanceled = false;
                dispose();
            }
        });
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setPreferredSize(cancelButton.getPreferredSize());
        cancelButton.setMinimumSize(cancelButton.getPreferredSize());
        cancelButton.setMaximumSize(cancelButton.getPreferredSize());
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                imageAnimatorCanceled = true;
                dispose();
            }
        });


        JLabel filler = new JLabel("                                        ");

        controllerPanel.add(filler,
                new ExGridBagConstraints(0, 0, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE));
        controllerPanel.add(cancelButton,
                new ExGridBagConstraints(1, 0, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE));
        controllerPanel.add(createimageAnimatorLines,
                new ExGridBagConstraints(3, 0, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE));
        controllerPanel.add(helpButton,
                new ExGridBagConstraints(5, 0, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE));
        return controllerPanel;
    }
    public ImageAnimatorData getImageAnimatorData() {

        return getImageAnimatorData(imageAnimators);
    }

    public ImageAnimatorData getImageAnimatorData(ArrayList<ImageAnimatorData> imageAnimators) {
        ImageAnimatorData mergedImageAnimatorData = new ImageAnimatorData(selectedBand, selectedUnfilteredBand.getName(), getFilterShortHandName(), ptsToPixelsMultiplier);
        return mergedImageAnimatorData;
    }


    public boolean isimageAnimatorCanceled() {
        return imageAnimatorCanceled;
    }

    public void setimageAnimatorCanceled(boolean imageAnimatorCanceled) {
        this.imageAnimatorCanceled = imageAnimatorCanceled;
    }

    public String getFilteredBandName() {
        return filteredBandName;
    }

    private ActionEvent getFilterActionEvent(FilteredBandAction filteredBandAction, ActionEvent actionEvent) {
        ActionEvent filterCommandEvent = new ActionEvent(filteredBandAction, 1,null, 0);
        return filterCommandEvent;
    }

    private FilterBand getDefaultFilterBand(RasterDataNode rasterDataNode) {
//        Filter defaultFilter = new Filter("Mean 2.5 Pixel Radius", "amc_2.5px", 5, 5, new double[]{
//                0.172, 0.764, 1, 0.764, 0.172,
//                0.764, 1, 1, 1, 0.764,
//                1, 1, 1, 1, 1,
//                0.764, 1, 1, 1, 0.764,
//                0.172, 0.764, 1, 0.764, 0.172,
//        }, 19.8);

        Filter defaultFilter =new Filter("Arithmetic Mean 5x5", "am5", 5, 5, new double[]{
                           +1, +1, +1, +1, +1,
                           +1, +1, +1, +1, +1,
                           +1, +1, +1, +1, +1,
                           +1, +1, +1, +1, +1,
                           +1, +1, +1, +1, +1,
                   }, 25.0);

        ImageAnimatorFilteredBandAction imageAnimatorFilteredBandAction = new ImageAnimatorFilteredBandAction();
        final FilterBand filteredBand = ImageAnimatorFilteredBandAction.getFilterBand(rasterDataNode,  selectedUnfilteredBand.getName() + "_am5", defaultFilter,1);
        filterBand = true;
        selectedFilteredBand = filteredBand;
        filteredBandName = filteredBand.getName();
        return filteredBand;
    }

    public double getNoDataValue() {
        return noDataValue;
    }

    public void setNoDataValue(double noDataValue) {
        this.noDataValue = noDataValue;
    }


    private double getPtsToPixelsMultiplier() {
        if (ptsToPixelsMultiplier == NULL_DOUBLE) {
            final double PTS_PER_INCH = 72.0;
            final double PAPER_HEIGHT = 11.0;
            final double PAPER_WIDTH = 8.5;

            double heightToWidthRatioPaper = (PAPER_HEIGHT) / (PAPER_WIDTH);
            double heightToWidthRatioRaster = raster.getRasterHeight() / raster.getRasterWidth();

            if (heightToWidthRatioRaster > heightToWidthRatioPaper) {
                // use height
                ptsToPixelsMultiplier = (1 / PTS_PER_INCH) * (raster.getRasterHeight() / (PAPER_HEIGHT));
            } else {
                // use width
                ptsToPixelsMultiplier = (1 / PTS_PER_INCH) * (raster.getRasterWidth() / (PAPER_WIDTH));
            }
        }

        return ptsToPixelsMultiplier;
    }

    private String getFilterShortHandName() {
        if (selectedFilteredBand == null) {
            return "not_filtered";
        }
        String selectedUnfilteredBandName = selectedUnfilteredBand.getName();
        String selectedFilteredBandName = selectedFilteredBand.getName();
        return selectedFilteredBandName.substring(selectedUnfilteredBandName.length() + 1);
    }
}
