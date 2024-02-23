package com.example.hmint.injection


import com.solana.mobilewalletadapter.clientlib.RpcCluster
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class RpcConfig @Inject constructor() : IRpcConfig {
    override val solanaRpcUrl: String = "https://api.mainnet.solana.com"


    override val rpcCluster: RpcCluster = RpcCluster.MainnetBeta
}
