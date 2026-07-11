package com.oldguy.markup

import com.oldguy.markup.ofx.OfxFile
import com.oldguy.markup.ofx.OfxParser
import com.oldguy.markup.ofx.SignonResponse
import com.oldguy.markup.ofx.TransactionResponse
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.fail
import kotlin.time.Instant

open class OfxParsing {

    suspend fun ofxTrans(
        path: String
    ) {
        val fil = OfxFile(path)
        fil.use { _, textBuffer ->
            OfxParser(fil.ofxHeaders).apply {
                transformTransactions(parseSgml(textBuffer)).apply {
                    transactionTests(first, second)
                }
                parserTests(this)
            }
        }
        assertNotNull(fil.file)
    }

    suspend fun ofxTransV2(
        path: String
    ) {
        val fil = OfxFile(path)
        fil.use { _, textBuffer ->
            OfxParser(fil.ofxHeaders).apply {
                transformTransactions(parseSgml(textBuffer)).apply {
                    transactionTests(first, second)
                }
                parserTests(this)
            }
        }
        assertNotNull(fil.file)
    }

    private fun parserTests(parser: OfxParser) {
        assertEquals("NONE", parser.security)
        assertEquals("NONE", parser.newFileUid)
        assertEquals("NONE", parser.oldFileUid)
        if (parser.headerVersion == "100") {
            assertEquals("102", parser.version)
            assertEquals("1252", parser.charset)
        } else if (parser.headerVersion == "200") {
            assertEquals("211", parser.version)
        } else
            fail("Invalid header version: ${parser.headerVersion}")

    }

    private fun transactionTests(signonResponse: SignonResponse, transactionResponse: TransactionResponse) {
        signonResponse.apply {
            assertEquals("0", status.code)
            assertEquals("INFO", status.severity)
            assertEquals("Elan Financial Services", financialInstitution.org)
            assertEquals("10308", financialInstitution.fid)
            assertEquals("10308", fiId)
            assertEquals(server, dtServer)
            assertEquals(profile, dtProfUp)
            assertEquals("ENG", language)
        }
        transactionResponse.apply {
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
                    1 -> {
                        assertEquals("DEBIT", it.type)
                        assertEquals(LocalDate(2025, 7, 29), it.postedDate)
                        assertEquals("-193.03", it.amount.toPlainString())
                        assertEquals("43279695-20f5-90f7-cc37-b05590135940", it.id)
                        assertEquals("EBM VENTURES           718-686-7", it.name)
                        assertEquals("24116415209712014795603; 05999; ; ; ;", it.memo)
                    }
                    2 -> {
                        assertEquals("DEBIT", it.type)
                        assertEquals(LocalDate(2025, 8, 4), it.postedDate)
                        assertEquals("-28.53", it.amount.toPlainString())
                        assertEquals("b511c36d-c8bf-d704-50f5-9cb4bbfd4c05", it.id)
                        assertEquals("BURGER KING #11982     ROSEVILLE", it.name)
                        assertEquals("24941445213054713322773; 05814; ; ; ;", it.memo)
                    }
                    3-> {
                        assertEquals("DEBIT", it.type)
                        assertEquals(LocalDate(2025, 8, 6), it.postedDate)
                        assertEquals("-45.94", it.amount.toPlainString())
                        assertEquals("b91293ad-c547-e016-fd50-6255e34e173c", it.id)
                        assertEquals("ATLASLIFESYLESHOP      800-945-4", it.name)
                        assertEquals("24072835217017021133744; 05732; ; ; ;", it.memo)
                    }
                }
            }
        }
    }

    companion object {
        val server = Instant.parse("2025-08-19T10:28:03.850Z")
            .toLocalDateTime(TimeZone.UTC)
        val profile = Instant.parse("2005-05-31T05:00:00.000Z")
            .toLocalDateTime(TimeZone.UTC)
    }
}