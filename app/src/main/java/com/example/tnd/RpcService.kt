package com.example.tnd

import android.util.Log
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
class RpcService {

    companion object {
        private const val BASE_URL = "https://mainnet.helius-rpc.com/"
        private const val CRYPTO_COMPARE_BASE_URL = "https://min-api.cryptocompare.com/data/"
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
    private val cryptoCompareRetrofit = Retrofit.Builder()
        .baseUrl(CRYPTO_COMPARE_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val cryptoCompareApi: CryptoCompareApi = cryptoCompareRetrofit.create(CryptoCompareApi::class.java)

    fun getMoneroPrice(callback: (Double?) -> Unit) {
        val call = cryptoCompareApi.getPrice("XMR", "USD")
        call.enqueue(object : Callback<Map<String, Double>> {
            override fun onResponse(call: Call<Map<String, Double>>, response: Response<Map<String, Double>>) {
                if (response.isSuccessful) {
                    val price = response.body()?.get("USD")
                    callback(price)
                } else {
                    Log.e(TAG, "Error: ${response.code()}")
                    callback(null)
                }
            }

            override fun onFailure(call: Call<Map<String, Double>>, t: Throwable) {
                Log.e(TAG, "Failed to fetch price", t)
                callback(null)
            }
        })
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

    suspend fun getCurrentSlot(): Long? {
        val request = SlotRequest()
        return try {
            val response = heliusApi.getSlot(request)
            if (response.isSuccessful) {
                val slotResult = response.body()
                Log.d(TAG, "Get current slot success: $slotResult")
                slotResult?.result
            } else {
                Log.e(TAG, "Error getting current slot: HTTP ${response.code()} ${response.message()}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception when getting current slot: ${e.message}", e)
            null
        }
    }
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

}

data class SlotRequest(
    val jsonrpc: String = "2.0",
    val id: Int = 1,
    val method: String = "getSlot"
)

data class SlotResponse(
    val jsonrpc: String,
    val result: Long,
    val id: Int
)
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
