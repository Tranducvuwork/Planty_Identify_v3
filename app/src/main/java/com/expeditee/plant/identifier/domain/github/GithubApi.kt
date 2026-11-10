package com.expeditee.plant.identifier.domain.github

import com.expeditee.plant.identifier.model.reponse.CareGuidesResponse
import com.expeditee.plant.identifier.model.reponse.FaqResponseItem
import com.expeditee.plant.identifier.model.reponse.PestDiseaseResponse
import com.expeditee.plant.identifier.model.reponse.PlantResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Url

interface GithubApi {
    @GET("detailed_plants_light.json")
    suspend fun getDetailedPlants(): Response<List<PlantResponse>>

    @GET
    suspend fun getCareGuide(@Url fileName: String = "care_guides.json"): Response<CareGuidesResponse>

    @GET
    suspend fun getFAQ(@Url fileName: String): Response<List<FaqResponseItem>>

    @GET
    suspend fun getDiseaseList(@Url fileName: String): Response<PestDiseaseResponse>

}