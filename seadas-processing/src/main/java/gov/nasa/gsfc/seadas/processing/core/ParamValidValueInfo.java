package gov.nasa.gsfc.seadas.processing.core;

/**
 * A ...
 *
 * @author Danny Knowles
 * @since SeaDAS 7.0
 */
public class ParamValidValueInfo implements Comparable<ParamValidValueInfo>, Cloneable {

    private String value = null;
    private String description = null;
    private boolean selected;
    private boolean selectedNegated = false;


    public ParamValidValueInfo(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public String getShortDescription(int maxLength) {
        if (description != null && description.length() > maxLength) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(description.substring(0,maxLength-1)).append(" ...");
         return stringBuilder.toString();
        } else {
        return description;
        }
    }

    public void setDescription(String description) {
        this.description = description;
    }

//    @Override
//    public int compareTo(Object object) {
//        return getValue().compareToIgnoreCase(((ParamValidValueInfo) object).getValue());
//    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder("");

        if (value != null) {
            stringBuilder.append(value);

            if (description != null) {
                stringBuilder.append(" - " + getShortDescription(70));
            }
        }

        return stringBuilder.toString();
    }

    @Override
    public ParamValidValueInfo clone() {
        ParamValidValueInfo validValueInfo = new ParamValidValueInfo(value);
        validValueInfo.description = description;
        return validValueInfo;
    }

    public boolean isSelected() {
        return selected;
    }

    public boolean isSelectedNegated() {
        return selectedNegated;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public void setSelectedNegated(boolean selectedNegated) {
        this.selectedNegated = selectedNegated;
    }


    @Override
    public int compareTo(ParamValidValueInfo o) {
        return getValue().compareToIgnoreCase((o).getValue());
    }
}
