package com.mason.cashify_budgettracker

import java.text.DecimalFormat

object CurrencyUtils {
    private val currencyFormat = DecimalFormat("R#,##0.00")

    fun formatCurrency(amount: Double, isExpense: Boolean = false): String {
        val formatted = currencyFormat.format(amount)
        return if (isExpense) "-$formatted" else formatted
    }
}