package gov.nasa.gsfc.seadas.earthdatacloud.matchup.ui;

import gov.nasa.gsfc.seadas.earthdatacloud.matchup.HarmonyEnvironment;

import javax.swing.JDialog;
import javax.swing.WindowConstants;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * The Satellite–In Situ Matchup window: a SeaBASS file against Level-2
 * products, one Harmony job per observation and product, written back as an
 * augmented SeaBASS file. Modeless, so SeaDAS stays usable during a run.
 */
public final class MatchupDialog extends JDialog {

    static final String TITLE = "Satellite–In Situ Matchup";

    private final MatchupPanel panel;

    /**
     * @param environment Harmony production, or UAT for testing
     * @param maxInFlight most Harmony jobs at once
     * @param halfBoxDeg  half-width of each request's box, degrees
     */
    public static MatchupDialog create(Window owner, HarmonyEnvironment environment, int maxInFlight,
                                       double halfBoxDeg) {
        return new MatchupDialog(owner, new MatchupSession(
                new MatchupSession.Options(environment, maxInFlight, halfBoxDeg), TokenSource.netrc()));
    }

    MatchupDialog(Window owner, MatchupSession session) {
        super(owner, TITLE + (session.options().environment() == HarmonyEnvironment.UAT ? " (Harmony UAT)" : ""),
                ModalityType.MODELESS);
        panel = new MatchupPanel(session, this::closeWindow);
        setContentPane(panel);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeWindow();
            }
        });
        pack();
        setLocationRelativeTo(owner);
    }

    private void closeWindow() {
        if (panel.confirmClose()) {
            dispose();
        }
    }
}
