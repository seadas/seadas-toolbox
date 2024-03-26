import com.izforge.izpack.api.adaptator.IXMLElement;
import com.izforge.izpack.api.data.InstallData;
import com.izforge.izpack.installer.console.AbstractConsolePanel;
import com.izforge.izpack.installer.console.ConsolePanel;
import com.izforge.izpack.installer.panel.PanelView;
import com.izforge.izpack.util.Console;
import com.nellarmonia.commons.utils.NellCipher;
import static com.shuttle.panel.ProxyPanel.PROXY_HOST;
import static com.shuttle.panel.ProxyPanel.PROXY_LOGIN;
import static com.shuttle.panel.ProxyPanel.PROXY_PASSWORD;
import static com.shuttle.panel.ProxyPanel.PROXY_PORT;
import static com.shuttle.panel.ProxyPanel.PROXY_PORT_ADDIN;
import static com.shuttle.panel.ProxyPanel.USE_CREDENTIALS;
import static com.shuttle.panel.ProxyPanel.USE_DEFAULT_CONFIG;
import static com.shuttle.panel.ProxyPanel.USE_PROXY;
import static com.shuttle.panel.ProxyPanel.USE_SHUTTLE_CREDENTIALS;
import java.util.Properties;

/**
 * The ProxyPanel console helper class.
 *
 */
public class ProxyConsolePanel extends AbstractConsolePanel {

    private InstallData installData;

    private boolean useProxy = false;
    private boolean useDefaultProxy = true;
    private boolean useFollowingProxy = false;
    private String proxyHost = "";
    private String proxyPort = "";
    private boolean useCredentials = false;
    private boolean useFollowingCredentials = true;
    private String login = "";
    private String password = "";
    private boolean useShuttleCredentials = false;

    /**
     * Constructs a <tt>ProxyConsolePanel</tt>.
     *
     * @param panel the parent panel/view. May be {@code null}
     * @param installData
     */
    public ProxyConsolePanel(PanelView<ConsolePanel> panel, InstallData installData) {
        super(panel);
        this.installData = installData;
    }

    /**
     * Runs the panel using the specified console.
     *

     * @param installData the installation data
     * @param console the console
     * @return <tt>true</tt> if the panel ran successfully, otherwise
     * <tt>false</tt>
     */
    @Override
    public boolean run(InstallData installData, Console console) {
        this.installData = installData;
        printHeadLine(installData, console);

        while (!promptUseProxy(console)) {
            //Continue to ask
        }
        if (useProxy) {
            if (!promptUseProxyRadio(console)) {
                return promptRerunPanel(installData, console);
            }
            if (!promptUseCredentials(console)) {
                return promptRerunPanel(installData, console);
            }
            if (useCredentials) {
                if (!promptUseCredentialRadio(console)) {
                    return promptRerunPanel(installData, console);
                }
            }
        }

        registerInputs();
        return promptEndPanel(installData, console);
    }

    private void registerInputs() {
        //Proxy
        installData.setVariable(USE_PROXY, Boolean.toString(useProxy));
        installData.setVariable(USE_DEFAULT_CONFIG, Boolean.toString(useDefaultProxy));
        if (useFollowingProxy) {
            installData.setVariable(PROXY_HOST, proxyHost);
            installData.setVariable(PROXY_PORT, proxyPort);
            if (!proxyPort.isEmpty()) {
                installData.setVariable(PROXY_PORT_ADDIN, proxyPort);
            }
        }

        //Proxy credentials
        installData.setVariable(USE_CREDENTIALS, Boolean.toString(useCredentials));
        if (useCredentials) {
            installData.setVariable(USE_SHUTTLE_CREDENTIALS, Boolean.toString(useShuttleCredentials));
            if (useFollowingCredentials) {
                installData.setVariable(PROXY_LOGIN, login);
                //We encrypt the password.
                installData.setVariable(PROXY_PASSWORD, NellCipher.encryptWithMarker(password, true));
            }
        }
    }

    private boolean promptUseProxyRadio(Console console) {
        console.println("Proxy use");
        console.println("0  [x] Use default proxy");
        console.println("1  [ ] Use custom proxy");
        int value = console.prompt("Input selection:", 0, 1, 0, -1);
        if (value != -1) {
            useDefaultProxy = value == 0;
            useFollowingProxy = value == 1;
            if (useFollowingProxy) {
                console.println("Using custom proxy.");
                proxyHost = console.prompt("Proxy host : ", "");
                proxyPort = console.prompt("Proxy port : ", "");
            }
            return true;
        } else {
            return false;
        }
    }

    private boolean promptUseProxy(Console console) {
        console.println("  [x] Use Proxy");
        int value = console.prompt("Enter 1 to select, 0 to deselect: ", 0, 1, 1, -1);
        if (value != -1) {
            useProxy = value == 1;
            return true;
        } else {
            return false;
        }
    }

    private boolean promptUseCredentials(Console console) {
        console.println("  [ ] Use Credentials");
        int value = console.prompt("Enter 1 to select, 0 to deselect: ", 0, 1, 1, -1);
        if (value != -1) {
            useCredentials = value == 1;
            return true;
        } else {
            return false;
        }
    }

    private boolean promptUseCredentialRadio(Console console) {
        console.println("Credential use");
        console.println("0  [x] Use following credentials");
        console.println("1  [ ] Use shuttle credentials");
        int value = console.prompt("Input selection:", 0, 1, 0, -1);
        if (value != -1) {
            useFollowingCredentials = value == 0;
            useShuttleCredentials = value == 1;
            if (useFollowingCredentials) {
                console.println("Using following credentials.");
                login = console.prompt("Username : ", "");
                String firstPwd = console.prompt("Password : ", "");
                String secondtPwd = console.prompt("Retype password : ", "");
                if (!firstPwd.equals(secondtPwd)) {
                    console.println("Passwords mismatch.");
                    return false;
                } else {
                    password = firstPwd;
                    return true;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void createInstallationRecord(IXMLElement panelRoot) {
        new ProxyPanelAutomationHelper().createInstallationRecord(installData, panelRoot);
    }

    @Override
    public boolean run(InstallData id, Properties prprts) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
