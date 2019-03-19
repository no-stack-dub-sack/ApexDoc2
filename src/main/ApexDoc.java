package main;

import main.models.*;
import main.models.EnumModel;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.TreeMap;

public class ApexDoc {

    // constants
    private static final String APEX_DOC_VERSION = "1.0.0";
    private static final String COMMENT_CLOSE = "*/";
    private static final String COMMENT_OPEN = "/**";
    private static final String GLOBAL = "global";
    private static final String PUBLIC = "public";
    private static final String WEB_SERVICE = "webService";
    private static final String PROTECTED = "protected";

    public static final String PRIVATE = "private";
    public static final String TEST_METHOD = "testMethod";
    public static final String CLASS = "class";
    public static final String ENUM = "enum";
    public static final String INTERFACE = "interface";
    public static final String ORDER_ALPHA = "alpha";
    public static final String ORDER_LOGICAL = "logical";

    // use special token for marking the end of a doc block
    // comment. Now that we're supporting multi-line for all
    // tokens and using a common comment parser, the parser
    // must know when a block ends in order to prevent weird
    // behavior when lesser scopes than available are indicated
    // e.g. private;public when there are protected methods
    public static final String DOC_BLOCK_BREAK = "@@BREAK@@";
    private static final ArrayList<String> SCOPES;

    // non-constant properties
    public static String[] rgstrScope;
    private static FileManager fileManager;
    public static String targetDirectory;
    private static String sourceDirectory;
    private static int numProcessed = 0;

    static {
        // initialize scopes const
        SCOPES = new ArrayList<String>();
        SCOPES.add(GLOBAL);
        SCOPES.add(PUBLIC);
        SCOPES.add(PRIVATE);
        SCOPES.add(PROTECTED);
        SCOPES.add(WEB_SERVICE);
        SCOPES.add(TEST_METHOD);
    }

    // public entry point when called from the command line.
    public static void main(String[] args) {
        try {
            RunApexDoc(args);
        } catch (Exception ex) {
            Utils.log(ex);
            Utils.printHelp();
            System.exit(-1);
        }
    }

    // public main routine which is used by both command line invocation and
    // Eclipse PlugIn invocation
    public static void RunApexDoc(String[] args) {
        StopWatch timer = new StopWatch();
        timer.start();

        String homefilepath = "";
        String bannerFilePath = "";
        String hostedSourceURL = "";
        String documentTitle = "";
        String sortOrder = ORDER_ALPHA;
        String includes = "";
        String excludes = "";

        boolean showMethodTOCDescription = true;

        // print ApexDoc2 version
        if (args.length == 1 && (args[0].equalsIgnoreCase("--v") || args[0].equalsIgnoreCase("--version"))) {
            Utils.log("ApexDoc2 version " + APEX_DOC_VERSION);
            System.exit(0);
        }

        // parse command line parameters
        for (int i = 0; i < args.length; i++) {
            if (args[i] == null) {
                continue;
            } else if (args[i].equalsIgnoreCase("-s")) {
                sourceDirectory = sourceDirectoryGuard(args[++i]);
            } else if (args[i].equalsIgnoreCase("-u")) {
                hostedSourceURL = sourceURLGuard(args[++i]);
            } else if (args[i].equalsIgnoreCase("-t")) {
                targetDirectory = targetDirectoryGuard(args[++i]);
            } else if (args[i].equalsIgnoreCase("-h")) {
                homefilepath = args[++i];
            } else if (args[i].equalsIgnoreCase("-b")) {
                bannerFilePath = args[++i];
            } else if (args[i].equalsIgnoreCase("-p")) {
                String scope = args[++i];
                rgstrScope = scopeGuard(scope);
            } else if (args[i].equalsIgnoreCase("-d")) {
                documentTitle = args[++i];
            } else if (args[i].equalsIgnoreCase("-c")) {
                showMethodTOCDescription = showTOCGuard(args[++i]);
            } else if (args[i].equalsIgnoreCase("-o")) {
                sortOrder = sortOrderGuard(args[++i].trim());
            } else if (args[i].equalsIgnoreCase("-e")) {
                excludes = args[++i].trim();
            } else if (args[i].equalsIgnoreCase("-i")) {
                includes = args[++i].trim();
            } else {
                Utils.printHelp();
                System.exit(-1);
            }
        }

        // ensure our required arguments are present
        sourceDirectoryGuard(sourceDirectory);
        targetDirectoryGuard(targetDirectory);

        // default scope to global and public if not specified
        if (rgstrScope == null || rgstrScope.length == 0) {
            rgstrScope = new String[3];
            rgstrScope[0] = GLOBAL;
            rgstrScope[1] = PUBLIC;
            rgstrScope[2] = WEB_SERVICE;
        }

        List<String> includeFiles = new ArrayList<String>();
        List<String> excludeFiles = new ArrayList<String>();

        if (!includes.equals("")) {
            includeFiles = Arrays.asList(includes.split(","));
        }

        if (!excludes.equals("")) {
            excludeFiles = Arrays.asList(excludes.split(","));
        }

        // find all the files to parse
        fileManager = new FileManager(targetDirectory);
        ArrayList<File> files = fileManager.getFiles(sourceDirectory, includeFiles, excludeFiles);
        ArrayList<TopLevelModel> models = new ArrayList<TopLevelModel>();
        TreeMap<String, TopLevelModel> modelMap = new TreeMap<String, TopLevelModel>();

        fileManager.setDocumentTitle(documentTitle);

        // set up document generator
        DocGen.sortOrderStyle = sortOrder;
        DocGen.hostedSourceURL = hostedSourceURL;
        DocGen.showMethodTOCDescription = showMethodTOCDescription;

        // parse each file, creating a class or enum model for it
        files.stream().forEach(fromFile -> {
            String fromFileName = fromFile.getAbsolutePath();
            TopLevelModel model = parseFileContents(fromFileName);
            modelMap.put(model.getName().toLowerCase(), model);
            if (model != null) {
                models.add(model);
                numProcessed++;
            }
        });

        // create our Groups
        TreeMap<String, ClassGroup> classGroupMap = createGroupNameMap(models, sourceDirectory);

        // load up optional specified file templates
        String bannerContents = fileManager.parseHTMLFile(bannerFilePath);
        String homeContents = fileManager.parseHTMLFile(homefilepath);

        // create our set of HTML files
        fileManager.createDocs(classGroupMap, modelMap, models, bannerContents, homeContents);

        // we are done!
        timer.stop();
        Utils.log("ApexDoc2 complete! " + numProcessed + " Apex files processed in " + timer.getTime() + " ms.");
        System.exit(0);
    }

    private static TreeMap<String, ClassGroup> createGroupNameMap(ArrayList<TopLevelModel> models,
            String sourceDirectory) {
        TreeMap<String, ClassGroup> map = new TreeMap<String, ClassGroup>();

        models.stream().forEach(model -> {
            String group = model.getGroupName();
            String contentPath = model.getGroupContentPath();
            if (contentPath != null && !contentPath.isEmpty()) {
                contentPath = sourceDirectory + "/" + contentPath;
            }

            ClassGroup cg;
            if (group != null) {
                cg = map.get(group);
                if (cg == null) {
                    cg = new ClassGroup(group, contentPath);
                } else if (cg.getContentSource() == null) {
                    cg.setContentSource(contentPath);
                }
                // put the new or potentially modified ClassGroup back in the map
                map.put(group, cg);
            }
        });

        return map;
    }

    public static TopLevelModel parseFileContents(String filePath) {
        try {
            // Get the object of DataInputStream
            FileInputStream fileStream = new FileInputStream(filePath);
            DataInputStream inputStream = new DataInputStream(fileStream);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            int nestedCurlyBraceDepth = 0, lineNum = 0;
            String line, originalLine, previousLine = "";
            boolean commentsStarted = false, docBlockStarted = false;

            ClassModel cModel = null, cModelParent = null;
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

            while ((line = reader.readLine()) != null) {
                originalLine = line;
                line = line.trim();
                lineNum++;

                if (line.length() == 0) {
                    continue;
                }

                // ignore anything after // style comments. this allows hiding
                // of tokens from ApexDoc. However, don't ignore when line
                // doesn't start with //, we want to preserver @example comments
                int offset = line.indexOf("//");
                if (offset == 0) {
                    line = line.substring(0, offset);
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
                int openCurlies = Utils.countChars(line, '{');
                int closeCurlies = Utils.countChars(line, '}');
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
                offset = line.indexOf("=");
                if (offset > -1) {
                    line = line.substring(0, offset);
                }

                // ignore anything after an '{' (if we're not dealing with an enum)
                // this avoids confusing properties with methods.
                offset = !Utils.isEnum(line) ? line.indexOf("{") : -1;
                if (offset > -1) {
                    line = line.substring(0, offset);
                }

                // skip lines not dealing with scope that are not inner
                // classes, interface methods, or (assumed to be) @isTest
                if (Utils.shouldSkipLine(line, cModel)) {
                    // preserve skipped line, it may be an annotation
                    // line for a class, method, prop, or enum (though
                    // enums support few and are unlikely to have any)
                    previousLine = originalLine;
                    continue;
                }

                // look for a class.
                if (Utils.isClassOrInterface(line)) {
                    // create the new class
                    ClassModel cModelNew = new ClassModel(cModelParent, comments, line, lineNum);
                    Utils.parseAnnotations(previousLine, line, cModelNew);
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
                    } else {
                        cModelParent = cModelNew;
                    }

                    previousLine = null;
                    continue;
                }

                // look for an enum
                if (Utils.isEnum(line)) {
                    EnumModel eModel = new EnumModel(comments, line, lineNum);
                    Utils.parseAnnotations(previousLine, line, eModel);
                    comments.clear();

                    ArrayList<String> values = new ArrayList<String>();
                    String nameLine = eModel.getNameLine();
                    // one-liner enum
                    if (line.endsWith("}")) {
                        line = line.replace("}", "");
                        line = line.replace("{", "");
                        // isolate values of one-liner, split at comma & add to list
                        line = line.substring(line.indexOf(nameLine) + nameLine.length());
                        values.addAll(Arrays.asList(line.trim().split(",")));
                    }
                    // enum is over multiple lines
                    else {
                        // handle fist line, there may be multiple values on it
                        line = line.replace("{", "");
                        line = line.substring(line.indexOf(nameLine) + nameLine.length());
                        values.addAll(Arrays.asList(line.trim().split(",")));

                        // handle each additional line of enum
                        while (!line.contains("}")) {
                            line = reader.readLine();
                            lineNum++;
                            // in case opening curly is on the second line
                            // also handle replacing closing curly for last line
                            String valLine = line.replace("{", "");
                            valLine = valLine.replace("}", "");
                            values.addAll(Arrays.asList(valLine.trim().split(",")));
                        }
                    }

                    // add all enum values to model
                    values.stream().forEach(value -> {
                        if (!value.trim().isEmpty()) {
                            eModel.getValues().add(value.trim());
                        }
                    });

                    // if no class models have been created, and we see an
                    // enum, we must be dealing with a class level enum and
                    // should return early, otherwise we're dealing with
                    // an inner enum and should add to our class model.
                    if (cModel == null && cModels.size() == 0) {
                        reader.close();
                        inputStream.close();
                        return eModel;
                    } else {
                        cModel.getEnums().add(eModel);
                        previousLine = null;
                        continue;
                    }
                }

                // look for a method
                if (line.contains("(")) {
                    int startingLine = lineNum;

                    // deal with a method over multiple lines.
                    while (!line.contains(")")) {
                        line += reader.readLine();
                        lineNum++;
                    }

                    MethodModel mModel = new MethodModel(comments, line, startingLine);
                    Utils.parseAnnotations(previousLine, line, mModel);
                    cModel.getMethods().add(mModel);
                    comments.clear();
                    previousLine = null;
                    continue;
                }

                // handle set & get within the property
                if (line.contains(" get ") ||
                    line.contains(" set ") ||
                    line.contains(" get;") ||
                    line.contains(" set;") ||
                    line.contains(" get{") ||
                    line.contains(" set{")) {
                    previousLine = null;
                    continue;
                }

                // must be a property
                PropertyModel pModel = new PropertyModel(comments, line, lineNum);
                Utils.parseAnnotations(previousLine, line, pModel);
                cModel.getProperties().add(pModel);
                comments.clear();
                previousLine = null;
                continue;
            }

            // Close the input stream
            inputStream.close();
            // we only want to return the parent class
            return cModelParent;
        } catch (Exception ex) { // Catch exception if any
            Utils.log(ex);
            return null;
        }
    }

    // argument guards
    private static String sourceDirectoryGuard(String path) throws IllegalArgumentException {
        if (path != null && new File(path).exists()) {
            return path;
        } else {
            throw new IllegalArgumentException(
                "Value for <source_directory> argument: '" + path +
                "' is invalid. Please provide a valid diectory."
            );
        }
    }

    private static String targetDirectoryGuard(String path) throws IllegalArgumentException {
        if (path != null && path.length() > 0) {
            return path;
        } else {
            throw new IllegalArgumentException(
                "Value for <target_directory> argument: '" + path +
                "' is invalid. Please provide a valid diectory."
            );
        }
    }

    private static String sourceURLGuard(String string) throws IllegalArgumentException {
        if (string != null && Utils.isURL(string)) {
            return string.trim();
        } else {
            throw new IllegalArgumentException(
                "Value for <source_url> argument: '" + string +
                "' is invalid. Please provide a valid URL where your source code is hosted, e.g.: " +
                "https://github.com/no-stack-dub-sack/ApexDoc2/tree/master/src/main"
            );
        }
    }

    private static boolean showTOCGuard(String string) throws IllegalArgumentException {
        if (string != null && (string.equalsIgnoreCase("true") || string.equalsIgnoreCase("false"))) {
            return Boolean.valueOf(string);
        } else {
            throw new IllegalArgumentException(
                "Value for <toc_descriptions> argument: '" + string +
                "' is invalid. Please provide either 'true' or 'false'."
            );
        }
    }

    private static String[] scopeGuard(String scopes) throws IllegalArgumentException {
        String[] scopeRegister = scopes.split(",");
        for (String scope : scopeRegister) {
            if (!SCOPES.contains(scope)) {
                throw new IllegalArgumentException(
                    "Value for <scope> argument: '" + scope +
                    "' is invalid. Please provide a comma delimited list of valid scopes." +
                    " Valid scopes include: " + String.join(", ", SCOPES)
                );
            }
        }

        return scopeRegister;
    }

    private static String sortOrderGuard(String sortOrder) throws IllegalArgumentException {
        if (sortOrder != null && (sortOrder.equalsIgnoreCase(ORDER_LOGICAL) || sortOrder.equalsIgnoreCase(ORDER_ALPHA))) {
            return sortOrder.toLowerCase();
        } else {
            throw new IllegalArgumentException(
                "Value for <sort_order> argument '" + sortOrder +
                "' is invalid. Options for this argument are: 'logical' or 'alpha'."
            );
        }
    }
}
