package com.expeditee.plant.identifier.presenter.reminder

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.common.BaseActivity
import com.expeditee.plant.identifier.databinding.ActivityReminderBinding
import com.expeditee.plant.identifier.extension.beGone
import com.expeditee.plant.identifier.extension.beVisible
import com.expeditee.plant.identifier.extension.collectLatestRepeatOnLifecycle
import com.expeditee.plant.identifier.extension.loadImageWithGithub
import com.expeditee.plant.identifier.extension.onDebounceClick
import com.expeditee.plant.identifier.extension.show
import com.expeditee.plant.identifier.model.ReminderScheduleModel
import com.expeditee.plant.identifier.presenter.reminderall.ReminderAllActivity
import com.expeditee.plant.identifier.widget.bottomsheet.DeleteConfirmDialog
import com.expeditee.plant.identifier.widget.bottomsheet.EditReminderBottomSheetDialogFragment
import com.expeditee.plant.identifier.common.OnPerformBackPressed
import kotlinx.coroutines.CoroutineScope
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.concurrent.TimeUnit

class ReminderActivity : BaseActivity<ActivityReminderBinding, ReminderViewModel, ReminderState>() {

    private val adapter by lazy {
        ReminderAdapter(
            onEdit = { showEditReminder(it) },
            onAction = { handleReminderAction(it) },
            onDelete = { confirmDeleteReminder(it) },
        )
    }

    override fun isFitsSystemWindows(): Boolean = false

    override fun getLazyViewModel() = viewModel<ReminderViewModel>()

    override fun provideLayout(): Int = R.layout.activity_reminder

    override fun initAds(block: (suspend CoroutineScope.() -> Unit)?) {
        super.initAds {
            adsManager.onLoadCollapseBanner(this@ReminderActivity)
        }
    }

    override fun initViews(savedInstanceState: Bundle?) {
        // Notifications disabled — skip POST_NOTIFICATIONS / exact-alarm prompts
        with(viewBinding.rvTodaySchedule) {
            layoutManager = LinearLayoutManager(this@ReminderActivity)
            this.adapter = this@ReminderActivity.adapter
        }
        viewBinding.ivPremiumBadge.show(false)
    }

    override fun initObservers() {
        super.initObservers()
        collectLatestRepeatOnLifecycle(viewModel.reminders) { reminders ->
            adapter.submitList(reminders)
        }
        collectLatestRepeatOnLifecycle(viewModel.isEmpty) { isEmpty ->
            viewBinding.layoutEmpty.show(isEmpty)
            viewBinding.rvTodaySchedule.show(!isEmpty)
        }
        collectLatestRepeatOnLifecycle(viewModel.nextReminder) {
            updateNextReminder(it)
        }
    }

    override fun initListeners() {
        viewBinding.ivBack.onDebounceClick {
            showInterAdBeforeDoAction(Runnable { finish() })
        }
        viewBinding.tvSeeAll.onDebounceClick {
            ReminderAllActivity.start(this)
        }
        viewBinding.ivPremiumBadge.onDebounceClick {
            
        }
        viewBinding.btnAddReminder.onDebounceClick {
            showEditReminder(null)
        }
        viewBinding.cardNextReminder.onDebounceClick {
            viewModel.nextReminder.value?.let { showEditReminder(it) }
        }
    }

    override fun onHandleBackPressed(onBackPressed: OnPerformBackPressed?) =
        super.onHandleBackPressed {
            showInterAdBeforeDoAction(kotlinx.coroutines.Runnable { finish() })
        }

    private fun updateNextReminder(reminder: ReminderScheduleModel?) {
        with(viewBinding) {
            if (reminder != null) {
                cardNextReminder.beVisible()
                tvNextTime.text = reminder.remindTime
                tvNextPlantName.text = reminder.plantName
                tvNextRepeat.text = reminder.repeatType
                tvNextRemindType.text = reminder.remindType

                val currentTime = System.currentTimeMillis()
                val timeLeftMillis = reminder.nextDate - currentTime

                tvTimeLeft.text = when {
                    timeLeftMillis <= 0 -> "Now"
                    timeLeftMillis < 60000 -> "Starting soon"
                    else -> {
                        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeLeftMillis) % 60
                        val hours = TimeUnit.MILLISECONDS.toHours(timeLeftMillis)
                        if (hours > 0) "In $hours h $minutes min" else "In $minutes min"
                    }
                }


                if (reminder.imagePath != null) {
                    Glide.with(ivNextPlant).load(reminder.imagePath).into(ivNextPlant)
                } else if (reminder.imageRes != 0) {
                    ivNextPlant.loadImageWithGithub(reminder.imageRes)
                }
            } else {
                cardNextReminder.beGone()
            }
        }
    }

    private fun showEditReminder(reminder: ReminderScheduleModel?) {
        EditReminderBottomSheetDialogFragment.newInstance(reminder)
            .show(supportFragmentManager)
    }

    private fun handleReminderAction(reminder: ReminderScheduleModel) {
        viewModel.onState(ReminderState.CompleteReminderAction(reminder))
    }

    private fun confirmDeleteReminder(reminder: ReminderScheduleModel) {
        DeleteConfirmDialog(this) {
            viewModel.onState(ReminderState.DeleteReminder(reminder))
        }.show()
    }

    override fun onResume() {
        super.onResume()
        viewBinding.ivPremiumBadge.show(false)
    }
}
