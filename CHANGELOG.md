# Changelog

## Unreleased

### Added
- `@exception` token support for methods. Credit goes to [@mlockett](https://github.com/mlockett) for his [pull request](https://github.com/SalesforceFoundation/ApexDoc/pull/75) to the original ApexDoc. Provide a list of exceptions or a description of the exceptions a method might throw.
- `@deprecated` token support for classes and methods. Credit goes to [@mlockett](https://github.com/mlockett) for his [pull request](https://github.com/SalesforceFoundation/ApexDoc/pull/75) to the original ApexDoc. Value for this token should be reasoning or what the method or class was replaced with. For methods, highlights name in red to draw attention.
- `@see` token support for classes and methods. Provide ApexDoc2 with a comma separated list of fully qualified class or method names with this token, and a link to that class or method in the documentation will be created. The name must be a fully qualified name, even if its a reference to another method in the same class, e.g. Class.Method, Class.InnerClass, Class.InnerClass.InnerClassMethod. If a matching class or method cannot be found, the text will not be wrapped in an `<a>` and it will be given a tooltip which says: 'A matching reference cannot be found!'. This works for classes, methods, inner classes and methods on inner classes. Properties cannot be linked to at this time.
- For overloaded constructors and methods, the `@see` token accepts a special syntax: `MyClass.MyInnerClass.MyOverloadedMethod[3]` where `3` is a zero based index indicating the overloaded method to point to (this would indicate the 4th overload of `MyOverloadedMethod`)
- Added support for inline code in most tokens. Wrap a word or words in backticks and they will be formatted as code in the output documentation. Useful for single line snippets and for drawing attention to keywords, etc. Multi-line code examples should still go in the `@example` token. E.g.:

```
/**
 * @description The following will be formatted as code: `String cool = 'cool!';`
 */
 public static String exampleMethod() {
```
- Added support for annotations like `@IsTest`, `@Future` and `@AuraEnabled` to be documented.
- Added syntax highlighting support, compliments of HighlightJS for `@example`s and signatures.
- Added optional **<toc_desc>** command line argument: `-c`. As a matter of preference, if you find the method description snippet in the class's table of contents distracting, you can now hide it with this argument. Defaults to `false`.
- Added optional **<sort_order>** command line argument: `-o`. This controls the order in which methods, properties, and inner classes are presented in the output documentation. Either 'logical', the order they appear in the source file, or 'alpha', alphabetically. Defaults to the ApexDoc original of alphabetical order.
- Added optional **<doc_title>** command line argument: `-d`. Allows you to set the value of the HTML document's `<title>` attribute. Now defaults to 'ApexDocs' instead of 'index.html'.
- Added support for `//` style comments inside of `@example` code snippets in case some explanation of the code is needed, it will appear properly commented out in the output docs.
- Added support for empty line preservation inside of `@example` code snippets where spacing might be needed for complex code examples.
- Added support for line breaks in ApexDoc comments with use of `<br>`, `<br />` or `<br >` tags.
- Changed logo to ApexDoc2 logo, added favicon.
- Added 'All' scope checkbox

### Changed
- Reordered output of `@author`, `@date`, and `@example` tokens so that example snippets always come last for better UI.
- When `@group-content` token is used, no longer link to class group html page on `<a>` tag click. Instead, use SVG info icon so that user can expand menu without navigating to class group content page.
- ~~Minified JQuery for 31.4KB gain.~~ See #9.
- Updated README to document `@author` and `@date` tokens on methods.
- Improved comment parser, removing hundreds of lines of code and supporting multi-line for all tokens. See #16.
- Changed author_file argument to banner_page and -a to -b
- Display properties as a table to better accommodate descriptions and for better overall UI
- Re-write menu code to utilize native HTML5 `<details>` and `<summary>` elements. This eliminates the dependency on CollapsibleList.js, which in turn had a dependency on jQuery. See #9.
- With CollapsibleList.js out of the picture, also remove dependency on jQuery by utilizing native JS for DOM querying (this is a huge win!) See #9.
- Removed Eclipse plugin code. Everyone's using VS Code now :-)
- Use Session Storage instead of cookie to store scope settings
- Denote constructors with `.<init>`
- Use `<details>` & `<summary>` tags for each section so that documentation becomes collapsible and more easily navigable for large files.
- Upgraded the project to Java 8.
- Remove the `ROOT_DIRECTORY` constant in HTML.java in favor of letting the user define the full target path for their docs. Make Target Directory argument required.

### Fixed
- Fixed CSS bug for TOC method descriptions: `text-overflow: ellipsis;` was not working as `white-space: nowrap;` was missing. Also made the width of the descriptions smaller, as they were extending across the whole page which I found a bit distracting. Now will have ellipsis overflow at 500px;
- Fixed line-height CSS for TOC method descriptions. The bottom of letters like 'g' and '__' were getting cut off, now full line is visible.
- Fix comparisons when getting token content so that tokens without values do not get rendered.
- Fix bug where classes without explicit access modifiers (either top-level `@isTest` or inner classes), assumed to be private, are ignored by the parser. See #3.
- Fix issue where strings with scope keywords are misinterpreted by apex doc as documentable code
- Improve scope detection so that methods and properties without explicit access modifiers are not ignored and are treated as private
- Fix various UI bugs where empty tokens were being rendered incorrectly
- Fix bug where when no source url was provided, links were still being created that pointed to the source directory. If files were not placed in the source directory, this would result in a 404. See #8.
- Fix Null Pointer Exception when `@group-content` path is invalid. Show warning instead of throwing exception.
- Fix bug where overloaded methods all had the same ID and TOC links would always point to the first version of the method.