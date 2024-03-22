package one.mixin.android.tip

import one.mixin.android.Constants
import one.mixin.android.crypto.initFromSeedAndSign
import one.mixin.android.crypto.newKeyPairFromSeed
import one.mixin.android.extension.toHex
import one.mixin.android.tip.bip44.Bip44Path
import one.mixin.android.tip.bip44.generateBip44Key
import org.web3j.crypto.Bip32ECKeyPair
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Keys
import org.web3j.crypto.Sign
import org.web3j.utils.Numeric

const val TAG_TIP_SIGN = "TIP_sign"

sealed class TipSignSpec(
    val algorithm: String,
    open val curve: String,
) {
    abstract fun public(priv: ByteArray): String

    abstract fun sign(
        priv: ByteArray,
        data: ByteArray,
    ): String

    sealed class Ecdsa(override val curve: String) : TipSignSpec("ecdsa", curve) {
        object Secp256k1 : Ecdsa("secp256k1") {
            override fun public(priv: ByteArray): String {
                return ECKeyPair.create(priv).publicKey.toByteArray().toHex()
            }

            override fun sign(
                priv: ByteArray,
                data: ByteArray,
            ): String {
                val keyPair = ECKeyPair.create(priv)
                val signature = Sign.signPrefixedMessage(data, keyPair)
                val b = ByteArray(65)
                System.arraycopy(signature.r, 0, b, 0, 32)
                System.arraycopy(signature.s, 0, b, 32, 32)
                System.arraycopy(signature.v, 0, b, 64, 1)
                return b.toHex()
            }
        }
    }

    sealed class Eddsa(override val curve: String) : TipSignSpec("eddsa", curve) {
        object Ed25519 : Eddsa("ed25519") {
            override fun public(priv: ByteArray): String {
                val keypair = newKeyPairFromSeed(priv)
                return keypair.publicKey.toHex()
            }

            override fun sign(
                priv: ByteArray,
                data: ByteArray,
            ): String {
                return initFromSeedAndSign(priv, data).toHex()
            }
        }
    }
}

sealed class TipSignAction(open val spec: TipSignSpec) {
    data class Public(override val spec: TipSignSpec) : TipSignAction(spec) {
        operator fun invoke(priv: ByteArray) = spec.public(tipPrivToPrivateKey(priv))
    }

    data class Signature(override val spec: TipSignSpec) : TipSignAction(spec) {
        operator fun invoke(
            priv: ByteArray,
            data: ByteArray,
        ) = spec.sign(tipPrivToPrivateKey(priv), data)
    }
}

fun matchTipSignAction(
    action: String,
    alg: String,
    crv: String,
): TipSignAction? {
    val spec = matchTipSignSpec(alg, crv) ?: return null

    return when (action) {
        "public" -> TipSignAction.Public(spec)
        "sign" -> TipSignAction.Signature(spec)
        else -> null
    }
}

private fun matchTipSignSpec(
    alg: String,
    crv: String,
): TipSignSpec? {
    return if (alg.equals("ecdsa", true) && crv.equals("secp256k1", true)) {
        TipSignSpec.Ecdsa.Secp256k1
    } else if (alg.equals("eddsa", true) && crv.equals("ed25519", true)) {
        TipSignSpec.Eddsa.Ed25519
    } else {
        null
    }
}

fun tipPrivToPrivateKey(
    priv: ByteArray,
    chainId: String = Constants.ChainId.ETHEREUM_CHAIN_ID,
): ByteArray {
    val masterKeyPair = Bip32ECKeyPair.generateKeyPair(priv)
    val bip44KeyPair =
        when (chainId) {
            Constants.ChainId.BITCOIN_CHAIN_ID -> generateBip44Key(masterKeyPair, Bip44Path.Bitcoin)
            Constants.ChainId.ETHEREUM_CHAIN_ID -> generateBip44Key(masterKeyPair, Bip44Path.Ethereum)
            Constants.ChainId.SOLANA_CHAIN_ID -> generateBip44Key(masterKeyPair, Bip44Path.Solana)
            else -> throw IllegalArgumentException("Not supported chainId")
        }
    return Numeric.toBytesPadded(bip44KeyPair.privateKey, 32)
}

fun tipPrivToAddress(
    priv: ByteArray,
    chainId: String,
): String {
    val masterKeyPair = Bip32ECKeyPair.generateKeyPair(priv)
    val bip44KeyPair =
        when (chainId) {
            Constants.ChainId.BITCOIN_CHAIN_ID -> generateBip44Key(masterKeyPair, Bip44Path.Bitcoin)
            Constants.ChainId.ETHEREUM_CHAIN_ID -> generateBip44Key(masterKeyPair, Bip44Path.Ethereum)
            Constants.ChainId.SOLANA_CHAIN_ID -> generateBip44Key(masterKeyPair, Bip44Path.Solana)
            else -> throw IllegalArgumentException("Not supported chainId")
        }
    return Keys.toChecksumAddress(Keys.getAddress(bip44KeyPair.publicKey))
}
