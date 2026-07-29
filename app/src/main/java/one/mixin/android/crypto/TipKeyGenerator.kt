package one.mixin.android.crypto

import one.mixin.android.Constants

internal data class TipDerivedKey(
    val privateKey: ByteArray,
    val address: String,
)

internal object TipKeyGenerator {
    fun generate(
        seed: ByteArray,
        chainId: String,
        index: Int,
    ): TipDerivedKey =
        when (chainId) {
            Constants.ChainId.BITCOIN_CHAIN_ID,
            Constants.ChainId.PEARL_CHAIN_ID -> UtxoKeyGenerator.deriveFromTipSeed(seed, chainId, index)
            Constants.ChainId.SOLANA_CHAIN_ID -> SolanaKeyGenerator.deriveFromTipSeed(seed, index)
            Constants.ChainId.TRON_CHAIN_ID -> TronKeyGenerator.deriveFromTipSeed(seed, index)
            in Constants.Web3EvmChainIds -> EthKeyGenerator.deriveFromTipSeed(seed, index)
            else -> throw IllegalArgumentException("Not supported chainId")
        }
}
