package gov.nasa.gsfc.seadas.earthdatacloud.matchup.ui;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Small text helpers for the window. */
final class Text {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);

    private Text() {
    }

    static String time(LocalDateTime time) {
        return time == null ? "—" : TIME.format(time);
    }

    static String number(double value, int decimals) {
        return Double.isNaN(value) ? "—" : String.format(Locale.ROOT, "%." + decimals + "f", value);
    }

    /** HTML that wraps at about 70 characters, for message boxes. */
    static String wrap(String text) {
        return "<html><body style='width: 420px'>" + escape(text).replace("\n", "<br>") + "</body></html>";
    }

    static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    static String plural(long n, String one, String many) {
        return n + " " + (n == 1 ? one : many);
    }
}
