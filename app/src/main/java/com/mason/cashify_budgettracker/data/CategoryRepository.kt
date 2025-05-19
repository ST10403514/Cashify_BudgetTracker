
package com.mason.cashify_budgettracker.data

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object CategoryRepository {
    private val db = Firebase.firestore

    suspend fun insert(category: Category) = withContext(Dispatchers.IO) {
        db.collection("users").document(category.userId)
            .collection("categories").add(category).await()
    }

    suspend fun getCategories(userId: String): List<Category> = withContext(Dispatchers.IO) {
        db.collection("users").document(userId).collection("categories")
            .get().await()
            .documents.mapNotNull { doc ->
                doc.toObject(Category::class.java)?.copy(id = doc.id)
            }
    }
}