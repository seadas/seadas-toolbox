package gov.nasa.gsfc.seadas.earthdatacloud.matchup.ui;

import gov.nasa.gsfc.seadas.earthdatacloud.matchup.HarmonyEnvironment;
import gov.nasa.gsfc.seadas.earthdatacloud.matchup.MatchupCollections;
import gov.nasa.gsfc.seadas.earthdatacloud.matchup.MatchupResults;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Drives the matchup window through a real run against Harmony UAT with the
 * sprint-review demo observations, saving a picture of each step. Runs
 * headless (no window on screen); needs UAT_TOKEN.
 *
 * <pre>java -Djava.awt.headless=true ... MatchupScreenshots out-dir</pre>
 */
public final class MatchupScreenshots {

    public static void main(String[] args) throws Exception {
        Path out = Path.of(args[0]);
        Files.createDirectories(out);
        Path sb = out.resolve("sims-demo.sb");
        Files.write(sb, List.of("/begin_header", "/investigators=Demo", "/missing=-9999", "/delimiter=comma",
                "/fields=station,date,time,lat,lon,chl",
                "/units=none,yyyymmdd,hh:mm:ss,degrees,degrees,mg/m^3", "/end_header",
                "ligurian,20260603,11:35:00,40.596,5.813,0.12",
                "baltic,20260603,11:35:00,55.914,19.015,2.40",
                "baltic_repeat,20260603,11:35:00,55.914,19.015,2.45",
                "santa_barbara,20101010,18:07:30,33.0,-119.5,1.10",
                "black_sea,20170406,09:33:00,42.925,40.537,0.85",
                "no_position,20170406,10:00:00,-9999,40.537,0.80"));

        MatchupSession session = new MatchupSession(new MatchupSession.Options(HarmonyEnvironment.UAT, 4, 0.1),
                TokenSource.fixed(System.getenv("UAT_TOKEN")));
        AtomicReference<MatchupPanel> panel = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> panel.set(new MatchupPanel(session, () -> {
        })));
        MatchupPanel p = panel.get();
        // Off screen, tables only get their header rows when added to a (peerless) hierarchy.
        edt(p::addNotify);

        edt(() -> ((ObservationsStep) p.step(0)).openFile(sb));
        edt(() -> p.go(0));
        shoot(p, out.resolve("1-observations.png"));

        edt(() -> {
            session.choose(MatchupCollections.in(HarmonyEnvironment.UAT).stream()
                    .filter(e -> List.of("MODIST_L2_OC", "VIIRSN_L2_OC", "PACE_OCI_L2_AOP_NRT").contains(e.shortName()))
                    .toList());
            p.go(1);
        });
        shoot(p, out.resolve("2a-satellite.png"));
        edt(() -> ((SatelliteStep) p.step(1)).checkCoverage());
        waitIdle(p.step(1));
        shoot(p, out.resolve("2b-coverage.png"));

        edt(() -> {
            p.go(2);
            ((RunStep) p.step(2)).start(false);
        });
        Thread.sleep(1500);
        shoot(p, out.resolve("3-start.png"));
        Thread.sleep(2500);
        shoot(p, out.resolve("3a-running.png"));
        waitIdle(p.step(2));
        shoot(p, out.resolve("3b-run-done.png"));

        edt(() -> p.go(3));
        Thread.sleep(1500);
        edt(() -> {
        });
        shoot(p, out.resolve("4a-results.png"));
        edt(() -> {
            javax.swing.JTable table = find(p.step(3), javax.swing.JTable.class, 0);
            int granuleColumn = table.getColumnModel().getColumnIndex("Granule");
            int row0 = 0;
            while (String.valueOf(table.getValueAt(row0, granuleColumn)).equals("—")) {
                row0++;
            }
            final int row = row0;
            table.setRowSelectionInterval(row, row);
            for (int i = 0; ; i++) {
                javax.swing.JButton b = find(p.step(3), javax.swing.JButton.class, i);
                if (b.getText().equals(RemoteGranules.TITLE)) {
                    System.out.println("Open granule remotely, obs " + table.getValueAt(row, 0) + " selected: enabled="
                            + b.isEnabled());
                    break;
                }
            }
        });
        shoot(p, out.resolve("4a-results-selected.png"));
        edt(() -> {
            javax.swing.JComboBox<?> product = find(p.step(3), javax.swing.JComboBox.class, 0);
            product.setSelectedItem("PACE OCI AOP NRT");
        });
        shoot(p, out.resolve("4b-results-pace.png"));
        edt(() -> {
            javax.swing.JComboBox<?> product = find(p.step(3), javax.swing.JComboBox.class, 0);
            product.setSelectedItem("All products");
        });
        shoot(p, out.resolve("4c-results-all.png"));
        MatchupResults all = MatchupResults.of(session.run());
        String pace = MatchupCollections.all().stream().filter(e -> e.shortName().equals("PACE_OCI_L2_AOP_NRT"))
                .findFirst().orElseThrow().id(HarmonyEnvironment.UAT);
        AtomicReference<java.awt.Container> view = new AtomicReference<>();
        edt(() -> view.set(CsvView.content(all.csv(r -> r.task().collection().equals(pace)), all.rows().size(),
                new javax.swing.JButton("Close"))));
        edt(() -> {
            view.get().setPreferredSize(new java.awt.Dimension(1100, 560));
            view.get().addNotify();
        });
        shoot(view.get(), out.resolve("4d-view-csv.png"));
        MatchupResults results = MatchupResults.of(session.run());
        results.writeCsv(out.resolve("sims-demo_matchup.csv"));
        System.out.println("Summary: " + results.summary(null));
        // The SeaBASS export, as the Export SeaBASS button makes it.
        java.util.Map<String, gov.nasa.gsfc.seadas.earthdatacloud.matchup.MatchupProduct> products =
                new java.util.LinkedHashMap<>();
        for (String id : session.run().settings().collections()) {
            products.put(id, gov.nasa.gsfc.seadas.earthdatacloud.matchup.MatchupProduct.fetch(
                    gov.nasa.gsfc.seadas.earthdatacloud.matchup.HttpTransport.jdk(), HarmonyEnvironment.UAT.cmr(), id));
        }
        var joined = session.run().join(products,
                gov.nasa.gsfc.seadas.earthdatacloud.matchup.ResultJoiner.Rows.ALL);
        joined.writer().write(MatchupSession.outputPath(sb, ".sb"));
        System.out.println("Export warnings: " + joined.warnings());
        System.out.println("Record: " + session.run().record());
        System.exit(0);
    }

    /** The {@code n}th component of a type, depth first. */
    @SuppressWarnings("unchecked")
    private static <T extends Component> T find(Container root, Class<T> type, int n) {
        List<Component> found = new java.util.ArrayList<>();
        collect(root, type, found);
        return (T) found.get(n);
    }

    private static void collect(Container root, Class<?> type, List<Component> found) {
        for (Component child : root.getComponents()) {
            if (type.isInstance(child)) {
                found.add(child);
            }
            if (child instanceof Container c) {
                collect(c, type, found);
            }
        }
    }

    private static void edt(Runnable action) throws Exception {
        SwingUtilities.invokeAndWait(action);
    }

    private static void waitIdle(Step step) throws Exception {
        AtomicBoolean busy = new AtomicBoolean(true);
        for (int i = 0; i < 600 && busy.get(); i++) {
            Thread.sleep(500);
            SwingUtilities.invokeAndWait(() -> busy.set(step.busy()));
        }
        Thread.sleep(300);
        edt(() -> {
        });
    }

    private static void shoot(Component component, Path png) throws Exception {
        AtomicReference<BufferedImage> image = new AtomicReference<>();
        // Twice: wrapped text knows its height once it has a width.
        SwingUtilities.invokeAndWait(() -> {
            component.setSize(component.getPreferredSize());
            layout(component);
        });
        SwingUtilities.invokeAndWait(() -> {
            layout(component);
            layout(component);
            BufferedImage img = new BufferedImage(component.getWidth(), component.getHeight(),
                    BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            component.printAll(g);
            g.dispose();
            image.set(img);
        });
        ImageIO.write(image.get(), "png", png.toFile());
        System.out.println("Saved " + png);
    }

    /** As revalidate would on screen: fresh sizes, then lay out. */
    private static void layout(Component component) {
        component.invalidate();
        component.doLayout();
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                layout(child);
            }
        }
    }
}
