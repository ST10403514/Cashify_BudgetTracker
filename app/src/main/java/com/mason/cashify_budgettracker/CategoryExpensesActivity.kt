package com.mason.cashify_budgettracker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.auth.FirebaseAuth
import com.mason.cashify_budgettracker.data.AppDatabase
import com.mason.cashify_budgettracker.data.Expense
import com.mason.cashify_budgettracker.databinding.ActivityCategoriesExpensesBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CategoryExpensesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCategoriesExpensesBinding
    private lateinit var auth: FirebaseAuth
    private var database: AppDatabase? = null
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private lateinit var category: String
    private var startDate: Long? = null
    private var endDate: Long? = null

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

        category = intent.getStringExtra("category") ?: ""
        if (category.isEmpty()) {
            Log.e("CategoryExpensesActivity", "No category provided in intent")
            Toast.makeText(this, "Invalid category", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        binding.tvTitle.text = "$category Expenses"

        setupRecyclerView()
        setupBottomNavigation()
        setupDateRangePicker()

        // Initialize database and load expenses
        lifecycleScope.launch {
            try {
                database = withContext(Dispatchers.IO) {
                    AppDatabase.getDatabase(this@CategoryExpensesActivity)
                }
                Log.d("CategoryExpensesActivity", "Database initialized")
                loadExpenses() // Load expenses after database is ready
            } catch (e: Exception) {
                Log.e("CategoryExpensesActivity", "Error initializing database: $e")
                Toast.makeText(this@CategoryExpensesActivity, "Error accessing database", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun setupRecyclerView() {
        binding.rvExpenses.apply {
            layoutManager = LinearLayoutManager(this@CategoryExpensesActivity)
            adapter = ExpenseAdapter(mutableListOf()) { expense ->
                try {
                    if (expense.photoPath.isNotEmpty()) {
                        val intent = Intent(this@CategoryExpensesActivity, ViewPhotoActivity::class.java)
                        intent.putExtra("photoPath", expense.photoPath)
                        startActivity(intent)
                        Log.d("CategoryExpensesActivity", "Launching ViewPhotoActivity for expense ${expense.id}")
                    }
                } catch (e: Exception) {
                    Log.e("CategoryExpensesActivity", "Error opening photo for expense ${expense.id}: $e")
                    Toast.makeText(this@CategoryExpensesActivity, "Error viewing photo", Toast.LENGTH_SHORT).show()
                }
            }
            setHasFixedSize(true)
        }
        Log.d("CategoryExpensesActivity", "RecyclerView set up")
    }

    private fun setupBottomNavigation() {
        binding.bottomNav.selectedItemId = R.id.nav_categories
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    try {
                        startActivity(Intent(this, MainActivity::class.java))
                        Log.d("CategoryExpensesActivity", "Navigating to MainActivity")
                        true
                    } catch (e: Exception) {
                        Log.e("CategoryExpensesActivity", "Error navigating to MainActivity: $e")
                        Toast.makeText(this, "Error opening Home", Toast.LENGTH_SHORT).show()
                        false
                    }
                }
                R.id.nav_categories -> {
                    try {
                        startActivity(Intent(this, CategoriesActivity::class.java))
                        Log.d("CategoryExpensesActivity", "Navigating to CategoriesActivity")
                        true
                    } catch (e: Exception) {
                        Log.e("CategoryExpensesActivity", "Error navigating to CategoriesActivity: $e")
                        Toast.makeText(this, "Error opening Categories", Toast.LENGTH_SHORT).show()
                        false
                    }
                }
                R.id.nav_goals -> {
                    try {
                        startActivity(Intent(this, GoalsActivity::class.java))
                        Log.d("CategoryExpensesActivity", "Navigating to GoalsActivity")
                        true
                    } catch (e: Exception) {
                        Log.e("CategoryExpensesActivity", "Error navigating to GoalsActivity: $e")
                        Toast.makeText(this, "Error opening Goals", Toast.LENGTH_SHORT).show()
                        false
                    }
                }
                else -> false
            }
        }
    }

    private fun setupDateRangePicker() {
        binding.chipDay.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Select Date Range")
                .build()
            datePicker.addOnPositiveButtonClickListener { selection ->
                startDate = selection.first
                endDate = selection.second
                val startDateStr = dateFormat.format(Date(startDate!!))
                val endDateStr = dateFormat.format(Date(endDate!!))
                binding.chipDay.text = "$startDateStr - $endDateStr"
                loadExpenses()
                Log.d("CategoryExpensesActivity", "Date range selected: $startDateStr to $endDateStr")
            }
            datePicker.show(supportFragmentManager, "DATE_RANGE_PICKER")
        }
    }

    private fun loadExpenses() {
        lifecycleScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            val db = database
            if (db == null) {
                Log.e("CategoryExpensesActivity", "Database not initialized in loadExpenses")
                Toast.makeText(this@CategoryExpensesActivity, "Database error", Toast.LENGTH_SHORT).show()
                return@launch
            }
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
                (binding.rvExpenses.adapter as? ExpenseAdapter)?.updateExpenses(expenses)
                Log.d("CategoryExpensesActivity", "Fetched expenses: ${expenses.map { it.id }}")
                if (expenses.isEmpty()) {
                    Toast.makeText(this@CategoryExpensesActivity, "No expenses found for $category", Toast.LENGTH_SHORT).show()
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
        database?.let {
            loadExpenses()
        } ?: Log.w("CategoryExpensesActivity", "Database not initialized in onResume")
    }
}