package com.example.tnd

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
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

        suspend fun getMinContextSlot(rpcService: RpcService): Map<String, Long>? {
            return try {
                val slot = rpcService.getCurrentSlot()
                if (slot != null) {
                    mapOf("minContextSlot" to slot)
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e("Utils", "Error getting minContextSlot: ${e.message}")
                null
            }
        }
    }
}