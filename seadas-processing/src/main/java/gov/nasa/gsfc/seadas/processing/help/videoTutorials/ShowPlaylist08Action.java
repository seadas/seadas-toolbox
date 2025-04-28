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
import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.esa.snap.runtime.Config;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.*;
import org.openide.util.actions.Presenter;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * This action launches the default browser to display the video playlist.
 */
@ActionID(category = "Video Tutorials Playlist", id = "ShowPlaylist08Action")
@ActionRegistration(
        displayName = "#CTL_ShowPlaylist08Action_DisplayName",
        popupText = "#CTL_ShowPlaylist08Action_ShortDescription")
@ActionReference(
        path = "Menu/Video-Tutorials/Overview",
        position = 0,
        separatorAfter = 1)
@NbBundle.Messages({
        "CTL_ShowPlaylist08Action_DisplayName=PLAYLIST: Overview",
        "CTL_ShowPlaylist08Action_ShortDescription=Opens YouTube playlist containing SeaDAS 'Overview' tutorial videos"
})

public class ShowPlaylist08Action extends AbstractSnapAction implements  LookupListener, Presenter.Menu {

    private static final String DEFAULT_PAGE_URL = "https://youtube.com/playlist?list=PLf60TttfDm30c5wSx7MgoaJEzNtYAIXoM";

    private final Lookup lookup;

    public ShowPlaylist08Action() {
        this(null);
    }

    public ShowPlaylist08Action(Lookup lookup) {
        putValue(ACTION_COMMAND_KEY, getClass().getName());
        putValue(SELECTED_KEY, false);
        putValue(NAME, Bundle.CTL_ShowPlaylist08Action_DisplayName());
        putValue(SHORT_DESCRIPTION, Bundle.CTL_ShowPlaylist08Action_ShortDescription());
        this.lookup = lookup != null ? lookup : Utilities.actionsGlobalContext();
        updateEnabledState();
    }


    /**
     * Launches the default browser to display the playlist.
     * Invoked when a command action is performed.
     *
     * @param event the command event.
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        DesktopHelper.browse(Config.instance().preferences().get("seadas.playlist08", DEFAULT_PAGE_URL));
        updateEnabledState();
    }


    protected void updateEnabledState() {
        super.setEnabled(true);
    }

    @Override
    public void resultChanged(LookupEvent ignored) {
        updateEnabledState();
    }


    @Override
    public JMenuItem getMenuPresenter() {
        JMenuItem menuItem = new JMenuItem(this);
        menuItem.setIcon(null);
        return menuItem;
    }

}
