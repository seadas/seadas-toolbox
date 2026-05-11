package gov.nasa.gsfc.seadas.earthdatacloud.ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;

/**
 * BBoxSelectionDialog (preview-image background)
 *
 * Shows a preview image (browse/quicklook) for the currently-selected granule and lets the user
 * draw a bounding box. Converts pixel coords -> geo coords using a provided geo extent:
 *
 *   lon = minLon + (x / width)  * (maxLon - minLon)
 *   lat = maxLat - (y / height) * (maxLat - minLat)
 *
 * Assumes the preview image is displayed in geographic lon/lat (EPSG:4326) and roughly rectified
 * over the bbox extent. Good enough for a v1 “draw bbox” UX.
 */
public class BBoxSelectionDialog extends JDialog {

    private final PreviewBBoxPanel panel;

    private boolean confirmed = false;
    private double latMin, latMax, lonMin, lonMax;

    public BBoxSelectionDialog(Window owner,
                               BufferedImage previewImage,
                               double extentLatMin, double extentLatMax,
                               double extentLonMin, double extentLonMax) {
        super(owner, "Draw Bounding Box", ModalityType.APPLICATION_MODAL);

        // Normalize extent
        double eLatMin = Math.min(extentLatMin, extentLatMax);
        double eLatMax = Math.max(extentLatMin, extentLatMax);
        double eLonMin = Math.min(extentLonMin, extentLonMax);
        double eLonMax = Math.max(extentLonMin, extentLonMax);

        this.panel = new PreviewBBoxPanel(previewImage, eLatMin, eLatMax, eLonMin, eLonMax);

        setLayout(new BorderLayout(8, 8));
        add(panel, BorderLayout.CENTER);
        add(createBottomPanel(), BorderLayout.SOUTH);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setPreferredSize(new Dimension(980, 620));
        pack();
        setLocationRelativeTo(owner);

        // ESC closes
        getRootPane().registerKeyboardAction(e -> {
            confirmed = false;
            dispose();
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    /** Optional: pre-fill an initial selection bbox (must be inside extent to show correctly). */
    public void setInitialBBox(double latMin, double latMax, double lonMin, double lonMax) {
        panel.setSelectionFromGeo(latMin, latMax, lonMin, lonMax);
    }

    /** Optional: enable/disable clamping selection to extent. Default is true. */
    public void setClampToExtent(boolean clamp) {
        panel.setClampToExtent(clamp);
    }

    public boolean isConfirmed() { return confirmed; }
    public double getLatMin() { return latMin; }
    public double getLatMax() { return latMax; }
    public double getLonMin() { return lonMin; }
    public double getLonMax() { return lonMax; }

    private JPanel createBottomPanel() {
        JPanel bottom = new JPanel(new BorderLayout(8, 8));

        JLabel hint = new JLabel("Drag to draw a box. Right-click to clear.");
        bottom.add(hint, BorderLayout.WEST);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton clear = new JButton("Clear");
        clear.addActionListener(e -> panel.clearSelection());

        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> {
            confirmed = false;
            dispose();
        });

        JButton ok = new JButton("OK");
        ok.addActionListener(e -> {
            if (!panel.hasSelection()) {
                JOptionPane.showMessageDialog(this,
                        "Please draw a bounding box first.",
                        "No selection",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            GeoBBox b = panel.getSelectionAsGeo();

            // Final normalize + clamp just in case
            latMin = Math.max(-90, Math.min(90, Math.min(b.latMin, b.latMax)));
            latMax = Math.max(-90, Math.min(90, Math.max(b.latMin, b.latMax)));
            lonMin = Math.max(-180, Math.min(180, Math.min(b.lonMin, b.lonMax)));
            lonMax = Math.max(-180, Math.min(180, Math.max(b.lonMin, b.lonMax)));

            // Optional warning for huge boxes (prevents 504 surprises)
            double latSpan = Math.abs(latMax - latMin);
            double lonSpan = Math.abs(lonMax - lonMin);
            if (latSpan > 10 || lonSpan > 10) {
                int choice = JOptionPane.showConfirmDialog(this,
                        String.format("This is a large selection (lat span %.2f°, lon span %.2f°).\nIt may time out (504).\n\nContinue?",
                                latSpan, lonSpan),
                        "Large Selection",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                if (choice != JOptionPane.YES_OPTION) return;
            }

            confirmed = true;
            dispose();
        });

        buttons.add(clear);
        buttons.add(cancel);
        buttons.add(ok);

        bottom.add(buttons, BorderLayout.EAST);
        return bottom;
    }

    private static class GeoBBox {
        double latMin, latMax, lonMin, lonMax;
    }

    public void setFootprintPolygons(org.json.simple.JSONArray polygons) {
        panel.setFootprintPolygons(polygons);
    }
    /**
     * Panel that loads and displays a preview image and supports drag-selection.
     * Uses the provided geo extent for pixel->geo conversion.
     */
// Drop-in replacement for PreviewBBoxPanel inside BBoxSelectionDialog
    private static class PreviewBBoxPanel extends JPanel {

        private final BufferedImage image;

        // Geo extent for the image (EPSG:4326 style linear mapping)
        private final double extentLatMin, extentLatMax, extentLonMin, extentLonMax;

        private boolean clampToExtent = true;

        // Selection state in panel pixel coords
        private Rectangle selectionPx;
        private Point dragStart;
        private boolean dragging;

        // Live status text (drawn by paintComponent; no embedded Swing components)
        private String liveText = "";

        private org.json.simple.JSONArray footprintPolygons;

        PreviewBBoxPanel(BufferedImage previewImage,
                         double extentLatMin, double extentLatMax,
                         double extentLonMin, double extentLonMax) {

            this.image = java.util.Objects.requireNonNull(previewImage, "previewImage");

            // Normalize extent
            this.extentLatMin = Math.min(extentLatMin, extentLatMax);
            this.extentLatMax = Math.max(extentLatMin, extentLatMax);
            this.extentLonMin = Math.min(extentLonMin, extentLonMax);
            this.extentLonMax = Math.max(extentLonMin, extentLonMax);

            setOpaque(true);
            setBackground(Color.BLACK);

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
                    repaint();
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (!dragging || dragStart == null) return;
                    Point cur = clampPointToPanel(e.getPoint());
                    selectionPx = rectFromPoints(dragStart, cur);
                    if (clampToExtent) selectionPx = clampRectToPanel(selectionPx);
                    updateLiveText();
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (!dragging) return;
                    dragging = false;

                    if (selectionPx == null || selectionPx.width < 3 || selectionPx.height < 3) {
                        selectionPx = null;
                        liveText = "";
                        repaint();
                        return;
                    }

                    if (clampToExtent) selectionPx = clampRectToPanel(selectionPx);
                    updateLiveText();
                    repaint();
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    // Optional: show cursor lat/lon even when not selecting
                    // Comment out if you prefer only showing selection bbox.
                    Point p = clampPointToPanel(e.getPoint());
                    liveText = String.format("Cursor  lat=%.6f  lon=%.6f", yToLat(p.y), xToLon(p.x));
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    // Clear cursor info when leaving panel, keep selection info if any
                    updateLiveText();
                    repaint();
                }
            };

            addMouseListener(mouse);
            addMouseMotionListener(mouse);

            // Improve rendering quality for scaled image
            setDoubleBuffered(true);
            double lonSpan = Math.abs(this.extentLonMax - this.extentLonMin);
            double latSpan = Math.abs(this.extentLatMax - this.extentLatMin);
            if (lonSpan < 1e-6 || latSpan < 1e-6) {
                throw new IllegalArgumentException("Invalid extent span: latSpan=" + latSpan + ", lonSpan=" + lonSpan);
            }
        }

        void setFootprintPolygons(org.json.simple.JSONArray polygons) {
            this.footprintPolygons = polygons;
            repaint();
        }

        void setClampToExtent(boolean clampToExtent) {
            this.clampToExtent = clampToExtent;
        }

        boolean hasSelection() {
            return selectionPx != null && selectionPx.width > 0 && selectionPx.height > 0;
        }

        void clearSelection() {
            selectionPx = null;
            dragging = false;
            dragStart = null;
            liveText = "";
            repaint();
        }

        GeoBBox getSelectionAsGeo() {
            if (!hasSelection()) return null;

            // Convert selection rectangle to geo bbox using extent
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
            double aLatMin = Math.min(latMin, latMax);
            double aLatMax = Math.max(latMin, latMax);
            double aLonMin = Math.min(lonMin, lonMax);
            double aLonMax = Math.max(lonMin, lonMax);

            if (clampToExtent) {
                aLatMin = clamp(aLatMin, extentLatMin, extentLatMax);
                aLatMax = clamp(aLatMax, extentLatMin, extentLatMax);
                aLonMin = clamp(aLonMin, extentLonMin, extentLonMax);
                aLonMax = clamp(aLonMax, extentLonMin, extentLonMax);
            }

            int x1 = lonToX(aLonMin);
            int x2 = lonToX(aLonMax);
            int y1 = latToY(aLatMax); // y down, so top is maxLat
            int y2 = latToY(aLatMin);

            selectionPx = rectFromPoints(new Point(x1, y1), new Point(x2, y2));
            if (clampToExtent) selectionPx = clampRectToPanel(selectionPx);

            updateLiveText();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();
            try {
                // Higher quality scaling
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Draw background image
                g2.drawImage(image, 0, 0, getWidth(), getHeight(), this);

                // Draw footprint polygon
                drawFootprintOverlay(g2);

                // Draw selection
                if (selectionPx != null) {
                    g2.setColor(new Color(0, 120, 255, 70));
                    g2.fillRect(selectionPx.x, selectionPx.y, selectionPx.width, selectionPx.height);
                    g2.setColor(new Color(0, 170, 255, 220));
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawRect(selectionPx.x, selectionPx.y, selectionPx.width, selectionPx.height);
                }

                // Draw live text overlay (top-left)
                if (liveText != null && !liveText.isBlank()) {
                    FontMetrics fm = g2.getFontMetrics();
                    int pad = 8;
                    int textW = fm.stringWidth(liveText);
                    int textH = fm.getHeight();
                    int x = 10;
                    int y = 10;

                    g2.setColor(new Color(0, 0, 0, 160));
                    g2.fillRoundRect(x, y, textW + 2 * pad, textH + 2 * pad, 10, 10);

                    g2.setColor(Color.WHITE);
                    g2.drawString(liveText, x + pad, y + pad + fm.getAscent());
                }
            } finally {
                g2.dispose();
            }
        }

        private void updateLiveText() {
            if (!hasSelection()) {
                // Keep empty unless mouseMoved sets cursor text
                if (dragging) liveText = "";
                return;
            }
            GeoBBox b = getSelectionAsGeo();
            if (b == null) {
                liveText = "";
                return;
            }
            liveText = String.format("Selection  lat(%.6f : %.6f)  lon(%.6f : %.6f)",
                    b.latMin, b.latMax, b.lonMin, b.lonMax);
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

        // --- Pixel <-> Geo conversions using extent (fixed: uses w-1/h-1 and clamping) ---

        private double xToLon(int x) {
            int w = Math.max(2, getWidth()) - 1;   // max pixel index
            x = clampInt(x, 0, w);
            double pct = (double) x / (double) w;  // 0..1
            return extentLonMin + pct * (extentLonMax - extentLonMin);
        }

        private double yToLat(int y) {
            int h = Math.max(2, getHeight()) - 1;  // max pixel index
            y = clampInt(y, 0, h);
            double pct = (double) y / (double) h;  // 0..1
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

        private double clamp(double v, double min, double max) {
            return Math.max(min, Math.min(max, v));
        }

        private int clampInt(int v, int min, int max) {
            return Math.max(min, Math.min(max, v));
        }
        private void drawFootprintOverlay(Graphics2D g2) {
            org.json.simple.JSONArray polys = this.footprintPolygons;
            if (polys == null || polys.isEmpty()) {
                return;
            }

            // Fill + outline styling (no external UI components)
            Composite oldComposite = g2.getComposite();
            Stroke oldStroke = g2.getStroke();

            try {
                // Slightly transparent fill
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.18f));
                g2.setColor(Color.YELLOW);

                // Stronger outline
                Stroke outlineStroke = new BasicStroke(2.0f);
                Color outlineColor = new Color(255, 255, 0, 200);

                // polys can be: [ [ "lat lon lat lon ..." ] ] or [ "lat lon ..." ] depending on API
                for (Object polyObj : polys) {
                    if (polyObj == null) continue;

                    if (polyObj instanceof org.json.simple.JSONArray polyArr) {
                        for (Object ringObj : polyArr) {
                            drawOneRing(g2, ringObj, outlineStroke, outlineColor);
                        }
                    } else {
                        drawOneRing(g2, polyObj, outlineStroke, outlineColor);
                    }
                }

            } finally {
                g2.setComposite(oldComposite);
                g2.setStroke(oldStroke);
            }
        }

        private void drawOneRing(Graphics2D g2,
                                 Object ringObj,
                                 Stroke outlineStroke,
                                 Color outlineColor) {
            if (ringObj == null) return;

            String ring = ringObj.toString().trim();
            if (ring.isEmpty()) return;

            String[] parts = ring.split("\\s+");
            if (parts.length < 4) return; // need at least 2 points

            // Build a Path2D from lat/lon pairs
            Path2D.Double path = new Path2D.Double();

            boolean started = false;
            for (int i = 0; i + 1 < parts.length; i += 2) {
                double lat;
                double lon;
                try {
                    lat = Double.parseDouble(parts[i]);
                    lon = Double.parseDouble(parts[i + 1]);
                } catch (NumberFormatException nfe) {
                    continue;
                }

                int x = lonToX(lon);
                int y = latToY(lat);

                if (!started) {
                    path.moveTo(x, y);
                    started = true;
                } else {
                    path.lineTo(x, y);
                }
            }

            if (!started) return;

            path.closePath();

            // Fill
            g2.fill(path);

            // Outline on top
            Composite old = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            g2.setColor(outlineColor);
            g2.setStroke(outlineStroke);
            g2.draw(path);
            g2.setComposite(old);
        }
    }}