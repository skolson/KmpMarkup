package com.oldguy.markup

import com.oldguy.common.io.File
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

class AndroidBasicParsingTests
    : BasicParsing() {

        @Test
        fun testEscapes() {
            val file = "Escapes.xml"
            runTest(timeout = 5.minutes) {
                AndroidTestBase().apply {
                    copyAssetToWorking(file)
                }
                val path = File.workingDirectory().fullPath + "/$file"
                escapesXml(path)
            }
        }

        @Test
        fun testMultilevel() {
            val file = "MultiLevel.xml"
            runTest(timeout = 5.minutes) {
                AndroidTestBase().apply {
                    copyAssetToWorking(file)
                }
                val path = File.workingDirectory().fullPath + "/$file"
                multilevelXml(path)
            }
        }

        @Test
        fun testMediumSize() {
            val file = "MediumSize.xml"
            runTest(timeout = 5.minutes) {
                AndroidTestBase().apply {
                    copyAssetToWorking(file)
                }
                val path = File.workingDirectory().fullPath + "/$file"
                mediumXml(path)
            }
        }

        @Test
        fun testNames() {
            val file = "encrypt.xml"
            runTest {
                AndroidTestBase().apply {
                    copyAssetToWorking(file)
                }
                val path = File.workingDirectory().fullPath + "/$file"
                namesXml(path)
            }
        }
}