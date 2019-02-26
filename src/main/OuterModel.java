package main;

import java.util.ArrayList;

public abstract class OuterModel extends ApexModel {
    public enum ModelType {
        CLASS,
        ENUM
    }

    public ModelType modelType;

    OuterModel(ArrayList<String> comments, ModelType modelType) {
        super(comments);
        this.modelType = modelType;
    }

    public abstract String getName();

    public abstract String getGroupName();

    public String getGroupContentPath() {
        return groupContentPath;
    }

    public ModelType getModelType() {
        return modelType;
    }
}