package com.mason.cashify_budgettracker

// CurrencyConverter.kt

object CurrencyConverter {

    private var exchangeRates: Map<String, Double> = mapOf("ZAR" to 1.0)
    private var selectedCurrency: String = "ZAR"

    fun getSelectedCurrency() = selectedCurrency

    fun setSelectedCurrency(currency: String) {
        selectedCurrency = currency
    }

    fun setExchangeRates(rates: Map<String, Double>) {
        exchangeRates = rates
    }

    fun convertAmount(amountInZAR: Double): Double {
        return amountInZAR * (exchangeRates[selectedCurrency] ?: 1.0)
    }

    fun getCurrencySymbol(): String {
        return when (selectedCurrency) {
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            "JPY" -> "¥"
            "ZAR" -> "R"
            else -> ""
        }
    }
}
