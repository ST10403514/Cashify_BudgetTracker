package com.mason.cashify_budgettracker

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mason.cashify_budgettracker.data.CategoryTotal
import com.mason.cashify_budgettracker.databinding.ItemCategoryBinding

//Adapter class for displaying a list of categories and their total amounts
class CategoryAdapter(
    private val onCategoryClick: (CategoryTotal) -> Unit
) : ListAdapter<CategoryTotal, CategoryAdapter.CategoryViewHolder>(CategoryDiffCallback()) {

    //ViewHolder to hold views for each item in list
    inner class CategoryViewHolder(
        private val binding: ItemCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        //Bind category data to the UI components
        fun bind(categoryTotal: CategoryTotal) {
            binding.tvCategoryName.text = categoryTotal.category

            val symbol = CurrencyConverter.getCurrencySymbol()
            val convertedTotal = CurrencyConverter.convertAmount(categoryTotal.total)
            binding.tvCategoryTotal.text = String.format("%s%.2f", symbol, convertedTotal)

            //Set click listener for item
            binding.root.setOnClickListener {
                onCategoryClick(categoryTotal)
            }
        }
    }

    //Creates a new ViewHolder by inflating item layout
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        //Inflate view for the item
        val binding = ItemCategoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CategoryViewHolder(binding)
    }

    //Binds category data to the ViewHolder at the given position
    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val categoryTotal = getItem(position)
        holder.bind(categoryTotal)
    }

    //DiffCallback to efficiently handle item changes in the list
    class CategoryDiffCallback : DiffUtil.ItemCallback<CategoryTotal>() {

        // Check if items are the same based on their category name
        override fun areItemsTheSame(oldItem: CategoryTotal, newItem: CategoryTotal): Boolean {
            return oldItem.category == newItem.category
        }

        //Check if content of items is the same
        override fun areContentsTheSame(oldItem: CategoryTotal, newItem: CategoryTotal): Boolean {
            return oldItem == newItem
        }
    }
}
