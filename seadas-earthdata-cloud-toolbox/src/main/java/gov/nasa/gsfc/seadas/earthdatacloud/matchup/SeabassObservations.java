package gov.nasa.gsfc.seadas.earthdatacloud.matchup;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The observations in a SeaBASS file: each row's UTC date-time and position,
 * as {@code mk_matchup.py} finds them.
 *
 * <p><b>Date-time</b> comes from the first of these field combinations the file
 * has, in the order of {@code SB_support.py} {@code fd_datetime} (its code, which
 * differs a little from its docstring):
 * <ol>
 *   <li>date, time</li>
 *   <li>year, month, day, hour, minute, second</li>
 *   <li>year, month, day, time</li>
 *   <li>date, hour, minute, second</li>
 *   <li>date_time</li>
 *   <li>year, sdy, hour, minute, second</li>
 *   <li>year, sdy, time</li>
 *   <li>header /start_date, time</li>
 *   <li>header /start_date, hour, minute, second</li>
 *   <li>year, month, day, hour, minute</li>
 *   <li>date, hour, minute</li>
 *   <li>year, sdy, hour, minute</li>
 *   <li>year, month, day, hour</li>
 *   <li>date, hour</li>
 *   <li>year, sdy, hour</li>
 *   <li>year, month, day</li>
 *   <li>date</li>
 *   <li>year, sdy</li>
 *   <li>headers /start_date, /start_time</li>
 *   <li>header /start_date</li>
 * </ol>
 * Missing parts are taken as 0, as the script does; when the hour is missing the
 * observation gets a note. Two script branches never work ({@code date_time}, and
 * {@code date} alone, which raise); here they are read as intended.
 *
 * <p><b>Position</b> comes from the {@code lat} and {@code lon} fields, which the
 * script requires; a file without them has no usable observations.
 *
 * <p>The script stops at the first row it can't use. Here each such row gets a
 * {@link SeabassObservation#problem() problem} and the rest stay usable.
 */
public final class SeabassObservations {

    private static final Pattern DATE = Pattern.compile("(\\d{4})(\\d{2})(\\d{2})");
    private static final Pattern TIME = Pattern.compile("(\\d{1,2}):(\\d{2}):(\\d{2})");
    private static final Pattern DATE_TIME =
            Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})\\s(\\d{1,2}):(\\d{2}):(\\d{2})");
    private static final Pattern START_TIME =
            Pattern.compile("(\\d{1,2}):(\\d{2}):(\\d{2})\\[(gmt|GMT)\\]");

    /** How a row's date-time is put together. */
    enum Source {
        DATE_TIME_FIELDS("date/time", false, "date", "time"),
        YMD_HMS("year/month/day/hour/minute/second", false, "year", "month", "day", "hour", "minute", "second"),
        YMD_TIME("year/month/day/time", false, "year", "month", "day", "time"),
        DATE_HMS("date/hour/minute/second", false, "date", "hour", "minute", "second"),
        DATE_TIME_FIELD("date_time", false, "date_time"),
        YSDY_HMS("year/sdy/hour/minute/second", false, "year", "sdy", "hour", "minute", "second"),
        YSDY_TIME("year/sdy/time", false, "year", "sdy", "time"),
        START_DATE_TIME("/start_date and time", true, "time"),
        START_DATE_HMS("/start_date and hour/minute/second", true, "hour", "minute", "second"),
        YMD_HM("year/month/day/hour/minute", false, "year", "month", "day", "hour", "minute"),
        DATE_HM("date/hour/minute", false, "date", "hour", "minute"),
        YSDY_HM("year/sdy/hour/minute", false, "year", "sdy", "hour", "minute"),
        YMD_H("year/month/day/hour", false, "year", "month", "day", "hour"),
        DATE_H("date/hour", false, "date", "hour"),
        YSDY_H("year/sdy/hour", false, "year", "sdy", "hour"),
        YMD("year/month/day", false, "year", "month", "day"),
        DATE("date", false, "date"),
        YSDY("year/sdy", false, "year", "sdy"),
        START_HEADERS("/start_date and /start_time", true),
        START_DATE("/start_date", true);

        final String label;
        final boolean needsStartDate;
        final String[] fields;

        Source(String label, boolean needsStartDate, String... fields) {
            this.label = label;
            this.needsStartDate = needsStartDate;
            this.fields = fields;
        }

        boolean fits(SeabassFile file) {
            if (needsStartDate && !file.headers().containsKey("start_date")) {
                return false;
            }
            if (this == START_HEADERS && !file.headers().containsKey("start_time")) {
                return false;
            }
            for (String field : fields) {
                if (!file.hasField(field)) {
                    return false;
                }
            }
            return true;
        }

        boolean noHour() {
            return this == YMD || this == DATE || this == YSDY || this == START_DATE;
        }
    }

    private SeabassObservations() {
    }

    /** The date-time combination the file uses, or null when it has none. */
    static Source source(SeabassFile file) {
        for (Source source : Source.values()) {
            if (source.fits(file)) {
                return source;
            }
        }
        return null;
    }

    /** Describes how the file's date-times are read, e.g. "date/time", or null when they can't be. */
    public static String timeSource(SeabassFile file) {
        Source source = source(file);
        return source != null ? source.label : null;
    }

    /** One observation per data row, in file order. */
    public static List<SeabassObservation> of(SeabassFile file) {
        Source source = source(file);
        boolean hasPosition = file.hasField("lat") && file.hasField("lon");
        List<SeabassObservation> observations = new ArrayList<>(file.rowCount());
        for (int row = 0; row < file.rowCount(); row++) {
            int line = file.lineNumber(row);
            String problem = null;
            LocalDateTime time = null;
            if (source == null) {
                problem = "No date and time: the file has none of the date-time fields mk_matchup.py reads";
            } else {
                try {
                    time = time(file, row, source);
                } catch (IllegalArgumentException | DateTimeException e) {
                    problem = "Date-time can't be read from " + source.label + ": " + e.getMessage();
                }
            }

            double lat = Double.NaN;
            double lon = Double.NaN;
            if (!hasPosition) {
                problem = first(problem, "No position: the file has no lat and lon fields");
            } else {
                String latText = file.value(row, "lat");
                String lonText = file.value(row, "lon");
                lat = number(file, latText);
                lon = number(file, lonText);
                if (Double.isNaN(lat)) {
                    problem = first(problem, "Latitude missing or not a number: " + latText);
                } else if (Math.abs(lat) > 90.0) {
                    problem = first(problem, "Latitude out of range: " + latText);
                }
                if (Double.isNaN(lon)) {
                    problem = first(problem, "Longitude missing or not a number: " + lonText);
                } else if (Math.abs(lon) > 180.0) {
                    problem = first(problem, "Longitude out of range (must be -180 to 180): " + lonText);
                }
            }

            String note = null;
            if (problem == null && source.noHour()) {
                note = "No time of day in the file: 00:00 UTC assumed";
            }
            observations.add(new SeabassObservation(row, line, time, lat, lon, problem, note));
        }
        return observations;
    }

    private static String first(String existing, String problem) {
        return existing != null ? existing : problem;
    }

    private static double number(SeabassFile file, String text) {
        if (text == null || file.isMissing(text)) {
            return Double.NaN;
        }
        try {
            return Double.parseDouble(text.strip());
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    static LocalDateTime time(SeabassFile file, int row, Source source) {
        for (String field : source.fields) {
            if (file.isMissing(file.value(row, field))) {
                throw new IllegalArgumentException(field + " is missing");
            }
        }
        switch (source) {
            case DATE_TIME_FIELDS:
                return at(date(field(file, row, "date")), time(field(file, row, "time")));
            case YMD_HMS:
                return LocalDateTime.of(integer(file, row, "year"), integer(file, row, "month"),
                        integer(file, row, "day"), integer(file, row, "hour"), integer(file, row, "minute"),
                        integer(file, row, "second"));
            case YMD_TIME:
                return at(ymd(file, row), time(field(file, row, "time")));
            case DATE_HMS:
                return date(field(file, row, "date")).atTime(integer(file, row, "hour"),
                        integer(file, row, "minute"), integer(file, row, "second"));
            case DATE_TIME_FIELD: {
                Matcher m = match(DATE_TIME, field(file, row, "date_time"), "date_time");
                return LocalDateTime.of(group(m, 1), group(m, 2), group(m, 3), group(m, 4), group(m, 5),
                        group(m, 6));
            }
            case YSDY_HMS:
                return ysdy(file, row).atTime(integer(file, row, "hour"), integer(file, row, "minute"),
                        integer(file, row, "second"));
            case YSDY_TIME:
                return at(ysdy(file, row), time(field(file, row, "time")));
            case START_DATE_TIME:
                return at(startDate(file), time(field(file, row, "time")));
            case START_DATE_HMS:
                return startDate(file).atTime(integer(file, row, "hour"), integer(file, row, "minute"),
                        integer(file, row, "second"));
            case YMD_HM:
                return ymd(file, row).atTime(integer(file, row, "hour"), integer(file, row, "minute"));
            case DATE_HM:
                return date(field(file, row, "date")).atTime(integer(file, row, "hour"),
                        integer(file, row, "minute"));
            case YSDY_HM:
                return ysdy(file, row).atTime(integer(file, row, "hour"), integer(file, row, "minute"));
            case YMD_H:
                return ymd(file, row).atTime(integer(file, row, "hour"), 0);
            case DATE_H:
                return date(field(file, row, "date")).atTime(integer(file, row, "hour"), 0);
            case YSDY_H:
                return ysdy(file, row).atTime(integer(file, row, "hour"), 0);
            case YMD:
                return ymd(file, row).atStartOfDay();
            case DATE:
                return date(field(file, row, "date")).atStartOfDay();
            case YSDY:
                return ysdy(file, row).atStartOfDay();
            case START_HEADERS: {
                Matcher m = match(START_TIME, file.headers().get("start_time"), "/start_time");
                return startDate(file).atTime(group(m, 1), group(m, 2), group(m, 3));
            }
            case START_DATE:
                return startDate(file).atStartOfDay();
            default:
                throw new IllegalStateException(source.name());
        }
    }

    private static String field(SeabassFile file, int row, String field) {
        return file.value(row, field);
    }

    private static LocalDateTime at(LocalDate date, int[] hms) {
        return date.atTime(hms[0], hms[1], hms[2]);
    }

    private static LocalDate date(String text) {
        Matcher m = match(DATE, text, "date");
        return LocalDate.of(group(m, 1), group(m, 2), group(m, 3));
    }

    private static int[] time(String text) {
        Matcher m = match(TIME, text, "time");
        return new int[]{group(m, 1), group(m, 2), group(m, 3)};
    }

    private static LocalDate ymd(SeabassFile file, int row) {
        return LocalDate.of(integer(file, row, "year"), integer(file, row, "month"), integer(file, row, "day"));
    }

    private static LocalDate ysdy(SeabassFile file, int row) {
        return LocalDate.ofYearDay(integer(file, row, "year"), integer(file, row, "sdy"));
    }

    private static LocalDate startDate(SeabassFile file) {
        return date(file.headers().get("start_date"));
    }

    private static Matcher match(Pattern pattern, String text, String what) {
        Matcher m = pattern.matcher(text == null ? "" : text);
        if (!m.find()) {
            throw new IllegalArgumentException(what + " not in the expected form: " + text);
        }
        return m;
    }

    private static int group(Matcher m, int group) {
        return Integer.parseInt(m.group(group));
    }

    /** A whole number as Python's {@code int()} gives it: "2024" and "2024.0" are 2024. */
    private static int integer(SeabassFile file, int row, String field) {
        String text = file.value(row, field).strip();
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            try {
                double value = Double.parseDouble(text);
                if (!Double.isFinite(value)) {
                    throw new IllegalArgumentException(field + " is not a number: " + text);
                }
                return (int) value;
            } catch (NumberFormatException e2) {
                throw new IllegalArgumentException(field + " is not a number: " + text);
            }
        }
    }
}
