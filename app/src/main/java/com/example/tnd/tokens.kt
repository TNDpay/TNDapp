package com.example.tnd

object TokenData {
    val tokenList = listOf(
        TokenItem(1, "SOL", R.drawable.token1_logo, 9, "So11111111111111111111111111111111111111112"),
        TokenItem(2, "USDC", R.drawable.token2_logo, 6, "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v"),
        TokenItem(3, "Bonk", R.drawable.bonk, 5, "DezXAZ8z7PnrnRJjz3wXBoRgixCa6xjnB7YaB1pPB263"),
        TokenItem(4, "USDT", R.drawable.usdt, 6, "Es9vMFrzaCERmJfrF4H2FYD4KCoNkY11McCe8BenwNYB"),
        TokenItem(5, "HNT", R.drawable.hnt, 8, "hntyVP6YFm1Hg25TN9WGLqM12b8TQmcknKrdu1oxWux"),
        TokenItem(6, "JUP", R.drawable.jup, 6, "JUPyiwrYJFskUPiHa7hkeR8VUtAeFoSYbKedZNsDvCN")
    )

    val tokenList_eth = listOf(
        TokenItem(1, "USDC", R.drawable.token2_logo, 6, "0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48")
    )

    val tokenList_polygon = listOf(
        TokenItem(1, "USDC", R.drawable.token2_logo, 6, "0x3c499c542cEF5E3811e1192ce70d8cC03d5c3359")
    )

    data class TokenItem(
        val id: Int,
        val name: String,
        val imageResId: Int,
        val decimals: Int,
        val mintAddress: String
    )
}
