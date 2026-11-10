package com.expeditee.plant.identifier.network

import com.expeditee.plant.identifier.network.model.FolkRemediesApiResponse
import com.expeditee.plant.identifier.network.model.PlantChatResponse
import com.expeditee.plant.identifier.network.model.PlantDiseaseApiResponse
import com.expeditee.plant.identifier.network.model.PlantListApiResponse
import com.expeditee.plant.identifier.network.model.PlantScanApiResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface PlantBackendApiService {

    @Multipart
    @POST("api/v1/plant/scan")
    suspend fun scanPlant(
        @Part file: MultipartBody.Part,
        @Part("language") language: RequestBody? = null,
    ): PlantScanApiResponse

    @Multipart
    @POST("api/v1/plant/disease")
    suspend fun diagnoseDisease(
        @Part file: MultipartBody.Part,
        @Part("language") language: RequestBody? = null,
    ): PlantDiseaseApiResponse

    @FormUrlEncoded
    @POST("api/v1/plant/chat")
    suspend fun sendChat(
        @Field("message") message: String,
        @Field("language") language: String,
    ): PlantChatResponse

    @GET("api/v1/plants")
    suspend fun listPlants(): PlantListApiResponse

    @GET("api/v1/plant/{plantId}")
    suspend fun getPlantDetail(@Path("plantId") plantId: String): PlantScanApiResponse

    @GET("api/v1/plant/qr/{code}")
    suspend fun getPlantByQr(@Path(value = "code", encoded = true) code: String): PlantScanApiResponse

    @GET("api/v1/folk-remedies")
    suspend fun getFolkRemedies(@Query("plant_id") plantId: String? = null): FolkRemediesApiResponse
}
