package com.expeditee.plant.identifier.extension

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.expeditee.plant.identifier.BuildConfig
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.helper.firebase.RemoteConfigManagement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.context.GlobalContext
import java.io.File

fun resolveImageUrlForShare(photoUrl: String): String? {
    val processed = if (photoUrl.contains("perenual.com")) {
        photoUrl.substringAfterLast("/")
    } else {
        photoUrl
    }
    return when {
        processed.isBlank() -> null
        processed.startsWith("http://") || processed.startsWith("https://") -> processed
        else -> {
            val baseUrl = try {
                GlobalContext.get().get<RemoteConfigManagement>().baseUrlData.trimEnd('/')
            } catch (e: Exception) { "" }
            if (baseUrl.isNotBlank()) "$baseUrl/PlantImage/$processed" else processed
        }
    }
}

fun buildGlideLoadModelForShare(imageUrl: String): Any {
    if (!imageUrl.contains("raw.githubusercontent.com") && !imageUrl.contains("github.com")) {
        return imageUrl
    }
    val gitToken = try {
        GlobalContext.get().get<RemoteConfigManagement>().token.takeIf { it.isNotBlank() } ?: ""
    } catch (e: Exception) {
        ""
    }
    return if (gitToken.isNotEmpty()) {
        val headers = LazyHeaders.Builder()
            .addHeader("Authorization", "token $gitToken")
            .addHeader("Accept", "application/vnd.github.v3.raw")
            .addHeader("User-Agent", "PlantIdentifierApp/1.0")
            .build()
        GlideUrl(imageUrl, headers)
    } else {
        imageUrl
    }
}

fun Context.shareImageFile(file: File) {
    val shareDir = File(cacheDir, "images")
    if (!shareDir.exists()) shareDir.mkdirs()
    val destFile = File(shareDir, "plant_${System.currentTimeMillis()}.jpg")
    try {
        file.copyTo(destFile, overwrite = true)
        launchShareImageIntent(destFile)
    } catch (e: Exception) {
        Toast.makeText(this, getString(R.string.load_error), Toast.LENGTH_SHORT).show()
    }
}

suspend fun Context.shareImageFromUrl(photoUrl: String) {
    val resolvedUrl = resolveImageUrlForShare(photoUrl) ?: run {
        withContext(Dispatchers.Main) {
            Toast.makeText(this@shareImageFromUrl, getString(R.string.load_error), Toast.LENGTH_SHORT).show()
        }
        return
    }
    val loadModel = buildGlideLoadModelForShare(resolvedUrl)
    val shareDir = File(cacheDir, "images")
    if (!shareDir.exists()) shareDir.mkdirs()
    val destFile = File(shareDir, "plant_${System.currentTimeMillis()}.jpg")
    withContext(Dispatchers.IO) {
        try {
            val downloaded = Glide.with(applicationContext)
                .asFile()
                .load(loadModel)
                .submit()
                .get()
            if (downloaded != null && downloaded.exists()) {
                downloaded.copyTo(destFile, overwrite = true)
                withContext(Dispatchers.Main) {
                    launchShareImageIntent(destFile)
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@shareImageFromUrl, getString(R.string.load_error), Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@shareImageFromUrl, getString(R.string.load_error), Toast.LENGTH_SHORT).show()
            }
        }
    }
}

fun Context.launchShareImageIntent(imageFile: File) {
    val authority = "${BuildConfig.APPLICATION_ID}.fileprovider"
    val uri: Uri = FileProvider.getUriForFile(this, authority, imageFile)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/jpeg"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooserIntent = Intent.createChooser(shareIntent, getString(R.string.sharing_photo))
    chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(chooserIntent)
}