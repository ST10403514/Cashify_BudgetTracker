
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
import com.mason.cashify_budgettracker.data.Goal
import com.mason.cashify_budgettracker.data.GoalRepository
import com.mason.cashify_budgettracker.databinding.ActivityGoalsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

data class GoalItem(val goal: Goal, val totalSpent: Double)

class GoalsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGoalsBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var goalAdapter: GoalAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityGoalsBinding.inflate(layoutInflater)
            setContentView(binding.root)
            Log.d("GoalsActivity", "onCreate: Binding and setContentView successful")
        } catch (e: Exception) {
            Log.e("GoalsActivity", "Error in onCreate: $e")
            Toast.makeText(this, "Error loading Goals page", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            Log.w("GoalsActivity", "No user logged in, redirecting to AuthActivity")
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        setupRecyclerView()

        lifecycleScope.launch {
            try {
                loadGoals()
                Log.d("GoalsActivity", "Goals loaded")
            } catch (e: Exception) {
                Log.e("GoalsActivity", "Error loading goals: $e")
                Toast.makeText(this@GoalsActivity, "Error accessing data", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        binding.btnAddGoal.setOnClickListener {
            Log.d("GoalsActivity", "Add New Goal clicked")
            startActivity(Intent(this, AddGoalActivity::class.java))
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    Log.d("GoalsActivity", "Navigating to MainActivity")
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_categories -> {
                    Log.d("GoalsActivity", "Navigating to CategoriesActivity")
                    startActivity(Intent(this, CategoriesActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_goals -> {
                    Log.d("GoalsActivity", "Goals tab selected")
                    true
                }
                R.id.nav_calendar -> {
                    Log.d("GoalsActivity", "Navigating to CalendarSets")
                    startActivity(Intent(this, CalendarSets::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
        binding.bottomNav.menu.findItem(R.id.nav_goals)?.isChecked = true
    }

    private fun setupRecyclerView() {
        goalAdapter = GoalAdapter()
        binding.rvGoals.apply {
            layoutManager = LinearLayoutManager(this@GoalsActivity)
            adapter = goalAdapter
        }
        Log.d("GoalsActivity", "RecyclerView set up")
    }

    private fun loadGoals() {
        lifecycleScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            try {
                val goals = withContext(Dispatchers.IO) {
                    GoalRepository.getGoals(userId)
                }
                val expenses = withContext(Dispatchers.IO) {
                    ExpenseRepository.getExpenses(userId)
                }

                val goalItems = goals.map { goal ->
                    val monthFormat = SimpleDateFormat("MM/yyyy", Locale.getDefault())
                    val goalMonth = try {
                        monthFormat.parse(goal.month)?.let {
                            Calendar.getInstance().apply {
                                time = it
                                set(Calendar.DAY_OF_MONTH, 1)
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.timeInMillis
                        } ?: 0L
                    } catch (e: Exception) {
                        Log.e("GoalsActivity", "Error parsing goal month: ${goal.month}", e)
                        0L
                    }

                    val totalSpent = expenses
                        .filter { expense ->
                            expense.category == goal.category &&
                                    expense.type == goal.type &&
                                    try {
                                        val expenseDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                            .parse(expense.date)
                                        val expenseMonth = Calendar.getInstance().apply {
                                            time = expenseDate
                                            set(Calendar.DAY_OF_MONTH, 1)
                                            set(Calendar.HOUR_OF_DAY, 0)
                                            set(Calendar.MINUTE, 0)
                                            set(Calendar.SECOND, 0)
                                            set(Calendar.MILLISECOND, 0)
                                        }.timeInMillis
                                        expenseMonth == goalMonth
                                    } catch (e: Exception) {
                                        Log.e("GoalsActivity", "Error parsing expense date: ${expense.date}", e)
                                        false
                                    }
                        }
                        .sumOf { it.amount }

                    GoalItem(goal, totalSpent)
                }

                goalAdapter.submitList(goalItems)
                Log.d("GoalsActivity", "Loaded goals: ${goals.map { it.id }}")
                if (goalItems.isEmpty()) {
                    Toast.makeText(this@GoalsActivity, "No goals found", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("GoalsActivity", "Error loading goals: $e")
                Toast.makeText(this@GoalsActivity, "Error loading goals", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("GoalsActivity", "onResume called")
        loadGoals()
    }
}