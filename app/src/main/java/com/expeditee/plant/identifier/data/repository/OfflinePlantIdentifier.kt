package com.expeditee.plant.identifier.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.expeditee.plant.identifier.classifier.IdentificationClassifier
import com.expeditee.plant.identifier.classifier.Prediction
import com.expeditee.plant.identifier.model.IdentifiedPlant
import com.expeditee.plant.identifier.model.ModeScan
import com.expeditee.plant.identifier.thesis.LocalThesisIdentifyBridge
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class OfflinePlantIdentifier(
    private val context: Context,
    private val identificationClassifier: IdentificationClassifier,
) {
    companion object {
        private const val TAG = "TAG123213"
        private const val OFFLINE_INFERENCE_TIMEOUT_MS = 15_000L
        private const val MIN_OFFLINE_CONFIDENCE_GAP = 0.08
    }

    suspend fun identify(bitmap: Bitmap, imageUri: String? = null): Result<IdentifiedPlant> {
        Log.d(TAG, "Starting offline identification...")
        if (!identificationClassifier.isModelExist()) {
            Log.e(TAG, "Offline model file missing")
            return Result.failure(IllegalStateException("Offline model is not ready"))
        }

        val predictions = withTimeoutOrNull(OFFLINE_INFERENCE_TIMEOUT_MS) {
            classifyFrameAsyncAwait(bitmap)
        } ?: run {
            Log.e(TAG, "AI inference timed out after $OFFLINE_INFERENCE_TIMEOUT_MS ms")
            return Result.failure(IllegalStateException("AI inference timed out"))
        }

        if (predictions.isEmpty()) {
            Log.w(TAG, "ML model returned no predictions")
            return Result.failure(IllegalArgumentException("ML model returned no predictions"))
        }

        val leafPredictions = predictions.filter { it.node.leafId?.isNotEmpty() == true }
        Log.d(TAG, "Total predictions: ${predictions.size}, Leaf predictions: ${leafPredictions.size}")

        if (leafPredictions.isEmpty()) {
            Log.w(TAG, "No plant leaf prediction found in the results")
            return Result.failure(IllegalArgumentException("No plant leaf prediction found"))
        }

        val bestMatch = leafPredictions.first()
        val confidence = bestMatch.probability
        Log.d(TAG, "Best match: ${bestMatch.node.name}, Confidence: $confidence")

        val secondBestLeaf = leafPredictions.getOrNull(1)
        if (secondBestLeaf != null) {
            val confidenceGap = confidence - secondBestLeaf.probability
            Log.d(TAG, "Confidence gap with second best (${secondBestLeaf.node.name}): $confidenceGap")
            if (confidenceGap < MIN_OFFLINE_CONFIDENCE_GAP) {
                Log.w(TAG, "Ambiguous prediction: gap $confidenceGap < $MIN_OFFLINE_CONFIDENCE_GAP")
                return Result.failure(
                    IllegalArgumentException(
                        "Ambiguous offline prediction (gap=$confidenceGap)",
                    ),
                )
            }
        }

        val scientificName = bestMatch.node.name?.trim().orEmpty()

        val validationError = PlantIdentifyValidation.validateIdentifyResult(
            scientificName = scientificName,
            confidence = confidence,
            mode = ModeScan.OFFLINE,
        )
        if (validationError != null) {
            Log.w(TAG, "Validation failed: $validationError")
            return Result.failure(IllegalArgumentException(validationError))
        }

        val identified = IdentifiedPlant(
            scientificName = scientificName,
            confidence = confidence,
            matchedFromCatalog = LocalThesisIdentifyBridge.matchedCatalog(
                context,
                scientificName,
                emptyList(),
            ),
        )

        LocalThesisIdentifyBridge.enrichAndStore(context, identified, imageUri)

        Log.i(TAG, "Offline identification SUCCESS: $scientificName ($confidence)")
        return Result.success(identified)
    }

    private suspend fun classifyFrameAsyncAwait(bitmap: Bitmap): List<Prediction> =
        suspendCancellableCoroutine { continuation ->
            identificationClassifier.classifyFrameAsync(bitmap)
                .addOnSuccessListener { continuation.resume(it) }
                .addOnFailureListener { continuation.resumeWithException(it) }
            continuation.invokeOnCancellation { }
        }
}

internal object PlantIdentifyValidation {
    private val invalidScientificNames = setOf("null", "Unknown", "", "unknown")
    private const val MIN_CONFIDENCE_ONLINE = 50.0
    private const val MIN_CONFIDENCE_OFFLINE = 0.25

    private fun isValidScientificName(scientificName: String): Boolean {
        return scientificName.isNotBlank() && scientificName !in invalidScientificNames
    }

    private fun isValidConfidence(confidence: Double?, mode: ModeScan): Boolean {
        confidence ?: return false
        return when (mode) {
            ModeScan.ONLINE -> confidence >= MIN_CONFIDENCE_ONLINE
            ModeScan.OFFLINE -> confidence >= MIN_CONFIDENCE_OFFLINE
        }
    }

    private fun getThreshold(mode: ModeScan): Double {
        return when (mode) {
            ModeScan.ONLINE -> MIN_CONFIDENCE_ONLINE
            ModeScan.OFFLINE -> MIN_CONFIDENCE_OFFLINE
        }
    }

    fun validateIdentifyResult(
        scientificName: String?,
        confidence: Double?,
        mode: ModeScan,
    ): String? {
        if (scientificName.isNullOrBlank()) {
            return "Missing scientificName in identify result"
        }
        if (!isValidScientificName(scientificName)) {
            return "Invalid scientificName: '$scientificName'"
        }
        if (confidence == null) {
            return "Missing confidence in identify result"
        }
        if (!isValidConfidence(confidence, mode)) {
            val threshold = getThreshold(mode)
            return "Confidence $confidence below threshold $threshold for $mode mode"
        }
        return null
    }
}
