package com.expeditee.plant.identifier.presenter.dialog.rate_app

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.LinearLayoutManager
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.common.BaseDialogFragment
import com.expeditee.plant.identifier.databinding.DialogFeedbackBinding
import com.expeditee.plant.identifier.helper.AppConfigSettings
import com.expeditee.plant.identifier.utils.TrackingHelper

class FeedbackDialog(
    private val context: Context,
    private var onFeedbackSubmit: ((String) -> Unit)? = null
) : BaseDialogFragment<DialogFeedbackBinding>(DialogFeedbackBinding::inflate) {
    override val TAG: String
        get() = "FeedbackDialog"

    private val appConfigSettings = AppConfigSettings(context)
    private lateinit var feedbackAdapter: FeedbackAdapter
    private val feedbackItems = mutableListOf<FeedbackItem>()

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            val windowParams: WindowManager.LayoutParams = attributes
            windowParams.flags =
                windowParams.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv() or WindowManager.LayoutParams.FLAG_DIM_BEHIND
            windowParams.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            attributes = windowParams
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTouchOutsideListener()
        try {
            ViewCompat.setOnApplyWindowInsetsListener(view) { viewRoot, inset ->
                val ime = inset.getInsets(WindowInsetsCompat.Type.ime())
                viewRoot.updateLayoutParams<MarginLayoutParams> {
                    bottomMargin = ime.bottom
                }
                WindowInsetsCompat.CONSUMED
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun initViews() {
        setupFeedbackItems()
        setupRecyclerView()
        updateSubmitButton()
    }

    override fun initListeners() {
        onClickViews()
    }

    private fun setupFeedbackItems() {
        feedbackItems.clear()
        feedbackItems.addAll(
            listOf(
                FeedbackItem("appCrashes", R.drawable.ic_app_crash, context.getString(R.string.feedback_app_crashes)),
                FeedbackItem("appLoadsSlowly", R.drawable.ic_app_slow, context.getString(R.string.feedback_app_loads_slowly)),
                FeedbackItem("plantIdentificationInaccurate", R.drawable.ic_the_display, context.getString(R.string.feedback_plant_identification_inaccurate)),
                FeedbackItem("interfaceHardToUse", R.drawable.ic_the_map_interface, context.getString(R.string.feedback_interface_hard)),
                FeedbackItem("cantIdentifyPlant", R.drawable.ic_cant_find, context.getString(R.string.feedback_cant_identify_plant)),
                FeedbackItem("diseaseDiagnosisNotWork", R.drawable.ic_measure_distance, context.getString(R.string.feedback_disease_diagnosis_not_work)),
                FeedbackItem("tooManyAds", R.drawable.ic_many_ads, context.getString(R.string.feedback_too_many_ads)),
                FeedbackItem("others", R.drawable.ic_others, context.getString(R.string.feedback_others))
            )
        )
    }

    private fun setupRecyclerView() {
        feedbackAdapter = FeedbackAdapter { item ->
            TrackingHelper.logEvent(
                "feedback_option_select", bundleOf(
                    "Option_name" to item.text
                )
            )
            handleItemClick(item)
        }

        viewBinding.rvFeedbackOptions.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = feedbackAdapter
        }

        feedbackAdapter.updateItems(feedbackItems)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTouchOutsideListener() {
        viewBinding.root.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val activity = v.context as? Activity
                val view = activity?.currentFocus
                if (view is EditText) {
                    val outRect = Rect()
                    view.getGlobalVisibleRect(outRect)
                    if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                        view.clearFocus()
                        hideKeyboard()
                    }
                }
            }
            false
        }
    }

    private fun onClickViews() {
        viewBinding.etFeedback.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateSubmitButton()
            }
        })

        viewBinding.buttonSubmit.setOnClickListener {
            val feedbackData = collectFeedbackData()
            if (feedbackData.isNotEmpty()) {
                sendFeedbackEmail(feedbackData)
            }
        }
    }

    private fun handleItemClick(item: FeedbackItem) {
        val currentIndex = feedbackItems.indexOfFirst { it.id == item.id }
        if (currentIndex != -1) {
            val currentItem = feedbackItems[currentIndex]
            val updatedItem = currentItem.copy(isSelected = !currentItem.isSelected)
            feedbackItems[currentIndex] = updatedItem
            feedbackAdapter.updateItemSelection(item.id, updatedItem.isSelected)

            if (item.id == "others") {
                viewBinding.tilFeedback.isVisible = updatedItem.isSelected
                if (!updatedItem.isSelected) {
                    viewBinding.etFeedback.text?.clear()
                }
            }
            updateSubmitButton()
        }
    }

    private fun updateSubmitButton() {
        val hasAnySelection = feedbackItems.any { it.isSelected }
        val isOthersSelected = feedbackItems.find { it.id == "others" }?.isSelected == true
        val hasOthersText = viewBinding.etFeedback.text?.toString()?.trim()?.isNotEmpty() == true
        val isButtonEnabled = when {
            !hasAnySelection -> false
            isOthersSelected && !hasOthersText -> false
            else -> true
        }

        viewBinding.buttonSubmit.apply {
            isSelected = isButtonEnabled
            isEnabled = isButtonEnabled
            text = if (isOthersSelected && hasOthersText) getString(R.string.feedback_submit) else getString(R.string.feedback_dialog_button_send)
        }
    }

    private fun collectFeedbackData(): String {
        val feedbackList = mutableListOf<String>()

        feedbackItems.forEach { item ->
            if (item.isSelected) {
                if (item.id == "others") {
                    val othersText = viewBinding.etFeedback.text?.toString()?.trim()
                    if (!othersText.isNullOrEmpty()) {
                        feedbackList.add("Others: $othersText")
                    }
                } else {
                    feedbackList.add(item.text)
                }
            }
        }
        return feedbackList.joinToString("; ")
    }

    private fun sendFeedbackEmail(feedbackData: String) {
        viewBinding.buttonSubmit.apply {
            isEnabled = false
            text = context.getString(R.string.feedback_sending)
        }

        EmailService.sendFeedbackEmail(
            context = context,
            feedbackData = feedbackData,
            onSuccess = {
                appConfigSettings.hasRatedOrFeedback = true
                dismiss()
                showThankYouDialog()
                onFeedbackSubmit?.invoke(feedbackData)
            },
            onError = { error ->
                Toast.makeText(context, "Failed to send feedback: $error", Toast.LENGTH_LONG).show()
                viewBinding.buttonSubmit.apply {
                    isEnabled = true
                    text = context.getString(R.string.feedback_submit)
                }
            }
        )
    }

    private fun showThankYouDialog() {
        ThankYouDialog(context).show()
    }

    private fun hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(viewBinding.root.windowToken, 0)
    }
}

