
package com.mason.cashify_budgettracker.data

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import android.util.Log

data class User(
    val id: String = "",
    val username: String = "",
    val email: String = ""
)

object UserRepository {
    private val db = Firebase.firestore

    suspend fun getUserByUsername(username: String): User? {
        return try {
            val snapshot = db.collection("users")
                .whereEqualTo("username", username)
                .get()
                .await()

            snapshot.documents.firstOrNull()?.toObject(User::class.java)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error fetching user", e)
            null
        }
    }

    suspend fun insert(user: User) {
        db.collection("users").document(user.id).set(user).await()
    }
}
