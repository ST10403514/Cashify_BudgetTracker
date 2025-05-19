
package com.mason.cashify_budgettracker.data

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class User(
    val id: String = "",
    val username: String = "",
    val email: String = ""
)

object UserRepository {
    private val db = Firebase.firestore

    suspend fun insert(user: User) = withContext(Dispatchers.IO) {
        db.collection("users").document(user.id).set(user).await()
    }

    suspend fun getUserByUsername(username: String): User? = withContext(Dispatchers.IO) {
        db.collection("users")
            .whereEqualTo("username", username)
            .get().await()
            .documents
            .firstOrNull()
            ?.toObject(User::class.java)
    }

    suspend fun getUserById(userId: String): User? = withContext(Dispatchers.IO) {
        db.collection("users").document(userId)
            .get().await()
            .toObject(User::class.java)
    }
}