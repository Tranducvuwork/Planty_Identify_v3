package com.expeditee.plant.identifier.widget.bottomsheet

import android.os.Bundle
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.expeditee.plant.identifier.common.BaseBottomSheetDialogFragment
import com.expeditee.plant.identifier.databinding.FragmentSelectPlantBottomSheetBinding
import com.expeditee.plant.identifier.extension.onClickWithScale
import com.expeditee.plant.identifier.model.GardenOption
import com.expeditee.plant.identifier.presenter.reminder.ReminderViewModel
import com.expeditee.plant.identifier.widget.adapter.GardenOptionAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class SelectPlantBottomSheetDialogFragment :
    BaseBottomSheetDialogFragment<FragmentSelectPlantBottomSheetBinding>(
        FragmentSelectPlantBottomSheetBinding::inflate
    ) {
    override val TAG = "SelectPlantBottomSheetDialogFragment"

    private val viewModel by viewModel<ReminderViewModel>()
    private var onPlantSelected: ((GardenOption) -> Unit)? = null
    private var currentSelectedId: String? = null

    private val adapter by lazy {
        GardenOptionAdapter { selectedOption ->
            currentSelectedId = selectedOption.id
        }
    }

    private var plants = listOf<GardenOption>()

    companion object {
        private const val ARG_SELECTED_PLANT_ID = "selected_plant_id"

        fun newInstance(selectedPlantId: Int) = SelectPlantBottomSheetDialogFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_SELECTED_PLANT_ID, selectedPlantId)
            }
        }
    }

    fun setOnPlantSelected(listener: (GardenOption) -> Unit): SelectPlantBottomSheetDialogFragment {
        onPlantSelected = listener
        return this
    }

    override fun onStart() {
        super.onStart()
        val bottomSheetDialog = dialog as? BottomSheetDialog ?: return
        val bottomSheet = bottomSheetDialog.findViewById<ViewGroup>(
            com.google.android.material.R.id.design_bottom_sheet
        ) ?: return
        val behavior = BottomSheetBehavior.from(bottomSheet)
        behavior.skipCollapsed = true
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.isDraggable = false
    }

    override fun initViews() {
        val initialSelectedId = arguments?.getInt(ARG_SELECTED_PLANT_ID, 0) ?: 0
        currentSelectedId = if (initialSelectedId > 0) initialSelectedId.toString() else null

        viewBinding.rvPlants.adapter = adapter
        currentSelectedId?.let { adapter.setSelected(it) }
    }

    override fun initObservers() {
        lifecycleScope.launch {
            viewModel.reminders.collectLatest { reminderList ->
                plants = reminderList.distinctBy { it.plantId }.map { reminder ->
                    GardenOption(
                        id = reminder.plantId.toString(),
                        name = reminder.plantName,
                        imageUrl = reminder.imagePath,
                    )
                }
                adapter.submitList(plants)
                currentSelectedId?.let { adapter.setSelected(it) }
            }
        }
    }

    override fun initListeners() {
        viewBinding.ivClose.onClickWithScale {
            dismiss()
        }

        viewBinding.btnAdd.onClickWithScale {
            handleAddClick()
        }
    }

    private fun handleAddClick() {
        val selectedOption = plants.find { it.id == currentSelectedId }
        if (selectedOption != null) {
            onPlantSelected?.invoke(selectedOption)
            dismiss()
        } else {
            dismiss()
        }
    }
}