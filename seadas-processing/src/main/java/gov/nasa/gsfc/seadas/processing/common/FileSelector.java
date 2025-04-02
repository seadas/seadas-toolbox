package gov.nasa.gsfc.seadas.processing.common;

import gov.nasa.gsfc.seadas.processing.core.ParamInfo;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.SnapFileChooser;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.SwingPropertyChangeSupport;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

/**
 * Created by IntelliJ IDEA.
 *
 * @author knowles
 * @author aabduraz Date: 5/25/12 Time: 10:29 AM To change this template use
 * File | Settings | File Templates.
 */
public class FileSelector {

    public static final String PROPERTY_KEY_APP_LAST_OPEN_DIR = "app.file.lastOpenDir";

    private JPanel jPanel = new JPanel(new GridBagLayout());
    private boolean isDir = false;

//    public enum Type {
//        IFILE,
//        OFILE,
//        DIR
//    }
    private String propertyName = "FILE_SELECTOR_PANEL_CHANGED";

    private AppContext appContext;

    private ParamInfo.Type type;
    private String name;
    private JLabel nameLabel;
    private JTextField fileTextfield;

    private JButton fileChooserButton;

    private File currentDirectory;

    private RegexFileFilter regexFileFilter;
    private JTextField filterRegexField;
    private JLabel filterRegexLabel;

    boolean putLabelAtTop = false;

    private String currentFilename = null;

    boolean fireTextFieldEnabled = true;

    private final JPanel filterPane = new JPanel(new GridBagLayout());

    private final SwingPropertyChangeSupport propertyChangeSupport = new SwingPropertyChangeSupport(this);

    public FileSelector(AppContext appContext) {
        this(appContext, null, false);
    }

    public FileSelector(AppContext appContext, ParamInfo.Type type, boolean putLabelAtTop) {
        this.appContext = appContext;
        this.putLabelAtTop = putLabelAtTop;
        setType(type);

        initComponents();
        addComponents();
    }

    public FileSelector(AppContext appContext, ParamInfo.Type type) {
        this(appContext, type, false);
    }

    public FileSelector(AppContext appContext, ParamInfo.Type type, String name) {
        this(appContext, type, false);
        setName(name);
    }

    public FileSelector(AppContext appContext, ParamInfo.Type type, String name, boolean putLabelAtTop) {
        this(appContext, type, putLabelAtTop);
        setName(name);
    }

    private void initComponents() {

        fileTextfield = createFileTextfield();

        if (putLabelAtTop) {
            nameLabel = new JLabel("  " + name);
        } else {
            nameLabel = new JLabel(name);
        }

        fileChooserButton = new JButton(new FileChooserAction());

    }

    private void addComponents() {
        if (name == null) {
            nameLabel.setVisible(false);
        }

        if (putLabelAtTop) {
            jPanel.add(nameLabel,
                    new GridBagConstraintsCustom(0, 0, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, 0));

            jPanel.add(fileTextfield,
                    new GridBagConstraintsCustom(0, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 0));
            jPanel.add(fileChooserButton,
                    new GridBagConstraintsCustom(1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, 0));

            jPanel.add(filterPane,
                    new GridBagConstraintsCustom(2, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, 0));
        } else {
            jPanel.add(nameLabel,
                    new GridBagConstraintsCustom(0, 0, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, 2));

            jPanel.add(fileTextfield,
                    new GridBagConstraintsCustom(1, 0, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 2));
            jPanel.add(fileChooserButton,
                    new GridBagConstraintsCustom(2, 0, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, 2));

            jPanel.add(filterPane,
                    new GridBagConstraintsCustom(3, 0, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, 2));

        }

        if (type != ParamInfo.Type.IFILE) {
            filterPane.setVisible(false);
        }
    }

    public void setEnabled(boolean enabled) {
        nameLabel.setEnabled(enabled);
        fileChooserButton.setEnabled(enabled);
        fileTextfield.setEnabled(enabled);

        if (type == ParamInfo.Type.IFILE) {
            filterRegexField.setEnabled(enabled);
            filterRegexLabel.setEnabled(enabled);
        }
    }

    public void setVisible(boolean visible) {
        jPanel.setVisible(visible);
        nameLabel.setVisible(visible);
        fileChooserButton.setVisible(visible);
        fileTextfield.setVisible(visible);

        if (type == ParamInfo.Type.IFILE) {
            filterRegexField.setVisible(visible);
            filterRegexLabel.setVisible(visible);
        }
    }

    public void setName(String name) {
        this.name = name;
        if (putLabelAtTop) {
            nameLabel.setText("  " + name);
        } else {
            nameLabel.setText(name);
        }
        if (name != null && name.length() > 0) {
            nameLabel.setVisible(true);
        }
    }

    public String getFileName() {
        return fileTextfield.getText();
    }

    public JTextField getFileTextField() {
        return fileTextfield;
    }

    public void setFilename(String filename) {
            fireTextFieldEnabled = false;
            currentFilename = filename;
            fileTextfield.setText(filename);
            fireTextFieldEnabled = true;
    }


    public void setFilenameAndFire(String filename) {
        String tmpCurrentFilename = currentFilename;
        setFilename(filename);
        fireEvent(propertyName, tmpCurrentFilename, filename);
    }




    private void handleFileTextfield() {

        String newFilename = fileTextfield.getText();

        boolean filenameChanged = false;
        if (newFilename != null) {
            if (!newFilename.equals(currentFilename)) {
                filenameChanged = true;
            }
        } else {
            if (currentFilename != null) {
                filenameChanged = true;
            }
        }

        if (filenameChanged) {
            fileTextfield.setFocusable(true);
            fileTextfield.validate();
            fileTextfield.repaint();
            String previousFilename = currentFilename;
            currentFilename = newFilename;
            fireEvent(propertyName, previousFilename, newFilename);
        }
    }

    public JTextField createFileTextfield() {

        final JTextField jTextField = new JTextField();

        jTextField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (fireTextFieldEnabled) {
                    handleFileTextfield();
                }
            }
        });

        jTextField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (fireTextFieldEnabled) {
                    handleFileTextfield();
                }
            }
        });

        return jTextField;
    }

    private void createFilterPane(JPanel mainPanel) {

        mainPanel.removeAll();

        filterRegexField = new JTextField("123456789 ");
        filterRegexField.setPreferredSize(filterRegexField.getPreferredSize());
        filterRegexField.setMinimumSize(filterRegexField.getPreferredSize());
        filterRegexField.setMaximumSize(filterRegexField.getPreferredSize());
        filterRegexField.setText("");

        filterRegexField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent documentEvent) {
                //To change body of implemented methods use File | Settings | File Templates.
                regexFileFilter = new RegexFileFilter(filterRegexField.getText());
                SeadasFileUtils.debug(regexFileFilter.getDescription());
            }

            @Override
            public void removeUpdate(DocumentEvent documentEvent) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void changedUpdate(DocumentEvent documentEvent) {
                //To change body of implemented methods use File | Settings | File Templates.
            }
        });

        filterRegexLabel = new JLabel("filter:");
        filterRegexLabel.setPreferredSize(filterRegexLabel.getPreferredSize());
        filterRegexLabel.setMinimumSize(filterRegexLabel.getPreferredSize());
        filterRegexLabel.setMaximumSize(filterRegexLabel.getPreferredSize());
        filterRegexLabel.setToolTipText("Filter the chooser by regular expression");

        mainPanel.add(filterRegexLabel,
                new GridBagConstraintsCustom(0, 0, 1, 0, GridBagConstraints.EAST, GridBagConstraints.NONE));
        mainPanel.add(filterRegexField,
                new GridBagConstraintsCustom(1, 0, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE));

    }

    public ParamInfo.Type getType() {
        return type;
    }

    public void setType(ParamInfo.Type type) {
        this.type = type;
        if (type == ParamInfo.Type.IFILE) {
            regexFileFilter = new RegexFileFilter();
            createFilterPane(filterPane);
            filterPane.setVisible(true);
        } else if (type == ParamInfo.Type.OFILE) {
            regexFileFilter = null;
            filterPane.removeAll();
            filterPane.setVisible(false);
        } else if (type == ParamInfo.Type.DIR) {
            regexFileFilter = null;
            filterPane.removeAll();
            filterPane.setVisible(false);
            isDir = true;
        }
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    private class FileChooserAction extends AbstractAction {

        private String APPROVE_BUTTON_TEXT = "Select";
        private JFileChooser fileChooser;



        private FileChooserAction() {
            super("...");
            fileChooser = new SnapFileChooser();

            fileChooser.setDialogTitle("Select Input File");

            fileChooser.setAcceptAllFileFilterUsed(true);
            fileChooser.setFileFilter(fileChooser.getAcceptAllFileFilter());
            if (isDir) {
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            }
        }

//        private FileChooserAction(String dialogTitle) {
//            super("...");
//            fileChooser = new SnapFileChooser();
//
//            fileChooser.setDialogTitle(dialogTitle);
//
//            fileChooser.setAcceptAllFileFilterUsed(true);
//            fileChooser.setFileFilter(fileChooser.getAcceptAllFileFilter());
//        }

        @Override
        public void actionPerformed(ActionEvent event) {
            final Window window = SwingUtilities.getWindowAncestor((JComponent) event.getSource());

            String homeDirPath = SystemUtils.getUserHomeDir().getPath();
            String openDir = appContext.getPreferences().getPropertyString(PROPERTY_KEY_APP_LAST_OPEN_DIR,
                    homeDirPath);
            currentDirectory = new File(openDir);
            fileChooser.setCurrentDirectory(currentDirectory);

            if (type == ParamInfo.Type.IFILE) {
                fileChooser.addChoosableFileFilter(regexFileFilter);
            }

            if (fileChooser.showDialog(window, APPROVE_BUTTON_TEXT) == JFileChooser.APPROVE_OPTION) {
                final File file = fileChooser.getSelectedFile();
                currentDirectory = fileChooser.getCurrentDirectory();
                appContext.getPreferences().setPropertyString(PROPERTY_KEY_APP_LAST_OPEN_DIR,
                        currentDirectory.getAbsolutePath());

                String filename = null;
                if (file != null) {
                    filename = file.getAbsolutePath();
                }

                setFilenameAndFire(filename);
            }
        }

    }

    private class RegexFileFilter extends FileFilter {

        private String regex;

        public RegexFileFilter() {
            this(null);
        }

        public RegexFileFilter(String regex) throws IllegalStateException {
            if (regex == null || regex.trim().length() == 0) {

                //throw new IllegalStateException();
                return;
            }

            this.regex = ".*" + regex + ".*";

        }

        /* (non-Javadoc)
        * @see java.io.FileFilter#accept(java.io.File)
         */
        public boolean accept(File pathname) {

            if (regex == null) {
                return true;
            }
            return (pathname.isFile() && pathname.getName().matches(this.regex));
        }

        public String getDescription() {
            return "Files matching regular expression: '" + regex + "'";
        }
    }

    public JPanel getjPanel() {
        return jPanel;
    }

    public JLabel getNameLabel() {
        return nameLabel;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(propertyName, listener);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(propertyName, listener);
    }

    public void fireEvent(String propertyName, Object oldValue, Object newValue) {
        propertyChangeSupport.firePropertyChange(new PropertyChangeEvent(this, propertyName, oldValue, newValue));
    }
}
