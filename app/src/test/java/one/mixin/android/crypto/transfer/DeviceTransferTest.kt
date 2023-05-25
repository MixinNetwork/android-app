package one.mixin.android.crypto.transfer

import one.mixin.android.extension.base64Encode
import one.mixin.android.ui.transfer.TransferCipher
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.security.InvalidKeyException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.util.Random
import java.util.UUID
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.IllegalBlockSizeException
import javax.crypto.Mac
import javax.crypto.NoSuchPaddingException
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.test.assertEquals

class DeviceTransferTest {
    private val secureRandom: SecureRandom by lazy { SecureRandom() }

    @Throws(IOException::class)
    fun generateRandomFile(fileName: String?, fileSize: Long) {
        val file = RandomAccessFile(fileName, "rw")
        val buffer = ByteArray(4096)
        var remaining = fileSize
        while (remaining > 0) {
            val size = buffer.size.toLong().coerceAtMost(remaining).toInt()
            Random().nextBytes(buffer)
            file.write(buffer, 0, size)
            remaining -= size.toLong()
        }
        file.close()
    }

    @Throws(
        NoSuchPaddingException::class,
        NoSuchAlgorithmException::class,
        InvalidKeyException::class,
        IOException::class,
        BadPaddingException::class,
        IllegalBlockSizeException::class,
    )
    fun encryptFile(inFileName: String?, outFileName: String?, secretKey: ByteArray, hMac: ByteArray): Pair<ByteArray, ByteArray> {
        val iv = ByteArray(16)
        secureRandom.nextBytes(iv)
        val keySpec = SecretKeySpec(secretKey, "AES")
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, IvParameterSpec(iv))
        val mac = Mac.getInstance("HmacSHA256")
        val secretKeySpec = SecretKeySpec(hMac, "HmacSHA256")
        mac.init(secretKeySpec)
        mac.update(iv)
        val `in` = FileInputStream(inFileName)
        val out = CipherOutputStream(FileOutputStream(outFileName), cipher)
        val buffer = ByteArray(1024)
        var count: Int
        while (`in`.read(buffer).also { count = it } > 0) {
            out.write(buffer, 0, count)
            mac.update(buffer, 0, count)
        }
        `in`.close()
        out.close()
        return Pair(iv, mac.doFinal())
    }

    @Throws(
        NoSuchPaddingException::class,
        NoSuchAlgorithmException::class,
        InvalidKeyException::class,
        IOException::class,
        BadPaddingException::class,
        IllegalBlockSizeException::class,
    )
    fun decryptFile(inFileName: String?, outFileName: String?, secretKey: ByteArray, iv: ByteArray, hMacKey: ByteArray): ByteArray {
        val keySpec = SecretKeySpec(secretKey, "AES")
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, keySpec, IvParameterSpec(iv))
        val `in` = CipherInputStream(FileInputStream(inFileName), cipher)
        val out = FileOutputStream(outFileName)
        val buffer = ByteArray(1024)
        var count: Int
        val mac = Mac.getInstance("HmacSHA256")
        val secretKeySpec = SecretKeySpec(hMacKey, "HmacSHA256")
        mac.init(secretKeySpec)
        mac.update(iv)
        while (`in`.read(buffer).also { count = it } > 0) {
            out.write(buffer, 0, count)
            mac.update(buffer, 0, count)
        }
        `in`.close()
        out.close()
        return mac.doFinal()
    }

    private fun calculateEncryptedSize(fileLength: Long): Int {
        val blockSize = 16
        val padding = blockSize - (fileLength % blockSize)
        return (fileLength + padding).toInt()
    }

    @Test
    fun testAesAndCrc() {
        repeat(100) {
            val secretBytes = TransferCipher.generateKey()
            val aesKey = secretBytes.sliceArray(0..31)
            val hMac = secretBytes.sliceArray(32..63)
            println("secret key ${secretBytes.base64Encode()}")
            val uuid = UUID.randomUUID().toString()
            val fileName = "$uuid.dat"
            val encFileName = "$uuid.enc"
            val decFileName = "$uuid.dec"
            // 5~10M random file
            val fileSize = (5242880 + Random().nextInt(5242880)).toLong()
            generateRandomFile(fileName, fileSize)

            val (iv, checkSum) = encryptFile(fileName, encFileName, aesKey, hMac)
            println("iv ${iv.base64Encode()}")
            // test EncryptedSize
            assertEquals(
                File(encFileName).length().toInt(),
                calculateEncryptedSize(File(fileName).length())
            )

            val decryptCheckSum = decryptFile(encFileName, decFileName, aesKey, iv, hMac)
            // test Encrypted checksum
            assert(checkSum.contentEquals(decryptCheckSum))

            val sourceMd5 = getFileMd5(File(fileName))
            val decMd5 = getFileMd5(File(decFileName))
            // test md5
            assertEquals(sourceMd5, decMd5)

            File(fileName).delete()
            File(encFileName).delete()
            File(decFileName).delete()
        }
    }

    private fun getFileMd5(file: File): String? {
        return try {
            val messageDigest = MessageDigest.getInstance("MD5")

            val fileInputStream = FileInputStream(file)
            val buffer = ByteArray(1024)
            var len = 0

            while (fileInputStream.read(buffer).also { len = it } != -1) {
                messageDigest.update(buffer, 0, len)
            }
            fileInputStream.close()

            val md5Bytes = messageDigest.digest()
            val sb = StringBuilder()
            for (b in md5Bytes) {
                val hexString = Integer.toHexString(0xFF and b.toInt())
                if (hexString.length == 1) {
                    sb.append('0')
                }
                sb.append(hexString)
            }
            sb.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
