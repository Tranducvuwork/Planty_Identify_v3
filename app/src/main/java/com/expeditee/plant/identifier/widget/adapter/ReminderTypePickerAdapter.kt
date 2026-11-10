package com.expeditee.plant.identifier.widget.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.databinding.ItemReminderTypePickerBinding

@Keep
data class ReminderTypePickerItem(
    val label: String,
    @DrawableRes val iconRes: Int,
)

class ReminderTypePickerAdapter(
    private val onItemClick: (Int) -> Unit,
) : androidx.recyclerview.widget.ListAdapter<ReminderTypePickerItem, ReminderTypePickerAdapter.VH>(
    Diff
) {

    private var selectedPosition: Int = 0

    fun setSelectedPosition(position: Int) {
        if (position == selectedPosition) return
        val old = selectedPosition
        selectedPosition = position
        notifyItemChanged(old)
        notifyItemChanged(selectedPosition)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemReminderTypePickerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position), position == selectedPosition)
    }

    inner class VH(
        private val binding: ItemReminderTypePickerBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    setSelectedPosition(pos)
                    onItemClick(pos)
                }
            }
        }

        fun bind(item: ReminderTypePickerItem, selected: Boolean) {
            binding.ivTypeIcon.setImageResource(item.iconRes)
            binding.tvTypeLabel.text = item.label

            val ctx = binding.root.context
            val labelColor = if (selected) {
                ContextCompat.getColor(ctx, R.color.reminder_green)
            } else {
                ContextCompat.getColor(ctx, R.color.reminder_text_secondary)
            }
            binding.tvTypeLabel.setTextColor(labelColor)

            val alpha = if (selected) 1f else 0.45f
            binding.ivTypeIcon.alpha = alpha
            binding.tvTypeLabel.alpha = if (selected) 1f else 0.75f

            binding.root.background = ContextCompat.getDrawable(
                ctx,
                if (selected) R.drawable.bg_fertilizing_row_selected else R.drawable.bg_fertilizing_row_unselected,
            )
        }
    }

    private object Diff : DiffUtil.ItemCallback<ReminderTypePickerItem>() {
        override fun areItemsTheSame(
            oldItem: ReminderTypePickerItem,
            newItem: ReminderTypePickerItem,
        ): Boolean = oldItem.label == newItem.label

        override fun areContentsTheSame(
            oldItem: ReminderTypePickerItem,
            newItem: ReminderTypePickerItem,
        ): Boolean = oldItem == newItem
    }
}
