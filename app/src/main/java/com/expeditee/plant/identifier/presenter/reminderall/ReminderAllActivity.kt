package com.expeditee.plant.identifier.presenter.reminderall

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.common.BaseActivity
import com.expeditee.plant.identifier.databinding.ActivityReminderAllBinding
import com.expeditee.plant.identifier.extension.beGone
import com.expeditee.plant.identifier.extension.beVisible
import com.expeditee.plant.identifier.extension.collectLatestRepeatOnLifecycle
import com.expeditee.plant.identifier.extension.hideKeyboard
import com.expeditee.plant.identifier.extension.onDebounceClick
import com.expeditee.plant.identifier.extension.showKeyboard
import com.expeditee.plant.identifier.model.ReminderScheduleModel
import com.expeditee.plant.identifier.presenter.reminder.ReminderAdapter
import com.expeditee.plant.identifier.widget.bottomsheet.DeleteConfirmDialog
import com.expeditee.plant.identifier.widget.bottomsheet.EditReminderBottomSheetDialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class ReminderAllActivity :
    BaseActivity<ActivityReminderAllBinding, ReminderAllViewModel, ReminderAllState>() {

    private val adapter by lazy {
        ReminderAdapter(
            onEdit = { showEditReminder(it) },
            onAction = { handleReminderAction(it) },
            onDelete = { confirmDeleteReminder(it) },
        )
    }

    private var searchJob: Job? = null

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, ReminderAllActivity::class.java))
        }
    }

    override fun isFitsSystemWindows(): Boolean = false

    override fun getLazyViewModel() = viewModel<ReminderAllViewModel>()

    override fun provideLayout(): Int = R.layout.activity_reminder_all

    override fun initAds(block: (suspend CoroutineScope.() -> Unit)?) {
        super.initAds {
            adsManager.onLoadCollapseBanner(this@ReminderAllActivity)
        }
    }

    override fun initViews(savedInstanceState: Bundle?) {
        with(viewBinding.rvReminders) {
            layoutManager = LinearLayoutManager(this@ReminderAllActivity)
            this.adapter = this@ReminderAllActivity.adapter
        }
        viewBinding.etSearchReminders.requestFocus()
        viewBinding.etSearchReminders.showKeyboard()
    }

    override fun initListeners() {
        viewBinding.ivBack.onDebounceClick { finish() }
        viewBinding.root.onDebounceClick {
            viewBinding.etSearchReminders.hideKeyboard()
            viewBinding.etSearchReminders.clearFocus()
        }

        with(viewBinding.etSearchReminders) {
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    val query = text.toString().trim()
                    viewModel.onState(ReminderAllState.Search(query))
                    clearFocus()
                    hideKeyboard()
                }
                true
            }

            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    searchJob?.cancel()
                    searchJob = lifecycleScope.launch {
                        delay(300)
                        val query = s.toString().trim()
                        if (query.isEmpty()) {
                            viewModel.onState(ReminderAllState.ClearSearch)
                        } else {
                            viewModel.onState(ReminderAllState.Search(query))
                        }
                    }
                }

                override fun afterTextChanged(s: Editable?) {}
            })
        }
    }

    override fun initObservers() {
        super.initObservers()
        collectLatestRepeatOnLifecycle(viewModel.searchResults) { reminders ->
            adapter.submitList(reminders)
            if (reminders.isEmpty() && viewModel.currentQuery.value.isNotEmpty()) {
                viewBinding.llEmpty.visibility = View.VISIBLE
                viewBinding.rvReminders.visibility = View.GONE
            } else {
                viewBinding.llEmpty.visibility = View.GONE
                viewBinding.rvReminders.visibility = View.VISIBLE
            }
        }
    }

    private fun showEditReminder(reminder: ReminderScheduleModel) {
        EditReminderBottomSheetDialogFragment.newInstance(reminder)
            .show(supportFragmentManager)
    }

    private fun handleReminderAction(reminder: ReminderScheduleModel) {
        viewModel.onState(ReminderAllState.CompleteReminderAction(reminder))
    }

    private fun confirmDeleteReminder(reminder: ReminderScheduleModel) {
        DeleteConfirmDialog(this) {
            viewModel.onState(ReminderAllState.DeleteReminder(reminder))
        }.show()
    }

    override fun onBillingPurchaseListener(hasPurchase: Boolean) {
        super.onBillingPurchaseListener(hasPurchase)
        if (true) {
            viewBinding.frBanner.removeAllViews()
            viewBinding.ivPremiumBadge.beGone()
        }
        lifecycleScope.launch {
            if (hasPurchase) {
                viewBinding.frBanner.beGone()
            } else {
                viewBinding.frBanner.beVisible()
            }
        }
    }

}
