package com.georacing.georacing.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Image compression utilities to prevent ANRs and OOM errors.
 * Designed for handling high-resolution camera photos in a memory-safe way.
 */
object ImageUtils {
    
    private const val MAX_DIMENSION = 1024
    private const val JPEG_QUALITY = 60
    
    /**
     * Compresses an image from URI to a ByteArray.
     * - Resizes to max 1024x1024 maintaining aspect ratio
     * - Compresses to JPEG at 60% quality
     * - Handles EXIF rotation
     * 
     * @param context Application context for content resolver
     * @param uri URI of the image to compress
     * @return Compressed image as ByteArray, or null on failure
     */
    suspend fun compressImage(context: Context, uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
        try {
            // 1. Decode bounds only first (memory efficient)
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, options)
            }
            
            val originalWidth = options.outWidth
            val originalHeight = options.outHeight
            
            if (originalWidth <= 0 || originalHeight <= 0) {
                return@withContext null
            }
            
            // 2. Calculate sample size for memory-efficient loading
            val sampleSize = calculateSampleSize(originalWidth, originalHeight, MAX_DIMENSION)
            
            // 3. Decode with sample size
            val decodingOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565 // Uses less memory
            }
            
            val bitmap = context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, decodingOptions)
            } ?: return@withContext null
            
            // 4. Handle EXIF rotation
            val rotatedBitmap = handleExifRotation(context, uri, bitmap)
            
            // 5. Scale to exact max dimension if still larger
            val scaledBitmap = scaleToMaxDimension(rotatedBitmap, MAX_DIMENSION)
            if (scaledBitmap !== rotatedBitmap) {
                rotatedBitmap.recycle()
            }
            
            // 6. Compress to JPEG ByteArray
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
            
            // Cleanup
            if (scaledBitmap !== bitmap) {
                scaledBitmap.recycle()
            }
            bitmap.recycle()
            
            outputStream.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun calculateSampleSize(width: Int, height: Int, targetMax: Int): Int {
        var sampleSize = 1
        val maxDimension = maxOf(width, height)
        
        while (maxDimension / sampleSize > targetMax * 2) {
            sampleSize *= 2
        }
        
        return sampleSize
    }
    
    private fun handleExifRotation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val exif = inputStream?.let { ExifInterface(it) }
            inputStream?.close()
            
            val orientation = exif?.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            ) ?: ExifInterface.ORIENTATION_NORMAL
            
            val rotation = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            
            if (rotation != 0f) {
                val matrix = Matrix().apply { postRotate(rotation) }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }
        } catch (e: Exception) {
            bitmap
        }
    }
    
    private fun scaleToMaxDimension(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        if (width <= maxDimension && height <= maxDimension) {
            return bitmap
        }
        
        val ratio = minOf(
            maxDimension.toFloat() / width,
            maxDimension.toFloat() / height
        )
        
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}
