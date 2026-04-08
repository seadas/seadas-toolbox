package gov.nasa.gsfc.seadas.earthdatacloud.data;

public class VariableItem {
    public final String fullName;
    public final String shortName;
    public final String groupName;

    public VariableItem(String fullName, String shortName, String groupName) {
        this.fullName = fullName;
        this.shortName = shortName;
        this.groupName = groupName;
    }

    @Override
    public String toString() {
        return fullName;
    }

    public String getFullName() { return fullName; }
    public String getShortName() { return shortName; }
    public String getGroupName() { return groupName; }
}