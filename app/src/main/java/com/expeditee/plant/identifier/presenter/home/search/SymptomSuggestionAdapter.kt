package com.expeditee.plant.identifier.presenter.home.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.expeditee.plant.identifier.databinding.ItemSearchHistoryBinding
import com.expeditee.plant.identifier.thesis.SymptomIntent

class SymptomSuggestionAdapter :
    ListAdapter<SymptomIntent, SymptomSuggestionAdapter.SymptomViewHolder>(DiffCallback()) {

    var onSymptomClick: (SymptomIntent) -> Unit = {}

    inner class SymptomViewHolder(private val binding: ItemSearchHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SymptomIntent) {
            binding.tvHistoryText.text = item.title
            binding.ivDelete.visibility = View.GONE
            binding.root.setOnClickListener { onSymptomClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SymptomViewHolder {
        return SymptomViewHolder(
            ItemSearchHistoryBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            ),
        )
    }

    override fun onBindViewHolder(holder: SymptomViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private class DiffCallback : DiffUtil.ItemCallback<SymptomIntent>() {
        override fun areItemsTheSame(oldItem: SymptomIntent, newItem: SymptomIntent): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: SymptomIntent, newItem: SymptomIntent): Boolean =
            oldItem == newItem
    }
}
