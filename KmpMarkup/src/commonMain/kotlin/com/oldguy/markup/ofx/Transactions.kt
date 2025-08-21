package com.oldguy.markup.ofx

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

data class TransactionResponse(
    val status: Status,
    val uid: String,
    val currency: String,
    val accountId: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val transactions: List<Transaction>,
    val balance: BigDecimal,
    val balanceAsOf: LocalDateTime
)

data class Transaction(
    val type: String,
    val postedDate: LocalDate,
    val amount: BigDecimal,
    val id: String,
    val name: String,
    val memo: String
)