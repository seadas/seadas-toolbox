package gov.nasa.gsfc.seadas.earthdatacloud.util;

public class RegionsInfo {

    private String name;
    private String north;
    private String south;
    private String west;
    private String east;

    public RegionsInfo(String name, String north, String south, String west, String east) {
        this.setName(name);
        this.setNorth(north);
        this.setSouth(south);
        this.setWest(west);
        this.setEast(east);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
}
