package main;
import java.util.ArrayList;

public class EnumModel extends ApexModel {

    private ArrayList<String> values = new ArrayList<String>();

    EnumModel(ArrayList<String> comments, String nameLine, int lineNum) {
        super(comments);
        this.setNameLine(nameLine, lineNum);
    }

    protected void setNameLine(String nameLine, int lineNum) {
        if (nameLine != null) {
            // remove any trailing stuff after property name. { =
            int i = nameLine.indexOf('{');
            if (i > 0) nameLine = nameLine.substring(0, i);
        }

        super.setNameLine(nameLine, lineNum);
    }

    public ArrayList<String> getValues() {
        return values;
    }
}