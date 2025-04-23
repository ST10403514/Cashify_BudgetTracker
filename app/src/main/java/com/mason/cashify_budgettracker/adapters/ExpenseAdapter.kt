package com.mason.cashify_budgettracker.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mason.cashify_budgettracker.databinding.ItemExpenseBinding
import com.mason.cashify_budgettracker.model.Expense
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

class ExpenseAdapter(
    private var expenses: List<Expense>,
    private val onItemClick: (Expense) -> Unit = {}
) : RecyclerView.Adapter<ExpenseAdapter.ViewHolder>() {

    // Customize NumberFormat for South African Rand
    private val currencyFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale("en", "ZA")).apply {
        currency = Currency.getInstance("ZAR") // Ensure currency is ZAR
    }

    class ViewHolder(val binding: ItemExpenseBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemExpenseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val expense = expenses[position]
        holder.binding.apply {
            // Set text values
            tvDescription.text = expense.description
            tvDate.text = expense.date
            tvCategory.text = expense.category
            tvAmount.text = currencyFormat.format(expense.amount)

            // Color code based on type
            val color = when (expense.type) {
                "income" -> Color.parseColor("#4CAF50") // Material Green 500
                else -> Color.parseColor("#F44336")      // Material Red 500
            }
            tvAmount.setTextColor(color)

            // Optional click handling
            root.setOnClickListener { onItemClick(expense) }
        }
    }

    override fun getItemCount() = expenses.size

    fun updateData(newExpenses: List<Expense>) {
        expenses = newExpenses
        notifyDataSetChanged()
    }
}