
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
import com.mason.cashify_budgettracker.data.CategoryTotal
import com.mason.cashify_budgettracker.data.ExpenseRepository
import com.mason.cashify_budgettracker.databinding.ActivityCategoriesBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class CategoriesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCategoriesBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var categoryAdapter: CategoryAdapter
    private var selectedStartDate: Long? = null
    private var selectedEndDate: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityCategoriesBinding.inflate(layoutInflater)
            setContentView(binding.root)
            Log.d("CategoriesActivity", "onCreate: Binding and setContentView successful")
        } catch (e: Exception) {
            Log.e("CategoriesActivity", "Error in onCreate: $e")
            Toast.makeText(this, "Error loading Categories", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            Log.w("CategoriesActivity", "No user logged in, redirecting to AuthActivity")
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        setupRecyclerView()
        setupBottomNavigation()
        setupChipGroup()

        lifecycleScope.launch {
            try {
                loadCategories()
                Log.d("CategoriesActivity", "Categories loaded")
            } catch (e: Exception) {
                Log.e("CategoriesActivity", "Error loading categories: $e")
                Toast.makeText(this@CategoriesActivity, "Error accessing data", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun setupRecyclerView() {
        categoryAdapter = CategoryAdapter { categoryTotal ->
            try {
                val intent = Intent(this@CategoriesActivity, CategoryExpensesActivity::class.java)
                intent.putExtra("category", categoryTotal.category)
                selectedStartDate?.let { intent.putExtra("startDate", it) }
                selectedEndDate?.let { intent.putExtra("endDate", it) }
                startActivity(intent)
                Log.d("CategoriesActivity", "Navigating to CategoryExpensesActivity for ${categoryTotal.category} with dates: $selectedStartDate - $selectedEndDate")
            } catch (e: Exception) {
                Log.e("CategoriesActivity", "Error navigating to CategoryExpensesActivity: $e")
                Toast.makeText(this@CategoriesActivity, "Error opening category", Toast.LENGTH_SHORT).show()
            }
        }

        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(this@CategoriesActivity)
            adapter = categoryAdapter
            setHasFixedSize(true)
        }
        Log.d("CategoriesActivity", "RecyclerView set up")
    }

    private fun setupBottomNavigation() {
        binding.bottomNav.selectedItemId = R.id.nav_categories
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    try {
                        startActivity(Intent(this, MainActivity::class.java))
                        Log.d("CategoriesActivity", "Navigating to MainActivity")
                        true
                    } catch (e: Exception) {
                        Log.e("CategoriesActivity", "Error navigating to MainActivity: $e")
                        Toast.makeText(this, "Error opening Home", Toast.LENGTH_SHORT).show()
                        false
                    }
                }
                R.id.nav_categories -> true
                R.id.nav_goals -> {
                    try {
                        startActivity(Intent(this, GoalsActivity::class.java))
                        Log.d("CategoriesActivity", "Navigating to GoalsActivity")
                        true
                    } catch (e: Exception) {
                        Log.e("CategoriesActivity", "Error navigating to GoalsActivity: $e")
                        Toast.makeText(this, "Error opening Goals", Toast.LENGTH_SHORT).show()
                        false
                    }
                }
                else -> false
            }
        }
    }

    private fun setupChipGroup() {
        binding.chipGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.chipAll -> {
                    Log.d("CategoriesActivity", "All chip selected")
                    selectedStartDate = null
                    selectedEndDate = null
                    binding.chipDay.text = "Pick Date"
                    loadCategories()
                }
                R.id.chipDay -> {
                    Log.d("CategoriesActivity", "Pick Date chip selected")
                    showDateRangePicker()
                }
            }
        }
    }

    private fun showDateRangePicker() {
        val dateRangePicker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Select Date Range")
            .build()

        dateRangePicker.addOnPositiveButtonClickListener { dateRange ->
            try {
                selectedStartDate = dateRange.first
                selectedEndDate = dateRange.second + TimeUnit.DAYS.toMillis(1) - 1
                val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                val startDateStr = dateFormat.format(java.util.Date(selectedStartDate!!))
                val endDateStr = dateFormat.format(java.util.Date(selectedEndDate!!))
                binding.chipDay.text = "$startDateStr - $endDateStr"
                Log.d("CategoriesActivity", "Date range selected: $startDateStr to $endDateStr")
                loadCategories()
                Toast.makeText(this, "Date range applied", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("CategoriesActivity", "Error selecting date range: $e")
                Toast.makeText(this, "Error applying date range", Toast.LENGTH_SHORT).show()
            }
        }

        dateRangePicker.addOnNegativeButtonClickListener {
            Log.d("CategoriesActivity", "Date range picker cancelled")
            binding.chipGroup.check(R.id.chipAll)
        }

        dateRangePicker.show(supportFragmentManager, "DATE_RANGE_PICKER")
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            try {
                val categoryTotals = withContext(Dispatchers.IO) {
                    if (selectedStartDate != null && selectedEndDate != null) {
                        ExpenseRepository.getCategoryTotalsByDateRange(userId, selectedStartDate!!, selectedEndDate!!)
                    } else {
                        ExpenseRepository.getCategoryTotals(userId)
                    }
                }
                categoryAdapter.submitList(categoryTotals)
                binding.tvTitle.text = if (selectedStartDate != null) {
                    "Categories"
                } else {
                    "Categories"
                }
                Log.d("CategoriesActivity", "Categories loaded: ${categoryTotals.map { it.category to it.total }}")
                if (categoryTotals.isEmpty()) {
                    Toast.makeText(this@CategoriesActivity, "No categories found", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("CategoriesActivity", "Error loading categories: $e")
                Toast.makeText(this@CategoriesActivity, "Error loading categories", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("CategoriesActivity", "onResume called")
        loadCategories()
    }
}