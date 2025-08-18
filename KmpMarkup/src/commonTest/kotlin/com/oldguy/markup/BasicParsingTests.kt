package com.oldguy.markup

import com.oldguy.common.io.File
import com.oldguy.common.io.Uri
import com.oldguy.markup.model.Attribute
import com.oldguy.markup.model.CData
import com.oldguy.markup.model.Comment
import com.oldguy.markup.model.Declaration
import com.oldguy.markup.model.Node
import com.oldguy.markup.model.ProcessingInstruction
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlin.time.Duration.Companion.minutes

class BasicParsingTests {

    @Test
    fun testEscapes() {
        runTest(timeout = 5.minutes) {
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
                                        nodeNames[0] -> {
                                            assertTrue(model.attributesList.isEmpty())
                                            assertEquals(1, model.level)
                                        }
                                        nodeNames[1] -> {
                                            assertEquals("foo", model.name)
                                            assertEquals("bar", model.text)
                                            assertTrue { model.attributesList.isEmpty() }
                                            assertTrue { model.namespacesList.isEmpty() }
                                            assertEquals(2, model.level)
                                        }
                                        nodeNames[2] -> {
                                            val uri = "http://hugospace.org"
                                            assertTrue(model.attributesList.isEmpty())
                                            assertEquals(1, model.namespacesList.size)
                                            val attr = model.namespacesList[0]
                                            assertTrue(attr.isNamespace)
                                            assertEquals(Attribute.reservedPrefix, attr.namespace)
                                            assertEquals(Attribute.reservedPrefix, attr.localName)
                                            assertEquals(Uri(uri).uriString, attr.uri?.uriString)
                                            assertEquals(uri, model.namespacesList[0].value)
                                            assertEquals(2, model.level)
                                        }
                                        nodeNames[3] -> {
                                            assertEquals("This is in a new namespace", model.text)
                                            assertEquals(3, model.level)
                                        }
                                        nodeNames[4] -> {
                                            assertEquals(contentData, model.text)
                                            assertEquals(2, model.level)
                                        }
                                        nodeNames[5] -> {
                                            assertEquals(1, model.attributesList.size)
                                            assertEquals("testattr", model.attributesList[0].name)
                                            assertEquals("123abc", model.attributesList[0].value)
                                            assertEquals("123abc", model.attributes.attributes["testattr"]?.value ?: "")
                                            assertTrue { model.namespacesList.isEmpty() }
                                        }
                                        else -> {
                                            fail("Unknown node name: ${model.name}")
                                        }
                                    }
                                }
                            }
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
                        }
                        when (model) {
                            is CData -> {
                                fail("CData should not be found")
                            }
                            else -> { }
                        }
                        true
                    }
                    assertTrue(startD)
                    assertTrue(endD)
                }
            }
        }
    }

    @Test
    fun testMultilevel() {
        runTest(timeout = 5.minutes) {
            val path = File.workingDirectory().fullPath + "/TestFiles/MultiLevel.xml"
            XmlFile(path).use { textBuffer ->
                XmlParser(textBuffer).apply {
                    pullParser = true
                    domParser = true
                    parse { event, model ->
                        when (event) {
                            XmlParser.Event.StartDocument -> {}
                            XmlParser.Event.EndDocument -> {}
                            XmlParser.Event.StartTag -> { }
                            XmlParser.Event.StartTagEnd -> { }
                            XmlParser.Event.EndTagStart -> { }
                            XmlParser.Event.EmptyTag -> {
                                fail("EmptyTag should not be found")
                            }
                            XmlParser.Event.CommentStart -> {
                                fail("CommentStart should not be found")
                            }
                            XmlParser.Event.CommentEnd -> {
                                fail("CommentEnd should not be found")
                            }
                            XmlParser.Event.CDataStart -> {
                                fail("CDataStart should not be found")
                            }
                            XmlParser.Event.CDataEnd -> {
                                fail("CDataEnd should not be found")
                            }
                            XmlParser.Event.ProcessingInstruction -> { }
                            XmlParser.Event.ProcessingInstructionEnd ->  { }
                            XmlParser.Event.Declaration -> {
                                assertTrue(model is Declaration)
                                assertEquals("xml", model.target)
                                assertEquals("1.0", model.version)
                                assertTrue(model.isVersionValid)
                                assertEquals(model.encoding, "UTF-8")
                            }
                            XmlParser.Event.DocType -> {
                                fail("DocType should not be found")
                            }
                            XmlParser.Event.CharacterEscape -> {
                                fail("CharacterEscape should not be found")
                            }
                        }
                        true
                    }
                    val bk101 = "bk101"
                    val bk102 = "bk102"
                    this.document.apply {
                        assertEquals(1, prolog.size)
                        val decl = prolog[0] as Declaration
                        assertEquals("xml", decl.target)
                        assertEquals("UTF-8", decl.encoding)
                        assertEquals("1.0", decl.version)
                        assertNotNull(root)
                        root?.traverse { parent, node ->
                            when (node.name) {
                                "catalog" -> {
                                    assertNull(parent)
                                    assertEquals(2, node.children.size)
                                    assertTrue(node.text.isEmpty())
                                    assertFalse(node.isLeafNode)
                                }
                                "book" -> {
                                    assertEquals("catalog", parent?.name ?: "")
                                    assertFalse(node.isLeafNode)
                                    when (node.attribute("id")?.value) {
                                        bk101 -> {
                                            assertTrue(node.text.isEmpty())
                                            assertEquals(7, node.children.size)
                                        }

                                        bk102 -> {
                                            assertTrue(node.text.isEmpty())
                                            assertEquals(6, node.children.size)
                                        }
                                        else -> fail("Unknown book id: ${node.attribute("id")}")
                                    }
                                }
                                "author" -> {
                                    assertEquals("book", parent?.name ?: "")
                                    assertTrue(node.isLeafNode)
                                    when (parent?.attribute("id")?.value) {
                                        bk101 -> assertEquals("Gambardella, Matthew", node.text)
                                        bk102 -> assertEquals("Corets, Eva", node.text)
                                        else -> fail("Parent invalid attribute: id, ${parent?.attribute("id")?.value ?: "null"}")
                                    }
                                }
                                "title" -> {
                                    assertEquals("book", parent?.name ?: "")
                                    assertTrue(node.isLeafNode)
                                    when (parent?.attribute("id")?.value) {
                                        bk101 -> assertEquals("XML Developer's Guide", node.text)
                                        bk102 -> assertEquals("Maeve Ascendant", node.text)
                                        else -> fail("Parent invalid attribute: id, ${parent?.attribute("id")?.value ?: "null"}")
                                    }
                                }
                                "genre" -> {
                                    assertEquals("book", parent?.name ?: "")
                                    assertTrue(node.isLeafNode)
                                    when (parent?.attribute("id")?.value) {
                                        bk101 -> assertEquals("Computer", node.text)
                                        bk102 -> assertEquals("Fantasy", node.text)
                                        else -> fail("Parent invalid attribute: id, ${parent?.attribute("id")?.value ?: "null"}")
                                    }
                                }
                                "price" -> {
                                    assertEquals("book", parent?.name ?: "")
                                    assertTrue(node.isLeafNode)
                                    when (parent?.attribute("id")?.value) {
                                        bk101 -> assertEquals("44.95", node.text)
                                        bk102 -> assertEquals("5.95", node.text)
                                        else -> fail("Parent invalid attribute: id, ${parent?.attribute("id")?.value ?: "null"}")
                                    }
                                }
                                "publish_date" -> {
                                    assertEquals("book", parent?.name ?: "")
                                    assertTrue(node.isLeafNode)
                                    when (parent?.attribute("id")?.value) {
                                        bk101 -> assertEquals("2000-10-01", node.text)
                                        bk102 -> assertEquals("2000-11-17", node.text)
                                        else -> fail("Parent invalid attribute: id, ${parent?.attribute("id")?.value ?: "null"}")
                                    }
                                }
                                "description" -> {
                                    assertEquals("book", parent?.name ?: "")
                                    assertTrue(node.isLeafNode)
                                    when (parent?.attribute("id")?.value) {
                                        bk101 -> assertEquals("An in-depth look at creating applications with XML.", node.text)
                                        bk102 -> assertEquals("A fantasy adventure in the ancient world of Maeve.", node.text)
                                        else -> fail("Parent invalid attribute: id, ${parent?.attribute("id")?.value ?: "null"}")
                                    }
                                }
                                "reviews" -> {
                                    assertEquals("book", parent?.name ?: "")
                                    assertEquals(bk101, parent?.attribute("id")?.value)
                                    assertEquals(2, node.children.size)
                                    assertFalse(node.isLeafNode)
                                }
                                "review" -> {
                                    assertEquals("reviews", parent?.name ?: "")
                                    assertEquals(2, node.children.size)
                                    assertFalse(node.isLeafNode)
                                    when (node.attribute("id")?.value) {
                                        "rev001" -> {
                                            assertEquals("5", node.children[0].text)
                                            assertEquals("Excellent guide for XML beginners.", node.children[1].text)
                                        }
                                        "rev002" -> {
                                            assertEquals("4", node.children[0].text)
                                            assertEquals("Covers a lot of ground, but could be more concise.", node.children[1].text)
                                        }
                                        else -> fail("Unknown review id: ${node.attribute("id")}")
                                    }
                                }
                                "rating" -> {
                                    assertEquals("review", parent?.name ?: "")
                                    assertTrue(node.isLeafNode)
                                    when (parent?.attribute("id")?.value) {
                                        "rev001" -> assertEquals("5", node.text)
                                        "rev002" -> assertEquals("4", node.text)
                                        else -> fail("Unknown review id: ${node.attribute("id")}")
                                    }
                                }
                                "comment" -> {
                                    assertEquals("review", parent?.name ?: "")
                                    assertTrue(node.isLeafNode)
                                    when (parent?.attribute("id")?.value) {
                                        "rev001" -> assertEquals("Excellent guide for XML beginners.", node.text)
                                        "rev002" -> assertEquals("Covers a lot of ground, but could be more concise.", node.text)
                                        else -> fail("Unknown review id: ${node.attribute("id")}")
                                    }
                                }
                                else -> {
                                    fail("Unknown node name: ${node.name}")
                                }
                            }

                        }
                    }
                }
            }
        }
    }

    companion object {
        private val nodeNames = listOf("root", "foo", "hugo", "hugochild", "ContentData", "bar")

        private val contentData = "(06/15/2004 00:31) <HTML><BODY\n" +
                "        bgcolor=\"#ffffff\"><FONT\n" +
                "        LANG=\"0\">hiho</FONT></BODY></HTML>"
    }
}