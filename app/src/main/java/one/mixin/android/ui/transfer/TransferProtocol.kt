package one.mixin.android.ui.transfer

import UUIDUtils
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import one.mixin.android.RxBus
import one.mixin.android.api.ChecksumException
import one.mixin.android.event.SpeedEvent
import one.mixin.android.extension.base64Encode
import one.mixin.android.extension.gzip
import one.mixin.android.extension.ungzip
import one.mixin.android.ui.transfer.vo.TransferCommand
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.io.EOFException
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.text.Charsets.UTF_8

/*
 * Command packet format:
 * ---------------------------------------------------------------------------------------
 * | type (1 byte 01) | body_length（4 bytes） | [iv (16bytes) | body(AES)] | hMac（32 bytes） |
 * ---------------------------------------------------------------------------------------
 *
 * Data packet format:
 * ---------------------------------------------------------------------------------------
 * | type (1 byte 02) | body_length（4 bytes） | [iv (16bytes) | body(AES)] | hMac（32 bytes） |
 * ---------------------------------------------------------------------------------------
 *
 * File packet format:
 * ---------------------------------------------------------------------------------------------------------
 * | type (1 byte 03) | body_length（4 bytes）| [uuid(16 bytes) | iv (16bytes) | body(AES)] | hMac（32 bytes） |
 * ---------------------------------------------------------------------------------------------------------
 *
 *  body_length equals to the content length within the "[]"
 *  [] indicates the data that needs to be verified
 */
@ExperimentalSerializationApi
class TransferProtocol(private val serializationJson: Json, private val secretBytes: ByteArray, private val server: Boolean = false) {

    companion object {
        const val TYPE_COMMAND = 0x01.toByte()
        const val TYPE_JSON = 0x02.toByte()
        const val TYPE_FILE = 0x03.toByte()
        private const val IV_LENGTH = 16
        private const val UUID_LENGTH = 16
        private const val H_MAC_LENGTH = 32
        private const val EXT_LENGTH = 37 // type + body_length + hMac
        private const val MAX_DATA_SIZE = 512000 // 500K
    }

    private val secureRandom: SecureRandom by lazy { SecureRandom() }
    private val aesKey by lazy {
        SecretKeySpec(secretBytes.sliceArray(0..31), "AES")
    }

    private val hMacKey by lazy {
        secretBytes.sliceArray(32..63)
    }

    fun read(inputStream: InputStream): Any? {
        val packageData = safeRead(inputStream, 5)
        val type = packageData[0]
        val sizeData = packageData.copyOfRange(1, 5)
        val size = byteArrayToInt(sizeData)
        return when (type) {
            TYPE_COMMAND -> { // COMMAND
                val ciphertext = readByteArray(inputStream, size)?.ungzip() ?: return null
                serializationJson.decodeFromStream<TransferCommand>(ByteArrayInputStream(decrypt(ciphertext)))
            }

            TYPE_JSON -> { // JSON
                val ciphertext = readByteArray(inputStream, size) ?: return null
                return sizeData + ciphertext
            }

            TYPE_FILE -> { // FILE
                readFile(inputStream, size)
            }

            else -> {
                throw IllegalStateException("Unknown")
            }
        }
    }

    private fun preprocessingData(content: String): ByteArray? {
        val data = content.toByteArray(UTF_8)
        if (data.size >= MAX_DATA_SIZE) {
            return null
        }
        return encrypt(data.gzip())
    }

    fun write(outputStream: OutputStream, type: Byte, content: String) {
        val writeData = preprocessingData(content) ?: return
        outputStream.write(byteArrayOf(type))
        outputStream.write(intToByteArray(writeData.size))
        outputStream.write(writeData)
        outputStream.write(checksum(writeData))
        if (server) calculateSpeed(writeData.size + EXT_LENGTH)
    }

    private fun encrypt(input: ByteArray): ByteArray {
        val iv = ByteArray(16)
        secureRandom.nextBytes(iv)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, IvParameterSpec(iv))
        val result = cipher.doFinal(input)
        return iv.plus(result)
    }

    private fun decrypt(ciphertext: ByteArray): ByteArray {
        val iv = ciphertext.sliceArray(0..15)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, aesKey, IvParameterSpec(iv))
        return cipher.doFinal(ciphertext.sliceArray(16 until ciphertext.size))
    }

    fun write(outputStream: OutputStream, file: File, messageId: String) {
        if (file.exists() && file.length() > 0) {
            // Read data from file into buffer and write to socket
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(hMacKey, "HmacSHA256"))
            outputStream.write(byteArrayOf(TYPE_FILE))
            val iv = ByteArray(IV_LENGTH)
            secureRandom.nextBytes(iv)
            outputStream.write(intToByteArray(calculateEncryptedSize(file.length()) + UUID_LENGTH + IV_LENGTH))

            val uuid = UUIDUtils.toByteArray(messageId)
            mac.update(uuid)
            outputStream.write(uuid)

            mac.update(iv)
            outputStream.write(iv)

            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, IvParameterSpec(iv))
            file.inputStream().use { fileInputStream ->
                CipherInputStream(fileInputStream, cipher).use { cis ->
                    val buffer = ByteArray(1024)
                    var read: Int
                    while (cis.read(buffer).also { read = it } != -1) {
                        outputStream.write(buffer, 0, read)
                        mac.update(buffer, 0, read)
                        if (server) calculateSpeed(read)
                    }
                }
            }
            val hMac = mac.doFinal()
            outputStream.write(hMac)
            if (server) calculateSpeed(EXT_LENGTH)
        }
    }

    private fun calculateEncryptedSize(fileLength: Long): Int {
        val blockSize = 16
        val padding = blockSize - (fileLength % blockSize)
        return (fileLength + padding).toInt()
    }

    private fun checksum(data: ByteArray): ByteArray {
        return calculateHmac(data)
    }

    @Throws(ChecksumException::class)
    private fun readByteArray(inputStream: InputStream, expectedLength: Int): ByteArray? {
        val data = safeRead(inputStream, expectedLength)
        val checksum = safeRead(inputStream, H_MAC_LENGTH)
        if (!checksum.contentEquals(checksum(data))) {
            Timber.e("ChecksumException $expectedLength ${checksum.base64Encode()} ${checksum(data).base64Encode()}")
            throw ChecksumException()
        }
        if (expectedLength >= MAX_DATA_SIZE) {
            return null
        }
        return data
    }

    private fun safeRead(inputStream: InputStream, expectedLength: Int): ByteArray {
        val data = ByteArray(expectedLength)
        var readLength = 0
        while (readLength < expectedLength) {
            val count = inputStream.read(data, readLength, expectedLength - readLength)
            if (count == -1) {
                throw EOFException("Unexpected end of data")
            }

            readLength += count
        }
        if (!server) calculateSpeed(expectedLength)
        return data
    }

    private var readCount: Long = 0
    private var lastTimeTime: Long = System.currentTimeMillis()

    private fun calculateSpeed(bytesPerRead: Int) {
        readCount += bytesPerRead
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastTimeTime > 1000) {
            val speed = calculateNetworkSpeed(readCount / ((currentTime - lastTimeTime) / 1000f))
            RxBus.publish(SpeedEvent(speed))
            readCount = 0
            lastTimeTime = currentTime
        }
    }

    private fun calculateNetworkSpeed(bytesPerSecond: Float): String {
        val speedInKb = bytesPerSecond / 1024
        return if (speedInKb < 1024) {
            String.format("%.2f KB/s", speedInKb)
        } else {
            val speedInMb = speedInKb / 1024
            String.format("%.2f MB/s", speedInMb)
        }
    }

    private lateinit var cachePath: File
    fun setCachePath(cachePath: File) {
        this.cachePath = cachePath
    }

    private fun readFile(inputStream: InputStream, expectedLength: Int): File {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(hMacKey, "HmacSHA256"))
        val uuidByteArray = safeRead(inputStream, UUID_LENGTH)
        val uuid = UUIDUtils.fromByteArray(uuidByteArray)
        mac.update(uuidByteArray)
        val iv = safeRead(inputStream, IV_LENGTH)
        mac.update(iv)

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, aesKey, IvParameterSpec(iv))
        val outFile = File(cachePath, uuid)
        val buffer = ByteArray(1024)
        var bytesRead = 0
        var bytesLeft = expectedLength - IV_LENGTH - UUID_LENGTH
        outFile.outputStream().use { fos ->
            CipherOutputStream(fos, cipher).use { cos ->
                while (bytesRead != -1 && bytesLeft > 0) {
                    bytesRead = inputStream.read(buffer, 0, bytesLeft.coerceAtMost(1024))
                    if (bytesRead > 0) {
                        cos.write(buffer, 0, bytesRead)
                        mac.update(buffer, 0, bytesRead)
                        bytesLeft -= bytesRead
                        if (!server) calculateSpeed(bytesRead)
                    }
                }
            }
        }
        val checksum = safeRead(inputStream, H_MAC_LENGTH)
        val decodeCheckSum = mac.doFinal()
        Timber.e("Receive file: ${outFile.name} ${outFile.length()} ${expectedLength - IV_LENGTH - UUID_LENGTH} ")
        if (!decodeCheckSum.contentEquals(checksum)) {
            Timber.e("Checksum ${checksum.base64Encode()} -- ${decodeCheckSum.base64Encode()}")
            throw ChecksumException()
        }
        if (!server) calculateSpeed(EXT_LENGTH)
        return outFile
    }

    private fun byteArrayToInt(byteArray: ByteArray): Int {
        var result = 0
        for (i in byteArray.indices) {
            result = result shl 8
            result = result or (byteArray[i].toInt() and 0xff)
        }
        return result
    }

    private fun intToByteArray(intValue: Int): ByteArray {
        val byteBuffer = ByteBuffer.allocate(4)
        byteBuffer.putInt(intValue)
        return byteBuffer.array()
    }

    private fun calculateHmac(bytes: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        val secretKeySpec = SecretKeySpec(hMacKey, "HmacSHA256")
        mac.init(secretKeySpec)
        return mac.doFinal(bytes)
    }
}
