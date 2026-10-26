package com.meucofre.app

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Modelos centrais do domínio financeiro do Meu Cofre.
 * Mantemos os mesmos campos e valores usados pela versão atual para preservar compatibilidade.
 */
internal data class Move(
    val id: String = newFinanceId(),
    val title: String,
    val amount: Double,
    val type: String,
    val category: String,
    val date: String = todayIsoDate(),
    val account: String,
    val destination: String = ""
)

internal data class Account(
    val id: String = newFinanceId(),
    val name: String,
    val type: String,
    val initial: Double = 0.0
)

internal data class Recurring(
    val id: String = newFinanceId(),
    val title: String,
    val amount: Double,
    val day: Int,
    val category: String,
    val account: String,
    val paidMonths: Set<String> = emptySet()
)

internal data class Goal(
    val id: String = newFinanceId(),
    val title: String,
    val target: Double,
    val saved: Double = 0.0
)

internal fun newFinanceId(): String = UUID.randomUUID().toString()

/** Data ISO local usada nos lançamentos: yyyy-MM-dd. */
internal fun todayIsoDate(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
