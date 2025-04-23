package com.mason.cashify_budgettracker.model

data class Expense(
    val id: String = "",
    val amount: Double = 0.0,
    val description: String = "",
    val date: String = "",       // Format: "DD/MM/YYYY"
    val category: String = "",   // Added this field
    val type: String = "",       // "income" or "expense"
    val userId: String = "",
    val categoryId: String = "",
    val startTime: String? = null,
    val endTime: String? = null
)