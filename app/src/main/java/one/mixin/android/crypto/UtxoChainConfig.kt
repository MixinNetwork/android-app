package one.mixin.android.crypto

import one.mixin.android.Constants
import one.mixin.android.tip.bip44.Bip44Path
import org.bitcoinj.base.Address
import org.bitcoinj.base.AddressParser
import org.bitcoinj.base.Base58
import org.bitcoinj.base.BitcoinNetwork
import org.bitcoinj.base.LegacyAddress
import org.bitcoinj.base.Network
import org.bitcoinj.base.ScriptType
import org.bitcoinj.base.SegwitAddress
import org.bitcoinj.crypto.ECKey
import java.math.BigInteger

data class UtxoChainConfig(
    val chainId: String,
    val network: Network,
    val addressScriptType: ScriptType,
    val decimals: Int,
    val wif: WifCodec,
    val dustRelayFeeAtomicPerKvB: Long,
    val incrementalRelayFeeAtomicPerKvB: Long,
    val walletMinimumFeeAtomicPerKvB: Long,
    val rbfEnabled: Boolean,
    val requiresOriginalRbfSignal: Boolean,
    val requiresHigherTargetFeeRate: Boolean,
    val replacementSafetyExtraAtomic: Long,
    private val path: (Int) -> IntArray,
    private val pathString: (Int) -> String,
    private val addressParser: (String) -> Address,
    private val dustThreshold: (ScriptType) -> Long,
) {
    fun derivationPath(index: Int): IntArray = path(index)

    fun derivationPathString(index: Int): String = pathString(index)

    fun extractAddressIndex(derivationPath: String): Int? {
        val prefix = pathString(0).dropLast(1)
        if (!derivationPath.startsWith(prefix)) return null
        val index = derivationPath.removePrefix(prefix).toIntOrNull()?.takeIf { it >= 0 } ?: return null
        return index.takeIf { pathString(it) == derivationPath }
    }

    fun parseAddress(address: String): Address = addressParser(address)

    fun dustThresholdAtomic(scriptType: ScriptType): Long = dustThreshold(scriptType)

    val conservativeMinimumRecipientAtomic: Long
        get() = listOf(ScriptType.P2WPKH, ScriptType.P2SH, ScriptType.P2PKH).maxOf(dustThreshold)

    val minimumChangeAtomic: Long
        get() = dustThreshold(addressScriptType)

    companion object {
        private const val BTC_MINIMUM_OUTPUT_ATOMIC = 1_000L
        private const val PEARL_MINIMUM_OUTPUT_ATOMIC = 100_000L
        private const val LTC_DUST_RELAY_FEE_ATOMIC_PER_KVB = 30_000L

        private val bitcoin = UtxoChainConfig(
            chainId = Constants.ChainId.BITCOIN_CHAIN_ID,
            network = BitcoinNetwork.MAINNET,
            addressScriptType = ScriptType.P2WPKH,
            decimals = 8,
            wif = WifCodec(128),
            dustRelayFeeAtomicPerKvB = 3_000L,
            incrementalRelayFeeAtomicPerKvB = 1_000L,
            walletMinimumFeeAtomicPerKvB = 1_000L,
            rbfEnabled = true,
            requiresOriginalRbfSignal = false,
            requiresHigherTargetFeeRate = false,
            replacementSafetyExtraAtomic = 300L,
            path = Bip44Path::bitcoinSegwit,
            pathString = Bip44Path::bitcoinSegwitPathString,
            addressParser = AddressParser.getDefault(BitcoinNetwork.MAINNET)::parseAddress,
            dustThreshold = { BTC_MINIMUM_OUTPUT_ATOMIC },
        )

        private val pearl = UtxoChainConfig(
            chainId = Constants.ChainId.PEARL_CHAIN_ID,
            network = PearlNetwork,
            addressScriptType = ScriptType.P2TR,
            decimals = 8,
            wif = WifCodec(128),
            dustRelayFeeAtomicPerKvB = 3_000L,
            incrementalRelayFeeAtomicPerKvB = 1_000L,
            walletMinimumFeeAtomicPerKvB = 1_000L,
            rbfEnabled = true,
            requiresOriginalRbfSignal = true,
            requiresHigherTargetFeeRate = false,
            replacementSafetyExtraAtomic = 300L,
            path = Bip44Path::pearl,
            pathString = Bip44Path::pearlPathString,
            addressParser = { address ->
                val parsed = SegwitAddress.fromBech32(address, PearlNetwork)
                require(
                    parsed.witnessVersion == 1 &&
                        parsed.witnessProgram.size == SegwitAddress.WITNESS_PROGRAM_LENGTH_TR,
                ) { "Invalid Pearl address" }
                parsed
            },
            dustThreshold = { PEARL_MINIMUM_OUTPUT_ATOMIC },
        )

        private val litecoin = UtxoChainConfig(
            chainId = Constants.ChainId.Litecoin,
            network = LitecoinNetwork,
            addressScriptType = ScriptType.P2WPKH,
            decimals = 8,
            wif = WifCodec(176),
            dustRelayFeeAtomicPerKvB = LTC_DUST_RELAY_FEE_ATOMIC_PER_KVB,
            incrementalRelayFeeAtomicPerKvB = 1_000L,
            walletMinimumFeeAtomicPerKvB = 10_000L,
            rbfEnabled = true,
            requiresOriginalRbfSignal = true,
            requiresHigherTargetFeeRate = true,
            replacementSafetyExtraAtomic = 0L,
            path = Bip44Path::litecoinSegwit,
            pathString = Bip44Path::litecoinSegwitPathString,
            addressParser = ::parseLitecoinAddress,
            dustThreshold = { scriptType ->
                when (scriptType) {
                    ScriptType.P2WPKH -> dustThreshold(serializedOutputBytes = 31, spendBytes = 67)
                    ScriptType.P2SH -> dustThreshold(serializedOutputBytes = 32, spendBytes = 148)
                    ScriptType.P2PKH -> dustThreshold(serializedOutputBytes = 34, spendBytes = 148)
                    else -> Long.MAX_VALUE
                }
            },
        )

        fun forChain(chainId: String): UtxoChainConfig =
            when (chainId) {
                Constants.ChainId.BITCOIN_CHAIN_ID -> bitcoin
                Constants.ChainId.PEARL_CHAIN_ID -> pearl
                Constants.ChainId.Litecoin -> litecoin
                else -> throw IllegalArgumentException("Unsupported UTXO chain: $chainId")
            }

        private fun parseLitecoinAddress(address: String): Address {
            require(!address.startsWith("ltc1mweb", ignoreCase = true)) { "MWEB addresses are not supported" }
            val parsed = runCatching {
                AddressParser.getDefault(LitecoinNetwork).parseAddress(address)
            }.recoverCatching {
                LegacyAddress.fromBase58(address, LitecoinLegacyP2shNetwork)
            }.getOrThrow()
            require(
                parsed.outputScriptType == ScriptType.P2PKH ||
                    parsed.outputScriptType == ScriptType.P2SH ||
                    parsed.outputScriptType == ScriptType.P2WPKH,
            ) { "Unsupported Litecoin address type" }
            return parsed
        }

        private fun dustThreshold(serializedOutputBytes: Long, spendBytes: Long): Long =
            (serializedOutputBytes + spendBytes) * LTC_DUST_RELAY_FEE_ATOMIC_PER_KVB / 1_000L
    }
}

class WifCodec(val prefix: Int) {
    fun encode(privateKey: ByteArray): String {
        validateSecp256k1PrivateKey(privateKey)
        return Base58.encodeChecked(prefix, privateKey + byteArrayOf(1))
    }

    fun decode(wif: String): ByteArray {
        val decoded = Base58.decodeChecked(wif)
        require(decoded.size == 34) { "Invalid compressed WIF length" }
        require(decoded[0].toInt() and 0xff == prefix) { "WIF belongs to another network" }
        require(decoded.last() == 1.toByte()) { "Uncompressed WIF is not supported" }
        return decoded.copyOfRange(1, 33).also(::validateSecp256k1PrivateKey)
    }
}

internal fun validateSecp256k1PrivateKey(privateKey: ByteArray) {
    require(privateKey.size == 32) { "Private key must be 32 bytes" }
    val value = BigInteger(1, privateKey)
    require(value.signum() > 0 && value < ECKey.ecDomainParameters().n) { "Invalid private key" }
}

internal object LitecoinNetwork : Network by BitcoinNetwork.MAINNET {
    override fun id(): String = "org.litecoin.production"
    override fun legacyAddressHeader(): Int = 48
    override fun legacyP2SHHeader(): Int = 50
    override fun segwitAddressHrp(): String = "ltc"
    override fun uriScheme(): String = "litecoin"
}

private object LitecoinLegacyP2shNetwork : Network by LitecoinNetwork {
    override fun id(): String = "org.litecoin.production.legacy-p2sh"
    override fun legacyP2SHHeader(): Int = 5
}

internal object PearlNetwork : Network by BitcoinNetwork.MAINNET {
    override fun id(): String = "org.pearl.production"
    override fun segwitAddressHrp(): String = "prl"
    override fun uriScheme(): String = "pearl"
}
