package com.expeditee.plant.identifier.widget.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.expeditee.plant.identifier.R

class PreviousFertilizingDateAdapter(
    private val context: Context,
    private val items: List<String>,
    private val rowHeightPx: Int,
    private val onPick: (Int) -> Unit,
) : RecyclerView.Adapter<PreviousFertilizingDateAdapter.VH>() {

    private var selectedPosition: Int = 0

    fun setSelectedPosition(position: Int) {
        if (position == selectedPosition) return
        val old = selectedPosition
        selectedPosition = position
        notifyItemChanged(old)
        notifyItemChanged(selectedPosition)
    }

    inner class VH(val textView: TextView) : RecyclerView.ViewHolder(textView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val tv = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_fertilizing_date, parent, false) as TextView
        tv.layoutParams = RecyclerView.LayoutParams(
            RecyclerView.LayoutParams.MATCH_PARENT,
            rowHeightPx,
        )
        return VH(tv)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val label = items[position]
        holder.textView.text = label
        val selected = position == selectedPosition
        holder.textView.setTextColor(
            ContextCompat.getColor(
                context,
                if (selected) R.color.reminder_green else R.color.reminder_text_secondary,
            ),
        )
        holder.textView.alpha = if (selected) 1f else 0.55f
        holder.textView.background = ContextCompat.getDrawable(
            context,
            if (selected) R.drawable.bg_fertilizing_row_selected else R.drawable.bg_fertilizing_row_unselected,
        )
        holder.textView.setOnClickListener {
            setSelectedPosition(position)
            onPick(position)
        }
    }
}