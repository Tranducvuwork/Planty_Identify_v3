package com.expeditee.plant.identifier.network

import android.util.Log
import com.expeditee.plant.identifier.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NvidiaApiClient {
    private const val TAG = "NvidiaApiClient"

    fun createService(): NvidiaChatApiService {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        val authInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer ${BuildConfig.NVIDIA_API_KEY}")
                .addHeader("Content-Type", "application/json")
                .build()
            Log.d(TAG, "→ ${request.method} ${request.url}")
            chain.proceed(request)
        }
        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .retryOnConnectionFailure(true)
            .build()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.NVIDIA_API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NvidiaChatApiService::class.java)
    }
}
