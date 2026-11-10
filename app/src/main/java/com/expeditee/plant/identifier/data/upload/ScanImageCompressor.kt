package com.expeditee.plant.identifier.data.upload

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import com.expeditee.plant.identifier.common.Constants
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object ScanImageCompressor {

    fun prepareUploadFile(source: File, enhanceCameraImage: Boolean = false): File {
        if (!source.exists() || !source.canRead() || source.length() <= 0L) return source

        return try {
            normalizeForUpload(source, enhanceCameraImage)
        } catch (_: OutOfMemoryError) {
            source
        } catch (_: Exception) {
            source
        }
    }

    private fun normalizeForUpload(source: File, enhanceCameraImage: Boolean): File {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(source.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return source

        val sampleSize = ScanImageSizing.calculateSampleSize(
            bounds.outWidth,
            bounds.outHeight,
            Constants.SCAN_IMAGE_MAX_DIMENSION,
        )
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decoded = BitmapFactory.decodeFile(source.absolutePath, decodeOptions) ?: return source
        val oriented = applyExifOrientation(decoded, source.absolutePath)
        if (oriented !== decoded) decoded.recycle()

        val focused = if (enhanceCameraImage) plantFocusCrop(oriented) else oriented
        if (focused !== oriented) oriented.recycle()

        val enhanced = if (enhanceCameraImage) enhanceForPlantRecognition(focused) else focused
        if (enhanced !== focused) focused.recycle()

        val scaled = scaleForRecognition(
            bitmap = enhanced,
            maxDimension = Constants.SCAN_IMAGE_MAX_DIMENSION,
            enhanceCameraImage = enhanceCameraImage,
        )
        if (scaled !== enhanced) enhanced.recycle()

        val parent = source.parentFile ?: return source.also { scaled.recycle() }
        val output = File(parent, "scan_upload_${System.currentTimeMillis()}.jpg")
        return try {
            FileOutputStream(output).use { stream ->
                if (!scaled.compress(
                        Bitmap.CompressFormat.JPEG,
                        Constants.IMAGE_COMPRESSION_QUALITY,
                        stream,
                    )
                ) {
                    output.delete()
                    return source
                }
            }
            if (output.length() > 0L) output else {
                output.delete()
                source
            }
        } catch (_: Exception) {
            output.delete()
            source
        } finally {
            scaled.recycle()
        }
    }

    private fun applyExifOrientation(bitmap: Bitmap, path: String): Bitmap {
        val orientation = runCatching {
            ExifInterface(path).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.preScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.preScale(-1f, 1f)
            }
            else -> return bitmap
        }

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun scaleToMaxDimension(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val largestEdge = max(width, height)
        if (largestEdge <= maxDimension) return bitmap

        val scale = maxDimension.toFloat() / largestEdge
        val targetWidth = (width * scale).roundToInt().coerceAtLeast(1)
        val targetHeight = (height * scale).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    private fun scaleForRecognition(bitmap: Bitmap, maxDimension: Int, enhanceCameraImage: Boolean): Bitmap {
        val normalized = scaleToMaxDimension(bitmap, maxDimension)
        if (!enhanceCameraImage) return normalized

        val largestEdge = max(normalized.width, normalized.height)
        val shortestEdge = min(normalized.width, normalized.height)
        val targetLargest = when {
            largestEdge < 1000 -> 1280
            shortestEdge < 720 -> 1440
            else -> largestEdge
        }.coerceAtMost(maxDimension)
        if (targetLargest <= largestEdge) return normalized

        val scale = targetLargest.toFloat() / largestEdge
        val targetWidth = (normalized.width * scale).roundToInt().coerceAtLeast(1)
        val targetHeight = (normalized.height * scale).roundToInt().coerceAtLeast(1)
        val upscaled = Bitmap.createScaledBitmap(normalized, targetWidth, targetHeight, true)
        if (normalized !== bitmap) normalized.recycle()
        return upscaled
    }

    private fun plantFocusCrop(bitmap: Bitmap): Bitmap {
        if (estimatePlantCoverage(bitmap) > 0.42f) return bitmap

        val mainPlantCrop = cropAroundMainPlant(bitmap)
        if (mainPlantCrop !== bitmap) return mainPlantCrop
        val plantCrop = cropAroundGreenPlant(bitmap)
        if (plantCrop !== bitmap) return plantCrop
        return centerFocusCrop(bitmap, cropRatio = 0.84f)
    }

    private fun cropAroundMainPlant(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width < 420 || height < 420) return bitmap

        val scanStep = max(4, min(width, height) / 180)
        val cropScales = floatArrayOf(0.78f, 0.64f, 0.52f)
        var bestLeft = 0
        var bestTop = 0
        var bestWidth = width
        var bestHeight = height
        var bestScore = 0f
        var bestHits = 0
        val imageCenterX = width / 2f
        val imageCenterY = height / 2f

        cropScales.forEach { scale ->
            val cropWidth = (width * scale).roundToInt().coerceIn(1, width)
            val cropHeight = (height * scale).roundToInt().coerceIn(1, height)
            val strideX = max(scanStep * 8, cropWidth / 8)
            val strideY = max(scanStep * 8, cropHeight / 8)
            var top = 0
            while (top <= height - cropHeight) {
                var left = 0
                while (left <= width - cropWidth) {
                    var hits = 0
                    var samples = 0
                    var y = top
                    while (y < top + cropHeight) {
                        var x = left
                        while (x < left + cropWidth) {
                            if (isLikelyPlantPixel(bitmap.getPixel(x, y))) hits++
                            samples++
                            x += scanStep
                        }
                        y += scanStep
                    }
                    if (samples > 0) {
                        val density = hits.toFloat() / samples
                        val centerX = left + cropWidth / 2f
                        val centerY = top + cropHeight / 2f
                        val dx = kotlin.math.abs(centerX - imageCenterX) / width
                        val dy = kotlin.math.abs(centerY - imageCenterY) / height
                        val centerBonus = (1f - (dx + dy)).coerceIn(0.55f, 1f)
                        val score = density * centerBonus
                        if (score > bestScore) {
                            bestScore = score
                            bestHits = hits
                            bestLeft = left
                            bestTop = top
                            bestWidth = cropWidth
                            bestHeight = cropHeight
                        }
                    }
                    left += strideX
                }
                top += strideY
            }
        }

        val fullSampleCount = ((width + scanStep - 1) / scanStep) * ((height + scanStep - 1) / scanStep)
        if (fullSampleCount <= 0) return bitmap
        if (bestHits < fullSampleCount * 0.015f || bestScore < 0.055f) return bitmap
        if (bestWidth >= width * 0.94f && bestHeight >= height * 0.94f) return bitmap

        return Bitmap.createBitmap(bitmap, bestLeft, bestTop, bestWidth, bestHeight)
    }

    private fun estimatePlantCoverage(bitmap: Bitmap): Float {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) return 0f

        val step = max(4, min(width, height) / 160)
        var hits = 0
        var samples = 0
        var y = 0
        while (y < height) {
            var x = 0
            while (x < width) {
                if (isLikelyPlantPixel(bitmap.getPixel(x, y))) hits++
                samples++
                x += step
            }
            y += step
        }
        return if (samples == 0) 0f else hits.toFloat() / samples
    }

    private fun cropAroundGreenPlant(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) return bitmap

        val step = max(2, min(width, height) / 220)
        var minX = width
        var minY = height
        var maxX = -1
        var maxY = -1
        var hitCount = 0

        var y = 0
        while (y < height) {
            var x = 0
            while (x < width) {
                if (isLikelyPlantPixel(bitmap.getPixel(x, y))) {
                    minX = min(minX, x)
                    minY = min(minY, y)
                    maxX = max(maxX, x)
                    maxY = max(maxY, y)
                    hitCount++
                }
                x += step
            }
            y += step
        }

        val sampleColumns = (width + step - 1) / step
        val sampleRows = (height + step - 1) / step
        val sampleCount = sampleColumns * sampleRows
        if (hitCount < sampleCount * 0.012f || maxX <= minX || maxY <= minY) return bitmap

        val boxWidth = maxX - minX
        val boxHeight = maxY - minY
        if (boxWidth < width * 0.18f || boxHeight < height * 0.18f) return bitmap

        val padX = (boxWidth * 0.28f).roundToInt()
        val padY = (boxHeight * 0.28f).roundToInt()
        var left = (minX - padX).coerceAtLeast(0)
        var top = (minY - padY).coerceAtLeast(0)
        var right = (maxX + padX).coerceAtMost(width - 1)
        var bottom = (maxY + padY).coerceAtMost(height - 1)

        val minCropSize = (min(width, height) * 0.52f).roundToInt()
        val currentWidth = right - left + 1
        val currentHeight = bottom - top + 1
        if (currentWidth < minCropSize) {
            val extra = minCropSize - currentWidth
            left = (left - extra / 2).coerceAtLeast(0)
            right = (right + extra - extra / 2).coerceAtMost(width - 1)
        }
        if (currentHeight < minCropSize) {
            val extra = minCropSize - currentHeight
            top = (top - extra / 2).coerceAtLeast(0)
            bottom = (bottom + extra - extra / 2).coerceAtMost(height - 1)
        }

        val cropWidth = right - left + 1
        val cropHeight = bottom - top + 1
        if (cropWidth >= width * 0.96f && cropHeight >= height * 0.96f) return bitmap
        return Bitmap.createBitmap(bitmap, left, top, cropWidth, cropHeight)
    }

    private fun isLikelyPlantPixel(pixel: Int): Boolean {
        val red = Color.red(pixel)
        val green = Color.green(pixel)
        val blue = Color.blue(pixel)
        val maxChannel = max(red, max(green, blue))
        val minChannel = min(red, min(green, blue))
        val saturation = if (maxChannel == 0) 0f else (maxChannel - minChannel).toFloat() / maxChannel
        val brightness = maxChannel / 255f

        val greenDominant = green > red * 1.06f && green > blue * 1.04f
        val healthyGreen = green > 55 && saturation > 0.13f && brightness in 0.18f..0.97f
        val yellowGreen = green > 85 && red > 55 && green >= blue * 1.22f && saturation > 0.16f
        val flowerColor = saturation > 0.28f &&
            brightness in 0.24f..0.98f &&
            (
                red > green * 1.18f && red > blue * 1.12f ||
                    blue > red * 1.08f && blue > green * 1.04f ||
                    red > 125 && green > 95 && blue < 95
                )
        return greenDominant && healthyGreen || yellowGreen || flowerColor
    }

    private fun centerFocusCrop(bitmap: Bitmap, cropRatio: Float): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) return bitmap

        val shortest = min(width, height)
        val cropSize = (shortest * cropRatio).roundToInt().coerceAtLeast(1)
        val left = ((width - cropSize) / 2).coerceAtLeast(0)
        val top = ((height - cropSize) / 2).coerceAtLeast(0)
        if (cropSize == width && cropSize == height) return bitmap

        return Bitmap.createBitmap(bitmap, left, top, cropSize, cropSize)
    }

    private fun enhanceForPlantRecognition(bitmap: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val exposure = estimateExposure(bitmap)
        val sharpness = estimateSharpness(bitmap)
        val contrast = if (exposure > 0.72f) 1.18f else 1.12f
        val brightness = if (exposure > 0.72f) -8f else 2f
        val saturation = if (exposure > 0.72f) 1.2f else 1.14f
        val colorMatrix = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, brightness,
                0f, contrast, 0f, 0f, brightness,
                0f, 0f, contrast, 0f, brightness,
                0f, 0f, 0f, 1f, 0f,
            ),
        ).apply {
            postConcat(ColorMatrix().apply { setSaturation(saturation) })
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }
        Canvas(output).drawBitmap(bitmap, 0f, 0f, paint)
        val sharpenStrength = when {
            sharpness < 7f -> 1.95f
            sharpness < 13f -> 1.75f
            else -> 1.55f
        }
        return sharpen(output, sharpenStrength)
    }

    private fun estimateExposure(bitmap: Bitmap): Float {
        val step = max(8, min(bitmap.width, bitmap.height) / 96)
        var total = 0f
        var count = 0
        var y = 0
        while (y < bitmap.height) {
            var x = 0
            while (x < bitmap.width) {
                val pixel = bitmap.getPixel(x, y)
                total += (Color.red(pixel) * 0.299f + Color.green(pixel) * 0.587f + Color.blue(pixel) * 0.114f) / 255f
                count++
                x += step
            }
            y += step
        }
        return if (count == 0) 0.5f else total / count
    }

    private fun estimateSharpness(bitmap: Bitmap): Float {
        val width = bitmap.width
        val height = bitmap.height
        if (width < 3 || height < 3) return 100f

        val step = max(4, min(width, height) / 120)
        var total = 0f
        var count = 0
        var y = step
        while (y < height - step) {
            var x = step
            while (x < width - step) {
                val center = luminance(bitmap.getPixel(x, y))
                val right = luminance(bitmap.getPixel(x + step, y))
                val bottom = luminance(bitmap.getPixel(x, y + step))
                total += abs(center - right) + abs(center - bottom)
                count++
                x += step
            }
            y += step
        }
        return if (count == 0) 100f else (total / count) / 255f * 100f
    }

    private fun luminance(pixel: Int): Float {
        return Color.red(pixel) * 0.299f + Color.green(pixel) * 0.587f + Color.blue(pixel) * 0.114f
    }

    private fun sharpen(bitmap: Bitmap, strength: Float): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width < 3 || height < 3) return bitmap

        val pixels = IntArray(width * height)
        val output = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        pixels.copyInto(output)

        for (y in 1 until height - 1) {
            val row = y * width
            for (x in 1 until width - 1) {
                val center = pixels[row + x]
                val left = pixels[row + x - 1]
                val right = pixels[row + x + 1]
                val top = pixels[row - width + x]
                val bottom = pixels[row + width + x]
                output[row + x] = Color.rgb(
                    sharpenChannel(Color.red(center), Color.red(left), Color.red(right), Color.red(top), Color.red(bottom), strength),
                    sharpenChannel(Color.green(center), Color.green(left), Color.green(right), Color.green(top), Color.green(bottom), strength),
                    sharpenChannel(Color.blue(center), Color.blue(left), Color.blue(right), Color.blue(top), Color.blue(bottom), strength),
                )
            }
        }

        val sharpened = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        sharpened.setPixels(output, 0, width, 0, 0, width, height)
        bitmap.recycle()
        return sharpened
    }

    private fun sharpenChannel(center: Int, left: Int, right: Int, top: Int, bottom: Int, strength: Float): Int {
        val neighborWeight = (strength - 1f) / 4f
        val value = (center * strength - (left + right + top + bottom) * neighborWeight).roundToInt()
        return value.coerceIn(0, 255)
    }
}
