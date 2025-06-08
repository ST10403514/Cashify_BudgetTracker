package com.mason.cashify_budgettracker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.auth.FirebaseAuth
import com.mason.cashify_budgettracker.data.ExpenseRepository
import com.mason.cashify_budgettracker.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var expenseAdapter: ExpenseAdapter
    private var currentFilter: String = "all"
    private var startDate: Long? = null
    private var endDate: Long? = null
    private var lastNavClickTime: Long = 0
    private val navDebounceDelay: Long = 500
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    private val currencies = listOf("ZAR", "USD", "EUR", "GBP", "JPY")

    private val apiKey = "cur_live_jjcHI3oPbo0SD8f2qe3Fn2C4h4GAabDHc0kAOGDt"
    private val baseUrl = "https://api.currencyapi.com/v3/latest"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //inflate layout and set content view
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

        //initialize Firebase Authentication
        auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            Log.w("MainActivity", "No user logged in, redirecting to AuthActivity")
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        val username = auth.currentUser?.email?.substringBefore("@") ?: "User"
        binding.tvWelcome.text = "Welcome $username!"
        Log.d("MainActivity", "Username set: $username")

        //initialize expense adapter with click listener for photos
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

        //setup filter chips listener for expense type and date filtering
        binding.chipGroup.setOnCheckedChangeListener { group, checkedId ->
            when (group.findViewById<Chip>(checkedId)?.id) {
                R.id.chipAll -> {
                    currentFilter = "all"
                    startDate = null
                    endDate = null
                    binding.chipDay.text = "Pick Date"
                    Log.d("MainActivity", "Filter changed to: $currentFilter")
                    loadExpenses()
                }
                R.id.chipExpenses -> {
                    currentFilter = "expense"
                    startDate = null
                    endDate = null
                    binding.chipDay.text = "Pick Date"
                    Log.d("MainActivity", "Filter changed to: $currentFilter")
                    loadExpenses()
                }
                R.id.chipIncome -> {
                    currentFilter = "income"
                    startDate = null
                    endDate = null
                    binding.chipDay.text = "Pick Date"
                    Log.d("MainActivity", "Filter changed to: $currentFilter")
                    loadExpenses()
                }
                R.id.chipDay -> {
                    currentFilter = "day"
                    showDateRangePicker()
                }
                else -> {
                    currentFilter = "all"
                    startDate = null
                    endDate = null
                    binding.chipDay.text = "Pick Date"
                    Log.d("MainActivity", "Filter changed to: $currentFilter")
                    loadExpenses()
                }
            }
        }

        binding.logoutButton.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            Log.d("MainActivity", "Logout clicked")
        }

        binding.btnAddExpense.setOnClickListener {
            Log.d("MainActivity", "Navigating to AddExpenseActivity via btnAddExpense")
            startActivity(Intent(this, AddExpenseActivity::class.java))
        }

        //set up bottom nav
        binding.bottomNav.setOnItemSelectedListener { item ->
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastNavClickTime < navDebounceDelay) {
                Log.d("MainActivity", "Navigation click debounced: ${item.itemId}")
                return@setOnItemSelectedListener false
            }
            lastNavClickTime = currentTime
            when (item.itemId) {
                R.id.nav_home -> {
                    Log.d("MainActivity", "Home tab selected")
                    true
                }
                R.id.nav_categories -> {
                    Log.d("MainActivity", "Navigating to CategoriesActivity")
                    startActivity(Intent(this, CategoriesActivity::class.java))
                    true
                }
                R.id.nav_goals -> {
                    Log.d("MainActivity", "Navigating to GoalsActivity")
                    startActivity(Intent(this, GoalsActivity::class.java))
                    true
                }
                R.id.nav_reports -> {
                    Log.d("MainActivity", "Navigating to ReportsActivity")
                    startActivity(Intent(this, ReportsActivity::class.java))
                    true
                }
                R.id.nav_calendar -> {
                    Log.d("MainActivity", "Navigating to CalendarSets")
                    startActivity(Intent(this, CalendarSets::class.java))
                    true
                }
                else -> {
                    Log.d("MainActivity", "Unknown navigation item: ${item.itemId}")
                    false
                }
            }
        }
        binding.bottomNav.menu.findItem(R.id.nav_home)?.isChecked = true

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, currencies)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.currencySpinner.adapter = adapter

        binding.currencySpinner.setSelection(currencies.indexOf(CurrencyConverter.getSelectedCurrency()))

        binding.currencySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val newCurrency = currencies[position]
                if (newCurrency != CurrencyConverter.getSelectedCurrency()) {
                    CurrencyConverter.setSelectedCurrency(newCurrency)
                    fetchExchangeRatesAndUpdate()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    //shows date range picker for filtering expenses by dat
    private fun showDateRangePicker() {
        val datePicker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Select Date Range")
            .build()
        datePicker.addOnPositiveButtonClickListener { selection ->
            startDate = selection.first
            endDate = selection.second
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = endDate!!
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            endDate = calendar.timeInMillis
            val startDateStr = dateFormat.format(Date(startDate!!))
            val endDateStr = dateFormat.format(Date(endDate!!))
            binding.chipDay.text = "$startDateStr - $endDateStr"
            Log.d("MainActivity", "Date range selected: $startDateStr to $endDateStr")
            loadExpenses()
        }
        datePicker.show(supportFragmentManager, "DATE_RANGE_PICKER")
    }

    //loads expenses from database, applies filters and currency conversion, updates UI
    private fun loadExpenses() {
        lifecycleScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                val expenses = withContext(Dispatchers.IO) {
                    when (currentFilter) {
                        "expense" -> ExpenseRepository.getExpensesByType(userId, "expense")
                        "income" -> ExpenseRepository.getExpensesByType(userId, "income")
                        "day" -> {
                            if (startDate != null && endDate != null) {
                                ExpenseRepository.getExpensesByDateRange(userId, startDate!!, endDate!!)
                            } else {
                                emptyList()
                            }
                        }
                        else -> ExpenseRepository.getExpenses(userId)
                    }
                }

                //apply currency conversion if exchange rate available, else use original amounts
                val convertedExpenses = expenses.map { expense ->
                    expense.copy(amount = CurrencyConverter.convertAmount(expense.amount))
                }

                expenseAdapter.updateExpenses(convertedExpenses)

                if (convertedExpenses.isEmpty()) {
                    Toast.makeText(this@MainActivity, "No expenses found", Toast.LENGTH_SHORT).show()
                }

                Log.d("MainActivity", "Fetched expenses (converted): ${convertedExpenses.map { it.id }}")

                //calculate balance in selected currency
                val balance = convertedExpenses.sumOf { expense ->
                    if (expense.type == "income") expense.amount else -expense.amount
                }

                //format the balance with currency symbol
                val currencySymbol = CurrencyConverter.getCurrencySymbol()
                val selectedCurrency = CurrencyConverter.getSelectedCurrency()
                binding.tvBalance.text = "Balance: $currencySymbol${DecimalFormat("0.00").format(balance)}"
                Log.d("MainActivity", "Balance updated: $balance $selectedCurrency")
            } catch (e: Exception) {
                Log.e("MainActivity", "Error loading expenses: $e")
                Toast.makeText(this@MainActivity, "Error loading expenses", Toast.LENGTH_SHORT).show()
            }
        }
    }

    //fetches currency exchange rates from API and updates conversion rates
    private fun fetchExchangeRatesAndUpdate() {
        lifecycleScope.launch {
            try {
                val urlString = "$baseUrl?apikey=$apiKey&base_currency=ZAR"
                val rates = withContext(Dispatchers.IO) {
                    val url = URL(urlString)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000

                    if (connection.responseCode == 200) {
                        val stream = connection.inputStream.bufferedReader().use { it.readText() }
                        parseRatesFromJson(stream)
                    } else {
                        null
                    }
                }

                if (rates != null) {
                    CurrencyConverter.setExchangeRates(rates)
                    loadExpenses()
                } else {
                    Toast.makeText(this@MainActivity, "Failed to get exchange rates", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error fetching exchange rates", Toast.LENGTH_SHORT).show()
            }
        }
    }


    //parses JSON response from currency API to get exchange rates
    private fun parseRatesFromJson(jsonString: String): Map<String, Double> {
        val jsonObject = JSONObject(jsonString)
        val data = jsonObject.getJSONObject("data")
        val ratesMap = mutableMapOf<String, Double>()
        currencies.forEach { currency ->
            if (currency != "ZAR") {
                val currencyObj = data.optJSONObject(currency)
                if (currencyObj != null) {
                    ratesMap[currency] = currencyObj.getDouble("value")
                }
            } else {
                ratesMap[currency] = 1.0
            }
        }
        return ratesMap
    }

    override fun onResume() {
        super.onResume()
        Log.d("MainActivity", "onResume called")
        loadExpenses()
    }
}
