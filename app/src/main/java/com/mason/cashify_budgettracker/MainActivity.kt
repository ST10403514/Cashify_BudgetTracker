package com.mason.cashify_budgettracker

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mason.cashify_budgettracker.databinding.ActivityMainBinding
import com.mason.cashify_budgettracker.model.Expense
import com.mason.cashify_budgettracker.adapters.ExpenseAdapter
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: ExpenseAdapter
    private val allExpenses = mutableListOf<Expense>()
    private var currentFilter: String = "all" // "all", "income", "expense", "day", "month"
    private var selectedDate: String = ""
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "ZA")).apply {
        currency = Currency.getInstance("ZAR")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase
        auth = Firebase.auth
        db = Firebase.firestore

        // Setup RecyclerView
        adapter = ExpenseAdapter(allExpenses)
        binding.rvExpenses.layoutManager = LinearLayoutManager(this)
        binding.rvExpenses.adapter = adapter

        // Setup sorting chips
        setupSortingChips()

        // Load user's expenses
        loadExpenses()

        // Set up FAB
        binding.fabAddExpense.setOnClickListener {
            startActivity(Intent(this, AddExpenseActivity::class.java))
        }

        binding.logoutButton.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
        }
    }

    private fun setupSortingChips() {
        binding.sortChipGroup.setOnCheckedChangeListener { group: ChipGroup, checkedId: Int ->
            when (checkedId) {
                R.id.chipAll -> {
                    currentFilter = "all"
                    binding.spinnerDateFilter.visibility = View.GONE
                    filterExpenses()
                }
                R.id.chipIncome -> {
                    currentFilter = "income"
                    binding.spinnerDateFilter.visibility = View.GONE
                    filterExpenses()
                }
                R.id.chipExpenses -> {
                    currentFilter = "expense"
                    binding.spinnerDateFilter.visibility = View.GONE
                    filterExpenses()
                }
                R.id.chipDay -> {
                    currentFilter = "day"
                    setupDateSpinner(true)
                    binding.spinnerDateFilter.visibility = View.VISIBLE
                }
                R.id.chipMonth -> {
                    currentFilter = "month"
                    setupDateSpinner(false)
                    binding.spinnerDateFilter.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun setupDateSpinner(isDayFilter: Boolean) {
        val dateFormat = if (isDayFilter) SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        else SimpleDateFormat("MM/yyyy", Locale.getDefault())

        val dates = allExpenses.map { it.date }.distinct()
        val formattedDates = dates.map { date ->
            val parts = date.split("/")
            if (isDayFilter) date else "${parts[1]}/${parts[2]}"
        }.distinct().sortedDescending()

        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, formattedDates)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerDateFilter.adapter = spinnerAdapter

        binding.spinnerDateFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedDate = parent.getItemAtPosition(position).toString()
                filterExpenses()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun filterExpenses() {
        val filteredList = when (currentFilter) {
            "income" -> allExpenses.filter { it.type == "income" }
            "expense" -> allExpenses.filter { it.type == "expense" }
            "day" -> allExpenses.filter { it.date == selectedDate }
            "month" -> {
                val monthYear = selectedDate.split("/")
                allExpenses.filter {
                    val parts = it.date.split("/")
                    parts[1] == monthYear[0] && parts[2] == monthYear[1]
                }
            }
            else -> allExpenses
        }.sortedByDescending {
            val parts = it.date.split("/")
            "${parts[2]}${parts[1]}${parts[0]}"
        }

        adapter.updateData(filteredList)
        updateBalance(filteredList)
    }

    private fun updateBalance(filteredList: List<Expense>) {
        val income = filteredList.filter { it.type == "income" }.sumOf { it.amount }
        val expenses = filteredList.filter { it.type == "expense" }.sumOf { it.amount }
        val balance = income - expenses
        binding.tvBalance.text = "Balance: ${currencyFormat.format(balance)}"
    }

    private fun loadExpenses() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("expenses")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                allExpenses.clear()
                allExpenses.addAll(documents.toObjects(Expense::class.java))
                filterExpenses()
                binding.welcomeText.text = "Welcome, ${auth.currentUser?.email}"
            }
    }

    override fun onResume() {
        super.onResume()
        loadExpenses()
    }
}