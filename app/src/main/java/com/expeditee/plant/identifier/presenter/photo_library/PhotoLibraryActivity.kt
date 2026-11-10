package com.expeditee.plant.identifier.presenter.photo_library

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.expeditee.plant.identifier.common.BaseActivity
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.common.ResultFlow
import com.expeditee.plant.identifier.databinding.ActivityPhotoLibraryBinding
import com.expeditee.plant.identifier.extension.collectLatestRepeatOnLifecycle
import com.expeditee.plant.identifier.extension.statusBars
import com.expeditee.plant.identifier.extension.shareImageFile
import com.expeditee.plant.identifier.extension.shareImageFromUrl
import com.expeditee.plant.identifier.presenter.photo_library.adapter.PhotoThumbnailAdapter
import com.expeditee.plant.identifier.presenter.photo_library.adapter.PhotoViewerAdapter
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs

class PhotoLibraryActivity :
    BaseActivity<ActivityPhotoLibraryBinding, PhotoLibraryViewModel, PhotoLibraryState>() {

    private lateinit var photoList: List<String>
    private var selectedIndex: Int = 0
    private var currentViewerPosition: Int = 0
    private lateinit var viewerLayoutManager: LinearLayoutManager
    private lateinit var snapHelper: PagerSnapHelper
    private lateinit var thumbnailAdapter: PhotoThumbnailAdapter

    companion object {
        private const val EXTRA_PHOTO_LIST = "extra_photo_list"
        private const val EXTRA_SELECTED_INDEX = "extra_selected_index"

        fun start(context: Context, photoList: List<String>, selectedIndex: Int) {
            val intent = Intent(context, PhotoLibraryActivity::class.java)
            intent.putStringArrayListExtra(EXTRA_PHOTO_LIST, ArrayList(photoList))
            intent.putExtra(EXTRA_SELECTED_INDEX, selectedIndex)
            context.startActivity(intent)
        }
    }

    override fun isFitsSystemWindows(): Boolean = false

    override fun getLazyViewModel(): Lazy<PhotoLibraryViewModel> = lazy {
        ViewModelProvider(this)[PhotoLibraryViewModel::class.java]
    }

    override fun provideLayout(): Int = R.layout.activity_photo_library

    override fun initViews(savedInstanceState: Bundle?) {
        photoList = intent.getStringArrayListExtra(EXTRA_PHOTO_LIST) ?: emptyList()
        selectedIndex = intent.getIntExtra(EXTRA_SELECTED_INDEX, 0).coerceIn(
            minimumValue = 0,
            maximumValue = (photoList.lastIndex).coerceAtLeast(0)
        )

        applyInset()
        if (photoList.isEmpty()) {
            Toast.makeText(this, getString(R.string.load_error), Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        setupPhotoViewer()
        setupPhotoThumbnailList()
    }

    override fun initObservers() {
        super.initObservers()
        collectLatestRepeatOnLifecycle(viewModel.photoState) { result ->
            if (result is ResultFlow.Error) {
                result.msg?.let { msg ->
                    Toast.makeText(this@PhotoLibraryActivity, msg, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun initListeners() {
        super.initListeners()

        viewBinding.ivBackToolbar.setOnClickListener {
            closeScreen()
        }
        viewBinding.ivShare.setOnClickListener {
            shareCurrentPhoto()
        }
        onHandleBackPressed {
            closeScreen()
        }
    }

    private fun applyInset() {
        applyInsetToView { _: View, insets ->
            viewBinding.root.setPadding(0, insets.statusBars().top, 0, 0)
        }
    }
    private fun setupPhotoViewer() {
        viewerLayoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        val adapter = PhotoViewerAdapter(photoList)

        viewBinding.photoViewPager.apply {
            this.adapter = adapter
            this.layoutManager = viewerLayoutManager
            setHasFixedSize(true)
            itemAnimator = null
        }

        snapHelper = PagerSnapHelper().also {
            it.attachToRecyclerView(viewBinding.photoViewPager)
        }

        viewBinding.photoViewPager.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val centerX = recyclerView.width / 2
                for (i in 0 until recyclerView.childCount) {
                    val child = recyclerView.getChildAt(i)
                    val childCenterX = (child.left + child.right) / 2
                    val distance = abs(centerX - childCenterX).toFloat()
                    val scale = 1 - (distance / recyclerView.width) * 0.25f
                    val alpha = 1 - (distance / recyclerView.width) * 0.6f
                    child.scaleX = scale.coerceAtLeast(0.85f)
                    child.scaleY = scale.coerceAtLeast(0.85f)
                    child.alpha = alpha.coerceAtLeast(0.5f)
                }
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val snapView = snapHelper.findSnapView(viewerLayoutManager)
                    snapView?.let {
                        val position = viewerLayoutManager.getPosition(it)
                        onViewerPositionChanged(position)
                    }
                }
            }
        })

        viewBinding.photoViewPager.post {
            viewBinding.photoViewPager.scrollToPosition(selectedIndex)
            onViewerPositionChanged(selectedIndex)
        }
    }

    private fun setupPhotoThumbnailList() {
        thumbnailAdapter = PhotoThumbnailAdapter(photoList) { position ->
            viewBinding.photoViewPager.scrollToPosition(position)
            onViewerPositionChanged(position)
        }

        viewBinding.photoListRecyclerView.apply {
            this.adapter = thumbnailAdapter
            layoutManager = LinearLayoutManager(
                this@PhotoLibraryActivity, LinearLayoutManager.HORIZONTAL, false
            )
        }

        thumbnailAdapter.setInitialSelection(selectedIndex)
    }

    private fun updateIndexText(currentIndex: Int) {
        viewBinding.indexText.text = buildString {
            append(currentIndex)
            append(" / ")
            append(photoList.size)
        }
    }

    private fun shareCurrentPhoto() {
        if (currentViewerPosition !in photoList.indices) {
            viewModel.notifyError(getString(R.string.load_error))
            return
        }
        val photoUrl = photoList[currentViewerPosition]
        val isLocalFile = photoUrl.startsWith("/") && File(photoUrl).exists()
        if (isLocalFile) {
            shareImageFile(File(photoUrl))
        } else {
            lifecycleScope.launch {
                shareImageFromUrl(photoUrl)
            }
        }
    }

    private fun closeScreen() {
        showInterAdBeforeDoAction(
            actionRunnable = Runnable { finish() }
        )
    }

    private fun onViewerPositionChanged(position: Int) {
        if (position !in photoList.indices) return
        currentViewerPosition = position
        updateIndexText(position + 1)
        if (::thumbnailAdapter.isInitialized) {
            thumbnailAdapter.setSelectedPosition(position)
            viewBinding.photoListRecyclerView.smoothScrollToPosition(position)
        }
    }
}
