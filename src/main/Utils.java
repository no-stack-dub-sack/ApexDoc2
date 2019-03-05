package main;

import main.models.ApexModel;
import main.models.ClassModel;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;

public class Utils {
    // any word that a method or property might start with
    // which would make the method or prop implicitly private
    private static final String[] KEYWORDS;
    private static final String[] COLLECTIONS;

    static {

        KEYWORDS = new String[] {
            "abstract", "final", "static", "virtual", "override",
            "void", "blob", "boolean", "date", "datetime", "decimal",
            "double", "id", "integer", "long", "object", "string", "time" };

        COLLECTIONS = new String[] { "list", "set", "map" };
    }

    public static boolean isClassOrInterface(String line) {
        // Accont for inner classes or @isTest classes without an access modifier; implicitly private
        if ((line.toLowerCase().matches(".*\\bclass\\b.*") || line.toLowerCase().contains(ApexDoc.INTERFACE + " "))) {
            return true;
        }

        return false;
    }

    public static boolean isEnum(String line) {
        line = stripAnnotations(line);
        if (line.matches("^(global\\s+|public\\s+|private\\s+)?enum\\b.*")) {
            return true;
        }

        return false;
    }

    public static String stripAnnotations(String line) {
        int i = 0;
        while (line.trim().startsWith("@")) {
            line = line.trim().replaceFirst("@\\w+\\s*(\\([\\w=.*''/\\s]+\\))?", "");
            if (i >= 100) break; // infinite loop protect, just in case
            i++;
        }

        return line;
    }

    public static void parseAnnotations(String previousLine, String line, ApexModel model) {
        // If previous line is not a comment line, it could be an annotation line.
        // Annotations may also be on the signature line, so check both for matches.
        if (previousLine != null && !previousLine.startsWith("*")) {
            line +=  " " + previousLine;
            if (line.toLowerCase().contains("isTest"))
            System.out.println(line);
        }

        ArrayList<String> matches = new ArrayList<String>();
        Matcher m = Pattern.compile("@\\w+\\s*(\\([\\w=.*''/\\s]+\\))?").matcher(line);

        while (m.find()) {
            matches.add(m.group().trim());
        }

        if (model != null) model.getAnnotations().addAll(matches);
    }

    /**
     * @description Helper method to determine if a line being parsed should be skipped.
     * Ignore lines not dealing with scope unless they start with the certain keywords:
     * We do not want to skip @isTest classes, inner classes, inner interfaces, or innter
     * enums defined without without explicit access modifiers. These are assumed to be
     * private. Also, interface methods don't have scope, so don't skip those lines either.
     */
    public static boolean shouldSkipLine(String line, ClassModel cModel){
        if (containsScope(line) == null &&
            !line.toLowerCase().startsWith(ApexDoc.ENUM + " ") &&
            !line.toLowerCase().startsWith(ApexDoc.CLASS + " ") &&
            !line.toLowerCase().startsWith(ApexDoc.INTERFACE + " ") &&
            !(cModel != null && cModel.getIsInterface() && line.contains("("))) {
                return true;
        }

        return false;
    }

    public static String containsScope(String line) {
        for (int i = 0; i < ApexDoc.rgstrScope.length; i++) {
            String scope = ApexDoc.rgstrScope[i].toLowerCase();

            // if line starts with annotations, replace them, so
            // we can accurately use startsWith to match scope.
            line = stripAnnotations(line);
            line = line.toLowerCase().trim();

            // see if line starts with registered scopes.
            if (line.startsWith(scope + " ")) {
                return scope;
            }

            // If it does not, and current scope is private, see if line
            // starts with keyword or primitive data type, or collection
            // which would mean it is implicitly private. This only works
            // for methods, otherwise we would be matching on method level
            // variables as well. This is why we check for '('. Unfortunately,
            // we cannot check for all data types, so if a method is not given
            // an explicit access modifier & it doesnt start with these keywords,
            // it will be undetectable by ApexDoc2.
            else if (scope.equals(ApexDoc.PRIVATE)) {
                for (String keyword : KEYWORDS) {
                    if (line.startsWith(keyword + " ") && line.contains("(")) {
                        return ApexDoc.PRIVATE;
                    }
                }

                // match implicitly private metehods which return collections
                for (String collection : COLLECTIONS) {
                    if (line.matches("^" + collection + "<.+>\\s.*") && line.contains("(")) {
                        return ApexDoc.PRIVATE;
                    }
                }
            }
        }
        return null;
    }

    public static String previousWord(String str, int searchIdx) {
        if (str == null)
            return null;
        if (searchIdx >= str.length())
            return null;

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

    public static int countChars(String str, char ch) {
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

    public static void printHelp() {
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
        log("Sort (O)rder        - Optional. The order in which class methods, properties, and inner classes are presented. Either 'Utils.logical', the order they appear in the source file, or 'alpha', alphabetically. Defaults to 'alpha'. ");
    }
}