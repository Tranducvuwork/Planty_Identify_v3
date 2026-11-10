package com.expeditee.plant.identifier.thesis

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.WindowManager
import androidx.core.view.isVisible
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.common.BaseDialog
import com.expeditee.plant.identifier.databinding.DialogDiaryEventBinding
import com.expeditee.plant.identifier.extension.onDebounceClick

class AddDiaryEventDialog(
    context: Context,
    private val onPickPhoto: () -> Unit,
    private val onSave: (title: String, content: String) -> Unit,
) : BaseDialog<DialogDiaryEventBinding>(context, R.style.BaseDialog) {

    override fun getLayoutDialog(): Int = R.layout.dialog_diary_event

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
        binding.btnPickPhoto.onDebounceClick { onPickPhoto() }
        binding.btnCancel.onDebounceClick { dismiss() }
        binding.btnSave.onDebounceClick {
            onSave(
                binding.etEventTitle.text?.toString().orEmpty().ifBlank { "Cập nhật" },
                binding.etEventContent.text?.toString().orEmpty(),
            )
            dismiss()
        }
    }

    fun showPhotoPreview(path: String) {
        if (!isShowing) return
        DiaryPhotos.load(binding.ivPhotoPreview, path)
        binding.btnPickPhoto.text = if (path.isBlank()) {
            "Chọn ảnh cho mốc này"
        } else {
            "Đổi ảnh"
        }
        binding.ivPhotoPreview.isVisible = path.isNotBlank()
    }
}
