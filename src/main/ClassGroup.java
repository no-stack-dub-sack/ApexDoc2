package main;

public class ClassGroup {
    private String name;
    private String contentSource;

    public ClassGroup(String name, String contentSource) {
        this.name = name;
        this.contentSource = contentSource;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContentSource() {
        return contentSource;
    }

    public void setContentSource(String contentSource) {
        this.contentSource = contentSource;
    }

    public String getContentFilename() {
        if (contentSource != null) {
            int idx1 = contentSource.lastIndexOf("/");
            int idx2 = contentSource.lastIndexOf(".");
            if (idx1 != -1 && idx2 != -1) {
                return contentSource.substring(idx1 + 1, idx2);
            }
        }
        return null;
    }
}
