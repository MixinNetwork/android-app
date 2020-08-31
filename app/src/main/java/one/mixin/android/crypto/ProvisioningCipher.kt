package one.mixin.android.crypto

import org.whispersystems.libsignal.InvalidKeyException
import org.whispersystems.libsignal.ecc.Curve
import org.whispersystems.libsignal.ecc.ECPublicKey
import org.whispersystems.libsignal.kdf.HKDFv3
import java.security.NoSuchAlgorithmException
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.Mac
import javax.crypto.NoSuchPaddingException
import javax.crypto.spec.SecretKeySpec
import kotlin.jvm.Throws

class ProvisioningCipher(private val theirPublicKey: ECPublicKey) {

    @Throws(InvalidKeyException::class)
    fun encrypt(message: ByteArray): ByteArray {
        val ourKeyPair = Curve.generateKeyPair()
        val sharedSecret = Curve.calculateAgreement(theirPublicKey, ourKeyPair.privateKey)
        val derivedSecret = HKDFv3().deriveSecrets(sharedSecret, "Mixin Provisioning Message".toByteArray(), 64)
        val parts = Util.split(derivedSecret, 32, 32)

        val version = byteArrayOf(0x01)
        val ciphertext = getCiphertext(parts[0], message)
        val mac = getMac(parts[1], Util.join(version, ciphertext))
        val body = Util.join(version, ciphertext, mac)
        return ProvisionEnvelope(ourKeyPair.publicKey.serialize(), body).toByteArray()
    }

    private fun getCiphertext(key: ByteArray, message: ByteArray): ByteArray {
        try {
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"))
            return Util.join(cipher.iv, cipher.doFinal(message))
        } catch (e: NoSuchAlgorithmException) {
            throw AssertionError(e)
        } catch (e: NoSuchPaddingException) {
            throw AssertionError(e)
        } catch (e: java.security.InvalidKeyException) {
            throw AssertionError(e)
        } catch (e: IllegalBlockSizeException) {
            throw AssertionError(e)
        } catch (e: BadPaddingException) {
            throw AssertionError(e)
        }
    }

    private fun getMac(key: ByteArray, message: ByteArray): ByteArray {
        try {
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(key, "HmacSHA256"))
            return mac.doFinal(message)
        } catch (e: NoSuchAlgorithmException) {
            throw AssertionError(e)
        } catch (e: java.security.InvalidKeyException) {
            throw AssertionError(e)
        }
    }

    companion object {
        private val TAG = ProvisioningCipher::class.java.simpleName
    }
}
