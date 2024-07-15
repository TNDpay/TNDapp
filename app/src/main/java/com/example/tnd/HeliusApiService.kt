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

    @POST("/")
    suspend fun getTokenAccountsByOwner(@Body request: TokenOwnerRequest): Response<TokenAccountResponse>

    @POST(".")
    suspend fun getSlot(@Body request: SlotRequest): Response<SlotResponse>

}
data class TokenOwnerRequest(
    val jsonrpc: String = "2.0",
    val id: Int = 1,
    val method: String = "getTokenAccountsByOwner",
    val params: List<Any>
)

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

data class TokenAccountResponse(
    val jsonrpc: String,
    val result: TokenAccountResult,
    val id: String
)

data class TokenAccountResult(
    val context: ContextData,
    val value: List<TokenAccountItem>
)

data class TokenAccountItem(
    val account: TokenAccountInfo,
    val pubkey: String,
    val state: String
)

data class TokenAccountInfo(
    val data: TokenData,
    val executable: Boolean,
    val lamports: Long,
    val owner: String,
    val rentEpoch: String,
    val space: Int
)
data class ParsedData(
    val info: TokenInfo,
    val type: String
)


data class TokenAmount(
    val amount: String,
    val decimals: Int,
    val uiAmount: Double,
    val uiAmountString: String
)

