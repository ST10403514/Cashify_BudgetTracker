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

        auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            Log.w("ReportsActivity", "No user logged in")
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        // Setup bottom navigation
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
                R.id.nav_calendar -> {
                    Log.d("ReportsActivity", "Navigating to CalendarSets")
                    startActivity(Intent(this, CalendarSets::class.java))
                    true
                }
                R.id.nav_reports -> {
                    Log.d("ReportsActivity", "Reports tab selected")
                    true
                }
                else -> {
                    Log.d("ReportsActivity", "Unknown navigation item: ${item.itemId}")
                    false
                }
            }
        }
        binding.bottomNav.menu.findItem(R.id.nav_reports)?.isChecked = true

        setupPeriodSpinner()
    }

    private fun setupPeriodSpinner() {
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.period_options,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPeriod.adapter = adapter

        binding.spinnerPeriod.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                when (position) {
                    0 -> loadDataForPeriod("week")
                    1 -> loadDataForPeriod("month")
                    2 -> showDateRangePicker()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun showDateRangePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, day ->
            val startDate = Calendar.getInstance().apply {
                set(year, month, day)
            }
            DatePickerDialog(this, { _, endYear, endMonth, endDay ->
                val endDate = Calendar.getInstance().apply {
                    set(endYear, endMonth, endDay)
                }
                loadDataForCustomRange(startDate, endDate)
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

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

    private fun loadDataForCustomRange(startCalendar: Calendar, endCalendar: Calendar) {
        lifecycleScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                val startDate = startCalendar.time
                val endDate = endCalendar.time
                val targetMonth = monthFormat.format(endDate)

                // Load expenses
                val expenses = withContext(Dispatchers.IO) {
                    ExpenseRepository.getExpenses(userId).filter {
                        try {
                            val expenseDate = dateFormat.parse(it.date)
                            expenseDate in startDate..endDate
                        } catch (e: Exception) {
                            Log.e("ReportsActivity", "Error parsing expense date: ${it.date}", e)
                            false
                        }
                    }
                }

                // Load goals for the target month
                val goals = withContext(Dispatchers.IO) {
                    GoalRepository.getGoals(userId).filter { it.month == targetMonth }
                }

                // Group expenses by category (expenses only) with currency conversion
                val categorySpends = expenses
                    .filter { it.type == "expense" }
                    .groupBy { it.category }
                    .mapValues { entry ->
                        entry.value.sumOf { CurrencyConverter.convertAmount(it.amount) }
                    }

                // Prepare chart data
                val entries = categorySpends.entries.mapIndexed { index, entry ->
                    BarEntry(index.toFloat(), entry.value.toFloat())
                }
                val labels = categorySpends.keys.toList()

                // Get min/max goals for charts
                val minGoals = mutableMapOf<String, Float>()
                val maxGoals = mutableMapOf<String, Float>()
                goals.forEach { goal ->
                    if (goal.category in labels) {
                        minGoals[goal.category] = CurrencyConverter.convertAmount(goal.minGoal).toFloat()
                        maxGoals[goal.category] = CurrencyConverter.convertAmount(goal.maxGoal).toFloat()
                    }
                }

                updateSpendingChart(entries, labels, minGoals, maxGoals)
                updateGoalsChart(labels, minGoals, maxGoals)
            } catch (e: Exception) {
                Toast.makeText(this@ReportsActivity, "Error loading data: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("ReportsActivity", "Error loading data", e)
            }
        }
    }

    private fun updateSpendingChart(entries: List<BarEntry>, labels: List<String>, minGoals: Map<String, Float>, maxGoals: Map<String, Float>) {
        val currencySymbol = CurrencyConverter.getCurrencySymbol()

        // Spending bars
        val barDataSet = BarDataSet(entries, "Spending").apply {
            color = android.graphics.Color.BLUE
            valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return String.format("%s%.2f", currencySymbol, value)
                }
            }
        }
        val barData = BarData(barDataSet).apply {
            barWidth = 0.3f
        }

        // Min goal lines
        val minLineEntries = labels.mapIndexed { index, category ->
            minGoals[category]?.takeIf { it > 0 }?.let {
                listOf(
                    Entry(index.toFloat() - 0.1f, it),
                    Entry(index.toFloat() + 0.1f, it)
                )
            } ?: emptyList()
        }.flatten()
        val minLineDataSet = LineDataSet(minLineEntries, "Min Goal").apply {
            color = android.graphics.Color.GREEN
            lineWidth = 2f
            setDrawCircles(false)
            setDrawValues(false)
        }

        // Max goal lines
        val maxLineEntries = labels.mapIndexed { index, category ->
            maxGoals[category]?.takeIf { it > 0 }?.let {
                listOf(
                    Entry(index.toFloat() - 0.1f, it),
                    Entry(index.toFloat() + 0.1f, it)
                )
            } ?: emptyList()
        }.flatten()
        val maxLineDataSet = LineDataSet(maxLineEntries, "Max Goal").apply {
            color = android.graphics.Color.RED
            lineWidth = 2f
            setDrawCircles(false)
            setDrawValues(false)
        }

        // Combine data
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
                labelRotationAngle = 45f
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
            // Enable and customize legend
            legend.isEnabled = true
            legend.form = com.github.mikephil.charting.components.Legend.LegendForm.LINE
            legend.textSize = 12f
            legend.horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER
            legend.verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.TOP
            // Increased padding
            legend.xEntrySpace = 30f // Increased spacing between entries
            animateY(1000)
            invalidate()
        }
    }

    private fun updateGoalsChart(labels: List<String>, minGoals: Map<String, Float>, maxGoals: Map<String, Float>) {
        val currencySymbol = CurrencyConverter.getCurrencySymbol()
        // Min goal bars
        val minEntries = labels.mapIndexed { index, category ->
            BarEntry(index.toFloat() - 0.2f, minGoals[category] ?: 0f)
        }
        val minDataSet = BarDataSet(minEntries, "Min Goals").apply {
            color = android.graphics.Color.GREEN
            valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return String.format("%s%.2f", currencySymbol, value)
                }
            }
        }
        // Max goal bars
        val maxEntries = labels.mapIndexed { index, category ->
            BarEntry(index.toFloat() + 0.2f, maxGoals[category] ?: 0f)
        }
        val maxDataSet = BarDataSet(maxEntries, "Max Goals").apply {
            color = android.graphics.Color.RED
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
                labelRotationAngle = 45f
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
            // Enable and customize legend
            legend.isEnabled = true
            legend.form = com.github.mikephil.charting.components.Legend.LegendForm.SQUARE
            legend.textSize = 12f
            legend.horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER
            legend.verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.TOP
            // Increased padding
            legend.xEntrySpace = 30f // Increased spacing between entries
            animateY(1000)
            invalidate()
        }
    }
}
