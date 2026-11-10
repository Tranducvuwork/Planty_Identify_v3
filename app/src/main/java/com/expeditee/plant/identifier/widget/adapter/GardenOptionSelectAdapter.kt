package com.expeditee.plant.identifier.widget.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.expeditee.plant.identifier.databinding.ItemGardenOptionBinding
import com.expeditee.plant.identifier.databinding.ItemGardentOptionSelectedBinding
import com.expeditee.plant.identifier.model.GardenOption

class GardenOptionSelectAdapter(
    private val onSelected: (GardenOption) -> Unit
) : ListAdapter<GardenOption, GardenOptionSelectAdapter.ViewHolder>(DiffCallback) {

    private var selectedId: String = "plant"

    fun setSelected(id: String) {
        val previousId = selectedId
        selectedId = id

        val prevIndex = currentList.indexOfFirst { it.id == previousId }
        val newIndex = currentList.indexOfFirst { it.id == selectedId }

        if (prevIndex != -1) notifyItemChanged(prevIndex)
        if (newIndex != -1) notifyItemChanged(newIndex)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemGardentOptionSelectedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemGardentOptionSelectedBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: GardenOption) {
            binding.apply {
                tvGardenName.text = item.name

                ivSelect.isSelected = item.id == selectedId

                root.setOnClickListener {
                    if (item.id == selectedId) return@setOnClickListener

                    val previousIndex = currentList.indexOfFirst { it.id == selectedId }
                    selectedId = item.id

                    if (previousIndex >= 0) {
                        notifyItemChanged(previousIndex)
                    }

                    notifyItemChanged(bindingAdapterPosition)

                    onSelected(item)
                }
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<GardenOption>() {
            override fun areItemsTheSame(a: GardenOption, b: GardenOption) = a.id == b.id
            override fun areContentsTheSame(a: GardenOption, b: GardenOption) = a == b
        }
    }
}