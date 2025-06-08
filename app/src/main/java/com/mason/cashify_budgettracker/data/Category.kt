package com.mason.cashify_budgettracker.data

import com.google.firebase.firestore.PropertyName

//category stored in Firestore
data class Category(
    @PropertyName("id") val id: String = "",
    @PropertyName("userId") val userId: String = "",
    @PropertyName("name") val name: String = ""
)