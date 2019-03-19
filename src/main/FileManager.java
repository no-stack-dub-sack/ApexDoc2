package main;

import main.models.*;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FileManager {
    private String path;
    private String documentTitle = "ApexDocs";
    private FileOutputStream fileOutputStream;
    private DataOutputStream dataOutputStream;

    public FileManager(String path) {
        if (path == null || path.trim().length() == 0) {
            this.path = ".";
        } else {
            this.path = path;
        }
    }

    public void setDocumentTitle(String documentTitle) {
        if (documentTitle != null && documentTitle.trim().length() > 0) {
            this.documentTitle = documentTitle;
        }
    }

    private boolean createHTML(TreeMap<String, String> mapFNameToContent) {
        try {
            (new File(path)).mkdirs();

            int i = 0;
            for (String fileName : mapFNameToContent.keySet()) {
                String contents = mapFNameToContent.get(fileName);
                fileName = path + "/" + fileName + ".html";
                File file = new File(fileName);
                fileOutputStream = new FileOutputStream(file);
                dataOutputStream = new DataOutputStream(fileOutputStream);
                dataOutputStream.write(contents.getBytes());
                dataOutputStream.close();
                fileOutputStream.close();
                // prepend \n on 1st iteration for space between cmd line input & output
                Utils.log((i == 0 ? "\n" : "") + fileName + " Processed...");
                i++;
            }

            Utils.log(""); // print new line

            copy(path);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * @description main routine that creates an HTML file for each class specified
     * @param mapGroupNameToClassGroup
     * @param models
     * @param bannerPage
     * @param homeContents
     * @param hostedSourceURL
     */
    public void createDocs(TreeMap<String, ClassGroup> groupNameMap, TreeMap<String, TopLevelModel> modelMap,
            ArrayList<TopLevelModel> models, String bannerPage, String homeContents) {

        String links = "<table width='100%'>";
        links += DocGen.makeHTMLScopingPanel();
        links += "<tr style='vertical-align:top;' >";
        links += DocGen.makeMenu(groupNameMap, models);

        if (homeContents != null && homeContents.trim().length() > 0) {
            homeContents = links + "<td class='contentTD'>" + "<h2 class='sectionTitle'>Home</h2>" + homeContents + "</td>";
            homeContents = DocGen.makeHeader(bannerPage, this.documentTitle) + homeContents + HTML.FOOTER;
        } else {
            homeContents = HTML.DEFAULT_HOME_CONTENTS;
            homeContents = links + "<td class='contentTD'>" + "<h2 class='sectionTitle'>Home</h2>" + homeContents + "</td>";
            homeContents = DocGen.makeHeader(bannerPage, this.documentTitle) + homeContents + HTML.FOOTER;
        }

        String fileName = "";
        TreeMap<String, String> fileMap = new TreeMap<String, String>();
        fileMap.put("index", homeContents);

        // create our Class Group content files
        createClassGroupContent(fileMap, links, bannerPage, groupNameMap);

        for (TopLevelModel model : models) {
            String contents = links;
            if (model.getNameLine() != null && model.getNameLine().length() > 0) {
                fileName = model.getName();
                contents += "<td class='contentTD'>";

                if (model.getModelType() == TopLevelModel.ModelType.CLASS) {

                    ClassModel cModel = (ClassModel) model;
                    contents += DocGen.documentClass(cModel, modelMap, models);

                    // get child classes to work with in the order user specifies
                    ArrayList<ClassModel> childClasses = DocGen.sortOrderStyle.equals(ApexDoc.ORDER_ALPHA)
                        ? cModel.getChildClassesSorted()
                        : cModel.getChildClasses();

                    // map over child classes returning HTML strings
                    List<String> childClassHTML = childClasses.stream().map(cmChild ->
                        DocGen.documentClass(cmChild, modelMap, models)).collect(Collectors.toList());

                    // join and concat with contents
                    contents += String.join("", childClassHTML);

                } else if (model.getModelType() == TopLevelModel.ModelType.ENUM) {
                    EnumModel eModel = (EnumModel) model;
                    contents += DocGen.documentEnum(eModel, modelMap, models);
                }

            } else {
                continue;
            }
            contents += "</div>";

            contents = DocGen.makeHeader(bannerPage, this.documentTitle) + contents + HTML.FOOTER;
            fileMap.put(fileName, contents);
        }

        createHTML(fileMap);
    }

    // create our Class Group content files
    private void createClassGroupContent(TreeMap<String, String> mapFNameToContent, String links, String bannerPage,
        TreeMap<String, ClassGroup> mapGroupNameToClassGroup) {

        mapGroupNameToClassGroup.keySet().stream().forEach(group -> {
            ClassGroup cg = mapGroupNameToClassGroup.get(group);
            if (cg.getContentSource() != null) {
                String cgContent = parseHTMLFile(cg.getContentSource());
                if (cgContent != "") {

                    String html =
                        DocGen.makeHeader(bannerPage, this.documentTitle) + links +
                        "<td class='contentTD'>" + "<h2 class='sectionTitle'>" +
                        DocGen.escapeHTML(cg.getName(), false) + "</h2>" + cgContent + "</td>";

                    html += HTML.FOOTER;

                    mapFNameToContent.put(cg.getContentFilename(), html);
                }
            }
        });
    }

    private void doCopy(String source, String target) throws Exception {

        InputStream is = this.getClass().getResourceAsStream("resources/" + source);
        FileOutputStream to = new FileOutputStream(target + "/" + source);

        byte[] buffer = new byte[4096];
        int bytesRead;

        while ((bytesRead = is.read(buffer)) != -1) {
            to.write(buffer, 0, bytesRead); // write
        }

        to.flush();
        to.close();
        is.close();
    }

    private void copy(String toFileName) throws IOException, Exception {
        doCopy("apex_doc_2_logo.png", toFileName);
        doCopy("favicon.png", toFileName);
        doCopy("index.css", toFileName);
        doCopy("index.js", toFileName);
        doCopy("highlight.css", toFileName);
        doCopy("highlight.js", toFileName);
    }

    public ArrayList<File> getFiles(String path) {
        ArrayList<String> include = new ArrayList<String>();
        // include.add("NotificationsEmailer.cls");
        ArrayList<String> includePatterns = new ArrayList<String>();
        // includePatterns.add("SOT_*");
        // includePatterns.add("*TriggerHandler.cls");

        ArrayList<String> exclude = new ArrayList<String>();
        exclude.add("QueueTriggerHandler.cls");
        ArrayList<String> excludePatterns = new ArrayList<String>();
        excludePatterns.add("*TriggerHandler.cls");
        excludePatterns.add("*TriggerFacade.cls");
        excludePatterns.add("*Processor.cls");
        excludePatterns.add("Contact*");
        excludePatterns.add("SOT_*");
        excludePatterns.add("Opportunity*");
        excludePatterns.add("Account*");
        excludePatterns.add("*Test.cls");

        boolean shouldFilter =
            include.size() > 0 || includePatterns.size() > 0 ||
            exclude.size() > 0 || excludePatterns.size() > 0;

        // local function for testing file inclusivity
        Function<String, Boolean> testIncludes = fileName -> {
            // there are no include parameters, include file
            if (include.size() == 0 && includePatterns.size() == 0) {
                return true;
            }

            // file explicitly included
            if (include.contains(fileName)) {
                return true;
            }

            // filename matches include pattern
            for (String pattern : includePatterns) {
                // replace trailing or leading char that indicates this is a wildcard pattern
                pattern = pattern.replace("*", "");
                if (fileName.startsWith(pattern) || fileName.endsWith(pattern)) {
                    return true;
                }
            }

            // file was not explicitly included and does not match pattern
            return false;
        };

        File folder = new File(path);
        ArrayList<File> filesToCopy = new ArrayList<File>();
        try {
            List<File> files = Arrays.asList(folder.listFiles());
            Utils.log("\nProcessing files:\n");
            if (files != null && files.size() > 0) {
                files.stream()

                    // remove non-.cls files
                    .filter(file -> file.getName().endsWith(".cls"))

                    // decide which files to keep and which to add
                    .forEach(file -> {
                        String fileName = file.getName();
                        // if include/exclude arguments provided, only add
                        // files that pass exclusivity and inclusivity tests
                        if (shouldFilter) {
                            // start by filtering out files explitly not included
                            if (!exclude.contains(fileName)) {
                                // if there are exlude patterns, test file against those
                                if (excludePatterns.size() > 0) {
                                    for (String pattern : excludePatterns) {
                                        pattern = pattern.replace("*", "");

                                        // file is excluded by pattern, return early
                                        if (fileName.startsWith(pattern) || fileName.endsWith(pattern)) {
                                            return;
                                        }
                                    }

                                    // file is definitely not excluded, test inclusivity
                                    if (testIncludes.apply(fileName)) {
                                        Utils.log(fileName);
                                        filesToCopy.add(file);
                                    }
                                }
                                // file not explicitly excluded, and no exclude
                                // patterns to test. move on to testing inclusivity
                                else {
                                    if (testIncludes.apply(fileName)) {
                                        Utils.log(fileName);
                                        filesToCopy.add(file);
                                    }
                                }
                            }
                        }
                        // No filtering arguents provided, add all files
                        else {
                            Utils.log(fileName);
                            filesToCopy.add(file);
                        }
                    }
                );
            } else {
                System.out.println("WARNING: No files found in directory: " + path);
            }
        } catch (SecurityException e) {
            Utils.log(e);
        }
        return filesToCopy;
    }

    private String parseFile(String filePath) {
        try {
            if (filePath != null && filePath.trim().length() > 0) {
                FileInputStream fstream = new FileInputStream(filePath);
                DataInputStream input = new DataInputStream(fstream);
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                String contents = "";
                String strLine;

                while ((strLine = reader.readLine()) != null) {
                    strLine = strLine.trim();
                    if (strLine != null && strLine.length() > 0) {
                        contents += strLine;
                    }
                }

                reader.close();
                return contents;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    public String parseHTMLFile(String filePath) {
        String contents = (parseFile(filePath)).trim();
        if (contents != null && contents.length() > 0) {
            int startIndex = contents.indexOf("<body>");
            int endIndex = contents.indexOf("</body>");
            if (startIndex != -1) {
                if (contents.indexOf("</body>") != -1) {
                    contents = contents.substring(startIndex, endIndex);
                    return contents;
                }
            }
        }
        return "";
    }
}
