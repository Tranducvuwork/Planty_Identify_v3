package com.expeditee.plant.identifier.presenter.home.explore

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.expeditee.plant.identifier.databinding.ItemExploreCategoryBinding
import com.expeditee.plant.identifier.extension.onClickWithScale
import com.expeditee.plant.identifier.model.ExploreCategoryModel

class ExploreCategoryAdapter(
    private val items: List<ExploreCategoryModel>,
    private val onItemClick: (ExploreCategoryModel) -> Unit = {}
) : RecyclerView.Adapter<ExploreCategoryAdapter.ExploreCategoryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExploreCategoryViewHolder {
        return ExploreCategoryViewHolder(
            ItemExploreCategoryBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ExploreCategoryViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ExploreCategoryViewHolder(
        private val binding: ItemExploreCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ExploreCategoryModel) {
            with(binding) {
                tvCategoryName.text = item.name
                ivCategory.setImageResource(item.imageRes)

                root.onClickWithScale {
                    onItemClick(item)
                }
            }
        }
    }
}