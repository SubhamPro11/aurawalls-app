package com.example.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

data class WallpaperDto(
    val id: String?,
    val title: String?,
    val category: String?,
    val resolution: String?,
    val url: String?,
    @com.squareup.moshi.Json(name = "thumbnail_url") val thumbnailUrl: String?,
    val views: Int?,
    val downloads: Int?,
    @com.squareup.moshi.Json(name = "creator_name") val creatorName: String?,
    @com.squareup.moshi.Json(name = "dominant_color") val dominantColor: String?,
    val tags: List<String>?
)

interface AuraWallsApiService {
    @GET("api/wallpapers")
    suspend fun getWallpapers(
        @Query("category") category: String? = null,
        @Query("search") search: String? = null,
        @Query("color") color: String? = null
    ): List<WallpaperDto>
}

object RetrofitClient {
    private const val BASE_URL = "https://aurawalls.qzz.io/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val logger = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(logger)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    val service: AuraWallsApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(AuraWallsApiService::class.java)
    }
}
