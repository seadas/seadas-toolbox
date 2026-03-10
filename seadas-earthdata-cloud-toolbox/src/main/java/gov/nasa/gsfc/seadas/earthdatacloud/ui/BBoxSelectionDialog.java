package gov.nasa.gsfc.seadas.earthdatacloud.ui;

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
 * This keeps the old helpful visual browse image without pretending that it is
 * georectified enough to define an exact subset.
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

        geoPanel.addSelectionListener((a, b, c, d) -> {
            previewPanel.setGeoSelection(a, b, c, d);
            readoutLabel.setText(String.format(
                    "Selection  lat(%.6f : %.6f)  lon(%.6f : %.6f)", a, b, c, d));
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
        geoPanel.setSelectionFromGeo(latMin, latMax, lonMin, lonMax);
        previewPanel.setGeoSelection(Math.min(latMin, latMax), Math.max(latMin, latMax),
                Math.min(lonMin, lonMax), Math.max(lonMin, lonMax));
    }

    public void setClampToExtent(boolean clamp) {
        geoPanel.setClampToExtent(clamp);
    }

    public void setFootprintPolygons(org.json.simple.JSONArray polygons) {
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
        clearBtn.addActionListener(e -> {
            geoPanel.clearSelection();
            previewPanel.clearGeoSelection();
            readoutLabel.setText("Selection cleared.");
        });

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
        void selectionChanged(double latMin, double latMax, double lonMin, double lonMax);
    }

    private static final class GeoBBox {
        double latMin, latMax, lonMin, lonMax;
    }

    @SuppressWarnings("serial")
    private static final class PreviewReferencePanel extends JPanel {
        private final BufferedImage image;
        private final double extentLatMin, extentLatMax, extentLonMin, extentLonMax;

        private org.json.simple.JSONArray footprintPolygons;
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

        void setFootprintPolygons(org.json.simple.JSONArray polygons) {
            this.footprintPolygons = polygons;
            repaint();
        }

        void setGeoSelection(double latMin, double latMax, double lonMin, double lonMax) {
            GeoBBox b = new GeoBBox();
            b.latMin = latMin;
            b.latMax = latMax;
            b.lonMin = lonMin;
            b.lonMax = lonMax;
            this.geoSelection = b;
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

                // Approximate footprint overlay for user context.
                paintFootprint(g2);

                // Approximate mirrored bbox from geographic panel.
                paintMirroredSelection(g2);

                // Title banner
                paintBanner(g2,
                        "Preview image (reference only) — selection is approximate here",
                        8, 8);

            } finally {
                g2.dispose();
            }
        }

        private void paintFootprint(Graphics2D g2) {
            if (footprintPolygons == null || footprintPolygons.isEmpty()) return;

            List<Path2D> paths = buildFootprintPaths();
            if (paths.isEmpty()) return;

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
            if (geoSelection == null) return;

            int x1 = lonToX(geoSelection.lonMin);
            int x2 = lonToX(geoSelection.lonMax);
            int y1 = latToY(geoSelection.latMax);
            int y2 = latToY(geoSelection.latMin);

            Rectangle r = rectFromPoints(new Point(x1, y1), new Point(x2, y2));

            g2.setColor(new Color(0, 200, 255, 45));
            g2.fill(r);
            g2.setColor(new Color(0, 200, 255));
            g2.setStroke(new BasicStroke(2f));
            g2.draw(r);
        }

        private List<Path2D> buildFootprintPaths() {
            List<Path2D> out = new ArrayList<>();
            if (footprintPolygons == null) return out;

            for (Object polyObj : footprintPolygons) {
                List<Double> coords = flattenPolygon(polyObj);
                if (coords.size() < 4) continue;

                Path2D.Double p = new Path2D.Double();
                boolean started = false;
                for (int i = 0; i + 1 < coords.size(); i += 2) {
                    // Existing code path treats these as lat lon pairs.
                    double lat = coords.get(i);
                    double lon = coords.get(i + 1);
                    int x = lonToX(lon);
                    int y = latToY(lat);
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

        private Rectangle rectFromPoints(Point a, Point b) {
            int x = Math.min(a.x, b.x);
            int y = Math.min(a.y, b.y);
            int w = Math.abs(a.x - b.x);
            int h = Math.abs(a.y - b.y);
            return new Rectangle(x, y, w, h);
        }

        private int lonToX(double lon) {
            int w = Math.max(2, getWidth()) - 1;
            lon = clamp(lon, extentLonMin, extentLonMax);
            double pct = (lon - extentLonMin) / (extentLonMax - extentLonMin);
            return (int) Math.round(clamp01(pct) * w);
        }

        private int latToY(double lat) {
            int h = Math.max(2, getHeight()) - 1;
            lat = clamp(lat, extentLatMin, extentLatMax);
            double pct = (extentLatMax - lat) / (extentLatMax - extentLatMin);
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

        private List<Double> flattenPolygon(Object polyObj) {
            List<Double> vals = new ArrayList<>();
            flattenRecursive(polyObj, vals);
            return vals;
        }

        @SuppressWarnings("unchecked")
        private void flattenRecursive(Object obj, List<Double> out) {
            if (obj == null) return;
            if (obj instanceof Number n) {
                out.add(n.doubleValue());
                return;
            }
            if (obj instanceof String s) {
                String[] pieces = s.trim().split("\\s+");
                for (String piece : pieces) {
                    if (!piece.isBlank()) {
                        try {
                            out.add(Double.parseDouble(piece));
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
                return;
            }
            if (obj instanceof org.json.simple.JSONArray arr) {
                for (Object item : arr) {
                    flattenRecursive(item, out);
                }
            }
        }
    }

    @SuppressWarnings("serial")
    private static final class GeoBBoxPanel extends JPanel {
        private final double extentLatMin, extentLatMax, extentLonMin, extentLonMax;
        private boolean clampToExtent = true;

        private Point dragStart;
        private Rectangle selectionPx;
        private boolean dragging;
        private String liveText = "";

        // Primary state: The actual geographic coordinates
        private GeoBBox currentGeoSelection;
        private List<Path2D> cachedFootprintPaths = new ArrayList<>();
        
        private org.json.simple.JSONArray footprintPolygons;
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
                    if (!dragging || dragStart == null) return;
                    Point cur = clampPointToPanel(e.getPoint());
                    selectionPx = rectFromPoints(dragStart, cur);
                    if (clampToExtent) selectionPx = clampRectToPanel(selectionPx);
                    updateLiveText();
                    fireSelectionChanged();
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (!dragging) return;
                    dragging = false;

                    if (selectionPx == null || selectionPx.width < 3 || selectionPx.height < 3) {
                        selectionPx = null;
                        liveText = "";
                        fireSelectionChanged();
                        repaint();
                        return;
                    }

                    if (clampToExtent) selectionPx = clampRectToPanel(selectionPx);
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

        void setFootprintPolygons(org.json.simple.JSONArray polygons) {
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
            if (selectionPx == null || selectionPx.width <= 0 || selectionPx.height <= 0) return null;

            double lon1 = xToLon(selectionPx.x);
            double lon2 = xToLon(selectionPx.x + selectionPx.width - 1);
            double lat1 = yToLat(selectionPx.y);
            double lat2 = yToLat(selectionPx.y + selectionPx.height - 1);

            GeoBBox b = new GeoBBox();
            b.lonMin = Math.min(lon1, lon2);
            b.lonMax = Math.max(lon1, lon2);
            b.latMin = Math.min(lat1, lat2);
            b.latMax = Math.max(lat1, lat2);

            if (clampToExtent) {
                b.lonMin = clamp(b.lonMin, extentLonMin, extentLonMax);
                b.lonMax = clamp(b.lonMax, extentLonMin, extentLonMax);
                b.latMin = clamp(b.latMin, extentLatMin, extentLatMax);
                b.latMax = clamp(b.latMax, extentLatMin, extentLatMax);
            }
            return b;
        }

        void setSelectionFromGeo(double latMin, double latMax, double lonMin, double lonMax) {
            int x1 = lonToX(Math.min(lonMin, lonMax));
            int x2 = lonToX(Math.max(lonMin, lonMax));
            int y1 = latToY(Math.max(latMin, latMax));
            int y2 = latToY(Math.min(latMin, latMax));
            selectionPx = rectFromPoints(new Point(x1, y1), new Point(x2, y2));
            updateLiveText();
            fireSelectionChanged();
            repaint();
        }

        private void fireSelectionChanged() {
            GeoBBox b = getSelectionAsGeo();
            if (b == null) return;
            for (SelectionListener listener : listeners) {
                listener.selectionChanged(b.latMin, b.latMax, b.lonMin, b.lonMax);
            }
        }

        private Rectangle rectFromPoints(Point a, Point b) {
            int x = Math.min(a.x, b.x);
            int y = Math.min(a.y, b.y);
            int w = Math.abs(a.x - b.x);
            int h = Math.abs(a.y - b.y);
            return new Rectangle(x, y, w, h);
        }

        private Rectangle clampRectToPanel(Rectangle r) {
            if (r == null) return null;
            int w = Math.max(2, getWidth()) - 1;
            int h = Math.max(2, getHeight()) - 1;

            int x1 = clampInt(r.x, 0, w);
            int y1 = clampInt(r.y, 0, h);
            int x2 = clampInt(r.x + r.width, 0, w);
            int y2 = clampInt(r.y + r.height, 0, h);
            return rectFromPoints(new Point(x1, y1), new Point(x2, y2));
        }

        private Point clampPointToPanel(Point p) {
            int w = Math.max(2, getWidth()) - 1;
            int h = Math.max(2, getHeight()) - 1;
            return new Point(clampInt(p.x, 0, w), clampInt(p.y, 0, h));
        }

        private double xToLon(int x) {
            int w = Math.max(2, getWidth()) - 1;
            x = clampInt(x, 0, w);
            double pct = (double) x / (double) w;
            return extentLonMin + pct * (extentLonMax - extentLonMin);
        }

        private double yToLat(int y) {
            int h = Math.max(2, getHeight()) - 1;
            y = clampInt(y, 0, h);
            double pct = (double) y / (double) h;
            return extentLatMax - pct * (extentLatMax - extentLatMin);
        }

        private int lonToX(double lon) {
            int w = Math.max(2, getWidth()) - 1;
            lon = clamp(lon, extentLonMin, extentLonMax);
            double pct = (lon - extentLonMin) / (extentLonMax - extentLonMin);
            return (int) Math.round(clamp01(pct) * w);
        }

        private int latToY(double lat) {
            int h = Math.max(2, getHeight()) - 1;
            lat = clamp(lat, extentLatMin, extentLatMax);
            double pct = (extentLatMax - lat) / (extentLatMax - extentLatMin);
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
                if (!dragging) liveText = "";
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
            if (w <= 1 || h <= 1) return;

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
            if (paths.isEmpty()) return;

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
            if (footprintPolygons == null) return out;

            for (Object polyObj : footprintPolygons) {
                List<Double> coords = flattenPolygon(polyObj);
                if (coords.size() < 4) continue;

                Path2D.Double p = new Path2D.Double();
                boolean started = false;
                for (int i = 0; i + 1 < coords.size(); i += 2) {
                    double lat = coords.get(i);
                    double lon = coords.get(i + 1);
                    int x = lonToX(lon);
                    int y = latToY(lat);
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

        private List<Double> flattenPolygon(Object polyObj) {
            List<Double> vals = new ArrayList<>();
            flattenRecursive(polyObj, vals);
            return vals;
        }

        @SuppressWarnings("unchecked")
        private void flattenRecursive(Object obj, List<Double> out) {
            if (obj == null) return;
            if (obj instanceof Number n) {
                out.add(n.doubleValue());
                return;
            }
            if (obj instanceof String s) {
                String[] pieces = s.trim().split("\\s+");
                for (String piece : pieces) {
                    if (!piece.isBlank()) {
                        try {
                            out.add(Double.parseDouble(piece));
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
                return;
            }
            if (obj instanceof org.json.simple.JSONArray arr) {
                for (Object item : arr) {
                    flattenRecursive(item, out);
                }
            }
        }
    }
}
