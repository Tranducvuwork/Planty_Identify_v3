package com.expeditee.plant.identifier.widget.bottomsheet

import android.os.Bundle
import androidx.annotation.StringRes
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.common.BaseBottomSheetDialogFragment
import com.expeditee.plant.identifier.databinding.FragmentMoveToBottomSheetBinding
import com.expeditee.plant.identifier.model.GardenOption
import com.expeditee.plant.identifier.model.MyPlantModel
import com.expeditee.plant.identifier.widget.adapter.GardenOptionSelectAdapter

class MoveToBottomSheetDialog : BaseBottomSheetDialogFragment<FragmentMoveToBottomSheetBinding>(
    FragmentMoveToBottomSheetBinding::inflate
) {
    override val TAG = "MoveToBottomSheetDialog"

    private var onMoveConfirmedListener: ((MyPlantModel, GardenOption) -> Unit)? = null
    private var onOptionConfirmedListener: ((GardenOption) -> Unit)? = null

    private val defaultSelectedId by lazy { getString(R.string.id_garden_plant) }
    private var selectedGardenId: String = ""

    private val adapter by lazy {
        GardenOptionSelectAdapter { selectedOption ->
            selectedGardenId = selectedOption.id
        }
    }

    private val gardenOptions by lazy {
        listOf(
            GardenOption(
                id = getString(R.string.id_garden_plant),
                name = getString(R.string.garden_option_plant)
            ),
            GardenOption(
                id = getString(R.string.id_garden_flower),
                name = getString(R.string.garden_option_flower)
            )
        )
    }

    companion object {
        private const val KEY_PLANT = "plant_key"
        private const val KEY_TITLE_RES = "title_res"
        private const val KEY_BUTTON_RES = "button_res"

        fun newInstance(
            plant: MyPlantModel,
            @StringRes titleRes: Int = R.string.move_to,
            @StringRes buttonRes: Int = R.string.move,
        ) = MoveToBottomSheetDialog().apply {
            arguments = Bundle().apply {
                putParcelable(KEY_PLANT, plant)
                putInt(KEY_TITLE_RES, titleRes)
                putInt(KEY_BUTTON_RES, buttonRes)
            }
        }

        fun newAddInstance(
            @StringRes titleRes: Int = R.string.add_plant_title,
            @StringRes buttonRes: Int = R.string.add,
        ) = MoveToBottomSheetDialog().apply {
            arguments = Bundle().apply {
                putInt(KEY_TITLE_RES, titleRes)
                putInt(KEY_BUTTON_RES, buttonRes)
            }
        }
    }

    fun setOnMoveConfirmed(listener: (MyPlantModel, GardenOption) -> Unit): MoveToBottomSheetDialog {
        this.onMoveConfirmedListener = listener
        return this
    }

    fun setOnOptionConfirmed(listener: (GardenOption) -> Unit): MoveToBottomSheetDialog {
        this.onOptionConfirmedListener = listener
        return this
    }

    private fun getPlant(): MyPlantModel? {
        return arguments?.getParcelable(KEY_PLANT)
    }

    @StringRes
    private fun getTitleRes(): Int {
        return arguments?.getInt(KEY_TITLE_RES, R.string.move_to) ?: R.string.move_to
    }

    @StringRes
    private fun getButtonRes(): Int {
        return arguments?.getInt(KEY_BUTTON_RES, R.string.move) ?: R.string.move
    }

    override fun initViews() {
        if (selectedGardenId.isEmpty()) {
            selectedGardenId = defaultSelectedId
        }

        viewBinding.tvTitle.setText(getTitleRes())
        viewBinding.btnMove.setText(getButtonRes())
        viewBinding.rvGardens.adapter = adapter

        adapter.setSelected(selectedGardenId)
        adapter.submitList(gardenOptions)

    }

    override fun initObservers() {}

    override fun initListeners() {
        viewBinding.ivClose.setOnClickListener {
            dismiss()
        }

        viewBinding.btnMove.setOnClickListener {
            handleMoveClick()
        }
    }

    private fun handleMoveClick() {
        gardenOptions.find { it.id == selectedGardenId }?.let { selectedOption ->
            val currentPlant = getPlant()
            if (currentPlant != null) {
                onMoveConfirmedListener?.invoke(currentPlant, selectedOption)
            } else {
                onOptionConfirmedListener?.invoke(selectedOption)
            }
            dismiss()
        }
    }

    override fun onDestroyView() {
        onMoveConfirmedListener = null
        onOptionConfirmedListener = null
        super.onDestroyView()
    }
}
