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
 * This action launches the default browser to display video.
 */
@ActionID(category = "Video Tutorial", id = "ShowVideos06Action")
@ActionRegistration(
        displayName = "#TutorialVideos06Action_DisplayName",
        menuText = "#TutorialVideos06Action_MenuText",
        popupText = "#TutorialVideos06Action_ShortDescription")
@ActionReference(
        path = "Menu/Video-Tutorials/General Tools",
        position = 10)
@NbBundle.Messages({
        "TutorialVideos06Action_DisplayName=Layer Manager Overview",
        "TutorialVideos06Action_MenuText=Layer Manager - Manage & Edit Graphic Image Layers",
        "TutorialVideos06Action_ShortDescription=Show YouTube playlist"
})



public class ShowTutorialVideos06Action extends AbstractAction {

    private static final String DEFAULT_PAGE_URL = "https://www.youtube.com/watch?v=wx08vP2JorA";

    /**
     * Launches the default browser to display the playlist.
     * Invoked when a command action is performed.
     *
     * @param event the command event.
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        DesktopHelper.browse(Config.instance().preferences().get("seadas.tutorial.videos.06", DEFAULT_PAGE_URL));
    }
}
