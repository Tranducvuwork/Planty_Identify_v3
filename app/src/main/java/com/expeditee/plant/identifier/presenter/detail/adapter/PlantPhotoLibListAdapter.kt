package com.expeditee.plant.identifier.presenter.detail.adapter

import androidx.databinding.ViewDataBinding
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.common.BaseRecyclerView
import com.expeditee.plant.identifier.databinding.ItemPlantPhotoBinding
import com.expeditee.plant.identifier.extension.loadImageWithGithub

class PlantPhotoLibListAdapter(
    private val onClickItem: (String, Int) -> Unit
) : BaseRecyclerView<String>() {

    override fun getItemLayout(): Int = R.layout.item_plant_photo

    override fun setData(binding: ViewDataBinding, item: String, position: Int) {
        val itemBinding = binding as ItemPlantPhotoBinding
        itemBinding.photoImageView.loadImageWithGithub(item)
    }

    override fun onClickView(binding: ViewDataBinding, obj: String, position: Int) {
        onClickItem(obj, position)
    }

    override fun submitData(newData: List<String>) {
        updateData(newData)
    }
}
