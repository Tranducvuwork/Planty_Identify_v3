package com.expeditee.plant.identifier.presenter.faq

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.databinding.ItemFaqListFaqBinding
import com.expeditee.plant.identifier.extension.loadImageWithGithub
import com.expeditee.plant.identifier.extension.onClickWithScale
import com.expeditee.plant.identifier.model.Faq

class FaqAdapter(
    private val onFaqClick: (Faq) -> Unit,
    private val onTagClick: (String) -> Unit
) : ListAdapter<Faq, FaqAdapter.FaqViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FaqViewHolder {
        val binding = ItemFaqListFaqBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FaqViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FaqViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FaqViewHolder(
        private val binding: ItemFaqListFaqBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Faq) = with(binding) {
            textFaqsListFaqQuestion.text = item.question
            if (item.imageUrl.isBlank()) {
                imageFaqsListFaqThumbnail.setImageResource(R.drawable.ic_placeholder_loading)
            } else {
                imageFaqsListFaqThumbnail.loadImageWithGithub(item.imageUrl)
            }
            textFaqsListFaqTags.text = item.tags.joinToString(" ") { "#$it" }
            textFaqsListFaqTags.setOnClickListener {
                item.tags.firstOrNull()?.let(onTagClick)
            }
            root.onClickWithScale { onFaqClick(item) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<Faq>() {
        override fun areItemsTheSame(oldItem: Faq, newItem: Faq): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Faq, newItem: Faq): Boolean = oldItem == newItem
    }
}
