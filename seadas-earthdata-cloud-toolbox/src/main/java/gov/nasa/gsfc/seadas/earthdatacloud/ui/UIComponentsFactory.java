package gov.nasa.gsfc.seadas.earthdatacloud.ui;

import org.jdatepicker.impl.JDatePanelImpl;
import org.jdatepicker.impl.JDatePickerImpl;
import org.jdatepicker.impl.UtilDateModel;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Properties;

public class UIComponentsFactory {

    private static JLabel dateRangeHintLabel = new JLabel();  // Declare as a class member
    private JPanel temporalPanel;
    private static JLabel dateRangeLabel; //
    private static JDatePickerImpl startDatePicker;
    private static JDatePickerImpl endDatePicker;
    private static JTextField minLatField;
    private static JTextField maxLatField;
    private static JTextField minLonField;
    private static JTextField maxLonField;
    public static JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setLayout(new FlowLayout(FlowLayout.LEFT));
        JLabel logoLabel = new JLabel();
        try {
            BufferedImage logoImage = ImageIO.read(UIComponentsFactory.class.getResource("/images/about_seadas.png"));
            Image scaledLogo = logoImage.getScaledInstance(80, -1, Image.SCALE_SMOOTH);
            ImageIcon logoIcon = new ImageIcon(scaledLogo);
            logoLabel.setIcon(logoIcon);
        } catch (IOException e) {
            e.printStackTrace();
        }

        JLabel mainTitle = new JLabel("OB_CLOUD Explorer");
        mainTitle.setFont(new Font("SansSerif", Font.BOLD, 20));

        JLabel subtitle = new JLabel("Powered by NASA's Harmony Search Service");
        subtitle.setFont(new Font("SansSerif", Font.ITALIC, 12));
        subtitle.setForeground(Color.GRAY);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.add(mainTitle);
        textPanel.add(subtitle);
        textPanel.setOpaque(false);

        panel.add(logoLabel, BorderLayout.WEST);
        panel.add(textPanel, BorderLayout.CENTER);

        return panel;
    }


    public static JPanel createFilterPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 10, 0));

        panel.add(createTemporalPanel());
        panel.add(createSpatialPanel());
        panel.add(createDayNightPanel());

        return panel;
    }
    public static JPanel createTemporalPanel() {
        JPanel temporalPanel = new JPanel(new GridBagLayout());
        temporalPanel.setBorder(BorderFactory.createTitledBorder("Temporal Filter"));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 2, 2, 2);
        c.anchor = GridBagConstraints.WEST;

        startDatePicker = createDatePicker();
        endDatePicker = createDatePicker();

        Dimension fieldSize = new Dimension(150, 28);
        startDatePicker.setPreferredSize(fieldSize);
        startDatePicker.setMinimumSize(fieldSize);
        endDatePicker.setPreferredSize(fieldSize);
        endDatePicker.setMinimumSize(fieldSize);

        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.NONE;
        temporalPanel.add(new JLabel("Start Date:"), c);

        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        temporalPanel.add(startDatePicker, c);

        c.gridx = 0;
        c.gridy = 1;
        c.fill = GridBagConstraints.NONE;
        temporalPanel.add(new JLabel("End Date:"), c);

        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        temporalPanel.add(endDatePicker, c);

        dateRangeHintLabel = new JLabel(" "); // Declare this as a field
        dateRangeHintLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        dateRangeHintLabel.setForeground(Color.DARK_GRAY);

        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 2;
        temporalPanel.add(dateRangeHintLabel, c);

        dateRangeLabel = new JLabel("Valid date range: ");
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(5, 5, 5, 5);
        temporalPanel.add(dateRangeLabel, c);

        return temporalPanel;
    }

    public static JPanel createSpatialPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Spatial Filter"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Min Lat:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        minLatField = new JTextField();
        panel.add(minLatField, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(new JLabel("Max Lat:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        maxLatField = new JTextField();
        panel.add(maxLatField, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(new JLabel("Min Lon:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        minLonField = new JTextField();
        panel.add(minLonField, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(new JLabel("Max Lon:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        maxLonField = new JTextField();
        panel.add(maxLonField, gbc);

        return panel;
    }

    public static JPanel createDayNightPanel() {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder("Day/Night Filter"));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JRadioButton dayButton = new JRadioButton("Day");
        JRadioButton nightButton = new JRadioButton("Night");
        JRadioButton bothButton = new JRadioButton("Both", true);

        ButtonGroup group = new ButtonGroup();
        group.add(dayButton);
        group.add(nightButton);
        group.add(bothButton);

        dayButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        nightButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        bothButton.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(Box.createVerticalStrut(5));
        panel.add(dayButton);
        panel.add(Box.createVerticalStrut(5));
        panel.add(nightButton);
        panel.add(Box.createVerticalStrut(5));
        panel.add(bothButton);
        panel.add(Box.createVerticalGlue());


        return panel;
    }
    public static JDatePickerImpl createDatePicker() {
        UtilDateModel model = new UtilDateModel();
        Properties p = new Properties();
        p.put("text.today", "Today");
        p.put("text.month", "Month");
        p.put("text.year", "Year");
        JDatePanelImpl datePanel = new JDatePanelImpl(model, p);
        return new JDatePickerImpl(datePanel, new org.jdatepicker.impl.DateComponentFormatter());
    }

    public static JComboBox<String> createDropdown() {
        JComboBox<String> dropdown = new JComboBox<>();
        dropdown.setPreferredSize(new Dimension(180, 25));
        return dropdown;
    }

    public static JSpinner createResultSpinner(int defaultValue, int min, int max, int step) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(defaultValue, min, max, step));
        spinner.setPreferredSize(new Dimension(80, 25));
        return spinner;
    }

    public static JSpinner createResultSpinner() {
        return createResultSpinner(25, 1, 10000, 1);
    }
    public static JTextField createCoordinateField() {
        JTextField field = new JTextField();
        field.setPreferredSize(new Dimension(150, 25));
        return field;
    }

    public static String getFormattedDate(JDatePickerImpl datePicker) {
        if (datePicker.getModel().getValue() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            return sdf.format(datePicker.getModel().getValue());
        }
        return null;
    }
    public static JSpinner createMaxResultsSpinner() {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(25, 1, 10000, 1));
        Dimension spinnerSize = new Dimension(80, 25);
        spinner.setPreferredSize(spinnerSize);
        return spinner;
    }

    public static JButton createButton(String text, Action action) {
        JButton button = new JButton(text);
        button.setAction(action);
        return button;
    }

    public static JLabel createLabel(String text) {
        return new JLabel(text);
    }
}
