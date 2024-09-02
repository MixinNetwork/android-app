package one.mixin.android.web3

import one.mixin.android.tip.wc.internal.Chain

@Suppress("ConstPropertyName")
object Web3ChainId {
    // bip44
    const val BtcChainId = 0
    const val SolanaChainId = 501
    const val MixinChainId = 2365

    // eip155
    const val EthChainId = 1
    const val OptimismChainId = 10
    const val BscChainId = 56
    const val PolygonChainId = 137
    const val BaseChainId = 8453
    const val ArbitrumChainId = 42161
    const val AvalancheChainId = 43114
    const val BlastChainId = 81457

    val eip155ChainIds = listOf(EthChainId, OptimismChainId, BscChainId, PolygonChainId, BaseChainId, ArbitrumChainId, AvalancheChainId, BlastChainId)

    fun getChainType(id: Int): ChainType =
        when {
            eip155ChainIds.contains(id) -> ChainType.ethereum
            SolanaChainId == id -> ChainType.solana
            else -> ChainType.solana
        }

    fun getChain(id: Int): Chain =
        when (id) {
            EthChainId -> Chain.Ethereum
            OptimismChainId -> Chain.Optimism
            BscChainId -> Chain.BinanceSmartChain
            PolygonChainId -> Chain.Polygon
            BaseChainId -> Chain.Base
            ArbitrumChainId -> Chain.Arbitrum
            AvalancheChainId -> Chain.Avalanche
            BlastChainId -> Chain.Blast
            else -> Chain.Solana
        }
}
