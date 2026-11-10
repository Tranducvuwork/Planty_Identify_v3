package com.expeditee.plant.identifier.presenter.chat

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.ViewGroup.MarginLayoutParams
import android.view.inputmethod.EditorInfo
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.LinearLayoutManager
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.common.BaseActivity
import com.expeditee.plant.identifier.common.OnPerformBackPressed
import com.expeditee.plant.identifier.common.ResultFlow
import com.expeditee.plant.identifier.databinding.ActivityPlantChatBinding
import com.expeditee.plant.identifier.extension.collectLatestRepeatOnLifecycle
import com.expeditee.plant.identifier.extension.hideKeyboard
import com.expeditee.plant.identifier.extension.ime
import com.expeditee.plant.identifier.extension.isNetwork
import com.expeditee.plant.identifier.extension.onDebounceClick
import com.expeditee.plant.identifier.extension.statusBars
import com.expeditee.plant.identifier.extension.showToast
import com.expeditee.plant.identifier.extension.showKeyboard
import com.expeditee.plant.identifier.model.PlantChatSuggestion
import com.expeditee.plant.identifier.presenter.chat.adapter.PlantChatAdapter
import com.expeditee.plant.identifier.thesis.ChatPestIllustration
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Locale

class PlantAssistantChatActivity : BaseActivity<ActivityPlantChatBinding, PlantAssistantChatViewModel, PlantAssistantChatIntent>() {

    companion object {
        const val EXTRA_INITIAL_QUESTION = "extra_initial_question"
        const val EXTRA_PLANT_NAME = "extra_plant_name"

        fun startWithQuestion(context: android.content.Context, question: String, plantName: String = "") {
            context.startActivity(
                android.content.Intent(context, PlantAssistantChatActivity::class.java).apply {
                    putExtra(EXTRA_INITIAL_QUESTION, question)
                    putExtra(EXTRA_PLANT_NAME, plantName)
                },
            )
        }
    }

    private lateinit var chatAdapter: PlantChatAdapter
    private var autoAsked = false
    private var textToSpeech: TextToSpeech? = null
    private var ttsReady = false

    private val voiceLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        val spoken = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            ?.trim()
            .orEmpty()
        if (spoken.isBlank()) return@registerForActivityResult
        viewBinding.edtMessage.setText(spoken)
        viewBinding.edtMessage.setSelection(spoken.length)
        sendMessage()
    }

    private val audioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            launchVoiceInput()
        } else {
            showToast(R.string.chat_voice_permission)
        }
    }

    override fun getLazyViewModel() = viewModel<PlantAssistantChatViewModel>()
    override fun provideLayout() = R.layout.activity_plant_chat
    override fun isFitsSystemWindows() = false

    override fun initViews(savedInstanceState: Bundle?) {
        setupToolbar()
        setupSystemInsets()
        setupRecyclerView()
        setupInputArea()
        setupTts()

        val initialQuestion = intent.getStringExtra(EXTRA_INITIAL_QUESTION).orEmpty()
        val plantName = intent.getStringExtra(EXTRA_PLANT_NAME).orEmpty()

        if (savedInstanceState == null) {
            val welcome = if (plantName.isNotBlank()) {
                getString(R.string.thesis_ai_prompt_format, plantName)
            } else {
                getString(R.string.chat_welcome_message)
            }
            chatAdapter.addAssistantMessage(welcome, showSuggestions = initialQuestion.isBlank())

            if (initialQuestion.isBlank()) {
                viewBinding.rcvMessages.post {
                    val suggestions = PlantChatSuggestion.randomStarterSuggestions(this@PlantAssistantChatActivity)
                    chatAdapter.updateSuggestions(suggestions)
                    viewBinding.rcvMessages.post {
                        if (chatAdapter.itemCount > 0) {
                            chatAdapter.notifyItemContentChangedAt(0)
                        }
                    }
                }
            } else {
                viewBinding.rcvMessages.post {
                    if (!autoAsked) {
                        autoAsked = true
                        viewBinding.edtMessage.setText(initialQuestion)
                        sendMessage()
                    }
                }
            }
        }
    }

    override fun initAds(block: (suspend kotlinx.coroutines.CoroutineScope.() -> Unit)?) {
        super.initAds {
            adsManager.onLoadCollapseBanner(this@PlantAssistantChatActivity)
        }
    }

    override fun initObservers() {
        super.initObservers()
        collectQueuedUserMessages()
        collectRequestState()
    }

    override fun initListeners() {
        super.initListeners()
        setupSendAction()
    }

    private fun setupToolbar() {
        viewBinding.ivBack.setOnClickListener {
            showInterAdBeforeDoAction(kotlinx.coroutines.Runnable {
                onBackPressedDispatcher.onBackPressed()
            })
        }
    }

    override fun onHandleBackPressed(onBackPressed: OnPerformBackPressed?) =
        super.onHandleBackPressed {
            showInterAdBeforeDoAction(kotlinx.coroutines.Runnable { finish() })
        }

    private fun setupRecyclerView() {
        chatAdapter = PlantChatAdapter(
            onSuggestionClick = ::onSuggestionClick,
            onSpeakClick = ::speakText,
        )
        viewBinding.rcvMessages.apply {
            layoutManager = LinearLayoutManager(this@PlantAssistantChatActivity)
            adapter = chatAdapter
            setHasFixedSize(true)
            itemAnimator = null
            isNestedScrollingEnabled = false
        }
        chatAdapter.setRecyclerView(viewBinding.rcvMessages)
    }

    private fun setupInputArea() {
        viewBinding.edtMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_SEARCH) {
                sendMessage()
                true
            } else {
                false
            }
        }
    }

    private fun setupSendAction() {
        viewBinding.ivSendMessage.onDebounceClick {
            sendMessage()
        }
        viewBinding.ivVoiceInput.onDebounceClick {
            requestVoiceInput()
        }
    }

    private fun setupSystemInsets() {
        applyInsetToView { _, insets ->
            viewBinding.toolbar.setPadding(0, insets.statusBars(true).top, 0, 0)
            viewBinding.root.updateLayoutParams<MarginLayoutParams> {
                bottomMargin = insets.ime().bottom
            }
        }
    }

    private fun onSuggestionClick(suggestion: PlantChatSuggestion) {
        chatAdapter.hideSuggestions()
        viewBinding.edtMessage.setText(suggestion.messageToSend)
        viewBinding.edtMessage.setSelection(viewBinding.edtMessage.text?.length ?: 0)
        viewBinding.edtMessage.requestFocus()
        viewBinding.edtMessage.showKeyboard()
    }

    private fun sendMessage() {
        val displayMessage = viewBinding.edtMessage.text?.toString()?.trim() ?: ""
        if (displayMessage.isEmpty()) return

        val plantName = intent.getStringExtra(EXTRA_PLANT_NAME).orEmpty()
        val localPestReply = ChatPestIllustration.build(this, displayMessage, plantName)
        if (localPestReply == null && !isNetwork()) {
            showToast(R.string.no_internet)
            return
        }

        chatAdapter.hideSuggestions()
        viewBinding.edtMessage.text?.clear()
        viewBinding.edtMessage.hideKeyboard()
        viewModel.onState(
            PlantAssistantChatIntent.SubmitMessage(
                message = displayMessage,
                displayMessage = displayMessage,
                plantName = plantName,
            )
        )
    }

    private fun requestVoiceInput() {
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            launchVoiceInput()
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun launchVoiceInput() {
        showToast(R.string.chat_voice_listening)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "vi-VN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.chat_voice_listening))
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        runCatching { voiceLauncher.launch(intent) }
            .onFailure { showToast(R.string.chat_voice_unsupported) }
    }

    private fun setupTts() {
        textToSpeech = TextToSpeech(this) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (!ttsReady) return@TextToSpeech
            val vietnamese = Locale("vi", "VN")
            val result = textToSpeech?.setLanguage(vietnamese)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                textToSpeech?.language = Locale.getDefault()
            }
        }
    }

    private fun speakText(raw: String) {
        val spoken = raw
            .replace(Regex("[*_`#>-]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        if (spoken.isBlank()) return
        val tts = textToSpeech
        if (!ttsReady || tts == null) {
            showToast(R.string.chat_speak_unavailable)
            return
        }
        if (tts.isSpeaking) {
            tts.stop()
            return
        }
        tts.speak(spoken, TextToSpeech.QUEUE_FLUSH, null, "plant_chat")
    }

    private fun collectQueuedUserMessages() {
        collectLatestRepeatOnLifecycle(viewModel.queuedUserMessage) { message ->
            message?.let {
                chatAdapter.addUserMessage(message = it.displayMessage, imageUrl = null)
            }
        }
    }

    private fun collectRequestState() {
        collectLatestRepeatOnLifecycle(viewModel.requestResult) { result ->
            when (result) {
                is ResultFlow.Error -> {
                    setSendingState(false)
                    showToast(result.msg ?: getString(R.string.chat_unknown_error))
                }

                is ResultFlow.Loading -> {
                    chatAdapter.showTypingIndicator()
                    setSendingState(true)
                }

                is ResultFlow.Success -> {
                    val finalAnswer = result.data
                    val imageUrls = viewModel.replyImages.value
                    if (!finalAnswer.isNullOrBlank()) {
                        val lastIndex = chatAdapter.itemCount - 1
                        if (lastIndex >= 0) {
                            chatAdapter.updateAssistantMessageStreaming(
                                finalAnswer,
                                showSuggestions = false,
                                imageUrls = imageUrls,
                            )
                        }
                    }
                    setSendingState(false)
                }

                else -> Unit
            }
        }
    }

    private fun setSendingState(isSending: Boolean) {
        viewBinding.ivSendMessage.isEnabled = !isSending
        viewBinding.ivVoiceInput.isEnabled = !isSending
        viewBinding.edtMessage.isEnabled = !isSending

        val alpha = if (isSending) 0.5f else 1f
        viewBinding.ivSendMessage.alpha = alpha
        viewBinding.ivVoiceInput.alpha = alpha
        viewBinding.edtMessage.alpha = alpha
    }

    override fun onPause() {
        textToSpeech?.stop()
        super.onPause()
    }

    override fun onDestroy() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        super.onDestroy()
    }
}
