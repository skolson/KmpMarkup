package com.oldguy.markup.ofx

import kotlinx.datetime.LocalDateTime

data class Status(
    val code: String,
    val severity: String
)

data class FinancialInstitution(
    val org: String,
    val fid: String
)

data class SignonResponse(
    val status: Status,
    val dtServer: LocalDateTime,
    val language: String,
    val dtProfUp: LocalDateTime,
    val financialInstitution: FinancialInstitution,
    val fiId: String
)