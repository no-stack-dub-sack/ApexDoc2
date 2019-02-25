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
        this.sortOrderStyle = sortOrder;
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

    private String maybeMakeSourceLink(ApexModel model, String className, String hostedSourceURL, String modelName) {
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
     * @param cModels
     * @param bannerPage
     * @param homeContents
     * @param hostedSourceURL
     */
    public void createDocs(TreeMap<String, ClassGroup> mapGroupNameToClassGroup, ArrayList<ClassModel> cModels,
                          String bannerPage, String homeContents, String hostedSourceURL) {

        String links = "<table width='100%'>";
        links += makeHTMLScopingPanel();
        links += "<tr style='vertical-align:top;' >";
        links += getPageLinks(mapGroupNameToClassGroup, cModels);

        if (homeContents != null && homeContents.trim().length() > 0) {
            homeContents = links + "<td class='contentTD'>" + "<h2 class='section-title'>Home</h2>" + homeContents + "</td>";
            homeContents = HTML.getHeader(bannerPage, this.documentTitle) + homeContents + HTML.FOOTER;
        } else {
            homeContents = HTML.DEFAULT_HOME_CONTENTS;
            homeContents = links + "<td class='contentTD'>" + "<h2 class='section-title'>Home</h2>" + homeContents + "</td>";
            homeContents = HTML.getHeader(bannerPage, this.documentTitle) + homeContents + HTML.FOOTER;
        }

        String fileName = "";
        TreeMap<String, String> mapFNameToContent = new TreeMap<String, String>();
        mapFNameToContent.put("index", homeContents);

        // create our Class Group content files
        createClassGroupContent(mapFNameToContent, links, bannerPage, mapGroupNameToClassGroup, cModels);

        for (ClassModel cModel : cModels) {
            String contents = links;
            if (cModel.getNameLine() != null && cModel.getNameLine().length() > 0) {
                fileName = cModel.getClassName();
                contents += "<td class='contentTD'>";

                contents += htmlForClassModel(cModel, hostedSourceURL, cModels);

                // get child classes to work with in the order user specifies
                ArrayList<ClassModel> childClasses = this.sortOrderStyle.equals(this.ALPHABETICAL)
                    ? cModel.getChildClassesSorted()
                    : cModel.getChildClasses();

                // deal with any nested classes
                for (ClassModel cmChild : childClasses) {
                    contents += "<p/>";
                    contents += htmlForClassModel(cmChild, hostedSourceURL, cModels);
                }

            } else {
                continue;
            }
            contents += "</div>";

            contents = HTML.getHeader(bannerPage, this.documentTitle) + contents + HTML.FOOTER;
            mapFNameToContent.put(fileName, contents);
        }
        createHTML(mapFNameToContent);
    }

    /**
     * @description creates the HTML for the provided class, including its
     *              property and methods
     * @param cModel
     * @param hostedSourceURL
     * @return html string
     */
    private String htmlForClassModel(ClassModel cModel, String hostedSourceURL, ArrayList<ClassModel> cModels) {
        String contents = "";
        hostedSourceURL = hostedSourceURL.trim();
        String outerClass = cModel.getTopmostClassName();
        boolean hasSource = hostedSourceURL != null && !hostedSourceURL.equals("");

        String sectionSourceLink = maybeMakeSourceLink(cModel, outerClass, hostedSourceURL, escapeHTML(cModel.getClassName()));
        String classSourceLink = maybeMakeSourceLink(cModel, outerClass, hostedSourceURL, escapeHTML(cModel.getNameLine()));

        contents += "<h2 class='section-title'>" + sectionSourceLink +
                 (hasSource ? "<span>" + HTML.EXTERNAL_LINK + "</span>" : "") +"</h2>";

        contents += "<div class='classSignature'>" + classSourceLink + "</div>";

        if (cModel.getEnums().size() > 0) {
            for (EnumModel enum_ : cModel.getEnums()) {
                System.out.println(enum_.getValues());
                System.out.println(enum_.getNameLine());
            }
        }

        if (!cModel.getDescription().equals("")) {
            contents += "<div class='classDetails'>" + escapeHTML(cModel.getDescription());
        }

        if (!cModel.getDeprecated().equals("")) {
            contents +="<div class='classSubtitle deprecated'>Deprecated</div>";
            contents += "<div class='classSubDescription'>" + escapeHTML(cModel.getDeprecated()) + "</div>";
        }

        if (!cModel.getSee().equals("")) {
            contents += "<div class='classSubtitle'>See</div>";
            contents += "<div class='classSubDescription'>" + createSeeLink(cModels, cModel.getSee()) + "</div>";
        }

        if (!cModel.getAuthor().equals("")) {
            contents += "<br/>" + escapeHTML(cModel.getAuthor());
        }

        if (!cModel.getDate().equals("")) {
            contents += "<br/>" + escapeHTML(cModel.getDate());
        }

        contents += "</div><p/>";

        if (cModel.getProperties().size() > 0) {
            // retrieve properties to work with in the order user specifies
            ArrayList<PropertyModel> properties = this.sortOrderStyle.equals(this.ALPHABETICAL)
                ? cModel.getPropertiesSorted()
                : cModel.getProperties();

            // start Properties
            contents += "<h2 class='subsection-title'>Properties</h2>" +
                        "<div class='subsection-container'> " +
                        "<table class='properties'>";

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
                String propSourceLink = maybeMakeSourceLink(prop, outerClass, hostedSourceURL, escapeHTML(prop.getNameLine()));
                contents += "<tr class='property " + prop.getScope() + "'>";
                contents += "<td class='clsPropertyName'>" + prop.getPropertyName() + "</td>";
                contents += "<td><div class='clsPropertyDeclaration'>" + propSourceLink + "</div></td>";

                // if any property has a description build out the third column
                if (hasDescription) {
                    contents += "<td><div class='clsPropertyDescription'>" + escapeHTML(prop.getDescription()) + "</div></td>";
                }

                contents += "</tr>";
            }
            // end Properties
            contents += "</table></div><p/>";
        }

        if (cModel.getMethods().size() > 0) {
            // retrieve methods to work with in the order user specifies
            ArrayList<MethodModel> methods = this.sortOrderStyle.equals(this.ALPHABETICAL)
                ? cModel.getMethodsSorted()
                : cModel.getMethods();

            // start Methods
            contents += "<h2 class='subsection-title'>Methods</h2><div>";

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
                String methodSourceLink = maybeMakeSourceLink(method, outerClass, hostedSourceURL, escapeHTML(method.getNameLine()));
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
                    contents += "<div class='methodSubDescription'>" + createSeeLink(cModels, method.getSee()) + "</div>";
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
        }

        return contents;
    }

    private String createSeeLink(ArrayList<ClassModel> classes, String qualifiersStr) throws IllegalArgumentException {
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

            for (ClassModel _class : classes) {
                // if first qualifier matches class name, begin search
                if (_class.getClassName().equalsIgnoreCase(parts[0])) {
                    // if only a single qualifier, stope here
                    if (parts.length == 1) {
                        href = _class.getClassName() + ".html";
                        foundMatch = true;
                        break;
                    }

                    // otherwise keep searching for a match for the second qualifier
                    if (parts.length >= 2) {
                        ArrayList<MethodModel> methods = _class.getMethods();
                        ArrayList<ClassModel> childClasses = _class.getChildClasses();

                        for (MethodModel method : methods) {
                            if (method.getMethodName().equalsIgnoreCase(parts[1])) {
                                // use actual class/methof name to create link to avoid case issues
                                href = _class.getClassName() + ".html#" + method.getMethodName();
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
                            if (childClass.getClassName().equalsIgnoreCase(childClassName)) {
                                String[] innerClass = childClass.getClassName().split("\\.");
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
            TreeMap<String, ClassGroup> mapGroupNameToClassGroup, ArrayList<ClassModel> cModels) {

        for (String group : mapGroupNameToClassGroup.keySet()) {
            ClassGroup cg = mapGroupNameToClassGroup.get(group);
            if (cg.getContentSource() != null) {
                String cgContent = parseHTMLFile(cg.getContentSource());
                if (cgContent != "") {

                    String html = HTML.getHeader(bannerPage, this.documentTitle) + links +
                                     "<td class='contentTD'>" + "<h2 class='section-title'>" +
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
     * @param cModels list of ClassModels
     * @return String of HTML
     */
    private String getPageLinks(TreeMap<String, ClassGroup> mapGroupNameToClassGroup, ArrayList<ClassModel> cModels) {
        boolean createMiscellaneousGroup = false;

        // this is the only place we need the list of class models sorted by name.
        TreeMap<String, ClassModel> tm = new TreeMap<String, ClassModel>();

        for (ClassModel cm : cModels) {
            tm.put(cm.getClassName().toLowerCase(), cm);
            if (!createMiscellaneousGroup && cm.getClassGroup() == null) {
                createMiscellaneousGroup = true;
            }
        }

        cModels = new ArrayList<ClassModel>(tm.values());

        String contents = "<td width='20%' vertical-align='top' >";
        contents+= "<div class='navbar'>";
        contents+= "<nav role='navigation'>";
        contents+= "<a class='nav-header' id='home' href='javascript:void(0)' onclick=\"goToLocation('index.html');\">";
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

            contents+= "<details id='" + groupId + "' class='classGroup'>";
            contents+= "<summary onclick='toggleActiveClass(this);' id='header-" + groupId + "' class='nav-header'>";

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

            for (ClassModel cModel : cModels) {
                // even though this algorithm is O(n^2), it was timed at just 12 milliseconds, so not an issue!
                if (group.equals(cModel.getClassGroup()) || (cModel.getClassGroup() == null && group == "Miscellaneous")) {
                    if (cModel.getNameLine() != null && cModel.getNameLine().trim().length() > 0) {
                        String fileName = cModel.getClassName();
                        contents+= "<li id='item-" + fileName + "' class='nav-item class " +
                                   cModel.getScope() + "' onclick=\"goToLocation('" + fileName + ".html');\">" +
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
