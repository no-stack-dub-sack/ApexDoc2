package main;

import java.util.List;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.Collections;
import java.util.Comparator;

public class ClassModel extends OuterModel {

    private boolean isInterface;
    private ClassModel cmodelParent;
    private ArrayList<MethodModel> methods;
    private ArrayList<PropertyModel> properties;
    private ArrayList<ClassModel> childClasses;
    private ArrayList<EnumModel> enums;

    public ClassModel(ClassModel cmodelParent, ArrayList<String> comments, String nameLine, int lineNum) {
        super(comments, ModelType.CLASS);
        super.setNameLine(nameLine, lineNum);

        this.cmodelParent = cmodelParent;
        this.childClasses = new ArrayList<ClassModel>();
        this.methods = new ArrayList<MethodModel>();
        this.properties = new ArrayList<PropertyModel>();
        this.enums = new ArrayList<EnumModel>();

        if (nameLine.toLowerCase().contains(" " + ApexDoc.INTERFACE + " ")) {
            this.isInterface = true;
        }
    }

    public String getExample() {
        // return example and remove trailing white space which
        // may have built up due to the allowance of preserving
        // white pace in complex code example blocks for methods
        return example == null ? "" : example.replace("\\s+$", "");
    }

    public String getSee() {
        return see == null ? "" : see;
    }

    public ArrayList<EnumModel> getEnums() {
        return enums;
    }

    public ArrayList<PropertyModel> getProperties() {
        return properties;
    }

    public ArrayList<PropertyModel> getPropertiesSorted() {
        TreeMap<String, PropertyModel> tm = new TreeMap<String, PropertyModel>();

        for (PropertyModel prop : properties) {
            tm.put(prop.getPropertyName().toLowerCase(), prop);
        }

        return new ArrayList<PropertyModel>(tm.values());
    }

    public ArrayList<MethodModel> getMethods() {
        // ensure interface methods take the
        // scope of their defining type
        if (this.isInterface) {
            for (MethodModel method : methods) {
                method.scope = this.getScope();
            }
        }

        return methods;
    }

    public ArrayList<MethodModel> getMethodsSorted() {
        @SuppressWarnings("unchecked")
		List<MethodModel> sorted = (List<MethodModel>)methods.clone();
        Collections.sort(sorted, new Comparator<MethodModel>() {
            public int compare(MethodModel o1, MethodModel o2) {
                String methodName1 = o1.getMethodName();
                String methodName2 = o2.getMethodName();
                String className = getName();

                if (methodName1.equals(className)) {
                    return Integer.MIN_VALUE;
                } else if (methodName2.equals(className)) {
                    return Integer.MAX_VALUE;
                }

                return (methodName1.toLowerCase().compareTo(methodName2.toLowerCase()));
            }
        });
        return new ArrayList<MethodModel>(sorted);
    }

    public void setMethods(ArrayList<MethodModel> methods) {
        this.methods = methods;
    }

    public ArrayList<ClassModel> getChildClasses() {
        return childClasses;
    }

    public ArrayList<ClassModel> getChildClassesSorted() {
        TreeMap<String, ClassModel> tm = new TreeMap<String, ClassModel>();

        for (ClassModel cm : childClasses) {
            tm.put(cm.getName().toLowerCase(), cm);
        }

        return new ArrayList<ClassModel>(tm.values());
    }

    public void addChildClass(ClassModel child) {
        childClasses.add(child);
    }

    public String getName() {
        String nameLine = getNameLine();
        String parent = cmodelParent == null ? "" : cmodelParent.getName() + ".";

        if (nameLine != null) {
            nameLine = nameLine.trim();
        }

        if (nameLine != null && nameLine.trim().length() > 0) {
            int keywordAt = nameLine.toLowerCase().indexOf(ApexDoc.CLASS + " ");

            int offset = 6;
            if (keywordAt == -1) {
                keywordAt = nameLine.toLowerCase().indexOf(ApexDoc.INTERFACE + " ");
                offset = 10;
            }

            if (keywordAt > -1) {
                nameLine = nameLine.substring(keywordAt + offset).trim();
            }

            int spaceAt = nameLine.indexOf(" ");
            if (spaceAt == -1) {
                return parent + nameLine;
            }

            try {
                String name = nameLine.substring(0, spaceAt);
                return parent + name;
            } catch (Exception ex) {
                return parent + nameLine.substring(nameLine.lastIndexOf(" ") + 1);
            }

        } else {
            return "";
        }
    }

    public String getTopmostClassName() {
        if (cmodelParent != null) {
            return cmodelParent.getName();
        } else {
            return getName();
        }
    }

    public String getGroupName() {
        String group;
        if (this.cmodelParent != null) {
            group = cmodelParent.getGroupName();
        } else {
            group = groupName;
        }

        return group.isEmpty() ? null : group;
    }

    public boolean getIsInterface() {
        return isInterface;
    }
}
