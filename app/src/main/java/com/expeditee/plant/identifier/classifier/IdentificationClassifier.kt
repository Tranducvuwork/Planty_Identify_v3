package com.expeditee.plant.identifier.classifier

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.core.graphics.scale
import com.expeditee.plant.identifier.common.Constants
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.BufferOverflowException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class IdentificationClassifier(
    private val context: Context,
) {
    companion object {
        private const val DIM_IMG_SIZE_X = 299
        private const val DIM_IMG_SIZE_Y = 299
        private const val IMAGE_MEAN = 128
        private const val IMAGE_STD = 128.0F
        private const val MODEL_INPUT_SIZE = 1072812
        private const val CHANNEL_MAX_VALUE = 0xFF
        private const val PIXEL_ARRAY_SIZE = DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y

        const val TAG = "TAG123213"
    }

    @get:Synchronized
    val modelFile: File by lazy {
        val file = File(context.filesDir, Constants.IDENTIFY_MODEL_SUB_PATH)
        file.parentFile?.takeIf { !it.exists() }?.mkdirs()
        file
    }

    private val executorService: ExecutorService = Executors.newCachedThreadPool()

    private var interpreter: Interpreter? = null
    var isInitialize = false
        private set

    private var imgData: ByteBuffer? = null
    private val pixelValues: IntArray = IntArray(PIXEL_ARRAY_SIZE)
    private var modelSize: Int = 0
    private var taxonomy: Taxonomy? = null

    fun isModelExist(): Boolean {
        return modelFile.exists() && modelFile.length() > 0
    }

    fun initializeFromInternalFile(taxonomyFileName: String): Task<Void?> {
        return initializeModel(
            taxonomyFileName = taxonomyFileName,
            loadInterpreter = { initializeInterpreterFromInternalFile() },
        )
    }

    fun close() {
        executorService.execute {
            interpreter?.close()
            interpreter = null
            isInitialize = false
        }
    }

    fun classifyFrameAsync(bitmap: Bitmap): Task<List<Prediction>> {
        val task = TaskCompletionSource<List<Prediction>>()
        executorService.execute {
            try {
                ensureInitialized()
                val predictions = classifyFrame(bitmap)
                task.setResult(predictions)
            } catch (e: Exception) {
                Log.e(TAG, "Classification failed: ${e.message}", e)
                task.setException(e)
            }
        }
        return task.task
    }

    private fun initializeModel(
        taxonomyFileName: String,
        loadInterpreter: () -> Unit,
    ): Task<Void?> {
        val task = TaskCompletionSource<Void?>()
        executorService.execute {
            try {
                loadInterpreter()
                initializeTaxonomy(taxonomyFileName)

                task.setResult(null)
            } catch (e: Exception) {
                Log.e(TAG, "Model initialization failed: ${e.message}", e)
                task.setException(e)
            }
        }
        return task.task
    }

    private fun ensureInitialized() {
        if (!isInitialize && isModelExist()) {
            synchronized(this) {
                if (!isInitialize) {
                    initializeInterpreterFromInternalFile()
                    initializeTaxonomy(Constants.TAXONOMY_FILE_NAME)
                }
            }
        }
        check(isInitialize) { "TF Lite Interpreter is not initialized yet." }
    }

    @Throws(IOException::class)
    private fun initializeInterpreterFromInternalFile() {
        val file = modelFile
        validateAndLoadModel(file)
    }

    @Throws(IOException::class)
    private fun validateAndLoadModel(file: File) {
        if (!file.exists() || file.length() == 0L) {
            throw IOException("Model file not found or empty: ${file.absolutePath}")
        }

        try {
            val model = loadModelFile(file)
            interpreter = Interpreter(model)
        } catch (error: IllegalArgumentException) {
            runCatching { file.delete() }
            throw IOException("Invalid TF Lite model file removed: ${file.absolutePath}", error)
        }
    }

    @Throws(IOException::class)
    private fun initializeTaxonomy(taxonomyFileName: String) {
        val taxonomyFile = File(context.filesDir, Constants.TAXONOMY_SUB_PATH)
        val inputStream = if (taxonomyFile.exists() && taxonomyFile.length() > 0) {
            taxonomyFile.inputStream()
        } else {
            context.assets.open(taxonomyFileName)
        }
        val taxonomy = Taxonomy.getTaxonomyFromInput(inputStream)
        this.taxonomy = taxonomy
        modelSize = taxonomy.getModelSize()
        isInitialize = true
    }

    @Throws(IOException::class)
    private fun loadModelFile(file: File): ByteBuffer {
        FileInputStream(file).use { inputStream ->
            val fileChannel = inputStream.channel
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, file.length())
        }
    }

    private fun classifyFrame(bitmap: Bitmap): List<Prediction> {
        val resizedImage = bitmap.scale(DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y)
        convertBitmapToByteBuffer(resizedImage)

        val currentImgData = imgData
        val currentInterpreter = interpreter
        val currentTaxonomy = taxonomy

        if (currentImgData == null || currentInterpreter == null || currentTaxonomy == null) {
            return emptyList()
        }

        return runInference(currentImgData, currentInterpreter, currentTaxonomy)
    }

    private fun runInference(
        imgData: ByteBuffer,
        interpreter: Interpreter,
        taxonomy: Taxonomy,
    ): List<Prediction> {
        val outputMap = hashMapOf<Int, Array<FloatArray>>()
        outputMap[0] = Array(1) { FloatArray(size = modelSize) }

        return try {
            interpreter.runForMultipleInputsOutputs(
                arrayOf(imgData),
                outputMap as Map<Int, Any>,
            )
            taxonomy.predict(outputMap).sortedByDescending { it.probability }
        } catch (e: Exception) {
            Log.e(TAG, "Inference failed: ${e.message}", e)
            emptyList()
        }
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap) {
        val byteBuffer = ByteBuffer.allocateDirect(MODEL_INPUT_SIZE).apply {
            order(ByteOrder.nativeOrder())
        }
        imgData = byteBuffer

        byteBuffer.rewind()
        bitmap.getPixels(pixelValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        try {
            convertPixelsToFloatArray(byteBuffer)
        } catch (_: BufferOverflowException) {
        } catch (_: IndexOutOfBoundsException) {
        }
    }

    private fun convertPixelsToFloatArray(byteBuffer: ByteBuffer) {
        var pixelIndex = 0

        for (y in 0 until DIM_IMG_SIZE_Y) {
            for (x in 0 until DIM_IMG_SIZE_X) {
                if (pixelIndex >= pixelValues.size) break

                val pixelValue = pixelValues[pixelIndex++]
                val normalizedRgb = extractAndNormalizeRgb(pixelValue)

                byteBuffer.putFloat(normalizedRgb.red)
                byteBuffer.putFloat(normalizedRgb.green)
                byteBuffer.putFloat(normalizedRgb.blue)
            }
        }
    }

    private fun extractAndNormalizeRgb(pixelValue: Int): RgbValues {
        val red = ((pixelValue shr 16 and CHANNEL_MAX_VALUE) - IMAGE_MEAN).toFloat() / IMAGE_STD
        val green = ((pixelValue shr 8 and CHANNEL_MAX_VALUE) - IMAGE_MEAN).toFloat() / IMAGE_STD
        val blue = ((pixelValue and CHANNEL_MAX_VALUE) - IMAGE_MEAN).toFloat() / IMAGE_STD
        return RgbValues(red, green, blue)
    }

    private data class RgbValues(
        val red: Float,
        val green: Float,
        val blue: Float,
    )
}
