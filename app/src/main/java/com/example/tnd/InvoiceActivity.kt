package com.example.tnd

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.NdefMessage
import android.os.Bundle
import android.widget.*
import android.view.View
import android.content.Context
import android.graphics.Typeface
import android.view.ViewGroup
import android.view.LayoutInflater
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.util.Log
import com.example.tnd.SetPreferencesActivity.Preferences
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputLayout

class InvoiceActivity : Activity() {

    private lateinit var nfcAdapter: NfcAdapter
    private lateinit var editTextAddress: EditText
    private lateinit var editTextAmount: EditText
    private lateinit var editTextFiatAmount: EditText
    private lateinit var textViewTokenAmount: TextView
    private lateinit var spinnerToken: Spinner
    private lateinit var pendingIntent: PendingIntent
    private lateinit var intentFiltersArray: Array<IntentFilter>
    private lateinit var techListsArray: Array<Array<String>>
    private lateinit var myAddressButton: Button
    private lateinit var userCurrency: Currency
    private var userAddress: String? = null
    private var connectedNetwork: String? = null
    private var isUsingFiat = false
    private lateinit var switchCurrency: SwitchMaterial


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_invoice)

        editTextAddress = findViewById(R.id.editTextAddress)
        editTextAmount = findViewById(R.id.editTextAmount)
        editTextFiatAmount = findViewById(R.id.editTextFiatAmount)
        spinnerToken = findViewById(R.id.spinnerToken)
        myAddressButton = findViewById(R.id.buttonMyAddress)
        textViewTokenAmount = findViewById(R.id.textViewTokenAmount)
        switchCurrency = findViewById(R.id.switchCurrency)

        userAddress = intent.getStringExtra("USER_ADDRESS")
        connectedNetwork = intent.getStringExtra("CONNECTED_NETWORK")
        editTextAmount.setText("0.00")
        myAddressButton.isEnabled = editTextAddress.text.isEmpty()

        myAddressButton.setOnClickListener {
            editTextAddress.setText(userAddress)
            myAddressButton.isEnabled = false
        }

        // Load user's preferred currency
        val currencyCode = SetPreferencesActivity.Preferences.getDefaultBaseCurrency(this)
        userCurrency = currencies.find { it.code == currencyCode } ?: currencies.first()
        // Update the hint text of the Fiat Amount input field
        val fiatAmountInputLayout = findViewById<TextInputLayout>(R.id.fiatAmountInputLayout)
        fiatAmountInputLayout.hint = "Fiat Amount (${userCurrency.symbol})"

        setupAmountWatcher()
        setupFiatAmountWatcher()
        setupAddressTextWatcher()
        setupCurrencySwitch()


        // Set up the token spinner
        val tokenAdapter = when (connectedNetwork) {
            "Solana" -> TokenAdapter(this, TokenData.tokenList_sol)
            "XMR" -> TokenAdapter(this, TokenData.tokenList_xmr)
            else -> TokenAdapter(this, TokenData.tokenList_sol)
        }
        spinnerToken.adapter = tokenAdapter
        val defaultInvoiceTokenId = Preferences.getDefaultInvoiceTokenId(this)
        val defaultInvoiceToken = TokenData.tokenList_sol.find { it.id == defaultInvoiceTokenId }
        defaultInvoiceToken?.let {
            spinnerToken.setSelection(tokenAdapter.getPosition(it))
        }
        updateFiatAmount()

        spinnerToken.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val selectedToken = parent.getItemAtPosition(position) as TokenData.TokenItem
                Log.d("InvoiceActivity", "Selected Token: ${selectedToken.name}")
                if (isUsingFiat) {
                    updateTokenAmount()
                } else {
                    updateFiatAmount()
                }
                getPrice(selectedToken) { price ->
                    if (price != null) {
                        Log.d("InvoiceActivity", "Price of ${selectedToken.name} is $price USD")
                    } else {
                        Log.d("InvoiceActivity", "Failed to fetch the price for ${selectedToken.name}")
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Optional: handle no selection
            }
        }

        val buttonSend: Button = findViewById(R.id.buttonSend)

        buttonSend.setOnClickListener {
            if (nfcAdapter?.isEnabled == true) {
                prepareAndStoreNfcData()
                Toast.makeText(this, "Ready to send via NFC. Tap devices together.", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "NFC is not available or not enabled", Toast.LENGTH_LONG).show()
            }
        }

        // Check for available NFC Adapter
        nfcAdapter = NfcAdapter.getDefaultAdapter(this) ?: run {
            Toast.makeText(this, "This device doesn't support NFC.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Check if NFC is enabled
        if (!nfcAdapter.isEnabled) {
            Toast.makeText(this, "NFC is disabled.", Toast.LENGTH_LONG).show()
        }

        // Create a PendingIntent object so the Android system can populate it with the details of the tag when it is scanned
        pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Setup an intent filter for all MIME based dispatches
        val ndef = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)
        try {
            ndef.addDataType("*/*")
        } catch (e: IntentFilter.MalformedMimeTypeException) {
            throw RuntimeException("fail", e)
        }

        intentFiltersArray = arrayOf(ndef)

        // Setup a tech list for all NfcF tags
        techListsArray = arrayOf(arrayOf(android.nfc.tech.NfcF::class.java.name))
        val initialToken = spinnerToken.selectedItem as TokenData.TokenItem
        getPrice(initialToken) { price ->
            runOnUiThread {
                if (price != null) {
                    updateInputFieldsState(isUsingFiat)
                } else {
                    // Handle price fetch failure
                    Toast.makeText(this, "Failed to fetch initial price", Toast.LENGTH_SHORT).show()
                }
            }
        }

    }
    private fun setupCurrencySwitch() {
        switchCurrency.setOnCheckedChangeListener { _, isChecked ->
            isUsingFiat = isChecked
            updateInputFieldsState(isChecked)
        }
    }
    private fun updateInputFieldsState(isFiatActive: Boolean) {
        // Enable/disable input fields
        editTextAmount.isEnabled = !isFiatActive
        editTextFiatAmount.isEnabled = isFiatActive

        // Change background color or alpha to indicate active field
        findViewById<TextInputLayout>(R.id.amountInputLayout).alpha = if (isFiatActive) 0.5f else 1.0f
        findViewById<TextInputLayout>(R.id.fiatAmountInputLayout).alpha = if (isFiatActive) 1.0f else 0.5f

        // Keep hints visible for both fields
        findViewById<TextInputLayout>(R.id.amountInputLayout).isHintEnabled = true
        findViewById<TextInputLayout>(R.id.fiatAmountInputLayout).isHintEnabled = true

        // Update text style for currency labels
        findViewById<TextView>(R.id.textViewToken).typeface = if (isFiatActive) Typeface.DEFAULT else Typeface.DEFAULT_BOLD
        findViewById<TextView>(R.id.textViewFiat).typeface = if (isFiatActive) Typeface.DEFAULT_BOLD else Typeface.DEFAULT

        // Update amounts
        if (isFiatActive) {
            updateTokenAmount()
        } else {
            updateFiatAmount()
        }
    }
    private fun setupAmountWatcher() {
        editTextAmount.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (!isUsingFiat) {
                    updateFiatAmount()
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupFiatAmountWatcher() {
        editTextFiatAmount.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (isUsingFiat) {
                    updateTokenAmount()
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupAddressTextWatcher() {
        editTextAddress.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                if (s.isNullOrEmpty()) {
                    myAddressButton.isEnabled = true
                    myAddressButton.visibility = View.VISIBLE
                } else {
                    myAddressButton.visibility = View.GONE
                    myAddressButton.isEnabled = false
                }
            }
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        })
    }

    private fun updateFiatAmount() {
        val amountString = editTextAmount.text.toString()
        val amount = amountString.toDoubleOrNull() ?: 0.0
        val selectedToken = spinnerToken.selectedItem as TokenData.TokenItem

        getPrice(selectedToken) { price ->
            price?.let {
                val totalInUSD = amount * it
                Utils.convertUSDToCurrency(totalInUSD, userCurrency.code) { convertedAmount ->
                    runOnUiThread {
                        if (convertedAmount != null) {
                            editTextFiatAmount.setText(String.format("%.2f", convertedAmount))
                            textViewTokenAmount.text = if (amount > 0) {
                                "${String.format("%.8f", amount)} ${selectedToken.name}"
                            } else {
                                "0 ${selectedToken.name}"
                            }
                        } else {
                            editTextFiatAmount.setText("")
                            textViewTokenAmount.text = "Conversion unavailable"
                        }
                    }
                }
            } ?: runOnUiThread {
                editTextFiatAmount.setText("")
                textViewTokenAmount.text = "Price unavailable"
            }
        }
    }

    private fun updateTokenAmount() {
        val fiatAmountString = editTextFiatAmount.text.toString()
        val fiatAmount = fiatAmountString.toDoubleOrNull() ?: 0.0
        val selectedToken = spinnerToken.selectedItem as TokenData.TokenItem

        getPrice(selectedToken) { price ->
            price?.let {
                val tokenAmount = fiatAmount / it
                runOnUiThread {
                    editTextAmount.setText(String.format("%.8f", tokenAmount))
                    textViewTokenAmount.text = if (tokenAmount > 0) {
                        "${String.format("%.8f", tokenAmount)} ${selectedToken.name}"
                    } else {
                        "0 ${selectedToken.name}"
                    }
                }
            } ?: runOnUiThread {
                editTextAmount.setText("")
                textViewTokenAmount.text = "Price unavailable"
            }
        }
    }

    public override fun onResume() {
        super.onResume()
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, techListsArray)
    }

    public override fun onPause() {
        super.onPause()
        nfcAdapter.disableForegroundDispatch(this)
    }

    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
            val rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            if (rawMessages != null && rawMessages.isNotEmpty()) {
                val ndefMessage = rawMessages[0] as NdefMessage
                val record = ndefMessage.records[0]
                val payload = record.payload
                val languageCodeLength = payload[0].toInt() and 0x3F
                val message = String(payload, languageCodeLength + 1, payload.size - languageCodeLength - 1, charset("UTF-8"))
            }
        }
    }

    private fun getPrice(selectedToken: TokenData.TokenItem, callback: (Double?) -> Unit) {
        Log.d("InvoiceActivity","Connected Network is: ${connectedNetwork}")
        connectedNetwork = "Solana" // Tmp, fix while we are only in Solana
        when (connectedNetwork) {
            "Solana" -> {
                SolanaUtils.getTokenPriceInDollars(selectedToken) { price ->
                    callback(price)
                }
                Log.d("InvoiceActivity", "Using Solana")
            }
            "XMR" -> {
                XMRUtils.getXMRPriceInUSD { price ->
                    callback(price)
                }
            }
            else -> callback(null)
        }
    }

    private fun prepareAndStoreNfcData() {
        val selectedToken = spinnerToken.selectedItem as TokenData.TokenItem
        val address = editTextAddress.text.toString()
        val amount = editTextAmount.text.toString()

        getSharedPreferences("NFC_DATA", MODE_PRIVATE).edit().apply {
            putString("address", address)
            putString("amount", amount)
            putString("id", selectedToken.id.toString())
            apply()
        }
    }

    // Custom adapter for the token spinner
    class TokenAdapter(context: Context, tokenList: List<TokenData.TokenItem>) : ArrayAdapter<TokenData.TokenItem>(context, 0, tokenList) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            return createItemView(position, convertView, parent)
        }

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
            return createItemView(position, convertView, parent)
        }

        private fun createItemView(position: Int, recycledView: View?, parent: ViewGroup): View {
            val tokenItem = getItem(position)
            val view = recycledView ?: LayoutInflater.from(context).inflate(R.layout.token_spinner_item, parent, false)
            view.findViewById<ImageView>(R.id.imageToken).setImageResource(tokenItem?.imageResId ?: 0)
            view.findViewById<TextView>(R.id.textToken).text = tokenItem?.name
            return view
        }
    }
}