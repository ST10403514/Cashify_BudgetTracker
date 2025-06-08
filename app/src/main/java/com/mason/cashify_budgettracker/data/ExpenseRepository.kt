
package com.mason.cashify_budgettracker.data

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext


//handles expense-related Firestore operations
object ExpenseRepository {
    private val db = Firebase.firestore

    //insert a new expense
    suspend fun insert(expense: Expense) = withContext(Dispatchers.IO) {
        db.collection("users").document(expense.userId)
            .collection("expenses").add(expense).await()
    }

    //get all expenses for a user
    suspend fun getExpenses(userId: String): List<Expense> = withContext(Dispatchers.IO) {
        db.collection("users").document(userId).collection("expenses")
            .get().await()
            .documents.mapNotNull { doc ->
                doc.toObject(Expense::class.java)?.copy(id = doc.id)
            }
    }

    //get expenses by type
    suspend fun getExpensesByType(userId: String, type: String): List<Expense> = withContext(Dispatchers.IO) {
        db.collection("users").document(userId).collection("expenses")
            .whereEqualTo("type", type)
            .get().await()
            .documents.mapNotNull { doc ->
                doc.toObject(Expense::class.java)?.copy(id = doc.id)
            }
    }

    //get expenses within a date range
    suspend fun getExpensesByDateRange(userId: String, startDate: Long, endDate: Long): List<Expense> = withContext(Dispatchers.IO) {
        db.collection("users").document(userId).collection("expenses")
            .whereGreaterThanOrEqualTo("timestamp", startDate)
            .whereLessThanOrEqualTo("timestamp", endDate)
            .get().await()
            .documents.mapNotNull { doc ->
                doc.toObject(Expense::class.java)?.copy(id = doc.id)
            }
    }

    //get expenses by category
    suspend fun getExpensesByCategory(userId: String, category: String): List<Expense> = withContext(Dispatchers.IO) {
        db.collection("users").document(userId).collection("expenses")
            .whereEqualTo("category", category)
            .get().await()
            .documents.mapNotNull { doc ->
                doc.toObject(Expense::class.java)?.copy(id = doc.id)
            }
    }

    //get expenses by category and date range
    suspend fun getExpensesByCategoryAndDateRange(userId: String, category: String, startDate: Long, endDate: Long): List<Expense> = withContext(Dispatchers.IO) {
        db.collection("users").document(userId).collection("expenses")
            .whereEqualTo("category", category)
            .whereGreaterThanOrEqualTo("timestamp", startDate)
            .whereLessThanOrEqualTo("timestamp", endDate)
            .get().await()
            .documents.mapNotNull { doc ->
                doc.toObject(Expense::class.java)?.copy(id = doc.id)
            }
    }

    //get total amounts grouped by category
    suspend fun getCategoryTotals(userId: String): List<CategoryTotal> = withContext(Dispatchers.IO) {
        val expenses = getExpenses(userId)
        expenses.groupBy { it.category }.map { (category, exps) ->
            val total = exps.sumOf { exp ->
                if (exp.type == "expense") -exp.amount else exp.amount
            }
            CategoryTotal(category, total)
        }
    }

    //get total amounts by category within a date range
    suspend fun getCategoryTotalsByDateRange(userId: String, startDate: Long, endDate: Long): List<CategoryTotal> = withContext(Dispatchers.IO) {
        val expenses = getExpensesByDateRange(userId, startDate, endDate)
        expenses.groupBy { it.category }.map { (category, exps) ->
            val total = exps.sumOf { exp ->
                if (exp.type == "expense") -exp.amount else exp.amount
            }
            CategoryTotal(category, total)
        }
    }

    //get distinct category names used by the user
    suspend fun getDistinctCategories(userId: String): List<String> = withContext(Dispatchers.IO) {
        getExpenses(userId).map { it.category }.distinct()
    }
}