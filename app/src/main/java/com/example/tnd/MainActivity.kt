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
    import android.widget.Toast
    import android.app.AlertDialog
    import android.icu.math.BigDecimal
    import io.metamask.androidsdk.Ethereum
    import io.metamask.androidsdk.Dapp
    import io.metamask.androidsdk.RequestError
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
    import com.solana.mobilewalletadapter.clientlib.ConnectionIdentity
    import com.solana.mobilewalletadapter.clientlib.Solana
    import androidx.cardview.widget.CardView
    import com.example.tnd.SetPreferencesActivity.Preferences

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
        private lateinit var chain:String
        private lateinit var iconUri: Uri
        private lateinit var identityName: String
        private lateinit var authToken: String
        private lateinit var drawerLayout: DrawerLayout
        private lateinit var addressTextView: TextView
        private lateinit var cardLayout: View
        private var connectedNetwork: String = ""
        private lateinit var ethereum: Ethereum
        private lateinit var walletAdapter: MobileWalletAdapter
        private lateinit var connectionIdentity: ConnectionIdentity
        private lateinit var userCurrency: Currency


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


            // Initialize ConnectionIdentity
            connectionIdentity = ConnectionIdentity(identityUri,iconUri,identityName)
            walletAdapter = MobileWalletAdapter(connectionIdentity)

            if (hasValidAuthToken()) {
                connectWalletButton.visibility = View.GONE
                cardLayout.visibility = View.VISIBLE
            } else {
                connectWalletButton.visibility = View.VISIBLE
                cardLayout.visibility = View.GONE
            }
            val (retrievedAuthToken, retrievedUserAddress,retrievedNetwork) = retrieveAuthData()
            if (retrievedAuthToken != null && retrievedUserAddress != null) {
                this.authToken = retrievedAuthToken
                this.userAddress = retrievedUserAddress
                this.connectedNetwork = retrievedNetwork ?: ""
                canTransact = true
                updateUserAddressUI(retrievedUserAddress)
                UIUtils.updateCardUI(this@MainActivity, userAddress, connectedNetwork)

            } else {
                connectWalletButton.visibility = View.VISIBLE
                connectWalletButton.setOnClickListener {
                    connectWallet()
                }
            }
            // Load user's preferred currency
            val currencyCode = SetPreferencesActivity.Preferences.getDefaultBaseCurrency(this)
            userCurrency = currencies.find { it.code == currencyCode } ?: currencies.first()

            connectWalletButton.setOnClickListener {
                connectWallet()
            }
            val chargeButton: Button = findViewById(R.id.ChargeButton)
            chargeButton.setOnClickListener {
                // Start InvoiceActivity when the charge button is clicked
                val intent = Intent(this@MainActivity, InvoiceActivity::class.java)
                intent.putExtra("USER_ADDRESS", userAddress)
                intent.putExtra("CONNECTED_NETWORK", connectedNetwork)
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
                                            Log.e("MainActivity", "Calling payment function rememebr we have ${this.authToken}")
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
                                                val tokenIn = TokenData.tokenList_sol.find { it.id == idSpinner }
                                                val tokenOut = TokenData.tokenList_sol.find { it.id == id }
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
                                    else -> {
                                        Log.e("MainActivity", "ERROR because connected network value rn is ${connectedNetwork}")
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
                    R.id.nav_options -> {
                        val intent = Intent(this, SetPreferencesActivity::class.java)
                        startActivity(intent)
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
                        Utils.openWebPage(this,"https://twitter.com/TNDpayments")
                        drawerLayout.closeDrawer(GravityCompat.START)
                        true
                    }
                    R.id.nav_telegram -> {
                        // Handle Twitter option by opening the Twitter URL
                        Utils.openWebPage(this,"https://t.me/tndpayments")
                        drawerLayout.closeDrawer(GravityCompat.START)
                        true
                    }
                    R.id.nav_website -> {
                        // Handle Website option by opening the website URL
                        Utils.openWebPage(this,"https://www.tndpayments.com/")
                        drawerLayout.closeDrawer(GravityCompat.START)
                        true
                    }
                    R.id.nav_explore_form ->{
                        Utils.openWebPage(this,"https://github.com/TNDpay/vendors/blob/main/user_license.MD")
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

            val tokenAdapter = if (connectedNetwork == "Solana") {
                TokenAdapter(this, TokenData.tokenList_sol)
            } else if (connectedNetwork == "XMR") {
                TokenAdapter(this, TokenData.tokenList_xmr)
            } else {
                // Handle other cases or provide a default
                TokenAdapter(this, TokenData.tokenList_sol)
            }
            spinnerToken.adapter = tokenAdapter
            // Set the default token for payments
            val defaultPaymentTokenId = Preferences.getDefaultPaymentTokenId(this)
            val defaultPaymentToken = TokenData.tokenList_sol.find { it.id == defaultPaymentTokenId }
            defaultPaymentToken?.let {
                spinnerToken.setSelection(tokenAdapter.getPosition(it))
            }

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
                        walletAdapter.blockchain = Solana.Mainnet
                        val result = walletAdapter.transact(activityResultSender) {

                            // Ensure we're authorized first
                            //reauthorize(identityUri, iconUri, identityName, authToken)
                            // Then sign and send the transaction
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
                                                    Utils.openWebPage(this@MainActivity,"https://solana.fm/tx/$readableSignature?cluster=mainnet-alpha")
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
                        val publicAddresReceiver=PublicKey(receiverAddr)
                        val publicMintRT=PublicKey(output)
                        destToken=SolanaUtils.findAssociatedTokenAddress(publicAddresReceiver, publicMintRT).toString()
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
                TokenAdapter(context, TokenData.tokenList_sol)
            } else {
                TokenAdapter(context, TokenData.tokenList_polygon)
            }
            spinnerToken.adapter = tokenAdapter
        }
        private fun updateUserAddressUI(address: String) {
            val userAddressTextView: TextView = findViewById(R.id.userAddressTextView)
            userAddressTextView.text = Utils.shortenAddress(address)
        }
        private fun storeAuthData(authToken: String, userAddress: String, network: String) {
            val sharedPreferences = getSharedPreferences("wallet_prefs", Context.MODE_PRIVATE)
            with(sharedPreferences.edit()) {
                putString("auth_token", authToken)
                putString("user_address", userAddress)
                putString("connected_network", network)
                apply()
            }
        }

        private fun retrieveAuthData(): Triple<String?, String?, String?> {
            val sharedPreferences = getSharedPreferences("wallet_prefs", Context.MODE_PRIVATE)
            val authToken = sharedPreferences.getString("auth_token", null)
            val userAddress = sharedPreferences.getString("user_address", null)
            val connectedNetwork = sharedPreferences.getString("connected_network", null)
            return Triple(authToken, userAddress,connectedNetwork)
        }


        private fun connectWallet() {
            // Create an AlertDialog to prompt the user to choose the wallet type
            connectSolanaWallet2()
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
        private fun connectSolanaWallet2() {
            scope.launch {
                try {
                    walletAdapter.blockchain = Solana.Mainnet
                    val result = walletAdapter.connect(activityResultSender)

                    when (result) {
                        is TransactionResult.Success -> {
                            val authResult = result.authResult
                            Log.e("MainActivity", "auth result IS THIS : $authResult")

                            userAddress = authResult.accounts.firstOrNull()?.publicKey?.let {
                                Base58.encode(it)
                            } ?: run {
                                Log.e("MainActivity", "No account returned from wallet")
                                return@launch
                            }

                            authToken = authResult.authToken
                            canTransact = true

                            Log.d("MainActivity", "Connected to Solana Wallet: $userAddress")
                            connectedNetwork = "Solana"

                            storeAuthData(authToken, userAddress, connectedNetwork)

                            withContext(Dispatchers.Main) {
                                UIUtils.updateCardUI(this@MainActivity, userAddress, connectedNetwork)
                                connectWalletButton.visibility = View.GONE
                                updateButton()
                                updateUserAddressUI(userAddress)
                                cardLayout.visibility = View.VISIBLE
                                updateTokenList(this@MainActivity, connectedNetwork)
                            }

                            val navigationView: NavigationView = findViewById(R.id.nav_view)
                            updateMenuItemsVisibility(navigationView)
                        }
                        is TransactionResult.NoWalletFound -> {
                            Log.e("MainActivity", "No wallet found")
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@MainActivity, "No wallet found", Toast.LENGTH_SHORT).show()
                            }
                        }
                        is TransactionResult.Failure -> {
                            Log.e("MainActivity", "Connection failed: ${result}")
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@MainActivity, "Connection failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error connecting to Solana Wallet: ${e.message}")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Error connecting to wallet", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        private fun connectSolanaWallet() {
            scope.launch {
                try {
                    chain="solana:mainnet"
                    walletAdapter.blockchain = Solana.Mainnet
                    val result = walletAdapter.transact(activityResultSender) {
                        authorize(identityUri, iconUri, identityName, chain )
                    }
                    Log.e("Mainactivity","result is ${result}")
                    when (result) {
                        is TransactionResult.Success -> {
                            val authResult = result.payload
                            userAddress = authResult.accounts.firstOrNull()?.publicKey?.let {
                                Base58.encode(it)
                            } ?: run {
                                Log.e("MainActivity", "No account returned from wallet")
                                return@launch
                            }
                            authToken = authResult.authToken
                            canTransact = true

                            Log.d("MainActivity", "Connected to Solana Wallet: $userAddress")
                            connectedNetwork = "Solana"

                            storeAuthData(authToken, userAddress, connectedNetwork)

                            withContext(Dispatchers.Main) {
                                UIUtils.updateCardUI(this@MainActivity, userAddress, connectedNetwork)
                                connectWalletButton.visibility = View.GONE
                                updateButton()
                                updateUserAddressUI(userAddress)
                                cardLayout.visibility = View.VISIBLE
                                updateTokenList(this@MainActivity, connectedNetwork)
                            }

                            val navigationView: NavigationView = findViewById(R.id.nav_view)
                            updateMenuItemsVisibility(navigationView)
                        }
                        is TransactionResult.NoWalletFound -> {
                            // Handle no wallet found case
                        }
                        is TransactionResult.Failure -> {
                            // Handle failure case
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error connecting to Solana Wallet: ${e.message}")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Error connecting to wallet", Toast.LENGTH_SHORT).show()
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
                    UIUtils.updateCardUI(this, ethereumAddress, connectedNetwork)

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

        companion object {
            private const val TAG = "MainActivity"
        }

        private fun disconnectWallet() {
            scope.launch {
                try {
                    val result = walletAdapter.disconnect(activityResultSender)

                    when (result) {
                        is TransactionResult.Success -> {
                            Log.d("MainActivity", "Successfully disconnected wallet")

                            // Clear shared preferences
                            val sharedPreferences = getSharedPreferences("wallet_prefs", Context.MODE_PRIVATE)
                            with(sharedPreferences.edit()) {
                                remove("auth_token")
                                remove("user_address")
                                remove("connectedNetwork")
                                apply()
                            }

                            // Update the UI and variables to reflect the disconnected state
                            authToken = ""
                            userAddress = ""
                            noWallet = true
                            canTransact = false

                            withContext(Dispatchers.Main) {
                                PayButton.visibility = View.GONE
                                declineButton.visibility = View.GONE
                                spinnerToken.visibility = View.GONE
                                findViewById<CardView>(R.id.paymentInfoCard).visibility = View.GONE
                                findViewById<TextView>(R.id.userAddressTextView).text = ""
                                connectWalletButton.visibility = View.VISIBLE
                                cardLayout.visibility = View.GONE

                                updateButton()
                                val navigationView: NavigationView = findViewById(R.id.nav_view)
                                updateMenuItemsVisibility(navigationView)
                                navigationView.menu.clear() // Clear existing menu
                                navigationView.inflateMenu(R.menu.drawer_menu) // Re-inflate the menu
                                updateMenuItemsVisibility(navigationView)
                            }
                        }
                        is TransactionResult.Failure -> {
                            Log.e("MainActivity", "Failed to disconnect wallet: ${result}")
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@MainActivity, "Failed to disconnect wallet", Toast.LENGTH_SHORT).show()
                            }
                        }
                        is TransactionResult.NoWalletFound -> {
                            Log.e("MainActivity", "No wallet found to disconnect")
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@MainActivity, "No wallet found to disconnect", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error disconnecting wallet: ${e.message}")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Error disconnecting wallet", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        private fun decline(){
            runOnUiThread {
                // Hide the Pay button
                PayButton.visibility = View.GONE
                declineButton.visibility = View.GONE
                spinnerToken.visibility = View.GONE
                findViewById<CardView>(R.id.paymentInfoCard).visibility = View.GONE

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
            val backgroundImageView = findViewById<ImageView>(R.id.backgroundImageView)
            UIUtils.updateBackgroundBasedOnNFCState(backgroundImageView)

            //For preferences:
            val currencyCode = SetPreferencesActivity.Preferences.getDefaultBaseCurrency(this)
            userCurrency = currencies.find { it.code == currencyCode } ?: currencies.first()

            // Reload default payment token
            val defaultPaymentTokenId = SetPreferencesActivity.Preferences.getDefaultPaymentTokenId(this)
            val tokenAdapter = spinnerToken.adapter as? TokenAdapter
            val defaultPaymentToken = TokenData.tokenList_sol.find { it.id == defaultPaymentTokenId }
            defaultPaymentToken?.let {
                tokenAdapter?.let { adapter ->
                    spinnerToken.setSelection(adapter.getPosition(it))
                }
            }
        }

        override fun onPause() {
            super.onPause()
            nfcAdapter?.disableReaderMode(this)
        }
        private fun updatePaymentInfo(paymentAddress: String, paymentAmount: Double, tokenName: String) {
            val tokenItem = TokenData.tokenList_general.find { it.name == tokenName }

            if (tokenItem != null) {
                SolanaUtils.getTokenPriceInDollars(tokenItem) { priceInUSD ->
                    if (priceInUSD != null) {
                        val calculateFiatValue = { convertedPrice: Double ->
                            val fiatValue = convertedPrice * paymentAmount
                            runOnUiThread {
                                findViewById<TextView>(R.id.paymentAddressTextView).text = "to: $paymentAddress"
                                findViewById<TextView>(R.id.paymentAmountTextView).text =
                                    "Amount: $paymentAmount $tokenName (${userCurrency.symbol}%.2f ${userCurrency.code})".format(fiatValue)

                                // Show the payment info card
                                findViewById<CardView>(R.id.paymentInfoCard).visibility = View.VISIBLE

                                // Show/hide other buttons as needed
                                PayButton.visibility = View.VISIBLE
                                declineButton.visibility = View.VISIBLE
                                spinnerToken.visibility = View.VISIBLE
                            }
                        }

                        if (userCurrency.code == "USD") {
                            // If user currency is USD, no conversion needed
                            calculateFiatValue(priceInUSD)
                        } else {
                            // Only convert if the user's currency is not USD
                            Utils.convertUSDToCurrency(priceInUSD, userCurrency.code) { convertedPrice ->
                                if (convertedPrice != null) {
                                    calculateFiatValue(convertedPrice)
                                } else {
                                    runOnUiThread {
                                        findViewById<TextView>(R.id.paymentAmountTextView).text =
                                            "Amount: $paymentAmount $tokenName (Fiat value unavailable)"
                                        // Show UI elements as needed
                                    }
                                }
                            }
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this, "Failed to get token price", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                runOnUiThread {
                    Toast.makeText(this, "Token not found", Toast.LENGTH_SHORT).show()
                }
            }
        }
        override fun onTagDiscovered(tag: Tag?) {
            tag?.let {
                val isoDep = IsoDep.get(it)
                isoDep.connect()
                val response = isoDep.transceive(Utils.hexStringToByteArray("00A4040007A0000002471001"))
                val responseString = String(response, Charsets.UTF_8)
                // Split the responseString into address, amount, and possibly token ID
                val paymentInfo = responseString.split("\n")
                if (paymentInfo.size >= 3) {
                    paymentAddress = paymentInfo[0]
                    paymentAmount = paymentInfo[1].toDoubleOrNull()
                    tokenId = paymentInfo[2].toIntOrNull()
                    val tokenName = TokenData.tokenList_sol.find { it.id == tokenId }?.name

                    Log.d("MainActivity", "HCE message: $responseString")
                    runOnUiThread {
                        // Call updatePaymentInfo only if all required values are non-null
                        if (paymentAddress != null && paymentAmount != null && tokenName != null) {
                            updatePaymentInfo(paymentAddress!!, paymentAmount!!, tokenName)
                        } else {
                            Log.e("MainActivity", "Some payment info is null: address=$paymentAddress, amount=$paymentAmount, token=$tokenName")
                        }

                        PayButton.visibility = if (paymentAddress != null && paymentAmount != null) View.VISIBLE else View.GONE
                        declineButton.visibility = View.VISIBLE
                        spinnerToken.visibility = View.VISIBLE
                    }
                } else {
                    runOnUiThread {
                        findViewById<CardView>(R.id.paymentInfoCard).visibility = View.GONE
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
                    val token = TokenData.tokenList_sol.find { it.id == id }
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
                    val associatedTokenAddress = SolanaUtils.findAssociatedTokenAddress(toPublicKey, splMintAddress)
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
                            source = SolanaUtils.findAssociatedTokenAddress(fromPublicKey, splMintAddress),
                            destination = associatedTokenAddress,
                            owner = fromPublicKey,
                            amount = amountInSmallestUnit
                        )
                    )
                    val bytes = transaction.serialize(SerializeConfig(requireAllSignatures = false))
                    walletAdapter.blockchain = Solana.Mainnet

                    val result = walletAdapter.transact(activityResultSender) {
                        // Ensure we're authorized first
                        //reauthorize(identityUri, iconUri, identityName, authToken)
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
                                                    Utils.openWebPage(this@MainActivity,"https://solana.fm/tx/$readableSignature?cluster=mainnet-alpha")
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
                        walletAdapter.blockchain = Solana.Mainnet
                        // Sign and send the transaction using the wallet adapter
                        val result = walletAdapter.transact(activityResultSender) {
                            // Ensure we're authorized first
                            //reauthorize(identityUri, iconUri, identityName, authToken)
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
                                        Utils.openWebPage(this@MainActivity,url)
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