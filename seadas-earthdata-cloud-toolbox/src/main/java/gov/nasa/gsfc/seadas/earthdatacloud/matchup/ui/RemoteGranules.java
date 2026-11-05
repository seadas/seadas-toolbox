package gov.nasa.gsfc.seadas.earthdatacloud.matchup.ui;

import gov.nasa.gsfc.seadas.earthdatacloud.auth.EarthdataRemoteAccess;
import gov.nasa.gsfc.seadas.earthdatacloud.matchup.GranuleLinks;
import gov.nasa.gsfc.seadas.earthdatacloud.matchup.HarmonyEnvironment;
import gov.nasa.gsfc.seadas.earthdatacloud.matchup.HttpTransport;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.rcp.SnapApp;

import javax.swing.JOptionPane;
import java.awt.Component;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Opens result granules in SeaDAS straight from the Earthdata Archive, as the
 * Earthdata Cloud data browser's Open Remotely does: SeaDAS reads the parts it
 * shows over HTTPS, with the Earthdata Login token from {@code ~/.netrc}.
 */
final class RemoteGranules {

    static final String TITLE = "Open granule remotely";
    /** As the data browser: more at once is slow and rarely meant. */
    static final int MAX_AT_ONCE = 5;

    /** Products opened from here, by granule name, so one still open isn't opened again. */
    private final Map<String, Product> opened = new HashMap<>();

    private record Outcome(Map<String, Product> products, List<String> problems) {
    }

    /**
     * Opens granules, each named with its product's short name.
     *
     * @param granules granule file name to product short name
     * @param always   run on the event thread when done, opened or not
     */
    void open(Component parent, Map<String, String> granules, Runnable always) {
        List<String> alreadyOpen = new ArrayList<>();
        Map<String, String> wanted = new LinkedHashMap<>();
        granules.forEach((granule, shortName) -> {
            Product product = opened.get(granule);
            if (product != null && SnapApp.getDefault().getProductManager().contains(product)) {
                alreadyOpen.add(granule);
            } else {
                wanted.put(granule, shortName);
            }
        });
        if (wanted.isEmpty()) {
            JOptionPane.showMessageDialog(parent, Text.wrap("Already open in SeaDAS: " + String.join(", ", alreadyOpen)),
                    TITLE, JOptionPane.INFORMATION_MESSAGE);
            SnapApp.getDefault().getMainFrame().toFront();
            always.run();
            return;
        }
        if (wanted.size() > MAX_AT_ONCE) {
            JOptionPane.showMessageDialog(parent, "Select rows of at most " + MAX_AT_ONCE + " granules.", TITLE,
                    JOptionPane.WARNING_MESSAGE);
            always.run();
            return;
        }

        EarthdataRemoteAccess.ensureRegistered();
        Background.run(parent, TITLE, () -> read(wanted), outcome -> {
            for (Map.Entry<String, Product> entry : outcome.products().entrySet()) {
                SnapApp.getDefault().getProductManager().addProduct(entry.getValue());
                opened.put(entry.getKey(), entry.getValue());
            }
            if (!outcome.problems().isEmpty()) {
                JOptionPane.showMessageDialog(parent, Text.wrap("Could not open:\n"
                        + String.join("\n", outcome.problems())), TITLE, JOptionPane.ERROR_MESSAGE);
            }
            if (!outcome.products().isEmpty()) {
                // The new products are in Product Explorer in the SeaDAS window.
                SnapApp.getDefault().getMainFrame().toFront();
            }
        }, always);
    }

    /** Off the event thread: finds each granule's link and reads its header. */
    private static Outcome read(Map<String, String> granules) throws InterruptedException {
        Map<String, Product> products = new LinkedHashMap<>();
        List<String> problems = new ArrayList<>();
        HttpTransport http = HttpTransport.jdk();
        for (Map.Entry<String, String> entry : granules.entrySet()) {
            String granule = entry.getKey();
            try {
                String link = GranuleLinks.find(http, HarmonyEnvironment.PRODUCTION.cmr(), entry.getValue(), granule);
                if (link == null) {
                    throw new IOException("not found in the Earthdata Archive");
                }
                ProductReader reader = ProductIO.getProductReaderForInput(link);
                if (reader == null) {
                    throw new IOException("no SeaDAS reader opens this file remotely");
                }
                products.put(granule, reader.readProductNodes(link, null));
            } catch (IOException | RuntimeException e) {
                SystemUtils.LOG.log(Level.WARNING, "Remote open failed: " + granule, e);
                problems.add(granule + ": " + rootMessage(e));
            }
        }
        return new Outcome(products, problems);
    }

    private static String rootMessage(Throwable t) {
        while (t.getCause() != null && t.getCause() != t) {
            t = t.getCause();
        }
        return t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
    }
}
