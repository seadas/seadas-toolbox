/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package gov.nasa.gsfc.seadas.processing.help.configuration;

import gov.nasa.gsfc.seadas.processing.help.DesktopHelper;
import org.esa.snap.runtime.Config;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * This action launches the default browser to display the project web page.
 */
@ActionID(category = "Help", id = "ShowSeadasSystemRequirementsAction")
@ActionRegistration(
        displayName = "#CTL_ShowSeadasSystemRequirementsAction_MenuText",
        popupText = "#CTL_ShowSeadasSystemRequirementsAction_MenuText")
@ActionReference(
        path = "Menu/Help/SeaDAS/Configuration",
        position = 10
)
@NbBundle.Messages({
        "CTL_ShowSeadasSystemRequirementsAction_MenuText=System Requirements",
        "CTL_ShowSeadasSystemRequirementsAction_ShortDescription=Browse the SeaDAS system requirements web page"
})
public class ShowSeadasSystemRequirementsAction extends AbstractAction {

    private static final String DEFAULT_PAGE_URL = "https://seadas.gsfc.nasa.gov/requirements/";

    /**
     * Launches the default browser to display the web site.
     * Invoked when a command action is performed.
     *
     * @param event the command event.
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        DesktopHelper.browse(Config.instance().preferences().get("seadas.systemRequirements", DEFAULT_PAGE_URL));
    }
}
