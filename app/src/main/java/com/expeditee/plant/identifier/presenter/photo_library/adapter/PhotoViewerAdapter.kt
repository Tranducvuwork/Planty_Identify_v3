package com.expeditee.plant.identifier.presenter.photo_library.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.expeditee.plant.identifier.databinding.ItemPhotoViewerBinding
import com.expeditee.plant.identifier.extension.loadImageWithGithub

class PhotoViewerAdapter(
    private val photoList: List<String>
) : RecyclerView.Adapter<PhotoViewerAdapter.PhotoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val binding = ItemPhotoViewerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PhotoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(photoList[position])
    }

    override fun getItemCount(): Int = photoList.size

    class PhotoViewHolder(private val binding: ItemPhotoViewerBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(photoUrl: String) {
            binding.photoImageView.loadImageWithGithub(photoUrl)
        }
    }
}
