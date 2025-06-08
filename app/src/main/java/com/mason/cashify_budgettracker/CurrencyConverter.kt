package com.mason.cashify_budgettracker

object CurrencyConverter {

    /*
          -------------------------------------------------------------------------
          Title: How can I get Foreign Exchange Rates-through an API
          Author: Ihsan Khan
          Date Published: 2020
          Date Accessed: 12 May 2025
          Code Version: Not specified
          Availability: https://stackoverflow.com/questions/64150258/how-can-i-get-foreign-exchange-rates-thru-an-api
          -------------------------------------------------------------------------
    */

    //holds exchange rates relative to ZAR
    private var exchangeRates: Map<String, Double> = mapOf("ZAR" to 1.0)
    private var selectedCurrency: String = "ZAR"

    fun getSelectedCurrency() = selectedCurrency

    fun setSelectedCurrency(currency: String) {
        selectedCurrency = currency
    }

    fun setExchangeRates(rates: Map<String, Double>) {
        exchangeRates = rates
    }

    //convert amount from ZAR to selected currency
    fun convertAmount(amountInZAR: Double): Double {
        return amountInZAR * (exchangeRates[selectedCurrency] ?: 1.0)
    }

    //get symbol for the selected currency
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
