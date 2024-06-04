package com.example.tnd

import android.app.AlertDialog
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
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
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
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