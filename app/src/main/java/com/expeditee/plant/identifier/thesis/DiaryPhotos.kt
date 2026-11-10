package com.expeditee.plant.identifier.thesis

import android.content.Context
import android.net.Uri
import android.widget.ImageView
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.expeditee.plant.identifier.thesis.ThesisPlantCatalog
import java.io.File

object DiaryPhotos {
    fun persist(context: Context, uri: Uri): String {
        return try {
            val dir = File(context.filesDir, "diary").apply { mkdirs() }
            val out = File(dir, "${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                out.outputStream().use { input.copyTo(it) }
            } ?: return ""
            if (!out.exists() || out.length() == 0L) "" else out.absolutePath
        } catch (_: Exception) {
            ""
        }
    }

    fun persistPath(context: Context, path: String?): String {
        if (path.isNullOrBlank() || path.startsWith("http")) return path.orEmpty()
        return try {
            val source = resolveFile(path) ?: return path
            if (!source.exists() || source.length() == 0L) return path
            val dir = File(context.filesDir, "diary").apply { mkdirs() }
            val out = File(dir, "${System.currentTimeMillis()}_${source.nameWithoutExtension}.jpg")
            source.inputStream().use { input ->
                out.outputStream().use { input.copyTo(it) }
            }
            if (out.exists() && out.length() > 0L) out.absolutePath else path
        } catch (_: Exception) {
            path
        }
    }

    fun load(imageView: ImageView, path: String?) {
        if (path.isNullOrBlank()) {
            imageView.setImageDrawable(null)
            imageView.isVisible = false
            return
        }
        imageView.isVisible = true
        Glide.with(imageView.context)
            .load(ThesisPlantCatalog.imageModel(path) ?: resolveFile(path) ?: path)
            .centerCrop()
            .into(imageView)
    }

    fun resolveFile(path: String?): File? {
        if (path.isNullOrBlank()) return null
        return when {
            path.startsWith("file:") -> {
                val parsed = Uri.parse(path).path ?: return null
                File(parsed)
            }
            else -> File(path)
        }
    }
}
