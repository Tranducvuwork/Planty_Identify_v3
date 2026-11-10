package com.expeditee.plant.identifier.presenter.reminder

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.databinding.ItemReminderScheduleBinding
import com.expeditee.plant.identifier.extension.onClickWithScale
import com.expeditee.plant.identifier.model.ReminderScheduleModel

class ReminderAdapter(
    private val onEdit: (ReminderScheduleModel) -> Unit,
    private val onAction: (ReminderScheduleModel) -> Unit,
    private val onDelete: (ReminderScheduleModel) -> Unit,
) : ListAdapter<ReminderScheduleModel, ReminderAdapter.ReminderViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReminderViewHolder {
        return ReminderViewHolder(
            ItemReminderScheduleBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ReminderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ReminderViewHolder(
        private val binding: ItemReminderScheduleBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ReminderScheduleModel) {
            with(binding) {
                if (item.imagePath != null) {
                    Glide.with(ivPlant).load(item.imagePath).into(ivPlant)
                } else if (item.imageRes != 0) {
                    ivPlant.setImageResource(item.imageRes)
                } else {
                    ivPlant.setImageResource(R.drawable.bg_card_light_meter)
                }

                tvPlantName.text = item.plantName
                tvRemindType.text = item.remindType
                tvRemindTime.text = item.remindTime
                tvRepeat.text = item.repeatType
                tvAction.onClickWithScale { onAction(item) }
                root.onClickWithScale { onEdit(item) }
                ivMore.onClickWithScale { onDelete(item) }
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<ReminderScheduleModel>() {
        override fun areItemsTheSame(
            oldItem: ReminderScheduleModel,
            newItem: ReminderScheduleModel
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: ReminderScheduleModel,
            newItem: ReminderScheduleModel
        ): Boolean {
            return oldItem == newItem
        }
    }
}
