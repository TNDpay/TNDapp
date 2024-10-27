package com.example.tnd

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.util.Log
import androidx.camera.view.PreviewView
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory


class QRScannerActivity : AppCompatActivity() {
    private lateinit var barcodeView: DecoratedBarcodeView

    companion object {
        private const val CAMERA_PERMISSION_REQUEST = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qrscanner)

        barcodeView = findViewById(R.id.barcodeView)

        // Configure QR code scanning only
        barcodeView.decoderFactory = DefaultDecoderFactory(listOf(com.google.zxing.BarcodeFormat.QR_CODE))

        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startScanning()
        } else {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST)
        }
    }
    private fun processQRContent(qrContent: String) {
        try {
            val parts = qrContent.split(",")
            if (parts.size >= 3) {
                val address = parts[0]
                val amount = parts[1].toDoubleOrNull()
                val tokenId = parts[2].toIntOrNull()

                if (amount != null && tokenId != null) {
                    // Store the payment data in SharedPreferences
                    getSharedPreferences("NFC_DATA", MODE_PRIVATE).edit().apply {
                        putString("address", address)
                        putString("amount", amount.toString())
                        putString("id", tokenId.toString())
                        apply()
                    }

                    // Log successful scan
                    Log.d("QRScanner", "Payment data processed - Address: $address, Amount: $amount, TokenId: $tokenId")

                    setResult(RESULT_OK)
                    finish()
                } else {
                    Log.e("QRScanner", "Invalid amount or token ID format")
                    Toast.makeText(this, "Invalid QR code format", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.e("QRScanner", "Invalid QR code format - insufficient parts")
                Toast.makeText(this, "Invalid QR code format", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("QRScanner", "Error processing QR code", e)
            Toast.makeText(this, "Error processing QR code", Toast.LENGTH_SHORT).show()
        }
    }
    private fun startScanning() {
        barcodeView.decodeContinuous(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult) {
                // Process QR code content
                result.text?.let { qrContent ->
                    processQRContent(qrContent)
                }
            }
        })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanning()
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        barcodeView.resume()
    }

    override fun onPause() {
        super.onPause()
        barcodeView.pause()
    }
}