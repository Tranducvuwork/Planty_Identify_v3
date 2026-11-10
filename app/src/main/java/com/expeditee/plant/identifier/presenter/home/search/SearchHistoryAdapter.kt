package com.expeditee.plant.identifier.presenter.home.search


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.expeditee.plant.identifier.databinding.ItemSearchHistoryBinding
import com.expeditee.plant.identifier.model.SearchHistoryModel

class SearchHistoryAdapter : ListAdapter<SearchHistoryModel, SearchHistoryAdapter.HistoryViewHolder>(DiffCallback()) {

    var onHistoryClick: (String) -> Unit = {}
    var onHistoryDelete: (SearchHistoryModel) -> Unit = {}

    inner class HistoryViewHolder(private val binding: ItemSearchHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SearchHistoryModel) {
            binding.apply {
                tvHistoryText.text = item.query
                ivDelete.visibility = View.VISIBLE
                root.setOnClickListener {
                    onHistoryClick(item.query)
                }

                ivDelete.setOnClickListener {
                    onHistoryDelete(item)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        return HistoryViewHolder(
            ItemSearchHistoryBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<SearchHistoryModel>() {
        override fun areItemsTheSame(
            oldItem: SearchHistoryModel,
            newItem: SearchHistoryModel
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: SearchHistoryModel,
            newItem: SearchHistoryModel
        ): Boolean = oldItem == newItem
    }
}
