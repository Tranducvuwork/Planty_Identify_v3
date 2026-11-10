package com.expeditee.plant.identifier.presenter.home

import android.content.Intent
import android.net.Uri
import android.widget.FrameLayout
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.common.BaseFragment
import com.expeditee.plant.identifier.databinding.FragmentMoreTabBinding
import com.expeditee.plant.identifier.extension.onClickWithScale
import com.expeditee.plant.identifier.extension.show
import com.expeditee.plant.identifier.extension.startActivity
import com.expeditee.plant.identifier.presenter.careguide.CareGuidesActivity
import com.expeditee.plant.identifier.presenter.dialog.rate_app.RateAppDialog
import com.expeditee.plant.identifier.presenter.disease_list.DiseaseListActivity
import com.expeditee.plant.identifier.presenter.faq.FaqListActivity
import com.expeditee.plant.identifier.presenter.home.more.MoreOptionAdapter
import com.expeditee.plant.identifier.presenter.home.more.MoreOptionItem
import com.expeditee.plant.identifier.presenter.home.more.MoreOptionType
import com.expeditee.plant.identifier.presenter.language.LanguageActivity
import com.expeditee.plant.identifier.thesis.FolkRemediesActivity
import com.expeditee.plant.identifier.thesis.PlantDiaryListActivity
import com.expeditee.plant.identifier.utils.AppUtils
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class MoreTabFragment : BaseFragment<FragmentMoreTabBinding, HomeViewModel, HomeState>(
    inflate = { layoutInflater, viewGroup, attachToParent ->
        DataBindingUtil.inflate(
            layoutInflater,
            R.layout.fragment_more_tab,
            viewGroup,
            attachToParent,
        )
    },
) {
    private val moreOptionAdapter by lazy { MoreOptionAdapter() }
    private val premiumHeaderCollapsedDp = 130
    private val titleMarginTopWhenPremiumHiddenDp = 55

    override fun getLazyViewModel() = activityViewModel<HomeViewModel>()

    override fun initViews() {
        setupHorizontalRecyclerView()
        updatePremiumSectionUI()
        bindChildSafetySwitches()
    }

    private fun bindChildSafetySwitches() {
        viewBinding.switchChildSafety.isChecked = appConfigSettings.childSafetyEnabled
        viewBinding.switchAlertSound.isChecked = appConfigSettings.childSafetySound
        viewBinding.switchAlertRed.isChecked = appConfigSettings.childSafetyRed
        viewBinding.switchAlertGuide.isChecked = appConfigSettings.childSafetyGuide
        viewBinding.switchLockShare.isChecked = appConfigSettings.childSafetyLockShare
        fun syncChildrenEnabled(enabled: Boolean) {
            listOf(
                viewBinding.switchAlertSound,
                viewBinding.switchAlertRed,
                viewBinding.switchAlertGuide,
                viewBinding.switchLockShare,
            ).forEach {
                it.isEnabled = enabled
                it.alpha = if (enabled) 1f else 0.45f
            }
        }
        syncChildrenEnabled(appConfigSettings.childSafetyEnabled)
        viewBinding.switchChildSafety.setOnCheckedChangeListener { _, checked ->
            appConfigSettings.childSafetyEnabled = checked
            syncChildrenEnabled(checked)
            if (checked) {
                viewBinding.switchAlertSound.isChecked = true
                viewBinding.switchAlertRed.isChecked = true
                viewBinding.switchAlertGuide.isChecked = true
                appConfigSettings.childSafetySound = true
                appConfigSettings.childSafetyRed = true
                appConfigSettings.childSafetyGuide = true
            }
        }
        viewBinding.switchAlertSound.setOnCheckedChangeListener { _, checked ->
            appConfigSettings.childSafetySound = checked
        }
        viewBinding.switchAlertRed.setOnCheckedChangeListener { _, checked ->
            appConfigSettings.childSafetyRed = checked
        }
        viewBinding.switchAlertGuide.setOnCheckedChangeListener { _, checked ->
            appConfigSettings.childSafetyGuide = checked
        }
        viewBinding.switchLockShare.setOnCheckedChangeListener { _, checked ->
            appConfigSettings.childSafetyLockShare = checked
        }
    }

    private fun setupHorizontalRecyclerView() {
        viewBinding.rvMoreOptions.apply {
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = moreOptionAdapter
        }
        loadMoreOptionItems()
    }

    private fun loadMoreOptionItems() {
        val items = listOf(
            MoreOptionItem(
                type = MoreOptionType.PLANT_FAQ,
                icon = R.drawable.ic_explore_nav_more,
                title = getString(R.string.plant_faq),
                subtitle = getString(R.string.answer_your_questions_about_plants),
                backgroundDrawable = R.drawable.bg_more_option_white,
                titleColor = R.color.primary_green,
                subtitleColor = R.color.color9CB68D
            ), MoreOptionItem(
                type = MoreOptionType.CARE_GUIDES,
                icon = R.drawable.ic_more_nav,
                title = getString(R.string.care_guides),
                subtitle = getString(R.string.take_care_of_our_plants_properly),
                backgroundDrawable = R.drawable.bg_more_option_white,
                titleColor = R.color.primary_green,
                subtitleColor = R.color.color9CB68D
            ), MoreOptionItem(
                type = MoreOptionType.DISEASE,
                icon = R.drawable.ic_diagnose_home,
                title = getString(R.string.disease),
                subtitle = getString(R.string.spot_and_manage_plant_health_issues),
                backgroundDrawable = R.drawable.bg_more_option_white,
                titleColor = R.color.primary_green,
                subtitleColor = R.color.color9CB68D
            ), MoreOptionItem(
                type = MoreOptionType.FOLK_REMEDY,
                icon = R.drawable.ic_explore_nav_more,
                title = getString(R.string.thesis_folk_title),
                subtitle = getString(R.string.thesis_folk_sub),
                backgroundDrawable = R.drawable.bg_more_option_white,
                titleColor = R.color.primary_green,
                subtitleColor = R.color.color9CB68D
            ), MoreOptionItem(
                type = MoreOptionType.PLANT_DIARY,
                icon = R.drawable.ic_more_nav,
                title = getString(R.string.thesis_diary_title),
                subtitle = getString(R.string.thesis_diary_sub),
                backgroundDrawable = R.drawable.bg_more_option_white,
                titleColor = R.color.primary_green,
                subtitleColor = R.color.color9CB68D
            )
        )
        moreOptionAdapter.submitData(items)
    }

    override fun initListeners() {
        viewBinding.apply {
            llLanguage.setOnClickListener {
                val intent = Intent(requireContext(), LanguageActivity::class.java).apply {
                    putExtra(LanguageActivity.EXTRA_SCREEN, HomeActivity::class.java)
                }
                startActivity(intent)
            }
            llShare.onClickWithScale {
                AppUtils.shareApp(requireContext())
            }
            llRateApp.onClickWithScale {
                RateAppDialog(requireActivity()).show()
            }
            moreOptionAdapter.onItemClickListener = { item ->
                when (item.type) {
                    MoreOptionType.CARE_GUIDES -> {
                        requireContext().startActivity<CareGuidesActivity>()
                    }

                    MoreOptionType.PLANT_FAQ -> {
                        startActivity(Intent(requireContext(), FaqListActivity::class.java))
                    }

                    MoreOptionType.DISEASE -> {
                        startActivity(Intent(requireContext(), DiseaseListActivity::class.java))
                    }

                    MoreOptionType.FOLK_REMEDY -> {
                        FolkRemediesActivity.start(requireContext())
                    }

                    MoreOptionType.PLANT_DIARY -> {
                        PlantDiaryListActivity.start(requireContext())
                    }

                    else -> {}
                }
            }
        }
    }


    private fun updatePremiumSectionUI() {
        val shouldShowPremium = false
        viewBinding.ivPremiumBadge.show(false)
        viewBinding.clPremium.show(false)
        viewBinding.headerContainer.minimumHeight = premiumHeaderCollapsedDp.dpToPx()
        (viewBinding.llTitle.layoutParams as? FrameLayout.LayoutParams)?.let { params ->
            params.topMargin = titleMarginTopWhenPremiumHiddenDp.dpToPx()
            viewBinding.llTitle.layoutParams = params
        }
    }

    private fun Int.dpToPx(): Int {
        return (this * requireContext().resources.displayMetrics.density).toInt()
    }

    override fun onBillingPurchaseListener(hasPurchase: Boolean) {
        updatePremiumSectionUI()
    }
}
