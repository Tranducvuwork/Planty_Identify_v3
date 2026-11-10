package com.expeditee.plant.identifier.presenter.camera

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.model.CameraMode

class CameraModeAdapter(
    private val onModeSelected: (CameraMode) -> Unit,
    private val onItemClick: (Int) -> Unit = {},
) : RecyclerView.Adapter<CameraModeAdapter.ViewHolder>() {

    private val items = mutableListOf<CameraMode>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.camera_mode_snap_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    @SuppressLint("NotifyDataSetChanged")
    fun submitData(newData: List<CameraMode>) {
        items.clear()
        items.addAll(newData)
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val modeIcon: ImageView = itemView.findViewById(R.id.modeIcon)
        private val modeText: TextView = itemView.findViewById(R.id.modeText)

        init {
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(position)
                    onModeSelected(items[position].copy(isSelected = true))
                }
            }
        }

        fun bind(item: CameraMode) {
            modeIcon.setImageResource(item.imgMode)
            val label = when (item.typeMode) {
                CameraMode.TYPE_IDENTIFY -> itemView.context.getString(R.string.identify)
                CameraMode.TYPE_DIAGNOSE -> itemView.context.getString(R.string.diagnose)
                else -> ""
            }
            modeText.text = label
            itemView.isSelected = item.isSelected
            val color = ContextCompat.getColor(
                itemView.context,
                if (item.isSelected) R.color.text_camera_mode_selected else R.color.text_camera_mode_muted,
            )
            modeText.setTextColor(color)
            modeIcon.setColorFilter(color)
        }
    }
}
