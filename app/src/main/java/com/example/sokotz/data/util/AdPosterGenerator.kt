package com.example.sokotz.data.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.example.sokotz.data.model.Listing
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.Locale

enum class PosterTheme(
    val titleSw: String,
    val titleEn: String,
    val bgStart: Int,
    val bgMid: Int,
    val bgEnd: Int,
    val accentStart: Int,
    val accentEnd: Int,
    val cardBg: Int,
    val cardBorder: Int,
    val ctaStart: Int,
    val ctaEnd: Int
) {
    NAVY_CYAN(
        titleSw = "Bahari (Navy Cyan)",
        titleEn = "Navy Cyan",
        bgStart = Color.parseColor("#080B18"),
        bgMid = Color.parseColor("#0D1530"),
        bgEnd = Color.parseColor("#111B3A"),
        accentStart = Color.parseColor("#38BDF8"),
        accentEnd = Color.parseColor("#8B5CF6"),
        cardBg = Color.parseColor("#0F1A38"),
        cardBorder = Color.parseColor("#253A6E"),
        ctaStart = Color.parseColor("#25D366"),
        ctaEnd = Color.parseColor("#128C7E")
    ),
    EMERALD_GOLD(
        titleSw = "Kijani & Dhahabu (Emerald)",
        titleEn = "Emerald Gold",
        bgStart = Color.parseColor("#042F2E"),
        bgMid = Color.parseColor("#064E3B"),
        bgEnd = Color.parseColor("#065F46"),
        accentStart = Color.parseColor("#F59E0B"),
        accentEnd = Color.parseColor("#10B981"),
        cardBg = Color.parseColor("#064E3B"),
        cardBorder = Color.parseColor("#10B981"),
        ctaStart = Color.parseColor("#F59E0B"),
        ctaEnd = Color.parseColor("#D97706")
    ),
    SUNSET_WARM(
        titleSw = "Machweo (Sunset Orange)",
        titleEn = "Sunset Warm",
        bgStart = Color.parseColor("#3A0B0B"),
        bgMid = Color.parseColor("#5A1313"),
        bgEnd = Color.parseColor("#7A1C1C"),
        accentStart = Color.parseColor("#FB923C"),
        accentEnd = Color.parseColor("#FBBF24"),
        cardBg = Color.parseColor("#4C1111"),
        cardBorder = Color.parseColor("#F97316"),
        ctaStart = Color.parseColor("#22C55E"),
        ctaEnd = Color.parseColor("#16A34A")
    ),
    ROYAL_VIOLET(
        titleSw = "Zambarau (Royal Violet)",
        titleEn = "Royal Violet",
        bgStart = Color.parseColor("#180A2E"),
        bgMid = Color.parseColor("#2A0E4E"),
        bgEnd = Color.parseColor("#3B126D"),
        accentStart = Color.parseColor("#C084FC"),
        accentEnd = Color.parseColor("#38BDF8"),
        cardBg = Color.parseColor("#2A0E4E"),
        cardBorder = Color.parseColor("#A855F7"),
        ctaStart = Color.parseColor("#38BDF8"),
        ctaEnd = Color.parseColor("#2563EB")
    )
}

object AdPosterGenerator {

    fun generatePoster(
        context: Context,
        title: String,
        priceTzs: Long,
        businessName: String,
        city: String,
        phoneNumber: String,
        categoryName: String,
        description: String = "",
        theme: PosterTheme = PosterTheme.NAVY_CYAN,
        promoBadge: String = "BIASHARA ILIYOTHIBITISHWA"
    ): Bitmap {
        val width = 1080
        val height = 1080
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Gradient Background
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                intArrayOf(theme.bgStart, theme.bgMid, theme.bgEnd),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 2. Decorative Top Glowing Wave / Bar
        val accentPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), 0f,
                intArrayOf(theme.accentStart, theme.accentEnd, theme.accentStart),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), 20f, accentPaint)

        // 3. SokoTZ Brand Header Pill
        val brandBoxPaint = Paint().apply {
            color = Color.parseColor("#1A2448")
            isAntiAlias = true
        }
        val headerRect = RectF(60f, 60f, 320f, 130f)
        canvas.drawRoundRect(headerRect, 20f, 20f, brandBoxPaint)

        val brandBorderPaint = Paint().apply {
            color = theme.accentStart
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
        val badgeWidth = 430f
        val verifiedRect = RectF(width - 60f - badgeWidth, 60f, width - 60f, 130f)
        canvas.drawRoundRect(verifiedRect, 20f, 20f, verifiedBadgePaint)

        val verifiedTextPaint = Paint().apply {
            color = Color.BLACK
            textSize = 26f
            isFakeBoldText = true
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
        }
        val badgeText = promoBadge.uppercase(Locale.ROOT).take(26)
        val textWidth = verifiedTextPaint.measureText(badgeText)
        val textX = (verifiedRect.left + (badgeWidth - textWidth) / 2f)
        canvas.drawText(badgeText, textX, 105f, verifiedTextPaint)

        // 4. Category Tag
        val catTextPaint = Paint().apply {
            color = theme.accentStart
            textSize = 34f
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText(categoryName.uppercase(Locale.ROOT), 60f, 205f, catTextPaint)

        // 5. Product / Service Title (Wrap if long)
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 62f
            isFakeBoldText = true
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
        }

        val words = title.trim().split(" ")
        var line = ""
        var y = 285f
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
            color = theme.cardBg
            isAntiAlias = true
        }
        val cardBorder = Paint().apply {
            color = theme.cardBorder
            style = Paint.Style.STROKE
            strokeWidth = 4f
            isAntiAlias = true
        }
        val cardRect = RectF(60f, y, width - 60f, y + 260f)
        canvas.drawRoundRect(cardRect, 30f, 30f, cardPaint)
        canvas.drawRoundRect(cardRect, 30f, 30f, cardBorder)

        // Price Label
        val priceLabelPaint = Paint().apply {
            color = Color.parseColor("#CBD5E1")
            textSize = 32f
            isAntiAlias = true
        }
        canvas.drawText("BEI YA OFA MAALUM:", 100f, y + 70f, priceLabelPaint)

        // Price Big Text
        val formattedPrice = NumberFormat.getNumberInstance(Locale.US).format(priceTzs)
        val pricePaint = Paint().apply {
            color = theme.accentStart
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
                color = Color.parseColor("#E2E8F0")
                textSize = 32f
                isAntiAlias = true
            }
            val shortDesc = description.take(130) + if (description.length > 130) "..." else ""
            canvas.drawText(shortDesc, 60f, y + 330f, descPaint)
        }

        // 8. Contact CTA Footer Box
        val footerPaint = Paint().apply {
            shader = LinearGradient(
                0f, height - 190f, width.toFloat(), height.toFloat(),
                intArrayOf(theme.ctaStart, theme.ctaEnd),
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
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
        return file
    }

    fun savePosterToGallery(context: Context, bitmap: Bitmap, fileName: String = "SokoTZ_Poster_${System.currentTimeMillis()}.jpg"): Uri? {
        return try {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/SokoTZ")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }
            }
            uri
        } catch (e: Exception) {
            Log.e("AdPosterGenerator", "Failed to save poster to gallery", e)
            null
        }
    }

    fun generatePoster(
        context: Context,
        listing: Listing,
        theme: PosterTheme = PosterTheme.NAVY_CYAN
    ): String {
        val bitmap = generatePoster(
            context = context,
            title = listing.titleSw.ifBlank { listing.titleEn },
            priceTzs = listing.priceTzs,
            businessName = listing.businessName,
            city = listing.city,
            phoneNumber = listing.phoneNumber,
            categoryName = listing.categorySlug.replace("-", " ").replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() },
            description = listing.descriptionSw.ifBlank { listing.descriptionEn },
            theme = theme
        )
        val file = savePosterToCache(context, bitmap, "sokotz_poster_${listing.id}.jpg")
        return file.absolutePath
    }
}
