package com.example.service

import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@JsonClass(generateAdapter = true)
data class Part(val text: String? = null)

@JsonClass(generateAdapter = true)
data class Content(val parts: List<Part>)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class PartResponse(val text: String? = null)

@JsonClass(generateAdapter = true)
data class ContentResponse(val parts: List<PartResponse>? = null)

@JsonClass(generateAdapter = true)
data class Candidate(val content: ContentResponse? = null)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(val candidates: List<Candidate>? = null)

interface GeminiApi {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiService {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val api: GeminiApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApi::class.java)
    }

    suspend fun generateResponse(prompt: String, systemInstruction: String? = null): String = withContext(Dispatchers.IO) {
        val key = BuildConfig.GEMINI_API_KEY
        if (key.isNullOrBlank() || key == "MY_GEMINI_API_KEY") {
            // Offline fall back simulating smart ERP answers beautifully
            return@withContext getOfflineResponse(prompt)
        }

        val contents = listOf(Content(parts = listOf(Part(text = prompt))))
        val instruction = systemInstruction?.let { Content(parts = listOf(Part(text = it))) }
        val request = GenerateContentRequest(contents, instruction)

        try {
            val response = api.generateContent(key, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "I apologize, but I received empty response data from the core intelligence engine."
        } catch (e: Exception) {
            e.printStackTrace()
            "Service is running in sandbox offline mode. ${getOfflineResponse(prompt)}"
        }
    }

    private fun getOfflineResponse(prompt: String): String {
        val lower = prompt.lowercase()
        return when {
            lower.contains("invoice") || lower.contains("generate") -> {
                "**Offline Invoice Suggestions:**\nBased on your prompt, here is a structured layout:\n- **Client**: Google Operations\n- **Service**: Enterprise Custom Solutions Dev\n- **Rate**: ₹1,500/hr\n- **Suggested Terms**: Net-15\n\n*(Connect online with a valid API Key to auto-draft live documents)*"
            }
            lower.contains("tax") || lower.contains("finance") || lower.contains("save") -> {
                "**Strategic Enterprise Finance Intel:**\n1. Ensure GST filing (Section 16 ITC) is matched against your sales receipts.\n2. Leverage 100% depreciation in Year 1 on IT hardware/server subscriptions as capital costs.\n3. Track bills automatically on outstanding credit terms to lower micro cash crunches."
            }
            lower.contains("hello") || lower.contains("hi") -> {
                "Hello! Welcome to **Abielan ERP Advisory Console**. How may I assist your enterprise financial workflows, invoice drafts, timer billing computations, or cash flow analysis today?"
            }
            else -> {
                "**Abielan ERP Consultation Desk:**\nI have received your request. To help with transaction tracking, please query:\n- *'Analyze GST filings and tax breaks'* \n- *'Standardize billing values for corporate sales orders'* \n- *'Brief cash-flow summary for recent invoices'*"
            }
        }
    }
}
