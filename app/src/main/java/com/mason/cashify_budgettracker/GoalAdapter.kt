package com.mason.cashify_budgettracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.mason.cashify_budgettracker.databinding.ItemGoalBinding
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

class GoalAdapter : ListAdapter<GoalItem, GoalAdapter.GoalViewHolder>(GoalDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalViewHolder {
        val binding = ItemGoalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GoalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GoalViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class GoalViewHolder(private val binding: ItemGoalBinding) : RecyclerView.ViewHolder(binding.root) {
        private val decimalFormat = DecimalFormat("R#,##0.00")
        private val monthFormat = SimpleDateFormat("MM/yyyy", Locale.getDefault())
        private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        fun bind(goalItem: GoalItem) {
            val goal = goalItem.goal
            val totalSpent = goalItem.totalSpent

            binding.tvCategory.text = goal.category
            binding.tvMonth.text = "Month: ${goal.month}"
            binding.tvCreatedAt.text = "Created: ${dateFormat.format(Date(goal.createdAt))}"
            binding.tvDescription.text = goal.description.takeIf { it.isNotEmpty() } ?: "No description"
            binding.tvType.text = goal.type.replaceFirstChar { it.uppercase() }
            binding.tvMinMax.text = "Min: ${decimalFormat.format(goal.minGoal)} | Max: ${decimalFormat.format(goal.maxGoal)}"

            // Load photo
            if (goal.photoPath.isNotEmpty()) {
                binding.ivPhoto.visibility = View.VISIBLE
                Glide.with(binding.ivPhoto.context)
                    .load(goal.photoPath)
                    .error(R.drawable.ic_photo_placeholder)
                    .into(binding.ivPhoto)
            } else {
                binding.ivPhoto.setImageDrawable(null)
                binding.ivPhoto.visibility = View.GONE
            }

            val progress = when {
                goal.maxGoal == 0.0 -> 0f
                else -> {
                    val amount = totalSpent.coerceAtMost(goal.maxGoal)
                    (amount / goal.maxGoal * 100).toFloat()
                }
            }
            binding.progressBar.progress = progress.toInt()
            binding.tvAmountProgress.text = "Spent: ${decimalFormat.format(totalSpent)}"

            binding.tvStatus.text = when {
                goal.type == "expense" && totalSpent > goal.maxGoal -> "Over Budget"
                goal.type == "expense" && totalSpent >= goal.minGoal -> "Within Budget"
                goal.type == "income" && totalSpent >= goal.minGoal -> "Goal Met"
                else -> "Below Goal"
            }
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
