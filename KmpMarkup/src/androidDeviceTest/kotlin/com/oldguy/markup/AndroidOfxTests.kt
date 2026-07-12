package com.oldguy.markup

import com.oldguy.common.io.File
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

class AndroidOfxTests
    : OfxParsing()
{
    @Test
    fun testOfxTrans() {
        val file = "OfxTrans.qfx"
        runTest(timeout = 5.minutes) {
            AndroidTestBase().apply {
                copyAssetToWorking(file)
            }
            val path = File.workingDirectory().fullPath + "/$file"
            ofxTrans(path)
        }
    }

    @Test
    fun testOfxTransV2() {
        val file = "OfxTransV2.qfx"
        runTest(timeout = 5.minutes) {
            AndroidTestBase().apply {
                copyAssetToWorking(file)
            }
            val path = File.workingDirectory().fullPath + "/$file"
            ofxTransV2(path)
        }
    }
}