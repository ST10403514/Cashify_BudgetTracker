package com.mason.cashify_budgettracker.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mason.cashify_budgettracker.model.Category
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class CategoryRepository {

    private val db: FirebaseFirestore = Firebase.firestore
    private val categoriesCollection = db.collection("categories")

    fun getUserCategories(userId: String): Flow<List<Category>> = callbackFlow {
        val listener = categoriesCollection
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error) // Close the flow with the error
                    return@addSnapshotListener
                }
                val categories = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Category::class.java)
                } ?: emptyList()
                trySend(categories).isSuccess // Send the data to the Flow
            }

        awaitClose { listener.remove() } // Clean up the listener when the Flow is closed
    }

    suspend fun addCategory(category: Category): String {
        categoriesCollection.document(category.id).set(category).await()
        return category.id
    }

    suspend fun deleteCategory(categoryId: String) {
        categoriesCollection.document(categoryId).delete().await()
    }

    companion object {
        fun createCategory(name: String, userId: String): Category {
            return Category(
                id = UUID.randomUUID().toString(),
                name = name,
                userId = userId
            )
        }
    }
}