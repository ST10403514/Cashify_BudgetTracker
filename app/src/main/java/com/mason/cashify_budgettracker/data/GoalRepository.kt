
package com.mason.cashify_budgettracker.data

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object GoalRepository {
    private val db = Firebase.firestore

    suspend fun insert(goal: Goal) = withContext(Dispatchers.IO) {
        db.collection("users").document(goal.userId)
            .collection("goals").add(goal).await()
    }

    suspend fun getGoals(userId: String): List<Goal> = withContext(Dispatchers.IO) {
        db.collection("users").document(userId).collection("goals")
            .get().await()
            .documents.mapNotNull { doc ->
                doc.toObject(Goal::class.java)?.copy(id = doc.id)
            }
    }
}
