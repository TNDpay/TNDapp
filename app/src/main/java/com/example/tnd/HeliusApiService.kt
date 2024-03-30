package com.example.tnd

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.Response

interface HeliusApiService {
    @POST("/")
    suspend fun sendTransaction(@Body request: SendTransactionRequest): Response<SendTransactionResponse>

    @POST("/")
    suspend fun getLatestBlockhash(@Body request: LatestBlockhashRequest): Response<LatestBlockhashResponse>

    @POST("/")
    suspend fun getAsset(@Body request: AssetRequest): Response<AssetResponse>

    @POST("/")
    suspend fun getAssetsByOwner(@Body request: AssetsByOwnerRequest): Response<AssetsByOwnerResponse>

    @POST("/")
    suspend fun getAccountInfo(@Body request: AccountInfoRequest): Response<AccountInfoResponse>

}