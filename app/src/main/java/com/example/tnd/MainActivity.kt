    package com.example.tnd

    import android.content.Context
    import android.content.Intent
    import android.net.Uri
    import android.nfc.NfcAdapter
    import android.nfc.Tag
    import android.nfc.tech.IsoDep
    import android.os.Bundle
    import android.util.Base64
    import android.util.Log
    import android.view.LayoutInflater
    import android.view.View
    import android.view.ViewGroup
    import android.widget.AdapterView
    import android.widget.ArrayAdapter
    import android.widget.Button
    import android.widget.ImageView
    import android.widget.Spinner
    import android.widget.TextView
    import androidx.appcompat.app.AppCompatActivity
    import androidx.core.view.GravityCompat
    import androidx.drawerlayout.widget.DrawerLayout
    import com.google.android.material.navigation.NavigationView
    import com.solana.core.PublicKey
    import com.solana.core.SerializeConfig
    import com.solana.core.Transaction
    import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
    import com.solana.mobilewalletadapter.clientlib.MobileWalletAdapter
    import com.solana.mobilewalletadapter.clientlib.TransactionResult
    import com.solana.programs.AssociatedTokenProgram
    import com.solana.programs.SystemProgram
    import com.solana.programs.TokenProgram
    import kotlinx.coroutines.CoroutineScope
    import kotlinx.coroutines.Dispatchers
    import kotlinx.coroutines.Job
    import kotlinx.coroutines.launch
    import kotlinx.coroutines.withContext
    import org.bitcoinj.core.Base58
    import retrofit2.Call
    import retrofit2.Callback
    import retrofit2.Response
    import retrofit2.Retrofit
    import retrofit2.converter.gson.GsonConverterFactory
    import kotlin.math.pow
    import androidx.browser.customtabs.CustomTabsIntent
    import androidx.browser.customtabs.CustomTabColorSchemeParams
    import android.widget.Toast
    import android.app.AlertDialog
    import android.icu.math.BigDecimal
    import okhttp3.OkHttpClient
    import okhttp3.Request
    import com.google.gson.Gson
    import com.google.gson.JsonObject
    import io.metamask.androidsdk.Ethereum
    import io.metamask.androidsdk.Dapp
    import io.metamask.androidsdk.RequestError
    import android.widget.LinearLayout
    import io.metamask.androidsdk.*
    import org.web3j.utils.Numeric
    import java.math.BigInteger
    import org.web3j.protocol.Web3j
    import org.web3j.protocol.http.HttpService
    import org.web3j.abi.datatypes.Function
    import org.web3j.abi.FunctionEncoder
    import org.web3j.abi.TypeReference
    import org.web3j.abi.datatypes.Address
    import org.web3j.abi.datatypes.generated.Uint256
    import org.web3j.abi.datatypes.generated.Uint8
    import org.web3j.protocol.core.DefaultBlockParameterName
    class MainActivity : AppCompatActivity(), NfcAdapter.ReaderCallback {
        private lateinit var textView: TextView
        private lateinit var connectWalletButton: Button
        private lateinit var PayButton: Button
        private lateinit var declineButton: Button
        private lateinit var spinnerToken: Spinner
        private var paymentAddress: String? = null
        private var paymentAmount: Double? = null
        private var tokenId: Int?=null
        private var nfcAdapter: NfcAdapter? = null
        private var userAddress = ""
        private var noWallet = false
        private var canTransact = false
        private val scope = CoroutineScope(Job() + Dispatchers.Main)
        private lateinit var activityResultSender: ActivityResultSender
        private lateinit var identityUri: Uri
        private lateinit var iconUri: Uri
        private lateinit var identityName: String
        private lateinit var authToken: String
        private lateinit var drawerLayout: DrawerLayout
        private lateinit var addressTextView: TextView
        private lateinit var cardLayout: View
        private var connectedNetwork: String = ""
        private lateinit var ethereum: Ethereum



        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main)
            activityResultSender = ActivityResultSender(this)

            textView = findViewById(R.id.textView)
            connectWalletButton = findViewById(R.id.connectWalletButton)
            PayButton = findViewById(R.id.PayButton)
            declineButton = findViewById(R.id.DeclineButton)
            spinnerToken = findViewById(R.id.spinnerToken)
            nfcAdapter = NfcAdapter.getDefaultAdapter(this)
            identityUri = Uri.parse(getString(R.string.id_url))
            iconUri = Uri.parse(getString(R.string.id_favicon))
            identityName = getString(R.string.app_name)
            drawerLayout = findViewById(R.id.drawer_layout)
            PayButton.visibility = View.GONE
            declineButton.visibility = View.GONE
            spinnerToken.visibility = View.GONE
            addressTextView = findViewById(R.id.addressTextView)
            cardLayout = findViewById(R.id.cardLayout)


            if (hasValidAuthToken()) {
                connectWalletButton.visibility = View.GONE
                cardLayout.visibility = View.VISIBLE
            } else {
                connectWalletButton.visibility = View.VISIBLE
                cardLayout.visibility = View.GONE
            }
            val (retrievedAuthToken, retrievedUserAddress) = retrieveAuthData()
            if (retrievedAuthToken != null && retrievedUserAddress != null) {
                this.authToken = retrievedAuthToken
                this.userAddress = retrievedUserAddress
                canTransact = true
                updateUserAddressUI(retrievedUserAddress)

            } else {
                connectWalletButton.visibility = View.VISIBLE
                connectWalletButton.setOnClickListener {
                    connectWallet()
                }
            }
            connectWalletButton.setOnClickListener {
                connectWallet()
            }
            val chargeButton: Button = findViewById(R.id.ChargeButton)
            chargeButton.setOnClickListener {
                // Start InvoiceActivity when the charge button is clicked
                val intent = Intent(this@MainActivity, InvoiceActivity::class.java)
                intent.putExtra("USER_ADDRESS", userAddress)
                startActivity(intent)
            }
            val exploreBoutton: Button = findViewById(R.id.exploreButton)
            exploreBoutton.setOnClickListener {
                val intent = Intent(this@MainActivity, ExploreActivity::class.java)
                startActivity(intent)
            }
            PayButton.setOnClickListener {
                    paymentAddress?.let { address ->
                        paymentAmount?.let { amount ->
                            tokenId?.let { id ->
                                val selectedToken = spinnerToken.selectedItem as? TokenData.TokenItem
                                val idSpinner = selectedToken?.id ?: 1
                                when (connectedNetwork) {
                                    "Solana" -> {
                                        if (canTransact) {
                                            Log.e("MainActivity", "Calling payment function")
                                            if (idSpinner == id) { // Checks if the selected token ID matches the tokenId
                                                if (id == 1) {
                                                    Log.d("MainActivity", "SOL PAY ")
                                                    sendSol(address, amount, userAddress)
                                                } else {
                                                    Log.d("MainActivity", "SPL PAY ")
                                                    // Handle the case where idSpinner matches id but is not 1
                                                    buildSendSPLTransaction(address, amount, userAddress, id)
                                                }
                                            } else {
                                                Log.d("MainActivity", "DIFFERENT ID S ")
                                                // If user have chosen other token we have to swap
                                                // Find the selected token to get the number of decimals and mint address
                                                val tokenIn = TokenData.tokenList.find { it.id == idSpinner }
                                                val tokenOut = TokenData.tokenList.find { it.id == id }
                                                // Perform the conversion only if both tokens are found
                                                if (tokenIn != null && tokenOut != null) {
                                                    // Convert the amount from Double to Long based on the token's decimals
                                                    val amountLong = (amount * Math.pow(10.0, tokenOut.decimals.toDouble())).toLong()
                                                    performSwap(tokenIn.mintAddress, tokenOut.mintAddress, amountLong, userAddress, address)
                                                } else {
                                                    Log.e("MainActivity", "One of the tokens could not be found.")
                                                }
                                            }
                                        } else {
                                            Log.e("MainActivity", "Cannot make payment: No user address or can't transact.")
                                        }
                                    }
                                    "Polygon" -> {
                                        CoroutineScope(Dispatchers.Main).launch {
                                            try {
                                                payWMM(amount, address)
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }
                                    }

                                    else -> {
                                        Log.e("MainActivity", "ERROR")
                                    }
                                }
                            } ?: run {
                                Log.e("MainActivity", "Token ID is null.")
                            }
                        }
                    }
            }


            val configButton: Button = findViewById(R.id.configButton)

            configButton.setOnClickListener {
                drawerLayout.openDrawer(GravityCompat.START)
            }
            updateButton()
            val navigationView: NavigationView = findViewById(R.id.nav_view)
            navigationView.setNavigationItemSelectedListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.nav_connect_wallet -> {
                        connectWallet()
                        drawerLayout.closeDrawer(GravityCompat.START)
                        true
                    }
                    R.id.nav_disconnect_wallet -> {
                        disconnectWallet()
                        drawerLayout.closeDrawer(GravityCompat.START)
                        true
                    }
                    //R.id.nav_pay_hist -> {
                        //val intent = Intent(this@MainActivity, HistoryActivity::class.java)
                        //startActivity(intent)
                        //drawerLayout.closeDrawer(GravityCompat.START)
                        //true
                    //}
                    R.id.nav_twitter -> {
                        // Handle Twitter option by opening the Twitter URL
                        openWebPage("https://twitter.com/TNDpayments")
                        drawerLayout.closeDrawer(GravityCompat.START)
                        true
                    }
                    R.id.nav_telegram -> {
                        // Handle Twitter option by opening the Twitter URL
                        openWebPage("https://t.me/tndpay")
                        drawerLayout.closeDrawer(GravityCompat.START)
                        true
                    }
                    R.id.nav_website -> {
                        // Handle Website option by opening the website URL
                        openWebPage("https://www.tndpayments.com/")
                        drawerLayout.closeDrawer(GravityCompat.START)
                        true
                    }
                    R.id.nav_explore_form ->{
                        openWebPage("https://forms.gle/jD5Rrdt1hcCcqdgk7")
                        drawerLayout.closeDrawer(GravityCompat.START)
                        true
                    }
                    else -> false
                }
            }

            declineButton = findViewById(R.id.DeclineButton)
            declineButton.setOnClickListener {
                decline()
            }
            val tokenAdapter = if (connectedNetwork == "Polygon") {
                TokenAdapter(this, TokenData.tokenList)
            } else {
                TokenAdapter(this, TokenData.tokenList_polygon)
            }
            spinnerToken.adapter = tokenAdapter


            spinnerToken.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    if (view == null) {
                        Log.e("MainActivity", "View is null in onItemSelected")
                        return
                    }

                    val selectedToken = parent.getItemAtPosition(position) as TokenData.TokenItem
                    // Handle the selected token
                    Log.d("InvoiceActivity", "Selected Token: ${selectedToken.name}")
                }
                override fun onNothingSelected(parent: AdapterView<*>) {
                    // Optional: handle no selection
                }
            }
            updateMenuItemsVisibility(navigationView)
        }
        private fun updateBackgroundBasedOnNFCState() {
            val nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(this)
            val backgroundImageView = findViewById<ImageView>(R.id.backgroundImageView)
            if (nfcAdapter != null && nfcAdapter.isEnabled) {
                // NFC is enabled - Set the image for NFC on state
                backgroundImageView.setImageResource(R.drawable.nfc_on) // Use your actual drawable resource
            } else {
                // NFC is disabled or not available - Set the image for NFC off state
                backgroundImageView.setImageResource(R.drawable.nfc_off) // Use your actual drawable resource
            }
        }


        private fun updateMenuItemsVisibility(navigationView: NavigationView) {
            val menu = navigationView.menu

            // Find the menu items
            val connectWalletMenuItem = menu.findItem(R.id.nav_connect_wallet)
            val disconnectWalletMenuItem = menu.findItem(R.id.nav_disconnect_wallet)

            if (hasValidAuthToken()) {
                // User is connected: show disconnect and hide connect
                Log.d("MainActivity", "is connected")
                connectWalletMenuItem.isVisible = false
                disconnectWalletMenuItem.isVisible = true
            } else {
                // User is not connected: show connect and hide disconnect
                Log.d("MainActivity", "is disconnected")
                disconnectWalletMenuItem.isVisible = false
                connectWalletMenuItem.isVisible = true
                Log.d("MenuVisibility", "After setting connect visible: ${connectWalletMenuItem.isVisible}")
            }
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
        fun Context.toast(message: String) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
        private fun sendSwap(swapTransaction: String) {
            scope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        // Decode the base64 encoded transaction "
                        val transactionBytes = Base64.decode(swapTransaction, Base64.DEFAULT)
                        // Start the signing process
                        Log.d("sendSwap", "Swap Transaction Response: $transactionBytes")
                        val walletAdapterClient = MobileWalletAdapter() // Initialize properly with context and other parameters
                        val result = walletAdapterClient.transact(activityResultSender) {
                            // These URI and token values should be provided appropriately
                            reauthorize(identityUri, iconUri, identityName, authToken)
                            signAndSendTransactions(arrayOf(transactionBytes))
                        }
                        when (result) {
                            is TransactionResult.Success -> {
                                // Transaction was successful, process the signatures
                                val signatures = result.payload.signatures
                                if (signatures.isNotEmpty()) {
                                    // Log or handle each signature
                                    signatures.forEach { signature ->
                                        val readableSignature = Base58.encode(signature)
                                        Log.d("MainActivity", "Swap transaction successful. Signature: $readableSignature")
                                        runOnUiThread {
                                            AlertDialog.Builder(this@MainActivity)
                                                .setTitle("Transaction Successful")
                                                .setMessage("Do you want to view the transaction on Solana Explorer?")
                                                .setPositiveButton("Yes") { dialog, id ->
                                                    openWebPage("https://solana.fm/tx/$readableSignature?cluster=mainnet-alpha")
                                                }
                                                .setNegativeButton("No") { dialog, id ->
                                                    dialog.dismiss()
                                                }
                                                .create()
                                                .show()
                                        }
                                        decline()
                                    }
                                } else {
                                    Log.e("MainActivity", "Swap transaction successful but no signatures received.")
                                }
                            }
                            is TransactionResult.Failure -> {
                                // Transaction failed
                                Log.e("MainActivity", "Swap transaction failed: ${result}")
                            }
                            is TransactionResult.NoWalletFound -> {
                                // No wallet application found
                                Log.e("MainActivity", "No wallet application found.")
                            }
                            else -> {
                                // Handle any other unexpected results
                                Log.e("MainActivity", "An unknown error occurred during transaction.")
                            }
                        }

                    } catch (e: Exception) {
                        Log.e("MainActivity", "Exception in sendSwapTransaction: ${e.message}")
                    }
                }
            }
        }

        fun quoteResponseFromSwapQuote(swapQuoteResponse: SwapQuoteResponse): QuoteResponse {
            return QuoteResponse(
                inputMint = swapQuoteResponse.inputMint,
                inAmount = swapQuoteResponse.inAmount.toString(),
                outputMint = swapQuoteResponse.outputMint,
                outAmount = swapQuoteResponse.outAmount.toString(),
                otherAmountThreshold = swapQuoteResponse.otherAmountThreshold.toString(),
                swapMode = swapQuoteResponse.swapMode,
                slippageBps = swapQuoteResponse.slippageBps,
                platformFee = PlatformFee(
                    amount = "0",
                    feeBps = swapQuoteResponse.platformFee?.let { (it / 10000).toInt() } ?: 0 // Convert the result to Int
                ),
                priceImpactPct = swapQuoteResponse.priceImpactPct,
                routePlan = swapQuoteResponse.routePlan.map { routePlanItem ->
                    RoutePlan(
                        swapInfo = SwapInfo(
                            ammKey = routePlanItem.swapInfo.ammKey,
                            label = routePlanItem.swapInfo.label,
                            inputMint = routePlanItem.swapInfo.inputMint,
                            outputMint = routePlanItem.swapInfo.outputMint,
                            inAmount = routePlanItem.swapInfo.inAmount,
                            outAmount = routePlanItem.swapInfo.outAmount,
                            feeAmount = routePlanItem.swapInfo.feeAmount,
                            feeMint = routePlanItem.swapInfo.feeMint
                        ),
                        percent = routePlanItem.percent
                    )
                },
                contextSlot = swapQuoteResponse.contextSlot,
                timeTaken = swapQuoteResponse.timeTaken
            )
        }


        fun performSwap(input: String,output: String, amount:Long, senderAddr:String, receiverAddr:String) { //Get Swap Instructions from jupiter API

            // Step 1: Obtain a swap quote
            val retrofit = Retrofit.Builder()
                .baseUrl("https://quote-api.jup.ag/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service = retrofit.create(JupiterApiService::class.java)

            service.getSwapQuote(
                inputMint = input,
                outputMint = output,
                amount = amount,
                swapMode = "ExactOut",
                slippageBps = 100
            ).enqueue(object : retrofit2.Callback<SwapQuoteResponse> {
                override fun onResponse(call: Call<SwapQuoteResponse>, response: Response<SwapQuoteResponse>) {
                    if (response.isSuccessful) {
                        val swapQuote = response.body()
                        val destToken: String
                        Log.d("SwapFunction", "token mint address : $output")
                        if (output=="So11111111111111111111111111111111111111112"){
                            destToken=receiverAddr
                            Log.d("SwapFunction", "Sol addres should be sent to : $receiverAddr")
                        }else {
                            val publicAddresReceiver=PublicKey(receiverAddr)
                            val publicMintRT=PublicKey(output)
                            destToken=findAssociatedTokenAddress(publicAddresReceiver, publicMintRT).toString()
                        }
                        // Proceed to Step 2: Obtain swap instructions using the swap quote
                        swapQuote?.let {
                            val swapRequest = SwapRequest(
                                userPublicKey = senderAddr,
                                //feeAccount = "4RR16cEeahqTGeHANxLBKisnBZmvrWZty6Y6xdZsXrKw",
                                destinationTokenAccount = destToken,
                                quoteResponse = quoteResponseFromSwapQuote(swapQuote)
                            )
                            service.getSwapTransaction(swapRequest).enqueue(object : Callback<SwapTransactionResponse> {
                                override fun onResponse(call: Call<SwapTransactionResponse>, response: Response<SwapTransactionResponse>) {
                                    if (response.isSuccessful) {
                                        val swapTransactionResponse = response.body()
                                        Log.d("SwapFunction", "Swap Transaction Response: $swapTransactionResponse")
                                        // Extract the serialized transaction string
                                        val serializedTransaction = swapTransactionResponse?.swapTransaction
                                        if (serializedTransaction != null) {
                                            // Call sendSwap with the serialized transaction
                                            sendSwap(serializedTransaction)
                                        } else {
                                            Log.e("SwapFunction", "Serialized transaction is null")
                                        }
                                    } else {
                                        Log.e("SwapFunction", "Error fetching swap transaction: ${response.errorBody()?.string()}")
                                    }
                                }

                                override fun onFailure(call: Call<SwapTransactionResponse>, t: Throwable) {
                                    Log.e("SwapFunction", "Swap Transaction API Network Error: ${t.message}", t)
                                }
                            })
                        }

                    } else {
                        // Handle error
                        Log.e("SwapFunction", "Error fetching swap quote: ${response.errorBody()?.string()}")
                    }
                }
                override fun onFailure(call: retrofit2.Call<SwapQuoteResponse>, t: Throwable) {
                    // There was a network error, log the error message
                    Log.e("SwapFunction", "Swap Quote API Network Error: ${t.message}", t)
                }
            })
        }
        private fun openWebPage(url: String) {
            val builder = CustomTabsIntent.Builder()
            val colorSchemeParams = CustomTabColorSchemeParams.Builder()
                .build()

            builder.setDefaultColorSchemeParams(colorSchemeParams)
            val customTabsIntent = builder.build()

            customTabsIntent.launchUrl(this, Uri.parse(url))
        }



        private fun hasValidAuthToken(): Boolean {
            // Retrieve the saved auth token from SharedPreferences or similar storage
            val sharedPreferences = getSharedPreferences("wallet_prefs", Context.MODE_PRIVATE)
            val authToken = sharedPreferences.getString("auth_token", null)
            // Add logic to check if the token is still valid (if possible)
            return authToken != null // and isValid(authToken)
        }
        private fun updateTokenList(context: Context, connectedNetwork:String) {
            Log.d("MainActivity",connectedNetwork)
            val tokenAdapter = if (connectedNetwork == "Solana") {
                TokenAdapter(context, TokenData.tokenList)
            } else {
                TokenAdapter(context, TokenData.tokenList_polygon)
            }
            spinnerToken.adapter = tokenAdapter
        }
        private fun updateUserAddressUI(address: String) {
            val userAddressTextView: TextView = findViewById(R.id.userAddressTextView)
            userAddressTextView.text = Utils.shortenAddress(address)
        }
        private fun storeAuthData(authToken: String, userAddress: String) {
            val sharedPreferences = getSharedPreferences("wallet_prefs", Context.MODE_PRIVATE)
            with(sharedPreferences.edit()) {
                putString("auth_token", authToken)
                putString("user_address", userAddress)
                apply()
            }
        }

        private fun retrieveAuthData(): Pair<String?, String?> {
            val sharedPreferences = getSharedPreferences("wallet_prefs", Context.MODE_PRIVATE)
            val authToken = sharedPreferences.getString("auth_token", null)
            val userAddress = sharedPreferences.getString("user_address", null)
            return Pair(authToken, userAddress)
        }


        private fun connectWallet() {
            // Create an AlertDialog to prompt the user to choose the wallet type
            connectSolanaWallet()
            //TODO: Use walletconnect instead of MetaMask api
            //val walletOptions = arrayOf("Solana Wallet", "MetaMask (ONLY POLYGON)")
            //AlertDialog.Builder(this)
            //    .setTitle("Select Wallet Type")
            //    .setItems(walletOptions) { _, which ->
            //        when (which) {
            //            0 -> connectSolanaWallet()
            //            1 -> showMetaMaskWarningDialog()
            //        }
            //    }
            //    .show()
        }

        private fun showMetaMaskWarningDialog() {
            AlertDialog.Builder(this)
                .setTitle("MetaMask Warning")
                .setMessage("Please note that the MetaMask integration is currently in beta. Make sure you are connected to the Polygon network in MetaMask. Bridge payments can cost up to \$2.")
                .setPositiveButton("Proceed") { _, _ ->
                    connectMetaMask()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        private fun connectSolanaWallet() {
            scope.launch {
                try {
                    val walletAdapterClient = MobileWalletAdapter()
                    val result = walletAdapterClient.transact(activityResultSender) {
                        authorize(
                            identityUri = identityUri,
                            iconUri = iconUri,
                            identityName = identityName
                        )
                    }

                    // Handle the result of Solana wallet connection attempt
                    when (result) {
                        is TransactionResult.Success -> {
                            // Save the values from the successful result
                            userAddress = PublicKey(result.payload.publicKey).toBase58()
                            authToken = result.payload.authToken
                            canTransact = true
                            // After successful wallet connection
                            storeAuthData(authToken, userAddress)
                            // Log the result and update UI on the main thread
                            Log.d("MainActivity", "Connected to Solana Wallet: $userAddress")
                            connectedNetwork = "Solana"
                            updateCardUI(userAddress)
                            runOnUiThread {
                                connectWalletButton.visibility = View.GONE
                                updateButton() // Update the connect button text
                                updateUserAddressUI(userAddress)
                                cardLayout.visibility = View.VISIBLE
                                updateTokenList(this@MainActivity,connectedNetwork)
                            }
                            val navigationView: NavigationView = findViewById(R.id.nav_view)
                            updateMenuItemsVisibility(navigationView)
                        }

                        is TransactionResult.NoWalletFound -> {
                            noWallet = true
                            canTransact = false
                            runOnUiThread {
                                updateButton() // Update the connect button text
                                // Show a message to the user prompting them to install a wallet
                            }
                        }

                        is TransactionResult.Failure -> {
                            canTransact = false
                            runOnUiThread {
                                updateButton() // Update the connect button text
                                // Show an error message to the user
                            }
                        }

                        else -> {
                            // Handle any other cases
                            runOnUiThread {
                                // Update the UI for an unknown error
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error connecting to Solana Wallet: ${e.message}")
                    // Handle exceptions
                    runOnUiThread {
                        // Update the UI to show an error message
                    }
                }
            }
        }

        private fun connectMetaMask() {
            ethereum = Ethereum(context = this)

            val dapp = Dapp("TND Pay", "https://www.tndpayments.com/")

            // This is the same as calling eth_requestAccounts
            ethereum.connect(dapp) { result ->
                if (result is RequestError) {
                    Log.e("MainActivity", "MetaMask connection error: ${result.message}")
                    runOnUiThread {
                        // Update the UI to show an error message
                    }
                } else {
                    Log.d("MainActivity", "MetaMask connection result: $result")
                    // Parse the result to get the user's Ethereum address
                    val ethereumAddress = result.toString()


                    connectedNetwork = "Polygon"
                    updateCardUI(ethereumAddress)

                    // Update the UI to reflect the connected state
                    runOnUiThread {
                        connectWalletButton.visibility = View.GONE
                        updateButton()
                        updateUserAddressUI(ethereumAddress)
                        cardLayout.visibility = View.VISIBLE
                    }
                }
            }
        }
        private suspend fun getBridgingCost():String {
            return withContext(Dispatchers.IO) {
                // Connect to an Ethereum node (like Infura)
                val api =BuildConfig.INFURA
                val web3 = Web3j.build(HttpService("https://polygon-mainnet.infura.io/v3/$api"))
                val destinationChainId = "4"
                val messenger = "1"
                val token = "0x3c499c542cEF5E3811e1192ce70d8cC03d5c3359"
                // Smart contract address
                val contractAddress = "0x7775d63836987f444E2F14AA0fA2602204D7D3E0"

                // Load the smart contract
                val function = Function(
                    "getBridgingCostInTokens",
                    listOf(Uint256(BigInteger(destinationChainId, 16)), Uint8(messenger.toBigInteger()), Address(token)),
                    listOf(object : TypeReference<Uint256>() {})
                )
                val encodedFunction = FunctionEncoder.encode(function)
                // Call the smart contract function
                val ethCall = web3.ethCall(
                    org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(
                        null,
                        contractAddress,
                        encodedFunction
                    ),
                    DefaultBlockParameterName.LATEST
                ).send()

                val result = ethCall.value
                Log.d(TAG, "Raw result: $result")

                return@withContext if (result != null) {
                    Log.d(TAG, "Bridging Cost (hex): $result")
                    result
                } else {
                    Log.e(TAG, "Failed to get bridging cost")
                    "0"
                }
            }
        }
        private suspend fun payWMM(raw_amount: Double, solanaAddress: String)  {
            val from = ethereum.selectedAddress
            val to = "0x7775d63836987f444e2f14aa0fa2602204d7d3e0"
            val methodId = "0x4cd480bd"
            val token = "0x0000000000000000000000003c499c542cef5e3811e1192ce70d8cc03d5c3359"
            val destinationChainId = "0000000000000000000000000000000000000000000000000000000000000004"
            val receiveToken = "c6fa7af3bedbad3a3d65f36aabc97431b1bbe4c2d2f6e0e47ca60203452f5d61"
            val nonce = "0"
            val messenger = "0000000000000000000000000000000000000000000000000000000000000001"

            fun solanaAddressToHex(solanaAddress: String): String {
                val publicKeyBytes = Base58.decode(solanaAddress)
                val lastBytes = publicKeyBytes.takeLast(32)
                return lastBytes.joinToString("") { "%02x".format(it) }
            }


            val amount = BigDecimal(raw_amount).toBigInteger().toString(16).padStart(64, '0')

            //val feeTokenAmount = getBridgingCost()
            val encodedFeeTokenAmount = getBridgingCost() //feeTokenAmount.toString(16).padStart(64, '0')
            Log.e(TAG, "GOT bridging cost $encodedFeeTokenAmount")
            val recipient = solanaAddressToHex(solanaAddress)

            // Encode the function parameters
            val encodedToken = Numeric.toHexString(Numeric.hexStringToByteArray(token))
            val encodedAmount = Numeric.toHexString(BigInteger(amount, 16).toByteArray())
            val encodedRecipient = Numeric.toHexString(Numeric.hexStringToByteArray(recipient))
            val encodedDestinationChainId = Numeric.toHexString(BigInteger(destinationChainId, 16).toByteArray())
            val encodedReceiveToken = Numeric.toHexString(Numeric.hexStringToByteArray(receiveToken))
            val encodedNonce = Numeric.toHexString(BigInteger(nonce, 16).toByteArray())
            val encodedMessenger = Numeric.toHexString(BigInteger(messenger, 16).toByteArray())

            // Concatenate the method ID and encoded parameters
            val data = methodId +
                    encodedToken.substring(2).padStart(64, '0') +
                    encodedAmount.substring(2).padStart(64, '0') +
                    encodedRecipient.substring(2).padStart(64, '0') +
                    encodedDestinationChainId.substring(2).padStart(64, '0') +
                    encodedReceiveToken.substring(2).padStart(64, '0') +
                    encodedNonce.substring(2).padStart(64, '0') +
                    encodedMessenger.substring(2).padStart(64, '0') +
                    encodedFeeTokenAmount.substring(2).padStart(64, '0')

            val params: Map<String, Any> = mapOf(
                "from" to from,
                "to" to to,
                "gas" to "0x7a120", // Adjust the gas limit as needed
                "value" to "0x0", // Set the value to 0 if not sending Ether
                "data" to data
            )

            // Create request
            val transactionRequest = EthereumRequest(
                method = EthereumMethod.ETH_SEND_TRANSACTION.value,
                params = listOf(params)
            )

            ethereum.sendRequest(transactionRequest) { result ->
                if (result is RequestError) {
                    // handle error
                } else {
                    Log.d(TAG, "Ethereum transaction result: $result")
                    openWebPage("https://polygonscan.com/tx/$result")
                }
            }
        }
        companion object {
            private const val TAG = "MainActivity"
        }
        private fun updateCardUI(address: String) {
            val cardLayout = findViewById<View>(R.id.cardLayout)
            val cardBackground = cardLayout.findViewById<LinearLayout>(R.id.card_background)
            val chainLogoImageView = cardLayout.findViewById<ImageView>(R.id.chainLogoImageView)
            val metamaskImageView = cardLayout.findViewById<ImageView>(R.id.metamaskImageView)
            val address_card =  cardLayout.findViewById<TextView>(R.id.addressTextView)
            when (connectedNetwork) {
                "Solana" -> {
                    cardBackground.setBackgroundResource(R.drawable.card_background)
                    chainLogoImageView.setImageResource(R.drawable.token1_logo)
                    metamaskImageView.visibility = View.GONE
                    address_card.text = Utils.shortenAddress(address)

                }
                "Polygon" -> {
                    cardBackground.setBackgroundResource(R.drawable.card_background_polygon)
                    chainLogoImageView.setImageResource(R.drawable.polygon)
                    metamaskImageView.visibility = View.VISIBLE
                    address_card.text = Utils.shortenAddress(address)

                }
                "ETH" -> {
                    cardBackground.setBackgroundResource(R.drawable.card_background_eth)
                    chainLogoImageView.setImageResource(R.drawable.eth)
                    metamaskImageView.visibility = View.VISIBLE
                    address_card.text = Utils.shortenAddress(address)

                }
                "XMR" -> {
                    cardBackground.setBackgroundResource(R.drawable.card_background_xmr)
                    chainLogoImageView.setImageResource(R.drawable.xmr_logo)
                    address_card.text = Utils.shortenAddress(address)

                }
            }
        }
        private fun disconnectWallet() {

            val sharedPreferences = getSharedPreferences("wallet_prefs", Context.MODE_PRIVATE)
            with(sharedPreferences.edit()) {
                remove("auth_token")
                remove("user_address")
                apply()
            }

            // Update the UI and variables to reflect the disconnected state
            authToken = ""
            userAddress = ""
            noWallet = true
            canTransact = false
            runOnUiThread {
                PayButton.visibility = View.GONE
                declineButton.visibility = View.GONE
                spinnerToken.visibility = View.GONE
                findViewById<TextView>(R.id.textViewTokenName).text = ""
                findViewById<TextView>(R.id.userAddressTextView).text = ""
                connectWalletButton.visibility = View.VISIBLE
                cardLayout.visibility = View.GONE

            }
            updateButton()
            val navigationView: NavigationView = findViewById(R.id.nav_view)
            updateMenuItemsVisibility(navigationView)
            navigationView.menu.clear() // Clear existing menu
            navigationView.inflateMenu(R.menu.drawer_menu) // Re-inflate the menu
            updateMenuItemsVisibility(navigationView)
        }
        private fun decline(){
            runOnUiThread {
                // Hide the Pay button
                PayButton.visibility = View.GONE
                declineButton.visibility = View.GONE
                spinnerToken.visibility = View.GONE
                findViewById<TextView>(R.id.textViewTokenName).text = ""

            }
            updateButton()
        }
        private fun updateButton() {
            val buttonText = when {
                noWallet -> "Please connect a wallet"
                userAddress.isEmpty() -> "Connect Wallet"
                userAddress.isNotEmpty() -> userAddress.take(4) + "..." + userAddress.takeLast(4)
                else -> ""
            }
            connectWalletButton.text = buttonText
        }

        override fun onResume() {
            super.onResume()
            nfcAdapter?.enableReaderMode(this, this,
                NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                null)
            updateBackgroundBasedOnNFCState()
        }

        override fun onPause() {
            super.onPause()
            nfcAdapter?.disableReaderMode(this)
        }

        override fun onTagDiscovered(tag: Tag?) {
            tag?.let {
                val isoDep = IsoDep.get(it)
                val tokens = TokenData.tokenList
                isoDep.connect()
                val response = isoDep.transceive(Utils.hexStringToByteArray("00A4040007A0000002471001"))
                val responseString = String(response, Charsets.UTF_8)
                // Split the responseString into address, amount, and possibly token ID
                val paymentInfo = responseString.split("\n")
                if (paymentInfo.size >= 3) {
                    paymentAddress = paymentInfo[0]
                    paymentAmount = paymentInfo[1].toDoubleOrNull()
                    tokenId = paymentInfo[2].toIntOrNull()
                    val token = tokens.find { it.id == tokenId }
                    Log.d("MainActivity", "HCE message: $responseString")
                    runOnUiThread {
                        // Set the text for payment address and amount
                        val paymentRequestText = "Payment request:\n$paymentAddress\nAmount: $paymentAmount"
                        findViewById<TextView>(R.id.textViewTokenName).text = paymentRequestText

                        // Only show the pay button if we have valid payment info
                        checkAddressForFlag(this,paymentInfo[0])

                        PayButton.visibility = if (paymentAddress != null && paymentAmount != null) View.VISIBLE else View.GONE
                        declineButton.visibility = View.VISIBLE
                        spinnerToken.visibility = View.VISIBLE
                    }
                } else {
                    runOnUiThread {
                        findViewById<TextView>(R.id.textViewTokenName).text = ""
                        PayButton.visibility = View.GONE
                        declineButton.visibility = View.GONE
                        spinnerToken.visibility = View.GONE
                    }
                }
                isoDep.close()
            }
        }

        private fun buildSendSPLTransaction(
            recipientAddress: String,
            amount: Double,
            senderAccount:  String,
            id: Int
        ) {
            scope.launch {
                withContext(Dispatchers.IO) {
                    val token = TokenData.tokenList.find { it.id == id }
                    if (token == null) {
                        Log.e("MainActivity", "Token with ID $id not found.")
                        return@withContext // Exit the function if token is not found
                    }
                    val mintAddress = token.mintAddress
                    val splMintAddress = PublicKey(mintAddress)
                    val splDecimals = token.decimals
                    val amountInSmallestUnit = (amount * 10.0.pow(splDecimals)).toLong()

                    val fromPublicKey = PublicKey(senderAccount)
                    val toPublicKey = PublicKey(recipientAddress)

                    val rpcService = RpcService()
                    val blockhashResult = rpcService.getLatestBlockhash()
                    val blockhash: String = blockhashResult?.result?.value?.blockhash ?: throw IllegalStateException("Blockhash could not be retrieved")

                    // Calculate the associated token account address
                    val associatedTokenAddress = findAssociatedTokenAddress(toPublicKey, splMintAddress)
                    val lol=associatedTokenAddress
                    Log.d("MainActivity", "The associated tokenaddress is $lol")
                    // Check if the associated token account exists
                    val token_init = rpcService.getTokenAccountsByOwner(toPublicKey.toString(),mintAddress)

                    Log.d("MainActivity", "Associated Token Account Info: $token_init")

                    val transaction = Transaction()
                    transaction.feePayer = fromPublicKey
                    transaction.recentBlockhash = blockhash
                    // Create the associated token account if it doesn't exist
                    if (!token_init) {
                        val createATokenInstruction = AssociatedTokenProgram.createAssociatedTokenAccountInstruction(
                            mint = splMintAddress,
                            associatedAccount = associatedTokenAddress,
                            owner = toPublicKey,
                            payer = fromPublicKey
                        )
                        transaction.add(createATokenInstruction)
                    }

                    // Add the SPL Token transfer instruction
                    transaction.add(
                        TokenProgram.transfer(
                            source = findAssociatedTokenAddress(fromPublicKey, splMintAddress),
                            destination = associatedTokenAddress,
                            owner = fromPublicKey,
                            amount = amountInSmallestUnit
                        )
                    )
                    val bytes = transaction.serialize(SerializeConfig(requireAllSignatures = false))

                    val walletAdapterClient = MobileWalletAdapter()
                    val result = walletAdapterClient.transact(activityResultSender) {
                        reauthorize(identityUri, iconUri, identityName, authToken)
                        signAndSendTransactions(arrayOf(bytes))
                    }

                    // Check and handle the transaction result
                    withContext(Dispatchers.Main) {
                        when (result) {
                            is TransactionResult.Success -> {
                                val signatures = result.payload.signatures
                                Log.d("MainActivity",signatures.toString())
                                if (signatures.isNotEmpty()) {
                                    // Log or handle each signature
                                    signatures.forEach { signature ->
                                        val readableSignature = Base58.encode(signature)
                                        Log.d("MainActivity", "Transaction successful. Signature: $readableSignature")
                                        runOnUiThread {
                                            AlertDialog.Builder(this@MainActivity)
                                                .setTitle("Transaction Successful")
                                                .setMessage("Do you want to view the transaction on Solana Explorer?")
                                                .setPositiveButton("Yes") { dialog, id ->
                                                    openWebPage("https://solana.fm/tx/$readableSignature?cluster=mainnet-alpha")
                                                }
                                                .setNegativeButton("No") { dialog, id ->
                                                    dialog.dismiss()
                                                }
                                                .create()
                                                .show()
                                        }
                                        decline()
                                    }
                                } else {
                                    Log.e("MainActivity", "Transaction successful but no signatures received.")
                                    toast("Transaction successful but no signatures received.")
                                }
                            }
                            is TransactionResult.Failure -> {
                                Log.e("MainActivity", "Transaction failed: ${result}")
                                toast("Transaction failed")
                            }
                            is TransactionResult.NoWalletFound -> {
                                Log.e("MainActivity", "No wallet application found.")
                                toast("No wallet application found.")
                            }
                            else -> {
                                Log.e("MainActivity", "An unknown error occurred.")
                            }
                        }
                    }
                }
                }
        }
        private fun sendSol(recipientAddress: String, amount: Double, senderAccount: String) {
            scope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        val rpcService = RpcService()
                        val blockhashResult = rpcService.getLatestBlockhash()
                        val blockhash: String = blockhashResult?.result?.value?.blockhash ?: throw IllegalStateException("Blockhash could not be retrieved")

                        // Create a transfer transaction
                        val transaction = Transaction()
                        transaction.add(
                            SystemProgram.transfer(
                                PublicKey(userAddress), // Sender's public key
                                PublicKey(recipientAddress), // Recipient's public key
                                (amount * 1_000_000_000).toLong()
                            )
                        )
                        transaction.recentBlockhash = blockhash
                        transaction.feePayer =PublicKey(senderAccount)

                        // Sign and send the transaction using the wallet adapter
                        val walletAdapterClient = MobileWalletAdapter()
                        val result = walletAdapterClient.transact(activityResultSender) {
                            reauthorize(identityUri, iconUri, identityName, authToken)
                            val sig =  signAndSendTransactions(arrayOf(transaction.serialize(SerializeConfig(requireAllSignatures = false)))).signatures.firstOrNull()
                            val txid=Base58.encode(sig)
                            Log.e("MainActivity",txid)
                            runOnUiThread {
                                AlertDialog.Builder(this@MainActivity)
                                    .setTitle("Transaction Successful")
                                    .setMessage("Do you want to view the transaction on Solana Explorer?")
                                    .setPositiveButton("Yes") { dialog, id ->
                                        // User clicked Yes button
                                        val url = "https://solana.fm/tx/$txid?cluster=mainnet-alpha"
                                        openWebPage(url)
                                    }
                                    .setNegativeButton("No") { dialog, id ->
                                        // User clicked No button or dismissed the dialog
                                        dialog.dismiss()
                                    }
                                    .create()
                                    .show()
                            }
                            decline()
                        }
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error sending SOL: ${e.message}")
                        toast("ERROR")
                    }
                }
            }
        }
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