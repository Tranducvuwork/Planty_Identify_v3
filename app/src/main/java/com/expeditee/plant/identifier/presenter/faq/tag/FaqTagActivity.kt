package com.expeditee.plant.identifier.presenter.faq.tag

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.core.text.buildSpannedString
import androidx.core.text.bold
import androidx.recyclerview.widget.LinearLayoutManager
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.common.BaseActivity
import com.expeditee.plant.identifier.databinding.ActivityFaqTagBinding
import com.expeditee.plant.identifier.extension.collectLatestRepeatOnLifecycle
import com.expeditee.plant.identifier.extension.statusBars
import com.expeditee.plant.identifier.presenter.faq.FaqAdapter
import com.expeditee.plant.identifier.presenter.faq.FaqDetailActivity
import org.koin.androidx.viewmodel.ext.android.viewModel

class FaqTagActivity : BaseActivity<ActivityFaqTagBinding, FaqTagViewModel, FaqTagState>() {

    companion object {
        private const val EXTRA_TAG = "extra_tag"

        fun start(activity: Activity, tag: String) {
            val intent = Intent(activity, FaqTagActivity::class.java).apply {
                putExtra(EXTRA_TAG, tag)
            }
            activity.startActivity(intent)
        }
    }

    private val tagName: String by lazy { intent?.getStringExtra(EXTRA_TAG).orEmpty() }

    override fun provideLayout() = R.layout.activity_faq_tag
    override fun getLazyViewModel() = viewModel<FaqTagViewModel>()
    override fun isFitsSystemWindows(): Boolean = false

    private val faqAdapter by lazy {
        FaqAdapter(
            onFaqClick = { FaqDetailActivity.start(this, it) },
            onTagClick = { start(this, it) }
        )
    }

    override fun initViews(savedInstanceState: Bundle?) {
        setupToolbar()
        setupRecyclerView()
        applyInset()
        viewModel.searchFaqsByTag(tagName)
    }

    private fun applyInset() {
        applyInsetToView { _, insets ->
            viewBinding.root.setPadding(0, insets.statusBars().top, 0, 0)
        }
    }

    override fun initObservers() {
        super.initObservers()
        collectLatestRepeatOnLifecycle(viewModel.faqsState) { state ->
            when (state) {
                is FaqTagState.Idle -> Unit
                is FaqTagState.Loading -> Unit
                is FaqTagState.Success -> {
                    viewBinding.textFaqTagCount.text = buildSpannedString {
                        append("${state.list.size} ")
                        bold { append(getString(R.string.faqs)) }
                        append(" ${getString(R.string.have_this_tag)}")
                    }
                    faqAdapter.submitList(state.list)
                }
            }
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(viewBinding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "#$tagName"
        }
        viewBinding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        viewBinding.collectionFaqTag.apply {
            layoutManager = LinearLayoutManager(this@FaqTagActivity)
            adapter = faqAdapter
        }
    }
}
