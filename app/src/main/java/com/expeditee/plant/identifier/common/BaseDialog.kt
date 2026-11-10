package com.expeditee.plant.identifier.common

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.expeditee.plant.identifier.R


abstract class BaseDialog<VB : ViewDataBinding>(
    context: Context,
    themeResId: Int = R.style.BaseDialog
) :
    Dialog(context, themeResId) {
    lateinit var binding: VB

    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        createContentView()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initViews()
        onResizeViews()
        onClickViews()
    }

    private fun createContentView() {
        val layoutView = getLayoutDialog()
        binding = DataBindingUtil.inflate(LayoutInflater.from(context), layoutView, null, false)
        setContentView(binding.root)
    }

    abstract fun getLayoutDialog(): Int

    open fun initViews() {}

    open fun onResizeViews() {}

    open fun onClickViews() {}


    fun setDialogBottom() {
        window?.run {
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            setGravity(Gravity.BOTTOM)
        }
    }

    fun showKeyboard() {
        val inputMethodManager =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
    }

    fun closeKeyboard() {
        val inputMethodManager =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        window?.decorView?.windowToken?.let { token ->
            inputMethodManager.hideSoftInputFromWindow(token, 0)
        }
    }


}

