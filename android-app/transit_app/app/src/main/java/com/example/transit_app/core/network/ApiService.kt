package com.example.transit_app.core.network

import com.example.transit_app.data.models.LocalDataResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

// This interface defines the endpoint we are calling
interface ApiService {
    @GET("api/v1/explore")
    suspend fun getExploreData(): LocalDataResponse
}

// This object creates the Retrofit client
object ApiClient {
    // 10.0.2.2 is the special IP that allows the Android Emulator
    // to access your computer's localhost (where your Python server is running)
    private const val BASE_URL = "http://10.0.2.2:8000/"

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}