package com.expeditee.plant.identifier.extension

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.helper.firebase.RemoteConfigManagement
import org.koin.core.context.GlobalContext

private const val TAG = "PlantImageLoader"
private const val GITHUB_IMAGE_FOLDER = "PlantImage"

fun ImageView.loadImageWithGithub(
    thumb: Any?, onReady: ((Any?) -> Unit)? = null, onFailed: (() -> Unit)? = null
) {
    val baseUrl = try {
        GlobalContext.get().get<RemoteConfigManagement>().baseUrlData.trimEnd('/')
    } catch (e: Exception) {
        null
    }

    val gitToken = try {
        GlobalContext.get().get<RemoteConfigManagement>().token.takeIf { it.isNotBlank() } ?: ""
    } catch (e: Exception) {
        ""
    }

    // Log input
    Log.d(TAG, "Input thumb: $thumb")

    val processedThumb = if (thumb is String && thumb.contains("perenual.com")) {
        thumb.substringAfterLast("/")
    } else {
        thumb
    }

    Log.d(TAG, "Processed thumb: $processedThumb")

    val imageUrl: Any? = when (processedThumb) {
        is String -> {
            when {
                processedThumb.isBlank() -> {
                    Log.w(TAG, "Thumb is blank, using default PlantImage/")
                    null
                }

                processedThumb.startsWith("content://") -> {
                    processedThumb.toUri()
                }

                processedThumb.startsWith("http://") || processedThumb.startsWith("https://") -> {
                    processedThumb
                }

                processedThumb.startsWith("/") || processedThumb.contains("/") -> {
                    val file = java.io.File(processedThumb)
                    if (file.exists()) file else {
                        Log.w(
                            TAG,
                            "File not found: $processedThumb, trying with PlantImage/ prefix"
                        )
                        buildPlantImageUrl("PlantImage/$processedThumb", baseUrl)
                    }
                }

                else -> {
                    Log.d(TAG, "Adding PlantImage/ prefix to: $processedThumb")
                    buildPlantImageUrl("PlantImage/$processedThumb", baseUrl)
                }
            }
        }

        else -> processedThumb
    }

    val finalImageUrl = imageUrl ?: if (processedThumb is String && processedThumb.isNotBlank()) {
        Log.w(TAG, "Image URL null, using default with PlantImage/")
        buildPlantImageUrl("PlantImage/$processedThumb", baseUrl)
    } else {
        null
    }

    if (finalImageUrl == null) {
        this.setImageResource(R.drawable.ic_placeholder_loading)
        onFailed?.invoke()
        Log.e(
            TAG,
            "Final image URL is null. Input: $thumb, Processed: $processedThumb, BaseUrl: $baseUrl"
        )
        return
    }

    val imageUrlString = (finalImageUrl as? String) ?: finalImageUrl.toString()
    Log.d(TAG, "Final image URL: $imageUrlString")
    Log.d(TAG, "Loading image from URL: $imageUrlString")

    val loadModel: Any =
        if (finalImageUrl is String && (finalImageUrl.contains("raw.githubusercontent.com") || finalImageUrl.contains(
                "github.com"
            )) && gitToken.isNotEmpty()
        ) {
            GlideUrl(
                finalImageUrl,
                LazyHeaders.Builder()
                    .addHeader("Authorization", "token $gitToken")
                    .addHeader("Accept", "application/vnd.github.v3.raw")
                    .addHeader("User-Agent", "PlantIdentifierApp/1.0")
                    .build()
            )
        } else {
            finalImageUrl
        }

    Glide.with(this)
        .load(loadModel)
        .diskCacheStrategy(DiskCacheStrategy.ALL)
        .placeholder(R.drawable.ic_placeholder_loading)
        .error(R.drawable.ic_placeholder_loading)
        .centerCrop()
        .listener(object : RequestListener<Drawable> {
            override fun onLoadFailed(
                e: GlideException?, model: Any?, target: Target<Drawable>, isFirstResource: Boolean
            ): Boolean {
                Log.e(TAG, " Failed to load image: $imageUrlString", e)
                post {
                    onFailed?.invoke()
                }
                return false
            }

            override fun onResourceReady(
                resource: Drawable,
                model: Any,
                target: Target<Drawable>,
                dataSource: DataSource,
                isFirstResource: Boolean
            ): Boolean {
                Log.d(TAG, " Image loaded successfully: $imageUrlString")
                Log.d(TAG, " Data source: ${dataSource.name}")
                post {
                    onReady?.invoke(resource)
                }
                return false
            }
        }).into(this)
}

private fun buildPlantImageUrl(filename: String, baseUrl: String?): String? {
    if (filename.isBlank() || baseUrl.isNullOrBlank()) {
        Log.w(TAG, "Cannot build URL - filename: $filename, baseUrl: $baseUrl")
        return null
    }
    val normalizedBase = baseUrl.trimEnd('/')
    val cleanFilename = filename.trim()
    val result = "$normalizedBase/$cleanFilename"
    return result
}

private fun String.toImageModel(): Any? {
    val value = trim()
    if (value.isBlank()) return null

    if (
        value.startsWith("content://") ||
        value.startsWith("file://") ||
        value.startsWith("android.resource://")
    ) {
        return Uri.parse(value)
    }

    if (value.startsWith("http://") || value.startsWith("https://")) {
        return if (isGitHubRawUrl()) {
            toGitHubGlideUrl()
        } else {
            value
        }
    }

    if (value.contains("/")) {
        return value
    }

    val baseUrl = try {
        GlobalContext.get().get<RemoteConfigManagement>().baseUrlData.trimEnd('/')
    } catch (e: Exception) {
        ""
    }
    return if (baseUrl.isNotBlank()) "$baseUrl/$GITHUB_IMAGE_FOLDER/$value" else null
}

private fun String.isGitHubRawUrl(): Boolean {
    return contains("raw.githubusercontent.com") || contains("github.com")
}

private fun String.toGitHubGlideUrl(): GlideUrl {
    val gitToken = try {
        GlobalContext.get().get<RemoteConfigManagement>().token.takeIf { it.isNotBlank() } ?: ""
    } catch (e: Exception) {
        ""
    }

    val headers = LazyHeaders.Builder()
        .addHeader("Accept", "application/vnd.github.v3.raw")
        .addHeader("Authorization", "token ${gitToken.trim()}")
        .addHeader("User-Agent", "PlantIdentifierApp/1.0")
        .build()

    return GlideUrl(this, headers)
}


fun ImageView.loadImageWithGlide(
    thumb: Any?,
    @DrawableRes error: Int? = null,
    roundedCornerDp: Int = 0
) {
    var options = RequestOptions()

    if (roundedCornerDp > 0) {
        options = options.transform(
            CenterCrop(),
            RoundedCorners(roundedCornerDp.dpToPx(context)),
        )
    }
    error?.let {
        options = options.error(it)
    }

    Glide.with(this)
        .load(thumb)
        .apply(options)
        .into(this)
}

private fun Int.dpToPx(context: Context): Int =
    (this * context.resources.displayMetrics.density).toInt()