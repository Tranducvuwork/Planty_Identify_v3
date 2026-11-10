package com.expeditee.plant.identifier.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.expeditee.plant.identifier.data.upload.UploadImageFileResolver
import com.expeditee.plant.identifier.extension.isNetwork
import com.expeditee.plant.identifier.model.IdentifiedPlant
import com.expeditee.plant.identifier.model.ModeScan
import com.expeditee.plant.identifier.network.PlantNotIdentifiedException
import com.expeditee.plant.identifier.thesis.PlantScanSession
import kotlinx.coroutines.coroutineScope

class OnlinePlantIdentifier(
    private val context: Context,
    private val uploadImageFileResolver: UploadImageFileResolver,
    private val plantScanRepository: PlantScanRepository,
) {
    companion object {
        private const val TAG = "ScanUploadDebug"
    }

    suspend fun identify(imageUri: Uri, imageSource: String): Result<IdentifiedPlant> = coroutineScope {
        Log.i(TAG, "identify start uri=$imageUri source=$imageSource")
        if (!context.isNetwork()) {
            Log.w(TAG, "identify failed no network")
            return@coroutineScope Result.failure(
                IllegalStateException("No internet connection available"),
            )
        }

        val sourceFile = uploadImageFileResolver.resolveToFile(imageUri)
            ?: return@coroutineScope Result.failure(
                IllegalArgumentException("Failed to resolve upload image file"),
            )
        Log.i(TAG, "identify resolved file=${sourceFile.absolutePath} exists=${sourceFile.exists()} size=${sourceFile.length()}B sha256=${fileSha256(sourceFile)}")

        val scanResult = try {
            // Gallery/upload: giống Swagger — gửi bytes gốc, không language, không crop/enhance.
            val fromGallery = shouldUseOriginalFirst(imageSource)
            plantScanRepository.scanPlant(
                imageFile = sourceFile,
                language = if (fromGallery) null else "en",
                enhanceCameraImage = !fromGallery && shouldNormalizeUpload(imageSource),
                preferOriginalFirst = fromGallery,
            ).getOrElse { error ->
                // Không log stack như crash — Unknown là case nghiệp vụ, UI sẽ mở màn error.
                Log.w(TAG, "identify scanPlant failed source=$imageSource error=${error.message}")
                return@coroutineScope Result.failure(error)
            }
        } finally {
            uploadImageFileResolver.cleanupResolvedFile(sourceFile)
        }
        Log.i(
            TAG,
            "identify scan result scientific='${scanResult.scientificName}' " +
                "common='${scanResult.commonNames.firstOrNull().orEmpty()}' confidence=${scanResult.confidence}",
        )

        val scientificName = scanResult.scientificName.trim()
        val confidence = scanResult.confidence.coerceIn(0.0, 100.0)
        val validationError = PlantIdentifyValidation.validateIdentifyResult(
            scientificName = scientificName,
            confidence = confidence,
            mode = ModeScan.ONLINE,
        )
        if (validationError != null) {
            Log.w(
                TAG,
                "identify validation rejected reason='$validationError' scientific='$scientificName' confidence=$confidence",
            )
            val error = if (
                validationError.contains("Invalid scientificName") ||
                validationError.contains("Missing scientificName") ||
                validationError.contains("below threshold")
            ) {
                PlantNotIdentifiedException(validationError)
            } else {
                IllegalArgumentException(validationError)
            }
            return@coroutineScope Result.failure(
                error,
            )
        }

        val commonNames = scanResult.commonNames.filter { it.isNotBlank() }
        PlantScanSession.store(scanResult, imageUri.toString())
        val identified = IdentifiedPlant(
            scientificName = scientificName,
            commonNames = commonNames,
            confidence = confidence / 100.0,
            description = scanResult.description,
            matchedFromCatalog = false,
        )

        Log.i(TAG, "identify success scientific='$scientificName' common='${commonNames.firstOrNull().orEmpty()}' confidence=$confidence")
        Result.success(identified)
    }

    private fun shouldNormalizeUpload(imageSource: String): Boolean {
        return imageSource.equals("camera", ignoreCase = true) ||
            imageSource.equals("gallery", ignoreCase = true) ||
            imageSource.equals("upload", ignoreCase = true)
    }

    private fun shouldUseOriginalFirst(imageSource: String): Boolean {
        return imageSource.equals("gallery", ignoreCase = true) ||
            imageSource.equals("upload", ignoreCase = true)
    }

    private fun fileSha256(file: java.io.File): String {
        return runCatching {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(8 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    digest.update(buffer, 0, read)
                }
            }
            digest.digest().joinToString("") { byte -> "%02x".format(byte) }
        }.getOrDefault("unknown")
    }
}
