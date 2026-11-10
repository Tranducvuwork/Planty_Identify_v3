package com.expeditee.plant.identifier.network

import android.util.Log
import com.expeditee.plant.identifier.BuildConfig
import com.expeditee.plant.identifier.common.Constants
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit

object PlantApiClient {
    private const val TAG = "PlantApiClient"

    fun createOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.HEADERS
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val headerInterceptor = Interceptor { chain ->
            val builder = chain.request()
                .newBuilder()
                .addHeader("X-API-Key", Constants.PLANT_API_KEY)
            // Free ngrok interstitial page breaks API clients without this header.
            val host = chain.request().url.host
            if (host.contains("ngrok", ignoreCase = true)) {
                builder.addHeader("ngrok-skip-browser-warning", "1")
            }
            val request = builder.build()
            Log.d(TAG, "→ ${request.method} ${request.url}")
            chain.proceed(request)
        }

        return OkHttpClient.Builder()
            .connectTimeout(Constants.NETWORK_TIMEOUT_CONNECT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(Constants.NETWORK_TIMEOUT_READ_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(Constants.NETWORK_TIMEOUT_WRITE_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(headerInterceptor)
            .addInterceptor(loggingInterceptor)
            .retryOnConnectionFailure(true)
            .build()
    }

    fun createRetrofit(client: OkHttpClient): Retrofit {
        val base = Constants.PLANT_API_BASE_URL
        Log.i(TAG, "Plant Retrofit baseUrl=$base useLocal=${BuildConfig.USE_LOCAL_PLANT_API}")
        return Retrofit.Builder()
            .baseUrl(base)
            .client(client)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
