package main.models;

import main.Utils;
import java.util.ArrayList;

public class MethodModel extends ApexModel {

    public MethodModel(ArrayList<String> comments, String nameLine, int lineNum) {
        super(comments);
        this.setNameLine(nameLine, lineNum);
    }

    protected void setNameLine(String nameLine, int lineNum) {
        // remove anything after the parameter list
        if (nameLine != null) {
            int i = nameLine.lastIndexOf(")");
            if (i >= 0) {
                nameLine = nameLine.substring(0, i + 1);
            }
        }
        super.setNameLine(nameLine, lineNum);
    }

    public ArrayList<String> getParams() {
        return params;
    }

    public String getException() {
        return exception == null ? "" : exception;
    }

    public String getReturns() {
        return returns == null ? "" : returns;
    }

    public String getMethodName() {
        String nameLine = getNameLine();
        if (nameLine != null && nameLine.trim().length() > 0) {
            nameLine = nameLine.trim();
            int lastindex = nameLine.indexOf("(");
            if (lastindex >= 0) {
                String methodName = Utils.previousWord(nameLine, lastindex);
                return methodName == null ? "" : methodName;
            }
        }
        return "";
    }
}
