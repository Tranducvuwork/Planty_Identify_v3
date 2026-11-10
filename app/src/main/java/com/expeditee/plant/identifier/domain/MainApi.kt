package com.expeditee.plant.identifier.domain

import com.expeditee.plant.identifier.BuildConfig
import com.expeditee.plant.identifier.di.Feature
import com.expeditee.plant.identifier.domain.github.GithubApi
import com.expeditee.plant.identifier.domain.repository.GithubRepository
import com.expeditee.plant.identifier.domain.repository.GithubRepositoryImpl
import com.expeditee.plant.identifier.domain.repository.WeatherRepository
import com.expeditee.plant.identifier.domain.weather.WeatherApiService
import com.expeditee.plant.identifier.helper.firebase.RemoteConfigManagement
import com.google.common.net.HttpHeaders
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

private const val DEFAULT_WEATHER_BASE_URL = "https://api.open-meteo.com/v1/"

internal fun MainApi(): Feature {
    val feature = object : Feature {
        override fun name() = "api_feature"

        override fun diModule() = module {
            single(named("github_client")) { provideGithubOkHttpClient(get()) }

            single { provideGithubApi(get(named("github_client")), get()) }

            singleOf(::GithubRepositoryImpl) bind GithubRepository::class

            single(named("weather_retrofit")) { provideWeatherRetrofit() }

            single<WeatherApiService> {
                get<Retrofit>(named("weather_retrofit")).create(WeatherApiService::class.java)
            }

            singleOf(::WeatherRepository)
        }

        private fun provideGithubOkHttpClient(remoteConfig: RemoteConfigManagement): OkHttpClient {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            }

            return OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .addNetworkInterceptor { chain ->
                    val requestBuilder = chain.request().newBuilder()
                    requestBuilder.addHeader(HttpHeaders.ACCEPT, "application/json")

                    val githubToken = remoteConfig.token
                    if (githubToken.isNotEmpty()) {
                        requestBuilder.addHeader(
                            HttpHeaders.AUTHORIZATION, "Bearer $githubToken"
                        )
                    }

                    chain.proceed(requestBuilder.build())
                }
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
        }

        private fun provideGithubApi(
            httpClient: OkHttpClient,
            remoteConfig: RemoteConfigManagement
        ): GithubApi {
            val maxRetries = 50
            var retries = 0
            while (!remoteConfig.isConfigFetched && retries < maxRetries) {
                Thread.sleep(100)
                retries++
            }

            val baseUrl = remoteConfig.baseUrlData.ifEmpty { "https://github.com" }
            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(httpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(GithubApi::class.java)
        }

        private fun provideWeatherRetrofit(): Retrofit {
            return Retrofit.Builder()
                .baseUrl(DEFAULT_WEATHER_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
    }
    return feature
}