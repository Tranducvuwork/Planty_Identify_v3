package com.expeditee.plant.identifier.presenter.disease_detail.adapter

import androidx.databinding.ViewDataBinding
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.common.BaseRecyclerView
import com.expeditee.plant.identifier.databinding.ItemSimilarImageBinding
import com.expeditee.plant.identifier.extension.loadImageWithGithub
import com.expeditee.plant.identifier.presenter.disease_detail.SimilarImageUiModel

class SimilarImagesAdapter(
    private val onClickItem: (SimilarImageUiModel, Int) -> Unit
) : BaseRecyclerView<SimilarImageUiModel>() {

    override fun getItemLayout(): Int = R.layout.item_similar_image

    override fun setData(binding: ViewDataBinding, item: SimilarImageUiModel, position: Int) {
        val itemBinding = binding as ItemSimilarImageBinding
        val imageUrl = item.urlSmall ?: item.url
        itemBinding.similarImageView.loadImageWithGithub(imageUrl)
    }

    override fun onClickView(binding: ViewDataBinding, obj: SimilarImageUiModel, position: Int) {
        onClickItem(obj, position)
    }

    override fun submitData(newData: List<SimilarImageUiModel>) {
        updateData(newData)
    }
}
