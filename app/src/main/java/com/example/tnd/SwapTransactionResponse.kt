package com.example.tnd

import com.google.gson.annotations.SerializedName

data class SwapTransactionResponse(
    @SerializedName("swapTransaction") val swapTransaction: String,
    @SerializedName("lastValidBlockHeight") val lastValidBlockHeight: Long,
    @SerializedName("prioritizationFeeLamports") val prioritizationFeeLamports: Long
)