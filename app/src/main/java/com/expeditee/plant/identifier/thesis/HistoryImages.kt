package com.expeditee.plant.identifier.thesis

import android.content.Context
import android.net.Uri
import java.io.File

/** Persist scan thumbnails so history/favorites still load after URI permission ends. */
object HistoryImages {
    fun persist(context: Context, path: String?): String {
        if (path.isNullOrBlank()) return ""
        if (path.startsWith("http")) return path

        val existing = resolveLocalFile(path)
        if (existing != null && existing.exists() && existing.length() > 0L) {
            val historyDir = File(context.filesDir, "history").absolutePath
            if (existing.absolutePath.startsWith(historyDir)) {
                return existing.absolutePath
            }
        }

        return try {
            val dir = File(context.filesDir, "history").apply { mkdirs() }
            val out = File(dir, "h_${System.currentTimeMillis()}.jpg")
            when {
                path.startsWith("content:") || path.startsWith("file:") -> {
                    context.contentResolver.openInputStream(Uri.parse(path))?.use { input ->
                        out.outputStream().use { input.copyTo(it) }
                    } ?: return path
                }
                else -> {
                    val src = File(path)
                    if (!src.exists()) return path
                    src.inputStream().use { input ->
                        out.outputStream().use { input.copyTo(it) }
                    }
                }
            }
            if (out.exists() && out.length() > 0L) out.absolutePath else path
        } catch (_: Exception) {
            path
        }
    }

    fun resolveLocalFile(path: String?): File? {
        if (path.isNullOrBlank() || path.startsWith("http") || path.startsWith("content:")) {
            return null
        }
        return when {
            path.startsWith("file:") -> {
                val parsed = Uri.parse(path).path ?: return null
                File(parsed)
            }
            else -> File(path)
        }
    }
}
