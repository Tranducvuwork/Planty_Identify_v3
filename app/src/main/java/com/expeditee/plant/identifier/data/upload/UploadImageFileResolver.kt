package com.expeditee.plant.identifier.data.upload

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.net.toFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class UploadImageFileResolver(
    private val context: Context,
    private val contentResolver: ContentResolver,
) {
    companion object {
        private const val TAG = "ScanUploadDebug"
    }

    suspend fun resolveToFile(imageUri: Uri): File? = withContext(Dispatchers.IO) {
        when (imageUri.scheme) {
            "file" -> runCatching { imageUri.toFile() }.getOrNull()?.takeIf { it.exists() }?.also {
                Log.i(TAG, "resolveToFile file uri=$imageUri path=${it.absolutePath} size=${it.length()}B")
            }
            "content" -> resolveContentUri(imageUri)
            else -> {
                Log.w(TAG, "resolveToFile unsupported uri=$imageUri scheme=${imageUri.scheme}")
                null
            }
        }
    }

    private fun resolveContentUri(imageUri: Uri): File? {
        val expectedAuthority = "${context.packageName}.fileprovider"
        if (imageUri.authority == expectedAuthority) {
            val lastPathSegment = imageUri.lastPathSegment ?: return null
            val appFile = File(context.filesDir, "images/$lastPathSegment")
            if (appFile.exists()) {
                Log.i(TAG, "resolveContentUri fileprovider uri=$imageUri path=${appFile.absolutePath} size=${appFile.length()}B")
                return appFile
            }
        }

        return runCatching {
            val mime = contentResolver.getType(imageUri)
            val displayName = queryDisplayName(imageUri)
            val ext = extensionFor(mime, displayName)
            contentResolver.openInputStream(imageUri)?.use { input ->
                val dir = File(context.cacheDir, "scan_upload_source").apply { mkdirs() }
                val output = File(dir, "scan_source_${System.currentTimeMillis()}.$ext")
                output.outputStream().use { stream ->
                    input.copyTo(stream)
                }
                output.also {
                    Log.i(
                        TAG,
                        "resolveContentUri copied uri=$imageUri mime=$mime name=$displayName " +
                            "path=${it.absolutePath} size=${it.length()}B",
                    )
                }
            }
        }.onFailure {
            Log.e(TAG, "resolveContentUri failed uri=$imageUri error=${it.message}", it)
        }.getOrNull()
    }

    private fun queryDisplayName(uri: Uri): String? {
        return runCatching {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (!cursor.moveToFirst()) return@use null
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) cursor.getString(index) else null
                }
        }.getOrNull()
    }

    private fun extensionFor(mime: String?, displayName: String?): String {
        val fromName = displayName
            ?.substringAfterLast('.', missingDelimiterValue = "")
            ?.lowercase()
            ?.takeIf { it in setOf("jpg", "jpeg", "png", "webp", "heic", "heif") }
        if (fromName != null) return if (fromName == "jpeg") "jpg" else fromName

        val fromMime = mime
            ?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
            ?.lowercase()
        return when (fromMime) {
            "jpeg" -> "jpg"
            "png", "webp", "heic", "heif", "jpg" -> fromMime
            else -> "jpg"
        }
    }

    fun cleanupResolvedFile(file: File?) {
        if (file == null) return
        val appImagesDir = File(context.filesDir, "images")
        if (file.parentFile == appImagesDir) return
        if (file.absolutePath.startsWith(context.cacheDir.absolutePath)) {
            file.delete()
        }
    }
}
