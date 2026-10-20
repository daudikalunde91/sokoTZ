package com.example.sokotz.data.remote

import android.util.Log
import com.example.BuildConfig
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
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val role: String? = null,
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String
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
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
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
        marketplaceContext: String = ""
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val trimmed = userMessage.trim()

        val systemPrompt = """
            Wewe ni sokoTZ Msaidizi (sokoTZ AI) — msaidizi rasmi wa kidijitali wa jukwaa kuu la SokoTZ nchini Tanzania.
            Lugha zako kuu ni Kiswahili na Kiingereza. Jibu kwa ufasaha kulingana na lugha anayotumia mteja.
            Majukumu yako:
            1. Kusaidia wanunuzi na wateja kupata biashara zilizothibitishwa, bidhaa za maduka ya Kariakoo, Arusha, Mwanza na nchi nzima.
            2. Kusaidia wafanyabiashara kutunga maelezo ya kuvutia ya matangazo (product sales copy).
            3. Kueleza bei katika Shilingi ya Kitanzania (TZS).
            4. Kutumia takwimu halisi za soko zilizopo:
            $marketplaceContext
            Kuwa mchangamfu, mwenye heshima na kutoa mwongozo sahihi. Usitumie jina la Gemini popote, jina lako ni sokoTZ AI au sokoTZ Msaidizi.
        """.trimIndent()

        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
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

            // Append latest user message
            contents.add(
                GeminiContent(
                    role = "user",
                    parts = listOf(GeminiPart(text = trimmed))
                )
            )

            val request = GeminiRequest(
                contents = contents,
                systemInstruction = GeminiContent(
                    role = "system",
                    parts = listOf(GeminiPart(text = systemPrompt))
                )
            )

            // Prioritize stable gemini-2.5-flash, then gemini-3.1-flash-lite-preview, then gemini-flash-latest
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
                        // If 503 Service Unavailable or 429 Too Many Requests, short delay before next try
                        if (e.message?.contains("503") == true || e.message?.contains("429") == true) {
                            kotlinx.coroutines.delay(400)
                        } else {
                            break // non-retryable for this model, try next model
                        }
                    }
                }
            }
        }

        // Resilient Fallback: Always returns an intelligent, structured response even if remote API returns 503
        generateLocalAssistantResponse(trimmed, marketplaceContext)
    }

    private fun generateLocalAssistantResponse(userMsg: String, context: String): String {
        val q = userMsg.lowercase()
        val isSw = !q.contains("hello") && !q.contains("price of") && !q.contains("what is") && !q.contains("how much")

        // Draft copy request
        if (q.contains("andika") || q.contains("tunga") || q.contains("draft") || q.contains("maelezo") || q.contains("tangazo")) {
            val subject = userMsg.replace(Regex("(?i)andika|tunga|tangazo|maelezo|draft|ya|kuhusu"), "").trim()
            return if (isSw) {
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
                🔥 *Hot Deal on SokoTZ: $subject* 🔥

                Looking for premium quality ${subject.ifBlank { "authentic products" }} in Tanzania?
                ✨ High quality, verified sellers, and great customer care.
                🚚 Nationwide delivery across Tanzania.
                📞 Tap the Call or WhatsApp button on this listing now to secure yours today!

                _SokoTZ — Tanzania's Premier Marketplace._
                """.trimIndent()
            }
        }

        // Inquiry about businesses or products
        return if (isSw) {
            """
            Habari! Mimi ni sokoTZ Msaidizi. 
            Niko hapa kukusaidia kupata wauzaji waaminifu, bidhaa halisi za Kariakoo na mikoani, pamoja na kutoa maelezo ya kina ya bei katika Shilingi ya Kitanzania (TZS).

            Unaweza kunitumia jina la bidhaa (k.m. "simu ya Samsung", "nguo za kike", "spea za gari") au kuniomba:
            👉 *"Nitafutie maduka ya elektroniki Kariakoo"*
            👉 *"Andika maelezo ya tangazo la viatu"*
            """.trimIndent()
        } else {
            """
            Hello! I am sokoTZ AI.
            I can help you find verified sellers, authentic products across Kariakoo and Tanzanian regions, and check prices in Tanzanian Shillings (TZS).

            Feel free to ask me to search the catalog or generate sales copy for your store!
            """.trimIndent()
        }
    }
}
