package com.expeditee.plant.identifier.presenter.home.more

import android.annotation.SuppressLint
import android.view.MotionEvent
import androidx.databinding.ViewDataBinding
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.common.BaseRecyclerView
import com.expeditee.plant.identifier.databinding.ItemMoreOptionBinding


class MoreOptionAdapter(
    private val animationConfig: AnimationConfig = AnimationConfig()
) : BaseRecyclerView<MoreOptionItem>() {

    var onItemClickListener: ((MoreOptionItem) -> Unit)? = null
    private var lastClickTime = 0L

    data class AnimationConfig(
        val scaleDown: Float = 0.9f,
        val animationDuration: Long = 100L,
        val debounceTime: Long = 300L
    )

    override fun getItemLayout(): Int = R.layout.item_more_option

    override fun submitData(newData: List<MoreOptionItem>) {
        updateData(newData)
    }

    override fun setData(binding: ViewDataBinding, item: MoreOptionItem, position: Int) {
        if (binding is ItemMoreOptionBinding) {
            binding.apply {
                ivIcon.setImageResource(item.icon)
                tvTitle.text = item.title
                tvSubtitle.text = item.subtitle
                root.setBackgroundResource(item.backgroundDrawable)
                tvTitle.setTextColor(context!!.getColor(item.titleColor))
                tvSubtitle.setTextColor(context!!.getColor(item.subtitleColor))

                setupScaleAnimation(root, item)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupScaleAnimation(view: android.view.View, item: MoreOptionItem) {
        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate()
                        .scaleX(animationConfig.scaleDown)
                        .scaleY(animationConfig.scaleDown)
                        .setDuration(animationConfig.animationDuration)
                        .start()
                }

                MotionEvent.ACTION_UP -> {
                    val now = System.currentTimeMillis()
                    v.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(animationConfig.animationDuration)
                        .withEndAction {
                            if (now - lastClickTime >= animationConfig.debounceTime) {
                                lastClickTime = now
                                onItemClickListener?.invoke(item)
                            }
                        }
                        .start()
                }

                MotionEvent.ACTION_CANCEL -> {
                    v.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(animationConfig.animationDuration)
                        .start()
                }
            }
            true
        }
    }

    override fun onClickView(binding: ViewDataBinding, obj: MoreOptionItem, position: Int) {
    }
}