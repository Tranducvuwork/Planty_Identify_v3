package com.expeditee.plant.identifier.presenter.dialog.rate_app

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.expeditee.plant.identifier.databinding.StarGroupBinding

class ApplicationRatingBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), RatingStar.OnRateListener {

    var onRateListener: ((Int) -> Unit)? = null
    private var ratingStar: RatingStar? = null
    private lateinit var binding: StarGroupBinding
    private var animationHandler: Handler? = null

    var rating: Float = 0f
        set(value) {
            field = value
            updateStarDisplay(value.toInt())
        }

    init {
        initBinding()
        setupRatingStar()
    }

    private fun initBinding() {
        binding = StarGroupBinding.inflate(LayoutInflater.from(context), this, true)
    }

    private fun setupRatingStar() {
        ratingStar = RatingStar(binding.starContainer)
        ratingStar?.setOnRateListener(this)
    }

    override fun onRate(star: Int) {
        rating = star.toFloat()
        onRateListener?.invoke(star)
    }

    private fun updateStarDisplay(starCount: Int) {
        for (i in 0 until binding.starContainer.childCount) {
            val child = binding.starContainer.getChildAt(i)
            if ("rate" == child.tag) {
                child.isSelected = i < starCount
            }
        }
    }

    fun showStarAnimation() {
        binding.apply {
            lottieAnim.visibility = VISIBLE
            starContainer.visibility = INVISIBLE
        }

        binding.lottieAnim.playAnimation()
        animationHandler = Handler(Looper.getMainLooper())
        animationHandler?.postDelayed({
            showRatingStars()
        }, 2200)
    }

    private fun showRatingStars() {
        binding.apply {
            lottieAnim.visibility = INVISIBLE
            starContainer.visibility = VISIBLE
        }
        rating = 0f
    }

    fun hideRatingStars() {
        binding.apply {
            lottieAnim.visibility = VISIBLE
            starContainer.visibility = INVISIBLE
        }
    }

    fun cancelAnimation() {
        animationHandler?.removeCallbacksAndMessages(null)
        binding.lottieAnim.cancelAnimation()
    }

    fun cleanup() {
        cancelAnimation()
        animationHandler = null
    }
}

