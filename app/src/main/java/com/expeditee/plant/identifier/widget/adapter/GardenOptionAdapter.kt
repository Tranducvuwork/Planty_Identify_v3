package com.expeditee.plant.identifier.widget.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.expeditee.plant.identifier.databinding.ItemGardenOptionBinding
import com.expeditee.plant.identifier.model.GardenOption

class GardenOptionAdapter(
    private val onSelected: (GardenOption) -> Unit
) : ListAdapter<GardenOption, GardenOptionAdapter.ViewHolder>(DiffCallback) {

    private var selectedId: String? = null

    fun setSelected(id: String) {
        val previousId = selectedId
        selectedId = id

        val prevIndex = currentList.indexOfFirst { it.id == previousId }
        val newIndex = currentList.indexOfFirst { it.id == selectedId }

        if (prevIndex != -1) notifyItemChanged(prevIndex)
        if (newIndex != -1) notifyItemChanged(newIndex)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemGardenOptionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemGardenOptionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: GardenOption) {
            binding.apply {
                tvGardenName.text = item.name

                loadPlantImage(item)

                root.isSelected = item.id == selectedId

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

        private fun loadPlantImage(item: GardenOption) {
            with(binding.ivPlantImage) {
                when {
                    !item.imageUrl.isNullOrEmpty() -> {
                        Glide.with(context)
                            .load(item.imageUrl)
                            .centerCrop()
                            .into(this)
                    }
                    item.imageRes != 0 -> {
                        setImageResource(item.imageRes)
                    }
                    else -> {
                        setImageDrawable(null)
                    }
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