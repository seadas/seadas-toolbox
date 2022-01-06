package gov.nasa.gsfc.seadas.processing.utilities;

import javax.swing.*;
import java.awt.*;

public class ScrolledPane extends JFrame {
    private JScrollPane scrollPane;

    public ScrolledPane(String programName, String message, Window window) {
        setTitle(programName);
        setSize(500, 500);
        setBackground(Color.gray);
        setLocationRelativeTo(window);
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout());
        getContentPane().add(topPanel);
        JTextArea text = new JTextArea(message);
        scrollPane = new JScrollPane();
        scrollPane.getViewport().add(text);
        topPanel.add(scrollPane, BorderLayout.CENTER);
    }

    public ScrolledPane(String programName, String message) {
        Window activeWindow = javax.swing.FocusManager.getCurrentManager().getActiveWindow();
        setTitle(programName);
        setSize(500, 500);
        setBackground(Color.gray);
        setLocationRelativeTo(activeWindow);
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout());
        getContentPane().add(topPanel);
        JTextArea text = new JTextArea(message);
        scrollPane = new JScrollPane();
        scrollPane.getViewport().add(text);
        topPanel.add(scrollPane, BorderLayout.CENTER);
    }

}
