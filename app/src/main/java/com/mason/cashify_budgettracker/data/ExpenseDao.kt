package com.mason.cashify_budgettracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.OnConflictStrategy

@Dao
interface ExpenseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: Expense)

    @Query("SELECT * FROM expenses WHERE userId = :userId")
    suspend fun getExpenses(userId: String): List<Expense>

    @Query("SELECT * FROM expenses WHERE userId = :userId AND type = :type")
    suspend fun getExpensesByType(userId: String, type: String): List<Expense>

    @Query("SELECT * FROM expenses WHERE userId = :userId AND date = :date")
    suspend fun getExpensesByDate(userId: String, date: String): List<Expense>

    @Query("SELECT category, SUM(amount) as total FROM expenses WHERE userId = :userId GROUP BY category")
    suspend fun getCategoryTotals(userId: String): List<CategoryTotal>

    @Query("SELECT * FROM expenses WHERE userId = :userId AND category = :category")
    suspend fun getExpensesByCategory(userId: String, category: String): List<Expense>

    @Query("SELECT * FROM expenses WHERE userId = :userId AND category = :category AND date = :date")
    suspend fun getExpensesByCategoryAndDate(userId: String, category: String, date: String): List<Expense>

    @Query("SELECT * FROM expenses WHERE userId = :userId AND category = :category AND strftime('%s', date, 'unixepoch') * 1000 BETWEEN :startDate AND :endDate")
    suspend fun getExpensesByCategoryAndDateRange(userId: String, category: String, startDate: Long, endDate: Long): List<Expense>
}