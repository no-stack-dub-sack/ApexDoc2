package main;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Stack;
import java.util.TreeMap;

public class ApexDoc {

    public static FileManager fm;
    public static String[] rgstrScope;
    public static String[] rgstrArgs;

    public ApexDoc() {
        try {
            File file = new File("apex_doc_log.txt");
            FileOutputStream fos = new FileOutputStream(file);
            PrintStream ps = new PrintStream(fos);
            System.setOut(ps);
        } catch (Exception ex) {
        }
    }

    // public entry point when called from the command line.
    public static void main(String[] args) {
        try {
            RunApexDoc(args, null);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println(ex.getMessage() + "\n");
            printHelp();
            System.exit(-1);
        }
    }

    // public entry point when called from the Eclipse PlugIn.
    // assumes PlugIn previously sets rgstrArgs before calling run.
    public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        RunApexDoc(rgstrArgs, monitor);
    }

    // public main routine which is used by both command line invocation and
    // Eclipse PlugIn invocation
    public static void RunApexDoc(String[] args, IProgressMonitor monitor) throws IllegalArgumentException {
        String sourceDirectory = "";
        String targetDirectory = "";
        String homefilepath = "";
        String authorfilepath = "";
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
            } else if (args[i].equalsIgnoreCase("-g")) {
                hostedSourceURL = args[++i];
            } else if (args[i].equalsIgnoreCase("-t")) {
                targetDirectory = args[++i];
            } else if (args[i].equalsIgnoreCase("-h")) {
                homefilepath = args[++i];
            } else if (args[i].equalsIgnoreCase("-a")) {
                authorfilepath = args[++i];
            } else if (args[i].equalsIgnoreCase("-p")) {
                String scope = args[++i];
                rgstrScope = scope.split(";");
            } else if (args[i].equalsIgnoreCase("-d")) {
                documentTitle = args[++i];
            } else if (args[i].equalsIgnoreCase("-n")) {
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
            rgstrScope[0] = "global";
            rgstrScope[1] = "public";
            rgstrScope[2] = "webService";
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


        if (monitor != null) {
            // each file is parsed, html created, written to disk.
            // but for each class file, there is an xml file we'll ignore.
            // plus we add 2 for the author file and home file loading.
            monitor.beginTask("ApexDoc2 - documenting your Apex Class files.", (files.size() / 2) * 3 + 2);
        }
        // parse each file, creating a class model for it
        for (File fromFile : files) {
            String fromFileName = fromFile.getAbsolutePath();
            if (fromFileName.endsWith(".cls")) {
                ClassModel cModel = parseFileContents(fromFileName);
                if (cModel != null) {
                    cModels.add(cModel);
                }
            }
            if (monitor != null) {
                monitor.worked(1);
            }
        }

        // create our Groups
        TreeMap<String, ClassGroup> classGroupMap = createGroupNameToClassGroupMap(cModels, sourceDirectory);

        // load up optional specified file templates
        String projectDetail = fm.parseHTMLFile(authorfilepath);
        if (monitor != null) {
            monitor.worked(1);
        }
        String homeContents = fm.parseHTMLFile(homefilepath);
        if (monitor != null) {
            monitor.worked(1);
        }

        // create our set of HTML files
        fm.createDoc(classGroupMap, cModels, projectDetail, homeContents, hostedSourceURL, monitor);
        if (monitor != null) {
            monitor.done();
        }

        // we are done!
        System.out.println("ApexDoc2 has completed!");
    }

    private static void printHelp() {
        System.out.println("ApexDoc2 - a tool for generating documentation from Salesforce Apex code class files.\n");
        System.out.println("    Invalid Arguments detected.  The correct syntax is:\n");
        System.out.println("apexdoc -s <source_directory> [-t <target_directory>] [-g <source_url>] [-h <homefile>] [-a <authorfile>] [-p <scope>] [-o <sort_order>] [-n <toc_desc>] [-d <doc_title>]\n");
        System.out.println("<source_directory> - The folder location which contains your apex .cls classes");
        System.out.println("<target_directory> - Optional. Specifies your target folder where documentation will be generated.");
        System.out.println("<source_url> - Optional. Specifies a URL where the source is hosted (so ApexDoc2 can provide links to your source).");
        System.out.println("<homefile> - Optional. Specifies the html file that contains the contents for the home page\'s content area.");
        System.out.println("<authorfile> - Optional. Specifies the text file that contains project information for the documentation header.");
        System.out.println("<scope> - Optional. Semicolon seperated list of scopes to document.  Defaults to 'global;public'. ");
        System.out.println("<doc_title> - Optional. The value for the document's <title> attribute.  Defaults to 'ApexDocs'. ");
        System.out.println("<toc_desc> - Optional. If 'false', will hide the method's description in the class's TOC. Defaults to 'true'.");
        System.out.println("<sort_order> - Optional. The order in which class methods, properties, and inner classes are presented. Either 'logical', the order they appear in the source file, or 'alpha', alphabetically. Defaults to 'alpha'. ");
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
                    if (line.startsWith("/**")) {
                    	if (line.endsWith("*/")) {
                            line = line.replace("*/", "");
                            commentEnded = true;
                    	}
                    	comments.add(line);
                    	docBlockStarted = true;
                    }
                    if (line.endsWith("*/") || commentEnded) {
                        commentsStarted = false;
                        docBlockStarted = false;
                    }
                    continue;
                }

                if (commentsStarted && line.endsWith("*/")) {
                    line = line.replace("*/", "");
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
                if ((line.toLowerCase().matches(".*\\bclass\\b.*") || line.toLowerCase().contains(" interface "))) {

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
        } catch (Exception e) { // Catch exception if any
            System.err.println("Error: " + e.getMessage());
        }

        return null;
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
            !line.toLowerCase().startsWith("class ") &&
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
                    (scope.equals("private") && (str.startsWith("static ") ||
                        str.startsWith("final ")))) {
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

    /*
     * private static void debug(ClassModel cModel){ try{
     * System.out.println("Class::::::::::::::::::::::::");
     * if(cModel.getClassName() != null)
     * System.out.println(cModel.getClassName()); if(cModel.getNameLine() !=
     * null) System.out.println(cModel.getNameLine());
     * System.out.println(cModel.getAuthor());
     * System.out.println(cModel.getDescription());
     * System.out.println(cModel.getDate());
     *
     * System.out.println("Properties::::::::::::::::::::::::"); for
     * (PropertyModel property : cModel.getProperties()) {
     * System.out.println(property.getNameLine());
     * System.out.println(property.getDescription()); }
     *
     * System.out.println("Methods::::::::::::::::::::::::"); for (MethodModel
     * method : cModel.getMethods()) {
     * System.out.println(method.getMethodName());
     * System.out.println(method.getAuthor());
     * System.out.println(method.getDescription());
     * System.out.println(method.getDate()); for (String param :
     * method.getParams()) { System.out.println(param); }
     *
     * }
     *
     * }catch (Exception e){ e.printStackTrace(); } }
     */

}
