package com.expeditee.plant.identifier.presenter.photo_library.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.databinding.ItemPhotoThumbnailBinding
import com.expeditee.plant.identifier.extension.loadImageWithGithub

class PhotoThumbnailAdapter(
    private val photoList: List<String>,
    private val onClickItem: (Int) -> Unit
) : RecyclerView.Adapter<PhotoThumbnailAdapter.ThumbnailViewHolder>() {

    private var selectedPosition: Int = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThumbnailViewHolder {
        val binding = ItemPhotoThumbnailBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ThumbnailViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ThumbnailViewHolder, position: Int) {
        holder.bind(photoList[position], position == selectedPosition)
        holder.itemView.setOnClickListener { onClickItem(position) }
    }

    override fun getItemCount(): Int = photoList.size

    fun setSelectedPosition(position: Int) {
        if (position == selectedPosition || position !in photoList.indices) return
        val previous = selectedPosition
        selectedPosition = position
        notifyItemChanged(previous)
        notifyItemChanged(selectedPosition)
    }

    fun setInitialSelection(position: Int) {
        selectedPosition = position.coerceIn(0, (photoList.lastIndex).coerceAtLeast(0))
        notifyDataSetChanged()
    }

    class ThumbnailViewHolder(private val binding: ItemPhotoThumbnailBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(photoUrl: String, isSelected: Boolean) {
            binding.thumbnailImageView.loadImageWithGithub(photoUrl)
            val colorRes = if (isSelected) R.color.primary_color else android.R.color.transparent
            binding.thumbnailCard.strokeColor =
                ContextCompat.getColor(binding.root.context, colorRes)
        }
    }
}
