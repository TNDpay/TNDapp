package com.example.tnd

data class SwapInstructionsResponse(
    val tokenLedgerInstruction: Instruction?,
    val computeBudgetInstructions: List<Instruction>,
    val setupInstructions: List<Instruction>,
    val swapInstruction: Instruction,
    val cleanupInstruction: Instruction?,
    val addressLookupTableAddresses: List<String>,
    val prioritizationFeeLamports: Long
)

data class Instruction(
    val programId: String,
    val accounts: List<Account>,
    val data: String
)

data class Account(
    val pubkey: String,
    val isSigner: Boolean,
    val isWritable: Boolean
)
