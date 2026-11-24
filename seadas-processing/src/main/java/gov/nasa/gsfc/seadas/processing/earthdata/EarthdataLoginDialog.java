package gov.nasa.gsfc.seadas.processing.earthdata;

import gov.nasa.gsfc.seadas.processing.ocssw.OCSSWDockerStarter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

/**
 * Asks for the NASA Earthdata Login, checks it with Earthdata and saves it in ~/.netrc.
 */
public final class EarthdataLoginDialog {

    public static final String TITLE = "Earthdata Login";

    private static final Color ERROR_COLOR = new Color(190, 30, 30);

    private final JDialog dialog;
    private final JTextField userField = new JTextField(24);
    private final JPasswordField passwordField = new JPasswordField(24);
    private final JLabel status = new JLabel(" ");
    private final JButton saveButton = new JButton("Save");
    private final JButton cancelButton = new JButton("Cancel");
    private boolean saved;

    private EarthdataLoginDialog(Component parent, String reason) {
        Window owner = parent == null ? null
                : parent instanceof Window ? (Window) parent : SwingUtilities.getWindowAncestor(parent);
        dialog = new JDialog(owner, TITLE, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        String savedUser = EarthdataLogin.getSavedUserName();
        if (savedUser != null) {
            userField.setText(savedUser);
        }

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 10, 0);

        String intro = (reason == null ? "" : reason + "<br><br>")
                + "Enter your NASA Earthdata Login. SeaDAS checks it with Earthdata and saves it in "
                + NetrcFile.inHome().getPath() + ", where SeaDAS and the processors (OCSSW) read it.";
        panel.add(new JLabel("<html><body style='width: 380px'>" + intro + "</body></html>"), c);

        c.gridy++;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(4, 0, 4, 8);
        panel.add(new JLabel("User name:"), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        panel.add(userField, c);

        c.gridy++;
        c.gridx = 0;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
        panel.add(new JLabel("Password:"), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(passwordField, c);

        c.gridy++;
        JCheckBox showPassword = new JCheckBox("Show password");
        char echo = passwordField.getEchoChar();
        showPassword.addActionListener(e -> passwordField.setEchoChar(showPassword.isSelected() ? (char) 0 : echo));
        panel.add(showPassword, c);

        c.gridy++;
        JPanel links = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        links.add(link("Create an account", EarthdataLogin.REGISTER_URL));
        links.add(new JLabel("   "));
        links.add(link("Forgot your password?", EarthdataLogin.RESET_PASSWORD_URL));
        panel.add(links, c);

        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 2;
        c.insets = new Insets(10, 0, 0, 0);
        panel.add(status, c);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(saveButton);
        buttons.add(cancelButton);
        saveButton.addActionListener(e -> checkAndSave());
        cancelButton.addActionListener(e -> dialog.dispose());
        dialog.getRootPane().setDefaultButton(saveButton);

        dialog.getContentPane().add(panel, BorderLayout.CENTER);
        dialog.getContentPane().add(buttons, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        if (savedUser != null) {
            dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowOpened(java.awt.event.WindowEvent e) {
                    passwordField.requestFocusInWindow();
                }
            });
        }
    }

    /**
     * Shows the dialog and waits until it closes. Returns true if a login was saved.
     *
     * @param reason why SeaDAS is asking, shown above the fields; null when the user opened it from the menu
     */
    public static boolean show(Component parent, String reason) {
        EarthdataLoginDialog d = new EarthdataLoginDialog(parent, reason);
        d.dialog.setVisible(true);
        return d.saved;
    }

    /**
     * If no Earthdata Login is saved, explains why it's needed and offers the dialog.
     * Returns true if a login is saved afterwards.
     */
    public static boolean offerIfMissing(Component parent, String reason) {
        if (EarthdataLogin.isSaved()) {
            return true;
        }
        int answer = JOptionPane.showOptionDialog(parent,
                "<html><body style='width: 380px'>" + reason + "<br><br>No Earthdata Login is saved yet. Set it now?</body></html>",
                TITLE, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                new Object[]{"Set Earthdata Login...", "Not Now"}, "Set Earthdata Login...");
        return answer == 0 && show(parent, null);
    }

    private void checkAndSave() {
        String user = userField.getText().trim();
        char[] passwordChars = passwordField.getPassword();
        String password = new String(passwordChars);
        Arrays.fill(passwordChars, '\0');
        try {
            NetrcFile.checkToken("user name", user);
            NetrcFile.checkToken("password", password);
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
            return;
        }

        setBusy(true);
        status.setForeground(UIManager.getColor("Label.foreground"));
        status.setText("Checking the login with Earthdata ...");
        new SwingWorker<EarthdataLogin.Check, Void>() {
            @Override
            protected EarthdataLogin.Check doInBackground() {
                return EarthdataLogin.check(user, password);
            }

            @Override
            protected void done() {
                setBusy(false);
                EarthdataLogin.Check result;
                try {
                    result = get();
                } catch (InterruptedException | ExecutionException e) {
                    result = EarthdataLogin.Check.UNREACHABLE;
                }
                switch (result) {
                    case OK:
                        save(user, password);
                        break;
                    case REJECTED:
                        showError("Earthdata didn't accept this user name and password.");
                        passwordField.requestFocusInWindow();
                        passwordField.selectAll();
                        break;
                    default:
                        int answer = JOptionPane.showConfirmDialog(dialog,
                                "SeaDAS couldn't reach Earthdata to check the login.\nSave it anyway?",
                                TITLE, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                        if (answer == JOptionPane.YES_OPTION) {
                            save(user, password);
                        } else {
                            status.setText(" ");
                        }
                }
            }
        }.execute();
    }

    private void save(String user, String password) {
        try {
            EarthdataLogin.save(user, password);
            saved = true;
            dialog.dispose();
            updateDocker(dialog.getOwner());
        } catch (Exception e) {
            showError("Couldn't save " + NetrcFile.inHome().getPath() + ": " + e.getMessage());
        }
    }

    /** If OCSSW runs in Docker, passes the new login to it in the background. */
    private static void updateDocker(Window owner) {
        Thread thread = new Thread(() -> {
            String problem = OCSSWDockerStarter.updateNetrc();
            if (problem != null) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(owner, problem, TITLE,
                        JOptionPane.WARNING_MESSAGE));
            }
        }, "Earthdata login to OCSSW Docker");
        thread.setDaemon(true);
        thread.start();
    }

    private void showError(String message) {
        status.setForeground(ERROR_COLOR);
        status.setText("<html><body style='width: 380px'>" + message + "</body></html>");
        dialog.pack();
    }

    private void setBusy(boolean busy) {
        saveButton.setEnabled(!busy);
        userField.setEnabled(!busy);
        passwordField.setEnabled(!busy);
        dialog.setCursor(busy ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : null);
    }

    private static JLabel link(String text, String url) {
        JLabel label = new JLabel("<html><a href=''>" + text + "</a></html>");
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        label.setToolTipText(url);
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI(url));
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(label, "Open this page in your web browser:\n" + url, TITLE,
                            JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });
        return label;
    }
}
