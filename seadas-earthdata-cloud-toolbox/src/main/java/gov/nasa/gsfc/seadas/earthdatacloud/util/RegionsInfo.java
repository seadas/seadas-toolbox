package gov.nasa.gsfc.seadas.earthdatacloud.util;

public class RegionsInfo {

    private String name;
    private boolean isRegion = false;
    private String lat;
    private String lon;
    private String north;
    private String south;
    private String west;
    private String east;

    public final static String SPECIAL_ENTRY = "-999";
    public final static double SPECIAL_ENTRY_DOUBLE = -999;

    public RegionsInfo(String name, String north, String south, String west, String east) {
        this.setName(name);
        this.setNorth(north);
        this.setSouth(south);
        this.setWest(west);
        this.setEast(east);
        this.isRegion = true;
    }

    public RegionsInfo(String name, String lat, String lon, String north, String south, String west, String east) {
        this.setName(name);
        this.setNorth(north);
        this.setSouth(south);
        this.setWest(west);
        this.setEast(east);
        this.lat = lat;
        this.lon = lon;
        this.isRegion = false;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCoordinates() {
        if (lat != null && lat.length() > 0 && lon != null && lon.length() > 0) {
            return lat + " " + lon;
        }
        return "";
    }

    public String getNorth() {
        return north;
    }

    public void setNorth(String north) {
        this.north = north;
    }

    public String getSouth() {
        return south;
    }

    public void setSouth(String south) {
        this.south = south;
    }

    public String getWest() {
        return west;
    }

    public void setWest(String west) {
        this.west = west;
    }

    public String getEast() {
        return east;
    }

    public void setEast(String east) {
        this.east = east;
    }

    public String toString() {
        return name;
    }

    public String getLat() {
        return lat;
    }

    public String getLon() {
        return lon;
    }

    public boolean isRegion() {
        return isRegion;
    }
}
