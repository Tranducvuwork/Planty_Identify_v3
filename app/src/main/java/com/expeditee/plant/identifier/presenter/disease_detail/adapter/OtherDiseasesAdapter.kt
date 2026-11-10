package com.expeditee.plant.identifier.presenter.disease_detail.adapter

import androidx.databinding.ViewDataBinding
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.common.BaseRecyclerView
import com.expeditee.plant.identifier.databinding.ItemOtherDiseaseBinding
import com.expeditee.plant.identifier.extension.loadImageWithGithub
import com.expeditee.plant.identifier.presenter.disease_detail.OtherDiseaseUiModel

class OtherDiseasesAdapter(
    private val onClickItem: (OtherDiseaseUiModel, Int) -> Unit
) : BaseRecyclerView<OtherDiseaseUiModel>() {

    override fun getItemLayout(): Int = R.layout.item_other_disease

    override fun setData(binding: ViewDataBinding, item: OtherDiseaseUiModel, position: Int) {
        val itemBinding = binding as ItemOtherDiseaseBinding
        itemBinding.diseaseNameText.text = item.displayName
        itemBinding.diseaseImageView.loadImageWithGithub(item.imageUrl)
    }

    override fun onClickView(binding: ViewDataBinding, obj: OtherDiseaseUiModel, position: Int) {
        onClickItem(obj, position)
    }

    override fun submitData(newData: List<OtherDiseaseUiModel>) {
        updateData(newData)
    }
}