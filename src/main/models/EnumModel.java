package main.models;

import main.ApexDoc;
import java.util.ArrayList;

public class EnumModel extends TopLevelModel {

    private ArrayList<String> values = new ArrayList<String>();

    public EnumModel(ArrayList<String> comments, String nameLine, int lineNum) {
        super(comments, ModelType.ENUM);
        this.setNameLine(nameLine, lineNum);
    }

    public String getName() {
        // public enum YEM
        String nameLine = this.getNameLine();
        int i = nameLine.indexOf(ApexDoc.ENUM);
        return nameLine.substring(i + ApexDoc.ENUM.length()).trim();
    }

    public String getGroupName() {
        return groupName;
    }

    protected void setNameLine(String nameLine, int lineNum) {
        if (nameLine != null) {
            // remove any trailing stuff after enum name
            int i = nameLine.indexOf('{');
            if (i > 0) nameLine = nameLine.substring(0, i);
        }

        super.setNameLine(nameLine.trim(), lineNum);
    }

    public ArrayList<String> getValues() {
        return values;
    }
}