
package com.mason.cashify_budgettracker.data

import com.google.firebase.firestore.PropertyName

//goal entry in firestore
data class Goal(
    @PropertyName("id") val id: String = "",
    @PropertyName("userId") val userId: String = "",
    @PropertyName("month") val month: String = "",
    @PropertyName("category") val category: String = "",
    @PropertyName("categoryId") val categoryId: String = "",
    @PropertyName("type") val type: String = "",
    @PropertyName("description") val description: String = "",
    @PropertyName("photoPath") val photoPath: String = "",
    @PropertyName("minGoal") val minGoal: Double = 0.0,
    @PropertyName("maxGoal") val maxGoal: Double = 0.0,
    @PropertyName("createdAt") val createdAt: Long = 0L
)