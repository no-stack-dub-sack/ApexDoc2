package main.models;

import main.ApexDoc;
import main.HTML;
import main.Utils;

import java.io.File;
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

    // model state variables
    private String author = "";
    private String date = "";
    private String deprecated = "";
    private String description = "";
    private ArrayList<String> annotations;
    protected String example = "";
    protected String exception = "";
    protected String groupName = "";
    protected String groupContentPath = "";
    protected ArrayList<String> params;
    protected String see = "";
    protected String returns = "";

    private String nameLine;
    private int lineNum;
    protected String scope;

    public ApexModel(ArrayList<String> comments) {
        annotations = new ArrayList<String>();
        params = new ArrayList<String>();
        this.parseComments(comments);
    }

    // model attribute getters / setters
    protected void setNameLine(String nameLine, int lineNum) {
        // strip any annotations from the signature line
        // we'll capture those and display them separately
        this.nameLine = Utils.stripAnnotations(nameLine).trim();
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

    protected void parseScope() {
        if (nameLine != null) {
            String str = Utils.containsScope(nameLine);
            if (str != null) {
                scope = str;
            }

            // TODO: perhaps this branch of control flow should
            // be present only in a class override method
            else {

                // this must be an inner class
                // or an @IsTest class
                scope = "private";
            }
        }
    }

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

    public ArrayList<String> getAnnotations() {
        return annotations;
    }

    // comment parser
    private void parseComments(ArrayList<String> comments) {
        String currBlock = null, block = null;
        for (String comment : comments) {
            boolean newBlock = false, isBreak = false;
            String lowerComment = comment.toLowerCase();
            int i;

            // skip lines that are just opening or closing comment blocks
            if (comment.trim().equals("/**") || comment.trim().equals("*/")) {
                continue;
            }

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

            // get everything after opening '*'s
            String line = "";
            comment = comment.trim();
            for (int j = 0; j < comment.length(); ++j) {
                char ch = comment.charAt(j);
                // skip the '/' of the oppening
                // block so comment is trimmed correctly
                if (ch == '/' && j == 0) {
                    continue;
                }

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
                    groupName += (!groupName.isEmpty() ? " " : "") + line.trim();
                } else if (currBlock.equals(EXAMPLE)) {
                    example += (!example.isEmpty() ? "\n" : "") + line;
                } else if (currBlock.equals(GROUP_CONTENT)) {
                    if (pathExists(line.trim())) groupContentPath += line.trim();
                }
            // not a recognized token, assume we're in un-tagged description
            } else if (currBlock == null && !line.trim().isEmpty()) {
                currBlock = block = DESCRIPTION;
                description += (!description.isEmpty() ? " " : "") + line.trim();
            } else if (line.trim().isEmpty()) {
                currBlock = null;
            }

            if (isBreak) break;
        }
    }

    // make sure path relative to target
    // directory exists for @group-content token
    private boolean pathExists(String line) {
        String root = ApexDoc.targetDirectory.endsWith("/")
            ? ApexDoc.targetDirectory + HTML.ROOT_DIRECTORY + "/"
            : ApexDoc.targetDirectory + "/" + HTML.ROOT_DIRECTORY + "/";

        String path = root + line.trim();
        if (line.trim().endsWith("html") && new File(path).exists()) {
            return true;
        } else {
            Utils.log("\nWARNING: @group-content path: '" + path + "' is invalid!\n");
            return false;
        }
    }
}
