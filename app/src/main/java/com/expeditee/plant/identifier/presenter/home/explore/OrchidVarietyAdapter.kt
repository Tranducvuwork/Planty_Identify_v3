package com.expeditee.plant.identifier.presenter.home.explore

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.databinding.ItemOrchidVarietyBinding
import com.expeditee.plant.identifier.extension.onClickWithScale
import com.expeditee.plant.identifier.thesis.OrchidVariety
import com.expeditee.plant.identifier.thesis.PlantImages

class OrchidVarietyAdapter(
    private val items: List<OrchidVariety>,
    private val onItemClick: (OrchidVariety) -> Unit,
) : RecyclerView.Adapter<OrchidVarietyAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
        ItemOrchidVarietyBinding.inflate(LayoutInflater.from(parent.context), parent, false),
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(
        private val binding: ItemOrchidVarietyBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: OrchidVariety) {
            binding.tvOrchidName.text = item.name
            binding.tvOrchidScientific.text = item.scientificName
            if (item.imagePath.isNullOrBlank()) {
                binding.ivOrchid.setImageResource(R.drawable.ic_flowers)
            } else {
                PlantImages.load(binding.ivOrchid, item.imagePath, R.drawable.ic_flowers)
            }
            binding.root.onClickWithScale { onItemClick(item) }
        }
    }
}
