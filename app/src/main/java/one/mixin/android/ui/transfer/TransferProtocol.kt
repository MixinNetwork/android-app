package one.mixin.android.ui.transfer

import timber.log.Timber
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.lang.IllegalStateException
import java.nio.ByteBuffer
import kotlin.experimental.xor
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
        if (size <= 0) return 
        if (type == TYPE_STRING) {
            return readDynamicLengthString(inputStream, size)
        } else if (type == TYPE_FILE) { // File
            // Todo
            return "File"
        } else {
            //
            return "Unknown"
        }
    }

    fun write(outputStream: OutputStream, content: String) {
        val data = content.toByteArray(UTF_8)
        outputStream.write(byteArrayOf(TYPE_STRING))
        outputStream.write(intToByteArray(data.size))
        outputStream.write(data)
        // outputStream.write(byteArrayOf(checksum(TYPE_STRING, data)))
    }

    fun write(outputStream: OutputStream, file: File){
        outputStream.write(byteArrayOf(TYPE_FILE))
        outputStream.write(intToByteArray(file.length().toInt()))
        // todo write file
        // outputStream.write(byteArrayOf(checksum(TYPE_STRING, data)))
    }

    private fun checksum(type: Byte, data: ByteArray): Byte {
        return type xor data.hashCode().toByte()
    }

    private fun readDynamicLengthString(inputStream: InputStream, expectedLength: Int): String {
        val data = ByteArray(expectedLength)
        inputStream.read(data)
        // val checksum = ByteArray(1)
        // inputStream.read(checksum)
        // Timber.e("$checksum")
        return String(data, UTF_8)
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
}