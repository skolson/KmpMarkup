package com.oldguy.markup

import com.oldguy.common.io.File
import com.oldguy.markup.ofx.OfxFile
import com.oldguy.markup.ofx.OfxParser
import com.oldguy.markup.ofx.SignonResponse
import com.oldguy.markup.ofx.TransactionResponse
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.fail
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class OfxTests
    :OfxParsing(){

    @Test
    fun testOfxTrans() {
        runTest(timeout = 5.minutes) {
            val path = File.workingDirectory().fullPath + "/TestFiles/OfxTrans.qfx"
            ofxTrans(path)
        }
    }

    @Test
    fun testOfxTransV2() {
        runTest(timeout = 5.minutes) {
            val path = File.workingDirectory().fullPath + "/TestFiles/OfxTransV2.qfx"
            ofxTransV2(path)
        }
    }
}