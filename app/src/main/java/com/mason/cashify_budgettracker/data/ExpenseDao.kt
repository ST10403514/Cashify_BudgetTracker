package com.mason.cashify_budgettracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ExpenseDao {
    @Insert
    suspend fun insert(expense: Expense)

    @Query("SELECT * FROM expenses WHERE userId = :userId")
    suspend fun getExpenses(userId: String): List<Expense>

    @Query("SELECT * FROM expenses WHERE userId = :userId AND type = :type")
    suspend fun getExpensesByType(userId: String, type: String): List<Expense>

    @Query("SELECT * FROM expenses WHERE userId = :userId AND date = :date")
    suspend fun getExpensesByDate(userId: String, date: String): List<Expense>

    @Query("SELECT * FROM expenses WHERE userId = :userId")
    suspend fun getAllExpenses(userId: String): List<Expense>


}