package com.example.tnd


import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Body
import retrofit2.http.POST

interface JupiterApiService {
    @GET("v6/price")
    fun getPrice(@Query("ids") tokenName: String): Call<PriceResponse>

    @GET("v6/quote")
    fun getSwapQuote(
        @Query("inputMint") inputMint: String,
        @Query("outputMint") outputMint: String,
        @Query("amount") amount: Long,
        @Query("swapMode") swapMode: String,
        @Query("slippageBps") slippageBps: Int
    ): Call<SwapQuoteResponse>

    @POST("v6/swap")
    fun getSwapTransaction(
        @Body swapRequest: SwapRequest
    ): Call<SwapTransactionResponse>

}
