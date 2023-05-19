package one.mixin.android.ui.transfer

import UUIDUtils
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import one.mixin.android.RxBus
import one.mixin.android.api.ChecksumException
import one.mixin.android.event.SpeedEvent
import one.mixin.android.ui.transfer.vo.TransferCommand
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.io.EOFException
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.util.zip.CRC32
import kotlin.text.Charsets.UTF_8

/*
 * Data packet format:
 * -----------------------------------------------------------------
 * | type (1 byte) | body_length（4 bytes） | body | crc（8 bytes） |
 * -----------------------------------------------------------------
 * File packet format:
 * ----------------------------------------------------------------------------------
 * | type (1 byte) | body_length（4 bytes） | uuid(16 bytes) | body | crc（8 bytes） |
 * ----------------------------------------------------------------------------------
 */
@ExperimentalSerializationApi
class TransferProtocol(private val serializationJson: Json, private val server: Boolean = false) {

    companion object {
        const val TYPE_COMMAND = 0x01.toByte()
        const val TYPE_JSON = 0x02.toByte()
        const val TYPE_FILE = 0x03.toByte()

        private val MAX_DATA_SIZE = 512000 // 500K
    }

    fun read(inputStream: InputStream): Any? {
        val packageData = safeRead(inputStream, 5)
        val type = packageData[0]
        val sizeData = packageData.copyOfRange(1, 5)
        val size = byteArrayToInt(sizeData)
        return when (type) {
            TYPE_COMMAND -> { // COMMAND
                serializationJson.decodeFromStream<TransferCommand>(ByteArrayInputStream(readByteArray(inputStream, size)))
            }

            TYPE_JSON -> { // JSON
                val data = readByteArray(inputStream, size) ?: return null
                return sizeData + data
            }

            TYPE_FILE -> { // FILE
                readFile(inputStream, size)
            }

            else -> {
                throw IllegalStateException("Unknown")
            }
        }
    }

    fun write(outputStream: OutputStream, type: Byte, content: String) {
        val data = content.toByteArray(UTF_8)
        if (data.size >= MAX_DATA_SIZE) {
            Timber.e(content)
            return
        }
        outputStream.write(byteArrayOf(type))
        outputStream.write(intToByteArray(data.size))
        outputStream.write(data)
        outputStream.write(checksum(data))
        if (server) calculateReadSpeed(data.size + 13)
    }

    fun write(outputStream: OutputStream, file: File, messageId: String) {
        if (file.exists() && file.length() > 0) {
            outputStream.write(byteArrayOf(TYPE_FILE))
            outputStream.write(intToByteArray(file.length().toInt() + 16))
            val crc = CRC32()
            val uuidByteArray = UUIDUtils.toByteArray(messageId)
            outputStream.write(uuidByteArray)
            crc.update(uuidByteArray)
            val fileInputStream = FileInputStream(file)
            val buffer = ByteArray(1024)
            // Read data from file into buffer and write to socket
            var bytesRead = fileInputStream.read(buffer, 0, 1024)
            while (bytesRead != -1) {
                outputStream.write(buffer, 0, bytesRead)
                crc.update((buffer.copyOfRange(0, bytesRead)))
                bytesRead = fileInputStream.read(buffer, 0, 1024)
                if (server) calculateReadSpeed(bytesRead)
            }
            fileInputStream.close()
            outputStream.write(longToBytes(crc.value))
            Timber.e("Send file: ${file.name} ${file.length()} ${crc.value}")
            if (server) calculateReadSpeed(29)
        }
    }

    private fun checksum(data: ByteArray): ByteArray {
        return calculateCrc32(data)
    }

    @Throws(ChecksumException::class)
    private fun readByteArray(inputStream: InputStream, expectedLength: Int): ByteArray? {
        val data = safeRead(inputStream, expectedLength)
        val checksum = safeRead(inputStream, 8)
        if (bytesToLong(checksum) != bytesToLong(checksum(data))) {
            Timber.e("ChecksumException $expectedLength ${bytesToLong(checksum)} ${bytesToLong(checksum(data))}")
            throw ChecksumException()
        }
        if (expectedLength >= MAX_DATA_SIZE) {
            Timber.e(String(data))
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
        if (!server) calculateReadSpeed(expectedLength)
        return data
    }

    private var readCount: Long = 0
    private var lastTimeTime: Long = System.currentTimeMillis()

    private fun calculateReadSpeed(bytesPerRead: Int) {
        readCount += bytesPerRead
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastTimeTime > 1000) {
            val speed = readCount / ((currentTime - lastTimeTime) / 1000f) / 1024f / 128f
            RxBus.publish(SpeedEvent(String.format("%.2f Mb/s", speed)))
            readCount = 0
            lastTimeTime = currentTime
        }
    }

    private lateinit var cachePath: File
    fun setCachePath(cachePath: File) {
        this.cachePath = cachePath
    }
    private fun readFile(inputStream: InputStream, expectedLength: Int): File {
        val crc = CRC32()
        val uuidByteArray = safeRead(inputStream, 16)
        crc.update(uuidByteArray)
        val uuid = UUIDUtils.fromByteArray(uuidByteArray)
        val outFile = File(cachePath, uuid)
        val buffer = ByteArray(1024)
        var bytesRead = 0
        var bytesLeft = expectedLength - 16
        outFile.outputStream().use { fos ->
            while (bytesRead != -1 && bytesLeft > 0) {
                bytesRead = inputStream.read(buffer, 0, bytesLeft.coerceAtMost(1024))
                if (bytesRead > 0) {
                    fos.write(buffer, 0, bytesRead)
                    crc.update(buffer.copyOfRange(0, bytesRead))
                    bytesLeft -= bytesRead
                    if (!server) calculateReadSpeed(bytesRead)
                }
            }
        }
        val checksum = safeRead(inputStream, 8)
        val checksumLong = bytesToLong(checksum)
        Timber.e("Receive file: ${outFile.name} ${outFile.length()} checksum ${bytesToLong(checksum)} -- ${crc.value}")
        if (checksumLong != crc.value) {
            throw ChecksumException()
        }
        if (!server) calculateReadSpeed(29)
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

    private fun longToBytes(longValue: Long): ByteArray {
        return ByteBuffer.allocate(java.lang.Long.BYTES).putLong(longValue).array()
    }

    private fun bytesToLong(byteArray: ByteArray): Long {
        return ByteBuffer.wrap(byteArray).long
    }

    private fun calculateCrc32(bytes: ByteArray): ByteArray {
        val crc = CRC32()
        crc.update(bytes)
        return longToBytes(crc.value)
    }
}
