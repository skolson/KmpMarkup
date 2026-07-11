package com.oldguy.markup

import com.oldguy.common.io.File
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.minutes

class BasicParsingTests
    : BasicParsing() {

    @Test
    fun testEscapes() {
        runTest(timeout = 5.minutes) {
            val path = File.workingDirectory().fullPath + "/TestFiles/Escapes.xml"
            escapesXml(path)
        }
    }

    @Test
    fun testMultilevel() {
        runTest(timeout = 5.minutes) {
            val path = File.workingDirectory().fullPath + "/TestFiles/MultiLevel.xml"
            multilevelXml(path)
        }
    }

    @Test
    fun testMediumSize() {
        runTest(timeout = 5.minutes) {
            val path = File.workingDirectory().fullPath + "/TestFiles/MediumSize.xml"
            mediumXml(path)
        }
    }

    @Test
    fun testNames() {
        runTest {
            val path = File.workingDirectory().fullPath + "/TestFiles/encrypt.xml"
            namesXml(path)
        }
    }
}