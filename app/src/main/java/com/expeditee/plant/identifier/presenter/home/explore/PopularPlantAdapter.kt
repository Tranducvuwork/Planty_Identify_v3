package com.expeditee.plant.identifier.presenter.home.explore

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.expeditee.plant.identifier.databinding.ItemExplorePopularPlantBinding
import com.expeditee.plant.identifier.extension.loadImageWithGithub
import com.expeditee.plant.identifier.extension.onClickWithScale
import com.expeditee.plant.identifier.model.PopularPlantModel

class PopularPlantAdapter :
    ListAdapter<PopularPlantModel, PopularPlantAdapter.PopularPlantViewHolder>(
        DiffCallback
    ) {

    var onPlantClick: ((PopularPlantModel) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PopularPlantViewHolder {
        return PopularPlantViewHolder(
            ItemExplorePopularPlantBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: PopularPlantViewHolder, position: Int) {
        getItem(position)?.let(holder::bind)
    }

    inner class PopularPlantViewHolder(
        private val binding: ItemExplorePopularPlantBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: PopularPlantModel) {
            with(binding) {
                tvPlantName.text = item.name
                binding.item = item
                ivPlant.loadImageWithGithub(item.imageUrl)
                root.onClickWithScale {
                    onPlantClick?.invoke(item)
                }
                binding.executePendingBindings()
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<PopularPlantModel>() {
            override fun areItemsTheSame(
                oldItem: PopularPlantModel,
                newItem: PopularPlantModel
            ): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(
                oldItem: PopularPlantModel,
                newItem: PopularPlantModel
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}