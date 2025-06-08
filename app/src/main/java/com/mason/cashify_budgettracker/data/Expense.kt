
package com.mason.cashify_budgettracker.data

import com.google.firebase.firestore.PropertyName

//expense or income entry in firestore
data class Expense(
    @PropertyName("id") val id: String = "",
    @PropertyName("userId") val userId: String = "",
    @PropertyName("amount") val amount: Double = 0.0,
    @PropertyName("category") val category: String = "",
    @PropertyName("categoryId") val categoryId: String = "",
    @PropertyName("type") val type: String = "",
    @PropertyName("date") val date: String = "",
    @PropertyName("timestamp") val timestamp: Long = 0L,
    @PropertyName("startTime") val startTime: String = "",
    @PropertyName("endTime") val endTime: String = "",
    @PropertyName("description") val description: String = "",
    @PropertyName("photoPath") val photoPath: String = ""
)
