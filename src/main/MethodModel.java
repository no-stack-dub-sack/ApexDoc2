package main;

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
        String nameLine = getNameLine().trim();
        if (nameLine != null && nameLine.length() > 0) {
            int lastindex = nameLine.indexOf("(");
            if (lastindex >= 0) {
                String methodName = ApexDoc.previousWord(nameLine, lastindex);
                return methodName;
            }
        }
        return "";
    }
}
