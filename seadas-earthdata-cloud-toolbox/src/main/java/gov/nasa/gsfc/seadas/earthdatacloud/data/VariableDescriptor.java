package gov.nasa.gsfc.seadas.earthdatacloud.data;

public class VariableDescriptor {
    private final String fullName;      // e.g. geophysical_data/chlor_a
    private final String shortName;     // e.g. chlor_a
    private final String groupName;     // e.g. geophysical_data

    public VariableDescriptor(String fullName, String shortName, String groupName) {
        this.fullName = fullName;
        this.shortName = shortName;
        this.groupName = groupName;
    }

    public String getFullName() {
        return fullName;
    }

    public String getShortName() {
        return shortName;
    }

    public String getGroupName() {
        return groupName;
    }
}
