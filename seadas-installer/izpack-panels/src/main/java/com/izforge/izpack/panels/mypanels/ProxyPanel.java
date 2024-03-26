import com.izforge.izpack.api.adaptator.IXMLElement;

import com.izforge.izpack.api.data.Panel;
import com.izforge.izpack.api.resource.Resources;
import com.izforge.izpack.gui.IzPanelLayout;
import static com.izforge.izpack.gui.LayoutConstants.NEXT_LINE;
import com.izforge.izpack.gui.log.Log;
import com.izforge.izpack.installer.data.GUIInstallData;
import com.izforge.izpack.installer.gui.InstallerFrame;
import com.izforge.izpack.installer.gui.IzPanel;
import com.nellarmonia.commons.utils.NellCipher;
import java.awt.Insets;
import java.awt.LayoutManager2;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * A custom panel representing a proxy configuration.
 *
 * @author Samir Hadzic
 */
public class ProxyPanel extends IzPanel {


    static final List<String> ENTRIES = new ArrayList();
    static final String USE_PROXY = "client.useproxy";
    static final String USE_DEFAULT_CONFIG = "client.proxyusedefaultconfiguration";
    static final String PROXY_HOST = "client.proxyhost";
    static final String PROXY_PORT = "client.proxyport";
    static final String PROXY_PORT_ADDIN = "client.proxyportaddin";
    static final String USE_CREDENTIALS = "client.proxyusecredentials";
    static final String USE_SHUTTLE_CREDENTIALS = "client.proxyuseshuttlecredentials";
    static final String PROXY_LOGIN = "client.proxylogin";
    static final String PROXY_PASSWORD = "client.proxypassword";

    static {
        ENTRIES.add(USE_PROXY);
        ENTRIES.add(USE_DEFAULT_CONFIG);
        ENTRIES.add(PROXY_HOST);
        ENTRIES.add(PROXY_PORT);
        ENTRIES.add(USE_CREDENTIALS);
        ENTRIES.add(USE_SHUTTLE_CREDENTIALS);
        ENTRIES.add(PROXY_LOGIN);
        ENTRIES.add(PROXY_PASSWORD);
/**
 * Creates an installation record for unattended installations on
 * {@link UserInputPanel}, created during GUI installations.
 *
 * @param rootElement
 */
        @Override
        public void createInstallationRecord(IXMLElement rootElement) {
            new ProxyPanelAutomationHelper().createInstallationRecord(installData, rootElement);

        }

        /**
         * Configure the first part by adding the proxy configuration.
         */
        private void configureProxySettings() {
            defaultProxy = new JRadioButton("Use default proxy server");

            //Default proxy must be selected
            defaultProxy.setSelected(true);

            defaultProxy.setMargin(indentedInset);
            followingProxy = new JRadioButton("Use following proxy server");
            followingProxy.setMargin(indentedInset);
            ButtonGroup buttonGroup = new ButtonGroup();
            buttonGroup.add(defaultProxy);
            buttonGroup.add(followingProxy);

            checkboxProxy = new JCheckBox("Use proxy server");

            //If we are on this panel, we want to use a proxy.
            checkboxProxy.setSelected(true);

            checkboxProxy.setMargin(new Insets(50, 70, 0, 0));
            checkboxProxy.addChangeListener(uiChangeListener);

            followingProxy.addChangeListener(uiChangeListener);
            add(checkboxProxy, NEXT_LINE);
            add(defaultProxy, NEXT_LINE);
            add(followingProxy, NEXT_LINE);
            JLabel hostLabel = new JLabel("Host");

            hostLabel.setBorder(paddingBorder);
            hostField = new JTextField(TEXTFIELD_LENGTH);

            JLabel portLabel = new JLabel("Port");
            portLabel.setBorder(paddingBorder);
            portField = new JTextField(TEXTFIELD_LENGTH);

            add(hostLabel, NEXT_LINE);
            add(hostField, NEXT_COLUMN);
            add(portLabel, NEXT_LINE);
            add(portField, NEXT_COLUMN);
        }

        /**
         * Configure the second part by adding all the credentials configuration.
         */
        private void configureProxyCredentials() {
            followingCredentials = new JRadioButton("Use the following credentials");
            followingCredentials.setMargin(indentedInset);

            followingCredentials.setSelected(true);

            installData.setVariable(USE_PROXY, Boolean.toString(checkboxProxy.isSelected()));
            installData.setVariable(USE_DEFAULT_CONFIG, Boolean.toString(defaultProxy.isSelected()));
            if (followingProxy.isSelected()) {
                installData.setVariable(PROXY_HOST, hostField.getText());
                installData.setVariable(PROXY_PORT, portField.getText());
                if (!portField.getText().isEmpty()) {
                    installData.setVariable(PROXY_PORT_ADDIN, portField.getText());
                }
            }

            //Proxy credentials
            installData.setVariable(USE_CREDENTIALS, Boolean.toString(checkboxCredentials.isSelected()));
            if (checkboxCredentials.isSelected()) {
                installData.setVariable(USE_SHUTTLE_CREDENTIALS, Boolean.toString(shuttleCredentials.isSelected()));
                if (followingCredentials.isSelected()) {
                    installData.setVariable(PROXY_LOGIN, loginField.getText());
                    //We encrypt the password.
                    installData.setVariable(PROXY_PASSWORD, NellCipher.encryptWithMarker(new String(passwordField.getPassword()), true));

                }
            }
        }

        /**
         * The panel is consider valid only if the passwords are equals.
         *
         * @return
         */
        @Override
        public boolean isValidated() {
            if (!Arrays.equals(passwordField.getPassword(), retypePasswordField.getPassword())) {
                emitWarning("Password mismatch", "Both passwords must match.");
                return false;
            }
            return true;
        }
    }