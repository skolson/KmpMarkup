## KmpMarkup

This library is a Kotlin Multiplatform implementation of a lightweight XML parser. It also has the beginnings of OFX support which is SGML with text headers.  There is no native/cinterop code other than what is in the KmpIO library, so the parser will act identically across all supported platforms. It has pull parser support, and basic DOM support, configured by options. It does not support DTDs or Schemas or full document validation.

Targets supported include JVM, Android, Ios, IosSimulatorX64, IosSimulatorArm64, LinuxX64, and LinuxArm64

The library is not published to Maven as of this writing. I'll be surprised if there is enough interest to warrant this being published :-)

The next markup parser planned is YAML.

Treat as alpha quality

## Reason for Existence

Apple and Linux native have XML libraries available via cinterop, which are complete XML implementations. Android and JVM have multiple XML libraries to choose from. Each implementation has its own idiosyncrasies, especially when needing "relaxed" syntaxing for more SGML dialects like OFX files (Quicken format). OFX files use SGML since their leaf nodes with text values optionally do not use closing tags. This is a simple pure Kotlin library that handles both in a consistent way. 


## Dependencies

- KmpIO is used for File IO and Charset (encoding) support. The basic text parsing tools in the TextBuffer class are the foundation of the XmlParser class in this library.
- kotlinx-datetime is used for parsing dates in the OFX support
- Ionspin BigDecimal is used for parsing amounts in the OFX support

## Usage

Define the library as a gradle dependency (assumes mavenLocal() is defined as a repo in your build.gradle scripts):

```
    dependencies {
        implementation("io.github.skolson:kmp-markup:0.1.0")
    }  
```

## Samples (from unit tests)

### Simple XML parsing

Part of a unit test for a file called Escapes.xml (see the TestFiles directory in the repo) is shown below. This file has a smattering of XML features, including escaped characters, namespaces, attributes, etc. It uses the pull parser option and disables DOM collection.

```
            val path = File.workingDirectory().fullPath + "/TestFiles/Escapes.xml"
            XmlFile(path).use { textBuffer ->
                XmlParser(textBuffer).apply {
                    pullParser = true
                    domParser = false
                    var startD = false
                    var endD = false
                    var nodeIndex = -1
                    parse { event, model ->
                        when (event) {
                            XmlParser.Event.StartDocument -> startD = true
                            XmlParser.Event.EndDocument -> endD = true
                            XmlParser.Event.StartTag -> {
                                nodeIndex++
                                if (model !is Node)
                                    fail("model is not a node: ${model.type}")
                                else {
                                    if (nodeIndex > 0) {
                                        assertNotNull(root)
                                        root?.let {
                                            assertEquals("root", it.name)
                                        }
                                    }
                                    val name = nodeNames[nodeIndex]
                                    assertEquals(name, model.name)
                                }
                            }
                            XmlParser.Event.EndTagStart -> {
                                if (model !is Node)
                                    fail("model is not a node: ${model.type}")
                                else {
                                    when (model.name) {
                                        "root" -> {
                                            assertTrue(model.attributesList.isEmpty())
                                            assertEquals(1, model.level)
                                        }
                                        "foo" -> {
                                            assertEquals("foo", model.name)
                                            assertEquals("bar", model.text)
                                            assertTrue { model.attributesList.isEmpty() }
                                            assertTrue { model.namespacesList.isEmpty() }
                                            assertEquals(2, model.level)
                                        }
                                    ... etc ...
                            XmlParser.Event.ProcessingInstructionEnd -> {
                                when (model) {
                                    is Declaration -> {
                                        model.apply {
                                            assertEquals("xml", target)
                                            assertEquals("1.0", version)
                                            assertTrue(isVersionValid)
                                            assertTrue(isStandaloneValid)
                                            assertTrue(isStandalone)
                                            assertEquals("utf-8", encoding)
                                            assertTrue(isCharsetSupported)
                                        }
                                    }
                                    is ProcessingInstruction -> {
                                        fail("ProcessingInstruction should only be Declaration")
                                    }
                                    else -> {
                                        fail("model is invalid for ProcessingInstructionEnd")
                                    }
                                }
                            }        
                            XmlParser.Event.CommentEnd -> {
                                (model as Comment).apply {
                                    if (text.startsWith(" c"))
                                        assertEquals(" comment ", text)
                                    else
                                        assertEquals("Any comment text here ", text)
                                }
                            }
                            else -> {}
                            ... etc ...
```


### OFX transactions file parsing

Part of A unit test to parse an OFX (Quicken) transaction file using the DOM parser is shown below as an example of how to use the OFX classes. OFX files have some text name:value text lines, followed by nearly XML, except leaf elements with text have no trailing/closing tags. In this test, a file named OfxTrans.qfx is parsed into a DOM model for credit card transactions.

In this test, the OfxFile class reads the file, parses and retains the text name:value pairs from the front of the file, determines which charset the ENCODING entry specifies, and uses that for parsing the rest. The OfxParser class then configures the underlying XmlParser class to use a DOM parser and support missing leaf node end tags.  It then parses the XML into a DOM model using the parseSgml method of the OfxParser, which uses the TextBuffer provided by OfxFile. Finally the vanilla DOM objects are transformed into OFX-specific transaction related business objects. The unit test then verifies the contents of the OFX SignonResponse content, and the TransactionResponse content. 

```
            val path = File.workingDirectory().fullPath + "/TestFiles/OfxTrans.qfx"
            OfxFile(path).use { textBuffer ->
                OfxParser(textBuffer).apply {
                    transformTransactions(parseSgml(textBuffer)).apply {
                        first.apply {
                            assertEquals("0", status.code)
                            assertEquals("INFO", status.severity)
                            assertEquals("Elan Financial Services", financialInstitution.org)
                            assertEquals("10308", financialInstitution.fid)
                            assertEquals("10308", fiId)
                            assertEquals(server, dtServer)
                            assertEquals(profile, dtProfUp)
                            assertEquals("ENG", language)
                        }
                        second.apply {
                            assertEquals("0", status.code)
                            assertEquals("INFO", status.severity)
                            assertEquals("USD", currency)
                            assertEquals("0", uid)
                            assertEquals("123456XXXXXX1234", accountId)
                            assertEquals(LocalDate(2025, 7, 18), startDate)
                            assertEquals(LocalDate(2025, 8, 23), endDate)
                            assertEquals(7, transactions.size)
                            transactions.forEachIndexed { i, it ->
                                when(i) {
                                    0 -> {
                                        assertEquals("DEBIT", it.type)
                                        assertEquals(LocalDate(2025, 7, 21), it.postedDate)
                                        assertEquals("-31.5", it.amount.toPlainString())
                                        assertEquals("090a9fb8-748c-380a-7648-72b5147a68a1", it.id)
                                        assertEquals("McDonalds 32113        191-67897", it.name)
                                        assertEquals("24793385202000034529075; 05814; ; ; ;", it.memo)
                                    }
                            ... etc ...
```

See the unit tests in src/commonTest for more examples of how to use the library.

### Major Classes

A model package contains various data classes for retaining the DOM parsing content.  The pull parser also makes limited use of these to help with parsing, as each Event has a matching model object. The model classes include:

- Node - the basic XML element, with attributes, namespaces, optional text, CData, or child elements/Nodes
- Attributes - a map of Attribute instances (if any) and Namespaces (if any)
- Attribute - a name/value pair
- CData - content from a CDATA tag usage in an element/Node
- Comment
- Document - this contains the root element reference in the DOM, the prolog, and any top-level comments
- Model - a sealed base class for all the classes in this package
- Declaration - the standard XML prolog
- ProcessingInstruction - any PI that is not the standard prolog

### ParseException

There are lots of these potentially thrown by the parser while parsing content. Contains an error message, a line number of the text input where the error occurred, and a line position.

### XmlFile

This has the logic for BOM detection, and charset determination based on the rules in the XML specification: https://www.w3.org/TR/xml/#sec-guessing .  Once a Charset is determined, a TextFile is opened as a source for a TextBuffer instance using the specified Charset. The TextBuffer instance is then positioned at the first XML character (past any BOM) for parsing using the XmlParser class.

Note that use of XmlFile is not required for cases where XML content is already in String form.  A TextBuffer instance coded with a List<String> (as an example) can also provide the XmlParser XML content.  XmlFile is useful in cases where a BOM may be present, and Charset decoding is required.

Typical example with the convenience function 'use' which handles file open/close, BOM/encoding detection and creation of a TextBuffer using this file as a byte source decoded using the detected Charset:

```
            XmlFile(path).use { textBuffer ->
                XmlParser(textBuffer).apply {
                    // configure the parser as needed
                    // invoke the parser, the lambda will be invoked, if the pull parser is configured, for each Event encountered. Each Event has an associated Model instance containing the related data available for that Event.
                    parse { event, model ->
                        true
                    }
                    doc = document  // this is the DOM model container if DOM parsing was configured.
                }
            }    
```

### XmlParser

Configuration options are available, which must be set as desired before invoking the parse function. 

- pullParser - true if the parse function lambda should be called for events as they are encountered by the parser.
- domParser - true if the DOM model should be retained. False if not. 
- sgmlNoLeafEndTag - true if the XML being parsed is really SGML where the XML end tag is omitted on leaf nodes. Default is false.

Both parsers can be enabled if desired for whatever reason. 

The pull parser invokes the parse function lambda for each of these supported events:
```
    enum class Event {
        StartDocument,
        EndDocument,
        StartTag,
        StartTagEnd,
        EndTagStart,
        EmptyTag,
        CommentStart,
        CommentEnd,
        CDataStart,
        CDataEnd,
        ProcessingInstruction,
        ProcessingInstructionEnd,
        Declaration,
        DocType,
        Entity
    }
```

The parse function has a lambda that is called with an Event and a matching Model instance for that event. The parse function can also terminate parsing by returning false. If it always returns true, it will be called repeatedly until the document is consumed. 

### OFX package

This package contains classes used to parse an OFX file containing credit card transactions, which was all that was required of the initial implementation. The foundation is laid for other OFX document types. OFX is a huge spec, this is just one small piece.

### OfxFile

Like XmlFile, this has the same setup.  It reads a set of name:value headers from the text file using UTF-8. One of the name:value pairs can specify a different encoding Charset, which this class detects and uses, if it is a Charset supported by KmpIO library.  The name/value pairs are retained in a Map for query. A TextBuffer instance is created with the detected Charset and positioned at the start of the SGML content after the text headers.

### OfxParser

This offers two functions:
- "parseSgml" which configures an XmlParser with the correction options and the TextBuffer (usually from OfxFile but can be any TextBuffer coded with source data). "parseSgml" uses the DOM parser option of XmlParser. 
- "transformTransactions" function which maps the low-level DOM objects from parseSgml into OFX business objects using LocalDate and LocalDateTime instances for dates and datetimes, and BigDecimal for amounts.