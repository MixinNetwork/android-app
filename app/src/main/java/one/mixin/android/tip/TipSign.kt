package one.mixin.android.tip

import android.util.Log
import blockchain.Blockchain
import one.mixin.android.Constants
import one.mixin.android.crypto.initFromSeedAndSign
import one.mixin.android.crypto.newKeyPairFromSeed
import one.mixin.android.extension.hexString
import one.mixin.android.extension.toHex
import one.mixin.android.tip.bip44.Bip44Path
import one.mixin.android.tip.bip44.generateBip44Key
import org.sol4k.Keypair
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

    when (chainId) {
        Constants.ChainId.ETHEREUM_CHAIN_ID -> {
            val addressFromGo = Blockchain.generateEthereumAddress(priv.hexString())
            val bip44KeyPair = generateBip44Key(masterKeyPair, Bip44Path.Ethereum)
            val address = Keys.toChecksumAddress(Keys.getAddress(bip44KeyPair.publicKey))
            if (address != addressFromGo) {
                throw IllegalArgumentException("Generate illegal Etherenum Address")
            }
            return Numeric.toBytesPadded(bip44KeyPair.privateKey, 32)
        }
        Constants.ChainId.SOLANA_CHAIN_ID -> {
            val addressFromGo = Blockchain.generateSolanaAddress(priv.hexString())
            val bip44KeyPair = generateBip44Key(masterKeyPair, Bip44Path.Solana)
            val seed = Numeric.toBytesPadded(bip44KeyPair.privateKey, 32)
            val kp = Keypair.fromSecretKey(seed)
            val address = kp.publicKey.toBase58()
            if (address != addressFromGo) {
                throw IllegalArgumentException("Generate illegal Solana Address")
            }
            return kp.secret
        }
        else -> throw IllegalArgumentException("Not supported chainId")
    }
}

// private key
fun privateKeyToAddress(
    priv: ByteArray,
    chainId: String,
): String {
    val masterKeyPair = Bip32ECKeyPair.generateKeyPair(priv)
    when (chainId) {
        Constants.ChainId.ETHEREUM_CHAIN_ID -> {
            val bip44KeyPair = generateBip44Key(masterKeyPair, Bip44Path.Ethereum)
            val address = Keys.toChecksumAddress(Keys.getAddress(bip44KeyPair.publicKey))
            val addressFromGo = Blockchain.generateEthereumAddress(priv.hexString())
            if (address != addressFromGo) {
                throw IllegalArgumentException("Generate illegal Address")
            }
            return address
        }
        Constants.ChainId.SOLANA_CHAIN_ID -> {
            val bip44KeyPair = generateBip44Key(masterKeyPair, Bip44Path.Solana)
            val seed = Numeric.toBytesPadded(bip44KeyPair.privateKey, 32)
            val kp = Keypair.fromSecretKey(seed)
            val address = kp.publicKey.toBase58()
            val addressFromGo = Blockchain.generateSolanaAddress(priv.hexString())
            if (address != addressFromGo) {
                throw IllegalArgumentException("Generate illegal Solana Address")
            }
            return address
        }
        else -> throw IllegalArgumentException("Not supported chainId")
    }
}
