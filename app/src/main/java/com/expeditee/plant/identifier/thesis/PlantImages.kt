package com.expeditee.plant.identifier.thesis

import android.graphics.drawable.Drawable
import android.view.View
import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.extension.loadImageWithGlide
import java.io.File

/** Resolve plant thumbnails from assets (`plant_images/…`) or remote URLs. */
object PlantImages {
    fun resolve(context: Context, url: String?): Any? {
        if (url.isNullOrBlank()) return null
        if (url.startsWith("plant_images/") ||
            url.startsWith("pest_images/") ||
            url.startsWith("morphology_images/") ||
            url.startsWith("fertilizer_images/")
        ) {
            return copyAssetToCache(context, url)
        }
        return ThesisPlantCatalog.imageModel(url)
    }

    fun load(
        imageView: ImageView,
        url: String?,
        error: Int = R.drawable.ic_camera_home,
        roundedCornerDp: Int = 0,
    ) {
        val model = resolve(imageView.context, url) ?: error
        imageView.loadImageWithGlide(model, error = error, roundedCornerDp = roundedCornerDp)
    }

    fun loadOptional(
        imageView: ImageView,
        url: String?,
        roundedCornerDp: Int = 0,
        onReady: (() -> Unit)? = null,
        onFailed: (() -> Unit)? = null,
    ) {
        val model = resolveStrict(imageView.context, url)
        if (model == null) {
            clear(imageView)
            onFailed?.invoke()
            return
        }

        imageView.visibility = View.INVISIBLE
        var options = RequestOptions()
        if (roundedCornerDp > 0) {
            val radius = (roundedCornerDp * imageView.context.resources.displayMetrics.density).toInt()
            options = options.transform(CenterCrop(), RoundedCorners(radius))
        }

        Glide.with(imageView)
            .load(model)
            .apply(options)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>,
                    isFirstResource: Boolean,
                ): Boolean {
                    imageView.post {
                        clear(imageView)
                        onFailed?.invoke()
                    }
                    return true
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean,
                ): Boolean {
                    imageView.post {
                        imageView.visibility = View.VISIBLE
                        onReady?.invoke()
                    }
                    return false
                }
            })
            .into(imageView)
    }

    fun clear(imageView: ImageView) {
        Glide.with(imageView).clear(imageView)
        imageView.setImageDrawable(null)
        imageView.visibility = View.GONE
    }

    fun loadThumb(
        imageView: ImageView,
        url: String?,
        error: Int = R.drawable.ic_camera_home,
    ) {
        val model = resolve(imageView.context, url) ?: error
        Glide.with(imageView)
            .load(model)
            .override(160, 160)
            .centerCrop()
            .placeholder(error)
            .error(error)
            .into(imageView)
    }

    private fun resolveStrict(context: Context, url: String?): Any? {
        if (url.isNullOrBlank()) return null
        if (url.startsWith("plant_images/") ||
            url.startsWith("pest_images/") ||
            url.startsWith("morphology_images/") ||
            url.startsWith("fertilizer_images/")
        ) {
            return copyAssetToCache(context, url, allowFallback = false)
        }
        return ThesisPlantCatalog.imageModel(url)
    }

    private fun copyAssetToCache(
        context: Context,
        assetPath: String,
        allowFallback: Boolean = true,
    ): File? {
        return try {
            val out = File(context.cacheDir, "plant_thumbs/$assetPath")
            if (out.exists() && out.length() > 0L) return out
            out.parentFile?.mkdirs()
            context.assets.open(assetPath).use { input ->
                out.outputStream().use { input.copyTo(it) }
            }
            if (out.exists() && out.length() > 0L) out else null
        } catch (_: Exception) {
            if (!allowFallback) return null
            try {
                val fallbackAsset = "plant_images/_default.jpg"
                val out = File(context.cacheDir, "plant_thumbs/$fallbackAsset")
                if (out.exists() && out.length() > 0L) return out
                out.parentFile?.mkdirs()
                context.assets.open(fallbackAsset).use { input ->
                    out.outputStream().use { input.copyTo(it) }
                }
                if (out.exists() && out.length() > 0L) out else null
            } catch (_: Exception) {
                null
            }
        }
    }
}
