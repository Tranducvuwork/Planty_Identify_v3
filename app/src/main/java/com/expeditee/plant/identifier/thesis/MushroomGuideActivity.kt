package com.expeditee.plant.identifier.thesis

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.view.WindowInsetsCompat
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.common.BaseActivity
import com.expeditee.plant.identifier.common.BaseViewModel
import com.expeditee.plant.identifier.databinding.ActivityMushroomGuideBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class MushroomGuideViewModel(
    application: android.app.Application,
) : BaseViewModel<MushroomGuideState>(application) {
    override fun onState(state: MushroomGuideState) = Unit
}

data class MushroomGuideState(
    val unused: Boolean = false,
) : BaseViewModel.State

class MushroomGuideActivity :
    BaseActivity<ActivityMushroomGuideBinding, MushroomGuideViewModel, MushroomGuideState>() {

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, MushroomGuideActivity::class.java))
        }

        private const val GUIDE = """CẢNH BÁO NẤM ĐỘC
Không ăn nấm hoang dã nếu chưa được chuyên gia định loại.

Cách phân biệt nghi độc (chỉ mang tính giáo dục):
• Mũ nấm trắng/xám, có vòng cổ và bao gốc (volva) — họ Amanita rất nguy hiểm
• Sợi tơ trắng, phiến nấm trắng, không đổi màu khi cắt không có nghĩa là an toàn
• Nấm mọc gần đường, bãi rác, gốc cây mục dễ nhiễm độc tố / kim loại nặng
• Không tin mẹo dân gian (muối, tỏi, thìa bạc)

Sơ cứu ban đầu nếu đã nuốt:
1. Gọi 115 ngay
2. Không tự gây nôn nếu chưa được hướng dẫn
3. Mang mẫu nấm (găng tay, túi kín) tới bệnh viện
4. Không uống rượu

Nguồn tham khảo: Wikipedia Mushroom poisoning; video tìm kiếm “phân biệt nấm độc”.
Chỉ dùng cho học tập — không thay thế ý kiến y tế."""
    }

    override fun provideLayout() = R.layout.activity_mushroom_guide
    override fun getLazyViewModel() = viewModel<MushroomGuideViewModel>()
    override fun isFitsSystemWindows() = false

    override fun initViews(savedInstanceState: Bundle?) {
        applyInsetToView(viewBinding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, bars.top, 0, bars.bottom)
        }
        viewBinding.toolbar.setNavigationOnClickListener { finish() }
        viewBinding.tvGuide.text = GUIDE
        viewBinding.btnPlayAlert.setOnClickListener {
            ToxicAlertPlayer.play(this, null)
        }
    }

    override fun onDestroy() {
        ToxicAlertPlayer.stop()
        super.onDestroy()
    }
}
