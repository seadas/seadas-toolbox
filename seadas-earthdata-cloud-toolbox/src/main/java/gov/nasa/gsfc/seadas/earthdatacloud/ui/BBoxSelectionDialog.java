package gov.nasa.gsfc.seadas.earthdatacloud.ui;

import org.json.simple.JSONArray;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Hybrid bbox selector for remote Harmony subsetting.
 *
 * LEFT PANEL  : browse/preview image for human visual context only.
 * RIGHT PANEL : authoritative geographic selector drawn in lon/lat space.
 *
 * Important design rule:
 *  - The geographic panel is the ONLY source of the bbox returned to Harmony.
 *  - The preview panel mirrors the selection approximately for user context.
 *
 * Footprint handling:
 *  - Footprints are interpreted as GeoJSON-style coordinate arrays: [lon, lat]
 *  - Nested polygon/ring structure is preserved; coordinates are NOT flattened
 *    into one continuous line.
 */
public class BBoxSelectionDialog extends JDialog {

    private final PreviewReferencePanel previewPanel;
    private final GeoBBoxPanel geoPanel;
    private final JLabel readoutLabel;

    private boolean confirmed = false;
    private double latMin, latMax, lonMin, lonMax;

    public BBoxSelectionDialog(Window owner,
                               BufferedImage previewImage,
                               double extentLatMin, double extentLatMax,
                               double extentLonMin, double extentLonMax) {
        super(owner, "Draw Bounding Box", ModalityType.APPLICATION_MODAL);

        double eLatMin = Math.min(extentLatMin, extentLatMax);
        double eLatMax = Math.max(extentLatMin, extentLatMax);
        double eLonMin = Math.min(extentLonMin, extentLonMax);
        double eLonMax = Math.max(extentLonMin, extentLonMax);

        previewPanel = new PreviewReferencePanel(previewImage, eLatMin, eLatMax, eLonMin, eLonMax);
        geoPanel = new GeoBBoxPanel(eLatMin, eLatMax, eLonMin, eLonMax);
        readoutLabel = new JLabel("Draw a box in the geographic panel on the right.");

        geoPanel.addSelectionListener(bbox -> {
            if (bbox == null) {
                previewPanel.clearGeoSelection();
                readoutLabel.setText("Selection cleared.");
            } else {
                previewPanel.setGeoSelection(bbox);
                readoutLabel.setText(String.format(
                        "Selection  lat(%.6f : %.6f)  lon(%.6f : %.6f)",
                        bbox.latMin, bbox.latMax, bbox.lonMin, bbox.lonMax));
            }
        });

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, previewPanel, geoPanel);
        splitPane.setResizeWeight(0.5);
        splitPane.setOneTouchExpandable(true);

        setLayout(new BorderLayout(8, 8));
        add(splitPane, BorderLayout.CENTER);
        add(createBottomPanel(), BorderLayout.SOUTH);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setPreferredSize(new Dimension(1240, 720));
        pack();
        setLocationRelativeTo(owner);

        getRootPane().registerKeyboardAction(e -> {
            confirmed = false;
            dispose();
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    public void setInitialBBox(double latMin, double latMax, double lonMin, double lonMax) {
        GeoBBox bbox = GeoBBox.of(latMin, latMax, lonMin, lonMax);
        geoPanel.setSelectionFromGeo(bbox);
        previewPanel.setGeoSelection(bbox);
    }

    public void setClampToExtent(boolean clamp) {
        geoPanel.setClampToExtent(clamp);
    }

    public void setFootprintPolygons(JSONArray polygons) {
        previewPanel.setFootprintPolygons(polygons);
        geoPanel.setFootprintPolygons(polygons);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public double getLatMin() {
        return latMin;
    }

    public double getLatMax() {
        return latMax;
    }

    public double getLonMin() {
        return lonMin;
    }

    public double getLonMax() {
        return lonMax;
    }

    private JPanel createBottomPanel() {
        JPanel bottom = new JPanel(new BorderLayout(8, 8));

        JLabel hint = new JLabel(
                "Left: browse image for visual reference only. Right: draw the actual geographic subset. " +
                        "Right-click in the geographic panel to clear.");
        bottom.add(hint, BorderLayout.NORTH);
        bottom.add(readoutLabel, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton clearBtn = new JButton("Clear");
        clearBtn.addActionListener(e -> geoPanel.clearSelection());

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> {
            confirmed = false;
            dispose();
        });

        JButton okBtn = new JButton("Use Selection");
        okBtn.addActionListener(e -> {
            GeoBBox bbox = geoPanel.getSelectionAsGeo();
            if (bbox == null) {
                JOptionPane.showMessageDialog(this,
                        "Draw a bounding box in the geographic panel first.",
                        "No Selection",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            confirmed = true;
            latMin = bbox.latMin;
            latMax = bbox.latMax;
            lonMin = bbox.lonMin;
            lonMax = bbox.lonMax;
            dispose();
        });

        buttons.add(clearBtn);
        buttons.add(cancelBtn);
        buttons.add(okBtn);
        bottom.add(buttons, BorderLayout.SOUTH);

        return bottom;
    }

    private interface SelectionListener {
        void selectionChanged(GeoBBox bbox);
    }

    private static final class GeoBBox {
        double latMin;
        double latMax;
        double lonMin;
        double lonMax;

        static GeoBBox of(double latMin, double latMax, double lonMin, double lonMax) {
            GeoBBox b = new GeoBBox();
            b.latMin = Math.min(latMin, latMax);
            b.latMax = Math.max(latMin, latMax);
            b.lonMin = Math.min(lonMin, lonMax);
            b.lonMax = Math.max(lonMin, lonMax);
            return b;
        }
    }

    private static final class LonLat {
        final double lon;
        final double lat;

        LonLat(double lon, double lat) {
            this.lon = lon;
            this.lat = lat;
        }
    }

    /**
     * Parses nested polygon/ring structures without flattening them into one line.
     * Expected coordinate order is [lon, lat].
     */
    private static final class FootprintParser {

        static List<List<LonLat>> parsePaths(JSONArray polygons) {
            List<List<LonLat>> paths = new ArrayList<>();
            if (polygons == null) {
                return paths;
            }

            for (Object polyObj : polygons) {
                collectCoordinateSequences(polyObj, paths);
            }
            return paths;
        }

        @SuppressWarnings("unchecked")
        private static void collectCoordinateSequences(Object obj, List<List<LonLat>> out) {
            if (!(obj instanceof JSONArray arr) || arr.isEmpty()) {
                return;
            }

            if (looksLikeCoordinatePairArray(arr)) {
                List<LonLat> ring = parseCoordinatePairSequence(arr);
                if (ring.size() >= 2) {
                    out.add(ring);
                }
                return;
            }

            for (Object item : arr) {
                if (item instanceof JSONArray child) {
                    collectCoordinateSequences(child, out);
                }
            }
        }

        private static boolean looksLikeCoordinatePairArray(JSONArray arr) {
            if (arr.isEmpty()) {
                return false;
            }

            for (Object item : arr) {
                if (!(item instanceof JSONArray pair) || pair.size() < 2) {
                    return false;
                }
                if (!(pair.get(0) instanceof Number || pair.get(0) instanceof String)) {
                    return false;
                }
                if (!(pair.get(1) instanceof Number || pair.get(1) instanceof String)) {
                    return false;
                }
            }
            return true;
        }

        private static List<LonLat> parseCoordinatePairSequence(JSONArray arr) {
            List<LonLat> coords = new ArrayList<>();
            for (Object item : arr) {
                if (!(item instanceof JSONArray pair) || pair.size() < 2) {
                    continue;
                }

                Double lon = asDouble(pair.get(0));
                Double lat = asDouble(pair.get(1));
                if (lon == null || lat == null) {
                    continue;
                }

                coords.add(new LonLat(lon, lat));
            }
            return coords;
        }

        private static Double asDouble(Object obj) {
            if (obj instanceof Number n) {
                return n.doubleValue();
            }
            if (obj instanceof String s) {
                try {
                    return Double.parseDouble(s.trim());
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
            return null;
        }
    }

    @SuppressWarnings("serial")
    private static final class PreviewReferencePanel extends JPanel {
        private final BufferedImage image;
        private final double extentLatMin;
        private final double extentLatMax;
        private final double extentLonMin;
        private final double extentLonMax;

        private JSONArray footprintPolygons;
        private GeoBBox geoSelection;

        PreviewReferencePanel(BufferedImage image,
                              double extentLatMin, double extentLatMax,
                              double extentLonMin, double extentLonMax) {
            this.image = image;
            this.extentLatMin = extentLatMin;
            this.extentLatMax = extentLatMax;
            this.extentLonMin = extentLonMin;
            this.extentLonMax = extentLonMax;
            setOpaque(true);
            setBackground(Color.BLACK);
            setDoubleBuffered(true);
        }

        void setFootprintPolygons(JSONArray polygons) {
            this.footprintPolygons = polygons;
            repaint();
        }

        void setGeoSelection(GeoBBox bbox) {
            this.geoSelection = bbox;
            repaint();
        }

        void clearGeoSelection() {
            this.geoSelection = null;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                if (image != null) {
                    g2.drawImage(image, 0, 0, getWidth(), getHeight(), this);
                } else {
                    g2.setColor(new Color(40, 40, 40));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                }

                paintFootprint(g2);
                paintMirroredSelection(g2);

                paintBanner(g2,
                        "Preview image (reference only) — selection is approximate here",
                        8, 8);

            } finally {
                g2.dispose();
            }
        }

        private void paintFootprint(Graphics2D g2) {
            if (footprintPolygons == null || footprintPolygons.isEmpty()) {
                return;
            }

            List<Path2D> paths = buildFootprintPaths();
            if (paths.isEmpty()) {
                return;
            }

            g2.setColor(new Color(255, 230, 80, 50));
            for (Path2D p : paths) {
                g2.fill(p);
            }

            g2.setColor(new Color(255, 215, 0));
            g2.setStroke(new BasicStroke(2f));
            for (Path2D p : paths) {
                g2.draw(p);
            }
        }

        private void paintMirroredSelection(Graphics2D g2) {
            if (geoSelection == null) {
                return;
            }

            int x1 = lonToX(geoSelection.lonMin);
            int x2 = lonToX(geoSelection.lonMax);
            int y1 = latToY(geoSelection.latMax);
            int y2 = latToY(geoSelection.latMin);

            Rectangle r = rectFromInclusivePoints(new Point(x1, y1), new Point(x2, y2));

            g2.setColor(new Color(0, 200, 255, 45));
            g2.fill(r);
            g2.setColor(new Color(0, 200, 255));
            g2.setStroke(new BasicStroke(2f));
            g2.draw(r);
        }

        private List<Path2D> buildFootprintPaths() {
            List<Path2D> out = new ArrayList<>();
            for (List<LonLat> ring : FootprintParser.parsePaths(footprintPolygons)) {
                if (ring.size() < 2) {
                    continue;
                }

                Path2D.Double p = new Path2D.Double();
                boolean started = false;

                for (LonLat point : ring) {
                    int x = lonToX(point.lon);
                    int y = latToY(point.lat);
                    if (!started) {
                        p.moveTo(x, y);
                        started = true;
                    } else {
                        p.lineTo(x, y);
                    }
                }

                p.closePath();
                out.add(p);
            }
            return out;
        }

        private Rectangle rectFromInclusivePoints(Point a, Point b) {
            int x = Math.min(a.x, b.x);
            int y = Math.min(a.y, b.y);
            int w = Math.abs(a.x - b.x);
            int h = Math.abs(a.y - b.y);
            return new Rectangle(x, y, w, h);
        }

        private int lonToX(double lon) {
            int w = Math.max(2, getWidth()) - 1;
            double lonSpan = extentLonMax - extentLonMin;
            if (lonSpan == 0.0) {
                return 0;
            }
            lon = clamp(lon, extentLonMin, extentLonMax);
            double pct = (lon - extentLonMin) / lonSpan;
            return (int) Math.round(clamp01(pct) * w);
        }

        private int latToY(double lat) {
            int h = Math.max(2, getHeight()) - 1;
            double latSpan = extentLatMax - extentLatMin;
            if (latSpan == 0.0) {
                return 0;
            }
            lat = clamp(lat, extentLatMin, extentLatMax);
            double pct = (extentLatMax - lat) / latSpan;
            return (int) Math.round(clamp01(pct) * h);
        }

        private double clamp01(double v) {
            return Math.max(0.0, Math.min(1.0, v));
        }

        private double clamp(double v, double lo, double hi) {
            return Math.max(lo, Math.min(hi, v));
        }

        private void paintBanner(Graphics2D g2, String text, int x, int y) {
            FontMetrics fm = g2.getFontMetrics();
            int pad = 6;
            int w = fm.stringWidth(text) + pad * 2;
            int h = fm.getHeight() + pad * 2;
            g2.setColor(new Color(0, 0, 0, 160));
            g2.fillRoundRect(x, y, w, h, 10, 10);
            g2.setColor(Color.WHITE);
            g2.drawString(text, x + pad, y + pad + fm.getAscent());
        }
    }

    @SuppressWarnings("serial")
    private static final class GeoBBoxPanel extends JPanel {
        private final double extentLatMin;
        private final double extentLatMax;
        private final double extentLonMin;
        private final double extentLonMax;

        private boolean clampToExtent = true;

        private Point dragStart;
        private Rectangle selectionPx;
        private boolean dragging;
        private String liveText = "";

        private JSONArray footprintPolygons;
        private final List<SelectionListener> listeners = new ArrayList<>();

        GeoBBoxPanel(double extentLatMin, double extentLatMax,
                     double extentLonMin, double extentLonMax) {
            this.extentLatMin = extentLatMin;
            this.extentLatMax = extentLatMax;
            this.extentLonMin = extentLonMin;
            this.extentLonMax = extentLonMax;

            setOpaque(true);
            setBackground(Color.WHITE);
            setDoubleBuffered(true);

            MouseAdapter mouse = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (SwingUtilities.isRightMouseButton(e)) {
                        clearSelection();
                        return;
                    }

                    dragStart = clampPointToPanel(e.getPoint());
                    selectionPx = new Rectangle(dragStart.x, dragStart.y, 0, 0);
                    dragging = true;
                    updateLiveText();
                    fireSelectionChanged();
                    repaint();
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (!dragging || dragStart == null) {
                        return;
                    }

                    Point cur = clampPointToPanel(e.getPoint());
                    selectionPx = rectFromInclusivePoints(dragStart, cur);
                    if (clampToExtent) {
                        selectionPx = clampRectToPanel(selectionPx);
                    }
                    updateLiveText();
                    fireSelectionChanged();
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (!dragging) {
                        return;
                    }
                    dragging = false;

                    if (selectionPx == null || selectionPx.width < 3 || selectionPx.height < 3) {
                        clearSelection();
                        return;
                    }

                    if (clampToExtent) {
                        selectionPx = clampRectToPanel(selectionPx);
                    }
                    updateLiveText();
                    fireSelectionChanged();
                    repaint();
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    Point p = clampPointToPanel(e.getPoint());
                    liveText = String.format("Cursor  lat=%.6f  lon=%.6f", yToLat(p.y), xToLon(p.x));
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    updateLiveText();
                    repaint();
                }
            };

            addMouseListener(mouse);
            addMouseMotionListener(mouse);
        }

        void setFootprintPolygons(JSONArray polygons) {
            this.footprintPolygons = polygons;
            repaint();
        }

        void setClampToExtent(boolean clampToExtent) {
            this.clampToExtent = clampToExtent;
        }

        void addSelectionListener(SelectionListener listener) {
            listeners.add(listener);
        }

        void clearSelection() {
            selectionPx = null;
            dragging = false;
            dragStart = null;
            liveText = "";
            fireSelectionChanged();
            repaint();
        }

        GeoBBox getSelectionAsGeo() {
            if (selectionPx == null || selectionPx.width <= 0 || selectionPx.height <= 0) {
                return null;
            }

            int x1 = selectionPx.x;
            int x2 = selectionPx.x + selectionPx.width;
            int y1 = selectionPx.y;
            int y2 = selectionPx.y + selectionPx.height;

            double lon1 = xToLon(x1);
            double lon2 = xToLon(x2);
            double lat1 = yToLat(y1);
            double lat2 = yToLat(y2);

            GeoBBox b = GeoBBox.of(lat1, lat2, lon1, lon2);

            if (clampToExtent) {
                b.lonMin = clamp(b.lonMin, extentLonMin, extentLonMax);
                b.lonMax = clamp(b.lonMax, extentLonMin, extentLonMax);
                b.latMin = clamp(b.latMin, extentLatMin, extentLatMax);
                b.latMax = clamp(b.latMax, extentLatMin, extentLatMax);
            }
            return b;
        }

        void setSelectionFromGeo(GeoBBox bbox) {
            if (bbox == null) {
                clearSelection();
                return;
            }

            int x1 = lonToX(bbox.lonMin);
            int x2 = lonToX(bbox.lonMax);
            int y1 = latToY(bbox.latMax);
            int y2 = latToY(bbox.latMin);

            selectionPx = rectFromInclusivePoints(new Point(x1, y1), new Point(x2, y2));
            updateLiveText();
            fireSelectionChanged();
            repaint();
        }

        private void fireSelectionChanged() {
            GeoBBox b = getSelectionAsGeo();
            for (SelectionListener listener : listeners) {
                listener.selectionChanged(b);
            }
        }

        private Rectangle rectFromInclusivePoints(Point a, Point b) {
            int x = Math.min(a.x, b.x);
            int y = Math.min(a.y, b.y);
            int w = Math.abs(a.x - b.x);
            int h = Math.abs(a.y - b.y);
            return new Rectangle(x, y, w, h);
        }

        private Rectangle clampRectToPanel(Rectangle r) {
            if (r == null) {
                return null;
            }

            int w = Math.max(2, getWidth()) - 1;
            int h = Math.max(2, getHeight()) - 1;

            int x1 = clampInt(r.x, 0, w);
            int y1 = clampInt(r.y, 0, h);
            int x2 = clampInt(r.x + r.width, 0, w);
            int y2 = clampInt(r.y + r.height, 0, h);

            return rectFromInclusivePoints(new Point(x1, y1), new Point(x2, y2));
        }

        private Point clampPointToPanel(Point p) {
            int w = Math.max(2, getWidth()) - 1;
            int h = Math.max(2, getHeight()) - 1;
            return new Point(clampInt(p.x, 0, w), clampInt(p.y, 0, h));
        }

        private double xToLon(int x) {
            int w = Math.max(2, getWidth()) - 1;
            if (w <= 0) {
                return extentLonMin;
            }
            x = clampInt(x, 0, w);
            double pct = (double) x / (double) w;
            return extentLonMin + pct * (extentLonMax - extentLonMin);
        }

        private double yToLat(int y) {
            int h = Math.max(2, getHeight()) - 1;
            if (h <= 0) {
                return extentLatMax;
            }
            y = clampInt(y, 0, h);
            double pct = (double) y / (double) h;
            return extentLatMax - pct * (extentLatMax - extentLatMin);
        }

        private int lonToX(double lon) {
            int w = Math.max(2, getWidth()) - 1;
            double lonSpan = extentLonMax - extentLonMin;
            if (lonSpan == 0.0) {
                return 0;
            }
            lon = clamp(lon, extentLonMin, extentLonMax);
            double pct = (lon - extentLonMin) / lonSpan;
            return (int) Math.round(clamp01(pct) * w);
        }

        private int latToY(double lat) {
            int h = Math.max(2, getHeight()) - 1;
            double latSpan = extentLatMax - extentLatMin;
            if (latSpan == 0.0) {
                return 0;
            }
            lat = clamp(lat, extentLatMin, extentLatMax);
            double pct = (extentLatMax - lat) / latSpan;
            return (int) Math.round(clamp01(pct) * h);
        }

        private double clamp01(double v) {
            return Math.max(0.0, Math.min(1.0, v));
        }

        private int clampInt(int v, int lo, int hi) {
            return Math.max(lo, Math.min(hi, v));
        }

        private double clamp(double v, double lo, double hi) {
            return Math.max(lo, Math.min(hi, v));
        }

        private void updateLiveText() {
            GeoBBox b = getSelectionAsGeo();
            if (b == null) {
                if (!dragging) {
                    liveText = "";
                }
                return;
            }

            liveText = String.format("Selection  lat(%.6f : %.6f)  lon(%.6f : %.6f)",
                    b.latMin, b.latMax, b.lonMin, b.lonMax);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                paintGraticule(g2);
                paintFootprint(g2);

                if (selectionPx != null) {
                    g2.setColor(new Color(0, 120, 215, 40));
                    g2.fill(selectionPx);
                    g2.setColor(new Color(0, 120, 215));
                    g2.setStroke(new BasicStroke(2f));
                    g2.draw(selectionPx);
                }

                if (liveText != null && !liveText.isBlank()) {
                    paintBanner(g2, liveText, 8, 8);
                }

                String extentText = String.format("Extent  lat(%.3f : %.3f) lon(%.3f : %.3f)",
                        extentLatMin, extentLatMax, extentLonMin, extentLonMax);
                paintBanner(g2, extentText, 8, getHeight() - 32);
                paintBanner(g2, "Geographic selector (authoritative)", getWidth() - 280, 8);

            } finally {
                g2.dispose();
            }
        }

        private void paintGraticule(Graphics2D g2) {
            int w = getWidth();
            int h = getHeight();
            if (w <= 1 || h <= 1) {
                return;
            }

            g2.setColor(new Color(235, 235, 235));
            for (int i = 0; i <= 10; i++) {
                int x = (int) Math.round(i * (w - 1) / 10.0);
                int y = (int) Math.round(i * (h - 1) / 10.0);
                g2.drawLine(x, 0, x, h - 1);
                g2.drawLine(0, y, w - 1, y);
            }

            g2.setColor(new Color(180, 180, 180));
            g2.drawRect(0, 0, w - 1, h - 1);
        }

        private void paintFootprint(Graphics2D g2) {
            if (footprintPolygons == null || footprintPolygons.isEmpty()) {
                return;
            }

            List<Path2D> paths = buildFootprintPaths();
            if (paths.isEmpty()) {
                return;
            }

            g2.setColor(new Color(255, 200, 0, 60));
            for (Path2D p : paths) {
                g2.fill(p);
            }

            g2.setColor(new Color(210, 140, 0));
            g2.setStroke(new BasicStroke(2f));
            for (Path2D p : paths) {
                g2.draw(p);
            }
        }

        private List<Path2D> buildFootprintPaths() {
            List<Path2D> out = new ArrayList<>();
            for (List<LonLat> ring : FootprintParser.parsePaths(footprintPolygons)) {
                if (ring.size() < 2) {
                    continue;
                }

                Path2D.Double p = new Path2D.Double();
                boolean started = false;

                for (LonLat point : ring) {
                    int x = lonToX(point.lon);
                    int y = latToY(point.lat);
                    if (!started) {
                        p.moveTo(x, y);
                        started = true;
                    } else {
                        p.lineTo(x, y);
                    }
                }

                p.closePath();
                out.add(p);
            }
            return out;
        }

        private void paintBanner(Graphics2D g2, String text, int x, int y) {
            FontMetrics fm = g2.getFontMetrics();
            int pad = 6;
            int w = fm.stringWidth(text) + pad * 2;
            int h = fm.getHeight() + pad * 2;
            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRoundRect(x, y, w, h, 10, 10);
            g2.setColor(Color.WHITE);
            g2.drawString(text, x + pad, y + pad + fm.getAscent());
        }
    }
}