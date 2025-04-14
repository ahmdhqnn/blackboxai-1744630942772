package com.example.woundscanner.ai

import android.graphics.*
import android.graphics.Bitmap
import android.graphics.RectF
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min

class WoundAnalysis {
    private var interpreter: Interpreter? = null
    private val imageProcessor = ImageProcessor.Builder()
        .add(ResizeOp(INPUT_SIZE, INPUT_SIZE, ResizeOp.ResizeMethod.BILINEAR))
        .build()

    fun preprocessImage(bitmap: Bitmap): Bitmap {
        // Convert to RGB if needed
        val rgbBitmap = if (bitmap.config != Bitmap.Config.ARGB_8888) {
            bitmap.copy(Bitmap.Config.ARGB_8888, true)
        } else {
            bitmap
        }

        // Apply image enhancement
        return enhanceImage(rgbBitmap)
    }

    fun detectWound(bitmap: Bitmap): RectF {
        // Convert bitmap to byte buffer
        val inputBuffer = convertBitmapToByteBuffer(bitmap)

        // Allocate output buffer for detection results
        val outputBuffer = Array(1) { Array(NUM_DETECTIONS) { FloatArray(6) } }

        // Run inference
        interpreter?.run(inputBuffer, outputBuffer)

        // Process detection results
        return processDetectionResult(outputBuffer[0][0])
    }

    fun classifyWound(bitmap: Bitmap, boundingBox: RectF): WoundClassification {
        // Crop wound region
        val croppedBitmap = cropWoundRegion(bitmap, boundingBox)

        // Convert bitmap to byte buffer
        val inputBuffer = convertBitmapToByteBuffer(croppedBitmap)

        // Allocate output buffer for classification results
        val outputBuffer = Array(1) { FloatArray(NUM_CLASSES) }

        // Run inference
        interpreter?.run(inputBuffer, outputBuffer)

        // Process classification results
        return processClassificationResult(outputBuffer[0])
    }

    fun generateRecommendations(classification: WoundClassification): String {
        return when (classification.type.lowercase()) {
            "abrasion" -> when (classification.severity.lowercase()) {
                "mild" -> """
                    1. Clean the wound with mild soap and water
                    2. Apply antibiotic ointment
                    3. Cover with sterile dressing
                    4. Change dressing daily
                    5. Monitor for signs of infection
                """.trimIndent()
                "moderate" -> """
                    1. Clean thoroughly with antiseptic solution
                    2. Apply prescribed antibiotic ointment
                    3. Use non-stick sterile dressing
                    4. Change dressing twice daily
                    5. Seek medical attention if condition worsens
                """.trimIndent()
                else -> """
                    1. Seek immediate medical attention
                    2. Keep wound clean and covered
                    3. Follow medical professional's instructions
                    4. Monitor for signs of infection
                    5. Schedule regular follow-up appointments
                """.trimIndent()
            }
            "laceration" -> when (classification.severity.lowercase()) {
                "mild" -> """
                    1. Clean wound with antiseptic solution
                    2. Apply sterile adhesive strips if needed
                    3. Cover with sterile dressing
                    4. Change dressing daily
                    5. Monitor healing progress
                """.trimIndent()
                "moderate" -> """
                    1. Seek medical evaluation for potential stitches
                    2. Keep wound elevated if possible
                    3. Apply prescribed treatment
                    4. Change dressing as directed
                    5. Schedule follow-up appointment
                """.trimIndent()
                else -> """
                    1. Seek immediate medical attention
                    2. Apply direct pressure to control bleeding
                    3. Keep wound clean and elevated
                    4. Follow medical professional's instructions
                    5. Regular follow-up care required
                """.trimIndent()
            }
            "burn" -> when (classification.severity.lowercase()) {
                "mild" -> """
                    1. Cool the burn with room temperature water
                    2. Apply burn-specific ointment
                    3. Cover loosely with sterile dressing
                    4. Take over-the-counter pain medication if needed
                    5. Monitor for signs of infection
                """.trimIndent()
                "moderate" -> """
                    1. Seek medical evaluation
                    2. Keep burn clean and covered
                    3. Apply prescribed burn treatment
                    4. Change dressing as directed
                    5. Regular follow-up care required
                """.trimIndent()
                else -> """
                    1. Seek immediate emergency care
                    2. Do not apply ointments
                    3. Cover loosely with clean, dry dressing
                    4. Follow medical professional's instructions
                    5. Specialized burn care may be required
                """.trimIndent()
            }
            else -> """
                1. Seek professional medical evaluation
                2. Keep wound clean and protected
                3. Follow medical professional's instructions
                4. Monitor for signs of infection
                5. Schedule follow-up appointments as needed
            """.trimIndent()
        }
    }

    private fun enhanceImage(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // Apply contrast enhancement
        val contrast = 1.3f
        val brightness = 0f

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val alpha = Color.alpha(pixel)
            var red = Color.red(pixel)
            var green = Color.green(pixel)
            var blue = Color.blue(pixel)

            // Apply contrast
            red = (((red / 255f - 0.5f) * contrast + 0.5f) * 255f + brightness).toInt()
            green = (((green / 255f - 0.5f) * contrast + 0.5f) * 255f + brightness).toInt()
            blue = (((blue / 255f - 0.5f) * contrast + 0.5f) * 255f + brightness).toInt()

            // Clamp values
            red = min(255, max(0, red))
            green = min(255, max(0, green))
            blue = min(255, max(0, blue))

            pixels[i] = Color.argb(alpha, red, green, blue)
        }

        val enhancedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        enhancedBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return enhancedBitmap
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(
            BATCH_SIZE * INPUT_SIZE * INPUT_SIZE * PIXEL_SIZE * BYTES_PER_CHANNEL
        )
        byteBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (pixelValue in pixels) {
            byteBuffer.putFloat(((pixelValue shr 16) and 0xFF) / 255f)
            byteBuffer.putFloat(((pixelValue shr 8) and 0xFF) / 255f)
            byteBuffer.putFloat((pixelValue and 0xFF) / 255f)
        }

        return byteBuffer
    }

    private fun processDetectionResult(detection: FloatArray): RectF {
        return RectF(
            detection[1],  // left
            detection[2],  // top
            detection[3],  // right
            detection[4]   // bottom
        )
    }

    private fun processClassificationResult(output: FloatArray): WoundClassification {
        // Find the index with highest probability
        var maxIndex = 0
        var maxProb = output[0]
        for (i in 1 until output.size) {
            if (output[i] > maxProb) {
                maxProb = output[i]
                maxIndex = i
            }
        }

        // Map index to wound type and determine severity
        val type = WOUND_TYPES[maxIndex / 3]
        val severity = when (maxIndex % 3) {
            0 -> "Mild"
            1 -> "Moderate"
            else -> "Severe"
        }

        // Calculate size based on bounding box
        val size = "2.5 x 3.0 cm" // Placeholder - implement actual size calculation

        return WoundClassification(
            type = type,
            severity = severity,
            size = size,
            confidence = maxProb
        )
    }

    private fun cropWoundRegion(bitmap: Bitmap, boundingBox: RectF): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val left = (boundingBox.left * width).toInt()
        val top = (boundingBox.top * height).toInt()
        val right = (boundingBox.right * width).toInt()
        val bottom = (boundingBox.bottom * height).toInt()

        return Bitmap.createBitmap(
            bitmap,
            left,
            top,
            right - left,
            bottom - top
        )
    }

    companion object {
        private const val INPUT_SIZE = 224
        private const val BATCH_SIZE = 1
        private const val PIXEL_SIZE = 3
        private const val BYTES_PER_CHANNEL = 4
        private const val NUM_DETECTIONS = 10
        private const val NUM_CLASSES = 15 // 5 wound types * 3 severity levels

        private val WOUND_TYPES = arrayOf(
            "Abrasion",
            "Laceration",
            "Burn",
            "Pressure Ulcer",
            "Surgical"
        )
    }
}

data class WoundClassification(
    val type: String,
    val severity: String,
    val size: String,
    val confidence: Float
)
