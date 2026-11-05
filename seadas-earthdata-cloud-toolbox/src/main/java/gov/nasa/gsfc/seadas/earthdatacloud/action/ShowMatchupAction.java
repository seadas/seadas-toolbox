package gov.nasa.gsfc.seadas.earthdatacloud.action;

import gov.nasa.gsfc.seadas.earthdatacloud.matchup.HarmonyEnvironment;
import gov.nasa.gsfc.seadas.earthdatacloud.matchup.ui.MatchupDialog;
import gov.nasa.gsfc.seadas.earthdatacloud.preferences.Earthdata_Cloud_Controller;
import gov.nasa.gsfc.seadas.earthdatacloud.util.MenuLocationUtils;
import org.esa.snap.rcp.SnapApp;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;

/**
 * Opens the Satellite–In Situ Matchup window: a SeaBASS file against Level-2
 * products, through the satellite-in-situ-matchup Harmony service.
 */
@ActionID(category = "View", id = "ShowMatchupAction")
@ActionRegistration(displayName = "#CTL_ShowMatchupActionName", lazy = true)
@ActionReferences({
        @ActionReference(path = MenuLocationUtils.EARTHDATA_CLOUD_MENU_LOCATION, position = 10),
        @ActionReference(path = MenuLocationUtils.EARTHDATA_CLOUD_MENU_LOCATION2, position = 50)
})
@NbBundle.Messages({
        "CTL_ShowMatchupActionName=Satellite–In Situ Matchup...",
        "CTL_ShowMatchupActionToolTip=Satellite values at in situ observations (SeaBASS), via Harmony"
})
public final class ShowMatchupAction extends AbstractAction {

    @Override
    public void actionPerformed(ActionEvent event) {
        HarmonyEnvironment environment = Earthdata_Cloud_Controller.MATCHUP_ENVIRONMENT_UAT.equals(
                Earthdata_Cloud_Controller.getPreferenceMatchupEnvironment())
                ? HarmonyEnvironment.UAT : HarmonyEnvironment.PRODUCTION;
        MatchupDialog dialog = MatchupDialog.create(SnapApp.getDefault().getMainFrame(), environment,
                Earthdata_Cloud_Controller.getPreferenceMatchupJobsInFlight(),
                Earthdata_Cloud_Controller.getPreferenceMatchupBoxHalfWidth());
        dialog.setVisible(true);
    }
}
