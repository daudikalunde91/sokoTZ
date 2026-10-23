package com.example.sokotz.data.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

data class ParsedAttachment(
    val name: String,
    val mimeType: String,
    val base64Data: String? = null,
    val extractedText: String? = null,
    val previewBitmap: Bitmap? = null
)

object DocumentReader {

    fun getFileName(context: Context, uri: Uri): String {
        return try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (index != -1) return it.getString(index)
                }
            }
            uri.lastPathSegment ?: "document_${System.currentTimeMillis()}"
        } catch (e: Exception) {
            "document_${System.currentTimeMillis()}"
        }
    }

    fun parseAttachment(context: Context, uri: Uri): ParsedAttachment? {
        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
        val fileName = getFileName(context, uri)

        return try {
            when {
                mimeType.startsWith("image/") -> {
                    val stream = contentResolver.openInputStream(uri)
                    val original = BitmapFactory.decodeStream(stream) ?: return null
                    val scaled = scaleBitmap(original, 1024)
                    val base64 = bitmapToBase64(scaled)
                    ParsedAttachment(
                        name = fileName,
                        mimeType = "image/jpeg",
                        base64Data = base64,
                        previewBitmap = scaled
                    )
                }
                mimeType == "application/pdf" || fileName.endsWith(".pdf", ignoreCase = true) -> {
                    // Render first page of PDF using Android PdfRenderer
                    val tempFile = File(context.cacheDir, "temp_pdf_${System.currentTimeMillis()}.pdf")
                    contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    val fileDescriptor = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
                    val pdfRenderer = PdfRenderer(fileDescriptor)
                    if (pdfRenderer.pageCount > 0) {
                        val page = pdfRenderer.openPage(0)
                        val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        page.close()
                        pdfRenderer.close()
                        fileDescriptor.close()
                        tempFile.delete()

                        val scaled = scaleBitmap(bitmap, 1024)
                        val base64 = bitmapToBase64(scaled)
                        ParsedAttachment(
                            name = fileName,
                            mimeType = "image/jpeg",
                            base64Data = base64,
                            previewBitmap = scaled
                        )
                    } else {
                        pdfRenderer.close()
                        fileDescriptor.close()
                        tempFile.delete()
                        ParsedAttachment(
                            name = fileName,
                            mimeType = "application/pdf",
                            extractedText = "PDF Document: $fileName"
                        )
                    }
                }
                mimeType.startsWith("text/") || fileName.endsWith(".txt", ignoreCase = true) ||
                fileName.endsWith(".csv", ignoreCase = true) || fileName.endsWith(".json", ignoreCase = true) -> {
                    val text = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                    ParsedAttachment(
                        name = fileName,
                        mimeType = "text/plain",
                        extractedText = text.take(5000)
                    )
                }
                else -> {
                    // Try decoding as image fallback
                    val stream = contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(stream)
                    if (bitmap != null) {
                        val scaled = scaleBitmap(bitmap, 1024)
                        ParsedAttachment(
                            name = fileName,
                            mimeType = "image/jpeg",
                            base64Data = bitmapToBase64(scaled),
                            previewBitmap = scaled
                        )
                    } else {
                        ParsedAttachment(
                            name = fileName,
                            mimeType = mimeType,
                            extractedText = "Uploaded File: $fileName"
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("DocumentReader", "Error parsing attachment $uri: ${e.message}", e)
            null
        }
    }

    private fun scaleBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap

        val ratio = width.toFloat() / height.toFloat()
        val targetWidth: Int
        val targetHeight: Int
        if (width > height) {
            targetWidth = maxDimension
            targetHeight = (maxDimension / ratio).toInt()
        } else {
            targetHeight = maxDimension
            targetWidth = (maxDimension * ratio).toInt()
        }
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        val byteArray = stream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
}
