package com.oldguy.markup

import com.oldguy.common.io.File
import com.oldguy.markup.model.CData
import com.oldguy.markup.model.Comment
import com.oldguy.markup.model.Declaration
import com.oldguy.markup.model.Node
import com.oldguy.markup.model.ProcessingInstruction
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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
                            }
                            XmlParser.Event.EndTagStart -> {
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
                                    when (nodeIndex) {
                                        0 -> {
                                            assertTrue(model.attributesList.isEmpty())
                                            assertEquals(1, level)
                                        }
                                        1 -> {
                                            assertEquals("foo", model.name)
                                            assertEquals("bar", model.text)
                                            assertTrue { model.attributesList.isEmpty() }
                                            assertTrue { model.namespacesList.isEmpty() }
                                            assertEquals(2, level)
                                        }
                                        2 -> {
                                            assertEquals(1, model.namespacesList.size)
                                            assertEquals("http://hugospace.org", model.namespacesList[0].value)
                                            assertTrue(model.attributesList.isEmpty())
                                            assertEquals(2, level)
                                        }
                                        3 -> {
                                            assertEquals("This is in a new namespace", model.text)
                                            assertEquals(3, level)
                                        }
                                        4 -> {
                                            assertEquals("hiho", model.text)
                                            assertEquals(2, level)
                                        }
                                        5 -> {
                                            assertEquals(1, model.attributesList.size)
                                            assertEquals("testattr", model.attributesList[0].name)
                                            assertEquals("123abc", model.attributesList[0].value)
                                            assertEquals("123abc", model.attributes.attributes["testattr"]?.value ?: "")
                                            assertTrue { model.namespacesList.isEmpty() }
                                        }
                                        else -> {}
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

    companion object {
        private val nodeNames = listOf("root", "foo", "hugo", "hugochild", "ContentData", "bar")

        private val contentData = "(06/15/2004 00:31) <HTML><BODY\n" +
                "        bgcolor=\"#ffffff\"><FONT\n" +
                "        LANG=\"0\">hiho</FONT></BODY></HTML>"
    }
}