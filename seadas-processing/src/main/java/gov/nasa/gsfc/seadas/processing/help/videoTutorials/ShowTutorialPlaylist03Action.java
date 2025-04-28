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
package gov.nasa.gsfc.seadas.processing.help.videoTutorials;

import gov.nasa.gsfc.seadas.processing.help.DesktopHelper;
import org.esa.snap.runtime.Config;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * This action launches the default browser to display the video playlist.
 */
@ActionID(category = "Video Tutorials Playlist", id = "ShowTutorialPlaylist03Action")
@ActionRegistration(
        displayName = "#CTL_ShowTutorialPlaylist03Action_DisplayName",
        menuText = "#CTL_ShowTutorialPlaylist03Action_MenuText",
        popupText = "#CTL_ShowTutorialPlaylist03Action_ShortDescription")
@ActionReference(
        path = "Menu/Video-Tutorials/Science Processors (Installation)",
        separatorAfter = 1,
        position = 0)
@NbBundle.Messages({
        "CTL_ShowTutorialPlaylist03Action_DisplayName=Install Science Processors",
        "CTL_ShowTutorialPlaylist03Action_MenuText=View Full Playlist",
        "CTL_ShowTutorialPlaylist03Action_ShortDescription=Show YouTube playlist"
})

public class ShowTutorialPlaylist03Action extends AbstractAction {

    private static final String DEFAULT_PAGE_URL = "https://youtube.com/playlist?list=PLf60TttfDm30AkfKNAJU6lW_YZODNCFZB&si=j1zUL-LpTnXpPck4";

    /**
     * Launches the default browser to display the playlist.
     * Invoked when a command action is performed.
     *
     * @param event the command event.
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        DesktopHelper.browse(Config.instance().preferences().get("seadas.tutorial.playlist.03", DEFAULT_PAGE_URL));
    }
}
