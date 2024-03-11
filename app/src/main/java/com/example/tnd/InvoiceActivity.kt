package com.example.tnd

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.os.Bundle
import android.widget.*
import android.view.View
import android.content.Context
import android.view.ViewGroup
import android.view.LayoutInflater
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.util.Log
import com.example.tnd.TokenData





class InvoiceActivity : Activity() {

    private lateinit var nfcAdapter: NfcAdapter
    private lateinit var editTextAddress: EditText
    private lateinit var editTextAmount: EditText
    private lateinit var textViewUSDCAmount: TextView
    private lateinit var spinnerToken: Spinner
    private lateinit var pendingIntent: PendingIntent
    private lateinit var intentFiltersArray: Array<IntentFilter>
    private lateinit var techListsArray: Array<Array<String>>
    private lateinit var myAddressButton: Button
    private var userAddress: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_invoice)

        editTextAddress = findViewById(R.id.editTextAddress)
        editTextAmount = findViewById(R.id.editTextAmount)
        spinnerToken = findViewById(R.id.spinnerToken)
        myAddressButton = findViewById(R.id.buttonMyAddress)
        textViewUSDCAmount = findViewById(R.id.textViewDollarAmount)

        userAddress = intent.getStringExtra("USER_ADDRESS")
        myAddressButton.isEnabled = editTextAddress.text.isEmpty()

        myAddressButton.setOnClickListener {
            // Set the user address in EditText when the button is clicked
            editTextAddress.setText(userAddress)
            myAddressButton.isEnabled = false

        }
        setupAmountWatcher()
        editTextAddress.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                // Not needed for this implementation
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                // Not needed for this implementation
            }

            override fun afterTextChanged(s: Editable) {
                // Disable "My Address" button if EditText is not empty
                if (s.isNullOrEmpty()) {
                    myAddressButton.isEnabled = true
                    myAddressButton.visibility = View.VISIBLE // Show button when there's no text
                } else {
                    myAddressButton.visibility = View.GONE
                    myAddressButton.isEnabled = false// Hide button when there's text
                }
            }
        })

        // Set up the token spinner
        val tokenAdapter = TokenAdapter(this, TokenData.tokenList)
        spinnerToken.adapter = tokenAdapter


        spinnerToken.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val selectedToken = parent.getItemAtPosition(position) as TokenData.TokenItem
                // Handle the selected token
                Log.d("InvoiceActivity", "Selected Token: ${selectedToken.name}")
                Utils.getTokenPriceInDollars(selectedToken) { price ->
                    if (price != null) {
                        // Update UI with the fetched price
                        Log.d("InvoiceActivity", "Price of ${selectedToken.name} is $price USD")
                    } else {
                        // Handle the error scenario
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
            // Check if NFC is available and enabled
            if (nfcAdapter == null || !nfcAdapter.isEnabled) {
                Toast.makeText(this, "NFC is not available or not enabled", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            val selectedToken = spinnerToken.selectedItem as TokenData.TokenItem
            Log.d("InvoiceActivity", "Selected Token ID: ${selectedToken.id}")
            val sharedPref = getSharedPreferences("NFC_DATA", MODE_PRIVATE)
            with(sharedPref.edit()) {
                putString("address", editTextAddress.text.toString())
                putString("amount", editTextAmount.text.toString())
                putString("id", selectedToken.id.toString())
                apply()
            }

            // Your logic to create an NdefMessage goes here
            val messageToSend = NdefMessage(
                arrayOf(
                    NdefRecord.createTextRecord(null, "${editTextAddress.text}\n${editTextAmount.text}\n${selectedToken.id}")
                )
            )

            nfcAdapter.setNdefPushMessage(messageToSend, this)
            Toast.makeText(this, "Ready to send message via NFC", Toast.LENGTH_LONG).show()
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
                val languageCodeLength = payload[0].toInt() and 0x3F // Mask the status byte to get the language code length
                val message = String(payload, languageCodeLength + 1, payload.size - languageCodeLength - 1, charset("UTF-8"))
            }
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
    private fun setupAmountWatcher() {
        editTextAmount.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                updateDollarAmount()
            }

            // You need to override these methods as well, but you can leave them empty
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }
    private fun updateDollarAmount() {
        val amountString = editTextAmount.text.toString()
        if (amountString.isNotEmpty()) {
            val amount = amountString.toDoubleOrNull()
            val selectedToken = spinnerToken.selectedItem as TokenData.TokenItem

            if (amount != null) {
                Utils.getTokenPriceInDollars(selectedToken) { price ->
                    price?.let {
                        val totalInDollars = amount * it
                        runOnUiThread {
                            textViewUSDCAmount.text = "$${String.format("%.2f", totalInDollars)}"
                        }
                    } ?: runOnUiThread {
                        textViewUSDCAmount.text = "Price unavailable"
                    }
                }
            }
        } else {
            textViewUSDCAmount.text = ""
        }
    }
}
