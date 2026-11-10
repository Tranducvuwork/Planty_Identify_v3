package com.expeditee.plant.identifier.data.upload

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

object PlantImageUploadHelper {

    data class PreparedUpload(
        val file: File,
        val compressed: Boolean,
    )

    fun validate(imageFile: File): Boolean {
        return imageFile.exists() && imageFile.canRead() && imageFile.length() > 0L
    }

    fun prepareUpload(imageFile: File, enhanceCameraImage: Boolean = false): PreparedUpload {
        val uploadFile = ScanImageCompressor.prepareUploadFile(
            source = imageFile,
            enhanceCameraImage = enhanceCameraImage,
        )
        return PreparedUpload(
            file = uploadFile,
            compressed = uploadFile != imageFile,
        )
    }

    fun createImagePart(uploadFile: File): MultipartBody.Part {
        val mime = when (uploadFile.extension.lowercase()) {
            "png" -> "image/png"
            "webp" -> "image/webp"
            "heic", "heif" -> "image/heic"
            else -> "image/jpeg"
        }
        return MultipartBody.Part.createFormData(
            name = "file",
            filename = uploadFile.name,
            body = uploadFile.asRequestBody(mime.toMediaTypeOrNull()),
        )
    }

    fun cleanup(originalFile: File, uploadFile: File) {
        if (uploadFile != originalFile) {
            uploadFile.delete()
        }
    }
}
