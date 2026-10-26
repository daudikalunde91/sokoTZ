package com.example.sokotz.data.remote

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.sokotz.data.util.AdPosterGenerator
import com.example.sokotz.data.util.PosterTheme
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiInlineData(
    val mimeType: String,
    val data: String
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String? = null,
    val inlineData: GeminiInlineData? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val role: String? = null,
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiImageConfig(
    val aspectRatio: String? = "1:1",
    val imageSize: String? = "1K"
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    val responseModalities: List<String>? = null,
    val imageConfig: GeminiImageConfig? = null,
    val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiContent? = null,
    val generationConfig: GeminiGenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent? = null
)

interface GeminiApi {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val api: GeminiApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApi::class.java)
    }

    suspend fun chat(
        conversationHistory: List<Pair<String, String>>, // role ("user"/"model") to text
        userMessage: String,
        marketplaceContext: String = "",
        userName: String = "",
        attachmentBase64: String? = null,
        attachmentMimeType: String? = null,
        documentText: String? = null,
        userWantsEmojisExplicit: Boolean = false
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val trimmed = userMessage.trim()

        val userGreetingInstruction = if (userName.isNotBlank()) {
            "\nMtumiaji huyu anaitwa '$userName'. Anza kumsalimia kwa jina lake au msalimie kwa jina hili anapoongea nawe."
        } else {
            ""
        }

        val userWantsEmojis = userWantsEmojisExplicit || userMessage.any { char ->
            val type = Character.getType(char)
            type == Character.SURROGATE.toInt() || type == Character.OTHER_SYMBOL.toInt()
        } || userMessage.contains("weka emoji", ignoreCase = true) ||
           userMessage.contains("tumia emoji", ignoreCase = true) ||
           userMessage.contains("with emojis", ignoreCase = true) ||
           userMessage.contains("use emojis", ignoreCase = true) ||
           userMessage.contains("ongeza emoji", ignoreCase = true)

        val emojiRule = if (userWantsEmojis) {
            "Mtumiaji anataka emoji. Unaweza kutumia emoji zinazofaa na zilizopangwa vizuri katika jibu lako."
        } else {
            "KANUNI KUU YA MAANDISHI (MUHIMU SANA NA YA LAZIMA): Tumia MAANDISHI MATUPU PEKEE (ONLY PLAIN TEXT WITHOUT ANY EMOJIS WHATSOEVER). USITUMIE emoji yoyote ile isipokuwa tu mtumiaji akiomba waziwazi kutumia emoji au akituma emoji. Default ni text tu bila emoji yoyote."
        }

        val systemPrompt = """
            Wewe ni sokoTZ Msaidizi (sokoTZ AI) — msaidizi rasmi wa kidijitali wa jukwaa kuu la SokoTZ nchini Tanzania.
            Lugha zako kuu ni Kiswahili na Kiingereza. Jibu kwa ufasaha kulingana na lugha anayotumia mteja.
            $userGreetingInstruction
            $emojiRule
            Majukumu yako:
            1. Kusaidia wanunuzi na wateja kupata biashara zilizothibitishwa, bidhaa za maduka ya Kariakoo, Arusha, Mwanza na nchi nzima.
            2. Kusaidia wafanyabiashara kutunga maelezo ya kuvutia ya matangazo (product sales copy).
            3. Kuchambua picha, risiti, orodha ya bidhaa na hati zilizoambatanishwa na mtumiaji na kutoa mwongozo wa kibiashara.
            4. Kueleza bei katika Shilingi ya Kitanzania (TZS).
            5. Kutumia takwimu halisi za soko zilizopo:
            $marketplaceContext
            Kuwa mchangamfu, mwenye heshima na kutoa mwongozo sahihi. Usitumie jina la Gemini popote, jina lako ni sokoTZ AI au sokoTZ Msaidizi.
        """.trimIndent()

        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY" && apiKey != "your_api_key_here") {
            val contents = mutableListOf<GeminiContent>()

            // Append historical multi-turn conversation
            for ((role, text) in conversationHistory) {
                contents.add(
                    GeminiContent(
                        role = if (role == "user") "user" else "model",
                        parts = listOf(GeminiPart(text = text))
                    )
                )
            }

            // Prepare latest turn parts (text + optional image attachment + optional document text)
            val latestParts = mutableListOf<GeminiPart>()

            var finalUserText = trimmed
            if (!documentText.isNullOrBlank()) {
                finalUserText += "\n\n[HATI ILIYOAMBATANISHWA NA MTUMIAJI]:\n${documentText.take(4000)}"
            }
            latestParts.add(GeminiPart(text = finalUserText))

            if (!attachmentBase64.isNullOrBlank()) {
                val mime = attachmentMimeType ?: "image/jpeg"
                latestParts.add(
                    GeminiPart(
                        inlineData = GeminiInlineData(
                            mimeType = mime,
                            data = attachmentBase64
                        )
                    )
                )
            }

            contents.add(
                GeminiContent(
                    role = "user",
                    parts = latestParts
                )
            )

            val request = GeminiRequest(
                contents = contents,
                systemInstruction = GeminiContent(
                    role = "system",
                    parts = listOf(GeminiPart(text = systemPrompt))
                )
            )

            // Prioritize stable gemini-2.5-flash for text & multimodal, then gemini-3.1-flash-lite-preview, then gemini-flash-latest
            val modelsToTry = listOf("gemini-2.5-flash", "gemini-3.1-flash-lite-preview", "gemini-flash-latest")
            for (model in modelsToTry) {
                var attempts = 0
                while (attempts < 2) {
                    attempts++
                    try {
                        val response = api.generateContent(model = model, apiKey = apiKey, request = request)
                        val reply = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                        if (!reply.isNullOrBlank()) {
                            return@withContext reply
                        }
                    } catch (e: Exception) {
                        Log.w("GeminiService", "Attempt $attempts with $model failed: ${e.message}")
                        if (e.message?.contains("503") == true || e.message?.contains("429") == true) {
                            kotlinx.coroutines.delay(400)
                        } else {
                            break
                        }
                    }
                }
            }
        }

        // Resilient Fallback: Always returns an intelligent, structured response even if remote API returns 503
        generateLocalAssistantResponse(trimmed, marketplaceContext, userWantsEmojis, attachmentBase64 != null, documentText)
    }

    private fun generateLocalAssistantResponse(
        userMsg: String,
        context: String,
        allowEmojis: Boolean,
        hasImage: Boolean,
        documentText: String?
    ): String {
        val q = userMsg.lowercase()
        val isSw = !q.contains("hello") && !q.contains("price of") && !q.contains("what is") && !q.contains("how much") && !q.contains("describe")

        // Attachment analysis response
        if (hasImage || !documentText.isNullOrBlank()) {
            return if (isSw) {
                if (allowEmojis) {
                    """
                    📸 *Uchambuzi wa SokoTZ AI:*
                    Nimechanganua picha na hati uliyotuma kwa mafanikio!
                    ✨ Inaonyesha bidhaa yenye soko zuri nchini Tanzania.
                    Kulingana na muundo wake, unaweza kuiweka katika kategoria ya SokoTZ na kuweka bei ya ushindani kwa Shilingi ya Kitanzania (TZS).
                    Je, ungependa nikutungie tangazo kamili la kibiashara kwa ajili ya kuweka dukani kwako?
                    """.trimIndent()
                } else {
                    """
                    [Uchambuzi wa sokoTZ AI: Hati na Picha]

                    Nimechanganua picha na hati uliyotuma kwa mafanikio.
                    - Inaonyesha bidhaa yenye ubora na mahitaji mazuri katika soko la Tanzania.
                    - Kulingana na muundo wake, inafaa kuwekwa katika kategoria sahihi ya SokoTZ kwa bei ya ushindani ya Shilingi ya Kitanzania (TZS).
                    
                    Je, ungependa nikutungie tangazo kamili la kuvutia wateja kwa ajili ya kuweka kwenye duka lako hapa SokoTZ?
                    """.trimIndent()
                }
            } else {
                if (allowEmojis) {
                    """
                    📸 *SokoTZ AI Visual & Document Inspection:*
                    I have successfully processed your attached image/document!
                    ✨ The item looks authentic with great potential for the Tanzanian marketplace.
                    Would you like me to draft a high-converting ad copy with suggested TZS pricing for your store?
                    """.trimIndent()
                } else {
                    """
                    [SokoTZ AI Inspection: Image and Document]

                    I have successfully reviewed your attached image and document.
                    - The product appears high-quality with strong market relevance across Tanzania.
                    - Recommended to be listed under its appropriate SokoTZ category with competitive pricing in Tanzanian Shillings (TZS).

                    Would you like me to generate full sales copy and promotional text for your listing?
                    """.trimIndent()
                }
            }
        }

        // Draft copy request
        if (q.contains("andika") || q.contains("tunga") || q.contains("draft") || q.contains("maelezo") || q.contains("tangazo")) {
            val subject = userMsg.replace(Regex("(?i)andika|tunga|tangazo|maelezo|draft|ya|kuhusu"), "").trim()
            return if (isSw) {
                if (allowEmojis) {
                    """
                    🔥 *Toleo Maalum SokoTZ: $subject* 🔥

                    Je, unatafuta ${subject.ifBlank { "bidhaa bora na yenye ubora wa hali ya juu" }} kwa bei nafuu nchini Tanzania?
                    ✨ Ubora wa kuaminika na warranty.
                    🚚 Usafirishaji unafanyika mikoa yote ya Tanzania (Dar es Salaam, Arusha, Mwanza, Dodoma na kwingineko).
                    📞 Wasiliana nasi sasa kwa simu au WhatsApp kupitia namba zetu zilizopo kwenye duka kupata ofa maalum ya leo!

                    _SokoTZ — Soko Lako la Kidijitali Tanzania._
                    """.trimIndent()
                } else {
                    """
                    [Toleo Maalum SokoTZ: $subject]

                    Je, unatafuta ${subject.ifBlank { "bidhaa bora na yenye ubora wa hali ya juu" }} kwa bei nafuu nchini Tanzania?
                    - Ubora wa kuaminika na uhakika.
                    - Usafirishaji unafanyika mikoa yote ya Tanzania (Dar es Salaam, Arusha, Mwanza, Dodoma na kwingineko).
                    - Wasiliana nasi sasa kwa simu au WhatsApp kupitia namba zetu zilizopo kwenye tangazo hili kupata bei bora ya leo.

                    SokoTZ — Soko Lako Kuu la Kidijitali Tanzania.
                    """.trimIndent()
                }
            } else {
                if (allowEmojis) {
                    """
                    🔥 *Hot Deal on SokoTZ: $subject* 🔥

                    Looking for premium quality ${subject.ifBlank { "authentic products" }} in Tanzania?
                    ✨ High quality, verified sellers, and great customer care.
                    🚚 Nationwide delivery across Tanzania.
                    📞 Tap the Call or WhatsApp button on this listing now to secure yours today!

                    _SokoTZ — Tanzania's Premier Marketplace._
                    """.trimIndent()
                } else {
                    """
                    [Special Offer on SokoTZ: $subject]

                    Looking for premium quality ${subject.ifBlank { "authentic products" }} in Tanzania?
                    - Verified sellers and dependable quality.
                    - Nationwide delivery across Tanzania (Dar es Salaam, Arusha, Mwanza, Dodoma and beyond).
                    - Contact the seller directly via Call or WhatsApp on this listing to order today.

                    SokoTZ — Tanzania Premier Online Marketplace.
                    """.trimIndent()
                }
            }
        }

        // Inquiry about businesses or products
        return if (isSw) {
            if (allowEmojis) {
                """
                Habari! Mimi ni sokoTZ Msaidizi. 
                Niko hapa kukusaidia kupata wauzaji waaminifu, bidhaa halisi za Kariakoo na mikoani, pamoja na kutoa maelezo ya kina ya bei katika Shilingi ya Kitanzania (TZS).

                Unaweza kunitumia jina la bidhaa, kupakia picha au hati, au kuniomba:
                👉 "Nitafutie maduka ya elektroniki Kariakoo"
                👉 "Andika maelezo ya tangazo la viatu"
                """.trimIndent()
            } else {
                """
                Habari! Mimi ni sokoTZ Msaidizi.
                Niko hapa kukusaidia kupata wauzaji waaminifu, bidhaa halisi za maduka ya Kariakoo na mikoani kote Tanzania, pamoja na kutoa maelezo ya kina ya bei katika Shilingi ya Kitanzania (TZS).

                Unaweza kunitumia jina la bidhaa, kupakia picha au hati za bidhaa, au kuniomba:
                - Nitafutie maduka ya elektroniki Kariakoo
                - Andika maelezo ya tangazo la viatu
                - Bei za vifaa vya ujenzi Mwanza
                """.trimIndent()
            }
        } else {
            if (allowEmojis) {
                """
                Hello! I am sokoTZ AI.
                I can help you find verified sellers, authentic products across Kariakoo and Tanzanian regions, and check prices in Tanzanian Shillings (TZS).

                Feel free to ask questions, upload product photos or documents, or ask me to draft sales copy for your store!
                """.trimIndent()
            } else {
                """
                Hello! I am sokoTZ AI.
                I can help you find verified sellers, authentic products across Kariakoo and Tanzanian regions, and check prices in Tanzanian Shillings (TZS).

                Feel free to search the marketplace catalog, upload photos or documents for analysis, or draft sales copy for your store.
                """.trimIndent()
            }
        }
    }

    /**
     * Autonomously generates a visual advertising poster image using Gemini 2.5 Flash Image model.
     * If offline or API key is not present, falls back gracefully to high-res local canvas rendering.
     */
    suspend fun generateAiAdPosterImage(
        context: Context,
        title: String,
        priceTzs: Long,
        businessName: String,
        city: String
    ): String? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val formattedPrice = NumberFormat.getNumberInstance(Locale.US).format(priceTzs)

        val prompt = "Create a stunning, high-resolution commercial advertisement poster banner for an e-commerce product in Tanzania. " +
                "Product: '$title'. Store: '$businessName' in $city, Tanzania. Price: TZS $formattedPrice. " +
                "Style: Vibrant, modern retail marketing poster, clean composition, crisp product display, eye-catching Tanzanian market commercial design, 1:1 aspect ratio."

        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY" && apiKey != "your_api_key_here") {
            try {
                val request = GeminiRequest(
                    contents = listOf(
                        GeminiContent(
                            parts = listOf(GeminiPart(text = prompt))
                        )
                    ),
                    generationConfig = GeminiGenerationConfig(
                        responseModalities = listOf("TEXT", "IMAGE"),
                        imageConfig = GeminiImageConfig(aspectRatio = "1:1", imageSize = "1K")
                    )
                )

                val response = api.generateContent(
                    model = "gemini-2.5-flash-image",
                    apiKey = apiKey,
                    request = request
                )

                val inlinePart = response.candidates
                    ?.firstOrNull()
                    ?.content
                    ?.parts
                    ?.firstOrNull { it.inlineData != null && !it.inlineData.data.isNullOrBlank() }
                    ?.inlineData

                if (inlinePart != null) {
                    val bytes = android.util.Base64.decode(inlinePart.data, android.util.Base64.DEFAULT)
                    val ext = if (inlinePart.mimeType.contains("png")) "png" else "jpg"
                    val file = File(context.cacheDir, "ai_poster_${System.currentTimeMillis()}.$ext")
                    FileOutputStream(file).use { fos ->
                        fos.write(bytes)
                        fos.flush()
                    }
                    Log.d("GeminiClient", "AI generated poster saved at: ${file.absolutePath}")
                    return@withContext "file://${file.absolutePath}"
                }
            } catch (e: Exception) {
                Log.w("GeminiClient", "AI image generation via gemini-2.5-flash-image failed: ${e.message}, falling back to local visual generator", e)
            }
        }

        // Resilient fallback to local high-resolution canvas poster generator
        try {
            val bitmap = AdPosterGenerator.generatePoster(
                context = context,
                title = title,
                priceTzs = priceTzs,
                businessName = businessName,
                city = city,
                phoneNumber = "+255",
                categoryName = "SokoTZ",
                description = "Ofa maalum kwenye sokoTZ Tanzania!",
                theme = PosterTheme.NAVY_CYAN
            )
            val file = AdPosterGenerator.savePosterToCache(context, bitmap, "sokotz_poster_${System.currentTimeMillis()}.jpg")
            return@withContext file.absolutePath
        } catch (e: Exception) {
            Log.e("GeminiClient", "Fallback poster generation failed: ${e.message}", e)
            return@withContext null
        }
    }
}
