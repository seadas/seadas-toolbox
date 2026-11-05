package gov.nasa.gsfc.seadas.earthdatacloud.matchup;

import java.io.IOException;

/**
 * A SeaBASS file that can't be read: a bad header, a missing required header,
 * or a data line that doesn't fit the fields. The message names the file and,
 * where there is one, the line.
 */
public class SeabassFormatException extends IOException {

    public SeabassFormatException(String message) {
        super(message);
    }
}
