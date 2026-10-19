package com.example.sokotz.data.remote

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
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
    @POST("v1beta/models/gemini-3.8-flash:generateContent")
    suspend fun generateContent(
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
        marketplaceContext: String = ""
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Tafadhali weka Gemini API Key yako kwenye Secrets panel ya AI Studio ili kuwezesha majibu ya Gemini AI."
        }

        val systemPrompt = """
            Wewe ni SokoTZ Assistant — msaidizi rasmi wa kidijitali wa jukwaa la SokoTZ nchini Tanzania.
            Lugha zako kuu ni Kiswahili na Kiingereza. Jibu kulingana na lugha anayotumia mteja.
            Majukumu yako:
            1. Kusaidia wanunuzi na wateja kupata biashara zilizothibitishwa, bidhaa na mafundi nchini Tanzania.
            2. Kusaidia wafanyabiashara (sellers) kutunga na kuandika maelezo ya kuvutia ya bidhaa (product descriptions) kwa Kiswahili na Kiingereza.
            3. Kueleza bei katika Shilingi ya Kitanzania (TZS).
            4. Kutumia takwimu halisi za soko zilizopo:
            $marketplaceContext
            Kuwa mchangamfu, mwenye heshima na mwaminifu. Usibuni takwimu za uongo.
        """.trimIndent()

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
                parts = listOf(GeminiPart(text = userMessage))
            )
        )

        val request = GeminiRequest(
            contents = contents,
            systemInstruction = GeminiContent(
                role = "system",
                parts = listOf(GeminiPart(text = systemPrompt))
            )
        )

        try {
            val response = api.generateContent(apiKey = apiKey, request = request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "Samahani, sikupokea jibu kutoka kwa huduma ya AI. Tafadhali jaribu tena."
        } catch (e: Exception) {
            Log.e("GeminiService", "Gemini API error", e)
            "Hitilafu wakati wa kuwasiliana na Gemini AI: ${e.localizedMessage ?: "Tafadhali jaribu tena."}"
        }
    }
}
