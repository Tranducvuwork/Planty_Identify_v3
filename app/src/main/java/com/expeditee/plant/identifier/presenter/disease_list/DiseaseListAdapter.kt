package com.expeditee.plant.identifier.presenter.disease_list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.databinding.ItemDiseaseBinding
import com.expeditee.plant.identifier.extension.loadImageWithGithub
import com.expeditee.plant.identifier.extension.onClickWithScale
import com.expeditee.plant.identifier.model.PestDisease

class DiseaseListAdapter(
    private val onDiseaseClick: (PestDisease) -> Unit
) : ListAdapter<PestDisease, DiseaseListAdapter.DiseaseViewHolder>(DiseaseDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiseaseViewHolder {
        val binding = ItemDiseaseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DiseaseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DiseaseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DiseaseViewHolder(
        private val binding: ItemDiseaseBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(disease: PestDisease) {
            binding.tvDiseaseName.text = disease.displayName()
            binding.tvDiseaseDescription.text = disease.firstSummary()
            val image = disease.firstImage()
            if (image.isBlank()) {
                binding.ivDiseaseImage.setImageResource(R.drawable.ic_placeholder_loading)
            } else {
                binding.ivDiseaseImage.loadImageWithGithub(image)
            }
            binding.root.onClickWithScale { onDiseaseClick(disease) }
        }
    }

    private class DiseaseDiffCallback : DiffUtil.ItemCallback<PestDisease>() {
        override fun areItemsTheSame(oldItem: PestDisease, newItem: PestDisease): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PestDisease, newItem: PestDisease): Boolean {
            return oldItem == newItem
        }
    }
}
