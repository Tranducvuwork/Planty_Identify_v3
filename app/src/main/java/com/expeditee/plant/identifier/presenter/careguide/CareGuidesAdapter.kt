package com.expeditee.plant.identifier.presenter.careguide

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.databinding.ItemCareGuideBinding
import com.expeditee.plant.identifier.extension.loadImageWithGithub
import com.expeditee.plant.identifier.extension.onClickWithScale
import com.expeditee.plant.identifier.model.reponse.SuggestedPlant

class CareGuidesAdapter(
    private val onGuideClick: (SuggestedPlant) -> Unit
) : PagingDataAdapter<SuggestedPlant, CareGuidesAdapter.CareGuideViewHolder>(CareGuideDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CareGuideViewHolder {
        val binding = ItemCareGuideBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CareGuideViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CareGuideViewHolder, position: Int) {
        getItem(position)?.let { plant ->
            holder.bind(plant)
        }
    }

    inner class CareGuideViewHolder(
        private val binding: ItemCareGuideBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(plant: SuggestedPlant) {
            binding.apply {
                val displayName =
                    plant.commonNames?.firstOrNull() ?: plant.scientificName ?: "Unknown"
                tvPlantName.text = displayName

                tvScientificName.text = plant.scientificName ?: ""

                val careInfo = buildString {
                    plant.growthCultivation?.lightRequirements?.let {
                        append("☀ $it")
                    }
                    plant.growthCultivation?.watering?.frequency?.let {
                        if (isNotEmpty()) append(" • ")
                        append("💧 $it")
                    }
                }
                val careInfoText = careInfo.ifEmpty { "Learn about care requirements" }
                tvCareInfo.text = careInfoText

                // Load image with fallback: urlSmall -> url -> null
                val imageUrl = plant.mainImage?.urlSmall ?: plant.mainImage?.url


                if (imageUrl != null && imageUrl.isNotEmpty()) {
                    ivPlantImage.loadImageWithGithub(imageUrl)
                } else {
                    // Set placeholder if no image available
                    ivPlantImage.setImageResource(R.drawable.ic_placeholder_loading)
                }

                root.onClickWithScale {
                    onGuideClick(plant)
                }
            }
        }
    }

    private class CareGuideDiffCallback : DiffUtil.ItemCallback<SuggestedPlant>() {
        override fun areItemsTheSame(oldItem: SuggestedPlant, newItem: SuggestedPlant): Boolean {
            return oldItem.scientificName == newItem.scientificName
        }

        override fun areContentsTheSame(oldItem: SuggestedPlant, newItem: SuggestedPlant): Boolean {
            return oldItem == newItem
        }
    }
}
