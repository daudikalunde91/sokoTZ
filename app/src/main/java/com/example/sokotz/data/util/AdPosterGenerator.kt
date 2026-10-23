package com.example.sokotz.data.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.Locale

object AdPosterGenerator {

    fun generatePoster(
        context: Context,
        title: String,
        priceTzs: Long,
        businessName: String,
        city: String,
        phoneNumber: String,
        categoryName: String,
        description: String = ""
    ): Bitmap {
        val width = 1080
        val height = 1080
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Modern Deep Navy Gradient Background
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                intArrayOf(Color.parseColor("#080B18"), Color.parseColor("#0D1530"), Color.parseColor("#111B3A")),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 2. Decorative Top Glowing Wave / Bar
        val accentPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), 0f,
                intArrayOf(Color.parseColor("#38BDF8"), Color.parseColor("#8B5CF6"), Color.parseColor("#3B82F6")),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), 20f, accentPaint)

        // 3. SokoTZ Brand Header
        val brandBoxPaint = Paint().apply {
            color = Color.parseColor("#1E2D5A")
            isAntiAlias = true
        }
        val headerRect = RectF(60f, 60f, 320f, 130f)
        canvas.drawRoundRect(headerRect, 20f, 20f, brandBoxPaint)

        val brandBorderPaint = Paint().apply {
            color = Color.parseColor("#38BDF8")
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }
        canvas.drawRoundRect(headerRect, 20f, 20f, brandBorderPaint)

        val brandTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 42f
            isFakeBoldText = true
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText("SokoTZ", 95f, 110f, brandTextPaint)

        // Tagline badge on the right
        val verifiedBadgePaint = Paint().apply {
            color = Color.parseColor("#10B981")
            isAntiAlias = true
        }
        val verifiedRect = RectF(width - 420f, 60f, width - 60f, 130f)
        canvas.drawRoundRect(verifiedRect, 20f, 20f, verifiedBadgePaint)

        val verifiedTextPaint = Paint().apply {
            color = Color.BLACK
            textSize = 28f
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText("BIASHARA ILIYOTHIBITISHWA", width - 400f, 105f, verifiedTextPaint)

        // 4. Category Tag
        val catTextPaint = Paint().apply {
            color = Color.parseColor("#38BDF8")
            textSize = 34f
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText(categoryName.uppercase(Locale.ROOT), 60f, 210f, catTextPaint)

        // 5. Product / Service Title (Wrap if long)
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 62f
            isFakeBoldText = true
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
        }

        val words = title.split(" ")
        var line = ""
        var y = 290f
        for (w in words) {
            val testLine = if (line.isEmpty()) w else "$line $w"
            if (titlePaint.measureText(testLine) > (width - 120)) {
                canvas.drawText(line, 60f, y, titlePaint)
                line = w
                y += 75f
            } else {
                line = testLine
            }
        }
        if (line.isNotEmpty()) {
            canvas.drawText(line, 60f, y, titlePaint)
            y += 85f
        }

        // 6. Central Card for Price & Info
        val cardPaint = Paint().apply {
            color = Color.parseColor("#0F1A38")
            isAntiAlias = true
        }
        val cardBorder = Paint().apply {
            color = Color.parseColor("#253A6E")
            style = Paint.Style.STROKE
            strokeWidth = 4f
            isAntiAlias = true
        }
        val cardRect = RectF(60f, y, width - 60f, y + 260f)
        canvas.drawRoundRect(cardRect, 30f, 30f, cardPaint)
        canvas.drawRoundRect(cardRect, 30f, 30f, cardBorder)

        // Price Label
        val priceLabelPaint = Paint().apply {
            color = Color.parseColor("#A8B3CC")
            textSize = 32f
            isAntiAlias = true
        }
        canvas.drawText("BEI YA OFA MAALUM:", 100f, y + 70f, priceLabelPaint)

        // Price Big Text
        val formattedPrice = NumberFormat.getNumberInstance(Locale.US).format(priceTzs)
        val pricePaint = Paint().apply {
            color = Color.parseColor("#38BDF8")
            textSize = 72f
            isFakeBoldText = true
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText("TZS $formattedPrice", 100f, y + 160f, pricePaint)

        // Location & Store
        val locationPaint = Paint().apply {
            color = Color.parseColor("#F4F7FF")
            textSize = 36f
            isAntiAlias = true
        }
        val locText = "Duka: $businessName • $city"
        canvas.drawText(locText.take(45), 100f, y + 225f, locationPaint)

        // 7. Description snippet if present
        if (description.isNotBlank()) {
            val descPaint = Paint().apply {
                color = Color.parseColor("#CBD5E1")
                textSize = 32f
                isAntiAlias = true
            }
            val shortDesc = description.take(130) + if (description.length > 130) "..." else ""
            canvas.drawText(shortDesc, 60f, y + 330f, descPaint)
        }

        // 8. Contact CTA Footer Box
        val footerPaint = Paint().apply {
            shader = LinearGradient(
                0f, height - 200f, width.toFloat(), height.toFloat(),
                intArrayOf(Color.parseColor("#25D366"), Color.parseColor("#128C7E")),
                null,
                Shader.TileMode.CLAMP
            )
            isAntiAlias = true
        }
        val footerRect = RectF(60f, height - 190f, width - 60f, height - 50f)
        canvas.drawRoundRect(footerRect, 24f, 24f, footerPaint)

        val ctaPaint = Paint().apply {
            color = Color.WHITE
            textSize = 44f
            isFakeBoldText = true
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
        }
        val contactText = "Wasiliana Sasa: $phoneNumber"
        val ctaWidth = ctaPaint.measureText(contactText)
        canvas.drawText(contactText, (width - ctaWidth) / 2f, height - 105f, ctaPaint)

        return bitmap
    }

    fun savePosterToCache(context: Context, bitmap: Bitmap, fileName: String = "ad_poster_${System.currentTimeMillis()}.jpg"): File {
        val file = File(context.cacheDir, fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        return file
    }

    fun generatePoster(context: Context, listing: com.example.sokotz.data.model.Listing): String {
        val bitmap = generatePoster(
            context = context,
            title = listing.titleSw.ifBlank { listing.titleEn },
            priceTzs = listing.priceTzs,
            businessName = listing.businessName,
            city = listing.city,
            phoneNumber = listing.phoneNumber,
            categoryName = listing.categorySlug.replace("-", " ").replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() },
            description = listing.descriptionSw.ifBlank { listing.descriptionEn }
        )
        val file = savePosterToCache(context, bitmap, "sokotz_poster_${listing.id}.jpg")
        return file.absolutePath
    }
}
