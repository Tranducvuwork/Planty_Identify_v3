package com.expeditee.plant.identifier.presenter.chat.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.expeditee.plant.identifier.databinding.ItemPlantChatMessageAiBinding
import com.expeditee.plant.identifier.databinding.ItemPlantChatMessageUserBinding
import com.expeditee.plant.identifier.extension.beGone
import com.expeditee.plant.identifier.extension.beVisible
import com.expeditee.plant.identifier.model.PlantChatSuggestion
import com.expeditee.plant.identifier.thesis.PlantImages
import io.noties.markwon.Markwon
import java.util.UUID

class PlantChatAdapter(
    private val onSuggestionClick: (PlantChatSuggestion) -> Unit,
    private val onSpeakClick: (String) -> Unit = {},
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_ASSISTANT = 2
        private const val VIEW_TYPE_TYPING = 3
    }

    data class MessageItem(
        val id: String = UUID.randomUUID().toString(),
        val content: String,
        val isUser: Boolean,
        val isTyping: Boolean = false,
        val showSuggestions: Boolean = false,
        val imageUrl: String? = null,
        val imageUrls: List<String> = emptyList(),
    )

    private val messages = mutableListOf<MessageItem>()
    private var recyclerView: RecyclerView? = null
    private var currentSuggestions: List<PlantChatSuggestion> = emptyList()
    private var hasUserAskedQuestion = false

    fun setRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
    }

    fun addUserMessage(message: String, imageUrl: String? = null) {
        hasUserAskedQuestion = true
        currentSuggestions = emptyList()
        messages.add(MessageItem(content = message, isUser = true, imageUrl = imageUrl))
        notifyItemInserted(messages.size - 1)
        scrollToBottom()
    }

    fun addAssistantMessage(
        response: String,
        showSuggestions: Boolean = false,
        imageUrls: List<String> = emptyList(),
    ) {
        val lastIndex = messages.size - 1
        val isInitialMessage = (messages.isEmpty() || (lastIndex >= 0 && messages[lastIndex].isTyping)) && !hasUserAskedQuestion
        val shouldShowSuggestions = showSuggestions && isInitialMessage

        if (lastIndex >= 0 && !messages[lastIndex].isUser && messages[lastIndex].isTyping) {
            messages[lastIndex] = MessageItem(
                content = response,
                isUser = false,
                showSuggestions = shouldShowSuggestions,
                imageUrls = imageUrls,
            )
            notifyItemChanged(lastIndex)
        } else {
            val insertIndex = messages.size
            messages.add(
                MessageItem(
                    content = response,
                    isUser = false,
                    showSuggestions = shouldShowSuggestions,
                    imageUrls = imageUrls,
                )
            )
            notifyItemInserted(insertIndex)
        }
        scrollToBottom()
    }

    fun updateAssistantMessageStreaming(
        response: String,
        showSuggestions: Boolean = false,
        imageUrls: List<String> = emptyList(),
    ) {
        val lastIndex = messages.size - 1

        if (lastIndex >= 0 && !messages[lastIndex].isUser) {
            val currentMessage = messages[lastIndex]
            val contentChanged = currentMessage.content != response ||
                currentMessage.isTyping ||
                currentMessage.imageUrls != imageUrls
            if (!contentChanged) return

            messages[lastIndex] = currentMessage.copy(
                content = response,
                isTyping = false,
                showSuggestions = showSuggestions,
                imageUrls = imageUrls,
            )
            notifyItemChanged(lastIndex)
            scrollToBottom()
        } else if (response.isNotBlank()) {
            val insertPosition = messages.size
            messages.add(
                MessageItem(
                    content = response,
                    isUser = false,
                    isTyping = false,
                    showSuggestions = showSuggestions,
                    imageUrls = imageUrls,
                )
            )
            notifyItemInserted(insertPosition)
            scrollToBottom()
        }
    }

    fun showTypingIndicator() {
        messages.add(MessageItem(content = "", isUser = false, isTyping = true))
        notifyItemInserted(messages.size - 1)
        scrollToBottom()
    }

    fun removeMessageAt(index: Int) {
        if (index in messages.indices) {
            messages.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    fun updateSuggestions(suggestions: List<PlantChatSuggestion>) {
        if (hasUserAskedQuestion) {
            currentSuggestions = emptyList()
            return
        }

        currentSuggestions = suggestions

        var firstAssistantMessageIndex = -1
        for (i in messages.indices) {
            if (!messages[i].isUser && !messages[i].isTyping) {
                firstAssistantMessageIndex = i
                break
            }
        }

        if (firstAssistantMessageIndex >= 0) {
            for (i in messages.indices) {
                if (!messages[i].isUser && !messages[i].isTyping) {
                    val shouldShow = i == firstAssistantMessageIndex && suggestions.isNotEmpty()
                    if (messages[i].showSuggestions != shouldShow) {
                        messages[i] = messages[i].copy(showSuggestions = shouldShow)
                        notifyItemChanged(i)
                    } else if (shouldShow) {
                        notifyItemChanged(i)
                    }
                }
            }
        }
    }

    fun hideSuggestions() {
        updateSuggestions(emptyList())
    }

    fun notifyItemContentChangedAt(index: Int) {
        if (index in messages.indices) {
            notifyItemChanged(index)
        }
    }

    private fun scrollToBottom() {
        recyclerView?.let { rv ->
            if (messages.isNotEmpty()) {
                rv.post { rv.scrollToPosition(messages.size - 1) }
            }
        }
    }

    private fun isLastAssistantMessage(position: Int): Boolean {
        if (position >= messages.size) return false
        for (i in messages.size - 1 downTo 0) {
            if (!messages[i].isUser && !messages[i].isTyping) {
                return position == i
            }
        }
        return false
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            messages[position].isTyping -> VIEW_TYPE_TYPING
            messages[position].isUser -> VIEW_TYPE_USER
            else -> VIEW_TYPE_ASSISTANT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_USER -> {
                val binding = ItemPlantChatMessageUserBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                UserMessageViewHolder(binding)
            }
            VIEW_TYPE_ASSISTANT, VIEW_TYPE_TYPING -> {
                val binding = ItemPlantChatMessageAiBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                AssistantMessageViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        when (holder) {
            is UserMessageViewHolder -> holder.bind(message)
            is AssistantMessageViewHolder -> holder.bind(message, isLastAssistantMessage(position))
        }
    }

    override fun getItemCount(): Int = messages.size

    inner class UserMessageViewHolder(
        private val binding: ItemPlantChatMessageUserBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: MessageItem) {
            binding.tvMessage.text = message.content
            if (message.content.isBlank()) {
                binding.tvMessage.beGone()
            } else {
                binding.tvMessage.beVisible()
            }

            if (message.imageUrl.isNullOrBlank()) {
                binding.ivMessageImage.setImageDrawable(null)
                binding.ivMessageImage.beGone()
            } else {
                binding.ivMessageImage.setImageURI(message.imageUrl.toUri())
                binding.ivMessageImage.beVisible()
            }
        }
    }

    inner class AssistantMessageViewHolder(
        private val binding: ItemPlantChatMessageAiBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val markwon = Markwon.create(binding.root.context)

        private val suggestionAdapter = PlantChatSuggestionAdapter { suggestion ->
            onSuggestionClick(suggestion)
        }

        init {
            binding.rcvSuggestions.apply {
                layoutManager = LinearLayoutManager(binding.root.context, LinearLayoutManager.VERTICAL, false)
                adapter = suggestionAdapter
                setHasFixedSize(true)
                itemAnimator = null
            }
        }

        fun bind(message: MessageItem, isLastMessage: Boolean) {
            if (message.isTyping) {
                binding.tvMessage.beGone()
                binding.llTypingIndicator.beVisible()
                binding.rcvSuggestions.beGone()
                binding.ivSpeak.beGone()
                binding.layoutPestImages.beGone()
                binding.layoutPestImages.removeAllViews()
            } else {
                binding.tvMessage.beVisible()
                binding.llTypingIndicator.beGone()
                markwon.setMarkdown(binding.tvMessage, message.content)
                bindPestImages(message.imageUrls)

                val canSpeak = message.content.isNotBlank()
                if (canSpeak) {
                    binding.ivSpeak.beVisible()
                    binding.ivSpeak.setOnClickListener {
                        onSpeakClick(message.content)
                    }
                } else {
                    binding.ivSpeak.beGone()
                    binding.ivSpeak.setOnClickListener(null)
                }

                val shouldShowSuggestions = isLastMessage && message.showSuggestions && currentSuggestions.isNotEmpty()
                if (shouldShowSuggestions) {
                    binding.rcvSuggestions.beVisible()
                    suggestionAdapter.updateSuggestionsWithDiff(currentSuggestions)
                } else {
                    binding.rcvSuggestions.beGone()
                }
            }
        }

        private fun bindPestImages(imageUrls: List<String>) {
            binding.layoutPestImages.removeAllViews()
            if (imageUrls.isEmpty()) {
                binding.layoutPestImages.beGone()
                return
            }
            binding.layoutPestImages.beVisible()
            val density = binding.root.context.resources.displayMetrics.density
            val imageHeight = (120 * density).toInt()
            val margin = (6 * density).toInt()
            imageUrls.forEach { url ->
                val imageView = android.widget.ImageView(binding.root.context).apply {
                    layoutParams = ViewGroup.MarginLayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        imageHeight,
                    ).apply {
                        topMargin = margin
                    }
                    scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                    contentDescription = binding.root.context.getString(
                        com.expeditee.plant.identifier.R.string.chat_pest_illustration,
                    )
                }
                PlantImages.load(imageView, url, roundedCornerDp = 8)
                binding.layoutPestImages.addView(imageView)
            }
        }
    }
}
