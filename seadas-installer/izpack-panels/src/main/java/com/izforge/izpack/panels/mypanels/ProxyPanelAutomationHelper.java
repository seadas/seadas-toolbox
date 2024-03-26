import com.izforge.izpack.api.adaptator.IXMLElement;
import com.izforge.izpack.api.adaptator.impl.XMLElementImpl;
import com.izforge.izpack.api.data.InstallData;
import com.izforge.izpack.api.data.Overrides;
import com.izforge.izpack.api.data.Variables;
import com.izforge.izpack.api.exception.InstallerException;
import com.izforge.izpack.installer.automation.PanelAutomation;
import static com.shuttle.panel.ProxyPanel.ENTRIES;

import java.util.*;
import java.util.logging.Logger;

/**
 * Functions to support automated usage of the ProxyPanel.
 *
 */
public class ProxyPanelAutomationHelper implements PanelAutomation {

    private static final Logger LOGGER = Logger.getLogger(ProxyPanelAutomationHelper.class.getName());

    // ------------------------------------------------------
    // automatic script section keys
    // ------------------------------------------------------
    private static final String AUTO_KEY_ENTRY = "entry";

    // ------------------------------------------------------
    // automatic script keys attributes
    // ------------------------------------------------------
    private static final String AUTO_ATTRIBUTE_KEY = "key";

    private static final String AUTO_ATTRIBUTE_VALUE = "value";

    /**
     * Default constructor, used during automated installation.
     */
    public ProxyPanelAutomationHelper() {

    }

    /**
     * Serialize state to XML and insert under panelRoot.
     *
     * @param installData The installation installData GUI.
     * @param rootElement The XML root element of the panels blackbox tree.
     */
    @Override
    public void createInstallationRecord(InstallData installData, IXMLElement rootElement) {
        IXMLElement dataElement;

        for (String key : ENTRIES) {
            dataElement = new XMLElementImpl(AUTO_KEY_ENTRY, rootElement);
            dataElement.setAttribute(AUTO_ATTRIBUTE_KEY, key);
            String value = installData.getVariable(key);
            dataElement.setAttribute(AUTO_ATTRIBUTE_VALUE, value);
            rootElement.addChild(dataElement);
        }
    }

    /**
     * Deserialize state from panelRoot and set installData variables
     * accordingly.
     *
     * @param idata The installation installDataGUI.
     * @param panelRoot The XML root element of the panels blackbox tree.
     * @throws InstallerException if some elements are missing.
     */
    @Override
    public void runAutomated(InstallData idata, IXMLElement panelRoot) throws InstallerException {
        String variable;
        String value;

        List<IXMLElement> userEntries = panelRoot.getChildrenNamed(AUTO_KEY_ENTRY);
        HashSet<String> blockedVariablesList = new HashSet<String>();

        // ----------------------------------------------------
        // retieve each entry and substitute the associated
        // variable
        // ----------------------------------------------------
        Variables variables = idata.getVariables();
        for (IXMLElement dataElement : userEntries) {
            variable = dataElement.getAttribute(AUTO_ATTRIBUTE_KEY);

            // Substitute variable used in the 'value' field
            value = dataElement.getAttribute(AUTO_ATTRIBUTE_VALUE);
            value = variables.replace(value);

            LOGGER.fine("Setting variable " + variable + " to " + value);
            idata.setVariable(variable, value);
            blockedVariablesList.add(variable);
        }
        idata.getVariables().registerBlockedVariableNames(blockedVariablesList, panelRoot.getName());
    }

    @Override
    public void processOptions(InstallData installData, Overrides overrides) {
    }
}