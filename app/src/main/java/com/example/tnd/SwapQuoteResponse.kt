package com.example.tnd

import com.google.gson.annotations.SerializedName

data class SwapQuoteResponse(
    @SerializedName("inputMint") val inputMint: String,
    @SerializedName("inAmount") val inAmount: Long,
    @SerializedName("outputMint") val outputMint: String,
    @SerializedName("outAmount") val outAmount: Long,
    @SerializedName("otherAmountThreshold") val otherAmountThreshold: Long,
    @SerializedName("swapMode") val swapMode: String,
    @SerializedName("slippageBps") val slippageBps: Int,
    @SerializedName("platformFee") val platformFee: Long?,
    @SerializedName("priceImpactPct") val priceImpactPct: Double,
    @SerializedName("routePlan") val routePlan: List<RoutePlanItem>,
    @SerializedName("contextSlot") val contextSlot: Long,
    @SerializedName("timeTaken") val timeTaken: Double
)

data class RoutePlanItem(
    @SerializedName("swapInfo") val swapInfo: SwapInfo,
    @SerializedName("percent") val percent: Int
)

data class SwapInfo(
    @SerializedName("ammKey") val ammKey: String,
    @SerializedName("label") val label: String,
    @SerializedName("inputMint") val inputMint: String,
    @SerializedName("outputMint") val outputMint: String,
    @SerializedName("inAmount") val inAmount: String,
    @SerializedName("outAmount") val outAmount: String,
    @SerializedName("feeAmount") val feeAmount: String,
    @SerializedName("feeMint") val feeMint: String
)
