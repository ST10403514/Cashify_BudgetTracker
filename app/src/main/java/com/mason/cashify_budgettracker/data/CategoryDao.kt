package com.mason.cashify_budgettracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface CategoryDao {
    @Insert
    suspend fun insert(category: Category)

    @Query("SELECT * FROM categories WHERE userId = :userId")
    suspend fun getCategories(userId: String): List<Category>
}