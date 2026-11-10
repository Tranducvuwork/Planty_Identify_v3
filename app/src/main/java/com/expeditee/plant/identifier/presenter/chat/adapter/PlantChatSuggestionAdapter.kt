package com.expeditee.plant.identifier.presenter.chat.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.expeditee.plant.identifier.databinding.ItemPlantChatSuggestionBinding
import com.expeditee.plant.identifier.model.PlantChatSuggestion

class PlantChatSuggestionAdapter(
    private val onSuggestionClick: (PlantChatSuggestion) -> Unit
) : RecyclerView.Adapter<PlantChatSuggestionAdapter.PlantSuggestionViewHolder>() {

    private var suggestions: List<PlantChatSuggestion> = emptyList()

    fun updateSuggestionsWithDiff(newSuggestions: List<PlantChatSuggestion>) {
        val oldSuggestions = suggestions
        suggestions = newSuggestions

        val diffCallback = PlantSuggestionDiffCallback(oldSuggestions, newSuggestions)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlantSuggestionViewHolder {
        val binding = ItemPlantChatSuggestionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PlantSuggestionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlantSuggestionViewHolder, position: Int) {
        holder.bind(suggestions[position])
    }

    override fun getItemCount(): Int = suggestions.size

    inner class PlantSuggestionViewHolder(
        private val binding: ItemPlantChatSuggestionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onSuggestionClick(suggestions[position])
                }
            }
        }

        fun bind(suggestion: PlantChatSuggestion) {
            binding.tvTitle.text = suggestion.title
        }
    }
}

class PlantSuggestionDiffCallback(
    private val oldList: List<PlantChatSuggestion>,
    private val newList: List<PlantChatSuggestion>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}
