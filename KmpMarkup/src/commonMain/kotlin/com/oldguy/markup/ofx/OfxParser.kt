package com.oldguy.markup.ofx

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import com.oldguy.common.io.TextBuffer
import com.oldguy.common.io.charsets.Charsets
import com.oldguy.markup.XmlParser
import com.oldguy.markup.model.Attribute
import com.oldguy.markup.model.Declaration
import com.oldguy.markup.model.ProcessingInstruction
import com.oldguy.markup.model.Node
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime

/**
 * Parses the content of an OFX compliant file.
 * @param textHeaders if this is empty, indicating no text headers were found, then the parser will
 * parse the SGML and require presence of a ProcessInstruction with and OFX target and attributes
 * containing the OFX header values. If for some reason a source has both (shouldn't happen), any
 * ProcessingInstruction values will override the text header values.
 *
 * @property version OFX version parsed from text headers or the OFX PI
 * @property headerVersion OFX header version parsed from text headers or the OFX PI
 * @property security OFX security parsed from text headers or the OFX PI
 * @property oldFileUid OFX old file uid parsed from text headers or the OFX PI
 * @property newFileUid OFX new file uid parsed from text headers or the OFX PI
 * @property encoding OFX encoding parsed from text headers or the XML prolog
 * @property charset OFX charset parsed from text headers or the XML prolog
 */
@OptIn(ExperimentalTime::class, FormatStringsInDatetimeFormats::class)
class OfxParser(textHeaders: Map<String, String>) {
    // These can come from a file or from a processing instruction
    val ofxHeaders = mutableMapOf<String, String>().apply {
        putAll(textHeaders)
    }

    val version get() = ofxHeaders[ofxHeadersList[2]] ?: ""
    val headerVersion get() = ofxHeaders[ofxHeadersList[0]] ?: ""
    val security get() = ofxHeaders[ofxHeadersList[3]] ?: ""
    val oldFileUid get() = ofxHeaders[ofxHeadersList[7]] ?: ""
    val newFileUid get() = ofxHeaders[ofxHeadersList[8]] ?: ""
    val encoding get() = ofxHeaders[ofxHeadersList[4]] ?: ""
    val charset get() = ofxHeaders[ofxHeadersList[5]] ?: ""

    suspend fun parseSgml(textBuffer: TextBuffer): Node {
        XmlParser(textBuffer).apply {
            pullParser = true
            domParser = true
            sgmlNoLeafEndTag = true
            parse { event, model ->
                when (event) {
                    XmlParser.Event.ProcessingInstructionEnd -> {
                        (model as ProcessingInstruction).apply {
                            if (target.uppercase() != "OFX")
                                throw IllegalStateException("Invalid OFX ProcessingInstruction target: $target")
                            Attribute.parseAttributes(content).apply {
                                if (isEmpty())
                                    throw IllegalStateException("No attributes found in OFX ProcessingInstruction")
                                for (attr in this) {
                                    if (!ofxHeadersList.contains(attr.name))
                                        throw IllegalStateException("Attribute ${attr.name} found in OFX ProcessingInstruction not legal")
                                    ofxHeaders[attr.name.uppercase()] = attr.value
                                }
                            }

                        }
                    }
                    XmlParser.Event.Declaration -> {
                        val d = model as Declaration
                        d.encoding?.let {
                            ofxHeaders["ENCODING"] = it
                            if (!d.isCharsetSupported)
                                throw IllegalStateException("Unsupported charset: ${d.encoding}")
                            if (it.uppercase() != textBuffer.charset.name.uppercase()) {
                                textBuffer.changeCharset(Charsets.fromName(it))
                            }
                        }
                    }
                    else -> {}
                }
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
        val ofxHeadersList = listOf(
            "OFXHEADER",
            "DATA",
            "VERSION",
            "SECURITY",
            "ENCODING",
            "CHARSET",
            "COMPRESSION",
            "OLDFILEUID",
            "NEWFILEUID"
        )

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