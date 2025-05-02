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

    //onCreate method initializes the activity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Inflate layout and set content view
        try {
            binding = ActivityCategoriesExpensesBinding.inflate(layoutInflater)
            setContentView(binding.root)
        } catch (e: Exception) {
            Log.e("CategoryExpensesActivity", "Error in onCreate: $e")
            Toast.makeText(this, "Error loading Category Expenses", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        //Initialize FirebaseAuth instance
        auth = FirebaseAuth.getInstance()

        //Check if the user is authenticated; if not, navigate to authentication screen
        if (auth.currentUser == null) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        //Set back button functionality to finish the activity
        binding.btnBack.setOnClickListener {
            finish()
        }

        //Retrieve category and date range passed from the previous activity
        category = intent.getStringExtra("category")?.trim() ?: ""
        startDate = intent.getLongExtra("startDate", -1).takeIf { it != -1L }
        endDate = intent.getLongExtra("endDate", -1).takeIf { it != -1L }

        //Handle invalid category scenario
        if (category.isEmpty()) {
            Toast.makeText(this, "Invalid category", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        //Set title for the activity based on the selected category
        binding.tvTitle.text = "$category Entries"

        //Set up RecyclerView and Bottom Navigation
        setupRecyclerView()
        setupBottomNavigation()

        //Launch a coroutine to initialize database and load expenses
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

    //Set up RecyclerView to display expense data
    private fun setupRecyclerView() {
        expenseAdapter = ExpenseAdapter(mutableListOf()) { expense ->
            try {
                //If expense has a photo, allow the user to view it
                if (expense.photoPath.isNotEmpty()) {
                    val intent = Intent(this@CategoryExpensesActivity, ViewPhotoActivity::class.java)
                    intent.putExtra("photoPath", expense.photoPath)
                    startActivity(intent)
                }
            } catch (e: Exception) {
                Toast.makeText(this@CategoryExpensesActivity, "Error viewing photo", Toast.LENGTH_SHORT).show()
            }
        }

        //Initialize RecyclerView with LinearLayoutManager and adapter
        binding.rvExpenses.apply {
            layoutManager = LinearLayoutManager(this@CategoryExpensesActivity)
            adapter = expenseAdapter
            setHasFixedSize(true)
        }
    }

    //Set up Bottom Navigation to allow navigation between different sections
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

    //Load expenses from database based on category and date range
    private fun loadExpenses() {
        lifecycleScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            val db = database ?: return@launch

            try {
                // Query the database for expenses within the specified category and date range
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

                // Update the RecyclerView adapter with the loaded expenses
                expenseAdapter.updateExpenses(expenses)

                // If no expenses are found, show a toast message
                if (expenses.isEmpty()) {
                    Toast.makeText(this@CategoryExpensesActivity, "No expenses found in selected range", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@CategoryExpensesActivity, "Error loading expenses", Toast.LENGTH_SHORT).show()
            }
        }
    }

    //Reload expenses when activity is resumed
    override fun onResume() {
        super.onResume()
        database?.let { loadExpenses() }
    }
}
