package com.example.tnd

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.Response

interface IronforgeApiService {
    @POST("mainnet?apiKey=${BuildConfig.IRONFORGE_API_KEY}")
    suspend fun getAccountInfo(@Body request: AccountInfoRequest): Response<AccountInfoResponse>

    @POST("mainnet?apiKey=${BuildConfig.IRONFORGE_API_KEY}")
    suspend fun getLatestBlockhash(@Body request: LatestBlockhashRequest): Response<LatestBlockhashResponse>

    @POST("mainnet?apiKey=${BuildConfig.IRONFORGE_API_KEY}")
    suspend fun sendTransaction(@Body request: SendTransactionRequest): Response<SendTransactionResponse>
}

data class SendTransactionRequest(
    val jsonrpc: String = "2.0",
    val id: Int = 1,
    val method: String = "sendTransaction",
    val params: List<String>
)

data class AccountInfoRequest(
    val jsonrpc: String = "2.0",
    val id: Int = 1,
    val method: String = "getAccountInfo",
    val params: List<Any>
)

data class AccountInfoResponse(
    val jsonrpc: String,
    val result: AccountInfoResult,
    val id: Int
)

data class AccountInfoResult(
    val context: ContextData,
    val value: AccountData
)

data class ContextData(
    val slot: Int
)

data class AccountData(
    val data: List<String>,
    val executable: Boolean,
    val lamports: Long,
    val owner: String,
    val rentEpoch: Int,
    val space: Int
)
data class Commitment(
    val commitment: String
)

data class LatestBlockhashRequest(
    val jsonrpc: String = "2.0",
    val id: Int = 1,
    val method: String = "getLatestBlockhash",
    val params: List<Map<String, String>>
)


data class BlockhashResult(
    val context: ContextData,
    val value: BlockhashValue
)

data class BlockhashValue(
    val blockhash: String,
    val lastValidBlockHeight: Int
)
data class LatestBlockhashResponse(
    val jsonrpc: String,
    val result: BlockhashResult,
    val id: Int
)

data class SendTransactionResponse(
    val jsonrpc: String,
    val result: String?,
    val error: ErrorResponse?,
    val id: Int
)

data class ErrorResponse(
    val code: Int,
    val message: String,
    val data: ErrorData
)

data class ErrorData(
    val accounts: List<String>?, // Assuming this could be a list of account identifiers
    val err: String,
    val logs: List<String>,
    val returnData: Any?, // Type depends on what 'returnData' can contain
    val unitsConsumed: Int
)
