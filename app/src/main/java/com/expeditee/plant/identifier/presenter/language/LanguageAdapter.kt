package com.expeditee.plant.identifier.presenter.language

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.expeditee.plant.identifier.databinding.ItemLanguageBinding
import com.expeditee.plant.identifier.extension.DataBindingViewHolder
import com.expeditee.plant.identifier.extension.onDebounceClick
import com.expeditee.plant.identifier.model.Language


class LanguageAdapter(
    var data: List<Language>,
    private val onLanguageClicked: (Language) -> Unit
) : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {

    companion object {

        private const val SELECT_CHANGED = ".SELECT_CHANGED"
    }

    val selectedLanguage: Language?
        get() = data.getOrNull(selectedPosition)

    fun submitData(newData: List<Language>) {
        data = newData
        notifyDataSetChanged()
    }

    private var selectedPosition = RecyclerView.NO_POSITION
        set(newPosition) {
            val oldPosition = field
            field = newPosition
            if (oldPosition != RecyclerView.NO_POSITION && oldPosition in data.indices) {
                notifyItemChanged(oldPosition, SELECT_CHANGED)
            }
            if (newPosition != RecyclerView.NO_POSITION && newPosition in data.indices) {
                notifyItemChanged(newPosition, SELECT_CHANGED)
            }
        }

    inner class LanguageViewHolder(
        binding: ItemLanguageBinding
    ) : DataBindingViewHolder<Language, ItemLanguageBinding>(binding) {

        override fun onBind(item: Language) {
            with(dataBinding) {
                nameLanguage = item.country
                ivLanguage.setImageResource(item.iconFlag)
                updateSelectState()
                onItemClicked = onDebounceClick {
                    if (layoutPosition == selectedPosition) {
                        return@onDebounceClick
                    }
                    selectedPosition = layoutPosition
                    onLanguageClicked(item)
                }
            }
        }

        fun updateSelectState() = dataBinding.run {
            isSelected = layoutPosition == selectedPosition
            root.isSelected = layoutPosition == selectedPosition
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): LanguageViewHolder {
        return LanguageViewHolder(
            ItemLanguageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        holder.onBind(data[position])
    }

    override fun onBindViewHolder(
        holder: LanguageViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNotEmpty()) {
            holder.updateSelectState()
            return
        }
        super.onBindViewHolder(holder, position, payloads)
    }

    override fun getItemCount(): Int = data.size
}
