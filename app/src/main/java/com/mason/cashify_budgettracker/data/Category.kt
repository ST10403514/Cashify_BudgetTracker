package com.mason.cashify_budgettracker.data

import com.google.firebase.firestore.PropertyName

data class Category(
    @PropertyName("id") val id: String = "",
    @PropertyName("userId") val userId: String = "",
    @PropertyName("name") val name: String = ""
)