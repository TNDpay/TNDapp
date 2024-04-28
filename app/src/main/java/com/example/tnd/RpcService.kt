package com.example.tnd

import android.util.Log
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RpcService {


    companion object {
        private const val BASE_URL = "https://mainnet.helius-rpc.com/"
        private const val TAG = "RpcService"
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val originalRequest = chain.request()
            val originalUrl = originalRequest.url
            val urlWithApiKey = originalUrl.newBuilder()
                    .addQueryParameter("api-key", BuildConfig.HELIUS_API_KEY)
                .build()
            val requestBuilder = originalRequest.newBuilder().url(urlWithApiKey)
                .header("Content-Type", "application/json")
                .build()
            chain.proceed(requestBuilder)
        }.build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val heliusApi = retrofit.create(HeliusApiService::class.java)


    suspend fun getAccountInfo(pubKey: String): AccountInfoResponse? {
        val request = AccountInfoRequest(
            jsonrpc = "2.0",
            id = 1,
            method = "getAccountInfo",
            params = listOf(pubKey, mapOf("encoding" to "base58"))
        )
        Log.e(TAG, request.toString())
        try {
            val response = heliusApi.getAccountInfo(request)
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
    suspend fun sendTransaction(serializedTransaction: String): SendTransactionResponse? {
        val request = SendTransactionRequest(
            params = listOf(serializedTransaction)
        )
        return try {
            val response = heliusApi.sendTransaction(request)
            if (response.isSuccessful) {
                Log.d(TAG, "sendTransaction Success: ${response.body()}")
                response.body()
            } else {
                Log.e(TAG, "sendTransaction Error: HTTP ${response.code()} ${response.message()}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "sendTransaction Exception: ${e.message}", e)
            null
        }
    }

    suspend fun getLatestBlockhash(): LatestBlockhashResponse? {
        val request = LatestBlockhashRequest(
            params = listOf(mapOf("commitment" to "processed"))
        )
        return try {
            val response = heliusApi.getLatestBlockhash(request)
            if (response.isSuccessful) {
                Log.d("getLatestBlockhash", "Success: ${response.body()}")
                response.body()
            } else {
                Log.e("getLatestBlockhash", "Error: HTTP ${response.code()} ${response.message()}")
                null
            }
        } catch (e: Exception) {
            Log.e("getLatestBlockhash", "Exception: ${e.message}", e)
            null
        }
    }
    data class ConfigObject(
        val mint: String,
        val encoding: String
    )

    suspend fun getTokenAccountsByOwner(pubKey: String,Mint: String): Boolean {
        val request = TokenOwnerRequest(
            jsonrpc = "2.0",
            id = 1,
            method = "getTokenAccountsByOwner",
            params = listOf(
                pubKey,
                mapOf("mint" to Mint),
                mapOf("encoding" to "jsonParsed")
            )
        )
        Log.e(TAG, request.toString())
        try {
            val response = heliusApi.getTokenAccountsByOwner(request)
            Log.e(TAG, "Response body: ${response.body()?.toString()}")
            if (response.isSuccessful) {
                val tokenAccountResponse = response.body()
                return tokenAccountResponse?.result?.value?.isNotEmpty() ?: false
            } else {
                Log.e(TAG, "Error: ${response.code()} ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception when calling getTokenAccountsByOwner: ${e.message}", e)
        }
        return false
    }


    suspend fun getAssetsByOwner(ownerAddress: String, page: Int, limit: Int): AssetsByOwnerResponse? {
        val request = AssetsByOwnerRequest(
            method = "getAssetsByOwner",
            params = AssetsByOwnerParams(ownerAddress, page, limit)
        )
        return try {
            val response = heliusApi.getAssetsByOwner(request)
            if (response.isSuccessful) {
                Log.d(TAG, "getAssetsByOwner Success: ${response.body()}")
                response.body()
            } else {
                Log.e(TAG, "getAssetsByOwner Error: HTTP ${response.code()} ${response.message()}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "getAssetsByOwner Exception: ${e.message}", e)
            null
        }
    }
}

data class AssetRequest(
    val jsonrpc: String = "2.0",
    val id: String = "my-id",
    val method: String,
    val params: AssetParams
)

data class AssetParams(
    val id: String
)

data class AssetResponse(
    val jsonrpc: String,
    val id: String,
    val result: AssetResult
)

data class AssetResult(
    val id: String,
// Other asset properties
)

data class AssetsByOwnerRequest(
    val jsonrpc: String = "2.0",
    val id: String = "my-id",
    val method: String,
    val params: AssetsByOwnerParams
)

data class AssetsByOwnerParams(
    val ownerAddress: String,
    val page: Int,
    val limit: Int
)

data class AssetsByOwnerResponse(
    val jsonrpc: String,
    val id: String,
    val result: AssetsByOwnerResult
)

data class AssetsByOwnerResult(
    val total: Int,
    val page: Int,
    val limit: Int,
    val items: List<AssetResult>
)

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