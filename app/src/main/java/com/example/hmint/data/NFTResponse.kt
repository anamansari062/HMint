package com.example.hmint.data

data class NftMintResponse(
    val jsonrpc: String,
    val id: String,
    val result: MintResult
)

data class MintResult(
    val signature: String,
    val minted: Boolean,
    val assetId: String
)