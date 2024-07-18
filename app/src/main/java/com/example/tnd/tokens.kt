package com.example.tnd

object TokenData {
    val tokenList_sol = listOf(
        TokenItem(1, "SOL", R.drawable.token1_logo, 9, "So11111111111111111111111111111111111111112"),
        TokenItem(2, "USDC", R.drawable.token2_logo, 6, "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v"),
        TokenItem(4, "USDT", R.drawable.usdt, 6, "Es9vMFrzaCERmJfrF4H2FYD4KCoNkY11McCe8BenwNYB"),
        TokenItem(6, "ORCA", R.drawable.orca, 6, "orcaEKTdK7LKz57vaAYr9QeNsVEPfiu6QeMU1kektZE"),
        TokenItem(3, "Bonk", R.drawable.bonk, 5, "DezXAZ8z7PnrnRJjz3wXBoRgixCa6xjnB7YaB1pPB263"),
        TokenItem(5, "HNT", R.drawable.hnt, 8, "hntyVP6YFm1Hg25TN9WGLqM12b8TQmcknKrdu1oxWux"),
        TokenItem(5, "WIF", R.drawable.wif, 6, "EKpQGSJtjMFqKZ9KQanSqYXRcF8fBopzLHYxdM65zcjm"),
        TokenItem(6, "TREMP", R.drawable.tremp, 6, "FU1q8vJpZNUrmqsciSjp8bAKKidGsLmouB8CBdf8TKQv"),
        TokenItem(6, "BODEN", R.drawable.boden, 9, "3psH1Mj1f7yUfaD5gh6Zj7epE8hhrMkMETgv5TshQA4o"),
    )

    val tokenList_eth = listOf(
        TokenItem(1, "USDC", R.drawable.token2_logo, 6, "0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48")
    )

    val tokenList_polygon = listOf(
        TokenItem(1, "USDC", R.drawable.token2_logo, 6, "0x3c499c542cEF5E3811e1192ce70d8cC03d5c3359")
    )
    val tokenList_soltmp = listOf(
        TokenItem(1, "SOL", R.drawable.token1_logo, 9, "So11111111111111111111111111111111111111112")
    )
    val tokenList_xmr = listOf(
        TokenItem(1, "XMR", R.drawable.xmr_logo, 9, "xmr_is_amazing")
    )
    val tokenList_general = listOf(
        TokenItem(1, "SOL", R.drawable.token1_logo, 9, "So11111111111111111111111111111111111111112"),
        TokenItem(2, "USDC", R.drawable.token2_logo, 6, "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v"),
        TokenItem(4, "USDT", R.drawable.usdt, 6, "Es9vMFrzaCERmJfrF4H2FYD4KCoNkY11McCe8BenwNYB"),
        TokenItem(6, "ORCA", R.drawable.orca, 6, "orcaEKTdK7LKz57vaAYr9QeNsVEPfiu6QeMU1kektZE"),
        TokenItem(3, "Bonk", R.drawable.bonk, 5, "DezXAZ8z7PnrnRJjz3wXBoRgixCa6xjnB7YaB1pPB263"),
        TokenItem(5, "HNT", R.drawable.hnt, 8, "hntyVP6YFm1Hg25TN9WGLqM12b8TQmcknKrdu1oxWux"),
        TokenItem(5, "WIF", R.drawable.wif, 6, "EKpQGSJtjMFqKZ9KQanSqYXRcF8fBopzLHYxdM65zcjm"),
        TokenItem(6, "TREMP", R.drawable.tremp, 9, "FU1q8vJpZNUrmqsciSjp8bAKKidGsLmouB8CBdf8TKQv"),
        TokenItem(6, "BODEN", R.drawable.boden, 9, "3psH1Mj1f7yUfaD5gh6Zj7epE8hhrMkMETgv5TshQA4o"),
    )

    data class TokenItem(
        val id: Int,
        val name: String,
        val imageResId: Int,
        val decimals: Int,
        val mintAddress: String
    )
}
