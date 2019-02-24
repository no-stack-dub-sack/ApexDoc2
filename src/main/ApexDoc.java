package main;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Stack;
import java.util.TreeMap;

public class ApexDoc {

    public static final String CLASS = "class";
    public static final String FINAL = "final";
    public static final String GLOBAL = "global";
    public static final String INTERFACE = " interface ";
    public static final String PRIVATE = "private";
    public static final String PUBLIC = "public";
    public static final String STATIC = "static";
    public static final String WEB_SERVICE = "webService";
    public static final String COMMENT_CLOSE = "*/";
    public static final String COMMENT_OPEN = "/**";

    // use special token for marking the end of a doc block
    // comment. Now that we're supporting multi-line for all
    // tokens and using a common comment parser, the parser
    // must know when a block ends in order to prevent weird
    // behavior when lesser scopes than available are indicated
    // e.g. private;public when there are protected methods
    public static final String DOC_BLOCK_BREAK = "@@BREAK@@";

    public static FileManager fm;
    public static String[] rgstrScope;

    // public entry point when called from the command line.
    public static void main(String[] args) {
        try {
            RunApexDoc(args);
        } catch (Exception ex) {
            log(ex);
            printHelp();
            System.exit(-1);
        }
    }

    // public main routine which is used by both command line invocation and
    // Eclipse PlugIn invocation
    public static void RunApexDoc(String[] args) throws IllegalArgumentException {
        String sourceDirectory = "";
        String targetDirectory = "";
        String homefilepath = "";
        String bannerFilePath = "";
        String hostedSourceURL = "";
        String documentTitle = "";
        String sortOrder = "";

        boolean showMethodTOCDescription = true;

        // parse command line parameters
        for (int i = 0; i < args.length; i++) {
            if (args[i] == null) {
                continue;
            } else if (args[i].equalsIgnoreCase("-s")) {
                sourceDirectory = args[++i];
            } else if (args[i].equalsIgnoreCase("-u")) {
                hostedSourceURL = args[++i];
            } else if (args[i].equalsIgnoreCase("-t")) {
                targetDirectory = args[++i];
            } else if (args[i].equalsIgnoreCase("-h")) {
                homefilepath = args[++i];
            } else if (args[i].equalsIgnoreCase("-b")) {
                bannerFilePath = args[++i];
            } else if (args[i].equalsIgnoreCase("-p")) {
                String scope = args[++i];
                rgstrScope = scope.split(";");
            } else if (args[i].equalsIgnoreCase("-d")) {
                documentTitle = args[++i];
            } else if (args[i].equalsIgnoreCase("-c")) {
                showMethodTOCDescription = Boolean.valueOf(args[++i]);
            } else if (args[i].equalsIgnoreCase("-o")) {
                sortOrder = args[++i].trim();
            } else {
                printHelp();
                System.exit(-1);
            }
        }

        // validate sortOrder argument, throw if invalid default to 'alpha' if not specified
        if (!sortOrder.isEmpty()) {
            if (!sortOrder.equalsIgnoreCase("logical") && !sortOrder.equalsIgnoreCase("alpha")) {
                throw new IllegalArgumentException("Value for <sort_order> argument '" + sortOrder +
                    "' is invalid. Options for this argument are: 'logical' or 'alpha'.");
            }
        } else {
            sortOrder = "alpha";
        }

        // default scope to global and public if not specified
        if (rgstrScope == null || rgstrScope.length == 0) {
            rgstrScope = new String[3];
            rgstrScope[0] = GLOBAL;
            rgstrScope[1] = PUBLIC;
            rgstrScope[2] = WEB_SERVICE;
        }

        // find all the files to parse
        fm = new FileManager(targetDirectory);
        ArrayList<File> files = fm.getFiles(sourceDirectory);
        ArrayList<ClassModel> cModels = new ArrayList<ClassModel>();

        // set document title & favicon
        fm.setDocumentTitle(documentTitle);
        // set property to determine method sort style and
        // whether or not to hide method descriptions in TOC
        fm.setShowMethodTOCDescription(showMethodTOCDescription);
        fm.setSortOrderStyle(sortOrder);

        // parse each file, creating a class model for it
        for (File fromFile : files) {
            String fromFileName = fromFile.getAbsolutePath();
            if (fromFileName.endsWith(".cls")) {
                ClassModel cModel = parseFileContents(fromFileName);
                if (cModel != null) {
                    cModels.add(cModel);
                }
            }
        }

        // create our Groups
        TreeMap<String, ClassGroup> classGroupMap = createGroupNameToClassGroupMap(cModels, sourceDirectory);

        // load up optional specified file templates
        String bannerContents = fm.parseHTMLFile(bannerFilePath);
        String homeContents = fm.parseHTMLFile(homefilepath);

        // create our set of HTML files
        fm.createDocs(classGroupMap, cModels, bannerContents, homeContents, hostedSourceURL);

        // we are done!
        log("ApexDoc2 has completed!");
    }

    private static void printHelp() {
        log("\nApexDoc2 - a tool for generating documentation from Salesforce Apex code class files.\n");
        log("    Invalid Arguments detected.  The correct syntax is:\n");
        log("apexdoc2 -s <source_directory> [-t <target_directory>] [-u <source_url>] [-h <home_page>] [-b <banner_page>] [-p <scope>] [-d <document_title>] [-c <toc_descriptions>] [-o <sort_order>]\n");
        log("(S)ource Directory  - The folder location which contains your Apex .cls classes");
        log("(T)arget_directory  - Optional. Specifies your target folder where documentation will be generated.");
        log("Source (U)RL        - Optional. Specifies a URL where the source is hosted (so ApexDoc2 can provide links to your source).");
        log("(H)ome Page         - Optional. Specifies the html file that contains the contents for the home page\'s content area.");
        log("(B)anner Page       - Optional. Specifies the text file that contains project information for the documentation header.");
        log("Sco(p)e             - Optional. Semicolon seperated list of scopes to document. Defaults to 'global;public'. ");
        log("(D)ocument Title    - Optional. The value for the document's <title> attribute. Defaults to 'ApexDocs'. ");
        log("TO(C) Descriptions  - Optional. If 'false', will hide the method's description in the class's TOC. Defaults to 'true'.");
        log("Sort (O)rder        - Optional. The order in which class methods, properties, and inner classes are presented. Either 'logical', the order they appear in the source file, or 'alpha', alphabetically. Defaults to 'alpha'. ");
    }

    private static TreeMap<String, ClassGroup> createGroupNameToClassGroupMap(ArrayList<ClassModel> cModels, String sourceDirectory) {
        TreeMap<String, ClassGroup> map = new TreeMap<String, ClassGroup>();
        for (ClassModel cmodel : cModels) {
            String group = cmodel.getClassGroup();
            String groupContent = cmodel.getClassGroupContent();
            if (groupContent != null && !groupContent.isEmpty()) {
                groupContent = sourceDirectory + "/" + groupContent;
            }

            ClassGroup cg;
            if (group != null) {
                cg = map.get(group);
                if (cg == null) {
                    cg = new ClassGroup(group, groupContent);
                } else if (cg.getContentSource() == null) {
                    cg.setContentSource(groupContent);
                }
                // put the new or potentially modified ClassGroup back in the map
                map.put(group, cg);
            }
        }
        return map;
    }

    public static ClassModel parseFileContents(String filePath) {
        try {
            // Get the object of DataInputStream
            FileInputStream fileStream = new FileInputStream(filePath);
            DataInputStream inputStream = new DataInputStream(fileStream);
            BufferedReader bufferReader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            boolean commentsStarted = false;
            boolean docBlockStarted = false;
            int nestedCurlyBraceDepth = 0;

            ClassModel cModel = null;
            ClassModel cModelParent = null;
            ArrayList<String> comments = new ArrayList<String>();
            Stack<ClassModel> cModels = new Stack<ClassModel>();

            // DH: Consider using java.io.StreamTokenizer to read the file a
            // token at a time?
            //
            // new strategy notes:
            // any line with " class " is a class definition
            // any line with scope (global, public, private) is a class, method,
            // or property definition.
            // you can detect a method vs. a property by the presence of ( )'s
            // you can also detect properties by get; or set;, though they may
            // not be on the first line.
            // in apex, methods that start with get and take no params, or set
            // with 1 param, are actually properties.
            //

            int lineNum = 0;
            while ((line = bufferReader.readLine()) != null) {
                lineNum++;

                line = line.trim();
                if (line.length() == 0) continue;

                // ignore anything after // style comments. this allows hiding
                //  of tokens from ApexDoc. However, don't ignore when line
                // doesn't start with //, we want to preserver @example comments
                int shouldIgnore = line.indexOf("//");
                if (shouldIgnore == 0) {
                    line = line.substring(0, shouldIgnore);
                }

                // gather up our comments
                if (line.startsWith("/*")) {
                    commentsStarted = true;
                    boolean commentEnded = false;
                    if (line.startsWith(COMMENT_OPEN)) {
                    	if (line.endsWith(COMMENT_CLOSE)) {
                            line = line.replace(COMMENT_CLOSE, DOC_BLOCK_BREAK);
                            commentEnded = true;
                    	}
                    	comments.add(line);
                    	docBlockStarted = true;
                    }
                    if (line.endsWith(COMMENT_CLOSE) || commentEnded) {
                        commentsStarted = false;
                        docBlockStarted = false;
                    }
                    continue;
                }

                if (commentsStarted && line.endsWith(COMMENT_CLOSE)) {
                    line = line.replace(COMMENT_CLOSE, DOC_BLOCK_BREAK);
                    if (docBlockStarted) {
                    	comments.add(line);
                    	docBlockStarted = false;
                    }
                    commentsStarted = false;
                    continue;
                }

                if (commentsStarted) {
                	if (docBlockStarted) {
                		comments.add(line);
                	}
                    continue;
                }

                // keep track of our nesting so we know which class we are in
                int openCurlies = countChars(line, '{');
                int closeCurlies = countChars(line, '}');
                nestedCurlyBraceDepth += openCurlies;
                nestedCurlyBraceDepth -= closeCurlies;

                // if we are in a nested class, and we just got back to nesting level 1,
                // then we are done with the nested class, and should set its props and methods.
                if (nestedCurlyBraceDepth == 1 && openCurlies != closeCurlies && cModels.size() > 1 && cModel != null) {
                    cModels.pop();
                    cModel = cModels.peek();
                    continue;
                }

                // ignore anything after an =. this avoids confusing properties with methods.
                shouldIgnore = line.indexOf("=");
                if (shouldIgnore > -1) {
                    line = line.substring(0, shouldIgnore);
                }

                // ignore anything after an {. this avoids confusing properties with methods.
                shouldIgnore = line.indexOf("{");
                if (shouldIgnore > -1) {
                    line = line.substring(0, shouldIgnore);
                }

                // skip lines not dealing with scope that are not inner
                // classes, interface methods, or (assumed to be) @isTest
                if (shouldSkipLine(line, cModel)) continue;

                // look for a class. Use regexp to match class since we might be dealing with an inner
                // class or @isTest class without an explicit access modifier (in other words, private)
                if ((line.toLowerCase().matches(".*\\bclass\\b.*") || line.toLowerCase().contains(INTERFACE))) {

                    // create the new class
                    ClassModel cModelNew = new ClassModel(cModelParent, comments, line, lineNum);
                    comments.clear();

                    // keep track of the new class, as long as it wasn't a single liner {}
                    // but handle not having any curlies on the class line!
                    if (openCurlies == 0 || openCurlies != closeCurlies) {
                        cModels.push(cModelNew);
                        cModel = cModelNew;
                    }

                    // add it to its parent (or track the parent)
                    if (cModelParent != null) {
                        cModelParent.addChildClass(cModelNew);
                    }
                    else {
                        cModelParent = cModelNew;
                    }
                    continue;
                }

                // look for a method
                if (line.contains("(")) {
                    // deal with a method over multiple lines.
                    while (!line.contains(")")) {
                        line += bufferReader.readLine();
                        lineNum++;
                    }
                    MethodModel mModel = new MethodModel(comments, line, lineNum);
                    cModel.getMethods().add(mModel);
                    comments.clear();
                    continue;
                }

                // handle set & get within the property
                if (line.contains(" get ") ||
                    line.contains(" set ") ||
                    line.contains(" get;") ||
                    line.contains(" set;") ||
                    line.contains(" get{") ||
                    line.contains(" set{")) {
                    continue;
                }

                // must be a property
                PropertyModel pModel = new PropertyModel(comments, line, lineNum);
                cModel.getProperties().add(pModel);
                comments.clear();
                continue;

            }

            // Close the input stream
            inputStream.close();
            // we only want to return the parent class
            return cModelParent;
        } catch (Exception ex) { // Catch exception if any
            log(ex);
            return null;
        }
    }

    /**
     * @description Helper method to determine if a line being parsed should be skipped.
     * Ignore lines not dealing with scope unless they start with the class keyword. If
     * so, must be an @isTest class or inner class since Apex does not otherwise allow
     * classes without access modifiers. Also, interface methods don't have scope, so
     * don't skip those lines either.
     */
    private static boolean shouldSkipLine(String line, ClassModel cModel) {
        if (containsScope(line) == null &&
            !line.toLowerCase().startsWith(CLASS + " ") &&
                !(cModel != null && cModel.getIsInterface() && line.contains("("))) {
                    return true;
        }

        return false;
    }

    public static String containsScope(String str) {
        for (int i = 0; i < rgstrScope.length; i++) {
            String scope = rgstrScope[i].toLowerCase();
            // if line starts with annotation, replace it so
            //  we can accurately use startsWith to match scope.
            str = str.toLowerCase().trim();
            str = str.replaceFirst("@\\w+\\b\\s{0,1}", "");
            // see if line starts with registered scopes. If it does
            // not, and current scope is private see if line starts
            // with static or final which are implicitly private
            if (str.startsWith(scope + " ") ||
                    (scope.equals(PRIVATE) && (str.startsWith(STATIC + " ") ||
                        str.startsWith(FINAL + " ")))) {
                return scope;
            }
        }
        return null;
    }

    /**
     * @description returns the previous word in a string
     * @param str string to search
     * @param searchIdx where to start searching backwards from
     * @return the previous word, or null if none found.
     */
    public static String previousWord(String str, int searchIdx) {
        if (str == null) return null;
        if (searchIdx >= str.length()) return null;

        int idxStart;
        int idxEnd;
        for (idxStart = searchIdx - 1, idxEnd = 0; idxStart >= 0; idxStart--) {
            if (idxEnd == 0) {
                if (str.charAt(idxStart) == ' ') {
                    continue;
                }
                idxEnd = idxStart + 1;
            } else if (str.charAt(idxStart) == ' ') {
                idxStart++;
                break;
            }
        }

        if (idxStart == -1) {
            return null;
        } else {
            return str.substring(idxStart, idxEnd);
        }
    }

    /**
     * @description Count the number of occurrences of character in the string
     * @param str
     * @param ch
     * @return int
     */
    private static int countChars(String str, char ch) {
        int count = 0;
        for (int i = 0; i < str.length(); ++i) {
            if (str.charAt(i) == ch) {
                ++count;
            }
        }
        return count;
    }

    public static void log(Exception ex) {
        log("");
        ex.printStackTrace();
        System.out.println("\n" + ex.getMessage());
    }

    public static void log(String message) {
        System.out.println(message);
    }
}
