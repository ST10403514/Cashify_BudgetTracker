
package com.mason.cashify_budgettracker.data

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import android.util.Log

//represents user in the system
data class User(
    val id: String = "",
    val username: String = "",
    val email: String = ""
)

//handles Firestore operations for user data
object UserRepository {
    private val db = Firebase.firestore

    //retrieve user by their username
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

    //insert new user into Firestore
    suspend fun insert(user: User) {
        db.collection("users").document(user.id).set(user).await()
    }
}
