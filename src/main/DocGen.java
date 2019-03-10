package main;

import main.models.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.function.Function;

public class DocGen {
    public static String sortOrderStyle;
    public static String hostedSourceURL;
    public static boolean showMethodTOCDescription;
    public static TreeMap<String, String> usedIds = new TreeMap<String, String>();

    public static String documentClass(ClassModel cModel, ArrayList<TopLevelModel> models) {
        String contents = "";

        contents += documentTopLevelAttributes(cModel, models, cModel.getTopmostClassName(), null);

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

    public static String documentEnum(EnumModel eModel, ArrayList<TopLevelModel> models) {
        String contents = "";

        String values = "<p />";
        values += "<table class='attrTable'>";
        values += "<tr><th>Values</th></tr><tr>";
        values += "<td class='enumValues'>" + String.join(", ", eModel.getValues()) + "</td>";
        values += "</tr></table>";

        contents += documentTopLevelAttributes(eModel, models, eModel.getName(), values);

        return contents;
    }

    private static String documentTopLevelAttributes(TopLevelModel model, ArrayList<TopLevelModel> models, String className, String additionalContent) {
        String sectionSourceLink = maybeMakeSourceLink(model, className, escapeHTML(model.getName()));
        String classSourceLink = maybeMakeSourceLink(model, className, escapeHTML(model.getNameLine()));
        boolean hasSource = hostedSourceURL != null && !hostedSourceURL.equals("");
        String contents = "";


        contents += "<h2 class='sectionTitle' id='" + model.getName() + "'>" + sectionSourceLink +
        (hasSource ? "<span>" + HTML.EXTERNAL_LINK + "</span>" : "") +"</h2>";

        if (model.getAnnotations().size() > 0) {
            contents += "<div class='classAnnotations'>" + String.join(" ", model.getAnnotations()) + "</div>";
        }

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
            contents += "<div class='classSubDescription'>" + makeSeeLinks(models, model.getSee()) + "</div>";
        }

        if (!model.getAuthor().equals("")) {
            contents += "<br/>" + escapeHTML(model.getAuthor());
        }

        if (!model.getDate().equals("")) {
            contents += "<br/>" + escapeHTML(model.getDate());
        }

        if (!model.getExample().equals("")) {
            contents += "<div class='classSubTitle'>Example</div>";
            contents += "<pre class='codeExample'><code>" + escapeHTML(model.getExample()) + "</code></pre>";
        }

        contents += "</div><p/>";

        return contents;
    }

    private static String documentProperties(ClassModel cModel) {
        String contents = "";
        // retrieve properties to work with in the order user specifies
        ArrayList<PropertyModel> properties = sortOrderStyle.equals(ApexDoc.ORDER_ALPHA)
            ? cModel.getPropertiesSorted()
            : cModel.getProperties();

        // start Properties
        contents += "<h2 class='subsectionTitle properties'>Properties</h2>" +
                    "<div class='subsectionContainer'> " +
                    "<table class='attrTable properties'>";

        // iterate once first to determine if we need to
        // build annotations and and description columns
        String descriptionCol = "", annotationsCol = "";
        for (PropertyModel prop : properties) {
            if (prop.getDescription().length() > 0) descriptionCol = "<th>Description</th>";
            if (prop.getAnnotations().size() > 0) annotationsCol = "<th>Annotations</th>";
        }

        String columnsTemplate = "<tr><th>Name</th><th>Signature</th>%s%s</tr>";
        contents += String.format(columnsTemplate, annotationsCol, descriptionCol);

        for (PropertyModel prop : properties) {
            String nameLine = Utils.highlightNameLine(prop.getNameLine());
            String propSourceLink = maybeMakeSourceLink(prop, cModel.getTopmostClassName(), nameLine);

            contents += "<tr class='property " + prop.getScope() + "'>";
            contents += "<td class='attrName'>" + prop.getPropertyName() + "</td>";
            contents += "<td><div class='attrSignature'>" + propSourceLink + "</div></td>";

            if (annotationsCol.length() > 0) {
                contents += "<td><div class='propAnnotations'>" + String.join(", ", prop.getAnnotations()) + "</div></td>";
            }

            // if any property has a description build out the third column
            if (descriptionCol.length() > 0) {
                contents += "<td><div class='attrDescription'>" + escapeHTML(prop.getDescription()) + "</div></td>";
            }

            contents += "</tr>";
        }
        // end Properties
        contents += "</table></div><p/>";
        return contents;
    }

    private static String documentInnerEnums(ClassModel cModel) {
        String contents = "";

        ArrayList<EnumModel> enums = sortOrderStyle.equals(ApexDoc.ORDER_ALPHA)
            ? cModel.getEnumsSorted()
            : cModel.getEnums();

        // start Properties
        contents += "<h2 class='subsectionTitle enums'>Enums</h2>" +
                    "<div class='subsectionContainer'> " +
                    "<table class='attrTable enums'>";

        // iterate once first to determine if we need to build the third column in the table
        String descriptionCol = "";
        for (EnumModel Enum : enums) {
            if (Enum.getDescription().length() > 0) descriptionCol = "<th>Description</th>";
        }

        contents += String.format("<tr><th>Name</th><th>Signature</th><th>Values</th>%s</tr>", descriptionCol);

        for (EnumModel Enum : enums) {
            String nameLine = Utils.highlightNameLine(Enum.getNameLine());
            String propSourceLink = maybeMakeSourceLink(Enum, cModel.getTopmostClassName(), nameLine);
            contents += "<tr class='enum " + Enum.getScope() + "'>";
            contents += "<td class='attrName'>" + Enum.getName() + "</td>";
            contents += "<td><div class='attrSignature'>" + propSourceLink + "</div></td>";
            contents += "<td class='enumValues'>" + String.join(", ", Enum.getValues()) + "</td>";

            // if any property has a description build out the third column
            if (descriptionCol.length() > 0) {
                contents += "<td><div class='attrDescription'>" + escapeHTML(Enum.getDescription()) + "</div></td>";
            }

            contents += "</tr>";
        }
        // end Properties
        contents += "</table></div><p/>";

        return contents;
    }

    private static String documentMethods(ClassModel cModel, ArrayList<TopLevelModel> models) {

        // track Ids used to make sure we're not generating duplicate
        // Ids within this class, and so that overloaded methods each
        // have their own unique anchor to link to in the TOC.
        TreeMap<String, Integer> idCountMap = new TreeMap<String, Integer>();

        // Local fucntion to make TOC entry
        Function<MethodModel, String> makeTOCEntry = method -> {
            boolean isDeprecated = !method.getDeprecated().equals("");
            String methodId = cModel.getName() + "." + method.getMethodName();

            // Get Id count and ammend as needed if Id has been used
            // previously in this class (must be an overloaded method)
            Integer count = idCountMap.get(methodId);

            if (count == null) {
                idCountMap.put(methodId, 1);
            } else {
                idCountMap.put(methodId, (count + 1));
                methodId += '_' + String.valueOf(count);
            }

            String entry =
                "<li class='method " + method.getScope() + "' >" +
                "<a class='methodTOCEntry" + (isDeprecated ? " deprecated" : "") + "'" +
                "href='#" + methodId + "'>" + method.getMethodName() + "</a>";

            // do not render description in TOC if user has indicated to hide
            if (showMethodTOCDescription && method.getDescription() != "") {
                entry += "<div class='methodTOCDescription'>" + method.getDescription() + "</div>";
            }

            entry += "</li>";
            return entry;
        };


        // retrieve methods to work with in the order user specifies
        ArrayList<MethodModel> methods = sortOrderStyle.equals(ApexDoc.ORDER_ALPHA)
            ? cModel.getMethodsSorted()
            : cModel.getMethods();

        // start Methods
        String contents = "<h2 class='subsectionTitle'>Methods</h2><div>";
        String tocHTML = "<ul class='methodTOC'>";
        String methodsHTML = "";

        // full method display
        for (MethodModel method : methods) {
            // create method TOC entry:
            tocHTML += makeTOCEntry.apply(method);

            // create full methods view HTML:
            boolean isDeprecated = !method.getDeprecated().equals("");
            String methodId = cModel.getName() + "." + method.getMethodName();
            String methodSourceLink = maybeMakeSourceLink(method, cModel.getTopmostClassName(), escapeHTML(method.getNameLine()));

            // if method is an overload, be sure to give it a unique Id
            // since the Id count has already been incremented by this
            // point, we need to suntract 1 from the current value
            Integer count = idCountMap.get(methodId);
            if (count > 1) {
                methodId += '_' + String.valueOf(count - 1);
            }

            // open current method
            methodsHTML += "<div class='method " + method.getScope() + "' >";

            // use fully qualified method name as ID to prevent from TOCs in the same file linking
            // to the same method. For example, an abstract class and a calss which extends that
            // class in the same file are likely to have the same methods and thus conflicting IDs.
            methodsHTML += "<h2 class='methodHeader" + (isDeprecated ? " deprecated" : "") + "'" +
                        "id='" + methodId + "'>" + method.getMethodName() + "</h2>";

            if (method.getAnnotations().size() > 0) {
                methodsHTML += "<div class='methodAnnotations'>" + String.join(" ", method.getAnnotations()) + "</div>";
            }

            methodsHTML += "<div class='methodSignature'>" + methodSourceLink + "</div>";

            if (!method.getDescription().equals("")) {
                methodsHTML += "<div class='methodDescription'>" + escapeHTML(method.getDescription()) + "</div>";
            }

            if (isDeprecated) {
                methodsHTML +="<div class='methodSubTitle deprecated'>Deprecated</div>";
                methodsHTML += "<div class='methodSubDescription'>" + escapeHTML(method.getDeprecated()) + "</div>";
            }

            if (method.getParams().size() > 0) {
                methodsHTML += "<div class='methodSubTitle'>Parameters</div>";
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
                        methodsHTML += "<div class='paramName'>" + paramName + "</div>";

                        if (paramDescription != null) {
                            methodsHTML += "<div class='paramDescription'>" + paramDescription + "</div>";
                        }
                    }
                }
                // end Parameters
            }

            if (!method.getReturns().equals("")) {
                methodsHTML += "<div class='methodSubTitle'>Return Value</div>";
                methodsHTML += "<div class='methodSubDescription'>" + escapeHTML(method.getReturns()) + "</div>";
            }

            if (!method.getException().equals("")) {
                methodsHTML += "<div class='methodSubTitle'>Exceptions</div>";
                methodsHTML += "<div class='methodSubDescription'>" + escapeHTML(method.getException()) + "</div>";
            }

            if (!method.getSee().equals("")) {
                methodsHTML += "<div class='methodSubTitle'>See</div>";
                methodsHTML += "<div class='methodSubDescription'>" + makeSeeLinks(models, method.getSee()) + "</div>";
            }

            if (!method.getAuthor().equals("")) {
                methodsHTML += "<div class='methodSubTitle'>Author</div>";
                methodsHTML += "<div class='methodSubDescription'>" + escapeHTML(method.getAuthor()) + "</div>";
            }

            if (!method.getDate().equals("")) {
                methodsHTML += "<div class='methodSubTitle'>Date</div>";
                methodsHTML += "<div class='methodSubDescription'>" + escapeHTML(method.getDate()) + "</div>";
            }

            if (!method.getExample().equals("")) {
                methodsHTML += "<div class='methodSubTitle'>Example</div>";
                methodsHTML += "<pre class='codeExample'><code>" + escapeHTML(method.getExample()) + "</code></pre>";
            }

            // end current method
            methodsHTML += "</div>";
        }

        // concat and close TOC and full methods display HTML
        contents += tocHTML;
        contents += "</ul>";
        contents += methodsHTML;
        contents += "</div>";

        return contents;
    }

    public static String escapeHTML(String s) {
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

        // wrap any words wrapped in backticks in code tags for styling
        String result = wrapInlineCode(out.toString());

        // preserve <br> tags so they render as HTML
        result = result.replaceAll("&#60;br\\s?/?&#62;", "<br>");

        return result;
    }

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

    public static String makeHTMLScopingPanel() {
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

    public static String makeHeader(String bannerPage, String documentTitle) {
        String header =  "<html><head><title>" + documentTitle + "</title>";

        if (bannerPage != null && bannerPage.trim().length() > 0) {
            header += HTML.HEADER_OPEN + bannerPage;
        } else {
            header += HTML.HEADER_OPEN + HTML.PROJECT_DETAIL + HTML.HEADER_CLOSE;
        }

        return header;
    }

    public static String makeMenu(TreeMap<String, ClassGroup> mapGroupNameToClassGroup, ArrayList<TopLevelModel> models) {
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

    private static String maybeMakeSourceLink(ApexModel model, String className, String modelName) {
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

    private static String makeSeeLinks(ArrayList<TopLevelModel> models, String qualifiersStr) throws IllegalArgumentException {
        // the @see token may contain a comma separated list of fully qualified
        // method or class names. Start by splitting them into individual qualifiers.
        String[] qualifiers = qualifiersStr.split(",");

        // initialize list to store created links
        ArrayList<String> links = new ArrayList<String>();

        // iterate over each qualifier and process
        // we could just take the users qualifiers and assume its a valid path
        // but this could easily result in dead links. This algorithm doesn't
        // exactly scream efficiency, but its still fast on moderate codebases
        // and its better than the alternative of dead links all over the place.
        for (String qualifier : qualifiers) {
            qualifier = qualifier.trim();

            // 1) continue if empty
            if (qualifier.isEmpty()) {
                continue;
            }

            // 2) chcek if URL, add to links and continue with loop if so
            if (Utils.isURL(qualifier)) {
                links.add("<a target='_blank' href='" + qualifier + "'>" + qualifier + "</a>");
                continue;
            }

            // 3) check if markdown-formatted URL. add to links and continue.
            // markdown parsing function will detect if URL is valid and return
            // a span with tooltip indicating invalid link if not
            if (Utils.isMarkdownURL(qualifier)) {
                links.add(Utils.markdownUrlToLink(qualifier));
                continue;
            }

            // 4) if not URL or empty, must be a qualified class or method name.
            // First prepare the qualifier by stripping away and saving any method
            // overload selector for later. E.g. SomeClass.SomeMethod[4] means: link
            // to the 4th overload (zero-based) of that method. This syntax is only required
            // to specify a method other than the 1st. Otherwise SomeClass.SomeMethod is fine
            int overloadSelector = 0;
            if (qualifier.matches(".*\\[\\d+\\]$")) {
                int i = qualifier.lastIndexOf('[');
                // isolate the number inside the brackets
                String selector = qualifier.substring(i+1, qualifier.length() - 1);
                System.out.println(selector);
                overloadSelector = Integer.valueOf(selector);
                // strip away the suffix from the qualifier
                qualifier = qualifier.substring(0, i);
            }

            String[] parts = qualifier.split("\\.");

            if (parts.length > 3) {
                Utils.log(qualifiersStr);
                String message = "Each comma separated qualifier of the @see token must be a fully qualified class "
                        + "or method name, with a minimum of 1 part and a maximum of 3. E.g. MyClassName, "
                        + "MyClassName.MyMethodName, MyClassName.MyInnerClassName.MyInnserClassMethodName.";
                throw new IllegalArgumentException(message);
            }

            String href = "";
            boolean foundMatch = false;

            for (TopLevelModel model : models) {
                // 4.A) if first qualifier matches class name, begin search
                if (model.getName().equalsIgnoreCase(parts[0])) {
                    // if only a single qualifier, stope here
                    if (parts.length == 1) {
                        href = model.getName() + ".html";
                        foundMatch = true;
                        break;
                    }

                    // 4.B) otherwise keep searching for a match for the second qualifier as long as
                    // model is not an enum model, in which case there is no searching left to do
                    if (parts.length >= 2 && model.getModelType() != TopLevelModel.ModelType.ENUM) {
                        ClassModel Class = (ClassModel) model;
                        ArrayList<MethodModel> methods = Class.getMethods();
                        ArrayList<ClassModel> childClasses = Class.getChildClasses();

                        int methodNum = 0;
                        for (MethodModel method : methods) {
                            if (method.getMethodName().equalsIgnoreCase(parts[1])) {
                                // use actual class/methof name to create link to avoid case issues
                                href = Class.getName() + ".html#" + Class.getName() + "." + method.getMethodName();
                                // no overload selector, we've made a match!
                                if (overloadSelector == 0) {
                                    foundMatch = true;
                                    break;
                                }
                                // If there's an overload suffix to take into account
                                // ensure that many overloads of the method actually
                                // exist before commiting to the method link.
                                else if (overloadSelector > 0 && methodNum != overloadSelector) {
                                    methodNum++;
                                    continue;
                                }
                                // confirmed overload exists. Match!!
                                else if (methodNum == overloadSelector) {
                                    href += '_' + String.valueOf(overloadSelector);
                                    foundMatch = true;
                                    break;
                                }
                            }
                        }

                        // match; go no further
                        if (foundMatch) break;

                        // 4.C) if after searching methods a match hasn't been found
                        // yet see if child class name matches the second qualifier.
                        for (ClassModel childClass : childClasses) {
                            // ApexDoc2 stores child class name as 'OuterClass.InnerClass'
                            // recreate that format below to try to make the match with
                            String childClassName = parts[0] + "." + parts[1];
                            if (childClass.getName().equalsIgnoreCase(childClassName)) {
                                String[] innerClass = childClass.getName().split("\\.");
                                // 4.D) If match, and only 2 parts, stop here.
                                if (parts.length == 2) {
                                    // to ensure the link works, use actual name rather than
                                    // user provided parts in case casing doesn't match
                                    href = innerClass[0] + ".html#" + innerClass[0] + "." + innerClass[1];
                                    foundMatch = true;
                                    break;
                                }

                                // 4.E) Otherwise, there must be 3 parts, attempt to match method.
                                ArrayList<MethodModel> childMethods = childClass.getMethods();
                                for (MethodModel method : childMethods) {
                                    if (method.getMethodName().equalsIgnoreCase(parts[2])) {
                                        // same as above, use actual name to avoid casing issues
                                        href = innerClass[0] + ".html#" + childClass.getName() + "."
                                                + method.getMethodName();
                                        foundMatch = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5) if match made, create link with goToLocation function onclick
            // Otherwise, add span with Tooltip indicating no link could be made
            String link;
            if (foundMatch) {
                link =
                    "<a href='javascript:void(0)' onclick=\"goToLocation" + "('" + href + "')\">" +
                    qualifier + "</a>";
            } else {
                link =
                    "<span title='A matching reference could not be found!'>" + qualifier + "</span>";
            }

            links.add(link);
        }

        // 6) collect links / spans and join back into a single string
        return String.join(", ", links);
    }
}