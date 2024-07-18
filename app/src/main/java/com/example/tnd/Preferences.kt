package com.example.tnd

import android.os.Bundle
import android.widget.Button
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log

class SetPreferencesActivity : AppCompatActivity() {

    private lateinit var spinnerPaymentToken: Spinner
    private lateinit var spinnerInvoiceToken: Spinner
    private lateinit var saveButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_preferences)

        spinnerPaymentToken = findViewById(R.id.spinnerPaymentToken)
        spinnerInvoiceToken = findViewById(R.id.spinnerInvoiceToken)
        saveButton = findViewById(R.id.buttonSave)

        setupSpinners()

        saveButton.setOnClickListener {
            savePreferences()
        }
    }

    private fun setupSpinners() {
        val tokenAdapter = MainActivity.TokenAdapter(this, TokenData.tokenList_sol)

        spinnerPaymentToken.adapter = tokenAdapter
        spinnerInvoiceToken.adapter = tokenAdapter

        // Set default selections based on saved preferences
        val defaultPaymentTokenId = Preferences.getDefaultPaymentTokenId(this)
        val defaultInvoiceTokenId = Preferences.getDefaultInvoiceTokenId(this)

        val paymentTokenPosition = TokenData.tokenList_sol.indexOfFirst { it.id == defaultPaymentTokenId }
        val invoiceTokenPosition = TokenData.tokenList_sol.indexOfFirst { it.id == defaultInvoiceTokenId }

        if (paymentTokenPosition != -1) spinnerPaymentToken.setSelection(paymentTokenPosition)
        if (invoiceTokenPosition != -1) spinnerInvoiceToken.setSelection(invoiceTokenPosition)
    }

    private fun savePreferences() {
        val selectedPaymentToken = spinnerPaymentToken.selectedItem as TokenData.TokenItem
        val selectedInvoiceToken = spinnerInvoiceToken.selectedItem as TokenData.TokenItem

        Preferences.setDefaultPaymentTokenId(this, selectedPaymentToken.id)
        Preferences.setDefaultInvoiceTokenId(this, selectedInvoiceToken.id)

        Log.d("SetPreferencesActivity", "Saved payment token: ${selectedPaymentToken.name}, invoice token: ${selectedInvoiceToken.name}")
    }

    object Preferences {
        private const val PREF_NAME = "UserPreferences"
        private const val KEY_DEFAULT_PAYMENT_TOKEN_ID = "default_payment_token_id"
        private const val KEY_DEFAULT_INVOICE_TOKEN_ID = "default_invoice_token_id"

        private fun getSharedPreferences(context: Context): SharedPreferences {
            return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        }

        fun setDefaultPaymentTokenId(context: Context, tokenId: Int) {
            getSharedPreferences(context)
                .edit()
                .putInt(KEY_DEFAULT_PAYMENT_TOKEN_ID, tokenId)
                .apply()
        }

        fun getDefaultPaymentTokenId(context: Context): Int {
            return getSharedPreferences(context).getInt(KEY_DEFAULT_PAYMENT_TOKEN_ID, 1) // Default to 1 (SOL) if not set
        }

        fun setDefaultInvoiceTokenId(context: Context, tokenId: Int) {
            getSharedPreferences(context)
                .edit()
                .putInt(KEY_DEFAULT_INVOICE_TOKEN_ID, tokenId)
                .apply()
        }

        fun getDefaultInvoiceTokenId(context: Context): Int {
            return getSharedPreferences(context).getInt(KEY_DEFAULT_INVOICE_TOKEN_ID, 1) // Default to 1 (SOL) if not set
        }
    }
}