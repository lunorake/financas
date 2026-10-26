package com.meucofre.app

import org.junit.Assert.assertEquals
import org.junit.Test

class FinanceCalculationsTest {
    @Test
    fun monthlySummaryCountsIncomeAndExpensesOnlyInSelectedMonth() {
        val moves = listOf(
            Move(title = "Salário", amount = 3000.0, type = "Receita", category = "Salário", date = "2026-10-05", account = "a"),
            Move(title = "Mercado", amount = 250.0, type = "Despesa", category = "Alimentação", date = "2026-10-06", account = "a"),
            Move(title = "Conta antiga", amount = 100.0, type = "Despesa", category = "Casa", date = "2026-09-30", account = "a")
        )

        val result = calculateMonthlySummary(moves, "2026-10")

        assertEquals(3000.0, result.income, 0.001)
        assertEquals(250.0, result.expense, 0.001)
        assertEquals(2750.0, result.net, 0.001)
    }

    @Test
    fun transferChangesAccountBalancesButNotConsolidatedBalance() {
        val accounts = listOf(
            Account(id = "a", name = "Conta A", type = "Corrente", initial = 1000.0),
            Account(id = "b", name = "Conta B", type = "Poupança", initial = 200.0)
        )
        val moves = listOf(
            Move(title = "Transferência", amount = 300.0, type = "Transferência", category = "Transferência", date = "2026-10-01", account = "a", destination = "b")
        )

        assertEquals(1200.0, calculateTotalBalance(accounts, moves), 0.001)
    }
}
