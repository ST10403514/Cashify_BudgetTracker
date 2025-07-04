package com.mason.cashify_budgettracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.mason.cashify_budgettracker.databinding.ItemGoalBinding
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.Color

//adapter for displaying Goal items in a RecyclerView
class GoalAdapter : ListAdapter<GoalItem, GoalAdapter.GoalViewHolder>(GoalDiffCallback()) {

    //creates a new ViewHolder by inflating the item layout
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalViewHolder {
        val binding = ItemGoalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GoalViewHolder(binding)
    }

    //binds data from GoalItem to ViewHolder at the specified position
    override fun onBindViewHolder(holder: GoalViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    //viewHolder for holding goal item views and binding data
    class GoalViewHolder(private val binding: ItemGoalBinding) : RecyclerView.ViewHolder(binding.root) {
        //Formatting date and month using SimpleDateFormat
        private val monthFormat = SimpleDateFormat("MM/yyyy", Locale.getDefault())
        private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        //binds a GoalItem to the corresponding views in the ViewHolder
        fun bind(goalItem: GoalItem) {
            val goal = goalItem.goal
            val totalSpent = goalItem.totalSpent

            //setting values for each text view in the item layout
            binding.tvCategory.text = goal.category
            binding.tvDescription.text = goal.description.takeIf { it.isNotEmpty() } ?: "No description"
            binding.tvMonth.text = "Month: ${goal.month}"
            binding.tvCreatedAt.text = "Created: ${dateFormat.format(Date(goal.createdAt))}"
            binding.tvType.text = goal.type.replaceFirstChar { it.uppercase() }

            val symbol = CurrencyConverter.getCurrencySymbol()
            val convertedMin = CurrencyConverter.convertAmount(goal.minGoal)
            val convertedMax = CurrencyConverter.convertAmount(goal.maxGoal)
            val convertedSpent = CurrencyConverter.convertAmount(totalSpent)

            binding.tvMinMax.text = "Min: $symbol${String.format("%.2f", convertedMin)} | Max: $symbol${String.format("%.2f", convertedMax)}"
            binding.tvAmountProgress.text = "Spent: $symbol${String.format("%.2f", convertedSpent)}"

            //conditionally load photo if it exists
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

            //calculating the progress based on the totalSpent and maxGoal
            val progress = when {
                goal.maxGoal == 0.0 -> 0f
                else -> {
                    val amount = totalSpent.coerceAtMost(goal.maxGoal)
                    (amount / goal.maxGoal * 100).toFloat()
                }
            }
            binding.progressBar.progress = progress.toInt()

            //setting status text and color based on goal type and progress
            when {
                goal.type == "expense" && totalSpent > goal.maxGoal -> {
                    binding.tvStatus.text = "Over Goal"
                    binding.tvStatus.setTextColor(Color.RED)
                }
                goal.type == "expense" && totalSpent >= goal.minGoal -> {
                    binding.tvStatus.text = "Within Goal"
                    binding.tvStatus.setTextColor(Color.GREEN)
                }
                goal.type == "income" && totalSpent >= goal.minGoal -> {
                    binding.tvStatus.text = "Within Goal"
                    binding.tvStatus.setTextColor(Color.GREEN)
                }
                else -> {
                    binding.tvStatus.text = "Below Goal"
                    binding.tvStatus.setTextColor(Color.RED)
                }
            }

        }
    }

    //diffUtil callback to optimize list updates by comparing old and new GoalItem objects.
    class GoalDiffCallback : DiffUtil.ItemCallback<GoalItem>() {
        //Check if two items represent the same goal item
        override fun areItemsTheSame(oldItem: GoalItem, newItem: GoalItem): Boolean {
            return oldItem.goal.id == newItem.goal.id
        }

        //check if content of two goal items are the same
        override fun areContentsTheSame(oldItem: GoalItem, newItem: GoalItem): Boolean {
            return oldItem == newItem
        }
    }
}
