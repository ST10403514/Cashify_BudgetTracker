package com.mason.cashify_budgettracker

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.mason.cashify_budgettracker.data.AppDatabase
import com.mason.cashify_budgettracker.data.Expense
import com.mason.cashify_budgettracker.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: AppDatabase
    private lateinit var expenseAdapter: ExpenseAdapter
    private var isDatabaseInitialized: Boolean = false
    private var currentFilter: String = "all"
    private var selectedDate: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)
            Log.d("MainActivity", "onCreate: Binding and setContentView successful")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in onCreate: $e")
            Toast.makeText(this, "Error loading Main page", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            Log.w("MainActivity", "No user logged in, redirecting to AuthActivity")
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        // Set username
        val username = auth.currentUser?.email?.substringBefore("@") ?: "User"
        binding.tvWelcome.text = "Welcome @$username"
        Log.d("MainActivity", "Username set: $username")

        // Initialize RecyclerView
        expenseAdapter = ExpenseAdapter(mutableListOf()) { expense ->
            if (expense.photoPath.isNotEmpty()) {
                val intent = Intent(this, ViewPhotoActivity::class.java).apply {
                    putExtra("photoPath", expense.photoPath)
                }
                startActivity(intent)
                Log.d("MainActivity", "Launching ViewPhotoActivity for expense ${expense.id}")
            }
        }
        binding.rvExpenses.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = expenseAdapter
        }

        // Initialize database
        lifecycleScope.launch {
            try {
                database = withContext(Dispatchers.IO) {
                    AppDatabase.getDatabase(this@MainActivity)
                }
                isDatabaseInitialized = true
                Log.d("MainActivity", "Database initialized")
                loadExpenses()
                Log.d("MainActivity", "Expenses loaded after database initialization")
            } catch (e: Exception) {
                Log.e("MainActivity", "Error initializing database: $e")
                Toast.makeText(this@MainActivity, "Error accessing database", Toast.LENGTH_SHORT).show()
            }
        }

        // Setup chip filters
        binding.chipGroup.setOnCheckedChangeListener { group, checkedId ->
            when (group.findViewById<Chip>(checkedId)?.id) {
                R.id.chipAll -> {
                    currentFilter = "all"
                    selectedDate = null
                    Log.d("MainActivity", "Filter changed to: $currentFilter")
                    loadExpenses()
                }
                R.id.chipExpenses -> {
                    currentFilter = "expense"
                    selectedDate = null
                    Log.d("MainActivity", "Filter changed to: $currentFilter")
                    loadExpenses()
                }
                R.id.chipIncome -> {
                    currentFilter = "income"
                    selectedDate = null
                    Log.d("MainActivity", "Filter changed to: $currentFilter")
                    loadExpenses()
                }
                R.id.chipDay -> {
                    currentFilter = "day"
                    showDatePicker()
                }
                else -> {
                    currentFilter = "all"
                    selectedDate = null
                    Log.d("MainActivity", "Filter changed to: $currentFilter")
                    loadExpenses()
                }
            }
        }

        // Setup logout button
        binding.logoutButton.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            Log.d("MainActivity", "Logout clicked")
        }

        // Setup bottom navigation
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    Log.d("MainActivity", "Home tab selected")
                    true
                }
                R.id.nav_add -> {
                    Log.d("MainActivity", "Navigating to AddExpenseActivity")
                    startActivity(Intent(this, AddExpenseActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_goals -> {
                    Log.d("MainActivity", "Navigating to GoalsActivity")
                    startActivity(Intent(this, GoalsActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
        binding.bottomNav.menu.findItem(R.id.nav_home)?.isChecked = true
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val formattedDate = String.format(
                    Locale.getDefault(),
                    "%02d/%02d/%04d",
                    selectedDay,
                    selectedMonth + 1,
                    selectedYear
                )
                selectedDate = formattedDate
                Log.d("MainActivity", "Date selected: $selectedDate")
                loadExpenses()
            },
            year,
            month,
            day
        )
        datePickerDialog.show()
    }

    private fun loadExpenses() {
        if (!isDatabaseInitialized) {
            Log.w("MainActivity", "Database not initialized, skipping expense load")
            Toast.makeText(this, "Database not ready, please wait", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                val expenses = withContext(Dispatchers.IO) {
                    when (currentFilter) {
                        "expense" -> database.expenseDao().getExpensesByType(userId, "expense")
                        "income" -> database.expenseDao().getExpensesByType(userId, "income")
                        "day" -> {
                            if (selectedDate != null) {
                                database.expenseDao().getExpensesByDate(userId, selectedDate!!)
                            } else {
                                emptyList()
                            }
                        }
                        else -> database.expenseDao().getAllExpenses(userId)
                    }
                }
                expenseAdapter.updateExpenses(expenses)
                Log.d("MainActivity", "Fetched expenses: $expenses")

                // Calculate balance
                val balance = expenses.sumOf { expense ->
                    if (expense.type == "income") expense.amount else -expense.amount
                }
                binding.tvBalance.text = "Balance: R${DecimalFormat("0.00").format(balance)}"
                Log.d("MainActivity", "Balance updated: $balance")
            } catch (e: Exception) {
                Log.e("MainActivity", "Error loading expenses: $e")
                Toast.makeText(this@MainActivity, "Error loading expenses", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("MainActivity", "onResume called")
        loadExpenses()
    }
}