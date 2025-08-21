package com.oldguy.markup.ofx

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import com.oldguy.common.io.TextBuffer
import com.oldguy.markup.XmlParser
import com.oldguy.markup.model.Node
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class, FormatStringsInDatetimeFormats::class)
class OfxParser(textBuffer: TextBuffer) {

    suspend fun parseSgml(textBuffer: TextBuffer): Node {
        XmlParser(textBuffer).apply {
            pullParser = false
            domParser = true
            sgmlNoLeafEndTag = true
            parse { event, model ->
                true
            }
            document.root?.let {
                if (it.name != rootName)
                    throw IllegalStateException("Invalid root name: ${it.name}")
            } ?: throw IllegalStateException("No root found")
            return document.root!!
        }
    }

    /**
     * Traverses a typical credit card transaction download conforming to the OFX spec. Responses contain a
     * SIGNONMSGSRSV1 tag with SONRS content, and a CREDITCARDMSGSRSV1 tag with CCSTMTTRNRS content.
     *
     * This is an example way to traverse an OFX node tree and extract content
     */
    fun transformTransactions(node: Node): Pair<SignonResponse, TransactionResponse> {
        val transactions = mutableListOf<Transaction>()
        var status = Status("", "")
        var uid = ""
        var currency = ""
        var accountId = ""
        var startDate: LocalDate? = null
        var endDate: LocalDate? = null
        var balance = BigDecimal.ZERO
        var balanceAsOf: LocalDateTime? = null
        var signonResponse: SignonResponse? = null
        node.traverse { parent, node ->
            when (node.name) {
                "SONRS" -> {
                    signonResponse = SignonResponse(
                        status = Status(
                            code = node.child("STATUS").child("CODE").text,
                            severity = node.child("STATUS").child("SEVERITY").text
                        ),
                        dtServer = getOfxDate(node.child("DTSERVER").text),
                        language = node.child("LANGUAGE").text,
                        dtProfUp = getOfxDate(node.child("DTPROFUP").text),
                        financialInstitution = FinancialInstitution(
                            org = node.child("FI").child("ORG").text,
                            fid = node.child("FI").child("FID").text
                        ),
                        fiId = node.child("INTU.BID").text
                    )
                }
                "CCSTMTTRNRS" -> {
                    val statementResponseNode = node.child("CCSTMTRS")
                    status = Status(
                        code = node.child("STATUS").child("CODE").text,
                        severity = node.child("STATUS").child("SEVERITY").text
                    )
                    uid = node.child("TRNUID").text
                    statementResponseNode.apply {
                        currency = child("CURDEF").text
                        accountId = child("CCACCTFROM").child("ACCTID").text
                        startDate = getOfxDate(child("BANKTRANLIST").child("DTSTART").text).date
                        endDate = getOfxDate(child("BANKTRANLIST").child("DTEND").text).date
                        balance = BigDecimal.parseString(child("LEDGERBAL").child("BALAMT").text)
                        balanceAsOf = getOfxDate(child("LEDGERBAL").child("DTASOF").text)
                    }

                }
                "STMTTRN" -> {
                    transactions.add(
                        Transaction(
                            type = node.child("TRNTYPE").text,
                            postedDate = getOfxDate(node.child("DTPOSTED").text).date,
                            amount = BigDecimal.parseString(node.child("TRNAMT").text),
                            id = node.child("FITID").text,
                            name = node.child("NAME").text,
                            memo = node.child("MEMO").text
                        )
                    )
                }
                else -> {}
            }
        }
        return Pair(
            signonResponse!!,
            TransactionResponse(
                status = status,
                uid = uid,
                currency = currency,
                accountId = accountId,
                startDate = startDate!!,
                endDate = endDate!!,
                transactions = transactions,
                balance = balance,
                balanceAsOf = balanceAsOf!!
            )
        )
    }

    /**
     * Parses the datetime strings in the OFX spec. Determines if string has timezone info in it and
     * if offset found, uses it. Otherwise UTC is assumed to return a LocalDateTime. If the field is
     * a date, the time component is set to midnight. So use the .date property of the
     * returned LocalDateTime to get the date only.
     */
    private fun getOfxDate(tagValue: String): LocalDateTime {
        var ofxDateFormat = ofxDateFormat1
        var ofxFormat = ofxFormat1
        if (tagValue.length >= ofxDateFormat2.length
            && tagValue[14] == ofxDateFormat2[14]
        ) {
            ofxDateFormat = ofxDateFormat2
            ofxFormat = ofxFormat2
        }
        val time = ofxFormat.parse(tagValue.substring(0, ofxDateFormat.length))
            .toInstant(TimeZone.UTC)
        val tz = if (tagValue.length > ofxDateFormat.length) {
            var timeZone = tagValue.substring(ofxDateFormat.length)
            timeZone = timeZone.substring(1, timeZone.length - 1)
            val tokens = timeZone.split(":").toTypedArray()
            // tokens are offset and TZ id, this code uses TZ id
            TimeZone.of(tokens[1])
        } else
            TimeZone.UTC
        return time.toLocalDateTime(tz)
    }

    companion object {
        val rootName = "OFX"
        private const val ofxDateFormat1 = "yyyyMMddHHmmss"
        private val ofxFormat1 = LocalDateTime.Format {
            byUnicodePattern(ofxDateFormat1)
        }
        private const val ofxDateFormat2 = "yyyyMMddHHmmss.SSS"
        private val ofxFormat2 = LocalDateTime.Format {
            byUnicodePattern(ofxDateFormat2)
        }
    }
}