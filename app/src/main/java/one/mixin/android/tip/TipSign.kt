package one.mixin.android.tip

import net.i2p.crypto.eddsa.EdDSAEngine
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec
import one.mixin.android.crypto.ed25519
import one.mixin.android.crypto.getPrivateKey
import one.mixin.android.crypto.getPublicKey
import one.mixin.android.extension.base64RawURLEncode
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Sign
import java.math.BigInteger
import java.security.MessageDigest

sealed class TipSignSpec(
    val algorithm: String,
    open val curve: String,
) {
    abstract fun public(priv: ByteArray): String
    abstract fun sign(priv: ByteArray, data: ByteArray): String

    sealed class Ecdsa(override val curve: String) : TipSignSpec("ecdsa", curve) {
        object Secp256k1 : Ecdsa("secp256k1") {
            override fun public(priv: ByteArray): String {
                return Sign.publicKeyFromPrivate(BigInteger(priv)).toByteArray().base64RawURLEncode()
            }

            override fun sign(priv: ByteArray, data: ByteArray): String {
                val privKey = BigInteger(priv)
                val pub = Sign.publicKeyFromPrivate(privKey)
                val keyPair = ECKeyPair(privKey, pub)
                val signature = Sign.signPrefixedMessage(data, keyPair)
                val b = ByteArray(65)
                System.arraycopy(signature.r, 0, b, 0, 32)
                System.arraycopy(signature.s, 0, b, 32, 32)
                System.arraycopy(signature.v, 0, b, 64, 1)
                return b.base64RawURLEncode()
            }
        }
    }

    sealed class Eddsa(override val curve: String) : TipSignSpec("eddsa", curve) {
        object Ed25519 : Eddsa("ed25519") {
            override fun public(priv: ByteArray): String {
                val privateSpec = EdDSAPrivateKeySpec(priv, ed25519)
                val pub = privateSpec.getPublicKey()
                return pub.abyte.base64RawURLEncode()
            }

            override fun sign(priv: ByteArray, data: ByteArray): String {
                val privateSpec = EdDSAPrivateKeySpec(priv, ed25519)
                val privateKey = privateSpec.getPrivateKey()
                val engine = EdDSAEngine(MessageDigest.getInstance(ed25519.hashAlgorithm))
                engine.initSign(privateKey)
                engine.update(data)
                return engine.sign().base64RawURLEncode()
            }
        }
    }
}

sealed class TipSignAction(open val spec: TipSignSpec) {
    data class Public(override val spec: TipSignSpec) : TipSignAction(spec) {
        operator fun invoke(priv: ByteArray) = spec.public(priv)
    }
    data class Signature(override val spec: TipSignSpec) : TipSignAction(spec) {
        operator fun invoke(priv: ByteArray, data: ByteArray) = spec.sign(priv, data)
    }
}

fun matchTipSignAction(action: String, alg: String, crv: String): TipSignAction? {
    val spec = matchTipSignSpec(alg, crv) ?: return null

    return when (action) {
        "public" -> TipSignAction.Public(spec)
        "sign" -> TipSignAction.Signature(spec)
        else -> null
    }
}

private fun matchTipSignSpec(alg: String, crv: String): TipSignSpec? {
    return if (alg.equals("ecdsa", true) && crv.equals("secp256k1", true)) {
        TipSignSpec.Ecdsa.Secp256k1
    } else if (alg.equals("eddsa", true) && crv.equals("ed25519", true)) {
        TipSignSpec.Eddsa.Ed25519
    } else {
        null
    }
}
