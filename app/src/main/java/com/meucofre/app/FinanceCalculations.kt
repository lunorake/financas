package com.meucofre.app

/** Resultado calculado para um mês, separado da interface para facilitar testes. */
internal data class MonthlySummary(
    val income: Double,
    val expense: Double
) {
    val net: Double get() = income - expense
}

/** Calcula entradas e despesas registradas no mês informado (formato yyyy-MM). */
internal fun calculateMonthlySummary(moves: List<Move>, month: String): MonthlySummary {
    val monthlyMoves = moves.filter { it.date.startsWith(month) }
    return MonthlySummary(
        income = monthlyMoves.filter { it.type == "Receita" }.sumOf { it.amount },
        expense = monthlyMoves.filter { it.type == "Despesa" }.sumOf { it.amount }
    )
}

/**
 * Calcula o saldo consolidado seguindo as regras atuais do app.
 * Transferências reduzem a conta de origem e aumentam a conta de destino,
 * sem serem contabilizadas como receita/despesa no resumo mensal.
 */
internal fun calculateTotalBalance(accounts: List<Account>, moves: List<Move>): Double {
    return accounts.sumOf { account ->
        account.initial +
            moves.filter { it.account == account.id }.sumOf { move ->
                when (move.type) {
                    "Receita" -> move.amount
                    "Despesa", "Transferência" -> -move.amount
                    else -> 0.0
                }
            } +
            moves.filter { it.type == "Transferência" && it.destination == account.id }
                .sumOf { it.amount }
    }
}
