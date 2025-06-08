package com.mason.cashify_budgettracker

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.CombinedData
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.android.material.datepicker.MaterialDatePicker
import com.mason.cashify_budgettracker.data.ExpenseRepository
import com.mason.cashify_budgettracker.data.Goal
import com.mason.cashify_budgettracker.data.GoalRepository
import com.mason.cashify_budgettracker.databinding.ActivityReportsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class ReportsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportsBinding
    private lateinit var auth: FirebaseAuth
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val monthFormat = SimpleDateFormat("MM/yyyy", Locale.getDefault())
    private var lastNavClickTime: Long = 0
    private val navDebounceDelay: Long = 500

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //set up view binding and load layout
        try {
            binding = ActivityReportsBinding.inflate(layoutInflater)
            setContentView(binding.root)
            Log.d("ReportsActivity", "onCreate: Binding successful")
        } catch (e: Exception) {
            Log.e("ReportsActivity", "Error in onCreate: $e")
            Toast.makeText(this, "Error loading Reports page", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        //initialise authentication
        auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            Log.w("ReportsActivity", "No user logged in")
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        //set up bottom navigation
        binding.bottomNav.setOnItemSelectedListener { item ->
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastNavClickTime < navDebounceDelay) {
                Log.d("ReportsActivity", "Navigation click debounced: ${item.itemId}")
                return@setOnItemSelectedListener false
            }
            lastNavClickTime = currentTime
            when (item.itemId) {
                R.id.nav_home -> {
                    Log.d("ReportsActivity", "Navigating to MainActivity")
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                R.id.nav_categories -> {
                    Log.d("ReportsActivity", "Navigating to CategoriesActivity")
                    startActivity(Intent(this, CategoriesActivity::class.java))
                    true
                }
                R.id.nav_goals -> {
                    Log.d("ReportsActivity", "Navigating to GoalsActivity")
                    startActivity(Intent(this, GoalsActivity::class.java))
                    true
                }
                R.id.nav_reports -> {
                    Log.d("ReportsActivity", "Reports tab selected")
                    true
                }
                R.id.nav_calendar -> {
                    Log.d("ReportsActivity", "Navigating to CalendarSets")
                    startActivity(Intent(this, CalendarSets::class.java))
                    true
                }
                else -> {
                    Log.d("ReportsActivity", "Unknown navigation item: ${item.itemId}")
                    false
                }
            }
        }
        binding.bottomNav.menu.findItem(R.id.nav_reports)?.isChecked = true

        setupPeriodChips()
    }

    //sets up chip filters for selecting weekly, monthly, or custom report periods.
    private fun setupPeriodChips() {
        val chipGroup = binding.chipGroupPeriod
        val customChip = binding.chipCustom
        val originalCustomText = customChip.text.toString()

        chipGroup.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.chipWeek -> {
                    loadDataForPeriod("week")
                    customChip.text = originalCustomText
                }
                R.id.chipMonth -> {
                    loadDataForPeriod("month")
                    customChip.text = originalCustomText
                }
                R.id.chipCustom -> {
                    showDateRangePicker { startDate, endDate ->
                        // Format date range text
                        val formattedRange = "${dateFormat.format(startDate.time)} - ${dateFormat.format(endDate.time)}"
                        customChip.text = formattedRange
                        loadDataForCustomRange(startDate, endDate)
                    }
                }
                else -> { /* No chip selected or cleared, do nothing */ }
            }
        }

        //set default chip checked
        chipGroup.check(R.id.chipWeek)
        loadDataForPeriod("week")
    }

    //displays a date range picker and returns the selected start and end dates
    private fun showDateRangePicker(onDateRangeSelected: (Calendar, Calendar) -> Unit) {
        val builder = MaterialDatePicker.Builder.dateRangePicker()
        builder.setTitleText("Select Date Range")
        val picker = builder.build()

        picker.show(supportFragmentManager, picker.toString())

        picker.addOnPositiveButtonClickListener { selection ->
            val startDateMillis = selection.first
            val endDateMillis = selection.second

            val startDate = Calendar.getInstance().apply { timeInMillis = startDateMillis ?: 0L }
            val endDate = Calendar.getInstance().apply { timeInMillis = endDateMillis ?: 0L }

            onDateRangeSelected(startDate, endDate)
        }
    }

    //loads transactions for the selected predefined time period
    private fun loadDataForPeriod(period: String) {
        val calendar = Calendar.getInstance()
        val endCalendar = Calendar.getInstance()
        when (period) {
            "week" -> calendar.add(Calendar.WEEK_OF_YEAR, -1)
            "month" -> calendar.add(Calendar.MONTH, -1)
        }
        val startCalendar = calendar
        loadDataForCustomRange(startCalendar, endCalendar)
    }

    //loads and processes transaction and goal data for a custom date range
    private fun loadDataForCustomRange(startCalendar: Calendar, endCalendar: Calendar) {
        lifecycleScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                val startDate = startCalendar.time
                val endDate = endCalendar.time
                val targetMonth = monthFormat.format(endDate)

                //load all transactions
                val transactions = withContext(Dispatchers.IO) {
                    ExpenseRepository.getExpenses(userId).filter {
                        try {
                            val transactionDate = dateFormat.parse(it.date)
                            transactionDate in startDate..endDate
                        } catch (e: Exception) {
                            Log.e("ReportsActivity", "Error parsing transaction date: ${it.date}", e)
                            false
                        }
                    }
                }

                //separate expenses and income
                val expenses = transactions.filter { it.type == "expense" }
                val income = transactions.filter { it.type == "income" }

                //show toast if no transactions
                if (expenses.isEmpty() && income.isEmpty()) {
                    Toast.makeText(this@ReportsActivity, "There are no transactions for this time period", Toast.LENGTH_SHORT).show()
                }

                //load goals for the target month
                val goals = withContext(Dispatchers.IO) {
                    GoalRepository.getGoals(userId).filter { it.month == targetMonth }
                }

                //group expenses and income by category
                val categorySpends = expenses.groupBy { it.category }
                    .mapValues { entry ->
                        entry.value.sumOf { CurrencyConverter.convertAmount(it.amount) }
                    }
                val categoryIncome = income.groupBy { it.category }
                    .mapValues { entry ->
                        entry.value.sumOf { CurrencyConverter.convertAmount(it.amount) }
                    }

                //combine categories from expenses and income
                val labels = (categorySpends.keys + categoryIncome.keys).distinct().sorted()

                //prepare expense and income chart data
                val expenseEntries = labels.mapIndexed { index, category ->
                    BarEntry(index.toFloat() - 0.2f, categorySpends[category]?.toFloat() ?: 0f)
                }
                val incomeEntries = labels.mapIndexed { index, category ->
                    BarEntry(index.toFloat() + 0.2f, categoryIncome[category]?.toFloat() ?: 0f)
                }

                //get min/max goals for charts (expense categories only)
                val minGoals = mutableMapOf<String, Float>()
                val maxGoals = mutableMapOf<String, Float>()
                goals.forEach { goal ->
                    if (goal.category in labels) {
                        minGoals[goal.category] = CurrencyConverter.convertAmount(goal.minGoal).toFloat()
                        maxGoals[goal.category] = CurrencyConverter.convertAmount(goal.maxGoal).toFloat()
                    }
                }

                updateSpendingChart(expenseEntries, incomeEntries, labels, minGoals, maxGoals)
                updateGoalsChart(labels, minGoals, maxGoals)
            } catch (e: Exception) {
                Toast.makeText(this@ReportsActivity, "Error loading data: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("ReportsActivity", "Error loading data", e)
            }
        }
    }

    //configures and displays bar and line charts for income, expenses, and goals
    private fun updateSpendingChart(
        expenseEntries: List<BarEntry>,
        incomeEntries: List<BarEntry>,
        labels: List<String>,
        minGoals: Map<String, Float>,
        maxGoals: Map<String, Float>
    ) {
        val currencySymbol = CurrencyConverter.getCurrencySymbol()

        //income bars
        val incomeDataSet = BarDataSet(incomeEntries, "Income").apply {
            color = android.graphics.Color.GREEN
            valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return String.format("%s%.2f", currencySymbol, value)
                }
            }
        }

        //expense bars
        val expenseDataSet = BarDataSet(expenseEntries, "Expenses").apply {
            color = android.graphics.Color.RED
            valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return String.format("%s%.2f", currencySymbol, value)
                }
            }
        }

        //combine bar data
        val barData = BarData(incomeDataSet, expenseDataSet).apply {
            barWidth = 0.2f
            groupBars(-0.5f, 0.4f, 0.1f) // Start at -0.5, group width 0.4, gap 0.1
        }

        //min goal lines
        val minLineEntries = labels.mapIndexed { index, category ->
            minGoals[category]?.takeIf { it > 0 }?.let {
                listOf(
                    Entry(index.toFloat() - 0.3f, it),
                    Entry(index.toFloat() + 0.3f, it)
                )
            } ?: emptyList()
        }.flatten()
        val minLineDataSet = LineDataSet(minLineEntries, "Min Goal").apply {
            color = ContextCompat.getColor(this@ReportsActivity, R.color.purple_500)
            lineWidth = 2f
            setDrawCircles(false)
            setDrawValues(false)
        }

        //max goal lines
        val maxLineEntries = labels.mapIndexed { index, category ->
            maxGoals[category]?.takeIf { it > 0 }?.let {
                listOf(
                    Entry(index.toFloat() - 0.3f, it),
                    Entry(index.toFloat() + 0.3f, it)
                )
            } ?: emptyList()
        }.flatten()
        val maxLineDataSet = LineDataSet(maxLineEntries, "Max Goal").apply {
            color = ContextCompat.getColor(this@ReportsActivity, R.color.purple_800)
            lineWidth = 2f
            setDrawCircles(false)
            setDrawValues(false)
        }

        //combine data
        val combinedData = CombinedData().apply {
            setData(barData)
            setData(LineData(minLineDataSet, maxLineDataSet))
        }

        binding.spendingChart.apply {
            data = combinedData
            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(labels)
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                labelRotationAngle = 0f
            }
            axisLeft.apply {
                axisMinimum = 0f
                valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return String.format("%s%.2f", currencySymbol, value)
                    }
                }
                removeAllLimitLines()
            }
            axisRight.isEnabled = false
            description.isEnabled = false
            setExtraOffsets(20f, 20f, 20f, 20f)
            legend.isEnabled = true
            legend.form = com.github.mikephil.charting.components.Legend.LegendForm.SQUARE
            legend.textSize = 12f
            legend.horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.LEFT
            legend.verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.TOP
            legend.xEntrySpace = 30f
            legend.xOffset = 10f
            animateY(1000)
            invalidate()
        }
    }
    //displays a bar chart showing target vs. saved amounts for each goal
    private fun updateGoalsChart(labels: List<String>, minGoals: Map<String, Float>, maxGoals: Map<String, Float>) {
        val currencySymbol = CurrencyConverter.getCurrencySymbol()
        //min goal bars
        val minEntries = labels.mapIndexed { index, category ->
            BarEntry(index.toFloat() - 0.2f, minGoals[category] ?: 0f)
        }
        val minDataSet = BarDataSet(minEntries, "Min Goals").apply {
            color = ContextCompat.getColor(this@ReportsActivity, R.color.purple_500)

            valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return String.format("%s%.2f", currencySymbol, value)
                }
            }
        }
        //max goal bars
        val maxEntries = labels.mapIndexed { index, category ->
            BarEntry(index.toFloat() + 0.2f, maxGoals[category] ?: 0f)
        }
        val maxDataSet = BarDataSet(maxEntries, "Max Goals").apply {
            color = ContextCompat.getColor(this@ReportsActivity, R.color.purple_800)
            valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return String.format("%s%.2f", currencySymbol, value)
                }
            }
        }
        val barData = BarData(minDataSet, maxDataSet).apply {
            barWidth = 0.2f
            groupBars(-0.5f, 0.4f, 0.1f)
        }

        binding.goalsChart.apply {
            data = barData
            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(labels)
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                labelRotationAngle = 0f
            }
            axisLeft.apply {
                axisMinimum = 0f
                valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return String.format("%s%.2f", currencySymbol, value)
                    }
                }
                removeAllLimitLines()
            }
            axisRight.isEnabled = false
            description.isEnabled = false
            setExtraOffsets(20f, 20f, 20f, 20f)
            legend.isEnabled = true
            legend.form = com.github.mikephil.charting.components.Legend.LegendForm.SQUARE
            legend.textSize = 12f
            legend.horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.LEFT
            legend.verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.TOP
            legend.xEntrySpace = 30f
            legend.xOffset = 10f
            animateY(1000)
            invalidate()
        }
    }
}
