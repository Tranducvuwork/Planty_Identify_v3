package com.expeditee.plant.identifier.network

import okhttp3.OkHttpClient
import org.koin.core.qualifier.named
import org.koin.dsl.module

val networkModule = module {
    single<OkHttpClient>(named("plant_api_client")) {
        PlantApiClient.createOkHttpClient()
    }

    single(named("plant_api_retrofit")) {
        PlantApiClient.createRetrofit(get(named("plant_api_client")))
    }

    single<PlantBackendApiService> {
        get<retrofit2.Retrofit>(named("plant_api_retrofit")).create(PlantBackendApiService::class.java)
    }
}
