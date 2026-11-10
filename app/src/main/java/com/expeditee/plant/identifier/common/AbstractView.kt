package com.expeditee.plant.identifier.common

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding

abstract class AbstractView(
    context: Context,
    attrs: AttributeSet?
) : FrameLayout(context, attrs, 0) {

    private var _binding: ViewDataBinding? = null
    protected val binding: ViewDataBinding
        get() = _binding ?: error("Binding is not initialized")

    init {
        val styleable = getStyleableId()
        if (styleable != null && attrs != null) {
            val ta = context.obtainStyledAttributes(attrs, styleable)
            try {
                initDataFromStyleable(ta)
            } finally {
                ta.recycle()
            }
        }

        _binding = DataBindingUtil.inflate(
            LayoutInflater.from(context),
            layoutId(),
            this,
            true
        )

        viewInitialized()
    }

    open fun getStyleableId(): IntArray? = null

    open fun initDataFromStyleable(a: TypedArray) {}

    abstract fun layoutId(): Int

    open fun viewBinding(): ViewDataBinding = binding

    open fun viewInitialized() {}

    protected fun setVariable(variableId: Int, value: Any?) {
        binding.setVariable(variableId, value)
    }

    protected fun executePendingBindings() {
        binding.executePendingBindings()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        _binding?.unbind()
        _binding = null
    }
}