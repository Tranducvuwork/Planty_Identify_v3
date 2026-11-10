package com.expeditee.plant.identifier.presenter.dialog.rate_app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.expeditee.plant.identifier.databinding.ItemFeedbackOptionBinding

class FeedbackAdapter(
    private val onItemClick: (FeedbackItem) -> Unit
) : RecyclerView.Adapter<FeedbackAdapter.FeedbackViewHolder>() {

    private val items = mutableListOf<FeedbackItem>()

    fun updateItems(newItems: List<FeedbackItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun updateItemSelection(itemId: String, isSelected: Boolean) {
        val index = items.indexOfFirst { it.id == itemId }
        if (index != -1) {
            items[index] = items[index].copy(isSelected = isSelected)
            notifyItemChanged(index)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedbackViewHolder {
        val binding = ItemFeedbackOptionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FeedbackViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FeedbackViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class FeedbackViewHolder(
        private val binding: ItemFeedbackOptionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FeedbackItem) {
            binding.apply {
                ivEmoji.setImageResource(item.iconRes)
                tvText.text = item.text
                root.isSelected = item.isSelected
                
                root.setOnClickListener {
                    onItemClick(item)
                }
            }
        }
    }
}

