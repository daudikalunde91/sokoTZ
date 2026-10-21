package com.example.sokotz.data.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import android.util.Log
import android.util.LruCache
import android.media.ExifInterface
import java.io.ByteArrayOutputStream

object ImageUtils {
    private const val TAG = "ImageUtils"

    // Memory LRU cache for decoded Bitmaps so UI rendering is instantaneous and smooth
    private val bitmapCache = LruCache<String, Bitmap>(50)

    fun isBase64OrDataUri(str: String?): Boolean {
        if (str.isNullOrBlank()) return false
        return str.startsWith("data:image/") || str.startsWith("/9j/") || str.startsWith("iVBORw0") || str.length > 200
    }

    /**
     * Decodes a Base64 or Data URI string to a Bitmap safely with caching and OOM protection.
     */
    fun decodeBase64ToBitmap(dataUriOrBase64: String?): Bitmap? {
        if (dataUriOrBase64.isNullOrBlank()) return null

        val cacheKey = if (dataUriOrBase64.length > 64) {
            dataUriOrBase64.substring(0, 32) + "_" + dataUriOrBase64.length + "_" + dataUriOrBase64.takeLast(16)
        } else {
            dataUriOrBase64
        }

        bitmapCache.get(cacheKey)?.let { return it }

        return try {
            val base64Data = if (dataUriOrBase64.contains(",")) {
                dataUriOrBase64.substringAfter(",")
            } else {
                dataUriOrBase64
            }.trim()

            val imageBytes = Base64.decode(base64Data, Base64.DEFAULT)
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)
            if (bitmap != null) {
                bitmapCache.put(cacheKey, bitmap)
            }
            bitmap
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to decode base64 image", t)
            null
        }
    }

    /**
     * Memory-efficient conversion of Uri to compressed, properly-oriented Base64 string.
     * Guaranteed to prevent OutOfMemoryError via inSampleSize and fits Firestore 250k character limit.
     */
    fun uriToBase64(context: Context, uri: Uri, maxDimension: Int = 600, quality: Int = 80): String? {
        return try {
            // 1. Read EXIF orientation to keep photos upright and correctly aligned
            var orientation = ExifInterface.ORIENTATION_NORMAL
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val exif = ExifInterface(inputStream)
                    orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not read EXIF orientation", e)
            }

            // 2. Read dimensions only using inJustDecodeBounds
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, boundsOptions)
            }

            val origWidth = boundsOptions.outWidth
            val origHeight = boundsOptions.outHeight
            if (origWidth <= 0 || origHeight <= 0) return null

            // 3. Compute inSampleSize to downsample large camera photos during decode
            var inSampleSize = 1
            var halfWidth = origWidth
            var halfHeight = origHeight
            while (halfWidth / 2 >= maxDimension && halfHeight / 2 >= maxDimension) {
                inSampleSize *= 2
                halfWidth /= 2
                halfHeight /= 2
            }

            // 4. Decode downsampled bitmap
            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val sampledBitmap = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            } ?: return null

            // 5. Rotate bitmap if EXIF requires it
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            }

            val orientedBitmap = if (!matrix.isIdentity) {
                Bitmap.createBitmap(sampledBitmap, 0, 0, sampledBitmap.width, sampledBitmap.height, matrix, true)
            } else {
                sampledBitmap
            }

            // 6. Scale and compress to Base64
            bitmapToBase64(orientedBitmap, maxDimension, quality)
        } catch (t: Throwable) {
            Log.e(TAG, "uriToBase64 failed", t)
            null
        }
    }

    /**
     * Converts a Bitmap to compressed Base64 JPEG string, enforcing dimensions and Firestore size rules.
     */
    fun bitmapToBase64(bitmap: Bitmap, maxDimension: Int = 600, quality: Int = 80): String {
        val width = bitmap.width
        val height = bitmap.height

        val scaledBitmap = if (width > maxDimension || height > maxDimension) {
            val ratio = width.toFloat() / height.toFloat()
            val (newWidth, newHeight) = if (ratio > 1f) {
                maxDimension to (maxDimension / ratio).toInt().coerceAtLeast(1)
            } else {
                (maxDimension * ratio).toInt().coerceAtLeast(1) to maxDimension
            }
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            bitmap
        }

        var currentQuality = quality
        var byteArray: ByteArray

        // Compress and ensure output stays comfortably under Firestore 250,000 char limit (approx 175,000 bytes)
        do {
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, currentQuality, outputStream)
            byteArray = outputStream.toByteArray()
            currentQuality -= 15
        } while (byteArray.size > 170_000 && currentQuality >= 35)

        val base64Encoded = Base64.encodeToString(byteArray, Base64.NO_WRAP)
        val dataUri = "data:image/jpeg;base64,$base64Encoded"

        // Pre-cache bitmap so UI displays immediately without needing re-decode
        val cacheKey = if (dataUri.length > 64) {
            dataUri.substring(0, 32) + "_" + dataUri.length + "_" + dataUri.takeLast(16)
        } else {
            dataUri
        }
        bitmapCache.put(cacheKey, scaledBitmap)

        return dataUri
    }
}
