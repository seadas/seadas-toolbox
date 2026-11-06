package gov.nasa.gsfc.seadas.earthdatacloud.matchup;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * In-situ values against satellite values, for the scatter plot: which
 * satellite variables have a SeaBASS counterpart (chlor_a and chl, Rrs_443 and
 * Rrs443, sst and wt), the pairs, and how well they agree.
 */
public final class MatchupPairs {

    /**
     * A satellite variable with its in-situ field.
     *
     * @param satellite e.g. {@code chlor_a}
     * @param inSitu    the SeaBASS field, e.g. {@code chl}
     */
    public record Variable(String satellite, String inSitu) {
        /** The service's column with the box mean. */
        public String meanColumn() {
            return "sat_" + satellite + "_mean";
        }

        /** Chlorophyll spans decades, so it is shown and compared in log10. */
        public boolean logByDefault() {
            String lower = satellite.toLowerCase(Locale.ROOT);
            return lower.startsWith("chl");
        }
    }

    /**
     * One observation and product.
     *
     * @param matched false for a rejected observation (shown only when asked for)
     */
    public record Point(MatchupResults.Item item, double inSitu, double satellite, boolean matched) {
    }

    /**
     * Agreement of satellite (y) with in situ (x), on the values as given or
     * their log10. Bias is mean(y - x), MAE mean |y - x|, RMSE the root mean
     * square of y - x; slope and intercept are the least-squares line, r2 the
     * squared Pearson correlation. In log10, 10^bias and 10^MAE are factors.
     */
    public record Stats(int n, double r2, double slope, double intercept, double bias, double mae, double rmse,
                        boolean log) {
    }

    private MatchupPairs() {
    }

    /** The SeaBASS field measuring what a satellite variable does, or null. */
    public static String inSituField(SeabassFile file, String variable) {
        List<String> candidates = new ArrayList<>();
        String lower = variable.toLowerCase(Locale.ROOT);
        if (lower.equals("chlor_a")) {
            // Tot_Chl_a is HPLC total chlorophyll a, what chlor_a estimates; Tchl adds b and c.
            candidates.addAll(List.of("chl", "chl_a", "chla", "tot_chl_a", "tchl"));
        } else if (lower.startsWith("sst")) {
            candidates.addAll(List.of("wt", "sst"));
        } else if (lower.matches("rrs_\\d+")) {
            String band = lower.substring(4);
            candidates.addAll(List.of("rrs" + band, "rrs_" + band));
        }
        candidates.add(lower);
        for (String candidate : candidates) {
            for (String field : file.fields()) {
                if (field.toLowerCase(Locale.ROOT).equals(candidate)) {
                    return field;
                }
            }
        }
        return null;
    }

    /** The variables with an in-situ field, in the order the results first have them. */
    public static List<Variable> variables(List<MatchupResults.Item> items, SeabassFile file) {
        List<Variable> variables = new ArrayList<>();
        List<String> seen = new ArrayList<>();
        for (MatchupResults.Item item : items) {
            if (item.chosen() == null) {
                continue;
            }
            for (String key : item.chosen().keySet()) {
                if (key.startsWith("sat_") && key.endsWith("_mean") && !seen.contains(key)) {
                    seen.add(key);
                    String satellite = key.substring("sat_".length(), key.length() - "_mean".length());
                    String field = inSituField(file, satellite);
                    if (field != null) {
                        variables.add(new Variable(satellite, field));
                    }
                }
            }
        }
        return variables;
    }

    /**
     * The pairs of matched results, and of rejected ones when asked, where
     * both values are numbers and the in-situ one isn't the file's missing value.
     */
    public static List<Point> points(List<MatchupResults.Item> items, SeabassFile file, Variable variable,
                                     boolean withRejected) {
        List<Point> points = new ArrayList<>();
        for (MatchupResults.Item item : items) {
            boolean matched = item.outcome() == MatchupResults.Outcome.MATCHED;
            if (item.chosen() == null || !(matched || withRejected && item.outcome() == MatchupResults.Outcome.REJECTED)) {
                continue;
            }
            String inSitu = file.value(item.task().row(), variable.inSitu());
            Double x = file.isFlagged(inSitu) ? null : number(inSitu);
            Double y = number(item.chosen().get(variable.meanColumn()));
            if (x != null && y != null) {
                points.add(new Point(item, x, y, matched));
            }
        }
        return points;
    }

    /** The statistics, or null with fewer than two usable points (in log10, both values must be above 0). */
    public static Stats stats(List<Point> points, boolean log) {
        List<double[]> xy = new ArrayList<>();
        for (Point p : points) {
            if (!log) {
                xy.add(new double[]{p.inSitu(), p.satellite()});
            } else if (p.inSitu() > 0 && p.satellite() > 0) {
                xy.add(new double[]{Math.log10(p.inSitu()), Math.log10(p.satellite())});
            }
        }
        int n = xy.size();
        if (n < 2) {
            return null;
        }
        double mx = 0;
        double my = 0;
        for (double[] v : xy) {
            mx += v[0];
            my += v[1];
        }
        mx /= n;
        my /= n;
        double sxx = 0;
        double syy = 0;
        double sxy = 0;
        double bias = 0;
        double mae = 0;
        double mse = 0;
        for (double[] v : xy) {
            double dx = v[0] - mx;
            double dy = v[1] - my;
            sxx += dx * dx;
            syy += dy * dy;
            sxy += dx * dy;
            double d = v[1] - v[0];
            bias += d;
            mae += Math.abs(d);
            mse += d * d;
        }
        double slope = sxx == 0 ? Double.NaN : sxy / sxx;
        double r2 = sxx == 0 || syy == 0 ? Double.NaN : sxy * sxy / (sxx * syy);
        return new Stats(n, r2, slope, my - slope * mx, bias / n, mae / n, Math.sqrt(mse / n), log);
    }

    private static Double number(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            double value = Double.parseDouble(text.trim());
            return Double.isFinite(value) ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
