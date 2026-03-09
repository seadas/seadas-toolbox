package gov.nasa.gsfc.seadas.earthdatacloud.util;

public interface GeoMapper {
    /** Converts Panel Pixel (x,y) to Lat/Lon */
    double[] pixelToLatLon(int x, int y, int width, int height);
}