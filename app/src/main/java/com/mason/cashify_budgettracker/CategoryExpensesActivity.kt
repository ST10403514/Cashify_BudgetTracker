package com.mason.cashify_budgettracker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.mason.cashify_budgettracker.data.ExpenseRepository
import com.mason.cashify_budgettracker.databinding.ActivityCategoriesExpensesBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class CategoryExpensesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCategoriesExpensesBinding
    private lateinit var auth: FirebaseAuth
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
            Log.d("CategoryExpensesActivity", "onCreate: Binding and setContentView successful")
        } catch (e: Exception) {
            Log.e("CategoryExpensesActivity", "Error in onCreate: $e")
            Toast.makeText(this, "Error loading Category Expenses", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            Log.w("CategoryExpensesActivity", "No user logged in, redirecting to AuthActivity")
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        binding.btnBack.setOnClickListener {
            Log.d("CategoryExpensesActivity", "Back button clicked")
            finish()
        }

        category = intent.getStringExtra("category")?.trim() ?: ""
        startDate = intent.getLongExtra("startDate", -1).takeIf { it != -1L }
        endDate = intent.getLongExtra("endDate", -1).takeIf { it != -1L }

        if (category.isEmpty()) {
            Toast.makeText(this, "Invalid category", Toast.LENGTH_SHORT).show()
            Log.w("CategoryExpensesActivity", "Invalid category")
            finish()
            return
        }

        binding.tvTitle.text = "$category Expenses"

        setupRecyclerView()
        setupBottomNavigation()

        lifecycleScope.launch {
            try {
                loadExpenses()
                Log.d("CategoryExpensesActivity", "Expenses loaded for category: $category")
            } catch (e: Exception) {
                Log.e("CategoryExpensesActivity", "Error loading expenses: $e")
                Toast.makeText(this@CategoryExpensesActivity, "Error accessing data", Toast.LENGTH_SHORT).show()
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
                    Log.d("CategoryExpensesActivity", "Navigating to ViewPhotoActivity for expense ${expense.id}")
                }
            } catch (e: Exception) {
                Log.e("CategoryExpensesActivity", "Error viewing photo: $e")
                Toast.makeText(this@CategoryExpensesActivity, "Error viewing photo", Toast.LENGTH_SHORT).show()
            }
        }

        binding.rvExpenses.apply {
            layoutManager = LinearLayoutManager(this@CategoryExpensesActivity)
            adapter = expenseAdapter
            setHasFixedSize(true)
        }
        Log.d("CategoryExpensesActivity", "RecyclerView set up")
    }

    private fun setupBottomNavigation() {
        binding.bottomNav.selectedItemId = R.id.nav_categories
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    Log.d("CategoryExpensesActivity", "Navigating to MainActivity")
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                R.id.nav_categories -> {
                    Log.d("CategoryExpensesActivity", "Categories tab selected")
                    startActivity(Intent(this, CategoriesActivity::class.java))
                    true
                }
                R.id.nav_goals -> {
                    Log.d("CategoryExpensesActivity", "Navigating to GoalsActivity")
                    startActivity(Intent(this, GoalsActivity::class.java))
                    true
                }
                R.id.nav_reports -> {
                    Log.d("CategoryExpensesActivity", "Navigating to ReportsActivity")
                    startActivity(Intent(this, ReportsActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun loadExpenses() {
        lifecycleScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            try {
                val expenses = withContext(Dispatchers.IO) {
                    // Fetch all expenses for the category
                    val allExpenses = ExpenseRepository.getExpensesByCategory(userId, category)
                    if (startDate != null && endDate != null) {
                        // Filter by date range in code
                        val startDateStr = dateFormat.format(Date(startDate!!))
                        val endDateStr = dateFormat.format(Date(endDate!!))
                        Log.d("CategoryExpensesActivity", "Filtering by date range: $startDateStr to $endDateStr")
                        allExpenses.filter { expense ->
                            try {
                                val expenseDate = dateFormat.parse(expense.date) ?: return@filter false
                                val start = dateFormat.parse(startDateStr) ?: return@filter false
                                val end = dateFormat.parse(endDateStr) ?: return@filter false
                                expenseDate in start..end
                            } catch (e: Exception) {
                                Log.e("CategoryExpensesActivity", "Error parsing date ${expense.date}: $e")
                                false
                            }
                        }
                    } else {
                        allExpenses
                    }
                }.toMutableList()

                expenseAdapter.updateExpenses(expenses)
                Log.d("CategoryExpensesActivity", "Loaded ${expenses.size} expenses: ${expenses.map { it.id }}")

                if (expenses.isEmpty()) {
                    Toast.makeText(this@CategoryExpensesActivity, "No expenses found in selected range", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("CategoryExpensesActivity", "Error loading expenses: $e")
                Toast.makeText(this@CategoryExpensesActivity, "Error loading expenses", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("CategoryExpensesActivity", "onResume called")
        loadExpenses()
    }
}