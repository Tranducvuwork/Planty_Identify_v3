package com.expeditee.plant.identifier.presenter.home.mygardentab

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.databinding.ItemMyGardenPlantBinding
import com.expeditee.plant.identifier.extension.loadImageWithGithub
import com.expeditee.plant.identifier.extension.onClickWithScale
import com.expeditee.plant.identifier.extension.onDebounceClick
import com.expeditee.plant.identifier.extension.setScientificTextCustom
import com.expeditee.plant.identifier.model.MyPlantModel
import com.expeditee.plant.identifier.widget.popup.MyGardenPopUp

class MyGardenPlantAdapter(
    private val onItemClick: (MyPlantModel) -> Unit,
    private val onMove: (MyPlantModel) -> Unit,
    private val onDelete: (MyPlantModel) -> Unit,
    private val onAddReminder: (MyPlantModel) -> Unit
) : PagingDataAdapter<MyPlantModel, MyGardenPlantAdapter.MyGardenPlantViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyGardenPlantViewHolder {
        return MyGardenPlantViewHolder(
            ItemMyGardenPlantBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: MyGardenPlantViewHolder, position: Int) {
        getItem(position)?.let(holder::bind)
    }

    inner class MyGardenPlantViewHolder(
        private val binding: ItemMyGardenPlantBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MyPlantModel) {
            with(binding) {
                val context = ivMenu.context

                tvMyGardenPlantName.text = item.name
                val prefix = context.getString(R.string.scientific_name_prefix)
                val name = item.scientificName.takeIf { it.isNotEmpty() } ?: item.name

                tvScientificName.setScientificTextCustom(
                    prefix = prefix,
                    suffix = name,
                    prefixFontRes = R.font.montserrat_medium,
                    suffixFontRes = R.font.montserrat_light,
                    prefixColor = Color.BLACK
                )

                ivMyGardenPlant.loadImageWithGithub(item.imageFile)

                ivMenu.onDebounceClick {
                    MyGardenPopUp.show(
                        context = ivMenu.context,
                        anchor = ivMenu,
                        item = item
                    ) { action, selectedItem ->
                        when (action) {
                            MyGardenPopUp.MenuAction.MOVE -> onMove(selectedItem)
                            MyGardenPopUp.MenuAction.DELETE -> onDelete(selectedItem)
                        }
                    }
                }

                with(chipStatus) {
                    text = item.status
                    val statusBg = when (item.status.lowercase()) {
                        context.getString(R.string.status_healthy) -> R.drawable.bg_status_chip_main
                        context.getString(R.string.status_unhealthy) -> R.drawable.bg_status_chip_orange
                        else -> R.drawable.bg_status_chip_main
                    }
                    setBackgroundResource(statusBg)
                }

                btnAddReminder.onClickWithScale {
                    onAddReminder(item)
                }

                root.onDebounceClick {
                    onItemClick(item)
                }
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<MyPlantModel>() {
            override fun areItemsTheSame(oldItem: MyPlantModel, newItem: MyPlantModel) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: MyPlantModel, newItem: MyPlantModel) =
                oldItem == newItem
        }
    }
}
