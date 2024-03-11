package com.example.tnd

data class SwapRequest(
    val userPublicKey: String,
    //val wrapAndUnwrapSol: Boolean,
    //val useSharedAccounts: Boolean,
    //val feeAccount: String,
    //val computeUnitPriceMicroLamports: Long,
    //val asLegacyTransaction: Boolean,
    //val restrictIntermediateTokens: Boolean,
    //val useTokenLedger: Boolean,
    val destinationTokenAccount: String,
    //val dynamicComputeUnitLimit: Boolean,
   // val skipUserAccountsRpcCalls: Boolean,
    val quoteResponse: QuoteResponse
)

data class QuoteResponse(
    val inputMint: String,
    val inAmount: String,
    val outputMint: String,
    val outAmount: String,
    val otherAmountThreshold: String,
    val swapMode: String,
    val slippageBps: Int,
    val platformFee: PlatformFee,
    val priceImpactPct: Double,
    val routePlan: List<RoutePlan>,
    val contextSlot: Long,
    val timeTaken: Double
)

data class RoutePlan(
    val swapInfo: SwapInfo,
    val percent: Int
)

data class SwapRequestInfo(
    val ammKey: String,
    val label: String,
    val inputMint: String,
    val outputMint: String,
    val inAmount: String,
    val outAmount: String,
    val feeAmount: String,
    val feeMint: String
)

data class PlatformFee(
    val amount: String,
    val feeBps: Int
)
