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
@ActionID(category = "Video Tutorials Playlist", id = "ShowTutorialPlaylist08Action")
@ActionRegistration(
        displayName = "#CTL_ShowTutorialPlaylist08Action_DisplayName",
        menuText = "#CTL_ShowTutorialPlaylist08Action_MenuText",
        popupText = "#CTL_ShowTutorialPlaylist08Action_ShortDescription")
@ActionReference(
        path = "Menu/Video-Tutorials/Overview",
        position = 0,
        separatorAfter = 1)
@NbBundle.Messages({
        "CTL_ShowTutorialPlaylist08Action_DisplayName=SeaDAS Overview",
        "CTL_ShowTutorialPlaylist08Action_MenuText=View Full Playlist",
        "CTL_ShowTutorialPlaylist08Action_ShortDescription=Show YouTube playlist"
})

public class ShowTutorialPlaylist08Action extends AbstractAction {

    private static final String DEFAULT_PAGE_URL = "https://youtube.com/playlist?list=PLf60TttfDm30c5wSx7MgoaJEzNtYAIXoM";

    /**
     * Launches the default browser to display the playlist.
     * Invoked when a command action is performed.
     *
     * @param event the command event.
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        DesktopHelper.browse(Config.instance().preferences().get("seadas.tutorial.playlist.08", DEFAULT_PAGE_URL));
    }
}
