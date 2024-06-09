package com.example.tnd

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.solana.core.PublicKey
import com.solana.programs.TokenProgram
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*
class SolanaUtils {
    companion object {

        fun findAssociatedTokenAddress(
            walletAddress: PublicKey,
            tokenMintAddress: PublicKey
        ): PublicKey {
            val associatedTokenAccountProgramId = PublicKey("ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL")

            // Construct the seeds for the findProgramAddress method
            val seeds = listOf(
                walletAddress.toByteArray(),
                TokenProgram.PROGRAM_ID.toByteArray(),
                tokenMintAddress.toByteArray()
            )

            // Find and return the associated token address
            return PublicKey.findProgramAddress(seeds, associatedTokenAccountProgramId).address
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
        fun checkAddressForFlag(context: Context, address: String) {
            CoroutineScope(Dispatchers.IO).launch {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("https://api.solana.fm/v0/accounts/$address")
                    .get()
                    .addHeader("accept", "application/json")
                    .build()

                try {
                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        if (responseBody != null) {
                            val gson = Gson()
                            val jsonResponse = gson.fromJson(responseBody, JsonObject::class.java)
                            val status = jsonResponse.get("status").asString
                            if (status == "Success") {
                                val result = jsonResponse.getAsJsonObject("result")
                                val data = result.getAsJsonObject("data")
                                val flag = data.get("flag").asString
                                if (flag == "hacker") {
                                    withContext(Dispatchers.Main) {
                                        AlertDialog.Builder(context).apply {
                                            setTitle("Security Alert")
                                            setMessage("This address is flagged as a hacker: $address")
                                            setPositiveButton("OK", null)
                                            create().show()
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Handle response error
                        Log.e("checkAddressForFlag", "Failed to fetch address data: ${response.message}")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Handle network error or parsing error
                }
            }
        }
    }
}