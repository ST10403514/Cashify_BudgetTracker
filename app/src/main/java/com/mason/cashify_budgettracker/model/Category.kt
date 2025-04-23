package com.mason.cashify_budgettracker.model

data class Category(
    val id: String = "",
    val name: String = "",
    val userId: String = "", // For user-specific categories
)