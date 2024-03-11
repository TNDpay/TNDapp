package com.example.tnd

import android.util.Log
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


data class AccountInfoContext(
    val apiVersion: String,
    val slot: Long
)

data class AccountInfoValue(
    val data: List<String>,
    val executable: Boolean,
    val lamports: Long,
    val owner: String,
    val rentEpoch: Long,
    val space: Int
)


class RpcService {

    companion object {
        private const val BASE_URL = "https://rpc.ironforge.network/"
        private const val TAG = "RpcService"
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val originalRequest = chain.request()
            val originalUrl = originalRequest.url
            val urlWithApiKey = originalUrl.newBuilder()
                .addQueryParameter("apiKey", BuildConfig.IRONFORGE_API_KEY)
                .build()
            val requestBuilder = originalRequest.newBuilder().url(urlWithApiKey)
                .header("Content-Type", "application/json")
                .header("x-ironforge-cache-control", "max-age=5")
                .build()
            chain.proceed(requestBuilder)
        }.build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val ironforgeApi = retrofit.create(IronforgeApiService::class.java)

    suspend fun sendTransaction(serializedTransaction: String): SendTransactionResponse? {
        val request = SendTransactionRequest(
            params = listOf(serializedTransaction)
        )
        return try {
            val response = ironforgeApi.sendTransaction(request)
            if (response.isSuccessful) {
                // Log the successful response
                Log.d(TAG, "sendTransaction Success: ${response.body()}")
                response.body()
            } else {
                // Log error with response code
                Log.e(TAG, "sendTransaction Error: HTTP ${response.code()} ${response.message()}")
                null
            }
        } catch (e: Exception) {
            // Log exception
            Log.e(TAG, "sendTransaction Exception: ${e.message}", e)
            null
        }
    }

    suspend fun getLatestBlockhash(): LatestBlockhashResponse? {
        val request = LatestBlockhashRequest(
            params = listOf(mapOf("commitment" to "processed"))
        )
        return try {
            val response = retrofit.create(IronforgeApiService::class.java).getLatestBlockhash(request)
            if (response.isSuccessful) {
                // Log the successful response
                Log.d("getLatestBlockhash", "Success: ${response.body()}")
                response.body()
            } else {
                // Log error with response code
                Log.e("getLatestBlockhash", "Error: HTTP ${response.code()} ${response.message()}")
                null
            }
        } catch (e: Exception) {
            // Log exception
            Log.e("getLatestBlockhash", "Exception: ${e.message}", e)
            null
        }
    }


    suspend fun getAccountInfo(pubKey: String): AccountInfoResponse? {
        val request = AccountInfoRequest(
            jsonrpc = "2.0",
            id = 1,
            method = "getAccountInfo",
            params = listOf(pubKey, mapOf("encoding" to "base58"))
        )
        Log.e(TAG, request.toString())
        try {
            val response = ironforgeApi.getAccountInfo(request)
            Log.e(TAG, response.toString())
            if (response.isSuccessful) {
                return response.body()
            } else {
                Log.e(TAG, "Error: ${response.code()} ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception when calling getAccountInfo: ${e.message}", e)
        }
        return null
    }
}
