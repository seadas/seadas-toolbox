package gov.nasa.gsfc.seadas.earthdatacloud.matchup.ui;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import java.awt.BorderLayout;

/** One step of the matchup window. */
abstract class Step extends JPanel {

    protected final MatchupSession session;

    Step(MatchupSession session) {
        super(new BorderLayout(0, 8));
        this.session = session;
        setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
    }

    /** The name in the step list. */
    abstract String title();

    /** Whether the step can be opened yet, and why not. */
    String unavailableReason() {
        return null;
    }

    /** Called when the step is opened. */
    void shown() {
    }

    /** Whether the step has work going that closing the window would stop. */
    boolean busy() {
        return false;
    }

    /** Asks the step's work to stop. */
    void stop() {
    }
}
