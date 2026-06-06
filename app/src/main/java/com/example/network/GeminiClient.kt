package com.example.network

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GeminiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    /**
     * Generates a custom wallpaper based on the prompt using Gemini 2.5 Image model.
     * Returns a Bitmap on success, or throws an exception.
     */
    suspend fun generateWallpaper(prompt: String): Bitmap = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalStateException("API Key is missing or invalid. Please configure your GEMINI_API_KEY in the Secrets panel.")
        }

        // Base URL for the image generation model
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-image:generateContent?key=$apiKey"

        val jsonRequest = """
            {
              "contents": [{
                "parts": [{"text": "$prompt"}]
              }],
              "generationConfig": {
                "responseModalities": ["IMAGE"],
                "imageConfig": {
                  "aspectRatio": "9:16",
                  "imageSize": "1K"
                }
              }
            }
        """.trimIndent()

        val requestBody = jsonRequest.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errBody = response.body?.string() ?: ""
                throw Exception("API call failed with code ${response.code}: $errBody")
            }

            val bodyString = response.body?.string() ?: throw Exception("Empty response from Gemini API")
            
            // Parse with Moshi map dynamically to prevent strict parsing breakdowns
            val adapter = moshi.adapter(Map::class.java)
            val parsed = adapter.fromJson(bodyString) ?: throw Exception("Failed to parse JSON response")

            val candidates = parsed["candidates"] as? List<*> ?: throw Exception("Invalid response: candidates missing")
            if (candidates.isEmpty()) throw Exception("No candidates returned from the AI model")
            
            val firstCandidate = candidates[0] as? Map<*, *> ?: throw Exception("Invalid candidate entry")
            val content = firstCandidate["content"] as? Map<*, *> ?: throw Exception("Invalid candidate content")
            val parts = content["parts"] as? List<*> ?: throw Exception("Invalid candidate parts")
            if (parts.isEmpty()) throw Exception("AI generated empty parts")

            val firstPart = parts[0] as? Map<*, *> ?: throw Exception("Invalid part structure")
            val inlineData = firstPart["inlineData"] as? Map<*, *> ?: throw Exception("No image data generated")
            val base64Data = inlineData["data"] as? String ?: throw Exception("No base64 data generated")

            // Decode the Base64 image data
            val imageBytes = Base64.decode(base64Data, Base64.NO_WRAP)
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                ?: throw Exception("Failed to decode generated image bytes into Bitmap")

            bitmap
        }
    }
}
