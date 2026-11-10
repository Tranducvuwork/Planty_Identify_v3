package com.expeditee.plant.identifier.presenter.faq

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.core.text.buildSpannedString
import androidx.core.text.bold
import com.expeditee.plant.identifier.BuildConfig
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.common.BaseActivity
import com.expeditee.plant.identifier.common.BaseViewModel
import com.expeditee.plant.identifier.common.CommonViewModel
import com.expeditee.plant.identifier.databinding.ActivityFaqDetailBinding
import com.expeditee.plant.identifier.extension.beGone
import com.expeditee.plant.identifier.extension.loadImageWithGithub
import com.expeditee.plant.identifier.extension.statusBars
import com.expeditee.plant.identifier.model.Faq
import com.expeditee.plant.identifier.presenter.faq.tag.FaqTagActivity
import kotlinx.coroutines.CoroutineScope
import org.koin.androidx.viewmodel.ext.android.viewModel

class FaqDetailActivity :
    BaseActivity<ActivityFaqDetailBinding, CommonViewModel, BaseViewModel.State>() {

    companion object {
        private const val EXTRA_FAQ = "extra_faq"

        fun start(activity: Activity, faq: Faq) {
            val intent = Intent(activity, FaqDetailActivity::class.java).apply {
                putExtra(EXTRA_FAQ, faq)
            }
            activity.startActivity(intent)
        }
    }

    private val faq: Faq? by lazy { intent.getParcelableExtra(EXTRA_FAQ) }

    override fun provideLayout() = R.layout.activity_faq_detail
    override fun getLazyViewModel() = viewModel<CommonViewModel>()
    override fun isFitsSystemWindows(): Boolean = false

    override fun initViews(savedInstanceState: Bundle?) {
        setupToolbar()
        displayFaqDetails()
        applyInset()
    }

    override fun initAds(block: (suspend CoroutineScope.() -> Unit)?) {
        super.initAds {
            adsManager.consumeNativeAd(this@FaqDetailActivity,viewBinding.nativeAdView)
        }
    }

    override fun onBillingPurchaseListener(hasPurchase: Boolean) {
        super.onBillingPurchaseListener(hasPurchase)
        if (!hasPurchase) return
        viewBinding.nativeAdView.removeAllViews()
        viewBinding.nativeAdView.beGone()
    }

    override fun initListeners() {
        super.initListeners()
        viewBinding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupToolbar() {
        setSupportActionBar(viewBinding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(R.string.faq_detail)
        }
    }

    private fun applyInset() {
        applyInsetToView { _, insets ->
            viewBinding.root.setPadding(0, insets.statusBars().top, 0, 0)
        }
    }

    private fun displayFaqDetails() {
        val faqData = faq ?: return
        with(viewBinding) {
            tvQuestion.text = faqData.question
            tvAnswer.text = faqData.answer
            if (faqData.imageUrl.isBlank()) {
                ivFaqImage.setImageResource(R.drawable.ic_placeholder_loading)
            } else {
                ivFaqImage.loadImageWithGithub(faqData.imageUrl)
            }
            tvTags.text = buildSpannedString {
                faqData.tags.forEachIndexed { index, tag ->
                    bold { append("#$tag") }
                    if (index < faqData.tags.lastIndex) append(" ")
                }
            }
            tvTags.setOnClickListener {
                faqData.tags.firstOrNull()?.let { tag ->
                    FaqTagActivity.start(this@FaqDetailActivity, tag)
                }
            }
        }
    }
}
