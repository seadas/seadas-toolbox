package gov.nasa.gsfc.seadas.earthdatacloud.matchup;

import java.time.LocalDateTime;

/**
 * One data row of a SeaBASS file as an observation to match: where and when.
 *
 * @param row        the row's index in the file's data (0-based)
 * @param lineNumber the row's line in the file (1-based)
 * @param time       UTC date-time, or null when it can't be read
 * @param lat        degrees north, or NaN when it can't be read
 * @param lon        degrees east, or NaN when it can't be read
 * @param problem    why the row can't be matched, or null when it can
 * @param note       something the user should know about a usable row (e.g. an assumed time), or null
 */
public record SeabassObservation(int row, int lineNumber, LocalDateTime time, double lat, double lon,
                                 String problem, String note) {

    /** Whether the row has a time and a valid position. */
    public boolean usable() {
        return problem == null;
    }
}
