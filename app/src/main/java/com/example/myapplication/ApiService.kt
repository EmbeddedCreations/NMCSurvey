package com.example.myapplication

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import okhttp3.RequestBody
import retrofit2.http.Query

interface ApiService {
    // Upload Data API
    @POST("app_upload_Image.php")
    fun uploadData(@Body body: RequestBody): Call<UploadResponse>

    // Fetch Survey Data API
    @GET("app_fetch_survey_data.php")
    fun fetchSurveyData(@Query("username") username: String): Call<List<SurveyItem>>

    // Fetch Survey Data API with optional lat/long for nearby locations
    @GET("app_fetch_survey_data.php")
    fun fetchSurveyDataNearby(
        @Query("username") username: String,
        @Query("lat") lat: Double? = null,    // Optional latitude parameter
        @Query("long") long: Double? = null  // Optional longitude parameter
    ): Call<List<SurveyItem>>
}

