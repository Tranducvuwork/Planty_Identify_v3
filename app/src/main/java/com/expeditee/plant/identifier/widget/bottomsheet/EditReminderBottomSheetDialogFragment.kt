package com.expeditee.plant.identifier.widget.bottomsheet

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.common.BaseBottomSheetDialogFragment
import com.expeditee.plant.identifier.databinding.DialogEditReminderBottomSheetBinding
import com.expeditee.plant.identifier.extension.onClickWithScale
import com.expeditee.plant.identifier.model.ReminderScheduleModel
import com.expeditee.plant.identifier.presenter.camera.ScanCameraActivity
import com.expeditee.plant.identifier.presenter.home.search.SearchActivity
import com.expeditee.plant.identifier.presenter.reminder.ReminderState
import com.expeditee.plant.identifier.presenter.reminder.ReminderViewModel
import com.expeditee.plant.identifier.widget.adapter.PreviousFertilizingDateAdapter
import com.expeditee.plant.identifier.widget.adapter.ReminderTypePickerAdapter
import com.expeditee.plant.identifier.widget.adapter.ReminderTypePickerItem
import com.google.android.material.datepicker.MaterialDatePicker
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone


class EditReminderBottomSheetDialogFragment :
    BaseBottomSheetDialogFragment<DialogEditReminderBottomSheetBinding>(
        DialogEditReminderBottomSheetBinding::inflate
    ) {

    override val TAG: String = "EditReminderBottomSheetDialogFragment"

    private val viewModel by viewModel<ReminderViewModel>()
    private var reminder: ReminderScheduleModel? = null

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    private val reminderTypeRows by lazy {
        listOf(
            ReminderTypePickerItem(getString(R.string.reminder_misting), R.drawable.ic_misting),
            ReminderTypePickerItem(
                getString(R.string.reminder_watering), R.drawable.ic_reminder_watering
            ),
            ReminderTypePickerItem(getString(R.string.rotating), R.drawable.ic_rotating),
        )
    }

    private val repeatUnits = listOf("DAYS", "WEEKS", "MONTHS")

    private var selectedReminderTypeIndex = 1
    private var selectedRepeatValue = 1
    private var selectedRepeatUnitIndex = 0
    private var selectedHour = 9
    private var selectedMinute = 30
    private var selectedPlantId = 0
    private var selectedPlantName = ""

    private var hasUserInteracted = false
    private var plantSelectionCommitted = false
    private var isRepeatSelected = false

    private var reminderTypeAdapter: ReminderTypePickerAdapter? = null
    private var previousFertilizingAdapter: PreviousFertilizingDateAdapter? = null

    private var selectedQuickLastDate: QuickLastDate = QuickLastDate.TODAY
    private var customLastDateFormatted: String? = null

    private enum class QuickLastDate { TODAY, YESTERDAY }

    companion object {
        private const val ARG_REMINDER = "arg_reminder"
        private const val REMINDER_TYPE_ITEM_HEIGHT_DP = 50

        fun newInstance(reminder: ReminderScheduleModel?): EditReminderBottomSheetDialogFragment {
            return EditReminderBottomSheetDialogFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_REMINDER, reminder)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()

        val bottomSheet =
            dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.post {
            val behavior =
                com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet)

            bottomSheet.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            bottomSheet.requestLayout()

            val screenHeight = resources.displayMetrics.heightPixels
            val maxHeight = (screenHeight * 0.90f).toInt()

            behavior.peekHeight = maxHeight
            behavior.maxHeight = maxHeight
            behavior.state =
                com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
            behavior.isDraggable = false
        }
    }

    override fun initViews() {
        reminder = arguments?.getSerializable(ARG_REMINDER) as? ReminderScheduleModel
        viewModel.onState(ReminderState.InitReminderEditor(reminder))
        setupInitialValues()
        setupAllPickers()
        updateAllSummaries()
        hasUserInteracted = false
    }

    override fun initObservers() = Unit

    override fun initListeners() {
        with(viewBinding) {
            ivCloseSheet.onClickWithScale { dismissAllowingStateLoss() }
            tvSelectPlant.onClickWithScale { showPlantSelectionDialog() }
            containerReminderType.onClickWithScale { toggleVisibility(pickerContainer) }
            containerRepeat.onClickWithScale { toggleVisibility(pickerContainerRepeat) }
            containerTime.onClickWithScale { toggleVisibility(pickerContainerTime) }
            containerPreviousFertilizing.onClickWithScale {
                toggleVisibility(pickerContainerPreviousFertilizing)
            }
            tvPreviousFertilizingSelect.onClickWithScale { showCustomDatePicker() }
            btnTurnOnReminder.onClickWithScale { handleSaveReminder() }
        }
    }

    private fun setupInitialValues() {
        val isEditMode = reminder != null && reminder?.id != 0
        viewBinding.tvEditTitle.text = if (isEditMode) {
            getString(R.string.reminder_edit_reminder_title)
        } else {
            getString(R.string.reminder_set_reminder_title)
        }

        selectedPlantId = reminder?.plantId ?: 0
        selectedPlantName = reminder?.plantName.orEmpty()
        viewBinding.tvSelectPlant.text = selectedPlantName.ifBlank {
            getString(R.string.reminder_select_plant)
        }

        val initialReminder = reminder
        plantSelectionCommitted = when {
            selectedPlantName.isBlank() -> false
            initialReminder != null && (initialReminder.plantId != 0 || !initialReminder.plantName.isNullOrBlank()) -> true

            else -> false
        }

        selectedReminderTypeIndex =
            reminderTypeRows.indexOfFirst { it.label == viewModel.remindType.value }
                .takeIf { it >= 0 } ?: 1

        val initialRepeatType = reminder?.repeatType
        isRepeatSelected = !initialRepeatType.isNullOrBlank()
        parseRepeatType(initialRepeatType ?: getString(R.string.reminder_every_day))

        parseTime(viewModel.remindTime.value)
        parseLastDateFromReminder(reminder?.lastDate)
    }

    private fun setupAllPickers() {
        setupReminderTypePicker()
        setupRepeatPicker()
        setupTimePicker()
        setupPreviousFertilizingPicker()
    }

    private fun setupReminderTypePicker() {
        val recyclerView = viewBinding.rvReminderType
        val layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
        recyclerView.layoutManager = layoutManager
        recyclerView.setHasFixedSize(true)
        recyclerView.itemAnimator = null

        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(recyclerView)

        reminderTypeAdapter = ReminderTypePickerAdapter { index ->
            selectedReminderTypeIndex = index
            updateReminderTypeSummary()
            syncViewModelFromUi()
        }.also { adapter ->
            adapter.submitList(reminderTypeRows)
            recyclerView.adapter = adapter
        }

        recyclerView.post {
            val offset = (recyclerView.height / 2) - (dpToPx(REMINDER_TYPE_ITEM_HEIGHT_DP) / 2)
            layoutManager.scrollToPositionWithOffset(selectedReminderTypeIndex, offset)
            reminderTypeAdapter?.setSelectedPosition(selectedReminderTypeIndex)
        }

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val centerView = snapHelper.findSnapView(layoutManager) ?: return
                    val position = layoutManager.getPosition(centerView)
                    if (position != RecyclerView.NO_POSITION) {
                        selectedReminderTypeIndex = position
                        reminderTypeAdapter?.setSelectedPosition(position)
                        updateReminderTypeSummary()
                        syncViewModelFromUi()
                    }
                }
            }
        })
    }

    private fun setupPreviousFertilizingPicker() {
        val recyclerView = viewBinding.rvPreviousFertilizing
        val layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
        recyclerView.layoutManager = layoutManager
        recyclerView.setHasFixedSize(true)
        recyclerView.itemAnimator = null

        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(recyclerView)

        val items = listOf(
            getString(R.string.reminder_today),
            getString(R.string.reminder_yesterday),
        )

        previousFertilizingAdapter = PreviousFertilizingDateAdapter(
            context = requireContext(),
            items = items,
            rowHeightPx = dpToPx(44),
        ) { index ->
            selectedQuickLastDate = if (index == 0) QuickLastDate.TODAY else QuickLastDate.YESTERDAY
            customLastDateFormatted = null
            previousFertilizingAdapter?.setSelectedPosition(index)
            updatePreviousFertilizingSummary()
            syncViewModelFromUi()
        }
        recyclerView.adapter = previousFertilizingAdapter

        val startIndex = if (selectedQuickLastDate == QuickLastDate.TODAY) 0 else 1
        recyclerView.post {
            val offset = (recyclerView.height / 2) - (dpToPx(44) / 2)
            layoutManager.scrollToPositionWithOffset(startIndex, offset)
            previousFertilizingAdapter?.setSelectedPosition(startIndex)
        }

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val centerView = snapHelper.findSnapView(layoutManager) ?: return
                    val position = layoutManager.getPosition(centerView)
                    if (position != RecyclerView.NO_POSITION) {
                        selectedQuickLastDate =
                            if (position == 0) QuickLastDate.TODAY else QuickLastDate.YESTERDAY
                        customLastDateFormatted = null
                        previousFertilizingAdapter?.setSelectedPosition(position)
                        updatePreviousFertilizingSummary()
                        syncViewModelFromUi()
                    }
                }
            }
        })
    }

    private fun setupRepeatPicker() {
        setupRepeatValuePicker()
        setupRepeatUnitPicker()
    }

    private fun setupRepeatValuePicker() {
        val picker = viewBinding.numberPickerRepeatValue
        val maxValue = getMaxRepeatValue()
        picker.minValue = 1
        picker.maxValue = maxValue
        picker.wrapSelectorWheel = true
        picker.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
        picker.value = selectedRepeatValue.coerceIn(1, maxValue)
        picker.setOnValueChangedListener { _, _, newValue ->
            selectedRepeatValue = newValue
            isRepeatSelected = true
            updateRepeatSummary()
            syncViewModelFromUi()
        }
    }

    private fun setupRepeatUnitPicker() {
        val picker = viewBinding.numberPickerRepeatUnit
        picker.minValue = 0
        picker.maxValue = repeatUnits.lastIndex
        picker.displayedValues = arrayOf("Days", "Weeks", "Months")
        picker.wrapSelectorWheel = true
        picker.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
        picker.value = selectedRepeatUnitIndex
        picker.setOnValueChangedListener { _, _, newValue ->
            selectedRepeatUnitIndex = newValue
            val maxValue = getMaxRepeatValue()
            if (selectedRepeatValue > maxValue) {
                selectedRepeatValue = maxValue
            }
            viewBinding.numberPickerRepeatValue.maxValue = maxValue
            viewBinding.numberPickerRepeatValue.value = selectedRepeatValue
            isRepeatSelected = true
            updateRepeatSummary()
            syncViewModelFromUi()
        }
    }

    private fun setupTimePicker() {
        setupTimeNumberPicker(viewBinding.numberPickerHour, 0, 23, selectedHour) {
            selectedHour = it
            updateTimeDisplay()
            syncViewModelFromUi()
        }
        setupTimeNumberPicker(viewBinding.numberPickerMinute, 0, 59, selectedMinute) {
            selectedMinute = it
            updateTimeDisplay()
            syncViewModelFromUi()
        }
    }

    private fun setupTimeNumberPicker(
        picker: com.shawnlin.numberpicker.NumberPicker,
        minValue: Int,
        maxValue: Int,
        value: Int,
        onChanged: (Int) -> Unit,
    ) {
        picker.minValue = minValue
        picker.maxValue = maxValue
        picker.value = value.coerceIn(minValue, maxValue)
        picker.wrapSelectorWheel = true
        picker.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
        picker.setFormatter { String.format(Locale.getDefault(), "%02d", it) }
        picker.setOnValueChangedListener { _, _, newValue -> onChanged(newValue) }
    }

    private fun updateAllSummaries() {
        updateReminderTypeSummary()
        updateRepeatSummary()
        updateTimeDisplay()
        updatePreviousFertilizingSummary()
        updateTurnOnButtonStyle()
    }

    private fun updateReminderTypeSummary() {
        viewBinding.tvReminderTypeSummary.text = reminderTypeRows[selectedReminderTypeIndex].label
    }

    private fun updateRepeatSummary() {
        if (isRepeatSelected) {
            viewBinding.tvRepeatSummary.text = getRepeatSummaryText()
        } else {
            viewBinding.tvRepeatSummary.text = getString(R.string.reminder_select)
        }
    }

    private fun updateTimeDisplay() {
        viewBinding.tvTimePicker.text =
            String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute)
    }

    private fun updatePreviousFertilizingSummary() {
        val summaryText = customLastDateFormatted ?: resolveQuickLastDateLabel()
        viewBinding.tvPreviousFertilizingSelect.text = summaryText
    }

    private fun resolveQuickLastDateLabel(): String {
        return if (selectedQuickLastDate == QuickLastDate.TODAY) {
            getString(R.string.reminder_today)
        } else {
            getString(R.string.reminder_yesterday)
        }
    }

    private fun toggleVisibility(target: View) {
        val views = listOf(
            viewBinding.pickerContainer,
            viewBinding.pickerContainerRepeat,
            viewBinding.pickerContainerTime,
            viewBinding.pickerContainerPreviousFertilizing,
        )
        views.forEach { view ->
            view.visibility = if (view == target && !view.isVisible) View.VISIBLE else View.GONE
        }
    }

    private fun showPlantSelectionDialog() {
        if (viewModel.reminders.value.isEmpty()) {
            SelectPlantBottomSheetDialog.newInstance().setOnIdentifyByPhotoListener { gardenId ->
                ScanCameraActivity.start(requireContext(), gardenId = gardenId)
            }.setOnSearchByNameListener { gardenId ->
                SearchActivity.start(requireContext(), gardenId = gardenId)
            }.show(childFragmentManager, "SelectPlantDialog")
        } else {
            SelectPlantBottomSheetDialogFragment.newInstance(selectedPlantId)
                .setOnPlantSelected { plant ->
                    selectedPlantId = plant.id.toIntOrNull() ?: 0
                    selectedPlantName = plant.name
                    plantSelectionCommitted = true
                    viewBinding.tvSelectPlant.text = selectedPlantName
                    hasUserInteracted = true
                    updateTurnOnButtonStyle()
                    viewModel.onState(ReminderState.SelectPlant(selectedPlantId, selectedPlantName))
                }.show(childFragmentManager)
        }
    }

    private fun showCustomDatePicker() {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.reminder_previous_fertilizing))
            .setTheme(R.style.AppMaterialCalendar).build()

        picker.addOnPositiveButtonClickListener { selectionUtc ->
            val millis = selectionUtc + TimeZone.getDefault().getOffset(selectionUtc)
            customLastDateFormatted = dateFormat.format(Date(millis))
            selectedQuickLastDate = QuickLastDate.TODAY
            hasUserInteracted = true
            updatePreviousFertilizingSummary()
            syncViewModelFromUi()
            viewBinding.pickerContainerPreviousFertilizing.visibility = View.GONE
        }

        picker.show(childFragmentManager, "")
    }

    private fun handleSaveReminder() {
        if (selectedPlantName.isBlank()) {
            showError(getString(R.string.reminder_please_select_plant))
            return
        }

        syncViewModelFromUi()

        if (!viewModel.isDataValid) {
            showError(getString(R.string.scan_processing_error_title))
            return
        }

        viewModel.onState(ReminderState.SaveCurrentEditor(reminder))
        dismissAllowingStateLoss()
    }

    private fun syncViewModelFromUi() {
        val remindType = reminderTypeRows[selectedReminderTypeIndex].label
        val repeatType = getRepeatSummaryText()
        val remindTime =
            String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute)
        val lastDate = resolveLastDateForStorage()

        hasUserInteracted = true
        viewModel.onState(
            ReminderState.UpdateEditorFields(
                plantId = selectedPlantId,
                plantName = selectedPlantName,
                remindType = remindType,
                repeatType = repeatType,
                remindTime = remindTime,
                lastDate = lastDate
            )
        )
        updateTurnOnButtonStyle()
    }


    private fun updateTurnOnButtonStyle() {
        val isEditMode = reminder != null && reminder?.id != 0
        val complete = isReminderFormComplete()
        val inFuture = isRemindTimeInFuture()

        val colorRes = when {
            !complete -> R.color.reminder_button_default
            isEditMode && (!hasUserInteracted || !inFuture) -> R.color.reminder_red
            else -> R.color.reminder_button_turn_on
        }

        val textRes = when {
            isEditMode && (!hasUserInteracted || !inFuture) -> R.string.reminder_turn_off
            else -> R.string.reminder_turn_on
        }

        viewBinding.btnTurnOnReminder.setText(textRes)
        applyTurnOnButtonBackground(colorRes)
    }

    private fun isReminderFormComplete(): Boolean =
        plantSelectionCommitted && selectedPlantName.isNotBlank()

    private fun isRemindTimeInFuture(): Boolean {
        val lastDateStr = resolveLastDateForStorage()
        val lastDate = runCatching { dateFormat.parse(lastDateStr) }.getOrNull() ?: return false

        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            time = lastDate
            set(Calendar.HOUR_OF_DAY, selectedHour)
            set(Calendar.MINUTE, selectedMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return target.timeInMillis > now.timeInMillis
    }


    private fun applyTurnOnButtonBackground(colorRes: Int) {
        val radius = 28f * resources.displayMetrics.density
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(ContextCompat.getColor(requireContext(), colorRes))
        }
        viewBinding.btnTurnOnReminder.background = drawable
    }

    private fun resolveLastDateForStorage(): String {
        customLastDateFormatted?.let { return it }
        val cal = Calendar.getInstance()
        if (selectedQuickLastDate == QuickLastDate.YESTERDAY) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return dateFormat.format(cal.time)
    }

    private fun parseLastDateFromReminder(raw: String?) {
        if (raw.isNullOrBlank()) {
            selectedQuickLastDate = QuickLastDate.TODAY
            customLastDateFormatted = null
            return
        }
        val todayLabel = getString(R.string.reminder_today)
        val yesterdayLabel = getString(R.string.reminder_yesterday)
        when {
            raw.equals(todayLabel, ignoreCase = true) -> {
                selectedQuickLastDate = QuickLastDate.TODAY
                customLastDateFormatted = null
            }

            raw.equals(yesterdayLabel, ignoreCase = true) -> {
                selectedQuickLastDate = QuickLastDate.YESTERDAY
                customLastDateFormatted = null
            }

            raw.contains("/") -> {
                customLastDateFormatted = raw
            }

            else -> {
                val parsed = runCatching { dateFormat.parse(raw) }.getOrNull()
                if (parsed != null) {
                    customLastDateFormatted = dateFormat.format(parsed)
                } else {
                    selectedQuickLastDate = QuickLastDate.TODAY
                    customLastDateFormatted = null
                }
            }
        }
    }

    private fun getRepeatSummaryText(): String {
        if (selectedRepeatUnitIndex == 0 && selectedRepeatValue == 1) return getString(R.string.reminder_every_day)
        if (selectedRepeatUnitIndex == 0 && selectedRepeatValue == 7) return getString(R.string.reminder_every_7_days)
        if (selectedRepeatUnitIndex == 0 && selectedRepeatValue == 14) return getString(R.string.reminder_every_14_days)

        val unitText = when (repeatUnits[selectedRepeatUnitIndex]) {
            "DAYS" -> if (selectedRepeatValue == 1) "day" else "days"
            "WEEKS" -> if (selectedRepeatValue == 1) "week" else "weeks"
            else -> if (selectedRepeatValue == 1) "month" else "months"
        }
        return "Every $selectedRepeatValue $unitText"
    }

    private fun parseRepeatType(repeatType: String) {
        val value = Regex("(\\d+)").find(repeatType)?.groupValues?.getOrNull(1)?.toIntOrNull()
        when {
            repeatType.contains("every day", ignoreCase = true) -> {
                selectedRepeatValue = 1
                selectedRepeatUnitIndex = 0
            }

            repeatType.contains("week", ignoreCase = true) -> {
                selectedRepeatValue = value ?: 1
                selectedRepeatUnitIndex = 1
            }

            repeatType.contains("month", ignoreCase = true) -> {
                selectedRepeatValue = value ?: 1
                selectedRepeatUnitIndex = 2
            }

            else -> {
                selectedRepeatValue = value ?: 1
                selectedRepeatUnitIndex = 0
            }
        }
    }

    private fun parseTime(time: String) {
        val parts = time.split(":")
        selectedHour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 9
        selectedMinute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 30
    }

    private fun getMaxRepeatValue(): Int {
        return when (repeatUnits[selectedRepeatUnitIndex]) {
            "DAYS" -> 31
            "WEEKS" -> 7
            else -> 12
        }
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
