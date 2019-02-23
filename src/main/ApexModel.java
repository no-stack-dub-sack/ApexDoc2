package main;

import java.util.ArrayList;

public class ApexModel {

    // token constants
    private static final String AUTHOR = "@author";
    private static final String DATE = "@date";
    private static final String DEPRECATED = "@deprecated";
    private static final String DESCRIPTION = "@description";
    private static final String EXAMPLE = "@example";
    private static final String EXCEPTION = "@exception";
    private static final String GROUP = "@group "; // needed to include space to not match group-content
    private static final String GROUP_CONTENT = "@group-content";
    private static final String PARAM = "@param";
    private static final String RETURN = "@return";
    private static final String SEE = "@see";

    private String author = "";
    private String date = "";
    private String deprecated = "";
    private String description = "";
    private String example = "";
    protected String exception = "";
    protected String classGroup = "";
    protected String classGroupContent = "";
    protected ArrayList<String> params = new ArrayList<String>();
    private String see = "";
    protected String returns = "";

    private String nameLine;
    private int lineNum;
    private String scope;

    public ApexModel(ArrayList<String> comments) {
        this.parseComments(comments);
    }

    // model attribute getters / setters
    protected void setNameLine(String nameLine, int lineNum) {
        this.nameLine = nameLine.trim();
        this.lineNum = lineNum;
        parseScope();
    }

    public String getNameLine() {
        return nameLine;
    }

    public int getLineNum() {
        return lineNum;
    }

    public String getScope() {
        return scope == null ? "" : scope;
    }

    private void parseScope() {
        if (nameLine != null) {
            String str = ApexDoc.containsScope(nameLine);
            if (str != null) {
                scope = str;
            } else {
                scope = "private";
            }
        }
    }

    // common @token getters
    public String getDescription() {
        return description == null ? "" : description;
    }

    public String getAuthor() {
        return author == null ? "" : author;
    }

    public String getDeprecated() {
        return deprecated == null ? "" : deprecated;
    }

    public String getDate() {
        return date == null ? "" : date;
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

    // comment parser
    private void parseComments(ArrayList<String> comments) {
        String currBlock = null, block = null;
        for (String comment : comments) {
            boolean newBlock = false, isBreak = false;
            String lowerComment = comment.toLowerCase();
            int i;

            // if we find a token, start a new block
            if (((i = lowerComment.indexOf(block = AUTHOR)) >= 0)
                || ((i = lowerComment.indexOf(block = DATE)) >= 0)
                || ((i = lowerComment.indexOf(block = SEE)) >= 0)
                || ((i = lowerComment.indexOf(block = RETURN)) >= 0)
                || ((i = lowerComment.indexOf(block = PARAM)) >= 0)
                || ((i = lowerComment.indexOf(block = EXCEPTION)) >= 0)
                || ((i = lowerComment.indexOf(block = DEPRECATED)) >= 0)
                || ((i = lowerComment.indexOf(block = DESCRIPTION)) >= 0)
                || ((i = lowerComment.indexOf(block = GROUP)) >= 0)
                || ((i = lowerComment.indexOf(block = GROUP_CONTENT)) >= 0)
                || ((i = lowerComment.indexOf(block = EXAMPLE)) >= 0)) {

                    comment = comment.substring(i + block.length());
                    currBlock = block;
                    newBlock = true;
                }

                // get everything after first '*'
                String line = "";
                comment = comment.trim();
                for (int j = 0; j < comment.length(); ++j) {
                    char ch = comment.charAt(j);
                    if (ch != '*') {
                        line = comment.substring(j);
                        break;
                    }
                }

            // replace docBlock break marker and indicate we should break after
            // this round. Otherwise we may get some strange behavior due to
            // multi-line support and this common parser for all models
            if (line.contains(ApexDoc.DOC_BLOCK_BREAK)) {
                line = line.replace(ApexDoc.DOC_BLOCK_BREAK, "");
                isBreak = true;
            };

            // add line to appropriate block...
            // if currBlock was not reset on this iteration we're on the next line of the last token, add line
            // to that value. Allow empty lines in example blocks to preserve whitespace in complex examples
            if (currBlock != null && (!line.trim().isEmpty() || line.trim().isEmpty() && currBlock.equals(EXAMPLE))) {
                if (currBlock.equals(AUTHOR)) {
                    author += (!author.isEmpty() ? " " : "") + line.trim();
                } else if (currBlock.equals(DATE)) {
                    date += (!date.isEmpty() ? " " : "") + line.trim();
                } else if (currBlock.equals(SEE)) {
                    see += (!see.isEmpty() ? " " : "") + line.trim();
                } else if (currBlock.equals(RETURN)) {
                    returns += (!returns.isEmpty() ? " " : "") + line.trim();
                } else if (currBlock.equals(PARAM)) {
                    String p = (newBlock ? "" : params.remove(params.size() - 1));
                    params.add(p + (!p.isEmpty() ? " " : "") + line.trim());
                } else if (currBlock.equals(EXCEPTION)) {
                    exception += (!exception.isEmpty() ? " " : "") + line.trim();
                } else if (currBlock.equals(DEPRECATED)) {
                    deprecated += (!deprecated.isEmpty() ? " " : "") + line.trim();
                } else if (currBlock.equals(DESCRIPTION)) {
                    description += (!description.isEmpty() ? " " : "") + line.trim();
                } else if (currBlock.equals(GROUP)) {
                    classGroup += (!classGroup.isEmpty() ? " " : "") + line.trim();
                } else if (currBlock.equals(GROUP_CONTENT)) {
                    classGroupContent += (!classGroupContent.isEmpty() ? " " : "") + line.trim();
                } else if (currBlock.equals(EXAMPLE)) {
                    example += (!example.isEmpty() ? "\n" : "") + line;
                }
            }

            if (isBreak) break;
        }
    }
}
