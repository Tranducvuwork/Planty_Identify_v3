package com.expeditee.plant.identifier.presenter.dialog.rate_app

import android.content.Intent
import android.util.Log
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.common.BaseDialog
import com.expeditee.plant.identifier.databinding.DialogNewRateAppBinding
import com.expeditee.plant.identifier.helper.AppConfigSettings
import com.expeditee.plant.identifier.utils.TrackingHelper
import com.expeditee.plant.identifier.utils.Utils

class RateAppDialog(
    context: FragmentActivity,
    private val screenName: String? = ""
) : BaseDialog<DialogNewRateAppBinding>(context, R.style.BaseDialog) {

    private val activity: FragmentActivity = context
    private val fragmentManager by lazy { activity.supportFragmentManager }
    private val appConfigSettings = AppConfigSettings(context)

    private var currentRating: Int = 0

    override fun getLayoutDialog(): Int = R.layout.dialog_new_rate_app

    override fun initViews() {
        super.initViews()
        binding.apply {
            ratingBar.hideRatingStars()
            buttonRate.isSelected = false
            buttonRate.isEnabled = false
        }

        TrackingHelper.logEvent(
            "rate_popup_shown", bundleOf(
                "source" to screenName
            )
        )
        showInitialState()
        startAnimation()
    }

    override fun onStart() {
        super.onStart()
        val params = window?.attributes
        params?.width = (activity.resources.displayMetrics.widthPixels * 0.85f).toInt()
        window?.attributes = params
    }

    private fun startAnimation() {
        binding.ratingBar.showStarAnimation()
    }

    override fun onClickViews() {
        super.onClickViews()

        binding.apply {
            buttonRate.setOnClickListener {
                handleRateClick()
            }
            tvMaybeLate.setOnClickListener {
                dismiss()
            }
            ratingBar.onRateListener = { star ->
                currentRating = star
                updateUIForRating(star)
            }
        }

        setCanceledOnTouchOutside(true)
        setCancelable(true)
    }

    private fun handleRateClick() {
        if (currentRating == 0) return

        when {
            currentRating >= 4 -> {
                appConfigSettings.hasRatedOrFeedback = true
                activity.sendBroadcast(Intent("ACTION_RATED_APP"))
                Utils.openGooglePlayStore(activity)
                dismiss()
            }

            currentRating in 1..3 -> showFeedbackDialog()
        }
    }

    private fun showFeedbackDialog() {
        dismiss()
        activity.window?.decorView?.post {
            if (!activity.isFinishing && !activity.isDestroyed) {
                FeedbackDialog(activity).show(fragmentManager)
            }
        }
    }

    private fun showInitialState() {
        binding.apply {
            imgRateDialog.setImageResource(R.drawable.img_rate_dialog_defaut)
            tvRatingTitle.text = activity.getString(R.string.rate_dialog_title_initial)
            tvRatingSubtitle.text = activity.getString(R.string.rate_dialog_subtitle_initial)
            buttonRate.text = activity.getString(R.string.rate_button_text)
            tvMaybeLate.text = activity.getString(R.string.maybe_later)
            buttonRate.isSelected = false
            buttonRate.isEnabled = false
        }
    }

    private fun updateUIForRating(rating: Int) {
        when {
            rating >= 4 -> showPositiveState(rating)
            rating in 1..3 -> showNegativeState(rating)
            else -> showInitialState()
        }
    }

    private fun showPositiveState(rating: Int) {
        binding.apply {
            imgRateDialog.setImageResource(R.drawable.img_rate_dialog_positive)
            tvRatingTitle.text = activity.getString(R.string.rate_dialog_title_positive)
            tvRatingSubtitle.text = activity.getString(R.string.rate_dialog_subtitle_positive)
            buttonRate.text = activity.getString(R.string.rate_button_text)
            tvMaybeLate.text = activity.getString(R.string.maybe_later)
            buttonRate.isSelected = true
            buttonRate.isEnabled = true
        }
    }

    private fun showNegativeState(rating: Int) {
        binding.apply {
            imgRateDialog.setImageResource(R.drawable.img_rate_dialog_negative)
            tvRatingTitle.text = activity.getString(R.string.rate_dialog_title_negative)
            tvRatingSubtitle.text = activity.getString(R.string.rate_dialog_subtitle_negative)
            buttonRate.text = activity.getString(R.string.rate_button_text)
            tvMaybeLate.text = activity.getString(R.string.maybe_later)
            buttonRate.isSelected = true
            buttonRate.isEnabled = true
        }
    }
}

