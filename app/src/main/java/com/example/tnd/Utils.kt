package com.example.tnd

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.*
import com.google.gson.Gson
import com.example.tnd.ExchangeRateCache
class Utils {
    companion object {
        private val client = OkHttpClient()
        private val gson = Gson()
        private var exchangeRates: Map<String, Double>? = null
        private val HEX_CHARS = "0123456789ABCDEF"
        fun hexStringToByteArray(data: String) : ByteArray {

            val result = ByteArray(data.length / 2)

            for (i in 0 until data.length step 2) {
                val firstIndex = HEX_CHARS.indexOf(data[i]);
                val secondIndex = HEX_CHARS.indexOf(data[i + 1]);

                val octet = firstIndex.shl(4).or(secondIndex)
                result.set(i.shr(1), octet.toByte())
            }

            return result
        }
        fun openWebPage(context: Context, url: String) {
            val builder = CustomTabsIntent.Builder()
            val colorSchemeParams = CustomTabColorSchemeParams.Builder().build()

            builder.setDefaultColorSchemeParams(colorSchemeParams)
            val customTabsIntent = builder.build()

            customTabsIntent.launchUrl(context, Uri.parse(url))
        }

        private val HEX_CHARS_ARRAY = "0123456789ABCDEF".toCharArray()
        fun toHex(byteArray: ByteArray) : String {
            val result = StringBuffer()

            byteArray.forEach {
                val octet = it.toInt()
                val firstIndex = (octet and 0xF0).ushr(4)
                val secondIndex = octet and 0x0F
                result.append(HEX_CHARS_ARRAY[firstIndex])
                result.append(HEX_CHARS_ARRAY[secondIndex])
            }

            return result.toString()
        }

        fun shortenAddress(address: String): String {
            if (address.length > 8) {
                return "${address.substring(0, 4)}....${address.substring(address.length - 4)}"
            }
            return address
        }

        private fun fetchExchangeRates(callback: (Boolean) -> Unit) {
            Log.d("FetchExchangeRates", "Starting to fetch exchange rates from Firestore")
            val db = FirebaseFirestore.getInstance()
            val docRef = db.collection("c_rates").document("qAGRkjY9ZC7rlrMNNifO")

            docRef.get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        Log.d("FetchExchangeRates", "Document data: ${document.data}")
                        exchangeRates = document.data?.mapValues { (_, value) ->
                            when (value) {
                                is Double -> value
                                is String -> value.toDoubleOrNull()
                                else -> null
                            }
                        }?.filterValues { it != null } as? Map<String, Double>

                        if (exchangeRates != null) {
                            Log.d("FetchExchangeRates", "Exchange rates fetched successfully: $exchangeRates")
                            callback(true)
                        } else {
                            Log.e("FetchExchangeRates", "Failed to parse exchange rates")
                            callback(false)
                        }
                    } else {
                        Log.e("FetchExchangeRates", "Document does not exist")
                        callback(false)
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("FetchExchangeRates", "Error getting document", exception)
                    callback(false)
                }
        }

        fun convertUSDToCurrency(usdAmount: Double, targetCurrencyCode: String, callback: (Double?) -> Unit) {
            Log.d("ConvertUSDToCurrency", "Starting conversion. USD Amount: $usdAmount, Target: $targetCurrencyCode")

            if (targetCurrencyCode == "USD") {
                Log.d("ConvertUSDToCurrency", "Target is USD, no conversion needed")
                callback(usdAmount)
                return
            }

            val cachedRate = ExchangeRateCache.getRate(targetCurrencyCode)
            if (cachedRate != null && ExchangeRateCache.isCacheValid()) {
                Log.d("ConvertUSDToCurrency", "Using cached rate: $cachedRate")
                val result = usdAmount * cachedRate
                callback(result)
                return
            }

            // If not in cache or cache is invalid, fetch from Firestore
            fetchExchangeRateFromFirestore(targetCurrencyCode) { rate ->
                if (rate != null) {
                    ExchangeRateCache.setRate(targetCurrencyCode, rate)
                    val result = usdAmount * rate
                    Log.d("ConvertUSDToCurrency", "Conversion successful. Rate: $rate, Result: $result")
                    callback(result)
                } else {
                    Log.e("ConvertUSDToCurrency", "Failed to fetch exchange rate")
                    callback(null)
                }
            }
        }
        private fun fetchExchangeRateFromFirestore(currencyCode: String, callback: (Double?) -> Unit) {
            Log.d("FetchExchangeRate", "Fetching exchange rate for $currencyCode from Firestore")
            val db = FirebaseFirestore.getInstance()
            val docRef = db.collection("c_rates").document("qAGRkjY9ZC7rlrMNNifO")

            docRef.get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val rateValue = document.get(currencyCode)
                        val rate = when (rateValue) {
                            is Double -> rateValue
                            is Long -> rateValue.toDouble()
                            is String -> rateValue.toDoubleOrNull()
                            else -> null
                        }

                        if (rate != null) {
                            Log.d("FetchExchangeRate", "Rate for $currencyCode: $rate")
                            callback(rate)
                        } else {
                            Log.e("FetchExchangeRate", "Rate not found or invalid for $currencyCode")
                            callback(null)
                        }
                    } else {
                        Log.e("FetchExchangeRate", "Document does not exist")
                        callback(null)
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("FetchExchangeRate", "Error getting document", exception)
                    callback(null)
                }
        }

        private fun performConversion(usdAmount: Double, targetCurrencyCode: String, callback: (Double?) -> Unit) {
            Log.d("PerformConversion", "Performing conversion. USD Amount: $usdAmount, Target: $targetCurrencyCode")
            Log.d("PerformConversion", "Exchange rates: $exchangeRates")

            val rate = exchangeRates?.get(targetCurrencyCode)
            if (rate != null) {
                val result = usdAmount * rate
                Log.d("PerformConversion", "Conversion successful. Rate: $rate, Result: $result")
                callback(result)
            } else {
                Log.e("PerformConversion", "Rate not found for $targetCurrencyCode")
                callback(null)
            }
        }
    }
}