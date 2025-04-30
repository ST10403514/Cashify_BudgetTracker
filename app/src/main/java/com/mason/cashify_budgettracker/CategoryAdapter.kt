package com.mason.cashify_budgettracker

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mason.cashify_budgettracker.data.CategoryTotal
import com.mason.cashify_budgettracker.databinding.ItemCategoryBinding

class CategoryAdapter(
    private val onCategoryClick: (CategoryTotal) -> Unit
) : ListAdapter<CategoryTotal, CategoryAdapter.CategoryViewHolder>(CategoryDiffCallback()) {

    inner class CategoryViewHolder(
        private val binding: ItemCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(categoryTotal: CategoryTotal) {
            binding.tvCategoryName.text = categoryTotal.category
            binding.tvCategoryTotal.text = String.format("R%.2f", categoryTotal.total)
            binding.root.setOnClickListener {
                onCategoryClick(categoryTotal)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val categoryTotal = getItem(position)
        holder.bind(categoryTotal)
    }

    class CategoryDiffCallback : DiffUtil.ItemCallback<CategoryTotal>() {
        override fun areItemsTheSame(oldItem: CategoryTotal, newItem: CategoryTotal): Boolean {
            return oldItem.category == newItem.category
        }

        override fun areContentsTheSame(oldItem: CategoryTotal, newItem: CategoryTotal): Boolean {
            return oldItem == newItem
        }
    }
}