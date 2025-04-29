package com.mason.cashify_budgettracker

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mason.cashify_budgettracker.data.Goal
import com.mason.cashify_budgettracker.databinding.ItemGoalBinding

class GoalAdapter : ListAdapter<GoalItem, GoalAdapter.GoalViewHolder>(GoalDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalViewHolder {
        val binding = ItemGoalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GoalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GoalViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class GoalViewHolder(private val binding: ItemGoalBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(goalItem: GoalItem) {
            val goal = goalItem.goal
            binding.tvCategory.text = goal.category
            binding.tvDescription.text = goal.description
            binding.tvType.text = goal.type.replaceFirstChar { char -> char.uppercase() }
            binding.tvMinMax.text = "Min: R${String.format("%.2f", goal.minGoal)} | Max: R${String.format("%.2f", goal.maxGoal)}"
            if (goal.photoPath.isNotEmpty()) {
                binding.ivPhoto.setImageURI(Uri.parse(goal.photoPath))
            } else {
                binding.ivPhoto.setImageResource(R.drawable.ic_photo_placeholder)
            }
            val progress = if (goal.maxGoal > 0) {
                ((goalItem.totalSpent / goal.maxGoal) * 100).toInt().coerceIn(0, 100)
            } else 0
            binding.progressBar.progress = progress
        }
    }

    class GoalDiffCallback : DiffUtil.ItemCallback<GoalItem>() {
        override fun areItemsTheSame(oldItem: GoalItem, newItem: GoalItem): Boolean {
            return oldItem.goal.id == newItem.goal.id
        }

        override fun areContentsTheSame(oldItem: GoalItem, newItem: GoalItem): Boolean {
            return oldItem == newItem
        }
    }
}