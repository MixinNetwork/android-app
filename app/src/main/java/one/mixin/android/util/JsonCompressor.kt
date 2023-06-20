package one.mixin.android.util

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

fun compress(json: String): ByteArray {
    val out = ByteArrayOutputStream()
    val gzip = GZIPOutputStream(out)
    gzip.write(json.toByteArray())
    gzip.close()
    return out.toByteArray()
}

fun decompress(compressedJson: ByteArray): ByteArray {
    val out = ByteArrayOutputStream()
    val `in` = ByteArrayInputStream(compressedJson)
    val gzip = GZIPInputStream(`in`)
    val buffer = ByteArray(1024)
    var len: Int
    while (gzip.read(buffer).also { len = it } > 0) {
        out.write(buffer, 0, len)
    }
    gzip.close()
    return out.toByteArray()
}
