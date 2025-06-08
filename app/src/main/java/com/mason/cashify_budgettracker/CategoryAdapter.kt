package com.mason.cashify_budgettracker

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mason.cashify_budgettracker.data.CategoryTotal
import com.mason.cashify_budgettracker.databinding.ItemCategoryBinding

//adapter class for displaying a list of categories and their total amounts
class CategoryAdapter(
    private val onCategoryClick: (CategoryTotal) -> Unit
) : ListAdapter<CategoryTotal, CategoryAdapter.CategoryViewHolder>(CategoryDiffCallback()) {

    //viewholder to hold views for each item in list
    inner class CategoryViewHolder(
        private val binding: ItemCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        //bind category data to the UI components
        fun bind(categoryTotal: CategoryTotal) {
            binding.tvCategoryName.text = categoryTotal.category

            val symbol = CurrencyConverter.getCurrencySymbol()
            val convertedTotal = CurrencyConverter.convertAmount(categoryTotal.total)

            val amountText = if (convertedTotal < 0) {
                //negative: show minus sign explicitly
                String.format("-%s%.2f", symbol, -convertedTotal)
            } else {
                //positive
                String.format("%s%.2f", symbol, convertedTotal)
            }

            binding.tvCategoryTotal.text = amountText

            //set text color: green for positive, red for negative
            val color = if (convertedTotal < 0) {
                android.graphics.Color.RED
            } else {
                android.graphics.Color.GREEN
            }
            binding.tvCategoryTotal.setTextColor(color)

            //click listener
            binding.root.setOnClickListener {
                onCategoryClick(categoryTotal)
            }
        }

    }

    //creates new ViewHolder by inflating item layout
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        //inflate view for the item
        val binding = ItemCategoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CategoryViewHolder(binding)
    }

    //binds category data to the ViewHolder at the given position
    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val categoryTotal = getItem(position)
        holder.bind(categoryTotal)
    }

    //diffCallback to efficiently handle item changes in the list
    class CategoryDiffCallback : DiffUtil.ItemCallback<CategoryTotal>() {

        //check if items are the same based on their category name
        override fun areItemsTheSame(oldItem: CategoryTotal, newItem: CategoryTotal): Boolean {
            return oldItem.category == newItem.category
        }

        //check if content of items is the same
        override fun areContentsTheSame(oldItem: CategoryTotal, newItem: CategoryTotal): Boolean {
            return oldItem == newItem
        }
    }
}
