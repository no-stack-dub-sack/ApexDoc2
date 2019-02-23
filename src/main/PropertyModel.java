package main;

import java.util.ArrayList;

public class PropertyModel extends ApexModel {

    public PropertyModel(ArrayList<String> comments, String nameLine, int lineNum) {
        super(comments);
        this.setNameLine(nameLine, lineNum);
    }

    protected void setNameLine(String nameLine, int lineNum) {
        if (nameLine != null) {
            // remove any trailing stuff after property name. { =
            int i = nameLine.indexOf('{');
            if (i == -1) i = nameLine.indexOf('=');
            if (i == -1) i = nameLine.indexOf(';');
            if (i >= 0) nameLine = nameLine.substring(0, i);
        }

        super.setNameLine(nameLine, lineNum);
    }

    public String getPropertyName() {
        String nameLine = getNameLine().trim();
        if (nameLine != null && nameLine.length() > 0) {
            int lastindex = nameLine.lastIndexOf(" ");
            if (lastindex >= 0) {
                String propertyName = nameLine.substring(lastindex + 1);
                return propertyName;
            }
        }
        return "";
    }
}