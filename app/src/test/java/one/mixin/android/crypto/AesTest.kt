package one.mixin.android.crypto


import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.Test
import tip.Tip
import java.security.Key
import java.security.Provider
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.system.measureTimeMillis

class AesTest {
    @Test
    fun aesTest() {

        // Generate 100 random strings
        val secureRandom = SecureRandom()
        // Generate random key
        val keyByte = ByteArray(32)
        secureRandom.nextBytes(keyByte)
        val key = SecretKeySpec(keyByte, "AES")

        val randomStrings = List(100) {
            ByteArray(secureRandom.nextInt(200) + 1).apply { secureRandom.nextBytes(this) }
        }

        // Test encryption and decryption speed
        val bouncyCastleTime = measureTimeMillis {
            randomStrings.forEach { input ->
                val encrypted = AesUtil.encryptBouncyCastle(input, key)
                val decrypted = AesUtil.decryptBouncyCastle(encrypted, key)
                require(input.contentEquals(decrypted)) { "Bouncy Castle encryption/decryption failed" }
            }
        }

        val javaxTime = measureTimeMillis {
            randomStrings.forEach { input ->
                val encrypted = AesUtil.encryptJavax(input, key)
                val decrypted = AesUtil.decryptJavax(encrypted, key)
                require(input.contentEquals(decrypted)) { "Javax encryption/decryption failed" }
            }
        }

        println("Bouncy Castle time: $bouncyCastleTime ms")
        println("Javax time: $javaxTime ms")
    }

}

object AesUtil {
    private const val AES_ALGORITHM = "AES/CBC/PKCS5Padding"
    private val BC_PROVIDER: Provider = BouncyCastleProvider()
    private val secureRandom = SecureRandom()
    @Throws(Exception::class)
    fun encryptBouncyCastle(input: ByteArray?, key: Key?): ByteArray {
        val cipher = Cipher.getInstance(AES_ALGORITHM, BC_PROVIDER)
        val iv = ByteArray(16)
        secureRandom.nextBytes(iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))
        val encrypted = cipher.doFinal(input)
        return iv.plus(encrypted)
    }

    @Throws(Exception::class)
    fun decryptBouncyCastle(input: ByteArray, key: Key?): ByteArray {
        val cipher = Cipher.getInstance(AES_ALGORITHM, BC_PROVIDER)
        val iv = input.sliceArray(0..15)
        cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))
        val encrypted: ByteArray = input.sliceArray(16 until input.size)
        return cipher.doFinal(encrypted)
    }

    @Throws(Exception::class)
    fun encryptJavax(input: ByteArray?, key: Key?): ByteArray {
        val cipher = Cipher.getInstance(AES_ALGORITHM)
        val iv = ByteArray(16)
        secureRandom.nextBytes(iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))
        val encrypted = cipher.doFinal(input)
        return iv.plus(encrypted)
    }

    @Throws(Exception::class)
    fun decryptJavax(input: ByteArray, key: Key?): ByteArray {
        val cipher = Cipher.getInstance(AES_ALGORITHM)
        val iv = input.sliceArray(0..15)
        val encrypted: ByteArray = input.sliceArray(16 until input.size)
        cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))
        return cipher.doFinal(encrypted)
    }

}
