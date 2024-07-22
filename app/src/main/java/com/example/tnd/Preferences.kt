package com.example.tnd

import android.os.Bundle
import android.widget.Button
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class CurrencyAdapter(context: Context, currencies: List<Currency>) :
    ArrayAdapter<Currency>(context, android.R.layout.simple_spinner_item, currencies) {

    init {
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        val currency = getItem(position)
        (view as TextView).text = "${currency?.symbol} ${currency?.name}"
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getDropDownView(position, convertView, parent)
        val currency = getItem(position)
        (view as TextView).text = "${currency?.symbol} ${currency?.name}"
        return view
    }
}
class SetPreferencesActivity : AppCompatActivity() {

    private lateinit var spinnerPaymentToken: Spinner
    private lateinit var spinnerInvoiceToken: Spinner
    private lateinit var spinnerBaseCurrency: Spinner
    private lateinit var saveButton: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_preferences)

        spinnerPaymentToken = findViewById(R.id.spinnerPaymentToken)
        spinnerInvoiceToken = findViewById(R.id.spinnerInvoiceToken)
        spinnerBaseCurrency = findViewById(R.id.spinnerBaseCurrency)

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

        // Setup base currency spinner
        val currencyAdapter = CurrencyAdapter(this, currencies)
        spinnerBaseCurrency.adapter = currencyAdapter


        // Set default selection based on saved preference
        val defaultCurrencyCode = Preferences.getDefaultBaseCurrency(this)
        val currencyPosition = currencies.indexOfFirst { it.code == defaultCurrencyCode }
        if (currencyPosition != -1) spinnerBaseCurrency.setSelection(currencyPosition)
    }

    private fun savePreferences() {
        val selectedPaymentToken = spinnerPaymentToken.selectedItem as TokenData.TokenItem
        val selectedInvoiceToken = spinnerInvoiceToken.selectedItem as TokenData.TokenItem
        val selectedCurrency = spinnerBaseCurrency.selectedItem as Currency

        Preferences.setDefaultPaymentTokenId(this, selectedPaymentToken.id)
        Preferences.setDefaultInvoiceTokenId(this, selectedInvoiceToken.id)
        Preferences.setDefaultBaseCurrency(this, selectedCurrency.code)

        Log.d("SetPreferencesActivity", "Saved payment token: ${selectedPaymentToken.name}, invoice token: ${selectedInvoiceToken.name}, base currency: ${selectedCurrency.name}")
        finish()
    }

    object Preferences {
        private const val PREF_NAME = "UserPreferences"
        private const val KEY_DEFAULT_PAYMENT_TOKEN_ID = "default_payment_token_id"
        private const val KEY_DEFAULT_INVOICE_TOKEN_ID = "default_invoice_token_id"
        private const val KEY_DEFAULT_BASE_CURRENCY = "default_base_currency"

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
        fun setDefaultBaseCurrency(context: Context, currencyCode: String) {
            getSharedPreferences(context)
                .edit()
                .putString(KEY_DEFAULT_BASE_CURRENCY, currencyCode)
                .apply()
        }
        fun getDefaultBaseCurrency(context: Context): String {
            return getSharedPreferences(context).getString(KEY_DEFAULT_BASE_CURRENCY, "USD") ?: "USD"
        }
    }
}
data class Currency(val code: String, val name: String, val symbol: String)
val currencies = listOf(
    Currency("USD", "US Dollar", "$"),
    Currency("EUR", "Euro", "€"),
    Currency("JPY", "Japanese Yen", "¥"),
    Currency("GBP", "British Pound", "£"),
    Currency("AUD", "Australian Dollar", "A$"),
    Currency("CAD", "Canadian Dollar", "C$"),
    Currency("CHF", "Swiss Franc", "Fr"),
    Currency("CNY", "Chinese Yuan", "¥"),
    Currency("HKD", "Hong Kong Dollar", "HK$"),
    Currency("SGD", "Singapore Dollar", "S$"),
    Currency("INR", "Indian Rupee", "₹"),
    Currency("KRW", "South Korean Won", "₩")
)