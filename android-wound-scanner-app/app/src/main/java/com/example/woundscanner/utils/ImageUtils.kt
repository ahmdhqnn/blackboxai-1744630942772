package com.example.woundscanner.utils

import android.content.Context
import android.graphics.*
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object ImageUtils {
    private const val JPEG_QUALITY = 90
    private const val MAX_IMAGE_DIMENSION = 1920
    private const val TIMESTAMP_FORMAT = "yyyyMMdd_HHmmss"
    private const val FILE_PREFIX = "WOUND_"
    private const val FILE_SUFFIX = ".jpg"

    /**
     * Create a temporary file for storing camera images
     */
    fun createImageFile(context: Context): File {
        val timeStamp = SimpleDateFormat(TIMESTAMP_FORMAT, Locale.getDefault()).format(Date())
        val fileName = "${FILE_PREFIX}${timeStamp}"
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(fileName, FILE_SUFFIX, storageDir)
    }

    /**
     * Get content URI for the image file using FileProvider
     */
    fun getUriForFile(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /**
     * Load bitmap from URI with proper rotation
     */
    fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            // Open input stream from URI
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                // Decode bounds first to determine image size
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(inputStream, null, options)

                // Calculate sample size for downscaling if needed
                val sampleSize = calculateSampleSize(
                    options.outWidth,
                    options.outHeight,
                    MAX_IMAGE_DIMENSION
                )

                // Reopen stream and decode full image with sample size
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.Options().apply {
                        inSampleSize = sampleSize
                    }.let { decodeOptions ->
                        BitmapFactory.decodeStream(stream, null, decodeOptions)
                    }
                }?.let { bitmap ->
                    // Fix orientation if needed
                    fixOrientation(context, uri, bitmap)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Save bitmap to a file
     */
    fun saveBitmapToFile(bitmap: Bitmap, file: File): Boolean {
        return try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Delete image file
     */
    fun deleteImage(context: Context, uri: Uri) {
        try {
            context.contentResolver.delete(uri, null, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Calculate sample size for downscaling large images
     */
    private fun calculateSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var sampleSize = 1
        if (width > maxDimension || height > maxDimension) {
            val halfWidth = width / 2
            val halfHeight = height / 2
            while ((halfWidth / sampleSize) >= maxDimension &&
                (halfHeight / sampleSize) >= maxDimension) {
                sampleSize *= 2
            }
        }
        return sampleSize
    }

    /**
     * Fix image orientation based on EXIF data
     */
    private fun fixOrientation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        val orientation = getOrientation(context, uri)
        if (orientation == 0) return bitmap

        val matrix = Matrix()
        matrix.postRotate(orientation.toFloat())
        return Bitmap.createBitmap(
            bitmap, 0, 0,
            bitmap.width, bitmap.height,
            matrix, true
        )
    }

    /**
     * Get image orientation from EXIF data
     */
    private fun getOrientation(context: Context, uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val exif = android.media.ExifInterface(input)
                when (exif.getAttributeInt(
                    android.media.ExifInterface.TAG_ORIENTATION,
                    android.media.ExifInterface.ORIENTATION_NORMAL
                )) {
                    android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } ?: 0
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    /**
     * Resize bitmap maintaining aspect ratio
     */
    fun resizeBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxDimension && height <= maxDimension) {
            return bitmap
        }

        val ratio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int

        if (width > height) {
            newWidth = maxDimension
            newHeight = (maxDimension / ratio).toInt()
        } else {
            newHeight = maxDimension
            newWidth = (maxDimension * ratio).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Draw bounding box on bitmap
     */
    fun drawBoundingBox(
        bitmap: Bitmap,
        rect: RectF,
        color: Int = Color.RED,
        strokeWidth: Float = 4f
    ): Bitmap {
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        val paint = Paint().apply {
            this.color = color
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            isAntiAlias = true
        }

        canvas.drawRect(
            rect.left * bitmap.width,
            rect.top * bitmap.height,
            rect.right * bitmap.width,
            rect.bottom * bitmap.height,
            paint
        )

        return mutableBitmap
    }

    /**
     * Calculate image dimensions in centimeters
     */
    fun calculateDimensions(
        bitmap: Bitmap,
        boundingBox: RectF,
        referenceObject: ReferenceObject
    ): Dimensions {
        val pixelsPerCm = referenceObject.getPixelsPerCm(bitmap)
        val widthCm = boundingBox.width() * bitmap.width / pixelsPerCm
        val heightCm = boundingBox.height() * bitmap.height / pixelsPerCm
        return Dimensions(widthCm, heightCm)
    }

    data class Dimensions(
        val width: Float,
        val height: Float
    ) {
        override fun toString(): String = "%.1f x %.1f cm".format(width, height)
    }

    data class ReferenceObject(
        val widthCm: Float,
        val heightCm: Float,
        val boundingBox: RectF
    ) {
        fun getPixelsPerCm(bitmap: Bitmap): Float {
            val pixelWidth = boundingBox.width() * bitmap.width
            return pixelWidth / widthCm
        }
    }
}
