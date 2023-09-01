package gov.nasa.gsfc.seadas.imageanimator.ui;

import gov.nasa.gsfc.seadas.imageanimator.data.ImageAnimatorData;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.ui.UIUtils;
import org.esa.snap.ui.product.ProductSceneView;
import org.esa.snap.ui.tool.ToolButtonFactory;
import org.openide.util.HelpCtx;

import javax.swing.*;
import javax.swing.event.SwingPropertyChangeSupport;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Hashtable;

/**
 * Created by IntelliJ IDEA.
 * User: Aynur Abdurazik (aabduraz)
 * Date: 3/9/23
 * Time: 12:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class ImageAnimatorDialog extends JDialog {

    public static final String TITLE = "Choose Images to Animate"; /*I18N*/

    static final String NEW_BAND_SELECTED_PROPERTY = "newBandSelected";
    static final String DELETE_BUTTON_PRESSED_PROPERTY = "deleteButtonPressed";
    private Component helpButton = null;
    private final static String helpId = "imageAnimatorLinesHelp";
    private final static String HELP_ICON = "icons/Help24.gif";

    private Product product;

    Band selectedBand;
    RasterDataNode raster;
    ArrayList<ImageAnimatorData> imageAnimators;
    ArrayList<String> activeBands;

    private SwingPropertyChangeSupport propertyChangeSupport;

    JPanel imageAnimatorPanel;
    JPanel angularViewAnimatorPanel;
    JCheckBoxTree bandNamesTree;

    JRadioButton bandImages = new JRadioButton("Band Images",true);
    JComboBox sortMethodJComboBox = new JComboBox();
    JRadioButton angularView = new JRadioButton("Angular View");
    JRadioButton spectrumView = new JRadioButton("Spectrum View");
    ButtonGroup buttonGroup = new ButtonGroup();
    JButton animateImagesButton = new JButton("Animate Images");
    JButton createImagesButton = new JButton("Create Images");

    public static String SORT_BY_WAVELENGTH = "Sort by Wavelength";
    public static String SORT_BY_ANGLE = "Sort by Angle";
    public static String SORT_BY_BANDNAME = "Sort by Band Name";
    public static String DEFAULT_SORT_BY = SORT_BY_BANDNAME;
    String sortMethod;

    private boolean imageAnimatorCanceled;

    public ImageAnimatorDialog(Product product, ArrayList<String> activeBands) {
        super(SnapApp.getDefault().getMainFrame(), TITLE, JDialog.DEFAULT_MODALITY_TYPE);
        this.product = product;

        propertyChangeSupport = new SwingPropertyChangeSupport(this);

        helpButton = getHelpButton();

        ProductSceneView productSceneView = SnapApp.getDefault().getSelectedProductSceneView();
        ProductNodeGroup<Band> bandGroup = product.getBandGroup();

        if (productSceneView != null) {

            Band[] bands = new Band[bandGroup.getNodeCount()];
            bandGroup.toArray(bands);
            this.activeBands = activeBands;

            imageAnimators = new ArrayList<ImageAnimatorData>();
            propertyChangeSupport.addPropertyChangeListener(NEW_BAND_SELECTED_PROPERTY, getBandPropertyListener());
            propertyChangeSupport.addPropertyChangeListener(DELETE_BUTTON_PRESSED_PROPERTY, getDeleteButtonPropertyListener());
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

        //final JPanel basicPanel = getImageAnimatorPanel();
        final JPanel basicPanel = getImageTypePanel();

        imageAnimatorContainerPanel.add(basicPanel,
                new ExGridBagConstraints(0, 0, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, 5));

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

        bandNamesTree = new JCheckBoxTree();
        bandNamesTree.setEditable(true);
        bandNamesTree.setDragEnabled(true);
        bandNamesTree.setDropMode(DropMode.ON_OR_INSERT);
        bandNamesTree.setScrollsOnExpand(true);

        DefaultMutableTreeNode root=new DefaultMutableTreeNode("Bands                              ");
        DefaultMutableTreeNode child;
        String bandName;

        Hashtable<String, DefaultMutableTreeNode> bandHash = new Hashtable<String, DefaultMutableTreeNode>();
        Band[] bands = product.getBands();
        // ArrayList<DefaultMutableTreeNode> folders = new ArrayList<DefaultMutableTreeNode>();

        for (Band band :bands) {
            bandName = band.getName();

            String[] parts = bandName.split("_");

            if (parts.length == 2 && parts[1].matches("\\d+")) { // check if filename matches prefix_number format
                String folderName = parts[0];
                String number = parts[1];

                DefaultMutableTreeNode folder = bandHash.get(folderName);
                if (folder == null) {
                    folder = new DefaultMutableTreeNode(folderName);
                    bandHash.put(folderName, folder);
                    folder.setAllowsChildren(true);
                    root.add(folder);
                }
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(bandName);
                folder.add(node);
            } else {
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(bandName);
                root.add(node);
            }
        }

        DefaultTreeModel model = new DefaultTreeModel(root);
        bandNamesTree.setModel(model);

        imageAnimatorPanel.add(imageAnimatorContainerPanel,
                new ExGridBagConstraints(0, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, 5));
        imageAnimatorPanel.add(new JScrollPane(bandNamesTree),
                new ExGridBagConstraints(1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 100, 230));
        mainPanel.add(imageAnimatorPanel,
                new ExGridBagConstraints(0, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 5));
        mainPanel.add(getControllerPanel(),
                new ExGridBagConstraints(0, 2, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, 5));



        bandNamesTree.addCheckChangeEventListener(new JCheckBoxTree.CheckChangeEventListener() {
            public void checkStateChanged(JCheckBoxTree.CheckChangeEvent event) {
                TreePath[] treePath = bandNamesTree.getCheckedPaths();
                Animation animation = new Animation();
                animation.setSortMethod((String) sortMethodJComboBox.getSelectedItem());
                boolean imageOpened = animation.checkImages(treePath);
                if (!imageOpened) {
                    createImagesButton.setEnabled(true);
                    animateImagesButton.setEnabled(false);
                } else {
                    createImagesButton.setEnabled(false);
                    animateImagesButton.setEnabled(true);
                }
                repaint();
//                System.out.println("event");
//                TreePath[] paths = bandNamesTree.getCheckedPaths();
//                for (TreePath tp : paths) {
//                    for (Object pathPart : tp.getPath()) {
//                        System.out.print(pathPart + ",");
//                    }
//                    System.out.println();
//                }
            }
        });

        add(mainPanel);

        //this will set the "Create imageAnimator Lines" button as a default button that listens to the Enter key
        mainPanel.getRootPane().setDefaultButton((JButton) ((JPanel) mainPanel.getComponent(1)).getComponent(1));
        setModalityType(ModalityType.APPLICATION_MODAL);
        setTitle("Animate Images");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        pack();
        return mainPanel;
    }

    private JPanel getImageTypePanel() {
        final JPanel imageTypePanel = new JPanel(new GridBagLayout());

        JLabel imageTypePanelLable = new JLabel("Select Image Type: ");


        bandImages.setBounds(120, 30, 120, 50);
        angularView.setBounds(120, 30, 120, 50);
        spectrumView.setBounds(120, 30, 120, 50);

        bandImages.setActionCommand("bandImages");
        angularView.setActionCommand("angularView");
        spectrumView.setActionCommand("spectrumView");

        //bandImages.setAction();
        imageTypePanelLable.setBounds(120, 30, 120, 50);

        RadioButtonActionListener actionListener = new RadioButtonActionListener();
//
        bandImages.addActionListener(actionListener);
        angularView.addActionListener(actionListener);
        spectrumView.addActionListener(actionListener);
        sortMethodJComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sortMethod = (String) sortMethodJComboBox.getSelectedItem();
            }
        });
//        bandImages.setForeground(Color.BLUE);
//        bandImages.setBackground(Color.YELLOW);
//        bandImages.setFont(new java.awt.Font("Calibri", Font.BOLD, 16));

        bandImages.setToolTipText("Select this option if you want to animate image for multiple bands");


        sortMethodJComboBox = new JComboBox();
        sortMethodJComboBox.setEditable(false);
        sortMethodJComboBox.addItem(SORT_BY_ANGLE);
        sortMethodJComboBox.addItem(SORT_BY_WAVELENGTH);
        sortMethodJComboBox.addItem(SORT_BY_BANDNAME);
        sortMethodJComboBox.setSelectedItem(DEFAULT_SORT_BY);
        sortMethodJComboBox.setName("sortMethodJComboBox");
        sortMethodJComboBox.setToolTipText("Band sorting for animation");


        imageTypePanel.add(imageTypePanelLable, new ExGridBagConstraints(1, 0, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, 5));
        imageTypePanel.add(bandImages, new ExGridBagConstraints(1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, 5));
        imageTypePanel.add(sortMethodJComboBox, new ExGridBagConstraints(1, 2, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, 5));
        imageTypePanel.add(angularView, new ExGridBagConstraints(1, 3, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, 5));
        imageTypePanel.add(spectrumView,  new ExGridBagConstraints(1, 4, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, 5));


        buttonGroup.add(bandImages);
        buttonGroup.add(angularView);
        buttonGroup.add(spectrumView);
        imageTypePanel.setVisible(true);

        return imageTypePanel;
    }

    public ArrayList<Band> getActiveBands() {
        ProductSceneView productSceneView = SnapApp.getDefault().getSelectedProductSceneView();
        ProductNodeGroup<Band> bandGroup = product.getBandGroup();

        if (productSceneView != null) {
            ImageInfo selectedImageInfo = productSceneView.getImageInfo();
            Band[] bands = new Band[bandGroup.getNodeCount()];
            bandGroup.toArray(bands);
            for (Band band : bands) {
                if (band.getImageInfo() != null) {
                    if (band.getImageInfo() == selectedImageInfo) {
                        selectedBand = band;
                    }
                }
            }
            raster = product.getRasterDataNode(selectedBand.getName());
        }
        return null;
    }

    private JPanel getControllerPanel() {
        JPanel controllerPanel = new JPanel(new GridBagLayout());

        createImagesButton.setEnabled(true);
        createImagesButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if (buttonGroup.getSelection().getActionCommand().equals(bandImages.getActionCommand())) {
                    imageAnimatorCanceled = false;
                    TreePath[] treePath = bandNamesTree.getCheckedPaths();
                    Animation animation = new Animation();
                    animation.setSortMethod((String) sortMethodJComboBox.getSelectedItem());
                    animation.createImages(treePath);
                    animateImagesButton.setEnabled(true);
                    createImagesButton.setEnabled(false);
                    repaint();
                }
            }
        });

        animateImagesButton.setPreferredSize(animateImagesButton.getPreferredSize());
        animateImagesButton.setMinimumSize(animateImagesButton.getPreferredSize());
        animateImagesButton.setMaximumSize(animateImagesButton.getPreferredSize());
        animateImagesButton.setEnabled(false);
        animateImagesButton.addActionListener(new ActionListener() {
                                            public void actionPerformed(ActionEvent event) {
                                                imageAnimatorCanceled = false;
                                                if (buttonGroup.getSelection().getActionCommand().equals(bandImages.getActionCommand())) {
                                                    TreePath[] treePath = bandNamesTree.getCheckedPaths();

                                                    Animation animation = new Animation();
                                                    animation.setSortMethod((String) sortMethodJComboBox.getSelectedItem());
                                                    ImageIcon[] images = animation.openImages(treePath);
                                                    AnimationWithSpeedControl.animate(images);
                                                    animateImagesButton.setEnabled(true);
                                                    createImagesButton.setEnabled(false);
                                                    repaint();
                                                }
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
        controllerPanel.add(createImagesButton,
                new ExGridBagConstraints(1, 0, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE));
        controllerPanel.add(animateImagesButton,
                new ExGridBagConstraints(3, 0, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE));
        controllerPanel.add(cancelButton,
                new ExGridBagConstraints(5, 0, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE));
        controllerPanel.add(helpButton,
                new ExGridBagConstraints(7, 0, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE));
        return controllerPanel;
    }

    class RadioButtonActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent event) {
            JRadioButton button = (JRadioButton) event.getSource();

            if (button == bandImages) {
                
                JDialog bandImageAnimationDialog = new JDialog(SnapApp.getDefault().getMainFrame(), "Band Image Animation", JDialog.DEFAULT_MODALITY_TYPE);

            } else if (button == angularView) {

                // option Windows is selected
                JDialog angularAnimationDialog = new JDialog(SnapApp.getDefault().getMainFrame(), JDialog.DEFAULT_MODALITY_TYPE);

                AngularAnimationTopComponent angularAnimationTopComponent = new AngularAnimationTopComponent();
                angularAnimationDialog.setDefaultCloseOperation(DISPOSE_ON_CLOSE);

                angularAnimationDialog.getContentPane().add(angularAnimationTopComponent);
                //Todo (For Bing) -- decide the size
//                angularAnimationDialog.setPreferredSize(new Dimension(350, 250));
                angularAnimationDialog.setPreferredSize(angularAnimationTopComponent.getPreferredSize());
//                angularAnimationDialog.setMinimumSize(angularAnimationDialog.getPreferredSize());
//                angularAnimationDialog.setMaximumSize(angularAnimationDialog.getPreferredSize());
                angularAnimationDialog.setLocationRelativeTo(null);
                angularAnimationDialog.pack();
                angularAnimationDialog.setVisible(true);

                angularAnimationDialog.dispose();
                bandImages.setSelected(true);

            } else if (button == spectrumView) {

                JDialog spectrumAnimationDialog = new JDialog(SnapApp.getDefault().getMainFrame(), JDialog.DEFAULT_MODALITY_TYPE);

                SpectrumAnimationTopComponent spectrumAnimationTopComponent = new SpectrumAnimationTopComponent();
                spectrumAnimationDialog.setDefaultCloseOperation(DISPOSE_ON_CLOSE);

                spectrumAnimationDialog.getContentPane().add(spectrumAnimationTopComponent);
                //Todo (For Bing) -- decide the size
//                angularAnimationDialog.setPreferredSize(new Dimension(350, 250));
                spectrumAnimationDialog.setPreferredSize(spectrumAnimationTopComponent.getPreferredSize());
//                angularAnimationDialog.setMinimumSize(angularAnimationDialog.getPreferredSize());
//                angularAnimationDialog.setMaximumSize(angularAnimationDialog.getPreferredSize());
                spectrumAnimationDialog.setLocationRelativeTo(null);
                spectrumAnimationDialog.pack();
                spectrumAnimationDialog.setVisible(true);

                spectrumAnimationDialog.dispose();
                bandImages.setSelected(true);

            }
        }
    }

}
