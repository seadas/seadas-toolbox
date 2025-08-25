package gov.nasa.gsfc.seadas.panoply;

import java.io.IOException;

import org.esa.snap.core.datamodel.*;
import ucar.nc2.*;
import ucar.nc2.dataset.*;

import ucar.nc2.constants.CF;
import java.util.List;

public final class PanoplyStyleMetadataBuilder {

    private PanoplyStyleMetadataBuilder() {}

    /** Attach a Panoply-style metadata subtree to a product. */
    public static void attachPanoplyMetadata(Product product, String fileUrlOrPath) throws IOException {
        try (NetcdfFile nc = NetcdfFiles.open(fileUrlOrPath)) {
            MetadataElement root = new MetadataElement("Panoply");

            root.addElement(buildGlobalAttributes(nc));
            root.addElement(buildDimensions(nc));
            root.addElement(buildVariables(nc));
            root.addElement(buildCoordinateSystems(fileUrlOrPath)); // open dataset separately
            root.addElement(buildCoverageSummary(nc));

            product.getMetadataRoot().addElement(root);
        }
    }

    private static MetadataElement buildGlobalAttributes(NetcdfFile nc) {
        MetadataElement e = new MetadataElement("Global_Attributes");
        for (Attribute a : nc.getGlobalAttributes()) {
            e.addAttribute(toMetaAttr(a));
        }
        return e;
    }

    private static MetadataElement buildDimensions(NetcdfFile nc) {
        MetadataElement e = new MetadataElement("Dimensions");
        for (Dimension d : nc.getDimensions()) {
            MetadataElement de = new MetadataElement(d.getShortName());
            de.addAttribute(new MetadataAttribute("length",
                    ProductData.createInstance(d.getLength()), true));
            // ProductData has no boolean factory → store as string
            de.addAttribute(new MetadataAttribute("isUnlimited",
                    ProductData.createInstance(Boolean.toString(d.isUnlimited())), true));
            e.addElement(de);
        }
        return e;
    }

    private static MetadataElement buildVariables(NetcdfFile nc) {
        MetadataElement e = new MetadataElement("Variables");
        for (Variable v : nc.getVariables()) {
            MetadataElement ve = new MetadataElement(v.getShortName());
            ve.addAttribute(new MetadataAttribute("dataType",
                    ProductData.createInstance(v.getDataType().toString()), true));
            ve.addAttribute(new MetadataAttribute("shape",
                    ProductData.createInstance(shapeString(v.getShape())), true));
            ve.addAttribute(new MetadataAttribute("dimensions",
                    ProductData.createInstance(dimNames(v.getDimensions())), true));

            MetadataElement attrs = new MetadataElement("Attributes");
            for (Attribute a : v.attributes()) {
                attrs.addAttribute(toMetaAttr(a));
            }
            ve.addElement(attrs);

            addIfPresent(ve, CF.STANDARD_NAME, v.findAttributeIgnoreCase(CF.STANDARD_NAME));
            addIfPresent(ve, CF.LONG_NAME,     v.findAttributeIgnoreCase(CF.LONG_NAME));
            addIfPresent(ve, CF.UNITS,         v.findAttributeIgnoreCase(CF.UNITS));
            addIfPresent(ve, "coordinates",    v.findAttributeIgnoreCase(CF.COORDINATES));
            addIfPresent(ve, "grid_mapping",   v.findAttributeIgnoreCase(CF.GRID_MAPPING));
            addIfPresent(ve, "bounds",         v.findAttributeIgnoreCase(CF.BOUNDS));

            e.addElement(ve);
        }
        return e;
    }

    /** Use NetcdfDatasets.openDataset(...) to avoid Enhance enum differences across versions. */
    private static MetadataElement buildCoordinateSystems(String fileUrlOrPath) throws IOException {
        try (NetcdfDataset ds = NetcdfDatasets.openDataset(fileUrlOrPath)) {
            MetadataElement e = new MetadataElement("Coordinate_Systems");
            for (ucar.nc2.dataset.CoordinateSystem cs : ds.getCoordinateSystems()) {
                MetadataElement cse = new MetadataElement(cs.getName() != null ? cs.getName() : "coord_sys");
                cse.addAttribute(new MetadataAttribute("isLatLon",
                        ProductData.createInstance(Boolean.toString(cs.isLatLon())), true));
                cse.addAttribute(new MetadataAttribute("domainRank",
                        ProductData.createInstance(cs.getDomain().size()), true));

                // Axes
                MetadataElement axes = new MetadataElement("Axes");
                for (CoordinateAxis ax : cs.getCoordinateAxes()) {
                    MetadataElement axe = new MetadataElement(ax.getShortName());
                    axe.addAttribute(new MetadataAttribute("axisType",
                            ProductData.createInstance(String.valueOf(ax.getAxisType())), true));
                    axe.addAttribute(new MetadataAttribute("dataType",
                            ProductData.createInstance(ax.getDataType().toString()), true));
                    axe.addAttribute(new MetadataAttribute("units",
                            ProductData.createInstance(nvl(ax.getUnitsString())), true));
                    axe.addAttribute(new MetadataAttribute("shape",
                            ProductData.createInstance(shapeString(ax.getShape())), true));
                    addIfPresent(axe, CF.STANDARD_NAME, ax.findAttributeIgnoreCase(CF.STANDARD_NAME));
                    addIfPresent(axe, CF.LONG_NAME,     ax.findAttributeIgnoreCase(CF.LONG_NAME));
                    axes.addElement(axe);
                }
                cse.addElement(axes);

                // Variables mapped to this CS (optional, cheap)
                MetadataElement mapped = new MetadataElement("Variables_Mapped");
                for (Variable v : ds.getVariables()) {
                    if (cs.isCoordinateSystemFor(v)) {
                        mapped.addElement(new MetadataElement(v.getShortName()));
                    }
                }
                cse.addElement(mapped);

                e.addElement(cse);
            }
            return e;
        }
    }

    private static MetadataElement buildCoverageSummary(NetcdfFile nc) {
        MetadataElement e = new MetadataElement("Coverage_Summary");
        addIfPresent(e, "time_coverage_start", nc.findGlobalAttributeIgnoreCase("time_coverage_start"));
        addIfPresent(e, "time_coverage_end",   nc.findGlobalAttributeIgnoreCase("time_coverage_end"));
        addIfPresent(e, "geospatial_lat_min",  nc.findGlobalAttributeIgnoreCase("geospatial_lat_min"));
        addIfPresent(e, "geospatial_lat_max",  nc.findGlobalAttributeIgnoreCase("geospatial_lat_max"));
        addIfPresent(e, "geospatial_lon_min",  nc.findGlobalAttributeIgnoreCase("geospatial_lon_min"));
        addIfPresent(e, "geospatial_lon_max",  nc.findGlobalAttributeIgnoreCase("geospatial_lon_max"));
        addIfPresent(e, "geospatial_vertical_min", nc.findGlobalAttributeIgnoreCase("geospatial_vertical_min"));
        addIfPresent(e, "geospatial_vertical_max", nc.findGlobalAttributeIgnoreCase("geospatial_vertical_max"));
        return e;
    }

    // ---------- helpers ----------

    private static void addIfPresent(MetadataElement e, String name, Attribute a) {
        if (a != null) {
            e.addAttribute(new MetadataAttribute(name, ProductData.createInstance(attrString(a)), true));
        }
    }

    private static MetadataAttribute toMetaAttr(Attribute a) {
        return new MetadataAttribute(a.getShortName(), ProductData.createInstance(attrString(a)), true);
    }

    private static String attrString(Attribute a) {
        if (a.isString()) return a.getStringValue();
        if (a.getLength() == 1) return String.valueOf(a.getNumericValue());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < a.getLength(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(String.valueOf(a.getNumericValue(i)));
        }
        return sb.toString();
    }

    private static String shapeString(int[] shape) {
        if (shape == null || shape.length == 0) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < shape.length; i++) {
            if (i > 0) sb.append(" x ");
            sb.append(shape[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    private static String dimNames(List<Dimension> dims) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < dims.size(); i++) {
            if (i > 0) sb.append(" ");
            sb.append(dims.get(i).getShortName());
        }
        return sb.toString();
    }

    private static String nvl(String s) { return s == null ? "" : s; }
}
