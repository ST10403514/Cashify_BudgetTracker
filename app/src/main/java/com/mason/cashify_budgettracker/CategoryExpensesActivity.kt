package com.mason.cashify_budgettracker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.mason.cashify_budgettracker.data.AppDatabase
import com.mason.cashify_budgettracker.databinding.ActivityCategoriesExpensesBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class CategoryExpensesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCategoriesExpensesBinding
    private lateinit var auth: FirebaseAuth
    private var database: AppDatabase? = null
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private lateinit var category: String
    private var startDate: Long? = null
    private var endDate: Long? = null
    private lateinit var expenseAdapter: ExpenseAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            binding = ActivityCategoriesExpensesBinding.inflate(layoutInflater)
            setContentView(binding.root)
        } catch (e: Exception) {
            Log.e("CategoryExpensesActivity", "Error in onCreate: $e")
            Toast.makeText(this, "Error loading Category Expenses", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        binding.btnBack.setOnClickListener {
            finish()
        }

        category = intent.getStringExtra("category")?.trim() ?: ""
        startDate = intent.getLongExtra("startDate", -1).takeIf { it != -1L }
        endDate = intent.getLongExtra("endDate", -1).takeIf { it != -1L }

        if (category.isEmpty()) {
            Toast.makeText(this, "Invalid category", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.tvTitle.text = "$category Entries"
        setupRecyclerView()
        setupBottomNavigation()

        lifecycleScope.launch {
            try {
                database = withContext(Dispatchers.IO) {
                    AppDatabase.getDatabase(this@CategoryExpensesActivity)
                }
                loadExpenses()
            } catch (e: Exception) {
                Toast.makeText(this@CategoryExpensesActivity, "Error accessing database", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun setupRecyclerView() {
        expenseAdapter = ExpenseAdapter(mutableListOf()) { expense ->
            try {
                if (expense.photoPath.isNotEmpty()) {
                    val intent = Intent(this@CategoryExpensesActivity, ViewPhotoActivity::class.java)
                    intent.putExtra("photoPath", expense.photoPath)
                    startActivity(intent)
                }
            } catch (e: Exception) {
                Toast.makeText(this@CategoryExpensesActivity, "Error viewing photo", Toast.LENGTH_SHORT).show()
            }
        }
        binding.rvExpenses.apply {
            layoutManager = LinearLayoutManager(this@CategoryExpensesActivity)
            adapter = expenseAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNav.selectedItemId = R.id.nav_categories
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                R.id.nav_categories -> {
                    startActivity(Intent(this, CategoriesActivity::class.java))
                    true
                }
                R.id.nav_goals -> {
                    startActivity(Intent(this, GoalsActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun loadExpenses() {
        lifecycleScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            val db = database ?: return@launch

            try {
                val expenses = withContext(Dispatchers.IO) {
                    if (startDate != null && endDate != null) {
                        db.expenseDao().getExpensesByCategoryAndDateRange(
                            userId,
                            category,
                            startDate!!,
                            endDate!!
                        )
                    } else {
                        db.expenseDao().getExpensesByCategory(userId, category)
                    }
                }.toMutableList()

                expenseAdapter.updateExpenses(expenses)

                if (expenses.isEmpty()) {
                    Toast.makeText(this@CategoryExpensesActivity, "No expenses found in selected range", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@CategoryExpensesActivity, "Error loading expenses", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        database?.let { loadExpenses() }
    }
}
