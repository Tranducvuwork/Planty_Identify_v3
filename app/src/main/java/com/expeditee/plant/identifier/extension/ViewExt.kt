package com.expeditee.plant.identifier.extension

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.HORIZONTAL
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.viewpager2.widget.ViewPager2
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.RenderEffectBlur
import eightbitlab.com.blurview.RenderScriptBlur
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

fun View.beVisible() {
    this.visibility = View.VISIBLE
}

fun View.beGone() {
    this.visibility = View.GONE
}

fun View.beInvisible() {
    this.visibility = View.INVISIBLE
}

fun View.show(isShow: Boolean) {
    visibility = if (isShow) View.VISIBLE else View.GONE
}

fun View.gone(isEmpty: Boolean) {
    visibility = if (isEmpty) View.GONE else View.VISIBLE
}

fun View.onDebounceClick(interval: Long = 800L, action: () -> Unit) {
    this.setOnClickListener(object : View.OnClickListener {
        private var previousClickTime: Long = 0
        override fun onClick(v: View) {
            if (SystemClock.elapsedRealtime() - previousClickTime < interval) return
            else action.invoke()
            previousClickTime = SystemClock.elapsedRealtime()
        }
    })
}

fun View.showKeyboard() {
    requestFocus()
    val inputMethodManager =
        context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
}

fun View.hideKeyboard(): Boolean {
    try {
        val inputMethodManager =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        return inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
    } catch (ignored: RuntimeException) {
    }
    return false
}

fun RecyclerView.onScrollWithDebounce(
    interval: Long = 50L,
    action: (recyclerView: RecyclerView, dx: Int, dy: Int) -> Unit,
) {
    this.addOnScrollListener(object : OnScrollListener() {
        private var preScrollTime: Long = 0
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            val lm = recyclerView.layoutManager as? LinearLayoutManager ?: return
            if (lm.orientation == LinearLayoutManager.HORIZONTAL && dx <= 0) return
            if (lm.orientation == LinearLayoutManager.VERTICAL && dy <= 0) return

            if (SystemClock.elapsedRealtime() - preScrollTime < interval) return
            else action.invoke(recyclerView, dx, dy)
            preScrollTime = SystemClock.elapsedRealtime()
        }
    })
}

fun RecyclerView.useLoadMore(
    linearLayoutManager: LinearLayoutManager,
    adapter: Adapter<out ViewHolder>,
    offset: Int = 5,
    onLoad: () -> Unit,
) {
    this.onScrollWithDebounce { _, dx, dy ->
        if (linearLayoutManager.orientation == HORIZONTAL) {
            if ((linearLayoutManager.canLoadMore(dx, adapter, offset))) {
                onLoad.invoke()
            }
        } else {
            if ((linearLayoutManager.canLoadMore(dy, adapter, offset))) {
                onLoad.invoke()
            }
        }
    }
}

fun ViewPager2.disableParentSwipeWhenUserSwipingInside() {
    val child = getChildAt(0)
    if (child is RecyclerView) {
        child.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE ->
                    v.parent?.requestDisallowInterceptTouchEvent(true)

                MotionEvent.ACTION_UP -> {
                    v.parent?.requestDisallowInterceptTouchEvent(false)
                    v.performClick() // handle end click events
                }
            }
            false
        }
    }
}

private fun LinearLayoutManager.canLoadMore(
    scrollPos: Int,
    adapter: Adapter<out ViewHolder>,
    loadOffset: Int,
): Boolean {
    return (findLastVisibleItemPosition() >= adapter.itemCount - loadOffset) && scrollPos > 10
}

inline fun onClick(crossinline block: (View) -> Unit) = View.OnClickListener { view ->
    block.invoke(view)
}

inline fun onDebounceClick(
    interval: Long = 300L,
    crossinline action: (View) -> Unit,
) = object : View.OnClickListener {
    private var previousClickTime: Long = 0
    override fun onClick(v: View) {
        if (SystemClock.elapsedRealtime() - previousClickTime < interval) return
        else action.invoke(v)
        previousClickTime = SystemClock.elapsedRealtime()
    }
}


@SuppressLint("ClickableViewAccessibility")
fun View.onClickWithScale(
    debounceTime: Long = 300L,
    onClick: () -> Unit
) {
    var lastClickTime = 0L

    setOnTouchListener { view, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                view.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).start()
            }

            MotionEvent.ACTION_UP -> {
                val now = System.currentTimeMillis()
                view.animate().scaleX(1f).scaleY(1f).setDuration(100).withEndAction {
                    if (now - lastClickTime >= debounceTime) {
                        lastClickTime = now
                        onClick()
                    }
                }.start()
            }

            MotionEvent.ACTION_CANCEL -> {
                view.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
            }
        }
        true
    }
}


fun BlurView.setEnable(enable: Boolean, viewGroup: ViewGroup) {
    if (!enable) {
        this.beGone()
        return
    }
    this.beVisible()
    val blurAlgorithm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        RenderEffectBlur()
    } else {
        @Suppress("DEPRECATION")
        RenderScriptBlur(context)
    }
    this.setupWith(viewGroup, blurAlgorithm).setBlurRadius(1f)
}

@OptIn(ObsoleteCoroutinesApi::class)
fun BlurView.delayTimeTicker(
    lifecycleOwner: LifecycleOwner,
    delayMillis: Long,
    onTick: (Long) -> Boolean,
    onFinished: () -> Unit
) {
    var job: Job? = null
    try {
        job = lifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            val startedAt = SystemClock.elapsedRealtime()
            ticker(200L).consumeAsFlow().collect {
                if (!isActive) return@collect
                val elapsedTime = SystemClock.elapsedRealtime() - startedAt
                if (elapsedTime >= delayMillis) {
                    beGone()
                    post(onFinished)
                    job?.takeIf { it.isActive }?.let {
                        job?.cancel()
                        job = null
                    }
                    return@collect
                }
                val isCancel = onTick(elapsedTime)
                if (isCancel) {
                    beGone()
                    job?.takeIf { it.isActive }?.let {
                        job?.cancel()
                        job = null
                    }
                    return@collect
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        onFinished.invoke()
        job?.takeIf { it.isActive }?.let {
            job?.cancel()
            job = null
        }
    }
}
