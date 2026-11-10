package com.expeditee.plant.identifier.thesis

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.WindowManager
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.common.BaseDialog
import com.expeditee.plant.identifier.databinding.DialogDiaryCreateBinding
import com.expeditee.plant.identifier.extension.onDebounceClick

class CreateDiaryDialog(
    context: Context,
    private val onSave: (title: String, plantName: String, specialties: String, note: String) -> Unit,
) : BaseDialog<DialogDiaryCreateBinding>(context, R.style.BaseDialog) {

    override fun getLayoutDialog(): Int = R.layout.dialog_diary_create

    override fun onStart() {
        super.onStart()
        window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val width = (context.resources.displayMetrics.widthPixels * 0.9f).toInt()
            setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)
            attributes = attributes?.apply {
                dimAmount = 0.6f
                flags = flags or WindowManager.LayoutParams.FLAG_DIM_BEHIND
            }
        }
    }

    override fun onClickViews() {
        binding.btnCancel.onDebounceClick { dismiss() }
        binding.btnSave.onDebounceClick {
            onSave(
                binding.etTitle.text?.toString().orEmpty().ifBlank { "Nhật ký trồng cây" },
                binding.etPlant.text?.toString().orEmpty().ifBlank { "Cây của tôi" },
                binding.etSpecialties.text?.toString().orEmpty(),
                binding.etNote.text?.toString().orEmpty(),
            )
            dismiss()
        }
    }
}
