package com.expeditee.plant.identifier.helper

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.graphics.scale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class SafeImageDecoder(
    private val context: Context,
    private val contentResolver: ContentResolver,
) {
    suspend fun decode(
        imageUri: Uri,
        maxWidth: Int = 1024,
        maxHeight: Int = 1024,
    ): Bitmap? = withContext(Dispatchers.IO) {
        if (isInternalFileProviderUri(imageUri)) {
            val fallbackBitmap = decodeFromAppFilesFallback(imageUri, maxWidth, maxHeight)
            return@withContext constrainBitmapSize(fallbackBitmap, maxWidth, maxHeight)
        }

        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        val canDecodeWithResolver = runCatching {
            contentResolver.openInputStream(imageUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, bounds)
            } != null
        }.getOrDefault(false)

        if (!canDecodeWithResolver || bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            val fallbackBitmap = decodeFromAppFilesFallback(imageUri, maxWidth, maxHeight)
            return@withContext constrainBitmapSize(fallbackBitmap, maxWidth, maxHeight)
        }
        val sampleSize =
            calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxWidth, maxHeight)
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decodedBitmap = contentResolver.openInputStream(imageUri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)
        }
        return@withContext constrainBitmapSize(decodedBitmap, maxWidth, maxHeight)
    }

    private fun isInternalFileProviderUri(imageUri: Uri): Boolean {
        val expectedAuthority = "${context.packageName}.fileprovider"
        return imageUri.scheme == "content" && imageUri.authority == expectedAuthority
    }

    private fun constrainBitmapSize(bitmap: Bitmap?, maxWidth: Int, maxHeight: Int): Bitmap? {
        bitmap ?: return null
        if (bitmap.width <= maxWidth && bitmap.height <= maxHeight) return bitmap

        val scale = min(maxWidth.toFloat() / bitmap.width, maxHeight.toFloat() / bitmap.height)
        val targetWidth = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
        val targetHeight = (bitmap.height * scale).roundToInt().coerceAtLeast(1)
        val resized = bitmap.scale(targetWidth, targetHeight)
        if (resized != bitmap) {
            bitmap.recycle()
        }
        return resized
    }

    private fun decodeFromAppFilesFallback(
        imageUri: Uri,
        maxWidth: Int,
        maxHeight: Int,
    ): Bitmap? {
        val fileName = imageUri.lastPathSegment ?: return null
        val fallbackFile = File(context.filesDir, "images/$fileName")
        if (!fallbackFile.exists()) {
            return null
        }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(fallbackFile.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return null
        }
        val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxWidth, maxHeight)
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeFile(fallbackFile.absolutePath, options)
    }

    private fun calculateInSampleSize(
        width: Int,
        height: Int,
        reqWidth: Int,
        reqHeight: Int,
    ): Int {
        var sample = 1
        if (height > reqHeight || width > reqWidth) {
            var halfHeight = height / 2
            var halfWidth = width / 2
            while (halfHeight / sample >= reqHeight && halfWidth / sample >= reqWidth) {
                sample *= 2
                halfHeight = max(halfHeight, 1)
                halfWidth = max(halfWidth, 1)
            }
        }
        return sample
    }
}