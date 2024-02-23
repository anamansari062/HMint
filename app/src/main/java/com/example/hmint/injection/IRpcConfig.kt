package com.example.hmint.injection


import com.solana.mobilewalletadapter.clientlib.RpcCluster


/**
 * RPC config interface
 */
interface IRpcConfig {
    val solanaRpcUrl: String
    val rpcCluster: RpcCluster
}
