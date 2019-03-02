package main;

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
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileManager {
    private String path;
    private String sortOrderStyle;
    private String hostedSourceURL;
    private StringBuffer infoMessages;
    private boolean showMethodTOCDescription;
    private String documentTitle = "ApexDocs";
    private FileOutputStream fileOutputStream;
    private DataOutputStream dataOutputStream;
    private final String ALPHABETICAL = "alpha";

    // constructors
    public FileManager() {
        infoMessages = new StringBuffer();
    }

    public FileManager(String path) {
        this.infoMessages = new StringBuffer();

        if (path == null || path.trim().length() == 0) {
            this.path = ".";
        } else {
            this.path = path;
        }
    }

    // private field setters
    public void setSortOrderStyle(String sortOrder) {
        this.sortOrderStyle = sortOrder.trim();
    }

    public void setHostedSourceURL(String sourceUrl) {
        this.hostedSourceURL = sourceUrl.trim();
    }

    public void setShowMethodTOCDescription(boolean isShow) {
        this.showMethodTOCDescription = isShow;
    }

    public void setDocumentTitle(String documentTitle) {
        if (documentTitle != null && documentTitle.trim().length() > 0) {
            this.documentTitle = documentTitle;
        }
    }

    // HTML formatters
    private static String wrapInlineCode(String html) {
        String[] words = html.split(" ");

        for (Integer i = 0; i < words.length; i++) {
            String str = words[i];
            Integer firstIndex = str.indexOf("`");
            Integer lastIndex = str.lastIndexOf("`");

            if (firstIndex > -1 && lastIndex > -1 && firstIndex != lastIndex) {
                str = str.replaceFirst("`", "<code class='inlineCode'>");
                str = str.replaceFirst("`", "</code>");
                words[i] = str;
            }
        }

        return String.join(" ", words);
    }

    private static String escapeHTML(String s) {
        StringBuilder out = new StringBuilder(Math.max(16, s.length()));
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c > 127 || c == '"' || c == '<' || c == '>' || c == '&') {
                out.append("&#");
                out.append((int) c);
                out.append(';');
            } else {
                out.append(c);
            }
        }
        return wrapInlineCode(out.toString());
    }

    private boolean createHTML(TreeMap<String, String> mapFNameToContent) {
        try {
            if (path.endsWith("/") || path.endsWith("\\")) {
                path += HTML.ROOT_DIRECTORY; // + "/" + fileName + ".html";
            } else {
                path += "/" + HTML.ROOT_DIRECTORY; // + "/" + fileName + ".html";
            }

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
                infoMessages.append(fileName + " Processed...\n");
                // prepend \n on 1st iteration for space between cmd line input & output
                ApexDoc.log((i == 0 ? "\n" : "") + fileName + " Processed...");
                i++;
            }
            copy(path);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    private String maybeMakeSourceLink(ApexModel model, String className, String modelName) {
        if (hostedSourceURL != null && !hostedSourceURL.equals("")) {
            // if user leaves off trailing slash, save the day!
            if (!hostedSourceURL.endsWith("/")) hostedSourceURL += "/";
            return "<a target='_blank' title='Go to source' class='hostedSourceLink' href='" +
                    hostedSourceURL + className + ".cls#L" + model.getLineNum() + "'>" +
                    modelName + "</a>";
        } else {
            return "<span>" + modelName + "</span>";
        }
    }

    private String makeHTMLScopingPanel() {
        String str = "<tr><td colspan='2' style='text-align: center;' >";
        str += "Show: ";

        for (int i = 0; i < ApexDoc.rgstrScope.length; i++) {
            str += "<input type='checkbox' checked='checked' id='cbx" + ApexDoc.rgstrScope[i] +
                    "' onclick='toggleScope(\"" + ApexDoc.rgstrScope[i] + "\", this.checked );'>" +
                    ApexDoc.rgstrScope[i] + "</input>&nbsp;&nbsp;";
        }
        str += "</td></tr>";
        return str;
    }

    /**
     * @description main routine that creates an HTML file for each class specified
     * @param mapGroupNameToClassGroup
     * @param models
     * @param bannerPage
     * @param homeContents
     * @param hostedSourceURL
     */
    public void createDocs(TreeMap<String, ClassGroup> groupNameMap, ArrayList<TopLevelModel> models,
                          String bannerPage, String homeContents) {

        String links = "<table width='100%'>";
        links += makeHTMLScopingPanel();
        links += "<tr style='vertical-align:top;' >";
        links += getPageLinks(groupNameMap, models);

        if (homeContents != null && homeContents.trim().length() > 0) {
            homeContents = links + "<td class='contentTD'>" + "<h2 class='sectionTitle'>Home</h2>" + homeContents + "</td>";
            homeContents = HTML.getHeader(bannerPage, this.documentTitle) + homeContents + HTML.FOOTER;
        } else {
            homeContents = HTML.DEFAULT_HOME_CONTENTS;
            homeContents = links + "<td class='contentTD'>" + "<h2 class='sectionTitle'>Home</h2>" + homeContents + "</td>";
            homeContents = HTML.getHeader(bannerPage, this.documentTitle) + homeContents + HTML.FOOTER;
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
                    contents += createHTMLForClassModel(cModel, models);

                    // get child classes to work with in the order user specifies
                    ArrayList<ClassModel> childClasses = this.sortOrderStyle.equals(this.ALPHABETICAL)
                        ? cModel.getChildClassesSorted()
                        : cModel.getChildClasses();

                    // deal with any nested classes
                    for (ClassModel cmChild : childClasses) {
                        contents += "<p/>";
                        contents += createHTMLForClassModel(cmChild, models);
                    }
                } else if (model.getModelType() == TopLevelModel.ModelType.ENUM) {
                    EnumModel eModel = (EnumModel) model;
                    contents += createHTMLForEnumModel(eModel, models);
                }

            } else {
                continue;
            }
            contents += "</div>";

            contents = HTML.getHeader(bannerPage, this.documentTitle) + contents + HTML.FOOTER;
            fileMap.put(fileName, contents);
        }

        createHTML(fileMap);
    }

    private String documentClassLevel(TopLevelModel model, ArrayList<TopLevelModel> models, String className, String additionalContent) {
        String sectionSourceLink = maybeMakeSourceLink(model, className, escapeHTML(model.getName()));
        String classSourceLink = maybeMakeSourceLink(model, className, escapeHTML(model.getNameLine()));
        boolean hasSource = hostedSourceURL != null && !hostedSourceURL.equals("");
        String contents = "";

        contents += "<h2 class='sectionTitle'>" + sectionSourceLink +
        (hasSource ? "<span>" + HTML.EXTERNAL_LINK + "</span>" : "") +"</h2>";

        contents += "<div class='classSignature'>" + classSourceLink + "</div>";

        if (!model.getDescription().equals("")) {
            contents += "<div class='classDetails'>" + escapeHTML(model.getDescription());
        }

        if (additionalContent != null) {
            contents += additionalContent;
        }

        if (!model.getDeprecated().equals("")) {
            contents +="<div class='classSubtitle deprecated'>Deprecated</div>";
            contents += "<div class='classSubDescription'>" + escapeHTML(model.getDeprecated()) + "</div>";
        }

        if (!model.getSee().equals("")) {
            contents += "<div class='classSubtitle'>See</div>";
            contents += "<div class='classSubDescription'>" + createSeeLink(models, model.getSee()) + "</div>";
        }

        if (!model.getAuthor().equals("")) {
            contents += "<br/>" + escapeHTML(model.getAuthor());
        }

        if (!model.getDate().equals("")) {
            contents += "<br/>" + escapeHTML(model.getDate());
        }

        contents += "</div><p/>";

        return contents;
    }

    private String documentProperties(ClassModel cModel) {
        String contents = "";
        // retrieve properties to work with in the order user specifies
        ArrayList<PropertyModel> properties = this.sortOrderStyle.equals(this.ALPHABETICAL)
            ? cModel.getPropertiesSorted()
            : cModel.getProperties();

        // start Properties
        contents += "<h2 class='subsectionTitle properties'>Properties</h2>" +
                    "<div class='subsectionContainer'> " +
                    "<table class='attrTable properties'>";

        // iterate once first to determine if we need to build the third column in the table
        boolean hasDescription = false;
        for (PropertyModel prop : properties) {
            if (prop.getDescription().length() > 0) hasDescription = true;
        }

        // if any property has a description build out the third column
        if (hasDescription) {
            contents += "<tr><th>Name</th><th>Signature</th><th>Description</th></tr>";
        } else {
            contents += "<tr><th>Name</th><th>Signature</th></tr>";
        }

        for (PropertyModel prop : properties) {
            String propSourceLink = maybeMakeSourceLink(prop, cModel.getTopmostClassName(), escapeHTML(prop.getNameLine()));
            contents += "<tr class='property " + prop.getScope() + "'>";
            contents += "<td class='attrName'>" + prop.getPropertyName() + "</td>";
            contents += "<td><div class='attrDeclaration'>" + propSourceLink + "</div></td>";

            // if any property has a description build out the third column
            if (hasDescription) {
                contents += "<td><div class='attrDescription'>" + escapeHTML(prop.getDescription()) + "</div></td>";
            }

            contents += "</tr>";
        }
        // end Properties
        contents += "</table></div><p/>";
        return contents;
    }

    private String documentInnerEnums(ClassModel cModel) {
        String contents = "";

        for (EnumModel enum_ : cModel.getEnums()) {
            System.out.println(enum_.getValues());
            System.out.println(enum_.getNameLine());
        }

        ArrayList<EnumModel> enums = this.sortOrderStyle.equals(this.ALPHABETICAL)
            ? cModel.getEnumsSorted()
            : cModel.getEnums();

        // start Properties
        contents += "<h2 class='subsectionTitle enums'>Enums</h2>" +
                    "<div class='subsectionContainer'> " +
                    "<table class='attrTable enums'>";

        // iterate once first to determine if we need to build the third column in the table
        boolean hasDescription = false;
        for (EnumModel _enum : enums) {
            if (_enum.getDescription().length() > 0) hasDescription = true;
        }

        // if any property has a description build out the third column
        if (hasDescription) {
            contents += "<tr><th>Name</th><th>Signature</th><th>Values</th><th>Description</th></tr>";
        } else {
            contents += "<tr><th>Name</th><th>Signature</th><th>Values</th></tr>";
        }

        for (EnumModel _enum : enums) {
            String propSourceLink = maybeMakeSourceLink(_enum, cModel.getTopmostClassName(), escapeHTML(_enum.getNameLine()));
            contents += "<tr class='enum " + _enum.getScope() + "'>";
            contents += "<td class='attrName'>" + _enum.getName() + "</td>";
            contents += "<td><div class='attrDeclaration'>" + propSourceLink + "</div></td>";
            contents += "<td class='enumValues'>" + String.join(", ", _enum.getValues()) + "</td>";

            // if any property has a description build out the third column
            if (hasDescription) {
                contents += "<td><div class='attrDescription'>" + escapeHTML(_enum.getDescription()) + "</div></td>";
            }

            contents += "</tr>";
        }
        // end Properties
        contents += "</table></div><p/>";

        return contents;
    }

    private String documentMethods(ClassModel cModel, ArrayList<TopLevelModel> models) {
        String contents = "";
        // retrieve methods to work with in the order user specifies
        ArrayList<MethodModel> methods = this.sortOrderStyle.equals(this.ALPHABETICAL)
            ? cModel.getMethodsSorted()
            : cModel.getMethods();

        // start Methods
        contents += "<h2 class='subsectionTitle'>Methods</h2><div>";

        // method Table of Contents (TOC)
        contents += "<ul class='methodTOC'>";
        for (MethodModel method : methods) {
            boolean isDeprecated = method.getDeprecated() != "";

            contents += "<li class='method " + method.getScope() + "' >";
            contents += "<a class='methodTOCEntry" + (isDeprecated ? " deprecated" : "") +
                        "' href='#" + method.getMethodName() + "'>" +
                        method.getMethodName() + "</a>";

            // do not render description in TOC if user has indicated to hide
            if (this.showMethodTOCDescription && method.getDescription() != "") {
                contents += "<div class='methodTOCDescription'>" + method.getDescription() + "</div>";
            }

            contents += "</li>";
        }
        contents += "</ul>";

        // full method display
        for (MethodModel method : methods) {
            boolean isDeprecated = !method.getDeprecated().equals("");
            String methodSourceLink = maybeMakeSourceLink(method, cModel.getTopmostClassName(), escapeHTML(method.getNameLine()));
            contents += "<div class='method " + method.getScope() + "' >";
            contents += "<h2 class='methodHeader" + (isDeprecated ? " deprecated" : "") + "'>" +
                        "<a id='" + method.getMethodName() + "'/>" + method.getMethodName() + "</h2>" +
                        "<div class='methodSignature'>" + methodSourceLink + "</div>";

            if (!method.getDescription().equals("")) {
                contents += "<div class='methodDescription'>" + escapeHTML(method.getDescription()) + "</div>";
            }

            if (isDeprecated) {
                contents +="<div class='methodSubTitle deprecated'>Deprecated</div>";
                contents += "<div class='methodSubDescription'>" + escapeHTML(method.getDeprecated()) + "</div>";
            }

            if (method.getParams().size() > 0) {
                contents += "<div class='methodSubTitle'>Parameters</div>";
                for (String param : method.getParams()) {
                    param = escapeHTML(param);
                    if (param != null && param.trim().length() > 0) {
                        Pattern p = Pattern.compile("\\s");
                        Matcher m = p.matcher(param);

                        String paramName;
                        String paramDescription;
                        if (m.find()) {
                            int ich = m.start();
                            paramName = param.substring(0, ich);
                            paramDescription = param.substring(ich + 1);
                        } else {
                            paramName = param;
                            paramDescription = null;
                        }
                        contents += "<div class='paramName'>" + paramName + "</div>";

                        if (paramDescription != null) {
                            contents += "<div class='paramDescription'>" + paramDescription + "</div>";
                        }
                    }
                }
                // end Parameters
            }

            if (!method.getReturns().equals("")) {
                contents += "<div class='methodSubTitle'>Return Value</div>";
                contents += "<div class='methodSubDescription'>" + escapeHTML(method.getReturns()) + "</div>";
            }

            if (!method.getException().equals("")) {
                contents += "<div class='methodSubTitle'>Exceptions</div>";
                contents += "<div class='methodSubDescription'>" + escapeHTML(method.getException()) + "</div>";
            }

            if (!method.getSee().equals("")) {
                contents += "<div class='methodSubTitle'>See</div>";
                contents += "<div class='methodSubDescription'>" + createSeeLink(models, method.getSee()) + "</div>";
            }

            if (!method.getAuthor().equals("")) {
                contents += "<div class='methodSubTitle'>Author</div>";
                contents += "<div class='methodSubDescription'>" + escapeHTML(method.getAuthor()) + "</div>";
            }

            if (!method.getDate().equals("")) {
                contents += "<div class='methodSubTitle'>Date</div>";
                contents += "<div class='methodSubDescription'>" + escapeHTML(method.getDate()) + "</div>";
            }

            if (!method.getExample().equals("")) {
                contents += "<div class='methodSubTitle'>Example</div>";
                contents += "<code class='methodExample'>" + escapeHTML(method.getExample()) + "</code>";
            }

            // end current method
            contents += "</div>";
        }
        // end all methods
        contents += "</div>";

        return contents;
    }
    /**
     * @description creates the HTML for the provided class, including its
     *              property and methods
     * @param cModel
     * @return html string
     */
    private String createHTMLForClassModel(ClassModel cModel, ArrayList<TopLevelModel> models) {
        String contents = "";

        contents += documentClassLevel(cModel, models, cModel.getTopmostClassName(), null);

        if (cModel.getProperties().size() > 0) {
            contents += documentProperties(cModel);
        }

        if (cModel.getEnums().size() > 0) {
            contents += documentInnerEnums(cModel);
        }

        if (cModel.getMethods().size() > 0) {
            contents += documentMethods(cModel, models);
        }

        return contents;
    }

    private String createHTMLForEnumModel(EnumModel eModel, ArrayList<TopLevelModel> models) {
        String contents = "";

        String values = "<p />";
        values += "<table class='attrTable'>";
        values += "<tr><th>Values</th></tr><tr>";
        values += "<td class='enumValues'>" + String.join(", ", eModel.getValues()) + "</td>";
        values += "</tr></table>";

        contents += documentClassLevel(eModel, models, eModel.getName(), values);

        return contents;
    }

    private String createSeeLink(ArrayList<TopLevelModel> models, String qualifiersStr) throws IllegalArgumentException {
        // the @see token may contain a comma separated list of fully qualified
        // method or class names. Start by splitting them into individual qualifiers.
        String[] qualifiers = qualifiersStr.split(",");

        // initialize list to store created links
        ArrayList<String> links = new ArrayList<String>();

        // iterate over each qualifier and process
        for (String qualifier : qualifiers) {

            String[] parts = qualifier.trim().split("\\.");

            if (parts.length > 3) {
                ApexDoc.log(qualifiersStr);
                String message = "Each comma separated qualifier of the @see token must be a fully qualified class " +
                                 "or method name, with a minimum of 1 part and a maximum of 3. E.g. MyClassName, " +
                                 "MyClassName.MyMethodName, MyClassName.MyInnerClassName.MyInnserClassMethodName.";
                throw new IllegalArgumentException(message);
            }

            String href = "";
            boolean foundMatch = false;

            for (TopLevelModel model : models) {
                // if first qualifier matches class name, begin search
                if (model.getName().equalsIgnoreCase(parts[0])) {
                    // if only a single qualifier, stope here
                    if (parts.length == 1) {
                        href = model.getName() + ".html";
                        foundMatch = true;
                        break;
                    }

                    // otherwise keep searching for a match for the second qualifier as long as our
                    // model is not an enum model, in which case there is no searching left to do
                    if (parts.length >= 2 && model.getModelType() != TopLevelModel.ModelType.ENUM) {
                        ClassModel _class = (ClassModel) model;
                        ArrayList<MethodModel> methods = _class.getMethods();
                        ArrayList<ClassModel> childClasses = _class.getChildClasses();

                        for (MethodModel method : methods) {
                            if (method.getMethodName().equalsIgnoreCase(parts[1])) {
                                // use actual class/methof name to create link to avoid case issues
                                href = _class.getName() + ".html#" + method.getMethodName();
                                foundMatch = true;
                                break;
                            }
                        }

                        // if after searching methods a match hasn't been found yet
                        // see if child class name matches the second qualifier.
                        for (ClassModel childClass : childClasses) {
                            // ApexDoc2 stores child class name as 'OuterClass.InnerClass'
                            // recreate that format below to try to make the match with
                            String childClassName = parts[0] + "." + parts[1];
                            if (childClass.getName().equalsIgnoreCase(childClassName)) {
                                String[] innerClass = childClass.getName().split("\\.");
                                // If match, and only 2 parts, stop here.
                                if (parts.length == 2) {
                                    // to ensure the link works, use actual name rather than
                                    // user provided parts in case casing doesn't match
                                    href =  innerClass[0] + ".html#" + innerClass[1];
                                    foundMatch = true;
                                    break;
                                }

                                // Otherwise, there must be 3 parts, attempt to match method.
                                ArrayList<MethodModel> childMethods = childClass.getMethods();
                                for (MethodModel method : childMethods) {
                                    if (method.getMethodName().equalsIgnoreCase(parts[2])) {
                                        // same as above, use actual name to avoid casing issues
                                        href = innerClass[0] + ".html#" + method.getMethodName();
                                        foundMatch = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            String link;
            if (foundMatch) {
                link = "<a href='javascript:void(0)' onclick=\"goToLocation" +
                       "('" + href + "')\">" + qualifier + "</a>";
            } else {
                link = "<span title='A matching reference could not be found!'>" +
                       qualifier + "</span>";
            }

            links.add(link);
        }

        return String.join(", ", links);
    }

    // create our Class Group content files
    private void createClassGroupContent(TreeMap<String, String> mapFNameToContent, String links, String bannerPage,
            TreeMap<String, ClassGroup> mapGroupNameToClassGroup) {

        for (String group : mapGroupNameToClassGroup.keySet()) {
            ClassGroup cg = mapGroupNameToClassGroup.get(group);
            if (cg.getContentSource() != null) {
                String cgContent = parseHTMLFile(cg.getContentSource());
                if (cgContent != "") {

                    String html = HTML.getHeader(bannerPage, this.documentTitle) + links +
                                     "<td class='contentTD'>" + "<h2 class='sectionTitle'>" +
                                     escapeHTML(cg.getName()) + "</h2>" + cgContent + "</td>";

                    html += HTML.FOOTER;

                    mapFNameToContent.put(cg.getContentFilename(), html);
                }
            }
        }
    }

    /**
     * @description generate the HTML string for the Class Menu to display on each page.
     * @param mapGroupNameToClassGroup map that holds all the Class names, and their respective Class Group.
     * @param models list of ClassModels
     * @return String of HTML
     */
    private String getPageLinks(TreeMap<String, ClassGroup> mapGroupNameToClassGroup, ArrayList<TopLevelModel> models) {
        boolean createMiscellaneousGroup = false;

        // this is the only place we need the list of class models sorted by name.
        TreeMap<String, TopLevelModel> tm = new TreeMap<String, TopLevelModel>();

        for (TopLevelModel model : models) {
            tm.put(model.getName().toLowerCase(), model);
            if (!createMiscellaneousGroup && model.getGroupName() == null) {
                createMiscellaneousGroup = true;
            }
        }

        models = new ArrayList<TopLevelModel>(tm.values());

        String contents = "<td width='20%' vertical-align='top' >";
        contents+= "<div class='navbar'>";
        contents+= "<nav role='navigation'>";
        contents+= "<a class='navHeader' id='home' href='javascript:void(0)' onclick=\"goToLocation('index.html');\">";
        contents+= "Home";
        contents+= "</a>";

        // add a bucket ClassGroup for all Classes without a ClassGroup specified
        if (createMiscellaneousGroup) {
            mapGroupNameToClassGroup.put("Miscellaneous", new ClassGroup("Miscellaneous", null));
        }

        // create a sorted list of ClassGroups
        for (String group : mapGroupNameToClassGroup.keySet()) {
            ClassGroup cg = mapGroupNameToClassGroup.get(group);
            String groupId = group.replaceAll(" ", "_");

            contents+= "<details id='" + groupId + "' class='groupName'>";
            contents+= "<summary onclick='toggleActiveClass(this);' id='header-" + groupId + "' class='navHeader'>";

            if (cg.getContentFilename() != null) {
                String destination = cg.getContentFilename() + ".html";
                // handle both onclick and onkeydown when tabbing to link
                contents+= "<a href='javascript:void(0)' title='See Class Group info' " +
                           "onclick=\"goToLocation('" + destination + "');\">" +
                           group + "</a>";
            } else {
                contents+= "<span>" + group + "</span>";
            }

            contents+= "</summary>";
            contents+= "<ul>";

            for (TopLevelModel model : models) {
                // even though this algorithm is O(n^2), it was timed at just 12 milliseconds, so not an issue!
                if (group.equals(model.getGroupName()) || (model.getGroupName() == null && group == "Miscellaneous")) {
                    if (model.getNameLine() != null && model.getNameLine().trim().length() > 0) {
                        String fileName = model.getName();
                        contents+= "<li id='item-" + fileName + "' class='navItem class " +
                                   model.getScope() + "' onclick=\"goToLocation('" + fileName + ".html');\">" +
                                   "<a href='javascript:void(0)'>" + fileName + "</a></li>";
                    }
                }
            }

            contents+= "</ul></details>";
        }

        contents+= "</nav></div>";
        contents+= "</td>";

        return contents;
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
    }

    public ArrayList<File> getFiles(String path) {
        File folder = new File(path);
        ArrayList<File> listOfFilesToCopy = new ArrayList<File>();
        if (folder != null) {
            File[] listOfFiles = folder.listFiles();
            if (listOfFiles != null && listOfFiles.length > 0) {
                for (int i = 0; i < listOfFiles.length; i++) {
                    if (listOfFiles[i].isFile()) {
                        listOfFilesToCopy.add(listOfFiles[i]);
                    }
                }
            } else {
                System.out.println("WARNING: No files found in directory: " + path);
            }
        }
        return listOfFilesToCopy;
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
