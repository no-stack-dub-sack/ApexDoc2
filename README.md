***

**NOTE:** ApexDoc2 has not yet been released! See the CHANGELOG for updates that will be forthcoming in the next release. This note will be removed with the first release since forking ApexDoc.

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
| --v | Used alone; print the ApexDoc2 version. E.g. `ApexDoc2 --v` |

## Usage
For easiest usage, we recommend creating a simple batch file to run the program. Copy ApexDoc2.jar file to your local machine (each release tag in gitHub has the matching apexdoc2.jar attached to it). Make sure that java is on your path, and create a batch file called 'ApexDoc2' containing the following:

```batch
@echo off
REM File Name: ApexDoc2.bat
REM replace the path below with the path where the ApexDoc2.jar file lives on your machine
java -jar C:\Users\path\to\ApexDoc2.jar %*
```

Then add the directory where the batch file lives to your PATH environment variable, and you will be able to call ApexDoc2 like so:

```shell
ApexDoc2
    -s C:\Users\pweinberg\Workspaces\MyProject\src\classes
    -t "C:\Users\pweinberg\Documentation\My Project Docs"
    -p global;public;private;testmethod;webService
    -h C:\Users\pweinberg\Documentation\assets\homepage.htm
    -b C:\Users\pweinberg\Documentation\assets\projectheader.htm
    -u http://github.com/NotARealAccount/MyProject/blob/dev/src/classes/
    -d "My Apex Project"
    -o logical
    -c false
```

A favicon has been added with ApexDoc2, so if you'd like to use your own favicon, simply replace the favicon png in the output directory with your own favicon. It must be a PNG and named favicon.png.

## Documenting Class Files
ApexDoc2 scans each class file, and looks for comment blocks with special keywords to identify the documentation to include for a given class, property, enum, or method.  The comment blocks must always begin with /** (or additional *'s) and can cover multiple lines.  Each line must start with * (or whitespace and then *).  The comment block ends with */.  Special tokens are called out with @token.

### Documentation Tokens
Note that in the table below, the 'Class' column includes any top-level types that live within a .cls file, including interfaces and enums. Tokens are all optional and are located in the lines above the type's declaration.

| Token | Description | Class | Method | Enum | Property |
|-------|-------------|-------|--------|------|----------|
| **@description** | A description or overview of the code you are documenting. | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: |
| **@group** | The group to display a class under in the menu hierarchy. Un-grouped classes will be placed under 'Miscellaneous'. | :heavy_check_mark: | :x: | :x: | :x: |
| **@group-content** | A relative path (to the provided source directory) to a static HTML file that provides content about the group. The group will be hyperlinked to this content, which will be parsed and placed into the documentation's content window. | :heavy_check_mark: | :x: | :x: | :x: |
| **@author** | The author of a class or method. | :heavy_check_mark: | :heavy_check_mark: | :x: | :x: |
| **@date** | The date a class or method was first implemented. | :heavy_check_mark: | :heavy_check_mark: | :x: | :x: |
| **@deprecated** | Indicates class or method should no longer be used; message should indicate replacement path. | :heavy_check_mark: | :heavy_check_mark: | :x: | :x: |
| **@example** | Example code usage. Start your example on the line below the token. Code will be given syntax highlighting complements of [highlight.js](https://highlightjs.org/) and be wrapped in `<pre><code>` tags to preserve whitespace. | :heavy_check_mark: | :heavy_check_mark: | :x: | :x: |
| **@param** *param name* | A description of what a method's parameter does. | :x: | :heavy_check_mark: | :x: | :x: |
| **@return** | A description of a method's return value. | :x: | :heavy_check_mark: | :x: | :x: |
| **@exception** | A description of or list of exceptions that a method throws. | :x: | :heavy_check_mark: | :x: | :x: |
| **@see** | A comma separated list of URLs, markdown URLs (e.g. '\[ApexDoc2\]\(https://github.com/no-stack-dub-sack/ApexDoc2)', or fully qualified class or method names. The latter creates link(s) to that class or method in the documentation. The name must be a fully qualified name, even if its a reference to another method in the same class, e.g. 'Class.Method', 'Class.InnerClass', 'Class.InnerClass.InnerClassMethod'. For overloaded constructors and methods, the `@see` token accepts a special syntax: 'MyClass.MyInnerClass.MyOverloadedMethod[3]' where '3' is a zero based index indicating the overloaded method to link to (this would indicate the 4th overload of `MyOverloadedMethod`). When a link cannot be made, a tooltip will be shown on hover. | :heavy_check_mark: | :heavy_check_mark: | :x: | :x: |

### Special Tokens
In addition to the `@token`s listed above, there are a few other special tokens to be aware of:

| Token | Description |
|-------|-------------|
| \` \` | Backticks, \` \`, can be used to indicate inline code within your ApexDoc2 comments. E.g. \`String cool = 'cool!';\` &mdash; the expression within the backticks will be formatted as code. |
| &lt;br&gt; | The &lt;br&gt; tag can be used to render line breaks in your comments when more complex formatting is needed. &lt;br /&gt; is also acceptable. |

### Class Comments (includes class-level Interfaces and Enums)
Located in the lines above any top-level type that lives within a .cls file, or in the lines above inner classes and interfaces.

```apex
/**
* @author P. Weinberg
* @date 2014
*
* @group Core Framework
* @group-content ../../ApexDocContent/Core_Framework.html
* @deprecated Replaced by `JobExtension`
* @see `JobExtension`, `JobPluggable`
*
* @description This class is the base class from which all 'Plugins' will extend. It provides a suite of abstract and
* virtual methods, which implement the `JobPluggable` interface.
* &lt;br&gt;&lt;br&gt;
* To ensure flexibility, all methods can be overridden to accommodate a particular plugin's needs.
*/
public abstract class JobPlugin implements JobPluggable {
```

### Property and Inner Enum Comments
These are the simplest comment blocks. They only accept description tokens (the token itself may optionally be omitted for brevity). For properties to be detected by ApexDoc2, they **must** be given an explicit access modifier or have signatures beginning with the `static` keywork. **Other implicitly private properties will not be detected.**

```apex
    /** The countries in which our accounts are located */
    public enum Countries { USA, CANADA, MEXICO, PERU, CHINA, RUSSIA, INDIA }

    /**
    * @description Specifies whether state and country picklists are enabled in this org.
    * Returns true if enabled.
    */
    public static Boolean isStateCountryPicklistsEnabled {
        get {
```

### Method Comments
In order for ApexDoc2 to best identify class methods, the method line must contain an explicit access modifier / scope: global, public, private, testMethod, webService (some implicitly private methods can be detected, but be wary of this. See the note on implicit privacy in the [Tips](#Tips) section below).

```apex
    /**
    * @description A utility method for returning field describe data
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

### Tips
- `@description` tokens are optional; you may omit them. ApexDoc2 comments without a token will be interpreted as the type's description.
- All tokens except `@group`, and `@group-name` support comments over multiple lines.
- Class and method annotations such as `@IsTest` or `@Future` will be displayed above the class or method's signature, while property annotations such as `@TestVisible` or `@InvocableProperty` will be displayed in the generated properties table.
- **Important note** on implicitly privacy: For ApexDoc2 to best document your class files, it is generally best practice to always give your classes, methods, properties, interfaces, and emums explicit access modifiers. That said, ApexDoc2 does have some ability to detect implicitly private types and methods. For instance, implicitly private `@IsTest` and inner classes, or methods whose signatures start with keywords like `void`, `abstract`, `override` and `virtual`, or with collections or primitive types can still be detected and will be assumed to be private (methods without access modifiers and whose signatures start with custom types or complex built-in types e.g. `Messaging.SendEmailResult[]` will not be detectable). However, in order to not confuse properties with local variables, properties *must* start with access modifiers or the `static` keyword in order to be detected. To best ensure accurate documentation, please always use access modifiers, which can only help to keep your code readable and easily understood!

## Support
ApexDox2 uses some modern HTML5 tags and JavaScript features, so unfortunately Internet Explorer is not supported. If IE supported the HTML5 tags we use (namely `<summary>` and `<details>` for easy, script-less collapsible menus and sections), I would have made an effort to keep the JS supportable by IE, but since IE doesn't support the basic building blocks of the documentation, it made no sense to hold back on the JavaScript, even though there's very little of it.