package main.models;

import java.util.ArrayList;

public abstract class TopLevelModel extends ApexModel {
    public enum ModelType {
        CLASS,
        ENUM
    }

    public ModelType modelType;

    public TopLevelModel(ArrayList<String> comments, ModelType modelType) {
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