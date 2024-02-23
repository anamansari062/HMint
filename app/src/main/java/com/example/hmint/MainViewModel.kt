package com.example.hmint

import android.content.ContentValues.TAG
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hmint.data.NftMintResponse
import com.example.hmint.usecase.Connected
import com.example.hmint.usecase.NotConnected
import com.example.hmint.usecase.WalletConnectionUseCase
import com.google.gson.Gson
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.solana.mobilewalletadapter.clientlib.MobileWalletAdapter
import com.solana.mobilewalletadapter.clientlib.RpcCluster
import com.solana.mobilewalletadapter.clientlib.TransactionResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.net.URL
import javax.inject.Inject

data class WalletViewState(
    val isLoading: Boolean = false,
    val canTransact: Boolean = false,
    val solBalance: Double = 0.0,
    val userAddress: String = "",
    val userLabel: String = "",
    val noWallet: Boolean = false,
    val mintResponse: String = ""
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val walletAdapter: MobileWalletAdapter,
    private val walletConnectionUseCase: WalletConnectionUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(WalletViewState())

    val viewState: StateFlow<WalletViewState>
        get() = _state


    init {
        viewModelScope.launch {
            walletConnectionUseCase.walletDetails
                .collect { walletDetails ->
                    val detailState = when (walletDetails) {
                        is Connected -> {
                            WalletViewState(
                                isLoading = false,
                                canTransact = true,
                                solBalance = 0.0,
                                userAddress = walletDetails.publicKey,
                                userLabel = walletDetails.accountLabel
                            )
                        }

                        is NotConnected -> WalletViewState()
                    }

                    _state.value = detailState
                }

        }
    }

    fun mintCNft(imageUri: Uri, nftName: String, user: String) =
        viewModelScope.launch {
            _state.update {
                _state.value.copy(
                    isLoading = true,
                )
            }

            withContext(viewModelScope.coroutineContext + Dispatchers.IO) {
                val apiKey = System.getProperty("api_key") ?: ""
                val url = URL("https://mainnet.helius-rpc.com/?api-key=$apiKey")
                val mediaType = "application/json".toMediaTypeOrNull()
                val requestBody = """
                                    {
                                        "jsonrpc": "2.0",
                                        "id": "helius-test",
                                        "method": "mintCompressedNft",
                                        "params": {
                                                    "name": "$nftName",
                                                    "symbol": "HM",
                                                    "owner": "$user",
                                                    "description": "This is a test cNFT minted by hMint app",
                                                    "attributes": [
                                                        {
                                                            "trait_type": "hMint",
                                                            "value": "1"
                                                        }
                                                    ],
                                                    "imageUrl": "$imageUri",
                                                    "sellerFeeBasisPoints": 6900
                                                   }
                                    }
                                    """.trimIndent()

                val body = requestBody.toRequestBody(mediaType)
                val request = Request.Builder()
                    .url(url)
                    .post(body)
                    .addHeader("accept", "application/json")
                    .addHeader("content-type", "application/json")
                    .build()
                val client = OkHttpClient()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        // Handle failure
                        e.printStackTrace()
                        Log.d(TAG, "NFT Mint Failed")
                        _state.update {
                            _state.value.copy(
                                isLoading = false,
                                mintResponse = "Mint Failed"
                            )
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful) {
                            val responseBody = response.body?.string() ?: ""
                            val gson = Gson()
                            val nft = gson.fromJson(responseBody, NftMintResponse::class.java)
                            Log.d(TAG, "NFT Mint Successful: $nft")
                            _state.update {
                                _state.value.copy(
                                    isLoading = false,
                                    mintResponse = "Minted Successfully"
                                )
                            }

                        } else {
                            // Handle non-successful response
                            Log.d(TAG, "NFT Mint Failed: ${response.code}")
                            _state.update {
                                _state.value.copy(
                                    isLoading = false,
                                    mintResponse = "Minted Failed"
                                )
                            }
                        }
                    }
                })


            }
        }


    fun connect(
        identityUri: Uri,
        iconUri: Uri,
        identityName: String,
        activityResultSender: ActivityResultSender
    ) {
        viewModelScope.launch {
            val result = walletAdapter.transact(activityResultSender) {
                authorize(
                    identityUri = identityUri,
                    iconUri = iconUri,
                    identityName = identityName,
                    rpcCluster = RpcCluster.MainnetBeta
                )
            }

            when (result) {
                is TransactionResult.Success -> {
                    walletConnectionUseCase.persistConnection(
                        result.payload.publicKey,
                        result.payload.accountLabel ?: "",
                        result.payload.authToken
                    )
                }

                is TransactionResult.NoWalletFound -> {
                    _state.update {
                        _state.value.copy(
                            noWallet = true,
                            canTransact = true
                        )
                    }
                }

                is TransactionResult.Failure -> {

                }
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            walletConnectionUseCase.clearConnection()
        }
    }
}

