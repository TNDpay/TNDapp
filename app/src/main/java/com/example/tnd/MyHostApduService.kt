package com.example.tnd

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log

class MyHostApduService : HostApduService() {

    override fun processCommandApdu(apdu: ByteArray, extras: Bundle?): ByteArray {
        Log.d("MyHostApduService", "Received APDU: ${apdu.joinToString(", ") { String.format("%02X", it) }}")


        val selectAidApdu = byteArrayOf(0x00.toByte(), 0xA4.toByte(), 0x04.toByte(), 0x00.toByte(), // Header
            0x07.toByte(), // Length of the AID
            0xA0.toByte(), 0x00.toByte(), 0x00.toByte(), 0x02.toByte(), 0x47.toByte(), 0x10.toByte(), 0x01.toByte()) // AID
        if (apdu contentEquals selectAidApdu) {
            val sharedPref = getSharedPreferences("NFC_DATA", MODE_PRIVATE)
            val address = sharedPref.getString("address", "defaultAddress")
            val amount = sharedPref.getString("amount", "defaultAmount")
            val tokenId = sharedPref.getString("id", null)
            val ethAddressAndAmount = "$address\n$amount\n$tokenId"
            val responseData = ethAddressAndAmount.toByteArray()

            // Make sure the combined data does not exceed the APDU response size limit
            if (responseData.size <= 255) {
                return responseData
            } else {
                Log.d("MyHostApduService", "Response data too long")
                return ByteArray(0)
            }
        } else {
            Log.d("MyHostApduService", "Unknown command, sending empty response")
            return ByteArray(0)
        }
    }




    override fun onCreate() {
        super.onCreate()
        Log.d("MyHostApduService", "Service created")
    }

    override fun onDeactivated(reason: Int) {
        Log.d("MyHostApduService", "Service deactivated")
    }


    companion object {
        // This is the AID (Application Identifier) for the service.
        // It s unique and registered with the NFC system on the device.
        private const val AID = "A0000002471001"
    }
}
