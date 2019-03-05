***

**NOTE:** ApexDoc2 has not been released! See the CHANGELOG for updates that will be forthcoming in the next release. This note will be removed with the first release since forking ApexDoc.

***

ApexDoc2
=======

ApexDoc2 is a java app that you can use to document your Salesforce Apex classes.  You tell ApexDoc2 where your class files are, and it will generate a set of static HTML pages that fully document each class, including its properties, methods, enums, and annotations.  Each static HTML page will include an expandable menu on its left hand side that shows the class groups that you've defined, and the classes within those groups.  Command line parameters allow you to control many aspects of ApexDoc2, such as providing your own banner and Project Home HTML for the pages to use.

## Credits
As the name implies, ApexDoc2 is the second rendition of this project. Before finding its new home here, the [original ApexDoc](https://github.com/SalesforceFoundation/ApexDoc) was created by Aslam Bari (http://techsahre.blogspot.com/2011/01/apexdoc-salesforce-code-documentation.html).  It was then taken and extended by David Habib, at Groundwire, in 2011. It was subsequently enhanced by David Habib of the Salesforce Foundation in late 2014 for use with Nonprofit Starter Pack (https://github.com/SalesforceFoundation/Cumulus). Please see the [CHANGELOG](https://github.com/no-stack-dub-sack/ApexDoc2/blob/master/CHANGELOG.md) for a list of enhancements since ApexDoc and additional credits.

## ApexDoc2 Rationale
As the Salesforce Foundation was [no longer able to offer direct support for reported issues or incorporate enhancement requests](https://github.com/SalesforceFoundation/ApexDoc#credits), I am attempting to revitalize the project as ApexDoc2. I believe there is still a need for this project, and pull requests and issues to the original ApexDoc continue to be submitted (however sparsely) which have, or report, some much needed bug fixes. Plus, it might even be fun! If anyone happens to notice this, pull requests, issues, and help are all welcome.

## Command Line Parameters
| parameter | description |
|-------------------------- | ---------------------|
| -s *(s)ource_directory* | The folder location which contains your apex .cls classes.|
| -t *(t)arget_directory* | The folder location where documentation will be generated to.|
| -u *source_(u)rl* | A URL where the source is hosted (so ApexDoc2 can provide links to your source). Optional.|
| -h *(h)ome_page* | The full path to an html file that contains the contents for the home page's content area. Optional.|
| -b *(b)anner_page* | The full path to an html file that contains the content for the banner section of each generated page. Optional.|
| -p *sco(p)e* | A semicolon separated list of scopes to document.  Defaults to 'global;public;webService'. Optional.|
| -d *(d)oc_title* | The value for the document's &lt;title&gt; attribute.  Defaults to 'ApexDocs'. Optional.|
| -c *to(c)_descriptions* | If 'true', will hide the method's description snippet in the class's table of contents. Defaults to 'false'. Optional.|
| -o *sort_(o)rder* | The order in which class methods, properties, and inner classes are presented. Either 'logical', the order they appear in the source file, or 'alpha', alphabetically. Defaults to 'alpha'. Optional.|

## Usage
Copy apexdoc.jar file to your local machine, somewhere on your path.  Each release tag in gitHub has the matching apexdoc.jar attached to it.  Make sure that java is on your path.  Invoke ApexDoc2 like this example:
```
java -jar apexdoc.jar
    -s C:\Users\pweinberg\Workspaces\Force.com IDE\Cumulus3\src\classes
    -t C:\Users\pweinberg\Dropbox\Cumulus\ApexDoc2
    -p global;public;private;testmethod;webService
    -h C:\Users\pweinberg\Dropbox\Cumulus\ApexDoc2\homepage.htm
    -b C:\Users\pweinberg\Dropbox\Cumulus\ApexDoc2\projectheader.htm
    -u http://github.com/SalesforceFoundation/Cumulus/blob/dev/src/classes/
    -d "My Docs Title"
    -o logical
    -c false
```

A favicon has been added with ApexDoc2, so if you'd like to use your own favicon, simply replace the favicon png in the output directory with your own favicon. It must be a PNG and named favicon.png.

## Documenting Class Files
ApexDoc2 scans each class file, and looks for comment blocks with special keywords to identify the documentation to include for a given class, property, enum, or method.  The comment blocks must always begin with /** (or additional *'s) and can cover multiple lines.  Each line must start with * (or whitespace and then *).  The comment block ends with */.  Special tokens are called out with @token.

## Tips
- `@description` tokens are optional; you may omit them.
- Within your ApexDoc2 comments, to indicate code snippets or special keywords, wrap in backticks; e.g. \`String cool = 'cool!';\`. This will be formatted as code in the output file.
- Class and method annotations such as `@IsTest` or `@Future` will be displayed above the class or method's signature, while property annotations such as `@TestVisible` or `@InvocableProperty` will be displayed in the generated properties table.
- *Important note* on implicitly privacy: For ApexDoc2 to best document your class files, it is generally best practice to always give your classes, properties, interfaces, and emums explicit access modifiers. That said, ApexDoc2 does have some ability to detect implicitly private types and methods. For instance, implicitly private `@IsTest` and inner classes, or methods whose signatures start with keywords like `void`, `abstract`, `virtual`, or with primitive types or collections can still be detected and will be assumed to be private. However, in order to not confuse properties with scoped variables, properties *must* start with access modifiers in order to be detected. To best ensure accurate documentation, please always use access modifiers, which will only help to make your code more readable and easily understood!

See examples below.

### Class Comments (includes class-level Interfaces and Enums)
In other words, includes any top-level types that live within a .cls file. Located in the lines above the type's declaration.  The special tokens are all optional.

| token | description |
|-------|-------------|
| @author | The author of the class |
| @date | The date the class was first implemented |
| @group | A group to display this class under, in the menu hierarchy|
| @group-content | A relative path (from the classes source directory) to a static html file that provides content about the group|
| @description | One or more lines that provide an overview of the class|
| @deprecated | Indicates class should no longer be used; message should indicate replacement |
| @see | A comma separated list of fully qualified class or method names; creates link(s) to that class or method in the documentation. The name must be a fully qualified name, even if its a reference to another method in the same class, e.g. Class.Method, Class.InnerClass, Class.InnerClass.InnerClassMethod|

Example
```
/**
* @author Salesforce.com Foundation
* @date 2014
*
* @group Accounts
* @group-content ../../ApexDocContent/Accounts.htm
* @deprecated Replaced by AccountTriggerHandler
* @see AccountTriggerHandler
*
* Look, no description token! Trigger Handler on Accounts that handles ensuring the correct `System__c`
* flags are set on our special accounts (Household, One-to-One), and also detects changes on Household
* Account that requires name updating.
*/
public with sharing class ACCT_Accounts_TDTM extends TDTM_Runnable {
```

### Property and Inner Enum Comments
Located in the lines above a property or an enum nested inside a class.  The special tokens are all optional.

| token | description |
|-------|-------------|
| @description | one or more lines that describe the property|

Examples
```
    /** The countries in which our accounts are located */
    public enum Countries { USA, CANADA, MEXICO, PERU, CHINA, RUSSIA, INDIA }

    /**
    * Specifies whether state and country picklists are enabled in this org.
    * returns true if enabled.
    */
    public static Boolean isStateCountryPicklistsEnabled {
        get {
```

### Method Comments
In order for ApexDoc2 to identify class methods, the method line must contain an explicit scope (global, public, private, testMethod, webService).  The comment block is located in the lines above a method.  The special tokens are all optional.

| token | description |
|-------|-------------|
| @author | The author of the method |
| @date | The date the method was first implemented |
| @description | One or more lines that provide an overview of the method|
| @param *param name* | A description of what the parameter does|
| @return | A description of the return value from the method|
| @deprecated | Indicates method should no longer be used; message should indicate replacement |
| @exception | A list of exceptions a method throws and/or description of Exception(s) that might be thrown |
| @example | Example code usage. This will be wrapped in <code> tags to preserve whitespace|
| @see | A comma separated list of fully qualified class or method names; creates link(s) to that class or method in the documentation. The name must be a fully qualified name, even if its a reference to another method in the same class, e.g. Class.Method, Class.InnerClass, Class.InnerClass.InnerClassMethod|

Example
```
    /**
    * @description Returns field describe data
    * @param objectName the name of the object to look up
    * @param fieldName the name of the field to look up
    * @return the describe field result for the given field
    * @exception System.QueryException
    * @see Utils.getSObjectDescribe, Utils.getPicklistDescribe
    *
    * @example
    * // this is how getFieldDescribe works (the whitespace below will be preserved for complex examples)
    *
    * Schema.DescribeFieldResult result = Utils.getFieldDescribe('Account', 'Name');
    * System.debug(result);
    */
    public static Schema.DescribeFieldResult getFieldDescribe(String objectName, String fieldName) {
```
