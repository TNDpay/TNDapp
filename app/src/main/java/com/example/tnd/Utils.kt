package com.example.tnd

import android.content.Context
import android.util.Log
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*
class Utils {
    companion object {
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
        fun getTokenPriceInDollars(tokenItem: TokenData.TokenItem, callback: (Double?) -> Unit) {
            // Check if the token is USDC, return the value 1 directly
            if (tokenItem.name.equals("USDC", ignoreCase = true)) {
                callback(1.0)
                return
            }

            val retrofit = Retrofit.Builder()
                .baseUrl("https://price.jup.ag/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service = retrofit.create(JupiterApiService::class.java)

            // Use tokenItem.name directly to build the correct request
            service.getPrice(tokenItem.name).enqueue(object : retrofit2.Callback<PriceResponse> {
                override fun onResponse(call: retrofit2.Call<PriceResponse>, response: retrofit2.Response<PriceResponse>) {
                    Log.d("Utils", "API Response: $response")
                    if (response.isSuccessful) {
                        // Assuming 'tokenItem.name' is the key in the response map
                        val price = response.body()?.data?.get(tokenItem.name)?.price
                        callback(price)
                    } else {
                        callback(null)
                    }
                }

                override fun onFailure(call: retrofit2.Call<PriceResponse>, t: Throwable) {
                    callback(null)
                }
            })
        }

    }
}