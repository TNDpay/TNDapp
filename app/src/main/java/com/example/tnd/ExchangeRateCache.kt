package com.example.tnd


object ExchangeRateCache {
    private val cache = mutableMapOf<String, Double>()
    private var lastUpdateTime: Long = 0
    private const val CACHE_DURATION = 1000 * 60 * 60 // 1 hour in milliseconds

    fun getRate(currency: String): Double? = cache[currency]

    fun setRate(currency: String, rate: Double) {
        cache[currency] = rate
        lastUpdateTime = System.currentTimeMillis()
    }

    fun isCacheValid(): Boolean = System.currentTimeMillis() - lastUpdateTime < CACHE_DURATION

}