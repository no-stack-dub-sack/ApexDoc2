package main;

import java.util.ArrayList;

public class MethodModel extends ApexModel {

    private ArrayList<String> params;
    private String exceptions;

    public MethodModel() {
        params = new ArrayList<String>();
    }

    public void setNameLine(String nameLine, int lineNum) {
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

    public void setException(String exceptions) {
        this.exceptions = exceptions;
    }

    public String getException() {
        return exceptions == null ? "" : exceptions;
    }

    public void setParams(ArrayList<String> params) {
        this.params = params;
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
