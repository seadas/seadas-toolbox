package gov.nasa.gsfc.seadas.earthdatacloud.matchup.ui;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import java.awt.BorderLayout;

/** One step of the matchup window. */
abstract class Step extends JPanel {

    protected final MatchupSession session;

    Step(MatchupSession session) {
        super(new BorderLayout(0, 14));
        this.session = session;
        setBackground(Look.MAIN);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 16, 24));
    }

    /** The name in the step list. */
    abstract String title();

    /** What the Help button says about this step. */
    abstract String help();

    /** The short note at the foot of the step list while this step is open; null for none. */
    String navNote() {
        return null;
    }

    /** The Next button's text. */
    String nextLabel() {
        return "Next ›";
    }

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
