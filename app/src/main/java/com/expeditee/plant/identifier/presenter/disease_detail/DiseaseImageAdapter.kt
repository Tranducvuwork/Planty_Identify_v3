package com.expeditee.plant.identifier.presenter.disease_detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.databinding.ItemDiseaseImageBinding
import com.expeditee.plant.identifier.extension.loadImageWithGithub
import com.expeditee.plant.identifier.model.PestDisease

class DiseaseImageAdapter : ListAdapter<PestDisease.Image, DiseaseImageAdapter.ImageViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemDiseaseImageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(getItem(position), position + 1)
    }

    inner class ImageViewHolder(
        private val binding: ItemDiseaseImageBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: PestDisease.Image, index: Int) {
            val url = item.regularUrl.ifBlank { item.smallUrl.ifBlank { item.thumbnail } }
            if (url.isBlank()) {
                binding.ivDiseaseImage.setImageResource(R.drawable.ic_placeholder_loading)
            } else {
                binding.ivDiseaseImage.loadImageWithGithub(url)
            }
            binding.tvDiseaseImageTitle.text = "Image $index"
        }
    }

    private object Diff : DiffUtil.ItemCallback<PestDisease.Image>() {
        override fun areItemsTheSame(oldItem: PestDisease.Image, newItem: PestDisease.Image): Boolean {
            return oldItem.originalUrl == newItem.originalUrl
        }

        override fun areContentsTheSame(oldItem: PestDisease.Image, newItem: PestDisease.Image): Boolean {
            return oldItem == newItem
        }
    }
}
