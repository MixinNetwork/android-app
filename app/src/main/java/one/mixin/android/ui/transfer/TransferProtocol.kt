package one.mixin.android.ui.transfer

import UUIDUtils
import kotlinx.coroutines.delay
import one.mixin.android.MixinApplication
import one.mixin.android.api.ChecksumException
import one.mixin.android.db.MixinDatabase
import one.mixin.android.extension.createDocumentTemp
import one.mixin.android.extension.getAudioPath
import one.mixin.android.extension.getDocumentPath
import one.mixin.android.extension.getExtensionName
import one.mixin.android.extension.getImagePath
import one.mixin.android.extension.getVideoPath
import one.mixin.android.vo.isAudio
import one.mixin.android.vo.isImage
import one.mixin.android.vo.isTranscript
import one.mixin.android.vo.isVideo
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

    private val messageDao by lazy {
        MixinDatabase.getDatabase(MixinApplication.appContext).messageDao()
    }

    suspend fun read(inputStream: InputStream): Pair<String?, File?> {
        if (inputStream.available() < 5) {
            delay(100)
            return read(inputStream)
        }
        val packageData = ByteArray(5)
        inputStream.read(packageData)
        val type = packageData[0]
        val size = byteArrayToInt(packageData.copyOfRange(1, 5))
        return when (type) {
            TYPE_STRING -> {
                Pair(readString(inputStream, size), null)
            }

            TYPE_FILE -> { // File
                val file = readFile(inputStream, size)
                if (file?.exists() == true) {
                    Pair(null, file)
                }
                Pair(null, null)
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
        var readLength = 0
        while (readLength < expectedLength) {
            readLength += inputStream.read(data, readLength, expectedLength - readLength)
        }
        val checksum = ByteArray(8)
        inputStream.read(checksum)
        if (bytesToLong(checksum) != bytesToLong(checksum(data))) {
            Timber.e("ChecksumException $expectedLength ${bytesToLong(checksum)} ${bytesToLong(checksum(data))}")
            throw ChecksumException()
        }
        return String(data, UTF_8)
    }

    private fun readFile(inputStream: InputStream, expectedLength: Int): File? {
        val uuidByteArray = ByteArray(16)
        val crc = CRC32()
        inputStream.read(uuidByteArray)
        crc.update(uuidByteArray)
        val uuid = UUIDUtils.fromByteArray(uuidByteArray)
        val message = messageDao.findMessageById(uuid)
        if (message == null) {
            val buffer = ByteArray(1024)
            var bytesRead = 0
            var bytesLeft = expectedLength - 16
            while (bytesRead != -1 && bytesLeft > 0) {
                bytesRead = inputStream.read(buffer, 0, bytesLeft.coerceAtMost(1024))
                bytesLeft -= bytesRead
            }
            val checksum = ByteArray(8)
            inputStream.read(checksum)
            // skip error file
            return null
        } else {
            val extensionName = message.name?.getExtensionName()
            val outFile = MixinApplication.get().let {
                if (message.isTranscript()) {
                    return null
                } else if (message.isImage()) {
                    it.getImagePath()
                } else if (message.isAudio()) {
                    it.getAudioPath()
                } else if (message.isVideo()) {
                    it.getVideoPath()
                } else {
                    it.getDocumentPath()
                }
            }.createDocumentTemp(message.conversationId, message.messageId, extensionName)
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
            Timber.e("Receive file: ${outFile.name} ${outFile.length()} checksum ${bytesToLong(checksum)} -- ${crc.value}")
            if (checksumLong != crc.value) {
                throw ChecksumException()
            }
            return outFile
        }
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
