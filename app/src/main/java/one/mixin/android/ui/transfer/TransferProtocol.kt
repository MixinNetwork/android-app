package one.mixin.android.ui.transfer

import UUIDUtils
import one.mixin.android.MixinApplication
import one.mixin.android.api.ChecksumException
import one.mixin.android.extension.getMediaPath
import one.mixin.android.extension.newTempFile
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.lang.IllegalStateException
import java.nio.ByteBuffer
import java.util.zip.CRC32
import kotlin.text.Charsets.UTF_8

class TransferProtocol {

    companion object {
        const val TYPE_STRING = 0x01.toByte()
        const val TYPE_FILE = 0x02.toByte()
    }

    fun read(inputStream: InputStream): String {
        val packageData = ByteArray(5)
        inputStream.read(packageData)
        val type = packageData[0]
        val size = byteArrayToInt(packageData.copyOfRange(1, 5))
        return when (type) {
            TYPE_STRING -> {
                readString(inputStream, size)
            }

            TYPE_FILE -> { // File
                readFile(inputStream, size).absolutePath
            }

            else -> {
                throw IllegalStateException("Unknown")
            }
        }
    }

    fun write(outputStream: OutputStream, content: String) {
        val data = content.toByteArray(UTF_8)
        outputStream.write(byteArrayOf(TYPE_STRING))
        outputStream.write(intToByteArray(data.size))
        outputStream.write(data)
        outputStream.write(checksum(data))
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
            }
            fileInputStream.close()
            outputStream.write(longToBytes(crc.value))
            Timber.e("Send file: ${file.name} ${file.length()} ${crc.value}")
        }
    }

    private fun checksum(data: ByteArray): ByteArray {
        return calculateCrc32(data)
    }

    private fun readString(inputStream: InputStream, expectedLength: Int): String {
        val data = ByteArray(expectedLength)
        inputStream.read(data)
        val checksum = ByteArray(8)
        inputStream.read(checksum)
        if (bytesToLong(checksum) != bytesToLong(checksum(data))) {
            throw ChecksumException()
        }
        return String(data, UTF_8)
    }

    private fun readFile(inputStream: InputStream, expectedLength: Int): File {
        val uuidByteArray = ByteArray(16)
        val crc = CRC32()
        inputStream.read(uuidByteArray)
        crc.update(uuidByteArray)
        val uuid = UUIDUtils.fromByteArray(uuidByteArray)
        // Todo replace real path
        val outFile = MixinApplication.get().getMediaPath()!!.newTempFile(uuid, "", false)
        val buffer = ByteArray(1024)
        var bytesRead = 0
        var bytesLeft = expectedLength - 16
        val fos = FileOutputStream(outFile)
        while (bytesRead != -1 && bytesLeft > 0) {
            bytesRead = inputStream.read(buffer, 0, bytesLeft.coerceAtMost(1024))
            if (bytesRead > 0) {
                fos.write(buffer, 0, bytesRead)
                crc.update(buffer.copyOfRange(0, bytesRead))
                bytesLeft -= bytesRead
            }
        }
        fos.close()
        val checksum = ByteArray(8)
        inputStream.read(checksum)
        val checksumLong = bytesToLong(checksum)
        if (checksumLong != crc.value) {
            throw ChecksumException()
        }
        Timber.e("Receive file: ${outFile.name} ${outFile.length()} checksum ${bytesToLong(checksum)} -- ${crc.value}")
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
