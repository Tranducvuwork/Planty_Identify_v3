package com.expeditee.plant.identifier.presenter.home.managementsite

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.databinding.ItemManagementSiteBinding
import com.expeditee.plant.identifier.model.GardenOption

class ManagementSiteAdapter(
    private val isSelectionMode: Boolean = true,
    private val onSiteClick: (GardenOption) -> Unit,
    private val onArrowClick: (GardenOption) -> Unit = {}
) : ListAdapter<GardenOption, ManagementSiteAdapter.ViewHolder>(DiffCallback) {
    private var selectedSiteId: String = ""

    fun setSelectedSiteId(siteId: String) {
        val oldId = selectedSiteId
        selectedSiteId = siteId
        val oldIndex = currentList.indexOfFirst { it.id == oldId }
        val newIndex = currentList.indexOfFirst { it.id == selectedSiteId }
        if (oldIndex != -1) notifyItemChanged(oldIndex)
        if (newIndex != -1) notifyItemChanged(newIndex)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemManagementSiteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemManagementSiteBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: GardenOption) {
            binding.tvSiteName.text = item.name
            val isSelected = item.id == selectedSiteId
            binding.layoutSiteItem.isSelected = isSelected

            if (isSelectionMode) {
                binding.ivSelected.visibility = View.VISIBLE
                binding.ivArrow.visibility = View.GONE
                binding.ivSelected.setImageResource(
                    if (isSelected) R.drawable.ic_checkbox_checked else R.drawable.ic_checkbox_unchecked
                )
            } else {
                binding.ivSelected.visibility = View.GONE
                binding.ivArrow.visibility = View.VISIBLE
            }

            binding.root.setOnClickListener {
                if (isSelectionMode) {
                    if (selectedSiteId != item.id) {
                        setSelectedSiteId(item.id)
                    }
                    onSiteClick(item)
                } else {
                    onArrowClick(item)
                }
            }

            binding.ivArrow.setOnClickListener {
                onArrowClick(item)
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<GardenOption>() {
        override fun areItemsTheSame(oldItem: GardenOption, newItem: GardenOption): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: GardenOption, newItem: GardenOption): Boolean {
            return oldItem == newItem
        }
    }
}
