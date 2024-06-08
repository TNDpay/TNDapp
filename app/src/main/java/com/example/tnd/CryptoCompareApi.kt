// CryptoCompareApi.kt
package com.example.tnd

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface CryptoCompareApi {
    @GET("price")
    fun getPrice(
        @Query("fsym") fsym: String,
        @Query("tsyms") tsyms: String
    ): Call<Map<String, Double>>
}
